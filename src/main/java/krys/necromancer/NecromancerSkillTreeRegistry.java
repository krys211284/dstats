package krys.necromancer;

import krys.ranking.CharacterSkillTreeRegistry;
import krys.ranking.PlayableClass;
import krys.skilltree.CharacterTreeSkill;
import krys.skilltree.DamagePercentComponent;
import krys.skilltree.DamagePercentComponentRankTable;
import krys.skilltree.DamagePercentRankTable;
import krys.skilltree.SkillResourceData;
import krys.skilltree.SkillChargeData;
import krys.skilltree.SkillActivationRequirement;
import krys.skilltree.SkillActivationConsumptionMode;
import krys.skilltree.SkillResourceGenerationFrequency;
import krys.skilltree.SkillResourceType;
import krys.skilltree.SkillTreeStatus;
import krys.skilltree.SkillTreeType;
import krys.skilltree.SkillUpgrade;
import krys.skilltree.SkillUpgradeGroup;
import krys.skilltree.SourceSkillCategory;
import krys.skilltree.UpgradeDamageImpact;
import krys.skilltree.UpgradeDamageImpactType;
import krys.skilltree.UpgradeDamageModifier;
import krys.skilltree.UpgradeDamageModifierType;
import krys.skilltree.UpgradeDamageSafety;
import krys.skilltree.UpgradeDamageValueKind;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static krys.skilltree.UpgradeDamageModifierType.*;
import static krys.skilltree.UpgradeDamageValueKind.*;
import static krys.skilltree.DamagePercentComponent.*;

/** Źródłowy katalog wszystkich 23 aktywnych skilli Nekromanty, Sezon 15 / 3.2.1; nie stanowi modelu runtime. */
public final class NecromancerSkillTreeRegistry {
    public static final SkillResourceType ESSENCE = new SkillResourceType(
            "essence", "Koszt Esencji", "Generowanie Esencji", "pkt. esencji");
    public static final SourceSkillCategory BASIC = new SourceSkillCategory("BASIC", "Podstawowe", 0);
    public static final SourceSkillCategory CORE = new SourceSkillCategory("CORE", "Główne", 1);
    public static final SourceSkillCategory CORPSE = new SourceSkillCategory("CORPSE", "Zwłoki", 2);
    public static final SourceSkillCategory MACABRE = new SourceSkillCategory("MACABRE", "Makabra", 3);
    public static final SourceSkillCategory CURSE = new SourceSkillCategory("CURSE", "Klątwy", 4);
    public static final SourceSkillCategory ULTIMATE = new SourceSkillCategory("ULTIMATE", "Moce Specjalne", 5);
    private static final SkillActivationRequirement CONSUMED_CORPSE = new SkillActivationRequirement(
            "corpse", "Zwłoki", 1, SkillActivationConsumptionMode.CONSUMED_ON_USE);
    public static final String MAX_VIEW_SOURCE = "https://d4builds.gg/database/class-skills/";
    private static final String MAX_VIEW_NOTE = "Sezon 15, patch 3.2.1. D4Builds pokazuje widok max-rank bez jawnej etykiety Rank 15; "
            + "projekt zapisuje ten widok pod rangą 15. Rangi 2..14 pozostają nieznane; wymagana weryfikacja. ";
    private static final String LUCKY_HIT_CONFLICT = "Konflikt źródeł: PL D4Guides i bieżący D4Builds podają +30% Lucky Hit, "
            + "natomiast EN D4Guides usuwa opis w changelogu 3.2.1. Zachowano node i +30% do weryfikacji.";
    private static final CharacterSkillTreeRegistry REGISTRY = createRegistry();

    private NecromancerSkillTreeRegistry() { }

    public static CharacterSkillTreeRegistry asCharacterRegistry() {
        return REGISTRY;
    }

    private static CharacterSkillTreeRegistry createRegistry() {
        return new CharacterSkillTreeRegistry(PlayableClass.NECROMANCER, "NecromancerSkillTreeRegistry",
                List.of(boneSplinters(), reap(), decompose(), hemorrhage(), boneSpear(), blight(), sever(),
                        bloodSurge(), bloodLance(), skeletonMage(), corpseExplosion(), corpseTendrils(), skeletonWarrior(),
                        bloodMist(), bonePrison(), boneSpirit(), golem(), ironMaiden(), decrepify(),
                        armyOfTheDead(), bloodWave(), boneStorm(), soulrift()), ESSENCE,
                Map.of("basic", "Podstawowe / Basic", "core", "Główne / Core", "corpse", "Zwłoki / Corpse", "macabre", "Makabra / Macabre", "curse", "Klątwy / Curse", "ultimate", "Moce Specjalne / Ultimate"),
                Set.of("BASIC", "CORE", "CORPSE", "MACABRE", "CURSE", "ULTIMATE"),
                Set.of("BASIC", "CORE", "CORPSE", "MACABRE", "CURSE", "ULTIMATE", "PROFANE", "BONE", "DARKNESS", "BLOOD", "CHANNELED", "PHYSICAL", "SHADOW", "SUMMON", "MINION"),
                "Kościane Odłamki, BONE, Dorzynki");
    }

    private static CharacterTreeSkill boneSplinters() {
        return skill("kosciane_odlamki", "Kościane Odłamki", "bone-splinters", 30, 77, 30,
                new SkillResourceData(null, 10, 5), Set.of("BASIC", "BONE", "PHYSICAL"),
                "Bone Splinters: bazowo 3 odłamki po 30% Physical na R1, po 77% w widoku max; nie sumujemy odłamków.",
                List.of(
                        node(1, "pociski", "Pociski", ADDITIONAL_HIT_OR_STRIKE, HIT_COUNT, "+1 odłamek",
                                "Po wybraniu ulepszenia", "Odłamek", true, false, null,
                                "Wystrzeliwuje 1 dodatkowy odłamek."),
                        node(1, "generowanie_esencji", "Generowanie Esencji", RESOURCE_OR_COST, TEXT_ONLY, "+5 pkt. esencji",
                                "Po wybraniu ulepszenia", "Esencja", false, false, null, "Generuje dodatkowo 5 Esencji."),
                        node(2, "odsloniecie", "Odsłonięcie", STATUS_DAMAGE_ENABLER, SECONDS, "4 s",
                                "Trafienie", "Vulnerable / Odsłonięcie", false, false, null, "Wywołuje Odsłonięcie na 4 s."),
                        node(2, "animusz", "Animusz", DEFENSE_OR_UTILITY, HIT_COUNT, "+1 kumulacja",
                                "Użycie", "Animusz / Resolve", false, false, null, "Przyznaje 1 kumulację Animuszu."),
                        node(3, "rykoszetujace_kolce", "Rykoszetujące Kolce", RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "45%",
                                "Każdy odłamek na R1", "Odłamek", false, true, 45,
                                "R1: 45% na odłamek; max-view: 115%. Odłamki odbijają się od ścian i lecą dalej."),
                        node(3, "krwawe_odlamki", "Krwawe Odłamki", RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "30%",
                                "Każdy odłamek na R1", "Odłamek Blood / Physical", false, true, 30,
                                "Staje się umiejętnością Krwi; R1: 30%, przebija 1 raz, każde trafienie leczy 2.0% maksymalnego zdrowia."),
                        extra(node(3, "poszukiwacze_cienia", "Poszukiwacze Cienia", FLAT_COMPONENT_PERCENT, COMPONENT_PERCENT, "7.5%",
                                "Impuls po trafieniu odłamkiem, R1", "Dodatkowy impuls Shadow", true, true, null,
                                "Staje się Darkness; odłamki szukają wrogów. R1: 30% Shadow i dodatkowy impuls 7.5%; max-view: 77% + 20%."),
                                RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "30%", "Odłamek Shadow", false, true,
                                "Główne trafienie R1: 30% Shadow; widok max: 77%.")));
    }

    private static CharacterTreeSkill reap() {
        return skill("zniwa", "Żniwa", "reap", 50, 128, 30, new SkillResourceData(null, 15, null),
                Set.of("BASIC", "DARKNESS", "SHADOW"), "Reap: R1 50% Shadow; widok max 128%.",
                List.of(
                        node(1, "szansa_krytyczna", "Szansa na Trafienie Krytyczne", STATUS_DAMAGE_ENABLER, PERCENT_PLUS, "10%[+]",
                                "Po wybraniu ulepszenia", "Szansa na trafienie krytyczne", false, false, null,
                                "D4Guides EN/PL podaje R1 +10%. D4Builds wyświetla +20%; rozbieżność skalowania/źródła wymaga weryfikacji. Nie zastępujemy R1."),
                        node(1, "generowanie_esencji", "Generowanie Esencji", RESOURCE_OR_COST, TEXT_ONLY, "+2 pkt. esencji za wroga",
                                "Za każdego trafionego wroga", "Esencja", false, false, null,
                                "Warunkowo +2 Esencji za każdego trafionego wroga; nie jest stałym dodatkiem do bazowego generowania."),
                        node(2, "zajadlosc", "Zajadłość", DEFENSE_OR_UTILITY, HIT_COUNT, "+1 kumulacja",
                                "Użycie", "Zajadłość / Ferocity", false, false, null, "Przyznaje 1 kumulację Zajadłości."),
                        node(2, "generowanie_zwlok", "Generowanie Zwłok", DEFENSE_OR_UTILITY, TEXT_ONLY, "Zwłoki",
                                "Pierwszy trafiony wróg", "Corpse / Zwłoki", false, false, null, "Tworzy zwłoki pod pierwszym trafionym wrogiem."),
                        extra(node(3, "przejmujacy_chlod", "Przejmujący Chłód", RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "50%",
                                "Trafienie R1", "Trafienie Cold", false, true, 50,
                                "R1: 50% Cold, wyziębienie 15%, +30% szybkości użycia. D4Builds max-view: 128% i wyższe wyziębienie; nie są wartościami R1."),
                                CAST_SPEED_OR_COOLDOWN, PERCENT_PLUS, "30%[+]", "Szybkość użycia", false, false,
                                "Po wybraniu Przejmującego Chłodu szybkość użycia wzrasta o 30%."),
                        node(3, "selekcja_naturalna", "Selekcja Naturalna", RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "62.5%",
                                "Trafienie R1", "Trafienie Blood / Physical", false, true, null,
                                "Staje się Blood; dokładne źródłowe R1 z baseline N1: 62.5% Physical. D4Guides EN wyświetla zaokrąglone 63%; "
                                        + "D4Builds max-view 161%. Egzekucja; po zabiciu 50% szans na Krwawą Kulę."),
                        extra(node(3, "dorzynki", "Dorzynki", ADDITIONAL_HIT_OR_STRIKE, COMPONENT_PERCENT, "25%",
                                "Drugi zamach, R1", "Drugi zamach", true, true, 25,
                                "Drugi zamach R1: 25% i 7.5 Esencji. D4Builds max-view: 64% oraz zaokrąglone 8 Esencji; zachowano dokładne 7.5."),
                                RESOURCE_OR_COST, TEXT_ONLY, "7.5 pkt. esencji", "Esencja z drugiego zamachu", false, false,
                                "Po wybraniu Dorzynek drugi zamach generuje 7.5 Esencji; nie jest stałym dodatkiem bazowej umiejętności.")));
    }

    private static CharacterTreeSkill decompose() {
        return skill("rozklad", "Rozkład", "decompose", 150, 383, 40,
                new SkillResourceData(null, new BigDecimal("12"), new BigDecimal("16"), null,
                        SkillResourceGenerationFrequency.PER_SECOND), Set.of("BASIC", "DARKNESS", "CHANNELED", "SHADOW"),
                "Decompose: R1 150% Corrupting / Shadow na sekundę, max-view 383% na sekundę. Tworzy możliwe do wykorzystania zwłoki co 1 s. "
                        + "D4Guides podaje zakres 12–16 Esencji / sek.; D4Builds pokazuje pojedyncze 13. Zachowano zakres.",
                List.of(
                        node(1, "premia_do_obrazen", "Premia do Obrażeń", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "50%[x]",
                                "Po wybraniu ulepszenia", "Obrażenia Rozkładu", false, false, 50, "Zwiększa obrażenia Rozkładu o 50%[x]."),
                        node(1, "bariera", "Bariera", DEFENSE_OR_UTILITY, PERCENT_PLUS, "10% maksymalnego zdrowia",
                                "Co sekundę", "Bariera", false, false, null, "Co sekundę daje barierę równą 10% maksymalnego zdrowia na 6 s."),
                        node(2, "ograniczenie_kontroli", "Ograniczenie Kontroli", STATUS_DAMAGE_ENABLER, PERCENT_PLUS, "50%",
                                "Trafiony wróg", "Spowolnienie", false, false, null, "Spowalnia o 50%."),
                        conflictedLuckyHit(),
                        node(3, "targanie", "Targanie", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Bone / Physical",
                                "Wrogowie między graczem a celem", "Trafienie Physical", false, false, null,
                                "Staje się umiejętnością Kości z obrażeniami Physical; trafia wszystkich wrogów między graczem a głównym celem."),
                        node(3, "gnijacy_rozprysk", "Gnijący Rozprysk", FLAT_COMPONENT_PERCENT, COMPONENT_PERCENT, "113%",
                                "Dodatkowy impuls co sekundę, R1", "Impuls wokół celu", true, true, 113,
                                "Dodatkowy impuls co sekundę: R1 113%, max-view 288%; trafia cel i pobliskich wrogów."),
                        node(3, "prochnienie", "Próchnienie", ADDITIONAL_HIT_OR_STRIKE, HIT_COUNT, "3 dodatkowych wrogów",
                                "Dostępność kolejnych celów", "Łańcuch", false, false, null,
                                "Łańcuch do 3 dodatkowych wrogów; opis nie zmienia domyślnego modelu single target.")));
    }

    private static CharacterTreeSkill hemorrhage() {
        return skill("krwotok", "Krwotok", "hemorrhage", 80, 204, 50, new SkillResourceData(null, 13, null),
                Set.of("BASIC", "BLOOD", "PHYSICAL"), "Hemorrhage: R1 80% Physical, max-view 204%; bazowo 20% szans na Krwawą Kulę.",
                List.of(
                        node(1, "szybkosc_uzycia", "Szybkość Użycia", CAST_SPEED_OR_COOLDOWN, PERCENT_PLUS, "20%[+]",
                                "Po wybraniu ulepszenia", "Szybkość użycia", false, false, null, "Szybkość użycia wzrasta o 20%."),
                        node(1, "krwawa_kula", "Krwawa Kula", DEFENSE_OR_UTILITY, PERCENT_PLUS, "+30%",
                                "Po wybraniu ulepszenia", "Szansa na Krwawą Kulę", false, false, null, "Dodatkowo 30% szans na Krwawą Kulę."),
                        node(2, "przytloczenie", "Przytłoczenie", STATUS_DAMAGE_ENABLER, HIT_COUNT, "+1 kumulacja",
                                "Użycie", "Overpower / Przytłoczenie", false, false, null, "Przyznaje 1 kumulację Przytłoczenia."),
                        node(2, "oslabienie", "Osłabienie", STATUS_DAMAGE_ENABLER, SECONDS, "4 s",
                                "Trafiony wróg", "Weaken / Osłabienie", false, false, null, "Osłabia wrogów na 4 s."),
                        node(3, "krwawy_wrzod", "Krwawy Wrzód", FLAT_COMPONENT_PERCENT, COMPONENT_PERCENT, "40%",
                                "Wrogowie wokół celu, R1", "Dodatkowe obrażenia obszarowe", true, true, 40,
                                "Dodatkowe obrażenia wokół celu: R1 40%, max-view 102%; nie sumujemy ich z trafieniem w modelu single target."),
                        node(3, "zmrozenie_krwi", "Zmrożenie Krwi", RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "80%",
                                "Trafienie R1; zamrożenie tego samego wroga raz na 6 s", "Trafienie Cold", false, true, 80,
                                "R1 80% Cold, max-view 204%. Zamraża na 3 s, tego samego wroga najwyżej raz na 6 s; "
                                        + "trafienie zamrożonego celu daje dodatkowo 50% szans na Krwawą Kulę."),
                        extra(node(3, "rozdarcie_dusz", "Rozdarcie Dusz", FLAT_COMPONENT_PERCENT, COMPONENT_PERCENT, "120%",
                                "Zamiast Krwawej Kuli, R1", "Rozszalały / Volatile Skeleton", true, true, 120,
                                "Staje się Darkness: R1 80% Shadow, max-view 204%. Zamiast kuli przywołuje Rozszalały Szkielet; "
                                        + "szkielet R1 120%, max-view 306%."),
                                RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "80%", "Trafienie Shadow", false, true,
                                "Główne trafienie R1: 80% Shadow; widok max: 204%.")));
    }

    private static CharacterTreeSkill boneSpear() {
        return coreSkill("wlocznia_z_kosci", "Włócznia z Kości", "bone-spear", 160, 408, 25, 50,
                Set.of("CORE", "BONE", "PHYSICAL"), "Bone Spear: R1 160% Physical, max-view 408%; przebija wrogów.",
                List.of(
                        node(2, "premia_do_obrazen_od_przebicia", "Premia do Obrażeń od Przebicia", MULTIPLICATIVE_DAMAGE_PERCENT,
                                PERCENT_X, "10%[x]", "Za każdego przeszytego wroga; maksymalnie 50%[x]", "Trafienie", false, false, 10,
                                "Premia 10%[x] za przeszytego wroga, do 50%[x]; zależy od liczby wrogów."),
                        node(2, "animusz", "Animusz", STATUS_DAMAGE_ENABLER, PERCENT_X, "5%[x]",
                                "Za każdą kumulację Animuszu / Resolve; maksymalnie 40%[x]", "Obrażenia krytyczne", false, false, null,
                                "Obrażenia krytyczne +5%[x] za kumulację Animuszu, do 40%[x]; nie zwiększa bezwarunkowo bazowego trafienia."),
                        extra(node(3, "rozszczepianie_cieni", "Rozszczepianie Cieni", RANK_SCALING_COMPONENT_PERCENT,
                                COMPONENT_PERCENT, "163%", "Wariant Darkness / Shadow, R1", "Włócznia Shadow", false, true, 163,
                                "Staje się Darkness / Shadow: R1 163%, max-view 416%. Przy trafieniu wroga lub ściany rozszczepia się "
                                        + "na 2 przeszywające włócznie, najwyżej 2 razy. Bazowe 160% / 408% pozostaje bez zmian."),
                                ADDITIONAL_HIT_OR_STRIKE, HIT_COUNT, "2 włócznie", "Rozszczepienie", true, false,
                                "Rozszczepienie zamiast przebijania; najwyżej 2 rozszczepienia przy trafieniu wroga lub ściany."),
                        extraWhen(node(3, "wlocznia_krwi", "Włócznia Krwi", RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT,
                                "160%", "Wariant Blood, R1", "Główna włócznia Blood", false, true, 160,
                                "Staje się Blood: R1 160%, max-view 408%. Zużywa pobliskie zwłoki, tworząc drugą włócznię "
                                        + "o 100% normalnych obrażeń; jest to współczynnik względny, bez osobnej absolutnej tabeli."),
                                ADDITIONAL_HIT_OR_STRIKE, TEXT_ONLY, "100% normalnych obrażeń", "Druga włócznia względem normalnej",
                                true, false, "Wybrany wariant i dostępne pobliskie zwłoki", "Druga włócznia powstaje ze zużytych zwłok."),
                        node(3, "kosciane_kolce", "Kościane Kolce", ADDITIONAL_HIT_OR_STRIKE, COMPONENT_PERCENT, "53%",
                                "Po zniszczeniu włóczni; każdy z 3 odłamków, R1", "Przeszywający odłamek", true, true, 53,
                                "Po zniszczeniu powstają 3 przeszywające odłamki: każdy R1 53%, max-view 135%; bez sumowania."),
                        node(1, "premia_do_obrazen_od_pierwszego_trafienia", "Premia do Obrażeń od Pierwszego Trafienia",
                                MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "40%[x]", "Pierwszy trafiony wróg", "Trafienie", false, false, 40,
                                "Pierwszy trafiony wróg otrzymuje obrażenia zwiększone o 40%[x]."),
                        node(1, "redukcja_kosztu", "Redukcja Kosztu", RESOURCE_OR_COST, TEXT_ONLY, "-5 pkt. esencji",
                                "Po wybraniu ulepszenia", "Koszt Esencji", false, false, null,
                                "Zmniejsza koszt o 5; bazowy koszt pozostaje 25.")));
    }

    private static CharacterTreeSkill blight() {
        String conflict = "Konflikt źródeł: PL D4Guides i bieżący D4Builds podają +20% Lucky Hit, "
                + "natomiast changelog EN D4Guides 3.2.1 pokazuje usunięcie tej właściwości. Zachowano node +20% do ręcznej weryfikacji.";
        Node luckyHit = new Node(new SkillUpgrade("szansa_szczesliwego_trafu", "Szansa na Szczęśliwy Traf",
                SkillTreeStatus.NEEDS_VERIFICATION, conflict), List.of(modifier("grupa_2", "Szansa na Szczęśliwy Traf",
                UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW, PERCENT_PLUS, "+20%", "Konflikt źródeł 3.2.1", "Lucky Hit",
                false, false, UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, conflict)),
                new UpgradeDamageImpact("Szansa na Szczęśliwy Traf", "grupa_2", UpgradeDamageImpactType.NEEDS_VERIFICATION, conflict, null));
        return coreSkill("mor", "Mór", "blight", null, null, 20, 40, Set.of("CORE", "DARKNESS", "SHADOW"),
                "Blight: trafienie R1 40%, max-view 102%, oraz skażony obszar: łącznie R1 210%, max-view 536% "
                        + "Corrupting / Shadow w ciągu 6 s. To procent za cały okres, nie na sekundę. " + conflict,
                List.of(
                        node(2, "premia_do_obrazen_za_ograniczenie_kontroli", "Premia do Obrażeń za Ograniczenie Kontroli",
                                MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "30%[x]", "Wróg z ograniczeniem kontroli / Crowd Controlled",
                                "Obrażenia Moru", false, false, 30, "Obrażenia +30%[x] przeciw wrogom z ograniczeniem kontroli."),
                        luckyHit,
                        node(3, "niestabilna_krew", "Niestabilna Krew", FLAT_COMPONENT_PERCENT, COMPONENT_PERCENT, "315%",
                                "Wariant Blood / Physical, R1; natychmiastowy wybuch skażonego obszaru", "Wybuch obszaru", true, true, 315,
                                "Staje się Blood / Physical; skażony obszar natychmiast wybucha: R1 315%, max-view 803%."),
                        extra(node(3, "wir", "Wir", RESOURCE_OR_COST, TEXT_ONLY, "-10 pkt. esencji", "Po wybraniu wariantu",
                                "Koszt Esencji", false, false, null, "Staje się Cold / Frostbite; koszt -10, przyciąga / Pull "
                                        + "i wyziębia o 15% co sekundę. Bazowy koszt pozostaje 20."),
                                STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Cold / Frostbite; Pull; Chill 15% co sekundę", "Kontrola obszaru", false, false,
                                "Przyciąganie i wyziębienie są efektami statusowymi, bez nowego potwierdzonego procentu obrażeń."),
                        extraWhen(node(3, "przeszywajacy_mrok", "Przeszywający Mrok", ADDITIONAL_HIT_OR_STRIKE, HIT_COUNT,
                                "+1 przebicie", "Dostępność kolejnego celu", "Dodatkowy skażony obszar", true, false, null,
                                "Przebija 1 dodatkowy raz i może stworzyć kolejny skażony obszar; obrażenia przeciw bossom +75%[x]."),
                                MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "75%[x]", "Obrażenia Moru", false, false, "Cel jest bossem",
                                "Premia +75%[x] dotyczy wyłącznie bossów."),
                        node(1, "premia_do_rozmiaru", "Premia do Rozmiaru", DEFENSE_OR_UTILITY, PERCENT_PLUS, "+35%",
                                "Po wybraniu ulepszenia", "Rozmiar obszaru", false, false, null, "Rozmiar skażonego obszaru +35%."),
                        node(1, "premia_do_obrazen_obszarowych", "Premia do Obrażeń Obszarowych", MULTIPLICATIVE_DAMAGE_PERCENT,
                                PERCENT_X, "40%[x]", "Skażony obszar", "DAMAGE_OVER_DURATION", false, false, 40,
                                "Skażony obszar zadaje obrażenia zwiększone o 40%[x].")),
                DamagePercentComponentRankTable.of(Map.of(PRIMARY_DAMAGE, ranks(40, 102), DAMAGE_OVER_DURATION, ranks(210, 536)),
                        Map.of(DAMAGE_OVER_DURATION, 6)), List.of());
    }

    private static CharacterTreeSkill sever() {
        return coreSkill("rozdarcie", "Rozdarcie", "sever", null, null, 20, 40, Set.of("CORE", "DARKNESS", "SHADOW"),
                "Sever: widmo zadaje R1 60%, max-view 153% podczas ruchu oraz R1 100%, max-view 255% zamachem kosy; "
                        + "dwa oddzielne komponenty Shadow, bez sumowania.",
                List.of(
                        extraWhen(node(1, "premia_do_obrazen", "Premia do Obrażeń", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X,
                                "10%[x]", "Po wybraniu ulepszenia", "Obrażenia Rozdarcia", false, false, 10,
                                "Obrażenia +10%[x] oraz dodatkowo +1%[x] za każdego posiadanego sługę; druga premia jest warunkowa."),
                                MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "1%[x]", "Obrażenia Rozdarcia", false, false,
                                "Za każdego posiadanego sługę / Minion", "Zależy od faktycznej liczby sług; nie jest stałym mnożnikiem."),
                        node(1, "redukcja_kosztu", "Redukcja Kosztu", RESOURCE_OR_COST, TEXT_ONLY, "-4 pkt. esencji",
                                "Po wybraniu ulepszenia", "Koszt Esencji", false, false, null, "Koszt -4; bazowy koszt pozostaje 20."),
                        node(3, "zniwiarski_lotos", "Żniwiarski Lotos", ADDITIONAL_HIT_OR_STRIKE, COMPONENT_PERCENT, "60%",
                                "W najdalszym punkcie; każde z 3 dodatkowych widm, R1", "Dodatkowe widmo", true, true, 60,
                                "Tworzy 3 dodatkowe widma w najdalszym punkcie: każde R1 60%, max-view 153%; bez sumowania."),
                        node(3, "nieublagany_zniwiarz", "Nieubłagany Żniwiarz", DEFENSE_OR_UTILITY, TEXT_ONLY, "Mobility; doskok",
                                "Po wybraniu wariantu", "Mobilność", false, false, null,
                                "Staje się również Mobility; doskok do przodu nie ma potwierdzonego osobnego współczynnika obrażeń."),
                        extra(node(3, "chlodna_pogon", "Chłodna Pogoń", ADDITIONAL_HIT_OR_STRIKE, TEXT_ONLY, "Ponowny atak na innego wroga",
                                "Dostępny inny wróg; nie zakładamy powtórnego trafienia tego samego celu", "Warunkowy dodatkowy atak", true, false, null,
                                "Staje się Cold, wyziębia o 15%, wyszukuje innego wroga i atakuje ponownie; brak automatycznego dodatkowego hitu single target."),
                                STATUS_DAMAGE_ENABLER, PERCENT_PLUS, "15%", "Wyziębienie / Chill", false, false,
                                "Wariant Cold wyziębia o 15%."),
                        node(2, "ograniczanie_kontroli_i_generowanie_zwlok", "Ograniczanie Kontroli i Generowanie Zwłok",
                                STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Slow 40% przez 3 s; Zwłoki", "Trafiony wróg", "Spowolnienie i Corpse",
                                false, false, null, "Spowalnia o 40% na 3 s i tworzy zwłoki."),
                        node(2, "zajadlosc", "Zajadłość", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "7%[x]",
                                "Za każdą kumulację Zajadłości / Ferocity", "Obrażenia Rozdarcia", false, false, 7,
                                "Obrażenia +7%[x] za kumulację Zajadłości; zależy od statusu.")),
                DamagePercentComponentRankTable.of(Map.of(FIRST_STRIKE_DAMAGE, ranks(60, 153), SECOND_STRIKE_DAMAGE, ranks(100, 255))), List.of());
    }

    private static CharacterTreeSkill bloodSurge() {
        var baseNovaBonus = modifier("efekt_bazowy", "Premia bazowej nowy", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X,
                "5%[x]", "Za każdego wroga objętego drenażem; maksymalnie 50%[x]", "BURST_DAMAGE", false, false,
                UpgradeDamageSafety.YES, "Bazowa nova zyskuje +5%[x] za wroga objętego drenażem, do +50%[x]; "
                        + "nie jest bezwarunkowym mnożnikiem ani dodatkowym ulepszeniem.");
        return coreSkill("naplyw_krwi", "Napływ Krwi", "blood-surge", null, null, 30, 12, Set.of("CORE", "BLOOD", "PHYSICAL"),
                "Blood Surge: drenaż R1 20%, max-view 51%; nova R1 125%, max-view 319% Physical. "
                        + "Bazowa nova: +5%[x] za wroga objętego drenażem, maksymalnie +50%[x]; premia warunkowa, bez automatycznego stosowania.",
                List.of(
                        node(2, "przytloczenie", "Przytłoczenie", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "50%[x]",
                                "Zużycie 2 kumulacji Przytłoczenia / Overpower", "Obrażenia i rozmiar nowy", false, false, 50,
                                "Zużywa 2 kumulacje Przytłoczenia: rozmiar +25%, obrażenia +50%[x]."),
                        node(2, "oslabienie", "Osłabienie", STATUS_DAMAGE_ENABLER, SECONDS, "4 s", "Trafiony wróg", "Weaken",
                                false, false, null, "Osłabia wrogów na 4 s."),
                        extraWhen(node(3, "igly", "Igły", ADDITIONAL_HIT_OR_STRIKE, COMPONENT_PERCENT, "60%",
                                "Wariant Bone zamiast nowy; każdy z 10 odłamków, R1", "Przeszywający odłamek Bone", true, true, 60,
                                "Staje się Bone: zamiast nowy 10 przeszywających odłamków, każdy R1 60%, max-view 153%. "
                                        + "Dodatkowo +1 odłamek za wroga objętego drenażem, najwyżej +5. Bez 10×60 ani 15×60 w tabeli bazowej."),
                                ADDITIONAL_HIT_OR_STRIKE, HIT_COUNT, "+1 odłamek", "Dodatkowy odłamek Bone", true, false,
                                "Za wroga objętego drenażem; maksymalnie +5 odłamków", "Liczba dodatkowych odłamków zależy od liczby wrogów."),
                        extraWhen(node(3, "ty_i_jaka_armia", "Ty i Jaka Armia?", FLAT_COMPONENT_PERCENT, COMPONENT_PERCENT, "20%",
                                "Aktywni słudzy; każda mała nova, R1", "Mała nova na słudze", true, true, 20,
                                "Małe novy na sługach: każda R1 20%, max-view 51%; +5%[x] za wroga objętego drenażem, do +50%[x]. "
                                        + "Nie mnożymy przez liczbę sług ani wrogów."),
                                MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "5%[x]", "Mała nova na słudze", false, false,
                                "Za wroga objętego drenażem; maksymalnie 50%[x]", "Warunkowa premia małych nov; nie jest zawsze aktywna."),
                        node(3, "krwawa_jatka", "Krwawa Jatka", FLAT_COMPONENT_PERCENT, COMPONENT_PERCENT, "125%",
                                "Echo nowy po opóźnieniu, R1", "Dodatkowa nova", true, true, 125,
                                "Nova powtarza się po opóźnieniu: R1 125%, max-view 319%; oddzielny dodatkowy komponent."),
                        node(1, "umocnienie", "Umocnienie", DEFENSE_OR_UTILITY, PERCENT_PLUS, "2% maksymalnego zdrowia",
                                "Za wroga objętego drenażem; maksymalnie 10%", "Fortify", false, false, null,
                                "Umocnienie 2% maksymalnego zdrowia za wroga objętego drenażem, do 10%."),
                        node(1, "premia_do_obrazen", "Premia do Obrażeń", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "25%[x]",
                                "Gracz w stanie Zdrowia / Healthy", "Obrażenia Napływu Krwi", false, false, 25,
                                "Obrażenia +25%[x], gdy gracz jest w stanie Zdrowia.")),
                DamagePercentComponentRankTable.of(Map.of(PRIMARY_DAMAGE, ranks(20, 51), BURST_DAMAGE, ranks(125, 319))), List.of(baseNovaBonus));
    }

    private static CharacterTreeSkill bloodLance() {
        return coreSkill("krwawa_lanca", "Krwawa Lanca", "blood-lance", 160, 408, 15, 50, Set.of("CORE", "BLOOD", "PHYSICAL"),
                "Blood Lance: R1 160% Physical, max-view 408%. Odbija się do innego wroga; przebicie / lanced trwa 3 s. "
                        + "Trafienie zadaje obrażenia również innym przebitym wrogom, bez mnożnika liczby celów single target.",
                List.of(
                        node(2, "przytloczenie", "Przytłoczenie", ADDITIONAL_HIT_OR_STRIKE, TEXT_ONLY, "50% normalnych obrażeń",
                                "Zużycie 2 kumulacji Przytłoczenia / Overpower", "Dodatkowa lanca względem normalnej", true, false, null,
                                "Zużywa 2 kumulacje Przytłoczenia i tworzy dodatkową lancę o 50% normalnych obrażeń; współczynnik względny, nie procent obrażeń broni."),
                        node(2, "redukcja_kosztu", "Redukcja Kosztu", RESOURCE_OR_COST, TEXT_ONLY, "-3 pkt. esencji",
                                "Po wybraniu ulepszenia", "Koszt Esencji", false, false, null, "Koszt -3; bazowy koszt pozostaje 15."),
                        node(1, "umocnienie", "Umocnienie", DEFENSE_OR_UTILITY, PERCENT_PLUS, "4% maksymalnego zdrowia",
                                "Trafienie już przebitego / lanced wroga", "Fortify", false, false, null,
                                "Umocnienie 4% maksymalnego zdrowia przy trafieniu już przebitego wroga."),
                        node(1, "rykoszet", "Rykoszet", ADDITIONAL_HIT_OR_STRIKE, HIT_COUNT, "+1 wróg",
                                "Dostępny kolejny wróg", "Rykoszet", true, false, null, "Odbija się do 1 dodatkowego wroga."),
                        node(3, "ropiejaca_rana", "Ropiejąca Rana", DAMAGE_OVER_TIME, COMPONENT_PERCENT, "100%",
                                "Wariant Darkness / Shadow; na sekundę, R1", "Corrupting / Shadow na sekundę", true, true, 100,
                                "Staje się Darkness / Shadow; wrogowie pozostają stale przebici. Corrupting: R1 100% na sekundę, "
                                        + "max-view 255% na sekundę. Patch 3.2.1 pozwala na najwyżej 5 kumulacji; nie stosujemy ich automatycznie."),
                        node(3, "okrwawione_kolce", "Okrwawione Kolce", ADDITIONAL_HIT_OR_STRIKE, TEXT_ONLY, "125% normalnych obrażeń",
                                "Co trzecie użycie tworzy Krwawą Kulę; użycie może zebrać pobliską kulę", "Dodatkowa lanca względem normalnej",
                                true, false, null, "Co trzecie użycie tworzy Krwawą Kulę. Zebranie pobliskiej kuli daje dodatkową lancę "
                                        + "o 125% normalnych obrażeń; 125% nie jest absolutnym procentem obrażeń broni ani bazową tabelą rang."),
                        extraWhen(node(3, "poszukiwanie_krwi", "Poszukiwanie Krwi", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "50%[x]",
                                "Za każdego przebitego / lanced wroga", "Obrażenia lancy", false, false, 50,
                                "Obrażenia +50%[x] za przebitego wroga i dodatkowo +50%[x] do pierwszego trafionego wroga; obie premie warunkowe."),
                                MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "50%[x]", "Obrażenia lancy", false, false,
                                "Pierwszy trafiony wróg", "Dodatkowe +50%[x] dotyczy pierwszego trafionego wroga.")));
    }

    private static CharacterTreeSkill skeletonMage() {
        return coreSkill("szkieletowy_mag", "Szkieletowy Mag", "skeleton-mage", 80, 204, 30, 20, Set.of("CORE", "SUMMON", "MINION"),
                "Skeleton Mage: R1 80%, max-view 204% za atak jednego maga; bazowo maksymalnie 3 magów, bez mnożenia przez ich liczbę. "
                        + "Konflikt typu obrażeń: D4Guides podaje Physical, bieżący D4Builds prezentuje Shadow Damage. "
                        + "Typ sługi może zależeć od wariantu Book of the Dead; nie dodajemy bazowych tagów PHYSICAL ani SHADOW. "
                        + "Konflikt nie blokuje procentów za pojedynczy atak.",
                List.of(
                        node(1, "czas_dzialania_premii_do_obrazen", "Czas Działania Premii do Obrażeń", MULTIPLICATIVE_DAMAGE_PERCENT,
                                PERCENT_X, "15%[x]", "Premia rośnie o 1% za sekundę życia maga; maksymalnie 25%[x]", "Atak jednego maga",
                                false, false, 15, "Obrażenia +15%[x], premia rośnie o 1% na sekundę życia maga do 25%[x]."),
                        node(1, "szansa_krytyczna", "Szansa na Trafienie Krytyczne", STATUS_DAMAGE_ENABLER, PERCENT_PLUS, "+5%",
                                "Gracz atakuje wroga zranionego przez maga", "Szansa krytyczna gracza", false, false, null,
                                "Gracz zyskuje +5% szansy krytycznej przeciw wrogom zranionym przez Szkieletowych Magów."),
                        node(3, "sabat", "Sabat", DEFENSE_OR_UTILITY, HIT_COUNT, "+2 magów", "Po wybraniu ulepszenia", "Limit magów",
                                false, false, null, "Maksymalna liczba magów +2; bez mnożenia bazowego procentu obrażeń."),
                        node(3, "dar_smierci", "Dar Śmierci", RESOURCE_OR_COST, PERCENT_X, "50%[x]",
                                "Wariant Corpse Skill wymaga zwłok; za każdego aktywnego maga", "Regeneracja Esencji", false, true, null,
                                "Staje się również Corpse Skill i wymaga zwłok. Każdy aktywny mag zwiększa regenerację Esencji: "
                                        + "R1 50%[x], max-view 128%[x]. Warunkowa globalna regeneracja, nie bazowe generowanie Esencji."),
                        extraWhen(extraWhen(node(3, "osobliwosc", "Osobliwość", RESOURCE_OR_COST, TEXT_ONLY, "Cała pozostała Esencja",
                                "Po wybraniu wariantu; użycie", "Dynamiczny koszt Esencji", false, false, null,
                                "Zużywa całą pozostałą Esencję; obrażenia +3%[x] i czas trwania +0.1 s za punkt zużytej Esencji. "
                                        + "Bazowy koszt pozostaje 30; brak założenia ilości pozostałego zasobu."),
                                MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "3%[x]", "Atak jednego maga", false, false,
                                "Za punkt Esencji faktycznie zużytej przez Osobliwość", "Dynamiczna premia zależna od zużytego zasobu."),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "+0.1 s za punkt Esencji", "Czas trwania maga", false, false,
                                "Za punkt Esencji faktycznie zużytej przez Osobliwość", "Czas trwania wydłuża się o 0.1 s za zużyty punkt."),
                        node(2, "premia_do_obrazen_od_ograniczenia_kontroli", "Premia do Obrażeń od Ograniczenia Kontroli",
                                MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "30%[x]", "Wróg z ograniczeniem kontroli / Crowd Controlled",
                                "Atak jednego maga", false, false, 30, "Obrażenia +30%[x] przeciw wrogom z ograniczeniem kontroli."),
                        node(2, "zajadlosc_animusz_lub_przytloczenie", "Zajadłość, Animusz lub Przytłoczenie", STATUS_DAMAGE_ENABLER,
                                TEXT_ONLY, "5% szansy na 1 kumulację", "Każdy atak maga; rodzaj zależy od wariantu", "Status gracza", false, false, null,
                                "Atak maga ma 5% szans na 1 kumulację: Ferocity dla Shadow, Animusz / Resolve dla Cold, "
                                        + "Przytłoczenie / Overpower dla Bone. To opis wariantów, nie bazowy tag obrażeń.")));
    }

    private static CharacterTreeSkill corpseExplosion() {
        return corpseSkill("wybuch_zwlok", "Wybuch Zwłok", "corpse-explosion", 180, 459, 25, null,
                List.of(CONSUMED_CORPSE), Set.of("CORPSE", "PROFANE", "PHYSICAL"),
                "Corpse Explosion: detonuje zużywane Zwłoki; R1 180%, max-view 459% Physical. "
                        + "Oficjalny patch 3.2.1 zwiększył bazowe obrażenia ze 110% do 180%. "
                        + "Bazowo wymaga 1 Zwłok; dodatkowe detonacje ani Transformations nie są sumowane z bazowym trafieniem.",
                List.of(
                        node(1, "wiele_zwlok", "Wiele Zwłok", RESOURCE_OR_COST, TEXT_ONLY, "Do 2 dodatkowych Zwłok",
                                "Dostępne dodatkowe Zwłoki", "Możliwość dodatkowych detonacji", false, false, null,
                                "Detonuje do 2 dodatkowych Zwłok; bazowe wymaganie pozostaje 1, bez automatycznego mnożenia obrażeń przez 3."),
                        node(1, "generowanie_esencji", "Generowanie Esencji", RESOURCE_OR_COST, TEXT_ONLY, "+15 pkt. esencji",
                                "Po użyciu z wybranym ulepszeniem", "Generowanie Esencji z ulepszenia", false, false, null,
                                "Użycie generuje 15 Esencji tylko po wyborze ulepszenia; brak bazowego generowania."),
                        node(2, "krwawa_kula", "Krwawa Kula", DEFENSE_OR_UTILITY, TEXT_ONLY, "20% szansy",
                                "Po użyciu", "Utworzenie Krwawej Kuli", false, false, null,
                                "20% szansy na utworzenie Krwawej Kuli; nie jest współczynnikiem obrażeń."),
                        node(2, "wydajne_zwloki", "Wydajne Zwłoki", RESOURCE_OR_COST, TEXT_ONLY, "1 dodatkowe zużycie tych samych Zwłok",
                                "Zwłoki zużyte przez Wybuch Zwłok", "Wydajność zużywania Zwłok", false, false, null,
                                "Zużyte Zwłoki mogą zostać zużyte jeszcze 1 raz; bazowo nadal wymagane i zużywane są 1 Zwłoki. "
                                        + "Nie oznacza niezużywania Zwłok, darmowego użycia ani automatycznego dodatkowego Wybuchu."),
                        extra(node(3, "krwawa_miazga", "Krwawa Miazga", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "75%[x]",
                                "Po wybraniu Transformation", "Obrażenia eksplozji", false, false, 75,
                                "Bloody Mess: obrażenia +75%[x], rozmiar +50%. Oficjalny patch 3.1 rozstrzyga zmianę 50% -> 75%; "
                                        + "D4Guides nadal pokazuje nieaktualne 50%[x], Icy Veins i D4Builds potwierdzają 75%. "
                                        + "Źródło rozstrzygające: https://news.blizzard.com/en-us/article/24287406/diablo-iv-patch-notes-3-1"),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "+50% rozmiaru", "Rozmiar eksplozji", false, false, "Rozmiar eksplozji +50%."),
                        variantDamage(node(3, "miazmaty", "Miazmaty", DAMAGE_OVER_TIME, COMPONENT_PERCENT, "350% w ciągu 6 s",
                                "Transformation zastępuje bazową eksplozję", "DAMAGE_OVER_DURATION: Miazmaty", false, true, 350,
                                "Miasma: również Darkness; zamiast eksplozji R1 350%, max-view 893% Corrupting łącznie w ciągu 6 s. "
                                        + "Nie jest procentem na sekundę i nie dodaje 180 + 350. Oficjalny patch 3.2.1 potwierdza R1 350%."),
                                DAMAGE_OVER_DURATION, 350, 893, 6),
                        extra(variantDamage(node(3, "szrapnel", "Szrapnel", RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "180%",
                                "Transformation zastępuje bazową eksplozję; za pojedynczy odłamek", "Obrażenia pojedynczego odłamka",
                                false, true, 180, "Shrapnel: również Bone; zamiast eksplozji 8 przebijających odłamków, każdy R1 180%, "
                                        + "max-view 459%. Oficjalny patch 3.2.1 potwierdza 180%. Nie sumujemy 8 odłamków ani bazowej eksplozji."),
                                PRIMARY_DAMAGE, 180, 459, null), ADDITIONAL_HIT_OR_STRIKE, HIT_COUNT, "8 odłamków",
                                "Liczba odłamków wariantu", false, false, "Wariant wystrzeliwuje 8 przebijających odłamków zamiast eksplozji.")));
    }

    private static CharacterTreeSkill corpseTendrils() {
        String conflict = "Konflikt źródeł: PL D4Guides, Icy Veins i bieżący D4Builds podają +40%[+] Lucky Hit, "
                + "natomiast changelog EN D4Guides 3.2.1 pokazuje usunięcie tej właściwości. "
                + "Brak oficjalnego rozstrzygnięcia; zachowano node i +40% do ręcznej weryfikacji, bez zmiany bazowego Lucky Hit 30%.";
        Node luckyHit = new Node(new SkillUpgrade("szansa_szczesliwego_trafu", "Szansa na Szczęśliwy Traf",
                SkillTreeStatus.NEEDS_VERIFICATION, conflict), List.of(modifier("grupa_2", "Szansa na Szczęśliwy Traf",
                UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW, PERCENT_PLUS, "+40%", "Konflikt źródeł 3.2.1",
                "Lucky Hit", false, false, UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, conflict)),
                new UpgradeDamageImpact("Szansa na Szczęśliwy Traf", "grupa_2", UpgradeDamageImpactType.NEEDS_VERIFICATION, conflict, null));
        return corpseSkill("trupie_macki", "Trupie Macki", "corpse-tendrils", 20, 98, 30, 11,
                List.of(CONSUMED_CORPSE), Set.of("CORPSE", "PROFANE", "PHYSICAL"),
                "Corpse Tendrils: bazowo wymaga i zużywa 1 Zwłoki. Spowalnia o 50% przez 1.5 s, następnie przyciąga / Pull "
                        + "i zadaje R1 20%, max-view 98% Physical. Źródłowe odnowienie 11 s nie określa cyklu ani DPS. "
                        + "Gorzkie Żniwa zmieniają zużywanie, Chodź Tu! usuwa wymaganie tylko w wariancie; bazowa lista pozostaje bez zmian.",
                List.of(
                        node(1, "szansa_krytyczna", "Szansa na Trafienie Krytyczne", STATUS_DAMAGE_ENABLER, PERCENT_PLUS, "+5%[+]",
                                "Przez 6 s przeciw wrogom zranionym przez Trupie Macki", "Szansa krytyczna gracza", false, false, null,
                                "Szansa krytyczna +5%[+] przez 6 s przeciw wrogom zranionym przez Trupie Macki."),
                        node(1, "generowanie_esencji", "Generowanie Esencji", RESOURCE_OR_COST, TEXT_ONLY, "+35 pkt. esencji",
                                "Po użyciu z wybranym ulepszeniem", "Generowanie Esencji z ulepszenia", false, false, null,
                                "Użycie generuje 35 Esencji tylko po wyborze ulepszenia; brak bazowego generowania."),
                        luckyHit,
                        node(2, "odsloniecie", "Odsłonięcie", STATUS_DAMAGE_ENABLER, SECONDS, "4 s",
                                "Wróg trafiony przez Trupie Macki", "Vulnerable", false, false, null, "Odsłonięcie / Vulnerable przez 4 s."),
                        extra(node(3, "gorzkie_zniwa", "Gorzkie Żniwa", RESOURCE_OR_COST, TEXT_ONLY, "Zwłoki wymagane, ale niezużywane",
                                "Po wybraniu Transformation", "Wymaganie użycia wariantu", false, false, null,
                                "Bitter Harvest: nadal wymaga Zwłok, ale ich nie zużywa (REQUIRED_NOT_CONSUMED); "
                                        + "wrogowie unieruchomieni przez 3 s po przyciągnięciu. Nie zmienia bazowej listy wymagań."),
                                STATUS_DAMAGE_ENABLER, SECONDS, "3 s", "Immobilized po Pull", false, false, "Unieruchomienie przez 3 s po przyciągnięciu."),
                        extraWhen(node(3, "chodz_tu", "Chodź Tu!", RESOURCE_OR_COST, TEXT_ONLY, "Wariant nie wymaga Zwłok",
                                "Po wybraniu Transformation", "Wymaganie użycia wariantu", false, false, null,
                                "Get Over Here!: używane na graczu zamiast Zwłok; usuwa wymaganie Zwłok tylko dla wariantu. "
                                        + "Umocnienie 5% maksymalnego zdrowia za każdego zranionego wroga; nie zakładamy liczby wrogów."),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "5% maksymalnego zdrowia za wroga", "Umocnienie / Fortify", false, false,
                                "Za każdego wroga zranionego przez wariant", "Umocnienie 5% maksymalnego zdrowia za zranionego wroga."),
                        extra(variantDamage(node(3, "szczeki_smierci", "Szczęki Śmierci", RANK_SCALING_COMPONENT_PERCENT,
                                COMPONENT_PERCENT, "100%", "Transformation zastępuje bazowe obrażenia", "Obrażenia Trupich Macek",
                                false, true, 100, "Jaws of Death: R1 100%, max-view 255% zamiast bazowego trafienia 20%; "
                                        + "bez opóźnienia przed Pull. Nie dodajemy 20 + 100."), PRIMARY_DAMAGE, 100, 255, null),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "Bez opóźnienia przed Pull", "Przyciągnięcie", false, false,
                                "Usuwa opóźnienie przed przyciągnięciem.")));
    }

    private static CharacterTreeSkill skeletonWarrior() {
        return corpseSkill("szkieletowy_wojownik", "Szkieletowy Wojownik", "skeleton-warrior", 65, 166, 20, 10,
                List.of(), Set.of("CORPSE", "MINION", "SUMMON"),
                "Skeleton Warrior — pasywnie: co 2 s wskrzesza wojownika z pobliskich Zwłok, maksymalnie 4; "
                        + "R1 65%, max-view 166% za atak jednego wojownika. Aktywnie: rozkazuje istniejącym wojownikom skoczyć i zaatakować; "
                        + "odnowienie rozkazu 10 s, bez wymagania Zwłok aktywnego użycia. Nie mnożymy obrażeń przez liczbę sług. "
                        + "Konflikt typu obrażeń: D4Guides PL podaje Physical, Icy Veins Physical or Shadow i warianty Bone/Blood/Darkness. "
                        + "Typ zależy od Book of the Dead; bez bezwarunkowych tagów PHYSICAL i SHADOW.",
                List.of(
                        node(1, "premia_do_obrazen", "Premia do Obrażeń", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "25%[x]",
                                "Przez 10 s po aktywnym rozkazie", "Atak jednego wojownika", false, false, 25,
                                "Aktywny rozkaz zwiększa obrażenia wojowników o 25%[x] przez 10 s."),
                        node(1, "leczenie", "Leczenie", DEFENSE_OR_UTILITY, TEXT_ONLY, "2% maksymalnego zdrowia",
                                "Każdy atak wojownika", "Leczenie gracza", false, false, null,
                                "Każdy atak wojownika leczy gracza za 2% jego maksymalnego zdrowia; nie są to obrażenia."),
                        node(2, "animusz", "Animusz", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "+3 kumulacje Animuszu / Resolve",
                                "Po aktywnym rozkazie", "Animusz gracza", false, false, null,
                                "Rozkaz daje 3 kumulacje Resolve / Animuszu. D4Guides i Icy Veins są zgodne; "
                                        + "D4Builds ma nagłówek Resolve, lecz błędnie wpisuje Ferocity w opisie. Zachowano Resolve."),
                        node(2, "odsloniecie", "Odsłonięcie", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "30% szansy; 4 s",
                                "Każdy atak wojownika", "Vulnerable", false, false, null,
                                "Każdy atak ma 30% szansy na Odsłonięcie / Vulnerable przez 4 s. To nie jest bazowy Lucky Hit, który wynosi 20%."),
                        node(3, "wladca_marionetek", "Władca Marionetek", DEFENSE_OR_UTILITY, HIT_COUNT, "+3 wojowników",
                                "Po wybraniu Transformation", "Maksymalna liczba wojowników", false, false, null,
                                "Master of Puppets: limit wojowników +3. D4Guides i Icy Veins podają zwiększenie o 3; "
                                        + "D4Builds ma niepoprawne increased to 3. Bazowy procent za atak pozostaje bez zmian; nie liczymy 7 × 65%."),
                        extra(extra(variantDamage(node(3, "sluzba_i_ofiara", "Służba i Ofiara", FLAT_COMPONENT_PERCENT,
                                COMPONENT_PERCENT, "150%", "Śmierć wojownika w wariancie", "Eksplozja śmierci wojownika",
                                true, true, 150, "Service and Sacrifice: nowy komponent eksplozji na śmierć, R1 150%, max-view 383%. "
                                        + "W walce wojownicy tracą 25% maksymalnego zdrowia na sekundę; odstęp wskrzeszania 2 s -> 0.5 s. "
                                        + "Nie zakładamy liczby śmierci ani DPS."), BURST_DAMAGE, 150, 383, null),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "Utrata 25% maksymalnego zdrowia / sek.", "Zdrowie wojownika", false, false,
                                "W walce wojownicy tracą 25% maksymalnego zdrowia na sekundę."),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "Wskrzeszanie co 0.5 s zamiast 2 s", "Pasywny odstęp wskrzeszania", false, false,
                                "Wskrzeszanie z pobliskich Zwłok co 0.5 s; nie jest odnowieniem aktywnego rozkazu ani dodatkowym aktywnym kosztem."),
                        extraWhen(node(3, "litania_smierci", "Litania Śmierci", STATUS_DAMAGE_ENABLER, PERCENT_PLUS, "+15%[+]",
                                "Wszyscy wojownicy przywołani; Kapłan przez 8 s", "Szansa krytyczna sług", false, true, null,
                                "Litany of Death: po przywołaniu wszystkich wojowników zamiast kolejnego przywołuje Kapłana na 8 s. "
                                        + "Szansa krytyczna sług R1 +15%[+], max-view +21%[+]; bez interpolacji 2..14, nie jest współczynnikiem obrażeń. "
                                        + "Leczy sługi za 100% maksymalnego zdrowia łącznie przez czas działania."),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "100% maksymalnego zdrowia w ciągu 8 s", "Leczenie sług", false, false,
                                "Wszyscy wojownicy przywołani; Kapłan przez 8 s", "Leczenie sług za 100% maksymalnego zdrowia łącznie przez 8 s.")));
    }

    private static CharacterTreeSkill bloodMist() {
        return macabreSkill("krwawa_mgla", "Krwawa Mgła", "blood-mist", SkillTreeType.DAMAGE, null, null, 9, 24, null,
                Set.of("MACABRE", "BLOOD", "PHYSICAL"),
                "Blood Mist: niewrażliwość / Immune przez 3 s; R1 420%, max-view 1071% łącznie w ciągu 3 s. "
                        + "Leczenie przez cały czas działania: R1 6.0%, max-view 15.3% maksymalnego zdrowia, bez tabeli leczenia. "
                        + "Bazowe użycie nie wymaga Zwłok. Teleportacja i zużycie Zwłok dotyczą wyłącznie Przypływu Krwi.",
                DamagePercentComponentRankTable.of(Map.of(DAMAGE_OVER_DURATION, ranks(420, 1071)), Map.of(DAMAGE_OVER_DURATION, 3)),
                List.of(
                        node(1, "szybkosc_ruchu", "Szybkość Ruchu", DEFENSE_OR_UTILITY, PERCENT_X, "+30%[x]",
                                "Przez 3 s", "Szybkość ruchu gracza", false, false, null, "Szybkość ruchu +30%[x] przez 3 s."),
                        node(1, "trafienie_krytyczne", "Trafienie Krytyczne", STATUS_DAMAGE_ENABLER, PERCENT_PLUS, "+5%[+]",
                                "Przez 5 s po zakończeniu Krwawej Mgły", "Szansa krytyczna gracza", false, false, null,
                                "Szansa na trafienie krytyczne +5%[+] przez 5 s po zakończeniu działania."),
                        node(2, "przytloczenie", "Przytłoczenie", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "1 kumulacja / sek.",
                                "Podczas działania; po teleportacji tylko z Przypływem Krwi", "Przytłoczenie / Overpower", false, false, null,
                                "Przyznaje 1 kumulację Przytłoczenia co sekundę. D4Guides 3.2.1 dodaje 1 kumulację po teleportacji; "
                                        + "teleport wymaga wyboru Przypływu Krwi / Blood Rush i nie jest efektem bazowej mgły."),
                        node(2, "odsloniecie_i_spowolnienie", "Odsłonięcie i Spowolnienie", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Vulnerable; Slow 70%; 4 s",
                                "Wróg objęty działaniem", "Odsłonięcie i Spowolnienie", false, false, null,
                                "Wywołuje Odsłonięcie / Vulnerable i Spowolnienie / Slow o 70% przez 4 s."),
                        node(3, "transfuzja_krwi", "Transfuzja Krwi", DEFENSE_OR_UTILITY, TEXT_ONLY, "Umocnienie 30% maksymalnego zdrowia; 1 Krwawa Kula / sek.",
                                "Przez czas działania", "Umocnienie / Fortify i Krwawe Kule", false, false, null,
                                "Blood Transfusion: Umocnienie 30% maksymalnego zdrowia przez czas działania; Krwawa Kula co sekundę. "
                                        + "Nie zmienia bazowej tabeli obrażeń."),
                        extra(extra(node(3, "przyplyw_krwi", "Przypływ Krwi", RESOURCE_OR_COST, TEXT_ONLY, "Zużywa 1 pobliskie Zwłoki",
                                "Transformation zamiast bazowej mgły; dostępne pobliskie Zwłoki", "Wymaganie użycia wariantu", false, false, null,
                                "Blood Rush: także Mobility; zamiast mgły zużywa 1 pobliskie Zwłoki i teleportuje do celu. "
                                        + "Odnowienie wariantu 24 s -> 8 s; Unstoppable R1 3 s, max-view 4.2 s, bez interpolacji. "
                                        + "Bazowe activationRequirements i odnowienie 24 s pozostają bez zmian."),
                                CAST_SPEED_OR_COOLDOWN, SECONDS, "Odnowienie wariantu: 8 s", "Odnowienie wariantu", false, false,
                                "Wyłącznie odnowienie Przypływu Krwi: 8 s zamiast bazowych 24 s."),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "Teleportacja; Unstoppable R1 3 s / max-view 4.2 s", "Mobilność i Nieustępliwość",
                                false, true, "Przypływ Krwi: Zużywa 1 pobliskie Zwłoki i teleportuje do celu. Odnowienie wariantu: 8 s. "
                                        + "Nieustępliwość / Unstoppable: R1 3 s, max-view 4.2 s; brak rang 2..14."),
                        variantDamage(node(3, "pochlaniajaca_mgla", "Pochłaniająca Mgła", DAMAGE_OVER_TIME, COMPONENT_PERCENT, "600% w ciągu 3 s",
                                "Transformation zastępuje bazowy profil Krwawej Mgły", "DAMAGE_OVER_DURATION: Pochłaniająca Mgła", false, true, 600,
                                "Devouring Mist: Darkness i Shadow, Wampiryczna Klątwa / Vampiric Curse. "
                                        + "R1 600%, max-view 1530% łącznie przez 3 s zastępuje 420/1071; bez sumowania profili."),
                                DAMAGE_OVER_DURATION, 600, 1530, 3)));
    }

    private static CharacterTreeSkill bonePrison() {
        String thorns = "Bramble: zyskujesz X Cierni za uwięzionego wroga; X zależy od poziomu postaci. "
                + "Zniszczony lub wygasły segment zadaje 100% twoich Cierni wokół siebie, a nie 100% Weapon Damage. "
                + "Bez stałej tabeli współczynników broni i przeliczania X.";
        Node bramble = new Node(new SkillUpgrade("ciern", "Cierń", SkillTreeStatus.NEEDS_VERIFICATION, thorns),
                List.of(new UpgradeDamageModifier("grupa_3", "Cierń", UpgradeDamageSafety.YES, THORNS_DAMAGE_MODIFIER,
                        "100% twoich Cierni; X Cierni za wroga", TEXT_ONLY, "Zniszczenie lub wygaśnięcie segmentu",
                        false, "Ciernie / Thorns", false, UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, UpgradeDamageSafety.NO, thorns)),
                new UpgradeDamageImpact("Cierń", "grupa_3", UpgradeDamageImpactType.NEEDS_VERIFICATION, thorns, null));
        return macabreSkill("wiezienie_z_kosci", "Więzienie z Kości", "bone-prison", SkillTreeType.DEFENSIVE, null, null, null, 15, null,
                Set.of("MACABRE", "BONE"),
                "Bone Prison: bazowo bez współczynnika obrażeń i bez Lucky Hit. Więzienie trwa R1 6 s, max-view 8.4 s; "
                        + "jego źródłowe Life jest utility, bez tabeli obrażeń i bez interpolacji czasu. "
                        + "Bazowo brak kosztu i generowania Esencji; +50/+25 pochodzi wyłącznie z ulepszenia.",
                DamagePercentComponentRankTable.empty(), List.of(
                        node(1, "generowanie_esencji", "Generowanie Esencji", RESOURCE_OR_COST, TEXT_ONLY, "+50 Esencji; dodatkowo +25 za Bossa",
                                "Użycie z ulepszeniem; dodatkowe +25 tylko po uwięzieniu Bossa", "Generowanie Esencji z ulepszenia", false, false, null,
                                "Użycie generuje 50 Esencji; uwięzienie Bossa dodatkowo 25. Bez bazowego generowania SkillResourceData."),
                        node(1, "redukcja_czasu_odnowienia", "Redukcja Czasu Odnowienia", CAST_SPEED_OR_COOLDOWN, TEXT_ONLY, "-0.5 s za wroga; maks. -5 s",
                                "Za każdego uwięzionego wroga", "Aktywne czasy odnowienia gracza", false, false, null,
                                "Skraca aktywne czasy odnowienia o 0.5 s za wroga, maksymalnie 5 s; dotyczy aktywnych cooldownów, nie tylko Więzienia."),
                        node(2, "animusz", "Animusz", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "1 kumulacja za wroga",
                                "Za każdego uwięzionego wroga", "Animusz / Resolve", false, false, null,
                                "Przyznaje 1 kumulację Animuszu / Resolve za każdego uwięzionego wroga."),
                        node(2, "odsloniecie", "Odsłonięcie", STATUS_DAMAGE_ENABLER, SECONDS, "8 s",
                                "Wróg uwięziony", "Vulnerable / Odsłonięcie", false, false, null, "Odsłonięcie / Vulnerable przez 8 s."),
                        bramble,
                        extra(variantDamage(node(3, "bezdenna_ciemnosc", "Bezdenna Ciemność", DAMAGE_OVER_TIME, COMPONENT_PERCENT, "450%",
                                "Zbezczeszczona Ziemia utworzona przez Transformation", "Zbezczeszczona Ziemia / Corrupting", true, true, 450,
                                "Plunging Darkness: Darkness, Zwłoki co sekundę i nowy komponent Zbezczeszczonej Ziemi. "
                                        + "R1 450%, max-view 1148%; źródła nie podają pewnego czasu dla tego współczynnika, bez duration i DPS. "
                                        + "Oficjalny Blizzard patch 3.2.1: 300% -> 450%. "
                                        + "https://news.blizzard.com/en-us/article/24295394/celebrate-30-years-of-diablo-in-season-of-hell-s-legacy"),
                                PRIMARY_DAMAGE, 450, 1148, null),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "Tworzy 1 Zwłoki / sek.", "Generowanie Zwłok", false, false,
                                "Tworzy Zwłoki co sekundę; nie jest generowaniem Esencji ani wymaganiem aktywacji."),
                        extra(extra(node(3, "uwiezienie_zycia", "Uwięzienie Życia", RESOURCE_OR_COST, TEXT_ONLY, "Generowanie Esencji R1 +20% / max-view +51%",
                                "Gracz wewnątrz więzienia wariantu", "Premia do generowania Esencji", false, true, null,
                                "Life Imprisonment: Blood, więzienie wokół gracza. Wewnątrz generowanie Esencji i szybkość użycia "
                                        + "R1 +20%, max-view +51%, bez interpolacji 2..14. Zużywa 5% maksymalnego zdrowia co sekundę. "
                                        + "Nie zmienia bazowego SkillResourceData."),
                                CAST_SPEED_OR_COOLDOWN, TEXT_ONLY, "Szybkość użycia R1 +20% / max-view +51%", "Szybkość użycia gracza", false, true,
                                "Wewnątrz więzienia: Cast Speed R1 +20%, max-view +51%; brak rang 2..14."),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "Utrata 5% maksymalnego zdrowia / sek.", "Zdrowie gracza", false, false,
                                "Wewnątrz więzienia wariantu traci 5% maksymalnego zdrowia co sekundę.")));
    }

    private static CharacterTreeSkill boneSpirit() {
        return macabreSkill("kosciane_widmo", "Kościane Widmo", "bone-spirit", SkillTreeType.DAMAGE, 150, 383, 33, null,
                new SkillChargeData(2, 12), Set.of("MACABRE", "BONE", "PHYSICAL"),
                "Bone Spirit: R1 150%, max-view 383% za eksplozję szukającego ducha. Bazowo 2 ładunki, odnowienie jednego 12 s, "
                        + "bez zwykłego cooldownSeconds. Ogólna rubryka Cooldown 12 s w D4Guides/Icy Veins nie oznacza dodatkowego cooldownu; "
                        + "jawny opis Charges / Charge Cooldown rozstrzyga semantykę. "
                        + "Ładunki i Umiejętność Główna opisują wyłącznie wybrany wariant, bez mutacji bazowych metadanych.",
                DamagePercentComponentRankTable.empty(), List.of(
                        node(1, "ladunki", "Ładunki", CAST_SPEED_OR_COOLDOWN, TEXT_ONLY, "+1 ładunek; charge cooldown 12 -> 10 s",
                                "Po wybraniu Enhancement", "Liczba i odnowienie ładunków wariantu", false, false, null,
                                "Bazowo 2 ładunki / 12 s; z ulepszeniem 3 ładunki / 10 s. Nie zmienia bazowego SkillChargeData(2, 12)."),
                        extraWhen(extra(extra(node(1, "umiejetnosc_glowna", "Umiejętność Główna", RESOURCE_OR_COST, TEXT_ONLY,
                                "koszt bazowy wariantu 30 Esencji", "Po wybraniu Enhancement grupy 1", "Koszt wariantu Core", false, false, null,
                                "Core Skill: Enhancement w grupie 1, staje się Core, kosztuje 30 Esencji, usuwa cooldown i odnowienie ładunków "
                                        + "wyłącznie w wariancie. Zużywa całą pozostałą Esencję; +3%[x] za punkt zużyty. "
                                        + "Nie zakładamy liczby punktów ani automatycznego mnożnika; bazowe resourceData i chargeData pozostają bez zmian."),
                                RESOURCE_OR_COST, TEXT_ONLY, "zużywa całą pozostałą Esencję", "Dodatkowy koszt wariantu", false, false,
                                "Wariant zużywa całą pozostałą Esencję przy użyciu; nie zakładamy jej ilości."),
                                CAST_SPEED_OR_COOLDOWN, TEXT_ONLY, "Wariant bez cooldownu i odnowienia ładunków", "Profil odnowienia wariantu", false, false,
                                "Umiejętność Główna usuwa mechanikę cooldownu / charge recharge tylko dla wariantu."),
                                MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "+3%[x] per Essence consumed", "Obrażenia wariantu Core", false, false,
                                "Za każdy punkt Esencji zużyty przez wariant", "Umiejętność Główna jest Enhancement grupy 1: koszt bazowy wariantu 30 Esencji; "
                                        + "zużywa całą pozostałą Esencję. Wariant bez cooldownu i odnowienia ładunków. "
                                        + "Obrażenia +3%[x] za każdy zużyty punkt Esencji; bez automatycznego multiplier."),
                        node(2, "maksymalna_esencja", "Maksymalna Esencja", RESOURCE_OR_COST, TEXT_ONLY, "+2 maksymalnej Esencji; maks. +20; 15 s",
                                "Za każdego trafionego wroga", "Maksymalna Esencja gracza", false, false, null,
                                "Każdy trafiony wróg zwiększa maksymalną Esencję o 2 na 15 s, do +20; bez zmiany bazowego kosztu i generowania."),
                        node(2, "premia_do_obrazen", "Premia do Obrażeń", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "+10%[x] / sek.; maks. +30%[x]",
                                "Następne Kościane Widmo; premia gromadzona co sekundę", "Eksplozja Kościanego Widma", false, false, null,
                                "Co sekundę następne Kościane Widmo zyskuje +10%[x], do +30%[x]. Warunkowa premia, bez automatycznego zastosowania maksimum."),
                        extra(variantDamage(node(3, "poltergeisty", "Poltergeisty", RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "30%",
                                "Po eksplozji; za pojedynczego małego ducha", "Eksplozja pojedynczego małego ducha", true, true, 30,
                                "Poltergeists: po eksplozji 3 mniejsze szukające duchy, każdy R1 30%, max-view 77%. "
                                        + "Dodatkowy komponent, bez sumowania 3 × 30 ani 3 × 77 z bazową tabelą."),
                                ADDITIONAL_STRIKE_DAMAGE, 30, 77, null),
                                ADDITIONAL_HIT_OR_STRIKE, HIT_COUNT, "3 małe duchy", "Liczba małych duchów", false, false,
                                "Tworzy 3 małe duchy; liczba jest oddzielna od współczynnika jednego ducha, bez sumy single target."),
                        variantDamage(node(3, "niedokonczone_porachunki", "Niedokończone Porachunki", DAMAGE_OVER_TIME, COMPONENT_PERCENT, "250%",
                                "Po eksplozji; dodatkowy komponent wariantu", "Zbezczeszczona Ziemia / Corrupting", true, true, 250,
                                "Unfinished Business: Darkness i Shadow; bazowe uderzenie pozostaje. Zbezczeszczona Ziemia to dodatkowy komponent "
                                        + "R1 250%, max-view 638%; bez sumowania z bazą. Brak pewnego czasu w źródłach, bez duration i DPS."),
                                PRIMARY_DAMAGE, 250, 638, null),
                        variantDamage(node(3, "projekcja_astralna", "Projekcja Astralna", RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "100%",
                                "Transformation zastępuje bazowy współczynnik", "Eksplozja spiralnego ducha", false, true, 100,
                                "Astral Projection: przestaje szukać wrogów, krąży po spirali wokół gracza, przebija i eksploduje przy trafieniu. "
                                        + "R1 100%, max-view 255% zastępuje bazowe 150/383; bez dodawania 150 + 100."),
                                PRIMARY_DAMAGE, 100, 255, null)));
    }

    private static CharacterTreeSkill golem() {
        return macabreSkill("golem", "Golem", "golem", SkillTreeType.DAMAGE, 125, 319, 20, 16, null,
                Set.of("MACABRE", "MINION", "SUMMON"),
                "Golem — pasywnie: przywołuje jednego Golema, R1 125%, max-view 319% za jeden atak jednego Golema; "
                        + "odradza się 10 s po śmierci. Aktywnie: rozkaz skoku, Nieustępliwość / Unstoppable Golema, Prowokacja / Taunt, "
                        + "otrzymywane obrażenia -30% przez 6 s i Ciernie równe 50% pancerza przez ten czas. "
                        + "Odnowienie aktywnego rozkazu 16 s; respawn 10 s jest oddzielną mechaniką. "
                        + "Typ obrażeń według Icy Veins: Physical or Shadow; Bone/Blood/Darkness zależą od wariantu i Book of the Dead. "
                        + "D4Guides/D4Builds ogólnie wskazują Physical; bez bezwarunkowych tagów typu obrażeń i bez mnożenia przez liczbę Golemów.",
                DamagePercentComponentRankTable.empty(), List.of(
                        node(1, "nieustepliwosc", "Nieustępliwość", DEFENSE_OR_UTILITY, SECONDS, "3 s",
                                "Aktywny rozkaz Golemowi", "Nieustępliwość gracza", false, false, null,
                                "Rozkaz zapewnia także GRACZOWI Nieustępliwość / Unstoppable przez 3 s."),
                        node(1, "premia_do_obrazen", "Premia do Obrażeń", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "+30%[x]",
                                "Przeciw Odsłoniętym / Vulnerable wrogom", "Atak Golema", false, false, 30,
                                "Obrażenia Golema +30%[x] przeciw Odsłoniętym wrogom; warunkowy mnożnik. "
                                        + "Bazowe R1 125% / max-view 319% dotyczy jednego ataku jednego Golema, bez mnożenia przez liczbę sług."),
                        node(2, "oslabienie", "Osłabienie", STATUS_DAMAGE_ENABLER, SECONDS, "4 s",
                                "Każdy atak Golema", "Osłabienie / Weaken", false, false, null,
                                "Atak Golema Osłabia / Weakens wroga przez 4 s."),
                        node(2, "animusz_przytloczenie_lub_zajadlosc", "Animusz, Przytłoczenie lub Zajadłość", STATUS_DAMAGE_ENABLER, HIT_COUNT, "3 kumulacje zależne od typu Golema",
                                "Aktywny rozkaz; typ Golema z Book of the Dead", "Animusz / Przytłoczenie / Zajadłość", false, false, null,
                                "Rozkaz daje 3 kumulacje: Bone -> Resolve / Animusz, Blood -> Overpower / Przytłoczenie, Iron -> Ferocity / Zajadłość."),
                        extra(extra(node(3, "grobowy_kwiat", "Grobowy Kwiat", ADDITIONAL_HIT_OR_STRIKE, TEXT_ONLY, "60% normalnych obrażeń",
                                "Transformation zamiast jednego Golema; za pojedynczego mniejszego Golema", "Względne obrażenia mniejszego Golema", false, false, null,
                                "Gravebloom: 3 mniejsze Golemy zamiast jednego; każdy zadaje 60% normalnych obrażeń i ma +30%[+] Attack Speed. "
                                        + "60% jest wartością względną, nie Weapon Damage; bez tabeli 60% i bez mnożenia 3 × 125 ani 3 × 60."),
                                ADDITIONAL_HIT_OR_STRIKE, HIT_COUNT, "3 mniejsze Golemy", "Liczba Golemów wariantu", false, false,
                                "Wariant przywołuje 3 mniejsze Golemy zamiast jednego; brak automatycznego mnożenia obrażeń."),
                                CAST_SPEED_OR_COOLDOWN, PERCENT_PLUS, "+30%[+]", "Szybkość ataku mniejszych Golemów", false, false,
                                "Mniejsze Golemy zyskują Attack Speed +30%[+]."),
                        extraWhen(extra(variantDamage(node(3, "plugawa_zarlocznosc", "Plugawa Żarłoczność", FLAT_COMPONENT_PERCENT,
                                COMPONENT_PERCENT, "300%", "Aktywny rozkaz w wariancie", "Erupcja Golema", true, true, 300,
                                "Fel Gluttony: rozkaz powoduje nową erupcję R1 300%, max-view 765%. "
                                        + "Pasywnie zużywa pobliskie Zwłoki co sekundę, skracając aktywne odnowienie o 2 s za zużyte Zwłoki. "
                                        + "Nie jest bazowym wymaganiem aktywacji; nie zakładamy dostępności Zwłok ani efektywnego cooldownu."),
                                BURST_DAMAGE, 300, 765, null),
                                RESOURCE_OR_COST, TEXT_ONLY, "Pasywnie zużywa pobliskie Zwłoki co sekundę", "Pasywne zużywanie Zwłok", false, false,
                                "Pasywne zużywanie Zwłok przez wariant, bez wymogu Zwłok dla bazowego rozkazu Golema."),
                                CAST_SPEED_OR_COOLDOWN, TEXT_ONLY, "-2 s aktywnego odnowienia za zużyte Zwłoki", "Aktywne odnowienie Golema", false, false,
                                "Tylko po pasywnym zużyciu dostępnych pobliskich Zwłok", "Skraca aktywne odnowienie o 2 s za zużyte Zwłoki; bez wyliczania effectiveCycleSeconds."),
                        extra(node(3, "gigantycznosc", "Gigantyczność", CAST_SPEED_OR_COOLDOWN, TEXT_ONLY, "Szybkość użycia innych sług R1 +20%[x] / max-view +51%[x]",
                                "Aura większego Golema; inni słudzy", "Szybkość użycia innych sług", false, true, null,
                                "Gargantua: jeden większy Golem, aura dla INNYCH sług; Cast Speed i Movement Speed "
                                        + "R1 +20%[x], max-view +51%[x], bez interpolacji 2..14. Nie zmienia bazowych obrażeń 125/319."),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "Szybkość ruchu innych sług R1 +20%[x] / max-view +51%[x]", "Szybkość ruchu innych sług",
                                false, true, "Aura zwiększa Movement Speed innych sług: R1 +20%[x], max-view +51%[x]; brak rang pośrednich.")));
    }

    private static CharacterTreeSkill macabreSkill(String id, String name, String sourceSlug, SkillTreeType type,
                                                   Integer rankOne, Integer maxView, Integer luckyHit, Integer cooldown,
                                                   SkillChargeData charges, Set<String> tags, String notes,
                                                   DamagePercentComponentRankTable components, List<Node> nodes) {
        return sourceSkill(id, name, sourceSlug, "macabre", MACABRE, rankOne, maxView, luckyHit, SkillResourceData.EMPTY,
                tags, notes, nodes, components, List.of(), cooldown, List.of(), type, charges);
    }

    private static CharacterTreeSkill ironMaiden() {
        String thorns = "Schadenfreude: przy co najmniej 1 wrogu pod wpływem Żelaznej Dziewicy gracz zyskuje X Cierni. "
                + "X zależy od poziomu / widoku źródła; EN/PL pokazują różne wartości podglądu, Icy Veins i D4Builds pozostawiają X. "
                + "Obrażenia Żelaznej Dziewicy zwiększone o 100% twoich Cierni, bez stałego procentu Weapon Damage.";
        Node schadenfreude = new Node(new SkillUpgrade("zlosliwa_radosc", "Złośliwa Radość", SkillTreeStatus.NEEDS_VERIFICATION, thorns),
                List.of(new UpgradeDamageModifier("grupa_3", "Złośliwa Radość", UpgradeDamageSafety.YES, THORNS_DAMAGE_MODIFIER,
                        "X Cierni; obrażenia zwiększone o 100% twoich Cierni", TEXT_ONLY,
                        "Co najmniej 1 wróg pod wpływem Żelaznej Dziewicy", false, "Ciernie / Thorns", false,
                        UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, UpgradeDamageSafety.NO, thorns)),
                new UpgradeDamageImpact("Złośliwa Radość", "grupa_3", UpgradeDamageImpactType.NEEDS_VERIFICATION, thorns, null));
        var components = DamagePercentComponentRankTable.of(
                Map.of(PERIODIC_DAMAGE, ranks(30, 77), EVENT_TRIGGERED_DAMAGE, ranks(30, 77)),
                Map.of(), Map.of(PERIODIC_DAMAGE, 2));
        return curseSkill("zelazna_dziewica", "Żelazna Dziewica", "iron-maiden", SkillTreeType.DAMAGE, 5,
                Set.of("CURSE", "PROFANE", "PHYSICAL"),
                "Iron Maiden: klątwa obszaru trwa 30 s. PERIODIC_DAMAGE R1 30%, max-view 77% co 2 s; "
                        + "EVENT_TRIGGERED_DAMAGE R1 30%, max-view 77% na zdarzenie: przeklęty wróg zadaje bezpośrednie obrażenia. "
                        + "Nie sumujemy impulsów, mechanizmów ani zdarzeń; nie wyznaczamy ich częstotliwości ani DPS. "
                        + "30 s to czas klątwy, nie duration komponentów. Bazowo Physical / Profane, koszt 10 Esencji, Lucky Hit 5%. "
                        + "Darkness / Shadow tylko w Mistrzu Małodobrym, Blood tylko w Krwawej Pannicy. "
                        + "Blizzard 3.2.1 naprawia błędną konwersję bazowego Iron Maiden przez Aspect of Crippling Darkness: "
                        + "https://news.blizzard.com/en-us/article/24295394/celebrate-30-years-of-diablo-in-season-of-hell-s-legacy",
                components, List.of(
                        extraWhen(node(1, "generowanie_esencji", "Generowanie Esencji", RESOURCE_OR_COST, TEXT_ONLY, "Koszt wariantu: 0 Esencji",
                                "Po wybraniu Enhancement", "Koszt Esencji wariantu", false, false, null,
                                "Wariant usuwa koszt Esencji i generuje 5 Esencji za każdego nowo przeklętego wroga, "
                                        + "który nie był już pod wpływem Żelaznej Dziewicy. Bazowy koszt 10 bez zmian."),
                                RESOURCE_OR_COST, TEXT_ONLY, "+5 Esencji za nowo przeklętego wroga", "Generowanie Esencji wariantu", false, false,
                                "Wróg nie był już pod wpływem Żelaznej Dziewicy",
                                "Generuje 5 Esencji za każdego nowo przeklętego wroga; bez założenia liczby wrogów i bez bazowego generation."),
                        extraWhen(node(1, "egzekucja_i_umocnienie", "Egzekucja i Umocnienie", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Egzekucja",
                                "Wróg pod wpływem Żelaznej Dziewicy", "Egzekucja", false, false, null,
                                "Przeklęci wrogowie ulegają egzekucji; opis źródłowy nie podaje progu Execute. "
                                        + "Śmierć przeklętego wroga umacnia o 5% maksymalnego zdrowia."),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "Umocnienie: 5% maksymalnego zdrowia", "Umocnienie", false, false,
                                "Śmierć wroga pod wpływem Żelaznej Dziewicy", "Śmierć przeklętego wroga daje Umocnienie 5% maksymalnego zdrowia."),
                        node(2, "zajadlosc_i_przytloczenie", "Zajadłość i Przytłoczenie", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Lucky Hit: do 15%; 1 Ferocity albo 1 Overpower",
                                "Trafienie wroga pod wpływem Żelaznej Dziewicy", "Losowy status gracza", false, false, null,
                                "Lucky Hit: do 15% szansy na losowo 1 Zajadłość albo 1 Przytłoczenie. Nie zmienia bazowego Lucky Hit 5%."),
                        node(2, "odsloniecie", "Odsłonięcie", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Vulnerable / Odsłonięcie",
                                "Wróg pod wpływem Żelaznej Dziewicy", "Vulnerable", false, false, null, "Przeklęci wrogowie są odsłonięci."),
                        schadenfreude,
                        node(3, "mistrz_malodobry", "Mistrz Małodobry", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Darkness / Shadow; 30% szansy na Stun 2 s",
                                "Wróg otrzymuje obrażenia Żelaznej Dziewicy", "Konwersja wariantu i ogłuszenie", false, false, null,
                                "Torture Artist: wariant jest także Darkness i zadaje Shadow; oba współczynniki pozostają R1 30% / max-view 77%. "
                                        + "Przy otrzymaniu obrażeń klątwy 30% szansy na ogłuszenie na 2 s. Nie mutuje bazowych tagów."),
                        extra(extra(variantDamage(node(3, "krwawa_pannica", "Krwawa Pannica", FLAT_COMPONENT_PERCENT, COMPONENT_PERCENT, "150%",
                                "Użycie wariantu, przy dostępnych Krwawych Kulach w wybranym obszarze", "Wybuch Krwawych Kul wariantu", true, true, 150,
                                "Blood Maiden: wariant jest także Blood; zraniony wróg ma 5% szansy utworzenia Krwawej Kuli. "
                                        + "Użycie zbiera 4 Blood Orbs w obszarze i wywołuje burst R1 150%, max-view 383%. "
                                        + "Oddzielna tabela BURST_DAMAGE; bez mnożenia przez 4, dodawania do bazowych 30/77 i założenia dostępności kul."),
                                BURST_DAMAGE, 150, 383, null),
                                DEFENSE_OR_UTILITY, HIT_COUNT, "Zbiera 4 Blood Orbs", "Krwawe Kule w wybranym obszarze", false, false,
                                "Użycie wariantu zbiera 4 Blood Orbs; liczba jest oddzielną metadaną utility, bez mnożenia burstu."),
                                STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Blood; 5% szansy na Krwawą Kulę", "Konwersja i generowanie Krwawej Kuli", false, false,
                                "Wariant jest także Blood; wrogowie zranieni klątwą mają 5% szansy utworzenia Krwawej Kuli.")));
    }

    private static CharacterTreeSkill decrepify() {
        return curseSkill("zniedoleznienie", "Zniedołężnienie", "decrepify", SkillTreeType.SUPPORT, null,
                Set.of("CURSE", "PROFANE"),
                "Decrepify: klątwa obszaru, Weaken / Osłabienie i Slow / Spowolnienie R1 40%, max-view 56%, czas klątwy 30 s. "
                        + "To utility, bez bazowych obrażeń, damage type i tabeli obrażeń dla Slow; brak interpolacji rang 2..14. "
                        + "Koszt 10 Esencji, brak źródłowego Lucky Hit i cooldownu. "
                        + "Świętokradczy Amok zastępuje cel i efekt klątwy: buff własnych Minions zamiast osłabiania i spowalniania wrogów.",
                DamagePercentComponentRankTable.empty(), List.of(
                        node(1, "szansa_na_trafienie_krytyczne", "Szansa na Trafienie Krytyczne", STATUS_DAMAGE_ENABLER, PERCENT_PLUS, "+5%[+]",
                                "Przeciw wrogowi pod wpływem Zniedołężnienia", "Szansa na trafienie krytyczne", false, false, null,
                                "Dodatkowe +5%[+] Critical Strike Chance przeciw przeklętym wrogom; D4Builds potwierdza +5%."),
                        node(1, "redukcja_czasu_odnowienia", "Redukcja Czasu Odnowienia", CAST_SPEED_OR_COOLDOWN, TEXT_ONLY, "Lucky Hit: do 15%; losowy aktywny cooldown -1 s",
                                "Trafienie wroga pod wpływem Zniedołężnienia", "Losowy aktywny czas odnowienia", false, false, null,
                                "Lucky Hit: do 15% szansy na redukcję losowego aktywnego cooldownu o 1 s; nie jest gwarantowana i nie nadaje cooldownu Zniedołężnieniu."),
                        node(2, "szybkosc_ruchu", "Szybkość Ruchu", DEFENSE_OR_UTILITY, PERCENT_X, "+2%[x] za wroga",
                                "Za każdego wroga pod wpływem Zniedołężnienia", "Szybkość ruchu gracza", false, false, null,
                                "Każdy przeklęty wróg zwiększa Movement Speed o 2%[x]; bez założenia liczby przeciwników i maksimum."),
                        node(2, "animusz", "Animusz", DEFENSE_OR_UTILITY, TEXT_ONLY, "Lucky Hit: do 25%; 1 Resolve",
                                "Trafienie wroga pod wpływem Zniedołężnienia", "Animusz / Resolve", false, false, null,
                                "Lucky Hit: do 25% szansy na 1 Animusz / Resolve; brak bazowego Lucky Hit pozostaje bez zmian."),
                        node(3, "oszalamiajaca_klatwa", "Oszałamiająca Klątwa", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Daze R1 3.0 s / max-view 4.2 s",
                                "Pierwsze objęcie wroga Zniedołężnieniem", "Oszołomienie / Daze", false, true, null,
                                "Dizzying Curse: pierwsze objęcie wroga klątwą oszałamia na R1 3.0 s, max-view 4.2 s; utility bez tabeli obrażeń i interpolacji."),
                        extraWhen(node(3, "upuszczanie_zdrowia", "Upuszczanie Zdrowia", DEFENSE_OR_UTILITY, TEXT_ONLY, "Blood; leczenie R1 10% / max-view 25.5% maksymalnego zdrowia",
                                "Trafienie wroga pod wpływem Zniedołężnienia; maksymalnie raz na sekundę", "Leczenie gracza", false, true, null,
                                "Life Tap: wariant jest także Blood; trafienie przeklętego wroga leczy R1 10%, max-view 25.5% maksymalnego zdrowia, "
                                        + "maksymalnie raz na sekundę. Śmierć przeklętego wroga daje 20% szansy na Krwawą Kulę. "
                                        + "Bez interpolacji, tabel obrażeń dla leczenia i mutacji bazowych tagów."),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "20% szansy na Blood Orb", "Krwawe Kule", false, false,
                                "Śmierć wroga pod wpływem Zniedołężnienia", "Śmierć przeklętego wroga daje 20% szansy utworzenia Krwawej Kuli."),
                        extra(node(3, "swietokradczy_amok", "Świętokradczy Amok", CAST_SPEED_OR_COOLDOWN, TEXT_ONLY, "Cast Speed Minions R1 +20%[x] / max-view +51%[x]",
                                "Transformation zastępuje cel i efekt klątwy: własne Minions zamiast wrogów", "Szybkość użycia własnych sług", false, true, null,
                                "Unholy Frenzy: Zniedołężnienie nakładane na własne Minions zamiast wrogów. Zastępuje cel i efekt bazowej klątwy; "
                                        + "Weaken i Slow nie są przenoszone na wrogów. Sługi zyskują Cast Speed i Movement Speed R1 +20%[x], "
                                        + "max-view +51%[x], bez interpolacji i bez mutacji bazowego skilla."),
                                DEFENSE_OR_UTILITY, TEXT_ONLY, "Movement Speed Minions R1 +20%[x] / max-view +51%[x]", "Szybkość ruchu własnych sług", false, true,
                                "Unholy Frenzy: wariant zastępuje cel i efekt bazowej klątwy; działa na własne Minions zamiast wrogów, "
                                        + "bez przenoszenia Weaken / Slow na wrogów. Cast Speed i Movement Speed sług R1 +20%[x], max-view +51%[x].")));
    }

    private static CharacterTreeSkill curseSkill(String id, String name, String slug, SkillTreeType type, Integer luckyHit,
                                                 Set<String> tags, String notes, DamagePercentComponentRankTable components,
                                                 List<Node> nodes) {
        return sourceSkill(id, name, slug, "curse", CURSE, null, null, luckyHit, new SkillResourceData(10, null, null),
                tags, notes, nodes, components, List.of(), null, List.of(), type, null);
    }

    private static CharacterTreeSkill armyOfTheDead() {
        String scalingLimit = "Unyielding Commander: podczas działania Armii sługi zadają R1 +30%[x] obrażeń. "
                + "Blizzard 3.2.1 i Icy Veins potwierdzają skalowanie z rangą skilla; bieżący główny D4Builds nadal pokazuje 30%. "
                + "Brak potwierdzonego max-view / rank15; bez interpolacji. Wymaga ręcznej weryfikacji skalowania.";
        Node commander = new Node(new SkillUpgrade("niezlomne_dowodztwo", "Niezłomne Dowództwo",
                SkillTreeStatus.NEEDS_VERIFICATION, scalingLimit),
                List.of(modifier("grupa_3", "Niezłomne Dowództwo", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X,
                        "R1 +30%[x]; max-view niepotwierdzony", "Podczas działania Armii Umarłych", "Obrażenia sług",
                        false, true, UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, scalingLimit)),
                new UpgradeDamageImpact("Niezłomne Dowództwo", "grupa_3", UpgradeDamageImpactType.NEEDS_VERIFICATION,
                        scalingLimit, null));
        return ultimateSkill("armia_umarlych", "Armia Umarłych", "army-of-the-dead", 120, 306, 64, 60,
                SkillResourceData.EMPTY, Set.of("ULTIMATE", "SUMMON", "PHYSICAL"),
                "Army of the Dead: per explosion; liczba eksplozji nie jest stała. Tabela opisuje pojedynczy Volatile Skeleton: "
                        + "R1 120% / max-view 306% Physical. Szkielety pojawiają się przez 7 s; eksplozja ogłusza na 2 s. "
                        + "Patch 3.2.1: ponowne użycie podczas działania tworzy dodatkowe szkielety; maksymalnie 5 stosów. "
                        + "Bez sumowania eksplozji, mnożenia przez ponowne casty i traktowania 5 stosów jako baseline.",
                DamagePercentComponentRankTable.empty(), List.of(
                        node(1, "redukcja_czasu_odnowienia", "Redukcja Czasu Odnowienia", CAST_SPEED_OR_COOLDOWN, SECONDS, "-20 s; 60 → 40 s",
                                "Po wybraniu ulepszenia", "Czas odnowienia", false, false, null,
                                "Cooldown wariantu zmniejszony z 60 do 40 s; bazowy cooldownSeconds pozostaje 60."),
                        node(1, "premia_do_obrazen", "Premia do Obrażeń", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "+2%[x] za sługę",
                                "Za każdego posiadanego Miniona", "Obrażenia Armii Umarłych", false, false, 2,
                                "Warunkowe +2%[x] damage za każdego Miniona; nie zakładamy liczby sług."),
                        node(2, "pasywna_premia", "Pasywna Premia", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "1 Volatile Skeleton co 4 s",
                                "Pasywnie po wybraniu ulepszenia", "Pasywne przywołanie", false, false, null,
                                "Pasywnie przywołuje 1 Volatile Skeleton co 4 s. Utility, bez PERIODIC_DAMAGE, liczby eksplozji aktywnego castu i DPS."),
                        node(2, "generowanie_zwlok", "Generowanie Zwłok", DEFENSE_OR_UTILITY, TEXT_ONLY, "50% szansy na Zwłoki",
                                "Po wybraniu ulepszenia", "Tworzenie Zwłok", false, false, null,
                                "50% szansy utworzenia Zwłok; bez activation requirement i resource generation."),
                        extra(commander, DEFENSE_OR_UTILITY, TEXT_ONLY, "90% redukcji obrażeń sług", "Obrona sług", false, false,
                                "Podczas działania Armii Umarłych sługi otrzymują obrażenia zmniejszone o 90%."),
                        node(3, "pietrzenie_cial", "Piętrzenie Ciał", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "+10%[x] za eksplozję; maks. +300%[x]",
                                "Każda eksplozja Volatile Skeleton na przeciwniku", "Obrażenia Armii otrzymywane przez cel", false, false, 10,
                                "Pile the Bodies: Armia zbiega się na celu. Każda eksplozja zwiększa otrzymywane od Armii obrażenia o 10%[x], "
                                        + "do 300%[x]. Warunkowe stosy, bez automatycznego maksimum."),
                        variantDamage(node(3, "martwe_zimno", "Martwe Zimno", RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "150%",
                                "Pojedyncza eksplozja wariantu", "Eksplozja Cold zamiast Physical", false, true, 150,
                                "Dead Cold: Cold i Freeze zamiast Stun. Per explosion R1 150% / max-view 383% zastępuje 120/306; "
                                        + "bez dodawania do bazowego współczynnika i zmiany bazowych tagów."), PRIMARY_DAMAGE, 150, 383, null)));
    }

    private static CharacterTreeSkill bloodWave() {
        return ultimateSkill("fala_krwi", "Fala Krwi", "blood-wave", 500, 1275, 50, 50,
                SkillResourceData.EMPTY, Set.of("ULTIMATE", "BLOOD", "PHYSICAL"),
                "Blood Wave: fala Physical R1 500% / max-view 1275%, Knock Back. Bazowy cooldown 50 s, bez kosztu. "
                        + "Transformations zachowują oddzielne tabele i overrides; bez sumowania dodatkowej fali z bazową.",
                DamagePercentComponentRankTable.empty(), List.of(
                        node(1, "premia_do_obrazen", "Premia do Obrażeń", MULTIPLICATIVE_DAMAGE_PERCENT, PERCENT_X, "+30%[x]",
                                "Gdy gracz jest umocniony / Fortified", "Obrażenia Fali Krwi", false, false, 30,
                                "Fala zadaje +30%[x] obrażeń while Fortified; warunek nie jest zakładany automatycznie."),
                        node(1, "redukcja_obrazen", "Redukcja Obrażeń", DEFENSE_OR_UTILITY, TEXT_ONLY, "10% redukcji obrażeń przez 10 s",
                                "Po użyciu Fali Krwi", "Obrona gracza", false, false, null,
                                "Użycie daje 10% Damage Reduction na 10 s."),
                        node(2, "przytloczenie", "Przytłoczenie", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "1 Overpower; Maximum Overpower +2",
                                "Po użyciu Fali Krwi", "Przytłoczenie gracza", false, false, null,
                                "Aktualna semantyka według Blizzard 3.2.1: 1 stos Overpower i Maximum Overpower +2. "
                                        + "Oficjalna zmiana dotyczy Hematolagnii; Icy Veins używa tego opisu dla node'a. Nie jest współczynnikiem obrażeń."),
                        node(2, "krwawa_kula", "Krwawa Kula", DEFENSE_OR_UTILITY, TEXT_ONLY, "Do 2 Blood Orbs",
                                "Podczas przemieszczania się fali", "Tworzenie Krwawych Kul", false, false, null,
                                "Fala tworzy do 2 Krwawych Kul; nie zakładamy zawsze dwóch."),
                        extra(variantDamage(node(3, "krwawy_przyplyw", "Krwawy Przypływ", ADDITIONAL_HIT_OR_STRIKE, COMPONENT_PERCENT, "500%",
                                "Dodatkowa fala wariantu", "Dodatkowa fala", true, true, 500,
                                "Tides of Blood: wystrzeliwuje 1 dodatkową falę R1 500% / max-view 1275%. "
                                        + "Oddzielna tabela; bez automatycznego 2 × 500 lub 2 × 1275."), ADDITIONAL_STRIKE_DAMAGE, 500, 1275, null),
                                ADDITIONAL_HIT_OR_STRIKE, HIT_COUNT, "+1 fala", "Liczba dodatkowych fal", true, false,
                                "Jedna dodatkowa fala, niezależna metadana liczby trafień."),
                        variantDamage(node(3, "sciezka_ciemnosci", "Ścieżka Ciemności", DAMAGE_OVER_TIME, COMPONENT_PERCENT, "800%",
                                "Wróg na Desecrated Ground wariantu", "Corrupting trail", true, true, 800,
                                "Path of Darkness: Darkness / Shadow, zostawia Desecrated Ground R1 800% / max-view 2040% Corrupting. "
                                        + "800% potwierdzone Blizzard 3.2.1. Źródło nie podaje jednoznacznego duration; PRIMARY_DAMAGE wariantu "
                                        + "bez duration, bez DAMAGE_OVER_DURATION i przeliczenia na sekundę."), PRIMARY_DAMAGE, 800, 2040, null),
                        extra(extra(variantDamage(node(3, "hematolagnia", "Hematolagnia", RANK_SCALING_COMPONENT_PERCENT, COMPONENT_PERCENT, "300%",
                                "Fala wariantu Hematolagnia", "Krótsza fala zastępująca bazową", false, true, 300,
                                "Hematolagnia: Core, no cooldown, koszt 70 Esencji. Krótsza fala R1 300% / max-view 765% "
                                        + "zastępuje 500/1275 i nie odrzuca / no Knock Back. Bazowe cooldownSeconds=50 i brak kosztu pozostają bez zmian."),
                                PRIMARY_DAMAGE, 300, 765, null), RESOURCE_OR_COST, TEXT_ONLY, "Koszt: 70 Esencji", "Koszt wariantu", false, false,
                                "Koszt wariantu 70 Esencji według Blizzard 3.2.1; bazowy skill pozostaje bez kosztu."),
                                CAST_SPEED_OR_COOLDOWN, TEXT_ONLY, "Bez cooldownu / no cooldown", "Override cooldownu wariantu", false, false,
                                "Hematolagnia nie ma cooldownu; bazowe 50 s nie jest mutowane.")));
    }

    private static CharacterTreeSkill boneStorm() {
        return ultimateSkill("nawalnica_kosci", "Nawałnica Kości", "bone-storm", null, null, 40, 60,
                SkillResourceData.EMPTY, Set.of("ULTIMATE", "BONE", "PHYSICAL"),
                "Bone Storm: wokół gracza i Golema, total over duration R1 400% / max-view 1020% Physical w ciągu 10 s. "
                        + "DAMAGE_OVER_DURATION, bez mnożenia przez obecność Golema i bez 400% / s. Bazowe użycie nie wymaga Zwłok.",
                DamagePercentComponentRankTable.of(Map.of(DAMAGE_OVER_DURATION, ranks(400, 1020)), Map.of(DAMAGE_OVER_DURATION, 10)), List.of(
                        node(1, "bariera", "Bariera", DEFENSE_OR_UTILITY, TEXT_ONLY, "50% maksymalnego zdrowia; 10 s",
                                "Po użyciu Nawałnicy Kości", "Bariera gracza", false, false, null,
                                "Bariera o wartości 50% Maximum Life na 10 s."),
                        node(1, "odsloniecie", "Odsłonięcie", STATUS_DAMAGE_ENABLER, SECONDS, "4 s",
                                "Wróg pod wpływem Nawałnicy Kości", "Vulnerable", false, false, null, "Odsłonięcie na 4 s."),
                        node(2, "redukcja_obrazen", "Redukcja Obrażeń", DEFENSE_OR_UTILITY, TEXT_ONLY, "10% redukcji obrażeń",
                                "Podczas działania Nawałnicy Kości", "Obrona gracza", false, false, null, "10% Damage Reduction podczas działania."),
                        extra(node(2, "wydluzenie_czasu_dzialania", "Wydłużenie Czasu Działania", RESOURCE_OR_COST, TEXT_ONLY, "Do 4 Zwłok; +2 s za każde",
                                "Za każde faktycznie zużyte Zwłoki", "Zużycie Zwłok i duration wariantu", false, false, null,
                                "Zużywa do 4 Zwłok, +2 s duration za każde. Nie jest bazowym activation requirement; "
                                        + "bez automatycznego 18 s, dodatkowych ticków i total damage."),
                                DEFENSE_OR_UTILITY, SECONDS, "+2 s za zużyte Zwłoki", "Wydłużenie czasu działania", false, false,
                                "Do 4 Zwłok; +2 s za każde faktycznie zużyte Zwłoki. Bez zmiany bazowego duration 10 s i bez bazowego wymagania Zwłok."),
                        extra(node(3, "rzucone_kosci", "Rzucone Kości", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Critical Strike Chance R1 +15%[+] / max-view +38%",
                                "Podczas działania Nawałnicy Kości", "Szansa na trafienie krytyczne", false, true, null,
                                "Roll the Bones: Critical Strike Chance i Cast Speed R1 +15%[+] / max-view +38%; "
                                        + "source scaling utility bez interpolacji rang 2..14 i bez damage coefficient."),
                                CAST_SPEED_OR_COOLDOWN, TEXT_ONLY, "Cast Speed R1 +15%[+] / max-view +38%", "Szybkość użycia", false, true,
                                "Cast Speed 15/38 jako source scaling utility, bez damage table."),
                        variantDamage(node(3, "cien_i_kosc", "Cień i Kość", DAMAGE_OVER_TIME, COMPONENT_PERCENT, "40%",
                                "Wróg zraniony Nawałnicą wariantu", "Dodatkowy Corrupting", true, true, 40,
                                "Shadow and Bone: Darkness / Shadow. Bazowe 400/1020 w ciągu 10 s pozostaje; "
                                        + "dodatkowe DAMAGE_OVER_DURATION R1 40% / max-view 102% w ciągu 2 s, bez sumowania strukturalnego."),
                                DAMAGE_OVER_DURATION, 40, 102, 2),
                        node(3, "wyglodnialy_cyklon", "Wygłodniały Cyklon", CAST_SPEED_OR_COOLDOWN, TEXT_ONLY, "Redukcja cooldownu R1 20 s / max-view 28 s",
                                "Po wybraniu wariantu", "Czas odnowienia wariantu", false, true, null,
                                "Hungry Cyclone: użycie na celu, wyszukuje wrogów. Redukcja cooldownu R1 20 s / max-view 28 s "
                                        + "bez interpolacji; bazowy cooldownSeconds=60, bez effectiveCycleSeconds.")));
    }

    private static CharacterTreeSkill soulrift() {
        return ultimateSkill("spetanie_dusz", "Spętanie Dusz", "soulrift", null, null, 20, 40,
                new SkillResourceData(null, new BigDecimal("2"), new BigDecimal("2"), null, SkillResourceGenerationFrequency.PER_EVENT),
                Set.of("ULTIMATE", "DARKNESS", "SHADOW"),
                "Soulrift: DAMAGE_OVER_DURATION R1 350% / max-view 893% Corrupting w ciągu 8 s. "
                        + "Generowanie 2 Esencji / zdarzenie; zdarzenie = pochłonięcie jednej duszy przeciwnika (absorbed enemy soul). "
                        + "Sprawdzanie / absorpcja co 0.25 s: źródło opisuje absorpcję dusz 3 przeciwników; z Bossów 3 dusze jednocześnie. "
                        + "Bez stałej liczby dostępnych przeciwników, resource rate i zagregowanego generowania przez 8 s. "
                        + "Mechanika z aktualnego EN D4Guides; PL tylko dla nazw z powodu uszkodzonego renderowania opisu.",
                DamagePercentComponentRankTable.of(Map.of(DAMAGE_OVER_DURATION, ranks(350, 893)), Map.of(DAMAGE_OVER_DURATION, 8)), List.of(
                        node(1, "szybkosc_ruchu", "Szybkość Ruchu", DEFENSE_OR_UTILITY, TEXT_ONLY, "Unhindered; +1% ruchu za duszę, maks. +40%",
                                "Podczas działania i przez 5 s po zakończeniu", "Ruch gracza", false, false, null,
                                "Podczas działania Unhindered i +1% Movement Speed za pochłoniętą duszę, do +40%; "
                                        + "premia utrzymuje się 5 s po zakończeniu. Bez automatycznego maksimum."),
                        node(1, "bariera", "Bariera", DEFENSE_OR_UTILITY, TEXT_ONLY, "0.5% maksymalnego zdrowia za duszę",
                                "Każda pochłonięta dusza", "Bariera gracza", false, false, null,
                                "Każda dusza daje Barrier 0.5% Maximum Life; bariera utrzymuje się 5 s po zakończeniu. Bez założenia liczby dusz."),
                        node(2, "zajadlosc", "Zajadłość", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "1 Ferocity co 30 dusz",
                                "Pochłonięcie kolejnych 30 dusz", "Zajadłość gracza", false, false, null,
                                "Każde 30 pochłoniętych dusz daje 1 Ferocity; osiągnięcie progu nie jest zakładane."),
                        node(2, "odsloniecie_i_ograniczanie_kontroli", "Odsłonięcie i Ograniczanie Kontroli", STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Vulnerable 4 s; Fear i Slow 85% na 5 s",
                                "Pochłonięcie duszy wroga; efekty obszarowe po zakończeniu", "Vulnerable, Fear, Slow", false, false, null,
                                "Pochłonięcie duszy odsłania wroga na 4 s. Po zakończeniu pobliscy wrogowie otrzymują Fear i Slow 85% na 5 s."),
                        node(3, "destylacja_duszy", "Destylacja Duszy", MULTIPLICATIVE_DAMAGE_PERCENT, TEXT_ONLY, "R1 +30%[x] / max-view +77%[x]",
                                "Podczas działania Spętania Dusz", "Globalne obrażenia Darkness", false, true, 30,
                                "Distilled Anima: globalne Darkness damage R1 +30%[x] / max-view +77%[x]. Blizzard potwierdza scaling z rangą; "
                                        + "source metadata bez interpolacji i bez tabeli damage component Soulrift."),
                        extra(variantDamage(node(3, "wir_dusz", "Wir Dusz", FLAT_COMPONENT_PERCENT, COMPONENT_PERCENT, "25%",
                                "Pochłonięcie jednej duszy przeciwnika", "Obrażenia za pojedynczą pochłoniętą duszę", true, true, 25,
                                "Soul Vortex: użycie na celu, Pull. EVENT_TRIGGERED_DAMAGE R1 25% / max-view 64% na jedną pochłoniętą duszę. "
                                        + "Bez mnożenia przez 3, cykle 0.25 s i liczbę przeciwników. Redukcja cooldownu 15 s, bazowo nadal 40 s."),
                                EVENT_TRIGGERED_DAMAGE, 25, 64, null), CAST_SPEED_OR_COOLDOWN, SECONDS, "Redukcja cooldownu 15 s", "Czas odnowienia wariantu", false, false,
                                "Soul Vortex zmniejsza cooldown o 15 s; bez mutacji bazowych 40 s i bez effective cooldown."),
                        extra(variantDamage(node(3, "zamarzniete_pustkowia", "Zamarznięte Pustkowia", FLAT_COMPONENT_PERCENT, COMPONENT_PERCENT, "75%",
                                "Tylko pochłonięcie duszy zamrożonego / Frozen przeciwnika", "Warunkowa eksplozja Frozen absorbed enemy", true, true, 75,
                                "Frozen Wasteland: Frostbite i Chill 30% co sekundę. Tylko Frozen absorbed enemy roztrzaskuje się: "
                                        + "BURST_DAMAGE R1 75% / max-view 191%. Bez założenia częstotliwości eksplozji i współczynnika obrażeń dla Chill/Frostbite."),
                                BURST_DAMAGE, 75, 191, null), STATUS_DAMAGE_ENABLER, TEXT_ONLY, "Frostbite; Chill 30% co sekundę", "Konwersja i wychłodzenie", false, false,
                                "Frostbite i Chill 30% co sekundę to metadane; źródło nie podaje osobnego damage coefficient.")));
    }

    private static CharacterTreeSkill ultimateSkill(String id, String name, String slug, Integer rankOne, Integer maxView,
                                                   int luckyHit, int cooldown, SkillResourceData resources, Set<String> tags,
                                                   String notes, DamagePercentComponentRankTable components, List<Node> nodes) {
        return sourceSkill(id, name, slug, "ultimate", ULTIMATE, rankOne, maxView, luckyHit, resources, tags, notes,
                nodes, components, List.of(), cooldown, List.of(), SkillTreeType.DAMAGE, null);
    }

    private static CharacterTreeSkill corpseSkill(String id, String name, String sourceSlug, int rankOne, int maxView,
                                                  int luckyHit, Integer cooldown, List<SkillActivationRequirement> requirements,
                                                  Set<String> tags, String notes, List<Node> nodes) {
        return sourceSkill(id, name, sourceSlug, "corpse", CORPSE, rankOne, maxView, luckyHit, SkillResourceData.EMPTY,
                tags, notes, nodes, DamagePercentComponentRankTable.empty(), List.of(), cooldown, requirements);
    }

    /** Zachowuje tabelę wariantu przy jego modyfikatorze, poza bazową tabelą skilla. */
    private static Node variantDamage(Node node, DamagePercentComponent component, int rankOne, int maxView, Integer duration) {
        UpgradeDamageModifier original = node.modifiers().getFirst();
        var table = DamagePercentComponentRankTable.of(Map.of(component, ranks(rankOne, maxView)),
                duration == null ? Map.of() : Map.of(component, duration));
        UpgradeDamageModifier variant = new UpgradeDamageModifier(original.getUpgradeGroup(), original.getUpgradeName(),
                original.getUpgradeHasDamageModifier(), original.getType(), original.getValue(), original.getValueKind(),
                original.getCondition(), original.createsNewDamageComponent(), original.getAffectedComponent(),
                original.scalesWithSkillRank(), original.getSafeForRankingDisplay(), original.getSafeForRuntimeDps(),
                original.getNotes(), table);
        return new Node(node.upgrade(), List.of(variant), node.impact());
    }

    private static DamagePercentRankTable ranks(int rankOne, int maxView) {
        return DamagePercentRankTable.of(Map.of(1, rankOne, 15, maxView));
    }

    private static CharacterTreeSkill coreSkill(String id, String name, String sourceSlug, Integer rankOne, Integer maxView,
                                                int cost, int luckyHit, Set<String> tags, String notes, List<Node> nodes) {
        return coreSkill(id, name, sourceSlug, rankOne, maxView, cost, luckyHit, tags, notes, nodes,
                DamagePercentComponentRankTable.empty(), List.of());
    }

    private static CharacterTreeSkill coreSkill(String id, String name, String sourceSlug, Integer rankOne, Integer maxView,
                                                int cost, int luckyHit, Set<String> tags, String notes, List<Node> nodes,
                                                DamagePercentComponentRankTable components, List<UpgradeDamageModifier> baseModifiers) {
        return sourceSkill(id, name, sourceSlug, "core", CORE, rankOne, maxView, luckyHit,
                new SkillResourceData(cost, null, null), tags, notes, nodes, components, baseModifiers);
    }

    private static CharacterTreeSkill skill(String id, String name, String sourceSlug, int rankOne, int maxView, int luckyHit,
                                            SkillResourceData resources, Set<String> tags, String notes, List<Node> nodes) {
        return sourceSkill(id, name, sourceSlug, "basic", BASIC, rankOne, maxView, luckyHit, resources, tags, notes, nodes,
                DamagePercentComponentRankTable.empty(), List.of());
    }

    private static CharacterTreeSkill sourceSkill(String id, String name, String sourceSlug, String skillGroup, SourceSkillCategory category,
                                                 Integer rankOne, Integer maxView, Integer luckyHit, SkillResourceData resources,
                                                 Set<String> tags, String notes, List<Node> nodes, DamagePercentComponentRankTable components,
                                                 List<UpgradeDamageModifier> baseModifiers) {
        return sourceSkill(id, name, sourceSlug, skillGroup, category, rankOne, maxView, luckyHit, resources, tags, notes,
                nodes, components, baseModifiers, null, List.of());
    }

    private static CharacterTreeSkill sourceSkill(String id, String name, String sourceSlug, String skillGroup, SourceSkillCategory category,
                                                 Integer rankOne, Integer maxView, Integer luckyHit, SkillResourceData resources,
                                                 Set<String> tags, String notes, List<Node> nodes, DamagePercentComponentRankTable components,
                                                 List<UpgradeDamageModifier> baseModifiers, Integer cooldown,
                                                 List<SkillActivationRequirement> requirements) {
        return sourceSkill(id, name, sourceSlug, skillGroup, category, rankOne, maxView, luckyHit, resources, tags, notes,
                nodes, components, baseModifiers, cooldown, requirements, SkillTreeType.DAMAGE, null);
    }

    private static CharacterTreeSkill sourceSkill(String id, String name, String sourceSlug, String skillGroup, SourceSkillCategory category,
                                                 Integer rankOne, Integer maxView, Integer luckyHit, SkillResourceData resources,
                                                 Set<String> tags, String notes, List<Node> nodes, DamagePercentComponentRankTable components,
                                                 List<UpgradeDamageModifier> baseModifiers, Integer cooldown,
                                                 List<SkillActivationRequirement> requirements, SkillTreeType type, SkillChargeData charges) {
        List<SkillUpgradeGroup> groups = List.of(1, 2, 3).stream().map(group -> new SkillUpgradeGroup(
                "grupa_" + group, "Grupa " + group, nodes.stream()
                .filter(node -> node.modifiers().getFirst().getUpgradeGroup().equals("grupa_" + group))
                .map(Node::upgrade).toList())).toList();
        List<UpgradeDamageModifier> modifiers = new ArrayList<>(baseModifiers);
        nodes.stream().flatMap(node -> node.modifiers().stream()).forEach(modifiers::add);
        return new CharacterTreeSkill(id, name, "https://d4guides.gg/en/database/skills/" + sourceSlug, skillGroup,
                rankOne, maxView, rankOne == null ? DamagePercentRankTable.empty() : ranks(rankOne, maxView),
                components, type, SkillTreeStatus.NEEDS_VERIFICATION,
                groups, MAX_VIEW_NOTE + notes + " Źródło max-view: " + MAX_VIEW_SOURCE + "; nazwy PL: "
                + "https://d4guides.gg/pl/baza-danych/umiejetnosci/" + id.replace('_', '-'),
                Set.of(category), tags, resources, luckyHit, nodes.stream().map(Node::impact).toList(), modifiers, cooldown, requirements, charges);
    }

    private static Node node(int group, String id, String name, UpgradeDamageModifierType type, UpgradeDamageValueKind kind,
                             String value, String condition, String component, boolean newComponent, boolean scales,
                             Integer damagePercent, String description) {
        String groupId = "grupa_" + group;
        UpgradeDamageModifier modifier = modifier(groupId, name, type, kind, value, condition, component,
                newComponent, scales, UpgradeDamageSafety.YES, description);
        UpgradeDamageImpactType impactType = switch (type) {
            case MULTIPLICATIVE_DAMAGE_PERCENT, ADDITIVE_DAMAGE_PERCENT, RANK_SCALING_COMPONENT_PERCENT -> UpgradeDamageImpactType.DIRECT_DAMAGE_PERCENT;
            case FLAT_COMPONENT_PERCENT -> UpgradeDamageImpactType.BURST_DAMAGE;
            case ADDITIONAL_HIT_OR_STRIKE -> UpgradeDamageImpactType.ADDITIONAL_HIT;
            case DAMAGE_OVER_TIME -> UpgradeDamageImpactType.DAMAGE_OVER_TIME;
            case RESOURCE_OR_COST, CAST_SPEED_OR_COOLDOWN -> UpgradeDamageImpactType.COOLDOWN_OR_COST;
            default -> UpgradeDamageImpactType.STATUS_OR_UTILITY;
        };
        return new Node(new SkillUpgrade(id, name, SkillTreeStatus.NEEDS_VERIFICATION, description), List.of(modifier),
                new UpgradeDamageImpact(name, groupId, impactType, description, damagePercent));
    }

    private static Node conflictedLuckyHit() {
        return new Node(new SkillUpgrade("szansa_szczesliwego_trafu", "Szansa na Szczęśliwy Traf",
                SkillTreeStatus.NEEDS_VERIFICATION, LUCKY_HIT_CONFLICT), List.of(modifier("grupa_2",
                "Szansa na Szczęśliwy Traf", UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW, PERCENT_PLUS, "+30%", "Konflikt źródeł 3.2.1",
                "Lucky Hit", false, false, UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, LUCKY_HIT_CONFLICT)),
                new UpgradeDamageImpact("Szansa na Szczęśliwy Traf", "grupa_2", UpgradeDamageImpactType.NEEDS_VERIFICATION,
                        LUCKY_HIT_CONFLICT, null));
    }

    private static Node extra(Node node, UpgradeDamageModifierType type, UpgradeDamageValueKind kind, String value,
                              String component, boolean newComponent, boolean scales, String description) {
        return extraWhen(node, type, kind, value, component, newComponent, scales, "Po wybraniu ulepszenia", description);
    }

    private static Node extraWhen(Node node, UpgradeDamageModifierType type, UpgradeDamageValueKind kind, String value,
                                  String component, boolean newComponent, boolean scales, String condition, String description) {
        List<UpgradeDamageModifier> modifiers = new ArrayList<>(node.modifiers());
        modifiers.add(modifier(modifiers.getFirst().getUpgradeGroup(), node.upgrade().getName(), type, kind, value,
                condition, component, newComponent, scales, UpgradeDamageSafety.YES, description));
        return new Node(node.upgrade(), List.copyOf(modifiers), node.impact());
    }

    private static UpgradeDamageModifier modifier(String group, String name, UpgradeDamageModifierType type,
                                                   UpgradeDamageValueKind kind, String value, String condition,
                                                   String component, boolean newComponent, boolean scales,
                                                   UpgradeDamageSafety displaySafety, String description) {
        boolean directDamage = switch (type) {
            case MULTIPLICATIVE_DAMAGE_PERCENT, ADDITIVE_DAMAGE_PERCENT, FLAT_COMPONENT_PERCENT,
                    RANK_SCALING_COMPONENT_PERCENT, ADDITIONAL_HIT_OR_STRIKE, DAMAGE_OVER_TIME -> true;
            default -> false;
        };
        return new UpgradeDamageModifier(group, name, directDamage ? UpgradeDamageSafety.YES : UpgradeDamageSafety.NO,
                type, value, kind, condition, newComponent, component, scales, displaySafety, UpgradeDamageSafety.NO, description);
    }

    private record Node(SkillUpgrade upgrade, List<UpgradeDamageModifier> modifiers, UpgradeDamageImpact impact) { }
}
