package krys.itemimport;

/** Status spójności liczby gwiazdek z nagłówka i przypisanych affixów GA. */
public enum GreaterAffixVerificationStatus {
    OK,
    HEADER_COUNT_MISSING,
    HEADER_COUNT_MISMATCH,
    LOCAL_MARKERS_AMBIGUOUS,
    NOT_ENOUGH_AFFIXES_FOR_HEADER_COUNT,
    REQUIRES_USER_CONFIRMATION
}
