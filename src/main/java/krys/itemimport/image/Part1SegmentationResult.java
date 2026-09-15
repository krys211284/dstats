package krys.itemimport.image;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Wynik segmentacji PART_1 na tymczasowe ROI przed przyszlym OCR. */
public final class Part1SegmentationResult {
    private final StructuralItemPart sourcePart;
    private final List<Part1Roi> rois;
    private final String diagnosticReason;

    public Part1SegmentationResult(StructuralItemPart sourcePart,
                                   List<Part1Roi> rois,
                                   String diagnosticReason) {
        this.sourcePart = Objects.requireNonNull(sourcePart, "sourcePart");
        this.rois = List.copyOf(rois == null ? List.of() : rois);
        this.diagnosticReason = diagnosticReason == null ? "" : diagnosticReason;
    }

    public StructuralItemPart getSourcePart() {
        return sourcePart;
    }

    public List<Part1Roi> getRois() {
        return rois;
    }

    public String getDiagnosticReason() {
        return diagnosticReason;
    }

    public Optional<Part1Roi> find(Part1RoiType type) {
        Objects.requireNonNull(type, "type");
        return rois.stream()
                .filter(roi -> roi.getType() == type)
                .findFirst();
    }
}
