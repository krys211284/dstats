package krys.simulation;

import krys.combat.DamageBreakdown;
import krys.skill.SkillId;

/** Reprezentatywny debug bezpośredniego hita dla konkretnego skilla użytego w symulacji. */
public final class SkillHitDebugSnapshot {
    private final SkillId skillId;
    private final String skillName;
    private final DamageBreakdown breakdown;

    public SkillHitDebugSnapshot(SkillId skillId, String skillName, DamageBreakdown breakdown) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.breakdown = breakdown;
    }

    public SkillId getSkillId() {
        return skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public DamageBreakdown getBreakdown() {
        return breakdown;
    }
}
