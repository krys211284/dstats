package krys.transfiguration;

/** Pryzmat dostrojenia zapisany przy Przeistoczeniu itemu. */
public enum HoradricTuningPrism {
    NONE("Brak"),
    ENTROPIC("Entropic"),
    KULLEAN("Kullean"),
    AGGRESSIVE("Aggressive"),
    PRAGMATIC("Pragmatic"),
    PROTECTOR("Protector's"),
    RESOURCEFUL("Resourceful"),
    ADEPT("Adept's"),
    CHROMATIC("Chromatic");

    private final String displayName;

    HoradricTuningPrism(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static HoradricTuningPrism fromNullable(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return NONE;
        }
        try {
            return HoradricTuningPrism.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            return NONE;
        }
    }
}
