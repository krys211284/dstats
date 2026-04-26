package krys.itemimport;

import krys.item.EquipmentSlot;

import java.util.Locale;

/** Buduje formularz ręcznego potwierdzenia z wstępnie rozpoznanych pól. */
public final class ItemImportEditableFormFactory {
    private final ImportedItemAffixExtractor affixExtractor = new ImportedItemAffixExtractor();
    private final AspectRegistry aspectRegistry = new AspectRegistry();

    public ItemImportEditableForm create(ItemImageImportCandidateParseResult parseResult) {
        ItemImportDraft draft = createDraft(parseResult);
        return new ItemImportEditableForm(
                parseResult.getImageMetadata().getOriginalFilename(),
                toSlotValue(parseResult.getSlotCandidate().getSuggestedValue()),
                toLongValue(parseResult.getWeaponDamageCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getStrengthCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getIntelligenceCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getThornsCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getBlockChanceCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getRetributionChanceCandidate().getSuggestedValue()),
                parseResult.getFullItemRead(),
                draft.getAffixes(),
                draft.getOcrSuggestedAspectId(),
                draft.getOcrAspectConfidence(),
                draft.getOcrSuggestedAspectId()
        );
    }

    public ItemImportDraft createDraft(ItemImageImportCandidateParseResult parseResult) {
        AspectRegistry.AspectMatch aspectMatch = aspectRegistry.suggestFromFullRead(parseResult.getFullItemRead())
                .orElse(new AspectRegistry.AspectMatch("", ItemImportFieldConfidence.UNKNOWN));
        if (!aspectMatch.aspectId().isBlank()
                && aspectRegistry.findById(aspectMatch.aspectId())
                .filter(aspect -> aspect.allowsSlot(parseResult.getSlotCandidate().getSuggestedValue()))
                .isEmpty()) {
            aspectMatch = new AspectRegistry.AspectMatch("", ItemImportFieldConfidence.UNKNOWN);
        }
        return new ItemImportDraft(
                parseResult,
                aspectMatch.aspectId(),
                aspectMatch.confidence(),
                affixExtractor.extractEditableAffixes(parseResult.getFullItemRead())
        );
    }

    private static String toSlotValue(EquipmentSlot slot) {
        return slot == null ? "" : slot.name();
    }

    private static String toLongValue(Long value) {
        return value == null ? "" : Long.toString(value);
    }

    private static String toDoubleValue(Double value) {
        return value == null ? "" : String.format(Locale.US, "%.0f", value);
    }
}
