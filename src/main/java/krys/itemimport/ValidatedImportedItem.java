package krys.itemimport;

import krys.item.EquipmentSlot;

/** Zatwierdzony przez użytkownika pojedynczy item gotowy do mapowania do modelu aplikacji. */
public final class ValidatedImportedItem {
    private final String sourceImageName;
    private final EquipmentSlot slot;
    private final long weaponDamage;
    private final double strength;
    private final double intelligence;
    private final double thorns;
    private final double blockChance;
    private final double retributionChance;

    public ValidatedImportedItem(String sourceImageName,
                                 EquipmentSlot slot,
                                 long weaponDamage,
                                 double strength,
                                 double intelligence,
                                 double thorns,
                                 double blockChance,
                                 double retributionChance) {
        this.sourceImageName = sourceImageName == null || sourceImageName.isBlank() ? "item" : sourceImageName;
        this.slot = slot;
        this.weaponDamage = weaponDamage;
        this.strength = strength;
        this.intelligence = intelligence;
        this.thorns = thorns;
        this.blockChance = blockChance;
        this.retributionChance = retributionChance;
    }

    public String getSourceImageName() {
        return sourceImageName;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public long getWeaponDamage() {
        return weaponDamage;
    }

    public double getStrength() {
        return strength;
    }

    public double getIntelligence() {
        return intelligence;
    }

    public double getThorns() {
        return thorns;
    }

    public double getBlockChance() {
        return blockChance;
    }

    public double getRetributionChance() {
        return retributionChance;
    }
}
