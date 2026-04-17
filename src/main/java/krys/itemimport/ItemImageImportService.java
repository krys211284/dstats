package krys.itemimport;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/** Wstępny analizator obrazu pojedynczego itemu z jawnie ręcznym potwierdzeniem użytkownika. */
public final class ItemImageImportService {
    private final ItemImageOcrPreprocessor ocrPreprocessor;
    private final ItemImageOcrTextReader ocrTextReader;
    private final ItemImageImportTextParser textParser;
    private final ItemImageImportCandidateMerger candidateMerger;

    public ItemImageImportService() {
        this(
                new ItemImageOcrPreprocessor(),
                new WindowsItemOcrTextReader(),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );
    }

    ItemImageImportService(ItemImageOcrPreprocessor ocrPreprocessor,
                           ItemImageOcrTextReader ocrTextReader,
                           ItemImageImportTextParser textParser,
                           ItemImageImportCandidateMerger candidateMerger) {
        this.ocrPreprocessor = ocrPreprocessor;
        this.ocrTextReader = ocrTextReader;
        this.textParser = textParser;
        this.candidateMerger = candidateMerger;
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
}
