package krys.itemimport;

import krys.tempering.ItemTemperingAffix;

import java.util.List;

/** Strukturalny draft importu powstający z OCR przed ręcznym zatwierdzeniem itemu. */
public final class ItemImportDraft {
    private final ItemImageImportCandidateParseResult parseResult;
    private final String ocrSuggestedAspectId;
    private final ItemImportFieldConfidence ocrAspectConfidence;
    private final List<ImportedItemAffix> affixes;
    private final List<ItemTemperingAffix> temperingAffixes;

    public ItemImportDraft(ItemImageImportCandidateParseResult parseResult,
                           String ocrSuggestedAspectId,
                           ItemImportFieldConfidence ocrAspectConfidence,
                           List<ImportedItemAffix> affixes,
                           List<ItemTemperingAffix> temperingAffixes) {
        this.parseResult = parseResult;
        this.ocrSuggestedAspectId = ocrSuggestedAspectId == null ? "" : ocrSuggestedAspectId;
        this.ocrAspectConfidence = ocrAspectConfidence == null ? ItemImportFieldConfidence.UNKNOWN : ocrAspectConfidence;
        this.affixes = affixes == null ? List.of() : List.copyOf(affixes);
        this.temperingAffixes = temperingAffixes == null ? List.of() : List.copyOf(temperingAffixes);
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
}
