package krys.simulation;

import krys.app.SampleBuildFactory;
import krys.combat.DamageEngine;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualSimulationReactiveDamageTest {
    private final ManualSimulationService simulationService = new ManualSimulationService(new DamageEngine());

    @Test
    void powinien_odpalac_enemy_hit_schedule_w_t3_t6_t9() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceReactiveCurrentBuild(
                        new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)
                ),
                9
        );

        assertEquals(3, result.getReactiveHitBreakdowns().size());
        assertIterableEquals(
                List.of(3, 6, 9),
                result.getReactiveHitBreakdowns().stream().map(reactive -> reactive.getTriggeredSecond()).toList()
        );
    }

    @Test
    void reactive_final_damage_powinno_wejsc_do_total_damage() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceReactiveCurrentBuild(
                        new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)
                ),
                3
        );

        assertEquals(40L, result.getTotalReactiveDamage());
        assertEquals(73L, result.getTotalDamage());
        assertEquals(40L, result.getStepTrace().get(2).getReactiveDamage());
        assertEquals(51L, result.getStepTrace().get(2).getTotalStepDamage());
        assertEquals(73L, result.getStepTrace().get(2).getCumulativeDamage());
    }

    @Test
    void tick_order_powinien_pozostac_delayed_reactive_active_cast() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceReactiveCurrentBuild(
                        new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE)
                ),
                4
        );

        assertEquals("delayed -> reactive -> active cast", result.getStepTrace().get(2).getTickOrderLabel());
        assertEquals(40L, result.getStepTrace().get(2).getReactiveDamage());
        assertEquals(13L, result.getStepTrace().get(2).getDirectDamage());
        assertEquals(0L, result.getStepTrace().get(2).getDelayedDamage());

        assertEquals("delayed -> reactive -> active cast", result.getStepTrace().get(3).getTickOrderLabel());
        assertEquals(8L, result.getStepTrace().get(3).getDelayedDamage());
        assertEquals(0L, result.getStepTrace().get(3).getReactiveDamage());
        assertEquals(13L, result.getStepTrace().get(3).getDirectDamage());
    }

    @Test
    void cumulative_damage_w_trace_powinno_pozostac_spojne_po_dodaniu_reactive() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceReactiveCurrentBuild(
                        new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE)
                ),
                60
        );

        long cumulative = 0L;
        for (SimulationStepTrace step : result.getStepTrace()) {
            cumulative += step.getTotalStepDamage();
            assertEquals(cumulative, step.getCumulativeDamage());
        }

        assertEquals(800L, result.getTotalReactiveDamage());
        assertEquals(1732L, result.getTotalDamage());
        assertEquals(cumulative, result.getTotalDamage());
        assertTrue(result.getReactiveHitBreakdowns().stream().allMatch(entry -> entry.getReactiveFinalDamage() == 40L));
    }
}
