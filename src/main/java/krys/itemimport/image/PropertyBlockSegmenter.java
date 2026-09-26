package krys.itemimport.image;

/** Segmentuje structural PART_2/PART_3 na neutralne bloki właściwości. */
public interface PropertyBlockSegmenter {
    PropertyBlockSegmentationResult segment(StructuralItemPart part);
}
