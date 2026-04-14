# AGENTS.md

## Cel pliku
Ten plik jest stałym kontraktem pracy dla kolejnych zadań wykonywanych w tym repo.

## Zasady nadrzędne
- Repo startuje od pustego stanu. Implementacja ma być budowana od fundamentów, bez pozorowania istniejącej architektury, której jeszcze nie ma.
- [README.md](./README.md) jest kontraktem wykonawczym projektu i źródłem prawdy dla logiki, architektury, testów oraz zasad UI/debug.
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

## Język i dokumentacja
- Dokumentacja projektowa ma być prowadzona po polsku.
- Komentarze w kodzie mają być prowadzone po polsku.

## Zakres pracy w pustym repo
- Nie wolno udawać, że istnieją klasy, moduły albo warstwy, których jeszcze nie zbudowano.
- Nowe elementy architektury trzeba wprowadzać świadomie i spójnie z README, zamiast dopisywać placeholdery.
