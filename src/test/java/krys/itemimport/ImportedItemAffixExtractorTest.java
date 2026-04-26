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
    void shouldRecognizeGreaterAffixWhenKnownAffixHasNoRollRange() {
        assertGreaterAffix("+114 siły", FullItemReadLineType.AFFIX);
        assertGreaterAffix("+494 cierni", FullItemReadLineType.AFFIX);
        assertGreaterAffix("13,2% redukcji czasu odnowienia", FullItemReadLineType.AFFIX);
        assertGreaterAffix("+7,0% szansy na szczęśliwy traf", FullItemReadLineType.AFFIX);
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
        assertTrue(missingRangeAffix.toDisplayLine().startsWith("* "));
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
}
