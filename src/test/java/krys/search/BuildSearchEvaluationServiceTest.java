package krys.search;

import krys.app.CurrentBuildRequest;
import krys.app.CurrentBuildSnapshotFactory;
import krys.combat.DamageEngine;
import krys.simulation.ManualSimulationService;
import krys.simulation.SimulationResult;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildSearchEvaluationServiceTest {
    private final ManualSimulationService manualSimulationService = new ManualSimulationService(new DamageEngine());
    private final BuildSearchEvaluationService evaluationService = new BuildSearchEvaluationService(manualSimulationService);
    private final CurrentBuildSnapshotFactory snapshotFactory = new CurrentBuildSnapshotFactory();

    @Test
    void powinien_oceniac_kandydata_przez_ten_sam_runtime_co_manual_simulation() {
        CurrentBuildRequest request = new CurrentBuildRequest(
                13,
                8,
                18.0d,
                0.0d,
                50.0d,
                50.0d,
                50.0d,
                Map.of(
                        SkillId.HOLY_BOLT,
                        new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE)
                ),
                List.of(SkillId.HOLY_BOLT),
                60
        );

        BuildSearchEvaluation evaluation = evaluationService.evaluate(new BuildSearchCandidate(request));
        SimulationResult directResult = manualSimulationService.calculateCurrentBuild(
                snapshotFactory.create(request),
                request.getHorizonSeconds()
        );

        assertEquals(directResult.getTotalDamage(), evaluation.getSimulationResult().getTotalDamage());
        assertEquals(directResult.getTotalReactiveDamage(), evaluation.getSimulationResult().getTotalReactiveDamage());
        assertEquals(directResult.getDelayedHitBreakdowns().size(), evaluation.getSimulationResult().getDelayedHitBreakdowns().size());
        assertEquals(directResult.getStepTrace().size(), evaluation.getSimulationResult().getStepTrace().size());
        assertEquals(1732L, directResult.getTotalDamage());
    }
}
