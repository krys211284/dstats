package krys.itemimport;

/** Jednostka wartości affixu używana w katalogu OCR i ręcznej korekcie. */
public enum AffixValueUnit {
    FLAT("flat"),
    PERCENT("percent"),
    POINTS("points"),
    RESOURCE("resource"),
    TEXT("text");

    private final String displayName;

    AffixValueUnit(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
