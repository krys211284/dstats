package krys.itemimport;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;

import java.util.EnumSet;
import java.util.List;

/** Wspólny punkt dostępu do zalążkowego katalogu aspektów aplikacji. */
public final class ApplicationAspectRegistry {
    private static final List<AspectDefinition> SEED_DEFINITIONS = List.of(
            new AspectDefinition(
                    "inner-calm",
                    "Aspekt Wewnętrznego Spokoju",
                    "Zwiększa zadawane obrażenia podczas stania w bezruchu. Premia jest trzykrotnie większa, jeśli postać stoi w bezruchu przez co najmniej 3 sekundy.",
                    EnumSet.of(EquipmentSlot.OFF_HAND),
                    EnumSet.of(HeroClass.PALADIN),
                    List.of("legendary", "damage")
            )
    );

    private static final AspectRegistry INSTANCE = new AspectRegistry(SEED_DEFINITIONS);

    private ApplicationAspectRegistry() {
    }

    public static AspectRegistry get() {
        return INSTANCE;
    }

    static List<AspectDefinition> seedDefinitions() {
        return SEED_DEFINITIONS;
    }
}
