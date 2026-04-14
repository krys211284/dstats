package krys.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pełny zapis jednego ticku runtime pochodzący z tej samej symulacji, która liczy wynik końcowy. */
public final class SimulationStepTrace {
    private final int second;
    private final SimulationActionType actionType;
    private final String actionName;
    private final long directDamage;
    private final long delayedDamage;
    private final long totalStepDamage;
    private final long cumulativeDamage;
    private final List<SkillBarStateTrace> skillBarStates;
    private final String selectionReason;

    public SimulationStepTrace(int second,
                               SimulationActionType actionType,
                               String actionName,
                               long directDamage,
                               long delayedDamage,
                               long totalStepDamage,
                               long cumulativeDamage,
                               List<SkillBarStateTrace> skillBarStates,
                               String selectionReason) {
        this.second = second;
        this.actionType = actionType;
        this.actionName = actionName;
        this.directDamage = directDamage;
        this.delayedDamage = delayedDamage;
        this.totalStepDamage = totalStepDamage;
        this.cumulativeDamage = cumulativeDamage;
        this.skillBarStates = Collections.unmodifiableList(new ArrayList<>(skillBarStates));
        this.selectionReason = selectionReason;
    }

    public int getSecond() {
        return second;
    }

    public SimulationActionType getActionType() {
        return actionType;
    }

    public String getActionName() {
        return actionName;
    }

    public long getDirectDamage() {
        return directDamage;
    }

    public long getDelayedDamage() {
        return delayedDamage;
    }

    public long getTotalStepDamage() {
        return totalStepDamage;
    }

    public long getCumulativeDamage() {
        return cumulativeDamage;
    }

    public List<SkillBarStateTrace> getSkillBarStates() {
        return skillBarStates;
    }

    public String getSelectionReason() {
        return selectionReason;
    }
}
