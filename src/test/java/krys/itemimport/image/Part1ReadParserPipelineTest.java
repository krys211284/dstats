package krys.itemimport.image;

import krys.item.EquipmentSlot;
import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.opencv.OpenCvHorizontalDividerDetector;
import krys.itemimport.image.opencv.OpenCvItemImageAssembler;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regresje neutralnego odczytu PART_1 na realnych fixture'ach. */
class Part1ReadParserPipelineTest {
    private final OpenCvItemImageAssembler assembler = new OpenCvItemImageAssembler();
    private final OpenCvHorizontalDividerDetector dividerDetector = new OpenCvHorizontalDividerDetector();
    private final ItemPartStructureNormalizer normalizer = new DeterministicItemPartStructureNormalizer();
    private final Part1RoiSegmenter segmenter = new DeterministicPart1RoiSegmenter();
    private final Part1RoiOcrReader reader = new TesseractPart1RoiOcrReader();
    private final Part1ReadParser parser = new Part1ReadParser();

    @Test
    void shouldParsePart1ReadResultForRealFixtures() throws Exception {
        assertRead(
                expected("buty.png", "KOSCIANE BACZIMIAGI METAMORFOZY", 2,
                        "Starożytne legendarne buty", "Buty", "LEGENDARY", true, false,
                        EquipmentSlot.BOOTS, 800, null, null),
                single("buty.png")
        );
        assertRead(
                expected("dziedzic-zatracenia-helm-full.png", "F DZIEDZIC ZATRACENIA", 1,
                        "Starożytny mityczny unikatowy hełm", "Hełm", "UNIQUE", true, true,
                        EquipmentSlot.HELMET, 900, 25, 25),
                single("dziedzic-zatracenia-helm-full.png")
        );
        assertRead(
                expected("helmet pair", "DZIEDZIC ZATRACENIA", 1,
                        "Starożytny mityczny unikatowy hełm", "Hełm", "UNIQUE", true, true,
                        EquipmentSlot.HELMET, 900, 25, 25),
                pair("dziedzic-zatracenia-helm-1.png", "dziedzic-zatracenia-helm-2.png")
        );
        assertRead(
                expected("tarcza.png", "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU", 1,
                        "Starożytna legendarna tarcza", "Tarcza", "LEGENDARY", true, false,
                        EquipmentSlot.OFF_HAND, 800, null, null),
                single("tarcza.png")
        );
        assertRead(
                expected("tarcza1.png", "TARCZA BURZY KSIĘŻYCOWEGO SZAŁU", 2,
                        "Starożytna legendarna tarcza", "Tarcza", "LEGENDARY", true, false,
                        EquipmentSlot.OFF_HAND, 900, 29, 25),
                single("tarcza1.png")
        );
        assertRead(
                expected("tarcza1.png + tarcza2.png", "TARCZA BURZY KSIĘŻYCOWEGO SZAŁU", 2,
                        "Starożytna legendarna tarcza", "Tarcza", "LEGENDARY", true, false,
                        EquipmentSlot.OFF_HAND, 900, 29, 25),
                pair("tarcza1.png", "tarcza2.png")
        );
        assertRead(
                expected("verathiel-miecz-v2.png", "BDŁAMEK VERATHIEL", 0,
                        "Starożytny unikatowy miecz", "Miecz", "UNIQUE", true, false,
                        EquipmentSlot.MAIN_HAND, 900, null, null),
                single("verathiel-miecz-v2.png")
        );
    }

    private void assertRead(Expected expected, AssembledItemImage image) {
        Part1ReadResult result = read(image);

        assertEquals(expected.itemName(), result.getItemName(), expected.label() + " itemName");
        assertEquals(expected.greaterAffixCount(), result.getGreaterAffixCount(), expected.label() + " greaterAffixCount");
        assertEquals(expected.classificationRaw(), result.getClassificationRaw(), expected.label() + " classificationRaw");
        assertEquals(expected.itemType(), result.getItemType(), expected.label() + " itemType");
        assertEquals(expected.itemRarity(), result.getItemRarity(), expected.label() + " itemRarity");
        assertTrue(result.getAncient().isPresent(), expected.label() + " ancient present");
        assertEquals(expected.ancient(), result.getAncient().orElseThrow(), expected.label() + " ancient");
        assertTrue(result.getMythicUnique().isPresent(), expected.label() + " mythicUnique present");
        assertEquals(expected.mythicUnique(), result.getMythicUnique().orElseThrow(), expected.label() + " mythicUnique");
        assertEquals(expected.equipmentSlot(), result.getEquipmentSlot().orElseThrow(), expected.label() + " equipmentSlot");
        assertOptionalInt(expected.itemPower(), result.getItemPower(), expected.label() + " itemPower");
        assertOptionalInt(expected.displayedQuality(), result.getDisplayedQuality(), expected.label() + " displayedQuality");
        assertOptionalInt(expected.masterworkingQuality(), result.getMasterworkingQuality(), expected.label() + " masterworkingQuality");
        assertTrue(result.getParseFailures().isEmpty(), expected.label() + " parse failures: " + result.getParseFailures());
        assertTrue(result.findRawOcr(Part1RoiType.NAME_AND_STARS).isPresent(), expected.label() + " raw NAME");
        assertTrue(result.findRawOcr(Part1RoiType.CLASSIFICATION).isPresent(), expected.label() + " raw CLASSIFICATION");
        assertTrue(result.findRawOcr(Part1RoiType.ITEM_POWER).isPresent(), expected.label() + " raw ITEM_POWER");
    }

    private Part1ReadResult read(AssembledItemImage image) {
        StructuralItemPart part1 = normalizer.normalize(dividerDetector.detect(image).getParts())
                .find(StructuralItemPartSlot.PART_1)
                .orElseThrow();
        List<Part1RoiOcrResult> ocrResults = segmenter.segment(part1).getRois().stream()
                .map(reader::read)
                .toList();
        return parser.parse(ocrResults);
    }

    private AssembledItemImage single(String filename) throws Exception {
        return assembler.assemble(List.of(request(filename)));
    }

    private AssembledItemImage pair(String first, String second) throws Exception {
        return assembler.assemble(List.of(request(first), request(second)));
    }

    private static ItemImageImportRequest request(String filename) throws Exception {
        return new ItemImageImportRequest(filename, "image/png", Files.readAllBytes(Path.of("src/test/resources/items", filename)));
    }

    private static Expected expected(String label,
                                     String itemName,
                                     int greaterAffixCount,
                                     String classificationRaw,
                                     String itemType,
                                     String itemRarity,
                                     boolean ancient,
                                     boolean mythicUnique,
                                     EquipmentSlot equipmentSlot,
                                     int itemPower,
                                     Integer displayedQuality,
                                     Integer masterworkingQuality) {
        return new Expected(label, itemName, greaterAffixCount, classificationRaw,
                itemType, itemRarity, ancient, mythicUnique, equipmentSlot,
                itemPower, displayedQuality, masterworkingQuality);
    }

    private static void assertOptionalInt(Integer expected, OptionalInt actual, String label) {
        if (expected == null) {
            assertTrue(actual.isEmpty(), label);
            return;
        }
        assertTrue(actual.isPresent(), label);
        assertEquals(expected.intValue(), actual.getAsInt(), label);
    }

    private record Expected(String label,
                            String itemName,
                            int greaterAffixCount,
                            String classificationRaw,
                            String itemType,
                            String itemRarity,
                            boolean ancient,
                            boolean mythicUnique,
                            EquipmentSlot equipmentSlot,
                            int itemPower,
                            Integer displayedQuality,
                            Integer masterworkingQuality) {
    }
}
