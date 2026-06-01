package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.transfiguration.HoradricTransfigurationOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje uzupełnianie formularza importu na podstawie pełnego odczytu OCR. */
class ItemImportEditableFormFactoryTest {
    private final ItemImportEditableFormFactory factory = new ItemImportEditableFormFactory();

    @Test
    void shouldRecognizeTransfigurationByTextWithoutRejectingValueOutsideCatalogRange() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+115 pkt. do wszystkich współczynników [75 - 100]")
                ),
                details("Generyczny Hełm", "")
        )));

        assertEquals(HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX, form.getTransfiguration().getOutcome());
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(115.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(75.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMin(), 0.0001d);
        assertEquals(100.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMax(), 0.0001d);
    }

    @Test
    void shouldUseTransfigurationValueLocalToAnchorInJoinedOcrLine() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX,
                                "+3 do umiejętności: Główne [3] +115 pkt. do wszystkich współczynników [75 - 100]")
                ),
                details("Generyczny Hełm", "")
        )));

        assertEquals(3.0d, affix(form, ImportedItemAffixType.CORE_SKILL_RANKS).getValue(), 0.0001d);
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(115.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(75.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMin(), 0.0001d);
        assertEquals(100.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMax(), 0.0001d);
    }

    @Test
    void shouldRecoverGluedFlatTransfigurationValueFromLocalOcrToken() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900 25 (+25) jakości",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX,
                                "+3 do umiejętności: Główne [31 4115 pkt. do wszystkich współczynników +175 - 1001")
                ),
                details("Generyczny Hełm", "")
        )));

        assertEquals(3.0d, affix(form, ImportedItemAffixType.CORE_SKILL_RANKS).getValue(), 0.0001d);
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(115.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(75.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMin(), 0.0001d);
        assertEquals(100.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMax(), 0.0001d);
    }

    @Test
    void shouldNotUsePreviousAffixValueWhenTransfigurationLocalValueIsUnsafe() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX,
                                "+3 do umiejętności: Główne [3] do wszystkich współczynników [75 - 100]")
                ),
                details("Generyczny Hełm", "")
        )));

        assertEquals(3.0d, affix(form, ImportedItemAffixType.CORE_SKILL_RANKS).getValue(), 0.0001d);
        assertEquals(HoradricTransfigurationOutcome.UNKNOWN, form.getTransfiguration().getOutcome());
        assertNull(form.getTransfiguration().getAddedTransfigurationAffix());
    }

    @Test
    void shouldKeepArbitraryAllStatsTransfigurationValueOutsideCatalogRange() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+999 pkt. do wszystkich współczynników [75 - 100]")
                ),
                details("Generyczny Hełm", "")
        )));

        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(999.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(75.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMin(), 0.0001d);
        assertEquals(100.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMax(), 0.0001d);
    }

    @Test
    void shouldImportDifferentTransfigurationValueOutsideCatalogRangeWhenTextMatchesDefinition() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczne Buty",
                "Starożytne unikatowe buty",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "1 000 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+47% szybkość ruchu [20 - 30]")
                ),
                details("Generyczne Buty", "")
        )));

        assertEquals("MOVEMENT_SPEED", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(47.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(20.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMin(), 0.0001d);
        assertEquals(30.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMax(), 0.0001d);
    }

    @Test
    void shouldUseCanonicalAspectTextFromRegistryForMatchedUniqueEffect() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Dziedzic Zatracenia",
                "Starożytny mityczny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(new FullItemReadLine(
                        FullItemReadLineType.ASPECT,
                        "Poddaj się nienawiści i doświadcz Łaski Matki, która zwiększy zadawane przez ciebie obrażenia o"
                )),
                details("Dziedzic Zatracenia", "Poddaj się nienawiści i doświadcz Łaski Matki")
        )));

        assertEquals("heir_of_perdition", form.getSelectedAspectId());
        assertTrue(form.getUniqueEffectText().contains("80%[x]"), form.getUniqueEffectText());
        assertEquals(AspectRuntimeStatus.DESCRIPTIVE_ONLY,
                ApplicationAspectRegistry.get().findById(form.getSelectedAspectId()).orElseThrow().getRuntimeStatus());
    }

    private static ItemImageImportCandidateParseResult parseResult(FullItemRead fullItemRead) {
        return new ItemImageImportCandidateParseResult(
                new ItemImageMetadata("test.png", "image/png", "png", 1, 1),
                fullItemRead,
                new ItemImportFieldCandidate<>("", EquipmentSlot.HELMET, ItemImportFieldConfidence.HIGH, ""),
                ItemImportFieldCandidate.unknown(""),
                ItemImportFieldCandidate.unknown(""),
                ItemImportFieldCandidate.unknown(""),
                ItemImportFieldCandidate.unknown(""),
                ItemImportFieldCandidate.unknown(""),
                ItemImportFieldCandidate.unknown(""),
                ""
        );
    }

    private static ItemImportDetails details(String itemName, String effectText) {
        return new ItemImportDetails(itemName, "Hełm", "UNIQUE", true, EquipmentSlot.HELMET,
                900L, null, null, null, null, null, 2004L, effectText, itemName.equals("Dziedzic Zatracenia"));
    }

    private static ImportedItemAffix affix(ItemImportEditableForm form, ImportedItemAffixType type) {
        return form.getAffixes().stream()
                .filter(affix -> affix.getType() == type)
                .findFirst()
                .orElseThrow();
    }
}
