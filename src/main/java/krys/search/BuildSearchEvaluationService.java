package krys.search;

import krys.app.CurrentBuildSnapshotFactory;
import krys.simulation.HeroBuildSnapshot;
import krys.simulation.ManualSimulationService;
import krys.simulation.SimulationResult;

/** Warstwa oceny searcha M12 uruchamiająca dokładnie ten sam runtime co flow manual simulation. */
public final class BuildSearchEvaluationService {
    private final ManualSimulationService manualSimulationService;
    private final CurrentBuildSnapshotFactory snapshotFactory;

    public BuildSearchEvaluationService(ManualSimulationService manualSimulationService) {
        this(manualSimulationService, new CurrentBuildSnapshotFactory());
    }

    BuildSearchEvaluationService(ManualSimulationService manualSimulationService,
                                 CurrentBuildSnapshotFactory snapshotFactory) {
        this.manualSimulationService = manualSimulationService;
        this.snapshotFactory = snapshotFactory;
    }

    public BuildSearchEvaluation evaluate(BuildSearchCandidate candidate) {
        HeroBuildSnapshot snapshot = snapshotFactory.create(candidate.getCurrentBuildRequest());
        SimulationResult simulationResult = manualSimulationService.calculateCurrentBuild(
                snapshot,
                candidate.getCurrentBuildRequest().getHorizonSeconds()
        );
        return new BuildSearchEvaluation(candidate, snapshot, simulationResult);
    }
}
