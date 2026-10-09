package krys.paladin;

import krys.skilltree.CharacterTreeSkill;
import krys.skilltree.SkillResourceData;
import krys.skilltree.SkillResourceType;
import krys.skilltree.SkillTreeStatus;
import krys.skilltree.SkillTreeType;
import krys.skilltree.SkillUpgrade;
import krys.skilltree.SkillUpgradeGroup;
import krys.skilltree.SourceSkillCategory;

import java.util.Map;
import java.util.stream.Collectors;

/** Granica między opisami/legacy Paladyna i wspólnym kontraktem rankingu. */
public final class PaladinSkillTreeAdapter {
    public static final SkillResourceType FAITH = new SkillResourceType(
            "faith", "Koszt Wiary", "Generowanie Wiary", "pkt. wiary");
    public static final Map<String, String> TREE_GROUP_NAMES = Map.of(
            "basic", "Podstawowe / Basic", "core", "Główne / Core", "aura", "Aura",
            "odwaga", "Odwaga", "sprawiedliwosc", "Sprawiedliwość", "moce_specjalne", "Moce Specjalne");

    private PaladinSkillTreeAdapter() { }

    public static CharacterTreeSkill toCharacterSkill(PaladinTreeSkill skill) {
        return new CharacterTreeSkill(skill.getSkillId(), skill.getSkillName(), skill.getSourcePdf(), skill.getSkillGroup(),
                skill.getBaseDamagePercentAtRank1(), skill.getBaseDamagePercentAtTreeMaxRank(),
                skill.getBaseDamagePercentRanks(), skill.getComponentDamagePercentRanks(),
                SkillTreeType.valueOf(skill.getType().name()), SkillTreeStatus.valueOf(skill.getStatus().name()),
                skill.getUpgradeGroups().stream().map(group -> new SkillUpgradeGroup(group.getId(), group.getName(),
                        group.getUpgrades().stream().map(upgrade -> new SkillUpgrade(upgrade.getId(), upgrade.getName(),
                                SkillTreeStatus.valueOf(upgrade.getStatus().name()), upgrade.getSourceNote())).toList())).toList(),
                skill.getNotes(), skill.getSkillCategories().stream().map(category -> new SourceSkillCategory(
                        category.name(), category.getDisplayName(), category.getDisplayOrder())).collect(Collectors.toSet()),
                skill.getTags().stream().map(Enum::name).collect(Collectors.toSet()),
                new SkillResourceData(skill.getFaithCost(), skill.getFaithGenerationBase(), skill.getFaithGenerationBonusKnown()),
                skill.getLuckyHitPercent(), skill.getUpgradeDamageImpacts(), skill.getUpgradeDamageModifiers());
    }
}
