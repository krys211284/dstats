package krys.item;

import java.util.List;

/** Typ przedmiotu niezależny od slotu; identyfikatory techniczne są stabilnym kontraktem. */
public enum ItemType {
    UNKNOWN("UNKNOWN", null, false, ItemHandedness.UNKNOWN, List.of()),
    SHIELD("Tarcza", EquipmentSlot.OFF_HAND, false, ItemHandedness.NONE, List.of("Tarcza", "Shield")),
    HELMET("Hełm", EquipmentSlot.HELMET, false, ItemHandedness.NONE, List.of("Hełm", "Helmet", "Helm")),
    BOOTS("Buty", EquipmentSlot.BOOTS, false, ItemHandedness.NONE, List.of("Buty", "Boots")),
    // Dotychczasowy foundation Miecza i jego tempering mają semantykę broni jednoręcznej.
    SWORD("Miecz", EquipmentSlot.MAIN_HAND, true, ItemHandedness.ONE_HANDED, List.of("Miecz", "Sword")),
    SCYTHE_TWO_HANDED("Kosa dwuręczna", EquipmentSlot.MAIN_HAND, true, ItemHandedness.TWO_HANDED,
            List.of("Kosa dwuręczna")),
    // Typ wyłącznie migracyjny: dawny ogólny opis nie potwierdza liczby rąk ani konkretnej broni.
    LEGACY_MAIN_HAND("Broń główna", EquipmentSlot.MAIN_HAND, true, ItemHandedness.UNKNOWN,
            List.of("Broń główna", "Main hand"));

    private final String displayName;
    private final EquipmentSlot equipmentSlot;
    private final boolean weapon;
    private final ItemHandedness handedness;
    private final List<String> legacyAliases;

    ItemType(String displayName, EquipmentSlot equipmentSlot, boolean weapon,
             ItemHandedness handedness, List<String> legacyAliases) {
        this.displayName = displayName;
        this.equipmentSlot = equipmentSlot;
        this.weapon = weapon;
        this.handedness = handedness;
        this.legacyAliases = legacyAliases;
    }

    public String getTechnicalId() { return name(); }
    public String getDisplayName() { return displayName; }
    public EquipmentSlot getEquipmentSlot() { return equipmentSlot; }
    public boolean isWeapon() { return weapon; }
    public ItemHandedness getHandedness() { return handedness; }
    public List<String> getLegacyAliases() { return legacyAliases; }
    public boolean isSelectable() { return this != UNKNOWN && this != LEGACY_MAIN_HAND; }
}
