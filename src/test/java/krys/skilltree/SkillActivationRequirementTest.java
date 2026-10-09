package krys.skilltree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillActivationRequirementTest {
    @Test
    void shouldDescribeBothConsumptionModesWithoutClassSpecificRules() {
        for (var mode : SkillActivationConsumptionMode.values()) {
            var requirement = new SkillActivationRequirement("test_object", "Obiekt testowy", 1, mode);
            assertEquals("test_object", requirement.id());
            assertEquals("Obiekt testowy", requirement.displayName());
            assertEquals(1, requirement.amount());
            assertEquals(mode, requirement.consumptionMode());
        }
    }

    @Test
    void shouldRejectNonPositiveAmounts() {
        for (int amount : new int[]{0, -1}) {
            assertThrows(IllegalArgumentException.class, () -> new SkillActivationRequirement(
                    "test", "Obiekt", amount, SkillActivationConsumptionMode.CONSUMED_ON_USE));
        }
    }

    @Test
    void shouldRequireIdNameAndConsumptionMode() {
        for (String empty : new String[]{null, "", " "}) {
            assertThrows(IllegalArgumentException.class, () -> new SkillActivationRequirement(
                    empty, "Obiekt", 1, SkillActivationConsumptionMode.CONSUMED_ON_USE));
            assertThrows(IllegalArgumentException.class, () -> new SkillActivationRequirement(
                    "test", empty, 1, SkillActivationConsumptionMode.CONSUMED_ON_USE));
        }
        assertThrows(NullPointerException.class, () -> new SkillActivationRequirement("test", "Obiekt", 1, null));
    }
}
