package krys.necromancer.bookofthedead;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static krys.necromancer.bookofthedead.BookOfDeadSourceFact.Unit.*;
import static krys.necromancer.bookofthedead.NecromancerMinionSpecializationId.*;
import static krys.necromancer.bookofthedead.NecromancerMinionTheme.*;

/** Kompletny katalog źródłowy Season 15 / 3.2.1, niezależny od drzewa skilli i runtime. */
public final class NecromancerBookOfDeadRegistry {
    public static final String SOURCE_REFERENCE =
            "https://www.icy-veins.com/d4/guides/necromancer-leveling-guide/#book-of-the-dead";
    public static final String FORTIFY_OFFICIAL_SOURCE =
            "https://news.blizzard.com/en-us/article/24244466/diablo-iv-patch-notes-2-5";
    public static final String SACRIFICE_OFFICIAL_SOURCE =
            "https://news.blizzard.com/en-us/article/24267729/prepare-for-the-reckoning-lord-of-hatred-draws-near";

    private static final NecromancerMinionRules RULES = new NecromancerMinionRules(
            number("20"), number("100"),
            "Sługi dziedziczą 100% statystyk i buffów gracza, które są dla nich użyteczne; to metadana źródłowa.",
            "Po Sacrifice sługi nadal mogą być przywoływane, jeśli odpowiedni skill przywołujący jest wykupiony "
                    + "i znajduje się na pasku umiejętności.", SOURCE_REFERENCE);
    private static final NecromancerBookOfDeadRegistry SOURCE_CATALOG =
            new NecromancerBookOfDeadRegistry(sourceDefinitions());

    private final List<NecromancerMinionSpecializationDefinition> all;
    private final Map<NecromancerMinionSpecializationId, NecromancerMinionSpecializationDefinition> byId;
    private final Map<String, NecromancerMinionSpecializationDefinition> byRawId;
    private final Map<NecromancerMinionFamily, List<NecromancerMinionSpecializationDefinition>> byFamily;

    /** Waliduje kompletną paczkę źródłową, w tym brak duplikatów i obecność wszystkich dziewięciu id. */
    public NecromancerBookOfDeadRegistry(List<NecromancerMinionSpecializationDefinition> definitions) {
        var ids = new EnumMap<NecromancerMinionSpecializationId, NecromancerMinionSpecializationDefinition>(
                NecromancerMinionSpecializationId.class);
        for (var definition : List.copyOf(definitions)) {
            if (ids.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalArgumentException("Powtórzone id specjalizacji: " + definition.id().rawId());
            }
        }
        if (ids.size() != NecromancerMinionSpecializationId.values().length) {
            throw new IllegalArgumentException("Katalog wymaga wszystkich dziewięciu specjalizacji");
        }
        all = ids.values().stream().sorted(Comparator
                .comparing(NecromancerMinionSpecializationDefinition::family)
                .thenComparingInt(NecromancerMinionSpecializationDefinition::unlockLevel)
                .thenComparing(NecromancerMinionSpecializationDefinition::id)).toList();
        byId = Map.copyOf(ids);
        var rawIds = new HashMap<String, NecromancerMinionSpecializationDefinition>();
        var families = new EnumMap<NecromancerMinionFamily, List<NecromancerMinionSpecializationDefinition>>(
                NecromancerMinionFamily.class);
        for (var definition : all) {
            rawIds.put(definition.id().rawId(), definition);
        }
        for (var family : NecromancerMinionFamily.values()) {
            var members = all.stream().filter(definition -> definition.family() == family).toList();
            if (members.size() != 3) {
                throw new IllegalArgumentException("Rodzina wymaga trzech specjalizacji: " + family);
            }
            families.put(family, members);
        }
        byRawId = Map.copyOf(rawIds);
        byFamily = Map.copyOf(families);
    }

    public static NecromancerBookOfDeadRegistry sourceCatalog() {
        return SOURCE_CATALOG;
    }

    public List<NecromancerMinionSpecializationDefinition> all() {
        return all;
    }

    public Map<NecromancerMinionSpecializationId, NecromancerMinionSpecializationDefinition> byId() {
        return byId;
    }

    public List<NecromancerMinionSpecializationDefinition> byFamily(NecromancerMinionFamily family) {
        return byFamily.get(Objects.requireNonNull(family, "family"));
    }

    public Optional<NecromancerMinionSpecializationDefinition> find(NecromancerMinionSpecializationId id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public Optional<NecromancerMinionSpecializationDefinition> findByRawId(String rawId) {
        return Optional.ofNullable(rawId == null ? null : byRawId.get(rawId));
    }

    public NecromancerMinionRules rules() {
        return RULES;
    }

    private static List<NecromancerMinionSpecializationDefinition> sourceDefinitions() {
        return List.of(
                specialization(SKIRMISHER, "Harcownicy", "Skirmishers", 5, BONE,
                        "Zadają o 30% więcej obrażeń i mają o 15% mniej zdrowia.",
                        List.of(fact("damage", "Obrażenia Harcowników +30%.", "30", PERCENT),
                                fact("life", "Zdrowie Harcowników -15%.", "-15", PERCENT)),
                        upgrade(BookOfDeadChoiceType.UPGRADE_1,
                                fact("additional_minions", "+2 dodatkowych Harcowników.", "2", COUNT),
                                text("aggression", "Agresywne zachowanie bezpośrednio po przywołaniu.")),
                        upgrade(BookOfDeadChoiceType.UPGRADE_2,
                                text("vulnerable", "Trafienie nakłada Odsłonięcie (Vulnerable) na 4 s."),
                                fact("slow", "Trafienie spowalnia o 50%.", "50", PERCENT),
                                fact("duration", "Czas Vulnerable i Slow: 4 s.", "4", SECONDS)),
                        sacrifice(SKIRMISHER,
                                fact("critical_strike_chance", "Szansa gracza na trafienie krytyczne +5%[+].", "5", PERCENT_ADDITIVE))),
                specialization(DEFENDER, "Obrońcy", "Defenders", 8, BLOOD,
                        "Mają o 15% więcej zdrowia.",
                        List.of(fact("life", "Zdrowie Obrońców +15%.", "15", PERCENT)),
                        upgrade(BookOfDeadChoiceType.UPGRADE_1,
                                new BookOfDeadSourceFact("thorns", "Liczba Cierni zależy od kontekstu statystyk / poziomu "
                                        + "źródła (source-dependent Thorns amount).", null, SOURCE_DEPENDENT_AMOUNT),
                                fact("taunt_duration", "Obrońcy mogą prowokować przeciwników (Taunt) przez 6 s.", "6", SECONDS),
                                fact("thorns_splinters", "Po otrzymaniu obrażeń odłamki zadają pobliskim wrogom 50% "
                                        + "Cierni danego Obrońcy.", "50", PERCENT_OF_THORNS)),
                        upgrade(BookOfDeadChoiceType.UPGRADE_2,
                                fact("blood_orb_chance", "Zadawanie obrażeń: 10% szansy utworzenia Krwawej Kuli.", "10", PERCENT_CHANCE)),
                        sacrifice(DEFENDER,
                                fact("all_resistance", "Odporność gracza na wszystkie żywioły +40%[+].", "40", PERCENT_ADDITIVE))),
                specialization(REAPER, "Żniwiarze", "Reapers", 12, DARKNESS,
                        "Wykonują specjalny przygotowywany atak (wind-up) co 10 s.",
                        List.of(fact("wind_up_interval", "Przygotowywany atak co 10 s.", "10", SECONDS)),
                        upgrade(BookOfDeadChoiceType.UPGRADE_1,
                                fact("cooldown_reduction", "Przygotowywany atak skraca jeden aktywny cooldown o 3 s.", "-3", SECONDS),
                                text("corpse", "Przygotowywany atak tworzy Zwłoki.")),
                        upgrade(BookOfDeadChoiceType.UPGRADE_2,
                                fact("damage", "Obrażenia Żniwiarzy +50%[x].", "50", PERCENT_MULTIPLICATIVE),
                                fact("stun_chance", "15% szansy ogłuszenia trafionego przeciwnika.", "15", PERCENT_CHANCE),
                                fact("stun_duration", "Ogłuszenie trwa 1 s.", "1", SECONDS)),
                        sacrifice(REAPER,
                                fact("player_damage", "Obrażenia gracza +15%[x].", "15", PERCENT_MULTIPLICATIVE))),
                specialization(SHADOW_MAGE, "Magowie Cienia", "Shadow Mages", 5, DARKNESS,
                        "Zadają obrażenia Cienia (Shadow).",
                        List.of(text("damage_type", "Typ obrażeń: Shadow.")),
                        upgrade(BookOfDeadChoiceType.UPGRADE_1,
                                fact("corrupting_damage", "Dodatkowe 100% Corrupting damage w ciągu 6 s; "
                                        + "coefficient mechaniki Book, poza tabelą skilla Skeleton Mage.", "100", SOURCE_DAMAGE_PERCENT),
                                fact("duration", "Corrupting damage rozłożone na 6 s.", "6", SECONDS)),
                        upgrade(BookOfDeadChoiceType.UPGRADE_2,
                                fact("barrier", "Pociski dają graczowi i Magowi Cienia Barierę równą 3% Maximum Life.", "3", PERCENT_OF_MAXIMUM_LIFE),
                                fact("duration", "Bariera trwa 4 s.", "4", SECONDS),
                                fact("barrier_cap", "Maksymalna Bariera: 30% Maximum Life.", "30", PERCENT_OF_MAXIMUM_LIFE),
                                text("recipients", "Barierę otrzymują gracz oraz Mag Cienia.")),
                        sacrifice(SHADOW_MAGE,
                                fact("essence_regeneration", "Regeneracja Esencji gracza +20%[+].", "20", PERCENT_ADDITIVE),
                                fact("maximum_essence", "Maksymalna Esencja gracza +20.", "20", FLAT_AMOUNT))),
                specialization(COLD_MAGE, "Magowie Zimna", "Cold Mages", 18, DARKNESS,
                        "Zadają obrażenia Zimna (Cold); tag źródłowy specjalizacji to Darkness.",
                        List.of(text("damage_type", "Typ obrażeń: Cold.")),
                        upgrade(BookOfDeadChoiceType.UPGRADE_1,
                                fact("forked_projectiles", "Początkowy pocisk rozdziela się na 2 po trafieniu; "
                                        + "bez założenia trzech trafień tego samego celu.", "2", COUNT),
                                fact("weaken_duration", "Trafieni przeciwnicy są Osłabieni (Weakened) przez 4 s.", "4", SECONDS)),
                        upgrade(BookOfDeadChoiceType.UPGRADE_2,
                                text("blizzard_trigger", "Okazjonalnie rzucają Zamieć (Blizzard); częstotliwość nie jest określona."),
                                fact("blizzard_damage", "Zamieć zadaje łącznie 200% Cold damage w ciągu 6 s.", "200", SOURCE_DAMAGE_PERCENT),
                                fact("duration", "Obrażenia Zamieci rozłożone na 6 s.", "6", SECONDS),
                                fact("chill", "Zamieć wyziębia o 6% na sekundę.", "6", PERCENT_PER_SECOND),
                                fact("vulnerable_duration", "Uszkodzeni przez Zamieć przeciwnicy są Vulnerable przez 4 s.", "4", SECONDS)),
                        sacrifice(COLD_MAGE,
                                fact("damage_to_vulnerable", "Obrażenia gracza przeciw Vulnerable enemies +20%[x].", "20", PERCENT_MULTIPLICATIVE))),
                specialization(BONE_MAGE, "Magowie Kości", "Bone Mages", 22, BONE,
                        "Zadają wysokie obrażenia kosztem własnego zdrowia.",
                        List.of(text("life_cost", "Ataki zadają wysokie obrażenia kosztem własnego Life Maga Kości.")),
                        upgrade(BookOfDeadChoiceType.UPGRADE_1,
                                fact("additional_projectiles", "+2 dodatkowe pociski.", "2", COUNT),
                                fact("projectile_damage", "Każdy dodatkowy pocisk zadaje 75% normalnych obrażeń "
                                        + "(relative to normal damage, bez podstawy Weapon Damage).", "75", PERCENT_OF_NORMAL_DAMAGE)),
                        boneMageFortifyChoice(),
                        sacrifice(BONE_MAGE,
                                fact("player_damage_with_overpower", "Obrażenia gracza +20%[x], tylko gdy gracz ma stos Overpower.", "20", PERCENT_MULTIPLICATIVE))),
                specialization(BONE_GOLEM, "Golem Kościany", "Bone Golem", 8, BONE,
                        "Zdolność bazowa prowokuje przeciwników (Taunt).",
                        List.of(text("base_ability", "Zdolność bazowa: Taunt enemies.")),
                        upgrade(BookOfDeadChoiceType.UPGRADE_1,
                                fact("corpses", "Aktywowanie zdolności Golema tworzy 5 Zwłok.", "5", COUNT)),
                        upgrade(BookOfDeadChoiceType.UPGRADE_2,
                                text("trigger", "Po otrzymaniu obrażeń Golem uwalnia Kościane Kolce (Bone Spikes)."),
                                fact("minimum_interval", "Kościane Kolce mogą wystąpić najwyżej raz na 3 s.", "3", SECONDS),
                                fact("spikes_damage", "Kościane Kolce: źródłowy coefficient mechaniki 250% damage; "
                                        + "poza tabelą skilla Golem.", "250", SOURCE_DAMAGE_PERCENT),
                                fact("vulnerable_duration", "Kościane Kolce nakładają Vulnerable na 4 s.", "4", SECONDS)),
                        sacrifice(BONE_GOLEM,
                                fact("attack_speed", "Szybkość ataku gracza +10%[+].", "10", PERCENT_ADDITIVE))),
                specialization(BLOOD_GOLEM, "Golem Krwawy", "Blood Golem", 28, BLOOD,
                        "Wysysa zdrowie pobliskich przeciwników, lecząc i wzmacniając siebie.",
                        List.of(text("base_ability", "Wysysanie Life pobliskich przeciwników leczy i wzmacnia Golema.")),
                        upgrade(BookOfDeadChoiceType.UPGRADE_1,
                                text("drain", "Wysysa Life innych sług; premie zależą od faktycznej liczby wyssanych Minions."),
                                fact("life_per_minion", "Maximum Life +5% na każdego wyssanego sługę.", "5", PERCENT),
                                fact("damage_per_minion", "Obrażenia +5%[x] na każdego wyssanego sługę.", "5", PERCENT_MULTIPLICATIVE),
                                fact("duration", "Premie po wyssaniu trwają 20 s.", "20", SECONDS),
                                fact("life_cap", "Limit premii Maximum Life: +50%.", "50", PERCENT),
                                fact("damage_cap", "Limit premii obrażeń: +50%[x].", "50", PERCENT_MULTIPLICATIVE)),
                        upgrade(BookOfDeadChoiceType.UPGRADE_2,
                                fact("fortify_per_enemy", "Aktywowanie zdolności wzmacnia (Fortify) gracza o 10% "
                                        + "Maximum Life na każdego wyssanego przeciwnika; bez założonej liczby wrogów.", "10", PERCENT_OF_MAXIMUM_LIFE)),
                        sacrifice(BLOOD_GOLEM,
                                fact("maximum_life", "Maksymalne zdrowie gracza +20%[x].", "20", PERCENT_MULTIPLICATIVE))),
                specialization(IRON_GOLEM, "Golem Żelazny", "Iron Golem", 32, DARKNESS,
                        "Uderza w ziemię (Slam) i ogłusza przeciwników.",
                        List.of(text("base_ability", "Zdolność bazowa: Slam oraz Stun enemies.")),
                        upgrade(BookOfDeadChoiceType.UPGRADE_1,
                                fact("shockwave_damage", "Każdy atak wywołuje falę uderzeniową: źródłowe 125%[x] "
                                        + "shockwave damage głównemu celowi i przeciwnikom za nim; poza tabelą skilla Golem.", "125", SOURCE_DAMAGE_PERCENT_MULTIPLICATIVE)),
                        upgrade(BookOfDeadChoiceType.UPGRADE_2,
                                text("pull", "Slam również przyciąga przeciwników (Pull)."),
                                fact("size", "Rozmiar Slam +50%.", "50", PERCENT)),
                        sacrifice(IRON_GOLEM,
                                fact("critical_strike_damage", "Obrażenia krytyczne gracza +15%[x].", "15", PERCENT_MULTIPLICATIVE)))
        );
    }

    private static BookOfDeadChoiceDefinition boneMageFortifyChoice() {
        var facts = List.of(
                fact("fortify", "Ataki Magów Kości wzmacniają (Fortify) gracza o 0.1% Maximum Life.", "0.1", PERCENT_OF_MAXIMUM_LIFE),
                text("corpse_on_death", "Śmierć Maga Kości tworzy Zwłoki."));
        var conflict = new BookOfDeadChoiceDefinition.SourceConflict("fortify", number("3"), SOURCE_REFERENCE,
                number("0.1"), FORTIFY_OFFICIAL_SOURCE, BookOfDeadChoiceDefinition.Resolution.OFFICIAL_SOURCE_RESOLVED,
                "Icy Veins wyświetla 3%; oficjalny Blizzard patch 2.5 zmniejszył Fortify na atak "
                        + "z 3% do 0.1% Maximum Life. Pierwszeństwo ma oficjalne źródło; konflikt rozstrzygnięty.");
        return new BookOfDeadChoiceDefinition(BookOfDeadChoiceType.UPGRADE_2, describe(facts), facts,
                List.of(), Optional.empty(), List.of(conflict));
    }

    private static NecromancerMinionSpecializationDefinition specialization(
            NecromancerMinionSpecializationId id, String displayName, String canonicalName, int unlockLevel,
            NecromancerMinionTheme theme, String passive, List<BookOfDeadSourceFact> passiveFacts,
            BookOfDeadChoiceDefinition upgrade1, BookOfDeadChoiceDefinition upgrade2, BookOfDeadChoiceDefinition sacrifice) {
        Optional<String> quest = id == BLOOD_GOLEM || id == IRON_GOLEM
                ? Optional.of("Call of the Underworld") : Optional.empty();
        return new NecromancerMinionSpecializationDefinition(id, displayName, canonicalName, unlockLevel, theme,
                passive, passiveFacts, List.of(upgrade1, upgrade2, sacrifice), quest,
                SOURCE_REFERENCE, BookOfDeadRuntimeStatus.NOT_RUNTIME_ENABLED);
    }

    private static BookOfDeadChoiceDefinition upgrade(BookOfDeadChoiceType type, BookOfDeadSourceFact... facts) {
        var effects = List.of(facts);
        return new BookOfDeadChoiceDefinition(type, describe(effects), effects,
                List.of(), Optional.empty(), List.of());
    }

    private static BookOfDeadChoiceDefinition sacrifice(NecromancerMinionSpecializationId id,
                                                        BookOfDeadSourceFact... facts) {
        boolean golem = id.family() == NecromancerMinionFamily.GOLEM;
        var penalty = new BookOfDeadSummonPenalty(golem
                ? BookOfDeadSummonPenalty.Type.GOLEM_DAMAGE_REDUCTION_MULTIPLICATIVE
                : BookOfDeadSummonPenalty.Type.SUMMON_COUNT_REDUCTION, number("50"), golem
                ? "Golem zadaje o 50%[x] mniej obrażeń; nadal można go przywoływać zgodnie z regułą summon skilla."
                : "Możliwa liczba summonów tej rodziny maleje o 50%; nadal można je przywoływać zgodnie z regułą summon skilla.");
        var bonuses = List.of(facts);
        return new BookOfDeadChoiceDefinition(BookOfDeadChoiceType.SACRIFICE,
                describe(bonuses) + " " + penalty.description(), List.of(), bonuses, Optional.of(penalty), List.of());
    }

    private static BookOfDeadSourceFact fact(String id, String description, String value, BookOfDeadSourceFact.Unit unit) {
        return new BookOfDeadSourceFact(id, description, number(value), unit);
    }

    private static BookOfDeadSourceFact text(String id, String description) {
        return new BookOfDeadSourceFact(id, description, null, TEXT);
    }

    private static BigDecimal number(String value) {
        return new BigDecimal(value);
    }

    private static String describe(List<BookOfDeadSourceFact> facts) {
        return facts.stream().map(BookOfDeadSourceFact::description).collect(Collectors.joining(" "));
    }
}
