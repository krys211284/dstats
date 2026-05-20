package krys.web;

import krys.paladin.PaladinSkillTreeRegistry;
import krys.paladin.PaladinTreeSkill;
import krys.paladin.UpgradeDamageModifier;
import krys.paladin.UpgradeDamageModifierType;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillState;
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
        List<HeroAssignedSkillPresentation.ModifierPresentation> active = new java.util.ArrayList<>();
        SkillUpgradeChoice choiceUpgrade = parseChoice(skillConfig);
        String choiceGroup3 = skillConfig.getChoiceGroup3();
        if (choiceUpgrade == SkillUpgradeChoice.LEFT || SkillState.CLASH_PUNISHMENT_CHOICE.equals(choiceGroup3)) {
            active.addAll(modifiers.stream()
                    .filter(modifier -> modifier.getUpgradeName().equals("Kara"))
                    .map(HeroAssignedSkillPresentation.ModifierPresentation::new)
                    .toList());
        }
        addSelectedGroupModifier(active, modifiers, skillConfig.getChoiceGroup1());
        addSelectedGroupModifier(active, modifiers, skillConfig.getChoiceGroup2());
        addSelectedGroupModifier(active, modifiers, choiceGroup3);
        if (parseRuntimeModifier(skillConfig) == SkillRuntimeModifierChoice.ANIMUS
                && !SkillState.CLASH_ANIMUS_CHOICE.equals(skillConfig.getChoiceGroup1())) {
            addSelectedGroupModifier(active, modifiers, SkillState.CLASH_ANIMUS_CHOICE);
        }
        return List.copyOf(active);
    }

    private static void addSelectedGroupModifier(List<HeroAssignedSkillPresentation.ModifierPresentation> active,
                                                 List<UpgradeDamageModifier> modifiers,
                                                 String selectedUpgradeId) {
        if (selectedUpgradeId == null || selectedUpgradeId.isBlank() || SkillState.NO_TREE_CHOICE.equals(selectedUpgradeId)) {
            return;
        }
        modifiers.stream()
                .filter(modifier -> modifierId(modifier).equals(selectedUpgradeId))
                .findFirst()
                .map(HeroAssignedSkillPresentation.ModifierPresentation::new)
                .ifPresent(candidate -> {
                    boolean alreadyPresent = active.stream().anyMatch(existing -> existing.getName().equals(candidate.getName()));
                    if (!alreadyPresent) {
                        active.add(candidate);
                    }
                });
    }

    private static String modifierId(UpgradeDamageModifier modifier) {
        return modifier.getUpgradeName()
                .toLowerCase(java.util.Locale.ROOT)
                .replace("ą", "a")
                .replace("ć", "c")
                .replace("ę", "e")
                .replace("ł", "l")
                .replace("ń", "n")
                .replace("ó", "o")
                .replace("ś", "s")
                .replace("ż", "z")
                .replace("ź", "z")
                .replace(" ", "_");
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

    private static SkillRuntimeModifierChoice parseRuntimeModifier(CurrentBuildFormData.SkillConfigFormData skillConfig) {
        try {
            return SkillRuntimeModifierChoice.valueOf(skillConfig.getRuntimeModifierChoice());
        } catch (IllegalArgumentException | NullPointerException exception) {
            return SkillRuntimeModifierChoice.NONE;
        }
    }
}
