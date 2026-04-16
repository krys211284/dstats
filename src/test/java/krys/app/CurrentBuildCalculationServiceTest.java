package krys.app;

import krys.combat.DamageEngine;
import krys.simulation.ManualSimulationService;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrentBuildCalculationServiceTest {
    private final CurrentBuildCalculationService calculationService = new CurrentBuildCalculationService(
            new ManualSimulationService(new DamageEngine())
    );

    @Test
    void powinien_uruchomic_obliczenie_na_realnym_modelu_wejscia_uzytkownika_m8() {
        CurrentBuildRequest request = new CurrentBuildRequest(
                13,
                8,
                18.0d,
                0.0d,
                50.0d,
                50.0d,
                50.0d,
                Map.of(
                        SkillId.HOLY_BOLT,
                        new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE)
                ),
                List.of(SkillId.HOLY_BOLT),
                60
        );
        CurrentBuildCalculation calculation = calculationService.calculate(request);

        assertEquals(1732L, calculation.getResult().getTotalDamage());
        assertEquals(800L, calculation.getResult().getTotalReactiveDamage());
        assertEquals(60, calculation.getResult().getHorizonSeconds());
        assertEquals(13, calculation.getSnapshot().getHero().getLevel());
        assertEquals(8L, calculation.getSnapshot().getAverageWeaponDamage());
        assertEquals(List.of(SkillId.HOLY_BOLT), calculation.getRequest().getActionBar());
        assertEquals(1, calculation.getResult().getDirectHitDebugSnapshots().size());
    }
}
