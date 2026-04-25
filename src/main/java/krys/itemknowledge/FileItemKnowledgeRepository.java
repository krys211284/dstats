package krys.itemknowledge;

import krys.item.EquipmentSlot;
import krys.itemimport.ImportedItemAffixType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plikowe repozytorium bazy wiedzy o itemach, oddzielone od biblioteki konkretnych itemów. */
public final class FileItemKnowledgeRepository implements ItemKnowledgeRepository {
    private static final String FILE_NAME = "item-knowledge.db";
    private static final String META_PREFIX = "META";
    private static final String ENTRY_PREFIX = "ENTRY";
    private static final String AFFIX_PREFIX = "AFFIX";
    private static final String ASPECT_PREFIX = "ASPECT";

    private final Path filePath;

    public FileItemKnowledgeRepository(Path dataDirectory) {
        this.filePath = dataDirectory.resolve(FILE_NAME);
    }

    @Override
    public synchronized ItemKnowledgeSnapshot load() {
        if (!Files.exists(filePath)) {
            return ItemKnowledgeSnapshot.empty();
        }
        try {
            ItemKnowledgeEpoch epoch = new ItemKnowledgeEpoch(1, "Epoka wiedzy 1");
            Map<ItemKnowledgeKey, EntryBuilder> builders = new LinkedHashMap<>();
            for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
                String trimmedLine = line.trim();
                if (trimmedLine.isBlank()) {
                    continue;
                }
                String[] tokens = trimmedLine.split("\\|", -1);
                if (tokens.length == 0) {
                    continue;
                }
                switch (tokens[0]) {
                    case META_PREFIX -> epoch = parseEpoch(tokens);
                    case ENTRY_PREFIX -> parseEntry(tokens, builders);
                    case AFFIX_PREFIX -> parseAffix(tokens, builders);
                    case ASPECT_PREFIX -> parseAspect(tokens, builders);
                    default -> {
                    }
                }
            }
            return new ItemKnowledgeSnapshot(epoch, builders.values().stream()
                    .map(EntryBuilder::build)
                    .toList());
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się odczytać bazy wiedzy o itemach.", exception);
        }
    }

    @Override
    public synchronized void save(ItemKnowledgeSnapshot snapshot) {
        ItemKnowledgeSnapshot safeSnapshot = snapshot == null ? ItemKnowledgeSnapshot.empty() : snapshot;
        ensureDirectoryExists();
        List<String> lines = new ArrayList<>();
        lines.add(META_PREFIX + "|" + safeSnapshot.getActiveEpoch().sequence() + "|" + encode(safeSnapshot.getActiveEpoch().label()));
        List<ItemKnowledgeEntry> entries = safeSnapshot.getEntries().stream()
                .sorted(Comparator.comparing((ItemKnowledgeEntry entry) -> entry.getKey().slot().name())
                        .thenComparing(entry -> entry.getKey().itemType()))
                .toList();
        for (ItemKnowledgeEntry entry : entries) {
            String slot = entry.getKey().slot().name();
            String type = encode(entry.getKey().itemType());
            lines.add(ENTRY_PREFIX + "|" + slot + "|" + type + "|" + entry.getItemObservationCount());
            entry.getAffixTypeCounts().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(affix -> lines.add(AFFIX_PREFIX + "|" + slot + "|" + type + "|" + affix.getKey().name() + "|" + affix.getValue()));
            entry.getAspectCounts().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(aspect -> lines.add(ASPECT_PREFIX + "|" + slot + "|" + type + "|" + encode(aspect.getKey()) + "|" + aspect.getValue()));
        }
        try {
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się zapisać bazy wiedzy o itemach.", exception);
        }
    }

    private static ItemKnowledgeEpoch parseEpoch(String[] tokens) {
        if (tokens.length < 3) {
            return new ItemKnowledgeEpoch(1, "Epoka wiedzy 1");
        }
        return new ItemKnowledgeEpoch(Integer.parseInt(tokens[1]), decode(tokens[2]));
    }

    private static void parseEntry(String[] tokens, Map<ItemKnowledgeKey, EntryBuilder> builders) {
        if (tokens.length < 4) {
            return;
        }
        ItemKnowledgeKey key = parseKey(tokens);
        builders.computeIfAbsent(key, EntryBuilder::new).itemObservationCount = Integer.parseInt(tokens[3]);
    }

    private static void parseAffix(String[] tokens, Map<ItemKnowledgeKey, EntryBuilder> builders) {
        if (tokens.length < 5) {
            return;
        }
        ItemKnowledgeKey key = parseKey(tokens);
        builders.computeIfAbsent(key, EntryBuilder::new)
                .affixTypeCounts.put(ImportedItemAffixType.valueOf(tokens[3]), Integer.parseInt(tokens[4]));
    }

    private static void parseAspect(String[] tokens, Map<ItemKnowledgeKey, EntryBuilder> builders) {
        if (tokens.length < 5) {
            return;
        }
        ItemKnowledgeKey key = parseKey(tokens);
        builders.computeIfAbsent(key, EntryBuilder::new)
                .aspectCounts.put(decode(tokens[3]), Integer.parseInt(tokens[4]));
    }

    private static ItemKnowledgeKey parseKey(String[] tokens) {
        return new ItemKnowledgeKey(EquipmentSlot.valueOf(tokens[1]), decode(tokens[2]));
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się przygotować katalogu bazy wiedzy o itemach.", exception);
        }
    }

    private static String encode(String value) {
        String safeValue = value == null ? "" : value;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(safeValue.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static final class EntryBuilder {
        private final ItemKnowledgeKey key;
        private int itemObservationCount;
        private final Map<ImportedItemAffixType, Integer> affixTypeCounts = new java.util.EnumMap<>(ImportedItemAffixType.class);
        private final Map<String, Integer> aspectCounts = new LinkedHashMap<>();

        private EntryBuilder(ItemKnowledgeKey key) {
            this.key = key;
        }

        private ItemKnowledgeEntry build() {
            return new ItemKnowledgeEntry(key, itemObservationCount, affixTypeCounts, aspectCounts);
        }
    }
}
