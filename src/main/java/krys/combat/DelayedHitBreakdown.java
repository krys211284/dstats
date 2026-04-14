package krys.combat;

/**
 * Debug pojedynczego zdarzenia delayed hit.
 * Przechowuje zarówno moment aplikacji, jak i moment detonacji.
 */
public final class DelayedHitBreakdown {
    private final String sourceSkillName;
    private final String delayedHitName;
    private final int appliedSecond;
    private final int triggerSecond;
    private final Integer detonatedSecond;
    private final boolean activeAtEnd;
    private final DamageBreakdown breakdown;

    public DelayedHitBreakdown(String sourceSkillName,
                               String delayedHitName,
                               int appliedSecond,
                               int triggerSecond,
                               Integer detonatedSecond,
                               boolean activeAtEnd,
                               DamageBreakdown breakdown) {
        this.sourceSkillName = sourceSkillName;
        this.delayedHitName = delayedHitName;
        this.appliedSecond = appliedSecond;
        this.triggerSecond = triggerSecond;
        this.detonatedSecond = detonatedSecond;
        this.activeAtEnd = activeAtEnd;
        this.breakdown = breakdown;
    }

    public String getSourceSkillName() {
        return sourceSkillName;
    }

    public String getDelayedHitName() {
        return delayedHitName;
    }

    public int getAppliedSecond() {
        return appliedSecond;
    }

    public int getTriggerSecond() {
        return triggerSecond;
    }

    public Integer getDetonatedSecond() {
        return detonatedSecond;
    }

    public boolean isActiveAtEnd() {
        return activeAtEnd;
    }

    public DamageBreakdown getBreakdown() {
        return breakdown;
    }

    public boolean isDetonated() {
        return detonatedSecond != null;
    }
}
