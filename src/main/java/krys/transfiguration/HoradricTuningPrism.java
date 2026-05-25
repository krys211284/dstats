package krys.transfiguration;

/** Pryzmat dostrojenia zapisany przy Przeistoczeniu itemu. */
public enum HoradricTuningPrism {
    NONE("Brak"),
    ENTROPIC("Entropiczny"),
    KULLEAN("Kulleana"),
    AGGRESSIVE("Agresywny"),
    PRAGMATIC("Pragmatyczny"),
    PROTECTOR("Protektora"),
    RESOURCEFUL("Zasobny"),
    ADEPT("Adeptowski"),
    CHROMATIC("Chromatyczny");

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
