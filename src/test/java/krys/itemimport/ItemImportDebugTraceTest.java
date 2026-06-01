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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(logs.contains("MERGE_SEGMENT_DECISION"));
        assertTrue(logs.contains("decision=acceptedAsOrdinary"));
        assertTrue(logs.contains("MERGE_REJECTED"));
        assertTrue(logs.contains("MERGE_NUMERIC_TOKENS"));
        assertTrue(logs.contains("[12,0]"));
    }

    @Test
    void traceMergeraPokazujeDecyzjeRegionowDlaSegmentowZDlugiejLinii() throws Exception {
        enableDebugForTest();

        String logs = captureLogs(() -> new ItemScreenshotTextMerger().merge(List.of(
                "+25,0% szansy na szczęśliwy traf [20,0]% "
                        + "+3 do umiejętności: Główne [3] "
                        + "+115 pkt. do wszystkich współczynników "
                        + "+12 do maksymalnej liczby kumulacji Animuszu "
                        + "Poddaj się nienawiści i doświadcz Łaski Matki, która zwiększy zadawane przez ciebie obrażenia o 80%[x]. "
                        + "+150 siły Puste gniazdo Wymaga 70 poziomu"
        )));

        assertTrue(logs.contains("MERGE_SEGMENT_DECISION"), logs);
        assertTrue(logs.contains("decision=acceptedAsOrdinary"), logs);
        assertTrue(logs.contains("decision=acceptedAsTransfiguration"), logs);
        assertTrue(logs.contains("decision=acceptedAsTempering"), logs);
        assertTrue(logs.contains("decision=acceptedAsAspect"), logs);
        assertTrue(logs.contains("SOCKET_GEM_RUNE_CANDIDATE"), logs);
        assertTrue(logs.contains("decision=ignoredAsSocketGemRune"), logs);
        assertTrue(logs.contains("segmentStart="), logs);
        assertTrue(logs.contains("segmentEnd="), logs);
        assertTrue(logs.contains("firstAspectEffectStart="), logs);
        assertTrue(logs.contains("firstSocketGemRuneStart="), logs);
        assertTrue(logs.contains("firstLoreVendorRequirementStart="), logs);
        assertTrue(logs.contains("effectiveSegmentRegion=ORDINARY_AFFIX_REGION"), logs);
        assertTrue(logs.contains("effectiveSegmentRegion=TRANSFIGURATION_REGION"), logs);
        assertTrue(logs.contains("effectiveSegmentRegion=TEMPERING_REGION"), logs);
        assertTrue(logs.contains("effectiveSegmentRegion=SOCKET_GEM_RUNE_REGION"), logs);
        assertTrue(logs.contains("localAnchorType=ORDINARY_AFFIX"), logs);
        assertTrue(logs.contains("localAnchorType=TRANSFIGURATION"), logs);
        assertTrue(logs.contains("localAnchorType=TEMPERING"), logs);
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
    void traceFinalnegoFormularzaHartowaniaRozdzielaWartoscZapisanaOcrIResolved() throws Exception {
        enableDebugForTest();

        String logs = captureLogs(() -> {
            ItemImageImportCandidateParseResult result = multiScreenService(
                    List.of(variant("helm-top", ItemImportTextFixtures.heirOfPerditionTopText())),
                    List.of(variant("helm-bottom", ItemImportTextFixtures.heirOfPerditionBottomText()))
            ).analyze(List.of(request("dziedzic-top.png"), request("dziedzic-bottom.png")));
            new ItemImportEditableFormFactory().create(result);
        });

        assertTrue(logs.contains("TEMPERING_FORM definitionId=\"defense_max_animus\""), logs);
        assertTrue(logs.contains("sourceLine=\"+12 do maksymalnej liczby kumulacji Animuszu\""), logs);
        assertTrue(logs.contains("ocrDisplayedValue=12"), logs);
        assertTrue(logs.contains("storedValue=5"), logs);
        assertTrue(logs.contains("greaterAffix=true"), logs);
        assertTrue(logs.contains("masterworkingQuality=25/25"), logs);
        assertTrue(logs.contains("perfectedAffix=\"TEMPERING_AFFIX:defense_max_animus\""), logs);
        assertTrue(logs.contains("resolvedValue=12"), logs);
        assertTrue(logs.contains("resolvedDisplayText=\"+12 do maksymalnej liczby kumulacji Animuszu\""), logs);
        assertTrue(logs.contains("runtimeStatus=DATA_ONLY"), logs);
        assertTrue(logs.contains("stored value is GA/base import value; resolved value uses masterworking perfected tempering"), logs);
        assertFalse(logs.contains("displayText=\"+5 do maksymalnej liczby kumulacji Animuszu\""), logs);
    }

    @Test
    void tracePokazujeIgnorowanieStatowZGniazdBezAffixCandidate() throws Exception {
        enableDebugForTest();

        String logs = captureLogs(() -> {
            ItemImageImportCandidateParseResult result = multiScreenService(
                    List.of(variant("helm-top", ItemImportTextFixtures.heirOfPerditionTopText())),
                    List.of(variant("helm-bottom", ItemImportTextFixtures.heirOfPerditionBottomTextWithJoinedSocketGemStatsAndFooter()))
            ).analyze(List.of(request("dziedzic-top.png"), request("dziedzic-bottom.png")));
            ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);
            assertTrue(form.getAffixes().stream().noneMatch(affix -> affix.getType() == ImportedItemAffixType.STRENGTH));
            assertTrue(form.getAffixes().stream().noneMatch(affix -> affix.getType() == ImportedItemAffixType.INTELLIGENCE));
            assertEquals(2, form.getSocketing().getOccupiedSocketCount());
        });

        assertTrue(logs.contains("SOCKET_GEM_RUNE_CANDIDATE"), logs);
        assertTrue(logs.contains("variantId=\"helm-bottom\""), logs);
        assertTrue(logs.contains("sourceRawLine=\". SIŁY +120 siły"), logs);
        assertTrue(logs.contains("sourceRegion=SOCKET_GEM_RUNE_REGION"), logs);
        assertTrue(logs.contains("parentLineRegion=ASPECT_EFFECT_REGION"), logs);
        assertTrue(logs.contains("parentLineRegion=LORE_VENDOR_REQUIREMENT_REGION"), logs);
        assertTrue(logs.contains("derivedFromSplit=true"), logs);
        assertTrue(logs.contains("segmentStart="), logs);
        assertTrue(logs.contains("segmentEnd="), logs);
        assertTrue(logs.contains("firstAspectEffectStart="), logs);
        assertTrue(logs.contains("firstLoreVendorRequirementStart="), logs);
        assertTrue(logs.contains("effectiveSegmentRegion=SOCKET_GEM_RUNE_REGION"), logs);
        assertTrue(logs.contains("localAnchorType=SOCKET_GEM_RUNE"), logs);
        assertTrue(logs.contains("decision=ignoredAsSocketGemRune"), logs);
        assertTrue(logs.contains("decision=acceptedAsOrdinary"), logs);
        assertTrue(logs.contains("MERGE_TYPED_LINE"), logs);
        assertTrue(logs.contains("MERGE_TYPED_OUTPUT"), logs);
        assertTrue(logs.contains("SOCKET_STAT_PRESERVED_ACROSS_MERGE"), logs);
        assertTrue(logs.contains("SOCKET_STAT_DEDUP_KEY"), logs);
        assertTrue(logs.contains("SOCKET_STAT_OCCURRENCE"), logs);
        assertTrue(logs.contains("runtimeStatus=DATA_ONLY"), logs);
        assertTrue(logs.contains("logicalLine=\"+150 siły\""), logs);
        assertTrue(logs.contains("matchedAffixType=STRENGTH"), logs);
        assertTrue(logs.contains("logicalLine=\"+120 siły\""), logs);
        assertTrue(logs.contains("logicalLine=\"+120 inteligencji\""), logs);
        assertTrue(logs.contains("matchedAffixType=INTELLIGENCE"), logs);
        assertTrue(logs.contains("ignoredForOrdinaryAffixes=true"), logs);
        assertTrue(logs.contains("reason=\"source line belongs to socket/gem/rune region before merger\""), logs);
        assertFalse(logs.contains("MERGE_DECISION logicalLine=\"affix:strength\" selected=\"+150 siły\""), logs);
        assertFalse(logs.contains("MERGE_DECISION logicalLine=\"affix:strength\" selected=\"+120 siły\""), logs);
        assertTrue(logs.contains("SOCKET_GEM_RUNE_MODEL"), logs);
        assertTrue(logs.contains("occupiedSocketCount=2"), logs);
        assertTrue(logs.contains("emptySocketCount=0"), logs);
        assertTrue(logs.contains("totalSocketCount=2"), logs);
        assertTrue(logs.contains("sourceCategory=socketGemRune"), logs);
        assertTrue(logs.contains("displayText=\"+150 siły\""), logs);
        assertTrue(logs.contains("displayText=\"+120 siły\""), logs);
        assertTrue(logs.contains("FINAL_IMPORT_FORM"), logs);
        assertTrue(logs.contains("ordinaryAffixes=4"), logs);
        assertFalse(logs.contains("sourceCategory=ordinary type=STRENGTH value=150.0000"), logs);
        assertFalse(logs.contains("sourceCategory=ordinary type=STRENGTH value=120.0000"), logs);
        assertFalse(logs.contains("sourceCategory=ordinary type=INTELLIGENCE value=120.0000"), logs);
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
