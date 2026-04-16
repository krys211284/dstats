package krys.app;

import krys.combat.DamageEngine;
import krys.simulation.ManualSimulationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrentBuildCalculationServiceTest {
    private final CurrentBuildCalculationService calculationService = new CurrentBuildCalculationService(
            new ManualSimulationService(new DamageEngine())
    );

    @Test
    void powinien_uruchomic_scenariusz_referencyjny_na_nowym_modelu_wejscia() {
        CurrentBuildCalculation calculation = calculationService.calculate(
                CurrentBuildReferenceRequests.createHolyBoltJudgement()
        );

        assertEquals(1732L, calculation.getResult().getTotalDamage());
        assertEquals(800L, calculation.getResult().getTotalReactiveDamage());
        assertEquals(60, calculation.getResult().getHorizonSeconds());
        assertEquals(13, calculation.getSnapshot().getHero().getLevel());
        assertEquals(8L, calculation.getSnapshot().getAverageWeaponDamage());
    }
}
