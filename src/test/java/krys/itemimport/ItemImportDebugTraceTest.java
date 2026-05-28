package krys.itemimport;

import krys.web.ItemImportPageModel;
import krys.web.ItemImportPageRenderer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje kontrolowany trace techniczny pipeline'u importu itemów. */
class ItemImportDebugTraceTest {
    private static final String CRIT_WITH_REFERENCE = """
            DIAGNOSTYCZNY HELM
            Moc przedmiotu: 900
            Starozytny mityczny unikatowy helm
            +15,0% szansy na trafienie krytyczne [12,0]%
            """;
    private static final String CRIT_WITHOUT_REFERENCE = """
            DIAGNOSTYCZNY HELM
            Moc przedmiotu: 900
            Starozytny mityczny unikatowy helm
            +15,0% szansy na trafienie krytyczne
            """;

    @AfterEach
    void clearDebugProperties() {
        System.clearProperty(ItemImportDebugTrace.JVM_PROPERTY);
        System.clearProperty(ItemImportDebugTrace.CONFIG_PROPERTY);
        System.clearProperty(ItemImportDebugTrace.FILE_PROPERTY);
    }

    @Test
    void debugImportuJestDomyslnieWylaczony() throws Exception {
        String logs = captureLogs(() -> {
            ItemImageImportCandidateParseResult result = singleScreenService(
                    List.of(variant("plain", CRIT_WITH_REFERENCE))
            ).analyze(request("single.png"));
            new ItemImportEditableFormFactory().create(result);
        });

        assertFalse(logs.contains("[ITEM_IMPORT_DEBUG]"));
        assertFalse(logs.contains("IMPORT_REQUEST"));
    }

    @Test
    void debugWlaczonySystemPropertyLogujeGlowneSekcjePipeline() throws Exception {
        enableDebugForTest();

        String logs = captureLogs(() -> {
            ItemImageImportCandidateParseResult result = singleScreenService(List.of(
                    variant("bez-bracketu", CRIT_WITHOUT_REFERENCE),
                    variant("z-bracketem", CRIT_WITH_REFERENCE)
            )).analyze(request("single.png"));
            new ItemImportEditableFormFactory().create(result);
        });

        assertTrue(logs.contains("IMPORT_REQUEST"));
        assertTrue(logs.contains("OCR_RAW_VARIANTS"));
        assertTrue(logs.contains("SCREEN_MERGER_INPUT"));
        assertTrue(logs.contains("SCREEN_MERGER_OUTPUT"));
        assertTrue(logs.contains("ITEM_DETAILS"));
        assertTrue(logs.contains("AFFIX_CANDIDATE"));
        assertTrue(logs.contains("AFFIX_MERGE"));
        assertTrue(logs.contains("FINAL_IMPORT_FORM"));
    }

    @Test
    void multiScreenImportTracePokazujeLiczbeScreenowMergerIFinalnyFormularz() throws Exception {
        enableDebugForTest();

        String logs = captureLogs(() -> {
            ItemImageImportCandidateParseResult result = multiScreenService(
                    List.of(variant("screen-0", CRIT_WITHOUT_REFERENCE)),
                    List.of(variant("screen-1", CRIT_WITH_REFERENCE))
            ).analyze(List.of(request("screen-a.png"), request("screen-b.png")));
            new ItemImportEditableFormFactory().create(result);
        });

        assertTrue(logs.contains("files=2 mode=MULTI"));
        assertTrue(logs.contains("SCREEN_MERGER_OUTPUT"));
        assertTrue(logs.contains("scope=multi-final"));
        assertTrue(logs.contains("FINAL_IMPORT_FORM"));
    }

    @Test
    void traceMergeraPokazujeWidocznoscLiniiZTokenamiLiczbowymiIBracketem() throws Exception {
        enableDebugForTest();

        String logs = captureLogs(() -> new ItemScreenshotTextMerger().merge(List.of(
                "+15,0% szansy na trafienie krytyczne",
                "+15,0% szansy na trafienie krytyczne [12,0]%"
        )));

        assertTrue(logs.contains("MERGE_DECISION"));
        assertTrue(logs.contains("MERGE_REJECTED"));
        assertTrue(logs.contains("MERGE_NUMERIC_TOKENS"));
        assertTrue(logs.contains("[12,0]"));
    }

    @Test
    void traceFinalnegoFormularzaZawieraPolaAffixow() throws Exception {
        enableDebugForTest();

        String logs = captureLogs(() -> {
            ItemImageImportCandidateParseResult result = singleScreenService(
                    List.of(variant("z-bracketem", CRIT_WITH_REFERENCE))
            ).analyze(request("single.png"));
            new ItemImportEditableFormFactory().create(result);
        });

        assertTrue(logs.contains("FINAL_IMPORT_FORM"));
        assertTrue(logs.contains("type=CRITICAL_STRIKE_CHANCE"));
        assertTrue(logs.contains("value=15.0000"));
        assertTrue(logs.contains("referenceValue=12.0000"));
        assertTrue(logs.contains("rollRangeMin=null"));
        assertTrue(logs.contains("rollRangeMax=null"));
        assertTrue(logs.contains("greaterAffix=false"));
    }

    @Test
    void rendererImportuNiePokazujeTechnicznegoDebugOutput() throws Exception {
        enableDebugForTest();
        final ItemImageImportCandidateParseResult[] resultHolder = new ItemImageImportCandidateParseResult[1];
        final ItemImportEditableForm[] formHolder = new ItemImportEditableForm[1];
        captureLogs(() -> {
            ItemImageImportCandidateParseResult result = singleScreenService(
                    List.of(variant("z-bracketem", CRIT_WITH_REFERENCE))
            ).analyze(request("single.png"));
            resultHolder[0] = result;
            formHolder[0] = new ItemImportEditableFormFactory().create(result);
        });

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                formHolder[0],
                resultHolder[0],
                List.of(),
                null,
                null,
                "",
                ""
        ));

        assertFalse(html.contains("ITEM_IMPORT_DEBUG"));
        assertFalse(html.contains("IMPORT_REQUEST"));
        assertFalse(html.contains("OCR_RAW_VARIANTS"));
    }

    private static ItemImageImportService singleScreenService(List<ItemImageOcrTextVariant> variants) {
        return multiScreenService(variants);
    }

    @SafeVarargs
    private static ItemImageImportService multiScreenService(List<ItemImageOcrTextVariant>... batches) {
        return new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new QueuedOcrTextReader(List.of(batches)),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger(),
                new ItemScreenshotTextMerger()
        );
    }

    private static ItemImageOcrTextVariant variant(String id, String text) {
        return new ItemImageOcrTextVariant(id, text);
    }

    private static ItemImageImportRequest request(String fileName) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB), "png", output);
        return new ItemImageImportRequest(fileName, "image/png", output.toByteArray());
    }

    private static void enableDebugForTest() {
        System.setProperty(ItemImportDebugTrace.JVM_PROPERTY, "true");
        System.setProperty(ItemImportDebugTrace.FILE_PROPERTY,
                Path.of("target", "item-import-debug-test.log").toString());
    }

    private static String captureLogs(ThrowingRunnable runnable) throws Exception {
        Logger logger = Logger.getLogger(ItemImportDebugTrace.LOGGER_NAME);
        CapturingHandler handler = new CapturingHandler();
        handler.setLevel(Level.ALL);
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        Level previousLevel = logger.getLevel();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        logger.addHandler(handler);
        try {
            runnable.run();
            return handler.contents();
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(previousUseParentHandlers);
            logger.setLevel(previousLevel);
        }
    }

    private static final class QueuedOcrTextReader implements ItemImageOcrTextReader {
        private final List<List<ItemImageOcrTextVariant>> batches;
        private int index;

        private QueuedOcrTextReader(List<List<ItemImageOcrTextVariant>> batches) {
            this.batches = batches;
        }

        @Override
        public List<ItemImageOcrTextVariant> readTextVariants(List<ItemImageOcrVariant> variants) {
            if (batches.isEmpty()) {
                return List.of();
            }
            List<ItemImageOcrTextVariant> result = batches.get(Math.min(index, batches.size() - 1));
            index++;
            return result;
        }
    }

    private static final class CapturingHandler extends Handler {
        private final StringBuilder builder = new StringBuilder();

        @Override
        public void publish(LogRecord record) {
            builder.append(record.getMessage()).append('\n');
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        private String contents() {
            return builder.toString();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
