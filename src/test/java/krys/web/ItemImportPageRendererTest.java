package krys.web;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImageMetadata;
import krys.itemimport.ItemImportFieldConfidence;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportEditableFormFactory;
import krys.itemimport.ItemImportFieldCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje strukturę SSR formularza ręcznej walidacji affixów itemu. */
class ItemImportPageRendererTest {
    @Test
    void shouldRenderEditableAffixListAsMainManualReviewModel() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "20.0",
                "0",
                new FullItemRead(
                        "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU",
                        "Starożytna legendarna tarcza",
                        "Starożytna legendarna",
                        "Moc przedmiotu: 800",
                        "Pancerz: 1 131 pkt.",
                        List.of(new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 siły [107 - 121]"))
                ),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 114.0d, "+114 siły [107 - 121]"),
                        new ImportedItemAffix(ImportedItemAffixType.THORNS, 494.0d, "+494 cierni [473 - 506]")
                ),
                "inner-calm",
                ItemImportFieldConfidence.HIGH,
                "inner-calm"
        );
        HeroProfile activeHero = new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty());

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                null,
                List.of(),
                null,
                activeHero,
                "Import testowy",
                ""
        ));

        assertTrue(html.contains("Ręczna weryfikacja affixów"));
        assertTrue(html.contains("Typ itemu"));
        assertTrue(html.contains("Slot ekwipunku"));
        assertTrue(html.contains("Aspekt"));
        assertTrue(html.contains("name=\"affixType_0\""));
        assertTrue(html.contains("name=\"affixValue_0\" value=\"114\""));
        assertTrue(html.contains("class=\"secondary-button remove-affix-button\""));
        assertFalse(html.contains("name=\"affixRemoved_0\""));
        assertTrue(html.contains("+114 siły [107 - 121]"));
        assertTrue(html.contains("name=\"affixType_1\""));
        assertTrue(html.contains("name=\"affixValue_1\" value=\"494\""));
        assertTrue(html.contains("Dodaj affix"));
        assertTrue(html.contains("type=\"button\" id=\"addAffixButton\""));
        assertTrue(html.contains("name=\"newAffixType\""));
        assertTrue(html.contains("name=\"newAffixValue\""));
        assertTrue(html.contains("Aspekt Wewnętrznego Spokoju"));
        assertFalse(html.contains(">Aspekt Wewnętrznego Spokoju (wysoka)<"));
        assertTrue(html.contains("Sugestia OCR: Aspekt Wewnętrznego Spokoju"));
        assertTrue(html.contains("wysoka"));
        assertFalse(html.contains("Projekcja do aktualnego runtime"));
        assertFalse(html.contains("Mapowanie do aktualnego modelu buildu"));
    }

    @Test
    void shouldCheckGreaterAffixCheckboxWhenExtractorDetectsMissingRollRange() {
        ItemImageImportCandidateParseResult parseResult = new ItemImageImportCandidateParseResult(
                new ItemImageMetadata("tarcza.png", "image/png", "PNG", 1200, 800),
                new FullItemRead(
                        "Tarcza testowa",
                        "Tarcza",
                        "Legendarny",
                        "800 mocy przedmiotu",
                        "1 131 pkt. pancerza",
                        List.of(new FullItemReadLine(FullItemReadLineType.AFFIX, "13,2% redukcji czasu odnowienia"))
                ),
                new ItemImportFieldCandidate<>("OFF_HAND", EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.HIGH, "slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                ItemImportFieldCandidate.unknown("str"),
                ItemImportFieldCandidate.unknown("int"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution"),
                "Import wspomagany"
        );
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(parseResult);
        HeroProfile activeHero = new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty());

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                null,
                List.of(),
                null,
                activeHero,
                "Import testowy",
                ""
        ));

        assertTrue(html.contains("name=\"affixGreater_0\" value=\"true\" checked"));
        assertTrue(html.contains("* 13,2% redukcji czasu odnowienia"));
    }
}
