package krys.transfiguration;

/** Pochodzenie wartości zapisanej przy affixie Przeistoczenia. */
public enum TransfigurationValueProvenance {
    SOURCE_ROLL("Wartość bazowa/source roll"),
    GAME_DISPLAYED_VALUE("Wartość widoczna w grze"),
    UNKNOWN("Nieznane pochodzenie");

    private final String displayName;

    TransfigurationValueProvenance(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TransfigurationValueProvenance fromNullable(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return UNKNOWN;
        }
        try {
            return TransfigurationValueProvenance.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
