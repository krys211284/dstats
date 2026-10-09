package krys.combat;

import java.util.List;
import java.util.Objects;

/** Total single target powstaje z niezaokrąglonych komponentów, niezależnie od ich debugowych longów. */
public record DirectDamageResult(
        DirectDamageContext context,
        List<DirectDamageComponentResult> components,
        double rawDamageExact,
        double rawCriticalDamageExact,
        long rawDamage,
        long finalDamage,
        long rawCriticalDamage,
        long criticalDamage) {

    public DirectDamageResult {
        Objects.requireNonNull(context, "context");
        components = List.copyOf(components);
        DirectDamageContext.requireFiniteNonNegative(rawDamageExact, "rawDamageExact");
        DirectDamageContext.requireFiniteNonNegative(rawCriticalDamageExact, "rawCriticalDamageExact");
    }
}
