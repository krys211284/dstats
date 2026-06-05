package krys.itemimport;

import java.util.List;

/** Walidacja spójności nagłówkowej liczby Greater Affix z finalnie przypisanymi affixami. */
public final class GreaterAffixImportVerification {
    private static final GreaterAffixImportVerification EMPTY = new GreaterAffixImportVerification(
            0,
            0,
            0,
            0,
            GreaterAffixVerificationStatus.HEADER_COUNT_MISSING,
            List.of()
    );

    private final int headerGaCount;
    private final int localGaAffixCount;
    private final int assignedGaAffixCount;
    private final int ordinaryAffixCount;
    private final GreaterAffixVerificationStatus status;
    private final List<String> warnings;

    public GreaterAffixImportVerification(int headerGaCount,
                                          int localGaAffixCount,
                                          int assignedGaAffixCount,
                                          int ordinaryAffixCount,
                                          GreaterAffixVerificationStatus status,
                                          List<String> warnings) {
        this.headerGaCount = Math.max(0, headerGaCount);
        this.localGaAffixCount = Math.max(0, localGaAffixCount);
        this.assignedGaAffixCount = Math.max(0, assignedGaAffixCount);
        this.ordinaryAffixCount = Math.max(0, ordinaryAffixCount);
        this.status = status == null ? GreaterAffixVerificationStatus.HEADER_COUNT_MISSING : status;
        this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static GreaterAffixImportVerification empty() {
        return EMPTY;
    }

    public int getHeaderGaCount() {
        return headerGaCount;
    }

    public int getLocalGaAffixCount() {
        return localGaAffixCount;
    }

    public int getAssignedGaAffixCount() {
        return assignedGaAffixCount;
    }

    public int getOrdinaryAffixCount() {
        return ordinaryAffixCount;
    }

    public GreaterAffixVerificationStatus getStatus() {
        return status;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
