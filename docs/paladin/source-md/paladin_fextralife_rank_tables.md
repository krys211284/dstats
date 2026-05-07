# Paladin - Fextralife HTML rank tables manifest

- Source ZIP: `diablo4.wiki.fextralife.com.zip`
- Source ZIP SHA-256: `7025e253dee065ea107f6fbc459fa9e5eb233877743be3f61473716313c374db`
- Scope: 24 main Paladin skill pages extracted from the local HTML archive.
- Runtime DPS: not unlocked by these tables.

## Summary

| skillId | PL | EN | Group | Rows | Status | R1 raw | R15 raw |
|---|---|---|---|---:|---|---|---|
| `wymach` | Wymach | Brandish | Basic | 15 | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` | Damage: [75%] | Damage: [191%] |
| `swiety_pocisk` | Święty Pocisk | Holy Bolt | Basic | 15 | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` | Damage: [90%] | Damage: [229%] |
| `starcie` | Starcie | Clash | Basic | 15 | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` | Damage: [115%] | Damage: [293%] |
| `natarcie` | Natarcie | Advance | Basic | 15 | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` | Damage: [105%] | Damage: [268%] |
| `blogoslawiona_tarcza` | Błogosławiona Tarcza | Blessed Shield | Core | 15 | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` | Damage: [205%] | Damage: [523%] |
| `blogoslawiony_mlot` | Błogosławiony Młot | Blessed Hammer | Core | 15 | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` | Damage: [115%] | Damage: [293%] |
| `boska_lanca` | Boska Lanca | Divine Lance | Core | 15 | `SINGLE_COMPONENT_PERCENT_BUT_MULTI_HIT_RUNTIME_NEEDS_MODEL` | Damage: [90%] | Damage: [229%] |
| `uderzenie_tarcza` | Uderzenie Tarczą | Shield Bash | Core | 15 | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` | Damage: [205%] | Damage: [523%] |
| `zapal` | Zapał | Zeal | Core | 15 | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` | Damage: [80%] Additional Strikes Damage: [20%] | Damage: [204%] Additional Strikes Damage: [51%] |
| `aura_fanatyzmu` | Aura Fanatyzmu | Fanaticism Aura | Aury | 15 | `SUPPORT_OR_NON_DAMAGE_TABLE` | Attack Speed: 5.0% Critical Strike Chance: 2.0% | Attack Speed: 7.0% Critical Strike Chance: 2.8% |
| `aura_smialosci` | Aura Śmiałości | Defiance Aura | Aury | 15 | `SUPPORT_OR_NON_DAMAGE_TABLE` | Armor: 30% All Resistances: 30% | Armor: 42% All Resistances: 42% |
| `aura_swietej_swiatlosci` | Aura Świętej Światłości | Holy Light Aura | Aury | 15 | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` | Passive Damage: [45%] Active Damage: [320%] | Passive Damage: [115%] Active Damage: [816%] |
| `szarza_z_tarcza` | Szarża z Tarczą | Shield Charge | Odwaga | 15 | `SINGLE_COMPONENT_PERCENT_BUT_TICK_OR_CHANNEL_RUNTIME_NEEDS_MODEL` | Damage: [90%] Armor Gained: 40% | Damage: [229%] Armor Gained: 56% |
| `egida` | Egida | Aegis | Odwaga | 15 | `SUPPORT_OR_NON_DAMAGE_TABLE` | Block Chance: 30% | Block Chance: 42% |
| `spadajaca_gwiazda` | Spadająca Gwiazda | Falling Star | Odwaga | 15 | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` | Landing Damage: [80%] Jump Damage: [240%] | Jump Damage: [204%] Landing Damage: [612%] |
| `mobilizacja` | Mobilizacja | Rally | Odwaga | 15 | `SUPPORT_OR_NON_DAMAGE_TABLE` | Faith Amount: 16 | Faith Amount: 30 |
| `skazanie` | Skazanie | Condemn | Sprawiedliwość | 15 | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` | Damage: [240%] | Damage: [612%] |
| `wlocznia_niebios` | Włócznia Niebios | Spear of the Heavens | Sprawiedliwość | 15 | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` | Damage: [160%] Burst Damage: [120%] | Damage: [408%] Burst Damage: [306%] |
| `konsekracja` | Konsekracja | Consecration | Sprawiedliwość | 15 | `SINGLE_COMPONENT_PERCENT_BUT_TICK_OR_CHANNEL_RUNTIME_NEEDS_MODEL` | Damage: [75%] Healing Amount: 4.0% | Damage: [191%] Healing Amount: 5.6% |
| `oczyszczenie` | Oczyszczenie | Purify | Sprawiedliwość | 15 | `SUPPORT_OR_NON_DAMAGE_TABLE` | Duration: 2 seconds | Duration: 2.8 |
| `furia_niebios` | Furia Niebios | Heaven's Fury | Moce Specjalne | 15 | `MANUAL_REVIEW_MULTI_PHASE_OR_TABLE_AMBIGUITY` | Damage: [200%] | Damage: [153%] |
| `forteca` | Forteca | Fortress | Moce Specjalne | 15 | `SUPPORT_OR_NON_DAMAGE_TABLE` | Defensive Area Duration: 8.0 seconds | Defensive Area Duration: 9.0 |
| `zenit` | Zenit | Zenith | Moce Specjalne | 15 | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` | First Strike Damage: [450%] Second Strike Damage: [400%] | First Strike Damage: [1,147%] Second Strike Damage: [1,020%] |
| `arbiter_sprawiedliwosci` | Arbiter Sprawiedliwości | Arbiter of Justice | Moce Specjalne | 15 | `MANUAL_REVIEW_HTML_PDF_MISMATCH_OR_TABLE_AMBIGUITY` | Damage: [600%] Arbiter Duration: 20.0 seconds | Damage: [530%] Arbiter Duration: 28.0 |

## Full tables

### Wymach / Brandish (`wymach`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Brandish.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Brandish` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [75%] | 75% |
| 2 | Damage: [83%] | 83% |
| 3 | Damage: [90%] | 90% |
| 4 | Damage: [97%] | 97% |
| 5 | Damage: [109%] | 109% |
| 6 | Damage: [116%] | 116% |
| 7 | Damage: [124%] | 124% |
| 8 | Damage: [131%] | 131% |
| 9 | Damage: [139%] | 139% |
| 10 | Damage: [150%] | 150% |
| 11 | Damage: [157%] | 157% |
| 12 | Damage: [165%] | 165% |
| 13 | Damage: [172%] | 172% |
| 14 | Damage: [180%] | 180% |
| 15 | Damage: [191%] | 191% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Święty Pocisk / Holy Bolt (`swiety_pocisk`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Holy Bolt.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Holy+Bolt` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [90%] | 90% |
| 2 | Damage: [99%] | 99% |
| 3 | Damage: [108%] | 108% |
| 4 | Damage: [117%] | 117% |
| 5 | Damage: [131%] | 131% |
| 6 | Damage: [139%] | 139% |
| 7 | Damage: [148%] | 148% |
| 8 | Damage: [157%] | 157% |
| 9 | Damage: [166%] | 166% |
| 10 | Damage: [180%] | 180% |
| 11 | Damage: [189%] | 189% |
| 12 | Damage: [198%] | 198% |
| 13 | Damage: [207%] | 207% |
| 14 | Damage: [216%] | 216% |
| 15 | Damage: [229%] | 229% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Starcie / Clash (`starcie`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Clash.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Clash` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [115%] | 115% |
| 2 | Damage: [126%] | 126% |
| 3 | Damage: [138%] | 138% |
| 4 | Damage: [149%] | 149% |
| 5 | Damage: [167%] | 167% |
| 6 | Damage: [178%] | 178% |
| 7 | Damage: [190%] | 190% |
| 8 | Damage: [201%] | 201% |
| 9 | Damage: [213%] | 213% |
| 10 | Damage: [230%] | 230% |
| 11 | Damage: [241%] | 241% |
| 12 | Damage: [253%] | 253% |
| 13 | Damage: [264%] | 264% |
| 14 | Damage: [276%] | 276% |
| 15 | Damage: [293%] | 293% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Natarcie / Advance (`natarcie`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Advance.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Advance` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [105%] | 105% |
| 2 | Damage: [115%] | 115% |
| 3 | Damage: [126%] | 126% |
| 4 | Damage: [136%] | 136% |
| 5 | Damage: [152%] | 152% |
| 6 | Damage: [163%] | 163% |
| 7 | Damage: [173%] | 173% |
| 8 | Damage: [184%] | 184% |
| 9 | Damage: [194%] | 194% |
| 10 | Damage: [210%] | 210% |
| 11 | Damage: [220%] | 220% |
| 12 | Damage: [231%] | 231% |
| 13 | Damage: [241%] | 241% |
| 14 | Damage: [252%] | 252% |
| 15 | Damage: [268%] | 268% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Błogosławiona Tarcza / Blessed Shield (`blogoslawiona_tarcza`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Blessed Shield.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Blessed+Shield` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [205%] | 205% |
| 2 | Damage: [226%] | 226% |
| 3 | Damage: [246%] | 246% |
| 4 | Damage: [266%] | 266% |
| 5 | Damage: [297%] | 297% |
| 6 | Damage: [318%] | 318% |
| 7 | Damage: [338%] | 338% |
| 8 | Damage: [359%] | 359% |
| 9 | Damage: [379%] | 379% |
| 10 | Damage: [410%] | 410% |
| 11 | Damage: [430%] | 430% |
| 12 | Damage: [451%] | 451% |
| 13 | Damage: [471%] | 471% |
| 14 | Damage: [492%] | 492% |
| 15 | Damage: [523%] | 523% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Błogosławiony Młot / Blessed Hammer (`blogoslawiony_mlot`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Blessed Hammer.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Blessed+Hammer` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [115%] | 115% |
| 2 | Damage: [126%] | 126% |
| 3 | Damage: [138%] | 138% |
| 4 | Damage: [149%] | 149% |
| 5 | Damage: [167%] | 167% |
| 6 | Damage: [178%] | 178% |
| 7 | Damage: [190%] | 190% |
| 8 | Damage: [201%] | 201% |
| 9 | Damage: [213%] | 213% |
| 10 | Damage: [230%] | 230% |
| 11 | Damage: [241%] | 241% |
| 12 | Damage: [253%] | 253% |
| 13 | Damage: [264%] | 264% |
| 14 | Damage: [276%] | 276% |
| 15 | Damage: [293%] | 293% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Boska Lanca / Divine Lance (`boska_lanca`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Divine Lance.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Divine+Lance` |
| Status tabeli rang | `SINGLE_COMPONENT_PERCENT_BUT_MULTI_HIT_RUNTIME_NEEDS_MODEL` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [90%] | 90% |
| 2 | Damage: [99%] | 99% |
| 3 | Damage: [108%] | 108% |
| 4 | Damage: [117%] | 117% |
| 5 | Damage: [131%] | 131% |
| 6 | Damage: [139%] | 139% |
| 7 | Damage: [148%] | 148% |
| 8 | Damage: [157%] | 157% |
| 9 | Damage: [166%] | 166% |
| 10 | Damage: [180%] | 180% |
| 11 | Damage: [189%] | 189% |
| 12 | Damage: [198%] | 198% |
| 13 | Damage: [207%] | 207% |
| 14 | Damage: [216%] | 216% |
| 15 | Damage: [229%] | 229% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Uderzenie Tarczą / Shield Bash (`uderzenie_tarcza`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Shield Bash.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Shield+Bash` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [205%] | 205% |
| 2 | Damage: [226%] | 226% |
| 3 | Damage: [246%] | 246% |
| 4 | Damage: [266%] | 266% |
| 5 | Damage: [297%] | 297% |
| 6 | Damage: [318%] | 318% |
| 7 | Damage: [338%] | 338% |
| 8 | Damage: [359%] | 359% |
| 9 | Damage: [379%] | 379% |
| 10 | Damage: [410%] | 410% |
| 11 | Damage: [430%] | 430% |
| 12 | Damage: [451%] | 451% |
| 13 | Damage: [471%] | 471% |
| 14 | Damage: [492%] | 492% |
| 15 | Damage: [523%] | 523% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Zapał / Zeal (`zapal`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Zeal.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Zeal` |
| Status tabeli rang | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` |
| Wykryte metryki z HTML | Damage, Additional Strikes Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [80%] Additional Strikes Damage: [20%] | 80%, 20% |
| 2 | Damage: [88%] Additional Strikes Damage: [22%] | 88%, 22% |
| 3 | Damage: [96%] Additional Strikes Damage: [24%] | 96%, 24% |
| 4 | Damage: [104%] Additional Strikes Damage: [26%] | 104%, 26% |
| 5 | Damage: [116%] Additional Strikes Damage: [29%] | 116%, 29% |
| 6 | Damage: [124%] Additional Strikes Damage: [31%] | 124%, 31% |
| 7 | Damage: [132%] Additional Strikes Damage: [33%] | 132%, 33% |
| 8 | Damage: [140%] Additional Strikes Damage: [35%] | 140%, 35% |
| 9 | Damage: [148%] Additional Strikes Damage: [37%] | 148%, 37% |
| 10 | Damage: [160%] Additional Strikes Damage: [40%] | 160%, 40% |
| 11 | Damage: [168%] Additional Strikes Damage: [42%] | 168%, 42% |
| 12 | Damage: [176%] Additional Strikes Damage: [44%] | 176%, 44% |
| 13 | Damage: [184%] Additional Strikes Damage: [46%] | 184%, 46% |
| 14 | Damage: [192%] Additional Strikes Damage: [48%] | 192%, 48% |
| 15 | Damage: [204%] Additional Strikes Damage: [51%] | 204%, 51% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Aura Fanatyzmu / Fanaticism Aura (`aura_fanatyzmu`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Fanaticism Aura.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Fanaticism+Aura` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Attack Speed, Critical Strike Chance |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Attack Speed: 5.0% Critical Strike Chance: 2.0% | - |
| 2 | Attack Speed: 5.2% Critical Strike Chance: 2.1% | - |
| 3 | Attack Speed: 5.4% Critical Strike Chance: 2.2% | - |
| 4 | Attack Speed: 5.6% Critical Strike Chance: 2.2% | - |
| 5 | Attack Speed: 5.8% Critical Strike Chance: 2.3% | - |
| 6 | Attack Speed: 6.0% Critical Strike Chance: 2.4% | - |
| 7 | Attack Speed: 6.1% Critical Strike Chance: 2.4% | - |
| 8 | Attack Speed: 6.3% Critical Strike Chance: 2.5% | - |
| 9 | Attack Speed: 6.4% Critical Strike Chance: 2.6% | - |
| 10 | Attack Speed: 6.5% Critical Strike Chance: 2.6% | - |
| 11 | Attack Speed: 6.6% Critical Strike Chance: 2.6% | - |
| 12 | Attack Speed: 6.7% Critical Strike Chance: 2.7% | - |
| 13 | Attack Speed: 6.8% Critical Strike Chance: 2.7% | - |
| 14 | Attack Speed: 6.9% Critical Strike Chance: 2.8% | - |
| 15 | Attack Speed: 7.0% Critical Strike Chance: 2.8% | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Aura Śmiałości / Defiance Aura (`aura_smialosci`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Defiance Aura.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Defiance+Aura` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Armor, All Resistances |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Armor: 30% All Resistances: 30% | - |
| 2 | Armor: 31% All Resistances: 31% | - |
| 3 | Armor: 32% All Resistances: 32% | - |
| 4 | Armor: 34% All Resistances: 34% | - |
| 5 | Armor: 35% All Resistances: 35% | - |
| 6 | Armor: 36% All Resistances: 36% | - |
| 7 | Armor: 37% All Resistances: 37% | - |
| 8 | Armor: 38% All Resistances: 38% | - |
| 9 | Armor: 38% All Resistances: 38% | - |
| 10 | Armor: 39% All Resistances: 39% | - |
| 11 | Armor: 40% All Resistances: 40% | - |
| 12 | Armor: 40% All Resistances: 40% | - |
| 13 | Armor: 41% All Resistances: 41% | - |
| 14 | Armor: 41% All Resistances: 41% | - |
| 15 | Armor: 42% All Resistances: 42% | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Aura Świętej Światłości / Holy Light Aura (`aura_swietej_swiatlosci`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Holy Light Aura.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Holy+Light+Aura` |
| Status tabeli rang | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` |
| Wykryte metryki z HTML | Passive Damage, Active Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Passive Damage: [45%] Active Damage: [320%] | 45%, 320% |
| 2 | Passive Damage: [50%] Active Damage: [352%] | 50%, 352% |
| 3 | Passive Damage: [54%] Active Damage: [384%] | 54%, 384% |
| 4 | Passive Damage: [58%] Active Damage: [416%] | 58%, 416% |
| 5 | Passive Damage: [65%] Active Damage: [464%] | 65%, 464% |
| 6 | Passive Damage: [70%] Active Damage: [496%] | 70%, 496% |
| 7 | Passive Damage: [74%] Active Damage: [528%] | 74%, 528% |
| 8 | Passive Damage: [79%] Active Damage: [560%] | 79%, 560% |
| 9 | Passive Damage: [83%] Active Damage: [592%] | 83%, 592% |
| 10 | Passive Damage: [90%] Active Damage: [640%] | 90%, 640% |
| 11 | Passive Damage: [94%] Active Damage: [672%] | 94%, 672% |
| 12 | Passive Damage: [99%] Active Damage: [704%] | 99%, 704% |
| 13 | Passive Damage: [103%] Active Damage: [736%] | 103%, 736% |
| 14 | Passive Damage: [108%] Active Damage: [768%] | 108%, 768% |
| 15 | Passive Damage: [115%] Active Damage: [816%] | 115%, 816% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Szarża z Tarczą / Shield Charge (`szarza_z_tarcza`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Shield Charge.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Shield+Charge` |
| Status tabeli rang | `SINGLE_COMPONENT_PERCENT_BUT_TICK_OR_CHANNEL_RUNTIME_NEEDS_MODEL` |
| Wykryte metryki z HTML | Damage, Armor Gained |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [90%] Armor Gained: 40% | 90% |
| 2 | Damage: [99%] Armor Gained: 42% | 99% |
| 3 | Damage: [108%] Armor Gained: 43% | 108% |
| 4 | Damage: [117%] Armor Gained: 45% | 117% |
| 5 | Damage: [131%] Armor Gained: 46% | 131% |
| 6 | Damage: [139%] Armor Gained: 48% | 139% |
| 7 | Damage: [148%] Armor Gained: 49% | 148% |
| 8 | Damage: [157%] Armor Gained: 50% | 157% |
| 9 | Damage: [166%] Armor Gained: 51% | 166% |
| 10 | Damage: [180%] Armor Gained: 52% | 180% |
| 11 | Damage: [189%] Armor Gained: 53% | 189% |
| 12 | Damage: [198%] Armor Gained: 54% | 198% |
| 13 | Damage: [207%] Armor Gained: 54% | 207% |
| 14 | Damage: [216%] Armor Gained: 55% | 216% |
| 15 | Damage: [229%] Armor Gained: 56% | 229% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Egida / Aegis (`egida`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Aegis.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Aegis` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Block Chance |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Block Chance: 30% | - |
| 2 | Block Chance: 31% | - |
| 3 | Block Chance: 32% | - |
| 4 | Block Chance: 34% | - |
| 5 | Block Chance: 35% | - |
| 6 | Block Chance: 36% | - |
| 7 | Block Chance: 37% | - |
| 8 | Block Chance: 38% | - |
| 9 | Block Chance: 38% | - |
| 10 | Block Chance: 39% | - |
| 11 | Block Chance: 40% | - |
| 12 | Block Chance: 40% | - |
| 13 | Block Chance: 41% | - |
| 14 | Block Chance: 41% | - |
| 15 | Block Chance: 42% | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Spadająca Gwiazda / Falling Star (`spadajaca_gwiazda`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Falling Star.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Falling+Star` |
| Status tabeli rang | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` |
| Wykryte metryki z HTML | Landing Damage, Jump Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Landing Damage: [80%] Jump Damage: [240%] | 80%, 240% |
| 2 | Landing Damage: [264%] | 264% |
| 3 | Jump Damage: [96%] Landing Damage: [288%] | 96%, 288% |
| 4 | Landing Damage: [312%] | 312% |
| 5 | Jump Damage: [116%] Landing Damage: [348%] | 116%, 348% |
| 6 | Jump Damage: [124%] Landing Damage: [372%] | 124%, 372% |
| 7 | Jump Damage: [132%] Landing Damage: [396%] | 132%, 396% |
| 8 | Landing Damage: [420%] | 420% |
| 9 | Jump Damage: [148%] Landing Damage: [444%] | 148%, 444% |
| 10 | Jump Damage: [160%] Landing Damage: [480%] | 160%, 480% |
| 11 | Jump Damage: [168%] Landing Damage: [504%] | 168%, 504% |
| 12 | Jump Damage: [176%] Landing Damage: [528%] | 176%, 528% |
| 13 | Landing Damage: [552%] | 552% |
| 14 | Jump Damage: [192%] Landing Damage: [576%] | 192%, 576% |
| 15 | Jump Damage: [204%] Landing Damage: [612%] | 204%, 612% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Mobilizacja / Rally (`mobilizacja`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Rally.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Rally` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Faith Amount |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Faith Amount: 16 | - |
| 2 | Faith Amount: 17 | - |
| 3 | Faith Amount: 18 | - |
| 4 | Faith Amount: 19 | - |
| 5 | Faith Amount: 20 | - |
| 6 | Faith Amount: 21 | - |
| 7 | Faith Amount: 22 | - |
| 8 | Faith Amount: 23 | - |
| 9 | Faith Amount: 24 | - |
| 10 | Faith Amount: 25 | - |
| 11 | Faith Amount: 26 | - |
| 12 | Faith Amount: 27 | - |
| 13 | Faith Amount: 28 | - |
| 14 | Faith Amount: 29 | - |
| 15 | Faith Amount: 30 | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Skazanie / Condemn (`skazanie`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Condemn.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Condemn` |
| Status tabeli rang | `DIRECT_SINGLE_DAMAGE_PERCENT_TABLE` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [240%] | 240% |
| 2 | Damage: [264%] | 264% |
| 3 | Damage: [288%] | 288% |
| 4 | Damage: [312%] | 312% |
| 5 | Damage: [348%] | 348% |
| 6 | Damage: [372%] | 372% |
| 7 | Damage: [396%] | 396% |
| 8 | Damage: [420%] | 420% |
| 9 | Damage: [444%] | 444% |
| 10 | Damage: [480%] | 480% |
| 11 | Damage: [504%] | 504% |
| 12 | Damage: [528%] | 528% |
| 13 | Damage: [552%] | 552% |
| 14 | Damage: [576%] | 576% |
| 15 | Damage: [612%] | 612% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Włócznia Niebios / Spear of the Heavens (`wlocznia_niebios`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Spear of the Heavens.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Spear+of+the+Heavens` |
| Status tabeli rang | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` |
| Wykryte metryki z HTML | Damage, Burst Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [160%] Burst Damage: [120%] | 160%, 120% |
| 2 | Damage: [176%] Burst Damage: [132%] | 176%, 132% |
| 3 | Damage: [192%] Burst Damage: [144%] | 192%, 144% |
| 4 | Damage: [208%] Burst Damage: [156%] | 208%, 156% |
| 5 | Damage: [232%] Burst Damage: [174%] | 232%, 174% |
| 6 | Damage: [248%] Burst Damage: [186%] | 248%, 186% |
| 7 | Damage: [264%] Burst Damage: [198%] | 264%, 198% |
| 8 | Damage: [280%] Burst Damage: [210%] | 280%, 210% |
| 9 | Damage: [296%] Burst Damage: [222%] | 296%, 222% |
| 10 | Damage: [320%] Burst Damage: [240%] | 320%, 240% |
| 11 | Damage: [336%] Burst Damage: [252%] | 336%, 252% |
| 12 | Damage: [352%] Burst Damage: [264%] | 352%, 264% |
| 13 | Damage: [368%] Burst Damage: [276%] | 368%, 276% |
| 14 | Damage: [384%] Burst Damage: [288%] | 384%, 288% |
| 15 | Damage: [408%] Burst Damage: [306%] | 408%, 306% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Konsekracja / Consecration (`konsekracja`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Consecration.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Consecration` |
| Status tabeli rang | `SINGLE_COMPONENT_PERCENT_BUT_TICK_OR_CHANNEL_RUNTIME_NEEDS_MODEL` |
| Wykryte metryki z HTML | Damage, Healing Amount |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [75%] Healing Amount: 4.0% | 75% |
| 2 | Damage: [83%] Healing Amount: 4.2% | 83% |
| 3 | Damage: [90%] Healing Amount: 4.3% | 90% |
| 4 | Damage: [97%] Healing Amount: 4.5% | 97% |
| 5 | Damage: [109%] Healing Amount: 4.6% | 109% |
| 6 | Damage: [116%] Healing Amount: 4.8% | 116% |
| 7 | Damage: [124%] Healing Amount: 4.9% | 124% |
| 8 | Damage: [131%] Healing Amount: 5.0% | 131% |
| 9 | Damage: [139%] Healing Amount: 5.1% | 139% |
| 10 | Damage: [150%] Healing Amount: 5.2% | 150% |
| 11 | Damage: [157%] Healing Amount: 5.3% | 157% |
| 12 | Damage: [165%] Healing Amount: 5.4% | 165% |
| 13 | Damage: [172%] Healing Amount: 5.4% | 172% |
| 14 | Damage: [180%] Healing Amount: 5.5% | 180% |
| 15 | Damage: [191%] Healing Amount: 5.6% | 191% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Oczyszczenie / Purify (`oczyszczenie`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Purify.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Purify` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Duration |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Duration: 2 seconds | - |
| 2 | Duration: 2.08 | - |
| 3 | Duration: 2.17 | - |
| 4 | Duration: 2.23 | - |
| 5 | Duration: 2.32 | - |
| 6 | Duration: 2.38 | - |
| 7 | Duration: 2.43 | - |
| 8 | Duration: 2.5 | - |
| 9 | Duration: 2.57 | - |
| 10 | Duration: 2.6 | - |
| 11 | Duration: 2.63 | - |
| 12 | Duration: 2.68 | - |
| 13 | Duration: 2.72 | - |
| 14 | Duration: 2.77 | - |
| 15 | Duration: 2.8 | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Furia Niebios / Heaven's Fury (`furia_niebios`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Heaven's Fury.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Heaven's+Fury` |
| Status tabeli rang | `MANUAL_REVIEW_MULTI_PHASE_OR_TABLE_AMBIGUITY` |
| Wykryte metryki z HTML | Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [200%] | 200% |
| 2 | Damage: [66%] | 66% |
| 3 | Damage: [72%] | 72% |
| 4 | Damage: [78%] | 78% |
| 5 | Damage: [87%] | 87% |
| 6 | Damage: [93%] | 93% |
| 7 | Damage: [99%] | 99% |
| 8 | Damage: [105%] | 105% |
| 9 | Damage: [111%] | 111% |
| 10 | Damage: [120%] | 120% |
| 11 | Damage: [126%] | 126% |
| 12 | Damage: [132%] | 132% |
| 13 | Damage: [138%] | 138% |
| 14 | Damage: [144%] | 144% |
| 15 | Damage: [153%] | 153% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Forteca / Fortress (`forteca`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Fortress.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Fortress` |
| Status tabeli rang | `SUPPORT_OR_NON_DAMAGE_TABLE` |
| Wykryte metryki z HTML | Defensive Area Duration |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Defensive Area Duration: 8.0 seconds | - |
| 2 | Defensive Area Duration: 8.1 | - |
| 3 | Defensive Area Duration: 8.2 | - |
| 4 | Defensive Area Duration: 8.3 | - |
| 5 | Defensive Area Duration: 8.4 | - |
| 6 | Defensive Area Duration: 8.5 | - |
| 7 | Defensive Area Duration: 8.6 | - |
| 8 | Defensive Area Duration: 8.6 | - |
| 9 | Defensive Area Duration: 8.7 | - |
| 10 | Defensive Area Duration: 8.8 | - |
| 11 | Defensive Area Duration: 8.8 | - |
| 12 | Defensive Area Duration: 8.9 | - |
| 13 | Defensive Area Duration: 8.9 | - |
| 14 | Defensive Area Duration: 8.9 | - |
| 15 | Defensive Area Duration: 9.0 | - |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Zenit / Zenith (`zenit`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Zenith.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Zenith` |
| Status tabeli rang | `MULTI_COMPONENT_TABLE_NEEDS_EXPLICIT_COMPONENT_MODEL` |
| Wykryte metryki z HTML | First Strike Damage, Second Strike Damage |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | First Strike Damage: [450%] Second Strike Damage: [400%] | 450%, 400% |
| 2 | First Strike Damage: [495%] Second Strike Damage: [440%] | 495%, 440% |
| 3 | First Strike Damage: [540%] Second Strike Damage: [480%] | 540%, 480% |
| 4 | First Strike Damage: [585%] Second Strike Damage: [520%] | 585%, 520% |
| 5 | First Strike Damage: [653%] Second Strike Damage: [580%] | 653%, 580% |
| 6 | First Strike Damage: [697%] Second Strike Damage: [620%] | 697%, 620% |
| 7 | First Strike Damage: [742%] Second Strike Damage: [660%] | 742%, 660% |
| 8 | First Strike Damage: [788%] Second Strike Damage: [700%] | 788%, 700% |
| 9 | First Strike Damage: [832%] Second Strike Damage: [740%] | 832%, 740% |
| 10 | First Strike Damage: [900%] Second Strike Damage: [800%] | 900%, 800% |
| 11 | First Strike Damage: [945%] Second Strike Damage: [840%] | 945%, 840% |
| 12 | First Strike Damage: [990%] Second Strike Damage: [880%] | 990%, 880% |
| 13 | First Strike Damage: [1,035%] Second Strike Damage: [920%] | 1035%, 920% |
| 14 | First Strike Damage: [1,080%] Second Strike Damage: [960%] | 1080%, 960% |
| 15 | First Strike Damage: [1,147%] Second Strike Damage: [1,020%] | 1147%, 1020% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.

### Arbiter Sprawiedliwości / Arbiter of Justice (`arbiter_sprawiedliwosci`)

| Pole | Wartosc |
|---|---|
| Lokalny plik HTML | `diablo4.wiki.fextralife.com/Arbiter of Justice.html` |
| URL źródłowy | `https://diablo4.wiki.fextralife.com/Arbiter+of+Justice` |
| Status tabeli rang | `MANUAL_REVIEW_HTML_PDF_MISMATCH_OR_TABLE_AMBIGUITY` |
| Wykryte metryki z HTML | Damage, Arbiter Duration |

| Ranga | Surowy wpis z tabeli Fextralife HTML | Wartość procentowa w nawiasach |
|---:|---|---|
| 1 | Damage: [600%] Arbiter Duration: 20.0 seconds | 600% |
| 2 | Damage: [660%] Arbiter Duration: 20.8 | 660% |
| 3 | Damage: [720%] Arbiter Duration: 21.6 | 720% |
| 4 | Damage: [780%] Arbiter Duration: 22.4 | 780% |
| 5 | Damage: [870%] Arbiter Duration: 23.2 | 870% |
| 6 | Damage: [930%] Arbiter Duration: 23.8 | 930% |
| 7 | Damage: [990%] Arbiter Duration: 24.4 | 990% |
| 8 | Damage: [1,050%] Arbiter Duration: 25.0 | 1050% |
| 9 | Damage: [1,110%] Arbiter Duration: 25.6 | 1110% |
| 10 | Damage: [1,200%] Arbiter Duration: 26.0 | 1200% |
| 11 | Damage: [1,260%] Arbiter Duration: 26.4 | 1260% |
| 12 | Damage: [1,320%] Arbiter Duration: 26.8 | 1320% |
| 13 | Damage: [1,380%] Arbiter Duration: 27.2 | 1380% |
| 14 | Damage: [1,440%] Arbiter Duration: 27.6 | 1440% |
| 15 | Damage: [530%] Arbiter Duration: 28.0 | 530% |

**Uwaga kontraktowa:** ta tabela jest źródłem danych rang, ale nie odblokowuje runtime DPS. Dla wielohitow, tickow, kanałów, odbić, rykoszetow i efektow warunkowych nie wolno sumowac komponentow bez osobnego testu single target.
