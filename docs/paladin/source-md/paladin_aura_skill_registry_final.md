# Paladyn - Aury

## Metadane źródeł

| Pole | Wartosc |
|---|---|
| Poprzedni PDF | `docs/paladin/source-pdfs/paladin_aura_skill_registry_final.pdf` |
| SHA-256 PDF | `7cce431e3fd262b67ff824188e78f1d9733a09609177f4cd888f21fc71986826` |
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

### Aura Fanatyzmu / Fanaticism Aura (`aura_fanatyzmu`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Fanaticism Aura.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Fanaticism+Aura` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Attack Speed, Critical Strike Chance |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Attack Speed: 5.0% Critical Strike Chance: 2.0% | - |
| 2 | Attack Speed: 5.2% Critical Strike Chance: 2.1% | - |
| 3 | Attack Speed: 5.4% Critical Strike Chance: 2.2% | - |
| 4 | Attack Speed: 5.6% Critical Strike Chance: 2.2% | - |
| 5 | Attack Speed: 5.8% Critical Strike Chance: 2.3% | - |
| 6 | Attack Speed: 6.0% Critical Strike Chance: 2.4% | - |
| 7 | Attack Speed: 6.1% Critical Strike Chance: 2.4% | - |
| 8 | Attack Speed: 6.3% Critical Strike Chance: 2.5% | - |
| 9 | Attack Speed: 6.4% Critical Strike Chance: 2.6% | - |
| 10 | Attack Speed: 6.5% Critical Strike Chance: 2.6% | - |
| 11 | Attack Speed: 6.6% Critical Strike Chance: 2.6% | - |
| 12 | Attack Speed: 6.7% Critical Strike Chance: 2.7% | - |
| 13 | Attack Speed: 6.8% Critical Strike Chance: 2.7% | - |
| 14 | Attack Speed: 6.9% Critical Strike Chance: 2.8% | - |
| 15 | Attack Speed: 7.0% Critical Strike Chance: 2.8% | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Aura Śmiałości / Defiance Aura (`aura_smialosci`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Defiance Aura.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Defiance+Aura` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Armor, All Resistances |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Armor: 30% All Resistances: 30% | - |
| 2 | Armor: 31% All Resistances: 31% | - |
| 3 | Armor: 32% All Resistances: 32% | - |
| 4 | Armor: 34% All Resistances: 34% | - |
| 5 | Armor: 35% All Resistances: 35% | - |
| 6 | Armor: 36% All Resistances: 36% | - |
| 7 | Armor: 37% All Resistances: 37% | - |
| 8 | Armor: 38% All Resistances: 38% | - |
| 9 | Armor: 38% All Resistances: 38% | - |
| 10 | Armor: 39% All Resistances: 39% | - |
| 11 | Armor: 40% All Resistances: 40% | - |
| 12 | Armor: 40% All Resistances: 40% | - |
| 13 | Armor: 41% All Resistances: 41% | - |
| 14 | Armor: 41% All Resistances: 41% | - |
| 15 | Armor: 42% All Resistances: 42% | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Aura Świętej Światłości / Holy Light Aura (`aura_swietej_swiatlosci`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Holy Light Aura.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Holy+Light+Aura` |
| Status tabeli rang | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` |
| Wykryte metryki z HTML | Passive Damage, Active Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Passive Damage: [45%] Active Damage: [320%] | 45%, 320% |
| 2 | Passive Damage: [50%] Active Damage: [352%] | 50%, 352% |
| 3 | Passive Damage: [54%] Active Damage: [384%] | 54%, 384% |
| 4 | Passive Damage: [58%] Active Damage: [416%] | 58%, 416% |
| 5 | Passive Damage: [65%] Active Damage: [464%] | 65%, 464% |
| 6 | Passive Damage: [70%] Active Damage: [496%] | 70%, 496% |
| 7 | Passive Damage: [74%] Active Damage: [528%] | 74%, 528% |
| 8 | Passive Damage: [79%] Active Damage: [560%] | 79%, 560% |
| 9 | Passive Damage: [83%] Active Damage: [592%] | 83%, 592% |
| 10 | Passive Damage: [90%] Active Damage: [640%] | 90%, 640% |
| 11 | Passive Damage: [94%] Active Damage: [672%] | 94%, 672% |
| 12 | Passive Damage: [99%] Active Damage: [704%] | 99%, 704% |
| 13 | Passive Damage: [103%] Active Damage: [736%] | 103%, 736% |
| 14 | Passive Damage: [108%] Active Damage: [768%] | 108%, 768% |
| 15 | Passive Damage: [115%] Active Damage: [816%] | 115%, 816% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

## Treść przekonwertowana z lokalnego PDF

Poniższa sekcja zachowuje tekst dawnego PDF-a w formie edytowalnej Markdown. W razie konfliktu z sekcją tabel rang należy dopisać notatkę weryfikacyjną, a nie nadpisywać danych liczbowych bez sprawdzenia źródła.

Paladyn - baza umiejętności: Aury
Finalny opis roboczy na podstawie screenów użytkownika. Dokument obejmuje grupę Aury: Aura Fanatyzmu,
Aura Śmiałości, Aura Świętej Światłości.

Status dokumentu
Na poziomie dokumentacji opisowej grupa Aury jest zamknięta. Otwarte pozostają tylko kwestie późniejszego
modelowania runtime: skalowanie rang 15/15, interakcja pasyw/aktywacja, zachowanie single target dla odbić,
przeskoków i losowych celów oraz dokładne skracanie cooldownu z Obrzędu Osądu.

Zasada drzewa
Dla każdej aury obowiązuje układ: grupa 1 - wybór 1 z 2, grupa 2 - wybór 1 z 2, grupa 3 - wybór 1 z 3. Mechaniki
spoza drzewka, które mogą pozwalać wybrać więcej modyfikatorów, nie są częścią tego bazowego opisu.

Tabela porównawcza
Aura               Tagi                Koszt     Cooldown   Lucky Hit        Pasyw                     Aktywacja

Aura               Aura, Krzepkość,    0 Wiary   13,02 s    brak na karcie   +8,2% szybkości ataku i   Osłabia pobliskich wrogów
Fanatyzmu          Zelota                                   / nie dotyczy?   +3,3% krytyka 0/15;       na 4 s
+10,2% i +4,1% 15/15; 3
s; max 5

Aura Śmiałości     Aura, Krzepkość,    0 Wiary   17,35 s    33% 0/15;        +49% pancerza i           Nieustępliwość na 2 s
Defensywa, Moloch                        55% 15/15        odporności 0/15; +61%
15/15

Aura Świętej       Aura, Sędzia        brak      25 s       25%              1 445 [45%] co 2 s do 3   2 pociski po 10 280 [320%],
Światłości                             kosztu                                losowych pobliskich       do 4 przeskoków, powrót
Wiary                                 wrogów                    leczy 12% max zdrowia
1. Aura Fanatyzmu
Kategoria: Aura. Typ: efekt pasywny + aktywacja. Status: komplet opisowy.

Bazowa umiejętność
- Nazwa: Aura Fanatyzmu.
- Tagi: Aura, Krzepkość, Zelota.
- Koszt: 0 pkt. wiary.
- Czas odnowienia: 13,02 sek.
- Ranga 0/15: pasyw +8,2%[+] szybkości ataku i +3,3%[+] szans na trafienie krytyczne na 3 sek.; maksymalnie 5
kumulacji.
- Ranga 15/15: pasyw +10,2%[+] szybkości ataku i +4,1%[+] szans na trafienie krytyczne na 3 sek.; maksymalnie
5 kumulacji.
- Działanie aktywne: osłabia wszystkich pobliskich wrogów na 4 sek.

Grupa 1 - wybór 1 z 2
Dodatkowa Maksymalna Ilość Zasobu
Status: PEWNE
Moc pasywna Aury Fanatyzmu zapewnia tobie oraz twoim sojusznikom 15% maksymalnej wartości zasobu.

Dodatkowa Kumulacja Efektu Pasywnego
Status: PEWNE
Moc pasywna Aury Fanatyzmu kumuluje się 1 dodatkowy raz.

Grupa 2 - wybór 1 z 2
Generowanie Zasobów
Status: PEWNE
Aura Fanatyzmu zwiększa generowanie zasobów u ciebie oraz twoich sojuszników o 25%.

Krzepkość
Status: PEWNE
Aura Fanatyzmu zyskuje 15% więcej premii za skuteczność.

Grupa 3 - wybór 1 z 3
Obrzęd Zemsty
Status: PEWNE
Pasywny efekt Aury Fanatyzmu zwiększa obrażenia od trafień krytycznych twoje i sojuszników o 12,2%[x]. Trafienia
krytyczne aktywują efekt pasywny Fanatyzmu.

Obrzęd Pokory
Status: PEWNE
Efekt aktywny Aury Fanatyzmu teraz powoduje Odsłonięcie pobliskich wrogów na 4 sek. Zranienie odsłoniętych
wrogów aktywuje też moc pasywną Fanatyzmu.

Obrzęd Odkupienia
Status: PEWNE
Osłabieni wrogowie, którzy zginą w zasięgu działania Aury Fanatyzmu, przywracają ci 0,8% zdrowia i umacniają
2,1% twojego maksymalnego zdrowia (269 pkt.).
Osłabieni wrogowie zadają obrażenia zmniejszone o 20% zwykli wrogowie, 15% wrogowie elitarni lub 10% bossowie.
2. Aura Śmiałości
Kategoria: Aura. Typ: efekt pasywny + aktywacja. Status: komplet opisowy.

Bazowa umiejętność
- Nazwa: Aura Śmiałości.
- Tagi: Aura, Krzepkość, Defensywa, Moloch.
- Koszt: 0 pkt. wiary.
- Czas odnowienia: 17,35 sek.
- Typ obrażeń widoczny na karcie: Obrażenia Fizyczne.
- Ranga 0/15: Lucky Hit 33%; pasyw +49% do pancerza i +49% do wszystkich odporności.
- Ranga 15/15: Lucky Hit 55%; pasyw +61% do pancerza i +61% do wszystkich odporności.
- Działanie aktywne: zyskujesz Nieustępliwość na 2 sek.

Grupa 1 - wybór 1 z 2
Nieustępliwość
Status: PEWNE
Gdy ty lub twój sojusznik zostaniecie okaleczeni, staniecie się nieustępliwi na 2 sek. i zyskacie 10 pkt. Animuszu.
Efekt ten może wystąpić raz na 5 sek.
Nieustępliwych postaci nie można objąć negatywnymi efektami, a już nałożone zostają zdjęte. Aktywny Animusz zwiększa pancerz o
25%[+]; odniesienie obrażeń bezpośrednich zużywa ładunek; maksymalnie 8 ładunków.

Maksimum Zdrowia
Status: PEWNE
Moc pasywna Aury Śmiałości przywraca też tobie oraz twoim sojusznikom 15% maksymalnego zdrowia.

Grupa 2 - wybór 1 z 2
Krzepkość
Status: PEWNE
Aura Śmiałości zyskuje 15% więcej premii za skuteczność.

Dodatkowe Leczenie
Status: PEWNE
Aura Śmiałości zapewnia tobie i twoim sojusznikom 25% premii do otrzymywanego leczenia.

Grupa 3 - wybór 1 z 3
Obrzęd Cierni
Status: PEWNE
Działanie pasywne Aury Śmiałości zapewnia też 1 811 pkt. Cierni i zwiększa obrażenia od wszystkich Cierni u ciebie
i twoich sojuszników o 160%[+]. Działanie aktywne Aury Śmiałości zamiast tego uwalnia novę, która zadaje
500%[x] twoich obrażeń od Cierni.

Obrzęd Modlitwy
Status: PEWNE
Działanie pasywne Aury Śmiałości przywraca też tobie oraz twoim sojusznikom 4,3% twojego maksymalnego
zdrowia (551 pkt.) na sekundę. Leczenie siebie i sojuszników gwarantuje ci 3 poziomy kumulacji Animuszu co 1 sek.

Obrzęd Mocy
Status: PEWNE
Aura Śmiałości zwiększa zadawane przez ciebie obrażenia o 21,3%[x] na 4 sek. za każdym razem, gdy zyskujesz
Animusz.
3. Aura Świętej Światłości
Kategoria: Aura. Typ: efekt pasywny + aktywacja. Status: komplet opisowy.

Bazowa umiejętność
- Nazwa: Aura Świętej Światłości.
- Ranga na screenie bazowym: 0/15 / jeszcze nie wyuczono.
- Tagi: Aura, Sędzia.
- Koszt: brak kosztu Wiary na tooltipie.
- Czas odnowienia: 25 sek.
- Szansa na szczęśliwy traf: 25%.
- Typ obrażeń: Obrażenia Świętości.

Działanie pasywne
Ty i twoi sojusznicy emanujecie Światłością, która zadaje 1 445 [45%] pkt. obrażeń co 2 sek. 3 losowym wrogom w
pobliżu. Szybkość ataku jeszcze bardziej zwiększa te obrażenia.

Działanie aktywne
Wypuszczasz 2 pociski czystej światłości, z których każdy zadaje 10 280 [320%] pkt. obrażeń Świętości najbliższym
wrogom i przeskakuje do 4 dodatkowych wrogów, po czym wraca do ciebie. Gdy twój pierwszy pocisk świętości
wróci, odzyskujesz 12% maksymalnego zdrowia (491 pkt.).

Grupa 1 - wybór 1 z 2
Dodatkowe Odbicie
Status: PEWNE
Moc aktywna Aury Świętej Światłości odbija się 1 dodatkowy raz.

Dodatkowe Cele
Status: PEWNE
Moc pasywna Aury Świętej Światłości teraz namierza 1 dodatkowego wroga.

Grupa 2 - wybór 1 z 2
Premia do Obrażeń Osądu
Status: PEWNE
Aura Świętej Światłości zadaje obrażenia zwiększone o 40%[x] wrogom pod wpływem Osądu.
Osąd oznacza wroga na 3 sek. i po wygaśnięciu zadaje 80% obrażeń.

Krzepkość
Status: PEWNE
Aura Świętej Światłości zyskuje 15% więcej premii za skuteczność.

Grupa 3 - wybór 1 z 3
Obrzęd Osądu
Status: PEWNE
Efekt aktywny Świętej Światłości także Osądza wrogów, a jego czas odnowienia zostaje skrócony za każdą rangę.
Osąd oznacza wroga na 3 sek. i po wygaśnięciu zadaje 80% obrażeń.

Obrzęd Łaski
Status: PEWNE
Efekt aktywny Świętej Światłości powoduje 2 dodatkowe przeskoki oraz zapewnia tobie i twoim sojusznikom
umocnienie o wartości 4,0% maksymalnego zdrowia (164 pkt.).
Umocnienie stanowi dodatkowy zasób zdrowia, którego zużywanie zapewnia ci leczenie z upływem czasu.

Obrzęd Podporządkowania
Status: PEWNE
Efekt aktywny Świętej Światłości teraz ogłusza wrogów na 3 sek. i zadaje im 4 176 [130%] pkt. obrażeń
obszarowych, jeśli wróg zginie w trakcie trwania tego efektu.
Otwarte kwestie do późniejszego modelowania runtime
- Skalowanie rang 15/15 dla części Aur, szczególnie Aury Świętej Światłości.
- Czy Lucky Hit dotyczy aktywnego efektu aury, pasywu, czy całej umiejętności.
- Czy cooldown aury zmienia się przez rangę, itemy lub modyfikatory.
- Jak modelować jednoczesny efekt pasywny i ręczną aktywację z cooldownem.
- Single target: ile losowych celów pasywu, pocisków, przeskoków i odbić realnie trafia pojedynczego przeciwnika.
- Obrzęd Osądu: dokładna formuła skrócenia cooldownu za każdą rangę.
- Efekty zależne od zabijania wrogów mają niższą istotność single target boss, chyba że boss generuje dodatki
albo gra traktuje śmierć addów jako trigger.
