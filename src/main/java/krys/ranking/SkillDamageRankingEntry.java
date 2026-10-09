package krys.ranking;

import java.util.Objects;

/** Wynik rankingu albo opis elementu wyłączonego z rankingu obrażeń. */
public final class SkillDamageRankingEntry {
    private final String skillId;
    private final String skillName;
    private final String sourceReference;
    private final String skillGroup;
    private final Integer baseDamagePercentAtRank1;
    private final Integer baseDamagePercentAtTreeMaxRank;
    private final SkillDamageModelType damageModelType;
    private final Long damagePerUse;
    private final Integer cooldownSeconds;
    private final Integer effectiveCycleSeconds;
    private final Double theoreticalDps;
    private final DamageTargetMode targetMode;
    private final SkillDamageVerificationStatus verificationStatus;
    private final String notes;

    public SkillDamageRankingEntry(String skillId,
                                          String skillName,
                                          String sourceReference,
                                          String skillGroup,
                                          SkillDamageModelType damageModelType,
                                          Long damagePerUse,
                                          Integer cooldownSeconds,
                                          Integer effectiveCycleSeconds,
                                          Double theoreticalDps,
                                          DamageTargetMode targetMode,
                                          SkillDamageVerificationStatus verificationStatus,
                                          String notes) {
        this(skillId, skillName, sourceReference, skillGroup, null, null, damageModelType, damagePerUse,
                cooldownSeconds, effectiveCycleSeconds, theoreticalDps, targetMode, verificationStatus, notes);
    }

    public SkillDamageRankingEntry(String skillId,
                                          String skillName,
                                          String sourceReference,
                                          String skillGroup,
                                          Integer baseDamagePercentAtRank1,
                                          Integer baseDamagePercentAtTreeMaxRank,
                                          SkillDamageModelType damageModelType,
                                          Long damagePerUse,
                                          Integer cooldownSeconds,
                                          Integer effectiveCycleSeconds,
                                          Double theoreticalDps,
                                          DamageTargetMode targetMode,
                                          SkillDamageVerificationStatus verificationStatus,
                                          String notes) {
        this.skillId = requireText(skillId, "skillId");
        this.skillName = requireText(skillName, "skillName");
        this.sourceReference = requireText(sourceReference, "sourceReference");
        this.skillGroup = requireText(skillGroup, "skillGroup");
        this.baseDamagePercentAtRank1 = baseDamagePercentAtRank1;
        this.baseDamagePercentAtTreeMaxRank = baseDamagePercentAtTreeMaxRank;
        this.damageModelType = Objects.requireNonNull(damageModelType, "damageModelType");
        this.damagePerUse = damagePerUse;
        this.cooldownSeconds = cooldownSeconds;
        this.effectiveCycleSeconds = effectiveCycleSeconds;
        this.theoreticalDps = theoreticalDps;
        this.targetMode = Objects.requireNonNull(targetMode, "targetMode");
        this.verificationStatus = Objects.requireNonNull(verificationStatus, "verificationStatus");
        this.notes = requireText(notes, "notes");
    }

    public String getSkillId() {
        return skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public String getSkillGroup() {
        return skillGroup;
    }

    public Integer getBaseDamagePercentAtRank1() {
        return baseDamagePercentAtRank1;
    }

    public Integer getBaseDamagePercentAtTreeMaxRank() {
        return baseDamagePercentAtTreeMaxRank;
    }

    public SkillDamageModelType getDamageModelType() {
        return damageModelType;
    }

    public Long getDamagePerUse() {
        return damagePerUse;
    }

    public Integer getCooldownSeconds() {
        return cooldownSeconds;
    }

    public Integer getEffectiveCycleSeconds() {
        return effectiveCycleSeconds;
    }

    public Double getTheoreticalDps() {
        return theoreticalDps;
    }

    public DamageTargetMode getTargetMode() {
        return targetMode;
    }

    public SkillDamageVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public String getNotes() {
        return notes;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }
}
