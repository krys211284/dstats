package krys.web;

import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemlibrary.SavedImportedItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje semantyczny render zapisanych itemów w bibliotece. */
class ItemLibraryPageRendererTest {
    @Test
    void shouldRenderShieldItemInSemanticSectionsWithoutFlatOcrDump() {
        SavedImportedItem shield = new SavedImportedItem(
                1L,
                "Ręka dodatkowa / tarcza.png",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                114.0d,
                0.0d,
                494.0d,
                20.0d,
                0.0d,
                new FullItemRead(
                        "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU",
                        "Starożytna legendarna tarcza",
                        "Starożytna legendarna",
                        "Moc przedmiotu: 800",
                        "1 131 pkt. pancerza",
                        List.of(
                                new FullItemReadLine(FullItemReadLineType.ITEM_NAME, "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU"),
                                new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Starożytna legendarna tarcza"),
                                new FullItemReadLine(FullItemReadLineType.ITEM_POWER, "Moc przedmiotu: 800"),
                                new FullItemReadLine(FullItemReadLineType.BASE_STAT, "1 131 pkt. pancerza"),
                                new FullItemReadLine(FullItemReadLineType.IMPLICIT, "45% redukcji blokowanych obrażeń [45]%"),
                                new FullItemReadLine(FullItemReadLineType.IMPLICIT, "20,0% szansy na blok [20,0]%"),
                                new FullItemReadLine(FullItemReadLineType.IMPLICIT, "+100% obrażeń od broni w głównej ręce [100]%"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+494 cierni [473 - 506]"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+7,0% szansy na szczęśliwy traf [7,0"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "13,2% redukcji czasu odnowienia"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 siły [107 - 121]"),
                                new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%"),
                                new FullItemReadLine(FullItemReadLineType.SOCKET, "Puste gniazdo")
                        )
                ),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.THORNS, 494.0d, "", false, 0, "+494 cierni [473 - 506]", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_CHANCE, 7.0d, "%", false, 1, "+7,0% szansy na szczęśliwy traf [7,0", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.COOLDOWN_REDUCTION, 13.2d, "%", true, 2, "13,2% redukcji czasu odnowienia", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 114.0d, "", false, 3, "+114 siły [107 - 121]", ImportedItemAffixSource.OCR)
                ),
                "inner-calm"
        );

        String html = render(List.of(shield));

        assertTrue(html.contains("Dane podstawowe"));
        assertTrue(html.contains("Base stats"));
        assertTrue(html.contains("Implicit / linie bazowe"));
        assertTrue(html.contains("Affixy"));
        assertTrue(html.contains("Aspekt / efekt legendarny"));
        assertTrue(html.contains("Socket / gniazdo"));
        assertTrue(html.contains("1 131 pkt. pancerza"));
        assertTrue(html.contains("45% redukcji blokowanych obrażeń [45]%"));
        assertTrue(html.contains("20,0% szansy na blok [20,0]%"));
        assertTrue(html.contains("+100% obrażeń od broni w głównej ręce [100]%"));
        assertTrue(html.contains("+494 cierni [473 - 506]"));
        assertTrue(html.contains("+7,0% szansy na szczęśliwy traf [7,0"));
        assertTrue(html.contains("* 13,2% redukcji czasu odnowienia"));
        assertTrue(html.contains("+114 siły [107 - 121]"));
        assertTrue(html.contains("Wybrany aspekt: Aspekt Wewnętrznego Spokoju"));
        assertFalse(html.contains("Affix: 45% redukcji blokowanych obrażeń"));
        assertFalse(html.contains("Affix: 20,0% szansy na blok"));
        assertFalse(html.contains("Affix: +100% obrażeń od broni w głównej ręce"));
        assertFalse(html.contains("Bazowa wartość: 800 1 131 pkt. pancerza"));
        assertFalse(html.contains("Projekcja do aktualnego runtime"));
        assertFalse(html.contains("Łączne obrażenia"));
        assertFalse(html.contains("<h2>DPS</h2>"));
        assertEquals(1, countOccurrences(html, ">Moc przedmiotu<"));
    }

    @Test
    void shouldRenderBootItemInTheSameSemanticSections() {
        SavedImportedItem boots = new SavedImportedItem(
                1L,
                "Buty / buty.png",
                "buty.png",
                EquipmentSlot.BOOTS,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                new FullItemRead(
                        "Marsz Pokutnika",
                        "Buty",
                        "Rzadki przedmiot",
                        "800 mocy przedmiotu",
                        "354 pkt. pancerza",
                        List.of(
                                new FullItemReadLine(FullItemReadLineType.ITEM_NAME, "Marsz Pokutnika"),
                                new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Buty"),
                                new FullItemReadLine(FullItemReadLineType.ITEM_POWER, "800 mocy przedmiotu"),
                                new FullItemReadLine(FullItemReadLineType.BASE_STAT, "354 pkt. pancerza"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+12,5% szybkości ruchu"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+7,0% uniku"),
                                new FullItemReadLine(FullItemReadLineType.SOCKET, "2 gniazda")
                        )
                ),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.MOVEMENT_SPEED, 12.5d, "%", true, 0, "+12,5% szybkości ruchu", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.DODGE_CHANCE, 7.0d, "%", true, 1, "+7,0% uniku", ImportedItemAffixSource.OCR)
                ),
                ""
        );

        String html = render(List.of(boots));

        assertTrue(html.contains("Base stats"));
        assertTrue(html.contains("354 pkt. pancerza"));
        assertTrue(html.contains("Affixy"));
        assertTrue(html.contains("* +12,5% szybkości ruchu"));
        assertTrue(html.contains("* +7,0% uniku"));
        assertTrue(html.contains("Socket / gniazdo"));
        assertTrue(html.contains("2 gniazda"));
        assertFalse(html.contains("Affix: 354 pkt. pancerza"));
        assertFalse(html.contains("Projekcja do aktualnego runtime"));
    }

    private static String render(List<SavedImportedItem> items) {
        return new ItemLibraryPageRenderer().render(new ItemLibraryPageModel(
                items,
                null,
                HeroItemSelection.empty(),
                List.of(),
                List.of(),
                "",
                null
        ));
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = value.indexOf(needle);
        while (index >= 0) {
            count++;
            index = value.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
