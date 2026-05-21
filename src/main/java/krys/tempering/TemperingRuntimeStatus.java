package krys.tempering;

/** Status wpływu hartowania na runtime. */
public enum TemperingRuntimeStatus {
    DATA_ONLY("Dane itemu / runtime nieaktywny"),
    NOT_RUNTIME_ENABLED("Opisowe / runtime DPS nieaktywny"),
    RUNTIME_ENABLED("Aktywne w runtime"),
    NEEDS_SOURCE("Wymaga źródła"),
    NEEDS_MECHANIC_DECISION("Wymaga decyzji mechaniki");

    private final String displayName;

    TemperingRuntimeStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
