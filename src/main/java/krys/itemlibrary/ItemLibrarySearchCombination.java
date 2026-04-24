package krys.itemlibrary;

import krys.itemimport.CurrentBuildImportableStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reprezentuje pojedynczą kombinację searcha złożoną z co najwyżej jednego zapisanego itemu per slot. */
public final class ItemLibrarySearchCombination {
    private final List<SavedImportedItem> selectedItems;
    private final CurrentBuildImportableStats totalContribution;

    public ItemLibrarySearchCombination(List<SavedImportedItem> selectedItems,
                                        CurrentBuildImportableStats totalContribution) {
        this.selectedItems = Collections.unmodifiableList(new ArrayList<>(selectedItems));
        this.totalContribution = totalContribution;
    }

    public static ItemLibrarySearchCombination empty() {
        return new ItemLibrarySearchCombination(
                List.of(),
                new CurrentBuildImportableStats(0L, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d)
        );
    }

    public List<SavedImportedItem> getSelectedItems() {
        return selectedItems;
    }

    public CurrentBuildImportableStats getTotalContribution() {
        return totalContribution;
    }

    public boolean isEmpty() {
        return selectedItems.isEmpty();
    }

    public String getSelectedItemsDescription() {
        if (selectedItems.isEmpty()) {
            return "Brak";
        }

        List<String> labels = new ArrayList<>();
        for (SavedImportedItem item : selectedItems) {
            labels.add(ItemLibraryPresentationSupport.slotDisplayName(item.getSlot()) + ": " + ItemLibraryPresentationSupport.userItemIdentifier(item));
        }
        return String.join(" || ", labels);
    }

    public String getContributionDescription() {
        return ItemLibraryPresentationSupport.contributionLabel(totalContribution);
    }

    public String toDeterministicKey() {
        if (selectedItems.isEmpty()) {
            return "EMPTY";
        }

        List<String> labels = new ArrayList<>();
        for (SavedImportedItem item : selectedItems) {
            labels.add(item.getSlot().name() + "#" + item.getItemId());
        }
        return String.join("|", labels);
    }

}
