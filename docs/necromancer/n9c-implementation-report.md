# N9C — neutralny loadout w transporcie request/snapshot

## Zakres i pliki

Neutralny jest payload skilli przenoszony przez CurrentBuildRequest → CurrentBuildSnapshotFactory → HeroBuildSnapshot. Webowy loadout, simulation, damage, search, CLI i persistence zachowują legacy API. Pozostałe pola requestu/snapshotu nadal opisują Paladyna; N9C nie uruchamia Current Build Nekromanty.

Dodano:

- `src/main/java/krys/runtime/RuntimeSkillLoadout.java`
- `src/main/java/krys/runtime/legacy/LegacyPaladinRuntimeSkillLoadoutAdapter.java`
- `src/test/java/krys/runtime/RuntimeSkillLoadoutTest.java`
- `src/test/java/krys/runtime/RuntimeSkillLoadoutArchitectureTest.java`
- `src/test/java/krys/runtime/legacy/LegacyPaladinRuntimeSkillLoadoutAdapterTest.java`
- `src/test/java/krys/app/CurrentBuildRequestRuntimeSkillLoadoutTest.java`
- `src/test/java/krys/simulation/HeroBuildSnapshotRuntimeSkillLoadoutTest.java`
- `docs/necromancer/n9c-implementation-report.md`

Zmieniono:

- `src/main/java/krys/app/CurrentBuildRequest.java` — jedno kanoniczne pole, compatibility getters i prywatna kopia dla with-methods.
- `src/main/java/krys/simulation/HeroBuildSnapshot.java` — jedno kanoniczne pole, jeden publiczny neutralny constructor i compatibility getters.
- `src/main/java/krys/app/CurrentBuildSnapshotFactory.java` — transport loadoutu bez map/list legacy.
- `src/test/java/krys/simulation/ManualSimulationTickRuntimeTest.java` — legalny fixture regresji WAIT, zachowujący scenariusz bez rzutu i obrażeń.
- `README.md` — aktualny kontrakt transportu, sekcja 5.9 i diagram oraz plan N10/N11.

## Finalny kontrakt loadoutu

```java
record RuntimeSkillLoadout(
    String classId,
    Map<RuntimeSkillKey, RuntimeSkillConfiguration> configurations,
    List<RuntimeSkillKey> actionBar
)
Optional<RuntimeSkillConfiguration> configuration(RuntimeSkillKey key)
boolean contains(RuntimeSkillKey key)
boolean isOnActionBar(RuntimeSkillKey key)
```

ClassId jest osobnym polem także dla pustego loadoutu. Wymaga non-null i `[a-z0-9][a-z0-9_-]*`, bez normalizacji. Map.copyOf/List.copyOf zapewniają defensive immutable copy; map/list i ich wpisy nie mogą być null. Klucz mapy musi być równy configuration.skillKey(); klucze mapy i paska muszą należeć do classId loadoutu. Nie ma katalogowego lookupu ani polityki klasy.

Mapa może zawierać rank 0. Pasek zachowuje kolejność, odrzuca duplikaty i wymaga istniejącej nauczonej konfiguracji każdego klucza. Nie usuwa błędnych wpisów i nie naprawia mismatchu. **Brak limitu 6** potwierdza test siedmiu nauczonych keys na pasku. Testy obejmują pusty Paladin/Necromancer, rank 1 Krwotoku, rank 0 poza paskiem, błędne klasy, map key/config key, nulls, mutacje i kolejność. Equality/hashCode obejmują trzy pola, z kolejnością listy. Lookup null key jest jawnie odrzucany. Nie ma parsera/serialization/persistence codec.

## Legacy loadout adapter i round-trip

```java
RuntimeSkillLoadout toRuntimeLoadout(Map<SkillId, SkillState> skills, List<SkillId> actionBar)
Optional<LegacyView> toLegacyView(RuntimeSkillLoadout loadout)
record LegacyView(Map<SkillId, SkillState> learnedSkills, List<SkillId> actionBar)
```

Forward ustawia classId=paladin, sprawdza map key == state.skillId, deleguje konfigurację do adaptera N9B i klucze paska do adaptera N9A. Przeszukanie src/main/java i src/test/java pod kątem konstruktorów, map oraz wpisów SkillState nie wykazało legalnego mismatchu. Nie dodano alternatywnego mapowania SkillId → techniczne id.

Null map/list → empty oraz LinkedHashSet z kolejnością pierwszego wystąpienia zachowują istniejącą semantykę legacy przed konstrukcją ścisłego loadoutu. Neutralny model sam odrzuca duplikaty. Null wpisy, brak konfiguracji i rank 0 na pasku są odrzucane. Rank 0 poza paskiem pozostaje zachowany.

Reverse: non-null loadout; classId inne niż paladin → Optional.empty. Dla paladin każda konfiguracja musi dać się odtworzyć przez adapter N9B, a każdy klucz paska przez N9A. Paladin:unknown lub błędne selections powodują IllegalArgumentException, bez pomijania danych. Immutable LegacyView defensywnie kopiuje EnumMap i listę, zachowując kolejność enumeracji starej mapy i kolejność paska.

Golden round-trip obejmuje BRANDISH R5/base/LEFT, HOLY_BOLT R5/base/MIDDLE, ADVANCE R1 i CLASH R5/base z niezależnymi runtime ANIMUS + grupa_1=generowanie_wiary oraz choice LEFT + grupa_3=potyczka. Porównuje wszystkie osiem pól: skillId, rank, baseUpgrade, choiceUpgrade, runtimeModifierChoice, choiceGroup1/2/3. Kolejność po deduplikacji: ADVANCE, CLASH, HOLY_BOLT, BRANDISH. Wszystkie fakty CLASH pozostają jednocześnie zachowane. Obowiązują invarianty technicznych id konfiguracji N9B; adapter nie normalizuje grupowych id.

## Request: kanoniczne przechowywanie i zgodność

CurrentBuildRequest przechowuje jedno final RuntimeSkillLoadout, bez pól learnedSkills/actionBar jako drugiego źródła prawdy. Zachowano wszystkie **siedem publicznych konstruktorów** z mapą/listą legacy. Dotychczasowa walidacja statów, resource/Animus, kryta i learned action bar poprzedza adaptację. Pozostałe defaulty i deduplikacja aspektów pozostają bez zmian.

GetRuntimeSkillLoadout zwraca kanoniczny immutable obiekt. GetLearnedSkills/getActionBar odtwarzają widok przez adapter. Wyniki zachowują pola i porządek, a nie identity obiektów SkillState, które są odtwarzane. Nie dodano publicznego request API przyjmującego loadout.

WithActiveAspectIds i withCriticalChancePercent używają prywatnego copy constructora: zachowują **ten sam obiekt loadoutu**, bez round-tripu legacy. Test assertSame oraz porównanie wszystkich pozostałych niezmienianych pól chronią selections/rank/order i pozostałe dane requestu. Zachowano walidację kryta oraz null/dedupe aspektów.

## Snapshot i neutralny transport factory

HeroBuildSnapshot również ma tylko jedno final RuntimeSkillLoadout. Zachowano **sześć starych publicznych konstruktorów**, null map/list/equipment → empty, default kryta zależny od hero, null hero → crit 0, resource/Animus/oath defaults i LinkedHashSet dedupe. Dodano dokładnie **jeden publiczny canonical constructor** z RuntimeSkillLoadout i pozostałymi polami. Wymaga non-null loadoutu, zachowuje jego identity i semantykę innych pól; walidacja kryta pozostaje taka sama.

GetLearnedSkills/getSelectedSkillBar/getSkillState(SkillId) używają widoku adaptera. Dla Paladyna wyniki ośmiu pól są identyczne; nieobecny skill oraz null SkillId zwracają null jak wcześniej. Dla payloadu innej klasy legacy gettery jawnie rzucają UnsupportedOperationException. To nie dodaje obsługi nowej klasy w simulation.

Factory przekazuje **request.getRuntimeSkillLoadout()** do nowego konstruktora snapshotu, bez getLearnedSkills/getActionBar, bez adaptera reverse i bez powrotu przez SkillId. Test assertSame obejmuje trzy istniejące reference requests oraz CLASH z niezależnymi wyborami. Test źródła factory zabezpiecza wyłącznie neutralny transport skilli.

Neutralny payload nie oznacza w pełni neutralnego snapshotu: hero, Przysięga, Animusz i pozostałe pola Paladyna nadal istnieją. Nie dodano class-aware requestu ani połączenia Nekromanty z current build UI.

## Architektura i zachowane granice

Loadout zna tylko RuntimeSkillKey/RuntimeSkillConfiguration/JDK. Legacy adapter zna tylko runtime models, legacy state/adapters i JDK. Testy skanują importy i kwalifikowane odwołania, sprawdzają brak limitu UI, typ record components, brak parsera, jedno kanoniczne pole w request/snapshot, publiczne konstruktory i neutralny transport factory.

ManualSimulationService korzysta nadal z snapshot compatibility getters; plik pozostaje bez zmian. DamageEngine, pięć plików kernelu, N8B resolver/profile/testy i wzory obrażeń pozostają bez zmian. Nie zmieniono HeroAssignedSkill, HeroSkillLoadout, HeroProfile, HeroService, formularzy, renderers, FileHeroProfileRepository, formatu persistence, BuildSearch, CLI ani danych katalogu.

N8B nadal obsługuje pojedynczą RuntimeSkillConfiguration i bazowy Krwotok R1 bez selections. Goldeny pozostają **2100/3150**, **2520/3780**, **22932/68796**, a decimal safety **62.5/7.5**. Katalogi pozostają **Necro 23/161**, **Book 3/9/27**, **Paladyn 24**. HeroClass tylko PALADIN, SkillId nadal BRANDISH/HOLY_BOLT/CLASH/ADVANCE. Nie wykonujemy upgrade'ów, Book ani Esencji. **ZIP nie jest tworzony.**

README zawiera ścieżkę legacy web/form → request/loadout → factory → snapshot/loadout → legacy view → simulation oraz oznaczoną jako przyszłą ścieżkę N10/N11 UI Nekromanty → loadout necromancer → class-aware Current Build. N9A/N9B CLOSED, N9C wdraża bridge, N10/N11 pozostają niewdrożone.

## Walidacja

Historyczny ManualSimulationTickRuntimeTest.powinien_wykonac_wait_gdy_brak_legalnego_skilla_do_rzutu zawierał malformed legacy fixture: pusta mapa skilli i HOLY_BOLT na pasku. Zgodnie z rozstrzygnięciem użytkownika zachowano ścisłe invarianty N9C i cel regresji ManualSimulationService. Fixture zastąpiono semantycznie równoważnym legalnym wejściem: HOLY_BOLT R1 bez upgrade'ów istnieje w mapie i pozostaje na pasku; aktywny istniejący verathiel_shard przy Wiara=0 i regeneracji=0 blokuje rzut przez wszystkie trzy ticki. Zachowano oczekiwane WAIT, total damage 0, brak direct-hit debugów i trzy kroki trace. Zmienia się wyłącznie niezbędny opis konfiguracji w trace: rank 1 i legalActive=true, ale hasRequiredResource/isLegalCandidate/selected=false. Test nadal wykonuje symulację i sprawdza każdy tick; nie został zastąpiony testem walidacji. ManualSimulationService.java nie zmieniono.

Osobne regresje HeroBuildSnapshotRuntimeSkillLoadoutTest na trzech granicach (neutralny loadout, legacy adapter, publiczny legacy constructor snapshotu) potwierdzają jawne IllegalArgumentException dla HOLY_BOLT na pasku bez konfiguracji oraz dla istniejącego HOLY_BOLT rank 0 na pasku. Nie usuwamy skilla z paska, nie tworzymy automatycznego stanu i nie podnosimy rangi; test chroni także niezmienione wejścia.

Końcowe testy celowane uruchomiono przez `mvn '-Dmaven.repo.local=.m2' clean test` z wyborem nowych typów, architektur oraz regresji current build, wszystkich manual simulation, search, persistence, kernelu, N8B i katalogów. Wynik: **574 testy / 73 suites, failures=0, errors=0, skipped=0**, BUILD SUCCESS, 26.874 s; zakończenie 08.10.2026 o 13:35:10 CEST. Nie wyłączono ManualSimulationTickRuntimeTest ani jego regresji WAIT.

Nowe suites: RuntimeSkillLoadoutTest 11, LegacyPaladinRuntimeSkillLoadoutAdapterTest 9, CurrentBuildRequestRuntimeSkillLoadoutTest 16, HeroBuildSnapshotRuntimeSkillLoadoutTest 13, RuntimeSkillLoadoutArchitectureTest 5 — **54 nowe testy**, wszystkie zielone. ManualSimulationTickRuntimeTest 6 oraz pozostałe 24 manual simulation przechodzą. Istniejące CurrentBuildCalculationServiceTest 20, CurrentBuildSnapshotFactoryTest 1 i pozostałe regresje request/CLI/web pozostają zielone bez zmiany damage expected values. N8B resolver 28, N8B architecture 2 i wszystkie dotychczasowe damage goldeny również przechodzą.

Świeża migawka sprzed N9C obejmuje 561 plików README/AGENTS/pom/src/docs. Audyt SHA-256 potwierdził osiem dodanych i pięć zmienionych plików wymienionych wyżej oraz **556 niezmienionych plików**, bez usunięć. DamageEngine, ManualSimulationService, N8B, web, CLI, search i persistence zachowują identyczne hashe. `git -c core.safecrlf=false diff --check` przechodzi.

Pełny `mvn '-Dmaven.repo.local=.m2' clean test`: **990 testów / 136 suites, failures=0, errors=0, skipped=0**, BUILD SUCCESS, **23 min 12 s**; zakończenie 08.10.2026 o **13:59:23 CEST**. Sumy potwierdzono niezależnie w 136 raportach XML Surefire. Wszystkie 30 regresji manual simulation, poprawiony test WAIT, nowe invarianty, current build, search, web, persistence, N8B, katalogi i pełny import obrazów/OCR przechodzą.

Wszystkie **569 plików** zamrożonych przed pełnym przebiegiem zachowały identyczne SHA-256 przez całe uruchomienie; nie dodano ani nie usunięto plików w tym czasie. Po testach uzupełniono wyłącznie niniejszy raport. Porównanie do świeżej migawki sprzed N9C potwierdza pięć zmienionych i osiem dodanych plików oraz 556 niezmienionych plików, w tym byte-for-byte ManualSimulationService, DamageEngine, kernel, N8B, web/profile/form, persistence, search i CLI. Zmiana istniejącego testu dotyczy wyłącznie fixture WAIT i koniecznego opisu jego nauczonego skilla w trace; damage goldeny pozostają bez zmian.

Końcowe `git diff --check` zakończyło się kodem 0, bez błędów whitespace; lokalny Git wypisał jedynie ostrzeżenia konwersji LF/CRLF. Dodatkowe `git -c core.safecrlf=false diff --check` przechodzi. ZIP w katalogu głównym i target: **0**. N9C spełnia kryteria zamknięcia; N10/N11 pozostają niewdrożone.
