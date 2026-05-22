package krys.transfiguration;

/** Dane itemu dla Przeistoczenia; w tym etapie nie wpływają na runtime. */
public final class ItemTransfiguration {
    private final boolean transfigured;
    private final boolean lockedAfterTransfiguration;
    private final HoradricTuningPrism tuningPrism;
    private final HoradricTransfigurationOutcome outcome;
    private final String upgradedAffixRef;
    private final TransfigurationAffixRoll addedTransfigurationAffix;
    private final String replacedAffixRef;
    private final TransfigurationAffixRoll replacementTransfigurationAffix;
    private final Integer bonusQuality;
    private final boolean indestructible;
    private final String notes;

    public ItemTransfiguration(boolean transfigured,
                               boolean lockedAfterTransfiguration,
                               HoradricTuningPrism tuningPrism,
                               HoradricTransfigurationOutcome outcome,
                               String upgradedAffixRef,
                               TransfigurationAffixRoll addedTransfigurationAffix,
                               String replacedAffixRef,
                               TransfigurationAffixRoll replacementTransfigurationAffix,
                               Integer bonusQuality,
                               boolean indestructible,
                               String notes) {
        if (!transfigured) {
            this.transfigured = false;
            this.lockedAfterTransfiguration = false;
            this.tuningPrism = HoradricTuningPrism.NONE;
            this.outcome = HoradricTransfigurationOutcome.NONE;
            this.upgradedAffixRef = "";
            this.addedTransfigurationAffix = null;
            this.replacedAffixRef = "";
            this.replacementTransfigurationAffix = null;
            this.bonusQuality = null;
            this.indestructible = false;
            this.notes = "";
            return;
        }
        HoradricTransfigurationOutcome safeOutcome = outcome == null || outcome == HoradricTransfigurationOutcome.NONE
                ? HoradricTransfigurationOutcome.UNKNOWN
                : outcome;
        this.transfigured = true;
        this.lockedAfterTransfiguration = lockedAfterTransfiguration;
        this.tuningPrism = tuningPrism == null ? HoradricTuningPrism.NONE : tuningPrism;
        this.outcome = safeOutcome;
        this.upgradedAffixRef = upgradedAffixRef == null ? "" : upgradedAffixRef;
        this.addedTransfigurationAffix = addedTransfigurationAffix;
        this.replacedAffixRef = replacedAffixRef == null ? "" : replacedAffixRef;
        this.replacementTransfigurationAffix = replacementTransfigurationAffix;
        this.bonusQuality = bonusQuality;
        this.indestructible = indestructible || safeOutcome == HoradricTransfigurationOutcome.INDESTRUCTIBLE;
        this.notes = notes == null ? "" : notes;
    }

    public static ItemTransfiguration none() {
        return new ItemTransfiguration(false, false, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.NONE,
                "", null, "", null, null, false, "");
    }

    public static ItemTransfiguration transfigured(HoradricTransfigurationOutcome outcome) {
        return new ItemTransfiguration(true, true, HoradricTuningPrism.NONE, outcome,
                "", null, "", null, null, outcome == HoradricTransfigurationOutcome.INDESTRUCTIBLE, "");
    }

    public boolean isTransfigured() {
        return transfigured;
    }

    public boolean isLockedAfterTransfiguration() {
        return lockedAfterTransfiguration;
    }

    public HoradricTuningPrism getTuningPrism() {
        return tuningPrism;
    }

    public HoradricTransfigurationOutcome getOutcome() {
        return outcome;
    }

    public String getUpgradedAffixRef() {
        return upgradedAffixRef;
    }

    public TransfigurationAffixRoll getAddedTransfigurationAffix() {
        return addedTransfigurationAffix;
    }

    public String getReplacedAffixRef() {
        return replacedAffixRef;
    }

    public TransfigurationAffixRoll getReplacementTransfigurationAffix() {
        return replacementTransfigurationAffix;
    }

    public Integer getBonusQuality() {
        return bonusQuality;
    }

    public boolean isIndestructible() {
        return indestructible;
    }

    public String getNotes() {
        return notes;
    }
}
