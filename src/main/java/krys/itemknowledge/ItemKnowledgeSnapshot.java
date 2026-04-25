package krys.itemknowledge;

import java.util.Comparator;
import java.util.List;

/** Pełny snapshot aktywnej bazy wiedzy o itemach. */
public final class ItemKnowledgeSnapshot {
    private final ItemKnowledgeEpoch activeEpoch;
    private final List<ItemKnowledgeEntry> entries;

    public ItemKnowledgeSnapshot(ItemKnowledgeEpoch activeEpoch, List<ItemKnowledgeEntry> entries) {
        this.activeEpoch = activeEpoch == null ? new ItemKnowledgeEpoch(1, "Epoka wiedzy 1") : activeEpoch;
        this.entries = entries == null ? List.of() : entries.stream()
                .sorted(Comparator.comparing((ItemKnowledgeEntry entry) -> entry.getKey().slot().name())
                        .thenComparing(entry -> entry.getKey().itemType()))
                .toList();
    }

    public static ItemKnowledgeSnapshot empty() {
        return new ItemKnowledgeSnapshot(new ItemKnowledgeEpoch(1, "Epoka wiedzy 1"), List.of());
    }

    public ItemKnowledgeEpoch getActiveEpoch() {
        return activeEpoch;
    }

    public List<ItemKnowledgeEntry> getEntries() {
        return entries;
    }

    public int getEntryCount() {
        return entries.size();
    }

    public int getItemObservationCount() {
        return entries.stream().mapToInt(ItemKnowledgeEntry::getItemObservationCount).sum();
    }

    public int getAffixObservationCount() {
        return entries.stream().mapToInt(ItemKnowledgeEntry::getTotalAffixObservations).sum();
    }

    public int getAspectObservationCount() {
        return entries.stream().mapToInt(ItemKnowledgeEntry::getTotalAspectObservations).sum();
    }
}
