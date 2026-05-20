package krys.web;

import krys.skill.SkillUpgradeChoice;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillId;
import krys.skill.SkillState;

import java.util.ArrayList;
import java.util.List;

/** Budżet punktów umiejętności aktywnego bohatera, niezależny od runtime DPS. */
public final class HeroSkillPointBudget {
    public static final int MIN_HERO_LEVEL = 1;
    public static final int MAX_HERO_LEVEL = 70;
    public static final int MAX_QUEST_SKILL_POINTS = 14;
    public static final int MAX_BOUGHT_SKILL_RANK = 15;

    private final Integer heroLevel;
    private final Integer questSkillPoints;
    private final int spentSkillPoints;
    private final List<String> validationErrors;

    private HeroSkillPointBudget(Integer heroLevel,
                                 Integer questSkillPoints,
                                 int spentSkillPoints,
                                 List<String> validationErrors) {
        this.heroLevel = heroLevel;
        this.questSkillPoints = questSkillPoints;
        this.spentSkillPoints = spentSkillPoints;
        this.validationErrors = List.copyOf(validationErrors);
    }

    public static HeroSkillPointBudget from(CurrentBuildFormData formData, HeroSkillLoadout skillLoadout) {
        List<String> errors = new ArrayList<>();
        Integer heroLevel = parseRange(
                formData.getLevel(),
                "Poziom bohatera",
                MIN_HERO_LEVEL,
                MAX_HERO_LEVEL,
                errors
        );
        Integer questSkillPoints = parseRange(
                formData.getQuestSkillPoints(),
                "Dodatkowe punkty z zadań",
                0,
                MAX_QUEST_SKILL_POINTS,
                errors
        );
        int spentSkillPoints = spentSkillPoints(skillLoadout);
        validateBoughtSkillRanks(skillLoadout, errors);
        if (heroLevel != null && questSkillPoints != null && spentSkillPoints > availableSkillPoints(heroLevel, questSkillPoints)) {
            errors.add("Nie można zapisać: wydano "
                    + spentSkillPoints
                    + " punktów, dostępne "
                    + availableSkillPoints(heroLevel, questSkillPoints)
                    + ".");
        }
        return new HeroSkillPointBudget(heroLevel, questSkillPoints, spentSkillPoints, errors);
    }

    public Integer getHeroLevel() {
        return heroLevel;
    }

    public Integer getQuestSkillPoints() {
        return questSkillPoints;
    }

    public int getLevelSkillPoints() {
        return heroLevel == null ? 0 : Math.max(0, Math.min(heroLevel, MAX_HERO_LEVEL) - 1);
    }

    public int getAvailableSkillPoints() {
        if (heroLevel == null || questSkillPoints == null) {
            return 0;
        }
        return availableSkillPoints(heroLevel, questSkillPoints);
    }

    public int getSpentSkillPoints() {
        return spentSkillPoints;
    }

    public int getRemainingSkillPoints() {
        return getAvailableSkillPoints() - spentSkillPoints;
    }

    public boolean isValid() {
        return validationErrors.isEmpty();
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    private static int availableSkillPoints(int heroLevel, int questSkillPoints) {
        return Math.max(0, Math.min(heroLevel, MAX_HERO_LEVEL) - 1) + questSkillPoints;
    }

    private static int spentSkillPoints(HeroSkillLoadout skillLoadout) {
        int spent = 0;
        for (HeroAssignedSkill assignedSkill : skillLoadout.getAssignedSkills().values()) {
            spent += Math.max(assignedSkill.getRank(), 0);
            if (assignedSkill.isBaseUpgrade()) {
                spent += 1;
            }
            if (assignedSkill.getSkillId() == SkillId.CLASH) {
                if (!SkillState.NO_TREE_CHOICE.equals(assignedSkill.getChoiceGroup1())) {
                    spent += 1;
                }
                if (!SkillState.NO_TREE_CHOICE.equals(assignedSkill.getChoiceGroup2())) {
                    spent += 1;
                }
                if (!SkillState.NO_TREE_CHOICE.equals(assignedSkill.getChoiceGroup3())) {
                    spent += 1;
                }
            } else {
                if (assignedSkill.getChoiceUpgrade() != SkillUpgradeChoice.NONE) {
                    spent += 1;
                }
                if (assignedSkill.getRuntimeModifierChoice() != SkillRuntimeModifierChoice.NONE) {
                    spent += 1;
                }
            }
        }
        return spent;
    }

    private static void validateBoughtSkillRanks(HeroSkillLoadout skillLoadout, List<String> errors) {
        for (HeroAssignedSkill assignedSkill : skillLoadout.getAssignedSkills().values()) {
            if (assignedSkill.getRank() > MAX_BOUGHT_SKILL_RANK) {
                errors.add("Ranga z punktów umiejętności "
                        + assignedSkill.getSkillId().name()
                        + " musi być w zakresie 0.."
                        + MAX_BOUGHT_SKILL_RANK
                        + ".");
            }
        }
    }

    private static Integer parseRange(String rawValue,
                                      String label,
                                      int minimumInclusive,
                                      int maximumInclusive,
                                      List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            errors.add(label + " jest wymagany.");
            return null;
        }
        try {
            int value = Integer.parseInt(rawValue);
            if (value < minimumInclusive || value > maximumInclusive) {
                errors.add(label + " musi być w zakresie " + minimumInclusive + ".." + maximumInclusive + ".");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.add(label + " musi być liczbą całkowitą.");
            return null;
        }
    }
}
