package krys.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Główna warstwa aplikacyjna backendowego searcha M9. */
public final class BuildSearchCalculationService {
    private final BuildSearchCandidateGenerator candidateGenerator;
    private final BuildSearchEvaluationService evaluationService;

    public BuildSearchCalculationService(BuildSearchEvaluationService evaluationService) {
        this(new BuildSearchCandidateGenerator(), evaluationService);
    }

    BuildSearchCalculationService(BuildSearchCandidateGenerator candidateGenerator,
                                  BuildSearchEvaluationService evaluationService) {
        this.candidateGenerator = candidateGenerator;
        this.evaluationService = evaluationService;
    }

    public BuildSearchResult calculate(BuildSearchRequest request) {
        List<BuildSearchCandidate> candidates = candidateGenerator.generate(request);
        List<BuildSearchEvaluation> evaluations = new ArrayList<>();
        for (BuildSearchCandidate candidate : candidates) {
            evaluations.add(evaluationService.evaluate(candidate));
        }

        evaluations.sort(searchComparator());

        List<BuildSearchRankedResult> topResults = new ArrayList<>();
        int limit = Math.min(request.getTopResultsLimit(), evaluations.size());
        for (int index = 0; index < limit; index++) {
            BuildSearchEvaluation evaluation = evaluations.get(index);
            topResults.add(new BuildSearchRankedResult(
                    index + 1,
                    evaluation.getCandidate(),
                    evaluation.getSnapshot(),
                    evaluation.getSimulationResult()
            ));
        }

        return new BuildSearchResult(request, candidates.size(), topResults);
    }

    private static Comparator<BuildSearchEvaluation> searchComparator() {
        return Comparator
                .comparingLong((BuildSearchEvaluation evaluation) -> evaluation.getSimulationResult().getTotalDamage())
                .reversed()
                .thenComparing(Comparator.comparingDouble((BuildSearchEvaluation evaluation) -> evaluation.getSimulationResult().getDps()).reversed())
                .thenComparing(evaluation -> evaluation.getCandidate().toDeterministicKey());
    }
}
