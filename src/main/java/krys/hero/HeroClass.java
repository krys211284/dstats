package krys.hero;

/** Tożsamość klasy profilu; sama obecność klasy nie oznacza obsługi obliczeń runtime. */
public enum HeroClass {
    PALADIN("paladin", "Paladyn"),
    NECROMANCER("necromancer", "Nekromanta");

    private final String technicalId;
    private final String displayName;

    HeroClass(String technicalId, String displayName) {
        this.technicalId = technicalId;
        this.displayName = displayName;
    }

    public String getTechnicalId() { return technicalId; }

    public String getDisplayName() { return displayName; }
}
