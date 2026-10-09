# Nekromanta — źródłowe dane Corpse, N3

Baseline: Diablo IV Sezon 15, patch 3.2.1. Źródła sprawdzone 06.10.2026. Trzy skille, 21 ulepszeń; cały katalog po N3: 13 skilli i 91 ulepszeń. Nowe skille mają `DAMAGE / NEEDS_VERIFICATION`, modyfikatory `safeForRuntimeDps=NO`.

## Źródła i rozstrzygnięcia

- [Oficjalny patch 3.1 Blizzard](https://news.blizzard.com/en-us/article/24287406/diablo-iv-patch-notes-3-1) rozstrzyga Bloody Mess: premia 50% → 75%.
- [Oficjalne noty sezonu i patcha 3.2.1](https://news.blizzard.com/en-us/article/24295394/celebrate-30-years-of-diablo-in-season-of-hell-s-legacy) potwierdzają Corpse Explosion 110% → 180%, Miasma 210% → 350%, Shrapnel 110% → 180% i zamianę pozycji Corpse Efficiency / Essence Generation.
- [Icy Veins — Necromancer Skills](https://www.icy-veins.com/d4/guides/necromancer-skills/) jawnie podaje Group 1 / Group 2 / T. To źródło membership; kolejność listy D4Guides nie określa grup. Enhancements to dwie alternatywy w każdej z grup 1 i 2, Transformation trzy alternatywy w grupie 3. Wybór katalogowy: `1 + 1 + 1`.
- D4Guides EN: [Corpse Explosion](https://d4guides.gg/en/database/skills/corpse-explosion), [Corpse Tendrils](https://d4guides.gg/en/database/skills/corpse-tendrils), [Skeleton Warrior](https://d4guides.gg/en/database/skills/skeleton-warrior). Polskie nazwy zweryfikowano w [indeksie Corpse PL](https://d4guides.gg/pl/baza-danych/umiejetnosci?category=corpse&class=necromancer), a +40 Lucky Hit również w [polskich nowościach sezonu](https://d4guides.gg/pl/baza-danych/nowosci-sezonu). Bezpośredni odczyt indywidualnych kart PL był niedostępny narzędziu; indeks głównej domeny udostępnił nazwy.
- [Główna baza D4Builds Class Skills](https://d4builds.gg/database/class-skills/) kontroluje max-view. Bezpośredni odczyt zwraca szkielet aplikacji; bieżący indeks głównej domeny udostępnił tekst umiejętności. Nie użyto starych subdomen ani branchów.

Oficjalne noty mają pierwszeństwo, następnie zgodność bieżących źródeł; nierozstrzygnięte konflikty pozostają manual review. Max-view nie podaje jawnej etykiety Rank 15: projekt zapisuje go pod 15 z notą o konieczności weryfikacji. Tabele zawierają tylko klucze 1 i 15, bez interpolacji 2..14.

## Baza i wymagania

| ID / nazwa | R1 / max-view | Lucky Hit | Odnowienie źródłowe | Aktywne wymaganie | Tagi bazowe |
|---|---:|---:|---|---|---|
| `wybuch_zwlok` / Wybuch Zwłok | 180% / 459% | 25% | null | `corpse / Zwłoki / 1 / CONSUMED_ON_USE` | CORPSE, PROFANE, PHYSICAL |
| `trupie_macki` / Trupie Macki | 20% / 98% | 30% | 11 s | `corpse / Zwłoki / 1 / CONSUMED_ON_USE` | CORPSE, PROFANE, PHYSICAL |
| `szkieletowy_wojownik` / Szkieletowy Wojownik | 65% / 166% za atak jednego wojownika | 20% | 10 s | puste | CORPSE, MINION, SUMMON |

Kategoria `CORPSE / Zwłoki / displayOrder=2`, grupa `corpse / Zwłoki / Corpse`, kolejność Basic → Core → Corpse. `SkillResourceData.EMPTY`: bez kosztu Esencji i bazowego generowania. Zwłoki są wymaganiem aktywacji, odrębnym od głównego zasobu klasy. Null oznacza brak zapisanej wartości odnowienia; zero i wartości ujemne są zabronione. Odnowienie nie określa cyklu, `damagePerUse` ani DPS.

Trupie Macki bazowo spowalniają o 50% przez 1.5 s, następnie przyciągają / Pull i zadają Physical damage. Wojownik pasywnie co 2 s wskrzesza z pobliskich Zwłok, do 4; aktywnie rozkazuje istniejącym sługom skoczyć i zaatakować. Pasywne wskrzeszanie nie tworzy wymogu Zwłok dla aktywnego rozkazu. D4Guides PL pokazuje Physical, Icy Veins Physical or Shadow oraz warianty Bone/Blood/Darkness; notes zachowują konflikt i zależność od Book of the Dead, bez bezwarunkowych tagów PHYSICAL/SHADOW.

## Wybuch Zwłok — dokładne grupy

| Grupa | ID / ulepszenie | Zachowane dane |
|---|---|---|
| 1 | `wiele_zwlok` / Wiele Zwłok | Do 2 dodatkowych detonowanych Zwłok; `RESOURCE_OR_COST`. Baza wymaga 1, nie 3; bez mnożenia obrażeń. |
| 1 | `generowanie_esencji` / Generowanie Esencji | +15 pkt. Esencji wyłącznie z ulepszenia; `RESOURCE_OR_COST`. Bazowe generowanie null. |
| 2 | `krwawa_kula` / Krwawa Kula | 20% szansy na Krwawą Kulę, utility bez damage coefficient. |
| 2 | `wydajne_zwloki` / Wydajne Zwłoki | Zużyte Zwłoki można zużyć 1 dodatkowy raz; `RESOURCE_OR_COST`. Nie jest niezużywaniem Zwłok, darmowym castem ani automatyczną dodatkową eksplozją. |
| 3 / T | `krwawa_miazga` / Krwawa Miazga | Rozmiar +50%, damage +75%[x], osobne modyfikatory jednego node'a. |
| 3 / T | `miazmaty` / Miazmaty | Również Darkness; zamiast eksplozji 350% / 893% Corrupting łącznie przez 6 s. |
| 3 / T | `szrapnel` / Szrapnel | Również Bone; zamiast eksplozji 8 przebijających odłamków, każdy 180% / 459%. |

Bloody Mess w D4Guides nadal ma nieaktualne 50%[x]. Blizzard 3.1 zwiększa premię do 75%; Icy Veins i D4Builds potwierdzają 75%. Rozstrzygnięcie i źródło są w notes; safe display YES, bez manual review.

## Trupie Macki — dokładne grupy

| Grupa | Ulepszenie | Zachowane dane |
|---|---|---|
| 1 | Szansa na Trafienie Krytyczne | +5%[+] przez 6 s przeciw wrogom zranionym przez Trupie Macki. |
| 1 | Generowanie Esencji | +35 pkt. Esencji tylko z ulepszenia; bazowe generowanie null. |
| 2 | Szansa na Szczęśliwy Traf | +40%, typ i safe display `NEEDS_MANUAL_REVIEW`; impact `NEEDS_VERIFICATION`. |
| 2 | Odsłonięcie | Vulnerable przez 4 s. |
| 3 / T | Gorzkie Żniwa | Zwłoki wymagane, ale niezużywane; `RESOURCE_OR_COST`, semantyka `REQUIRED_NOT_CONSUMED` w opisie. Immobilized 3 s po Pull. |
| 3 / T | Chodź Tu! | Wariant używany na graczu zamiast Zwłok; jawne usunięcie wymagania w `RESOURCE_OR_COST`. Fortify 5% maksymalnego zdrowia za zranionego wroga. |
| 3 / T | Szczęki Śmierci | 100% / 255% zamiast bazowego trafienia, bez opóźnienia przed Pull. |

PL D4Guides, Icy Veins i D4Builds zachowują Lucky Hit +40%; [changelog EN dla 3.2.1](https://d4guides.gg/en/database/skills/corpse-tendrils) skreśla tę właściwość. Brak oficjalnego rozstrzygnięcia: node i wartość zostają w grupie 2 z konfliktem. Bazowy Lucky Hit pozostaje 30%. Gorzkie Żniwa i Chodź Tu! opisują warianty; nie zmieniają bazowej listy wymagań ani nie wykonują override engine. Nie zakładamy liczby wrogów dla Fortify.

## Szkieletowy Wojownik — dokładne grupy

| Grupa | Ulepszenie | Zachowane dane |
|---|---|---|
| 1 | Premia do Obrażeń | Rozkaz daje +25%[x] damage przez 10 s. |
| 1 | Leczenie | Każdy atak leczy gracza za 2% maksymalnego zdrowia, utility bez damage. |
| 2 | Animusz | Rozkaz daje 3 kumulacje Resolve / Animuszu. D4Guides i Icy Veins są zgodne; D4Builds błędnie wpisuje Ferocity pod nagłówkiem Resolve. |
| 2 | Odsłonięcie | Atak ma 30% szansy na Vulnerable przez 4 s; to nie jest bazowy Lucky Hit. |
| 3 / T | Władca Marionetek | Limit +3 wojowników; zgodność D4Guides/Icy Veins, D4Builds błędnie pisze „increased to 3”. Bez zmiany procentu za atak. |
| 3 / T | Służba i Ofiara | Nowy komponent eksplozji na śmierć 150% / 383%; utrata 25% maksymalnego zdrowia na sekundę w walce, odstęp wskrzeszania 2 s → 0.5 s. |
| 3 / T | Litania Śmierci | Wszyscy wojownicy przywołani → Kapłan na 8 s, szansa krytyczna sług R1 +15%[+], max-view +21%[+]; leczenie 100% maksymalnego zdrowia łącznie w czasie działania. |

Litania jest modyfikatorem statusu skalowanym rangą, z wartościami 15/21 w notes; nie jest współczynnikiem obrażeń. Nie zakładamy liczby sług, ataków ani śmierci.

## Tabele wariantów i granica runtime

Opcjonalna `UpgradeDamageModifier.componentDamagePercentRanks` używa istniejącego neutralnego `DamagePercentComponentRankTable`, domyślnie pustego dla wcześniejszych danych. Tabele są niemutowalne; tooltipy pokazują punkty źródłowe i czas.

| Wariant | Komponent | R1 | Rank 15 / max-view | Czas | Relacja do bazy |
|---|---|---:|---:|---|---|
| Miazmaty | DAMAGE_OVER_DURATION | 350 | 893 | 6 s | zastępuje eksplozję; nie damage per second |
| Szrapnel | PRIMARY_DAMAGE | 180 | 459 | brak | zastępuje eksplozję; procent na odłamek, liczba 8 osobno |
| Szczęki Śmierci | PRIMARY_DAMAGE | 100 | 255 | brak | zastępuje bazowe obrażenia |
| Służba i Ofiara | BURST_DAMAGE | 150 | 383 | brak | nowy komponent na śmierć jednego wojownika |

Każda tabela ma wyłącznie rangi 1 i 15. Nie dodajemy 180+350 ani 20+100, nie mnożymy przez 8 odłamków, 4/7 wojowników ani 3 detonacje. Nie interpolujemy 2..14, nie liczymy cyklu ani DPS. `HeroClass`, `SkillId`, `DamageEngine`, manual simulation i current build pozostają bez zmian. Wszystkie modyfikatory mają runtime NO. ZIP nie jest tworzony.
