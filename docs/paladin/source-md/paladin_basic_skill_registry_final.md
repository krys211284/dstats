# Paladyn - umiejętności podstawowe / Basic

## Metadane źródeł

| Pole | Wartosc |
|---|---|
| Poprzedni PDF | `docs/paladin/source-pdfs/paladin_basic_skill_registry_final.pdf` |
| SHA-256 PDF | `9f80175fb0e45f7be795c111a0c579f775abf1e8b04bd9bee1de4b21f03e0617` |
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

### Wymach / Brandish (`wymach`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Brandish.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Brandish` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [75%] | 75% |
| 2 | Damage: [83%] | 83% |
| 3 | Damage: [90%] | 90% |
| 4 | Damage: [97%] | 97% |
| 5 | Damage: [109%] | 109% |
| 6 | Damage: [116%] | 116% |
| 7 | Damage: [124%] | 124% |
| 8 | Damage: [131%] | 131% |
| 9 | Damage: [139%] | 139% |
| 10 | Damage: [150%] | 150% |
| 11 | Damage: [157%] | 157% |
| 12 | Damage: [165%] | 165% |
| 13 | Damage: [172%] | 172% |
| 14 | Damage: [180%] | 180% |
| 15 | Damage: [191%] | 191% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Święty Pocisk / Holy Bolt (`swiety_pocisk`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Holy Bolt.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Holy+Bolt` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
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

### Starcie / Clash (`starcie`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Clash.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Clash` |
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

### Natarcie / Advance (`natarcie`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Advance.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Advance` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [105%] | 105% |
| 2 | Damage: [115%] | 115% |
| 3 | Damage: [126%] | 126% |
| 4 | Damage: [136%] | 136% |
| 5 | Damage: [152%] | 152% |
| 6 | Damage: [163%] | 163% |
| 7 | Damage: [173%] | 173% |
| 8 | Damage: [184%] | 184% |
| 9 | Damage: [194%] | 194% |
| 10 | Damage: [210%] | 210% |
| 11 | Damage: [220%] | 220% |
| 12 | Damage: [231%] | 231% |
| 13 | Damage: [241%] | 241% |
| 14 | Damage: [252%] | 252% |
| 15 | Damage: [268%] | 268% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

## Treść przekonwertowana z lokalnego PDF

Poniższa sekcja zachowuje tekst dawnego PDF-a w formie edytowalnej Markdown. W razie konfliktu z sekcją tabel rang należy dopisać notatkę weryfikacyjną, a nie nadpisywać danych liczbowych bez sprawdzenia źródła.

Paladyn - umiejętności podstawowe / Basic
Wersja finalna na podstawie aktualnie potwierdzonych screenów i korekt użytkownika

1. Założenia dokumentu
Dokument obejmuje grupę Basic / Podstawowe dla Paladyna. Źródłem prawdy są screeny użytkownika. Wartości
niepotwierdzone pozostają oznaczone jako DO WERYFIKACJI. Po korekcie użytkownika wszystkie bazowe screeny tej
paczki traktujemy jako ranga 0/15.

- Nie ma obowiązkowego węzła pośredniego typu Enhanced / Ulepszony.

- Dla każdej umiejętności modelujemy trzy grupy modyfikatorów: 2 / 2 / 3; z każdej grupy można wybrać
maksymalnie jeden modyfikator.

- Mechaniki spoza drzewka mogą później pozwolić wybrać wszystkie modyfikatory, ale nie są częścią
bazowego modelu drzewa.

- Screeny 15/15 dla Basic zostaną dosłane później, jeśli będziemy modelować skalowanie rang.

- Single target dla dodatkowych łuków, rykoszetów i pocisków pozostaje do testów empirycznych.

2. Tabela porównawcza - Basic / Podstawowe
Skill           Tagi                 Typ        Wiara   Lucky Hit   Bazowy wsp   Kluczowa mechanika
obrażeń                        ółczynnik

Wymach          Podstawowe, Adept    Świętość   14      26%         75%          fala energii po łuku

Święty Pocisk   Podstawowe, Sędzia   Świętość   16      57%         90%          rzut świętym młotem

Starcie         Podstawowe, Moloch   Fizyczne   20      65%         115%         Marsz Krzyżowca, wymaga tarczy

Natarcie        Podstawowe,          Fizyczne   18      18%         105%         szarża / mobilność
Mobilność, Fanatyk

3. Wymach
Status: potwierdzone po korektach użytkownika. Ranga na screenie: 0/15.

Bazowo: generuje 14 pkt. wiary, Lucky Hit 26%, obrażenia Świętości, tagi Podstawowe i Adept. Opis:
wykonujesz Wymach Światłością, wypuszczając po łuku energię zadającą 149 791 [75%] pkt. obrażeń.

Grupa 1 - wybór 1 z 2
- Generowanie Wiary: Wymach generuje dodatkowo 5 pkt. wiary.

- Zwiększenie Obrażeń: Wymach zadaje obrażenia zwiększone o 20%[x].

Grupa 2 - wybór 1 z 2
- Szybkość Użycia: Wymach ma zwiększoną o 20%[+] szybkość użycia.

- Odsłonięcie: Wymach wywołuje Odsłonięcie wrogów na 4 sek. Odsłonięci wrogowie otrzymują obrażenia
zwiększone o 20%.

Grupa 3 - wybór 1 z 3
- Powracająca Światłość: światłość Wymachu powraca do ciebie, zadając w drodze powrotnej 104 854 [52%]
pkt. obrażeń.

- Miecz Mistrzostwa: nazwa potwierdzona przez użytkownika. Gdy jesteś w pełni sił, Wymach zadaje 254 645
[128%] pkt. obrażeń i przemieszcza się o 100% szybciej. Pełnia sił oznacza więcej niż 80% zdrowia.

- Krzyżowe Uderzenie: Wymach uderza 2 dodatkowymi łukami zadającymi 239 666 [120%] pkt. obrażeń.
Uwaga single target: 2 dodatkowe łuki nie są automatycznie 2 dodatkowymi trafieniami w pojedynczy cel. Wymaga
testu w grze.
4. Święty Pocisk
Status: potwierdzone w zakresie paczki Basic. Pełne brzmienie modyfikatora Szybkość Użycia zostało
potwierdzone przez użytkownika.

Bazowo: ranga 0/15, generuje 16 pkt. wiary, Lucky Hit 57%, obrażenia Świętości, tagi Podstawowe i Sędzia.
Opis: rzucasz święty młot, zadając 179 749 [90%] pkt. obrażeń.

Grupa 1 - wybór 1 z 2
- Generowanie Wiary: Święty Pocisk generuje dodatkowe 7 pkt. wiary.

- Osąd: Święty Pocisk powoduje Osąd. Osąd oznacza wroga na 3 sek. i po wygaśnięciu zadaje 80% obrażeń.

Grupa 2 - wybór 1 z 2
- Spowolnienie: Święty Pocisk spowalnia wrogów o 50% na 5 sek.

- Szybkość Użycia: zwiększa szybkość użycia Świętego Pocisku o 10% po trafieniu wroga, do 30% na 4 sek.;
skraca czas odnowienia Świętego Pocisku o 30%. Status: potwierdzone przez użytkownika.

Grupa 3 - wybór 1 z 3
- Burzowy Pocisk: Święty Pocisk staje się umiejętnością Sprawiedliwości z czasem odnowienia 7 sek. Po
wystrzeleniu rozbija się o ziemię, zadając 629 124 [315%] pkt. obrażeń i ogłuszając wrogów na obszarze na 2
sek.

- Boski Pocisk: Święty Pocisk staje się umiejętnością Adepta, przebija, zadaje 239 606 [120%] pkt. obrażeń i
powoduje Odsłonięcie.

- Rykoszetujący Pocisk: Święty Pocisk zadaje 269 624 [135%] pkt. obrażeń i rykoszetuje 3 razy.
Uwaga single target: rykoszety i przebicia wymagają późniejszego testu, ile razy realnie trafiają pojedynczy cel.

5. Starcie
Status: nazwa Brać Ich i pełne brzmienie Potyczki potwierdzone przez użytkownika.

Bazowo: ranga 0/15, generuje 20 pkt. wiary, Lucky Hit 65%, obrażenia Fizyczne, wymaga tarczy, tagi
Podstawowe i Moloch. Opis: uderzasz bronią i tarczą, zadając 83 881 [115%] pkt. obrażeń. Trafienie wroga
zapewnia Marsz Krzyżowca, zwiększając szansę na blok o 15%[x] na 6 sek.

Grupa 1 - wybór 1 z 2
- Generowanie Wiary: Starcie generuje dodatkowe 10 pkt. wiary.

- Animusz: uderzenie wroga Starciem zapewnia 2 kumulacje Animuszu. Animusz zwiększa pancerz o 25%[+],
obrażenia bezpośrednie wyczerpują ładunek, limit 8 ładunków.

Grupa 2 - wybór 1 z 2
- Skuteczność Marszu Krzyżowca: skuteczność Marszu Krzyżowca zwiększa się o 25%[x].

- Zwiększenie Obrażeń: Starcie zadaje obrażenia zwiększone o 20%[x].

Grupa 3 - wybór 1 z 3
- Brać Ich: potwierdzone. Animusz wzmacnia Starcie, które zadaje obrażenia zwiększone o 8%[x] za każdy
poziom kumulacji Animuszu. Co 3. atak przyciąga do ciebie wrogów.

- Potyczka: potwierdzone. Starcie staje się umiejętnością Fanatyka i wywołuje dodatkowe uderzenie za 113
240 [155%]. Marsz Krzyżowca nie zapewnia już szansy na blok, tylko 10%[+] premii do szansy na trafienie
krytyczne, maksymalnie 30%[+].

- Kara: w trakcie Marszu Krzyżowca zyskujesz 30%[+] szansy na Odwet, 3 489 pkt. cierni i obrażenia od cierni
zwiększone o 20%[x].

6. Natarcie
Status: kompletne w zakresie dostarczonych screenów Basic.

Bazowo: ranga 0/15, generuje 18 pkt. wiary, Lucky Hit 18%, obrażenia Fizyczne, tagi Podstawowe, Mobilność i
Fanatyk. Opis: szarżujesz naprzód, zadając wrogom na drodze 76 587 [105%] pkt. obrażeń.

Grupa 1 - wybór 1 z 2
- Umocnienie: Natarcie zapewnia 2% maksymalnego zdrowia jako umocnienie, kiedy trafisz wroga po raz
pierwszy.

- Nieograniczenie: Natarcie zapewnia Nieograniczenie na 2 sek. Nieograniczona postać może przechodzić
przez wrogów, a jej szybkość ruchu nie może zostać zmniejszona.

Grupa 2 - wybór 1 z 2
- Błysk Ostrza: Natarcie staje się umiejętnością Adepta z czasem odnowienia 10 sek. Staje się zrywem przez
wrogów, powodując Odsłonięcie i zadając 167 727 [230%] pkt. obrażeń.

- Pędząca Fala: Natarcie staje się umiejętnością Molocha. Trafienie rozchodzi się falą, zadając 99 564 [136%]
pkt. obrażeń. Zyskujesz 1 Animusz za każdy 1 m przebyty podczas Natarcia.

Grupa 3 - wybór 1 z 3
- Zryw Forpoczty: Natarcie powoduje szarżę na wrogów, zapewniając 1 poziom Ferworu za każdego trafionego
wroga. Zadajesz obrażenia zwiększone o 25%[x] za każdy poziom Ferworu.

- Osłabienie: Natarcie osłabia wrogów na 4 sek. Osłabieni wrogowie zadają mniej obrażeń: 20% zwykli, 15%
elitarni, 10% bossowie.

- Szansa na Trafienie Krytyczne: Natarcie daje 5%[+] premii do szansy na trafienie krytyczne na 8 sek.

7. Rejestr mechanik z Basic
- Wiara: zasób Paladyna; Basic skille ją generują.

- Odsłonięcie: zwiększa otrzymywane obrażenia o 20%; domyślnie w projekcie 3 sek., ale konkretne tooltipy
mogą nadpisać czas, np. Wymach i Natarcie.

- Osąd: oznacza wroga na 3 sek.; po wygaśnięciu zadaje 80% obrażeń.

- Pełnia sił: postać ma więcej niż 80% zdrowia.

- Animusz: pancerz +25%[+], obrażenia bezpośrednie zużywają ładunek, limit 8.

- Ferwor: wzmacnia wybrane umiejętności za każdy poziom kumulacji.

- Odwet: mechanika cierni/proc po bloku lub trafieniu - szczegóły zależne od tooltipu.

- Osłabienie: zmniejsza obrażenia zadawane przez cel; wartości zależą od typu wroga.

8. Pozostałe otwarte punkty Basic
- Screeny 15/15 dla Basic - użytkownik wyśle później, jeśli będziemy modelować skalowanie rang.

- Single target: trzeba empirycznie ustalić, ile dodatkowych łuków, rykoszetów, pocisków i odbić realnie trafia
pojedynczy cel.
