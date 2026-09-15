package krys.itemimport.image;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pilnuje, ze POC OCR PART_1 nie wraca do systemowego Windows OCR. */
class Part1OcrWindowsExclusionTest {
    @Test
    void shouldNotReferenceWindowsOcrRuntimeInProductionCodeOrPom() throws Exception {
        List<String> forbiddenTokens = List.of(
                "Windows.Media." + "Ocr",
                "Windows OCR"
        );

        try (Stream<Path> paths = Stream.concat(
                Files.walk(Path.of("src/main/java")).filter(Files::isRegularFile),
                Stream.of(Path.of("pom.xml"))
        )) {
            List<Path> offenders = paths
                    .filter(path -> containsAny(path, forbiddenTokens))
                    .toList();

            assertTrue(offenders.isEmpty(), "Zakazane referencje Windows OCR: " + offenders);
        }
    }

    private static boolean containsAny(Path path, List<String> forbiddenTokens) {
        try {
            String content = Files.readString(path);
            return forbiddenTokens.stream().anyMatch(content::contains);
        } catch (Exception exception) {
            throw new IllegalStateException("Nie udalo sie sprawdzic pliku: " + path, exception);
        }
    }
}
