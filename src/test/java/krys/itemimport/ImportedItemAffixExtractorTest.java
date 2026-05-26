package krys.itemimport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void shouldRepairVerathielLifeOnHitRangeFromDamagedOcrUsingCatalogContext() {
        for (String sourceLine : List.of(
                "+545 pkt. zdrowia przy trafieniu [526 - 632]",
                "+545 pkt. zdrowia przy trafieniu [5 - 632]",
                "+545 pkt. zdrowia przy trafieniu [632]",
                "+545 pkt. zdrowia przy trafieniu 526 632",
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
