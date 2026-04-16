package krys.search;

import krys.simulation.HeroBuildSnapshot;
import krys.simulation.SimulationResult;

/** Pojedynczy wynik rankingu searcha M9. */
public final class BuildSearchRankedResult {
    private final int rank;
    private final BuildSearchCandidate candidate;
    private final HeroBuildSnapshot snapshot;
    private final SimulationResult simulationResult;

    public BuildSearchRankedResult(int rank,
                                   BuildSearchCandidate candidate,
                                   HeroBuildSnapshot snapshot,
                                   SimulationResult simulationResult) {
        this.rank = rank;
        this.candidate = candidate;
        this.snapshot = snapshot;
        this.simulationResult = simulationResult;
    }

    public int getRank() {
        return rank;
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
