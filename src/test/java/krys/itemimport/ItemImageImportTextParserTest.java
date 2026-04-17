package krys.itemimport;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Testuje polskie frazy OCR dla foundation importu itemu. */
class ItemImageImportTextParserTest {
    private final ItemImageImportTextParser parser = new ItemImageImportTextParser();
    private final ItemImageMetadata metadata = new ItemImageMetadata("shield.png", "image/png", "PNG", 1200, 1600);

    @Test
    void shouldRecognizePolishShieldSlotAndFoundationAffixes() {
        String ocrText = """
                Tarcza
                +114 do siły
                +494 do cierni
                +20,0% szansy na blok
                """;

        ItemImageImportCandidateParseResult result = parser.parse(metadata, ocrText);

        assertEquals(EquipmentSlot.OFF_HAND, result.getSlotCandidate().getSuggestedValue());
        assertEquals(ItemImportFieldConfidence.HIGH, result.getSlotCandidate().getConfidence());
        assertEquals(114.0d, result.getStrengthCandidate().getSuggestedValue());
        assertEquals(494.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertNull(result.getIntelligenceCandidate().getSuggestedValue());
        assertNull(result.getRetributionChanceCandidate().getSuggestedValue());
    }

    @Test
    void shouldPreferMainRollOutsideReferenceRangeForPolishFoundationAffixes() {
        String ocrText = """
                Tarcza
                +114 do siły [107 - 121]
                +494 do cierni [473 - 506]
                +20,0% szansy na blok [18,0 - 22,5]
                """;

        ItemImageImportCandidateParseResult result = parser.parse(metadata, ocrText);

        assertEquals(EquipmentSlot.OFF_HAND, result.getSlotCandidate().getSuggestedValue());
        assertEquals(114.0d, result.getStrengthCandidate().getSuggestedValue());
        assertEquals(494.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, result.getBlockChanceCandidate().getSuggestedValue());
    }

    @Test
    void shouldRecognizeBootSlotWithoutHallucinatingUnsupportedAffixes() {
        String ocrText = """
                Buty
                +12,5% szybkości ruchu
                +7,0% uniku
                """;

        ItemImageImportCandidateParseResult result = parser.parse(metadata, ocrText);

        assertEquals(EquipmentSlot.BOOTS, result.getSlotCandidate().getSuggestedValue());
        assertNull(result.getWeaponDamageCandidate().getSuggestedValue());
        assertNull(result.getStrengthCandidate().getSuggestedValue());
        assertNull(result.getIntelligenceCandidate().getSuggestedValue());
        assertNull(result.getThornsCandidate().getSuggestedValue());
        assertNull(result.getBlockChanceCandidate().getSuggestedValue());
        assertNull(result.getRetributionChanceCandidate().getSuggestedValue());
    }
}
