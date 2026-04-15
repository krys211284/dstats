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
    private final long reactiveDamage;
    private final long totalStepDamage;
    private final long cumulativeDamage;
    private final List<SkillBarStateTrace> skillBarStates;
    private final String selectionReason;
    private final String tickOrderLabel;

    public SimulationStepTrace(int second,
                               SimulationActionType actionType,
                               String actionName,
                               long directDamage,
                               long delayedDamage,
                               long reactiveDamage,
                               long totalStepDamage,
                               long cumulativeDamage,
                               List<SkillBarStateTrace> skillBarStates,
                               String selectionReason,
                               String tickOrderLabel) {
        this.second = second;
        this.actionType = actionType;
        this.actionName = actionName;
        this.directDamage = directDamage;
        this.delayedDamage = delayedDamage;
        this.reactiveDamage = reactiveDamage;
        this.totalStepDamage = totalStepDamage;
        this.cumulativeDamage = cumulativeDamage;
        this.skillBarStates = Collections.unmodifiableList(new ArrayList<>(skillBarStates));
        this.selectionReason = selectionReason;
        this.tickOrderLabel = tickOrderLabel;
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

    public long getReactiveDamage() {
        return reactiveDamage;
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

    public String getTickOrderLabel() {
        return tickOrderLabel;
    }
}
