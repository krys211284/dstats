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
    private final long normalDirectDamage;
    private final long criticalDirectDamage;
    private final double criticalChancePercent;
    private final double criticalRollPercent;
    private final boolean criticalHit;
    private final long appliedDirectDamage;
    private final long delayedDamage;
    private final long reactiveDamage;
    private final long totalStepDamage;
    private final long cumulativeDamage;
    private final List<SkillBarStateTrace> skillBarStates;
    private final String selectionReason;
    private final String tickOrderLabel;
    private final double primaryResourceBefore;
    private final double primaryResourceCost;
    private final double primaryResourceGenerated;
    private final double primaryResourceRegenerated;
    private final double primaryResourceAfter;
    private final double animusBefore;
    private final double animusSpent;
    private final double animusMinimum;
    private final double animusGenerated;
    private final double animusAfter;
    private final boolean molochBuffActivated;
    private final boolean molochBuffActive;
    private final int molochBuffRemainingSeconds;

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
        this(second, actionType, actionName, directDamage, delayedDamage, reactiveDamage, totalStepDamage,
                cumulativeDamage, skillBarStates, selectionReason, tickOrderLabel,
                0.0d, 0.0d, 0.0d, 0.0d, 0.0d,
                0.0d, 0.0d, 0.0d, 0.0d, false, false, 0);
    }

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
                               String tickOrderLabel,
                               double primaryResourceBefore,
                               double primaryResourceCost,
                               double primaryResourceGenerated,
                               double primaryResourceRegenerated,
                               double primaryResourceAfter) {
        this(second, actionType, actionName, directDamage, delayedDamage, reactiveDamage, totalStepDamage,
                cumulativeDamage, skillBarStates, selectionReason, tickOrderLabel,
                primaryResourceBefore, primaryResourceCost, primaryResourceGenerated, primaryResourceRegenerated,
                primaryResourceAfter, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, false, false, 0);
    }

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
                               String tickOrderLabel,
                               double primaryResourceBefore,
                               double primaryResourceCost,
                               double primaryResourceGenerated,
                               double primaryResourceRegenerated,
                               double primaryResourceAfter,
                               double animusBefore,
                               double animusSpent,
                               double animusGenerated,
                               double animusAfter,
                               boolean molochBuffActivated,
                               boolean molochBuffActive,
                               int molochBuffRemainingSeconds) {
        this(second, actionType, actionName, directDamage, delayedDamage, reactiveDamage, totalStepDamage,
                cumulativeDamage, skillBarStates, selectionReason, tickOrderLabel,
                primaryResourceBefore, primaryResourceCost, primaryResourceGenerated, primaryResourceRegenerated,
                primaryResourceAfter, animusBefore, animusSpent, 0.0d, animusGenerated, animusAfter,
                molochBuffActivated, molochBuffActive, molochBuffRemainingSeconds);
    }

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
                               String tickOrderLabel,
                               double primaryResourceBefore,
                               double primaryResourceCost,
                               double primaryResourceGenerated,
                               double primaryResourceRegenerated,
                               double primaryResourceAfter,
                               double animusBefore,
                               double animusSpent,
                               double animusMinimum,
                               double animusGenerated,
                               double animusAfter,
                               boolean molochBuffActivated,
                               boolean molochBuffActive,
                               int molochBuffRemainingSeconds) {
        this(second, actionType, actionName, directDamage, directDamage, 0.0d, Double.NaN,
                false, directDamage, delayedDamage, reactiveDamage, totalStepDamage, cumulativeDamage,
                skillBarStates, selectionReason, tickOrderLabel,
                primaryResourceBefore, primaryResourceCost, primaryResourceGenerated, primaryResourceRegenerated,
                primaryResourceAfter, animusBefore, animusSpent, animusMinimum, animusGenerated, animusAfter,
                molochBuffActivated, molochBuffActive, molochBuffRemainingSeconds);
    }

    public SimulationStepTrace(int second,
                               SimulationActionType actionType,
                               String actionName,
                               long normalDirectDamage,
                               long criticalDirectDamage,
                               double criticalChancePercent,
                               double criticalRollPercent,
                               boolean criticalHit,
                               long appliedDirectDamage,
                               long delayedDamage,
                               long reactiveDamage,
                               long totalStepDamage,
                               long cumulativeDamage,
                               List<SkillBarStateTrace> skillBarStates,
                               String selectionReason,
                               String tickOrderLabel,
                               double primaryResourceBefore,
                               double primaryResourceCost,
                               double primaryResourceGenerated,
                               double primaryResourceRegenerated,
                               double primaryResourceAfter,
                               double animusBefore,
                               double animusSpent,
                               double animusMinimum,
                               double animusGenerated,
                               double animusAfter,
                               boolean molochBuffActivated,
                               boolean molochBuffActive,
                               int molochBuffRemainingSeconds) {
        this.second = second;
        this.actionType = actionType;
        this.actionName = actionName;
        this.directDamage = appliedDirectDamage;
        this.normalDirectDamage = normalDirectDamage;
        this.criticalDirectDamage = criticalDirectDamage;
        this.criticalChancePercent = criticalChancePercent;
        this.criticalRollPercent = criticalRollPercent;
        this.criticalHit = criticalHit;
        this.appliedDirectDamage = appliedDirectDamage;
        this.delayedDamage = delayedDamage;
        this.reactiveDamage = reactiveDamage;
        this.totalStepDamage = totalStepDamage;
        this.cumulativeDamage = cumulativeDamage;
        this.skillBarStates = Collections.unmodifiableList(new ArrayList<>(skillBarStates));
        this.selectionReason = selectionReason;
        this.tickOrderLabel = tickOrderLabel;
        this.primaryResourceBefore = primaryResourceBefore;
        this.primaryResourceCost = primaryResourceCost;
        this.primaryResourceGenerated = primaryResourceGenerated;
        this.primaryResourceRegenerated = primaryResourceRegenerated;
        this.primaryResourceAfter = primaryResourceAfter;
        this.animusBefore = animusBefore;
        this.animusSpent = animusSpent;
        this.animusMinimum = animusMinimum;
        this.animusGenerated = animusGenerated;
        this.animusAfter = animusAfter;
        this.molochBuffActivated = molochBuffActivated;
        this.molochBuffActive = molochBuffActive;
        this.molochBuffRemainingSeconds = molochBuffRemainingSeconds;
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

    public long getNormalDirectDamage() {
        return normalDirectDamage;
    }

    public long getCriticalDirectDamage() {
        return criticalDirectDamage;
    }

    public double getCriticalChancePercent() {
        return criticalChancePercent;
    }

    public double getCriticalRollPercent() {
        return criticalRollPercent;
    }

    public boolean hasCriticalRoll() {
        return !Double.isNaN(criticalRollPercent);
    }

    public boolean isCriticalHit() {
        return criticalHit;
    }

    public long getAppliedDirectDamage() {
        return appliedDirectDamage;
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

    public double getPrimaryResourceBefore() {
        return primaryResourceBefore;
    }

    public double getPrimaryResourceCost() {
        return primaryResourceCost;
    }

    public double getPrimaryResourceGenerated() {
        return primaryResourceGenerated;
    }

    public double getPrimaryResourceRegenerated() {
        return primaryResourceRegenerated;
    }

    public double getPrimaryResourceAfter() {
        return primaryResourceAfter;
    }

    public double getAnimusBefore() {
        return animusBefore;
    }

    public double getAnimusSpent() {
        return animusSpent;
    }

    public double getAnimusMinimum() {
        return animusMinimum;
    }

    public double getAnimusGenerated() {
        return animusGenerated;
    }

    public double getAnimusAfter() {
        return animusAfter;
    }

    public boolean isMolochBuffActivated() {
        return molochBuffActivated;
    }

    public boolean isMolochBuffActive() {
        return molochBuffActive;
    }

    public boolean isMolochDamageApplied() {
        return actionType == SimulationActionType.SKILL && directDamage > 0L && molochBuffActive;
    }

    public int getMolochBuffRemainingSeconds() {
        return molochBuffRemainingSeconds;
    }
}
