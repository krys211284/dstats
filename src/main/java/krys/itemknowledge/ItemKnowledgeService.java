package krys.itemknowledge;

import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ValidatedImportedItem;

import java.text.Normalizer;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Warstwa aplikacyjna bazy wiedzy uczącej się wyłącznie z ręcznie zatwierdzonych itemów. */
public final class ItemKnowledgeService {
    private final ItemKnowledgeRepository repository;

    public ItemKnowledgeService(ItemKnowledgeRepository repository) {
        this.repository = repository;
    }

    public ItemKnowledgeSnapshot getSnapshot() {
        return repository.load();
    }

    public ItemKnowledgeSnapshot learnFromConfirmedItem(ValidatedImportedItem importedItem, FullItemRead fullItemRead) {
        if (importedItem == null) {
            throw new IllegalArgumentException("Zatwierdzony item jest wymagany do uczenia bazy wiedzy.");
        }
        FullItemRead safeRead = fullItemRead == null ? FullItemRead.empty() : fullItemRead;
        ItemKnowledgeSnapshot snapshot = repository.load();
        ItemKnowledgeKey key = new ItemKnowledgeKey(importedItem.getSlot(), resolveItemType(importedItem.getSlot(), safeRead));
        Map<ItemKnowledgeKey, ItemKnowledgeEntry> entriesByKey = new LinkedHashMap<>();
        for (ItemKnowledgeEntry entry : snapshot.getEntries()) {
            entriesByKey.put(entry.getKey(), entry);
        }
        ItemKnowledgeEntry updatedEntry = entriesByKey
                .getOrDefault(key, ItemKnowledgeEntry.empty(key))
                .withObservation(affixObservationCounts(importedItem), aspectObservationCounts(safeRead));
        entriesByKey.put(key, updatedEntry);

        ItemKnowledgeSnapshot updatedSnapshot = new ItemKnowledgeSnapshot(snapshot.getActiveEpoch(), entriesByKey.values().stream().toList());
        repository.save(updatedSnapshot);
        return updatedSnapshot;
    }

    public ItemKnowledgeSnapshot resetKnowledge(String requestedEpochLabel) {
        ItemKnowledgeSnapshot snapshot = repository.load();
        ItemKnowledgeSnapshot resetSnapshot = new ItemKnowledgeSnapshot(snapshot.getActiveEpoch().next(requestedEpochLabel), java.util.List.of());
        repository.save(resetSnapshot);
        return resetSnapshot;
    }

    private static Map<ImportedItemAffixType, Integer> affixObservationCounts(ValidatedImportedItem importedItem) {
        EnumMap<ImportedItemAffixType, Integer> counts = new EnumMap<>(ImportedItemAffixType.class);
        for (ImportedItemAffix affix : importedItem.getAffixes()) {
            counts.merge(affix.getType(), 1, Integer::sum);
        }
        return counts;
    }

    private static Map<String, Integer> aspectObservationCounts(FullItemRead fullItemRead) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (FullItemReadLine line : fullItemRead.getLines()) {
            if (line.getType() == FullItemReadLineType.ASPECT && line.getText() != null && !line.getText().isBlank()) {
                counts.merge(line.getText(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static String resolveItemType(EquipmentSlot slot, FullItemRead fullItemRead) {
        String typeLine = fullItemRead.getItemTypeLine();
        String normalized = normalize(typeLine);
        if (normalized.contains("TARCZA")) {
            return "Tarcza";
        }
        if (normalized.contains("BUTY")) {
            return "Buty";
        }
        if (normalized.contains("BRON GLOWNA") || slot == EquipmentSlot.MAIN_HAND) {
            return "Broń główna";
        }
        if (typeLine != null && !typeLine.isBlank()) {
            return typeLine;
        }
        return slot.name();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(java.util.Locale.ROOT);
    }
}
