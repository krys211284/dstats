package krys.itemimport.image;

import java.util.Arrays;

/** Fragment finalnego obrazu itemu wyznaczony przez strukturalne separatory poziome. */
public final class ItemImagePart {
    private final int index;
    private final String name;
    private final int startYInclusive;
    private final int endYExclusive;
    private final int width;
    private final int height;
    private final String format;
    private final byte[] imageBytes;

    public ItemImagePart(int index,
                         String name,
                         int startYInclusive,
                         int endYExclusive,
                         int width,
                         int height,
                         String format,
                         byte[] imageBytes) {
        if (index <= 0) {
            throw new IllegalArgumentException("Indeks czesci obrazu musi byc dodatni.");
        }
        if (startYInclusive < 0 || endYExclusive <= startYInclusive) {
            throw new IllegalArgumentException("Czesc obrazu itemu musi miec dodatni zakres Y.");
        }
        if (width <= 0 || height <= 0 || height != endYExclusive - startYInclusive) {
            throw new IllegalArgumentException("Czesc obrazu itemu musi miec dodatnie, spojne wymiary.");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Czesc obrazu itemu musi zawierac zakodowany obraz.");
        }
        this.index = index;
        this.name = name == null || name.isBlank() ? "PART_" + index : name;
        this.startYInclusive = startYInclusive;
        this.endYExclusive = endYExclusive;
        this.width = width;
        this.height = height;
        this.format = format == null || format.isBlank() ? "PNG" : format;
        this.imageBytes = Arrays.copyOf(imageBytes, imageBytes.length);
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public int getStartYInclusive() {
        return startYInclusive;
    }

    public int getEndYExclusive() {
        return endYExclusive;
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
