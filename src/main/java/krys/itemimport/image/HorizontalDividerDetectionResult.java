package krys.itemimport.image;

import java.util.Arrays;
import java.util.List;

/** Wynik strukturalnej detekcji poziomych separatorow w finalnym obrazie itemu. */
public final class HorizontalDividerDetectionResult {
    private final List<HorizontalDivider> dividers;
    private final List<ItemImagePart> parts;
    private final byte[] overlayImageBytes;
    private final String overlayFormat;
    private final int overlayWidth;
    private final int overlayHeight;

    public HorizontalDividerDetectionResult(List<HorizontalDivider> dividers,
                                            List<ItemImagePart> parts,
                                            byte[] overlayImageBytes,
                                            String overlayFormat,
                                            int overlayWidth,
                                            int overlayHeight) {
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("Detekcja separatorow musi zwrocic co najmniej jedna czesc obrazu.");
        }
        if (overlayImageBytes == null || overlayImageBytes.length == 0) {
            throw new IllegalArgumentException("Detekcja separatorow musi zwrocic obraz overlay.");
        }
        if (overlayWidth <= 0 || overlayHeight <= 0) {
            throw new IllegalArgumentException("Overlay separatorow musi miec dodatnie wymiary.");
        }
        this.dividers = List.copyOf(dividers == null ? List.of() : dividers);
        this.parts = List.copyOf(parts);
        this.overlayImageBytes = Arrays.copyOf(overlayImageBytes, overlayImageBytes.length);
        this.overlayFormat = overlayFormat == null || overlayFormat.isBlank() ? "PNG" : overlayFormat;
        this.overlayWidth = overlayWidth;
        this.overlayHeight = overlayHeight;
    }

    public List<HorizontalDivider> getDividers() {
        return dividers;
    }

    public List<ItemImagePart> getParts() {
        return parts;
    }

    public byte[] getOverlayImageBytes() {
        return Arrays.copyOf(overlayImageBytes, overlayImageBytes.length);
    }

    public String getOverlayFormat() {
        return overlayFormat;
    }

    public int getOverlayWidth() {
        return overlayWidth;
    }

    public int getOverlayHeight() {
        return overlayHeight;
    }
}
