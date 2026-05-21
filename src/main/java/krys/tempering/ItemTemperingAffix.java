package krys.tempering;

/** Hartowany affix zapisany na konkretnym itemie. */
public final class ItemTemperingAffix {
    private final String definitionId;
    private final TemperingCategory category;
    private final double value;
    private final String displayText;
    private final TemperingRuntimeStatus runtimeStatus;

    public ItemTemperingAffix(String definitionId,
                              TemperingCategory category,
                              double value,
                              String displayText,
                              TemperingRuntimeStatus runtimeStatus) {
        if (definitionId == null || definitionId.isBlank()) {
            throw new IllegalArgumentException("Id definicji hartowania jest wymagane.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Kategoria hartowania jest wymagana.");
        }
        if (value < 0.0d) {
            throw new IllegalArgumentException("Wartość hartowania nie może być ujemna.");
        }
        this.definitionId = definitionId;
        this.category = category;
        this.value = value;
        this.displayText = displayText == null ? "" : displayText;
        this.runtimeStatus = runtimeStatus == null ? TemperingRuntimeStatus.DATA_ONLY : runtimeStatus;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public TemperingCategory getCategory() {
        return category;
    }

    public double getValue() {
        return value;
    }

    public String getDisplayText() {
        return displayText;
    }

    public TemperingRuntimeStatus getRuntimeStatus() {
        return runtimeStatus;
    }
}
