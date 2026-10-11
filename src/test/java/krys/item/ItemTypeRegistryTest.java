package krys.item;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ItemTypeRegistryTest {
    @Test
    void catalogShouldContainOnlyConfirmedSelectableTypes() {
        assertEquals(List.of(ItemType.SHIELD, ItemType.HELMET, ItemType.BOOTS, ItemType.SWORD, ItemType.SCYTHE_TWO_HANDED), ItemTypeRegistry.selectableTypes());
        assertEquals(EquipmentSlot.MAIN_HAND, ItemType.SCYTHE_TWO_HANDED.getEquipmentSlot());
        assertTrue(ItemType.SCYTHE_TWO_HANDED.isWeapon());
        assertEquals(ItemHandedness.TWO_HANDED, ItemType.SCYTHE_TWO_HANDED.getHandedness());
        assertEquals(ItemHandedness.ONE_HANDED, ItemType.SWORD.getHandedness());
        assertEquals(ItemHandedness.NONE, ItemType.SHIELD.getHandedness());
        assertFalse(ItemType.SHIELD.isWeapon());
    }

    @Test
    void legacyNamesAndTechnicalIdsShouldResolveToTheSameDefinition() {
        for (ItemType type : ItemTypeRegistry.selectableTypes()) {
            assertSame(type, ItemTypeRegistry.resolve(type.getTechnicalId()));
            assertSame(type, ItemTypeRegistry.resolve(type.getDisplayName()));
        }
        assertEquals(List.of(ItemType.SCYTHE_TWO_HANDED), ItemTypeRegistry.matchingTypes("Starożytna unikatowa\nkosa dwureczna"));
    }

    @Test
    void unknownAndAmbiguousTypesShouldStayUnresolved() {
        for (String raw : List.of("", "Różdżka", "Kosa jednoręczna", "Miecz dwuręczny", "miecznik", "tarcza miecz", "Nieznana tarcza", "Zepsuty Helmet")) {
            assertEquals(ItemType.UNKNOWN, ItemTypeRegistry.resolve(raw), raw);
        }
        assertEquals("Nieznany dawny typ", ItemTypeRegistry.displayOrLegacy("Nieznany dawny typ"));
    }

    @Test
    void genericLegacyWeaponShouldNotInventHandednessOrBecomeANewFormOption() {
        assertEquals(ItemType.LEGACY_MAIN_HAND, ItemTypeRegistry.resolve("Main hand"));
        assertEquals(ItemHandedness.UNKNOWN, ItemType.LEGACY_MAIN_HAND.getHandedness());
        assertFalse(ItemType.LEGACY_MAIN_HAND.isSelectable());
    }
}
