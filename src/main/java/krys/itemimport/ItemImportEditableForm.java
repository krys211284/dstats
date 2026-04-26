package krys.itemimport;

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
        this.ocrSuggestedAspectId = "";
        this.ocrAspectConfidence = ItemImportFieldConfidence.UNKNOWN;
        this.selectedAspectId = "";
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
}
