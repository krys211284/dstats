package krys.itemlibrary;

import krys.item.HeroEquipmentSlot;

/** Przypisanie zapisanego itemu z biblioteki do konkretnego slotu bohatera. */
public final class HeroSlotItemAssignment {
    private final HeroEquipmentSlot heroSlot;
    private final SavedImportedItem item;

    public HeroSlotItemAssignment(HeroEquipmentSlot heroSlot, SavedImportedItem item) {
        if (heroSlot == null) {
            throw new IllegalArgumentException("Slot bohatera jest wymagany.");
        }
        if (item == null) {
            throw new IllegalArgumentException("Item przypisany do slotu bohatera jest wymagany.");
        }
        if (!heroSlot.supports(item.getSlot())) {
            throw new IllegalArgumentException("Item biblioteki nie pasuje do slotu bohatera.");
        }
        this.heroSlot = heroSlot;
        this.item = item;
    }

    public HeroEquipmentSlot getHeroSlot() {
        return heroSlot;
    }

    public SavedImportedItem getItem() {
        return item;
    }
}
