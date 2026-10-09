# N11 — pierwszy grywalny Current Build Nekromanty

**Status: CLOSED.** Pełny `mvn clean test` zakończony bez failures/errors/skips. Kod i README pozostawały zamrożone podczas przebiegu; po nim uzupełniono wyłącznie ten raport.

## Zakres

Użytkownik może utworzyć/wybrać Nekromantę, wybrać Krwotok R1, założyć broń i itemy z biblioteki, podać Intelligence spoza aktywnych itemów, oznaczyć cel Vulnerable, obliczyć normalny/krytyczny hit oraz zapisać i odzyskać konfigurację po restarcie. To direct hit single target przed redukcją celu, Season 15 / live 3.2.2, bez DPS ani pełnej symulacji.

| Zakres produktu | Stan |
|---|---|
| FULL PROFILE SUPPORT | Tak |
| FIRST DIRECT-HIT CURRENT BUILD | Tak |
| FULL NECROMANCER RUNTIME | Nie |

## Pliki względem początku N11

Lista wynika ze świeżego porównania SHA-256 po N10, obejmującego 581 plików src/docs oraz README/AGENTS/pom. Repo miało wcześniejsze niezacommitowane zmiany; porównanie do HEAD nie wyznacza zakresu N11.

Dodano 16 plików:

- `src/main/java/krys/necromancer/app/NecromancerCurrentBuildCalculation.java`
- `src/main/java/krys/necromancer/app/NecromancerCurrentBuildCalculationService.java`
- `src/main/java/krys/web/CurrentBuildRuntimeMode.java`
- `src/main/java/krys/web/CurrentBuildWeaponDamageResolver.java`
- `src/main/java/krys/web/NecromancerCurrentBuildFormData.java`
- `src/main/java/krys/web/NecromancerCurrentBuildFormQuerySupport.java`
- `src/main/java/krys/web/NecromancerCurrentBuildInputResolver.java`
- `src/main/java/krys/web/NecromancerCurrentBuildLoadoutEditor.java`
- `src/main/java/krys/web/NecromancerCurrentBuildPageRenderer.java`
- `src/test/java/krys/necromancer/app/NecromancerCurrentBuildArchitectureTest.java`
- `src/test/java/krys/necromancer/app/NecromancerCurrentBuildCalculationServiceTest.java`
- `src/test/java/krys/web/CurrentBuildWeaponDamageResolverTest.java`
- `src/test/java/krys/web/NecromancerCurrentBuildFormTest.java`
- `src/test/java/krys/web/NecromancerCurrentBuildInputResolverTest.java`
- `src/test/java/krys/web/NecromancerCurrentBuildWebFlowTest.java`
- `docs/necromancer/n11-implementation-report.md`

Zmieniono 9 plików:

- `README.md`
- `src/main/java/krys/web/CurrentBuildController.java`
- `src/main/java/krys/web/CurrentBuildPageModel.java`
- `src/main/java/krys/web/CurrentBuildPageRenderer.java`
- `src/main/java/krys/web/CurrentBuildRuntimeInputResolver.java`
- `src/main/java/krys/web/CurrentBuildRuntimeSupport.java`
- `src/main/java/krys/web/HeroProfile.java`
- `src/main/java/krys/web/HeroService.java`
- `src/test/java/krys/web/NecromancerProfileWebFlowTest.java`

Zmieniona regresja N10 zachowuje odrzucenie legacy mutacji, inline przełączenie obu klas, unsupported Search i brak dereferencji null mapperów/usług Paladyna. Oczekuje teraz dedykowanego UI N11 zamiast historycznej zapowiedzi N11. Nie zmieniono żadnego oczekiwanego wyniku obrażeń Paladyna.

## Service, loadout i wejścia

`NecromancerCurrentBuildCalculationService.calculate(RuntimeSkillLoadout, NecromancerSeason15DirectDamageProfile)` sprawdza classId i pobiera istniejący node `necromancer:krwotok` z mapy. Wymaga obecności na action bar; komunikat dla konfiguracji poza paskiem to „Krwotok musi znajdować się na pasku akcji.”. Przekazuje dokładnie tę konfigurację do N8B, który odrzuca rank różny od 1 i niepuste selections. Nie wykonuje innych node'ów. Wynik `NecromancerCurrentBuildCalculation(configuration, profile, result)` nie korzysta z typów request/snapshot/simulation Paladyna.

Editor dodaje Krwotok R1 empty i klucz paska wyłącznie przy braku jego konfiguracji. Istniejących selections/rang/paska nie naprawia. Usunięcie usuwa tylko Krwotok. Pozostałe konfiguracje i kolejność ich action bara przetrwają calculate/save. Persisted `grupa_1=szybkosc_uzycia` wywołuje jawny błąd, nie wynik bazowego hitu; UI pokazuje ostrzeżenie, a zapisany node pozostaje nienaruszony.

`ItemLibraryService.resolveEffectiveCurrentBuild` dostaje zerową ręczną bazę i wybór slotów bohatera. Wkład INT pochodzi z rzeczywistego mappera biblioteki po Masterworkingu. Neutralny wspólny helper broni wybiera dodatnie averageWeaponDamage, potem dodatni contribution.weaponDamage; brak obu źródeł powoduje błąd. Paladyn używa tego samego helpera z zachowanym priorytetem i wynikami. N11 nie dodaje legacy INT, nie wylicza klasy z poziomu i nie używa DPS/min/max we wzorze.

```text
itemIntelligence = activeItemsContribution.intelligence
effectiveIntelligence = nonItemIntelligence + itemIntelligence
profile = NecromancerSeason15DirectDamageProfile(
    resolvedAverageWeaponDamage, effectiveIntelligence,
    0, 0, 1, 0, 0, 0, 0, targetVulnerable)
```

Sześć niewspieranych procentowych buckets to 0, separate multiplier to 1. Vulnerable jest jawną flagą celu i daje context multiplier 1/1.2. Formularz dopuszcza pustą/nieujemną skończoną non-item INT, boolean celu i blank/NONE/krwotok; rank/selections/SkillId/pola Paladyna są odrzucane. Query nadpisuje tylko dwa pola Necro i zachowuje wszystkie pozostałe; selectedSkill nie jest dodatkowym źródłem prawdy. Neutralne API profilu i HeroService wymagają zgodności klasy i zapisują kanoniczny loadout. Format runtime-v1 i legacy readers pozostają bez zmian.

Calculate jest transient. Globalne wybory itemów mają skutek dopiero przy save po walidacji wszystkich slotów; niepoprawny zapis nie mutuje żadnego slotu. SlotAction natychmiast ustawia/czyści item zgodnie z dotychczasowym flow, bez zapisu kontekstu skilla/INT/celu. Save może zapisać stan bez skilla/broni i nie wymaga kalkulacji. Wybrany persisted Krwotok musi spełniać R1/empty/on-bar. Inline zmiana bohatera korzysta z istniejącej akcji; po niej następuje właściwy renderer klasy.

## SSR i wyniki HTTP

Strona zawiera kontekst aktywnego bohatera i przełącznik, nagłówek „N11 — bazowy direct hit Krwotoku R1”, zakres PRE_TARGET_MITIGATION, katalogowy coefficient 80%, wybór Krwotoku R1, pole „Inteligencja poza aktywnymi itemami”, checkbox Vulnerable/Odsłonięty i wspólny paper-doll ekwipunku. Nie ma pól Przysięgi, Animuszu, Wiary, legacy assigned skills ani edytowalnej rangi/upgrades. GET nie liczy. Wynik jest osobną sekcją z normal/critical hit, coefficient z `spec.damagePercent`, średnimi obrażeniami broni, trzema wartościami INT i mnożnikami z result.context. Help jawnie wymienia wszystkie niewspierane mechaniki.

| HTTP E2E | Wynik |
|---|---|
| A: persisted Krwotok R1, aktywna broń average 1000, non-item INT 1000 | 200; 2100 / 3150 |
| B: ten sam build, Vulnerable | 200; 2520 / 3780; ×1.2 |
| C: broń 1000, non-item INT 900, imported affix +100 effective INT | 200; INT 1000; 2100 / 3150 |
| Masterworking | Wkład odczytany z rzeczywistego mappera, różny od source 100; non-item dopełnia INT do 1000; 2100 / 3150 |
| Brak broni | 200; „Wybierz aktywną broń z poprawnymi średnimi obrażeniami.”; brak calculation |
| Brak skilla | 200; „Wybierz Krwotok R1.”; brak calculation |
| Persisted upgrade | 200; upgrade'y niewykonywane w N11; brak calculation, selections bez zmian |
| Persisted rank 0 / konfiguracja poza paskiem / rank 2 | 200; jawne odrzucenie, brak silent repair |
| SAVE + nowy serwer/repository/service w tym samym katalogu | R1 empty, Krwotok na bar, INT 900, Vulnerable true, aktywna broń i zapisany SSR odtworzone |
| Transient calculate / błędny save | Brak mutacji query, loadoutu i globalnych wyborów itemów |
| Inne konfiguracje Necro | Zachowane przy dodaniu/usunięciu Krwotoku; nie są wykonywane |
| Pozostałe affixy, także Critical Strike Chance | Nie zmieniają golden direct hitu |

## Walidacja

Zestaw celowany zakończył się 09.10.2026 o 08:59:51 CEST: **440 testów, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS, 18.227 s. Obejmuje 32 nowe testy N11 oraz stare regresje Current Build Paladyna (59), Search (5), N8B resolvera (28), neutralnego kernelu, modeli i adapterów runtime, request/snapshot, legalnego WAIT, profilu/persistence i katalogów.

**Pełny clean test: BUILD SUCCESS.** Polecenie `mvn '-Dmaven.repo.local=.m2' clean test` korzysta z lokalnego cache Maven, bez selektora testów lub pomijania przypadków. Zakończone 09.10.2026 o **09:28:50 CEST**, czas **23:03 min**. Wynik konsoli i niezależny audyt wszystkich XML Surefire są zgodne: **1072 testy w 149 klasach, 0 failures, 0 errors, 0 skipped**. Nowe testy N11: 32 w 6 klasach; istniejące 1040 testów zachowane.

**git diff --check: PASS, exit 0**, bez błędów whitespace. Komunikaty Git o przyszłej konwersji LF/CRLF nie są błędami tego sprawdzenia. Dodatkowo skontrolowano whitespace w dodanych plikach.

**SHA-256: PASS.** Wszystkie **597 plików** zamrożone przed clean test były identyczne po jego zakończeniu. Względem początku N11: **16 dodanych, 9 zmienionych, 572 istniejące bez zmian, 0 usuniętych**. Osobna lista 59 chronionych plików również pozostała bez zmian, w tym DamageEngine, ManualSimulationService, pięć plików kernelu, N8B resolver/profile, modele/adaptery runtime, Search i legacy definicje/baseline. Katalogi, Book, codec, reader persistence, request/snapshot i wcześniejsze raporty są niezmienione. Po aktualizacji raportu ponownie porównano hash wszystkich plików: różni się wyłącznie ten raport. Nie utworzono ZIP-a; skan repo poza cache zależności nie wykazał plików .zip.

## Kontrola 33 wymagań raportu

| Nr | Potwierdzenie |
|---:|---|
| 1 | Pełna lista 16 dodanych i 9 zmienionych plików powyżej |
| 2 | Finalna usługa: `krys.necromancer.app.NecromancerCurrentBuildCalculationService` |
| 3 | Konfiguracja Krwotoku pobrana z mapy kanonicznego RuntimeSkillLoadout |
| 4 | Wymagane classId necromancer, R1, empty selections i action bar; bez napraw |
| 5 | Wspólny helper average >0 → contribution >0 → błąd N11 |
| 6 | Efektywna item INT z existing ItemLibrary mappera i Masterworkingu |
| 7 | Jawna finite/nonnegative non-item INT; brak zgadywanego baseline |
| 8 | Effective INT = non-item + active item contribution, bez legacy INT |
| 9 | Profil liczbowy opisany powyżej |
| 10 | Zerowe niewspierane procentowe buckets; separate =1 |
| 11 | Target Vulnerable false/true daje ×1/×1.2 z context N8B |
| 12 | Golden A przez HTTP: 2100/3150 |
| 13 | Golden B przez HTTP: 2520/3780 |
| 14 | Golden C przez mapper: 900+100=1000 INT, 2100/3150 |
| 15 | Brak broni: HTTP 200, jawny błąd i brak wyniku |
| 16 | Brak skilla: HTTP 200, jawny błąd i brak wyniku |
| 17 | Persisted upgrade odrzucony; selections nie są usuwane |
| 18 | SAVE/restart plikowego profilu i serwera: stan odtworzony |
| 19 | Opis rzeczywistego SSR i wyników powyżej; osobny renderer Necro, wspólny ekwipunek |
| 20 | GET/POST Search i POST szczegółów nadal unsupported Necro, HTTP 200 |
| 21 | Stare 59 testów Current Build Paladyna przechodzi bez zmian expected damage |
| 22 | Stare 5 testów Search Paladyna przechodzi |
| 23 | N8B: 2100/3150, 2520/3780, 22932/68796, decimal 62.5/7.5 bez zmian |
| 24 | Architektura N11 wymusza wyłącznie neutralne modele, N8B, DirectDamageResult i JDK; stare testy architektury przechodzą |
| 25 | Pełny mvn clean test: BUILD SUCCESS, 1072 testy / 149 klas, 0 failures/errors/skips, 23:03 min |
| 26 | git diff --check: PASS, exit 0; nowe pliki również bez błędów whitespace |
| 27 | Necromancer 23 skille / 161 upgrade'ów |
| 28 | Book 3 rodziny / 9 specjalizacji / 27 wyborów |
| 29 | Paladin 24 skille katalogu |
| 30 | SkillId: tylko BRANDISH, HOLY_BOLT, CLASH, ADVANCE |
| 31 | DamageEngine, ManualSimulationService, pięć plików kernelu i N8B resolver/profile byte-for-byte bez zmian względem początku N11; audyt SHA-256 PASS |
| 32 | Brak HeroClassDef i HeroClassStatBaseline Nekromanty |
| 33 | ZIP nie jest tworzony; używamy test, bez package |

Nie wykonujemy upgrade'ów, Book, Essence, Lucky Hit, Blood Orb, Overpower, pozostałych skilli, expected crit, target mitigation ani DPS. Nie rozszerzamy symulacji lub Search, nie dodajemy SkillId.KRWOTOK ani rang 2..14/R15 runtime.
