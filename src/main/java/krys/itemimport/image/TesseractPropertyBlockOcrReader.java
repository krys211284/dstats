package krys.itemimport.image;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Surowy OCR Tess4J dla pojedynczego PropertyBlock ROI. */
public final class TesseractPropertyBlockOcrReader implements PropertyBlockOcrReader {
    public static final String LANGUAGE = "pol";
    public static final String PREPROCESSING =
            "property-block Tess4J profiles: anchor-mask, edge-trim, luma/grayscale x3-x5";

    private static final String OUTPUT_FORMAT = "PNG";
    private static final String POL_TRAINEDDATA_RESOURCE = "/tessdata/pol.traineddata";
    private static final int BORDER = 24;
    private static final int TOP_EDGE_TRIM = 3;
    private static final int RIGHT_EDGE_TRIM = 3;
    private static final Pattern POLISH_DECIMAL_PERCENT = Pattern.compile("(?<!\\d)\\d+,\\d%(?!\\d)");
    private static final String PROPERTY_BLOCK_WHITELIST =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
                    + "ĄĆĘŁŃÓŚŹŻąćęłńóśźż"
                    + "0123456789 +-.,:%/()[]'\"";

    private final Path tessdataDirectory;

    public TesseractPropertyBlockOcrReader() {
        this(resolveBundledTessdataDirectory());
    }

    TesseractPropertyBlockOcrReader(Path tessdataDirectory) {
        this.tessdataDirectory = Objects.requireNonNull(tessdataDirectory, "tessdataDirectory");
    }

    @Override
    public PropertyBlockReadResult read(PropertyBlock block) {
        Objects.requireNonNull(block, "block");
        long start = System.nanoTime();
        try {
            BufferedImage source = textRegion(decode(block), block);
            int pageSegMode = lineCount(block) <= 1
                    ? ITessAPI.TessPageSegMode.PSM_SINGLE_LINE
                    : ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK;
            List<OcrProfile> profiles = List.of(
                    profile("PROPERTY_BLOCK luma-bicubic-x3", pageSegMode,
                            PreprocessMode.BINARY_LUMA, Interpolation.BICUBIC, 3),
                    profile("PROPERTY_BLOCK luma-bicubic-x4", pageSegMode,
                            PreprocessMode.BINARY_LUMA, Interpolation.BICUBIC, 4),
                    profile("PROPERTY_BLOCK luma-nearest-x3", pageSegMode,
                            PreprocessMode.BINARY_LUMA, Interpolation.NEAREST, 3),
                    profile("PROPERTY_BLOCK luma-nearest-x4", pageSegMode,
                            PreprocessMode.BINARY_LUMA, Interpolation.NEAREST, 4),
                    profile("PROPERTY_BLOCK luma-bicubic-x3 psm-block",
                            ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK,
                            PreprocessMode.BINARY_LUMA, Interpolation.BICUBIC, 3),
                    profile("PROPERTY_BLOCK luma-nearest-x5", pageSegMode,
                            PreprocessMode.BINARY_LUMA, Interpolation.NEAREST, 5),
                    linewiseProfile("PROPERTY_BLOCK adaptive-thin-separator-mask linewise",
                            PreprocessMode.BINARY_LUMA_THIN_SEPARATOR_MASK, Interpolation.BICUBIC, 3),
                    profile("PROPERTY_BLOCK grayscale-bicubic-x4", pageSegMode,
                            PreprocessMode.INVERTED_GRAYSCALE, Interpolation.BICUBIC, 4)
            );
            List<OcrAttempt> attempts = profiles.stream()
                    .map(profile -> attempt(source, profile))
                    .toList();
            OcrAttempt bestAttempt = attempts.stream()
                    .max(Comparator.comparingDouble(attempt -> selectionScore(attempt, attempts)))
                    .orElseThrow();
            List<PropertyBlockOcrProfileResult> profileResults = attempts.stream()
                    .map(attempt -> new PropertyBlockOcrProfileResult(
                            attempt.profile().description(),
                            attempt.rawText(),
                            PropertyBlockNumericEvidenceAggregator.extractNumericTokens(attempt.rawText()),
                            selectionScore(attempt, attempts),
                            attempt.failure()))
                    .toList();
            PropertyBlockNumericEvidenceAggregator.Aggregation numericAggregation =
                    PropertyBlockNumericEvidenceAggregator.aggregate(profileResults);
            List<String> failures = new ArrayList<>();
            attempts.stream()
                    .filter(attempt -> !attempt.failure().isBlank())
                    .map(attempt -> attempt.profile().description() + ": " + attempt.failure())
                    .forEach(failures::add);
            if (normalizeRawText(bestAttempt.rawText()).isBlank()) {
                failures.add("OCR zwrocil pusty tekst dla PropertyBlock.");
            }
            failures.addAll(numericAggregation.diagnostics());
            return new PropertyBlockReadResult(
                    block.getSourcePart(),
                    block.getOrder(),
                    block.getAnchorType(),
                    bestAttempt.rawText(),
                    elapsedMillis(start),
                    bestAttempt.profile().description(),
                    failures,
                    numericAggregation.evidence(),
                    profileResults
            );
        } catch (RuntimeException | IOException exception) {
            return PropertyBlockReadResult.failed(
                    block,
                    elapsedMillis(start),
                    "Blad OCR PropertyBlock: " + safeMessage(exception)
            );
        }
    }

    Path getTessdataDirectory() {
        return tessdataDirectory;
    }

    private OcrAttempt attempt(BufferedImage source, OcrProfile profile) {
        try {
            if (profile.linewise()) {
                return attemptLinewise(source, profile);
            }
            BufferedImage prepared = preprocess(source, profile);
            OcrRecognition recognition = recognize(prepared, profile, profile.pageSegMode());
            return new OcrAttempt(profile, recognition.rawText(),
                    textScore(recognition.rawText(), recognition.lines())
                            + compactGlyphProfileBonus(source, profile), "");
        } catch (RuntimeException exception) {
            return new OcrAttempt(profile, "", -1_000_000.0d, safeMessage(exception));
        }
    }

    private OcrAttempt attemptLinewise(BufferedImage source, OcrProfile profile) {
        List<HorizontalBand> bands = horizontalTextBands(source);
        if (bands.size() <= 1) {
            return new OcrAttempt(profile, "", -10_000.0d, "");
        }
        List<Word> allLines = new ArrayList<>();
        List<String> rawLines = new ArrayList<>();
        boolean usedSeparatorMask = false;
        for (HorizontalBand band : bands) {
            BufferedImage line = crop(source, 0, band.start(), source.getWidth(), band.height());
            LineRecognition lineRecognition = profile.mode() == PreprocessMode.BINARY_LUMA_THIN_SEPARATOR_MASK
                    ? bestAdaptiveLineRecognition(line)
                    : new LineRecognition(recognize(preprocess(line, profile), profile,
                            ITessAPI.TessPageSegMode.PSM_SINGLE_LINE), false);
            OcrRecognition recognition = lineRecognition.recognition();
            if (!recognition.rawText().isBlank()) {
                rawLines.add(recognition.rawText());
                allLines.addAll(recognition.lines());
                usedSeparatorMask |= lineRecognition.usedSeparatorMask();
            }
        }
        String rawText = String.join("\n", rawLines);
        return new OcrAttempt(profile, rawText,
                textScore(rawText, allLines) + (usedSeparatorMask ? 200.0d : 0.0d), "");
    }

    private LineRecognition bestAdaptiveLineRecognition(BufferedImage line) {
        List<OcrProfile> lineProfiles = new ArrayList<>(List.of(
                profile("line-luma-bicubic-x3", ITessAPI.TessPageSegMode.PSM_SINGLE_LINE,
                        PreprocessMode.BINARY_LUMA, Interpolation.BICUBIC, 3),
                profile("line-luma-bicubic-x4", ITessAPI.TessPageSegMode.PSM_SINGLE_LINE,
                        PreprocessMode.BINARY_LUMA, Interpolation.BICUBIC, 4),
                profile("line-luma-nearest-x3", ITessAPI.TessPageSegMode.PSM_SINGLE_LINE,
                        PreprocessMode.BINARY_LUMA, Interpolation.NEAREST, 3),
                profile("line-luma-nearest-x4", ITessAPI.TessPageSegMode.PSM_SINGLE_LINE,
                        PreprocessMode.BINARY_LUMA, Interpolation.NEAREST, 4)
        ));
        if (hasThinSeparator(line)) {
            lineProfiles.add(profile("line-mask-bicubic-x3", ITessAPI.TessPageSegMode.PSM_SINGLE_LINE,
                    PreprocessMode.BINARY_LUMA_THIN_SEPARATOR_MASK, Interpolation.BICUBIC, 3));
            lineProfiles.add(profile("line-mask-bicubic-x4", ITessAPI.TessPageSegMode.PSM_SINGLE_LINE,
                    PreprocessMode.BINARY_LUMA_THIN_SEPARATOR_MASK, Interpolation.BICUBIC, 4));
        }
        return lineProfiles.stream()
                .map(candidate -> {
                    OcrRecognition recognition = recognize(preprocess(line, candidate), candidate,
                            ITessAPI.TessPageSegMode.PSM_SINGLE_LINE);
                    boolean masked = candidate.mode() == PreprocessMode.BINARY_LUMA_THIN_SEPARATOR_MASK;
                    ScoredLineRecognition scored = new ScoredLineRecognition(recognition, masked,
                            textScore(recognition.rawText(), recognition.lines())
                                    + polishDecimalPercentBonus(recognition.rawText())
                                    + (masked ? 100.0d : 0.0d));
                    return scored;
                })
                .max(Comparator.comparingDouble(ScoredLineRecognition::score))
                .map(candidate -> new LineRecognition(candidate.recognition(), candidate.usedSeparatorMask()))
                .orElseThrow();
    }

    private static double polishDecimalPercentBonus(String rawText) {
        Matcher matcher = POLISH_DECIMAL_PERCENT.matcher(normalizeRawText(rawText));
        int matches = 0;
        while (matcher.find()) {
            matches++;
        }
        return matches * 100.0d;
    }

    private OcrRecognition recognize(BufferedImage prepared, OcrProfile profile, int pageSegMode) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataDirectory.toString());
        tesseract.setLanguage(LANGUAGE);
        tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
        tesseract.setPageSegMode(pageSegMode);
        tesseract.setVariable("tessedit_char_whitelist", PROPERTY_BLOCK_WHITELIST);
        tesseract.setVariable("user_defined_dpi", "300");
        tesseract.setVariable("preserve_interword_spaces", "1");
        List<Word> lines = tesseract.getWords(prepared, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);
        String rawText = lines.stream()
                .sorted(Comparator.comparingInt((Word word) -> word.getBoundingBox().y)
                        .thenComparingInt(word -> word.getBoundingBox().x))
                .map(Word::getText)
                .map(TesseractPropertyBlockOcrReader::normalizeRawText)
                .filter(text -> !text.isBlank())
                .reduce("", (left, right) -> left.isBlank() ? right : left + "\n" + right);
        return new OcrRecognition(rawText, lines);
    }

    private static List<HorizontalBand> horizontalTextBands(BufferedImage source) {
        List<HorizontalBand> bands = new ArrayList<>();
        int start = -1;
        int lastActive = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            int foreground = foregroundInRow(source, y);
            boolean active = foreground >= 2 && foreground < source.getWidth() * 0.60d;
            if (active) {
                if (start < 0) {
                    start = y;
                }
                lastActive = y;
            } else if (start >= 0 && y - lastActive >= 3) {
                addHorizontalBand(bands, source.getHeight(), start, lastActive);
                start = -1;
                lastActive = -1;
            }
        }
        if (start >= 0) {
            addHorizontalBand(bands, source.getHeight(), start, lastActive);
        }
        return bands;
    }

    private static int foregroundInRow(BufferedImage source, int y) {
        int foreground = 0;
        int scanWidth = Math.max(0, source.getWidth() - 3);
        for (int x = 0; x < scanWidth; x++) {
            int rgb = source.getRGB(x, y);
            int red = (rgb >> 16) & 0xff;
            int green = (rgb >> 8) & 0xff;
            int blue = rgb & 0xff;
            int maximum = Math.max(red, Math.max(green, blue));
            int minimum = Math.min(red, Math.min(green, blue));
            if (luma(rgb) >= 112 || (maximum >= 105 && maximum - minimum >= 26)) {
                foreground++;
            }
        }
        return foreground;
    }

    private static void addHorizontalBand(List<HorizontalBand> bands,
                                          int imageHeight,
                                          int start,
                                          int end) {
        if (end - start + 1 < 4) {
            return;
        }
        bands.add(new HorizontalBand(Math.max(0, start - 2), Math.min(imageHeight - 1, end + 2)));
    }

    private static BufferedImage crop(BufferedImage source, int x, int y, int width, int height) {
        BufferedImage cropped = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = cropped.createGraphics();
        graphics.drawImage(source, 0, 0, width, height, x, y, x + width, y + height, null);
        graphics.dispose();
        return cropped;
    }

    private static Path resolveBundledTessdataDirectory() {
        Path directory = Path.of(System.getProperty("java.io.tmpdir"), "dstats-tessdata", "property-block-poc");
        Path polishData = directory.resolve("pol.traineddata");
        try {
            Files.createDirectories(directory);
            try (InputStream inputStream = TesseractPropertyBlockOcrReader.class.getResourceAsStream(POL_TRAINEDDATA_RESOURCE)) {
                if (inputStream == null) {
                    throw new IllegalStateException("Brak zasobu " + POL_TRAINEDDATA_RESOURCE + " dla Tesseract OCR.");
                }
                Files.copy(inputStream, polishData, StandardCopyOption.REPLACE_EXISTING);
            }
            return directory;
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udalo sie przygotowac lokalnego katalogu tessdata dla PropertyBlock OCR.", exception);
        }
    }

    private static BufferedImage decode(PropertyBlock block) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(block.getImageBytes()));
        if (image == null) {
            throw new IOException("Nie udalo sie zdekodowac obrazu PropertyBlock.");
        }
        return image;
    }

    private static BufferedImage textRegion(BufferedImage source, PropertyBlock block) {
        BufferedImage cleaned = source;
        if (block.getAnchorType() != PropertyBlockAnchorType.NONE && block.getAnchor().isPresent()) {
            PropertyBlockAnchor anchor = block.getAnchor().orElseThrow();
            if (anchor.getWidth() >= 7 || anchor.getHeight() >= 7) {
                cleaned = isSocket(block.getAnchorType())
                        ? maskSocketAnchor(source, block, anchor)
                        : maskVisualAnchor(source, anchor);
            }
        }
        int right = rightTextBoundary(cleaned);
        int top = cleaned.getHeight() >= 30
                ? Math.min(TOP_EDGE_TRIM, cleaned.getHeight() - 1)
                : 0;
        if (right <= 0 || top >= cleaned.getHeight()) {
            return cleaned;
        }
        BufferedImage cropped = new BufferedImage(right, cleaned.getHeight() - top, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = cropped.createGraphics();
        graphics.drawImage(cleaned, 0, 0, cropped.getWidth(), cropped.getHeight(),
                0, top, right, cleaned.getHeight(), null);
        graphics.dispose();
        return cropped;
    }

    private static int rightTextBoundary(BufferedImage source) {
        int fallback = source.getWidth() - RIGHT_EDGE_TRIM;
        if (source.getWidth() < 50) {
            return fallback;
        }
        int scanStart = Math.max(source.getWidth() - 10,
                (int) Math.floor(source.getWidth() * 0.85d));
        for (int x = scanStart; x < source.getWidth(); x++) {
            if (foregroundInColumn(source, x, source.getHeight())
                    >= Math.ceil(source.getHeight() * 0.70d)) {
                return Math.min(fallback, x);
            }
        }
        return fallback;
    }

    private static boolean isSocket(PropertyBlockAnchorType anchorType) {
        return anchorType == PropertyBlockAnchorType.EMPTY_SOCKET
                || anchorType == PropertyBlockAnchorType.FILLED_SOCKET;
    }

    private static BufferedImage maskSocketAnchor(BufferedImage source,
                                                  PropertyBlock block,
                                                  PropertyBlockAnchor anchor) {
        if (anchor.getWidth() >= anchor.getHeight() * 1.75d) {
            return maskVisualAnchor(source, anchor);
        }
        int localAnchorX = Math.max(0, anchor.getX() - block.getX());
        int visualIconWidth = Math.min(anchor.getWidth(),
                Math.max(1, (int) Math.round(anchor.getHeight() * 0.85d)));
        int gutterRight = Math.min(source.getWidth(), localAnchorX + visualIconWidth + 1);
        return maskLeftGutter(source, gutterRight);
    }

    private static BufferedImage maskVisualAnchor(BufferedImage source, PropertyBlockAnchor anchor) {
        int scanHeight = Math.min(source.getHeight(), Math.max(18, Math.min(28, anchor.getHeight() + 4)));
        List<ColumnGroup> groups = new ArrayList<>();
        int groupStart = -1;
        int lastActive = -1;
        int peakForeground = 0;
        for (int x = 0; x < source.getWidth(); x++) {
            int foreground = foregroundInColumn(source, x, scanHeight);
            if (foreground >= 2) {
                if (groupStart < 0) {
                    groupStart = x;
                    peakForeground = foreground;
                }
                lastActive = x;
                peakForeground = Math.max(peakForeground, foreground);
            } else if (groupStart >= 0 && x - lastActive >= 3) {
                groups.add(new ColumnGroup(groupStart, lastActive, peakForeground));
                groupStart = -1;
                lastActive = -1;
                peakForeground = 0;
            }
        }
        if (groupStart >= 0) {
            groups.add(new ColumnGroup(groupStart, lastActive, peakForeground));
        }
        List<ColumnGroup> contentGroups = groups.stream()
                .filter(group -> group.width() >= 2)
                .toList();
        ColumnGroup icon = contentGroups.stream()
                .filter(group -> !group.isVerticalEdge(scanHeight))
                .findFirst()
                .orElse(null);
        if (icon == null) {
            return source;
        }
        return maskLeftGutter(source, Math.min(source.getWidth(), icon.end() + 2));
    }

    private static BufferedImage maskLeftGutter(BufferedImage source, int gutterRight) {
        BufferedImage masked = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = masked.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.setColor(averageDarkBackground(source));
        graphics.fillRect(0, 0, gutterRight, source.getHeight());
        graphics.dispose();
        return masked;
    }

    private static int foregroundInColumn(BufferedImage source, int x, int scanHeight) {
        int foreground = 0;
        for (int y = 0; y < scanHeight; y++) {
            int rgb = source.getRGB(x, y);
            int red = (rgb >> 16) & 0xff;
            int green = (rgb >> 8) & 0xff;
            int blue = rgb & 0xff;
            int maximum = Math.max(red, Math.max(green, blue));
            int minimum = Math.min(red, Math.min(green, blue));
            if (luma(rgb) >= 112 || (maximum >= 105 && maximum - minimum >= 26)) {
                foreground++;
            }
        }
        return foreground;
    }

    private static Color averageDarkBackground(BufferedImage source) {
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        int samples = 0;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int rgb = source.getRGB(x, y);
                if (luma(rgb) < 96) {
                    red += (rgb >> 16) & 0xff;
                    green += (rgb >> 8) & 0xff;
                    blue += rgb & 0xff;
                    samples++;
                }
            }
        }
        if (samples == 0) {
            return Color.BLACK;
        }
        return new Color((int) (red / samples), (int) (green / samples), (int) (blue / samples));
    }

    private static int lineCount(PropertyBlock block) {
        String marker = "lineCount=";
        int start = block.getDiagnostics().indexOf(marker);
        if (start < 0) {
            return 2;
        }
        try {
            return Integer.parseInt(block.getDiagnostics().substring(start + marker.length()).trim());
        } catch (NumberFormatException exception) {
            return 2;
        }
    }

    private static BufferedImage preprocess(BufferedImage source, OcrProfile profile) {
        BufferedImage workingSource = profile.mode() == PreprocessMode.BINARY_LUMA_THIN_SEPARATOR_MASK
                ? maskThinSeparators(source)
                : source;
        BufferedImage scaled = new BufferedImage(workingSource.getWidth() * profile.scale(),
                workingSource.getHeight() * profile.scale(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, profile.interpolation().hint());
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(workingSource, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        graphics.dispose();
        return switch (profile.mode()) {
            case BINARY_LUMA -> binaryLuma(scaled);
            case BINARY_LUMA_THIN_SEPARATOR_MASK -> binaryLuma(scaled);
            case INVERTED_GRAYSCALE -> invertedGrayscale(scaled);
        };
    }

    private static BufferedImage maskThinSeparators(BufferedImage source) {
        BufferedImage masked = crop(source, 0, 0, source.getWidth(), source.getHeight());
        Color background = averageDarkBackground(source);
        for (HorizontalBand band : horizontalTextBands(source)) {
            List<GlyphGroup> groups = glyphGroups(source, band);
            for (int index = 1; index + 1 < groups.size(); index++) {
                GlyphGroup previous = groups.get(index - 1);
                GlyphGroup candidate = groups.get(index);
                GlyphGroup next = groups.get(index + 1);
                if (isLineEndGlyph(groups, index + 1)
                        && isThinSeparator(previous, candidate, next, band)) {
                    int shift = candidate.width() + 1;
                    Graphics2D graphics = masked.createGraphics();
                    graphics.setColor(background);
                    graphics.fillRect(candidate.start(), band.start(), candidate.width(), band.height());
                    graphics.dispose();
                    BufferedImage snapshot = crop(masked, 0, 0, masked.getWidth(), masked.getHeight());
                    graphics = masked.createGraphics();
                    graphics.drawImage(snapshot,
                            next.start() - shift, band.start(), masked.getWidth() - shift, band.end() + 1,
                            next.start(), band.start(), masked.getWidth(), band.end() + 1, null);
                    graphics.setColor(background);
                    graphics.fillRect(masked.getWidth() - shift, band.start(), shift, band.height());
                    graphics.dispose();
                }
            }
        }
        return masked;
    }

    private static boolean isThinSeparator(GlyphGroup previous,
                                           GlyphGroup candidate,
                                           GlyphGroup next,
                                           HorizontalBand band) {
        boolean thinTallGlyph = candidate.width() <= 3
                && candidate.height() >= Math.max(7, band.height() / 2);
        boolean flankedByWideGlyphs = previous.width() >= 5
                && next.width() >= 8
                && candidate.start() - previous.end() <= 4
                && next.start() - candidate.end() <= 4;
        return thinTallGlyph && flankedByWideGlyphs;
    }

    private static boolean isLineEndGlyph(List<GlyphGroup> groups, int index) {
        return index + 1 >= groups.size()
                || groups.get(index + 1).start() - groups.get(index).end() > 12;
    }

    private static List<GlyphGroup> glyphGroups(BufferedImage source, HorizontalBand band) {
        List<GlyphGroup> groups = new ArrayList<>();
        int start = -1;
        int end = -1;
        int top = Integer.MAX_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (int x = 0; x < source.getWidth(); x++) {
            int columnTop = Integer.MAX_VALUE;
            int columnBottom = Integer.MIN_VALUE;
            for (int y = band.start(); y <= band.end(); y++) {
                if (isForeground(source.getRGB(x, y))) {
                    columnTop = Math.min(columnTop, y);
                    columnBottom = Math.max(columnBottom, y);
                }
            }
            if (columnBottom >= columnTop) {
                if (start < 0) {
                    start = x;
                }
                end = x;
                top = Math.min(top, columnTop);
                bottom = Math.max(bottom, columnBottom);
            } else if (start >= 0) {
                groups.add(new GlyphGroup(start, end, top, bottom));
                start = -1;
                top = Integer.MAX_VALUE;
                bottom = Integer.MIN_VALUE;
            }
        }
        if (start >= 0) {
            groups.add(new GlyphGroup(start, end, top, bottom));
        }
        return groups;
    }

    private static boolean isForeground(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int maximum = Math.max(red, Math.max(green, blue));
        int minimum = Math.min(red, Math.min(green, blue));
        return luma(rgb) >= 112 || (maximum >= 105 && maximum - minimum >= 26);
    }

    private static BufferedImage binaryLuma(BufferedImage scaled) {
        return binaryLuma(scaled, 96);
    }

    private static BufferedImage binaryLuma(BufferedImage scaled, int minimumThreshold) {
        int[] lumas = new int[scaled.getWidth() * scaled.getHeight()];
        int cursor = 0;
        for (int y = 0; y < scaled.getHeight(); y++) {
            for (int x = 0; x < scaled.getWidth(); x++) {
                lumas[cursor++] = luma(scaled.getRGB(x, y));
            }
        }
        int threshold = Math.max(minimumThreshold, otsuThreshold(lumas));
        return binary(scaled, (rgb, gray) -> gray >= threshold);
    }

    private static BufferedImage binary(BufferedImage scaled, ForegroundPredicate foreground) {
        BufferedImage binary = new BufferedImage(scaled.getWidth() + BORDER * 2,
                scaled.getHeight() + BORDER * 2, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D graphics = binary.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, binary.getWidth(), binary.getHeight());
        graphics.dispose();
        for (int y = 0; y < scaled.getHeight(); y++) {
            for (int x = 0; x < scaled.getWidth(); x++) {
                int rgb = scaled.getRGB(x, y);
                int gray = luma(rgb);
                binary.setRGB(x + BORDER, y + BORDER,
                        foreground.test(rgb, gray) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return binary;
    }

    private static BufferedImage invertedGrayscale(BufferedImage scaled) {
        BufferedImage prepared = new BufferedImage(scaled.getWidth() + BORDER * 2,
                scaled.getHeight() + BORDER * 2, BufferedImage.TYPE_BYTE_GRAY);
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

    private static double textScore(String rawText, List<Word> lines) {
        String normalized = normalizeRawText(rawText);
        if (normalized.isBlank()) {
            return -10_000.0d;
        }
        int letters = 0;
        int digits = 0;
        int punctuation = 0;
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if (Character.isLetter(character)) {
                letters++;
            } else if (Character.isDigit(character)) {
                digits++;
            } else if (!Character.isWhitespace(character)) {
                punctuation++;
            }
        }
        double confidence = lines.stream()
                .filter(word -> word.getText() != null && !word.getText().isBlank())
                .mapToDouble(Word::getConfidence)
                .average()
                .orElse(0.0d);
        long lineCount = rawText == null ? 0L : rawText.lines().map(String::trim).filter(line -> !line.isBlank()).count();
        long plusSigns = normalized.chars().filter(character -> character == '+').count();
        return confidence * 2.0d + letters * 3.0d + digits * 2.0d
                + punctuation + plusSigns * 8.0d + Math.min(lineCount, 12) * 3.0d;
    }

    private static double compactGlyphProfileBonus(BufferedImage source, OcrProfile profile) {
        boolean compactSingleLine = source.getWidth() <= 120 && source.getHeight() <= 20;
        return compactSingleLine
                && profile.scale() == 5
                && profile.interpolation() == Interpolation.NEAREST
                ? 120.0d
                : 0.0d;
    }

    private static double selectionScore(OcrAttempt candidate,
                                         List<OcrAttempt> attempts) {
        return candidate.score() + extraDigitOutlierPenalty(candidate, attempts);
    }

    private static double extraDigitOutlierPenalty(OcrAttempt candidate,
                                                   List<OcrAttempt> attempts) {
        String candidateDigits = digitFingerprint(candidate.rawText());
        String consensusDigits = mostFrequentDigitFingerprint(attempts);
        if (consensusDigits.isBlank()
                || candidateDigits.length() != consensusDigits.length() + 1
                || !isSubsequence(consensusDigits, candidateDigits)) {
            return 0.0d;
        }
        long consensusCount = attempts.stream()
                .map(attempt -> digitFingerprint(attempt.rawText()))
                .filter(consensusDigits::equals)
                .count();
        return consensusCount >= 3 ? -250.0d : 0.0d;
    }

    private static String mostFrequentDigitFingerprint(List<OcrAttempt> attempts) {
        String mostFrequent = "";
        long highestCount = 0L;
        for (OcrAttempt attempt : attempts) {
            String fingerprint = digitFingerprint(attempt.rawText());
            if (fingerprint.isBlank()) {
                continue;
            }
            long count = attempts.stream()
                    .map(candidate -> digitFingerprint(candidate.rawText()))
                    .filter(fingerprint::equals)
                    .count();
            if (count > highestCount) {
                mostFrequent = fingerprint;
                highestCount = count;
            }
        }
        return mostFrequent;
    }

    private static String digitFingerprint(String rawText) {
        return normalizeRawText(rawText).replaceAll("\\D", "");
    }

    private static boolean isSubsequence(String shorter, String longer) {
        int cursor = 0;
        for (int index = 0; index < longer.length() && cursor < shorter.length(); index++) {
            if (shorter.charAt(cursor) == longer.charAt(index)) {
                cursor++;
            }
        }
        return cursor == shorter.length();
    }

    private static boolean hasThinSeparator(BufferedImage source) {
        for (HorizontalBand band : horizontalTextBands(source)) {
            List<GlyphGroup> groups = glyphGroups(source, band);
            for (int index = 1; index + 1 < groups.size(); index++) {
                if (isLineEndGlyph(groups, index + 1)
                        && isThinSeparator(groups.get(index - 1), groups.get(index),
                        groups.get(index + 1), band)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String normalizeRawText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }
        return rawText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .reduce("", (left, right) -> left.isBlank() ? right : left + "\n" + right)
                .trim();
    }

    private static OcrProfile profile(String description,
                                      int pageSegMode,
                                      PreprocessMode mode,
                                      Interpolation interpolation,
                                      int scale) {
        return new OcrProfile(description, pageSegMode, mode, interpolation, scale);
    }

    private static OcrProfile linewiseProfile(String description,
                                              PreprocessMode mode,
                                              Interpolation interpolation,
                                              int scale) {
        return new OcrProfile(description, ITessAPI.TessPageSegMode.PSM_SINGLE_LINE,
                mode, interpolation, scale, true);
    }

    private static long elapsedMillis(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private enum PreprocessMode {
        BINARY_LUMA,
        BINARY_LUMA_THIN_SEPARATOR_MASK,
        INVERTED_GRAYSCALE
    }

    private enum Interpolation {
        BICUBIC(RenderingHints.VALUE_INTERPOLATION_BICUBIC),
        NEAREST(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        private final Object hint;

        Interpolation(Object hint) {
            this.hint = hint;
        }

        private Object hint() {
            return hint;
        }
    }

    @FunctionalInterface
    private interface ForegroundPredicate {
        boolean test(int rgb, int gray);
    }

    private record OcrProfile(String description,
                              int pageSegMode,
                              PreprocessMode mode,
                              Interpolation interpolation,
                              int scale,
                              boolean linewise) {
        private OcrProfile(String description,
                           int pageSegMode,
                           PreprocessMode mode,
                           Interpolation interpolation,
                           int scale) {
            this(description, pageSegMode, mode, interpolation, scale, false);
        }
    }

    private record OcrRecognition(String rawText, List<Word> lines) {
    }

    private record LineRecognition(OcrRecognition recognition, boolean usedSeparatorMask) {
    }

    private record ScoredLineRecognition(OcrRecognition recognition,
                                         boolean usedSeparatorMask,
                                         double score) {
    }

    private record OcrAttempt(OcrProfile profile, String rawText, double score, String failure) {
    }

    private record ColumnGroup(int start, int end, int peakForeground) {
        private int width() {
            return end - start + 1;
        }

        private boolean isVerticalEdge(int scanHeight) {
            return start <= 8 && peakForeground >= Math.ceil(scanHeight * 0.70d);
        }
    }

    private record HorizontalBand(int start, int end) {
        private int height() {
            return end - start + 1;
        }
    }

    private record GlyphGroup(int start, int end, int top, int bottom) {
        private int width() {
            return end - start + 1;
        }

        private int height() {
            return bottom - top + 1;
        }
    }
}
