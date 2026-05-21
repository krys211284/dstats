package krys.masterworking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ItemMasterworkingTest {
    @Test
    void shouldExposeDefaultStateForOldItems() {
        ItemMasterworking masterworking = ItemMasterworking.defaultState();

        assertFalse(masterworking.isEnabled());
        assertEquals(0, masterworking.getQualityCurrent());
        assertEquals(25, masterworking.getQualityMax());
        assertEquals("0/25", masterworking.qualityLabel());
    }
}
