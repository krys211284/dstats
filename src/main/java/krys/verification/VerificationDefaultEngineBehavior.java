package krys.verification;

/** Zachowanie silnika przed zweryfikowaniem mechaniki. */
public enum VerificationDefaultEngineBehavior {
    IGNORED("ignored"),
    METADATA_ONLY("metadataOnly"),
    BLOCKED("blocked");

    private final String id;

    VerificationDefaultEngineBehavior(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
