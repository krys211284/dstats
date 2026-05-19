package krys.combat;

import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import krys.skill.StatusId;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageEngineFormulaRegressionTest {
    private final DamageEngine damageEngine = new DamageEngine();

    @Test
    void powinien_pilnowac_wzoru_main_stat_i_kryta() {
        DamageBreakdown breakdown = damageEngine.calculate(
                DamageEngineGoldenValuesTest.referenceSnapshot(new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)),
                SkillId.BRANDISH,
                EnumSet.noneOf(StatusId.class)
        );

        assertEquals(40.0d, breakdown.getMainStat(), 0.0000001d);
        assertEquals(1.05d, breakdown.getMainStatMultiplier(), 0.0000001d);
        assertEquals(19.0d, breakdown.getIntelligence(), 0.0000001d);
        assertEquals(0.015d, breakdown.getCritDamageBonusFromItems(), 0.0000001d);
        assertEquals(0.0076d, breakdown.getCritDamageBonusFromIntelligence(), 0.0000001d);
        assertEquals(0.5226d, breakdown.getCritDamageBonusTotal(), 0.0000001d);
        assertEquals(1.5226d, breakdown.getCritMultiplier(), 0.0000001d);
    }

    @Test
    void powinien_liczyc_main_stat_paladyna_przez_dzielnik_800() {
        DamageBreakdown breakdown = damageEngine.calculate(
                DamageEngineGoldenValuesTest.level70ClashSnapshot(304.0d, List.of()),
                SkillId.CLASH,
                EnumSet.noneOf(StatusId.class)
        );

        assertEquals(304.0d, breakdown.getMainStat(), 0.0000001d);
        assertEquals(1.38d, breakdown.getMainStatMultiplier(), 0.0000001d);
    }

    @Test
    void powinien_dla_poziomu_70_zostawiac_20_procent_obrazen_po_redukcji() {
        DamageBreakdown breakdown = damageEngine.calculate(
                DamageEngineGoldenValuesTest.level70ClashSnapshot(304.0d, List.of()),
                SkillId.CLASH,
                EnumSet.noneOf(StatusId.class)
        );

        assertEquals(0.80d, breakdown.getLevelDamageReduction(), 0.0000001d);
        assertEquals(0.20d, breakdown.getDamageTakenAfterLevelReductionMultiplier(), 0.0000001d);
        assertEquals(Math.round(breakdown.getRawDamage() * 0.20d), breakdown.getFinalDamage());
    }

    @Test
    void powinien_wlaczac_obrazeniowy_efekt_verathiela_tylko_gdy_aspekt_jest_aktywny() {
        DamageBreakdown withoutVerathiel = damageEngine.calculate(
                DamageEngineGoldenValuesTest.level70ClashSnapshot(304.0d, List.of()),
                SkillId.CLASH,
                EnumSet.noneOf(StatusId.class)
        );
        DamageBreakdown withVerathiel = damageEngine.calculate(
                DamageEngineGoldenValuesTest.level70ClashSnapshot(304.0d, List.of("verathiel_shard")),
                SkillId.CLASH,
                EnumSet.noneOf(StatusId.class)
        );

        assertEquals(1.0d, withoutVerathiel.getVerathielBasicSkillMultiplier(), 0.0000001d);
        assertEquals(2.0d, withVerathiel.getVerathielBasicSkillMultiplier(), 0.0000001d);
        assertEquals(10563L, withVerathiel.getRawDamage());
        assertEquals(2113L, withVerathiel.getFinalDamage());
    }

    @Test
    void powinien_pilnowac_redukcji_poziomu_i_mnoznika_broni() {
        DamageBreakdown breakdown = damageEngine.calculate(
                DamageEngineGoldenValuesTest.referenceSnapshot(new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)),
                SkillId.BRANDISH,
                EnumSet.noneOf(StatusId.class)
        );

        assertEquals(2.0d, breakdown.getWeaponMultiplier(), 0.0000001d);
        assertEquals(0.38d, breakdown.getLevelDamageReduction(), 0.0000001d);
    }

    @Test
    void powinien_stosowac_replace_base_damage() {
        DamageBreakdown breakdown = damageEngine.calculate(
                DamageEngineGoldenValuesTest.referenceSnapshot(new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.LEFT)),
                SkillId.BRANDISH,
                EnumSet.noneOf(StatusId.class)
        );

        assertEquals(6L, breakdown.getBaseDamage());
        assertEquals(73L, breakdown.getComponents().get(0).getSkillDamagePercent());
    }

    @Test
    void powinien_pominac_boczne_hity_w_single_target_i_zachowac_regule_52() {
        DamageBreakdown breakdown = damageEngine.calculate(
                DamageEngineGoldenValuesTest.referenceSnapshot(new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.RIGHT)),
                SkillId.BRANDISH,
                EnumSet.of(StatusId.VULNERABLE)
        );

        assertEquals(168L, breakdown.getComponents().get(0).getSkillDamagePercent());
        assertEquals(168L, breakdown.getComponents().get(1).getSkillDamagePercent());
        assertEquals(2, breakdown.getComponents().get(1).getHitCount());
        assertEquals(34L, breakdown.getRawDamage());
        assertEquals(52L, breakdown.getRawCriticalDamage());
        assertEquals(Math.round(breakdown.getRawDamage() * breakdown.getCritMultiplier()), breakdown.getRawCriticalDamage());
    }
}
