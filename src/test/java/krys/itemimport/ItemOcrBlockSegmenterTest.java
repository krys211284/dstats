package krys.itemimport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje katalogową segmentację długich bloków OCR itemu. */
class ItemOcrBlockSegmenterTest {
    private final ItemOcrBlockSegmenter segmenter = new ItemOcrBlockSegmenter();

    @Test
    void shouldSegmentLongBlockByAffixTransfigurationAndTemperingAnchors() {
        String block = "+3 do umiejętności: Główne [31 +115 pkt. do wszystkich współczynników +175 - 1001 "
                + "+12 do maksymalnej liczby kumulacji Animuszu";

        List<String> lines = segmenter.segmentLines(List.of(block));

        assertTrue(lines.contains(block));
        assertContains(lines, "+3 DO UMIEJETNOSCI: GLOWNE [31");
        assertContains(lines, "+115 PKT. DO WSZYSTKICH WSPOLCZYNNIKOW [75 - 100]");
        assertContains(lines, "+12 DO MAKSYMALNEJ LICZBY KUMULACJI ANIMUSZU");
    }

    @Test
    void shouldRecoverGluedFlatTransfigurationSegmentValueNearAnchor() {
        String block = "+3 do umiejętności: Główne [31 4115 pkt. do wszystkich współczynników +175 - 1001 "
                + "+12 do maksymalnej liczby kumulacji Animuszu";

        List<String> lines = segmenter.segmentLines(List.of(block));

        assertTrue(lines.contains(block));
        assertContains(lines, "+115 PKT. DO WSZYSTKICH WSPOLCZYNNIKOW [75 - 100]");
    }

    @Test
    void shouldPreserveOriginalTextWhenAddingCandidates() {
        String block = "+9 szybkość ruchu +12 do maksymalnej liczby kumulacji Animuszu";

        List<String> lines = segmenter.segmentLines(List.of(block));

        assertTrue(lines.contains(block));
        assertContains(lines, "+9 SZYBKOSC RUCHU");
        assertContains(lines, "+12 DO MAKSYMALNEJ LICZBY KUMULACJI ANIMUSZU");
    }

    private static void assertContains(List<String> lines, String expected) {
        assertTrue(lines.stream().anyMatch(line -> line.contains(expected)), () -> "Brak segmentu: " + expected + " w " + lines);
    }
}
