package krys.itemimport;

/** Tryb zastosowania zatwierdzonego itemu do istniejących statów current build. */
public enum CurrentBuildItemApplicationMode {
    OVERWRITE("Zastosuj do current build"),
    ADD_CONTRIBUTION("Dodaj wkład itemu do current build");

    private final String displayName;

    CurrentBuildItemApplicationMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
