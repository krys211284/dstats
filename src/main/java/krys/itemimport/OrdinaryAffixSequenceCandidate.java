package krys.itemimport;

/** Kandydat spójnej sekwencji tooltipa używany wyłącznie do wyznaczenia ORDINARY_AFFIXES. */
final class OrdinaryAffixSequenceCandidate {
    private final String sequenceId;
    private final String sourceVariant;
    private final FullItemRead fullItemRead;
    private final boolean globalMergedSequence;

    OrdinaryAffixSequenceCandidate(String sequenceId,
                                   String sourceVariant,
                                   FullItemRead fullItemRead,
                                   boolean globalMergedSequence) {
        this.sequenceId = sequenceId == null || sequenceId.isBlank() ? "ordinary-sequence" : sequenceId;
        this.sourceVariant = sourceVariant == null ? "" : sourceVariant;
        this.fullItemRead = fullItemRead == null ? FullItemRead.empty() : fullItemRead;
        this.globalMergedSequence = globalMergedSequence;
    }

    static OrdinaryAffixSequenceCandidate global(FullItemRead fullItemRead) {
        return new OrdinaryAffixSequenceCandidate("global-full-read", "GLOBAL_FULL_READ", fullItemRead, true);
    }

    String sequenceId() {
        return sequenceId;
    }

    String sourceVariant() {
        return sourceVariant;
    }

    FullItemRead fullItemRead() {
        return fullItemRead;
    }

    boolean globalMergedSequence() {
        return globalMergedSequence;
    }
}
