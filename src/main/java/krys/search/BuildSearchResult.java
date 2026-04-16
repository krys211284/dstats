package krys.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Wynik backendowego searcha M9 z pełną liczbą ocenionych kandydatów i top N wyników. */
public final class BuildSearchResult {
    private final BuildSearchRequest request;
    private final int evaluatedCandidateCount;
    private final List<BuildSearchRankedResult> topResults;

    public BuildSearchResult(BuildSearchRequest request,
                             int evaluatedCandidateCount,
                             List<BuildSearchRankedResult> topResults) {
        this.request = request;
        this.evaluatedCandidateCount = evaluatedCandidateCount;
        this.topResults = Collections.unmodifiableList(new ArrayList<>(topResults));
    }

    public BuildSearchRequest getRequest() {
        return request;
    }

    public int getEvaluatedCandidateCount() {
        return evaluatedCandidateCount;
    }

    public List<BuildSearchRankedResult> getTopResults() {
        return topResults;
    }
}
