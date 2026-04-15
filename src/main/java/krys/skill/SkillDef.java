package krys.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Definicja skilla używana przez wspólny Damage Engine. */
public final class SkillDef {
    private final SkillId id;
    private final String name;
    private final long resourceCost;
    private final int cooldownSeconds;
    private final long[] rankSkillDamagePercents;
    private final List<SkillRuntimeEffect> baseUpgradeEffects;
    private final ReactiveSelfBuffProfile baseReactiveBuffProfile;
    private final Map<SkillUpgradeChoice, List<SkillRuntimeEffect>> choiceEffects;
    private final Map<SkillUpgradeChoice, ReactiveSelfBuffProfile> choiceReactiveBuffProfiles;
    private final Map<SkillUpgradeChoice, CriticalRoundingPolicy> criticalRoundingPolicies;
    private final Map<SkillUpgradeChoice, String> choiceDisplayNames;

    public SkillDef(SkillId id,
                    String name,
                    long resourceCost,
                    int cooldownSeconds,
                    long[] rankSkillDamagePercents,
                    List<SkillRuntimeEffect> baseUpgradeEffects,
                    Map<SkillUpgradeChoice, List<SkillRuntimeEffect>> choiceEffects,
                    Map<SkillUpgradeChoice, CriticalRoundingPolicy> criticalRoundingPolicies,
                    ReactiveSelfBuffProfile baseReactiveBuffProfile,
                    Map<SkillUpgradeChoice, ReactiveSelfBuffProfile> choiceReactiveBuffProfiles,
                    Map<SkillUpgradeChoice, String> choiceDisplayNames) {
        this.id = id;
        this.name = name;
        this.resourceCost = resourceCost;
        this.cooldownSeconds = cooldownSeconds;
        this.rankSkillDamagePercents = rankSkillDamagePercents.clone();
        this.baseUpgradeEffects = Collections.unmodifiableList(new ArrayList<>(baseUpgradeEffects));
        this.baseReactiveBuffProfile = baseReactiveBuffProfile;
        Map<SkillUpgradeChoice, List<SkillRuntimeEffect>> copy = new EnumMap<>(SkillUpgradeChoice.class);
        for (Map.Entry<SkillUpgradeChoice, List<SkillRuntimeEffect>> entry : choiceEffects.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        this.choiceEffects = Collections.unmodifiableMap(copy);
        Map<SkillUpgradeChoice, ReactiveSelfBuffProfile> reactiveBuffCopy = new EnumMap<>(SkillUpgradeChoice.class);
        reactiveBuffCopy.putAll(choiceReactiveBuffProfiles);
        this.choiceReactiveBuffProfiles = Collections.unmodifiableMap(reactiveBuffCopy);
        Map<SkillUpgradeChoice, CriticalRoundingPolicy> policyCopy = new EnumMap<>(SkillUpgradeChoice.class);
        policyCopy.putAll(criticalRoundingPolicies);
        this.criticalRoundingPolicies = Collections.unmodifiableMap(policyCopy);
        Map<SkillUpgradeChoice, String> choiceDisplayCopy = new EnumMap<>(SkillUpgradeChoice.class);
        choiceDisplayCopy.putAll(choiceDisplayNames);
        this.choiceDisplayNames = Collections.unmodifiableMap(choiceDisplayCopy);
    }

    public SkillId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getResourceCost() {
        return resourceCost;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public long getBaseSkillDamagePercent(int rank) {
        if (rank <= 0 || rank > rankSkillDamagePercents.length) {
            throw new IllegalArgumentException("Niepoprawny rank skilla " + name + ": " + rank);
        }
        return rankSkillDamagePercents[rank - 1];
    }

    public List<SkillRuntimeEffect> getBaseUpgradeEffects() {
        return baseUpgradeEffects;
    }

    public ReactiveSelfBuffProfile getBaseReactiveBuffProfile() {
        return baseReactiveBuffProfile;
    }

    public List<SkillRuntimeEffect> getChoiceEffects(SkillUpgradeChoice choiceUpgrade) {
        return choiceEffects.getOrDefault(choiceUpgrade, List.of());
    }

    public ReactiveSelfBuffProfile getChoiceReactiveBuffProfile(SkillUpgradeChoice choiceUpgrade) {
        return choiceReactiveBuffProfiles.get(choiceUpgrade);
    }

    public CriticalRoundingPolicy getCriticalRoundingPolicy(SkillUpgradeChoice choiceUpgrade) {
        return criticalRoundingPolicies.getOrDefault(choiceUpgrade, CriticalRoundingPolicy.EXACT_PIPELINE);
    }

    public String getChoiceDisplayName(SkillUpgradeChoice choiceUpgrade) {
        return choiceDisplayNames.getOrDefault(choiceUpgrade, choiceUpgrade.getDisplayName());
    }

    public Set<SkillUpgradeChoice> getAvailableChoiceUpgrades() {
        LinkedHashSet<SkillUpgradeChoice> choices = new LinkedHashSet<>();
        choices.addAll(choiceEffects.keySet());
        choices.addAll(choiceReactiveBuffProfiles.keySet());
        choices.addAll(criticalRoundingPolicies.keySet());
        choices.addAll(choiceDisplayNames.keySet());
        return Collections.unmodifiableSet(choices);
    }
}
