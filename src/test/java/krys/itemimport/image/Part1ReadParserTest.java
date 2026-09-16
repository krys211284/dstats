package krys.itemimport.image;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Part1ReadParserTest {
    private final Part1ReadParser parser = new Part1ReadParser();

    @Test
    void shouldParseNeutralPart1ReadResultFromRawRoiOcr() {
        Part1ReadResult result = parser.parse(List.of(
                result(Part1RoiType.NAME_AND_STARS, "TARCZA BURZY\nKSIĘŻYCOWEGO\nSZAŁU\n", 2),
                result(Part1RoiType.CLASSIFICATION, "Starożytna legendarna\ntarcza\n"),
                result(Part1RoiType.ITEM_POWER, "x Moc przedmiotu: 900\n"),
                result(Part1RoiType.OPTIONAL_INFO, "29 (* +25) jakości\n© Rynsztunek w Zbrojowni\n% Przeistoczony\n")
        ));

        assertEquals("TARCZA BURZY KSIĘŻYCOWEGO SZAŁU", result.getItemName());
        assertEquals(2, result.getGreaterAffixCount());
        assertEquals("Starożytna legendarna tarcza", result.getClassificationRaw());
        assertEquals("Tarcza", result.getItemType());
        assertEquals("LEGENDARY", result.getItemRarity());
        assertOptionalBoolean(true, result.getAncient());
        assertOptionalBoolean(false, result.getMythicUnique());
        assertEquals(EquipmentSlot.OFF_HAND, result.getEquipmentSlot().orElseThrow());
        assertOptionalInt(900, result.getItemPower());
        assertOptionalInt(29, result.getDisplayedQuality());
        assertOptionalInt(25, result.getMasterworkingQuality());
        assertTrue(result.getParseFailures().isEmpty());
        assertEquals("x Moc przedmiotu: 900\n", result.findRawOcr(Part1RoiType.ITEM_POWER).orElseThrow().getRawText());
    }

    @Test
    void shouldParseQualityRegardlessOfSymbolInsideParentheses() {
        Part1ReadResult result = parser.parse(List.of(
                result(Part1RoiType.NAME_AND_STARS, "DZIEDZIC\nZATRACENIA", 1),
                result(Part1RoiType.CLASSIFICATION, "Starożytny mityczny unikatowy hełm"),
                result(Part1RoiType.ITEM_POWER, "Moc przedmiotu: 900"),
                result(Part1RoiType.OPTIONAL_INFO, "25 (% +25) jakości\n% Przeistoczony")
        ));

        assertOptionalInt(25, result.getDisplayedQuality());
        assertOptionalInt(25, result.getMasterworkingQuality());
        assertTrue(result.getParseFailures().isEmpty());
    }

    @Test
    void shouldKeepEmptyQualityWhenOptionalInfoHasNoQualityLine() {
        Part1ReadResult result = parser.parse(List.of(
                result(Part1RoiType.NAME_AND_STARS, "BDŁAMEK\nVERATHIEL", 0),
                result(Part1RoiType.CLASSIFICATION, "Starożytny unikatowy miecz"),
                result(Part1RoiType.ITEM_POWER, "Moc przedmiotu: 900"),
                result(Part1RoiType.OPTIONAL_INFO, "© Rynsztunek w Zbrojowni\n% Przeistoczony")
        ));

        assertTrue(result.getDisplayedQuality().isEmpty());
        assertTrue(result.getMasterworkingQuality().isEmpty());
        assertTrue(result.getParseFailures().isEmpty());
    }

    @Test
    void shouldReportQualityParseFailureWhenQualityLineIsPresentButUnreadable() {
        Part1ReadResult result = parser.parse(List.of(
                result(Part1RoiType.NAME_AND_STARS, "DZIEDZIC\nZATRACENIA", 1),
                result(Part1RoiType.CLASSIFICATION, "Starożytny mityczny unikatowy hełm"),
                result(Part1RoiType.ITEM_POWER, "Moc przedmiotu: 900"),
                result(Part1RoiType.OPTIONAL_INFO, "?? (% +??) jakości\n% Przeistoczony")
        ));

        assertTrue(result.getDisplayedQuality().isEmpty());
        assertTrue(result.getMasterworkingQuality().isEmpty());
        assertEquals(List.of("Nie udało się sparsować jakości z OPTIONAL_INFO."), result.getParseFailures());
    }

    @Test
    void shouldNotGuessItemPowerWhenNumberIsNotReliable() {
        Part1ReadResult result = parser.parse(List.of(
                result(Part1RoiType.NAME_AND_STARS, "Nazwa", 0),
                result(Part1RoiType.CLASSIFICATION, "Starożytne legendarne buty"),
                result(Part1RoiType.ITEM_POWER, "Moc przedmiotu: ???")
        ));

        assertTrue(result.getItemPower().isEmpty());
        assertEquals(List.of("Nie udało się sparsować mocy przedmiotu z ITEM_POWER."), result.getParseFailures());
    }

    @Test
    void shouldUseZeroStarsWhenNameRoiIsMissing() {
        Part1ReadResult result = parser.parse(List.of(result(Part1RoiType.CLASSIFICATION, "Starożytna tarcza")));

        assertEquals("", result.getItemName());
        assertEquals(0, result.getGreaterAffixCount());
        assertEquals("Starożytna tarcza", result.getClassificationRaw());
        assertTrue(result.getParseFailures().contains("Brak nazwy itemu w NAME_AND_STARS."));
    }

    @Test
    void shouldParseClassificationGenderVariants() {
        assertClassification("Starożytne legendarne buty", "Buty", "LEGENDARY", true, false, EquipmentSlot.BOOTS);
        assertClassification("Starożytny mityczny unikatowy hełm", "Hełm", "UNIQUE", true, true, EquipmentSlot.HELMET);
        assertClassification("Starożytny unikatowy miecz", "Miecz", "UNIQUE", true, false, EquipmentSlot.MAIN_HAND);
    }

    @Test
    void shouldReportUnknownClassificationFields() {
        Part1ReadResult result = parser.parse(List.of(
                result(Part1RoiType.NAME_AND_STARS, "Nazwa", 0),
                result(Part1RoiType.CLASSIFICATION, "Starożytny błyszczący przedmiot"),
                result(Part1RoiType.ITEM_POWER, "Moc przedmiotu: 800")
        ));

        assertEquals("", result.getItemType());
        assertEquals("", result.getItemRarity());
        assertTrue(result.getEquipmentSlot().isEmpty());
        assertTrue(result.getParseFailures().contains("Nie udało się sparsować rzadkości z CLASSIFICATION: Starożytny błyszczący przedmiot"));
        assertTrue(result.getParseFailures().contains("Nie udało się sparsować typu itemu z CLASSIFICATION: Starożytny błyszczący przedmiot"));
    }

    @Test
    void shouldReportAmbiguousClassificationFields() {
        Part1ReadResult result = parser.parse(List.of(
                result(Part1RoiType.NAME_AND_STARS, "Nazwa", 0),
                result(Part1RoiType.CLASSIFICATION, "Starożytna legendarna unikatowa tarcza miecz"),
                result(Part1RoiType.ITEM_POWER, "Moc przedmiotu: 800")
        ));

        assertEquals("", result.getItemType());
        assertEquals("", result.getItemRarity());
        assertTrue(result.getEquipmentSlot().isEmpty());
        assertTrue(result.getParseFailures().contains("Niejednoznaczna rzadkość w CLASSIFICATION: Starożytna legendarna unikatowa tarcza miecz"));
        assertTrue(result.getParseFailures().contains("Niejednoznaczny typ itemu w CLASSIFICATION: Starożytna legendarna unikatowa tarcza miecz"));
    }

    @Test
    void shouldReportRequiredFieldFailures() {
        Part1ReadResult result = parser.parse(List.of());

        assertEquals(List.of(
                "Brak nazwy itemu w NAME_AND_STARS.",
                "Brak klasyfikacji w CLASSIFICATION.",
                "Brak mocy przedmiotu w ITEM_POWER."
        ), result.getParseFailures());
    }

    private static Part1RoiOcrResult result(Part1RoiType type, String rawText) {
        return result(type, rawText, 0);
    }

    private static Part1RoiOcrResult result(Part1RoiType type, String rawText, int greaterAffixCount) {
        return new Part1RoiOcrResult(type, rawText, 1L, "test", List.of(), greaterAffixCount);
    }

    private static void assertOptionalInt(int expected, OptionalInt actual) {
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.getAsInt());
    }

    private void assertClassification(String classificationRaw,
                                      String itemType,
                                      String itemRarity,
                                      boolean ancient,
                                      boolean mythicUnique,
                                      EquipmentSlot equipmentSlot) {
        Part1ReadResult result = parser.parse(List.of(
                result(Part1RoiType.NAME_AND_STARS, "Nazwa", 0),
                result(Part1RoiType.CLASSIFICATION, classificationRaw),
                result(Part1RoiType.ITEM_POWER, "Moc przedmiotu: 800")
        ));

        assertEquals(itemType, result.getItemType());
        assertEquals(itemRarity, result.getItemRarity());
        assertOptionalBoolean(ancient, result.getAncient());
        assertOptionalBoolean(mythicUnique, result.getMythicUnique());
        assertEquals(equipmentSlot, result.getEquipmentSlot().orElseThrow());
        assertTrue(result.getParseFailures().isEmpty());
    }

    private static void assertOptionalBoolean(boolean expected, java.util.Optional<Boolean> actual) {
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }
}
