# Nekromanta — źródłowe dane Basic, etap N1

Katalog obejmuje cztery umiejętności i 28 ulepszeń, po siedem w układzie grup 2 + 2 + 3. Dane są opisowe: wszystkie skille mają `DAMAGE / NEEDS_VERIFICATION`, a wszystkie modyfikatory `safeForRuntimeDps = NO`. Nie stanowią obliczeń obrażeń ani DPS. Domyślny kontrakt pozostaje single target.

## Wersja i pochodzenie

Baseline dostarczony w zadaniu N1: Diablo IV Sezon 15, patch 3.2.1. Sprawdzone 06.10.2026 strony D4Guides deklarują tę wersję. [Oficjalne patch notes Blizzard](https://news.blizzard.com/en-us/article/24295395/diablo-iv-patch-notes) podają patch 3.2.2 z 30.09.2026; jego lista zmian klas obejmuje Druida i Czarodzieja, bez Nekromanty.

R1 i opisy pochodzą z D4Guides EN, polskie nazwy z D4Guides PL. [Bieżąca baza D4Builds](https://d4builds.gg/database/class-skills/) jest źródłem kontrolnym widoku max. Bezpośredni odczyt strony zwrócił jedynie szkielet aplikacji; aktualny indeks głównej domeny udostępnił opisy max odpowiadające baseline zadania. Nie wykorzystano danych subdomen testowych ani mirrorów.

D4Builds nie oznacza tych wartości jawnie jako Rank 15. Przypisanie widoku max do klucza 15 jest konwencją projektu, zapisaną w `notes`, i nadal wymaga weryfikacji. Tabele mają tylko klucze 1 i 15; rangi 2..14 są puste. Nie ma wzoru skalowania ani sumowania komponentów.

| ID / nazwa | R1 | Widok max zapisany pod 15 | Lucky Hit | Esencja | Tagi bazowe |
|---|---:|---:|---:|---|---|
| `kosciane_odlamki` / Kościane Odłamki | 30% na odłamek | 77% na odłamek | 30% | 10 / użycie; znany opcjonalny dodatek +5 | BASIC, BONE, PHYSICAL |
| `zniwa` / Żniwa | 50% | 128% | 30% | 15 / użycie | BASIC, DARKNESS, SHADOW |
| `rozklad` / Rozkład | 150% / sek. | 383% / sek. | 40% | 12–16 / sek. | BASIC, DARKNESS, CHANNELED, SHADOW |
| `krwotok` / Krwotok | 80% | 204% | 50% | 13 / użycie | BASIC, BLOOD, PHYSICAL |

Wszystkie należą do grupy `basic` / `Podstawowe / Basic` i kategorii `BASIC` / `Podstawowe` / kolejność 0. Zasób: `essence`, `Koszt Esencji`, `Generowanie Esencji`, `pkt. esencji`.

## Kościane Odłamki / Bone Splinters

Źródła: [D4Guides EN](https://d4guides.gg/en/database/skills/bone-splinters), [nazwy PL](https://d4guides.gg/pl/baza-danych/umiejetnosci/kosciane-odlamki), [max D4Builds](https://d4builds.gg/database/class-skills/). Bazowo trzy odłamki Physical, każdy z własnym procentem.

| Grupa | Ulepszenie | Zachowane dane |
|---|---|---|
| 1 | Pociski | +1 odłamek |
| 1 | Generowanie Esencji | +5 Esencji |
| 2 | Odsłonięcie | Vulnerable przez 4 s |
| 2 | Animusz | +1 kumulacja Resolve |
| 3 | Rykoszetujące Kolce | R1 45% na odłamek; max 115%; odbicia od ścian i większy zasięg |
| 3 | Krwawe Odłamki | Blood; R1 30%; przebicie 1 raz; leczenie 2.0% maksymalnego zdrowia na trafienie R1 |
| 3 | Poszukiwacze Cienia | Darkness; R1 30% Shadow oraz osobny impuls 7.5%; max 77% + 20%; szukanie wrogów |

## Żniwa / Reap

Źródła: [D4Guides EN](https://d4guides.gg/en/database/skills/reap), [nazwy PL](https://d4guides.gg/pl/baza-danych/umiejetnosci/zniwa), [max D4Builds](https://d4builds.gg/database/class-skills/).

| Grupa | Ulepszenie | Zachowane dane |
|---|---|---|
| 1 | Szansa na Trafienie Krytyczne | R1 +10%; D4Builds +20% pozostaje rozbieżnością skalowania/źródła |
| 1 | Generowanie Esencji | +2 za każdego trafionego wroga; warunkowe |
| 2 | Zajadłość | +1 kumulacja Ferocity |
| 2 | Generowanie Zwłok | zwłoki pod pierwszym trafionym wrogiem |
| 3 | Przejmujący Chłód | R1 50% Cold, wyziębienie 15%, szybkość użycia +30%; max obrażeń 128%, większe wyziębienie |
| 3 | Selekcja Naturalna | Blood; dokładne R1 62.5% Physical; max 161%; egzekucja; kula z szansą 50% po zabiciu |
| 3 | Dorzynki | drugi zamach R1 25% i 7.5 Esencji; max 64%, prezentacja Esencji w D4Builds zaokrąglona do 8 |

Dokładne 62.5% pochodzi z przekazanego baseline N1. Aktualny widoczny tekst D4Guides EN podaje zaokrąglone 63%; nie udajemy niezależnego potwierdzenia 62.5% tym widocznym tekstem. Zachowano obie informacje w metadanych. +2 za wroga i 7.5 drugiego zamachu nie trafiają do `generationBonusKnown` bazowych Żniw.

## Rozkład / Decompose

Źródła: [D4Guides EN z changelogiem](https://d4guides.gg/en/database/skills/decompose), [nazwy i opis PL](https://d4guides.gg/pl/baza-danych/umiejetnosci/rozklad), [max D4Builds](https://d4builds.gg/database/class-skills/). Bazowe obrażenia Corrupting / Shadow są podane na sekundę; zwłoki powstają co 1 s.

| Grupa | Ulepszenie | Zachowane dane |
|---|---|---|
| 1 | Premia do Obrażeń | 50%[x] |
| 1 | Bariera | co sekundę 10% maksymalnego zdrowia, czas 6 s |
| 2 | Ograniczenie Kontroli | spowolnienie 50% |
| 2 | Szansa na Szczęśliwy Traf | +30%; konflikt PL/EN, `NEEDS_MANUAL_REVIEW` dla tej wartości |
| 3 | Targanie | Bone / Physical; wrogowie między graczem a głównym celem |
| 3 | Gnijący Rozprysk | dodatkowy impuls co sekundę, R1 113%, max 288% |
| 3 | Próchnienie | łańcuch do trzech dodatkowych wrogów |

Zakres Esencji D4Guides EN to 12–16 na sekundę; pojedyncze 13 D4Builds nie zastępuje zakresu. PL D4Guides oraz bieżący D4Builds pokazują +30% Lucky Hit, a EN D4Guides usuwa opis w changelogu 3.2.1. Węzeł pozostaje w grupie 2, wartość w modyfikatorze i tooltipie `Manual review`, a jego impact ma `NEEDS_VERIFICATION`. Pozostałe proste modyfikatory nie są ukrywane przez ten konflikt.

## Krwotok / Hemorrhage

Źródła: [D4Guides EN](https://d4guides.gg/en/database/skills/hemorrhage), [nazwy PL](https://d4guides.gg/pl/baza-danych/umiejetnosci/krwotok), [max D4Builds](https://d4builds.gg/database/class-skills/). Bazowo 20% szans na Krwawą Kulę. Bezpośredni fetch PL nie powiódł się, ale aktualny indeks tej dokładnej strony udostępnił polskie nazwy i opisy.

| Grupa | Ulepszenie | Zachowane dane |
|---|---|---|
| 1 | Szybkość Użycia | +20% |
| 1 | Krwawa Kula | dodatkowa szansa +30% |
| 2 | Przytłoczenie | +1 kumulacja Overpower |
| 2 | Osłabienie | Weaken przez 4 s |
| 3 | Krwawy Wrzód | dodatkowe obrażenia wokół celu, R1 40%, max 102% |
| 3 | Zmrożenie Krwi | R1 80% Cold, max 204%; zamrożenie 3 s raz na 6 s na tego samego wroga; +50% szans na kulę przy trafieniu zamrożonego |
| 3 | Rozdarcie Dusz | Darkness; R1 80% Shadow, max 204%; zamiast kuli Rozszalały / Volatile Skeleton z R1 120%, max 306% |

## Model i regresje

`sourceReference` wskazuje EN WWW, a `notes` zachowują canonical name, PL WWW, pochodzenie max i jego ograniczenia. Legacy PDF Paladyna oraz Verification Matrix nie są migrowane. `SkillResourceData` używa `BigDecimal`, obu granic i `PER_USE`/`PER_SECOND`; SSR zachowuje zakres oraz częstotliwość. Sortowanie bazowe bierze dolną granicę, maxKnown górną z uwzględnieniem znanego możliwego dodatku z wybranego ulepszenia, bez normalizacji częstotliwości.

Wartości procentowe modyfikatorów w `value` opisują R1; dane max pozostają w `notes`, więc nie mieszają się z metryką maksimum procentów R1. Parser i sortowanie używają `BigDecimal`, również dla 7.5%, 62.5% i kilku procentów w jednym polu. `UpgradeDamageImpact.damagePercent` pozostaje `Integer`; dla komponentów dziesiętnych jest pusty, a dokładny procent zachowują opis i `UpgradeDamageModifier.value`. Potwierdzone komponenty mają `safeForRankingDisplay = YES`, co pozwala pokazać opis źródła, bez wykonania w engine.

Testy źródeł chronią cztery skille Basic, 28 nazw, rangi, Lucky Hit, zasób, konflikty i blokadę runtime. Po N2 regresje HTTP Basic używają `/ranking-obrazen?character=necromancer&skillGroup=basic`, sprawdzając wyszukiwanie wszystkich 28 ulepszeń, filtry, sortowanie i zakres `/ sek.`. Cały katalog ma 10 skilli; Core opisują [dane N2](core-source-data.md). Paladyn zachowuje 24 skille. Ranking działa w regresji bez instancji `DamageEngine`. `HeroClass`, `SkillId`, manual simulation i current build nie otrzymują obsługi Nekromanty.
