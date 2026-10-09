package krys.skilltree;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Niemutowalny opis skilla dowolnej klasy; metadane i interpretację źródeł dostarcza rejestr klasy. */
public final class CharacterTreeSkill {
    private final String skillId;
    private final String skillName;
    private final String sourceReference;
    private final String skillGroup;
    private final Integer baseDamagePercentAtRank1;
    private final Integer baseDamagePercentAtTreeMaxRank;
    private final DamagePercentRankTable baseDamagePercentRanks;
    private final DamagePercentComponentRankTable componentDamagePercentRanks;
    private final SkillTreeType type;
    private final SkillTreeStatus status;
    private final List<SkillUpgradeGroup> upgradeGroups;
    private final String notes;
    private final Set<SourceSkillCategory> skillCategories;
    private final Set<String> tags;
    private final SkillResourceData resourceData;
    private final Integer luckyHitPercent;
    private final Integer cooldownSeconds;
    private final SkillChargeData chargeData;
    private final List<SkillActivationRequirement> activationRequirements;
    private final List<UpgradeDamageImpact> upgradeDamageImpacts;
    private final List<UpgradeDamageModifier> upgradeDamageModifiers;

    public CharacterTreeSkill(String skillId, String skillName, String sourceReference, String skillGroup,
                              Integer baseDamagePercentAtRank1, Integer baseDamagePercentAtTreeMaxRank,
                              SkillTreeType type, SkillTreeStatus status, List<SkillUpgradeGroup> upgradeGroups,
                              String notes) {
        this(skillId, skillName, sourceReference, skillGroup, baseDamagePercentAtRank1, baseDamagePercentAtTreeMaxRank,
                DamagePercentRankTable.empty(), DamagePercentComponentRankTable.empty(), type, status,
                upgradeGroups, notes, Set.of(), Set.of(), SkillResourceData.EMPTY, null, List.of(), List.of());
    }

    public CharacterTreeSkill(String skillId, String skillName, String sourceReference, String skillGroup,
                              Integer baseDamagePercentAtRank1, Integer baseDamagePercentAtTreeMaxRank,
                              DamagePercentRankTable baseDamagePercentRanks,
                              DamagePercentComponentRankTable componentDamagePercentRanks,
                              SkillTreeType type, SkillTreeStatus status, List<SkillUpgradeGroup> upgradeGroups,
                              String notes, Set<SourceSkillCategory> skillCategories, Set<String> tags,
                              SkillResourceData resourceData, Integer luckyHitPercent,
                              List<UpgradeDamageImpact> upgradeDamageImpacts,
                              List<UpgradeDamageModifier> upgradeDamageModifiers) {
        this(skillId, skillName, sourceReference, skillGroup, baseDamagePercentAtRank1, baseDamagePercentAtTreeMaxRank,
                baseDamagePercentRanks, componentDamagePercentRanks, type, status, upgradeGroups, notes,
                skillCategories, tags, resourceData, luckyHitPercent, upgradeDamageImpacts, upgradeDamageModifiers,
                null, List.of());
    }

    public CharacterTreeSkill(String skillId, String skillName, String sourceReference, String skillGroup,
                              Integer baseDamagePercentAtRank1, Integer baseDamagePercentAtTreeMaxRank,
                              DamagePercentRankTable baseDamagePercentRanks,
                              DamagePercentComponentRankTable componentDamagePercentRanks,
                              SkillTreeType type, SkillTreeStatus status, List<SkillUpgradeGroup> upgradeGroups,
                              String notes, Set<SourceSkillCategory> skillCategories, Set<String> tags,
                              SkillResourceData resourceData, Integer luckyHitPercent,
                              List<UpgradeDamageImpact> upgradeDamageImpacts,
                              List<UpgradeDamageModifier> upgradeDamageModifiers, Integer cooldownSeconds,
                              List<SkillActivationRequirement> activationRequirements) {
        this(skillId, skillName, sourceReference, skillGroup, baseDamagePercentAtRank1, baseDamagePercentAtTreeMaxRank,
                baseDamagePercentRanks, componentDamagePercentRanks, type, status, upgradeGroups, notes,
                skillCategories, tags, resourceData, luckyHitPercent, upgradeDamageImpacts, upgradeDamageModifiers,
                cooldownSeconds, activationRequirements, null);
    }

    public CharacterTreeSkill(String skillId, String skillName, String sourceReference, String skillGroup,
                              Integer baseDamagePercentAtRank1, Integer baseDamagePercentAtTreeMaxRank,
                              DamagePercentRankTable baseDamagePercentRanks,
                              DamagePercentComponentRankTable componentDamagePercentRanks,
                              SkillTreeType type, SkillTreeStatus status, List<SkillUpgradeGroup> upgradeGroups,
                              String notes, Set<SourceSkillCategory> skillCategories, Set<String> tags,
                              SkillResourceData resourceData, Integer luckyHitPercent,
                              List<UpgradeDamageImpact> upgradeDamageImpacts,
                              List<UpgradeDamageModifier> upgradeDamageModifiers, Integer cooldownSeconds,
                              List<SkillActivationRequirement> activationRequirements, SkillChargeData chargeData) {
        this.skillId = requireText(skillId, "skillId");
        this.skillName = requireText(skillName, "skillName");
        this.sourceReference = requireText(sourceReference, "sourceReference");
        this.skillGroup = requireText(skillGroup, "skillGroup");
        this.baseDamagePercentAtRank1 = baseDamagePercentAtRank1;
        this.baseDamagePercentAtTreeMaxRank = baseDamagePercentAtTreeMaxRank;
        this.baseDamagePercentRanks = Objects.requireNonNull(baseDamagePercentRanks, "baseDamagePercentRanks");
        this.componentDamagePercentRanks = Objects.requireNonNull(componentDamagePercentRanks, "componentDamagePercentRanks");
        this.type = Objects.requireNonNull(type, "type");
        this.status = Objects.requireNonNull(status, "status");
        this.upgradeGroups = List.copyOf(upgradeGroups);
        this.notes = requireText(notes, "notes");
        this.skillCategories = Set.copyOf(skillCategories);
        this.tags = Set.copyOf(tags);
        this.tags.forEach(tag -> requireText(tag, "tag"));
        this.resourceData = Objects.requireNonNull(resourceData, "resourceData");
        this.luckyHitPercent = luckyHitPercent;
        if (cooldownSeconds != null && cooldownSeconds <= 0) {
            throw new IllegalArgumentException("Źródłowe odnowienie musi być dodatnie; brak odnowienia oznacza null.");
        }
        this.cooldownSeconds = cooldownSeconds;
        if (cooldownSeconds != null && chargeData != null) {
            throw new IllegalArgumentException("Zwykłe odnowienie i odnowienie ładunków są odrębnymi profilami bazowymi.");
        }
        this.chargeData = chargeData;
        this.activationRequirements = List.copyOf(activationRequirements);
        this.upgradeDamageImpacts = List.copyOf(upgradeDamageImpacts);
        this.upgradeDamageModifiers = List.copyOf(upgradeDamageModifiers);
    }

    public String getSkillId() { return skillId; }
    public String getSkillName() { return skillName; }
    public String getSourceReference() { return sourceReference; }
    public String getSkillGroup() { return skillGroup; }
    public Integer getBaseDamagePercentAtRank1() {
        return baseDamagePercentRanks.isEmpty() ? baseDamagePercentAtRank1 : baseDamagePercentRanks.damagePercentAtRank1();
    }
    public Integer getBaseDamagePercentAtTreeMaxRank() {
        return baseDamagePercentRanks.isEmpty() ? baseDamagePercentAtTreeMaxRank
                : baseDamagePercentRanks.damagePercentAtTreeMaxRank(DamagePercentRankTable.MAX_RANK);
    }
    public DamagePercentRankTable getBaseDamagePercentRanks() { return baseDamagePercentRanks; }
    public DamagePercentComponentRankTable getComponentDamagePercentRanks() { return componentDamagePercentRanks; }
    public SkillTreeType getType() { return type; }
    public SkillTreeStatus getStatus() { return status; }
    public List<SkillUpgradeGroup> getUpgradeGroups() { return upgradeGroups; }
    public String getNotes() { return notes; }
    public Set<SourceSkillCategory> getSkillCategories() { return skillCategories; }
    public Set<String> getTags() { return tags; }
    public SkillResourceData getResourceData() { return resourceData; }
    public Integer getLuckyHitPercent() { return luckyHitPercent; }
    public Integer getCooldownSeconds() { return cooldownSeconds; }
    public SkillChargeData getChargeData() { return chargeData; }
    public List<SkillActivationRequirement> getActivationRequirements() { return activationRequirements; }
    public List<UpgradeDamageImpact> getUpgradeDamageImpacts() { return upgradeDamageImpacts; }
    public List<UpgradeDamageModifier> getUpgradeDamageModifiers() { return upgradeDamageModifiers; }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }
}
