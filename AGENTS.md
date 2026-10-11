# AGENTS.md

## Cel pliku
Ten plik jest stałym kontraktem pracy dla kolejnych zadań wykonywanych w tym repo.

## Zasady nadrzędne
- Repo startuje od pustego stanu. Implementacja ma być budowana od fundamentów, bez pozorowania istniejącej architektury, której jeszcze nie ma.
- [README.md](./README.md) jest kontraktem wykonawczym projektu i źródłem prawdy dla logiki, architektury, testów oraz zasad UI/debug.
- README musi odzwierciedlać realny aktualny zakres repo, a nie szerszy plan projektu.
- Każda zmiana logiki wymaga równoczesnej aktualizacji kodu, testów i README.

## Logika i liczby
- Nie zgadujemy wartości liczbowych.
- Nowe liczby w testach muszą wynikać z golden values albo z aktualnego outputu engine dla tego samego buildu.
- Projekt liczy single target jako model domyślny i kontraktowy.
- Search i manual simulation muszą używać tego samego `Damage Engine` i tej samej logiki runtime.

## Jakość dostarczania
- Nie wolno oddawać niepełnych paczek projektu.
- Nie wolno oddawać paczki z failing testami.
- Jeżeli logika się zmienia, a testy albo README nie zostały zaktualizowane, zadanie nie jest ukończone.

## Walidacja i budowanie
- Podczas implementacji najpierw uruchamiamy celowane suites: `mvn '-Dmaven.repo.local=.m2' '-Dtest=ChangedArea*Test' test`.
- Po implementacji uruchamiamy `mvn '-Dmaven.repo.local=.m2' -Pfast test`: wszystkie testy poza jawnym tagiem JUnit `ocr-integration`.
- Zmiana OCR, preprocessingu, segmentacji lub importera obrazu wymaga dodatkowo `mvn '-Dmaven.repo.local=.m2' -Pocr test`.
- Bez profilu `mvn test` nadal oznacza FULL, łącznie z prawdziwym OCR. Pełny przebieg uruchamiamy przy zamknięciu większego milestone'u, zmianie szerokiej infrastruktury lub jawnym żądaniu użytkownika.
- Nie używamy `clean` w każdej iteracji. `clean test` jest mocnym checkpointem końcowym.
- Po zakończonych testach budujemy aplikację przez `mvn '-Dmaven.repo.local=.m2' -DskipTests package`, bez powtarzania Surefire. Dystrybucja musi zawierać `target/app/dstats.jar` i `target/app/lib/*`.
- Zwykłe `package` tworzy także archiwum źródeł/projektu `target/dstats-YYYY-MM-DD-HH-mm.zip`, bez dodatkowego profilu. `mvn clean package` wykonuje domyślne FULL; `mvn clean package -DskipTests` pomija wykonanie testów Surefire i również tworzy ZIP. Codex uruchamia budowanie ZIP-a na jawne zlecenie użytkownika.
- `ocr-integration` oznacza prawdziwe rozpoznawanie Tess4J/Tesseract, a nie cały package `image`. Testy parserów, mapperów i audytów evidence używają typowanych golden OCR fixtures.
- Nie usuwamy przypadków testowych ani assertions, nie zmieniamy golden values i nie wyłączamy OCR w domyślnym FULL.

## Język i dokumentacja
- Dokumentacja projektowa ma być prowadzona po polsku.
- Komentarze w kodzie mają być prowadzone po polsku.

## Zakres pracy w pustym repo
- Nie wolno udawać, że istnieją klasy, moduły albo warstwy, których jeszcze nie zbudowano.
- Nowe elementy architektury trzeba wprowadzać świadomie i spójnie z README, zamiast dopisywać placeholdery.
