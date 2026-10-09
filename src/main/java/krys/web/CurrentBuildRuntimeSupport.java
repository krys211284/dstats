package krys.web;

import krys.hero.HeroClass;

/** Paladin-only guard legacy UI i Search; tryby Current Build wyznacza CurrentBuildRuntimeMode. */
public final class CurrentBuildRuntimeSupport {
    public static final String NECROMANCER_MESSAGE = "Current Build Nekromanty obsługuje bazowy direct hit Krwotoku R1 w N11.";
    public static final String NECROMANCER_SEARCH_MESSAGE = "Search dla Nekromanty nie jest jeszcze obsługiwany.";

    private CurrentBuildRuntimeSupport() { }

    public static boolean isSupported(HeroClass heroClass) {
        return heroClass == HeroClass.PALADIN;
    }

    static void requireLegacyPaladin(HeroProfile hero) {
        if (!isSupported(hero.getHeroClass())) {
            throw new UnsupportedOperationException("Edycja buildu przez legacy Paladin UI jest dostępna wyłącznie dla Paladyna. " + NECROMANCER_MESSAGE);
        }
    }
}
