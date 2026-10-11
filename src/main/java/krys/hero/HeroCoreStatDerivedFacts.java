package krys.hero;

import java.math.BigDecimal;

/** Dokładne fakty tooltipów core stats; procenty są punktami procentowymi, bez zaokrąglenia UI. */
public record HeroCoreStatDerivedFacts(BigDecimal armorFromStrength, BigDecimal skillDamagePercent,
                                       BigDecimal allResistance, BigDecimal resourceGenerationPercent,
                                       BigDecimal healingReceivedPercent, BigDecimal criticalChancePercent,
                                       BigDecimal dodgeChancePercent) {
    /** Scalar obrażeń zależy od klasy/sezonu i jest jawnie podawany przez prezentację. */
    public static HeroCoreStatDerivedFacts from(HeroCoreStats stats, BigDecimal skillDamagePercentPerIntelligence) {
        return new HeroCoreStatDerivedFacts(BigDecimal.valueOf(stats.strength()).multiply(new BigDecimal("2")),
                BigDecimal.valueOf(stats.intelligence()).multiply(skillDamagePercentPerIntelligence),
                BigDecimal.valueOf(stats.intelligence()).multiply(new BigDecimal("0.4")),
                BigDecimal.valueOf(stats.willpower()).multiply(new BigDecimal("0.005")),
                BigDecimal.valueOf(stats.willpower()).multiply(new BigDecimal("0.035")),
                BigDecimal.valueOf(stats.dexterity()).multiply(new BigDecimal("0.0025")),
                BigDecimal.valueOf(stats.dexterity()).multiply(new BigDecimal("0.006")));
    }
}
