package krys.itemimport.image;

import java.util.List;

/** Neutralny odczyt wartosci PropertyBlock po OCR i klasyfikacji semantycznej. */
public final class PropertyBlockValueReadResult {
    private final StructuralItemPartSlot sourcePart;
    private final int blockOrder;
    private final PropertyBlockSemanticKind semanticKind;
    private final String definitionId;
    private final String valueRole;
    private final Double value;
    private final String unit;
    private final Double rollRangeMin;
    private final Double rollRangeMax;
    private final Double referenceValue;
    private final Long itemArmor;
    private final Long weaponDps;
    private final Long weaponDamageMin;
    private final Long weaponDamageMax;
    private final Double attacksPerSecond;
    private final List<NumericParameter> parameters;
    private final boolean certain;
    private final boolean ambiguous;
    private final List<String> diagnostics;
    private final List<PropertyBlockNumericEvidence> rawNumericEvidence;

    public PropertyBlockValueReadResult(StructuralItemPartSlot sourcePart,
                                        int blockOrder,
                                        PropertyBlockSemanticKind semanticKind,
                                        String definitionId,
                                        String valueRole,
                                        Double value,
                                        String unit,
                                        Double rollRangeMin,
                                        Double rollRangeMax,
                                        Double referenceValue,
                                        Long itemArmor,
                                        Long weaponDps,
                                        Long weaponDamageMin,
                                        Long weaponDamageMax,
                                        Double attacksPerSecond,
                                        List<NumericParameter> parameters,
                                        boolean certain,
                                        boolean ambiguous,
                                        List<String> diagnostics,
                                        List<PropertyBlockNumericEvidence> rawNumericEvidence) {
        this.sourcePart = sourcePart;
        this.blockOrder = Math.max(1, blockOrder);
        this.semanticKind = semanticKind == null ? PropertyBlockSemanticKind.UNKNOWN : semanticKind;
        this.definitionId = definitionId == null ? "" : definitionId;
        this.valueRole = valueRole == null ? "" : valueRole;
        this.value = nonNegativeOrNull(value);
        this.unit = unit == null ? "" : unit;
        this.rollRangeMin = nonNegativeOrNull(rollRangeMin);
        this.rollRangeMax = nonNegativeOrNull(rollRangeMax);
        this.referenceValue = nonNegativeOrNull(referenceValue);
        this.itemArmor = nonNegativeOrNull(itemArmor);
        this.weaponDps = nonNegativeOrNull(weaponDps);
        this.weaponDamageMin = nonNegativeOrNull(weaponDamageMin);
        this.weaponDamageMax = nonNegativeOrNull(weaponDamageMax);
        this.attacksPerSecond = nonNegativeOrNull(attacksPerSecond);
        this.parameters = List.copyOf(parameters == null ? List.of() : parameters);
        this.certain = certain;
        this.ambiguous = ambiguous;
        this.diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
        this.rawNumericEvidence = List.copyOf(rawNumericEvidence == null ? List.of() : rawNumericEvidence);
    }

    public StructuralItemPartSlot getSourcePart() {
        return sourcePart;
    }

    public int getBlockOrder() {
        return blockOrder;
    }

    public PropertyBlockSemanticKind getSemanticKind() {
        return semanticKind;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getValueRole() {
        return valueRole;
    }

    public Double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
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

    public Long getItemArmor() {
        return itemArmor;
    }

    public Long getWeaponDps() {
        return weaponDps;
    }

    public Long getWeaponDamageMin() {
        return weaponDamageMin;
    }

    public Long getWeaponDamageMax() {
        return weaponDamageMax;
    }

    public Double getAttacksPerSecond() {
        return attacksPerSecond;
    }

    public List<NumericParameter> getParameters() {
        return parameters;
    }

    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    public boolean isCertain() {
        return certain;
    }

    public boolean isAmbiguous() {
        return ambiguous;
    }

    public List<String> getDiagnostics() {
        return diagnostics;
    }

    public List<PropertyBlockNumericEvidence> getRawNumericEvidence() {
        return rawNumericEvidence;
    }

    public boolean hasSingleValue() {
        return value != null;
    }

    public boolean hasRollRange() {
        return rollRangeMin != null && rollRangeMax != null;
    }

    public boolean hasReferenceValue() {
        return referenceValue != null;
    }

    public boolean hasBaseStatValue() {
        return itemArmor != null
                || weaponDps != null
                || weaponDamageMin != null
                || weaponDamageMax != null
                || attacksPerSecond != null;
    }

    private static Double nonNegativeOrNull(Double value) {
        if (value != null && value < 0.0d) {
            throw new IllegalArgumentException("Wartosc PropertyBlock nie moze byc ujemna.");
        }
        return value;
    }

    private static Long nonNegativeOrNull(Long value) {
        if (value != null && value < 0L) {
            throw new IllegalArgumentException("Wartosc PropertyBlock nie moze byc ujemna.");
        }
        return value;
    }

    /** Jedna liczba albo zakres zachowany w kolejnosci z NumericEvidence. */
    public static final class NumericParameter {
        private final int order;
        private final String role;
        private final String rawToken;
        private final Double value;
        private final Double rangeMin;
        private final Double rangeMax;
        private final String unit;
        private final boolean ambiguous;
        private final List<String> alternativeValues;

        public NumericParameter(int order,
                                String role,
                                String rawToken,
                                Double value,
                                Double rangeMin,
                                Double rangeMax,
                                String unit,
                                boolean ambiguous,
                                List<String> alternativeValues) {
            this.order = Math.max(1, order);
            this.role = role == null ? "" : role;
            this.rawToken = rawToken == null ? "" : rawToken;
            this.value = nonNegativeOrNull(value);
            this.rangeMin = nonNegativeOrNull(rangeMin);
            this.rangeMax = nonNegativeOrNull(rangeMax);
            this.unit = unit == null ? "" : unit;
            this.ambiguous = ambiguous;
            this.alternativeValues = List.copyOf(alternativeValues == null ? List.of() : alternativeValues);
        }

        public int getOrder() {
            return order;
        }

        public String getRole() {
            return role;
        }

        public String getRawToken() {
            return rawToken;
        }

        public Double getValue() {
            return value;
        }

        public Double getRangeMin() {
            return rangeMin;
        }

        public Double getRangeMax() {
            return rangeMax;
        }

        public String getUnit() {
            return unit;
        }

        public boolean isAmbiguous() {
            return ambiguous;
        }

        public List<String> getAlternativeValues() {
            return alternativeValues;
        }

        public boolean hasRange() {
            return rangeMin != null && rangeMax != null;
        }
    }
}
