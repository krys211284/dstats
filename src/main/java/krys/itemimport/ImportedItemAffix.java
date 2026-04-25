package krys.itemimport;

/** Pojedynczy affix itemu po OCR albo ręcznej korekcie użytkownika. */
public final class ImportedItemAffix {
    private final ImportedItemAffixType type;
    private final double value;
    private final String sourceText;

    public ImportedItemAffix(ImportedItemAffixType type, double value) {
        this(type, value, "");
    }

    public ImportedItemAffix(ImportedItemAffixType type, double value, String sourceText) {
        if (type == null) {
            throw new IllegalArgumentException("Typ affixu jest wymagany.");
        }
        if (value < 0.0d) {
            throw new IllegalArgumentException("Wartość affixu nie może być ujemna.");
        }
        this.type = type;
        this.value = value;
        this.sourceText = sourceText == null ? "" : sourceText;
    }

    public ImportedItemAffixType getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public String getSourceText() {
        return sourceText;
    }

    public String toDisplayLine() {
        return sourceText.isBlank() ? type.formatLine(value) : sourceText;
    }
}
