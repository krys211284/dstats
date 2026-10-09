package krys.runtime;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeSkillConfigurationTest {
    private static final RuntimeSkillKey KEY = new RuntimeSkillKey("necromancer", "krwotok");

    @Test
    void learnedAndUnlearnedConfigurationShouldUseEmptyMapForNoSelections() {
        var learned = new RuntimeSkillConfiguration(KEY, 1, Map.of());
        assertTrue(learned.isLearned());
        assertEquals(Optional.empty(), learned.selectedOption("grupa_1"));
        var unlearned = new RuntimeSkillConfiguration(KEY, 0, Map.of());
        assertFalse(unlearned.isLearned());
        assertTrue(unlearned.selections().isEmpty());
    }

    @Test
    void rankShouldHaveNoNeutralUpperLimit() {
        assertTrue(new RuntimeSkillConfiguration(KEY, Integer.MAX_VALUE, Map.of("custom-slot", "option_2")).isLearned());
    }

    @Test
    void negativeRankAndUnlearnedSelectionsShouldBeRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillConfiguration(KEY, -1, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillConfiguration(KEY, 0, Map.of("grupa_1", "szybkosc_uzycia")));
    }

    @Test
    void nullKeyMapAndEntriesShouldBeRejected() {
        assertThrows(NullPointerException.class, () -> new RuntimeSkillConfiguration(null, 1, Map.of()));
        assertThrows(NullPointerException.class, () -> new RuntimeSkillConfiguration(KEY, 1, null));
        var nullSlot = new HashMap<String, String>();
        nullSlot.put(null, "option");
        assertThrows(NullPointerException.class, () -> new RuntimeSkillConfiguration(KEY, 1, nullSlot));
        var nullOption = new HashMap<String, String>();
        nullOption.put("slot", null);
        assertThrows(NullPointerException.class, () -> new RuntimeSkillConfiguration(KEY, 1, nullOption));
    }

    @Test
    void selectionMapShouldBeDefensivelyCopiedAndImmutable() {
        var input = new HashMap<>(Map.of("grupa_1", "szybkosc_uzycia"));
        var configuration = new RuntimeSkillConfiguration(KEY, 1, input);
        input.put("grupa_1", "krwawa_kula");
        input.put("grupa_2", "przytloczenie");
        assertEquals(Map.of("grupa_1", "szybkosc_uzycia"), configuration.selections());
        assertEquals(Optional.of("szybkosc_uzycia"), configuration.selectedOption("grupa_1"));
        assertThrows(UnsupportedOperationException.class, () -> configuration.selections().put("grupa_1", "krwawa_kula"));
        assertThrows(UnsupportedOperationException.class, () -> configuration.selections().remove("grupa_1"));
        assertThrows(UnsupportedOperationException.class, () -> configuration.selections().entrySet().iterator().next().setValue("krwawa_kula"));
        assertThrows(UnsupportedOperationException.class, () -> configuration.selections().clear());
    }

    @Test
    void technicalSlotAndOptionIdsShouldBeValidatedWithoutNormalization() {
        for (String invalid : new String[]{"", " ", "Grupa_1", "NONE", " slot", "slot ", "two words", "slot!", "żółć",
                "slot:id", "slot.id", "_slot", "-slot", "slot\n"}) {
            assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillConfiguration(KEY, 1, Map.of(invalid, "option")), invalid);
            assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillConfiguration(KEY, 1, Map.of("slot", invalid)), invalid);
        }
    }

    @Test
    void lookupShouldRejectInvalidSlotAndNotInterpretUnknownSelections() {
        var configuration = new RuntimeSkillConfiguration(KEY, 1, Map.of("custom-slot", "custom_option"));
        assertEquals(Optional.of("custom_option"), configuration.selectedOption("custom-slot"));
        assertEquals(Optional.empty(), configuration.selectedOption("another_slot"));
        assertThrows(NullPointerException.class, () -> configuration.selectedOption(null));
        assertThrows(IllegalArgumentException.class, () -> configuration.selectedOption("Custom-slot"));
    }

    @Test
    void equalityAndHashCodeShouldIncludeKeyRankAndSelectionsRegardlessOfMapOrder() {
        var first = new RuntimeSkillConfiguration(KEY, 1, Map.of("grupa_1", "szybkosc_uzycia", "grupa_2", "przytloczenie"));
        var second = new RuntimeSkillConfiguration(KEY, 1, Map.of("grupa_2", "przytloczenie", "grupa_1", "szybkosc_uzycia"));
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new RuntimeSkillConfiguration(new RuntimeSkillKey("paladin", "krwotok"), 1, first.selections()));
        assertNotEquals(first, new RuntimeSkillConfiguration(KEY, 2, first.selections()));
        assertNotEquals(first, new RuntimeSkillConfiguration(KEY, 1, Map.of()));
    }
}
