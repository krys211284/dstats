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

Aktualny foundation zaimplementowany w repo obejmuje wyłącznie minimalny wspólny silnik pojedynczego uderzenia dla `Brandish` oraz jego potwierdzonych przypadków testowych. Manual simulation, build search, UI i pełna symulacja tickowa pozostają poza bieżącym zakresem kodu.

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

Na tym etapie te modele są źródłem prawdy dla:
- wyniku pojedynczego uderzenia,
- wyniku krytycznego,
- listy komponentów obrażeń,
- informacji o wliczeniu albo pominięciu komponentu w single target.

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
Aktualny foundation repo obejmuje następujący skill Paladina:
- `Brandish`

Kontraktowe zasady dla tej grupy:
- `Brandish` jest kategorią `Basic`,
- `Brandish` ma `resourceCost = 0`,
- brak kosztu zasobu nie zmienia reguły rotacji LRU.

Pozostałe skille wymieniane w opisie celu projektu nie należą jeszcze do aktualnego zakresu implementacji repo.

### 4.4. Model efektów runtime
Aktualny foundation wspiera następujące typy efektów runtime:
- `REPLACE_BASE_DAMAGE`,
- `DAMAGE`,
- `APPLY_STATUS`,

Efekt może:
- podmienić bazowy procent obrażeń głównego komponentu,
- dodać osobny komponent obrażeń,
- nałożyć status na cel.

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

Reguła referencyjnego zaokrąglenia dla `Brandish rank 5 + Krzyżowe uderzenie (Vulnerable)`:
- w modelu single target końcowy `raw crit hit` dla głównego trafienia jest liczony od wcześniej zaokrąglonego `raw hit`, a nie od niezaokrąglonej wartości exact,
- kontraktowy porządek dla tego przypadku jest następujący:
  1. policz exact `raw hit` dla głównego trafienia `168%`,
  2. zaokrąglij go do całkowitego `raw hit`,
  3. policz `raw crit hit = round(rawHitRounded * critMultiplier)`,
- ta reguła jest wymagana, aby referencyjny wynik dla tego scenariusza wynosił `raw hit = 34` oraz `raw crit hit = 52`.

### 7.2. Delayed hit
Delayed hit nie jest jeszcze częścią aktualnego foundation repo. Aktualny kod nie implementuje opóźnionych trafień ani ich debug breakdownu.

### 7.3. Reactive damage
Reactive damage nie jest jeszcze częścią aktualnego foundation repo. Aktualny kod nie implementuje obrażeń reaktywnych ani pełnej symulacji tickowej.

## 8. Rotacja symulacji
### 8.1. Horyzont i tick
Pełna rotacja symulacji nie jest jeszcze częścią aktualnego foundation repo. Obecny kod nie implementuje ticków, `WAIT` ani śladu `stepTrace`.

### 8.2. Model LRU
Reguła LRU pozostaje częścią docelowej architektury projektu, ale nie jest jeszcze aktywną częścią runtime foundation.

### 8.3. Trace
`stepTrace` nie jest jeszcze implementowany w aktualnym foundation repo.

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
Repo nie implementuje jeszcze UI. Aktualny foundation dostarcza wyłącznie modele debug w kodzie.

### 10.2. Konfiguracja do porównania
Na obecnym etapie foundation nie ma warstwy prezentacji konfiguracji do porównania.

### 10.3. Debug single hit
Aktualny foundation implementuje debug danych, nie renderowanie UI. W kodzie istnieją:
- `DamageBreakdown` jako wynik końcowy pojedynczego uderzenia,
- `DamageComponentBreakdown` jako wynik debug pojedynczych komponentów,
- informacja, czy komponent został wliczony do single target, czy pominięty z powodem.

### 10.4. Delayed i reactive debug
Delayed i reactive debug nie są jeszcze implementowane w aktualnym foundation repo.

### 10.5. Wynik searcha
Wynik searcha nie jest jeszcze implementowany w aktualnym foundation repo.

### 10.6. Trace i formatowanie
Modele `SimulationResult`, `stepTrace`, formatowanie UI i CSV pozostają poza aktualnym zakresem foundation repo.

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
