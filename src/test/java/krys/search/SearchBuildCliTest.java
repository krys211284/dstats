package krys.search;

import krys.combat.DamageEngine;
import krys.simulation.ManualSimulationService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        assertTrue(output.contains("Ocenieni kandydaci:"));
        assertTrue(output.contains("== Top wyniki =="));
        assertTrue(output.contains("Build input:"));
        assertTrue(output.contains("Action bar:"));
        assertTrue(output.contains("total damage="));
        assertTrue(output.contains("DPS="));
    }
}
