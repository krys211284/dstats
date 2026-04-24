package krys.web;

import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

/** Pojedyncza umiejętność jawnie przypisana do bohatera wraz z jej konfiguracją. */
public final class HeroAssignedSkill {
    private final SkillId skillId;
    private final int rank;
    private final boolean baseUpgrade;
    private final SkillUpgradeChoice choiceUpgrade;

    public HeroAssignedSkill(SkillId skillId,
                             int rank,
                             boolean baseUpgrade,
                             SkillUpgradeChoice choiceUpgrade) {
        if (skillId == null) {
            throw new IllegalArgumentException("Id przypisanej umiejętności jest wymagane.");
        }
        if (rank < 0) {
            throw new IllegalArgumentException("Ranga przypisanej umiejętności nie może być ujemna.");
        }
        if (choiceUpgrade == null) {
            throw new IllegalArgumentException("Dodatkowy modyfikator przypisanej umiejętności jest wymagany.");
        }
        this.skillId = skillId;
        this.rank = rank;
        this.baseUpgrade = baseUpgrade;
        this.choiceUpgrade = choiceUpgrade;
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

    public CurrentBuildFormData.SkillConfigFormData toFormData() {
        return new CurrentBuildFormData.SkillConfigFormData(
                Integer.toString(rank),
                baseUpgrade,
                choiceUpgrade.name()
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
        return new HeroAssignedSkill(
                skillId,
                Math.max(rank, 0),
                formData.isBaseUpgrade(),
                choiceUpgrade
        );
    }
}
