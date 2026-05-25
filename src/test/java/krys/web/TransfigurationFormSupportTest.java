package krys.web;

import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationValueProvenance;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje parsowanie dynamicznych pól formularza Przeistoczenia. */
class TransfigurationFormSupportTest {
    @Test
    void shouldClearSubmittedFieldsThatDoNotBelongToSelectedOutcome() {
        ItemTransfiguration transfiguration = TransfigurationFormSupport.parse(Map.of(
                "transfigurationState", "TRANSFIGURED",
                "transfigurationOutcome", "INDESTRUCTIBLE",
                "transfigurationAddedAffixId", "ALL_STATS",
                "transfigurationAddedDisplayedValue", "96",
                "transfigurationReplacementAffixId", "TOTAL_ARMOR_PERCENT",
                "transfigurationReplacementDisplayedValue", "10",
                "transfigurationBonusQuality", "15"
        ));

        assertEquals(HoradricTransfigurationOutcome.INDESTRUCTIBLE, transfiguration.getOutcome());
        assertNull(transfiguration.getAddedTransfigurationAffix());
        assertNull(transfiguration.getReplacementTransfigurationAffix());
        assertNull(transfiguration.getBonusQuality());
    }

    @Test
    void shouldDefaultBonusAffixValueProvenanceToGameDisplayedValue() {
        ItemTransfiguration transfiguration = TransfigurationFormSupport.parse(Map.of(
                "transfigurationState", "TRANSFIGURED",
                "transfigurationOutcome", "BONUS_TRANSFIGURATION_AFFIX",
                "transfigurationAddedAffixId", "ALL_STATS",
                "transfigurationAddedDisplayedValue", "96"
        ));

        assertEquals(HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX, transfiguration.getOutcome());
        assertTrue(transfiguration.isLockedAfterTransfiguration());
        assertEquals(HoradricTuningPrism.NONE, transfiguration.getTuningPrism());
        assertEquals("ALL_STATS", transfiguration.getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(96.0d, transfiguration.getAddedTransfigurationAffix().getDisplayedValue());
        assertEquals(TransfigurationValueProvenance.GAME_DISPLAYED_VALUE,
                transfiguration.getAddedTransfigurationAffix().getValueProvenance());
        assertNull(transfiguration.getReplacementTransfigurationAffix());
    }

    @Test
    void shouldClearEverythingWhenStateIsNotTransfigured() {
        ItemTransfiguration transfiguration = TransfigurationFormSupport.parse(Map.of(
                "transfigurationState", "NONE",
                "transfigurationOutcome", "BONUS_TRANSFIGURATION_AFFIX",
                "transfigurationAddedAffixId", "ALL_STATS",
                "transfigurationAddedDisplayedValue", "96",
                "transfigurationLockedAfter", "true",
                "transfigurationTuningPrism", "AGGRESSIVE"
        ));

        assertFalse(transfiguration.isTransfigured());
        assertFalse(transfiguration.isLockedAfterTransfiguration());
        assertEquals(HoradricTuningPrism.NONE, transfiguration.getTuningPrism());
        assertEquals(HoradricTransfigurationOutcome.NONE, transfiguration.getOutcome());
        assertNull(transfiguration.getAddedTransfigurationAffix());
    }
}
