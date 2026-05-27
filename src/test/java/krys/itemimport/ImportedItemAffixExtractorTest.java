package krys.itemimport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje rozpoznawanie strukturalnych affixów z pełnego odczytu OCR itemu. */
class ImportedItemAffixExtractorTest {
    private final ImportedItemAffixExtractor extractor = new ImportedItemAffixExtractor();

    @Test
    void shouldRecognizeGreaterAffixFromOcrMarkers() {
        assertGreaterAffix("* +55 siły", FullItemReadLineType.AFFIX);
        assertGreaterAffix("★ +12 inteligencji", FullItemReadLineType.AFFIX);
        assertGreaterAffix("⭐ +90 cierni", FullItemReadLineType.AFFIX);
        assertGreaterAffix("✦ +7,0% szansy na szczęśliwy traf", FullItemReadLineType.AFFIX);
    }

    @Test
    void shouldNotGuessGreaterAffixWhenKnownAffixHasNoRollRange() {
        assertNotGreaterAffix("+114 siły", FullItemReadLineType.AFFIX);
        assertNotGreaterAffix("+494 cierni", FullItemReadLineType.AFFIX);
        assertNotGreaterAffix("13,2% redukcji czasu odnowienia", FullItemReadLineType.AFFIX);
        assertNotGreaterAffix("+7,0% szansy na szczęśliwy traf", FullItemReadLineType.AFFIX);
    }

    @Test
    void shouldNotRecognizeGreaterAffixWhenFullRollRangeIsPresent() {
        assertNotGreaterAffix("+114 siły [107 - 121]", FullItemReadLineType.AFFIX);
        assertNotGreaterAffix("+494 cierni [473 - 506]", FullItemReadLineType.AFFIX);
        assertNotGreaterAffix("+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%", FullItemReadLineType.AFFIX);
    }

    @Test
    void shouldNotRecognizeGreaterAffixWhenDamagedRollRangeFragmentIsPresent() {
        assertNotGreaterAffix("+7,0% szansy na szczęśliwy traf [7,0", FullItemReadLineType.AFFIX);
        assertNotGreaterAffix("+114 siły [107", FullItemReadLineType.AFFIX);
    }

    @Test
    void shouldRecognizeSingleValueRollRangesAndGreaterAffixFromDisplayedValue() {
        ImportedItemAffix criticalChance = extractSingle("+15,0% szansy na trafienie krytyczne [12,0]%", FullItemReadLineType.AFFIX);
        ImportedItemAffix luckyHit = extractSingle("+25,0% szansy na szczęśliwy traf [20,0]%", FullItemReadLineType.AFFIX);
        ImportedItemAffix movementSpeed = extractSingle("+25% szybkości ruchu [20]%", FullItemReadLineType.AFFIX);
        ImportedItemAffix coreRanks = extractSingle("+3 do umiejętności: Główne [3]", FullItemReadLineType.AFFIX);

        assertEquals(ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, criticalChance.getType());
        assertEquals(15.0d, criticalChance.getValue(), 0.0001d);
        assertEquals(12.0d, criticalChance.getRollRangeMin(), 0.0001d);
        assertEquals(12.0d, criticalChance.getRollRangeMax(), 0.0001d);
        assertTrue(criticalChance.isGreaterAffix());
        assertEquals(ImportedItemAffixType.LUCKY_HIT_CHANCE, luckyHit.getType());
        assertEquals(25.0d, luckyHit.getValue(), 0.0001d);
        assertEquals(20.0d, luckyHit.getRollRangeMin(), 0.0001d);
        assertEquals(20.0d, luckyHit.getRollRangeMax(), 0.0001d);
        assertTrue(luckyHit.isGreaterAffix());
        assertEquals(ImportedItemAffixType.MOVEMENT_SPEED, movementSpeed.getType());
        assertEquals(25.0d, movementSpeed.getValue(), 0.0001d);
        assertEquals(20.0d, movementSpeed.getRollRangeMin(), 0.0001d);
        assertEquals(20.0d, movementSpeed.getRollRangeMax(), 0.0001d);
        assertTrue(movementSpeed.isGreaterAffix());
        assertEquals(ImportedItemAffixType.CORE_SKILL_RANKS, coreRanks.getType());
        assertEquals(3.0d, coreRanks.getValue(), 0.0001d);
        assertEquals(3.0d, coreRanks.getRollRangeMin(), 0.0001d);
        assertEquals(3.0d, coreRanks.getRollRangeMax(), 0.0001d);
        assertFalse(coreRanks.isGreaterAffix());
    }

    @Test
    void shouldTreatMythicSingleValueBracketsAsReferenceValuesWithoutGreaterAffix() {
        ImportedItemAffix criticalChance = extractSingleMythic("+15,0% szansy na trafienie krytyczne [12,0]%", FullItemReadLineType.AFFIX);
        ImportedItemAffix luckyHit = extractSingleMythic("+25,0% szansy na szczęśliwy traf [20,0]%", FullItemReadLineType.AFFIX);
        ImportedItemAffix movementSpeed = extractSingleMythic("+25% szybkości ruchu [20]%", FullItemReadLineType.AFFIX);
        ImportedItemAffix coreRanks = extractSingleMythic("+3 do umiejętności: Główne [3]", FullItemReadLineType.AFFIX);

        assertMythicReferenceAffix(criticalChance, ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 15.0d, 12.0d);
        assertMythicReferenceAffix(luckyHit, ImportedItemAffixType.LUCKY_HIT_CHANCE, 25.0d, 20.0d);
        assertMythicReferenceAffix(movementSpeed, ImportedItemAffixType.MOVEMENT_SPEED, 25.0d, 20.0d);
        assertMythicReferenceAffix(coreRanks, ImportedItemAffixType.CORE_SKILL_RANKS, 3.0d, 3.0d);
    }

    @Test
    void shouldAvoidGreaterAffixFalsePositivesOutsideEditableRecognizedAffixes() {
        assertNoGreaterAffix("1 131 pkt. pancerza", FullItemReadLineType.BASE_STAT);
        assertNoGreaterAffix("20,0% szansy na blok [20,0]%", FullItemReadLineType.BASE_STAT);
        assertNoGreaterAffix("Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%", FullItemReadLineType.ASPECT);
        assertNoGreaterAffix("Puste gniazdo", FullItemReadLineType.SOCKET);
    }

    @Test
    void shouldKeepGreaterMarkerOutOfAffixLabelAndMissingRangeMarkerOutOfRawLine() {
        ImportedItemAffix markedAffix = extractSingle("* +55 siły", FullItemReadLineType.AFFIX);
        ImportedItemAffix missingRangeAffix = extractSingle("13,2% redukcji czasu odnowienia", FullItemReadLineType.AFFIX);

        assertEquals("Siła", markedAffix.getLabel());
        assertEquals("* +55 siły", markedAffix.getRawOcrLine());
        assertEquals("13,2% redukcji czasu odnowienia", missingRangeAffix.getRawOcrLine());
        assertEquals("13,2% redukcji czasu odnowienia", missingRangeAffix.getSourceText());
        assertFalse(missingRangeAffix.toDisplayLine().startsWith("* "));
    }

    @Test
    void shouldPreferCompleteDamageReductionRangeOverDamagedOcrRange() {
        List<ImportedItemAffix> affixes = extractor.extractEditableAffixes(new FullItemRead(
                "Generyczna Tarcza",
                "Legendarna tarcza",
                "LEGENDARY",
                "Moc przedmiotu: 900",
                "1 202 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "11,4% redukcji obrażeń [11 - 0]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "11,4% redukcji obrażeń [11,0 - 15,0]")
                )
        ));

        assertEquals(1, affixes.size());
        ImportedItemAffix affix = affixes.getFirst();
        assertEquals(ImportedItemAffixType.DAMAGE_REDUCTION, affix.getType());
        assertEquals(11.4d, affix.getValue());
        assertEquals(11.0d, affix.getRollRangeMin());
        assertEquals(15.0d, affix.getRollRangeMax());
        assertEquals("11,0 - 15,0", affix.getRollRangeLabel());
    }

    @Test
    void shouldKeepVerathielLifeOnHitRangeOnlyWhenOcrContainsParsedRange() {
        for (String sourceLine : List.of(
                "+545 pkt. zdrowia przy trafieniu [526 - 632]",
                "+545 pkt. zdrowia przy trafieniu [526 – 632]"
        )) {
            ImportedItemAffix affix = extractSingleVerathiel(sourceLine);

            assertEquals(ImportedItemAffixType.LIFE_ON_HIT, affix.getType(), sourceLine);
            assertEquals(545.0d, affix.getValue(), sourceLine);
            assertEquals(526.0d, affix.getRollRangeMin(), sourceLine);
            assertEquals(632.0d, affix.getRollRangeMax(), sourceLine);
        }
    }

    @Test
    void shouldNotFillVerathielLifeOnHitRangeFromCatalogWhenOcrRangeIsMissingOrSingleBoundary() {
        for (String sourceLine : List.of(
                "+545 pkt. zdrowia przy trafieniu [632]",
                "+545 pkt. zdrowia przy trafieniu 526 632",
                "+545 pkt. zdrowia przy trafieniu 526",
                "+545 pkt. zdrowia przy trafieniu 632"
        )) {
            ImportedItemAffix affix = extractSingleVerathiel(sourceLine);

            assertEquals(ImportedItemAffixType.LIFE_ON_HIT, affix.getType(), sourceLine);
            assertEquals(545.0d, affix.getValue(), sourceLine);
            assertEquals(null, affix.getRollRangeMin(), sourceLine);
            assertEquals(null, affix.getRollRangeMax(), sourceLine);
        }
    }

    @Test
    void shouldKeepVerathielLuckyHitResourceRestoreAsSingleCompositeAffix() {
        List<ImportedItemAffix> affixes = extractVerathiel(
                "Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]"
        );

        assertEquals(1, affixes.size());
        ImportedItemAffix affix = affixes.getFirst();
        assertEquals(ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE, affix.getType());
        assertEquals(3.0d, affix.getValue());
        assertEquals("+3", affix.getDisplayValue());
        assertEquals(3.0d, affix.getRollRangeMin());
        assertEquals(4.0d, affix.getRollRangeMax());
    }

    @Test
    void shouldRecognizeUniqueWeaponPresentationAffixesWithRollRanges() {
        List<ImportedItemAffix> affixes = extractor.extractEditableAffixes(new FullItemRead(
                "Odłamek Verathiela",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "1 874 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+134 obrażeń od broni [94 - 157]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+172 siły [150 - 180]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+300 zdrowia za zabicie [+300]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Mnożnik x16% obrażeń z upływem czasu [15 - 30]%")
                ),
                new ItemImportDetails(
                        "Odłamek Verathiela",
                        "Miecz",
                        "UNIQUE",
                        true,
                        krys.item.EquipmentSlot.MAIN_HAND,
                        900L,
                        1874L,
                        1390L,
                        2018L,
                        1704L,
                        1.10d,
                        ""
                )
        ));

        assertEquals(4, affixes.size());
        assertAffix(affixes, ImportedItemAffixType.WEAPON_DAMAGE_FLAT, 134.0d, 94.0d, 157.0d);
        assertAffix(affixes, ImportedItemAffixType.STRENGTH, 172.0d, 150.0d, 180.0d);
        assertAffix(affixes, ImportedItemAffixType.LIFE_ON_KILL, 300.0d, 300.0d, 300.0d);
        assertAffix(affixes, ImportedItemAffixType.DAMAGE_OVER_TIME_MULTIPLIER, 16.0d, 15.0d, 30.0d);
    }

    @Test
    void shouldRecognizeDotMultiplierRollRangeFromSingleVerathielLine() {
        List<ImportedItemAffix> affixes = extractor.extractEditableAffixes(new FullItemRead(
                "Odłamek Verathiela",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "1 874 pkt. obrażeń na sek.",
                List.of(new FullItemReadLine(
                        FullItemReadLineType.AFFIX,
                        "Mnożnik x16% obrażeń z upływem czasu [15 - 30]%"
                )),
                new ItemImportDetails(
                        "Odłamek Verathiela",
                        "Miecz",
                        "UNIQUE",
                        true,
                        krys.item.EquipmentSlot.MAIN_HAND,
                        900L,
                        1874L,
                        1390L,
                        2018L,
                        1704L,
                        1.10d,
                        ""
                )
        ));

        assertEquals(1, affixes.size());
        assertAffix(affixes, ImportedItemAffixType.DAMAGE_OVER_TIME_MULTIPLIER, 16.0d, 15.0d, 30.0d);
        assertFalse(affixes.getFirst().isGreaterAffix());
    }

    private void assertGreaterAffix(String text, FullItemReadLineType type) {
        assertTrue(extractSingle(text, type).isGreaterAffix(), text);
    }

    private void assertNotGreaterAffix(String text, FullItemReadLineType type) {
        assertFalse(extractSingle(text, type).isGreaterAffix(), text);
    }

    private void assertNoGreaterAffix(String text, FullItemReadLineType type) {
        assertFalse(extract(text, type).stream().anyMatch(ImportedItemAffix::isGreaterAffix), text);
    }

    private ImportedItemAffix extractSingle(String text, FullItemReadLineType type) {
        List<ImportedItemAffix> affixes = extract(text, type);
        assertEquals(1, affixes.size(), text);
        return affixes.getFirst();
    }

    private ImportedItemAffix extractSingleMythic(String text, FullItemReadLineType type) {
        List<ImportedItemAffix> affixes = extractor.extractEditableAffixes(new FullItemRead(
                "Dziedzic Zatracenia",
                "Starożytny mityczny unikatowy hełm",
                "UNIQUE",
                "900 mocy przedmiotu",
                "2 004 pkt. pancerza",
                List.of(new FullItemReadLine(type, text)),
                new ItemImportDetails("Dziedzic Zatracenia", "Hełm", "UNIQUE", true, krys.item.EquipmentSlot.HELMET,
                        900L, null, null, null, null, null, 2004L, "", true)
        ));
        assertEquals(1, affixes.size(), text);
        return affixes.getFirst();
    }

    private static void assertMythicReferenceAffix(ImportedItemAffix affix,
                                                   ImportedItemAffixType expectedType,
                                                   double expectedValue,
                                                   double expectedReferenceValue) {
        assertEquals(expectedType, affix.getType());
        assertEquals(expectedValue, affix.getValue(), 0.0001d, expectedType.getDisplayName());
        assertEquals(expectedReferenceValue, affix.getReferenceValue(), 0.0001d, expectedType.getDisplayName());
        assertNull(affix.getRollRangeMin(), expectedType.getDisplayName());
        assertNull(affix.getRollRangeMax(), expectedType.getDisplayName());
        assertFalse(affix.isGreaterAffix(), expectedType.getDisplayName());
    }

    private List<ImportedItemAffix> extract(String text, FullItemReadLineType type) {
        return extractor.extractEditableAffixes(new FullItemRead(
                "Item testowy",
                "Tarcza",
                "Legendarny",
                "800 mocy przedmiotu",
                "1 131 pkt. pancerza",
                List.of(new FullItemReadLine(type, text))
        ));
    }

    private ImportedItemAffix extractSingleVerathiel(String text) {
        List<ImportedItemAffix> affixes = extractVerathiel(text);
        assertEquals(1, affixes.size(), text);
        return affixes.getFirst();
    }

    private static void assertAffix(List<ImportedItemAffix> affixes,
                                    ImportedItemAffixType expectedType,
                                    double expectedValue,
                                    double expectedRangeMin,
                                    double expectedRangeMax) {
        ImportedItemAffix affix = affixes.stream()
                .filter(candidate -> candidate.getType() == expectedType)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Brak affixu: " + expectedType.getDisplayName()));
        assertEquals(expectedValue, affix.getValue(), 0.0001d, expectedType.getDisplayName());
        assertEquals(expectedRangeMin, affix.getRollRangeMin(), 0.0001d, expectedType.getDisplayName());
        assertEquals(expectedRangeMax, affix.getRollRangeMax(), 0.0001d, expectedType.getDisplayName());
    }

    private List<ImportedItemAffix> extractVerathiel(String text) {
        return extractor.extractEditableAffixes(new FullItemRead(
                "Odłamek Verathiela",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "1 830 pkt. obrażeń na sek.",
                List.of(new FullItemReadLine(FullItemReadLineType.AFFIX, text)),
                new ItemImportDetails(
                        "Odłamek Verathiela",
                        "Miecz",
                        "UNIQUE",
                        true,
                        krys.item.EquipmentSlot.MAIN_HAND,
                        900L,
                        1830L,
                        1350L,
                        1978L,
                        1664L,
                        1.10d,
                        ""
                )
        ));
    }
}
