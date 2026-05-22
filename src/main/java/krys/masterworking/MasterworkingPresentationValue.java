package krys.masterworking;

/** Wynik prezentacyjnego przeliczenia wartosci itemu przez Doskonalenie. */
public final class MasterworkingPresentationValue {
    private final String label;
    private final String baseValueLabel;
    private final String displayValueLabel;
    private final String suffixLabel;
    private final boolean supported;
    private final boolean perfected;
    private final String note;

    public MasterworkingPresentationValue(String label,
                                          String baseValueLabel,
                                          String displayValueLabel,
                                          String suffixLabel,
                                          boolean supported,
                                          boolean perfected,
                                          String note) {
        this.label = label == null ? "" : label;
        this.baseValueLabel = baseValueLabel == null ? "" : baseValueLabel;
        this.displayValueLabel = displayValueLabel == null ? "" : displayValueLabel;
        this.suffixLabel = suffixLabel == null ? "" : suffixLabel;
        this.supported = supported;
        this.perfected = perfected;
        this.note = note == null ? "" : note;
    }

    public String getLabel() {
        return label;
    }

    public String getBaseValueLabel() {
        return baseValueLabel;
    }

    public String getDisplayValueLabel() {
        return displayValueLabel;
    }

    public String getSuffixLabel() {
        return suffixLabel;
    }

    public boolean isSupported() {
        return supported;
    }

    public boolean isPerfected() {
        return perfected;
    }

    public String getNote() {
        return note;
    }

    public boolean hasChangedValue() {
        return !baseValueLabel.equals(displayValueLabel);
    }
}
