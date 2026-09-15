package krys.itemimport.image;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Obrazowa segmentacja PART_1 na tymczasowe ROI, bez OCR i bez zmiany granic ItemImagePart. */
public final class DeterministicPart1RoiSegmenter implements Part1RoiSegmenter {
    private static final String OUTPUT_FORMAT = "PNG";

    @Override
    public Part1SegmentationResult segment(StructuralItemPart part1) {
        if (part1 == null) {
            throw new IllegalArgumentException("Brak structural PART_1 do segmentacji ROI.");
        }
        if (part1.getSlot() != StructuralItemPartSlot.PART_1) {
            throw new IllegalArgumentException("Segmentacja PART_1 wymaga structural slot PART_1.");
        }
        BufferedImage image = decode(part1.getSourcePart());
        Rectangle textScanBounds = detectTextScanBounds(image);
        List<TextLine> lines = detectTextLines(image, textScanBounds);
        if (lines.size() < 3) {
            throw new IllegalStateException("PART_1 nie zawiera wystarczajacej liczby wykrytych linii tekstu: lines="
                    + lines.size() + ", width=" + image.getWidth() + ", height=" + image.getHeight() + ".");
        }

        int itemPowerIndex = findItemPowerLineIndex(lines);
        int nameEndIndex = findNameEndIndex(lines, itemPowerIndex);
        List<Part1Roi> rois = new ArrayList<>();
        rois.add(buildRoi(image, Part1RoiType.NAME_AND_STARS, lines.subList(0, nameEndIndex + 1)));
        rois.add(buildRoi(image, Part1RoiType.CLASSIFICATION, lines.subList(nameEndIndex + 1, itemPowerIndex)));
        rois.add(buildRoi(image, Part1RoiType.ITEM_POWER, lines.subList(itemPowerIndex, itemPowerIndex + 1)));

        List<TextLine> optionalLines = findOptionalLines(lines, itemPowerIndex);
        if (!optionalLines.isEmpty()) {
            rois.add(buildRoi(image, Part1RoiType.OPTIONAL_INFO, optionalLines));
        }
        return new Part1SegmentationResult(part1, rois, "");
    }

    private static BufferedImage decode(ItemImagePart part) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(part.getImageBytes()));
            if (image == null) {
                throw new IllegalArgumentException("Nie udalo sie zdekodowac obrazu PART_1.");
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Nie udalo sie zdekodowac obrazu PART_1.", exception);
        }
    }

    private static Rectangle detectTextScanBounds(BufferedImage image) {
        int left = detectPanelLeft(image);
        int right = detectPanelRight(image, left);
        int top = detectPanelTop(image, left, right);
        return new Rectangle(left, top, right - left, image.getHeight() - top);
    }

    private static int detectPanelLeft(BufferedImage image) {
        int threshold = Math.max(4, image.getHeight() / 3);
        for (int x = 0; x < image.getWidth(); x++) {
            int count = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                if (isPanelPixel(image.getRGB(x, y))) {
                    count++;
                }
            }
            if (count >= threshold) {
                return Math.max(0, x - 2);
            }
        }
        return 0;
    }

    private static int detectPanelRight(BufferedImage image, int left) {
        int threshold = Math.max(4, image.getHeight() / 3);
        for (int x = image.getWidth() - 1; x >= left; x--) {
            int count = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                if (isPanelPixel(image.getRGB(x, y))) {
                    count++;
                }
            }
            if (count >= threshold) {
                return Math.min(image.getWidth(), x + 3);
            }
        }
        return image.getWidth();
    }

    private static int detectPanelTop(BufferedImage image, int left, int right) {
        int width = Math.max(1, right - left);
        int requiredCoverage = Math.max(10, width * 55 / 100);
        int requiredEdge = Math.max(8, width / 7);
        for (int y = 0; y < image.getHeight(); y++) {
            int coverage = 0;
            int edge = 0;
            for (int x = left; x < right; x++) {
                int rgb = image.getRGB(x, y);
                if (isPanelPixel(rgb)) {
                    coverage++;
                }
                if (y + 1 < image.getHeight()) {
                    int below = image.getRGB(x, y + 1);
                    if (Math.abs(luma(rgb) - luma(below)) >= 18) {
                        edge++;
                    }
                }
            }
            if (coverage >= requiredCoverage
                    || (coverage >= width * 35 / 100 && edge >= requiredEdge)) {
                return Math.max(0, y - 1);
            }
        }
        return 0;
    }

    private static boolean isPanelPixel(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        return max >= 24;
    }

    private static List<TextLine> detectTextLines(BufferedImage image, Rectangle textScanBounds) {
        int width = image.getWidth();
        int height = textScanBounds.y + textScanBounds.height;
        int scanStartX = textScanBounds.x + Math.max(8, textScanBounds.width * 4 / 100);
        int scanEndX = Math.max(scanStartX + 1, Math.min(width, textScanBounds.x + textScanBounds.width * 68 / 100));
        int scanWidth = scanEndX - scanStartX;
        int threshold = Math.max(6, scanWidth / 70);
        boolean[] activeRows = new boolean[image.getHeight()];
        int[] rowActivity = new int[image.getHeight()];
        for (int y = textScanBounds.y; y < height; y++) {
            int activity = 0;
            for (int x = scanStartX; x < scanEndX; x++) {
                if (isTextSignal(image, x, y)) {
                    activity++;
                }
            }
            rowActivity[y] = activity;
        }
        for (int y = textScanBounds.y; y < height; y++) {
            int smoothed = 0;
            int start = Math.max(textScanBounds.y, y - 1);
            int end = Math.min(height, y + 2);
            for (int cursor = start; cursor < end; cursor++) {
                smoothed = Math.max(smoothed, rowActivity[cursor]);
            }
            activeRows[y] = smoothed >= threshold;
        }

        List<Range> ranges = new ArrayList<>();
        int startY = -1;
        int inactiveGap = 0;
        for (int y = textScanBounds.y; y < height; y++) {
            if (activeRows[y]) {
                if (startY < 0) {
                    startY = y;
                }
                inactiveGap = 0;
                continue;
            }
            if (startY >= 0) {
                inactiveGap++;
                if (inactiveGap > 3) {
                    int endY = y - inactiveGap + 1;
                    addRange(ranges, startY, endY);
                    startY = -1;
                    inactiveGap = 0;
                }
            }
        }
        if (startY >= 0) {
            addRange(ranges, startY, height - inactiveGap);
        }

        return ranges.stream()
                .map(range -> describeLine(image, range, scanStartX, scanEndX))
                .filter(line -> line.height() >= 4 && line.signalPixels() >= threshold * Math.max(3, line.height() / 2))
                .sorted(Comparator.comparingInt(TextLine::startY))
                .toList();
    }

    private static boolean isTextSignal(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);
        if (!isTextColor(rgb)) {
            return false;
        }
        int center = luma(rgb);
        int left = luma(image.getRGB(Math.max(0, x - 1), y));
        int right = luma(image.getRGB(Math.min(image.getWidth() - 1, x + 1), y));
        int up = luma(image.getRGB(x, Math.max(0, y - 1)));
        int down = luma(image.getRGB(x, Math.min(image.getHeight() - 1, y + 1)));
        int localContrast = Math.max(
                Math.max(Math.abs(center - left), Math.abs(center - right)),
                Math.max(Math.abs(center - up), Math.abs(center - down))
        );
        return localContrast >= 8;
    }

    private static boolean isTextColor(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        int luma = luma(rgb);
        boolean whiteText = luma >= 120 && max - min <= 70;
        boolean paleText = luma >= 105 && max >= 125 && max - min <= 90 && (max - min >= 10 || luma >= 125);
        boolean warmText = red >= 125 && green >= 75 && blue <= 125 && red - blue >= 28;
        boolean coolText = max >= 115 && luma >= 82 && blue >= 90 && red >= 90 && max - min >= 18;
        boolean vividText = max >= 145 && max - min >= 45;
        return whiteText || paleText || warmText || coolText || vividText;
    }

    private static int luma(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        return (red * 299 + green * 587 + blue * 114) / 1000;
    }

    private static void addRange(List<Range> ranges, int startY, int endY) {
        if (endY - startY >= 3) {
            ranges.add(new Range(startY, endY));
        }
    }

    private static TextLine describeLine(BufferedImage image, Range range, int scanStartX, int scanEndX) {
        int minX = scanEndX;
        int maxX = scanStartX;
        int signalPixels = 0;
        int whitePixels = 0;
        long redSum = 0L;
        long greenSum = 0L;
        long blueSum = 0L;
        for (int y = range.start(); y < range.endExclusive(); y++) {
            for (int x = scanStartX; x < scanEndX; x++) {
                int rgb = image.getRGB(x, y);
                if (isTextSignal(image, x, y)) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x + 1);
                    signalPixels++;
                    redSum += (rgb >> 16) & 0xff;
                    greenSum += (rgb >> 8) & 0xff;
                    blueSum += rgb & 0xff;
                    if (isWhiteTextSignal(rgb)) {
                        whitePixels++;
                    }
                }
            }
        }
        if (signalPixels == 0) {
            minX = scanStartX;
            maxX = scanStartX + 1;
        }
        double whiteRatio = signalPixels == 0 ? 0.0d : whitePixels / (double) signalPixels;
        int averageRed = signalPixels == 0 ? 0 : (int) (redSum / signalPixels);
        int averageGreen = signalPixels == 0 ? 0 : (int) (greenSum / signalPixels);
        int averageBlue = signalPixels == 0 ? 0 : (int) (blueSum / signalPixels);
        int saturation = Math.max(averageRed, Math.max(averageGreen, averageBlue))
                - Math.min(averageRed, Math.min(averageGreen, averageBlue));
        return new TextLine(range.start(), range.endExclusive(), minX, maxX, signalPixels, whiteRatio, saturation);
    }

    private static boolean isWhiteTextSignal(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        int luma = (red * 299 + green * 587 + blue * 114) / 1000;
        return luma >= 120 && max - min <= 70;
    }

    private static int findItemPowerLineIndex(List<TextLine> lines) {
        int maxHeaderHeight = lines.stream()
                .limit(Math.min(5, lines.size()))
                .mapToInt(TextLine::height)
                .max()
                .orElse(1);
        int maxLineWidth = lines.stream()
                .mapToInt(TextLine::width)
                .max()
                .orElse(1);
        for (int index = Math.min(2, lines.size() - 1); index < lines.size(); index++) {
            TextLine line = lines.get(index);
            if ((line.saturation() <= 38 || line.whiteRatio() >= 0.88d)
                    && line.height() <= maxHeaderHeight
                    && line.width() >= maxLineWidth * 0.28d) {
                return index;
            }
        }
        return Math.min(lines.size() - 1, Math.max(2, lines.size() / 3));
    }

    private static int findNameEndIndex(List<TextLine> lines, int itemPowerIndex) {
        if (itemPowerIndex <= 1) {
            return 0;
        }
        int largestGap = -1;
        int splitIndex = 0;
        for (int index = 0; index < itemPowerIndex - 1; index++) {
            int gap = lines.get(index + 1).startY() - lines.get(index).endYExclusive();
            if (gap > largestGap) {
                largestGap = gap;
                splitIndex = index;
            }
        }
        return Math.max(0, Math.min(splitIndex, itemPowerIndex - 2));
    }

    private static List<TextLine> findOptionalLines(List<TextLine> lines, int itemPowerIndex) {
        if (itemPowerIndex + 1 >= lines.size()) {
            return List.of();
        }
        TextLine itemPower = lines.get(itemPowerIndex);
        TextLine next = lines.get(itemPowerIndex + 1);
        int initialGap = next.startY() - itemPower.endYExclusive();
        int optionalGapThreshold = Math.max(10, Math.min(36, itemPower.height() * 2));
        if (initialGap > optionalGapThreshold
                || next.height() < itemPower.height() * 0.55d
                || next.width() < itemPower.width() * 0.45d) {
            return List.of();
        }

        List<TextLine> optional = new ArrayList<>();
        optional.add(next);
        TextLine previous = next;
        for (int index = itemPowerIndex + 2; index < lines.size(); index++) {
            TextLine candidate = lines.get(index);
            int gap = candidate.startY() - previous.endYExclusive();
            if (gap > optionalGapThreshold) {
                break;
            }
            optional.add(candidate);
            previous = candidate;
        }
        return List.copyOf(optional);
    }

    private static Part1Roi buildRoi(BufferedImage image, Part1RoiType type, List<TextLine> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Nie mozna utworzyc ROI PART_1 bez linii tekstowych.");
        }
        int averageHeight = Math.max(1, (int) Math.round(lines.stream().mapToInt(TextLine::height).average().orElse(1.0d)));
        int marginX = type == Part1RoiType.NAME_AND_STARS
                ? Math.max(8, image.getWidth() / 38)
                : Math.max(4, image.getWidth() / 80);
        int marginY = Math.max(3, averageHeight / 5);
        int minX = lines.stream().mapToInt(TextLine::minX).min().orElse(0);
        int maxX = lines.stream().mapToInt(TextLine::maxX).max().orElse(image.getWidth());
        int minY = lines.stream().mapToInt(TextLine::startY).min().orElse(0);
        int maxY = lines.stream().mapToInt(TextLine::endYExclusive).max().orElse(image.getHeight());
        int x = clamp(minX - marginX, 0, image.getWidth() - 1);
        int y = clamp(minY - marginY, 0, image.getHeight() - 1);
        int right = clamp(maxX + marginX, x + 1, image.getWidth());
        int bottom = clamp(maxY + marginY, y + 1, image.getHeight());
        int width = right - x;
        int height = bottom - y;
        return new Part1Roi(type, x, y, width, height, OUTPUT_FORMAT, encodePng(image.getSubimage(x, y, width, height)));
    }

    private static byte[] encodePng(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, OUTPUT_FORMAT, outputStream)) {
                throw new IllegalStateException("Nie udalo sie zakodowac ROI PART_1 do PNG.");
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udalo sie zakodowac ROI PART_1 do PNG.", exception);
        }
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private record Range(int start, int endExclusive) {
    }

    private record TextLine(int startY,
                            int endYExclusive,
                            int minX,
                            int maxX,
                            int signalPixels,
                            double whiteRatio,
                            int saturation) {
        private int height() {
            return endYExclusive - startY;
        }

        private int width() {
            return maxX - minX;
        }
    }
}
