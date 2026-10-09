# Macabre Nekromanty — dane źródłowe N4

Baseline: Diablo IV Sezon 15, patch 3.2.1. Odczyt: 07.10.2026. N4 dodaje Makabrę do istniejącego katalogu: po 4 Basic, 6 Core i 3 Corpse następują 4 Macabre, łącznie 17 aktywnych skilli i 119 ulepszeń. Macabre ma 28 ulepszeń. Dane wcześniejszych 13 skilli i 91 ulepszeń pozostają zachowane.

## Źródła i pierwszeństwo

1. [Oficjalny Blizzard — Sezon 15 / patch 3.2.1](https://news.blizzard.com/en-us/article/24295394/celebrate-30-years-of-diablo-in-season-of-hell-s-legacy), sekcja Bone Prison: Plunging Darkness 300% -> 450%.
2. D4Guides EN, jawny tekst R1: [Blood Mist](https://d4guides.gg/en/database/skills/blood-mist), [Bone Prison](https://d4guides.gg/en/database/skills/bone-prison), [Bone Spirit](https://d4guides.gg/en/database/skills/bone-spirit), [Golem](https://d4guides.gg/en/database/skills/golem).
3. D4Guides PL, nazwy: [Krwawa Mgła](https://d4guides.gg/pl/baza-danych/umiejetnosci/krwawa-mgla), [Więzienie z Kości](https://d4guides.gg/pl/baza-danych/umiejetnosci/wiezienie-z-kosci), [Kościane Widmo](https://d4guides.gg/pl/baza-danych/umiejetnosci/kosciane-widmo), [Golem](https://d4guides.gg/pl/baza-danych/umiejetnosci/golem). PL może przedstawiać obrażenia jako punkty zależne od postaci; nie przeliczamy ich na współczynniki. Kościane Widmo odczytano także przez indeks wyszukiwania tej samej strony po błędzie jej bezpośredniego otwarcia.
4. [Icy Veins — Necromancer Skills](https://www.icy-veins.com/d4/guides/necromancer-skills/), rozstrzygające jawne Group 1, Group 2 i T oraz Lucky Hit i semantyka pasywnego Golema.
5. [Główna domena D4Builds Class Skills](https://d4builds.gg/database/class-skills/), wyłącznie max-view. Bez wykorzystywania danych starych subdomen/branchy.

Hierarchia danych: oficjalny patch Blizzard, aktualny jawny tekst D4Guides/Icy Veins, główna domena D4Builds dla max-view, nierozstrzygnięty konflikt opisany i oznaczony do ręcznej weryfikacji. Membership pochodzi z jawnych grup Icy Veins, nie z kolejności list D4Guides. Max-view nie ma jawnej etykiety Rank 15: przypisanie pod rangą 15 jest istniejącą konwencją projektu wymagającą weryfikacji. Tabele przechowują tylko 1 i 15; 2..14 pozostają nieznane.

Bezpośrednie otwarcie D4Builds zwraca powłokę aplikacji; wartości odczytano z indeksowanej treści głównego adresu. Bazowe EN Golema nie udostępniło treści narzędziu; opis aktywny i polskie nazwy potwierdza PL, pasywny atak, Lucky Hit, grupy i zmienność typu potwierdza Icy Veins. Nie dodano z tego powodu zgadywanych wartości ani danych starych stron. Potwierdzone wartości są zgodne z golden N4 przekazanymi w zadaniu.

## Katalog i odnowienie

Kategoria `SourceSkillCategory("MACABRE", "Makabra", 3)`, grupa `macabre / Makabra / Macabre`. Wszystkie skille mają `NEEDS_VERIFICATION`. Bazowe wymagania aktywacji są puste, a `SkillResourceData.EMPTY` nie zawiera kosztu ani generowania Esencji.

| Id / skill | Typ | Bezpieczne tagi | R1 / max-view | Lucky Hit | Zwykły cooldown | Ładunki |
|---|---|---|---|---|---|---|
| krwawa_mgla / Krwawa Mgła | DAMAGE | MACABRE, BLOOD, PHYSICAL | 420% / 1071% łącznie przez 3 s | 9% | 24 s | null |
| wiezienie_z_kosci / Więzienie z Kości | DEFENSIVE | MACABRE, BONE | brak bazowego współczynnika | null | 15 s | null |
| kosciane_widmo / Kościane Widmo | DAMAGE | MACABRE, BONE, PHYSICAL | 150% / 383% za eksplozję | 33% | null | 2; odnowienie ładunku 12 s |
| golem / Golem | DAMAGE | MACABRE, MINION, SUMMON | 125% / 319% za atak jednego Golema | 20% | 16 s aktywnego rozkazu | null |

Neutralny immutable record `SkillChargeData(int baseCharges, int chargeCooldownSeconds)` wymaga obu wartości > 0. Pole `CharacterTreeSkill.chargeData` jest domyślnie null; dotychczasowe skille zachowują null. Konstruktor odrzuca jednoczesny zwykły cooldown i dane ładunków. Widmo ma bazowo wyłącznie 2/12, nawet jeśli ogólna rubryka strony także nazywa 12 s „Cooldown”. Jawne Charges / Charge Cooldown określa mechanikę.

SSR w istniejącej kolumnie Odnowienie: 24 s / 15 s / `2 ładunki; 12 s / ładunek` / 16 s. Tooltip ładunków pokazuje alternatywne ulepszenia; ich wybór nie mutuje bazowego metadata. Search indeksuje ładunki, bazową liczbę i odnowienie. Żaden z tych parametrów nie wypełnia damagePerUse, effectiveCycleSeconds ani theoreticalDps.

## Dokładne mapy 2 + 2 + 3

| Skill | Grupa 1 — 2 Enhancements | Grupa 2 — 2 Enhancements | Grupa 3 — 3 Transformation variants |
|---|---|---|---|
| Krwawa Mgła | Szybkość Ruchu; Trafienie Krytyczne | Przytłoczenie; Odsłonięcie i Spowolnienie | Transfuzja Krwi; Przypływ Krwi; Pochłaniająca Mgła |
| Więzienie z Kości | Generowanie Esencji; Redukcja Czasu Odnowienia | Animusz; Odsłonięcie | Cierń; Bezdenna Ciemność; Uwięzienie Życia |
| Kościane Widmo | Ładunki; Umiejętność Główna | Maksymalna Esencja; Premia do Obrażeń | Poltergeisty; Niedokończone Porachunki; Projekcja Astralna |
| Golem | Nieustępliwość; Premia do Obrażeń | Osłabienie; Animusz, Przytłoczenie lub Zajadłość | Grobowy Kwiat; Plugawa Żarłoczność; Gigantyczność |

Golden membership obejmuje wszystkie 17 skilli: dokładne zbiory, wszystkie modyfikatory node'a i impact, rozłączność oraz 12 kombinacji wyborów po jednej alternatywie z każdej grupy. Umiejętność Główna Widma należy do grupy 1 mimo zmiany profilu skilla.

## Krwawa Mgła

Bazowo Immune przez 3 s i łączne obrażenia za ten okres. Prosta tabela i oba proste procenty są puste/null. `componentDamagePercentRanks` zawiera wyłącznie `DAMAGE_OVER_DURATION: {1:420, 15:1071}`, duration 3. Leczenie R1 6.0% / max-view 15.3% maksymalnego zdrowia przez czas działania pozostaje w notes, bez healing rank-table.

| Grupa / id / nazwa | Semantyka modyfikatora |
|---|---|
| 1 / szybkosc_ruchu / Szybkość Ruchu | DEFENSE_OR_UTILITY: Movement Speed +30%[x], 3 s |
| 1 / trafienie_krytyczne / Trafienie Krytyczne | STATUS_DAMAGE_ENABLER: szansa krytyczna +5%[+], 5 s po zakończeniu |
| 2 / przytloczenie / Przytłoczenie | STATUS_DAMAGE_ENABLER: 1 kumulacja Overpower co sekundę; tekst D4Guides 3.2.1 dodaje 1 po teleportacji wyłącznie z Przypływem Krwi |
| 2 / odsloniecie_i_spowolnienie / Odsłonięcie i Spowolnienie | STATUS_DAMAGE_ENABLER: Vulnerable + Slow 70%, 4 s |
| 3 / transfuzja_krwi / Transfuzja Krwi | DEFENSE_OR_UTILITY: Fortify 30% maks. zdrowia przez czas działania; Krwawa Kula co sekundę; baza bez zmian |
| 3 / przyplyw_krwi / Przypływ Krwi | Mobility, zamiast mgły teleport do celu, RESOURCE_OR_COST „Zużywa 1 pobliskie Zwłoki”, CAST_SPEED_OR_COOLDOWN „Odnowienie wariantu: 8 s”, Unstoppable R1 3 s / max-view 4.2 s |
| 3 / pochlaniajaca_mgla / Pochłaniająca Mgła | Darkness / Shadow i Vampiric Curse; DAMAGE_OVER_TIME z tabelą DAMAGE_OVER_DURATION 600/1530 przez 3 s zastępuje bazowy profil |

Przypływ nie dodaje Zwłok do bazowej activationRequirements i nie zmienia bazowych 24 s. Oba jego wymagania wariantu są widoczne także w tooltipie utility. Nie wykonujemy variant override engine ani 420 + 600.

## Więzienie z Kości

Typ DEFENSIVE, brak Lucky Hit, brak obu bazowych tabel i brak prostych procentów. Źródłowe Life więzienia jest utility. Czas działania R1 6 s / max-view 8.4 s w notes, bez interpolacji i tabeli obrażeń. Odnowienie 15 s.

| Grupa / id / nazwa | Semantyka modyfikatora |
|---|---|
| 1 / generowanie_esencji / Generowanie Esencji | RESOURCE_OR_COST: użycie +50 Esencji; dodatkowo +25 po uwięzieniu Bossa; bez bazowej generacji |
| 1 / redukcja_czasu_odnowienia / Redukcja Czasu Odnowienia | CAST_SPEED_OR_COOLDOWN: aktywne cooldowny gracza -0.5 s za uwięzionego wroga, do -5 s |
| 2 / animusz / Animusz | STATUS_DAMAGE_ENABLER: 1 Resolve za uwięzionego wroga |
| 2 / odsloniecie / Odsłonięcie | STATUS_DAMAGE_ENABLER: Vulnerable 8 s |
| 3 / ciern / Cierń | THORNS_DAMAGE_MODIFIER: X Cierni za wroga; segment zniszczony/wygasły zadaje 100% twoich Cierni; TEXT_ONLY, bez współczynnika broni |
| 3 / bezdenna_ciemnosc / Bezdenna Ciemność | Darkness; Zwłoki co sekundę; nowy komponent Zbezczeszczonej Ziemi 450/1148 |
| 3 / uwiezienie_zycia / Uwięzienie Życia | Blood i więzienie wokół gracza; wewnątrz generowanie Esencji i Cast Speed R1 +20% / max-view +51%; utrata 5% maks. zdrowia/sek. |

Bezdenna Ciemność ma `DAMAGE_OVER_TIME`, `createsNewDamageComponent=true` i tabelę wariantu `PRIMARY_DAMAGE: {1:450,15:1148}`. Oficjalny patch 3.2.1 rozstrzyga R1 300 -> 450. Neutralny PRIMARY_DAMAGE określa procent komponentu tego wariantu, nie tick, uderzenie ani procent na sekundę. Źródło nie podaje pewnego duration tego współczynnika; nie przypisujemy mu czasu więzienia. Nie stosujemy DAMAGE_OVER_DURATION bez czasu ani DAMAGE_PER_SECOND.

Cierń nie ma absolutnej tabeli i impact.damagePercent jest null. X różni się z poziomem źródłowej postaci, nie jest stałym coefficient. Typ Cierni korzysta z istniejącej prezentacji Manual review (`safeForRankingDisplay=NEEDS_MANUAL_REVIEW`), a runtime zawsze NO. To potrzeba odrębnej interpretacji Cierni, nie nierozstrzygnięty konflikt wartości 450.

## Kościane Widmo

Sparse baza `{1:150,15:383}`, 2 ładunki / 12 s za ładunek, bez zwykłego odnowienia. Zasób bazowy pusty. Kategorie i metadane bazowe nie są mutowane przez ulepszenia.

| Grupa / id / nazwa | Semantyka modyfikatora |
|---|---|
| 1 / ladunki / Ładunki | CAST_SPEED_OR_COOLDOWN: +1 ładunek; 2 -> 3 i charge cooldown 12 -> 10 s; baza 2/12 |
| 1 / umiejetnosc_glowna / Umiejętność Główna | staje się Core; RESOURCE_OR_COST: 30 Esencji i cała pozostała Esencja jako dwa efekty; CAST_SPEED_OR_COOLDOWN: wariant bez cooldownu/charge recharge; MULTIPLICATIVE_DAMAGE_PERCENT: +3%[x] za zużyty punkt |
| 2 / maksymalna_esencja / Maksymalna Esencja | RESOURCE_OR_COST: +2 maksymalnej Esencji za wroga, 15 s, do +20; bez zmiany bazowego kosztu/generowania |
| 2 / premia_do_obrazen / Premia do Obrażeń | MULTIPLICATIVE_DAMAGE_PERCENT: następne Widmo +10%[x] co sekundę do +30%[x], wyłącznie warunkowo |
| 3 / poltergeisty / Poltergeisty | dodatkowy ADDITIONAL_STRIKE_DAMAGE 30/77 za małego ducha; osobny HIT_COUNT = 3 |
| 3 / niedokonczone_porachunki / Niedokończone Porachunki | Darkness / Shadow; baza pozostaje; dodatkowy PRIMARY_DAMAGE 250/638 Zbezczeszczonej Ziemi, DAMAGE_OVER_TIME bez pewnego duration |
| 3 / projekcja_astralna / Projekcja Astralna | duch nie szuka, porusza się po spirali wokół gracza, przebija i eksploduje przy trafieniu; PRIMARY_DAMAGE 100/255 zastępuje bazę |

Koszt i mnożnik Umiejętności Głównej są zależne od wariantu i liczby zużytych punktów, bez automatycznych obliczeń. Jej pozycja w grupie 1 jest chroniona golden testem. Nie sumujemy trzech Poltergeistów ani 150 + 250 / 150 + 100. Czas Zbezczeszczonej Ziemi pozostaje nieznany.

## Golem

Pasywnie przywołuje jednego Golema; baza `{1:125,15:319}` opisuje jeden atak jednego Golema. Po śmierci odradza się po 10 s. Aktywny rozkaz z odnowieniem 16 s: skok do celu, Unstoppable Golema, Taunt, otrzymywane obrażenia -30% przez 6 s i Ciernie równe 50% pancerza przez ten czas.

Icy Veins opisuje Physical or Shadow i Bone/Blood/Darkness zależne od Book of the Dead / wariantu. Ogólny Physical w D4Guides/D4Builds nie daje bezwarunkowego tagu. Bezpieczne tagi: tylko MACABRE/MINION/SUMMON.

| Grupa / id / nazwa | Semantyka modyfikatora |
|---|---|
| 1 / nieustepliwosc / Nieustępliwość | DEFENSE_OR_UTILITY: rozkaz daje także GRACZOWI Unstoppable 3 s |
| 1 / premia_do_obrazen / Premia do Obrażeń | MULTIPLICATIVE_DAMAGE_PERCENT: +30%[x] przeciw Vulnerable, warunkowo; tooltip przypomina jednostkę jednego ataku |
| 2 / oslabienie / Osłabienie | STATUS_DAMAGE_ENABLER: każdy atak Weakens na 4 s |
| 2 / animusz_przytloczenie_lub_zajadlosc / Animusz, Przytłoczenie lub Zajadłość | STATUS_DAMAGE_ENABLER: rozkaz daje 3 kumulacje; Bone -> Resolve, Blood -> Overpower, Iron -> Ferocity |
| 3 / grobowy_kwiat / Grobowy Kwiat | zamiast jednego 3 mniejsze; osobny count 3, TEXT_ONLY „60% normalnych obrażeń” za każdego i Attack Speed +30%[+] |
| 3 / plugawa_zarlocznosc / Plugawa Żarłoczność | dodatkowy BURST_DAMAGE 300/765 erupcji przy rozkazie; pasywne zużywanie pobliskich Zwłok co sekundę i aktywne odnowienie -2 s za zużyte Zwłoki |
| 3 / gigantycznosc / Gigantyczność | jeden większy Golem; aura Cast Speed i Movement Speed INNYCH sług R1 +20%[x] / max-view +51%[x] |

Gravebloom nie tworzy tabeli 60% Weapon Damage ani wyniku 3 × 125 / 3 × 60%. Fel Gluttony nie dodaje bazowego wymagania Zwłok i nie zakłada ich dostępności; aktywny cooldown nadal 16 s. Gigantyczność nie zmienia 125/319. Nie mnożymy bazowego procentu przez liczbę sług ani nie traktujemy respawn jako cooldownu.

## Tabele wariantów i granica runtime

| Wariant | Komponent | R1 | Max-view pod 15 | Duration | Relacja do bazy |
|---|---|---:|---:|---|---|
| Pochłaniająca Mgła | DAMAGE_OVER_DURATION | 600 | 1530 | 3 s | zastępuje |
| Bezdenna Ciemność | PRIMARY_DAMAGE | 450 | 1148 | nieznany, null | nowy |
| Poltergeisty | ADDITIONAL_STRIKE_DAMAGE | 30 | 77 | brak czasu | dodatkowy, za ducha |
| Niedokończone Porachunki | PRIMARY_DAMAGE | 250 | 638 | nieznany, null | dodatkowy |
| Projekcja Astralna | PRIMARY_DAMAGE | 100 | 255 | brak czasu | zastępuje |
| Plugawa Żarłoczność | BURST_DAMAGE | 300 | 765 | brak czasu | nowy |

Wszystkie nowe modyfikatory: `safeForRuntimeDps=NO`. Wszystkie nowe tabele: tylko 1/15, bez interpolacji. Wszystkie metryki runtime puste, także przy rankingu bez instancji engine. HeroClass, SkillId, DamageEngine, ManualSimulationService, PaladinSkillDefs, search build i current build nie zostały rozszerzone. ZIP nie powstaje.

HTTP/search chronią 17/119, filtry Macabre 4/4, dotychczasowe 13/91 i Paladyna 24. Wszystkie polskie nazwy 119 upgrade'ów są wyszukiwalne. Testy w repo: SkillChargeDataTest, NecromancerMacabreSkillTreeRegistryTest, NecromancerMacabreDamageRankingWebServerTest i rozszerzony NecromancerUpgradeGroupMembershipTest; pozostałe golden i regresje pozostają wymagane.
