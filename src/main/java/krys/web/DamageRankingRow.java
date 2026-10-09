package krys.web;

import krys.ranking.SkillDamageRankingEntry;
import krys.skilltree.CharacterTreeSkill;
import krys.skilltree.SkillChargeData;
import krys.skilltree.SkillActivationRequirement;
import krys.skilltree.SkillActivationConsumptionMode;
import krys.skilltree.DamagePercentComponentRankTable;
import krys.skilltree.SkillResourceType;
import krys.skilltree.SkillResourceData;
import krys.skilltree.SkillResourceGenerationFrequency;
import krys.skilltree.SkillTreeType;
import krys.skilltree.SourceSkillCategory;
import krys.skilltree.UpgradeDamageImpact;
import krys.skilltree.UpgradeDamageModifier;
import krys.skilltree.UpgradeDamageModifierType;
import krys.skilltree.UpgradeDamageSafety;

import java.util.List;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

/** Wiersz widoku rankingu łączący opis rankingu z typem umiejętności z rejestru drzewa. */
public final class DamageRankingRow {
    private static final Pattern PERCENT_VALUE_PATTERN = Pattern.compile("(?<![\\d.,])(\\d+(?:[.,]\\d+)?)\\s*%");

    private final SkillDamageRankingEntry entry;
    private final SkillTreeType type;
    private final Set<SourceSkillCategory> skillCategories;
    private final String damageComponentsDescription;
    private final DamagePercentComponentRankTable componentDamagePercentRanks;
    private final List<UpgradeDamageImpact> upgradeDamageImpacts;
    private final List<UpgradeDamageModifier> upgradeDamageModifiers;
    private final Set<String> tags;
    private final SkillResourceData resourceData;
    private final Integer luckyHitPercent;
    private final SkillResourceType resourceType;
    private final String treeGroupDisplayName;
    private final List<SkillActivationRequirement> activationRequirements;
    private final SkillChargeData chargeData;

    public DamageRankingRow(SkillDamageRankingEntry entry, CharacterTreeSkill treeSkill,
                            SkillResourceType resourceType, String treeGroupDisplayName) {
        this.entry = entry;
        this.activationRequirements = treeSkill.getActivationRequirements();
        this.chargeData = treeSkill.getChargeData();
        this.type = treeSkill.getType();
        this.skillCategories = treeSkill.getSkillCategories();
        this.damageComponentsDescription = describeDamageComponents(treeSkill);
        this.componentDamagePercentRanks = treeSkill.getComponentDamagePercentRanks();
        this.upgradeDamageImpacts = List.copyOf(treeSkill.getUpgradeDamageImpacts());
        this.upgradeDamageModifiers = List.copyOf(treeSkill.getUpgradeDamageModifiers());
        this.tags = treeSkill.getTags();
        this.resourceData = treeSkill.getResourceData();
        this.luckyHitPercent = treeSkill.getLuckyHitPercent();
        this.resourceType = resourceType;
        this.treeGroupDisplayName = treeGroupDisplayName;
    }

    public SkillDamageRankingEntry getEntry() {
        return entry;
    }

    public SkillTreeType getType() {
        return type;
    }

    public Set<SourceSkillCategory> getSkillCategories() {
        return skillCategories;
    }

    public String getSkillCategoriesDisplay() {
        return skillCategories.stream()
                .sorted(java.util.Comparator.comparingInt(SourceSkillCategory::getDisplayOrder))
                .map(SourceSkillCategory::getDisplayName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    public String getSourceCategoriesDisplay() {
        return getSkillCategoriesDisplay();
    }

    public boolean hasSkillCategory(String category) {
        return skillCategories.stream().anyMatch(value -> value.getId().equalsIgnoreCase(category));
    }

    public String getTreeGroupDisplayName() {
        return treeGroupDisplayName;
    }

    public SkillResourceType getResourceType() {
        return resourceType;
    }

    public String getDamageComponentsDescription() {
        return damageComponentsDescription;
    }

    public DamagePercentComponentRankTable getComponentDamagePercentRanks() {
        return componentDamagePercentRanks;
    }

    public List<UpgradeDamageImpact> getUpgradeDamageImpactsForGroup(String groupId) {
        return upgradeDamageImpacts.stream()
                .filter(impact -> impact.getGroupId().equals(groupId))
                .toList();
    }

    public List<UpgradeDamageModifier> getUpgradeDamageModifiersForGroup(String groupId) {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getUpgradeGroup().equals(groupId))
                .toList();
    }

    public Set<String> getTags() {
        return tags;
    }

    public String getMechanicTagsDisplay() {
        return tags.stream()
                .sorted()
                .collect(java.util.stream.Collectors.joining(","));
    }

    public boolean hasTag(String tag) {
        return tags.stream().anyMatch(value -> value.equalsIgnoreCase(tag));
    }

    public Integer getBaseDamagePercentAtRank1() {
        return entry.getBaseDamagePercentAtRank1();
    }

    public Integer getBaseDamagePercentAtTreeMaxRank() {
        return entry.getBaseDamagePercentAtTreeMaxRank();
    }

    public boolean isDpsCalculable() {
        return entry.getDamagePerUse() != null
                || entry.getTheoreticalDps() != null;
    }

    public Double getSingleTargetDps() {
        return entry.getTheoreticalDps();
    }

    public String getDamageProfile() {
        if (getBaseDamagePercentAtRank1() != null || getBaseDamagePercentAtTreeMaxRank() != null) {
            return "SIMPLE";
        }
        if (!componentDamagePercentRanks.isEmpty()) {
            return "COMPONENT";
        }
        return switch (entry.getVerificationStatus()) {
            case NON_DAMAGE -> "NON_DAMAGE";
            case NEEDS_VERIFICATION, PARTIAL -> "NEEDS_REVIEW";
            case UNSUPPORTED -> "UNSUPPORTED";
            case SUPPORTED -> "SUPPORTED";
        };
    }

    public boolean hasDirectUpgradeDamage() {
        return upgradeDamageModifiers.stream().anyMatch(modifier ->
                modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES
                        && (modifier.getType() == UpgradeDamageModifierType.MULTIPLICATIVE_DAMAGE_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.ADDITIVE_DAMAGE_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.RANK_SCALING_COMPONENT_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.ADDITIONAL_HIT_OR_STRIKE
                        || modifier.getType() == UpgradeDamageModifierType.DAMAGE_OVER_TIME));
    }

    public boolean hasNewDamageComponent() {
        return upgradeDamageModifiers.stream()
                .anyMatch(UpgradeDamageModifier::createsNewDamageComponent);
    }

    public boolean hasStatusDamageEnabler() {
        return upgradeDamageModifiers.stream()
                .anyMatch(modifier -> modifier.getType() == UpgradeDamageModifierType.STATUS_DAMAGE_ENABLER);
    }

    public boolean hasResourceGeneration() {
        return resourceData.hasGeneration()
                || upgradeDamageModifiers.stream()
                .anyMatch(modifier -> modifier.getType() == UpgradeDamageModifierType.RESOURCE_OR_COST);
    }

    public boolean hasResourceCost() {
        return resourceData.cost() != null;
    }

    public boolean hasCooldownOrCastSpeed() {
        return upgradeDamageModifiers.stream()
                .anyMatch(modifier -> modifier.getType() == UpgradeDamageModifierType.CAST_SPEED_OR_COOLDOWN);
    }

    public boolean hasDefenseOrUtility() {
        return upgradeDamageModifiers.stream()
                .anyMatch(modifier -> modifier.getType() == UpgradeDamageModifierType.DEFENSE_OR_UTILITY
                        && modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES);
    }

    public boolean hasManualReviewUpgrade() {
        return upgradeDamageModifiers.stream()
                .anyMatch(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.NEEDS_MANUAL_REVIEW
                        || modifier.getType() == UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW
                        || modifier.getType() == UpgradeDamageModifierType.THORNS_DAMAGE_MODIFIER);
    }

    public List<UpgradeDamageModifier> damageMultiplierModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES)
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.MULTIPLICATIVE_DAMAGE_PERCENT)
                .toList();
    }

    public BigDecimal maxDamageMultiplierPercent() {
        return maxPercentValue(damageMultiplierModifiers());
    }

    public List<UpgradeDamageModifier> damageBonusModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES)
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.ADDITIVE_DAMAGE_PERCENT)
                .toList();
    }

    public BigDecimal maxDamageBonusPercent() {
        return maxPercentValue(damageBonusModifiers());
    }

    public List<UpgradeDamageModifier> extraHitOrComponentModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES)
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.RANK_SCALING_COMPONENT_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.ADDITIONAL_HIT_OR_STRIKE)
                .toList();
    }

    public BigDecimal maxExtraHitOrComponentPercent() {
        return maxPercentValue(extraHitOrComponentModifiers());
    }

    public List<UpgradeDamageModifier> damageOverTimeModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES)
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.DAMAGE_OVER_TIME)
                .toList();
    }

    public BigDecimal maxDamageOverTimePercent() {
        return maxPercentValue(damageOverTimeModifiers());
    }

    public List<UpgradeDamageModifier> statusDamageModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.STATUS_DAMAGE_ENABLER)
                .toList();
    }

    public List<UpgradeDamageModifier> resourceModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.RESOURCE_OR_COST)
                .toList();
    }

    public List<UpgradeDamageModifier> cooldownOrCastSpeedModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.CAST_SPEED_OR_COOLDOWN)
                .toList();
    }

    public List<UpgradeDamageModifier> defenseOrUtilityModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.DEFENSE_OR_UTILITY)
                .filter(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES)
                .toList();
    }

    public List<UpgradeDamageModifier> manualReviewModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.NEEDS_MANUAL_REVIEW
                        || modifier.getType() == UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW
                        || modifier.getType() == UpgradeDamageModifierType.THORNS_DAMAGE_MODIFIER)
                .toList();
    }

    public String getResourceCostSummary() {
        return decimalText(resourceData.cost());
    }

    public BigDecimal getResourceCostSortValue() {
        return resourceData.cost();
    }

    public String getResourceGenerationBaseSummary() {
        if (!resourceData.hasGeneration()) {
            return "-";
        }
        String summary = decimalText(resourceData.generationMin());
        if (resourceData.generationMin().compareTo(resourceData.generationMax()) != 0) {
            summary += "–" + decimalText(resourceData.generationMax());
        }
        return summary + switch (resourceData.generationFrequency()) {
            case PER_USE -> "";
            case PER_SECOND -> " / sek.";
            case PER_EVENT -> " / zdarzenie";
        };
    }

    public String getResourceGenerationSummary() {
        String base = getResourceGenerationBaseSummary();
        return !resourceData.hasGeneration() || resourceData.generationBonusKnown() == null ? base
                : base + "; +" + decimalText(resourceData.generationBonusKnown()) + " — " + resourceType.generationLabel();
    }

    public BigDecimal getResourceGenerationBonusKnown() {
        return resourceData.generationBonusKnown();
    }

    public Integer getLuckyHitPercent() {
        return luckyHitPercent;
    }

    public String getLuckyHitSummary() {
        return luckyHitPercent == null ? "-" : luckyHitPercent + "%";
    }

    public String getCooldownSummary() {
        if (chargeData != null) {
            return chargeData.baseCharges() + " ładunki; " + chargeData.chargeCooldownSeconds() + " s / ładunek";
        }
        return entry.getCooldownSeconds() == null ? "-" : entry.getCooldownSeconds() + " s";
    }

    public SkillChargeData getChargeData() {
        return chargeData;
    }

    public List<SkillActivationRequirement> getActivationRequirements() {
        return activationRequirements;
    }

    public String getActivationRequirementsSummary() {
        return activationRequirements.isEmpty() ? "-" : activationRequirements.stream()
                .map(requirement -> requirement.amount() + " × " + requirement.displayName() + " ("
                        + (requirement.consumptionMode() == SkillActivationConsumptionMode.CONSUMED_ON_USE
                        ? "zużywane" : "wymagane, niezużywane") + ")")
                .collect(java.util.stream.Collectors.joining("; "));
    }

    /** Sortowanie bazowe porównuje dolne granice źródłowych zakresów, bez zmiany częstotliwości. */
    public BigDecimal getResourceGenerationBaseSortValue() {
        return resourceData.generationMin();
    }

    /** Sortowanie maxKnown porównuje górne granice wraz z potwierdzonymi dodatkami. */
    public BigDecimal getResourceGenerationMaxKnownSortValue() {
        return resourceData.generationMaxKnown();
    }

    private static String describeDamageComponents(CharacterTreeSkill treeSkill) {
        if (!treeSkill.getBaseDamagePercentRanks().isEmpty()) {
            if (treeSkill.getBaseDamagePercentRanks().asMap().size() < 15) {
                return "częściowa tabela bazowych procentów obrażeń; dostępne punkty: "
                        + treeSkill.getBaseDamagePercentRanks().asMap().keySet().stream().sorted()
                        .map(Object::toString).collect(java.util.stream.Collectors.joining(", "));
            }
            return "prosta tabela bazowych procentów obrażeń 1..15";
        }
        return switch (treeSkill.getType()) {
            case DAMAGE -> "brak prostej tabeli bazowych procentów; wymaga weryfikacji komponentów";
            case NON_DAMAGE, SUPPORT, DEFENSIVE, MOBILITY, SPECIAL, UNCLASSIFIED -> "brak prostego komponentu obrażeń w bieżącym modelu";
        };
    }

    private static BigDecimal maxPercentValue(List<UpgradeDamageModifier> modifiers) {
        return modifiers.stream()
                .map(DamageRankingRow::maxPercentInModifier)
                .filter(value -> value != null)
                .max(BigDecimal::compareTo)
                .orElse(null);
    }

    private static BigDecimal maxPercentInModifier(UpgradeDamageModifier modifier) {
        Matcher matcher = PERCENT_VALUE_PATTERN.matcher(modifier.getValue());
        BigDecimal max = null;
        while (matcher.find()) {
            BigDecimal value = new BigDecimal(matcher.group(1).replace(',', '.'));
            if (max == null || value.compareTo(max) > 0) {
                max = value;
            }
        }
        return max;
    }

    private static String decimalText(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

}
