# Diablo 4 DPS Engine / Build WebApp

## Status dokumentu
Ten README opisuje wyłącznie aktualny stan projektu. Jest kontraktem wykonawczym dla implementacji od zera w pustym repo i nie zawiera historii decyzji, etykiet wersji ani logu zmian.

## Podsumowanie redakcyjne
- Scalono logicznie jeden wspólny kontrakt runtime dla `Damage Engine`, manual simulation i build search, jeden model statusów i targetowania single target, jeden kontrakt debugowania wyników oraz jeden kontrakt testów i golden values.
- Celowo pominięto historię wersji, tymczasowe poprawki techniczne, log błędów UI/CRUD, duplikaty tych samych reguł, komentarze typu "po tej rundzie testów" oraz zapisy później zastąpione nowszą regułą.
- Rozstrzygnięte konflikty źródeł: dokumentacja i UI używają nazwy `Vulnerable`, a historyczne `Exposed` pozostaje wyłącznie aliasem technicznym; search ocenia `build + skill bar`, a kolejność paska pozostaje semantyczna przez `LRU`; modyfikatory typu `replace` podmieniają bazowy hit tam, gdzie finalna reguła tak stanowi; czas trwania statusów wynika z definicji konkretnego efektu, a nie z niespójnych ogólnych zapisów historycznych.

## 1. Cel projektu
Projekt służy do deterministycznego liczenia obrażeń Paladina w modelu single target oraz jest przygotowywany architektonicznie pod późniejsze wyszukiwanie najlepszego legalnego buildu.

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
- minimalne webowe GUI SSR dla trybu `Policz aktualny build`,
- prawdziwy ekran główny `/` działający jako hub aplikacji z grupami modułów i statusami,
- globalną nawigację SSR wspólną dla głównych ekranów aplikacji,
- centralny rejestr modułów aplikacji z opisem, grupą, statusem i URL,
- placeholder pages dla przyszłych sekcji dodatku i sezonu bez implementacji ich mechaniki,
- pierwszy SSR flow `Importuj item ze screena` dla pojedynczego itemu z ręcznym potwierdzeniem użytkownika,
- realny odczyt OCR pojedynczego screena itemu dla ograniczonego zakresu pól foundation,
- preprocessing obrazu itemu przed OCR z heurystycznym wycięciem obszaru tekstowego oraz kilkoma wariantami obrazu,
- deterministyczne scalanie wyników OCR z kilku wariantów bez zmiany runtime current build, simulation i searcha,
- parser polskich affixów foundation dla rozpoznawalnych fraz slotu i statów,
- model wstępnego rozpoznania z poziomem pewności i uwagami per pole,
- walidowany formularz zatwierdzonego itemu i mapowanie jego pól do aktualnego modelu buildu,
- dwa czytelnie nazwane tryby zastosowania zatwierdzonego itemu do current build: `Zastosuj do current build` oraz `Dodaj wkład do current build`,
- minimalną trwałą bibliotekę zapisanych itemów z wieloma itemami tego samego slotu, zapisywaną lokalnie w stabilnym katalogu użytkownika,
- wybór jednego aktywnego itemu per slot w bibliotece oraz deterministyczne dodawanie aktywnych itemów do ręcznej bazy current build,
- nowy tryb searcha po bibliotece itemów, który generuje kombinacje zapisanych itemów per slot i nadal składa je do effective current build przed tym samym runtime,
- pierwsze minimalne webowe GUI SSR dla trybu `Znajdź najlepszy build`,
- pierwszy drill-down SSR z wyniku searcha do pełnej analizy reprezentanta znormalizowanego wyniku na tym samym runtime co manual simulation,
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
- globalna nawigacja SSR jest renderowana z tego samego rejestru modułów, a nie z rozproszonych ręcznych linków w wielu ekranach,
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
- `ItemImageOcrPreprocessor` wycina heurystycznie obszar tekstowy, ogranicza wpływ ramki, grafiki itemu i dolnego overlayu oraz przygotowuje warianty `grayscale`, `upscale`, `contrast`, `threshold` i lekkie wyostrzenie,
- `WindowsItemOcrTextReader` uruchamia OCR dla kilku wariantów obrazu,
- `ItemImageImportCandidateMerger` scala per pole wyniki z kilku wariantów OCR w sposób deterministyczny i obniża pewność przy równorzędnych sprzecznych odczytach,
- `ItemImportEditableFormFactory` przygotowuje formularz ręcznego potwierdzenia na bazie wstępnego odczytu,
- `ItemImportFormMapper` waliduje ręcznie poprawiony item do `ValidatedImportedItem`,
- `ValidatedImportedItemToItemMapper` mapuje zatwierdzony item do aktualnego modelu `Item`,
- `ImportedItemCurrentBuildContributionMapper` mapuje zatwierdzony item do aktualnego agregowanego modelu buildu używanego przez `CurrentBuildRequest`,
- `ImportedItemCurrentBuildApplicationService` stosuje zatwierdzony item do istniejących statów current build w trybie `nadpisz` albo `dodaj wkład`,
- `ItemLibraryRepository` trwale zapisuje minimalną bibliotekę zatwierdzonych itemów oraz aktywny wybór per slot bez bazy danych,
- `ItemLibraryService` zapisuje zatwierdzony item do biblioteki, udostępnia listę zapisanych itemów, pilnuje jednego aktywnego itemu per slot i składa aktywne itemy do effective current build,
- `ItemLibraryDataDirectoryResolver` rozwiązuje katalog trwałych danych biblioteki itemów przez `dstats.dataDir` albo domyślny katalog użytkownika `~/.dstats/item-library/` i wykonuje bezpieczną migrację z legacy `target/item-library-runtime/`,
- biblioteka itemów pozostaje warstwą aplikacyjną przed `CurrentBuildRequest`,
- effective current build jest składany jako `ręczna baza formularza, która może pozostać częściowo pusta albo zerowa + aktywne itemy z biblioteki -> finalne effective current build stats -> CurrentBuildRequest -> CurrentBuildSnapshotFactory -> runtime`,
- tryb searcha po bibliotece itemów używa analogicznego kontraktu `ręczna baza searcha, która może pozostać częściowo pusta albo zerowa + kandydacka kombinacja zapisanych itemów z biblioteki -> finalne effective current build stats -> CurrentBuildRequest -> CurrentBuildSnapshotFactory -> runtime`,
- walidacja wejścia current build dotyczy finalnych effective stats mapowanych do `CurrentBuildRequest`, a nie wyłącznie surowej ręcznej bazy formularza,
- GUI importu itemu pozostaje cienką warstwą wejściową nad obecnym modelem current build i nie implementuje alternatywnego runtime,
- CLI searcha mapuje argumenty do `BuildSearchRequest` przez `BuildSearchCliRequestParser`,
- GUI searcha mapuje formularz do `BuildSearchRequest` przez `SearchBuildFormMapper`,
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

### 4.3. Zakres startowy domeny
Aktualny foundation repo obejmuje następujące skille Paladina:
- `Brandish`
- `Holy Bolt`
- `Clash`
- `Advance`

Kontraktowe zasady dla tej grupy:
- `Brandish`, `Holy Bolt`, `Clash` i `Advance` są kategorią `Basic`,
- `Brandish`, `Holy Bolt`, `Clash` i `Advance` mają `resourceCost = 0`,
- brak kosztu zasobu nie zmienia reguły rotacji LRU.

Aktualny kontrakt foundation dla `Advance` jest celowo minimalny i skupia się na pierwszym pełnym use case direct-hit runtime z cooldownem:
- bazowy `Advance` używa bezpośredniego hita `147%`,
- na obecnym etapie foundation `Advance` używa tego samego kontraktowego `147%` dla rang `1..5`,
- bazowe rozszerzenie `Advance` odblokowuje dodatkowe modyfikatory `Wave Dash` oraz `Flash of the Blade`,
- `Advance + Wave Dash` trafia ten sam cel dwa razy: `147%` oraz dodatkowa fala `191%`,
- `Advance + Flash of the Blade` podmienia bazowy hit na pojedynczy hit `322%`,
- `Advance + Flash of the Blade` nakłada `Vulnerable` po trafieniu,
- `Advance + Flash of the Blade` ustawia efektywny cooldown na `8 s`,
- bazowy `Advance` i `Advance + Wave Dash` nie ustawiają cooldownu.

Aktualny kontrakt foundation dla `Clash` jest celowo minimalny i ograniczony do pierwszego pełnego use case reactive foundation:
- bazowy `Clash` nie zadaje bezpośrednich obrażeń,
- bazowe rozszerzenie `Clash` oznacza `Crusader's March`,
- `Crusader's March` daje `Resolve` na `3 s` i `+25% block chance` na `3 s`,
- dodatkowy modyfikator `LEFT` oznacza `Punishment`,
- `Punishment` daje `+50 Thorns` na `3 s`,
- `Resolve` w aktualnym foundation jest czasowym stanem logicznym potrzebnym do debugowania i dalszego rozszerzania reactive, a nie pełnym ogólnym systemem stacków.

Pozostałe skille wymieniane w szerszym planie projektu nie należą jeszcze do aktualnego zakresu implementacji repo.

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
- `ItemImageImportCandidateParseResult` reprezentuje wstępny odczyt OCR wraz z metadanymi obrazu, poziomem pewności i uwagami per pole po scaleniu kilku wariantów OCR,
- `ItemImportEditableForm` reprezentuje ręcznie edytowalny formularz potwierdzenia,
- `ValidatedImportedItem` reprezentuje item zatwierdzony po walidacji,
- `ValidatedImportedItemToItemMapper` mapuje zatwierdzony item do aktualnego modelu `Item`,
- `ImportedItemCurrentBuildContributionMapper` mapuje zatwierdzony item do agregowanych pól aktualnego modelu current build,
- `ImportedItemCurrentBuildApplicationService` nakłada wkład itemu na istniejący current build w dwóch trybach kontraktowych.

Minimalny zakres pól importu itemu:
- `slot / typ itemu`,
- `weapon damage`, jeżeli dotyczy,
- `strength`,
- `intelligence`,
- `thorns`,
- `block chance`,
- `retribution chance`.

Minimalny zakres rozpoznawania tekstu OCR dla M13.1:
- slot / typ itemu rozpoznawany jest ostrożnie zarówno z angielskich, jak i wybranych polskich nazw typu itemu,
- aktualnie jawnie wspierane są co najmniej `tarcza` i `buty`, a dotychczasowe foundation slotów `MAIN_HAND`, `OFF_HAND`, `CHEST` i `RING` pozostają bez zmian,
- parser foundation rozpoznaje polskie frazy dla `Strength`, `Intelligence`, `Thorns` i `Block chance`,
- parser może rozpoznać `Retribution chance` tylko wtedy, gdy OCR zawiera jednoznaczną frazę `retribution chance` albo `szansa na odwet`,
- jeżeli linia affixu zawiera jednocześnie realny roll i zakres referencyjny w `[]` albo `()`, parser ma wybierać realny roll jako wartość affixu,
- liczby z zakresu referencyjnego w `[]` albo `()` nie są domyślną wartością affixu i nie mogą wygrywać z głównym rollem linii,
- nieobsługiwane affixy nie mogą być mapowane do statów foundation.

Jawne ograniczenia aktualnego foundation importu:
- flow nie obiecuje pełnej bezbłędności OCR ani vision,
- aktualny foundation wykonuje techniczną walidację obrazu, heurystyczny preprocessing, realny OCR kilku wariantów pojedynczego itemu oraz renderuje poziom niepewności pól,
- heurystyczne wycięcie obszaru tekstowego ma ograniczać wpływ ramki, grafiki itemu i dolnego overlayu, ale nie daje gwarancji pełnego odcięcia każdego zakłócającego elementu,
- przy równorzędnych sprzecznych odczytach z kilku wariantów OCR pole pozostaje z obniżoną pewnością zamiast sztucznego podbicia pewności,
- gdy parser nie potrafi bezpiecznie odróżnić głównego rolla od wartości referencyjnych, pole ma pozostać nierozpoznane zamiast zgadywania,
- użytkownik musi ręcznie zatwierdzić albo poprawić pola przed użyciem itemu,
- tryb `nadpisz` podstawia do current build tylko te pola, które rozpoznany item rzeczywiście wnosi,
- tryb `dodaj wkład` sumuje rozpoznany wkład itemu do statów current build przekazanych do importu,
- flow nie importuje jeszcze całego ekwipunku ani całej postaci,
- flow nie buduje jeszcze pełnego wielo-itemowego workflow ani sesji inventory,
- flow nie omija obecnego modelu current build i nie buduje bocznego modelu runtime.

### 4.6. Minimalna biblioteka zapisanych itemów
Aktualny foundation repo obejmuje minimalną bibliotekę zapisanych itemów jako warstwę aplikacyjną nad current build, a nie osobny model runtime.

Kontrakt biblioteki itemów:
- `SavedImportedItem` jest trwałą wersją zatwierdzonego itemu z własnym stabilnym `itemId`,
- biblioteka może przechowywać wiele itemów tego samego slotu,
- `ActiveItemSelection` przechowuje najwyżej jeden aktywny `savedItemId` per `EquipmentSlot`,
- aktywacja itemu jest walidowana względem slotu; nie można ustawić jako aktywnego itemu z innego slotu,
- usunięcie itemu czyści aktywny wybór dla tego itemu, jeśli był aktywny,
- biblioteka jest lokalną biblioteką użytkownika, a nie systemem kont, chmurą ani współdzielonym inventory,
- biblioteka nie jest pełnym inventory managerem, stashem ani porównywarką itemów.

Kontrakt integracji biblioteki z current build:
- pola formularza `Policz aktualny build` oznaczają ręczną bazę poza biblioteką itemów,
- ręczna baza może być częściowo pusta albo zerowa, jeżeli finalne effective stats zostaną domknięte przez aktywne itemy z biblioteki,
- aktywne itemy z biblioteki są deterministycznie dodawane do tej bazy,
- użytkownik nie powinien ręcznie wpisywać tych samych statów, które pochodzą już z aktywnych itemów,
- effective current build nadal kończy się zwykłym `CurrentBuildRequest`,
- walidacja requestu dotyczy dopiero finalnych effective stats po zsumowaniu ręcznej bazy i aktywnych itemów,
- `CurrentBuildSnapshotFactory` i runtime nadal pracują na tych samych płaskich polach co wcześniej,
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

Aktualny model main stat dla Paladina:

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
- lekkie placeholder pages dla przyszłych sekcji dodatku i sezonu,
- główną ścieżkę użytkownika opartą o `CurrentBuildRequest`, a nie o testowy snapshot,
- wspólną usługę aplikacyjną `CurrentBuildCalculationService` dla GUI i CLI,
- wspólną fabrykę runtime `CurrentBuildSnapshotFactory` budującą `HeroBuildSnapshot`,
- osobny input flow `ItemImageImportRequest -> ItemImageImportService -> ItemImportFormMapper` dla importu obrazu itemu,
- preprocessing OCR i deterministyczne scalanie wyniku per pole jeszcze przed ręcznym potwierdzeniem użytkownika,
- trwałą bibliotekę itemów opartą o prosty lokalny zapis plikowy bez bazy danych,
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
- render realnie rozpoznanych pól OCR, poziomu niepewności i ręcznego potwierdzenia pól itemu,
- prosty ekran SSR `/biblioteka-itemow` z listą zapisanych itemów i wyborem aktywnego itemu per slot,
- sekcję aktywnych itemów z biblioteki na ekranie `Policz aktualny build`,
- dwie czytelnie nazwane akcje zastosowania zatwierdzonego itemu do current build: `Zastosuj do current build` i `Dodaj wkład do current build`,
- akcję `Zapisz do biblioteki` po zatwierdzeniu importu pojedynczego itemu,
- wejście do importu itemu bez sesji wielu itemów, z możliwością zachowania kontekstu current build przez query string,
- mapowanie zatwierdzonego itemu do modelu `Item` oraz do agregowanych pól current build,
- osobny ekran GUI SSR searcha dla minimalnej przestrzeni foundation,
- sekcję audit / preflight searcha w GUI searcha,
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
- globalna nawigacja SSR jest widoczna co najmniej na ekranach `Strona główna`, `Policz aktualny build`, `Importuj item ze screena`, `Biblioteka itemów` oraz `Znajdź najlepszy build`,
- nawigacja jest renderowana z centralnego modelu modułów i prowadzi do aktywnych sekcji aplikacji,
- nawigacja nie buduje alternatywnego frontendu JS i pozostaje prostym SSR.

Kontrakt statusów modułów:
- `Dostępne` oznacza moduł działający na aktualnym foundation repo,
- `W przygotowaniu` oznacza moduł produktowo zaplanowany, ale bez doprecyzowanej jeszcze logiki,
- `Po premierze dodatku` oznacza sekcję odłożoną do czasu stabilizacji zasad po premierze,
- `Wymaga dodatku` oznacza sekcję zależną od nowych systemów dodatku,
- `Sezonowe` oznacza sekcję planowaną jako warstwa produktowa dla sezonu lub wydarzenia.

Aktualne moduły `Dostępne`:
- `Strona główna`
- `Policz aktualny build`
- `Znajdź najlepszy build`
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
- dla każdego wyniku pokazanie `total damage` i `DPS`,
- dla każdego wyniku pokazanie akcji przejścia do szczegółów reprezentanta.

Kontrakt prezentacyjny drill-downu searcha:
- drill-down jednoznacznie wskazuje wybrany wynik po normalizacji,
- drill-down pokazuje `Build input`, `Action bar skills`, `Action bar`, `total damage` oraz `DPS`,
- drill-down pokazuje `Tryb biblioteki itemów`, `Wybrane itemy z biblioteki` oraz `Łączny wkład itemów`,
- drill-down pokazuje `Direct hit debug`, `Delayed hit debug`, `Reactive debug` oraz `stepTrace`,
- drill-down pokazuje końcowe stany `Judgement`, `Resolve`, `Active block chance` oraz `Active thorns bonus`,
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
- GUI pozwala ustawić level, staty buildu, konfigurację wszystkich skilli foundation oraz action bar, a następnie kliknąć `Policz aktualny build`.
- GUI pozwala z tego samego formularza przejść do importu pojedynczego itemu ze screena z zachowaniem aktualnego kontekstu current build.
- GUI rozdziela trzy czytelne warstwy: `Baza ręczna`, `Aktywne itemy z biblioteki` oraz `Efektywne staty do obliczeń`.
- GUI pokazuje, że pola formularza są ręczną bazą poza biblioteką itemów i mogą pozostać częściowo puste albo zerowe, jeżeli active items dopełnią finalne effective stats.
- GUI pokazuje aktywny wkład biblioteki oraz finalne efektywne staty użyte do obliczeń na tym samym pipeline `effective stats -> CurrentBuildRequest -> CurrentBuildSnapshotFactory -> runtime`.
- GUI i CLI przechodzą przez ten sam kontrakt `CurrentBuildRequest -> CurrentBuildSnapshotFactory -> CurrentBuildCalculationService -> runtime`.
- scenariusze referencyjne są trybem pomocniczym do smoke testów i regresji, a nie główną ścieżką produktu.
- GUI i CLI pokazują `total damage`, `DPS`, debug bezpośredniego hita dla użytego skilla, debug delayed hitów, reactive debug, `stepTrace`, `Resolve aktywny na końcu`, `Active block chance na końcu` oraz `Active thorns bonus na końcu`.
- GUI i CLI pokazują `Thorns raw / tick`, `Thorns final / tick`, `Retribution expected raw / tick`, `Retribution expected final / tick`, `Reactive final / tick` oraz `Reactive contribution`.
- GUI i CLI pokazują naturalne `WAIT`, `cooldown=true/false` oraz `cooldownRemaining` dla scenariusza `Advance + Flash of the Blade`.
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
- GUI searcha pozwala ustawić level, weapon damage, strength, intelligence, thorns, block chance, retribution chance, zakresy skilli foundation, rozmiary action bara, top N, horyzont symulacji oraz opcjonalny tryb biblioteki itemów,
- GUI searcha przechodzi przez kontrakt `SearchBuildFormMapper -> BuildSearchRequest -> BuildSearchCalculationService -> BuildSearchPresentationNormalizer`,
- GUI searcha pokazuje audit / preflight searcha obok wyniku,
- GUI searcha pokazuje `Liczba legalnych kandydatów`, `Rozmiar przestrzeni statów`, opcjonalnie `Rozmiar przestrzeni biblioteki itemów`, `Rozmiar przestrzeni skilli`, `Rozmiar przestrzeni action bara` oraz `Skala search space`,
- GUI searcha pokazuje wejściową przestrzeń searcha,
- GUI searcha pokazuje `Ocenieni kandydaci`, `Wyniki po normalizacji`, `Top wyniki po normalizacji`, `Build input`, `Action bar skills`, `Action bar`, `Tryb biblioteki itemów`, `Wybrane itemy z biblioteki`, `Łączny wkład itemów`, `Total damage` oraz `DPS`,
- GUI searcha pozwala z listy wyników przejść do szczegółów reprezentanta przez osobny SSR drill-down,
- drill-down przechodzi przez kontrakt `CurrentBuildRequest -> CurrentBuildSnapshotFactory -> CurrentBuildCalculationService -> runtime`,
- drill-down pokazuje `Build input`, `Action bar skills`, `Action bar`, `Tryb biblioteki itemów`, `Wybrane itemy z biblioteki`, `Łączny wkład itemów`, `Total damage`, `DPS`, `Direct hit debug`, `Delayed hit debug`, `Reactive debug`, `Step trace`, `Judgement aktywny na końcu`, `Resolve aktywny na końcu`, `Active block chance na końcu` oraz `Active thorns bonus na końcu`,
- GUI searcha nie implementuje live progressu, CSV, wielowątkowości ani rozbudowanego UX ponad minimalny SSR.

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
- GUI przyjmuje upload obrazu pojedynczego itemu przez `multipart/form-data`,
- GUI waliduje technicznie, czy upload jest prawidłowym obrazem,
- GUI wykonuje preprocessing i realny OCR kilku wariantów pojedynczego screena, a następnie pokazuje metadane obrazu, poziom pewności oraz uwagi dla pól wstępnego odczytu,
- GUI pokazuje ręczny formularz zatwierdzenia obejmujący `slot`, `weapon damage`, `strength`, `intelligence`, `thorns`, `block chance` i `retribution chance`,
- zatwierdzony item jest mapowany do aktualnego modelu `Item` oraz do agregowanych pól current build,
- GUI po zatwierdzeniu itemu pozwala także wybrać akcję `Zapisz do biblioteki`,
- GUI pozwala przejść do `Policz aktualny build` w dwóch czytelnie nazwanych trybach: `Zastosuj do current build` albo `Dodaj wkład do current build`,
- jeżeli import został otwarty z formularza current build, tryb `dodaj wkład` wykorzystuje przekazane staty bez ręcznego sumowania przez użytkownika,
- flow nie obiecuje pełnej bezbłędności OCR i wymaga ręcznego potwierdzenia użytkownika przed użyciem danych,
- poza zakresem pozostają pełny wielo-itemowy workflow i pełny OCR całej postaci.

Smoke test GUI biblioteki itemów:

```text
http://127.0.0.1:8080/biblioteka-itemow
```

Kontrakt prezentacji dla smoke testu biblioteki itemów:
- GUI biblioteki jest po polsku i jasno komunikuje, że to minimalna warstwa zapisanych itemów nad current build,
- GUI biblioteki pokazuje globalną nawigację SSR wspólną z ekranem głównym i pozostałymi głównymi modułami,
- GUI biblioteki pokazuje listę zapisanych itemów wraz ze slotem, nazwą, źródłem i wkładem do current build,
- GUI biblioteki pozwala mieć wiele itemów tego samego slotu,
- GUI biblioteki wyraźnie oznacza aktywny item badge'em `Aktywny`, przy nieaktywnym pokazuje akcję `Ustaw jako aktywny` i komunikuje zasadę `jeden aktywny item per slot`,
- ustawienie nowego aktywnego itemu w bibliotece zastępuje poprzedni aktywny wybór w tym samym slocie,
- pusty stan biblioteki zawiera krótki komunikat SSR oraz bezpośredni link do importu itemu,
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
- obecność grup modułów na ekranie głównym,
- obecność statusów modułów na ekranie głównym,
- obecność globalnej nawigacji na głównych stronach SSR,
- routing do istniejących sekcji po dodaniu app shell,
- render placeholder pages przyszłych sekcji,
- endpoint formularza GUI dla `Policz aktualny build`,
- uruchomienie obliczenia przez GUI nad tym samym runtime,
- obecność kluczowych sekcji wyniku w GUI: `total damage`, `DPS`, direct hit debug, delayed hit debug, reactive debug i `stepTrace`,
- obecność sekcji reactive debug w GUI dla scenariusza `Clash`,
- obecność `WAIT` i stanu cooldownu w GUI dla scenariusza `Advance`,
- preprocessing obrazu itemu i przygotowanie kilku wariantów OCR,
- deterministyczne scalanie per pole wyników z kilku wariantów OCR,
- rozpoznanie ograniczonych pól foundation z pojedynczego screena itemu do `candidate parse result`,
- rozpoznanie polskich fraz foundation dla `Strength`, `Thorns` i `Block chance`,
- wybór realnego rolla zamiast liczby z zakresu referencyjnego dla linii typu `+114 do siły [107 - 121]`,
- rozpoznanie slotu dla co najmniej `buty` i `tarcza`,
- brak halucynacji dla nieobsługiwanych affixów OCR,
- mapowanie wstępnie rozpoznanych pól itemu do formularza ręcznego potwierdzenia,
- walidację ręcznie poprawionego itemu,
- mapowanie zatwierdzonego itemu do aktualnego modelu `Item`,
- mapowanie zatwierdzonego itemu do aktualnego agregowanego modelu current build,
- aplikowanie zatwierdzonego itemu do current build w trybie `nadpisz`,
- aplikowanie zatwierdzonego itemu do current build w trybie `dodaj wkład`,
- zapis zatwierdzonego itemu do trwałej biblioteki,
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
- upload obrazu itemu i render sekcji wstępnego rozpoznania,
- zatwierdzenie itemu i render dwóch trybów przejścia do current build,
- zapis itemu do biblioteki z poziomu SSR po zatwierdzeniu importu,
- GET strony biblioteki itemów,
- zachowanie `currentBuildQuery` w flow `biblioteka itemów -> import kolejnego itemu -> powrót do current build`,
- SSR ustawienia aktywnego itemu w bibliotece,
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
- obecność sekcji `Audit / preflight searcha`, `Ocenieni kandydaci`, `Wyniki po normalizacji`, `Top wyniki po normalizacji`, `Tryb biblioteki itemów`, `Wybrane itemy z biblioteki`, `Łączny wkład itemów`, `Total damage` i `DPS` w GUI searcha,
- przejście z listy wyników searcha do szczegółów kandydata,
- obecność sekcji `Tryb biblioteki itemów`, `Wybrane itemy z biblioteki`, `Łączny wkład itemów`, `Total damage`, `DPS`, `Direct hit debug`, `Delayed hit debug`, `Reactive debug` i `Step trace` w drill-downie searcha,
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
