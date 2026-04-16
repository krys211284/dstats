package krys.itemimport;

/** Poziom pewności dla pojedynczego pola rozpoznawanego podczas importu itemu ze screena. */
public enum ItemImportFieldConfidence {
    HIGH("wysoka"),
    MEDIUM("średnia"),
    LOW("niska"),
    UNKNOWN("brak pewnego odczytu");

    private final String displayName;

    ItemImportFieldConfidence(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
