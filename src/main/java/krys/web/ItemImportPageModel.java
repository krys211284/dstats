package krys.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model widoku SSR dla diagnostycznego importu obrazu itemu po Etapie 3A. */
public final class ItemImportPageModel {
    private final List<String> validationErrors;
    private final OpenCvImagePreview openCvImagePreview;
    private final String openCvPreviewError;
    private final HeroProfile activeHero;
    private final String helpText;
    private final String currentBuildQuery;

    public ItemImportPageModel(List<String> validationErrors,
                               OpenCvImagePreview openCvImagePreview,
                               String openCvPreviewError,
                               HeroProfile activeHero,
                               String helpText,
                               String currentBuildQuery) {
        this.validationErrors = Collections.unmodifiableList(new ArrayList<>(validationErrors == null ? List.of() : validationErrors));
        this.openCvImagePreview = openCvImagePreview;
        this.openCvPreviewError = openCvPreviewError == null ? "" : openCvPreviewError;
        this.activeHero = activeHero;
        this.helpText = helpText == null ? "" : helpText;
        this.currentBuildQuery = currentBuildQuery == null ? "" : currentBuildQuery;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public OpenCvImagePreview getOpenCvImagePreview() {
        return openCvImagePreview;
    }

    public String getOpenCvPreviewError() {
        return openCvPreviewError;
    }

    public HeroProfile getActiveHero() {
        return activeHero;
    }

    public String getHelpText() {
        return helpText;
    }

    public String getCurrentBuildQuery() {
        return currentBuildQuery;
    }

    public boolean hasOpenCvImagePreview() {
        return openCvImagePreview != null;
    }

    public boolean hasOpenCvPreviewError() {
        return !openCvPreviewError.isBlank();
    }

    public boolean hasActiveHero() {
        return activeHero != null;
    }

    /** Diagnostyczny podglad zlozonego obrazu OpenCV, bez podpinania go pod OCR. */
    public static final class OpenCvImagePreview {
        private final String dataUri;
        private final int sourceImageCount;
        private final int width;
        private final int height;
        private final String format;
        private final String compositionPolicy;
        private final String assemblyStatus;
        private final List<OpenCvStitchDiagnosticView> stitchDiagnostics;
        private final HorizontalDividerDetectionView horizontalDividerDetection;
        private final String horizontalDividerDetectionError;
        private final String debugWarning;

        public OpenCvImagePreview(String dataUri,
                                  int sourceImageCount,
                                  int width,
                                  int height,
                                  String format,
                                  String compositionPolicy,
                                  String assemblyStatus,
                                  List<OpenCvStitchDiagnosticView> stitchDiagnostics,
                                  HorizontalDividerDetectionView horizontalDividerDetection,
                                  String horizontalDividerDetectionError,
                                  String debugWarning) {
            this.dataUri = dataUri == null ? "" : dataUri;
            this.sourceImageCount = sourceImageCount;
            this.width = width;
            this.height = height;
            this.format = format == null ? "" : format;
            this.compositionPolicy = compositionPolicy == null ? "" : compositionPolicy;
            this.assemblyStatus = assemblyStatus == null ? "" : assemblyStatus;
            this.stitchDiagnostics = List.copyOf(stitchDiagnostics == null ? List.of() : stitchDiagnostics);
            this.horizontalDividerDetection = horizontalDividerDetection;
            this.horizontalDividerDetectionError = horizontalDividerDetectionError == null ? "" : horizontalDividerDetectionError;
            this.debugWarning = debugWarning == null ? "" : debugWarning;
        }

        public String getDataUri() {
            return dataUri;
        }

        public int getSourceImageCount() {
            return sourceImageCount;
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

        public String getCompositionPolicy() {
            return compositionPolicy;
        }

        public String getAssemblyStatus() {
            return assemblyStatus;
        }

        public List<OpenCvStitchDiagnosticView> getStitchDiagnostics() {
            return stitchDiagnostics;
        }

        public boolean hasStitchDiagnostics() {
            return !stitchDiagnostics.isEmpty();
        }

        public HorizontalDividerDetectionView getHorizontalDividerDetection() {
            return horizontalDividerDetection;
        }

        public boolean hasHorizontalDividerDetection() {
            return horizontalDividerDetection != null;
        }

        public String getHorizontalDividerDetectionError() {
            return horizontalDividerDetectionError;
        }

        public boolean hasHorizontalDividerDetectionError() {
            return !horizontalDividerDetectionError.isBlank();
        }

        public String getDebugWarning() {
            return debugWarning;
        }

        public boolean hasDebugWarning() {
            return !debugWarning.isBlank();
        }
    }

    /** Diagnostyka strukturalnych separatorow poziomych wykrytych na finalnym obrazie OpenCV. */
    public static final class HorizontalDividerDetectionView {
        private final int dividerCount;
        private final int partCount;
        private final String overlayDataUri;
        private final List<HorizontalDividerView> dividers;
        private final List<ItemImagePartView> parts;
        private final ItemPartStructureNormalizationView structureNormalization;
        private final String structureNormalizationError;

        public HorizontalDividerDetectionView(int dividerCount,
                                              int partCount,
                                              String overlayDataUri,
                                              List<HorizontalDividerView> dividers,
                                              List<ItemImagePartView> parts,
                                              ItemPartStructureNormalizationView structureNormalization,
                                              String structureNormalizationError) {
            this.dividerCount = dividerCount;
            this.partCount = partCount;
            this.overlayDataUri = overlayDataUri == null ? "" : overlayDataUri;
            this.dividers = List.copyOf(dividers == null ? List.of() : dividers);
            this.parts = List.copyOf(parts == null ? List.of() : parts);
            this.structureNormalization = structureNormalization;
            this.structureNormalizationError = structureNormalizationError == null ? "" : structureNormalizationError;
        }

        public int getDividerCount() {
            return dividerCount;
        }

        public int getPartCount() {
            return partCount;
        }

        public String getOverlayDataUri() {
            return overlayDataUri;
        }

        public List<HorizontalDividerView> getDividers() {
            return dividers;
        }

        public List<ItemImagePartView> getParts() {
            return parts;
        }

        public ItemPartStructureNormalizationView getStructureNormalization() {
            return structureNormalization;
        }

        public boolean hasStructureNormalization() {
            return structureNormalization != null;
        }

        public String getStructureNormalizationError() {
            return structureNormalizationError;
        }

        public boolean hasStructureNormalizationError() {
            return !structureNormalizationError.isBlank();
        }
    }

    /** Pojedynczy separator poziomy w widoku diagnostycznym. */
    public static final class HorizontalDividerView {
        private final int index;
        private final int y;
        private final int bandStartY;
        private final int bandEndY;
        private final String strength;

        public HorizontalDividerView(int index, int y, int bandStartY, int bandEndY, String strength) {
            this.index = index;
            this.y = y;
            this.bandStartY = bandStartY;
            this.bandEndY = bandEndY;
            this.strength = strength == null ? "" : strength;
        }

        public int getIndex() {
            return index;
        }

        public int getY() {
            return y;
        }

        public int getBandStartY() {
            return bandStartY;
        }

        public int getBandEndY() {
            return bandEndY;
        }

        public String getStrength() {
            return strength;
        }
    }

    /** Pojedyncza surowa czesc finalnego obrazu po podziale przez separatory. */
    public static final class ItemImagePartView {
        private final int index;
        private final String name;
        private final int startYInclusive;
        private final int endYExclusive;
        private final int width;
        private final int height;
        private final String dataUri;

        public ItemImagePartView(int index,
                                 String name,
                                 int startYInclusive,
                                 int endYExclusive,
                                 int width,
                                 int height,
                                 String dataUri) {
            this.index = index;
            this.name = name == null ? "" : name;
            this.startYInclusive = startYInclusive;
            this.endYExclusive = endYExclusive;
            this.width = width;
            this.height = height;
            this.dataUri = dataUri == null ? "" : dataUri;
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

        public String getDataUri() {
            return dataUri;
        }
    }

    /** Diagnostyka Etapu 3A bez ingerencji w obrazy i raw parts z Etapu 2A. */
    public static final class ItemPartStructureNormalizationView {
        private final String status;
        private final int rawPartCount;
        private final List<StructuralItemPartMappingView> mappings;
        private final String diagnosticReason;

        public ItemPartStructureNormalizationView(String status,
                                                  int rawPartCount,
                                                  List<StructuralItemPartMappingView> mappings,
                                                  String diagnosticReason) {
            this.status = status == null ? "" : status;
            this.rawPartCount = rawPartCount;
            this.mappings = List.copyOf(mappings == null ? List.of() : mappings);
            this.diagnosticReason = diagnosticReason == null ? "" : diagnosticReason;
        }

        public String getStatus() {
            return status;
        }

        public int getRawPartCount() {
            return rawPartCount;
        }

        public List<StructuralItemPartMappingView> getMappings() {
            return mappings;
        }

        public String getDiagnosticReason() {
            return diagnosticReason;
        }

        public boolean hasDiagnosticReason() {
            return !diagnosticReason.isBlank();
        }
    }

    /** Jeden wiersz mapowania structural PART_N -> raw part #N albo absent. */
    public static final class StructuralItemPartMappingView {
        private final String structuralSlot;
        private final boolean present;
        private final int rawPartIndex;

        public StructuralItemPartMappingView(String structuralSlot, boolean present, int rawPartIndex) {
            this.structuralSlot = structuralSlot == null ? "" : structuralSlot;
            this.present = present;
            this.rawPartIndex = rawPartIndex;
        }

        public String getStructuralSlot() {
            return structuralSlot;
        }

        public boolean isPresent() {
            return present;
        }

        public int getRawPartIndex() {
            return rawPartIndex;
        }
    }

    /** Diagnostyka jednej pary obrazow pokazywana tylko w panelu OpenCV. */
    public static final class OpenCvStitchDiagnosticView {
        private final int firstSourceIndex;
        private final int secondSourceIndex;
        private final int offsetX;
        private final int offsetY;
        private final int overlapHeight;
        private final int seamY;
        private final String confidence;
        private final String status;

        public OpenCvStitchDiagnosticView(int firstSourceIndex,
                                          int secondSourceIndex,
                                          int offsetX,
                                          int offsetY,
                                          int overlapHeight,
                                          int seamY,
                                          String confidence,
                                          String status) {
            this.firstSourceIndex = firstSourceIndex;
            this.secondSourceIndex = secondSourceIndex;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.overlapHeight = overlapHeight;
            this.seamY = seamY;
            this.confidence = confidence == null ? "" : confidence;
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

        public String getConfidence() {
            return confidence;
        }

        public String getStatus() {
            return status;
        }
    }
}
