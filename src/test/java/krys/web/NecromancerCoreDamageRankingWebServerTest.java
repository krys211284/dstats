package krys.web;

import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.paladin.PaladinSkillTreeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerCoreDamageRankingWebServerTest {
    private static final Set<String> CORE_IDS = Set.of("wlocznia_z_kosci", "mor", "rozdarcie", "naplyw_krwi", "krwawa_lanca", "szkieletowy_mag");
    private CurrentBuildWebServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        server = new CurrentBuildWebServer(0, Files.createTempDirectory("necromancer-core-ranking"));
        server.start();
        client = HttpClient.newHttpClient();
        baseUrl = "http://127.0.0.1:" + server.getPort();
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.close();
    }

    @Test
    void shouldRenderTheExtendedNecromancerCatalogAndFilterBothCoreGroupAndCategory() throws Exception {
        String page = get("");
        assertEquals(23, ids(page).size());
        assertEquals(4, Pattern.compile("data-skill-group=\"basic\"").matcher(page).results().count());
        assertEquals(6, Pattern.compile("data-skill-group=\"core\"").matcher(page).results().count());
        assertTrue(page.contains("<option value=\"core\">Główne / Core</option>"));
        assertTrue(page.contains("<option value=\"CORE\">Główne</option>"));
        assertTrue(page.indexOf("<option value=\"basic\"") < page.indexOf("<option value=\"core\""));
        for (var skill : PaladinSkillTreeRegistry.allSkills()) {
            assertFalse(ids(page).contains(skill.getSkillId()));
        }
        for (String filter : List.of("&skillGroup=core", "&sourceCategory=CORE")) {
            var ids = ids(get(filter));
            assertEquals(6, ids.size());
            assertEquals(CORE_IDS, Set.copyOf(ids));
        }
        assertEquals(4, ids(get("&skillGroup=basic")).size());
    }

    @Test
    void shouldRenderSeparateRankComponentsAndExplicitTotalDuration() throws Exception {
        String page = get("&skillGroup=core");
        var blight = row(page, "mor");
        assertTrue(blight.contains("<code>PRIMARY_DAMAGE</code>: 40%"));
        assertTrue(blight.contains("<code>PRIMARY_DAMAGE</code>: 102%"));
        assertTrue(blight.contains("<code>DAMAGE_OVER_DURATION</code>: 210% w ciągu 6 s"));
        assertTrue(blight.contains("<code>DAMAGE_OVER_DURATION</code>: 536% w ciągu 6 s"));
        assertFalse(blight.contains("<code>DAMAGE_PER_SECOND</code>"));
        assertFalse(blight.contains(">250%</td>"));
        var sever = row(page, "rozdarcie");
        assertTrue(sever.contains("<code>FIRST_STRIKE_DAMAGE</code>: 60%"));
        assertTrue(sever.contains("<code>FIRST_STRIKE_DAMAGE</code>: 153%"));
        assertTrue(sever.contains("<code>SECOND_STRIKE_DAMAGE</code>: 100%"));
        assertTrue(sever.contains("<code>SECOND_STRIKE_DAMAGE</code>: 255%"));
        assertFalse(sever.contains(">160%</td>"));
        var surge = row(page, "naplyw_krwi");
        assertTrue(surge.contains("<code>PRIMARY_DAMAGE</code>: 20%"));
        assertTrue(surge.contains("<code>PRIMARY_DAMAGE</code>: 51%"));
        assertTrue(surge.contains("<code>BURST_DAMAGE</code>: 125%"));
        assertTrue(surge.contains("<code>BURST_DAMAGE</code>: 319%"));
        assertFalse(surge.contains(">145%</td>"));
        assertTrue(row(page, "szkieletowy_mag").contains("</td><td>80%</td><td>204%</td>"));
        assertEquals(List.of("mor"), ids(get("&skillGroup=core&q=" + encode("w ciągu 6 s"))));
    }

    @Test
    void shouldFindAllFortyTwoPolishCoreUpgradeNamesThroughTheSharedSearch() throws Exception {
        int count = 0;
        for (var skill : NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills().stream()
                .filter(skill -> skill.getSkillGroup().equals("core")).toList()) {
            assertTrue(ids(get("&q=" + encode(skill.getSkillName()))).contains(skill.getSkillId()));
            for (var upgrade : skill.getUpgradeGroups().stream().flatMap(group -> group.getUpgrades().stream()).toList()) {
                assertTrue(ids(get("&q=" + encode(upgrade.getName()))).contains(skill.getSkillId()), skill.getSkillName() + " / " + upgrade.getName());
                count++;
            }
        }
        assertEquals(42, count);
    }

    @Test
    void shouldKeepBlightLuckyHitConflictVisibleAlongsideDecompose() throws Exception {
        assertEquals(Set.of("mor", "rozklad", "trupie_macki", "wiezienie_z_kosci", "zelazna_dziewica", "armia_umarlych"), Set.copyOf(ids(get("&hasManualReviewUpgrade=YES"))));
        var blight = row(get("&skillGroup=core"), "mor");
        assertTrue(blight.contains("Manual review"));
        assertTrue(blight.contains("+20%"));
        assertTrue(blight.contains("Konflikt źródeł"));
        assertTrue(blight.contains("changelog EN D4Guides 3.2.1"));
        assertTrue(blight.contains("Lucky Hit 40%"));
    }

    private String get(String parameters) throws Exception {
        String path = "/ranking-obrazen?character=necromancer" + parameters;
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), path);
        return response.body();
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    private static List<String> ids(String page) {
        return Pattern.compile("data-skill-id=\"([^\"]+)\"").matcher(page).results().map(match -> match.group(1)).toList();
    }

    private static String row(String page, String id) {
        return Pattern.compile("<tr[^>]*data-skill-id=\"" + id + "\"[\\s\\S]*?</tr>").matcher(page).results()
                .map(match -> match.group()).findFirst().orElseThrow();
    }
}
