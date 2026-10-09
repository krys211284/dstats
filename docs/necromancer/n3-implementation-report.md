# Raport wdrożenia N3 — Corpse

N3 rozszerza istniejący `NecromancerSkillTreeRegistry` do 13 aktywnych skilli i 91 ulepszeń: 4 Basic / 28, 6 Core / 42 i 3 Corpse / 21. Dane 10 wcześniejszych skilli oraz 24 skilli Paladyna pozostają bez zmian. Nie powstał drugi registry, kontroler ani ranking.

## Pliki N3

Zmodyfikowane:

- `README.md`
- `src/main/java/krys/necromancer/NecromancerSkillTreeRegistry.java`
- `src/main/java/krys/skilltree/CharacterTreeSkill.java`
- `src/main/java/krys/skilltree/UpgradeDamageModifier.java`
- `src/main/java/krys/ranking/DamageRankingService.java`
- `src/main/java/krys/web/DamageRankingRow.java`
- `src/main/java/krys/web/DamageRankingSearchText.java`
- `src/main/java/krys/web/DamageRankingPageRenderer.java`
- `src/main/resources/templates/damage-ranking.html`
- `src/test/java/krys/necromancer/NecromancerSkillTreeRegistryTest.java`
- `src/test/java/krys/necromancer/NecromancerCoreSkillTreeRegistryTest.java`
- `src/test/java/krys/necromancer/NecromancerUpgradeGroupMembershipTest.java`
- `src/test/java/krys/web/NecromancerCoreDamageRankingWebServerTest.java`
- `src/test/java/krys/web/DamageRankingWebServerTest.java`

Dodane:

- `src/main/java/krys/skilltree/SkillActivationRequirement.java`
- `src/main/java/krys/skilltree/SkillActivationConsumptionMode.java`
- `src/test/java/krys/skilltree/SkillActivationRequirementTest.java`
- `src/test/java/krys/skilltree/CharacterTreeSkillMetadataTest.java`
- `src/test/java/krys/necromancer/NecromancerCorpseSkillTreeRegistryTest.java`
- `src/test/java/krys/web/NecromancerCorpseDamageRankingWebServerTest.java`
- `docs/necromancer/corpse-source-data.md`
- `docs/necromancer/n3-implementation-report.md`

Lista dotyczy N3 względem migawki początku zadania. Wcześniejsze zmiany N0–N2.1 i zmiany użytkownika pozostają zachowane.

## Modele i semantyka

`CharacterTreeSkill.cooldownSeconds` jest nullable Integer źródłowego odnowienia: null = brak zapisanej wartości, dodatnie liczby są legalne, zero i ujemne wartości zabronione. Stare konstruktory domyślnie zapisują null; adapter Paladyna nie inferuje wartości z notes. Ranking przekazuje cooldown do istniejącego pola entry, bez `damagePerUse`, `effectiveCycleSeconds` i `theoreticalDps`.

`SkillActivationRequirement` jest neutralnym recordem `(id, displayName, int amount, consumptionMode)`: teksty niepuste, amount > 0, tryb wymagany. `SkillActivationConsumptionMode` ma `CONSUMED_ON_USE` i `REQUIRED_NOT_CONSUMED`. Lista skilla jest domyślnie pusta i defensywnie kopiowana. Wybuch Zwłok i Trupie Macki mają bazowo `corpse / Zwłoki / 1 / CONSUMED_ON_USE`. Aktywny Wojownik nie wymaga Zwłok; pasywne wskrzeszanie co 2 s do limitu 4 jest w notes. Główny zasób pozostaje Esencją, bez bazowego generowania +15/+35 z ulepszeń.

| Skill | R1 / max-view | Lucky Hit | Odnowienie | Aktywne wymaganie |
|---|---:|---:|---|---|
| Wybuch Zwłok | 180% / 459% | 25% | null | 1 zużywane Zwłoki |
| Trupie Macki | 20% / 98% | 30% | 11 s | 1 zużywane Zwłoki |
| Szkieletowy Wojownik | 65% / 166% za atak jednego wojownika | 20% | 10 s | brak |

## Trzy mapy 2 + 2 + 3

| Skill | Grupa 1 — Enhancements | Grupa 2 — Enhancements | Grupa 3 — Transformation |
|---|---|---|---|
| Wybuch Zwłok | Wiele Zwłok; Generowanie Esencji | Krwawa Kula; Wydajne Zwłoki | Krwawa Miazga; Miazmaty; Szrapnel |
| Trupie Macki | Szansa na Trafienie Krytyczne; Generowanie Esencji | Szansa na Szczęśliwy Traf; Odsłonięcie | Gorzkie Żniwa; Chodź Tu!; Szczęki Śmierci |
| Szkieletowy Wojownik | Premia do Obrażeń; Leczenie | Animusz; Odsłonięcie | Władca Marionetek; Służba i Ofiara; Litania Śmierci |

Istniejący golden test N2.1 został rozszerzony do 13 skilli. Sprawdza dokładne zbiory członków, każdą reprezentację `SkillUpgradeGroup` / `UpgradeDamageModifier.upgradeGroup` / `UpgradeDamageImpact.groupId` oraz wszystkie 12 kombinacji wyborów po jednej alternatywie z grupy. Dotychczasowe golden zbiory nie zostały zmienione.

## Warianty i konflikty

`UpgradeDamageModifier.componentDamagePercentRanks` przechowuje istniejący model tabel komponentów; wcześniejsze konstruktory dają pustą tabelę. Miazmaty: `DAMAGE_OVER_DURATION` 350/893, duration 6 s, zastępuje eksplozję. Szrapnel: `PRIMARY_DAMAGE` 180/459 na odłamek i osobny modifier liczby 8, zastępuje eksplozję. Szczęki Śmierci: `PRIMARY_DAMAGE` 100/255 zamiast 20/98. Służba i Ofiara: nowy `BURST_DAMAGE` 150/383 na śmierć wojownika; strata zdrowia 25%/sek. i odstęp wskrzeszania 0.5 s są opisowe. Wszystkie tabele mają tylko rangi 1 i 15, bez sumowania i interpolacji. Tooltipy pokazują tabele wariantów i jednostkę czasu.

Krwawa Miazga ma 75%[x] i rozmiar +50%. [Oficjalny patch 3.1](https://news.blizzard.com/en-us/article/24287406/diablo-iv-patch-notes-3-1) rozstrzyga nieaktualne 50%[x] D4Guides; Icy Veins i D4Builds potwierdzają 75%. Safe display YES, bez manual review. Lucky Hit Trupich Macek +40% pozostaje node'em grupy 2: typ i safe display `NEEDS_MANUAL_REVIEW`, impact `NEEDS_VERIFICATION`, dokładna nota PL/Icy Veins/D4Builds kontra usunięcie w changelogu EN 3.2.1. Baza nadal ma Lucky Hit 30%.

Wojownik ma wyłącznie CORPSE/MINION/SUMMON; konflikt Physical/Shadow i warianty Book of the Dead są w notes. Animusz to 3 Resolve, limit Władcy Marionetek +3; błędne sformułowania D4Builds są odnotowane. Litania zachowuje szansę krytyczną R1 +15%[+] / max-view +21%[+] w notes modyfikatora statusu, bez damage coefficient i interpolacji.

## Walidacja

Czysty przebieg wybranych regresji: **94 testy, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS. Obejmuje modele, wszystkie grupy, nowe golden values, Paladin adapter, neutralny pipeline i HTTP.

Pełny `mvn '-Dmaven.repo.local=.m2' clean test`: **BUILD SUCCESS, 683 testy w 110 zestawach, 0 failures, 0 errors, 0 skipped**, czas **22 min 48 s**; zakończony 06.10.2026 o 23:07:43 CEST. Maven skompilował od zera 344 pliki Java produkcyjne i 112 testowych. Sumy niezależnie potwierdzono z XML Surefire. Kod, testy i dokumentacja były niezmienione przez cały przebieg; po jego zakończeniu dopisano wyłącznie wynik walidacji w tym raporcie. `git diff --check` przechodzi.

HTTP: 200 dla obu klas; Nekromanta 13 wierszy (4/6/3), Paladyn 24. `skillGroup=corpse` i `sourceCategory=CORPSE` dają dokładnie 3. SSR: Odnowienie 11 s / 10 s / -, Wymaganie użycia `1 × Zwłoki (zużywane)` dla Wybuchu/Macek, `-` dla Wojownika i dotychczasowych wpisów. Wszystkie 91 nazw ulepszeń oraz Zwłoki, Służba i Ofiara, Szczęki Śmierci, Miazmaty i odnowienie są znajdowane przez wspólny search.

Porównanie tekstu dziesięciu istniejących definicji skilli z migawką N2.1 (po normalizacji końców linii) potwierdziło identyczność. Regresje Basic/Core i Paladyna pozostają aktywne. Hashe 97 plików Paladyna, combat, symulacji, searcha i identyfikatorów runtime są identyczne z początkiem N3; pozostałe pliki poza podaną listą N3 również pozostały bez zmian. N3 nie zmienia `HeroClass`, `SkillId`, `DamageEngine`, `ManualSimulationService`, `PaladinSkillDefs` ani current build; wszystkie modyfikatory mają runtime NO. Nie liczono DPS, sum minionów/odłamków/detonacji ani rang pośrednich. ZIP nie został utworzony; katalog target po testach nie zawiera ZIP-ów.
