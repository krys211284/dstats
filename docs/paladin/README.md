# Źródła Paladyna

Katalog `source-md/` jest nowym edytowalnym źródłem dokumentacji Paladyna. Zawiera opisowe rejestry umiejętności, materiały przeniesione do Markdown oraz pomocniczy ekstrakt danych rang z lokalnej paczki HTML Fextralife.

Katalog `source-pdfs/` zostaje czasowo w repo jako archiwum i źródło porównawcze dla wcześniejszych materiałów. PDF-y nie są już opisywane jako jedyne źródło prawdy; nowe prace dokumentacyjne powinny zaczynać się od edytowalnych plików Markdown w `source-md/`, a PDF-y mogą służyć do porównania albo audytu pochodzenia danych.

Plik `source-md/paladin_fextralife_rank_tables.json` jest maszynowym ekstraktem tabel rang z lokalnej paczki HTML Fextralife. Te dane są źródłem pomocniczym dla `DamagePercentRankTable`, a nie gotową implementacją DPS. Na obecnym etapie do rejestru wpisano pełną tabelę rang tylko dla `blogoslawiony_mlot`.

## Zasady pracy

- Nie wolno zgadywać brakujących wartości liczbowych.
- Wartości i mechaniki muszą pochodzić z jawnych materiałów źródłowych w `source-md/` albo z archiwalnych PDF-ów w `source-pdfs/`, jeśli są używane porównawczo.
- Obecność danych Markdown albo JSON nie odblokowuje runtime DPS.
- Wartości damage/rank z Markdown albo JSON są wejściem źródłowym do tabel rang, nie gotowym DPS i nie podstawą do automatycznego wypełniania runtime.
- Wpisy oznaczone jako `DO_WERYFIKACJI` mają zostać później odwzorowane jako `requiresVerification: true`.
- Niepewne mechaniki nie mogą wpływać na kalkulacje DPS bez osobnej weryfikacji.
- Każda zmiana logiki wymaga aktualizacji kodu, testów i README w tym samym commicie.

## Zakres

Ten katalog przechowuje dokumenty źródłowe i zasady ich użycia. Aplikacyjny rejestr drzewa Paladyna jest zaimplementowany w `krys.paladin.PaladinSkillTreeRegistry`, ale pełny runtime DPS dla tych umiejętności nie jest jeszcze zaimplementowany.

Nie są wymagane pliki `source-md/README.md` ani `source-md/SHASUMS.txt`. Nawigacja i kontrakt dokumentacji mogą być prowadzone przez ten plik oraz główny `README.md`.

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
- Markdown, JSON i PDF-y pozostają źródłami tooltipów oraz danych wejściowych, ale sam tooltip z adnotacją `DO WERYFIKACJI` ani sama tabela rang nie wystarczają do implementacji wpływu na DPS.
