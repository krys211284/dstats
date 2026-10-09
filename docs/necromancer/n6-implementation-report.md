# N6 — raport wdrożenia Mocy Specjalnych / Ultimate

Baseline: Sezon 15 / patch 3.2.1; patch 3.2.2 bez zmian użytych danych.
Stan po N6: **23/23 aktywne skille Nekromanty, 161 ulepszeń**; **24 skille Paladyna**.
Wcześniejsze 19 skilli / 133 ulepszenia zachowują dane i dokładne grupy.
Źródła i wszystkie metadane: [ultimate-source-data.md](ultimate-source-data.md).

## 1. Pliki zmienione i dodane

Lista dotyczy N6 względem zastanego, zweryfikowanego stanu po N5, a nie całego dirty diff repo.
**24 pliki: 20 zmienionych, 4 dodane.** Nie usunięto plików.

| Plik | Zmiana |
| --- | --- |
| `docs/necromancer/ultimate-source-data.md` | dodany |
| `docs/necromancer/n6-implementation-report.md` | dodany |
| `README.md` | zmieniony |
| `src/main/java/krys/necromancer/NecromancerSkillTreeRegistry.java` | zmieniony |
| `src/main/java/krys/skilltree/SkillResourceGenerationFrequency.java` | zmieniony |
| `src/main/java/krys/web/DamageRankingPageRenderer.java` | zmieniony |
| `src/main/java/krys/web/DamageRankingRow.java` | zmieniony |
| `src/test/java/krys/necromancer/NecromancerCoreSkillTreeRegistryTest.java` | zmieniony |
| `src/test/java/krys/necromancer/NecromancerCorpseSkillTreeRegistryTest.java` | zmieniony |
| `src/test/java/krys/necromancer/NecromancerCurseSkillTreeRegistryTest.java` | zmieniony |
| `src/test/java/krys/necromancer/NecromancerMacabreSkillTreeRegistryTest.java` | zmieniony |
| `src/test/java/krys/necromancer/NecromancerSkillTreeRegistryTest.java` | zmieniony |
| `src/test/java/krys/necromancer/NecromancerUltimateSkillTreeRegistryTest.java` | dodany |
| `src/test/java/krys/necromancer/NecromancerUpgradeGroupMembershipTest.java` | zmieniony |
| `src/test/java/krys/skilltree/CharacterTreeSkillMetadataTest.java` | zmieniony |
| `src/test/java/krys/skilltree/SkillChargeDataTest.java` | zmieniony |
| `src/test/java/krys/skilltree/SkillResourceDataTest.java` | zmieniony |
| `src/test/java/krys/web/DamageRankingWebServerTest.java` | zmieniony |
| `src/test/java/krys/web/NecromancerCoreDamageRankingWebServerTest.java` | zmieniony |
| `src/test/java/krys/web/NecromancerCorpseDamageRankingWebServerTest.java` | zmieniony |
| `src/test/java/krys/web/NecromancerCurseDamageRankingWebServerTest.java` | zmieniony |
| `src/test/java/krys/web/NecromancerMacabreDamageRankingWebServerTest.java` | zmieniony |
| `src/test/java/krys/web/NecromancerUltimateDamageRankingWebServerTest.java` | dodany |
| `src/test/java/krys/web/NeutralDamageRankingWebServerTest.java` | zmieniony |

Starsze testy dostosowują bieżącą liczebność do 23/161, zachowują zakresy historyczne
i dodają cztery wpisy niezależnego golden membership. Testy Paladyna nadal chronią te same dane i komponenty;
odróżniają metadane tooltipu od faktycznie renderowanych komponentów.

## 2. SkillResourceGenerationFrequency

Enum zawiera teraz `PER_USE`, `PER_SECOND` i neutralne `PER_EVENT`.
Nowa wartość oznacza ilość zasobu na jedno zdarzenie określone w źródłowych notes.
Nie zmieniono SkillResourceData, jego walidacji, domyślnego PER_USE ani konstruktorów kompatybilności.
Wspólny DamageRankingRow dodaje jednostkę „ / zdarzenie” w switchu, bez zależności od klasy.

## 3. Spętanie Dusz — zasób per event

`generationMin=2`, `generationMax=2`, `generationBonusKnown=null`, `cost=null`, `frequency=PER_EVENT`.
Zdarzenie: **pochłonięcie jednej duszy przeciwnika**.
Notes: absorpcja/sprawdzanie co 0.25 s, dusze 3 przeciwników, z Bossów 3 dusze jednocześnie.
SSR: **2 / zdarzenie**, bez / sek. i zagregowanego resource rate.
Facet uznaje generation i zachowuje wcześniejszą regułę RESOURCE_OR_COST ulepszeń
(w Ultimate obejmuje również koszt Hematolagnii i zużycie Zwłok Nawałnicy).
Oba sortowania porównują źródłową liczbę 2 bez uwzględniania częstotliwości; braki liczb pozostają na końcu.
Niezależny fixture z kluczem Paladyna sprawdza SSR, search, facet, row sort values i oba kierunki HTTP sortowania.

## 4. Dane czterech Ultimate

Kategoria: `ULTIMATE / Moce Specjalne / displayOrder=5`; grupa: `ultimate / Moce Specjalne / Ultimate`.
Nazwa z gry ma pierwszeństwo nad label D4Guides „Specjalne”.
Wszystkie cztery: `DAMAGE / NEEDS_VERIFICATION`, cost null, charge data null, puste activation requirements.

| Id / skill | Tagi | Obrażenia R1 / max-view | Cooldown | Lucky Hit | Generation |
| --- | --- | --- | --- | --- | --- |
| armia_umarlych / Armia Umarłych | ULTIMATE, SUMMON, PHYSICAL | 120/306% per explosion | 60 s | 64% | brak |
| fala_krwi / Fala Krwi | ULTIMATE, BLOOD, PHYSICAL | 500/1275% | 50 s | 50% | brak |
| nawalnica_kosci / Nawałnica Kości | ULTIMATE, BONE, PHYSICAL | DAMAGE_OVER_DURATION 400/1020%, 10 s | 60 s | 40% | brak |
| spetanie_dusz / Spętanie Dusz | ULTIMATE, DARKNESS, SHADOW | DAMAGE_OVER_DURATION 350/893%, 8 s | 40 s | 20% | 2 / zdarzenie |

Armia i Fala: proste sparse tabele. Nawałnica i Spętanie: puste proste tabele i null obu głównych procentów,
komponent DAMAGE_OVER_DURATION z duration. Sparse tabele zawierają wyłącznie 1 i 15;
nie interpolowano rang 2..14. Max-view zapisano pod 15 z jawnym zastrzeżeniem interpretacji źródła.

## 5. Wszystkie cztery mapy 2+2+3

| Skill | Grupa 1 | Grupa 2 | Grupa 3 / Transformation |
| --- | --- | --- | --- |
| Armia Umarłych | Redukcja Czasu Odnowienia; Premia do Obrażeń | Pasywna Premia; Generowanie Zwłok | Niezłomne Dowództwo; Piętrzenie Ciał; Martwe Zimno |
| Fala Krwi | Premia do Obrażeń; Redukcja Obrażeń | Przytłoczenie; Krwawa Kula | Krwawy Przypływ; Ścieżka Ciemności; Hematolagnia |
| Nawałnica Kości | Bariera; Odsłonięcie | Redukcja Obrażeń; Wydłużenie Czasu Działania | Rzucone Kości; Cień i Kość; Wygłodniały Cyklon |
| Spętanie Dusz | Szybkość Ruchu; Bariera | Zajadłość; Odsłonięcie i Ograniczanie Kontroli | Destylacja Duszy; Wir Dusz; Zamarznięte Pustkowia |

Golden membership pokrywa 23/23 skille: exact SkillUpgradeGroup, każdy UpgradeDamageModifier.upgradeGroup,
UpgradeDamageImpact.groupId, rozłączność node'ów i dokładnie 12 kombinacji per skill.

## 6. Armia — pojedyncza eksplozja i stacking

Tabela 120/306 opisuje **per explosion; liczba eksplozji nie jest stała**.
Źródłowe 7 s pojawiania się szkieletów, Stun 2 s oraz do 5 stosów z ponownych castów są notes.
Nie sumowano eksplozji, nie mnożono przez casty i nie założono 5 stosów.
Pasywna Premia 1 Volatile Skeleton co 4 s to utility, bez PERIODIC_DAMAGE i bazowej liczby hitów.
Premia +2%[x] za Miniona i Piętrzenie Ciał +10%[x] za eksplozję do 300% są warunkowe.
Martwe Zimno: wariant PRIMARY_DAMAGE 150/383 na eksplozję, replacing base, Cold/Freeze zamiast Stun.
Bazowy cooldown 60 pozostaje mimo modifiera 60 → 40.

## 7. Niezłomne Dowództwo — niepotwierdzony R15

Zachowano R1 +30%[x] damage własnych sług i `scalesWithSkillRank=true`.
Oficjalny Blizzard/Icy Veins potwierdza scaling; główne D4Builds nadal pokazuje 30%.
Nie stworzono tabeli ani wymyślonej wartości rank15.
MULTIPLICATIVE_DAMAGE_PERCENT ma display NEEDS_MANUAL_REVIEW, impact NEEDS_VERIFICATION, runtime NO.
Tooltip manual review pokazuje znane R1 i brak potwierdzonego max-view; 90% redukcji obrażeń sług to osobny utility modifier.

## 8. Fala Krwi i trzy transformations

Krwawy Przypływ: oddzielne ADDITIONAL_STRIKE_DAMAGE 500/1275 i oddzielna liczba +1 fali;
bez 2 × bazowy coefficient.
Ścieżka Ciemności: Darkness/Shadow i dodatkowy Corrupting trail 800/2040,
DAMAGE_OVER_TIME z PRIMARY_DAMAGE bez nieznanego duration.
Hematolagnia: replacing PRIMARY_DAMAGE 300/765, Core, no cooldown, koszt 70 i no Knock Back;
osobne RESOURCE_OR_COST i CAST_SPEED_OR_COOLDOWN. Baza zachowuje cooldown 50 / brak kosztu.
Przytłoczenie ma aktualny opis 1 Overpower / Maximum Overpower +2 z Icy Veins;
notes wskazują, że oficjalna zmiana Blizzard dotyczy Hematolagnii.

## 9. Nawałnica — damage over duration i Zwłoki

DAMAGE_OVER_DURATION 400/1020 przez 10 s, bez 400%/s.
Wydłużenie Czasu Działania: do 4 faktycznie zużytych Zwłok i +2 s za każde jako RESOURCE_OR_COST/duration utility.
Bazowa lista wymagań pusta, bez automatycznych 18 s, dodatkowych ticków i damage total.
Cień i Kość: oddzielne DAMAGE_OVER_DURATION 40/102 przez 2 s poza bazowym komponentem.
Rzucone Kości zachowuje Crit Chance/Cast Speed 15/38 jako utility scaling bez damage table.
Wygłodniały Cyklon zachowuje source cooldown reduction 20/28 s, bazowe 60 s i brak effectiveCycleSeconds.

## 10. Spętanie — baza i warianty

Baza DAMAGE_OVER_DURATION 350/893 przez 8 s.
Destylacja Duszy: globalny warunkowy Darkness multiplier 30/77%[x] ze scalingiem,
bez component table Soulrift i interpolacji.
Wir Dusz: EVENT_TRIGGERED_DAMAGE 25/64 za jedną duszę, duration/interval null,
użycie na celu/Pull i osobna redukcja cooldownu 15 s. Bez mnożenia przez 3/cykle.
Zamarznięte Pustkowia: BURST_DAMAGE 75/191 tylko za pochłonięcie duszy Frozen enemy;
jawny warunek i brak zadanej częstotliwości. Frostbite/Chill 30% co sekundę to status notes bez osobnego coefficient.

## 11. HTTP, filtry i search

HTTP 200 dla `/ranking-obrazen?character=necromancer`: 23 wiersze.
Filtry `skillGroup=ultimate`, `sourceCategory=ULTIMATE`, `tag=ULTIMATE` po 4.
Kolejność basic → core → corpse → macabre → curse → ultimate.
Wspólny search znajduje wszystkie 161 nazw upgrade'ów, Moce Specjalne, cztery skille,
Martwe Zimno, Hematolagnia, Wygłodniały Cyklon, Destylacja Duszy, 0.25 s i zdarzenie.
Nie zmieniono serwisu, kontrolera ani searchu na potrzeby Ultimate.
Wspólny renderer udostępnia notes w tooltipach nazw wszystkich klas,
w tym jednostkę per explosion Armii i semantykę dusz.
SSR zachowuje 16 kolumn, właściwe duration, Lucky Hit, cooldowny i 2 / zdarzenie.

## 12. Kompletność i zachowanie wcześniejszych danych

Nekromanta: **23 skille / 161 ulepszeń**:
Basic 4/28, Core 6/42, Corpse 3/21, Macabre 4/28, Curse 2/14, Ultimate 4/28.
Porównanie względem świeżej migawki przed N6 potwierdza identyczność całego bloku fabryk
wcześniejszych 19 skilli i wszystkich istniejących helperów registry.
Golden mapy 19 wcześniejszych skilli pozostały, dodano 4.
Kontrola SHA-256 potwierdza zachowanie 495 zastanych plików poza zakresem N6, w tym Paladyna,
runtime, current build, zamkniętych raportów N0–N5 i niezwiązanych zmian użytkownika.

## 13. Walidacja

Testy ukierunkowane: **195 testów w 27 zestawach, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
Czas Maven: **15.529 s**, zakończenie **07.10.2026 14:20:12 CEST**.

```powershell
mvn '-Dmaven.repo.local=.m2' '-Dtest=SkillChargeDataTest,SkillResourceDataTest,CharacterTreeSkillMetadataTest,Necromancer*Test,DamagePercent*Test,DamageRanking*Test,Paladin*AuditTest,Neutral*Test,CharacterSkillTreeRegistryTest' clean test
```

Pełna regresja: **756 testów w 117 zestawach, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
Czas Maven: **22 min 53 s**, zakończenie **07.10.2026 14:50:07 CEST**.
Wynik potwierdzony sumą raportów XML Surefire. Czysta kompilacja objęła 345 plików produkcyjnych i 119 testowych.

```powershell
mvn '-Dmaven.repo.local=.m2' clean test
```

`git -c core.safecrlf=false diff --check`: poprawny.
Wszystkie 519 kontrolowanych plików kodu, testów i dokumentacji zachowały sumy SHA-256
między startem i końcem pełnej regresji. Po jej zakończeniu zmieniono wyłącznie wynik walidacji w tym raporcie.

## 14. Paladyn

Zachowuje **24 skille**, dane, adapter, runtime i current build.
Audyty golden i HTTP Paladyna przechodzą w walidacji ukierunkowanej.
Neutralny tooltip notes nie zmienia danych ani bazowych komponentów.

## 15. Runtime/current build

Bez zmian HeroClass, SkillId, DamageEngine, ManualSimulationService i current build.
SkillChargeData, SkillActivationRequirement, zwykły cooldown i modele periodic/event damage nie zostały zmienione.
Wszystkie nowe modifiers mają safeForRuntimeDps=NO.
Ranking wszystkich 23 skilli działa z null engine; damagePerUse, effectiveCycleSeconds i theoreticalDps pozostają null.
Nie dodano resource-rate engine, nie sumowano eksplozji, fal, ticków, stosów i warunkowych zdarzeń.
Nie przyjęto dostępności minionów, Zwłok lub Frozen enemies.

## 16. ZIP

**ZIP nie został utworzony.** Polecenia Maven kończą się na fazie test, bez package/assembly.
