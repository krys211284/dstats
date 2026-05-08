package krys.paladin;

/** Prezentacyjna kategoria/tier umiejętności w rankingu, oparta o lokalne źródła. */
public enum SkillCategory {
    PODSTAWOWE("Podstawowe", 10),
    ADEPT("Adept", 20),
    SEDZIA("Sędzia", 30),
    MOLOCH("Moloch", 40),
    MOBILNOSC("Mobilność", 50),
    FANATYK("Fanatyk", 60),
    CORE("Core", 70),
    AURA("Aura", 80),
    ODWAGA("Odwaga", 90),
    WSPARCIE("Wsparcie", 100),
    DEFENSYWNA("Defensywna", 110),
    SPECJALNA("Specjalna", 120),
    NEEDS_MANUAL_REVIEW("NEEDS_MANUAL_REVIEW", 1000);

    private final String displayName;
    private final int displayOrder;

    SkillCategory(String displayName, int displayOrder) {
        this.displayName = displayName;
        this.displayOrder = displayOrder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
