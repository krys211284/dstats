package krys.web;

import krys.hero.HeroClass;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportEditableForm;
import org.junit.jupiter.api.Test;

import java.util.List;

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
                )
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
        assertTrue(html.contains("name=\"affixType_0\""));
        assertTrue(html.contains("name=\"affixValue_0\" value=\"114\""));
        assertTrue(html.contains("name=\"affixRemoved_0\""));
        assertTrue(html.contains("+114 siły [107 - 121]"));
        assertTrue(html.contains("name=\"affixType_1\""));
        assertTrue(html.contains("name=\"affixValue_1\" value=\"494\""));
        assertTrue(html.contains("Dodaj affix"));
        assertTrue(html.contains("name=\"newAffixType\""));
        assertTrue(html.contains("name=\"newAffixValue\""));
        assertTrue(html.contains("Projekcja do aktualnego runtime"));
    }
}
