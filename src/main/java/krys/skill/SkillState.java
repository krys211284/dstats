package krys.skill;

/**
 * Stan nauczonego skilla w buildzie bohatera.
 * Walidacja pilnuje kontraktu README: dodatkowy modyfikator wymaga bazowego rozszerzenia.
 */
public final class SkillState {
    public static final String NO_TREE_CHOICE = "NONE";
    public static final String CLASH_ANIMUS_CHOICE = "animusz";
    public static final String CLASH_PUNISHMENT_CHOICE = "kara";

    private final SkillId skillId;
    private final int rank;
    private final boolean baseUpgrade;
    private final SkillUpgradeChoice choiceUpgrade;
    private final SkillRuntimeModifierChoice runtimeModifierChoice;
    private final String choiceGroup1;
    private final String choiceGroup2;
    private final String choiceGroup3;

    public SkillState(SkillId skillId, int rank, boolean baseUpgrade, SkillUpgradeChoice choiceUpgrade) {
        this(skillId, rank, baseUpgrade, choiceUpgrade, SkillRuntimeModifierChoice.NONE);
    }

    public SkillState(SkillId skillId,
                      int rank,
                      boolean baseUpgrade,
                      SkillUpgradeChoice choiceUpgrade,
                      SkillRuntimeModifierChoice runtimeModifierChoice) {
        this(skillId, rank, baseUpgrade, choiceUpgrade, runtimeModifierChoice,
                NO_TREE_CHOICE,
                NO_TREE_CHOICE,
                NO_TREE_CHOICE);
    }

    public SkillState(SkillId skillId,
                      int rank,
                      boolean baseUpgrade,
                      SkillUpgradeChoice choiceUpgrade,
                      SkillRuntimeModifierChoice runtimeModifierChoice,
                      String choiceGroup1,
                      String choiceGroup2,
                      String choiceGroup3) {
        if (skillId == null) {
            throw new IllegalArgumentException("Id skilla jest wymagane");
        }
        if (choiceUpgrade == null) {
            throw new IllegalArgumentException("Dodatkowy modyfikator skilla jest wymagany");
        }
        if (runtimeModifierChoice == null) {
            throw new IllegalArgumentException("Modyfikator runtime skilla jest wymagany");
        }
        String normalizedGroup1 = normalizeTreeChoice(choiceGroup1);
        String normalizedGroup2 = normalizeTreeChoice(choiceGroup2);
        String normalizedGroup3 = normalizeTreeChoice(choiceGroup3);
        if (skillId == SkillId.CLASH && NO_TREE_CHOICE.equals(normalizedGroup1)
                && runtimeModifierChoice == SkillRuntimeModifierChoice.ANIMUS) {
            normalizedGroup1 = CLASH_ANIMUS_CHOICE;
        }
        if (skillId == SkillId.CLASH && NO_TREE_CHOICE.equals(normalizedGroup3)
                && choiceUpgrade == SkillUpgradeChoice.LEFT) {
            normalizedGroup3 = CLASH_PUNISHMENT_CHOICE;
        }
        SkillRuntimeModifierChoice effectiveRuntimeModifierChoice = CLASH_ANIMUS_CHOICE.equals(normalizedGroup1)
                ? SkillRuntimeModifierChoice.ANIMUS
                : runtimeModifierChoice;
        SkillUpgradeChoice effectiveChoiceUpgrade = skillId == SkillId.CLASH && CLASH_PUNISHMENT_CHOICE.equals(normalizedGroup3)
                ? SkillUpgradeChoice.LEFT
                : choiceUpgrade;
        if (rank < 0 || rank > 5) {
            throw new IllegalArgumentException("Rank skilla musi mieścić się w zakresie 0..5");
        }
        if (rank == 0 && (baseUpgrade || effectiveChoiceUpgrade != SkillUpgradeChoice.NONE
                || effectiveRuntimeModifierChoice != SkillRuntimeModifierChoice.NONE
                || !NO_TREE_CHOICE.equals(normalizedGroup1)
                || !NO_TREE_CHOICE.equals(normalizedGroup2)
                || !NO_TREE_CHOICE.equals(normalizedGroup3))) {
            throw new IllegalArgumentException("Skill przy rank 0 nie może mieć upgrade'ów");
        }
        if (!baseUpgrade && effectiveChoiceUpgrade != SkillUpgradeChoice.NONE) {
            throw new IllegalArgumentException("Dodatkowy modyfikator wymaga bazowego rozszerzenia");
        }
        if (skillId != SkillId.CLASH && effectiveRuntimeModifierChoice != SkillRuntimeModifierChoice.NONE) {
            throw new IllegalArgumentException("Ten modyfikator runtime jest dostępny tylko dla Starcia");
        }
        if (skillId != SkillId.CLASH && (!NO_TREE_CHOICE.equals(normalizedGroup1)
                || !NO_TREE_CHOICE.equals(normalizedGroup2)
                || !NO_TREE_CHOICE.equals(normalizedGroup3))) {
            throw new IllegalArgumentException("Wybory grupowe są dostępne tylko dla Starcia");
        }
        this.skillId = skillId;
        this.rank = rank;
        this.baseUpgrade = baseUpgrade;
        this.choiceUpgrade = effectiveChoiceUpgrade;
        this.runtimeModifierChoice = effectiveRuntimeModifierChoice;
        this.choiceGroup1 = normalizedGroup1;
        this.choiceGroup2 = normalizedGroup2;
        this.choiceGroup3 = normalizedGroup3;
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

    private static String normalizeTreeChoice(String rawChoice) {
        if (rawChoice == null || rawChoice.isBlank() || NO_TREE_CHOICE.equalsIgnoreCase(rawChoice)) {
            return NO_TREE_CHOICE;
        }
        return rawChoice;
    }
}
