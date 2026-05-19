package krys.simulation;

import krys.app.SampleBuildFactory;
import krys.combat.DamageEngine;
import krys.hero.Hero;
import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.item.Item;
import krys.item.ItemStat;
import krys.item.ItemStatType;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClashManualSimulationTest {
    private final ManualSimulationService simulationService = new ManualSimulationService(new DamageEngine());

    @Test
    void starcie_bez_aktywnej_tarczy_nie_powinno_zadawac_obrazen() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                clashSnapshot(1664L, true, false),
                10
        );

        assertEquals(0L, result.getTotalDamage());
        assertEquals(0.0d, result.getDps(), 0.0000001d);
        assertTrue(result.getDirectHitDebugSnapshots().isEmpty());
        assertEquals("WAIT", result.getStepTrace().getFirst().getActionName());
        assertTrue(result.getStepTrace().getFirst().getSelectionReason().contains("brak aktywnej tarczy"));
        assertFalse(result.getStepTrace().getFirst().getSkillBarStates().getFirst().isLegalCandidate());
    }

    @Test
    void starcie_bez_aktywnej_broni_nie_powinno_zadawac_obrazen() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                clashSnapshot(0L, false, true),
                10
        );

        assertEquals(0L, result.getTotalDamage());
        assertEquals(0.0d, result.getDps(), 0.0000001d);
        assertTrue(result.getDirectHitDebugSnapshots().isEmpty());
        assertTrue(result.getStepTrace().getFirst().getSelectionReason().contains("brak aktywnej broni"));
    }

    @Test
    void natarcie_bez_aktywnej_tarczy_nadal_jest_legalne() {
        HeroBuildSnapshot snapshot = snapshot(
                new SkillState(SkillId.ADVANCE, 1, false, SkillUpgradeChoice.NONE),
                1664L,
                true,
                false
        );

        SimulationResult result = simulationService.calculateCurrentBuild(snapshot, 10);

        assertTrue(result.getTotalDamage() > 0L);
        assertFalse(result.getDirectHitDebugSnapshots().isEmpty());
        assertEquals(SkillId.ADVANCE, result.getDirectHitDebugSnapshots().getFirst().getSkillId());
    }

    @Test
    void powinien_wykonac_podstawowy_use_case_clash_w_manual_simulation() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceReactiveCurrentBuild(
                        new SkillState(SkillId.CLASH, 5, false, SkillUpgradeChoice.NONE)
                ),
                9
        );

        assertEquals(276L, result.getTotalDamage());
        assertEquals(123L, result.getTotalReactiveDamage());
        assertEquals(3, result.getReactiveHitBreakdowns().size());
        assertEquals("Clash", result.getStepTrace().get(0).getActionName());
        assertEquals(17L, result.getStepTrace().get(0).getDirectDamage());
        assertFalse(result.isResolveActiveAtEnd());
        assertEquals(0.50d, result.getActiveBlockChanceAtEnd(), 0.0000001d);
        assertEquals(0.0d, result.getActiveThornsBonusAtEnd(), 0.0000001d);
    }

    @Test
    void crusaders_march_powinno_dawac_block_chance_i_resolve() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceReactiveCurrentBuild(
                        new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.NONE)
                ),
                9
        );

        assertEquals(288L, result.getTotalDamage());
        assertEquals(135L, result.getTotalReactiveDamage());
        assertTrue(result.isResolveActiveAtEnd());
        assertEquals(0.75d, result.getActiveBlockChanceAtEnd(), 0.0000001d);
        assertEquals(0.0d, result.getActiveThornsBonusAtEnd(), 0.0000001d);
        assertEquals(45L, result.getReactiveHitBreakdowns().get(0).getReactiveFinalDamage());
        assertEquals(20L, result.getReactiveHitBreakdowns().get(0).getRetributionExpectedRawDamage());
        assertEquals(12L, result.getReactiveHitBreakdowns().get(0).getRetributionExpectedFinalDamage());
        assertTrue(result.getReactiveHitBreakdowns().get(0).isResolveActive());
        assertEquals(2, result.getReactiveHitBreakdowns().get(0).getResolveRemainingSeconds());
    }

    @Test
    void punishment_powinno_dawac_bonus_do_thorns_i_podbijac_reactive() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceReactiveCurrentBuild(
                        new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT)
                ),
                9
        );

        assertEquals(420L, result.getTotalDamage());
        assertEquals(267L, result.getTotalReactiveDamage());
        assertTrue(result.isResolveActiveAtEnd());
        assertEquals(0.75d, result.getActiveBlockChanceAtEnd(), 0.0000001d);
        assertEquals(50.0d, result.getActiveThornsBonusAtEnd(), 0.0000001d);
        assertEquals(105L, result.getReactiveHitBreakdowns().get(0).getThornsRawDamage());
        assertEquals(65L, result.getReactiveHitBreakdowns().get(0).getThornsFinalDamage());
        assertEquals(39L, result.getReactiveHitBreakdowns().get(0).getRetributionExpectedRawDamage());
        assertEquals(24L, result.getReactiveHitBreakdowns().get(0).getRetributionExpectedFinalDamage());
        assertEquals(89L, result.getReactiveHitBreakdowns().get(0).getReactiveFinalDamage());
        assertTrue(result.getReactiveHitBreakdowns().get(0).isPunishmentActive());
    }

    @Test
    void clash_crusaders_march_i_punishment_powinny_podbijac_reactive_w_runtime_manual_simulation() {
        SimulationResult baseClash = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceReactiveCurrentBuild(
                        new SkillState(SkillId.CLASH, 5, false, SkillUpgradeChoice.NONE)
                ),
                9
        );
        SimulationResult clashWithResolve = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceReactiveCurrentBuild(
                        new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.NONE)
                ),
                9
        );
        SimulationResult clashWithResolveAndPunishment = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceReactiveCurrentBuild(
                        new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT)
                ),
                9
        );

        assertEquals(41L, baseClash.getReactiveHitBreakdowns().get(0).getReactiveFinalDamage());
        assertEquals(45L, clashWithResolve.getReactiveHitBreakdowns().get(0).getReactiveFinalDamage());
        assertEquals(89L, clashWithResolveAndPunishment.getReactiveHitBreakdowns().get(0).getReactiveFinalDamage());

        assertEquals(123L, baseClash.getTotalReactiveDamage());
        assertEquals(135L, clashWithResolve.getTotalReactiveDamage());
        assertEquals(267L, clashWithResolveAndPunishment.getTotalReactiveDamage());

        assertFalse(baseClash.getReactiveHitBreakdowns().get(0).isResolveActive());
        assertTrue(clashWithResolve.getReactiveHitBreakdowns().get(0).isResolveActive());
        assertTrue(clashWithResolveAndPunishment.getReactiveHitBreakdowns().get(0).isResolveActive());
        assertTrue(clashWithResolveAndPunishment.getReactiveHitBreakdowns().get(0).isPunishmentActive());
    }

    private static HeroBuildSnapshot clashSnapshot(long weaponDamage, boolean hasActiveWeapon, boolean hasActiveShield) {
        return snapshot(
                new SkillState(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE),
                weaponDamage,
                hasActiveWeapon,
                hasActiveShield
        );
    }

    private static HeroBuildSnapshot snapshot(SkillState skillState,
                                              long weaponDamage,
                                              boolean hasActiveWeapon,
                                              boolean hasActiveShield) {
        Hero hero = new Hero(1, "Paladyn", 70, HeroClass.PALADIN);
        List<Item> items = List.of(
                new Item(1, "Techniczna broń", EquipmentSlot.MAIN_HAND, List.of(
                        new ItemStat(ItemStatType.CRIT_DAMAGE, 1.5d)
                )),
                new Item(2, "Techniczna tarcza", EquipmentSlot.OFF_HAND, List.of(
                        new ItemStat(ItemStatType.MAIN_HAND_WEAPON_DAMAGE, 100.0d)
                ))
        );
        return new HeroBuildSnapshot(
                hero,
                0,
                weaponDamage,
                0.0d,
                items,
                hasActiveWeapon,
                hasActiveShield,
                Map.of(skillState.getSkillId(), skillState),
                List.of(skillState.getSkillId())
        );
    }
}
