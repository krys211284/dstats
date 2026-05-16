package krys.itemimport;

/** Status użycia affixu przez runtime DPS. */
public enum AffixRuntimeStatus {
    DESCRIPTIVE_ONLY("Nieaktywny / opisowy");

    private final String displayName;

    AffixRuntimeStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
