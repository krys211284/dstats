package krys.itemimport.image;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Mały POC OCR dla ROI PART_1 oparty o Tess4J i zasób tessdata/pol.traineddata. */
public final class TesseractPart1RoiOcrReader implements Part1RoiOcrReader {
    public static final String LANGUAGE = "pol";
    public static final String PREPROCESSING =
            "roi-specific Tess4J profiles; NAME_TEXT uses internal sub-ROI and best generic variant";

    private static final int DEFAULT_SCALE = 3;
    private static final int NAME_SCALE = 4;
    private static final int BORDER = 18;
    private static final String OUTPUT_FORMAT = "PNG";
    private static final String POL_TRAINEDDATA_RESOURCE = "/tessdata/pol.traineddata";
    private static final String LETTER_WHITELIST =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
                    + "ĄĆĘŁŃÓŚŹŻąćęłńóśźż -'";
    private static final String ITEM_POWER_WHITELIST =
            LETTER_WHITELIST + "0123456789:+,.*()[]";

    private final Path tessdataDirectory;

    public TesseractPart1RoiOcrReader() {
        this(resolveBundledTessdataDirectory());
    }

    TesseractPart1RoiOcrReader(Path tessdataDirectory) {
        this.tessdataDirectory = Objects.requireNonNull(tessdataDirectory, "tessdataDirectory");
    }

    @Override
    public Part1RoiOcrResult read(Part1Roi roi) {
        Objects.requireNonNull(roi, "roi");
        long start = System.nanoTime();
        try {
            BufferedImage source = decode(roi);
            return switch (roi.getType()) {
                case NAME_AND_STARS -> readNameAndStars(roi, source, start);
                case CLASSIFICATION -> readGeneric(roi, source, start, profile(
                        "CLASSIFICATION binary-x3 psm-single-block letters",
                        DEFAULT_SCALE,
                        ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK,
                        LETTER_WHITELIST,
                        PreprocessMode.BINARY_BRIGHT_TEXT
                ));
                case ITEM_POWER -> readGeneric(roi, source, start, profile(
                        "ITEM_POWER binary-x3 psm-single-line",
                        DEFAULT_SCALE,
                        ITessAPI.TessPageSegMode.PSM_SINGLE_LINE,
                        ITEM_POWER_WHITELIST,
                        PreprocessMode.BINARY_BRIGHT_TEXT
                ));
                case OPTIONAL_INFO -> readGeneric(roi, source, start, profile(
                        "OPTIONAL_INFO binary-x3 psm-single-block",
                        DEFAULT_SCALE,
                        ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK,
                        "",
                        PreprocessMode.BINARY_BRIGHT_TEXT
                ));
            };
        } catch (IOException | TesseractException exception) {
            throw new IllegalStateException("Nie udalo sie wykonac OCR Tesseract dla ROI PART_1 " + roi.getType() + ".", exception);
        }
    }

    Path getTessdataDirectory() {
        return tessdataDirectory;
    }

    private Part1RoiOcrResult readNameAndStars(Part1Roi roi, BufferedImage source, long start)
            throws IOException, TesseractException {
        NameRegions regions = splitNameRegions(source);
        Rectangle nameRectangle = regions.nameText();
        BufferedImage nameImage = source.getSubimage(
                nameRectangle.x,
                nameRectangle.y,
                nameRectangle.width,
                nameRectangle.height
        );
        NameCleanupResult cleanup = cleanupNameDecoration(nameImage);
        BufferedImage ocrImage = cleanup.image();
        List<OcrProfile> profiles = List.of(
                profile("NAME_TEXT inverted-grayscale-x4 psm-single-block letters", NAME_SCALE,
                        ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK, LETTER_WHITELIST, PreprocessMode.INVERTED_GRAYSCALE),
                profile("NAME_TEXT binary-x4 psm-single-block letters", NAME_SCALE,
                        ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK, LETTER_WHITELIST, PreprocessMode.BINARY_BRIGHT_TEXT),
                profile("NAME_TEXT inverted-grayscale-x4 psm-sparse-text letters", NAME_SCALE,
                        ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT, LETTER_WHITELIST, PreprocessMode.INVERTED_GRAYSCALE)
        );
        OcrAttempt bestAttempt = profiles.stream()
                .map(profile -> attempt(ocrImage, profile))
                .max(Comparator.comparingDouble(OcrAttempt::score))
                .orElseThrow();
        BufferedImage finalOcrImage = bestAttempt.preparedImage() == null
                ? preprocess(ocrImage, bestAttempt.profile())
                : bestAttempt.preparedImage();
        List<StarCandidate> starCandidates = detectGreaterAffixStars(source);

        List<Part1RoiOcrSubRegion> subRegions = new ArrayList<>();
        subRegions.add(new Part1RoiOcrSubRegion(
                "NAME_TEXT",
                nameRectangle.x,
                nameRectangle.y,
                nameRectangle.width,
                nameRectangle.height,
                OUTPUT_FORMAT,
                encodePng(nameImage),
                "source before decoration cleanup"
        ));
        subRegions.add(new Part1RoiOcrSubRegion(
                "NAME_TEXT_CLEANED",
                nameRectangle.x,
                nameRectangle.y,
                nameRectangle.width,
                nameRectangle.height,
                OUTPUT_FORMAT,
                encodePng(ocrImage),
                cleanup.description()
        ));
        subRegions.add(new Part1RoiOcrSubRegion(
                "NAME_TEXT_FINAL",
                0,
                0,
                finalOcrImage.getWidth(),
                finalOcrImage.getHeight(),
                OUTPUT_FORMAT,
                encodePng(finalOcrImage),
                bestAttempt.profile().description()
        ));
        regions.optionalStars().ifPresent(stars -> subRegions.add(new Part1RoiOcrSubRegion(
                "STARS_REGION",
                stars.x,
                stars.y,
                stars.width,
                stars.height,
                OUTPUT_FORMAT,
                encodePng(source.getSubimage(stars.x, stars.y, stars.width, stars.height)),
                "not OCRed; prepared for visual greater-affix counting"
        )));
        for (int index = 0; index < starCandidates.size(); index++) {
            StarCandidate candidate = starCandidates.get(index);
            Rectangle bounds = candidate.bounds();
            subRegions.add(new Part1RoiOcrSubRegion(
                    "GREATER_AFFIX_STAR_" + (index + 1),
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    OUTPUT_FORMAT,
                    encodePng(source.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height)),
                    "visual star candidate confidence=" + String.format(java.util.Locale.ROOT, "%.3f", candidate.confidence())
            ));
        }

        return new Part1RoiOcrResult(
                roi.getType(),
                bestAttempt.rawText(),
                elapsedMillis(start),
                bestAttempt.profile().description(),
                subRegions,
                starCandidates.size()
        );
    }

    private Part1RoiOcrResult readGeneric(Part1Roi roi, BufferedImage source, long start, OcrProfile profile)
            throws IOException, TesseractException {
        OcrAttempt attempt = attempt(source, profile);
        return new Part1RoiOcrResult(roi.getType(), attempt.rawText(), elapsedMillis(start), profile.description());
    }

    private OcrAttempt attempt(BufferedImage source, OcrProfile profile) {
        try {
            BufferedImage prepared = preprocess(source, profile);
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessdataDirectory.toString());
            tesseract.setLanguage(LANGUAGE);
            tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
            tesseract.setPageSegMode(profile.pageSegMode());
            if (!profile.whitelist().isBlank()) {
                tesseract.setVariable("tessedit_char_whitelist", profile.whitelist());
            }
            String rawText = tesseract.doOCR(prepared);
            return new OcrAttempt(profile, rawText, nameTextScore(rawText), prepared);
        } catch (TesseractException exception) {
            return new OcrAttempt(profile, "", -1_000_000.0d, null);
        }
    }

    private static Path resolveBundledTessdataDirectory() {
        Path directory = Path.of(System.getProperty("java.io.tmpdir"), "dstats-tessdata", "part1-poc");
        Path polishData = directory.resolve("pol.traineddata");
        try {
            Files.createDirectories(directory);
            try (InputStream inputStream = TesseractPart1RoiOcrReader.class.getResourceAsStream(POL_TRAINEDDATA_RESOURCE)) {
                if (inputStream == null) {
                    throw new IllegalStateException("Brak zasobu " + POL_TRAINEDDATA_RESOURCE + " dla Tesseract OCR.");
                }
                Files.copy(inputStream, polishData, StandardCopyOption.REPLACE_EXISTING);
            }
            return directory;
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udalo sie przygotowac lokalnego katalogu tessdata dla PART_1 OCR.", exception);
        }
    }

    private static BufferedImage decode(Part1Roi roi) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(roi.getImageBytes()));
        if (image == null) {
            throw new IOException("Nie udalo sie zdekodowac obrazu ROI PART_1.");
        }
        return image;
    }

    private static NameRegions splitNameRegions(BufferedImage source) {
        Rectangle textBounds = nameTextBounds(source);
        Rectangle stars = detectStarsRegion(source, textBounds);
        Rectangle name = new Rectangle(textBounds);
        if (isReliableStarsRegion(source, stars) && stars.x > name.x + source.getWidth() / 4) {
            int right = Math.max(name.x + 1, stars.x - Math.max(4, source.getWidth() / 60));
            name.width = right - name.x;
        } else {
            stars = new Rectangle();
        }
        name = pad(name, source.getWidth(), source.getHeight(), 3, 2);
        Rectangle starsRegion = stars.width <= 0
                ? new Rectangle()
                : pad(stars, source.getWidth(), source.getHeight(), 4, 4);
        return new NameRegions(name, starsRegion.width <= 0 ? null : starsRegion);
    }

    private static Rectangle nameTextBounds(BufferedImage source) {
        List<Rectangle> bands = detectNameTextBands(source);
        if (bands.isEmpty()) {
            return activeTextBounds(source);
        }
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = 0;
        int maxY = 0;
        for (Rectangle band : bands) {
            minX = Math.min(minX, band.x);
            minY = Math.min(minY, band.y);
            maxX = Math.max(maxX, band.x + band.width);
            maxY = Math.max(maxY, band.y + band.height);
        }
        Rectangle bounds = new Rectangle(minX, minY, maxX - minX, maxY - minY);
        bounds.x = refineNameTextLeft(source, bounds);
        bounds.width = maxX - bounds.x;
        return pad(bounds, source.getWidth(), source.getHeight(), Math.max(4, source.getWidth() / 55), 2);
    }

    private static int refineNameTextLeft(BufferedImage source, Rectangle bounds) {
        int minColumnActivity = Math.max(3, bounds.height / 18);
        List<Range> ranges = new ArrayList<>();
        int start = -1;
        int signalInRun = 0;
        List<Integer> runSignals = new ArrayList<>();
        for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
            int activity = 0;
            for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
                if (isNameTextSignal(source.getRGB(x, y))) {
                    activity++;
                }
            }
            if (activity >= minColumnActivity) {
                if (start < 0) {
                    start = x;
                    signalInRun = 0;
                }
                signalInRun += activity;
                continue;
            }
            if (start >= 0) {
                ranges.add(new Range(start, x));
                runSignals.add(signalInRun);
                start = -1;
            }
        }
        if (start >= 0) {
            ranges.add(new Range(start, bounds.x + bounds.width));
            runSignals.add(signalInRun);
        }
        if (ranges.size() < 2) {
            return bounds.x;
        }
        int largestSignal = runSignals.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minimumTextRunWidth = Math.max(6, bounds.width / 45);
        for (int index = 0; index < ranges.size(); index++) {
            Range range = ranges.get(index);
            int runWidth = range.endExclusive() - range.start();
            int runSignal = runSignals.get(index);
            if (runWidth >= minimumTextRunWidth || runSignal >= largestSignal * 0.24d) {
                return range.start();
            }
        }
        return bounds.x;
    }

    private static List<Rectangle> detectNameTextBands(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int activityThreshold = Math.max(5, width / 90);
        int minimumBandWidth = Math.max(24, width / 7);
        boolean[] activeRows = new boolean[height];
        for (int y = 0; y < height; y++) {
            int activity = 0;
            for (int x = 0; x < width; x++) {
                if (isNameTextSignal(source.getRGB(x, y))) {
                    activity++;
                }
            }
            activeRows[y] = activity >= activityThreshold;
        }

        List<Rectangle> bands = new ArrayList<>();
        int startY = -1;
        int inactiveGap = 0;
        for (int y = 0; y < height; y++) {
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
                    addNameBand(source, bands, startY, y - inactiveGap + 1, minimumBandWidth);
                    startY = -1;
                    inactiveGap = 0;
                }
            }
        }
        if (startY >= 0) {
            addNameBand(source, bands, startY, height - inactiveGap, minimumBandWidth);
        }
        return bands;
    }

    private static void addNameBand(BufferedImage source,
                                    List<Rectangle> bands,
                                    int startY,
                                    int endY,
                                    int minimumBandWidth) {
        if (endY - startY < Math.max(5, source.getHeight() / 18)) {
            return;
        }
        int minX = source.getWidth();
        int maxX = 0;
        int signalPixels = 0;
        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (isNameTextSignal(source.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x + 1);
                    signalPixels++;
                }
            }
        }
        if (maxX <= minX || maxX - minX < minimumBandWidth || signalPixels < minimumBandWidth) {
            return;
        }
        bands.add(new Rectangle(minX, startY, maxX - minX, endY - startY));
    }

    private static boolean isReliableStarsRegion(BufferedImage source, Rectangle stars) {
        if (stars.width <= 0 || stars.height <= 0) {
            return false;
        }
        boolean rightEdge = stars.x >= source.getWidth() * 2 / 3;
        boolean compact = stars.width <= source.getWidth() / 4 && stars.height <= source.getHeight() * 3 / 4;
        return rightEdge && compact;
    }

    private static Rectangle activeTextBounds(BufferedImage source) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = 0;
        int maxY = 0;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (isNameTextSignal(source.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x + 1);
                    maxY = Math.max(maxY, y + 1);
                }
            }
        }
        if (maxX <= minX || maxY <= minY) {
            return new Rectangle(0, 0, source.getWidth(), source.getHeight());
        }
        return pad(new Rectangle(minX, minY, maxX - minX, maxY - minY), source.getWidth(), source.getHeight(), 4, 4);
    }

    private static Rectangle detectStarsRegion(BufferedImage source, Rectangle textBounds) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = 0;
        int maxY = 0;
        int minSearchX = Math.max(source.getWidth() / 3, textBounds.x + textBounds.width / 2);
        for (int y = Math.max(0, textBounds.y); y < Math.min(source.getHeight(), textBounds.y + textBounds.height); y++) {
            for (int x = minSearchX; x < source.getWidth(); x++) {
                if (isStarLikeSignal(source.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x + 1);
                    maxY = Math.max(maxY, y + 1);
                }
            }
        }
        if (maxX <= minX || maxY <= minY) {
            return new Rectangle();
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private static boolean isNameTextSignal(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        int luma = luma(rgb);
        boolean bright = luma >= 85 && max - min >= 12;
        boolean pale = luma >= 115 && max - min <= 85;
        return bright || pale;
    }

    private static boolean isStarLikeSignal(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        int luma = luma(rgb);
        boolean paleStar = luma >= 150 && max - min <= 90;
        boolean warmStar = red >= 150 && green >= 115 && blue <= 190 && red - blue >= 18;
        return paleStar || warmStar;
    }

    private static List<StarCandidate> detectGreaterAffixStars(BufferedImage source) {
        List<SignalComponent> components = detectComponents(source, TesseractPart1RoiOcrReader::isGreaterAffixStarCoreSignal);
        List<StarCandidate> candidates = new ArrayList<>();
        for (SignalComponent component : components) {
            Optional<StarCandidate> candidate = toStarCandidate(source, component);
            candidate.ifPresent(candidates::add);
        }
        candidates.sort(Comparator
                .comparingInt((StarCandidate candidate) -> candidate.bounds().y)
                .thenComparingInt(candidate -> candidate.bounds().x));
        return candidates;
    }

    private static Optional<StarCandidate> toStarCandidate(BufferedImage source, SignalComponent component) {
        int width = source.getWidth();
        int height = source.getHeight();
        int minSize = Math.max(5, Math.min(width, height) / 18);
        int maxSize = Math.max(18, Math.min(width, height) * 2 / 3);
        int minArea = Math.max(56, width * height / 260);
        if (component.touchesTop() || component.touchesLeft() || component.area() < minArea) {
            return Optional.empty();
        }
        if (component.width() < minSize || component.height() < minSize
                || component.width() > maxSize || component.height() > maxSize) {
            return Optional.empty();
        }
        double aspect = component.width() / (double) component.height();
        if (aspect < 0.55d || aspect > 1.85d) {
            return Optional.empty();
        }
        double density = component.area() / (double) (component.width() * component.height());
        if (density < 0.48d || density > 0.78d) {
            return Optional.empty();
        }
        double squareScore = 1.0d - Math.min(1.0d, Math.abs(1.0d - aspect));
        double sizeScore = Math.min(1.0d, component.area() / (double) Math.max(12, width * height / 1_500));
        double colorScore = averageStarColorScore(source, component);
        double confidence = clamp01(0.38d * squareScore + 0.34d * colorScore + 0.28d * sizeScore);
        if (confidence < 0.56d) {
            return Optional.empty();
        }
        Rectangle bounds = pad(new Rectangle(component.minX(), component.minY(), component.width(), component.height()),
                width, height, 3, 3);
        return Optional.of(new StarCandidate(bounds, confidence));
    }

    private static double averageStarColorScore(BufferedImage source, SignalComponent component) {
        double score = 0.0d;
        for (int pixel : component.pixels()) {
            int x = pixel % source.getWidth();
            int y = pixel / source.getWidth();
            score += starColorScore(source.getRGB(x, y));
        }
        return component.pixels().isEmpty() ? 0.0d : score / component.pixels().size();
    }

    private static boolean isGreaterAffixStarCoreSignal(int rgb) {
        return starColorScore(rgb) >= 0.56d;
    }

    private static double starColorScore(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        int luma = luma(rgb);
        if (blue < 120 || red < 170 || green < 110 || luma < 155) {
            return 0.0d;
        }
        double brightness = clamp01((luma - 155) / 70.0d);
        double pale = clamp01(1.0d - Math.max(0, max - min - 95) / 80.0d);
        double pink = clamp01((red - blue + 35) / 90.0d) * clamp01((red - green + 55) / 110.0d);
        return clamp01(0.50d * brightness + 0.30d * pale + 0.20d * pink);
    }

    private static List<SignalComponent> detectComponents(BufferedImage source, PixelPredicate predicate) {
        int width = source.getWidth();
        int height = source.getHeight();
        boolean[][] visited = new boolean[height][width];
        List<SignalComponent> components = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (visited[y][x] || !predicate.test(source.getRGB(x, y))) {
                    continue;
                }
                components.add(readSignalComponent(source, x, y, visited, predicate));
            }
        }
        return components;
    }

    private static NameCleanupResult cleanupNameDecoration(BufferedImage source) {
        List<SignalComponent> components = detectComponents(source, TesseractPart1RoiOcrReader::isDecorationCleanupSignal);
        int textStartX = detectProtectedTextStart(source, components);
        boolean[][] removed = new boolean[source.getHeight()][source.getWidth()];
        boolean[][] protectedSignal = new boolean[source.getHeight()][source.getWidth()];
        int removedComponents = 0;
        int removedPixels = 0;

        for (SignalComponent component : components) {
            if (isDecorativeEdgeComponent(source, component, textStartX)) {
                removedComponents++;
                for (int pixel : component.pixels()) {
                    int x = pixel % source.getWidth();
                    int y = pixel / source.getWidth();
                    if (!removed[y][x]) {
                        removed[y][x] = true;
                        removedPixels++;
                    }
                }
                continue;
            }
            for (int pixel : component.pixels()) {
                int x = pixel % source.getWidth();
                int y = pixel / source.getWidth();
                protectedSignal[y][x] = true;
            }
        }

        boolean[][] mask = dilateRemovedMask(removed, protectedSignal);
        BufferedImage cleaned = copyImage(source);
        int fallbackBackground = estimateGlobalBackground(source, mask);
        for (int y = 0; y < source.getHeight(); y++) {
            int rowBackground = estimateRowBackground(source, mask, y, fallbackBackground);
            for (int x = 0; x < source.getWidth(); x++) {
                if (mask[y][x]) {
                    cleaned.setRGB(x, y, rowBackground);
                }
            }
        }

        int maskedPixels = count(mask);
        String description = "edge-connected frame/ornament cleanup: removedComponents="
                + removedComponents + ", signalPixels=" + removedPixels + ", maskedPixels=" + maskedPixels;
        return new NameCleanupResult(cleaned, description);
    }

    private static SignalComponent readSignalComponent(BufferedImage source,
                                                       int startX,
                                                       int startY,
                                                       boolean[][] visited,
                                                       PixelPredicate predicate) {
        int width = source.getWidth();
        int height = source.getHeight();
        Deque<Integer> queue = new ArrayDeque<>();
        List<Integer> pixels = new ArrayList<>();
        int minX = startX;
        int minY = startY;
        int maxX = startX + 1;
        int maxY = startY + 1;
        boolean touchesTop = startY == 0;
        boolean touchesLeft = startX == 0;
        visited[startY][startX] = true;
        queue.add(startY * width + startX);

        while (!queue.isEmpty()) {
            int pixel = queue.removeFirst();
            pixels.add(pixel);
            int x = pixel % width;
            int y = pixel / width;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + 1);
            maxY = Math.max(maxY, y + 1);
            touchesTop = touchesTop || y == 0;
            touchesLeft = touchesLeft || x == 0;

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) {
                        continue;
                    }
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height || visited[ny][nx]) {
                        continue;
                    }
                    visited[ny][nx] = true;
                    if (predicate.test(source.getRGB(nx, ny))) {
                        queue.addLast(ny * width + nx);
                    }
                }
            }
        }
        return new SignalComponent(pixels, minX, minY, maxX, maxY, touchesTop, touchesLeft);
    }

    private static int detectProtectedTextStart(BufferedImage source, List<SignalComponent> components) {
        int width = source.getWidth();
        int height = source.getHeight();
        int minimumTextArea = Math.max(10, width * height / 900);
        int minimumTextHeight = Math.max(5, height / 12);
        int textStartX = width;
        for (SignalComponent component : components) {
            if (component.touchesTop() || isWideFrameAggregate(source, component)) {
                continue;
            }
            if (component.area() < minimumTextArea || component.height() < minimumTextHeight) {
                continue;
            }
            textStartX = Math.min(textStartX, component.minX());
        }
        if (textStartX < width) {
            return textStartX;
        }
        return Math.max(1, width / 5);
    }

    private static boolean isDecorativeEdgeComponent(BufferedImage source, SignalComponent component, int textStartX) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (component.area() < Math.max(8, width * height / 4_000)) {
            return false;
        }
        boolean topLeftBeforeText = component.minX() <= Math.max(4, width / 12)
                && component.maxX() <= textStartX - 2
                && component.minY() <= Math.max(6, height / 2);
        if (topLeftBeforeText) {
            return true;
        }
        if (!component.touchesTop() && !component.touchesLeft()) {
            return false;
        }
        boolean topConnected = component.touchesTop();
        boolean beforeText = component.maxX() <= Math.max(textStartX - 2, width / 16);
        boolean cornerConnected = component.touchesTop() && component.touchesLeft();
        boolean leftHeaderFrame = component.touchesLeft()
                && component.minY() <= Math.max(4, height / 5)
                && component.maxX() <= Math.max(textStartX + 2, width / 7);
        return topConnected || beforeText || cornerConnected || leftHeaderFrame;
    }

    private static boolean isWideFrameAggregate(BufferedImage source, SignalComponent component) {
        return component.width() > source.getWidth() / 2
                && component.area() > Math.max(40, source.getWidth() * source.getHeight() / 80);
    }

    private static boolean[][] dilateRemovedMask(boolean[][] removed, boolean[][] protectedSignal) {
        int height = removed.length;
        int width = removed[0].length;
        boolean[][] dilated = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!removed[y][x]) {
                    continue;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= width || ny >= height
                                || protectedSignal[ny][nx] && !removed[ny][nx]) {
                            continue;
                        }
                        dilated[ny][nx] = true;
                    }
                }
            }
        }
        return dilated;
    }

    private static int estimateGlobalBackground(BufferedImage source, boolean[][] mask) {
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        int count = 0;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int rgb = source.getRGB(x, y);
                if (!mask[y][x] && isNameBackgroundLike(rgb)) {
                    red += (rgb >> 16) & 0xff;
                    green += (rgb >> 8) & 0xff;
                    blue += rgb & 0xff;
                    count++;
                }
            }
        }
        if (count == 0) {
            return Color.BLACK.getRGB();
        }
        return new Color((int) (red / count), (int) (green / count), (int) (blue / count)).getRGB();
    }

    private static int estimateRowBackground(BufferedImage source, boolean[][] mask, int y, int fallback) {
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        int count = 0;
        for (int x = 0; x < source.getWidth(); x++) {
            int rgb = source.getRGB(x, y);
            if (!mask[y][x] && isNameBackgroundLike(rgb)) {
                red += (rgb >> 16) & 0xff;
                green += (rgb >> 8) & 0xff;
                blue += rgb & 0xff;
                count++;
            }
        }
        if (count == 0) {
            return fallback;
        }
        return new Color((int) (red / count), (int) (green / count), (int) (blue / count)).getRGB();
    }

    private static boolean isNameBackgroundLike(int rgb) {
        return luma(rgb) <= 135 && !isNameTextSignal(rgb);
    }

    private static boolean isDecorationCleanupSignal(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        int luma = luma(rgb);
        boolean saturatedFrameOrText = luma >= 125 && max - min >= 24;
        boolean paleFrame = luma >= 155 && max - min <= 96;
        boolean purpleFrame = luma >= 75 && blue >= red + 8 && blue >= green + 8 && max - min >= 18;
        return saturatedFrameOrText || paleFrame || purpleFrame;
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private static int count(boolean[][] mask) {
        int count = 0;
        for (boolean[] row : mask) {
            for (boolean pixel : row) {
                if (pixel) {
                    count++;
                }
            }
        }
        return count;
    }

    private static Rectangle pad(Rectangle rectangle, int imageWidth, int imageHeight, int padX, int padY) {
        int x = clamp(rectangle.x - padX, 0, imageWidth - 1);
        int y = clamp(rectangle.y - padY, 0, imageHeight - 1);
        int right = clamp(rectangle.x + rectangle.width + padX, x + 1, imageWidth);
        int bottom = clamp(rectangle.y + rectangle.height + padY, y + 1, imageHeight);
        return new Rectangle(x, y, right - x, bottom - y);
    }

    private static BufferedImage preprocess(BufferedImage source, OcrProfile profile) {
        BufferedImage scaled = new BufferedImage(source.getWidth() * profile.scale(), source.getHeight() * profile.scale(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        graphics.dispose();

        return switch (profile.mode()) {
            case INVERTED_GRAYSCALE -> invertedGrayscale(scaled);
            case BINARY_BRIGHT_TEXT -> binaryBrightText(scaled);
        };
    }

    private static BufferedImage invertedGrayscale(BufferedImage scaled) {
        BufferedImage prepared = new BufferedImage(scaled.getWidth() + BORDER * 2, scaled.getHeight() + BORDER * 2, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = prepared.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, prepared.getWidth(), prepared.getHeight());
        graphics.dispose();
        for (int y = 0; y < scaled.getHeight(); y++) {
            for (int x = 0; x < scaled.getWidth(); x++) {
                int inverted = 255 - luma(scaled.getRGB(x, y));
                int rgb = new Color(inverted, inverted, inverted).getRGB();
                prepared.setRGB(x + BORDER, y + BORDER, rgb);
            }
        }
        return prepared;
    }

    private static BufferedImage binaryBrightText(BufferedImage scaled) {
        int[] lumas = new int[scaled.getWidth() * scaled.getHeight()];
        int cursor = 0;
        for (int y = 0; y < scaled.getHeight(); y++) {
            for (int x = 0; x < scaled.getWidth(); x++) {
                lumas[cursor++] = luma(scaled.getRGB(x, y));
            }
        }
        int threshold = otsuThreshold(lumas);

        BufferedImage binary = new BufferedImage(scaled.getWidth() + BORDER * 2, scaled.getHeight() + BORDER * 2, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D binaryGraphics = binary.createGraphics();
        binaryGraphics.setColor(Color.WHITE);
        binaryGraphics.fillRect(0, 0, binary.getWidth(), binary.getHeight());
        binaryGraphics.dispose();

        for (int y = 0; y < scaled.getHeight(); y++) {
            for (int x = 0; x < scaled.getWidth(); x++) {
                int gray = luma(scaled.getRGB(x, y));
                binary.setRGB(x + BORDER, y + BORDER, gray >= threshold ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return binary;
    }

    private static int luma(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        return (red * 299 + green * 587 + blue * 114) / 1000;
    }

    private static int otsuThreshold(int[] values) {
        int[] histogram = new int[256];
        for (int value : values) {
            histogram[Math.max(0, Math.min(255, value))]++;
        }

        int total = values.length;
        long sum = 0L;
        for (int value = 0; value < histogram.length; value++) {
            sum += (long) value * histogram[value];
        }

        long backgroundWeight = 0L;
        long backgroundSum = 0L;
        double bestVariance = -1.0d;
        int bestThreshold = 128;
        for (int threshold = 0; threshold < histogram.length; threshold++) {
            backgroundWeight += histogram[threshold];
            if (backgroundWeight == 0) {
                continue;
            }
            long foregroundWeight = total - backgroundWeight;
            if (foregroundWeight == 0) {
                break;
            }
            backgroundSum += (long) threshold * histogram[threshold];
            double backgroundMean = backgroundSum / (double) backgroundWeight;
            double foregroundMean = (sum - backgroundSum) / (double) foregroundWeight;
            double variance = backgroundWeight * (double) foregroundWeight
                    * (backgroundMean - foregroundMean)
                    * (backgroundMean - foregroundMean);
            if (variance > bestVariance) {
                bestVariance = variance;
                bestThreshold = threshold;
            }
        }
        return Math.max(55, Math.min(220, bestThreshold));
    }

    private static double nameTextScore(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return -10_000.0d;
        }
        int letters = 0;
        int whitespace = 0;
        int digits = 0;
        int punctuation = 0;
        int shortLinePenalty = 0;
        for (int index = 0; index < rawText.length(); index++) {
            char character = rawText.charAt(index);
            if (Character.isLetter(character)) {
                letters++;
            } else if (Character.isWhitespace(character)) {
                whitespace++;
            } else if (Character.isDigit(character)) {
                digits++;
            } else {
                punctuation++;
            }
        }
        long lines = rawText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .count();
        for (String line : rawText.lines().map(String::trim).filter(line -> !line.isBlank()).toList()) {
            long lineLetters = line.chars().filter(Character::isLetter).count();
            if (line.length() <= 2 || lineLetters < 3) {
                shortLinePenalty += 14;
            } else if (line.length() <= 4) {
                shortLinePenalty += 10;
            }
        }
        return letters * 4.0d
                + whitespace * 0.6d
                + Math.min(lines, 4) * 3.0d
                - digits * 3.0d
                - punctuation * 2.0d
                - shortLinePenalty;
    }

    private static OcrProfile profile(String description, int scale, int pageSegMode, String whitelist, PreprocessMode mode) {
        return new OcrProfile(description, scale, pageSegMode, whitelist == null ? "" : whitelist, mode);
    }

    private static byte[] encodePng(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, OUTPUT_FORMAT, outputStream)) {
                throw new IllegalStateException("Nie udalo sie zakodowac sub-ROI OCR do PNG.");
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udalo sie zakodowac sub-ROI OCR do PNG.", exception);
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

    private static double clamp01(double value) {
        if (value < 0.0d) {
            return 0.0d;
        }
        if (value > 1.0d) {
            return 1.0d;
        }
        return value;
    }

    private static long elapsedMillis(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private enum PreprocessMode {
        BINARY_BRIGHT_TEXT,
        INVERTED_GRAYSCALE
    }

    private record NameRegions(Rectangle nameText, Rectangle stars) {
        private Optional<Rectangle> optionalStars() {
            return stars == null ? Optional.empty() : Optional.of(stars);
        }
    }

    private record NameCleanupResult(BufferedImage image, String description) {
    }

    private record StarCandidate(Rectangle bounds, double confidence) {
    }

    private record SignalComponent(List<Integer> pixels,
                                   int minX,
                                   int minY,
                                   int maxX,
                                   int maxY,
                                   boolean touchesTop,
                                   boolean touchesLeft) {
        private int area() {
            return pixels.size();
        }

        private int height() {
            return maxY - minY;
        }

        private int width() {
            return maxX - minX;
        }
    }

    private record Range(int start, int endExclusive) {
    }

    private record OcrProfile(String description, int scale, int pageSegMode, String whitelist, PreprocessMode mode) {
    }

    private record OcrAttempt(OcrProfile profile, String rawText, double score, BufferedImage preparedImage) {
    }

    @FunctionalInterface
    private interface PixelPredicate {
        boolean test(int rgb);
    }
}
