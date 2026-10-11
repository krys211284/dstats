package krys.itemimport.image;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrGoldenFixturesTest {
    @Test
    void shouldPreserveBlockMetadataAndFullProfileEvidence() {
        for (var source : List.of(
                new Source(List.of("tarcza1.png", "tarcza2.png"), StructuralItemPartSlot.PART_2),
                new Source(List.of("tarcza1.png", "tarcza2.png"), StructuralItemPartSlot.PART_3),
                new Source(List.of("dziedzic-zatracenia-helm-full.png"), StructuralItemPartSlot.PART_3),
                new Source(List.of("verathiel-miecz-v2.png"), StructuralItemPartSlot.PART_3))) {
            for (var fixture : OcrGoldenFixtures.blocks(source.files(), source.slot())) {
                assertEquals(fixture.block().getSourcePart(), fixture.read().getSourcePart());
                assertEquals(fixture.block().getOrder(), fixture.read().getBlockOrder());
                assertEquals(fixture.block().getAnchorType(), fixture.read().getAnchorType());
                assertTrue(fixture.block().getImageBytes().length > 0);
                assertTrue(fixture.read().getProfileResults().stream().anyMatch(profile ->
                        profile.getRawText().equals(fixture.read().getRawText())
                                && profile.getProfile().equals(fixture.read().getPreprocessing())));
                var rebuilt = PropertyBlockNumericEvidenceAggregator.aggregate(fixture.read().getProfileResults());
                OcrGoldenFixtures.assertSameRead(fixture.read().getNumericEvidence(), rebuilt.evidence());
            }
        }
    }

    @Test
    void shouldPreservePart1SubRegionsAsRealDiagnosticImages() throws Exception {
        for (var files : List.of(
                List.of("buty.png"), List.of("dziedzic-zatracenia-helm-full.png"),
                List.of("dziedzic-zatracenia-helm-1.png", "dziedzic-zatracenia-helm-2.png"),
                List.of("tarcza.png"), List.of("tarcza1.png"), List.of("tarcza1.png", "tarcza2.png"),
                List.of("verathiel-miecz-v2.png"))) {
            var reads = OcrGoldenFixtures.part1(files.toArray(String[]::new));
            assertTrue(reads.stream().anyMatch(read -> read.getType() == Part1RoiType.NAME_AND_STARS));
            for (var read : reads) {
                for (var region : read.getSubRegions()) {
                    var image = ImageIO.read(new ByteArrayInputStream(region.getImageBytes()));
                    assertNotNull(image);
                    assertEquals(region.getWidth(), image.getWidth());
                    assertEquals(region.getHeight(), image.getHeight());
                }
            }
        }
    }

    @Test
    void shouldKeepSourceImagesAndBundledModelIdenticalToFixtureProvenance() throws Exception {
        Properties provenance = new Properties();
        try (var input = getClass().getResourceAsStream("/ocr/provenance.properties")) {
            provenance.load(input);
        }
        for (String name : provenance.stringPropertyNames()) {
            Path source;
            if (name.startsWith("sha256.items/")) {
                source = Path.of("src/test/resources", name.substring("sha256.".length()));
            } else if (name.endsWith("/tessdata/pol.traineddata")) {
                source = Path.of(name.substring("sha256.".length()));
            } else continue;
            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)));
            assertEquals(provenance.getProperty(name), sha256, source.toString());
        }
    }

    private record Source(List<String> files, StructuralItemPartSlot slot) {
    }
}
