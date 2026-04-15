package krys.app;

import krys.simulation.HeroBuildSnapshot;
import krys.simulation.SimulationResult;
import krys.skill.SkillState;

/** Wynik aplikacyjny flow „Policz aktualny build” oparty na wspólnym runtime M6. */
public final class CurrentBuildCalculation {
    private final CurrentBuildRequest request;
    private final SkillState skillState;
    private final HeroBuildSnapshot snapshot;
    private final SimulationResult result;

    public CurrentBuildCalculation(CurrentBuildRequest request,
                                   SkillState skillState,
                                   HeroBuildSnapshot snapshot,
                                   SimulationResult result) {
        this.request = request;
        this.skillState = skillState;
        this.snapshot = snapshot;
        this.result = result;
    }

    public CurrentBuildRequest getRequest() {
        return request;
    }

    public SkillState getSkillState() {
        return skillState;
    }

    public HeroBuildSnapshot getSnapshot() {
        return snapshot;
    }

    public SimulationResult getResult() {
        return result;
    }
}
