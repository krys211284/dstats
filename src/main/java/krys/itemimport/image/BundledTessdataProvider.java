package krys.itemimport.image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Wspólny, leniwy i bezpieczny wątkowo katalog bundlowanego polskiego modelu OCR. */
final class BundledTessdataProvider {
    private static final String RESOURCE = "/tessdata/pol.traineddata";
    private static final ConcurrentMap<Path, Path> PREPARED = new ConcurrentHashMap<>();

    private BundledTessdataProvider() {
    }

    static Path directory() {
        return prepare(Path.of(System.getProperty("java.io.tmpdir"), "dstats-tessdata", "bundled-pol"));
    }

    static Path prepare(Path targetDirectory) {
        Path directory = targetDirectory.toAbsolutePath().normalize();
        return PREPARED.compute(directory, (path, prepared) -> {
            Path model = path.resolve("pol.traineddata");
            try {
                // Po przygotowaniu nie czytamy ani nie kopiujemy ponownie całego modelu.
                if (prepared != null && Files.isRegularFile(model) && Files.size(model) == Model.DATA.length) {
                    return prepared;
                }
                Files.createDirectories(path);
                if (Files.isRegularFile(model) && Arrays.equals(sha256(Files.readAllBytes(model)), Model.SHA256)) {
                    return path;
                }
                Path temporary = Files.createTempFile(path, "pol-", ".traineddata.tmp");
                try {
                    Files.write(temporary, Model.DATA);
                    // Inny reader lub JVM może zobaczyć wyłącznie kompletny model.
                    try {
                        Files.move(temporary, model, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    } catch (AtomicMoveNotSupportedException exception) {
                        Files.move(temporary, model, StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    Files.deleteIfExists(temporary);
                }
                return path;
            } catch (IOException exception) {
                throw new IllegalStateException("Nie udało się przygotować bundlowanego tessdata: " + path, exception);
            }
        });
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class Model {
        private static final byte[] DATA = read();
        private static final byte[] SHA256 = sha256(DATA);

        private static byte[] read() {
            try (InputStream input = BundledTessdataProvider.class.getResourceAsStream(RESOURCE)) {
                if (input == null) throw new IllegalStateException("Brak zasobu " + RESOURCE + ".");
                return input.readAllBytes();
            } catch (IOException exception) {
                throw new IllegalStateException("Nie udało się odczytać " + RESOURCE + ".", exception);
            }
        }
    }
}
