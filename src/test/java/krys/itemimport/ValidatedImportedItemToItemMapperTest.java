package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.item.Item;
import krys.item.ItemStatType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Test mapowania zatwierdzonego itemu do aktualnego modelu itemu używanego przez runtime. */
class ValidatedImportedItemToItemMapperTest {
    @Test
    void shouldMapValidatedImportedItemToRuntimeItem() {
        ValidatedImportedItem importedItem = new ValidatedImportedItem(
                "amulet.png",
                EquipmentSlot.RING,
                0L,
                14.0d,
                9.0d,
                40.0d,
                12.0d,
                18.0d
        );

        Item item = new ValidatedImportedItemToItemMapper().map(importedItem);

        assertEquals(EquipmentSlot.RING, item.getSlot());
        assertEquals("Zaimportowany item: amulet.png", item.getName());
        assertEquals(5, item.getStats().size());
        assertEquals(ItemStatType.STRENGTH, item.getStats().get(0).getType());
        assertEquals(14.0d, item.getStats().get(0).getValue());
        assertEquals(ItemStatType.RETRIBUTION_CHANCE, item.getStats().get(4).getType());
        assertEquals(18.0d, item.getStats().get(4).getValue());
    }
}
