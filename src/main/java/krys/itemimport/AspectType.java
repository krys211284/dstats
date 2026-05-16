package krys.itemimport;

/** Typ aspektu w katalogu importu itemów. */
public enum AspectType {
    LEGENDARY("Legendarny"),
    UNIQUE("Unikatowy");

    private final String displayName;

    AspectType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
