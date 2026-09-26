package krys.itemimport.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Generyczna, obrazowa segmentacja PART_2/PART_3 na logiczne bloki właściwości. */
public final class VisualPropertyBlockSegmenter implements PropertyBlockSegmenter {
    private final PropertyBlockAnchorDetector anchorDetector;

    public VisualPropertyBlockSegmenter() {
        this(new VisualPropertyBlockAnchorDetector());
    }

    public VisualPropertyBlockSegmenter(PropertyBlockAnchorDetector anchorDetector) {
        this.anchorDetector = Objects.requireNonNull(anchorDetector, "anchorDetector");
    }

    @Override
    public PropertyBlockSegmentationResult segment(StructuralItemPart part) {
        Objects.requireNonNull(part, "part");
        StructuralItemPartSlot slot = part.getSlot();
        if (slot != StructuralItemPartSlot.PART_2 && slot != StructuralItemPartSlot.PART_3) {
            throw new IllegalArgumentException("Segmentacja bloków właściwości obsługuje tylko PART_2 i PART_3.");
        }
        BufferedImage image = decode(part.getSourcePart());
        SegmentScale scale = SegmentScale.from(image);
        List<TextLine> lines = detectTextLines(image, scale);
        List<PropertyBlockAnchor> anchors = filterAnchorColumn(anchorDetector.detect(image), image.getWidth());
        List<TextLine> segmentationLines = addUnmatchedOversizedSocketLines(lines, anchors, image, scale);
        List<LineWithAnchor> anchoredLines = segmentationLines.stream()
                .map(line -> new LineWithAnchor(line, findAnchorForLine(line, anchors, image.getWidth()).orElse(null)))
                .toList();
        List<BlockDraft> drafts = removeTrailingFlavorAfterSocketPair(
                removeTrailingFlavorTextDrafts(
                        groupBlocks(anchoredLines, image.getWidth(), image.getHeight(), scale),
                        image,
                        scale),
                image.getHeight(),
                scale);
        List<PropertyBlock> blocks = new ArrayList<>();
        int order = 1;
        for (BlockDraft draft : drafts) {
            if (!draft.lines().isEmpty()) {
                blocks.add(toBlock(slot, order++, image, draft, scale));
            }
        }
        String diagnostics = "lines=" + lines.size() + ", anchors=" + anchors.size() + ", blocks=" + blocks.size();
        return new PropertyBlockSegmentationResult(part, blocks, diagnostics);
    }

    private static List<TextLine> addUnmatchedOversizedSocketLines(List<TextLine> lines,
                                                                    List<PropertyBlockAnchor> anchors,
                                                                    BufferedImage image,
                                                                    SegmentScale scale) {
        List<TextLine> result = new ArrayList<>(lines);
        for (PropertyBlockAnchor anchor : anchors) {
            boolean matched = lines.stream()
                    .anyMatch(line -> findAnchorForLine(line, List.of(anchor), image.getWidth()).isPresent());
            if (!matched && isOversizedSocketGeometry(anchor, image.getWidth(), scale)) {
                result.add(expandOversizedSocketLine(image, anchor, scale));
            }
        }
        return result.stream().sorted(Comparator.comparingInt(TextLine::y)).toList();
    }

    private static TextLine expandOversizedSocketLine(BufferedImage image,
                                                       PropertyBlockAnchor anchor,
                                                       SegmentScale scale) {
        int anchorRight = anchor.getX() + anchor.getWidth();
        int lastContentX = anchorRight - 1;
        int maxGap = Math.max(2, (int) Math.round(scale.unit() * 0.55d));
        int gap = 0;
        int endX = image.getWidth() - horizontalScanMargin(image.getWidth());
        int minColumnPixels = Math.max(1, (int) Math.round(anchor.getHeight() * 0.08d));
        for (int x = anchorRight; x < endX; x++) {
            int columnPixels = 0;
            for (int y = anchor.getY(); y < anchor.getY() + anchor.getHeight() && y < image.getHeight(); y++) {
                if (isAnchorGapForeground(image, x, y)) {
                    columnPixels++;
                }
            }
            if (columnPixels >= minColumnPixels) {
                lastContentX = x;
                gap = 0;
            } else {
                gap++;
                if (gap > maxGap) {
                    break;
                }
            }
        }
        return new TextLine(anchor.getX(), anchor.getY(),
                lastContentX - anchor.getX() + 1, anchor.getHeight());
    }

    private static boolean isOversizedSocketGeometry(PropertyBlockAnchor anchor,
                                                       int imageWidth,
                                                       SegmentScale scale) {
        double unit = scale.unit();
        double height = anchor.getHeight();
        boolean exceedsStandardLimit = anchor.getWidth() > unit * 1.85d || height > unit * 1.85d;
        return exceedsStandardLimit
                && height >= unit * 1.15d
                && height <= unit * 1.45d
                && anchor.getWidth() <= Math.min(unit * 3.55d, imageWidth * 0.19d)
                && anchor.getWidth() / height <= 2.65d;
    }

    private static List<PropertyBlockAnchor> filterAnchorColumn(List<PropertyBlockAnchor> anchors, int imageWidth) {
        if (anchors.isEmpty()) {
            return anchors;
        }
        double unit = Math.max(8.0d, imageWidth * 0.052d);
        int minX = anchors.stream().mapToInt(PropertyBlockAnchor::getX).min().orElse(0);
        double maxIconColumnX = minX + unit * 0.82d;
        double maxCompactIconColumnX = minX + unit * 1.50d;
        return anchors.stream()
                .filter(anchor -> anchor.getX() <= maxIconColumnX
                        || (isFrameRecoveredAnchor(anchor)
                        && anchor.getX() <= maxCompactIconColumnX
                        && anchor.getWidth() <= unit * 0.95d))
                .toList();
    }

    private static boolean isFrameRecoveredAnchor(PropertyBlockAnchor anchor) {
        return anchor.getConfidence() > 0.0d && anchor.getConfidence() < 0.445d;
    }

    private static List<TextLine> detectTextLines(BufferedImage image, SegmentScale scale) {
        int width = image.getWidth();
        int height = image.getHeight();
        int marginX = horizontalScanMargin(width);
        boolean[] activeRows = new boolean[height];
        for (int y = 0; y < height; y++) {
            int foreground = 0;
            for (int x = marginX; x < width - marginX; x++) {
                if (isForeground(image.getRGB(x, y))) {
                    foreground++;
                }
            }
            activeRows[y] = foreground >= Math.max(3, (int) Math.round(width * 0.022d));
        }

        List<Band> bands = new ArrayList<>();
        int y = 0;
        while (y < height) {
            while (y < height && !activeRows[y]) {
                y++;
            }
            if (y >= height) {
                break;
            }
            int start = y;
            int lastActive = y;
            int gap = 0;
            int bridgeGap = Math.max(1, (int) Math.round(scale.unit() * 0.055d));
            while (y + 1 < height && gap <= bridgeGap) {
                y++;
                if (activeRows[y]) {
                    lastActive = y;
                    gap = 0;
                } else {
                    gap++;
                }
            }
            int end = lastActive + 1;
            if (end - start >= Math.max(2, (int) Math.round(scale.unit() * 0.12d))) {
                bands.add(new Band(start, end));
            }
            y = Math.max(y, end);
        }

        List<TextLine> lines = new ArrayList<>();
        for (Band band : bands) {
            Bounds bounds = foregroundBounds(image, band.startY(), band.endY());
            if (bounds != null
                    && bounds.width() >= Math.max(5, (int) Math.round(scale.unit() * 0.28d))
                    && bounds.height() >= Math.max(2, (int) Math.round(scale.unit() * 0.12d))) {
                lines.add(new TextLine(bounds.x(), bounds.y(), bounds.width(), bounds.height()));
            }
        }
        return mergeTextFragments(lines, scale).stream()
                .sorted(Comparator.comparingInt(TextLine::y))
                .toList();
    }

    private static List<TextLine> mergeTextFragments(List<TextLine> lines, SegmentScale scale) {
        List<TextLine> sorted = lines.stream()
                .sorted(Comparator.comparingInt(TextLine::y))
                .toList();
        List<TextLine> merged = new ArrayList<>();
        for (TextLine line : sorted) {
            if (!merged.isEmpty()) {
                TextLine previous = merged.getLast();
                int gap = line.y() - previous.bottom();
                boolean sameVisualLine = gap <= Math.max(1, (int) Math.round(scale.unit() * 0.10d))
                        && Math.abs(centerY(line) - centerY(previous)) <= Math.max(previous.height(), line.height());
                if (sameVisualLine) {
                    merged.set(merged.size() - 1, previous.union(line));
                    continue;
                }
            }
            merged.add(line);
        }
        return merged;
    }

    private static Optional<PropertyBlockAnchor> findAnchorForLine(TextLine line,
                                                                   List<PropertyBlockAnchor> anchors,
                                                                   int imageWidth) {
        int centerY = centerY(line);
        return anchors.stream()
                .filter(anchor -> Math.abs(centerY(anchor) - centerY) <= Math.max(line.height(), imageWidth * 0.025d))
                .filter(anchor -> centerX(anchor) < Math.max(line.x(), imageWidth * 0.27d))
                .min(Comparator.comparingInt(VisualPropertyBlockSegmenter::centerX)
                        .thenComparingInt(anchor -> Math.abs(centerY(anchor) - centerY)));
    }

    private static List<BlockDraft> groupBlocks(List<LineWithAnchor> lines,
                                                int imageWidth,
                                                int imageHeight,
                                                SegmentScale scale) {
        List<LineWithAnchor> candidates = lines.stream()
                .filter(line -> isContentLine(line.line(), imageWidth, imageHeight, scale))
                .toList();
        List<BlockDraft> blocks = new ArrayList<>();
        BlockDraft current = null;
        int firstAnchoredIndex = firstAnchoredIndex(candidates);

        for (int index = 0; index < candidates.size(); index++) {
            LineWithAnchor candidate = candidates.get(index);
            TextLine line = candidate.line();
            boolean hasAnchor = candidate.anchor() != null;
            boolean startsLeadingNone = current == null && !hasAnchor && (firstAnchoredIndex < 0 || index < firstAnchoredIndex);
            boolean startsBlock = hasAnchor || startsLeadingNone;

            if (startsBlock) {
                if (current != null && !current.lines().isEmpty()) {
                    blocks.add(current);
                }
                current = new BlockDraft(candidate.anchorType(), candidate.anchor());
                current.add(candidate);
                continue;
            }

            if (current == null) {
                continue;
            }

            boolean boundedByNextAnchor = current.anchor() != null && hasFutureAnchor(candidates, index);
            if (boundedByNextAnchor || isContinuation(current, line, imageWidth, scale)) {
                current.add(candidate);
            } else {
                blocks.add(current);
                current = null;
            }
        }
        if (current != null && !current.lines().isEmpty()) {
            blocks.add(current);
        }
        return blocks;
    }

    private static boolean hasFutureAnchor(List<LineWithAnchor> lines, int currentIndex) {
        for (int index = currentIndex + 1; index < lines.size(); index++) {
            if (lines.get(index).anchor() != null) {
                return true;
            }
        }
        return false;
    }

    private static List<BlockDraft> removeTrailingFlavorAfterSocketPair(List<BlockDraft> drafts,
                                                                        int imageHeight,
                                                                        SegmentScale scale) {
        List<BlockDraft> result = new ArrayList<>();
        int socketLikeRun = 0;
        boolean socketPairSeen = false;
        for (BlockDraft draft : drafts) {
            if (socketPairSeen && isLateSmallUnknownIconDraft(draft, imageHeight, scale)) {
                continue;
            }
            result.add(draft);
            if (isSocketLikeDraft(draft, scale)) {
                socketLikeRun++;
                if (socketLikeRun >= 2) {
                    socketPairSeen = true;
                }
            } else if (draft.anchorType() != PropertyBlockAnchorType.NONE) {
                socketLikeRun = 0;
            }
        }
        return result;
    }

    private static List<BlockDraft> removeTrailingFlavorTextDrafts(List<BlockDraft> drafts,
                                                                    BufferedImage image,
                                                                    SegmentScale scale) {
        int suffixStart = drafts.size();
        while (suffixStart > 0
                && isFlavorTextDraft(drafts.get(suffixStart - 1), image, scale)) {
            suffixStart--;
        }
        if (suffixStart == drafts.size()) {
            return drafts;
        }
        int suffixLineCount = drafts.subList(suffixStart, drafts.size()).stream()
                .mapToInt(draft -> draft.lines().size())
                .sum();
        boolean hasFlavorSequence = drafts.size() - suffixStart >= 2 && suffixLineCount >= 3;
        if (!hasFlavorSequence) {
            return drafts;
        }
        return new ArrayList<>(drafts.subList(0, suffixStart));
    }

    private static boolean isFlavorTextDraft(BlockDraft draft,
                                             BufferedImage image,
                                             SegmentScale scale) {
        PropertyBlockAnchor anchor = draft.anchor();
        if (anchor == null || draft.lines().size() < 2) {
            return false;
        }
        double unit = scale.unit();
        boolean textFragmentGeometry = anchor.getWidth() >= unit * 0.90d
                && anchor.getWidth() <= unit * 1.75d
                && anchor.getHeight() >= unit * 0.65d
                && anchor.getHeight() <= unit * 1.15d;
        return textFragmentGeometry
                && foregroundGapToRight(image, anchor, scale) <= unit * 0.20d;
    }

    private static int foregroundGapToRight(BufferedImage image,
                                            PropertyBlockAnchor anchor,
                                            SegmentScale scale) {
        int startX = anchor.getX() + anchor.getWidth();
        int endX = Math.min(image.getWidth(), startX + Math.max(1, (int) Math.ceil(scale.unit() * 0.55d)));
        int startY = Math.max(0, anchor.getY());
        int endY = Math.min(image.getHeight(), anchor.getY() + anchor.getHeight());
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                if (isAnchorGapForeground(image, x, y)) {
                    return x - startX;
                }
            }
        }
        return endX - startX;
    }

    private static boolean isAnchorGapForeground(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);
        return max >= 96
                || anchorGapLocalContrast(image, x, y) >= 20
                || (max >= 76 && hsb[1] >= 0.14f && max - min >= 8);
    }

    private static int anchorGapLocalContrast(BufferedImage image, int x, int y) {
        int luminance = luminance(image.getRGB(x, y));
        int maxDifference = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < image.getWidth() && ny < image.getHeight()) {
                    maxDifference = Math.max(maxDifference,
                            Math.abs(luminance - luminance(image.getRGB(nx, ny))));
                }
            }
        }
        return maxDifference;
    }

    private static int luminance(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        return (red * 299 + green * 587 + blue * 114) / 1000;
    }

    private static boolean isSocketLikeDraft(BlockDraft draft, SegmentScale scale) {
        PropertyBlockAnchor anchor = draft.anchor();
        if (anchor == null) {
            return false;
        }
        return anchor.getType() == PropertyBlockAnchorType.EMPTY_SOCKET
                || anchor.getType() == PropertyBlockAnchorType.FILLED_SOCKET
                || (anchor.getType() == PropertyBlockAnchorType.UNKNOWN_ICON
                && anchor.getWidth() >= scale.unit() * 2.60d
                && anchor.getHeight() >= scale.unit() * 1.10d);
    }

    private static boolean isLateSmallUnknownIconDraft(BlockDraft draft, int imageHeight, SegmentScale scale) {
        PropertyBlockAnchor anchor = draft.anchor();
        if (anchor == null || anchor.getType() != PropertyBlockAnchorType.UNKNOWN_ICON) {
            return false;
        }
        return draft.firstLine().y() > imageHeight * 0.72d
                && anchor.getWidth() <= scale.unit() * 1.70d
                && draft.lines().size() <= 2;
    }

    private static boolean isContentLine(TextLine line, int imageWidth, int imageHeight, SegmentScale scale) {
        if (line.width() < Math.max(7, (int) Math.round(scale.unit() * 0.40d))
                || line.height() < Math.max(2, (int) Math.round(scale.unit() * 0.14d))) {
            return false;
        }
        if (line.y() > imageHeight * 0.86d && line.x() > imageWidth * 0.22d) {
            return false;
        }
        return true;
    }

    private static int firstAnchoredIndex(List<LineWithAnchor> candidates) {
        for (int index = 0; index < candidates.size(); index++) {
            if (candidates.get(index).anchor() != null) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isContinuation(BlockDraft current, TextLine line, int imageWidth, SegmentScale scale) {
        TextLine previous = current.lastLine();
        int gap = verticalGap(previous, line);
        if (gap > Math.max(previous.height() * 1.15d, scale.unit() * 0.68d)) {
            return false;
        }
        double leftTolerance = current.anchorType() == PropertyBlockAnchorType.NONE
                ? scale.unit() * 0.72d
                : scale.unit() * 1.35d;
        boolean alignedWithText = line.x() >= Math.max(0, current.minTextX() - leftTolerance);
        boolean notFooterLike = !(line.x() > imageWidth * 0.30d
                && line.width() < imageWidth * 0.45d
                && gap >= scale.unit() * 0.32d);
        return alignedWithText && notFooterLike;
    }

    private static PropertyBlock toBlock(StructuralItemPartSlot slot,
                                         int order,
                                         BufferedImage image,
                                         BlockDraft draft,
                                         SegmentScale scale) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (LineWithAnchor line : draft.lines()) {
            minX = Math.min(minX, line.line().x());
            minY = Math.min(minY, line.line().y());
            maxX = Math.max(maxX, line.line().right());
            maxY = Math.max(maxY, line.line().bottom());
        }
        if (draft.anchor() != null) {
            PropertyBlockAnchor anchor = draft.anchor();
            minX = Math.min(minX, anchor.getX());
            minY = Math.min(minY, anchor.getY());
            maxX = Math.max(maxX, anchor.getX() + anchor.getWidth());
            maxY = Math.max(maxY, anchor.getY() + anchor.getHeight());
        }
        int paddingX = Math.max(2, (int) Math.round(scale.unit() * 0.16d));
        int paddingY = Math.max(1, (int) Math.round(scale.unit() * 0.12d));
        minX = Math.max(0, minX - paddingX);
        minY = Math.max(0, minY - paddingY);
        maxX = Math.min(image.getWidth(), maxX + paddingX);
        maxY = Math.min(image.getHeight(), maxY + paddingY);
        byte[] bytes = encode(crop(image, minX, minY, maxX - minX, maxY - minY));
        String diagnostics = "lineCount=" + draft.lines().size();
        return new PropertyBlock(
                slot,
                order,
                minX,
                minY,
                maxX - minX,
                maxY - minY,
                draft.anchorType(),
                draft.anchor(),
                "PNG",
                bytes,
                diagnostics
        );
    }

    private static Bounds foregroundBounds(BufferedImage image, int startY, int endY) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int marginX = horizontalScanMargin(image.getWidth());
        for (int y = startY; y < endY; y++) {
            for (int x = marginX; x < image.getWidth() - marginX; x++) {
                if (isForeground(image.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (minX == Integer.MAX_VALUE) {
            return null;
        }
        return new Bounds(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static int horizontalScanMargin(int width) {
        return Math.max(1, (int) Math.round(width * 0.04d));
    }

    private static boolean isForeground(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);
        return max >= 170 || (max >= 118 && hsb[1] >= 0.16f && max - min >= 18);
    }

    private static BufferedImage decode(ItemImagePart part) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(part.getImageBytes()));
            if (image == null) {
                throw new IllegalArgumentException("Nie udało się odczytać obrazu " + part.getName() + ".");
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Nie udało się odczytać obrazu " + part.getName() + ".", exception);
        }
    }

    private static BufferedImage crop(BufferedImage source, int x, int y, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(source, 0, 0, width, height, x, y, x + width, y + height, null);
        graphics.dispose();
        return image;
    }

    private static byte[] encode(BufferedImage image) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się zapisać diagnostycznego ROI bloku właściwości.", exception);
        }
    }

    private static int centerY(TextLine line) {
        return line.y() + line.height() / 2;
    }

    private static int centerY(PropertyBlockAnchor anchor) {
        return anchor.getY() + anchor.getHeight() / 2;
    }

    private static int centerX(PropertyBlockAnchor anchor) {
        return anchor.getX() + anchor.getWidth() / 2;
    }

    private static int verticalGap(TextLine previous, TextLine next) {
        return next.y() - previous.bottom();
    }

    private record SegmentScale(double unit) {
        private static SegmentScale from(BufferedImage image) {
            return new SegmentScale(Math.max(8.0d, image.getWidth() * 0.052d));
        }
    }

    private record Band(int startY, int endY) {
    }

    private record Bounds(int x, int y, int width, int height) {
    }

    private record TextLine(int x, int y, int width, int height) {
        private int right() {
            return x + width;
        }

        private int bottom() {
            return y + height;
        }

        private TextLine union(TextLine other) {
            int minX = Math.min(x, other.x);
            int minY = Math.min(y, other.y);
            int maxX = Math.max(right(), other.right());
            int maxY = Math.max(bottom(), other.bottom());
            return new TextLine(minX, minY, maxX - minX, maxY - minY);
        }
    }

    private record LineWithAnchor(TextLine line, PropertyBlockAnchor anchor) {
        private PropertyBlockAnchorType anchorType() {
            return anchor == null ? PropertyBlockAnchorType.NONE : anchor.getType();
        }
    }

    private static final class BlockDraft {
        private final PropertyBlockAnchorType anchorType;
        private final PropertyBlockAnchor anchor;
        private final List<LineWithAnchor> lines = new ArrayList<>();

        private BlockDraft(PropertyBlockAnchorType anchorType, PropertyBlockAnchor anchor) {
            this.anchorType = anchorType == null ? PropertyBlockAnchorType.NONE : anchorType;
            this.anchor = anchor;
        }

        private void add(LineWithAnchor line) {
            lines.add(line);
        }

        private List<LineWithAnchor> lines() {
            return lines;
        }

        private PropertyBlockAnchorType anchorType() {
            return anchorType;
        }

        private PropertyBlockAnchor anchor() {
            return anchor;
        }

        private TextLine lastLine() {
            return lines.getLast().line();
        }

        private TextLine firstLine() {
            return lines.getFirst().line();
        }

        private int minTextX() {
            return lines.stream().map(LineWithAnchor::line).mapToInt(TextLine::x).min().orElse(0);
        }
    }
}
