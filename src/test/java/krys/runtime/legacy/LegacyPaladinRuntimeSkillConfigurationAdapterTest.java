package krys.runtime.legacy;

import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPaladinRuntimeSkillConfigurationAdapterTest {
    @Test
    void representativeLegacyStatesShouldRoundTripEveryField() {
        var states = List.of(
                new SkillState(SkillId.BRANDISH, 1, false, SkillUpgradeChoice.NONE),
                new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.LEFT),
                new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.MIDDLE),
                new SkillState(SkillId.ADVANCE, 1, false, SkillUpgradeChoice.NONE),
                new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.NONE, SkillRuntimeModifierChoice.NONE,
                        "animusz", "zwiekszenie_obrazen", "kara"));
        for (var state : states) {
            assertRoundTrip(state);
        }
        var brandish = LegacyPaladinRuntimeSkillConfigurationAdapter.toRuntimeConfiguration(states.get(1));
        assertEquals(Map.of("legacy_base_upgrade", "enabled", "legacy_choice_upgrade", "left"), brandish.selections());
        var holyBolt = LegacyPaladinRuntimeSkillConfigurationAdapter.toRuntimeConfiguration(states.get(2));
        assertEquals(Map.of("legacy_base_upgrade", "enabled", "legacy_choice_upgrade", "middle"), holyBolt.selections());
        var clash = LegacyPaladinRuntimeSkillConfigurationAdapter.toRuntimeConfiguration(states.get(4));
        assertEquals(Map.of("legacy_base_upgrade", "enabled", "legacy_choice_upgrade", "left", "legacy_runtime_modifier", "animus",
                "grupa_1", "animusz", "grupa_2", "zwiekszenie_obrazen", "grupa_3", "kara"), clash.selections());
    }

    @Test
    void clashCoexistingLegacyAndDifferentGroupChoicesShouldBeLossless() {
        var state = new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT, SkillRuntimeModifierChoice.ANIMUS,
                "generowanie_wiary", "zwiekszenie_obrazen", "potyczka");
        assertEquals(SkillRuntimeModifierChoice.ANIMUS, state.getRuntimeModifierChoice());
        assertEquals("generowanie_wiary", state.getChoiceGroup1());
        assertEquals(SkillUpgradeChoice.LEFT, state.getChoiceUpgrade());
        assertEquals("potyczka", state.getChoiceGroup3());
        var configuration = LegacyPaladinRuntimeSkillConfigurationAdapter.toRuntimeConfiguration(state);
        assertEquals(Map.of("legacy_base_upgrade", "enabled", "legacy_choice_upgrade", "left", "legacy_runtime_modifier", "animus",
                "grupa_1", "generowanie_wiary", "grupa_2", "zwiekszenie_obrazen", "grupa_3", "potyczka"), configuration.selections());
        assertRoundTrip(state);
    }

    @Test
    void allFourUnlearnedStatesShouldRoundTripWithNoSelections() {
        for (var id : SkillId.values()) {
            var state = new SkillState(id, 0, false, SkillUpgradeChoice.NONE);
            var configuration = LegacyPaladinRuntimeSkillConfigurationAdapter.toRuntimeConfiguration(state);
            assertFalse(configuration.isLearned());
            assertTrue(configuration.selections().isEmpty());
            assertRoundTrip(state);
        }
    }

    @Test
    void missingSlotsShouldRestoreLegacyDefaultsForAllFourKeys() {
        for (var id : SkillId.values()) {
            var configuration = new RuntimeSkillConfiguration(LegacyPaladinRuntimeSkillKeyAdapter.toRuntimeKey(id), 1, Map.of());
            assertFields(new SkillState(id, 1, false, SkillUpgradeChoice.NONE),
                    LegacyPaladinRuntimeSkillConfigurationAdapter.toLegacySkillState(configuration).orElseThrow());
        }
    }

    @ParameterizedTest
    @CsvSource({"left, LEFT", "middle, MIDDLE", "right, RIGHT"})
    void explicitChoiceSlotsShouldReverseExactly(String option, SkillUpgradeChoice expected) {
        var configuration = paladin("wymach", 5, Map.of("legacy_base_upgrade", "enabled", "legacy_choice_upgrade", option));
        assertFields(new SkillState(SkillId.BRANDISH, 5, true, expected),
                LegacyPaladinRuntimeSkillConfigurationAdapter.toLegacySkillState(configuration).orElseThrow());
    }

    @Test
    void nonLegacyKeysShouldReturnEmptyWithoutInterpretingTheirSelections() {
        for (String externalId : new String[]{"necromancer:krwotok", "paladin:unknown", "necromancer:wymach"}) {
            assertTrue(LegacyPaladinRuntimeSkillConfigurationAdapter.toLegacySkillState(
                    new RuntimeSkillConfiguration(RuntimeSkillKey.parse(externalId), 1, Map.of("foo", "bar"))).isEmpty());
        }
    }

    @ParameterizedTest
    @CsvSource({"foo, bar", "legacy_base_upgrade, disabled", "legacy_choice_upgrade, unknown", "legacy_choice_upgrade, none",
            "legacy_runtime_modifier, unknown", "legacy_runtime_modifier, none", "grupa_1, none", "grupa_2, none", "grupa_3, none"})
    void unknownSlotsInvalidReservedValuesAndExplicitNoSelectionShouldBeRejected(String slot, String option) {
        assertThrows(IllegalArgumentException.class, () -> LegacyPaladinRuntimeSkillConfigurationAdapter.toLegacySkillState(
                paladin("starcie", 1, Map.of(slot, option))));
    }

    @Test
    void existingLegacyConstructorShouldRemainAuthorityForInvalidCombinations() {
        for (var configuration : List.of(
                paladin("wymach", 6, Map.of()),
                paladin("wymach", 1, Map.of("legacy_choice_upgrade", "left")),
                paladin("wymach", 1, Map.of("legacy_runtime_modifier", "animus")),
                paladin("natarcie", 1, Map.of("grupa_1", "anything")))) {
            assertThrows(IllegalArgumentException.class, () -> LegacyPaladinRuntimeSkillConfigurationAdapter.toLegacySkillState(configuration));
        }
    }

    @Test
    void bridgeShouldPreserveTechnicalGroupChoicesWithoutCatalogInterpretation() {
        var state = new SkillState(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE, SkillRuntimeModifierChoice.NONE,
                "custom_option", SkillState.NO_TREE_CHOICE, SkillState.NO_TREE_CHOICE);
        assertRoundTrip(state);
    }

    @Test
    void invalidLegacyGroupSyntaxShouldFailWithoutNormalization() {
        var state = new SkillState(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE, SkillRuntimeModifierChoice.NONE,
                "Invalid Option", SkillState.NO_TREE_CHOICE, SkillState.NO_TREE_CHOICE);
        assertThrows(IllegalArgumentException.class, () -> LegacyPaladinRuntimeSkillConfigurationAdapter.toRuntimeConfiguration(state));
    }

    @Test
    void nullInputsShouldThrowExplicitly() {
        assertThrows(NullPointerException.class, () -> LegacyPaladinRuntimeSkillConfigurationAdapter.toRuntimeConfiguration(null));
        assertThrows(NullPointerException.class, () -> LegacyPaladinRuntimeSkillConfigurationAdapter.toLegacySkillState(null));
    }

    private static RuntimeSkillConfiguration paladin(String id, int rank, Map<String, String> selections) {
        return new RuntimeSkillConfiguration(new RuntimeSkillKey("paladin", id), rank, selections);
    }

    private static void assertRoundTrip(SkillState state) {
        var configuration = LegacyPaladinRuntimeSkillConfigurationAdapter.toRuntimeConfiguration(state);
        assertEquals(LegacyPaladinRuntimeSkillKeyAdapter.toRuntimeKey(state.getSkillId()), configuration.skillKey());
        assertEquals(state.getRank(), configuration.rank());
        assertFields(state, LegacyPaladinRuntimeSkillConfigurationAdapter.toLegacySkillState(configuration).orElseThrow());
    }

    private static void assertFields(SkillState expected, SkillState actual) {
        assertAll(
                () -> assertEquals(expected.getSkillId(), actual.getSkillId()),
                () -> assertEquals(expected.getRank(), actual.getRank()),
                () -> assertEquals(expected.isBaseUpgrade(), actual.isBaseUpgrade()),
                () -> assertEquals(expected.getChoiceUpgrade(), actual.getChoiceUpgrade()),
                () -> assertEquals(expected.getRuntimeModifierChoice(), actual.getRuntimeModifierChoice()),
                () -> assertEquals(expected.getChoiceGroup1(), actual.getChoiceGroup1()),
                () -> assertEquals(expected.getChoiceGroup2(), actual.getChoiceGroup2()),
                () -> assertEquals(expected.getChoiceGroup3(), actual.getChoiceGroup3()));
    }
}
