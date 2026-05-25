package krys.transfiguration;

import krys.itemimport.ImportedItemAffix;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Formatowanie Przeistoczenia w normalnym UI; runtime pozostaje nieaktywny. */
public final class TransfigurationPresentationSupport {
    public static final String RUNTIME_INACTIVE_LABEL = "Runtime nieaktywny";

    private TransfigurationPresentationSupport() {
    }

    public static String compactSummary(ItemTransfiguration transfiguration, List<ImportedItemAffix> itemAffixes) {
        if (transfiguration == null || !transfiguration.isTransfigured()) {
            return "";
        }
        StringBuilder summary = new StringBuilder("Przeistoczenie · ");
        summary.append(outcomeSummary(transfiguration, itemAffixes));
        summary.append(" · ").append(RUNTIME_INACTIVE_LABEL);
        return summary.toString();
    }

    public static String lockStatus(ItemTransfiguration transfiguration) {
        if (transfiguration == null || !transfiguration.isTransfigured()) {
            return "";
        }
        return transfiguration.isLockedAfterTransfiguration()
                ? "Przedmiot niemodyfikowalny po przeistoczeniu"
                : "Przedmiot nie został zablokowany po przeistoczeniu";
    }

    public static String formatRoll(TransfigurationAffixRoll roll) {
        if (roll == null || roll.isEmpty()) {
            return "";
        }
        TransfigurationAffixDefinition definition = TransfigurationAffixCatalog.findById(roll.getDefinitionId()).orElse(null);
        String name = definition == null ? roll.getDefinitionId() : definition.getDisplayName();
        String value = formatValue(roll.getDisplayedValue(), definition == null ? TransfigurationAffixValueKind.FLAT : definition.getValueKind());
        String element = roll.getElement().isBlank() ? "" : " (" + roll.getElement() + ")";
        return value + " " + name + element;
    }

    public static String formatRange(TransfigurationAffixDefinition definition) {
        String min = formatNumber(definition.getMin());
        String max = formatNumber(definition.getMax());
        return switch (definition.getValueKind()) {
            case FLAT, RANKS -> min + "-" + max;
            case PERCENT -> min + "-" + max + "%";
            case MULTIPLICATIVE_PERCENT -> min + "-" + max + "%[x]";
        };
    }

    public static String formatScaledRange(TransfigurationAffixDefinition definition, double multiplier) {
        String min = formatNumber(definition.getMin() * multiplier);
        String max = formatNumber(definition.getMax() * multiplier);
        return switch (definition.getValueKind()) {
            case FLAT, RANKS -> min + "-" + max;
            case PERCENT -> min + "-" + max + "%";
            case MULTIPLICATIVE_PERCENT -> min + "-" + max + "%[x]";
        };
    }

    private static String outcomeSummary(ItemTransfiguration transfiguration, List<ImportedItemAffix> itemAffixes) {
        return switch (transfiguration.getOutcome()) {
            case INDESTRUCTIBLE -> "Niezniszczalny";
            case UPGRADE_TO_GREATER_AFFIX -> "Ulepszono do Greater Affix: "
                    + affixRefLabel(transfiguration.getUpgradedAffixRef(), itemAffixes);
            case BONUS_TRANSFIGURATION_AFFIX -> "Bonusowy affix: "
                    + emptyLabel(formatRoll(transfiguration.getAddedTransfigurationAffix()));
            case REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX -> "Zamiana affixu: "
                    + affixRefLabel(transfiguration.getReplacedAffixRef(), itemAffixes)
                    + " -> "
                    + emptyLabel(formatRoll(transfiguration.getReplacementTransfigurationAffix()));
            case BONUS_ITEM_QUALITY -> "Bonusowa jakość +"
                    + (transfiguration.getBonusQuality() == null ? "?" : transfiguration.getBonusQuality());
            case UNKNOWN -> "Nieznany / do uzupełnienia";
            case NONE -> "Brak";
        };
    }

    private static String affixRefLabel(String ref, List<ImportedItemAffix> itemAffixes) {
        if (ref == null || ref.isBlank()) {
            return "Brak";
        }
        for (ImportedItemAffix affix : itemAffixes == null ? List.<ImportedItemAffix>of() : itemAffixes) {
            if (affix.getType().name().equals(ref)) {
                return affix.getType().getDisplayName();
            }
        }
        return ref;
    }

    private static String formatValue(double value, TransfigurationAffixValueKind kind) {
        String prefix = kind == TransfigurationAffixValueKind.FLAT || kind == TransfigurationAffixValueKind.RANKS ? "+" : "";
        String suffix = switch (kind) {
            case FLAT, RANKS -> "";
            case PERCENT -> "%";
            case MULTIPLICATIVE_PERCENT -> "%[x]";
        };
        return prefix + formatNumber(value) + suffix;
    }

    private static String formatNumber(double value) {
        BigDecimal decimal = BigDecimal.valueOf(value).stripTrailingZeros();
        return decimal.scale() <= 0
                ? decimal.toPlainString()
                : String.format(Locale.US, "%s", decimal.toPlainString()).replace('.', ',');
    }

    private static String emptyLabel(String value) {
        return value == null || value.isBlank() ? "Brak" : value;
    }
}
