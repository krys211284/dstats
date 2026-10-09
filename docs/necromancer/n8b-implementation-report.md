# N8B — pierwszy standalone direct hit Nekromanty

Data wdrożenia: 08.10.2026. Baseline: Season 15 / live 3.2.2; scalar i katalog skilla z 3.2.1.

## Zakres i pliki

N8B dodaje rzeczywiste obliczenie wyłącznie bazowego `krwotok`, rank 1, single target, bez ulepszeń. Wynik to `DirectDamageResult` w granicy **PRE_TARGET_MITIGATION**. Nie dodaje klasy do current build ani symulacji.

Dodane:

- `src/main/java/krys/necromancer/combat/NecromancerSeason15DirectDamageProfile.java`
- `src/main/java/krys/necromancer/combat/NecromancerSeason15DirectDamageResolver.java`
- `src/test/java/krys/necromancer/combat/NecromancerSeason15DirectDamageResolverTest.java`
- `src/test/java/krys/necromancer/combat/NecromancerSeason15DirectDamageArchitectureTest.java`
- `docs/necromancer/n8b-implementation-report.md`

Zmieniony istniejący plik: `README.md` — bieżący status i kontrakt sekcji 5.6, diagram obu resolverów oraz źródła. Wcześniejsze zmiany N0–N8A i inne zastane zmiany repo są osobnym zakresem; audyt N8B porównuje hash plików ze świeżym stanem sprzed tego etapu, nie z HEAD.

## Źródła i wersja

Źródła sprawdzono bezpośrednio 08.10.2026:

| Rola | Źródło | Zastosowanie |
|---|---|---|
| OFFICIAL | [Blizzard — 3.2.1, Season of Hell's Legacy](https://news.blizzard.com/en-us/article/24295394/celebrate-30-years-of-diablo-in-season-of-hell-s-legacy) | Necromancer core-stat scalar 1.25 → 1.625, +1.625% damage / 10 INT |
| OFFICIAL | [Blizzard — 3.2.2, 30.09.2026](https://news.blizzard.com/en-us/article/24295395/diablo-iv-patch-notes) | W zmianach klas wymieniono Druida i Sorcerera; brak zmiany Nekromanty |
| COMMUNITY CURRENT MECHANICS REFERENCE | [D4Guides — Damage Calculation](https://d4guides.gg/en/guides/mechanics/damage-calculation-in-diablo-4-how-your-damage-works) | Współczesne buckets Season 15 / 3.2.2, base crit / Vulnerable, conditional `[+]`; aktualizacja 05.10.2026 |
| CATALOG | [D4Guides — Hemorrhage](https://d4guides.gg/en/database/skills/hemorrhage) | R1 80% Physical; strona oznacza Season 15 / 3.2.1 |

Community guide nie jest oficjalną dokumentacją Blizzarda. N8B nie aktualizuje danych katalogowych na podstawie rozbieżności między źródłami.

## Profil standalone i API

Immutable record `NecromancerSeason15DirectDamageProfile` ma dokładnie:

```text
averageWeaponDamage
intelligence
additiveDamagePercent
damageBucketPercent
resolvedSeparateMultiplicativeMultiplier
criticalStrikeDamageAdditivePercent
criticalStrikeDamageMultiplierPercent
vulnerableDamageAdditivePercent
vulnerableDamageMultiplierPercent
targetVulnerable
```

Dziewięć liczb typu double musi być skończonych i nieujemnych. Procenty to punkty procentowe, np. 40 oznacza +40%; separate multiplier to rozwiązany iloczyn, np. 1.30. Zerowa broń i separate multiplier są dozwolone. Nie ma item resolvera, szansy kryta ani target armor / enemy reduction.

Publiczne `calculate(String skillId, int rank, profile)` akceptuje tylko `krwotok`, rank 1. Inne skille, null/pusty identyfikator i rank != 1 kończą się jawnym `UnsupportedOperationException`. `resolveContext(profile)` udostępnia rozwiązane wejście matematyczne, bez rozszerzania wsparcia skilli. Nieznane 2..14 i max-view 15 nie są wykonywane.

## Matematyka poza kernelem

```text
mainStat = 1 + intelligence * 0.001625
A = 1 + additiveDamagePercent / 100
    + (targetVulnerable ? vulnerableDamageAdditivePercent / 100 : 0)
damageBucket = 1 + damageBucketPercent / 100
multiplicative = damageBucket * resolvedSeparateMultiplicativeMultiplier
vulnerable = targetVulnerable ? 1.2 * (1 + vulnerableDamageMultiplierPercent / 100) : 1
criticalAdditive = A + criticalStrikeDamageAdditivePercent / 100
critical = (criticalAdditive / A) * 1.5 * (1 + criticalStrikeDamageMultiplierPercent / 100)
```

INT 0 / 100 / 1000 daje ×1 / ×1.1625 / ×2.625, bez bonusu krytycznego z INT. `A>=1` eliminuje dzielenie przez zero. Crit `[+]` wchodzi do wspólnej sumy wyłącznie dla kryta; oba bonusy Vulnerable są pomijane dla zwykłego celu. Przepełnienia rozwiązanych mnożników / wyników odrzuca walidacja neutralnego contextu / kernelu.

Context ustawia weaponMultiplier=1, postMitigationMultiplier=1 i `EXACT_COMPONENT_SUM`. **Wynik jest PRE_TARGET_MITIGATION**, dlatego raw==final w obu wariantach; neutralne nazwy `finalDamage` i `criticalDamage` nie oznaczają wdrożonej redukcji celu. `*Exact` nadal oznacza double przed `Math.round`, nie arbitralną precyzję.

## Coefficient i komponent

`NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill("krwotok")` dostarcza `CharacterTreeSkill`. Wartość R1 pobierana jest z `getBaseDamagePercentRanks().damagePercentAtRank(rank)`, następnie przekazywana jako `BigDecimal` do helpera `baseHitSpec`. Resolver nie ma stałej 80.

Spec: `Krwotok — bazowe trafienie`, source z `getSourceReference()`, coefficient 80 z katalogu, hitCount=1, condition=null, active=true, includedInSingleTarget=true, exclusionReason=null. Helper przyjmuje i zachowuje również 62.5 i 7.5 bez zawężania do long/int. Test tych wartości nie zmienia katalogu ani nie włącza kolejnego skilla runtime.

## Niezależne goldeny

Wspólne wejście A/B/C: broń 1000, INT 1000, coefficient 80% → base 800 i mainStat ×2.625.

| Golden | Mnożniki / wejścia | Normal | Crit |
|---|---|---:|---:|
| A | Wszystkie procenty 0, separate=1, zwykły cel | 2100 | 3150 |
| B | A, ale Vulnerable ×1.2 | 2520 | 3780 |
| C | Additive +300%, damage bucket +40%, separate ×1.30, crit additive +100%, CSD +60%, Vulnerable multiplier +25% | 22932 | 68796 |

C: `800 × 2.625 × 4 × 1.4 × 1.3 × 1.5 = 22932`; critical factor `(4+1)/4 × 1.5 × 1.6 = 3`; kryt `22932 × 3 = 68796`. A/B/C asercje obejmują niezaokrąglone total oraz oba warianty raw/final.

D: Vulnerable +100% additive / +25% multiplier przeciw zwykłemu celowi nadal daje 2100/3150. Vulnerable additive +100% na celu Vulnerable podwaja B do 5040/7560. Crit additive +100% przy A=1 pozostawia zwykłe 2100 i daje kryt 6300. Łączne Vulnerable additive +100% i crit additive +100% daje normal 5040 i kryt 11340: krytyczna suma wynosi 3, nie iloczyn dwóch osobnych +100%. Wszystkie te liczby wynikają z dostarczonych A/B i jawnej algebry. Decimal helper przy broni 100 / INT 0: 62.5 → raw 62.5 / crit 93.75 → 63/94; 7.5 → raw 7.5 / crit 11.25 → 8/11.

## Architektura i regresje

Nowa produkcja zależy wyłącznie od JDK, neutralnych `DirectDamage*`, istniejącego `NecromancerSkillTreeRegistry` i `CharacterTreeSkill`. Test skanuje importy oraz kwalifikowane odwołania, blokuje legacy tożsamość, engine/breakdown, web, current build, search i symulację; odrzuca też zawężenia BigDecimal do long/int oraz duplikację 80. Istniejący test architektury pięciu klas kernelu pozostaje wymagany.

Katalog jest niezależny od wsparcia hitu: wszystkie źródłowe statusy i safety są niezmienione. Regresje potwierdzają Necromancer 23/161, Book 3/9/27, Paladin 24. Krwotok zachowuje R1 80, max-view 204, Lucky Hit 50, generowanie Esencji 13 i siedem upgrade'ów. HeroClass pozostaje tylko PALADIN; SkillId nadal BRANDISH / HOLY_BOLT / CLASH / ADVANCE.

Pięć plików kernelu i `DamageEngine.java` pozostają byte-for-byte zgodne z N8A. Nie zmieniono legacy divisor 800, INT → crit, Verathiel, Moloch, CriticalRoundingPolicy ani DamageBreakdown. Nie zmieniono current build, loadoutu, simulation, build search, persistence ani katalogów. Nie wykonujemy Esencji, Lucky Hit, Blood Orb, upgrade'ów, Book, Overpower, crit chance / expected crit, mitigation ani DPS. ZIP nie jest tworzony.

## Walidacja

Testy celowane: **430 testów / 59 suite, 0 failures / 0 errors / 0 skipped, BUILD SUCCESS**, 21.700 s. Zakończenie: 08.10.2026 07:56:16 CEST. Sumy potwierdzono z XML Surefire. Polecenie:

```powershell
mvn '-Dmaven.repo.local=.m2' clean test '-Dtest=DirectDamage*Test,*DamageEngine*Test,ReactiveDamageFoundationTest,*ManualSimulation*Test,HeroBuildSnapshotTest,*CurrentBuild*Test,BuildSearch*Test,SearchBuild*Test,Necromancer*Test,BookOfDead*Test,Paladin*Test,CharacterSkillTreeRegistryTest,Neutral*Test'
```

Nowe testy N8B: **23 resolver/profile + 2 architektura = 25**, wszystkie bez błędów. Istniejący kernel: 21 matematycznych + 2 architektoniczne bez błędów. Dotychczasowe goldeny DamageEngine / Reactive (19 testów w pięciu suite), manual simulation / snapshot (31 w sześciu suite), build search (15 w sześciu suite) i regresje current build przechodzą bez zmiany oczekiwań.

Audyt zakresu względem świeżej migawki 541 istniejących plików: **540 bez zmian, tylko README zmieniony, pięć nowych plików**. Szczególnie `DamageEngine.java` i wszystkie pięć klas neutralnego kernelu zachowują identyczne SHA-256. `git -c core.safecrlf=false diff --check` przechodzi; katalog główny i target nie zawierają ZIP-a.

Pełny `mvn '-Dmaven.repo.local=.m2' clean test`: **847 testów / 123 suite, 0 failures / 0 errors / 0 skipped, BUILD SUCCESS**, 22 min 55 s. Zakończenie: **08.10.2026 08:19:51 CEST**. Liczby potwierdzono niezależnie z `target/surefire-reports/TEST-*.xml`. Wynik obejmuje wszystkie 822 zastane testy oraz 25 nowych N8B. Przeszły dotychczasowe goldeny Paladyna, neutralny kernel, katalogi, Book, current build, manual simulation, search, HTTP, persistence i import obrazów / OCR. Istniejące testy i oczekiwania nie zostały zmienione.

W czasie pełnego uruchomienia wszystkie **546 zamrożonych plików** zachowały identyczne SHA-256. Po sukcesie zmieniono wyłącznie ten raport, dopisując wynik walidacji; kod i README odpowiadają dokładnie przetestowanej wersji. Końcowy zakres N8B to jeden zmieniony istniejący plik (README) oraz pięć dodanych. ZIP nie został utworzony.
