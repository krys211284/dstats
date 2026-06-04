package krys.itemimport;

/** Metadane źródła linii OCR zachowane niezależnie od tekstu wybranego po deduplikacji. */
public final class FullItemReadLineSource {
    private static final int ORDER_BUCKET = 10_000;

    private final int sourceScreenIndex;
    private final String sourceVariantId;
    private final int sourceLineOrder;
    private final int sourceSegmentStart;
    private final int sourceSegmentEnd;
    private final String sourceRawLine;
    private final String parentRawLine;
    private final String sourceRegion;
    private final int visualBlockOrder;
    private final int visualSourceLineOrder;
    private final int visualSegmentStart;
    private final int visualSegmentEnd;
    private final String visualSourceText;

    public FullItemReadLineSource(int sourceScreenIndex,
                                  String sourceVariantId,
                                  int sourceLineOrder,
                                  int sourceSegmentStart,
                                  int sourceSegmentEnd,
                                  String sourceRawLine,
                                  String parentRawLine,
                                  String sourceRegion,
                                  int visualBlockOrder,
                                  int visualSourceLineOrder,
                                  int visualSegmentStart,
                                  int visualSegmentEnd,
                                  String visualSourceText) {
        this.sourceScreenIndex = sourceScreenIndex;
        this.sourceVariantId = sourceVariantId == null ? "" : sourceVariantId;
        this.sourceLineOrder = sourceLineOrder;
        this.sourceSegmentStart = sourceSegmentStart;
        this.sourceSegmentEnd = sourceSegmentEnd;
        this.sourceRawLine = sourceRawLine == null ? "" : sourceRawLine;
        this.parentRawLine = parentRawLine == null || parentRawLine.isBlank() ? this.sourceRawLine : parentRawLine;
        this.sourceRegion = sourceRegion == null ? "" : sourceRegion;
        this.visualBlockOrder = visualBlockOrder;
        this.visualSourceLineOrder = visualSourceLineOrder;
        this.visualSegmentStart = visualSegmentStart;
        this.visualSegmentEnd = visualSegmentEnd;
        this.visualSourceText = visualSourceText == null || visualSourceText.isBlank()
                ? this.parentRawLine
                : visualSourceText;
    }

    public static FullItemReadLineSource unknown(String rawLine) {
        String safeRawLine = rawLine == null ? "" : rawLine;
        return new FullItemReadLineSource(-1, "", -1, -1, -1, safeRawLine, safeRawLine,
                "UNKNOWN", -1, -1, -1, -1, safeRawLine);
    }

    static FullItemReadLineSource fromMergedLine(MergedOcrLine line) {
        if (line == null) {
            return unknown("");
        }
        return new FullItemReadLineSource(
                line.getSourceIndex(),
                line.getSourceVariant(),
                line.getSourceLineOrder(),
                line.getSegmentStart(),
                line.getSegmentEnd(),
                line.getSourceRawLine(),
                line.getSourceRawLine(),
                line.getSourceRegion(),
                line.getSourceLineOrder(),
                line.getSourceLineOrder(),
                line.getSegmentStart(),
                line.getSegmentEnd(),
                line.getSourceRawLine()
        );
    }

    public FullItemReadLineSource withVisualAnchorFrom(FullItemReadLineSource anchor) {
        FullItemReadLineSource safeAnchor = anchor == null ? this : anchor;
        return new FullItemReadLineSource(
                sourceScreenIndex,
                sourceVariantId,
                sourceLineOrder,
                sourceSegmentStart,
                sourceSegmentEnd,
                sourceRawLine,
                safeAnchor.getParentRawLine(),
                sourceRegion,
                safeAnchor.getVisualBlockOrder(),
                safeAnchor.getVisualSourceLineOrder(),
                safeAnchor.getVisualSegmentStart(),
                safeAnchor.getVisualSegmentEnd(),
                safeAnchor.getVisualSourceText()
        );
    }

    public FullItemReadLineSource withParentRawLine(String newParentRawLine) {
        return new FullItemReadLineSource(sourceScreenIndex, sourceVariantId, sourceLineOrder, sourceSegmentStart,
                sourceSegmentEnd, sourceRawLine, newParentRawLine, sourceRegion, visualBlockOrder,
                visualSourceLineOrder, visualSegmentStart, visualSegmentEnd, visualSourceText);
    }

    public int getSourceScreenIndex() {
        return sourceScreenIndex;
    }

    public String getSourceVariantId() {
        return sourceVariantId;
    }

    public int getSourceLineOrder() {
        return sourceLineOrder;
    }

    public int getSourceSegmentStart() {
        return sourceSegmentStart;
    }

    public int getSourceSegmentEnd() {
        return sourceSegmentEnd;
    }

    public String getSourceRawLine() {
        return sourceRawLine;
    }

    public String getParentRawLine() {
        return parentRawLine;
    }

    public String getSourceRegion() {
        return sourceRegion;
    }

    public int getVisualBlockOrder() {
        return visualBlockOrder;
    }

    public int getVisualSourceLineOrder() {
        return visualSourceLineOrder;
    }

    public int getVisualSegmentStart() {
        return visualSegmentStart;
    }

    public int getVisualSegmentEnd() {
        return visualSegmentEnd;
    }

    public String getVisualSourceText() {
        return visualSourceText;
    }

    public int selectedOrder(int fallbackOrder, int fallbackSegmentStart) {
        return compositeOrder(sourceLineOrder, sourceSegmentStart, fallbackOrder, fallbackSegmentStart);
    }

    public int visualOrder(int fallbackOrder, int fallbackSegmentStart) {
        return compositeOrder(visualSourceLineOrder, visualSegmentStart, fallbackOrder, fallbackSegmentStart);
    }

    private static int compositeOrder(int lineOrder, int segmentStart, int fallbackOrder, int fallbackSegmentStart) {
        int safeLineOrder = lineOrder >= 0 ? lineOrder : Math.max(0, fallbackOrder);
        int safeSegmentStart = segmentStart >= 0 ? segmentStart : Math.max(0, fallbackSegmentStart);
        return safeLineOrder * ORDER_BUCKET + Math.min(ORDER_BUCKET - 1, safeSegmentStart);
    }
}
