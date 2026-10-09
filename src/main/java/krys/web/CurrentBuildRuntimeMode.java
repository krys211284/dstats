package krys.web;

import krys.hero.HeroClass;

/** Jawna granica Current Build, niezależna od Paladin-only guardów Search i legacy UI. */
public enum CurrentBuildRuntimeMode {
    NONE, LEGACY_PALADIN, NECROMANCER_DIRECT_HIT;

    public static CurrentBuildRuntimeMode forHero(HeroProfile hero) {
        if (hero == null) { return NONE; }
        return switch (hero.getHeroClass()) {
            case PALADIN -> LEGACY_PALADIN;
            case NECROMANCER -> NECROMANCER_DIRECT_HIT;
        };
    }
}
