package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.opencv.OpenCvHorizontalDividerDetector;
import krys.itemimport.image.opencv.OpenCvItemImageAssembler;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Macierz semantyczna realnych PropertyBlock po zamkniętej segmentacji i OCR. */
class PropertyBlockSemanticMapperFixtureTest {
    private final OpenCvItemImageAssembler assembler = new OpenCvItemImageAssembler();
    private final OpenCvHorizontalDividerDetector dividerDetector = new OpenCvHorizontalDividerDetector();
    private final ItemPartStructureNormalizer normalizer = new DeterministicItemPartStructureNormalizer();
    private final PropertyBlockSegmenter segmenter = new VisualPropertyBlockSegmenter();
    private final PropertyBlockOcrReader ocrReader = new TesseractPropertyBlockOcrReader();
    private final PropertyBlockSemanticMapper semanticMapper = new PropertyBlockSemanticMapper();

    @Test
    void shouldMapTarczaMatrix() throws Exception {
        List<PropertyBlockSemanticResult> part2 = map(
                List.of("tarcza1.png", "tarcza2.png"), StructuralItemPartSlot.PART_2);
        List<PropertyBlockSemanticResult> part3 = map(
                List.of("tarcza1.png", "tarcza2.png"), StructuralItemPartSlot.PART_3);

        assertKinds(part2, BASE_STAT, IMPLICIT, IMPLICIT);
        assertKinds(part3, AFFIX, AFFIX, AFFIX, AFFIX, TRANSFIGURATION, TEMPERING, ASPECT, SOCKET);
        assertDefinitions(part3,
                "strength",
                "critical_strike_chance",
                "damage_reduction",
                "cooldown_reduction",
                "BONUS_ITEM_QUALITY",
                "defense_max_animus",
                "naznaczenie_aspect",
                "EMPTY_SOCKET");
        assertFalse(part3.get(1).isNumericValueCertain(), describeEvidence(part3.get(1)));
        assertFalse(part3.get(2).isNumericValueCertain(), describeEvidence(part3.get(2)));
        assertTrue(part3.get(3).isGreaterAffix());
        assertTrue(part3.get(5).isGreaterAffix());
    }

    @Test
    void shouldMapDziedzicFullMatrix() throws Exception {
        List<PropertyBlockSemanticResult> blocks = map(
                List.of("dziedzic-zatracenia-helm-full.png"), StructuralItemPartSlot.PART_3);

        assertKinds(blocks,
                BASE_STAT, AFFIX, AFFIX, AFFIX, AFFIX,
                TRANSFIGURATION, TEMPERING, ASPECT, SOCKET, SOCKET);
        assertDefinitions(blocks,
                "",
                "critical_strike_chance",
                "lucky_hit_chance",
                "movement_speed",
                "core_skill_ranks",
                "ALL_STATS",
                "defense_max_animus",
                "heir_of_perdition",
                "FILLED_SOCKET",
                "FILLED_SOCKET");
        assertEquals("ALL_STATS", blocks.get(5).getDefinitionId());
        assertFalse(blocks.get(5).isNumericValueCertain());
        assertTrue(blocks.get(5).getDiagnostics().stream()
                .anyMatch(value -> value.contains("NUMERIC_OCR_AMBIGUOUS")));
        assertEquals(PropertyBlockSemanticKind.SOCKET, blocks.get(8).getKind());
        assertFalse(blocks.get(8).getDefinitionId().equals("strength"));
    }

    @Test
    void shouldMapVerathielMatrix() throws Exception {
        List<PropertyBlockSemanticResult> blocks = map(
                List.of("verathiel-miecz-v2.png"), StructuralItemPartSlot.PART_3);

        assertKinds(blocks, BASE_STAT, IMPLICIT, AFFIX, AFFIX, AFFIX, ASPECT);
        assertDefinitions(blocks,
                "",
                "",
                "strength",
                "life_on_kill",
                "damage_over_time_multiplier",
                "verathiel_shard");
        assertFalse(blocks.get(3).isNumericValueCertain());
        assertEquals("life_on_kill", blocks.get(3).getDefinitionId());
    }

    private List<PropertyBlockSemanticResult> map(List<String> filenames,
                                                  StructuralItemPartSlot slot) throws Exception {
        AssembledItemImage image = assembler.assemble(filenames.stream().map(PropertyBlockSemanticMapperFixtureTest::request).toList());
        StructuralItemPart part = normalizer.normalize(dividerDetector.detect(image).getParts()).find(slot).orElseThrow();
        return segmenter.segment(part).getBlocks().stream()
                .map(block -> semanticMapper.map(block, ocrReader.read(block)))
                .toList();
    }

    private static void assertKinds(List<PropertyBlockSemanticResult> results,
                                    PropertyBlockSemanticKind... expected) {
        assertEquals(List.of(expected), results.stream().map(PropertyBlockSemanticResult::getKind).toList());
    }

    private static void assertDefinitions(List<PropertyBlockSemanticResult> results, String... expected) {
        assertEquals(List.of(expected), results.stream().map(PropertyBlockSemanticResult::getDefinitionId).toList());
    }

    private static String describeEvidence(PropertyBlockSemanticResult result) {
        return result.getNumericEvidence().stream()
                .map(evidence -> evidence.getTokenOrder() + "=" + evidence.getSelectedValue()
                        + " support=" + evidence.getSupportCount()
                        + " alternatives=" + evidence.getAlternativeValues()
                        + " ambiguous=" + evidence.isAmbiguous())
                .toList()
                .toString();
    }

    private static ItemImageImportRequest request(String filename) {
        try {
            return new ItemImageImportRequest(filename, "image/png",
                    Files.readAllBytes(Path.of("src/test/resources/items", filename)));
        } catch (Exception exception) {
            throw new IllegalStateException("Nie udało się wczytać fixture " + filename + ".", exception);
        }
    }

    private static final PropertyBlockSemanticKind BASE_STAT = PropertyBlockSemanticKind.BASE_STAT;
    private static final PropertyBlockSemanticKind IMPLICIT = PropertyBlockSemanticKind.IMPLICIT;
    private static final PropertyBlockSemanticKind AFFIX = PropertyBlockSemanticKind.AFFIX;
    private static final PropertyBlockSemanticKind TEMPERING = PropertyBlockSemanticKind.TEMPERING;
    private static final PropertyBlockSemanticKind TRANSFIGURATION = PropertyBlockSemanticKind.TRANSFIGURATION;
    private static final PropertyBlockSemanticKind ASPECT = PropertyBlockSemanticKind.ASPECT;
    private static final PropertyBlockSemanticKind SOCKET = PropertyBlockSemanticKind.SOCKET;
}
