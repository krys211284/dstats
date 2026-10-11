package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.opencv.OpenCvHorizontalDividerDetector;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Regresje realnego pipeline'u Etap 1 -> Etap 2A -> Etap 3A. */
class ItemPartStructureNormalizerPipelineTest {
    private final ItemImageAssembler assembler = ImageAssemblyFixtures::assemble;
    private final OpenCvHorizontalDividerDetector dividerDetector = new OpenCvHorizontalDividerDetector();
    private final ItemPartStructureNormalizer normalizer = new DeterministicItemPartStructureNormalizer();

    @Test
    void shouldNormalizeVerathielV2AsThreePartStructure() throws Exception {
        assertThreePartMapping(detect(singleFixture("verathiel-miecz-v2.png")));
    }

    @Test
    void shouldKeepVerathielV1IncompleteWithoutGuessedMappings() throws Exception {
        HorizontalDividerDetectionResult result = detect(singleFixture("verathiel-miecz.png"));

        StructuredItemParts structured = normalizer.normalize(result.getParts());

        assertEquals(2, result.getParts().size());
        assertEquals(ItemStructureStatus.INCOMPLETE, structured.getStatus());
        assertEquals(0, structured.getStructuralParts().size());
    }

    @Test
    void shouldNormalizeHelmetPairAsThreePartStructure() throws Exception {
        AssembledItemImage image = assembler.assemble(List.of(
                request("dziedzic-zatracenia-helm-1.png", Files.readAllBytes(itemPath("dziedzic-zatracenia-helm-1.png"))),
                request("dziedzic-zatracenia-helm-2.png", Files.readAllBytes(itemPath("dziedzic-zatracenia-helm-2.png")))
        ));

        assertThreePartMapping(detect(image));
    }

    @Test
    void shouldNormalizeTarczaPairAsFourPartStructure() throws Exception {
        AssembledItemImage image = assembler.assemble(List.of(
                request("tarcza1.png", Files.readAllBytes(itemPath("tarcza1.png"))),
                request("tarcza2.png", Files.readAllBytes(itemPath("tarcza2.png")))
        ));

        HorizontalDividerDetectionResult result = detect(image);
        StructuredItemParts structured = normalizer.normalize(result.getParts());

        assertEquals(4, result.getParts().size());
        assertEquals(ItemStructureStatus.STRUCTURALLY_NORMALIZED, structured.getStatus());
        assertSame(result.getParts().get(0), structured.find(StructuralItemPartSlot.PART_1).orElseThrow().getSourcePart());
        assertSame(result.getParts().get(1), structured.find(StructuralItemPartSlot.PART_2).orElseThrow().getSourcePart());
        assertSame(result.getParts().get(2), structured.find(StructuralItemPartSlot.PART_3).orElseThrow().getSourcePart());
        assertSame(result.getParts().get(3), structured.find(StructuralItemPartSlot.PART_4).orElseThrow().getSourcePart());
    }

    @Test
    void shouldNormalizeKnownThreePartFixturesWithoutTreatingStatusAsCompletenessProof() throws Exception {
        assertThreePartMapping(detect(singleFixture("buty.png")));
        assertThreePartMapping(detect(singleFixture("tarcza.png")));
    }

    @Test
    void shouldNormalizeSyntheticSplitTarczaAsThreePartStructure() throws Exception {
        BufferedImage shield = ImageIO.read(itemPath("tarcza.png").toFile());
        BufferedImage first = crop(shield, 0, 0, shield.getWidth(), 1000);
        BufferedImage second = crop(shield, 0, 700, shield.getWidth(), shield.getHeight() - 700);
        AssembledItemImage image = assembler.assemble(List.of(
                request("tarcza-1.png", png(first)),
                request("tarcza-2.png", png(second))
        ));

        assertThreePartMapping(detect(image));
    }

    private void assertThreePartMapping(HorizontalDividerDetectionResult result) {
        StructuredItemParts structured = normalizer.normalize(result.getParts());

        assertEquals(3, result.getParts().size());
        assertEquals(ItemStructureStatus.STRUCTURALLY_NORMALIZED, structured.getStatus());
        assertSame(result.getParts().get(0), structured.find(StructuralItemPartSlot.PART_1).orElseThrow().getSourcePart());
        assertFalse(structured.find(StructuralItemPartSlot.PART_2).isPresent());
        assertSame(result.getParts().get(1), structured.find(StructuralItemPartSlot.PART_3).orElseThrow().getSourcePart());
        assertSame(result.getParts().get(2), structured.find(StructuralItemPartSlot.PART_4).orElseThrow().getSourcePart());
    }

    private HorizontalDividerDetectionResult detect(AssembledItemImage image) {
        return dividerDetector.detect(image);
    }

    private AssembledItemImage singleFixture(String filename) throws Exception {
        return assembler.assemble(List.of(request(filename, Files.readAllBytes(itemPath(filename)))));
    }

    private static Path itemPath(String filename) {
        return Path.of("src/test/resources/items", filename);
    }

    private static ItemImageImportRequest request(String filename, byte[] bytes) {
        return new ItemImageImportRequest(filename, "image/png", bytes);
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
}
