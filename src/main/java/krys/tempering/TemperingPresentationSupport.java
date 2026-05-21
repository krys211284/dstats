package krys.tempering;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formatowanie hartowania bez wpływu na runtime. */
public final class TemperingPresentationSupport {
    private TemperingPresentationSupport() {
    }

    public static String formatRange(TemperingAffixDefinition definition) {
        return formatValue(definition.getRangeMin(), definition.getUnit())
                + " - "
                + formatValue(definition.getRangeMax(), definition.getUnit());
    }

    public static String formatAffix(ItemTemperingAffix affix, TemperingAffixRegistry registry) {
        if (affix == null) {
            return "";
        }
        if (affix.getDisplayText() != null && !affix.getDisplayText().isBlank()) {
            return affix.getDisplayText();
        }
        return registry.findById(affix.getDefinitionId())
                .map(definition -> "+" + formatValue(affix.getValue(), definition.getUnit())
                        + (definition.getUnit() == TemperingValueUnit.PERCENT ? "% " : " ")
                        + definition.getDisplayName())
                .orElse("+" + formatValue(affix.getValue(), TemperingValueUnit.FLAT) + " " + affix.getDefinitionId());
    }

    public static String formatValue(double value, TemperingValueUnit unit) {
        int scale = unit == TemperingValueUnit.PERCENT ? 1 : 0;
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .toPlainString()
                .replace('.', ',');
    }
}
