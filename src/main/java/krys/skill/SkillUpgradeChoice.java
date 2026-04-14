package krys.skill;

/** Wybór dodatkowego modyfikatora skilla. */
public enum SkillUpgradeChoice {
    NONE("Brak"),
    LEFT("Powrót światłości"),
    MIDDLE("Miecz Mistrzostwa"),
    RIGHT("Krzyżowe uderzenie (Vulnerable)");

    private final String displayName;

    SkillUpgradeChoice(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
