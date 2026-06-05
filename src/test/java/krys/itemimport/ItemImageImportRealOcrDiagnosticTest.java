package krys.itemimport;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Pierwszy punkt wejścia do diagnostyki realnego OCR dla obrazów itemów.
 *
 * <p>Test uruchamia realny preprocessing oraz realny OCR na wybranych fixture'ach obrazów itemów i zapisuje raw OCR
 * variants, debug evidence oraz materiał do ręcznej analizy błędów. Wyniki tej diagnostyki służą później do budowania
 * stabilnych testów mechanizmowych opartych o kontrolowane OCR text fixtures. To nie jest główna deterministyczna
 * regresja importu, bo realny OCR może zależeć od środowiska Windows, konfiguracji OCR i jakości konkretnego obrazu.</p>
 */
class ItemImageImportRealOcrDiagnosticTest {
    /**
     * Diagnozuje fixture `verathiel-miecz-v2.png` i zapisuje raport wariantów OCR dla pojedynczego screena broni.
     *
     * <p>Test jest częścią standardowego przebiegu testów. Służy do zebrania raw OCR, znormalizowanych linii,
     * full-read-lines i affixów jako smoke/diagnostic realnego OCR.</p>
     */
    @Test
    void shouldWriteVerathielV2OcrDiagnostic() throws Exception {
        // Fixture reprezentuje realny pojedynczy screen Odłamka Verathiela używany jako materiał do analizy OCR.
        writeDiagnostic("verathiel-miecz-v2", List.of(Path.of("src/test/resources/items/verathiel-miecz-v2.png")));
    }

    /**
     * Diagnozuje dwuekranowy fixture Dziedzica Zatracenia i zapisuje raport OCR dla górnej oraz dolnej części tooltipa.
     *
     * <p>Test jest standardowym smoke/diagnostic realnego OCR: jego wynik ma pomóc ustalić, które raw variants i które
     * etapy parsera tracą dane przed przeniesieniem przypadku do stabilnych fixture'ów tekstowych.</p>
     */
    @Test
    void shouldWriteHeirOfPerditionOcrDiagnostic() throws Exception {
        // Dwa obrazy odpowiadają ręcznie przewiniętemu tooltipowi: osobno górny i dolny screen itemu.
        writeDiagnostic("dziedzic-zatracenia-helm", List.of(
                Path.of("src/test/resources/items/dziedzic-zatracenia-helm-1.png"),
                Path.of("src/test/resources/items/dziedzic-zatracenia-helm-2.png")
        ));
    }

    private static void writeDiagnostic(String reportName, List<Path> imagePaths) throws Exception {
        ItemImageImportTextParser parser = new ItemImageImportTextParser();
        ItemImageImportCandidateMerger merger = new ItemImageImportCandidateMerger();
        // Raport pokazuje także wewnętrzną normalizację parsera, żeby łatwo porównać raw OCR z wejściem parsera.
        Method normalizedLinesMethod = ItemImageImportTextParser.class.getDeclaredMethod("normalizedLines", String.class);
        normalizedLinesMethod.setAccessible(true);

        StringBuilder report = new StringBuilder();
        report.append("# OCR diagnostic for ").append(reportName).append("\n\n");
        // Wszystkie warianty ze wszystkich screenów trafiają później przez ten sam candidate merger co normalny import.
        List<ItemImageImportCandidateParseResult> allParsedVariants = new ArrayList<>();

        for (Path imagePath : imagePaths) {
            // Ścieżki fixture'ów są względne wobec repo, żeby raport dało się odtworzyć lokalnie bez danych użytkownika.
            byte[] imageBytes = Files.readAllBytes(imagePath);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            ItemImageMetadata metadata = new ItemImageMetadata(
                    imagePath.getFileName().toString(),
                    "image/png",
                    "PNG",
                    image.getWidth(),
                    image.getHeight()
            );
            // Preprocessing musi być ten sam co w imporcie, bo diagnostyka ma pokazać realne warianty wejściowe OCR.
            ItemImageOcrPreprocessor preprocessor = new ItemImageOcrPreprocessor();
            List<ItemImageOcrVariant> imageVariants = preprocessor.prepareVariants(image);
            // Tu zaczyna się środowiskowo zmienna część: Windows OCR czyta każdy przygotowany wariant obrazu.
            List<ItemImageOcrTextVariant> textVariants = new WindowsItemOcrTextReader().readTextVariants(imageVariants);
            // Każdy raw OCR variant jest parsowany osobno, żeby raport pokazał, które warianty niosą brakujące dane.
            List<ItemImageImportCandidateParseResult> parsedVariants = textVariants.stream()
                    .map(variant -> parser.parse(metadata, variant.getText()))
                    .toList();
            allParsedVariants.addAll(parsedVariants);

            report.append("image=").append(imagePath).append(" ")
                    .append(image.getWidth()).append("x").append(image.getHeight()).append("\n");
            report.append("variants=").append(imageVariants.size()).append("\n\n");
            for (ItemImageOcrTextVariant variant : textVariants) {
                // Raw variant id pozwala powiązać tekst z konkretnym wariantem preprocessingu.
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
                // Formularz wariantu pokazuje, które affixy dałoby się zbudować bez scalania z innymi wariantami OCR.
                ItemImportEditableForm variantForm = new ItemImportEditableFormFactory().create(parsed);
                report.append("[affixes]\n");
                appendAffixes(report, variantForm.getAffixes());
                report.append("\n");
            }
        }

        // Sekcja merged sprawdza wynik deterministycznego candidate mergera na zebranym materiale z realnego OCR.
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

        // Raport trafia do `target/`, bo jest lokalnym artefaktem diagnostycznym, a nie fixture'em regresyjnym.
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
