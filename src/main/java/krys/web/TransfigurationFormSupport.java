package krys.web;

import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixRoll;

import java.util.Map;

/** Parser pól formularza Przeistoczenia itemu. */
final class TransfigurationFormSupport {
    private TransfigurationFormSupport() {
    }

    static ItemTransfiguration parse(Map<String, String> fields) {
        boolean transfigured = "true".equals(fields.get("transfigurationTransfigured"))
                || "TRANSFIGURED".equals(fields.get("transfigurationState"));
        if (!transfigured) {
            return ItemTransfiguration.none();
        }
        HoradricTransfigurationOutcome outcome = HoradricTransfigurationOutcome.fromNullable(
                fields.getOrDefault("transfigurationOutcome", ""));
        boolean locked = !fields.containsKey("transfigurationLockedAfter")
                || "true".equals(fields.get("transfigurationLockedAfter"));
        return new ItemTransfiguration(
                true,
                locked,
                HoradricTuningPrism.fromNullable(fields.getOrDefault("transfigurationTuningPrism", "")),
                outcome,
                fields.getOrDefault("transfigurationUpgradedAffixRef", ""),
                parseRoll(fields, "transfigurationAdded"),
                fields.getOrDefault("transfigurationReplacedAffixRef", ""),
                parseRoll(fields, "transfigurationReplacement"),
                parseInteger(fields.get("transfigurationBonusQuality")),
                "true".equals(fields.get("transfigurationIndestructible")),
                fields.getOrDefault("transfigurationNotes", "")
        );
    }

    private static TransfigurationAffixRoll parseRoll(Map<String, String> fields, String prefix) {
        String definitionId = fields.getOrDefault(prefix + "AffixId", "");
        if (definitionId.isBlank()) {
            return null;
        }
        return new TransfigurationAffixRoll(
                definitionId,
                parseDouble(fields.get(prefix + "AffixValue")),
                fields.getOrDefault(prefix + "AffixElement", "")
        );
    }

    private static Integer parseInteger(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(rawValue.replace(" ", ""));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static double parseDouble(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return -1.0d;
        }
        try {
            return Double.parseDouble(rawValue.replace(',', '.').replace(" ", ""));
        } catch (NumberFormatException exception) {
            return -1.0d;
        }
    }
}
