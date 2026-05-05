package krys.verification;

/** Wynik decyzji, czy mechanika może wpłynąć na kalkulację. */
public final class VerificationUsageDecision {
    private final VerificationMatrixEntry entry;
    private final VerificationImpact requestedImpact;
    private final boolean allowedToAffectCalculation;
    private final String message;

    public VerificationUsageDecision(VerificationMatrixEntry entry,
                                     VerificationImpact requestedImpact,
                                     boolean allowedToAffectCalculation,
                                     String message) {
        this.entry = entry;
        this.requestedImpact = requestedImpact;
        this.allowedToAffectCalculation = allowedToAffectCalculation;
        this.message = message;
    }

    public VerificationMatrixEntry getEntry() {
        return entry;
    }

    public VerificationImpact getRequestedImpact() {
        return requestedImpact;
    }

    public boolean isAllowedToAffectCalculation() {
        return allowedToAffectCalculation;
    }

    public String getMessage() {
        return message;
    }
}
