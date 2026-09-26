package krys.itemimport.image;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Obrazowy detektor ikon startowych w lewym obszarze PART_2/PART_3. */
public final class VisualPropertyBlockAnchorDetector implements PropertyBlockAnchorDetector {
    @Override
    public List<PropertyBlockAnchor> detect(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        AnchorScale scale = AnchorScale.from(image);
        int laneWidth = Math.max(1, Math.min(width, (int) Math.ceil(width * 0.24d)));
        boolean[][] visited = new boolean[height][laneWidth];
        List<PropertyBlockAnchor> anchors = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < laneWidth; x++) {
                if (visited[y][x] || !isCandidatePixel(image, x, y, scale)) {
                    continue;
                }
                Component component = collect(image, visited, x, y, laneWidth, scale);
                if (isPotentialAnchor(component, width, height, scale)) {
                    PropertyBlockAnchor anchor = toAnchor(component, scale);
                    anchors.add(anchor);
                } else if (isOversizedFrameLikeComponent(component, scale)) {
                    for (Component localComponent : splitLocalComponentsAwayFromFrame(component, width, height, scale)) {
                        anchors.add(toRecoveredAnchor(localComponent, scale));
                    }
                }
            }
        }
        return mergeCloseAnchors(anchors, scale).stream()
                .sorted(Comparator.comparingInt(PropertyBlockAnchor::getY)
                        .thenComparingInt(PropertyBlockAnchor::getX))
                .toList();
    }

    private static Component collect(BufferedImage image,
                                     boolean[][] visited,
                                     int startX,
                                     int startY,
                                     int laneWidth,
                                     AnchorScale scale) {
        int height = image.getHeight();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;
        Component component = new Component();
        while (!queue.isEmpty()) {
            int[] point = queue.removeFirst();
            int x = point[0];
            int y = point[1];
            component.add(x, y, image.getRGB(x, y));
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) {
                        continue;
                    }
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx < 0 || nx >= laneWidth || ny < 0 || ny >= height
                            || visited[ny][nx] || !isCandidatePixel(image, nx, ny, scale)) {
                        continue;
                    }
                    visited[ny][nx] = true;
                    queue.addLast(new int[]{nx, ny});
                }
            }
        }
        return component;
    }

    private static boolean isPotentialAnchor(Component component, int imageWidth, int imageHeight, AnchorScale scale) {
        int width = component.width();
        int height = component.height();
        double unit = scale.unit();
        if (component.pixelCount < Math.max(3, Math.round(unit * unit * 0.015d))) {
            return false;
        }
        if (width < Math.max(2, unit * 0.18d) || height < Math.max(2, unit * 0.18d)) {
            return false;
        }
        if ((width > unit * 1.85d || height > unit * 1.85d)
                && !isSocketSizedComponent(component, imageWidth, scale)) {
            return false;
        }
        if (component.minX < imageWidth * 0.025d
                || component.centerX() > imageWidth * 0.18d
                || component.minY < imageHeight * 0.015d) {
            return false;
        }
        double fill = component.pixelCount / (double) (width * height);
        double aspect = width / (double) height;
        return fill >= 0.08d && fill <= 0.94d && aspect >= 0.30d && aspect <= 2.90d;
    }

    private static boolean isOversizedFrameLikeComponent(Component component, AnchorScale scale) {
        double unit = scale.unit();
        return component.height() > unit * 3.20d
                && component.width() > unit * 0.85d
                && (component.width() > unit * 1.85d || component.height() > unit * 1.85d);
    }

    private static List<Component> splitLocalComponentsAwayFromFrame(Component component,
                                                                     int imageWidth,
                                                                     int imageHeight,
                                                                     AnchorScale scale) {
        int width = component.width();
        int height = component.height();
        int[] columnCounts = new int[width];
        int[] rowCounts = new int[height];
        boolean[][] mask = new boolean[height][width];
        int[][] rgbByLocal = new int[height][width];
        int maxRecoveredLocalX = Math.min(width - 1, (int) Math.ceil(scale.unit() * 2.70d));
        for (Pixel pixel : component.pixels) {
            int localX = pixel.x() - component.minX;
            int localY = pixel.y() - component.minY;
            if (localX > maxRecoveredLocalX) {
                continue;
            }
            mask[localY][localX] = true;
            rgbByLocal[localY][localX] = pixel.rgb();
            columnCounts[localX]++;
            rowCounts[localY]++;
        }

        boolean[] frameColumns = frameColumns(mask, columnCounts, height, scale);
        boolean[] frameRows = frameRows(rowCounts, width, scale);
        boolean[][] visited = new boolean[height][width];
        List<Component> rawLocalComponents = new ArrayList<>();
        List<Component> localComponents = new ArrayList<>();
        for (int localY = 0; localY < height; localY++) {
            for (int localX = 0; localX < width; localX++) {
                if (visited[localY][localX]
                        || !mask[localY][localX]
                        || frameColumns[localX]
                        || frameRows[localY]) {
                    continue;
                }
                Component localComponent = collectLocal(component, mask, rgbByLocal, frameColumns, frameRows,
                        visited, localX, localY);
                rawLocalComponents.add(localComponent);
                if (isRecoveredLocalAnchor(localComponent, component, imageWidth, imageHeight, scale)) {
                    localComponents.add(localComponent);
                }
            }
        }
        localComponents.addAll(mergeRecoveredFragments(rawLocalComponents, component, imageWidth, imageHeight, scale));
        return deduplicateRecoveredFrameAnchors(localComponents, scale);
    }

    private static boolean[] frameColumns(boolean[][] mask,
                                          int[] columnCounts,
                                          int componentHeight,
                                          AnchorScale scale) {
        boolean[] frameColumns = new boolean[columnCounts.length];
        int countThreshold = Math.max((int) Math.round(scale.unit() * 2.20d),
                (int) Math.round(componentHeight * 0.50d));
        int runThreshold = Math.max((int) Math.round(scale.unit() * 2.85d),
                (int) Math.round(componentHeight * 0.08d));
        for (int x = 0; x < columnCounts.length; x++) {
            frameColumns[x] = columnCounts[x] >= countThreshold
                    || longestVerticalRun(mask, x) >= runThreshold;
        }
        return frameColumns;
    }

    private static int longestVerticalRun(boolean[][] mask, int x) {
        int longest = 0;
        int current = 0;
        for (boolean[] booleans : mask) {
            if (booleans[x]) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return longest;
    }

    private static boolean[] frameRows(int[] rowCounts, int componentWidth, AnchorScale scale) {
        boolean[] frameRows = new boolean[rowCounts.length];
        if (componentWidth <= scale.unit() * 3.20d) {
            return frameRows;
        }
        int threshold = Math.max((int) Math.round(scale.unit() * 1.85d),
                (int) Math.round(componentWidth * 0.62d));
        for (int y = 0; y < rowCounts.length; y++) {
            frameRows[y] = rowCounts[y] >= threshold;
        }
        return frameRows;
    }

    private static Component collectLocal(Component source,
                                          boolean[][] mask,
                                          int[][] rgbByLocal,
                                          boolean[] frameColumns,
                                          boolean[] frameRows,
                                          boolean[][] visited,
                                          int startX,
                                          int startY) {
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;
        Component component = new Component();
        while (!queue.isEmpty()) {
            int[] point = queue.removeFirst();
            int localX = point[0];
            int localY = point[1];
            component.add(source.minX + localX, source.minY + localY, rgbByLocal[localY][localX]);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) {
                        continue;
                    }
                    int nx = localX + dx;
                    int ny = localY + dy;
                    if (nx < 0 || nx >= mask[0].length || ny < 0 || ny >= mask.length
                            || visited[ny][nx]
                            || !mask[ny][nx]
                            || frameColumns[nx]
                            || frameRows[ny]) {
                        continue;
                    }
                    visited[ny][nx] = true;
                    queue.addLast(new int[]{nx, ny});
                }
            }
        }
        return component;
    }

    private static boolean isLocalRecoveryCandidate(Component localComponent,
                                                    Component source,
                                                    int imageWidth,
                                                    int imageHeight,
                                                    AnchorScale scale) {
        if (localComponent.pixelCount < Math.max(3, Math.round(scale.unit() * scale.unit() * 0.010d))) {
            return false;
        }
        if (localComponent.width() > scale.unit() * 2.10d || localComponent.height() > scale.unit() * 2.10d) {
            return false;
        }
        double minRecoveredX = Math.max(imageWidth * 0.045d, source.minX + scale.unit() * 0.82d);
        double minRecoveredY = Math.max(imageHeight * 0.075d, scale.unit() * 2.50d);
        if (localComponent.minX < minRecoveredX || localComponent.minY < minRecoveredY) {
            return false;
        }
        return localComponent.minX > source.minX
                && localComponent.height() <= source.height() * 0.12d;
    }

    private static boolean isRecoveredLocalAnchor(Component localComponent,
                                                  Component source,
                                                  int imageWidth,
                                                  int imageHeight,
                                                  AnchorScale scale) {
        return isLocalRecoveryCandidate(localComponent, source, imageWidth, imageHeight, scale)
                && (isPotentialAnchor(localComponent, imageWidth, imageHeight, scale)
                || isRecoveredFrameAnchorGeometry(localComponent, imageWidth, imageHeight, scale));
    }

    private static boolean isRecoveredFrameAnchorGeometry(Component component,
                                                          int imageWidth,
                                                          int imageHeight,
                                                          AnchorScale scale) {
        double unit = scale.unit();
        int width = component.width();
        int height = component.height();
        if (component.pixelCount < Math.max(6, Math.round(unit * unit * 0.030d))) {
            return false;
        }
        if (width < Math.max(3, unit * 0.18d) || height < Math.max(3, unit * 0.18d)) {
            return false;
        }
        if (width > unit * 1.35d || height > unit * 2.10d) {
            return false;
        }
        if (component.minX < imageWidth * 0.045d
                || component.centerX() > imageWidth * 0.18d
                || component.minY < imageHeight * 0.015d) {
            return false;
        }
        double fill = component.pixelCount / (double) (width * height);
        double aspect = width / (double) height;
        return fill >= 0.055d && fill <= 0.94d && aspect >= 0.12d && aspect <= 2.90d;
    }

    private static List<Component> mergeRecoveredFragments(List<Component> rawComponents,
                                                           Component source,
                                                           int imageWidth,
                                                           int imageHeight,
                                                           AnchorScale scale) {
        List<Component> fragments = rawComponents.stream()
                .filter(component -> isRecoverableFragment(component, source, imageWidth, imageHeight, scale))
                .sorted(Comparator.comparingInt(Component::minY).thenComparingInt(Component::minX))
                .toList();
        List<Component> merged = new ArrayList<>();
        Component current = null;
        for (Component fragment : fragments) {
            if (current == null) {
                current = fragment.copy();
                continue;
            }
            if (canMergeRecoveredFragment(current, fragment, scale)) {
                current.addAll(fragment);
            } else {
                if (isRecoveredFrameAnchorGeometry(current, imageWidth, imageHeight, scale)) {
                    merged.add(current);
                }
                current = fragment.copy();
            }
        }
        if (current != null && isRecoveredFrameAnchorGeometry(current, imageWidth, imageHeight, scale)) {
            merged.add(current);
        }
        return merged;
    }

    private static List<Component> deduplicateRecoveredFrameAnchors(List<Component> components, AnchorScale scale) {
        List<Component> sorted = components.stream()
                .filter(component -> component.pixelCount > 0)
                .sorted(Comparator.comparingInt(Component::minY).thenComparingInt(Component::minX))
                .toList();
        List<Component> result = new ArrayList<>();
        for (Component component : sorted) {
            boolean closeToPrevious = result.stream()
                    .anyMatch(previous -> Math.abs(previous.centerY() - component.centerY()) <= scale.unit() * 3.65d);
            if (!closeToPrevious) {
                result.add(component);
            }
        }
        return result;
    }


    private static boolean isRecoverableFragment(Component component,
                                                 Component source,
                                                 int imageWidth,
                                                 int imageHeight,
                                                 AnchorScale scale) {
        if (!isLocalRecoveryCandidate(component, source, imageWidth, imageHeight, scale)) {
            return false;
        }
        return component.width() <= Math.max(2, scale.unit() * 0.40d)
                && component.height() <= scale.unit() * 2.45d
                && component.pixelCount >= 3;
    }

    private static boolean canMergeRecoveredFragment(Component current, Component next, AnchorScale scale) {
        int gap = next.minY - current.maxY - 1;
        int mergedWidth = Math.max(current.maxX, next.maxX) - Math.min(current.minX, next.minX) + 1;
        int mergedHeight = Math.max(current.maxY, next.maxY) - Math.min(current.minY, next.minY) + 1;
        return gap <= Math.max(1, (int) Math.round(scale.unit() * 0.18d))
                && mergedWidth <= scale.unit() * 1.35d
                && mergedHeight <= scale.unit() * 2.10d
                && Math.abs(current.centerX() - next.centerX()) <= scale.unit() * 0.95d;
    }

    private static boolean isSocketSizedComponent(Component component, int imageWidth, AnchorScale scale) {
        double unit = scale.unit();
        double height = component.height();
        return height >= unit * 1.15d
                && height <= unit * 1.45d
                && component.width() <= Math.min(unit * 3.55d, imageWidth * 0.19d)
                && component.width() / height <= 2.65d;
    }

    private static PropertyBlockAnchor toAnchor(Component component, AnchorScale scale) {
        PropertyBlockAnchorType type = classify(component, scale);
        double confidence = switch (type) {
            case UNKNOWN_ICON -> 0.45d;
            case NONE -> 0.0d;
            default -> 0.72d;
        };
        return new PropertyBlockAnchor(
                type,
                component.minX,
                component.minY,
                component.width(),
                component.height(),
                confidence
        );
    }

    private static PropertyBlockAnchor toRecoveredAnchor(Component component, AnchorScale scale) {
        PropertyBlockAnchorType type = classify(component, scale);
        if (type == PropertyBlockAnchorType.UNKNOWN_ICON && isRecoveredDiamondCore(component, scale)) {
            type = PropertyBlockAnchorType.DIAMOND;
        }
        return new PropertyBlockAnchor(
                type,
                component.minX,
                component.minY,
                component.width(),
                component.height(),
                0.44d
        );
    }

    private static PropertyBlockAnchorType classify(Component component, AnchorScale scale) {
        int width = component.width();
        int height = component.height();
        double saturation = component.averageSaturation();
        double brightness = component.averageBrightness();
        double fill = component.pixelCount / (double) (width * height);
        double normalizedSize = Math.max(width, height) / scale.unit();
        double normalizedWidth = width / scale.unit();
        double normalizedHeight = height / scale.unit();
        double aspect = width / (double) height;
        double chromaticHue = component.chromaticHue();
        double coloredRatio = component.coloredRatio();
        double centerFill = component.centerFill();
        double borderFill = component.borderFill();
        double holeRatio = component.holeRatio();

        if (normalizedHeight >= 0.75d && normalizedHeight <= 1.05d
                && isIconAspect(aspect, 0.45d, 0.66d)
                && fill >= 0.44d && coloredRatio <= 0.10d) {
            return PropertyBlockAnchorType.NONE;
        }

        boolean socketGeometry = normalizedHeight >= 1.15d && normalizedHeight <= 1.48d
                && normalizedWidth >= 1.25d && normalizedWidth <= 3.60d
                && isIconAspect(aspect, 1.05d, 2.70d);
        if (socketGeometry) {
            boolean filled = isRubyHue(chromaticHue)
                    || holeRatio < 0.06d;
            return filled ? PropertyBlockAnchorType.FILLED_SOCKET : PropertyBlockAnchorType.EMPTY_SOCKET;
        }

        if (normalizedSize <= 0.26d
                && isIconAspect(aspect, 0.75d, 1.25d)
                && isPurpleOrBlue(chromaticHue, saturation)) {
            return PropertyBlockAnchorType.TRANSFIGURATION_MARK;
        }

        if (normalizedSize >= 0.48d && normalizedSize <= 0.70d
                && isIconAspect(aspect, 0.75d, 1.35d)
                && fill >= 0.68d
                && coloredRatio >= 0.45d
                && isGold(chromaticHue, saturation)) {
            return PropertyBlockAnchorType.LEGENDARY_POWER_STAR;
        }

        boolean chromaticUniqueStar = normalizedSize >= 0.60d && normalizedSize <= 0.78d
                && isIconAspect(aspect, 0.75d, 1.25d)
                && saturation >= 0.25d
                && coloredRatio >= 0.45d
                && isPurpleOrBlue(chromaticHue, saturation);
        boolean sparseUniqueStar = normalizedSize >= 0.72d && normalizedSize <= 0.90d
                && isIconAspect(aspect, 0.75d, 1.30d)
                && fill >= 0.45d && fill <= 0.62d
                && borderFill <= 0.35d
                && centerFill >= 0.80d;
        if (chromaticUniqueStar || sparseUniqueStar) {
            return PropertyBlockAnchorType.UNIQUE_POWER_STAR;
        }

        if (normalizedSize >= 0.75d && normalizedSize <= 1.10d
                && isIconAspect(aspect, 0.75d, 1.25d)
                && fill >= 0.60d && fill <= 0.78d
                && saturation >= 0.30d
                && coloredRatio >= 0.30d
                && isWarmStarHue(chromaticHue)) {
            return PropertyBlockAnchorType.GREATER_STAR;
        }

        if (isIconAspect(aspect, 0.72d, 1.38d)
                && fill >= 0.24d && fill <= 0.84d
                && normalizedSize >= 0.27d
                && normalizedSize <= 0.70d
                && brightness >= 0.24d
                && centerFill >= 0.70d) {
            return PropertyBlockAnchorType.DIAMOND;
        }

        if (isIconAspect(aspect, 0.75d, 1.25d)
                && normalizedSize >= 0.82d && normalizedSize <= 1.08d
                && fill >= 0.79d && fill <= 0.90d
                && brightness >= 0.34d
                && (saturation < 0.25d || coloredRatio <= 0.10d)
                && centerFill >= 0.90d) {
            return PropertyBlockAnchorType.TRANSFIGURATION_MARK;
        }
        return PropertyBlockAnchorType.UNKNOWN_ICON;
    }

    private static boolean isRecoveredDiamondCore(Component component, AnchorScale scale) {
        double normalizedWidth = component.width() / scale.unit();
        double normalizedHeight = component.height() / scale.unit();
        double aspect = component.width() / (double) component.height();
        return normalizedWidth >= 0.50d && normalizedWidth <= 0.90d
                && normalizedHeight >= 1.15d && normalizedHeight <= 2.10d
                && aspect >= 0.25d && aspect <= 0.65d;
    }

    private static boolean isIconAspect(double aspect, double min, double max) {
        return aspect >= min && aspect <= max;
    }

    private static boolean isGold(double hue, double saturation) {
        return saturation >= 0.20d && hue >= 20.0d && hue <= 75.0d;
    }

    private static boolean isPurpleOrBlue(double hue, double saturation) {
        return saturation >= 0.20d && ((hue >= 205.0d && hue <= 285.0d) || (hue >= 285.0d && hue <= 330.0d));
    }

    private static boolean isRubyHue(double hue) {
        return hue <= 22.0d || hue >= 300.0d;
    }

    private static boolean isWarmStarHue(double hue) {
        return hue <= 25.0d || hue >= 340.0d;
    }

    private static List<PropertyBlockAnchor> mergeCloseAnchors(List<PropertyBlockAnchor> anchors, AnchorScale scale) {
        List<PropertyBlockAnchor> sorted = anchors.stream()
                .sorted(Comparator.comparingInt(PropertyBlockAnchor::getY)
                        .thenComparingInt(PropertyBlockAnchor::getX))
                .toList();
        List<PropertyBlockAnchor> merged = new ArrayList<>();
        for (PropertyBlockAnchor anchor : sorted) {
            if (!merged.isEmpty()) {
                PropertyBlockAnchor previous = merged.getLast();
                boolean closeY = Math.abs(centerY(previous) - centerY(anchor)) <= Math.max(1, scale.unit() * 0.18d);
                boolean closeX = Math.abs(centerX(previous) - centerX(anchor)) <= Math.max(1, scale.unit() * 0.45d);
                if (closeY && closeX) {
                    merged.set(merged.size() - 1, larger(previous, anchor));
                    continue;
                }
            }
            merged.add(anchor);
        }
        return merged;
    }

    private static PropertyBlockAnchor larger(PropertyBlockAnchor left, PropertyBlockAnchor right) {
        int leftArea = left.getWidth() * left.getHeight();
        int rightArea = right.getWidth() * right.getHeight();
        return rightArea > leftArea ? right : left;
    }

    private static int centerX(PropertyBlockAnchor anchor) {
        return anchor.getX() + anchor.getWidth() / 2;
    }

    private static int centerY(PropertyBlockAnchor anchor) {
        return anchor.getY() + anchor.getHeight() / 2;
    }

    private static boolean isCandidatePixel(BufferedImage image, int x, int y, AnchorScale scale) {
        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        int localContrast = localContrast(image, x, y);
        float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);
        return max >= scale.brightnessFloor()
                || localContrast >= scale.contrastFloor()
                || (max >= scale.colorBrightnessFloor() && hsb[1] >= 0.14f && max - min >= 8);
    }

    private static int localContrast(BufferedImage image, int x, int y) {
        int luminance = luminance(image.getRGB(x, y));
        int maxDifference = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= image.getWidth() || ny >= image.getHeight()) {
                    continue;
                }
                maxDifference = Math.max(maxDifference, Math.abs(luminance - luminance(image.getRGB(nx, ny))));
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

    private record AnchorScale(double unit, int brightnessFloor, int colorBrightnessFloor, int contrastFloor) {
        private static AnchorScale from(BufferedImage image) {
            double unit = Math.max(8.0d, image.getWidth() * 0.052d);
            int brightnessFloor = 96;
            int colorBrightnessFloor = 76;
            int contrastFloor = 20;
            return new AnchorScale(unit, brightnessFloor, colorBrightnessFloor, contrastFloor);
        }
    }

    private static final class Component {
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;
        private int pixelCount;
        private double hueSum;
        private double saturationSum;
        private double brightnessSum;
        private final List<Pixel> pixels = new ArrayList<>();

        private void add(int x, int y, int rgb) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            pixelCount++;
            pixels.add(new Pixel(x, y, rgb));
            float[] hsb = java.awt.Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, null);
            hueSum += hsb[0] * 360.0d;
            saturationSum += hsb[1];
            brightnessSum += hsb[2];
        }

        private void addAll(Component other) {
            for (Pixel pixel : other.pixels) {
                add(pixel.x(), pixel.y(), pixel.rgb());
            }
        }

        private Component copy() {
            Component copy = new Component();
            copy.addAll(this);
            return copy;
        }

        private int minX() {
            return minX;
        }

        private int minY() {
            return minY;
        }

        private int width() {
            return maxX - minX + 1;
        }

        private int height() {
            return maxY - minY + 1;
        }

        private double averageHue() {
            return pixelCount == 0 ? 0.0d : hueSum / pixelCount;
        }

        private double averageSaturation() {
            return pixelCount == 0 ? 0.0d : saturationSum / pixelCount;
        }

        private double averageBrightness() {
            return pixelCount == 0 ? 0.0d : brightnessSum / pixelCount;
        }

        private double chromaticHue() {
            double x = 0.0d;
            double y = 0.0d;
            for (Pixel pixel : pixels) {
                float[] hsb = hsb(pixel.rgb());
                double weight = hsb[1] * hsb[2];
                double angle = hsb[0] * Math.PI * 2.0d;
                x += Math.cos(angle) * weight;
                y += Math.sin(angle) * weight;
            }
            double hue = Math.toDegrees(Math.atan2(y, x));
            return hue < 0.0d ? hue + 360.0d : hue;
        }

        private double coloredRatio() {
            long colored = pixels.stream().filter(pixel -> {
                float[] hsb = hsb(pixel.rgb());
                return hsb[1] >= 0.25f && hsb[2] >= 0.25f;
            }).count();
            return pixelCount == 0 ? 0.0d : colored / (double) pixelCount;
        }

        private double centerFill() {
            int startX = minX + width() / 4;
            int endX = maxX - width() / 4;
            int startY = minY + height() / 4;
            int endY = maxY - height() / 4;
            int centerArea = Math.max(1, (endX - startX + 1) * (endY - startY + 1));
            long centerPixels = pixels.stream()
                    .filter(pixel -> pixel.x() >= startX && pixel.x() <= endX
                            && pixel.y() >= startY && pixel.y() <= endY)
                    .count();
            return centerPixels / (double) centerArea;
        }

        private double borderFill() {
            int thickness = Math.max(1, (int) Math.round(Math.min(width(), height()) * 0.15d));
            int innerWidth = Math.max(0, width() - thickness * 2);
            int innerHeight = Math.max(0, height() - thickness * 2);
            int borderArea = Math.max(1, width() * height() - innerWidth * innerHeight);
            long borderPixels = pixels.stream()
                    .filter(pixel -> pixel.x() < minX + thickness || pixel.x() > maxX - thickness
                            || pixel.y() < minY + thickness || pixel.y() > maxY - thickness)
                    .count();
            return borderPixels / (double) borderArea;
        }

        private double holeRatio() {
            boolean[][] mask = mask();
            boolean[][] outside = new boolean[height()][width()];
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            for (int x = 0; x < width(); x++) {
                enqueueBackground(mask, outside, queue, x, 0);
                enqueueBackground(mask, outside, queue, x, height() - 1);
            }
            for (int y = 0; y < height(); y++) {
                enqueueBackground(mask, outside, queue, 0, y);
                enqueueBackground(mask, outside, queue, width() - 1, y);
            }
            while (!queue.isEmpty()) {
                int[] point = queue.removeFirst();
                int x = point[0];
                int y = point[1];
                enqueueBackground(mask, outside, queue, x - 1, y);
                enqueueBackground(mask, outside, queue, x + 1, y);
                enqueueBackground(mask, outside, queue, x, y - 1);
                enqueueBackground(mask, outside, queue, x, y + 1);
            }
            int holes = 0;
            for (int y = 0; y < height(); y++) {
                for (int x = 0; x < width(); x++) {
                    if (!mask[y][x] && !outside[y][x]) {
                        holes++;
                    }
                }
            }
            return holes / (double) (width() * height());
        }

        private boolean[][] mask() {
            boolean[][] mask = new boolean[height()][width()];
            for (Pixel pixel : pixels) {
                mask[pixel.y() - minY][pixel.x() - minX] = true;
            }
            return mask;
        }

        private static void enqueueBackground(boolean[][] mask,
                                              boolean[][] outside,
                                              ArrayDeque<int[]> queue,
                                              int x,
                                              int y) {
            if (y < 0 || y >= mask.length || x < 0 || x >= mask[0].length
                    || mask[y][x] || outside[y][x]) {
                return;
            }
            outside[y][x] = true;
            queue.addLast(new int[]{x, y});
        }

        private static float[] hsb(int rgb) {
            return java.awt.Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, null);
        }

        private double centerX() {
            return minX + width() / 2.0d;
        }

        private double centerY() {
            return minY + height() / 2.0d;
        }

    }

    private record Pixel(int x, int y, int rgb) {
    }
}
