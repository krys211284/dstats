package krys.itemimport;

import krys.masterworking.ItemMasterworking;
import krys.socketing.ItemSocketing;
import krys.tempering.ItemTemperingAffix;
import krys.transfiguration.ItemTransfiguration;

import java.util.List;

/** Strukturalny draft importu powstający z OCR przed ręcznym zatwierdzeniem itemu. */
public final class ItemImportDraft {
    private final ItemImageImportCandidateParseResult parseResult;
    private final String ocrSuggestedAspectId;
    private final ItemImportFieldConfidence ocrAspectConfidence;
    private final List<ImportedItemAffix> affixes;
    private final List<ItemTemperingAffix> temperingAffixes;
    private final ItemMasterworking masterworking;
    private final ItemTransfiguration transfiguration;
    private final ItemSocketing socketing;

    public ItemImportDraft(ItemImageImportCandidateParseResult parseResult,
                           String ocrSuggestedAspectId,
                           ItemImportFieldConfidence ocrAspectConfidence,
                           List<ImportedItemAffix> affixes,
                           List<ItemTemperingAffix> temperingAffixes) {
        this(parseResult, ocrSuggestedAspectId, ocrAspectConfidence, affixes, temperingAffixes,
                ItemMasterworking.defaultState(), ItemTransfiguration.none());
    }

    public ItemImportDraft(ItemImageImportCandidateParseResult parseResult,
                           String ocrSuggestedAspectId,
                           ItemImportFieldConfidence ocrAspectConfidence,
                           List<ImportedItemAffix> affixes,
                           List<ItemTemperingAffix> temperingAffixes,
                           ItemMasterworking masterworking,
                           ItemTransfiguration transfiguration) {
        this(parseResult, ocrSuggestedAspectId, ocrAspectConfidence, affixes, temperingAffixes,
                masterworking, transfiguration, ItemSocketing.empty());
    }

    public ItemImportDraft(ItemImageImportCandidateParseResult parseResult,
                           String ocrSuggestedAspectId,
                           ItemImportFieldConfidence ocrAspectConfidence,
                           List<ImportedItemAffix> affixes,
                           List<ItemTemperingAffix> temperingAffixes,
                           ItemMasterworking masterworking,
                           ItemTransfiguration transfiguration,
                           ItemSocketing socketing) {
        this.parseResult = parseResult;
        this.ocrSuggestedAspectId = ocrSuggestedAspectId == null ? "" : ocrSuggestedAspectId;
        this.ocrAspectConfidence = ocrAspectConfidence == null ? ItemImportFieldConfidence.UNKNOWN : ocrAspectConfidence;
        this.affixes = affixes == null ? List.of() : List.copyOf(affixes);
        this.temperingAffixes = temperingAffixes == null ? List.of() : List.copyOf(temperingAffixes);
        this.masterworking = masterworking == null ? ItemMasterworking.defaultState() : masterworking;
        this.transfiguration = transfiguration == null ? ItemTransfiguration.none() : transfiguration;
        this.socketing = socketing == null ? ItemSocketing.empty() : socketing;
    }

    public ItemImageImportCandidateParseResult getParseResult() {
        return parseResult;
    }

    public String getOcrSuggestedAspectId() {
        return ocrSuggestedAspectId;
    }

    public ItemImportFieldConfidence getOcrAspectConfidence() {
        return ocrAspectConfidence;
    }

    public List<ImportedItemAffix> getAffixes() {
        return affixes;
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
}
