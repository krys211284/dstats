# Paladyn - Odwaga

## Metadane źródeł

| Pole | Wartosc |
|---|---|
| Poprzedni PDF | `docs/paladin/source-pdfs/diablo4_paladyn_odwaga_umiejetnosci.pdf` |
| SHA-256 PDF | `9fbee6d073414d5e66379a474d26c0cef4694d010e1c4311f30f99490c9fbb11` |
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

### Szarża z Tarczą / Shield Charge (`szarza_z_tarcza`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Shield Charge.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Shield+Charge` |
| Status tabeli rang | `SINGLE_COMPONENT_PERCENT_BUT_TICK_OR_CHANNEL_RUNTIME_NEEDS_MODEL` |
| Wykryte metryki z HTML | Damage, Armor Gained |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [90%] Armor Gained: 40% | 90% |
| 2 | Damage: [99%] Armor Gained: 42% | 99% |
| 3 | Damage: [108%] Armor Gained: 43% | 108% |
| 4 | Damage: [117%] Armor Gained: 45% | 117% |
| 5 | Damage: [131%] Armor Gained: 46% | 131% |
| 6 | Damage: [139%] Armor Gained: 48% | 139% |
| 7 | Damage: [148%] Armor Gained: 49% | 148% |
| 8 | Damage: [157%] Armor Gained: 50% | 157% |
| 9 | Damage: [166%] Armor Gained: 51% | 166% |
| 10 | Damage: [180%] Armor Gained: 52% | 180% |
| 11 | Damage: [189%] Armor Gained: 53% | 189% |
| 12 | Damage: [198%] Armor Gained: 54% | 198% |
| 13 | Damage: [207%] Armor Gained: 54% | 207% |
| 14 | Damage: [216%] Armor Gained: 55% | 216% |
| 15 | Damage: [229%] Armor Gained: 56% | 229% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Egida / Aegis (`egida`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Aegis.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Aegis` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Block Chance |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Block Chance: 30% | - |
| 2 | Block Chance: 31% | - |
| 3 | Block Chance: 32% | - |
| 4 | Block Chance: 34% | - |
| 5 | Block Chance: 35% | - |
| 6 | Block Chance: 36% | - |
| 7 | Block Chance: 37% | - |
| 8 | Block Chance: 38% | - |
| 9 | Block Chance: 38% | - |
| 10 | Block Chance: 39% | - |
| 11 | Block Chance: 40% | - |
| 12 | Block Chance: 40% | - |
| 13 | Block Chance: 41% | - |
| 14 | Block Chance: 41% | - |
| 15 | Block Chance: 42% | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Spadająca Gwiazda / Falling Star (`spadajaca_gwiazda`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Falling Star.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Falling+Star` |
| Status tabeli rang | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` |
| Wykryte metryki z HTML | Landing Damage, Jump Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Landing Damage: [80%] Jump Damage: [240%] | 80%, 240% |
| 2 | Landing Damage: [264%] | 264% |
| 3 | Jump Damage: [96%] Landing Damage: [288%] | 96%, 288% |
| 4 | Landing Damage: [312%] | 312% |
| 5 | Jump Damage: [116%] Landing Damage: [348%] | 116%, 348% |
| 6 | Jump Damage: [124%] Landing Damage: [372%] | 124%, 372% |
| 7 | Jump Damage: [132%] Landing Damage: [396%] | 132%, 396% |
| 8 | Landing Damage: [420%] | 420% |
| 9 | Jump Damage: [148%] Landing Damage: [444%] | 148%, 444% |
| 10 | Jump Damage: [160%] Landing Damage: [480%] | 160%, 480% |
| 11 | Jump Damage: [168%] Landing Damage: [504%] | 168%, 504% |
| 12 | Jump Damage: [176%] Landing Damage: [528%] | 176%, 528% |
| 13 | Landing Damage: [552%] | 552% |
| 14 | Jump Damage: [192%] Landing Damage: [576%] | 192%, 576% |
| 15 | Jump Damage: [204%] Landing Damage: [612%] | 204%, 612% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Mobilizacja / Rally (`mobilizacja`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Rally.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Rally` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Faith Amount |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Faith Amount: 16 | - |
| 2 | Faith Amount: 17 | - |
| 3 | Faith Amount: 18 | - |
| 4 | Faith Amount: 19 | - |
| 5 | Faith Amount: 20 | - |
| 6 | Faith Amount: 21 | - |
| 7 | Faith Amount: 22 | - |
| 8 | Faith Amount: 23 | - |
| 9 | Faith Amount: 24 | - |
| 10 | Faith Amount: 25 | - |
| 11 | Faith Amount: 26 | - |
| 12 | Faith Amount: 27 | - |
| 13 | Faith Amount: 28 | - |
| 14 | Faith Amount: 29 | - |
| 15 | Faith Amount: 30 | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

## Treść przekonwertowana z lokalnego PDF

Poniższa sekcja zachowuje tekst dawnego PDF-a w formie edytowalnej Markdown. W razie konfliktu z sekcją tabel rang należy dopisać notatkę weryfikacyjną, a nie nadpisywać danych liczbowych bez sprawdzenia źródła.

Diablo 4 DPS Engine - Paladyn
Grupa umiejętności: Odwaga
Dokument opisowy tooltipów i modyfikatorów - wersja robocza do późniejszego modelowania DPS
Engine

Status: grupa zamknięta opisowo na podstawie przesłanych screenów. Część mechanik pozostaje oznaczona
jako DO WERYFIKACJI dla późniejszego modelowania single target.

Zasada nadrzędna: nie zgadujemy wartości. Dane niepewne albo wymagające testów w engine oznaczono
jako DO WERYFIKACJI.

Opis grupy: Umiejętności Odwagi wzmacniają twoją determinację i czynią z ciebie niepowstrzymaną siłę.

Struktura grupy
- Umiejętności: Szarża z Tarczą, Egida, Spadająca Gwiazda, Mobilizacja.

- Dla każdej umiejętności: bazowa umiejętność, grupa 1 - wybór 1 z 2, grupa 2 - wybór 1 z 2, grupa 3 - wybór 1 z
3.

- Z jednej grupy modyfikatorów można wybrać tylko jeden modyfikator.

- Mechaniki spoza drzewka mogą później pozwalać dobrać więcej modyfikatorów, ale nie zakładamy tego na
etapie bazowej dokumentacji.

- Dla DPS Engine interesuje nas single target, ale w tym dokumencie zapisano pełny opis tooltipów.

- Rykoszety, fale, wybuchy, dodatkowe cele i wielokrotne trafienia wymagają późniejszej osobnej weryfikacji
single target.

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                      Strona 1

1. Szarża z Tarczą
Bazowa umiejętność
- Nazwa: Szarża z Tarczą

- Ranga ze screena bazowego: 0/15

- Tagi: Odwaga, Mobilność, Moloch, Podtrzymanie

- Typ obrażeń: Obrażenia Fizyczne

- Wymaganie: Wymaga tarczy

- Koszt: brak widocznego kosztu zasobu w bazowym tooltipie

- Cooldown: 10 sek.

- Lucky Hit: Szansa na szczęśliwy traf: 35%

- Opis tooltipa: Szarżujesz z tarczą i odrzucasz wrogów, zyskując 40%[+] pancerza i zadając 2 382 [90%] pkt.
obrażeń podczas podtrzymywania.

- Efekty aktywne: szarża z tarczą; odrzucenie wrogów; 40%[+] pancerza; 2 382 [90%] pkt. obrażeń podczas
podtrzymywania.

Uwagi single target / engine
- Efekt odrzucenia nie jest bezpośrednio obrażeniowy, ale może mieć znaczenie kontrolne.

- Obrażenia podczas podtrzymywania wymagają późniejszego modelu: liczba ticków, częstotliwość i maksymalny
czas podtrzymania.

Rzeczy do weryfikacji
- Czy obrażenia 2 382 [90%] są zadawane raz, cyklicznie podczas podtrzymywania, czy zależą od czasu trwania
podtrzymania.

- Czy 40%[+] pancerza trwa tylko podczas podtrzymywania, czy także po zakończeniu.

Szarża z Tarczą - grupa 1: wybór 1 z 2
1A. Premia do Obrażeń
Opis: Obrażenia Szarży z Tarczą są zwiększane o 10%[x] po trafieniu wroga na 6 sek., do maksymalnie 30%[x].

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- premia po trafieniu: 10%[x]

- czas trwania premii: 6 sek.

- maksymalna premia: 30%[x]

Interpretacja robocza:
- Efekt wygląda jak kumulująca się premia do obrażeń Szarży z Tarczą po trafieniu wroga.

- Wartość 10%[x] i cap 30%[x] sugerują maksymalnie 3 poziomy kumulacji, ale tooltip nie mówi tego wprost.

Rzeczy do weryfikacji:
- Czy kolejne trafienia odświeżają czas trwania wszystkich kumulacji, czy każda kumulacja ma własny timer.

- Czy premia dotyczy tylko kolejnych obrażeń tej samej aktywacji, czy także kolejnych użyć przez 6 sek.

- Jak liczyć ją w single target przy obrażeniach podczas podtrzymywania.

1B. Animusz
Opis: Szarża z Tarczą zapewnia 1 poziom kumulacji Animuszu przy każdym trafieniu wroga.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
Wartości:

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                      Strona 2

- zysk Animuszu: 1 poziom kumulacji przy każdym trafieniu wroga

Widoczny opis słowa kluczowego:
- Aktywny Animusz zwiększa wartość pancerza o 25%[+].

- Odniesienie obrażeń bezpośrednich zużywa ładunek.

- Możesz mieć maksymalnie 8 ładunków.

Rzeczy do weryfikacji:
- Czy każde trafienie oznacza każdy hit/tick Szarży z Tarczą, czy tylko jednego trafionego przeciwnika na
aktywację.

- Czy w single target Szarża może wygenerować więcej niż 1 ładunek Animuszu podczas jednego użycia.

- Czy premia 25%[+] pancerza jest stała przy minimum 1 ładunku, czy skaluje się z liczbą ładunków.

Szarża z Tarczą - grupa 2: wybór 1 z 2
2A. Odwet
Opis: Szarża z Tarczą zapewnia 10%[+] szans na Odwet, gdy trafisz wroga w czasie szarżowania, do
maksymalnie 30%[+].

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- szansa na Odwet po trafieniu w czasie szarżowania: 10%[+]

- maksymalna szansa: 30%[+]

Widoczny opis słowa kluczowego:
- Odwet zapewnia szansę na pulsowanie cierni wokół ciebie, gdy wykonujesz blok.

- Obrażenia te liczą się jako umiejętność Molocha.

Interpretacja robocza:
- Efekt wygląda jak kumulowana szansa na aktywację mechaniki Odwetu przez trafienia Szarżą z Tarczą.

- Wartość 10%[+] do 30%[+] sugeruje maksymalnie 3 przyrosty, ale liczba kumulacji nie jest jawnie podana.

Rzeczy do weryfikacji:
- Czy szansa na Odwet ma czas trwania, czy jest powiązana tylko z konkretnym trafieniem.

- Czy Odwet zadaje obrażenia cierni natychmiast po bloku, cyklicznie, czy jako osobny puls.

- Czy obrażenia Odwetu skalują się jak ciernie, jak umiejętność Molocha, czy oba typy skalowania naraz.

- Czy w standardowym drzewku można połączyć ten efekt z Trafieniem Jako Blok - bazowo nie, bo to ta sama
grupa.

2B. Trafienie Jako Blok
Opis: Trafienia Szarżą z Tarczą liczą się jako zablokowanie ataku.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- brak dodatkowych wartości liczbowych w tooltipie

Interpretacja robocza:
- Każde trafienie Szarżą z Tarczą może uruchamiać mechaniki zależne od bloku.

- Efekt jest potencjalnie ważny dla interakcji defensywnych i kontrataków, ale sam nie dodaje jawnych obrażeń w
tooltipie.

Rzeczy do weryfikacji:
- Czy każde trafienie/tick Szarży liczy się jako osobny blok.

- Czy w single target podczas jednego podtrzymania można wygenerować wiele bloków.

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                        Strona 3

- Które efekty globalne, pasywne i z ekwipunku reagują na blok i powinny być później obsłużone przez DPS
Engine.

Szarża z Tarczą - grupa 3: wybór 1 z 3
3A. Nieustępliwa Szarża
Opis: Szarża z Tarczą staje się umiejętnością Główną, zadającą 2 859 [108%] pkt. obrażeń. Teraz użycie Szarży
z Tarczą kosztuje 20 pkt. wiary oraz dodatkowo 1 pkt. wiary na sekundę.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- nowy typ/kategoria: Główna

- obrażenia: 2 859 [108%] pkt. obrażeń

- koszt użycia: 20 pkt. wiary

- koszt podtrzymania / dodatkowy koszt: 1 pkt. wiary na sekundę

Interpretacja robocza:
- Modyfikator konwertuje Szarżę z Tarczą w umiejętność Główną i dodaje koszt wiary.

- Koszt 1 pkt. wiary na sekundę najpewniej dotyczy trwania szarży, ale mechanika wymaga potwierdzenia.

Rzeczy do weryfikacji:
- Czy 2 859 [108%] zastępuje bazowe 2 382 [90%], czy jest dodatkowym komponentem.

- Czy koszt 20 wiary jest pobierany przy rozpoczęciu, a 1 wiary/s podczas podtrzymywania.

- Czy brak wiary przerywa podtrzymywanie.

- Czy po konwersji skill korzysta z bonusów do umiejętności Głównych oraz nadal z tagów
Odwaga/Mobilność/Moloch/Podtrzymanie.

3B. Szarża Prawości
Opis: Szarża z Tarczą staje się umiejętnością Sędziego i zużywa przy trafieniu Osąd, zwiększając obrażenia
nowy kończącej o 20%[+] za każdy zużyty Osąd, maksymalnie do 100%. Gdy Szarża z Tarczą się kończy,
uwalniasz świętą nowę, zadając 2 891 [90%] pkt. obrażeń i ogłuszając wrogów na 3 sek.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- nowy typ/kategoria: Sędzia

- premia do obrażeń świętej nowy za każdy zużyty Osąd: 20%[+]

- maksymalna premia: 100%

- obrażenia świętej nowy po zakończeniu Szarży: 2 891 [90%] pkt. obrażeń

- ogłuszenie: 3 sek.

Widoczny opis słowa kluczowego:
- Osąd oznacza wroga na 3 sek. i po wygaśnięciu zadaje 80% obrażeń.

Rzeczy do weryfikacji:
- Fragment "obrażenia nowy kończącej" zapisano z tooltipa, ale forma językowa jest niepewna - sens wskazuje
na obrażenia świętej nowy kończącej Szarżę.

- Czy Szarża zużywa Osąd przy każdym trafieniu, czy tylko raz na wroga.

- Czy w single target można zużyć kilka Osądów na jednym celu przed zakończeniem Szarży.

- Czy premia 20%[+] za Osąd dotyczy wyłącznie świętej nowy kończącej Szarżę.

- Czy nova może trafić pojedynczy cel i czy jest osobnym hitem.

3C. Szarża Falangi

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                       Strona 4

Opis: Impet Szarży z Tarczą sunie naprzód, niosąc falę energii i powalając wrogów, i zadaje 5 059 [157%] pkt.
obrażeń.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- obrażenia fali energii / impetu: 5 059 [157%] pkt. obrażeń

- efekt kontroli: powalenie wrogów

Interpretacja robocza:
- Modyfikator dodaje lub przekształca Szarżę z Tarczą w atak z falą energii sunącą naprzód.

- Może być istotny dla single target jako dodatkowy komponent obrażeń, ale trzeba potwierdzić, czy fala trafia
pojedynczy cel niezależnie od kontaktu samej Szarży.

Rzeczy do weryfikacji:
- Czy 5 059 [157%] zastępuje bazowe obrażenia Szarży, czy jest osobnym komponentem.

- Czy fala energii może trafić tego samego pojedynczego celu co Szarża.

- Czy fala ma zasięg/linię/projektile i czy może wielokrotnie trafić ten sam cel.

- Czy powalenie wrogów ma dalsze interakcje obrażeniowe.

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                      Strona 5

2. Egida
Bazowa umiejętność
- Nazwa: Egida

- Ranga ze screena bazowego: 0/15

- Tagi: Odwaga, Defensywa, Moloch

- Typ obrażeń: brak widocznego typu obrażeń

- Koszt: brak widocznego kosztu zasobu

- Cooldown: 20 sek.

- Lucky Hit: brak widocznej wartości Lucky Hit

- Opis tooltipa: Otaczasz się tarczami Światłości, prowokując pobliskich wrogów i zyskując 30% szansy na blok na
4 sek.

- Efekty aktywne: tarcze Światłości; prowokacja pobliskich wrogów; 30% szansy na blok; czas trwania 4 sek.

Uwagi single target / engine
- Bazowo umiejętność wygląda defensywnie i nie ma bezpośrednich obrażeń.

- Prowokacja może mieć znaczenie dla symulacji ataków wroga, ale nie jest bezpośrednim składnikiem DPS.

Rzeczy do weryfikacji
- Czy tarcze Światłości mają limit, liczbę trafień albo dodatkowe interakcje z modyfikatorami.

- Czy brak Lucky Hit jest ostateczny, czy wynika z braku obrażeń bazowych.

Egida - grupa 1: wybór 1 z 2
1A. Nieustępliwość
Opis: Użycie Egidy zapewnia nieustępliwość na 4 sek.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- czas trwania nieustępliwości: 4 sek.

Widoczny opis słowa kluczowego:
- Nieustępliwych postaci nie można objąć żadnymi negatywnymi efektami, a już nałożone zostają zdjęte.

Rzeczy do weryfikacji:
- Czy nieustępliwość z Egidy działa dokładnie przez ten sam czas co bazowa premia do bloku Egidy, czyli 4 sek.

- Czy zdjęcie już nałożonych negatywnych efektów następuje natychmiast przy użyciu Egidy.

- Czy efekt ma znaczenie tylko defensywne, czy wpływa też na uptime rotacji przez odporność na kontrolę.

1B. Redukcja Czasu Odnowienia
Opis: Skraca czas odnowienia Egidy o 4 sek.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- redukcja cooldownu Egidy: 4 sek.

- bazowy cooldown Egidy: 20 sek.

- roboczy cooldown po modyfikatorze: 16 sek.

Rzeczy do weryfikacji:
- Czy redukcja jest stała i bezwarunkowa.

- Czy działa przed/po innymi źródłami redukcji czasu odnowienia, jeśli takie pojawią się później.

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                       Strona 6

Egida - grupa 2: wybór 1 z 2
2A. Redukcja Blokowanych Obrażeń
Opis: Użycie Egidy zwiększa redukcję blokowanych obrażeń o 15%, kiedy efekt jest aktywny.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- premia do redukcji blokowanych obrażeń: 15%

- warunek: kiedy efekt jest aktywny

Interpretacja robocza:
- Efekt wzmacnia defensywną jakość bloków w czasie działania Egidy.

- Tooltip nie pokazuje bezpośrednich obrażeń ani interakcji DPS.

Rzeczy do weryfikacji:
- Czy "efekt" oznacza bazowy czas działania Egidy, czyli 4 sek., albo wydłużony czas przy modyfikatorze Czas
Działania.

- Czy premia 15% jest addytywna do istniejącej redukcji blokowanych obrażeń.

- Czy redukcja blokowanych obrażeń może wpływać na mechaniki kontrataku, Odwetu, cierni lub inne efekty
zależne od bloku.

2B. Czas Działania
Opis: Wydłuża czas działania Egidy o 4 sek.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- wydłużenie czasu działania: 4 sek.

- bazowy czas działania Egidy: 4 sek.

- roboczy czas działania po modyfikatorze: 8 sek.

Interpretacja robocza:
- Efekt wydłuża czas aktywności bazowej premii Egidy: prowokacji i 30% szansy na blok.

- Jeśli inne modyfikatory działają "kiedy efekt jest aktywny", trzeba później potwierdzić, czy korzystają z
wydłużonego czasu przy łączeniu przez mechaniki spoza standardowego drzewka.

Rzeczy do weryfikacji:
- Czy wydłużenie dotyczy wszystkich efektów Egidy, w tym prowokacji i szansy na blok.

- Czy wydłużenie wpływa także na nieustępliwość z grupy 1, jeśli oba efekty zostaną połączone mechaniką spoza
standardowego wyboru.

- Czy czas działania 8 sek. jest poprawnym wynikiem 4 sek. + 4 sek.

Egida - grupa 3: wybór 1 z 3
3A. Zdecydowana Stanowczość
Opis: Działanie pasywne: zyskiwanie lub zużywanie Animuszu daje 4% szans na aktywowanie Egidy bez
zużywania zasobów. Użycie Egidy przyznaje 8 kumulacji Animuszu.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- szansa na aktywowanie Egidy przy zyskaniu lub zużyciu Animuszu: 4%

- Animusz przy użyciu Egidy: 8 kumulacji

Widoczny opis słowa kluczowego:
- Aktywny Animusz zwiększa wartość pancerza o 25%[+].

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                          Strona 7

- Odniesienie obrażeń bezpośrednich zużywa ładunek.

- Możesz mieć maksymalnie 8 ładunków.

Interpretacja robocza:
- Modyfikator tworzy pętlę defensywną Egida <-> Animusz.

- Sformułowanie "bez zużywania zasobów" zapisano dosłownie z tooltipa; bazowa Egida nie ma widocznego
kosztu zasobu, więc praktyczne znaczenie wymaga weryfikacji.

Rzeczy do weryfikacji:
- Czy pasywna aktywacja Egidy omija cooldown, zużycie ładunku/czasu odnowienia albo tylko koszt zasobu.

- Czy 4% szansy testowane jest przy każdym pojedynczym zyskaniu/zużyciu ładunku Animuszu.

- Czy użycie Egidy zawsze ustawia Animusz na 8 ładunków, czy dodaje 8 z limitem 8.

- Czy automatycznie aktywowana Egida również przyznaje 8 kumulacji Animuszu.

3B. Tarcza Wiary
Opis: Egida zamiast prowokować wrogów powoduje teraz Osąd. Zablokowanie ataku, gdy Egida jest aktywna,
powoduje Osądzenie napastnika.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- czas oznaczenia Osądu: 3 sek.

- obrażenia po wygaśnięciu Osądu: 80% obrażeń

Widoczny opis słowa kluczowego:
- Osąd oznacza wroga na 3 sek. i po wygaśnięciu zadaje 80% obrażeń.

Interpretacja robocza:
- Modyfikator zastępuje bazową prowokację efektem Osądu.

- Blok podczas aktywnej Egidy dodatkowo nakłada Osądzenie na napastnika.

Rzeczy do weryfikacji:
- Czy "powoduje teraz Osąd" oznacza nałożenie Osądu na pobliskich wrogów w momencie użycia Egidy zamiast
prowokacji.

- Czy "Osądzenie napastnika" jest tym samym stanem co Osąd, czy odrębną akcją/efektem.

- Czy każdy blok podczas aktywnej Egidy może nakładać Osąd wielokrotnie na ten sam cel.

- Czy Osąd z bloków ma własny cooldown wewnętrzny.

- Jak liczyć obrażenia Osądu w single target, szczególnie przy odnawianiu przed wygaśnięciem.

3C. Bezkarność
Opis: Gdy Egida jest aktywna, zyskujesz 40% cierni oraz szansę na Odwet.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- premia cierni podczas aktywnej Egidy: 40%

- dodatkowy efekt: szansa na Odwet

- dokładna szansa na Odwet: DO WERYFIKACJI

Widoczny opis słowa kluczowego:
- Odwet zapewnia szansę na pulsowanie cierni wokół ciebie, gdy wykonujesz blok.

- Obrażenia te liczą się jako umiejętność Molocha.

Interpretacja robocza:
- Modyfikator wzmacnia ciernie i dodaje/umożliwia mechanikę Odwetu w czasie aktywnej Egidy.

- Tooltip nie pokazuje liczbowej szansy na Odwet.

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                  Strona 8

Rzeczy do weryfikacji:
- Czy 40% cierni oznacza +40% do aktualnej wartości cierni, czy przyznanie cierni równych 40% jakiejś
statystyki.

- Jaka jest dokładna szansa na Odwet.

- Czy Odwet wymaga bloku i czy korzysta z bazowej 30% szansy na blok Egidy.

- Czy obrażenia Odwetu skalują się od cierni po zwiększeniu o 40%.

- Jak często pulsowanie cierni może trafić pojedynczy cel.

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                    Strona 9

3. Spadająca Gwiazda
Bazowa umiejętność
- Nazwa: Spadająca Gwiazda

- Ranga ze screena bazowego: 0/15

- Tagi: Odwaga, Adept, Mobilność

- Typ obrażeń: Obrażenia Świętości

- Koszt: brak widocznego kosztu zasobu

- Cooldown: 12 sek.

- Lucky Hit: Szansa na szczęśliwy traf: 24%

- Opis tooltipa: Wzbijasz się w niebo na anielskich skrzydłach i pikujesz na pole bitwy, zadając 2 570 [80%] pkt.
obrażeń w chwili startu i 7 710 [240%] pkt. obrażeń przy lądowaniu.

- Efekty aktywne: start / wybicie w niebo; 2 570 [80%] pkt. obrażeń w chwili startu; lądowanie / pikowanie; 7 710
[240%] pkt. obrażeń przy lądowaniu.

Uwagi single target / engine
- Bazowo widoczne są dwa komponenty obrażeń: start i lądowanie.

- Trzeba potwierdzić, czy pojedynczy cel może otrzymać oba komponenty w typowej rotacji single target.

Rzeczy do weryfikacji
- Czy oba komponenty mogą trafić ten sam cel.

- Czy start ma obszar działania wokół miejsca startu, a lądowanie wokół miejsca docelowego.

- Czy czas przebywania w powietrzu wpływa na DPS / uptime / animację.

Spadająca Gwiazda - grupa 1: wybór 1 z 2
1A. Dodatkowy Ładunek
Opis: Spadająca Gwiazda ma 1 dodatkowy ładunek.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- dodatkowe ładunki: +1

Interpretacja robocza:
- Modyfikator dodaje system ładunków albo zwiększa liczbę dostępnych użyć Spadającej Gwiazdy o 1.

- Bazowy tooltip pokazuje cooldown 12 sek., ale nie pokazuje liczby ładunków.

Rzeczy do weryfikacji:
- Bazowa liczba ładunków Spadającej Gwiazdy.

- Czy każdy ładunek odnawia się niezależnie przez bazowe 12 sek.

- Czy dodatkowy ładunek wpływa wyłącznie na burst/rotację, bez zmiany pojedynczego hitu.

1B. Odsłonięcie
Opis: Spadająca Gwiazda wywołuje odsłonięcie wrogów na 4 sek. po wylądowaniu.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- czas trwania Odsłonięcia: 4 sek.

- zwiększone otrzymywane obrażenia: 20%

Widoczny opis słowa kluczowego:

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                       Strona 10

- Odsłonięci wrogowie otrzymują obrażenia zwiększone o 20%.

Interpretacja robocza:
- Odsłonięcie jest nakładane po wylądowaniu, więc roboczo nie zakładamy, że zwiększa obrażenia samego
lądowania. Dla engine obowiązuje kolejność hit -> efekt, o ile testy później nie potwierdzą inaczej.

Rzeczy do weryfikacji:
- Czy Odsłonięcie jest nakładane tylko na cele trafione lądowaniem, czy na wszystkich wrogów w obszarze
lądowania.

- Czy czas 4 sek. zastępuje domyślne założenie projektu dla Exposed/Odsłonięcia, które wynosi 3 sek., gdy
tooltip nie podaje inaczej.

- Czy Odsłonięcie zwiększa obrażenia kolejnych hitów po lądowaniu, a nie obrażenia startu/lądowania tej samej
aktywacji.

Spadająca Gwiazda - grupa 2: wybór 1 z 2
2A. Obrażenia
Opis: Spadająca Gwiazda zadaje obrażenia zwiększone o 20%[x], jeśli zostanie użyta w ciągu 15 sek.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- premia do obrażeń: 20%[x]

- okno czasowe: 15 sek.

Interpretacja robocza:
- Tooltip sugeruje premię do obrażeń przy użyciu Spadającej Gwiazdy w określonym oknie czasowym, ale screen
nie pokazuje pełnego warunku odniesienia - "w ciągu 15 sek." od czego.

Rzeczy do weryfikacji:
- Czy chodzi o użycie Spadającej Gwiazdy w ciągu 15 sek. od poprzedniego użycia tej samej umiejętności.

- Czy premia dotyczy obu komponentów bazowych: startu i lądowania.

- Czy premia działa na dodatkowe komponenty z modyfikatorów grupy 3.

- Czy okno 15 sek. jest odświeżane kolejnymi użyciami.

2B. Redukcja Czasu Odnowienia
Opis: Zabicie odsłoniętego wroga skraca czas odnowienia Spadającej Gwiazdy o 0,5 sek.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- redukcja cooldownu za zabicie odsłoniętego wroga: 0,5 sek.

Widoczny opis słowa kluczowego:
- Odsłonięci wrogowie otrzymują obrażenia zwiększone o 20%.

Interpretacja robocza:
- Efekt zależy od zabicia celu z aktywnym Odsłonięciem.

- W typowym boss/single target może mieć niską wartość albo zerową, jeśli nie ma zabijanych dodatkowych
celów.

Rzeczy do weryfikacji:
- Czy redukcja działa tylko na zabicia wykonane przez Spadającą Gwiazdę, czy dowolnym źródłem obrażeń.

- Czy skraca cooldown jednego ładunku, wszystkich ładunków, czy aktualnie odnawiającego się ładunku.

- Czy w single target z jednym bossem bez addów efekt jest pomijany.

- Czy zabicie wroga odsłoniętego przez inne źródło także aktywuje redukcję.

Spadająca Gwiazda - grupa 3: wybór 1 z 3

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                      Strona 11

Uwaga o wartościach: część screenów grupy 3 pochodzi z rozwiniętego drzewka/postaci. Wartości liczbowe
zapisano ze screenów, ale skalowanie rang i statystyk wymaga późniejszej weryfikacji.

3A. Prędkość Światłości
Opis: Spadająca Gwiazda przy upadku przecina pole bitwy i powala wrogów na 2 sek., a także tworzy na twojej
drodze pęknięcie, które zadaje 289 406 [180%] pkt. obrażeń, a następnie wybucha i zadaje 96 468 [60%] pkt.
obrażeń.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- powalenie wrogów: 2 sek.

- obrażenia pęknięcia na drodze: 289 406 [180%] pkt. obrażeń

- obrażenia wybuchu pęknięcia: 96 468 [60%] pkt. obrażeń

Interpretacja robocza:
- Modyfikator dodaje/przekształca lądowanie w przejście przez pole bitwy oraz tworzy pęknięcie z dwoma
komponentami obrażeń.

Rzeczy do weryfikacji:
- Czy pęknięcie i jego wybuch są dodatkowymi komponentami obok bazowych obrażeń startu/lądowania, czy
zastępują część bazowego działania.

- Czy pojedynczy cel może zostać trafiony lądowaniem/przecięciem, pęknięciem i wybuchem.

- Czy pęknięcie zadaje obrażenia jednorazowo, liniowo po drodze, czy tickami.

- Czy wybuch pęknięcia ma opóźnienie i osobny hit.

- Jak traktować powalenie w DPS Engine.

3B. Upadek Gwiazdy
Opis: Przy upadku Spadająca Gwiazda zadaje 168 820 [105%] pkt. obrażeń i wybucha dodatkowo 3 razy.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- dodatkowe obrażenia przy upadku: 168 820 [105%] pkt. obrażeń

- liczba dodatkowych wybuchów: 3

Interpretacja robocza:
- Modyfikator dodaje dodatkowy komponent przy upadku i serię 3 dodatkowych wybuchów.

- Tooltip nie pokazuje jednoznacznie, czy wartość 168 820 [105%] dotyczy każdego z dodatkowych wybuchów,
pierwszego upadku, czy komponentu uruchamiającego wybuchy.

Rzeczy do weryfikacji:
- Czy 3 dodatkowe wybuchy mogą trafić ten sam pojedynczy cel.

- Czy każdy wybuch zadaje 168 820 [105%], czy tylko jeden komponent ma tę wartość.

- Czy wybuchy następują natychmiast, sekwencyjnie, czy z opóźnieniem.

- Czy obrażenia są dodatkowe wobec bazowego lądowania 7 710 [240%], czy zastępują lądowanie.

3C. Fanatyczne Zstąpienie
Opis: Spadająca Gwiazda staje się umiejętnością Zeloty i teraz zadaje przy lądowaniu 900 247 [560%] pkt.
obrażeń, osłabiając trafionych wrogów na 4 sek.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- nowy typ/kategoria: Zelota

- obrażenia przy lądowaniu: 900 247 [560%] pkt. obrażeń

- czas trwania Osłabienia: 4 sek.

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                     Strona 12

- redukcja obrażeń zadawanych przez zwykłych wrogów: 20%

- redukcja obrażeń zadawanych przez wrogów elitarnych: 15%

- redukcja obrażeń zadawanych przez bossów: 10%

Widoczny opis słowa kluczowego:
- Osłabieni wrogowie zadają obrażenia zmniejszone o 20% (zwykli wrogowie), 15% (wrogowie elitarni) lub 10%
(bossowie).

Interpretacja robocza:
- Modyfikator konwertuje Spadającą Gwiazdę w umiejętność Zeloty i wzmacnia/zmienia komponent lądowania.

- Osłabienie jest efektem defensywnym, bez bezpośredniego zwiększenia DPS.

Rzeczy do weryfikacji:
- Czy 900 247 [560%] zastępuje bazowy komponent lądowania, czy jest dodatkowym komponentem.

- Czy komponent startu 2 570 [80%] z bazowego tooltipa nadal występuje.

- Czy Osłabienie nakładane jest po zadaniu obrażeń lądowania, czy przed nimi.

- Czy efekt Osłabienia ma znaczenie w modelu DPS single target, czy tylko w modelu defensywnym.

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                 Strona 13

4. Mobilizacja
Bazowa umiejętność
- Nazwa: Mobilizacja

- Ranga ze screena bazowego: 0/15

- Tagi: Odwaga, Zelota

- Typ obrażeń: brak widocznego typu obrażeń

- Koszt: brak widocznego kosztu zasobu

- Ładunki: 3

- Cooldown: Czas odnowienia ładunku: 16 sek.

- Lucky Hit: brak widocznej wartości Lucky Hit

- Opis tooltipa: Ruszasz naprzód, generujesz 16 pkt. wiary i zyskujesz 15%[+] premii do szybkości ruchu na 6
sek.

- Efekty aktywne: ruch naprzód; generowanie 16 pkt. wiary; 15%[+] szybkości ruchu; czas trwania 6 sek.; 3
ładunki po 16 sek. odnowienia.

Uwagi single target / engine
- Bazowo brak widocznych obrażeń.

- Istotne dla rotacji jako generator wiary, mobilność i buff ruchu.

- Później trzeba zdecydować, czy ruch naprzód wpływa na uptime celu, czy jest efektem pozycyjnym bez
obrażeń.

Rzeczy do weryfikacji
- Czy każdy ładunek niezależnie odnawia się przez 16 sek.

- Czy premia do szybkości ruchu kumuluje się lub odświeża przy wielokrotnym użyciu.

- Czy ruch naprzód może przechodzić przez cel albo powodować ukryte trafienie bez obrażeń.

Mobilizacja - grupa 1: wybór 1 z 2
1A. Szansa na Trafienie Krytyczne
Opis: Użycie Mobilizacji zwiększa szansę na trafienie krytyczne o 5%[+], kiedy efekt jest aktywny.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- premia do szansy na trafienie krytyczne: 5%[+]

- warunek: kiedy efekt jest aktywny

Interpretacja robocza:
- Efekt najpewniej oznacza aktywny buff Mobilizacji, czyli bazowo 6 sek., ale trzeba to potwierdzić.

- Premia jest ofensywna i istotna dla DPS Engine, mimo że sama Mobilizacja nie zadaje bazowo obrażeń.

Rzeczy do weryfikacji:
- Czy premia trwa tyle samo co bazowa premia do szybkości ruchu Mobilizacji, czyli 6 sek.

- Czy przy ponownym użyciu premia odświeża czas trwania, czy może się kumulować.

- Czy premia wpływa na wszystkie źródła obrażeń, czy tylko na umiejętności Paladyna / aktywne skille.

1B. Premia do Czasu Działania
Opis: Wydłuża czas działania Mobilizacji o 4 sek.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.
Wartości:

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                    Strona 14

- wydłużenie czasu działania: 4 sek.

- bazowy czas działania Mobilizacji: 6 sek.

- roboczy czas działania po modyfikatorze: 10 sek.

Interpretacja robocza:
- Modyfikator wydłuża aktywny efekt Mobilizacji, czyli co najmniej premię do szybkości ruchu 15%[+].

Rzeczy do weryfikacji:
- Czy wydłużenie dotyczy wyłącznie premii do szybkości ruchu, czy wszystkich efektów aktywnej Mobilizacji.

- Czy wydłużenie wpływa na buffy z innych grup, jeśli zostaną połączone mechaniką spoza standardowego
wyboru.

- Czy kolejne użycia Mobilizacji odświeżają czas działania do pełnych 10 sek. przy tym modyfikatorze.

Mobilizacja - grupa 2: wybór 1 z 2
2A. Redukcja Kosztu
Opis: Czas odnowienia i koszt Mobilizacji są zmniejszone o 15%.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- redukcja czasu odnowienia Mobilizacji: 15%

- redukcja kosztu Mobilizacji: 15%

Interpretacja robocza:
- Bazowa Mobilizacja nie ma widocznego kosztu, tylko generuje 16 pkt. wiary, dlatego fragment o koszcie
zapisano dosłownie, ale jego praktyczne znaczenie wymaga weryfikacji.

- Bazowy czas odnowienia ładunku wynosi 16 sek.; redukcja 15% sugeruje 13,6 sek., ale nie zapisujemy tego
jako pewnej wartości docelowej bez potwierdzenia.

Rzeczy do weryfikacji:
- Co oznacza koszt Mobilizacji, skoro bazowy tooltip pokazuje generowanie wiary, a nie koszt.

- Czy redukcja 15% dotyczy czasu odnowienia każdego z 3 ładunków.

- Czy czas odnowienia po redukcji jest liczony jako 16 sek. x 0,85 = 13,6 sek., czy gra zaokrągla wartość.

- Czy redukcja wpływa na ewentualne koszty z modyfikatorów grupy 3.

2B. Nieograniczenie i Szybkość Ruchu
Opis: Użycie Mobilizacji zwiększa dodatkowo twoją szybkość ruchu o 5%[+] i zapewnia nieograniczenie, kiedy
efekt jest aktywny.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- dodatkowa premia do szybkości ruchu: 5%[+]

- bazowa premia Mobilizacji do szybkości ruchu: 15%[+]

- robocza łączna premia do szybkości ruchu przy aktywnym efekcie: 20%[+]

- czas trwania: najpewniej czas aktywnego efektu Mobilizacji, bazowo 6 sek. - DO WERYFIKACJI

Widoczny opis słowa kluczowego:
- Nieograniczone postacie mogą przenikać przez wrogów, a ich szybkość ruchu nie może zostać zmniejszona.

Interpretacja robocza:
- Modyfikator wzmacnia mobilność i zapewnia stan Nieograniczenia podczas aktywnej Mobilizacji.

- Sam w sobie nie dodaje jawnych obrażeń, ale może wpływać na uptime i pozycjonowanie.

Rzeczy do weryfikacji:

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                         Strona 15

- Czy dodatkowe 5%[+] sumuje się addytywnie z bazowym 15%[+] do 20%[+].

- Czy Nieograniczenie trwa dokładnie tyle, ile aktywny efekt Mobilizacji.

- Czy przy Premii do Czasu Działania z grupy 1 Nieograniczenie również trwałoby dłużej, jeśli efekty zostaną
połączone mechaniką spoza standardowego wyboru.

- Czy przechodzenie przez wrogów wpływa na model single target lub tylko na ruch/pozycjonowanie.

Mobilizacja - grupa 3: wybór 1 z 3
3A. Słowa Poświęcenia
Opis: Mobilizacja zużywa teraz 35% twojego maksymalnego zdrowia (4 938 pkt.) i może się kumulować
maksymalnie 3 razy.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- koszt zdrowia: 35% maksymalnego zdrowia

- wartość zdrowia ze screena: 4 938 pkt.

- maksymalna liczba kumulacji: 3

Interpretacja robocza:
- Modyfikator dodaje koszt w zdrowiu do Mobilizacji albo zastępuje dotychczasowy koszt/warunek użycia
kosztem zdrowia.

- Tooltip mówi o kumulowaniu do 3 razy, ale nie pokazuje jednoznacznie, który efekt się kumuluje.

Rzeczy do weryfikacji:
- Czy koszt 35% maksymalnego zdrowia jest płacony przy każdym użyciu Mobilizacji.

- Czy koszt zdrowia zastępuje koszt zasobu, czy jest dodatkowym kosztem.

- Co dokładnie kumuluje się maksymalnie 3 razy: premia Mobilizacji, efekt z tego modyfikatora, czy inny stan.

- Czy kolejne użycia odświeżają czas działania kumulacji, czy każda kumulacja ma osobny timer.

- Czy koszt zdrowia może zabić postać albo jest ograniczony do pozostawienia minimum zdrowia.

3B. Słowa Natchnienia
Opis: Mobilizacja zużywa wszystkie posiadane ładunki, skracając aktywne czasy odnowienia umiejętności
Sprawiedliwości o 3 sek. za każdy ładunek.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- zużycie: wszystkie posiadane ładunki Mobilizacji

- redukcja aktywnych cooldownów: 3 sek. za każdy ładunek

- dotyczy: umiejętności Sprawiedliwości

Interpretacja robocza:
- Modyfikator przekształca Mobilizację w narzędzie redukcji cooldownów dla umiejętności z kategorii/tagu
Sprawiedliwość.

- Przy bazowych 3 ładunkach maksymalna redukcja aktywnych czasów odnowienia wynosi roboczo 9 sek., ale
trzeba to potwierdzić w engine i przy ewentualnych dodatkowych ładunkach.

Rzeczy do weryfikacji:
- Które umiejętności należą do kategorii/tagu Sprawiedliwości.

- Czy redukcja obejmuje wszystkie aktywne cooldowny umiejętności Sprawiedliwości naraz, czy tylko jedną
umiejętność.

- Czy redukcja działa tylko na cooldowny już aktywne w momencie użycia Mobilizacji.

- Czy przy dodatkowym ładunku redukcja może przekroczyć 9 sek.

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                       Strona 16

- Czy zużycie wszystkich ładunków oznacza brak standardowego wielokrotnego użycia Mobilizacji i czy efekty
Mobilizacji aktywują się raz, czy per zużyty ładunek.

3C. Słowa Pokrzepienia
Opis: Mobilizacja generuje o 20 pkt. więcej wiary i zyskuje 1 dodatkowy ładunek.

Ograniczenie wyboru: Możesz wybrać tylko jedno ulepszenie.

Wartości:
- dodatkowa generacja wiary: +20 pkt. wiary

- bazowa generacja wiary Mobilizacji: 16 pkt. wiary

- robocza generacja wiary po modyfikatorze: 36 pkt. wiary

- dodatkowy ładunek: +1

- bazowa liczba ładunków Mobilizacji: 3

- robocza liczba ładunków po modyfikatorze: 4

Interpretacja robocza:
- Modyfikator wzmacnia Mobilizację jako generator zasobu i zwiększa liczbę dostępnych ładunków.

- Jest bezpośrednio istotny dla rotacji DPS przez większą generację wiary i większą dostępność Mobilizacji.

Rzeczy do weryfikacji:
- Czy +20 pkt. więcej wiary oznacza bazowe 16 + 20 = 36 pkt. wiary przy każdym użyciu.

- Czy dodatkowy ładunek odnawia się z tym samym czasem odnowienia co bazowe ładunki, czyli 16 sek. przed
modyfikatorami redukcji cooldownu.

- Czy przy połączeniu z Redukcją Kosztu z grupy 2 przez mechaniki spoza standardowego wyboru czas
odnowienia dodatkowego ładunku też jest redukowany.

- Czy dodatkowy ładunek wpływa na efekty zużywające wszystkie ładunki, np. Słowa Natchnienia, jeśli
połączenie byłoby możliwe mechaniką spoza standardowego drzewka.

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                          Strona 17

Podsumowanie stanu grupy Odwaga
Status: grupa Odwaga jest zamknięta opisowo. Wszystkie 4 umiejętności mają zapisaną bazę oraz grupy
modyfikatorów 1/2/3.

Umiejętności zamknięte opisowo
- Szarża z Tarczą - baza + grupa 1 + grupa 2 + grupa 3.

- Egida - baza + grupa 1 + grupa 2 + grupa 3.

- Spadająca Gwiazda - baza + grupa 1 + grupa 2 + grupa 3.

- Mobilizacja - baza + grupa 1 + grupa 2 + grupa 3.

Najważniejsze otwarte kwestie do późniejszego modelowania single
target
- Szarża z Tarczą: ticki / czas podtrzymywania; czy modyfikatory grupy 3 zastępują bazowe obrażenia czy dodają
komponenty; interakcje z blokiem, Odwetem, Animuszem i Osądem.

- Egida: wpływ prowokacji i bloku na symulację ataków wroga; interakcje z Animuszem, Osądem, Odwetem i
cierniami; czy efekty aktywacji automatycznej omijają cooldown.

- Spadająca Gwiazda: czy start i lądowanie mogą trafić ten sam cel; liczba trafień pęknięcia, wybuchów i
dodatkowych eksplozji; czy modyfikatory grupy 3 zastępują bazowe lądowanie.

- Mobilizacja: czy jest wyłącznie generatorem/buffem, jak działa koszt zdrowia, co kumuluje się przy Słowach
Poświęcenia oraz jak redukować cooldowny umiejętności Sprawiedliwości.

Notatki kontraktowe dla późniejszego DPS Engine
- Nie zgadujemy wartości liczbowych w implementacji ani testach.

- Wartości opisane jako robocze wyliczenia, np. 16 sek. x 0,85 = 13,6 sek. albo 16 + 20 = 36 wiary, nie są golden
values, dopóki nie zostaną potwierdzone w engine/testach.

- Jeśli tooltip mówi, że efekt następuje po trafieniu lub po wylądowaniu, roboczo obowiązuje kolejność: hit
najpierw liczy obrażenia, potem nakłada efekt, chyba że testy lub opis potwierdzą inaczej.

- Dla single target nie zakładamy automatycznie, że wszystkie fale, wybuchy, powalenia, nowe albo pęknięcia
trafiają pojedynczy cel wielokrotnie.

Diablo 4 DPS Engine - Paladyn - Odwaga                                                                          Strona 18
