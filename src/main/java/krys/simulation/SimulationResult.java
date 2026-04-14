package krys.simulation;

import krys.combat.DamageBreakdown;
import krys.combat.DelayedHitBreakdown;

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
    private final List<SimulationStepTrace> stepTrace;
    private final boolean judgementActiveAtEnd;

    public SimulationResult(long totalDamage,
                            double dps,
                            int horizonSeconds,
                            List<SkillHitDebugSnapshot> directHitDebugSnapshots,
                            List<DelayedHitBreakdown> delayedHitBreakdowns,
                            List<SimulationStepTrace> stepTrace,
                            boolean judgementActiveAtEnd) {
        this.totalDamage = totalDamage;
        this.dps = dps;
        this.horizonSeconds = horizonSeconds;
        this.directHitDebugSnapshots = Collections.unmodifiableList(new ArrayList<>(directHitDebugSnapshots));
        this.delayedHitBreakdowns = Collections.unmodifiableList(new ArrayList<>(delayedHitBreakdowns));
        this.stepTrace = Collections.unmodifiableList(new ArrayList<>(stepTrace));
        this.judgementActiveAtEnd = judgementActiveAtEnd;
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

    public List<SimulationStepTrace> getStepTrace() {
        return stepTrace;
    }

    public boolean isJudgementActiveAtEnd() {
        return judgementActiveAtEnd;
    }
}
