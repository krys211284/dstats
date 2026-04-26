package krys.itemimport;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje heurystyczne wycięcie obszaru tekstowego i przygotowanie wariantów OCR. */
class ItemImageOcrPreprocessorTest {
    @Test
    void shouldCropToTextColumnAndPrepareScaledVariants() {
        BufferedImage screenshot = buildDecoratedTooltipScreenshot();

        List<ItemImageOcrVariant> variants = new ItemImageOcrPreprocessor().prepareVariants(screenshot);

        assertEquals(
                List.of(
                        "original",
                        "text-crop",
                        "text-crop-gray-x2-contrast",
                        "text-crop-gray-x3-threshold",
                        "text-crop-gray-x3-sharpen",
                        "bottom-effect-x4"
                ),
                variants.stream().map(ItemImageOcrVariant::getVariantId).toList()
        );

        ItemImageOcrVariant croppedVariant = variants.get(1);
        assertTrue(croppedVariant.getSourceX() > 650, "crop powinien odciąć lewą grafikę itemu");
        assertTrue(croppedVariant.getSourceWidth() < 700, "crop powinien skupić się na kolumnie tekstowej");
        assertTrue(croppedVariant.getSourceY() + croppedVariant.getSourceHeight() < 980,
                "crop powinien odciąć dolny overlay");
        assertTrue(variants.get(2).getImage().getWidth() > croppedVariant.getImage().getWidth());
        assertTrue(variants.get(3).getImage().getWidth() > variants.get(2).getImage().getWidth());
        assertTrue(variants.get(5).getSourceY() > screenshot.getHeight() / 2,
                "dodatkowy crop efektu powinien sprawdzać dolną część tooltipa");
        assertTrue(variants.get(5).getImage().getWidth() > screenshot.getWidth(),
                "dodatkowy crop efektu powinien być mocno powiększony dla OCR");
    }

    private static BufferedImage buildDecoratedTooltipScreenshot() {
        BufferedImage image = new BufferedImage(1600, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(10, 11, 16));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        graphics.setColor(new Color(152, 120, 44));
        graphics.drawRect(28, 24, image.getWidth() - 56, image.getHeight() - 48);

        graphics.setColor(new Color(36, 60, 160));
        graphics.fillRoundRect(140, 160, 410, 520, 40, 40);
        graphics.setColor(new Color(190, 70, 32));
        graphics.fillOval(210, 240, 230, 260);

        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 42));
        graphics.setColor(new Color(236, 228, 208));
        int y = 180;
        for (String line : List.of("TARCZA", "+114 DO SILY", "+494 DO CIERNI", "20,0% SZANSY NA BLOK")) {
            graphics.drawString(line, 840, y);
            y += 120;
        }

        graphics.setColor(new Color(245, 245, 245));
        graphics.fillRect(0, 1040, image.getWidth(), 120);
        graphics.setColor(new Color(28, 28, 28));
        graphics.drawString("PRZEWIN W DOL", 1080, 1120);
        graphics.dispose();
        return image;
    }
}
