package krys.itemimport;

/** Metadane techniczne obrazu wykorzystane do bezpiecznego foundation importu. */
public final class ItemImageMetadata {
    private final String originalFilename;
    private final String contentType;
    private final String format;
    private final int width;
    private final int height;

    public ItemImageMetadata(String originalFilename,
                             String contentType,
                             String format,
                             int width,
                             int height) {
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.format = format;
        this.width = width;
        this.height = height;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFormat() {
        return format;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
