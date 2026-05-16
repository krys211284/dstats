package krys.itemimport;

/** Kategoria affixu w katalogu importu itemów. */
public enum AffixCategory {
    OFFENSIVE("Ofensywny"),
    DEFENSIVE("Defensywny"),
    RESOURCE("Zasób"),
    UTILITY("Użytkowy"),
    ATTRIBUTE("Atrybut");

    private final String displayName;

    AffixCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
