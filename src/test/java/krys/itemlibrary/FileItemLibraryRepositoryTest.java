package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje trwały zapis minimalnej biblioteki itemów bez bazy danych. */
class FileItemLibraryRepositoryTest {
    @Test
    void shouldPersistSavedItemsAndSelectionOnDisk() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-repo");
        FileItemLibraryRepository repository = new FileItemLibraryRepository(tempDirectory);

        SavedImportedItem firstItem = repository.save(new SavedImportedItem(
                0L,
                "OFF_HAND / tarcza-a.png",
                "tarcza-a.png",
                EquipmentSlot.OFF_HAND,
                0L,
                114.0d,
                0.0d,
                494.0d,
                20.0d,
                0.0d,
                new FullItemRead(
                        "Nieugaszony Bastion",
                        "Tarcza",
                        "Legendarny",
                        "800 mocy przedmiotu",
                        "1 131 pkt. pancerza",
                        List.of(new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 do siły [107 - 121]"))
                ),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 114.0d, "", true, 0, "* +114 do siły [107 - 121]", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.THORNS, 494.0d, "+494 cierni [473 - 506]")
                ),
                "inner-calm"
        ));
        SavedImportedItem secondItem = repository.save(new SavedImportedItem(
                0L,
                "OFF_HAND / tarcza-b.png",
                "tarcza-b.png",
                EquipmentSlot.OFF_HAND,
                0L,
                120.0d,
                0.0d,
                500.0d,
                22.0d,
                0.0d
        ));
        repository.saveSelection(new ActiveItemSelection(Map.of(EquipmentSlot.OFF_HAND, secondItem.getItemId())));

        FileItemLibraryRepository reloadedRepository = new FileItemLibraryRepository(tempDirectory);

        List<SavedImportedItem> savedItems = reloadedRepository.findAll();
        assertEquals(2, savedItems.size());
        assertEquals(firstItem.getItemId(), savedItems.get(0).getItemId());
        assertEquals(secondItem.getItemId(), savedItems.get(1).getItemId());
        assertEquals(EquipmentSlot.OFF_HAND, savedItems.get(0).getSlot());
        assertEquals("Nieugaszony Bastion", savedItems.get(0).getFullItemRead().getItemName());
        assertEquals("1 131 pkt. pancerza", savedItems.get(0).getFullItemRead().getBaseItemValue());
        assertEquals("+114 do siły [107 - 121]", savedItems.get(0).getFullItemRead().getLines().getFirst().getText());
        assertEquals(2, savedItems.get(0).getAffixes().size());
        assertEquals(ImportedItemAffixType.STRENGTH, savedItems.get(0).getAffixes().getFirst().getType());
        assertEquals(114.0d, savedItems.get(0).getAffixes().getFirst().getValue());
        assertEquals("* +114 do siły [107 - 121]", savedItems.get(0).getAffixes().getFirst().getSourceText());
        assertTrue(savedItems.get(0).getAffixes().getFirst().isGreaterAffix());
        assertEquals("inner-calm", savedItems.get(0).getSelectedAspectId());
        assertTrue(reloadedRepository.findById(secondItem.getItemId()).isPresent());
        assertEquals(secondItem.getItemId(), reloadedRepository.loadSelection().getSelectedItemId(EquipmentSlot.OFF_HAND));
    }
}
