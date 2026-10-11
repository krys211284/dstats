package krys.hero;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HeroCoreStatDerivedFactsTest {
    @Test
    void nakedNecromancerFactsShouldMatchTheExactValuesAndGameUiRounding() {
        var facts = HeroCoreStatDerivedFacts.from(HeroClassCoreStatProgressions.get(HeroClass.NECROMANCER).atLevel(70), new BigDecimal("0.1625"));
        exact("152", facts.armorFromStrength());
        exact("12.8375", facts.skillDamagePercent());
        exact("31.6", facts.allResistance());
        exact("0.385", facts.resourceGenerationPercent());
        exact("2.695", facts.healingReceivedPercent());
        exact("0.19", facts.criticalChancePercent());
        exact("0.456", facts.dodgeChancePercent());
        assertEquals("12.8", facts.skillDamagePercent().setScale(1, RoundingMode.HALF_UP).toPlainString());
        assertEquals("32", facts.allResistance().setScale(0, RoundingMode.HALF_UP).toPlainString());
        assertEquals("0.4", facts.resourceGenerationPercent().setScale(1, RoundingMode.HALF_UP).toPlainString());
        assertEquals("2.7", facts.healingReceivedPercent().setScale(1, RoundingMode.HALF_UP).toPlainString());
        assertEquals("0.2", facts.criticalChancePercent().setScale(1, RoundingMode.HALF_UP).toPlainString());
        assertEquals("0.5", facts.dodgeChancePercent().setScale(1, RoundingMode.HALF_UP).toPlainString());
    }

    private static void exact(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
}
