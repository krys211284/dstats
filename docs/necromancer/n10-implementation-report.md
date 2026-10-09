# N10 — profil Nekromanty, UI i persistence

**Status: CLOSED.** Pełny clean test zakończony bez failures/errors/skips; kod i README pozostawały zamrożone podczas przebiegu. Po testach uzupełniono wyłącznie ten raport.

## Zakres i granica wykonania

Nekromanta jest klasą profilu aplikacji: użytkownik może utworzyć postać, zobaczyć ją na liście, aktywować, zmienić nazwę/poziom, usunąć i odzyskać zapisany profil po restarcie. Current Build oraz search Nekromanty zwracają HTTP 200 z komunikatem braku obsługi. N11 będzie pierwszym end-to-end Current Build Krwotoku. Standalone N8B nie jest podłączony do UI.

## Pliki N10

Lista dotyczy zmian względem świeżego stanu repo po N9C, z uwzględnieniem istniejących wcześniej niezacommitowanych zmian. Nie jest porównaniem całego drzewa do HEAD.

Dodane pliki:

- `src/main/java/krys/web/CurrentBuildRuntimeSupport.java`
- `src/main/java/krys/web/LegacyPaladinHeroSkillLoadoutAdapter.java`
- `src/main/java/krys/web/RuntimeSkillLoadoutCodec.java`
- `src/main/java/krys/web/UnsupportedHeroRuntimePageRenderer.java`
- `src/test/java/krys/hero/HeroClassMetadataTest.java`
- `src/test/java/krys/web/FileHeroProfileRuntimePersistenceTest.java`
- `src/test/java/krys/web/HeroProfileRuntimeSkillLoadoutTest.java`
- `src/test/java/krys/web/HeroServiceNecromancerTest.java`
- `src/test/java/krys/web/LegacyPaladinHeroSkillLoadoutAdapterTest.java`
- `src/test/java/krys/web/NecromancerProfileWebFlowTest.java`
- `src/test/java/krys/web/RuntimeSkillLoadoutCodecTest.java`
- `docs/necromancer/n10-implementation-report.md`

Zmienione pliki:

- `README.md`
- `src/main/java/krys/hero/HeroClass.java`
- `src/main/java/krys/web/CurrentBuildController.java`
- `src/main/java/krys/web/CurrentBuildPageModel.java`
- `src/main/java/krys/web/CurrentBuildPageRenderer.java`
- `src/main/java/krys/web/CurrentBuildRuntimeInputPresentation.java`
- `src/main/java/krys/web/FileHeroProfileRepository.java`
- `src/main/java/krys/web/HeroesController.java`
- `src/main/java/krys/web/HeroesPageRenderer.java`
- `src/main/java/krys/web/HeroProfile.java`
- `src/main/java/krys/web/HeroService.java`
- `src/main/java/krys/web/SearchBuildController.java`
- `src/main/java/krys/web/SearchBuildDetailsController.java`
- `src/main/java/krys/web/SearchBuildDetailsPageRenderer.java`
- `src/main/java/krys/web/SearchBuildPageRenderer.java`
- `src/main/resources/templates/heroes.html`
- `src/test/java/krys/combat/DirectDamageKernelArchitectureTest.java`
- `src/test/java/krys/necromancer/NecromancerSkillTreeRegistryTest.java`
- `src/test/java/krys/necromancer/bookofthedead/NecromancerBookOfDeadRegistryTest.java`
- `src/test/java/krys/necromancer/combat/NecromancerSeason15DirectDamageArchitectureTest.java`
- `src/test/java/krys/runtime/RuntimeSkillKeyArchitectureTest.java`
- `src/test/java/krys/web/FileHeroProfileRepositoryTest.java`

Pięć istniejących regresji enumu uwzględnia teraz autoryzowaną klasę profilu NECROMANCER; oczekiwane dane katalogów i cztery SkillId pozostają bez zmian.

## Model i adapter

`HeroClass` zawiera dokładnie `PALADIN("paladin", "Paladyn")` i `NECROMANCER("necromancer", "Nekromanta")`. API: `getTechnicalId()` i `getDisplayName()`. Generic renderery używają metadata enumu zamiast pobierać HeroClassDef tylko dla nazwy.

HeroClassDefs nadal posiada wyłącznie PALADIN. Brak definicji NECROMANCER jest jawny i celowy: legacy mainStatDivisor=800 oraz obsługa Intelligence nie stanowią modelu Season 15 Nekromanty. Nie dodano fikcyjnego divisor ani baseline; `HeroClassStatBaselines.find(NECROMANCER, 70)` pozostaje empty. Istniejące dane Paladyna są niezmienione.

HeroProfile ma dokładnie sześć pól: heroId, name, heroClass, currentBuildQuery, itemSelection i canonical RuntimeSkillLoadout. Wymaga zgodności classId z technicalId klasy; mismatch w obu kierunkach jest odrzucany bez naprawy. Konstruktor canonical nie konwertuje do legacy. Stare konstruktory delegują do adaptera wyłącznie dla Paladyna. Compatibility `getSkillLoadout()` odtwarza widok Paladyna i rzuca UnsupportedOperationException dla Nekromanty.

LegacyPaladinHeroSkillLoadoutAdapter przenosi osiem pól HeroAssignedSkill do SkillState i deleguje do istniejących adapterów N9C/N9B/N9A. Nie ma drugiego switcha tożsamości. Powrót zachowuje wszystkie pola, rank 0 poza paskiem i kolejność akcji. Round-trip zabezpiecza wszystkie cztery legacy skille oraz niezależne fakty CLASH: ANIMUS + generowanie_wiary, LEFT + potyczka, grupa 2 zwiekszenie_obrazen. HeroAssignedSkill i wnętrze HeroSkillLoadout nie zostały zmienione.

Paladyn jest tworzony z tego samego `HeroSkillLoadout.fromCurrentBuildFormData(default form)` co wcześniej. Aktualny kod default form zawiera ADVANCE R5 z base/RIGHT na pasku; nie został zmieniony do R0. Osobna regresja chroni legalny przypisany ADVANCE R0 poza paskiem. Nowy Nekromanta ma classId=necromancer, empty configurations/actionBar i item selection, bez automatycznie przypisanego skilla. Poziom obu klas ma zakres 1..70; pierwszy bohater jest aktywny.

Edycja nazwy/poziomu zachowuje klasę, canonical loadout, item selection i wszystkie pozostałe pola query, włącznie z polem spoza formularza w regresji. Kopie immutable profilu zachowują ten sam obiekt loadoutu; odczyt przez repozytorium zachowuje equals. updateActiveHeroLevel działa dla obu klas. Cztery legacy metody HeroService (add/remove SkillId, saveActiveHeroState i updateActiveHeroBuildFormData) jawnie odrzucają Nekromantę.

CurrentBuildFormData pozostaje przejściowym typem Paladyna. Profil Nekromanty odczytuje tylko bazowy query dla poziomu, bez applyToFormData ani getSkillLoadout. N11 wprowadzi osobną ścieżkę Current Build.

## Persistence i migracja

Nowy zapis ma nadal siedem pól HERO. Skill payload w polu 7 jest kodowany Base64 URL bez paddingu; po dekodowaniu:

```text
runtime-v1~classId~configurations~actionBar
configuration: classId:skillId@rank@slot=option,slot=option
configurations: wpisy rozdzielone ;
actionBar: external ids rozdzielone , w kolejności użytkownika
pusty Nekromanta: runtime-v1~necromancer~~
```

Konfiguracje są sortowane po external id, selections po slot id. Zachowane są rangi (również 0), brak wpisu dla braku wyboru, wszystkie selections i kolejność paska. Codec jest odpowiedzialnością web/persistence, bez serializacji w RuntimeSkillLoadout, bez Map.toString/record.toString. Każdy nowy zapis obu klas jest neutralny.

Stary rekord sześciopolowy Paladyna odtwarza loadout z currentBuildQuery przez istniejący parser formularza i adapter. Zachowuje jego historyczną interpretację grup CLASH. Stary rekord siedmiopolowy czyta legacy wpisy 4/5/8-polowe. Osiem pól zachowuje również niezależne wartości modifier/choice i grup. Reader deleguje do strict adaptera N9C przed ewentualnym widokiem HeroSkillLoadout, więc nie czyści błędnego action bara. Ponowny save migruje rekordy do runtime-v1.

Historyczne rekordy są wyłącznie Paladin-only. Niepoprawne wpisy, duplikaty konfiguracji/slotów, nieznane wersje, mismatch klasy, brak konfiguracji skilla paska i rank 0 na pasku są odrzucane jawnym błędem danych. Nie ma pomijania malformed entries ani silent repair.

Regresje persistence sprawdzają canonical i compatibility Paladyna, empty Nekromantę z item selection, techniczny Krwotok R1 z paskiem i wariant z selections pobranymi z katalogu. Przechowanie danych technicznych nie oznacza wykonania skilla. Test restartu tworzy nowy FileHeroProfileRepository/HeroService na tym samym katalogu; test HTTP dodatkowo restartuje całą instancję serwera.

## Granica web i UI

/bohaterowie generuje wybór obu klas z HeroClass.values i metadata. Tworzenie Mortis NECROMANCER level 70, lista, aktywacja, edycja, restart i usuwanie mają regresję HTTP. Klasa nie jest polem edycji; dodatkowy ręcznie przesłany heroClass jest ignorowany. Niepoprawna klasa daje czytelny błąd strony HTTP 200.

CurrentBuildRuntimeSupport dopuszcza runtime wyłącznie PALADIN. CurrentBuildPageModel nie odtwarza legacy loadoutu dla unsupported klasy: assigned/eligible IDs empty, budget null. Renderer pokazuje profil i komunikat „Current Build Nekromanty zostanie podłączony w N11”, bez aktywnego formularza Paladyna, Przysięgi, Animuszu i legacy skill editor. GET, POST calculate/save i próby przypisania SkillId nie mutują query, loadoutu ani item selection. Inline switch działa Paladyn → Nekromanta i odwrotnie.

GET/POST /znajdz-najlepszy-build i POST /znajdz-najlepszy-build/szczegoly dla Nekromanty zwracają HTTP 200 z komunikatem „Search dla Nekromanty nie jest jeszcze obsługiwany”. Nie odczytują legacy skilli i nie wykonują mapperów ani obliczeń. Regresja uruchamia te same kontrolery z nieobecnymi mapperami/usługami obliczeń i persisted Krwotokiem; poprawne odpowiedzi potwierdzają, że granica kończy przepływ przed ich wywołaniem. Paladyn zachowuje istniejące Current Build/search i oczekiwane obrażenia.

## Walidacja

Ostatni przebieg celowany: **318 testów, Failures 0, Errors 0, Skipped 0**, BUILD SUCCESS, 12.847 s; zakończony 8 października 2026 o 14:56:51 CEST. Obejmuje wszystkie nowe regresje, existing HeroService/persistence/Current Build/search, N9C i N8B oraz katalogi/Book i kernel architecture.

Pełny `mvn '-Dmaven.repo.local=.m2' clean test`: **1040 testów / 143 zestawy, Failures 0, Errors 0, Skipped 0**, BUILD SUCCESS, 23:35 min; zakończony 8 października 2026 o **15:21:41 CEST**. Sumy niezależnie potwierdzono ze wszystkich raportów XML Surefire po clean. Zestawy nowych regresji N10: 50 przypadków w 7 klasach testowych. Existing Paladin Current Build: 59 testów, Search: 5, HeroService: 2, persistence: 1 — wszystkie poprawne. Pięć zestawów N9C (loadout, legacy loadout adapter, request, snapshot, architecture): 54 przypadki; dodatkowo ManualSimulationTickRuntimeTest: 6 i CurrentBuildSnapshotFactoryTest: 1 — wszystkie poprawne. N8B resolver: 28 przypadków oraz architecture: 2 — wszystkie poprawne.

`git diff --check`: exit 0, bez błędów whitespace. Git wypisał wyłącznie istniejące ostrzeżenia konwersji LF/CRLF; kontrola `git -c core.safecrlf=false diff --check` również exit 0, bez outputu.

## Potwierdzenia regresji i zakresu

CurrentBuildRequest i HeroBuildSnapshot nadal przechowują canonical RuntimeSkillLoadout, factory przenosi ten sam payload, a invarianty N9C nie zostały zmienione. Historyczna naprawiona regresja WAIT pozostała bez zmian w N10.

N8B nadal przyjmuje RuntimeSkillConfiguration + NecromancerSeason15DirectDamageProfile i wykonuje wyłącznie necromancer:krwotok R1 z empty selections. Goldeny pozostają 2100/3150, 2520/3780, 22932/68796, a decimal 62.5/7.5. N10 nie zmienia współczynników ani formuł.

Katalog Nekromanty: 23 skills / 161 upgrades. Book: 3 families / 9 specializations / 27 choices. Paladyn: 24 skills. SkillId nadal wyłącznie BRANDISH, HOLY_BOLT, CLASH, ADVANCE. HeroClass.NECROMANCER jest tożsamością profilu, nie rozszerzeniem enumu skilli legacy.

Kontrola SHA-256 względem świeżego stanu N9C: **569 plików bazowych → 581 końcowych; 22 zmienione, 12 dodanych, 547 niezmienionych, 0 usuniętych**. Wszystkie 581 plików zamrożonych przed pełnym clean zachowały identyczne SHA podczas przebiegu. Zakres chroniony obejmuje DamageEngine, ManualSimulationService, pięć plików kernelu, resolver/profile N8B, modele i adaptery N9 oraz request/factory/snapshot N9C, HeroClassDefs/baselines, HeroAssignedSkill/HeroSkillLoadout, katalogi i dane Book — wszystkie byte-for-byte bez zmian względem N9C. W kodzie produkcyjnym zmieniły się wyłącznie HeroClass i wskazane pliki web. Nie dodano Book selection do profilu, Esencji, Lucky Hit, Blood Orb, wykonania upgrade'ów ani nowych Necro SkillId. Nie uruchomiono package ani nie utworzono ZIP-a; końcowa kontrola całego workspace wykazała **0 plików ZIP**.

## Raport odbiorczy — 32 punkty

| Nr | Wymaganie | Wynik |
| --- | --- | --- |
| 1 | Lista plików | 22 zmienione + 12 dodanych; pełna lista powyżej, względem świeżego N9C. |
| 2 | HeroClass i metadata | PALADIN/paladin/Paladyn, NECROMANCER/necromancer/Nekromanta; test metadata poprawny. |
| 3 | HeroClassDefs | Nadal wyłącznie PALADIN; pobranie NECROMANCER jawnie odrzucane. |
| 4 | Canonical HeroProfile | Sześć pól, jeden RuntimeSkillLoadout; legacy getter jest widokiem. |
| 5 | Class/loadout invariant | Dokładna równość technicalId i classId; oba kierunki mismatchu odrzucane. |
| 6 | Legacy adapter | HeroAssignedSkill → SkillState → istniejące adaptery N9; bez nowego switcha identity. |
| 7 | Lossless Paladin bridge | Osiem pól czterech skilli, rank 0 poza paskiem, kolejność i oba niezależne edge cases CLASH zachowane. |
| 8 | Default Paladin | Ten sam fromCurrentBuildFormData(default form), bez zmiany istniejącego defaultu; ADVANCE R0 osobno chroniony. |
| 9 | Empty Necromancer | classId necromancer, empty configurations/actionBar/item selection, poziom 1..70. |
| 10 | Nowy zapis | Deterministyczny runtime-v1 w Base64 pola 7; konfiguracje/sloty sortowane, kolejność paska zachowana. |
| 11 | Legacy 6-field | Odczyt PALADIN z query przez dotychczasowy formularz, potem bridge; save migruje do runtime-v1. |
| 12 | Legacy 7-field | Odczyt PALADIN wpisów 4/5/8-polowych; save migruje, malformed entries odrzucane. |
| 13 | Paladin persistence | Canonical i compatibility round-trip poprawne; wszystkie pola CLASH i ranga 0 zachowane. |
| 14 | Necro empty persistence | Klasa, nazwa, poziom, item selection i pusty loadout zachowane; legacy getter unsupported. |
| 15 | Necro technical persistence | Krwotok R1 z empty selections i paskiem zachowany dokładnie; dodatkowo selections z katalogu. |
| 16 | Restart | Nowe repo/service oraz restart HTTP serwera odzyskują ten sam profil i aktywne id. |
| 17 | Create HTTP | Mortis, NECROMANCER, 70 → HTTP 200, poprawny profil i label. |
| 18 | Edit name/level | HTTP i service poprawne; klasa immutable, loadout/items i pozostałe query zachowane. |
| 19 | Current Build Necro | GET/POST 200 + komunikat N11, bez formularza Paladyna, mappera, calculation i mutacji. |
| 20 | Search Necro | GET/POST i POST szczegółów 200 + unsupported; bez legacy skills i usług obliczeń. |
| 21 | Brak fake runtime Necro | Brak HeroClassDef, divisor i baseline; find(NECROMANCER,70) empty. |
| 22 | Existing Paladin web | 59 testów Current Build i 5 Search poprawnych; oczekiwane damage values bez zmian. |
| 23 | N9C | 54 przypadki loadout/adapter/request/snapshot/architecture poprawne; modele/transport byte-for-byte bez zmian. |
| 24 | N8B | Goldeny 2100/3150, 2520/3780, 22932/68796 i decimal 62.5/7.5 bez zmian; 28 resolver + 2 architecture poprawne. |
| 25 | Full clean test | 1040 / 143 zestawy, failures 0, errors 0, skips 0; BUILD SUCCESS. |
| 26 | Diff check | git diff --check exit 0; kontrola bez ostrzeżeń CRLF także exit 0. |
| 27 | Necro catalog | 23 skills / 161 upgrades; dane i statusy bez zmian. |
| 28 | Book | 3 families / 9 specializations / 27 choices; bez wykonania i selekcji w profilu. |
| 29 | Paladin catalog | 24 skills; dane bez zmian. |
| 30 | SkillId | Nadal tylko BRANDISH, HOLY_BOLT, CLASH, ADVANCE. |
| 31 | Engine/simulation/kernel | DamageEngine, ManualSimulationService i pięć plików kernelu mają identyczne SHA-256 jak w N9C. |
| 32 | ZIP | Nie utworzono archiwum; po clean test w workspace jest 0 ZIP-ów. |
