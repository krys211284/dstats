package krys.web;

import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;

/** Pojedyncza umiejętność jawnie przypisana do bohatera wraz z jej konfiguracją. */
public final class HeroAssignedSkill {
    private final SkillId skillId;
    private final int rank;
    private final boolean baseUpgrade;
    private final SkillUpgradeChoice choiceUpgrade;
    private final SkillRuntimeModifierChoice runtimeModifierChoice;
    private final String choiceGroup1;
    private final String choiceGroup2;
    private final String choiceGroup3;

    public HeroAssignedSkill(SkillId skillId,
                             int rank,
                             boolean baseUpgrade,
                             SkillUpgradeChoice choiceUpgrade) {
        this(skillId, rank, baseUpgrade, choiceUpgrade, SkillRuntimeModifierChoice.NONE);
    }

    public HeroAssignedSkill(SkillId skillId,
                             int rank,
                             boolean baseUpgrade,
                             SkillUpgradeChoice choiceUpgrade,
                             SkillRuntimeModifierChoice runtimeModifierChoice) {
        this(skillId, rank, baseUpgrade, choiceUpgrade, runtimeModifierChoice,
                SkillState.NO_TREE_CHOICE,
                SkillState.NO_TREE_CHOICE,
                SkillState.NO_TREE_CHOICE);
    }

    public HeroAssignedSkill(SkillId skillId,
                             int rank,
                             boolean baseUpgrade,
                             SkillUpgradeChoice choiceUpgrade,
                             SkillRuntimeModifierChoice runtimeModifierChoice,
                             String choiceGroup1,
                             String choiceGroup2,
                             String choiceGroup3) {
        if (skillId == null) {
            throw new IllegalArgumentException("Id przypisanej umiejętności jest wymagane.");
        }
        if (rank < 0) {
            throw new IllegalArgumentException("Ranga przypisanej umiejętności nie może być ujemna.");
        }
        if (choiceUpgrade == null) {
            throw new IllegalArgumentException("Dodatkowy modyfikator przypisanej umiejętności jest wymagany.");
        }
        if (runtimeModifierChoice == null) {
            throw new IllegalArgumentException("Modyfikator runtime przypisanej umiejętności jest wymagany.");
        }
        this.skillId = skillId;
        this.rank = rank;
        this.baseUpgrade = baseUpgrade;
        String normalizedGroup1 = normalizeTreeChoice(choiceGroup1);
        String normalizedGroup2 = normalizeTreeChoice(choiceGroup2);
        String normalizedGroup3 = normalizeTreeChoice(choiceGroup3);
        if (skillId == SkillId.CLASH && SkillState.NO_TREE_CHOICE.equals(normalizedGroup1)
                && runtimeModifierChoice == SkillRuntimeModifierChoice.ANIMUS) {
            normalizedGroup1 = SkillState.CLASH_ANIMUS_CHOICE;
        }
        if (skillId == SkillId.CLASH && SkillState.NO_TREE_CHOICE.equals(normalizedGroup3)
                && choiceUpgrade == SkillUpgradeChoice.LEFT) {
            normalizedGroup3 = SkillState.CLASH_PUNISHMENT_CHOICE;
        }
        this.choiceGroup1 = normalizedGroup1;
        this.choiceGroup2 = normalizedGroup2;
        this.choiceGroup3 = normalizedGroup3;
        this.choiceUpgrade = skillId == SkillId.CLASH && SkillState.CLASH_PUNISHMENT_CHOICE.equals(this.choiceGroup3)
                ? SkillUpgradeChoice.LEFT
                : choiceUpgrade;
        this.runtimeModifierChoice = skillId == SkillId.CLASH && SkillState.CLASH_ANIMUS_CHOICE.equals(this.choiceGroup1)
                ? SkillRuntimeModifierChoice.ANIMUS
                : runtimeModifierChoice;
    }

    public SkillId getSkillId() {
        return skillId;
    }

    public int getRank() {
        return rank;
    }

    public boolean isBaseUpgrade() {
        return baseUpgrade;
    }

    public SkillUpgradeChoice getChoiceUpgrade() {
        return choiceUpgrade;
    }

    public SkillRuntimeModifierChoice getRuntimeModifierChoice() {
        return runtimeModifierChoice;
    }

    public String getChoiceGroup1() {
        return choiceGroup1;
    }

    public String getChoiceGroup2() {
        return choiceGroup2;
    }

    public String getChoiceGroup3() {
        return choiceGroup3;
    }

    public CurrentBuildFormData.SkillConfigFormData toFormData() {
        return new CurrentBuildFormData.SkillConfigFormData(
                Integer.toString(rank),
                baseUpgrade,
                choiceUpgrade.name(),
                runtimeModifierChoice.name(),
                choiceGroup1,
                choiceGroup2,
                choiceGroup3
        );
    }

    public boolean isLearned() {
        return rank > 0;
    }

    public static HeroAssignedSkill fromFormData(SkillId skillId, CurrentBuildFormData.SkillConfigFormData formData) {
        int rank = 0;
        if (formData.getRank() != null && !formData.getRank().isBlank()) {
            try {
                rank = Integer.parseInt(formData.getRank());
            } catch (NumberFormatException exception) {
                rank = 0;
            }
        }
        SkillUpgradeChoice choiceUpgrade;
        try {
            choiceUpgrade = SkillUpgradeChoice.valueOf(formData.getChoiceUpgrade());
        } catch (IllegalArgumentException | NullPointerException exception) {
            choiceUpgrade = SkillUpgradeChoice.NONE;
        }
        SkillRuntimeModifierChoice runtimeModifierChoice;
        try {
            runtimeModifierChoice = SkillRuntimeModifierChoice.valueOf(formData.getRuntimeModifierChoice());
        } catch (IllegalArgumentException | NullPointerException exception) {
            runtimeModifierChoice = SkillRuntimeModifierChoice.NONE;
        }
        return new HeroAssignedSkill(
                skillId,
                Math.max(rank, 0),
                formData.isBaseUpgrade(),
                choiceUpgrade,
                runtimeModifierChoice,
                formData.getChoiceGroup1(),
                formData.getChoiceGroup2(),
                formData.getChoiceGroup3()
        );
    }

    private static String normalizeTreeChoice(String rawChoice) {
        if (rawChoice == null || rawChoice.isBlank() || SkillState.NO_TREE_CHOICE.equalsIgnoreCase(rawChoice)) {
            return SkillState.NO_TREE_CHOICE;
        }
        return rawChoice;
    }
}
