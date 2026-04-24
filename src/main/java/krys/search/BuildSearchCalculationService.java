package krys.search;

import krys.itemlibrary.ItemLibraryService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Główna warstwa aplikacyjna backendowego searcha M12. */
public final class BuildSearchCalculationService {
    private final BuildSearchCandidateGenerator candidateGenerator;
    private final BuildSearchEvaluationService evaluationService;
    private final BuildSearchPresentationNormalizer presentationNormalizer;

    public BuildSearchCalculationService(BuildSearchEvaluationService evaluationService) {
        this(new BuildSearchCandidateGenerator(), evaluationService, new BuildSearchPresentationNormalizer());
    }

    public BuildSearchCalculationService(BuildSearchEvaluationService evaluationService,
                                         ItemLibraryService itemLibraryService) {
        this(new BuildSearchCandidateGenerator(itemLibraryService), evaluationService, new BuildSearchPresentationNormalizer());
    }

    BuildSearchCalculationService(BuildSearchCandidateGenerator candidateGenerator,
                                  BuildSearchEvaluationService evaluationService,
                                  BuildSearchPresentationNormalizer presentationNormalizer) {
        this.candidateGenerator = candidateGenerator;
        this.evaluationService = evaluationService;
        this.presentationNormalizer = presentationNormalizer;
    }

    public BuildSearchAudit preflight(BuildSearchRequest request) {
        return candidateGenerator.audit(request);
    }

    public BuildSearchResult calculate(BuildSearchRequest request) {
        return calculate(request, BuildSearchProgressListener.noop());
    }

    public BuildSearchResult calculate(BuildSearchRequest request, BuildSearchProgressListener progressListener) {
        BuildSearchAudit audit = preflight(request);
        progressListener.onSearchStarted(audit);

        List<BuildSearchCandidate> candidates = candidateGenerator.generate(request);
        List<BuildSearchEvaluation> evaluations = new ArrayList<>();
        int evaluatedCount = 0;
        for (BuildSearchCandidate candidate : candidates) {
            evaluations.add(evaluationService.evaluate(candidate));
            evaluatedCount++;
            progressListener.onCandidateEvaluated(evaluatedCount, candidates.size());
        }

        evaluations.sort(searchComparator());
        BuildSearchPresentationNormalizer.BuildSearchPresentationView presentationView =
                presentationNormalizer.normalize(evaluations, request.getTopResultsLimit());

        BuildSearchResult result = new BuildSearchResult(
                request,
                audit,
                candidates.size(),
                presentationView.normalizedResultCount(),
                presentationView.topResults()
        );
        progressListener.onSearchCompleted(result);
        return result;
    }

    private static Comparator<BuildSearchEvaluation> searchComparator() {
        return Comparator
                .comparingLong((BuildSearchEvaluation evaluation) -> evaluation.getSimulationResult().getTotalDamage())
                .reversed()
                .thenComparing(Comparator.comparingDouble((BuildSearchEvaluation evaluation) -> evaluation.getSimulationResult().getDps()).reversed())
                .thenComparing(evaluation -> evaluation.getCandidate().toDeterministicKey());
    }
}
