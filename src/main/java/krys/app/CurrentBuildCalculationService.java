package krys.app;

import krys.simulation.HeroBuildSnapshot;
import krys.simulation.ManualSimulationService;
import krys.simulation.SimulationResult;
import krys.skill.SkillState;

/** Cienka warstwa aplikacyjna budująca snapshot i uruchamiająca istniejący runtime M3. */
public final class CurrentBuildCalculationService {
    private final ManualSimulationService manualSimulationService;

    public CurrentBuildCalculationService(ManualSimulationService manualSimulationService) {
        this.manualSimulationService = manualSimulationService;
    }

    public CurrentBuildCalculation calculate(CurrentBuildRequest request) {
        SkillState skillState = new SkillState(
                request.getSkillId(),
                request.getRank(),
                request.isBaseUpgrade(),
                request.getChoiceUpgrade()
        );
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(skillState);
        SimulationResult result = manualSimulationService.calculateCurrentBuild(snapshot, request.getHorizonSeconds());
        return new CurrentBuildCalculation(request, skillState, snapshot, result);
    }
}
