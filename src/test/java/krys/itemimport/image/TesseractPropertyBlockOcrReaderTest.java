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
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regresje Tess4J dla neutralnego OCR pojedynczych PropertyBlock. */
@OcrIntegrationTest
class TesseractPropertyBlockOcrReaderTest {
    private final ItemImageAssembler assembler = ImageAssemblyFixtures::assemble;
    private final OpenCvHorizontalDividerDetector dividerDetector = new OpenCvHorizontalDividerDetector();
    private final ItemPartStructureNormalizer normalizer = new DeterministicItemPartStructureNormalizer();
    private final PropertyBlockSegmenter segmenter = new VisualPropertyBlockSegmenter();

    @Test
    void shouldReadSyntheticPropertyBlockAndKeepNeutralMetadata() throws Exception {
        TesseractPropertyBlockOcrReader reader = new TesseractPropertyBlockOcrReader();
        PropertyBlock block = new PropertyBlock(
                StructuralItemPartSlot.PART_3,
                2,
                0,
                0,
                560,
                110,
                PropertyBlockAnchorType.DIAMOND,
                null,
                "PNG",
                blockPng("+150 siły"),
                "lineCount=1"
        );

        PropertyBlockReadResult result = reader.read(block);

        assertEquals(StructuralItemPartSlot.PART_3, result.getSourcePart());
        assertEquals(2, result.getBlockOrder());
        assertEquals(PropertyBlockAnchorType.DIAMOND, result.getAnchorType());
        assertFalse(result.getRawText().isBlank(), result.getFailureDiagnostics().toString());
        assertTrue(result.getRawText().contains("150"), result.getRawText());
        assertTrue(result.getPreprocessing().contains("PROPERTY_BLOCK"), result.getPreprocessing());
        assertEquals(8, result.getProfileResults().size());
        assertTrue(result.getProfileResults().stream()
                .anyMatch(profile -> profile.getProfile().equals(result.getPreprocessing())
                        && profile.getRawText().equals(result.getRawText())));
        assertFalse(result.getNumericEvidence().isEmpty());
        assertTrue(Files.exists(reader.getTessdataDirectory().resolve("pol.traineddata")));
    }

    @Test
    void shouldReadEveryTarczaPropertyBlockWithExactNumbersAndSymbols() throws Exception {
        List<String> fixtures = List.of("tarcza1.png", "tarcza2.png");
        List<PropertyBlockReadResult> part2 = readBlocks(fixtures, StructuralItemPartSlot.PART_2);
        List<PropertyBlockReadResult> part3 = readBlocks(fixtures, StructuralItemPartSlot.PART_3);

        assertEquals(3, part2.size());
        assertText(part2.get(0), "1502", "pancerza");
        assertNumbers(part2.get(1), "20,0%", "20,0%");
        assertText(part2.get(1), "blok");
        assertNumbers(part2.get(2), "+100%", "100%");
        assertText(part2.get(2), "broni", "ręce");
        assertStartsWith(part2, "1502", "20,0", "+100");

        assertEquals(8, part3.size());
        assertNumbers(part3.get(0), "+217", "150-180");
        assertNumbers(part3.get(1), "+11,0%", "6,5-8,5");
        assertNumbers(part3.get(2), "17,6%", "11,0-15,0");
        assertNumbers(part3.get(3), "12,3%");
        assertFalse(normalized(part3.get(3)).contains("12,5%"), part3.get(3).getRawText());
        assertSelectedEvidence(part3.get(3), "12,3%");
        assertAlternativeEvidence(part3.get(3), 1, "12,5%");
        assertNumbers(part3.get(4), "+4", "1-15");
        assertText(part3.get(4), "jakości");
        assertNumbers(part3.get(5), "+12");
        assertText(part3.get(5), "maksymalnej liczby", "kumulacji Animuszu");

        PropertyBlockReadResult legendary = part3.get(6);
        assertTrue(nonBlankLineCount(legendary) >= 11, legendary.getRawText());
        assertText(legendary, "Naznaczenie", "Podstawową", "Szału Krwi", "szybkości ruchu");
        assertNumbers(legendary, "4%", "40-60%", "60%", "15%");
        assertText(part3.get(7), "Puste gniazdo");
        assertStartsWith(part3, "+217", "+11,0", "17,6", "12,3", "+4", "+12",
                "Naznaczenie", "Puste gniazdo");
    }

    @Test
    void shouldReadEveryDziedzicPropertyBlockWithExactNumbersAndSymbols() throws Exception {
        List<PropertyBlockReadResult> blocks = readBlocks(
                List.of("dziedzic-zatracenia-helm-1.png", "dziedzic-zatracenia-helm-2.png"),
                StructuralItemPartSlot.PART_3);

        assertEquals(10, blocks.size());
        assertText(blocks.get(0), "2 004", "pancerza");
        assertNumbers(blocks.get(0), "+32,8%");
        assertNumbers(blocks.get(1), "+15,0%", "12,0%");
        assertNumbers(blocks.get(2), "+25,0%");
        assertNumbers(blocks.get(3), "20,0%");
        assertNumbers(blocks.get(4), "+25%", "20%", "+3", "+115");
        assertText(blocks.get(4), "szybkości ruchu", "Główne", "wszystkich");
        assertNumbers(blocks.get(5), "75-100");
        assertNumbers(blocks.get(6), "+12");
        assertText(blocks.get(6), "kumulacji Animuszu");
        assertNumbers(blocks.get(7), "80%");
        assertTrue(nonBlankLineCount(blocks.get(7)) >= 6, blocks.get(7).getRawText());
        assertText(blocks.get(7), "Poddaj się nienawiści", "Łaski Matki", "sojusznikom");
        assertText(blocks.get(8), "Puste gniazdo");
        assertText(blocks.get(9), "Puste gniazdo");
    }

    @Test
    void shouldReadCriticalNumbersFromDziedzicFullFixture() throws Exception {
        List<PropertyBlockReadResult> reads = readBlocks(
                List.of("dziedzic-zatracenia-helm-full.png"), StructuralItemPartSlot.PART_3);

        PropertyBlockReadResult block1 = reads.get(0);
        PropertyBlockReadResult block2 = reads.get(1);
        PropertyBlockReadResult block3 = reads.get(2);
        PropertyBlockReadResult block6 = reads.get(5);
        PropertyBlockReadResult block9 = reads.get(8);

        assertEquals(1, block1.getBlockOrder());
        assertIntegerIgnoringGrouping(block1, "2004");
        assertText(block1, "pancerza");
        assertSelectedEvidence(block1, "2004");
        assertAlternativeEvidence(block1, 1, "20604");

        assertEquals(2, block2.getBlockOrder());
        assertNumbers(block2, "+15,0%", "12,0%");
        assertNotCertainSelectedEvidence(block2, 2, "12,01%");

        assertEquals(3, block3.getBlockOrder());
        assertNumbers(block3, "+25,0%", "20,0%");

        assertEquals(6, block6.getBlockOrder());
        assertNumbers(block6, "+115", "75-100");
        assertText(block6, "wszystkich", "współczynników");
        assertTrue(block6.getRawText().stripLeading().startsWith("+115"), block6.getRawText());
        assertAmbiguousEvidence(block6, 1, "+115", "1115", "115");
        assertSelectedEvidenceAt(block6, 2, "75-100");
        assertAlternativeEvidence(block6, 2, "75-1001", "75-106");

        assertEquals(9, block9.getBlockOrder());
        assertNumbers(block9, "+150");
        assertText(block9, "siły");
        assertTrue(block9.getRawText().stripLeading().startsWith("+150"), block9.getRawText());
    }

    @Test
    void shouldReadEveryVerathielPropertyBlockWithExactNumbersAndSymbols() throws Exception {
        List<PropertyBlockReadResult> blocks = readBlocks(
                List.of("verathiel-miecz-v2.png"), StructuralItemPartSlot.PART_3);

        assertEquals(6, blocks.size());
        assertText(blocks.get(0), "1 874", "obrażeń na sek.", "trafienie");
        assertNumbers(blocks.get(0), "1 390-2 018", "1,10");
        assertNumbers(blocks.get(1), "+134", "94-157");
        assertNumbers(blocks.get(2), "+172", "150-180");
        assertNumbers(blocks.get(3), "+300", "300");
        assertNumbers(blocks.get(4), "16%", "15-30%");
        assertNumbers(blocks.get(5), "100%", "70-100%", "25");
        assertText(blocks.get(5), "Umiejętności Podstawowe", "podstawowego zasobu");
    }

    @Test
    void shouldKeepRepresentativeNumbersStableUnderMinorImageVariations() throws Exception {
        List<PropertyBlock> tarcza = propertyBlocks(
                List.of("tarcza1.png", "tarcza2.png"), StructuralItemPartSlot.PART_3);
        List<PropertyBlock> dziedzic = propertyBlocks(
                List.of("dziedzic-zatracenia-helm-full.png"), StructuralItemPartSlot.PART_3);
        List<PropertyBlock> verathiel = propertyBlocks(
                List.of("verathiel-miecz-v2.png"), StructuralItemPartSlot.PART_3);
        List<NumericStabilityCase> cases = List.of(
                new NumericStabilityCase("Dziedzic #1", dziedzic.get(0), "2004"),
                new NumericStabilityCase("Dziedzic #2", dziedzic.get(1), null, "+15,0%", "12,0%"),
                new NumericStabilityCase("Dziedzic #6", dziedzic.get(5), null, "+115", "75-100"),
                new NumericStabilityCase("Tarcza #4", tarcza.get(3), null, "12,3%"),
                new NumericStabilityCase("Verathiel #6", verathiel.get(5), null,
                        "100%", "70-100%", "25")
        );
        TesseractPropertyBlockOcrReader reader = new TesseractPropertyBlockOcrReader();

        for (NumericStabilityCase testCase : cases) {
            assertStableNumbers(testCase, reader.read(resized(testCase.block(), 1.03d)), "resize 1,03x");
            if (!testCase.label().equals("Verathiel #6")) {
                assertStableNumbers(testCase, reader.read(adjustedTone(testCase.block(), 1.03d, 4)),
                        "contrast 1,03 + brightness 4");
            }
        }

        PropertyBlockReadResult perturbedDziedzic = reader.read(adjustedTone(dziedzic.get(1), 1.02d, 2));
        assertAmbiguousEvidence(perturbedDziedzic, 1, "+15,0%", "+15,07%", "+15,076");
        assertAmbiguousEvidence(perturbedDziedzic, 2, "12,0%", "12,01%");

        PropertyBlockReadResult perturbedVerathiel = reader.read(adjustedTone(verathiel.get(5), 1.03d, 4));
        assertAmbiguousEvidence(perturbedVerathiel, 1, "100%", "1007%", "1", "10074");
        assertSelectedEvidenceAt(perturbedVerathiel, 2, "70-100%");
        assertSelectedEvidenceAt(perturbedVerathiel, 3, "25");
    }

    private List<PropertyBlockReadResult> readBlocks(List<String> filenames,
                                                     StructuralItemPartSlot slot) throws Exception {
        TesseractPropertyBlockOcrReader reader = new TesseractPropertyBlockOcrReader();
        List<PropertyBlock> blocks = propertyBlocks(filenames, slot);
        List<PropertyBlockReadResult> reads = blocks.stream().map(reader::read).toList();
        // Wariant pary Dziedzica ma osobne dotychczasowe expected; pozostałe wejścia są źródłem downstream fixtures.
        if (!filenames.contains("dziedzic-zatracenia-helm-1.png")) {
            for (int i = 0; i < blocks.size(); i++) {
                OcrGoldenFixtures.captureBlock(filenames, blocks.get(i), reads.get(i));
            }
            List<OcrGoldenFixtures.BlockRead> golden = OcrGoldenFixtures.blocks(filenames, slot);
            OcrGoldenFixtures.assertSameRead(golden.stream().map(OcrGoldenFixtures.BlockRead::block).toList(), blocks);
            OcrGoldenFixtures.assertSameRead(golden.stream().map(OcrGoldenFixtures.BlockRead::read).toList(), reads);
        }
        return reads;
    }

    private List<PropertyBlock> propertyBlocks(List<String> filenames,
                                               StructuralItemPartSlot slot) throws Exception {
        AssembledItemImage image = assembler.assemble(filenames.stream()
                .map(TesseractPropertyBlockOcrReaderTest::request)
                .toList());
        StructuralItemPart part = normalizer.normalize(dividerDetector.detect(image).getParts())
                .find(slot)
                .orElseThrow();
        return segmenter.segment(part).getBlocks();
    }

    private static void assertNumbers(PropertyBlockReadResult result, String... expected) {
        String normalized = normalized(result);
        for (String token : expected) {
            String tokenPattern = token.endsWith("%")
                    ? Pattern.quote(token.substring(0, token.length() - 1)) + "\\]?%"
                    : Pattern.quote(token);
            Pattern pattern = Pattern.compile("(?<!\\d)" + tokenPattern + "(?!\\d)");
            assertTrue(pattern.matcher(normalized).find(),
                    () -> "Brak dokładnego tokenu liczbowego " + token + " w OCR: " + result.getRawText());
        }
    }

    private static void assertIntegerIgnoringGrouping(PropertyBlockReadResult result, String expectedDigits) {
        String digitPattern = expectedDigits.chars()
                .mapToObj(character -> Pattern.quote(Character.toString((char) character)))
                .reduce((left, right) -> left + "\\s*" + right)
                .orElseThrow();
        Pattern pattern = Pattern.compile("(?<!\\d)" + digitPattern + "(?!\\d)");
        assertTrue(pattern.matcher(result.getRawText()).find(),
                () -> "Brak wartości liczbowej " + expectedDigits + " w OCR: " + result.getRawText());
    }

    private static void assertStableNumbers(NumericStabilityCase testCase,
                                            PropertyBlockReadResult result,
                                            String variation) {
        if (testCase.groupedInteger() != null) {
            assertIntegerIgnoringGrouping(result, testCase.groupedInteger());
        }
        try {
            assertNumbers(result, testCase.expectedTokens());
        } catch (AssertionError error) {
            throw new AssertionError(testCase.label() + " / " + variation + ": " + error.getMessage(), error);
        }
    }

    private static void assertSelectedEvidence(PropertyBlockReadResult result, String... expectedValues) {
        assertEquals(expectedValues.length, result.getNumericEvidence().size(),
                () -> "Nieoczekiwana liczba tokenów evidence: " + describeEvidence(result));
        for (int index = 0; index < expectedValues.length; index++) {
            PropertyBlockNumericEvidence evidence = result.getNumericEvidence().get(index);
            assertFalse(evidence.isAmbiguous(), () -> "Niejednoznaczne evidence: " + describeEvidence(result));
            assertEquals(expectedValues[index], evidence.getSelectedValue(),
                    () -> "Nieoczekiwane selectedValue: " + describeEvidence(result));
            assertEquals(evidence.getSupportingProfiles().size(), evidence.getSupportCount());
            assertTrue(evidence.getSupportCount() >= 2, () -> "Za słabe evidence: " + describeEvidence(result));
        }
        assertFalse(result.getProfileResults().isEmpty());
    }

    private static void assertAlternativeEvidence(PropertyBlockReadResult result,
                                                  int tokenOrder,
                                                  String... expectedAlternatives) {
        PropertyBlockNumericEvidence evidence = result.getNumericEvidence().stream()
                .filter(candidate -> candidate.getTokenOrder() == tokenOrder)
                .findFirst()
                .orElseThrow();
        for (String expectedAlternative : expectedAlternatives) {
            assertTrue(evidence.getAlternativeValues().contains(expectedAlternative),
                    () -> "Brak alternatywy " + expectedAlternative + ": " + describeEvidence(result));
        }
    }

    private static void assertAmbiguousEvidence(PropertyBlockReadResult result,
                                                int tokenOrder,
                                                String... expectedValues) {
        PropertyBlockNumericEvidence evidence = result.getNumericEvidence().stream()
                .filter(candidate -> candidate.getTokenOrder() == tokenOrder)
                .findFirst()
                .orElseThrow();
        assertTrue(evidence.isAmbiguous(), () -> "Evidence powinno być niejednoznaczne: " + describeEvidence(result));
        assertEquals("", evidence.getSelectedValue());
        for (String expectedValue : expectedValues) {
            assertTrue(evidence.getAlternativeValues().contains(expectedValue),
                    () -> "Brak wartości " + expectedValue + ": " + describeEvidence(result));
        }
        assertTrue(result.getFailureDiagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.startsWith("NUMERIC_OCR_AMBIGUOUS tokenOrder=" + tokenOrder)));
    }

    private static void assertNotCertainSelectedEvidence(PropertyBlockReadResult result,
                                                         int tokenOrder,
                                                         String forbiddenValue) {
        PropertyBlockNumericEvidence evidence = result.getNumericEvidence().stream()
                .filter(candidate -> candidate.getTokenOrder() == tokenOrder)
                .findFirst()
                .orElseThrow();
        assertFalse(!evidence.isAmbiguous() && forbiddenValue.equals(evidence.getSelectedValue()),
                () -> "Evidence nie może pewnie wybrać " + forbiddenValue + ": " + describeEvidence(result));
    }

    private static void assertSelectedEvidenceAt(PropertyBlockReadResult result,
                                                 int tokenOrder,
                                                 String expectedValue) {
        PropertyBlockNumericEvidence evidence = result.getNumericEvidence().stream()
                .filter(candidate -> candidate.getTokenOrder() == tokenOrder)
                .findFirst()
                .orElseThrow();
        assertFalse(evidence.isAmbiguous(), () -> "Niejednoznaczne evidence: " + describeEvidence(result));
        assertEquals(expectedValue, evidence.getSelectedValue(),
                () -> "Nieoczekiwane selectedValue: " + describeEvidence(result));
    }

    private static String describeEvidence(PropertyBlockReadResult result) {
        return result.getNumericEvidence().stream()
                .map(evidence -> evidence.getTokenOrder() + "=" + evidence.getSelectedValue()
                        + " support=" + evidence.getSupportCount()
                        + " alternatives=" + evidence.getAlternativeValues()
                        + " ambiguous=" + evidence.isAmbiguous())
                .toList()
                .toString();
    }

    private static PropertyBlock resized(PropertyBlock block, double scale) throws Exception {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(block.getImageBytes()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return transformedBlock(block, resized, scale, scale);
    }

    private static PropertyBlock adjustedTone(PropertyBlock block,
                                               double contrast,
                                               int brightness) throws Exception {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(block.getImageBytes()));
        BufferedImage adjusted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                Color color = new Color(source.getRGB(x, y));
                int red = adjustedChannel(color.getRed(), contrast, brightness);
                int green = adjustedChannel(color.getGreen(), contrast, brightness);
                int blue = adjustedChannel(color.getBlue(), contrast, brightness);
                adjusted.setRGB(x, y, new Color(red, green, blue).getRGB());
            }
        }
        return transformedBlock(block, adjusted, 1.0d, 1.0d);
    }

    private static int adjustedChannel(int value, double contrast, int brightness) {
        return Math.max(0, Math.min(255,
                (int) Math.round((value - 128) * contrast + 128 + brightness)));
    }

    private static PropertyBlock transformedBlock(PropertyBlock block,
                                                  BufferedImage image,
                                                  double scaleX,
                                                  double scaleY) throws Exception {
        PropertyBlockAnchor anchor = block.getAnchor()
                .map(original -> new PropertyBlockAnchor(
                        original.getType(),
                        block.getX() + (int) Math.round((original.getX() - block.getX()) * scaleX),
                        block.getY() + (int) Math.round((original.getY() - block.getY()) * scaleY),
                        Math.max(1, (int) Math.round(original.getWidth() * scaleX)),
                        Math.max(1, (int) Math.round(original.getHeight() * scaleY)),
                        original.getConfidence()))
                .orElse(null);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", output);
        return new PropertyBlock(
                block.getSourcePart(), block.getOrder(), block.getX(), block.getY(),
                image.getWidth(), image.getHeight(), block.getAnchorType(), anchor,
                "PNG", output.toByteArray(), block.getDiagnostics());
    }

    private static void assertStartsWith(List<PropertyBlockReadResult> results, String... expected) {
        assertEquals(expected.length, results.size());
        for (int index = 0; index < expected.length; index++) {
            String rawText = results.get(index).getRawText().stripLeading();
            assertTrue(rawText.startsWith(expected[index]),
                    "Nieoczekiwany anchor albo artefakt przed OCR bloku #" + (index + 1) + ": " + rawText);
        }
    }

    private static void assertText(PropertyBlockReadResult result, String... expected) {
        String normalized = normalized(result);
        for (String fragment : expected) {
            assertTrue(normalized.contains(fragment),
                    () -> "Brak tekstu " + fragment + " w OCR: " + result.getRawText());
        }
    }

    private static String normalized(PropertyBlockReadResult result) {
        return result.getRawText()
                .replaceAll("\\s+", " ")
                .replaceAll("\\+\\s+", "+")
                .replaceAll("\\s*-\\s*", "-")
                .trim();
    }

    private static long nonBlankLineCount(PropertyBlockReadResult result) {
        return result.getRawText().lines().map(String::trim).filter(line -> !line.isBlank()).count();
    }

    private static byte[] blockPng(String text) throws Exception {
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

    private static ItemImageImportRequest request(String filename) {
        try {
            return new ItemImageImportRequest(filename, "image/png",
                    Files.readAllBytes(Path.of("src/test/resources/items", filename)));
        } catch (Exception exception) {
            throw new IllegalStateException("Nie udalo sie wczytac fixture OCR PropertyBlock " + filename + ".", exception);
        }
    }

    private record NumericStabilityCase(String label,
                                        PropertyBlock block,
                                        String groupedInteger,
                                        String... expectedTokens) {
    }

}
