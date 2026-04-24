package krys.itemlibrary;

import krys.itemimport.CurrentBuildImportableStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
            labels.add(item.getSlot().name() + ": " + item.getDisplayName());
        }
        return String.join(" || ", labels);
    }

    public String getContributionDescription() {
        List<String> labels = new ArrayList<>();
        if (totalContribution.getWeaponDamage() > 0L) {
            labels.add("weapon=" + totalContribution.getWeaponDamage());
        }
        if (totalContribution.getStrength() > 0.0d) {
            labels.add("str=" + formatWhole(totalContribution.getStrength()));
        }
        if (totalContribution.getIntelligence() > 0.0d) {
            labels.add("int=" + formatWhole(totalContribution.getIntelligence()));
        }
        if (totalContribution.getThorns() > 0.0d) {
            labels.add("thorns=" + formatWhole(totalContribution.getThorns()));
        }
        if (totalContribution.getBlockChance() > 0.0d) {
            labels.add("block=" + formatWhole(totalContribution.getBlockChance()) + "%");
        }
        if (totalContribution.getRetributionChance() > 0.0d) {
            labels.add("retribution=" + formatWhole(totalContribution.getRetributionChance()) + "%");
        }
        return labels.isEmpty() ? "Brak wkładu" : String.join(", ", labels);
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

    private static String formatWhole(double value) {
        return String.format(Locale.US, "%.0f", value);
    }
}
