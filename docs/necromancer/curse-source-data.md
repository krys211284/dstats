# N5 — Klątwy / Curse Nekromanty: dane źródłowe

Baseline: **Diablo IV, Sezon 15 / patch 3.2.1**. Odczyt źródeł: 7 października 2026.
Zakres: Żelazna Dziewica i Zniedołężnienie, 14 ulepszeń. Po N5 katalog obejmuje 19 skilli i 133 ulepszenia.
Dane wcześniejszych 17 skilli i ich 119 ulepszeń pozostają zachowane.

## Źródła i rozstrzygnięcia

- [D4Guides EN: Iron Maiden](https://d4guides.gg/en/database/skills/iron-maiden), [PL: Żelazna Dziewica](https://d4guides.gg/pl/baza-danych/umiejetnosci/zelazna-dziewica): R1, koszt i efekty; polskie nazwy.
- [D4Guides EN: Decrepify](https://d4guides.gg/en/database/skills/decrepify), [PL: Zniedołężnienie](https://d4guides.gg/pl/baza-danych/umiejetnosci/zniedoleznienie): bazowe utility i ulepszenia.
- [Icy Veins: Necromancer Skills Guide, sekcja Curse](https://www.icy-veins.com/d4/guides/necromancer-skills/): rozstrzygające, jawne Group 1 / Group 2 / T; Lucky Hit Żelaznej Dziewicy 5%.
- [D4Builds: główna domena Class Skills](https://d4builds.gg/database/class-skills/): max-view obu mechanizmów 77%, burst 383%, Slow 56%, Daze 4.2 s, leczenie 25.5%, szybkość sług 51%.
- [Blizzard: oficjalne patch notes 3.2.1, Necromancer Bug Fixes](https://news.blizzard.com/en-us/article/24295394/celebrate-30-years-of-diablo-in-season-of-hell-s-legacy): naprawa Aspect of Crippling Darkness, który błędnie nadawał bazowemu Iron Maiden tag Darkness i konwersję Shadow.

Kolejność rozstrzygania: oficjalny Blizzard, aktualny jawny tekst D4Guides/Icy Veins,
główna domena D4Builds dla max-view, nierozstrzygnięty konflikt do ręcznej weryfikacji.
Nie użyto danych branch/subdomain D4Builds. Widok max zapisano projektowo pod rangą 15;
strona nie etykietuje go jawnie „Rank 15”. Status skilli pozostaje `NEEDS_VERIFICATION`.
Współczynniki mają tylko punkty 1 i 15, bez interpolacji 2..14.
Polski podgląd Żelaznej Dziewicy pokazuje liczby bezwzględne zależne od podglądu;
nie przeliczamy ich na procent obrażeń broni. X Cierni pozostaje dynamiczne, jak w Icy Veins/D4Builds.

## Dane bazowe i jednostki

Kategoria `CURSE / Klątwy / displayOrder=4`, grupa `curse / Klątwy / Curse`,
kolejność Basic → Core → Corpse → Macabre → Curse.

| Pole | Żelazna Dziewica | Zniedołężnienie |
| --- | --- | --- |
| Id / nazwa EN | `zelazna_dziewica` / Iron Maiden | `zniedoleznienie` / Decrepify |
| Typ | `DAMAGE` | `SUPPORT` |
| Bazowe tagi | CURSE, PROFANE, PHYSICAL | CURSE, PROFANE |
| Koszt Esencji | 10 | 10 |
| Bazowe generowanie | brak | brak |
| Lucky Hit | 5% | brak źródłowej wartości |
| Zwykły cooldown / charge data | brak / brak | brak / brak |
| Wymaganie aktywacji | brak | brak |
| Czas klątwy, notes | 30 s | 30 s |
| Prosta tabela / główne procenty | pusta / null, null | pusta / null, null |
| Utility bazowe | przeklina obszar | Weaken; Slow R1 40%, max-view 56% |
| Komponenty bazowe | dwa opisane poniżej | brak; SSR „nie dotyczy” |

Żelazna Dziewica ma **dwa odrębne mechanizmy**:

| Komponent | R1 | Max-view pod 15 | Interwał | Duration komponentu | Znaczenie |
| --- | --- | --- | --- | --- | --- |
| `PERIODIC_DAMAGE` | 30% | 77% | 2 s | null | pojedynczy impuls co 2 s pod wpływem klątwy |
| `EVENT_TRIGGERED_DAMAGE` | 30% | 77% | null | null | pojedyncze zdarzenie: przeklęty wróg zadaje bezpośrednie obrażenia |

30 s jest czasem działania klątwy w notes, nie okresem, za który podano 30% obrażeń.
Nie zapisujemy 450% over 30 s, 15 ticków, 30%/sek. ani sumy periodic + event.
Nie zakładamy częstotliwości zdarzeń. `intervalSecondsFor` przechowuje dodatni odstęp
między impulsami niezależnie od `durationSecondsFor`; mapy są niemutowalne.
Brak interwału jest dozwolony. Interwał przypisany do nieobecnego komponentu jest błędem.
Dotychczasowe `DAMAGE_OVER_DURATION` nadal wymaga jawnego duration.
SSR i search zachowują jednostki „30% co 2 s” oraz „30% na zdarzenie” również w tabelach wariantów.

## Żelazna Dziewica — dokładne grupy

| Grupa | Id | Nazwa | Źródłowa semantyka / reprezentacja |
| --- | --- | --- | --- |
| 1 | `generowanie_esencji` | Generowanie Esencji | dwa RESOURCE_OR_COST: koszt wariantu 0; +5 Esencji za każdego nowo przeklętego wroga, który nie był już pod wpływem Iron Maiden |
| 1 | `egzekucja_i_umocnienie` | Egzekucja i Umocnienie | STATUS egzekucji bez dopisanego progu; DEFENSE: śmierć przeklętego wroga daje Umocnienie 5% maksymalnego zdrowia |
| 2 | `zajadlosc_i_przytloczenie` | Zajadłość i Przytłoczenie | STATUS: Lucky Hit do 15% na losowo 1 Ferocity albo 1 Overpower |
| 2 | `odsloniecie` | Odsłonięcie | STATUS: przeklęci wrogowie Vulnerable |
| 3 / T | `zlosliwa_radosc` | Złośliwa Radość | THORNS_DAMAGE_MODIFIER: przy co najmniej 1 przeklętym wrogu X Cierni i obrażenia zwiększone o 100% własnych Cierni |
| 3 / T | `mistrz_malodobry` | Mistrz Małodobry | STATUS/notes: Darkness i Shadow tylko wariantu, oba coefficient 30/77 zachowane; 30% szansy na Stun 2 s przy obrażeniach klątwy |
| 3 / T | `krwawa_pannica` | Krwawa Pannica | Blood wariantu; 5% szansy na Blood Orb; osobny burst 150/383 i utility zbierania 4 Blood Orbs |

Generowanie Esencji nie mutuje bazowego kosztu 10 i nie wpisuje +5 do bazowego generation.
Nie zakładamy liczby wrogów. Lucky Hit ulepszenia 15% jest odrębne od bazowego 5%.
Złośliwa Radość jest analogiczna do Ciernia z N4: `TEXT_ONLY`, bez tabeli obrażeń broni,
`safeForRankingDisplay=NEEDS_MANUAL_REVIEW`, `safeForRuntimeDps=NO`,
impact `NEEDS_VERIFICATION` z damage percent null. Źródłowe X zależy od poziomu/widoku,
nie jest stałym Weapon Damage ani zwykłym +100%[x].

Krwawa Pannica zachowuje osobną wariantową tabelę
`BURST_DAMAGE {1:150, 15:383}`, bez duration/interwału, bez wpisania do bazowych komponentów.
Zebranie 4 Blood Orbs to oddzielny `DEFENSE_OR_UTILITY / HIT_COUNT`.
Nie mnożymy burstu przez 4 i nie zakładamy bezwarunkowej dostępności kul.
Blood nie trafia do bazowych tagów. Mistrz Małodobry również nie zmienia bazowych PHYSICAL tagów.

## Zniedołężnienie — dokładne grupy

| Grupa | Id | Nazwa | Źródłowa semantyka / reprezentacja |
| --- | --- | --- | --- |
| 1 | `szansa_na_trafienie_krytyczne` | Szansa na Trafienie Krytyczne | STATUS: +5%[+] crit przeciw przeklętemu wrogowi; max-view potwierdza tę samą wartość |
| 1 | `redukcja_czasu_odnowienia` | Redukcja Czasu Odnowienia | CAST_SPEED_OR_COOLDOWN: Lucky Hit do 15% na redukcję losowego aktywnego cooldownu o 1 s |
| 2 | `szybkosc_ruchu` | Szybkość Ruchu | DEFENSE: +2%[x] ruchu za każdego przeklętego wroga |
| 2 | `animusz` | Animusz | DEFENSE: Lucky Hit do 25% na 1 Resolve |
| 3 / T | `oszalamiajaca_klatwa` | Oszałamiająca Klątwa | STATUS: Daze R1 3.0 s / max-view 4.2 s przy pierwszym objęciu klątwą |
| 3 / T | `upuszczanie_zdrowia` | Upuszczanie Zdrowia | DEFENSE: Blood wariantu; leczenie R1 10% / max-view 25.5% maksymalnego zdrowia po trafieniu, maksymalnie raz na sekundę; śmierć daje 20% szansy Blood Orb |
| 3 / T | `swietokradczy_amok` | Świętokradczy Amok | CAST_SPEED i DEFENSE/Movement Speed własnych Minions R1 +20%[x] / max-view +51%[x]; zastępuje cel i efekt bazowej klątwy |

Slow 40/56, Daze 3.0/4.2, leczenie 10/25.5 i szybkość 20/51 są zachowane w notes/modifierach utility.
Nie używają `DamagePercentRankTable`, nie są damage coefficient i nie interpolujemy rang.
Cooldown Reduction jest warunkowe i dotyczy losowego aktywnego cooldownu,
nie nadaje bazowego odnowienia Zniedołężnieniu. Szybkość Ruchu nie zakłada liczby wrogów ani maksimum.

**Świętokradczy Amok zastępuje cel i efekt:** własne Minions zamiast wrogów.
Wariant daje Cast Speed i Movement Speed sługom, nie przenosi bazowego Weaken/Slow na wrogów.
Bazowy CharacterTreeSkill pozostaje SUPPORT, z niezmienionymi tagami i efektem źródłowym.

## Integracja i granica runtime

Każdy skill ma 2 Enhancements w grupie 1, 2 w grupie 2 oraz 3 alternatywne Transformation w grupie 3.
Golden membership pokrywa wszystkie 19 skilli, zbiory, wszystkie modifiers/impacts,
rozłączność oraz dokładnie 12 wyborów 2 × 2 × 3 per skill.
HTTP daje 200: 19 wierszy bez filtra; grupa/kategoria/tag Curse po 2.
Wspólny search indeksuje wszystkie 133 nazwy ulepszeń, nowe nazwy i notes 30 s/interwał 2 s,
bez warunku zależnego od klasy lub kategorii.

Wszystkie nowe modyfikatory mają `safeForRuntimeDps=NO`.
Nie zmieniono HeroClass, SkillId, DamageEngine, ManualSimulationService, current build,
SkillChargeData, SkillActivationRequirement ani semantyki cooldownu.
Ranking nie liczy DPS, liczby zdarzeń ani sumy obrażeń klątwy. N5 nie tworzy ZIP-a.

