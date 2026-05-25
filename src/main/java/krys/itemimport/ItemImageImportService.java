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

    public ItemImageImportService() {
        this(
                new ItemImageOcrPreprocessor(),
                new WindowsItemOcrTextReader(),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger(),
                new ItemScreenshotTextMerger()
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
        this.ocrPreprocessor = ocrPreprocessor;
        this.ocrTextReader = ocrTextReader;
        this.textParser = textParser;
        this.candidateMerger = candidateMerger;
        this.textMerger = textMerger;
    }

    public ItemImageImportCandidateParseResult analyze(ItemImageImportRequest request) {
        BufferedImage image = readImage(request.getImageBytes());
        ItemImageMetadata metadata = new ItemImageMetadata(
                request.getOriginalFilename(),
                request.getContentType(),
                resolveFormat(request.getImageBytes()),
                image.getWidth(),
                image.getHeight()
        );
        var variants = ocrPreprocessor.prepareVariants(image);
        var ocrTexts = ocrTextReader.readTextVariants(variants);
        if (ocrTexts.isEmpty()) {
            return candidateMerger.merge(metadata, variants.size(), List.of(textParser.parse(metadata, "")));
        }

        return candidateMerger.merge(
                metadata,
                variants.size(),
                ocrTexts.stream()
                        .map(ocrText -> textParser.parse(metadata, ocrText.getText()))
                        .toList()
        );
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

        List<String> ocrTexts = new ArrayList<>();
        int analyzedVariantCount = 0;
        int totalHeight = 0;
        int maxWidth = 0;
        StringBuilder fileNames = new StringBuilder();
        String contentType = requests.getFirst().getContentType();
        for (ItemImageImportRequest request : requests) {
            BufferedImage image = readImage(request.getImageBytes());
            totalHeight += image.getHeight();
            maxWidth = Math.max(maxWidth, image.getWidth());
            if (!fileNames.isEmpty()) {
                fileNames.append(", ");
            }
            fileNames.append(request.getOriginalFilename());

            var variants = ocrPreprocessor.prepareVariants(image);
            analyzedVariantCount += variants.size();
            var textVariants = ocrTextReader.readTextVariants(variants);
            List<String> variantTexts = textVariants.stream()
                    .map(ItemImageOcrTextVariant::getText)
                    .toList();
            ocrTexts.add(textMerger.merge(variantTexts));
        }

        ItemImageMetadata metadata = new ItemImageMetadata(
                fileNames.toString(),
                contentType,
                "MULTI",
                maxWidth,
                totalHeight
        );
        String mergedText = textMerger.merge(ocrTexts);
        ItemImageImportCandidateParseResult parsed = textParser.parse(metadata, mergedText);
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
                "Import wieloscreenowy: " + requests.size() + " obrazy scalone jako jeden item. "
                        + "OCR analizował " + analyzedVariantCount + " wariantów obrazu."
        );
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
}
