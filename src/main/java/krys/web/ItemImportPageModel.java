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
        private final Part1SegmentationView part1Segmentation;
        private final String part1SegmentationError;

        public ItemPartStructureNormalizationView(String status,
                                                  int rawPartCount,
                                                  List<StructuralItemPartMappingView> mappings,
                                                  String diagnosticReason) {
            this(status, rawPartCount, mappings, diagnosticReason, null, "");
        }

        public ItemPartStructureNormalizationView(String status,
                                                  int rawPartCount,
                                                  List<StructuralItemPartMappingView> mappings,
                                                  String diagnosticReason,
                                                  Part1SegmentationView part1Segmentation,
                                                  String part1SegmentationError) {
            this.status = status == null ? "" : status;
            this.rawPartCount = rawPartCount;
            this.mappings = List.copyOf(mappings == null ? List.of() : mappings);
            this.diagnosticReason = diagnosticReason == null ? "" : diagnosticReason;
            this.part1Segmentation = part1Segmentation;
            this.part1SegmentationError = part1SegmentationError == null ? "" : part1SegmentationError;
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

        public Part1SegmentationView getPart1Segmentation() {
            return part1Segmentation;
        }

        public boolean hasPart1Segmentation() {
            return part1Segmentation != null;
        }

        public String getPart1SegmentationError() {
            return part1SegmentationError;
        }

        public boolean hasPart1SegmentationError() {
            return !part1SegmentationError.isBlank();
        }
    }

    /** Diagnostyka tymczasowej segmentacji PART_1 na ROI przed OCR. */
    public static final class Part1SegmentationView {
        private final String sourcePartDataUri;
        private final int sourcePartIndex;
        private final String sourcePartName;
        private final int sourcePartWidth;
        private final int sourcePartHeight;
        private final List<Part1RoiView> rois;
        private final Part1ReadResultView readResult;

        public Part1SegmentationView(String sourcePartDataUri,
                                     int sourcePartIndex,
                                     String sourcePartName,
                                     int sourcePartWidth,
                                     int sourcePartHeight,
                                     List<Part1RoiView> rois) {
            this(sourcePartDataUri, sourcePartIndex, sourcePartName, sourcePartWidth, sourcePartHeight, rois, null);
        }

        public Part1SegmentationView(String sourcePartDataUri,
                                     int sourcePartIndex,
                                     String sourcePartName,
                                     int sourcePartWidth,
                                     int sourcePartHeight,
                                     List<Part1RoiView> rois,
                                     Part1ReadResultView readResult) {
            this.sourcePartDataUri = sourcePartDataUri == null ? "" : sourcePartDataUri;
            this.sourcePartIndex = sourcePartIndex;
            this.sourcePartName = sourcePartName == null ? "" : sourcePartName;
            this.sourcePartWidth = sourcePartWidth;
            this.sourcePartHeight = sourcePartHeight;
            this.rois = List.copyOf(rois == null ? List.of() : rois);
            this.readResult = readResult;
        }

        public String getSourcePartDataUri() {
            return sourcePartDataUri;
        }

        public int getSourcePartIndex() {
            return sourcePartIndex;
        }

        public String getSourcePartName() {
            return sourcePartName;
        }

        public int getSourcePartWidth() {
            return sourcePartWidth;
        }

        public int getSourcePartHeight() {
            return sourcePartHeight;
        }

        public List<Part1RoiView> getRois() {
            return rois;
        }

        public Part1ReadResultView getReadResult() {
            return readResult;
        }

        public boolean hasReadResult() {
            return readResult != null;
        }
    }

    /** Neutralny wynik odczytu PART_1 pokazywany wyłącznie diagnostycznie. */
    public static final class Part1ReadResultView {
        private final String itemName;
        private final int greaterAffixCount;
        private final String classificationRaw;
        private final String itemType;
        private final String itemRarity;
        private final String ancient;
        private final String mythicUnique;
        private final String equipmentSlot;
        private final String itemPower;
        private final String displayedQuality;
        private final String masterworkingQuality;
        private final List<String> parseFailures;

        public Part1ReadResultView(String itemName,
                                   int greaterAffixCount,
                                   String classificationRaw,
                                   String itemType,
                                   String itemRarity,
                                   String ancient,
                                   String mythicUnique,
                                   String equipmentSlot,
                                   String itemPower,
                                   String displayedQuality,
                                   String masterworkingQuality,
                                   List<String> parseFailures) {
            this.itemName = itemName == null ? "" : itemName;
            this.greaterAffixCount = Math.max(0, greaterAffixCount);
            this.classificationRaw = classificationRaw == null ? "" : classificationRaw;
            this.itemType = itemType == null || itemType.isBlank() ? "brak" : itemType;
            this.itemRarity = itemRarity == null || itemRarity.isBlank() ? "brak" : itemRarity;
            this.ancient = ancient == null || ancient.isBlank() ? "brak" : ancient;
            this.mythicUnique = mythicUnique == null || mythicUnique.isBlank() ? "brak" : mythicUnique;
            this.equipmentSlot = equipmentSlot == null || equipmentSlot.isBlank() ? "brak" : equipmentSlot;
            this.itemPower = itemPower == null || itemPower.isBlank() ? "brak" : itemPower;
            this.displayedQuality = displayedQuality == null || displayedQuality.isBlank() ? "brak" : displayedQuality;
            this.masterworkingQuality = masterworkingQuality == null || masterworkingQuality.isBlank() ? "brak" : masterworkingQuality;
            this.parseFailures = List.copyOf(parseFailures == null ? List.of() : parseFailures);
        }

        public String getItemName() {
            return itemName;
        }

        public int getGreaterAffixCount() {
            return greaterAffixCount;
        }

        public String getClassificationRaw() {
            return classificationRaw;
        }

        public String getItemType() {
            return itemType;
        }

        public String getItemRarity() {
            return itemRarity;
        }

        public String getAncient() {
            return ancient;
        }

        public String getMythicUnique() {
            return mythicUnique;
        }

        public String getEquipmentSlot() {
            return equipmentSlot;
        }

        public String getItemPower() {
            return itemPower;
        }

        public String getDisplayedQuality() {
            return displayedQuality;
        }

        public String getMasterworkingQuality() {
            return masterworkingQuality;
        }

        public List<String> getParseFailures() {
            return parseFailures;
        }

        public boolean hasParseFailures() {
            return !parseFailures.isEmpty();
        }
    }

    /** Pojedynczy tymczasowy ROI PART_1 pokazywany w diagnostyce UI. */
    public static final class Part1RoiView {
        private final String type;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final String dataUri;
        private final String rawOcrText;
        private final long ocrDurationMillis;
        private final String ocrPreprocessing;
        private final String ocrError;
        private final List<Part1RoiSubRegionView> subRegions;
        private final int greaterAffixCount;

        public Part1RoiView(String type, int x, int y, int width, int height, String dataUri) {
            this(type, x, y, width, height, dataUri, "", 0L, "", "");
        }

        public Part1RoiView(String type,
                            int x,
                            int y,
                            int width,
                            int height,
                            String dataUri,
                            String rawOcrText,
                            long ocrDurationMillis,
                            String ocrPreprocessing,
                            String ocrError) {
            this(type, x, y, width, height, dataUri, rawOcrText, ocrDurationMillis, ocrPreprocessing, ocrError, List.of());
        }

        public Part1RoiView(String type,
                            int x,
                            int y,
                            int width,
                            int height,
                            String dataUri,
                            String rawOcrText,
                            long ocrDurationMillis,
                            String ocrPreprocessing,
                            String ocrError,
                            List<Part1RoiSubRegionView> subRegions) {
            this(type, x, y, width, height, dataUri, rawOcrText, ocrDurationMillis,
                    ocrPreprocessing, ocrError, subRegions, 0);
        }

        public Part1RoiView(String type,
                            int x,
                            int y,
                            int width,
                            int height,
                            String dataUri,
                            String rawOcrText,
                            long ocrDurationMillis,
                            String ocrPreprocessing,
                            String ocrError,
                            List<Part1RoiSubRegionView> subRegions,
                            int greaterAffixCount) {
            this.type = type == null ? "" : type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.dataUri = dataUri == null ? "" : dataUri;
            this.rawOcrText = rawOcrText == null ? "" : rawOcrText;
            this.ocrDurationMillis = Math.max(0L, ocrDurationMillis);
            this.ocrPreprocessing = ocrPreprocessing == null ? "" : ocrPreprocessing;
            this.ocrError = ocrError == null ? "" : ocrError;
            this.subRegions = List.copyOf(subRegions == null ? List.of() : subRegions);
            this.greaterAffixCount = Math.max(0, greaterAffixCount);
        }

        public String getType() {
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

        public String getDataUri() {
            return dataUri;
        }

        public String getRawOcrText() {
            return rawOcrText;
        }

        public long getOcrDurationMillis() {
            return ocrDurationMillis;
        }

        public String getOcrPreprocessing() {
            return ocrPreprocessing;
        }

        public String getOcrError() {
            return ocrError;
        }

        public boolean hasOcrError() {
            return !ocrError.isBlank();
        }

        public List<Part1RoiSubRegionView> getSubRegions() {
            return subRegions;
        }

        public boolean hasSubRegions() {
            return !subRegions.isEmpty();
        }

        public int getGreaterAffixCount() {
            return greaterAffixCount;
        }

        public boolean hasGreaterAffixDiagnostics() {
            return "NAME_AND_STARS".equals(type);
        }
    }

    /** Wewnętrzny sub-ROI użyty wyłącznie do diagnostyki OCR w ramach publicznego ROI PART_1. */
    public static final class Part1RoiSubRegionView {
        private final String type;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final String dataUri;
        private final String preprocessing;

        public Part1RoiSubRegionView(String type,
                                     int x,
                                     int y,
                                     int width,
                                     int height,
                                     String dataUri,
                                     String preprocessing) {
            this.type = type == null ? "" : type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.dataUri = dataUri == null ? "" : dataUri;
            this.preprocessing = preprocessing == null ? "" : preprocessing;
        }

        public String getType() {
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

        public String getDataUri() {
            return dataUri;
        }

        public String getPreprocessing() {
            return preprocessing;
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
