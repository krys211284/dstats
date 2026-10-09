package krys.web;

import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.CurrentHeroActiveItemStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrentBuildWeaponDamageResolverTest {
    @Test
    void shouldPreferAverageThenPositiveContributionAndOtherwiseZeroWithoutUsingDpsOrMinMax() {
        var contribution = new CurrentBuildImportableStats(1000, 0, 0, 0, 0, 0);
        var stats = new CurrentHeroActiveItemStats(3150L, 2100L, 3780L, 1664L, 1.1, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of());
        assertEquals(1664, CurrentBuildWeaponDamageResolver.resolve(stats, contribution));
        assertEquals(1000, CurrentBuildWeaponDamageResolver.resolve(CurrentHeroActiveItemStats.empty(), contribution));
        var zeroAverage = new CurrentHeroActiveItemStats(3150L, 2100L, 3780L, 0L, 1.1, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of());
        assertEquals(1000, CurrentBuildWeaponDamageResolver.resolve(zeroAverage, contribution));
        assertEquals(0, CurrentBuildWeaponDamageResolver.resolve(zeroAverage, new CurrentBuildImportableStats(0, 0, 0, 0, 0, 0)));
    }
}
