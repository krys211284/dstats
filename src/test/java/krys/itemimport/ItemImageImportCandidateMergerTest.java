package krys.itemimport;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje deterministyczne scalanie wyników z wielu wariantów OCR. */
class ItemImageImportCandidateMergerTest {
    private final ItemImageMetadata metadata = new ItemImageMetadata("shield.png", "image/png", "PNG", 1200, 1600);

    @Test
    void shouldPreferHigherConfidenceValueFromAnotherVariant() {
        ItemImageImportCandidateParseResult lowQuality = parseResult(
                ItemImportFieldCandidate.unknown("slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                new ItemImportFieldCandidate<>("+111 do siły", 111.0d, ItemImportFieldConfidence.MEDIUM, "medium"),
                ItemImportFieldCandidate.unknown("intelligence"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution")
        );
        ItemImageImportCandidateParseResult betterVariant = parseResult(
                new ItemImportFieldCandidate<>("Tarcza", EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.HIGH, "high"),
                ItemImportFieldCandidate.unknown("weapon"),
                new ItemImportFieldCandidate<>("+114 do siły", 114.0d, ItemImportFieldConfidence.HIGH, "high"),
                ItemImportFieldCandidate.unknown("intelligence"),
                new ItemImportFieldCandidate<>("+494 do cierni", 494.0d, ItemImportFieldConfidence.HIGH, "high"),
                new ItemImportFieldCandidate<>("+20,0% szansy na blok", 20.0d, ItemImportFieldConfidence.HIGH, "high"),
                ItemImportFieldCandidate.unknown("retribution")
        );

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger()
                .merge(metadata, 5, java.util.List.of(lowQuality, betterVariant));

        assertEquals(EquipmentSlot.OFF_HAND, merged.getSlotCandidate().getSuggestedValue());
        assertEquals(114.0d, merged.getStrengthCandidate().getSuggestedValue());
        assertEquals(ItemImportFieldConfidence.HIGH, merged.getStrengthCandidate().getConfidence());
        assertEquals(494.0d, merged.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, merged.getBlockChanceCandidate().getSuggestedValue());
    }

    @Test
    void shouldLowerConfidenceWhenTopVariantsConflict() {
        ItemImageImportCandidateParseResult firstVariant = parseResult(
                ItemImportFieldCandidate.unknown("slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                new ItemImportFieldCandidate<>("+114 do siły", 114.0d, ItemImportFieldConfidence.HIGH, "high"),
                ItemImportFieldCandidate.unknown("intelligence"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution")
        );
        ItemImageImportCandidateParseResult secondVariant = parseResult(
                ItemImportFieldCandidate.unknown("slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                new ItemImportFieldCandidate<>("+111 do siły", 111.0d, ItemImportFieldConfidence.HIGH, "high"),
                ItemImportFieldCandidate.unknown("intelligence"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution")
        );

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger()
                .merge(metadata, 5, java.util.List.of(firstVariant, secondVariant));

        assertEquals(114.0d, merged.getStrengthCandidate().getSuggestedValue());
        assertEquals(ItemImportFieldConfidence.MEDIUM, merged.getStrengthCandidate().getConfidence());
        assertTrue(merged.getStrengthCandidate().getNote().contains("sprzeczne wartości"));
    }

    private ItemImageImportCandidateParseResult parseResult(ItemImportFieldCandidate<EquipmentSlot> slotCandidate,
                                                            ItemImportFieldCandidate<Long> weaponDamageCandidate,
                                                            ItemImportFieldCandidate<Double> strengthCandidate,
                                                            ItemImportFieldCandidate<Double> intelligenceCandidate,
                                                            ItemImportFieldCandidate<Double> thornsCandidate,
                                                            ItemImportFieldCandidate<Double> blockChanceCandidate,
                                                            ItemImportFieldCandidate<Double> retributionChanceCandidate) {
        return new ItemImageImportCandidateParseResult(
                metadata,
                slotCandidate,
                weaponDamageCandidate,
                strengthCandidate,
                intelligenceCandidate,
                thornsCandidate,
                blockChanceCandidate,
                retributionChanceCandidate,
                "test"
        );
    }
}
