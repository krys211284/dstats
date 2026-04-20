package krys.itemlibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Rozwiązuje trwałą lokalizację lokalnej biblioteki itemów i migruje starszy katalog runtime poza target. */
public final class ItemLibraryDataDirectoryResolver {
    public static final String DATA_DIRECTORY_PROPERTY = "dstats.dataDir";

    private static final String SAVED_ITEMS_FILE_NAME = "saved-items.db";
    private static final String ACTIVE_SELECTION_FILE_NAME = "active-selection.db";

    private final String configuredDataDirectory;
    private final Path userHomeDirectory;
    private final Path legacyDataDirectory;

    public ItemLibraryDataDirectoryResolver() {
        this(
                System.getProperty(DATA_DIRECTORY_PROPERTY),
                readUserHomeDirectory(),
                Path.of("target", "item-library-runtime")
        );
    }

    ItemLibraryDataDirectoryResolver(String configuredDataDirectory,
                                     Path userHomeDirectory,
                                     Path legacyDataDirectory) {
        this.configuredDataDirectory = configuredDataDirectory;
        this.userHomeDirectory = requirePath(userHomeDirectory, "Katalog użytkownika dla biblioteki itemów jest wymagany.")
                .toAbsolutePath()
                .normalize();
        this.legacyDataDirectory = requirePath(legacyDataDirectory, "Stara lokalizacja biblioteki itemów jest wymagana.")
                .toAbsolutePath()
                .normalize();
    }

    public Path resolveDataDirectory() {
        Path resolvedDataDirectory = resolveConfiguredOrDefaultDirectory();
        migrateLegacyDataIfNeeded(resolvedDataDirectory);
        return resolvedDataDirectory;
    }

    private Path resolveConfiguredOrDefaultDirectory() {
        if (configuredDataDirectory != null && !configuredDataDirectory.isBlank()) {
            return Path.of(configuredDataDirectory).toAbsolutePath().normalize();
        }
        return userHomeDirectory.resolve(".dstats").resolve("item-library").toAbsolutePath().normalize();
    }

    private void migrateLegacyDataIfNeeded(Path resolvedDataDirectory) {
        if (resolvedDataDirectory.equals(legacyDataDirectory)) {
            return;
        }
        if (!hasRuntimeData(legacyDataDirectory)) {
            return;
        }
        if (hasRuntimeData(resolvedDataDirectory)) {
            // Gdy nowa lokalizacja ma już choć jeden plik runtime, staje się źródłem prawdy
            // i nie mieszamy plików z legacy target, żeby nie nadpisać nowszej sesji użytkownika.
            return;
        }

        try {
            Files.createDirectories(resolvedDataDirectory);
            copyIfExists(
                    legacyDataDirectory.resolve(SAVED_ITEMS_FILE_NAME),
                    resolvedDataDirectory.resolve(SAVED_ITEMS_FILE_NAME)
            );
            copyIfExists(
                    legacyDataDirectory.resolve(ACTIVE_SELECTION_FILE_NAME),
                    resolvedDataDirectory.resolve(ACTIVE_SELECTION_FILE_NAME)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się zmigrować biblioteki itemów do stabilnego katalogu danych.", exception);
        }
    }

    private static void copyIfExists(Path source, Path target) throws IOException {
        if (!Files.isRegularFile(source)) {
            return;
        }
        Files.copy(source, target);
    }

    private static boolean hasRuntimeData(Path directory) {
        return Files.isRegularFile(directory.resolve(SAVED_ITEMS_FILE_NAME))
                || Files.isRegularFile(directory.resolve(ACTIVE_SELECTION_FILE_NAME));
    }

    private static Path readUserHomeDirectory() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            throw new IllegalStateException("Brak system property user.home potrzebnego do wyznaczenia katalogu biblioteki itemów.");
        }
        return Path.of(userHome);
    }

    private static Path requirePath(Path path, String message) {
        if (path == null) {
            throw new IllegalArgumentException(message);
        }
        return path;
    }
}
