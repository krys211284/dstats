package krys.itemimport;

/** Pojedyncza wartość liczbowa aspektu, np. główny mnożnik albo szansa. */
public final class AspectRollDefinition {
    private final String id;
    private final String label;
    private final double currentValue;
    private final Double rangeMin;
    private final Double rangeMax;
    private final String valueSuffix;

    public AspectRollDefinition(String id,
                                String label,
                                double currentValue,
                                Double rangeMin,
                                Double rangeMax,
                                String valueSuffix) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id wartości aspektu jest wymagane.");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Nazwa wartości aspektu jest wymagana.");
        }
        this.id = id;
        this.label = label;
        this.currentValue = currentValue;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
        this.valueSuffix = valueSuffix == null ? "" : valueSuffix;
    }

    public static AspectRollDefinition roll(String id,
                                            String label,
                                            double currentValue,
                                            double rangeMin,
                                            double rangeMax,
                                            String valueSuffix) {
        return new AspectRollDefinition(id, label, currentValue, rangeMin, rangeMax, valueSuffix);
    }

    public static AspectRollDefinition fixed(String id,
                                             String label,
                                             double currentValue,
                                             String valueSuffix) {
        return new AspectRollDefinition(id, label, currentValue, null, null, valueSuffix);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public Double getRangeMin() {
        return rangeMin;
    }

    public Double getRangeMax() {
        return rangeMax;
    }

    public String getValueSuffix() {
        return valueSuffix;
    }

    public boolean hasRange() {
        return rangeMin != null && rangeMax != null;
    }
}
