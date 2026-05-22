package krys.masterworking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemMasterworkingTest {
    @Test
    void shouldExposeDefaultStateForOldItems() {
        ItemMasterworking masterworking = ItemMasterworking.defaultState();

        assertEquals(0, masterworking.getQualityCurrent());
        assertEquals(25, masterworking.getQualityMax());
        assertNull(masterworking.getPerfectedAffix());
        assertEquals("0/25", masterworking.qualityLabel());
    }

    @Test
    void shouldExposeAllowedQualityStepsFromGame() {
        assertEquals(java.util.List.of(0, 3, 6, 9, 12, 15, 17, 20, 21, 25), ItemMasterworking.ALLOWED_QUALITY_STEPS);
        assertTrue(ItemMasterworking.isAllowedQualityStep(17));
        assertFalse(ItemMasterworking.isAllowedQualityStep(18));
        assertEquals("0, 3, 6, 9, 12, 15, 17, 20, 21, 25", ItemMasterworking.allowedQualityStepsLabel());
    }

    @Test
    void shouldReadUnsupportedPersistedQualityAsZeroWithoutGuessingNearestStep() {
        ItemMasterworking masterworking = ItemMasterworking.fromPersisted(18, 25, null);

        assertEquals(0, masterworking.getQualityCurrent());
        assertEquals(25, masterworking.getQualityMax());
    }
}
