# Źródła Paladyna

Katalog `source-pdfs/` zawiera dokumenty źródłowe dla rejestru umiejętności Paladyna. Te PDF-y są źródłem prawdy dla wartości liczbowych, opisów mechanik oraz przyszłego odwzorowania danych umiejętności.

## Zasady pracy

- Nie wolno zgadywać brakujących wartości liczbowych.
- Wartości i mechaniki muszą pochodzić z PDF-ów w `source-pdfs/`.
- Wpisy oznaczone jako `DO_WERYFIKACJI` mają zostać później odwzorowane jako `requiresVerification: true`.
- Niepewne mechaniki nie mogą wpływać na kalkulacje DPS bez osobnej weryfikacji.
- Każda zmiana logiki wymaga aktualizacji kodu, testów i README w tym samym commicie.

## Zakres

Ten katalog przechowuje wyłącznie dokumenty źródłowe i zasady ich użycia. Model danych umiejętności Paladyna nie jest jeszcze implementowany w repozytorium.
