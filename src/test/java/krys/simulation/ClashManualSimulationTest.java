package krys.simulation;

import krys.app.SampleBuildFactory;
import krys.combat.DamageEngine;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClashManualSimulationTest {
    private final ManualSimulationService simulationService = new ManualSimulationService(new DamageEngine());

    @Test
    void powinien_wykonac_podstawowy_use_case_clash_w_manual_simulation() {
        SimulationResult result = simulationService.calculateCurrentBuild(
                SampleBuildFactory.createReferenceReactiveCurrentBuild(
                        new SkillState(SkillId.CLASH, 5, false, SkillUpgradeChoice.NONE)
                ),
                9
        );

        assertEquals(120L, result.getTotalDamage());
        assertEquals(120L, result.getTotalReactiveDamage());
        assertEquals(3, result.getReactiveHitBreakdowns().size());
        assertEquals("Clash", result.getStepTrace().get(0).getActionName());
        assertEquals(0L, result.getStepTrace().get(0).getDirectDamage());
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

        assertEquals(132L, result.getTotalDamage());
        assertEquals(132L, result.getTotalReactiveDamage());
        assertTrue(result.isResolveActiveAtEnd());
        assertEquals(0.75d, result.getActiveBlockChanceAtEnd(), 0.0000001d);
        assertEquals(0.0d, result.getActiveThornsBonusAtEnd(), 0.0000001d);
        assertEquals(44L, result.getReactiveHitBreakdowns().get(0).getReactiveFinalDamage());
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

        assertEquals(264L, result.getTotalDamage());
        assertEquals(264L, result.getTotalReactiveDamage());
        assertTrue(result.isResolveActiveAtEnd());
        assertEquals(0.75d, result.getActiveBlockChanceAtEnd(), 0.0000001d);
        assertEquals(50.0d, result.getActiveThornsBonusAtEnd(), 0.0000001d);
        assertEquals(104L, result.getReactiveHitBreakdowns().get(0).getThornsRawDamage());
        assertEquals(64L, result.getReactiveHitBreakdowns().get(0).getThornsFinalDamage());
        assertEquals(39L, result.getReactiveHitBreakdowns().get(0).getRetributionExpectedRawDamage());
        assertEquals(24L, result.getReactiveHitBreakdowns().get(0).getRetributionExpectedFinalDamage());
        assertEquals(88L, result.getReactiveHitBreakdowns().get(0).getReactiveFinalDamage());
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

        assertEquals(40L, baseClash.getReactiveHitBreakdowns().get(0).getReactiveFinalDamage());
        assertEquals(44L, clashWithResolve.getReactiveHitBreakdowns().get(0).getReactiveFinalDamage());
        assertEquals(88L, clashWithResolveAndPunishment.getReactiveHitBreakdowns().get(0).getReactiveFinalDamage());

        assertEquals(120L, baseClash.getTotalReactiveDamage());
        assertEquals(132L, clashWithResolve.getTotalReactiveDamage());
        assertEquals(264L, clashWithResolveAndPunishment.getTotalReactiveDamage());

        assertFalse(baseClash.getReactiveHitBreakdowns().get(0).isResolveActive());
        assertTrue(clashWithResolve.getReactiveHitBreakdowns().get(0).isResolveActive());
        assertTrue(clashWithResolveAndPunishment.getReactiveHitBreakdowns().get(0).isResolveActive());
        assertTrue(clashWithResolveAndPunishment.getReactiveHitBreakdowns().get(0).isPunishmentActive());
    }
}
