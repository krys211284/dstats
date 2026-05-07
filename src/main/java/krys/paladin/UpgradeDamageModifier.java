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
