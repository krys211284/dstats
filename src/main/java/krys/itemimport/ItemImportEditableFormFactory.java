package krys.itemimport;

import krys.item.EquipmentSlot;

import java.util.Locale;

/** Buduje formularz ręcznego potwierdzenia z wstępnie rozpoznanych pól. */
public final class ItemImportEditableFormFactory {
    private final ImportedItemAffixExtractor affixExtractor = new ImportedItemAffixExtractor();
    private final AspectRegistry aspectRegistry;

    public ItemImportEditableFormFactory() {
        this(ApplicationAspectRegistry.get());
    }

    ItemImportEditableFormFactory(AspectRegistry aspectRegistry) {
        this.aspectRegistry = aspectRegistry;
    }

    public ItemImportEditableForm create(ItemImageImportCandidateParseResult parseResult) {
        ItemImportDraft draft = createDraft(parseResult);
        return new ItemImportEditableForm(
                parseResult.getImageMetadata().getOriginalFilename(),
                toSlotValue(parseResult.getSlotCandidate().getSuggestedValue() == null
                        ? parseResult.getFullItemRead().getDetails().getEquipmentSlot()
                        : parseResult.getSlotCandidate().getSuggestedValue()),
                "0",
                toDoubleValue(parseResult.getStrengthCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getIntelligenceCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getThornsCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getBlockChanceCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getRetributionChanceCandidate().getSuggestedValue()),
                parseResult.getFullItemRead(),
                draft.getAffixes(),
                draft.getOcrSuggestedAspectId(),
                draft.getOcrAspectConfidence(),
                draft.getOcrSuggestedAspectId(),
                parseResult.getFullItemRead().getDetails()
        );
    }

    public ItemImportDraft createDraft(ItemImageImportCandidateParseResult parseResult) {
        AspectRegistry.AspectMatch aspectMatch = aspectRegistry.suggestFromFullRead(parseResult.getFullItemRead())
                .orElse(new AspectRegistry.AspectMatch("", ItemImportFieldConfidence.UNKNOWN));
        EquipmentSlot effectiveSlot = parseResult.getSlotCandidate().getSuggestedValue() == null
                ? parseResult.getFullItemRead().getDetails().getEquipmentSlot()
                : parseResult.getSlotCandidate().getSuggestedValue();
        if (!aspectMatch.aspectId().isBlank()
                && aspectRegistry.findById(aspectMatch.aspectId())
                .filter(aspect -> aspect.allowsSlot(effectiveSlot))
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
