package krys.verification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje kontrakt ignorowania lokalnych plików diagnostycznych. */
class GitIgnoreContractTest {
    @Test
    void shouldIgnoreDiagnosticLogsDirectory() throws Exception {
        Path gitIgnore = Path.of(".gitignore");

        assertTrue(Files.exists(gitIgnore), ".gitignore musi istnieć.");
        assertTrue(Files.readAllLines(gitIgnore).stream()
                        .map(String::trim)
                        .anyMatch("logs/"::equals),
                ".gitignore musi zawierać wpis logs/.");
    }
}
