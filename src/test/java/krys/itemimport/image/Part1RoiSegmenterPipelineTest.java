package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.opencv.OpenCvHorizontalDividerDetector;
import krys.itemimport.image.opencv.OpenCvItemImageAssembler;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regresje segmentacji structural PART_1 na tymczasowe ROI przed OCR. */
class Part1RoiSegmenterPipelineTest {
    private final OpenCvItemImageAssembler assembler = new OpenCvItemImageAssembler();
    private final OpenCvHorizontalDividerDetector dividerDetector = new OpenCvHorizontalDividerDetector();
    private final ItemPartStructureNormalizer normalizer = new DeterministicItemPartStructureNormalizer();
    private final Part1RoiSegmenter segmenter = new DeterministicPart1RoiSegmenter();

    @Test
    void shouldSegmentPart1ForRealStructurallyNormalizedFixtures() throws Exception {
        assertSegmentedPart1("buty.png", singleFixture("buty.png"), false);
        assertSegmentedPart1(
                "dziedzic-zatracenia-helm-1.png + dziedzic-zatracenia-helm-2.png",
                assembledPair("dziedzic-zatracenia-helm-1.png", "dziedzic-zatracenia-helm-2.png"),
                true
        );
        assertSegmentedPart1("dziedzic-zatracenia-helm-full.png", singleFixture("dziedzic-zatracenia-helm-full.png"), true);
        assertSegmentedPart1("tarcza.png", singleFixture("tarcza.png"), false);
        assertSegmentedPart1("tarcza1.png", singleFixture("tarcza1.png"), true);
        assertSegmentedPart1("tarcza1.png + tarcza2.png", assembledPair("tarcza1.png", "tarcza2.png"), true);
        assertSegmentedPart1("verathiel-miecz-v2.png", singleFixture("verathiel-miecz-v2.png"), false);
    }

    @Test
    void shouldNotGuessPart1ForIncompleteStructure() throws Exception {
        StructuredItemParts structured = normalize(singleFixture("verathiel-miecz.png"));

        assertEquals(ItemStructureStatus.INCOMPLETE, structured.getStatus());
        assertFalse(structured.find(StructuralItemPartSlot.PART_1).isPresent());
    }

    @Test
    void shouldKeepSemanticPart1RoiBoundsForProblematicRealFixtures() throws Exception {
        assertNoPart1ContentRegionInSegmentationResult();

        Part1SegmentationResult helmet = segment(assembledPair("dziedzic-zatracenia-helm-1.png", "dziedzic-zatracenia-helm-2.png"));
        assertRoiBounds("helmet name", helmet, Part1RoiType.NAME_AND_STARS, 8, 4, 226, 64);
        assertRoiBounds("helmet classification", helmet, Part1RoiType.CLASSIFICATION, 20, 74, 152, 42);
        assertRoiBounds("helmet item power", helmet, Part1RoiType.ITEM_POWER, 20, 120, 158, 19);
        assertRoiBounds("helmet optional", helmet, Part1RoiType.OPTIONAL_INFO, 20, 143, 201, 71);

        AssembledItemImage helmetFullImage = singleFixture("dziedzic-zatracenia-helm-full.png");
        HorizontalDividerDetectionResult helmetFullDividers = dividerDetector.detect(helmetFullImage);
        StructuredItemParts helmetFullStructured = normalizer.normalize(helmetFullDividers.getParts());
        assertEquals(2, helmetFullDividers.getDividers().size(), "helmet full dividers");
        assertEquals(3, helmetFullDividers.getParts().size(), "helmet full raw parts");
        assertEquals(List.of(StructuralItemPartSlot.PART_1, StructuralItemPartSlot.PART_3, StructuralItemPartSlot.PART_4),
                helmetFullStructured.getStructuralParts().stream().map(StructuralItemPart::getSlot).toList(),
                "helmet full structural slots");
        Part1SegmentationResult helmetFull = segmenter.segment(
                helmetFullStructured.find(StructuralItemPartSlot.PART_1).orElseThrow());
        assertRoiBounds("helmet full name", helmetFull, Part1RoiType.NAME_AND_STARS, 14, 5, 238, 67);
        assertRoiBounds("helmet full classification", helmetFull, Part1RoiType.CLASSIFICATION, 19, 77, 167, 41);
        assertRoiBounds("helmet full item power", helmetFull, Part1RoiType.ITEM_POWER, 19, 122, 173, 19);
        assertRoiBounds("helmet full optional", helmetFull, Part1RoiType.OPTIONAL_INFO, 19, 143, 216, 76);

        Part1SegmentationResult tarczaPair = segment(assembledPair("tarcza1.png", "tarcza2.png"));
        assertRoiBounds("tarcza pair name", tarczaPair, Part1RoiType.NAME_AND_STARS, 28, 0, 230, 84);
        assertRoiBounds("tarcza pair classification", tarczaPair, Part1RoiType.CLASSIFICATION, 42, 91, 166, 41);
        assertRoiBounds("tarcza pair item power", tarczaPair, Part1RoiType.ITEM_POWER, 42, 134, 158, 20);

        Part1SegmentationResult verathiel = segment(singleFixture("verathiel-miecz-v2.png"));
        assertRoiBounds("verathiel name", verathiel, Part1RoiType.NAME_AND_STARS, 15, 2, 224, 68);
        assertRoiBounds("verathiel classification", verathiel, Part1RoiType.CLASSIFICATION, 27, 74, 207, 23);
        assertRoiBounds("verathiel item power", verathiel, Part1RoiType.ITEM_POWER, 27, 98, 158, 20);

        Part1SegmentationResult buty = segment(singleFixture("buty.png"));
        assertRoiBounds("buty name", buty, Part1RoiType.NAME_AND_STARS, 10, 0, 518, 256);
        assertRoiBounds("buty classification", buty, Part1RoiType.CLASSIFICATION, 26, 269, 475, 52);
        assertRoiBounds("buty item power", buty, Part1RoiType.ITEM_POWER, 25, 325, 370, 52);
    }

    private void assertSegmentedPart1(String label, AssembledItemImage image, boolean expectedOptionalInfo) {
        StructuredItemParts structured = normalize(image);
        StructuralItemPart part1 = structured.find(StructuralItemPartSlot.PART_1)
                .orElseThrow(() -> new AssertionError(label + " nie ma structural PART_1."));

        Part1SegmentationResult result = segmenter.segment(part1);

        assertSame(part1, result.getSourcePart());
        assertRoi(label, result, Part1RoiType.NAME_AND_STARS);
        assertRoi(label, result, Part1RoiType.CLASSIFICATION);
        assertRoi(label, result, Part1RoiType.ITEM_POWER);
        assertEquals(expectedOptionalInfo, result.find(Part1RoiType.OPTIONAL_INFO).isPresent(),
                label + " ma nieoczekiwany stan OPTIONAL_INFO: " + bounds(result));
        assertVerticalOrder(label, result, expectedOptionalInfo);
    }

    private void assertRoi(String label, Part1SegmentationResult result, Part1RoiType type) {
        Part1Roi roi = result.find(type)
                .orElseThrow(() -> new AssertionError(label + " nie ma ROI " + type + ": " + bounds(result)));
        ItemImagePart source = result.getSourcePart().getSourcePart();
        assertTrue(roi.getX() >= 0, label);
        assertTrue(roi.getY() >= 0, label);
        assertTrue(roi.getWidth() > 0, label);
        assertTrue(roi.getHeight() > 0, label);
        assertTrue(roi.getX() + roi.getWidth() <= source.getWidth(), label + " ROI poza szerokoscia: " + bounds(result));
        assertTrue(roi.getY() + roi.getHeight() <= source.getHeight(), label + " ROI poza wysokoscia: " + bounds(result));
        assertNotNull(decode(roi), label + " ROI " + type + " nie jest poprawnym PNG.");
    }

    private void assertVerticalOrder(String label, Part1SegmentationResult result, boolean expectedOptionalInfo) {
        Part1Roi name = result.find(Part1RoiType.NAME_AND_STARS).orElseThrow();
        Part1Roi classification = result.find(Part1RoiType.CLASSIFICATION).orElseThrow();
        Part1Roi itemPower = result.find(Part1RoiType.ITEM_POWER).orElseThrow();
        assertTrue(name.getY() < classification.getY(), label + " NAME nie jest nad CLASSIFICATION: " + bounds(result));
        assertTrue(classification.getY() < itemPower.getY(), label + " CLASSIFICATION nie jest nad ITEM_POWER: " + bounds(result));
        assertTrue(name.getHeight() >= classification.getHeight(), label + " nazwa nie wyglada na dominujacy blok: " + bounds(result));
        if (expectedOptionalInfo) {
            Part1Roi optional = result.find(Part1RoiType.OPTIONAL_INFO).orElseThrow();
            assertTrue(itemPower.getY() < optional.getY(), label + " OPTIONAL_INFO nie jest pod ITEM_POWER: " + bounds(result));
        }
    }

    private StructuredItemParts normalize(AssembledItemImage image) {
        return normalizer.normalize(dividerDetector.detect(image).getParts());
    }

    private Part1SegmentationResult segment(AssembledItemImage image) {
        StructuredItemParts structured = normalize(image);
        StructuralItemPart part1 = structured.find(StructuralItemPartSlot.PART_1).orElseThrow();
        return segmenter.segment(part1);
    }

    private static void assertNoPart1ContentRegionInSegmentationResult() {
        for (Field field : Part1SegmentationResult.class.getDeclaredFields()) {
            assertFalse(field.getType().getSimpleName().contains("Part1ContentRegion"),
                    "Part1SegmentationResult nie powinien zawierac modelu PART_1_CONTENT.");
        }
    }

    private static void assertRoiBounds(String label,
                                        Part1SegmentationResult result,
                                        Part1RoiType type,
                                        int x,
                                        int y,
                                        int width,
                                        int height) {
        Part1Roi roi = result.find(type)
                .orElseThrow(() -> new AssertionError(label + " nie ma ROI " + type + ": " + bounds(result)));
        assertEquals(x, roi.getX(), label + " x");
        assertEquals(y, roi.getY(), label + " y");
        assertEquals(width, roi.getWidth(), label + " width");
        assertEquals(height, roi.getHeight(), label + " height");
    }

    private AssembledItemImage singleFixture(String filename) throws Exception {
        return assembler.assemble(List.of(request(filename, Files.readAllBytes(itemPath(filename)))));
    }

    private AssembledItemImage assembledPair(String first, String second) throws Exception {
        return assembler.assemble(List.of(
                request(first, Files.readAllBytes(itemPath(first))),
                request(second, Files.readAllBytes(itemPath(second)))
        ));
    }

    private static Path itemPath(String filename) {
        return Path.of("src/test/resources/items", filename);
    }

    private static ItemImageImportRequest request(String filename, byte[] bytes) {
        return new ItemImageImportRequest(filename, "image/png", bytes);
    }

    private static BufferedImage decode(Part1Roi roi) {
        try {
            return ImageIO.read(new ByteArrayInputStream(roi.getImageBytes()));
        } catch (Exception exception) {
            return null;
        }
    }

    private static String bounds(Part1SegmentationResult result) {
        return result.getRois().stream()
                .map(roi -> roi.getType() + "=" + roi.getX() + "," + roi.getY()
                        + "," + roi.getWidth() + "x" + roi.getHeight())
                .toList()
                .toString();
    }
}
