package krys.itemimport;

/** Aplikuje wkład zatwierdzonego itemu do już istniejących statów current build. */
public final class ImportedItemCurrentBuildApplicationService {
    public CurrentBuildImportableStats apply(CurrentBuildImportableStats baseStats,
                                             ImportedItemCurrentBuildContribution contribution,
                                             CurrentBuildItemApplicationMode mode) {
        if (mode == CurrentBuildItemApplicationMode.ADD_CONTRIBUTION) {
            return new CurrentBuildImportableStats(
                    applyAdditiveLong(baseStats.getWeaponDamage(), contribution.getWeaponDamage()),
                    applyAdditiveDouble(baseStats.getStrength(), contribution.getStrength()),
                    applyAdditiveDouble(baseStats.getIntelligence(), contribution.getIntelligence()),
                    applyAdditiveDouble(baseStats.getThorns(), contribution.getThorns()),
                    applyAdditiveDouble(baseStats.getBlockChance(), contribution.getBlockChance()),
                    applyAdditiveDouble(baseStats.getRetributionChance(), contribution.getRetributionChance())
            );
        }

        return new CurrentBuildImportableStats(
                applyOverwriteLong(baseStats.getWeaponDamage(), contribution.getWeaponDamage()),
                applyOverwriteDouble(baseStats.getStrength(), contribution.getStrength()),
                applyOverwriteDouble(baseStats.getIntelligence(), contribution.getIntelligence()),
                applyOverwriteDouble(baseStats.getThorns(), contribution.getThorns()),
                applyOverwriteDouble(baseStats.getBlockChance(), contribution.getBlockChance()),
                applyOverwriteDouble(baseStats.getRetributionChance(), contribution.getRetributionChance())
        );
    }

    private static long applyAdditiveLong(long baseValue, long contributionValue) {
        return contributionValue > 0L ? baseValue + contributionValue : baseValue;
    }

    private static double applyAdditiveDouble(double baseValue, double contributionValue) {
        return contributionValue > 0.0d ? baseValue + contributionValue : baseValue;
    }

    private static long applyOverwriteLong(long baseValue, long contributionValue) {
        return contributionValue > 0L ? contributionValue : baseValue;
    }

    private static double applyOverwriteDouble(double baseValue, double contributionValue) {
        return contributionValue > 0.0d ? contributionValue : baseValue;
    }
}
