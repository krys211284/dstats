package krys.skilltree;

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
    private final DamagePercentComponentRankTable componentDamagePercentRanks;

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
        this(upgradeGroup, upgradeName, upgradeHasDamageModifier, type, value, valueKind, condition,
                createsNewDamageComponent, affectedComponent, scalesWithSkillRank, safeForRankingDisplay,
                safeForRuntimeDps, notes, DamagePercentComponentRankTable.empty());
    }

    public UpgradeDamageModifier(String upgradeGroup, String upgradeName,
                                 UpgradeDamageSafety upgradeHasDamageModifier, UpgradeDamageModifierType type,
                                 String value, UpgradeDamageValueKind valueKind, String condition,
                                 boolean createsNewDamageComponent, String affectedComponent, boolean scalesWithSkillRank,
                                 UpgradeDamageSafety safeForRankingDisplay, UpgradeDamageSafety safeForRuntimeDps,
                                 String notes, DamagePercentComponentRankTable componentDamagePercentRanks) {
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
        this.componentDamagePercentRanks = Objects.requireNonNull(componentDamagePercentRanks, "componentDamagePercentRanks");
        if (safeForRuntimeDps == UpgradeDamageSafety.YES) {
            throw new IllegalArgumentException("Modyfikator ulepszenia nie może jeszcze odblokowywać runtime DPS.");
        }
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

    /** Tabele komponentów wybranego wariantu; nie zmieniają bazowych obrażeń umiejętności. */
    public DamagePercentComponentRankTable getComponentDamagePercentRanks() {
        return componentDamagePercentRanks;
    }

    public String getRankingTooltipSourceLabel() {
        if (safeForRankingDisplay == UpgradeDamageSafety.NEEDS_MANUAL_REVIEW
                || type == UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW
                || type == UpgradeDamageModifierType.THORNS_DAMAGE_MODIFIER) {
            return "Manual review";
        }
        if (upgradeGroup.equals("efekt_bazowy")) {
            return "Umiejętność";
        }
        return "Modyfikator";
    }

    public String getRankingTooltipDescription() {
        String description = notes
                .replaceFirst("^Efekt bazowy [^:]+: ", "")
                .replaceFirst("^Lokalny Markdown [^:]+ podaje: ", "")
                .replaceFirst("^Lokalny Markdown [^ ]+ podaje ", "");
        String ownLabel = upgradeName + ": ";
        if (description.startsWith(ownLabel)) {
            return description.substring(ownLabel.length());
        }
        return description;
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

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }
}
