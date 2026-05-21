package krys.tempering;

/** Globalne kategorie hartowania itemów. */
public enum TemperingCategory {
    WEAPON("Broń"),
    OFFENSE("Ofensywa"),
    DEFENSE("Defensywa"),
    UTILITY("Funkcjonalność"),
    MOBILITY("Mobilność"),
    RESOURCE("Zasoby");

    private final String displayName;

    TemperingCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
