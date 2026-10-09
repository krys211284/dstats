# N5 — raport wdrożenia Klątw / Curse

Baseline: Sezon 15 / patch 3.2.1. Zakres: katalog źródłowy, neutralne komponenty/interwały, SSR/search, testy i README.
Po N5: **19 aktywnych skilli Nekromanty, 133 ulepszenia**; Paladyn **24 skille**.
Dane i membership wcześniejszych 17 skilli / 119 ulepszeń są zachowane.
Źródła i dokładne semantyki: [curse-source-data.md](curse-source-data.md).

## 1. Pliki zmienione i dodane

Lista dotyczy N5 względem zastanego stanu po N4, a nie całego istniejącego dirty diff repo.
24 pliki: 20 zmienionych, 4 dodane. Nie usunięto plików.

| Plik | Zmiana |
| --- | --- |
| `docs/necromancer/curse-source-data.md` | dodany |
| `docs/necromancer/n5-implementation-report.md` | dodany |
| `README.md` | zmieniony |
| `src/main/java/krys/necromancer/NecromancerSkillTreeRegistry.java` | zmieniony |
| `src/main/java/krys/skilltree/DamagePercentComponent.java` | zmieniony |
| `src/main/java/krys/skilltree/DamagePercentComponentRankTable.java` | zmieniony |
| `src/main/java/krys/web/DamageRankingController.java` | zmieniony |
| `src/main/java/krys/web/DamageRankingPageRenderer.java` | zmieniony |
| `src/main/java/krys/web/DamageRankingSearchText.java` | zmieniony |
| `src/test/java/krys/necromancer/NecromancerCoreSkillTreeRegistryTest.java` | zmieniony |
| `src/test/java/krys/necromancer/NecromancerCorpseSkillTreeRegistryTest.java` | zmieniony |
| `src/test/java/krys/necromancer/NecromancerCurseSkillTreeRegistryTest.java` | dodany |
| `src/test/java/krys/necromancer/NecromancerMacabreSkillTreeRegistryTest.java` | zmieniony |
| `src/test/java/krys/necromancer/NecromancerSkillTreeRegistryTest.java` | zmieniony |
| `src/test/java/krys/necromancer/NecromancerUpgradeGroupMembershipTest.java` | zmieniony |
| `src/test/java/krys/skilltree/CharacterTreeSkillMetadataTest.java` | zmieniony |
| `src/test/java/krys/skilltree/DamagePercentComponentRankTableTest.java` | zmieniony |
| `src/test/java/krys/skilltree/DamagePercentComponentTest.java` | zmieniony |
| `src/test/java/krys/skilltree/SkillChargeDataTest.java` | zmieniony |
| `src/test/java/krys/web/NecromancerCoreDamageRankingWebServerTest.java` | zmieniony |
| `src/test/java/krys/web/NecromancerCorpseDamageRankingWebServerTest.java` | zmieniony |
| `src/test/java/krys/web/NecromancerCurseDamageRankingWebServerTest.java` | dodany |
| `src/test/java/krys/web/NecromancerMacabreDamageRankingWebServerTest.java` | zmieniony |
| `src/test/java/krys/web/NeutralDamageRankingWebServerTest.java` | zmieniony |

Zmiany starszych testów dostosowują aktualną liczebność do 19/133, zachowują zakresy historyczne
i rozszerzają golden mapę o dwa skille. Poprzednie wartości i mapy nie zostały zastąpione.
Bez zmian: DamageRankingService, modele cooldownu/ładunków/aktywacji, adapter i dane Paladyna,
runtime/current build oraz dokumenty zamkniętych N0–N4.

## 2–3. Komponenty i interwał

`PERIODIC_DAMAGE` przechowuje obrażenia pojedynczego dyskretnego impulsu.
`EVENT_TRIGGERED_DAMAGE` przechowuje obrażenia jednego źródłowo opisanego zdarzenia bez zadanej częstotliwości.
`DamagePercentComponentRankTable.of(tables, durations, intervals)` kopiuje mapy;
`intervalSecondsFor(component)` zwraca opcjonalny dodatni interwał przypisany tylko do obecnego komponentu.
Brak interwału jest poprawny. Interwał nie jest duration; reguła obowiązkowego duration
dla `DAMAGE_OVER_DURATION` pozostaje. Nie dodano engine częstotliwości ani obliczania DPS.

SSR bazowych i wariantowych tabel pokazuje „co 2 s” oraz „na zdarzenie”.
Search indeksuje interwały, komponenty wariantów i notes źródłowe wszystkich klas,
bez wyjątku dla Curse. Test fikcyjnego rejestru z kluczem Paladyna sprawdza tę neutralność.

## 4–5. Dane bazowe

| Pole | Żelazna Dziewica | Zniedołężnienie |
| --- | --- | --- |
| Id | `zelazna_dziewica` | `zniedoleznienie` |
| Typ / status | DAMAGE / NEEDS_VERIFICATION | SUPPORT / NEEDS_VERIFICATION |
| Tagi | CURSE, PROFANE, PHYSICAL | CURSE, PROFANE |
| Esencja | koszt 10; brak generowania | koszt 10; brak generowania |
| Lucky Hit | 5% | null |
| Cooldown / ładunki / wymagania | null / null / puste | null / null / puste |
| Prosta tabela / główne procenty | pusta / null, null | pusta / null, null |
| Komponenty | PERIODIC_DAMAGE 30/77 co 2 s; EVENT_TRIGGERED_DAMAGE 30/77 na zdarzenie | pusta tabela |
| Duration komponentów | null dla obu; 30 s to czas klątwy w notes | brak komponentów; 30 s w notes |
| Utility | przeklina obszar | Weaken; Slow R1 40% / max-view 56% w notes |
| SSR obrażeń | dwa komponenty osobno | „nie dotyczy” |

Tabele obrażeń zawierają tylko `1` i `15`; rangi 2..14 pozostają nieznane.
Nie sumujemy mechanizmów, ticków, zdarzeń ani nie zapisujemy fałszywych 450% over 30 s.

## 6. Dokładne mapy 2+2+3

| Skill | Grupa 1 | Grupa 2 | Grupa 3 / Transformation |
| --- | --- | --- | --- |
| Żelazna Dziewica | Generowanie Esencji; Egzekucja i Umocnienie | Zajadłość i Przytłoczenie; Odsłonięcie | Złośliwa Radość; Mistrz Małodobry; Krwawa Pannica |
| Zniedołężnienie | Szansa na Trafienie Krytyczne; Redukcja Czasu Odnowienia | Szybkość Ruchu; Animusz | Oszałamiająca Klątwa; Upuszczanie Zdrowia; Świętokradczy Amok |

Golden membership obejmuje dokładnie 19 skilli: SkillUpgradeGroup, każdy UpgradeDamageModifier.upgradeGroup,
UpgradeDamageImpact.groupId, rozłączność node'ów i 12 kombinacji 2 × 2 × 3 per skill.
Grupa 3 zawiera wzajemnie wykluczające się warianty, bez łączenia ich efektów.

## 7–9. Transformation i warunkowe metadane

- **Złośliwa Radość / Schadenfreude:** THORNS_DAMAGE_MODIFIER, TEXT_ONLY, dynamiczne X Cierni i obrażenia zwiększone o 100% własnych Cierni.
  Warunek co najmniej jednego przeklętego wroga. Display NEEDS_MANUAL_REVIEW, runtime NO, impact bez procentu broni.
  Nie jest zwykłym mnożnikiem +100%[x].
- **Mistrz Małodobry / Torture Artist:** Darkness/Shadow wyłącznie wariantu, oba coefficient 30/77 pozostają.
  30% szansy na Stun 2 s przy obrażeniach klątwy. Bazowe tagi nie zmieniają się; uwzględniono poprawkę Blizzard 3.2.1.
- **Krwawa Pannica / Blood Maiden:** osobna wariantowa tabela BURST_DAMAGE 150/383,
  oddzielny DEFENSE_OR_UTILITY/HIT_COUNT „Zbiera 4 Blood Orbs”, Blood i 5% szansy na kulę jako metadane wariantu.
  Bez 4 × burst, dodawania do bazowych 30/77 i założenia dostępności kul.
- **Świętokradczy Amok / Unholy Frenzy:** dwa utility modifiers: Cast Speed i Movement Speed własnych Minions,
  R1 +20%[x] / max-view +51%[x]. Wariant zastępuje cel i efekt klątwy, działa na sługi zamiast wrogów,
  nie przenosi bazowego Weaken/Slow na wrogów. Bazowy CharacterTreeSkill pozostaje bez zmian.
- Oszałamiająca Klątwa zachowuje 3.0/4.2 s Daze; Upuszczanie Zdrowia 10/25.5% leczenia maksymalnego zdrowia,
  maksymalnie raz na sekundę i 20% szansy na Blood Orb przy śmierci. Utility bez DamagePercentRankTable/interpolacji.
- Generowanie Esencji Żelaznej Dziewicy to dwa RESOURCE_OR_COST: koszt wariantu 0 oraz +5 za nowo przeklętego wroga;
  bazowy koszt 10 i brak bazowego generation zachowane. Nie założono liczby wrogów.
  Redukcja cooldownu Zniedołężnienia jest losowa, warunkowa do 15% i dotyczy losowego aktywnego cooldownu.

## 10–11. HTTP, filtry, search i liczebność

`/ranking-obrazen?character=necromancer`: HTTP 200, dokładnie 19 wierszy:
Basic 4, Core 6, Corpse 3, Macabre 4, Curse 2.
`skillGroup=curse`, `sourceCategory=CURSE` i `tag=CURSE`: po 2 wiersze.
Grupy i kategorie filtrów mają źródłową kolejność Basic → Core → Corpse → Macabre → Curse.
Katalog zawiera 133 ulepszenia, w tym 14 Curse. SSR zachowuje 16 kolumn i istniejące profile odnowienia/wymagań.

Rzeczywiste testy HTTP sprawdzają oba komponenty i jednostki, koszt 10, Lucky Hit 5%/brak wartości,
puste cooldowny/wymagania, support „nie dotyczy”, tooltipy wariantów i manual review Cierni.
Wspólny search znajduje wszystkie 133 nazwy ulepszeń, nowe nazwy skilli i Transformation,
„2 s” oraz „30 s”. Search jest nadal wyszukiwaniem podciągu; „2 s” może dopasować również źródłowe „4.2 s”.

## 12. Walidacja

Testy ukierunkowane: **169 testów w 24 zestawach, 0 failures, 0 errors, 0 skipped**, zakończenie 07.10.2026 11:05:18 CEST.

```powershell
mvn '-Dmaven.repo.local=.m2' '-Dtest=SkillChargeDataTest,CharacterTreeSkillMetadataTest,Necromancer*Test,DamagePercent*Test,DamageRanking*Test,Paladin*AuditTest,Neutral*Test,CharacterSkillTreeRegistryTest' clean test
```

Pełne testy: **733 testy w 115 zestawach, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
Czas Maven: **22 min 52 s**; zakończenie **07.10.2026 11:34:00 CEST**.
Wynik potwierdzony sumą raportów XML Surefire. Czysta kompilacja objęła 345 plików produkcyjnych i 117 testowych.

```powershell
mvn '-Dmaven.repo.local=.m2' clean test
```

`git -c core.safecrlf=false diff --check`: poprawny.
Porównanie względem świeżego stanu przed N5 potwierdza identyczność całego bloku fabryk wcześniejszych 17 skilli
oraz wszystkich dotychczasowych helperów registry. SHA-256 potwierdza zachowanie 489 plików poza zakresem N5.
Wszystkie 513 kontrolowanych plików kodu, testów i dokumentacji zachowały sumy między startem i końcem pełnych testów.
Po pełnej regresji zaktualizowano wyłącznie wynik walidacji w tym raporcie.

## 13–15. Paladyn, runtime i ZIP

Paladyn zachowuje 24 skille, dane i adapter. Istniejące golden audyty i HTTP Paladyna przechodzą.
SkillChargeData, SkillActivationRequirement i modele cooldownu nie zostały zmienione.
Ranking wszystkich 19 skilli działa z null engine i nie ma damagePerUse, effectiveCycleSeconds ani theoreticalDps.
Wszystkie nowe modifier mają safeForRuntimeDps=NO.
HeroClass, SkillId, DamageEngine, ManualSimulationService i current build pozostają bez zmian.
Nie interpolowano rang, nie wyliczono Iron Maiden DPS ani liczby zdarzeń,
nie zamieniono Schadenfreude na Weapon Damage i nie policzono 4 × Blood Maiden burst.

**ZIP nie został utworzony**; wykonywane polecenia kończą się na fazie test, bez package/assembly.
