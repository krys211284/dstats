package krys.itemknowledge;

import krys.itemimport.ImportedItemAffixType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Zagregowane obserwacje affixów i aspektów dla jednego typu itemu w aktywnej epoce wiedzy. */
public final class ItemKnowledgeEntry {
    private final ItemKnowledgeKey key;
    private final int itemObservationCount;
    private final Map<ImportedItemAffixType, Integer> affixTypeCounts;
    private final Map<String, Integer> aspectCounts;

    public ItemKnowledgeEntry(ItemKnowledgeKey key,
                              int itemObservationCount,
                              Map<ImportedItemAffixType, Integer> affixTypeCounts,
                              Map<String, Integer> aspectCounts) {
        if (key == null) {
            throw new IllegalArgumentException("Klucz wpisu wiedzy jest wymagany.");
        }
        if (itemObservationCount < 0) {
            throw new IllegalArgumentException("Licznik obserwacji itemów nie może być ujemny.");
        }
        this.key = key;
        this.itemObservationCount = itemObservationCount;
        this.affixTypeCounts = copyAffixCounts(affixTypeCounts);
        this.aspectCounts = copyAspectCounts(aspectCounts);
    }

    public static ItemKnowledgeEntry empty(ItemKnowledgeKey key) {
        return new ItemKnowledgeEntry(key, 0, Map.of(), Map.of());
    }

    public ItemKnowledgeEntry withObservation(Map<ImportedItemAffixType, Integer> observedAffixes,
                                              Map<String, Integer> observedAspects) {
        Map<ImportedItemAffixType, Integer> updatedAffixes = copyAffixCounts(affixTypeCounts);
        for (Map.Entry<ImportedItemAffixType, Integer> entry : observedAffixes.entrySet()) {
            updatedAffixes.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        Map<String, Integer> updatedAspects = copyAspectCounts(aspectCounts);
        for (Map.Entry<String, Integer> entry : observedAspects.entrySet()) {
            updatedAspects.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        return new ItemKnowledgeEntry(key, itemObservationCount + 1, updatedAffixes, updatedAspects);
    }

    public ItemKnowledgeKey getKey() {
        return key;
    }

    public int getItemObservationCount() {
        return itemObservationCount;
    }

    public Map<ImportedItemAffixType, Integer> getAffixTypeCounts() {
        return Map.copyOf(affixTypeCounts);
    }

    public Map<String, Integer> getAspectCounts() {
        return Map.copyOf(aspectCounts);
    }

    public int getTotalAffixObservations() {
        return affixTypeCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalAspectObservations() {
        return aspectCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static Map<ImportedItemAffixType, Integer> copyAffixCounts(Map<ImportedItemAffixType, Integer> counts) {
        EnumMap<ImportedItemAffixType, Integer> copy = new EnumMap<>(ImportedItemAffixType.class);
        if (counts == null) {
            return copy;
        }
        for (Map.Entry<ImportedItemAffixType, Integer> entry : counts.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }

    private static Map<String, Integer> copyAspectCounts(Map<String, Integer> counts) {
        LinkedHashMap<String, Integer> copy = new LinkedHashMap<>();
        if (counts == null) {
            return copy;
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null && entry.getValue() > 0) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }
}
