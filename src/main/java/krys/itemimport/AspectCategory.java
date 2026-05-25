package krys.itemimport;

/** Kategoria aspektu w Kodeksie Potęgi. */
public enum AspectCategory {
    OFFENSE("Ofensywa"),
    UNKNOWN("Nieznana");

    private final String displayName;

    AspectCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
