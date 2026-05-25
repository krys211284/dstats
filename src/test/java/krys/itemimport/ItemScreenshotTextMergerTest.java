package krys.itemimport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemScreenshotTextMergerTest {
    private final ItemScreenshotTextMerger merger = new ItemScreenshotTextMerger();

    @Test
    void shouldMergeOverlappingOcrTextsInUploadOrder() {
        String merged = merger.merge(List.of(
                """
                        A
                        B
                        C
                        Przewiń w dół
                        """,
                """
                        Przewiń do góry
                        C
                        D
                        E
                        """
        ));

        assertTrue(merged.contains("A" + System.lineSeparator() + "B" + System.lineSeparator() + "C"
                + System.lineSeparator() + "D" + System.lineSeparator() + "E"), merged);
        assertFalse(merged.contains("Przewiń"));
    }

    @Test
    void shouldMergeRealScrolledShieldTextAndDropUiOnlyLines() {
        String merged = merger.merge(List.of(realShieldTopText(), realShieldBottomText()));

        for (String expected : List.of(
                "Miażdżąca Tarcza Kościanych Łusek",
                "Moc przedmiotu: 900",
                "25 (+25) jakości",
                "Przeistoczony",
                "1 502 pkt. pancerza",
                "+270 siły",
                "+588 do odporności na wszystkie żywioły",
                "+945 do odporności na: Ogień",
                "14,3% redukcji obrażeń",
                "+96 pkt. do wszystkich współczynników",
                "+12 do maksymalnej liczby kumulacji Animuszu",
                "Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x]",
                "Puste gniazdo",
                "Przedmiot z dodatku Lord of Hatred",
                "Brak możliwości modyfikacji"
        )) {
            assertTrue(merged.contains(expected), expected + "\n" + merged);
        }
        for (String forbidden : List.of(
                "Przewiń w dół",
                "Przewiń do góry",
                "(Wytrzymałość: +4,6%)",
                "Wyposaż",
                "Porównaj",
                "Oznacz jako śmieć",
                "Upuść"
        )) {
            assertFalse(merged.contains(forbidden), forbidden + "\n" + merged);
        }
    }

    @Test
    void shouldCanonicalizeNoisyShieldBaseLineDuplicates() {
        String merged = merger.merge(List.of(
                """
                        20,0% szansy na blok [20,0]%
                        +100% obrażeń od broni w głównej ręce [100]%
                        """,
                """
                        20,0% szansy na blok [20,010]%
                        +100% obrażeń od broni w głównej ręce [1001
                        """
        ));

        assertTrue(merged.contains("20,0% szansy na blok [20,0]%"), merged);
        assertTrue(merged.contains("+100% obrażeń od broni w głównej ręce [100]%"), merged);
        assertFalse(merged.contains("[20,010]"), merged);
        assertFalse(merged.contains("[1001"), merged);
    }

    @Test
    void shouldSplitAllStatsFromDamageReductionJoinedLineWithoutTakingFirstNumber() {
        String merged = merger.merge(List.of(
                "14,3% redukcji obrażeń [11,0 - 15,0]% +96 pkt. do wszystkich współczynników [+75 - 100]"
        ));

        assertTrue(merged.contains("14,3% redukcji obrażeń [11,0 - 15,0]%"), merged);
        assertTrue(merged.contains("+96 pkt. do wszystkich współczynników [+75 - 100]"), merged);
        assertFalse(merged.contains("+14,3 pkt. do wszystkich współczynników"), merged);
    }

    @Test
    void shouldNormalizeFortifyAspectOcrVariantsDuringCanonicalMerge() {
        for (String rawAspect : List.of(
                "Gdy masz umocnienie, zadajesz obrażenia zwiększone o61%[x] [45 - 65]%.",
                "Gdy masz umocnienie, zadajesz obrażenia zwiększone o 610[x] [45 - 65]%. 70 poziomu",
                "Gdy masz umocnienie, zadajesz obrażenia zwiększone 0 610/01x] [45 - 651%."
        )) {
            String merged = merger.merge(List.of(rawAspect));

            assertEquals("Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.",
                    merged, rawAspect);
        }
    }

    static String realShieldTopText() {
        return """
                Miażdżąca Tarcza Kościanych Łusek
                Starożytna legendarna tarcza
                Moc przedmiotu: 900
                25 (+25) jakości
                Przeistoczony
                1 502 pkt. pancerza
                (Wytrzymałość: +4,6%)
                20,0% szansy na blok [20,0]%
                +100% obrażeń od broni w głównej ręce [100]%
                +270 siły
                +588 do odporności na wszystkie żywioły
                +945 do odporności na: Ogień
                Przewiń w dół
                Wyposaż
                Porównaj
                """;
    }

    static String realShieldBottomText() {
        return """
                Przewiń do góry
                +945 do odporności na: Ogień
                14,3% redukcji obrażeń [11,0 - 15,0]%
                +96 pkt. do wszystkich współczynników [+75 - 100]
                +12 do maksymalnej liczby kumulacji Animuszu
                Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.
                Puste gniazdo
                Przedmiot z dodatku Lord of Hatred
                Brak możliwości modyfikacji
                Wartość sprzedaży: 38 450
                Trwałość: 100/100
                Oznacz jako śmieć
                Upuść
                """;
    }
}
