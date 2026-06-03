package krys.tempering;

/** Hartowany affix zapisany na konkretnym itemie. */
public final class ItemTemperingAffix {
    private final String definitionId;
    private final TemperingCategory category;
    private final double value;
    private final String displayText;
    private final String sourceLine;
    private final TemperingRuntimeStatus runtimeStatus;
    private final boolean greaterAffix;

    public ItemTemperingAffix(String definitionId,
                              TemperingCategory category,
                              double value,
                              String displayText,
                              TemperingRuntimeStatus runtimeStatus) {
        this(definitionId, category, value, displayText, runtimeStatus, false);
    }

    public ItemTemperingAffix(String definitionId,
                              TemperingCategory category,
                              double value,
                              String displayText,
                              TemperingRuntimeStatus runtimeStatus,
                              boolean greaterAffix) {
        this(definitionId, category, value, displayText, "", runtimeStatus, greaterAffix);
    }

    public ItemTemperingAffix(String definitionId,
                              TemperingCategory category,
                              double value,
                              String displayText,
                              String sourceLine,
                              TemperingRuntimeStatus runtimeStatus,
                              boolean greaterAffix) {
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
        this.sourceLine = sourceLine == null ? "" : sourceLine;
        this.runtimeStatus = runtimeStatus == null ? TemperingRuntimeStatus.DATA_ONLY : runtimeStatus;
        this.greaterAffix = greaterAffix;
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

    public String getSourceLine() {
        return sourceLine;
    }

    public TemperingRuntimeStatus getRuntimeStatus() {
        return runtimeStatus;
    }

    public boolean isGreaterAffix() {
        return greaterAffix;
    }
}
