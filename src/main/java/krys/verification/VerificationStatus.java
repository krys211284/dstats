package krys.verification;

/** Status procesu weryfikacji mechaniki. */
public enum VerificationStatus {
    REQUIRES_VERIFICATION("requiresVerification"),
    VERIFIED("verified");

    private final String id;

    VerificationStatus(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
