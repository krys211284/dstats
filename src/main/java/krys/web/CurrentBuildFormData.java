package krys.web;

import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

import java.util.Map;

/** Surowe dane formularza GUI M7, zachowywane także przy błędach walidacji. */
public final class CurrentBuildFormData {
    private final String skillId;
    private final String rank;
    private final boolean baseUpgrade;
    private final String choiceUpgrade;
    private final String horizonSeconds;

    public CurrentBuildFormData(String skillId,
                                String rank,
                                boolean baseUpgrade,
                                String choiceUpgrade,
                                String horizonSeconds) {
        this.skillId = skillId;
        this.rank = rank;
        this.baseUpgrade = baseUpgrade;
        this.choiceUpgrade = choiceUpgrade;
        this.horizonSeconds = horizonSeconds;
    }

    public static CurrentBuildFormData defaultValues() {
        return new CurrentBuildFormData(
                SkillId.ADVANCE.name(),
                "5",
                true,
                SkillUpgradeChoice.RIGHT.name(),
                "10"
        );
    }

    public static CurrentBuildFormData fromFormFields(Map<String, String> fields) {
        CurrentBuildFormData defaults = defaultValues();
        return new CurrentBuildFormData(
                fields.getOrDefault("skillId", defaults.getSkillId()),
                fields.getOrDefault("rank", defaults.getRank()),
                fields.containsKey("baseUpgrade"),
                fields.getOrDefault("choiceUpgrade", defaults.getChoiceUpgrade()),
                fields.getOrDefault("horizonSeconds", defaults.getHorizonSeconds())
        );
    }

    public String getSkillId() {
        return skillId;
    }

    public String getRank() {
        return rank;
    }

    public boolean isBaseUpgrade() {
        return baseUpgrade;
    }

    public String getChoiceUpgrade() {
        return choiceUpgrade;
    }

    public String getHorizonSeconds() {
        return horizonSeconds;
    }
}
