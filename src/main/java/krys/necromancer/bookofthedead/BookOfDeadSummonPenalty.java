package krys.necromancer.bookofthedead;

import java.math.BigDecimal;
import java.util.Objects;

/** Kara Sacrifice opisana oddzielnie od bonusu gracza; nie wyłącza przywołania sług. */
public record BookOfDeadSummonPenalty(Type type, BigDecimal reductionPercent, String description) {
    public enum Type {
        SUMMON_COUNT_REDUCTION,
        GOLEM_DAMAGE_REDUCTION_MULTIPLICATIVE
    }

    public BookOfDeadSummonPenalty {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(reductionPercent, "reductionPercent");
        BookOfDeadSourceFact.requireText(description, "description");
        if (reductionPercent.signum() <= 0 || reductionPercent.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Procent redukcji musi należeć do (0, 100]");
        }
    }
}
