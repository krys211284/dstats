package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.opencv.OpenCvHorizontalDividerDetector;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regresje neutralnej segmentacji PART_2/PART_3 na obrazowe bloki właściwości. */
class PropertyBlockSegmenterPipelineTest {
    private final ItemImageAssembler assembler = ImageAssemblyFixtures::assemble;
    private final OpenCvHorizontalDividerDetector dividerDetector = new OpenCvHorizontalDividerDetector();
    private final ItemPartStructureNormalizer normalizer = new DeterministicItemPartStructureNormalizer();
    private final PropertyBlockSegmenter segmenter = new VisualPropertyBlockSegmenter();

    @TestFactory
    Stream<DynamicTest> shouldSegmentEachRealFixturePropertyPartWithoutDomainParsing() {
        return fixtureExpectations().stream()
                .map(expected -> dynamicTest(expected.label() + " " + expected.slot(), () -> {
                    StructuredItemParts structured = structured(assemble(expected.filenames()));
                    assertEquals(expected.anchors(), anchorTypes(segment(structured, expected.slot())),
                            expected.label() + " " + expected.slot() + " anchors");
                }));
    }

    @Test
    void shouldNotDependOnInputFilename() throws Exception {
        AssembledItemImage original = assembler.assemble(List.of(request("tarcza1.png")));
        AssembledItemImage renamed = assembler.assemble(List.of(renamedRequest("tarcza1.png", "renamed-fixture.png")));

        StructuredItemParts originalStructured = structured(original);
        StructuredItemParts renamedStructured = structured(renamed);

        assertEquals(
                anchorTypes(segment(originalStructured, StructuralItemPartSlot.PART_2)),
                anchorTypes(segment(renamedStructured, StructuralItemPartSlot.PART_2))
        );
        assertEquals(
                anchorTypes(segment(originalStructured, StructuralItemPartSlot.PART_3)),
                anchorTypes(segment(renamedStructured, StructuralItemPartSlot.PART_3))
        );
    }

    @Test
    void shouldKeepMultilineBlocksTogetherAndAvoidFooterSwallowing() throws Exception {
        int footerY = 80;
        PropertyBlockAnchor anchor = new PropertyBlockAnchor(
                PropertyBlockAnchorType.DIAMOND, 20, 19, 7, 7, 1.0d);
        PropertyBlockSegmenter controlledSegmenter = new VisualPropertyBlockSegmenter(image -> List.of(anchor));

        PropertyBlockSegmentationResult result = controlledSegmenter.segment(syntheticMultilinePart(footerY));

        assertEquals(1, result.getBlocks().size());
        PropertyBlock block = result.getBlocks().getFirst();
        assertEquals(PropertyBlockAnchorType.DIAMOND, block.getAnchorType());
        assertTrue(block.getX() <= anchor.getX(), "Blok powinien zaczynać się przy anchorze.");
        assertTrue(block.getDiagnostics().contains("lineCount=3"),
                "Wszystkie continuation lines powinny należeć do jednego bloku.");
        assertTrue(block.getY() + block.getHeight() < footerY,
                "Trailing footer nie powinien należeć do bloku właściwości.");
    }

    @Test
    void shouldKeepGeometricUnknownIconAsPropertyBlockAnchor() throws Exception {
        PropertyBlockAnchor anchor = new PropertyBlockAnchor(
                PropertyBlockAnchorType.UNKNOWN_ICON, 20, 19, 7, 7, 0.45d);
        PropertyBlockSegmenter controlledSegmenter = new VisualPropertyBlockSegmenter(image -> List.of(anchor));

        PropertyBlockSegmentationResult result = controlledSegmenter.segment(syntheticMultilinePart(80));

        assertEquals(1, result.getBlocks().size());
        PropertyBlock block = result.getBlocks().getFirst();
        assertEquals(PropertyBlockAnchorType.UNKNOWN_ICON, block.getAnchorType());
        assertTrue(block.getAnchor().isPresent(),
                "Geometrycznie poprawny UNKNOWN_ICON powinien zostać anchorem bloku.");
        assertTrue(block.getDiagnostics().contains("lineCount=3"));
    }

    @Test
    void shouldRejectTrailingFlavorFragmentsAndKeepRealUnknownIcon() throws Exception {
        PropertyBlockAnchor realIcon = new PropertyBlockAnchor(
                PropertyBlockAnchorType.UNKNOWN_ICON, 20, 19, 7, 7, 0.45d);
        PropertyBlockAnchor firstFlavorFragment = new PropertyBlockAnchor(
                PropertyBlockAnchorType.UNKNOWN_ICON, 20, 70, 10, 7, 0.45d);
        PropertyBlockAnchor secondFlavorFragment = new PropertyBlockAnchor(
                PropertyBlockAnchorType.TRANSFIGURATION_MARK, 20, 96, 14, 7, 0.84d);
        PropertyBlockSegmenter controlledSegmenter = new VisualPropertyBlockSegmenter(
                image -> List.of(realIcon, firstFlavorFragment, secondFlavorFragment));

        PropertyBlockSegmentationResult result = controlledSegmenter.segment(syntheticFlavorPart());

        assertEquals(1, result.getBlocks().size(),
                "Końcowe fragmenty flavor text nie powinny tworzyć PropertyBlock.");
        PropertyBlock block = result.getBlocks().getFirst();
        assertEquals(PropertyBlockAnchorType.UNKNOWN_ICON, block.getAnchorType());
        assertEquals(realIcon.getY(), block.getAnchor().orElseThrow().getY(),
                "Prawdziwy UNKNOWN_ICON powinien pozostać anchorem bloku.");
    }

    @Test
    void shouldAcceptOversizedSocketAnchorsAndRejectLargeFrame() {
        BufferedImage image = new BufferedImage(200, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.drawRect(22, 20, 18, 10);
        graphics.setColor(new Color(190, 35, 45));
        graphics.drawRect(22, 50, 18, 10);
        graphics.setColor(Color.WHITE);
        graphics.drawRect(6, 5, 10, 105);
        graphics.dispose();

        List<PropertyBlockAnchor> anchors = new VisualPropertyBlockAnchorDetector().detect(image);

        assertTrue(anchors.stream().anyMatch(anchor -> anchor.getY() >= 19 && anchor.getY() <= 21),
                "Poprawny EMPTY_SOCKET nie powinien odpadać jako TOO_LARGE.");
        assertTrue(anchors.stream().anyMatch(anchor -> anchor.getY() >= 49 && anchor.getY() <= 51),
                "Poprawny FILLED_SOCKET nie powinien odpadać jako TOO_LARGE.");
        assertFalse(anchors.stream().anyMatch(anchor -> anchor.getHeight() > 80),
                "Duży komponent ramki nadal powinien być odrzucany.");
    }

    @Test
    void shouldKeepUnknownIconAsClassifierFallback() {
        BufferedImage image = new BufferedImage(200, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.drawRect(22, 20, 12, 7);
        graphics.dispose();

        List<PropertyBlockAnchor> anchors = new VisualPropertyBlockAnchorDetector().detect(image);

        assertTrue(anchors.stream().anyMatch(anchor -> anchor.getType() == PropertyBlockAnchorType.UNKNOWN_ICON),
                "Nierozpoznany poprawny kontur powinien zachować fallback UNKNOWN_ICON.");
    }

    @Test
    void shouldKeepFullLegendaryPowerAndSocketTextInTarczaPairPart3() throws Exception {
        StructuredItemParts structured = structured(pair("tarcza1.png", "tarcza2.png"));

        PropertyBlockSegmentationResult result = segment(structured, StructuralItemPartSlot.PART_3);

        assertEquals(List.of(
                PropertyBlockAnchorType.DIAMOND,
                PropertyBlockAnchorType.DIAMOND,
                PropertyBlockAnchorType.DIAMOND,
                PropertyBlockAnchorType.GREATER_STAR,
                PropertyBlockAnchorType.TRANSFIGURATION_MARK,
                PropertyBlockAnchorType.GREATER_STAR,
                PropertyBlockAnchorType.LEGENDARY_POWER_STAR,
                PropertyBlockAnchorType.EMPTY_SOCKET
        ), anchorTypes(result));
        assertEquals(8, result.getBlocks().size());

        PropertyBlock legendaryPower = result.getBlocks().get(6);
        assertBlockBounds(legendaryPower, 17, 199, 308, 229);
        assertTrue(legendaryPower.getDiagnostics().contains("lineCount=11"),
                "Legendary power powinien zawierać wszystkie linie aż do następnego anchora.");

        PropertyBlock emptySocket = result.getBlocks().get(7);
        assertBlockBounds(emptySocket, 38, 433, 131, 28);
        assertTrue(legendaryPower.getY() + legendaryPower.getHeight() < emptySocket.getY(),
                "Legendary power powinien kończyć się przed wpisem EMPTY_SOCKET.");
    }

    private static void assertBlockBounds(PropertyBlock block, int x, int y, int width, int height) {
        assertEquals(x, block.getX(), "block x");
        assertEquals(y, block.getY(), "block y");
        assertEquals(width, block.getWidth(), "block width");
        assertEquals(height, block.getHeight(), "block height");
    }

    private PropertyBlockSegmentationResult segment(StructuredItemParts structured, StructuralItemPartSlot slot) {
        return segmenter.segment(structured.find(slot).orElseThrow());
    }

    private StructuredItemParts structured(AssembledItemImage image) {
        return normalizer.normalize(dividerDetector.detect(image).getParts());
    }

    private static List<PropertyBlockAnchorType> anchorTypes(PropertyBlockSegmentationResult result) {
        return result.getBlocks().stream()
                .map(PropertyBlock::getAnchorType)
                .toList();
    }

    private AssembledItemImage single(String filename) throws Exception {
        return assembler.assemble(List.of(request(filename)));
    }

    private AssembledItemImage pair(String first, String second) throws Exception {
        return assembler.assemble(List.of(request(first), request(second)));
    }

    private AssembledItemImage assemble(List<String> filenames) throws Exception {
        return assembler.assemble(filenames.stream().map(filename -> {
            try {
                return request(filename);
            } catch (Exception exception) {
                throw new IllegalStateException("Nie udało się odczytać fixture'a " + filename + ".", exception);
            }
        }).toList());
    }

    private static StructuralItemPart syntheticMultilinePart(int footerY) throws Exception {
        int width = 200;
        int height = 120;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(20, 19, 7, 7);
        graphics.fillRect(40, 20, 120, 6);
        graphics.fillRect(40, 32, 105, 6);
        graphics.fillRect(40, 44, 90, 6);
        graphics.fillRect(70, footerY, 65, 6);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", output);
        ItemImagePart source = new ItemImagePart(
                3, "synthetic-multiline", 0, height, width, height, "PNG", output.toByteArray());
        return new StructuralItemPart(StructuralItemPartSlot.PART_3, source);
    }

    private static StructuralItemPart syntheticFlavorPart() throws Exception {
        int width = 200;
        int height = 140;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(20, 19, 7, 7);
        graphics.fillRect(40, 20, 120, 6);
        graphics.fillRect(40, 32, 105, 6);

        graphics.fillRect(20, 70, 10, 7);
        graphics.fillRect(32, 70, 148, 6);
        graphics.fillRect(22, 82, 154, 6);
        graphics.fillRect(20, 96, 14, 7);
        graphics.fillRect(36, 96, 144, 6);
        graphics.fillRect(22, 108, 150, 6);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", output);
        ItemImagePart source = new ItemImagePart(
                3, "synthetic-flavor", 0, height, width, height, "PNG", output.toByteArray());
        return new StructuralItemPart(StructuralItemPartSlot.PART_3, source);
    }

    private static ItemImageImportRequest request(String filename) throws Exception {
        return renamedRequest(filename, filename);
    }

    private static ItemImageImportRequest renamedRequest(String sourceFilename, String requestFilename) throws Exception {
        return new ItemImageImportRequest(requestFilename, "image/png",
                Files.readAllBytes(Path.of("src/test/resources/items", sourceFilename)));
    }

    private static List<FixtureExpectation> fixtureExpectations() {
        return List.of(
                expected("buty.png", List.of("buty.png"), StructuralItemPartSlot.PART_3, List.of(
                        PropertyBlockAnchorType.NONE,
                        PropertyBlockAnchorType.DIAMOND
                )),
                expected("dziedzic-zatracenia-helm-full.png",
                        List.of("dziedzic-zatracenia-helm-full.png"), StructuralItemPartSlot.PART_3, helmetOracle()),
                expected("dziedzic helmet pair",
                        List.of("dziedzic-zatracenia-helm-1.png", "dziedzic-zatracenia-helm-2.png"),
                        StructuralItemPartSlot.PART_3, helmetOracle()),
                expected("tarcza.png", List.of("tarcza.png"), StructuralItemPartSlot.PART_3, List.of(
                        PropertyBlockAnchorType.NONE,
                        PropertyBlockAnchorType.DIAMOND,
                        PropertyBlockAnchorType.DIAMOND,
                        PropertyBlockAnchorType.DIAMOND
                )),
                expected("tarcza pair", List.of("tarcza1.png", "tarcza2.png"), StructuralItemPartSlot.PART_2,
                        List.of(
                                PropertyBlockAnchorType.NONE,
                                PropertyBlockAnchorType.DIAMOND,
                                PropertyBlockAnchorType.DIAMOND
                        )),
                expected("tarcza pair", List.of("tarcza1.png", "tarcza2.png"), StructuralItemPartSlot.PART_3,
                        List.of(
                                PropertyBlockAnchorType.DIAMOND,
                                PropertyBlockAnchorType.DIAMOND,
                                PropertyBlockAnchorType.DIAMOND,
                                PropertyBlockAnchorType.GREATER_STAR,
                                PropertyBlockAnchorType.TRANSFIGURATION_MARK,
                                PropertyBlockAnchorType.GREATER_STAR,
                                PropertyBlockAnchorType.LEGENDARY_POWER_STAR,
                                PropertyBlockAnchorType.EMPTY_SOCKET
                        )),
                expected("verathiel-miecz-v2.png", List.of("verathiel-miecz-v2.png"),
                        StructuralItemPartSlot.PART_3, List.of(
                                PropertyBlockAnchorType.NONE,
                                PropertyBlockAnchorType.DIAMOND,
                                PropertyBlockAnchorType.DIAMOND,
                                PropertyBlockAnchorType.DIAMOND,
                                PropertyBlockAnchorType.DIAMOND,
                                PropertyBlockAnchorType.UNIQUE_POWER_STAR
                        ))
        );
    }

    private static List<PropertyBlockAnchorType> helmetOracle() {
        return List.of(
                PropertyBlockAnchorType.NONE,
                PropertyBlockAnchorType.DIAMOND,
                PropertyBlockAnchorType.DIAMOND,
                PropertyBlockAnchorType.DIAMOND,
                PropertyBlockAnchorType.DIAMOND,
                PropertyBlockAnchorType.TRANSFIGURATION_MARK,
                PropertyBlockAnchorType.GREATER_STAR,
                PropertyBlockAnchorType.UNIQUE_POWER_STAR,
                PropertyBlockAnchorType.FILLED_SOCKET,
                PropertyBlockAnchorType.FILLED_SOCKET
        );
    }

    private static FixtureExpectation expected(String label,
                                               List<String> filenames,
                                               StructuralItemPartSlot slot,
                                               List<PropertyBlockAnchorType> anchors) {
        return new FixtureExpectation(label, filenames, slot, anchors);
    }

    private record FixtureExpectation(String label,
                                      List<String> filenames,
                                      StructuralItemPartSlot slot,
                                      List<PropertyBlockAnchorType> anchors) {
    }
}
