package krys.web;

import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Jawny stan przypisanych umiejętności bohatera i jego paska akcji. */
public final class HeroSkillLoadout {
    private final Map<SkillId, HeroAssignedSkill> assignedSkills;
    private final List<SkillId> actionBarSkills;

    public HeroSkillLoadout(Map<SkillId, HeroAssignedSkill> assignedSkills,
                            List<SkillId> actionBarSkills) {
        this.assignedSkills = Collections.unmodifiableMap(copyAssignedSkills(assignedSkills));
        this.actionBarSkills = Collections.unmodifiableList(sanitizeActionBar(actionBarSkills, this.assignedSkills));
    }

    public static HeroSkillLoadout foundationDefault() {
        return fromCurrentBuildFormData(CurrentBuildFormData.defaultValues());
    }

    public static HeroSkillLoadout fromCurrentBuildFormData(CurrentBuildFormData formData) {
        EnumMap<SkillId, HeroAssignedSkill> assigned = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            CurrentBuildFormData.SkillConfigFormData skillConfig = formData.getSkillConfig(skillId);
            if (skillConfig == null) {
                continue;
            }
            int rank = parseRank(skillConfig.getRank());
            boolean isReferencedOnActionBar = formData.getActionBarSlots().stream().anyMatch(skillId.name()::equals);
            if (rank > 0 || isReferencedOnActionBar) {
                assigned.put(skillId, new HeroAssignedSkill(
                        skillId,
                        rank,
                        skillConfig.isBaseUpgrade(),
                        SkillUpgradeChoice.valueOf(skillConfig.getChoiceUpgrade())
                ));
            }
        }
        if (assigned.isEmpty()) {
            assigned.put(SkillId.ADVANCE, new HeroAssignedSkill(SkillId.ADVANCE, 0, false, SkillUpgradeChoice.NONE));
        }
        return new HeroSkillLoadout(assigned, parseActionBar(formData.getActionBarSlots()));
    }

    public Map<SkillId, HeroAssignedSkill> getAssignedSkills() {
        return assignedSkills;
    }

    public List<SkillId> getAssignedSkillIds() {
        return List.copyOf(assignedSkills.keySet());
    }

    public boolean isAssigned(SkillId skillId) {
        return assignedSkills.containsKey(skillId);
    }

    public HeroAssignedSkill getAssignedSkill(SkillId skillId) {
        return assignedSkills.get(skillId);
    }

    public List<SkillId> getActionBarSkills() {
        return actionBarSkills;
    }

    public HeroSkillLoadout withAssignedSkill(SkillId skillId) {
        if (assignedSkills.containsKey(skillId)) {
            return this;
        }
        EnumMap<SkillId, HeroAssignedSkill> updated = new EnumMap<>(assignedSkills);
        updated.put(skillId, new HeroAssignedSkill(skillId, 0, false, SkillUpgradeChoice.NONE));
        return new HeroSkillLoadout(updated, actionBarSkills);
    }

    public HeroSkillLoadout withoutAssignedSkill(SkillId skillId) {
        if (!assignedSkills.containsKey(skillId)) {
            return this;
        }
        EnumMap<SkillId, HeroAssignedSkill> updated = new EnumMap<>(assignedSkills);
        updated.remove(skillId);
        return new HeroSkillLoadout(updated, actionBarSkills);
    }

    public HeroSkillLoadout withAppliedFormData(CurrentBuildFormData formData) {
        EnumMap<SkillId, HeroAssignedSkill> updated = new EnumMap<>(SkillId.class);
        for (SkillId skillId : assignedSkills.keySet()) {
            updated.put(skillId, HeroAssignedSkill.fromFormData(skillId, formData.getSkillConfig(skillId)));
        }
        return new HeroSkillLoadout(updated, parseActionBar(formData.getActionBarSlots()));
    }

    public CurrentBuildFormData applyToFormData(CurrentBuildFormData baseFormData) {
        EnumMap<SkillId, CurrentBuildFormData.SkillConfigFormData> skillConfigs = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            HeroAssignedSkill assignedSkill = assignedSkills.get(skillId);
            if (assignedSkill == null) {
                skillConfigs.put(skillId, new CurrentBuildFormData.SkillConfigFormData("0", false, SkillUpgradeChoice.NONE.name()));
                continue;
            }
            skillConfigs.put(skillId, assignedSkill.toFormData());
        }

        List<String> actionBarSlots = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            actionBarSlots.add(index < actionBarSkills.size() ? actionBarSkills.get(index).name() : "NONE");
        }

        return new CurrentBuildFormData(
                baseFormData.getLevel(),
                baseFormData.getWeaponDamage(),
                baseFormData.getStrength(),
                baseFormData.getIntelligence(),
                baseFormData.getThorns(),
                baseFormData.getBlockChance(),
                baseFormData.getRetributionChance(),
                baseFormData.getHorizonSeconds(),
                skillConfigs,
                actionBarSlots
        );
    }

    public boolean actionBarDiffersFrom(CurrentBuildFormData formData) {
        return !List.copyOf(formData.getActionBarSlots()).equals(applyToFormData(formData).getActionBarSlots());
    }

    public List<SkillId> getActionBarEligibleSkills() {
        List<SkillId> eligibleSkills = new ArrayList<>();
        for (HeroAssignedSkill assignedSkill : assignedSkills.values()) {
            if (assignedSkill.isLearned()) {
                eligibleSkills.add(assignedSkill.getSkillId());
            }
        }
        return List.copyOf(eligibleSkills);
    }

    private static EnumMap<SkillId, HeroAssignedSkill> copyAssignedSkills(Map<SkillId, HeroAssignedSkill> assignedSkills) {
        EnumMap<SkillId, HeroAssignedSkill> copy = new EnumMap<>(SkillId.class);
        if (assignedSkills == null) {
            return copy;
        }
        for (Map.Entry<SkillId, HeroAssignedSkill> entry : assignedSkills.entrySet()) {
            copy.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    private static List<SkillId> parseActionBar(List<String> rawActionBarSlots) {
        List<SkillId> skills = new ArrayList<>();
        if (rawActionBarSlots == null) {
            return skills;
        }
        for (String rawSkillId : rawActionBarSlots) {
            if (rawSkillId == null || rawSkillId.isBlank() || "NONE".equals(rawSkillId)) {
                continue;
            }
            try {
                skills.add(SkillId.valueOf(rawSkillId));
            } catch (IllegalArgumentException exception) {
                // Ignorujemy niepoprawną wartość i czyścimy pasek do bezpiecznego podzbioru.
            }
        }
        return skills;
    }

    private static List<SkillId> sanitizeActionBar(List<SkillId> requestedActionBarSkills,
                                                   Map<SkillId, HeroAssignedSkill> assignedSkills) {
        LinkedHashSet<SkillId> uniqueSkills = new LinkedHashSet<>();
        if (requestedActionBarSkills != null) {
            for (SkillId skillId : requestedActionBarSkills) {
                HeroAssignedSkill assignedSkill = assignedSkills.get(skillId);
                if (assignedSkill == null || !assignedSkill.isLearned()) {
                    continue;
                }
                uniqueSkills.add(skillId);
                if (uniqueSkills.size() == 4) {
                    break;
                }
            }
        }
        return List.copyOf(uniqueSkills);
    }

    private static int parseRank(String rawRank) {
        if (rawRank == null || rawRank.isBlank()) {
            return 0;
        }
        return Integer.parseInt(rawRank);
    }
}
