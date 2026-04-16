package krys.search;

import java.util.Locale;

/** Minimalny reporter progressu CLI dla searcha M12. */
final class SearchBuildCliProgressReporter implements BuildSearchProgressListener {
    private int lastPrintedPercent = -1;

    @Override
    public void onSearchStarted(BuildSearchAudit audit) {
        System.out.println("== Start searcha ==");
        System.out.println("Liczba legalnych kandydatów: " + audit.getLegalCandidateCount());
        System.out.println("Rozmiar przestrzeni statów: " + audit.getStatSpaceSize());
        System.out.println("Rozmiar przestrzeni skilli: " + audit.getSkillSpaceSize());
        System.out.println("Rozmiar przestrzeni action bara: " + audit.getActionBarSpaceSize());
        System.out.println("Skala search space: " + audit.getSpaceScale().getDisplayName());
        System.out.println();
    }

    @Override
    public void onCandidateEvaluated(int evaluatedCount, int totalCandidates) {
        if (totalCandidates <= 0) {
            return;
        }

        int percent = (int) Math.floor((evaluatedCount * 100.0d) / totalCandidates);
        if (evaluatedCount == totalCandidates || totalCandidates <= 100 || percent > lastPrintedPercent) {
            lastPrintedPercent = percent;
            System.out.println("Postęp: " + evaluatedCount + "/" + totalCandidates
                    + " (" + String.format(Locale.US, "%d%%", percent) + ")");
        }
    }

    @Override
    public void onSearchCompleted(BuildSearchResult result) {
        System.out.println();
        System.out.println("== Search zakończony ==");
        System.out.println("Oceniono kandydatów: " + result.getEvaluatedCandidateCount());
        System.out.println();
    }
}
