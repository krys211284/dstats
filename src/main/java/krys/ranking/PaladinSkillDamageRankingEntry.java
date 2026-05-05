package krys.ranking;

import java.util.Objects;

/** Wynik rankingu albo opis elementu wyłączonego z rankingu obrażeń. */
public final class PaladinSkillDamageRankingEntry {
    private final String skillId;
    private final String skillName;
    private final String sourcePdf;
    private final String skillGroup;
    private final PaladinSkillDamageModelType damageModelType;
    private final Long damagePerUse;
    private final Integer cooldownSeconds;
    private final Integer effectiveCycleSeconds;
    private final Double theoreticalDps;
    private final PaladinDamageTargetMode targetMode;
    private final PaladinSkillDamageVerificationStatus verificationStatus;
    private final String notes;

    public PaladinSkillDamageRankingEntry(String skillId,
                                          String skillName,
                                          String sourcePdf,
                                          String skillGroup,
                                          PaladinSkillDamageModelType damageModelType,
                                          Long damagePerUse,
                                          Integer cooldownSeconds,
                                          Integer effectiveCycleSeconds,
                                          Double theoreticalDps,
                                          PaladinDamageTargetMode targetMode,
                                          PaladinSkillDamageVerificationStatus verificationStatus,
                                          String notes) {
        this.skillId = requireText(skillId, "skillId");
        this.skillName = requireText(skillName, "skillName");
        this.sourcePdf = requireText(sourcePdf, "sourcePdf");
        this.skillGroup = requireText(skillGroup, "skillGroup");
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

    public String getSourcePdf() {
        return sourcePdf;
    }

    public String getSkillGroup() {
        return skillGroup;
    }

    public PaladinSkillDamageModelType getDamageModelType() {
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

    public PaladinDamageTargetMode getTargetMode() {
        return targetMode;
    }

    public PaladinSkillDamageVerificationStatus getVerificationStatus() {
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
