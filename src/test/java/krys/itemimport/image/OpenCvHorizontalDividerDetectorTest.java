package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.opencv.OpenCvHorizontalDividerDetector;
import krys.itemimport.image.opencv.OpenCvItemImageAssembler;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje obrazowa detekcje strukturalnych separatorow poziomych Etapu 2A. */
class OpenCvHorizontalDividerDetectorTest {
    private final OpenCvHorizontalDividerDetector detector = new OpenCvHorizontalDividerDetector();
    private final OpenCvItemImageAssembler assembler = new OpenCvItemImageAssembler();

    @Test
    void shouldReturnSinglePartWhenImageHasNoStructuralDivider() throws Exception {
        AssembledItemImage image = assembled(panel(260, 220));

        HorizontalDividerDetectionResult result = detector.detect(image);

        assertDividerContract(result, 0, 1, image.getWidth(), image.getHeight());
    }

    @Test
    void shouldDetectOneTwoAndThreeStructuralDividers() throws Exception {
        assertDividerContract(detector.detect(assembled(panel(300, 260, 120))), 1, 2, 300, 260);
        assertDividerContract(detector.detect(assembled(panel(300, 360, 110, 240))), 2, 3, 300, 360);
        assertDividerContract(detector.detect(assembled(panel(300, 480, 105, 230, 350))), 3, 4, 300, 480);
    }

    @Test
    void shouldKeepSeparateCloseDividersInTallImage() throws Exception {
        AssembledItemImage image = assembled(panel(320, 1200, 300, 352, 840));

        HorizontalDividerDetectionResult result = detector.detect(image);

        assertDividerContract(result, 3, 4, 320, 1200);
        assertDividerAround(result, 0, 300, 4);
        assertDividerAround(result, 1, 352, 4);
        assertDividerAround(result, 2, 840, 4);
    }

    @Test
    void shouldMergeClusteredNeighborRowsIntoSingleDivider() throws Exception {
        BufferedImage image = panel(320, 260);
        drawDivider(image, 128, DividerStyle.CLUSTER);

        HorizontalDividerDetectionResult result = detector.detect(assembled(image));

        assertDividerContract(result, 1, 2, 320, 260);
        HorizontalDivider divider = result.getDividers().getFirst();
        assertTrue(divider.getBandEndY() - divider.getBandStartY() >= 2);
    }

    @Test
    void shouldDetectDividerWithCentralGapAndOrnament() throws Exception {
        BufferedImage image = panel(340, 280);
        drawDivider(image, 140, DividerStyle.CENTRAL_GAP);
        drawDivider(image, 210, DividerStyle.CENTRAL_ORNAMENT);

        HorizontalDividerDetectionResult result = detector.detect(assembled(image));

        assertDividerContract(result, 2, 3, 340, 280);
    }

    @Test
    void shouldDetectOneSidedLongStructuralDividerWithoutPromotingShortFragments() throws Exception {
        BufferedImage image = panel(360, 320);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(37, 34, 29));
        graphics.drawLine(28, 154, 165, 154);
        graphics.drawLine(163, 154, 175, 148);
        graphics.drawLine(163, 154, 175, 160);
        graphics.setColor(new Color(82, 72, 48));
        graphics.drawLine(76, 96, 128, 96);
        graphics.drawLine(225, 224, 276, 224);
        for (int y = 188; y < 216; y += 10) {
            for (int x = 72; x < 252; x += 34) {
                graphics.fillRect(x, y, 18, 3);
            }
        }
        graphics.dispose();

        HorizontalDividerDetectionResult result = detector.detect(assembled(image));

        assertDividerContract(result, 1, 2, 360, 320);
        assertDividerAround(result, 0, 154, 4);
    }

    @Test
    void shouldRejectTopBottomFramesAndVerticalFrames() throws Exception {
        BufferedImage image = panel(300, 260);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(150, 130, 82));
        graphics.drawRect(18, 18, 264, 222);
        graphics.drawLine(28, 38, 28, 220);
        graphics.drawLine(272, 38, 272, 220);
        graphics.dispose();

        HorizontalDividerDetectionResult result = detector.detect(assembled(image));

        assertDividerContract(result, 0, 1, 300, 260);
    }

    @Test
    void shouldRejectShortTextLikeFragments() throws Exception {
        BufferedImage image = panel(360, 260);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(190, 178, 122));
        for (int x = 90; x < 260; x += 18) {
            graphics.drawLine(x, 128, x + 8, 128);
            graphics.drawLine(x + 2, 134, x + 11, 134);
        }
        graphics.dispose();

        HorizontalDividerDetectionResult result = detector.detect(assembled(image));

        assertDividerContract(result, 0, 1, 360, 260);
    }

    @Test
    void shouldUseInternalRoiButKeepFullWidthParts() throws Exception {
        BufferedImage image = new BufferedImage(420, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(25, 22, 20));
        graphics.fillRect(70, 40, 260, 280);
        graphics.dispose();
        drawDivider(image, 150, DividerStyle.NORMAL, 78, 322);
        drawDivider(image, 245, DividerStyle.NORMAL, 78, 322);

        HorizontalDividerDetectionResult result = detector.detect(assembled(image));

        assertDividerContract(result, 2, 3, 420, 360);
        for (ItemImagePart part : result.getParts()) {
            assertEquals(420, part.getWidth());
        }
    }

    @Test
    void shouldPreserveRowsWithoutGapsOrOverlapWhenSplitting() throws Exception {
        AssembledItemImage image = assembled(panel(320, 300, 100, 200));

        HorizontalDividerDetectionResult result = detector.detect(image);

        assertDividerContract(result, 2, 3, 320, 300);
        int expectedStart = 0;
        int totalHeight = 0;
        for (ItemImagePart part : result.getParts()) {
            assertEquals(expectedStart, part.getStartYInclusive());
            assertTrue(part.getEndYExclusive() > part.getStartYInclusive());
            expectedStart = part.getEndYExclusive();
            totalHeight += part.getHeight();
        }
        assertEquals(image.getHeight(), expectedStart);
        assertEquals(image.getHeight(), totalHeight);
    }

    @Test
    void shouldDetectButyFixtureDividers() throws Exception {
        AssembledItemImage image = fixture("buty.png");

        HorizontalDividerDetectionResult result = detector.detect(image);

        assertDividerContract(result, 2, 3, image.getWidth(), image.getHeight());
    }

    @Test
    void shouldDetectShieldFixtureDividers() throws Exception {
        AssembledItemImage image = fixture("tarcza.png");

        HorizontalDividerDetectionResult result = detector.detect(image);

        assertDividerContract(result, 2, 3, image.getWidth(), image.getHeight());
    }

    @Test
    void shouldDetectVerathielFixtureDividers() throws Exception {
        AssembledItemImage image = fixture("verathiel-miecz.png");

        HorizontalDividerDetectionResult result = detector.detect(image);

        assertDividerContract(result, 1, 2, image.getWidth(), image.getHeight());
    }

    @Test
    void shouldDetectVerathielV2FixtureDividersWithOutsideTooltipArea() throws Exception {
        AssembledItemImage image = fixture("verathiel-miecz-v2.png");

        HorizontalDividerDetectionResult result = detector.detect(image);

        assertDividerContract(result, 2, 3, image.getWidth(), image.getHeight());
    }

    @Test
    void shouldDetectDividersAfterSingleImagePanelLocalization() throws Exception {
        assertLocalizedSingleFixtureDividers("buty.png", 2, 3);
        assertLocalizedSingleFixtureDividers("dziedzic-zatracenia-helm-full.png", 2, 3);
        assertLocalizedSingleFixtureDividers("tarcza.png", 2, 3);
        assertLocalizedSingleFixtureDividers("tarcza1.png", 3, 4);
        assertLocalizedSingleFixtureDividers("verathiel-miecz-v2.png", 2, 3);
    }

    @Test
    void shouldDetectHelmetDividersAfterAcceptedMultiImageStitching() throws Exception {
        byte[] first = Files.readAllBytes(Path.of("src/test/resources/items/dziedzic-zatracenia-helm-1.png"));
        byte[] second = Files.readAllBytes(Path.of("src/test/resources/items/dziedzic-zatracenia-helm-2.png"));
        AssembledItemImage image = assembler.assemble(List.of(
                request("dziedzic-zatracenia-helm-1.png", first),
                request("dziedzic-zatracenia-helm-2.png", second)
        ));

        HorizontalDividerDetectionResult result = detector.detect(image);

        assertEquals(AssembledItemImage.STATUS_STITCHED, image.getAssemblyStatus());
        assertDividerContract(result, 2, 3, image.getWidth(), image.getHeight());
    }

    @Test
    void shouldDetectShieldDividersAfterTwoOverlappingScreensWithoutCountingSeam() throws Exception {
        BufferedImage shield = ImageIO.read(Path.of("src/test/resources/items/tarcza.png").toFile());
        BufferedImage first = crop(shield, 0, 0, shield.getWidth(), 1000);
        BufferedImage second = crop(shield, 0, 700, shield.getWidth(), shield.getHeight() - 700);
        AssembledItemImage image = assembler.assemble(List.of(
                request("tarcza-1.png", png(first)),
                request("tarcza-2.png", png(second))
        ));

        HorizontalDividerDetectionResult result = detector.detect(image);

        assertEquals(AssembledItemImage.STATUS_STITCHED, image.getAssemblyStatus());
        assertDividerContract(result, 2, 3, image.getWidth(), image.getHeight());
    }

    @Test
    void shouldDetectThreeDividersInRealTwoScreenMoonrageShieldWithoutCountingSeam() throws Exception {
        byte[] first = Files.readAllBytes(Path.of("src/test/resources/items/tarcza1.png"));
        byte[] second = Files.readAllBytes(Path.of("src/test/resources/items/tarcza2.png"));
        AssembledItemImage image = assembler.assemble(List.of(
                request("tarcza1.png", first),
                request("tarcza2.png", second)
        ));

        HorizontalDividerDetectionResult result = detector.detect(image);

        assertEquals(2, image.getSourceImageCount());
        assertEquals(AssembledItemImage.STATUS_STITCHED, image.getAssemblyStatus());
        assertEquals(356, image.getWidth());
        assertEquals(974, image.getHeight());
        assertEquals(1, image.getStitchDiagnostics().size());
        AssembledItemImage.StitchDiagnostic diagnostic = image.getStitchDiagnostics().getFirst();
        assertEquals(-26, diagnostic.getOffsetX());
        assertEquals(211, diagnostic.getOffsetY());
        assertEquals(516, diagnostic.getOverlapHeight());
        assertEquals(377, diagnostic.getSeamY());
        assertTrue(diagnostic.getConfidence() >= 0.94d);
        assertDividerContract(result, 3, 4, 356, 974);
        assertDividerAround(result, 0, 237, 6);
        assertDividerAround(result, 1, 348, 6);
        assertDividerAround(result, 2, 874, 8);
        assertThinBands(result, 8);
        assertPartBoundariesAround(result, List.of(0, 237, 348, 874, 974), 8);
        for (HorizontalDivider divider : result.getDividers()) {
            assertTrue(Math.abs(divider.getY() - diagnostic.getSeamY()) > 20,
                    "Seam nie moze byc zwrocony jako structural divider: " + dividerRows(result));
        }
    }

    private static void assertDividerContract(HorizontalDividerDetectionResult result,
                                              int expectedDividers,
                                              int expectedParts,
                                              int expectedWidth,
                                              int expectedHeight) throws Exception {
        assertEquals(expectedDividers, result.getDividers().size(), dividerRows(result));
        assertEquals(expectedParts, result.getParts().size());
        assertEquals(expectedWidth, result.getOverlayWidth());
        assertEquals(expectedHeight, result.getOverlayHeight());
        assertTrue(result.getOverlayImageBytes().length > 0);
        assertNotNull(ImageIO.read(new ByteArrayInputStream(result.getOverlayImageBytes())));

        int previousY = -1;
        for (HorizontalDivider divider : result.getDividers()) {
            assertTrue(divider.getY() > previousY);
            assertTrue(divider.getY() > 0);
            assertTrue(divider.getY() < expectedHeight);
            assertTrue(divider.getBandStartY() <= divider.getY());
            assertTrue(divider.getBandEndY() > divider.getY());
            assertTrue(divider.getStrength() >= 0.0d && divider.getStrength() <= 1.0d);
            previousY = divider.getY();
        }

        int expectedStart = 0;
        int heightSum = 0;
        for (ItemImagePart part : result.getParts()) {
            assertEquals("PART_" + part.getIndex(), part.getName());
            assertEquals(expectedWidth, part.getWidth());
            assertEquals(expectedStart, part.getStartYInclusive());
            assertEquals(part.getEndYExclusive() - part.getStartYInclusive(), part.getHeight());
            assertTrue(part.getHeight() > 0);
            assertFalse(part.getImageBytes().length == 0);
            BufferedImage decodedPart = ImageIO.read(new ByteArrayInputStream(part.getImageBytes()));
            assertNotNull(decodedPart);
            assertEquals(expectedWidth, decodedPart.getWidth());
            assertEquals(part.getHeight(), decodedPart.getHeight());
            expectedStart = part.getEndYExclusive();
            heightSum += part.getHeight();
        }
        assertEquals(expectedHeight, expectedStart);
        assertEquals(expectedHeight, heightSum);
    }

    private static String dividerRows(HorizontalDividerDetectionResult result) {
        return result.getDividers().stream()
                .map(divider -> divider.getY() + "@" + String.format(java.util.Locale.US, "%.3f", divider.getStrength()))
                .toList()
                .toString();
    }

    private static void assertDividerAround(HorizontalDividerDetectionResult result,
                                            int dividerIndex,
                                            int expectedY,
                                            int tolerance) {
        int actualY = result.getDividers().get(dividerIndex).getY();
        assertTrue(Math.abs(actualY - expectedY) <= tolerance,
                "Divider #" + (dividerIndex + 1) + " expected around " + expectedY + " but was " + actualY
                        + " rows=" + dividerRows(result));
    }

    private static void assertThinBands(HorizontalDividerDetectionResult result, int maxHeight) {
        for (HorizontalDivider divider : result.getDividers()) {
            assertTrue(divider.getBandEndY() - divider.getBandStartY() <= maxHeight,
                    "Pasmo separatora jest zbyt szerokie: " + dividerRows(result));
        }
    }

    private static void assertPartBoundariesAround(HorizontalDividerDetectionResult result,
                                                   List<Integer> boundaries,
                                                   int tolerance) {
        assertEquals(boundaries.size() - 1, result.getParts().size());
        for (int index = 0; index < result.getParts().size(); index++) {
            ItemImagePart part = result.getParts().get(index);
            if (index == 0) {
                assertEquals(0, part.getStartYInclusive());
            } else {
                assertTrue(Math.abs(part.getStartYInclusive() - boundaries.get(index)) <= tolerance,
                        "Poczatek czesci #" + part.getIndex() + " poza tolerancja.");
            }
            if (index == result.getParts().size() - 1) {
                assertEquals(boundaries.getLast(), part.getEndYExclusive());
            } else {
                assertTrue(Math.abs(part.getEndYExclusive() - boundaries.get(index + 1)) <= tolerance,
                        "Koniec czesci #" + part.getIndex() + " poza tolerancja.");
            }
        }
    }

    private static AssembledItemImage fixture(String filename) throws Exception {
        BufferedImage image = ImageIO.read(Path.of("src/test/resources/items", filename).toFile());
        return assembled(image);
    }

    private void assertLocalizedSingleFixtureDividers(String filename, int expectedDividers, int expectedParts) throws Exception {
        AssembledItemImage image = assembler.assemble(List.of(request(filename,
                Files.readAllBytes(Path.of("src/test/resources/items", filename)))));

        HorizontalDividerDetectionResult result = detector.detect(image);

        assertEquals(AssembledItemImage.STATUS_SINGLE_IMAGE, image.getAssemblyStatus(), filename);
        assertDividerContract(result, expectedDividers, expectedParts, image.getWidth(), image.getHeight());
    }

    private static AssembledItemImage assembled(BufferedImage image) throws Exception {
        return new AssembledItemImage(
                png(image),
                "PNG",
                image.getWidth(),
                image.getHeight(),
                1,
                OpenCvItemImageAssembler.SINGLE_IMAGE_POLICY,
                AssembledItemImage.STATUS_SINGLE_IMAGE,
                List.of()
        );
    }

    private static BufferedImage panel(int width, int height, int... dividers) {
        BufferedImage image = panel(width, height);
        for (int divider : dividers) {
            drawDivider(image, divider, DividerStyle.NORMAL);
        }
        return image;
    }

    private static BufferedImage panel(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(18, 16, 15));
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(new Color(31, 27, 24));
        graphics.fillRect(18, 18, width - 36, height - 36);
        graphics.dispose();
        return image;
    }

    private static void drawDivider(BufferedImage image, int y, DividerStyle style) {
        drawDivider(image, y, style, 28, image.getWidth() - 28);
    }

    private static void drawDivider(BufferedImage image, int y, DividerStyle style, int startX, int endX) {
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(184, 159, 92));
        int center = image.getWidth() / 2;
        switch (style) {
            case NORMAL -> graphics.drawLine(startX, y, endX, y);
            case CLUSTER -> {
                graphics.drawLine(startX, y - 1, endX, y - 1);
                graphics.drawLine(startX, y, endX, y);
                graphics.drawLine(startX, y + 1, endX, y + 1);
            }
            case CENTRAL_GAP -> {
                graphics.drawLine(startX, y, center - 30, y);
                graphics.drawLine(center + 30, y, endX, y);
            }
            case CENTRAL_ORNAMENT -> {
                graphics.drawLine(startX, y, center - 18, y);
                graphics.drawLine(center + 18, y, endX, y);
                graphics.drawLine(center - 10, y, center, y - 5);
                graphics.drawLine(center, y - 5, center + 10, y);
                graphics.drawLine(center - 10, y, center, y + 5);
                graphics.drawLine(center, y + 5, center + 10, y);
            }
        }
        graphics.dispose();
    }

    private static ItemImageImportRequest request(String name, byte[] bytes) {
        return new ItemImageImportRequest(name, "image/png", bytes);
    }

    private static BufferedImage crop(BufferedImage source, int x, int y, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(source, 0, 0, width, height, x, y, x + width, y + height, null);
        graphics.dispose();
        return image;
    }

    private static byte[] png(BufferedImage image) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private enum DividerStyle {
        NORMAL,
        CLUSTER,
        CENTRAL_GAP,
        CENTRAL_ORNAMENT
    }
}
