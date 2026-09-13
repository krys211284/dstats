package krys.web;

import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ValidatedImportedItem;
import krys.itemlibrary.SavedImportedItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model widoku SSR dla foundation importu pojedynczego itemu ze screena. */
public final class ItemImportPageModel {
    private final ItemImportEditableForm editableForm;
    private final ItemImageImportCandidateParseResult parseResult;
    private final List<String> validationErrors;
    private final ConfirmedImportView confirmedImportView;
    private final OpenCvImagePreview openCvImagePreview;
    private final String openCvPreviewError;
    private final HeroProfile activeHero;
    private final String helpText;
    private final String currentBuildQuery;

    public ItemImportPageModel(ItemImportEditableForm editableForm,
                               ItemImageImportCandidateParseResult parseResult,
                               List<String> validationErrors,
                               ConfirmedImportView confirmedImportView,
                               HeroProfile activeHero,
                               String helpText,
                               String currentBuildQuery) {
        this(
                editableForm,
                parseResult,
                validationErrors,
                confirmedImportView,
                null,
                "",
                activeHero,
                helpText,
                currentBuildQuery
        );
    }

    public ItemImportPageModel(ItemImportEditableForm editableForm,
                               ItemImageImportCandidateParseResult parseResult,
                               List<String> validationErrors,
                               ConfirmedImportView confirmedImportView,
                               OpenCvImagePreview openCvImagePreview,
                               String openCvPreviewError,
                               HeroProfile activeHero,
                               String helpText,
                               String currentBuildQuery) {
        this.editableForm = editableForm;
        this.parseResult = parseResult;
        this.validationErrors = Collections.unmodifiableList(new ArrayList<>(validationErrors));
        this.confirmedImportView = confirmedImportView;
        this.openCvImagePreview = openCvImagePreview;
        this.openCvPreviewError = openCvPreviewError == null ? "" : openCvPreviewError;
        this.activeHero = activeHero;
        this.helpText = helpText;
        this.currentBuildQuery = currentBuildQuery;
    }

    public ItemImportEditableForm getEditableForm() {
        return editableForm;
    }

    public ItemImageImportCandidateParseResult getParseResult() {
        return parseResult;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public ConfirmedImportView getConfirmedImportView() {
        return confirmedImportView;
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

    public boolean hasParseResult() {
        return parseResult != null && editableForm != null;
    }

    public boolean hasEditableForm() {
        return editableForm != null;
    }

    public boolean hasConfirmedImport() {
        return confirmedImportView != null;
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

    /** Diagnostyczny podgląd złożonego obrazu OpenCV, bez podpinania go pod OCR. */
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
                                  String debugWarning) {
            this(
                    dataUri,
                    sourceImageCount,
                    width,
                    height,
                    format,
                    compositionPolicy,
                    "",
                    List.of(),
                    null,
                    "",
                    debugWarning
            );
        }

        public OpenCvImagePreview(String dataUri,
                                  int sourceImageCount,
                                  int width,
                                  int height,
                                  String format,
                                  String compositionPolicy,
                                  String assemblyStatus,
                                  List<OpenCvStitchDiagnosticView> stitchDiagnostics,
                                  String debugWarning) {
            this(
                    dataUri,
                    sourceImageCount,
                    width,
                    height,
                    format,
                    compositionPolicy,
                    assemblyStatus,
                    stitchDiagnostics,
                    null,
                    "",
                    debugWarning
            );
        }

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

        public HorizontalDividerDetectionView(int dividerCount,
                                              int partCount,
                                              String overlayDataUri,
                                              List<HorizontalDividerView> dividers,
                                              List<ItemImagePartView> parts) {
            this.dividerCount = dividerCount;
            this.partCount = partCount;
            this.overlayDataUri = overlayDataUri == null ? "" : overlayDataUri;
            this.dividers = List.copyOf(dividers == null ? List.of() : dividers);
            this.parts = List.copyOf(parts == null ? List.of() : parts);
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

    /** Pojedyncza czesc finalnego obrazu po podziale przez separatory. */
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

    /** Widok zatwierdzonego itemu i jego mapowania do aktualnego modelu buildu. */
    public static final class ConfirmedImportView {
        private final ValidatedImportedItem importedItem;
        private final SavedImportedItem savedItem;
        private final krys.item.Item mappedItem;
        private final ImportedItemCurrentBuildContribution contribution;

        public ConfirmedImportView(ValidatedImportedItem importedItem,
                                   SavedImportedItem savedItem,
                                   krys.item.Item mappedItem,
                                   ImportedItemCurrentBuildContribution contribution) {
            this.importedItem = importedItem;
            this.savedItem = savedItem;
            this.mappedItem = mappedItem;
            this.contribution = contribution;
        }

        public ValidatedImportedItem getImportedItem() {
            return importedItem;
        }

        public SavedImportedItem getSavedItem() {
            return savedItem;
        }

        public krys.item.Item getMappedItem() {
            return mappedItem;
        }

        public ImportedItemCurrentBuildContribution getContribution() {
            return contribution;
        }
    }
}
