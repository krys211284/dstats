# Diablo 4 DPS Engine / Paladin WebApp

## Status dokumentu
Ten README opisuje wyłącznie aktualny stan projektu. Jest kontraktem wykonawczym dla implementacji od zera w pustym repo i nie zawiera historii decyzji, etykiet wersji ani logu zmian.

## Podsumowanie redakcyjne
- Scalono logicznie jeden wspólny kontrakt runtime dla `Damage Engine`, manual simulation i build search, jeden model statusów i targetowania single target, jeden kontrakt debugowania wyników oraz jeden kontrakt testów i golden values.
- Celowo pominięto historię wersji, tymczasowe poprawki techniczne, log błędów UI/CRUD, duplikaty tych samych reguł, komentarze typu "po tej rundzie testów" oraz zapisy później zastąpione nowszą regułą.
- Rozstrzygnięte konflikty źródeł: dokumentacja i UI używają nazwy `Vulnerable`, a historyczne `Exposed` pozostaje wyłącznie aliasem technicznym; search ocenia `build + skill bar` bez permutacji slotów; modyfikatory typu `replace` podmieniają bazowy hit tam, gdzie finalna reguła tak stanowi; czas trwania statusów wynika z definicji konkretnego efektu, a nie z niespójnych ogólnych zapisów historycznych.

## 1. Cel projektu
Projekt służy do deterministycznego liczenia obrażeń Paladina w modelu single target oraz jest przygotowywany architektonicznie pod późniejsze wyszukiwanie najlepszego legalnego buildu.

Docelowo system ma wspierać dwa tryby pracy:
- `Policz aktualny build` - manual simulation dla aktualnej konfiguracji bohatera.
- `Znajdź najlepszy build` - build search oceniający legalne buildy i legalne konfiguracje paska skilli.

Aktualny foundation zaimplementowany w repo obejmuje:
- minimalny wspólny silnik pojedynczego uderzenia dla `Brandish` i `Holy Bolt`,
- pierwszy pełny use case cooldownowego direct-hit runtime dla `Advance`,
- pełny pierwszy use case reactive foundation dla `Clash`,
- delayed hit `Judgement` dla bazowego rozszerzenia `Holy Bolt`,
- foundation reactive damage dla `Thorns` i `Retribution`,
- ograniczony kontrakt `Resolve`, `Crusader's March` i `Punishment` potrzebny do use case `Clash`,
- tickową manual simulation dla trybu `Policz aktualny build`,
- minimalne webowe GUI SSR dla trybu `Policz aktualny build`,
- CLI pozostające równoległym smoke testem tego samego runtime.

Build search, pełny system zasobów, pełny system defensywnych statusów, pełne feature'y `Fervor`, pełny ogólny system `Resolve`, `Fala Zealot` oraz pełna docelowa warstwa UI pozostają poza bieżącym zakresem kodu.

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

Kontrakt M7 dla `Advance` jest celowo minimalny i skupia się na pierwszym pełnym use case direct-hit runtime z cooldownem:
- bazowy `Advance` używa bezpośredniego hita `147%`,
- na obecnym etapie foundation `Advance` używa tego samego kontraktowego `147%` dla rang `1..5`,
- bazowe rozszerzenie `Advance` odblokowuje dodatkowe modyfikatory `Wave Dash` oraz `Flash of the Blade`,
- `Advance + Wave Dash` trafia ten sam cel dwa razy: `147%` oraz dodatkowa fala `191%`,
- `Advance + Flash of the Blade` podmienia bazowy hit na pojedynczy hit `322%`,
- `Advance + Flash of the Blade` nakłada `Vulnerable` po trafieniu,
- `Advance + Flash of the Blade` ustawia efektywny cooldown na `8 s`,
- bazowy `Advance` i `Advance + Wave Dash` nie ustawiają cooldownu.

Kontrakt M6 dla `Clash` jest celowo minimalny i ograniczony do pierwszego pełnego use case reactive foundation:
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

Kontrakt M6 dla `Clash` dokłada do reactive foundation:
- `Crusader's March` ustawia `activeBlockChanceBonusPercent = 25`,
- `Punishment` ustawia `activeThornsBonus = 50`,
- oba buffy trwają `3 s`,
- `Resolve` jest stanem debug/runtime towarzyszącym `Crusader's March`,
- `Retribution expected raw = thornsDamage * activeBlockChance * retributionChance`.

Kontrakt M7 dla `Advance` dokłada do direct-hit runtime:
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
Reactive damage jest częścią aktualnego foundation repo w zakresie M7 dla `Thorns`, `Retribution` i pierwszego pełnego use case `Clash`.

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
Repo implementuje minimalne webowe GUI M7 oraz CLI dla tego samego flow `Policz aktualny build`. Aktualny foundation dostarcza:
- prosty serwer HTTP z SSR bez rozbudowanego frontendu JS,
- pojedynczy ekran formularza dla `Brandish`, `Holy Bolt`, `Clash` i `Advance`,
- render wyniku oparty wyłącznie o istniejące modele debug i wynik runtime,
- sekcję reactive debug dla foundation `Thorns`, `Retribution` i use case `Clash`,
- trace z informacją o cooldownie i `WAIT` dla use case `Advance`,
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

### 10.5. Wynik searcha
Wynik searcha nie jest jeszcze implementowany w aktualnym foundation repo.

### 10.6. Trace i formatowanie
Aktualny foundation implementuje `stepTrace` w modelu danych i udostępnia go przez CLI oraz webowe GUI.

Kontrakt prezentacyjny trace:
- trace pokazuje tick po ticku tę samą symulację, która liczy wynik końcowy,
- dla każdego kroku pokazuje akcję, delayed damage, reactive damage, direct damage, step damage i cumulative damage,
- dla każdego kroku pokazuje kontraktową kolejność `delayed -> reactive -> active cast`,
- dla każdego kroku pokazuje stan skilli z paska potrzebny do walidacji `LRU`,
- dla każdego kroku pokazuje co najmniej `cooldown=true/false` oraz `cooldownRemaining`,
- CSV i pełny docelowy UX pozostają poza aktualnym zakresem repo.

### 10.7. Pierwszy smoke test użytkownika
Aktualny smoke test użytkownika dla M7 jest oparty o GUI oraz równoległe CLI i scenariusz:
- `Advance`
- `rank 5`
- bazowe rozszerzenie włączone
- dodatkowy modyfikator `Flash of the Blade`
- horyzont `10 s`
- referencyjny sample build GUI/CLI z active reactive foundation:
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
java '-Dfile.encoding=UTF-8' -cp target/classes krys.app.CalculateCurrentBuildCli --skill ADVANCE --rank 5 --base-upgrade true --choice RIGHT --seconds 10 --show-trace true
```

Kontrakt prezentacji dla tego smoke testu:
- GUI jest po polsku i jasno komunikuje, że to aktualny foundation manual simulation, a nie pełny produkt końcowy.
- GUI pozwala ustawić skill, rank, bazowe rozszerzenie, dodatkowy modyfikator i horyzont symulacji, a następnie kliknąć `Policz aktualny build`.
- GUI i CLI pokazują `total damage`, `DPS`, debug bezpośredniego hita dla użytego skilla, debug delayed hitów, reactive debug, `stepTrace`, `Resolve aktywny na końcu`, `Active block chance na końcu` oraz `Active thorns bonus na końcu`.
- GUI i CLI pokazują `Thorns raw / tick`, `Thorns final / tick`, `Retribution expected raw / tick`, `Retribution expected final / tick`, `Reactive final / tick` oraz `Reactive contribution`.
- GUI i CLI pokazują naturalne `WAIT`, `cooldown=true/false` oraz `cooldownRemaining` dla scenariusza `Advance + Flash of the Blade`.
- CLI pokazuje użytkową nazwę skilla, a nie techniczny enum.
- Output powinien być czytelny w UTF-8; w Windows wymagane jest uruchomienie konsoli po `chcp 65001`.
- Dla referencyjnego scenariusza GUI/CLI `Advance rank 5 + Flash of the Blade` na sample buildzie wynik manual simulation wynosi `total damage = 186`, `DPS = 18.6000`, `total reactive damage = 120`, dwa casty `Advance` w `t=1` i `t=9`, naturalne `WAIT` w `t=2..8`, `cooldownRemaining=7` w `t=2` oraz `cooldownRemaining=1` w `t=8`.
- Dla powyższego sample buildu pojedynczy cast `Advance + Flash of the Blade` daje `raw = 54`, `final = 33`, `raw crit = 82`, `crit = 51`.
- Regresyjny scenariusz `Clash rank 5 + Crusader's March + Punishment` pozostaje dodatkowym smoke testem niższego poziomu dla reactive foundation.

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
- endpoint formularza GUI dla `Policz aktualny build`,
- uruchomienie obliczenia przez GUI nad tym samym runtime M7,
- obecność kluczowych sekcji wyniku w GUI: `total damage`, `DPS`, direct hit debug, delayed hit debug, reactive debug i `stepTrace`,
- obecność sekcji reactive debug w GUI dla scenariusza `Clash`,
- obecność `WAIT` i stanu cooldownu w GUI dla scenariusza `Advance`,
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
