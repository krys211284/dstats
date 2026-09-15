package krys.itemimport.image;

import java.util.Arrays;
import java.util.Objects;

/** Tymczasowy region obrazu PART_1; nie jest nowym ItemImagePart. */
public final class Part1Roi {
    private final Part1RoiType type;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final String format;
    private final byte[] imageBytes;

    public Part1Roi(Part1RoiType type,
                    int x,
                    int y,
                    int width,
                    int height,
                    String format,
                    byte[] imageBytes) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("ROI PART_1 musi miec dodatnie wymiary i nieujemne wspolrzedne.");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("ROI PART_1 musi zawierac zakodowany obraz diagnostyczny.");
        }
        this.type = Objects.requireNonNull(type, "type");
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.format = format == null || format.isBlank() ? "PNG" : format;
        this.imageBytes = Arrays.copyOf(imageBytes, imageBytes.length);
    }

    public Part1RoiType getType() {
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

    public String getFormat() {
        return format;
    }

    public byte[] getImageBytes() {
        return Arrays.copyOf(imageBytes, imageBytes.length);
    }
}
