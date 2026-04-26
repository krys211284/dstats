package krys.web;

import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy GUI flow importu itemu pokrywają upload obrazu, ręczne potwierdzenie i mapowanie do current build. */
class ItemImportWebServerTest {
    private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+bYQ0AAAAASUVORK5CYII="
    );

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
        assertTrue(response.body().contains("To jest import wspomagany pojedynczego itemu ze screena."));
        assertTrue(response.body().contains("Wstępnie rozpoznane pola"));
        assertTrue(response.body().contains("Tu pojawią się rozpoznane pola itemu"));
        assertTrue(response.body().contains("Aktywny bohater importu"));
    }

    @Test
    void shouldExposeCurrentBuildIntegrationButtonWithoutRegressingCurrentBuildFlow() throws Exception {
        createHero("Importer", "13");
        HttpResponse<String> response = sendGet("/policz-aktualny-build");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("formaction=\"/importuj-item-ze-screena\""));
        assertTrue(response.body().contains("Importuj item dla aktywnego bohatera"));
    }

    @Test
    void shouldUploadImageAndRenderManualConfirmationFields() throws Exception {
        createHero("Importer", "13");
        HttpResponse<String> response = sendMultipartImagePost("/importuj-item-ze-screena", "sztylet.png", "image/png", PNG_1X1);

        assertEquals(200, response.statusCode());
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
        assertTrue(response.body().contains("Dodaj affix"));
        assertTrue(response.body().contains("name=\"newAffixType\""));
        assertTrue(response.body().contains("name=\"newAffixValue\""));
        assertTrue(response.body().contains("name=\"formAction\" value=\"addAffix\""));
        assertTrue(response.body().contains("name=\"formAction\" value=\"confirmItem\""));
        assertTrue(response.body().contains("Projekcja do aktualnego runtime"));
    }

    @Test
    void shouldAddAffixWithoutSavingItemAndUpdateRuntimeProjection() throws Exception {
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
        assertTrue(response.body().contains("<div class=\"summary-label\">Siła</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">114</div>"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Kolce</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">494</div>"));

        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");

        assertEquals(200, libraryResponse.statusCode());
        assertTrue(libraryResponse.body().contains("Biblioteka jest pusta"));
    }

    @Test
    void shouldConfirmImportedItemAutomaticallySaveItToLibraryAndExposeNextActions() throws Exception {
        createHero("Importer", "13");
        String currentBuildQuery = "level=13&weaponDamage=200&strength=30&intelligence=11&thorns=70&blockChance=10&retributionChance=15&horizonSeconds=10"
                + "&rank_BRANDISH=0&choiceUpgrade_BRANDISH=NONE"
                + "&rank_HOLY_BOLT=0&choiceUpgrade_HOLY_BOLT=NONE"
                + "&rank_CLASH=0&choiceUpgrade_CLASH=NONE"
                + "&rank_ADVANCE=5&baseUpgrade_ADVANCE=true&choiceUpgrade_ADVANCE=RIGHT"
                + "&actionBar1=ADVANCE&actionBar2=NONE&actionBar3=NONE&actionBar4=NONE";
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
        assertTrue(response.body().contains("Mapowanie do modelu itemu aplikacji"));
        assertTrue(response.body().contains("Mapowanie do aktualnego modelu buildu"));
        assertTrue(response.body().contains("Plik źródłowy"));
        assertTrue(response.body().contains("Aktywny bohater"));
        assertTrue(response.body().contains("bulawa.png"));
        assertTrue(response.body().contains("Identyfikator biblioteki"));
        assertTrue(response.body().contains("Wkład itemu"));
        assertTrue(response.body().contains("Pełny odczyt zapisany w bibliotece"));
        assertTrue(response.body().contains("Młot Importera"));
        assertTrue(response.body().contains("Aspekt testowego impetu"));
        assertTrue(response.body().contains("Załóż bohaterowi: Broń"));
        assertTrue(response.body().contains("Przejdź do biblioteki"));
        assertTrue(response.body().contains("Wróć do aktualnego buildu"));
        assertFalse(response.body().contains("Zapisz do biblioteki"));
        assertFalse(response.body().contains("Zastosuj do aktualnego buildu"));
        assertFalse(response.body().contains("Dodaj wkład itemu do aktualnego buildu"));
        assertTrue(response.body().contains("Slot w modelu aplikacji"));
        assertTrue(response.body().contains("Staty modelu itemu"));
        assertFalse(response.body().contains(">MAIN_HAND<"));

        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");

        assertEquals(200, libraryResponse.statusCode());
        assertFalse(libraryResponse.body().contains("Biblioteka jest pusta"));
        assertTrue(libraryResponse.body().contains("Broń główna / bulawa.png"));
        assertTrue(libraryResponse.body().contains("obr. broni +321"));
        assertTrue(libraryResponse.body().contains("Załóż bohaterowi: Broń"));
        assertTrue(libraryResponse.body().contains("Pełniejszy odczyt itemu"));
        assertTrue(libraryResponse.body().contains("Młot Importera"));
        assertTrue(libraryResponse.body().contains("Aspekt testowego impetu"));
    }

    @Test
    void shouldRenderFullItemReadAsProductPreviewSeparateFromFoundationMapping() throws Exception {
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

        HttpResponse<String> response = sendUrlEncodedPost("/importuj-item-ze-screena", Map.of(
                "sourceImageName", "tarcza.png",
                "slot", "OFF_HAND",
                "weaponDamage", "0",
                "strength", "114",
                "intelligence", "0",
                "thorns", "494",
                "blockChance", "20.0",
                "retributionChance", "0",
                "fullItemRead", fullShieldRead,
                "currentBuildQuery", ""
        ));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Pełny odczyt zapisany w bibliotece"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Nazwa</div>"));
        assertTrue(response.body().contains("NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Typ</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">Tarcza</div>"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Rzadkość</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">Starożytna legendarna</div>"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Moc przedmiotu</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">800</div>"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Pancerz</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">1 131</div>"));
        assertTrue(response.body().contains("Pełny zapis itemu"));
        assertTrue(response.body().contains("Linie bazowe / implicit"));
        assertTrue(response.body().contains("45% redukcji blokowanych obrażeń [45]%"));
        assertTrue(response.body().contains("20,0% szansy na blok [20,01]%"));
        assertTrue(response.body().contains("+100% obrażeń od broni w głównej ręce [100]%"));
        assertTrue(response.body().contains("Affixy"));
        assertTrue(response.body().contains("+114 siły [107 - 121]"));
        assertTrue(response.body().contains("+494 cierni [473 - 506]"));
        assertTrue(response.body().contains("+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%"));
        assertTrue(response.body().contains("13,2% redukcji czasu odnowienia"));
        assertTrue(response.body().contains("Aspekt / efekt legendarny"));
        assertTrue(response.body().contains("Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%"));
        assertTrue(response.body().contains("Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek."));
        assertTrue(response.body().contains("Dodatkowe / sezonowe linie"));
        assertTrue(response.body().contains("Rozjuszenie: +8%"));
        assertTrue(response.body().contains("Socket / gniazdo"));
        assertTrue(response.body().contains("Puste gniazdo"));
        assertTrue(response.body().indexOf("Aspekt / efekt legendarny") < response.body().indexOf("Ta premia jest trzy razy większa"));
        assertTrue(response.body().indexOf("Ta premia jest trzy razy większa") < response.body().indexOf("Socket / gniazdo"));
        assertTrue(response.body().indexOf("Linie bazowe / implicit") < response.body().indexOf("Affixy"));
        assertTrue(response.body().indexOf("Affixy") < response.body().indexOf("Aspekt / efekt legendarny"));
        assertTrue(response.body().indexOf("Aspekt / efekt legendarny") < response.body().indexOf("Dodatkowe / sezonowe linie"));
        assertTrue(response.body().indexOf("Dodatkowe / sezonowe linie") < response.body().indexOf("Socket / gniazdo"));
        assertFalse(response.body().contains("Szczegóły techniczne OCR"));
        assertFalse(response.body().contains("Typ linii"));
        assertTrue(response.body().contains("Mapowanie do aktualnego modelu buildu"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Siła</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">114</div>"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Kolce</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">494</div>"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Szansa bloku [%]</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">20</div>"));
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
        assertTrue(response.body().contains("+120 siły"));
        assertTrue(response.body().contains("+494 cierni [473 - 506]"));
        assertTrue(response.body().contains("13,2% redukcji czasu odnowienia"));
        assertTrue(response.body().contains("+33 inteligencji"));
        assertFalse(response.body().contains("+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Siła</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">120</div>"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Inteligencja</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">33</div>"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Kolce</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">494</div>"));
        assertTrue(response.body().contains("<div class=\"summary-label\">Szansa bloku [%]</div>"));
        assertTrue(response.body().contains("<div class=\"summary-value\">20</div>"));

        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");

        assertEquals(200, libraryResponse.statusCode());
        assertTrue(libraryResponse.body().contains("+120 siły"));
        assertTrue(libraryResponse.body().contains("+33 inteligencji"));
        assertFalse(libraryResponse.body().contains("+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%"));
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
        String boundary = "----CodexItemImportBoundary";
        byte[] headerBytes = (
                "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"itemImage\"; filename=\"" + fileName + "\"\r\n"
                        + "Content-Type: " + contentType + "\r\n\r\n"
        ).getBytes(StandardCharsets.ISO_8859_1);
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1);
        byte[] body = new byte[headerBytes.length + fileContent.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileContent, 0, body, headerBytes.length, fileContent.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileContent.length, footerBytes.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
}
