package krys.web;

/** Grupuje moduły aplikacji na potrzeby ekranu głównego i przyszłej rozbudowy. */
public enum AppModuleGroup {
    APP_SHELL("Aplikacja", "Główna warstwa wejścia i nawigacji po modułach."),
    BUILD_TOOLS("Narzędzia builda", "Moduły liczące aktualny build i wyszukujące najlepsze konfiguracje."),
    ITEMS_AND_IMPORT("Itemy i import", "Moduły importu, biblioteki i pracy z itemami przed runtime."),
    EXPANSION_AND_SEASON("Systemy dodatku i przyszłe sekcje", "Placeholdery przygotowujące strukturę aplikacji pod sekcje dodatku i sezonu.");

    private final String displayName;
    private final String description;

    AppModuleGroup(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
