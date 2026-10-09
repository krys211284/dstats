# N9B — neutralna konfiguracja pojedynczego skilla runtime

## Zakres i pliki

Dodano neutralny value object, validator katalogowy Nekromanty i bezstratny bridge do istniejącego SkillState. N8B przyjmuje konfigurację, ale wykonuje nadal tylko bazowy Krwotok R1 z pustymi selections. Loadout, snapshot, current build, search, UI i persistence pozostają legacy.

Dodane pliki:

- `src/main/java/krys/runtime/RuntimeSkillConfiguration.java`
- `src/main/java/krys/runtime/legacy/LegacyPaladinRuntimeSkillConfigurationAdapter.java`
- `src/main/java/krys/necromancer/runtime/NecromancerRuntimeSkillConfigurationValidator.java`
- `src/test/java/krys/runtime/RuntimeSkillConfigurationTest.java`
- `src/test/java/krys/runtime/RuntimeSkillConfigurationArchitectureTest.java`
- `src/test/java/krys/runtime/legacy/LegacyPaladinRuntimeSkillConfigurationAdapterTest.java`
- `src/test/java/krys/necromancer/runtime/NecromancerRuntimeSkillConfigurationValidatorTest.java`
- `docs/necromancer/n9b-implementation-report.md`

Zmienione pliki:

- `src/main/java/krys/necromancer/combat/NecromancerSeason15DirectDamageResolver.java` — configuration API i jawne odrzucanie niepustych selections.
- `src/test/java/krys/necromancer/combat/NecromancerSeason15DirectDamageResolverTest.java` — migracja inputu oraz rejection selections, bez zmiany damage goldenów.
- `src/test/java/krys/necromancer/combat/NecromancerSeason15DirectDamageArchitectureTest.java` — configuration jako dozwolona zależność.
- `src/test/java/krys/runtime/RuntimeSkillKeyArchitectureTest.java` — aktualny publiczny kontrakt resolvera: dwa argumenty, pierwszy configuration.
- `README.md` — sekcja 5.8, diagram, aktualne API N8B oraz plan N9C/N10/N11.

## Finalny model i invarianty

```java
record RuntimeSkillConfiguration(RuntimeSkillKey skillKey, int rank, Map<String, String> selections)
boolean isLearned()
Optional<String> selectedOption(String selectionSlotId)
```

Key/map non-null; rank>=0 bez neutralnego górnego limitu; rank 0 wymaga pustych selections. Map.copyOf tworzy defensive immutable copy i odrzuca null key/value. Slot/option muszą pasować do `[a-z0-9][a-z0-9_-]*`, bez trimowania i zmiany wielkości liter. Null powoduje NullPointerException, pozostałe błędy invariantów IllegalArgumentException. isLearned oznacza rank>0. Lookup waliduje składnię slotu i zwraca Optional.empty dla nieobecnego wpisu.

Model nie zna klas, katalogu, rank budget ani znaczenia wyborów. Neutralny brak wyboru to brak wpisu; nie ma sentinel NONE. Equality/hashCode obejmują key, rank i mapę, niezależnie od kolejności wpisów. Test dopuszcza Integer.MAX_VALUE jako rank, dowodząc braku limitu klasy w neutralnym modelu. Nie ma serialization/parse/asExternalId/JSON/query/persistence codec konfiguracji.

## Validator katalogowy Nekromanty

`NecromancerRuntimeSkillConfigurationValidator.validate` wymaga classId necromancer, istniejącego skillId z NecromancerSkillTreeRegistry, rank 0..DamagePercentRankTable.MAX_RANK i zgodności każdego slotu/option z dokładną grupą danego skilla. Nie tworzy ogólnego registry i nie przenosi CharacterSkillTreeRegistry z rankingu.

Legalne: zero, częściowe lub wszystkie grupy, w tym sama grupa_2=oslabienie. Nie ma polityki poprzednich grup ani skill points. Nie wymaga znanej tabeli coefficient dla aktualnego rank; rangi 2..14 są strukturalnie dopuszczalne, ale nie są przez to damage verified.

Krwotok exact: grupa_1=szybkosc_uzycia, grupa_2=przytloczenie, grupa_3=krwawy_wrzod. Cross-group grupa_1=przytloczenie, grupa_4, nieznany option, reserved legacy slot, inny namespace, nieznany skill oraz rank 16 są odrzucane.

Test iteruje po wszystkich **23** skillach. Dla każdego config rank 1 bez selections przechodzi; config z pierwszym upgradem każdej istniejącej grupy również przechodzi. To wyłącznie reprezentacja struktury, bez wykonywania ulepszeń.

## Reserved legacy slots i mapowanie

Prywatne stałe pozostają w LegacyPaladinRuntimeSkillConfigurationAdapter:

| Slot | Wartości obecnego bridge'a |
|---|---|
| legacy_base_upgrade | enabled |
| legacy_choice_upgrade | left / middle / right |
| legacy_runtime_modifier | animus |
| grupa_1 / grupa_2 / grupa_3 | Dokładna techniczna wartość odpowiedniego pola SkillState |

Forward: skillId → LegacyPaladinRuntimeSkillKeyAdapter → skillKey; rank bez zmiany; true base flag → enabled; enumy inne niż NONE → name.toLowerCase(Locale.ROOT); grupy inne niż NO_TREE_CHOICE → dokładna wartość pola. Nie usuwa się pozornej redundancji ani nie interpretuje CLASH.

Reverse: null odrzucany jawnie; nieznany legacy key → Optional.empty. Dla znanego klucza dozwolone jest tylko sześć slotów z tabeli. Nieznane sloty, niepoprawne reserved values i explicit none zamiast braku wpisu powodują IllegalArgumentException. Brak wpisów daje base=false, choice/runtime NONE i grupy NO_TREE_CHOICE. Powstaje istniejący SkillState; jego walidacja rozstrzyga rank 0..5 i dozwolone kombinacje. Błędy nie są zamieniane na Optional.empty ani fallback NONE.

Adapter nie zna katalogów. Techniczne niestandardowe opcje grupowe, które dopuszcza istniejący SkillState, pozostają zachowane. Jeśli stare pole grupowe ma niepoprawną składnię techniczną (stary konstruktor dopuszcza również swobodny tekst), forward jawnie odrzuca je przez invarianty nowego modelu — nie normalizuje i nie gubi danych.

## Lossless round-trip i Starcie

Helper porównuje wszystkie osiem pól: skillId, rank, baseUpgrade, choiceUpgrade, runtimeModifierChoice, choiceGroup1, choiceGroup2, choiceGroup3.

Reprezentatywne przypadki: BRANDISH R1 bez upgrade'ów, BRANDISH R5/base/LEFT, HOLY_BOLT R5/base/MIDDLE, ADVANCE learned, CLASH R5/base z grupami animusz/zwiekszenie_obrazen/kara oraz wszystkie cztery rank 0 bez selections. Dodatkowo reverse chroni left/middle/right, domyślne wartości oraz błędy istniejącego konstruktora.

Ważny edge case jest legalny w zastanym konstruktorze: CLASH R5/base, runtime ANIMUS, grupa_1=generowanie_wiary, choice LEFT, grupa_3=potyczka. Forward zachowuje równocześnie legacy_runtime_modifier=animus i grupa_1=generowanie_wiary oraz legacy_choice_upgrade=left i grupa_3=potyczka. Reverse odtwarza wszystkie pola bez uproszczenia.

## Migracja i granica N8B

Przeszukanie `src/main/java` po resolverze/profilu potwierdziło brak produkcyjnych callerów starego calculate(RuntimeSkillKey, int, profile). Poprzedni publiczny overload usunięto; aktualne API to calculate(RuntimeSkillConfiguration, profile).

Resolver wyciąga configuration.skillKey oraz rank, wymaga necromancer:krwotok, rank 1 i EMPTY selections. Null configuration daje jawny wyjątek; każdy inny konstruowalny input UnsupportedOperationException. Ujemna ranga nie daje się skonstruować i jest odrzucana wcześniej przez neutralny model; odpowiedni test przeniesiono do invariantów, bez zmiany damage expected values.

**Catalog-valid != runtime-supported.** Testy jawnie potwierdzają, że Kościane Odłamki oraz Krwotok z jedną lub wszystkimi trzema legalnymi grupami przechodzą catalog validator i są odrzucane przez resolver. Jakiekolwiek niepuste selections są odrzucane, także nieznane sloty, bez ignorowania i fallbacku do bazowych obrażeń.

Configuration.skillKey().skillId() trafia po sprawdzeniu granicy do tego samego NecromancerSkillTreeRegistry / CharacterTreeSkill / DamagePercentRankTable. R1 80 nadal pochodzi z katalogu. BigDecimal → DirectDamageComponentSpec bez intValue/longValue/longValueExact; helper regressions 62.5 i 7.5 pozostają. Nie zmieniono profilu, Intelligence, buckets, postMitigation=1 ani EXACT_COMPONENT_SUM.

Goldeny N8B pozostają A **2100/3150**, B **2520/3780**, C **22932/68796**; zmienia się wyłącznie input konfiguracji. PRE_TARGET_MITIGATION, brak zasobów, Lucky Hit, upgrade'ów i Book pozostają granicą wykonania.

## Architektura i zachowane granice

Nowe architektury: configuration tylko RuntimeSkillKey/JDK; legacy bridge tylko configuration/identity adapter/SkillState/enums/JDK; validator tylko configuration, istniejący registry Nekromanty, neutralne skilltree types/JDK. Testy skanują importy i kwalifikowane odwołania, sprawdzają brak rank policy i legacy slots w modelu oraz prywatność reserved slots adaptera. Dotychczasowe architektury identity, N8B i kernelu pozostają wymagane.

Nie zmieniono SkillState.java, HeroAssignedSkill, HeroSkillLoadout, CurrentBuildFormData/Mapper/Request/SnapshotFactory, HeroBuildSnapshot, ManualSimulationService, BuildSearch, FileHeroProfileRepository, formatu profilu, UI, DamageEngine ani pięciu plików kernelu. Centralizacja N9A zostaje. Nie dodano klasy ani enum values.

Katalogi bez zmian: Necromancer 23/161, Book 3/9/27, Paladin 24. Krwotok nadal R1 80, max-view 204, Lucky Hit 50, generowanie Esencji 13, siedem upgrade'ów i dokładne 2+2+3 membership. HeroClass tylko PALADIN; SkillId BRANDISH / HOLY_BOLT / CLASH / ADVANCE. ZIP nie jest tworzony.

Plan: N9A identity CLOSED, N9B single-skill configuration wdrożone; N9C loadout/snapshot bridge, N10 HeroClass.NECROMANCER + profil/UI i N11 pierwszy end-to-end Current Build Nekromanty pozostają niewdrożone.

## Walidacja

Testy celowane uruchomiono przez `mvn '-Dmaven.repo.local=.m2' clean test` z wyborem nowych typów, architektur oraz regresji N8B, kernelu, Paladyna, katalogów, current build, simulation, search i persistence. Wynik: **520 testów / 68 suites, failures=0, errors=0, skipped=0**, BUILD SUCCESS, 22.022 s; zakończenie 08.10.2026 o 11:10:58 CEST.

Nowe suites: RuntimeSkillConfigurationTest 8, LegacyPaladinRuntimeSkillConfigurationAdapterTest 21, NecromancerRuntimeSkillConfigurationValidatorTest 10, RuntimeSkillConfigurationArchitectureTest 3. N8B resolver 28 i jego architektura 2 pozostają zielone; damage goldeny/formula regression Paladyna 18, manual simulation 30 i search 20 również przechodzą bez zmiany expected values.

Świeża migawka SHA-256 sprzed N9B obejmowała 553 pliki README/AGENTS/pom/src/docs. Audyt zakresu potwierdza tylko pięć zmienionych istniejących plików i osiem dodanych wymienionych wyżej; 548 istniejących plików pozostaje bez zmian. Nie usunięto plików. `git -c core.safecrlf=false diff --check` przechodzi.

Pełny `mvn '-Dmaven.repo.local=.m2' clean test`: **936 testów / 131 suites, failures=0, errors=0, skipped=0**, BUILD SUCCESS, 22 min 57 s; zakończenie 08.10.2026 o 11:37:09 CEST. Sumy potwierdzono niezależnie w 131 raportach XML Surefire. Ukończono również pełne regresje importu obrazów, OCR i web.

Wszystkie **561 plików** zamrożonych przed pełnym przebiegiem zachowały identyczne SHA-256 przez całe uruchomienie; nie dodano ani nie usunięto plików w tym czasie. Po testach uzupełniono wyłącznie niniejszy raport wynikiem. Porównanie do świeżej migawki sprzed N9B nadal potwierdza pięć zmienionych i osiem dodanych plików oraz 548 niezmienionych plików, w tym wskazane modele legacy, simulation, search, persistence, DamageEngine i kernel. Katalogi oraz istniejące expected damage values pozostają bez zmian. Końcowe `git -c core.safecrlf=false diff --check` przechodzi; ZIP w katalogu głównym i target: **0**.
