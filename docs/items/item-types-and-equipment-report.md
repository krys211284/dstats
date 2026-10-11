# Typy itemów i zgodność wyposażenia

Stan: **CLOSED** — implementacja, dokumentacja, testy celowane, pełny clean test i końcowe audyty zakończone.

Zakres dotyczy identyfikacji itemu, formularza, zapisu i dozwolonej konfiguracji wyposażenia. Punktem startowym jest stan repo po zamknięciu zweryfikowanego baseline'u nagiego Nekromanty level 70. Nie wprowadzono nowych wartości obrażeń ani efektu unikatowego OLŚNIENIA.

## 1. Finalny model ItemType

Neutralny enum `krys.item.ItemType` przechowuje stabilne technical id, polską nazwę prezentacyjną, dozwolony `EquipmentSlot`, flagę `weapon`, `ItemHandedness` i aliasy legacy. `ItemImportDetails.getResolvedItemType()` udostępnia kanoniczny typ, a `getItemType()` zachowuje kompatybilny tekst nazwy albo nierozpoznany tekst historyczny. Dane liczbowe itemu i statystyki runtime nie zostały zmienione.

## 2. Registry

`ItemTypeRegistry` jest wspólnym katalogiem używanym przez PART_1, formularze importu i edycji, persistence, prezentację/filtry biblioteki oraz hartowanie. `selectableTypes()` dostarcza opcje UI. `resolve()` rozpoznaje dokładne technical id lub znormalizowane znane aliasy. `matchingTypes()` wyszukuje aliasy z granicami słów w klasyfikacji OCR; niejednoznaczność pozostaje błędem odczytu. Usunięto lokalne `Part1ReadParser.TYPE_MAPPINGS` i decyzje hartowania oparte na `contains` polskich nazw.

## 3. Legacy migration

Stare `Tarcza`, `Hełm`, `Buty`, `Miecz` i dotychczasowe angielskie aliasy nadal są odczytywane. Payload details zachowuje `ITEM_TYPE` i dodaje linię `ITEM_TYPE_ID`; zewnętrzny format rekordu pozostaje kompatybilny. Znany technical id ma pierwszeństwo przy odczycie nowych rekordów. Brak id uruchamia rozpoznanie tekstu legacy, bez przepisywania pliku podczas odczytu.

Nieznany tekst, również zawierający słowo znanego typu, nie jest losowo migrowany: `Nieznana tarcza` pozostaje `UNKNOWN` z zachowanym oryginalnym tekstem. Formularz pokazuje historyczną wartość do ręcznej korekty. Ogólne stare `Broń główna` / `Main hand` są reprezentowane przez migracyjny `LEGACY_MAIN_HAND`, z handedness `UNKNOWN`, i nie trafiają do listy nowych typów. Nie zgadujemy liczby rąk tego historycznego opisu.

Przed rozszerzeniem katalogu sprawdzono dotychczasowy parser, modele, fixture'y, testy i zapis biblioteki. Lokalny plik użytkownika `saved-items.db` był pusty; sprawdzenie było wyłącznie odczytem. Nie było potwierdzonych danych uzasadniających dodanie innych rodzajów broni Nekromanty.

## 4. Aktualnie obsługiwane typy

| Technical id | Nazwa PL | EquipmentSlot | Weapon | Handedness |
|---|---|---|---|---|
| `SHIELD` | Tarcza | `OFF_HAND` | false | `NONE` |
| `HELMET` | Hełm | `HELMET` | false | `NONE` |
| `BOOTS` | Buty | `BOOTS` | false | `NONE` |
| `SWORD` | Miecz | `MAIN_HAND` | true | `ONE_HANDED` |
| `SCYTHE_TWO_HANDED` | Kosa dwuręczna | `MAIN_HAND` | true | `TWO_HANDED` |

To pięć opcji wybieranych w UI. `UNKNOWN` ma pusty slot. `LEGACY_MAIN_HAND` służy tylko kompatybilności. `SWORD` zachowuje istniejącą semantykę jednoręcznej broni foundation, w tym współistnienie z tarczą i dostępność hartowania. Nie dodano kosy jednoręcznej, różdżki ani fokusu.

## 5. Wynik parsera dla kosy dwuręcznej

Regresja PART_1 dla nazwy `OLŚNIENIE`, klasyfikacji `Starożytna unikatowa kosa dwuręczna` i item power `900` potwierdza:

- item type: `SCYTHE_TWO_HANDED`,
- display: `Kosa dwuręczna`,
- slot: `MAIN_HAND`,
- handedness: `TWO_HANDED`,
- rarity: `UNIQUE`,
- ancient: `true`,
- item power: `900`.

Ten fixture potwierdza klasyfikację. Wartości rzeczywistych statystyk i działanie efektu unikatowego wymagają osobnych danych użytkownika i nie są częścią tego zadania.

## 6. Walidacja type → slot

Zmiana typu w UI ustawia slot z definicji registry. Zachowany edytowalny select slotu nie omija walidacji backendu. `ItemImportFormMapper` odrzuca brak potwierdzonego typu, brak slotu i niezgodność typu ze slotem; `ItemLibraryService` dodatkowo pilnuje zgodności znanego typu przy zapisie, aktualizacji i aktywacji.

Osobne regresje odrzucają `SCYTHE_TWO_HANDED + HELMET`, `SCYTHE_TWO_HANDED + OFF_HAND`, `SHIELD + MAIN_HAND`, `HELMET + MAIN_HAND`. Nie ma automatycznej zmiany slotu błędnego submitu. Historyczne programowe modele bez danych typu pozostają odczytywalne; nowy zapis z formularza wymaga potwierdzonego typu i slotu.

## 7. Handedness

`ItemHandedness` zawiera `NONE`, `ONE_HANDED`, `TWO_HANDED` oraz `UNKNOWN` dla nieprecyzyjnego historycznego opisu. Kosa dwuręczna jest bronią `TWO_HANDED` w `MAIN_HAND`. Nie dodano `EquipmentSlot.TWO_HAND` ani nowego runtime wyposażenia.

## 8. Blokada TWO_HANDED + OFF_HAND

`ItemLibraryService.validateEquipmentSelection()` i wspólne rozwiązywanie przypisań odrzucają jednoczesną dwuręczną broń `MAIN_HAND` oraz item `OFF_HAND`. Walidacja obejmuje wkład do buildu, statystyki aktywnych itemów, zakładanie z biblioteki i current build. Globalny zapis najpierw buduje i sprawdza cały kandydat, następnie zapisuje selekcję. Nie pozostawia częściowo zmienionego wyposażenia po błędzie. Search pomija niedozwolone kombinacje.

Edycja miecza założonego dowolnemu bohaterowi na kosę dwuręczną jest odrzucana, jeśli ten bohater ma aktywny `OFF_HAND`. Itemy nie są usuwane z biblioteki. Wczytany historyczny konflikt pokazuje błąd w current build obu klas i nie uruchamia obliczenia nielegalnego zestawu; zapisany wybór pozostaje bez zmian. Jawne `Wyczyść slot` pozwala usunąć `OFF_HAND`. UI sygnalizuje blokadę i nie oferuje założenia OFF_HAND przy dwuręcznej broni.

Regresje HTTP sprawdzają obie kolejności zakładania, aktywację z biblioteki, pojedynczą zmianę slotu, globalny submit, edycję aktywnej broni oraz korektę niespójnego historycznego wyposażenia dla Paladyna i Nekromanty. Jednoręczny miecz z tarczą nadal jest dozwolony.

## 9. Brak przypadkowego HELMET default

Wspólne UI importu i edycji renderuje `<select name="itemType">` z centralnego registry. Pierwsze opcje typu i slotu są puste i opisane jako `UNKNOWN / wybierz`. Nieznany import pozostawia oba pola nierozpoznane; test sprawdza brak wybranego `HELMET`. Submit bez ręcznej korekty daje jawne błędy walidacji i niczego nie zapisuje. Historyczny nieznany tekst pozostaje widoczny i escapowany.

Filtr typu i prezentacja biblioteki używają nazw PL z registry dla rozpoznanych typów; nieznane wartości historyczne pozostają dostępne. Nie utrzymujemy osobnej listy typów w rendererze.

## 10. TemperingEligibilityRegistry

Decyzje używają `ItemType`; kompatybilne overloady tekstowe delegują do centralnego parsera. Zachowano dotychczasową dostępność:

- `SWORD` / stare `Miecz`, `Sword` oraz migracyjne `Broń główna`, `Main hand` w `MAIN_HAND`: `WEAPON`, `OFFENSE`,
- `SHIELD` w `OFF_HAND`: `WEAPON`, `OFFENSE`, `DEFENSE`, `UTILITY`,
- hełm: `DEFENSE`,
- pozostałe nieuzupełnione typy: brak kategorii.

`SCYTHE_TWO_HANDED` nie otrzymuje nowych kategorii. Regresja porównuje stare wejścia tekstowe z typowanymi definicjami.

## 11. Testy i zakres zmian

Dodano cztery klasy regresji: `ItemTypeRegistryTest`, `ItemTypeImportTest`, `ItemTypePersistenceAndEquipmentTest`, `ItemTypeWebFlowTest`. Łącznie z dodatkowym testem hartowania są to 23 nowe przypadki. Uzupełniono historyczne fixture'y formularzy o jawny, zgodny typ i slot, a fixture zapisanych butów o potwierdzone `ItemImportDetails`. Dotyczy to również testów domyślnego katalogu storage i bazy wiedzy, których stare formularze zapisu tarczy pomijały typ itemu. Zachowano ich scenariusze zapisu/uczenia, zamiast zmieniać je w testy walidacji. Te zmiany legalizują wejście testu; nie są automatyczną naprawą produkcyjnego importu i nie zmieniają istniejących golden values obrażeń.

Ostatni zestaw celowany zakończył się `BUILD SUCCESS`: **260 tests, 0 failures, 0 errors, 0 skipped**, 14,803 s, 2026-10-09 15:00:19 CEST. Obejmował import, PART_1, bibliotekę/persistence, storage, bazę wiedzy, UI, hartowanie, current build obu klas, zweryfikowany baseline i search.

Polecenie zestawu celowanego:

```powershell
mvn '-Dmaven.repo.local=.m2' '-Dtest=ItemType*Test,Part1ReadParserTest,ItemImportFormMapperTest,ImageItemImportDraftMapperTest,TemperingRegistryTest,ItemLibrary*Test,FileItemLibraryRepositoryTest,Item*PageRendererTest,CurrentBuildWebServerTest,CurrentBuildWebServerStorageIntegrationTest,ItemKnowledgeWebServerTest,Necromancer*WebFlowTest,CurrentBuildRuntimeInputResolverTest,SearchBuildWebServerTest,HeroClass*Test,HeroCore*Test' test
```

Zakres wyznaczono względem SHA-256 stanu początkowego tego zadania, zawierającego 606 plików źródłowych/kontraktowych. Dodano 8 plików: trzy produkcyjne modele/registry, cztery klasy testowe i ten raport. Zmieniono 24 istniejące pliki: 16 produkcyjnych integracji, siedem testów i README. Nie usunięto plików.

Zmiany wcześniejszego zadania były już obecne przy rozpoczęciu pracy i zostały zachowane. Baseline Nekromanty level70, jego poprzedni raport i testy, `DamageEngine`, `ManualSimulationService`, `DirectDamageCalculator`, N8B, katalogi skilli i Book of the Dead pozostają bez zmian względem tego punktu startowego.

## 12. Pełny clean test

Polecenie: `mvn '-Dmaven.repo.local=.m2' clean test`. Parametr wskazuje lokalny cache zależności; wykonywane fazy to wymagane `clean test`. Pierwszy pełny przebieg wykrył dwa stare fixture'y bez typu tarczy (storage i baza wiedzy). Po ich jawnym uzupełnieniu ponowny pełny przebieg zakończył się **BUILD SUCCESS**.

| Kontrola | Wynik |
|---|---|
| Testy | 1106 |
| Klasy z raportami Surefire XML | 157 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Czas Maven | 23:00 min |
| Zakończenie | 2026-10-09 15:23:50 CEST |
| Kod wyjścia | 0 |

Osobny odczyt wszystkich 157 raportów XML potwierdził te same sumy, bez raportów z failures/errors/skips. SHA-256 wszystkich 614 plików objętych zamrożeniem potwierdził brak zmian podczas pełnego przebiegu. Po teście uzupełniono wyłącznie ten raport; kod, testy i README pozostają w stanie przetestowanym pełnym clean testem.

## 13. git diff --check

`git diff --check` przed i po pełnym teście: kod wyjścia `0`, bez błędów whitespace. Ostrzeżenia Git o przyszłej konwersji LF/CRLF nie są błędami whitespace. Wariant z `core.safecrlf=false` jest również czysty. Osobno sprawdzono osiem nowych plików, których zwykły `git diff` jeszcze nie obejmuje. Porównanie SHA-256 potwierdza zachowanie zamrożonego kodu i chronionych obszarów względem początku zadania.

## 14. Brak ZIP

Nie uruchamiano `package` ani archiwizacji. **ZIP nie został utworzony.** Końcowy audyt plików projektu, również ukrytych, poza cache `.m2` i metadanymi `.git`: **0 archiwów ZIP**.
