package krys.search;

import krys.app.CurrentBuildRequest;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.ItemLibrarySearchCombination;
import krys.itemlibrary.ItemLibraryService;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Generator legalnych kandydatów backendowego searcha M12.
 * Generuje wyłącznie buildy oraz konfiguracje action bara zgodne z kontraktami manual simulation.
 */
public final class BuildSearchCandidateGenerator {
    private final ItemLibraryService itemLibraryService;

    public BuildSearchCandidateGenerator() {
        this(null);
    }

    public BuildSearchCandidateGenerator(ItemLibraryService itemLibraryService) {
        this.itemLibraryService = itemLibraryService;
    }

    public BuildSearchAudit audit(BuildSearchRequest request) {
        List<Map<SkillId, SkillState>> learnedSkillMaps = generateLearnedSkillMaps(request);
        List<ItemLibrarySearchCombination> itemLibraryCombinations = resolveItemLibraryCombinations(request);
        long statSpaceSize = countStatSpace(request);
        long actionBarSpaceSize = countActionBarSpace(learnedSkillMaps, request.getActionBarSizes());
        long legalCandidateCount = generateCandidates(request, learnedSkillMaps, itemLibraryCombinations).size();
        return new BuildSearchAudit(
                request.isUseItemLibrary(),
                legalCandidateCount,
                statSpaceSize,
                itemLibraryCombinations.size(),
                learnedSkillMaps.size(),
                actionBarSpaceSize
        );
    }

    public List<BuildSearchCandidate> generate(BuildSearchRequest request) {
        List<Map<SkillId, SkillState>> learnedSkillMaps = generateLearnedSkillMaps(request);
        List<ItemLibrarySearchCombination> itemLibraryCombinations = resolveItemLibraryCombinations(request);
        return generateCandidates(request, learnedSkillMaps, itemLibraryCombinations);
    }

    private static long countStatSpace(BuildSearchRequest request) {
        return (long) request.getLevelValues().size()
                * request.getWeaponDamageValues().size()
                * request.getStrengthValues().size()
                * request.getIntelligenceValues().size()
                * request.getThornsValues().size()
                * request.getBlockChanceValues().size()
                * request.getRetributionChanceValues().size();
    }

    private List<BuildSearchCandidate> generateCandidates(BuildSearchRequest request,
                                                          List<Map<SkillId, SkillState>> learnedSkillMaps,
                                                          List<ItemLibrarySearchCombination> itemLibraryCombinations) {
        List<BuildSearchCandidate> candidates = new ArrayList<>();

        for (Integer level : request.getLevelValues()) {
            for (Long weaponDamage : request.getWeaponDamageValues()) {
                for (Double strength : request.getStrengthValues()) {
                    for (Double intelligence : request.getIntelligenceValues()) {
                        for (Double thorns : request.getThornsValues()) {
                            for (Double blockChance : request.getBlockChanceValues()) {
                                for (Double retributionChance : request.getRetributionChanceValues()) {
                                    CurrentBuildImportableStats manualBaseStats = new CurrentBuildImportableStats(
                                            weaponDamage,
                                            strength,
                                            intelligence,
                                            thorns,
                                            blockChance,
                                            retributionChance
                                    );
                                    for (ItemLibrarySearchCombination itemLibraryCombination : itemLibraryCombinations) {
                                        CurrentBuildImportableStats effectiveStats = resolveEffectiveStats(
                                                request,
                                                manualBaseStats,
                                                itemLibraryCombination
                                        );
                                        for (Map<SkillId, SkillState> learnedSkills : learnedSkillMaps) {
                                            List<List<SkillId>> actionBars = generateActionBars(learnedSkills, request.getActionBarSizes());
                                            for (List<SkillId> actionBar : actionBars) {
                                                BuildSearchCandidate candidate = createCandidate(
                                                        request,
                                                        level,
                                                        effectiveStats,
                                                        learnedSkills,
                                                        actionBar,
                                                        itemLibraryCombination
                                                );
                                                if (candidate != null) {
                                                    candidates.add(candidate);
                                                }
                                            }
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

    private static long countActionBarSpace(List<Map<SkillId, SkillState>> learnedSkillMaps, List<Integer> actionBarSizes) {
        long count = 0L;
        for (Map<SkillId, SkillState> learnedSkills : learnedSkillMaps) {
            int learnedSkillCount = learnedSkills.size();
            for (Integer actionBarSize : actionBarSizes) {
                if (actionBarSize > learnedSkillCount) {
                    continue;
                }
                count += permutations(learnedSkillCount, actionBarSize);
            }
        }
        return count;
    }

    private List<ItemLibrarySearchCombination> resolveItemLibraryCombinations(BuildSearchRequest request) {
        if (!request.isUseItemLibrary()) {
            return List.of(ItemLibrarySearchCombination.empty());
        }
        if (itemLibraryService == null) {
            throw new IllegalArgumentException("Tryb searcha po bibliotece itemów wymaga skonfigurowanego ItemLibraryService.");
        }
        return itemLibraryService.generateSearchCombinations();
    }

    private CurrentBuildImportableStats resolveEffectiveStats(BuildSearchRequest request,
                                                              CurrentBuildImportableStats manualBaseStats,
                                                              ItemLibrarySearchCombination itemLibraryCombination) {
        if (!request.isUseItemLibrary()) {
            return new CurrentBuildImportableStats(
                    manualBaseStats.getWeaponDamage() + request.getActiveHeroItemsContribution().getWeaponDamage(),
                    manualBaseStats.getStrength() + request.getActiveHeroItemsContribution().getStrength(),
                    manualBaseStats.getIntelligence() + request.getActiveHeroItemsContribution().getIntelligence(),
                    manualBaseStats.getThorns() + request.getActiveHeroItemsContribution().getThorns(),
                    manualBaseStats.getBlockChance() + request.getActiveHeroItemsContribution().getBlockChance(),
                    manualBaseStats.getRetributionChance() + request.getActiveHeroItemsContribution().getRetributionChance()
            );
        }
        return itemLibraryService.resolveEffectiveStats(manualBaseStats, itemLibraryCombination);
    }

    private static BuildSearchCandidate createCandidate(BuildSearchRequest request,
                                                        int level,
                                                        CurrentBuildImportableStats effectiveStats,
                                                        Map<SkillId, SkillState> learnedSkills,
                                                        List<SkillId> actionBar,
                                                        ItemLibrarySearchCombination itemLibraryCombination) {
        try {
            return new BuildSearchCandidate(
                    new CurrentBuildRequest(
                            level,
                            effectiveStats.getWeaponDamage(),
                            effectiveStats.getStrength(),
                            effectiveStats.getIntelligence(),
                            effectiveStats.getThorns(),
                            effectiveStats.getBlockChance(),
                            effectiveStats.getRetributionChance(),
                            learnedSkills,
                            actionBar,
                            request.getHorizonSeconds()
                    ),
                    request.isUseItemLibrary(),
                    itemLibraryCombination
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static long permutations(int itemCount, int selectionSize) {
        long result = 1L;
        for (int value = 0; value < selectionSize; value++) {
            result *= (itemCount - value);
        }
        return result;
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
