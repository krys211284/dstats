# Raport wdrożenia N4 — Macabre

N4 dodaje kompletną Makabrę do istniejącego rejestru Nekromanty: 4 nowe skille i 28 ulepszeń. Stan katalogu: 17 skilli / 119 ulepszeń, kolejność Basic -> Core -> Corpse -> Macabre. Dane wcześniejszych 13 skilli / 91 ulepszeń i 24 skilli Paladyna są zachowane. Baseline: Sezon 15 / patch 3.2.1.

## 1. Zmienione i dodane pliki

Lista obejmuje dokładnie 19 plików względem migawki początku N4, a nie git HEAD. Wcześniejsze zmiany N0–N3 i zmiany użytkownika pozostały zachowane.

Zmodyfikowane (13):

- `src/main/java/krys/necromancer/NecromancerSkillTreeRegistry.java`
- `src/main/java/krys/skilltree/CharacterTreeSkill.java`
- `src/main/java/krys/web/DamageRankingPageRenderer.java`
- `src/main/java/krys/web/DamageRankingRow.java`
- `src/main/java/krys/web/DamageRankingSearchText.java`
- `src/test/java/krys/necromancer/NecromancerCoreSkillTreeRegistryTest.java`
- `src/test/java/krys/necromancer/NecromancerCorpseSkillTreeRegistryTest.java`
- `src/test/java/krys/necromancer/NecromancerSkillTreeRegistryTest.java`
- `src/test/java/krys/necromancer/NecromancerUpgradeGroupMembershipTest.java`
- `src/test/java/krys/skilltree/CharacterTreeSkillMetadataTest.java`
- `src/test/java/krys/web/NecromancerCoreDamageRankingWebServerTest.java`
- `src/test/java/krys/web/NecromancerCorpseDamageRankingWebServerTest.java`
- `README.md`

Dodane (6):

- `src/main/java/krys/skilltree/SkillChargeData.java`
- `src/test/java/krys/necromancer/NecromancerMacabreSkillTreeRegistryTest.java`
- `src/test/java/krys/skilltree/SkillChargeDataTest.java`
- `src/test/java/krys/web/NecromancerMacabreDamageRankingWebServerTest.java`
- `docs/necromancer/macabre-source-data.md`
- `docs/necromancer/n4-implementation-report.md`

## 2. Finalny SkillChargeData

Neutralny immutable record `SkillChargeData(int baseCharges, int chargeCooldownSeconds)`. Oba pola muszą być > 0; 0 i wartości ujemne powodują IllegalArgumentException. `CharacterTreeSkill` ma nullable `chargeData` i getter. Zachowano wcześniejsze konstruktory, delegujące do nowego z null. Wszystkie wcześniejsze 37 wpisów obu katalogów zachowują null.

## 3. Cooldown i odnowienie ładunków

Zwykły bazowy cooldown pozostaje `Integer cooldownSeconds`; model ładunków ma odrębny czas odnowienia pojedynczego ładunku. Konstruktor odrzuca ich równoczesną obecność. Kościane Widmo ma cooldownSeconds=null i chargeData=2/12.

Neutralny `DamageRankingRow` przenosi dane ładunków ze skilla. Istniejąca kolumna Odnowienie pokazuje 24 s / 15 s / `2 ładunki; 12 s / ładunek` / 16 s. Tooltip ładunków pokazuje alternatywy Ładunki (3/10) i Umiejętność Główna (bez charge recharge tylko w wariancie). Nie dodano kolumny. Wyszukiwanie indeksuje nazwę, liczbę i charge cooldown. Ranking nie traktuje 12 s jako zwykłego cooldownu.

## 4. Dokładne dane bazowe Macabre

Wszystkie: NEEDS_VERIFICATION, bazowo SkillResourceData.EMPTY i puste activationRequirements. Kategoria `MACABRE / Makabra / displayOrder=3`, grupa `macabre / Makabra / Macabre`.

| Skill | Typ / tagi | R1 / max-view | Lucky Hit | Odnowienie |
|---|---|---|---|---|
| Krwawa Mgła | DAMAGE; MACABRE/BLOOD/PHYSICAL | DAMAGE_OVER_DURATION 420/1071 przez 3 s | 9% | 24 s |
| Więzienie z Kości | DEFENSIVE; MACABRE/BONE | brak współczynnika i obu bazowych tabel | null | 15 s |
| Kościane Widmo | DAMAGE; MACABRE/BONE/PHYSICAL | baza 150/383 | 33% | 2 ładunki / 12 s; zwykły cooldown null |
| Golem | DAMAGE; MACABRE/MINION/SUMMON | baza 125/319 za atak jednego Golema | 20% | 16 s aktywnego rozkazu |

Wszystkie tabele mają tylko punkty 1 i 15, bez interpolacji. Max-view pod 15 zachowuje opisane zastrzeżenie braku jawnej etykiety rangi w D4Builds. Pełne źródła i 28 node'ów opisuje [macabre-source-data.md](macabre-source-data.md).

## 5. Cztery mapy 2 + 2 + 3

| Skill | Grupa 1 | Grupa 2 | Grupa 3 / Transformation |
|---|---|---|---|
| Krwawa Mgła | Szybkość Ruchu; Trafienie Krytyczne | Przytłoczenie; Odsłonięcie i Spowolnienie | Transfuzja Krwi; Przypływ Krwi; Pochłaniająca Mgła |
| Więzienie z Kości | Generowanie Esencji; Redukcja Czasu Odnowienia | Animusz; Odsłonięcie | Cierń; Bezdenna Ciemność; Uwięzienie Życia |
| Kościane Widmo | Ładunki; Umiejętność Główna | Maksymalna Esencja; Premia do Obrażeń | Poltergeisty; Niedokończone Porachunki; Projekcja Astralna |
| Golem | Nieustępliwość; Premia do Obrażeń | Osłabienie; Animusz, Przytłoczenie lub Zajadłość | Grobowy Kwiat; Plugawa Żarłoczność; Gigantyczność |

Golden membership sprawdza wszystkie 17 skilli: exact SkillUpgradeGroup membership, każdy UpgradeDamageModifier.upgradeGroup i UpgradeDamageImpact.groupId, rozłączność i dokładnie 12 kombinacji per skill. Poprzednie zbiory golden nie zmieniły się. Źródłem struktury jest [jawne Group 1 / Group 2 / T Icy Veins](https://www.icy-veins.com/d4/guides/necromancer-skills/).

## 6. Blood Mist jako damage-over-duration

Prosta baza i oba proste procenty są puste/null. Bazowy komponent `DAMAGE_OVER_DURATION {1:420,15:1071}`, duration 3, opisuje cały okres Immune 3 s. Leczenie R1 6.0% / max-view 15.3% maksymalnego zdrowia jest w notes, bez tabeli healing.

Pochłaniająca Mgła ma własną tabelę DAMAGE_OVER_DURATION 600/1530 przez 3 s i zastępuje bazę. Nie powstaje hit 420%, suma 420+600 ani procent na sekundę. Przytłoczenie po teleportacji jest zależne od Przypływu Krwi.

## 7. Blood Rush: Zwłoki i cooldown wariantu

Przypływ Krwi ma RESOURCE_OR_COST „Zużywa 1 pobliskie Zwłoki” i CAST_SPEED_OR_COOLDOWN „Odnowienie wariantu: 8 s”. Także Mobility; zamiast mgły teleportuje do celu, Unstoppable R1 3 s / max-view 4.2 s w notes. Tooltip utility pokazuje koszt i cooldown wariantu. Bazowe 24 s i puste wymagania Krwawej Mgły pozostają zachowane. Nie ma override engine.

## 8. Bazowe Bone Prison i Plunging Darkness

Więzienie ma typ DEFENSIVE, Lucky Hit null i brak obrażeń bazowych: SSR „nie dotyczy”. Czas więzienia R1 6 s / max-view 8.4 s i Life utility są w notes, bez sztucznej tabeli damage. Esencja +50 / dodatkowo +25 za Bossa pochodzi wyłącznie z upgrade'u; redukcja 0.5 s za wroga do 5 s dotyczy aktywnych cooldownów gracza.

Cierń to THORNS_DAMAGE_MODIFIER / TEXT_ONLY: 100% własnych Cierni, X za wroga zależne od poziomu, bez współczynnika broni i bez tabeli. Prezentacja Manual review zachowuje istniejącą granicę modelu Cierni.

Bezdenna Ciemność tworzy nowy komponent: DAMAGE_OVER_TIME z tabelą PRIMARY_DAMAGE {1:450,15:1148}. R1 450 rozstrzyga [oficjalny patch Blizzard 3.2.1, 300 -> 450](https://news.blizzard.com/en-us/article/24295394/celebrate-30-years-of-diablo-in-season-of-hell-s-legacy). Brak potwierdzonego czasu współczynnika: duration null, bez utożsamiania go z czasem więzienia i bez DPS. Uwięzienie Życia przechowuje premie generowania/Cast Speed R1 +20% / max-view +51% i stratę 5% zdrowia/sek. jako warunkowe metadata.

## 9. Bone Spirit i Core Skill

Bazowa tabela 150/383, 2 ładunki / 12 s. Ładunki opisują 3/10 tylko dla wariantu. Umiejętność Główna jest Enhancement grupy 1; cztery modyfikatory opisują koszt 30 Esencji, zużycie całej pozostałej Esencji, usunięcie cooldownu/charge recharge tylko w wariancie i +3%[x] za zużyty punkt. Bazowe chargeData, resourceData i kategoria MACABRE pozostają zachowane. Nie zakładamy ilości zużytej Esencji ani automatycznego multiplier.

Premia do Obrażeń +10%[x]/sek. do +30%[x] jest warunkowa. Poltergeisty: dodatkowy ADDITIONAL_STRIKE_DAMAGE 30/77 za małego ducha i osobny count 3. Niedokończone Porachunki: pozostawiają bazę, dodatkowy PRIMARY_DAMAGE 250/638, DAMAGE_OVER_TIME bez pewnego duration. Projekcja Astralna: PRIMARY_DAMAGE 100/255 zastępuje bazę. Nie sumujemy 3 duchów, 150+250 ani 150+100.

## 10. Golem, Gravebloom i Fel Gluttony

Baza 125/319 dotyczy pojedynczego pasywnego ataku jednego Golema. Aktywny rozkaz ma 16 s: skok, Unstoppable Golema, Taunt, otrzymywane obrażenia -30% przez 6 s, Ciernie równe 50% pancerza przez ten czas. Respawn 10 s jest osobny. Icy Veins opisuje zmienny Physical lub Shadow / Bone, Blood lub Darkness zależnie od Book of the Dead; tylko MACABRE/MINION/SUMMON są bezwarunkowymi tagami.

Grobowy Kwiat zapisuje trzy mniejsze Golemy, TEXT_ONLY „60% normalnych obrażeń” za każdego i Attack Speed +30%[+]. Bez tabeli 60% Weapon Damage i bez mnożenia liczby sług. Plugawa Żarłoczność dodaje BURST_DAMAGE 300/765 erupcji; pasywne zużywanie Zwłok co sekundę i -2 s cooldown za zużyte Zwłoki to osobne modyfikatory wariantu, bez bazowego wymagania Zwłok i bez założenia ich dostępności. Gigantyczność opisuje aurę INNYCH sług R1 +20%[x] / max-view +51%[x], bez zmiany bazowej tabeli.

## 11. HTTP, SSR, filtry i wyszukiwanie

HTTP 200: Nekromanta 17 wierszy (4/6/3/4), oba filtry `skillGroup=macabre` i `sourceCategory=MACABRE` po 4. Basic/Core/Corpse zachowują 4/6/3. SSR: pełny okres Krwawej Mgły, nieobrażeniowe Więzienie bez Lucky Hit, 2/12 ładunków Widma, baza jednego Golema i cooldown aktywny 16 s.

Search znajduje wszystkie 119 polskich nazw upgrade'ów; test Macabre dodatkowo chroni 28 nowych nazw, „ładunki”, „2 ładunki”, „12 s / ładunek”, Przypływ Krwi, Bezdenną Ciemność, Plugawą Żarłoczność i Grobowy Kwiat. Fraza „Odnowienie 12 s” nie jest zwykłym bazowym cooldownem Widma.

## 12. Regresja 17/119 i zachowanie poprzedniego katalogu

Nekromanta: dokładnie 17 skilli / 119 upgrade'ów; Macabre 4/28. Porównanie definicji wszystkich 13 wcześniejszych skilli z migawką początku N4, po normalizacji końców linii, potwierdziło identyczność. Stare golden liczb, opisów, membership, konfliktów, komponentów i HTTP pozostają aktywne. Helper źródłowy rejestru uogólniono tylko o nullable Lucky Hit, typ i opcjonalne chargeData; stare wywołania zachowują DAMAGE i null ładunków.

## 13. Testy

Wybrane regresje: `mvn '-Dmaven.repo.local=.m2' '-Dtest=SkillChargeDataTest,CharacterTreeSkillMetadataTest,Necromancer*Test,DamagePercent*Test,DamageRanking*Test,Paladin*AuditTest,Neutral*Test,CharacterSkillTreeRegistryTest' clean test`: **BUILD SUCCESS, 147 testów, 0 failures, 0 errors, 0 skipped**, 14.256 s. Modele, wszystkie golden grupy i liczby, HTTP, wspólny neutralny ranking i regresje Paladyna.

Pełny `mvn '-Dmaven.repo.local=.m2' clean test`: **BUILD SUCCESS, 711 testów w 113 zestawach, 0 failures, 0 errors, 0 skipped**, czas **22 min 53 s**. Zakończony 07.10.2026 o 09:21:42 CEST. Maven skompilował od zera 345 plików Java produkcyjnych i 115 testowych. Sumy potwierdzono niezależnie z XML Surefire, w tym 11 testów rejestru Macabre, 18 golden membership, 8 modelu ładunków i 5 HTTP Macabre.

Wszystkie 509 plików migawki kodu, testów i dokumentacji pozostawały niezmienione przez pełny przebieg. Po jego zakończeniu dopisano wyłącznie wynik walidacji w tym raporcie. Kontrola zakresu potwierdziła identyczność 490 wcześniejszych plików poza zmianami N4 i brak usuniętych plików. `git diff --check` przechodzi.

## 14. Paladyn

24 skille zachowane. Pliki domeny Paladyna, adapter, źródła, tabele, grupy i runtime są identyczne z początkiem N4 według hashy; wybrane audyty i dotychczasowy HTTP przechodzą. Domyślna klasa i odnowienia/wymagania wcześniejszych wpisów nie zmieniły się.

## 15. Runtime i current build

Bez zmian HeroClass, SkillId, DamageEngine, ManualSimulationService, PaladinSkillDefs, current build i search build. Hashe plików poza wskazaną listą N4 pozostają zgodne z początkiem zadania. Wszystkie nowe modyfikatory mają safeForRuntimeDps=NO. Ranking działa bez instancji engine; damagePerUse, effectiveCycleSeconds i theoreticalDps są null dla wszystkich 17 skilli i każdej metryki. Nie wyliczano DPS z cooldownu ani charge cooldownu, sum duchów/Golemów, automatycznych premii lub dostępnych Zwłok. Rangi 2..14 pozostają nieznane.

## 16. ZIP

ZIP nie został utworzony. Użyto fazy clean test; nie uruchamiano fazy package ani assembly. Po pełnym przebiegu testów katalog target nie zawiera ZIP-ów.
