# N8A — neutralny direct-damage kernel

N8A wydziela neutralną matematykę pojedynczego direct damage z istniejącego DamageEngine. Jest refaktorem zachowującym zachowanie legacy Paladyna; nie uruchamia Nekromanty i nie aktualizuje formuł klasowych do Season 15. Raport dotyczy zmian względem świeżego stanu zastanego po N7, z zachowaniem wcześniejszych lokalnych zmian repo.

## 1. Lista plików

Zmodyfikowano:

- `src/main/java/krys/combat/DamageEngine.java` — adapter obu publicznych ścieżek direct hit do neutralnego kernelu; reactive i resolvery pozostają bez zmian.
- `README.md` — rozdział resolverów legacy i czystej matematyki, nowe modele / testy oraz plan N8B / N9. Istniejący opis main stat uściślono do realnego dzielnika 800; opis redukcji uwzględnia istniejący wyjątek poziomu 70. Kod reguł statystyk nie został zmieniony.

Dodano:

- `src/main/java/krys/combat/DirectDamageCalculator.java`
- `src/main/java/krys/combat/DirectDamageContext.java`
- `src/main/java/krys/combat/DirectDamageComponentSpec.java`
- `src/main/java/krys/combat/DirectDamageComponentResult.java`
- `src/main/java/krys/combat/DirectDamageResult.java`
- `src/test/java/krys/combat/DirectDamageCalculatorTest.java`
- `src/test/java/krys/combat/DirectDamageKernelArchitectureTest.java`
- `docs/necromancer/n8a-implementation-report.md`

Łącznie: **10 plików N8A — 2 zmodyfikowane / 8 nowych**. Nie zmieniono istniejących testów ani ich oczekiwanych wyników.

## 2. Finalny model kernelu

Wszystkie pięć typów produkcyjnych znajduje się w `krys.combat`. Calculator jest stateless; cztery modele są immutable records. Wejściowy kontekst zawiera już rozwiązane liczby i neutralną politykę zaokrągleń. Component spec opisuje współczynnik, hit count oraz gotowe flagi / etykiety aktywności i single target. Component result zawiera spec, niezaokrąglone wartości bazowe / raw / raw critical i zaokrąglone wartości debug. Result przechowuje kontekst, immutable listę komponentów, niezaokrąglone sumy raw / raw critical i cztery końcowe longi. Wynik nie zawiera klasy, skilla, zasobów lub stanu symulacji.

`DirectDamageCalculator.calculate(context, specs)` kopiuje listy i oblicza każdy komponent w zadanej kolejności. Nie rozwiązuje warunków opisanych conditionLabel. Aktywny wykluczony komponent pozostaje obliczony w debugu; nieaktywny ma zerowe raw / final. Żaden z nich nie trafia do totalu single target. Pusta lista i broń o wartości zero dają legalny wynik zero.

## 3. Pola DirectDamageContext

| Pole | Kontrakt |
| --- | --- |
| averageWeaponDamage | Skończona nieujemna średnia obrażeń; 0 jest legalne |
| weaponMultiplier | Gotowy nieujemny mnożnik broni |
| mainStatMultiplier | Gotowy nieujemny mnożnik głównej statystyki |
| additiveMultiplier | Gotowy nieujemny mnożnik bonusów addytywnych |
| vulnerableMultiplier | Gotowy nieujemny mnożnik celu |
| multiplicativeMultiplier | Gotowy nieujemny iloczyn dodatkowych mnożników |
| criticalMultiplier | Gotowy nieujemny mnożnik kryta |
| postMitigationMultiplier | Gotowy mnożnik po redukcji w [0,1] |
| criticalTotalRoundingPolicy | EXACT_COMPONENT_SUM albo ROUNDED_RAW_TOTAL |

Konstruktor z ośmioma liczbami wybiera domyślną politykę EXACT_COMPONENT_SUM. Walidacja odrzuca NaN, ±Infinity, ujemne wejścia, brak polityki, nieprawidłowy coefficient / hit count oraz nieskończony wynik mnożenia lub sumy. Metadane tekstowe spec mogą pozostać null, zgodnie z wcześniejszym kontraktem opcjonalnych etykiet standalone / debug.

## 4. Decimal coefficient

`DirectDamageComponentSpec.damagePercent` jest BigDecimal, nie long; zachowuje źródłową wartość i precyzję, np. 62.5, 7.5 lub 62.50. Coefficient musi być nieujemny i mieć skończoną reprezentację double używaną przez pipeline. `SkillDef.long[]` i `DamageComponentBreakdown.long skillDamagePercent` pozostają bez zmian. Adapter tworzy BigDecimal.valueOf z istniejących longów i mapuje je do debugowego long przez longValueExact.

Obliczenia zachowują arytmetykę double legacy. Nazwy `*Exact` oznaczają wartości przed Math.round, nie arytmetykę arbitralnej precyzji. BigDecimal nie służy do przebudowy numerycznego pipeline'u Paladyna.

## 5. Exact total przed rounding

Kernel zachowuje dokładnie dotychczasową kolejność operacji:

```text
base = averageWeaponDamage * (damagePercent.doubleValue() / 100.0)
raw = base * weaponMultiplier * mainStatMultiplier * additiveMultiplier
      * vulnerableMultiplier * multiplicativeMultiplier * 1.0 * hitCount
rawCritical = base * weaponMultiplier * mainStatMultiplier * additiveMultiplier
      * vulnerableMultiplier * multiplicativeMultiplier * criticalMultiplier * hitCount
totalRaw = suma raw aktywnych komponentów single target w ich kolejności
totalRawCritical = analogiczna suma rawCritical
rawDamage = Math.round(totalRaw)
finalDamage = Math.round(totalRaw * postMitigationMultiplier)
```

Kryt EXACT_COMPONENT_SUM: Math.round(totalRawCritical) i Math.round(totalRawCritical × postMitigation). Kryt ROUNDED_RAW_TOTAL: Math.round(rawDamage × criticalMultiplier), następnie Math.round(roundedRawCritical × postMitigation). Component debug nadal używa własnego exact pipeline'u. Kernel przechowuje też exact sumę komponentów krytycznych przy polityce ROUNDED_RAW_TOTAL, aby nie ukrywać różnicy obu podstaw zaokrąglenia.

Hit count jest matematycznie równoważnym czynnikiem wzoru; jego pozycja po krycie pozostaje celowo identyczna z legacy, aby nie zmienić rezultatów binary floating point. Nie sumujemy zaokrąglonych komponentów. Golden rounding: dwa komponenty 62.5 dają raw total **125**, mimo debugowej sumy **126**; przy postMitigation 0.5 final total to **63**, mimo debugowej sumy **62**.

## 6–7. Adapter DamageEngine i granica klasowa

DamageEngine zachowuje publiczne calculate(...) z obiema sygnaturami i calculateStandaloneHit(...). Wcześniejsze resolvery nadal wyznaczają HeroClassDef, statystyki, legacy Intelligence crit, Vulnerable, redukcję poziomu, Verathiela, Molocha, SkillDef, base replacement i aktywność efektów. Adapter buduje neutralny context i specs, uruchamia calculator oraz mapuje neutralne wartości do istniejącego DamageBreakdown / DamageComponentBreakdown. Standalone ma nadal mnożniki Verathiela / Molocha równe 1.

SkillDef/CriticalRoundingPolicy wybiera w adapterze, kiedy użyć neutralnej ROUNDED_RAW_TOTAL; kernel nie zna powodu wyboru lub Brandish. Dzielnik 800 pozostaje w HeroClassDefs / resolverach, a Intelligence → crit, level reduction i efekty itemów / przysięgi pozostają po stronie legacy. Reactive damage nie jest migrowane w N8A. Wiara, Animusz, delayed triggers, ticki, action bar i search nie są częścią kernelu.

Nowe klasy mają wyłącznie importy JDK i odniesienia do własnych modeli. Nie zależą od krys.paladin, krys.necromancer, krys.skill, krys.web, krys.ranking, krys.simulation lub HeroBuildSnapshot. Test architektury czyta tylko pięć nowych plików kernelu i kontroluje również typy pól oraz brak nazw / tożsamości klasowych.

## 8–10. Testy i wyniki

Nowe testy: **21 testów DirectDamageCalculatorTest + 2 testy DirectDamageKernelArchitectureTest**. Wszystkie przeszły. Goldeny z kontraktu N8A: 100% →100, 62.5% →63, 7.5% →8, 30% ×3 trafienia →90. Dodatkowy jawny golden wszystkich mnożników: 100×2×1.2×1.5×1.2×2 =864 raw, kryt ×1.5 =1296, postMitigation 0.5 daje 432 / 648. Pokryto aktywność, single target, rounding totalu, obie polityki kryta, zerową broń, walidację i kopie kolekcji.

Istniejące goldeny DamageEngine, reactive i ManualSimulation zachowano bez zmiany oczekiwań: **19 testów / 5 suite** engine / reactive oraz **31 testów / 6 suite** symulacji / HeroBuildSnapshot, wszystkie bez błędów. Obejmują Brandish rank 1 / 5, dodatkowe komponenty, boczne łuki, Clash, Verathiela, Molocha, krytyki, standalone Judgement, delayed hit, zasoby, kolejność ticków i deterministyczny roll kryta. Ukierunkowane testy obejmują także current build, build search, HTTP, katalogi i Book.

Ukierunkowany clean test: **405 testów / 57 suite**, 0 failures / errors / skipped, **BUILD SUCCESS**, 21.687 s, zakończenie **08.10.2026 05:52:16 CEST**. Polecenie:

```powershell
mvn '-Dmaven.repo.local=.m2' clean test '-Dtest=DirectDamage*Test,*DamageEngine*Test,ReactiveDamageFoundationTest,*ManualSimulation*Test,HeroBuildSnapshotTest,*CurrentBuild*Test,BuildSearch*Test,SearchBuild*Test,Necromancer*Test,BookOfDead*Test,Paladin*Test,CharacterSkillTreeRegistryTest,Neutral*Test'
```

Pełny `mvn '-Dmaven.repo.local=.m2' clean test`: **822 testy / 121 suite**, **0 failures / 0 errors / 0 skipped**, **BUILD SUCCESS**, 22 min 52 s. Zakończenie: **08.10.2026 06:18:32 CEST**. Sumy potwierdzono niezależnie z `target/surefire-reports/TEST-*.xml`. Pełny wynik obejmuje 23 nowe testy i wszystkie 799 testów zastanego zakresu repo, bez zmiany istniejących oczekiwań; current build, build search, manual simulation, HTTP, persistence, katalogi i import obrazów / OCR przechodzą regresje.

## 11–16. Zachowany zakres repo

- Nekromanta: **23 skille / 161 upgrade'ów**, katalog-only.
- Book of the Dead: **3 rodziny / 9 specjalizacji / 27 wyborów**, bez runtime wykonania.
- Paladyn: **24 skille**.
- HeroClass: wyłącznie **PALADIN**.
- SkillId: dokładnie **BRANDISH, HOLY_BOLT, CLASH, ADVANCE**.
- DamageBreakdown i DamageComponentBreakdown zachowują dotychczasową strukturę i typy.
- HeroSkillLoadout, CurrentBuildFormData, CurrentBuildRequest, SkillState, PaladinSkillDefs, ManualSimulationService i BuildSearch nie są migrowane lub zmieniane.
- Current build, search, simulation behavior i wspólny pipeline pozostają legacy Paladin runtime. Nie liczymy Nekromanty, Book, Essence, Lucky Hit ani DPS z cooldownów.

Kontrola SHA-256 względem świeżego snapshotu **534 plików** stanu po N7: zmieniono wyłącznie DamageEngine.java i README.md; **532 pozostałe istniejące pliki są identyczne**, w tym wszystkie wcześniejsze testy, katalogi i warstwy current build / simulation / search. Dodano 8 plików N8A. Oddzielne porównanie fragmentów DamageEngine potwierdza identyczność reactive foundation, base replacement / warunków i resolverów level reduction / Verathiel. Chronimy stan zastany, nie starszy HEAD. Wszystkie **542 hashe** zamrożonego kodu / dokumentacji były identyczne przed i po pełnym teście. Po zakończeniu testów uzupełniono wyłącznie ten raport o faktyczne wyniki. `git -c core.safecrlf=false diff --check` przechodzi bez błędów.

## 17. Dostawa

ZIP nie został utworzony. Nie uruchamiano package / assembly; końcowa kontrola potwierdziła 0 ZIP-ów w katalogu głównym repo i w target. **N8A jest zamknięte**. N8B (pierwszy standalone direct hit Nekromanty na aktualnym profilu Season 15) i N9 (neutralna runtime identity / migracja loadoutu) pozostają osobnymi, niewdrożonymi etapami.
