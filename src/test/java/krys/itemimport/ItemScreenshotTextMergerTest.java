package krys.itemimport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        String merged = merger.merge(List.of(ItemImportTextFixtures.realShieldTopText(), ItemImportTextFixtures.realShieldBottomText()));

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
    void shouldKeepVerathielDotMultiplierRangeDuringCanonicalMerge() {
        String merged = merger.merge(List.of(
                "Mnoznik x16% obrazen z uplywem czasu [15 - 301%",
                "Mnożnik x16% obrażeń z upływem czasu"
        ));

        assertEquals("Mnożnik x16% obrażeń z upływem czasu [15 - 30]%", merged);
    }

    @Test
    void shouldPreserveBracketDataFromRicherGenericAffixVariant() {
        String merged = merger.merge(List.of(
                """
                        Generyczny Helm Testowy
                        Starożytny mityczny unikatowy hełm
                        +15,0% szansy na trafienie krytyczne
                        """,
                "+15,0% szansy na trafienie krytyczne [12,0]%"
        ));

        assertTrue(merged.contains("+15,0% szansy na trafienie krytyczne [12,0]%"), merged);
    }

    @Test
    void shouldPreserveDamagedMythicReferenceBeforeParserNormalizesIt() {
        String merged = merger.merge(List.of(
                """
                        Dziedzic Zatracenia
                        Starożytny mityczny unikatowy hełm
                        Moc przedmiotu: 900
                        +15,0% szansy na trafienie krytyczne 112,01% +25,0% szansy szczęsnwy traf [20,01% +25% szybkości ruchu 1201% +3 do umiejętności: Główne [31
                        """,
                "+15,0% szansy na trafienie krytyczne"
        ));

        assertTrue(merged.contains("+15,0% szansy na trafienie krytyczne [12]%"), merged);
        assertTrue(merged.contains("+25,0% szansy na szczęśliwy traf [20,0]%"), merged);
        assertTrue(merged.contains("+25% szybkości ruchu [20]%"), merged);
        assertTrue(merged.contains("+3 do umiejętności: Główne"), merged);
        assertFalse(merged.contains("+3 do umiejętności: Główne [3]"), merged);

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(new ItemImageImportTextParser().parse(
                new ItemImageMetadata("dziedzic-merged.png", "image/png", "MULTI", 327, 1444),
                merged
        ));

        assertEquals(4, form.getAffixes().size());
        assertMythicReferenceAffix(form, ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 15.0d, 12.0d);
        assertMythicReferenceAffix(form, ImportedItemAffixType.LUCKY_HIT_CHANCE, 25.0d, 20.0d);
        assertMythicReferenceAffix(form, ImportedItemAffixType.MOVEMENT_SPEED, 25.0d, 20.0d);
        ImportedItemAffix coreRanks = form.getAffixes().stream()
                .filter(candidate -> candidate.getType() == ImportedItemAffixType.CORE_SKILL_RANKS)
                .findFirst()
                .orElseThrow();
        assertEquals(3.0d, coreRanks.getValue(), 0.0001d);
        assertNull(coreRanks.getReferenceValue());
        assertNull(coreRanks.getRollRangeMin());
        assertNull(coreRanks.getRollRangeMax());
        assertFalse(coreRanks.isGreaterAffix());
    }

    @Test
    void shouldNotRepairDamagedCoreSkillRankBracketIntoCertainReference() {
        String merged = merger.merge(List.of(
                """
                        Generyczny Helm Testowy
                        Starożytny mityczny unikatowy hełm
                        +2 do umiejętności: Główne [31
                        """
        ));

        assertTrue(merged.contains("+2 do umiejętności: Główne"), merged);
        assertFalse(merged.contains("[3]"), merged);

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(new ItemImageImportTextParser().parse(
                new ItemImageMetadata("core-ranks-damaged.png", "image/png", "MULTI", 327, 1444),
                merged
        ));
        ImportedItemAffix coreRanks = form.getAffixes().stream()
                .filter(candidate -> candidate.getType() == ImportedItemAffixType.CORE_SKILL_RANKS)
                .findFirst()
                .orElseThrow();

        assertEquals(2.0d, coreRanks.getValue(), 0.0001d);
        assertNull(coreRanks.getReferenceValue());
        assertFalse(coreRanks.isGreaterAffix());
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

    @Test
    void shouldKeepStormMoonShieldCanonicalLinesAndQualityBonus() {
        String merged = merger.merge(List.of(
                ItemImportTextFixtures.stormMoonShieldTopText(),
                ItemImportTextFixtures.stormMoonShieldBottomText()
        ));

        for (String expected : List.of(
                "Tarcza Burzy Księżycowego Szału",
                "29 (+25) jakości",
                "+217 siły",
                "+11,0% szansy na trafienie krytyczne",
                "17,6% redukcji obrażeń",
                "12,3% redukcji czasu odnowienia",
                "+4 do jakości przedmiotu [1 - 15]",
                "+12 do maksymalnej liczby kumulacji Animuszu",
                "Puste gniazdo",
                "Naznaczenie",
                "Wampirycznego Szału Krwi"
        )) {
            assertTrue(merged.contains(expected), expected + "\n" + merged);
        }
        for (String forbidden : List.of(
                "Rynsztunek w Zbrojowni",
                "Przewiń w dół",
                "Przewiń do góry",
                "Wymaga 70 poziomu",
                "Przypisano do konta",
                "Wartość sprzedaży",
                "Trwałość"
        )) {
            assertFalse(merged.contains(forbidden), forbidden + "\n" + merged);
        }
    }

    private static void assertMythicReferenceAffix(ItemImportEditableForm form,
                                                   ImportedItemAffixType expectedType,
                                                   double expectedValue,
                                                   double expectedReferenceValue) {
        ImportedItemAffix affix = form.getAffixes().stream()
                .filter(candidate -> candidate.getType() == expectedType)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Brak affixu: " + expectedType.getDisplayName()));
        assertEquals(expectedValue, affix.getValue(), 0.0001d, expectedType.getDisplayName());
        assertEquals(expectedReferenceValue, affix.getReferenceValue(), 0.0001d, expectedType.getDisplayName());
        assertNull(affix.getRollRangeMin(), expectedType.getDisplayName());
        assertNull(affix.getRollRangeMax(), expectedType.getDisplayName());
        assertFalse(affix.isGreaterAffix(), expectedType.getDisplayName());
    }

}
