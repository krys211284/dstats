package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.opencv.OpenCvItemImageAssembler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje assembler obrazow itemow oparty o OpenCV i overlap-aware stitching. */
class OpenCvItemImageAssemblerTest {
    private final OpenCvItemImageAssembler assembler = new OpenCvItemImageAssembler();

    @Test
    void shouldKeepSingleImagePanelDimensionsWithoutScalingWhenNoOuterChromeExists() throws Exception {
        BufferedImage source = patternedPanel(17, 9, 11);

        AssembledItemImage image = assembler.assemble(List.of(request("single.png", png(source))));

        assertEquals(17, image.getWidth());
        assertEquals(9, image.getHeight());
        assertEquals(1, image.getSourceImageCount());
        assertEquals(OpenCvItemImageAssembler.SINGLE_IMAGE_POLICY, image.getCompositionPolicy());
        assertEquals(AssembledItemImage.STATUS_SINGLE_IMAGE, image.getAssemblyStatus());
        assertTrue(image.getStitchDiagnostics().isEmpty());
        assertImagesEqual(source, decode(image));
    }

    @Test
    void shouldCropOuterChromeAboveSingleImagePanelWithoutUsingTextContent() throws Exception {
        BufferedImage panel = patternedPanel(82, 96, 13);
        BufferedImage source = screenshotWithChromeAbovePanel(panel, 34, 12);

        AssembledItemImage image = assembler.assemble(List.of(request("single-with-chrome.png", png(source))));

        BufferedImage decoded = decode(image);
        assertEquals(1, image.getSourceImageCount());
        assertEquals(OpenCvItemImageAssembler.SINGLE_IMAGE_POLICY, image.getCompositionPolicy());
        assertEquals(AssembledItemImage.STATUS_SINGLE_IMAGE, image.getAssemblyStatus());
        assertTrue(image.getHeight() < source.getHeight(), "single-image powinien odciac zewnetrzny chrome nad panelem.");
        assertTrue(source.getHeight() - image.getHeight() >= 20, "odciecie powinno usunac tekstowy pasek nad panelem.");
        assertTrue(image.getWidth() <= source.getWidth());
        assertContentPixelsEqual(panel, decoded, findPanelLeftOffset(decoded, panel));
    }

    @Test
    void shouldCropOuterChromeFromRealFullHelmetSingleScreenshot() throws Exception {
        byte[] fixture = Files.readAllBytes(itemPath("dziedzic-zatracenia-helm-full.png"));
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(fixture));

        AssembledItemImage image = assembler.assemble(List.of(request("dziedzic-zatracenia-helm-full.png", fixture)));

        BufferedImage decoded = decode(image);
        PointOffset offset = findImageOffset(source, decoded);
        assertEquals(1, image.getSourceImageCount());
        assertEquals(OpenCvItemImageAssembler.SINGLE_IMAGE_POLICY, image.getCompositionPolicy());
        assertEquals(AssembledItemImage.STATUS_SINGLE_IMAGE, image.getAssemblyStatus());
        assertTrue(image.getHeight() < source.getHeight(), "realny single screenshot powinien zostac uciety od gory.");
        assertTrue(offset.y() > 0, "crop powinien zaczynac sie ponizej zewnetrznego chrome UI.");
        assertEquals(source.getHeight(), offset.y() + image.getHeight(),
                "assembler powinien zachowac dol itemu po odcieciu gornego chrome.");
        assertTrue(image.getWidth() <= source.getWidth());
        assertTooltipTopLooksLikeItemPanel(decoded);
    }

    @Test
    void shouldStitchTwoImagesWithKnownOverlapWithoutDuplicatingRows() throws Exception {
        BufferedImage panel = patternedPanel(64, 120, 21);
        BufferedImage first = screenshotFromPanel(panel, 0, 80, 64, 0);
        BufferedImage second = screenshotFromPanel(panel, 50, 70, 64, 0);

        AssembledItemImage image = assembler.assemble(List.of(
                request("top.png", png(first)),
                request("bottom.png", png(second))
        ));

        assertEquals(64, image.getWidth());
        assertEquals(120, image.getHeight());
        assertEquals(OpenCvItemImageAssembler.STITCHED_COMPOSITION_POLICY, image.getCompositionPolicy());
        assertEquals(AssembledItemImage.STATUS_STITCHED, image.getAssemblyStatus());
        assertEquals(1, image.getStitchDiagnostics().size());
        AssembledItemImage.StitchDiagnostic diagnostic = image.getStitchDiagnostics().getFirst();
        assertEquals(30, diagnostic.getOverlapHeight());
        assertEquals(50, diagnostic.getOffsetY());
        assertTrue(diagnostic.getConfidence() >= 0.99d);
        assertImagesEqual(panel, decode(image));
    }

    @Test
    void shouldLocalizeAndStitchXShiftedPanelWithoutScaling() throws Exception {
        BufferedImage panel = patternedPanel(60, 115, 31);
        BufferedImage first = screenshotFromPanel(panel, 0, 70, 110, 12);
        BufferedImage second = screenshotFromPanel(panel, 45, 70, 110, 28);

        AssembledItemImage image = assembler.assemble(List.of(
                request("top.png", png(first)),
                request("bottom.png", png(second))
        ));

        BufferedImage stitched = decode(image);
        assertTrue(stitched.getWidth() < 110);
        assertEquals(115, stitched.getHeight());
        assertContentPixelsEqual(panel, stitched, findPanelLeftOffset(stitched, panel));
    }

    @Test
    void shouldIgnoreDifferentFullScreenshotWidthsWhenPanelIsTheSameScale() throws Exception {
        BufferedImage panel = patternedPanel(60, 118, 41);
        BufferedImage first = screenshotFromPanel(panel, 0, 74, 124, 22);
        BufferedImage second = screenshotFromPanel(panel, 46, 72, 96, 12);

        AssembledItemImage image = assembler.assemble(List.of(
                request("wide-screen.png", png(first)),
                request("narrow-screen.png", png(second))
        ));

        BufferedImage stitched = decode(image);
        assertTrue(stitched.getWidth() < 96);
        assertEquals(118, stitched.getHeight());
        assertContentPixelsEqual(panel, stitched, findPanelLeftOffset(stitched, panel));
    }

    @Test
    void shouldHandleLargeOverlap() throws Exception {
        BufferedImage panel = patternedPanel(58, 120, 51);
        BufferedImage first = screenshotFromPanel(panel, 0, 90, 58, 0);
        BufferedImage second = screenshotFromPanel(panel, 30, 90, 58, 0);

        AssembledItemImage image = assembler.assemble(List.of(
                request("top.png", png(first)),
                request("bottom.png", png(second))
        ));

        assertEquals(120, image.getHeight());
        assertEquals(60, image.getStitchDiagnostics().getFirst().getOverlapHeight());
        assertImagesEqual(panel, decode(image));
    }

    @Test
    void shouldHandleSmallSufficientOverlap() throws Exception {
        BufferedImage panel = patternedPanel(58, 112, 61);
        BufferedImage first = screenshotFromPanel(panel, 0, 60, 58, 0);
        BufferedImage second = screenshotFromPanel(panel, 52, 60, 58, 0);

        AssembledItemImage image = assembler.assemble(List.of(
                request("top.png", png(first)),
                request("bottom.png", png(second))
        ));

        assertEquals(112, image.getHeight());
        assertEquals(8, image.getStitchDiagnostics().getFirst().getOverlapHeight());
        assertImagesEqual(panel, decode(image));
    }

    @Test
    void shouldRejectNoOverlapInsteadOfPretendingVerticalConcatIsFinalResult() throws Exception {
        BufferedImage firstPanel = patternedPanel(58, 70, 71);
        BufferedImage secondPanel = patternedPanel(58, 70, 177);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> assembler.assemble(List.of(
                request("first.png", png(firstPanel)),
                request("second.png", png(secondPanel))
        )));

        assertTrue(exception.getMessage().contains("STITCH_NOT_CONFIDENT"));
        assertTrue(exception.getMessage().contains("Pionowa kompozycja pozostaje tylko artefaktem diagnostycznym."));
    }

    @Test
    void shouldStitchWhenOverlapContainsSmallDisturbingElement() throws Exception {
        BufferedImage panel = patternedPanel(64, 120, 81);
        BufferedImage first = screenshotFromPanel(panel, 0, 80, 64, 0);
        BufferedImage second = screenshotFromPanel(panel, 50, 70, 64, 0);
        Graphics2D graphics = second.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(18, 10, 20, 7);
        graphics.dispose();

        AssembledItemImage image = assembler.assemble(List.of(
                request("top.png", png(first)),
                request("bottom.png", png(second))
        ));

        assertEquals(120, image.getHeight());
        assertEquals(30, image.getStitchDiagnostics().getFirst().getOverlapHeight());
        assertTrue(image.getStitchDiagnostics().getFirst().getConfidence() >= 0.82d);
    }

    @Test
    void shouldRespectUploadOrderAcrossThreeScreens() throws Exception {
        BufferedImage panel = patternedPanel(50, 150, 91);
        BufferedImage first = screenshotFromPanel(panel, 0, 70, 50, 0);
        BufferedImage second = screenshotFromPanel(panel, 50, 70, 50, 0);
        BufferedImage third = screenshotFromPanel(panel, 100, 50, 50, 0);

        AssembledItemImage image = assembler.assemble(List.of(
                request("top.png", png(first)),
                request("middle.png", png(second)),
                request("bottom.png", png(third))
        ));

        assertEquals(150, image.getHeight());
        assertEquals(2, image.getStitchDiagnostics().size());
        assertImagesEqual(panel, decode(image));
    }

    @Test
    void shouldWriteMultiImageDebugArtifactsWithoutReadingThemBack() throws Exception {
        BufferedImage panel = patternedPanel(48, 90, 101);
        BufferedImage first = screenshotFromPanel(panel, 0, 60, 48, 0);
        BufferedImage second = screenshotFromPanel(panel, 40, 50, 48, 0);

        assembler.assemble(List.of(
                request("top.png", png(first)),
                request("bottom.png", png(second))
        ));

        assertTrue(Files.exists(Path.of("target", "item-import-debug", "raw-composition.png")));
        assertTrue(Files.exists(Path.of("target", "item-import-debug", "stitched-item.png")));
        assertTrue(Files.exists(Path.of("target", "item-import-debug", "source-01-panel.png")));
        assertTrue(Files.exists(Path.of("target", "item-import-debug", "source-02-panel.png")));
        assertTrue(Files.exists(Path.of("target", "item-import-debug", "overlap-01-02.png")));
    }

    @Test
    void shouldKeepStitchedResultWhenDebugArtifactsCannotBeWritten(@TempDir Path tempDirectory) throws Exception {
        BufferedImage panel = patternedPanel(48, 90, 107);
        BufferedImage first = screenshotFromPanel(panel, 0, 60, 48, 0);
        BufferedImage second = screenshotFromPanel(panel, 40, 50, 48, 0);
        Path debugDirectoryAsFile = tempDirectory.resolve("debug-output-is-file");
        Files.writeString(debugDirectoryAsFile, "blokada katalogu debug");
        String previousDebugDirectory = System.getProperty(AssembledItemImageDebugWriter.DEBUG_DIRECTORY_PROPERTY);
        Logger logger = Logger.getLogger(AssembledItemImageDebugWriter.class.getName());
        CapturingLogHandler handler = new CapturingLogHandler();
        logger.addHandler(handler);
        System.setProperty(AssembledItemImageDebugWriter.DEBUG_DIRECTORY_PROPERTY, debugDirectoryAsFile.toString());
        try {
            AssembledItemImage image = assembler.assemble(List.of(
                    request("top.png", png(first)),
                    request("bottom.png", png(second))
            ));

            assertEquals(AssembledItemImage.STATUS_STITCHED, image.getAssemblyStatus());
            assertEquals(90, image.getHeight());
            assertEquals(1, image.getStitchDiagnostics().size());
            assertTrue(handler.contains("OPENCV_DEBUG_ARTIFACT_WARNING"));
        } finally {
            logger.removeHandler(handler);
            if (previousDebugDirectory == null) {
                System.clearProperty(AssembledItemImageDebugWriter.DEBUG_DIRECTORY_PROPERTY);
            } else {
                System.setProperty(AssembledItemImageDebugWriter.DEBUG_DIRECTORY_PROPERTY, previousDebugDirectory);
            }
        }
    }

    @Test
    void shouldRejectEmptyInputList() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> assembler.assemble(List.of()));

        assertEquals("Wgraj co najmniej jeden obraz itemu do zlozenia.", exception.getMessage());
    }

    @Test
    void shouldRejectCorruptedImageBytes() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> assembler.assemble(List.of(request("broken.bin", new byte[]{1, 2, 3, 4}))));

        assertTrue(exception.getMessage().contains("Nie udalo sie zdekodowac obrazu itemu #1 przez OpenCV."));
    }

    @Test
    void shouldWriteDebugArtifactWithoutChangingAssembledImage(@TempDir Path tempDirectory) throws Exception {
        AssembledItemImage image = assembler.assemble(List.of(request("single.png", png(patternedPanel(4, 4, 111)))));
        byte[] before = image.getImageBytes();
        Path outputPath = tempDirectory.resolve("stitched-item.png");

        Path writtenPath = new AssembledItemImageDebugWriter().write(image, outputPath);

        assertEquals(outputPath, writtenPath);
        assertTrue(Files.exists(writtenPath));
        assertArrayEquals(before, image.getImageBytes());
        assertEquals(4, decode(image).getWidth());
    }

    @Test
    void shouldAssembleRealItemScreenshotFixture() throws Exception {
        byte[] fixture = Files.readAllBytes(Path.of("src/test/resources/items/verathiel-miecz-v2.png"));
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(fixture));

        AssembledItemImage image = assembler.assemble(List.of(request("verathiel-miecz-v2.png", fixture)));

        BufferedImage decoded = decode(image);
        assertEquals(decoded.getWidth(), image.getWidth());
        assertEquals(decoded.getHeight(), image.getHeight());
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);
        assertTrue(image.getWidth() < source.getWidth(), "single-image fixture powinien zostac zawężony do panelu itemu.");
        assertEquals(source.getHeight(), image.getHeight(), "fixture nie ma chrome nad panelem, wiec gora nie powinna zostac ucieta.");
        assertEquals(AssembledItemImage.STATUS_SINGLE_IMAGE, image.getAssemblyStatus());
    }

    @Test
    void shouldAssembleRealSingleImageFixturesAfterPanelLocalization() throws Exception {
        assertRealSingleFixtureAssembles("buty.png");
        assertRealSingleFixtureAssembles("tarcza.png");
        assertRealSingleFixtureAssembles("tarcza1.png");
        assertRealSingleFixtureAssembles("verathiel-miecz-v2.png");
    }

    @Test
    void shouldStitchRealTwoScreenHelmetFixtureWhenOverlapIsPresent() throws Exception {
        byte[] first = Files.readAllBytes(Path.of("src/test/resources/items/dziedzic-zatracenia-helm-1.png"));
        byte[] second = Files.readAllBytes(Path.of("src/test/resources/items/dziedzic-zatracenia-helm-2.png"));

        AssembledItemImage image = assembler.assemble(List.of(
                request("dziedzic-zatracenia-helm-1.png", first),
                request("dziedzic-zatracenia-helm-2.png", second)
        ));

        assertEquals(2, image.getSourceImageCount());
        assertEquals(AssembledItemImage.STATUS_STITCHED, image.getAssemblyStatus());
        assertEquals(1, image.getStitchDiagnostics().size());
        assertTrue(image.getStitchDiagnostics().getFirst().getConfidence() >= 0.82d);
        assertTrue(image.getHeight() > 0);
        assertTrue(image.getWidth() > 0);
    }

    @Test
    void shouldStitchRealTwoScreenShieldFixtureWhenOverlapIsPresent() throws Exception {
        BufferedImage shield = ImageIO.read(Path.of("src/test/resources/items/tarcza.png").toFile());
        BufferedImage first = crop(shield, 0, 0, shield.getWidth(), 1000);
        BufferedImage second = crop(shield, 0, 700, shield.getWidth(), shield.getHeight() - 700);
        int verticalConcatHeight = first.getHeight() + second.getHeight();

        AssembledItemImage image = assembler.assemble(List.of(
                request("tarcza-1.png", png(first)),
                request("tarcza-2.png", png(second))
        ));

        assertEquals(2, image.getSourceImageCount());
        assertEquals(AssembledItemImage.STATUS_STITCHED, image.getAssemblyStatus());
        assertEquals(1, image.getStitchDiagnostics().size());
        AssembledItemImage.StitchDiagnostic diagnostic = image.getStitchDiagnostics().getFirst();
        assertTrue(diagnostic.getConfidence() >= 0.82d);
        assertTrue(diagnostic.getOverlapHeight() > 0);
        assertTrue(diagnostic.getOffsetY() > 0);
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);
        assertTrue(image.getHeight() < verticalConcatHeight);
        assertTrue(verticalConcatHeight - image.getHeight() >= Math.max(1, diagnostic.getOverlapHeight() / 2));
    }

    private static ItemImageImportRequest request(String name, byte[] bytes) {
        return new ItemImageImportRequest(name, "image/png", bytes);
    }

    private static BufferedImage patternedPanel(int width, int height, int seed) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, patternedColor(x, y, seed).getRGB());
            }
        }
        return image;
    }

    private static Color patternedColor(int x, int y, int seed) {
        int hash = seed * 1_000_003 + y * 91_913 + x * 31_337 + x * y * 701;
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        int red = 45 + Math.floorMod(hash, 180);
        int green = 45 + Math.floorMod(hash >>> 8, 180);
        int blue = 45 + Math.floorMod(hash >>> 16, 180);
        return new Color(red, green, blue);
    }

    private static BufferedImage screenshotFromPanel(BufferedImage panel,
                                                     int viewportY,
                                                     int viewportHeight,
                                                     int screenshotWidth,
                                                     int panelX) {
        BufferedImage image = new BufferedImage(screenshotWidth, viewportHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, screenshotWidth, viewportHeight);
        graphics.drawImage(panel, panelX, -viewportY, null);
        graphics.dispose();
        return image;
    }

    private static BufferedImage screenshotWithChromeAbovePanel(BufferedImage panel, int chromeHeight, int panelX) {
        BufferedImage image = new BufferedImage(panel.getWidth() + panelX * 2, panel.getHeight() + chromeHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(230, 230, 230));
        graphics.fillRect(24, 9, 34, 9);
        graphics.drawLine(64, 13, 110, 13);
        graphics.drawImage(panel, panelX, chromeHeight, null);
        graphics.dispose();
        return image;
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

    private static BufferedImage decode(AssembledItemImage image) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(image.getImageBytes()));
    }

    private void assertRealSingleFixtureAssembles(String filename) throws Exception {
        byte[] fixture = Files.readAllBytes(itemPath(filename));

        AssembledItemImage image = assembler.assemble(List.of(request(filename, fixture)));

        assertEquals(1, image.getSourceImageCount(), filename);
        assertEquals(AssembledItemImage.STATUS_SINGLE_IMAGE, image.getAssemblyStatus(), filename);
        assertTrue(image.getWidth() > 0, filename);
        assertTrue(image.getHeight() > 0, filename);
        assertTrue(decode(image).getWidth() == image.getWidth(), filename);
    }

    private static Path itemPath(String filename) {
        return Path.of("src/test/resources/items", filename);
    }

    private static void assertContentPixelsEqual(BufferedImage expectedPanel, BufferedImage actual, int leftMargin) {
        assertEquals(expectedPanel.getHeight(), actual.getHeight());
        for (int y = 0; y < expectedPanel.getHeight(); y++) {
            for (int x = 0; x < expectedPanel.getWidth(); x++) {
                assertColor(actual, x + leftMargin, y, new Color(expectedPanel.getRGB(x, y)));
            }
        }
    }

    private static int findPanelLeftOffset(BufferedImage stitched, BufferedImage panel) {
        for (int x = 0; x <= stitched.getWidth() - panel.getWidth(); x++) {
            if (matchesPanelSamples(stitched, panel, x)) {
                return x;
            }
        }
        throw new AssertionError("Nie znaleziono poczatku panelu w stitched image.");
    }

    private static boolean matchesPanelSamples(BufferedImage stitched, BufferedImage panel, int leftMargin) {
        int stepX = Math.max(1, panel.getWidth() / 16);
        int stepY = Math.max(1, panel.getHeight() / 24);
        for (int y = 0; y < panel.getHeight(); y += stepY) {
            for (int x = 0; x < panel.getWidth(); x += stepX) {
                if (stitched.getRGB(leftMargin + x, y) != panel.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return stitched.getRGB(leftMargin + panel.getWidth() - 1, panel.getHeight() - 1)
                == panel.getRGB(panel.getWidth() - 1, panel.getHeight() - 1);
    }

    private static PointOffset findImageOffset(BufferedImage source, BufferedImage assembled) {
        for (int y = 0; y <= source.getHeight() - assembled.getHeight(); y++) {
            for (int x = 0; x <= source.getWidth() - assembled.getWidth(); x++) {
                if (matchesImageSamples(source, assembled, x, y)) {
                    return new PointOffset(x, y);
                }
            }
        }
        throw new AssertionError("Nie znaleziono zlozonego obrazu wewnatrz zrodlowego screenshotu.");
    }

    private static boolean matchesImageSamples(BufferedImage source, BufferedImage assembled, int offsetX, int offsetY) {
        int stepX = Math.max(1, assembled.getWidth() / 16);
        int stepY = Math.max(1, assembled.getHeight() / 24);
        for (int y = 0; y < assembled.getHeight(); y += stepY) {
            for (int x = 0; x < assembled.getWidth(); x += stepX) {
                if (source.getRGB(offsetX + x, offsetY + y) != assembled.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return source.getRGB(offsetX + assembled.getWidth() - 1, offsetY + assembled.getHeight() - 1)
                == assembled.getRGB(assembled.getWidth() - 1, assembled.getHeight() - 1);
    }

    private static void assertTooltipTopLooksLikeItemPanel(BufferedImage image) {
        int inspectedHeight = Math.min(10, image.getHeight());
        int brightPixels = 0;
        int signalPixels = 0;
        for (int y = 0; y < inspectedHeight; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y));
                int luma = (color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114) / 1000;
                if (luma >= 145) {
                    brightPixels++;
                }
                if (luma >= 18) {
                    signalPixels++;
                }
            }
        }
        int inspectedPixels = inspectedHeight * image.getWidth();
        assertTrue(signalPixels >= inspectedPixels / 4, "gora wyniku powinna zawierac ramke/tlo tooltipa itemu.");
        assertTrue(brightPixels < inspectedPixels / 3, "gora wyniku nie powinna byc jasnym paskiem zewnetrznego UI.");
    }

    private static void assertImagesEqual(BufferedImage expected, BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertColor(actual, x, y, new Color(expected.getRGB(x, y)));
            }
        }
    }

    private static void assertColor(BufferedImage image, int x, int y, Color expected) {
        Color actual = new Color(image.getRGB(x, y));
        assertEquals(expected.getRed(), actual.getRed(), "red at " + x + "," + y);
        assertEquals(expected.getGreen(), actual.getGreen(), "green at " + x + "," + y);
        assertEquals(expected.getBlue(), actual.getBlue(), "blue at " + x + "," + y);
    }

    private record PointOffset(int x, int y) {
    }

    private static final class CapturingLogHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record != null && record.getMessage() != null) {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        private boolean contains(String text) {
            return messages.stream().anyMatch(message -> message.contains(text));
        }
    }
}
