package krys.search;

import krys.app.CurrentBuildCalculationService;
import krys.combat.DamageEngine;
import krys.item.EquipmentSlot;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.simulation.ManualSimulationService;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildSearchCalculationServiceTest {
    private final BuildSearchCalculationService calculationService = new BuildSearchCalculationService(
            new BuildSearchEvaluationService(new ManualSimulationService(new DamageEngine()))
    );
    private final BuildSearchCandidateGenerator candidateGenerator = new BuildSearchCandidateGenerator();

    @Test
    void powinien_rankowac_wyniki_deterministycznie_po_total_damage_i_dps() {
        BuildSearchRequest request = new BuildSearchRequest(
                List.of(13),
                List.of(8L),
                List.of(18.0d),
                List.of(0.0d),
                List.of(0.0d),
                List.of(0.0d),
                List.of(0.0d),
                createRankingSkillSpaces(),
                List.of(1),
                9,
                3
        );

        BuildSearchResult result = calculationService.calculate(request);

        assertEquals(3, result.getEvaluatedCandidateCount());
        assertEquals(3, result.getNormalizedResultCount());
        assertEquals(3, result.getTopResults().size());
        assertEquals("Advance", result.getTopResults().get(0).getCandidate().getActionBarDescription());
        assertEquals(315L, result.getTopResults().get(0).getSimulationResult().getTotalDamage());
        assertEquals("Wave Dash", result.getTopResults().get(0).getCandidate().getLearnedSkillsDescription().split("choice=")[1]);
        assertEquals("Advance", result.getTopResults().get(1).getCandidate().getActionBarDescription());
        assertEquals(135L, result.getTopResults().get(1).getSimulationResult().getTotalDamage());
        assertEquals("Brak", result.getTopResults().get(1).getCandidate().getLearnedSkillsDescription().split("choice=")[1]);
        assertEquals("Advance", result.getTopResults().get(2).getCandidate().getActionBarDescription());
        assertEquals(66L, result.getTopResults().get(2).getSimulationResult().getTotalDamage());
        assertEquals("Flash of the Blade", result.getTopResults().get(2).getCandidate().getLearnedSkillsDescription().split("choice=")[1]);
    }

    @Test
    void powinien_zachowac_liczbe_ocenionych_kandydatow_i_zmniejszyc_liczbe_wynikow_uzytkowych() {
        BuildSearchRequest request = BuildSearchReferenceRequests.createFoundationM9();

        BuildSearchResult result = calculationService.calculate(request);

        assertEquals(2949L, result.getAudit().getLegalCandidateCount());
        assertEquals(1L, result.getAudit().getStatSpaceSize());
        assertEquals(countExpectedSkillVariants(request), result.getAudit().getSkillSpaceSize());
        assertEquals(2949L, result.getAudit().getActionBarSpaceSize());
        assertEquals(BuildSearchSpaceScale.LARGE, result.getAudit().getSpaceScale());
        assertEquals(candidateGenerator.generate(request).size(), result.getEvaluatedCandidateCount());
        assertEquals(result.getAudit().getLegalCandidateCount(), result.getEvaluatedCandidateCount());
        assertTrue(result.getNormalizedResultCount() < result.getEvaluatedCandidateCount());
        assertEquals(Math.min(request.getTopResultsLimit(), result.getNormalizedResultCount()), result.getTopResults().size());
        assertEquals("Advance -> Clash", result.getTopResults().get(0).getCandidate().getActionBarDescription());
        assertEquals(439L, result.getTopResults().get(0).getSimulationResult().getTotalDamage());
        assertEquals(48.77777777777778d, result.getTopResults().get(0).getSimulationResult().getDps(), 0.0000000001d);
    }

    @Test
    void powinien_zachowac_deterministyczny_porzadek_po_normalizacji() {
        BuildSearchRequest request = BuildSearchReferenceRequests.createFoundationM9();

        BuildSearchResult firstRun = calculationService.calculate(request);
        BuildSearchResult secondRun = calculationService.calculate(request);

        assertEquals(firstRun.getEvaluatedCandidateCount(), secondRun.getEvaluatedCandidateCount());
        assertEquals(firstRun.getNormalizedResultCount(), secondRun.getNormalizedResultCount());
        assertEquals(firstRun.getTopResults().size(), secondRun.getTopResults().size());
        for (int index = 0; index < firstRun.getTopResults().size(); index++) {
            assertEquals(
                    firstRun.getTopResults().get(index).getCandidate().getInputProfileDescription(),
                    secondRun.getTopResults().get(index).getCandidate().getInputProfileDescription()
            );
            assertEquals(
                    firstRun.getTopResults().get(index).getCandidate().getActionBarSkillsDescription(),
                    secondRun.getTopResults().get(index).getCandidate().getActionBarSkillsDescription()
            );
            assertEquals(
                    firstRun.getTopResults().get(index).getSimulationResult().getTotalDamage(),
                    secondRun.getTopResults().get(index).getSimulationResult().getTotalDamage()
            );
        }
    }

    @Test
    void powinien_wyliczac_preflight_spojny_z_liczba_legalnych_kandydatow() {
        BuildSearchRequest request = new BuildSearchRequest(
                List.of(13),
                List.of(8L),
                List.of(18.0d),
                List.of(0.0d),
                List.of(0.0d),
                List.of(0.0d),
                List.of(0.0d),
                createRankingSkillSpaces(),
                List.of(1),
                9,
                3
        );

        BuildSearchAudit audit = calculationService.preflight(request);

        assertEquals(1L, audit.getStatSpaceSize());
        assertEquals(3L, audit.getSkillSpaceSize());
        assertEquals(3L, audit.getActionBarSpaceSize());
        assertEquals(3L, audit.getLegalCandidateCount());
        assertEquals(BuildSearchSpaceScale.SMALL, audit.getSpaceScale());
        assertEquals(candidateGenerator.generate(request).size(), audit.getLegalCandidateCount());
    }

    @Test
    void powinien_uzywac_tego_samego_runtime_dla_searcha_po_bibliotece_itemow() throws Exception {
        Path tempDirectory = Files.createTempDirectory("search-library-runtime");
        ItemLibraryService itemLibraryService = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        itemLibraryService.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "weapon-a.png",
                EquipmentSlot.MAIN_HAND,
                300L,
                55.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        ));
        itemLibraryService.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "weapon-b.png",
                EquipmentSlot.MAIN_HAND,
                321L,
                60.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        ));
        itemLibraryService.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "shield-a.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                11.0d,
                90.0d,
                18.0d,
                25.0d
        ));
        BuildSearchCalculationService calculationServiceWithLibrary = new BuildSearchCalculationService(
                new BuildSearchEvaluationService(new ManualSimulationService(new DamageEngine())),
                itemLibraryService
        );
        BuildSearchRequest request = new BuildSearchRequest(
                true,
                List.of(13),
                List.of(0L),
                List.of(18.0d),
                List.of(0.0d),
                List.of(50.0d),
                List.of(50.0d),
                List.of(50.0d),
                createSingleAdvanceSkillSpace(),
                List.of(1),
                9,
                4
        );

        BuildSearchResult result = calculationServiceWithLibrary.calculate(request);

        assertTrue(result.getAudit().isUsingItemLibrary());
        assertEquals(6L, result.getAudit().getItemLibraryCombinationSpaceSize());
        assertEquals(4, result.getEvaluatedCandidateCount());
        assertEquals("Włączony", result.getTopResults().getFirst().getCandidate().getItemLibraryModeDescription());
        assertTrue(result.getTopResults().getFirst().getCandidate().getSelectedItemLibraryItemsDescription().contains("Broń"));
        assertTrue(result.getTopResults().getFirst().getCandidate().getSelectedItemLibraryItemsDescription().contains("Tarcza"));
        assertTrue(result.getTopResults().getFirst().getCandidate().getItemLibraryContributionDescription().contains("obrażenia broni=321"));

        krys.app.CurrentBuildCalculation expectedCalculation = new CurrentBuildCalculationService(
                new ManualSimulationService(new DamageEngine())
        ).calculate(result.getTopResults().getFirst().getCandidate().getCurrentBuildRequest());

        assertEquals(expectedCalculation.getResult().getTotalDamage(), result.getTopResults().getFirst().getSimulationResult().getTotalDamage());
        assertEquals(expectedCalculation.getResult().getDps(), result.getTopResults().getFirst().getSimulationResult().getDps(), 0.0000000001d);

        BuildSearchResult secondRun = calculationServiceWithLibrary.calculate(request);

        assertEquals(
                result.getTopResults().getFirst().getCandidate().getSelectedItemLibraryItemsDescription(),
                secondRun.getTopResults().getFirst().getCandidate().getSelectedItemLibraryItemsDescription()
        );
        assertEquals(
                result.getTopResults().getFirst().getSimulationResult().getTotalDamage(),
                secondRun.getTopResults().getFirst().getSimulationResult().getTotalDamage()
        );
    }

    private static Map<SkillId, BuildSearchSkillSpace> createRankingSkillSpaces() {
        Map<SkillId, BuildSearchSkillSpace> skillSpaces = new EnumMap<>(SkillId.class);
        skillSpaces.put(SkillId.BRANDISH, new BuildSearchSkillSpace(
                SkillId.BRANDISH,
                List.of(0),
                List.of(false),
                List.of(SkillUpgradeChoice.NONE)
        ));
        skillSpaces.put(SkillId.HOLY_BOLT, new BuildSearchSkillSpace(
                SkillId.HOLY_BOLT,
                List.of(0),
                List.of(false),
                List.of(SkillUpgradeChoice.NONE)
        ));
        skillSpaces.put(SkillId.CLASH, new BuildSearchSkillSpace(
                SkillId.CLASH,
                List.of(0),
                List.of(false),
                List.of(SkillUpgradeChoice.NONE)
        ));
        skillSpaces.put(SkillId.ADVANCE, new BuildSearchSkillSpace(
                SkillId.ADVANCE,
                List.of(5),
                List.of(true),
                List.of(SkillUpgradeChoice.NONE, SkillUpgradeChoice.LEFT, SkillUpgradeChoice.RIGHT)
        ));
        return skillSpaces;
    }

    private static Map<SkillId, BuildSearchSkillSpace> createSingleAdvanceSkillSpace() {
        Map<SkillId, BuildSearchSkillSpace> skillSpaces = new EnumMap<>(SkillId.class);
        skillSpaces.put(SkillId.BRANDISH, new BuildSearchSkillSpace(
                SkillId.BRANDISH,
                List.of(0),
                List.of(false),
                List.of(SkillUpgradeChoice.NONE)
        ));
        skillSpaces.put(SkillId.HOLY_BOLT, new BuildSearchSkillSpace(
                SkillId.HOLY_BOLT,
                List.of(0),
                List.of(false),
                List.of(SkillUpgradeChoice.NONE)
        ));
        skillSpaces.put(SkillId.CLASH, new BuildSearchSkillSpace(
                SkillId.CLASH,
                List.of(0),
                List.of(false),
                List.of(SkillUpgradeChoice.NONE)
        ));
        skillSpaces.put(SkillId.ADVANCE, new BuildSearchSkillSpace(
                SkillId.ADVANCE,
                List.of(5),
                List.of(true),
                List.of(SkillUpgradeChoice.LEFT)
        ));
        return skillSpaces;
    }

    private static long countExpectedSkillVariants(BuildSearchRequest request) {
        long total = 1L;
        for (SkillId skillId : SkillId.values()) {
            total *= countLegalStates(request.getSkillSpace(skillId));
        }
        return total;
    }

    private static long countLegalStates(BuildSearchSkillSpace skillSpace) {
        long count = 0L;
        for (Integer rank : skillSpace.getRankValues()) {
            if (rank == 0) {
                count++;
                continue;
            }
            if (skillSpace.getBaseUpgradeValues().contains(Boolean.FALSE)) {
                count++;
            }
            if (skillSpace.getBaseUpgradeValues().contains(Boolean.TRUE)) {
                count += skillSpace.getChoiceUpgradeValues().size();
            }
        }
        return count;
    }
}
