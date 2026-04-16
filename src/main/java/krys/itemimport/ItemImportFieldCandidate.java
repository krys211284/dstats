package krys.itemimport;

/** Wstępnie rozpoznane pole z obrazu wraz z poziomem pewności i notą niepewności. */
public final class ItemImportFieldCandidate<T> {
    private final String rawValue;
    private final T suggestedValue;
    private final ItemImportFieldConfidence confidence;
    private final String note;

    public ItemImportFieldCandidate(String rawValue,
                                    T suggestedValue,
                                    ItemImportFieldConfidence confidence,
                                    String note) {
        this.rawValue = rawValue;
        this.suggestedValue = suggestedValue;
        this.confidence = confidence;
        this.note = note;
    }

    public static <T> ItemImportFieldCandidate<T> unknown(String note) {
        return new ItemImportFieldCandidate<>(null, null, ItemImportFieldConfidence.UNKNOWN, note);
    }

    public String getRawValue() {
        return rawValue;
    }

    public T getSuggestedValue() {
        return suggestedValue;
    }

    public ItemImportFieldConfidence getConfidence() {
        return confidence;
    }

    public String getNote() {
        return note;
    }
}
