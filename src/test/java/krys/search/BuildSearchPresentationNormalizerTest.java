package krys.search;

import krys.app.CurrentBuildRequest;
import krys.combat.DamageEngine;
import krys.item.EquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.ItemLibrarySearchCombination;
import krys.itemlibrary.SavedImportedItem;
import krys.simulation.ManualSimulationService;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildSearchPresentationNormalizerTest {
    private final BuildSearchEvaluationService evaluationService = new BuildSearchEvaluationService(
            new ManualSimulationService(new DamageEngine())
    );
    private final BuildSearchPresentationNormalizer normalizer = new BuildSearchPresentationNormalizer();

    @Test
    void powinien_normalizowac_top_wyniki_bez_zmiany_surowej_oceny() {
        BuildSearchEvaluation firstEquivalent = evaluationService.evaluate(new BuildSearchCandidate(createAdvanceClashRequest(
                Map.of(
                        SkillId.ADVANCE, new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.LEFT),
                        SkillId.CLASH, new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT)
                ),
                List.of(SkillId.ADVANCE, SkillId.CLASH)
        )));
        BuildSearchEvaluation secondEquivalent = evaluationService.evaluate(new BuildSearchCandidate(createAdvanceClashRequest(
                Map.of(
                        SkillId.BRANDISH, new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE),
                        SkillId.ADVANCE, new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.LEFT),
                        SkillId.CLASH, new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT)
                ),
                List.of(SkillId.ADVANCE, SkillId.CLASH)
        )));
        BuildSearchEvaluation thirdEquivalent = evaluationService.evaluate(new BuildSearchCandidate(createAdvanceClashRequest(
                Map.of(
                        SkillId.HOLY_BOLT, new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE),
                        SkillId.ADVANCE, new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.LEFT),
                        SkillId.CLASH, new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT)
                ),
                List.of(SkillId.ADVANCE, SkillId.CLASH)
        )));
        BuildSearchEvaluation distinct = evaluationService.evaluate(new BuildSearchCandidate(createAdvanceClashRequest(
                Map.of(
                        SkillId.ADVANCE, new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.LEFT),
                        SkillId.CLASH, new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT)
                ),
                List.of(SkillId.CLASH, SkillId.ADVANCE)
        )));

        BuildSearchPresentationNormalizer.BuildSearchPresentationView presentationView = normalizer.normalize(
                List.of(firstEquivalent, secondEquivalent, thirdEquivalent, distinct),
                5
        );

        assertEquals(2, presentationView.normalizedResultCount());
        assertEquals(2, presentationView.topResults().size());
        assertEquals("Advance -> Clash", presentationView.topResults().get(0).getCandidate().getActionBarDescription());
        assertEquals("Advance rank 5 | base=tak | choice=Wave Dash || Clash rank 5 | base=tak | choice=Punishment",
                presentationView.topResults().get(0).getCandidate().getActionBarSkillsDescription());
        assertEquals("Clash -> Advance", presentationView.topResults().get(1).getCandidate().getActionBarDescription());
        assertEquals(4, List.of(firstEquivalent, secondEquivalent, thirdEquivalent, distinct).size());
    }

    @Test
    void powinien_zachowac_oddzielne_wyniki_dla_roznych_kombinacji_itemow_biblioteki() {
        ItemLibrarySearchCombination firstCombination = new ItemLibrarySearchCombination(
                List.of(new SavedImportedItem(
                        1L,
                        "MAIN_HAND / weapon-a.png",
                        "weapon-a.png",
                        EquipmentSlot.MAIN_HAND,
                        300L,
                        55.0d,
                        0.0d,
                        0.0d,
                        0.0d,
                        0.0d
                )),
                new CurrentBuildImportableStats(300L, 55.0d, 0.0d, 0.0d, 0.0d, 0.0d)
        );
        ItemLibrarySearchCombination secondCombination = new ItemLibrarySearchCombination(
                List.of(new SavedImportedItem(
                        2L,
                        "MAIN_HAND / weapon-b.png",
                        "weapon-b.png",
                        EquipmentSlot.MAIN_HAND,
                        300L,
                        55.0d,
                        0.0d,
                        0.0d,
                        0.0d,
                        0.0d
                )),
                new CurrentBuildImportableStats(300L, 55.0d, 0.0d, 0.0d, 0.0d, 0.0d)
        );
        BuildSearchEvaluation firstEquivalent = evaluationService.evaluate(new BuildSearchCandidate(
                createAdvanceClashRequest(
                        Map.of(SkillId.ADVANCE, new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.LEFT)),
                        List.of(SkillId.ADVANCE)
                ),
                true,
                firstCombination
        ));
        BuildSearchEvaluation secondEquivalent = evaluationService.evaluate(new BuildSearchCandidate(
                createAdvanceClashRequest(
                        Map.of(SkillId.ADVANCE, new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.LEFT)),
                        List.of(SkillId.ADVANCE)
                ),
                true,
                secondCombination
        ));

        BuildSearchPresentationNormalizer.BuildSearchPresentationView presentationView = normalizer.normalize(
                List.of(firstEquivalent, secondEquivalent),
                5
        );

        assertEquals(2, presentationView.normalizedResultCount());
        assertEquals(2, presentationView.topResults().size());
        assertEquals("Broń główna: #1 / weapon-a.png", presentationView.topResults().get(0).getCandidate().getSelectedItemLibraryItemsDescription());
        assertEquals("Broń główna: #2 / weapon-b.png", presentationView.topResults().get(1).getCandidate().getSelectedItemLibraryItemsDescription());
    }

    private static CurrentBuildRequest createAdvanceClashRequest(Map<SkillId, SkillState> learnedSkills, List<SkillId> actionBar) {
        return new CurrentBuildRequest(
                13,
                8,
                18.0d,
                0.0d,
                50.0d,
                50.0d,
                50.0d,
                learnedSkills,
                actionBar,
                9
        );
    }
}
