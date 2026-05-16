package krys.hero;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** Rejestr zweryfikowanych baseline'ów prezentacyjnych z gry; nie jest wejściem runtime DPS. */
public final class HeroClassStatBaselines {
    private static final Map<String, HeroClassStatBaseline> BASELINES = Map.of(
            key(HeroClass.PALADIN, 70),
            paladinLevel70WithoutItems()
    );

    private HeroClassStatBaselines() {
    }

    private static HeroClassStatBaseline paladinLevel70WithoutItems() {
        int strength = 79;
        int intelligence = 76;
        return new HeroClassStatBaseline(
                HeroClass.PALADIN,
                70,
                strength,
                intelligence,
                76,
                77,
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
                        new BigDecimal("0.0")
                ),
                new BigDecimal("50.0"),
                new BigDecimal("20.0"),
                0
        );
    }

    public static Optional<HeroClassStatBaseline> find(HeroClass heroClass, int level) {
        return Optional.ofNullable(BASELINES.get(key(heroClass, level)));
    }

    private static String key(HeroClass heroClass, int level) {
        return heroClass.name() + ":" + level;
    }
}
