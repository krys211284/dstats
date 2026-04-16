package krys.search;

import krys.skill.SkillId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Wejście aplikacyjne backendowego searcha M9.
 * Search działa na dyskretnych wartościach wejścia i generuje kandydatów wyłącznie z aktualnego foundation runtime.
 */
public final class BuildSearchRequest {
    private final List<Integer> levelValues;
    private final List<Long> weaponDamageValues;
    private final List<Double> strengthValues;
    private final List<Double> intelligenceValues;
    private final List<Double> thornsValues;
    private final List<Double> blockChanceValues;
    private final List<Double> retributionChanceValues;
    private final Map<SkillId, BuildSearchSkillSpace> skillSpaces;
    private final List<Integer> actionBarSizes;
    private final int horizonSeconds;
    private final int topResultsLimit;

    public BuildSearchRequest(List<Integer> levelValues,
                              List<Long> weaponDamageValues,
                              List<Double> strengthValues,
                              List<Double> intelligenceValues,
                              List<Double> thornsValues,
                              List<Double> blockChanceValues,
                              List<Double> retributionChanceValues,
                              Map<SkillId, BuildSearchSkillSpace> skillSpaces,
                              List<Integer> actionBarSizes,
                              int horizonSeconds,
                              int topResultsLimit) {
        this.levelValues = Collections.unmodifiableList(normalizePositiveIntegers(levelValues, "Level"));
        this.weaponDamageValues = Collections.unmodifiableList(normalizePositiveLongs(weaponDamageValues, "Weapon damage"));
        this.strengthValues = Collections.unmodifiableList(normalizeNonNegativeDoubles(strengthValues, "Strength"));
        this.intelligenceValues = Collections.unmodifiableList(normalizeNonNegativeDoubles(intelligenceValues, "Intelligence"));
        this.thornsValues = Collections.unmodifiableList(normalizeNonNegativeDoubles(thornsValues, "Thorns"));
        this.blockChanceValues = Collections.unmodifiableList(normalizeNonNegativeDoubles(blockChanceValues, "Block chance"));
        this.retributionChanceValues = Collections.unmodifiableList(normalizeNonNegativeDoubles(retributionChanceValues, "Retribution chance"));
        this.skillSpaces = Collections.unmodifiableMap(normalizeSkillSpaces(skillSpaces));
        this.actionBarSizes = Collections.unmodifiableList(normalizePositiveIntegers(actionBarSizes, "Rozmiar action bara"));

        if (horizonSeconds <= 0) {
            throw new IllegalArgumentException("Horyzont searcha musi być dodatni");
        }
        if (topResultsLimit <= 0) {
            throw new IllegalArgumentException("Top N musi być dodatnie");
        }

        this.horizonSeconds = horizonSeconds;
        this.topResultsLimit = topResultsLimit;
    }

    public List<Integer> getLevelValues() {
        return levelValues;
    }

    public List<Long> getWeaponDamageValues() {
        return weaponDamageValues;
    }

    public List<Double> getStrengthValues() {
        return strengthValues;
    }

    public List<Double> getIntelligenceValues() {
        return intelligenceValues;
    }

    public List<Double> getThornsValues() {
        return thornsValues;
    }

    public List<Double> getBlockChanceValues() {
        return blockChanceValues;
    }

    public List<Double> getRetributionChanceValues() {
        return retributionChanceValues;
    }

    public Map<SkillId, BuildSearchSkillSpace> getSkillSpaces() {
        return skillSpaces;
    }

    public BuildSearchSkillSpace getSkillSpace(SkillId skillId) {
        return skillSpaces.get(skillId);
    }

    public List<Integer> getActionBarSizes() {
        return actionBarSizes;
    }

    public int getHorizonSeconds() {
        return horizonSeconds;
    }

    public int getTopResultsLimit() {
        return topResultsLimit;
    }

    private static List<Integer> normalizePositiveIntegers(List<Integer> values, String label) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(label + " values nie mogą być puste");
        }

        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        for (Integer value : values) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException(label + " musi być dodatnie");
            }
            unique.add(value);
        }

        List<Integer> normalized = new ArrayList<>(unique);
        Collections.sort(normalized);
        return normalized;
    }

    private static List<Long> normalizePositiveLongs(List<Long> values, String label) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(label + " values nie mogą być puste");
        }

        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value == null || value <= 0L) {
                throw new IllegalArgumentException(label + " musi być dodatnie");
            }
            unique.add(value);
        }

        List<Long> normalized = new ArrayList<>(unique);
        Collections.sort(normalized);
        return normalized;
    }

    private static List<Double> normalizeNonNegativeDoubles(List<Double> values, String label) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(label + " values nie mogą być puste");
        }

        LinkedHashSet<Double> unique = new LinkedHashSet<>();
        for (Double value : values) {
            if (value == null || value < 0.0d) {
                throw new IllegalArgumentException(label + " nie może być ujemne");
            }
            unique.add(value);
        }

        List<Double> normalized = new ArrayList<>(unique);
        Collections.sort(normalized);
        return normalized;
    }

    private static Map<SkillId, BuildSearchSkillSpace> normalizeSkillSpaces(Map<SkillId, BuildSearchSkillSpace> rawSkillSpaces) {
        if (rawSkillSpaces == null) {
            throw new IllegalArgumentException("Skill spaces nie mogą być nullem");
        }

        EnumMap<SkillId, BuildSearchSkillSpace> normalized = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            BuildSearchSkillSpace skillSpace = rawSkillSpaces.get(skillId);
            if (skillSpace == null) {
                throw new IllegalArgumentException("Brakuje zakresu searcha dla skilla: " + skillId);
            }
            if (skillSpace.getSkillId() != skillId) {
                throw new IllegalArgumentException("Zakres searcha ma niespójny skillId dla: " + skillId);
            }
            normalized.put(skillId, skillSpace);
        }
        return normalized;
    }
}
