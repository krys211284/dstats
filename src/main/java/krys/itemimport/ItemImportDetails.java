package krys.itemimport;

import krys.item.EquipmentSlot;

import java.util.Locale;

/** Strukturalne dane itemu odczytane z OCR albo poprawione ręcznie przed zapisem. */
public final class ItemImportDetails {
    private final String itemName;
    private final String itemType;
    private final String itemRarity;
    private final boolean ancient;
    private final EquipmentSlot equipmentSlot;
    private final Long itemPower;
    private final Long weaponDps;
    private final Long weaponDamageMin;
    private final Long weaponDamageMax;
    private final Long averageWeaponDamage;
    private final Double attacksPerSecond;
    private final Long itemArmor;
    private final String uniqueEffectText;

    public ItemImportDetails(String itemName,
                             String itemType,
                             String itemRarity,
                             boolean ancient,
                             EquipmentSlot equipmentSlot,
                             Long itemPower,
                             Long weaponDps,
                             Long weaponDamageMin,
                             Long weaponDamageMax,
                             Long averageWeaponDamage,
                             Double attacksPerSecond,
                             String uniqueEffectText) {
        this(itemName, itemType, itemRarity, ancient, equipmentSlot, itemPower, weaponDps,
                weaponDamageMin, weaponDamageMax, averageWeaponDamage, attacksPerSecond, null, uniqueEffectText);
    }

    public ItemImportDetails(String itemName,
                             String itemType,
                             String itemRarity,
                             boolean ancient,
                             EquipmentSlot equipmentSlot,
                             Long itemPower,
                             Long weaponDps,
                             Long weaponDamageMin,
                             Long weaponDamageMax,
                             Long averageWeaponDamage,
                             Double attacksPerSecond,
                             Long itemArmor,
                             String uniqueEffectText) {
        this.itemName = normalize(itemName);
        this.itemType = normalize(itemType);
        this.itemRarity = normalize(itemRarity);
        this.ancient = ancient;
        this.equipmentSlot = equipmentSlot;
        this.itemPower = nonNegativeOrNull("Moc przedmiotu", itemPower);
        this.weaponDps = nonNegativeOrNull("DPS broni", weaponDps);
        this.weaponDamageMin = nonNegativeOrNull("Minimalne obrażenia broni", weaponDamageMin);
        this.weaponDamageMax = nonNegativeOrNull("Maksymalne obrażenia broni", weaponDamageMax);
        this.averageWeaponDamage = nonNegativeOrNull("Średnie obrażenia broni",
                averageWeaponDamage == null ? calculateAverage(weaponDamageMin, weaponDamageMax) : averageWeaponDamage);
        this.attacksPerSecond = nonNegativeOrNull("Ataki na sekundę", attacksPerSecond);
        this.itemArmor = nonNegativeOrNull("Pancerz", itemArmor);
        this.uniqueEffectText = normalize(uniqueEffectText);
    }

    public static ItemImportDetails empty() {
        return new ItemImportDetails("", "", "", false, null, null, null, null, null, null, null, "");
    }

    private static Long calculateAverage(Long min, Long max) {
        if (min == null || max == null) {
            return null;
        }
        return Math.round((min + max) / 2.0d);
    }

    private static Long nonNegativeOrNull(String label, Long value) {
        if (value != null && value < 0L) {
            throw new IllegalArgumentException(label + " nie może być ujemna.");
        }
        return value;
    }

    private static Double nonNegativeOrNull(String label, Double value) {
        if (value != null && value < 0.0d) {
            throw new IllegalArgumentException(label + " nie może być ujemne.");
        }
        return value;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public String getItemRarity() {
        return itemRarity;
    }

    public boolean isAncient() {
        return ancient;
    }

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    public Long getItemPower() {
        return itemPower;
    }

    public Long getWeaponDps() {
        return weaponDps;
    }

    public Long getWeaponDamageMin() {
        return weaponDamageMin;
    }

    public Long getWeaponDamageMax() {
        return weaponDamageMax;
    }

    public Long getAverageWeaponDamage() {
        return averageWeaponDamage;
    }

    public Double getAttacksPerSecond() {
        return attacksPerSecond;
    }

    public String getUniqueEffectText() {
        return uniqueEffectText;
    }

    public Long getItemArmor() {
        return itemArmor;
    }

    public boolean hasAnyData() {
        return !itemName.isBlank()
                || !itemType.isBlank()
                || !itemRarity.isBlank()
                || ancient
                || equipmentSlot != null
                || itemPower != null
                || weaponDps != null
                || weaponDamageMin != null
                || weaponDamageMax != null
                || averageWeaponDamage != null
                || attacksPerSecond != null
                || itemArmor != null
                || !uniqueEffectText.isBlank();
    }

    public String getRarityOrUnknown() {
        return itemRarity.isBlank() ? "UNKNOWN" : itemRarity.toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
