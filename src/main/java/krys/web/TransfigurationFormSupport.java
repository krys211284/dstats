package krys.web;

import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixRoll;
import krys.transfiguration.TransfigurationValueProvenance;

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
        String upgradedAffixRef = "";
        TransfigurationAffixRoll addedRoll = null;
        String replacedAffixRef = "";
        TransfigurationAffixRoll replacementRoll = null;
        Integer bonusQuality = null;
        if (outcome == HoradricTransfigurationOutcome.UPGRADE_TO_GREATER_AFFIX) {
            upgradedAffixRef = fields.getOrDefault("transfigurationUpgradedAffixRef", "");
        } else if (outcome == HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX) {
            addedRoll = parseRoll(fields, "transfigurationAdded");
        } else if (outcome == HoradricTransfigurationOutcome.REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX) {
            replacedAffixRef = fields.getOrDefault("transfigurationReplacedAffixRef", "");
            replacementRoll = parseRoll(fields, "transfigurationReplacement");
        } else if (outcome == HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY) {
            bonusQuality = parseInteger(fields.get("transfigurationBonusQuality"));
        }
        return new ItemTransfiguration(
                true,
                locked,
                HoradricTuningPrism.fromNullable(fields.getOrDefault("transfigurationTuningPrism", "")),
                outcome,
                upgradedAffixRef,
                addedRoll,
                replacedAffixRef,
                replacementRoll,
                bonusQuality,
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
                parseDouble(firstNonBlank(fields.get(prefix + "DisplayedValue"), fields.get(prefix + "AffixValue"))),
                TransfigurationValueProvenance.fromNullable(fields.getOrDefault(prefix + "ValueProvenance",
                        TransfigurationValueProvenance.GAME_DISPLAYED_VALUE.name())),
                fields.getOrDefault(prefix + "AffixElement", "")
        );
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
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
