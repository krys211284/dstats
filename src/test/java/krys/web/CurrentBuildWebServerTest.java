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
import java.util.Map;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy GUI pokrywają endpoint formularza i podstawowy render wyniku bez powielania logiki runtime. */
class CurrentBuildWebServerTest {
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
    void shouldRenderFormForCurrentBuildPage() throws Exception {
        HttpResponse<String> response = sendGet("/policz-aktualny-build");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Policz aktualny build"));
        assertTrue(response.body().contains("Konfiguracja foundation manual simulation"));
        assertTrue(response.body().contains("name=\"skillId\""));
        assertTrue(response.body().contains("name=\"rank\""));
        assertTrue(response.body().contains("name=\"choiceUpgrade\""));
        assertTrue(response.body().contains("name=\"horizonSeconds\""));
    }

    @Test
    void shouldCalculateCurrentBuildAndRenderRequiredSections() throws Exception {
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                Map.of(
                        "skillId", "HOLY_BOLT",
                        "rank", "5",
                        "baseUpgrade", "true",
                        "choiceUpgrade", "NONE",
                        "horizonSeconds", "60"
                )
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Total damage"));
        assertTrue(response.body().contains(">1732<"));
        assertTrue(response.body().contains("DPS"));
        assertTrue(response.body().contains("Debug bezpośrednich hitów"));
        assertTrue(response.body().contains("Delayed hit debug"));
        assertTrue(response.body().contains("Reactive debug"));
        assertTrue(response.body().contains("Reactive contribution"));
        assertTrue(response.body().contains(">800<"));
        assertTrue(response.body().contains(">52<"));
        assertTrue(response.body().contains(">32<"));
        assertTrue(response.body().contains(">13<"));
        assertTrue(response.body().contains(">8<"));
        assertTrue(response.body().contains(">40<"));
        assertTrue(response.body().contains("Step trace"));
        assertTrue(response.body().contains("Judgement"));
        assertTrue(response.body().contains("Holy Bolt"));
    }

    @Test
    void shouldRejectChoiceThatDoesNotBelongToSelectedSkill() throws Exception {
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                Map.of(
                        "skillId", "HOLY_BOLT",
                        "rank", "5",
                        "baseUpgrade", "true",
                        "choiceUpgrade", "LEFT",
                        "horizonSeconds", "60"
                )
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Błędy formularza"));
        assertTrue(response.body().contains("Wybrany dodatkowy modyfikator nie jest dostępny dla wskazanego skilla w aktualnym foundation."));
    }

    @Test
    void shouldRenderClashScenarioWithResolveAndReactiveBonuses() throws Exception {
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                Map.of(
                        "skillId", "CLASH",
                        "rank", "5",
                        "baseUpgrade", "true",
                        "choiceUpgrade", "LEFT",
                        "horizonSeconds", "9"
                )
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Total damage"));
        assertTrue(response.body().contains("Reactive contribution"));
        assertTrue(response.body().contains("264"));
        assertTrue(response.body().contains("Reactive debug"));
        assertTrue(response.body().contains("Resolve aktywny na końcu"));
        assertTrue(response.body().contains("Active block chance na końcu"));
        assertTrue(response.body().contains("Active thorns bonus na końcu"));
        assertTrue(response.body().contains("75.00%"));
        assertTrue(response.body().contains(">50<"));
        assertTrue(response.body().contains(">104<"));
        assertTrue(response.body().contains(">64<"));
        assertTrue(response.body().contains(">39<"));
        assertTrue(response.body().contains(">24<"));
        assertTrue(response.body().contains(">88<"));
        assertTrue(response.body().contains("Clash"));
        assertTrue(response.body().contains("Punishment"));
    }

    @Test
    void shouldRenderAdvanceScenarioWithCooldownAndWait() throws Exception {
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                Map.of(
                        "skillId", "ADVANCE",
                        "rank", "5",
                        "baseUpgrade", "true",
                        "choiceUpgrade", "RIGHT",
                        "horizonSeconds", "10"
                )
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Advance"));
        assertTrue(response.body().contains("Flash of the Blade"));
        assertTrue(response.body().contains("Total damage"));
        assertTrue(response.body().contains("186"));
        assertTrue(response.body().contains("Debug bezpośrednich hitów"));
        assertTrue(response.body().contains(">322<"));
        assertTrue(response.body().contains(">33<"));
        assertTrue(response.body().contains("Step trace"));
        assertTrue(response.body().contains("WAIT"));
        assertTrue(response.body().contains("cooldown=true"));
        assertTrue(response.body().contains("cooldownRemaining=7"));
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendPost(String path, Map<String, String> formFields) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(toFormBody(formFields), StandardCharsets.UTF_8))
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
