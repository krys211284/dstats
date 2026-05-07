package krys.paladin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Bazowy wpis umiejętności w rejestrze drzewa Paladyna. */
public final class PaladinTreeSkill {
    private final String skillId;
    private final String skillName;
    private final String sourcePdf;
    private final String skillGroup;
    private final Integer baseDamagePercentAtRank1;
    private final Integer baseDamagePercentAtTreeMaxRank;
    private final DamagePercentRankTable baseDamagePercentRanks;
    private final PaladinSkillTreeType type;
    private final PaladinSkillTreeStatus status;
    private final List<PaladinSkillUpgradeGroup> upgradeGroups;
    private final String notes;

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes) {
        this(skillId, skillName, sourcePdf, skillGroup, null, null, type, status, upgradeGroups, notes);
    }

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            Integer baseDamagePercentAtRank1,
                            Integer baseDamagePercentAtTreeMaxRank,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes) {
        this(skillId, skillName, sourcePdf, skillGroup, baseDamagePercentAtRank1, baseDamagePercentAtTreeMaxRank,
                DamagePercentRankTable.empty(), type, status, upgradeGroups, notes);
    }

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            DamagePercentRankTable baseDamagePercentRanks,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes) {
        this(skillId, skillName, sourcePdf, skillGroup, null, null, baseDamagePercentRanks, type, status, upgradeGroups, notes);
    }

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            Integer baseDamagePercentAtRank1,
                            Integer baseDamagePercentAtTreeMaxRank,
                            DamagePercentRankTable baseDamagePercentRanks,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes) {
        this.skillId = requireText(skillId, "skillId");
        this.skillName = requireText(skillName, "skillName");
        this.sourcePdf = requireText(sourcePdf, "sourcePdf");
        this.skillGroup = requireText(skillGroup, "skillGroup");
        this.baseDamagePercentAtRank1 = baseDamagePercentAtRank1;
        this.baseDamagePercentAtTreeMaxRank = baseDamagePercentAtTreeMaxRank;
        this.baseDamagePercentRanks = Objects.requireNonNull(baseDamagePercentRanks, "baseDamagePercentRanks");
        this.type = Objects.requireNonNull(type, "type");
        this.status = Objects.requireNonNull(status, "status");
        this.upgradeGroups = Collections.unmodifiableList(new ArrayList<>(upgradeGroups));
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

    public Integer getBaseDamagePercentAtRank1() {
        if (!baseDamagePercentRanks.isEmpty()) {
            return baseDamagePercentRanks.damagePercentAtRank1();
        }
        return baseDamagePercentAtRank1;
    }

    public Integer getBaseDamagePercentAtTreeMaxRank() {
        if (!baseDamagePercentRanks.isEmpty()) {
            return baseDamagePercentRanks.damagePercentAtTreeMaxRank(DamagePercentRankTable.MAX_RANK);
        }
        return baseDamagePercentAtTreeMaxRank;
    }

    public DamagePercentRankTable getBaseDamagePercentRanks() {
        return baseDamagePercentRanks;
    }

    public Integer damagePercentAtRank(int rank) {
        return baseDamagePercentRanks.damagePercentAtRank(rank);
    }

    public Integer damagePercentAtRank1() {
        return getBaseDamagePercentAtRank1();
    }

    public Integer damagePercentAtTreeMaxRank(int treeMaxRank) {
        if (!baseDamagePercentRanks.isEmpty()) {
            return baseDamagePercentRanks.damagePercentAtTreeMaxRank(treeMaxRank);
        }
        if (treeMaxRank == DamagePercentRankTable.MAX_RANK) {
            return baseDamagePercentAtTreeMaxRank;
        }
        return null;
    }

    public PaladinSkillTreeType getType() {
        return type;
    }

    public PaladinSkillTreeStatus getStatus() {
        return status;
    }

    public List<PaladinSkillUpgradeGroup> getUpgradeGroups() {
        return upgradeGroups;
    }

    public List<UpgradeDamageImpact> getUpgradeDamageImpacts() {
        return upgradeGroups.stream()
                .flatMap(group -> group.getUpgrades().stream()
                        .map(upgrade -> UpgradeDamageImpact.fromUpgrade(group.getId(), upgrade)))
                .toList();
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
