package krys.itemimport;

/** Mapuje zatwierdzony item do agregowanych pól aktualnego modelu buildu. */
public final class ImportedItemCurrentBuildContributionMapper {
    public ImportedItemCurrentBuildContribution map(ValidatedImportedItem importedItem) {
        return new ImportedItemCurrentBuildContribution(
                importedItem.getWeaponDamage(),
                importedItem.getStrength(),
                importedItem.getIntelligence(),
                importedItem.getThorns(),
                importedItem.getBlockChance(),
                importedItem.getRetributionChance()
        );
    }
}
