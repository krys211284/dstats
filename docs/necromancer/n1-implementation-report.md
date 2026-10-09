# Raport wdrożenia N1

Raport historyczny zamkniętego N1. Aktualny katalog po N2 ma 10 skilli (4 Basic i 6 Core); bieżący zakres i walidację opisują [README](../../README.md) oraz [raport N2](n2-implementation-report.md). Opis `generationMaxKnown()` poniżej uwzględnia korektę semantyki N2 bez zmiany zachowania.

## 1. Pliki zmienione i dodane względem zakończonego N0

Lista wynika z porównania SHA-256 ze stanem repo przed N1; nie obejmuje wcześniejszych zmian użytkownika ani N0.

| Plik | Zmiana N1 |
|---|---|
| [src/main/java/krys/necromancer/NecromancerSkillTreeRegistry.java](../../src/main/java/krys/necromancer/NecromancerSkillTreeRegistry.java) | Dodany |
| [src/main/java/krys/ranking/DamageRankingService.java](../../src/main/java/krys/ranking/DamageRankingService.java) | Zmieniony |
| [src/main/java/krys/ranking/PlayableClass.java](../../src/main/java/krys/ranking/PlayableClass.java) | Zmieniony |
| [src/main/java/krys/ranking/SkillDamageRankingEntry.java](../../src/main/java/krys/ranking/SkillDamageRankingEntry.java) | Zmieniony |
| [src/main/java/krys/ranking/SkillTreeRegistryProvider.java](../../src/main/java/krys/ranking/SkillTreeRegistryProvider.java) | Zmieniony |
| [src/main/java/krys/skilltree/CharacterTreeSkill.java](../../src/main/java/krys/skilltree/CharacterTreeSkill.java) | Zmieniony |
| [src/main/java/krys/skilltree/SkillResourceData.java](../../src/main/java/krys/skilltree/SkillResourceData.java) | Zmieniony |
| [src/main/java/krys/skilltree/SkillResourceGenerationFrequency.java](../../src/main/java/krys/skilltree/SkillResourceGenerationFrequency.java) | Dodany |
| [src/main/java/krys/web/CurrentBuildWebServer.java](../../src/main/java/krys/web/CurrentBuildWebServer.java) | Zmieniony |
| [src/main/java/krys/web/DamageRankingController.java](../../src/main/java/krys/web/DamageRankingController.java) | Zmieniony |
| [src/main/java/krys/web/DamageRankingPageRenderer.java](../../src/main/java/krys/web/DamageRankingPageRenderer.java) | Zmieniony |
| [src/main/java/krys/web/DamageRankingRow.java](../../src/main/java/krys/web/DamageRankingRow.java) | Zmieniony |
| [src/main/java/krys/web/DamageRankingSearchText.java](../../src/main/java/krys/web/DamageRankingSearchText.java) | Zmieniony |
| [src/test/java/krys/necromancer/NecromancerSkillTreeRegistryTest.java](../../src/test/java/krys/necromancer/NecromancerSkillTreeRegistryTest.java) | Dodany |
| [src/test/java/krys/paladin/PaladinSkillTreeAdapterTest.java](../../src/test/java/krys/paladin/PaladinSkillTreeAdapterTest.java) | Zmieniony |
| [src/test/java/krys/ranking/DamageRankingServiceTest.java](../../src/test/java/krys/ranking/DamageRankingServiceTest.java) | Zmieniony |
| [src/test/java/krys/skilltree/SkillResourceDataTest.java](../../src/test/java/krys/skilltree/SkillResourceDataTest.java) | Dodany |
| [src/test/java/krys/web/DamageRankingRowDecimalTest.java](../../src/test/java/krys/web/DamageRankingRowDecimalTest.java) | Dodany |
| [src/test/java/krys/web/NecromancerDamageRankingWebServerTest.java](../../src/test/java/krys/web/NecromancerDamageRankingWebServerTest.java) | Dodany |
| [docs/necromancer/basic-source-data.md](../../docs/necromancer/basic-source-data.md) | Dodany |
| [README.md](../../README.md) | Zmieniony |
| [docs/necromancer/n1-implementation-report.md](n1-implementation-report.md) | Dodany — niniejszy raport |

## 2. Model zasobu

`SkillResourceData(BigDecimal cost, BigDecimal generationMin, BigDecimal generationMax, BigDecimal generationBonusKnown, SkillResourceGenerationFrequency generationFrequency)`. Równe min/max oznaczają wartość dokładną; obie puste brak danych. Częstotliwość to `PER_USE` lub `PER_SECOND`. Konstruktor trzech `Integer` zachowuje dane i prezentację Paladyna. Bazowy sort porównuje min, maxKnown porównuje max z uwzględnieniem znanego możliwego dodatku z wybranego ulepszenia, bez przeliczania częstotliwości. Puste wartości trafiają na koniec obu kierunków.

## 3. Źródło

Neutralne `CharacterTreeSkill` i `SkillDamageRankingEntry` mają `sourceReference` / `getSourceReference()`, bez delegującego starego gettera. Ranking przekazuje nowe pole. Adapter Paladyna nadal pobiera legacy `getSourcePdf()` i zapisuje jego treść w neutralnym polu. Verification Matrix i pomocnicze metadane legacy PDF pozostają przy dotychczasowym kontrakcie. Nekromanta wskazuje WWW.

## 4. Procenty dziesiętne

Parser przyjmuje kropkę lub przecinek, przechowuje wynik jako `BigDecimal` i wybiera największy procent z `value`. Nie zgubi części 7.5% ani 62.5%; porównanie sortowania nie używa zaokrąglenia ani liczb zmiennoprzecinkowych. Testy obejmują 7.5%, 62.5%, 50%, 7,5%, kilka procentów oraz HTTP asc/desc z brakującą wartością. Tabele bazowych rang nadal używają `Integer`.

## 5. Dane czterech skilli

| Umiejętność | R1 | Max-view | Lucky Hit | Esencja | Ulepszenia |
|---|---:|---:|---:|---|---|
| Kościane Odłamki | 30% na odłamek | 77% na odłamek | 30% | 10 / użycie, znany opcjonalny dodatek +5 | 2 + 2 + 3 |
| Żniwa | 50% | 128% | 30% | 15 / użycie | 2 + 2 + 3 |
| Rozkład | 150% / sek. | 383% / sek. | 40% | 12–16 / sek. | 2 + 2 + 3 |
| Krwotok | 80% | 204% | 50% | 13 / użycie | 2 + 2 + 3 |

Łącznie cztery skille Basic i 28 ulepszeń. Każdy `DAMAGE / NEEDS_VERIFICATION`. Tabele zawierają tylko 1 i 15; max-view nie jest niezależnym potwierdzeniem etykiety Rank 15. Szczegóły i wszystkie nazwy w [danych źródłowych](basic-source-data.md).

## 6. Konflikty źródeł

Rozkład / Szansa na Szczęśliwy Traf: zachowany node +30%, `safeForRankingDisplay = NEEDS_MANUAL_REVIEW`, impact `NEEDS_VERIFICATION`; tooltip podaje konflikt PL i bieżącego D4Builds z usunięciem opisu w EN changelogu 3.2.1. Żniwa: R1 krytyka +10% nie zastępuje +20% z D4Builds; dokładne 62.5% Selekcji Naturalnej z baseline N1 pozostaje obok informacji o wyświetlanym 63%; 7.5 Esencji Dorzynek pozostaje obok źródłowego zaokrąglenia do 8. Rozkład zachowuje 12–16 mimo pojedynczego 13 w D4Builds. Pochodzenie oraz ograniczenie max-view są zapisane przy każdym skillu i w dokumentacji.

## 7. Request HTTP

`GET /ranking-obrazen?character=necromancer` w rzeczywistym `CurrentBuildWebServer` zwraca **HTTP 200**, aktywną klasę `Nekromanta` i dokładnie ID `kosciane_odlamki`, `zniwa`, `rozklad`, `krwotok`, bez wpisów Paladyna. Ten sam kontroler i renderer obsługują obie klasy. Sprawdzono wyszukiwanie nazw wszystkich 28 ulepszeń, nazwy z polskimi znakami, tagi, facet generowania, oba kierunki sortowania zasobu oraz `12–16 / sek.` w komórce Rozkładu.

## 8. Testy

Pełny `mvn '-Dmaven.repo.local=.m2' clean test`: **BUILD SUCCESS — 635 testów w 103 zestawach, 0 failures, 0 errors, 0 skipped; 22 min 50 s**, zakończony 06.10.2026. Obejmuje runtime, manual simulation, search, current build, audyty Paladyna, nowe regresje Nekromanty oraz cały pipeline obrazu/OCR.

Ukierunkowany pierwszy przebieg nowych regresji N1 i rankingu Paladyna: **38 testów, bez błędów**. Po końcowej korekcie dwóch tekstów UI ponowiono wszystkie regresje wspólnego drzewa, zasobów, serwisu i HTTP rankingu: **53 testy, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. Potwierdzono także obecność końcowych tekstów w skompilowanych klasach. `git diff --check` przechodzi. Nie uruchomiono tworzenia ZIP-a.

## 9. Paladyn

Provider produkcyjny nadal zwraca dokładnie 24 skille Paladyna. Exhaustive adapter regression chroni wartości, tabele, źródła, kolejność, kategorie, tagi, Lucky Hit, zasób i modyfikatory. Dotychczasowe 21 regresji HTTP rankingu Paladyna przechodzi, w tym historyczne aliasy filtrów/sortowania i domyślny wybór Paladyna.

## 10. Runtime / current build

`HeroClass` nadal zawiera wyłącznie `PALADIN`; `SkillId` nie ma ID Nekromanty. SHA-256 `DamageEngine`, `ManualSimulationService`, `SkillId`, `PaladinSkillDefs`, `HeroClass` i istniejącego rejestru Paladyna są identyczne względem początku N1. Ranking Nekromanty przechodzi testy bez instancji engine, a jego metryki obrażeń użycia, cooldownu, cyklu i DPS pozostają puste. Wszystkie modyfikatory mają `safeForRuntimeDps = NO`. Jedyna zmiana `CurrentBuildWebServer` to fabryka providera rejestrów rankingu; funkcje current build nie uzyskały obsługi Nekromanty. Wcześniejsze zmiany użytkownika zachowano.
