package krys.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pokrywa app shell, ekran główny, placeholdery i globalną nawigację SSR. */
class AppShellWebServerTest {
    private CurrentBuildWebServer webServer;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        Path tempDirectory = Files.createTempDirectory("app-shell-web");
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
    void shouldRenderHomeHubWithModuleGroupsStatusesAndAvailableSections() throws Exception {
        HttpResponse<String> response = sendGet("/");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<title>Build WebApp - Strona główna</title>"));
        assertTrue(response.body().contains("Strona główna aplikacji"));
        assertTrue(response.body().contains("Narzędzia builda"));
        assertTrue(response.body().contains("Itemy i import"));
        assertTrue(response.body().contains("Systemy dodatku i przyszłe sekcje"));
        assertTrue(response.body().contains("Policz aktualny build"));
        assertTrue(response.body().contains("Znajdź najlepszy build"));
        assertTrue(response.body().contains("Importuj item ze screena"));
        assertTrue(response.body().contains("Biblioteka itemów"));
        assertTrue(response.body().contains("Plany Wojenne"));
        assertTrue(response.body().contains("Medalion"));
        assertTrue(response.body().contains("Kostka Horadrimów"));
        assertTrue(response.body().contains("Filtr łupów"));
        assertTrue(response.body().contains("Drzewka umiejętności 3.0"));
        assertTrue(response.body().contains("System przedmiotów 3.0"));
        assertTrue(response.body().contains("Wieża / rankingi"));
        assertTrue(response.body().contains("Rezonująca Nienawiść"));
        assertTrue(response.body().contains("Wędkarstwo"));
        assertTrue(response.body().contains("Dostępne"));
        assertTrue(response.body().contains("W przygotowaniu"));
        assertTrue(response.body().contains("Po premierze dodatku"));
        assertTrue(response.body().contains("Wymaga dodatku"));
        assertTrue(response.body().contains("Sezonowe"));
    }

    @Test
    void shouldRenderGlobalNavigationOnMainPages() throws Exception {
        assertNavigation(sendGet("/"));
        assertNavigation(sendGet("/policz-aktualny-build"));
        assertNavigation(sendGet("/importuj-item-ze-screena"));
        assertNavigation(sendGet("/biblioteka-itemow"));
        assertNavigation(sendGet("/znajdz-najlepszy-build"));
    }

    @Test
    void shouldRenderPlaceholderPagesWithoutPromisingMechanics() throws Exception {
        HttpResponse<String> medallionResponse = sendGet("/medalion");
        HttpResponse<String> seasonalResponse = sendGet("/plany-wojenne");

        assertEquals(200, medallionResponse.statusCode());
        assertTrue(medallionResponse.body().contains("<title>Build WebApp - Medalion</title>"));
        assertTrue(medallionResponse.body().contains("Placeholder modułu"));
        assertTrue(medallionResponse.body().contains("Medalion"));
        assertTrue(medallionResponse.body().contains("Wymaga dodatku"));
        assertTrue(medallionResponse.body().contains("Szczegółowa logika tej sekcji zostanie doprecyzowana po stabilizacji zasad po premierze dodatku."));
        assertTrue(medallionResponse.body().contains("nie zgaduje zasad dodatku ani sezonu"));

        assertEquals(200, seasonalResponse.statusCode());
        assertTrue(seasonalResponse.body().contains("Plany Wojenne"));
        assertTrue(seasonalResponse.body().contains("Sezonowe"));
        assertTrue(seasonalResponse.body().contains("Placeholder"));
    }

    @Test
    void shouldKeepExistingRoutesReachableAfterAddingAppShell() throws Exception {
        assertEquals(200, sendGet("/").statusCode());
        assertEquals(200, sendGet("/policz-aktualny-build").statusCode());
        assertEquals(200, sendGet("/importuj-item-ze-screena").statusCode());
        assertEquals(200, sendGet("/biblioteka-itemow").statusCode());
        assertEquals(200, sendGet("/znajdz-najlepszy-build").statusCode());
        assertEquals(404, sendGet("/nieznana-sekcja").statusCode());
    }

    private static void assertNavigation(HttpResponse<String> response) {
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("aria-label=\"Główna nawigacja aplikacji\""));
        assertTrue(response.body().contains(">Strona główna</a>"));
        assertTrue(response.body().contains(">Policz aktualny build</a>"));
        assertTrue(response.body().contains(">Znajdź najlepszy build</a>"));
        assertTrue(response.body().contains(">Importuj item ze screena</a>"));
        assertTrue(response.body().contains(">Biblioteka itemów</a>"));
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
