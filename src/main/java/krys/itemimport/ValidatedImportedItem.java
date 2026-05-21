package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.masterworking.ItemMasterworking;
import krys.tempering.ItemTemperingAffix;

import java.util.List;

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
    private final List<ImportedItemAffix> affixes;
    private final String selectedAspectId;
    private final ItemImportDetails details;
    private final List<ItemTemperingAffix> temperingAffixes;
    private final ItemMasterworking masterworking;

    public ValidatedImportedItem(String sourceImageName,
                                 EquipmentSlot slot,
                                 long weaponDamage,
                                 double strength,
                                 double intelligence,
                                 double thorns,
                                 double blockChance,
                                 double retributionChance) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance, List.of());
    }

    public ValidatedImportedItem(String sourceImageName,
                                 EquipmentSlot slot,
                                 long weaponDamage,
                                 double strength,
                                 double intelligence,
                                 double thorns,
                                 double blockChance,
                                 double retributionChance,
                                 List<ImportedItemAffix> affixes) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance, affixes, "");
    }

    public ValidatedImportedItem(String sourceImageName,
                                 EquipmentSlot slot,
                                 long weaponDamage,
                                 double strength,
                                 double intelligence,
                                 double thorns,
                                 double blockChance,
                                 double retributionChance,
                                 List<ImportedItemAffix> affixes,
                                 String selectedAspectId) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                affixes, selectedAspectId, ItemImportDetails.empty());
    }

    public ValidatedImportedItem(String sourceImageName,
                                 EquipmentSlot slot,
                                 long weaponDamage,
                                 double strength,
                                 double intelligence,
                                 double thorns,
                                 double blockChance,
                                 double retributionChance,
                                 List<ImportedItemAffix> affixes,
                                 String selectedAspectId,
                                 ItemImportDetails details) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                affixes, selectedAspectId, details, List.of());
    }

    public ValidatedImportedItem(String sourceImageName,
                                 EquipmentSlot slot,
                                 long weaponDamage,
                                 double strength,
                                 double intelligence,
                                 double thorns,
                                 double blockChance,
                                 double retributionChance,
                                 List<ImportedItemAffix> affixes,
                                 String selectedAspectId,
                                 ItemImportDetails details,
                                 List<ItemTemperingAffix> temperingAffixes) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                affixes, selectedAspectId, details, temperingAffixes, ItemMasterworking.defaultState());
    }

    public ValidatedImportedItem(String sourceImageName,
                                 EquipmentSlot slot,
                                 long weaponDamage,
                                 double strength,
                                 double intelligence,
                                 double thorns,
                                 double blockChance,
                                 double retributionChance,
                                 List<ImportedItemAffix> affixes,
                                 String selectedAspectId,
                                 ItemImportDetails details,
                                 List<ItemTemperingAffix> temperingAffixes,
                                 ItemMasterworking masterworking) {
        this.sourceImageName = sourceImageName == null || sourceImageName.isBlank() ? "item" : sourceImageName;
        this.slot = slot;
        this.weaponDamage = weaponDamage;
        this.strength = strength;
        this.intelligence = intelligence;
        this.thorns = thorns;
        this.blockChance = blockChance;
        this.retributionChance = retributionChance;
        this.affixes = affixes == null ? List.of() : List.copyOf(affixes);
        this.selectedAspectId = selectedAspectId == null ? "" : selectedAspectId;
        this.details = details == null ? ItemImportDetails.empty() : details;
        this.temperingAffixes = temperingAffixes == null ? List.of() : List.copyOf(temperingAffixes);
        this.masterworking = masterworking == null ? ItemMasterworking.defaultState() : masterworking;
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

    public List<ImportedItemAffix> getAffixes() {
        return affixes;
    }

    public String getSelectedAspectId() {
        return selectedAspectId;
    }

    public ItemImportDetails getDetails() {
        return details;
    }

    public List<ItemTemperingAffix> getTemperingAffixes() {
        return temperingAffixes;
    }

    public ItemMasterworking getMasterworking() {
        return masterworking;
    }

    public String getItemName() {
        return details.getItemName();
    }

    public String getItemType() {
        return details.getItemType();
    }

    public String getItemRarity() {
        return details.getItemRarity();
    }

    public boolean isAncient() {
        return details.isAncient();
    }

    public EquipmentSlot getEquipmentSlot() {
        return details.getEquipmentSlot();
    }

    public Long getItemPower() {
        return details.getItemPower();
    }

    public Long getWeaponDps() {
        return details.getWeaponDps();
    }

    public Long getWeaponDamageMin() {
        return details.getWeaponDamageMin();
    }

    public Long getWeaponDamageMax() {
        return details.getWeaponDamageMax();
    }

    public Long getAverageWeaponDamage() {
        return details.getAverageWeaponDamage();
    }

    public Double getAttacksPerSecond() {
        return details.getAttacksPerSecond();
    }

    public String getUniqueEffectText() {
        return details.getUniqueEffectText();
    }

    public Long getItemArmor() {
        return details.getItemArmor();
    }
}
