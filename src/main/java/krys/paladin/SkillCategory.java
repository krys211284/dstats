package krys.paladin;

/** Prezentacyjna kategoria/tier umiejętności w rankingu, oparta o lokalne źródła. */
public enum SkillCategory {
    PODSTAWOWA("Podstawowa"),
    ADEPT("Adept"),
    CORE("Core"),
    DEFENSYWNA("Defensywna"),
    MOBILNOSC("Mobilność"),
    AURA("Aura"),
    SPECJALNA("Specjalna"),
    WSPARCIE("Wsparcie"),
    NEEDS_MANUAL_REVIEW("NEEDS_MANUAL_REVIEW");

    private final String displayName;

    SkillCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
