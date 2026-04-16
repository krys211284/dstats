package krys.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Wynik backendowego searcha M12 z audytem, liczbą ocenionych kandydatów i top N wyników. */
public final class BuildSearchResult {
    private final BuildSearchRequest request;
    private final BuildSearchAudit audit;
    private final int evaluatedCandidateCount;
    private final int normalizedResultCount;
    private final List<BuildSearchRankedResult> topResults;

    public BuildSearchResult(BuildSearchRequest request,
                             BuildSearchAudit audit,
                             int evaluatedCandidateCount,
                             int normalizedResultCount,
                             List<BuildSearchRankedResult> topResults) {
        this.request = request;
        this.audit = audit;
        this.evaluatedCandidateCount = evaluatedCandidateCount;
        this.normalizedResultCount = normalizedResultCount;
        this.topResults = Collections.unmodifiableList(new ArrayList<>(topResults));
    }

    public BuildSearchRequest getRequest() {
        return request;
    }

    public BuildSearchAudit getAudit() {
        return audit;
    }

    public int getEvaluatedCandidateCount() {
        return evaluatedCandidateCount;
    }

    public int getNormalizedResultCount() {
        return normalizedResultCount;
    }

    public List<BuildSearchRankedResult> getTopResults() {
        return topResults;
    }
}
