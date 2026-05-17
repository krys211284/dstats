package krys.web;

import krys.paladin.PaladinSkillTreeRegistry;
import krys.paladin.PaladinTreeSkill;
import krys.paladin.UpgradeDamageModifier;
import krys.paladin.UpgradeDamageModifierType;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

import java.util.List;
import java.util.Optional;

/** Łączy legacy identyfikatory runtime z opisowym katalogiem drzewa Paladyna używanym przez ranking. */
public final class HeroSkillCatalogAdapter {
    private HeroSkillCatalogAdapter() {
    }

    public static HeroAssignedSkillPresentation present(SkillId skillId) {
        return present(skillId, new CurrentBuildFormData.SkillConfigFormData("0", false, SkillUpgradeChoice.NONE.name()));
    }

    public static HeroAssignedSkillPresentation present(SkillId skillId, CurrentBuildFormData.SkillConfigFormData skillConfig) {
        Optional<PaladinTreeSkill> treeSkill = findTreeSkill(skillId);
        int currentRank = parseRank(skillConfig);
        if (treeSkill.isEmpty()) {
            return new HeroAssignedSkillPresentation(
                    skillId,
                    PaladinSkillDefs.get(skillId).getName(),
                    null,
                    currentRank,
                    null,
                    List.of(),
                    List.of(),
                    null
            );
        }
        PaladinTreeSkill skill = treeSkill.orElseThrow();
        List<UpgradeDamageModifier> modifiers = skill.getUpgradeDamageModifiers();
        boolean rankActive = currentRank > 0;
        HeroAssignedSkillPresentation.ModifierPresentation faithGenerationModifier = modifiers.stream()
                .filter(modifier -> modifier.getType() == UpgradeDamageModifierType.RESOURCE_OR_COST)
                .filter(modifier -> modifier.getUpgradeName().equals("Generowanie Wiary"))
                .findFirst()
                .map(HeroAssignedSkillPresentation.ModifierPresentation::new)
                .orElse(null);
        List<HeroAssignedSkillPresentation.ModifierPresentation> baseEffects = rankActive
                ? modifiers.stream()
                .filter(modifier -> modifier.getUpgradeGroup().equals("efekt_bazowy"))
                .map(HeroAssignedSkillPresentation.ModifierPresentation::new)
                .toList()
                : List.of();
        List<HeroAssignedSkillPresentation.ModifierPresentation> activeModifiers = rankActive
                ? activeModifiers(skillId, skillConfig, modifiers)
                : List.of();
        return new HeroAssignedSkillPresentation(
                skillId,
                skill.getSkillName(),
                skill,
                currentRank,
                damagePercentAtCurrentRank(skill, currentRank),
                activeModifiers,
                baseEffects,
                faithGenerationModifier
        );
    }

    public static String displayName(SkillId skillId) {
        return present(skillId).getDisplayName();
    }

    public static String displayName(String runtimeSkillName) {
        SkillId skillId = findRuntimeSkillId(runtimeSkillName).orElse(null);
        if (skillId == null) {
            return runtimeSkillName;
        }
        return displayName(skillId);
    }

    public static String replaceRuntimeSkillNames(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String localized = text;
        for (SkillId skillId : SkillId.values()) {
            String legacyName = PaladinSkillDefs.get(skillId).getName();
            localized = localized.replace(legacyName, displayName(skillId));
        }
        return localized;
    }

    private static Optional<PaladinTreeSkill> findTreeSkill(SkillId skillId) {
        return switch (skillId) {
            case BRANDISH -> PaladinSkillTreeRegistry.findSkill("wymach");
            case HOLY_BOLT -> PaladinSkillTreeRegistry.findSkill("swiety_pocisk");
            case CLASH -> PaladinSkillTreeRegistry.findSkill("starcie");
            case ADVANCE -> PaladinSkillTreeRegistry.findSkill("natarcie");
            default -> Optional.empty();
        };
    }

    private static Optional<SkillId> findRuntimeSkillId(String runtimeSkillName) {
        if (runtimeSkillName == null || runtimeSkillName.isBlank()) {
            return Optional.empty();
        }
        for (SkillId skillId : SkillId.values()) {
            if (skillId.name().equals(runtimeSkillName) || PaladinSkillDefs.get(skillId).getName().equals(runtimeSkillName)) {
                return Optional.of(skillId);
            }
        }
        return Optional.empty();
    }

    private static List<HeroAssignedSkillPresentation.ModifierPresentation> activeModifiers(SkillId skillId,
                                                                                             CurrentBuildFormData.SkillConfigFormData skillConfig,
                                                                                             List<UpgradeDamageModifier> modifiers) {
        return switch (skillId) {
            case CLASH -> activeClashModifiers(skillConfig, modifiers);
            default -> List.of();
        };
    }

    private static List<HeroAssignedSkillPresentation.ModifierPresentation> activeClashModifiers(CurrentBuildFormData.SkillConfigFormData skillConfig,
                                                                                                  List<UpgradeDamageModifier> modifiers) {
        SkillUpgradeChoice choiceUpgrade = parseChoice(skillConfig);
        if (choiceUpgrade == SkillUpgradeChoice.LEFT) {
            return modifiers.stream()
                    .filter(modifier -> modifier.getUpgradeName().equals("Kara"))
                    .map(HeroAssignedSkillPresentation.ModifierPresentation::new)
                    .toList();
        }
        return List.of();
    }

    private static Integer damagePercentAtCurrentRank(PaladinTreeSkill skill, int currentRank) {
        if (currentRank <= 0) {
            return null;
        }
        try {
            return skill.damagePercentAtRank(currentRank);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static int parseRank(CurrentBuildFormData.SkillConfigFormData skillConfig) {
        if (skillConfig == null || skillConfig.getRank() == null || skillConfig.getRank().isBlank()) {
            return 0;
        }
        try {
            return Math.max(Integer.parseInt(skillConfig.getRank()), 0);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static SkillUpgradeChoice parseChoice(CurrentBuildFormData.SkillConfigFormData skillConfig) {
        try {
            return SkillUpgradeChoice.valueOf(skillConfig.getChoiceUpgrade());
        } catch (IllegalArgumentException | NullPointerException exception) {
            return SkillUpgradeChoice.NONE;
        }
    }
}
