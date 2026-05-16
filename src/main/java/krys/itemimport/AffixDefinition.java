package krys.itemimport;

import java.util.List;

/** Znany affix używany przez OCR, ręczną korektę i przyszłe budowanie idealnych itemów. */
public final class AffixDefinition {
    private final String id;
    private final ImportedItemAffixType formType;
    private final String displayName;
    private final String description;
    private final AffixCategory category;
    private final List<String> ocrAliases;
    private final AffixValueUnit valueUnit;
    private final Double catalogValue;
    private final Double rollRangeMin;
    private final Double rollRangeMax;
    private final Double chancePercent;
    private final Double resourceAmount;
    private final AffixRuntimeStatus runtimeStatus;
    private final boolean automaticMatchingAllowed;
    private final boolean manualVerificationRequired;

    public AffixDefinition(String id,
                           ImportedItemAffixType formType,
                           String displayName,
                           AffixCategory category,
                           List<String> ocrAliases,
                           AffixValueUnit valueUnit,
                           Double catalogValue,
                           Double rollRangeMin,
                           Double rollRangeMax,
                           Double chancePercent,
                           Double resourceAmount,
                           AffixRuntimeStatus runtimeStatus,
                           boolean automaticMatchingAllowed,
                           boolean manualVerificationRequired) {
        this(id, formType, displayName, displayName, category, ocrAliases, valueUnit, catalogValue, rollRangeMin,
                rollRangeMax, chancePercent, resourceAmount, runtimeStatus, automaticMatchingAllowed,
                manualVerificationRequired);
    }

    public AffixDefinition(String id,
                           ImportedItemAffixType formType,
                           String displayName,
                           String description,
                           AffixCategory category,
                           List<String> ocrAliases,
                           AffixValueUnit valueUnit,
                           Double catalogValue,
                           Double rollRangeMin,
                           Double rollRangeMax,
                           Double chancePercent,
                           Double resourceAmount,
                           AffixRuntimeStatus runtimeStatus,
                           boolean automaticMatchingAllowed,
                           boolean manualVerificationRequired) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id affixu jest wymagane.");
        }
        if (formType == null) {
            throw new IllegalArgumentException("Typ formularza affixu jest wymagany.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Nazwa affixu jest wymagana.");
        }
        if (ocrAliases == null || ocrAliases.isEmpty()) {
            throw new IllegalArgumentException("Affix musi mieć co najmniej jeden alias OCR.");
        }
        this.id = id;
        this.formType = formType;
        this.displayName = displayName;
        this.description = description == null || description.isBlank() ? displayName : description;
        this.category = category == null ? AffixCategory.UTILITY : category;
        this.ocrAliases = List.copyOf(ocrAliases);
        this.valueUnit = valueUnit == null ? AffixValueUnit.FLAT : valueUnit;
        this.catalogValue = catalogValue;
        this.rollRangeMin = rollRangeMin;
        this.rollRangeMax = rollRangeMax;
        this.chancePercent = chancePercent;
        this.resourceAmount = resourceAmount;
        this.runtimeStatus = runtimeStatus == null ? AffixRuntimeStatus.DESCRIPTIVE_ONLY : runtimeStatus;
        this.automaticMatchingAllowed = automaticMatchingAllowed;
        this.manualVerificationRequired = manualVerificationRequired;
    }

    public String getId() {
        return id;
    }

    public ImportedItemAffixType getFormType() {
        return formType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public AffixCategory getCategory() {
        return category;
    }

    public List<String> getOcrAliases() {
        return ocrAliases;
    }

    public AffixValueUnit getValueUnit() {
        return valueUnit;
    }

    public Double getCatalogValue() {
        return catalogValue;
    }

    public Double getRollRangeMin() {
        return rollRangeMin;
    }

    public Double getRollRangeMax() {
        return rollRangeMax;
    }

    public Double getChancePercent() {
        return chancePercent;
    }

    public Double getResourceAmount() {
        return resourceAmount;
    }

    public AffixRuntimeStatus getRuntimeStatus() {
        return runtimeStatus;
    }

    public boolean isAutomaticMatchingAllowed() {
        return automaticMatchingAllowed;
    }

    public boolean isManualVerificationRequired() {
        return manualVerificationRequired;
    }
}
