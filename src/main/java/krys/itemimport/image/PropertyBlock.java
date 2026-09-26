package krys.itemimport.image;

import java.util.Arrays;
import java.util.Optional;

/** Neutralny obrazowy blok właściwości z PART_2/PART_3, bez OCR i bez mapowania domenowego. */
public final class PropertyBlock {
    private final StructuralItemPartSlot sourcePart;
    private final int order;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final PropertyBlockAnchorType anchorType;
    private final PropertyBlockAnchor anchor;
    private final String format;
    private final byte[] imageBytes;
    private final String diagnostics;

    public PropertyBlock(StructuralItemPartSlot sourcePart,
                         int order,
                         int x,
                         int y,
                         int width,
                         int height,
                         PropertyBlockAnchorType anchorType,
                         PropertyBlockAnchor anchor,
                         String format,
                         byte[] imageBytes,
                         String diagnostics) {
        if (sourcePart != StructuralItemPartSlot.PART_2 && sourcePart != StructuralItemPartSlot.PART_3) {
            throw new IllegalArgumentException("Bloki właściwości mogą pochodzić tylko z PART_2 albo PART_3.");
        }
        if (order <= 0 || x < 0 || y < 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Blok właściwości musi mieć dodatni order i wymiary.");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Blok właściwości musi zawierać obraz diagnostyczny.");
        }
        this.sourcePart = sourcePart;
        this.order = order;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.anchorType = anchorType == null ? PropertyBlockAnchorType.NONE : anchorType;
        this.anchor = anchor;
        this.format = format == null || format.isBlank() ? "PNG" : format;
        this.imageBytes = Arrays.copyOf(imageBytes, imageBytes.length);
        this.diagnostics = diagnostics == null ? "" : diagnostics;
    }

    public StructuralItemPartSlot getSourcePart() {
        return sourcePart;
    }

    public int getOrder() {
        return order;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public PropertyBlockAnchorType getAnchorType() {
        return anchorType;
    }

    public Optional<PropertyBlockAnchor> getAnchor() {
        return Optional.ofNullable(anchor);
    }

    public String getFormat() {
        return format;
    }

    public byte[] getImageBytes() {
        return Arrays.copyOf(imageBytes, imageBytes.length);
    }

    public String getDiagnostics() {
        return diagnostics;
    }
}
