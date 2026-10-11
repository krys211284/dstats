package krys.hero;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Chroni zweryfikowany baseline prezentacyjny Paladyna bez itemów. */
class HeroClassStatBaselinesTest {
    @Test
    void paladyn_poziom_70_bez_itemow_ma_rozbite_pancerz_i_szanse_krytyczna() {
        HeroClassStatBaseline baseline = HeroClassStatBaselines.find(HeroClass.PALADIN, 70).orElseThrow();

        assertEquals(79, baseline.getStrength());
        assertEquals(76, baseline.getIntelligence());
        assertEquals(76, baseline.getWillpower());
        assertEquals(77, baseline.getDexterity());
        assertEquals(1610, baseline.getToughness());
        assertEquals(1526, baseline.getMaxHealth());

        HeroArmorBreakdown armor = baseline.getArmorBreakdown();
        assertEquals(158, armor.getArmorFromStrength());
        assertEquals(0, armor.getArmorFromItems());
        assertEquals(0, armor.getArmorFromOtherSources());
        assertEquals(158, armor.getTotalArmor());
        assertEquals(158, baseline.getArmor());

        HeroCriticalChanceBreakdown criticalChance = baseline.getCriticalChanceBreakdown();
        assertEquals(0, new BigDecimal("5.0").compareTo(criticalChance.getBaseCriticalChancePercent()));
        assertEquals(0, new BigDecimal("0.2").compareTo(criticalChance.getCriticalChanceFromCoreStatPercent()));
        assertEquals(0, new BigDecimal("0.0").compareTo(criticalChance.getCriticalChanceFromItemsPercent()));
        assertEquals(0, new BigDecimal("0.0").compareTo(criticalChance.getCriticalChanceFromOtherSourcesPercent()));
        assertEquals(0, new BigDecimal("5.2").compareTo(criticalChance.getTotalCriticalChancePercent()));
        assertEquals(0, new BigDecimal("5.2").compareTo(baseline.getCriticalChancePercent()));
    }

    @Test
    void nie_ma_baselineu_dla_niezweryfikowanego_poziomu() {
        assertTrue(HeroClassStatBaselines.find(HeroClass.PALADIN, 69).isEmpty());
        for (int level : new int[]{1, 2, 69}) {
            assertTrue(HeroClassStatBaselines.find(HeroClass.NECROMANCER, level).isEmpty());
        }
    }

    @Test
    void nagi_nekromanta_poziom_70_ma_pelny_snapshot_zweryfikowany_w_grze() {
        var baseline = HeroClassStatBaselines.find(HeroClass.NECROMANCER, 70).orElseThrow();
        assertEquals(76, baseline.getStrength());
        assertEquals(79, baseline.getIntelligence());
        assertEquals(77, baseline.getWillpower());
        assertEquals(76, baseline.getDexterity());
        assertEquals(1611, baseline.getToughness());
        assertEquals(152, baseline.getArmor());
        assertEquals(152, baseline.getArmorBreakdown().getArmorFromStrength());
        assertEquals(0, baseline.getArmorBreakdown().getArmorFromItems());
        assertEquals(0, baseline.getArmorBreakdown().getArmorFromOtherSources());
        assertEquals(1526, baseline.getMaxHealth());
        assertEquals(32, baseline.getPhysicalResistance());
        assertEquals(32, baseline.getFireResistance());
        assertEquals(32, baseline.getLightningResistance());
        assertEquals(32, baseline.getColdResistance());
        assertEquals(32, baseline.getPoisonResistance());
        assertEquals(32, baseline.getShadowResistance());
        assertEquals(0, baseline.getWeaponDamage());
        assertEquals(new BigDecimal("1.00"), baseline.getWeaponSpeed());
        assertEquals(0, new BigDecimal("5.2").compareTo(baseline.getCriticalChancePercent()));
        assertEquals("Zręczności", baseline.getCriticalChanceBreakdown().getCoreStatSourceLabel());
        assertEquals(0, new BigDecimal("0.2").compareTo(baseline.getCriticalChanceBreakdown().getCriticalChanceFromCoreStatPercent()));
        assertEquals(0, new BigDecimal("50.0").compareTo(baseline.getCriticalDamagePercent()));
        assertEquals(0, new BigDecimal("20.0").compareTo(baseline.getVulnerableDamagePercent()));
        assertEquals(0, baseline.getThorns());
    }
}
