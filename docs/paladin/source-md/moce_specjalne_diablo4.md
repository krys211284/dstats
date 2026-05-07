# Paladyn - Moce Specjalne

## Metadane źródeł

| Pole | Wartosc |
|---|---|
| Poprzedni PDF | `docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf` |
| SHA-256 PDF | `a559c9ddd65c0a64d31a5efbec2baae4a6db6aaa466060665736f580b0adefc0` |
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

### Furia Niebios / Heaven's Fury (`furia_niebios`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Heaven's Fury.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Heaven's+Fury` |
| Status tabeli rang | `MANUAL_REVIEW_MULTI_PHASE_OR_TABLE_AMBIGUITY` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [200%] | 200% |
| 2 | Damage: [66%] | 66% |
| 3 | Damage: [72%] | 72% |
| 4 | Damage: [78%] | 78% |
| 5 | Damage: [87%] | 87% |
| 6 | Damage: [93%] | 93% |
| 7 | Damage: [99%] | 99% |
| 8 | Damage: [105%] | 105% |
| 9 | Damage: [111%] | 111% |
| 10 | Damage: [120%] | 120% |
| 11 | Damage: [126%] | 126% |
| 12 | Damage: [132%] | 132% |
| 13 | Damage: [138%] | 138% |
| 14 | Damage: [144%] | 144% |
| 15 | Damage: [153%] | 153% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Forteca / Fortress (`forteca`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Fortress.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Fortress` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Defensive Area Duration |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Defensive Area Duration: 8.0 seconds | - |
| 2 | Defensive Area Duration: 8.1 | - |
| 3 | Defensive Area Duration: 8.2 | - |
| 4 | Defensive Area Duration: 8.3 | - |
| 5 | Defensive Area Duration: 8.4 | - |
| 6 | Defensive Area Duration: 8.5 | - |
| 7 | Defensive Area Duration: 8.6 | - |
| 8 | Defensive Area Duration: 8.6 | - |
| 9 | Defensive Area Duration: 8.7 | - |
| 10 | Defensive Area Duration: 8.8 | - |
| 11 | Defensive Area Duration: 8.8 | - |
| 12 | Defensive Area Duration: 8.9 | - |
| 13 | Defensive Area Duration: 8.9 | - |
| 14 | Defensive Area Duration: 8.9 | - |
| 15 | Defensive Area Duration: 9.0 | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Zenit / Zenith (`zenit`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Zenith.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Zenith` |
| Status tabeli rang | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` |
| Wykryte metryki z HTML | First Strike Damage, Second Strike Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | First Strike Damage: [450%] Second Strike Damage: [400%] | 450%, 400% |
| 2 | First Strike Damage: [495%] Second Strike Damage: [440%] | 495%, 440% |
| 3 | First Strike Damage: [540%] Second Strike Damage: [480%] | 540%, 480% |
| 4 | First Strike Damage: [585%] Second Strike Damage: [520%] | 585%, 520% |
| 5 | First Strike Damage: [653%] Second Strike Damage: [580%] | 653%, 580% |
| 6 | First Strike Damage: [697%] Second Strike Damage: [620%] | 697%, 620% |
| 7 | First Strike Damage: [742%] Second Strike Damage: [660%] | 742%, 660% |
| 8 | First Strike Damage: [788%] Second Strike Damage: [700%] | 788%, 700% |
| 9 | First Strike Damage: [832%] Second Strike Damage: [740%] | 832%, 740% |
| 10 | First Strike Damage: [900%] Second Strike Damage: [800%] | 900%, 800% |
| 11 | First Strike Damage: [945%] Second Strike Damage: [840%] | 945%, 840% |
| 12 | First Strike Damage: [990%] Second Strike Damage: [880%] | 990%, 880% |
| 13 | First Strike Damage: [1,035%] Second Strike Damage: [920%] | 1035%, 920% |
| 14 | First Strike Damage: [1,080%] Second Strike Damage: [960%] | 1080%, 960% |
| 15 | First Strike Damage: [1,147%] Second Strike Damage: [1,020%] | 1147%, 1020% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Arbiter Sprawiedliwości / Arbiter of Justice (`arbiter_sprawiedliwosci`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Arbiter of Justice.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Arbiter+of+Justice` |
| Status tabeli rang | `MANUAL_REVIEW_HTML_PDF_MISMATCH_OR_TABLE_AMBIGUITY` |
| Wykryte metryki z HTML | Damage, Arbiter Duration |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [600%] Arbiter Duration: 20.0 seconds | 600% |
| 2 | Damage: [660%] Arbiter Duration: 20.8 | 660% |
| 3 | Damage: [720%] Arbiter Duration: 21.6 | 720% |
| 4 | Damage: [780%] Arbiter Duration: 22.4 | 780% |
| 5 | Damage: [870%] Arbiter Duration: 23.2 | 870% |
| 6 | Damage: [930%] Arbiter Duration: 23.8 | 930% |
| 7 | Damage: [990%] Arbiter Duration: 24.4 | 990% |
| 8 | Damage: [1,050%] Arbiter Duration: 25.0 | 1050% |
| 9 | Damage: [1,110%] Arbiter Duration: 25.6 | 1110% |
| 10 | Damage: [1,200%] Arbiter Duration: 26.0 | 1200% |
| 11 | Damage: [1,260%] Arbiter Duration: 26.4 | 1260% |
| 12 | Damage: [1,320%] Arbiter Duration: 26.8 | 1320% |
| 13 | Damage: [1,380%] Arbiter Duration: 27.2 | 1380% |
| 14 | Damage: [1,440%] Arbiter Duration: 27.6 | 1440% |
| 15 | Damage: [530%] Arbiter Duration: 28.0 | 530% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

## Treść przekonwertowana z lokalnego PDF

Poniższa sekcja zachowuje tekst dawnego PDF-a w formie edytowalnej Markdown. W razie konfliktu z sekcją tabel rang należy dopisać notatkę weryfikacyjną, a nie nadpisywać danych liczbowych bez sprawdzenia źródła.

Paladyn - Moce Specjalne                                                                                                 Strona 1

Paladyn - Moce Specjalne
Rejestr źródłowy umiejętności i ulepszeń. Plik przeznaczony do zastąpienia
docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf.

Zakres dokumentu
Ten PDF obejmuje cztery wpisy umiejętności w drzewie Mocy Specjalnych: Furia Niebios, Forteca,
Zenit oraz Arbiter Sprawiedliwości. Układ grup dla Furii Niebios, Fortecy, Zenitu i Arbitra
Sprawiedliwości wynosi [2, 2, 3].

Umiejętność                   Typy / tagi opisowe              Układ grup        Uwagi

Furia Niebios                 Specjalne, Sędzia                [2, 2, 3]         Poprawny układ: 2 ulepszenia w
grupie 1, 2 w grupie 2, 3 w grupie 3.

Forteca                       Specjalne, Defensywa,            [2, 2, 3]         Forteca jest wpisem umiejętności w
Moloch                                             drzewie Mocy Specjalnych;
Cierniowa Reduta jest jej
ulepszeniem w grupie 3.

Zenit                         Specjalne, Zelota                [2, 2, 3]         Układ zgodny z poprawioną wersją:
grupa 3 ma trzy warianty.

Arbiter Sprawiedliwości       Specjalne, Adept, Mobilność      [2, 2, 3]         Wpis zawiera grupy Skrzydeł oraz
Boską Interwencję.

Warunki kontrolne dla repozytorium
-     Furia Niebios zawiera sekcję "1. Furia Niebios" oraz ulepszenia: Ostateczna Sprawiedliwość, Krok w
Światłości, Potrojenie.
-     Forteca jest wpisem umiejętności w drzewie Mocy Specjalnych, a Cierniowa Reduta pozostaje
ulepszeniem Fortecy w grupa_3.
-     Zenit zachowuje układ [2, 2, 3].
-     Arbiter Sprawiedliwości zachowuje komplet grup 1/2/3.

Źródłowy rejestr opisowy Mocy Specjalnych - wygenerowany do podmiany docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf
1. Furia Niebios
Specjalne          Sędzia

Czas odnowienia                18,41 sek.

Szansa na szczęśliwy           4%
traf

Opis bazowy                    Chwytasz Światłość, zadając wokół siebie 401 606 [200%] pkt. obrażeń na
sekundę, po czym uwalniasz ją. Światłość wyszukuje pobliskich wrogów i zadaje
im 144 578 [72%] pkt. obrażeń przy każdym trafieniu przez 7 sek.

Typ obrażeń                    Obrażenia Świętości

Układ grup potwierdzony: grupa_1: Czas Działania, Spowolnienie; grupa_2: Osąd, Premia do Obrażeń; grupa_3:
Ostateczna Sprawiedliwość, Krok w Światłości, Potrojenie.

Grupy ulepszeń
Grupa            Ulepszenie                     Opis

grupa_1          Czas Działania                 Wydłuża czas działania Furii Niebios o 4 sek.

Spowolnienie                   Furia Niebios spowalnia wrogów o 50% na 5 sek.

grupa_2          Osąd                           Światło Furii Niebios Osądza wrogów.

Premia do Obrażeń              Wrogowie trafieni Furią Niebios odnoszą obrażenia od Furii Niebios
zwiększone o 20%, aż do śmierci.

grupa_3          Ostateczna                     Furii Niebios można teraz używać wszędzie. Światłość koncentruje
Sprawiedliwość                 się, rośnie i przyciąga wrogów, zadając 239 383 [105%] pkt.
obrażeń, a następnie wybucha, zadając 2 872 605 [1260%] pkt.
obrażeń. Czas odnowienia Furii Niebios jest zwiększony o 10 sek.

Krok w Światłości              Furia Niebios rozdziela się na 5 orbitujących promieni, z których
każdy zadaje przy trafieniu 177 827 [78%] pkt. obrażeń przez 7 sek.

Potrojenie                     Furia Niebios rozdziela się na 3 promienie, z których każdy zadaje
przy trafieniu 129 951 [57%] pkt. obrażeń przez 7 sek.

Układ grup: [2, 2, 3].

Źródłowy rejestr opisowy Mocy Specjalnych - wygenerowany do podmiany docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf
2. Forteca
Specjalne        Defensywa           Moloch

Czas odnowienia                36,83 sek.

Opis bazowy                    Okazujesz determinację, zyskując niewrażliwość na 3,0 sek. i tworząc wokół siebie
strefę ochronną na 8,0 sek.

Efekt obszaru                  Przebywanie w obrębie Fortecy zapewnia tobie i twoim sojusznikom 9 poziomów
kumulacji Animuszu co 0,5 sek.

Typ obrażeń                    Obrażenia Fizyczne

Układ grup potwierdzony: grupa_1: Nieustępliwość, Użycie bez Zużywania Zasobów; grupa_2: Premia do Obrażeń
Animuszu, Czas Działania; grupa_3: Barykada, Cierniowa Reduta, Okopanie.

Grupy ulepszeń
Grupa            Ulepszenie                     Opis

grupa_1          Nieustępliwość                 Na obszarze działania twojej Fortecy zyskujesz nieustępliwość.

Użycie bez Zużywania           Na obszarze działania twojej Fortecy umiejętności nie zużywają
Zasobów                        zasobów.

grupa_2          Premia do Obrażeń              Zyskujesz premię 4,0%[x] do obrażeń za każdy poziom kumulacji
Animuszu                       Animuszu na obszarze działania twojej Fortecy.

Czas Działania                 Wydłuża czas działania Fortecy o 4 sek.

grupa_3          Barykada                       Zabijanie wrogów w obrębie swojej Fortecy wydłuża jej działanie o
0,5 sek., do maksymalnie 20 sek. Możesz ponownie użyć Fortecy,
by przenieść ją w miejsce, w którym obecnie jesteś.

Cierniowa Reduta               Wrogowie w obrębie Fortecy zostają spowolnieni o 50% i odnoszą
500% twoich obrażeń od cierni na sekundę.

Okopanie                       Dopóki pozostajesz w obrębie swojej Fortecy, twoje zdrowie nie
może spaść poniżej 1 pkt. Czas działania Fortecy jest skrócony o
50%.

Układ grup: [2, 2, 3].

Źródłowy rejestr opisowy Mocy Specjalnych - wygenerowany do podmiany docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf
3. Zenit
Specjalne          Zelota

Czas odnowienia                15,34 sek.

Szansa na szczęśliwy           39%
traf

Opis bazowy                    Przywołujesz boski miecz, który rozpłatuje pole bitwy, zadając 245 378 [450%]
pkt. obrażeń.

Ponowne użycie                 Ponowne użycie Zenitu przecina pole bitwy, zadając 218 113 [400%] pkt. obrażeń
i powalając wrogów na 2 sek.

Typ obrażeń                    Obrażenia Fizyczne

Układ grup potwierdzony: grupa_1: Szansa na Trafienie Krytyczne, Osłabienie; grupa_2: Nieustępliwość, Osłabienie;
grupa_3: Empirejska Klinga, Rozdarcie, Homilia Stali.

Grupy ulepszeń
Grupa            Ulepszenie                     Opis

grupa_1          Szansa na Trafienie            Użycie umiejętności Zenit zwiększa o 5%[+] szansę na trafienie
Krytyczne                      krytyczne na 8 sek.

Osłabienie                     Zenit wywołuje osłabienie wrogów.

grupa_2          Nieustępliwość                 Podczas działania Zenitu zyskujesz nieustępliwość.

Osłabienie                     Zabijanie osłabionych wrogów podczas działania Zenitu skraca jego
czas odnowienia o 2 sek.

grupa_3          Empirejska Klinga              Zenit jest od teraz również umiejętnością Mobilności, która
powoduje doskok do wrogów; zyskuje 25%[+] premii do obrażeń od
trafień krytycznych.

Rozdarcie                      Zenit jednym trafieniem tworzy w ziemi wyrwę, powalając wrogów i
zadając im 545 037 [900%] pkt. obrażeń. Po upływie 1 sek. wyrwa
wybucha, zadając 408 778 [675%] pkt. obrażeń.

Homilia Stali                  Zenit uwalnia twój gniew, zadając 95 381 [157%] pkt. obrażeń, i
można używać go bez ograniczeń przez 4 sek., zanim rozpocznie
się jego czas odnowienia. Drugie użycie Zenitu wydłuża ten czas
trwania o 1 sek. zamiast powalać wrogów.

Układ grup: [2, 2, 3].

Źródłowy rejestr opisowy Mocy Specjalnych - wygenerowany do podmiany docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf
4. Arbiter Sprawiedliwości
Specjalne             Adept         Mobilność

Czas odnowienia                 61,38 sek.

Szansa na szczęśliwy            22%
traf

Opis bazowy                     Wznosisz się ku niebiosom i opadasz na pole bitwy jako Arbiter przez 57,0 sek.,
zadając po wylądowaniu 2 388 993 [1530%] pkt. obrażeń.

Modyfikatory postaci            Postać Arbitra gwarantuje 15%[x] premii do szybkości ruchu i zastępuje odskok
anielskim skokiem. Dodatkowo uderzenia skrzydeł wokół ciebie zadają 160%
obrażeń.

Układ grup potwierdzony: grupa_1: Szybkość Ruchu, Czas Działania; grupa_2: Ponowne Użycie Uderzenia Skrzydeł,
Redukcja Czasu Odnowienia; grupa_3: Skrzydła Serafina, Skrzydła Sprawiedliwości, Boska Interwencja.

Grupy ulepszeń
Grupa            Ulepszenie                     Opis

grupa_1          Szybkość Ruchu                 Arbiter Sprawiedliwości zapewnia 30%[x] premii do szybkości
ruchu, gdy jesteś w postaci Arbitra.

Czas Działania                 Czas działania Arbitra jest wydłużony o 10 sek.

grupa_2          Ponowne Użycie                 Opóźnienie przed ponownym użyciem uderzeń skrzydeł Arbitra
Uderzenia Skrzydeł             zostaje skrócone o 0,5 sek.

Redukcja Czasu                 Skraca czas odnowienia Arbitra Sprawiedliwości o 20 sek.
Odnowienia

grupa_3          Skrzydła Serafina              Postać Arbitra zyskuje następujące premie: uderzenia skrzydeł
zadają obrażenia zwiększone o 200%[x]; uderzenia skrzydeł
powodują odsłonięcie.

Skrzydła                       Uderzenia skrzydeł Arbitra powodują teraz Osądzenie wrogów.
Sprawiedliwości

Boska Interwencja              Arbiter Sprawiedliwości wbija się w niebo na dodatkowe 2 sek. i
sprowadza na pole bitwy grad Błogosławionych Włóczni. Po
wylądowaniu Arbiter Sprawiedliwości ostrzeliwuje pole bitwy 10
dodatkowymi Błogosławionymi Włóczniami, które powodują
odsłonięcie na 4 sek.

Układ grup: [2, 2, 3].

Źródłowy rejestr opisowy Mocy Specjalnych - wygenerowany do podmiany docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf
Kontrola tekstowa PDF
Poniższe frazy są celowo obecne w tekście PDF-a, aby można było automatycznie zweryfikować
poprawność źródła po podmianie w repozytorium.

Kontrola                     Frazy

Sekcja Furii                 1. Furia Niebios

Furia grupa 3                Ostateczna Sprawiedliwość; Krok w Światłości; Potrojenie

Forteca grupa 3              Barykada; Cierniowa Reduta; Okopanie

Zenit grupa 3                Empirejska Klinga; Rozdarcie; Homilia Stali

Arbiter grupa 3              Skrzydła Serafina; Skrzydła Sprawiedliwości; Boska Interwencja

Notatka terminologiczna
Określenie "wpis umiejętności" oznacza techniczny wpis w rejestrze drzewa, a nie kategorię Diablo 4
"Główne/Core". Forteca należy do Mocy Specjalnych i nie jest klasyfikowana jako umiejętność
Główna/Core.

Źródłowy rejestr opisowy Mocy Specjalnych - wygenerowany do podmiany docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf
