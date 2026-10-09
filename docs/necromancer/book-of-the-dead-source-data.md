# N7 — dane źródłowe Księgi Umarłych

Zakres: **Season 15 / patch 3.2.1**, 3 rodziny, 9 specjalizacji, 27 wyborów. Wartości golden i polskie nazwy pochodzą z kontraktu zadania N7. Katalog `krys.necromancer.bookofthedead` jest opisową domeną mechaniki klasowej, oddzielną od 23 skilli / 161 upgrade'ów Nekromanty. Każdy wpis ma `NOT_RUNTIME_ENABLED`. Poniższe fakty nie obliczają DPS, liczby przywołań, odziedziczonych statystyk ani stanu bohatera.

## Źródła i pierwszeństwo

- [Icy Veins — Necromancer, Season 15, Book of the Dead](https://www.icy-veins.com/d4/guides/necromancer-leveling-guide/#book-of-the-dead): rodziny, poziomy, theme, pasywy, alternatywy, bonusy i kary Sacrifice oraz globalne reguły minionów.
- [Blizzard — Lord of Hatred, zmiany Nekromanty](https://news.blizzard.com/en-us/article/24267729/prepare-for-the-reckoning-lord-of-hatred-draws-near): Sacrifice dopuszcza aktywne miniony przy zainwestowanych punktach i odpowiednim skillu na pasku; zmniejsza liczbę szkieletów lub obrażenia Golema.
- [Blizzard — oficjalny patch 2.5, Skeleton Mage, Bone](https://news.blizzard.com/en-us/article/24244466/diablo-iv-patch-notes-2-5): Fortify na atak zmniejszone z 3% do 0.1% Maximum Life; ma pierwszeństwo nad wyświetlanym przez Icy Veins 3%.

Źródła sprawdzono 07.10.2026. Oficjalny opis Lord of Hatred rozstrzyga możliwość przywołań, a golden wartości 50% i pozostałe liczby N7 pozostają zgodne z zadanym kontraktem i bieżącym opisem Book. Żadna różnica w Book nie zmienia wcześniej zamkniętych tabel, tagów lub notes N0–N6.

## Sacrifice po Lord of Hatred

Każde Sacrifice składa się z **bonusu dla gracza** i **osobnej kary dla summonów**. Zakupiony skill przywołujący na pasku nadal pozwala przywoływać sługi. Warriors / Mages mają możliwą liczbę summonów zmniejszoną o 50%; Golem zadaje o 50%[x] mniej obrażeń. `BookOfDeadSummonPenalty.reductionPercent=50` jest dodatnią wielkością redukcji, nie premią i nie wykonywanym mnożnikiem. Typ określa redukcję liczby albo mnożnikowe zmniejszenie obrażeń Golema. Nie istnieje boolean `minionDisabledBySacrifice`, Book selection ani jego persistence.

## Szkieletowi Wojownicy

### Harcownicy / Skirmishers

`skirmisher`, unlock **5**, theme **BONE**.

- Passive: obrażenia +30%, zdrowie -15%.
- Upgrade #1: +2 dodatkowych Harcowników; agresywne zachowanie bezpośrednio po przywołaniu.
- Upgrade #2: trafienie nakłada Vulnerable i Slow 50% na 4 s.
- Sacrifice: gracz Critical Strike Chance +5%[+]; możliwa liczba summonów -50%.

### Obrońcy / Defenders

`defender`, unlock **8**, theme **BLOOD**.

- Passive: Life +15%.
- Upgrade #1: Ciernie zależne od kontekstu statystyk / poziomu źródła; Taunt 6 s. Otrzymanie obrażeń uwalnia odłamki, które zadają pobliskim wrogom **50% Cierni danego Obrońcy**.
- Upgrade #2: zadawanie obrażeń ma 10% szansy utworzyć Blood Orb.
- Sacrifice: gracz Resistance to All Elements +40%[+]; możliwa liczba summonów -50%.

Icy Veins może wyświetlać absolutną liczbę Cierni, np. 3481. Katalog nie wpisuje jej jako stałej: fakt `thorns` ma `SOURCE_DEPENDENT_AMOUNT`, `value=null` i wyjaśnienie `source-dependent Thorns amount`. Pewne 50% odłamków ma odrębną jednostkę `PERCENT_OF_THORNS`.

### Żniwiarze / Reapers

`reaper`, unlock **12**, theme **DARKNESS**.

- Passive: przygotowywany atak wind-up co 10 s.
- Upgrade #1: wind-up skraca jeden aktywny cooldown o 3 s i tworzy Corpse.
- Upgrade #2: obrażenia +50%[x], 15% szansy na Stun trwający 1 s.
- Sacrifice: obrażenia gracza +15%[x]; możliwa liczba summonów -50%.

## Szkieletowi Magowie

### Magowie Cienia / Shadow Mages

`shadow_mage`, unlock **5**, theme **DARKNESS**, obrażenia **Shadow**.

- Passive: zadają obrażenia Shadow.
- Upgrade #1: dodatkowe Corrupting damage **100% w ciągu 6 s**.
- Upgrade #2: pociski dają **graczowi oraz Magowi Cienia** Barrier równą 3% Maximum Life na 4 s, maksymalnie 30% Maximum Life.
- Sacrifice: gracz Essence Regeneration +20%[+], Maximum Essence +20; możliwa liczba summonów -50%.

100% jest źródłowym coefficient mechaniki Book; nie jest runtime table skilla Skeleton Mage. Bariera ma jawny czas, limit i dwóch odbiorców.

### Magowie Zimna / Cold Mages

`cold_mage`, unlock **18**, theme **DARKNESS**, obrażenia / mechanika źródłowa **COLD**.

- Passive: zadają obrażenia Cold.
- Upgrade #1: początkowy pocisk rozdziela się na 2 po trafieniu; trafieni przeciwnicy są Weakened przez 4 s.
- Upgrade #2: okazjonalna Blizzard zadaje **200% Cold damage w ciągu 6 s**, Chill 6% na sekundę; uszkodzeni przeciwnicy są Vulnerable przez 4 s.
- Sacrifice: gracz Damage to Vulnerable enemies +20%[x]; możliwa liczba summonów -50%.

Fork nie określa trzech trafień tego samego celu. Blizzard nie ma częstotliwości ani wyliczonego DPS; 200% to obrażenia przez cały duration. Theme Darkness nie zmienia damage type w istniejącym registry skilli.

### Magowie Kości / Bone Mages

`bone_mage`, unlock **22**, theme **BONE**.

- Passive: wysokie obrażenia kosztem własnego Life.
- Upgrade #1: +2 dodatkowe pociski, każdy zadaje **75% normal damage**.
- Upgrade #2: ataki wzmacniają (Fortify) gracza o **0.1% Maximum Life**; śmierć Maga tworzy Corpse.
- Sacrifice: obrażenia gracza +20%[x] **gdy gracz ma stos Overpower**; możliwa liczba summonów -50%.

75% jest względne wobec normalnych obrażeń (`PERCENT_OF_NORMAL_DAMAGE`), bez podstawy Weapon Damage. Nie dodajemy tych pocisków do tabeli obrażeń Skeleton Mage.

| Konflikt Fortify na atak | Wartość / rozstrzygnięcie |
| --- | --- |
| Icy Veins display | 3% Maximum Life |
| Oficjalny Blizzard patch 2.5 | zmiana 3% → 0.1% Maximum Life |
| Wartość katalogowa | **0.1% Maximum Life** |
| Status danych | **OFFICIAL_SOURCE_RESOLVED** |

`SourceConflict` zapisuje identyfikator faktu `fortify`, wartość wyświetlaną, wartość kanoniczną, oba źródła i uzasadnienie pierwszeństwa oficjalnego patcha. Konstruktor specjalizacji kontroluje zgodność konfliktu z liczbą faktu. To rozstrzygnięcie źródłowe, bez manual-review i bez uruchomienia efektu.

## Golemy

### Golem Kościany / Bone Golem

`bone_golem`, unlock **8**, theme **BONE**.

- Zdolność bazowa: Taunt enemies.
- Upgrade #1: aktywowanie zdolności tworzy 5 Corpses.
- Upgrade #2: po otrzymaniu obrażeń, **najwyżej raz na 3 s**, Bone Spikes zadają źródłowe **250% damage** i nakładają Vulnerable na 4 s.
- Sacrifice: gracz Attack Speed **+10%[+]**; Golem damage -50%[x].

Aktualny golden to 10%, bez starej wartości 15%. 250% jest coefficient mechaniki Book, poza damage table Golema.

### Golem Krwawy / Blood Golem

`blood_golem`, unlock **28**, theme **BLOOD**, dodatkowy wymóg: **Call of the Underworld**.

- Zdolność bazowa: wysysa Life pobliskich przeciwników, lecząc i wzmacniając siebie.
- Upgrade #1: wysysa Life innych Minions. Per wyssany Minion: Maximum Life +5%, damage +5%[x] przez 20 s. Limity: +50% Maximum Life i +50%[x] damage.
- Upgrade #2: aktywowanie zdolności wzmacnia (Fortify) gracza o **10% Maximum Life na każdego wyssanego wroga**.
- Sacrifice: gracz Maximum Life +20%[x]; Golem damage -50%[x].

Premie per Minion, duration i limity są osobnymi faktami. Nie zakładamy dziesięciu minionów, osiągnięcia limitu ani liczby wyssanych przeciwników. Quest jest opisowym wymogiem odblokowania, bez wykonania / sprawdzania stanu postaci.

### Golem Żelazny / Iron Golem

`iron_golem`, unlock **32**, theme **DARKNESS**, dodatkowy wymóg: **Call of the Underworld**.

- Zdolność bazowa: ground Slam i Stun enemies.
- Upgrade #1: każdy atak wywołuje shockwave, źródłowe **125%[x] damage** głównemu celowi i przeciwnikom za nim.
- Upgrade #2: Slam przyciąga (Pull) przeciwników; size +50%.
- Sacrifice: gracz Critical Strike Damage +15%[x]; Golem damage -50%[x].

125%[x] ma jednostkę `SOURCE_DAMAGE_PERCENT_MULTIPLICATIVE`: to źródłowa wartość shockwave mechanic, bez integracji z Golem skill damage table.

## Globalne reguły minionów i granica N7

`NecromancerMinionRules` przechowuje **20% base Minion Lucky Hit Chance** oraz **100% dziedziczenia użytecznych statystyk i buffów gracza**, z opisem warunku przywołań po Sacrifice. Reguły są wyłącznie metadanymi. Nie tworzymy runtime inheritance engine ani kalkulatora obrażeń, liczebności lub Lucky Hit.

Wszystkie listy i mapy katalogu są immutable i kopiują wejścia. Registry waliduje komplet 9 id, trzy wpisy w rodzinie i odrzuca duplikaty. Specjalizacja waliduje dokładnie trzy różne alternatywy i poprawny typ kary rodziny. Opisy są wymagane i niepuste. Dane nie korzystają z modeli drzewa skillów ani abstrakcji wspólnej z Przysięgami Paladyna.

N7 nie dopisuje NECROMANCER do HeroClass, Book selection do HeroProfile / persistence lub current build, nie zmienia SkillId, DamageEngine, ManualSimulationService, DamageRankingService, danych / notes 23 skilli ani PaladinOathRegistry. Current build pozostaje Paladyn-only. Pakiet nie ma zależności od tych warstw; 75%, 100%, 125%, 200%, 250% ani Sacrifice nie są wykonywane.
