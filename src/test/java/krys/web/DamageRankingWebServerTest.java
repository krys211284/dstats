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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRankingWebServerTest {
    private CurrentBuildWebServer webServer;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        Path tempDirectory = Files.createTempDirectory("paladin-ranking-web");
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
    void shouldRenderAllTwentyFourPaladinTreeSkillsByDefault() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Ranking obrażeń"));
        assertTrue(response.body().contains("<option value=\"paladin\" selected>Paladyn</option>"));
        assertTrue(response.body().contains("Wpisy w rejestrze"));
        assertTrue(response.body().contains(">24<"));
        assertEquals(24, countSkillRows(response.body()));
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
        assertTrue(response.body().contains("data-skill-id=\"forteca\""));
        assertTrue(response.body().contains("data-skill-id=\"arbiter_sprawiedliwosci\""));
    }

    @Test
    void shouldRenderPaladinRankingForExplicitCharacterParameter() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("PaladinSkillTreeRegistry"));
        assertTrue(response.body().contains("<th>Obrażenia % R1</th>"));
        assertTrue(response.body().contains("<th>Obrażenia % max drzewo</th>"));
        assertEquals(24, countSkillRows(response.body()));
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
        assertTrue(response.body().contains("data-skill-id=\"zenit\""));
        assertTrue(response.body().contains("data-skill-id=\"forteca\""));
    }

    @Test
    void missingBaseDamagePercentValuesShouldRenderAsNoDataNotBlockedOrZero() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin");

        assertEquals(200, response.statusCode());
        assertEquals(24, countSkillRows(response.body()));
        assertEquals(48, countOccurrences(response.body(), "brak danych"));
        assertTrue(allRowsContainMissingBaseDamageCells(response.body()));
        assertFalse(response.body().contains(">0%</td>"));
    }

    @Test
    void legacyPaladinRankingEndpointShouldRenderTheGenericPaladinRankingAlias() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen-paladyna");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Ranking obrażeń"));
        assertTrue(response.body().contains("<form class=\"ranking-filters\" method=\"get\" action=\"/ranking-obrazen\">"));
        assertEquals(24, countSkillRows(response.body()));
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
    }

    @Test
    void shouldNotExposeLegacyFoundationSkillsAsDefaultPaladinTree() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin");

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("data-skill-id=\"BRANDISH\""));
        assertFalse(response.body().contains("data-skill-id=\"HOLY_BOLT\""));
        assertFalse(response.body().contains("data-skill-id=\"CLASH\""));
        assertFalse(response.body().contains("data-skill-id=\"ADVANCE\""));
        assertFalse(response.body().contains(">Brandish<"));
        assertFalse(response.body().contains(">Holy Bolt<"));
        assertFalse(response.body().contains(">Clash<"));
        assertFalse(response.body().contains(">Advance<"));
    }

    @Test
    void needsVerificationSkillsShouldNotHaveCalculatedDpsValues() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin&verificationStatus=NEEDS_VERIFICATION");

        assertEquals(200, response.statusCode());
        assertTrue(countSkillRows(response.body()) > 0);
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
        assertFalse(response.body().contains("data-verification-status=\"NON_DAMAGE\""));
        assertFalse(response.body().contains("data-verification-status=\"SUPPORTED\""));
        assertTrue(allRowsWithStatusContainBlockedDps(response.body(), "NEEDS_VERIFICATION"));
    }

    @Test
    void nonDamageSkillsShouldNotBeTreatedAsDamageSkills() throws Exception {
        HttpResponse<String> nonDamageResponse = sendGet("/ranking-obrazen?character=paladin&verificationStatus=NON_DAMAGE");

        assertEquals(200, nonDamageResponse.statusCode());
        assertEquals(2, countSkillRows(nonDamageResponse.body()));
        assertTrue(nonDamageResponse.body().contains("data-skill-id=\"aura_fanatyzmu\""));
        assertTrue(nonDamageResponse.body().contains("data-skill-id=\"mobilizacja\""));
        assertFalse(nonDamageResponse.body().contains("data-skill-type=\"DAMAGE\""));

        HttpResponse<String> damageTypeResponse = sendGet("/ranking-obrazen?character=paladin&type=DAMAGE");
        assertEquals(200, damageTypeResponse.statusCode());
        assertFalse(damageTypeResponse.body().contains("data-skill-id=\"aura_fanatyzmu\""));
        assertFalse(damageTypeResponse.body().contains("data-skill-id=\"mobilizacja\""));
    }

    @Test
    void shouldFilterBySkillGroupStatusTypeAndMetric() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin&skillGroup=moce_specjalne&verificationStatus=NEEDS_VERIFICATION&type=DAMAGE&metric=DAMAGE_PER_USE");

        assertEquals(200, response.statusCode());
        assertEquals(2, countSkillRows(response.body()));
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
        assertTrue(response.body().contains("data-skill-id=\"arbiter_sprawiedliwosci\""));
        assertFalse(response.body().contains("data-skill-id=\"forteca\""));
        assertTrue(response.body().contains("<option value=\"DAMAGE_PER_USE\" selected>"));
    }

    @Test
    void baseDamagePercentMetricsShouldKeepPaladinTreeAsDefaultSource() throws Exception {
        HttpResponse<String> rankOneMetricResponse = sendGet("/ranking-obrazen?character=paladin&metric=BASE_DAMAGE_PERCENT_RANK_1");
        HttpResponse<String> treeMaxMetricResponse = sendGet("/ranking-obrazen?character=paladin&metric=BASE_DAMAGE_PERCENT_TREE_MAX");

        assertEquals(200, rankOneMetricResponse.statusCode());
        assertEquals(24, countSkillRows(rankOneMetricResponse.body()));
        assertTrue(rankOneMetricResponse.body().contains("<option value=\"BASE_DAMAGE_PERCENT_RANK_1\" selected>"));
        assertFalse(rankOneMetricResponse.body().contains("data-skill-id=\"BRANDISH\""));
        assertFalse(rankOneMetricResponse.body().contains("data-skill-id=\"HOLY_BOLT\""));
        assertFalse(rankOneMetricResponse.body().contains("data-skill-id=\"CLASH\""));
        assertFalse(rankOneMetricResponse.body().contains("data-skill-id=\"ADVANCE\""));

        assertEquals(200, treeMaxMetricResponse.statusCode());
        assertEquals(24, countSkillRows(treeMaxMetricResponse.body()));
        assertTrue(treeMaxMetricResponse.body().contains("<option value=\"BASE_DAMAGE_PERCENT_TREE_MAX\" selected>"));
        assertFalse(treeMaxMetricResponse.body().contains("data-skill-id=\"BRANDISH\""));
        assertFalse(treeMaxMetricResponse.body().contains("data-skill-id=\"HOLY_BOLT\""));
        assertFalse(treeMaxMetricResponse.body().contains("data-skill-id=\"CLASH\""));
        assertFalse(treeMaxMetricResponse.body().contains("data-skill-id=\"ADVANCE\""));
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static int countSkillRows(String html) {
        Matcher matcher = Pattern.compile("data-skill-row=\"true\"").matcher(html);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static boolean allRowsWithStatusContainBlockedDps(String html, String status) {
        Matcher matcher = Pattern.compile("(?s)<tr class=\"damage-ranking-row\"[^>]*data-verification-status=\"" + status + "\"[^>]*>(.*?)</tr>")
                .matcher(html);
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String row = matcher.group(1);
            if (countOccurrences(row, "zablokowane") < 3) {
                return false;
            }
        }
        return found;
    }

    private static boolean allRowsContainMissingBaseDamageCells(String html) {
        Matcher matcher = Pattern.compile("(?s)<tr class=\"damage-ranking-row\"[^>]*>.*?</td><td><code>.*?</code></td><td>.*?</td><td>.*?</td><td>.*?</td><td>(.*?)</td><td>(.*?)</td><td>.*?</td>")
                .matcher(html);
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String rankOneCell = matcher.group(1);
            String treeMaxCell = matcher.group(2);
            if (!rankOneCell.contains("brak danych") || !treeMaxCell.contains("brak danych")) {
                return false;
            }
            if (rankOneCell.contains("zablokowane") || treeMaxCell.contains("zablokowane")) {
                return false;
            }
        }
        return found;
    }

    private static int countOccurrences(String value, String fragment) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(fragment, index)) >= 0) {
            count++;
            index += fragment.length();
        }
        return count;
    }
}
