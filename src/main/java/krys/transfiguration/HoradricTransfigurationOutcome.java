package krys.transfiguration;

/** Wynik receptury Przeistoczenie przedmiotu z Kostki Horadrimów. */
public enum HoradricTransfigurationOutcome {
    NONE("Brak"),
    INDESTRUCTIBLE("Niezniszczalny"),
    UPGRADE_TO_GREATER_AFFIX("Ulepszenie do Greater Affix"),
    BONUS_TRANSFIGURATION_AFFIX("Bonusowy affix Przeistoczenia"),
    REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX("Zamiana istniejącego affixu na affix Przeistoczenia"),
    BONUS_ITEM_QUALITY("Bonusowa jakość itemu"),
    UNKNOWN("Nieznany / do uzupełnienia");

    private final String displayName;

    HoradricTransfigurationOutcome(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static HoradricTransfigurationOutcome fromNullable(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return NONE;
        }
        try {
            return HoradricTransfigurationOutcome.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
