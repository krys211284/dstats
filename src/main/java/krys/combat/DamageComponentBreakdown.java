package krys.combat;

/**
 * Debug pojedynczego komponentu obrażeń.
 * Rozróżnia komponenty aktywne od pominiętych w single target.
 */
public final class DamageComponentBreakdown {
    private final String name;
    private final String source;
    private final long skillDamagePercent;
    private final int hitCount;
    private final String conditionLabel;
    private final boolean active;
    private final boolean includedInSingleTarget;
    private final String exclusionReason;
    private final long rawDamage;
    private final long finalDamage;
    private final long rawCriticalDamage;
    private final long criticalDamage;

    public DamageComponentBreakdown(String name,
                                    String source,
                                    long skillDamagePercent,
                                    int hitCount,
                                    String conditionLabel,
                                    boolean active,
                                    boolean includedInSingleTarget,
                                    String exclusionReason,
                                    long rawDamage,
                                    long finalDamage,
                                    long rawCriticalDamage,
                                    long criticalDamage) {
        this.name = name;
        this.source = source;
        this.skillDamagePercent = skillDamagePercent;
        this.hitCount = hitCount;
        this.conditionLabel = conditionLabel;
        this.active = active;
        this.includedInSingleTarget = includedInSingleTarget;
        this.exclusionReason = exclusionReason;
        this.rawDamage = rawDamage;
        this.finalDamage = finalDamage;
        this.rawCriticalDamage = rawCriticalDamage;
        this.criticalDamage = criticalDamage;
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return source;
    }

    public long getSkillDamagePercent() {
        return skillDamagePercent;
    }

    public int getHitCount() {
        return hitCount;
    }

    public String getConditionLabel() {
        return conditionLabel;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isIncludedInSingleTarget() {
        return includedInSingleTarget;
    }

    public String getExclusionReason() {
        return exclusionReason;
    }

    public long getRawDamage() {
        return rawDamage;
    }

    public long getFinalDamage() {
        return finalDamage;
    }

    public long getRawCriticalDamage() {
        return rawCriticalDamage;
    }

    public long getCriticalDamage() {
        return criticalDamage;
    }
}
