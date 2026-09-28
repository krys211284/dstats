package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.opencv.OpenCvHorizontalDividerDetector;
import krys.itemimport.image.opencv.OpenCvItemImageAssembler;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyBlockNumericEvidenceAuditTest {
    private final OpenCvItemImageAssembler assembler = new OpenCvItemImageAssembler();
    private final OpenCvHorizontalDividerDetector dividerDetector = new OpenCvHorizontalDividerDetector();
    private final ItemPartStructureNormalizer normalizer = new DeterministicItemPartStructureNormalizer();
    private final PropertyBlockSegmenter segmenter = new VisualPropertyBlockSegmenter();

    @Test
    void shouldKeepExpectedAmbiguityCoverageAcrossAllTwentySevenBlocks() throws Exception {
        List<LabeledBlock> blocks = new ArrayList<>();
        add(blocks, "Tarcza PART_2", List.of("tarcza1.png", "tarcza2.png"), StructuralItemPartSlot.PART_2);
        add(blocks, "Tarcza PART_3", List.of("tarcza1.png", "tarcza2.png"), StructuralItemPartSlot.PART_3);
        add(blocks, "Dziedzic PART_3", List.of("dziedzic-zatracenia-helm-full.png"), StructuralItemPartSlot.PART_3);
        add(blocks, "Verathiel PART_3", List.of("verathiel-miecz-v2.png"), StructuralItemPartSlot.PART_3);

        TesseractPropertyBlockOcrReader reader = new TesseractPropertyBlockOcrReader();
        List<String> ambiguous = new ArrayList<>();
        for (LabeledBlock labeled : blocks) {
            PropertyBlockReadResult result = reader.read(labeled.block());
            if (result.hasAmbiguousNumericEvidence()) {
                ambiguous.add(labeled.label());
            }
        }
        assertEquals(27, blocks.size());
        assertEquals(21, blocks.size() - ambiguous.size());
        assertEquals(List.of(
                "Tarcza PART_3 #2",
                "Tarcza PART_3 #3",
                "Dziedzic PART_3 #2",
                "Dziedzic PART_3 #3",
                "Dziedzic PART_3 #6",
                "Verathiel PART_3 #4"
        ), ambiguous);
    }

    private void add(List<LabeledBlock> destination,
                     String label,
                     List<String> filenames,
                     StructuralItemPartSlot slot) throws Exception {
        AssembledItemImage image = assembler.assemble(filenames.stream().map(PropertyBlockNumericEvidenceAuditTest::request).toList());
        StructuralItemPart part = normalizer.normalize(dividerDetector.detect(image).getParts()).find(slot).orElseThrow();
        for (PropertyBlock block : segmenter.segment(part).getBlocks()) {
            destination.add(new LabeledBlock(label + " #" + block.getOrder(), block));
        }
    }

    private static ItemImageImportRequest request(String filename) {
        try {
            return new ItemImageImportRequest(filename, "image/png",
                    Files.readAllBytes(Path.of("src/test/resources/items", filename)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record LabeledBlock(String label, PropertyBlock block) {
    }
}
