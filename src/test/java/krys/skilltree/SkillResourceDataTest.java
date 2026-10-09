package krys.skilltree;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class SkillResourceDataTest {
    @Test
    void shouldKeepExistingExactPerUseValuesAndTheConfirmedBonus() {
        var data = new SkillResourceData(null, 10, 5);
        assertEquals(new BigDecimal("10"), data.generationMin());
        assertEquals(new BigDecimal("10"), data.generationMax());
        assertEquals(new BigDecimal("15"), data.generationMaxKnown());
        assertEquals(SkillResourceGenerationFrequency.PER_USE, data.generationFrequency());
        assertTrue(data.hasGeneration());
        assertFalse(SkillResourceData.EMPTY.hasGeneration());
        assertNull(SkillResourceData.EMPTY.generationMaxKnown());
    }

    @Test
    void shouldRetainFractionalSourceValuesAndPerSecondRanges() {
        var data = new SkillResourceData(null, new BigDecimal("7.5"), new BigDecimal("7.5"), null,
                SkillResourceGenerationFrequency.PER_USE);
        assertEquals(new BigDecimal("7.5"), data.generationMaxKnown());
        var range = new SkillResourceData(null, new BigDecimal("12"), new BigDecimal("16"), null,
                SkillResourceGenerationFrequency.PER_SECOND);
        assertEquals(new BigDecimal("12"), range.generationMin());
        assertEquals(new BigDecimal("16"), range.generationMaxKnown());
        assertEquals(SkillResourceGenerationFrequency.PER_SECOND, range.generationFrequency());
    }

    @Test
    void shouldKeepSourceAmountPerEventWithoutInferringFrequencyOrTotals() {
        // Golden N6: dokładnie 2 Esencji za pochłonięcie jednej duszy.
        var data = new SkillResourceData(null, new BigDecimal("2"), new BigDecimal("2"), null,
                SkillResourceGenerationFrequency.PER_EVENT);
        assertEquals(new BigDecimal("2"), data.generationMin());
        assertEquals(new BigDecimal("2"), data.generationMax());
        assertEquals(new BigDecimal("2"), data.generationMaxKnown());
        assertEquals(SkillResourceGenerationFrequency.PER_EVENT, data.generationFrequency());
        assertTrue(data.hasGeneration());
        assertNull(data.cost());
        assertNull(data.generationBonusKnown());
    }

    @Test
    void shouldRejectIncompleteReversedOrNegativeResourceRanges() {
        assertThrows(IllegalArgumentException.class, () -> new SkillResourceData(null, BigDecimal.TEN, null, null, SkillResourceGenerationFrequency.PER_USE));
        assertThrows(IllegalArgumentException.class, () -> new SkillResourceData(null, new BigDecimal("16"), new BigDecimal("12"), null, SkillResourceGenerationFrequency.PER_SECOND));
        assertThrows(IllegalArgumentException.class, () -> new SkillResourceData(-10, null, null));
        assertThrows(NullPointerException.class, () -> new SkillResourceData(null, null, null, null, null));
    }
}
