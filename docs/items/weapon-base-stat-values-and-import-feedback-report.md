# Kanoniczne statystyki broni i komunikaty wyniku importu

Korekta kontynuuje poprzedni resolver; nie cofa wcześniejszej centralizacji statystyk broni ani zmian FAST/OCR/FULL i Maven package. Zakres produkcyjny tej korekty obejmuje wyłącznie `WeaponBaseStatConsistencyResolver` i `ItemImportPageRenderer`.

## Przyczyna DPS 533 i blokująca pewność

Poprzedni resolver wyznaczał `rangeCertain = min != null && max != null && certainty.min() && certainty.max()`. Warunek `!rangeCertain || aps == null || !certainty.aps()` wybierał `canonicalDps = observedDps`. Dlatego nawet istniejący poprawny zakres i APS pozostawiały `533`, gdy min, max albo APS miały niepewne evidence. Pewność samego observed DPS nie blokowała wyliczenia z pewnych źródeł.

Zgłoszony zestaw zawierał już average `5037`, a DPS nadal `533`. W poprzedniej wersji resolvera taki wynik przy kompletnych źródłach wskazuje na `apsCertain=false`: niepewność min/max blokowała również canonical average. Jest to wniosek z kodu i zgłoszonych pól, a nie odczyt flag z rzeczywistego OCR; zlecenie nie zawiera zrzutu tych flag. Testy obejmują niezależną niepewność min, max i APS oraz wszystkie ich kombinacje.

## Wartość i certainty

`rangePresent` zależy wyłącznie od obecności obu granic; `rangeCertain` osobno opisuje ich pewność. Poprawne obecne granice dają exact average i canonical average niezależnie od certainty. Obecny poprawny APS daje exact DPS i canonical DPS niezależnie od certainty. Zachowano `BigDecimal`, dokładny midpoint i `HALF_UP`; DPS nie mnoży wcześniej zaokrąglonej średniej.

`averageCertain = minCertain && maxCertain`, a przy kompletnych źródłach `dpsCertain = averageCertain && apsCertain`. Niepewność dodaje `WEAPON_BASE_STATS_UNCERTAIN`; różnica między observed i derived dodaje również `WEAPON_DPS_OCR_INCONSISTENT`. Zgodność liczb przy niepewnych źródłach nie potwierdza pewności wyniku. Tylko brak składnika blokuje wyliczenie z tego składnika; zachowano walidację niepoprawnych wartości i przepełnienia.

Regresja zgłoszonego zestawu we wszystkich siedmiu kombinacjach niepewności źródeł daje:

| Pole | Wynik |
|---|---:|
| observedDps | 533 |
| weaponDamageMin | 4269 |
| weaponDamageMax | 5805 |
| exactAverage / canonicalAverage | 5037 / 5037 |
| attacksPerSecond w formularzu | 0.90 |
| exactDps / canonicalDps | 4533.3 / 4533 |
| dpsCertain | false |

Przy niepewnym min/max `averageCertain=false`; przy niepewnym wyłącznie APS `averageCertain=true`. W obu przypadkach są obie diagnostyki i wymagane sprawdzenie. Test mappera używa neutralnych read resultów i istniejących test doubles dla ambiguous range albo APS; sprawdza wszystkie pięć pól formularza, canonical i observed w modelu, review notes oraz niezmienione raw `FullItemRead` i numeric evidence. Test renderera formularza sprawdza wartości i diagnostykę.

## Pojedynczy obraz i brak komunikatu

Controller już przyjmował pojedynczy obraz. Przy udanym assemblerze/dividerze oraz normalizacji `INCOMPLETE` zwracał preview bez draftu, z pustym głównym `openCvPreviewError`. Poprzedni `renderNormalImportStatus()` sprawdzał wyłącznie ten główny błąd. Formularza nie było, a pusty status powodował pozorną ciszę po kliknięciu.

Renderer sprawdza teraz status normalizacji oraz liczbę raw parts. Status użytkowy jest renderowany niezależnie od `diagnostics`; techniczny podgląd pozostaje włączany przez `?diagnostics=true`.

| Wynik pojedynczego obrazu | Zachowanie |
|---|---|
| Poprawne 3 lub 4 części | Formularz weryfikacji; opcjonalna `PART_2` bez zmian. |
| `INCOMPLETE`, 0/1/2 części | Brak formularza; komunikat o niekompletnej strukturze, liczba części, wymóg 3 lub 4 i instrukcja dodania pozostałych screenshotów od góry do dołu. |
| `INVALID_STRUCTURE` | Brak formularza; komunikat o niepoprawnej strukturze oraz liczba części i instrukcja. |
| Inny wynik przygotowania obrazu bez draftu | Ogólny komunikat o nieudanym odczycie, także gdy błąd wystąpił dopiero po assemblerze. |
| Ekran przed uploadem | Brak komunikatu o nieudanym imporcie. |

Niekompletna lub niepoprawna struktura nie jest nazywana błędem OpenCV. Controller i kontrakt normalizacji nie zostały zmienione. Testy HTTP sprawdzają pojedynczy upload dla 3/4 oraz 1/2/5 części w obu trybach diagnostycznych i potwierdzają brak wywołań dalszego odczytu przy niepoprawnej strukturze. Renderer pokrywa również status z 0 częściami. Wynik produkcyjnego dividera wymaga co najmniej jednej części; nie rozszerzano tego kontraktu, by wymusić sztuczny HTTP przypadek 0 części. Użyto istniejącego generatora obrazu w pamięci i istniejących doubles, bez nowych plików obrazów.

## Pliki zmienione w tej korekcie

- `src/main/java/krys/itemimport/WeaponBaseStatConsistencyResolver.java`
- `src/main/java/krys/web/ItemImportPageRenderer.java`
- `src/test/java/krys/itemimport/WeaponBaseStatConsistencyResolverTest.java`
- `src/test/java/krys/itemimport/image/ImageItemImportDraftMapperTest.java`
- `src/test/java/krys/web/ItemEditPageRendererTest.java`
- `src/test/java/krys/web/ItemImportPageRendererTest.java`
- `src/test/java/krys/web/ItemImportWebServerTest.java`
- `README.md`
- `docs/items/weapon-base-stat-consistency-report.md` — oznaczenie poprzedniego raportu jako historycznego.
- `docs/items/weapon-base-stat-values-and-import-feedback-report.md` — ten raport.
- `docs/items/weapon-base-stat-values-and-import-feedback-sha256.csv` — kontrola chronionych plików.

Pozostałe zmiany widoczne w `git status` pochodzą z poprzedniej poprawki i zostały zachowane bajtowo: `ItemImportDetails`, `ItemImportFormMapper`, `ImageItemImportDraftMapper`, `ItemEditPageRenderer`, `ItemImportFormMapperTest` i poprzedni manifest SHA.

## Walidacja

Targeted: `mvn '-Dmaven.repo.local=.m2' '-Dtest=WeaponBaseStatConsistencyResolverTest,ImageItemImportDraftMapperTest,ItemImportPageRendererTest,ItemImportWebServerTest,ItemEditPageRendererTest,ItemImportFormMapperTest' test`.

| Suite targeted | Przypadki |
|---|---:|
| WeaponBaseStatConsistencyResolverTest | 26 |
| ImageItemImportDraftMapperTest | 9 |
| ItemImportPageRendererTest | 14 |
| ItemImportWebServerTest | 20 |
| ItemEditPageRendererTest | 15 |
| ItemImportFormMapperTest | 27 |

Końcowe targeted: **111 testów / 6 suites, 10,872 s zewnętrznie, Maven 9,555 s, suma czasów suites 1,534 s, 0 failures / errors / skipped, BUILD SUCCESS**. Pierwszy przebieg ujawnił dwa niepoprawne test doubles próbujące zwrócić 0 części z dividera; przypadek ten przeniesiono do właściwej warstwy renderera, zachowując produkcyjny kontrakt dividera. Pozostałe testy przeszły już w pierwszym przebiegu.

FAST: `mvn '-Dmaven.repo.local=.m2' -Pfast test`: **1157 testów / 158 suites, 235,168 s zewnętrznie, Maven 3 min 53 s, suma czasów suites 230,688 s, 0 failures / errors / skipped, BUILD SUCCESS**. Korekta dodaje 24 wykonywane przypadki względem poprzedniego FAST (1133); istniejące przypadki nie zostały usunięte.

Zliczanie bierze suites z komunikatów `Running` danego logu i odpowiadające im XML Surefire. Nie sumuje starych XML z wcześniejszego FULL/OCR. Lokalne logi, zewnętrzne pomiary `Stopwatch`, kopie targeted XML i summary znajdują się w `logs/import-feedback/`.

## Zakres chroniony i artefakty

[Manifest SHA-256](weapon-base-stat-values-and-import-feedback-sha256.csv) zawiera before/after i zgodność **646 z 646 plików** poza zakresem korekty, w tym **399 plików src/main** i **wszystkie 23 zasoby src/test/resources**. Obejmuje DamageEngine, ManualSimulationService, DirectDamageCalculator, NecromancerSeason15DirectDamageResolver, RuntimeSkillKey/Configuration/Loadout, hero baseline, ItemTypeRegistry, katalogi skilli, Book of the Dead, obu readerów Tesseract, numeric evidence aggregator, preprocessing/assembler/divider/TessdataProvider oraz `pom.xml`, `AGENTS.md` i `.vscode/settings.json`.

Nie zmieniono OCR, profili FAST/OCR/FULL, tagów ani golden fixtures. Nie dodano itemów ani nowych PNG/JPG/WEBP lub realnych item fixtures. Nie uruchamiano OCR, FULL ani package i nie tworzono ZIP-a. Istniejący `target/dstats-2026-10-11-21-02.zip` zachował SHA-256 `F4AB1307E5526688BF8824660C831A5108B4C2F76D33AC88F7E8DA1A7E6CD71B`.

Ta walidacja potwierdza neutralny przepływ tych wartości do formularza i zachowanie UI z doubles. Nie jest nowym uruchomieniem rzeczywistego OCR ani aplikacji z przebudowanego `target/app/dstats.jar`; zgodnie ze zleceniem nie wykonywano package.

Końcowe `git diff --check`: **exit 0, bez błędów whitespace**. Ponowna kontrola po FAST potwierdziła 646 identycznych hashy oraz niezmieniony hash i czas istniejącego ZIP-a. Porównanie listy plików z początkiem korekty wykazało tylko dwa nowe pliki dokumentacji wymienione wyżej, bez nowych zasobów obrazowych.
