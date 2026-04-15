package krys.simulation;

import krys.app.SampleBuildFactory;
import krys.combat.DamageEngine;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvanceManualSimulationTest {
    private final ManualSimulationService simulationService = new ManualSimulationService(new DamageEngine());

    @Test
    void powinien_wykonac_bazowy_advance_w_manual_simulation() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceCurrentBuild(
                        new SkillState(SkillId.ADVANCE, 5, false, SkillUpgradeChoice.NONE)
                ),
                3
        );

        assertEquals(45L, result.getTotalDamage());
        assertEquals(15L, result.getStepTrace().get(0).getDirectDamage());
        assertEquals(15L, result.getStepTrace().get(1).getDirectDamage());
        assertEquals(15L, result.getStepTrace().get(2).getDirectDamage());
        assertEquals("Advance", result.getDirectHitDebugSnapshots().get(0).getSkillName());
    }

    @Test
    void wave_dash_powinno_dodawac_drugi_direct_hit_tez_w_runtime_manual_simulation() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceCurrentBuild(
                        new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.LEFT)
                ),
                3
        );

        assertEquals(105L, result.getTotalDamage());
        assertEquals(35L, result.getStepTrace().get(0).getDirectDamage());
        assertEquals(35L, result.getStepTrace().get(1).getDirectDamage());
        assertEquals(35L, result.getStepTrace().get(2).getDirectDamage());
        assertEquals(2, result.getDirectHitDebugSnapshots().get(0).getBreakdown().getComponents().size());
    }

    @Test
    void flash_of_the_blade_powinno_laczyc_replace_status_i_cooldown_w_tym_samym_runtime() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                List.of(
                        new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT),
                        new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)
                ),
                List.of(SkillId.ADVANCE, SkillId.BRANDISH)
        );

        SimulationResult result = simulationService.calculateCurrentBuild(snapshot, 3);

        assertEquals("Advance", result.getStepTrace().get(0).getActionName());
        assertEquals(33L, result.getStepTrace().get(0).getDirectDamage());

        assertEquals("Brandish", result.getStepTrace().get(1).getActionName());
        assertEquals(13L, result.getStepTrace().get(1).getDirectDamage());
        assertTrue(result.getStepTrace().get(1).getSkillBarStates().get(0).isOnCooldown());

        assertEquals("Brandish", result.getStepTrace().get(2).getActionName());
        assertEquals(13L, result.getStepTrace().get(2).getDirectDamage());
    }

    @Test
    void flash_of_the_blade_powinno_ustawiac_cooldown_8_s() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceCurrentBuild(
                        new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)
                ),
                9
        );

        assertEquals("Advance", result.getStepTrace().get(0).getActionName());
        assertEquals("WAIT", result.getStepTrace().get(1).getActionName());
        assertEquals(7, result.getStepTrace().get(1).getSkillBarStates().get(0).getCooldownRemainingSeconds());
        assertEquals("WAIT", result.getStepTrace().get(7).getActionName());
        assertEquals(1, result.getStepTrace().get(7).getSkillBarStates().get(0).getCooldownRemainingSeconds());
        assertEquals("Advance", result.getStepTrace().get(8).getActionName());
        assertEquals(66L, result.getTotalDamage());
    }

    @Test
    void powinien_wykonac_naturalny_wait_gdy_advance_jest_na_cooldownie() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceCurrentBuild(
                        new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)
                ),
                8
        );

        long waitCount = result.getStepTrace().stream()
                .filter(step -> step.getActionType() == SimulationActionType.WAIT)
                .count();

        assertEquals(7L, waitCount);
        assertTrue(result.getStepTrace().get(1).getSelectionReason().contains("WAIT"));
        assertTrue(result.getStepTrace().get(1).getSkillBarStates().get(0).isOnCooldown());
        assertFalse(result.getStepTrace().get(1).getSkillBarStates().get(0).isSelected());
    }

    @Test
    void cooldown_powinien_naturalnie_wplywac_na_wybor_lru() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                List.of(
                        new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT),
                        new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)
                ),
                List.of(SkillId.ADVANCE, SkillId.BRANDISH)
        );

        SimulationResult result = simulationService.calculateCurrentBuild(snapshot, 9);

        assertEquals("Advance", result.getStepTrace().get(0).getActionName());
        assertEquals("Brandish", result.getStepTrace().get(1).getActionName());
        assertEquals("Brandish", result.getStepTrace().get(7).getActionName());
        assertEquals("Advance", result.getStepTrace().get(8).getActionName());
        assertTrue(result.getStepTrace().get(1).getSkillBarStates().get(0).isOnCooldown());
        assertEquals(7, result.getStepTrace().get(1).getSkillBarStates().get(0).getCooldownRemainingSeconds());
        assertTrue(result.getStepTrace().get(8).getSelectionReason().contains("według LRU"));
        assertEquals(147L, result.getTotalDamage());
    }
}
