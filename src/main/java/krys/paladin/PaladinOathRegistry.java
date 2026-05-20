package krys.paladin;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lokalny registry Przysięg Paladyna oparty wyłącznie o zatwierdzoną transkrypcję screenów użytkownika. */
public final class PaladinOathRegistry {
    public static final String SOURCE_REFERENCE = "docs/paladin/source-md/paladin_oaths_user_screens_2026_05_19.md";

    private static final Map<PaladinOathId, PaladinOathDefinition> DEFINITIONS = buildDefinitions();

    private PaladinOathRegistry() {
    }

    public static List<PaladinOathDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    public static Optional<PaladinOathDefinition> find(PaladinOathId id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static Optional<PaladinOathDefinition> findByRawId(String rawId) {
        if (rawId == null || rawId.isBlank() || "NONE".equals(rawId)) {
            return Optional.empty();
        }
        try {
            return find(PaladinOathId.valueOf(rawId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static Map<PaladinOathId, PaladinOathDefinition> buildDefinitions() {
        EnumMap<PaladinOathId, PaladinOathDefinition> definitions = new EnumMap<>(PaladinOathId.class);
        put(definitions, new PaladinOathDefinition(
                PaladinOathId.ADEPT,
                "Adept",
                "Zapewnia równowagę",
                List.of(
                        "Użycie umiejętności Adepta posiadającej czas odnowienia zmienia cię w postać Arbitra na 4,5 sek.",
                        "Uderzenia skrzydeł zyskują korzyści umiejętności Adepta.",
                        "Pod postacią Arbitra twoje umiejętności Adepta zadają obrażenia zwiększone o 50%[x]."
                ),
                List.of(
                        "Postać Arbitra gwarantuje 15%[x] premii do szybkości ruchu i zastępuje odskok anielskim skokiem.",
                        "Dodatkowo uderzenia skrzydeł wokół ciebie zadają 160% obrażeń."
                ),
                PaladinOathFamily.ADEPT,
                PaladinOathRuntimeStatus.NOT_RUNTIME_ENABLED,
                SOURCE_REFERENCE
        ));
        put(definitions, new PaladinOathDefinition(
                PaladinOathId.JUDGE,
                "Sędzia",
                "Niech sprawiedliwość dosięgnie wszystkich",
                List.of(
                        "Twoje umiejętności Podstawowe mogą teraz nakładać Osąd.",
                        "Osąd można teraz detonować wcześniej poprzez użycie umiejętności Głównych Sędziego, zadając 80% obrażeń od broni na niewielkim obszarze i zyskując korzyści umiejętności Sędziego.",
                        "Za każdym razem gdy dokonujesz na wrogu Osądu, zwiększasz obrażenia, które od ciebie otrzymuje, o 8%[x], maksymalnie do 80%[x], aż wróg zginie."
                ),
                List.of("Osąd oznacza wroga na 3 sek. i po wygaśnięciu zadaje 80% obrażeń."),
                PaladinOathFamily.JUDGE,
                PaladinOathRuntimeStatus.NOT_RUNTIME_ENABLED,
                SOURCE_REFERENCE
        ));
        put(definitions, new PaladinOathDefinition(
                PaladinOathId.JUGGERNAUT,
                "Moloch",
                "Nigdy się nie ugnę",
                List.of(
                        "Użycie umiejętności Molocha zużywa 8 poziomów kumulacji Animuszu i sprawia, że umiejętności Molocha przez 5 sek. zadają obrażenia zwiększone o 60%[x] i mają rozmiar zwiększony o 20%.",
                        "Twoja minimalna wartość Animuszu zwiększa się o 1 i nie jest on zużywany, gdy odnosisz obrażenia."
                ),
                List.of(
                        "Aktywny Animusz zwiększa wartość pancerza o 25%[+].",
                        "Odniesienie obrażeń bezpośrednich zużywa ładunek.",
                        "Możesz mieć maksymalnie 8 ładunków."
                ),
                PaladinOathFamily.JUGGERNAUT,
                PaladinOathRuntimeStatus.PARTIALLY_RUNTIME_ENABLED,
                SOURCE_REFERENCE
        ));
        put(definitions, new PaladinOathDefinition(
                PaladinOathId.ZEALOT,
                "Zelota",
                "Niech nikt nie zwątpi",
                List.of(
                        "Używanie umiejętności Zeloty zapewnia Ferwor na 2 sek.",
                        "Trafienia krytyczne tymi umiejętnościami naśladują atak, zadając 17% obrażeń i powtarzając każdy poziom kumulacji Ferworu.",
                        "Mając maksimum Ferworu, zyskujesz dodatkowo umocnienie równe 1% twojego maksymalnego zdrowia (37 pkt.)."
                ),
                List.of(
                        "Umocnienie stanowi dodatkowy zasób zdrowia, którego zużywanie zapewnia ci leczenie z upływem czasu.",
                        "Ferwor wzmacnia niektóre umiejętności za każdy poziom kumulacji."
                ),
                PaladinOathFamily.ZEALOT,
                PaladinOathRuntimeStatus.NOT_RUNTIME_ENABLED,
                SOURCE_REFERENCE
        ));
        return definitions;
    }

    private static void put(Map<PaladinOathId, PaladinOathDefinition> definitions,
                            PaladinOathDefinition definition) {
        definitions.put(definition.getId(), definition);
    }
}
