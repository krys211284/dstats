package krys.itemimport.image;

import krys.item.EquipmentSlot;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Neutralny wynik odczytu PART_1, bez mapowania do domeny itemu. */
public final class Part1ReadResult {
    private final String itemName;
    private final int greaterAffixCount;
    private final String classificationRaw;
    private final String itemType;
    private final String itemRarity;
    private final Optional<Boolean> ancient;
    private final Optional<Boolean> mythicUnique;
    private final Optional<EquipmentSlot> equipmentSlot;
    private final OptionalInt itemPower;
    private final OptionalInt displayedQuality;
    private final OptionalInt masterworkingQuality;
    private final Map<Part1RoiType, Part1RoiOcrResult> rawOcrByRoi;
    private final List<String> parseFailures;

    public Part1ReadResult(String itemName,
                           int greaterAffixCount,
                           String classificationRaw,
                           String itemType,
                           String itemRarity,
                           Optional<Boolean> ancient,
                           Optional<Boolean> mythicUnique,
                           Optional<EquipmentSlot> equipmentSlot,
                           OptionalInt itemPower,
                           OptionalInt displayedQuality,
                           OptionalInt masterworkingQuality,
                           Map<Part1RoiType, Part1RoiOcrResult> rawOcrByRoi,
                           List<String> parseFailures) {
        this.itemName = itemName == null ? "" : itemName;
        this.greaterAffixCount = Math.max(0, greaterAffixCount);
        this.classificationRaw = classificationRaw == null ? "" : classificationRaw;
        this.itemType = itemType == null ? "" : itemType;
        this.itemRarity = itemRarity == null ? "" : itemRarity;
        this.ancient = ancient == null ? Optional.empty() : ancient;
        this.mythicUnique = mythicUnique == null ? Optional.empty() : mythicUnique;
        this.equipmentSlot = equipmentSlot == null ? Optional.empty() : equipmentSlot;
        this.itemPower = itemPower == null ? OptionalInt.empty() : itemPower;
        this.displayedQuality = displayedQuality == null ? OptionalInt.empty() : displayedQuality;
        this.masterworkingQuality = masterworkingQuality == null ? OptionalInt.empty() : masterworkingQuality;
        EnumMap<Part1RoiType, Part1RoiOcrResult> rawCopy = new EnumMap<>(Part1RoiType.class);
        if (rawOcrByRoi != null) {
            rawCopy.putAll(rawOcrByRoi);
        }
        this.rawOcrByRoi = Map.copyOf(rawCopy);
        this.parseFailures = List.copyOf(parseFailures == null ? List.of() : parseFailures);
    }

    public String getItemName() {
        return itemName;
    }

    public int getGreaterAffixCount() {
        return greaterAffixCount;
    }

    public String getClassificationRaw() {
        return classificationRaw;
    }

    public String getItemType() {
        return itemType;
    }

    public String getItemRarity() {
        return itemRarity;
    }

    public Optional<Boolean> getAncient() {
        return ancient;
    }

    public Optional<Boolean> getMythicUnique() {
        return mythicUnique;
    }

    public Optional<EquipmentSlot> getEquipmentSlot() {
        return equipmentSlot;
    }

    public OptionalInt getItemPower() {
        return itemPower;
    }

    public OptionalInt getDisplayedQuality() {
        return displayedQuality;
    }

    public OptionalInt getMasterworkingQuality() {
        return masterworkingQuality;
    }

    public Map<Part1RoiType, Part1RoiOcrResult> getRawOcrByRoi() {
        return rawOcrByRoi;
    }

    public Optional<Part1RoiOcrResult> findRawOcr(Part1RoiType type) {
        return Optional.ofNullable(rawOcrByRoi.get(Objects.requireNonNull(type, "type")));
    }

    public List<String> getParseFailures() {
        return parseFailures;
    }

    public boolean hasParseFailures() {
        return !parseFailures.isEmpty();
    }
}
