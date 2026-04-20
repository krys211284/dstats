package krys.web;

import krys.itemlibrary.ItemLibraryDataDirectoryResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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

/** Testuje integrację domyślnego web servera z resolverem katalogu danych biblioteki itemów. */
class CurrentBuildWebServerStorageIntegrationTest {
    private final String originalDataDirectoryProperty = System.getProperty(ItemLibraryDataDirectoryResolver.DATA_DIRECTORY_PROPERTY);
    private CurrentBuildWebServer webServer;

    @AfterEach
    void tearDown() {
        if (webServer != null) {
            webServer.close();
        }
        restoreDataDirectoryProperty();
    }

    @Test
    void shouldUseDstatsDataDirSystemPropertyForDefaultWebServerStorage() throws Exception {
        Path overrideDirectory = Files.createTempDirectory("dstats-web-storage");
        System.setProperty(ItemLibraryDataDirectoryResolver.DATA_DIRECTORY_PROPERTY, overrideDirectory.toString());

        webServer = new CurrentBuildWebServer(0);
        webServer.start();

        HttpClient httpClient = HttpClient.newHttpClient();
        String baseUrl = "http://127.0.0.1:" + webServer.getPort();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/biblioteka-itemow"))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(toFormBody(Map.of(
                        "action", "saveImportedItem",
                        "sourceImageName", "override-storage.png",
                        "slot", "OFF_HAND",
                        "weaponDamage", "0",
                        "strength", "114",
                        "intelligence", "0",
                        "thorns", "494",
                        "blockChance", "20",
                        "retributionChance", "0",
                        "currentBuildQuery", buildCurrentBuildQuery()
                )), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Zapisano item w bibliotece"));
        assertTrue(Files.isRegularFile(overrideDirectory.resolve("saved-items.db")));
    }

    private void restoreDataDirectoryProperty() {
        if (originalDataDirectoryProperty == null) {
            System.clearProperty(ItemLibraryDataDirectoryResolver.DATA_DIRECTORY_PROPERTY);
            return;
        }
        System.setProperty(ItemLibraryDataDirectoryResolver.DATA_DIRECTORY_PROPERTY, originalDataDirectoryProperty);
    }

    private static String buildCurrentBuildQuery() {
        return "level=13&weaponDamage=8&strength=18&intelligence=0&thorns=50&blockChance=50&retributionChance=50&horizonSeconds=10"
                + "&rank_BRANDISH=0&choiceUpgrade_BRANDISH=NONE"
                + "&rank_HOLY_BOLT=0&choiceUpgrade_HOLY_BOLT=NONE"
                + "&rank_CLASH=0&choiceUpgrade_CLASH=NONE"
                + "&rank_ADVANCE=5&baseUpgrade_ADVANCE=true&choiceUpgrade_ADVANCE=RIGHT"
                + "&actionBar1=ADVANCE&actionBar2=NONE&actionBar3=NONE&actionBar4=NONE";
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
