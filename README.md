# Diablo 4 DPS Engine / Paladin WebApp

## Status dokumentu
Ten README opisuje wyłącznie aktualny stan projektu. Jest kontraktem wykonawczym dla implementacji od zera w pustym repo i nie zawiera historii decyzji, etykiet wersji ani logu zmian.

## Podsumowanie redakcyjne
- Scalono logicznie jeden wspólny kontrakt runtime dla `Damage Engine`, manual simulation i build search, jeden model statusów i targetowania single target, jeden kontrakt debugowania wyników oraz jeden kontrakt testów i golden values.
- Celowo pominięto historię wersji, tymczasowe poprawki techniczne, log błędów UI/CRUD, duplikaty tych samych reguł, komentarze typu "po tej rundzie testów" oraz zapisy później zastąpione nowszą regułą.
- Rozstrzygnięte konflikty źródeł: dokumentacja i UI używają nazwy `Vulnerable`, a historyczne `Exposed` pozostaje wyłącznie aliasem technicznym; search ocenia `build + skill bar` bez permutacji slotów; modyfikatory typu `replace` podmieniają bazowy hit tam, gdzie finalna reguła tak stanowi; czas trwania statusów wynika z definicji konkretnego efektu, a nie z niespójnych ogólnych zapisów historycznych.

## 1. Cel projektu
Projekt służy do deterministycznego liczenia obrażeń Paladina w modelu single target oraz do wyszukiwania najlepszego legalnego buildu.

System wspiera dwa tryby pracy:
- `Policz aktualny build` - manual simulation dla aktualnej konfiguracji bohatera.
- `Znajdź najlepszy build` - build search oceniający legalne buildy i legalne konfiguracje paska skilli.

Aktualny foundation zaimplementowany w repo obejmuje:
- minimalny wspólny silnik pojedynczego uderzenia dla `Brandish` i `Holy Bolt`,
- delayed hit `Judgement` dla bazowego rozszerzenia `Holy Bolt`,
- tickową manual simulation dla trybu `Policz aktualny build`,
- minimalne webowe GUI SSR dla trybu `Policz aktualny build`,
- CLI pozostające równoległym smoke testem tego samego runtime.

Build search, reactive damage i pełna docelowa warstwa UI pozostają poza bieżącym zakresem kodu.

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

### 3.2. Wspólne wejście runtime
Docelowe wspólne wejście runtime ma postać `HeroBuildSnapshot`. Ten model musi zachowywać pełny stan buildu używany przez runtime:
- tożsamość i klasę bohatera,
- poziom bohatera,
- bonusowe punkty skilli,
- średnie obrażenia broni używane przez engine,
- globalny bonus procentowy do obrażeń, jeśli wynika z buildu,
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
- tickowego `stepTrace` dla dokładnie tej samej symulacji, która liczy wynik końcowy,
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

Kontraktowe zasady dla tej grupy:
- `Brandish` i `Holy Bolt` są kategorią `Basic`,
- `Brandish` i `Holy Bolt` mają `resourceCost = 0`,
- `Brandish` i `Holy Bolt` mają `cooldown = 0`,
- brak kosztu zasobu nie zmienia reguły rotacji LRU.

Pozostałe skille wymieniane w opisie celu projektu nie należą jeszcze do aktualnego zakresu implementacji repo.

### 4.4. Model efektów runtime
Aktualny foundation wspiera następujące typy efektów runtime:
- `REPLACE_BASE_DAMAGE`,
- `DAMAGE`,
- `APPLY_STATUS`,
- `APPLY_DELAYED_HIT`

Efekt może:
- podmienić bazowy procent obrażeń głównego komponentu,
- dodać osobny komponent obrażeń,
- nałożyć status na cel,
- zaplanować delayed hit z określonym trigger time.

Pozostałe typy efektów opisane w szerszej dokumentacji projektu nie są jeszcze częścią aktualnego foundation kodowego.

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
Reactive damage nie jest jeszcze częścią aktualnego foundation repo. Aktualny kod implementuje tickową symulację manualną, ale nie implementuje obrażeń reaktywnych.

## 8. Rotacja symulacji
### 8.1. Horyzont i tick
Aktualny foundation implementuje tickową manual simulation dla trybu `Policz aktualny build`.

Obowiązujący zakres:
- horyzont jest dodatnim parametrem wejściowym manual simulation przekazywanym do tego samego runtime przez CLI i GUI,
- referencyjny smoke test manual simulation oraz zamrożone wartości README pozostają liczone dla horyzontu `60 s`,
- brak reactive damage,
- kolejność ticku:
  1. delayed hit,
  2. aktywny cast.
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
- wszystkie aktualnie zaimplementowane skille mają `cooldown = 0`,
- dlatego w bieżącym foundation warunki cooldownu i zasobu są trywialnie spełnione dla `Brandish` i `Holy Bolt`,
- `WAIT` występuje wtedy, gdy żaden skill z paska nie spełnia minimalnych warunków legalnego castu.

### 8.3. Trace
`stepTrace` jest częścią aktualnego foundation repo i musi pochodzić z dokładnie tej samej pętli symulacji, która liczy wynik końcowy.

Minimalny kontrakt `stepTrace`:
- numer sekundy,
- akcja `SKILL` albo `WAIT`,
- nazwa akcji,
- damage bezpośredni,
- damage z delayed hitów,
- łączny damage kroku,
- cumulative damage po kroku,
- stan skilli z paska potrzebny do walidacji wyboru `LRU`,
- `selectionReason`.

## 9. Build search
### 9.1. Jednostka oceny
Build search nie jest jeszcze częścią aktualnego foundation repo.

### 9.2. Etap 1 - legalne buildy
Sekcja pozostaje poza aktualnym zakresem implementacji. Jedyny aktywny kontrakt foundation w repo to legalny model pojedynczego skilla `Brandish` i jego wariantów.

### 9.3. Etap 2 - konfiguracje paska skilli
Konfiguracje paska skilli nie są jeszcze implementowane w aktualnym foundation repo.

### 9.4. Wymagania runtime dla searcha
Search pozostaje celem architektonicznym projektu, ale nie należy do aktualnego foundation kodowego.

### 9.5. Audyt, progres i eksport
Audyt searcha, progress i eksport CSV nie są jeszcze częścią aktualnego foundation repo.

## 10. UI, debug i prezentacja wyników
### 10.1. Zasady ogólne
Repo implementuje minimalne webowe GUI M4 oraz CLI dla tego samego flow `Policz aktualny build`. Aktualny foundation dostarcza:
- prosty serwer HTTP z SSR bez rozbudowanego frontendu JS,
- pojedynczy ekran formularza dla `Brandish` i `Holy Bolt`,
- render wyniku oparty wyłącznie o istniejące modele debug i wynik runtime,
- modele debug w kodzie,
- CLI dla równoległego ręcznego smoke testu użytkownika.

### 10.2. Konfiguracja do porównania
Na obecnym etapie foundation nie ma warstwy prezentacji konfiguracji do porównania.

### 10.3. Debug single hit
Aktualny foundation implementuje debug danych oraz ich minimalny render w GUI i CLI. W kodzie istnieją:
- `DamageBreakdown` jako wynik końcowy pojedynczego uderzenia,
- `DamageComponentBreakdown` jako wynik debug pojedynczych komponentów,
- `SkillHitDebugSnapshot` jako reprezentatywny debug bezpośredniego hita per skill użyty w symulacji,
- informacja, czy komponent został wliczony do single target, czy pominięty z powodem.

`SimulationResult` nie modeluje jednego globalnego „selected skill” ani jednego globalnego `singleHitBreakdown` dla całej symulacji wieloskillowej.

### 10.4. Delayed i reactive debug
Aktualny foundation implementuje delayed debug dla `Judgement`:
- informację, kiedy delayed hit został nałożony,
- informację, kiedy miał detonować,
- informację, czy detonował w horyzoncie symulacji,
- breakdown delayed hita po detonacji,
- informację, czy `Judgement` pozostał aktywny na końcu horyzontu.

Reactive debug nie jest jeszcze implementowany.

### 10.5. Wynik searcha
Wynik searcha nie jest jeszcze implementowany w aktualnym foundation repo.

### 10.6. Trace i formatowanie
Aktualny foundation implementuje `stepTrace` w modelu danych i udostępnia go przez CLI oraz webowe GUI.

Kontrakt prezentacyjny trace:
- trace pokazuje tick po ticku tę samą symulację, która liczy wynik końcowy,
- dla każdego kroku pokazuje akcję, delayed damage, direct damage, step damage i cumulative damage,
- dla każdego kroku pokazuje stan skilli z paska potrzebny do walidacji `LRU`,
- CSV i pełny docelowy UX pozostają poza aktualnym zakresem repo.

### 10.7. Pierwszy smoke test użytkownika
Aktualny smoke test użytkownika dla M4 jest oparty o GUI oraz równoległe CLI i scenariusz:
- `Holy Bolt`
- `rank 5`
- bazowe rozszerzenie `Judgement`
- horyzont `60 s`

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
java '-Dfile.encoding=UTF-8' -cp target/classes krys.app.CalculateCurrentBuildCli --skill HOLY_BOLT --rank 5 --base-upgrade true --seconds 60 --show-trace true
```

Kontrakt prezentacji dla tego smoke testu:
- GUI jest po polsku i jasno komunikuje, że to aktualny foundation manual simulation, a nie pełny produkt końcowy.
- GUI pozwala ustawić skill, rank, bazowe rozszerzenie, dodatkowy modyfikator i horyzont symulacji, a następnie kliknąć `Policz aktualny build`.
- GUI i CLI pokazują `total damage`, `DPS`, debug bezpośredniego hita dla użytego skilla, debug delayed hitów, `stepTrace` oraz informację, czy `Judgement` pozostał aktywny na końcu horyzontu.
- CLI pokazuje użytkową nazwę skilla, a nie techniczny enum.
- Output powinien być czytelny w UTF-8; w Windows wymagane jest uruchomienie konsoli po `chcp 65001`.
- Dla referencyjnego scenariusza `Holy Bolt rank 5 + Judgement` wynik manual simulation wynosi `total damage = 932`, `DPS = 932 / 60`, `19` detonacji `Judgement` w horyzoncie i `1` aktywny `Judgement` pozostały na końcu.

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
- tick order `delayed hit -> aktywny cast`,
- `WAIT` przy braku legalnego castu,
- wybór `LRU`,
- tie-break według kolejności na pasku,
- zgodność cumulative damage z `stepTrace`,
- tickową manual simulation,
- endpoint formularza M4 dla `Policz aktualny build`,
- uruchomienie obliczenia przez GUI nad tym samym runtime M3,
- obecność kluczowych sekcji wyniku w GUI: `total damage`, `DPS`, direct hit debug, delayed hit debug i `stepTrace`,
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

Zamrożone wartości:

| Scenariusz | Base damage | Raw hit | Single hit | Raw crit hit | Critical hit |
| --- | ---: | ---: | ---: | ---: | ---: |
| `Brandish rank 1` | 6 | 12 | 8 | 19 | 12 |
| `Brandish rank 5` | 8 | 17 | 11 | 27 | 16 |
| `Brandish rank 5 + Powrót światłości` | - | 24 | 15 | 37 | 23 |

Dodatkowe aktualne referencje kontraktowe:
- `Brandish rank 5 + Powrót światłości` składa się z dwóch komponentów `73%`, każdy `raw = 12`, `final = 8`.
- `Holy Bolt rank 5` dla referencyjnego buildu daje `raw = 21`, `final = 13`, `raw crit = 32`, `crit = 20`.
- `Judgement` dla referencyjnego buildu daje `raw = 13`, `final = 8`, `raw crit = 20`, `crit = 13`.
- Manual simulation dla `Holy Bolt rank 5 + Judgement` w horyzoncie `60 s` daje `total damage = 932`, `DPS = 932 / 60`, `19` detonacji `Judgement` w horyzoncie i `1` aktywny `Judgement` pozostały na końcu.
- Manual simulation dla `Brandish rank 5` w horyzoncie `60 s` daje `total damage = 660`, `DPS = 660 / 60` i brak delayed hitów.
- `Brandish rank 5 + Krzyżowe uderzenie (Vulnerable)` w modelu single target liczy wyłącznie główny hit `168%`; dla referencyjnego przypadku z aktywnym `Vulnerable` przed trafieniem wynik ST pozostaje `raw hit = 34`, `single hit = 21`, `raw crit hit = 52`, `critical hit = 32`.
- Dla powyższego scenariusza `Brandish + Krzyżowe uderzenie (Vulnerable)` referencyjny `raw crit hit = 52` wynika z reguły: najpierw `raw hit` głównego trafienia jest zaokrąglany do `34`, a dopiero potem liczony jest `raw crit hit = round(34 * critMultiplier) = 52`.

## 12. Zasady dostarczania
- Projekt dostarczany jest jako pełna paczka projektu.
- Nie wolno dostarczać pojedynczych plików jako substytutu gotowego projektu.
- Nie wolno zostawiać placeholderów w logice, testach ani UI.
- Każda zmiana logiki wymaga:
  - aktualizacji kodu,
  - aktualizacji testów,
  - aktualizacji README.
- Przed dostarczeniem paczki obowiązuje uruchomienie testów i potwierdzenie `100%` przejścia.
- Jeżeli zmiana wpływa na liczby referencyjne, trzeba zaktualizować golden values i wszystkie miejsca diagnostyczne zależne od tych liczb.
- README ma pozostać samowystarczalnym kontraktem projektu dla kolejnych implementacji od zera.
