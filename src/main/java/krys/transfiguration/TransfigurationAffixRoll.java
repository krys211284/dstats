package krys.transfiguration;

/** Ręcznie zapisany roll affixu Przeistoczenia. */
public final class TransfigurationAffixRoll {
    private final String definitionId;
    private final double value;
    private final String element;

    public TransfigurationAffixRoll(String definitionId, double value) {
        this(definitionId, value, "");
    }

    public TransfigurationAffixRoll(String definitionId, double value, String element) {
        this.definitionId = definitionId == null ? "" : definitionId;
        this.value = value;
        this.element = element == null ? "" : element;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public double getValue() {
        return value;
    }

    public String getElement() {
        return element;
    }

    public boolean isEmpty() {
        return definitionId.isBlank();
    }
}
