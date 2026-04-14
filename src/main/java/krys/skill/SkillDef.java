package krys.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Definicja skilla używana przez wspólny Damage Engine. */
public final class SkillDef {
    private final SkillId id;
    private final String name;
    private final long[] rankSkillDamagePercents;
    private final Map<SkillUpgradeChoice, List<SkillRuntimeEffect>> choiceEffects;

    public SkillDef(SkillId id,
                    String name,
                    long[] rankSkillDamagePercents,
                    Map<SkillUpgradeChoice, List<SkillRuntimeEffect>> choiceEffects) {
        this.id = id;
        this.name = name;
        this.rankSkillDamagePercents = rankSkillDamagePercents.clone();
        Map<SkillUpgradeChoice, List<SkillRuntimeEffect>> copy = new EnumMap<>(SkillUpgradeChoice.class);
        for (Map.Entry<SkillUpgradeChoice, List<SkillRuntimeEffect>> entry : choiceEffects.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        this.choiceEffects = Collections.unmodifiableMap(copy);
    }

    public SkillId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getBaseSkillDamagePercent(int rank) {
        if (rank <= 0 || rank > rankSkillDamagePercents.length) {
            throw new IllegalArgumentException("Niepoprawny rank skilla " + name + ": " + rank);
        }
        return rankSkillDamagePercents[rank - 1];
    }

    public List<SkillRuntimeEffect> getChoiceEffects(SkillUpgradeChoice choiceUpgrade) {
        return choiceEffects.getOrDefault(choiceUpgrade, List.of());
    }
}
