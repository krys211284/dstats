package krys.simulation;

import krys.app.SampleBuildFactory;
import krys.combat.DamageEngine;
import krys.combat.DelayedHitBreakdown;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualSimulationJudgementTest {
    private final ManualSimulationService simulationService = new ManualSimulationService(new DamageEngine());

    @Test
    void powinien_odpalic_judgement_po_3_sekundach_w_trigger_time() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceCurrentBuild(new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE)),
                4
        );

        assertEquals(2, result.getDelayedHitBreakdowns().size());

        DelayedHitBreakdown first = result.getDelayedHitBreakdowns().get(0);
        DelayedHitBreakdown second = result.getDelayedHitBreakdowns().get(1);

        assertEquals(1, first.getAppliedSecond());
        assertEquals(4, first.getTriggerSecond());
        assertEquals(4, first.getDetonatedSecond());
        assertNotNull(first.getBreakdown());

        assertEquals(4, second.getAppliedSecond());
        assertEquals(7, second.getTriggerSecond());
        assertTrue(second.isActiveAtEnd());
    }

    @Test
    void nie_powinien_stackowac_ani_odswiezac_timera_judgement() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceCurrentBuild(new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE)),
                3
        );

        assertEquals(1, result.getDelayedHitBreakdowns().size());
        DelayedHitBreakdown pending = result.getDelayedHitBreakdowns().get(0);
        assertEquals(1, pending.getAppliedSecond());
        assertEquals(4, pending.getTriggerSecond());
        assertTrue(pending.isActiveAtEnd());
        assertFalse(pending.isDetonated());
    }

    @Test
    void powinien_policzyc_minimalna_manual_simulation_dla_holy_bolt_z_judgement() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceCurrentBuild(new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE)),
                60
        );

        long detonatedCount = result.getDelayedHitBreakdowns().stream().filter(DelayedHitBreakdown::isDetonated).count();

        assertEquals("Holy Bolt", result.getSelectedSkillName());
        assertEquals(932L, result.getTotalDamage());
        assertEquals(932.0d / 60.0d, result.getDps(), 0.0000001d);
        assertNotNull(result.getSingleHitBreakdown());
        assertEquals(21L, result.getSingleHitBreakdown().getRawDamage());
        assertEquals(13L, result.getSingleHitBreakdown().getFinalDamage());
        assertEquals(20, result.getDelayedHitBreakdowns().size());
        assertEquals(19L, detonatedCount);
        assertTrue(result.isJudgementActiveAtEnd());
    }
}
