package krys.itemimport;

/** Pojedynczy affix itemu po OCR albo ręcznej korekcie użytkownika. */
public final class ImportedItemAffix {
    private final ImportedItemAffixType type;
    private final double value;
    private final String unit;
    private final boolean greaterAffix;
    private final int displayOrder;
    private final String rawOcrLine;
    private final ImportedItemAffixSource source;
    private final String sourceText;

    public ImportedItemAffix(ImportedItemAffixType type, double value) {
        this(type, value, "");
    }

    public ImportedItemAffix(ImportedItemAffixType type, double value, String sourceText) {
        this(type, value, defaultUnit(type), false, 0, sourceText, ImportedItemAffixSource.OCR);
    }

    public ImportedItemAffix(ImportedItemAffixType type,
                             double value,
                             String unit,
                             boolean greaterAffix,
                             int displayOrder,
                             String rawOcrLine,
                             ImportedItemAffixSource source) {
        if (type == null) {
            throw new IllegalArgumentException("Typ affixu jest wymagany.");
        }
        if (value < 0.0d) {
            throw new IllegalArgumentException("Wartość affixu nie może być ujemna.");
        }
        this.type = type;
        this.value = value;
        this.unit = unit == null ? "" : unit;
        this.greaterAffix = greaterAffix;
        this.displayOrder = Math.max(0, displayOrder);
        this.rawOcrLine = rawOcrLine == null ? "" : rawOcrLine;
        this.source = source == null ? ImportedItemAffixSource.MANUAL : source;
        this.sourceText = this.rawOcrLine;
    }

    public ImportedItemAffixType getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public String getLabel() {
        return type.getDisplayName();
    }

    public String getName() {
        return type.getDisplayName();
    }

    public String getUnit() {
        return unit;
    }

    public boolean isGreaterAffix() {
        return greaterAffix;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getRawOcrLine() {
        return rawOcrLine;
    }

    public ImportedItemAffixSource getSource() {
        return source;
    }

    public String getSourceText() {
        return sourceText;
    }

    public String toDisplayLine() {
        String displayLine = sourceText.isBlank() ? type.formatLine(value) : sourceText;
        return greaterAffix && !startsWithGreaterMarker(displayLine) ? "* " + displayLine : displayLine;
    }

    private static boolean startsWithGreaterMarker(String line) {
        String trimmedLine = line == null ? "" : line.trim();
        return trimmedLine.startsWith("*")
                || trimmedLine.startsWith("★")
                || trimmedLine.startsWith("⭐")
                || trimmedLine.startsWith("✦");
    }

    private static String defaultUnit(ImportedItemAffixType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case BLOCK_CHANCE, RETRIBUTION_CHANCE, LUCKY_HIT_CHANCE, COOLDOWN_REDUCTION,
                 MOVEMENT_SPEED, DODGE_CHANCE -> "%";
            case STRENGTH, INTELLIGENCE, THORNS -> "";
        };
    }
}
