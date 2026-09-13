package krys.web;

import com.sun.net.httpserver.HttpServer;
import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.ItemImageMetadata;
import krys.itemimport.ItemImportFieldCandidate;
import krys.itemimport.ItemImportFieldConfidence;
import krys.itemimport.ItemImportEditableFormFactory;
import krys.itemimport.ItemImportFormMapper;
import krys.itemimport.ValidatedImportedItemToItemMapper;
import krys.itemimport.FullItemReadAffixUpdater;
import krys.itemimport.ImportedItemCurrentBuildContributionMapper;
import krys.itemimport.image.AssembledItemImage;
import krys.itemimport.image.AssembledItemImageDebugWriter;
import krys.itemimport.image.opencv.OpenCvItemImageAssembler;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy GUI flow importu itemu pokrywają upload obrazu, ręczne potwierdzenie i mapowanie do current build. */
class ItemImportWebServerTest {
    private CurrentBuildWebServer webServer;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        Path tempDirectory = Files.createTempDirectory("item-import-web");
        webServer = new CurrentBuildWebServer(0, tempDirectory);
        webServer.start();
        httpClient = HttpClient.newHttpClient();
        baseUrl = "http://127.0.0.1:" + webServer.getPort();
    }

    @AfterEach
    void tearDown() {
        if (webServer != null) {
            webServer.close();
        }
    }

    @Test
    void shouldRenderEmptyStateWithoutActiveHero() throws Exception {
        HttpResponse<String> response = sendGet("/importuj-item-ze-screena");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Importuj pojedynczy item ze screena"));
        assertTrue(response.body().contains("Brak aktywnego bohatera"));
        assertTrue(response.body().contains("Najpierw wybierz bohatera"));
        assertTrue(response.body().contains("Przejdź do modułu Bohaterowie"));
    }

    @Test
    void shouldRenderItemImportUploadForm() throws Exception {
        createHero("Importer", "13");
        HttpResponse<String> response = sendGet("/importuj-item-ze-screena");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Importuj pojedynczy item ze screena"));
        assertTrue(response.body().contains("name=\"itemImage\""));
        assertTrue(response.body().contains("Możesz dodać jeden lub kilka screenów tego samego itemu."));
        assertTrue(response.body().contains("multiple"));
        assertTrue(response.body().contains("Wstępnie rozpoznane pola"));
        assertTrue(response.body().contains("Tu pojawią się rozpoznane pola itemu"));
        assertTrue(response.body().contains("Aktywny bohater importu"));
    }

    @Test
    void shouldExposeCurrentBuildIntegrationButtonWithoutRegressingCurrentBuildFlow() throws Exception {
        createHero("Importer", "13");
        HttpResponse<String> response = sendGet("/policz-aktualny-build");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("href=\"/importuj-item-ze-screena"));
        assertTrue(response.body().contains("Importuj nowy item"));
        assertTrue(response.body().contains("current-build-sticky-actions"));
        assertTrue(response.body().contains("Zapisz zmiany"));
    }

    @Test
    void shouldUploadImageAndRenderManualConfirmationFields() throws Exception {
        createHero("Importer", "13");
        HttpResponse<String> response = sendMultipartImagePost("/importuj-item-ze-screena", "sztylet.png", "image/png", png(1, 1, Color.RED));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Podgląd obrazu przygotowanego przez OpenCV"));
        assertTrue(response.body().contains("Podgląd nowego pipeline'u obrazu. OCR nadal korzysta z dotychczasowego mechanizmu."));
        assertEquals(1, countOccurrences(response.body(), "data-opencv-assembled-preview"));
        assertTrue(response.body().contains("data:image/png;base64,"));
        assertTrue(response.body().contains("Liczba obrazów źródłowych"));
        assertTrue(response.body().contains("<div class=\"summary-value\">1"));
        assertTrue(response.body().contains("Wstępnie rozpoznane pola"));
        assertTrue(response.body().contains("sztylet.png"));
        assertTrue(response.body().contains("Rozmiar obrazu"));
        assertTrue(response.body().contains("1 x 1"));
        assertTrue(response.body().contains("Ręczne potwierdzenie itemu"));
        assertTrue(response.body().contains("name=\"sourceImageName\" value=\"sztylet.png\""));
        assertTrue(response.body().contains("name=\"slot\""));
        assertFalse(response.body().contains("<th>Pole</th>"));
        assertFalse(response.body().contains("<th>Sugerowana wartość</th>"));
        assertFalse(response.body().contains("<th>Pewność</th>"));
        assertFalse(response.body().contains("<th>Uwagi</th>"));
        assertTrue(response.body().contains("Ręczna weryfikacja affixów"));
        assertTrue(response.body().contains("Typ itemu"));
        assertTrue(response.body().contains("Slot ekwipunku"));
        assertTrue(response.body().contains("Aspekt"));
        assertTrue(response.body().contains("Dodaj affix"));
        assertTrue(response.body().contains("name=\"newAffixType\""));
        assertTrue(response.body().contains("name=\"newAffixValue\""));
        assertTrue(response.body().contains("type=\"button\" id=\"addAffixButton\""));
        assertTrue(response.body().contains("<noscript>"));
        assertTrue(response.body().contains("name=\"formAction\" value=\"addAffix\""));
        assertTrue(response.body().contains("name=\"formAction\" value=\"confirmItem\""));
        assertFalse(response.body().contains("Projekcja do aktualnego runtime"));
    }

    @Test
    void shouldRenderSingleAssembledOpenCvPreviewForTwoUploadedImages() throws Exception {
        createHero("Importer", "13");
        BufferedImage panel = patternedPanel(36, 70, 201);

        HttpResponse<String> response = sendMultipartImagePost("/importuj-item-ze-screena", List.of(
                new MultipartUpload("top.png", "image/png", png(screenshotFromPanel(panel, 0, 45, 36, 0))),
                new MultipartUpload("bottom.png", "image/png", png(screenshotFromPanel(panel, 30, 40, 36, 0)))
        ));

        assertEquals(200, response.statusCode());
        String previewSection = sectionByH2(response.body(), "Podgląd obrazu przygotowanego przez OpenCV");
        assertEquals(1, countOccurrences(previewSection, "data-opencv-assembled-preview"));
        assertTrue(previewSection.contains("Liczba obrazów źródłowych"));
        assertTrue(previewSection.contains("<div class=\"summary-value\">2"));
        assertTrue(previewSection.contains("Szerokość finalnego obrazu"));
        assertTrue(previewSection.contains("<div class=\"summary-value\">36"));
        assertTrue(previewSection.contains("Wysokość finalnego obrazu"));
        assertTrue(previewSection.contains("<div class=\"summary-value\">70"));
        assertTrue(previewSection.contains("Format"));
        assertTrue(previewSection.contains("PNG"));
        assertTrue(previewSection.contains("Status składania"));
        assertTrue(previewSection.contains("STITCHED"));
        assertTrue(previewSection.contains("compositionPolicy"));
        assertTrue(previewSection.contains(OpenCvItemImageAssembler.COMPOSITION_POLICY));
        assertTrue(previewSection.contains("Diagnostyka stitching"));
        assertTrue(previewSection.contains("1 -> 2"));
    }

    @Test
    void shouldKeepInputOrderInRenderedOpenCvPreviewImage() throws Exception {
        createHero("Importer", "13");
        BufferedImage panel = patternedPanel(40, 100, 211);

        HttpResponse<String> response = sendMultipartImagePost("/importuj-item-ze-screena", List.of(
                new MultipartUpload("first.png", "image/png", png(screenshotFromPanel(panel, 0, 60, 40, 0))),
                new MultipartUpload("second.png", "image/png", png(screenshotFromPanel(panel, 40, 60, 40, 0)))
        ));

        assertEquals(200, response.statusCode());
        BufferedImage preview = decodeOpenCvPreview(response.body());

        assertEquals(40, preview.getWidth());
        assertEquals(100, preview.getHeight());
        assertColor(preview, 1, 1, patternedColor(1, 1, 211));
        assertColor(preview, 1, 99, patternedColor(1, 99, 211));
    }

    @Test
    void shouldRenderOpenCvNoOverlapErrorInsteadOfFakeMultiPreview() throws Exception {
        createHero("Importer", "13");

        HttpResponse<String> response = sendMultipartImagePost("/importuj-item-ze-screena", List.of(
                new MultipartUpload("first.png", "image/png", png(patternedPanel(36, 40, 301))),
                new MultipartUpload("second.png", "image/png", png(patternedPanel(36, 40, 777)))
        ));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Błąd podglądu OpenCV"));
        assertTrue(response.body().contains("STITCH_NOT_CONFIDENT"));
        assertFalse(response.body().contains("data-opencv-assembled-preview"));
    }

    @Test
    void shouldRenderRealFixtureAsOpenCvPreviewDataUri() throws Exception {
        createHero("Importer", "13");
        byte[] fixture = Files.readAllBytes(Path.of("src/test/resources/items/verathiel-miecz-v2.png"));

        HttpResponse<String> response = sendMultipartImagePost(
                "/importuj-item-ze-screena",
                "verathiel-miecz-v2.png",
                "image/png",
                fixture
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Podgląd obrazu przygotowanego przez OpenCV"));
        assertTrue(response.body().contains("src=\"data:image/png;base64,"));
        BufferedImage preview = decodeOpenCvPreview(response.body());
        assertTrue(preview.getWidth() > 0);
        assertTrue(preview.getHeight() > 0);
    }

    @Test
    void shouldSeparateOpenCvPreviewErrorFromLegacyImportErrorForCorruptedImage() throws Exception {
        createHero("Importer", "13");

        HttpResponse<String> response = sendMultipartImagePost(
                "/importuj-item-ze-screena",
                "uszkodzony.bin",
                "application/octet-stream",
                new byte[]{1, 2, 3, 4}
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Błąd podglądu OpenCV"));
        assertTrue(response.body().contains("Nie udalo sie zdekodowac obrazu itemu #1 przez OpenCV."));
        assertTrue(response.body().contains("Błędy formularza"));
        assertTrue(response.body().contains("Wgrany plik nie jest obsługiwanym obrazem itemu."));
        assertFalse(response.body().contains("data-opencv-assembled-preview"));
    }

    @Test
    void shouldPassOriginalUploadedRequestsToLegacyAnalyzerInsteadOfAssembledPreview() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-import-opencv-controller");
        HeroService heroService = new HeroService(new FileHeroProfileRepository(tempDirectory));
        heroService.createHero("Importer", HeroClass.PALADIN, 13);
        ItemLibraryService itemLibraryService = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        AtomicReference<List<ItemImageImportRequest>> capturedRequests = new AtomicReference<>();
        byte[] firstImage = png(5, 3, Color.RED);
        byte[] secondImage = png(7, 4, Color.BLUE);
        byte[] fakePreviewImage = png(2, 2, Color.GREEN);

        ItemImportController controller = new ItemImportController(
                requests -> {
                    capturedRequests.set(List.copyOf(requests));
                    return parseResultForOriginalRequests(requests);
                },
                new ItemImportPageRenderer(),
                new ItemImportEditableFormFactory(),
                new ItemImportFormMapper(),
                new ValidatedImportedItemToItemMapper(),
                new ImportedItemCurrentBuildContributionMapper(),
                new FullItemReadAffixUpdater(),
                itemLibraryService,
                null,
                heroService,
                requests -> new AssembledItemImage(
                        fakePreviewImage,
                        "PNG",
                        2,
                        2,
                        requests.size(),
                        "TEST_PREVIEW_POLICY"
                ),
                new AssembledItemImageDebugWriter()
        );
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/importuj-item-ze-screena", controller);
        server.start();
        try {
            String localBaseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

            HttpResponse<String> response = sendMultipartImagePost(localBaseUrl, "/importuj-item-ze-screena", List.of(
                    new MultipartUpload("oryginal-a.png", "image/png", firstImage),
                    new MultipartUpload("oryginal-b.png", "image/png", secondImage)
            ));

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("TEST_PREVIEW_POLICY"));
            List<ItemImageImportRequest> requests = capturedRequests.get();
            assertEquals(2, requests.size());
            assertEquals("oryginal-a.png", requests.get(0).getOriginalFilename());
            assertEquals("oryginal-b.png", requests.get(1).getOriginalFilename());
            assertArrayEquals(firstImage, requests.get(0).getImageBytes());
            assertArrayEquals(secondImage, requests.get(1).getImageBytes());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldKeepAffixAddAsClientSideEditActionWithoutSavingItem() throws Exception {
        createHero("Importer", "13");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("formAction", "addAffix");
        fields.put("sourceImageName", "tarcza.png");
        fields.put("slot", "OFF_HAND");
        fields.put("weaponDamage", "0");
        fields.put("strength", "0");
        fields.put("intelligence", "0");
        fields.put("thorns", "0");
        fields.put("blockChance", "20.0");
        fields.put("retributionChance", "0");
        fields.put("fullItemRead", FullItemReadFormCodec.encode(new FullItemRead(
                "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU",
                "Starożytna legendarna tarcza",
                "Starożytna legendarna",
                "Moc przedmiotu: 800",
                "Pancerz: 1 131 pkt.",
                List.of(new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 siły [107 - 121]"))
        )));
        fields.put("currentBuildQuery", "");
        fields.put("affixCount", "1");
        fields.put("affixType_0", "STRENGTH");
        fields.put("affixValue_0", "114");
        fields.put("affixOriginalType_0", "STRENGTH");
        fields.put("affixOriginalValue_0", "114");
        fields.put("affixSourceText_0", "+114 siły [107 - 121]");
        fields.put("newAffixType", "THORNS");
        fields.put("newAffixValue", "494");

        HttpResponse<String> response = sendUrlEncodedPost("/importuj-item-ze-screena", fields);

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("Zatwierdzony item zapisany do biblioteki"));
        assertTrue(response.body().contains("Ręczna weryfikacja affixów"));
        assertTrue(response.body().contains("name=\"affixType_1\""));
        assertTrue(response.body().contains("name=\"affixValue_1\" value=\"494\""));
        assertFalse(response.body().contains("Projekcja do aktualnego runtime"));

        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");

        assertEquals(200, libraryResponse.statusCode());
        assertTrue(libraryResponse.body().contains("Biblioteka jest pusta"));
    }

    @Test
    void shouldConfirmImportedItemAutomaticallySaveItToLibraryAndExposeNextActions() throws Exception {
        createHero("Importer", "13");
        String currentBuildQuery = "level=13&questSkillPoints=0&weaponDamage=200&strength=30&intelligence=11&thorns=70&blockChance=10&retributionChance=15&horizonSeconds=10"
                + "&rank_BRANDISH=0&choiceUpgrade_BRANDISH=NONE"
                + "&rank_HOLY_BOLT=0&choiceUpgrade_HOLY_BOLT=NONE"
                + "&rank_CLASH=0&choiceUpgrade_CLASH=NONE"
                + "&rank_ADVANCE=5&baseUpgrade_ADVANCE=true&choiceUpgrade_ADVANCE=RIGHT"
                + "&actionBar1=ADVANCE&actionBar2=NONE&actionBar3=NONE&actionBar4=NONE&actionBar5=NONE&actionBar6=NONE";
        HttpResponse<String> response = sendUrlEncodedPost("/importuj-item-ze-screena", Map.of(
                "sourceImageName", "bulawa.png",
                "slot", "MAIN_HAND",
                "weaponDamage", "321",
                "strength", "55",
                "intelligence", "0",
                "thorns", "90",
                "blockChance", "18",
                "retributionChance", "25",
                "fullItemRead", FullItemReadFormCodec.encode(new FullItemRead(
                        "Młot Importera",
                        "Broń główna",
                        "Legendarny",
                        "800 mocy przedmiotu",
                        "321 obrażeń broni",
                        List.of(
                                new FullItemReadLine(FullItemReadLineType.ITEM_NAME, "Młot Importera"),
                                new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Broń główna"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+55 Strength"),
                                new FullItemReadLine(FullItemReadLineType.ASPECT, "Aspekt testowego impetu")
                        )
                )),
                "currentBuildQuery", currentBuildQuery
        ));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Zatwierdzony item zapisany do biblioteki"));
        assertFalse(response.body().contains("Mapowanie do modelu itemu aplikacji"));
        assertFalse(response.body().contains("Mapowanie do aktualnego modelu buildu"));
        assertTrue(response.body().contains("Plik źródłowy"));
        assertTrue(response.body().contains("Aktywny bohater"));
        assertTrue(response.body().contains("bulawa.png"));
        assertTrue(response.body().contains("Identyfikator biblioteki"));
        assertFalse(response.body().contains("Wkład itemu"));
        assertFalse(response.body().contains("Pełny odczyt zapisany w bibliotece"));
        assertFalse(response.body().contains("Pełny zapis itemu"));
        assertFalse(response.body().contains("Aspekt testowego impetu"));
        assertTrue(response.body().contains("Załóż bohaterowi: Broń"));
        assertTrue(response.body().contains("Przejdź do biblioteki"));
        assertTrue(response.body().contains("Wróć do aktualnego buildu"));
        assertFalse(response.body().contains("Zapisz do biblioteki"));
        assertFalse(response.body().contains("Zastosuj do aktualnego buildu"));
        assertFalse(response.body().contains("Dodaj wkład itemu do aktualnego buildu"));
        assertFalse(response.body().contains("Slot w modelu aplikacji"));
        assertFalse(response.body().contains("Staty modelu itemu"));
        assertFalse(response.body().contains(">MAIN_HAND<"));

        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");

        assertEquals(200, libraryResponse.statusCode());
        assertFalse(libraryResponse.body().contains("Biblioteka jest pusta"));
        assertTrue(libraryResponse.body().contains("Broń główna / bulawa.png"));
        assertTrue(libraryResponse.body().contains("<th>Item</th>"));
        assertTrue(libraryResponse.body().contains("<th>Slot / typ</th>"));
        assertTrue(libraryResponse.body().contains("class=\"icon-action assign-action\" title=\"Załóż bohaterowi: Broń\""));
        assertTrue(libraryResponse.body().contains("aria-label=\"Załóż item Broń główna / bulawa.png bohaterowi w slocie Broń\""));
        assertFalse(libraryResponse.body().contains(">Załóż bohaterowi: Broń</button>"));
        assertTrue(libraryResponse.body().contains("class=\"item-name item-details-link\""));
        assertTrue(libraryResponse.body().contains("class=\"item-details-modal\""));
        assertTrue(libraryResponse.body().contains("Młot Importera"));
        assertFalse(libraryResponse.body().contains("Aspekt testowego impetu"));
    }

    @Test
    void shouldNotRenderFullItemReadDumpInImportConfirmation() throws Exception {
        createHero("Importer", "13");
        String fullShieldRead = FullItemReadFormCodec.encode(new FullItemRead(
                "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU",
                "Starożytna legendarna tarcza",
                "Starożytna legendarna tarcza",
                "Moc przedmiotu: 800",
                "Pancerz: 1 131 pkt.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.ITEM_NAME, "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU"),
                        new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Starożytna legendarna tarcza"),
                        new FullItemReadLine(FullItemReadLineType.ITEM_POWER, "Moc przedmiotu: 800"),
                        new FullItemReadLine(FullItemReadLineType.BASE_STAT, "Pancerz: 1 131 pkt."),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "45% redukcji blokowanych obrażeń [45]%"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "20,0% szansy na blok [20,01]%"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+100% obrażeń od broni w głównej ręce [100]%"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 siły [107 - 121]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+494 cierni [473 - 506]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "13,2% redukcji czasu odnowienia"),
                        new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%"),
                        new FullItemReadLine(FullItemReadLineType.SOCKET, "Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek."),
                        new FullItemReadLine(FullItemReadLineType.OTHER, "Rozjuszenie: +8% do szans na trafienie krytyczne za każdą rangę serii zabójstw [8]%"),
                        new FullItemReadLine(FullItemReadLineType.SOCKET, "Puste gniazdo")
                )
        ));

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("sourceImageName", "tarcza.png");
        fields.put("slot", "OFF_HAND");
        fields.put("fullItemRead", fullShieldRead);
        fields.put("currentBuildQuery", "");
        fields.put("affixCount", "4");
        fields.put("affixType_0", "STRENGTH");
        fields.put("affixValue_0", "114");
        fields.put("affixType_1", "THORNS");
        fields.put("affixValue_1", "494");
        fields.put("affixGreater_1", "true");
        fields.put("affixType_2", "LUCKY_HIT_CHANCE");
        fields.put("affixValue_2", "7.0");
        fields.put("affixType_3", "COOLDOWN_REDUCTION");
        fields.put("affixValue_3", "13.2");

        HttpResponse<String> response = sendUrlEncodedPost("/importuj-item-ze-screena", fields);

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("Pełny odczyt zapisany w bibliotece"));
        assertFalse(response.body().contains("Pełny zapis itemu"));
        assertFalse(response.body().contains("Linie bazowe"));
        assertFalse(response.body().contains("45% redukcji blokowanych obrażeń [45]%"));
        assertFalse(response.body().contains("20,0% szansy na blok [20,01]%"));
        assertFalse(response.body().contains("+100% obrażeń od broni w głównej ręce [100]%"));
        assertFalse(response.body().contains("Affixy"));
        assertFalse(response.body().contains("+114 siły"));
        assertFalse(response.body().contains("+494 cierni"));
        assertFalse(response.body().contains("+7% szansy na szczęśliwy traf"));
        assertFalse(response.body().contains("13,2% redukcji czasu odnowienia"));
        assertFalse(response.body().contains("Aspekt / efekt legendarny"));
        assertFalse(response.body().contains("Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%"));
        assertFalse(response.body().contains("Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek."));
        assertFalse(response.body().contains("Dodatkowe / sezonowe linie"));
        assertFalse(response.body().contains("Rozjuszenie: +8%"));
        assertFalse(response.body().contains("Socket / gniazdo"));
        assertFalse(response.body().contains("Puste gniazdo"));
        assertFalse(response.body().contains("Szczegóły techniczne OCR"));
        assertFalse(response.body().contains("Typ linii"));
        assertFalse(response.body().contains("Mapowanie do aktualnego modelu buildu"));
        assertFalse(response.body().contains("Projekcja do aktualnego runtime"));
        assertFalse(response.body().contains(">OFF_HAND<"));
    }

    @Test
    void shouldConfirmItemWithEditedAffixListAndSaveEditedFullReadToLibrary() throws Exception {
        createHero("Importer", "13");
        String fullShieldRead = FullItemReadFormCodec.encode(new FullItemRead(
                "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU",
                "Starożytna legendarna tarcza",
                "Starożytna legendarna",
                "Moc przedmiotu: 800",
                "Pancerz: 1 131 pkt.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.ITEM_NAME, "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU"),
                        new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Starożytna legendarna tarcza"),
                        new FullItemReadLine(FullItemReadLineType.ITEM_POWER, "Moc przedmiotu: 800"),
                        new FullItemReadLine(FullItemReadLineType.BASE_STAT, "Pancerz: 1 131 pkt."),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "45% redukcji blokowanych obrażeń [45]%"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "20,0% szansy na blok [20,01]%"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+100% obrażeń od broni w głównej ręce [100]%"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 siły [107 - 121]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+494 cierni [473 - 506]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "13,2% redukcji czasu odnowienia"),
                        new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%"),
                        new FullItemReadLine(FullItemReadLineType.OTHER, "Rozjuszenie: +8% do szans na trafienie krytyczne za każdą rangę serii zabójstw [8]%"),
                        new FullItemReadLine(FullItemReadLineType.SOCKET, "Puste gniazdo")
                )
        ));
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("sourceImageName", "tarcza.png");
        fields.put("slot", "OFF_HAND");
        fields.put("weaponDamage", "0");
        fields.put("strength", "0");
        fields.put("intelligence", "0");
        fields.put("thorns", "0");
        fields.put("blockChance", "20.0");
        fields.put("retributionChance", "0");
        fields.put("fullItemRead", fullShieldRead);
        fields.put("currentBuildQuery", "");
        fields.put("formAction", "confirmItem");
        fields.put("affixCount", "5");
        fields.put("affixType_0", "STRENGTH");
        fields.put("affixValue_0", "120");
        fields.put("affixOriginalType_0", "STRENGTH");
        fields.put("affixOriginalValue_0", "114");
        fields.put("affixSourceText_0", "+114 siły [107 - 121]");
        fields.put("affixType_1", "THORNS");
        fields.put("affixValue_1", "494");
        fields.put("affixOriginalType_1", "THORNS");
        fields.put("affixOriginalValue_1", "494");
        fields.put("affixSourceText_1", "+494 cierni [473 - 506]");
        fields.put("affixType_2", "LUCKY_HIT_CHANCE");
        fields.put("affixValue_2", "7.0");
        fields.put("affixOriginalType_2", "LUCKY_HIT_CHANCE");
        fields.put("affixOriginalValue_2", "7");
        fields.put("affixSourceText_2", "+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%");
        fields.put("affixRemoved_2", "true");
        fields.put("affixType_3", "COOLDOWN_REDUCTION");
        fields.put("affixValue_3", "13.2");
        fields.put("affixOriginalType_3", "COOLDOWN_REDUCTION");
        fields.put("affixOriginalValue_3", "13.2");
        fields.put("affixSourceText_3", "13,2% redukcji czasu odnowienia");
        fields.put("affixType_4", "INTELLIGENCE");
        fields.put("affixValue_4", "33");
        fields.put("affixOriginalType_4", "INTELLIGENCE");
        fields.put("affixOriginalValue_4", "33");
        fields.put("affixSourceText_4", "");

        HttpResponse<String> response = sendUrlEncodedPost("/importuj-item-ze-screena", fields);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Zatwierdzony item zapisany do biblioteki"));
        assertFalse(response.body().contains("+120 siły"));
        assertFalse(response.body().contains("+114 siły [107 - 121]"));
        assertFalse(response.body().contains("+494 cierni"));
        assertFalse(response.body().contains("13,2% redukcji czasu odnowienia"));
        assertFalse(response.body().contains("+33 inteligencji"));
        assertFalse(response.body().contains("+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%"));
        assertFalse(response.body().contains("Mapowanie do aktualnego modelu buildu"));
        assertFalse(response.body().contains("Projekcja do aktualnego runtime"));

        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");

        assertEquals(200, libraryResponse.statusCode());
        assertTrue(libraryResponse.body().contains("+120 siły"));
        assertFalse(libraryResponse.body().contains("+114 siły [107 - 121]"));
        assertTrue(libraryResponse.body().contains("+494 cierni"));
        assertTrue(libraryResponse.body().contains("+33 inteligencji"));
        assertFalse(libraryResponse.body().contains("+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%"));
    }

    @Test
    void shouldNotRestoreRemovedAffixesFromHiddenFoundationFallbacks() throws Exception {
        createHero("Importer", "13");
        String fullRead = FullItemReadFormCodec.encode(new FullItemRead(
                "Tarcza bez affixów",
                "Tarcza",
                "Legendarny",
                "Moc przedmiotu: 800",
                "Pancerz: 1 131 pkt.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 siły [107 - 121]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+494 cierni [473 - 506]")
                )
        ));
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("sourceImageName", "tarcza.png");
        fields.put("slot", "OFF_HAND");
        fields.put("weaponDamage", "0");
        fields.put("strength", "114");
        fields.put("thorns", "494");
        fields.put("fullItemRead", fullRead);
        fields.put("currentBuildQuery", "");
        fields.put("formAction", "confirmItem");
        fields.put("affixCount", "0");

        HttpResponse<String> response = sendUrlEncodedPost("/importuj-item-ze-screena", fields);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Zatwierdzony item zapisany do biblioteki"));
        assertFalse(response.body().contains("+114 siły"));
        assertFalse(response.body().contains("+494 cierni"));

        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");

        assertEquals(200, libraryResponse.statusCode());
        assertFalse(libraryResponse.body().contains("+114 siły"));
        assertFalse(libraryResponse.body().contains("+494 cierni"));
    }

    @Test
    void shouldRejectInvalidAffixRowsInsteadOfSilentlyDroppingThem() throws Exception {
        createHero("Importer", "13");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("sourceImageName", "tarcza.png");
        fields.put("slot", "OFF_HAND");
        fields.put("fullItemRead", FullItemReadFormCodec.encode(FullItemRead.empty()));
        fields.put("currentBuildQuery", "");
        fields.put("formAction", "confirmItem");
        fields.put("affixCount", "5");
        fields.put("affixType_0", "THORNS");
        fields.put("affixValue_0", "494");
        fields.put("affixType_1", "STRENGTH");
        fields.put("affixValue_1", "");
        fields.put("affixType_2", "");
        fields.put("affixValue_2", "120");
        fields.put("affixType_3", "INTELLIGENCE");
        fields.put("affixValue_3", "abc");
        fields.put("affixType_4", "STRENGTH");
        fields.put("affixValue_4", "-1");

        HttpResponse<String> response = sendUrlEncodedPost("/importuj-item-ze-screena", fields);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Błędy formularza"));
        assertTrue(response.body().contains("Affix #2: wartość affixu jest wymagana"));
        assertTrue(response.body().contains("Affix #3: typ affixu jest wymagany"));
        assertTrue(response.body().contains("Affix #4: affix ma niepoprawny typ albo wartość"));
        assertTrue(response.body().contains("Affix #5"));
        assertTrue(response.body().contains("name=\"affixType_0\""));
        assertTrue(response.body().contains("name=\"affixValue_0\" value=\"494\""));
        assertFalse(response.body().contains("Zatwierdzony item zapisany do biblioteki"));

        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");
        assertTrue(libraryResponse.body().contains("Biblioteka jest pusta"));
    }

    @Test
    void shouldRejectSelectedAspectThatDoesNotMatchChangedSlot() throws Exception {
        createHero("Importer", "13");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("sourceImageName", "buty.png");
        fields.put("slot", "BOOTS");
        fields.put("weaponDamage", "0");
        fields.put("fullItemRead", FullItemReadFormCodec.encode(new FullItemRead(
                "Buty testowe",
                "Buty",
                "Legendarny",
                "Moc przedmiotu: 800",
                "354 pkt. pancerza",
                List.of(new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%"))
        )));
        fields.put("currentBuildQuery", "");
        fields.put("formAction", "confirmItem");
        fields.put("selectedAspectId", "inner-calm");
        fields.put("ocrSuggestedAspectId", "inner-calm");
        fields.put("ocrAspectConfidence", "HIGH");
        fields.put("affixCount", "0");

        HttpResponse<String> response = sendUrlEncodedPost("/importuj-item-ze-screena", fields);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Wybrany aspekt nie pasuje do slotu itemu."));
        assertTrue(response.body().contains("Wybrany aspekt nie pasuje do obecnego slotu itemu"));
        assertFalse(response.body().contains("Zatwierdzony item zapisany do biblioteki"));

        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");
        assertTrue(libraryResponse.body().contains("Biblioteka jest pusta"));
    }

    @Test
    void shouldRenderNeutralBuildWebAppTitlesOnMainPages() throws Exception {
        createHero("Importer", "13");
        HttpResponse<String> currentBuildResponse = sendGet("/policz-aktualny-build");
        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");
        HttpResponse<String> importResponse = sendGet("/importuj-item-ze-screena");
        HttpResponse<String> searchResponse = sendGet("/znajdz-najlepszy-build");

        assertTrue(currentBuildResponse.body().contains("<title>Build WebApp - Policz aktualny build</title>"));
        assertTrue(libraryResponse.body().contains("<title>Build WebApp - Biblioteka itemów</title>"));
        assertTrue(importResponse.body().contains("<title>Build WebApp - Importuj item ze screena</title>"));
        assertTrue(searchResponse.body().contains("<title>Build WebApp - Znajdź najlepszy build</title>"));
        assertFalse(currentBuildResponse.body().contains("Paladin WebApp"));
        assertFalse(libraryResponse.body().contains("Paladin WebApp"));
        assertFalse(importResponse.body().contains("Paladin WebApp"));
        assertFalse(searchResponse.body().contains("Paladin WebApp"));
    }

    private void createHero(String heroName, String heroLevel) throws Exception {
        HttpResponse<String> response = sendUrlEncodedPost("/bohaterowie", Map.of(
                "action", "createHero",
                "heroName", heroName,
                "heroClass", "PALADIN",
                "heroLevel", heroLevel
        ));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Utworzono bohatera " + heroName + "."));
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendUrlEncodedPost(String path, Map<String, String> fields) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(toFormBody(fields), StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendMultipartImagePost(String path,
                                                        String fileName,
                                                        String contentType,
                                                        byte[] fileContent) throws Exception {
        return sendMultipartImagePost(path, List.of(new MultipartUpload(fileName, contentType, fileContent)));
    }

    private HttpResponse<String> sendMultipartImagePost(String path, List<MultipartUpload> files) throws Exception {
        return sendMultipartImagePost(baseUrl, path, files);
    }

    private HttpResponse<String> sendMultipartImagePost(String targetBaseUrl,
                                                        String path,
                                                        List<MultipartUpload> files) throws Exception {
        String boundary = "----CodexItemImportBoundary";
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
        for (MultipartUpload file : files) {
            bodyStream.write((
                    "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"itemImage\"; filename=\"" + file.fileName() + "\"\r\n"
                            + "Content-Type: " + file.contentType() + "\r\n\r\n"
            ).getBytes(StandardCharsets.ISO_8859_1));
            bodyStream.write(file.content());
            bodyStream.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
        }
        bodyStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetBaseUrl + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyStream.toByteArray()))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static byte[] png(int width, int height, Color color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private static BufferedImage patternedPanel(int width, int height, int seed) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, patternedColor(x, y, seed).getRGB());
            }
        }
        return image;
    }

    private static Color patternedColor(int x, int y, int seed) {
        int hash = seed * 1_000_003 + y * 91_913 + x * 31_337 + x * y * 701;
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        int red = 45 + Math.floorMod(hash, 180);
        int green = 45 + Math.floorMod(hash >>> 8, 180);
        int blue = 45 + Math.floorMod(hash >>> 16, 180);
        return new Color(red, green, blue);
    }

    private static BufferedImage screenshotFromPanel(BufferedImage panel,
                                                     int viewportY,
                                                     int viewportHeight,
                                                     int screenshotWidth,
                                                     int panelX) {
        BufferedImage image = new BufferedImage(screenshotWidth, viewportHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, screenshotWidth, viewportHeight);
        graphics.drawImage(panel, panelX, -viewportY, null);
        graphics.dispose();
        return image;
    }

    private static BufferedImage decodeOpenCvPreview(String html) throws IOException {
        String dataUri = openCvPreviewDataUri(html);
        String prefix = "data:image/png;base64,";
        assertTrue(dataUri.startsWith(prefix));
        byte[] imageBytes = Base64.getDecoder().decode(dataUri.substring(prefix.length()));
        return ImageIO.read(new ByteArrayInputStream(imageBytes));
    }

    private static String openCvPreviewDataUri(String html) {
        String marker = "data-opencv-assembled-preview";
        int markerIndex = html.indexOf(marker);
        if (markerIndex < 0) {
            throw new AssertionError("Brak podglądu OpenCV w HTML.");
        }
        int srcStart = html.lastIndexOf("src=\"", markerIndex);
        if (srcStart < 0) {
            throw new AssertionError("Brak atrybutu src podglądu OpenCV.");
        }
        srcStart += "src=\"".length();
        int srcEnd = html.indexOf("\"", srcStart);
        if (srcEnd < 0) {
            throw new AssertionError("Nie udało się wyciąć data URI podglądu OpenCV.");
        }
        return html.substring(srcStart, srcEnd);
    }

    private static void assertColor(BufferedImage image, int x, int y, Color expected) {
        Color actual = new Color(image.getRGB(x, y));
        assertEquals(expected.getRed(), actual.getRed(), "red");
        assertEquals(expected.getGreen(), actual.getGreen(), "green");
        assertEquals(expected.getBlue(), actual.getBlue(), "blue");
    }

    private static String sectionByH2(String html, String heading) {
        String marker = "<h2>" + heading + "</h2>";
        int headingIndex = html.indexOf(marker);
        if (headingIndex < 0) {
            throw new AssertionError("Brak sekcji: " + heading);
        }
        int start = html.lastIndexOf("<section", headingIndex);
        int end = html.indexOf("</section>", headingIndex);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć sekcji: " + heading);
        }
        return html.substring(start, end + "</section>".length());
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = value.indexOf(needle);
        while (index >= 0) {
            count++;
            index = value.indexOf(needle, index + needle.length());
        }
        return count;
    }

    private static ItemImageImportCandidateParseResult parseResultForOriginalRequests(List<ItemImageImportRequest> requests) {
        String fileNames = requests.stream()
                .map(ItemImageImportRequest::getOriginalFilename)
                .reduce("", (left, right) -> left.isBlank() ? right : left + ", " + right);
        return new ItemImageImportCandidateParseResult(
                new ItemImageMetadata(fileNames, "image/png", "PNG", 5, 7),
                new FullItemRead(
                        "Testowy item",
                        "Tarcza",
                        "Legendarny",
                        "800 mocy przedmiotu",
                        "1 000 pkt. pancerza",
                        List.of()
                ),
                new ItemImportFieldCandidate<>("OFF_HAND", EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.HIGH, "test"),
                ItemImportFieldCandidate.unknown("weapon"),
                ItemImportFieldCandidate.unknown("strength"),
                ItemImportFieldCandidate.unknown("intelligence"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution"),
                "Import testowy"
        );
    }

    private static String toFormBody(Map<String, String> formFields) {
        StringJoiner body = new StringJoiner("&");
        for (Map.Entry<String, String> entry : formFields.entrySet()) {
            body.add(encode(entry.getKey()) + "=" + encode(entry.getValue()));
        }
        return body.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record MultipartUpload(String fileName, String contentType, byte[] content) {
    }

}
