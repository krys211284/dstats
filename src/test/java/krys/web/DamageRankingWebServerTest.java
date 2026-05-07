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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRankingWebServerTest {
    private static final Map<String, List<Integer>> SIMPLE_SINGLE_COMPONENT_R1_TREE_MAX = Map.ofEntries(
            Map.entry("wymach", List.of(75, 191)),
            Map.entry("swiety_pocisk", List.of(90, 229)),
            Map.entry("starcie", List.of(115, 293)),
            Map.entry("natarcie", List.of(105, 268)),
            Map.entry("blogoslawiona_tarcza", List.of(205, 523)),
            Map.entry("blogoslawiony_mlot", List.of(115, 293)),
            Map.entry("boska_lanca", List.of(90, 229)),
            Map.entry("uderzenie_tarcza", List.of(205, 523)),
            Map.entry("szarza_z_tarcza", List.of(90, 229)),
            Map.entry("skazanie", List.of(240, 612)),
            Map.entry("konsekracja", List.of(75, 191))
    );
    private static final Set<String> SKILLS_WITHOUT_SIMPLE_DAMAGE_TABLE = Set.of(
            "zapal",
            "aura_fanatyzmu",
            "aura_smialosci",
            "aura_swietej_swiatlosci",
            "egida",
            "spadajaca_gwiazda",
            "mobilizacja",
            "wlocznia_niebios",
            "oczyszczenie",
            "furia_niebios",
            "forteca",
            "zenit",
            "arbiter_sprawiedliwosci"
    );

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
        assertTrue(response.body().contains("<main class=\"layout wide-page ranking-page\">"));
        assertTrue(response.body().contains(".wide-page.ranking-page"));
        assertTrue(response.body().contains("width: calc(100vw - 32px)"));
        assertTrue(response.body().contains("max-width: none"));
        assertTrue(response.body().contains("overflow-x: auto"));
        assertTrue(response.body().contains("<th>skillName</th>"));
        assertTrue(response.body().contains("<th>skillGroup</th>"));
        assertTrue(response.body().contains("<th>type</th>"));
        assertTrue(response.body().contains("<th>verificationStatus</th>"));
        assertTrue(response.body().contains("<th>Obrażenia % R1</th>"));
        assertTrue(response.body().contains("<th>Obrażenia % max drzewo</th>"));
        assertTrue(response.body().contains("<th>grupa_1: wpływ na obrażenia</th>"));
        assertTrue(response.body().contains("<th>grupa_2: wpływ na obrażenia</th>"));
        assertTrue(response.body().contains("<th>grupa_3: wpływ na obrażenia</th>"));
        assertFalse(response.body().contains("<th>skillId</th>"));
        assertFalse(response.body().contains("<th>Komponenty obrażeń</th>"));
        assertFalse(response.body().contains("<th>Ulepszenia wpływające na obrażenia</th>"));
        assertFalse(response.body().contains("<th>Ulepszenia bez wpływu na obrażenia</th>"));
        assertFalse(response.body().contains("<th>damagePerUse</th>"));
        assertFalse(response.body().contains("<th>theoreticalDps</th>"));
        assertFalse(response.body().contains("<th>singleTargetDps</th>"));
        assertFalse(response.body().contains("<th>reason / notes</th>"));
        assertFalse(response.body().contains("<th>sourcePdf</th>"));
        assertTrue(response.body().contains("<option value=\"BASE_DAMAGE_PERCENT_RANK_1\""));
        assertTrue(response.body().contains("<option value=\"BASE_DAMAGE_PERCENT_TREE_MAX\" selected>"));
        assertFalse(response.body().contains("DAMAGE_PER_USE"));
        assertFalse(response.body().contains("THEORETICAL_DPS"));
        assertFalse(response.body().contains("SINGLE_TARGET_DPS"));
        assertEquals(24, countSkillRows(response.body()));
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
        assertTrue(response.body().contains("data-skill-id=\"zenit\""));
        assertTrue(response.body().contains("data-skill-id=\"forteca\""));
    }

    @Test
    void baseDamagePercentValuesShouldRenderForImportedSimpleSingleComponentSkillsOnly() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin");

        assertEquals(200, response.statusCode());
        assertEquals(24, countSkillRows(response.body()));
        assertEquals(26, countOccurrences(response.body(), "brak danych"));
        assertTrue(response.body().contains("data-skill-id=\"blogoslawiony_mlot\""));
        assertTrue(response.body().contains(">115%</td>"));
        assertTrue(response.body().contains(">293%</td>"));
        assertTrue(response.body().contains("data-skill-id=\"wymach\""));
        assertTrue(response.body().contains(">75%</td>"));
        assertTrue(response.body().contains(">191%</td>"));
        assertTrue(response.body().contains("data-skill-id=\"skazanie\""));
        assertTrue(response.body().contains(">240%</td>"));
        assertTrue(response.body().contains(">612%</td>"));
        assertTrue(response.body().contains("data-skill-id=\"blogoslawiona_tarcza\""));
        assertTrue(response.body().contains(">205%</td>"));
        assertTrue(response.body().contains(">523%</td>"));
        assertTrue(allRowsContainExpectedBaseDamageCells(response.body()));
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
        assertFalse(response.body().contains("<th>damagePerUse</th>"));
        assertFalse(response.body().contains("<th>theoreticalDps</th>"));
        assertFalse(response.body().contains("<th>singleTargetDps</th>"));
        assertFalse(response.body().contains("zablokowane"));
        assertTrue(response.body().contains("wymaga weryfikacji"));
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
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin&skillGroup=moce_specjalne&verificationStatus=NEEDS_VERIFICATION&type=DAMAGE&metric=BASE_DAMAGE_PERCENT_TREE_MAX");

        assertEquals(200, response.statusCode());
        assertEquals(2, countSkillRows(response.body()));
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
        assertTrue(response.body().contains("data-skill-id=\"arbiter_sprawiedliwosci\""));
        assertFalse(response.body().contains("data-skill-id=\"forteca\""));
        assertTrue(response.body().contains("<option value=\"BASE_DAMAGE_PERCENT_TREE_MAX\" selected>"));
    }

    @Test
    void baseDamagePercentMetricsShouldKeepPaladinTreeAsDefaultSource() throws Exception {
        HttpResponse<String> rankOneMetricResponse = sendGet("/ranking-obrazen?character=paladin&metric=BASE_DAMAGE_PERCENT_RANK_1");
        HttpResponse<String> treeMaxMetricResponse = sendGet("/ranking-obrazen?character=paladin&metric=BASE_DAMAGE_PERCENT_TREE_MAX");

        assertEquals(200, rankOneMetricResponse.statusCode());
        assertEquals(24, countSkillRows(rankOneMetricResponse.body()));
        assertTrue(rankOneMetricResponse.body().contains("<option value=\"BASE_DAMAGE_PERCENT_RANK_1\" selected>"));
        assertEquals("skazanie", firstSkillId(rankOneMetricResponse.body()));
        assertFalse(rankOneMetricResponse.body().contains("data-skill-id=\"BRANDISH\""));
        assertFalse(rankOneMetricResponse.body().contains("data-skill-id=\"HOLY_BOLT\""));
        assertFalse(rankOneMetricResponse.body().contains("data-skill-id=\"CLASH\""));
        assertFalse(rankOneMetricResponse.body().contains("data-skill-id=\"ADVANCE\""));

        assertEquals(200, treeMaxMetricResponse.statusCode());
        assertEquals(24, countSkillRows(treeMaxMetricResponse.body()));
        assertTrue(treeMaxMetricResponse.body().contains("<option value=\"BASE_DAMAGE_PERCENT_TREE_MAX\" selected>"));
        assertEquals("skazanie", firstSkillId(treeMaxMetricResponse.body()));
        assertFalse(treeMaxMetricResponse.body().contains("data-skill-id=\"BRANDISH\""));
        assertFalse(treeMaxMetricResponse.body().contains("data-skill-id=\"HOLY_BOLT\""));
        assertFalse(treeMaxMetricResponse.body().contains("data-skill-id=\"CLASH\""));
        assertFalse(treeMaxMetricResponse.body().contains("data-skill-id=\"ADVANCE\""));
    }

    @Test
    void runtimeMetricQueryShouldFallBackToBaseMetricInMainRankingUi() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin&metric=SINGLE_TARGET_DPS");

        assertEquals(200, response.statusCode());
        assertEquals(24, countSkillRows(response.body()));
        assertTrue(response.body().contains("<option value=\"BASE_DAMAGE_PERCENT_TREE_MAX\" selected>"));
        assertFalse(response.body().contains("<option value=\"SINGLE_TARGET_DPS\""));
        assertEquals("skazanie", firstSkillId(response.body()));
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

    private static boolean allRowsContainExpectedBaseDamageCells(String html) {
        Matcher matcher = Pattern.compile("(?s)<tr class=\"damage-ranking-row\"[^>]*data-skill-id=\"([^\"]+)\"[^>]*>.*?</td><td>.*?</td><td>.*?</td><td>.*?</td><td>(.*?)</td><td>(.*?)</td><td>.*?</td>")
                .matcher(html);
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String skillId = matcher.group(1);
            String rankOneCell = matcher.group(2);
            String treeMaxCell = matcher.group(3);
            if (SIMPLE_SINGLE_COMPONENT_R1_TREE_MAX.containsKey(skillId)) {
                List<Integer> expected = SIMPLE_SINGLE_COMPONENT_R1_TREE_MAX.get(skillId);
                if (!rankOneCell.contains(expected.get(0) + "%") || !treeMaxCell.contains(expected.get(1) + "%")) {
                    return false;
                }
            } else if (SKILLS_WITHOUT_SIMPLE_DAMAGE_TABLE.contains(skillId)) {
                if (!rankOneCell.contains("brak danych") || !treeMaxCell.contains("brak danych")) {
                    return false;
                }
            } else {
                return false;
            }
            if (rankOneCell.contains("zablokowane") || treeMaxCell.contains("zablokowane")) {
                return false;
            }
        }
        return found;
    }

    private static String firstSkillId(String html) {
        Matcher matcher = Pattern.compile("data-skill-row=\"true\" data-skill-id=\"([^\"]+)\"").matcher(html);
        if (!matcher.find()) {
            throw new AssertionError("Brak wierszy rankingu.");
        }
        return matcher.group(1);
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
