package krys.web;

import krys.runtime.RuntimeSkillLoadout;
import krys.runtime.legacy.LegacyPaladinRuntimeSkillLoadoutAdapter;
import krys.skill.SkillId;
import krys.skill.SkillState;

import java.util.EnumMap;
import java.util.Objects;

/** Bezstratny most webowego widoku Paladyna do neutralnego stanu profilu. */
public final class LegacyPaladinHeroSkillLoadoutAdapter {
    private LegacyPaladinHeroSkillLoadoutAdapter() { }

    public static RuntimeSkillLoadout toRuntimeLoadout(HeroSkillLoadout loadout) {
        Objects.requireNonNull(loadout, "loadout");
        var states = new EnumMap<SkillId, SkillState>(SkillId.class);
        for (var entry : loadout.getAssignedSkills().entrySet()) {
            var skill = Objects.requireNonNull(entry.getValue(), "assignedSkill");
            if (entry.getKey() != skill.getSkillId()) {
                throw new IllegalArgumentException("Klucz mapy musi odpowiadać HeroAssignedSkill.skillId.");
            }
            states.put(entry.getKey(), new SkillState(skill.getSkillId(), skill.getRank(), skill.isBaseUpgrade(),
                    skill.getChoiceUpgrade(), skill.getRuntimeModifierChoice(),
                    skill.getChoiceGroup1(), skill.getChoiceGroup2(), skill.getChoiceGroup3()));
        }
        return LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(states, loadout.getActionBarSkills());
    }

    public static HeroSkillLoadout toLegacyLoadout(RuntimeSkillLoadout loadout) {
        var view = LegacyPaladinRuntimeSkillLoadoutAdapter.toLegacyView(loadout)
                .orElseThrow(() -> new UnsupportedOperationException("Legacy Paladin view jest dostępny wyłącznie dla Paladyna."));
        var assigned = new EnumMap<SkillId, HeroAssignedSkill>(SkillId.class);
        for (var state : view.learnedSkills().values()) {
            assigned.put(state.getSkillId(), new HeroAssignedSkill(state.getSkillId(), state.getRank(), state.isBaseUpgrade(),
                    state.getChoiceUpgrade(), state.getRuntimeModifierChoice(),
                    state.getChoiceGroup1(), state.getChoiceGroup2(), state.getChoiceGroup3()));
        }
        return new HeroSkillLoadout(assigned, view.actionBar());
    }
}
