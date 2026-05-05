package krys.verification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerificationMatrixGuardTest {
    private final VerificationMatrixGuard guard = new VerificationMatrixGuard();

    @Test
    void powinna_pominac_wklad_dps_dla_mechaniki_requires_verification_zachowanie_ignored() {
        long result = guard.addDpsContributionIfAllowed(
                "paladin-basic-ricochet-bounce-extra-projectile-single-target",
                100L,
                999L
        );

        assertEquals(100L, result);
    }

    @Test
    void powinna_traktowac_mechanike_metadata_only_jako_nieliczaca_sie_do_dps() {
        VerificationUsageDecision decision = guard.resolveCalculationUse(
                "purification-echo-hit-behavior",
                VerificationImpact.DPS
        );

        assertFalse(decision.isAllowedToAffectCalculation());
        assertEquals(VerificationDefaultEngineBehavior.METADATA_ONLY, decision.getEntry().getDefaultEngineBehavior());
    }

    @Test
    void powinna_blokowac_probe_uzycia_niezweryfikowanej_mechaniki_w_kalkulacji_dps() {
        VerificationBlockedMechanicException exception = assertThrows(
                VerificationBlockedMechanicException.class,
                () -> guard.resolveCalculationUse("shield-charge-tick-rate", VerificationImpact.DPS)
        );

        assertEquals(
                "Mechanika shield-charge-tick-rate wymaga weryfikacji przed użyciem w kalkulacji dps.",
                exception.getMessage()
        );
    }
}
