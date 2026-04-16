package krys.search;

import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ograniczony zakres legalnych stanów pojedynczego skilla dla searcha M9.
 * Search może generować wyłącznie stany zgodne z definicją skilla i kontraktem upgrade'ów.
 */
public final class BuildSearchSkillSpace {
    private final SkillId skillId;
    private final List<Integer> rankValues;
    private final List<Boolean> baseUpgradeValues;
    private final List<SkillUpgradeChoice> choiceUpgradeValues;

    public BuildSearchSkillSpace(SkillId skillId,
                                 List<Integer> rankValues,
                                 List<Boolean> baseUpgradeValues,
                                 List<SkillUpgradeChoice> choiceUpgradeValues) {
        if (skillId == null) {
            throw new IllegalArgumentException("SkillId nie może być nullem");
        }

        List<Integer> normalizedRanks = normalizeRankValues(rankValues);
        List<Boolean> normalizedBaseUpgradeValues = normalizeBaseUpgradeValues(baseUpgradeValues);
        List<SkillUpgradeChoice> normalizedChoiceUpgradeValues = normalizeChoiceUpgradeValues(skillId, choiceUpgradeValues);

        this.skillId = skillId;
        this.rankValues = Collections.unmodifiableList(normalizedRanks);
        this.baseUpgradeValues = Collections.unmodifiableList(normalizedBaseUpgradeValues);
        this.choiceUpgradeValues = Collections.unmodifiableList(normalizedChoiceUpgradeValues);
    }

    public SkillId getSkillId() {
        return skillId;
    }

    public List<Integer> getRankValues() {
        return rankValues;
    }

    public List<Boolean> getBaseUpgradeValues() {
        return baseUpgradeValues;
    }

    public List<SkillUpgradeChoice> getChoiceUpgradeValues() {
        return choiceUpgradeValues;
    }

    private static List<Integer> normalizeRankValues(List<Integer> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            throw new IllegalArgumentException("Search range ranków nie może być pusty");
        }

        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        for (Integer value : rawValues) {
            if (value == null || value < 0 || value > 5) {
                throw new IllegalArgumentException("Rank skilla w searchu musi mieścić się w zakresie 0..5");
            }
            unique.add(value);
        }

        List<Integer> normalized = new ArrayList<>(unique);
        Collections.sort(normalized);
        return normalized;
    }

    private static List<Boolean> normalizeBaseUpgradeValues(List<Boolean> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            throw new IllegalArgumentException("Zakres bazowego rozszerzenia nie może być pusty");
        }

        boolean containsFalse = false;
        boolean containsTrue = false;
        for (Boolean value : rawValues) {
            if (value == null) {
                throw new IllegalArgumentException("Zakres bazowego rozszerzenia nie może zawierać null");
            }
            if (value) {
                containsTrue = true;
            } else {
                containsFalse = true;
            }
        }

        List<Boolean> normalized = new ArrayList<>();
        if (containsFalse) {
            normalized.add(Boolean.FALSE);
        }
        if (containsTrue) {
            normalized.add(Boolean.TRUE);
        }
        return normalized;
    }

    private static List<SkillUpgradeChoice> normalizeChoiceUpgradeValues(SkillId skillId,
                                                                         List<SkillUpgradeChoice> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            throw new IllegalArgumentException("Zakres dodatkowych modyfikatorów nie może być pusty");
        }

        Set<SkillUpgradeChoice> legalChoices = EnumSet.noneOf(SkillUpgradeChoice.class);
        legalChoices.add(SkillUpgradeChoice.NONE);
        legalChoices.addAll(PaladinSkillDefs.get(skillId).getAvailableChoiceUpgrades());

        LinkedHashSet<SkillUpgradeChoice> unique = new LinkedHashSet<>();
        for (SkillUpgradeChoice choiceUpgrade : rawValues) {
            if (choiceUpgrade == null) {
                throw new IllegalArgumentException("Zakres dodatkowych modyfikatorów nie może zawierać null");
            }
            if (!legalChoices.contains(choiceUpgrade)) {
                throw new IllegalArgumentException("Nielegalny dodatkowy modyfikator dla " + skillId + ": " + choiceUpgrade);
            }
            unique.add(choiceUpgrade);
        }

        List<SkillUpgradeChoice> normalized = new ArrayList<>();
        for (SkillUpgradeChoice choiceUpgrade : SkillUpgradeChoice.values()) {
            if (unique.contains(choiceUpgrade)) {
                normalized.add(choiceUpgrade);
            }
        }
        return normalized;
    }
}
