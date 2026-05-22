package krys.transfiguration;

/** Źródłowa definicja regularnego wyniku Przeistoczenia. */
public final class HoradricTransfigurationOutcomeDefinition {
    private final HoradricTransfigurationOutcome outcome;
    private final String description;
    private final String sampleChanceLabel;

    public HoradricTransfigurationOutcomeDefinition(HoradricTransfigurationOutcome outcome,
                                                    String description,
                                                    String sampleChanceLabel) {
        this.outcome = outcome;
        this.description = description == null ? "" : description;
        this.sampleChanceLabel = sampleChanceLabel == null ? "" : sampleChanceLabel;
    }

    public HoradricTransfigurationOutcome getOutcome() {
        return outcome;
    }

    public String getDescription() {
        return description;
    }

    public String getSampleChanceLabel() {
        return sampleChanceLabel;
    }
}
