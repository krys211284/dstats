# Zweryfikowany baseline nagiego Nekromanty poziomu 70

**Status: CLOSED.** Pełny clean test i audyt końcowy przeszły bez failures/errors/skips. Kod, testy i README pozostawały zamrożone przez cały przebieg; po nim uzupełniono wyłącznie ten raport. N0–N11 pozostają zamknięte; to zadanie odtwarza nagą postać użytkownika i nie dodaje następnych skilli.

## Źródło i snapshot acceptance

Źródłem jest snapshot i tooltipy dostarczone przez użytkownika bezpośrednio z gry, po zdjęciu wszystkich itemów i umiejętności. Nie zastępujemy tych wartości danymi z innych baz. Pełny `HeroClassStatBaseline` Nekromanty jest zweryfikowany wyłącznie dla poziomu 70.

| Statystyka | Wartość z gry / SSR |
|---|---|
| Klasa / poziom | Nekromanta / 70 |
| STR / INT / WILL / DEX | 76 / 79 / 77 / 76 |
| Wytrzymałość / Pancerz / Maksimum zdrowia | 1611 / 152 / 1526 |
| Odporność fizyczna | 32 |
| Ogień / błyskawice / zimno / trucizna / cień | 32 / 32 / 32 / 32 / 32 |
| Podstawowe obrażenia broni / Szybkość broni | 0 / 1,00 |
| Szansa na trafienie krytyczne / Obrażenia krytyczne | 5,2% / 50,0% |
| Obrażenia odsłoniętym celom / Ciernie | 20,0% / 0 |
| Pancerz z Siły / z itemów / z innych źródeł | 152 / 0 / 0 |
| Item contribution czterech core stats | 0 / 0 / 0 / 0 |

Test `NecromancerVerifiedBaselineWebFlowTest.nakedLevel70ShouldMatchAllVerifiedGameStatsWithoutManualIntelligence` tworzy bohatera przez HTTP, sprawdza pusty wybór itemów, pustą mapę konfiguracji i pusty pasek, następnie wykonuje GET Current Build. Odpowiedź 200 zawiera pełny snapshot, pole dodatkowej INT z wartością 0 i brak wyniku obrażeń. Sekcja `Statystyki bohatera` jest domyślnie rozwinięta. Polski separator dziesiętny odpowiada liczbowo dostarczonemu snapshotowi gry.

## Neutralna progresja i fakty core stats

`HeroCoreStats` i `HeroClassCoreStatProgression` nie zależą od runtime obrażeń. Rejestr `HeroClassCoreStatProgressions` przechowuje początkowe cztery statystyki obu klas i przyrost 1 na każdy kolejny poziom. Poziomy niedodatnie i przepełnienie są odrzucane.

| Klasa | Level 1: STR / INT / WILL / DEX | Level 2 | Level 70 |
|---|---|---|---|
| Nekromanta | 7 / 10 / 8 / 7 | 8 / 11 / 9 / 8 | 76 / 79 / 77 / 76 |
| Paladyn | 10 / 7 / 7 / 8 | 11 / 8 / 8 / 9 | 79 / 76 / 76 / 77 |

`HeroCoreStatDerivedFacts` zachowuje dokładne wartości `BigDecimal`. Scalar obrażeń skilli jest parametrem podawanym przez prezentację: dla Nekromanty 0.1625 punktu procentowego na INT. Procenty UI mają jedno miejsce, a odporności zero miejsc po przecinku, z rounding `HALF_UP`.

| Źródło level 70 | Fakt | Dokładnie | UI |
|---|---|---|---|
| STR 76 | Pancerz | 152 | 152 |
| INT 79 | Obrażenia umiejętności | 12.8375% | 12,8% |
| INT 79 | Wszystkie odporności | 31.6 | 32 |
| WILL 77 | Generowanie zasobu | 0.385% | 0,4% |
| WILL 77 | Otrzymywane leczenie | 2.695% | 2,7% |
| DEX 76 | Wkład do szansy krytycznej | 0.19% | 0,2% |
| DEX 76 | Szansa uniku | 0.456% | 0,5% |

Dokładne fakty i rounding mają osobną regresję. `HeroCriticalChanceBreakdown` używa `criticalChanceFromCoreStatPercent` oraz etykiety źródła; dla Nekromanty źródłem dodatkowych 0,2% jest Zręczność. Łączna szansa snapshotu to 5,2%. Paladyn zachowuje dotychczasowy total 5,2% i etykietę tooltipu, bez zmiany legacy damage calculation.

## Prezentacja i semantyka INT

`CurrentHeroStatsPresentation.from` korzysta z metadata klasy, neutralnej progresji, `HeroClassStatBaselines` i aktywnych wkładów itemów. Nie pobiera `HeroClassDef`. Obie klasy korzystają ze wspólnego renderera statystyk. Nekromanta otrzymuje cztery core stats, pełne kafelki snapshotu poziomu 70 oraz osobne fakty wpływu bazowych core stats.

Na innych poziomach core stats są znane, ale pełny baseline nie istnieje. Regresja HTTP poziomu 2 potwierdza 8/11/9/8, komunikat braku pełnego baseline'u i brak wyliczonej Wytrzymałości/Maksimum zdrowia. Nie ma interpolacji ani nowego wzoru tych wartości. Po założeniu itemu UI oznacza Wytrzymałość jako wartość snapshotu nagiej postaci, zamiast zgadywać jej zależność od itemu.

```text
effectiveIntelligence = classLevelBaselineIntelligence
                      + additionalNonItemIntelligence
                      + activeItemIntelligence
nagi Nekromanta 70: 79 + 0 + 0 = 79
```

Formularz używa `additionalNonItemIntelligence`, a query `necromancerAdditionalNonItemIntelligence`. Dodatkowa INT jest skończona i nieujemna; puste pole oznacza 0. Baseline nie jest ręcznym wejściem i nie jest dodawany ponownie przez legacy formularz Paladyna. Profil N8B otrzymuje jedynie efektywną INT i dotychczasowe wejścia, bez zmiany matematyki resolvera.

Odczyt starego query `necromancerNonItemIntelligence` lub historycznego POST `nonItemIntelligence` zachowuje dawną semantykę totalu spoza aktywnych itemów. Przeliczenie to `max(0, stara wartość − aktualny baseline INT)`.

| Stary zapis N11, level 70 | Nowa dodatkowa INT | Efektywna INT bez itemów |
|---|---|---|
| 1000 | 921 | 1000 |
| 900 | 821 | 900 |
| 79 | 0 | 79 |
| 0 | 0 | 79 |

Dla historycznych wartości niższych od baseline'u przyjęto dodatkową INT 0 i widoczne wyjaśnienie migracji. GET nie zmienia persisted query. Dopiero SAVE zapisuje nowe pole, usuwa stare i zachowuje pozostałe pola oraz kanoniczny loadout. Gdy zapisane query ma oba klucze, nowy ma pierwszeństwo; POST z oboma polami formularza jest odrzucany. Niepoprawne wartości liczbowe nadal podlegają walidacji, bez cichego podstawiania poprawnych danych do kalkulacji.

Regresje HTTP zabezpieczają odczyt starego totalu 1000 jako dodatku 921, brak mutacji GET, zachowanie nieznanego pola query, SAVE/restart i wyjaśnienie dla historycznego 0. Nowy formularz 921 z baseline'em 79 i bronią 1000 nadal daje golden 2100/3150. Golden z itemową INT 100 zachowuje total 1000 jako 79+821+100. Vulnerable nadal daje 2520/3780. Masterworking nadal korzysta z rzeczywistego wkładu istniejącego mappera.

## Pliki względem początku zadania

Świeży snapshot SHA-256 na początku obejmował 597 plików: src/docs oraz README/AGENTS/pom. Repo rozpoczynało ten etap z czystym statusem Git. Dodano 9 plików, zmieniono 15, nie usunięto żadnego.

Dodano:

- `src/main/java/krys/hero/HeroCoreStats.java`
- `src/main/java/krys/hero/HeroClassCoreStatProgression.java`
- `src/main/java/krys/hero/HeroClassCoreStatProgressions.java`
- `src/main/java/krys/hero/HeroCoreStatDerivedFacts.java`
- `src/test/java/krys/hero/HeroClassCoreStatProgressionsTest.java`
- `src/test/java/krys/hero/HeroCoreStatDerivedFactsTest.java`
- `src/test/java/krys/hero/HeroCoreStatArchitectureTest.java`
- `src/test/java/krys/web/NecromancerVerifiedBaselineWebFlowTest.java`
- `docs/necromancer/verified-level70-baseline-report.md`

Zmieniono:

- `README.md`
- `src/main/java/krys/hero/HeroClassStatBaselines.java`
- `src/main/java/krys/hero/HeroCriticalChanceBreakdown.java`
- `src/main/java/krys/web/CurrentHeroStatsPresentation.java`
- `src/main/java/krys/web/CurrentBuildController.java`
- `src/main/java/krys/web/CurrentBuildPageRenderer.java`
- `src/main/java/krys/web/NecromancerCurrentBuildFormData.java`
- `src/main/java/krys/web/NecromancerCurrentBuildFormQuerySupport.java`
- `src/main/java/krys/web/NecromancerCurrentBuildInputResolver.java`
- `src/main/java/krys/web/NecromancerCurrentBuildPageRenderer.java`
- `src/test/java/krys/hero/HeroClassMetadataTest.java`
- `src/test/java/krys/hero/HeroClassStatBaselinesTest.java`
- `src/test/java/krys/web/NecromancerCurrentBuildFormTest.java`
- `src/test/java/krys/web/NecromancerCurrentBuildInputResolverTest.java`
- `src/test/java/krys/web/NecromancerCurrentBuildWebFlowTest.java`

Zmiany starych testów INT dostosowują wejście lub nazwę pola do nowej semantyki. Oczekiwane obrażenia N11/N8B i Paladyna pozostają takie same. Stare HTTP POST non-item INT pozostawiono w regresjach jako sprawdzenie kompatybilności. Nowe testy to 11 przypadków: 10 w czterech nowych klasach i jeden dodatkowy w istniejącym teście baseline'ów.

## Walidacja

Zestaw celowany zakończył się 09.10.2026 o 12:42:49 CEST: **181 testów, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS, 19.422 s. Obejmuje progresję/derived facts/architekturę, pełny snapshot HTTP, nowe i stare zapisy INT, N11, N8B, kernel, profil Nekromanty oraz 59 testów Current Build i 5 testów Search Paladyna.

Polecenie zestawu celowanego:

```powershell
mvn '-Dmaven.repo.local=.m2' '-Dtest=HeroClass*Test,HeroCore*Test,NecromancerVerifiedBaselineWebFlowTest,NecromancerCurrentBuild*Test,NecromancerProfileWebFlowTest,CurrentBuildWebServerTest,CurrentBuildRuntimeInputResolverTest,SearchBuildWebServerTest,NecromancerSeason15*Test,DirectDamage*Test' test
```

**Pełny clean test: BUILD SUCCESS.** Polecenie `mvn '-Dmaven.repo.local=.m2' clean test` wykonało cały zestaw z lokalnym cache, bez selektora i bez pomijania testów. Zakończenie: **09.10.2026, 13:15:18 CEST**, czas **23:35 min**. Konsola i niezależny audyt wszystkich XML Surefire potwierdzają **1083 testy w 153 klasach, 0 failures, 0 errors, 0 skipped**. Dotychczasowe 1072 przypadki oraz 11 nowych przeszły.

Pełny przebieg obejmuje 59 regresji Current Build Paladyna, 5 Search, 28 resolvera N8B, 6 ManualSimulationTickRuntimeTest (w tym legalny WAIT), katalogi Nekromanty i Book, runtime/loadout/snapshot, persistence oraz wszystkie testy HTTP nowego snapshotu i migracji INT. Goldeny N8B 2100/3150, 2520/3780, 22932/68796 i decimal 62.5/7.5 pozostają zachowane.

**git diff --check: PASS, exit 0.** Kontrola dodanych plików również nie wykazała trailing whitespace ani pustych końcowych linii. Komunikaty Git o przyszłej konwersji LF/CRLF nie były błędami kontroli.

**SHA-256: PASS.** Wszystkie **606 plików** zamrożonych przed clean test były identyczne po jego zakończeniu. Względem początku zadania: **9 dodanych, 15 zmienionych, 582 istniejące bez zmian, 0 usuniętych**. Osobny audyt 69 chronionych plików również potwierdza brak zmian, w tym w DamageEngine, ManualSimulationService, kernelu, N8B, neutralnych modelach runtime, Search i legacy definicjach. Po uzupełnieniu raportu ponownie skontrolowano hashe: różni się wyłącznie ten raport. Skan repo poza cache zależności nie wykazał plików ZIP; nie wykonano package ani utworzenia ZIP-a.

## Granice zakresu

Nie dodano Nekromanty do HeroClassDefs. DamageEngine, ManualSimulationService, DirectDamageCalculator i pozostałe pliki kernelu, resolver/profile N8B, modele runtime i ich invarianty, HeroProfile/HeroService, codec, persistence i katalogi pozostają bez zmian względem początku zadania. Katalog Nekromanty nadal 23/161, Book 3/9/27, Paladyn 24; SkillId nadal ma cztery wartości legacy. Wcześniejsze raporty N0–N11 pozostają historycznym zapisem zamkniętych etapów.

Nie rozwijamy skilli/upgrade'ów, Paragonu, Altars, Renown, Search Nekromanty ani pełnej symulacji. Nie zgadujemy Maximum Life/Toughness innych poziomów i nie tworzymy ZIP-a. Po zgodności nagiego snapshotu użytkownik może wskazać pierwszy item swojego rzeczywistego buildu do kolejnego zadania.
