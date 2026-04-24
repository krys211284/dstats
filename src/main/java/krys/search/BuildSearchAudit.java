package krys.search;

/** Minimalny audit preflightu searcha M12 bez ingerencji w runtime oceny kandydatów. */
public final class BuildSearchAudit {
    static final long SMALL_MAX_LEGAL_CANDIDATES = 100L;
    static final long MEDIUM_MAX_LEGAL_CANDIDATES = 1000L;

    private final boolean usingItemLibrary;
    private final long legalCandidateCount;
    private final long statSpaceSize;
    private final long itemLibraryCombinationSpaceSize;
    private final long skillSpaceSize;
    private final long actionBarSpaceSize;
    private final BuildSearchSpaceScale spaceScale;

    public BuildSearchAudit(long legalCandidateCount,
                            long statSpaceSize,
                            long skillSpaceSize,
                            long actionBarSpaceSize) {
        this(false, legalCandidateCount, statSpaceSize, 1L, skillSpaceSize, actionBarSpaceSize);
    }

    public BuildSearchAudit(boolean usingItemLibrary,
                            long legalCandidateCount,
                            long statSpaceSize,
                            long itemLibraryCombinationSpaceSize,
                            long skillSpaceSize,
                            long actionBarSpaceSize) {
        this.usingItemLibrary = usingItemLibrary;
        this.legalCandidateCount = legalCandidateCount;
        this.statSpaceSize = statSpaceSize;
        this.itemLibraryCombinationSpaceSize = itemLibraryCombinationSpaceSize;
        this.skillSpaceSize = skillSpaceSize;
        this.actionBarSpaceSize = actionBarSpaceSize;
        this.spaceScale = classify(legalCandidateCount);
    }

    public boolean isUsingItemLibrary() {
        return usingItemLibrary;
    }

    public long getLegalCandidateCount() {
        return legalCandidateCount;
    }

    public long getStatSpaceSize() {
        return statSpaceSize;
    }

    public long getItemLibraryCombinationSpaceSize() {
        return itemLibraryCombinationSpaceSize;
    }

    public long getSkillSpaceSize() {
        return skillSpaceSize;
    }

    public long getActionBarSpaceSize() {
        return actionBarSpaceSize;
    }

    public BuildSearchSpaceScale getSpaceScale() {
        return spaceScale;
    }

    private static BuildSearchSpaceScale classify(long legalCandidateCount) {
        if (legalCandidateCount <= SMALL_MAX_LEGAL_CANDIDATES) {
            return BuildSearchSpaceScale.SMALL;
        }
        if (legalCandidateCount <= MEDIUM_MAX_LEGAL_CANDIDATES) {
            return BuildSearchSpaceScale.MEDIUM;
        }
        return BuildSearchSpaceScale.LARGE;
    }
}
