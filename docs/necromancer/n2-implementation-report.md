# Raport wdrożenia N2

## 1. Pliki zmienione i dodane względem zakończonego N1

Zakres N2 porównano przez SHA-256 ze stanem plików przed rozpoczęciem etapu: 13 zmienionych i 4 dodane pliki, bez usunięć. Wcześniejsze zmiany N0/N1 i użytkownika pozostają zachowane.

| Plik | Zmiana |
|---|---|
| [NecromancerSkillTreeRegistry.java](../../src/main/java/krys/necromancer/NecromancerSkillTreeRegistry.java) | sześć Core w istniejącym rejestrze, kategoria/grupa, 42 ulepszenia i opisy warunków |
| [DamagePercentComponent.java](../../src/main/java/krys/skilltree/DamagePercentComponent.java) | nowy neutralny `DAMAGE_OVER_DURATION` |
| [DamagePercentComponentRankTable.java](../../src/main/java/krys/skilltree/DamagePercentComponentRankTable.java) | czas okresu komponentu i jego walidacja |
| [SkillResourceData.java](../../src/main/java/krys/skilltree/SkillResourceData.java) | tylko korekta komentarza `generationMaxKnown()` |
| [DamageRankingPageRenderer.java](../../src/main/java/krys/web/DamageRankingPageRenderer.java) | jawny okres przy procentach komponentu |
| [DamageRankingSearchText.java](../../src/main/java/krys/web/DamageRankingSearchText.java) | okres komponentu w searchu |
| [NecromancerSkillTreeRegistryTest.java](../../src/test/java/krys/necromancer/NecromancerSkillTreeRegistryTest.java) | zachowany zakres Basic; provider i granica runtime dla 10 wpisów |
| [NecromancerCoreSkillTreeRegistryTest.java](../../src/test/java/krys/necromancer/NecromancerCoreSkillTreeRegistryTest.java) | dodany, golden values i semantyka Core |
| [DamagePercentComponentTest.java](../../src/test/java/krys/skilltree/DamagePercentComponentTest.java) | rozszerzony zbiór komponentów |
| [DamagePercentComponentRankTableTest.java](../../src/test/java/krys/skilltree/DamagePercentComponentRankTableTest.java) | jednostka czasu, walidacja i niezmienność danych |
| [NecromancerDamageRankingWebServerTest.java](../../src/test/java/krys/web/NecromancerDamageRankingWebServerTest.java) | regresje czterech Basic z jawnym filtrem grupy |
| [NecromancerCoreDamageRankingWebServerTest.java](../../src/test/java/krys/web/NecromancerCoreDamageRankingWebServerTest.java) | dodany, rzeczywisty HTTP, komponenty, filtry i 42 wyszukiwane nazwy |
| [README.md](../../README.md) | aktualny zakres i kontrakt N2 |
| [basic-source-data.md](basic-source-data.md) | korekta możliwego dodatku zasobu i zakresu testów Basic po N2 |
| [n1-implementation-report.md](n1-implementation-report.md) | oznaczenie raportu historycznego i korekta opisu zasobu |
| [core-source-data.md](core-source-data.md) | dodany, wszystkie dane i 42 ulepszenia Core |
| [n2-implementation-report.md](n2-implementation-report.md) | dodany, niniejszy raport |

## 2. Liczba i struktura skilli

Nekromanta ma dokładnie **10 = 4 Basic + 6 Core**. Core: `wlocznia_z_kosci`, `mor`, `rozdarcie`, `naplyw_krwi`, `krwawa_lanca`, `szkieletowy_mag`. Każdy ma kategorię `CORE / Główne / 1`, grupę `core`, `DAMAGE / NEEDS_VERIFICATION` i ulepszenia `2 + 2 + 3`. Kolejność grup w rejestrze i filtrze to Basic, Core. Dane istniejących Basic pozostają zachowane.

## 3–4. Komponenty i semantyka okresu

Mór: `PRIMARY_DAMAGE` i `DAMAGE_OVER_DURATION`. Rozdarcie: `FIRST_STRIKE_DAMAGE` i `SECOND_STRIKE_DAMAGE`. Napływ Krwi: `PRIMARY_DAMAGE` i `BURST_DAMAGE`. Te trzy skille nie mają prostego bazowego procentu ani sumy komponentów.

`DAMAGE_OVER_DURATION` zapisuje procent łącznie za okres z `durationSecondsFor(component)=6`. Notes, SSR i search zachowują `210% w ciągu 6 s` oraz `536% w ciągu 6 s`. Brak czasu lub niepoprawne przypisanie czasu jest odrzucane. Nie wstawiamy wartości do `DAMAGE_PER_SECOND` ani nie wyliczamy DPS. Dotychczasowa fabryka tabel bez czasu nadal obsługuje wszystkie istniejące komponenty Paladyna.

## 5–6. Golden values, koszty i Lucky Hit

| Core | R1 | Max-view zapisany pod 15 | Koszt Esencji | Lucky Hit |
|---|---|---|---:|---:|
| Włócznia z Kości | 160% | 408% | 25 | 50% |
| Mór | trafienie 40%; obszar 210% w ciągu 6 s | trafienie 102%; obszar 536% w ciągu 6 s | 20 | 40% |
| Rozdarcie | pierwsze 60%; drugie 100% | pierwsze 153%; drugie 255% | 20 | 40% |
| Napływ Krwi | drenaż 20%; nova 125% | drenaż 51%; nova 319% | 30 | 12% |
| Krwawa Lanca | 160% | 408% | 15 | 50% |
| Szkieletowy Mag | 80% za atak jednego maga | 204% za atak jednego maga | 30 | 20% |

Tabele mają wyłącznie rangi 1 i 15. D4Builds pokazuje max-view bez jawnej etykiety Rank 15; 15 jest mapowaniem projektu, wymagającym weryfikacji. Nie ma interpolacji 2..14 ani bazowego generowania Esencji dla Core. Redukcje kosztu i dynamiczne zużycie zasobu nie zastępują kosztów z tabeli.

## 7. Konflikt Lucky Hit Moru

Node +20% pozostaje. Typ modyfikatora i bezpieczeństwo wyświetlania: `NEEDS_MANUAL_REVIEW`; typ impact: `NEEDS_VERIFICATION`. Notes i tooltip opisują PL D4Guides / bieżący D4Builds +20% kontra usunięcie właściwości w EN changelogu 3.2.1. Bazowy Lucky Hit 40% jest zachowany. Filtr manual review zwraca Mór i Rozkład.

## 8. Konflikt typu obrażeń Szkieletowego Maga

Notes wskazują Physical z D4Guides, Shadow Damage z D4Builds i możliwą zależność od Book of the Dead. Bazowe tagi to wyłącznie `CORE`, `SUMMON`, `MINION`; bez bezwarunkowego `PHYSICAL` lub `SHADOW`. Wartości 80% / 204% za jeden atak pozostają dostępne.

## 9. Ulepszenia

**42 Core + 28 Basic = 70**. Wszystkie polskie nazwy są chronione golden listami i wyszukiwane przez wspólny search. Warianty zachowują osobne procenty i notes max-view, warunki liczby wrogów/sług/statusów oraz współczynniki względne normalnych obrażeń. Lista wszystkich 42 znajduje się w [danych źródłowych Core](core-source-data.md).

## 10. Testy

Pierwszy przebieg testów katalogu, komponentów, zasobu i rankingu: **91 testów, 0 failures, 0 errors, 0 skipped**.

Pełny przebieg `mvn '-Dmaven.repo.local=.m2' clean test`: **BUILD SUCCESS — 649 testów, 105 zestawów, 0 failures, 0 errors, 0 skipped**. Ukończono 06.10.2026 o 16:09:36 (Europe/Warsaw), czas Maven **23:01 min**. Przebieg obejmuje istniejące testy OCR/OpenCV, audyty Paladyna, engine, search, manual simulation, current build oraz rzeczywisty HTTP. `git -c core.safecrlf=false diff --check` przeszedł poprawnie.

HTTP 200: `/ranking-obrazen?character=necromancer` pokazuje 10 wpisów bez Paladyna; `skillGroup=core` i `sourceCategory=CORE` po sześć, `skillGroup=basic` cztery. SSR rozdziela komponenty, a search odnajduje 42 nowe i 28 dotychczasowych nazw. Ranking z pustą referencją engine zwraca 10 wpisów z pustymi metrykami runtime i `SINGLE_TARGET`.

## 11. Paladyn

Rejestr zachowuje **24 skille**. Dane rejestru, tabele i interpretacje Paladyna pozostają bez zmian. Porównanie SHA-256 ze stanem przed N2 potwierdziło brak zmian w 51 plikach domeny Paladyna oraz chronionych plikach runtime. Dotychczasowe testy rejestru, adaptera oraz HTTP chronią jego dane i prezentację; domyślny endpoint nadal wybiera Paladyna.

## 12. Granica runtime/current build

`HeroClass` nadal zawiera wyłącznie `PALADIN`. `SkillId`, `DamageEngine`, `ManualSimulationService`, `PaladinSkillDefs` oraz obsługa current build nie otrzymały zmian. Każdy modyfikator Core ma `safeForRuntimeDps=NO`. N2 dodaje dane do obecnego pipeline katalogu; nie powstaje nowy kontroler, ranking ani drugi rejestr Nekromanty. Nie utworzono ZIP-a.
