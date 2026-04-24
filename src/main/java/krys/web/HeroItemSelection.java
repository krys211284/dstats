package krys.web;

import krys.item.HeroEquipmentSlot;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Wybór jednego aktywnego itemu per slot dla konkretnego bohatera. */
public final class HeroItemSelection {
    private final Map<HeroEquipmentSlot, Long> selectedItemIdsBySlot;

    public HeroItemSelection(Map<HeroEquipmentSlot, Long> selectedItemIdsBySlot) {
        EnumMap<HeroEquipmentSlot, Long> copy = new EnumMap<>(HeroEquipmentSlot.class);
        if (selectedItemIdsBySlot != null) {
            for (Map.Entry<HeroEquipmentSlot, Long> entry : selectedItemIdsBySlot.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0L) {
                    continue;
                }
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        this.selectedItemIdsBySlot = Collections.unmodifiableMap(copy);
    }

    public static HeroItemSelection empty() {
        return new HeroItemSelection(Map.of());
    }

    public Map<HeroEquipmentSlot, Long> getSelectedItemIdsBySlot() {
        return selectedItemIdsBySlot;
    }

    public Long getSelectedItemId(HeroEquipmentSlot slot) {
        return selectedItemIdsBySlot.get(slot);
    }

    public boolean isSelected(HeroEquipmentSlot slot, long itemId) {
        Long selectedItemId = selectedItemIdsBySlot.get(slot);
        return selectedItemId != null && selectedItemId == itemId;
    }

    public HeroItemSelection withSelectedItem(HeroEquipmentSlot slot, long itemId) {
        EnumMap<HeroEquipmentSlot, Long> updated = new EnumMap<>(HeroEquipmentSlot.class);
        updated.putAll(selectedItemIdsBySlot);
        updated.put(slot, itemId);
        return new HeroItemSelection(updated);
    }

    public HeroItemSelection withoutItemId(long itemId) {
        EnumMap<HeroEquipmentSlot, Long> updated = new EnumMap<>(HeroEquipmentSlot.class);
        updated.putAll(selectedItemIdsBySlot);
        updated.entrySet().removeIf(entry -> entry.getValue() == itemId);
        return new HeroItemSelection(updated);
    }

    public HeroItemSelection withoutSlot(HeroEquipmentSlot slot) {
        EnumMap<HeroEquipmentSlot, Long> updated = new EnumMap<>(HeroEquipmentSlot.class);
        updated.putAll(selectedItemIdsBySlot);
        updated.remove(slot);
        return new HeroItemSelection(updated);
    }
}
