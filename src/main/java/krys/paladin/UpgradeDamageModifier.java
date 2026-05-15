package krys.paladin;

import java.util.Objects;

/** Źródłowy opis modyfikatora ulepszenia, bez sumowania i bez wpływu na runtime DPS. */
public final class UpgradeDamageModifier {
    private final String upgradeGroup;
    private final String upgradeName;
    private final UpgradeDamageSafety upgradeHasDamageModifier;
    private final UpgradeDamageModifierType type;
    private final String value;
    private final UpgradeDamageValueKind valueKind;
    private final String condition;
    private final boolean createsNewDamageComponent;
    private final String affectedComponent;
    private final boolean scalesWithSkillRank;
    private final UpgradeDamageSafety safeForRankingDisplay;
    private final UpgradeDamageSafety safeForRuntimeDps;
    private final String notes;

    public UpgradeDamageModifier(String upgradeGroup,
                                 String upgradeName,
                                 UpgradeDamageSafety upgradeHasDamageModifier,
                                 UpgradeDamageModifierType type,
                                 String value,
                                 UpgradeDamageValueKind valueKind,
                                 String condition,
                                 boolean createsNewDamageComponent,
                                 String affectedComponent,
                                 boolean scalesWithSkillRank,
                                 UpgradeDamageSafety safeForRankingDisplay,
                                 UpgradeDamageSafety safeForRuntimeDps,
                                 String notes) {
        this.upgradeGroup = requireText(upgradeGroup, "upgradeGroup");
        this.upgradeName = requireText(upgradeName, "upgradeName");
        this.upgradeHasDamageModifier = Objects.requireNonNull(upgradeHasDamageModifier, "upgradeHasDamageModifier");
        this.type = Objects.requireNonNull(type, "type");
        this.value = requireText(value, "value");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        this.condition = requireText(condition, "condition");
        this.createsNewDamageComponent = createsNewDamageComponent;
        this.affectedComponent = requireText(affectedComponent, "affectedComponent");
        this.scalesWithSkillRank = scalesWithSkillRank;
        this.safeForRankingDisplay = Objects.requireNonNull(safeForRankingDisplay, "safeForRankingDisplay");
        this.safeForRuntimeDps = Objects.requireNonNull(safeForRuntimeDps, "safeForRuntimeDps");
        this.notes = requireText(notes, "notes");
        if (safeForRuntimeDps == UpgradeDamageSafety.YES) {
            throw new IllegalArgumentException("Modyfikator ulepszenia nie może jeszcze odblokowywać runtime DPS.");
        }
    }

    public static UpgradeDamageModifier fromUpgrade(String groupId, PaladinSkillUpgrade upgrade) {
        return fromUpgrade("unknown", groupId, upgrade);
    }

    public static UpgradeDamageModifier fromUpgrade(String skillId, String groupId, PaladinSkillUpgrade upgrade) {
        String normalizedSkillId = requireText(skillId, "skillId");
        Objects.requireNonNull(upgrade, "upgrade");
        String upgradeId = upgrade.getId();

        if (normalizedSkillId.equals("wymach")) {
            UpgradeDamageModifier brandishModifier = brandishModifier(groupId, upgrade);
            if (brandishModifier != null) {
                return brandishModifier;
            }
        }
        if (normalizedSkillId.equals("starcie")) {
            UpgradeDamageModifier clashModifier = clashModifier(groupId, upgrade);
            if (clashModifier != null) {
                return clashModifier;
            }
        }
        return classifyGeneric(groupId, upgrade);
    }

    private static UpgradeDamageModifier brandishModifier(String groupId, PaladinSkillUpgrade upgrade) {
        return switch (upgrade.getId()) {
            case "generowanie_wiary" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.NO,
                    UpgradeDamageModifierType.RESOURCE_OR_COST,
                    "5 Faith",
                    UpgradeDamageValueKind.TEXT_ONLY,
                    "po wyborze ulepszenia",
                    false,
                    "none",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Źródło Brandish podaje dodatkowe generowanie wiary; to nie jest bezpośredni damage.");
            case "zwiekszenie_obrazen" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.YES,
                    UpgradeDamageModifierType.MULTIPLICATIVE_DAMAGE_PERCENT,
                    "20%[X]",
                    UpgradeDamageValueKind.PERCENT_X,
                    "po wyborze ulepszenia",
                    false,
                    "PRIMARY_DAMAGE",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Dla Wymachu lokalny Markdown podaje obrażenia zwiększone o 20%[x]. Dla innych skilli wymaga potwierdzenia mechaniki runtime.");
            case "szybkosc_uzycia", "zwiekszenie_szybkosci_uzycia" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.NO,
                    UpgradeDamageModifierType.CAST_SPEED_OR_COOLDOWN,
                    "20%[+]",
                    UpgradeDamageValueKind.PERCENT_PLUS,
                    "po wyborze ulepszenia",
                    false,
                    "none",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Szybkość użycia może wpływać pośrednio na rotację, ale nie jest bezpośrednim procentem obrażeń.");
            case "odsloniecie" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.YES,
                    UpgradeDamageModifierType.STATUS_DAMAGE_ENABLER,
                    "20%",
                    UpgradeDamageValueKind.PERCENT_PLUS,
                    "Odsłonięci wrogowie przez 4 sek.",
                    false,
                    "status",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Status zwiększa potencjalne obrażenia pośrednio; runtime wymaga osobnego kontraktu.");
            case "powracajaca_swiatlosc" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.YES,
                    UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT,
                    "52%",
                    UpgradeDamageValueKind.COMPONENT_PERCENT,
                    "powrót światłości do gracza",
                    true,
                    "RETURN_DAMAGE",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Lokalny Markdown Wymachu podaje powrotny komponent 52%.");
            case "miecz_mistrzostwa" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.YES,
                    UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT,
                    "128%",
                    UpgradeDamageValueKind.COMPONENT_PERCENT,
                    "gdy postać ma więcej niż 80% zdrowia",
                    false,
                    "PRIMARY_DAMAGE",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Lokalny Markdown Wymachu podaje warunkowe 128% i szybsze przemieszczenie.");
            case "krzyzowe_uderzenie" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.YES,
                    UpgradeDamageModifierType.ADDITIONAL_HIT_OR_STRIKE,
                    "2 dodatkowe łuki; 120%",
                    UpgradeDamageValueKind.COMPONENT_PERCENT,
                    "dodatkowe łuki nie są automatycznie dodatkowymi trafieniami single target",
                    true,
                    "ADDITIONAL_ARC_DAMAGE",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Lokalny Markdown Wymachu podaje 2 dodatkowe łuki po 120%; bez sumowania w single target.");
            default -> null;
        };
    }

    private static UpgradeDamageModifier clashModifier(String groupId, PaladinSkillUpgrade upgrade) {
        return switch (upgrade.getId()) {
            case "generowanie_wiary" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.NO,
                    UpgradeDamageModifierType.RESOURCE_OR_COST,
                    "+10 Faith",
                    UpgradeDamageValueKind.TEXT_ONLY,
                    "po wyborze ulepszenia",
                    false,
                    "none",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Lokalny Markdown Starcia podaje: Starcie generuje dodatkowe 10 pkt. wiary.");
            case "animusz" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.NO,
                    UpgradeDamageModifierType.DEFENSE_OR_UTILITY,
                    "2 kumulacje; 25%[+]",
                    UpgradeDamageValueKind.TEXT_ONLY,
                    "po trafieniu wroga Starciem; limit 8 ładunków",
                    false,
                    "ANIMUSZ",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Animusz: uderzenie wroga Starciem zapewnia 2 kumulacje Animuszu. Animusz zwiększa pancerz o 25%[+], obrażenia bezpośrednie wyczerpują ładunek, limit 8 ładunków.");
            case "skutecznosc_marszu_krzyzowca" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.NO,
                    UpgradeDamageModifierType.DEFENSE_OR_UTILITY,
                    "25%[X]",
                    UpgradeDamageValueKind.PERCENT_X,
                    "wzmacnia Marsz Krzyżowca",
                    false,
                    "MARSZ_KRZYZOWCA",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Skuteczność Marszu Krzyżowca: skuteczność Marszu Krzyżowca zwiększona o 25%[x]. To efekt utility/defense, nie DPS runtime.");
            case "zwiekszenie_obrazen" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.YES,
                    UpgradeDamageModifierType.MULTIPLICATIVE_DAMAGE_PERCENT,
                    "20%[X]",
                    UpgradeDamageValueKind.PERCENT_X,
                    "po wyborze ulepszenia",
                    false,
                    "PRIMARY_DAMAGE",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Lokalny Markdown Starcia podaje obrażenia zwiększone o 20%[x]. Wartość jest tylko prezentacyjna i nie odblokowuje runtime DPS.");
            case "brac_ich" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.YES,
                    UpgradeDamageModifierType.MULTIPLICATIVE_DAMAGE_PERCENT,
                    "8%[X]",
                    UpgradeDamageValueKind.PERCENT_X,
                    "za każdy poziom Animuszu",
                    false,
                    "PRIMARY_DAMAGE",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Brać Ich: Animusz wzmacnia Starcie, które zadaje obrażenia zwiększone o 8%[x] za każdy poziom Animuszu. Warunkowe, nie sumowane i nie używane w runtime DPS.");
            case "potyczka" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.YES,
                    UpgradeDamageModifierType.ADDITIONAL_HIT_OR_STRIKE,
                    "155%",
                    UpgradeDamageValueKind.COMPONENT_PERCENT,
                    "dodatkowe uderzenie; Starcie staje się umiejętnością Fanatyka",
                    true,
                    "ADDITIONAL_STRIKE_DAMAGE",
                    false,
                    UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO,
                    "Potyczka: Starcie staje się umiejętnością Fanatyka i wywołuje dodatkowe uderzenie za 155%. Marsz Krzyżowca nie zapewnia już szansy na blok, tylko 10%[+] premii do szansy na trafienie krytyczne, maks. 30%[+]. Wartość nie jest sumowana z bazowymi obrażeniami.");
            case "kara" -> new UpgradeDamageModifier(
                    groupId,
                    upgrade.getName(),
                    UpgradeDamageSafety.NEEDS_MANUAL_REVIEW,
                    UpgradeDamageModifierType.THORNS_DAMAGE_MODIFIER,
                    "30%[+] Odwet; 3489 cierni; 20%[X]",
                    UpgradeDamageValueKind.NEEDS_MANUAL_REVIEW,
                    "Odwet i ciernie wymagają osobnej weryfikacji",
                    false,
                    "THORNS_RETRIBUTION",
                    false,
                    UpgradeDamageSafety.NEEDS_MANUAL_REVIEW,
                    UpgradeDamageSafety.NEEDS_MANUAL_REVIEW,
                    "Kara: 30%[+] szansy na Odwet, 3489 cierni i 20%[x] obrażeń od cierni. Pozostaje w Manual review, nie jest zwykłym direct damage.");
            default -> null;
        };
    }

    public String getUpgradeGroup() {
        return upgradeGroup;
    }

    public String getUpgradeName() {
        return upgradeName;
    }

    public UpgradeDamageSafety getUpgradeHasDamageModifier() {
        return upgradeHasDamageModifier;
    }

    public UpgradeDamageModifierType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public UpgradeDamageValueKind getValueKind() {
        return valueKind;
    }

    public String getCondition() {
        return condition;
    }

    public boolean createsNewDamageComponent() {
        return createsNewDamageComponent;
    }

    public String getAffectedComponent() {
        return affectedComponent;
    }

    public boolean scalesWithSkillRank() {
        return scalesWithSkillRank;
    }

    public UpgradeDamageSafety getSafeForRankingDisplay() {
        return safeForRankingDisplay;
    }

    public UpgradeDamageSafety getSafeForRuntimeDps() {
        return safeForRuntimeDps;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isDirectDamageModifier() {
        return switch (type) {
            case MULTIPLICATIVE_DAMAGE_PERCENT,
                    ADDITIVE_DAMAGE_PERCENT,
                    FLAT_COMPONENT_PERCENT,
                    RANK_SCALING_COMPONENT_PERCENT,
                    ADDITIONAL_HIT_OR_STRIKE,
                    DAMAGE_OVER_TIME,
                    THORNS_DAMAGE_MODIFIER -> true;
            case STATUS_DAMAGE_ENABLER,
                    CAST_SPEED_OR_COOLDOWN,
                    RESOURCE_OR_COST,
                    DEFENSE_OR_UTILITY,
                    NO_DAMAGE_IMPACT,
                    NEEDS_MANUAL_REVIEW -> false;
        };
    }

    private static UpgradeDamageModifier classifyGeneric(String groupId, PaladinSkillUpgrade upgrade) {
        String id = upgrade.getId();
        String name = upgrade.getName().toLowerCase();

        if (upgrade.getStatus() == PaladinSkillTreeStatus.UNSUPPORTED) {
            return manualReview(groupId, upgrade, "Wariant nieobsługiwany albo bez pełnej nazwy w lokalnym rejestrze.");
        }
        if (containsAny(id, name, "cierni")) {
            return new UpgradeDamageModifier(groupId, upgrade.getName(), UpgradeDamageSafety.NEEDS_MANUAL_REVIEW,
                    UpgradeDamageModifierType.THORNS_DAMAGE_MODIFIER, "wymaga weryfikacji",
                    UpgradeDamageValueKind.NEEDS_MANUAL_REVIEW, "ciernie wymagają osobnego runtime",
                    false, "THORNS", false, UpgradeDamageSafety.NEEDS_MANUAL_REVIEW,
                    UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, "Wpływ cierni na DPS wymaga osobnego kontraktu.");
        }
        if (containsAny(id, name, "obrazen", "obrażeń", "cios", "uderzenie", "pocisk", "rozdarcie", "klinga", "zemsta", "odwet", "osad", "osąd", "porazenie", "porażenie", "zadoscuczynienie", "rozgrzeszenie")) {
            return manualReview(groupId, upgrade, "Nazwa wskazuje możliwy modyfikator obrażeń albo komponent, ale brak bezpiecznej wartości w modelu tego etapu.");
        }
        if (containsAny(id, name, "odsloniecie", "odsłonięcie", "oslabienie", "osłabienie", "spowolnienie")) {
            return new UpgradeDamageModifier(groupId, upgrade.getName(), UpgradeDamageSafety.NO,
                    UpgradeDamageModifierType.STATUS_DAMAGE_ENABLER, "status",
                    UpgradeDamageValueKind.TEXT_ONLY, "po wyborze ulepszenia",
                    false, "status", false, UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO, "Status może wpływać pośrednio na DPS, ale nie jest samodzielnym damage percent.");
        }
        if (containsAny(id, name, "szybkosc", "szybkość", "odnowienia", "cooldown", "czas_dzialania", "czas działania")) {
            return new UpgradeDamageModifier(groupId, upgrade.getName(), UpgradeDamageSafety.NO,
                    UpgradeDamageModifierType.CAST_SPEED_OR_COOLDOWN, "tekst źródłowy",
                    UpgradeDamageValueKind.TEXT_ONLY, "po wyborze ulepszenia",
                    false, "none", false, UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO, "Tempo, cooldown albo czas działania nie są bezpośrednim damage percent.");
        }
        if (containsAny(id, name, "koszt", "generowanie", "zasob", "zasób", "wiary", "faith")) {
            return new UpgradeDamageModifier(groupId, upgrade.getName(), UpgradeDamageSafety.NO,
                    UpgradeDamageModifierType.RESOURCE_OR_COST, "tekst źródłowy",
                    UpgradeDamageValueKind.TEXT_ONLY, "po wyborze ulepszenia",
                    false, "none", false, UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO, "Zasób albo koszt nie jest bezpośrednim damage percent.");
        }
        if (containsAny(id, name, "blok", "zdrowia", "pancerz", "leczenie", "krzepkosc", "krzepkość", "umocnienie", "nieustepliwosc", "nieustępliwość", "barykada", "okopanie", "rozmiar", "dodatkowe_cele", "dodatkowy_ladunek", "dodatkowy ładunek", "maksimum")) {
            return new UpgradeDamageModifier(groupId, upgrade.getName(), UpgradeDamageSafety.NO,
                    UpgradeDamageModifierType.DEFENSE_OR_UTILITY, "brak bezpośredniego damage",
                    UpgradeDamageValueKind.NONE, "po wyborze ulepszenia",
                    false, "none", false, UpgradeDamageSafety.YES,
                    UpgradeDamageSafety.NO, "Efekt defensywny albo użytkowy bez bezpośredniego damage percent.");
        }
        return new UpgradeDamageModifier(groupId, upgrade.getName(), UpgradeDamageSafety.NO,
                UpgradeDamageModifierType.NO_DAMAGE_IMPACT, "brak",
                UpgradeDamageValueKind.NONE, "po wyborze ulepszenia",
                false, "none", false, UpgradeDamageSafety.YES,
                UpgradeDamageSafety.NO, "Brak rozpoznanego wpływu na obrażenia w lokalnym modelu opisowym.");
    }

    private static UpgradeDamageModifier manualReview(String groupId, PaladinSkillUpgrade upgrade, String notes) {
        return new UpgradeDamageModifier(groupId, upgrade.getName(), UpgradeDamageSafety.NEEDS_MANUAL_REVIEW,
                UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW, "wymaga weryfikacji",
                UpgradeDamageValueKind.NEEDS_MANUAL_REVIEW, "brak bezpiecznego kontraktu runtime",
                false, "needs_manual_review", false, UpgradeDamageSafety.NEEDS_MANUAL_REVIEW,
                UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, notes);
    }

    private static boolean containsAny(String id, String name, String... fragments) {
        for (String fragment : fragments) {
            if (id.contains(fragment) || name.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }
}
