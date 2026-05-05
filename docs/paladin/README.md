# Źródła Paladyna

Katalog `source-pdfs/` zawiera dokumenty źródłowe dla rejestru umiejętności Paladyna. Te PDF-y są źródłem prawdy dla wartości liczbowych, opisów mechanik oraz przyszłego odwzorowania danych umiejętności.

## Zasady pracy

- Nie wolno zgadywać brakujących wartości liczbowych.
- Wartości i mechaniki muszą pochodzić z PDF-ów w `source-pdfs/`.
- Wpisy oznaczone jako `DO_WERYFIKACJI` mają zostać później odwzorowane jako `requiresVerification: true`.
- Niepewne mechaniki nie mogą wpływać na kalkulacje DPS bez osobnej weryfikacji.
- Każda zmiana logiki wymaga aktualizacji kodu, testów i README w tym samym commicie.

## Zakres

Ten katalog przechowuje dokumenty źródłowe i zasady ich użycia. Aplikacyjny rejestr drzewa Paladyna jest zaimplementowany w `krys.paladin.PaladinSkillTreeRegistry`, ale pełny runtime DPS dla tych umiejętności nie jest jeszcze zaimplementowany.

## Verification Matrix

Warstwa `krys.verification` przechowuje macierz mechanik Paladyna wymagających osobnej weryfikacji przed użyciem w kalkulacjach. Dotyczy to przede wszystkim tooltipów oznaczonych jako `DO WERYFIKACJI` albo `DO_WERYFIKACJI` oraz mechanik, których zachowanie single target nie wynika jednoznacznie z PDF-ów.

Proces dopuszczenia mechaniki do runtime:

1. Tooltip z PDF trafia do `Verification Matrix` jako wpis `requiresVerification`.
2. Wpis opisuje pytanie weryfikacyjne, źródłowy PDF, kategorię, impact oraz domyślne zachowanie silnika przed weryfikacją.
3. Mechanika przechodzi osobny test empiryczny poza tooltipem.
4. Dopiero po teście status może zostać zmieniony na `verified`.
5. Dopiero wpis `verified` może zostać wykorzystany w DPS runtime.

Zasady dla `Verification Matrix`:

- Brakujące wartości liczbowe i zachowania mechanik nie mogą być zgadywane.
- Mechanika ze statusem `requiresVerification` nie może wpływać na wynik DPS.
- Próba użycia niezweryfikowanej mechaniki w kalkulacji musi zostać jawnie zablokowana albo pominięta zgodnie z jej `default engine behavior`.
- Zmiana statusu z `requiresVerification` na `verified` wymaga testu jednostkowego albo regresyjnego, aktualizacji README oraz wskazania źródła weryfikacji.
- PDF-y pozostają źródłem tooltipów, ale sam tooltip z adnotacją `DO WERYFIKACJI` nie wystarcza do implementacji wpływu na DPS.
