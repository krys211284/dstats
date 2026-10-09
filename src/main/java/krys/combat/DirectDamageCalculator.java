package krys.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Czysta matematyka direct damage dla rozwiązanych wejść; bez resolverów i stanu symulacji. */
public final class DirectDamageCalculator {
    public DirectDamageResult calculate(DirectDamageContext context, List<DirectDamageComponentSpec> specs) {
        Objects.requireNonNull(context, "context");
        List<DirectDamageComponentSpec> components = List.copyOf(specs);
        List<DirectDamageComponentResult> results = new ArrayList<>();
        double totalRaw = 0.0d;
        double totalRawCritical = 0.0d;
        for (var spec : components) {
            double base = DirectDamageContext.requireFiniteNonNegative(
                    context.averageWeaponDamage() * (spec.damagePercent().doubleValue() / 100.0d), "baseDamageExact");
            double raw = spec.active() ? rawDamage(base, spec.hitCount(), context, 1.0d) : 0.0d;
            double rawCritical = spec.active() ? rawDamage(base, spec.hitCount(), context, context.criticalMultiplier()) : 0.0d;
            results.add(new DirectDamageComponentResult(spec, base, raw, rawCritical, Math.round(base),
                    Math.round(raw), finalDamage(raw, context), Math.round(rawCritical), finalDamage(rawCritical, context)));
            if (spec.active() && spec.includedInSingleTarget()) {
                totalRaw = DirectDamageContext.requireFiniteNonNegative(totalRaw + raw, "totalRawDamageExact");
                totalRawCritical = DirectDamageContext.requireFiniteNonNegative(
                        totalRawCritical + rawCritical, "totalRawCriticalDamageExact");
            }
        }

        long rawDamage = Math.round(totalRaw);
        long rawCriticalDamage;
        long criticalDamage;
        if (context.criticalTotalRoundingPolicy() == DirectDamageContext.CriticalTotalRoundingPolicy.ROUNDED_RAW_TOTAL) {
            double roundedRawCritical = DirectDamageContext.requireFiniteNonNegative(
                    rawDamage * context.criticalMultiplier(), "roundedRawCriticalDamage");
            rawCriticalDamage = Math.round(roundedRawCritical);
            criticalDamage = finalDamage(rawCriticalDamage, context);
        } else {
            rawCriticalDamage = Math.round(totalRawCritical);
            criticalDamage = finalDamage(totalRawCritical, context);
        }
        return new DirectDamageResult(context, results, totalRaw, totalRawCritical,
                rawDamage, finalDamage(totalRaw, context), rawCriticalDamage, criticalDamage);
    }

    private static double rawDamage(double base, int hitCount, DirectDamageContext context, double criticalMultiplier) {
        // Kolejność operacji double zachowuje dotychczasowy pipeline, w tym hitCount po mnożniku kryta.
        return DirectDamageContext.requireFiniteNonNegative(base
                * context.weaponMultiplier()
                * context.mainStatMultiplier()
                * context.additiveMultiplier()
                * context.vulnerableMultiplier()
                * context.multiplicativeMultiplier()
                * criticalMultiplier
                * hitCount, "rawDamageExact");
    }

    private static long finalDamage(double rawDamage, DirectDamageContext context) {
        return Math.round(DirectDamageContext.requireFiniteNonNegative(
                rawDamage * context.postMitigationMultiplier(), "postMitigationDamageExact"));
    }
}
