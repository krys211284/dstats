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

        HeroArmorBreakdown armor = baseline.getArmorBreakdown();
        assertEquals(158, armor.getArmorFromStrength());
        assertEquals(0, armor.getArmorFromItems());
        assertEquals(0, armor.getArmorFromOtherSources());
        assertEquals(158, armor.getTotalArmor());
        assertEquals(158, baseline.getArmor());

        HeroCriticalChanceBreakdown criticalChance = baseline.getCriticalChanceBreakdown();
        assertEquals(0, new BigDecimal("5.0").compareTo(criticalChance.getBaseCriticalChancePercent()));
        assertEquals(0, new BigDecimal("0.2").compareTo(criticalChance.getCriticalChanceFromIntelligencePercent()));
        assertEquals(0, new BigDecimal("0.0").compareTo(criticalChance.getCriticalChanceFromItemsPercent()));
        assertEquals(0, new BigDecimal("0.0").compareTo(criticalChance.getCriticalChanceFromOtherSourcesPercent()));
        assertEquals(0, new BigDecimal("5.2").compareTo(criticalChance.getTotalCriticalChancePercent()));
        assertEquals(0, new BigDecimal("5.2").compareTo(baseline.getCriticalChancePercent()));
    }

    @Test
    void nie_ma_baselineu_dla_niezweryfikowanego_poziomu() {
        assertTrue(HeroClassStatBaselines.find(HeroClass.PALADIN, 69).isEmpty());
    }
}
