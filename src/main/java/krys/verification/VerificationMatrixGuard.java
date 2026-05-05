package krys.verification;

/** Bramka chroniąca runtime przed użyciem niezweryfikowanych mechanik. */
public final class VerificationMatrixGuard {
    public VerificationUsageDecision resolveCalculationUse(String stableId, VerificationImpact requestedImpact) {
        return resolveCalculationUse(VerificationMatrix.requireByStableId(stableId), requestedImpact);
    }

    public VerificationUsageDecision resolveCalculationUse(VerificationMatrixEntry entry, VerificationImpact requestedImpact) {
        if (!entry.hasImpact(requestedImpact)) {
            return new VerificationUsageDecision(
                    entry,
                    requestedImpact,
                    false,
                    "Mechanika nie deklaruje wpływu na " + requestedImpact.getId() + "."
            );
        }

        if (entry.getCurrentStatus() == VerificationStatus.VERIFIED) {
            return new VerificationUsageDecision(
                    entry,
                    requestedImpact,
                    true,
                    "Mechanika zweryfikowana i może wpływać na kalkulację."
            );
        }

        return switch (entry.getDefaultEngineBehavior()) {
            case IGNORED -> new VerificationUsageDecision(
                    entry,
                    requestedImpact,
                    false,
                    "Mechanika wymaga weryfikacji i zostaje pominięta w kalkulacji."
            );
            case METADATA_ONLY -> new VerificationUsageDecision(
                    entry,
                    requestedImpact,
                    false,
                    "Mechanika wymaga weryfikacji i może być użyta wyłącznie jako metadane."
            );
            case BLOCKED -> throw new VerificationBlockedMechanicException(
                    "Mechanika " + entry.getStableId() + " wymaga weryfikacji przed użyciem w kalkulacji " + requestedImpact.getId() + "."
            );
        };
    }

    public long addDpsContributionIfAllowed(String stableId, long currentDamage, long proposedUnverifiedContribution) {
        VerificationUsageDecision decision = resolveCalculationUse(stableId, VerificationImpact.DPS);
        if (!decision.isAllowedToAffectCalculation()) {
            return currentDamage;
        }
        return currentDamage + proposedUnverifiedContribution;
    }
}
