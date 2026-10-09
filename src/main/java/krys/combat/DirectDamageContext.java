package krys.combat;

import java.util.Objects;

/** Rozwiązane wartości matematyczne; reguły statystyk, klas i efektów należą do adapterów. */
public record DirectDamageContext(
        double averageWeaponDamage,
        double weaponMultiplier,
        double mainStatMultiplier,
        double additiveMultiplier,
        double vulnerableMultiplier,
        double multiplicativeMultiplier,
        double criticalMultiplier,
        double postMitigationMultiplier,
        CriticalTotalRoundingPolicy criticalTotalRoundingPolicy) {

    /** Polityka matematyczna totalu krytycznego, niezależna od tożsamości skilla lub klasy. */
    public enum CriticalTotalRoundingPolicy {
        EXACT_COMPONENT_SUM,
        ROUNDED_RAW_TOTAL
    }

    public DirectDamageContext(double averageWeaponDamage, double weaponMultiplier, double mainStatMultiplier,
                               double additiveMultiplier, double vulnerableMultiplier, double multiplicativeMultiplier,
                               double criticalMultiplier, double postMitigationMultiplier) {
        this(averageWeaponDamage, weaponMultiplier, mainStatMultiplier, additiveMultiplier, vulnerableMultiplier,
                multiplicativeMultiplier, criticalMultiplier, postMitigationMultiplier,
                CriticalTotalRoundingPolicy.EXACT_COMPONENT_SUM);
    }

    public DirectDamageContext {
        requireFiniteNonNegative(averageWeaponDamage, "averageWeaponDamage");
        requireFiniteNonNegative(weaponMultiplier, "weaponMultiplier");
        requireFiniteNonNegative(mainStatMultiplier, "mainStatMultiplier");
        requireFiniteNonNegative(additiveMultiplier, "additiveMultiplier");
        requireFiniteNonNegative(vulnerableMultiplier, "vulnerableMultiplier");
        requireFiniteNonNegative(multiplicativeMultiplier, "multiplicativeMultiplier");
        requireFiniteNonNegative(criticalMultiplier, "criticalMultiplier");
        requireFiniteNonNegative(postMitigationMultiplier, "postMitigationMultiplier");
        if (postMitigationMultiplier > 1.0d) {
            throw new IllegalArgumentException("postMitigationMultiplier musi należeć do [0, 1]");
        }
        Objects.requireNonNull(criticalTotalRoundingPolicy, "criticalTotalRoundingPolicy");
    }

    static double requireFiniteNonNegative(double value, String field) {
        if (!Double.isFinite(value) || value < 0.0d) {
            throw new IllegalArgumentException("Pole " + field + " wymaga skończonej, nieujemnej wartości");
        }
        return value;
    }
}
