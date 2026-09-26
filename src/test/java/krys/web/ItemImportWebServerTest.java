package krys.web;

import com.sun.net.httpserver.HttpServer;
import krys.hero.HeroClass;
import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.AssembledItemImage;
import krys.itemimport.image.AssembledItemImageDebugWriter;
import krys.itemimport.image.DeterministicItemPartStructureNormalizer;
import krys.itemimport.image.HorizontalDivider;
import krys.itemimport.image.HorizontalDividerDetectionResult;
import krys.itemimport.image.ItemImagePart;
import krys.itemimport.image.ItemPartStructureNormalizer;
import krys.itemimport.image.Part1Roi;
import krys.itemimport.image.Part1RoiOcrReader;
import krys.itemimport.image.Part1RoiOcrResult;
import krys.itemimport.image.Part1RoiOcrSubRegion;
import krys.itemimport.image.Part1RoiSegmenter;
import krys.itemimport.image.Part1RoiType;
import krys.itemimport.image.Part1SegmentationResult;
import krys.itemimport.image.PropertyBlock;
import krys.itemimport.image.PropertyBlockAnchorType;
import krys.itemimport.image.PropertyBlockSegmentationResult;
import krys.itemimport.image.PropertyBlockSegmenter;
import krys.itemimport.image.StructuralItemPart;
import krys.itemimport.image.StructuralItemPartSlot;
import krys.itemimport.image.StructuredItemParts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy HTTP aktualnego importu obrazu: Etap 1/2/3A oraz surowy POC OCR PART_1. */
class ItemImportWebServerTest {
    private HttpServer server;
    private HttpClient httpClient;
    private String baseUrl;
    private InMemoryHeroProfileRepository heroRepository;
    private CapturingAssembler assembler;
    private StubDividerDetector dividerDetector;
    private ConfigurableStructureNormalizer structureNormalizer;
    private StubPart1RoiSegmenter part1RoiSegmenter;
    private StubPart1RoiOcrReader part1RoiOcrReader;
    private StubPropertyBlockSegmenter propertyBlockSegmenter;

    @BeforeEach
    void setUp() throws Exception {
        heroRepository = new InMemoryHeroProfileRepository();
        HeroService heroService = new HeroService(heroRepository);
        assembler = new CapturingAssembler();
        dividerDetector = new StubDividerDetector();
        structureNormalizer = new ConfigurableStructureNormalizer();
        part1RoiSegmenter = new StubPart1RoiSegmenter();
        part1RoiOcrReader = new StubPart1RoiOcrReader();
        propertyBlockSegmenter = new StubPropertyBlockSegmenter();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/importuj-item-ze-screena", new ItemImportController(
                new ItemImportPageRenderer(),
                heroService,
                assembler,
                new AssembledItemImageDebugWriter(),
                dividerDetector,
                structureNormalizer,
                part1RoiSegmenter,
                part1RoiOcrReader,
                propertyBlockSegmenter
        ));
        server.start();
        httpClient = HttpClient.newHttpClient();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldRenderEmptyStateWithoutActiveHeroAndWithoutLegacyOcrSections() throws Exception {
        HttpResponse<String> response = sendGet("/importuj-item-ze-screena");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Importuj pojedynczy item ze screena"));
        assertTrue(response.body().contains("Brak aktywnego bohatera"));
        assertFalse(response.body().contains("Wstępnie rozpoznane pola"));
        assertFalse(response.body().contains("Surowy tekst OCR"));
        assertFalse(response.body().contains("Zatwierdź import itemu"));
    }

    @Test
    void shouldAcceptScreenshotsAndStopAtStructuredItemParts() throws Exception {
        createActiveHero();
        dividerDetector.rawPartCount = 3;

        HttpResponse<String> response = sendMultipartUpload(pngBytes(Color.ORANGE), pngBytes(Color.CYAN));

        assertEquals(200, response.statusCode());
        assertEquals(2, assembler.lastRequestCount);
        assertTrue(response.body().contains("data-opencv-assembled-preview"));
        assertTrue(response.body().contains("data-opencv-divider-diagnostics"));
        assertTrue(response.body().contains("data-opencv-part-preview"));
        assertTrue(response.body().contains("data-item-structure-normalization"));
        assertTrue(response.body().contains("data-item-structure-status=\"STRUCTURALLY_NORMALIZED\""));
        assertStructureMapping(response.body(), "PART_1", true, 1);
        assertStructureMapping(response.body(), "PART_2", false, 0);
        assertStructureMapping(response.body(), "PART_3", true, 2);
        assertStructureMapping(response.body(), "PART_4", true, 3);
        assertPart4Skipped(response.body());
        assertTrue(response.body().contains("data-property-block-segmentation"));
        assertTrue(response.body().contains("data-property-block-source=\"PART_3\" data-property-block-count=\"2\""));
        assertTrue(response.body().contains("data-property-block-anchor-type=\"NONE\""));
        assertTrue(response.body().contains("data-property-block-anchor-type=\"DIAMOND\""));
        assertEquals(List.of(StructuralItemPartSlot.PART_3), propertyBlockSegmenter.segmentedSlots);
        assertTrue(response.body().contains("data-part1-segmentation"));
        assertTrue(response.body().contains("data-part1-source"));
        assertTrue(response.body().contains("data-part1-roi-type=\"NAME_AND_STARS\""));
        assertTrue(response.body().contains("data-part1-roi-type=\"CLASSIFICATION\""));
        assertTrue(response.body().contains("data-part1-roi-type=\"ITEM_POWER\""));
        assertTrue(response.body().contains("data-part1-roi-type=\"OPTIONAL_INFO\""));
        assertTrue(response.body().contains("data-part1-roi-ocr"));
        assertTrue(response.body().contains("data-part1-roi-subregion-type=\"NAME_TEXT\""));
        assertTrue(response.body().contains("data-part1-roi-subregion-type=\"NAME_TEXT_CLEANED\""));
        assertTrue(response.body().contains("data-part1-roi-subregion-type=\"NAME_TEXT_FINAL\""));
        assertTrue(response.body().contains("data-part1-greater-affix-count=\"2\""));
        assertTrue(response.body().contains("Greater Affix stars: 2"));
        assertTrue(response.body().contains("data-part1-roi-subregion-type=\"GREATER_AFFIX_STAR_1\""));
        assertTrue(response.body().contains("stub-name-profile"));
        assertTrue(response.body().contains("stub-name-cleanup"));
        assertTrue(response.body().contains("stub-name-final"));
        assertTrue(response.body().contains("stub-star-confidence"));
        assertTrue(response.body().contains("raw-NAME_AND_STARS"));
        assertTrue(response.body().contains("raw-CLASSIFICATION"));
        assertTrue(response.body().contains("raw-ITEM_POWER"));
        assertTrue(response.body().contains("raw-OPTIONAL_INFO"));
        assertTrue(response.body().contains("data-part1-read-result"));
        assertTrue(response.body().contains("PART_1 — Wynik odczytu"));
        assertTrue(response.body().contains("raw-NAME_AND_STARS"));
        assertTrue(response.body().contains("raw-CLASSIFICATION"));
        assertTrue(response.body().contains("777"));
        assertTrue(response.body().contains("Typ"));
        assertTrue(response.body().contains("Tarcza"));
        assertTrue(response.body().contains("Rzadkość"));
        assertTrue(response.body().contains("LEGENDARY"));
        assertTrue(response.body().contains("Ancient"));
        assertTrue(response.body().contains("true"));
        assertTrue(response.body().contains("Mythic Unique"));
        assertTrue(response.body().contains("false"));
        assertTrue(response.body().contains("Slot"));
        assertTrue(response.body().contains("OFF_HAND"));
        assertTrue(response.body().contains("Jakość wyświetlana"));
        assertTrue(response.body().contains("Masterworking quality"));
        assertEquals(List.of(
                Part1RoiType.NAME_AND_STARS,
                Part1RoiType.CLASSIFICATION,
                Part1RoiType.ITEM_POWER,
                Part1RoiType.OPTIONAL_INFO
        ), part1RoiOcrReader.readTypes);
        assertFalse(response.body().contains("Surowy tekst OCR"));
        assertFalse(response.body().contains("Odczyt OCR"));
        assertFalse(response.body().contains("Zatwierdź import itemu"));
    }

    @Test
    void shouldReadEachAvailablePart1RoiSeparatelyWhenOptionalInfoIsMissing() throws Exception {
        createActiveHero();
        dividerDetector.rawPartCount = 3;
        part1RoiSegmenter.includeOptionalInfo = false;

        HttpResponse<String> response = sendMultipartUpload(pngBytes(Color.ORANGE));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("data-part1-roi-type=\"NAME_AND_STARS\""));
        assertTrue(response.body().contains("data-part1-roi-type=\"CLASSIFICATION\""));
        assertTrue(response.body().contains("data-part1-roi-type=\"ITEM_POWER\""));
        assertFalse(response.body().contains("data-part1-roi-type=\"OPTIONAL_INFO\""));
        assertTrue(response.body().contains("data-part1-read-result"));
        assertTrue(response.body().contains("Masterworking quality"));
        assertTrue(response.body().contains("brak"));
        assertEquals(List.of(
                Part1RoiType.NAME_AND_STARS,
                Part1RoiType.CLASSIFICATION,
                Part1RoiType.ITEM_POWER
        ), part1RoiOcrReader.readTypes);
    }

    @Test
    void shouldRenderFourPartStructureWithoutAbsentSlots() throws Exception {
        createActiveHero();
        dividerDetector.rawPartCount = 4;

        HttpResponse<String> response = sendMultipartUpload(pngBytes(Color.ORANGE));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("data-item-structure-status=\"STRUCTURALLY_NORMALIZED\""));
        assertStructureMapping(response.body(), "PART_1", true, 1);
        assertStructureMapping(response.body(), "PART_2", true, 2);
        assertStructureMapping(response.body(), "PART_3", true, 3);
        assertStructureMapping(response.body(), "PART_4", true, 4);
        assertPart4Skipped(response.body());
        assertEquals(List.of(StructuralItemPartSlot.PART_1), part1RoiSegmenter.segmentedSlots);
        assertEquals(List.of(StructuralItemPartSlot.PART_2, StructuralItemPartSlot.PART_3),
                propertyBlockSegmenter.segmentedSlots);
        assertEquals(List.of(
                Part1RoiType.NAME_AND_STARS,
                Part1RoiType.CLASSIFICATION,
                Part1RoiType.ITEM_POWER,
                Part1RoiType.OPTIONAL_INFO
        ), part1RoiOcrReader.readTypes);
        assertFalse(response.body().contains("data-item-structure-present=\"false\""));
    }

    @Test
    void shouldRenderIncompleteForTwoRawPartsWithoutGuessedMappings() throws Exception {
        createActiveHero();
        dividerDetector.rawPartCount = 2;

        HttpResponse<String> response = sendMultipartUpload(pngBytes(Color.ORANGE));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("data-item-structure-status=\"INCOMPLETE\""));
        assertTrue(response.body().contains("data-item-structure-raw-part-count=\"2\""));
        assertTrue(response.body().contains("Brak normalizacji strukturalnej. Nie zgadujemy brakujących slotów."));
        assertFalse(response.body().contains("data-item-structure-row"));
        assertFalse(response.body().contains("data-item-structure-slot=\"PART_1\""));
        assertFalse(response.body().contains("data-item-structure-slot=\"PART_2\""));
        assertFalse(response.body().contains("data-part1-segmentation"));
    }

    @Test
    void shouldKeepDividerDiagnosticsWhenStructureNormalizationFails() throws Exception {
        createActiveHero();
        dividerDetector.rawPartCount = 3;
        structureNormalizer.failure = new RuntimeException("structure-stop");

        HttpResponse<String> response = sendMultipartUpload(pngBytes(Color.ORANGE));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("data-opencv-assembled-preview"));
        assertTrue(response.body().contains("data-opencv-divider-diagnostics"));
        assertTrue(response.body().contains("data-opencv-part-preview"));
        assertTrue(response.body().contains("Błąd normalizacji struktury: structure-stop"));
        assertFalse(response.body().contains("data-item-structure-normalization"));
        assertFalse(response.body().contains("data-item-structure-status=\"STRUCTURALLY_NORMALIZED\""));
        assertFalse(response.body().contains("Błąd detekcji separatorów: structure-stop"));
    }

    @Test
    void shouldShowAssemblerErrorWithoutLegacyOcrFallback() throws Exception {
        createActiveHero();
        assembler.failure = new IllegalArgumentException("assembler-stop");

        HttpResponse<String> response = sendMultipartUpload(pngBytes(Color.BLACK));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Błąd podglądu OpenCV: assembler-stop"));
        assertFalse(response.body().contains("data-opencv-assembled-preview"));
        assertFalse(response.body().contains("Surowy tekst OCR"));
    }

    @Test
    void shouldKeepStageOneDiagnosticsWhenDividerDetectionFails() throws Exception {
        createActiveHero();
        dividerDetector.failure = new IllegalStateException("divider-stop");

        HttpResponse<String> response = sendMultipartUpload(pngBytes(Color.GREEN));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("data-opencv-assembled-preview"));
        assertTrue(response.body().contains("Błąd detekcji separatorów: divider-stop"));
        assertFalse(response.body().contains("data-item-structure-normalization"));
        assertFalse(response.body().contains("Surowy tekst OCR"));
    }

    private void createActiveHero() {
        new HeroService(heroRepository).createHero("Importer", HeroClass.PALADIN, 13);
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private HttpResponse<String> sendMultipartUpload(byte[]... images) throws Exception {
        String boundary = "----dstats-test-boundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (int index = 0; index < images.length; index++) {
            body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Disposition: form-data; name=\"itemImage\"; filename=\"item-" + index + ".png\"\r\n").getBytes(StandardCharsets.UTF_8));
            body.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            body.write(images[index]);
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/importuj-item-ze-screena"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private static byte[] pngBytes(Color color) throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static void assertStructureMapping(String html, String slot, boolean present, int rawPartIndex) {
        assertTrue(html.contains("data-item-structure-row data-item-structure-slot=\"" + slot
                + "\" data-item-structure-present=\"" + present
                + "\" data-item-structure-raw-part-index=\"" + rawPartIndex + "\""));
    }

    private static void assertPart4Skipped(String html) {
        assertTrue(html.contains("data-item-structure-slot=\"PART_4\""));
        assertTrue(html.contains("data-item-structure-semantic-status=\"SKIPPED_NOT_REQUIRED\""));
        assertTrue(html.contains("SKIPPED_NOT_REQUIRED — pominięty zgodnie z projektem"));
    }

    private static final class CapturingAssembler implements krys.itemimport.image.ItemImageAssembler {
        private int lastRequestCount;
        private RuntimeException failure;

        @Override
        public AssembledItemImage assemble(List<ItemImageImportRequest> requests) {
            if (failure != null) {
                throw failure;
            }
            lastRequestCount = requests.size();
            try {
                return new AssembledItemImage(
                        pngBytes(Color.MAGENTA),
                        "PNG",
                        8,
                        8,
                        requests.size(),
                        requests.size() == 1 ? "single-image" : "vertical-stitch",
                        requests.size() == 1 ? AssembledItemImage.STATUS_SINGLE_IMAGE : AssembledItemImage.STATUS_STITCHED,
                        List.of()
                );
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
    }

    private static final class StubDividerDetector implements krys.itemimport.image.HorizontalDividerDetector {
        private RuntimeException failure;
        private int rawPartCount = 3;

        @Override
        public HorizontalDividerDetectionResult detect(AssembledItemImage image) {
            if (failure != null) {
                throw failure;
            }
            try {
                List<ItemImagePart> parts = new ArrayList<>();
                List<HorizontalDivider> dividers = new ArrayList<>();
                int partHeight = 8 / rawPartCount;
                int startY = 0;
                for (int index = 1; index <= rawPartCount; index++) {
                    int endY = index == rawPartCount ? 8 : startY + partHeight;
                    parts.add(new ItemImagePart(index, "PART_" + index + "_RAW",
                            startY, endY, 8, endY - startY, "PNG", pngBytes(Color.RED)));
                    if (index < rawPartCount) {
                        dividers.add(new HorizontalDivider(endY, Math.max(0, endY - 1), Math.min(8, endY + 1), 0.92d));
                    }
                    startY = endY;
                }
                return new HorizontalDividerDetectionResult(
                        dividers,
                        parts,
                        pngBytes(Color.WHITE),
                        "PNG",
                        8,
                        8
                );
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
    }

    private static final class ConfigurableStructureNormalizer implements ItemPartStructureNormalizer {
        private final DeterministicItemPartStructureNormalizer delegate = new DeterministicItemPartStructureNormalizer();
        private RuntimeException failure;

        @Override
        public StructuredItemParts normalize(List<ItemImagePart> rawParts) {
            if (failure != null) {
                throw failure;
            }
            return delegate.normalize(rawParts);
        }
    }

    private static final class StubPart1RoiSegmenter implements Part1RoiSegmenter {
        private boolean includeOptionalInfo = true;
        private final List<StructuralItemPartSlot> segmentedSlots = new ArrayList<>();

        @Override
        public Part1SegmentationResult segment(StructuralItemPart part1) {
            segmentedSlots.add(part1.getSlot());
            try {
                List<Part1Roi> rois = new ArrayList<>();
                rois.add(new Part1Roi(Part1RoiType.NAME_AND_STARS, 0, 0, 4, 2, "PNG", pngBytes(Color.YELLOW)));
                rois.add(new Part1Roi(Part1RoiType.CLASSIFICATION, 0, 2, 4, 2, "PNG", pngBytes(Color.ORANGE)));
                rois.add(new Part1Roi(Part1RoiType.ITEM_POWER, 0, 4, 4, 2, "PNG", pngBytes(Color.WHITE)));
                if (includeOptionalInfo) {
                    rois.add(new Part1Roi(Part1RoiType.OPTIONAL_INFO, 0, 6, 4, 2, "PNG", pngBytes(Color.GRAY)));
                }
                return new Part1SegmentationResult(
                        part1,
                        rois,
                        ""
                );
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
    }

    private static final class StubPropertyBlockSegmenter implements PropertyBlockSegmenter {
        private final List<StructuralItemPartSlot> segmentedSlots = new ArrayList<>();

        @Override
        public PropertyBlockSegmentationResult segment(StructuralItemPart part) {
            segmentedSlots.add(part.getSlot());
            try {
                return new PropertyBlockSegmentationResult(
                        part,
                        List.of(
                                new PropertyBlock(
                                        part.getSlot(),
                                        1,
                                        0,
                                        0,
                                        4,
                                        2,
                                        PropertyBlockAnchorType.NONE,
                                        null,
                                        "PNG",
                                        pngBytes(Color.BLUE),
                                        "lineCount=1"
                                ),
                                new PropertyBlock(
                                        part.getSlot(),
                                        2,
                                        0,
                                        2,
                                        4,
                                        2,
                                        PropertyBlockAnchorType.DIAMOND,
                                        null,
                                        "PNG",
                                        pngBytes(Color.PINK),
                                        "lineCount=1"
                                )
                        ),
                        "stub-property-blocks"
                );
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
    }

    private static final class StubPart1RoiOcrReader implements Part1RoiOcrReader {
        private final List<Part1RoiType> readTypes = new ArrayList<>();

        @Override
        public Part1RoiOcrResult read(Part1Roi roi) {
            readTypes.add(roi.getType());
            if (roi.getType() == Part1RoiType.NAME_AND_STARS) {
                try {
                    return new Part1RoiOcrResult(roi.getType(), "raw-" + roi.getType(), 7L, "stub-preprocess", List.of(
                            new Part1RoiOcrSubRegion("NAME_TEXT", 0, 0, 3, 2, "PNG",
                                    pngBytes(Color.YELLOW), "stub-name-profile"),
                            new Part1RoiOcrSubRegion("NAME_TEXT_CLEANED", 0, 0, 3, 2, "PNG",
                                    pngBytes(Color.ORANGE), "stub-name-cleanup"),
                            new Part1RoiOcrSubRegion("NAME_TEXT_FINAL", 0, 0, 12, 8, "PNG",
                                    pngBytes(Color.WHITE), "stub-name-final"),
                            new Part1RoiOcrSubRegion("GREATER_AFFIX_STAR_1", 2, 0, 2, 2, "PNG",
                                    pngBytes(Color.PINK), "stub-star-confidence")
                    ), 2);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }
            if (roi.getType() == Part1RoiType.ITEM_POWER) {
                return new Part1RoiOcrResult(roi.getType(), "raw-ITEM_POWER Moc przedmiotu: 777", 7L, "stub-preprocess");
            }
            if (roi.getType() == Part1RoiType.OPTIONAL_INFO) {
                return new Part1RoiOcrResult(roi.getType(), "25 (% +25) jakości\nraw-OPTIONAL_INFO", 7L, "stub-preprocess");
            }
            if (roi.getType() == Part1RoiType.CLASSIFICATION) {
                return new Part1RoiOcrResult(roi.getType(), "raw-CLASSIFICATION Starożytna legendarna tarcza", 7L, "stub-preprocess");
            }
            return new Part1RoiOcrResult(roi.getType(), "raw-" + roi.getType(), 7L, "stub-preprocess");
        }
    }

    private static final class InMemoryHeroProfileRepository implements HeroProfileRepository {
        private final List<HeroProfile> heroes = new ArrayList<>();
        private Long activeHeroId;

        @Override
        public HeroProfile save(HeroProfile heroProfile) {
            delete(heroProfile.getHeroId());
            heroes.add(heroProfile);
            return heroProfile;
        }

        @Override
        public List<HeroProfile> findAll() {
            return List.copyOf(heroes);
        }

        @Override
        public Optional<HeroProfile> findById(long heroId) {
            return heroes.stream().filter(hero -> hero.getHeroId() == heroId).findFirst();
        }

        @Override
        public void delete(long heroId) {
            heroes.removeIf(hero -> hero.getHeroId() == heroId);
        }

        @Override
        public Optional<Long> loadActiveHeroId() {
            return Optional.ofNullable(activeHeroId);
        }

        @Override
        public void saveActiveHeroId(Long heroId) {
            activeHeroId = heroId;
        }
    }
}
