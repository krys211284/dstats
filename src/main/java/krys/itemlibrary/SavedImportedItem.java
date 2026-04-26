package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.ImportedItemAffix;

import java.util.List;

/** Trwale zapisany item z biblioteki użytkownika z własnym stabilnym identyfikatorem. */
public final class SavedImportedItem {
    private final long itemId;
    private final String displayName;
    private final String sourceImageName;
    private final EquipmentSlot slot;
    private final long weaponDamage;
    private final double strength;
    private final double intelligence;
    private final double thorns;
    private final double blockChance;
    private final double retributionChance;
    private final FullItemRead fullItemRead;
    private final List<ImportedItemAffix> affixes;
    private final String selectedAspectId;

    public SavedImportedItem(long itemId,
                             String displayName,
                             String sourceImageName,
                             EquipmentSlot slot,
                             long weaponDamage,
                             double strength,
                             double intelligence,
                             double thorns,
                             double blockChance,
                             double retributionChance) {
        this(itemId, displayName, sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance, FullItemRead.empty());
    }

    public SavedImportedItem(long itemId,
                             String displayName,
                             String sourceImageName,
                             EquipmentSlot slot,
                             long weaponDamage,
                             double strength,
                             double intelligence,
                             double thorns,
                             double blockChance,
                             double retributionChance,
                             FullItemRead fullItemRead) {
        this(itemId, displayName, sourceImageName, slot, weaponDamage, strength, intelligence, thorns, blockChance, retributionChance, fullItemRead, List.of());
    }

    public SavedImportedItem(long itemId,
                             String displayName,
                             String sourceImageName,
                             EquipmentSlot slot,
                             long weaponDamage,
                             double strength,
                             double intelligence,
                             double thorns,
                             double blockChance,
                             double retributionChance,
                             FullItemRead fullItemRead,
                             List<ImportedItemAffix> affixes) {
        this(itemId, displayName, sourceImageName, slot, weaponDamage, strength, intelligence, thorns,
                blockChance, retributionChance, fullItemRead, affixes, "");
    }

    public SavedImportedItem(long itemId,
                             String displayName,
                             String sourceImageName,
                             EquipmentSlot slot,
                             long weaponDamage,
                             double strength,
                             double intelligence,
                             double thorns,
                             double blockChance,
                             double retributionChance,
                             FullItemRead fullItemRead,
                             List<ImportedItemAffix> affixes,
                             String selectedAspectId) {
        if (itemId < 0L) {
            throw new IllegalArgumentException("Id itemu nie może być ujemne.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name itemu jest wymagany.");
        }
        if (sourceImageName == null || sourceImageName.isBlank()) {
            throw new IllegalArgumentException("Nazwa źródłowego obrazu itemu jest wymagana.");
        }
        if (slot == null) {
            throw new IllegalArgumentException("Slot itemu jest wymagany.");
        }
        if (weaponDamage < 0L) {
            throw new IllegalArgumentException("Weapon damage itemu nie może być ujemny.");
        }
        validateNonNegative("Strength", strength);
        validateNonNegative("Intelligence", intelligence);
        validateNonNegative("Thorns", thorns);
        validateNonNegative("Block chance", blockChance);
        validateNonNegative("Retribution chance", retributionChance);

        this.itemId = itemId;
        this.displayName = displayName;
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
        this.selectedAspectId = selectedAspectId == null ? "" : selectedAspectId;
    }

    private static void validateNonNegative(String label, double value) {
        if (value < 0.0d) {
            throw new IllegalArgumentException(label + " itemu nie może być ujemny.");
        }
    }

    public long getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
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

    public FullItemRead getFullItemRead() {
        return fullItemRead;
    }

    public List<ImportedItemAffix> getAffixes() {
        return affixes;
    }

    public String getSelectedAspectId() {
        return selectedAspectId;
    }
}
