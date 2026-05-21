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

    public static String formatGreaterAffixValue(TemperingAffixDefinition definition) {
        return formatFlexibleValue(definition.greaterAffixValue());
    }

    public static String formatAffix(ItemTemperingAffix affix, TemperingAffixRegistry registry) {
        if (affix == null) {
            return "";
        }
        if (affix.getDisplayText() != null && !affix.getDisplayText().isBlank()) {
            return affix.getDisplayText();
        }
        return registry.findById(affix.getDefinitionId())
                .map(definition -> "+" + formatAffixValue(affix, definition)
                        + (definition.getUnit() == TemperingValueUnit.PERCENT ? "% " : " ")
                        + definition.getDisplayName())
                .orElse("+" + formatFlexibleValue(affix.getValue()) + " " + affix.getDefinitionId());
    }

    public static String formatSavedAffixEffect(ItemTemperingAffix affix, TemperingAffixRegistry registry) {
        if (affix == null) {
            return "";
        }
        String label = formatAffix(affix, registry);
        String categoryPrefix = affix.getCategory().getDisplayName() + ": ";
        if (label.startsWith(categoryPrefix)) {
            return label.substring(categoryPrefix.length());
        }
        return label;
    }

    public static String compactRuntimeStatus(TemperingRuntimeStatus status) {
        if (status == TemperingRuntimeStatus.DATA_ONLY || status == TemperingRuntimeStatus.NOT_RUNTIME_ENABLED) {
            return "Runtime nieaktywny";
        }
        return status.getDisplayName();
    }

    public static String formatValue(double value, TemperingValueUnit unit) {
        int scale = unit == TemperingValueUnit.PERCENT ? 1 : 0;
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .toPlainString()
                .replace('.', ',');
    }

    private static String formatFlexibleValue(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
                .replace('.', ',');
    }

    private static String formatAffixValue(ItemTemperingAffix affix, TemperingAffixDefinition definition) {
        if (affix.isGreaterAffix()) {
            return formatFlexibleValue(affix.getValue());
        }
        return formatValue(affix.getValue(), definition.getUnit());
    }
}
