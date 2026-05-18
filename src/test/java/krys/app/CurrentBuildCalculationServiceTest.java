package krys.app;

import krys.combat.DamageEngine;
import krys.combat.DamageBreakdown;
import krys.combat.DamageComponentBreakdown;
import krys.simulation.ManualSimulationService;
import krys.simulation.SkillHitDebugSnapshot;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentBuildCalculationServiceTest {
    private final CurrentBuildCalculationService calculationService = new CurrentBuildCalculationService(
            new ManualSimulationService(new DamageEngine())
    );

    @Test
    void powinien_uruchomic_obliczenie_na_realnym_modelu_wejscia_uzytkownika_m9() {
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

    @Test
    void powinien_zachowac_regresje_manual_simulation_po_normalizacji_searcha_m91() {
        CurrentBuildRequest request = new CurrentBuildRequest(
                13,
                8,
                18.0d,
                0.0d,
                50.0d,
                50.0d,
                50.0d,
                Map.of(
                        SkillId.ADVANCE,
                        new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)
                ),
                List.of(SkillId.ADVANCE),
                10
        );

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        assertEquals(186L, calculation.getResult().getTotalDamage());
        assertEquals(120L, calculation.getResult().getTotalReactiveDamage());
        assertEquals(18.6000d, calculation.getResult().getDps(), 0.0000001d);
        assertEquals(10, calculation.getResult().getStepTrace().size());
    }

    @Test
    void current_build_paladyna_70_nie_powinien_dublowac_baseline_sily_w_damage_engine() {
        CurrentBuildRequest request = new CurrentBuildRequest(
                70,
                1664,
                79.0d,
                76.0d,
                0.0d,
                0.0d,
                0.0d,
                Map.of(SkillId.CLASH, new SkillState(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE)),
                List.of(SkillId.CLASH),
                10
        );

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        SkillHitDebugSnapshot debugSnapshot = calculation.getResult().getDirectHitDebugSnapshots().getFirst();
        DamageBreakdown breakdown = debugSnapshot.getBreakdown();
        DamageComponentBreakdown mainHit = breakdown.getComponents().getFirst();
        assertEquals(79.0d, breakdown.getMainStat(), 0.0000001d);
        assertEquals(76.0d, breakdown.getIntelligence(), 0.0000001d);
        assertEquals("Główny hit", mainHit.getName());
        assertEquals(115L, mainHit.getSkillDamagePercent());
        assertTrue(mainHit.getRawDamage() > 0L);
        assertTrue(mainHit.getFinalDamage() > 0L);
    }
}
