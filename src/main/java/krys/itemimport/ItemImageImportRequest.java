package krys.itemimport;

import java.util.Arrays;

/** Surowe wejście warstwy importu obrazu pojedynczego itemu. */
public final class ItemImageImportRequest {
    private final String originalFilename;
    private final String contentType;
    private final byte[] imageBytes;

    public ItemImageImportRequest(String originalFilename, String contentType, byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Obraz itemu nie może być pusty");
        }
        this.originalFilename = originalFilename == null ? "item" : originalFilename;
        this.contentType = contentType == null ? "application/octet-stream" : contentType;
        this.imageBytes = Arrays.copyOf(imageBytes, imageBytes.length);
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getImageBytes() {
        return Arrays.copyOf(imageBytes, imageBytes.length);
    }
}
