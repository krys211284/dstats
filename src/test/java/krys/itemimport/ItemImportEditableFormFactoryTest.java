package krys.itemimport;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Test mapowania wstępnie rozpoznanych pól do edytowalnego formularza potwierdzenia. */
class ItemImportEditableFormFactoryTest {
    @Test
    void shouldDetectEmptySocketFromExactSocketLine() {
        ItemImageImportCandidateParseResult parseResult = new ItemImageImportCandidateParseResult(
                new ItemImageMetadata("tarcza.png", "image/png", "PNG", 1200, 800),
                new FullItemRead(
                        "Tarcza",
                        "Tarcza",
                        "Legendarny",
                        "Moc przedmiotu: 900",
                        "1 502 pkt. pancerza",
                        java.util.List.of(new FullItemReadLine(FullItemReadLineType.SOCKET, "Puste gniazdo"))
                ),
                new ItemImportFieldCandidate<>("OFF_HAND", EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.MEDIUM, "slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                ItemImportFieldCandidate.unknown("str"),
                ItemImportFieldCandidate.unknown("int"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution"),
                "Import wspomagany"
        );

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(parseResult);

        assertEquals(1, form.getSocketing().getSocketCount());
        assertEquals(krys.socketing.SocketContentType.EMPTY, form.getSocketing().socketAt(0).getContentType());
    }

    @Test
    void shouldMapSuggestedFieldsToEditableForm() {
        ItemImageImportCandidateParseResult parseResult = new ItemImageImportCandidateParseResult(
                new ItemImageMetadata("tarcza.png", "image/png", "PNG", 1200, 800),
                new FullItemRead(
                        "Tarcza testowa",
                        "Tarcza",
                        "Legendarny",
                        "800 mocy przedmiotu",
                        "1 131 pkt. pancerza",
                        java.util.List.of(
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+55 Strength"),
                                new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%"),
                                new FullItemReadLine(FullItemReadLineType.ASPECT, "Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek.")
                        )
                ),
                new ItemImportFieldCandidate<>("OFF_HAND", EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.MEDIUM, "slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                new ItemImportFieldCandidate<>("+55 Strength", 55.0d, ItemImportFieldConfidence.HIGH, "str"),
                new ItemImportFieldCandidate<>("+12 Intelligence", 12.0d, ItemImportFieldConfidence.MEDIUM, "int"),
                new ItemImportFieldCandidate<>("+90 Thorns", 90.0d, ItemImportFieldConfidence.HIGH, "thorns"),
                new ItemImportFieldCandidate<>("+18% Block Chance", 18.0d, ItemImportFieldConfidence.LOW, "block"),
                new ItemImportFieldCandidate<>("+25% Retribution Chance", 25.0d, ItemImportFieldConfidence.MEDIUM, "retribution"),
                "Import wspomagany"
        );

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(parseResult);

        assertEquals("tarcza.png", form.getSourceImageName());
        assertEquals("OFF_HAND", form.getSlot());
        assertEquals("0", form.getWeaponDamage());
        assertEquals("55", form.getStrength());
        assertEquals("12", form.getIntelligence());
        assertEquals("90", form.getThorns());
        assertEquals("18", form.getBlockChance());
        assertEquals("25", form.getRetributionChance());
        assertEquals("Tarcza testowa", form.getFullItemRead().getItemName());
        assertEquals(3, form.getFullItemRead().getLines().size());
        assertEquals(1, form.getAffixes().size());
        assertEquals(ImportedItemAffixType.STRENGTH, form.getAffixes().getFirst().getType());
        assertEquals(55.0d, form.getAffixes().getFirst().getValue());
        assertEquals("+55 Strength", form.getAffixes().getFirst().getSourceText());
        assertEquals("inner-calm", form.getOcrSuggestedAspectId());
        assertEquals(ItemImportFieldConfidence.HIGH, form.getOcrAspectConfidence());
        assertEquals("inner-calm", form.getSelectedAspectId());
    }

    @Test
    void shouldKeepGreaterAffixMarkerAsStructuredFieldForKnownOcrSymbols() {
        ItemImageImportCandidateParseResult parseResult = new ItemImageImportCandidateParseResult(
                new ItemImageMetadata("gwiazdki.png", "image/png", "PNG", 1200, 800),
                new FullItemRead(
                        "Test gwiazdek",
                        "Tarcza",
                        "Legendarny",
                        "800 mocy przedmiotu",
                        "1 131 pkt. pancerza",
                        java.util.List.of(
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "* +55 siły"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "★ +12 inteligencji"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "⭐ +90 cierni"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "✦ +7,0% szansy na szczęśliwy traf")
                        )
                ),
                new ItemImportFieldCandidate<>("OFF_HAND", EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.MEDIUM, "slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                ItemImportFieldCandidate.unknown("str"),
                ItemImportFieldCandidate.unknown("int"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution"),
                "Import wspomagany"
        );

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(parseResult);

        assertEquals(4, form.getAffixes().size());
        for (ImportedItemAffix affix : form.getAffixes()) {
            assertEquals(true, affix.isGreaterAffix());
        }
    }

    @Test
    void shouldKeepUnknownOcrAspectAsSuggestionMissWithoutSelectingFinalAspect() {
        ItemImageImportCandidateParseResult parseResult = new ItemImageImportCandidateParseResult(
                new ItemImageMetadata("unknown.png", "image/png", "PNG", 1200, 800),
                new FullItemRead(
                        "Tarcza testowa",
                        "Tarcza",
                        "Legendarny",
                        "800 mocy przedmiotu",
                        "1 131 pkt. pancerza",
                        java.util.List.of(new FullItemReadLine(FullItemReadLineType.ASPECT, "Aspekt zupełnie nieznany z OCR"))
                ),
                new ItemImportFieldCandidate<>("OFF_HAND", EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.MEDIUM, "slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                ItemImportFieldCandidate.unknown("str"),
                ItemImportFieldCandidate.unknown("int"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution"),
                "Import wspomagany"
        );

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(parseResult);

        assertEquals("", form.getOcrSuggestedAspectId());
        assertEquals(ItemImportFieldConfidence.UNKNOWN, form.getOcrAspectConfidence());
        assertEquals("", form.getSelectedAspectId());
        assertEquals("Aspekt zupełnie nieznany z OCR", form.getFullItemRead().getLines().getFirst().getText());
    }

    @Test
    void shouldPrefillVerathielManualConfirmationFromNoisyOcrDetailsWithoutLegacyWeaponDamage() {
        ItemImageImportCandidateParseResult parseResult = new ItemImageImportTextParser().parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                """
                        ODLFIK VERATHEL
                        Starożytny unikatowy miecz
                        Moc przedmiotu. 900
                        1 830 pkt. obrażeń na sek.
                        [1 350 - 1 978] pkt. obrażeń za trafienie
                        1,10 ataku na sekundę
                        """
        );

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(parseResult);

        assertEquals("Odłamek Verathiela", form.getItemName());
        assertEquals("Miecz", form.getItemType());
        assertEquals("UNIQUE", form.getItemRarity());
        assertEquals(true, form.isAncient());
        assertEquals("MAIN_HAND", form.getSlot());
        assertEquals("900", form.getItemPower());
        assertEquals("1830", form.getWeaponDps());
        assertEquals("1350", form.getWeaponDamageMin());
        assertEquals("1978", form.getWeaponDamageMax());
        assertEquals("1664", form.getAverageWeaponDamage());
        assertEquals("1.10", form.getAttacksPerSecond());
        assertEquals("0", form.getWeaponDamage());
    }

    @Test
    void shouldSelectVerathielUniqueAspectFromRecognizedUniqueEffect() {
        ItemImageImportCandidateParseResult parseResult = new ItemImageImportTextParser().parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                ItemImageImportTextParserTest.verathielRawText()
        );

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(parseResult);
        AspectDefinition aspect = ApplicationAspectRegistry.get().findById(form.getSelectedAspectId()).orElseThrow();

        assertEquals("verathiel_shard", form.getOcrSuggestedAspectId());
        assertEquals("verathiel_shard", form.getSelectedAspectId());
        assertEquals(AspectType.UNIQUE, aspect.getAspectType());
        assertEquals(AspectRuntimeStatus.DESCRIPTIVE_ONLY, aspect.getRuntimeStatus());
    }
}
