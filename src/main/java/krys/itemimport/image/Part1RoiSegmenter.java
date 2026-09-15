package krys.itemimport.image;

/** Segmentuje structural PART_1 na tymczasowe ROI przygotowane pod przyszly OCR. */
public interface Part1RoiSegmenter {
    Part1SegmentationResult segment(StructuralItemPart part1);
}
