package krys.itemimport;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Diagnostyka realnego Windows OCR uruchamiana tylko po fladze testowej. */
class ItemImageImportRealOcrDiagnosticTest {
    @Test
    void shouldWriteVerathielV2OcrDiagnosticWhenEnabled() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("dstats.runOcrDiagnostics"));
        writeDiagnostic("verathiel-miecz-v2", List.of(Path.of("src/test/resources/items/verathiel-miecz-v2.png")));
    }

    @Test
    void shouldWriteHeirOfPerditionOcrDiagnosticWhenEnabled() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("dstats.runOcrDiagnostics"));
        writeDiagnostic("dziedzic-zatracenia-helm", List.of(
                Path.of("src/test/resources/items/dziedzic-zatracenia-helm-1.png"),
                Path.of("src/test/resources/items/dziedzic-zatracenia-helm-2.png")
        ));
    }

    private static void writeDiagnostic(String reportName, List<Path> imagePaths) throws Exception {
        ItemImageImportTextParser parser = new ItemImageImportTextParser();
        ItemImageImportCandidateMerger merger = new ItemImageImportCandidateMerger();
        Method normalizedLinesMethod = ItemImageImportTextParser.class.getDeclaredMethod("normalizedLines", String.class);
        normalizedLinesMethod.setAccessible(true);

        StringBuilder report = new StringBuilder();
        report.append("# OCR diagnostic for ").append(reportName).append("\n\n");
        List<ItemImageImportCandidateParseResult> allParsedVariants = new ArrayList<>();

        for (Path imagePath : imagePaths) {
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
            List<ItemImageImportCandidateParseResult> parsedVariants = textVariants.stream()
                    .map(variant -> parser.parse(metadata, variant.getText()))
                    .toList();
            allParsedVariants.addAll(parsedVariants);

            report.append("image=").append(imagePath).append(" ")
                    .append(image.getWidth()).append("x").append(image.getHeight()).append("\n");
            report.append("variants=").append(imageVariants.size()).append("\n\n");
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
        }

        ItemImageMetadata mergedMetadata = new ItemImageMetadata(reportName + ".png", "image/png", "PNG", 0, 0);
        ItemImageImportCandidateParseResult merged = merger.merge(mergedMetadata, allParsedVariants.size(), allParsedVariants);
        ItemImportEditableForm mergedForm = new ItemImportEditableFormFactory().create(merged);
        report.append("---merged---\n");
        report.append("[full-read-lines]\n");
        for (FullItemReadLine line : merged.getFullItemRead().getLines()) {
            report.append(line.getType()).append(": ").append(line.getText()).append("\n");
        }
        report.append("[affixes]\n");
        appendAffixes(report, mergedForm.getAffixes());

        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/" + reportName + "-ocr-diagnostic.txt"), report.toString());
    }

    private static void appendAffixes(StringBuilder report, List<ImportedItemAffix> affixes) {
        for (ImportedItemAffix affix : affixes) {
            report.append(affix.getType())
                    .append(" value=").append(affix.getValue())
                    .append(" reference=").append(affix.getReferenceValue())
                    .append(" range=").append(affix.getRollRangeMin()).append("-").append(affix.getRollRangeMax())
                    .append(" ga=").append(affix.isGreaterAffix())
                    .append(" source=").append(affix.getSourceText())
                    .append("\n");
        }
    }
}
