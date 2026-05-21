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

        CurrentHeroActiveItemStats activeItemStats = libraryResolution.getActiveHeroItemStats();
        Optional<HeroClassStatBaseline> baseline = HeroClassStatBaselines.find(activeHero.getHeroClass(), parseLevel(formData));

        long weaponDamage = resolveWeaponDamage(activeItemStats, libraryResolution.getActiveItemsContribution());
        double strength = baseline.map(HeroClassStatBaseline::getStrength).orElse(0) + libraryResolution.getActiveItemsContribution().getStrength();
        double intelligence = baseline.map(HeroClassStatBaseline::getIntelligence).orElse(0) + libraryResolution.getActiveItemsContribution().getIntelligence();
        double thorns = baseline.map(HeroClassStatBaseline::getThorns).orElse(0) + libraryResolution.getActiveItemsContribution().getThorns();
        double blockChance = libraryResolution.getActiveItemsContribution().getBlockChance();
        double retributionChance = libraryResolution.getActiveItemsContribution().getRetributionChance();

        return new CurrentBuildImportableStats(
                weaponDamage,
                strength,
                intelligence,
                thorns,
                blockChance,
                retributionChance
        );
    }

    CurrentBuildFormData applyRuntimeResourceBonuses(CurrentBuildFormData formData,
                                                     EffectiveCurrentBuildResolution libraryResolution) {
        return CurrentBuildFormQuerySupport.withMaxAnimus(
                formData,
                formatMaxAnimus(resolveMaximumAnimus(formData, libraryResolution))
        );
    }

    static double resolveMaximumAnimus(CurrentBuildFormData formData,
                                       EffectiveCurrentBuildResolution libraryResolution) {
        double baseMaxAnimus = parseDouble(formData == null ? null : formData.getMaxAnimus());
        if (libraryResolution == null || libraryResolution.getActiveHeroItemStats() == null) {
            return baseMaxAnimus;
        }
        return baseMaxAnimus + libraryResolution.getActiveHeroItemStats().getMaxAnimusFromTempering();
    }

    private static long resolveWeaponDamage(CurrentHeroActiveItemStats activeItemStats,
                                            CurrentBuildImportableStats activeItemsContribution) {
        Long averageWeaponDamage = activeItemStats.getAverageWeaponDamage();
        if (averageWeaponDamage != null && averageWeaponDamage > 0L) {
            return averageWeaponDamage;
        }
        if (activeItemsContribution.getWeaponDamage() > 0L) {
            return activeItemsContribution.getWeaponDamage();
        }
        return 0L;
    }

    private static int parseLevel(CurrentBuildFormData formData) {
        try {
            return Math.max(1, Integer.parseInt(formData.getLevel()));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException exception) {
            return 0.0d;
        }
    }

    private static String formatMaxAnimus(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }
}
