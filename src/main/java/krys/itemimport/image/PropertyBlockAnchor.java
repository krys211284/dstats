package krys.itemimport.image;

import java.util.Optional;

/** Wykryty wizualny anchor wpisu właściwości, bez interpretacji domenowej. */
public final class PropertyBlockAnchor {
    private final PropertyBlockAnchorType type;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final double confidence;

    public PropertyBlockAnchor(PropertyBlockAnchorType type,
                               int x,
                               int y,
                               int width,
                               int height,
                               double confidence) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Anchor bloku właściwości musi mieć dodatnie wymiary.");
        }
        this.type = type == null ? PropertyBlockAnchorType.UNKNOWN_ICON : type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    public static Optional<PropertyBlockAnchor> none() {
        return Optional.empty();
    }

    public PropertyBlockAnchorType getType() {
        return type;
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

    public double getConfidence() {
        return confidence;
    }
}
