package krys.itemimport.image;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyBlockNumericEvidenceAuditTest {

    @Test
    void shouldKeepExpectedAmbiguityCoverageAcrossAllTwentySevenBlocks() throws Exception {
        List<LabeledBlock> blocks = new ArrayList<>();
        add(blocks, "Tarcza PART_2", List.of("tarcza1.png", "tarcza2.png"), StructuralItemPartSlot.PART_2);
        add(blocks, "Tarcza PART_3", List.of("tarcza1.png", "tarcza2.png"), StructuralItemPartSlot.PART_3);
        add(blocks, "Dziedzic PART_3", List.of("dziedzic-zatracenia-helm-full.png"), StructuralItemPartSlot.PART_3);
        add(blocks, "Verathiel PART_3", List.of("verathiel-miecz-v2.png"), StructuralItemPartSlot.PART_3);

        List<String> ambiguous = new ArrayList<>();
        for (LabeledBlock labeled : blocks) {
            PropertyBlockReadResult result = labeled.read();
            var aggregation = PropertyBlockNumericEvidenceAggregator.aggregate(result.getProfileResults());
            OcrGoldenFixtures.assertSameRead(result.getNumericEvidence(), aggregation.evidence());
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
        for (OcrGoldenFixtures.BlockRead fixture : OcrGoldenFixtures.blocks(filenames, slot)) {
            destination.add(new LabeledBlock(label + " #" + fixture.block().getOrder(), fixture.read()));
        }
    }

    private record LabeledBlock(String label, PropertyBlockReadResult read) {
    }
}
