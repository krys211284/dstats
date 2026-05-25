package krys.itemimport;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;

import java.util.EnumSet;
import java.util.List;

/** Wspólny punkt dostępu do zalążkowego katalogu aspektów aplikacji. */
public final class ApplicationAspectRegistry {
    private static final EnumSet<EquipmentSlot> OFFENSIVE_SLOTS = EnumSet.of(
            EquipmentSlot.AMULET,
            EquipmentSlot.MAIN_HAND,
            EquipmentSlot.OFF_HAND,
            EquipmentSlot.GLOVES,
            EquipmentSlot.RING
    );
    private static final EnumSet<HeroClass> PALADIN_ONLY = EnumSet.of(HeroClass.PALADIN);
    private static final EnumSet<HeroClass> GENERIC = EnumSet.noneOf(HeroClass.class);

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
                    "Gdy masz umocnienie, zadajesz obrażenia zwiększone o X%[x] [45 - 65]%.",
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
            ),
            offensivePaladin(
                    "immortal_glory_aspect",
                    "Aspekt Nieśmiertelnej Chwały",
                    "Gdy jesteś w pełni sił, twoje umiejętności zadają obrażenia zwiększone o 35%[x] [20 - 35]%. Premia jest podwojona w walce z osłabionymi wrogami.",
                    21,
                    AspectRollDefinition.roll("damage_multiplier", "Zwiększone obrażenia", 35.0d, 20.0d, 35.0d, "%[x]"),
                    null,
                    null,
                    List.of("paladin", "offense", "full-life", "vulnerable", "descriptive-only")
            ),
            offensivePaladin(
                    "watkins_law_aspect",
                    "Aspekt Prawa Watkinsa",
                    "Twoje umiejętności Sędziego zadają obrażenia zwiększone o 65,0%[x] [45,0 - 65,0]% wrogom pod wpływem Osądu.",
                    21,
                    AspectRollDefinition.roll("damage_multiplier", "Zwiększone obrażenia Sędziego", 65.0d, 45.0d, 65.0d, "%[x]"),
                    null,
                    null,
                    List.of("paladin", "offense", "judge", "judgement", "descriptive-only")
            ),
            offensivePaladin(
                    "proselytism_aspect",
                    "Aspekt Prozelityzmu",
                    "Istnieje 40% szans na to, że umiejętności Sędziego przeskoczą do pobliskiego wroga, zadając 150,0%[x] [50,0 - 150]% swoich obrażeń. Jeżeli w zasięgu nie ma innych wrogów, dodatkowe obrażenia otrzymuje cel początkowy.",
                    21,
                    List.of(
                            AspectRollDefinition.fixed("jump_chance", "Szansa przeskoku", 40.0d, "%"),
                            AspectRollDefinition.roll("damage_multiplier", "Obrażenia przeskoku", 150.0d, 50.0d, 150.0d, "%[x]")
                    ),
                    null,
                    null,
                    List.of("paladin", "offense", "judge", "single-target-note", "descriptive-only")
            ),
            offensivePaladin(
                    "redirected_power_aspect",
                    "Aspekt Przekierowanej Mocy",
                    "Zyskujesz premię do obrażeń od trafień krytycznych równą 60%[x] [40 - 60]% szansy na blok. Blokowanie podwaja tę premię na 10 sek.",
                    21,
                    AspectRollDefinition.roll("critical_damage_from_block", "Obrażenia krytyczne z szansy na blok", 60.0d, 40.0d, 60.0d, "%[x]"),
                    10,
                    null,
                    List.of("paladin", "offense", "block", "critical-damage", "descriptive-only")
            ),
            offensivePaladin(
                    "sanctified_punishment_aspect",
                    "Aspekt Uświęconej Kary",
                    "Obrażenia Świętości i Ognia są zwiększone o 60,0%[x] [40,0 - 60,0]%.",
                    21,
                    AspectRollDefinition.roll("holy_fire_damage_multiplier", "Obrażenia Świętości i Ognia", 60.0d, 40.0d, 60.0d, "%[x]"),
                    null,
                    null,
                    List.of("paladin", "offense", "holy", "fire", "descriptive-only")
            ),
            offensive(
                    "crushing_aspect",
                    "Miażdżący aspekt",
                    "Gdy masz umocnienie, zadajesz obrażenia zwiększone o 65%[x] [45 - 65]%.",
                    21,
                    AspectRollDefinition.roll("fortify_damage_multiplier", "Zwiększone obrażenia przy umocnieniu", 65.0d, 45.0d, 65.0d, "%[x]"),
                    null,
                    null,
                    List.of("generic", "offense", "fortify", "damage", "same-effect-as-fortify_damage_increased", "descriptive-only")
            ),
            offensive(
                    "accelerating_aspect",
                    "Przyspieszający aspekt",
                    "Trafienia krytyczne umiejętnościami Głównymi zwiększają twoją szybkość ataku o 50,0%[+] [30,0 - 50,0]% na 5 sek.",
                    21,
                    AspectRollDefinition.roll("attack_speed_bonus", "Szybkość ataku", 50.0d, 30.0d, 50.0d, "%[+]"),
                    5,
                    null,
                    List.of("generic", "offense", "critical-hit", "core-skills", "attack-speed", "descriptive-only")
            ),
            offensivePaladin(
                    "golden_hour_aspect",
                    "Aspekt Złotej Godziny",
                    "Detonacje Osądu mogą detonować inne Osądy przed czasem. Osąd zadaje obrażenia zwiększone o 100%[x] [60 - 100]%.",
                    21,
                    AspectRollDefinition.roll("judgement_damage_multiplier", "Obrażenia Osądu", 100.0d, 60.0d, 100.0d, "%[x]"),
                    null,
                    null,
                    List.of("paladin", "offense", "judgement", "detonation", "descriptive-only")
            ),
            offensivePaladin(
                    "elemental_fate_aspect",
                    "Aspekt Żywiołowego Losu",
                    "Obrażenia zadawane określonymi zbiorami typów obrażeń zwiększają się o 60%[x] [40 - 60]% na 7 sek. Ten efekt obowiązuje na zmianę w 2 zbiorach: Obrażenia Ognia, Błyskawic, Świętości i Fizyczne. Obrażenia Zimna, Trucizny i Cienia. Los może ci sprzyjać i wielokrotnie przyznać tę samą premię, która może kumulować się do 6 razy. Jeśli osiągniesz maksimum, masz szczęśliwy dzień!",
                    21,
                    AspectRollDefinition.roll("elemental_damage_multiplier", "Obrażenia wybranego zbioru typów", 60.0d, 40.0d, 60.0d, "%[x]"),
                    7,
                    6,
                    List.of("paladin", "offense", "elemental", "rotation", "stacks", "descriptive-only")
            ),
            offensive(
                    "bristling_aspect",
                    "Najeżony aspekt",
                    "Kiedy unikasz ataku lub blokujesz go, atakujący odnosi 300% [200 - 300]% obrażeń od cierni.",
                    21,
                    AspectRollDefinition.roll("thorns_retaliation", "Obrażenia od cierni", 300.0d, 200.0d, 300.0d, "%"),
                    null,
                    null,
                    List.of("generic", "offense", "thorns", "dodge", "block", "descriptive-only")
            ),
            offensive(
                    "relentless_aspect",
                    "Nieubłagany aspekt",
                    "Powalanie wrogów sprawia, że odnoszą oni od ciebie obrażenia zwiększone o 60%[x] [50 - 60]% przez 6 sek.",
                    11,
                    AspectRollDefinition.roll("knockdown_damage_taken", "Zwiększone obrażenia po powaleniu", 60.0d, 50.0d, 60.0d, "%[x]"),
                    6,
                    null,
                    List.of("generic", "offense", "knockdown", "descriptive-only")
            ),
            offensivePaladin(
                    "revelatory_aspect",
                    "Objawienny aspekt",
                    "Umiejętności Zeloty zadają obrażenia zwiększone o 32,5%[x] [12,5 - 32,5]%. Premia jest podwojona, kiedy posiadasz co najmniej 3 poziomy kumulacji Ferworu.",
                    21,
                    AspectRollDefinition.roll("zealot_damage_multiplier", "Obrażenia Zeloty", 32.5d, 12.5d, 32.5d, "%[x]"),
                    null,
                    3,
                    List.of("paladin", "offense", "zealot", "fervor", "descriptive-only")
            ),
            offensive(
                    "penitential_aspect",
                    "Penitencki aspekt",
                    "Zużywając zdrowie na użycie umiejętności, zyskujesz 15,0% [5,0 - 15,0]% do szans na trafienie krytyczne i 55%[x] [35 - 55]% do obrażeń od trafień krytycznych na 4 sek.",
                    21,
                    List.of(
                            AspectRollDefinition.roll("critical_chance", "Szansa na trafienie krytyczne", 15.0d, 5.0d, 15.0d, "%"),
                            AspectRollDefinition.roll("critical_damage", "Obrażenia od trafień krytycznych", 55.0d, 35.0d, 55.0d, "%[x]")
                    ),
                    4,
                    null,
                    List.of("generic", "offense", "health-cost", "critical-chance", "critical-damage", "descriptive-only")
            ),
            offensive(
                    "smiting_aspect",
                    "Pogromowy aspekt",
                    "Zadajesz obrażenia zwiększone o 90%[x] [60 - 90]% obezwładnionym wrogom.",
                    31,
                    AspectRollDefinition.roll("overpower_damage_multiplier", "Obrażenia obezwładnionym wrogom", 90.0d, 60.0d, 90.0d, "%[x]"),
                    null,
                    null,
                    List.of("generic", "offense", "overpower", "descriptive-only")
            ),
            offensive(
                    "conceited_aspect",
                    "Pyszałkowaty aspekt",
                    "Kiedy masz aktywną barierę, zadajesz obrażenia zwiększone o 60%[x] [40 - 60]%.",
                    21,
                    AspectRollDefinition.roll("barrier_damage_multiplier", "Zwiększone obrażenia przy barierze", 60.0d, 40.0d, 60.0d, "%[x]"),
                    null,
                    null,
                    List.of("generic", "offense", "barrier", "descriptive-only")
            ),
            offensive(
                    "naznaczenie_aspect",
                    "Naznaczenie",
                    "Zadanie wrogowi obrażeń umiejętnością Podstawową zwiększa twoją szybkość ataku o 4% na 10 sek. Efekt kumuluje się maksymalnie 5 razy. Przy maksymalnej kumulacji wchodzisz w stan Wampirycznego Szału Krwi, który zapewnia zwiększenie obrażeń od umiejętności Podstawowych o 60%[x] oraz zwiększenie szybkości ruchu o 15% przez 10 sek.",
                    1,
                    List.of(
                            AspectRollDefinition.fixed("attack_speed_per_stack", "Szybkość ataku za kumulację", 4.0d, "%"),
                            AspectRollDefinition.fixed("basic_skill_damage_multiplier", "Obrażenia umiejętności Podstawowych", 60.0d, "%[x]"),
                            AspectRollDefinition.fixed("movement_speed_bonus", "Szybkość ruchu", 15.0d, "%")
                    ),
                    10,
                    5,
                    List.of("generic", "offense", "basic-skill", "attack-speed", "vampiric-blood-rage", "descriptive-only")
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

    private static AspectDefinition offensivePaladin(String id,
                                                     String displayName,
                                                     String effectDescription,
                                                     int rankMax,
                                                     AspectRollDefinition roll,
                                                     Integer durationSeconds,
                                                     Integer maxStacks,
                                                     List<String> tags) {
        return offensivePaladin(id, displayName, effectDescription, rankMax, List.of(roll), durationSeconds, maxStacks, tags);
    }

    private static AspectDefinition offensivePaladin(String id,
                                                     String displayName,
                                                     String effectDescription,
                                                     int rankMax,
                                                     List<AspectRollDefinition> rolls,
                                                     Integer durationSeconds,
                                                     Integer maxStacks,
                                                     List<String> tags) {
        return aspect(id, displayName, effectDescription, PALADIN_ONLY, rankMax, rolls, durationSeconds, maxStacks, tags);
    }

    private static AspectDefinition offensive(String id,
                                              String displayName,
                                              String effectDescription,
                                              int rankMax,
                                              AspectRollDefinition roll,
                                              Integer durationSeconds,
                                              Integer maxStacks,
                                              List<String> tags) {
        return aspect(id, displayName, effectDescription, GENERIC, rankMax, List.of(roll), durationSeconds, maxStacks, tags);
    }

    private static AspectDefinition offensive(String id,
                                              String displayName,
                                              String effectDescription,
                                              int rankMax,
                                              List<AspectRollDefinition> rolls,
                                              Integer durationSeconds,
                                              Integer maxStacks,
                                              List<String> tags) {
        return aspect(id, displayName, effectDescription, GENERIC, rankMax, rolls, durationSeconds, maxStacks, tags);
    }

    private static AspectDefinition aspect(String id,
                                           String displayName,
                                           String effectDescription,
                                           EnumSet<HeroClass> heroClasses,
                                           int rankMax,
                                           List<AspectRollDefinition> rolls,
                                           Integer durationSeconds,
                                           Integer maxStacks,
                                           List<String> tags) {
        return new AspectDefinition(
                id,
                displayName,
                effectDescription,
                AspectCategory.OFFENSE,
                AspectType.LEGENDARY,
                AspectRuntimeStatus.DESCRIPTIVE_ONLY,
                OFFENSIVE_SLOTS,
                heroClasses,
                tags,
                AspectDefinitionSource.VERIFIED_SCREENSHOT,
                rankMax,
                rankMax,
                rolls,
                durationSeconds,
                maxStacks
        );
    }
}
