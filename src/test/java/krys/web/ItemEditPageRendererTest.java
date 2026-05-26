package krys.web;

import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportFieldConfidence;
import krys.itemlibrary.ItemLibraryFilter;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.ItemMasterworking;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje formularz edycji itemu w bibliotece. */
class ItemEditPageRendererTest {
    @Test
    void shouldRenderNewOffensiveAspectsInEditSelect() {
        SavedImportedItem item = new SavedImportedItem(
                1L,
                "Tarcza",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        );
        ItemImportEditableForm form = new ItemImportEditableForm(
                item.getSourceImageName(),
                item.getSlot().name(),
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "sanctified_punishment_aspect"
        );

        String html = new ItemEditPageRenderer().render(new ItemEditPageModel(
                item,
                form,
                List.of(),
                List.of(),
                ItemLibraryFilter.empty()
        ));

        assertTrue(html.contains("Aspekt Uświęconej Kary"));
        assertTrue(html.contains("selected"));
        assertTrue(html.contains("Obrażenia Świętości i Ognia są zwiększone o 60,0%[x] [40,0 - 60,0]%."));
    }

    @Test
    void shouldRenderSingleMasterworkingQualityFieldInEditForm() {
        SavedImportedItem item = new SavedImportedItem(
                1L,
                "Tarcza",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        );
        ItemImportEditableForm form = new ItemImportEditableForm(
                item.getSourceImageName(),
                item.getSlot().name(),
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                new ItemImportDetails("Tarcza", "Tarcza", "LEGENDARY", true, EquipmentSlot.OFF_HAND,
                        900L, null, null, null, null, null, 1202L, ""),
                List.of(),
                ItemMasterworking.quality(25)
        );

        String html = new ItemEditPageRenderer().render(new ItemEditPageModel(
                item,
                form,
                List.of(),
                List.of(),
                ItemLibraryFilter.empty()
        ));

        assertTrue(html.contains("name=\"masterworkingQualityCurrent\""));
        assertTrue(html.contains("<option value=\"25\" selected>25/25</option>"));
        assertFalse(html.contains("Jakość maksymalna"));
        assertFalse(html.contains("name=\"masterworkingQualityMax\""));
    }
}
