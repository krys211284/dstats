package krys.itemimport;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Wstępny analizator obrazu pojedynczego itemu z jawnie ręcznym potwierdzeniem użytkownika. */
public final class ItemImageImportService {
    public static final int MAX_SCREENSHOT_COUNT = 5;

    private final ItemImageOcrPreprocessor ocrPreprocessor;
    private final ItemImageOcrTextReader ocrTextReader;
    private final ItemImageImportTextParser textParser;
    private final ItemImageImportCandidateMerger candidateMerger;
    private final ItemScreenshotTextMerger textMerger;
    private final GreaterAffixHeaderStarDetector headerStarDetector;

    public ItemImageImportService() {
        this(
                new ItemImageOcrPreprocessor(),
                new WindowsItemOcrTextReader(),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger(),
                new ItemScreenshotTextMerger(),
                new GreaterAffixHeaderStarDetector()
        );
    }

    ItemImageImportService(ItemImageOcrPreprocessor ocrPreprocessor,
                           ItemImageOcrTextReader ocrTextReader,
                           ItemImageImportTextParser textParser,
                           ItemImageImportCandidateMerger candidateMerger) {
        this(ocrPreprocessor, ocrTextReader, textParser, candidateMerger, new ItemScreenshotTextMerger());
    }

    ItemImageImportService(ItemImageOcrPreprocessor ocrPreprocessor,
                           ItemImageOcrTextReader ocrTextReader,
                           ItemImageImportTextParser textParser,
                           ItemImageImportCandidateMerger candidateMerger,
                           ItemScreenshotTextMerger textMerger) {
        this(ocrPreprocessor, ocrTextReader, textParser, candidateMerger, textMerger, new GreaterAffixHeaderStarDetector());
    }

    ItemImageImportService(ItemImageOcrPreprocessor ocrPreprocessor,
                           ItemImageOcrTextReader ocrTextReader,
                           ItemImageImportTextParser textParser,
                           ItemImageImportCandidateMerger candidateMerger,
                           ItemScreenshotTextMerger textMerger,
                           GreaterAffixHeaderStarDetector headerStarDetector) {
        this.ocrPreprocessor = ocrPreprocessor;
        this.ocrTextReader = ocrTextReader;
        this.textParser = textParser;
        this.candidateMerger = candidateMerger;
        this.textMerger = textMerger;
        this.headerStarDetector = headerStarDetector;
    }

    public ItemImageImportCandidateParseResult analyze(ItemImageImportRequest request) {
        try (ItemImportDebugTrace.Scope ignored = ItemImportDebugTrace.startImport()) {
            BufferedImage image = readImage(request.getImageBytes());
            ItemImageMetadata metadata = new ItemImageMetadata(
                    request.getOriginalFilename(),
                    request.getContentType(),
                    resolveFormat(request.getImageBytes()),
                    image.getWidth(),
                    image.getHeight()
            );
            ItemImportDebugTrace.bindMetadata(metadata);
            logImportRequest(List.of(request), List.of(image), "SINGLE");
            var variants = ocrPreprocessor.prepareVariants(image);
            var ocrTexts = ocrTextReader.readTextVariants(variants);
            logOcrRawVariants(0, ocrTexts);
            GreaterAffixHeaderEvidence headerEvidence = headerStarDetector.detect(ocrTexts);
            List<String> variantTexts = ocrTexts.stream()
                    .map(ItemImageOcrTextVariant::getText)
                    .toList();
            logMergerInput("SCREEN_MERGER_INPUT", 0, variantTexts);
            ItemScreenshotMergedText mergedScreenText = textMerger.mergeTextVariantsTyped(ocrTexts);
            logMergerOutput("SCREEN_MERGER_OUTPUT", "screen=0 scope=single-typed-merge", mergedScreenText.asPlainText());

            ItemImageImportCandidateParseResult typedParse;
            try (ItemImportDebugTrace.Scope variantScope = ItemImportDebugTrace.withOcrVariant(0, -1, "SINGLE_TYPED_MERGED")) {
                typedParse = textParser.parse(metadata, mergedScreenText);
            }

            List<ItemImageImportCandidateParseResult> parsedVariants = new ArrayList<>();
            if (ocrTexts.isEmpty()) {
                try (ItemImportDebugTrace.Scope variantScope = ItemImportDebugTrace.withOcrVariant(0, 0, "EMPTY")) {
                    parsedVariants.add(textParser.parse(metadata, ""));
                }
            } else {
                for (int index = 0; index < ocrTexts.size(); index++) {
                    ItemImageOcrTextVariant ocrText = ocrTexts.get(index);
                    try (ItemImportDebugTrace.Scope variantScope = ItemImportDebugTrace.withOcrVariant(0, index, ocrText.getVariantId())) {
                        parsedVariants.add(textParser.parse(metadata, ocrText.getText()));
                    }
                }
            }
            return candidateMerger.merge(metadata, variants.size(), parsedVariants, typedParse, headerEvidence);
        }
    }

    public ItemImageImportCandidateParseResult analyze(List<ItemImageImportRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Wgraj screenshot pojedynczego itemu.");
        }
        if (requests.size() > MAX_SCREENSHOT_COUNT) {
            throw new IllegalArgumentException("Można przesłać maksymalnie 5 screenów jednego itemu.");
        }
        if (requests.size() == 1) {
            return analyze(requests.getFirst());
        }

        try (ItemImportDebugTrace.Scope ignored = ItemImportDebugTrace.startImport()) {
            List<ItemScreenshotMergedText> ocrTexts = new ArrayList<>();
            List<ItemImageOcrTextVariant> rawTextVariants = new ArrayList<>();
            List<BufferedImage> images = new ArrayList<>();
            int analyzedVariantCount = 0;
            int totalHeight = 0;
            int maxWidth = 0;
            StringBuilder fileNames = new StringBuilder();
            String contentType = requests.getFirst().getContentType();
            for (int requestIndex = 0; requestIndex < requests.size(); requestIndex++) {
                ItemImageImportRequest request = requests.get(requestIndex);
                BufferedImage image = readImage(request.getImageBytes());
                images.add(image);
                totalHeight += image.getHeight();
                maxWidth = Math.max(maxWidth, image.getWidth());
                if (!fileNames.isEmpty()) {
                    fileNames.append(", ");
                }
                fileNames.append(request.getOriginalFilename());

                var variants = ocrPreprocessor.prepareVariants(image);
                analyzedVariantCount += variants.size();
                var textVariants = ocrTextReader.readTextVariants(variants);
                rawTextVariants.addAll(textVariants);
                logOcrRawVariants(requestIndex, textVariants);
                List<String> variantTexts = textVariants.stream()
                        .map(ItemImageOcrTextVariant::getText)
                        .toList();
                logMergerInput("SCREEN_MERGER_INPUT", requestIndex, variantTexts);
                ItemScreenshotMergedText mergedScreenText = textMerger.mergeTextVariantsTyped(textVariants);
                logMergerOutput("SCREEN_MERGER_OUTPUT", "screen=" + requestIndex + " scope=per-screen", mergedScreenText.asPlainText());
                ocrTexts.add(mergedScreenText);
            }
            logImportRequest(requests, images, "MULTI");

            ItemImageMetadata metadata = new ItemImageMetadata(
                    fileNames.toString(),
                    contentType,
                    "MULTI",
                    maxWidth,
                    totalHeight
            );
            ItemImportDebugTrace.bindMetadata(metadata);
            GreaterAffixHeaderEvidence headerEvidence = headerStarDetector.detect(rawTextVariants);
            logMergerInput("SCREEN_MERGER_INPUT", -1, ocrTexts.stream()
                    .map(ItemScreenshotMergedText::asPlainText)
                    .toList());
            ItemScreenshotMergedText mergedText = textMerger.mergeMergedTexts(ocrTexts);
            logMergerOutput("SCREEN_MERGER_OUTPUT", "scope=multi-final", mergedText.asPlainText());
            ItemImageImportCandidateParseResult parsed;
            try (ItemImportDebugTrace.Scope variantScope = ItemImportDebugTrace.withOcrVariant(-1, -1, "MULTI_MERGED")) {
                parsed = textParser.parse(metadata, mergedText);
            }
            return new ItemImageImportCandidateParseResult(
                    metadata,
                    parsed.getFullItemRead(),
                    parsed.getSlotCandidate(),
                    parsed.getWeaponDamageCandidate(),
                    parsed.getStrengthCandidate(),
                    parsed.getIntelligenceCandidate(),
                    parsed.getThornsCandidate(),
                    parsed.getBlockChanceCandidate(),
                    parsed.getRetributionChanceCandidate(),
                    headerEvidence,
                    "Import wieloscreenowy: " + requests.size() + " obrazy scalone jako jeden item. "
                            + "OCR analizował " + analyzedVariantCount + " wariantów obrazu."
            );
        }
    }

    private static BufferedImage readImage(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new IllegalArgumentException("Wgrany plik nie jest obsługiwanym obrazem itemu.");
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Nie udało się odczytać obrazu itemu.", exception);
        }
    }

    private static String resolveFormat(byte[] imageBytes) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (readers.hasNext()) {
                return readers.next().getFormatName().toUpperCase();
            }
            return "UNKNOWN";
        } catch (IOException exception) {
            return "UNKNOWN";
        }
    }

    private static String selectBestTextVariant(List<ItemImageOcrTextVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return "";
        }
        ItemImageOcrTextVariant best = variants.getFirst();
        int bestScore = textVariantScore(best.getText());
        for (ItemImageOcrTextVariant variant : variants) {
            int score = textVariantScore(variant.getText());
            if (score > bestScore) {
                best = variant;
                bestScore = score;
            }
        }
        return best.getText();
    }

    private static int textVariantScore(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int nonBlankLines = 0;
        for (String line : text.split("\\R")) {
            if (!line.trim().isBlank()) {
                nonBlankLines++;
            }
        }
        return nonBlankLines * 1000 + text.length();
    }

    private static int nonBlankLineCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String line : text.split("\\R")) {
            if (!line.trim().isBlank()) {
                count++;
            }
        }
        return count;
    }

    private static void logImportRequest(List<ItemImageImportRequest> requests, List<BufferedImage> images, String mode) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        ItemImportDebugTrace.log("IMPORT_REQUEST", () -> "files=" + requests.size()
                + " mode=" + mode
                + " timestamp=" + java.time.LocalDateTime.now()
                + " debugActive=true");
        for (int index = 0; index < requests.size(); index++) {
            ItemImageImportRequest request = requests.get(index);
            BufferedImage image = index < images.size() ? images.get(index) : null;
            int fileIndex = index;
            ItemImportDebugTrace.log("FILE", () -> "index=" + fileIndex
                    + " name=" + ItemImportDebugTrace.quote(ItemImportDebugTrace.safeFileName(request.getOriginalFilename()))
                    + " contentType=" + ItemImportDebugTrace.quote(request.getContentType())
                    + " width=" + (image == null ? "null" : image.getWidth())
                    + " height=" + (image == null ? "null" : image.getHeight()));
        }
    }

    private static void logOcrRawVariants(int screenIndex, List<ItemImageOcrTextVariant> variants) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        if (variants == null || variants.isEmpty()) {
            ItemImportDebugTrace.log("OCR_RAW_VARIANTS", () -> "screen=" + screenIndex + " variants=0");
            return;
        }
        for (int index = 0; index < variants.size(); index++) {
            ItemImageOcrTextVariant variant = variants.get(index);
            int variantIndex = index;
            ItemImportDebugTrace.log("OCR_RAW_VARIANTS", () -> "screen=" + screenIndex
                    + " variant=" + variantIndex
                    + " variantId=" + ItemImportDebugTrace.quote(variant.getVariantId())
                    + " raw=" + ItemImportDebugTrace.compactText(variant.getText()));
        }
    }

    private static void logMergerInput(String section, int screenIndex, List<String> variantTexts) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        List<String> texts = variantTexts == null ? List.of() : variantTexts;
        for (int variantIndex = 0; variantIndex < texts.size(); variantIndex++) {
            String text = texts.get(variantIndex);
            String[] lines = text == null ? new String[0] : text.split("\\R");
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String line = lines[lineIndex] == null ? "" : lines[lineIndex].trim();
                if (line.isBlank()) {
                    continue;
                }
                int finalVariantIndex = variantIndex;
                int finalLineIndex = lineIndex;
                ItemImportDebugTrace.log(section, () -> "screen=" + screenIndex
                        + " variant=" + finalVariantIndex
                        + " line=" + finalLineIndex
                        + " hasNumeric=" + ItemImportDebugTrace.hasNumericOrBracketTokens(line)
                        + " tokens=" + ItemImportDebugTrace.numericTokens(line)
                        + " source=" + ItemImportDebugTrace.compactText(line));
            }
        }
    }

    private static void logMergerOutput(String section, String scope, String mergedText) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        ItemImportDebugTrace.log(section, () -> scope + " merged=" + ItemImportDebugTrace.compactText(mergedText));
        String[] lines = mergedText == null ? new String[0] : mergedText.split("\\R");
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (line == null || line.isBlank()) {
                continue;
            }
            int finalIndex = index;
            ItemImportDebugTrace.log(section, () -> scope
                    + " line=" + finalIndex
                    + " hasNumeric=" + ItemImportDebugTrace.hasNumericOrBracketTokens(line)
                    + " tokens=" + ItemImportDebugTrace.numericTokens(line)
                    + " text=" + ItemImportDebugTrace.compactText(line));
        }
    }
}
