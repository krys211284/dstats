# Dane źródłowe Core Nekromanty — N2, grupy po korekcie N2.1

Kontrakt wykonawczy: [README](../../README.md). Baseline: Sezon 15 / patch 3.2.1; patch 3.2.2 nie zmienia użytych danych Nekromanty. Golden values pochodzą z dostarczonego kontraktu N2, D4Guides EN/PL oraz głównego [max-view D4Builds](https://d4builds.gg/database/class-skills/). Nie używamy starych subdomen ani branchy D4Builds. Widok max nie ma jawnej etykiety Rank 15; projekt zapisuje go pod 15 ze statusem `NEEDS_VERIFICATION`. Rangi 2..14 pozostają nieznane.

Core rozszerza istniejący `NecromancerSkillTreeRegistry`. Grupa `core` ma nazwę `Główne / Core`, po `basic`; kategoria to `CORE / Główne / displayOrder=1`. Sześć skilli ma `DAMAGE / NEEDS_VERIFICATION`, po siedem ulepszeń w grupach 2 + 2 + 3. Cały katalog Nekromanty to 4 Basic + 6 Core i 28 + 42 = 70 ulepszeń. Każdy modyfikator ma `safeForRuntimeDps=NO`.

N2.1 koryguje wyłącznie członkostwo istniejących node'ów w grupach. Źródłem struktury jest [Icy Veins — Necromancer Skills](https://www.icy-veins.com/d4/guides/necromancer-skills/), które jawnie podaje `Group 1`, `Group 2` i `T`. Grupy 1 i 2 zawierają po dwie wzajemnie wykluczające się Enhancements; grupa 3 odpowiada `T`, czyli trzem wzajemnie wykluczającym się Transformation variants. Katalogowy wybór to dokładnie jeden node z każdej grupy. Kolejność wpisów D4Guides nie określa grup; D4Guides nadal jest źródłem polskich nazw, liczb i opisów zachowanych z N2. Pierwotne grupowanie N2 było błędne; przebieg korekty opisuje [raport N2.1](n2.1-implementation-report.md), a historyczny raport N2 pozostaje zachowany.

## Źródła i bazowe golden values

| ID / nazwa | EN / PL | Tagi bazowe | R1 / max-view | Esencja: koszt | Lucky Hit |
|---|---|---|---|---:|---:|
| `wlocznia_z_kosci` / Włócznia z Kości | [Bone Spear](https://d4guides.gg/en/database/skills/bone-spear) / [PL](https://d4guides.gg/pl/baza-danych/umiejetnosci/wlocznia-z-kosci) | CORE, BONE, PHYSICAL | 160% / 408% | 25 | 50% |
| `mor` / Mór | [Blight](https://d4guides.gg/en/database/skills/blight) / [PL](https://d4guides.gg/pl/baza-danych/umiejetnosci/mor) | CORE, DARKNESS, SHADOW | trafienie 40% / 102%; obszar 210% / 536% łącznie w ciągu 6 s | 20 | 40% |
| `rozdarcie` / Rozdarcie | [Sever](https://d4guides.gg/en/database/skills/sever) / [PL](https://d4guides.gg/pl/baza-danych/umiejetnosci/rozdarcie) | CORE, DARKNESS, SHADOW | ruch widma 60% / 153%; zamach kosy 100% / 255% | 20 | 40% |
| `naplyw_krwi` / Napływ Krwi | [Blood Surge](https://d4guides.gg/en/database/skills/blood-surge) / [PL](https://d4guides.gg/pl/baza-danych/umiejetnosci/naplyw-krwi) | CORE, BLOOD, PHYSICAL | drenaż 20% / 51%; nova 125% / 319% | 30 | 12% |
| `krwawa_lanca` / Krwawa Lanca | [Blood Lance](https://d4guides.gg/en/database/skills/blood-lance) / [PL](https://d4guides.gg/pl/baza-danych/umiejetnosci/krwawa-lanca) | CORE, BLOOD, PHYSICAL | 160% / 408% | 15 | 50% |
| `szkieletowy_mag` / Szkieletowy Mag | [Skeleton Mage](https://d4guides.gg/en/database/skills/skeleton-mage) / [PL](https://d4guides.gg/pl/baza-danych/umiejetnosci/szkieletowy-mag) | CORE, SUMMON, MINION | 80% / 204% za atak jednego maga | 30 | 20% |

W czasie N2 źródło PL Moru było niedostępne w narzędziu WWW; jego nazwy i konflikt +20% zachowano zgodnie z dostarczonym kontraktem. EN oraz główny max-view D4Builds pozwalają zachować rozbieżność bez arbitralnego rozstrzygnięcia.

Każda prosta tabela oraz każda tabela komponentu ma tylko punkty 1 i 15. Core nie ma bazowego generowania Esencji. Warianty nie zastępują kosztów ani bazowych tagów. Szkieletowy Mag przywołuje bazowo do 3 magów; procent opisuje jeden atak jednego maga, bez mnożenia przez 3. Krwawa Lanca odbija się do kolejnego wroga, pozostawia przebicie na 3 s i trafia innych przebitych wrogów; liczba wrogów nie staje się stałym mnożnikiem single target.

## Oddzielne komponenty i jednostki

| Skill | `DamagePercentComponent` | R1 | Max-view | Okres |
|---|---|---:|---:|---|
| Mór | `PRIMARY_DAMAGE` | 40% | 102% | pojedyncze trafienie |
| Mór | `DAMAGE_OVER_DURATION` | 210% | 536% | łącznie w ciągu 6 s |
| Rozdarcie | `FIRST_STRIKE_DAMAGE` | 60% | 153% | ruch widma |
| Rozdarcie | `SECOND_STRIKE_DAMAGE` | 100% | 255% | zamach kosy |
| Napływ Krwi | `PRIMARY_DAMAGE` | 20% | 51% | drenaż |
| Napływ Krwi | `BURST_DAMAGE` | 125% | 319% | nova |

Te trzy skille mają pustą prostą tabelę i główne procenty `null`. `DamagePercentComponentRankTable.of(tables, durationsSeconds)` dla Moru zapisuje `DAMAGE_OVER_DURATION -> 6`; `durationSecondsFor` udostępnia czas bez obliczeń. Neutralny komponent wymaga dodatniego czasu dla istniejącego komponentu. Notes zachowują tę samą jednostkę. SSR i search pokazują `210% w ciągu 6 s` oraz `536% w ciągu 6 s`. Nie używamy `DAMAGE_PER_SECOND`, nie sumujemy komponentów i nie dzielimy przez 6. Istniejące komponenty Paladyna zachowują wartości i prezentację.

Bazowa nova Napływu Krwi ma opis `efekt_bazowy`: +5%[x] za wroga objętego drenażem, do +50%[x]. To warunkowy modyfikator, nie ósme ulepszenie i nie automatyczne +50% w rankingu/runtime.

## Wszystkie 42 ulepszenia

W kolumnie procentów wariantów pierwsza wartość oznacza R1, druga max-view. Dane max pozostają w notes. Pola `value` nie podstawiają max za R1. Warunki, względne obrażenia i efekty użytkowe pozostają opisami.

### Włócznia z Kości

| Grupa | Nazwa PL | Źródłowy efekt i ograniczenie |
|---:|---|---|
| 1 / Enhancement | Premia do Obrażeń od Pierwszego Trafienia | 40%[x] do pierwszego trafionego wroga |
| 1 / Enhancement | Redukcja Kosztu | -5 Esencji, bazowy koszt 25 pozostaje |
| 2 / Enhancement | Premia do Obrażeń od Przebicia | 10%[x] za przeszytego wroga, do 50%[x]; warunkowy multiplier |
| 2 / Enhancement | Animusz | 5%[x] obrażeń krytycznych za Resolve, do 40%[x]; status/enabler |
| 3 / Transformation | Kościane Kolce | po zniszczeniu 3 przeszywające odłamki, każdy 53% / 135%; dodatkowe hity |
| 3 / Transformation | Włócznia Krwi | Blood 160% / 408%; zużycie pobliskich zwłok tworzy drugą włócznię o 100% normalnych obrażeń |
| 3 / Transformation | Rozszczepianie Cieni | Darkness / Shadow 163% / 416%; zamiast przebijania 2 przeszywające włócznie przy trafieniu wroga lub ściany; najwyżej 2 rozszczepienia |

### Mór

| Grupa | Nazwa PL | Źródłowy efekt i ograniczenie |
|---:|---|---|
| 1 / Enhancement | Premia do Obrażeń Obszarowych | skażony obszar 40%[x] |
| 1 / Enhancement | Premia do Rozmiaru | rozmiar +35% |
| 2 / Enhancement | Premia do Obrażeń za Ograniczenie Kontroli | 30%[x] przeciw Crowd Controlled |
| 2 / Enhancement | Szansa na Szczęśliwy Traf | zachowane +20%; konflikt PL/D4Builds kontra usunięcie w EN changelogu 3.2.1; manual review |
| 3 / Transformation | Przeszywający Mrok | +1 przebicie, możliwy dodatkowy skażony obszar; 75%[x] tylko przeciw bossom |
| 3 / Transformation | Wir | Cold / Frostbite, koszt -10, Pull, Chill 15% co sekundę; bazowy koszt 20 pozostaje |
| 3 / Transformation | Niestabilna Krew | Blood / Physical; natychmiastowy wybuch obszaru 315% / 803%; nowy komponent |

### Rozdarcie

| Grupa | Nazwa PL | Źródłowy efekt i ograniczenie |
|---:|---|---|
| 1 / Enhancement | Premia do Obrażeń | 10%[x] oraz osobne warunkowe 1%[x] za sługę |
| 1 / Enhancement | Redukcja Kosztu | -4 Esencji, bazowy koszt 20 pozostaje |
| 2 / Enhancement | Zajadłość | 7%[x] za kumulację Ferocity |
| 2 / Enhancement | Ograniczanie Kontroli i Generowanie Zwłok | Slow 40% przez 3 s, Corpse |
| 3 / Transformation | Żniwiarski Lotos | 3 dodatkowe widma w najdalszym punkcie, każde 60% / 153% |
| 3 / Transformation | Nieubłagany Żniwiarz | także Mobility; doskok do przodu, bez osobnego potwierdzonego procentu obrażeń |
| 3 / Transformation | Chłodna Pogoń | Cold, Chill 15%, ponowny atak na innego wroga; dodatkowy hit warunkowy, bez założenia tego samego celu |

### Napływ Krwi

| Grupa | Nazwa PL | Źródłowy efekt i ograniczenie |
|---:|---|---|
| 1 / Enhancement | Premia do Obrażeń | 25%[x] podczas Healthy |
| 1 / Enhancement | Umocnienie | 2% maksymalnego zdrowia za wroga objętego drenażem, do 10% |
| 2 / Enhancement | Przytłoczenie | zużycie 2 kumulacji Overpower, rozmiar +25%, obrażenia +50%[x] |
| 2 / Enhancement | Osłabienie | Weaken 4 s |
| 3 / Transformation | Krwawa Jatka | echo nowy po opóźnieniu 125% / 319%; dodatkowy komponent |
| 3 / Transformation | Ty i Jaka Armia? | małe novy na sługach 20% / 51%; +5%[x] za wroga objętego drenażem, do +50%[x] |
| 3 / Transformation | Igły | Bone zamiast nowy: 10 przeszywających odłamków, każdy 60% / 153%; +1 za wroga objętego drenażem, do +5; bez sum 10×60 i 15×60 |

### Krwawa Lanca

| Grupa | Nazwa PL | Źródłowy efekt i ograniczenie |
|---:|---|---|
| 1 / Enhancement | Rykoszet | +1 dodatkowy wróg |
| 1 / Enhancement | Umocnienie | 4% maksymalnego zdrowia przy trafieniu już przebitego / lanced wroga |
| 2 / Enhancement | Przytłoczenie | zużycie 2 kumulacji Overpower, dodatkowa lanca o 50% normalnych obrażeń |
| 2 / Enhancement | Redukcja Kosztu | -3 Esencji, bazowy koszt 15 pozostaje |
| 3 / Transformation | Poszukiwanie Krwi | 50%[x] za lanced wroga oraz osobne 50%[x] do pierwszego trafionego; warunkowe |
| 3 / Transformation | Okrwawione Kolce | co trzecie użycie Krwawa Kula; zebranie pobliskiej kuli tworzy dodatkową lancę o 125% normalnych obrażeń |
| 3 / Transformation | Ropiejąca Rana | Darkness / Shadow, stale lanced; Corrupting 100% / 255% na sekundę; patch 3.2.1 do 5 kumulacji, bez automatycznego stosowania |

Procenty dodatkowych lanc `50%`, `100%`, `125%` są względem normalnych obrażeń. Model zapisuje je jako `ADDITIONAL_HIT_OR_STRIKE / TEXT_ONLY`, z jawnie względnym `affectedComponent`, bez absolutnej tabeli procentów broni. Dla Przytłoczenia i Okrwawionych Kolców `UpgradeDamageImpact.damagePercent=null`; Włócznia Krwi zachowuje niezależny główny komponent 160% i osobny opis względnej drugiej włóczni.

### Szkieletowy Mag

| Grupa | Nazwa PL | Źródłowy efekt i ograniczenie |
|---:|---|---|
| 1 / Enhancement | Czas Działania Premii do Obrażeń | 15%[x], rośnie o 1% za sekundę życia maga, do 25%[x] |
| 1 / Enhancement | Szansa na Trafienie Krytyczne | gracz +5% przeciw wrogowi zranionemu przez maga |
| 2 / Enhancement | Zajadłość, Animusz lub Przytłoczenie | 5% szans na 1 kumulację za atak: Ferocity przy Shadow, Resolve przy Cold, Overpower przy Bone |
| 2 / Enhancement | Premia do Obrażeń od Ograniczenia Kontroli | 30%[x] przeciw Crowd Controlled |
| 3 / Transformation | Sabat | limit magów +2, bez mnożnika tabeli bazowej |
| 3 / Transformation | Dar Śmierci | także Corpse Skill, wymaga zwłok; regeneracja Esencji za aktywnego maga R1 50%[x], max-view 128%[x]; warunkowy/globalny modyfikator, bez bazowego generowania |
| 3 / Transformation | Osobliwość | zużywa całą pozostałą Esencję; 3%[x] obrażeń i +0.1 s za faktycznie zużyty punkt; bazowy koszt 30 pozostaje |

## Konflikty i bezpieczeństwo interpretacji

Mór / Lucky Hit zachowuje node +20%. `UpgradeDamageModifier.type=NEEDS_MANUAL_REVIEW`, `safeForRankingDisplay=NEEDS_MANUAL_REVIEW`, `UpgradeDamageImpact.type=NEEDS_VERIFICATION`. Notes i tooltip wskazują konflikt PL D4Guides / bieżącego D4Builds z usunięciem właściwości w changelogu EN 3.2.1. Bazowy Lucky Hit Moru nadal wynosi 40%; ulepszenie go nie zastępuje.

Szkieletowy Mag: D4Guides opisuje bazowy Physical, D4Builds pokazuje Shadow Damage, a typ może zależeć od wariantu Book of the Dead. Notes ujawniają rozbieżność. Bazowe tagi pozostają `CORE`, `SUMMON`, `MINION`, bez `PHYSICAL` i `SHADOW`. Konflikt nie blokuje 80% / 204% za pojedynczy atak.

Wspólny ranking jest opisowy i single target. Liczba wrogów, sług, odłamków i kumulacji nie staje się automatycznym mnożnikiem. Redukcje kosztów, całkowite zużycie zasobu i globalna regeneracja są modyfikatorami. `HeroClass`, `SkillId`, `DamageEngine`, `ManualSimulationService` oraz current build zachowują zakres Paladyna.
