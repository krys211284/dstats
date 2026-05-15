package krys.web;

import krys.paladin.DamagePercentComponentRankTable;
import krys.paladin.PaladinSkillTreeType;
import krys.paladin.PaladinTreeSkill;
import krys.paladin.SkillCategory;
import krys.paladin.SkillTag;
import krys.paladin.UpgradeDamageImpact;
import krys.paladin.UpgradeDamageModifier;
import krys.paladin.UpgradeDamageModifierType;
import krys.paladin.UpgradeDamageSafety;
import krys.ranking.PaladinSkillDamageRankingEntry;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Wiersz widoku rankingu łączący opis rankingu z typem umiejętności z rejestru drzewa. */
public final class DamageRankingRow {
    private static final Pattern PERCENT_VALUE_PATTERN = Pattern.compile("(\\d+)%");

    private final PaladinSkillDamageRankingEntry entry;
    private final PaladinSkillTreeType type;
    private final Set<SkillCategory> skillCategories;
    private final String damageComponentsDescription;
    private final DamagePercentComponentRankTable componentDamagePercentRanks;
    private final List<UpgradeDamageImpact> upgradeDamageImpacts;
    private final List<UpgradeDamageModifier> upgradeDamageModifiers;
    private final Set<SkillTag> tags;
    private final Integer faithCost;
    private final Integer faithGenerationBase;
    private final Integer faithGenerationBonusKnown;
    private final Integer luckyHitPercent;

    public DamageRankingRow(PaladinSkillDamageRankingEntry entry, PaladinTreeSkill treeSkill) {
        this.entry = entry;
        this.type = treeSkill.getType();
        this.skillCategories = treeSkill.getSkillCategories();
        this.damageComponentsDescription = describeDamageComponents(treeSkill);
        this.componentDamagePercentRanks = treeSkill.getComponentDamagePercentRanks();
        this.upgradeDamageImpacts = List.copyOf(treeSkill.getUpgradeDamageImpacts());
        this.upgradeDamageModifiers = List.copyOf(treeSkill.getUpgradeDamageModifiers());
        this.tags = treeSkill.getTags();
        this.faithCost = treeSkill.getFaithCost();
        this.faithGenerationBase = treeSkill.getFaithGenerationBase();
        this.faithGenerationBonusKnown = treeSkill.getFaithGenerationBonusKnown();
        this.luckyHitPercent = treeSkill.getLuckyHitPercent();
    }

    public PaladinSkillDamageRankingEntry getEntry() {
        return entry;
    }

    public PaladinSkillTreeType getType() {
        return type;
    }

    public Set<SkillCategory> getSkillCategories() {
        return skillCategories;
    }

    public String getSkillCategoriesDisplay() {
        return skillCategories.stream()
                .sorted(java.util.Comparator.comparingInt(SkillCategory::getDisplayOrder))
                .map(SkillCategory::getDisplayName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    public String getSourceCategoriesDisplay() {
        return getSkillCategoriesDisplay();
    }

    public boolean hasSkillCategory(SkillCategory category) {
        return skillCategories.contains(category);
    }

    public String getTreeGroupDisplayName() {
        return switch (entry.getSkillGroup()) {
            case "basic" -> "Podstawowe / Basic";
            case "core" -> "Główne / Core";
            case "aura" -> "Aura";
            case "odwaga" -> "Odwaga";
            case "sprawiedliwosc" -> "Sprawiedliwość";
            case "moce_specjalne" -> "Moce Specjalne";
            default -> entry.getSkillGroup();
        };
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

    public Set<SkillTag> getTags() {
        return tags;
    }

    public String getMechanicTagsDisplay() {
        return tags.stream()
                .map(Enum::name)
                .sorted()
                .collect(java.util.stream.Collectors.joining(","));
    }

    public boolean hasTag(SkillTag tag) {
        return tags.contains(tag);
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
        return faithGenerationBase != null
                || upgradeDamageModifiers.stream()
                .anyMatch(modifier -> modifier.getType() == UpgradeDamageModifierType.RESOURCE_OR_COST);
    }

    public boolean hasFaithCost() {
        return faithCost != null;
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

    public Integer maxDamageMultiplierPercent() {
        return maxPercentValue(damageMultiplierModifiers());
    }

    public List<UpgradeDamageModifier> damageBonusModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES)
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.ADDITIVE_DAMAGE_PERCENT)
                .toList();
    }

    public Integer maxDamageBonusPercent() {
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

    public Integer maxExtraHitOrComponentPercent() {
        return maxPercentValue(extraHitOrComponentModifiers());
    }

    public List<UpgradeDamageModifier> damageOverTimeModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES)
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.DAMAGE_OVER_TIME)
                .toList();
    }

    public Integer maxDamageOverTimePercent() {
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

    public String getFaithCostSummary() {
        return faithCost == null ? "-" : faithCost.toString();
    }

    public Integer getFaithCostSortValue() {
        return faithCost;
    }

    public String getFaithGenerationSummary() {
        if (faithGenerationBase == null) {
            return "-";
        }
        if (faithGenerationBonusKnown == null) {
            return faithGenerationBase.toString();
        }
        return faithGenerationBase + "; +" + faithGenerationBonusKnown + " — Generowanie Wiary";
    }

    public Integer getFaithGenerationBonusKnown() {
        return faithGenerationBonusKnown;
    }

    public Integer getLuckyHitPercent() {
        return luckyHitPercent;
    }

    public String getLuckyHitSummary() {
        return luckyHitPercent == null ? "-" : luckyHitPercent + "%";
    }

    public Integer getFaithGenerationBaseSortValue() {
        return faithGenerationBase;
    }

    public Integer getFaithGenerationMaxKnownSortValue() {
        if (faithGenerationBase == null) {
            return null;
        }
        if (faithGenerationBonusKnown == null) {
            return faithGenerationBase;
        }
        return faithGenerationBase + faithGenerationBonusKnown;
    }

    private static String describeDamageComponents(PaladinTreeSkill treeSkill) {
        if (!treeSkill.getBaseDamagePercentRanks().isEmpty()) {
            return "prosta tabela bazowych procentów obrażeń 1..15";
        }
        return switch (treeSkill.getType()) {
            case DAMAGE -> "brak prostej tabeli bazowych procentów; wymaga weryfikacji komponentów";
            case NON_DAMAGE, SUPPORT, DEFENSIVE, MOBILITY, SPECIAL, UNCLASSIFIED -> "brak prostego komponentu obrażeń w bieżącym modelu";
        };
    }

    private static Integer maxPercentValue(List<UpgradeDamageModifier> modifiers) {
        return modifiers.stream()
                .map(DamageRankingRow::firstPercentValue)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private static Integer firstPercentValue(UpgradeDamageModifier modifier) {
        Matcher matcher = PERCENT_VALUE_PATTERN.matcher(modifier.getValue());
        Integer max = null;
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            if (max == null || value > max) {
                max = value;
            }
        }
        return max;
    }

}
