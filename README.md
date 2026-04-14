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
- Jeden wspólny engine liczy:
  - single hit debug,
  - delayed hit,
  - reactive damage,
  - manual simulation,
  - build search.
- Search nie może używać skróconej lub alternatywnej logiki względem manual simulation.

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
Docelowe wspólne wyjście runtime ma postać `SimulationResult`. Ten model jest źródłem prawdy dla UI wyniku i musi zawierać co najmniej:
- wynik końcowy,
- użyty pasek skilli,
- dane single hit debug,
- delayed hit debug,
- reactive debug,
- metryki searcha, jeżeli wynik pochodzi z build search,
- `stepTrace` pochodzący z tej samej symulacji, która policzyła wynik końcowy.

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
Zakres startowy projektu obejmuje następujące aktywne skille Paladina:
- `Brandish`
- `Holy Bolt`
- `Clash`
- `Advance`

Kontraktowe zasady dla tej grupy:
- wszystkie powyższe skille są kategorią `Basic`,
- wszystkie powyższe skille mają `resourceCost = 0`,
- brak kosztu zasobu nie zmienia reguły rotacji LRU.

### 4.4. Model efektów runtime
Runtime musi wspierać efekty danych skilla co najmniej w formach:
- `REPLACE_BASE_DAMAGE`,
- `DAMAGE`,
- `APPLY_STATUS`,
- `APPLY_BUFF`,
- `ADD_STACK`,
- `SET_COOLDOWN`.

Efekt może:
- podmienić bazowy procent obrażeń głównego komponentu,
- dodać osobny komponent obrażeń,
- nałożyć status na cel,
- nałożyć buff na bohatera,
- dodać stack stanu bohatera,
- nadpisać efektywny cooldown skilla.

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
- `Stun` z `Storm Bolt` trwa `2 s`.
- `Crusader's March` trwa `6 s`.
- `Resolve` ma cap `8` stacków.
- Trafienie przeciwnika usuwa dokładnie `1` stack `Resolve`.
- `Fervor` ma cap `3` stacków.
- `Fervor` daje `+25%[x]` damage na stack dla wspieranych skilli.
- `Fervor` wygasa całkowicie po przekroczeniu `3 s` bez odświeżenia nowym trafieniem.

### 6.4. Warunki komponentów
Warunki aktywacji komponentów i buffów są liczone względem aktualnego stanu w ticku:
- `Healthy` dotyczy stanu bohatera,
- `Vulnerable` i `Stun` dotyczą stanu celu,
- `Resolve` i `Fervor` dotyczą stanu bohatera.

## 7. Single hit / delayed hit / reactive damage
### 7.1. Single hit
- `Debug pojedynczego hita` pokazuje wyłącznie główne natychmiastowe trafienie skilla.
- `minHit` i `maxHit` nie należą do aktualnego modelu; projekt liczy obrażenia na bazie średnich obrażeń broni.
- Panel single hit pokazuje stan targetu przed użyciem skilla.
- Jeżeli skill nakłada status, panel single hit nie może pokazywać korzyści z tego statusu dla tego samego trafienia.

Rozstrzygnięcia kontraktowe dla wariantów objętych zakresem startowym:
- `Brandish + Powrót światłości` liczy dwa komponenty `73% + 73%`; nie wolno liczyć `105% + 73%`.
- `Brandish + Miecz Mistrzostwa` przy stanie `Healthy` podmienia główny hit na `178%`; nie tworzy dodatkowego komponentu.
- `Brandish + Krzyżowe uderzenie (Vulnerable)` zawsze podmienia główny hit na `168%`; dwa dodatkowe łuki `168%` są komponentami bocznymi i nie wchodzą do single target.
- Dla `Brandish + Krzyżowe uderzenie (Vulnerable)` warunek `Vulnerable` dotyczy wyłącznie dwóch dodatkowych bocznych trafień.
- `Holy Bolt + Ricocheting Bolt` podmienia główny hit z `126%` na `189%`; trzy rykoszety są debugowym komponentem pominiętym w single target.
- `Holy Bolt + Divine Bolt` podmienia główny hit na `168%` i nakłada `Vulnerable` po trafieniu.
- `Holy Bolt + Storm Bolt` podmienia główny hit na `441%`; efekt `Stun` jest statusem użytkowym, a nie osobnym komponentem obrażeń.
- `Clash + Fala Zealot` nie zwiększa obrażeń bazowego trafienia `Clash`; jeśli fala AoE jest pokazywana, pozostaje pominięta w single target.
- `Advance + Wave Dash` trafia ten sam cel dwa razy: bazowy hit `147%` oraz dodatkowa fala `191%`.
- `Advance + Flash of the Blade` podmienia bazowy hit na pojedynczy hit `322%`, nakłada `Vulnerable` po trafieniu i ustawia efektywny cooldown na `8 s`.

Reguła referencyjnego zaokrąglenia dla `Brandish rank 5 + Krzyżowe uderzenie (Vulnerable)`:
- w modelu single target końcowy `raw crit hit` dla głównego trafienia jest liczony od wcześniej zaokrąglonego `raw hit`, a nie od niezaokrąglonej wartości exact,
- kontraktowy porządek dla tego przypadku jest następujący:
  1. policz exact `raw hit` dla głównego trafienia `168%`,
  2. zaokrąglij go do całkowitego `raw hit`,
  3. policz `raw crit hit = round(rawHitRounded * critMultiplier)`,
- ta reguła jest wymagana, aby referencyjny wynik dla tego scenariusza wynosił `raw hit = 34` oraz `raw crit hit = 52`.

### 7.2. Delayed hit
- Delayed hit jest osobnym komponentem obrażeń, a nie częścią natychmiastowego single hita.
- Delayed hit jest liczony w `trigger time`, nie w momencie aplikacji efektu.
- Delayed hit używa aktualnego stanu celu i aktualnego pipeline'u w momencie detonacji.
- `Judgement` jest aktualnym referencyjnym delayed hitem:
  - pochodzi z bazowego rozszerzenia `Holy Bolt`,
  - detonuje po `3 s`,
  - zadaje osobny hit `80% skillDamagePercent`,
  - nie stackuje się na jednym celu,
  - nie odświeża timera, jeśli poprzednia detonacja już czeka,
  - może zostać nałożony ponownie dopiero po przetworzeniu poprzedniej detonacji.
- Przy `Vulnerable = 2 s` i `Judgement = 3 s` delayed hit `Judgement` nie korzysta domyślnie z `Vulnerable` nałożonego przez to samo wcześniejsze trafienie, chyba że status został utrzymany innym źródłem.

### 7.3. Reactive damage
- Reactive damage nie jest częścią single hita.
- Reactive damage jest osobnym komponentem całkowitego DPS w symulacji.
- Przeciwnik trafia bohatera raz na `3 s`.
- Pierwsze trafienie przeciwnika następuje w `t = 3`, kolejne w `t = 6`, `t = 9` itd.
- Nie modelujemy obrażeń zadawanych przez przeciwnika bohaterowi; modelujemy wyłącznie obrażenia reaktywne generowane przez bohatera.
- Probabilistyczne reactive effects są liczone deterministycznie jako `expected value`.

Aktualny model reactive:
- Reactive trzeba rozdzielać na `Thorns` i `Retribution`.
- Dla reactive najpierw liczymy bazowe `Thorns` z buildu plus bonus z `Punishment`.
- Następnie nakładamy `mainStatMultiplier`.
- Dopiero potem nakładamy pozostałe reactive multipliers i redukcję poziomu celu.
- `Retribution` jest expected value liczoną na bazie `Thorns` już przeskalowanych przez `mainStatMultiplier`.
- Aktualny wzór expected value dla pulse'a `Retribution`:

```text
retributionExpectedRaw = thornsDamage * blockChance * retributionChance
```

Aktualne reguły związane ze skillami:
- `Clash + Crusader's March` daje `+15% Block Chance` i `2` stacki `Resolve` przez `6 s`.
- `Clash + Punishment` daje bonus do `Thorns` zależny od rangi `Clash`: rank 1 = `+10`, rank 5 = `+18`, wzrost liniowy co `+2`.
- `Clash + Fala Zealot` zastępuje efekt bazowego `Crusader's March`; nie daje `Block Chance` ani `Resolve`.
- `Clash + Fala Zealot` daje `+10% crit chance` po każdym trafieniu `Clash`, do maksymalnie `+30%`, na kolejne użycia i jest liczony jako expected value między normal hit i crit.

## 8. Rotacja symulacji
### 8.1. Horyzont i tick
Aktualny kontrakt symulacji zakłada `60` kroków po `1` sekundzie i ślad `stepTrace` dla tych samych `60` kroków.

W każdym ticku symulacji należy rozliczyć kolejno:
1. delayed damage,
2. reactive damage,
3. aktywny cast wybrany przez model LRU, jeżeli istnieje legalny kandydat.

`WAIT` oznacza brak aktywnego castu, ale nie zatrzymuje symulacji i nie blokuje delayed ani reactive damage.

### 8.2. Model LRU
Wybór aktywnego skilla działa jako LRU:
- wybierany jest skill użyty najdawniej,
- skill nigdy wcześniej nieużyty ma wyższy priorytet niż skill używany wcześniej,
- przy remisie wygrywa kolejność na pasku akcji.

Kandydat do użycia musi jednocześnie:
- być aktywnym skillem na pasku,
- mieć `rank > 0`,
- być legalnie aktywny w buildzie,
- nie być na cooldownie,
- mieć dostępny zasób, jeżeli skill w ogóle używa zasobu.

### 8.3. Trace
`stepTrace` musi pochodzić z dokładnie tej samej symulacji, która policzyła wynik końcowy. Nie wolno rekonstruować trace na podstawie samego stringa rotacji.

Każdy krok trace musi przechowywać co najmniej:
- numer sekundy,
- akcję (`skill` albo `WAIT`),
- damage bezpośredni,
- damage z delayed hits,
- damage reaktywny,
- łączny damage kroku,
- cumulative damage po kroku,
- resource przed i po kroku,
- snapshot stanu bohatera przed i po kroku,
- snapshot stanu przeciwnika przed i po kroku,
- snapshot stanu skilli z action bara przed i po kroku,
- `selectionReason`.

## 9. Build search
### 9.1. Jednostka oceny
Search ocenia parę:
- `build`
- `skill bar`

`Build + skill bar` jest kontraktową jednostką rankingu.

### 9.2. Etap 1 - legalne buildy
Etap 1 generuje wszystkie legalne konfiguracje punktów i modyfikatorów dla wszystkich skilli objętych zakresem startowym.

Reguły stanu pojedynczego skilla:
- `OFF`
- `rank 1..5` bez modyfikatorów,
- `rank 1..5` tylko z bazowym upgradem,
- `rank 1..5` z bazowym upgradem i dokładnie jednym dodatkowym modyfikatorem.

Reguły kosztu:
- `rank k` kosztuje `k` punktów,
- bazowy upgrade kosztuje `+1` punkt,
- dodatkowy modyfikator kosztuje `+1` punkt.

Zakres startowy Etapu 1:
- search obejmuje `Brandish`, `Holy Bolt`, `Clash`, `Advance`,
- search generuje wyłącznie legalne stany skilli,
- search nie może tworzyć stanów sprzecznych z modelem domeny.

### 9.3. Etap 2 - konfiguracje paska skilli
Aktualny limit paska aktywnych skilli to `6`.

Reguły Etapu 2:
- aktywny skill musi być na pasku, aby mógł zostać użyty i zadawać obrażenia,
- pasywny skill nie musi być na pasku, ale nie może zostać na niego dodany i sam nie zadaje obrażeń,
- jeśli liczba aktywnych skilli z punktami mieści się w limicie `6`, search generuje dokładnie jedną konfigurację paska zawierającą komplet tych skilli,
- dopiero gdy liczba aktywnych skilli przekracza limit, search generuje nieuporządkowane kombinacje `K choose 6`,
- `A | B` i `B | A` są traktowane jako ta sama konfiguracja,
- search używa jednej kanonicznej kolejności paska do rozstrzygania remisów LRU i zachowania deterministyczności,
- build bez żadnego aktywnego skilla nie generuje legalnej konfiguracji paska i nie jest oceniany.

### 9.4. Wymagania runtime dla searcha
- Search i manual simulation muszą używać tej samej logiki runtime.
- Search nie może zakładać monospamu jednego skilla jako kontraktu projektowego.
- Architektura searcha ma pozostać gotowa pod przyszły lookahead, ale aktualny wynik ma być liczony bieżącą symulacją LRU.
- Search powinien wykorzystywać wielowątkowość przede wszystkim do oceny konfiguracji `build + skill bar`.
- Wynik searcha wielowątkowego musi pozostać deterministyczny względem wersji single-thread.

### 9.5. Audyt, progres i eksport
Wynik searcha musi raportować osobno:
- czas Etapu 1,
- czas Etapu 2,
- czas całkowity,
- liczbę wygenerowanych buildów,
- liczbę ocenionych konfiguracji paska,
- wybrany pasek skilli dla najlepszego wyniku.

Progress dla długiego searcha musi pokazywać:
- aktualny etap,
- `processed / total`,
- procent,
- elapsed time,
- ETA.

Eksport CSV:
- musi korzystać z danych już policzonego zadania,
- nie może uruchamiać pełnego searcha drugi raz,
- dla wyniku searcha zapisuje osobne wiersze dla każdej konfiguracji `build + skill bar`,
- musi zawierać co najmniej: `build_id`, stan i koszt każdego skilla, `skill_bar_id`, `skill_bar`, `total_damage`.

## 10. UI, debug i prezentacja wyników
### 10.1. Zasady ogólne
- UI używa polskich nazw.
- Nazwy techniczne w kodzie mogą pozostać angielskie.
- Wynik `Policz aktualny build` i `Znajdź najlepszy build` musi mieć wspólny układ sekcji, aby dało się porównać wyniki `1:1`.
- Główny wynik ma być pokazany raz; nie wolno duplikować tej samej konfiguracji w wielu sekcjach.
- Widok desktopowy powinien używać szerokiej przestrzeni roboczej.
- Wynik musi pokazywać listę itemów z aktualnego buildu bez duplikowania tych samych informacji w paskach podsumowania.

### 10.2. Konfiguracja do porównania
Sekcja porównania dla obu trybów musi pokazywać:
- użyty pasek skilli,
- pełny build w czytelnych blokach per skill,
- status aktywny / nieaktywny,
- status `Na pasku` / `Poza paskiem`,
- rank,
- bazowe rozszerzenie,
- dodatkowy modyfikator,
- koszt punktów,
- koszt zasobu,
- efektywny cooldown,
- źródło cooldownu, jeżeli cooldown pochodzi z aktywnego efektu `SET_COOLDOWN`.

Brak jawnego rozróżnienia między `kosztem punktów` i `kosztem zasobu` jest błędem kontraktu UI/debug.

### 10.3. Debug single hit
Sekcja single hit:
- pokazuje duże liczby `Single hit` i `Critical hit` jako wynik końcowy,
- pokazuje niżej `Raw hit` i `Raw crit hit` jako dane diagnostyczne,
- pokazuje skill, rank, aktywny modyfikator oraz stan targetu przed użyciem skilla,
- nie pokazuje `minHit` i `maxHit`,
- pokazuje tabelę komponentów z rozróżnieniem `wliczony do ST` / `pominięty w ST`,
- dla komponentu pominiętego w ST musi pokazać powód wykluczenia.

### 10.4. Delayed i reactive debug
- Delayed hits mają osobną sekcję debug; nie są częścią panelu single hit.
- Jeżeli skill powinien mieć delayed panel, ale dane debug są puste, UI pokazuje ostrzeżenie zamiast cichego ukrycia sekcji.
- Reactive panel pokazuje osobno:
  - `Thorns raw / tick`,
  - `Thorns final / tick`,
  - `Retribution expected raw / tick`,
  - `Retribution expected final / tick`,
  - `Reactive final / tick`.

### 10.5. Wynik searcha
Wynik searcha musi pokazywać:
- najlepszy build,
- najlepszy pasek skilli,
- metryki Etapu 1 i Etapu 2,
- top konfiguracje do ręcznej weryfikacji,
- dostępny eksport CSV wszystkich policzonych konfiguracji,
- progress i ETA dla trwającego zadania.

### 10.6. Trace i formatowanie
- `SimulationResult` jest kontraktowym modelem źródłowym dla listy `60` kroków i dla UI aktywnego kroku.
- Duże liczby w UI są formatowane separatorem tysięcznym jako spacja.
- Czas w UI jest formatowany jako `ms / s / min`.
- CSV pozostaje surowe liczbowo, bez separatorów formatowania prezentacyjnego.

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
- delayed hit `Judgement`,
- reactive damage `Thorns` i `Retribution`,
- model ticku i LRU,
- zgodność manual simulation i search,
- kanoniczną obsługę paska skilli,
- brak permutacji `A | B` vs `B | A`,
- poprawność `stepTrace`.

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
- `Holy Bolt rank 5` dla potwierdzonego buildu referencyjnego pozostaje `raw = 21`, `final = 13`, `rawCrit = 32`, `crit = 20`.
- `Brandish rank 5 + Krzyżowe uderzenie (Vulnerable)` w modelu single target liczy wyłącznie główny hit `168%`; dla referencyjnego przypadku z aktywnym `Vulnerable` przed trafieniem wynik ST pozostaje `raw hit = 34`, `single hit = 21`, `raw crit hit = 52`, `critical hit = 32`.
- Dla powyższego scenariusza `Brandish + Krzyżowe uderzenie (Vulnerable)` referencyjny `raw crit hit = 52` wynika z reguły: najpierw `raw hit` głównego trafienia jest zaokrąglany do `34`, a dopiero potem liczony jest `raw crit hit = round(34 * critMultiplier) = 52`.
- `Clash + Fala Zealot` liczy kolejne użycia jako expected value; dla bazowego rozkładu `17 / 25` referencyjna sekwencja pozostaje `17 -> 18 -> 18 -> 18`.

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
