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

class ManualSimulationTickRuntimeTest {
    private final ManualSimulationService simulationService = new ManualSimulationService(new DamageEngine());

    @Test
    void powinien_wykonac_wait_gdy_brak_legalnego_skilla_do_rzutu() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                List.of(),
                List.of(SkillId.HOLY_BOLT)
        );

        SimulationResult result = simulationService.calculateCurrentBuild(snapshot, 3);

        assertEquals(0L, result.getTotalDamage());
        assertTrue(result.getDirectHitDebugSnapshots().isEmpty());
        assertEquals(3, result.getStepTrace().size());
        assertEquals(SimulationActionType.WAIT, result.getStepTrace().get(0).getActionType());
        assertEquals("WAIT", result.getStepTrace().get(0).getActionName());
        assertTrue(result.getStepTrace().get(0).getSelectionReason().contains("WAIT"));
        assertEquals(1, result.getStepTrace().get(0).getSkillBarStates().size());
        assertEquals(0, result.getStepTrace().get(0).getSkillBarStates().get(0).getRank());
        assertFalse(result.getStepTrace().get(0).getSkillBarStates().get(0).isLegalActive());
    }

    @Test
    void powinien_wykonac_wait_gdy_pasek_jest_pusty() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                List.of(new SkillState(SkillId.HOLY_BOLT, 5, false, SkillUpgradeChoice.NONE)),
                List.of()
        );

        SimulationResult result = simulationService.calculateCurrentBuild(snapshot, 2);

        assertEquals(0L, result.getTotalDamage());
        assertTrue(result.getDirectHitDebugSnapshots().isEmpty());
        assertEquals(2, result.getStepTrace().size());
        assertEquals(SimulationActionType.WAIT, result.getStepTrace().get(0).getActionType());
        assertTrue(result.getStepTrace().get(0).getSelectionReason().contains("pusty"));
        assertTrue(result.getStepTrace().get(0).getSkillBarStates().isEmpty());
    }

    @Test
    void powinien_rozstrzygnac_tie_break_dla_nigdy_nieuzytych_skilli_wedlug_kolejnosci_na_pasku() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                List.of(
                        new SkillState(SkillId.HOLY_BOLT, 5, false, SkillUpgradeChoice.NONE),
                        new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)
                ),
                List.of(SkillId.HOLY_BOLT, SkillId.BRANDISH)
        );

        SimulationResult result = simulationService.calculateCurrentBuild(snapshot, 4);

        assertEquals("Holy Bolt", result.getStepTrace().get(0).getActionName());
        assertEquals("Brandish", result.getStepTrace().get(1).getActionName());
        assertTrue(result.getStepTrace().get(0).getSelectionReason().contains("kolejnością na pasku"));
        assertTrue(result.getStepTrace().get(2).getSelectionReason().contains("według LRU"));
        assertTrue(result.getStepTrace().get(0).getSkillBarStates().get(0).isSelected());
        assertFalse(result.getStepTrace().get(0).getSkillBarStates().get(1).isSelected());
        assertEquals(2, result.getDirectHitDebugSnapshots().size());
        assertEquals("Holy Bolt", result.getDirectHitDebugSnapshots().get(0).getSkillName());
        assertEquals("Brandish", result.getDirectHitDebugSnapshots().get(1).getSkillName());
    }

    @Test
    void powinien_wybierac_skill_uzyty_najdawniej_w_modelu_lru() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                List.of(
                        new SkillState(SkillId.HOLY_BOLT, 5, false, SkillUpgradeChoice.NONE),
                        new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)
                ),
                List.of(SkillId.HOLY_BOLT, SkillId.BRANDISH)
        );

        SimulationResult result = simulationService.calculateCurrentBuild(snapshot, 4);

        assertEquals("Holy Bolt", result.getStepTrace().get(0).getActionName());
        assertEquals("Brandish", result.getStepTrace().get(1).getActionName());
        assertEquals("Holy Bolt", result.getStepTrace().get(2).getActionName());
        assertEquals("Brandish", result.getStepTrace().get(3).getActionName());
        assertTrue(result.getStepTrace().get(2).getSelectionReason().contains("według LRU"));
    }

    @Test
    void cumulative_damage_w_trace_powinno_byc_spojne_z_wynikiem_koncowym() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                List.of(
                        new SkillState(SkillId.HOLY_BOLT, 5, false, SkillUpgradeChoice.NONE),
                        new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)
                ),
                List.of(SkillId.HOLY_BOLT, SkillId.BRANDISH)
        );

        SimulationResult result = simulationService.calculateCurrentBuild(snapshot, 4);

        long cumulative = 0L;
        for (SimulationStepTrace step : result.getStepTrace()) {
            cumulative += step.getTotalStepDamage();
            assertEquals(cumulative, step.getCumulativeDamage());
        }
        assertEquals(cumulative, result.getTotalDamage());
        assertEquals(48L, result.getTotalDamage());
    }

    @Test
    void powinien_zachowac_regresje_manual_simulation_dla_brandish() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceCurrentBuild(new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)),
                60
        );

        assertEquals(660L, result.getTotalDamage());
        assertEquals(660.0d / 60.0d, result.getDps(), 0.0000001d);
        assertEquals(1, result.getDirectHitDebugSnapshots().size());
        assertEquals("Brandish", result.getDirectHitDebugSnapshots().get(0).getSkillName());
        assertEquals(60, result.getStepTrace().size());
        assertEquals(0, result.getDelayedHitBreakdowns().size());
        assertEquals(11L, result.getStepTrace().get(0).getDirectDamage());
        assertEquals(660L, result.getStepTrace().get(59).getCumulativeDamage());
    }
}
