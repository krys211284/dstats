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
    private final String affixDefinitionId;
    private final Double rollRangeMin;
    private final Double rollRangeMax;
    private final Double referenceValue;
    private final String displayValue;
    private final String visualSourceText;
    private final int visualDisplayOrder;
    private final boolean greaterAffixConfirmationRequired;

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
        this(type, value, unit, greaterAffix, displayOrder, rawOcrLine, source, "", null, null, "");
    }

    public ImportedItemAffix(ImportedItemAffixType type,
                             double value,
                             String unit,
                             boolean greaterAffix,
                             int displayOrder,
                             String rawOcrLine,
                             ImportedItemAffixSource source,
                             String affixDefinitionId,
                             Double rollRangeMin,
                             Double rollRangeMax,
                             String displayValue) {
        this(type, value, unit, greaterAffix, displayOrder, rawOcrLine, source, affixDefinitionId,
                rollRangeMin, rollRangeMax, null, displayValue);
    }

    public ImportedItemAffix(ImportedItemAffixType type,
                             double value,
                             String unit,
                             boolean greaterAffix,
                             int displayOrder,
                             String rawOcrLine,
                             ImportedItemAffixSource source,
                             String affixDefinitionId,
                             Double rollRangeMin,
                             Double rollRangeMax,
                             Double referenceValue,
                             String displayValue) {
        this(type, value, unit, greaterAffix, displayOrder, rawOcrLine, source, affixDefinitionId,
                rollRangeMin, rollRangeMax, referenceValue, displayValue, rawOcrLine, displayOrder, false);
    }

    public ImportedItemAffix(ImportedItemAffixType type,
                             double value,
                             String unit,
                             boolean greaterAffix,
                             int displayOrder,
                             String rawOcrLine,
                             ImportedItemAffixSource source,
                             String affixDefinitionId,
                             Double rollRangeMin,
                             Double rollRangeMax,
                             Double referenceValue,
                             String displayValue,
                             String visualSourceText,
                             int visualDisplayOrder) {
        this(type, value, unit, greaterAffix, displayOrder, rawOcrLine, source, affixDefinitionId,
                rollRangeMin, rollRangeMax, referenceValue, displayValue, visualSourceText, visualDisplayOrder, false);
    }

    private ImportedItemAffix(ImportedItemAffixType type,
                              double value,
                              String unit,
                              boolean greaterAffix,
                              int displayOrder,
                              String rawOcrLine,
                              ImportedItemAffixSource source,
                              String affixDefinitionId,
                              Double rollRangeMin,
                              Double rollRangeMax,
                              Double referenceValue,
                              String displayValue,
                              String visualSourceText,
                              int visualDisplayOrder,
                              boolean greaterAffixConfirmationRequired) {
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
        this.affixDefinitionId = affixDefinitionId == null ? "" : affixDefinitionId;
        this.rollRangeMin = nonNegativeRangeValue(rollRangeMin);
        this.rollRangeMax = nonNegativeRangeValue(rollRangeMax);
        this.referenceValue = nonNegativeRangeValue(referenceValue);
        this.displayValue = displayValue == null ? "" : displayValue;
        this.visualSourceText = visualSourceText == null || visualSourceText.isBlank()
                ? this.rawOcrLine
                : visualSourceText;
        this.visualDisplayOrder = Math.max(0, visualDisplayOrder);
        this.greaterAffixConfirmationRequired = greaterAffixConfirmationRequired;
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

    public String getAffixDefinitionId() {
        return affixDefinitionId.isBlank() ? type.name() : affixDefinitionId;
    }

    public Double getRollRangeMin() {
        return rollRangeMin;
    }

    public Double getRollRangeMax() {
        return rollRangeMax;
    }

    public Double getReferenceValue() {
        return referenceValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public String getVisualSourceText() {
        return visualSourceText;
    }

    public int getVisualDisplayOrder() {
        return visualDisplayOrder;
    }

    public boolean isGreaterAffixConfirmationRequired() {
        return greaterAffixConfirmationRequired;
    }

    public ImportedItemAffix withVisualAnchor(String newVisualSourceText,
                                              int newVisualDisplayOrder,
                                              boolean newGreaterAffix,
                                              boolean newGreaterAffixConfirmationRequired) {
        return new ImportedItemAffix(
                type,
                value,
                unit,
                newGreaterAffix,
                displayOrder,
                rawOcrLine,
                source,
                affixDefinitionId,
                rollRangeMin,
                rollRangeMax,
                referenceValue,
                displayValue,
                newVisualSourceText,
                newVisualDisplayOrder,
                newGreaterAffixConfirmationRequired
        );
    }

    public String getValueLabel() {
        return displayValue.isBlank() ? formatValue(value) + unit : displayValue;
    }

    public String getRollRangeLabel() {
        if (rollRangeMin == null || rollRangeMax == null) {
            return "";
        }
        return formatRangeValue(rollRangeMin, type) + " - " + formatRangeValue(rollRangeMax, type);
    }

    public String getReferenceValueLabel() {
        if (referenceValue == null) {
            return "";
        }
        return formatRangeValue(referenceValue, type);
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
            case BLOCK_CHANCE, RETRIBUTION_CHANCE, CRITICAL_STRIKE_CHANCE, LUCKY_HIT_CHANCE, COOLDOWN_REDUCTION,
                 MOVEMENT_SPEED, DODGE_CHANCE, DAMAGE_REDUCTION, ALL_DAMAGE_MULTIPLIER, DAMAGE_OVER_TIME_MULTIPLIER -> "%";
            case STRENGTH, INTELLIGENCE, THORNS, ALL_RESISTANCE, FIRE_RESISTANCE, WEAPON_DAMAGE_FLAT, MAXIMUM_LIFE, LIFE_ON_HIT,
                 LIFE_ON_KILL, LUCKY_HIT_PRIMARY_RESOURCE, CORE_SKILL_RANKS -> "";
        };
    }

    private static Double nonNegativeRangeValue(Double value) {
        if (value != null && value < 0.0d) {
            throw new IllegalArgumentException("Zakres rolla affixu nie może być ujemny.");
        }
        return value;
    }

    private static String formatValue(double value) {
        if (Math.rint(value) == value) {
            return String.format(java.util.Locale.US, "%.0f", value);
        }
        return String.format(java.util.Locale.US, "%.2f", value).replace('.', ',');
    }

    private static String formatRangeValue(double value, ImportedItemAffixType type) {
        if (type == ImportedItemAffixType.DAMAGE_REDUCTION) {
            return String.format(java.util.Locale.US, "%.1f", value).replace('.', ',');
        }
        return formatValue(value);
    }
}
