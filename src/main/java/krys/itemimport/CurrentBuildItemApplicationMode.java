package krys.itemimport;

/** Tryb zastosowania zatwierdzonego itemu do istniejących statów aktualnego buildu. */
public enum CurrentBuildItemApplicationMode {
    OVERWRITE("Zastosuj do aktualnego buildu"),
    ADD_CONTRIBUTION("Dodaj wkład itemu do aktualnego buildu");

    private final String displayName;

    CurrentBuildItemApplicationMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
