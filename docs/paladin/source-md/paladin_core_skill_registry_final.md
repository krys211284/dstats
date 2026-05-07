# Paladyn - umiejętności główne / Core

## Metadane źródeł

| Pole | Wartosc |
|---|---|
| Poprzedni PDF | `docs/paladin/source-pdfs/paladin_core_skill_registry_final.pdf` |
| SHA-256 PDF | `f752d27f1dc0339b51d0e63ffd41e235002d685f25f1f44e65a6252dcfd0e1b9` |
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

### Błogosławiona Tarcza / Blessed Shield (`blogoslawiona_tarcza`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Blessed Shield.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Blessed+Shield` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [205%] | 205% |
| 2 | Damage: [226%] | 226% |
| 3 | Damage: [246%] | 246% |
| 4 | Damage: [266%] | 266% |
| 5 | Damage: [297%] | 297% |
| 6 | Damage: [318%] | 318% |
| 7 | Damage: [338%] | 338% |
| 8 | Damage: [359%] | 359% |
| 9 | Damage: [379%] | 379% |
| 10 | Damage: [410%] | 410% |
| 11 | Damage: [430%] | 430% |
| 12 | Damage: [451%] | 451% |
| 13 | Damage: [471%] | 471% |
| 14 | Damage: [492%] | 492% |
| 15 | Damage: [523%] | 523% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Błogosławiony Młot / Blessed Hammer (`blogoslawiony_mlot`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Blessed Hammer.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Blessed+Hammer` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [115%] | 115% |
| 2 | Damage: [126%] | 126% |
| 3 | Damage: [138%] | 138% |
| 4 | Damage: [149%] | 149% |
| 5 | Damage: [167%] | 167% |
| 6 | Damage: [178%] | 178% |
| 7 | Damage: [190%] | 190% |
| 8 | Damage: [201%] | 201% |
| 9 | Damage: [213%] | 213% |
| 10 | Damage: [230%] | 230% |
| 11 | Damage: [241%] | 241% |
| 12 | Damage: [253%] | 253% |
| 13 | Damage: [264%] | 264% |
| 14 | Damage: [276%] | 276% |
| 15 | Damage: [293%] | 293% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Boska Lanca / Divine Lance (`boska_lanca`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Divine Lance.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Divine+Lance` |
| Status tabeli rang | `SINGLE_COMPONENT_PERCENT_BUT_MULTI_HIT_RUNTIME_NEEDS_MODEL` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [90%] | 90% |
| 2 | Damage: [99%] | 99% |
| 3 | Damage: [108%] | 108% |
| 4 | Damage: [117%] | 117% |
| 5 | Damage: [131%] | 131% |
| 6 | Damage: [139%] | 139% |
| 7 | Damage: [148%] | 148% |
| 8 | Damage: [157%] | 157% |
| 9 | Damage: [166%] | 166% |
| 10 | Damage: [180%] | 180% |
| 11 | Damage: [189%] | 189% |
| 12 | Damage: [198%] | 198% |
| 13 | Damage: [207%] | 207% |
| 14 | Damage: [216%] | 216% |
| 15 | Damage: [229%] | 229% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Uderzenie Tarczą / Shield Bash (`uderzenie_tarcza`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Shield Bash.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Shield+Bash` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [205%] | 205% |
| 2 | Damage: [226%] | 226% |
| 3 | Damage: [246%] | 246% |
| 4 | Damage: [266%] | 266% |
| 5 | Damage: [297%] | 297% |
| 6 | Damage: [318%] | 318% |
| 7 | Damage: [338%] | 338% |
| 8 | Damage: [359%] | 359% |
| 9 | Damage: [379%] | 379% |
| 10 | Damage: [410%] | 410% |
| 11 | Damage: [430%] | 430% |
| 12 | Damage: [451%] | 451% |
| 13 | Damage: [471%] | 471% |
| 14 | Damage: [492%] | 492% |
| 15 | Damage: [523%] | 523% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Zapał / Zeal (`zapal`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Zeal.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Zeal` |
| Status tabeli rang | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` |
| Wykryte metryki z HTML | Damage, Additional Strikes Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [80%] Additional Strikes Damage: [20%] | 80%, 20% |
| 2 | Damage: [88%] Additional Strikes Damage: [22%] | 88%, 22% |
| 3 | Damage: [96%] Additional Strikes Damage: [24%] | 96%, 24% |
| 4 | Damage: [104%] Additional Strikes Damage: [26%] | 104%, 26% |
| 5 | Damage: [116%] Additional Strikes Damage: [29%] | 116%, 29% |
| 6 | Damage: [124%] Additional Strikes Damage: [31%] | 124%, 31% |
| 7 | Damage: [132%] Additional Strikes Damage: [33%] | 132%, 33% |
| 8 | Damage: [140%] Additional Strikes Damage: [35%] | 140%, 35% |
| 9 | Damage: [148%] Additional Strikes Damage: [37%] | 148%, 37% |
| 10 | Damage: [160%] Additional Strikes Damage: [40%] | 160%, 40% |
| 11 | Damage: [168%] Additional Strikes Damage: [42%] | 168%, 42% |
| 12 | Damage: [176%] Additional Strikes Damage: [44%] | 176%, 44% |
| 13 | Damage: [184%] Additional Strikes Damage: [46%] | 184%, 46% |
| 14 | Damage: [192%] Additional Strikes Damage: [48%] | 192%, 48% |
| 15 | Damage: [204%] Additional Strikes Damage: [51%] | 204%, 51% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

## Treść przekonwertowana z lokalnego PDF

Poniższa sekcja zachowuje tekst dawnego PDF-a w formie edytowalnej Markdown. W razie konfliktu z sekcją tabel rang należy dopisać notatkę weryfikacyjną, a nie nadpisywać danych liczbowych bez sprawdzenia źródła.

Paladyn - umiejętności główne / Core
Wersja finalna na podstawie aktualnie potwierdzonych screenów, lvl15 i korekt użytkownika

1. Założenia dokumentu
Dokument obejmuje grupę Core / Główne dla Paladyna. Źródłem prawdy są screeny użytkownika, w tym paczki 0/15,
3/15, 15/15 oraz dodatkowe screeny weryfikacyjne. Paczka lvl15 służy zarówno do weryfikacji skalowania rang, jak i do
potwierdzania tekstów, które były nieczytelne na niższych rangach.

- Każdy skill ma trzy grupy modyfikatorów 2 / 2 / 3; z każdej grupy można wybrać maksymalnie jeden
modyfikator.

- Wartości aktualnych obrażeń mogą zależeć od stanu postaci i buildu. Współczynniki procentowe zapisujemy
jako kluczowe dla przyszłego modelu.

- Rykoszety, bumerangi, włócznie, wybuchy i dodatkowe trafienia wymagają późniejszych testów single
target.

2. Tabela porównawcza - Core / Główne
Skill            Tagi                 Typ        Koszt    Lucky Hit     Współczynnik / snapshot       Status

Błogosławiona    Główne, Sędzia       Świętość   28       30% 0/15;     216% 0/15; 259% 3/15          spisana
Tarcza                                                    39% 3/15

Błogosławiony    Główne, Sędzia       Świętość   10       31% 3/15;     138% 3/15; 293% 15/15         tekstowo prawie zamknięty
Młot                                                      24% 15/15

Boska Lanca      Główne, Adept,       Świętość   25       6% 0/15; 8%   2 x 90% 0/15; 2 x 108% 3/15   częściowo zamknięta
Mobilność                                3/15

Uderzenie        Główne, Mobilność,   Fizyczne   32       16%           205% bazowo                   częściowo zamknięte
Tarczą           Moloch

Zapał            Główne, Zelota       Fizyczne   20       3%            80% + 3 x 20%                 część modów do
potwierdzenia

3. Błogosławiona Tarcza
Bazowo: koszt 28 pkt. wiary, obrażenia Świętości, wymaga tarczy, tagi Główne i Sędzia. Rzucasz tarczę, która
rykoszetuje maksymalnie 3 razy.

- 0/15: Lucky Hit 30%, 0 [216%] pkt. obrażeń.

- 3/15: Lucky Hit 39%, 272 462 [259%] pkt. obrażeń; następna ranga 295 168 [281%].

Grupa 1
- Generowanie Wiary: gdy trafi wroga o ograniczonej kontroli, otrzymujesz 5 pkt. wiary.

- Szybkość Użycia: 20%[+] premii do szybkości użycia.

Grupa 2
- Premia do Obrażeń: dodatkowo 75% obrażeń za każdy punkt szansy na blok z tarczy.

- Dodatkowy Pancerz i Szansa na Blok: +2%[+] pancerza i +2%[+] szansy na blok na 6 sek. za każdym
razem, gdy trafia wroga.

Grupa 3
- Tarcza Sprawiedliwości: powoduje wybuch Osądu i rykoszetuje dodatkowo 5 razy; każdy wybuch Osądu
wzmacnia następny rzut o 40%[x], maksymalnie 5 razy.

- Tarcza Ożywieńca: zawraca jak bumerang i zamiast rykoszetować, przeszywa cele; 0 [270%]. Po trafieniu
obrażenia +5% na 6 sek., maksymalnie 5 razy.
- Tarcza Pomsty: staje się Molocha, leci w linii prostej, wywołuje impuls cierni przez 3 sek., potem wybucha za
0 [378%].

4. Błogosławiony Młot
Status: zaktualizowany po screenach 3/15, 15/15 oraz dodatkowych screenach Budującej Walki i Apostolskiej
Aureoli.

- 3/15: koszt bazowy 10 pkt. wiary, Lucky Hit 31%, tagi Główne i Sędzia, obrażenia Świętości. Rzucasz
Błogosławiony Młot, który porusza się po spirali, zadając 147 781 [138%] pkt. obrażeń; następna ranga 160
097 [149%].

- 15/15: koszt 10 pkt. wiary, Lucky Hit 24%, 0 [293%] pkt. obrażeń. Różnica Lucky Hit między snapshotami
wymaga późniejszego porównania w identycznych warunkach.

Grupa 1
- Redukcja Kosztu: Użycie Błogosławionego Młota zmniejsza jego koszt o 5% na 2 sek., maksymalnie do 5
razy.

- Premia do Obrażeń: trafianie wrogów Błogosławionym Młotem zwiększa jego obrażenia o 5%[x],
maksymalnie do 5 razy, na 3 sek.

Grupa 2
- Zwiększenie Szybkości Użycia: Błogosławiony Młot zyskuje 15%[+] szybkości użycia, a jego młoty
przemieszczają się 25%[+] szybciej.

- Spowolnienie: Błogosławiony Młot spowalnia wrogów o 50% na 5 sek.

Grupa 3
- Budująca Walka: Błogosławiony Młot wyrzuca 3 młoty, zadając 111 666 [104%] pkt. obrażeń celom na
obszarze trafienia. Wcześniejszy snapshot pokazywał 0 [67%].

- Apostolska Aureola: Błogosławiony Młot staje się umiejętnością Adepta. Podąża za tobą i okrąża cię, zadając
wrogom 191 427 [178%] pkt. obrażeń i atakując o 20% szybciej.

- Druzgocący Cios: na screenie 15/15 Błogosławiony Młot zadaje 0 [293%] pkt. obrażeń i przy trafieniu
wywołuje 3 mniejsze wybuchy, z których każdy zadaje 0 [117%] pkt. obrażeń. Wcześniejszy snapshot: 0
[115%] + 3 x 0 [46%].

5. Boska Lanca
- 0/15: koszt 25, Lucky Hit 6%, 2 trafienia po 0 [90%].

- 3/15: koszt 25, Lucky Hit 8%, tagi Główne, Adept, Mobilność. Nabijasz wrogów na niebiańską lancę, która
uderza 2 razy, każde trafienie 115 655 [108%]; następna ranga 125 293 [117%].

Grupa 1
- Premia do Obrażeń: obrażenia Boskiej Lancy zwiększają się o 15%[x]; premia jest podwojona przeciw
odsłoniętym wrogom. Status: prawie pewne.

- Redukcja Kosztu: zabijanie wrogów Boską Lancą zmniejsza koszt użycia kolejnej Boskiej Lancy o 10%,
maksymalnie do 50%. Status: do potwierdzenia pełnego brzmienia.

Grupa 2
- Szybkość Użycia: Boska Lanca zyskuje 20%[+] premii do szybkości użycia. Potwierdzone.

- Skumulowane Obrażenia: roboczo - kolejne trafienia Boską Lancą w ten sam cel zadają zwiększone
obrażenia, maksymalnie do 30%. Status: do weryfikacji.

Grupa 3
- Żarliwy Rzut: Boska Lanca staje się umiejętnością Zeloty. Uderza na większym obszarze 2 dodatkowymi
włóczniami, które zadają po 112 041 [105%] pkt. obrażeń. Obrażenia od trafień krytycznych są zwiększone o
10%[x]. Potwierdzone.

- Boski Oszczep: roboczo - rzucasz Boską Lancę, która zadaje 0 [90%] pierwszemu trafionemu wrogowi,
potem wybucha za ok. 0 [150%]. Do potwierdzenia.

- Trzeci wariant grupy 3: nadal wymaga nazwy i pełnego opisu, jeśli nie został już pokryty jednym z
powyższych screenów.

6. Uderzenie Tarczą
Bazowo: koszt 32 pkt. wiary, Lucky Hit 16%, obrażenia Fizyczne, wymaga tarczy, tagi Główne, Mobilność,
Moloch. Szarżujesz na wroga przed sobą i uderzasz go, zadając 0 [205%] pkt. obrażeń.

Grupa 1
- Uderzenia są Blokowaniem: trafienia Uderzeniem Tarczą liczą się jako zablokowanie ataku; efekt raz na
użycie. Status: do potwierdzenia pełnego brzmienia.

- Premia do Rozmiaru: Uderzenie Tarczą zwiększa swój rozmiar o 40%. Status: do potwierdzenia pełnego
brzmienia.

Grupa 2
- Oblężenie: za każdym razem gdy masz Animusz, Uderzenie Tarczą ma obszar trafienia zwiększony o 50%[x]
i zadaje 149 633 [381%] pkt. obrażeń. Zyskujesz 1 pkt. Animuszu za każdego trafionego wroga. Potwierdzone.

- Porażenie: Uderzenie Tarczą poraża pierwszego trafionego wroga, zadając 174 572 [445%] pkt. obrażeń i
ogłuszając go na 1 sek. Następnie odrzuca wrogów z tyłu i zadaje im 62 347 [159%] pkt. obrażeń.
Potwierdzone.

Grupa 3
- Wyłom: trafienia Uderzeniem Tarczą zapewniają 10%[+] szans na Odwet na 5 sek.; efekt kumuluje się
maksymalnie 5 razy. Potwierdzone.

- Odległość: Uderzenie Tarczą ma teraz większy zasięg. Potwierdzone.

- Premia do Obrażeń: roboczo - Uderzenie Tarczą zadaje obrażenia zwiększone o 100%[+] swojemu
głównemu celowi. Do potwierdzenia.

7. Zapał
Bazowo: koszt 20 pkt. wiary, Lucky Hit 3%, obrażenia Fizyczne, tagi Główne i Zelota. Atakujesz wrogów z
oślepiającą szybkością, zadając 0 [80%] pkt. obrażeń i natychmiast wyprowadzasz 3 dodatkowe ataki, każdy
po 0 [20%].

Grupa 1
- Osłabienie: Zapał osłabia wrogów na 4 sek. Osłabieni zadają mniej obrażeń: 20% zwykli, 15% elitarni, 10%
bossowie.

- Szansa na Trafienie Krytyczne: Zapał ma dodatkowo 6%[+] szansy na trafienie krytyczne; premia jest
podwojona przeciw osłabionym wrogom.

Grupa 2
- Umocnienie: roboczo - krytyczne trafienie Zapałem daje szansę na umocnienie równe części maksymalnego
zdrowia. Wartość 1% szans jest podejrzana i wymaga potwierdzenia.

- Dodatkowe Ciosy: Zapał zyskuje 2 dodatkowe uderzenia.

Grupa 3
- Dziedzictwo Zeloty: Zapał wydłuża natarcie, wyszukując losowych pobliskich wrogów i atakując ich
dodatkowo 4 razy.
- Śmierć albo Chwała: roboczo - Zapał zamiast wiary zużywa 10% maksymalnego zdrowia i zadaje obrażenia
dookoła; po utracie zdrowia zwiększa obrażenia Zapału o 60% i szansę na trafienie krytyczne o 20% na 6 sek.
Do potwierdzenia pełnego brzmienia.

- Ostatni wariant grupy 3: roboczo - efekt rozplatania Zapału ma większy zasięg, +2 dodatkowe ciosy i
+25%[x] obrażeń przeciw osłabionym wrogom. Brakuje nazwy.

8. Rejestr mechanik z Core
- Ograniczona kontrola: warunek dla generowania Wiary przez Błogosławioną Tarczę.

- Rykoszet, bumerang, przebijanie i dodatkowe włócznie: wymagają osobnej interpretacji single target.

- Osąd: oznaczenie na 3 sek., po wygaśnięciu zadaje 80% obrażeń.

- Animusz: pancerz +25%[+], obrażenia bezpośrednie zużywają ładunek, limit 8.

- Odwet: szansa na pulsowanie cierni wokół postaci podczas bloku; obrażenia liczą się jako umiejętność
Molocha.

- Koszt zdrowia zamiast Wiary: pojawia się przy Zapał / Śmierć albo Chwała.

- Hit count: kilka skilli Core ma wiele trafień lub dodatkowe wybuchy; runtime musi modelować je jawnie.

9. Pozostałe otwarte punkty Core
- Błogosławiony Młot: tekstowo niemal zamknięty; pozostała tylko spójna tabela skalowania snapshotów.

- Boska Lanca: do potwierdzenia Redukcja Kosztu, Skumulowane Obrażenia, Boski Oszczep i ewentualny
trzeci wariant grupy 3.

- Uderzenie Tarczą: do potwierdzenia Uderzenia są Blokowaniem, Premia do Rozmiaru i Premia do Obrażeń
100%[+].

- Zapał: do potwierdzenia Umocnienie, Śmierć albo Chwała oraz nazwa ostatniego wariantu grupy 3.

- Globalnie: screeny i testy single target dla rykoszetów, bumerangów, włóczni, dodatkowych trafień i
wybuchów.
