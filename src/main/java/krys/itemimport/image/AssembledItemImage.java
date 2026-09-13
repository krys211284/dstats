package krys.itemimport.image;

import java.util.Arrays;
import java.util.List;

/** Neutralny wynik deterministycznego skladania obrazow itemu. */
public final class AssembledItemImage {
    public static final String STATUS_SINGLE_IMAGE = "SINGLE_IMAGE";
    public static final String STATUS_STITCHED = "STITCHED";

    private final byte[] imageBytes;
    private final String format;
    private final int width;
    private final int height;
    private final int sourceImageCount;
    private final String compositionPolicy;
    private final String assemblyStatus;
    private final List<StitchDiagnostic> stitchDiagnostics;

    public AssembledItemImage(byte[] imageBytes,
                              String format,
                              int width,
                              int height,
                              int sourceImageCount,
                              String compositionPolicy) {
        this(
                imageBytes,
                format,
                width,
                height,
                sourceImageCount,
                compositionPolicy,
                "",
                List.of()
        );
    }

    public AssembledItemImage(byte[] imageBytes,
                              String format,
                              int width,
                              int height,
                              int sourceImageCount,
                              String compositionPolicy,
                              String assemblyStatus,
                              List<StitchDiagnostic> stitchDiagnostics) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Zlozony obraz itemu nie moze byc pusty.");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Zlozony obraz itemu musi miec dodatnie wymiary.");
        }
        if (sourceImageCount <= 0) {
            throw new IllegalArgumentException("Zlozony obraz itemu musi miec co najmniej jedno zrodlo.");
        }
        this.imageBytes = Arrays.copyOf(imageBytes, imageBytes.length);
        this.format = format == null || format.isBlank() ? "PNG" : format;
        this.width = width;
        this.height = height;
        this.sourceImageCount = sourceImageCount;
        this.compositionPolicy = compositionPolicy == null ? "" : compositionPolicy;
        this.assemblyStatus = assemblyStatus == null ? "" : assemblyStatus;
        this.stitchDiagnostics = List.copyOf(stitchDiagnostics == null ? List.of() : stitchDiagnostics);
    }

    public byte[] getImageBytes() {
        return Arrays.copyOf(imageBytes, imageBytes.length);
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

    public int getSourceImageCount() {
        return sourceImageCount;
    }

    public String getCompositionPolicy() {
        return compositionPolicy;
    }

    public String getAssemblyStatus() {
        return assemblyStatus;
    }

    public List<StitchDiagnostic> getStitchDiagnostics() {
        return stitchDiagnostics;
    }

    /** Diagnostyka jednej pary sasiednich screenow uzytej przy skladaniu overlapu. */
    public static final class StitchDiagnostic {
        private final int firstSourceIndex;
        private final int secondSourceIndex;
        private final int offsetX;
        private final int offsetY;
        private final int overlapHeight;
        private final int seamY;
        private final double confidence;
        private final String status;

        public StitchDiagnostic(int firstSourceIndex,
                                int secondSourceIndex,
                                int offsetX,
                                int offsetY,
                                int overlapHeight,
                                int seamY,
                                double confidence,
                                String status) {
            this.firstSourceIndex = firstSourceIndex;
            this.secondSourceIndex = secondSourceIndex;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.overlapHeight = overlapHeight;
            this.seamY = seamY;
            this.confidence = confidence;
            this.status = status == null ? "" : status;
        }

        public int getFirstSourceIndex() {
            return firstSourceIndex;
        }

        public int getSecondSourceIndex() {
            return secondSourceIndex;
        }

        public int getOffsetX() {
            return offsetX;
        }

        public int getOffsetY() {
            return offsetY;
        }

        public int getOverlapHeight() {
            return overlapHeight;
        }

        public int getSeamY() {
            return seamY;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getStatus() {
            return status;
        }
    }
}
