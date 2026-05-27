package krys.tempering;

import krys.item.EquipmentSlot;

import java.util.List;

/** Macierz potwierdzonej dostępności kategorii hartowania per typ/slot itemu. */
public final class TemperingEligibilityRegistry {
    private TemperingEligibilityRegistry() {
    }

    public static List<TemperingCategory> availableCategories(EquipmentSlot slot, String itemType) {
        if (isShield(slot, itemType)) {
            return List.of(
                    TemperingCategory.WEAPON,
                    TemperingCategory.OFFENSE,
                    TemperingCategory.DEFENSE,
                    TemperingCategory.UTILITY
            );
        }
        if (isOneHandWeapon(slot, itemType)) {
            return List.of(
                    TemperingCategory.WEAPON,
                    TemperingCategory.OFFENSE
            );
        }
        if (isHelmet(slot, itemType)) {
            return List.of(TemperingCategory.DEFENSE);
        }
        return List.of();
    }

    public static boolean isCategoryAvailable(EquipmentSlot slot, String itemType, TemperingCategory category) {
        return availableCategories(slot, itemType).contains(category);
    }

    private static boolean isShield(EquipmentSlot slot, String itemType) {
        return slot == EquipmentSlot.OFF_HAND && normalize(itemType).contains("TARCZA");
    }

    private static boolean isOneHandWeapon(EquipmentSlot slot, String itemType) {
        String normalizedType = normalize(itemType);
        return slot == EquipmentSlot.MAIN_HAND
                && (normalizedType.contains("MIECZ")
                || normalizedType.contains("SWORD")
                || normalizedType.contains("BRON GLOWNA")
                || normalizedType.contains("MAIN HAND"));
    }

    private static boolean isHelmet(EquipmentSlot slot, String itemType) {
        String normalizedType = normalize(itemType);
        return slot == EquipmentSlot.HELMET
                || normalizedType.contains("HELM")
                || normalizedType.contains("HELMET");
    }

    private static String normalize(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(java.util.Locale.ROOT);
    }
}
