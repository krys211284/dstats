package krys.itemlibrary;

import krys.itemimport.ImportedItemCurrentBuildContribution;

/** Mapuje zapisany item z biblioteki do agregowanego wkładu current build. */
public final class SavedImportedItemCurrentBuildContributionMapper {
    public ImportedItemCurrentBuildContribution map(SavedImportedItem item) {
        return new ImportedItemCurrentBuildContribution(
                item.getWeaponDamage(),
                item.getStrength(),
                item.getIntelligence(),
                item.getThorns(),
                item.getBlockChance(),
                item.getRetributionChance()
        );
    }
}
