package krys.transfiguration;

import krys.item.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;

/** Katalog affixów ranków umiejętności Przeistoczenia dla Paladyna. */
public final class TransfigurationSkillRankCatalog {
    private static final List<Entry> PALADIN_ENTRIES = List.of(
            new Entry(EquipmentSlot.HELMET, "Head", "Aura"),
            new Entry(EquipmentSlot.HELMET, "Head", "Valor"),
            new Entry(EquipmentSlot.CHEST, "Chest", "Aura"),
            new Entry(EquipmentSlot.CHEST, "Chest", "Justice"),
            new Entry(EquipmentSlot.GLOVES, "Gloves", "Core"),
            new Entry(EquipmentSlot.PANTS, "Pants", "Basic"),
            new Entry(EquipmentSlot.PANTS, "Pants", "Aura"),
            new Entry(EquipmentSlot.BOOTS, "Boots", "Aura"),
            new Entry(EquipmentSlot.BOOTS, "Boots", "Justice"),
            new Entry(EquipmentSlot.AMULET, "Amulet", "All Skills"),
            new Entry(EquipmentSlot.AMULET, "Amulet", "Core"),
            new Entry(EquipmentSlot.RING, "Rings", "All Skills"),
            new Entry(EquipmentSlot.RING, "Rings", "Valor"),
            new Entry(EquipmentSlot.MAIN_HAND, "1H Weapon", "Basic"),
            new Entry(EquipmentSlot.OFF_HAND, "Shield", "Basic"),
            new Entry(EquipmentSlot.OFF_HAND, "Shield", "Aura")
    );

    private TransfigurationSkillRankCatalog() {
    }

    public static List<Entry> paladinEntries() {
        return PALADIN_ENTRIES;
    }

    public static List<String> paladinTagsFor(EquipmentSlot slot) {
        List<String> tags = new ArrayList<>();
        for (Entry entry : PALADIN_ENTRIES) {
            if (entry.slot() == slot) {
                tags.add(entry.skillTag());
            }
        }
        return List.copyOf(tags);
    }

    public record Entry(EquipmentSlot slot, String sourceSlotLabel, String skillTag) {
    }
}
