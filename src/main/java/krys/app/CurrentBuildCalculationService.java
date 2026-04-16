package krys.app;

import krys.simulation.HeroBuildSnapshot;
import krys.simulation.ManualSimulationService;
import krys.simulation.SimulationResult;
import krys.skill.SkillState;

/** Cienka warstwa aplikacyjna budująca snapshot i uruchamiająca istniejący runtime M8. */
public final class CurrentBuildCalculationService {
    private final ManualSimulationService manualSimulationService;
    private final CurrentBuildSnapshotFactory snapshotFactory;

    public CurrentBuildCalculationService(ManualSimulationService manualSimulationService) {
        this(manualSimulationService, new CurrentBuildSnapshotFactory());
    }

    CurrentBuildCalculationService(ManualSimulationService manualSimulationService,
                                   CurrentBuildSnapshotFactory snapshotFactory) {
        this.manualSimulationService = manualSimulationService;
        this.snapshotFactory = snapshotFactory;
    }

    public CurrentBuildCalculation calculate(CurrentBuildRequest request) {
        HeroBuildSnapshot snapshot = snapshotFactory.create(request);
        SimulationResult result = manualSimulationService.calculateCurrentBuild(snapshot, request.getHorizonSeconds());
        return new CurrentBuildCalculation(request, snapshot, result);
    }
}
