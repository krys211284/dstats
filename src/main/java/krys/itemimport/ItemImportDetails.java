package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.item.ItemType;
import krys.item.ItemTypeRegistry;

import java.util.Locale;
import java.util.Optional;

/** Strukturalne dane itemu zapisane albo poprawione ręcznie przed zapisem. */
public final class ItemImportDetails {
    private final String itemName;
    private final String itemType;
    private final ItemType resolvedItemType;
    private final String itemRarity;
    private final boolean ancient;
    private final EquipmentSlot equipmentSlot;
    private final Long itemPower;
    private final Long weaponDps;
    private final Long weaponDamageMin;
    private final Long weaponDamageMax;
    private final Long averageWeaponDamage;
    private final Double attacksPerSecond;
    private final WeaponBaseStatConsistencyResolver.Result weaponBaseStatConsistency;
    private final Long itemArmor;
    private final String uniqueEffectText;
    private final boolean mythicUnique;

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
        this(itemName, itemType, itemRarity, ancient, equipmentSlot, itemPower, weaponDps,
                weaponDamageMin, weaponDamageMax, averageWeaponDamage, attacksPerSecond, itemArmor, uniqueEffectText, false);
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
                             String uniqueEffectText,
                             boolean mythicUnique) {
        this(itemName, itemType, itemRarity, ancient, equipmentSlot, itemPower, weaponDps,
                weaponDamageMin, weaponDamageMax, averageWeaponDamage, attacksPerSecond, itemArmor,
                uniqueEffectText, mythicUnique, WeaponBaseStatConsistencyResolver.Certainty.confirmed());
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
                             String uniqueEffectText,
                             boolean mythicUnique,
                             WeaponBaseStatConsistencyResolver.Certainty weaponStatCertainty) {
        this.itemName = normalize(itemName);
        this.resolvedItemType = ItemTypeRegistry.resolve(itemType);
        this.itemType = resolvedItemType == ItemType.UNKNOWN ? normalize(itemType) : resolvedItemType.getDisplayName();
        this.itemRarity = normalize(itemRarity);
        this.ancient = ancient;
        this.equipmentSlot = equipmentSlot;
        this.itemPower = nonNegativeOrNull("Moc przedmiotu", itemPower);
        this.weaponDamageMin = nonNegativeOrNull("Minimalne obrażenia broni", weaponDamageMin);
        this.weaponDamageMax = nonNegativeOrNull("Maksymalne obrażenia broni", weaponDamageMax);
        this.attacksPerSecond = nonNegativeOrNull("Ataki na sekundę", attacksPerSecond);
        Long suppliedAverage = nonNegativeOrNull("Średnie obrażenia broni", averageWeaponDamage);
        this.weaponBaseStatConsistency = WeaponBaseStatConsistencyResolver.resolve(
                this.weaponDamageMin, this.weaponDamageMax, this.attacksPerSecond,
                nonNegativeOrNull("DPS broni", weaponDps), weaponStatCertainty);
        this.weaponDps = weaponBaseStatConsistency.canonicalDps();
        this.averageWeaponDamage = weaponBaseStatConsistency.canonicalAverage() == null
                ? suppliedAverage : weaponBaseStatConsistency.canonicalAverage();
        this.itemArmor = nonNegativeOrNull("Pancerz", itemArmor);
        this.uniqueEffectText = normalizeFortifyLegendaryEffect(uniqueEffectText)
                .orElse(EffectTextTokenNormalizer.normalizeMultiplierTokens(normalize(uniqueEffectText)));
        this.mythicUnique = mythicUnique;
    }

    public static ItemImportDetails empty() {
        return new ItemImportDetails("", "", "", false, null, null, null, null, null, null, null, "");
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

    public ItemType getResolvedItemType() { return resolvedItemType; }

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

    public WeaponBaseStatConsistencyResolver.Result getWeaponBaseStatConsistency() {
        return weaponBaseStatConsistency;
    }

    public String getUniqueEffectText() {
        return uniqueEffectText;
    }

    public Long getItemArmor() {
        return itemArmor;
    }

    public boolean isMythicUnique() {
        return mythicUnique;
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
                || mythicUnique
                || !uniqueEffectText.isBlank();
    }

    public String getRarityOrUnknown() {
        return itemRarity.isBlank() ? "UNKNOWN" : itemRarity.toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static Optional<String> normalizeFortifyLegendaryEffect(String text) {
        return FortifyLegendaryEffectNormalizer.normalize(text);
    }
}
