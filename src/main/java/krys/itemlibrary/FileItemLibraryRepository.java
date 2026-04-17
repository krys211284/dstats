package krys.itemlibrary;

import krys.item.EquipmentSlot;

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

/** Trwałe repozytorium biblioteki itemów oparte o proste pliki tekstowe w katalogu roboczym projektu. */
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
                    item.getRetributionChance()
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
        if (tokens.length != 11 || !ITEM_PREFIX.equals(tokens[0])) {
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
                Double.parseDouble(tokens[10])
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
                    formatDouble(item.getRetributionChance())
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
}
