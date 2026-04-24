package krys.item;

import java.util.ArrayList;
import java.util.List;

/** Stałe sloty ekwipunku bohatera widoczne na ekranie aktualnego buildu. */
public enum HeroEquipmentSlot {
    HELMET("Hełm", EquipmentSlot.HELMET),
    CHEST("Zbroja", EquipmentSlot.CHEST),
    GLOVES("Rękawice", EquipmentSlot.GLOVES),
    PANTS("Spodnie", EquipmentSlot.PANTS),
    BOOTS("Buty", EquipmentSlot.BOOTS),
    MAIN_HAND("Broń", EquipmentSlot.MAIN_HAND),
    AMULET("Amulet", EquipmentSlot.AMULET),
    RING_LEFT("Pierścień 1", EquipmentSlot.RING),
    RING_RIGHT("Pierścień 2", EquipmentSlot.RING),
    OFF_HAND("Tarcza", EquipmentSlot.OFF_HAND);

    private final String displayName;
    private final EquipmentSlot compatibleItemSlot;

    HeroEquipmentSlot(String displayName, EquipmentSlot compatibleItemSlot) {
        this.displayName = displayName;
        this.compatibleItemSlot = compatibleItemSlot;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EquipmentSlot getCompatibleItemSlot() {
        return compatibleItemSlot;
    }

    public boolean supports(EquipmentSlot itemSlot) {
        return compatibleItemSlot == itemSlot;
    }

    public static List<HeroEquipmentSlot> compatibleWith(EquipmentSlot itemSlot) {
        List<HeroEquipmentSlot> slots = new ArrayList<>();
        for (HeroEquipmentSlot heroSlot : values()) {
            if (heroSlot.supports(itemSlot)) {
                slots.add(heroSlot);
            }
        }
        return List.copyOf(slots);
    }
}
