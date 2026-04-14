package krys.app;

import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

/** Wejście aplikacyjne dla flow „Policz aktualny build” używanego przez CLI i GUI. */
public final class CurrentBuildRequest {
    private final SkillId skillId;
    private final int rank;
    private final boolean baseUpgrade;
    private final SkillUpgradeChoice choiceUpgrade;
    private final int horizonSeconds;

    public CurrentBuildRequest(SkillId skillId,
                               int rank,
                               boolean baseUpgrade,
                               SkillUpgradeChoice choiceUpgrade,
                               int horizonSeconds) {
        if (skillId == null) {
            throw new IllegalArgumentException("Skill wejściowy jest wymagany");
        }
        if (choiceUpgrade == null) {
            throw new IllegalArgumentException("Dodatkowy modyfikator jest wymagany");
        }
        if (rank < 0 || rank > 5) {
            throw new IllegalArgumentException("Rank skilla musi mieścić się w zakresie 0..5");
        }
        if (horizonSeconds <= 0) {
            throw new IllegalArgumentException("Horyzont symulacji musi być dodatni");
        }
        this.skillId = skillId;
        this.rank = rank;
        this.baseUpgrade = baseUpgrade;
        this.choiceUpgrade = choiceUpgrade;
        this.horizonSeconds = horizonSeconds;
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

    public int getHorizonSeconds() {
        return horizonSeconds;
    }
}
