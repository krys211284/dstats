# Paladyn - Sprawiedliwość

## Metadane źródeł

| Pole | Wartosc |
|---|---|
| Poprzedni PDF | `docs/paladin/source-pdfs/diablo4_paladyn_sprawiedliwosc_umiejetnosci.pdf` |
| SHA-256 PDF | `677700cafe92ea1311c5eb166abcbd7a9327097c174c5024569dc0e5898edc5c` |
| Zewnętrzna paczka HTML | `diablo4.wiki.fextralife.com.zip` |
| SHA-256 HTML ZIP | `7025e253dee065ea107f6fbc459fa9e5eb233877743be3f61473716313c374db` |
| Status runtime DPS | `NIE ODBLOKOWUJE`; dokumentacja zrodlowa i tabele rang nie są wynikiem DPS |

## Kontrakt interpretacji

- Lokalne PDF-y / Markdown z repo sa kontraktem opisowym drzewa, grup ulepszen i notatek weryfikacyjnych.
- Fextralife HTML jest zewnetrznym zrodlem pomocniczym dla tabel rang 1-15, gdy lokalna strona HTML pokazuje jednoznaczny wiersz `Rank N` i wartość tooltipa.
- Nie wolno zgadywac brakujących wartości, interpolować luk ani sumowac wielohitów/ticków/komponentów bez osobnego testu single target.
- `treeMaxRank` oznacza maksymalną rangę możliwą do wyklikania w drzewie bez bonusów z itemów; obecnie dla aktywnych skilli jest to Rank 15.
- `effectiveRank` po bonusach z przedmiotów nie jest tutaj implementowany.

## Tabele rang z lokalnego HTML Fextralife

### Skazanie / Condemn (`skazanie`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Condemn.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Condemn` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [240%] | 240% |
| 2 | Damage: [264%] | 264% |
| 3 | Damage: [288%] | 288% |
| 4 | Damage: [312%] | 312% |
| 5 | Damage: [348%] | 348% |
| 6 | Damage: [372%] | 372% |
| 7 | Damage: [396%] | 396% |
| 8 | Damage: [420%] | 420% |
| 9 | Damage: [444%] | 444% |
| 10 | Damage: [480%] | 480% |
| 11 | Damage: [504%] | 504% |
| 12 | Damage: [528%] | 528% |
| 13 | Damage: [552%] | 552% |
| 14 | Damage: [576%] | 576% |
| 15 | Damage: [612%] | 612% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Włócznia Niebios / Spear of the Heavens (`wlocznia_niebios`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Spear of the Heavens.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Spear+of+the+Heavens` |
| Status tabeli rang | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` |
| Wykryte metryki z HTML | Damage, Burst Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [160%] Burst Damage: [120%] | 160%, 120% |
| 2 | Damage: [176%] Burst Damage: [132%] | 176%, 132% |
| 3 | Damage: [192%] Burst Damage: [144%] | 192%, 144% |
| 4 | Damage: [208%] Burst Damage: [156%] | 208%, 156% |
| 5 | Damage: [232%] Burst Damage: [174%] | 232%, 174% |
| 6 | Damage: [248%] Burst Damage: [186%] | 248%, 186% |
| 7 | Damage: [264%] Burst Damage: [198%] | 264%, 198% |
| 8 | Damage: [280%] Burst Damage: [210%] | 280%, 210% |
| 9 | Damage: [296%] Burst Damage: [222%] | 296%, 222% |
| 10 | Damage: [320%] Burst Damage: [240%] | 320%, 240% |
| 11 | Damage: [336%] Burst Damage: [252%] | 336%, 252% |
| 12 | Damage: [352%] Burst Damage: [264%] | 352%, 264% |
| 13 | Damage: [368%] Burst Damage: [276%] | 368%, 276% |
| 14 | Damage: [384%] Burst Damage: [288%] | 384%, 288% |
| 15 | Damage: [408%] Burst Damage: [306%] | 408%, 306% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Konsekracja / Consecration (`konsekracja`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Consecration.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Consecration` |
| Status tabeli rang | `SINGLE_COMPONENT_PERCENT_BUT_TICK_OR_CHANNEL_RUNTIME_NEEDS_MODEL` |
| Wykryte metryki z HTML | Damage, Healing Amount |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [75%] Healing Amount: 4.0% | 75% |
| 2 | Damage: [83%] Healing Amount: 4.2% | 83% |
| 3 | Damage: [90%] Healing Amount: 4.3% | 90% |
| 4 | Damage: [97%] Healing Amount: 4.5% | 97% |
| 5 | Damage: [109%] Healing Amount: 4.6% | 109% |
| 6 | Damage: [116%] Healing Amount: 4.8% | 116% |
| 7 | Damage: [124%] Healing Amount: 4.9% | 124% |
| 8 | Damage: [131%] Healing Amount: 5.0% | 131% |
| 9 | Damage: [139%] Healing Amount: 5.1% | 139% |
| 10 | Damage: [150%] Healing Amount: 5.2% | 150% |
| 11 | Damage: [157%] Healing Amount: 5.3% | 157% |
| 12 | Damage: [165%] Healing Amount: 5.4% | 165% |
| 13 | Damage: [172%] Healing Amount: 5.4% | 172% |
| 14 | Damage: [180%] Healing Amount: 5.5% | 180% |
| 15 | Damage: [191%] Healing Amount: 5.6% | 191% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Oczyszczenie / Purify (`oczyszczenie`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Purify.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Purify` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Duration |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Duration: 2 seconds | - |
| 2 | Duration: 2.08 | - |
| 3 | Duration: 2.17 | - |
| 4 | Duration: 2.23 | - |
| 5 | Duration: 2.32 | - |
| 6 | Duration: 2.38 | - |
| 7 | Duration: 2.43 | - |
| 8 | Duration: 2.5 | - |
| 9 | Duration: 2.57 | - |
| 10 | Duration: 2.6 | - |
| 11 | Duration: 2.63 | - |
| 12 | Duration: 2.68 | - |
| 13 | Duration: 2.72 | - |
| 14 | Duration: 2.77 | - |
| 15 | Duration: 2.8 | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

## Treść przekonwertowana z lokalnego PDF

Poniższa sekcja zachowuje tekst dawnego PDF-a w formie edytowalnej Markdown. W razie konfliktu z sekcją tabel rang należy dopisać notatkę weryfikacyjną, a nie nadpisywać danych liczbowych bez sprawdzenia źródła.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość
Dokument roboczy z mapowania tooltipów umiejętności. Grupa zamknięta opisowo: baza + grupa 1 +
grupa 2 + grupa 3 dla wszystkich umiejętności. Wartości pochodzą z przesłanych screenów i
odzwierciedlają widoczne rangi oraz aktualną konfigurację postaci; nie są jeszcze normalizowane do
0/15.

Status grupy
- Nazwa grupy: Sprawiedliwość.
- Opis grupy: Umiejętności Sprawiedliwości panują nad polem bitwy dzięki potężnej mocy Światłości.
- Umiejętności w grupie: Skazanie, Włócznia Niebios, Konsekracja, Oczyszczenie.
- Nie zgadujemy wartości. Wszystko, czego nie da się odczytać pewnie, oznaczono jako DO WERYFIKACJI.
- Na etapie dokumentacji zapisujemy pełny opis tooltipów. Modelowanie single target będzie osobnym etapem.

Słowa kluczowe widoczne na screenach
- Osąd: oznacza wroga na 3 sek. i po wygaśnięciu zadaje 80% obrażeń.
- Odsłonięci: wrogowie otrzymują obrażenia zwiększone o 20%.
- Osłabieni: wrogowie zadają obrażenia zmniejszone o 20% (zwykli wrogowie), 15% (wrogowie elitarni) lub 10%
(bossowie).

- Nieograniczone: postacie mogą przenikać przez wrogów, a ich szybkość ruchu nie może zostać zmniejszona.
- Oszołomieni: wrogowie nie mogą atakować ani używać umiejętności, ale wciąż mogą się poruszać.
- Umocnienie: stanowi dodatkowy zasób zdrowia, którego używanie zapewnia leczenie z upływem czasu.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                         Strona 1

1. Skazanie

Bazowa umiejętność
- Ranga ze screena: 4/15.
- Udział przedmiotu: 3.
- Tagi: Sprawiedliwość, Adept.
- Typ obrażeń: brak osobnej linii typu obrażeń na widocznej części tooltipa - DO WERYFIKACJI.
- Koszt: brak widocznego kosztu zasobu.
- Cooldown: 0,5 sek.
- Lucky Hit: 36%.
- Opis: Czerpiesz ze Światłości i po upływie 1,5 sek. przyciągasz wrogów, ogłuszasz ich na chwilę i zadajesz 501
637 [312%] pkt. obrażeń.

- Dodatkowy opis: Kiedy używasz Skazania, zyskujesz nieograniczenie na 1,5 sek.
- Następna ranga: Obrażenia: 559 519 [348%].
- Aktywne modyfikatory widoczne na tooltipie: dodatkowy ładunek + Odsłonięcie 4 sek.; Osłabienie 4 sek.;
rozmiar +50%.

- Uwagi single target: trzeba potwierdzić, czy obrażenie po 1,5 sek. zawsze trafia pojedynczy cel, jeśli cel
pozostaje w obszarze.

- Rzeczy do weryfikacji: typ obrażeń; dokładny czas „ogłuszenia na chwilę”; moment nakładania
Odsłonięcia/Osłabienia; bazowa liczba ładunków.

Grupa 1 - wybór 1 z 2
1A. Osłabienie
- Opis: Skazanie wywołuje osłabienie wrogów na 4 sek.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Osłabieni: zadawane obrażenia zmniejszone o 20% / 15% / 10% zależnie od typu wroga.
- Wartości:
- czas trwania Osłabienia: 4 sek.
- redukcja obrażeń zwykłych wrogów: 20%
- elitarnych: 15%
- bossów: 10%
- Rzeczy do weryfikacji:
- czy Osłabienie nakładane jest od razu przy użyciu, czy dopiero po 1,5 sek.
- czy obejmuje wszystkich wrogów w obszarze, czy tylko trafionych efektem końcowym.

1B. Redukcja Czasu Odnowienia
- Opis: Skazanie skraca swój czas odnowienia o 1 sek. za każdego trafionego wroga, maksymalnie do 4 sek.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- redukcja cooldownu: 1 sek. za każdego trafionego wroga
- maksymalna redukcja: 4 sek.
- Rzeczy do weryfikacji:
- kiedy naliczana jest redukcja: przy użyciu czy po finalnym trafieniu po 1,5 sek.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                                 Strona 2

- jak działa przy dodatkowych ładunkach.
- jak traktować redukcję w single target przy widocznym cooldownie 0,5 sek.

Grupa 2 - wybór 1 z 2
2A. Szybkość Ruchu
- Opis: Użycie Skazania zwiększa twoją szybkość ruchu o 30% na 3 sek.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- premia do szybkości ruchu: 30%
- czas trwania: 3 sek.
- Rzeczy do weryfikacji:
- czy premia ma znacznik [+], czy jest zwykłym 30%.
- czy działa równolegle z bazowym Nieograniczeniem na 1,5 sek.
- czy kolejne użycia odświeżają czas trwania.

2B. Zwiększenie Rozmiaru
- Opis: Rozmiar Skazania jest zwiększony o 50%.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- zwiększenie rozmiaru obszaru: 50%
- Rzeczy do weryfikacji:
- czy zwiększa tylko obszar przyciągnięcia/kontroli, czy także obszar obrażeń.
- czy w single target uznajemy brak wpływu na DPS, jeśli cel i tak jest w obszarze.

Grupa 3 - wybór 1 z 3
3A. Zebranie Trzódki
- Opis: Podczas ładowania Skazanie oznacza wszystkich wrogów, a po aktywacji ich ku tobie przyciąga.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- brak dodatkowych wartości liczbowych w tooltipie.
- Rzeczy do weryfikacji:
- czy „oznacza” jest tylko technicznym oznaczeniem celów, czy konkretnym statusem.
- czy obejmuje wszystkich wrogów w obszarze Skazania.
- czy zmienia moment przyciągnięcia względem bazowego opóźnienia 1,5 sek.

3B. Zadośćuczynienie
- Opis: Zamiast przyciągać wrogów, Skazanie aktywuje się teraz znacznie szybciej i zadaje 1 114 215 [693%] pkt.
obrażeń.

- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- obrażenia: 1 114 215 [693%]
- aktywacja: znacznie szybciej - brak dokładnej wartości czasu.
- Rzeczy do weryfikacji:
- dokładny czas aktywacji.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                          Strona 3

- czy 1 114 215 [693%] zastępuje bazowe obrażenia Skazania.
- czy usunięcie przyciągania dotyczy tylko przyciągnięcia, czy też innych efektów kontroli.

3C. Wezwanie Winnych
- Opis: Skazanie ma teraz 1 dodatkowy ładunek i powoduje odsłonięcie na 4 sek.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Odsłonięci: wrogowie otrzymują obrażenia zwiększone o 20%.
- Wartości:
- dodatkowy ładunek: +1
- Odsłonięcie: 4 sek.
- zwiększone otrzymywane obrażenia: 20%
- Rzeczy do weryfikacji:
- bazowa liczba ładunków Skazania.
- czy Odsłonięcie nakładane jest przy użyciu, podczas ładowania, czy po finalnej aktywacji.
- czy Odsłonięcie zwiększa obrażenia samego Skazania, czy dopiero kolejne obrażenia po efekcie.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                       Strona 4

2. Włócznia Niebios

Bazowa umiejętność
- Ranga ze screena: 5/15.
- Udział przedmiotu: 5.
- Tagi: Sprawiedliwość, Sędzia.
- Typ obrażeń: Obrażenia Świętości.
- Koszt: brak widocznego kosztu zasobu.
- Cooldown: 8,59 sek.
- Lucky Hit: 45%.
- Opis: Sprowadzasz z nieba 4 niebiańskie włócznie, które zadają 373 012 [232%] pkt. obrażeń i powalają wrogów
na 1,5 sek. Po 1,5 sek. włócznie wybuchają, zadając 279 759 [174%] pkt. obrażeń.

- Następna ranga: Obrażenia: 398 737 [248%]; Nagłe obrażenia: 299 053 [186%].
- Efekty aktywne: 4 włócznie, pierwsze obrażenia 373 012 [232%], powalenie 1,5 sek., wybuch po 1,5 sek. za 279
759 [174%].

- Single target: trzeba zweryfikować, ile z 4 włóczni i ile wybuchów może trafić pojedynczy cel.

Grupa 1 - wybór 1 z 2
1A. Premia do Obrażeń Osądu
- Opis: Włócznia Niebios zadaje obrażenia zwiększone o 40%[x] wrogom pod wpływem Osądu.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Osąd: oznacza wroga na 3 sek. i po wygaśnięciu zadaje 80% obrażeń.
- Wartości:
- premia przeciw wrogom pod wpływem Osądu: 40%[x]
- czas Osądu: 3 sek.
- obrażenia Osądu po wygaśnięciu: 80% obrażeń
- Rzeczy do weryfikacji:
- czy premia obejmuje pierwsze trafienia włóczni i wybuchy po 1,5 sek.
- czy Osąd musi być aktywny w chwili pierwszego trafienia, czy osobno w chwili wybuchu.

1B. Redukcja Czasu Odnowienia
- Opis: Zużycie Osądu skraca aktywny czas odnowienia Włóczni Niebios o 0,5 sek.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Osąd: oznacza wroga na 3 sek. i po wygaśnięciu zadaje 80% obrażeń.
- Wartości:
- redukcja aktywnego cooldownu: 0,5 sek.
- warunek: zużycie Osądu
- Rzeczy do weryfikacji:
- co dokładnie liczy się jako zużycie Osądu.
- czy jedno zużycie przez wielohitową Włócznię redukuje cooldown raz, czy wielokrotnie.
- czy redukcja dotyczy tylko cooldownu już trwającego.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                         Strona 5

Grupa 2 - wybór 1 z 2
2A. Pociski
- Opis: Włócznia Niebios uwalnia 3 dodatkowe pociski po pierwszym trafieniu.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- dodatkowe pociski: 3
- warunek: po pierwszym trafieniu
- Rzeczy do weryfikacji:
- czy 3 pociski powstają z każdej z 4 włóczni, czy tylko z pierwszego trafienia całej umiejętności.
- czy pociski mają własne obrażenia.
- czy pociski mogą trafić ten sam single target.
- czy pociski również wybuchają po 1,5 sek.

2B. Odsłonięcie
- Opis: Włócznia Niebios wywołuje odsłonięcie wrogów na 4 sek.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Odsłonięci: wrogowie otrzymują obrażenia zwiększone o 20%.
- Wartości:
- Odsłonięcie: 4 sek.
- zwiększone otrzymywane obrażenia: 20%
- Rzeczy do weryfikacji:
- czy Odsłonięcie nakłada się przy pierwszym trafieniu, przy każdym trafieniu, czy dopiero po wybuchu.
- czy może zwiększyć obrażenia późniejszych wybuchów tej samej Włóczni.
- domyślna robocza kolejność dla pierwszego trafienia: hit -> efekt, do potwierdzenia.

Grupa 3 - wybór 1 z 3
3A. Werdykt Niebios
- Opis: Włócznia Niebios zyskuje 3 włócznie. Włócznie zadają 399 227 [240%] pkt. obrażeń na sekundę przez 12
sek. Włócznie te mogą zostać naznaczone przez Osąd.

- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Osąd: oznacza wroga na 3 sek. i po wygaśnięciu zadaje 80% obrażeń.
- Wartości:
- liczba włóczni: 3
- obrażenia: 399 227 [240%] pkt. obrażeń na sekundę
- czas trwania: 12 sek.
- Rzeczy do weryfikacji:
- czy 3 włócznie są dodatkiem do bazowych 4, czy osobnym/zastępczym efektem.
- czy 399 227 [240%] to łączny DPS wszystkich 3 włóczni, czy DPS każdej włóczni.
- jak działa „naznaczenie przez Osąd”.
- czy wszystkie 3 włócznie mogą zadawać obrażenia jednemu celowi.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                              Strona 6

3B. Rozdarcie Niebios
- Opis: Włócznia Niebios teraz zrzuca 8 włóczni wzdłuż drogi do celu.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- liczba włóczni: 8
- układ: wzdłuż drogi do celu
- Rzeczy do weryfikacji:
- czy 8 włóczni zastępuje bazowe 4, czy jest efektem dodatkowym.
- czy każda z 8 włóczni ma bazowe obrażenia i opóźniony wybuch.
- ile z 8 włóczni realnie trafia single target.

3C. Pięść Niebios
- Opis: Włócznia Niebios staje się umiejętnością Główną i jest teraz czystą świętą energią, która opada z nieba.
- Dodatkowy opis: Przy trafieniu zadaje 1 282 850 [771%] pkt. obrażeń oraz rozszczepia się na pociski (5), zadając
dodatkowo 1 282 850 [771%] pkt. obrażeń wrogom na ich drodze.

- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- konwersja: umiejętność Główna
- obrażenia przy trafieniu: 1 282 850 [771%]
- liczba pocisków: 5
- dodatkowe obrażenia pocisków: 1 282 850 [771%]
- Rzeczy do weryfikacji:
- czy obrażenia pocisków są łączne, czy na każdy pocisk.
- czy pociski mogą ponownie trafić ten sam single target.
- czy konwersja do umiejętności Głównej dodaje koszt zasobu.
- czy bazowe 4 włócznie i ich wybuchy są zastąpione.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                              Strona 7

3. Konsekracja

Bazowa umiejętność
- Ranga ze screena: 13/15.
- Udział przedmiotu: 5.
- Tagi: Sprawiedliwość, Defensywa, Sędzia.
- Typ obrażeń: brak osobnej linii typu obrażeń na widocznej części tooltipa - DO WERYFIKACJI.
- Koszt: brak widocznego kosztu zasobu.
- Cooldown: 11,05 sek.
- Lucky Hit: 17%.
- Opis: Kąpiesz się w Światłości przez 6 sek., przywracając sobie i sojusznikom 5,4% maksymalnego zdrowia (762
pkt.) na sek. i zadając wrogom 277 347 [172%] pkt. obrażeń na sekundę.

- Następna ranga: Obrażenia: 289 406 [180%] pkt.; Wartość leczenia: 5,5%.
- Aktywne modyfikatory widoczne na tooltipie: Rozmiar Konsekracji +56%; w obszarze Konsekracji postać i
sojusznicy zadają 7%[x] więcej obrażeń i zużywają 15%[x] mniej zasobów.

- Single target: trzeba potwierdzić częstotliwość ticków i czy tooltipowe „na sekundę” oznacza dokładnie 1 tick/s.

Grupa 1 - wybór 1 z 2
1A. Czas Działania
- Opis: Wydłuża czas działania Konsekracji o 35%[x].
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- wydłużenie czasu działania: 35%[x]
- bazowy czas działania: 6 sek.
- roboczo po modyfikatorze: 8,1 sek. - DO WERYFIKACJI pod zaokrąglenia.
- Rzeczy do weryfikacji:
- czy wydłuża leczenie, obrażenia na sekundę i efekty z innych modyfikatorów.
- czy zwiększa liczbę ticków obrażeń/lecznia.
- jak gra zaokrągla czas działania.

1B. Osłabienie
- Opis: Konsekracja osłabia wrogów w zasięgu.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Osłabieni: zadawane obrażenia zmniejszone o 20% / 15% / 10%.
- Wartości:
- czas trwania Osłabienia: DO WERYFIKACJI
- Rzeczy do weryfikacji:
- czy Osłabienie działa tylko, gdy wróg stoi w obszarze Konsekracji.
- czy utrzymuje się po opuszczeniu obszaru.
- czy jest nakładane ciągle/tickowo, czy tylko przy wejściu.

Grupa 2 - wybór 1 z 2
2A. Umocnienie

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                              Strona 8

- Opis: Pierwsze użycie Konsekracji zapewnia umocnienie o wartości 0,5% maksymalnego zdrowia (67 pkt.) za
każdego trafionego wroga.

- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Umocnienie: dodatkowy zasób zdrowia, którego używanie zapewnia leczenie z upływem czasu.
- Wartości:
- Umocnienie za wroga: 0,5% maksymalnego zdrowia
- wartość ze screena: 67 pkt.
- warunek: pierwsze użycie Konsekracji
- Rzeczy do weryfikacji:
- co dokładnie oznacza pierwsze użycie Konsekracji.
- czy trafiony wróg oznacza wroga trafionego pierwszym tickiem, dowolnym tickiem, czy obecnego w obszarze
przy aktywacji.

- czy w single target efekt daje 0,5% / 67 pkt. za jednego wroga.

2B. Generowanie Zasobów
- Opis: Konsekracja zwiększa generowanie podstawowych zasobów u ciebie i twoich sojuszników o 10%[+].
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- premia do generowania podstawowych zasobów: 10%[+]
- dotyczy: postać i sojusznicy
- Rzeczy do weryfikacji:
- czy premia działa tylko w obszarze Konsekracji.
- czy podstawowe zasoby dla Paladyna oznaczają wyłącznie wiarę.
- czy premia obejmuje generowanie z umiejętności, pasywów, trafień, regeneracji naturalnej i/lub ekwipunku.

Grupa 3 - wybór 1 z 3
3A. Uświęcenie
- Opis: Konsekracja utrzymuje się przez 3 sek., zwiększając obrażenia i wartość leczenia o 20% w każdej
sekundzie.

- Dodatkowy opis: Gdy Konsekracja przestanie działać, wybucha, zadając wszystkim wrogom 426 697 [198%] pkt.
obrażeń i przywracając tobie oraz wszystkim sojusznikom 15% ich maksymalnego zdrowia (1 999 pkt.).

- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- czas wskazany w tooltipie: 3 sek.
- wzrost obrażeń i leczenia: 20% w każdej sekundzie
- wybuch po zakończeniu: 426 697 [198%]
- leczenie po zakończeniu: 15% maks. zdrowia (1 999 pkt.)
- Rzeczy do weryfikacji:
- dokładne znaczenie czasu 3 sek. względem bazowych 6 sek.
- czy wzrost 20%/s jest addytywny, multiplikatywny i czy dotyczy tylko Konsekracji.
- czy wybuch jest dodatkowy względem bazowych ticków.
- czy wybuch zawsze trafia single target stojący w obszarze.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                            Strona 9

3B. Bastion
- Opis: Konsekracja unieruchamia wrogów i można jej używać wszędzie.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- brak dodatkowych wartości liczbowych w tooltipie.
- Rzeczy do weryfikacji:
- czas trwania unieruchomienia.
- czy unieruchomienie działa na bossów lub tylko buduje stagger.
- co dokładnie oznacza możliwość używania Konsekracji „wszędzie”.

3C. Uświęcona Ziemia
- Opis: Rozmiar Konsekracji jest zwiększony o 56%.
- Dodatkowy opis: Przebywając na obszarze Konsekracji, ty i twoi sojusznicy zadajecie obrażenia zwiększone o
7%[x] i zużywacie o 15%[x] mniej zasobów.

- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- zwiększenie rozmiaru: 56%
- zwiększone zadawane obrażenia: 7%[x]
- mniejsze zużycie zasobów: 15%[x]
- Rzeczy do weryfikacji:
- czy 7%[x] dotyczy wszystkich obrażeń postaci, czy tylko wybranych źródeł.
- czy 15%[x] dotyczy wszystkich kosztów zasobów.
- czy trzeba pozostawać w obszarze przez cały czas.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                          Strona 10

4. Oczyszczenie

Bazowa umiejętność
- Ranga ze screena: 5/15.
- Udział przedmiotu: 5.
- Tagi: Sprawiedliwość, Defensywa, Sędzia.
- Typ obrażeń: brak widocznego typu obrażeń.
- Koszt: brak widocznego kosztu zasobu.
- Cooldown: 7,37 sek.
- Lucky Hit: brak widocznej wartości.
- Opis: Spowijasz wrogów w Światłość, oszałamiając ich na 2,32 sek.
- Następna ranga: Czas działania: 2,38.
- Widoczny opis: Oszołomieni wrogowie nie mogą atakować ani używać umiejętności, ale wciąż mogą się
poruszać.

- Single target: bazowo kontrola pola bez bezpośrednich obrażeń; znaczenie DPS zależy od modyfikatorów.

Grupa 1 - wybór 1 z 2
1A. Generowanie Wiary
- Opis: Oczyszczenie generuje 10 pkt. wiary za każdego trafionego wroga.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- generowanie wiary: 10 pkt. za każdego trafionego wroga
- Rzeczy do weryfikacji:
- czy trafiony wróg oznacza każdego wroga objętego oszołomieniem.
- czy boss liczy się jako trafiony wróg, nawet jeśli nie przyjmuje pełnej kontroli.
- czy w single target daje dokładnie 10 pkt. wiary za jedno użycie.

1B. Redukcja Czasu Odnowienia
- Opis: Oczyszczenie skraca swój czas odnowienia o 1 sek. za każdego trafionego wroga, maksymalnie do 3 sek.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Wartości:
- redukcja cooldownu: 1 sek. za każdego trafionego wroga
- maksymalna redukcja: 3 sek.
- bazowy cooldown ze screena: 7,37 sek. przy randze 5/15 i aktualnym buildzie.
- Rzeczy do weryfikacji:
- czy redukcja nalicza się przy użyciu, czy dopiero po skutecznym zastosowaniu oszołomienia.
- czy bossowie liczą się jako trafieni.
- czy cap 3 sek. dotyczy jednego użycia.

Grupa 2 - wybór 1 z 2
2A. Premia do Rozmiaru
- Opis: Zwiększa rozmiar Oczyszczenia o 50%.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                        Strona 11

- Wartości:
- zwiększenie rozmiaru obszaru: 50%
- Rzeczy do weryfikacji:
- czy zwiększa cały obszar aplikacji oszołomienia.
- czy większy rozmiar wpływa tylko na liczbę trafionych wrogów.
- w single target prawdopodobnie bez wpływu na DPS, jeśli cel i tak jest w zasięgu.

2B. Echo
- Opis: Efekt Oczyszczenia jest powtarzany 2 krotnie poza miejscem użycia umiejętności.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Oszołomieni: wrogowie nie mogą atakować ani używać umiejętności, ale wciąż mogą się poruszać.
- Wartości:
- liczba powtórzeń efektu: 2
- miejsce powtórzeń: poza miejscem użycia umiejętności
- Rzeczy do weryfikacji:
- czy „2 krotnie” oznacza 2 dodatkowe powtórzenia, czy łącznie dwa wystąpienia.
- gdzie dokładnie pojawiają się powtórzenia.
- czy powtórzenia mogą objąć ten sam single target.
- czy powtórzenia generują wiarę lub redukują cooldown przy połączeniach spoza standardowego wyboru.

Grupa 3 - wybór 1 z 3
3A. Zasądzenie
- Opis: Oczyszczenie dodatkowo Osądza wrogów, a jego czas odnowienia zostaje skrócony o 4 sek.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Osąd: oznacza wroga na 3 sek. i po wygaśnięciu zadaje 80% obrażeń.
- Wartości:
- skrócenie cooldownu: 4 sek.
- Osąd: 3 sek.
- obrażenia Osądu po wygaśnięciu: 80% obrażeń
- Rzeczy do weryfikacji:
- czy redukcja cooldownu o 4 sek. jest stała, czy działa dopiero po trafieniu/osądzeniu.
- czy Osąd nakłada się na wszystkich wrogów objętych Oczyszczeniem.
- czy boss również dostaje Osąd.

3B. Rozgrzeszenie
- Opis: Oczyszczenia można teraz używać na wrogach. Zamiast wywoływać oszołomienie, zadaje im 1 096 573
[551%] pkt. obrażeń i odsłania ich na 4,32 sek.

- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Odsłonięci: wrogowie otrzymują obrażenia zwiększone o 20%.
- Wartości:

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                       Strona 12

- obrażenia: 1 096 573 [551%]
- Odsłonięcie: 4,32 sek.
- zwiększone otrzymywane obrażenia: 20%
- bazowe oszołomienie zostaje zastąpione.
- Rzeczy do weryfikacji:
- czy obrażenia są naliczane na każdego trafionego wroga.
- czy Odsłonięcie nakłada się po zadaniu obrażeń, zgodnie z roboczą zasadą hit -> efekt.
- czy Oczyszczenie staje się celowane na wrogach, czy nadal działa obszarowo.

3C. Poddanie
- Opis: Zamiast oszołomienia, Oczyszczenie powoduje ogłuszenie wrogów na 4,32 sek.
- Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
- Widoczne słowa kluczowe / definicje:
- Na screenie widoczna była definicja Oszołomionych, ale tooltip modyfikatora mówi o ogłuszeniu - rozbieżność
do sprawdzenia.

- Wartości:
- bazowe oszołomienie zostaje zastąpione.
- ogłuszenie: 4,32 sek.
- Rzeczy do weryfikacji:
- pełna definicja Ogłuszenia w tej wersji gry.
- czy ogłuszenie trwa pełne 4,32 sek. na bossach, czy działa przez stagger/ograniczenie.
- czy zmiana kontroli wpływa na efekty zależne od oszołomionych wrogów.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                          Strona 13

Lista rzeczy otwartych dla grupy Sprawiedliwość
- Potwierdzenie bazowych wartości na 0/15, jeśli będziemy modelować skalowanie rang.
- Skazanie: moment nakładania Odsłonięcia/Osłabienia i dokładny czas aktywacji Zadośćuczynienia.
- Włócznia Niebios: liczba włóczni, pocisków i wybuchów realnie trafiających pojedynczy cel.
- Konsekracja: ticki obrażeń/lecznia, wpływ buffów obrażeń, mobilna lub powiększona Konsekracja, oraz końcowy
wybuch z Uświęcenia.

- Oczyszczenie: generowanie wiary, redukcje cooldownu, powtórzenia Echo, aplikacja Osądu/Odsłonięcia oraz
różnica między oszołomieniem i ogłuszeniem.

- Dla wszystkich efektów statusowych: potwierdzić kolejność hit -> efekt, chyba że test/tooltip wymusi wyjątek.

Notatki do późniejszego modelowania DPS Engine
- Skazanie: burst po opóźnieniu 1,5 sek., kontrola pola, Nieograniczenie, opcjonalne Odsłonięcie/Osłabienie i
wariant z dużym szybszym obrażeniem.

- Włócznia Niebios: wielohit/multi-projectile z opóźnionym wybuchem; wymaga ścisłej weryfikacji single target.
- Konsekracja: obszar z obrażeniami na sekundę, leczeniem, buffami oraz potencjalnymi efektami końcowymi;
istotna dla rotacji, jeśli buffy działają na wszystkie źródła obrażeń.

- Oczyszczenie: bazowo kontrola bez obrażeń; przez modyfikatory może stać się generatorem wiary, źródłem
Osądu/Odsłonięcia albo ofensywną umiejętnością z obrażeniami.

Diablo 4 DPS Engine - Paladyn - Sprawiedliwość                                                            Strona 14
