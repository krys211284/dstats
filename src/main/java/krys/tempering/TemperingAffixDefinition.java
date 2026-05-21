package krys.tempering;

/** Definicja katalogowa hartowanego affixu. */
public final class TemperingAffixDefinition {
    private final String id;
    private final TemperingCategory category;
    private final String displayName;
    private final String descriptionTemplate;
    private final double rangeMin;
    private final double rangeMax;
    private final TemperingValueUnit unit;
    private final TemperingRuntimeStatus runtimeStatus;
    private final String notes;
    private final Double greaterAffixValueOverride;

    public TemperingAffixDefinition(String id,
                                    TemperingCategory category,
                                    String displayName,
                                    String descriptionTemplate,
                                    double rangeMin,
                                    double rangeMax,
                                    TemperingValueUnit unit,
                                    TemperingRuntimeStatus runtimeStatus,
                                    String notes) {
        this(id, category, displayName, descriptionTemplate, rangeMin, rangeMax, unit, runtimeStatus, notes, null);
    }

    public TemperingAffixDefinition(String id,
                                    TemperingCategory category,
                                    String displayName,
                                    String descriptionTemplate,
                                    double rangeMin,
                                    double rangeMax,
                                    TemperingValueUnit unit,
                                    TemperingRuntimeStatus runtimeStatus,
                                    String notes,
                                    Double greaterAffixValueOverride) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id hartowania jest wymagane.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Kategoria hartowania jest wymagana.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Nazwa hartowanego affixu jest wymagana.");
        }
        if (rangeMax < rangeMin) {
            throw new IllegalArgumentException("Zakres hartowanego affixu jest niepoprawny.");
        }
        this.id = id;
        this.category = category;
        this.displayName = displayName;
        this.descriptionTemplate = descriptionTemplate == null ? displayName : descriptionTemplate;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
        this.unit = unit == null ? TemperingValueUnit.FLAT : unit;
        this.runtimeStatus = runtimeStatus == null ? TemperingRuntimeStatus.DATA_ONLY : runtimeStatus;
        this.notes = notes == null ? "" : notes;
        this.greaterAffixValueOverride = greaterAffixValueOverride;
    }

    public String getId() {
        return id;
    }

    public TemperingCategory getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescriptionTemplate() {
        return descriptionTemplate;
    }

    public double getRangeMin() {
        return rangeMin;
    }

    public double getRangeMax() {
        return rangeMax;
    }

    public TemperingValueUnit getUnit() {
        return unit;
    }

    public TemperingRuntimeStatus getRuntimeStatus() {
        return runtimeStatus;
    }

    public String getNotes() {
        return notes;
    }

    public Double getGreaterAffixValueOverride() {
        return greaterAffixValueOverride;
    }

    public double greaterAffixValue() {
        if (greaterAffixValueOverride != null) {
            return greaterAffixValueOverride;
        }
        return rangeMax * 1.25d;
    }

    public boolean accepts(double value) {
        return value >= rangeMin && value <= rangeMax;
    }
}
