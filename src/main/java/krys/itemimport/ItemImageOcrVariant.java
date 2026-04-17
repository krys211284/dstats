package krys.itemimport;

import java.awt.image.BufferedImage;

/** Pojedynczy wariant obrazu przygotowany do OCR wraz z obszarem źródłowym. */
final class ItemImageOcrVariant {
    private final String variantId;
    private final BufferedImage image;
    private final int sourceX;
    private final int sourceY;
    private final int sourceWidth;
    private final int sourceHeight;

    ItemImageOcrVariant(String variantId,
                        BufferedImage image,
                        int sourceX,
                        int sourceY,
                        int sourceWidth,
                        int sourceHeight) {
        this.variantId = variantId;
        this.image = image;
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    String getVariantId() {
        return variantId;
    }

    BufferedImage getImage() {
        return image;
    }

    int getSourceX() {
        return sourceX;
    }

    int getSourceY() {
        return sourceY;
    }

    int getSourceWidth() {
        return sourceWidth;
    }

    int getSourceHeight() {
        return sourceHeight;
    }
}
