package krys.paladin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static krys.paladin.DamagePercentComponent.ACTIVE_DAMAGE;
import static krys.paladin.DamagePercentComponent.ADDITIONAL_STRIKE_DAMAGE;
import static krys.paladin.DamagePercentComponent.BURST_DAMAGE;
import static krys.paladin.DamagePercentComponent.FIRST_STRIKE_DAMAGE;
import static krys.paladin.DamagePercentComponent.LANDING_DAMAGE;
import static krys.paladin.DamagePercentComponent.PASSIVE_DAMAGE;
import static krys.paladin.DamagePercentComponent.PRIMARY_DAMAGE;
import static krys.paladin.DamagePercentComponent.SECOND_STRIKE_DAMAGE;
import static krys.paladin.PaladinSkillTreeStatus.NEEDS_VERIFICATION;
import static krys.paladin.PaladinSkillTreeStatus.NON_DAMAGE;
import static krys.paladin.PaladinSkillTreeStatus.UNSUPPORTED;
import static krys.paladin.PaladinSkillTreeType.DAMAGE;
import static krys.paladin.PaladinSkillTreeType.DEFENSIVE;
import static krys.paladin.PaladinSkillTreeType.MOBILITY;
import static krys.paladin.PaladinSkillTreeType.SPECIAL;
import static krys.paladin.PaladinSkillTreeType.SUPPORT;

/** Pelny opisowy rejestr drzewa Paladyna oparty o lokalne materialy zrodlowe. */
public final class PaladinSkillTreeRegistry {
    public static final String BASIC_PDF = "docs/paladin/source-pdfs/paladin_basic_skill_registry_final.pdf";
    public static final String CORE_PDF = "docs/paladin/source-pdfs/paladin_core_skill_registry_final.pdf";
    public static final String AURA_PDF = "docs/paladin/source-pdfs/paladin_aura_skill_registry_final.pdf";
    public static final String COURAGE_PDF = "docs/paladin/source-pdfs/diablo4_paladyn_odwaga_umiejetnosci.pdf";
    public static final String JUSTICE_PDF = "docs/paladin/source-pdfs/diablo4_paladyn_sprawiedliwosc_umiejetnosci.pdf";
    public static final String SPECIAL_POWERS_PDF = "docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf";

    private static final String NO_RUNTIME = "Skill jest odwzorowany opisowo z PDF, ale nie ma jeszcze zweryfikowanego modelu DPS runtime.";
    private static final String NO_EXPLICIT_R1_TREE_MAX = NO_RUNTIME + " Bazowy procent obrażeń pozostaje null: PDF nie podaje jednoznacznie R1/treeMax.";
    private static final String MULTI_COMPONENT_PERCENT_NEEDS_VERIFICATION = NO_RUNTIME + " Bazowy procent obrażeń pozostaje null: komponent wielohitowy/tickowy/warunkowy wymaga weryfikacji interpretacji procentu obrażeń.";
    private static final String LOCAL_JSON_DAMAGE_RANK_TABLE = NO_RUNTIME + " Lokalny JSON Fextralife podaje pełną pojedynczą tabelę bazowych procentów obrażeń 1..15; tabela pozostaje źródłem opisowym i nie odblokowuje DPS runtime.";
    private static final String LOCAL_JSON_COMPONENT_DAMAGE_RANK_TABLE = NO_RUNTIME + " Lokalny JSON Fextralife podaje komponentowe tabele bazowych procentów obrażeń 1..15; komponenty pozostają danymi źródłowymi, nie są sumowane i nie odblokowują DPS runtime.";
    private static final Map<String, PaladinTreeSkill> SKILLS_BY_ID = createSkills();

    private PaladinSkillTreeRegistry() {
    }

    public static List<PaladinTreeSkill> allSkills() {
        return List.copyOf(SKILLS_BY_ID.values());
    }

    public static Optional<PaladinTreeSkill> findSkill(String skillId) {
        return Optional.ofNullable(SKILLS_BY_ID.get(skillId));
    }

    public static PaladinTreeSkill requireSkill(String skillId) {
        return findSkill(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Nieznany skill Paladyna w rejestrze PDF: " + skillId));
    }

    private static Map<String, PaladinTreeSkill> createSkills() {
        LinkedHashMap<String, PaladinTreeSkill> skills = new LinkedHashMap<>();

        put(skills, skill("wymach", "Wymach", BASIC_PDF, "basic", damagePercentRanks(
                        75, 83, 90, 97, 109, 116, 124, 131, 139, 150, 157, 165, 172, 180, 191
                ), DAMAGE, NEEDS_VERIFICATION,
                groups(BASIC_PDF, "Wymach",
                        group1(upgrade("generowanie_wiary", "Generowanie Wiary"), upgrade("zwiekszenie_obrazen", "Zwiększenie Obrażeń")),
                        group2(upgrade("szybkosc_uzycia", "Szybkość Użycia"), upgrade("odsloniecie", "Odsłonięcie")),
                        group3(upgrade("powracajaca_swiatlosc", "Powracająca Światłość"), upgrade("miecz_mistrzostwa", "Miecz Mistrzostwa"), upgrade("krzyzowe_uderzenie", "Krzyżowe Uderzenie"))),
                LOCAL_JSON_DAMAGE_RANK_TABLE, null, 14, 5, 26, List.of()));
        put(skills, skill("swiety_pocisk", "Święty Pocisk", BASIC_PDF, "basic", damagePercentRanks(
                        90, 99, 108, 117, 131, 139, 148, 157, 166, 180, 189, 198, 207, 216, 229
                ), DAMAGE, NEEDS_VERIFICATION,
                groups(BASIC_PDF, "Święty Pocisk",
                        group1(upgrade("generowanie_wiary", "Generowanie Wiary"), upgrade("osad", "Osąd")),
                        group2(upgrade("spowolnienie", "Spowolnienie"), upgrade("szybkosc_uzycia", "Szybkość Użycia")),
                        group3(upgrade("burzowy_pocisk", "Burzowy Pocisk"), upgrade("boski_pocisk", "Boski Pocisk"), upgrade("rykoszetujacy_pocisk", "Rykoszetujący Pocisk"))),
                LOCAL_JSON_DAMAGE_RANK_TABLE, null, 16, 7, 57, List.of()));
        put(skills, skill("starcie", "Starcie", BASIC_PDF, "basic", damagePercentRanks(
                        115, 126, 138, 149, 167, 178, 190, 201, 213, 230, 241, 253, 264, 276, 293
                ), DAMAGE, NEEDS_VERIFICATION,
                groups(BASIC_PDF, "Starcie",
                        group1(upgrade("generowanie_wiary", "Generowanie Wiary"), upgrade("animusz", "Animusz")),
                        group2(upgrade("skutecznosc_marszu_krzyzowca", "Skuteczność Marszu Krzyżowca"), upgrade("zwiekszenie_obrazen", "Zwiększenie Obrażeń")),
                        group3(upgrade("brac_ich", "Brać Ich"), upgrade("potyczka", "Potyczka"), upgrade("kara", "Kara"))),
                LOCAL_JSON_DAMAGE_RANK_TABLE, null, 20, 10, 63, List.of(baseUtilityModifier(
                        "Starcie",
                        "Marsz Krzyżowca",
                        "15%[X]",
                        "6 sek.",
                        "Trafienie wroga Starciem zwiększa szansę na blok o 15%[x] na 6 sek."
                ))));
        put(skills, skill("natarcie", "Natarcie", BASIC_PDF, "basic", damagePercentRanks(
                        105, 115, 126, 136, 152, 163, 173, 184, 194, 210, 220, 231, 241, 252, 268
                ), MOBILITY, NEEDS_VERIFICATION,
                groups(BASIC_PDF, "Natarcie",
                        group1(upgrade("umocnienie", "Umocnienie"), upgrade("nieograniczenie", "Nieograniczenie")),
                        group2(upgrade("blysk_ostrza", "Błysk Ostrza"), upgrade("pedzaca_fala", "Pędząca Fala")),
                        group3(upgrade("zryw_forpoczty", "Zryw Forpoczty"), upgrade("oslabienie", "Osłabienie"), upgrade("szansa_na_trafienie_krytyczne", "Szansa na Trafienie Krytyczne"))),
                LOCAL_JSON_DAMAGE_RANK_TABLE, null, 18, null, 18, List.of()));

        put(skills, skill("blogoslawiona_tarcza", "Błogosławiona Tarcza", CORE_PDF, "core", damagePercentRanks(
                        205, 226, 246, 266, 297, 318, 338, 359, 379, 410, 430, 451, 471, 492, 523
                ), DAMAGE, NEEDS_VERIFICATION,
                groups(CORE_PDF, "Błogosławiona Tarcza",
                        group1(upgrade("generowanie_wiary", "Generowanie Wiary"), upgrade("szybkosc_uzycia", "Szybkość Użycia")),
                        group2(upgrade("premia_do_obrazen", "Premia do Obrażeń"), upgrade("dodatkowy_pancerz_i_szansa_na_blok", "Dodatkowy Pancerz i Szansa na Blok")),
                        group3(upgrade("tarcza_sprawiedliwosci", "Tarcza Sprawiedliwości"), upgrade("tarcza_ozywienca", "Tarcza Ożywieńca"), upgrade("tarcza_pomsty", "Tarcza Pomsty"))),
                LOCAL_JSON_DAMAGE_RANK_TABLE));
        put(skills, skill("blogoslawiony_mlot", "Błogosławiony Młot", CORE_PDF, "core", blessedHammerDamagePercentRanks(), DAMAGE, NEEDS_VERIFICATION,
                groups(CORE_PDF, "Błogosławiony Młot",
                        group1(upgrade("redukcja_kosztu", "Redukcja Kosztu"), upgrade("premia_do_obrazen", "Premia do Obrażeń")),
                        group2(upgrade("zwiekszenie_szybkosci_uzycia", "Zwiększenie Szybkości Użycia"), upgrade("spowolnienie", "Spowolnienie")),
                        group3(upgrade("budujaca_walka", "Budująca Walka"), upgrade("apostolska_aureola", "Apostolska Aureola"), upgrade("druzgocacy_cios", "Druzgocący Cios"))),
                NO_RUNTIME + " Lokalny JSON Fextralife podaje pełną tabelę bazowych procentów obrażeń 1..15; tabela pozostaje źródłem opisowym i nie odblokowuje DPS runtime.",
                10, null, null));
        put(skills, skill("boska_lanca", "Boska Lanca", CORE_PDF, "core", damagePercentRanks(
                        90, 99, 108, 117, 131, 139, 148, 157, 166, 180, 189, 198, 207, 216, 229
                ), DAMAGE, NEEDS_VERIFICATION,
                groups(CORE_PDF, "Boska Lanca",
                        group1(upgrade("premia_do_obrazen", "Premia do Obrażeń"), upgrade("redukcja_kosztu", "Redukcja Kosztu")),
                        group2(upgrade("szybkosc_uzycia", "Szybkość Użycia"), upgrade("skumulowane_obrazenia", "Skumulowane Obrażenia")),
                        group3(upgrade("zarliwy_rzut", "Żarliwy Rzut"), upgrade("boski_oszczep", "Boski Oszczep"), unsupportedUpgrade("trzeci_wariant_grupy_3", "Trzeci wariant grupy 3"))),
                LOCAL_JSON_DAMAGE_RANK_TABLE + " PDF wskazuje trzeci wariant grupy 3 bez nazwy i pełnego opisu."));
        put(skills, skill("uderzenie_tarcza", "Uderzenie Tarczą", CORE_PDF, "core", damagePercentRanks(
                        205, 226, 246, 266, 297, 318, 338, 359, 379, 410, 430, 451, 471, 492, 523
                ), DAMAGE, NEEDS_VERIFICATION,
                groups(CORE_PDF, "Uderzenie Tarczą",
                        group1(upgrade("uderzenia_sa_blokowaniem", "Uderzenia są Blokowaniem"), upgrade("premia_do_rozmiaru", "Premia do Rozmiaru")),
                        group2(upgrade("oblezenie", "Oblężenie"), upgrade("porazenie", "Porażenie")),
                        group3(upgrade("wylom", "Wyłom"), upgrade("odleglosc", "Odległość"), upgrade("premia_do_obrazen", "Premia do Obrażeń"))),
                LOCAL_JSON_DAMAGE_RANK_TABLE));
        put(skills, skill("zapal", "Zapał", CORE_PDF, "core", componentDamagePercentRanks(Map.ofEntries(
                        Map.entry(PRIMARY_DAMAGE, damagePercentRanks(80, 88, 96, 104, 116, 124, 132, 140, 148, 160, 168, 176, 184, 192, 204)),
                        Map.entry(ADDITIONAL_STRIKE_DAMAGE, damagePercentRanks(20, 22, 24, 26, 29, 31, 33, 35, 37, 40, 42, 44, 46, 48, 51))
                )), DAMAGE, NEEDS_VERIFICATION,
                groups(CORE_PDF, "Zapał",
                        group1(upgrade("oslabienie", "Osłabienie"), upgrade("szansa_na_trafienie_krytyczne", "Szansa na Trafienie Krytyczne")),
                        group2(upgrade("umocnienie", "Umocnienie"), upgrade("dodatkowe_ciosy", "Dodatkowe Ciosy")),
                        group3(upgrade("dziedzictwo_zeloty", "Dziedzictwo Zeloty"), upgrade("smierc_albo_chwala", "Śmierć albo Chwała"), unsupportedUpgrade("ostatni_wariant_grupy_3", "Ostatni wariant grupy 3"))),
                LOCAL_JSON_COMPONENT_DAMAGE_RANK_TABLE + " PDF wskazuje ostatni wariant grupy 3 bez nazwy."));

        put(skills, skill("aura_fanatyzmu", "Aura Fanatyzmu", AURA_PDF, "aura", SUPPORT, NON_DAMAGE,
                groups(AURA_PDF, "Aura Fanatyzmu",
                        group1(upgrade("dodatkowa_maksymalna_ilosc_zasobu", "Dodatkowa Maksymalna Ilość Zasobu"), upgrade("dodatkowa_kumulacja_efektu_pasywnego", "Dodatkowa Kumulacja Efektu Pasywnego")),
                        group2(upgrade("generowanie_zasobow", "Generowanie Zasobów"), upgrade("krzepkosc", "Krzepkość")),
                        group3(upgrade("obrzed_zemsty", "Obrzęd Zemsty"), upgrade("obrzed_pokory", "Obrzęd Pokory"), upgrade("obrzed_odkupienia", "Obrzęd Odkupienia"))),
                "Aura opisuje buffy/debuffy; brak bezpośredniego modelu obrażeń jako osobne źródło DPS."));
        put(skills, skill("aura_smialosci", "Aura Śmiałości", AURA_PDF, "aura", DEFENSIVE, NEEDS_VERIFICATION,
                groups(AURA_PDF, "Aura Śmiałości",
                        group1(upgrade("nieustepliwosc", "Nieustępliwość"), upgrade("maksimum_zdrowia", "Maksimum Zdrowia")),
                        group2(upgrade("krzepkosc", "Krzepkość"), upgrade("dodatkowe_leczenie", "Dodatkowe Leczenie")),
                        group3(upgrade("obrzed_cierni", "Obrzęd Cierni"), upgrade("obrzed_modlitwy", "Obrzęd Modlitwy"), upgrade("obrzed_mocy", "Obrzęd Mocy"))),
                "Aura ma efekty defensywne, ciernie i premię obrażeń; wpływ na DPS wymaga weryfikacji."));
        put(skills, skill("aura_swietej_swiatlosci", "Aura Świętej Światłości", AURA_PDF, "aura", componentDamagePercentRanks(Map.ofEntries(
                        Map.entry(PASSIVE_DAMAGE, damagePercentRanks(45, 50, 54, 58, 65, 70, 74, 79, 83, 90, 94, 99, 103, 108, 115)),
                        Map.entry(ACTIVE_DAMAGE, damagePercentRanks(320, 352, 384, 416, 464, 496, 528, 560, 592, 640, 672, 704, 736, 768, 816))
                )), DAMAGE, NEEDS_VERIFICATION,
                groups(AURA_PDF, "Aura Świętej Światłości",
                        group1(upgrade("dodatkowe_odbicie", "Dodatkowe Odbicie"), upgrade("dodatkowe_cele", "Dodatkowe Cele")),
                        group2(upgrade("premia_do_obrazen_osadu", "Premia do Obrażeń Osądu"), upgrade("krzepkosc", "Krzepkość")),
                        group3(upgrade("obrzed_osadu", "Obrzęd Osądu"), upgrade("obrzed_laski", "Obrzęd Łaski"), upgrade("obrzed_podporzadkowania", "Obrzęd Podporządkowania"))),
                LOCAL_JSON_COMPONENT_DAMAGE_RANK_TABLE));

        put(skills, skill("szarza_z_tarcza", "Szarża z Tarczą", COURAGE_PDF, "odwaga", damagePercentRanks(
                        90, 99, 108, 117, 131, 139, 148, 157, 166, 180, 189, 198, 207, 216, 229
                ), MOBILITY, NEEDS_VERIFICATION,
                groups(COURAGE_PDF, "Szarża z Tarczą",
                        group1(upgrade("premia_do_obrazen", "Premia do Obrażeń"), upgrade("animusz", "Animusz")),
                        group2(upgrade("odwet", "Odwet"), upgrade("trafienie_jako_blok", "Trafienie Jako Blok")),
                        group3(upgrade("nieustepliwa_szarza", "Nieustępliwa Szarża"), upgrade("szarza_prawosci", "Szarża Prawości"), upgrade("szarza_falangi", "Szarża Falangi"))),
                LOCAL_JSON_DAMAGE_RANK_TABLE + " JSON zawiera też utility Armor Gained, które nie jest sumowane do obrażeń."));
        put(skills, skill("egida", "Egida", COURAGE_PDF, "odwaga", DEFENSIVE, NEEDS_VERIFICATION,
                groups(COURAGE_PDF, "Egida",
                        group1(upgrade("nieustepliwosc", "Nieustępliwość"), upgrade("redukcja_czasu_odnowienia", "Redukcja Czasu Odnowienia")),
                        group2(upgrade("redukcja_blokowanych_obrazen", "Redukcja Blokowanych Obrażeń"), upgrade("czas_dzialania", "Czas Działania")),
                        group3(upgrade("zdecydowana_stanowczosc", "Zdecydowana Stanowczość"), upgrade("tarcza_wiary", "Tarcza Wiary"), upgrade("bezkarnosc", "Bezkarność"))),
                "Bazowo defensywna; modyfikatory Osądu, Odwetu i cierni wymagają weryfikacji DPS."));
        put(skills, skill("spadajaca_gwiazda", "Spadająca Gwiazda", COURAGE_PDF, "odwaga", componentDamagePercentRanks(Map.ofEntries(
                        Map.entry(LANDING_DAMAGE, damagePercentRanks(80, 264, 288, 312, 348, 372, 396, 420, 444, 480, 504, 528, 552, 576, 612))
                )), DAMAGE, NEEDS_VERIFICATION,
                groups(COURAGE_PDF, "Spadająca Gwiazda",
                        group1(upgrade("dodatkowy_ladunek", "Dodatkowy Ładunek"), upgrade("odsloniecie", "Odsłonięcie")),
                        group2(upgrade("obrazenia", "Obrażenia"), upgrade("redukcja_czasu_odnowienia", "Redukcja Czasu Odnowienia")),
                        group3(upgrade("predkosc_swiatlosci", "Prędkość Światłości"), upgrade("upadek_gwiazdy", "Upadek Gwiazdy"), upgrade("fanatyczne_zstapienie", "Fanatyczne Zstąpienie"))),
                LOCAL_JSON_COMPONENT_DAMAGE_RANK_TABLE + " Komponent JUMP_DAMAGE nie został zaimportowany, bo lokalny JSON nie podaje go jednoznacznie dla rang 2, 4, 8 i 13."));
        put(skills, skill("mobilizacja", "Mobilizacja", COURAGE_PDF, "odwaga", SUPPORT, NON_DAMAGE,
                groups(COURAGE_PDF, "Mobilizacja",
                        group1(upgrade("szansa_na_trafienie_krytyczne", "Szansa na Trafienie Krytyczne"), upgrade("premia_do_czasu_dzialania", "Premia do Czasu Działania")),
                        group2(upgrade("redukcja_kosztu", "Redukcja Kosztu"), upgrade("nieograniczenie_i_szybkosc_ruchu", "Nieograniczenie i Szybkość Ruchu")),
                        group3(upgrade("slowa_poswiecenia", "Słowa Poświęcenia"), upgrade("slowa_natchnienia", "Słowa Natchnienia"), upgrade("slowa_pokrzepienia", "Słowa Pokrzepienia"))),
                "Skill wsparcia bez bezpośredniego komponentu obrażeń w PDF."));

        put(skills, skill("skazanie", "Skazanie", JUSTICE_PDF, "sprawiedliwosc", damagePercentRanks(
                        240, 264, 288, 312, 348, 372, 396, 420, 444, 480, 504, 528, 552, 576, 612
                ), DAMAGE, NEEDS_VERIFICATION,
                groups(JUSTICE_PDF, "Skazanie",
                        group1(upgrade("oslabienie", "Osłabienie"), upgrade("redukcja_czasu_odnowienia", "Redukcja Czasu Odnowienia")),
                        group2(upgrade("szybkosc_ruchu", "Szybkość Ruchu"), upgrade("zwiekszenie_rozmiaru", "Zwiększenie Rozmiaru")),
                        group3(upgrade("zebranie_trzodki", "Zebranie Trzódki"), upgrade("zadoscuczynienie", "Zadośćuczynienie"), upgrade("wezwanie_winnych", "Wezwanie Winnych"))),
                LOCAL_JSON_DAMAGE_RANK_TABLE + " Typ obrażeń i runtime single target nadal wymagają osobnej weryfikacji."));
        put(skills, skill("wlocznia_niebios", "Włócznia Niebios", JUSTICE_PDF, "sprawiedliwosc", componentDamagePercentRanks(Map.ofEntries(
                        Map.entry(PRIMARY_DAMAGE, damagePercentRanks(160, 176, 192, 208, 232, 248, 264, 280, 296, 320, 336, 352, 368, 384, 408)),
                        Map.entry(BURST_DAMAGE, damagePercentRanks(120, 132, 144, 156, 174, 186, 198, 210, 222, 240, 252, 264, 276, 288, 306))
                )), DAMAGE, NEEDS_VERIFICATION,
                groups(JUSTICE_PDF, "Włócznia Niebios",
                        group1(upgrade("premia_do_obrazen_osadu", "Premia do Obrażeń Osądu"), upgrade("redukcja_czasu_odnowienia", "Redukcja Czasu Odnowienia")),
                        group2(upgrade("pociski", "Pociski"), upgrade("odsloniecie", "Odsłonięcie")),
                        group3(upgrade("werdykt_niebios", "Werdykt Niebios"), upgrade("rozdarcie_niebios", "Rozdarcie Niebios"), upgrade("piesc_niebios", "Pięść Niebios"))),
                LOCAL_JSON_COMPONENT_DAMAGE_RANK_TABLE));
        put(skills, skill("konsekracja", "Konsekracja", JUSTICE_PDF, "sprawiedliwosc", damagePercentRanks(
                        75, 83, 90, 97, 109, 116, 124, 131, 139, 150, 157, 165, 172, 180, 191
                ), DAMAGE, NEEDS_VERIFICATION,
                groups(JUSTICE_PDF, "Konsekracja",
                        group1(upgrade("czas_dzialania", "Czas Działania"), upgrade("oslabienie", "Osłabienie")),
                        group2(upgrade("umocnienie", "Umocnienie"), upgrade("generowanie_zasobow", "Generowanie Zasobów")),
                        group3(upgrade("uswiecenie", "Uświęcenie"), upgrade("bastion", "Bastion"), upgrade("uswiecona_ziemia", "Uświęcona Ziemia"))),
                LOCAL_JSON_DAMAGE_RANK_TABLE + " JSON zawiera też healing, który nie jest sumowany do obrażeń."));
        put(skills, skill("oczyszczenie", "Oczyszczenie", JUSTICE_PDF, "sprawiedliwosc", SUPPORT, NEEDS_VERIFICATION,
                groups(JUSTICE_PDF, "Oczyszczenie",
                        group1(upgrade("generowanie_wiary", "Generowanie Wiary"), upgrade("redukcja_czasu_odnowienia", "Redukcja Czasu Odnowienia")),
                        group2(upgrade("premia_do_rozmiaru", "Premia do Rozmiaru"), upgrade("echo", "Echo")),
                        group3(upgrade("zasadzenie", "Zasądzenie"), upgrade("rozgrzeszenie", "Rozgrzeszenie"), upgrade("poddanie", "Poddanie"))),
                "Bazowo kontrola bez obrażeń; modyfikatory Osądu i Rozgrzeszenia wymagają weryfikacji DPS."));

        put(skills, skill("furia_niebios", "Furia Niebios", SPECIAL_POWERS_PDF, "moce_specjalne", DAMAGE, NEEDS_VERIFICATION,
                groups(SPECIAL_POWERS_PDF, "Furia Niebios",
                        group1(upgrade("czas_dzialania", "Czas Działania"), upgrade("spowolnienie", "Spowolnienie")),
                        group2(upgrade("osad", "Osąd"), upgrade("premia_do_obrazen", "Premia do Obrażeń")),
                        group3(upgrade("ostateczna_sprawiedliwosc", "Ostateczna Sprawiedliwość"), upgrade("krok_w_swiatlosci", "Krok w Światłości"), upgrade("potrojenie", "Potrojenie"))),
                MULTI_COMPONENT_PERCENT_NEEDS_VERIFICATION));
        put(skills, skill("forteca", "Forteca", SPECIAL_POWERS_PDF, "moce_specjalne", DEFENSIVE, NEEDS_VERIFICATION,
                groups(SPECIAL_POWERS_PDF, "Forteca",
                        group1(upgrade("nieustepliwosc", "Nieustępliwość"), upgrade("uzycie_bez_zuzywania_zasobow", "Użycie bez Zużywania Zasobów")),
                        group2(upgrade("premia_do_obrazen_animuszu", "Premia do Obrażeń Animuszu"), upgrade("czas_dzialania", "Czas Działania")),
                        group3(upgrade("barykada", "Barykada"), upgrade("cierniowa_reduta", "Cierniowa Reduta"), upgrade("okopanie", "Okopanie"))),
                "Forteca jest wpisem umiejętności w drzewie Mocy Specjalnych; tagi opisowe z PDF: Specjalne, Defensywa, Moloch. Cierniowa Reduta jest jej ulepszeniem z grupy 3."));
        put(skills, skill("zenit", "Zenit", SPECIAL_POWERS_PDF, "moce_specjalne", componentDamagePercentRanks(Map.ofEntries(
                        Map.entry(FIRST_STRIKE_DAMAGE, damagePercentRanks(450, 495, 540, 585, 653, 697, 742, 788, 832, 900, 945, 990, 1035, 1080, 1147)),
                        Map.entry(SECOND_STRIKE_DAMAGE, damagePercentRanks(400, 440, 480, 520, 580, 620, 660, 700, 740, 800, 840, 880, 920, 960, 1020))
                )), SPECIAL, NEEDS_VERIFICATION,
                groups(SPECIAL_POWERS_PDF, "Zenit",
                        group1(upgrade("szansa_na_trafienie_krytyczne", "Szansa na Trafienie Krytyczne"), upgrade("oslabienie", "Osłabienie")),
                        group2(upgrade("nieustepliwosc", "Nieustępliwość"), upgrade("oslabienie_cooldown", "Osłabienie")),
                        group3(upgrade("empirejska_klinga", "Empirejska Klinga"), upgrade("rozdarcie", "Rozdarcie"), upgrade("homilia_stali", "Homilia Stali"))),
                LOCAL_JSON_COMPONENT_DAMAGE_RANK_TABLE));
        put(skills, skill("arbiter_sprawiedliwosci", "Arbiter Sprawiedliwości", SPECIAL_POWERS_PDF, "moce_specjalne", DAMAGE, NEEDS_VERIFICATION,
                groups(SPECIAL_POWERS_PDF, "Arbiter Sprawiedliwości",
                        group1(upgrade("szybkosc_ruchu", "Szybkość Ruchu"), upgrade("czas_dzialania", "Czas Działania")),
                        group2(upgrade("ponowne_uzycie_uderzenia_skrzydel", "Ponowne Użycie Uderzenia Skrzydeł"), upgrade("redukcja_czasu_odnowienia", "Redukcja Czasu Odnowienia")),
                        group3(upgrade("skrzydla_serafina", "Skrzydła Serafina"), upgrade("skrzydla_sprawiedliwosci", "Skrzydła Sprawiedliwości"), upgrade("boska_interwencja", "Boska Interwencja"))),
                MULTI_COMPONENT_PERCENT_NEEDS_VERIFICATION));

        return Map.copyOf(skills);
    }

    private static PaladinTreeSkill skill(String skillId,
                                          String skillName,
                                          String sourcePdf,
                                          String skillGroup,
                                          PaladinSkillTreeType type,
                                          PaladinSkillTreeStatus status,
                                          List<PaladinSkillUpgradeGroup> upgradeGroups,
                                          String notes) {
        return new PaladinTreeSkill(skillId, skillName, sourcePdf, skillGroup, type, status, upgradeGroups, notes);
    }

    private static PaladinTreeSkill skill(String skillId,
                                          String skillName,
                                          String sourcePdf,
                                          String skillGroup,
                                          DamagePercentRankTable baseDamagePercentRanks,
                                          PaladinSkillTreeType type,
                                          PaladinSkillTreeStatus status,
                                          List<PaladinSkillUpgradeGroup> upgradeGroups,
                                          String notes) {
        return new PaladinTreeSkill(skillId, skillName, sourcePdf, skillGroup, baseDamagePercentRanks, type, status, upgradeGroups, notes);
    }

    private static PaladinTreeSkill skill(String skillId,
                                          String skillName,
                                          String sourcePdf,
                                          String skillGroup,
                                          DamagePercentRankTable baseDamagePercentRanks,
                                          PaladinSkillTreeType type,
                                          PaladinSkillTreeStatus status,
                                          List<PaladinSkillUpgradeGroup> upgradeGroups,
                                          String notes,
                                          Integer faithCost,
                                          Integer faithGenerationBase,
                                          Integer faithGenerationBonusKnown) {
        return new PaladinTreeSkill(skillId, skillName, sourcePdf, skillGroup, null, null,
                baseDamagePercentRanks, DamagePercentComponentRankTable.empty(), type, status, upgradeGroups, notes,
                faithCost, faithGenerationBase, faithGenerationBonusKnown);
    }

    private static PaladinTreeSkill skill(String skillId,
                                          String skillName,
                                          String sourcePdf,
                                          String skillGroup,
                                          DamagePercentRankTable baseDamagePercentRanks,
                                          PaladinSkillTreeType type,
                                          PaladinSkillTreeStatus status,
                                          List<PaladinSkillUpgradeGroup> upgradeGroups,
                                          String notes,
                                          Integer faithCost,
                                          Integer faithGenerationBase,
                                          Integer faithGenerationBonusKnown,
                                          Integer luckyHitPercent,
                                          List<UpgradeDamageModifier> baseDescriptionModifiers) {
        return new PaladinTreeSkill(skillId, skillName, sourcePdf, skillGroup, null, null,
                baseDamagePercentRanks, DamagePercentComponentRankTable.empty(), type, status, upgradeGroups, notes,
                faithCost, faithGenerationBase, faithGenerationBonusKnown, luckyHitPercent, baseDescriptionModifiers);
    }

    private static PaladinTreeSkill skill(String skillId,
                                          String skillName,
                                          String sourcePdf,
                                          String skillGroup,
                                          DamagePercentComponentRankTable componentDamagePercentRanks,
                                          PaladinSkillTreeType type,
                                          PaladinSkillTreeStatus status,
                                          List<PaladinSkillUpgradeGroup> upgradeGroups,
                                          String notes) {
        return new PaladinTreeSkill(skillId, skillName, sourcePdf, skillGroup, componentDamagePercentRanks, type, status, upgradeGroups, notes);
    }

    private static PaladinTreeSkill skill(String skillId,
                                          String skillName,
                                          String sourcePdf,
                                          String skillGroup,
                                          Integer baseDamagePercentAtRank1,
                                          Integer baseDamagePercentAtTreeMaxRank,
                                          PaladinSkillTreeType type,
                                          PaladinSkillTreeStatus status,
                                          List<PaladinSkillUpgradeGroup> upgradeGroups,
                                          String notes) {
        return new PaladinTreeSkill(skillId, skillName, sourcePdf, skillGroup, baseDamagePercentAtRank1, baseDamagePercentAtTreeMaxRank, type, status, upgradeGroups, notes);
    }

    private static DamagePercentRankTable blessedHammerDamagePercentRanks() {
        return damagePercentRanks(115, 126, 138, 149, 167, 178, 190, 201, 213, 230, 241, 253, 264, 276, 293);
    }

    private static DamagePercentComponentRankTable componentDamagePercentRanks(Map<DamagePercentComponent, DamagePercentRankTable> tablesByComponent) {
        return DamagePercentComponentRankTable.of(tablesByComponent);
    }

    private static DamagePercentRankTable damagePercentRanks(int rank1,
                                                             int rank2,
                                                             int rank3,
                                                             int rank4,
                                                             int rank5,
                                                             int rank6,
                                                             int rank7,
                                                             int rank8,
                                                             int rank9,
                                                             int rank10,
                                                             int rank11,
                                                             int rank12,
                                                             int rank13,
                                                             int rank14,
                                                             int rank15) {
        return DamagePercentRankTable.of(Map.ofEntries(
                Map.entry(1, rank1),
                Map.entry(2, rank2),
                Map.entry(3, rank3),
                Map.entry(4, rank4),
                Map.entry(5, rank5),
                Map.entry(6, rank6),
                Map.entry(7, rank7),
                Map.entry(8, rank8),
                Map.entry(9, rank9),
                Map.entry(10, rank10),
                Map.entry(11, rank11),
                Map.entry(12, rank12),
                Map.entry(13, rank13),
                Map.entry(14, rank14),
                Map.entry(15, rank15)
        ));
    }

    private static List<PaladinSkillUpgradeGroup> groups(String sourcePdf,
                                                         String skillName,
                                                         List<PaladinSkillUpgrade> group1,
                                                         List<PaladinSkillUpgrade> group2,
                                                         List<PaladinSkillUpgrade> group3) {
        return List.of(
                new PaladinSkillUpgradeGroup("grupa_1", "Grupa 1", withSource(sourcePdf, skillName, "grupa_1", group1)),
                new PaladinSkillUpgradeGroup("grupa_2", "Grupa 2", withSource(sourcePdf, skillName, "grupa_2", group2)),
                new PaladinSkillUpgradeGroup("grupa_3", "Grupa 3", withSource(sourcePdf, skillName, "grupa_3", group3))
        );
    }

    private static List<PaladinSkillUpgrade> withSource(String sourcePdf,
                                                        String skillName,
                                                        String groupId,
                                                        List<PaladinSkillUpgrade> upgrades) {
        return upgrades.stream()
                .map(upgrade -> new PaladinSkillUpgrade(
                        upgrade.getId(),
                        upgrade.getName(),
                        upgrade.getStatus(),
                        sourcePdf + " | " + skillName + " | " + groupId
                ))
                .toList();
    }

    private static List<PaladinSkillUpgrade> group1(PaladinSkillUpgrade... upgrades) {
        return List.of(upgrades);
    }

    private static List<PaladinSkillUpgrade> group2(PaladinSkillUpgrade... upgrades) {
        return List.of(upgrades);
    }

    private static List<PaladinSkillUpgrade> group3(PaladinSkillUpgrade... upgrades) {
        return List.of(upgrades);
    }

    private static PaladinSkillUpgrade upgrade(String id, String name) {
        return new PaladinSkillUpgrade(id, name, NEEDS_VERIFICATION, "Źródło zostanie uzupełnione przy budowie grupy.");
    }

    private static PaladinSkillUpgrade unsupportedUpgrade(String id, String name) {
        return new PaladinSkillUpgrade(id, name, UNSUPPORTED, "PDF nie podaje pełnej nazwy albo opisu wariantu.");
    }

    private static UpgradeDamageModifier baseUtilityModifier(String skillName,
                                                             String effectName,
                                                             String value,
                                                             String condition,
                                                             String notes) {
        return new UpgradeDamageModifier(
                "efekt_bazowy",
                effectName,
                UpgradeDamageSafety.NO,
                UpgradeDamageModifierType.DEFENSE_OR_UTILITY,
                value,
                UpgradeDamageValueKind.TEXT_ONLY,
                condition,
                false,
                "utility",
                false,
                UpgradeDamageSafety.YES,
                UpgradeDamageSafety.NO,
                "Efekt bazowy " + skillName + ": " + notes
        );
    }

    private static void put(Map<String, PaladinTreeSkill> skills, PaladinTreeSkill skill) {
        PaladinTreeSkill previous = skills.put(skill.getSkillId(), skill);
        if (previous != null) {
            throw new IllegalStateException("Zduplikowany skill Paladyna: " + skill.getSkillId());
        }
    }
}
