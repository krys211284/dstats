package krys.combat;

import java.util.Objects;

/** Wartości przed zaokrągleniem i całkowity debug jednego komponentu. */
public record DirectDamageComponentResult(
        DirectDamageComponentSpec spec,
        double baseDamageExact,
        double rawDamageExact,
        double rawCriticalDamageExact,
        long baseDamage,
        long rawDamage,
        long finalDamage,
        long rawCriticalDamage,
        long criticalDamage) {

    public DirectDamageComponentResult {
        Objects.requireNonNull(spec, "spec");
        DirectDamageContext.requireFiniteNonNegative(baseDamageExact, "baseDamageExact");
        DirectDamageContext.requireFiniteNonNegative(rawDamageExact, "rawDamageExact");
        DirectDamageContext.requireFiniteNonNegative(rawCriticalDamageExact, "rawCriticalDamageExact");
    }
}
