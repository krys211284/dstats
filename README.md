# Diablo 4 DPS Engine / Build WebApp

## Status dokumentu
Ten README opisuje wyłącznie aktualny stan projektu. Jest kontraktem wykonawczym dla implementacji od zera w pustym repo i nie zawiera historii decyzji, etykiet wersji ani logu zmian.

## Podsumowanie redakcyjne
- Scalono logicznie jeden wspólny kontrakt runtime dla `Damage Engine`, manual simulation i build search, jeden model statusów i targetowania single target, jeden kontrakt debugowania wyników oraz jeden kontrakt testów i golden values.
- Celowo pominięto historię wersji, tymczasowe poprawki techniczne, log błędów UI/CRUD, duplikaty tych samych reguł, komentarze typu "po tej rundzie testów" oraz zapisy później zastąpione nowszą regułą.
- Rozstrzygnięte konflikty źródeł: dokumentacja i UI używają nazwy `Vulnerable`, a historyczne `Exposed` pozostaje wyłącznie aliasem technicznym; search ocenia `build + skill bar`, a kolejność paska pozostaje semantyczna przez `LRU`; modyfikatory typu `replace` podmieniają bazowy hit tam, gdzie finalna reguła tak stanowi; czas trwania statusów wynika z definicji konkretnego efektu, a nie z niespójnych ogólnych zapisów historycznych.

## Spis treści
- [1. Cel projektu](#1-cel-projektu)
- [2. Kontrakty wykonawcze](#2-kontrakty-wykonawcze)
- [3. Architektura systemu](#3-architektura-systemu)
- [4. Model domeny](#4-model-domeny)
- [5. Damage Engine](#5-damage-engine)
- [6. Statusy i zasady targetowania](#6-statusy-i-zasady-targetowania)
- [7. Single hit / delayed hit / reactive damage](#7-single-hit--delayed-hit--reactive-damage)
- [8. Rotacja symulacji](#8-rotacja-symulacji)
- [9. Build search](#9-build-search)
- [10. UI, debug i prezentacja wyników](#10-ui-debug-i-prezentacja-wyników)
- [11. Testy i golden values](#11-testy-i-golden-values)
- [12. Zasady dostarczania](#12-zasady-dostarczania)

## 1. Cel projektu
Projekt `Diablo 4 DPS Engine / Build WebApp` nie jest definiowany produktowo jako narzędzie dla jednej klasy postaci. Docelowo architektura aplikacji ma wspierać wiele klas, ale aktualny zaimplementowany foundation domenowy i referencyjny runtime koncentrują się na obecnym zakresie klasowym repo.

Na dziś ten zaimplementowany zakres klasowy jest paladinocentryczny: to na nim opierają się bieżące skille, search foundation, przykłady referencyjne i golden values. Ten stan opisuje aktualny zakres implementacji, a nie definicję całego produktu.

Docelowo system ma wspierać dwa tryby pracy:
- `Policz aktualny build` - manual simulation dla aktualnej konfiguracji bohatera.
- `Znajdź najlepszy build` - build search oceniający legalne buildy i legalne konfiguracje paska skilli.

Aktualny stan repo obejmuje foundation backendowego searcha, minimalne GUI SSR oraz realny foundation importu pojedynczego itemu ze screena:
- minimalny wspólny silnik pojedynczego uderzenia dla `Brandish` i `Holy Bolt`,
- pierwszy pełny use case cooldownowego direct-hit runtime dla `Advance`,
- pełny pierwszy use case reactive foundation dla `Clash`,
- delayed hit `Judgement` dla bazowego rozszerzenia `Holy Bolt`,
- foundation reactive damage dla `Thorns` i `Retribution`,
- ograniczony kontrakt `Resolve`, `Crusader's March` i `Punishment` potrzebny do use case `Clash`,
- tickową manual simulation dla trybu `Policz aktualny build`,
- realny model wejścia użytkownika oparty o `CurrentBuildRequest`,
- `CurrentBuildSnapshotFactory` budujący `HeroBuildSnapshot` z realnych danych wejściowych użytkownika,
- wspólną usługę aplikacyjną `CurrentBuildCalculationService` nad istniejącym runtime,
- osobną warstwę aplikacyjną backendowego searcha opartą o `BuildSearchRequest`,
- generator legalnych kandydatów searcha obejmujący aktualny foundation skilli i action bara,
- ranking kandydatów po `total damage` i `DPS` liczonych przez ten sam runtime,
- warstwę prezentacyjną normalizującą wyniki searcha po zakończonej ocenie kandydatów,
- nowy pierwszorzędny byt produktu `bohater` z własnym kontekstem buildu, aktywnymi slotami, przypisanymi umiejętnościami i paskiem akcji,
- moduł `Bohaterowie` z listą wielu bohaterów, tworzeniem, wyborem aktywnego bohatera i usuwaniem,
- minimalne webowe GUI SSR dla trybu `Policz aktualny build`,
- hero-centryczny ekran `Policz aktualny build`, który pracuje jawnie w kontekście aktywnego bohatera,
- inline zmianę aktywnego bohatera bez opuszczania ekranu `Policz aktualny build`,
- czytelną edycję poziomu aktywnego bohatera wyłącznie w sekcji `Punkty umiejętności` na ekranie `Policz aktualny build`,
- sekcję `Ekwipunek aktualnego buildu` pokazującą pełny układ slotów bohatera jako stały layout SSR z aktywnym itemem albo pustym slotem,
- bezpośrednią zmianę aktywnego itemu per slot z poziomu current build w kontekście aktywnego bohatera bez budowania osobnego runtime ekwipunku,
- czytelne rozdzielenie operacji slotu na `Wybierz z biblioteki`, `Importuj nowy item`, `Zmień item` i `Wyczyść slot`,
- jawny model przypisanych umiejętności bohatera z trwałym zapisem rangi, bazowego ulepszenia i dodatkowego modyfikatora,
- render current build ograniczony do umiejętności przypisanych aktywnemu bohaterowi oraz pasek akcji wybierający wyłącznie spośród przypisanych i nauczonych umiejętności,
- zwinięte szczegóły użytych itemów pokazujące, które aktywne itemy rzeczywiście składają się na bieżący build i jaki mają wkład,
- prawdziwy ekran główny `/` działający jako hub aplikacji z grupami modułów i statusami,
- globalną nawigację SSR wspólną dla głównych ekranów aplikacji,
- wspólny renderer app shell oraz wspólny zestaw tokenów wizualnych dla głównych ekranów SSR, z czytelnym wyróżnianiem aktywnego modułu, akcji i statusów,
- centralny rejestr modułów aplikacji z opisem, grupą, statusem i URL,
- placeholder pages dla przyszłych sekcji dodatku i sezonu bez implementacji ich mechaniki,
- pierwszy SSR flow `Importuj item ze screena` dla pojedynczego itemu z ręcznym potwierdzeniem użytkownika,
- realny odczyt OCR pojedynczego screena itemu jako pełniejszego rekordu itemu oraz osobną projekcję kompatybilności do pól foundation,
- preprocessing obrazu itemu przed OCR z heurystycznym wycięciem obszaru tekstowego oraz kilkoma wariantami obrazu,
- deterministyczne scalanie wyników OCR z kilku wariantów bez zmiany runtime current build, simulation i searcha,
- parser polskich affixów foundation dla rozpoznawalnych fraz slotu i statów,
- model wstępnego rozpoznania z poziomem pewności i uwagami per pole,
- walidowany formularz zatwierdzonego itemu z edytowalną listą affixów jako główną warstwą ręcznej korekty oraz osobnym mapowaniem do aktualnego modelu buildu,
- automatyczny zapis zatwierdzonego itemu do biblioteki po ręcznym potwierdzeniu pól importu,
- minimalną trwałą bibliotekę zapisanych itemów z wieloma itemami tego samego slotu, zapisywaną lokalnie w stabilnym katalogu użytkownika,
- osobną bazę wiedzy itemów uczącą się wyłącznie z ręcznie zatwierdzonych itemów, z aktywną epoką wiedzy, obserwowanymi typami affixów, obserwowanymi aspektami i resetem sezonowym,
- wspólną bibliotekę zapisanych itemów oraz niezależny wybór aktywnego itemu per slot dla każdego bohatera,
- wyraźny wynik zapisu importowanego itemu do biblioteki z nazwą, slotem, identyfikatorem i akcjami `Załóż bohaterowi`, `Zmień w slocie` oraz `Wróć do aktualnego buildu`,
- nowy tryb searcha po bibliotece itemów, który generuje kombinacje zapisanych itemów per slot w kontekście aktywnego bohatera i nadal składa je do effective current build przed tym samym runtime,
- uproszczony i bardziej produktowy formularz GUI searcha z wyeksponowanym trybem biblioteki itemów,
- ujednolicony polski język głównych ekranów SSR, helperów, sekcji wynikowych i akcji użytkownika,
- empty state sekcji `Wstępnie rozpoznane pola` w imporcie pojedynczego itemu,
- pierwsze minimalne webowe GUI SSR dla trybu `Znajdź najlepszy build`,
- pierwszy drill-down SSR z wyniku searcha do pełnej analizy reprezentanta znormalizowanego wyniku na tym samym runtime co manual simulation,
- ogólny opisowy ekran SSR `/ranking-obrazen` z parametrem `character=paladin`, pokazujący wszystkie 24 wpisy z `PaladinSkillTreeRegistry`, ich status policzalności, źródła opisowe i blokadę DPS dla niezweryfikowanych mechanik,
- edytowalne źródła Markdown Paladyna w `docs/paladin/source-md/` oraz pomocniczy ekstrakt JSON tabel rang z lokalnej paczki HTML Fextralife,
- foundation audytu/preflightu searcha z liczbą legalnych kandydatów i rozmiarem search space,
- minimalny progress CLI searcha dla etapu oceny kandydatów,
- CLI dla manual simulation oraz osobne CLI backendowego searcha jako równoległe smoke testy tego samego runtime.

Pełny system zasobów, pełny system defensywnych statusów, pełne feature'y `Fervor`, pełny ogólny system `Resolve`, `Fala Zealot`, live progress searcha w GUI, eksport CSV, wielowątkowość, pełny wielo-itemowy flow, pełna sesja ekwipunku, pełny OCR całej postaci oraz pełna docelowa warstwa UX/UI pozostają poza bieżącym zakresem kodu.

Kontrakt architektoniczny jest wspólny dla obu trybów:
- oba tryby muszą używać tego samego `Damage Engine`,
- oba tryby muszą używać tej samej logiki ticków, statusów, targetowania i wyboru skilla,
- wynik searcha musi dać się odtworzyć manualnie po zastosowaniu tego samego buildu i tego samego paska skilli.

## 2. Kontrakty wykonawcze
- README jest źródłem prawdy dla architektury, logiki runtime, zasad testowych i kontraktu UI/debug.
- Każda zmiana logiki wymaga równoczesnej aktualizacji kodu, testów i README.
- Nie wolno zgadywać wartości liczbowych w kodzie ani w testach.
- Wszystkie nowe liczby w testach muszą wynikać z istniejących golden values albo z aktualnego outputu engine dla tego samego buildu.
- Dokumentacja projektowa oraz komentarze w kodzie muszą być prowadzone po polsku.
- Silnik, symulacja i search muszą być deterministyczne.
- RNG nie należy do logiki kontraktowej; dla efektów probabilistycznych używany jest `expected value`.
- Domyślnym modelem projektu jest single target.
- Każde dostarczenie projektu musi być pełną paczką projektu; nie wolno dostarczać niepełnych paczek ani placeholderów zamiast realnej logiki.
- Paczka projektu nie może być generowana z failing testami; wymagane jest `100%` przechodzących testów.

## 3. Architektura systemu
### 3.1. Główne założenia
- Architektura jest data-driven.
- Aktualny kod implementuje jeden wspólny foundation `Damage Engine` dla single hit i komponentów obrażeń.
- Ten sam foundation ma pozostać bazą dla przyszłej manual simulation i build search.
- Search nie może docelowo używać skróconej lub alternatywnej logiki względem manual simulation.

### 3.1.1. App shell i moduły
Aktualny frontend SSR posiada produktową warstwę app shell nad istniejącymi flow obliczeń.

Kontrakt app shell:
- root `/` jest prawdziwym ekranem głównym aplikacji, a nie technicznym przekierowaniem do innego formularza,
- ekran główny grupuje moduły w obszary `Narzędzia builda`, `Itemy i import` oraz `Systemy dodatku i przyszłe sekcje`,
- istnieje centralny rejestr modułów opisujący `id`, nazwę, opis, grupę, status, URL oraz to, czy moduł jest aktywny czy placeholderowy,
- moduł `Bohaterowie` jest częścią aktywnej nawigacji produktu i stanowi punkt wejścia do pracy na aktywnym bohaterze,
- globalna nawigacja SSR jest renderowana z tego samego rejestru modułów, a nie z rozproszonych ręcznych linków w wielu ekranach,
- główne ekrany SSR korzystają z tego samego renderera app shell i tego samego zestawu tokenów wizualnych dla tła, paneli, przycisków, statusów i aktywnej zakładki,
- główne ekrany SSR korzystają też z szerszego wspólnego kontenera layoutu, żeby current build, search i biblioteka lepiej wykorzystywały szerokie monitory bez łamania mobilnego SSR,
- aktywny moduł `Ranking obrażeń` jest ogólnym ekranem nad `DamageRankingService` i providerem rejestrów klas; dla `character=paladin` używa `PaladinSkillTreeRegistry` i nie implementuje nowych formuł DPS,
- istniejące flow `Policz aktualny build`, `Importuj item ze screena`, `Biblioteka itemów`, `Znajdź najlepszy build` i drill-down searcha pozostają cienkimi warstwami nad tym samym runtime,
- placeholder pages są świadomą warstwą produktową przygotowującą architekturę aplikacji pod przyszłe sekcje, a nie atrapą zastępującą istniejącą logikę runtime.

### 3.2. Wspólne wejście runtime
Produktowy model wejścia użytkownika dla manual simulation ma postać `CurrentBuildRequest`.

Kontrakt aktualnej warstwy aplikacyjnej:
- GUI mapuje formularz do `CurrentBuildRequest` przez `CurrentBuildFormMapper`,
- CLI mapuje argumenty do `CurrentBuildRequest` przez `CurrentBuildCliRequestParser`,
- `CurrentBuildSnapshotFactory` buduje z requestu runtime `HeroBuildSnapshot`,
- `CurrentBuildCalculationService` uruchamia ten sam runtime dla GUI i CLI,
- GUI importu itemu mapuje upload obrazu do `ItemImageImportRequest`,
- `ItemImageImportService` wykonuje techniczną walidację obrazu, przygotowuje kilka wariantów obrazu, uruchamia realny OCR pojedynczego screena itemu i buduje `ItemImageImportCandidateParseResult`,
- `ItemImageOcrPreprocessor` wycina heurystycznie obszar tekstowy, ogranicza wpływ ramki, grafiki itemu i dolnego overlayu oraz przygotowuje warianty `grayscale`, `upscale`, `contrast`, `threshold`, lekkie wyostrzenie i dolny crop efektu legendarnego powiększony pod OCR,
- `WindowsItemOcrTextReader` uruchamia OCR dla kilku wariantów obrazu,
- `ItemImageImportCandidateMerger` scala per pole wyniki z kilku wariantów OCR w sposób deterministyczny, deduplikuje stabilne linie `FullItemRead` po kluczu semantycznym i obniża pewność przy równorzędnych sprzecznych odczytach,
- `ItemImportEditableFormFactory` przygotowuje strukturalny `ItemImportDraft`, a dopiero z niego formularz ręcznego potwierdzenia,
- `AspectRegistry` jest katalogiem znanych aspektów dostępnych w imporcie i waliduje zgodność wybranego aspektu ze slotem itemu,
- `AffixRegistry` / `ApplicationAffixRegistry` jest osobnym katalogiem affixów, analogicznym koncepcyjnie do katalogu aspektów, ale niezależnym od niego; służy do dopasowania OCR, ręcznej korekty i przyszłego tworzenia idealnych itemów na bazie katalogu,
- `ItemImportFormMapper` waliduje ręcznie poprawiony item do `ValidatedImportedItem`,
- `ValidatedImportedItemToItemMapper` mapuje zatwierdzony item do aktualnego modelu `Item`,
- `ImportedItemCurrentBuildContributionMapper` mapuje zatwierdzony item do aktualnego agregowanego modelu buildu używanego przez `CurrentBuildRequest`,
- `ImportedItemCurrentBuildApplicationService` stosuje zatwierdzony item do istniejących statów current build w trybie `nadpisz` albo `dodaj wkład`,
- `HeroProfileRepository` trwale zapisuje bohaterów aplikacji oraz identyfikator aktywnego bohatera bez systemu kont użytkowników,
- `HeroService` zarządza listą bohaterów, aktywnym bohaterem, jego kontekstem buildu, poziomem, aktywnymi slotami oraz przypisanymi umiejętnościami,
- `ItemLibraryRepository` trwale zapisuje minimalną wspólną bibliotekę zatwierdzonych itemów bez duplikowania jej per bohater,
- `ItemLibraryService` zapisuje zatwierdzony item do biblioteki, udostępnia listę zapisanych itemów, waliduje zgodność itemu z hero slotem i składa aktywne itemy wybranego bohatera do effective current build,
- SSR current build pokazuje selekcję aktywnych slotów aktywnego bohatera jako `Ekwipunek aktualnego buildu` i zmienia aktywny item per slot bez budowania bocznego modelu equipment runtime,
- SSR current build pozwala zmienić aktywnego bohatera bez opuszczania ekranu buildu, a poziom bohatera edytuje tylko w sekcji `Punkty umiejętności`,
- bohater ma własny trwały `HeroSkillLoadout`, a current build renderuje i zapisuje wyłącznie umiejętności przypisane aktywnemu bohaterowi,
- przypisywanie umiejętności bohatera odbywa się bezpośrednio na ekranie `Policz aktualny build` przez sekcję `Umiejętności bohatera` i operacje `Dodaj umiejętność` oraz `Usuń umiejętność`,
- `/bohaterowie` pozostaje modułem zarządzania profilami, a `/policz-aktualny-build` jest istniejącym miejscem edycji przypisanych umiejętności aktywnego bohatera,
- karty przypisanych umiejętności mogą pokazywać opisowy blok aktualnej konfiguracji z tego samego `PaladinSkillTreeRegistry`, którego używa `/ranking-obrazen`; pierwszy most prezentacyjny mapuje legacy `SkillId.CLASH` na wpis registry `starcie`, bez zmiany enumu runtime,
- `/policz-aktualny-build` pokazuje dane przypisanej umiejętności na aktualnej randze kupionej punktami: ranga `0` oznacza umiejętność przypisaną, ale bez aktywnych danych bojowych, a procent obrażeń jest pokazywany tylko wtedy, gdy istnieje jawna wartość dla tej rangi; ekran nie interpoluje między R1 i max drzewa,
- przypisanie umiejętności nie aktywuje automatycznie wszystkich modyfikatorów katalogowych z drzewa Paladyna; aktywne modyfikatory prezentacyjne wynikają z aktualnej konfiguracji legacy, są renderowane jako nazwy z tooltipami źródłowymi i nie odblokowują runtime DPS, nie sumują komponentów ani nie zastępują sekcji `Konfiguracja runtime legacy`,
- bazowe generowanie Wiary pokazywane przy przypisanej umiejętności jest bazową wartością umiejętności, a nie aktywnym modyfikatorem konfiguracji; modyfikator `Generowanie Wiary` może pojawić się w aktywnych modyfikatorach tylko wtedy, gdy istnieje jawne mapowanie aktualnej konfiguracji,
- current build waliduje budżet punktów umiejętności: poziom bohatera `1..70` daje `max(0, poziom - 1)` punktów, dodatkowe punkty z zadań mają zakres `0..14`, więc poziom `70` z kompletem zadań daje maksymalnie `83` punkty,
- koszt konfiguracji umiejętności to suma rang kupionych punktami, aktywnych bazowych ulepszeń i wybranych dodatkowych modyfikatorów innych niż `Brak`; ranga kupiona punktami ma zakres `0..15`, ranga `0` bez ulepszeń kosztuje `0`, ranga `15` kosztuje `15`, a przekroczenie dostępnego budżetu oznacza nielegalną konfigurację bez automatycznego usuwania wyborów użytkownika,
- itemowe bonusy do poziomu/rangi umiejętności są osobną przyszłą warstwą względem rangi kupionej punktami, nie kosztują punktów umiejętności i w tym etapie nie implementują `effectiveRank`,
- `/policz-aktualny-build` używa szerokiego wariantu layoutu i zwijanych sekcji dla edycji aktualnego builda; duży widoczny hero nagłówek strony został usunięty, poziom bohatera edytuje się tylko w sekcji `Punkty umiejętności`, a zwinięty panel `Aktywny bohater` pokazuje poziom wyłącznie informacyjnie,
- wszystkie główne sekcje `/policz-aktualny-build` są domyślnie zwinięte; sekcja punktów nie pokazuje stałego komunikatu poprawności, a błędy budżetu pojawiają się dopiero wtedy, gdy wystąpią,
- główne edytowalne pola aktualnego builda zapisuje sticky pasek `Zapisz zmiany`; błędnego przydziału punktów albo nielegalnego paska akcji nie da się zapisać do profilu, a link `Wycofaj zmiany` przeładowuje `/policz-aktualny-build` z ostatnio zapisanym stanem i nie usuwa bohatera, przypisanych umiejętności ani itemów,
- pasek akcji current build ma `6` miejsc i jest ograniczony do przypisanych oraz nauczonych umiejętności aktywnego bohatera; nielegalne wpisy blokują zapis zamiast cichego czyszczenia profilu,
- `ItemLibraryDataDirectoryResolver` rozwiązuje katalog trwałych danych biblioteki itemów przez `dstats.dataDir` albo domyślny katalog użytkownika `~/.dstats/item-library/` i wykonuje bezpieczną migrację z legacy `target/item-library-runtime/`,
- biblioteka itemów pozostaje warstwą aplikacyjną przed `CurrentBuildRequest`, a aktywny wybór slotów należy do konkretnego bohatera,
- effective current build jest składany jako `bohater + jego ręczne nadpisanie statów + jego aktywne itemy per slot -> finalne effective current build stats -> CurrentBuildRequest -> CurrentBuildSnapshotFactory -> runtime`,
- ekran `Policz aktualny build` jest roboczym ekranem konfiguracji bohatera i pokazuje hierarchię `punkty umiejętności -> umiejętności -> pasek akcji -> podstawowe statystyki bohatera -> ekwipunek -> efektywne staty -> wynik/debug`; efektywne staty do obliczeń są podsumowaniem wejścia do runtime na końcu, `Szczegóły użytych itemów` i `Zaawansowane ręczne nadpisanie statów` nie są głównymi sekcjami UI, a debugi symulacji są w końcowej zwiniętej sekcji,
- pusty slot w current build rozdziela dwie ścieżki operacyjne: `Wybierz z biblioteki` dla istniejących itemów oraz `Importuj nowy item` dla dopisania nowego zapisu do wspólnej biblioteki,
- slot z aktywnym itemem pokazuje operacje `Zmień item` i `Wyczyść slot`, a import pozostaje ścieżką pomocniczą do dopisania nowego itemu,
- tryb searcha po bibliotece itemów używa analogicznego kontraktu `aktywny bohater + jego ręczna baza searcha + kandydacka kombinacja zapisanych itemów z biblioteki -> finalne effective current build stats -> CurrentBuildRequest -> CurrentBuildSnapshotFactory -> runtime`,
- walidacja wejścia current build dotyczy finalnych effective stats mapowanych do `CurrentBuildRequest`, a nie wyłącznie surowej ręcznej bazy formularza,
- GUI importu itemu pozostaje cienką warstwą wejściową nad obecnym modelem current build w kontekście aktywnego bohatera i nie implementuje alternatywnego runtime,
- CLI searcha mapuje argumenty do `BuildSearchRequest` przez `BuildSearchCliRequestParser`,
- GUI searcha mapuje formularz do `BuildSearchRequest` przez `SearchBuildFormMapper`,
- GUI searcha dziedziczy domyślne wartości z aktywnego bohatera i ogranicza przestrzeń skilli do umiejętności przypisanych temu bohaterowi,
- `BuildSearchCalculationService` generuje legalnych kandydatów przez `BuildSearchCandidateGenerator`,
- przy włączonym trybie biblioteki itemów `BuildSearchCandidateGenerator` pobiera deterministyczne kombinacje `0..1 zapisany item per slot`, składa ich wkład do effective current build i dopiero wtedy buduje `CurrentBuildRequest`,
- każdy kandydat searcha jest adaptowany do `CurrentBuildRequest`, a następnie do `HeroBuildSnapshot` przez ten sam `CurrentBuildSnapshotFactory`,
- `BuildSearchEvaluationService` ocenia kandydatów przez ten sam `ManualSimulationService`,
- `BuildSearchCandidateGenerator` liczy także preflight/audit dokładnie dla tej samej legalnej przestrzeni kandydatów, którą później generuje do oceny,
- `BuildSearchPresentationNormalizer` działa dopiero po zakończeniu surowej oceny i normalizuje wyłącznie warstwę prezentacji wyników,
- `BuildSearchProgressListener` raportuje wyłącznie postęp warstwy aplikacyjnej searcha i nie zmienia logiki oceny,
- GUI searcha jest cienkim SSR nad `BuildSearchCalculationService` i nie implementuje bocznej logiki searcha,
- GUI searcha pokazuje audit/preflight obok wyniku, ale nie implementuje live progressu,
- drill-down GUI searcha mapuje wybranego reprezentanta wyniku po normalizacji do `CurrentBuildRequest` i uruchamia ten sam `CurrentBuildCalculationService` co flow `Policz aktualny build`,
- drill-down searcha nie przywraca bocznie nieprzypisanych umiejętności, tylko odtwarza request z tym samym ograniczeniem skilli co wynik searcha,
- `AppModuleRegistry` jest centralnym rejestrem modułów aplikacji używanym przez ekran główny, placeholder pages i globalną nawigację,
- `HomeController` oraz `HomePageRenderer` budują SSR hub aplikacji na `/`,
- `PlaceholderPageController` oraz `PlaceholderPageRenderer` obsługują przyszłe sekcje dodatku i sezonu bez implementacji ich mechaniki,
- scenariusze referencyjne pozostają wyłącznie trybem pomocniczym budowanym już na `CurrentBuildRequest`,
- `SampleBuildFactory` nie jest główną ścieżką flow użytkownika; pozostaje pomocą testową niższego poziomu.

Wspólne wejście runtime dla manual simulation i dalszego rozwoju searcha ma postać `HeroBuildSnapshot`. Ten model musi zachowywać pełny stan buildu używany przez runtime:
- tożsamość i klasę bohatera,
- poziom bohatera,
- bonusowe punkty skilli,
- średnie obrażenia broni używane przez engine,
- globalny bonus procentowy do obrażeń, jeśli wynika z buildu,
- bazowe `Thorns` z buildu,
- `block chance`,
- `retribution chance`,
- ekwipunek per slot,
- nauczone skille z rangami,
- stan bazowego rozszerzenia i dodatkowego modyfikatora dla każdego skilla,
- aktualnie wybrany pasek aktywnych skilli.

Snapshot nie może gubić:
- equipment,
- rang skilli,
- rozszerzeń,
- konfiguracji paska,
- innych danych wpływających na runtime.

### 3.3. Wspólne wyjście runtime
Aktualne wspólne wyjście foundation ma postać:
- `DamageBreakdown`
- `DamageComponentBreakdown`
- `DelayedHitBreakdown`
- `ReactiveHitBreakdown`
- `SkillHitDebugSnapshot`
- `SimulationStepTrace`
- `SkillBarStateTrace`
- `SimulationResult`

Na tym etapie te modele są źródłem prawdy dla:
- wyniku pojedynczego uderzenia,
- wyniku krytycznego,
- listy komponentów obrażeń,
- informacji o wliczeniu albo pominięciu komponentu w single target,
- reprezentatywnego debug bezpośredniego hita dla każdego skilla użytego w symulacji,
- listy delayed hitów w manual simulation,
- listy reactive hitów w manual simulation,
- tickowego `stepTrace` dla dokładnie tej samej symulacji, która liczy wynik końcowy,
- total reactive damage dla trybu `Policz aktualny build`,
- total damage i DPS dla trybu `Policz aktualny build`.

Docelowe bogatsze modele wyników dla manual simulation, searcha i UI pozostają poza aktualnym zakresem implementacji.

## 4. Model domeny
### 4.1. Build
Build bohatera to:
- bohater,
- wyposażone itemy,
- nauczone skille,
- wariant każdego skilla,
- aktualny pasek aktywnych skilli.

Reguły legalności buildu:
- equipment jest zapisany per slot,
- item nie może być użyty równocześnie w więcej niż jednym slocie,
- reguła dotyczy także ringów,
- założenie broni 2H czyści off-hand,
- `Item Power` i `Required Level` są metadanymi i nie wpływają na damage engine.

### 4.2. Model skilla
Stan pojedynczego skilla obejmuje:
- `OFF` albo `rank 1..5`,
- `bazowe rozszerzenie` jako osobny stan,
- maksymalnie jeden `dodatkowy modyfikator`.

Reguły legalności stanu skilla:
- dodatkowy modyfikator nie może istnieć bez bazowego rozszerzenia,
- skill z upgradem przy `rank 0` jest nielegalny,
- skill może mieć maksymalnie jeden dodatkowy modyfikator.

### 4.3. Rejestr drzewa Paladyna
Model drzewa Paladyna w nowych funkcjach korzysta z lokalnych materiałów źródłowych w `docs/paladin/`. Rejestr `krys.paladin.PaladinSkillTreeRegistry` rozróżnia wpisy umiejętności w drzewie, grupy ulepszeń, pojedyncze ulepszenia, `sourcePdf`, `skillGroup`, typ skilla oraz status obsługi.

`docs/paladin/source-md/` jest nowym edytowalnym źródłem opisowego drzewa Paladyna. Zawiera wersje Markdown rejestrów umiejętności oraz materiały pomocnicze, które można rozwijać bez edycji PDF-ów.

`docs/paladin/source-pdfs/` jest nadal utrzymywane jako archiwum i źródło porównawcze dla wcześniejszych PDF-ów. PDF-y nie są już opisywane jako jedyne źródło prawdy, ale pozostają użyteczne do audytu pochodzenia danych i regresji SHA istniejących materiałów.

Aktualny rejestr drzewa Paladyna pozostaje pełnym rejestrem opisowym wpisów umiejętności w drzewie ze wszystkich lokalnych materiałów przeniesionych z PDF-ów źródłowych:
- `paladin_basic_skill_registry_final.pdf`,
- `paladin_core_skill_registry_final.pdf`,
- `paladin_aura_skill_registry_final.pdf`,
- `diablo4_paladyn_odwaga_umiejetnosci.pdf`,
- `diablo4_paladyn_sprawiedliwosc_umiejetnosci.pdf`,
- `moce_specjalne_diablo4.pdf`.

Wymagane edytowalne materiały Markdown w `docs/paladin/source-md/`:
- `paladin_basic_skill_registry_final.md`,
- `paladin_core_skill_registry_final.md`,
- `paladin_aura_skill_registry_final.md`,
- `diablo4_paladyn_odwaga_umiejetnosci.md`,
- `diablo4_paladyn_sprawiedliwosc_umiejetnosci.md`,
- `moce_specjalne_diablo4.md`,
- `paladin_fextralife_rank_tables.md`,
- `paladin_fextralife_html_manifest.md`.

`docs/paladin/source-md/paladin_fextralife_rank_tables.json` jest maszynowym ekstraktem tabel rang z lokalnej paczki HTML Fextralife. JSON jest pomocniczym źródłem danych dla modeli `DamagePercentRankTable` i `DamagePercentComponentRankTable`, ale sam nie przełącza `PaladinSkillTreeRegistry` automatycznie na pełne tabele rang i nie oznacza implementacji realnego DPS.

Przed masowym importem wartości obrażeń z JSON-a obowiązuje audyt w `docs/paladin/source-md/paladin_damage_rank_table_audit.md`. Audyt klasyfikuje każdy wpis do jednej z kategorii:
- `SIMPLE_SINGLE_COMPONENT` - jedna jednoznaczna tabela obrażeń, kandydat do `DamagePercentRankTable`,
- `MULTI_COMPONENT` - więcej niż jeden komponent obrażeń, kandydat do przyszłego modelu komponentowego,
- `NON_DAMAGE` - brak bezpośredniej tabeli obrażeń albo wpis utility/support/status/cooldown,
- `NEEDS_MANUAL_REVIEW` - dane niejednoznaczne, podejrzane albo wymagające ręcznej weryfikacji.

Audyt jest dokumentem źródłowym procesu importu. Sam dokument audytu nie odblokowuje runtime DPS i nie zastępuje testów mechaniki single target.

`DamagePercentRankTable` jest domenowym modelem pełnej tabeli bazowych procentów obrażeń per ranga. Tabela przechowuje wartości `rank -> damagePercent` dla dopuszczalnych rang `1..15`, gdzie liczba całkowita `115` oznacza `115%`. Model odrzuca rangi poza zakresem `1..15`, odrzuca `null` rank/value przy tworzeniu, przy odczycie brakującej poprawnej rangi zwraca `null` i pozostaje niemutowalny po utworzeniu.

`DamagePercentComponent` nazywa komponent obrażeń w skillach wielokomponentowych, np. `PRIMARY_DAMAGE`, `ADDITIONAL_STRIKE_DAMAGE`, `PASSIVE_DAMAGE`, `ACTIVE_DAMAGE`, `JUMP_DAMAGE`, `LANDING_DAMAGE`, `BURST_DAMAGE`, `DAMAGE_PER_SECOND`, `FIRST_STRIKE_DAMAGE` i `SECOND_STRIKE_DAMAGE`. `DamagePercentComponentRankTable` przechowuje mapę `DamagePercentComponent -> DamagePercentRankTable`. Ten model jest źródłowym opisem komponentów, a nie gotowym DPS.

Po audycie do `PaladinSkillTreeRegistry` zaimportowano proste tabele `DamagePercentRankTable` tylko dla skilli sklasyfikowanych jako `SIMPLE_SINGLE_COMPONENT` i `recommendedModel = DamagePercentRankTable`: `wymach`, `swiety_pocisk`, `starcie`, `natarcie`, `blogoslawiona_tarcza`, `blogoslawiony_mlot`, `boska_lanca`, `uderzenie_tarcza`, `szarza_z_tarcza`, `skazanie` i `konsekracja`. Każda z tych tabel ma komplet rang `1..15` przepisany z lokalnego JSON-a. `blogoslawiony_mlot` zachowuje dotychczasową tabelę `1=115 ... 15=293`.

Skille `MULTI_COMPONENT` nie są importowane do prostych tabel rang, nawet jeśli lokalny JSON zawiera dla nich dane. Dla `zapal`, `aura_swietej_swiatlosci`, `spadajaca_gwiazda`, `wlocznia_niebios` i `zenit` zaimportowano tylko komponentowe tabele rang do `DamagePercentComponentRankTable`. Komponenty nie są sumowane, nie są spłaszczane do jednej wartości procentowej i nie wpływają na `baseDamagePercentAtRank1`, `baseDamagePercentAtTreeMaxRank`, sortowanie po bazowych procentach ani runtime DPS. `spadajaca_gwiazda` ma zaimportowany `LANDING_DAMAGE`; `JUMP_DAMAGE` nie został wpisany, bo lokalny JSON nie podaje go jednoznacznie dla wszystkich rang 1..15. Skille `NON_DAMAGE` i `NEEDS_MANUAL_REVIEW` nie dostały prostych ani komponentowych tabel rang.

Po imporcie prostych i komponentowych tabel rang obowiązuje osobny audyt obecności obrażeń w `docs/paladin/source-md/paladin_damage_presence_audit.md`. Audyt sprawdza, czy każdy z 24 skilli ma obrażenia w bazowej umiejętności, prostej tabeli rang, komponentowej tabeli rang albo wyłącznie przez ulepszenia z `grupa_1`, `grupa_2` lub `grupa_3`. Wynik audytu używa klasyfikacji: `HAS_BASE_DAMAGE` dla skilli z prostą bazową tabelą obrażeń, `HAS_COMPONENT_DAMAGE` dla skilli z rozdzielonymi komponentami obrażeń, `HAS_UPGRADE_DAMAGE_ONLY` dla skilli bez obrażeń bazowych, ale z ulepszeniem sugerującym obrażenia albo zmianę obrażeń, `NON_DAMAGE` dla skilli bez wykrytego źródła obrażeń oraz `NEEDS_MANUAL_REVIEW` dla wpisów niejednoznacznych. Audyt obecności obrażeń nie importuje nowych wartości liczbowych, nie zmienia `PaladinSkillTreeRegistry`, nie zmienia UI i nie odblokowuje runtime DPS.

Osobny audyt modyfikatorów obrażeń z ulepszeń znajduje się w `docs/paladin/source-md/paladin_upgrade_damage_modifier_audit.md`. Model `UpgradeDamageModifier` opisuje pojedyncze ulepszenie przez grupę, nazwę, typ modyfikatora, wartość źródłową, rodzaj wartości, warunek, informację o nowym komponencie, komponent docelowy, skalowanie rangą oraz decyzje `safeForRankingDisplay` i `safeForRuntimeDps`. Typy `UpgradeDamageModifierType` i `UpgradeDamageValueKind` rozdzielają bazowy damage skilla, komponentowy damage skilla, modyfikator damage z ulepszenia oraz pośrednie efekty typu status, szybkość użycia, cooldown, koszt, zasób, utility i defensywa. Modyfikatory ulepszeń są danymi źródłowymi do prezentacji i audytu: nie są sumowane, nie zmieniają bazowych tabel, nie zmieniają sortowania, nie odblokowują runtime DPS i `DamageEngine` nadal ich nie używa.

Nie są wymagane pliki `docs/paladin/source-md/README.md` ani `docs/paladin/source-md/SHASUMS.txt`. Nawigacja źródeł Paladyna może być prowadzona przez `docs/paladin/README.md` oraz ten główny README.

Rejestr zawiera 24 wpisy umiejętności w drzewie. Dla każdego z nich modelowana jest relacja:
- umiejętność bazowa w grupie,
- `grupa_1`, `grupa_2`, `grupa_3`,
- pojedyncze ulepszenia w grupie,
- status ulepszenia i `sourceNote`.

To nie jest jeszcze pełny DPS runtime. Rejestr, Markdown i JSON mogą opisywać umiejętność, jej modyfikatory oraz wartości damage/rank, ale dopóki mechanika single target nie jest zweryfikowana i zaimplementowana w `DamageEngine`, nie wolno liczyć dla niej `damagePerUse`, `effectiveCycleSeconds` ani `theoreticalDps`.

Poprawka Mocy Specjalnych: `Forteca` jest wpisem umiejętności w drzewie z PDF `moce_specjalne_diablo4.pdf`, ma `skillGroup=moce_specjalne` i opisowe tagi z PDF `Specjalne`, `Defensywa`, `Moloch`. Nie jest oznaczana jako kategoria `Główne` / `Core`. `Cierniowa Reduta` jest ulepszeniem `Fortecy` w `grupa_3`, a nie osobną umiejętnością bazową w grupie.

`Furia Niebios` w Mocy Specjalnych ma układ grup `2 / 2 / 3`: `grupa_1` zawiera `Czas Działania` i `Spowolnienie`, `grupa_2` zawiera `Osąd` i `Premia do Obrażeń`, a `grupa_3` zawiera `Ostateczna Sprawiedliwość`, `Krok w Światłości` i `Potrojenie`.

Testy regresyjne Mocy Specjalnych zabezpieczają dokładny układ `Zenitu`: `grupa_1` zawiera `Szansa na Trafienie Krytyczne` i `Osłabienie`, `grupa_2` zawiera `Nieustępliwość` i `Osłabienie`, a `grupa_3` zawiera `Empirejska Klinga`, `Rozdarcie` i `Homilia Stali`.

`Skazanie` ma opis obrażeń w PDF `diablo4_paladyn_sprawiedliwosc_umiejetnosci.pdf`, więc w rejestrze ma typ `DAMAGE` i status `NEEDS_VERIFICATION`. DPS Skazania nie jest liczony.

Różnica kontraktowa:
- `skill exists in tree` oznacza, że umiejętność istnieje w rejestrze PDF i może być pokazywana w analizie,
- `skill can be calculated by DPS engine` oznacza, że istnieje bezpieczny, zaimplementowany model obrażeń w `DamageEngine`.

Statusy rejestru:
- `SUPPORTED` - skill ma bezpieczny model runtime i może wpływać na DPS,
- `NEEDS_VERIFICATION` - skill albo mechanika istnieje w PDF, ale wymaga empirycznej weryfikacji przed wpływem na DPS,
- `UNSUPPORTED` - skill istnieje w PDF, ale nie ma jeszcze modelu runtime ani wystarczająco pewnego odwzorowania do kalkulacji,
- `NON_DAMAGE` - skill jest znany jako niezadający obrażeń i nie trafia do rankingu obrażeń jako osobne źródło DPS.

Obecnie żaden nowy skill z pełnego rejestru PDF nie jest jeszcze `SUPPORTED`, ponieważ nie został zaimplementowany bezpieczny model DPS na podstawie zweryfikowanych mechanik. Nie wolno uzupełniać `damagePerUse`, cooldownów ani DPS na podstawie intuicji albo niepełnego tooltipa.

Stary `PaladinSkillDefs` z `Brandish`, `Holy Bolt`, `Clash` i `Advance` został zdegradowany do `legacy/test-only`. Pozostaje w repo, ponieważ istniejące testy `DamageEngine`, manual simulation i search nadal regresyjnie chronią stary foundation, ale te skille nie są już domyślnym źródłem danych rankingu Paladyna i nie należą do rejestru wpisów umiejętności w drzewie Paladyna.

Warstwa `krys.verification` dodaje `Verification Matrix` dla mechanik z pełnego drzewa Paladyna, które wymagają osobnej weryfikacji przed użyciem w kalkulacjach. Wpisy `requiresVerification` są metadanymi procesu i nie mogą wpływać na DPS; próba ich użycia ma zostać pominięta albo zablokowana zgodnie z `default engine behavior`.

Warstwa `krys.ranking` dodaje aplikacyjny ranking obrażeń nad rejestrem drzewa wybranej klasy. `PlayableClass`, `CharacterSkillTreeRegistry` i `SkillTreeRegistryProvider` są cienką warstwą wyboru klasy; obecnie jedyną obsługiwaną klasą jest `paladin`, a jej provider używa `PaladinSkillTreeRegistry`. Nowe klasy mają być dodawane przez kolejny provider / rejestr klasy, nie przez nowe endpointy ani osobne zakładki per klasa.

Ranking domyślnie działa w trybie `SINGLE_TARGET`, zwraca metadane źródłowe oraz status weryfikacji danych. Ranking może też pokazywać bazowe procenty obrażeń z opisu, Markdown, JSON albo PDF, ale te wartości są wyłącznie źródłowym opisem siły umiejętności i nie są runtime DPS.

Pola bazowych procentów obrażeń:
- `baseDamagePercentAtRank1` - procent obrażeń jawnie podany w opisie, Markdown, JSON albo PDF dla rangi 1; R1 oznacza minimalny wyklikany poziom / rangę umiejętności w drzewie,
- `baseDamagePercentAtTreeMaxRank` - procent obrażeń jawnie podany w opisie, Markdown, JSON albo PDF dla maksymalnej rangi możliwej do wyklikania w drzewie bez bonusów z przedmiotów.

`treeMaxRank` obecnie oznacza `15`, czyli maksymalną rangę możliwą do wyklikania w drzewie bez bonusów z itemów. Nie jest to absolutne maksimum umiejętności w całym buildzie. Bonusy z itemów, affixów, aspektów albo innych mechanik mogą w przyszłości zwiększać rzeczywistą rangę używaną przez build ponad `treeMaxRank`. Pojęcie `effectiveRank` po itemach jest zostawione na przyszłość i nie jest teraz implementowane.

Wartości `baseDamagePercentAtRank1` i `baseDamagePercentAtTreeMaxRank` są wartościami pochodnymi z `DamagePercentRankTable`, jeśli dany skill ma tabelę rang. Jeżeli tabela nie istnieje, gettery zachowują kompatybilny fallback do ręcznie wpisanych wartości źródłowych. Te wartości nie są normalizowane względem najlepszej umiejętności, nie są DPS, nie odblokowują runtime i nie mogą być zgadywane. Jeżeli źródło nie podaje jawnie wartości dla R1 albo maksymalnej rangi drzewa, odpowiednie pole pozostaje `null`, a SSR renderuje `brak danych`. Poziomy pośrednie nie są wyliczane przez interpolację; albo istnieją jawnie w tabeli, albo pozostają brakiem danych.

Aktualnie rejestr uzupełnia proste tabele rang tylko dla wartości jednoznacznie przeniesionych z lokalnego JSON-a dla skilli `SIMPLE_SINGLE_COMPONENT`. `Błogosławiony Młot` ma `baseDamagePercentAtRank1 = 115` i `baseDamagePercentAtTreeMaxRank = 293`, bo są to wartości pochodne z tabeli rang `1..15`. Skille `MULTI_COMPONENT` mogą mieć komponentowe tabele źródłowe, ale ich główne `baseDamagePercentAtRank1` i `baseDamagePercentAtTreeMaxRank` pozostają `null`, żeby nie spłaszczać ani nie sumować komponentów. Wartości wielohitowe, tickowe i warunkowe nie są sumowane ani przepisywane jako bazowa siła umiejętności bez osobnej weryfikacji interpretacji.

Lokalny JSON jest pomocniczym źródłem tabel rang, ale implementacja realnego runtime wymaga osobnych testów mechaniki single target. Prosta ani komponentowa tabela rang nie zmienia statusu `NEEDS_VERIFICATION`, nie wypełnia `damagePerUse`, `theoreticalDps` ani `singleTargetDps`, nie odblokowuje runtime DPS i nie jest jeszcze konsumowana przez `DamageEngine`.

Pola `damagePerUse`, `effectiveCycleSeconds` i `theoreticalDps` pozostają puste dla skilli `NEEDS_VERIFICATION` albo `UNSUPPORTED`. Runtime metryki `DAMAGE_PER_USE`, `THEORETICAL_DPS` i `SINGLE_TARGET_DPS` pozostają w domenie na potrzeby przyszłego debug/runtime widoku, ale nie są pokazywane w głównej tabeli `/ranking-obrazen` i nie są dostępne jako filtr użytkownika.

Ekran SSR `/ranking-obrazen` jest na obecnym etapie widocznym dla użytkownika porównaniem bazowej siły umiejętności i opisowego wpływu ulepszeń, a nie ekranem realnego DPS. Parametr `character=paladin` wybiera rejestr Paladyna; gdy parametr `character` nie jest podany, a Paladyn jest jedyną obsługiwaną klasą, ekran domyślnie wybiera `paladin`. Dla `character=paladin` ekran pokazuje wszystkie 24 wpisy z `PaladinSkillTreeRegistry`, a nie tylko wpisy policzalne.

Główna tabela używa szerokiego wariantu layoutu i zawiera tylko kolumny decyzyjne: `skillName`, `Kategorie z gry`, `Obrażenia % R1`, `Obrażenia % max drzewo`, `Koszt Wiary`, `Generowanie Wiary`, `Lucky Hit`, `Dmg multiplier`, `Dmg bonus`, `Extra hit / component`, `Damage over time`, `Status / debuff`, `Defense / utility` i `Manual review`. Kolumny `Grupa drzewa`, `tags`, `type` oraz `Speed / cooldown` nie są już renderowane w głównej tabeli.

Ranking rozdziela trzy pojęcia. `Grupa drzewa` / `skillGroup` oznacza techniczne miejsce skilla w drzewie, np. `basic`, `core`, `aura`, `odwaga`, `moce_specjalne`; pozostaje filtrem i atrybutem `data-skill-group`, ale nie jest osobną kolumną głównej tabeli. `Kategorie z gry` / `sourceCategories` / `skillCategories` są wyłącznie źródłowymi kategoriami z gry, PDF albo lokalnego Markdowna; dla `Wymach` lokalny Markdown `paladin_basic_skill_registry_final.md` potwierdza `Podstawowe, Adept`. Mechaniczne `tags` z enumu `SkillTag` są wewnętrznymi facetami porównywarki, np. `DAMAGE`, `HOLY_DAMAGE`, `FAITH_GENERATION`, `CAST_SPEED`, `EXPOSED`, `VULNERABLE` albo `MULTI_HIT`; mogą pozostać w modelu i atrybucie `data-mechanic-tags`, ale nie są renderowane jako kolumna `tags` ani jako „tagi z gry”.

Skill może mieć więcej niż jedną kategorię źródłową. Potwierdzone przykłady z lokalnej paczki Basic: `Wymach` ma `Podstawowe, Adept`, `Święty Pocisk` ma `Podstawowe, Sędzia`, `Starcie` ma `Podstawowe, Moloch`, a `Natarcie` ma `Podstawowe, Mobilność, Zeloty`. Kategorie nadawane dopiero przez ulepszenia, np. wariant „staje się umiejętnością Adepta”, nie są bazowymi kategoriami skilla i pozostają informacją ulepszenia. W lokalnej dokumentacji Paladyna obowiązuje korekta terminologiczna `Fanatyk/Fanatyka` na `Zeloty`; dotyczy to opisów kategorii i modyfikatorów z drzewa Paladyna, bez zmiany PDF-ów ani JSON-a rang.

Kolumny R1/treeMax renderują jedną wartość dla `SIMPLE_SINGLE_COMPONENT`, krótką listę komponentów dla `MULTI_COMPONENT`, `nie dotyczy` dla wpisów nieobrażeniowych oraz `wymaga weryfikacji` dla wpisów bez bezpiecznej kompletnej tabeli. Kolumny `grupa_1`, `grupa_2` i `grupa_3` pozostają w danych źródłowych ulepszeń, ale nie są już trzema szerokimi kolumnami głównej tabeli.

Tabela jest sortowalna kliknięciem w nagłówki kolumn. Widok używa parametrów `sort=<columnKey>` i `direction=asc|desc`; aktualnie sortowany nagłówek ma `aria-sort` i tylko ten nagłówek dostaje atrybut. Obsługiwane klucze sortowania dla głównej tabeli to `skillName`, `sourceCategories`, `baseDamageRank1`, `baseDamageTreeMax`, `faithCost`, `faithGeneratedBase`, `faithGeneratedMaxKnown`, `maxDamageMultiplierPercent`, `maxDamageBonusPercent`, `maxExtraHitOrComponentPercent`, `maxDamageOverTimePercent`, `hasStatusDamageEnabler`, `hasDefenseOrUtility` i `hasManualReviewUpgrade`. Backend nadal toleruje historyczne parametry `type`, `tag`, `skillGroup` i `hasCooldownOrCastSpeed`, ale UI nie renderuje już filtrów `Typ umiejętności`, `tag` ani `Speed / cooldown`. Sortowanie zachowuje aktywny filtr tekstowy `q`, tak samo jak pozostałe filtry. Sortowanie `sourceCategories` używa tekstu wyświetlanego dla całego zbioru kategorii, bez sprowadzania skilla do jednej kategorii. `faithCost` sortuje po potwierdzonym koszcie użycia, `faithGeneratedBase` po bazowej generacji, a `faithGeneratedMaxKnown` po bazowej generacji powiększonej o potwierdzony dodatek z ulepszenia; brak danych trafia za wartościami liczbowymi. Numeryczne sort keys dla modyfikatorów używają największej wartości procentowej w danej kategorii, bez sumowania wielu wpisów. Domyślne sortowanie to `baseDamageTreeMax desc`; wpisy bez prostej wartości liczbowej trafiają za wpisami liczbowymi, a remisy są rozstrzygane przez `skillName asc`. Dotychczasowy filtr `Metryka rankingu` został zastąpiony sortowaniem po nagłówkach.

Tagi mechaniczne są opisowymi etykietami z enumu `SkillTag`, wyprowadzanymi z lokalnych danych skilla, typu, grupy i modyfikatorów ulepszeń. Każdy skill Paladyna ma niepusty zestaw tagów mechanicznych, np. `DAMAGE`, `BASIC`, `HOLY_DAMAGE`, `FAITH_GENERATION`, `CAST_SPEED`, `EXPOSED`, `VULNERABLE`, `SHIELD` albo `AURA`, jeżeli lokalne dane to potwierdzają. Widok udostępnia filtr `sourceCategory` po źródłowych `skillCategories` oraz filtry faceted `hasDirectUpgradeDamage`, `hasNewDamageComponent`, `hasStatusDamageEnabler`, `hasFaithCost`, `hasResourceGeneration`, `hasDefenseOrUtility` i `hasManualReviewUpgrade` z wartościami `ALL`, `YES`, `NO`. Mechaniczne tagi nie są pokazywane w UI pod nazwą `tagi`.

Kolumny `Koszt Wiary`, `Generowanie Wiary` i `Lucky Hit` są rozdzielone. `Koszt Wiary` pokazuje potwierdzony koszt użycia, np. `Błogosławiony Młot` ma `10`; jeżeli lokalne źródła nie podają kosztu, komórka pokazuje `-`. `Generowanie Wiary` pokazuje bazową generację, a nazwę modyfikatora pokazuje bez jego wartości liczbowej, np. `Wymach` ma `14; Generowanie Wiary`, `Święty Pocisk` ma `16; Generowanie Wiary`, `Starcie` ma `20; Generowanie Wiary`, a `Natarcie` ma `18`. Jeżeli dodatek pochodzi z ulepszenia, jego wartość i pełny opis pozostają w tooltipie oraz `data-search-text`, np. tooltip Starcia zawiera `Starcie generuje dodatkowe 10 pkt. wiary`. `Lucky Hit` jest opisową wartością źródłową z lokalnych materiałów i zaakceptowanych korekt widoku rankingu, np. Basic pokazuje `Wymach = 26%`, `Święty Pocisk = 57%`, `Starcie = 63%`, `Natarcie = 18%`; brak danych renderuje `-`, nigdy `0%`. Jeżeli lokalny source-md nadal zawiera starszą wartość Lucky Hit Starcia, `63%` pozostaje zaakceptowaną korektą prezentacyjną rankingu bez zmiany source-md. Te wartości są danymi opisowymi UI i sortowania/filtrowania, nie są używane przez `DamageEngine`.

Kolumny faceted renderują skrócone, opisowe dane z `UpgradeDamageModifier`, jeżeli `safeForRankingDisplay = YES`, albo wpisy wymagające ręcznej weryfikacji w `Manual review`. Główna treść list modyfikatorów pokazuje tylko nazwy modyfikatorów albo cech, bez wartości, warunków i dopisków opisowych. Każda widoczna pozycja modyfikatora ma tooltip użytkowy z prefiksem źródła cechy: `Umiejętność:`, `Modyfikator:` albo `Manual review:`. Tooltipy biorą pełną treść z lokalnych opisów modelu i dokumentacji source-md, zawierają szczegóły wartości i pozostają także w `aria-label`; nie są kontraktem runtime DPS i nie odblokowują mechanik w `DamageEngine`. `Dmg multiplier` oznacza mnożnik obrażeń, np. `20%[X]`; `Dmg bonus` oznacza addytywny bonus obrażeń, np. `20%[+]`; `Extra hit / component` oznacza osobne źródło obrażeń, dodatkowy hit, powrót pocisku albo dodatkowy łuk; `Damage over time` jest zarezerwowane dla DOT/tick/periodic damage. `Status / debuff`, `Defense / utility` i `Manual review` również pokazują w głównej treści tylko nazwy wpisów, a wartości typu `15%[X]`, `25%[X]`, `30%[+]`, `20%[X]`, warunki, blok, kryt, Odwet i ciernie trafiają do tooltipów. Brak danych w komórkach jest renderowany jako `-`. `Speed / cooldown` nie jest już kolumną głównej tabeli, chociaż dane tego typu mogą pozostać w modelu audytowym.

Wartości liczbowe w komórkach modyfikatorów nie są głównym tekstem list. Przykładowo Wymach pokazuje w `Extra hit / component` nazwy `Powracająca Światłość`, `Miecz Mistrzostwa` i `Krzyżowe Uderzenie`, a wartości `52%`, `128%` i `120%` pozostają w tooltipach, `aria-label` i `data-search-text`. Komponent `Potyczka` pokazuje nazwę `Potyczka`, a wartość `155%` pozostaje szczegółem tooltipa. Puste kategorie pokazują neutralny placeholder `-` z opisem w `title`/`aria-label`; tekst `brak wpływu na obrażenia` nie jest pokazywany w głównej tabeli. Listy komponentów i modyfikatorów są prezentacyjne: nie są sumowane, nie tworzą wartości `razem`/`total`, nie zmieniają prostych tabel `baseDamagePercentRanks`, nie wpływają na runtime DPS i nie odblokowują `DamageEngine`.

Starcie ma w rankingu opisowo pokazane potwierdzone dane z lokalnego Markdown Basic bez liczenia DPS: `Generowanie Wiary` pokazuje `20; Generowanie Wiary`; `Defense / utility` pokazuje nazwy `Marsz Krzyżowca`, `Animusz` i `Skuteczność Marszu Krzyżowca`; `Dmg multiplier` pokazuje nazwy `Zwiększenie Obrażeń` i `Brać Ich`; `Extra hit / component` pokazuje `Potyczka`; `Manual review` pokazuje `Kara`. Wartości i warunki tych wpisów, w tym `+10`, `15%[X]`, `2 kumulacje`, `25%[X]`, `20%[X]`, `8%[X]`, `155%`, informacja o przejściu Potyczki w umiejętność `Zeloty`, Odwet i ciernie, pozostają w tooltipach oraz `data-search-text`. Te wpisy nie zmieniają `baseDamagePercentAtRank1 = 115`, `baseDamagePercentAtTreeMaxRank = 293`, nie są sumowane ze sobą ani z bazą i nie są konsumowane przez runtime.

Techniczny `skillId` nie jest osobną kolumną domyślnej tabeli, ale pozostaje w danych i w atrybucie `data-skill-id` wiersza. `verificationStatus` również nie jest osobną kolumną głównej tabeli; status pozostaje w modelach, filtrze `Status weryfikacji`, atrybucie `data-verification-status`, `title`/`aria-label` oraz klasach wiersza typu `verification-needs-verification`, `verification-non-damage`, `verification-unsupported` i `verification-supported`. Kolor nie jest jedynym nośnikiem informacji o statusie, bo status jest dostępny tekstowo w atrybutach HTML i krótkiej legendzie nad tabelą. Kolumny runtime `damagePerUse`, `theoreticalDps` i `singleTargetDps` nie są renderowane w domyślnej tabeli, dopóki runtime DPS nowych umiejętności pozostaje zablokowany. Pola diagnostyczne `reason / notes` i `sourcePdf` również pozostają poza domyślną tabelą.

Dane runtime, diagnostyczne i komponenty obrażeń pozostają w modelach oraz testach jako materiał pomocniczy dla przyszłego widoku szczegółowego skilla, panelu debug albo rozwijanego wiersza. Opisowy wpływ ulepszeń jest klasyfikowany bez liczenia DPS i bez wpisywania wartości liczbowych, jeśli lokalne źródła nie podają ich jednoznacznie. Wpływ ulepszeń nie jest sumowany z bazowym procentem skilla, nie zmienia `baseDamagePercentAtRank1`, nie zmienia `baseDamagePercentAtTreeMaxRank`, nie wpływa na sortowanie po bazowych procentach i nie odblokowuje `DamageEngine`.

Endpoint `/ranking-obrazen-paladyna` pozostaje wyłącznie aliasem kompatybilności wstecznej do tego samego widoku Paladyna. Nie jest docelowym wzorcem dla kolejnych klas i nie jest osobnym widocznym modułem w głównej nawigacji.

Filtry ekranu:
- postać (`character`, obecnie `paladin`),
- szybki filtr tekstowy `Szukaj` (`q`),
- grupa drzewa (`skillGroup`),
- kategoria z gry (`sourceCategory`),
- status weryfikacji (`verificationStatus`),
- filtry faceted opisujące wpływ ulepszeń (`hasDirectUpgradeDamage`, `hasNewDamageComponent`, `hasStatusDamageEnabler`, `hasFaithCost`, `hasResourceGeneration`, `hasDefenseOrUtility`, `hasManualReviewUpgrade`).

Pole `Szukaj` / `q` filtruje nazwę umiejętności, `skillId`, grupę drzewa, kategorie z gry, wartości Wiary, Lucky Hit, wartości obrażeń, nazwy modyfikatorów oraz treści tooltipów/opisów wiersza. Backend stosuje `q` jako odtwarzalny parametr URL, np. `/ranking-obrazen?character=paladin&skillGroup=basic&q=star` renderuje pasujące wiersze po stronie serwera. Search może więc znaleźć dane, które nie są widoczne jako główny tekst komórki, np. `120`, `155`, `Odwet`, `cierni` albo `blok`, jeżeli występują w tooltipie lub `data-search-text`. Frontend dodaje progressive enhancement: wpisywanie w `type="search"` filtruje bieżącą tabelę bez klikania `Filtruj`, aktualizuje licznik `Widoczne po filtrach` w regionie `aria-live` i pokazuje komunikat `Brak umiejętności pasujących do filtrów.`, gdy żaden wiersz nie pasuje. Pole `Szukaj` jest częścią standardowej siatki filtrów, używa tych samych klas kontrolek co selecty i jest objęte wspólnym stylem `input[type="search"]`; ma pomoc dostępną przez `aria-describedby` w elemencie ukrytym wizualnie, żeby nie rozpychać układu. Wyszukiwanie ignoruje wielkość liter i polskie znaki, więc `swiety`, `mlot` i `blogoslawiony` trafiają odpowiednio w nazwy z `Ś`, `ł` i znakami diakrytycznymi. Przycisk `Filtruj` wysyła `q` razem z pozostałymi filtrami, linki sortowania zachowują `q`, a `Wyczyść` wraca do `/ranking-obrazen` bez parametru `q`. To jest wyłącznie zmiana UI/accessibility/filtering i metadanych opisowych rankingu: nie wpływa na runtime DPS, nie zmienia `DamageEngine`, nie sumuje komponentów i nie odblokowuje nowych mechanik runtime.

Ograniczenia rankingu:
- ranking używa opisowego rejestru drzewa Paladyna, a nie legacy `PaladinSkillDefs`,
- bazowe procenty obrażeń pochodzą wyłącznie z jawnych danych źródłowych opisu, Markdown, JSON albo PDF; brak jawnej wartości oznacza `null` i `brak danych` w SSR,
- umiejętności niepoliczalne są widoczne jako `NEEDS_VERIFICATION`, `UNSUPPORTED` albo `NON_DAMAGE`,
- node'y czysto użytkowe oznaczone jako `NON_DAMAGE` nie są traktowane jako damage skill; opisowy ekran nadal pokazuje je jawnie z pustymi wartościami DPS,
- mechaniki z `Verification Matrix` pozostają `NEEDS_VERIFICATION` i nie są oznaczane jako `SUPPORTED`,
- obecność plików MD albo JSON nie odblokowuje runtime DPS i nie implementuje `effectiveRank`,
- efekty wielocelowe nie mogą zwiększać wyniku single target bez jawnego, zweryfikowanego modelu.

Obecnie ekran jest rankingiem opisowym z blokadą niezweryfikowanych mechanik. Kolejne etapy mogą odblokowywać konkretne umiejętności dopiero po osobnej weryfikacji single target i dopiero wtedy wolno wypełniać `damagePerUse`, `theoreticalDps` oraz `singleTargetDps` dla tych wpisów.

### 4.4. Model efektów runtime
Aktualny foundation wspiera następujące typy efektów runtime:
- `REPLACE_BASE_DAMAGE`,
- `DAMAGE`,
- `APPLY_STATUS`,
- `APPLY_DELAYED_HIT`,
- `SET_COOLDOWN`

Efekt może:
- podmienić bazowy procent obrażeń głównego komponentu,
- dodać osobny komponent obrażeń,
- nałożyć status na cel,
- zaplanować delayed hit z określonym trigger time,
- ustawić efektywny cooldown skilla po castcie.

Pozostałe typy efektów opisane w szerszej dokumentacji projektu nie są jeszcze częścią aktualnego foundation kodowego.

### 4.5. Model importu itemu ze screena
Aktualny foundation importu obrazu obejmuje wyłącznie pojedynczy item i jawnie ręczne potwierdzenie użytkownika.

Kontrakt domenowy importu itemu:
- `ItemImageImportRequest` reprezentuje upload pojedynczego obrazu itemu,
- `FullItemRead` reprezentuje pełniejszy odczyt widocznego itemu z OCR: nazwę, typ / slot, rzadkość, moc przedmiotu, base stat, linie implicit / bazowe, właściwe affixy, aspekt / moc, gniazda i inne zachowane linie,
- `ItemImportDetails` reprezentuje strukturalnie potwierdzane dane itemu: `itemName`, `itemType`, `itemRarity`, `isAncient`, `equipmentSlot`, `itemPower`, osobne pola broni `weaponDps`, `weaponDamageMin`, `weaponDamageMax`, `averageWeaponDamage`, `attacksPerSecond` oraz opisowy `uniqueEffectText`,
- parser i UI importu nie mogą wpisywać fałszywego fallbacku `1` dla `itemPower` ani bazowych obrażeń; gdy odczyt nie jest pewny, pole zostaje puste albo pokazuje `Brak pewnego odczytu`,
- `ItemImageImportCandidateParseResult` reprezentuje wstępny odczyt OCR wraz z metadanymi obrazu, pełnym odczytem itemu, poziomem pewności i uwagami per pole foundation po scaleniu kilku wariantów OCR,
- `ItemImportDraft` jest strukturalnym draftem importu powstałym z OCR; OCR może w nim zapisać sugestie, ale nie tworzy finalnego itemu,
- `AspectDefinition` i `AspectRegistry` reprezentują znane aspekty przez stabilne `id`, nazwę prezentacyjną, typ aspektu `LEGENDARY` albo `UNIQUE`, status runtime, semantyczny opis efektu aspektu, dozwolone sloty itemu, opcjonalne klasy i tagi,
- bieżący `AspectRegistry` jest zalążkowym katalogiem znanych aspektów potrzebnym do stabilizacji importu; nie udaje pełnej produkcyjnej bazy wszystkich aspektów Diablo 4 i zawiera wyłącznie świadomie dodane definicje, obecnie `inner-calm` / `Aspekt Wewnętrznego Spokoju` dla zgodnych slotów ręki dodatkowej oraz `verathiel_shard` / `Odłamek Verathiela` jako aspekt unikatowy `UNIQUE` dla `MAIN_HAND`,
- `ApplicationAspectRegistry` jest wspólnym punktem dostępu do tego samego zalążkowego katalogu dla factory formularza, mappera, importu SSR i biblioteki,
- opis efektu w `AspectRegistry` jest ogólnym opisem znanego aspektu, a nie raw OCR z konkretnego itemu; dla `inner-calm` opis mówi o zwiększaniu zadawanych obrażeń podczas stania w bezruchu i trzykrotnie większej premii po co najmniej 3 sekundach bez ruchu,
- nie zgadujemy wartości rolli liczbowych w registry; procenty i zakresy rolla mogą pochodzić wyłącznie z pomocniczego OCR effect konkretnego itemu albo z przyszłej ręcznej korekty itemu,
- aspekt na finalnym itemie jest zapisywany jako `selectedAspectId`; surowy tekst OCR aspektu może istnieć w `FullItemRead` tylko jako zachowany odczyt diagnostyczno-prezentacyjny,
- unikatowy efekt itemu jest zapisywany osobno jako `uniqueEffectText`, a rozpoznany aspekt unikatowy może być równolegle finalnym `selectedAspectId` z katalogu aspektów; nie jest zwykłym affixem ani zwykłym aspektem legendarnym i w tym etapie pozostaje opisowy oraz nieaktywny w runtime DPS,
- OCR może zaproponować wyłącznie `ocrSuggestedAspectId` i poziom pewności sugestii, a nie dowolny finalny tekst aspektu,
- wybrany aspekt musi istnieć w `AspectRegistry` i jego `allowedItemSlots` musi zawierać slot importowanego itemu; aspekt spoza slotu jest błędem walidacji i nie może zostać zapisany,
- jeśli OCR wykryje tekst aspektu, ale katalog nie zna dopasowania, `ocrSuggestedAspectId` i `selectedAspectId` pozostają puste, UI pokazuje komunikat o braku dopasowania w katalogu, a surowy tekst może zostać zachowany wyłącznie w `FullItemRead` jako diagnostyka; dla Odłamka Verathiela dopasowanie do `verathiel_shard` usuwa ten komunikat i pokazuje `Aspekt unikatowy: Odłamek Verathiela`,
- `ImportedItemAffix` reprezentuje pojedynczy edytowalny affix itemu jako strukturę: typ z katalogu, nazwa / etykieta, wartość, jednostka, `greaterAffix`, kolejność prezentacji, surowa linia OCR i źródło `OCR`, `MANUAL` albo `CORRECTED`,
- `ImportedItemAffixType` jest katalogiem znanych typów affixów używanym wspólnie przez UI ręcznej walidacji i projekcję do runtime,
- `greaterAffix` jest trwałym polem boolean modelu affixu, niezależnym od surowej linii OCR i od prezentacyjnej gwiazdki w UI,
- OCR oznacza `greaterAffix=true`, gdy rozpoznany edytowalny affix zaczyna się od markera `*`, `★`, `⭐` albo `✦`,
- parser ma zachować bracketowy zakres rolla, jeżeli OCR go odczytał, także gdy OCR doda techniczny znak przed nawiasem, np. `+[107 - 121]`,
- OCR nie oznacza `greaterAffix=true` tylko dlatego, że rozpoznany edytowalny affix nie ma bracketowego zakresu rolla; brak zakresu pozostaje polem do ręcznej weryfikacji,
- parser nie może oznaczać affixu jako `Greater Affix` tylko dlatego, że własna normalizacja zgubiła zakres rolla obecny w raw OCR albo w sąsiednim wariancie OCR,
- `ItemImportEditableForm` reprezentuje ręcznie edytowalny formularz potwierdzenia, którego głównym modelem korekty jest lista affixów, a nie sztywna tabela kilku pól foundation,
- `ValidatedImportedItem` reprezentuje item zatwierdzony po walidacji razem z pełnym rekordem affixów po edycji użytkownika, finalnym `selectedAspectId` i strukturalnymi `ItemImportDetails`,
- `ValidatedImportedItemToItemMapper` mapuje zatwierdzony item do aktualnego modelu `Item`,
- `ImportedItemCurrentBuildContributionMapper` mapuje zatwierdzony item do agregowanych pól aktualnego modelu current build,
- `ItemLibraryService` zapisuje zatwierdzony item razem z `FullItemRead`, `ItemImportDetails` i listą affixów po ręcznej edycji do biblioteki,
- mapping foundation pozostaje osobną warstwą: z pełnego odczytu, listy affixów i ręcznie potwierdzonego slotu do aktualnie wspieranego podzbioru runtime.

Semantyczny podział linii itemu:
- dane podstawowe itemu obejmują nazwę, typ itemu, slot ekwipunku, rzadkość i moc przedmiotu; nie są affixami,
- base stat to bazowa wartość itemu, np. pancerz albo bazowe obrażenia broni; base stat nie trafia do listy edytowalnych affixów,
- dla broni import rozdziela `DPS broni`, minimalne i maksymalne obrażenia za trafienie, `Średnie obrażenia trafienia` oraz `Ataki na sekundę`; DPS nie jest średnią obrażeń trafienia,
- implicit / linie bazowe to właściwości bazowe typu itemu, np. redukcja blokowanych obrażeń, szansa bloku albo obrażenia od broni w głównej ręce na tarczy; są prezentowane osobno od affixów,
- affix to edytowalna właściwość z katalogu `AffixRegistry`, mapowana technicznie do `ImportedItemAffixType`, np. siła, ciernie, szansa na szczęśliwy traf albo redukcja czasu odnowienia; tylko ta lista zasila ręczną korektę affixów,
- aspekt / efekt legendarny albo unikatowy jest mapowany do `selectedAspectId` z `AspectRegistry`, a surowe linie OCR aspektu mogą pozostać tylko kontekstem diagnostyczno-prezentacyjnym,
- unikatowy efekt / aspekt unikatowego itemu może zostać zapisany jako osobne pole opisowe `uniqueEffectText` oraz jako dopasowany aspekt katalogowy typu `UNIQUE`; nie jest dodawany do listy affixów i nie odblokowuje runtime DPS,
- `selectedAspectId` jest finalnym źródłem prawdy aspektu zapisanego itemu; raw OCR efektu aspektu nigdy nie zastępuje finalnego wyboru z `AspectRegistry`,
- raw OCR efektu aspektu jest prezentowany tylko pomocniczo: pełny bezpieczny odczyt jest pokazywany jako `Odczyt OCR efektu`, a samotny ogon efektu bez pierwszej części jest zastępowany komunikatem `Odczyt efektu OCR niepełny / wymaga ręcznej weryfikacji.`,
- socket / gniazdo jest osobną sekcją prezentacyjną i nie może przejmować tekstu aspektu ani affixów.

Kontrakt prezentacji pełnego odczytu itemu:
- główny widok pełnego odczytu nie jest technicznym dumpem OCR, tylko rekordem produktu możliwie 1:1 względem screena,
- nagłówek itemu pokazuje osobno nazwę, typ, rzadkość, `Ancient`, slot, moc przedmiotu oraz dane broni, jeżeli item je posiada,
- pełny zapis itemu pokazuje osobno i w czytelnej kolejności linie bazowe / implicit, aspekt / efekt legendarny, dodatkowe / sezonowe linie oraz socket / gniazdo; affixy nie są pokazywane w tej sekcji, bo ich źródłem prawdy jest tabela ręcznej walidacji affixów,
- klasyfikacja semantyczna aspektu / efektu legendarnego ma pierwszeństwo przed technicznym typem linii OCR, więc treść aspektu nie może trafiać do `Socket / gniazdo`,
- sekcja ręcznej walidacji affixów jest głównym modelem korekty itemu i pozwala zmienić typ affixu, poprawić jego wartość, usunąć błędny affix albo dodać brakujący affix z katalogu znanych typów,
- po wejściu w ręczną korektę finalny zapis itemu powstaje wyłącznie z danych widocznych i zatwierdzonych w formularzu; OCR jest tylko źródłem wartości początkowych,
- ukryte pola nie mogą przywracać usuniętych ani zmienionych affixów, a lista widocznych wierszy affixów jest jedynym źródłem prawdy dla affixów finalnego itemu,
- każdy wiersz affixu pozwala ręcznie poprawić checkbox `Greater Affix`; gwiazdka jest wyłącznie prezentacją, a trwały kontrakt danych stanowi pole `greaterAffix`,
- aspekt jest wybierany z listy znanych aspektów zgodnych ze slotem itemu; sugestia OCR może zostać pokazana albo wstępnie wybrana tylko wtedy, gdy pasuje do slotu,
- sekcja aspektu w imporcie pokazuje czysty wybór `selectedAspectId` jako select oraz opis efektu z `AspectRegistry`; typ aspektu, status runtime, sugestia OCR, confidence i `Odczyt OCR efektu` pozostają danymi modelu / debug, ale nie są widocznym tekstem głównego formularza dla dopasowanego aspektu katalogowego Odłamka Verathiela,
- zmiana slotu itemu nie może zostawić niezgodnego aspektu jako cichego finalnego wyboru: frontend odświeża opcje selecta progresywnie, a backend niezależnie od JS odrzuca `selectedAspectId` spoza `allowedItemSlots`,
- OCR confidence aspektu pozostaje danymi technicznymi formularza i nie jest częścią nazwy opcji aspektu w selectcie ani głównego UI dla dopasowanego aspektu katalogowego,
- `Dodaj affix` jest progresywną akcją edycji formularza po stronie klienta: dodaje widoczny wiersz bez przeładowania strony i bez zapisu do biblioteki,
- formularz zawiera także SSR fallback `<noscript>` dla `Dodaj affix`, który wysyła `formAction=addAffix` i odświeża tylko bieżący formularz bez zapisu itemu,
- usunięcie affixu usuwa wiersz z formularza; usunięty affix nie jest wysyłany jako aktywny affix i nie może wrócić z `FullItemRead` ani z pól foundation,
- `Zatwierdź item` jest osobną końcową akcją walidacji i zapisu zatwierdzonego itemu do biblioteki,
- pełny odczyt nie usuwa widocznych informacji tylko dlatego, że obecny runtime ich jeszcze nie wykorzystuje,
- dla `Odłamek Verathiela` kontrakt importu zapisuje: `itemPower=900`, `weaponDps=1830`, zakres obrażeń za trafienie `1350-1978`, `averageWeaponDamage=1664` oraz `attacksPerSecond=1.10`; `900` jest obecnie maksymalną mocą przedmiotu według ustalenia projektu,
- dla `Odłamek Verathiela` parser obsługuje także lokalnie zaszumiony OCR nazwy, np. `ODŁFIK VERATHEL`, wyłącznie w kontekście unikatowego miecza; taki odczyt prefilluje formularz ręcznego potwierdzenia nazwą `Odłamek Verathiela` zamiast pustego pola,
- dla `Odłamek Verathiela` min/max muszą pochodzić z jawnego zakresu obrażeń za trafienie `1350-1978`; `averageWeaponDamage=1664` jest liczone wyłącznie z tego zakresu, nie z DPS i szybkości ataku,
- affix `+94 obrażeń od broni [94 - 157]` jest w tym etapie tylko affixem do ręcznej weryfikacji i nie jest sumowany z `averageWeaponDamage`; lucky hit przywracający podstawowy zasób oraz aspekt unikatowy Verathiela także nie są aktywne w runtime DPS,
- ekran importu nie pokazuje projekcji do aktualnego runtime; import itemu jest korektą i zapisem danych itemu, a nie ekranem analizy DPS,
- techniczne kandydaty OCR, tabela pewności i surowy techniczny dump OCR nie są częścią zwykłego głównego flow użytkownika.

Minimalny zakres pól foundation mapowanych do runtime:
- `slot / typ itemu`,
- `weapon damage`, jeżeli dotyczy,
- `strength`,
- `intelligence`,
- `thorns`,
- `block chance`,
- `retribution chance`.

`weapon damage` w tej sekcji jest polem legacy / foundation i nie oznacza DPS broni. Nowe dane broni z importu są zapisywane w `ItemImportDetails`; ten etap nie przekazuje `averageWeaponDamage` ani unikatowego efektu do runtime current build.
W flow import-only DPS broni nie jest przepisywany do legacy `weaponDamage`: dla Verathiela `weaponDps=1830`, `averageWeaponDamage=1664`, a legacy `weaponDamage=0`.

Minimalny zakres rozpoznawania tekstu OCR dla M13.1:
- slot / typ itemu rozpoznawany jest ostrożnie zarówno z angielskich, jak i wybranych polskich nazw typu itemu,
- aktualnie jawnie wspierane są co najmniej `tarcza`, `buty` i unikatowy miecz `Odłamek Verathiela`, a dotychczasowe foundation slotów `broń główna`, `ręka dodatkowa`, `pancerz` i `pierścień` pozostają bez zmian,
- parser obsługuje polskie separatory liczb z OCR, np. `1 830`, `1 350`, `1 978`, `2 141`, `1 831`, `2 200`, oraz przecinek dziesiętny, np. `1,10`,
- parser rozpoznaje bezpieczne frazy broni: `pkt. obrażeń na sek.`, `pkt. obrażeń za trafienie`, `ataku na sekundę`, `Moc przedmiotu`, `Starożytny unikatowy miecz` i `Umiejętności Podstawowe`,
- parser Odłamka Verathiela rozbija także skondensowany OCR jednej linii na osobne pola broni, osobne affixy i osobny aspekt unikatowy; wspierane są warianty zakresu bez nawiasów, z różnymi myślnikami i z usuniętymi polskimi znakami,
- pełny odczyt OCR zachowuje rozpoznane linie widocznego itemu nawet wtedy, gdy aktualny runtime ich jeszcze nie liczy,
- pełny odczyt rozdziela linie co najmniej na: nazwa, typ / slot, rzadkość, moc przedmiotu, base stat, implicit / linia bazowa, affix, aspekt / moc, gniazdo oraz inna linia,
- po scaleniu wariantów OCR ta sama stabilna linia itemu albo ten sam affix nie może występować wielokrotnie w `FullItemRead`,
- ta sama zduplikowana linia OCR nie może wielokrotnie zasilać edytowalnej listy affixów ani projekcji do aktualnego runtime,
- wartości bazowe itemu, takie jak moc przedmiotu, pancerz, trwałość albo wartość sprzedaży, są metadanymi odczytu i nie mogą trafiać do edytowalnych affixów,
- tekst aspektu z OCR jest mapowany wyłącznie na sugestię z `AspectRegistry`; finalny item zapisuje wybrany identyfikator aspektu, a nie dowolny tekst OCR,
- parser foundation rozpoznaje polskie frazy dla `Strength`, `Intelligence`, `Thorns` i `Block chance`,
- parser może rozpoznać `Retribution chance` tylko wtedy, gdy OCR zawiera jednoznaczną frazę `retribution chance` albo `szansa na odwet`,
- jeżeli linia affixu zawiera jednocześnie realny roll i zakres referencyjny w `[]` albo `()`, parser ma wybierać realny roll jako wartość affixu,
- liczby z zakresu referencyjnego w `[]` albo `()` nie są domyślną wartością affixu i nie mogą wygrywać z głównym rollem linii,
- uszkodzone fragmenty zakresów z OCR, np. `[1001%`, nie mogą powodować odrzucenia kolejnego poprawnego rolla affixu,
- bazowa wartość pancerza itemu, np. `1 131 pkt. pancerza`, nie jest affixem i nie może zasilać pól `strength`, `thorns` ani `block chance`,
- nieobsługiwane affixy nie mogą być mapowane do statów foundation.

Ten etap importu miecza nie zmienia `DamageEngine`, nie podłącza itemu do liczenia DPS current build i nie implementuje `effectiveRank`.

Jawne ograniczenia aktualnego foundation importu:
- flow nie obiecuje pełnej bezbłędności OCR ani vision,
- aktualny foundation wykonuje techniczną walidację obrazu, heurystyczny preprocessing, realny OCR kilku wariantów pojedynczego itemu oraz renderuje poziom niepewności pól,
- heurystyczne wycięcie obszaru tekstowego ma ograniczać wpływ ramki, grafiki itemu i dolnego overlayu, ale nie daje gwarancji pełnego odcięcia każdego zakłócającego elementu,
- przy równorzędnych sprzecznych odczytach z kilku wariantów OCR pole pozostaje z obniżoną pewnością zamiast sztucznego podbicia pewności,
- gdy parser nie potrafi bezpiecznie odróżnić głównego rolla od wartości referencyjnych, pole ma pozostać nierozpoznane zamiast zgadywania,
- użytkownik musi ręcznie zatwierdzić item przed użyciem, a główną warstwą korekty jest lista affixów z możliwością zmiany typu, wartości, usunięcia i dodania wiersza,
- użytkownik może dodać wiele affixów bez przeładowania strony, a stan formularza, zaznaczenia `Greater Affix` i wybrany aspekt muszą przetrwać walidację,
- przed pierwszym uploadem sekcja `Wstępnie rozpoznane pola` pokazuje jawny empty state i komunikuje, gdzie pojawi się wynik OCR,
- akcja `Dodaj affix` zmienia tylko bieżący formularz; nie zapisuje itemu ani nie uczy bazy wiedzy,
- po `Zatwierdź item` zatwierdzony item jest automatycznie zapisywany do biblioteki,
- po automatycznym zapisie użytkownik dostaje czytelne potwierdzenie z nazwą itemu, plikiem źródłowym, slotem, identyfikatorem biblioteki, wkładem oraz dalszymi akcjami `Załóż bohaterowi`, `Przejdź do biblioteki` i `Wróć do aktualnego buildu`,
- pełniejszy odczyt itemu jest prezentowany osobno od sekcji `Mapowanie do aktualnego modelu buildu` jako pełny zapis itemu z nagłówkiem, implicitami, affixami, efektem specjalnym, dodatkowymi liniami i socketem / gniazdem,
- projekcja foundation powstaje z edytowalnej listy affixów i pól kompatybilności potrzebnych obecnemu runtime; nie jest docelowym modelem itemu,
- flow nie importuje jeszcze całego ekwipunku ani całej postaci,
- flow nie buduje jeszcze pełnego wielo-itemowego workflow ani sesji inventory,
- flow nie omija obecnego modelu current build i nie buduje bocznego modelu runtime.

### 4.6. Baza wiedzy o itemach
Aktualny foundation repo obejmuje osobną bazę wiedzy o itemach jako warstwę obserwacji i przyszłych sugestii. Nie jest to biblioteka konkretnych itemów użytkownika i nie jest to runtime.

Kontrakt domenowy bazy wiedzy:
- `ItemKnowledgeService` uczy bazę wyłącznie po ręcznym zatwierdzeniu itemu w imporcie, po ostatecznej korekcie listy affixów,
- baza nie uczy się z surowego OCR, wstępnych kandydatów ani niezatwierdzonego formularza,
- obserwacje aspektów są liczone z finalnego `selectedAspectId`; surowe linie OCR aspektu w `FullItemRead` są diagnostyką i nie tworzą arbitralnej obserwacji aspektu, jeśli użytkownik nie zatwierdził aspektu z katalogu,
- `ItemKnowledgeSnapshot` reprezentuje aktywną epokę wiedzy oraz wpisy obserwacji pogrupowane po slocie i typie itemu,
- `ItemKnowledgeEntry` zapamiętuje dla typu itemu liczbę zatwierdzonych itemów, obserwowane typy affixów, obserwowane aspekty / efekty specjalne oraz liczniki wystąpień,
- baza korzysta z istniejących `ImportedItemAffix`, `ImportedItemAffixType`, `selectedAspectId` i `FullItemRead`; nie wprowadza drugiego konkurencyjnego modelu affixów ani aspektów,
- baza wiedzy jest odseparowana od `SavedImportedItem`: zapis konkretnego itemu pozostaje źródłem prawdy dla itemu użytkownika, a baza wiedzy przechowuje tylko zagregowane obserwacje,
- obserwacje są zapisywane w osobnym pliku `item-knowledge.db` w katalogu danych aplikacji.

Kontrakt epoki i resetu:
- każda baza ma aktywną epokę wiedzy, np. `Epoka wiedzy 1` albo nazwaną epokę sezonową,
- reset bazy wiedzy rozpoczyna kolejną epokę i czyści aktywne obserwacje,
- reset jest przeznaczony na zmianę sezonu, patcha albo sytuację, w której użytkownik nie chce mieszać nowych obserwacji ze starymi.

Kontrakt UI bazy wiedzy:
- ekran `/baza-wiedzy-itemow` pokazuje aktywną epokę, liczbę typów itemów, liczbę zatwierdzonych obserwacji, liczbę obserwacji affixów i aspektów,
- ekran pokazuje wpisy wiedzy per typ itemu z listą zaobserwowanych typów affixów i aspektów,
- ekran pozwala rozpocząć nową epokę wiedzy i wyczyścić aktywne obserwacje,
- baza wiedzy na tym etapie nie podpowiada automatycznie affixów, nie ostrzega o nietypowych affixach i nie wpływa na search; przygotowuje model pod te przyszłe scenariusze.

Poza aktualnym zakresem bazy wiedzy pozostają:
- automatyczne poprawianie konkretnego itemu użytkownika,
- autopodpowiedzi w formularzu importu,
- warningi zgodności affixów z typem itemu,
- wyszukiwanie idealnego itemu po bazie wiedzy,
- archiwalny przegląd poprzednich epok.

### 4.7. Minimalna biblioteka zapisanych itemów
Aktualny foundation repo obejmuje minimalną bibliotekę zapisanych itemów jako warstwę aplikacyjną nad current build, a nie osobny model runtime.

Kontrakt biblioteki itemów:
- `SavedImportedItem` jest trwałą wersją zatwierdzonego itemu z własnym stabilnym `itemId`,
- biblioteka może przechowywać wiele itemów tego samego slotu,
- ekran `/biblioteka-itemow` pokazuje zapisane itemy jako kompaktowy indeks tabelaryczny, w którym jeden item zajmuje jeden wiersz,
- główna tabela biblioteki ma kolumny `Item`, `Slot / typ`, `Aspekt`, `Affixy` i `Akcje`; nie ma osobnych kolumn `GA`, `Status` ani `Źródło`,
- normalny widok biblioteki nie pokazuje komunikatów OCR-weryfikacyjnych z importu, takich jak niepełny odczyt efektu aspektu; te komunikaty pozostają częścią importu i formularza edycji, gdzie użytkownik zatwierdza dane,
- `SavedImportedItem` przechowuje także `FullItemRead`, czyli pełniejszy opis rozpoznanych linii itemu niezależny od aktualnie wspieranych pól runtime,
- `SavedImportedItem` przechowuje strukturalną listę affixów z polem `greaterAffix` oraz finalny `selectedAspectId`; surowy tekst aspektu w `FullItemRead` nie zastępuje wyboru z registry,
- kolumna `Item` pokazuje nazwę itemu jako link otwierający szczegóły, rzadkość i moc przedmiotu, jeśli są dostępne; item założony przez aktywnego bohatera dostaje subtelny badge `Założony` przy nazwie zamiast osobnej kolumny statusu,
- kolumna `Slot / typ` pokazuje slot ekwipunku i typ itemu,
- kolumna `Aspekt` pokazuje finalny aspekt z `selectedAspectId` przez nazwę z registry albo `Brak`; opis efektu aspektu z registry jest dostępny jako tooltip / `title` na nazwie aspektu, bez rozpychania tabeli,
- kolumna `Affixy` pokazuje pionową krótką listę zatwierdzonych affixów; `Greater Affix` jest prezentowany bezpośrednio przy konkretnym affixie jako gwiazdka, np. `★ 13,2% redukcji czasu odnowienia`, a nie jako osobny status itemu,
- szczegóły itemu otwierają się po kliknięciu nazwy itemu jako modal / popup na środku ekranu oparty o SSR HTML i CSS `:target`, bez nowego API i bez SPA,
- modal szczegółów itemu pokazuje osobne sekcje `Dane podstawowe`, `Base stats`, `Implicit / linie bazowe`, `Affixy`, `Aspekt / efekt legendarny`, `Socket / gniazdo`, źródło itemu oraz diagnostykę OCR tylko wtedy, gdy istnieją faktyczne linie diagnostyczne; pusta sekcja `Diagnostyka OCR` nie jest renderowana,
- biblioteka renderuje właściwe affixy z zatwierdzonej listy `SavedImportedItem.getAffixes()`, dzięki czemu ręczne usunięcie albo korekta affixu nie jest cofana przez surowy `FullItemRead`,
- base staty i implicity nie mogą być renderowane jako `Affix`; `Greater Affix` jest pokazywany wyłącznie prezentacyjną gwiazdką przy affixach z `greaterAffix=true`,
- biblioteka normalizuje base staty defensywnie: moc przedmiotu pozostaje w `Dane podstawowe`, pancerz pozostaje w `Base stats`, a sklejki OCR typu `800 1 131 pkt. pancerza` nie są renderowane jako osobna wartość,
- sekcja aspektu w normalnym widoku biblioteki pokazuje finalny `selectedAspectId` przez nazwę z `AspectRegistry`, typ aspektu i opis efektu znanego aspektu z registry; `uniqueEffectText` unikatowego itemu może być pokazany osobno ze statusem opisowym, ale raw OCR effect nie zastępuje finalnego aspektu registry,
- UI importu dla dopasowanego aspektu katalogowego nie pokazuje pomocniczego `Odczyt OCR efektu`; surowy odczyt może pozostać w modelu/debugu albo w ścieżkach ręcznej diagnostyki, ale nie jest głównym tekstem formularza,
- kolumna `Akcje` używa kompaktowych ikon o wspólnym rozmiarze dla `Załóż / Zmień w slocie`, `Edytuj` i `Usuń`; akcje mają dostępne `aria-label`, a semantyka pustego i zajętego slotu pozostaje zachowana w etykietach,
- każdy zapisany item ma minimalistyczną akcję `Edytuj`, prowadzącą do SSR formularza `/biblioteka-itemow/edytuj?itemId=<id>`, oraz minimalistyczną akcję `Usuń` z dostępnym `aria-label`,
- tabela biblioteki nie pokazuje przycisku `Pokaż slot w current build`; przejście do current build pozostaje w nawigacji strony, a nie w każdym wierszu itemu,
- formularz edycji startuje z danych `SavedImportedItem`: `sourceImageName`, `slot`, `FullItemRead`, strukturalne affixy, `greaterAffix` i finalny `selectedAspectId`; OCR nie jest uruchamiany ponownie,
- zapis edycji używa tego samego mapowania i walidacji co import, aktualizuje istniejący item pod tym samym `itemId`, zachowuje `sourceImageName`, zapisuje zaktualizowany `FullItemRead` po korekcie affixów oraz zachowuje finalny `selectedAspectId`,
- jeśli edytowany item jest aktualnie założony bohaterowi, aktywna selekcja nadal wskazuje ten sam `itemId`, więc zmiana automatycznie wpływa na używany item bez ponownego przypisania,
- panel filtrów biblioteki działa jako SSR GET query params i obejmuje: `q`, `slot`, `type`, `status`, `aspect`, `affix` oraz `greater=true`,
- filtr `q` przeszukuje strukturalne dane itemu: nazwę itemu, plik źródłowy, displayName wybranego aspektu i nazwy / linie zatwierdzonych affixów,
- filtry domenowe działają na strukturze `SavedImportedItem`: `slot`, `selectedAspectId`, lista `affixes`, `greaterAffix`, `FullItemRead.itemTypeLine` i `FullItemRead.itemName`, a nie na przypadkowym płaskim OCR dumpie,
- filtr aspektu obsługuje wszystkie aspekty z `ApplicationAspectRegistry` oraz wartość `brak aspektu`; filtr statusu rozróżnia itemy założone i nieużywane względem aktywnego bohatera,
- UI filtrów pokazuje liczbę wyników, zachowuje wybrane wartości po filtrowaniu oraz ma akcję `Wyczyść filtry`,
- `HeroItemSelection` przechowuje najwyżej jeden aktywny `savedItemId` per `HeroEquipmentSlot` dla konkretnego bohatera,
- biblioteka jest wspólna dla wszystkich bohaterów, ale aktywna selekcja slotów jest niezależna per bohater,
- założenie itemu z biblioteki jest walidowane względem hero slotu; nie można przypisać itemu z niepasującego typu slotu,
- akcja `Załóż bohaterowi` jest ikoną primary dla zgodnego pustego slotu aktywnego bohatera, a `Zmień w slocie` ikoną primary dla zgodnego slotu, który ma już inny item,
- item używany przez aktywnego bohatera jest oznaczony przy nazwie badge'em `Założony`; item nieużywany nie dostaje osobnej kolumny ani ciężkiego statusu w głównej tabeli,
- usunięcie itemu czyści aktywny wybór tego itemu u wszystkich bohaterów, jeśli był aktywny,
- biblioteka jest lokalną biblioteką użytkownika, a nie systemem kont, chmurą ani współdzielonym inventory,
- biblioteka nie jest pełnym inventory managerem, stashem ani porównywarką itemów.

Kontrakt integracji biblioteki z current build:
- ukryte dane formularza `Policz aktualny build` zachowują kompatybilność z ręczną bazą statów poza biblioteką itemów,
- ręczna baza może być częściowo pusta albo zerowa, jeżeli finalne effective stats zostaną domknięte przez aktywne itemy z biblioteki; użytkownik nie edytuje już tej bazy w osobnej sekcji UI current build,
- aktywne itemy z biblioteki są deterministycznie dodawane do tej bazy w kontekście aktywnego bohatera,
- użytkownik nie powinien ręcznie wpisywać tych samych statów, które pochodzą już z aktywnych itemów,
- ekran `Policz aktualny build` pokazuje pełny layout slotów bohatera jako sekcję `Ekwipunek aktualnego buildu`, ale jest to wyłącznie warstwa prezentacji i sterowania aktywną selekcją slotów bohatera,
- zmiana itemu per slot z poziomu current build nie buduje osobnego equipment runtime i nie omija `ItemLibraryService`,
- current build nie renderuje już osobnej sekcji `Użyte itemy`; aktywne itemy pozostają widoczne w slotach ekwipunku, a ich wkład nadal zasila effective stats,
- effective current build nadal kończy się zwykłym `CurrentBuildRequest`,
- walidacja requestu dotyczy dopiero finalnych effective stats po zsumowaniu ręcznej bazy i aktywnych itemów,
- `CurrentBuildSnapshotFactory` i runtime nadal pracują na tych samych płaskich polach co wcześniej,
- P1.3.3 zmienia wyłącznie prezentację biblioteki itemów z dużych kart na kompaktowy indeks tabelaryczny z rozwijanymi szczegółami; `Damage Engine`, manual simulation, search runtime i projekcja DPS nie zostały zmienione,
- biblioteka itemów nie buduje alternatywnego snapshot flow i nie omija istniejącego runtime.

Kontrakt integracji biblioteki z backendowym searchem:
- tryb biblioteki itemów jest opcjonalnym rozszerzeniem istniejącego `BuildSearchRequest`, a nie osobnym trybem runtime,
- search generuje kombinacje co najwyżej jednego zapisanego itemu per slot wyłącznie z aktualnej biblioteki użytkownika,
- search nie buduje pełnego equipment managera ani osobnego modelu całego ekwipunku,
- dla każdej kombinacji biblioteki search liczy łączny wkład itemów i składa go do ręcznej bazy searcha przed zbudowaniem `CurrentBuildRequest`,
- kandydat searcha, wynik listy top oraz drill-down przenoszą tę samą kombinację biblioteki itemów bez ponownego mapowania do alternatywnego pipeline'u,
- włączenie trybu biblioteki itemów nie zmienia `CurrentBuildSnapshotFactory`, `ManualSimulationService` ani `Damage Engine`.

Kontrakt trwałości danych biblioteki:
- domyślny katalog danych biblioteki itemów to `~/.dstats/item-library/`,
- po restarcie aplikacji biblioteka używa tych samych danych użytkownika i nie zależy od katalogu builda,
- nowy build aplikacji korzysta z tej samej biblioteki użytkownika, o ile nie zmieniono `dstats.dataDir`,
- ustawienie system property `dstats.dataDir=/wlasna/sciezka` albo `-Ddstats.dataDir=C:\sciezka` nadpisuje domyślną lokalizację i jest kontraktowym sposobem wskazania katalogu testowego albo niestandardowego storage,
- przy pierwszym użyciu nowej lokalizacji aplikacja kopiuje legacy pliki `saved-items.db` i `active-selection.db` z `target/item-library-runtime/`, jeżeli nowa lokalizacja nie ma jeszcze własnych plików runtime,
- migracja kopiuje dane zamiast ich przenosić, więc stare pliki w `target/item-library-runtime/` pozostają kopią bezpieczeństwa,
- jeżeli nowa lokalizacja zawiera już choć jeden plik runtime biblioteki, staje się źródłem prawdy i legacy `target/item-library-runtime/` nie nadpisuje jej danych.

Poza aktualnym zakresem biblioteki itemów pozostają:
- pełny inventory manager,
- pełny stash postaci,
- batch import,
- porównania item vs item,
- osobny runtime wielo-itemowego ekwipunku.

## 5. Damage Engine
### 5.1. Zasady ogólne
- `weaponDamage` jest średnią wartością obrażeń broni używaną przez silnik.
- `skillDamagePercent` jest właściwym wejściem do liczenia `baseDamage`.
- `flatDamage` nie należy do aktualnego modelu obrażeń.
- `additive` jest sumą bonusów zebranych do jednego mnożnika.
- osobne multipliery są liczone iloczynem.
- brak snapshotu runtime: trafienie liczy aktualny stan bohatera i aktualny stan celu w momencie trafienia lub detonacji.
- overpower nie należy do aktualnego kontraktu projektu.

### 5.2. Wzory
Aktualny model bazowy:

```text
baseDamage = weaponDamage * skillDamagePercent / 100
```

Aktualny model main stat dla obecnie zaimplementowanego zakresu paladinocentrycznego:

```text
baseMainStat = 10 + (level - 1)
itemMainStat = suma affixów STRENGTH
totalMainStat = baseMainStat + itemMainStat
mainStatMultiplier = 1 + (totalMainStat / 1000)
```

Aktualny model Intelligence używanej przez crit:

```text
baseIntelligence = 7 + (level - 1)
totalIntelligence = baseIntelligence + suma affixów INTELLIGENCE
critFromIntelligence = totalIntelligence * 0.0004
```

Aktualny model crit:

```text
critDamageBonusTotal = 0.50 + critFromItems + critFromIntelligence
critMultiplier = 1 + critDamageBonusTotal
```

Aktualny model redukcji poziomu:

```text
levelDamageReductionPercent = min(85, level + 25)
levelDamageReduction = levelDamageReductionPercent / 100
```

Aktualny model reactive foundation:

```text
blockChance = suma affixów BLOCK_CHANCE / 100
retributionChance = suma affixów RETRIBUTION_CHANCE / 100
activeBlockChance = blockChance + activeBlockChanceBonusPercent / 100
activeThorns = baseThornsFromBuild + activeThornsBonus
thornsRawDamage = round(activeThorns * mainStatMultiplier)
thornsFinalDamage = round(activeThorns * mainStatMultiplier * (1 - levelDamageReduction))
retributionExpectedRawDamage = round(activeThorns * mainStatMultiplier * activeBlockChance * retributionChance)
retributionExpectedFinalDamage = round(activeThorns * mainStatMultiplier * activeBlockChance * retributionChance * (1 - levelDamageReduction))
reactiveFinalPerEnemyHit = thornsFinalDamage + retributionExpectedFinalDamage
```

Aktualny kontrakt `Clash` dokłada do reactive foundation:
- `Crusader's March` ustawia `activeBlockChanceBonusPercent = 25`,
- `Punishment` ustawia `activeThornsBonus = 50`,
- oba buffy trwają `3 s`,
- `Resolve` jest stanem debug/runtime towarzyszącym `Crusader's March`,
- `Retribution expected raw = thornsDamage * activeBlockChance * retributionChance`.

Aktualny kontrakt `Advance` dokłada do direct-hit runtime:
- `Wave Dash` jest dodatkowym komponentem `DAMAGE` trafiającym ten sam cel,
- `Flash of the Blade` łączy `REPLACE_BASE_DAMAGE`, `APPLY_STATUS` oraz `SET_COOLDOWN`,
- cooldown jest liczony w runtime per skill, a nie jako boczna logika GUI albo CLI,
- stan `Vulnerable` na celu jest utrzymywany w tej samej pętli tickowej, która liczy direct hit, delayed hit, reactive i `stepTrace`.

Aktualny model komponentowy:

```text
componentRawDamage =
    baseDamage
    * weaponMultiplier
    * mainStatMultiplier
    * additiveMultiplier
    * statusAndEffectMultipliers
    * separateMultipliers
    * hitCount
```

```text
rawDamage = round(sum(componentRawDamage dla komponentów wliczanych do single target))
finalDamage = round(sum(componentRawDamage dla komponentów wliczanych do single target) * (1 - levelDamageReduction))
```

Wariant krytyczny jest liczony jako osobny przebieg pełnego pipeline'u obrażeń, a nie jako kosmetyczna etykieta na zwykłym hicie.

### 5.3. Kolejność pipeline'u
Kolejność liczenia pojedynczego komponentu:
1. `baseDamage`
2. `weapon multiplier`
3. `mainStat multiplier`
4. `additiveMultiplier`
5. multipliery statusów i efektów
6. `separate multipliers`
7. redukcja poziomu celu

### 5.4. Reguły komponentów
- Każdy aktywny komponent przechodzi przez pełny pipeline obrażeń.
- Warunki efektów są sprawdzane osobno dla każdego komponentu.
- Suma aktywnych komponentów daje końcowy `Raw hit`, `Single hit`, `Raw crit hit` i `Critical hit`.
- Komponenty niewliczane do single target mogą istnieć w debugu, ale nie mogą trafiać do wyniku końcowego.
- Wartości wyjściowe debug i golden values są liczbami całkowitymi po zaokrągleniu.

## 6. Statusy i zasady targetowania
### 6.1. Single target
- Single target jest domyślnym i obowiązkowym modelem całego projektu.
- Do `total damage`, `single hit`, `critical hit`, `raw hit` i `raw crit hit` liczone są tylko komponenty trafiające główny cel.
- Komponenty istniejące w danych skilla, ale nietrafiające głównego celu, pozostają informacją debugową i muszą być oznaczone jako pominięte w single target wraz z powodem.

### 6.2. Vulnerable / Exposed
- Dokumentacja i UI używają nazwy `Vulnerable` zgodnej z grą.
- Historyczne `Exposed` pozostaje wyłącznie aliasem technicznym w starszych artefaktach; nie wolno modelować go jako osobnej współczesnej mechaniki domenowej.
- `Vulnerable` jest globalnym multiplierem celu.
- Aktualny mnożnik `Vulnerable` to `x1.20`.
- Trafienie najpierw liczy obrażenia, a dopiero potem nakłada status.
- Trafienie nakładające `Vulnerable` nie korzysta jeszcze z nowo nałożonego statusu.

### 6.3. Czasy i stany obowiązujące w aktualnym modelu
Kontrakt statusów jest efektowy, nie globalny. Nie istnieje globalny domyślny czas trwania statusu ani buffa.

Reguła fallbacku jest zero-jedynkowa:
- każdy nowy efekt runtime, który nakłada status albo buff czasowy, musi jawnie definiować czas trwania w danych efektu,
- brak jawnie zdefiniowanego czasu trwania oznacza efekt nielegalny i taki efekt nie może zostać dodany do projektu,
- caps stacków i reguły usuwania stacków są definiowane osobno i nie zastępują jawnego czasu trwania tam, gdzie efekt jest czasowy.

Obowiązują następujące potwierdzone czasy i limity:
- `Vulnerable` nakładane przez warianty objęte zakresem startowym trwa `2 s`.
- `Judgement` detonuje po `3 s`.
- `Resolve` z `Crusader's March` trwa `3 s`.
- bonus `Punishment` do `Thorns` trwa `3 s`.

### 6.4. Warunki komponentów
Aktualny foundation wykorzystuje warunek komponentu:
- `Vulnerable` jako stan celu.

Pozostałe statusy wymieniane w szerszej specyfikacji nie są jeszcze aktywną częścią runtime repo.

## 7. Single hit / delayed hit / reactive damage
### 7.1. Single hit
- `Debug pojedynczego hita` pokazuje wyłącznie główne natychmiastowe trafienie skilla.
- `minHit` i `maxHit` nie należą do aktualnego modelu; projekt liczy obrażenia na bazie średnich obrażeń broni.
- Panel single hit pokazuje stan targetu przed użyciem skilla.
- Jeżeli skill nakłada status, panel single hit nie może pokazywać korzyści z tego statusu dla tego samego trafienia.

Rozstrzygnięcia kontraktowe dla aktualnie zaimplementowanych wariantów:
- `Brandish + Powrót światłości` liczy dwa komponenty `73% + 73%`; nie wolno liczyć `105% + 73%`.
- `Brandish + Krzyżowe uderzenie (Vulnerable)` zawsze podmienia główny hit na `168%`; dwa dodatkowe łuki `168%` są komponentami bocznymi i nie wchodzą do single target.
- Dla `Brandish + Krzyżowe uderzenie (Vulnerable)` warunek `Vulnerable` dotyczy wyłącznie dwóch dodatkowych bocznych trafień.
- Bazowy `Holy Bolt` liczy tylko główne natychmiastowe trafienie.
- Bazowe rozszerzenie `Judgement` nie zmienia natychmiastowego single hita `Holy Bolt`; dodaje osobny delayed hit.
- Bazowy `Advance` liczy pojedynczy direct hit `147%`.
- `Advance + Wave Dash` liczy dwa single target komponenty `147% + 191%`.
- `Advance + Flash of the Blade` zastępuje bazowy hit pojedynczym direct hitem `322%`, nakłada `Vulnerable` po trafieniu i nie korzysta z nowo nałożonego statusu dla tego samego uderzenia.

Reguła referencyjnego zaokrąglenia dla `Brandish rank 5 + Krzyżowe uderzenie (Vulnerable)`:
- w modelu single target końcowy `raw crit hit` dla głównego trafienia jest liczony od wcześniej zaokrąglonego `raw hit`, a nie od niezaokrąglonej wartości exact,
- kontraktowy porządek dla tego przypadku jest następujący:
  1. policz exact `raw hit` dla głównego trafienia `168%`,
  2. zaokrąglij go do całkowitego `raw hit`,
  3. policz `raw crit hit = round(rawHitRounded * critMultiplier)`,
- ta reguła jest wymagana, aby referencyjny wynik dla tego scenariusza wynosił `raw hit = 34` oraz `raw crit hit = 52`.

### 7.2. Delayed hit
Delayed hit jest częścią aktualnego foundation repo wyłącznie dla `Holy Bolt + Judgement`.

Obowiązujące reguły:
- delayed hit jest osobnym komponentem obrażeń i nie należy do natychmiastowego single hita,
- delayed hit jest liczony w `trigger time`,
- delayed hit używa tego samego `Damage Engine`,
- `Judgement` jest opóźnionym hitem `80% skillDamagePercent`,
- `Judgement` detonuje po `3 s`,
- na jednym celu `Judgement` nie stackuje się,
- kolejny cast `Holy Bolt` nie odświeża timera aktywnego `Judgement`,
- ponowne nałożenie jest dozwolone dopiero po detonacji poprzedniego delayed hita,
- delayed hit `Judgement` jest single target i wchodzi do `total damage`.

### 7.3. Reactive damage
Reactive damage jest częścią aktualnego foundation repo dla `Thorns`, `Retribution` i pierwszego pełnego use case `Clash`.

Obowiązujące reguły:
- reactive damage jest osobnym torem obrażeń i nie należy do single hita skilla,
- reactive damage wchodzi do `total damage` i `DPS`,
- reactive damage używa tego samego runtime manual simulation co delayed hit i aktywny cast,
- przeciwnik trafia bohatera raz na `3 s`,
- pierwszy enemy hit następuje w `t=3`,
- kolejne enemy hity następują w `t=6`, `t=9`, `t=12` i tak dalej,
- `Thorns` liczone jest z bazowej wartości buildu przechodzącej przez `mainStatMultiplier` i redukcję poziomu celu,
- `Retribution` jest liczone deterministycznie jako `expected value`,
- `Retribution expected raw = thornsDamage * activeBlockChance * retributionChance`,
- `Clash` jest pierwszym pełnym use case reactive foundation i korzysta z tego samego toru reactive co każdy inny scenariusz,
- bazowy `Clash` nie dodaje direct damage i służy wyłącznie do ustawiania własnych buffów reactive,
- `Crusader's March` zwiększa aktywny `block chance` i ustawia stan `Resolve`,
- `Punishment` zwiększa aktywne `Thorns`,
- reactive debug zapisuje per enemy hit co najmniej `Resolve`, aktywny `block chance`, aktywny bonus do `Thorns`, `Thorns raw/final`, `Retribution expected raw/final` i `Reactive final`,
- pełny ogólny system `Resolve`, `Fervor` i dalsze reactive feature'y pozostają poza aktualnym zakresem.

## 8. Rotacja symulacji
### 8.1. Horyzont i tick
Aktualny foundation implementuje tickową manual simulation dla trybu `Policz aktualny build`.

Obowiązujący zakres:
- horyzont jest dodatnim parametrem wejściowym manual simulation przekazywanym do tego samego runtime przez CLI i GUI,
- referencyjne smoke testy manual simulation oraz zamrożone wartości README pozostają liczone dla horyzontu `60 s`,
- kolejność ticku:
  1. delayed hit,
  2. reactive damage,
  3. aktywny cast.
- jeżeli w danym ticku nie istnieje legalny cast, tick pozostaje częścią symulacji i jest zapisywany jako `WAIT`.

### 8.2. Model LRU
Aktualny foundation implementuje model wyboru aktywnego skilla jako `LRU`.

Obowiązujące reguły wyboru:
- wybierany jest legalny skill użyty najdawniej,
- skill nigdy wcześniej nieużyty ma wyższy priorytet niż skill użyty wcześniej,
- przy remisie wygrywa kolejność na pasku,
- kandydat musi jednocześnie:
  1. być na pasku,
  2. mieć `rank > 0`,
  3. być legalnie aktywny,
  4. nie być na cooldownie,
  5. mieć wymagany zasób, jeżeli skill go używa.

Zakres aktualnej implementacji:
- wszystkie aktualnie zaimplementowane skille mają `resourceCost = 0`,
- `Brandish`, `Holy Bolt` i `Clash` mają efektywnie `cooldown = 0`,
- `Advance + Flash of the Blade` ustawia efektywny cooldown `8 s`,
- cooldown jest utrzymywany osobno per skill wewnątrz runtime i wpływa na wybór `LRU`,
- `WAIT` występuje wtedy, gdy żaden skill z paska nie spełnia minimalnych warunków legalnego castu.

### 8.3. Trace
`stepTrace` jest częścią aktualnego foundation repo i musi pochodzić z dokładnie tej samej pętli symulacji, która liczy wynik końcowy.

Minimalny kontrakt `stepTrace`:
- numer sekundy,
- akcja `SKILL` albo `WAIT`,
- nazwa akcji,
- damage bezpośredni,
- damage z delayed hitów,
- damage z reactive hitów,
- łączny damage kroku,
- cumulative damage po kroku,
- jawny zapis kontraktowej kolejności ticku,
- stan skilli z paska potrzebny do walidacji wyboru `LRU`,
- stan cooldownu per skill potrzebny do ręcznej walidacji `WAIT` i powrotu skilla po cooldownie,
- `selectionReason`.

## 9. Build search
### 9.1. Jednostka oceny
Backendowy search jest częścią aktualnego foundation repo.

Jednostką oceny jest pojedynczy legalny kandydat zawierający:
- pełny opis wejściowego buildu w modelu aktualnych statów użytkownika,
- legalne stany skilli foundation,
- legalny i uporządkowany action bar,
- wspólny horyzont symulacji.

Search buduje dla każdego kandydata dokładnie taki sam `HeroBuildSnapshot`, jaki byłby zbudowany dla odpowiadającego mu flow `Policz aktualny build`.

Aktualny kontrakt searcha rozdziela cztery poziomy pracy searcha:
- preflight / audit search space,
- surową ocenę legalnych kandydatów,
- znormalizowane wyniki użytkowe prezentowane po ocenie,
- drill-down pojedynczego reprezentanta znormalizowanego wyniku.

Preflight / audit nie uruchamia jeszcze właściwej oceny runtime. Jest to osobny etap kontraktowy pokazujący koszt przestrzeni searcha przed albo obok wyniku.

### 9.2. Etap 1 - preflight / audit
Minimalny kontrakt preflightu searcha obejmuje:
- liczbę legalnych kandydatów,
- rozmiar wejściowej przestrzeni statów,
- opcjonalnie rozmiar przestrzeni kombinacji biblioteki itemów, gdy tryb biblioteki jest włączony,
- rozmiar przestrzeni skilli,
- rozmiar przestrzeni action bara,
- klasyfikację skali search space.

Definicje kontraktowe aktualnego foundation searcha:
- `rozmiar wejściowej przestrzeni statów` to iloczyn liczby dozwolonych wartości `level`, `weapon damage`, `strength`, `intelligence`, `thorns`, `block chance` i `retribution chance`,
- `rozmiar przestrzeni biblioteki itemów` to liczba deterministycznie wygenerowanych kombinacji `0..1 item per slot` z aktualnie zapisanej biblioteki użytkownika,
- `rozmiar przestrzeni skilli` to liczba legalnych wariantów nauczonych skilli wygenerowanych z aktualnych zakresów `rank`, `base upgrade` i `choice`,
- `rozmiar przestrzeni action bara` to łączna liczba legalnych konfiguracji action bara wynikających z legalnych wariantów skilli i dozwolonych rozmiarów paska,
- `liczba legalnych kandydatów` to dokładnie ta sama liczba kandydatów, która później zostanie oceniona przez backend searcha.

Jawne progi skali search space:
- `mała` dla `<= 100` legalnych kandydatów,
- `średnia` dla `101..1000` legalnych kandydatów,
- `duża` dla `> 1000` legalnych kandydatów.

### 9.3. Etap 2 - legalne buildy
Aktualny backend search obejmuje wyłącznie foundation:
- `Brandish`
- `Holy Bolt`
- `Clash`
- `Advance`
- obecny model statów buildu: `level`, `weapon damage`, `strength`, `intelligence`, `thorns`, `block chance`, `retribution chance`
- obecny model action bara

Wejście searcha jest dyskretne i ograniczone:
- zakres statów wejściowych jest podawany jako lista dozwolonych wartości,
- przy włączonym trybie biblioteki `weapon damage` w ręcznej bazie może wynosić `0`, jeżeli dodatni `weapon damage` wnosi wybrany item z biblioteki,
- zakres stanu każdego skilla jest podawany jako lista dozwolonych `rank`, `base upgrade` i `choice`,
- search generuje wyłącznie stany legalne względem kontraktu `SkillState`,
- `rank 0` oznacza `OFF` i nie może mieć upgrade'ów,
- search nie tworzy nielegalnych kombinacji choice bez bazowego rozszerzenia.

Kontrakt kandydatów itemowych z biblioteki:
- search rozważa wyłącznie zapisane itemy biblioteki użytkownika,
- search wybiera co najwyżej jeden item per slot,
- search może pozostawić slot bez wybranego itemu, jeżeli dana kombinacja go nie zawiera,
- search nie przechowuje osobnego runtime pełnego equipmentu; przekształca tylko łączny wkład wybranej kombinacji do istniejących płaskich pól current build.

### 9.4. Etap 3 - konfiguracje paska skilli
Aktualny backend search generuje legalne konfiguracje paska skilli dla wskazanych rozmiarów action bara.

Kontrakt legalności action bara:
- action bar może zawierać wyłącznie nauczone skille z `rank > 0`,
- ten sam skill nie może wystąpić dwa razy,
- rozmiar action bara musi należeć do zakresu wejściowego searcha,
- kolejność action bara jest semantyczna, ponieważ wpływa na tie-break `LRU`,
- search nie traktuje permutacji jako szumu technicznego; inna kolejność paska to inny kandydat tylko wtedy, gdy naprawdę zmienia zachowanie runtime.

### 9.5. Wymagania runtime dla searcha
Aktualny backend search używa dokładnie tego samego runtime co manual simulation:
- kandydat searcha jest adaptowany do `CurrentBuildRequest`,
- `CurrentBuildSnapshotFactory` buduje z niego `HeroBuildSnapshot`,
- `BuildSearchEvaluationService` wywołuje ten sam `ManualSimulationService`,
- `ManualSimulationService` korzysta z tego samego `Damage Engine`, tej samej logiki `LRU`, tych samych cooldownów, delayed hitów, statusów i reactive.

Search nie może:
- używać skróconej logiki liczenia,
- liczyć DPS poza `SimulationResult`,
- omijać `CurrentBuildSnapshotFactory`,
- implementować osobnego „mock runtime” dla rankingu.

### 9.6. Ocena, ranking, normalizacja i drill-down
Ranking kandydatów jest deterministyczny i na obecnym etapie sortuje po:
1. `total damage` malejąco,
2. `DPS` malejąco,
3. deterministycznym kluczu opisu kandydata, który przy trybie biblioteki obejmuje także wybraną kombinację itemów.

Po posortowaniu surowych ocen działa warstwa normalizacji prezentacyjnej:
- normalizacja nie zmienia generatora kandydatów,
- normalizacja nie zmienia liczby ocenionych kandydatów,
- normalizacja nie zmienia surowej kolejności oceny,
- normalizacja redukuje tylko rekordy równoważne użytkowo dla aktualnego foundation,
- normalizacja nie może scalać wyników, które zmieniają zachowanie runtime,
- normalizacja nie może scalać dwóch różnych kombinacji itemów biblioteki, nawet jeśli dają taki sam runtime signature,
- dla aktualnego foundation dopuszczalne jest scalenie kandydatów różniących się wyłącznie dodatkowymi nauczonymi skillami poza action barem, jeśli action bar, konfiguracja skilli na pasku i sygnatura runtime pozostają takie same.

Minimalny wynik użytkowy searcha zawiera:
- liczbę ocenionych kandydatów,
- liczbę wyników po normalizacji,
- top `N` znormalizowanych wyników,
- opis wejściowego buildu,
- opis skilli znajdujących się na action barze,
- wybrany action bar,
- informację, czy wynik korzysta z trybu biblioteki itemów,
- listę wybranych itemów z biblioteki per slot,
- łączny wkład wybranych itemów do effective stats,
- `total damage`,
- `DPS`.

Aktualny drill-down searcha:
- nie zmienia generatora kandydatów ani liczby ocenionych kandydatów,
- nie zmienia surowej oceny ani rankingu,
- pokazuje szczegóły reprezentanta wybranego wyniku po normalizacji,
- odtwarza tę samą kombinację itemów z biblioteki i ten sam łączny wkład itemów widoczny na liście wyników,
- odtwarza szczegóły przez ten sam runtime i te same modele wynikowe co `Policz aktualny build`.

Poza aktualnym zakresem foundation searcha pozostają:
- live progress GUI,
- eksport CSV,
- wielowątkowość,
- zaawansowane heurystyki i optymalizacje wydajności,
- pełny inventory manager i pełny stash budowane jako osobny model searcha,
- bogatszy UX searcha ponad minimalny SSR.

## 10. UI, debug i prezentacja wyników
### 10.1. Zasady ogólne
Repo implementuje działające webowe GUI SSR i CLI dla flow `Policz aktualny build`, osobne CLI backendowego searcha, minimalne GUI SSR dla flow `Znajdź najlepszy build`, audit/preflight search space, drill-down SSR szczegółów wybranego wyniku oraz pierwszy SSR flow importu pojedynczego itemu ze screena. Aktualny foundation dostarcza:
- ekran główny `/` jako hub produktu z listą modułów, grup i statusów,
- centralny app shell z globalną nawigacją SSR na głównych ekranach aplikacji,
- centralny rejestr modułów aktywnych i placeholderowych,
- moduł `Bohaterowie` jako produktowy punkt wejścia do zarządzania listą bohaterów i aktywnym bohaterem,
- lekkie placeholder pages dla przyszłych sekcji dodatku i sezonu,
- aktywnego bohatera jako kontekst dla current build, importu, biblioteki itemów, searcha i drill-downu,
- główną ścieżkę użytkownika opartą o `CurrentBuildRequest`, a nie o testowy snapshot,
- wspólną usługę aplikacyjną `CurrentBuildCalculationService` dla GUI i CLI,
- wspólną fabrykę runtime `CurrentBuildSnapshotFactory` budującą `HeroBuildSnapshot`,
- osobny input flow `ItemImageImportRequest -> ItemImageImportService -> ItemImportFormMapper` dla importu obrazu itemu,
- preprocessing OCR i deterministyczne scalanie wyniku per pole jeszcze przed ręcznym potwierdzeniem użytkownika,
- trwałą bibliotekę itemów opartą o prosty lokalny zapis plikowy bez bazy danych,
- osobną bazę wiedzy itemów opartą o prosty lokalny zapis plikowy, z aktywną epoką i resetem obserwacji,
- osobną usługę `BuildSearchCalculationService` dla backendowego searcha,
- prosty serwer HTTP z SSR bez rozbudowanego frontendu JS,
- pojedynczy ekran formularza dla `Brandish`, `Holy Bolt`, `Clash` i `Advance`,
- render wyniku oparty wyłącznie o istniejące modele debug i wynik runtime,
- sekcję reactive debug dla foundation `Thorns`, `Retribution` i use case `Clash`,
- trace z informacją o cooldownie i `WAIT` dla use case `Advance`,
- modele debug w kodzie,
- CLI dla równoległego ręcznego smoke testu użytkownika,
- CLI searcha z tekstowym outputem audytu, minimalnego progressu oraz znormalizowanych top wyników,
- osobny ekran GUI SSR importu wspomaganego obrazem dla pojedynczego itemu,
- render realnie rozpoznanych pól OCR, poziomu niepewności, empty state przed uploadem i ręcznego potwierdzenia pól itemu,
- prosty ekran SSR `/biblioteka-itemow` z listą zapisanych itemów i wyborem aktywnego itemu per slot,
- prosty ekran SSR `/baza-wiedzy-itemow` ze statusem epoki, licznikami obserwacji, listą zaobserwowanych affixów / aspektów oraz resetem wiedzy,
- sekcję `Ekwipunek aktualnego buildu` na ekranie `Policz aktualny build`,
- wcześniejszą sekcję użytych itemów zastąpioną prezentacją aktywnych itemów w slotach ekwipunku na ekranie `Policz aktualny build`,
- zmianę aktywnego itemu per slot bezpośrednio z current build przez ten sam stan biblioteki itemów, ale w kontekście aktywnego bohatera,
- automatyczny zapis zatwierdzonego itemu do biblioteki po kliknięciu `Zatwierdź item`,
- uczenie bazy wiedzy dopiero po kliknięciu `Zatwierdź item`, z listy affixów po ręcznej korekcie i z zatwierdzonego pełnego odczytu,
- akcje dalszego flow po zatwierdzeniu importu: `Załóż bohaterowi`, `Przejdź do biblioteki` i `Wróć do aktualnego buildu`,
- wynik zapisu itemu do biblioteki z dalszymi akcjami użytkownika,
- wejście do importu itemu bez sesji wielu itemów, z możliwością zachowania kontekstu current build przez query string,
- mapowanie zatwierdzonego itemu do modelu `Item` oraz do agregowanych pól current build,
- osobny ekran GUI SSR searcha dla minimalnej przestrzeni foundation z wyeksponowanym trybem biblioteki itemów,
- sekcję audit / preflight searcha w GUI searcha oraz bardziej czytelną hierarchię formularza,
- osobną stronę SSR szczegółów reprezentanta znormalizowanego wyniku searcha.

### 10.3. Konfiguracja do porównania
Na obecnym etapie foundation nie ma warstwy prezentacji konfiguracji do porównania.

### 10.2. App shell, menu i statusy modułów
Aktualny frontend SSR ma produktową warstwę app shell porządkującą istniejące i przyszłe sekcje aplikacji bez zmiany logiki runtime.

Kontrakt ekranu głównego `/`:
- root renderuje stronę główną aplikacji, a nie formularz current build,
- ekran główny pokazuje dostępne moduły, przyszłe placeholdery, grupy funkcjonalne i status każdego modułu,
- ekran główny używa centralnego rejestru modułów zamiast ręcznie rozproszonych linków,
- ekran główny nie obiecuje mechanik dodatku, które nie zostały jeszcze ustabilizowane.

Kontrakt globalnej nawigacji:
- globalna nawigacja SSR jest widoczna co najmniej na ekranach `Strona główna`, `Bohaterowie`, `Policz aktualny build`, `Importuj item ze screena`, `Biblioteka itemów`, `Znajdź najlepszy build` oraz `Ranking obrażeń`,
- nawigacja jest renderowana z centralnego modelu modułów i prowadzi do aktywnych sekcji aplikacji,
- ekran `Bohaterowie` używa dokładnie tego samego app shell i tego samego wyróżnienia aktywnej zakładki co pozostałe główne ekrany,
- wspólny system wizualny SSR opiera się na jednych tokenach kolorystycznych dla tła, powierzchni, tekstu, obramowań, akcentu, przycisków i statusów,
- nawigacja nie buduje alternatywnego frontendu JS i pozostaje prostym SSR.

Kontrakt statusów modułów:
- `Dostępne` oznacza moduł działający na aktualnym foundation repo,
- `W przygotowaniu` oznacza moduł produktowo zaplanowany, ale bez doprecyzowanej jeszcze logiki,
- `Po premierze dodatku` oznacza sekcję odłożoną do czasu stabilizacji zasad po premierze,
- `Wymaga dodatku` oznacza sekcję zależną od nowych systemów dodatku,
- `Sezonowe` oznacza sekcję planowaną jako warstwa produktowa dla sezonu lub wydarzenia.

Aktualne moduły `Dostępne`:
- `Strona główna`
- `Bohaterowie`
- `Policz aktualny build`
- `Znajdź najlepszy build`
- `Ranking obrażeń`
- `Importuj item ze screena`
- `Biblioteka itemów`

Aktualne placeholdery przyszłych sekcji:
- `Plany Wojenne`
- `Medalion`
- `Kostka Horadrimów`
- `Filtr łupów`
- `Drzewka umiejętności 3.0`
- `System przedmiotów 3.0`
- `Wieża / rankingi`
- `Rezonująca Nienawiść`
- `Wędkarstwo`

Kontrakt placeholder pages:
- placeholder ma własny URL, nazwę, opis, status i grupę modułu,
- placeholder jasno komunikuje, że szczegółowa logika zostanie doprecyzowana po stabilizacji zasad po premierze dodatku,
- placeholder nie implementuje mechaniki, nowych formuł, nowych systemów itemów ani osobnego runtime,
- placeholder jest częścią świadomie zaprojektowanej architektury aplikacji, a nie techniczną atrapą pozostawioną zamiast ukończonego flow.

### 10.4. Debug single hit
Aktualny foundation implementuje debug danych oraz ich minimalny render w GUI i CLI. W kodzie istnieją:
- `DamageBreakdown` jako wynik końcowy pojedynczego uderzenia,
- `DamageComponentBreakdown` jako wynik debug pojedynczych komponentów,
- `SkillHitDebugSnapshot` jako reprezentatywny debug bezpośredniego hita per skill użyty w symulacji,
- informacja, czy komponent został wliczony do single target, czy pominięty z powodem.

`SimulationResult` nie modeluje jednego globalnego „selected skill” ani jednego globalnego `singleHitBreakdown` dla całej symulacji wieloskillowej.

### 10.5. Delayed i reactive debug
Aktualny foundation implementuje delayed debug dla `Judgement`:
- informację, kiedy delayed hit został nałożony,
- informację, kiedy miał detonować,
- informację, czy detonował w horyzoncie symulacji,
- breakdown delayed hita po detonacji,
- informację, czy `Judgement` pozostał aktywny na końcu horyzontu.

Aktualny foundation implementuje reactive debug dla `Thorns`, `Retribution` i use case `Clash`:
- informację, w której sekundzie wystąpił enemy hit,
- informację, czy `Resolve` było aktywne i ile czasu pozostawało,
- aktywny `block chance`,
- aktywny bonus do `Thorns`,
- `Thorns raw / tick`,
- `Thorns final / tick`,
- `Retribution expected raw / tick`,
- `Retribution expected final / tick`,
- `Reactive final / tick`,
- sumaryczny wkład reactive do wyniku końcowego,
- informację, czy `Resolve` oraz reactive bonusy pozostały aktywne na końcu horyzontu.

### 10.6. Wynik searcha
Aktualny foundation implementuje backendowy wynik searcha, preflight / audit, minimalny progress CLI, render w CLI i minimalnym GUI SSR oraz drill-down pojedynczego reprezentanta znormalizowanego wyniku.

Minimalny kontrakt prezentacyjny audytu / preflightu searcha:
- pokazanie liczby legalnych kandydatów,
- pokazanie rozmiaru wejściowej przestrzeni statów,
- przy włączonym trybie biblioteki pokazanie rozmiaru przestrzeni biblioteki itemów,
- pokazanie rozmiaru przestrzeni skilli,
- pokazanie rozmiaru przestrzeni action bara,
- pokazanie skali `mała`, `średnia` albo `duża` według jawnych progów kontraktowych.

Minimalny kontrakt prezentacyjny listy wyników searcha:
- pokazanie wejściowej przestrzeni searcha,
- pokazanie liczby ocenionych kandydatów,
- pokazanie liczby wyników po normalizacji,
- pokazanie top `N` wyników po normalizacji,
- dla każdego wyniku pokazanie opisu wejściowego buildu,
- dla każdego wyniku pokazanie skilli na action barze i samego action bara,
- dla każdego wyniku pokazanie stanu trybu biblioteki itemów,
- dla każdego wyniku pokazanie wybranych itemów z biblioteki per slot i ich łącznego wkładu,
- dla każdego wyniku pokazanie `Łączne obrażenia` i `DPS`,
- dla każdego wyniku pokazanie akcji przejścia do szczegółów reprezentanta.

Kontrakt prezentacyjny drill-downu searcha:
- drill-down jednoznacznie wskazuje wybrany wynik po normalizacji,
- drill-down pokazuje `Wejście buildu`, `Skille na pasku`, `Pasek akcji`, `Łączne obrażenia` oraz `DPS`,
- drill-down pokazuje `Tryb biblioteki itemów`, `Wybrane itemy z biblioteki` oraz `Łączny wkład itemów`,
- drill-down pokazuje `Debug bezpośrednich trafień`, `Debug opóźnionych trafień`, `Debug obrażeń reaktywnych` oraz `Ślad kroków symulacji`,
- drill-down pokazuje końcowe stany `Judgement`, `Resolve`, `Końcowa szansa bloku` oraz `Końcowy bonus do kolców`,
- drill-down używa dokładnie tego samego runtime i tych samych modeli wynikowych co `Policz aktualny build`,
- drill-down nie zmienia warstwy backendowego searcha i nie przelicza listy wyników alternatywną logiką.

Kontrakt progresu CLI searcha:
- CLI pokazuje start searcha,
- CLI pokazuje postęp ocenionych kandydatów w trakcie oceny,
- CLI pokazuje zakończenie searcha,
- progress nie zmienia kolejności, rankingu ani logiki runtime.

Minimalne GUI searcha, audit oraz drill-down są częścią aktualnego zakresu. Poza zakresem pozostają live progress GUI, CSV, bogatsza warstwa UX i dodatkowe operacje na wynikach ponad prosty render SSR.

### 10.7. Trace i formatowanie
Aktualny foundation implementuje `stepTrace` w modelu danych i udostępnia go przez CLI oraz webowe GUI.

Kontrakt prezentacyjny trace:
- trace pokazuje tick po ticku tę samą symulację, która liczy wynik końcowy,
- dla każdego kroku pokazuje akcję, delayed damage, reactive damage, direct damage, step damage i cumulative damage,
- dla każdego kroku pokazuje kontraktową kolejność `delayed -> reactive -> active cast`,
- dla każdego kroku pokazuje stan skilli z paska potrzebny do walidacji `LRU`,
- dla każdego kroku pokazuje co najmniej `cooldown=true/false` oraz `cooldownRemaining`,
- CSV i pełny docelowy UX pozostają poza aktualnym zakresem repo.

### 10.8. Smoke testy użytkownika
Aktualny smoke test app shell obejmuje ekran główny, globalną nawigację i placeholdery przyszłych sekcji.

Smoke test ekranu głównego:

```text
http://127.0.0.1:8080/
```

Kontrakt prezentacji dla smoke testu ekranu głównego:
- ekran główny działa jako hub aplikacji i renderuje się pod `/`,
- ekran główny pokazuje grupy `Narzędzia builda`, `Itemy i import` oraz `Systemy dodatku i przyszłe sekcje`,
- ekran główny pokazuje status modułów i odróżnia moduły dostępne od placeholderów,
- ekran główny korzysta z tego samego centralnego rejestru modułów co globalna nawigacja i routing placeholder pages,
- ekran główny nie obiecuje szczegółowej mechaniki nowych systemów dodatku.

Smoke test placeholdera przyszłej sekcji:

```text
http://127.0.0.1:8080/medalion
```

Kontrakt prezentacji dla smoke testu placeholdera:
- placeholder ma własny SSR URL i własny tytuł strony,
- placeholder pokazuje nazwę sekcji, grupę, status i krótki opis produktowy,
- placeholder jasno komunikuje odłożenie szczegółowej logiki do czasu stabilizacji zasad po premierze dodatku,
- placeholder nie implementuje obliczeń, nowych formuł ani alternatywnego runtime.

Aktualny podstawowy smoke test manual simulation pozostaje oparty o GUI oraz równoległe CLI i scenariusz:
- `Advance`
- `rank 5`
- bazowe rozszerzenie włączone
- dodatkowy modyfikator `Flash of the Blade`
- horyzont `10 s`
- pomocniczy scenariusz referencyjny GUI/CLI z active reactive foundation:
  - `+50 THORNS`
  - `+50% BLOCK_CHANCE`
  - `+50% RETRIBUTION_CHANCE`

Uruchomienie w Windows PowerShell:

```powershell
chcp 65001
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd' '-Dmaven.repo.local=.m2' test
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd' '-Dmaven.repo.local=.m2' compile
java '-Dfile.encoding=UTF-8' -cp target/classes krys.web.CurrentBuildWebServer --port 8080
```

Bez `--host` i `--address` serwer binduje się domyślnie do `127.0.0.1`, czyli pozostaje lokalny dla komputera uruchamiającego aplikację. Równoważny lokalny tryb:

```powershell
java "-Dfile.encoding=UTF-8" -cp target/classes krys.web.CurrentBuildWebServer --port 8080
```

Tryb LAN wymaga jawnego hosta:

```powershell
java "-Dfile.encoding=UTF-8" -cp target/classes krys.web.CurrentBuildWebServer --host 0.0.0.0 --port 8080
```

`--address` jest aliasem `--host`:

```powershell
java "-Dfile.encoding=UTF-8" -cp target/classes krys.web.CurrentBuildWebServer --address 0.0.0.0 --port 8080
```

Adres IP komputera w sieci lokalnej można sprawdzić przez:

```powershell
ipconfig
```

Przykładowy wpis:

```text
IPv4 Address: 192.168.1.51
```

Adres z drugiego urządzenia w tej samej sieci:

```text
http://192.168.1.51:8080/
```

Bind można sprawdzić przez:

```powershell
netstat -ano | findstr :8080
```

Oczekiwany bind lokalny:

```text
127.0.0.1:8080 LISTENING
```

Oczekiwany bind LAN:

```text
0.0.0.0:8080 LISTENING
```

Jeżeli Windows Firewall blokuje wejście z LAN, regułę można dodać ręcznie w terminalu uruchomionym jako Administrator:

```powershell
netsh advfirewall firewall add rule name="Diablo DPS Engine 8080" dir=in action=allow protocol=TCP localport=8080
```

Nie wystawiaj aplikacji do internetu, nie ustawiaj port forwarding na routerze i używaj `--host 0.0.0.0` tylko w zaufanej sieci lokalnej. Aplikacja nie otwiera firewalla automatycznie.

Następnie otwórz w przeglądarce:

```text
http://127.0.0.1:8080/policz-aktualny-build
```

Równoległy smoke test CLI pozostaje dostępny:

```powershell
java '-Dfile.encoding=UTF-8' -cp target/classes krys.app.CalculateCurrentBuildCli --advance-rank 5 --advance-base-upgrade true --advance-choice RIGHT --action-bar ADVANCE --seconds 10 --show-trace true
```

Kontrakt prezentacji dla tego smoke testu:
- GUI jest po polsku i jasno komunikuje, że to aktualny foundation manual simulation, a nie pełny produkt końcowy.
- GUI pokazuje globalną nawigację SSR prowadzącą do ekranu głównego i głównych modułów aplikacji.
- GUI wymaga aktywnego bohatera i jasno komunikuje empty state, jeżeli bohater nie został jeszcze utworzony.
- GUI pokazuje, dla którego bohatera pracujemy, pozwala inline zmienić aktywnego bohatera, a poziom zapisuje tylko w sekcji `Punkty umiejętności`.
- GUI pozwala ustawić konfigurację wyłącznie przypisanych umiejętności bohatera oraz jego pasek akcji, a zapis wykonuje sticky przycisk `Zapisz zmiany`.
- GUI pozwala z tego samego formularza przejść do importu pojedynczego itemu ze screena z zachowaniem aktualnego kontekstu current build.
- GUI rozdziela warstwy w domyślnie zwiniętych sekcjach: `Aktywny bohater`, `Punkty umiejętności`, `Umiejętności bohatera`, `Pasek akcji`, `Statystyki bohatera`, `Ekwipunek aktualnego buildu`, `Wynik symulacji` i `Debug symulacji`.
- GUI nie renderuje osobnej sekcji `Centrum buildu`, dużego hero nagłówka strony, sekcji `Szczegóły użytych itemów` ani sekcji `Zaawansowane ręczne nadpisanie statów`.
- GUI pokazuje jedną użytkową sekcję `Statystyki bohatera`; jej wartości pochodzą z jawnych źródeł: klasy, poziomu bohatera, aktywnych itemów oraz zweryfikowanych baseline'ów prezentacyjnych. Legacy manual defaults current build nie są statystykami bohatera.
- W `Statystyki bohatera` atrybuty `Siła`, `Inteligencja`, `Siła woli` i `Zręczność` są prezentowane razem w grupie `Główne`; informacja o baseline Paladyna poziom `70` bez itemów pozostaje kontraktem danych, ale nie jest widocznym akapitem w UI.
- Zweryfikowany baseline prezentacyjny istnieje obecnie dla `Paladyn`, poziom `70`, bez itemów: siła `79`, inteligencja `76`, siła woli `76`, zręczność `77`, wytrzymałość `1610`, pancerz `158`, maksimum zdrowia `1526`, podstawowe obrażenia od broni `0`, szybkość broni `1,00`, szansa na trafienie krytyczne `5,2%`, obrażenia od trafień krytycznych `50,0%`, obrażenia zadawane odsłoniętym celom `20,0%` i ciernie `0`.
- Pancerz w `Statystyki bohatera` jest modelem prezentacyjnym z rozbiciem na `z siły`, `z itemów/głównego wyposażenia`, `z innych źródeł` i `łącznie`. Dla zweryfikowanego Paladyna poziom `70` bez itemów tooltip gry potwierdza `79 * 2 = 158` pancerza z siły, `0` z itemów/głównego wyposażenia, `0` z innych źródeł i `158` łącznie. Obecny model aktywnego itemu nie ma jawnego pola `ARMOR`, więc wkład pancerza z itemów pozostaje `0` i nie jest zgadywany.
- Szansa na trafienie krytyczne w `Statystyki bohatera` jest modelem prezentacyjnym z rozbiciem baseline: bazowo `5,0%`, `+0,2%` z Inteligencji dla zweryfikowanego baseline'u `76` Inteligencji, `+0,0%` z itemów, `+0,0%` z innych źródeł i `5,2%` łącznie. Pełny wzór kryta z Inteligencji nie jest jeszcze potwierdzony i nie jest implementowany; brak baseline'u nie jest interpolowany. Obecny model itemu nie ma jawnego `CRIT_CHANCE`, a `CRIT_DAMAGE` nie jest `CRIT_CHANCE` i pozostaje osobną statystyką obrażeń krytycznych.
- Rozbicie pancerza i szansy krytycznej jest dostępne w `title`/`aria-label` kafelków, bez długiego technicznego akapitu w głównym widoku. Ta zmiana nie zmienia `DamageEngine`, nie odblokowuje runtime DPS i nie implementuje `effectiveRank`.
- Odporności w sekcji `Statystyki bohatera` są rozdzielone na typy, bez zbiorczego kafelka: fizyczne, ogień, błyskawice, zimno, trucizna i cień. Dla baseline'u Paladyna poziom `70` bez itemów każda z tych odporności wynosi `30`.
- Brakujące statystyki dla poziomów bez jawnego baseline'u nie są interpolowane z poziomu `70`; UI pokazuje tylko statystyki z jawną formułą albo z aktywnych itemów i komunikuje brak baseline'u.
- GUI pokazuje pełny stały layout slotów bohatera: `Hełm`, `Zbroja`, `Rękawice`, `Spodnie`, `Buty`, `Broń`, `Amulet`, `Pierścień 1`, `Pierścień 2`, `Tarcza`.
- GUI pokazuje wspierane sloty ekwipunku, aktywny item albo pusty slot, skrót wkładu itemu, status aktywności oraz akcje `Wybierz z biblioteki`, `Importuj nowy item`, `Zmień item` i `Wyczyść slot` dla aktywnych slotów tego bohatera.
- GUI pokazuje sekcję `Umiejętności bohatera`, pozwala dodać albo usunąć przypisaną umiejętność i nie renderuje bezwarunkowo wszystkich skilli foundation.
- GUI w tej sekcji rozdziela aktualne dane przypisanej umiejętności od `Konfiguracja runtime legacy`; dla `SkillId.CLASH` prezentuje nazwę `Starcie`, aktualną rangę, kategorie, jawny procent obrażeń dla tej rangi, Lucky Hit i bazowe generowanie Wiary, a pełny katalog R1/max i modyfikatorów pozostaje w `/ranking-obrazen`.
- GUI pokazuje sekcję `Punkty umiejętności`, w której można edytować poziom bohatera `1..70`, dodatkowe punkty z zadań `0..14` oraz zobaczyć punkty dostępne, wydane i pozostałe; ranga kupowana punktami przy przypisanej umiejętności ma zakres `0..15`, itemowe bonusy do poziomu/rangi umiejętności są osobną przyszłą warstwą, a błędny zakres albo przekroczenie budżetu blokuje zapis profilu bez automatycznego usuwania wyborów.
- GUI current build ma szeroki layout i sticky pasek akcji formularza; `Zapisz zmiany` zapisuje główne pola edycji aktualnego buildu, a `Wycofaj zmiany` wraca do ostatniego zapisanego stanu bez usuwania profilu i przypisanych umiejętności.
- GUI ogranicza sześciomiejscowy pasek akcji do przypisanych i nauczonych umiejętności aktywnego bohatera; nielegalne wpisy blokują zapis profilu.
- GUI i główne ekrany SSR korzystają z szerszego kontenera layoutu, dzięki czemu lepiej wykorzystują szerokie monitory bez rozwalania mobilnego układu.
- Techniczne effective stats użyte do obliczeń pozostają częścią końcowego, domyślnie zwiniętego `Debug symulacji` na tym samym pipeline `effective stats -> CurrentBuildRequest -> CurrentBuildSnapshotFactory -> runtime`; nie są równorzędną sekcją użytkową obok `Statystyki bohatera`.
- GUI i CLI przechodzą przez ten sam kontrakt `CurrentBuildRequest -> CurrentBuildSnapshotFactory -> CurrentBuildCalculationService -> runtime`.
- scenariusze referencyjne są trybem pomocniczym do smoke testów i regresji, a nie główną ścieżką produktu.
- GUI i CLI pokazują `Łączne obrażenia`, `DPS`, debug bezpośredniego hita dla użytego skilla, debug opóźnionych trafień, debug obrażeń reaktywnych, `Ślad kroków symulacji`, `Resolve aktywny na końcu`, `Końcowa szansa bloku` oraz `Końcowy bonus do kolców`.
- GUI i CLI pokazują `Kolce surowe`, `Kolce końcowe`, `Retribution oczekiwane surowe`, `Retribution oczekiwane końcowe`, `Końcowe reaktywne` oraz `Wkład obrażeń reaktywnych`.
- GUI i CLI pokazują naturalne `WAIT`, `odnowienie=tak/nie` oraz `pozostałe odnowienie` dla scenariusza `Advance + Flash of the Blade`.
- CLI pokazuje użytkową nazwę skilla, a nie techniczny enum.
- Output powinien być czytelny w UTF-8; w Windows wymagane jest uruchomienie konsoli po `chcp 65001`.
- Dla referencyjnego scenariusza GUI/CLI `Advance rank 5 + Flash of the Blade` na sample buildzie wynik manual simulation wynosi `total damage = 186`, `DPS = 18.6000`, `total reactive damage = 120`, dwa casty `Advance` w `t=1` i `t=9`, naturalne `WAIT` w `t=2..8`, `cooldownRemaining=7` w `t=2` oraz `cooldownRemaining=1` w `t=8`.
- Dla powyższego sample buildu pojedynczy cast `Advance + Flash of the Blade` daje `raw = 54`, `final = 33`, `raw crit = 82`, `crit = 51`.
- Regresyjny scenariusz `Clash rank 5 + Crusader's March + Punishment` pozostaje dodatkowym smoke testem niższego poziomu dla reactive foundation.

Aktualny smoke test backendowego searcha obejmuje CLI, minimalne GUI SSR, audit/preflight oraz drill-down pojedynczego wyniku.

Smoke test GUI searcha:

```text
http://127.0.0.1:8080/znajdz-najlepszy-build
```

Kontrakt prezentacji dla smoke testu GUI searcha:
- GUI searcha jest po polsku i jasno komunikuje, że to minimalny SSR nad istniejącym backendem searcha,
- GUI searcha pokazuje globalną nawigację SSR wspólną z ekranem głównym i pozostałymi głównymi modułami,
- GUI searcha wymaga aktywnego bohatera i pokazuje jego kontekst nad formularzem,
- GUI searcha pozwala ustawić poziom, obrażenia broni, siłę, inteligencję, kolce, szansę bloku, szansę retribution, zakresy skilli foundation, rozmiary action bara, limit wyników, horyzont symulacji oraz opcjonalny tryb biblioteki itemów,
- GUI searcha przechodzi przez kontrakt `SearchBuildFormMapper -> BuildSearchRequest -> BuildSearchCalculationService -> BuildSearchPresentationNormalizer`,
- GUI searcha wyraźnie eksponuje `Tryb biblioteki itemów` jako osobną decyzję produktową w formularzu,
- GUI searcha pokazuje audit / preflight searcha obok wyniku,
- GUI searcha pokazuje `Liczba legalnych kandydatów`, `Rozmiar przestrzeni statów`, opcjonalnie `Rozmiar przestrzeni biblioteki itemów`, `Rozmiar przestrzeni skilli`, `Rozmiar przestrzeni paska akcji` oraz `Skala przestrzeni searcha`,
- GUI searcha pokazuje wejściową przestrzeń searcha,
- GUI searcha pokazuje `Ocenieni kandydaci`, `Wyniki po normalizacji`, `Najlepsze wyniki po normalizacji`, `Wejście buildu`, `Skille na pasku`, `Pasek akcji`, `Tryb biblioteki itemów`, `Wybrane itemy z biblioteki`, `Łączny wkład itemów`, `Łączne obrażenia` oraz `DPS`,
- GUI searcha pozwala z listy wyników przejść do szczegółów reprezentanta przez osobny SSR drill-down,
- drill-down przechodzi przez kontrakt `CurrentBuildRequest -> CurrentBuildSnapshotFactory -> CurrentBuildCalculationService -> runtime`,
- drill-down pokazuje `Wejście buildu`, `Skille na pasku`, `Pasek akcji`, `Tryb biblioteki itemów`, `Wybrane itemy z biblioteki`, `Łączny wkład itemów`, `Łączne obrażenia`, `DPS`, `Debug bezpośrednich trafień`, `Debug opóźnionych trafień`, `Debug obrażeń reaktywnych`, `Ślad kroków symulacji`, `Judgement aktywny na końcu`, `Resolve aktywny na końcu`, `Końcowa szansa bloku` oraz `Końcowy bonus do kolców`,
- GUI searcha nie implementuje live progressu, CSV, wielowątkowości ani rozbudowanego UX ponad minimalny SSR.

Smoke test GUI rankingu obrażeń:

```text
http://127.0.0.1:8080/ranking-obrazen?character=paladin
```

Kontrakt prezentacji dla smoke testu rankingu Paladyna:
- endpoint `/ranking-obrazen?character=paladin` renderuje 24 wiersze z `PaladinSkillTreeRegistry`,
- endpoint `/ranking-obrazen-paladyna` działa tylko jako alias kompatybilności wstecznej dla tego samego widoku Paladyna,
- ekran pokazuje globalną nawigację SSR wspólną z pozostałymi modułami,
- ekran nie pokazuje legacy skilli `Brandish`, `Holy Bolt`, `Clash` ani `Advance` jako domyślnego drzewa Paladyna,
- ekran pokazuje kolumny `Obrażenia % R1` i `Obrażenia % max drzewo`; brak jawnej wartości źródłowej jest renderowany jako `nie dotyczy` albo `wymaga weryfikacji`, a nie jako `0%` ani `zablokowane`,
- ekran pokazuje `NEEDS_VERIFICATION`, `UNSUPPORTED` i `NON_DAMAGE` bez wartości DPS, jeżeli wpis nie jest zweryfikowany albo nie jest skillem obrażeniowym,
- ekran ma filtry `character`, `skillGroup`, `sourceCategory`, `verificationStatus` oraz filtry faceted dla wpływu ulepszeń; UI nie renderuje filtrów `type`, `tag` ani `hasCooldownOrCastSpeed`,
- ekran nie renderuje filtra `Metryka rankingu`; porządek tabeli zmienia się przez sortowalne nagłówki z parametrami `sort` i `direction`,
- ekran renderuje kolumny `Kategorie z gry`, `Koszt Wiary`, `Generowanie Wiary`, `Dmg multiplier`, `Dmg bonus`, `Extra hit / component`, `Damage over time`, `Status / debuff`, `Defense / utility` i `Manual review` zamiast technicznych kolumn `Grupa drzewa`, `tags`, `type`, `Speed / cooldown` oraz szerokich kolumn `grupa_1`, `grupa_2`, `grupa_3`,
- ekran pokazuje w kolumnie `Kategorie z gry` wyłącznie kategorie źródłowe z gry/PDF/Markdown, a mechaniczne tagi porównywarki pozostawia poza kolumną `tags`,
- ekran nie implementuje nowego runtime DPS i nie odblokowuje żadnej mechaniki bez weryfikacji single target.

Smoke test CLI searcha:

```powershell
java '-Dfile.encoding=UTF-8' -cp target/classes krys.search.SearchBuildCli --reference FOUNDATION_M9 --top 5
```

Kontrakt prezentacji dla smoke testu searcha:
- search CLI jasno komunikuje, że to backend foundation searcha, a nie GUI search,
- search CLI wypisuje audit / preflight searcha jeszcze przed top wynikami,
- search CLI wypisuje start searcha, postęp ocenionych kandydatów i zakończenie,
- search CLI wypisuje wejściową przestrzeń searcha,
- search CLI wypisuje `Liczba legalnych kandydatów`, `Rozmiar przestrzeni statów`, opcjonalnie `Rozmiar przestrzeni biblioteki itemów`, `Rozmiar przestrzeni skilli`, `Rozmiar przestrzeni action bara` oraz `Skala search space`,
- search CLI wypisuje liczbę ocenionych kandydatów,
- search CLI wypisuje liczbę wyników po normalizacji,
- search CLI wypisuje top `N` wyników po normalizacji z opisem buildu, skillami na action barze, action barem, stanem trybu biblioteki itemów, wybranymi itemami biblioteki i ich łącznym wkładem,
- search CLI wypisuje `total damage` oraz `DPS`,
- search CLI przechodzi przez kontrakt `BuildSearchRequest -> BuildSearchCandidateGenerator -> CurrentBuildRequest -> CurrentBuildSnapshotFactory -> BuildSearchEvaluationService -> ManualSimulationService -> BuildSearchPresentationNormalizer`,
- dla referencyjnego smoke testu `FOUNDATION_M9 --top 5` search CLI daje `Ocenieni kandydaci = 2949`, `Wyniki po normalizacji = 137` oraz top 1 `total damage = 439`, `DPS = 48.7778`, `Action bar = Advance -> Clash`.

Smoke test GUI importu itemu:

```text
http://127.0.0.1:8080/importuj-item-ze-screena
```

Kontrakt prezentacji dla smoke testu importu itemu:
- GUI importu jest po polsku i jasno komunikuje, że to import wspomagany pojedynczego itemu, a nie pełny automatyczny import całej postaci,
- GUI importu pokazuje globalną nawigację SSR wspólną z ekranem głównym i pozostałymi głównymi modułami,
- GUI importu wymaga aktywnego bohatera i w empty state kieruje użytkownika do modułu `Bohaterowie`,
- GUI przyjmuje upload obrazu pojedynczego itemu przez `multipart/form-data`,
- GUI waliduje technicznie, czy upload jest prawidłowym obrazem,
- GUI przed pierwszym uploadem pokazuje sensowny empty state sekcji `Wstępnie rozpoznane pola`,
- GUI wykonuje preprocessing i realny OCR kilku wariantów pojedynczego screena, a następnie pokazuje metadane obrazu oraz produktowy odczyt itemu bez domyślnej tabeli technicznych kandydatów OCR,
- GUI pokazuje sekcję `Pełny odczyt widocznego itemu` jako czytelny rekord itemu z nagłówkiem, liniami bazowymi / implicit, aspektem / efektem legendarnym, dodatkowymi liniami i socketem / gniazdem; affixy nie są renderowane w tej sekcji i są edytowane wyłącznie w tabeli ręcznej weryfikacji affixów,
- GUI pokazuje ręczny formularz zatwierdzenia z edytowalną listą affixów opartą o `ApplicationAffixRegistry`: użytkownik może zmienić typ i wartość affixu, oznaczyć `Greater Affix`, usunąć wiersz z tabeli albo dodać brakujący affix z katalogu przez akcję `Dodaj affix` bez przeładowania strony; tabela affixów nie pokazuje kolumny OCR/source, chociaż źródło może pozostać w modelu, ukrytych polach albo debug danych,
- GUI pokazuje wybór aspektu jako listę czystych nazw znanych aspektów zgodnych ze slotem itemu oraz opis efektu aspektu z `AspectRegistry`, bez widocznych technicznych opisów `Aspekt unikatowy`, `Typ`, `Status runtime`, `Sugestia OCR`, `Pewność OCR sugestii` i `Odczyt OCR efektu` dla dopasowanego Odłamka Verathiela,
- GUI nie kopiuje raw OCR effect do registry: opis aspektu jest ogólny i nie zawiera zgadywanych procentów rolla,
- jeśli OCR wykrył tekst aspektu, ale `AspectRegistry` nie zna dopasowania, GUI pokazuje komunikat o braku dopasowania w katalogu i zostawia finalny wybór aspektu pusty; rozpoznany efekt Odłamka Verathiela jest dopasowywany jako aspekt unikatowy, więc tego komunikatu nie pokazuje,
- ekran importu nie renderuje sekcji `Projekcja do aktualnego runtime`; pola runtime pozostają warstwą techniczną za biblioteką itemów, a nie częścią korekty OCR,
- kliknięcie `Dodaj affix` w głównym flow JS nie zapisuje itemu do biblioteki i nie wykonuje POST; bez JS działa fallback `<noscript>` wysyłający `formAction=addAffix`, który odświeża formularz bez zapisu itemu,
- zatwierdzony item jest mapowany do aktualnego modelu `Item`, do agregowanych pól current build oraz automatycznie zapisywany jako `SavedImportedItem` z pełnym odczytem `FullItemRead` i listą affixów w bibliotece; dla Odłamka Verathiela legacy `weaponDamage` pozostaje `0`, bo import strukturalny rozdziela `weaponDps`, min/max obrażeń za trafienie, średnią obrażeń trafienia i szybkość ataku,
- po zatwierdzeniu itemu baza wiedzy zapisuje obserwacje typów affixów i aspektów dla typu itemu, ale nie zmienia konkretnego itemu użytkownika,
- GUI po zatwierdzeniu itemu pokazuje, dla jakiego aktywnego bohatera pracujemy,
- GUI po zatwierdzeniu itemu pokazuje nazwę itemu / plik źródłowy, slot, identyfikator biblioteki, wkład oraz akcje `Załóż bohaterowi`, `Przejdź do biblioteki` i `Wróć do aktualnego buildu`,
- GUI po zatwierdzeniu pokazuje `Pełny odczyt zapisany w bibliotece` jako podgląd itemu; dla broni rozdziela DPS, min/max obrażeń za trafienie, średnią obrażeń trafienia i szybkość ataku,
- import Odłamka Verathiela rozdziela `weaponDps = 1830`, `weaponDamageMin = 1350`, `weaponDamageMax = 1978`, `averageWeaponDamage = 1664` i `attacksPerSecond = 1.10`; średnia jest liczona wyłącznie z jawnego min/max, nigdy z DPS i attack speed,
- ręczne potwierdzenie itemu prefilluje pola broni z danych strukturalnych OCR/mergera: `Obrażenia za trafienie min = 1350`, `Obrażenia za trafienie max = 1978` i `Średnie obrażenia trafienia = 1664`,
- katalog affixów zawiera dla Odłamka Verathiela 4 opisowe wpisy: `Obrażenia od broni` z rollem `94 [94 - 157]`, `Maksymalne zdrowie` z rollem `2141 [1831 - 2200]`, `Zdrowie przy trafieniu` z rollem `545 [526 - 632]` oraz `Szczęśliwy traf: odzyskanie podstawowego zasobu` z wartością opisową `15% / +3` i zakresem `3 - 4`,
- affixy Odłamka Verathiela są deduplikowane po dopasowaniu katalogowym, wartości i fladze `Greater Affix`; gdy warianty OCR różnią się jakością zakresu, zachowywany jest pełniejszy wpis zgodny z katalogowym zakresem,
- `+94 obrażeń od broni` pozostaje affixem opisowym i nie jest sumowane z `averageWeaponDamage`; aspekt unikatowy Odłamka Verathiela pozostaje w `AspectRegistry` jako `UNIQUE`, nie jest affixem i pozostaje nieaktywny w runtime DPS,
- import Odłamka Verathiela nie podłącza itemu do runtime DPS, nie implementuje `effectiveRank` i nie zmienia `DamageEngine`,
- flow nie obiecuje pełnej bezbłędności OCR i wymaga ręcznego potwierdzenia użytkownika przed użyciem danych,
- poza zakresem pozostają pełny wielo-itemowy workflow i pełny OCR całej postaci.

Smoke test GUI biblioteki itemów:

```text
http://127.0.0.1:8080/biblioteka-itemow
```

Kontrakt prezentacji dla smoke testu biblioteki itemów:
- GUI biblioteki jest czystym widokiem zapisanych itemów i nie pokazuje raw OCR effect ani komunikatów OCR-weryfikacyjnych w głównej tabeli,
- główny widok biblioteki jest kompaktową tabelą z kolumnami `Item`, `Slot / typ`, `Aspekt`, `Affixy` i `Akcje`,
- `Greater Affix` jest widoczny przy konkretnym affixie w skrócie affixów, a nie jako osobna kolumna `GA`,
- affixy w kolumnie `Affixy` są renderowane pionowo, jeden pod drugim,
- nazwa itemu otwiera modal szczegółów; kolumna `Akcje` nie ma osobnego przycisku `Szczegóły`,
- nazwa aspektu ma tooltip / `title` z opisem efektu z registry,
- status użycia jest widoczny jako badge `Założony` przy nazwie itemu, a nie jako osobna kolumna `Status`; źródło itemu nie jest osobną kolumną i może pojawić się w szczegółach,
- sekcja `Aspekt / efekt legendarny` pokazuje finalny aspekt z `selectedAspectId` przez nazwę z `AspectRegistry`, typ / etykietę aspektu i opis efektu z registry; jeśli aspekt nie jest wybrany, pokazuje `Brak wybranego aspektu.`,
- pusta `Diagnostyka OCR` nie jest renderowana; diagnostyka może pojawić się tylko jako osobny szczegół techniczny, gdy są zapisane faktyczne linie diagnostyczne,
- modal szczegółów pokazuje pełne semantyczne sekcje itemu bez wracania do ciężkich kart jako głównej listy,
- każdy item pokazuje w jednej poziomej grupie kompaktowe ikony `Załóż / Zmień w slocie`, `Edytuj` i `Usuń`; wszystkie używają wspólnego rozmiaru i mają `aria-label`,
- ikona `Załóż / Zmień w slocie` ma styl primary / niebieski, `Edytuj` neutralny, a `Usuń` danger / czerwony,
- zapis edycji aktualizuje istniejący item po tym samym `itemId`, więc aktywny item pozostaje założony i zaczyna działać z nowymi danymi przez tę samą selekcję,
- panel filtrów obsługuje `q`, `slot`, `type`, `status`, `aspect`, `affix` i `greater=true`, pokazuje liczbę wyników oraz zachowuje wybrane wartości po filtrowaniu,
- filtry działają na danych strukturalnych zapisanego itemu, a nie na płaskim dumpie OCR,
- `Wyczyść filtry` wraca do pełnej listy zapisanych itemów.

Smoke test GUI bazy wiedzy itemów:

```text
http://127.0.0.1:8080/baza-wiedzy-itemow
```

Kontrakt prezentacji dla smoke testu bazy wiedzy itemów:
- GUI bazy wiedzy jest po polsku i jasno komunikuje, że to osobna warstwa obserwacji, a nie biblioteka konkretnych itemów ani runtime,
- GUI pokazuje aktywną epokę wiedzy oraz liczniki wpisów, zatwierdzonych obserwacji itemów, obserwacji affixów i obserwacji aspektów,
- GUI pokazuje obserwacje pogrupowane po typie itemu i slocie,
- GUI pokazuje zaobserwowane typy affixów i aspekty / efekty specjalne z licznikami wystąpień,
- GUI pozwala rozpocząć nową epokę wiedzy i wyczyścić aktywne obserwacje,
- pusty stan prowadzi użytkownika do importu itemu,
- GUI bazy wiedzy nie stosuje sugestii automatycznie i nie zmienia zapisanych itemów użytkownika.

Smoke test GUI biblioteki itemów:

```text
http://127.0.0.1:8080/biblioteka-itemow
```

Kontrakt prezentacji dla smoke testu biblioteki itemów:
- GUI biblioteki jest po polsku i jasno komunikuje, że to przegląd zapisanych itemów nad current build,
- GUI biblioteki pokazuje globalną nawigację SSR wspólną z ekranem głównym i pozostałymi głównymi modułami,
- GUI biblioteki pokazuje aktywnego bohatera albo empty state bez bohatera,
- GUI biblioteki pokazuje zapisane itemy jako kompaktowy indeks tabelaryczny z kolumnami `Item`, `Slot / typ`, `Aspekt`, `Affixy` i `Akcje`,
- GUI biblioteki otwiera modal szczegółów po kliknięciu nazwy itemu, jeżeli item ma zapisane linie `FullItemRead`,
- GUI biblioteki pozwala mieć wiele itemów tego samego slotu,
- GUI biblioteki oznacza item używany przez aktywnego bohatera badge'em `Założony` przy nazwie itemu, bez osobnej kolumny statusu,
- GUI biblioteki pokazuje kompaktową ikonę `Załóż bohaterowi` dla zgodnego pustego slotu oraz kompaktową ikonę `Zmień w slocie` dla zgodnego slotu, który ma już item aktywnego bohatera,
- GUI biblioteki nie pokazuje akcji `Pokaż slot w current build` w wierszach tabeli,
- ustawienie nowego aktywnego itemu w bibliotece zastępuje poprzedni aktywny wybór tylko w tym samym slocie aktywnego bohatera,
- pusty stan biblioteki zawiera krótki komunikat SSR oraz bezpośredni link do importu itemu,
- pusty stan bez aktywnego bohatera prowadzi użytkownika do modułu `Bohaterowie`,
- aktywny item z biblioteki trafia do effective current build dopiero przez istniejący pipeline current build,
- GUI biblioteki działa na lokalnym trwałym storage użytkownika poza `target/`, więc restart aplikacji i nowy build widzą tę samą bibliotekę, chyba że ustawiono inne `dstats.dataDir`,
- GUI biblioteki zachowuje `currentBuildQuery` w flow `current build -> biblioteka itemów -> import kolejnego itemu -> powrót / zastosowanie do current build`,
- GUI biblioteki nie jest jeszcze pełnym inventory managerem ani stashem.

## 11. Testy i golden values
### 11.1. Reguły testowe
- Wszystkie testy muszą przechodzić w `100%`.
- Testy muszą być deterministyczne.
- Testy nie zgadują liczb.
- Zmiana logiki wymaga aktualizacji kodu, testów i README w tej samej zmianie.
- Każdy termin użyty w testach musi być spójny z terminologią README.
- Pęknięcie golden values oznacza zmianę zachowania engine, a nie kosmetyczną różnicę.

### 11.2. Obowiązkowe obszary pokrycia
Minimalny zakres testów obejmuje:
- pipeline `Damage Engine`,
- wzór main stat,
- wzór crit,
- redukcję obrażeń zależną od poziomu,
- reguły `REPLACE_BASE_DAMAGE`,
- reguły single target,
- golden values dla bazowego `Holy Bolt`,
- delayed hit `Judgement`,
- trigger time `Judgement`,
- brak stackowania i brak refreshu `Judgement`,
- enemy hit schedule `t=3`, `t=6`, `t=9`...,
- reactive foundation `Thorns`,
- deterministyczne `expected value` dla `Retribution`,
- bazowe działanie `Advance`,
- `Wave Dash` jako drugi direct-hit komponent na tym samym celu,
- `Flash of the Blade` jako połączenie `REPLACE_BASE_DAMAGE`, `APPLY_STATUS` i `SET_COOLDOWN`,
- cooldown `8 s` dla `Advance + Flash of the Blade`,
- naturalne `WAIT` wynikające z cooldownu,
- wpływ cooldownu na wybór `LRU`,
- podstawowe działanie `Clash` w manual simulation,
- `Crusader's March` jako źródło `Resolve` i `block chance`,
- `Punishment` jako źródło bonusu do `Thorns`,
- wpływ `Clash`, `Crusader's March` i `Punishment` na reactive damage,
- tick order `delayed hit -> reactive damage -> aktywny cast`,
- `WAIT` przy braku legalnego castu,
- wybór `LRU`,
- tie-break według kolejności na pasku,
- zgodność cumulative damage z `stepTrace`,
- tickową manual simulation,
- render ekranu głównego app shell pod `/`,
- render modułu `Bohaterowie`,
- utworzenie pierwszego bohatera,
- listę wielu bohaterów,
- ustawienie aktywnego bohatera,
- usunięcie bohatera,
- render empty state przy braku bohatera,
- obecność aktywnego bohatera w głównym UI,
- obecność grup modułów na ekranie głównym,
- obecność statusów modułów na ekranie głównym,
- obecność globalnej nawigacji na głównych stronach SSR,
- routing do istniejących sekcji po dodaniu app shell,
- render placeholder pages przyszłych sekcji,
- endpoint formularza GUI dla `Policz aktualny build`,
- current build w kontekście aktywnego bohatera,
- uruchomienie obliczenia przez GUI nad tym samym runtime,
- render sekcji `Ekwipunek aktualnego buildu`,
- render pełnego układu slotów bohatera, aktywnego itemu albo pustego slotu bez osobnej sekcji `Użyte itemy`,
- SSR zmianę aktywnego itemu per slot dla konkretnego bohatera bez zmiany runtime,
- niezależność aktywnych slotów między różnymi bohaterami,
- obecność kluczowych sekcji wyniku w GUI: `Łączne obrażenia`, `DPS`, debug bezpośrednich trafień, debug opóźnionych trafień, debug obrażeń reaktywnych i `Ślad kroków symulacji`,
- obecność sekcji reactive debug w GUI dla scenariusza `Clash`,
- obecność `WAIT` i stanu cooldownu w GUI dla scenariusza `Advance`,
- preprocessing obrazu itemu i przygotowanie kilku wariantów OCR, w tym dolnego cropu efektu legendarnego powiększonego pod odczyt wartości typu `11,0%[x]`,
- deterministyczne scalanie per pole wyników z kilku wariantów OCR,
- deduplikację stabilnych linii `FullItemRead` po scaleniu wariantów OCR,
- rozpoznanie ograniczonych pól foundation z pojedynczego screena itemu do `candidate parse result`,
- pełny odczyt widocznego itemu do `FullItemRead` niezależnie od mappingu foundation,
- zachowanie pełnych linii itemu w formularzu potwierdzenia i w zapisie biblioteki,
- odczyt `src/test/resources/items/tarcza.png` jako pełniejszego modelu itemu z nazwą `NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU`, typem, rzadkością, mocą, bazowym pancerzem `1 131 pkt. pancerza`, stabilnymi implicitami, stabilnymi affixami, efektem legendarnym z `11,0%[x]` i gniazdem; sezonowe linie, takie jak `Rozjuszenie`, mogą zostać zachowane jako dodatkowy OCR, ale nie są wymaganym kontraktem regresyjnym itemu,
- strukturalny podział `tarcza.png`: `1 131 pkt. pancerza` jako base stat, `45% redukcji blokowanych obrażeń`, `20,0% szansy na blok` i `+100% obrażeń od broni w głównej ręce` jako implicit / linie bazowe, a `+494 cierni`, `+7,0% szansy na szczęśliwy traf`, `13,2% redukcji czasu odnowienia` i `+114 siły` jako właściwe affixy,
- zachowanie zakresu `+114 siły [107 - 121]`, brak sklejania `+7,0% szansy na szczęśliwy traf [7,0` z `13,2% redukcji czasu odnowienia` oraz ostrożne oznaczanie `Greater Affix`: tylko jawne markery OCR `*`, `★`, `⭐` i `✦` ustawiają `greaterAffix=true`, a sam brak bracketowego zakresu rolla nie wystarcza do automatycznej gwiazdki,
- render biblioteki dla tarczy bez płaskich wpisów typu `Affix: 45% redukcji blokowanych obrażeń`, bez dublowania mocy przedmiotu i bazowej wartości jako dodatkowych bulletów oraz z osobnymi sekcjami semantycznymi,
- render biblioteki bez sklejki base stat `800 1 131 pkt. pancerza`, z prezentacyjną gwiazdką wyłącznie dla affixów `greaterAffix=true` oraz bez oczywiście zdublowanych/sklejonych wariantów raw OCR aspektu,
- prezentację aspektu: import i formularz edycji pokazują `selectedAspectId`, opis efektu z `AspectRegistry` oraz pomocniczy OCR effect, a normalny widok biblioteki pokazuje tylko finalny aspekt i opis z registry bez raw OCR effect,
- snapshot realnego outputu Windows OCR dla `src/test/resources/items/tarcza.png`, który zabezpiecza ścieżkę regresji `800 / 1 131`, nazwę, bazową wartość i efekt legendarny bez fake OCR readera jako głównej weryfikacji,
- mapowanie tekstu aspektu z `tarcza.png` do sugestii `ocrSuggestedAspectId`, bez zapisu dowolnego tekstu OCR jako finalnego aspektu itemu,
- walidację, że aspekt zgodny ze slotem itemu przechodzi, a aspekt spoza `allowedItemSlots` jest odrzucany,
- przepływ nieznanego aspektu OCR: brak dopasowania w `AspectRegistry` zostawia `selectedAspectId` pusty, pokazuje komunikat w UI i zachowuje raw OCR tylko jako pomocniczy `FullItemRead`,
- bazę wiedzy aspektów opartą o finalny `selectedAspectId`, bez tworzenia obserwacji aspektu z raw OCR, gdy użytkownik nie zatwierdził aspektu z katalogu,
- fallback SSR `<noscript>` dla `Dodaj affix` obok głównego flow JS bez przeładowania,
- czysty widok biblioteki bez pustej `Diagnostyka OCR` i bez komunikatów OCR-weryfikacyjnych w normalnym podglądzie itemu,
- kompaktowy tabelaryczny indeks biblioteki z kolumnami `Item`, `Slot / typ`, `Aspekt`, `Affixy` i `Akcje`, bez osobnych kolumn `GA`, `Status` i `Źródło`,
- modal `Szczegóły` otwierany z nazwy itemu, z pełnymi sekcjami semantycznymi, tooltipem opisu aspektu oraz prezentacyjną gwiazdką `Greater Affix` bezpośrednio przy affixie w pionowym skrócie affixów,
- jednolitą kolumnę `Akcje`, w której `Załóż / Zmień`, `Edytuj` i `Usuń` są kompaktowymi ikonami o wspólnym rozmiarze i dostępnych etykietach,
- edycję zapisanego itemu przez `/biblioteka-itemow/edytuj?itemId=<id>` z aktualizacją tego samego `itemId`, zachowaniem `greaterAffix`, `selectedAspectId` i wpływem na aktywnie założony item,
- filtry biblioteki po danych strukturalnych: `q`, slot, typ itemu, status użycia, aspekt, affix oraz `greater=true`,
- brak ukrytego fallbacku, który po usunięciu affixu przywraca `Siła = 114` albo inne wartości z OCR / foundation,
- edycję affixu tak, że finalny zapis zawiera nową wartość, a stara wartość z OCR nie wraca do biblioteki,
- usunięcie wszystkich affixów z formularza bez ponownego odzyskania ich z `FullItemRead`,
- walidację błędnych wierszy affixów: typ bez wartości, wartość bez typu, wartość nieparsowalna i wartość ujemna nie mogą być cicho pomijane,
- zachowanie `greaterAffix=true` przez formularz, zapis biblioteki i ponowny odczyt oraz rozpoznanie jawnych markerów OCR `*`, `★`, `⭐` i `✦`, bez zgadywania gwiazdki wyłącznie na podstawie brakującego bracketowego zakresu rolla,
- brak renderowania sekcji `Projekcja do aktualnego runtime` na ekranie importu,
- rozdzielenie `Typ itemu` i `Slot ekwipunku` w ręcznym potwierdzeniu oraz pokazanie aspektu w tej samej sekcji formularza,
- brak widocznych technicznych pól OCR aspektu (`Sugestia OCR`, `Pewność OCR sugestii`, `Odczyt OCR efektu`) dla dopasowanego katalogowego aspektu Odłamka Verathiela,
- brak widocznej kolumny `Odczyt OCR / źródło` i tekstu `Źródło: OCR` w głównej tabeli ręcznej weryfikacji affixów; źródło OCR może pozostać w modelu i persistencji,
- render pełnego odczytu itemu jako produktowego podglądu z osobnymi polami nagłówka, implicitami, efektem specjalnym, dodatkowymi liniami i socketem / gniazdem; affixy są dostępne w ręcznej weryfikacji affixów, a nie w sekcji `Pełny zapis itemu`,
- render edytowalnej listy affixów jako głównego modelu ręcznej walidacji importowanego itemu,
- projekcję aktualnych statów runtime z listy affixów z zachowaniem pełnych affixów nieobsługiwanych jeszcze przez runtime,
- brak wielokrotnego naliczania tego samego affixu pochodzącego z kilku wariantów OCR w ręcznej weryfikacji i projekcji runtime,
- odczyt `src/test/resources/items/buty.png` jako pełniejszego modelu itemu bez halucynowania nieobsługiwanych foundation statów oraz z rozpoznaniem wielu edytowalnych affixów bez projekcji do runtime,
- regresję `buty.png` przez ścieżkę draft -> formularz -> zapis -> reload biblioteki, z oddzieleniem base statów butów od affixów i z renderem tych samych sekcji semantycznych co dla tarczy,
- rozpoznanie polskich fraz foundation dla `Strength`, `Thorns` i `Block chance`,
- wybór realnego rolla zamiast liczby z zakresu referencyjnego dla linii typu `+114 do siły [107 - 121]`,
- rozpoznanie slotu dla co najmniej `buty` i `tarcza`,
- brak halucynacji dla nieobsługiwanych affixów OCR,
- mapowanie wstępnie rozpoznanych pól itemu do formularza ręcznego potwierdzenia,
- mapowanie rozpoznanych linii affixów do katalogu `ImportedItemAffixType`,
- walidację ręcznie poprawionego itemu,
- mapowanie zatwierdzonego itemu do aktualnego modelu `Item`,
- mapowanie zatwierdzonego itemu do aktualnego agregowanego modelu current build,
- aplikowanie zatwierdzonego itemu do current build w trybie `nadpisz`,
- aplikowanie zatwierdzonego itemu do current build w trybie `dodaj wkład`,
- zapis zatwierdzonego itemu do trwałej biblioteki,
- trwały zapis i odczyt listy affixów po ręcznej edycji w bibliotece itemów,
- trwały zapis i odczyt `greaterAffix` jako pola modelu affixu, niezależnie od gwiazdki użytej w prezentacji,
- trwały zapis i odczyt pełniejszego `FullItemRead` w bibliotece itemów,
- uczenie bazy wiedzy z zatwierdzonego itemu po ręcznej edycji affixów,
- trwały zapis i odczyt aktywnej epoki bazy wiedzy oraz liczników affixów i aspektów,
- reset bazy wiedzy przez rozpoczęcie nowej epoki,
- odczyt listy zapisanych itemów z biblioteki,
- wiele itemów tego samego slotu w bibliotece,
- aktywację jednego itemu per slot i zmianę aktywnego itemu z A na B,
- użycie `dstats.dataDir` do wskazania własnego katalogu danych biblioteki,
- domyślną lokalizację biblioteki poza `target/` z segmentem `dstats`,
- migrację legacy biblioteki z `target/item-library-runtime/`,
- zachowanie zapisanych itemów po migracji legacy storage,
- zachowanie `active selection` po migracji legacy storage,
- brak nadpisania nowej lokalizacji, gdy zawiera już własne dane biblioteki,
- agregację aktywnych itemów do effective current build,
- flow, w którym ręczna baza current build jest częściowo pusta albo zerowa, ale aktywne itemy dopełniają finalne effective stats przed `CurrentBuildRequest`,
- potwierdzenie, że effective current build nadal kończy się ścieżką `CurrentBuildRequest -> CurrentBuildSnapshotFactory -> runtime`,
- GET formularza GUI importu itemu,
- empty state importu bez aktywnego bohatera,
- empty state sekcji `Wstępnie rozpoznane pola`,
- upload obrazu itemu i render sekcji wstępnego rozpoznania,
- dodanie, usunięcie oraz zmiana typu i wartości affixu w flow potwierdzenia importu,
- regresję parsera, w której bazowy pancerz tarczy `1 131 pkt. pancerza` nie może zostać rozlany do pól siły, kolców ani szansy bloku,
- rozpoznanie realnych rolli tarczy `+114 siły`, `+494 cierni` i `+20,0% szansy na blok` mimo obecności bazowego pancerza,
- zatwierdzenie itemu z automatycznym zapisem do biblioteki,
- render potwierdzenia automatycznego zapisu itemu do biblioteki z akcjami dalszego flow,
- render pełnego odczytu itemu w GUI importu i w rozwijanych szczegółach GUI biblioteki,
- GET strony bazy wiedzy itemów,
- SSR statusu aktywnej epoki, liczników obserwacji i wpisów wiedzy per typ itemu,
- SSR resetu bazy wiedzy i pustego stanu nowej epoki,
- GET strony biblioteki itemów,
- empty state biblioteki bez aktywnego bohatera,
- zachowanie `currentBuildQuery` w flow `biblioteka itemów -> import kolejnego itemu -> powrót do current build`,
- SSR tabelarycznego indeksu biblioteki itemów z rozwijanymi szczegółami,
- SSR założenia itemu z biblioteki do zgodnego slotu aktywnego bohatera,
- render badge'a `Założony` przy aktywnym itemie bez osobnej kolumny statusu w bibliotece,
- render sekcji aktywnych itemów na `/policz-aktualny-build`,
- generowanie legalnych kandydatów searcha,
- generowanie deterministycznych kombinacji itemów biblioteki do searcha,
- zasadę najwyżej jednego itemu per slot w kombinacji searcha po bibliotece,
- integrację `kandydat biblioteki itemów -> effective stats -> CurrentBuildRequest`,
- poprawne wyliczenie liczby legalnych kandydatów w preflight searcha,
- spójność preflight searcha z rzeczywistą liczbą ocenionych kandydatów,
- zachowanie legalności action bara w searchu,
- użycie wspólnego runtime do oceny kandydatów searcha,
- deterministyczny ranking wyników searcha,
- brak zmiany wyników rankingu po dodaniu audytu i progressu,
- zachowanie liczby ocenionych kandydatów po dodaniu normalizacji wyników,
- normalizację top wyników bez zmiany surowej oceny,
- brak scalenia dwóch różnych kombinacji itemów biblioteki podczas normalizacji wyników,
- deterministyczny porządek wyników po normalizacji,
- CLI / entrypoint backendowego searcha,
- obecność informacji auditowych w CLI searcha,
- GET formularza GUI searcha,
- POST uruchamiającego GUI searcha,
- obecność wyeksponowanego `Trybu biblioteki itemów` i poprawionej hierarchii formularza searcha,
- obecność sekcji `Audit i preflight searcha`, `Ocenieni kandydaci`, `Wyniki po normalizacji`, `Najlepsze wyniki po normalizacji`, `Tryb biblioteki itemów`, `Wybrane itemy z biblioteki`, `Łączny wkład itemów`, `Łączne obrażenia` i `DPS` w GUI searcha,
- przejście z listy wyników searcha do szczegółów kandydata,
- obecność sekcji `Tryb biblioteki itemów`, `Wybrane itemy z biblioteki`, `Łączny wkład itemów`, `Łączne obrażenia`, `DPS`, `Debug bezpośrednich trafień`, `Debug opóźnionych trafień`, `Debug obrażeń reaktywnych` i `Ślad kroków symulacji` w drill-downie searcha,
- użycie tego samego runtime do wyliczenia szczegółów drill-downu searcha,
- odtworzenie tej samej kombinacji itemów biblioteki w drill-downie searcha,
- bezpieczne kopiowanie pustego stanu snapshotu,
- specjalną regułę zaokrąglenia prowadzącą do `raw crit hit = 52`.

### 11.3. Aktualne zamrożone fixture i wartości
Wspólne dane referencyjne aktualnych golden values:
- bohater: `Krys`
- poziom: `13`
- broń: `Short Sword 6-10`, średnio `8`
- skill testowy: `Brandish`
- tarcza: `+100% MAIN_HAND_WEAPON_DAMAGE`
- broń: `+1.5% CRIT_DAMAGE`
- itemy do `Strength`: `7 + 8 + 3`
- całkowity `Main stat`: `40`
- całkowita `Intelligence`: `19`
- redukcja obrażeń na poziomie `13`: `38%`
- sample reactive foundation dla GUI/CLI: `THORNS = 50`, `BLOCK_CHANCE = 50%`, `RETRIBUTION_CHANCE = 50%`
- kontrakt bazowego `Advance`: `147%`
- kontrakt `Advance + Wave Dash`: dodatkowy direct hit `191%`
- kontrakt `Advance + Flash of the Blade`: `322%`, `Vulnerable 2 s`, `cooldown 8 s`
- kontrakt `Clash + Crusader's March`: `Resolve = 3 s`, `+25% BLOCK_CHANCE`
- kontrakt `Clash + Punishment`: `+50 THORNS` na `3 s`

Zamrożone wartości:

| Scenariusz | Base damage | Raw hit | Single hit | Raw crit hit | Critical hit |
| --- | ---: | ---: | ---: | ---: | ---: |
| `Brandish rank 1` | 6 | 12 | 8 | 19 | 12 |
| `Brandish rank 5` | 8 | 17 | 11 | 27 | 16 |
| `Brandish rank 5 + Powrót światłości` | - | 24 | 15 | 37 | 23 |
| `Advance rank 5` | 12 | 24 | 15 | 37 | 23 |
| `Advance rank 5 + Wave Dash` | 12 | 56 | 35 | 86 | 53 |
| `Advance rank 5 + Flash of the Blade` | 26 | 54 | 33 | 82 | 51 |

Dodatkowe aktualne referencje kontraktowe:
- `Brandish rank 5 + Powrót światłości` składa się z dwóch komponentów `73%`, każdy `raw = 12`, `final = 8`.
- `Advance rank 5` dla referencyjnego buildu daje `raw = 24`, `final = 15`, `raw crit = 37`, `crit = 23`.
- `Advance rank 5 + Wave Dash` składa się z dwóch komponentów `147%` oraz `191%`; odpowiednio `raw = 24/final = 15` oraz `raw = 32/final = 20`.
- `Advance rank 5 + Flash of the Blade` daje `raw = 54`, `final = 33`, `raw crit = 82`, `crit = 51`.
- Manual simulation dla niereaktywnego scenariusza `Advance rank 5 + Flash of the Blade` w horyzoncie `9 s` daje dwa casty `Advance`, `7` naturalnych `WAIT` i `total damage = 66`.
- Manual simulation dla niereaktywnego scenariusza `Advance rank 5 + Flash of the Blade` z `Brandish` na pasku w horyzoncie `9 s` daje `total damage = 147`; `Brandish` korzysta z `Vulnerable` po `Flash of the Blade` w `t=2` i `t=3`, a następnie `Advance` wraca po cooldownie w `t=9`.
- `Holy Bolt rank 5` dla referencyjnego buildu daje `raw = 21`, `final = 13`, `raw crit = 32`, `crit = 20`.
- `Judgement` dla referencyjnego buildu daje `raw = 13`, `final = 8`, `raw crit = 20`, `crit = 13`.
- Manual simulation dla niereaktywnego regresyjnego scenariusza `Holy Bolt rank 5 + Judgement` w horyzoncie `60 s` daje `total damage = 932`, `DPS = 932 / 60`, `19` detonacji `Judgement` w horyzoncie i `1` aktywny `Judgement` pozostały na końcu.
- Manual simulation dla niereaktywnego regresyjnego scenariusza `Brandish rank 5` w horyzoncie `60 s` daje `total damage = 660`, `DPS = 660 / 60` i brak delayed hitów.
- Dla sample buildu pojedynczy enemy hit reactive bez buffów `Clash` daje `Thorns raw = 52`, `Thorns final = 32`, `Retribution expected raw = 13`, `Retribution expected final = 8` oraz `Reactive final = 40`.
- Dla scenariusza `Clash rank 5 + Crusader's March` pojedynczy enemy hit reactive daje `active block chance = 75%`, `Thorns raw = 52`, `Thorns final = 32`, `Retribution expected raw = 20`, `Retribution expected final = 12` oraz `Reactive final = 44`.
- Dla scenariusza `Clash rank 5 + Crusader's March + Punishment` pojedynczy enemy hit reactive daje `active block chance = 75%`, `active thorns bonus = 50`, `Thorns raw = 104`, `Thorns final = 64`, `Retribution expected raw = 39`, `Retribution expected final = 24` oraz `Reactive final = 88`.
- Enemy hit schedule w horyzoncie `60 s` daje `20` reactive ticków.
- Manual simulation dla scenariusza GUI/CLI `Advance rank 5 + Flash of the Blade` na sample buildzie w horyzoncie `10 s` daje `total damage = 186`, `DPS = 18.6000`, `total reactive damage = 120`, dwa casty `Advance`, naturalne `WAIT` oraz stan cooldownu widoczny w trace.
- Manual simulation dla scenariusza GUI/CLI `Clash rank 5 + Crusader's March + Punishment` na sample buildzie w horyzoncie `60 s` daje `total damage = 1760`, `DPS = 1760 / 60`, `total reactive damage = 1760`, `Resolve aktywny na końcu = tak`, `Active block chance na końcu = 75%` oraz `Active thorns bonus na końcu = 50`.
- Manual simulation dla scenariusza regresyjnego `Holy Bolt rank 5 + Judgement` na sample buildzie M5a w horyzoncie `60 s` daje `total damage = 1732`, `DPS = 1732 / 60`, `total reactive damage = 800`, `19` detonacji `Judgement` i `1` aktywny `Judgement` pozostały na końcu.
- Backendowy search dla scenariusza `Advance rank 5` z choice range `NONE, LEFT, RIGHT`, `bar size = 1` i `horyzont = 9 s` daje deterministyczny ranking: `Wave Dash = 315`, `bazowy Advance = 135`, `Flash of the Blade = 66`.
- Backendowy search dla smoke testu `FOUNDATION_M9 --top 5` daje `2949` ocenionych kandydatów, `137` wyników po normalizacji oraz top 1 `Advance -> Clash` z `Wave Dash + Punishment`, `total damage = 439`, `DPS = 48.7778`.
- GUI searcha dla smoke testu `FOUNDATION_M9` pokazuje `Ocenieni kandydaci = 2949`, `Wyniki po normalizacji = 137` oraz top 1 `Advance -> Clash` z `total damage = 439`, `DPS = 48.7778`.
- `Brandish rank 5 + Krzyżowe uderzenie (Vulnerable)` w modelu single target liczy wyłącznie główny hit `168%`; dla referencyjnego przypadku z aktywnym `Vulnerable` przed trafieniem wynik ST pozostaje `raw hit = 34`, `single hit = 21`, `raw crit hit = 52`, `critical hit = 32`.
- Dla powyższego scenariusza `Brandish + Krzyżowe uderzenie (Vulnerable)` referencyjny `raw crit hit = 52` wynika z reguły: najpierw `raw hit` głównego trafienia jest zaokrąglany do `34`, a dopiero potem liczony jest `raw crit hit = round(34 * critMultiplier) = 52`.

## 12. Zasady dostarczania
- Projekt dostarczany jest jako pełna paczka projektu.
- Nie wolno dostarczać pojedynczych plików jako substytutu gotowego projektu.
- Nie wolno zostawiać technicznych placeholderów w logice, testach ani UI zamiast ukończonego istniejącego flow; wyjątkiem są jawnie opisane produktowe placeholder pages przyszłych modułów w app shell.
- Każda zmiana logiki wymaga:
  - aktualizacji kodu,
  - aktualizacji testów,
  - aktualizacji README.
- Przed dostarczeniem paczki obowiązuje uruchomienie testów i potwierdzenie `100%` przejścia.
- Jeżeli zmiana wpływa na liczby referencyjne, trzeba zaktualizować golden values i wszystkie miejsca diagnostyczne zależne od tych liczb.
- README ma pozostać samowystarczalnym kontraktem projektu dla kolejnych implementacji od zera.
