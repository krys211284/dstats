package krys.itemimport;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Diagnostyka realnego Windows OCR uruchamiana tylko po fladze testowej. */
class ItemImageImportRealOcrDiagnosticTest {
    @Test
    void shouldWriteVerathielV2OcrDiagnosticWhenEnabled() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("dstats.runOcrDiagnostics"));
        Path imagePath = Path.of("src/test/resources/items/verathiel-miecz-v2.png");
        byte[] imageBytes = Files.readAllBytes(imagePath);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        ItemImageMetadata metadata = new ItemImageMetadata(
                imagePath.getFileName().toString(),
                "image/png",
                "PNG",
                image.getWidth(),
                image.getHeight()
        );
        ItemImageOcrPreprocessor preprocessor = new ItemImageOcrPreprocessor();
        List<ItemImageOcrVariant> imageVariants = preprocessor.prepareVariants(image);
        List<ItemImageOcrTextVariant> textVariants = new WindowsItemOcrTextReader().readTextVariants(imageVariants);
        ItemImageImportTextParser parser = new ItemImageImportTextParser();
        ItemImageImportCandidateMerger merger = new ItemImageImportCandidateMerger();
        Method normalizedLinesMethod = ItemImageImportTextParser.class.getDeclaredMethod("normalizedLines", String.class);
        normalizedLinesMethod.setAccessible(true);

        StringBuilder report = new StringBuilder();
        report.append("# OCR diagnostic for ").append(imagePath).append("\n\n");
        report.append("image=").append(image.getWidth()).append("x").append(image.getHeight()).append("\n");
        report.append("variants=").append(imageVariants.size()).append("\n\n");

        List<ItemImageImportCandidateParseResult> parsedVariants = textVariants.stream()
                .map(variant -> parser.parse(metadata, variant.getText()))
                .toList();
        for (ItemImageOcrTextVariant variant : textVariants) {
            report.append("---").append(variant.getVariantId()).append("---\n");
            report.append(variant.getText()).append("\n");
            @SuppressWarnings("unchecked")
            List<String> normalizedLines = (List<String>) normalizedLinesMethod.invoke(parser, variant.getText());
            report.append("[normalized]\n");
            for (String line : normalizedLines) {
                report.append(line).append("\n");
            }
            ItemImageImportCandidateParseResult parsed = parser.parse(metadata, variant.getText());
            report.append("[full-read-lines]\n");
            for (FullItemReadLine line : parsed.getFullItemRead().getLines()) {
                report.append(line.getType()).append(": ").append(line.getText()).append("\n");
            }
            ItemImportEditableForm variantForm = new ItemImportEditableFormFactory().create(parsed);
            report.append("[affixes]\n");
            appendAffixes(report, variantForm.getAffixes());
            report.append("\n");
        }

        ItemImageImportCandidateParseResult merged = merger.merge(metadata, textVariants.size(), parsedVariants);
        ItemImportEditableForm mergedForm = new ItemImportEditableFormFactory().create(merged);
        report.append("---merged---\n");
        report.append("[full-read-lines]\n");
        for (FullItemReadLine line : merged.getFullItemRead().getLines()) {
            report.append(line.getType()).append(": ").append(line.getText()).append("\n");
        }
        report.append("[affixes]\n");
        appendAffixes(report, mergedForm.getAffixes());

        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/verathiel-miecz-v2-ocr-diagnostic.txt"), report.toString());
    }

    private static void appendAffixes(StringBuilder report, List<ImportedItemAffix> affixes) {
        for (ImportedItemAffix affix : affixes) {
            report.append(affix.getType())
                    .append(" value=").append(affix.getValue())
                    .append(" range=").append(affix.getRollRangeMin()).append("-").append(affix.getRollRangeMax())
                    .append(" source=").append(affix.getSourceText())
                    .append("\n");
        }
    }
}
