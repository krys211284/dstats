package krys.itemimport.image;

/** Wykrywa strukturalne poziome separatory w finalnym obrazie itemu. */
public interface HorizontalDividerDetector {
    HorizontalDividerDetectionResult detect(AssembledItemImage image);
}
