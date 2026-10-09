package krys.combat;

import java.math.BigDecimal;
import java.util.Objects;

/** Rozwiązany komponent direct damage; etykiety są metadanymi, bez wykonywania warunków. */
public record DirectDamageComponentSpec(
        String name,
        String source,
        BigDecimal damagePercent,
        int hitCount,
        String conditionLabel,
        boolean active,
        boolean includedInSingleTarget,
        String exclusionReason) {

    public DirectDamageComponentSpec {
        Objects.requireNonNull(damagePercent, "damagePercent");
        if (damagePercent.signum() < 0) {
            throw new IllegalArgumentException("damagePercent musi być nieujemny");
        }
        DirectDamageContext.requireFiniteNonNegative(damagePercent.doubleValue(), "damagePercent");
        if (hitCount <= 0) {
            throw new IllegalArgumentException("hitCount musi być większy od zera");
        }
    }
}
