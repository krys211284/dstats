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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje minimalny UI bazy wiedzy i uczenie po zatwierdzonym imporcie. */
class ItemKnowledgeWebServerTest {
    private CurrentBuildWebServer webServer;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        Path tempDirectory = Files.createTempDirectory("item-knowledge-web");
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
    void shouldLearnKnowledgeOnlyAfterConfirmedImportAndAllowEpochReset() throws Exception {
        HttpResponse<String> emptyKnowledge = sendGet("/baza-wiedzy-itemow");

        assertEquals(200, emptyKnowledge.statusCode());
        assertTrue(emptyKnowledge.body().contains("Baza wiedzy itemów"));
        assertTrue(emptyKnowledge.body().contains("Epoka wiedzy 1"));
        assertTrue(emptyKnowledge.body().contains("Brak obserwacji w aktywnej epoce"));

        createHero("Importer", "13");
        HttpResponse<String> importResponse = sendUrlEncodedPost("/importuj-item-ze-screena", confirmedShieldFields());

        assertEquals(200, importResponse.statusCode());
        assertTrue(importResponse.body().contains("Zatwierdzony item zapisany do biblioteki"));

        HttpResponse<String> learnedKnowledge = sendGet("/baza-wiedzy-itemow");

        assertEquals(200, learnedKnowledge.statusCode());
        assertTrue(learnedKnowledge.body().contains("Tarcza"));
        assertTrue(learnedKnowledge.body().contains("Siła"));
        assertTrue(learnedKnowledge.body().contains("Ciernie"));
        assertTrue(learnedKnowledge.body().contains("Szansa na szczęśliwy traf"));
        assertTrue(learnedKnowledge.body().contains("Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%"));
        assertTrue(learnedKnowledge.body().contains("<div class=\"summary-value\">1</div>"));

        HttpResponse<String> resetResponse = sendUrlEncodedPost("/baza-wiedzy-itemow", Map.of(
                "action", "resetKnowledge",
                "epochLabel", "Sezon testowy"
        ));

        assertEquals(200, resetResponse.statusCode());
        assertTrue(resetResponse.body().contains("Rozpoczęto nową epokę wiedzy"));
        assertTrue(resetResponse.body().contains("Sezon testowy"));
        assertTrue(resetResponse.body().contains("Brak obserwacji w aktywnej epoce"));
    }

    private static Map<String, String> confirmedShieldFields() {
        String fullShieldRead = FullItemReadFormCodec.encode(new FullItemRead(
                "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU",
                "Starożytna legendarna tarcza",
                "Starożytna legendarna",
                "Moc przedmiotu: 800",
                "Pancerz: 1 131 pkt.",
                java.util.List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 siły [107 - 121]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+494 cierni [473 - 506]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%"),
                        new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%")
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
        fields.put("affixCount", "3");
        fields.put("affixType_0", "STRENGTH");
        fields.put("affixValue_0", "114");
        fields.put("affixOriginalType_0", "STRENGTH");
        fields.put("affixOriginalValue_0", "114");
        fields.put("affixSourceText_0", "+114 siły [107 - 121]");
        fields.put("affixType_1", "THORNS");
        fields.put("affixValue_1", "494");
        fields.put("affixOriginalType_1", "THORNS");
        fields.put("affixOriginalValue_1", "494");
        fields.put("affixSourceText_1", "+494 cierni [473 - 506]");
        fields.put("affixType_2", "LUCKY_HIT_CHANCE");
        fields.put("affixValue_2", "7");
        fields.put("affixOriginalType_2", "LUCKY_HIT_CHANCE");
        fields.put("affixOriginalValue_2", "7");
        fields.put("affixSourceText_2", "+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%");
        return fields;
    }

    private void createHero(String heroName, String heroLevel) throws Exception {
        HttpResponse<String> response = sendUrlEncodedPost("/bohaterowie", Map.of(
                "action", "createHero",
                "heroName", heroName,
                "heroClass", "PALADIN",
                "heroLevel", heroLevel
        ));
        assertEquals(200, response.statusCode());
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
