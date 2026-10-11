package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.opencv.OpenCvHorizontalDividerDetector;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Techniczny smoke test Tess4J dla surowego OCR pojedynczego ROI PART_1. */
@OcrIntegrationTest
class TesseractPart1RoiOcrReaderTest {
    private static final Map<List<String>, List<Part1RoiOcrResult>> REAL_READS = new ConcurrentHashMap<>();
    @Test
    void shouldReadRawTextFromSinglePart1RoiUsingBundledPolishTessdata() throws Exception {
        TesseractPart1RoiOcrReader reader = new TesseractPart1RoiOcrReader();
        Part1Roi roi = new Part1Roi(
                Part1RoiType.ITEM_POWER,
                0,
                0,
                560,
                110,
                "PNG",
                roiPng("Moc 800")
        );

        Part1RoiOcrResult result = reader.read(roi);

        assertEquals(Part1RoiType.ITEM_POWER, result.getType());
        assertFalse(result.getRawText().isBlank());
        assertTrue(result.getRawText().contains("800"), result.getRawText());
        assertTrue(result.getDurationMillis() >= 0L);
        assertTrue(result.getPreprocessing().contains("ITEM_POWER"), result.getPreprocessing());
        assertTrue(Files.exists(reader.getTessdataDirectory().resolve("pol.traineddata")));
    }

    @Test
    void shouldReadNameThroughInternalNameTextSubRoiWithoutOcringStars() throws Exception {
        TesseractPart1RoiOcrReader reader = new TesseractPart1RoiOcrReader();
        Part1Roi roi = new Part1Roi(
                Part1RoiType.NAME_AND_STARS,
                0,
                0,
                520,
                100,
                "PNG",
                roiPng("BURZOWY KROK        ***")
        );

        Part1RoiOcrResult result = reader.read(roi);

        assertEquals(Part1RoiType.NAME_AND_STARS, result.getType());
        assertFalse(result.getRawText().isBlank());
        assertTrue(result.getPreprocessing().contains("NAME_TEXT"), result.getPreprocessing());
        assertTrue(result.getSubRegions().stream().anyMatch(subRegion -> "NAME_TEXT".equals(subRegion.getType())));
        assertFalse(result.getRawText().contains("*"), result.getRawText());
    }

    @Test
    void shouldExposeCleanedNameTextDiagnosticsAndKeepFirstShieldLetter() throws Exception {
        Part1RoiOcrResult result = realNameRead("tarcza1.png");

        Map<String, Part1RoiOcrSubRegion> subRegions = result.getSubRegions().stream()
                .collect(Collectors.toMap(Part1RoiOcrSubRegion::getType, subRegion -> subRegion));
        Part1RoiOcrSubRegion source = subRegions.get("NAME_TEXT");
        Part1RoiOcrSubRegion cleaned = subRegions.get("NAME_TEXT_CLEANED");
        Part1RoiOcrSubRegion finalImage = subRegions.get("NAME_TEXT_FINAL");
        assertTrue(result.getRawText().contains("TARCZA"), result.getRawText());
        assertFalse(result.getRawText().startsWith("ARCZA"), result.getRawText());
        assertTrue(result.getPreprocessing().contains("NAME_TEXT"), result.getPreprocessing());
        assertTrue(cleaned.getPreprocessing().contains("frame/ornament cleanup"), cleaned.getPreprocessing());
        assertEquals(source.getX(), cleaned.getX());
        assertEquals(source.getY(), cleaned.getY());
        assertEquals(source.getWidth(), cleaned.getWidth());
        assertEquals(source.getHeight(), cleaned.getHeight());
        assertTrue(differentPixels(decode(source), decode(cleaned)) > 0,
                "cleaned NAME_TEXT powinien roznic sie od zrodla po usunieciu dekoracji.");
        assertTrue(decode(finalImage).getWidth() > cleaned.getWidth());
        assertTrue(decode(finalImage).getHeight() > cleaned.getHeight());
    }

    @Test
    void shouldNotRegressAlreadyReadableHelmetPairName() throws Exception {
        Part1RoiOcrResult result = realNameRead("dziedzic-zatracenia-helm-1.png", "dziedzic-zatracenia-helm-2.png");

        assertTrue(result.getRawText().contains("DZIEDZIC"), result.getRawText());
        assertTrue(result.getRawText().contains("ZATRACENIA"), result.getRawText());
    }

    @Test
    void shouldCountGreaterAffixStarsVisuallyForRealPart1Fixtures() throws Exception {
        assertGreaterAffixCount(2, "buty.png");
        assertGreaterAffixCount(1, "dziedzic-zatracenia-helm-full.png");
        assertGreaterAffixCount(1, "dziedzic-zatracenia-helm-1.png", "dziedzic-zatracenia-helm-2.png");
        assertGreaterAffixCount(1, "tarcza.png");
        assertGreaterAffixCount(2, "tarcza1.png");
        assertGreaterAffixCount(2, "tarcza1.png", "tarcza2.png");
        assertGreaterAffixCount(0, "verathiel-miecz-v2.png");
    }

    private static byte[] roiPng(String text) throws Exception {
        BufferedImage image = new BufferedImage(560, 110, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(new Color(24, 20, 18));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(232, 229, 218));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
        graphics.drawString(text, 28, 68);
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private static Part1RoiOcrResult realNameRead(String... filenames) {
        return realReads(filenames).stream()
                .filter(read -> read.getType() == Part1RoiType.NAME_AND_STARS).findFirst().orElseThrow();
    }

    private static List<Part1RoiOcrResult> realReads(String... filenames) {
        return REAL_READS.computeIfAbsent(List.of(filenames), TesseractPart1RoiOcrReaderTest::readFixture);
    }

    private static List<Part1RoiOcrResult> readFixture(List<String> filenames) {
        OpenCvHorizontalDividerDetector dividerDetector = new OpenCvHorizontalDividerDetector();
        ItemPartStructureNormalizer normalizer = new DeterministicItemPartStructureNormalizer();
        Part1RoiSegmenter segmenter = new DeterministicPart1RoiSegmenter();
        List<ItemImageImportRequest> requests = filenames.stream()
                .map(TesseractPart1RoiOcrReaderTest::request)
                .toList();
        AssembledItemImage image = ImageAssemblyFixtures.assemble(requests);
        StructuralItemPart part1 = normalizer.normalize(dividerDetector.detect(image).getParts())
                .find(StructuralItemPartSlot.PART_1)
                .orElseThrow();
        var reader = new TesseractPart1RoiOcrReader();
        return segmenter.segment(part1).getRois().stream().map(reader::read).toList();
    }

    private static void assertGreaterAffixCount(int expected, String... filenames) throws Exception {
        List<Part1RoiOcrResult> reads = realReads(filenames);
        OcrGoldenFixtures.capturePart1(List.of(filenames), reads);
        OcrGoldenFixtures.assertSameRead(OcrGoldenFixtures.part1(filenames), reads);
        Part1RoiOcrResult result = realNameRead(filenames);

        assertEquals(expected, result.getGreaterAffixCount(), String.join(" + ", filenames));
        long candidateSubRegions = result.getSubRegions().stream()
                .filter(subRegion -> subRegion.getType().startsWith("GREATER_AFFIX_STAR_"))
                .peek(subRegion -> assertTrue(subRegion.getPreprocessing().contains("confidence="),
                        subRegion.getPreprocessing()))
                .count();
        assertEquals(expected, candidateSubRegions, String.join(" + ", filenames));
    }

    private static ItemImageImportRequest request(String filename) {
        try {
            return new ItemImageImportRequest(filename, "image/png",
                    Files.readAllBytes(Path.of("src/test/resources/items", filename)));
        } catch (Exception exception) {
            throw new IllegalStateException("Nie udalo sie wczytac fixture OCR " + filename + ".", exception);
        }
    }

    private static BufferedImage decode(Part1RoiOcrSubRegion subRegion) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(subRegion.getImageBytes()));
    }

    private static int differentPixels(BufferedImage first, BufferedImage second) {
        int different = 0;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) {
                    different++;
                }
            }
        }
        return different;
    }
}
