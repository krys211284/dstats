package krys.web;

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
import java.util.Map;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje SSR biblioteki itemów: zapis, lista, aktywacja i sekcję active items na current build. */
class ItemLibraryWebServerTest {
    private CurrentBuildWebServer webServer;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        Path tempDirectory = Files.createTempDirectory("item-library-web");
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
    void shouldSaveItemsRenderLibraryAndUseActiveItemOnCurrentBuildPage() throws Exception {
        HttpResponse<String> firstSave = sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", "shield-a.png",
                "slot", "OFF_HAND",
                "weaponDamage", "0",
                "strength", "114",
                "intelligence", "0",
                "thorns", "494",
                "blockChance", "20",
                "retributionChance", "0",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, firstSave.statusCode());
        assertTrue(firstSave.body().contains("Zapisano item w bibliotece"));
        assertTrue(firstSave.body().contains("OFF_HAND / shield-a.png"));

        HttpResponse<String> secondSave = sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", "shield-b.png",
                "slot", "OFF_HAND",
                "weaponDamage", "0",
                "strength", "120",
                "intelligence", "0",
                "thorns", "500",
                "blockChance", "22",
                "retributionChance", "0",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, secondSave.statusCode());
        assertTrue(secondSave.body().contains("Możesz mieć wiele zapisanych itemów tego samego slotu"));
        assertTrue(secondSave.body().contains("OFF_HAND / shield-a.png"));
        assertTrue(secondSave.body().contains("OFF_HAND / shield-b.png"));

        HttpResponse<String> activateResponse = sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "activateItem",
                "itemId", "2",
                "slot", "OFF_HAND",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, activateResponse.statusCode());
        assertTrue(activateResponse.body().contains("Aktywny item dla slotu OFF_HAND został zmieniony."));
        assertTrue(activateResponse.body().contains("Nowy wybór zastępuje poprzedni aktywny item w tym samym slocie."));
        assertTrue(activateResponse.body().contains("class=\"status-badge status-active\">Aktywny</span>"));
        assertTrue(activateResponse.body().contains("Ustaw jako aktywny"));
        assertTrue(activateResponse.body().contains("OFF_HAND / shield-b.png"));

        HttpResponse<String> currentBuildResponse = sendGet("/policz-aktualny-build?" + buildCurrentBuildQuery());
        assertEquals(200, currentBuildResponse.statusCode());
        assertTrue(currentBuildResponse.body().contains("Aktywne itemy z biblioteki"));
        assertTrue(currentBuildResponse.body().contains("OFF_HAND / shield-b.png"));
        assertTrue(currentBuildResponse.body().contains("Efektywne staty do obliczeń"));
        assertTrue(currentBuildResponse.body().contains(">120<"));
        assertTrue(currentBuildResponse.body().contains("Do obliczeń runtime trafiają: weapon damage=200, strength=150"));
    }

    @Test
    void shouldKeepCurrentBuildQueryWhenNavigatingFromLibraryToItemImport() throws Exception {
        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow?" + buildCurrentBuildQuery());

        assertEquals(200, libraryResponse.statusCode());
        assertTrue(libraryResponse.body().contains("href=\"/importuj-item-ze-screena?level=13&amp;weaponDamage=200&amp;strength=30"));

        HttpResponse<String> importResponse = sendGet("/importuj-item-ze-screena?" + buildCurrentBuildQuery());

        assertEquals(200, importResponse.statusCode());
        assertTrue(importResponse.body().contains("action=\"/importuj-item-ze-screena?level=13&amp;weaponDamage=200&amp;strength=30"));
        assertTrue(importResponse.body().contains("blockChance=10&amp;retributionChance=15&amp;horizonSeconds=10"));
    }

    @Test
    void shouldRenderEmptyStateWithImportLinkAndDeleteMessage() throws Exception {
        HttpResponse<String> emptyResponse = sendGet("/biblioteka-itemow?" + buildCurrentBuildQuery());

        assertEquals(200, emptyResponse.statusCode());
        assertTrue(emptyResponse.body().contains("Biblioteka jest pusta"));
        assertTrue(emptyResponse.body().contains("Zaimportuj pierwszy item"));
        assertTrue(emptyResponse.body().contains("href=\"/importuj-item-ze-screena?level=13&amp;weaponDamage=200&amp;strength=30"));

        HttpResponse<String> saveResponse = sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", "empty-state.png",
                "slot", "OFF_HAND",
                "weaponDamage", "0",
                "strength", "10",
                "intelligence", "0",
                "thorns", "0",
                "blockChance", "0",
                "retributionChance", "0",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, saveResponse.statusCode());

        HttpResponse<String> deleteResponse = sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "deleteItem",
                "itemId", "1",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));

        assertEquals(200, deleteResponse.statusCode());
        assertTrue(deleteResponse.body().contains("Usunięto item z biblioteki."));
        assertTrue(deleteResponse.body().contains("Biblioteka jest pusta"));
        assertTrue(deleteResponse.body().contains("Importuj item ze screena"));
    }

    private static String buildCurrentBuildQuery() {
        return "level=13&weaponDamage=200&strength=30&intelligence=11&thorns=70&blockChance=10&retributionChance=15&horizonSeconds=10"
                + "&rank_BRANDISH=0&choiceUpgrade_BRANDISH=NONE"
                + "&rank_HOLY_BOLT=0&choiceUpgrade_HOLY_BOLT=NONE"
                + "&rank_CLASH=0&choiceUpgrade_CLASH=NONE"
                + "&rank_ADVANCE=5&baseUpgrade_ADVANCE=true&choiceUpgrade_ADVANCE=RIGHT"
                + "&actionBar1=ADVANCE&actionBar2=NONE&actionBar3=NONE&actionBar4=NONE";
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
