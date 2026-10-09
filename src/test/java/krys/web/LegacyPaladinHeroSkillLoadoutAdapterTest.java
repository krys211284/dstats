package krys.web;

import krys.runtime.RuntimeSkillLoadout;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LegacyPaladinHeroSkillLoadoutAdapterTest {
    static HeroSkillLoadout paladinLoadout() {
        return new HeroSkillLoadout(Map.of(
                SkillId.ADVANCE, new HeroAssignedSkill(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT),
                SkillId.CLASH, new HeroAssignedSkill(SkillId.CLASH, 3, true, SkillUpgradeChoice.LEFT,
                        SkillRuntimeModifierChoice.ANIMUS, "generowanie_wiary", "zwiekszenie_obrazen", "potyczka"),
                SkillId.HOLY_BOLT, new HeroAssignedSkill(SkillId.HOLY_BOLT, 0, false, SkillUpgradeChoice.NONE),
                SkillId.BRANDISH, new HeroAssignedSkill(SkillId.BRANDISH, 1, false, SkillUpgradeChoice.NONE)),
                List.of(SkillId.CLASH, SkillId.ADVANCE, SkillId.BRANDISH));
    }

    static void assertFields(HeroAssignedSkill expected, HeroAssignedSkill actual) {
        assertEquals(expected.getSkillId(), actual.getSkillId());
        assertEquals(expected.getRank(), actual.getRank());
        assertEquals(expected.isBaseUpgrade(), actual.isBaseUpgrade());
        assertEquals(expected.getChoiceUpgrade(), actual.getChoiceUpgrade());
        assertEquals(expected.getRuntimeModifierChoice(), actual.getRuntimeModifierChoice());
        assertEquals(expected.getChoiceGroup1(), actual.getChoiceGroup1());
        assertEquals(expected.getChoiceGroup2(), actual.getChoiceGroup2());
        assertEquals(expected.getChoiceGroup3(), actual.getChoiceGroup3());
    }

    @Test
    void roundTripShouldPreserveAllEightFieldsIncludingIndependentClashChoicesAndRankZero() {
        var original = paladinLoadout();
        var canonical = LegacyPaladinHeroSkillLoadoutAdapter.toRuntimeLoadout(original);
        var restored = LegacyPaladinHeroSkillLoadoutAdapter.toLegacyLoadout(canonical);
        assertEquals("paladin", canonical.classId());
        for (var skill : original.getAssignedSkills().values()) {
            assertFields(skill, restored.getAssignedSkill(skill.getSkillId()));
        }
        assertEquals(original.getActionBarSkills(), restored.getActionBarSkills());
        assertEquals(canonical, LegacyPaladinHeroSkillLoadoutAdapter.toRuntimeLoadout(restored));
    }

    @Test
    void foundationDefaultShouldRemainIdenticalToTheCurrentDefaultForm() {
        var canonical = LegacyPaladinHeroSkillLoadoutAdapter.toRuntimeLoadout(HeroSkillLoadout.foundationDefault());
        var expected = HeroSkillLoadout.fromCurrentBuildFormData(CurrentBuildFormData.defaultValues());
        var restored = LegacyPaladinHeroSkillLoadoutAdapter.toLegacyLoadout(canonical);
        assertFields(expected.getAssignedSkill(SkillId.ADVANCE), restored.getAssignedSkill(SkillId.ADVANCE));
        assertEquals(expected.getActionBarSkills(), restored.getActionBarSkills());
        assertEquals(List.of(SkillId.ADVANCE), LegacyPaladinHeroSkillLoadoutAdapter.toLegacyLoadout(canonical).getAssignedSkillIds());
    }

    @Test
    void assignedRankZeroAdvanceShouldBePreservedOutsideTheActionBar() {
        var original = new HeroSkillLoadout(Map.of(SkillId.ADVANCE,
                new HeroAssignedSkill(SkillId.ADVANCE, 0, false, SkillUpgradeChoice.NONE)), List.of());
        var canonical = LegacyPaladinHeroSkillLoadoutAdapter.toRuntimeLoadout(original);
        var restored = LegacyPaladinHeroSkillLoadoutAdapter.toLegacyLoadout(canonical);
        assertFields(original.getAssignedSkill(SkillId.ADVANCE), restored.getAssignedSkill(SkillId.ADVANCE));
        assertTrue(canonical.actionBar().isEmpty());
        assertEquals(1, canonical.configurations().size());
    }

    @Test
    void bridgeShouldRejectMismatchedLegacyKeysAndUnsupportedClasses() {
        var mismatch = new HeroSkillLoadout(Map.of(SkillId.ADVANCE,
                new HeroAssignedSkill(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE)), List.of());
        assertThrows(IllegalArgumentException.class, () -> LegacyPaladinHeroSkillLoadoutAdapter.toRuntimeLoadout(mismatch));
        assertThrows(UnsupportedOperationException.class, () -> LegacyPaladinHeroSkillLoadoutAdapter.toLegacyLoadout(
                new RuntimeSkillLoadout("necromancer", Map.of(), List.of())));
    }
}
