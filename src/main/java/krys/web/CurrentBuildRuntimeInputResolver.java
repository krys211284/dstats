package krys.web;

import krys.hero.HeroClassStatBaseline;
import krys.hero.HeroClassStatBaselines;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.CurrentHeroActiveItemStats;
import krys.itemlibrary.EffectiveCurrentBuildResolution;

import java.util.Optional;

/** Buduje jawne statystyki wejściowe runtime dla current build na podstawie bohatera i aktywnych itemów. */
final class CurrentBuildRuntimeInputResolver {
    CurrentBuildImportableStats resolve(HeroProfile activeHero,
                                        CurrentBuildFormData formData,
                                        EffectiveCurrentBuildResolution libraryResolution) {
        if (activeHero == null || libraryResolution == null || libraryResolution.getEffectiveStats() == null) {
            return null;
        }

        CurrentBuildImportableStats legacyEffectiveStats = libraryResolution.getEffectiveStats();
        CurrentHeroActiveItemStats activeItemStats = libraryResolution.getActiveHeroItemStats();
        Optional<HeroClassStatBaseline> baseline = HeroClassStatBaselines.find(activeHero.getHeroClass(), parseLevel(formData));

        long weaponDamage = resolveWeaponDamage(legacyEffectiveStats, activeItemStats);
        double strength = baseline
                .map(value -> value.getStrength() + libraryResolution.getActiveItemsContribution().getStrength())
                .orElse(legacyEffectiveStats.getStrength());
        double intelligence = baseline
                .map(value -> value.getIntelligence() + libraryResolution.getActiveItemsContribution().getIntelligence())
                .orElse(legacyEffectiveStats.getIntelligence());
        double thorns = baseline
                .map(value -> value.getThorns() + libraryResolution.getActiveItemsContribution().getThorns())
                .orElse(legacyEffectiveStats.getThorns());
        double blockChance = baseline
                .map(value -> libraryResolution.getActiveItemsContribution().getBlockChance())
                .orElse(legacyEffectiveStats.getBlockChance());
        double retributionChance = baseline
                .map(value -> libraryResolution.getActiveItemsContribution().getRetributionChance())
                .orElse(legacyEffectiveStats.getRetributionChance());

        return new CurrentBuildImportableStats(
                weaponDamage,
                strength,
                intelligence,
                thorns,
                blockChance,
                retributionChance
        );
    }

    private static long resolveWeaponDamage(CurrentBuildImportableStats legacyEffectiveStats,
                                            CurrentHeroActiveItemStats activeItemStats) {
        Long averageWeaponDamage = activeItemStats.getAverageWeaponDamage();
        if (averageWeaponDamage != null && averageWeaponDamage > 0L) {
            return averageWeaponDamage;
        }
        return legacyEffectiveStats.getWeaponDamage();
    }

    private static int parseLevel(CurrentBuildFormData formData) {
        try {
            return Math.max(1, Integer.parseInt(formData.getLevel()));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }
}
