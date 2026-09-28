package krys.itemimport.image;

import krys.transfiguration.HoradricTransfigurationOutcome;

import java.util.List;

/** Neutralny wynik klasyfikacji PropertyBlock, przed budową domenowego itemu. */
public final class PropertyBlockSemanticResult {
    private final StructuralItemPartSlot sourcePart;
    private final int blockOrder;
    private final PropertyBlockAnchorType anchorType;
    private final PropertyBlockSemanticKind kind;
    private final String definitionId;
    private final List<String> candidateDefinitionIds;
    private final boolean greaterAffix;
    private final boolean numericValueCertain;
    private final String rawText;
    private final List<PropertyBlockNumericEvidence> numericEvidence;
    private final HoradricTransfigurationOutcome transfigurationOutcome;
    private final List<String> diagnostics;

    public PropertyBlockSemanticResult(StructuralItemPartSlot sourcePart,
                                       int blockOrder,
                                       PropertyBlockAnchorType anchorType,
                                       PropertyBlockSemanticKind kind,
                                       String definitionId,
                                       List<String> candidateDefinitionIds,
                                       boolean greaterAffix,
                                       boolean numericValueCertain,
                                       String rawText,
                                       List<PropertyBlockNumericEvidence> numericEvidence,
                                       HoradricTransfigurationOutcome transfigurationOutcome,
                                       List<String> diagnostics) {
        this.sourcePart = sourcePart;
        this.blockOrder = Math.max(1, blockOrder);
        this.anchorType = anchorType == null ? PropertyBlockAnchorType.NONE : anchorType;
        this.kind = kind == null ? PropertyBlockSemanticKind.UNKNOWN : kind;
        this.definitionId = definitionId == null ? "" : definitionId;
        this.candidateDefinitionIds = List.copyOf(
                candidateDefinitionIds == null ? List.of() : candidateDefinitionIds);
        this.greaterAffix = greaterAffix;
        this.numericValueCertain = numericValueCertain;
        this.rawText = rawText == null ? "" : rawText;
        this.numericEvidence = List.copyOf(numericEvidence == null ? List.of() : numericEvidence);
        this.transfigurationOutcome = transfigurationOutcome == null
                ? HoradricTransfigurationOutcome.NONE
                : transfigurationOutcome;
        this.diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    public StructuralItemPartSlot getSourcePart() {
        return sourcePart;
    }

    public int getBlockOrder() {
        return blockOrder;
    }

    public PropertyBlockAnchorType getAnchorType() {
        return anchorType;
    }

    public PropertyBlockSemanticKind getKind() {
        return kind;
    }

    public PropertyBlockSemanticKind getSemanticKind() {
        return kind;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public boolean hasDefinitionId() {
        return !definitionId.isBlank();
    }

    public List<String> getCandidateDefinitionIds() {
        return candidateDefinitionIds;
    }

    public boolean isGreaterAffix() {
        return greaterAffix;
    }

    public boolean isNumericValueCertain() {
        return numericValueCertain;
    }

    public String getRawText() {
        return rawText;
    }

    public List<PropertyBlockNumericEvidence> getNumericEvidence() {
        return numericEvidence;
    }

    public HoradricTransfigurationOutcome getTransfigurationOutcome() {
        return transfigurationOutcome;
    }

    public List<String> getDiagnostics() {
        return diagnostics;
    }

    public boolean isAmbiguous() {
        return kind == PropertyBlockSemanticKind.AMBIGUOUS;
    }
}
