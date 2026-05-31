package krys.transfiguration;

/** Ręcznie zapisany roll affixu Przeistoczenia. */
public final class TransfigurationAffixRoll {
    private final String definitionId;
    private final double displayedValue;
    private final TransfigurationValueProvenance valueProvenance;
    private final String element;
    private final Double sourceRangeMin;
    private final Double sourceRangeMax;

    public TransfigurationAffixRoll(String definitionId, double displayedValue) {
        this(definitionId, displayedValue, TransfigurationValueProvenance.GAME_DISPLAYED_VALUE, "");
    }

    public TransfigurationAffixRoll(String definitionId, double displayedValue, String element) {
        this(definitionId, displayedValue, TransfigurationValueProvenance.GAME_DISPLAYED_VALUE, element);
    }

    public TransfigurationAffixRoll(String definitionId,
                                    double displayedValue,
                                    TransfigurationValueProvenance valueProvenance,
                                    String element) {
        this(definitionId, displayedValue, valueProvenance, element, null, null);
    }

    public TransfigurationAffixRoll(String definitionId,
                                    double displayedValue,
                                    TransfigurationValueProvenance valueProvenance,
                                    String element,
                                    Double sourceRangeMin,
                                    Double sourceRangeMax) {
        this.definitionId = definitionId == null ? "" : definitionId;
        this.displayedValue = displayedValue;
        this.valueProvenance = valueProvenance == null ? TransfigurationValueProvenance.UNKNOWN : valueProvenance;
        this.element = element == null ? "" : element;
        this.sourceRangeMin = sourceRangeMin;
        this.sourceRangeMax = sourceRangeMax;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public double getDisplayedValue() {
        return displayedValue;
    }

    /** Kompatybilny alias dla starszego kodu Stage 1; oznacza wartość widoczną/zapisaną, nie source roll. */
    public double getValue() {
        return displayedValue;
    }

    public TransfigurationValueProvenance getValueProvenance() {
        return valueProvenance;
    }

    public String getElement() {
        return element;
    }

    public Double getSourceRangeMin() {
        return sourceRangeMin;
    }

    public Double getSourceRangeMax() {
        return sourceRangeMax;
    }

    public boolean isEmpty() {
        return definitionId.isBlank();
    }
}
