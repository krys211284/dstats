package krys.web;

import krys.itemimport.CurrentBuildImportableStats;
import krys.hero.HeroClass;
import krys.runtime.RuntimeSkillLoadout;
import java.util.Map;
import krys.itemlibrary.CurrentHeroActiveItemStats;
import krys.itemlibrary.EffectiveCurrentBuildResolution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerCurrentBuildInputResolverTest {
    private final NecromancerCurrentBuildInputResolver resolver = new NecromancerCurrentBuildInputResolver();
    private final HeroProfile hero = new HeroProfile(1, "Mortis", HeroClass.NECROMANCER, "level=70", HeroItemSelection.empty(), new RuntimeSkillLoadout("necromancer", Map.of(), List.of()));

    @Test
    void shouldUseOnlyWeaponAndItemPlusNonItemIntelligenceWithZeroUnsupportedBuckets() {
        var contribution = new CurrentBuildImportableStats(1000, 100, 100, 100, 100, 100, 100);
        var resolution = new EffectiveCurrentBuildResolution(contribution, List.of(), contribution, CurrentHeroActiveItemStats.empty(), contribution);
        var profile = resolver.resolve(hero, new NecromancerCurrentBuildFormData("821", true, "krwotok"), resolution);
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
        assertTrue(assertThrows(IllegalArgumentException.class, () -> resolver.resolve(hero, new NecromancerCurrentBuildFormData("900", false, "krwotok"), resolution)).getMessage().contains("Wybierz aktywną broń"));
        var large = new CurrentBuildImportableStats(1000, 0, Double.MAX_VALUE, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(hero, new NecromancerCurrentBuildFormData(Double.toString(Double.MAX_VALUE), false, "krwotok"),
                new EffectiveCurrentBuildResolution(zero, List.of(), large, large)));
    }
}
