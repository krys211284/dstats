package krys.masterworking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemMasterworkingTest {
    @Test
    void shouldExposeDefaultStateForOldItems() {
        ItemMasterworking masterworking = ItemMasterworking.defaultState();

        assertEquals(0, masterworking.getQualityCurrent());
        assertEquals(25, masterworking.getQualityMax());
        assertNull(masterworking.getPerfectedAffix());
        assertEquals("0/25", masterworking.qualityLabel());
    }
}
