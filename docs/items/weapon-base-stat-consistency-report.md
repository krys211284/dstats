# Spójność bazowych statystyk broni

Raport historyczny pierwszej poprawki. Opisane niżej blokowanie wyliczeń przez niepewność źródeł zostało zastąpione kontraktem rozdzielającym wartość i certainty; aktualny stan oraz walidację opisuje [raport korekty i komunikatów importu](weapon-base-stat-values-and-import-feedback-report.md).

Etap ogranicza się do zależności min/max/APS/average/DPS. Zgłoszenie użytkownika sprawdzono testami liczbowymi, bez nowych screenshotów ani nowych golden OCR fixtures. Nie naprawiano nazwy, affixów, socketów, przeistoczenia ani unique effect rzeczywistego itemu.

## Przyczyna i poprzedni przepływ

`PropertyBlockValueReader.baseStat` wybierał zaobserwowaną liczbę DPS z numeric evidence. Dla zgłoszonego wejścia był to kandydat `533`. `ImageItemImportDraftMapper` kopiował go do `ItemImportDetails` niezależnie od poprawnego zakresu i APS. Nie istniała kontrola zależności DPS od tych składników; ten etap nie zmienia sposobu uzyskania samego OCR `533`.

Średnia była wyliczana niezależnie w czterech miejscach: mapperze draftu obrazu, `ItemImportDetails` przy braku average, `ItemImportFormMapper` przy braku/zerowym average oraz rendererze formularza. Podane niezerowe sprzeczne average mogło pozostać w modelu. Te cztery wzory zastąpił jeden resolver używany przez `ItemImportDetails`; renderer wyświetla wynik modelu.

## Nowy kontrakt

`WeaponBaseStatConsistencyResolver` zależy wyłącznie od JDK i przyjmuje min, max, APS, observed DPS oraz niezależną pewność każdego składnika. `ItemImportDetails` wywołuje go przy budowaniu danych itemu. Mapper obrazu dostarcza pewność odczytu, a dane wpisane lub potwierdzone ręcznie używają pewności potwierdzonej. Nie ma warunków zależnych od klasy, nazwy itemu, typu kosy ani zgłoszonych wartości.

Dokładny midpoint to `(min + max) / 2`, liczony przez `BigDecimal`, bez przepełnienia sumy dwóch `Long`. `canonicalAverage` to midpoint zaokrąglony do `Long`. Dokładny DPS to **dokładny midpoint × APS**, a nie modelowa średnia po zaokrągleniu. `canonicalDps` jest zaokrąglany do `Long` przez `HALF_UP`: dla nieujemnych liczb < 0,5 w dół, ≥ 0,5 w górę. Reguła zachowuje dotychczasowe zaokrąglenie średniej oraz DPS obu istniejących zestawów Verathiela.

| Źródła | Dokładna średnia | Modelowa średnia | Dokładny DPS | Modelowy DPS |
| --- | ---: | ---: | ---: | ---: |
| Zgłoszenie: 4269–5805, APS 0,90 | 5037 | 5037 | 4533,3 | 4533 |
| Verathiel: 1350–1978, APS 1,10 | 1664 | 1664 | 1830,4 | 1830 |
| Verathiel v2: 1390–2018, APS 1,10 | 1704 | 1704 | 1874,4 | 1874 |

Testy granic zaokrąglenia obejmują dokładny DPS `1874,4 → 1874`, `1533,6 → 1534` i `2518,5 → 2519`. Oddzielny liczbowy wariant zakresu `1390–2019` zabezpiecza midpoint `1704,5`, modelowe average `1705` i DPS `1874,95 → 1875` przy APS 1,10. Wszystkie oczekiwane liczby zostały potwierdzone outputem resolvera tego buildu w passing unit tests; warianty liczbowe nie są nowymi fixture'ami obrazowymi.

Zaobserwowany DPS jest evidence. Gdy pewne źródła wyznaczają wynik, matching OCR potwierdza go, brak OCR nie blokuje obliczenia, a różnica nie nadpisuje derived DPS. Zgłoszony przypadek daje `canonicalAverage=5037`, `canonicalDps=4533`, `observedDps=533`, status `INCONSISTENT` i `WEAPON_DPS_OCR_INCONSISTENT ocr=533 derived=4533`. Brak danych źródłowych ma status `INCOMPLETE_SOURCE`, a nie fałszywe potwierdzenie zgodności.

## Evidence i niepewność

Wynik dostępny przez `ItemImportDetails.getWeaponBaseStatConsistency()` przechowuje zaobserwowany DPS, dokładne wyniki, pewność, status i diagnostykę. Mapper dopisuje diagnostykę do istniejących `reviewNotes`. Surowe `FullItemRead.baseItemValue` i linie pozostają bez korekty, więc po zapisie/reload biblioteki nadal można odczytać raw `533`. Test zapisu sprawdza równocześnie canonical `4533` i niezmieniony raw tekst. Nie zmieniano klas `FullItemRead`, profili OCR ani evidence odczytu.

Mapper przenosi `numericValueCertain`, `PropertyBlockValueReadResult.isCertain/isAmbiguous`, parametry liczbowe i `PropertyBlockNumericEvidence`. Niepewny zakres nie tworzy automatycznej średniej; niepewny min/max/APS nie tworzy derived DPS. Odczytany DPS może pozostać w polu do korekty, lecz wynik ma `dpsCertain=false`, status `SOURCE_UNCERTAIN` i zachowane `NUMERIC_OCR_AMBIGUOUS`. Renderer nie oblicza brakującego average ponownie. Gdy ambiguous jest wyłącznie token DPS, jednoznaczne parametry zakresu i APS mogą dać canonical DPS; istniejąca notatka ambiguity pozostaje.

Bez min lub max nie powstaje derived average. Bez APS nie powstaje derived DPS; niezależnie wpisany/odczytany DPS pozostaje kompatybilnym kandydatem. Przy niepełnym źródle ręczne average/DPS są nadal obsługiwane. Przy pełnych potwierdzonych źródłach sprzeczny ręczny komplet jest normalizowany podczas budowania `ItemImportDetails`. Min/max/APS pozostają edytowalne. Odwrócony zakres, nieskończony APS i przepełnienie wyniku tworzą `INVALID_SOURCE`; mapper formularza nie pozwala zapisać takiego źródła.

Resolver nie czyta affixów i nie dodaje ponownie żadnego bonusu obrażeń od broni. Runtime nadal preferuje istniejące `averageWeaponDamage`; nie dodano użycia weapon DPS w formule lub nowej ścieżce runtime.

## Zmienione i dodane pliki

- `src/main/java/krys/itemimport/WeaponBaseStatConsistencyResolver.java` — nowy neutralny resolver, pewność, statusy i diagnostyka.
- `src/main/java/krys/itemimport/ItemImportDetails.java` — wspólne canonical average/DPS oraz wynik resolvera; kompatybilne dotychczasowe konstruktory.
- `src/main/java/krys/itemimport/ItemImportFormMapper.java` — usunięty osobny wzór i walidacja niepoprawnych źródeł.
- `src/main/java/krys/itemimport/image/ImageItemImportDraftMapper.java` — pewność składników, review notes, canonical pola draftu; raw bez zmian.
- `src/main/java/krys/web/ItemEditPageRenderer.java` — usunięte ponowne liczenie średniej.
- `src/test/java/krys/itemimport/WeaponBaseStatConsistencyResolverTest.java` — nowy kontrakt liczbowy, 19 przypadków.
- `src/test/java/krys/itemimport/ItemImportFormMapperTest.java` — sprzeczny komplet ręczny, zapis raw, niepełny i odwrócony zakres.
- `src/test/java/krys/itemimport/image/ImageItemImportDraftMapperTest.java` — korekta DPS, ambiguity całego bloku i tokenów, istniejący golden Verathiela.
- `src/test/java/krys/web/ItemEditPageRendererTest.java` — canonical pola i diagnostyka, brak liczenia niepewnej średniej w UI.
- `README.md` — aktualny kontrakt zależnych statystyk.
- `docs/items/weapon-base-stat-consistency-report.md` — ten raport.
- `docs/items/weapon-base-stat-protected-sha256.csv` — SHA BEFORE/AFTER chronionych plików.

## Walidacja

| Przebieg | Testy | Suites | Czas zewnętrzny [s] | Suma suites [s] | Failures / errors / skipped | Wynik |
| --- | ---: | ---: | ---: | ---: | --- | --- |
| targeted | 103 | 10 | 14,204 | 0,861 | 0 / 0 / 0 | BUILD SUCCESS |
| FAST | 1133 | 158 | 233,884 | 229,325 | 0 / 0 / 0 | BUILD SUCCESS |

Czas zewnętrzny mierzono `Stopwatch` wokół Maven. Maven targeted raportuje 12,773 s; FAST 3 min 52 s. Zliczano wyłącznie suites oznaczone `Running` w danym logu, ponieważ bez `clean` mogą pozostać wcześniejsze XML OCR. Lokalne logi i pomiary: `logs/weapon-consistency/targeted.log`, `fast.log`, `validation-summary.json`.

Targeted: `mvn '-Dmaven.repo.local=.m2' '-Dtest=WeaponBaseStatConsistencyResolverTest,ItemImportFormMapperTest,ImageItemImportDraftMapperTest,PropertyBlockValueReaderTest,PropertyBlockSemanticMapperFixtureTest,Part1ReadParserPipelineTest,ItemEditPageRendererTest,ItemTypeImportTest,FileItemLibraryRepositoryTest,ItemTypePersistenceAndEquipmentTest' test`.

FAST: `mvn '-Dmaven.repo.local=.m2' -Pfast test`. Dodano 29 przypadków; nie usuwano przypadków ani nie zmieniano istniejących expected. Przeszły istniejące regresje PART_1, bloków/value readera i mappera dla Tarczy, Dziedzica i Verathiela, testy biblioteki oraz dotychczasowe testy runtime i hero baseline.

`git diff --check`: 0 błędów. [Pełna tabela SHA-256 BEFORE/AFTER](weapon-base-stat-protected-sha256.csv) potwierdza **422 identyczne pliki**: 396 chronionych plików `src/main`, wszystkie 23 zasoby `src/test/resources` oraz `pom.xml`, `AGENTS.md` i ustawienia IDE. Obejmuje DamageEngine, ManualSimulationService, DirectDamageCalculator, resolver Nekromanty, modele RuntimeSkill, hero baseline, ItemTypeRegistry, katalogi Nekromanty, Book of the Dead, OCR, assembler, divider, provider tessdata i model `pol`.

Nie zmieniono OCR/preprocessingu/profili/PSM/whitelist, golden fixtures, obrazów źródłowych ani konfiguracji FAST/OCR/FULL. Nie dodano screenshotów ani realnych item fixtures. Nie uruchamiano OCR, FULL, `clean test` ani `package`. Nie utworzono nowego ZIP-a; SHA i timestamp zastanego archiwum pozostały identyczne.
