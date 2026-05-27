package krys.app;

import krys.hero.HeroClass;
import krys.hero.HeroClassStatBaselines;
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
    public static final double DEFAULT_INITIAL_PRIMARY_RESOURCE = 100.0d;
    public static final double DEFAULT_MAX_PRIMARY_RESOURCE = 100.0d;
    public static final double DEFAULT_PRIMARY_RESOURCE_REGEN_PER_SECOND = 1.50d;
    public static final double DEFAULT_INITIAL_ANIMUS = 8.0d;
    public static final double DEFAULT_MAX_ANIMUS = 8.0d;
    public static final String DEFAULT_SELECTED_PALADIN_OATH_ID = "NONE";
    public static final long DEFAULT_SIMULATION_SEED = 1L;
    public static final int MIN_SIMULATION_STEP_COUNT = 1;
    public static final int MAX_SIMULATION_STEP_COUNT = 200;

    private final int level;
    private final long weaponDamage;
    private final double strength;
    private final double intelligence;
    private final double thorns;
    private final double blockChance;
    private final double retributionChance;
    private final boolean hasActiveWeapon;
    private final boolean hasActiveShield;
    private final Map<SkillId, SkillState> learnedSkills;
    private final List<SkillId> actionBar;
    private final int horizonSeconds;
    private final double initialPrimaryResource;
    private final double maxPrimaryResource;
    private final double primaryResourceRegenPerSecond;
    private final String selectedPaladinOathId;
    private final double initialAnimus;
    private final double maxAnimus;
    private final List<String> activeAspectIds;
    private final double criticalChancePercent;
    private final long simulationSeed;

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
        this(level, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                weaponDamage > 0L, true, learnedSkills, actionBar, horizonSeconds);
    }

    public CurrentBuildRequest(int level,
                               long weaponDamage,
                               double strength,
                               double intelligence,
                               double thorns,
                               double blockChance,
                               double retributionChance,
                               Map<SkillId, SkillState> learnedSkills,
                               List<SkillId> actionBar,
                               int horizonSeconds,
                               List<String> activeAspectIds) {
        this(level, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                weaponDamage > 0L, true, learnedSkills, actionBar, horizonSeconds, activeAspectIds);
    }

    public CurrentBuildRequest(int level,
                               long weaponDamage,
                               double strength,
                               double intelligence,
                               double thorns,
                               double blockChance,
                               double retributionChance,
                               boolean hasActiveWeapon,
                               boolean hasActiveShield,
                               Map<SkillId, SkillState> learnedSkills,
                               List<SkillId> actionBar,
                               int horizonSeconds) {
        this(level, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                hasActiveWeapon, hasActiveShield, learnedSkills, actionBar, horizonSeconds, List.of());
    }

    public CurrentBuildRequest(int level,
                               long weaponDamage,
                               double strength,
                               double intelligence,
                               double thorns,
                               double blockChance,
                               double retributionChance,
                               boolean hasActiveWeapon,
                               boolean hasActiveShield,
                               Map<SkillId, SkillState> learnedSkills,
                               List<SkillId> actionBar,
                               int horizonSeconds,
                               List<String> activeAspectIds) {
        this(level, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                hasActiveWeapon, hasActiveShield, learnedSkills, actionBar, horizonSeconds,
                DEFAULT_INITIAL_PRIMARY_RESOURCE, DEFAULT_MAX_PRIMARY_RESOURCE,
                DEFAULT_PRIMARY_RESOURCE_REGEN_PER_SECOND, DEFAULT_SELECTED_PALADIN_OATH_ID,
                DEFAULT_INITIAL_ANIMUS, DEFAULT_MAX_ANIMUS, activeAspectIds);
    }

    public CurrentBuildRequest(int level,
                               long weaponDamage,
                               double strength,
                               double intelligence,
                               double thorns,
                               double blockChance,
                               double retributionChance,
                               boolean hasActiveWeapon,
                               boolean hasActiveShield,
                               Map<SkillId, SkillState> learnedSkills,
                               List<SkillId> actionBar,
                               int horizonSeconds,
                               double initialPrimaryResource,
                               double maxPrimaryResource,
                               double primaryResourceRegenPerSecond,
                               List<String> activeAspectIds) {
        this(level, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                hasActiveWeapon, hasActiveShield, learnedSkills, actionBar, horizonSeconds,
                initialPrimaryResource, maxPrimaryResource, primaryResourceRegenPerSecond,
                DEFAULT_SELECTED_PALADIN_OATH_ID, DEFAULT_INITIAL_ANIMUS, DEFAULT_MAX_ANIMUS, activeAspectIds);
    }

    public CurrentBuildRequest(int level,
                               long weaponDamage,
                               double strength,
                               double intelligence,
                               double thorns,
                               double blockChance,
                               double retributionChance,
                               boolean hasActiveWeapon,
                               boolean hasActiveShield,
                               Map<SkillId, SkillState> learnedSkills,
                               List<SkillId> actionBar,
                               int horizonSeconds,
                               double initialPrimaryResource,
                               double maxPrimaryResource,
                               double primaryResourceRegenPerSecond,
                               String selectedPaladinOathId,
                               double initialAnimus,
                               double maxAnimus,
                               List<String> activeAspectIds) {
        this(level, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                hasActiveWeapon, hasActiveShield, learnedSkills, actionBar, horizonSeconds,
                initialPrimaryResource, maxPrimaryResource, primaryResourceRegenPerSecond,
                selectedPaladinOathId, initialAnimus, maxAnimus, activeAspectIds,
                resolveDefaultCriticalChancePercent(level), DEFAULT_SIMULATION_SEED);
    }

    public CurrentBuildRequest(int level,
                               long weaponDamage,
                               double strength,
                               double intelligence,
                               double thorns,
                               double blockChance,
                               double retributionChance,
                               boolean hasActiveWeapon,
                               boolean hasActiveShield,
                               Map<SkillId, SkillState> learnedSkills,
                               List<SkillId> actionBar,
                               int horizonSeconds,
                               double initialPrimaryResource,
                               double maxPrimaryResource,
                               double primaryResourceRegenPerSecond,
                               String selectedPaladinOathId,
                               double initialAnimus,
                               double maxAnimus,
                               List<String> activeAspectIds,
                               double criticalChancePercent,
                               long simulationSeed) {
        if (level <= 0) {
            throw new IllegalArgumentException("Level bohatera musi być dodatni");
        }
        if (weaponDamage < 0) {
            throw new IllegalArgumentException("Weapon damage nie może być ujemny");
        }
        validateNonNegative("Strength", strength);
        validateNonNegative("Intelligence", intelligence);
        validateNonNegative("Thorns", thorns);
        validateNonNegative("Block chance", blockChance);
        validateNonNegative("Retribution chance", retributionChance);
        if (horizonSeconds < MIN_SIMULATION_STEP_COUNT || horizonSeconds > MAX_SIMULATION_STEP_COUNT) {
            throw new IllegalArgumentException("Liczba kroków symulacji musi być liczbą całkowitą od 1 do 200.");
        }
        validateNonNegative("Początkowa Wiara", initialPrimaryResource);
        validateNonNegative("Maksymalna Wiara", maxPrimaryResource);
        validateNonNegative("Regeneracja Wiary/s", primaryResourceRegenPerSecond);
        if (initialPrimaryResource > maxPrimaryResource) {
            throw new IllegalArgumentException("Początkowa Wiara nie może być większa niż Maksymalna Wiara.");
        }
        validateNonNegative("Początkowy Animusz", initialAnimus);
        validateNonNegative("Maksymalny Animusz", maxAnimus);
        validateCriticalChance(criticalChancePercent);
        if (initialAnimus > maxAnimus) {
            throw new IllegalArgumentException("Początkowy Animusz nie może być większy niż Maksymalny Animusz.");
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
        this.hasActiveWeapon = hasActiveWeapon;
        this.hasActiveShield = hasActiveShield;
        this.learnedSkills = Collections.unmodifiableMap(learnedSkillsCopy);
        this.actionBar = Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(actionBar == null ? List.of() : actionBar)));
        this.horizonSeconds = horizonSeconds;
        this.initialPrimaryResource = initialPrimaryResource;
        this.maxPrimaryResource = maxPrimaryResource;
        this.primaryResourceRegenPerSecond = primaryResourceRegenPerSecond;
        this.selectedPaladinOathId = selectedPaladinOathId == null || selectedPaladinOathId.isBlank()
                ? DEFAULT_SELECTED_PALADIN_OATH_ID
                : selectedPaladinOathId;
        this.initialAnimus = initialAnimus;
        this.maxAnimus = maxAnimus;
        this.activeAspectIds = Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(activeAspectIds == null ? List.of() : activeAspectIds)));
        this.criticalChancePercent = criticalChancePercent;
        this.simulationSeed = simulationSeed;
    }

    private static void validateNonNegative(String label, double value) {
        if (value < 0.0d) {
            throw new IllegalArgumentException(label + " nie może być ujemny");
        }
    }

    private static void validateCriticalChance(double value) {
        if (value < 0.0d || value > 100.0d) {
            throw new IllegalArgumentException("Szansa kryta musi być w zakresie 0-100%.");
        }
    }

    private static double resolveDefaultCriticalChancePercent(int level) {
        return HeroClassStatBaselines.find(HeroClass.PALADIN, level)
                .map(baseline -> baseline.getCriticalChancePercent().doubleValue())
                .orElse(0.0d);
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

    public boolean hasActiveWeapon() {
        return hasActiveWeapon;
    }

    public boolean hasActiveShield() {
        return hasActiveShield;
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

    public int getSimulationStepCount() {
        return horizonSeconds;
    }

    public double getInitialPrimaryResource() {
        return initialPrimaryResource;
    }

    public double getMaxPrimaryResource() {
        return maxPrimaryResource;
    }

    public double getPrimaryResourceRegenPerSecond() {
        return primaryResourceRegenPerSecond;
    }

    public String getSelectedPaladinOathId() {
        return selectedPaladinOathId;
    }

    public double getInitialAnimus() {
        return initialAnimus;
    }

    public double getMaxAnimus() {
        return maxAnimus;
    }

    public List<String> getActiveAspectIds() {
        return activeAspectIds;
    }

    public double getCriticalChancePercent() {
        return criticalChancePercent;
    }

    public long getSimulationSeed() {
        return simulationSeed;
    }

    public CurrentBuildRequest withActiveAspectIds(List<String> activeAspectIds) {
        return new CurrentBuildRequest(
                level,
                weaponDamage,
                strength,
                intelligence,
                thorns,
                blockChance,
                retributionChance,
                hasActiveWeapon,
                hasActiveShield,
                learnedSkills,
                actionBar,
                horizonSeconds,
                initialPrimaryResource,
                maxPrimaryResource,
                primaryResourceRegenPerSecond,
                selectedPaladinOathId,
                initialAnimus,
                maxAnimus,
                activeAspectIds,
                criticalChancePercent,
                simulationSeed
        );
    }

    public CurrentBuildRequest withCriticalChancePercent(double criticalChancePercent) {
        return new CurrentBuildRequest(
                level,
                weaponDamage,
                strength,
                intelligence,
                thorns,
                blockChance,
                retributionChance,
                hasActiveWeapon,
                hasActiveShield,
                learnedSkills,
                actionBar,
                horizonSeconds,
                initialPrimaryResource,
                maxPrimaryResource,
                primaryResourceRegenPerSecond,
                selectedPaladinOathId,
                initialAnimus,
                maxAnimus,
                activeAspectIds,
                criticalChancePercent,
                simulationSeed
        );
    }
}
