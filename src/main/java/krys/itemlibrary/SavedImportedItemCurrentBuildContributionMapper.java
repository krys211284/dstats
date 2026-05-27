package krys.itemlibrary;

import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.masterworking.MasterworkingResolvedItemValueResolver;

/** Mapuje zapisany item z biblioteki do agregowanego wkładu current build. */
public final class SavedImportedItemCurrentBuildContributionMapper {
    private final MasterworkingResolvedItemValueResolver masterworkingValueResolver = new MasterworkingResolvedItemValueResolver();

    public ImportedItemCurrentBuildContribution map(SavedImportedItem item) {
        return new ImportedItemCurrentBuildContribution(
                item.getWeaponDamage(),
                effectiveScalarContribution(item, ImportedItemAffixType.STRENGTH, item.getStrength()),
                effectiveScalarContribution(item, ImportedItemAffixType.INTELLIGENCE, item.getIntelligence()),
                effectiveScalarContribution(item, ImportedItemAffixType.THORNS, item.getThorns()),
                effectiveScalarContribution(item, ImportedItemAffixType.BLOCK_CHANCE, item.getBlockChance()),
                effectiveScalarContribution(item, ImportedItemAffixType.RETRIBUTION_CHANCE, item.getRetributionChance()),
                effectiveScalarContribution(item, ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 0.0d)
        );
    }

    private double effectiveScalarContribution(SavedImportedItem item,
                                               ImportedItemAffixType type,
                                               double sourceContribution) {
        double value = 0.0d;
        boolean found = false;
        for (ImportedItemAffix affix : item.getAffixes()) {
            if (affix.getType() != type) {
                continue;
            }
            found = true;
            value += masterworkingValueResolver.resolveAffixValue(affix, item.getMasterworking());
        }
        return found ? value : sourceContribution;
    }
}
