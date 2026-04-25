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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Test realnego rozpoznania ograniczonych pól foundation z pojedynczego screena itemu. */
class ItemImageImportServiceTest {
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
        assertEquals(114.0d, result.getStrengthCandidate().getSuggestedValue());
        assertEquals(494.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertNull(result.getIntelligenceCandidate().getSuggestedValue());
    }

    @Test
    void shouldReadFullShieldItemFromFixtureAndKeepFoundationMappingSeparate() throws Exception {
        byte[] imageBytes = Files.readAllBytes(Path.of("src/test/resources/items/tarcza.png"));
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new FakeOcrTextReader(Map.of(
                        "original", "Nieugaszony Bastion\nLegendarny przedmiot\nTarcza\n800 mocy przedmiotu\n1 131 pkt. pancerza\n+114 do siły [107 - 121]\n+494 cierni [473 - 506]\n+20,0% szansy na blok [18,0 - 22,5]\nAspekt niezłomnej ściany",
                        "text-crop", "Tarcza\n1 131 pkt. pancerza",
                        "text-crop-gray-x2-contrast", "+114 do siły [107 - 121]\n+494 cierni [473 - 506]",
                        "text-crop-gray-x3-threshold", "+20,0% szansy na blok [18,0 - 22,5]",
                        "text-crop-gray-x3-sharpen", "800 mocy przedmiotu"
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
        assertEquals("Nieugaszony Bastion", result.getFullItemRead().getItemName());
        assertEquals("Tarcza", result.getFullItemRead().getItemTypeLine());
        assertEquals("Legendarny przedmiot", result.getFullItemRead().getRarity());
        assertEquals("800 mocy przedmiotu", result.getFullItemRead().getItemPower());
        assertEquals("1 131 pkt. pancerza", result.getFullItemRead().getBaseItemValue());
        assertEquals(true, result.getFullItemRead().getLines().stream().anyMatch(line -> line.getText().contains("Aspekt niezłomnej ściany")));
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
        assertNull(result.getStrengthCandidate().getSuggestedValue());
        assertNull(result.getThornsCandidate().getSuggestedValue());
        assertNull(result.getBlockChanceCandidate().getSuggestedValue());
        assertEquals("Marsz Pokutnika", result.getFullItemRead().getItemName());
        assertEquals("Buty", result.getFullItemRead().getItemTypeLine());
        assertEquals("800 mocy przedmiotu", result.getFullItemRead().getItemPower());
        assertEquals(true, result.getFullItemRead().getLines().stream().anyMatch(line -> line.getText().contains("+12,5% szybkości ruchu")));
        assertEquals(true, result.getFullItemRead().getLines().stream().anyMatch(line -> line.getType() == FullItemReadLineType.SOCKET));
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
}
