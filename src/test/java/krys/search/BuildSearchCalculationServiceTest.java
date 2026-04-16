package krys.search;

import krys.combat.DamageEngine;
import krys.simulation.ManualSimulationService;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

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

        assertEquals(candidateGenerator.generate(request).size(), result.getEvaluatedCandidateCount());
        assertTrue(result.getNormalizedResultCount() < result.getEvaluatedCandidateCount());
        assertEquals(Math.min(request.getTopResultsLimit(), result.getNormalizedResultCount()), result.getTopResults().size());
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
}
