package krys.itemimport;

/** Status użycia aspektu przez runtime DPS. */
public enum AspectRuntimeStatus {
    DESCRIPTIVE_ONLY("Nieaktywny w runtime DPS");

    private final String displayName;

    AspectRuntimeStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
