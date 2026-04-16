package krys.search;

/** Minimalny listener postępu searcha M12 dla CLI i przyszłych integracji bez zmiany oceny kandydatów. */
public interface BuildSearchProgressListener {
    static BuildSearchProgressListener noop() {
        return new BuildSearchProgressListener() {
        };
    }

    default void onSearchStarted(BuildSearchAudit audit) {
    }

    default void onCandidateEvaluated(int evaluatedCount, int totalCandidates) {
    }

    default void onSearchCompleted(BuildSearchResult result) {
    }
}
