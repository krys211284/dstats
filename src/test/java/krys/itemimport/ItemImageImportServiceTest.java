package krys.itemimport;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test realnego rozpoznania ograniczonych pól foundation z pojedynczego screena itemu. */
class ItemImageImportServiceTest {
    private static final List<String> STABLE_SHIELD_LINES = List.of(
            "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU",
            "Starożytna legendarna tarcza",
            "Moc przedmiotu: 800",
            "Pancerz: 1 131 pkt.",
            "45% redukcji blokowanych obrażeń [45]%",
            "20,0% szansy na blok [20,01]%",
            "+100% obrażeń od broni w głównej ręce [100]%",
            "+114 siły [107 - 121]",
            "+494 cierni [473 - 506]",
            "+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%",
            "13,2% redukcji czasu odnowienia",
            "Rozjuszenie: +8% do szans na trafienie krytyczne za każdą rangę serii zabójstw [8]%",
            "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%",
            "Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek.",
            "Puste gniazdo"
    );

    @Test
    void shouldMergeFieldsAcrossPreparedVariantsWithoutChangingImportFlow() throws Exception {
        byte[] imageBytes = buildShieldLikeScreenshot();
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new FakeOcrTextReader(Map.of(
                        "original", "PRZEWIN W DOL",
                        "text-crop", "Tarcza",
                        "text-crop-gray-x2-contrast", "+114 do siły [107 - 121]",
                        "text-crop-gray-x3-threshold", "+494 do cierni [473 - 506]\n+20,0% szansy na blok [18,0 - 22,5]",
                        "text-crop-gray-x3-sharpen", ""
                )),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(
                new ItemImageImportRequest("shield-like.png", "image/png", imageBytes)
        );

        assertEquals("shield-like.png", result.getImageMetadata().getOriginalFilename());
        assertEquals("OFF_HAND", result.getSlotCandidate().getSuggestedValue().name());
        assertEquals(114.0d, result.getStrengthCandidate().getSuggestedValue(),
                () -> result.getStrengthCandidate().getRawValue());
        assertEquals(494.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertNull(result.getIntelligenceCandidate().getSuggestedValue());
    }

    @Test
    void shouldMapShieldFixtureFromCapturedRealOcrSnapshotWithoutHeaderNumberLeak() throws Exception {
        byte[] imageBytes = Files.readAllBytes(Path.of("src/test/resources/items/tarcza.png"));
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new CapturedOcrSnapshotReader(Path.of("src/test/resources/items/tarcza-windows-ocr-snapshot.txt")),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(
                new ItemImageImportRequest("tarcza.png", "image/png", imageBytes)
        );

        assertEquals("tarcza.png", result.getImageMetadata().getOriginalFilename());
        assertEquals("OFF_HAND", result.getSlotCandidate().getSuggestedValue().name());
        assertEquals(114.0d, result.getStrengthCandidate().getSuggestedValue(),
                () -> result.getStrengthCandidate().getRawValue());
        assertEquals(494.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertNull(result.getWeaponDamageCandidate().getSuggestedValue());
        assertNotEquals(800.0d, result.getStrengthCandidate().getSuggestedValue());
        assertNotEquals(1131.0d, result.getStrengthCandidate().getSuggestedValue());
        assertNotEquals(800.0d, result.getThornsCandidate().getSuggestedValue());
        assertNotEquals(1131.0d, result.getThornsCandidate().getSuggestedValue());
        assertNotEquals(45.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertNotEquals(100.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertTrue(result.getFullItemRead().getLines().stream()
                .map(FullItemReadLine::getText)
                .anyMatch(line -> line.contains("Moc przedmiotu: 800")));
    }

    @Test
    void shouldReadFullShieldItemFromFixtureAndKeepFoundationMappingSeparate() throws Exception {
        byte[] imageBytes = Files.readAllBytes(Path.of("src/test/resources/items/tarcza.png"));
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new FakeOcrTextReader(Map.of(
                        "original", String.join("\n", STABLE_SHIELD_LINES),
                        "text-crop", "Starożytna legendarna tarcza\nPancerz: 1 131 pkt.\n45% redukcji blokowanych obrażeń [45]%",
                        "text-crop-gray-x2-contrast", "+114 siły [107 - 121]\n+494 cierni [473 - 506]\n+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%",
                        "text-crop-gray-x3-threshold", "20,0% szansy na blok [20,01]%\n+100% obrażeń od broni w głównej ręce [100]%",
                        "text-crop-gray-x3-sharpen", "Moc przedmiotu: 800\n13,2% redukcji czasu odnowienia"
                )),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(
                new ItemImageImportRequest("tarcza.png", "image/png", imageBytes)
        );

        assertEquals("tarcza.png", result.getImageMetadata().getOriginalFilename());
        assertEquals("OFF_HAND", result.getSlotCandidate().getSuggestedValue().name());
        assertEquals(114.0d, result.getStrengthCandidate().getSuggestedValue());
        assertEquals(494.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertNull(result.getWeaponDamageCandidate().getSuggestedValue());
        assertNotEquals(800.0d, result.getStrengthCandidate().getSuggestedValue());
        assertNotEquals(1131.0d, result.getStrengthCandidate().getSuggestedValue());
        assertNotEquals(45.0d, result.getStrengthCandidate().getSuggestedValue());
        assertNotEquals(100.0d, result.getStrengthCandidate().getSuggestedValue());
        assertNotEquals(800.0d, result.getThornsCandidate().getSuggestedValue());
        assertNotEquals(1131.0d, result.getThornsCandidate().getSuggestedValue());
        assertNotEquals(45.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertNotEquals(100.0d, result.getBlockChanceCandidate().getSuggestedValue());

        assertEquals("NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU", result.getFullItemRead().getItemName());
        assertEquals("Starożytna legendarna tarcza", result.getFullItemRead().getItemTypeLine());
        assertEquals("Starożytna legendarna tarcza", result.getFullItemRead().getRarity());
        assertEquals("Moc przedmiotu: 800", result.getFullItemRead().getItemPower());
        assertEquals("Pancerz: 1 131 pkt.", result.getFullItemRead().getBaseItemValue());
        List<String> fullReadLines = result.getFullItemRead().getLines().stream()
                .map(FullItemReadLine::getText)
                .toList();
        for (String expectedLine : STABLE_SHIELD_LINES) {
            assertTrue(fullReadLines.contains(expectedLine), "Brak stabilnej linii tarczy: " + expectedLine);
        }
        assertTrue(result.getFullItemRead().getLines().stream().anyMatch(line -> line.getType() == FullItemReadLineType.SOCKET));
    }

    @Test
    void shouldReadFullBootItemFromFixtureWithoutHallucinatingFoundationStats() throws Exception {
        byte[] imageBytes = Files.readAllBytes(Path.of("src/test/resources/items/buty.png"));
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new FakeOcrTextReader(Map.of(
                        "original", "Marsz Pokutnika\nRzadki przedmiot\nButy\n800 mocy przedmiotu\n354 pkt. pancerza\n+12,5% szybkości ruchu\n+7,0% uniku\n2 gniazda",
                        "text-crop", "Buty\n+12,5% szybkości ruchu",
                        "text-crop-gray-x2-contrast", "+7,0% uniku",
                        "text-crop-gray-x3-threshold", "2 gniazda",
                        "text-crop-gray-x3-sharpen", "800 mocy przedmiotu"
                )),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(
                new ItemImageImportRequest("buty.png", "image/png", imageBytes)
        );

        assertEquals("BOOTS", result.getSlotCandidate().getSuggestedValue().name());
        assertNull(result.getWeaponDamageCandidate().getSuggestedValue());
        assertNull(result.getStrengthCandidate().getSuggestedValue());
        assertNull(result.getIntelligenceCandidate().getSuggestedValue());
        assertNull(result.getThornsCandidate().getSuggestedValue());
        assertNull(result.getBlockChanceCandidate().getSuggestedValue());
        assertNull(result.getRetributionChanceCandidate().getSuggestedValue());
        assertEquals("Marsz Pokutnika", result.getFullItemRead().getItemName());
        assertEquals("Buty", result.getFullItemRead().getItemTypeLine());
        assertEquals("800 mocy przedmiotu", result.getFullItemRead().getItemPower());
        assertTrue(result.getFullItemRead().getLines().stream().anyMatch(line -> line.getText().contains("+12,5% szybkości ruchu")));
        assertTrue(result.getFullItemRead().getLines().stream().anyMatch(line -> line.getType() == FullItemReadLineType.SOCKET));
        assertFalse(result.getFullItemRead().getLines().stream().anyMatch(line -> line.getText().contains("Rozjuszenie")));
    }

    @Test
    void shouldRecognizeFoundationFieldsFromSingleItemScreenshot() throws Exception {
        Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"));
        byte[] imageBytes = buildSyntheticItemScreenshot();

        ItemImageImportCandidateParseResult result = new ItemImageImportService().analyze(
                new ItemImageImportRequest("synthetic-item.png", "image/png", imageBytes)
        );

        assertEquals("synthetic-item.png", result.getImageMetadata().getOriginalFilename());
        assertNotNull(result.getSlotCandidate().getSuggestedValue());
        assertEquals("MAIN_HAND", result.getSlotCandidate().getSuggestedValue().name());
        assertEquals(321L, result.getWeaponDamageCandidate().getSuggestedValue());
        assertEquals(55.0d, result.getStrengthCandidate().getSuggestedValue());
        assertEquals(13.0d, result.getIntelligenceCandidate().getSuggestedValue());
        assertEquals(90.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(18.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertNotNull(result.getRetributionChanceCandidate().getSuggestedValue());
    }

    private static byte[] buildSyntheticItemScreenshot() throws Exception {
        BufferedImage image = new BufferedImage(1400, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(12, 14, 18));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 34));
        graphics.setColor(new Color(240, 232, 214));

        String[] lines = {
                "ITEM TYPE MAIN HAND",
                "WEAPON DAMAGE 321",
                "STRENGTH 55",
                "INTELLIGENCE 13",
                "THORNS 90",
                "BLOCK CHANCE 18",
                "RETRIBUTION CHANCE 25"
        };

        int y = 120;
        for (String line : lines) {
            graphics.drawString(line, 100, y);
            y += 95;
        }
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private static byte[] buildShieldLikeScreenshot() throws Exception {
        BufferedImage image = new BufferedImage(1600, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(12, 14, 18));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(132, 100, 32));
        graphics.drawRect(26, 24, image.getWidth() - 52, image.getHeight() - 48);

        graphics.setColor(new Color(40, 58, 150));
        graphics.fillRoundRect(120, 170, 390, 500, 40, 40);
        graphics.setColor(new Color(190, 72, 38));
        graphics.fillOval(210, 250, 210, 240);

        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 42));
        graphics.setColor(new Color(238, 229, 209));
        int y = 190;
        for (String line : List.of("TARCZA", "+114 DO SILY", "+494 DO CIERNI", "20,0% SZANSY NA BLOK")) {
            graphics.drawString(line, 840, y);
            y += 120;
        }

        graphics.setColor(new Color(245, 245, 245));
        graphics.fillRect(0, 1050, image.getWidth(), 110);
        graphics.setColor(new Color(24, 24, 24));
        graphics.drawString("PRZEWIN W DOL", 1080, 1125);
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private static final class FakeOcrTextReader implements ItemImageOcrTextReader {
        private final Map<String, String> variantTexts;

        private FakeOcrTextReader(Map<String, String> variantTexts) {
            this.variantTexts = variantTexts;
        }

        @Override
        public List<ItemImageOcrTextVariant> readTextVariants(List<ItemImageOcrVariant> variants) {
            return variants.stream()
                    .map(variant -> new ItemImageOcrTextVariant(
                            variant.getVariantId(),
                            variantTexts.getOrDefault(variant.getVariantId(), "")
                    ))
                    .toList();
        }
    }

    /** Czytnik snapshotu realnego Windows OCR zebranego z tej samej ścieżki preprocessing co aplikacja. */
    private static final class CapturedOcrSnapshotReader implements ItemImageOcrTextReader {
        private final Map<String, String> variantTexts;

        private CapturedOcrSnapshotReader(Path snapshotPath) throws Exception {
            this.variantTexts = parseSnapshot(snapshotPath);
        }

        @Override
        public List<ItemImageOcrTextVariant> readTextVariants(List<ItemImageOcrVariant> variants) {
            return variants.stream()
                    .map(variant -> new ItemImageOcrTextVariant(
                            variant.getVariantId(),
                            variantTexts.getOrDefault(variant.getVariantId(), "")
                    ))
                    .toList();
        }

        private static Map<String, String> parseSnapshot(Path snapshotPath) throws Exception {
            Map<String, StringBuilder> builders = new LinkedHashMap<>();
            String currentVariantId = null;
            for (String line : Files.readAllLines(snapshotPath)) {
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("---") && line.endsWith("---")) {
                    currentVariantId = line.substring(3, line.length() - 3);
                    builders.putIfAbsent(currentVariantId, new StringBuilder());
                    continue;
                }
                if (currentVariantId == null) {
                    continue;
                }
                StringBuilder builder = builders.get(currentVariantId);
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(line);
            }
            Map<String, String> snapshot = new LinkedHashMap<>();
            for (Map.Entry<String, StringBuilder> entry : builders.entrySet()) {
                snapshot.put(entry.getKey(), entry.getValue().toString());
            }
            return snapshot;
        }
    }
}
