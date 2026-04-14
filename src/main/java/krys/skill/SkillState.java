package krys.skill;

/**
 * Stan nauczonego skilla w buildzie bohatera.
 * Walidacja pilnuje kontraktu README: dodatkowy modyfikator wymaga bazowego rozszerzenia.
 */
public final class SkillState {
    private final SkillId skillId;
    private final int rank;
    private final boolean baseUpgrade;
    private final SkillUpgradeChoice choiceUpgrade;

    public SkillState(SkillId skillId, int rank, boolean baseUpgrade, SkillUpgradeChoice choiceUpgrade) {
        if (rank < 0 || rank > 5) {
            throw new IllegalArgumentException("Rank skilla musi mieścić się w zakresie 0..5");
        }
        if (rank == 0 && (baseUpgrade || choiceUpgrade != SkillUpgradeChoice.NONE)) {
            throw new IllegalArgumentException("Skill przy rank 0 nie może mieć upgrade'ów");
        }
        if (!baseUpgrade && choiceUpgrade != SkillUpgradeChoice.NONE) {
            throw new IllegalArgumentException("Dodatkowy modyfikator wymaga bazowego rozszerzenia");
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
}
