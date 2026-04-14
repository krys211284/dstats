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
        assertEquals(8L, first.getBreakdown().getFinalDamage());

        assertEquals(4, second.getAppliedSecond());
        assertEquals(7, second.getTriggerSecond());
        assertTrue(second.isActiveAtEnd());

        SimulationStepTrace tick4 = result.getStepTrace().get(3);
        assertEquals(4, tick4.getSecond());
        assertEquals(SimulationActionType.SKILL, tick4.getActionType());
        assertEquals("Holy Bolt", tick4.getActionName());
        assertEquals(13L, tick4.getDirectDamage());
        assertEquals(8L, tick4.getDelayedDamage());
        assertEquals(21L, tick4.getTotalStepDamage());
        assertEquals(60L, tick4.getCumulativeDamage());
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
        assertEquals(3, result.getStepTrace().size());
    }

    @Test
    void powinien_policzyc_minimalna_manual_simulation_dla_holy_bolt_z_judgement() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceCurrentBuild(new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE)),
                60
        );

        long detonatedCount = result.getDelayedHitBreakdowns().stream().filter(DelayedHitBreakdown::isDetonated).count();

        assertEquals(932L, result.getTotalDamage());
        assertEquals(932.0d / 60.0d, result.getDps(), 0.0000001d);
        assertEquals(1, result.getDirectHitDebugSnapshots().size());
        assertEquals("Holy Bolt", result.getDirectHitDebugSnapshots().get(0).getSkillName());
        assertNotNull(result.getDirectHitDebugSnapshots().get(0).getBreakdown());
        assertEquals(21L, result.getDirectHitDebugSnapshots().get(0).getBreakdown().getRawDamage());
        assertEquals(13L, result.getDirectHitDebugSnapshots().get(0).getBreakdown().getFinalDamage());
        assertEquals(60, result.getStepTrace().size());
        assertEquals(932L, result.getStepTrace().get(59).getCumulativeDamage());
        assertEquals(20, result.getDelayedHitBreakdowns().size());
        assertEquals(19L, detonatedCount);
        assertTrue(result.isJudgementActiveAtEnd());
    }
}
