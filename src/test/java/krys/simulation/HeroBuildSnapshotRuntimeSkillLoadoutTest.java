package krys.simulation;

import krys.hero.Hero;
import krys.hero.HeroClass;
import krys.hero.HeroClassStatBaselines;
import krys.item.EquipmentSlot;
import krys.item.Item;
import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.runtime.RuntimeSkillLoadout;
import krys.runtime.legacy.LegacyPaladinRuntimeSkillLoadoutAdapter;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class HeroBuildSnapshotRuntimeSkillLoadoutTest {
    @ParameterizedTest
    @MethodSource("legacyConstructors")
    void allLegacySnapshotConstructorsShouldKeepDefaultsAndClashFields(HeroBuildSnapshot snapshot) {
        assertEquals("paladin", snapshot.getRuntimeSkillLoadout().classId());
        assertEquals(List.of(SkillId.CLASH), snapshot.getSelectedSkillBar());
        assertFields(clash(), snapshot.getSkillState(SkillId.CLASH));
        assertNull(snapshot.getSkillState(SkillId.ADVANCE));
        assertNull(snapshot.getSkillState(null));
        assertEquals(HeroClassStatBaselines.find(HeroClass.PALADIN, 13)
                .map(baseline -> baseline.getCriticalChancePercent().doubleValue()).orElse(0.0d), snapshot.getCriticalChancePercent());
        assertEquals(HeroBuildSnapshot.DEFAULT_SIMULATION_SEED, snapshot.getSimulationSeed());
    }

    @Test
    void canonicalConstructorShouldKeepLoadoutIdentityAndOtherSnapshotFields() {
        var loadout = LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(Map.of(SkillId.CLASH, clash()), List.of(SkillId.CLASH));
        var hero = hero();
        var items = new ArrayList<>(List.of(new Item(1, "Broń testowa", EquipmentSlot.MAIN_HAND, List.of())));
        var aspects = new ArrayList<>(List.of("aspect_a", "aspect_a", "aspect_b"));
        var snapshot = new HeroBuildSnapshot(hero, 2, 8, 0, items, true, false, loadout,
                30, 100, 1.5, "moloch", 3, 8, aspects, 25, 42);
        items.clear();
        aspects.clear();
        assertSame(loadout, snapshot.getRuntimeSkillLoadout());
        assertSame(hero, snapshot.getHero());
        assertEquals(2, snapshot.getBonusSkillPoints());
        assertEquals(8, snapshot.getAverageWeaponDamage());
        assertEquals(0, snapshot.getTotalPercentDamageBonus());
        assertEquals(1, snapshot.getEquippedItems().size());
        assertTrue(snapshot.hasActiveWeapon());
        assertFalse(snapshot.hasActiveShield());
        assertEquals(30, snapshot.getInitialPrimaryResource());
        assertEquals(100, snapshot.getMaxPrimaryResource());
        assertEquals(1.5, snapshot.getPrimaryResourceRegenPerSecond());
        assertEquals("moloch", snapshot.getSelectedPaladinOathId());
        assertEquals(3, snapshot.getInitialAnimus());
        assertEquals(8, snapshot.getMaxAnimus());
        assertEquals(List.of("aspect_a", "aspect_b"), snapshot.getActiveAspectIds());
        assertEquals(25, snapshot.getCriticalChancePercent());
        assertEquals(42, snapshot.getSimulationSeed());
        assertFields(clash(), snapshot.getLearnedSkills().get(SkillId.CLASH));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getLearnedSkills().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getSelectedSkillBar().add(SkillId.ADVANCE));
    }

    @Test
    void nullLegacyCollectionsAndHeroShouldKeepHistoricalEmptyDefaults() {
        var snapshot = new HeroBuildSnapshot(null, 0, 8, 0, null, null, null);
        assertNull(snapshot.getHero());
        assertTrue(snapshot.getEquippedItems().isEmpty());
        assertTrue(snapshot.getLearnedSkills().isEmpty());
        assertTrue(snapshot.getSelectedSkillBar().isEmpty());
        assertFalse(snapshot.hasActiveWeapon());
        assertFalse(snapshot.hasActiveShield());
        assertEquals(0, snapshot.getCriticalChancePercent());
        assertEquals(HeroBuildSnapshot.DEFAULT_INITIAL_PRIMARY_RESOURCE, snapshot.getInitialPrimaryResource());
        assertEquals(HeroBuildSnapshot.DEFAULT_MAX_PRIMARY_RESOURCE, snapshot.getMaxPrimaryResource());
        assertEquals(HeroBuildSnapshot.DEFAULT_PRIMARY_RESOURCE_REGEN_PER_SECOND, snapshot.getPrimaryResourceRegenPerSecond());
        assertEquals(HeroBuildSnapshot.DEFAULT_SELECTED_PALADIN_OATH_ID, snapshot.getSelectedPaladinOathId());
        assertEquals(HeroBuildSnapshot.DEFAULT_INITIAL_ANIMUS, snapshot.getInitialAnimus());
        assertEquals(HeroBuildSnapshot.DEFAULT_MAX_ANIMUS, snapshot.getMaxAnimus());
    }

    @Test
    void legacySnapshotInputShouldBeDefensiveAndAllowRankZeroOffBar() {
        var unlearned = new SkillState(SkillId.ADVANCE, 0, false, SkillUpgradeChoice.NONE);
        var skills = new java.util.EnumMap<SkillId, SkillState>(SkillId.class);
        skills.put(SkillId.CLASH, clash());
        skills.put(SkillId.ADVANCE, unlearned);
        var bar = new ArrayList<>(List.of(SkillId.CLASH, SkillId.CLASH));
        var snapshot = new HeroBuildSnapshot(hero(), 0, 8, 0, List.of(), skills, bar);
        skills.clear();
        bar.clear();
        assertEquals(List.of(SkillId.CLASH), snapshot.getSelectedSkillBar());
        assertFields(unlearned, snapshot.getSkillState(SkillId.ADVANCE));
        assertFields(clash(), snapshot.getSkillState(SkillId.CLASH));
        assertThrows(IllegalArgumentException.class, () -> new HeroBuildSnapshot(hero(), 0, 8, 0, List.of(),
                Map.of(SkillId.ADVANCE, unlearned), List.of(SkillId.ADVANCE)));
        assertThrows(IllegalArgumentException.class, () -> new HeroBuildSnapshot(hero(), 0, 8, 0, List.of(),
                Map.of(SkillId.BRANDISH, clash()), List.of()));
    }

    @Test
    void canonicalPayloadShouldRequireNonNullAndLegacyViewShouldRejectOtherClassesExplicitly() {
        assertThrows(NullPointerException.class, () -> canonical(null, 0));
        var loadout = new RuntimeSkillLoadout("necromancer", Map.of(), List.of());
        var snapshot = canonical(loadout, 0);
        assertSame(loadout, snapshot.getRuntimeSkillLoadout());
        assertThrows(UnsupportedOperationException.class, snapshot::getLearnedSkills);
        assertThrows(UnsupportedOperationException.class, snapshot::getSelectedSkillBar);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getSkillState(SkillId.CLASH));
    }

    @Test
    void criticalValidationAndBlankOathShouldKeepExistingSemantics() {
        var loadout = new RuntimeSkillLoadout("paladin", Map.of(), List.of());
        assertThrows(IllegalArgumentException.class, () -> canonical(loadout, -1));
        assertThrows(IllegalArgumentException.class, () -> canonical(loadout, 101));
        assertEquals("NONE", canonical(loadout, 0).getSelectedPaladinOathId());
        assertTrue(canonical(loadout, 0).getActiveAspectIds().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new HeroBuildSnapshot(hero(), 0, 8, 0, List.of(), false, false,
                Map.of(), List.of(), 100, 100, 1.5, "NONE", 8, 8, List.of(), 101, 1));
    }

    @Test
    void holyBoltOnActionBarWithoutConfigurationShouldBeRejectedAtEveryPayloadBoundary() {
        var key = new RuntimeSkillKey("paladin", "swiety_pocisk");
        assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillLoadout("paladin", Map.of(), List.of(key)));
        assertThrows(IllegalArgumentException.class, () -> LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(
                Map.of(), List.of(SkillId.HOLY_BOLT)));
        assertThrows(IllegalArgumentException.class, () -> new HeroBuildSnapshot(hero(), 0, 8, 0, List.of(),
                Map.of(), List.of(SkillId.HOLY_BOLT)));
    }

    @Test
    void holyBoltRankZeroOnActionBarShouldBeRejectedWithoutPromotingRankOrRemovingSkill() {
        var key = new RuntimeSkillKey("paladin", "swiety_pocisk");
        var configuration = new RuntimeSkillConfiguration(key, 0, Map.of());
        var state = new SkillState(SkillId.HOLY_BOLT, 0, false, SkillUpgradeChoice.NONE);
        var skills = Map.of(SkillId.HOLY_BOLT, state);
        var actionBar = List.of(SkillId.HOLY_BOLT);
        assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillLoadout("paladin", Map.of(key, configuration), List.of(key)));
        assertThrows(IllegalArgumentException.class, () -> LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(skills, actionBar));
        assertThrows(IllegalArgumentException.class, () -> new HeroBuildSnapshot(hero(), 0, 8, 0, List.of(), skills, actionBar));
        assertEquals(0, state.getRank());
        assertEquals(List.of(SkillId.HOLY_BOLT), actionBar);
        assertSame(state, skills.get(SkillId.HOLY_BOLT));
    }

    private static Stream<HeroBuildSnapshot> legacyConstructors() {
        var hero = hero();
        var skills = Map.of(SkillId.CLASH, clash());
        var bar = List.of(SkillId.CLASH, SkillId.CLASH);
        var critical = HeroClassStatBaselines.find(HeroClass.PALADIN, 13)
                .map(baseline -> baseline.getCriticalChancePercent().doubleValue()).orElse(0.0d);
        return Stream.of(
                new HeroBuildSnapshot(hero, 0, 8, 0, List.of(), skills, bar),
                new HeroBuildSnapshot(hero, 0, 8, 0, List.of(), false, false, skills, bar),
                new HeroBuildSnapshot(hero, 0, 8, 0, List.of(), false, false, skills, bar, List.of()),
                new HeroBuildSnapshot(hero, 0, 8, 0, List.of(), false, false, skills, bar, 100, 100, 1.5, List.of()),
                new HeroBuildSnapshot(hero, 0, 8, 0, List.of(), false, false, skills, bar, 100, 100, 1.5, "NONE", 8, 8, List.of()),
                new HeroBuildSnapshot(hero, 0, 8, 0, List.of(), false, false, skills, bar, 100, 100, 1.5, "NONE", 8, 8, List.of(), critical, 1));
    }

    private static HeroBuildSnapshot canonical(RuntimeSkillLoadout loadout, double critical) {
        return new HeroBuildSnapshot(hero(), 0, 8, 0, null, false, false, loadout,
                100, 100, 1.5, " ", 8, 8, null, critical, 1);
    }

    private static Hero hero() {
        return new Hero(1, "Paladyn testowy", 13, HeroClass.PALADIN);
    }

    private static SkillState clash() {
        return new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT, SkillRuntimeModifierChoice.ANIMUS,
                "generowanie_wiary", "zwiekszenie_obrazen", "potyczka");
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
