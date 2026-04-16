package krys.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Główna warstwa aplikacyjna backendowego searcha M9. */
public final class BuildSearchCalculationService {
    private final BuildSearchCandidateGenerator candidateGenerator;
    private final BuildSearchEvaluationService evaluationService;
    private final BuildSearchPresentationNormalizer presentationNormalizer;

    public BuildSearchCalculationService(BuildSearchEvaluationService evaluationService) {
        this(new BuildSearchCandidateGenerator(), evaluationService, new BuildSearchPresentationNormalizer());
    }

    BuildSearchCalculationService(BuildSearchCandidateGenerator candidateGenerator,
                                  BuildSearchEvaluationService evaluationService,
                                  BuildSearchPresentationNormalizer presentationNormalizer) {
        this.candidateGenerator = candidateGenerator;
        this.evaluationService = evaluationService;
        this.presentationNormalizer = presentationNormalizer;
    }

    public BuildSearchResult calculate(BuildSearchRequest request) {
        List<BuildSearchCandidate> candidates = candidateGenerator.generate(request);
        List<BuildSearchEvaluation> evaluations = new ArrayList<>();
        for (BuildSearchCandidate candidate : candidates) {
            evaluations.add(evaluationService.evaluate(candidate));
        }

        evaluations.sort(searchComparator());
        BuildSearchPresentationNormalizer.BuildSearchPresentationView presentationView =
                presentationNormalizer.normalize(evaluations, request.getTopResultsLimit());

        return new BuildSearchResult(
                request,
                candidates.size(),
                presentationView.normalizedResultCount(),
                presentationView.topResults()
        );
    }

    private static Comparator<BuildSearchEvaluation> searchComparator() {
        return Comparator
                .comparingLong((BuildSearchEvaluation evaluation) -> evaluation.getSimulationResult().getTotalDamage())
                .reversed()
                .thenComparing(Comparator.comparingDouble((BuildSearchEvaluation evaluation) -> evaluation.getSimulationResult().getDps()).reversed())
                .thenComparing(evaluation -> evaluation.getCandidate().toDeterministicKey());
    }
}
