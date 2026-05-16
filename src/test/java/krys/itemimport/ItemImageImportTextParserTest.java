package krys.itemimport;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void shouldIgnoreShieldBaseArmorWhenAffixRollIsMissingFromOcrLine() {
        String ocrText = """
                Tarcza
                1 131 pkt. pancerza do siły [107 - 121]
                1 131 pkt. pancerza do cierni [473 - 506]
                1 131 pkt. pancerza szansy na blok [18,0 - 22,5]
                """;

        ItemImageImportCandidateParseResult result = parser.parse(metadata, ocrText);

        assertEquals(EquipmentSlot.OFF_HAND, result.getSlotCandidate().getSuggestedValue());
        assertNull(result.getStrengthCandidate().getSuggestedValue());
        assertNull(result.getThornsCandidate().getSuggestedValue());
        assertNull(result.getBlockChanceCandidate().getSuggestedValue());
    }

    @Test
    void shouldRecognizeShieldRollsWithoutLeakingBaseArmorIntoAffixes() {
        String ocrText = """
                Tarcza
                1 131 pkt. pancerza
                +114 do siły [107 - 121]
                +494 cierni [473 - 506]
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

    @Test
    void shouldRecognizeVerathielUniqueSwordWeaponFieldsFromPolishOcr() {
        ItemImageImportCandidateParseResult result = parser.parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                verathielRawText()
        );

        ItemImportDetails details = result.getFullItemRead().getDetails();

        assertEquals("Odłamek Verathiela", details.getItemName());
        assertEquals("UNIQUE", details.getItemRarity());
        assertTrue(details.isAncient());
        assertEquals("Miecz", details.getItemType());
        assertEquals(EquipmentSlot.MAIN_HAND, details.getEquipmentSlot());
        assertEquals(900L, details.getItemPower());
        assertEquals(1830L, details.getWeaponDps());
        assertEquals(1350L, details.getWeaponDamageMin());
        assertEquals(1978L, details.getWeaponDamageMax());
        assertEquals(1664L, details.getAverageWeaponDamage());
        assertEquals(1.10d, details.getAttacksPerSecond());
        assertFalse(details.getUniqueEffectText().isBlank());
        assertTrue(details.getUniqueEffectText().contains("100%[x]"));
        assertTrue(details.getUniqueEffectText().contains("[70 - 100]"));
        assertTrue(details.getUniqueEffectText().contains("25 pkt. podstawowego zasobu"));
    }

    @Test
    void shouldNotRegressItemPowerToFirstDigitWhenVerathielRawTextContainsNineHundred() {
        ItemImageImportCandidateParseResult result = parser.parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                verathielRawText()
        );

        assertEquals(900L, result.getFullItemRead().getDetails().getItemPower());
        assertFalse(Long.valueOf(1L).equals(result.getFullItemRead().getDetails().getItemPower()));
    }

    @Test
    void shouldExtractVerathielAffixesSeparatelyFromUniqueEffect() {
        ItemImageImportCandidateParseResult result = parser.parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                verathielRawText()
        );

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals(4, form.getAffixes().size());
        assertTrue(form.getAffixes().stream().anyMatch(affix ->
                affix.getType() == ImportedItemAffixType.WEAPON_DAMAGE_FLAT
                        && affix.getValue() == 94.0d
                        && affix.getSourceText().contains("[94 - 157]")));
        assertTrue(form.getAffixes().stream().anyMatch(affix ->
                affix.getType() == ImportedItemAffixType.MAXIMUM_LIFE
                        && affix.getValue() == 2141.0d
                        && affix.getSourceText().contains("[1 831 - 2 200]")));
        assertTrue(form.getAffixes().stream().anyMatch(affix ->
                affix.getType() == ImportedItemAffixType.LIFE_ON_HIT
                        && affix.getValue() == 545.0d
                        && affix.getSourceText().contains("[526 - 632]")));
        assertTrue(form.getAffixes().stream().anyMatch(affix ->
                affix.getType() == ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE
                        && affix.getSourceText().contains("15%")
                        && affix.getSourceText().contains("+3")
                        && affix.getSourceText().contains("[3 - 4]")));
        assertTrue(form.getAffixes().stream()
                .noneMatch(affix -> affix.getSourceText().contains("Umiejętności Podstawowe")));
    }

    @Test
    void shouldRecognizeNoisyVerathielOcrWithoutPolishCharacters() {
        ItemImageImportCandidateParseResult result = parser.parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                """
                        ODLFIK VERATHEL
                        STAROZYTNY UNIKATOWY MIECZ
                        Moc   przedmiotu . 900
                        1 830 pkt. obrazen na sek. (+1830)
                        [1 350 - 1 978] pkt. obrazen za trafienie
                        1,10 ataku na sekunde
                        +94 obrazen od broni [94 - 157]
                        Umiejetnosci Podstawowe zadaja obrazenia zwiekszone o 100%[x] [70 - 100],
                        ale dodatkowo zuzywaja 25 pkt. podstawowego zasobu.
                        """
        );

        ItemImportDetails details = result.getFullItemRead().getDetails();

        assertEquals("Odłamek Verathiela", details.getItemName());
        assertEquals("UNIQUE", details.getItemRarity());
        assertTrue(details.isAncient());
        assertEquals("Miecz", details.getItemType());
        assertEquals(EquipmentSlot.MAIN_HAND, details.getEquipmentSlot());
        assertEquals(900L, details.getItemPower());
        assertEquals(1830L, details.getWeaponDps());
        assertEquals(1350L, details.getWeaponDamageMin());
        assertEquals(1978L, details.getWeaponDamageMax());
        assertEquals(1664L, details.getAverageWeaponDamage());
        assertEquals(1.10d, details.getAttacksPerSecond());
    }

    @Test
    void shouldRecognizeVerathielDamageRangeFromSupportedOcrVariants() {
        for (String rangeLine : List.of(
                "[1 350 - 1 978] pkt. obrażeń za trafienie",
                "1 350 - 1 978 pkt. obrażeń za trafienie",
                "[1350 - 1978] pkt. obrażeń za trafienie",
                "1350-1978 pkt. obrażeń za trafienie",
                "1 350 – 1 978"
        )) {
            ItemImageImportCandidateParseResult result = parser.parse(
                    new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                    """
                            ODŁAMEK VERATHIEL
                            Starożytny unikatowy miecz
                            1 830 pkt. obrażeń na sek.
                            %s
                            1,10 ataku na sekundę
                            """.formatted(rangeLine)
            );

            ItemImportDetails details = result.getFullItemRead().getDetails();
            assertEquals(1350L, details.getWeaponDamageMin(), rangeLine);
            assertEquals(1978L, details.getWeaponDamageMax(), rangeLine);
            assertEquals(1664L, details.getAverageWeaponDamage(), rangeLine);
            assertFalse(Long.valueOf(1830L).equals(details.getAverageWeaponDamage()), rangeLine);
        }
    }

    @Test
    void shouldRecognizeVerathielDamageRangeFromCondensedNoisyOcrLine() {
        ItemImageImportCandidateParseResult result = parser.parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                """
                        ODLFIK VERATHEL Starozytny unikatowy miecz Moc przedmiotu 900 1 830 pkt. obrazen na sek. 1350–1978 pkt. obrazen za trafienie 1,10 ataku na sekunde +94 obrazen od broni [94 - 157] +2 141 maksymalnego zdrowia [1 831 - 2 200] +545 pkt. zdrowia przy trafieniu [526 - 632] Szczesliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4] Umiejetnosci Podstawowe zadaja obrazenia zwiekszone o 100%[x] [70 - 100], ale dodatkowo zuzywaja 25 pkt. podstawowego zasobu.
                        """
        );

        ItemImportDetails details = result.getFullItemRead().getDetails();
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals(1350L, details.getWeaponDamageMin());
        assertEquals(1978L, details.getWeaponDamageMax());
        assertEquals(1664L, details.getAverageWeaponDamage());
        assertEquals(4, form.getAffixes().size());
        assertEquals("verathiel_shard", form.getSelectedAspectId());
    }

    @Test
    void shouldTreatAncientUniqueRarityAsAncientTrueAndUnique() {
        for (String text : List.of(
                "Starożytny unikatowy miecz",
                "Starożytna unikatowa",
                "STAROZYTNY UNIKATOWY MIECZ",
                "starozytny unikatowy miecz"
        )) {
            ItemImageImportCandidateParseResult result = parser.parse(
                    new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                    text
            );

            assertEquals("UNIQUE", result.getFullItemRead().getDetails().getItemRarity(), text);
            assertTrue(result.getFullItemRead().getDetails().isAncient(), text);
            if (text.toUpperCase().contains("MIECZ")) {
                assertEquals("Miecz", result.getFullItemRead().getDetails().getItemType(), text);
                assertEquals(EquipmentSlot.MAIN_HAND, result.getFullItemRead().getDetails().getEquipmentSlot(), text);
            }
        }
    }

    @Test
    void shouldRecognizeItemPowerNineHundredFromLooseAndCondensedOcrForms() {
        for (String text : List.of(
                "Moc przedmiotu : 900",
                "Moc przedmiotu. 900",
                "Moc przedmiotu 900",
                "Moc@@@przedmiotu###900",
                "Mocprzedmiotu900"
        )) {
            ItemImageImportCandidateParseResult result = parser.parse(
                    new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                    text
            );

            assertEquals(900L, result.getFullItemRead().getDetails().getItemPower(), text);
            assertFalse(Long.valueOf(1L).equals(result.getFullItemRead().getDetails().getItemPower()), text);
        }
    }

    static String verathielRawText() {
        return """
                ODŁAMEK
                VERATHIEL
                Starożytny unikatowy miecz
                Moc przedmiotu: 900
                1 830 pkt. obrażeń na sek.
                [1 350 - 1 978] pkt. obrażeń za trafienie
                1,10 ataku na sekundę (Szybka)
                +94 obrażeń od broni [94 - 157]
                +2 141 maksymalnego zdrowia [1 831 - 2 200]
                +545 pkt. zdrowia przy trafieniu [526 - 632]
                Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]
                Umiejętności Podstawowe zadają
                obrażenia zwiększone o 100%[x] [70 - 100],
                ale dodatkowo zużywają 25 pkt. podstawowego zasobu.
                """;
    }
}
