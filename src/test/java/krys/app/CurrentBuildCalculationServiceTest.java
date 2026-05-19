package krys.app;

import krys.combat.DamageEngine;
import krys.combat.DamageBreakdown;
import krys.combat.DamageComponentBreakdown;
import krys.simulation.ManualSimulationService;
import krys.simulation.SimulationStepTrace;
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

    @Test
    void current_build_starcia_z_verathielem_i_tarcza_powinien_byc_blisko_screena_gry() {
        CurrentBuildRequest request = new CurrentBuildRequest(
                70,
                1664,
                304.0d,
                76.0d,
                0.0d,
                20.0d,
                0.0d,
                Map.of(SkillId.CLASH, new SkillState(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE)),
                List.of(SkillId.CLASH),
                10,
                List.of("verathiel_shard")
        );

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        SkillHitDebugSnapshot debugSnapshot = calculation.getResult().getDirectHitDebugSnapshots().getFirst();
        DamageBreakdown breakdown = debugSnapshot.getBreakdown();
        DamageComponentBreakdown mainHit = breakdown.getComponents().getFirst();
        assertEquals(1664L, calculation.getSnapshot().getAverageWeaponDamage());
        assertEquals(2.0d, breakdown.getWeaponMultiplier(), 0.0000001d);
        assertEquals(304.0d, breakdown.getMainStat(), 0.0000001d);
        assertEquals(1.38d, breakdown.getMainStatMultiplier(), 0.0000001d);
        assertEquals(115L, mainHit.getSkillDamagePercent());
        assertEquals(2.0d, breakdown.getVerathielBasicSkillMultiplier(), 0.0000001d);
        assertEquals(0.80d, breakdown.getLevelDamageReduction(), 0.0000001d);
        assertEquals(0.20d, breakdown.getDamageTakenAfterLevelReductionMultiplier(), 0.0000001d);
        assertEquals(10563L, breakdown.getRawDamage());
        assertEquals(2113L, breakdown.getFinalDamage());
        assertEquals(21130L, calculation.getResult().getTotalDamage());
        assertEquals(2113.0d, calculation.getResult().getDps(), 0.0000001d);
        assertEquals(65.0d, calculation.getResult().getFinalPrimaryResource(), 0.0000001d);
        assertEquals(250.0d, calculation.getResult().getTotalPrimaryResourceCost(), 0.0000001d);
        assertEquals(200.0d, calculation.getResult().getTotalPrimaryResourceGenerated(), 0.0000001d);
        assertEquals(15.0d, calculation.getResult().getTotalPrimaryResourceRegenerated(), 0.0000001d);
        SimulationStepTrace firstStep = calculation.getResult().getStepTrace().getFirst();
        assertEquals(100.0d, firstStep.getPrimaryResourceBefore(), 0.0000001d);
        assertEquals(25.0d, firstStep.getPrimaryResourceCost(), 0.0000001d);
        assertEquals(20.0d, firstStep.getPrimaryResourceGenerated(), 0.0000001d);
        assertEquals(1.5d, firstStep.getPrimaryResourceRegenerated(), 0.0000001d);
        assertEquals(96.5d, firstStep.getPrimaryResourceAfter(), 0.0000001d);
    }

    @Test
    void current_build_z_verathielem_i_zerowa_wiara_czeka_bez_obrazen_przez_10s() {
        CurrentBuildRequest request = new CurrentBuildRequest(
                70,
                1664,
                304.0d,
                76.0d,
                0.0d,
                20.0d,
                0.0d,
                true,
                true,
                Map.of(SkillId.CLASH, new SkillState(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE)),
                List.of(SkillId.CLASH),
                10,
                0.0d,
                100.0d,
                1.50d,
                List.of("verathiel_shard")
        );

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        assertEquals(0L, calculation.getResult().getTotalDamage());
        assertEquals(0.0d, calculation.getResult().getDps(), 0.0000001d);
        assertEquals(15.0d, calculation.getResult().getFinalPrimaryResource(), 0.0000001d);
        assertTrue(calculation.getResult().getDirectHitDebugSnapshots().isEmpty());
        assertTrue(calculation.getResult().getStepTrace().getFirst().getSelectionReason().contains("brak zasobu"));
        assertEquals(0.0d, calculation.getResult().getStepTrace().getFirst().getPrimaryResourceBefore(), 0.0000001d);
        assertEquals(1.5d, calculation.getResult().getStepTrace().getFirst().getPrimaryResourceAfter(), 0.0000001d);
    }

    @Test
    void starcie_bez_verathiela_nie_ma_kosztu_25_i_generuje_wiare() {
        CurrentBuildRequest request = new CurrentBuildRequest(
                70,
                1664,
                304.0d,
                76.0d,
                0.0d,
                20.0d,
                0.0d,
                true,
                true,
                Map.of(SkillId.CLASH, new SkillState(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE)),
                List.of(SkillId.CLASH),
                10,
                0.0d,
                100.0d,
                1.50d,
                List.of()
        );

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        assertTrue(calculation.getResult().getTotalDamage() > 0L);
        assertEquals(1.0d, calculation.getResult().getDirectHitDebugSnapshots().getFirst()
                .getBreakdown().getVerathielBasicSkillMultiplier(), 0.0000001d);
        SimulationStepTrace firstStep = calculation.getResult().getStepTrace().getFirst();
        assertEquals(0.0d, firstStep.getPrimaryResourceBefore(), 0.0000001d);
        assertEquals(0.0d, firstStep.getPrimaryResourceCost(), 0.0000001d);
        assertEquals(20.0d, firstStep.getPrimaryResourceGenerated(), 0.0000001d);
        assertEquals(1.5d, firstStep.getPrimaryResourceRegenerated(), 0.0000001d);
        assertEquals(21.5d, firstStep.getPrimaryResourceAfter(), 0.0000001d);
        assertEquals(100.0d, calculation.getResult().getFinalPrimaryResource(), 0.0000001d);
    }
}
