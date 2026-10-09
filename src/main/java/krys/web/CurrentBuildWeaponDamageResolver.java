package krys.web;

import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.CurrentHeroActiveItemStats;

/** Wspólna kolejność źródeł średnich obrażeń aktywnej broni, bez reguł klasy. */
final class CurrentBuildWeaponDamageResolver {
    private CurrentBuildWeaponDamageResolver() { }

    static long resolve(CurrentHeroActiveItemStats activeItemStats, CurrentBuildImportableStats contribution) {
        Long average = activeItemStats.getAverageWeaponDamage();
        if (average != null && average > 0L) {
            return average;
        }
        return contribution.getWeaponDamage() > 0L ? contribution.getWeaponDamage() : 0L;
    }
}
