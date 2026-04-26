package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Trwałe repozytorium biblioteki itemów oparte o proste pliki tekstowe w wyznaczonym katalogu danych użytkownika. */
public final class FileItemLibraryRepository implements ItemLibraryRepository {
    private static final String ITEM_PREFIX = "ITEM";
    private static final String SELECTION_PREFIX = "SEL";

    private final Path itemsFilePath;
    private final Path selectionFilePath;

    public FileItemLibraryRepository(Path dataDirectory) {
        this.itemsFilePath = dataDirectory.resolve("saved-items.db");
        this.selectionFilePath = dataDirectory.resolve("active-selection.db");
    }

    @Override
    public synchronized SavedImportedItem save(SavedImportedItem item) {
        List<SavedImportedItem> items = loadItems();
        SavedImportedItem persistedItem = item;
        if (item.getItemId() <= 0L) {
            long nextId = items.stream()
                    .mapToLong(SavedImportedItem::getItemId)
                    .max()
                    .orElse(0L) + 1L;
            persistedItem = new SavedImportedItem(
                    nextId,
                    item.getDisplayName(),
                    item.getSourceImageName(),
                    item.getSlot(),
                    item.getWeaponDamage(),
                    item.getStrength(),
                    item.getIntelligence(),
                    item.getThorns(),
                    item.getBlockChance(),
                    item.getRetributionChance(),
                    item.getFullItemRead(),
                    item.getAffixes(),
                    item.getSelectedAspectId()
            );
        }

        SavedImportedItem finalPersistedItem = persistedItem;
        items.removeIf(existingItem -> existingItem.getItemId() == finalPersistedItem.getItemId());
        items.add(finalPersistedItem);
        items.sort(Comparator.comparingLong(SavedImportedItem::getItemId));
        writeItems(items);
        return finalPersistedItem;
    }

    @Override
    public synchronized List<SavedImportedItem> findAll() {
        return List.copyOf(loadItems());
    }

    @Override
    public synchronized Optional<SavedImportedItem> findById(long itemId) {
        return loadItems().stream()
                .filter(item -> item.getItemId() == itemId)
                .findFirst();
    }

    @Override
    public synchronized void delete(long itemId) {
        List<SavedImportedItem> items = loadItems();
        items.removeIf(item -> item.getItemId() == itemId);
        writeItems(items);
    }

    @Override
    public synchronized ActiveItemSelection loadSelection() {
        if (!Files.exists(selectionFilePath)) {
            return ActiveItemSelection.empty();
        }

        EnumMap<EquipmentSlot, Long> selection = new EnumMap<>(EquipmentSlot.class);
        try {
            for (String line : Files.readAllLines(selectionFilePath, StandardCharsets.UTF_8)) {
                String trimmedLine = line.trim();
                if (trimmedLine.isBlank()) {
                    continue;
                }
                String[] tokens = trimmedLine.split("\\|", -1);
                if (tokens.length != 3 || !SELECTION_PREFIX.equals(tokens[0])) {
                    continue;
                }
                selection.put(
                        EquipmentSlot.valueOf(tokens[1]),
                        Long.parseLong(tokens[2])
                );
            }
            return new ActiveItemSelection(selection);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się odczytać pliku selekcji biblioteki itemów.", exception);
        }
    }

    @Override
    public synchronized void saveSelection(ActiveItemSelection selection) {
        ensureDirectoryExists();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<EquipmentSlot, Long> entry : selection.getSelectedItemIdsBySlot().entrySet()) {
            lines.add(SELECTION_PREFIX + "|" + entry.getKey().name() + "|" + entry.getValue());
        }
        try {
            Files.write(selectionFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się zapisać selekcji biblioteki itemów.", exception);
        }
    }

    private List<SavedImportedItem> loadItems() {
        if (!Files.exists(itemsFilePath)) {
            return new ArrayList<>();
        }

        try {
            List<SavedImportedItem> items = new ArrayList<>();
            for (String line : Files.readAllLines(itemsFilePath, StandardCharsets.UTF_8)) {
                String trimmedLine = line.trim();
                if (trimmedLine.isBlank()) {
                    continue;
                }
                items.add(parseItem(trimmedLine));
            }
            items.sort(Comparator.comparingLong(SavedImportedItem::getItemId));
            return items;
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się odczytać pliku biblioteki itemów.", exception);
        }
    }

    private SavedImportedItem parseItem(String line) {
        String[] tokens = line.split("\\|", -1);
        if ((tokens.length != 11 && tokens.length != 12 && tokens.length != 13 && tokens.length != 14) || !ITEM_PREFIX.equals(tokens[0])) {
            throw new IllegalStateException("Plik biblioteki itemów ma niepoprawny format.");
        }
        return new SavedImportedItem(
                Long.parseLong(tokens[1]),
                decode(tokens[2]),
                decode(tokens[3]),
                EquipmentSlot.valueOf(tokens[4]),
                Long.parseLong(tokens[5]),
                Double.parseDouble(tokens[6]),
                Double.parseDouble(tokens[7]),
                Double.parseDouble(tokens[8]),
                Double.parseDouble(tokens[9]),
                Double.parseDouble(tokens[10]),
                tokens.length >= 12 ? decodeFullItemRead(tokens[11]) : FullItemRead.empty(),
                tokens.length >= 13 ? decodeAffixes(tokens[12]) : List.of(),
                tokens.length >= 14 ? decode(tokens[13]) : ""
        );
    }

    private void writeItems(List<SavedImportedItem> items) {
        ensureDirectoryExists();
        List<String> lines = new ArrayList<>();
        for (SavedImportedItem item : items) {
            lines.add(String.join("|",
                    ITEM_PREFIX,
                    Long.toString(item.getItemId()),
                    encode(item.getDisplayName()),
                    encode(item.getSourceImageName()),
                    item.getSlot().name(),
                    Long.toString(item.getWeaponDamage()),
                    formatDouble(item.getStrength()),
                    formatDouble(item.getIntelligence()),
                    formatDouble(item.getThorns()),
                    formatDouble(item.getBlockChance()),
                    formatDouble(item.getRetributionChance()),
                    encodeFullItemRead(item.getFullItemRead()),
                    encodeAffixes(item.getAffixes()),
                    encode(item.getSelectedAspectId())
            ));
        }
        try {
            Files.write(itemsFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się zapisać biblioteki itemów.", exception);
        }
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(itemsFilePath.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się przygotować katalogu biblioteki itemów.", exception);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String formatDouble(double value) {
        return String.format(Locale.US, "%.4f", value);
    }

    private static String encodeFullItemRead(FullItemRead fullItemRead) {
        FullItemRead safeRead = fullItemRead == null ? FullItemRead.empty() : fullItemRead;
        List<String> payloadLines = new ArrayList<>();
        payloadLines.add("NAME|" + encode(safeRead.getItemName()));
        payloadLines.add("TYPE|" + encode(safeRead.getItemTypeLine()));
        payloadLines.add("RARITY|" + encode(safeRead.getRarity()));
        payloadLines.add("POWER|" + encode(safeRead.getItemPower()));
        payloadLines.add("BASE|" + encode(safeRead.getBaseItemValue()));
        for (FullItemReadLine line : safeRead.getLines()) {
            payloadLines.add("LINE|" + line.getType().name() + "|" + encode(line.getText()));
        }
        return encode(String.join("\n", payloadLines));
    }

    private static FullItemRead decodeFullItemRead(String encodedPayload) {
        String payload = decode(encodedPayload);
        String itemName = "";
        String itemTypeLine = "";
        String rarity = "";
        String itemPower = "";
        String baseItemValue = "";
        List<FullItemReadLine> lines = new ArrayList<>();
        for (String line : payload.split("\\R")) {
            String[] tokens = line.split("\\|", -1);
            if (tokens.length < 2) {
                continue;
            }
            switch (tokens[0]) {
                case "NAME" -> itemName = decode(tokens[1]);
                case "TYPE" -> itemTypeLine = decode(tokens[1]);
                case "RARITY" -> rarity = decode(tokens[1]);
                case "POWER" -> itemPower = decode(tokens[1]);
                case "BASE" -> baseItemValue = decode(tokens[1]);
                case "LINE" -> {
                    if (tokens.length >= 3) {
                        lines.add(new FullItemReadLine(FullItemReadLineType.valueOf(tokens[1]), decode(tokens[2])));
                    }
                }
                default -> {
                }
            }
        }
        return new FullItemRead(itemName, itemTypeLine, rarity, itemPower, baseItemValue, lines);
    }

    private static String encodeAffixes(List<ImportedItemAffix> affixes) {
        List<String> payloadLines = new ArrayList<>();
        for (ImportedItemAffix affix : affixes) {
            payloadLines.add(String.join("|",
                    affix.getType().name(),
                    formatDouble(affix.getValue()),
                    encode(affix.getUnit()),
                    Boolean.toString(affix.isGreaterAffix()),
                    Integer.toString(affix.getDisplayOrder()),
                    encode(affix.getRawOcrLine()),
                    affix.getSource().name()
            ));
        }
        return encode(String.join("\n", payloadLines));
    }

    private static List<ImportedItemAffix> decodeAffixes(String encodedPayload) {
        String payload = decode(encodedPayload);
        List<ImportedItemAffix> affixes = new ArrayList<>();
        for (String line : payload.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] tokens = line.split("\\|", -1);
            if (tokens.length < 2) {
                continue;
            }
            ImportedItemAffixType type = ImportedItemAffixType.valueOf(tokens[0]);
            double value = Double.parseDouble(tokens[1]);
            if (tokens.length >= 7) {
                affixes.add(new ImportedItemAffix(
                        type,
                        value,
                        decode(tokens[2]),
                        Boolean.parseBoolean(tokens[3]),
                        Integer.parseInt(tokens[4]),
                        decode(tokens[5]),
                        ImportedItemAffixSource.valueOf(tokens[6])
                ));
            } else {
                affixes.add(new ImportedItemAffix(type, value, tokens.length >= 3 ? decode(tokens[2]) : ""));
            }
        }
        return affixes;
    }
}
