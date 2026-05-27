package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.app.CurrentBuildCalculationService;
import krys.app.CurrentBuildRequest;
import krys.combat.DamageEngine;
import krys.simulation.ManualSimulationService;
import krys.simulation.SimulationStepTrace;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentBuildCalculationSectionsRendererTest {
    private final CurrentBuildCalculationService calculationService = new CurrentBuildCalculationService(
            new ManualSimulationService(new DamageEngine())
    );

    @Test
    void trace_renderuje_zwykly_krytyczny_roll_i_zastosowany_damage() {
        CurrentBuildCalculation calculation = calculationService.calculate(currentBuildRequest(100.0d, 1L));
        SimulationStepTrace firstStep = calculation.getResult().getStepTrace().getFirst();

        String html = CurrentBuildCalculationSectionsRenderer.renderStepTrace(calculation);
        String firstRow = traceRowBySecond(html, 1);

        assertTrue(html.contains("<th>Zwykłe</th>"));
        assertTrue(html.contains("<th>Krytyczne</th>"));
        assertTrue(html.contains("<th>Szansa kryta</th>"));
        assertTrue(html.contains("<th>Roll kryta</th>"));
        assertTrue(html.contains("<th>Kryt?</th>"));
        assertTrue(html.contains("<th>Zastosowane</th>"));
        assertFalse(html.contains("<th>Bezpośrednie</th>"));

        assertTrue(firstStep.isCriticalHit());
        assertTrue(firstRow.contains("<td>" + firstStep.getNormalDirectDamage() + "</td>"));
        assertTrue(firstRow.contains("<td>" + firstStep.getCriticalDirectDamage() + "</td>"));
        assertTrue(firstRow.contains("<td>100,0%</td>"));
        assertTrue(firstRow.contains("<td>Tak</td>"));
        assertTrue(firstRow.contains("<td>" + firstStep.getAppliedDirectDamage() + "</td>"));
        assertTrue(firstRow.contains("<td>" + firstStep.getTotalStepDamage() + "</td>"));
        assertTrue(firstStep.getTotalStepDamage() >= firstStep.getAppliedDirectDamage());
    }

    private static CurrentBuildRequest currentBuildRequest(double criticalChancePercent, long simulationSeed) {
        return new CurrentBuildRequest(
                13,
                8,
                18.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                true,
                true,
                Map.of(SkillId.ADVANCE, new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)),
                List.of(SkillId.ADVANCE),
                1,
                CurrentBuildRequest.DEFAULT_INITIAL_PRIMARY_RESOURCE,
                CurrentBuildRequest.DEFAULT_MAX_PRIMARY_RESOURCE,
                CurrentBuildRequest.DEFAULT_PRIMARY_RESOURCE_REGEN_PER_SECOND,
                CurrentBuildRequest.DEFAULT_SELECTED_PALADIN_OATH_ID,
                CurrentBuildRequest.DEFAULT_INITIAL_ANIMUS,
                CurrentBuildRequest.DEFAULT_MAX_ANIMUS,
                List.of(),
                criticalChancePercent,
                simulationSeed
        );
    }

    private static String traceRowBySecond(String html, int second) {
        String marker = "<tr><td>" + second + "</td>";
        int start = html.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("Brak wiersza trace dla kroku: " + second);
        }
        int end = html.indexOf("</tr>", start);
        if (end < 0) {
            throw new AssertionError("Nie udało się wyciąć wiersza trace dla kroku: " + second);
        }
        return html.substring(start, end + "</tr>".length());
    }
}
