package krys.itemknowledge;

import krys.item.EquipmentSlot;

/** Klucz grupowania obserwacji wiedzy o itemach. */
public record ItemKnowledgeKey(EquipmentSlot slot, String itemType) {
    public ItemKnowledgeKey {
        if (slot == null) {
            throw new IllegalArgumentException("Slot wiedzy o itemie jest wymagany.");
        }
        if (itemType == null || itemType.isBlank()) {
            itemType = slot.name();
        }
    }
}
