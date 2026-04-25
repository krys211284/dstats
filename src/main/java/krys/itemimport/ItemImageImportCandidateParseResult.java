package krys.itemimport;

import krys.item.EquipmentSlot;

/** Wynik wstępnego odczytu itemu ze screena przed ręcznym potwierdzeniem użytkownika. */
public final class ItemImageImportCandidateParseResult {
    private final ItemImageMetadata imageMetadata;
    private final FullItemRead fullItemRead;
    private final ItemImportFieldCandidate<EquipmentSlot> slotCandidate;
    private final ItemImportFieldCandidate<Long> weaponDamageCandidate;
    private final ItemImportFieldCandidate<Double> strengthCandidate;
    private final ItemImportFieldCandidate<Double> intelligenceCandidate;
    private final ItemImportFieldCandidate<Double> thornsCandidate;
    private final ItemImportFieldCandidate<Double> blockChanceCandidate;
    private final ItemImportFieldCandidate<Double> retributionChanceCandidate;
    private final String importNotice;

    public ItemImageImportCandidateParseResult(ItemImageMetadata imageMetadata,
                                               FullItemRead fullItemRead,
                                               ItemImportFieldCandidate<EquipmentSlot> slotCandidate,
                                               ItemImportFieldCandidate<Long> weaponDamageCandidate,
                                               ItemImportFieldCandidate<Double> strengthCandidate,
                                               ItemImportFieldCandidate<Double> intelligenceCandidate,
                                               ItemImportFieldCandidate<Double> thornsCandidate,
                                               ItemImportFieldCandidate<Double> blockChanceCandidate,
                                               ItemImportFieldCandidate<Double> retributionChanceCandidate,
                                               String importNotice) {
        this.imageMetadata = imageMetadata;
        this.fullItemRead = fullItemRead == null ? FullItemRead.empty() : fullItemRead;
        this.slotCandidate = slotCandidate;
        this.weaponDamageCandidate = weaponDamageCandidate;
        this.strengthCandidate = strengthCandidate;
        this.intelligenceCandidate = intelligenceCandidate;
        this.thornsCandidate = thornsCandidate;
        this.blockChanceCandidate = blockChanceCandidate;
        this.retributionChanceCandidate = retributionChanceCandidate;
        this.importNotice = importNotice;
    }

    public ItemImageMetadata getImageMetadata() {
        return imageMetadata;
    }

    public FullItemRead getFullItemRead() {
        return fullItemRead;
    }

    public ItemImportFieldCandidate<EquipmentSlot> getSlotCandidate() {
        return slotCandidate;
    }

    public ItemImportFieldCandidate<Long> getWeaponDamageCandidate() {
        return weaponDamageCandidate;
    }

    public ItemImportFieldCandidate<Double> getStrengthCandidate() {
        return strengthCandidate;
    }

    public ItemImportFieldCandidate<Double> getIntelligenceCandidate() {
        return intelligenceCandidate;
    }

    public ItemImportFieldCandidate<Double> getThornsCandidate() {
        return thornsCandidate;
    }

    public ItemImportFieldCandidate<Double> getBlockChanceCandidate() {
        return blockChanceCandidate;
    }

    public ItemImportFieldCandidate<Double> getRetributionChanceCandidate() {
        return retributionChanceCandidate;
    }

    public String getImportNotice() {
        return importNotice;
    }
}
