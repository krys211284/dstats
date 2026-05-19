package krys.simulation;

import krys.combat.DamageBreakdown;
import krys.skill.SkillId;

/** Reprezentatywny debug bezpośredniego hita dla konkretnego skilla użytego w symulacji. */
public final class SkillHitDebugSnapshot {
    private final SkillId skillId;
    private final String skillName;
    private final int skillRank;
    private final DamageBreakdown breakdown;

    public SkillHitDebugSnapshot(SkillId skillId, String skillName, DamageBreakdown breakdown) {
        this(skillId, skillName, 0, breakdown);
    }

    public SkillHitDebugSnapshot(SkillId skillId, String skillName, int skillRank, DamageBreakdown breakdown) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.skillRank = Math.max(0, skillRank);
        this.breakdown = breakdown;
    }

    public SkillId getSkillId() {
        return skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public int getSkillRank() {
        return skillRank;
    }

    public DamageBreakdown getBreakdown() {
        return breakdown;
    }
}
