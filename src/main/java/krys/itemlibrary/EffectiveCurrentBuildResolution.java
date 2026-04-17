package krys.itemlibrary;

import krys.itemimport.CurrentBuildImportableStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Wynik złożenia ręcznej bazy current build z aktywnymi itemami biblioteki. */
public final class EffectiveCurrentBuildResolution {
    private final CurrentBuildImportableStats manualBaseStats;
    private final List<SavedImportedItem> activeItems;
    private final CurrentBuildImportableStats activeItemsContribution;
    private final CurrentBuildImportableStats effectiveStats;

    public EffectiveCurrentBuildResolution(CurrentBuildImportableStats manualBaseStats,
                                           List<SavedImportedItem> activeItems,
                                           CurrentBuildImportableStats activeItemsContribution,
                                           CurrentBuildImportableStats effectiveStats) {
        this.manualBaseStats = manualBaseStats;
        this.activeItems = Collections.unmodifiableList(new ArrayList<>(activeItems));
        this.activeItemsContribution = activeItemsContribution;
        this.effectiveStats = effectiveStats;
    }

    public CurrentBuildImportableStats getManualBaseStats() {
        return manualBaseStats;
    }

    public List<SavedImportedItem> getActiveItems() {
        return activeItems;
    }

    public CurrentBuildImportableStats getActiveItemsContribution() {
        return activeItemsContribution;
    }

    public CurrentBuildImportableStats getEffectiveStats() {
        return effectiveStats;
    }
}
