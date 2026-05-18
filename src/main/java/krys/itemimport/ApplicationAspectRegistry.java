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
            ),
            new AspectDefinition(
                    "fortify_damage_increased",
                    "Umocnienie: zwiększone obrażenia",
                    "Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.",
                    AspectType.LEGENDARY,
                    AspectRuntimeStatus.DESCRIPTIVE_ONLY,
                    EnumSet.of(EquipmentSlot.OFF_HAND),
                    EnumSet.of(HeroClass.PALADIN),
                    List.of("legendary", "fortify", "damage", "descriptive-only")
            ),
            new AspectDefinition(
                    "verathiel_shard",
                    "Odłamek Verathiela",
                    "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100], ale dodatkowo zużywają 25 pkt. podstawowego zasobu.",
                    AspectType.UNIQUE,
                    AspectRuntimeStatus.DESCRIPTIVE_ONLY,
                    EnumSet.of(EquipmentSlot.MAIN_HAND),
                    EnumSet.noneOf(HeroClass.class),
                    List.of("unique", "sword", "basic-skills", "resource-cost")
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
