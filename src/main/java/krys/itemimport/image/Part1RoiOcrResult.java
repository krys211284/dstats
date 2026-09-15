package krys.itemimport.image;

import java.util.Objects;
import java.util.List;

/** Surowy wynik OCR dla pojedynczego ROI PART_1, bez mapowania do domeny itemu. */
public final class Part1RoiOcrResult {
    private final Part1RoiType type;
    private final String rawText;
    private final long durationMillis;
    private final String preprocessing;
    private final List<Part1RoiOcrSubRegion> subRegions;
    private final int greaterAffixCount;

    public Part1RoiOcrResult(Part1RoiType type, String rawText, long durationMillis, String preprocessing) {
        this(type, rawText, durationMillis, preprocessing, List.of());
    }

    public Part1RoiOcrResult(Part1RoiType type,
                             String rawText,
                             long durationMillis,
                             String preprocessing,
                             List<Part1RoiOcrSubRegion> subRegions) {
        this(type, rawText, durationMillis, preprocessing, subRegions, 0);
    }

    public Part1RoiOcrResult(Part1RoiType type,
                             String rawText,
                             long durationMillis,
                             String preprocessing,
                             List<Part1RoiOcrSubRegion> subRegions,
                             int greaterAffixCount) {
        this.type = Objects.requireNonNull(type, "type");
        this.rawText = rawText == null ? "" : rawText;
        this.durationMillis = Math.max(0L, durationMillis);
        this.preprocessing = preprocessing == null ? "" : preprocessing;
        this.subRegions = List.copyOf(subRegions == null ? List.of() : subRegions);
        this.greaterAffixCount = Math.max(0, greaterAffixCount);
    }

    public Part1RoiType getType() {
        return type;
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

    public List<Part1RoiOcrSubRegion> getSubRegions() {
        return subRegions;
    }

    public int getGreaterAffixCount() {
        return greaterAffixCount;
    }
}
