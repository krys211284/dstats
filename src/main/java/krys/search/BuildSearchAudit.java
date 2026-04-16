package krys.search;

/** Minimalny audit preflightu searcha M12 bez ingerencji w runtime oceny kandydatów. */
public final class BuildSearchAudit {
    static final long SMALL_MAX_LEGAL_CANDIDATES = 100L;
    static final long MEDIUM_MAX_LEGAL_CANDIDATES = 1000L;

    private final long legalCandidateCount;
    private final long statSpaceSize;
    private final long skillSpaceSize;
    private final long actionBarSpaceSize;
    private final BuildSearchSpaceScale spaceScale;

    public BuildSearchAudit(long legalCandidateCount,
                            long statSpaceSize,
                            long skillSpaceSize,
                            long actionBarSpaceSize) {
        this.legalCandidateCount = legalCandidateCount;
        this.statSpaceSize = statSpaceSize;
        this.skillSpaceSize = skillSpaceSize;
        this.actionBarSpaceSize = actionBarSpaceSize;
        this.spaceScale = classify(legalCandidateCount);
    }

    public long getLegalCandidateCount() {
        return legalCandidateCount;
    }

    public long getStatSpaceSize() {
        return statSpaceSize;
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
