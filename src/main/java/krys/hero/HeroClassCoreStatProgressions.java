package krys.hero;

import java.util.Map;

/** Zweryfikowane startowe core stats i liniowy przyrost obu klas; bez legacy runtime. */
public final class HeroClassCoreStatProgressions {
    private static final Map<HeroClass, HeroClassCoreStatProgression> PROGRESSIONS = Map.of(
            HeroClass.NECROMANCER, new HeroClassCoreStatProgression(new HeroCoreStats(7, 10, 8, 7), 1),
            HeroClass.PALADIN, new HeroClassCoreStatProgression(new HeroCoreStats(10, 7, 7, 8), 1));

    private HeroClassCoreStatProgressions() { }

    public static HeroClassCoreStatProgression get(HeroClass heroClass) {
        var progression = PROGRESSIONS.get(heroClass);
        if (progression == null) { throw new IllegalArgumentException("Brak progresji core stats dla klasy: " + heroClass); }
        return progression;
    }
}
