package krys.search;

import krys.app.CurrentBuildRequest;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Generator legalnych kandydatów backendowego searcha M9.
 * Generuje wyłącznie buildy oraz konfiguracje action bara zgodne z kontraktami manual simulation.
 */
public final class BuildSearchCandidateGenerator {

    public List<BuildSearchCandidate> generate(BuildSearchRequest request) {
        List<Map<SkillId, SkillState>> learnedSkillMaps = generateLearnedSkillMaps(request);
        List<BuildSearchCandidate> candidates = new ArrayList<>();

        for (Integer level : request.getLevelValues()) {
            for (Long weaponDamage : request.getWeaponDamageValues()) {
                for (Double strength : request.getStrengthValues()) {
                    for (Double intelligence : request.getIntelligenceValues()) {
                        for (Double thorns : request.getThornsValues()) {
                            for (Double blockChance : request.getBlockChanceValues()) {
                                for (Double retributionChance : request.getRetributionChanceValues()) {
                                    for (Map<SkillId, SkillState> learnedSkills : learnedSkillMaps) {
                                        List<List<SkillId>> actionBars = generateActionBars(learnedSkills, request.getActionBarSizes());
                                        for (List<SkillId> actionBar : actionBars) {
                                            candidates.add(new BuildSearchCandidate(new CurrentBuildRequest(
                                                    level,
                                                    weaponDamage,
                                                    strength,
                                                    intelligence,
                                                    thorns,
                                                    blockChance,
                                                    retributionChance,
                                                    learnedSkills,
                                                    actionBar,
                                                    request.getHorizonSeconds()
                                            )));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return candidates;
    }

    private static List<Map<SkillId, SkillState>> generateLearnedSkillMaps(BuildSearchRequest request) {
        List<Map<SkillId, SkillState>> variants = new ArrayList<>();
        generateLearnedSkillMapsRecursive(request, SkillId.values(), 0, new EnumMap<>(SkillId.class), variants);
        return variants;
    }

    private static void generateLearnedSkillMapsRecursive(BuildSearchRequest request,
                                                          SkillId[] orderedSkillIds,
                                                          int index,
                                                          EnumMap<SkillId, SkillState> current,
                                                          List<Map<SkillId, SkillState>> variants) {
        if (index >= orderedSkillIds.length) {
            variants.add(new EnumMap<>(current));
            return;
        }

        SkillId skillId = orderedSkillIds[index];
        for (SkillState skillState : generateLegalStates(request.getSkillSpace(skillId))) {
            if (skillState.getRank() > 0) {
                current.put(skillId, skillState);
            } else {
                current.remove(skillId);
            }
            generateLearnedSkillMapsRecursive(request, orderedSkillIds, index + 1, current, variants);
        }
    }

    private static List<SkillState> generateLegalStates(BuildSearchSkillSpace skillSpace) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<SkillState> states = new ArrayList<>();
        SkillId skillId = skillSpace.getSkillId();

        for (Integer rank : skillSpace.getRankValues()) {
            if (rank == 0) {
                addState(states, seen, new SkillState(skillId, 0, false, SkillUpgradeChoice.NONE));
                continue;
            }

            if (skillSpace.getBaseUpgradeValues().contains(Boolean.FALSE)) {
                addState(states, seen, new SkillState(skillId, rank, false, SkillUpgradeChoice.NONE));
            }
            if (skillSpace.getBaseUpgradeValues().contains(Boolean.TRUE)) {
                for (SkillUpgradeChoice choiceUpgrade : skillSpace.getChoiceUpgradeValues()) {
                    addState(states, seen, new SkillState(skillId, rank, true, choiceUpgrade));
                }
            }
        }

        return states;
    }

    private static void addState(List<SkillState> states, LinkedHashSet<String> seen, SkillState state) {
        String key = state.getSkillId() + "|" + state.getRank() + "|" + state.isBaseUpgrade() + "|" + state.getChoiceUpgrade();
        if (seen.add(key)) {
            states.add(state);
        }
    }

    private static List<List<SkillId>> generateActionBars(Map<SkillId, SkillState> learnedSkills, List<Integer> actionBarSizes) {
        List<SkillId> learnedSkillIds = new ArrayList<>(learnedSkills.keySet());
        List<List<SkillId>> actionBars = new ArrayList<>();
        for (Integer actionBarSize : actionBarSizes) {
            if (actionBarSize > learnedSkillIds.size()) {
                continue;
            }
            generateActionBarsRecursive(learnedSkillIds, actionBarSize, new boolean[learnedSkillIds.size()], new ArrayList<>(), actionBars);
        }
        return actionBars;
    }

    private static void generateActionBarsRecursive(List<SkillId> learnedSkillIds,
                                                    int targetSize,
                                                    boolean[] used,
                                                    List<SkillId> current,
                                                    List<List<SkillId>> actionBars) {
        if (current.size() == targetSize) {
            actionBars.add(List.copyOf(current));
            return;
        }

        for (int index = 0; index < learnedSkillIds.size(); index++) {
            if (used[index]) {
                continue;
            }
            used[index] = true;
            current.add(learnedSkillIds.get(index));
            generateActionBarsRecursive(learnedSkillIds, targetSize, used, current, actionBars);
            current.remove(current.size() - 1);
            used[index] = false;
        }
    }
}
