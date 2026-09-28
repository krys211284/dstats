package krys.itemimport.image;

import java.util.List;
import java.util.Objects;

/** Neutralny wynik odczytu pojedynczego PropertyBlock, bez klasyfikacji domenowej i bez affix matching. */
public final class PropertyBlockReadResult {
    private final StructuralItemPartSlot sourcePart;
    private final int blockOrder;
    private final PropertyBlockAnchorType anchorType;
    private final String rawText;
    private final long durationMillis;
    private final String preprocessing;
    private final List<String> failureDiagnostics;
    private final List<PropertyBlockNumericEvidence> numericEvidence;
    private final List<PropertyBlockOcrProfileResult> profileResults;

    public PropertyBlockReadResult(StructuralItemPartSlot sourcePart,
                                   int blockOrder,
                                   PropertyBlockAnchorType anchorType,
                                   String rawText,
                                   long durationMillis,
                                   String preprocessing,
                                   List<String> failureDiagnostics) {
        this(sourcePart, blockOrder, anchorType, rawText, durationMillis, preprocessing,
                failureDiagnostics, List.of(), List.of());
    }

    public PropertyBlockReadResult(StructuralItemPartSlot sourcePart,
                                   int blockOrder,
                                   PropertyBlockAnchorType anchorType,
                                   String rawText,
                                   long durationMillis,
                                   String preprocessing,
                                   List<String> failureDiagnostics,
                                   List<PropertyBlockNumericEvidence> numericEvidence,
                                   List<PropertyBlockOcrProfileResult> profileResults) {
        if (sourcePart != StructuralItemPartSlot.PART_2 && sourcePart != StructuralItemPartSlot.PART_3) {
            throw new IllegalArgumentException("PropertyBlockReadResult obsluguje tylko PART_2 albo PART_3.");
        }
        this.sourcePart = sourcePart;
        this.blockOrder = Math.max(1, blockOrder);
        this.anchorType = anchorType == null ? PropertyBlockAnchorType.NONE : anchorType;
        this.rawText = rawText == null ? "" : rawText;
        this.durationMillis = Math.max(0L, durationMillis);
        this.preprocessing = preprocessing == null ? "" : preprocessing;
        this.failureDiagnostics = List.copyOf(failureDiagnostics == null ? List.of() : failureDiagnostics);
        this.numericEvidence = List.copyOf(numericEvidence == null ? List.of() : numericEvidence);
        this.profileResults = List.copyOf(profileResults == null ? List.of() : profileResults);
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

    public String getRawText() {
        return rawText;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public String getPreprocessing() {
        return preprocessing;
    }

    public List<String> getFailureDiagnostics() {
        return failureDiagnostics;
    }

    public boolean hasFailureDiagnostics() {
        return !failureDiagnostics.isEmpty();
    }

    public List<PropertyBlockNumericEvidence> getNumericEvidence() {
        return numericEvidence;
    }

    public List<PropertyBlockOcrProfileResult> getProfileResults() {
        return profileResults;
    }

    public boolean hasAmbiguousNumericEvidence() {
        return numericEvidence.stream().anyMatch(PropertyBlockNumericEvidence::isAmbiguous);
    }

    public static PropertyBlockReadResult failed(PropertyBlock block, long durationMillis, String diagnostic) {
        Objects.requireNonNull(block, "block");
        return new PropertyBlockReadResult(
                block.getSourcePart(),
                block.getOrder(),
                block.getAnchorType(),
                "",
                durationMillis,
                "",
                diagnostic == null || diagnostic.isBlank() ? List.of() : List.of(diagnostic)
        );
    }
}
