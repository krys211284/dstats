# Wydajność testów i budowania aplikacji

## BEFORE — zachowany baseline

Punkt startowy: `dstats-2026-10-09-16-07`. Zastane 157 raportów XML Surefire: **1106 testów, 0 failures/errors/skipped**. Suma atrybutów `testsuite.time`: **1376,957 s = 22 min 56,957 s**. Użytkownik podał około 23 min czasu pełnego `clean test`. XML mierzy czas suites, a nie pełny czas Maven; nie utożsamiamy tych dwóch miar.

Kompletna tabela suites, liczby testów, czasu i udziału: [baseline-suites.csv](baseline-suites.csv). Raporty zapisano przed pierwszym pomiarem i zmianami infrastruktury. Nie uruchomiono początkowego pełnego zestawu.

### TOP 20 suites BEFORE

| Suite | Testy | Czas [s] | Udział w sumie suites |
| --- | ---: | ---: | ---: |
| krys.itemimport.image.ItemPartStructureNormalizerPipelineTest | 6 | 199,165 | 14,46% |
| krys.itemimport.image.OpenCvHorizontalDividerDetectorTest | 18 | 181,764 | 13,20% |
| krys.itemimport.image.TesseractPropertyBlockOcrReaderTest | 6 | 160,011 | 11,62% |
| krys.itemimport.image.OpenCvItemImageAssemblerTest | 20 | 148,141 | 10,76% |
| krys.itemimport.image.PropertyBlockSegmenterPipelineTest | 14 | 121,619 | 8,83% |
| krys.itemimport.image.Part1RoiSegmenterPipelineTest | 3 | 117,986 | 8,57% |
| krys.itemimport.image.PropertyBlockPipelineDiagnosticsTest | 1 | 91,304 | 6,63% |
| krys.itemimport.image.TesseractPart1RoiOcrReaderTest | 5 | 90,511 | 6,57% |
| krys.itemimport.image.PropertyBlockNumericEvidenceAuditTest | 1 | 87,41 | 6,35% |
| krys.itemimport.image.PropertyBlockSemanticMapperFixtureTest | 3 | 86,441 | 6,28% |
| krys.itemimport.image.Part1ReadParserPipelineTest | 1 | 64,579 | 4,69% |
| krys.web.DziedzicImageImportPipelineRegressionTest | 1 | 9,354 | 0,68% |
| krys.web.CurrentBuildWebServerTest | 59 | 4,136 | 0,30% |
| krys.search.BuildSearchCalculationServiceTest | 5 | 2,871 | 0,21% |
| krys.web.SearchBuildWebServerTest | 5 | 1,993 | 0,14% |
| krys.web.ItemLibraryWebServerTest | 15 | 1,022 | 0,07% |
| krys.web.NecromancerCurrentBuildWebFlowTest | 16 | 0,988 | 0,07% |
| krys.web.DamageRankingWebServerTest | 21 | 0,839 | 0,06% |
| krys.web.NecromancerCorpseDamageRankingWebServerTest | 5 | 0,782 | 0,06% |
| krys.web.AppShellWebServerTest | 5 | 0,664 | 0,05% |

## Oddzielny pomiar sześciu suites i recognition calls

Przed zastąpieniem downstream OCR fixture'ami wykonano celowany pomiar sześciu wskazanych klas: **17 testów, 0 failures/errors/skipped, BUILD SUCCESS**. Czas zewnętrzny: **550,491 s**, suma suites: **536,531 s**. Włączono JFR oraz znacznik przed `doOCR(BufferedImage)` i `getWords(..., RIL_TEXTLINE)`. Liczymy wywołania rozpoznawania, a nie liczbę deklaracji profili; profil linewise może wykonywać wiele rzeczywistych odczytów. Te sześć klas obejmowało wszystkie zastane miejsca rzeczywistego OCR w testach.

| Suite | Testy | Osobny czas [s] | PART_1 calls | PropertyBlock calls | Razem |
| --- | ---: | ---: | ---: | ---: | ---: |
| krys.itemimport.image.Part1ReadParserPipelineTest | 1 | 87,515 | 39 | 0 | 39 |
| krys.itemimport.image.PropertyBlockNumericEvidenceAuditTest | 1 | 87,635 | 0 | 349 | 349 |
| krys.itemimport.image.PropertyBlockSemanticMapperFixtureTest | 3 | 97,43 | 0 | 349 | 349 |
| krys.itemimport.image.TesseractPart1RoiOcrReaderTest | 5 | 90,919 | 31 | 0 | 31 |
| krys.itemimport.image.TesseractPropertyBlockOcrReaderTest | 6 | 163,543 | 0 | 586 | 586 |
| krys.web.DziedzicImageImportPipelineRegressionTest | 1 | 9,489 | 6 | 132 | 138 |

Łącznie **1492 calls**. Downstream mapper, numeric audit i parser powtarzały **737 calls**: 349 + 349 + 39. Dane pomiaru: [baseline-ocr-measurement.csv](baseline-ocr-measurement.csv). Pozostałe testy realnego OCR zachowano; rozszerzono guards o porównanie pełnego outputu golden, również dla wszystkich dziesięciu bloków Dziedzica FULL.

## Źródłowy bottleneck i zakres optymalizacji

Zachowane XML pokazują, że sześć kosztownych suites OpenCV-only zajmowało łącznie **859,979 s**, około **62,45%** sumy suites. Sześć suites z OCR zajmowało **498,306 s**, około **36,19%**; ich czas również obejmował powtarzany assembler. Nie wystarczało wyłącznie oznaczyć OCR tagiem.

JFR z osobnego pomiaru: `FeatureImage.colorDiff` — **36 825 próbek / 95,29% próbek metod Java**, `hasSignal` — 0,99%, `scoreOverlap` — 0,70%. To udział w próbkach wykonania **Java**, nie w całym czasie natywnego OCR. Surowy lokalny profil i log: `logs/performance/baseline.jfr`, `baseline-hot-methods.txt`, `baseline-targeted.log`.

Wprowadzone zmiany:

- `@OcrIntegrationTest` / `@Tag("ocr-integration")`; FAST wyklucza wyłącznie tag, OCR wybiera wyłącznie tag, default FULL nie ma filtra.
- `PropertyBlockSemanticMapperFixtureTest`: known block + known pełny read result → rzeczywisty mapper; zachowane wszystkie dotychczasowe assertions.
- `PropertyBlockNumericEvidenceAuditTest`: 27 pełnych wyników; dodatkowo odtwarza evidence z zachowanych profili przez rzeczywisty aggregator. Macierz nadal 21 non-ambiguous oraz 6 ambiguous: Tarcza PART_3 #2/#3, Dziedzic PART_3 #2/#3/#6, Verathiel PART_3 #4.
- `Part1ReadParserPipelineTest`: siedem rzeczywistych snapshotów `Part1RoiOcrResult` → rzeczywisty parser; zachowane nazwy, klasyfikacja, sloty, gwiazdki, moc, jakość i wszystkie assertions.
- Golden XML zawiera pełne typowane modele: source slot/order, anchor, obraz i bounds bloku, raw text, preprocessing, diagnostics, osiem profili z score/failure/tokens oraz pełne numeric evidence z supporting profiles/support count/alternatives/ambiguity. PART_1 zachowuje także sub-ROI PNG, bounds, preprocessing i star count. Jedynym pomijanym polem w porównaniu realnego read jest `durationMillis`. Schemat jawnie definiuje kolejność pól, a zmiana modelu bez aktualizacji schematu kończy się błędem.
- SHA źródłowych obrazów, modelu i kodu odczytu zapisano w `src/test/resources/ocr/provenance.properties`; FAST sprawdza obraz/model i spójność evidence. Zwykłe testy nie zapisują ani nie aktualizują golden fixtures.
- Wspólny testowy cache assembly jest ograniczony do dokładnie tego samego uporządkowanego wejścia (filename, MIME, SHA-256 bytes). Nie obejmuje SUT testów assemblera ani E2E. Nowe warianty obrazu nadal wykonują prawdziwe składanie.
- Profilowany hot loop assemblera używa packed RGB, indeksów wierszy i dokładnych, zachowanych w tablicach różnic sąsiednich pikseli tekstury. Nie zmienia próbek, warunków zliczania tekstury, sumowania, scoringu, confidence, progów ani seam. Guards porównują cały PNG przez SHA-256 i wszystkie pola stitching do outputu oryginalnego assemblera. Dodatkowe tablice istnieją tylko podczas składania; jest to wymiana części pamięci na mniej ponownych obliczeń.
- `BundledTessdataProvider`: wspólny lazy model `pol`, cache per znormalizowany katalog, synchronizacja `ConcurrentHashMap.compute`, sprawdzenie SHA istniejącego modelu, publikacja kompletnego pliku, brak kolejnej kopii dla następnych readerów; odtworzenie po usunięciu pliku i ponowienie po błędzie przygotowania. Testy obejmują równoczesnych konsumentów, zachowanie timestampu, model istniejący/uszkodzony, usunięcie, retry po błędzie I/O i współdzielenie katalogu przez oba readery. To retry przygotowania pliku, nie retry failing OCR testów.
- Podczas etapu pomiarowego source ZIP przeniesiono do `source-bundle`; profil nie był uruchamiany. Późniejsza korekta workflow przywraca assembly do normalnej fazy `package` i usuwa ten zbędny profil. Thin JAR i `dependency:copy-dependencies` nadal należą do normalnego package.
- README i AGENTS definiują targeted → FAST → OCR dla image changes → FULL przy formalnym checkpoint → `-DskipTests package`.

Przy pomiarach wykryto konflikt kompilatora IDE z Mavenem: automatyczne przebudowy/przeładowanie POM usuwały klasy podczas runu (`NoClassDefFoundError`). Wyłączono oba mechanizmy w ustawieniach Java workspace. Przerwane pomiary nie są wynikami akceptacji. Nie ignorowano failures, nie dodano retry/Disabled ani nie zmieniono expected.

## AFTER — wyniki akceptacji

Czas zewnętrzny mierzono monotonicznym `Stopwatch` wokół polecenia Maven, z logiem zachowanym poza `target`, żeby końcowy `clean` nie usunął pomiarów. FAST/OCR oraz targeted miały dodatkowo diagnostyczne `-Ddstats.measureOcr=true`; nie zmienia ono filtrowania ani logiki OCR. Końcowy FULL wykonano **raz**, dokładnie `mvn '-Dmaven.repo.local=.m2' clean test`, bez profilu i bez diagnostycznego parametru. Następnie wykonano dokładnie `mvn '-Dmaven.repo.local=.m2' -DskipTests package`.

| Przebieg | Testy | Suites | Czas zewnętrzny [s] | Suma suites [s] | Wynik |
| --- | ---: | ---: | ---: | ---: | --- |
| targeted | 56 | 10 | 115,007 | 102,059 | BUILD SUCCESS |
| fast | 1104 | 157 | 233,612 | 229,061 | BUILD SUCCESS |
| ocr | 12 | 3 | 120,631 | 116,623 | BUILD SUCCESS |
| full | 1116 | 160 | 306,127 | 292,110 | BUILD SUCCESS |
| package (historyczny pomiar przed przywróceniem ZIP-a) | 0 | 0 | 6,772 | — | BUILD SUCCESS |

Porównanie jednakowej miary XML: **1376,957 → 292,110 s**, redukcja **78,79%**, przy większej liczbie testów. Czas pełnego polecenia AFTER: **306,127 s = 5 min 6,127 s** (Maven: 5 min 4 s), wobec około 23 min BEFORE.

Każdy przebieg testowy: **0 failures, 0 errors, 0 skipped**. Package: **zero wykonanych testów**, Surefire `Tests are skipped`. Pełne dane: [after-summary.csv](after-summary.csv), [after-fast-suites.csv](after-fast-suites.csv), [after-ocr-suites.csv](after-ocr-suites.csv), [after-full-suites.csv](after-full-suites.csv).

### Zachowanie pełnej regresji

FULL: **1116 testów / 160 suites**, wobec zastanych **1106 / 157**. Każda z 157 zastanych suites nadal istnieje i zachowała co najmniej tę samą liczbę przypadków. Nie usunięto żadnego zastanego przypadku. Dziesięć nowych przypadków sprawdza provider tessdata (4), bezpieczeństwo cache inputów (3) i spójność/provenance golden fixtures (3). Guards PNG oraz pełnego OCR dodano do istniejących przypadków.

FAST i OCR są rozłączne: **1104 + 12 = 1116**, **157 + 3 = 160 suites**. Ich suma odpowiada dokładnie FULL. W domyślnym FULL log zawiera obie dedicated Tesseract suites i `DziedzicImageImportPipelineRegressionTest`, wszystkie z wykonanymi przypadkami i bez skip.

### Rzeczywisty OCR AFTER

FAST: **0 recognition calls**, przy włączonym znaczniku pomiarowym. OCR: **836 calls** (PART_1 reader 43, PropertyBlock reader 655, E2E 138), wobec BEFORE **1492**. Usunięto 737 powtórzeń downstream; 81 dodatkowych odczytów wzmacnia porównanie całych PART_1 results i wszystkich dziesięciu bloków Dziedzica FULL. Netto: **656 mniej calls, 43,97% redukcji**. Pełny checkpoint nie włączał pomiarowych znaczników; obecność rzeczywistego OCR potwierdzają suites i ich guards.

Testy `ocr-integration`:

| Klasa | Przypadki | Zakres |
| --- | ---: | --- |
| `TesseractPart1RoiOcrReaderTest` | 5 | Rzeczywiste syntetyczne ROI i Tarcza/Dziedzic/Verathiel; pełna zgodność siedmiu golden PART_1 results. |
| `TesseractPropertyBlockOcrReaderTest` | 6 | Wszystkie dotychczasowe obrazowe warianty, numbers/text/evidence, osiem profili oraz zgodność całej macierzy 27 bloków. |
| `DziedzicImageImportPipelineRegressionTest` | 1 | Rzeczywiste image → assembler → divider → segmenter → Tesseract → semantic/value → verification/runtime. |

Kompletna lista 12 metod: [ocr-integration-tests.csv](ocr-integration-tests.csv).

### Pozostały hotspot i limit FAST

Preferowany limit **FAST ≤ 60 s nie został osiągnięty**: rzeczywisty wynik to **233,612 s**. Niczego nie wyłączono w celu zbliżenia się do tego limitu. Najdroższe w FAST:

| Suite | Czas [s] | Wyjaśnienie |
| --- | ---: | --- |
| `ItemPartStructureNormalizerPipelineTest` | 120,640 | Pierwszy konsument przygotowuje brakujące obrazy, w tym kosztowny syntetyczny split tarczy i realne pary. |
| `OpenCvItemImageAssemblerTest` | 83,392 | Sam SUT celowo wykonuje rzeczywiste niezależne przypadki bez cache. |
| `BuildSearchCalculationServiceTest` | 3,586 | Rzeczywisty runtime kandydatów. |
| `OpenCvHorizontalDividerDetectorTest` | 3,154 | Wspólny input skraca BEFORE 181,764 s bez wyłączenia 18 przypadków. |

Osobny pomiar dwóch najdroższych przypadków assemblera z potwierdzonym źródłowym SHA: [baseline-assembly-measurement.csv](baseline-assembly-measurement.csv) i [after-assembly-measurement.csv](after-assembly-measurement.csv). Syntetyczny split tarczy: **138,493 → 64,444 s**; para hełmu: **27,443 → 15,953 s**. Cały PNG oraz dokładne confidence/offset/overlap/seam pozostały identyczne.

Dalszy etap dla limitu 60 s powinien profilować i przyspieszyć dokładne wyliczanie overlapu w OpenCV assemblerze z tymi samymi guardami. Redukowanie profili, progów lub jakości OCR nie rozwiązuje pozostałego hotspotu. W tej rundzie nie zmieniono żadnego profilu, whitelist, PSM, preprocessingu, scoringu OCR ani reguł ambiguity.

## Package i runtime — historyczny pomiar przed przywróceniem ZIP-a

`-DskipTests package`: **BUILD SUCCESS**, czas zewnętrzny **6,772 s** (Maven raportuje 5,485 s). Log: `logs/performance/package.log`, wiersz `Tests are skipped`; brak `Running ...`, brak wykonanej assembly. Surefire nie powtórzył testów.

- `target/app/dstats.jar` istnieje: **3 440 657 bajtów**.
- `target/app/lib` istnieje i zawiera **54 biblioteki runtime**. Wszystkie ścieżki `Class-Path` manifestu wskazują istniejące pliki. OpenCV/Tess4J pozostają w dystrybucji, `pol.traineddata` jest w JAR.
- Smoke rzeczywistego `java -jar target/app/dstats.jar`: **HTTP 200**, prawidłowy app shell `Bohaterowie`. Test użył osobnego `-Ddstats.dataDir=target/package-smoke-data`, lokalnego hosta i dostępnego portu; proces zamknięto po sprawdzeniu.
- W tym pomiarze normalne package nie wykonywało `maven-assembly-plugin`; `source-bundle` nie był uruchomiony.
- **W tym pomiarze nie utworzono ZIP-a.** Zastany wcześniej source ZIP usunął `clean`, nie utworzono jego następcy.

## Aktualny kontrakt package po korekcie workflow

`maven-assembly-plugin` ponownie należy do domyślnej fazy `package`; profil `source-bundle` został usunięty. Deskryptor, nazwa `dstats-YYYY-MM-DD-HH-mm.zip`, katalog bazowy i wykluczenia są zachowane. ZIP zawiera źródła, testy, golden fixtures, dokumentację, `pom.xml` i `.gitignore`; nie zawiera `.m2`, `logs`, `target`, `.git`, `.idea` ani plików `*.iml`.

`mvn clean package` wykonuje domyślne FULL, buduje `target/app/dstats.jar`, kopiuje runtime dependencies do `target/app/lib` i tworzy ZIP projektu. `mvn clean package -DskipTests` dostarcza te same artefakty przy komunikacie Surefire `Tests are skipped` i zerze wykonanych testów. Czas historycznego package powyżej nie jest pomiarem tego przywróconego workflow z `clean` i ZIP-em.

Korekta dotyczy wyłącznie konfiguracji pakowania i dokumentacji. FAST/OCR/FULL, tagi OCR, golden fixtures, cache, OpenCV assembler, `BundledTessdataProvider` i logika aplikacji pozostają bez zmian. Walidacja korekty obejmuje `mvn '-Dmaven.repo.local=.m2' clean package -DskipTests`, kontrolę JAR/lib i zawartości ZIP-a oraz `git diff --check`; nie wymaga ponownego FULL. Lokalny log tego budowania: `logs/performance/package-workflow-fix.log`.

## Chronione pliki i kontrola diffu

[protected-files-sha256.csv](protected-files-sha256.csv) zawiera SHA-256 BEFORE/AFTER **396 zastanych plików `src/main` identycznych bajt w bajt**, w tym wszystkie wskazane obszary chronione: DamageEngine, ManualSimulationService, DirectDamageCalculator, NecromancerSeason15DirectDamageResolver, trzy modele RuntimeSkill, hero baseline, ItemTypeRegistry, item values, katalogi Nekromanty oraz Book of the Dead. Porównanie odnosi się do zastanego workspace, a nie do HEAD; wcześniejszych zmian użytkownika nie cofnięto.

Jedynymi zmienionymi zastanymi plikami produkcyjnymi są dwa readery (provider + jawna diagnostyka calls) oraz OpenCV assembler (reprezentacja RGB i zachowanie dokładnych różnic tekstury). Nowy plik produkcyjny to `BundledTessdataProvider`.

`git diff --check` w etapie optymalizacji: **0 błędów**. W tym etapie ZIP-a nie tworzono. OLŚNIENIA nie kontynuowano.
