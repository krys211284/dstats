package krys.necromancer.combat;

/**
 * Niemutowalne, już zsumowane wejścia standalone direct hit dla Season 15 / live 3.2.2.
 * Wartości procentowe podaje się w punktach procentowych; separate multiplier jest gotowym iloczynem.
 * Granica wyniku to PRE_TARGET_MITIGATION: profil nie zawiera redukcji celu ani szansy kryta.
 */
public record NecromancerSeason15DirectDamageProfile(
        double averageWeaponDamage,
        double intelligence,
        double additiveDamagePercent,
        double damageBucketPercent,
        double resolvedSeparateMultiplicativeMultiplier,
        double criticalStrikeDamageAdditivePercent,
        double criticalStrikeDamageMultiplierPercent,
        double vulnerableDamageAdditivePercent,
        double vulnerableDamageMultiplierPercent,
        boolean targetVulnerable) {

    public NecromancerSeason15DirectDamageProfile {
        requireFiniteNonNegative(averageWeaponDamage, "averageWeaponDamage");
        requireFiniteNonNegative(intelligence, "intelligence");
        requireFiniteNonNegative(additiveDamagePercent, "additiveDamagePercent");
        requireFiniteNonNegative(damageBucketPercent, "damageBucketPercent");
        requireFiniteNonNegative(resolvedSeparateMultiplicativeMultiplier, "resolvedSeparateMultiplicativeMultiplier");
        requireFiniteNonNegative(criticalStrikeDamageAdditivePercent, "criticalStrikeDamageAdditivePercent");
        requireFiniteNonNegative(criticalStrikeDamageMultiplierPercent, "criticalStrikeDamageMultiplierPercent");
        requireFiniteNonNegative(vulnerableDamageAdditivePercent, "vulnerableDamageAdditivePercent");
        requireFiniteNonNegative(vulnerableDamageMultiplierPercent, "vulnerableDamageMultiplierPercent");
    }

    private static void requireFiniteNonNegative(double value, String field) {
        if (!Double.isFinite(value) || value < 0.0d) {
            throw new IllegalArgumentException("Pole " + field + " wymaga skończonej, nieujemnej wartości");
        }
    }
}
