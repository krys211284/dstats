package krys.simulation;

import krys.combat.DelayedHitBreakdown;
import krys.combat.ReactiveHitBreakdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Minimalny wynik ręcznej symulacji dla trybu „Policz aktualny build”. */
public final class SimulationResult {
    private final long totalDamage;
    private final double dps;
    private final int horizonSeconds;
    private final List<SkillHitDebugSnapshot> directHitDebugSnapshots;
    private final List<DelayedHitBreakdown> delayedHitBreakdowns;
    private final List<ReactiveHitBreakdown> reactiveHitBreakdowns;
    private final long totalReactiveDamage;
    private final boolean resolveActiveAtEnd;
    private final double activeBlockChanceAtEnd;
    private final double activeThornsBonusAtEnd;
    private final List<SimulationStepTrace> stepTrace;
    private final boolean judgementActiveAtEnd;
    private final double initialPrimaryResource;
    private final double finalPrimaryResource;
    private final double maxPrimaryResource;
    private final double primaryResourceRegenPerSecond;
    private final double totalPrimaryResourceCost;
    private final double totalPrimaryResourceGenerated;
    private final double totalPrimaryResourceRegenerated;

    public SimulationResult(long totalDamage,
                            double dps,
                            int horizonSeconds,
                            List<SkillHitDebugSnapshot> directHitDebugSnapshots,
                            List<DelayedHitBreakdown> delayedHitBreakdowns,
                            List<ReactiveHitBreakdown> reactiveHitBreakdowns,
                            long totalReactiveDamage,
                            boolean resolveActiveAtEnd,
                            double activeBlockChanceAtEnd,
                            double activeThornsBonusAtEnd,
                            List<SimulationStepTrace> stepTrace,
                            boolean judgementActiveAtEnd) {
        this(totalDamage, dps, horizonSeconds, directHitDebugSnapshots, delayedHitBreakdowns, reactiveHitBreakdowns,
                totalReactiveDamage, resolveActiveAtEnd, activeBlockChanceAtEnd, activeThornsBonusAtEnd, stepTrace,
                judgementActiveAtEnd, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
    }

    public SimulationResult(long totalDamage,
                            double dps,
                            int horizonSeconds,
                            List<SkillHitDebugSnapshot> directHitDebugSnapshots,
                            List<DelayedHitBreakdown> delayedHitBreakdowns,
                            List<ReactiveHitBreakdown> reactiveHitBreakdowns,
                            long totalReactiveDamage,
                            boolean resolveActiveAtEnd,
                            double activeBlockChanceAtEnd,
                            double activeThornsBonusAtEnd,
                            List<SimulationStepTrace> stepTrace,
                            boolean judgementActiveAtEnd,
                            double initialPrimaryResource,
                            double finalPrimaryResource,
                            double maxPrimaryResource,
                            double primaryResourceRegenPerSecond,
                            double totalPrimaryResourceCost,
                            double totalPrimaryResourceGenerated,
                            double totalPrimaryResourceRegenerated) {
        this.totalDamage = totalDamage;
        this.dps = dps;
        this.horizonSeconds = horizonSeconds;
        this.directHitDebugSnapshots = Collections.unmodifiableList(new ArrayList<>(directHitDebugSnapshots));
        this.delayedHitBreakdowns = Collections.unmodifiableList(new ArrayList<>(delayedHitBreakdowns));
        this.reactiveHitBreakdowns = Collections.unmodifiableList(new ArrayList<>(reactiveHitBreakdowns));
        this.totalReactiveDamage = totalReactiveDamage;
        this.resolveActiveAtEnd = resolveActiveAtEnd;
        this.activeBlockChanceAtEnd = activeBlockChanceAtEnd;
        this.activeThornsBonusAtEnd = activeThornsBonusAtEnd;
        this.stepTrace = Collections.unmodifiableList(new ArrayList<>(stepTrace));
        this.judgementActiveAtEnd = judgementActiveAtEnd;
        this.initialPrimaryResource = initialPrimaryResource;
        this.finalPrimaryResource = finalPrimaryResource;
        this.maxPrimaryResource = maxPrimaryResource;
        this.primaryResourceRegenPerSecond = primaryResourceRegenPerSecond;
        this.totalPrimaryResourceCost = totalPrimaryResourceCost;
        this.totalPrimaryResourceGenerated = totalPrimaryResourceGenerated;
        this.totalPrimaryResourceRegenerated = totalPrimaryResourceRegenerated;
    }

    public long getTotalDamage() {
        return totalDamage;
    }

    public double getDps() {
        return dps;
    }

    public int getHorizonSeconds() {
        return horizonSeconds;
    }

    public List<SkillHitDebugSnapshot> getDirectHitDebugSnapshots() {
        return directHitDebugSnapshots;
    }

    public List<DelayedHitBreakdown> getDelayedHitBreakdowns() {
        return delayedHitBreakdowns;
    }

    public List<ReactiveHitBreakdown> getReactiveHitBreakdowns() {
        return reactiveHitBreakdowns;
    }

    public long getTotalReactiveDamage() {
        return totalReactiveDamage;
    }

    public boolean isResolveActiveAtEnd() {
        return resolveActiveAtEnd;
    }

    public double getActiveBlockChanceAtEnd() {
        return activeBlockChanceAtEnd;
    }

    public double getActiveThornsBonusAtEnd() {
        return activeThornsBonusAtEnd;
    }

    public List<SimulationStepTrace> getStepTrace() {
        return stepTrace;
    }

    public boolean isJudgementActiveAtEnd() {
        return judgementActiveAtEnd;
    }

    public double getInitialPrimaryResource() {
        return initialPrimaryResource;
    }

    public double getFinalPrimaryResource() {
        return finalPrimaryResource;
    }

    public double getMaxPrimaryResource() {
        return maxPrimaryResource;
    }

    public double getPrimaryResourceRegenPerSecond() {
        return primaryResourceRegenPerSecond;
    }

    public double getTotalPrimaryResourceCost() {
        return totalPrimaryResourceCost;
    }

    public double getTotalPrimaryResourceGenerated() {
        return totalPrimaryResourceGenerated;
    }

    public double getTotalPrimaryResourceRegenerated() {
        return totalPrimaryResourceRegenerated;
    }
}
