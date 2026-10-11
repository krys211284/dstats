package krys.hero;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** Rejestr zweryfikowanych baseline'ów prezentacyjnych z gry; nie jest wejściem runtime DPS. */
public final class HeroClassStatBaselines {
    private static final Map<String, HeroClassStatBaseline> BASELINES = Map.of(
            key(HeroClass.PALADIN, 70),
            paladinLevel70WithoutItems(),
            key(HeroClass.NECROMANCER, 70), necromancerLevel70WithoutItems()
    );

    private HeroClassStatBaselines() {
    }

    private static HeroClassStatBaseline paladinLevel70WithoutItems() {
        var stats = HeroClassCoreStatProgressions.get(HeroClass.PALADIN).atLevel(70);
        int strength = stats.strength();
        int intelligence = stats.intelligence();
        return new HeroClassStatBaseline(
                HeroClass.PALADIN,
                70,
                strength,
                intelligence,
                stats.willpower(),
                stats.dexterity(),
                1610,
                new HeroArmorBreakdown(strength * 2, 0, 0),
                1526,
                30,
                30,
                30,
                30,
                30,
                30,
                0L,
                new BigDecimal("1.00"),
                new HeroCriticalChanceBreakdown(
                        new BigDecimal("5.0"),
                        new BigDecimal("0.2"),
                        new BigDecimal("0.0"),
                        new BigDecimal("0.0"),
                        "Inteligencji"
                ),
                new BigDecimal("50.0"),
                new BigDecimal("20.0"),
                0
        );
    }

    /** Pełny snapshot z gry potwierdzony tylko dla nagiego Nekromanty poziomu 70. */
    private static HeroClassStatBaseline necromancerLevel70WithoutItems() {
        var stats = HeroClassCoreStatProgressions.get(HeroClass.NECROMANCER).atLevel(70);
        return new HeroClassStatBaseline(HeroClass.NECROMANCER, 70,
                stats.strength(), stats.intelligence(), stats.willpower(), stats.dexterity(),
                1611, new HeroArmorBreakdown(152, 0, 0), 1526,
                32, 32, 32, 32, 32, 32, 0L, new BigDecimal("1.00"),
                new HeroCriticalChanceBreakdown(new BigDecimal("5.0"), new BigDecimal("0.2"),
                        BigDecimal.ZERO, BigDecimal.ZERO, "Zręczności"),
                new BigDecimal("50.0"), new BigDecimal("20.0"), 0);
    }

    public static Optional<HeroClassStatBaseline> find(HeroClass heroClass, int level) {
        return Optional.ofNullable(BASELINES.get(key(heroClass, level)));
    }

    private static String key(HeroClass heroClass, int level) {
        return heroClass.name() + ":" + level;
    }
}
