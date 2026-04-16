package krys.app;

import krys.simulation.HeroBuildSnapshot;
import krys.simulation.SimulationResult;

/** Wynik aplikacyjny flow „Policz aktualny build” oparty na wspólnym runtime M8. */
public final class CurrentBuildCalculation {
    private final CurrentBuildRequest request;
    private final HeroBuildSnapshot snapshot;
    private final SimulationResult result;

    public CurrentBuildCalculation(CurrentBuildRequest request,
                                   HeroBuildSnapshot snapshot,
                                   SimulationResult result) {
        this.request = request;
        this.snapshot = snapshot;
        this.result = result;
    }

    public CurrentBuildRequest getRequest() {
        return request;
    }

    public HeroBuildSnapshot getSnapshot() {
        return snapshot;
    }

    public SimulationResult getResult() {
        return result;
    }
}
