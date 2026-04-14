package krys.simulation;

import krys.skill.SkillId;

/** Stan pojedynczego skilla z paska zapisany w trace tego samego ticku, z którego pochodzi wybór LRU. */
public final class SkillBarStateTrace {
    private final SkillId skillId;
    private final String skillName;
    private final int barIndex;
    private final int rank;
    private final boolean legalActive;
    private final boolean onCooldown;
    private final boolean hasRequiredResource;
    private final boolean neverUsed;
    private final Integer lastUsedSecond;
    private final boolean selected;

    public SkillBarStateTrace(SkillId skillId,
                              String skillName,
                              int barIndex,
                              int rank,
                              boolean legalActive,
                              boolean onCooldown,
                              boolean hasRequiredResource,
                              boolean neverUsed,
                              Integer lastUsedSecond,
                              boolean selected) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.barIndex = barIndex;
        this.rank = rank;
        this.legalActive = legalActive;
        this.onCooldown = onCooldown;
        this.hasRequiredResource = hasRequiredResource;
        this.neverUsed = neverUsed;
        this.lastUsedSecond = lastUsedSecond;
        this.selected = selected;
    }

    public SkillId getSkillId() {
        return skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public int getBarIndex() {
        return barIndex;
    }

    public int getRank() {
        return rank;
    }

    public boolean isLegalActive() {
        return legalActive;
    }

    public boolean isOnCooldown() {
        return onCooldown;
    }

    public boolean hasRequiredResource() {
        return hasRequiredResource;
    }

    public boolean isNeverUsed() {
        return neverUsed;
    }

    public Integer getLastUsedSecond() {
        return lastUsedSecond;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isLegalCandidate() {
        return legalActive && !onCooldown && hasRequiredResource;
    }
}
