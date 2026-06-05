package krys.itemimport;

import java.util.List;

/** Dowód liczby gwiazdek Greater Affix wykrytej w bezpiecznym nagłówku OCR itemu. */
public final class GreaterAffixHeaderEvidence {
    private static final GreaterAffixHeaderEvidence NOT_DETECTED = new GreaterAffixHeaderEvidence(
            0,
            GreaterAffixHeaderEvidenceSource.NOT_DETECTED,
            "",
            "",
            false,
            List.of()
    );

    private final int detectedCount;
    private final GreaterAffixHeaderEvidenceSource source;
    private final String sourceVariantId;
    private final String sourceText;
    private final boolean reliable;
    private final List<String> warnings;

    public GreaterAffixHeaderEvidence(int detectedCount,
                                      GreaterAffixHeaderEvidenceSource source,
                                      String sourceVariantId,
                                      String sourceText,
                                      boolean reliable,
                                      List<String> warnings) {
        this.detectedCount = Math.max(0, detectedCount);
        this.source = source == null ? GreaterAffixHeaderEvidenceSource.NOT_DETECTED : source;
        this.sourceVariantId = sourceVariantId == null ? "" : sourceVariantId;
        this.sourceText = sourceText == null ? "" : sourceText;
        this.reliable = reliable;
        this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static GreaterAffixHeaderEvidence notDetected() {
        return NOT_DETECTED;
    }

    public int getDetectedCount() {
        return detectedCount;
    }

    public GreaterAffixHeaderEvidenceSource getSource() {
        return source;
    }

    public String getSourceVariantId() {
        return sourceVariantId;
    }

    public String getSourceText() {
        return sourceText;
    }

    public boolean isReliable() {
        return reliable;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
