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

class BuildSearchCalculationServiceTest {
    private final BuildSearchCalculationService calculationService = new BuildSearchCalculationService(
            new BuildSearchEvaluationService(new ManualSimulationService(new DamageEngine()))
    );

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
