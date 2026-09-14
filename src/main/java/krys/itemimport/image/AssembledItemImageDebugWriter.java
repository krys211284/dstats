package krys.itemimport.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Zapisuje lokalny artefakt diagnostyczny finalnego obrazu bez wplywu na pipeline. */
public final class AssembledItemImageDebugWriter {
    public static final String DEBUG_DIRECTORY_PROPERTY = "dstats.itemImport.openCvDebugDir";
    public static final Path DEBUG_DIRECTORY = Path.of("target", "item-import-debug");
    public static final Path STITCHED_ITEM_IMAGE_PATH = DEBUG_DIRECTORY.resolve("stitched-item.png");
    public static final Path LEGACY_ASSEMBLED_ITEM_IMAGE_PATH = DEBUG_DIRECTORY.resolve("assembled-item.png");
    public static final Path DEFAULT_DEBUG_IMAGE_PATH = STITCHED_ITEM_IMAGE_PATH;
    private static final Logger LOGGER = Logger.getLogger(AssembledItemImageDebugWriter.class.getName());

    public Path writeDefault(AssembledItemImage image) {
        return write(image, stitchedItemImagePath());
    }

    public boolean tryWriteDefault(AssembledItemImage image) {
        return tryWrite(image, stitchedItemImagePath());
    }

    public boolean tryWrite(AssembledItemImage image, Path outputPath) {
        try {
            write(image, outputPath);
            return true;
        } catch (RuntimeException exception) {
            logDebugArtifactWarning(outputPath == null ? stitchedItemImagePath() : outputPath, exception);
            return false;
        }
    }

    public Path write(AssembledItemImage image, Path outputPath) {
        if (image == null) {
            throw new IllegalArgumentException("Brak zlozonego obrazu do zapisu diagnostycznego.");
        }
        Path safePath = outputPath == null ? DEFAULT_DEBUG_IMAGE_PATH : outputPath;
        try {
            Path parent = safePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(safePath, image.getImageBytes());
            return safePath;
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udalo sie zapisac diagnostycznego obrazu itemu.", exception);
        }
    }

    public static Path debugDirectory() {
        String configuredDirectory = System.getProperty(DEBUG_DIRECTORY_PROPERTY);
        return configuredDirectory == null || configuredDirectory.isBlank()
                ? DEBUG_DIRECTORY
                : Path.of(configuredDirectory);
    }

    public static Path stitchedItemImagePath() {
        return debugDirectory().resolve("stitched-item.png");
    }

    public static Path debugPath(String filename) {
        String safeFilename = filename == null || filename.isBlank() ? "opencv-debug.png" : filename;
        return debugDirectory().resolve(safeFilename);
    }

    public static void logDebugArtifactWarning(Path outputPath, Exception exception) {
        String reason = exception == null ? "" : exception.getMessage();
        String path = outputPath == null ? "" : outputPath.toString();
        LOGGER.log(
                Level.WARNING,
                "OPENCV_DEBUG_ARTIFACT_WARNING path=\"{0}\" reason=\"{1}\"",
                new Object[]{path, reason}
        );
        if (exception != null) {
            LOGGER.log(Level.FINE, "Szczegoly bledu zapisu artefaktu diagnostycznego OpenCV.", exception);
        }
    }
}
