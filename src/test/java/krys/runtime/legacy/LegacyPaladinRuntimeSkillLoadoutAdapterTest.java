package krys.runtime.legacy;

import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.runtime.RuntimeSkillLoadout;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPaladinRuntimeSkillLoadoutAdapterTest {
    @Test
    void emptyAndNullLegacyInputsShouldPreserveEmptyPaladinSemantics() {
        var empty = LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(Map.of(), List.of());
        assertEquals("paladin", empty.classId());
        assertEquals(empty, LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(null, null));
        var view = LegacyPaladinRuntimeSkillLoadoutAdapter.toLegacyView(empty).orElseThrow();
        assertTrue(view.learnedSkills().isEmpty());
        assertTrue(view.actionBar().isEmpty());
        assertThrows(NullPointerException.class, () -> LegacyPaladinRuntimeSkillLoadoutAdapter.toLegacyView(null));
    }

    @Test
    void allFourSkillsAndClashIndependentChoicesShouldRoundTripEveryFieldAfterLegacyDeduplication() {
        var skills = goldenSkills();
        var actionBar = List.of(SkillId.ADVANCE, SkillId.CLASH, SkillId.ADVANCE, SkillId.HOLY_BOLT, SkillId.BRANDISH, SkillId.CLASH);
        var loadout = LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(skills, actionBar);
        assertEquals(List.of("paladin:natarcie", "paladin:starcie", "paladin:swiety_pocisk", "paladin:wymach"),
                loadout.actionBar().stream().map(RuntimeSkillKey::asExternalId).toList());
        var view = LegacyPaladinRuntimeSkillLoadoutAdapter.toLegacyView(loadout).orElseThrow();
        assertEquals(List.of(SkillId.ADVANCE, SkillId.CLASH, SkillId.HOLY_BOLT, SkillId.BRANDISH), view.actionBar());
        assertEquals(List.of(SkillId.values()), new ArrayList<>(view.learnedSkills().keySet()));
        assertEquals(skills.keySet(), view.learnedSkills().keySet());
        skills.forEach((id, state) -> assertFields(state, view.learnedSkills().get(id)));
        assertEquals(Map.of("legacy_base_upgrade", "enabled", "legacy_choice_upgrade", "left", "legacy_runtime_modifier", "animus",
                "grupa_1", "generowanie_wiary", "grupa_2", "zwiekszenie_obrazen", "grupa_3", "potyczka"),
                loadout.configuration(new RuntimeSkillKey("paladin", "starcie")).orElseThrow().selections());
    }

    @Test
    void assignedRankZeroShouldRoundTripOutsideActionBar() {
        var state = new SkillState(SkillId.ADVANCE, 0, false, SkillUpgradeChoice.NONE);
        var loadout = LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(Map.of(SkillId.ADVANCE, state), List.of());
        assertFields(state, LegacyPaladinRuntimeSkillLoadoutAdapter.toLegacyView(loadout).orElseThrow().learnedSkills().get(SkillId.ADVANCE));
        assertThrows(IllegalArgumentException.class, () -> LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(
                Map.of(SkillId.ADVANCE, state), List.of(SkillId.ADVANCE)));
    }

    @Test
    void nonPaladinLoadoutShouldReturnEmptyWithoutConvertingSkills() {
        var key = new RuntimeSkillKey("necromancer", "krwotok");
        var configuration = new RuntimeSkillConfiguration(key, 1, Map.of("grupa_1", "szybkosc_uzycia"));
        assertTrue(LegacyPaladinRuntimeSkillLoadoutAdapter.toLegacyView(new RuntimeSkillLoadout(
                "necromancer", Map.of(key, configuration), List.of(key))).isEmpty());
        assertTrue(LegacyPaladinRuntimeSkillLoadoutAdapter.toLegacyView(new RuntimeSkillLoadout("necromancer", Map.of(), List.of())).isEmpty());
    }

    @Test
    void unknownPaladinIdentityShouldThrowInsteadOfSilentlyDroppingConfiguration() {
        var key = new RuntimeSkillKey("paladin", "unknown");
        var configuration = new RuntimeSkillConfiguration(key, 1, Map.of());
        for (var actionBar : List.of(List.<RuntimeSkillKey>of(), List.of(key))) {
            assertThrows(IllegalArgumentException.class, () -> LegacyPaladinRuntimeSkillLoadoutAdapter.toLegacyView(
                    new RuntimeSkillLoadout("paladin", Map.of(key, configuration), actionBar)));
        }
    }

    @Test
    void invalidLegacyConfigurationShouldThrowInsteadOfLosingItsSelections() {
        var key = new RuntimeSkillKey("paladin", "wymach");
        for (var configuration : List.of(new RuntimeSkillConfiguration(key, 6, Map.of()),
                new RuntimeSkillConfiguration(key, 1, Map.of("foo", "bar")))) {
            assertThrows(IllegalArgumentException.class, () -> LegacyPaladinRuntimeSkillLoadoutAdapter.toLegacyView(
                    new RuntimeSkillLoadout("paladin", Map.of(key, configuration), List.of(key))));
        }
    }

    @Test
    void mismatchedMapKeyAndMissingActionBarStateShouldBeRejected() {
        var state = new SkillState(SkillId.BRANDISH, 1, false, SkillUpgradeChoice.NONE);
        assertThrows(IllegalArgumentException.class, () -> LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(
                Map.of(SkillId.HOLY_BOLT, state), List.of()));
        assertThrows(IllegalArgumentException.class, () -> LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(
                Map.of(SkillId.BRANDISH, state), List.of(SkillId.ADVANCE)));
    }

    @Test
    void nullLegacyEntriesShouldNotBeSilentlyDropped() {
        var nullKey = new HashMap<SkillId, SkillState>();
        nullKey.put(null, new SkillState(SkillId.BRANDISH, 1, false, SkillUpgradeChoice.NONE));
        var nullState = new HashMap<SkillId, SkillState>();
        nullState.put(SkillId.BRANDISH, null);
        assertThrows(NullPointerException.class, () -> LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(nullKey, List.of()));
        assertThrows(NullPointerException.class, () -> LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(nullState, List.of()));
        var nullBar = new ArrayList<SkillId>();
        nullBar.add(null);
        assertThrows(NullPointerException.class, () -> LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(Map.of(), nullBar));
    }

    @Test
    void legacyViewShouldBeDefensiveAndImmutable() {
        var skills = goldenSkills();
        var bar = new ArrayList<>(List.of(SkillId.CLASH, SkillId.ADVANCE));
        var view = new LegacyPaladinRuntimeSkillLoadoutAdapter.LegacyView(skills, bar);
        skills.clear();
        bar.clear();
        assertEquals(4, view.learnedSkills().size());
        assertEquals(List.of(SkillId.CLASH, SkillId.ADVANCE), view.actionBar());
        assertThrows(UnsupportedOperationException.class, () -> view.learnedSkills().clear());
        assertThrows(UnsupportedOperationException.class, () -> view.actionBar().add(SkillId.BRANDISH));
    }

    private static EnumMap<SkillId, SkillState> goldenSkills() {
        var skills = new EnumMap<SkillId, SkillState>(SkillId.class);
        skills.put(SkillId.BRANDISH, new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.LEFT));
        skills.put(SkillId.HOLY_BOLT, new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.MIDDLE));
        skills.put(SkillId.CLASH, new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT, SkillRuntimeModifierChoice.ANIMUS,
                "generowanie_wiary", "zwiekszenie_obrazen", "potyczka"));
        skills.put(SkillId.ADVANCE, new SkillState(SkillId.ADVANCE, 1, false, SkillUpgradeChoice.NONE));
        return skills;
    }

    private static void assertFields(SkillState expected, SkillState actual) {
        assertNotNull(actual);
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
