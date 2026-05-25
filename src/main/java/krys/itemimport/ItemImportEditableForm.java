package krys.itemimport;

import krys.masterworking.ItemMasterworking;
import krys.socketing.ItemSocketing;
import krys.tempering.ItemTemperingAffix;
import krys.transfiguration.ItemTransfiguration;

import java.util.List;

/** Edytowalny formularz itemu wypełniany przez użytkownika po wstępnym odczycie obrazu. */
public final class ItemImportEditableForm {
    private final String sourceImageName;
    private final String slot;
    private final String weaponDamage;
    private final String strength;
    private final String intelligence;
    private final String thorns;
    private final String blockChance;
    private final String retributionChance;
    private final FullItemRead fullItemRead;
    private final List<ImportedItemAffix> affixes;
    private final String ocrSuggestedAspectId;
    private final ItemImportFieldConfidence ocrAspectConfidence;
    private final String selectedAspectId;
    private final ItemImportDetails details;
    private final List<ItemTemperingAffix> temperingAffixes;
    private final ItemMasterworking masterworking;
    private final ItemTransfiguration transfiguration;
    private final ItemSocketing socketing;

    public ItemImportEditableForm(String sourceImageName,
                                  String slot,
                                  String weaponDamage,
                                  String strength,
                                  String intelligence,
                                  String thorns,
                                  String blockChance,
                                  String retributionChance) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance, FullItemRead.empty());
    }

    public ItemImportEditableForm(String sourceImageName,
                                  String slot,
                                  String weaponDamage,
                                  String strength,
                                  String intelligence,
                                  String thorns,
                                  String blockChance,
                                  String retributionChance,
                                  FullItemRead fullItemRead) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance, fullItemRead, List.of());
    }

    public ItemImportEditableForm(String sourceImageName,
                                  String slot,
                                  String weaponDamage,
                                  String strength,
                                  String intelligence,
                                  String thorns,
                                  String blockChance,
                                  String retributionChance,
                                  FullItemRead fullItemRead,
                                  List<ImportedItemAffix> affixes) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                fullItemRead, affixes, "", ItemImportFieldConfidence.UNKNOWN, "",
                fullItemRead == null ? ItemImportDetails.empty() : fullItemRead.getDetails(), List.of());
    }

    public ItemImportEditableForm(String sourceImageName,
                                  String slot,
                                  String weaponDamage,
                                  String strength,
                                  String intelligence,
                                  String thorns,
                                  String blockChance,
                                  String retributionChance,
                                  FullItemRead fullItemRead,
                                  List<ImportedItemAffix> affixes,
                                  String ocrSuggestedAspectId,
                                  ItemImportFieldConfidence ocrAspectConfidence,
                                  String selectedAspectId) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                fullItemRead, affixes, ocrSuggestedAspectId, ocrAspectConfidence, selectedAspectId,
                fullItemRead == null ? ItemImportDetails.empty() : fullItemRead.getDetails(), List.of());
    }

    public ItemImportEditableForm(String sourceImageName,
                                  String slot,
                                  String weaponDamage,
                                  String strength,
                                  String intelligence,
                                  String thorns,
                                  String blockChance,
                                  String retributionChance,
                                  FullItemRead fullItemRead,
                                  List<ImportedItemAffix> affixes,
                                  String ocrSuggestedAspectId,
                                  ItemImportFieldConfidence ocrAspectConfidence,
                                  String selectedAspectId,
                                  ItemImportDetails details) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                fullItemRead, affixes, ocrSuggestedAspectId, ocrAspectConfidence, selectedAspectId, details, List.of());
    }

    public ItemImportEditableForm(String sourceImageName,
                                  String slot,
                                  String weaponDamage,
                                  String strength,
                                  String intelligence,
                                  String thorns,
                                  String blockChance,
                                  String retributionChance,
                                  FullItemRead fullItemRead,
                                  List<ImportedItemAffix> affixes,
                                  String ocrSuggestedAspectId,
                                  ItemImportFieldConfidence ocrAspectConfidence,
                                  String selectedAspectId,
                                  ItemImportDetails details,
                                  List<ItemTemperingAffix> temperingAffixes) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                fullItemRead, affixes, ocrSuggestedAspectId, ocrAspectConfidence, selectedAspectId, details,
                temperingAffixes, ItemMasterworking.defaultState());
    }

    public ItemImportEditableForm(String sourceImageName,
                                  String slot,
                                  String weaponDamage,
                                  String strength,
                                  String intelligence,
                                  String thorns,
                                  String blockChance,
                                  String retributionChance,
                                  FullItemRead fullItemRead,
                                  List<ImportedItemAffix> affixes,
                                  String ocrSuggestedAspectId,
                                  ItemImportFieldConfidence ocrAspectConfidence,
                                  String selectedAspectId,
                                  ItemImportDetails details,
                                  List<ItemTemperingAffix> temperingAffixes,
                                  ItemMasterworking masterworking) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                fullItemRead, affixes, ocrSuggestedAspectId, ocrAspectConfidence, selectedAspectId, details,
                temperingAffixes, masterworking, ItemTransfiguration.none());
    }

    public ItemImportEditableForm(String sourceImageName,
                                  String slot,
                                  String weaponDamage,
                                  String strength,
                                  String intelligence,
                                  String thorns,
                                  String blockChance,
                                  String retributionChance,
                                  FullItemRead fullItemRead,
                                  List<ImportedItemAffix> affixes,
                                  String ocrSuggestedAspectId,
                                  ItemImportFieldConfidence ocrAspectConfidence,
                                  String selectedAspectId,
                                  ItemImportDetails details,
                                  List<ItemTemperingAffix> temperingAffixes,
                                  ItemMasterworking masterworking,
                                  ItemTransfiguration transfiguration) {
        this(sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance,
                fullItemRead, affixes, ocrSuggestedAspectId, ocrAspectConfidence, selectedAspectId, details,
                temperingAffixes, masterworking, transfiguration, ItemSocketing.empty());
    }

    public ItemImportEditableForm(String sourceImageName,
                                  String slot,
                                  String weaponDamage,
                                  String strength,
                                  String intelligence,
                                  String thorns,
                                  String blockChance,
                                  String retributionChance,
                                  FullItemRead fullItemRead,
                                  List<ImportedItemAffix> affixes,
                                  String ocrSuggestedAspectId,
                                  ItemImportFieldConfidence ocrAspectConfidence,
                                  String selectedAspectId,
                                  ItemImportDetails details,
                                  List<ItemTemperingAffix> temperingAffixes,
                                  ItemMasterworking masterworking,
                                  ItemTransfiguration transfiguration,
                                  ItemSocketing socketing) {
        this.sourceImageName = sourceImageName;
        this.slot = slot;
        this.weaponDamage = weaponDamage;
        this.strength = strength;
        this.intelligence = intelligence;
        this.thorns = thorns;
        this.blockChance = blockChance;
        this.retributionChance = retributionChance;
        this.fullItemRead = fullItemRead == null ? FullItemRead.empty() : fullItemRead;
        this.affixes = affixes == null ? List.of() : List.copyOf(affixes);
        this.ocrSuggestedAspectId = ocrSuggestedAspectId == null ? "" : ocrSuggestedAspectId;
        this.ocrAspectConfidence = ocrAspectConfidence == null ? ItemImportFieldConfidence.UNKNOWN : ocrAspectConfidence;
        this.selectedAspectId = selectedAspectId == null ? "" : selectedAspectId;
        this.details = details == null ? ItemImportDetails.empty() : details;
        this.temperingAffixes = temperingAffixes == null ? List.of() : List.copyOf(temperingAffixes);
        this.masterworking = masterworking == null ? ItemMasterworking.defaultState() : masterworking;
        this.transfiguration = transfiguration == null ? ItemTransfiguration.none() : transfiguration;
        this.socketing = socketing == null ? ItemSocketing.empty() : socketing;
    }

    public String getSourceImageName() {
        return sourceImageName;
    }

    public String getSlot() {
        return slot;
    }

    public String getWeaponDamage() {
        return weaponDamage;
    }

    public String getStrength() {
        return strength;
    }

    public String getIntelligence() {
        return intelligence;
    }

    public String getThorns() {
        return thorns;
    }

    public String getBlockChance() {
        return blockChance;
    }

    public String getRetributionChance() {
        return retributionChance;
    }

    public FullItemRead getFullItemRead() {
        return fullItemRead;
    }

    public List<ImportedItemAffix> getAffixes() {
        return affixes;
    }

    public String getOcrSuggestedAspectId() {
        return ocrSuggestedAspectId;
    }

    public ItemImportFieldConfidence getOcrAspectConfidence() {
        return ocrAspectConfidence;
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

    public ItemTransfiguration getTransfiguration() {
        return transfiguration;
    }

    public ItemSocketing getSocketing() {
        return socketing;
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

    public String getItemPower() {
        return details.getItemPower() == null ? "" : Long.toString(details.getItemPower());
    }

    public String getWeaponDps() {
        return details.getWeaponDps() == null ? "" : Long.toString(details.getWeaponDps());
    }

    public String getWeaponDamageMin() {
        return details.getWeaponDamageMin() == null ? "" : Long.toString(details.getWeaponDamageMin());
    }

    public String getWeaponDamageMax() {
        return details.getWeaponDamageMax() == null ? "" : Long.toString(details.getWeaponDamageMax());
    }

    public String getAverageWeaponDamage() {
        return details.getAverageWeaponDamage() == null ? "" : Long.toString(details.getAverageWeaponDamage());
    }

    public String getAttacksPerSecond() {
        return details.getAttacksPerSecond() == null ? "" : String.format(java.util.Locale.US, "%.2f", details.getAttacksPerSecond());
    }

    public String getUniqueEffectText() {
        return details.getUniqueEffectText();
    }

    public String getItemArmor() {
        return details.getItemArmor() == null ? "" : Long.toString(details.getItemArmor());
    }
}
