package krys.web;

/** Produktowy status modułu widoczny w hubie aplikacji i na stronach placeholderów. */
public enum AppModuleStatus {
    AVAILABLE("Dostępne", "status-available"),
    IN_PREPARATION("W przygotowaniu", "status-in-preparation"),
    AFTER_EXPANSION_LAUNCH("Po premierze dodatku", "status-after-expansion"),
    REQUIRES_EXPANSION("Wymaga dodatku", "status-requires-expansion"),
    SEASONAL("Sezonowe", "status-seasonal");

    private final String displayName;
    private final String cssClassName;

    AppModuleStatus(String displayName, String cssClassName) {
        this.displayName = displayName;
        this.cssClassName = cssClassName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClassName() {
        return cssClassName;
    }
}
