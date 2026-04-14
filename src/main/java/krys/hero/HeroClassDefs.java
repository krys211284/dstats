package krys.hero;

import krys.item.ItemStatType;

import java.util.Map;

/** Rejestr minimalnych definicji klas potrzebnych do pierwszego etapu projektu. */
public final class HeroClassDefs {
    private static final Map<HeroClass, HeroClassDef> DEFINITIONS = Map.of(
            HeroClass.PALADIN,
            new HeroClassDef(HeroClass.PALADIN, ItemStatType.STRENGTH, 10, 1, 1000.0d, 7, 1)
    );

    private HeroClassDefs() {
    }

    public static HeroClassDef get(HeroClass heroClass) {
        HeroClassDef definition = DEFINITIONS.get(heroClass);
        if (definition == null) {
            throw new IllegalArgumentException("Brak definicji klasy bohatera: " + heroClass);
        }
        return definition;
    }
}
