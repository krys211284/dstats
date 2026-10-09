package krys.itemimport;

/** Określa, czy wartości wpisywane w formularzu są source values czy wartościami widocznymi w grze. */
public enum ItemImportValueMode {
    SOURCE_VALUE,
    GAME_DISPLAYED_VALUE;

    public static ItemImportValueMode fromFormValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return SOURCE_VALUE;
        }
        try {
            return ItemImportValueMode.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            return SOURCE_VALUE;
        }
    }
}
