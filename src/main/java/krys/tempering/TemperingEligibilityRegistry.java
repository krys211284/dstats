package krys.tempering;

import krys.item.EquipmentSlot;
import krys.item.ItemType;
import krys.item.ItemTypeRegistry;

import java.util.List;

/** Macierz potwierdzonej dostępności kategorii hartowania per typ/slot itemu. */
public final class TemperingEligibilityRegistry {
    private TemperingEligibilityRegistry() {
    }

    public static List<TemperingCategory> availableCategories(EquipmentSlot slot, String itemType) {
        return availableCategories(slot, ItemTypeRegistry.resolve(itemType));
    }

    public static List<TemperingCategory> availableCategories(EquipmentSlot slot, ItemType itemType) {
        if (slot == EquipmentSlot.OFF_HAND && itemType == ItemType.SHIELD) {
            return List.of(
                    TemperingCategory.WEAPON,
                    TemperingCategory.OFFENSE,
                    TemperingCategory.DEFENSE,
                    TemperingCategory.UTILITY
            );
        }
        if (slot == EquipmentSlot.MAIN_HAND && (itemType == ItemType.SWORD || itemType == ItemType.LEGACY_MAIN_HAND)) {
            return List.of(
                    TemperingCategory.WEAPON,
                    TemperingCategory.OFFENSE
            );
        }
        if (slot == EquipmentSlot.HELMET || itemType == ItemType.HELMET) {
            return List.of(TemperingCategory.DEFENSE);
        }
        return List.of();
    }

    public static boolean isCategoryAvailable(EquipmentSlot slot, String itemType, TemperingCategory category) {
        return availableCategories(slot, itemType).contains(category);
    }

    public static boolean isCategoryAvailable(EquipmentSlot slot, ItemType itemType, TemperingCategory category) {
        return availableCategories(slot, itemType).contains(category);
    }

}
