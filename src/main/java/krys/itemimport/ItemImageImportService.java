package krys.itemimport;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

/** Wstępny analizator obrazu pojedynczego itemu z jawnie ręcznym potwierdzeniem użytkownika. */
public final class ItemImageImportService {
    private static final String MANUAL_CONFIRMATION_NOTE =
            "Aktualny foundation nie wykonuje jeszcze pełnego OCR tekstu z itemu. Obraz jest walidowany technicznie, a wszystkie pola wymagają ręcznego potwierdzenia użytkownika.";

    public ItemImageImportCandidateParseResult analyze(ItemImageImportRequest request) {
        BufferedImage image = readImage(request.getImageBytes());
        ItemImageMetadata metadata = new ItemImageMetadata(
                request.getOriginalFilename(),
                request.getContentType(),
                resolveFormat(request.getImageBytes()),
                image.getWidth(),
                image.getHeight()
        );
        return new ItemImageImportCandidateParseResult(
                metadata,
                ItemImportFieldCandidate.unknown("Brak pewnego odczytu slotu w foundation importu obrazu."),
                ItemImportFieldCandidate.unknown("Brak pewnego odczytu weapon damage z obrazu."),
                ItemImportFieldCandidate.unknown("Brak pewnego odczytu strength z obrazu."),
                ItemImportFieldCandidate.unknown("Brak pewnego odczytu intelligence z obrazu."),
                ItemImportFieldCandidate.unknown("Brak pewnego odczytu thorns z obrazu."),
                ItemImportFieldCandidate.unknown("Brak pewnego odczytu block chance z obrazu."),
                ItemImportFieldCandidate.unknown("Brak pewnego odczytu retribution chance z obrazu."),
                MANUAL_CONFIRMATION_NOTE
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
