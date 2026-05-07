package krys.web;

import krys.paladin.DamagePercentComponentRankTable;
import krys.paladin.PaladinSkillTreeType;
import krys.paladin.PaladinTreeSkill;
import krys.paladin.SkillTag;
import krys.paladin.UpgradeDamageImpact;
import krys.paladin.UpgradeDamageModifier;
import krys.paladin.UpgradeDamageModifierType;
import krys.paladin.UpgradeDamageSafety;
import krys.ranking.PaladinSkillDamageRankingEntry;

import java.util.List;
import java.util.Set;

/** Wiersz widoku rankingu łączący opis rankingu z typem umiejętności z rejestru drzewa. */
public final class DamageRankingRow {
    private final PaladinSkillDamageRankingEntry entry;
    private final PaladinSkillTreeType type;
    private final String damageComponentsDescription;
    private final DamagePercentComponentRankTable componentDamagePercentRanks;
    private final List<UpgradeDamageImpact> upgradeDamageImpacts;
    private final List<UpgradeDamageModifier> upgradeDamageModifiers;
    private final Set<SkillTag> tags;

    public DamageRankingRow(PaladinSkillDamageRankingEntry entry, PaladinTreeSkill treeSkill) {
        this.entry = entry;
        this.type = treeSkill.getType();
        this.damageComponentsDescription = describeDamageComponents(treeSkill);
        this.componentDamagePercentRanks = treeSkill.getComponentDamagePercentRanks();
        this.upgradeDamageImpacts = List.copyOf(treeSkill.getUpgradeDamageImpacts());
        this.upgradeDamageModifiers = List.copyOf(treeSkill.getUpgradeDamageModifiers());
        this.tags = treeSkill.getTags();
    }

    public PaladinSkillDamageRankingEntry getEntry() {
        return entry;
    }

    public PaladinSkillTreeType getType() {
        return type;
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
                        && !modifier.createsNewDamageComponent()
                        && (modifier.getType() == UpgradeDamageModifierType.MULTIPLICATIVE_DAMAGE_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.ADDITIVE_DAMAGE_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.RANK_SCALING_COMPONENT_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT));
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
        return upgradeDamageModifiers.stream()
                .anyMatch(modifier -> modifier.getType() == UpgradeDamageModifierType.RESOURCE_OR_COST);
    }

    public boolean hasCooldownOrCastSpeed() {
        return upgradeDamageModifiers.stream()
                .anyMatch(modifier -> modifier.getType() == UpgradeDamageModifierType.CAST_SPEED_OR_COOLDOWN);
    }

    public boolean hasDefenseOrUtility() {
        return upgradeDamageModifiers.stream()
                .anyMatch(modifier -> modifier.getType() == UpgradeDamageModifierType.DEFENSE_OR_UTILITY
                        || modifier.getType() == UpgradeDamageModifierType.NO_DAMAGE_IMPACT);
    }

    public boolean hasManualReviewUpgrade() {
        return upgradeDamageModifiers.stream()
                .anyMatch(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.NEEDS_MANUAL_REVIEW
                        || modifier.getType() == UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW
                        || modifier.getType() == UpgradeDamageModifierType.THORNS_DAMAGE_MODIFIER);
    }

    public List<UpgradeDamageModifier> directUpgradeDamageModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES)
                .filter(modifier -> !modifier.createsNewDamageComponent())
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.MULTIPLICATIVE_DAMAGE_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.ADDITIVE_DAMAGE_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.RANK_SCALING_COMPONENT_PERCENT
                        || modifier.getType() == UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT)
                .toList();
    }

    public List<UpgradeDamageModifier> newDamageComponentModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(UpgradeDamageModifier::createsNewDamageComponent)
                .toList();
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
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.DEFENSE_OR_UTILITY
                        || modifier.getType() == UpgradeDamageModifierType.NO_DAMAGE_IMPACT)
                .toList();
    }

    public List<UpgradeDamageModifier> manualReviewModifiers() {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.NEEDS_MANUAL_REVIEW
                        || modifier.getType() == UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW
                        || modifier.getType() == UpgradeDamageModifierType.THORNS_DAMAGE_MODIFIER)
                .toList();
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

}
