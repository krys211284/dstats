package krys.app;

import krys.combat.DamageEngine;
import krys.combat.DamageBreakdown;
import krys.combat.DamageComponentBreakdown;
import krys.paladin.PaladinOathId;
import krys.simulation.ManualSimulationService;
import krys.simulation.SimulationStepTrace;
import krys.simulation.SkillHitDebugSnapshot;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void moloch_z_zerowym_animuszem_nie_zmienia_sanity_damage_i_clampuje_minimum() {
        CurrentBuildRequest request = clashVerathielRequest(PaladinOathId.JUGGERNAUT.name(), 0.0d);

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        assertEquals(21130L, calculation.getResult().getTotalDamage());
        assertEquals(2113.0d, calculation.getResult().getDps(), 0.0000001d);
        assertEquals(65.0d, calculation.getResult().getFinalPrimaryResource(), 0.0000001d);
        assertEquals(1.0d, calculation.getResult().getInitialAnimus(), 0.0000001d);
        assertEquals(1.0d, calculation.getResult().getFinalAnimus(), 0.0000001d);
        assertEquals(0, calculation.getResult().getMolochBuffActivationCount());
        DamageBreakdown breakdown = calculation.getResult().getDirectHitDebugSnapshots().getFirst().getBreakdown();
        assertEquals(1.0d, breakdown.getMolochOathMultiplier(), 0.0000001d);
        assertEquals(2113L, breakdown.getFinalDamage());
    }

    @Test
    void moloch_bez_modyfikatora_animusz_nie_generuje_ladunkow() {
        CurrentBuildRequest request = clashVerathielRequest(PaladinOathId.JUGGERNAUT.name(), 1.0d);

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        assertEquals(21130L, calculation.getResult().getTotalDamage());
        assertEquals(2113.0d, calculation.getResult().getDps(), 0.0000001d);
        assertEquals(65.0d, calculation.getResult().getFinalPrimaryResource(), 0.0000001d);
        assertEquals(1.0d, calculation.getResult().getFinalAnimus(), 0.0000001d);
        assertEquals(0.0d, calculation.getResult().getTotalClashAnimusGenerated(), 0.0000001d);
        assertEquals(0, calculation.getResult().getMolochBuffActivationCount());
    }

    @Test
    void animusz_starcia_generuje_dwa_ladunki_po_legalnym_trafieniu() {
        CurrentBuildRequest request = clashVerathielRequest(PaladinOathId.JUGGERNAUT.name(), 1.0d, true, 1);

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        SimulationStepTrace firstStep = calculation.getResult().getStepTrace().getFirst();
        assertEquals(1.0d, firstStep.getAnimusBefore(), 0.0000001d);
        assertEquals(0.0d, firstStep.getAnimusSpent(), 0.0000001d);
        assertEquals(2.0d, firstStep.getAnimusGenerated(), 0.0000001d);
        assertEquals(3.0d, firstStep.getAnimusAfter(), 0.0000001d);
        assertEquals(2113L, firstStep.getDirectDamage());
        assertFalse(firstStep.isMolochBuffActivated());
        assertFalse(firstStep.isMolochBuffActive());
    }

    @Test
    void animusz_starcia_nie_aktywuje_molocha_w_hicie_ktory_dobija_do_osmiu() {
        CurrentBuildRequest request = clashVerathielRequest(PaladinOathId.JUGGERNAUT.name(), 7.0d, true, 2);

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        SimulationStepTrace firstStep = calculation.getResult().getStepTrace().get(0);
        assertEquals(7.0d, firstStep.getAnimusBefore(), 0.0000001d);
        assertEquals(0.0d, firstStep.getAnimusSpent(), 0.0000001d);
        assertEquals(1.0d, firstStep.getAnimusGenerated(), 0.0000001d);
        assertEquals(8.0d, firstStep.getAnimusAfter(), 0.0000001d);
        assertEquals(2113L, firstStep.getDirectDamage());
        assertFalse(firstStep.isMolochBuffActivated());

        SimulationStepTrace secondStep = calculation.getResult().getStepTrace().get(1);
        assertEquals(8.0d, secondStep.getAnimusBefore(), 0.0000001d);
        assertEquals(8.0d, secondStep.getAnimusSpent(), 0.0000001d);
        assertEquals(2.0d, secondStep.getAnimusGenerated(), 0.0000001d);
        assertEquals(3.0d, secondStep.getAnimusAfter(), 0.0000001d);
        assertEquals(3380L, secondStep.getDirectDamage());
        assertTrue(secondStep.isMolochBuffActivated());
    }

    @Test
    void moloch_z_osem_animuszu_aktywuje_buff_na_piec_castow_i_nie_zmienia_wiary() {
        CurrentBuildRequest request = clashVerathielRequest(PaladinOathId.JUGGERNAUT.name(), 8.0d);

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        DamageBreakdown buffedBreakdown = calculation.getResult().getDirectHitDebugSnapshots().getFirst().getBreakdown();
        assertEquals(1.60d, buffedBreakdown.getMolochOathMultiplier(), 0.0000001d);
        assertEquals(16901L, buffedBreakdown.getRawDamage());
        assertEquals(3380L, buffedBreakdown.getFinalDamage());
        assertEquals(5224L, buffedBreakdown.getCriticalDamage());
        assertEquals(27465L, calculation.getResult().getTotalDamage());
        assertEquals(2746.5d, calculation.getResult().getDps(), 0.0000001d);
        assertEquals(65.0d, calculation.getResult().getFinalPrimaryResource(), 0.0000001d);
        assertEquals(250.0d, calculation.getResult().getTotalPrimaryResourceCost(), 0.0000001d);
        assertEquals(200.0d, calculation.getResult().getTotalPrimaryResourceGenerated(), 0.0000001d);
        assertEquals(15.0d, calculation.getResult().getTotalPrimaryResourceRegenerated(), 0.0000001d);
        assertEquals(8.0d, calculation.getResult().getInitialAnimus(), 0.0000001d);
        assertEquals(1.0d, calculation.getResult().getFinalAnimus(), 0.0000001d);
        assertEquals(1, calculation.getResult().getMolochBuffActivationCount());

        SimulationStepTrace firstStep = calculation.getResult().getStepTrace().getFirst();
        assertEquals(8.0d, firstStep.getAnimusBefore(), 0.0000001d);
        assertEquals(8.0d, firstStep.getAnimusSpent(), 0.0000001d);
        assertEquals(1.0d, firstStep.getAnimusAfter(), 0.0000001d);
        assertTrue(firstStep.isMolochBuffActivated());
        assertTrue(firstStep.isMolochBuffActive());
        assertEquals(5, firstStep.getMolochBuffRemainingSeconds());
        assertEquals(3380L, firstStep.getDirectDamage());
        assertEquals(3380L, calculation.getResult().getStepTrace().get(4).getDirectDamage());
        assertEquals(2113L, calculation.getResult().getStepTrace().get(5).getDirectDamage());
    }

    @Test
    void animusz_starcia_initial_1_daje_zloty_wynik_10s_i_nie_zmienia_wiary() {
        CurrentBuildRequest request = clashVerathielRequest(PaladinOathId.JUGGERNAUT.name(), 1.0d, true, 10);

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        assertEquals(28732L, calculation.getResult().getTotalDamage());
        assertEquals(2873.2d, calculation.getResult().getDps(), 0.0000001d);
        assertEquals(65.0d, calculation.getResult().getFinalPrimaryResource(), 0.0000001d);
        assertEquals(5.0d, calculation.getResult().getFinalAnimus(), 0.0000001d);
        assertEquals(2, calculation.getResult().getMolochBuffActivationCount());
        assertEquals(18.0d, calculation.getResult().getTotalClashAnimusGenerated(), 0.0000001d);
        assertEquals(6, calculation.getResult().getStepTrace().stream()
                .filter(step -> step.getDirectDamage() == 3380L)
                .count());
        assertEquals(2113L, calculation.getResult().getStepTrace().get(3).getDirectDamage());
        assertEquals(3380L, calculation.getResult().getStepTrace().get(4).getDirectDamage());
        assertTrue(calculation.getResult().getStepTrace().get(8).isMolochBuffActivated());
    }

    @Test
    void animusz_starcia_initial_8_daje_zloty_wynik_10s() {
        CurrentBuildRequest request = clashVerathielRequest(PaladinOathId.JUGGERNAUT.name(), 8.0d, true, 10);

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        assertEquals(33800L, calculation.getResult().getTotalDamage());
        assertEquals(3380.0d, calculation.getResult().getDps(), 0.0000001d);
        assertEquals(65.0d, calculation.getResult().getFinalPrimaryResource(), 0.0000001d);
        assertEquals(5.0d, calculation.getResult().getFinalAnimus(), 0.0000001d);
        assertEquals(3, calculation.getResult().getMolochBuffActivationCount());
        assertEquals(18.0d, calculation.getResult().getTotalClashAnimusGenerated(), 0.0000001d);
        assertEquals(10, calculation.getResult().getStepTrace().stream()
                .filter(step -> step.getDirectDamage() == 3380L)
                .count());
    }

    @Test
    void animusz_starcia_bez_molocha_nie_zmienia_damage_i_nie_pokazuje_buffa() {
        CurrentBuildRequest request = clashVerathielRequest("NONE", 1.0d, true, 10);

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        assertEquals(21130L, calculation.getResult().getTotalDamage());
        assertEquals(2113.0d, calculation.getResult().getDps(), 0.0000001d);
        assertEquals(0, calculation.getResult().getMolochBuffActivationCount());
        assertEquals(8.0d, calculation.getResult().getFinalAnimus(), 0.0000001d);
        assertEquals(7.0d, calculation.getResult().getTotalClashAnimusGenerated(), 0.0000001d);
        assertEquals(1.0d, calculation.getResult().getDirectHitDebugSnapshots().getFirst()
                .getBreakdown().getMolochOathMultiplier(), 0.0000001d);
    }

    @Test
    void opisowe_grupy_starcia_nie_zmieniaja_damage() {
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
                Map.of(SkillId.CLASH, new SkillState(
                        SkillId.CLASH,
                        1,
                        false,
                        SkillUpgradeChoice.NONE,
                        SkillRuntimeModifierChoice.NONE,
                        SkillState.NO_TREE_CHOICE,
                        "zwiekszenie_obrazen",
                        "brac_ich"
                )),
                List.of(SkillId.CLASH),
                10,
                100.0d,
                100.0d,
                1.50d,
                PaladinOathId.JUGGERNAUT.name(),
                1.0d,
                8.0d,
                List.of("verathiel_shard")
        );

        CurrentBuildCalculation calculation = calculationService.calculate(request);

        assertEquals(21130L, calculation.getResult().getTotalDamage());
        assertEquals(2113.0d, calculation.getResult().getDps(), 0.0000001d);
        assertEquals(1.0d, calculation.getResult().getFinalAnimus(), 0.0000001d);
        assertEquals(0, calculation.getResult().getMolochBuffActivationCount());
        assertEquals(1.0d, calculation.getResult().getDirectHitDebugSnapshots().getFirst()
                .getBreakdown().getMolochOathMultiplier(), 0.0000001d);
    }

    @Test
    void inne_przysiegi_nie_zmieniaja_sanity_damage() {
        for (String oathId : List.of(PaladinOathId.ADEPT.name(), PaladinOathId.JUDGE.name(), PaladinOathId.ZEALOT.name())) {
            CurrentBuildCalculation calculation = calculationService.calculate(clashVerathielRequest(oathId, 8.0d));

            assertEquals(21130L, calculation.getResult().getTotalDamage());
            assertEquals(2113.0d, calculation.getResult().getDps(), 0.0000001d);
            assertEquals(65.0d, calculation.getResult().getFinalPrimaryResource(), 0.0000001d);
            assertFalse(calculation.getResult().hasAnimusRuntimeData());
            assertEquals(1.0d, calculation.getResult().getDirectHitDebugSnapshots().getFirst()
                    .getBreakdown().getMolochOathMultiplier(), 0.0000001d);
        }
    }

    private static CurrentBuildRequest clashVerathielRequest(String selectedOathId, double initialAnimus) {
        return clashVerathielRequest(selectedOathId, initialAnimus, false, 10);
    }

    private static CurrentBuildRequest clashVerathielRequest(String selectedOathId,
                                                             double initialAnimus,
                                                             boolean clashAnimus,
                                                             int horizonSeconds) {
        return new CurrentBuildRequest(
                70,
                1664,
                304.0d,
                76.0d,
                0.0d,
                20.0d,
                0.0d,
                true,
                true,
                Map.of(SkillId.CLASH, new SkillState(
                        SkillId.CLASH,
                        1,
                        false,
                        SkillUpgradeChoice.NONE,
                        clashAnimus ? SkillRuntimeModifierChoice.ANIMUS : SkillRuntimeModifierChoice.NONE
                )),
                List.of(SkillId.CLASH),
                horizonSeconds,
                100.0d,
                100.0d,
                1.50d,
                selectedOathId,
                initialAnimus,
                8.0d,
                List.of("verathiel_shard")
        );
    }
}
