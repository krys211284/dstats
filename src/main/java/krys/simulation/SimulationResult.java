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
    private final String selectedSkillName;
    private final DamageBreakdown singleHitBreakdown;
    private final List<DelayedHitBreakdown> delayedHitBreakdowns;
    private final boolean judgementActiveAtEnd;

    public SimulationResult(long totalDamage,
                            double dps,
                            int horizonSeconds,
                            String selectedSkillName,
                            DamageBreakdown singleHitBreakdown,
                            List<DelayedHitBreakdown> delayedHitBreakdowns,
                            boolean judgementActiveAtEnd) {
        this.totalDamage = totalDamage;
        this.dps = dps;
        this.horizonSeconds = horizonSeconds;
        this.selectedSkillName = selectedSkillName;
        this.singleHitBreakdown = singleHitBreakdown;
        this.delayedHitBreakdowns = Collections.unmodifiableList(new ArrayList<>(delayedHitBreakdowns));
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

    public String getSelectedSkillName() {
        return selectedSkillName;
    }

    public DamageBreakdown getSingleHitBreakdown() {
        return singleHitBreakdown;
    }

    public List<DelayedHitBreakdown> getDelayedHitBreakdowns() {
        return delayedHitBreakdowns;
    }

    public boolean isJudgementActiveAtEnd() {
        return judgementActiveAtEnd;
    }
}
