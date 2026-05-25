package krys.itemimport;

import org.junit.jupiter.api.Test;

import java.util.List;

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
