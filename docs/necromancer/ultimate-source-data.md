# Moce Specjalne / Ultimate Nekromanty — dane źródłowe N6

Baseline: **Diablo IV Sezon 15 / patch 3.2.1**. Patch 3.2.2 nie zmienił danych użytych w N6.
Sprawdzenie źródeł: 07.10.2026. Zakres: cztery aktywne Ultimate i 28 ulepszeń; katalog źródłowy bez runtime.

## Źródła i rozstrzygnięcia

1. [Blizzard — zmiany Sezonu 15 / 3.2.1](https://news.blizzard.com/en-us/article/24295394/celebrate-30-years-of-diablo-in-season-of-hell-s-legacy):
   ponowne użycie i stacking Armii, rank scaling Niezłomnego Dowództwa, 800% Ścieżki Ciemności,
   koszt Hematolagnii 70 i aktualizacja Overpower dla tego wariantu, Soulrift 350% / cooldown 40 s,
   redukcja Soul Vortex 15 s i rank scaling Distilled Anima.
2. [Icy Veins — Necromancer Skills](https://www.icy-veins.com/d4/guides/necromancer-skills/):
   jawne Group 1, Group 2 i T oraz wartości R1 i mechaniki; T to trzy wzajemnie wykluczające się Transformation.
3. D4Guides EN Sezon 15 / patch 3.2.1:
   [Army of the Dead](https://d4guides.gg/en/database/skills/army-of-the-dead),
   [Blood Wave](https://d4guides.gg/en/database/skills/blood-wave),
   [Bone Storm](https://d4guides.gg/en/database/skills/bone-storm),
   [Soulrift](https://d4guides.gg/en/database/skills/soulrift).
   Polskie nazwy: [Armia Umarłych](https://d4guides.gg/pl/baza-danych/umiejetnosci/armia-umarlych),
   [Fala Krwi](https://d4guides.gg/pl/baza-danych/umiejetnosci/fala-krwi),
   [Nawałnica Kości](https://d4guides.gg/pl/baza-danych/umiejetnosci/nawalnica-kosci),
   [Spętanie Dusz](https://d4guides.gg/pl/baza-danych/umiejetnosci/spetanie-dusz)
   i [listing Ultimate](https://d4guides.gg/pl/baza-danych/umiejetnosci?category=ultimate&page=2).
   Mechanika Soulrift pochodzi z EN: polski opis bazowy renderuje błędne liczby/tekst, zachowujemy jedynie nazwy.
4. [Główne bieżące D4Builds — Class Skills](https://d4builds.gg/database/class-skills/):
   kontrola max-view. Nie użyto branch/test/starych subdomen. Treść dynamiczna w bezpośrednim odczycie
   nie ujawnia całej bazy; aktualny indeks wyszukiwarki dla tej samej głównej strony zawiera komplet użytych współczynników.

Hierarchia: jawna zmiana Blizzard → aktualne Icy Veins / D4Guides → główne D4Builds dla max-view →
nierozstrzygnięte ograniczenie w notes/manual review. Max-view D4Builds nie ma jawnej etykiety Rank 15;
projekt przechowuje ten widok pod 15 z zastrzeżeniem weryfikacji. Każda tabela zawiera tylko 1 i 15,
bez interpolacji 2..14. Wyjątek: dla Niezłomnego Dowództwa nie ma potwierdzonego max-view i nie tworzymy tabeli R15.

## Grupa i bazowe dane

Technical category: `ULTIMATE`, displayName **Moce Specjalne**, displayOrder **5**.
Skill group: `ultimate`, tree group: **Moce Specjalne / Ultimate**.
Label D4Guides „Specjalne” nie zastępuje nazwy z gry.
Kolejność: basic → core → corpse → macabre → curse → ultimate.

| Id / skill | Tagi bazowe | R1 / max-view | Interpretacja | Cooldown | Lucky Hit | Zasób |
| --- | --- | --- | --- | --- | --- | --- |
| `armia_umarlych` / Armia Umarłych | ULTIMATE, SUMMON, PHYSICAL | 120 / 306% | pojedyncza eksplozja Volatile Skeleton | 60 s | 64% | brak |
| `fala_krwi` / Fala Krwi | ULTIMATE, BLOOD, PHYSICAL | 500 / 1275% | fala z Knock Back | 50 s | 50% | brak |
| `nawalnica_kosci` / Nawałnica Kości | ULTIMATE, BONE, PHYSICAL | 400 / 1020% | DAMAGE_OVER_DURATION, 10 s | 60 s | 40% | brak |
| `spetanie_dusz` / Spętanie Dusz | ULTIMATE, DARKNESS, SHADOW | 350 / 893% | DAMAGE_OVER_DURATION, 8 s | 40 s | 20% | 2 Esencji / zdarzenie |

Wszystkie cztery: `DAMAGE / NEEDS_VERIFICATION`, cost `null`, charge data `null`,
activation requirements puste. Armia i Fala mają prostą tabelę; Nawałnica i Spętanie mają pustą prostą tabelę,
oba główne procenty `null` i wyłącznie komponentową tabelę z wymaganym duration.
Każdy upgrade ma `safeForRuntimeDps=NO`.

## Dokładne grupy 2 + 2 + 3

| Skill | Grupa 1: Enhancements | Grupa 2: Enhancements | Grupa 3: Transformation |
| --- | --- | --- | --- |
| Armia Umarłych | Redukcja Czasu Odnowienia; Premia do Obrażeń | Pasywna Premia; Generowanie Zwłok | Niezłomne Dowództwo; Piętrzenie Ciał; Martwe Zimno |
| Fala Krwi | Premia do Obrażeń; Redukcja Obrażeń | Przytłoczenie; Krwawa Kula | Krwawy Przypływ; Ścieżka Ciemności; Hematolagnia |
| Nawałnica Kości | Bariera; Odsłonięcie | Redukcja Obrażeń; Wydłużenie Czasu Działania | Rzucone Kości; Cień i Kość; Wygłodniały Cyklon |
| Spętanie Dusz | Szybkość Ruchu; Bariera | Zajadłość; Odsłonięcie i Ograniczanie Kontroli | Destylacja Duszy; Wir Dusz; Zamarznięte Pustkowia |

`SkillUpgradeGroup`, każdy `UpgradeDamageModifier.upgradeGroup` i `UpgradeDamageImpact.groupId`
pozostają spójne; golden test obejmuje wszystkie 23 skille, rozłączność node'ów i 12 kombinacji 2 × 2 × 3 na skill.
Grupy wyboru nie sumują efektów alternatyw.

## Armia Umarłych

Bazowe 120/306% oznacza **per explosion; liczba eksplozji nie jest stała**.
Volatile Skeletons pojawiają się przez 7 s; eksplozja daje Stun 2 s.
Ponowny cast podczas działania przywołuje więcej szkieletów, maksymalnie 5 stosów według 3.2.1.
Nie znamy liczby eksplozji, nie sumujemy ich i nie mnożymy przez casty ani maksimum stosów.

| Node | Reprezentacja / warunek |
| --- | --- |
| Redukcja Czasu Odnowienia | CAST_SPEED_OR_COOLDOWN, -20 s: 60 → 40 s; baza nadal 60 |
| Premia do Obrażeń | MULTIPLICATIVE_DAMAGE_PERCENT +2%[x] za posiadanego Miniona; bez zakładania ich liczby |
| Pasywna Premia | STATUS_DAMAGE_ENABLER, 1 szkielet co 4 s; utility bez PERIODIC_DAMAGE i liczby hitów aktywnego użycia |
| Generowanie Zwłok | DEFENSE_OR_UTILITY, 50% szansy na Zwłoki; nie resource generation ani activation requirement |
| Niezłomne Dowództwo | MULTIPLICATIVE_DAMAGE_PERCENT R1 +30%[x] obrażeń sług, scalesWithSkillRank=true; osobna obrona sług 90% |
| Piętrzenie Ciał | Cel skupia Armię, +10%[x] obrażeń otrzymywanych od Armii za eksplozję, maks. 300%; warunkowe stosy |
| Martwe Zimno | PRIMARY_DAMAGE wariantu 150/383% na eksplozję zastępuje 120/306; Cold, Freeze zamiast Stun |

**Niezłomne Dowództwo:** Blizzard i Icy Veins jawnie potwierdzają rank scaling +30%[x].
Główne D4Builds nadal pokazuje 30%. Brak potwierdzonego rank15; notes zachowują ograniczenie,
`safeForRankingDisplay=NEEDS_MANUAL_REVIEW`, impact `NEEDS_VERIFICATION`, runtime `NO`.
Znane R1 nie jest dopisywane pod 15 i nie jest interpolowane.
Obrona 90% pozostaje osobnym utility modifier. Bazowe tagi Armii nie przyjmują konwersji wariantów.

## Fala Krwi

| Node | Reprezentacja / warunek |
| --- | --- |
| Premia do Obrażeń | MULTIPLICATIVE_DAMAGE_PERCENT +30%[x] tylko while Fortified |
| Redukcja Obrażeń | 10% Damage Reduction na 10 s po użyciu |
| Przytłoczenie | 1 Overpower i Maximum Overpower +2, status bez damage coefficient |
| Krwawa Kula | Do 2 Blood Orbs podczas ruchu, bez gwarantowania dwóch |
| Krwawy Przypływ | +1 dodatkowa fala i oddzielne ADDITIONAL_STRIKE_DAMAGE 500/1275%, createsNewDamageComponent=true |
| Ścieżka Ciemności | Darkness/Shadow, Desecrated Ground 800/2040% Corrupting; oddzielny wariant DAMAGE_OVER_TIME z PRIMARY_DAMAGE |
| Hematolagnia | Core, koszt 70, no cooldown, krótsza fala bez Knock Back; PRIMARY_DAMAGE 300/765% zastępuje 500/1275 |

Ścieżka Ciemności: 800% potwierdzone zmianą 3.2.1; źródło nie podaje jednoznacznego duration przy coefficient.
Nie tworzymy fikcyjnego DAMAGE_OVER_DURATION ani DPS.
Krwawy Przypływ nie zmienia tabeli bazowej na 2 × 500 / 2 × 1275.
Hematolagnia ma osobne RESOURCE_OR_COST i CAST_SPEED_OR_COOLDOWN; baza pozostaje cost null / cooldown 50.
Przytłoczenie: użyto aktualnego opisu Icy Veins wskazanego w zadaniu.
Oficjalna zmiana 1 Overpower / Maximum Overpower +2 dotyczy wyboru Hematolagnii;
D4Builds i PL D4Guides zachowują mieszany starszy opis. Ograniczenie kontekstu jest jawne w notes.

## Nawałnica Kości

Baza: 400/1020% **łącznie przez 10 s**, wokół gracza i Golema; bez mnożenia przez obecność Golema.
Nie 400%/s.

| Node | Reprezentacja / warunek |
| --- | --- |
| Bariera | 50% Maximum Life na 10 s po użyciu |
| Odsłonięcie | Vulnerable 4 s |
| Redukcja Obrażeń | 10% Damage Reduction podczas działania |
| Wydłużenie Czasu Działania | RESOURCE_OR_COST: do 4 Zwłok; duration utility +2 s za faktycznie zużyte Zwłoki |
| Rzucone Kości | Crit Chance i Cast Speed +15%[+] / max-view +38%; dwa utility modifiers, source rank scaling |
| Cień i Kość | Darkness/Shadow wariantu, dodatkowy DAMAGE_OVER_DURATION 40/102% w ciągu 2 s |
| Wygłodniały Cyklon | Użycie na celu, wyszukiwanie wrogów; cooldown reduction R1 20 s / max-view 28 s |

Baza nie wymaga Zwłok. Upgrade nie dodaje activation requirement, nie wpisuje automatycznych 18 s,
nie liczy dodatkowych ticków ani total damage. Cień i Kość nie sumuje się strukturalnie z 400/1020 przez 10 s.
Utility 15/38 i 20/28 zachowuje oba punkty w tekstowych source modifiers, bez tabel obrażeń i interpolacji.
Bazowy cooldown 60 s pozostaje; effectiveCycleSeconds nie powstaje.

## Spętanie Dusz

Baza: DAMAGE_OVER_DURATION 350/893% przez 8 s.
`SkillResourceData(null, 2, 2, null, PER_EVENT)` oznacza dokładnie 2 Esencji za
**pochłonięcie jednej duszy przeciwnika**.
Absorpcja/sprawdzanie co **0.25 s**, dusze **3 przeciwników**, z Bossów **3 dusze jednocześnie**:
źródłowe notes, bez stałej liczby dostępnych wrogów. Nie przeliczamy tego na 6/0.25 s, 24/s lub 192/8 s.

| Node | Reprezentacja / warunek |
| --- | --- |
| Szybkość Ruchu | Unhindered podczas działania, +1% ruchu za duszę do +40%, premia utrzymuje się 5 s po zakończeniu |
| Bariera | 0.5% Maximum Life za duszę, utrzymuje się 5 s po zakończeniu |
| Zajadłość | 1 Ferocity co 30 pochłoniętych dusz, bez zakładania osiągnięcia progu |
| Odsłonięcie i Ograniczanie Kontroli | Vulnerable 4 s za duszę; po zakończeniu pobliscy wrogowie Fear/Slow 85% przez 5 s |
| Destylacja Duszy | Globalny warunkowy Darkness multiplier R1 +30%[x] / max-view +77%[x], scaling z rangą potwierdza Blizzard |
| Wir Dusz | Użycie na celu i Pull, EVENT_TRIGGERED_DAMAGE 25/64% za jedną duszę, osobna redukcja cooldownu 15 s |
| Zamarznięte Pustkowia | Frostbite/Chill 30% co sekundę w notes; BURST_DAMAGE 75/191% tylko przy pochłonięciu duszy Frozen enemy |

Destylacja Duszy nie jest component table Soulrift.
Wir Dusz nie ma interwału/duration i nie mnoży coefficient przez 3, cykle ani wrogów; baza cooldown 40 s.
Zamarznięte Pustkowia ma jawny warunek Frozen absorbed enemy i niezakładaną częstotliwość eksplozji.
Źródło nie podaje oddzielnego coefficient dla Frostbite/Chill; nie tworzymy go.

## Sparse tabele wariantów

| Wariant | Komponent | R1 / max-view | Duration | Znaczenie |
| --- | --- | --- | --- | --- |
| Martwe Zimno | PRIMARY_DAMAGE | 150 / 383 | brak | zastępuje eksplozję |
| Krwawy Przypływ | ADDITIONAL_STRIKE_DAMAGE | 500 / 1275 | brak | dodatkowa fala, +1 oddzielnie |
| Ścieżka Ciemności | PRIMARY_DAMAGE | 800 / 2040 | nieznany | dodatkowy trail |
| Hematolagnia | PRIMARY_DAMAGE | 300 / 765 | brak | zastępuje falę |
| Cień i Kość | DAMAGE_OVER_DURATION | 40 / 102 | 2 s | dodatkowy Corrupting |
| Wir Dusz | EVENT_TRIGGERED_DAMAGE | 25 / 64 | brak | jedna pochłonięta dusza |
| Zamarznięte Pustkowia | BURST_DAMAGE | 75 / 191 | brak | tylko Frozen absorbed enemy |

Wszystkie sparse tabele: dokładnie `{1:R1, 15:max-view}`, brak rang 2..14.
Żaden wariant nie mutuje bazowych tagów, kosztów, cooldownów lub activation requirements.

## HTTP i granica runtime

HTTP 200: cały Nekromanta 23 wiersze, Ultimate group/category/tag po 4.
SSR zachowuje 16 kolumn, total-over-duration oraz `2 / zdarzenie`.
Notes źródłowe są w neutralnym tooltipie nazwy każdego skilla, w tym jednostka pojedynczej eksplozji Armii.
Search indeksuje wszystkie 161 nazw upgrade'ów, kategorię Moce Specjalne, nowe nazwy, 0.25 s i zdarzenie.
Facet generowania uznaje PER_EVENT i zachowuje wcześniejszą regułę RESOURCE_OR_COST ulepszeń.
Sortowanie obu generowań używa źródłowej liczby 2, bez częstotliwości; braki na końcu w obu kierunkach.

Nie zmieniono HeroClass, SkillId, DamageEngine, ManualSimulationService i current build.
Nie dodano resource-rate engine, runtime Nekromanty, interpolacji ani ZIP-a.

