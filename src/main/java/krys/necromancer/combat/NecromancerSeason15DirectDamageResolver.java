package krys.necromancer.combat;

import krys.combat.DirectDamageCalculator;
import krys.combat.DirectDamageComponentSpec;
import krys.combat.DirectDamageContext;
import krys.combat.DirectDamageResult;
import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.runtime.RuntimeSkillConfiguration;
import krys.skilltree.CharacterTreeSkill;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Season 15 / live 3.2.2: wyłącznie bazowy Krwotok R1, single target, PRE_TARGET_MITIGATION.
 * Scalar INT pochodzi z oficjalnego patcha 3.2.1; buckets z bieżącej referencji community D4Guides.
 * Źródła i granice wykonania opisuje README, sekcja N8B. Ulepszenia katalogu nie są wykonywane.
 */
public final class NecromancerSeason15DirectDamageResolver {
    private static final double DAMAGE_PER_INTELLIGENCE = 0.001625d;
    private static final double BASE_CRITICAL_MULTIPLIER = 1.5d;
    private static final double BASE_VULNERABLE_MULTIPLIER = 1.2d;

    private final DirectDamageCalculator calculator = new DirectDamageCalculator();

    /** Zwraca oba deterministyczne warianty hitu przed redukcją obrażeń celu. */
    public DirectDamageResult calculate(RuntimeSkillConfiguration configuration, NecromancerSeason15DirectDamageProfile profile) {
        Objects.requireNonNull(configuration, "configuration");
        var skillKey = configuration.skillKey();
        int rank = configuration.rank();
        if (!"necromancer".equals(skillKey.classId()) || !"krwotok".equals(skillKey.skillId())
                || rank != 1 || !configuration.selections().isEmpty()) {
            throw new UnsupportedOperationException("N8B obsługuje wyłącznie bazowy krwotok, rank 1: "
                    + skillKey.asExternalId() + ", rank " + rank + ", bez selections");
        }
        CharacterTreeSkill skill = NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill(skillKey.skillId());
        var catalogCoefficient = skill.getBaseDamagePercentRanks().damagePercentAtRank(rank);
        if (catalogCoefficient == null) {
            throw new IllegalStateException("Brak źródłowego współczynnika Krwotoku R1 w katalogu");
        }
        BigDecimal damagePercent = BigDecimal.valueOf(catalogCoefficient);
        return calculator.calculate(resolveContext(profile), List.of(baseHitSpec(skill, damagePercent)));
    }

    /** Rozwiązuje współczesne reguły poza neutralnym kernelem; A zawsze wynosi co najmniej 1. */
    public DirectDamageContext resolveContext(NecromancerSeason15DirectDamageProfile profile) {
        Objects.requireNonNull(profile, "profile");
        double mainStatMultiplier = 1.0d + profile.intelligence() * DAMAGE_PER_INTELLIGENCE;
        double additiveMultiplier = 1.0d + profile.additiveDamagePercent() / 100.0d
                + (profile.targetVulnerable() ? profile.vulnerableDamageAdditivePercent() / 100.0d : 0.0d);
        double damageBucketMultiplier = 1.0d + profile.damageBucketPercent() / 100.0d;
        double multiplicativeMultiplier = damageBucketMultiplier * profile.resolvedSeparateMultiplicativeMultiplier();
        double vulnerableMultiplier = profile.targetVulnerable()
                ? BASE_VULNERABLE_MULTIPLIER * (1.0d + profile.vulnerableDamageMultiplierPercent() / 100.0d)
                : 1.0d;
        double criticalAdditiveMultiplier = additiveMultiplier + profile.criticalStrikeDamageAdditivePercent() / 100.0d;
        double criticalMultiplier = (criticalAdditiveMultiplier / additiveMultiplier)
                * BASE_CRITICAL_MULTIPLIER * (1.0d + profile.criticalStrikeDamageMultiplierPercent() / 100.0d);
        return new DirectDamageContext(profile.averageWeaponDamage(), 1.0d, mainStatMultiplier,
                additiveMultiplier, vulnerableMultiplier, multiplicativeMultiplier, criticalMultiplier, 1.0d,
                DirectDamageContext.CriticalTotalRoundingPolicy.EXACT_COMPONENT_SUM);
    }

    /** Zachowuje BigDecimal bez zawężania; helper nie rozszerza publicznego wsparcia skilli ani rang. */
    static DirectDamageComponentSpec baseHitSpec(CharacterTreeSkill skill, BigDecimal damagePercent) {
        return new DirectDamageComponentSpec(skill.getSkillName() + " — bazowe trafienie",
                skill.getSourceReference(), damagePercent, 1, null, true, true, null);
    }
}
