package krys.app;

import krys.hero.HeroClass;
import krys.hero.HeroClassStatBaselines;
import krys.runtime.RuntimeSkillKey;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CurrentBuildRequestRuntimeSkillLoadoutTest {
    @ParameterizedTest
    @MethodSource("legacyConstructors")
    void everyLegacyConstructorShouldKeepCanonicalSkillsDefaultsAndDeduplicatedOrder(CurrentBuildRequest request) {
        assertEquals("paladin", request.getRuntimeSkillLoadout().classId());
        assertEquals(List.of(new RuntimeSkillKey("paladin", "starcie"), new RuntimeSkillKey("paladin", "wymach")),
                request.getRuntimeSkillLoadout().actionBar());
        assertEquals(List.of(SkillId.CLASH, SkillId.BRANDISH), request.getActionBar());
        assertEquals(goldenSkills().keySet(), request.getLearnedSkills().keySet());
        goldenSkills().forEach((id, state) -> assertFields(state, request.getLearnedSkills().get(id)));
        assertEquals(HeroClassStatBaselines.find(HeroClass.PALADIN, request.getLevel())
                .map(baseline -> baseline.getCriticalChancePercent().doubleValue()).orElse(0.0d), request.getCriticalChancePercent());
        assertEquals(CurrentBuildRequest.DEFAULT_SIMULATION_SEED, request.getSimulationSeed());
    }

    @ParameterizedTest
    @EnumSource(value = CurrentBuildReferenceScenario.class, names = {"HOLY_BOLT_JUDGEMENT", "CLASH_PUNISHMENT", "ADVANCE_FLASH"})
    void existingReferenceRequestsShouldTransportSameCanonicalLoadoutIntoSnapshot(CurrentBuildReferenceScenario scenario) {
        var request = CurrentBuildReferenceRequests.create(scenario);
        var snapshot = new CurrentBuildSnapshotFactory().create(request);
        assertSame(request.getRuntimeSkillLoadout(), snapshot.getRuntimeSkillLoadout());
        assertEquals(request.getActionBar(), snapshot.getSelectedSkillBar());
        assertEquals(request.getLearnedSkills().keySet(), snapshot.getLearnedSkills().keySet());
        request.getLearnedSkills().forEach((id, state) -> assertFields(state, snapshot.getSkillState(id)));
    }

    @Test
    void clashIndependentLegacyAndGroupFactsShouldSurviveRequestFactoryAndSnapshot() {
        var request = request(goldenSkills(), goldenBar());
        var snapshot = new CurrentBuildSnapshotFactory().create(request);
        assertSame(request.getRuntimeSkillLoadout(), snapshot.getRuntimeSkillLoadout());
        assertEquals(Map.of("legacy_base_upgrade", "enabled", "legacy_choice_upgrade", "left", "legacy_runtime_modifier", "animus",
                "grupa_1", "generowanie_wiary", "grupa_2", "zwiekszenie_obrazen", "grupa_3", "potyczka"),
                snapshot.getRuntimeSkillLoadout().configuration(new RuntimeSkillKey("paladin", "starcie")).orElseThrow().selections());
        goldenSkills().forEach((id, state) -> assertFields(state, snapshot.getSkillState(id)));
        assertEquals(List.of(SkillId.CLASH, SkillId.BRANDISH), snapshot.getSelectedSkillBar());
    }

    @Test
    void withMethodsShouldKeepSameLoadoutAndEveryUnchangedRequestField() throws Exception {
        var source = new CurrentBuildRequest(20, 11, 30, 26, 60, 40, 20, true, false,
                goldenSkills(), goldenBar(), 15, 30, 100, 1.5, "moloch", 3, 8,
                List.of("aspect_a", "aspect_b"), 25, 42);
        var aspects = source.withActiveAspectIds(List.of("aspect_c", "aspect_c", "aspect_a"));
        var critical = source.withCriticalChancePercent(50);
        for (var changed : List.of(aspects, critical)) {
            assertSame(source.getRuntimeSkillLoadout(), changed.getRuntimeSkillLoadout());
            for (var field : CurrentBuildRequest.class.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !Set.of("activeAspectIds", "criticalChancePercent").contains(field.getName())) {
                    field.setAccessible(true);
                    assertEquals(field.get(source), field.get(changed), field.getName());
                }
            }
        }
        assertEquals(List.of("aspect_c", "aspect_a"), aspects.getActiveAspectIds());
        assertEquals(source.getCriticalChancePercent(), aspects.getCriticalChancePercent());
        assertEquals(source.getActiveAspectIds(), critical.getActiveAspectIds());
        assertEquals(50, critical.getCriticalChancePercent());
        assertEquals(List.of(), source.withActiveAspectIds(null).getActiveAspectIds());
        assertThrows(IllegalArgumentException.class, () -> source.withCriticalChancePercent(-1));
        assertThrows(IllegalArgumentException.class, () -> source.withCriticalChancePercent(101));
    }

    @Test
    void requestShouldDefensivelyCopyInputAndExposeImmutableLegacyViews() {
        var skills = goldenSkills();
        var bar = new ArrayList<>(goldenBar());
        var request = request(skills, bar);
        skills.clear();
        bar.clear();
        assertEquals(2, request.getRuntimeSkillLoadout().configurations().size());
        assertEquals(List.of(SkillId.CLASH, SkillId.BRANDISH), request.getActionBar());
        assertThrows(UnsupportedOperationException.class, () -> request.getLearnedSkills().clear());
        assertThrows(UnsupportedOperationException.class, () -> request.getActionBar().add(SkillId.ADVANCE));
    }

    @Test
    void nullLegacyCollectionsShouldKeepEmptyPaladinDefaults() {
        var request = request(null, null);
        assertEquals("paladin", request.getRuntimeSkillLoadout().classId());
        assertTrue(request.getLearnedSkills().isEmpty());
        assertTrue(request.getActionBar().isEmpty());
        assertEquals(CurrentBuildRequest.DEFAULT_INITIAL_PRIMARY_RESOURCE, request.getInitialPrimaryResource());
        assertEquals(CurrentBuildRequest.DEFAULT_MAX_PRIMARY_RESOURCE, request.getMaxPrimaryResource());
        assertEquals(CurrentBuildRequest.DEFAULT_PRIMARY_RESOURCE_REGEN_PER_SECOND, request.getPrimaryResourceRegenPerSecond());
        assertEquals(CurrentBuildRequest.DEFAULT_SELECTED_PALADIN_OATH_ID, request.getSelectedPaladinOathId());
        assertEquals(CurrentBuildRequest.DEFAULT_INITIAL_ANIMUS, request.getInitialAnimus());
        assertEquals(CurrentBuildRequest.DEFAULT_MAX_ANIMUS, request.getMaxAnimus());
    }

    @Test
    void availableLevel70CriticalBaselineShouldSurviveRequestAndSnapshotTransport() {
        var request = new CurrentBuildRequest(70, 11, 79, 76, 0, 0, 0, goldenSkills(), goldenBar(), 15);
        double expected = HeroClassStatBaselines.find(HeroClass.PALADIN, 70).orElseThrow().getCriticalChancePercent().doubleValue();
        assertEquals(expected, request.getCriticalChancePercent());
        assertEquals(expected, new CurrentBuildSnapshotFactory().create(request).getCriticalChancePercent());
    }

    @Test
    void legacyActionBarValidationAndMapMismatchRejectionShouldRemainExplicit() {
        var unlearned = new SkillState(SkillId.ADVANCE, 0, false, SkillUpgradeChoice.NONE);
        var request = request(Map.of(SkillId.ADVANCE, unlearned), List.of());
        assertEquals(0, request.getLearnedSkills().get(SkillId.ADVANCE).getRank());
        assertThrows(IllegalArgumentException.class, () -> request(Map.of(SkillId.ADVANCE, unlearned), List.of(SkillId.ADVANCE)));
        assertThrows(IllegalArgumentException.class, () -> request(Map.of(), List.of(SkillId.ADVANCE)));
        assertThrows(IllegalArgumentException.class, () -> request(Map.of(SkillId.ADVANCE, goldenSkills().get(SkillId.BRANDISH)), List.of()));
        var nullBar = new ArrayList<SkillId>();
        nullBar.add(null);
        assertThrows(IllegalArgumentException.class, () -> request(Map.of(), nullBar));
    }

    private static Stream<CurrentBuildRequest> legacyConstructors() {
        var skills = goldenSkills();
        var bar = goldenBar();
        var critical = HeroClassStatBaselines.find(HeroClass.PALADIN, 20)
                .map(baseline -> baseline.getCriticalChancePercent().doubleValue()).orElse(0.0d);
        return Stream.of(
                request(skills, bar),
                new CurrentBuildRequest(20, 11, 30, 26, 60, 40, 20, skills, bar, 15, List.of()),
                new CurrentBuildRequest(20, 11, 30, 26, 60, 40, 20, true, true, skills, bar, 15),
                new CurrentBuildRequest(20, 11, 30, 26, 60, 40, 20, true, true, skills, bar, 15, List.of()),
                new CurrentBuildRequest(20, 11, 30, 26, 60, 40, 20, true, true, skills, bar, 15, 100, 100, 1.5, List.of()),
                new CurrentBuildRequest(20, 11, 30, 26, 60, 40, 20, true, true, skills, bar, 15, 100, 100, 1.5, "NONE", 8, 8, List.of()),
                new CurrentBuildRequest(20, 11, 30, 26, 60, 40, 20, true, true, skills, bar, 15, 100, 100, 1.5, "NONE", 8, 8, List.of(), critical, 1));
    }

    private static CurrentBuildRequest request(Map<SkillId, SkillState> skills, List<SkillId> actionBar) {
        return new CurrentBuildRequest(20, 11, 30, 26, 60, 40, 20, skills, actionBar, 15);
    }

    private static EnumMap<SkillId, SkillState> goldenSkills() {
        var skills = new EnumMap<SkillId, SkillState>(SkillId.class);
        skills.put(SkillId.BRANDISH, new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.LEFT));
        skills.put(SkillId.CLASH, new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT, SkillRuntimeModifierChoice.ANIMUS,
                "generowanie_wiary", "zwiekszenie_obrazen", "potyczka"));
        return skills;
    }

    private static List<SkillId> goldenBar() {
        return List.of(SkillId.CLASH, SkillId.BRANDISH, SkillId.CLASH);
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
