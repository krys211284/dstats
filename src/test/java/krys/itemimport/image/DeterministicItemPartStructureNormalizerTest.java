package krys.itemimport.image;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje neutralna normalizacje strukturalna Etapu 3A bez przetwarzania obrazu. */
class DeterministicItemPartStructureNormalizerTest {
    private final DeterministicItemPartStructureNormalizer normalizer = new DeterministicItemPartStructureNormalizer();

    @Test
    void shouldMapFourRawPartsToFourStructuralSlots() {
        List<ItemImagePart> rawParts = parts(4);

        StructuredItemParts result = normalizer.normalize(rawParts);

        assertEquals(ItemStructureStatus.STRUCTURALLY_NORMALIZED, result.getStatus());
        assertMapping(result, StructuralItemPartSlot.PART_1, rawParts.get(0));
        assertMapping(result, StructuralItemPartSlot.PART_2, rawParts.get(1));
        assertMapping(result, StructuralItemPartSlot.PART_3, rawParts.get(2));
        assertMapping(result, StructuralItemPartSlot.PART_4, rawParts.get(3));
    }

    @Test
    void shouldMapThreeRawPartsToPartOneThreeAndFourWithAbsentPartTwo() {
        List<ItemImagePart> rawParts = parts(3);

        StructuredItemParts result = normalizer.normalize(rawParts);

        assertEquals(ItemStructureStatus.STRUCTURALLY_NORMALIZED, result.getStatus());
        assertMapping(result, StructuralItemPartSlot.PART_1, rawParts.get(0));
        assertFalse(result.find(StructuralItemPartSlot.PART_2).isPresent());
        assertMapping(result, StructuralItemPartSlot.PART_3, rawParts.get(1));
        assertMapping(result, StructuralItemPartSlot.PART_4, rawParts.get(2));
        assertEquals(3, result.getStructuralParts().size());
    }

    @Test
    void shouldNotTreatRawPartNamesAsStructuralSlots() {
        List<ItemImagePart> rawParts = List.of(
                part(1, "PART_1"),
                part(2, "PART_2"),
                part(3, "PART_3")
        );

        StructuredItemParts result = normalizer.normalize(rawParts);

        assertSame(rawParts.get(1), result.find(StructuralItemPartSlot.PART_3).orElseThrow().getSourcePart());
        assertEquals("PART_2", result.find(StructuralItemPartSlot.PART_3).orElseThrow().getSourcePart().getName());
        assertFalse(result.find(StructuralItemPartSlot.PART_2).isPresent());
    }

    @Test
    void shouldReturnIncompleteWithoutGuessedMappingsForOneOrTwoRawParts() {
        assertIncompleteWithoutMappings(parts(1));
        assertIncompleteWithoutMappings(parts(2));
    }

    @Test
    void shouldReturnInvalidWithoutGuessedMappingsForUnexpectedPartCount() {
        List<ItemImagePart> rawParts = parts(5);

        StructuredItemParts result = normalizer.normalize(rawParts);

        assertEquals(ItemStructureStatus.INVALID_STRUCTURE, result.getStatus());
        assertTrue(result.getStructuralParts().isEmpty());
        assertEquals(rawParts, result.getRawParts());
    }

    @Test
    void shouldKeepStableTopToBottomOrderOfStructuralParts() {
        List<ItemImagePart> threeParts = parts(3);
        List<ItemImagePart> fourParts = parts(4);

        assertEquals(List.of(StructuralItemPartSlot.PART_1, StructuralItemPartSlot.PART_3, StructuralItemPartSlot.PART_4),
                normalizer.normalize(threeParts).getStructuralParts().stream().map(StructuralItemPart::getSlot).toList());
        assertEquals(List.of(StructuralItemPartSlot.PART_1, StructuralItemPartSlot.PART_2, StructuralItemPartSlot.PART_3, StructuralItemPartSlot.PART_4),
                normalizer.normalize(fourParts).getStructuralParts().stream().map(StructuralItemPart::getSlot).toList());
    }

    @Test
    void shouldPreserveTheExactSourcePartObjectAndMetadata() {
        ItemImagePart rawPart = new ItemImagePart(
                2,
                "RAW_SECOND",
                10,
                15,
                4,
                5,
                "PNG",
                new byte[]{9, 8, 7, 6}
        );
        List<ItemImagePart> rawParts = List.of(part(1, "RAW_FIRST"), rawPart, part(3, "RAW_THIRD"));

        StructuredItemParts result = normalizer.normalize(rawParts);
        ItemImagePart sourcePart = result.find(StructuralItemPartSlot.PART_3).orElseThrow().getSourcePart();

        assertSame(rawPart, sourcePart);
        assertEquals(2, sourcePart.getIndex());
        assertEquals("RAW_SECOND", sourcePart.getName());
        assertEquals(10, sourcePart.getStartYInclusive());
        assertEquals(15, sourcePart.getEndYExclusive());
        assertEquals(4, sourcePart.getWidth());
        assertEquals(5, sourcePart.getHeight());
        assertEquals("PNG", sourcePart.getFormat());
        assertArrayEquals(new byte[]{9, 8, 7, 6}, sourcePart.getImageBytes());
    }

    @Test
    void shouldNotModifyInputListAndShouldExposeImmutableResultLists() {
        ArrayList<ItemImagePart> rawParts = new ArrayList<>(parts(3));
        List<ItemImagePart> originalOrder = List.copyOf(rawParts);

        StructuredItemParts result = normalizer.normalize(rawParts);

        assertEquals(originalOrder, rawParts);
        assertEquals(originalOrder, result.getRawParts());
        assertThrows(UnsupportedOperationException.class, () -> result.getRawParts().add(part(9, "PART_9")));
        assertThrows(UnsupportedOperationException.class, () -> result.getStructuralParts().clear());
    }

    @Test
    void shouldHandleEmptyOrNullInputDefensivelyAsIncomplete() {
        assertIncompleteWithoutMappings(List.of());

        StructuredItemParts result = normalizer.normalize(null);

        assertEquals(ItemStructureStatus.INCOMPLETE, result.getStatus());
        assertTrue(result.getRawParts().isEmpty());
        assertTrue(result.getStructuralParts().isEmpty());
    }

    private void assertIncompleteWithoutMappings(List<ItemImagePart> rawParts) {
        StructuredItemParts result = normalizer.normalize(rawParts);

        assertEquals(ItemStructureStatus.INCOMPLETE, result.getStatus());
        assertTrue(result.getStructuralParts().isEmpty());
        assertEquals(rawParts, result.getRawParts());
    }

    private static void assertMapping(StructuredItemParts result, StructuralItemPartSlot slot, ItemImagePart sourcePart) {
        StructuralItemPart structuralPart = result.find(slot).orElseThrow();

        assertEquals(slot, structuralPart.getSlot());
        assertSame(sourcePart, structuralPart.getSourcePart());
    }

    private static List<ItemImagePart> parts(int count) {
        List<ItemImagePart> parts = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            parts.add(part(index, "PART_" + index));
        }
        return parts;
    }

    private static ItemImagePart part(int index, String name) {
        return new ItemImagePart(
                index,
                name,
                (index - 1) * 10,
                index * 10,
                3,
                10,
                "PNG",
                new byte[]{(byte) index, (byte) (index + 1)}
        );
    }
}
