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
import java.util.Base64;
import java.util.Map;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        webServer = new CurrentBuildWebServer(0);
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
    void shouldRenderItemImportUploadForm() throws Exception {
        HttpResponse<String> response = sendGet("/importuj-item-ze-screena");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Importuj pojedynczy item ze screena"));
        assertTrue(response.body().contains("name=\"itemImage\""));
        assertTrue(response.body().contains("To jest import wspomagany pojedynczego itemu ze screena."));
    }

    @Test
    void shouldUploadImageAndRenderManualConfirmationFields() throws Exception {
        HttpResponse<String> response = sendMultipartImagePost("/importuj-item-ze-screena", "sztylet.png", "image/png", PNG_1X1);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Wstępnie rozpoznane pola"));
        assertTrue(response.body().contains("sztylet.png"));
        assertTrue(response.body().contains("Rozmiar obrazu"));
        assertTrue(response.body().contains("1 x 1"));
        assertTrue(response.body().contains("Brak pewnego odczytu weapon damage z obrazu."));
        assertTrue(response.body().contains("Ręczne potwierdzenie itemu"));
        assertTrue(response.body().contains("name=\"sourceImageName\" value=\"sztylet.png\""));
        assertTrue(response.body().contains("name=\"slot\""));
    }

    @Test
    void shouldConfirmImportedItemAndExposePrefillForCurrentBuild() throws Exception {
        HttpResponse<String> response = sendUrlEncodedPost("/importuj-item-ze-screena", Map.of(
                "sourceImageName", "bulawa.png",
                "slot", "MAIN_HAND",
                "weaponDamage", "321",
                "strength", "55",
                "intelligence", "0",
                "thorns", "90",
                "blockChance", "18",
                "retributionChance", "25"
        ));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Zatwierdzony item"));
        assertTrue(response.body().contains("Mapowanie do modelu itemu aplikacji"));
        assertTrue(response.body().contains("Mapowanie do aktualnego modelu buildu"));
        assertTrue(response.body().contains("Zaimportowany item: bulawa.png"));
        assertTrue(response.body().contains("/policz-aktualny-build?weaponDamage=321&amp;strength=55&amp;intelligence=0&amp;thorns=90&amp;blockChance=18&amp;retributionChance=25"));

        HttpResponse<String> currentBuildResponse = sendGet(
                "/policz-aktualny-build?weaponDamage=321&strength=55&intelligence=0&thorns=90&blockChance=18&retributionChance=25"
        );
        assertEquals(200, currentBuildResponse.statusCode());
        assertTrue(currentBuildResponse.body().contains("name=\"weaponDamage\" value=\"321\""));
        assertTrue(currentBuildResponse.body().contains("name=\"strength\" value=\"55\""));
        assertTrue(currentBuildResponse.body().contains("name=\"thorns\" value=\"90\""));
        assertTrue(currentBuildResponse.body().contains("name=\"blockChance\" value=\"18\""));
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
