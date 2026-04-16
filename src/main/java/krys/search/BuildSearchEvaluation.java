package krys.search;

import krys.simulation.HeroBuildSnapshot;
import krys.simulation.SimulationResult;

/** Wynik oceny pojedynczego kandydata searcha przez wspólny runtime manual simulation. */
public final class BuildSearchEvaluation {
    private final BuildSearchCandidate candidate;
    private final HeroBuildSnapshot snapshot;
    private final SimulationResult simulationResult;

    public BuildSearchEvaluation(BuildSearchCandidate candidate,
                                 HeroBuildSnapshot snapshot,
                                 SimulationResult simulationResult) {
        this.candidate = candidate;
        this.snapshot = snapshot;
        this.simulationResult = simulationResult;
    }

    public BuildSearchCandidate getCandidate() {
        return candidate;
    }

    public HeroBuildSnapshot getSnapshot() {
        return snapshot;
    }

    public SimulationResult getSimulationResult() {
        return simulationResult;
    }
}
