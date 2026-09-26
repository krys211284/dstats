package krys.itemimport.image;

import java.awt.image.BufferedImage;
import java.util.List;

/** Detektor wizualnych anchorów w PART_2/PART_3, bez klasyfikacji domenowej. */
public interface PropertyBlockAnchorDetector {
    List<PropertyBlockAnchor> detect(BufferedImage image);
}
