package krys.itemimport.image.opencv;

import krys.itemimport.image.AssembledItemImage;
import krys.itemimport.image.HorizontalDivider;
import krys.itemimport.image.HorizontalDividerDetectionResult;
import krys.itemimport.image.HorizontalDividerDetector;
import krys.itemimport.image.ItemImagePart;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgproc.line;

/** OpenCV-owa detekcja strukturalnych poziomych separatorow itemu. */
public final class OpenCvHorizontalDividerDetector implements HorizontalDividerDetector {
    private static final String OUTPUT_FORMAT = "PNG";
    private static final int ACTIVITY_RADIUS = 4;
    private static final int MAX_INACTIVE_GAP = 14;
    private static final int ROI_MARGIN_PIXELS = 4;
    private static final double MIN_DIVIDER_STRENGTH = 0.48d;

    @Override
    public HorizontalDividerDetectionResult detect(AssembledItemImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Brak finalnego obrazu itemu do detekcji separatorow.");
        }
        try (Mat source = decode(image)) {
            FeatureImage features = FeatureImage.from(source);
            Bounds analysisBounds = findAnalysisBounds(features);
            List<HorizontalDivider> dividers = detectDividers(features, analysisBounds);
            List<ItemImagePart> parts = buildParts(source, dividers);
            byte[] overlayImageBytes;
            try (Mat overlay = buildOverlay(source, dividers)) {
                overlayImageBytes = encodePng(overlay);
            }
            return new HorizontalDividerDetectionResult(
                    dividers,
                    parts,
                    overlayImageBytes,
                    OUTPUT_FORMAT,
                    source.cols(),
                    source.rows()
            );
        }
    }

    private static Mat decode(AssembledItemImage image) {
        byte[] imageBytes = image.getImageBytes();
        try (BytePointer pointer = new BytePointer(imageBytes);
             Mat encoded = new Mat(1, imageBytes.length, CV_8UC1, pointer)) {
            Mat decoded = imdecode(encoded, IMREAD_COLOR);
            if (decoded == null || decoded.empty()) {
                if (decoded != null) {
                    decoded.close();
                }
                throw new IllegalArgumentException("Nie udalo sie zdekodowac finalnego obrazu itemu do detekcji separatorow.");
            }
            return decoded;
        }
    }

    private static Bounds findAnalysisBounds(FeatureImage features) {
        int[] rowActivity = new int[features.height()];
        int[] columnActivity = new int[features.width()];
        for (int y = 0; y < features.height(); y++) {
            for (int x = 0; x < features.width(); x++) {
                if (features.hasPanelSignal(x, y)) {
                    rowActivity[y]++;
                    columnActivity[x]++;
                }
            }
        }

        Range xRange = denseRange(columnActivity, Math.max(2, features.height() / 150), features.width());
        Range yRange = denseRange(rowActivity, Math.max(2, features.width() / 150), features.height());
        if (xRange.isEmpty() || yRange.isEmpty()) {
            return new Bounds(0, 0, features.width(), features.height());
        }

        int x = Math.max(0, xRange.start() - ROI_MARGIN_PIXELS);
        int y = Math.max(0, yRange.start() - ROI_MARGIN_PIXELS);
        int right = Math.min(features.width(), xRange.endExclusive() + ROI_MARGIN_PIXELS);
        int bottom = Math.min(features.height(), yRange.endExclusive() + ROI_MARGIN_PIXELS);
        if (right - x < Math.min(16, features.width()) || bottom - y < Math.min(16, features.height())) {
            return new Bounds(0, 0, features.width(), features.height());
        }
        return new Bounds(x, y, right, bottom);
    }

    private static List<HorizontalDivider> detectDividers(FeatureImage features, Bounds bounds) {
        int analysisWidth = bounds.width();
        int analysisHeight = bounds.height();
        if (analysisWidth < 24 || analysisHeight < 32) {
            return List.of();
        }

        int topGuard = Math.max(20, analysisHeight / 12);
        int bottomGuard = Math.max(20, analysisHeight / 12);
        int sideGuard = Math.max(4, analysisWidth / 90);
        int scanStartY = Math.max(bounds.top() + topGuard, 2);
        int scanEndY = Math.min(bounds.bottom() - bottomGuard, features.height() - 2);
        int scanStartX = Math.max(bounds.left() + sideGuard, 2);
        int scanEndX = Math.min(bounds.right() - sideGuard, features.width() - 2);
        if (scanEndY <= scanStartY || scanEndX <= scanStartX) {
            return List.of();
        }

        List<RowCandidate> rowCandidates = new ArrayList<>();
        for (int y = scanStartY; y < scanEndY; y++) {
            RowScore score = scoreRow(features, y, scanStartX, scanEndX);
            if (score.strength() >= MIN_DIVIDER_STRENGTH) {
                rowCandidates.add(new RowCandidate(y, score.strength(), score.coverage(), score.supportWidth()));
            }
        }
        if (rowCandidates.isEmpty()) {
            return List.of();
        }

        List<RowCluster> clusters = clusterRows(rowCandidates, Math.max(3, analysisHeight / 230));
        List<HorizontalDivider> dividers = new ArrayList<>();
        for (RowCluster cluster : clusters) {
            int bandStart = cluster.startY();
            int bandEnd = cluster.endYExclusive();
            int bandHeight = bandEnd - bandStart;
            int maxBandHeight = Math.max(8, analysisHeight / 42);
            if (bandHeight > maxBandHeight) {
                continue;
            }
            int y = cluster.weightedY();
            if (y <= 0 || y >= features.height() || y <= bounds.top() + topGuard || y >= bounds.bottom() - bottomGuard) {
                continue;
            }
            dividers.add(new HorizontalDivider(y, bandStart, bandEnd, clamp(cluster.strength())));
        }

        List<HorizontalDivider> sortedDividers = dividers.stream()
                .sorted(Comparator.comparingInt(HorizontalDivider::getY))
                .toList();
        return mergeCloseDividers(sortedDividers, Math.max(18, analysisHeight / 28));
    }

    private static List<HorizontalDivider> mergeCloseDividers(List<HorizontalDivider> dividers, int maxDistance) {
        if (dividers.isEmpty()) {
            return List.of();
        }
        List<HorizontalDivider> merged = new ArrayList<>();
        HorizontalDivider current = dividers.getFirst();
        for (int index = 1; index < dividers.size(); index++) {
            HorizontalDivider next = dividers.get(index);
            if (next.getY() - current.getY() <= maxDistance) {
                int bandStart = Math.min(current.getBandStartY(), next.getBandStartY());
                int bandEnd = Math.max(current.getBandEndY(), next.getBandEndY());
                HorizontalDivider representative = next.getStrength() >= current.getStrength() ? next : current;
                current = new HorizontalDivider(
                        representative.getY(),
                        Math.min(bandStart, representative.getY()),
                        Math.max(bandEnd, representative.getY() + 1),
                        Math.max(current.getStrength(), next.getStrength())
                );
                continue;
            }
            merged.add(current);
            current = next;
        }
        merged.add(current);
        return merged;
    }

    private static RowScore scoreRow(FeatureImage features, int y, int startX, int endX) {
        int width = endX - startX;
        boolean[] active = new boolean[width];
        double edgeSum = 0.0d;
        int activePixels = 0;
        int contrastThreshold = Math.max(8, Math.min(28, features.averageVerticalContrast(y, startX, endX) * 2));
        for (int x = startX; x < endX; x++) {
            int localEdge = features.localVerticalContrast(x, y);
            edgeSum += localEdge;
            int localPeak = features.localPeakContrast(x, y);
            boolean signal = features.hasPanelSignal(x, y)
                    && (localEdge >= contrastThreshold || localPeak >= Math.max(7, contrastThreshold - 4));
            if (signal) {
                active[x - startX] = true;
                activePixels++;
            }
        }
        if (activePixels == 0) {
            return RowScore.empty();
        }

        boolean[] closed = closeSmallHorizontalGaps(active, Math.max(3, width / 80));
        double rawCoverage = activePixels / (double) width;
        int rawLongestRun = longestRun(active);
        int closedPixels = countTrue(closed);
        int longestRun = longestRun(closed);
        int leftRun = longestRun(closed, 0, Math.max(1, width * 44 / 100));
        int rightRun = longestRun(closed, Math.min(width - 1, width * 56 / 100), width);
        int leftSupport = countTrue(closed, 0, Math.max(1, width * 44 / 100));
        int rightSupport = countTrue(closed, Math.min(width - 1, width * 56 / 100), width);
        int edgeBandWidth = Math.max(1, width * 22 / 100);
        int farLeftRun = longestRun(closed, 0, edgeBandWidth);
        int farRightRun = longestRun(closed, width - edgeBandWidth, width);

        double coverage = closedPixels / (double) width;
        double lineRunSupport = Math.max(longestRun / Math.max(1.0d, width * 0.58d),
                (leftRun + rightRun) / Math.max(1.0d, width * 0.48d));
        double sideSupport = Math.min(leftSupport, rightSupport) / Math.max(1.0d, width * 0.14d);
        boolean spansBothSides = leftSupport >= width * 0.08d && rightSupport >= width * 0.08d;
        boolean spansLongLine = longestRun >= width * 0.42d && coverage >= 0.15d;
        boolean reachesOuterSides = farLeftRun >= width * 0.07d && farRightRun >= width * 0.07d;
        boolean continuousRawLine = rawLongestRun >= width * 0.24d && rawCoverage >= 0.14d;
        boolean supportedAcrossSides = reachesOuterSides && rawLongestRun >= width * 0.12d && rawCoverage >= 0.16d;
        if ((!continuousRawLine && !supportedAcrossSides) || (!spansBothSides && !spansLongLine)) {
            return RowScore.empty();
        }

        double averageEdge = edgeSum / width;
        double edgeScore = clamp((averageEdge - 4.0d) / 18.0d);
        double coverageScore = clamp((coverage - 0.11d) / 0.26d);
        double runScore = clamp(lineRunSupport);
        double sideScore = clamp(sideSupport);
        double strength = clamp(edgeScore * 0.30d + coverageScore * 0.23d + runScore * 0.31d + sideScore * 0.16d);
        return new RowScore(strength, coverage, Math.max(longestRun, leftRun + rightRun));
    }

    private static List<RowCluster> clusterRows(List<RowCandidate> candidates, int maxGap) {
        List<RowCluster> clusters = new ArrayList<>();
        RowCluster current = null;
        for (RowCandidate candidate : candidates) {
            if (current == null || candidate.y() - current.lastY() > maxGap) {
                current = new RowCluster(candidate);
                clusters.add(current);
            } else {
                current.add(candidate);
            }
        }
        return clusters;
    }

    private static List<ItemImagePart> buildParts(Mat source, List<HorizontalDivider> dividers) {
        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(0);
        for (HorizontalDivider divider : dividers) {
            if (divider.getY() > 0 && divider.getY() < source.rows()
                    && divider.getY() > boundaries.getLast()) {
                boundaries.add(divider.getY());
            }
        }
        boundaries.add(source.rows());

        List<ItemImagePart> parts = new ArrayList<>();
        for (int index = 0; index < boundaries.size() - 1; index++) {
            int startY = boundaries.get(index);
            int endY = boundaries.get(index + 1);
            int height = endY - startY;
            if (height <= 0) {
                continue;
            }
            byte[] partBytes;
            try (Rect region = new Rect(0, startY, source.cols(), height);
                 Mat part = new Mat(source, region)) {
                partBytes = encodePng(part);
            }
            parts.add(new ItemImagePart(
                    index + 1,
                    "PART_" + (index + 1),
                    startY,
                    endY,
                    source.cols(),
                    height,
                    OUTPUT_FORMAT,
                    partBytes
            ));
        }
        return parts;
    }

    private static Mat buildOverlay(Mat source, List<HorizontalDivider> dividers) {
        Mat overlay = source.clone();
        for (HorizontalDivider divider : dividers) {
            try (Point start = new Point(0, divider.getY());
                 Point end = new Point(Math.max(0, overlay.cols() - 1), divider.getY());
                 Scalar lineColor = new Scalar(0.0, 255.0, 255.0, 0.0)) {
                line(overlay, start, end, lineColor, 2, 8, 0);
            }
        }
        return overlay;
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
                if (inactiveGap > MAX_INACTIVE_GAP) {
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
        int start = Math.max(0, index - ACTIVITY_RADIUS);
        int end = Math.min(activity.length, index + ACTIVITY_RADIUS + 1);
        for (int cursor = start; cursor < end; cursor++) {
            sum += activity[cursor];
        }
        return sum;
    }

    private static boolean[] closeSmallHorizontalGaps(boolean[] source, int maxGap) {
        boolean[] result = source.clone();
        int gapStart = -1;
        int lastSignal = -1;
        for (int index = 0; index < result.length; index++) {
            if (result[index]) {
                if (gapStart >= 0 && lastSignal >= 0 && index - gapStart <= maxGap) {
                    for (int cursor = gapStart; cursor < index; cursor++) {
                        result[cursor] = true;
                    }
                }
                gapStart = -1;
                lastSignal = index;
            } else if (lastSignal >= 0 && gapStart < 0) {
                gapStart = index;
            }
        }
        return result;
    }

    private static int countTrue(boolean[] values) {
        return countTrue(values, 0, values.length);
    }

    private static int countTrue(boolean[] values, int start, int end) {
        int count = 0;
        int safeStart = Math.max(0, start);
        int safeEnd = Math.min(values.length, end);
        for (int index = safeStart; index < safeEnd; index++) {
            if (values[index]) {
                count++;
            }
        }
        return count;
    }

    private static int longestRun(boolean[] values) {
        return longestRun(values, 0, values.length);
    }

    private static int longestRun(boolean[] values, int start, int end) {
        int longest = 0;
        int current = 0;
        int safeStart = Math.max(0, start);
        int safeEnd = Math.min(values.length, end);
        for (int index = safeStart; index < safeEnd; index++) {
            if (values[index]) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return longest;
    }

    private static byte[] encodePng(Mat image) {
        try (BytePointer encoded = new BytePointer()) {
            boolean encodedSuccessfully = imencode(".png", image, encoded);
            if (!encodedSuccessfully || encoded.limit() <= 0) {
                throw new IllegalStateException("Nie udalo sie zakodowac obrazu separatorow do PNG.");
            }
            byte[] bytes = new byte[(int) encoded.limit()];
            encoded.get(bytes);
            return bytes;
        }
    }

    private static double clamp(double value) {
        if (value < 0.0d) {
            return 0.0d;
        }
        if (value > 1.0d) {
            return 1.0d;
        }
        return value;
    }

    private record FeatureImage(int width, int height, byte[] luminance, boolean[] panelSignal) {
        private static FeatureImage from(Mat image) {
            int width = image.cols();
            int height = image.rows();
            byte[] luminance = new byte[width * height];
            boolean[] panelSignal = new boolean[width * height];
            BytePointer data = image.data();
            long step = image.step();
            int channels = image.channels();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    long offset = (long) y * step + (long) x * channels;
                    int blue = data.get(offset) & 0xff;
                    int green = channels == 1 ? blue : data.get(offset + 1L) & 0xff;
                    int red = channels == 1 ? blue : data.get(offset + 2L) & 0xff;
                    int max = Math.max(red, Math.max(green, blue));
                    int min = Math.min(red, Math.min(green, blue));
                    int luma = (red * 299 + green * 587 + blue * 114) / 1000;
                    int index = y * width + x;
                    luminance[index] = (byte) luma;
                    panelSignal[index] = max > 28 || (max > 18 && max - min > 18);
                }
            }
            return new FeatureImage(width, height, luminance, panelSignal);
        }

        private boolean hasPanelSignal(int x, int y) {
            return panelSignal[y * width + x];
        }

        private int luma(int x, int y) {
            return luminance[y * width + x] & 0xff;
        }

        private int localVerticalContrast(int x, int y) {
            int center = luma(x, y);
            int up = luma(x, Math.max(0, y - 1));
            int down = luma(x, Math.min(height - 1, y + 1));
            return Math.max(Math.abs(center - up), Math.abs(center - down));
        }

        private int localPeakContrast(int x, int y) {
            int center = luma(x, y);
            int up = luma(x, Math.max(0, y - 2));
            int down = luma(x, Math.min(height - 1, y + 2));
            int outsideAverage = (up + down) / 2;
            return Math.abs(center - outsideAverage);
        }

        private int averageVerticalContrast(int y, int startX, int endX) {
            int sum = 0;
            for (int x = startX; x < endX; x++) {
                sum += localVerticalContrast(x, y);
            }
            return sum / Math.max(1, endX - startX);
        }
    }

    private record Bounds(int left, int top, int right, int bottom) {
        private int width() {
            return right - left;
        }

        private int height() {
            return bottom - top;
        }
    }

    private record Range(int start, int endExclusive) {
        private static Range empty() {
            return new Range(-1, -1);
        }

        private boolean isEmpty() {
            return start < 0 || endExclusive <= start;
        }
    }

    private record RowScore(double strength, double coverage, int supportWidth) {
        private static RowScore empty() {
            return new RowScore(0.0d, 0.0d, 0);
        }
    }

    private record RowCandidate(int y, double strength, double coverage, int supportWidth) {
    }

    private static final class RowCluster {
        private final List<RowCandidate> candidates = new ArrayList<>();

        private RowCluster(RowCandidate firstCandidate) {
            candidates.add(firstCandidate);
        }

        private void add(RowCandidate candidate) {
            candidates.add(candidate);
        }

        private int startY() {
            return candidates.getFirst().y();
        }

        private int endYExclusive() {
            return candidates.getLast().y() + 1;
        }

        private int lastY() {
            return candidates.getLast().y();
        }

        private int weightedY() {
            double weightedSum = 0.0d;
            double weight = 0.0d;
            for (RowCandidate candidate : candidates) {
                double candidateWeight = candidate.strength() * (1.0d + candidate.coverage())
                        * Math.max(1, candidate.supportWidth());
                weightedSum += candidate.y() * candidateWeight;
                weight += candidateWeight;
            }
            return (int) Math.round(weightedSum / Math.max(0.0001d, weight));
        }

        private double strength() {
            double best = 0.0d;
            for (RowCandidate candidate : candidates) {
                best = Math.max(best, candidate.strength());
            }
            return best;
        }
    }
}
