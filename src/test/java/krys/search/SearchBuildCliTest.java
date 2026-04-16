package krys.search;

import krys.combat.DamageEngine;
import krys.simulation.ManualSimulationService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchBuildCliTest {

    @Test
    void powinien_renderowac_tekstowy_output_top_wynikow_searcha() {
        BuildSearchResult result = new BuildSearchCalculationService(
                new BuildSearchEvaluationService(new ManualSimulationService(new DamageEngine()))
        ).calculate(new BuildSearchRequest(
                List.of(13),
                List.of(8L),
                List.of(18.0d),
                List.of(0.0d),
                List.of(50.0d),
                List.of(50.0d),
                List.of(50.0d),
                BuildSearchReferenceRequests.createFoundationM9().getSkillSpaces(),
                List.of(1),
                9,
                2
        ));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
        try {
            SearchBuildCli.printResult(result);
        } finally {
            System.setOut(originalOut);
        }

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("=== Znajdź najlepszy build ==="));
        assertTrue(output.contains("Audit / preflight:"));
        assertTrue(output.contains("Liczba legalnych kandydatów:"));
        assertTrue(output.contains("Rozmiar przestrzeni statów:"));
        assertTrue(output.contains("Rozmiar przestrzeni skilli:"));
        assertTrue(output.contains("Rozmiar przestrzeni action bara:"));
        assertTrue(output.contains("Skala search space:"));
        assertTrue(output.contains("Ocenieni kandydaci:"));
        assertTrue(output.contains("Wyniki po normalizacji:"));
        assertTrue(output.contains("== Top wyniki po normalizacji =="));
        assertTrue(output.contains("Build input:"));
        assertTrue(output.contains("Action bar skills:"));
        assertTrue(output.contains("Action bar:"));
        assertTrue(output.contains("total damage="));
        assertTrue(output.contains("DPS="));
    }

    @Test
    void powinien_renderowac_minimalny_progress_dla_cli_searcha() {
        BuildSearchRequest request = new BuildSearchRequest(
                List.of(13),
                List.of(8L),
                List.of(18.0d),
                List.of(0.0d),
                List.of(0.0d),
                List.of(0.0d),
                List.of(0.0d),
                createSmallProgressSkillSpaces(),
                List.of(1),
                9,
                2
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
        try {
            BuildSearchResult result = new BuildSearchCalculationService(
                    new BuildSearchEvaluationService(new ManualSimulationService(new DamageEngine()))
            ).calculate(request, new SearchBuildCliProgressReporter());
            SearchBuildCli.printResult(result);
        } finally {
            System.setOut(originalOut);
        }

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("== Start searcha =="));
        assertTrue(output.contains("Postęp: 1/2"));
        assertTrue(output.contains("Postęp: 2/2"));
        assertTrue(output.contains("== Search zakończony =="));
        assertTrue(output.contains("Skala search space: mała"));
    }

    private static Map<krys.skill.SkillId, BuildSearchSkillSpace> createSmallProgressSkillSpaces() {
        Map<krys.skill.SkillId, BuildSearchSkillSpace> skillSpaces = new EnumMap<>(krys.skill.SkillId.class);
        skillSpaces.put(krys.skill.SkillId.BRANDISH, new BuildSearchSkillSpace(
                krys.skill.SkillId.BRANDISH,
                List.of(0),
                List.of(false),
                List.of(krys.skill.SkillUpgradeChoice.NONE)
        ));
        skillSpaces.put(krys.skill.SkillId.HOLY_BOLT, new BuildSearchSkillSpace(
                krys.skill.SkillId.HOLY_BOLT,
                List.of(0),
                List.of(false),
                List.of(krys.skill.SkillUpgradeChoice.NONE)
        ));
        skillSpaces.put(krys.skill.SkillId.CLASH, new BuildSearchSkillSpace(
                krys.skill.SkillId.CLASH,
                List.of(0),
                List.of(false),
                List.of(krys.skill.SkillUpgradeChoice.NONE)
        ));
        skillSpaces.put(krys.skill.SkillId.ADVANCE, new BuildSearchSkillSpace(
                krys.skill.SkillId.ADVANCE,
                List.of(5),
                List.of(true),
                List.of(krys.skill.SkillUpgradeChoice.NONE, krys.skill.SkillUpgradeChoice.LEFT)
        ));
        return skillSpaces;
    }
}
