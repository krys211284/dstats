package krys.itemlibrary;

import krys.itemimport.CurrentBuildImportableStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reprezentuje pojedynczą kombinację searcha złożoną z co najwyżej jednego zapisanego itemu per slot. */
public final class ItemLibrarySearchCombination {
    private final List<HeroSlotItemAssignment> selectedItems;
    private final CurrentBuildImportableStats totalContribution;

    public ItemLibrarySearchCombination(List<HeroSlotItemAssignment> selectedItems,
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

    public List<HeroSlotItemAssignment> getSelectedItems() {
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
        for (HeroSlotItemAssignment assignment : selectedItems) {
            labels.add(ItemLibraryPresentationSupport.heroSlotDisplayName(assignment.getHeroSlot())
                    + ": "
                    + ItemLibraryPresentationSupport.userItemIdentifier(assignment.getItem()));
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
        for (HeroSlotItemAssignment assignment : selectedItems) {
            labels.add(assignment.getHeroSlot().name() + "#" + assignment.getItem().getItemId());
        }
        return String.join("|", labels);
    }

}
