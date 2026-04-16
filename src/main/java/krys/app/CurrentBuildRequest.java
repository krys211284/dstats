package krys.app;

import krys.skill.SkillId;
import krys.skill.SkillState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Wejście aplikacyjne dla flow „Policz aktualny build” używanego przez CLI i GUI. */
public final class CurrentBuildRequest {
    private final int level;
    private final long weaponDamage;
    private final double strength;
    private final double intelligence;
    private final double thorns;
    private final double blockChance;
    private final double retributionChance;
    private final Map<SkillId, SkillState> learnedSkills;
    private final List<SkillId> actionBar;
    private final int horizonSeconds;

    public CurrentBuildRequest(int level,
                               long weaponDamage,
                               double strength,
                               double intelligence,
                               double thorns,
                               double blockChance,
                               double retributionChance,
                               Map<SkillId, SkillState> learnedSkills,
                               List<SkillId> actionBar,
                               int horizonSeconds) {
        if (level <= 0) {
            throw new IllegalArgumentException("Level bohatera musi być dodatni");
        }
        if (weaponDamage <= 0) {
            throw new IllegalArgumentException("Weapon damage musi być dodatni");
        }
        validateNonNegative("Strength", strength);
        validateNonNegative("Intelligence", intelligence);
        validateNonNegative("Thorns", thorns);
        validateNonNegative("Block chance", blockChance);
        validateNonNegative("Retribution chance", retributionChance);
        if (horizonSeconds <= 0) {
            throw new IllegalArgumentException("Horyzont symulacji musi być dodatni");
        }

        EnumMap<SkillId, SkillState> learnedSkillsCopy = new EnumMap<>(SkillId.class);
        if (learnedSkills != null) {
            learnedSkillsCopy.putAll(learnedSkills);
        }
        validateActionBarAgainstLearnedSkills(actionBar, learnedSkillsCopy);

        this.level = level;
        this.weaponDamage = weaponDamage;
        this.strength = strength;
        this.intelligence = intelligence;
        this.thorns = thorns;
        this.blockChance = blockChance;
        this.retributionChance = retributionChance;
        this.learnedSkills = Collections.unmodifiableMap(learnedSkillsCopy);
        this.actionBar = Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(actionBar == null ? List.of() : actionBar)));
        this.horizonSeconds = horizonSeconds;
    }

    private static void validateNonNegative(String label, double value) {
        if (value < 0.0d) {
            throw new IllegalArgumentException(label + " nie może być ujemny");
        }
    }

    private static void validateActionBarAgainstLearnedSkills(List<SkillId> actionBar, Map<SkillId, SkillState> learnedSkills) {
        if (actionBar == null) {
            return;
        }
        for (SkillId skillId : actionBar) {
            SkillState state = learnedSkills.get(skillId);
            if (state == null || state.getRank() <= 0) {
                throw new IllegalArgumentException("Action bar może zawierać tylko nauczone skille z rank > 0: " + skillId);
            }
        }
    }

    public int getLevel() {
        return level;
    }

    public long getWeaponDamage() {
        return weaponDamage;
    }

    public double getStrength() {
        return strength;
    }

    public double getIntelligence() {
        return intelligence;
    }

    public double getThorns() {
        return thorns;
    }

    public double getBlockChance() {
        return blockChance;
    }

    public double getRetributionChance() {
        return retributionChance;
    }

    public Map<SkillId, SkillState> getLearnedSkills() {
        return learnedSkills;
    }

    public List<SkillId> getActionBar() {
        return actionBar;
    }

    public int getHorizonSeconds() {
        return horizonSeconds;
    }
}
