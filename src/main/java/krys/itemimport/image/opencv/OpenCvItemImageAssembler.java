package krys.itemimport.image.opencv;

import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.AssembledItemImage;
import krys.itemimport.image.AssembledItemImage.StitchDiagnostic;
import krys.itemimport.image.AssembledItemImageDebugWriter;
import krys.itemimport.image.ItemImageAssembler;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgproc.line;

/** Implementacja skladania obrazow itemu oparta o OpenCV; typy Mat nie wychodza poza te klase. */
public final class OpenCvItemImageAssembler implements ItemImageAssembler {
    public static final String SINGLE_IMAGE_POLICY = "SINGLE_IMAGE_PANEL_CROP_NO_SCALE";
    public static final String STITCHED_COMPOSITION_POLICY = "OVERLAP_AWARE_PANEL_STITCH_NO_SCALE";
    public static final String RAW_COMPOSITION_POLICY = "DIAGNOSTIC_VERTICAL_UPLOAD_ORDER_NO_SCALE_MAX_WIDTH_LEFT_ALIGN_BLACK_PADDING";
    public static final String COMPOSITION_POLICY = STITCHED_COMPOSITION_POLICY;

    private static final double MIN_STITCH_CONFIDENCE = 0.82d;
    private static final double MIN_TEXTURE_SCORE = 2.0d;
    private static final int MIN_OVERLAP_PIXELS = 8;
    private static final int MAX_HORIZONTAL_SHIFT_PIXELS = 80;
    private static final int OVERLAP_SEARCH_STEP = 1;
    private static final int OFFSET_SEARCH_STEP = 1;
    private static final int SAMPLE_STEP = 2;
    private static final int PANEL_MARGIN_PIXELS = 8;
    private static final int PANEL_ACTIVITY_RADIUS = 4;
    private static final int PANEL_MAX_INACTIVE_GAP = 14;

    @Override
    public AssembledItemImage assemble(List<ItemImageImportRequest> requests) {
        List<ItemImageImportRequest> safeRequests = requests == null ? List.of() : requests;
        if (safeRequests.isEmpty()) {
            throw new IllegalArgumentException("Wgraj co najmniej jeden obraz itemu do zlozenia.");
        }

        List<Mat> decodedImages = new ArrayList<>();
        List<PanelImage> panels = new ArrayList<>();
        try {
            for (int index = 0; index < safeRequests.size(); index++) {
                decodedImages.add(decodeImage(safeRequests.get(index), index));
            }

            if (decodedImages.size() == 1) {
                Mat image = decodedImages.getFirst();
                PanelImage panel = localizePanel(image, PANEL_MARGIN_PIXELS, 0, PANEL_MARGIN_PIXELS, PANEL_MARGIN_PIXELS, true);
                panels.add(panel);
                return new AssembledItemImage(
                        encodePng(panel.image()),
                        "PNG",
                        panel.image().cols(),
                        panel.image().rows(),
                        1,
                        SINGLE_IMAGE_POLICY,
                        AssembledItemImage.STATUS_SINGLE_IMAGE,
                        List.of()
                );
            }

            Mat rawComposition = buildRawComposition(decodedImages);
            try {
                writeDebugPng(rawComposition, debugPath("raw-composition.png"));
            } finally {
                rawComposition.close();
            }

            for (int index = 0; index < decodedImages.size(); index++) {
                PanelImage panel = localizePanel(decodedImages.get(index));
                panels.add(panel);
                writeDebugPng(panel.image(), debugPath("source-%02d-panel.png".formatted(index + 1)));
            }

            List<StitchMatch> matches = new ArrayList<>();
            List<StitchDiagnostic> diagnostics = new ArrayList<>();
            for (int index = 0; index < panels.size() - 1; index++) {
                StitchMatch match = findBestMatch(panels.get(index), panels.get(index + 1), index);
                matches.add(match);
                diagnostics.add(match.toDiagnostic(index));
                Mat overlapDebugImage = buildOverlapDebugImage(panels.get(index), panels.get(index + 1), match);
                try {
                    writeDebugPng(overlapDebugImage, debugPath("overlap-%02d-%02d.png".formatted(index + 1, index + 2)));
                } finally {
                    overlapDebugImage.close();
                }
            }

            Mat stitched = composeStitchedPanels(panels, matches);
            try {
                writeDebugPng(stitched, AssembledItemImageDebugWriter.stitchedItemImagePath());
                return new AssembledItemImage(
                        encodePng(stitched),
                        "PNG",
                        stitched.cols(),
                        stitched.rows(),
                        decodedImages.size(),
                        STITCHED_COMPOSITION_POLICY,
                        AssembledItemImage.STATUS_STITCHED,
                        diagnostics
                );
            } finally {
                stitched.close();
            }
        } finally {
            for (PanelImage panel : panels) {
                panel.close();
            }
            for (Mat image : decodedImages) {
                image.close();
            }
        }
    }

    private static Mat decodeImage(ItemImageImportRequest request, int index) {
        if (request == null) {
            throw new IllegalArgumentException("Obraz itemu #" + (index + 1) + " nie moze byc pusty.");
        }
        byte[] imageBytes = request.getImageBytes();
        try (BytePointer pointer = new BytePointer(imageBytes);
             Mat encoded = new Mat(1, imageBytes.length, CV_8UC1, pointer)) {
            Mat image = imdecode(encoded, IMREAD_COLOR);
            if (image == null || image.empty()) {
                if (image != null) {
                    image.close();
                }
                throw new IllegalArgumentException("Nie udalo sie zdekodowac obrazu itemu #" + (index + 1) + " przez OpenCV.");
            }
            return image;
        }
    }

    private static PanelImage localizePanel(Mat source) {
        return localizePanel(source, PANEL_MARGIN_PIXELS, PANEL_MARGIN_PIXELS, PANEL_MARGIN_PIXELS, PANEL_MARGIN_PIXELS, false);
    }

    private static PanelImage localizePanel(Mat source,
                                            int marginLeft,
                                            int marginTop,
                                            int marginRight,
                                            int marginBottom,
                                            boolean rejectExternalChrome) {
        int rows = source.rows();
        int cols = source.cols();
        int[] rowActivity = new int[rows];
        int[] columnActivity = new int[cols];

        BytePointer data = source.data();
        long step = source.step();
        int channels = source.channels();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Pixel pixel = pixelAt(data, step, channels, x, y);
                if (isPanelSignal(pixel)) {
                    rowActivity[y]++;
                    columnActivity[x]++;
                }
            }
        }

        Range xRange = denseRange(columnActivity, Math.max(2, rows / 150), cols);
        Range yRange = denseRange(rowActivity, Math.max(2, cols / 150), rows);
        if (xRange.isEmpty() || yRange.isEmpty()) {
            return new PanelImage(source.clone(), 0, 0);
        }
        yRange = new Range(refinePanelTop(source, xRange, yRange, rejectExternalChrome), yRange.endExclusive());

        int x = Math.max(0, xRange.start() - marginLeft);
        int y = Math.max(0, yRange.start() - marginTop);
        int right = Math.min(cols, xRange.endExclusive() + marginRight);
        int bottom = Math.min(rows, yRange.endExclusive() + marginBottom);
        if (right - x < Math.min(16, cols) || bottom - y < Math.min(16, rows)) {
            return new PanelImage(source.clone(), 0, 0);
        }

        try (Rect region = new Rect(x, y, right - x, bottom - y);
             Mat panel = new Mat(source, region)) {
            return new PanelImage(panel.clone(), x, y);
        }
    }

    private static int refinePanelTop(Mat source, Range xRange, Range yRange, boolean rejectExternalChrome) {
        int width = Math.max(1, xRange.endExclusive() - xRange.start());
        int requiredCoverage = Math.max(8, width * 55 / 100);
        int requiredEdge = Math.max(8, width / 7);
        BytePointer data = source.data();
        long step = source.step();
        int channels = source.channels();
        int[] coverageByRow = new int[source.rows()];
        int[] edgeByRow = new int[source.rows()];
        int[] tooltipBodyByRow = new int[source.rows()];
        int[] neutralChromeByRow = new int[source.rows()];
        int centerStart = xRange.start() + width / 5;
        int centerEnd = xRange.endExclusive() - width / 5;
        for (int y = yRange.start(); y < yRange.endExclusive(); y++) {
            int coverage = 0;
            int edge = 0;
            int tooltipBody = 0;
            int neutralChrome = 0;
            for (int x = xRange.start(); x < xRange.endExclusive(); x++) {
                Pixel pixel = pixelAt(data, step, channels, x, y);
                if (isPanelSignal(pixel)) {
                    coverage++;
                }
                if (x >= centerStart && x < centerEnd && isTooltipBodyPixel(pixel)) {
                    tooltipBody++;
                }
                if (x >= centerStart && x < centerEnd && isNeutralChromePixel(pixel)) {
                    neutralChrome++;
                }
                if (y + 1 < source.rows()) {
                    Pixel below = pixelAt(data, step, channels, x, y + 1);
                    if (Math.abs(luma(pixel) - luma(below)) >= 18) {
                        edge++;
                    }
                }
            }
            coverageByRow[y] = coverage;
            edgeByRow[y] = edge;
            tooltipBodyByRow[y] = tooltipBody;
            neutralChromeByRow[y] = neutralChrome;
        }
        int firstPanelSignal = -1;
        int firstTooltipBody = -1;
        if (rejectExternalChrome) {
            firstTooltipBody = findPanelStartAfterExternalChrome(
                    coverageByRow,
                    edgeByRow,
                    tooltipBodyByRow,
                    neutralChromeByRow,
                    xRange,
                    yRange,
                    requiredCoverage,
                    requiredEdge
            );
        }
        for (int y = yRange.start(); y < yRange.endExclusive(); y++) {
            if (hasSustainedCoverage(coverageByRow, y, yRange.endExclusive(), requiredCoverage, 3)
                    || (coverageByRow[y] >= width * 35 / 100
                    && edgeByRow[y] >= requiredEdge
                    && hasSustainedCoverage(coverageByRow, y + 1, yRange.endExclusive(), requiredCoverage, 2))) {
                if (firstPanelSignal < 0) {
                    firstPanelSignal = y;
                }
                if (!rejectExternalChrome) {
                    break;
                }
            }
        }
        if (rejectExternalChrome && firstTooltipBody >= 0) {
            return firstTooltipBody;
        }
        return firstPanelSignal >= 0 ? firstPanelSignal : yRange.start();
    }

    private static int findPanelStartAfterExternalChrome(int[] coverageByRow,
                                                         int[] edgeByRow,
                                                         int[] tooltipBodyByRow,
                                                         int[] neutralChromeByRow,
                                                         Range xRange,
                                                         Range yRange,
                                                         int requiredCoverage,
                                                         int requiredEdge) {
        int width = Math.max(1, xRange.endExclusive() - xRange.start());
        int centerWidth = Math.max(1, width * 3 / 5);
        int lowBodyThreshold = Math.max(2, centerWidth * 18 / 100);
        int highBodyThreshold = Math.max(4, centerWidth * 55 / 100);
        int neutralChromeThreshold = Math.max(4, centerWidth * 24 / 100);
        int lowRun = 0;
        int neutralChromeRun = 0;
        boolean seenNeutralChrome = false;
        boolean seenExternalGap = false;
        for (int y = yRange.start(); y < yRange.endExclusive(); y++) {
            if (neutralChromeByRow[y] >= neutralChromeThreshold) {
                neutralChromeRun++;
            } else {
                neutralChromeRun = 0;
            }
            if (neutralChromeRun >= 3) {
                seenNeutralChrome = true;
            }
            if (tooltipBodyByRow[y] <= lowBodyThreshold) {
                lowRun++;
            } else {
                lowRun = 0;
            }
            if (seenNeutralChrome && lowRun >= 5) {
                seenExternalGap = true;
            }
            if (!seenExternalGap) {
                continue;
            }
            boolean panelSignal = hasSustainedCoverage(coverageByRow, y, yRange.endExclusive(), requiredCoverage, 3)
                    || (coverageByRow[y] >= width * 35 / 100
                    && edgeByRow[y] >= requiredEdge
                    && hasSustainedCoverage(coverageByRow, y + 1, yRange.endExclusive(), requiredCoverage, 2));
            if (panelSignal && hasSustainedCoverage(tooltipBodyByRow, y, yRange.endExclusive(), highBodyThreshold, 3)) {
                return y;
            }
        }
        return -1;
    }

    private static boolean hasSustainedCoverage(int[] coverageByRow,
                                                int startY,
                                                int endY,
                                                int requiredCoverage,
                                                int requiredRows) {
        if (startY < 0 || startY + requiredRows > endY) {
            return false;
        }
        for (int y = startY; y < startY + requiredRows; y++) {
            if (coverageByRow[y] < requiredCoverage) {
                return false;
            }
        }
        return true;
    }

    private static int luma(Pixel pixel) {
        return (pixel.red() * 299 + pixel.green() * 587 + pixel.blue() * 114) / 1000;
    }

    private static boolean isTooltipBodyPixel(Pixel pixel) {
        int luma = luma(pixel);
        int max = Math.max(pixel.red(), Math.max(pixel.green(), pixel.blue()));
        return luma >= 18 && luma <= 115 && max > 24;
    }

    private static boolean isNeutralChromePixel(Pixel pixel) {
        int luma = luma(pixel);
        int max = Math.max(pixel.red(), Math.max(pixel.green(), pixel.blue()));
        int min = Math.min(pixel.red(), Math.min(pixel.green(), pixel.blue()));
        return luma >= 120 && max - min <= 55;
    }

    private static boolean isPanelSignal(Pixel pixel) {
        int max = Math.max(pixel.red(), Math.max(pixel.green(), pixel.blue()));
        int min = Math.min(pixel.red(), Math.min(pixel.green(), pixel.blue()));
        return max > 28 || (max > 18 && max - min > 18);
    }

    private static Range denseRange(int[] activity, int threshold, int fullLength) {
        int bestStart = -1;
        int bestEnd = -1;
        int currentStart = -1;
        int inactiveGap = 0;
        for (int index = 0; index < activity.length; index++) {
            boolean active = smoothedActivity(activity, index) >= threshold;
            if (active) {
                if (currentStart < 0) {
                    currentStart = index;
                }
                inactiveGap = 0;
                continue;
            }
            if (currentStart >= 0) {
                inactiveGap++;
                if (inactiveGap > PANEL_MAX_INACTIVE_GAP) {
                    int end = index - inactiveGap + 1;
                    if (end - currentStart > bestEnd - bestStart) {
                        bestStart = currentStart;
                        bestEnd = end;
                    }
                    currentStart = -1;
                    inactiveGap = 0;
                }
            }
        }
        if (currentStart >= 0) {
            int end = activity.length - inactiveGap;
            if (end - currentStart > bestEnd - bestStart) {
                bestStart = currentStart;
                bestEnd = end;
            }
        }
        if (bestStart < 0 || bestEnd <= bestStart || bestEnd - bestStart < Math.min(16, fullLength)) {
            return Range.empty();
        }
        return new Range(bestStart, bestEnd);
    }

    private static int smoothedActivity(int[] activity, int index) {
        int sum = 0;
        int start = Math.max(0, index - PANEL_ACTIVITY_RADIUS);
        int end = Math.min(activity.length, index + PANEL_ACTIVITY_RADIUS + 1);
        for (int cursor = start; cursor < end; cursor++) {
            sum += activity[cursor];
        }
        return sum;
    }

    private static StitchMatch findBestMatch(PanelImage firstPanel, PanelImage secondPanel, int pairIndex) {
        FeatureImage first = firstPanel.features();
        FeatureImage second = secondPanel.features();
        int minSide = Math.min(first.width(), second.width());
        int minCompareWidth = Math.min(minSide, Math.max(12, minSide / 3));
        int maxOverlap = Math.min(first.height(), second.height()) - 1;
        if (maxOverlap < MIN_OVERLAP_PIXELS || minCompareWidth <= 0) {
            throw noConfidentOverlap(pairIndex, null);
        }

        int maxHorizontalShift = Math.min(MAX_HORIZONTAL_SHIFT_PIXELS, Math.max(first.width(), second.width()) / 2);
        CandidateMatch best = searchBestCandidate(first, second, minCompareWidth, MIN_OVERLAP_PIXELS, maxOverlap,
                -maxHorizontalShift, maxHorizontalShift, OVERLAP_SEARCH_STEP, OFFSET_SEARCH_STEP, null);
        if (best != null) {
            best = searchBestCandidate(first, second, minCompareWidth,
                    Math.max(MIN_OVERLAP_PIXELS, best.overlapHeight() - 4),
                    Math.min(maxOverlap, best.overlapHeight() + 4),
                    best.offsetX() - 4,
                    best.offsetX() + 4,
                    1,
                    1,
                    best);
        }

        if (best == null || best.confidence() < MIN_STITCH_CONFIDENCE) {
            throw noConfidentOverlap(pairIndex, best);
        }

        int seamY = chooseSeam(first, second, best.offsetX(), best.overlapHeight());
        return new StitchMatch(
                best.offsetX(),
                first.height() - best.overlapHeight(),
                best.overlapHeight(),
                seamY,
                best.confidence()
        );
    }

    private static CandidateMatch searchBestCandidate(FeatureImage first,
                                                      FeatureImage second,
                                                      int minCompareWidth,
                                                      int minOverlap,
                                                      int maxOverlap,
                                                      int minOffsetX,
                                                      int maxOffsetX,
                                                      int overlapStep,
                                                      int offsetStep,
                                                      CandidateMatch initialBest) {
        CandidateMatch best = initialBest;
        for (int overlapHeight = minOverlap; overlapHeight <= maxOverlap; overlapHeight += overlapStep) {
            for (int offsetX = minOffsetX; offsetX <= maxOffsetX; offsetX += offsetStep) {
                int firstX = Math.max(0, offsetX);
                int secondX = Math.max(0, -offsetX);
                int compareWidth = Math.min(first.width() - firstX, second.width() - secondX);
                if (compareWidth < minCompareWidth) {
                    continue;
                }
                CandidateScore score = scoreOverlap(first, second, firstX, secondX, compareWidth, overlapHeight);
                CandidateMatch candidate = new CandidateMatch(
                        offsetX,
                        overlapHeight,
                        score.confidence(),
                        rankingScore(score.confidence(), overlapHeight, maxOverlap, offsetX)
                );
                if (best == null || candidate.rankingScore() > best.rankingScore()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static CandidateScore scoreOverlap(FeatureImage first,
                                               FeatureImage second,
                                               int firstX,
                                               int secondX,
                                               int compareWidth,
                                               int overlapHeight) {
        long diffSum = 0L;
        long textureSum = 0L;
        int textureSamples = 0;
        int samples = 0;
        int possibleSamples = 0;
        int firstTop = first.height() - overlapHeight;
        for (int y = 0; y < overlapHeight; y += SAMPLE_STEP) {
            for (int x = 0; x < compareWidth; x += SAMPLE_STEP) {
                possibleSamples++;
                if (!first.hasSignal(firstX + x, firstTop + y) || !second.hasSignal(secondX + x, y)) {
                    continue;
                }
                diffSum += first.colorDiff(firstX + x, firstTop + y, second, secondX + x, y);
                samples++;
                if (x + SAMPLE_STEP < compareWidth
                        && first.hasSignal(firstX + x + SAMPLE_STEP, firstTop + y)) {
                    textureSum += first.colorDiff(firstX + x, firstTop + y, first, firstX + x + SAMPLE_STEP, firstTop + y);
                    textureSamples++;
                }
                if (y + SAMPLE_STEP < overlapHeight
                        && first.hasSignal(firstX + x, firstTop + y + SAMPLE_STEP)) {
                    textureSum += first.colorDiff(firstX + x, firstTop + y, first, firstX + x, firstTop + y + SAMPLE_STEP);
                    textureSamples++;
                }
            }
        }
        if (samples < Math.max(8, possibleSamples / 50)) {
            return new CandidateScore(0.0d);
        }
        double similarity = 1.0d - (diffSum / (samples * 765.0d));
        double textureScore = textureSamples == 0 ? 0.0d : textureSum / (textureSamples * 3.0d);
        double textureFactor = Math.min(1.0d, textureScore / MIN_TEXTURE_SCORE);
        return new CandidateScore(Math.max(0.0d, similarity * textureFactor));
    }

    private static double rankingScore(double confidence, int overlapHeight, int maxOverlap, int offsetX) {
        double overlapBonus = maxOverlap <= 0 ? 0.0d : Math.min(0.06d, overlapHeight / (double) maxOverlap * 0.06d);
        double offsetPenalty = Math.abs(offsetX) * 0.0004d;
        return confidence + overlapBonus - offsetPenalty;
    }

    private static int chooseSeam(FeatureImage first, FeatureImage second, int offsetX, int overlapHeight) {
        int firstX = Math.max(0, offsetX);
        int secondX = Math.max(0, -offsetX);
        int compareWidth = Math.min(first.width() - firstX, second.width() - secondX);
        int firstTop = first.height() - overlapHeight;
        int low = Math.max(1, overlapHeight / 5);
        int high = Math.min(overlapHeight - 1, (overlapHeight * 4) / 5);
        if (high <= low) {
            return Math.max(1, overlapHeight / 2);
        }

        int bestY = overlapHeight / 2;
        double bestDiff = Double.MAX_VALUE;
        for (int y = low; y <= high; y++) {
            double rowDiff = averageRowDiff(first, second, firstX, secondX, compareWidth, firstTop, y);
            if (rowDiff < bestDiff) {
                bestDiff = rowDiff;
                bestY = y;
            }
        }
        return bestY;
    }

    private static double averageRowDiff(FeatureImage first,
                                         FeatureImage second,
                                         int firstX,
                                         int secondX,
                                         int compareWidth,
                                         int firstTop,
                                         int y) {
        long diffSum = 0L;
        int samples = 0;
        for (int x = 0; x < compareWidth; x += SAMPLE_STEP) {
            if (!first.hasSignal(firstX + x, firstTop + y) || !second.hasSignal(secondX + x, y)) {
                continue;
            }
            diffSum += first.colorDiff(firstX + x, firstTop + y, second, secondX + x, y);
            samples++;
        }
        return samples == 0 ? Double.MAX_VALUE : diffSum / (samples * 3.0d);
    }

    private static IllegalArgumentException noConfidentOverlap(int pairIndex, CandidateMatch best) {
        String confidence = best == null ? "brak" : String.format(Locale.US, "%.3f", best.confidence());
        String bestCandidate = best == null
                ? ""
                : ", najlepszy overlap=" + best.overlapHeight() + " px, offsetX=" + best.offsetX();
        return new IllegalArgumentException(
                "STITCH_NOT_CONFIDENT: nie znaleziono wiarygodnego overlapu miedzy obrazem #"
                        + (pairIndex + 1)
                        + " i #"
                        + (pairIndex + 2)
                        + " (confidence="
                        + confidence
                        + ", wymagane="
                        + String.format(Locale.US, "%.2f", MIN_STITCH_CONFIDENCE)
                        + bestCandidate
                        + "). Pionowa kompozycja pozostaje tylko artefaktem diagnostycznym."
        );
    }

    private static Mat composeStitchedPanels(List<PanelImage> panels, List<StitchMatch> matches) {
        int count = panels.size();
        int[] globalX = new int[count];
        int[] globalY = new int[count];
        for (int index = 0; index < matches.size(); index++) {
            StitchMatch match = matches.get(index);
            globalX[index + 1] = globalX[index] + match.offsetX();
            globalY[index + 1] = globalY[index] + match.offsetY();
        }

        int minX = 0;
        int maxX = 0;
        int minY = 0;
        int maxY = 0;
        int[] startY = new int[count];
        int[] endY = new int[count];
        for (int index = 0; index < count; index++) {
            PanelImage panel = panels.get(index);
            startY[index] = index == 0 ? 0 : matches.get(index - 1).seamY();
            endY[index] = index == count - 1
                    ? panel.image().rows()
                    : panel.image().rows() - matches.get(index).overlapHeight() + matches.get(index).seamY();
            if (endY[index] <= startY[index]) {
                throw new IllegalStateException("Wybrany seam daje pusty fragment panelu #" + (index + 1) + ".");
            }
            minX = Math.min(minX, globalX[index]);
            maxX = Math.max(maxX, globalX[index] + panel.image().cols());
            minY = Math.min(minY, globalY[index] + startY[index]);
            maxY = Math.max(maxY, globalY[index] + endY[index]);
        }

        Mat assembled = new Mat(maxY - minY, maxX - minX, panels.getFirst().image().type(), new Scalar(0.0, 0.0, 0.0, 0.0));
        for (int index = 0; index < count; index++) {
            PanelImage panel = panels.get(index);
            int segmentHeight = endY[index] - startY[index];
            try (Rect sourceRegion = new Rect(0, startY[index], panel.image().cols(), segmentHeight);
                 Mat source = new Mat(panel.image(), sourceRegion);
                 Rect targetRegion = new Rect(globalX[index] - minX, globalY[index] + startY[index] - minY,
                         panel.image().cols(), segmentHeight);
                 Mat target = new Mat(assembled, targetRegion)) {
                source.copyTo(target);
            }
        }
        return assembled;
    }

    private static Mat buildRawComposition(List<Mat> images) {
        int maxWidth = 0;
        int totalHeight = 0;
        for (Mat image : images) {
            maxWidth = Math.max(maxWidth, image.cols());
            totalHeight += image.rows();
        }
        Mat assembled = new Mat(totalHeight, maxWidth, images.getFirst().type(), new Scalar(0.0, 0.0, 0.0, 0.0));
        int y = 0;
        for (Mat image : images) {
            try (Rect region = new Rect(0, y, image.cols(), image.rows());
                 Mat target = new Mat(assembled, region)) {
                image.copyTo(target);
            }
            y += image.rows();
        }
        return assembled;
    }

    private static Mat buildOverlapDebugImage(PanelImage first, PanelImage second, StitchMatch match) {
        int height = match.overlapHeight();
        int width = first.image().cols() + second.image().cols();
        Mat debug = new Mat(height, width, first.image().type(), new Scalar(0.0, 0.0, 0.0, 0.0));
        try (Rect firstSourceRegion = new Rect(0, first.image().rows() - height, first.image().cols(), height);
             Rect firstTargetRegion = new Rect(0, 0, first.image().cols(), height);
             Mat firstSource = new Mat(first.image(), firstSourceRegion);
             Mat firstTarget = new Mat(debug, firstTargetRegion);
             Rect secondSourceRegion = new Rect(0, 0, second.image().cols(), height);
             Rect secondTargetRegion = new Rect(first.image().cols(), 0, second.image().cols(), height);
             Mat secondSource = new Mat(second.image(), secondSourceRegion);
             Mat secondTarget = new Mat(debug, secondTargetRegion)) {
            firstSource.copyTo(firstTarget);
            secondSource.copyTo(secondTarget);
        }
        try (Point leftStart = new Point(0, match.seamY());
             Point leftEnd = new Point(Math.max(0, first.image().cols() - 1), match.seamY());
             Point rightStart = new Point(first.image().cols(), match.seamY());
             Point rightEnd = new Point(width - 1, match.seamY());
             Scalar seamColor = new Scalar(0.0, 0.0, 255.0, 0.0)) {
            line(debug, leftStart, leftEnd, seamColor, 1, 8, 0);
            line(debug, rightStart, rightEnd, seamColor, 1, 8, 0);
        }
        return debug;
    }

    private static void writeDebugPng(Mat image, Path outputPath) {
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputPath, encodePng(image));
        } catch (IOException | RuntimeException exception) {
            AssembledItemImageDebugWriter.logDebugArtifactWarning(outputPath, exception);
        }
    }

    private static Path debugPath(String filename) {
        return AssembledItemImageDebugWriter.debugPath(filename);
    }

    private static byte[] encodePng(Mat image) {
        try (BytePointer encoded = new BytePointer()) {
            boolean encodedSuccessfully = imencode(".png", image, encoded);
            if (!encodedSuccessfully || encoded.limit() <= 0) {
                throw new IllegalStateException("Nie udalo sie zakodowac zlozonego obrazu itemu do PNG.");
            }
            byte[] bytes = new byte[(int) encoded.limit()];
            encoded.get(bytes);
            return bytes;
        }
    }

    private static Pixel pixelAt(BytePointer data, long step, int channels, int x, int y) {
        long offset = (long) y * step + (long) x * channels;
        int blue = data.get(offset) & 0xff;
        if (channels == 1) {
            return new Pixel(blue, blue, blue);
        }
        int green = data.get(offset + 1L) & 0xff;
        int red = data.get(offset + 2L) & 0xff;
        return new Pixel(red, green, blue);
    }

    private record PanelImage(Mat image, int sourceX, int sourceY) implements AutoCloseable {
        private FeatureImage features() {
            return FeatureImage.from(image);
        }

        @Override
        public void close() {
            image.close();
        }
    }

    private record FeatureImage(int width, int height, byte[] red, byte[] green, byte[] blue, boolean[] signal) {
        private static FeatureImage from(Mat image) {
            int width = image.cols();
            int height = image.rows();
            byte[] red = new byte[width * height];
            byte[] green = new byte[width * height];
            byte[] blue = new byte[width * height];
            boolean[] signal = new boolean[width * height];
            BytePointer data = image.data();
            long step = image.step();
            int channels = image.channels();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Pixel pixel = pixelAt(data, step, channels, x, y);
                    int index = y * width + x;
                    red[index] = (byte) pixel.red();
                    green[index] = (byte) pixel.green();
                    blue[index] = (byte) pixel.blue();
                    signal[index] = isPanelSignal(pixel);
                }
            }
            return new FeatureImage(width, height, red, green, blue, signal);
        }

        private boolean hasSignal(int x, int y) {
            return signal[y * width + x];
        }

        private int colorDiff(int x, int y, FeatureImage other, int otherX, int otherY) {
            int index = y * width + x;
            int otherIndex = otherY * other.width + otherX;
            return Math.abs((red[index] & 0xff) - (other.red[otherIndex] & 0xff))
                    + Math.abs((green[index] & 0xff) - (other.green[otherIndex] & 0xff))
                    + Math.abs((blue[index] & 0xff) - (other.blue[otherIndex] & 0xff));
        }
    }

    private record Pixel(int red, int green, int blue) {
    }

    private record Range(int start, int endExclusive) {
        private static Range empty() {
            return new Range(-1, -1);
        }

        private boolean isEmpty() {
            return start < 0 || endExclusive <= start;
        }
    }

    private record CandidateScore(double confidence) {
    }

    private record CandidateMatch(int offsetX, int overlapHeight, double confidence, double rankingScore) {
    }

    private record StitchMatch(int offsetX, int offsetY, int overlapHeight, int seamY, double confidence) {
        private StitchDiagnostic toDiagnostic(int pairIndex) {
            return new StitchDiagnostic(
                    pairIndex + 1,
                    pairIndex + 2,
                    offsetX,
                    offsetY,
                    overlapHeight,
                    seamY,
                    confidence,
                    "OK"
            );
        }
    }
}
