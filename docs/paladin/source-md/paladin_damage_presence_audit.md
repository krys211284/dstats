# Audyt obecności obrażeń Paladyna

Ten audyt sprawdza, czy 24 umiejętności Paladyna mają jakikolwiek komponent obrażeń w bazowym skillu, prostych tabelach rang, komponentowych tabelach rang albo w ulepszeniach `grupa_1`, `grupa_2`, `grupa_3`.

Źródła audytu:
- `docs/paladin/source-md/paladin_fextralife_rank_tables.json`,
- `docs/paladin/source-md/paladin_damage_rank_table_audit.md`,
- `PaladinSkillTreeRegistry`,
- lokalne pliki Markdown w `docs/paladin/source-md/`,
- `README.md` jako kontrakt wykonawczy.

Audyt nie importuje nowych wartości liczbowych, nie zmienia rejestru, nie zmienia UI i nie odblokowuje runtime DPS.

## Wartości pól

- `baseSkillHasDamage`: `YES`, `NO`, `NEEDS_MANUAL_REVIEW`.
- `baseDamageRankTable`: `SIMPLE_FULL_1_TO_15`, `COMPONENT_FULL_1_TO_15`, `PARTIAL_OR_COMPONENT_LIMITED`, `NONE`.
- `upgradeAddsOrChangesDamage`: `YES`, `NO`, `NEEDS_MANUAL_REVIEW`.
- `upgradeDamageGroups`: `grupa_1`, `grupa_2`, `grupa_3`, `none`, `needs_manual_review`.
- `finalDamagePresence`: `HAS_BASE_DAMAGE`, `HAS_COMPONENT_DAMAGE`, `HAS_UPGRADE_DAMAGE_ONLY`, `NON_DAMAGE`, `NEEDS_MANUAL_REVIEW`.

## Tabela audytu

| skillId | polishName | englishName | skillGroup | currentRegistryClassification | baseSkillHasDamage | baseDamageRankTable | componentDamageRankTable | upgradeAddsOrChangesDamage | upgradeDamageGroups | currentMainRankingDisplay | finalDamagePresence | notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `wymach` | Wymach | Brandish | basic | SIMPLE_SINGLE_COMPONENT | YES | SIMPLE_FULL_1_TO_15 | none | YES | grupa_1; grupa_3 | single percent R1/treeMax | HAS_BASE_DAMAGE | Prosta tabela `Damage` 1..15 jest w rejestrze. Ulepszenia mogą zmieniać obrażenia, ale nie są sumowane do bazowej tabeli. |
| `swiety_pocisk` | Święty Pocisk | Holy Bolt | basic | SIMPLE_SINGLE_COMPONENT | YES | SIMPLE_FULL_1_TO_15 | none | NEEDS_MANUAL_REVIEW | grupa_1; grupa_3 | single percent R1/treeMax | HAS_BASE_DAMAGE | Prosta tabela `Damage` 1..15 jest w rejestrze. Ulepszenia pocisku/osądu wymagają osobnego modelu mechaniki. |
| `starcie` | Starcie | Clash | basic | SIMPLE_SINGLE_COMPONENT | YES | SIMPLE_FULL_1_TO_15 | none | YES | grupa_2; grupa_3 | single percent R1/treeMax | HAS_BASE_DAMAGE | Prosta tabela `Damage` 1..15 jest w rejestrze. Upgrade damage nie zmienia wartości bazowej. |
| `natarcie` | Natarcie | Advance | basic | SIMPLE_SINGLE_COMPONENT | YES | SIMPLE_FULL_1_TO_15 | none | NEEDS_MANUAL_REVIEW | grupa_2; grupa_3 | single percent R1/treeMax | HAS_BASE_DAMAGE | Prosta tabela `Damage` 1..15 jest w rejestrze. Ulepszenia mobility/damage wymagają osobnej walidacji runtime. |
| `blogoslawiona_tarcza` | Błogosławiona Tarcza | Blessed Shield | core | SIMPLE_SINGLE_COMPONENT | YES | SIMPLE_FULL_1_TO_15 | none | YES | grupa_2; grupa_3 | single percent R1/treeMax | HAS_BASE_DAMAGE | Prosta tabela `Damage` 1..15 jest w rejestrze. Komponenty ulepszeń pozostają opisowe. |
| `blogoslawiony_mlot` | Błogosławiony Młot | Blessed Hammer | core | SIMPLE_SINGLE_COMPONENT | YES | SIMPLE_FULL_1_TO_15 | none | YES | grupa_1; grupa_3 | single percent R1/treeMax | HAS_BASE_DAMAGE | Prosta tabela `Damage` 1..15 jest w rejestrze i daje R1=115 oraz R15=293. |
| `boska_lanca` | Boska Lanca | Divine Lance | core | SIMPLE_SINGLE_COMPONENT | YES | SIMPLE_FULL_1_TO_15 | none | NEEDS_MANUAL_REVIEW | grupa_1; grupa_2; grupa_3 | single percent R1/treeMax | HAS_BASE_DAMAGE | Prosta tabela `Damage` 1..15 jest w rejestrze. Wpływ ulepszeń osądu/pocisków nie jest liczony. |
| `uderzenie_tarcza` | Uderzenie Tarczą | Shield Bash | core | SIMPLE_SINGLE_COMPONENT | YES | SIMPLE_FULL_1_TO_15 | none | YES | grupa_2; grupa_3 | single percent R1/treeMax | HAS_BASE_DAMAGE | Prosta tabela `Damage` 1..15 jest w rejestrze. Ulepszenia obszarowe nie zmieniają bazowych pól R1/treeMax. |
| `zapal` | Zapał | Zeal | core | MULTI_COMPONENT | YES | COMPONENT_FULL_1_TO_15 | PRIMARY_DAMAGE; ADDITIONAL_STRIKE_DAMAGE | YES | grupa_2; grupa_3 | component list R1/treeMax | HAS_COMPONENT_DAMAGE | Ma komponentowe tabele 1..15 dla głównego i dodatkowego uderzenia. Komponenty nie są sumowane i brak prostej tabeli bazowej. |
| `aura_fanatyzmu` | Aura Fanatyzmu | Fanaticism Aura | aura | NON_DAMAGE | NO | NONE | none | NEEDS_MANUAL_REVIEW | grupa_3 | nie dotyczy | HAS_UPGRADE_DAMAGE_ONLY | Bazowa aura nie ma bezpośredniego damage percent. `Obrzęd Zemsty` sugeruje wpływ ofensywny, ale bez importu liczbowego i bez runtime DPS. |
| `aura_smialosci` | Aura Śmiałości | Defiance Aura | aura | NON_DAMAGE | NO | NONE | none | NEEDS_MANUAL_REVIEW | grupa_3 | nie dotyczy | HAS_UPGRADE_DAMAGE_ONLY | Bazowa aura jest defensywna. `Obrzęd Cierni` i `Obrzęd Mocy` wskazują obrażenia albo premię do obrażeń tylko przez ulepszenia. |
| `aura_swietej_swiatlosci` | Aura Świętej Światłości | Holy Light Aura | aura | MULTI_COMPONENT | YES | COMPONENT_FULL_1_TO_15 | PASSIVE_DAMAGE; ACTIVE_DAMAGE | YES | grupa_1; grupa_2; grupa_3 | component list R1/treeMax | HAS_COMPONENT_DAMAGE | Ma komponentowe tabele 1..15 dla passive i active damage. Komponenty pozostają rozdzielone. |
| `szarza_z_tarcza` | Szarża z Tarczą | Shield Charge | odwaga | SIMPLE_SINGLE_COMPONENT | YES | SIMPLE_FULL_1_TO_15 | none | YES | grupa_1; grupa_2; grupa_3 | single percent R1/treeMax | HAS_BASE_DAMAGE | Prosta tabela `Damage` 1..15 jest w rejestrze. Armor gained nie jest komponentem obrażeń. |
| `egida` | Egida | Aegis | odwaga | NON_DAMAGE | NO | NONE | none | NEEDS_MANUAL_REVIEW | needs_manual_review | nie dotyczy | NEEDS_MANUAL_REVIEW | Bazowo defensywna. Notatki rejestru wskazują Osąd, Odwet i ciernie, ale źródła nie pozwalają bezpiecznie przypisać prostego damage presence. |
| `spadajaca_gwiazda` | Spadająca Gwiazda | Falling Star | odwaga | MULTI_COMPONENT | YES | PARTIAL_OR_COMPONENT_LIMITED | LANDING_DAMAGE | YES | grupa_2; grupa_3 | component list R1/treeMax | HAS_COMPONENT_DAMAGE | Ma komponentową tabelę dla `LANDING_DAMAGE`. `JUMP_DAMAGE` nie jest kompletny 1..15 w lokalnym JSON-ie i nie został zaimportowany. |
| `mobilizacja` | Mobilizacja | Rally | odwaga | NON_DAMAGE | NO | NONE | none | NEEDS_MANUAL_REVIEW | grupa_1 | nie dotyczy | HAS_UPGRADE_DAMAGE_ONLY | Bazowy skill jest wsparciem i nie ma damage percent. `Szansa na Trafienie Krytyczne` może zmieniać obrażenia buildu, ale nie jest bezpośrednim DPS skilla. |
| `skazanie` | Skazanie | Condemn | sprawiedliwosc | SIMPLE_SINGLE_COMPONENT | YES | SIMPLE_FULL_1_TO_15 | none | NEEDS_MANUAL_REVIEW | grupa_3 | single percent R1/treeMax | HAS_BASE_DAMAGE | Prosta tabela `Damage` 1..15 jest w rejestrze. Typ obrażeń i mechanika runtime nadal wymagają osobnego kontraktu. |
| `wlocznia_niebios` | Włócznia Niebios | Spear of the Heavens | sprawiedliwosc | MULTI_COMPONENT | YES | COMPONENT_FULL_1_TO_15 | PRIMARY_DAMAGE; BURST_DAMAGE | YES | grupa_1; grupa_2; grupa_3 | component list R1/treeMax | HAS_COMPONENT_DAMAGE | Ma komponentowe tabele 1..15 dla primary hit i burst. Brak prostej tabeli bazowej. |
| `konsekracja` | Konsekracja | Consecration | sprawiedliwosc | SIMPLE_SINGLE_COMPONENT | YES | SIMPLE_FULL_1_TO_15 | none | NEEDS_MANUAL_REVIEW | grupa_3 | single percent R1/treeMax | HAS_BASE_DAMAGE | Prosta tabela `Damage` 1..15 jest w rejestrze. Healing amount nie jest sumowany do obrażeń. |
| `oczyszczenie` | Oczyszczenie | Purify | sprawiedliwosc | NON_DAMAGE | NO | NONE | none | NEEDS_MANUAL_REVIEW | needs_manual_review | nie dotyczy | NEEDS_MANUAL_REVIEW | Bazowo kontrola/support bez obrażeń. Modyfikatory Osądu i Rozgrzeszenia wymagają ręcznej weryfikacji przed klasyfikacją damage presence. |
| `furia_niebios` | Furia Niebios | Heaven's Fury | moce_specjalne | NEEDS_MANUAL_REVIEW | NEEDS_MANUAL_REVIEW | PARTIAL_OR_COMPONENT_LIMITED | none | NEEDS_MANUAL_REVIEW | grupa_2; grupa_3 | wymaga weryfikacji | NEEDS_MANUAL_REVIEW | Lokalny JSON wskazuje obrażenia, ale tabela jest podejrzana i nie została zaimportowana. Skill pozostaje do ręcznej weryfikacji. |
| `forteca` | Forteca | Fortress | moce_specjalne | NON_DAMAGE | NO | NONE | none | NEEDS_MANUAL_REVIEW | grupa_2; grupa_3 | nie dotyczy | HAS_UPGRADE_DAMAGE_ONLY | Bazowa Forteca jest defensywna. `Premia do Obrażeń Animuszu` i `Cierniowa Reduta` wskazują obrażenia tylko przez ulepszenia. |
| `zenit` | Zenit | Zenith | moce_specjalne | MULTI_COMPONENT | YES | COMPONENT_FULL_1_TO_15 | FIRST_STRIKE_DAMAGE; SECOND_STRIKE_DAMAGE | YES | grupa_3 | component list R1/treeMax | HAS_COMPONENT_DAMAGE | Ma komponentowe tabele 1..15 dla pierwszego i drugiego uderzenia. Komponenty nie są sumowane. |
| `arbiter_sprawiedliwosci` | Arbiter Sprawiedliwości | Arbiter of Justice | moce_specjalne | NEEDS_MANUAL_REVIEW | NEEDS_MANUAL_REVIEW | PARTIAL_OR_COMPONENT_LIMITED | none | NEEDS_MANUAL_REVIEW | grupa_2; grupa_3 | wymaga weryfikacji | NEEDS_MANUAL_REVIEW | Lokalny JSON wskazuje damage i duration, ale damage spada między rank 1 i rank 15. Brak bezpiecznego importu w tym etapie. |

## Podsumowanie

- `HAS_BASE_DAMAGE`: 11 skilli.
- `HAS_COMPONENT_DAMAGE`: 5 skilli.
- `HAS_UPGRADE_DAMAGE_ONLY`: 4 skille.
- `NON_DAMAGE`: 0 skilli.
- `NEEDS_MANUAL_REVIEW`: 4 skille.

Skille z obrażeniami tylko przez ulepszenia: `aura_fanatyzmu`, `aura_smialosci`, `mobilizacja`, `forteca`.

Skille wymagające ręcznej weryfikacji: `egida`, `oczyszczenie`, `furia_niebios`, `arbiter_sprawiedliwosci`.

Audyt nie zmienia aktualnego renderowania `/ranking-obrazen`; wpisy z `currentMainRankingDisplay = nie dotyczy` nadal tak wyglądają w głównym widoku do czasu osobnego kontraktu UI.
