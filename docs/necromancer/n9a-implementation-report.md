# N9A — neutralna tożsamość skilla runtime

## Zakres i pliki

N9A wprowadza wyłącznie neutralną identity i adapter czterech legacy skilli. N8B przechodzi ze Stringa na klucz; bieżący loadout, state, snapshot, trace, current build, search i persistence zachowują dotychczasowe modele i zachowanie.

Dodane:

- `src/main/java/krys/runtime/RuntimeSkillKey.java`
- `src/main/java/krys/runtime/legacy/LegacyPaladinRuntimeSkillKeyAdapter.java`
- `src/test/java/krys/runtime/RuntimeSkillKeyTest.java`
- `src/test/java/krys/runtime/RuntimeSkillKeyArchitectureTest.java`
- `src/test/java/krys/runtime/legacy/LegacyPaladinRuntimeSkillKeyAdapterTest.java`
- `src/test/java/krys/web/HeroSkillCatalogAdapterTest.java`
- `docs/necromancer/n9a-implementation-report.md`

Zmienione:

- `src/main/java/krys/necromancer/combat/NecromancerSeason15DirectDamageResolver.java` — typowany klucz i walidacja namespace/id.
- `src/main/java/krys/web/HeroSkillCatalogAdapter.java` — tylko import oraz prywatny lookup do katalogu.
- `src/main/java/krys/combat/DamageEngine.java` — tylko import oraz mapowanie w helperze kategorii Basic.
- `src/main/java/krys/simulation/ManualSimulationService.java` — tylko import oraz prywatny lookup do katalogu.
- `src/test/java/krys/necromancer/combat/NecromancerSeason15DirectDamageResolverTest.java` — migracja identity i rejection tests, bez zmiany goldenów.
- `src/test/java/krys/necromancer/combat/NecromancerSeason15DirectDamageArchitectureTest.java` — RuntimeSkillKey jako dodatkowa dozwolona zależność.
- `README.md` — aktualny kontrakt N8B, sekcja N9A, diagram i plan N9B/N9C.

Audyt zastanego kodu znalazł trzy switche SkillId → katalog, nie tylko wymieniony w zadaniu HeroSkillCatalogAdapter. W celu spełnienia kryterium jednego centralnego mapowania zastąpiono także switche w DamageEngine i ManualSimulationService. Zmiana jest ograniczona do lookup helperów: te same identyfikatory trafiają do tego samego PaladinSkillTreeRegistry, z tymi samymi metodami find/require i sprawdzaniem kategorii. Nie zmieniono matematyki, konfiguracji, zasobów, statusów, pętli symulacji ani modelu persistence.

## Finalny kontrakt RuntimeSkillKey

```java
RuntimeSkillKey(String classId, String skillId)
String asExternalId()
static RuntimeSkillKey parse(String externalId)
```

Record jest immutable; oba pola typu String. Każdy segment musi pasować do `[a-z0-9][a-z0-9_-]*`, bez normalizacji. Null jest odrzucany przez NullPointerException; blank, uppercase, spacje, znaki narodowe, znaki specjalne i niedozwolony pierwszy znak przez IllegalArgumentException. Nie ma zależności od klasy domenowej ani walidacji istnienia skilla w katalogu.

External format: `classId:skillId`. Parser akceptuje dokładnie jeden separator i dwa poprawne segmenty. `key.equals(RuntimeSkillKey.parse(key.asExternalId()))`. Equality i hashCode uwzględniają classId i skillId, także przy użyciu zbioru. Obecne persistence nie korzysta z tego formatu.

## Adapter legacy i katalog

| SkillId | Klucz | Istniejąca nazwa katalogowa |
|---|---|---|
| BRANDISH | paladin:wymach | Wymach |
| HOLY_BOLT | paladin:swiety_pocisk | Święty Pocisk |
| CLASH | paladin:starcie | Starcie |
| ADVANCE | paladin:natarcie | Natarcie |

`toRuntimeKey` zawiera jeden exhaustive switch. `toLegacySkillId` wyszukuje na podstawie tego samego mapowania, bez drugiego switcha. Dla czterech wartości zachodzi legacy → key → legacy, wszystkie klucze są różne. Nieznany klucz / inna klasa daje Optional.empty(); null w obu kierunkach jawny wyjątek. Produkcyjny adapter nie zna katalogu; zgodność czterech id i nazw sprawdzają wyłącznie testy.

## Migracja N8B

Przeszukanie `src/main/java` po nazwach resolvera i profilu wykazało wyłącznie ich definicje i użycie profilu we własnym resolverze. Brak produkcyjnego callera `calculate(String, int, profile)`; publiczny String overload został usunięty bez deprecated wrappera.

Aktualne API: `calculate(RuntimeSkillKey skillKey, int rank, profile)`. Wymaga dokładnie classId=`necromancer`, skillId=`krwotok`, rank=1. Zły namespace, inny poprawny id lub rank != 1 daje UnsupportedOperationException. Null key daje NullPointerException. Niepoprawna składnia identity jest odrzucana wcześniej przez RuntimeSkillKey.

Po walidacji obu części klucza lookup używa `skillKey.skillId()` w istniejącym NecromancerSkillTreeRegistry. Nie ma ogólnego registry runtime, SkillTreeRegistryProvider ani zależności od rankingu. Coefficient nadal pochodzi z katalogowego R1 80; ścieżka BigDecimal → DirectDamageComponentSpec nie używa intValue/longValue/longValueExact. Helper-level regresje 62.5 i 7.5 pozostają bez zmiany.

Profil Season 15, Intelligence scalar, buckets, postMitigation=1, EXACT_COMPONENT_SUM i PRE_TARGET_MITIGATION są bez zmian. Dotychczasowe goldeny: A 2100/3150, B 2520/3780, C 22932/68796; wszystkie oczekiwania zachowane. Rejection tests jawnie obejmują `paladin:krwotok`, `necromancer:wymach`, `necromancer:unknown` i Krwotok rank 2, a także wszystkie inne katalogowe skille oraz pozostałe nieobsługiwane rangi.

## Prezentacja i istniejący runtime

HeroSkillCatalogAdapter nadal przyjmuje SkillId i zwraca to samo HeroAssignedSkillPresentation. Zmieniony prywatny helper: legacy adapter → `PaladinSkillTreeRegistry.findSkill(key.skillId())`. Pozostałe metody tego adaptera, prezentacje, formularze i renderery nie zostały zmienione.

Przed testami wykonano sondę oryginalnego HeroSkillCatalogAdapter, z zachowaniem kopii kodu sprzed N9A w tymczasowym target. Wyniki tego samego katalogu dla R1: Wymach 75, Święty Pocisk 90, Starcie 115, Natarcie 105. Nowa regresja chroni te liczby, nazwy, ten sam obiekt tree skill, generowanie Wiary, efekty bazowe i wybory Starcia. Sonda potwierdziła: Starcie legacy LEFT + ANIMUS → Kara/Animusz; tree choices → Kara/Animusz/Zwiększenie Obrażeń, bez duplikacji; rank 0 → brak aktywnych modyfikatorów i efektów bazowych. Kod sondy jest usuwany przez Maven clean i nie stanowi dostarczonego modułu.

**RuntimeSkillKey jest neutralną identity, SkillState nadal jest legacy Paladin state.** Żadne pola SkillId ani EnumMap nie zostały migrowane. Nie zmieniono HeroAssignedSkill/Loadout, CurrentBuildFormData/Request, HeroBuildSnapshot, trace, BuildSearch modeli, FileHeroProfileRepository ani formatu pliku hero profiles. Legacy mapowanie w engine/symulacji deleguje do adaptera, bez zmiany zachowania. Kernel i matematyka Paladyna pozostają bez zmian.

Plan: N9A identity jest wdrożone; N9B neutralny assigned-skill/configuration model, N9C neutralny loadout/snapshot bridge oraz późniejszy current build Nekromanty nie są wdrożone.

## Architektura i zachowane granice

Testy kontrolują: RuntimeSkillKey zna tylko JDK; legacy adapter tylko JDK, RuntimeSkillKey i SkillId; N8B dopuszcza dodatkowo neutralny klucz; publiczny resolver ma jeden calculate i typowany pierwszy argument; jeden produkcyjny właściciel mapowania do katalogu. Kontrola źródeł obejmuje importy oraz pełne kwalifikowane odwołania, a dotychczasowa kontrola decimal safety nadal działa.

HeroClass tylko PALADIN; SkillId nadal BRANDISH / HOLY_BOLT / CLASH / ADVANCE. Katalogi bez zmian: Necromancer 23/161, Book 3/9/27, Paladin 24. Statusy i safety danych bez zmian. Nekromanta nie jest podłączony do current build; nie rozszerzono wykonania poza bazowy Krwotok R1 ani nie wykonuje się upgrade'ów / Book. ZIP nie jest tworzony.

## Walidacja

Testy celowane: **477 testów / 64 suite, 0 failures / 0 errors / 0 skipped, BUILD SUCCESS**, 24.039 s. Zakończenie: 08.10.2026 09:07:48 CEST. Sumy potwierdzono niezależnie z XML Surefire. Polecenie:

```powershell
mvn '-Dmaven.repo.local=.m2' clean test '-Dtest=RuntimeSkillKey*Test,LegacyPaladinRuntimeSkillKeyAdapterTest,HeroSkillCatalogAdapterTest,DirectDamage*Test,*DamageEngine*Test,ReactiveDamageFoundationTest,*ManualSimulation*Test,HeroBuildSnapshotTest,*CurrentBuild*Test,BuildSearch*Test,SearchBuild*Test,Necromancer*Test,BookOfDead*Test,Paladin*Test,CharacterSkillTreeRegistryTest,Neutral*Test,FileHeroProfileRepositoryTest'
```

Nowe suite: RuntimeSkillKey 23, legacy adapter 7, architektura N9A 5, prezentacja HeroSkillCatalogAdapter 7 — łącznie **42 nowe testy**. N8B resolver zachowuje wcześniejsze 23 przypadki i dodaje 4 rejection cases (27); architektura N8B nadal 2. Wszystkie przechodzą, w tym bijekcja, zgodność czterech mapowań z katalogiem, decimal regressions i trzy dotychczasowe goldeny N8B. Goldeny DamageEngine/Reactive (19), manual simulation/snapshot (31) i search (15) przechodzą bez zmiany oczekiwań. Testy current build / HTTP i FileHeroProfileRepository również przechodzą.

Świeża migawka sprzed N9A obejmuje **546 istniejących plików**. Audyt: **539 bez zmian, 7 zmienionych, 7 dodanych**, zgodnie z listą powyżej. Pięć plików neutralnego kernelu, profil N8B, katalogi, modele loadoutu/current build/search/snapshot/trace, renderery i persistence mają identyczne SHA-256. Dla DamageEngine i ManualSimulationService odwrócenie w pamięci wyłącznie dodanego importu i centralizacji helpera odtwarza dokładny SHA-256 sprzed N9A; pozostała treść tych plików jest identyczna.

Pełny `mvn '-Dmaven.repo.local=.m2' clean test`: **893 testy / 127 suite, 0 failures / 0 errors / 0 skipped, BUILD SUCCESS**, 23 min 32 s. Zakończenie: **08.10.2026 09:33:18 CEST**. Sumy potwierdzono niezależnie z `target/surefire-reports/TEST-*.xml`. Wynik obejmuje dotychczasowy zakres 847 testów po migracji identity oraz 46 dodatkowych przypadków N9A (42 w czterech nowych suite i 4 rejection cases N8B).

Przeszły wszystkie architektury: N9A 5, N8B 2, neutralny kernel 2. Legacy adapter ma wszystkie 7 testów bez błędów, w tym cztery exact mappings, round-trip i zgodność z katalogiem. N8B resolver 27 oraz HeroSkillCatalogAdapter 7 również przechodzą. Goldeny N8B i Paladyna, current build/HTML, search, simulation, persistence oraz obrazy/OCR zachowują wyniki. Potwierdzone katalogi: Necromancer 23/161, Book 3/9/27, Paladin 24; HeroClass tylko PALADIN, SkillId dokładnie cztery legacy wartości.

W czasie pełnego uruchomienia wszystkie **553 zamrożone pliki** zachowały identyczne SHA-256. Po sukcesie dopisano wynik wyłącznie w tym raporcie; kod i README odpowiadają dokładnie przetestowanej wersji. Końcowy zakres to 7 zmienionych i 7 nowych plików. `git -c core.safecrlf=false diff --check` przechodzi. Root i target zawierają 0 ZIP-ów; ZIP nie został utworzony.
