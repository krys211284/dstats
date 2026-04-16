package krys.itemimport;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy walidacji ręcznie poprawionego itemu przed zatwierdzeniem do modelu aplikacji. */
class ItemImportFormMapperTest {
    @Test
    void shouldValidateManuallyCorrectedMainHandItem() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "topor.png",
                "MAIN_HAND",
                "444",
                "70",
                "0",
                "0",
                "0",
                "0"
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(EquipmentSlot.MAIN_HAND, result.getItem().getSlot());
        assertEquals(444L, result.getItem().getWeaponDamage());
        assertEquals(70.0d, result.getItem().getStrength());
    }

    @Test
    void shouldRejectWeaponDamageOutsideMainHandSlot() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "150",
                "12",
                "0",
                "30",
                "18",
                "25"
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertNull(result.getItem());
        assertEquals(1, result.getErrors().size());
        assertEquals("Weapon damage można ustawić wyłącznie dla slotu MAIN_HAND.", result.getErrors().getFirst());
    }
}
