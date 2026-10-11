package krys.itemimport.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledTessdataProviderTest {
    @TempDir
    Path temporary;

    @Test
    void shouldReuseCompleteModelAcrossConcurrentReadersWithoutCopyingAgain() throws Exception {
        Path directory = temporary.resolve("concurrent");
        try (var workers = Executors.newFixedThreadPool(8)) {
            var jobs = new ArrayList<Callable<Path>>();
            for (int i = 0; i < 32; i++) jobs.add(() -> BundledTessdataProvider.prepare(directory));
            for (var result : workers.invokeAll(jobs)) assertEquals(directory, result.get());
        }
        assertBundledBytes(directory);
        Path model = directory.resolve("pol.traineddata");
        FileTime sentinel = FileTime.fromMillis(0);
        Files.setLastModifiedTime(model, sentinel);
        assertEquals(directory, BundledTessdataProvider.prepare(directory.resolve(".")));
        assertEquals(sentinel, Files.getLastModifiedTime(model));
        try (var files = Files.list(directory)) {
            assertEquals(java.util.List.of(model), files.toList());
        }
    }

    @Test
    void shouldReuseExistingBundledBytesAndReplaceIncompleteModel() throws Exception {
        Path valid = Files.createDirectories(temporary.resolve("valid"));
        Files.write(valid.resolve("pol.traineddata"), bundledBytes());
        FileTime sentinel = FileTime.fromMillis(0);
        Files.setLastModifiedTime(valid.resolve("pol.traineddata"), sentinel);
        BundledTessdataProvider.prepare(valid);
        assertEquals(sentinel, Files.getLastModifiedTime(valid.resolve("pol.traineddata")));

        Path incomplete = Files.createDirectories(temporary.resolve("incomplete"));
        Files.writeString(incomplete.resolve("pol.traineddata"), "przerwany zapis");
        BundledTessdataProvider.prepare(incomplete);
        assertBundledBytes(incomplete);
    }

    @Test
    void shouldRestoreDeletedModelAndRetryAfterPreparationFailure() throws Exception {
        Path directory = BundledTessdataProvider.prepare(temporary.resolve("deleted"));
        Files.delete(directory.resolve("pol.traineddata"));
        BundledTessdataProvider.prepare(directory);
        assertBundledBytes(directory);

        Path blocked = temporary.resolve("blocked");
        Files.writeString(blocked, "plik zamiast katalogu");
        assertThrows(IllegalStateException.class, () -> BundledTessdataProvider.prepare(blocked));
        Files.delete(blocked);
        BundledTessdataProvider.prepare(blocked);
        assertBundledBytes(blocked);
    }

    @Test
    void shouldShareBundledDirectoryBetweenBothReaders() {
        var part1 = new TesseractPart1RoiOcrReader();
        var blocks = new TesseractPropertyBlockOcrReader();
        assertEquals(part1.getTessdataDirectory(), blocks.getTessdataDirectory());
        assertTrue(Files.isRegularFile(part1.getTessdataDirectory().resolve("pol.traineddata")));
    }

    private static void assertBundledBytes(Path directory) throws Exception {
        assertArrayEquals(bundledBytes(), Files.readAllBytes(directory.resolve("pol.traineddata")));
    }

    private static byte[] bundledBytes() throws Exception {
        try (var input = BundledTessdataProviderTest.class.getResourceAsStream("/tessdata/pol.traineddata")) {
            return input.readAllBytes();
        }
    }
}
