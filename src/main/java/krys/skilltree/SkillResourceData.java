package krys.skilltree;

import java.math.BigDecimal;
import java.util.Objects;

/** Źródłowe wartości zasobu; równe granice oznaczają wartość dokładną, null brak danych. */
public record SkillResourceData(BigDecimal cost, BigDecimal generationMin, BigDecimal generationMax,
                                BigDecimal generationBonusKnown, SkillResourceGenerationFrequency generationFrequency) {
    public static final SkillResourceData EMPTY = new SkillResourceData(null, null, null);

    public SkillResourceData {
        Objects.requireNonNull(generationFrequency, "generationFrequency");
        requireNonNegative(cost);
        requireNonNegative(generationMin);
        requireNonNegative(generationMax);
        requireNonNegative(generationBonusKnown);
        if ((generationMin == null) != (generationMax == null)
                || (generationMin != null && generationMin.compareTo(generationMax) > 0)) {
            throw new IllegalArgumentException("Generowanie zasobu wymaga obu granic w kolejności min <= max.");
        }
    }

    /** Zachowuje dokładne, jednorazowe wartości istniejącego adaptera Paladyna. */
    public SkillResourceData(Integer cost, Integer generationBase, Integer generationBonusKnown) {
        this(decimal(cost), decimal(generationBase), decimal(generationBase), decimal(generationBonusKnown),
                SkillResourceGenerationFrequency.PER_USE);
    }

    public boolean hasGeneration() {
        return generationMin != null;
    }

    /** Maksymalna znana wartość z uwzględnieniem znanego możliwego dodatku z wybranego ulepszenia. */
    public BigDecimal generationMaxKnown() {
        return generationMax == null ? null : generationMax.add(
                generationBonusKnown == null ? BigDecimal.ZERO : generationBonusKnown);
    }

    private static BigDecimal decimal(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private static void requireNonNegative(BigDecimal value) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException("Wartość zasobu nie może być ujemna.");
        }
    }
}
