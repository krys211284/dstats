package krys.necromancer.combat;

import krys.combat.DirectDamageCalculator;
import krys.combat.DirectDamageContext;
import krys.combat.DirectDamageResult;
import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.skilltree.SkillTreeStatus;
import krys.skilltree.UpgradeDamageSafety;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerSeason15DirectDamageResolverTest {
    private static final double EPSILON = 1.0e-8d;
    private static final RuntimeSkillKey HEMORRHAGE = new RuntimeSkillKey("necromancer", "krwotok");
    private final NecromancerSeason15DirectDamageResolver resolver = new NecromancerSeason15DirectDamageResolver();

    @Test
    void goldenAShouldCalculateMinimalSeason15Hit() {
        var result = calculate(minimal(1000, false));
        assertDamage(result, 2100, 3150);
        var component = result.components().getFirst();
        assertEquals(800.0d, component.baseDamageExact(), EPSILON);
        assertEquals(800L, component.baseDamage());
        assertEquals(2100.0d, component.rawDamageExact(), EPSILON);
        assertEquals(3150.0d, component.rawCriticalDamageExact(), EPSILON);
        assertEquals(2.625d, result.context().mainStatMultiplier(), EPSILON);
        assertEquals(1.0d, result.context().weaponMultiplier());
        assertEquals(1.0d, result.context().postMitigationMultiplier());
        assertEquals(DirectDamageContext.CriticalTotalRoundingPolicy.EXACT_COMPONENT_SUM,
                result.context().criticalTotalRoundingPolicy());
    }

    @Test
    void goldenBShouldApplyBaseVulnerableFactor() {
        var result = calculate(minimal(1000, true));
        assertDamage(result, 2520, 3780);
        assertEquals(1.2d, result.context().vulnerableMultiplier(), EPSILON);
    }

    @Test
    void goldenCShouldResolveAllModernBuckets() {
        var profile = new NecromancerSeason15DirectDamageProfile(1000, 1000,
                300, 40, 1.30d, 100, 60, 0, 25, true);
        var result = calculate(profile);
        assertDamage(result, 22932, 68796);
        assertEquals(800.0d, result.components().getFirst().baseDamageExact(), EPSILON);
        assertEquals(4.0d, result.context().additiveMultiplier(), EPSILON);
        assertEquals(1.4d * 1.3d, result.context().multiplicativeMultiplier(), EPSILON);
        assertEquals(1.5d, result.context().vulnerableMultiplier(), EPSILON);
        assertEquals(3.0d, result.context().criticalMultiplier(), EPSILON);
    }

    @Test
    void goldenDShouldIgnoreBothVulnerableBonusesAgainstNormalTarget() {
        var withBonuses = new NecromancerSeason15DirectDamageProfile(1000, 1000,
                0, 0, 1, 0, 0, 100, 25, false);
        assertDamage(calculate(withBonuses), 2100, 3150);
        assertEquals(calculate(minimal(1000, false)), calculate(withBonuses));
    }

    @Test
    void goldenDShouldAddVulnerableBonusOnlyWhenTargetIsVulnerable() {
        var profile = new NecromancerSeason15DirectDamageProfile(1000, 1000,
                0, 0, 1, 0, 0, 100, 0, true);
        // Golden B z additive +100%: 2520 × 2 oraz 3780 × 2.
        assertDamage(calculate(profile), 5040, 7560);
        assertEquals(2.0d, resolver.resolveContext(profile).additiveMultiplier());
    }

    @Test
    void goldenDShouldAddCriticalBonusOnlyToCriticalHit() {
        var profile = new NecromancerSeason15DirectDamageProfile(1000, 1000,
                0, 0, 1, 100, 0, 0, 0, false);
        // Golden A: normal pozostaje 2100; kryt 3150 × (1 + 1) / 1.
        assertDamage(calculate(profile), 2100, 6300);
        assertEquals(1.0d, resolver.resolveContext(profile).additiveMultiplier());
        assertEquals(3.0d, resolver.resolveContext(profile).criticalMultiplier());
    }

    @Test
    void conditionalCriticalAdditiveShouldShareBucketWithVulnerableAdditive() {
        var profile = new NecromancerSeason15DirectDamageProfile(1000, 1000,
                0, 0, 1, 100, 0, 100, 0, true);
        // A=2, krytyczny additive=3; 2520 × 2 = 5040, 3780 × 3 = 11340.
        assertDamage(calculate(profile), 5040, 11340);
        assertEquals(2.25d, resolver.resolveContext(profile).criticalMultiplier(), EPSILON);
    }

    @ParameterizedTest
    @CsvSource({"0, 1.0", "100, 1.1625", "1000, 2.625"})
    void intelligenceShouldUseOfficialSeason15ScalarWithoutCriticalBonus(double intelligence, double expected) {
        var context = resolver.resolveContext(minimal(intelligence, false));
        assertEquals(expected, context.mainStatMultiplier(), EPSILON);
        assertEquals(1.5d, context.criticalMultiplier());
    }

    @Test
    void componentShouldReadCoefficientAndMetadataFromUnchangedCatalog() {
        var skill = NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill("krwotok");
        var result = calculate(minimal(1000, false));
        assertEquals(1, result.components().size());
        var spec = result.components().getFirst().spec();
        assertEquals("Krwotok — bazowe trafienie", spec.name());
        assertEquals(skill.getSourceReference(), spec.source());
        assertEquals(new BigDecimal("80"), spec.damagePercent());
        assertEquals(1, spec.hitCount());
        assertNull(spec.conditionLabel());
        assertTrue(spec.active());
        assertTrue(spec.includedInSingleTarget());
        assertNull(spec.exclusionReason());
        assertEquals(80, skill.getBaseDamagePercentRanks().damagePercentAtRank(1));
        assertEquals(204, skill.getBaseDamagePercentRanks().damagePercentAtRank(15));
        assertEquals(50, skill.getLuckyHitPercent());
        assertEquals(new BigDecimal("13"), skill.getResourceData().generationMin());
        assertEquals(new BigDecimal("13"), skill.getResourceData().generationMax());
        assertEquals(7, skill.getUpgradeGroups().stream().mapToInt(group -> group.getUpgrades().size()).sum());
        assertEquals(SkillTreeStatus.NEEDS_VERIFICATION, skill.getStatus());
        assertTrue(skill.getUpgradeDamageModifiers().stream()
                .allMatch(modifier -> modifier.getSafeForRuntimeDps() == UpgradeDamageSafety.NO));
    }

    @ParameterizedTest
    @CsvSource({"62.5, 62.5, 63, 93.75, 94", "7.5, 7.5, 8, 11.25, 11"})
    void componentBridgeShouldPreserveDecimalCoefficient(String coefficient, double normalExact,
                                                         long normal, double criticalExact, long critical) {
        var skill = NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill("krwotok");
        var decimal = new BigDecimal(coefficient);
        var spec = NecromancerSeason15DirectDamageResolver.baseHitSpec(skill, decimal);
        assertSame(decimal, spec.damagePercent());
        // Helper-level: neutralny hit bronią 100, INT 0; nie zmienia danych ani wsparcia runtime katalogu.
        var result = new DirectDamageCalculator().calculate(resolver.resolveContext(
                new NecromancerSeason15DirectDamageProfile(100, 0, 0, 0, 1, 0, 0, 0, 0, false)), List.of(spec));
        assertEquals(normalExact, result.rawDamageExact(), EPSILON);
        assertEquals(normal, result.rawDamage());
        assertEquals(criticalExact, result.rawCriticalDamageExact(), EPSILON);
        assertEquals(critical, result.rawCriticalDamage());
        assertEquals(new BigDecimal("80"), calculate(minimal(1000, false)).components().getFirst().spec().damagePercent());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 14, 15, 16})
    void unsupportedRanksShouldBeRejected(int rank) {
        assertThrows(UnsupportedOperationException.class, () -> resolver.calculate(
                new RuntimeSkillConfiguration(HEMORRHAGE, rank, Map.of()), minimal(1000, false)));
    }

    @Test
    void allOtherCatalogSkillsShouldBeRejected() {
        var profile = minimal(1000, false);
        for (var skill : NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills()) {
            if (!"krwotok".equals(skill.getSkillId())) {
                assertThrows(UnsupportedOperationException.class, () -> resolver.calculate(
                        new RuntimeSkillConfiguration(new RuntimeSkillKey("necromancer", skill.getSkillId()), 1, Map.of()), profile));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"paladin, krwotok", "necromancer, wymach", "necromancer, unknown"})
    void unsupportedNamespaceOrSkillShouldBeRejected(String classId, String skillId) {
        var key = new RuntimeSkillKey(classId, skillId);
        assertThrows(UnsupportedOperationException.class, () -> resolver.calculate(
                new RuntimeSkillConfiguration(key, 1, Map.of()), minimal(1000, false)));
    }

    @Test
    void nullConfigurationShouldBeRejected() {
        assertThrows(NullPointerException.class, () -> resolver.calculate(null, minimal(1000, false)));
    }

    @Test
    void oneOrAllThreeCatalogSelectionsShouldBeRejectedInsteadOfIgnored() {
        for (var selections : List.of(Map.of("grupa_1", "szybkosc_uzycia"),
                Map.of("grupa_1", "szybkosc_uzycia", "grupa_2", "przytloczenie", "grupa_3", "krwawy_wrzod"))) {
            assertThrows(UnsupportedOperationException.class, () -> resolver.calculate(
                    new RuntimeSkillConfiguration(HEMORRHAGE, 1, selections), minimal(1000, false)));
        }
    }

    @Test
    void anyNonemptySelectionsShouldBeRejectedWithoutFallbackToBaseHit() {
        assertThrows(UnsupportedOperationException.class, () -> resolver.calculate(
                new RuntimeSkillConfiguration(HEMORRHAGE, 1, Map.of("unknown_slot", "unknown_option")), minimal(1000, false)));
    }

    @Test
    void profileShouldRejectInvalidValuesInEveryNumericField() {
        for (int field = 0; field < 9; field++) {
            for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                var values = new double[]{1000, 1000, 0, 0, 1, 0, 0, 0, 0};
                values[field] = invalid;
                assertThrows(IllegalArgumentException.class, () -> new NecromancerSeason15DirectDamageProfile(
                        values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], values[8], false));
            }
        }
    }

    @Test
    void zeroWeaponAndZeroSeparateMultiplierShouldBeLegal() {
        assertDamage(calculate(new NecromancerSeason15DirectDamageProfile(0, 1000,
                0, 0, 1, 0, 0, 0, 0, false)), 0, 0);
        assertDamage(calculate(new NecromancerSeason15DirectDamageProfile(1000, 1000,
                0, 0, 0, 0, 0, 0, 0, false)), 0, 0);
    }

    @Test
    void missingProfileAndOverflowShouldBeRejected() {
        assertThrows(NullPointerException.class, () -> resolver.calculate(new RuntimeSkillConfiguration(HEMORRHAGE, 1, Map.of()), null));
        var overflow = new NecromancerSeason15DirectDamageProfile(1000, 1000,
                0, 40, Double.MAX_VALUE, 0, 0, 0, 0, false);
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveContext(overflow));
    }

    private DirectDamageResult calculate(NecromancerSeason15DirectDamageProfile profile) {
        return resolver.calculate(new RuntimeSkillConfiguration(HEMORRHAGE, 1, Map.of()), profile);
    }

    private static NecromancerSeason15DirectDamageProfile minimal(double intelligence, boolean vulnerable) {
        return new NecromancerSeason15DirectDamageProfile(1000, intelligence, 0, 0, 1, 0, 0, 0, 0, vulnerable);
    }

    private static void assertDamage(DirectDamageResult result, long normal, long critical) {
        assertEquals((double) normal, result.rawDamageExact(), EPSILON);
        assertEquals(normal, result.rawDamage());
        assertEquals(normal, result.finalDamage());
        assertEquals((double) critical, result.rawCriticalDamageExact(), EPSILON);
        assertEquals(critical, result.rawCriticalDamage());
        assertEquals(critical, result.criticalDamage());
        assertEquals(1.0d, result.context().postMitigationMultiplier());
    }
}
