package krys.web;

import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.CurrentHeroActiveItemStats;
import krys.itemlibrary.EffectiveCurrentBuildResolution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerCurrentBuildInputResolverTest {
    private final NecromancerCurrentBuildInputResolver resolver = new NecromancerCurrentBuildInputResolver();

    @Test
    void shouldUseOnlyWeaponAndItemPlusNonItemIntelligenceWithZeroUnsupportedBuckets() {
        var contribution = new CurrentBuildImportableStats(1000, 100, 100, 100, 100, 100, 100);
        var resolution = new EffectiveCurrentBuildResolution(contribution, List.of(), contribution, CurrentHeroActiveItemStats.empty(), contribution);
        var profile = resolver.resolve(new NecromancerCurrentBuildFormData("900", true, "krwotok"), resolution);
        assertEquals(1000, profile.averageWeaponDamage());
        assertEquals(1000, profile.intelligence());
        assertTrue(profile.targetVulnerable());
        assertEquals(0, profile.additiveDamagePercent());
        assertEquals(0, profile.damageBucketPercent());
        assertEquals(1, profile.resolvedSeparateMultiplicativeMultiplier());
        assertEquals(0, profile.criticalStrikeDamageAdditivePercent());
        assertEquals(0, profile.criticalStrikeDamageMultiplierPercent());
        assertEquals(0, profile.vulnerableDamageAdditivePercent());
        assertEquals(0, profile.vulnerableDamageMultiplierPercent());
    }

    @Test
    void shouldRejectAbsentWeaponAndNonFiniteEffectiveIntelligence() {
        var zero = new CurrentBuildImportableStats(0, 0, 0, 0, 0, 0);
        var resolution = new EffectiveCurrentBuildResolution(zero, List.of(), zero, zero);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> resolver.resolve(new NecromancerCurrentBuildFormData("900", false, "krwotok"), resolution)).getMessage().contains("Wybierz aktywną broń"));
        var large = new CurrentBuildImportableStats(1000, 0, Double.MAX_VALUE, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(new NecromancerCurrentBuildFormData(Double.toString(Double.MAX_VALUE), false, "krwotok"),
                new EffectiveCurrentBuildResolution(zero, List.of(), large, large)));
    }
}
