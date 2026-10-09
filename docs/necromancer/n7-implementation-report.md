# N7 — raport implementacji Book of the Dead

Zakres dostawy: własny katalog źródłowy Nekromanty, Season 15 / patch 3.2.1. Szczegółowe dane i odnośniki do źródeł: [book-of-the-dead-source-data.md](book-of-the-dead-source-data.md). Raport dotyczy zmian N7 względem stanu zastanego po N6, łącznie z istniejącymi lokalnymi zmianami.

## 1. Zmienione / dodane pliki

Zmieniony istniejący plik: `README.md` — kontrakt osobnej domeny Book, liczebności, jednostki, Sacrifice i granica runtime.

Nowe pliki produkcyjne w `src/main/java/krys/necromancer/bookofthedead/`:

- `NecromancerMinionFamily.java`
- `NecromancerMinionSpecializationId.java`
- `NecromancerMinionTheme.java`
- `BookOfDeadChoiceType.java`
- `BookOfDeadRuntimeStatus.java`
- `BookOfDeadSourceFact.java`
- `BookOfDeadSummonPenalty.java`
- `BookOfDeadChoiceDefinition.java`
- `NecromancerMinionSpecializationDefinition.java`
- `NecromancerMinionRules.java`
- `NecromancerBookOfDeadRegistry.java`

Nowe testy w `src/test/java/krys/necromancer/bookofthedead/`:

- `NecromancerBookOfDeadRegistryTest.java`
- `BookOfDeadDomainValidationTest.java`

Nowa dokumentacja:

- `docs/necromancer/book-of-the-dead-source-data.md`
- `docs/necromancer/n7-implementation-report.md`

Łącznie N7: **16 plików — 1 zmieniony, 15 nowych**. Dotychczasowe lokalne zmiany repo zachowano.

## 2. Finalny model domenowy

Własny pakiet `krys.necromancer.bookofthedead`, bez modeli skill-tree i bez ClassMechanicRegistry. Enums określają rodzinę, id z technicznym stringiem, theme, typ alternatywy i status `NOT_RUNTIME_ENABLED`. Immutable record specjalizacji przechowuje nazwy PL / EN, unlock, pasyw, fakty, trzy alternatywy, opcjonalny wymóg questu, sourceReference i runtimeStatus. Rodzina wynika jednoznacznie z id i jest dostępna przez `family()`.

`BookOfDeadChoiceDefinition` rozdziela zwykłe `effects` od Sacrifice z `playerBonuses` i `summonPenalty`; `SourceConflict` zawiera audyt rozstrzygnięcia źródeł. `BookOfDeadSourceFact` przechowuje niepusty id, opis, opcjonalną liczbę BigDecimal i jawną jednostkę. `BookOfDeadSummonPenalty` określa typ i dodatnią wielkość redukcji. Żaden model nie wykonuje efektów.

`NecromancerMinionRules` opisuje globalne 20% Lucky Hit, 100% użytecznych statystyk / buffów oraz warunek przywołania po Sacrifice. `NecromancerBookOfDeadRegistry.sourceCatalog()` zwraca kompletny katalog z `all()`, `byId()`, `byFamily(...)`, `find(...)` i `findByRawId(...)`. Konstruktor waliduje kompletność / duplikaty i porządkuje rodzinę, poziom odblokowania, source order. Wszystkie kolekcje są immutable, wejścia kopiowane; nie istnieje stan wyboru Book na bohaterze.

## 3–4. Liczebność i mapa specjalizacji

**3 rodziny / 9 specjalizacji / 27 choice definitions**. Każda specjalizacja: Passive / base ability + Upgrade #1 + Upgrade #2 + Sacrifice.

| Rodzina | Enum / techniczne id | PL / EN | Unlock | Theme | Dodatkowy wymóg |
| --- | --- | --- | --- | --- | --- |
| SKELETAL_WARRIOR | SKIRMISHER / `skirmisher` | Harcownicy / Skirmishers | 5 | BONE | — |
| SKELETAL_WARRIOR | DEFENDER / `defender` | Obrońcy / Defenders | 8 | BLOOD | — |
| SKELETAL_WARRIOR | REAPER / `reaper` | Żniwiarze / Reapers | 12 | DARKNESS | — |
| SKELETAL_MAGE | SHADOW_MAGE / `shadow_mage` | Magowie Cienia / Shadow Mages | 5 | DARKNESS | — |
| SKELETAL_MAGE | COLD_MAGE / `cold_mage` | Magowie Zimna / Cold Mages | 18 | DARKNESS | — |
| SKELETAL_MAGE | BONE_MAGE / `bone_mage` | Magowie Kości / Bone Mages | 22 | BONE | — |
| GOLEM | BONE_GOLEM / `bone_golem` | Golem Kościany / Bone Golem | 8 | BONE | — |
| GOLEM | BLOOD_GOLEM / `blood_golem` | Golem Krwawy / Blood Golem | 28 | BLOOD | Call of the Underworld |
| GOLEM | IRON_GOLEM / `iron_golem` | Golem Żelazny / Iron Golem | 32 | DARKNESS | Call of the Underworld |

## 5. Sacrifice po Lord of Hatred

Oddzielne playerBonuses oraz summonPenalty. Warriors / Mages: `SUMMON_COUNT_REDUCTION`, 50%. Golem: `GOLEM_DAMAGE_REDUCTION_MULTIPLICATIVE`, 50%. Wybranie Sacrifice nie koduje zakazu istnienia sług: zakupiony odpowiedni summon skill na pasku nadal pozwala je przywoływać. Brak flagi `minionDisabledBySacrifice`. Test sprawdza również brak booleanów wyłączających miniony w recordach. Kary i bonusy nie są wykonywane.

## 6. Efekty względne i source coefficients

Bone Mage Upgrade #1: dodatkowe pociski `COUNT=2`, każdy `75 / PERCENT_OF_NORMAL_DAMAGE`; bez interpretacji 75% Weapon Damage. Odłamki Obrońcy: `50 / PERCENT_OF_THORNS`. Corrupting 100%, Blizzard 200%, Bone Spikes 250% i shockwave 125%[x] to odrębne źródłowe współczynniki Book, poza tabelami skilli. Duration i warunki są osobnymi faktami. Nie sumujemy forków single target, nie zakładamy częstotliwości Blizzard, maksimum Blood Golema ani liczby wyssanych wrogów.

## 7. Bone Mage Fortify

Katalogowe `0.1 / PERCENT_OF_MAXIMUM_LIFE` i `OFFICIAL_SOURCE_RESOLVED`. Metadane zachowują Icy Veins display 3%, oficjalny Blizzard patch 2.5 0.1%, oba odnośniki i uzasadnienie. Wartość oficjalna ma pierwszeństwo; test golden i walidacja modelu pilnują zgodności, bez manual-review.

## 8. Defender Thorns

Brak absolutnej wartości zależnej od poziomu / kontekstu źródła. `SOURCE_DEPENDENT_AMOUNT`, `value=null`, opis `source-dependent Thorns amount`. Pewne splinter damage = 50% własnych Thorns. Test odrzuca próbę przypisania absolutnej liczby do jednostki source-dependent.

## 9. Walidacja

Testy Book obejmują golden metadata, kompletność, wszystkie liczby / jednostki, pasywy, warunki, odbiorców Barriery, trigger corpse / spikes, Sacrifice, konflikt źródeł, source-dependent Thorns i brak podłączenia produkcyjnej domeny do skill-tree / runtime. Testy konstruktorów obejmują kopie wejść, niezmienność kolekcji, duplikaty, brak alternatyw, błędny typ kary i sprzeczny konflikt źródłowy.

Nowe testy Book: **43 / 2 suite**, 0 failures / errors / skipped. Ukierunkowany `clean test` Book, katalogów Nekromanty, Paladyna, metadanych źródłowych i HTTP ranking: **229 testów / 30 suite**, 0 failures / errors / skipped, **BUILD SUCCESS**, 15.039 s, zakończenie 07.10.2026 16:02:43 CEST. Polecenie:

```powershell
mvn '-Dmaven.repo.local=.m2' clean test '-Dtest=BookOfDead*Test,Necromancer*Test,Paladin*Test,Neutral*Test,SkillResourceDataTest,SkillCharge*Test,CharacterTreeMetadataTest,DamagePercent*Test,CharacterSkillTreeRegistryTest'
```

Pełny `mvn '-Dmaven.repo.local=.m2' clean test`: **799 testów / 119 suite**, **0 failures / 0 errors / 0 skipped**, **BUILD SUCCESS**, 22 min 54 s. Zakończenie: **07.10.2026 16:26:56 CEST**. Sumy potwierdzono niezależnie z plików `target/surefire-reports/TEST-*.xml`. Obejmuje 43 nowe testy Book oraz wszystkie dotychczasowe regresje engine, manual simulation, current build, search, katalogów, HTTP, persistence i importu obrazów / OCR. Nie uruchamiano package ani assembly.

## 10–12. Zachowana granica N7

- Nekromanta: **23 skille / 161 upgrade'ów**, wszystkie wcześniejsze membership / liczby / tagi / notes bez zmian.
- Paladyn: **24 skille**, PaladinOathRegistry i jego modele bez zmian.
- HeroClass: wyłącznie **PALADIN**. SkillId: cztery legacy / test-only identyfikatory.
- HeroClassDefs, CurrentBuildSnapshotFactory, CurrentBuildRequest / FormData, HeroProfile i formaty persistence, DamageEngine, ManualSimulationService, DamageRankingService bez zmian N7.
- Current build pozostaje Paladyn-only. Book nie ma wyboru na bohaterze, UI, persistence, runtime execution, minion counts, stat inheritance engine ani minion DPS.

Kontrola SHA-256 względem świeżego snapshotu **519 plików** stanu po N6: spośród istniejących plików zmienił się wyłącznie README.md; **518 pozostałych plików bez zmian**, w tym wszystkie dotychczasowe pliki produkcyjne, testy i dokumenty źródłowe. Dodano 15 plików N7. Wszystkie **534 hashe** zamrożonego kodu / dokumentacji pozostały identyczne przez cały pełny test. Po teście uzupełniono wyłącznie ten raport o faktyczne wyniki. Porównanie dotyczy stanu zastanego, nie starszego HEAD Git. `git -c core.safecrlf=false diff --check` zakończył się poprawnie.

## 13. Dostawa

ZIP nie został utworzony. Nie uruchamiano package / assembly; końcowa kontrola potwierdziła 0 ZIP-ów w katalogu głównym repo i w target. Dostawa obejmuje kod, golden testy, README i dokumentację w repo. N7 jest zamknięte.
