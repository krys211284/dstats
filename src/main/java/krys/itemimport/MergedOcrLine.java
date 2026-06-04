package krys.itemimport;

/** Pojedyncza linia po merge OCR razem z pochodzeniem i decyzją klasyfikacji. */
final class MergedOcrLine {
    static final String SOCKET_GEM_RUNE = "SOCKET_GEM_RUNE";
    static final String DATA_ONLY = "DATA_ONLY";

    private final String text;
    private final String canonicalKey;
    private final String sourceCategory;
    private final String sourceRegion;
    private final String sourceRawLine;
    private final int sourceIndex;
    private final String sourceVariant;
    private final int sourceLineOrder;
    private final int segmentStart;
    private final int segmentEnd;
    private final String localAnchorType;
    private final String matchedType;
    private final Double value;
    private final String runtimeStatus;

    MergedOcrLine(String text,
                  String canonicalKey,
                  String sourceCategory,
                  String sourceRegion,
                  String sourceRawLine,
                  int sourceIndex,
                  String sourceVariant,
                  int sourceLineOrder,
                  int segmentStart,
                  int segmentEnd,
                  String localAnchorType,
                  String matchedType,
                  Double value,
                  String runtimeStatus) {
        this.text = text == null ? "" : text;
        this.canonicalKey = canonicalKey == null ? "" : canonicalKey;
        this.sourceCategory = sourceCategory == null ? "" : sourceCategory;
        this.sourceRegion = sourceRegion == null ? "" : sourceRegion;
        this.sourceRawLine = sourceRawLine == null ? "" : sourceRawLine;
        this.sourceIndex = sourceIndex;
        this.sourceVariant = sourceVariant == null ? "" : sourceVariant;
        this.sourceLineOrder = sourceLineOrder;
        this.segmentStart = segmentStart;
        this.segmentEnd = segmentEnd;
        this.localAnchorType = localAnchorType == null ? "" : localAnchorType;
        this.matchedType = matchedType == null ? "" : matchedType;
        this.value = value;
        this.runtimeStatus = runtimeStatus == null ? "" : runtimeStatus;
    }

    String getText() {
        return text;
    }

    String getCanonicalKey() {
        return canonicalKey;
    }

    String getSourceCategory() {
        return sourceCategory;
    }

    String getSourceRegion() {
        return sourceRegion;
    }

    String getSourceRawLine() {
        return sourceRawLine;
    }

    int getSourceIndex() {
        return sourceIndex;
    }

    String getSourceVariant() {
        return sourceVariant;
    }

    int getSourceLineOrder() {
        return sourceLineOrder;
    }

    int getSegmentStart() {
        return segmentStart;
    }

    int getSegmentEnd() {
        return segmentEnd;
    }

    String getLocalAnchorType() {
        return localAnchorType;
    }

    String getMatchedType() {
        return matchedType;
    }

    Double getValue() {
        return value;
    }

    String getRuntimeStatus() {
        return runtimeStatus;
    }

    boolean isSocketGemRuneData() {
        return SOCKET_GEM_RUNE.equals(sourceCategory)
                || canonicalKey.startsWith("socket-stat:")
                || DATA_ONLY.equals(runtimeStatus);
    }

    String occurrenceKey() {
        return "variant=" + sourceVariant
                + "|screen=" + sourceIndex
                + "|line=" + sourceLineOrder
                + "|segment=" + segmentStart + "-" + segmentEnd
                + "|text=" + text
                + "|value=" + (value == null ? "" : value);
    }
}
