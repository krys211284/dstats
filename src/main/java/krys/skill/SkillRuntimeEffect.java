package krys.skill;

/**
 * Minimalna reprezentacja efektu runtime skilla.
 * Na tym etapie wspiera wyłącznie pola niezbędne do policzenia Brandish.
 */
public final class SkillRuntimeEffect {
    private final EffectType effectType;
    private final String componentName;
    private final StatusId conditionStatus;
    private final StatusId appliedStatus;
    private final int durationSeconds;
    private final long skillDamagePercent;
    private final int hitCount;
    private final boolean includedInSingleTarget;

    private SkillRuntimeEffect(EffectType effectType,
                               String componentName,
                               StatusId conditionStatus,
                               StatusId appliedStatus,
                               int durationSeconds,
                               long skillDamagePercent,
                               int hitCount,
                               boolean includedInSingleTarget) {
        this.effectType = effectType;
        this.componentName = componentName;
        this.conditionStatus = conditionStatus;
        this.appliedStatus = appliedStatus;
        this.durationSeconds = durationSeconds;
        this.skillDamagePercent = skillDamagePercent;
        this.hitCount = hitCount;
        this.includedInSingleTarget = includedInSingleTarget;
    }

    public static SkillRuntimeEffect replaceBaseDamage(String componentName, StatusId conditionStatus, long skillDamagePercent) {
        return new SkillRuntimeEffect(EffectType.REPLACE_BASE_DAMAGE, componentName, conditionStatus, StatusId.NONE, 0, skillDamagePercent, 1, true);
    }

    public static SkillRuntimeEffect damage(String componentName, StatusId conditionStatus, long skillDamagePercent, int hitCount, boolean includedInSingleTarget) {
        return new SkillRuntimeEffect(EffectType.DAMAGE, componentName, conditionStatus, StatusId.NONE, 0, skillDamagePercent, hitCount, includedInSingleTarget);
    }

    public static SkillRuntimeEffect applyStatus(StatusId appliedStatus, int durationSeconds) {
        return new SkillRuntimeEffect(EffectType.APPLY_STATUS, "Nałożenie statusu", StatusId.NONE, appliedStatus, durationSeconds, 0, 0, false);
    }

    public static SkillRuntimeEffect applyDelayedHit(String componentName, int delaySeconds, long skillDamagePercent) {
        return new SkillRuntimeEffect(EffectType.APPLY_DELAYED_HIT, componentName, StatusId.NONE, StatusId.NONE, delaySeconds, skillDamagePercent, 1, true);
    }

    public EffectType getEffectType() {
        return effectType;
    }

    public String getComponentName() {
        return componentName;
    }

    public StatusId getConditionStatus() {
        return conditionStatus;
    }

    public StatusId getAppliedStatus() {
        return appliedStatus;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public long getSkillDamagePercent() {
        return skillDamagePercent;
    }

    public int getHitCount() {
        return hitCount;
    }

    public boolean isIncludedInSingleTarget() {
        return includedInSingleTarget;
    }
}
