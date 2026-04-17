package krys.itemlibrary;

import krys.item.EquipmentSlot;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Wybór jednego aktywnego itemu per slot w minimalnej bibliotece itemów. */
public final class ActiveItemSelection {
    private final Map<EquipmentSlot, Long> selectedItemIdsBySlot;

    public ActiveItemSelection(Map<EquipmentSlot, Long> selectedItemIdsBySlot) {
        EnumMap<EquipmentSlot, Long> copy = new EnumMap<>(EquipmentSlot.class);
        if (selectedItemIdsBySlot != null) {
            for (Map.Entry<EquipmentSlot, Long> entry : selectedItemIdsBySlot.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0L) {
                    continue;
                }
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        this.selectedItemIdsBySlot = Collections.unmodifiableMap(copy);
    }

    public static ActiveItemSelection empty() {
        return new ActiveItemSelection(Map.of());
    }

    public Map<EquipmentSlot, Long> getSelectedItemIdsBySlot() {
        return selectedItemIdsBySlot;
    }

    public Long getSelectedItemId(EquipmentSlot slot) {
        return selectedItemIdsBySlot.get(slot);
    }

    public boolean isSelected(EquipmentSlot slot, long itemId) {
        Long selectedItemId = selectedItemIdsBySlot.get(slot);
        return selectedItemId != null && selectedItemId == itemId;
    }

    public ActiveItemSelection withSelectedItem(EquipmentSlot slot, long itemId) {
        EnumMap<EquipmentSlot, Long> updated = new EnumMap<>(EquipmentSlot.class);
        updated.putAll(selectedItemIdsBySlot);
        updated.put(slot, itemId);
        return new ActiveItemSelection(updated);
    }

    public ActiveItemSelection withoutItemId(long itemId) {
        EnumMap<EquipmentSlot, Long> updated = new EnumMap<>(EquipmentSlot.class);
        updated.putAll(selectedItemIdsBySlot);
        updated.entrySet().removeIf(entry -> entry.getValue() == itemId);
        return new ActiveItemSelection(updated);
    }
}
