package krys.web;

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
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerCurseDamageRankingWebServerTest {
    private static final Set<String> CURSE_IDS = Set.of("zelazna_dziewica", "zniedoleznienie");
    private CurrentBuildWebServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        server = new CurrentBuildWebServer(0, Files.createTempDirectory("necromancer-curse-ranking"));
        server.start();
        client = HttpClient.newHttpClient();
        baseUrl = "http://127.0.0.1:" + server.getPort();
    }

    @AfterEach
    void tearDown() { client.close(); server.close(); }

    @Test
    void shouldExposeTwentyThreeRowsAndTwoCurseRowsThroughCategoryGroupAndTagFiltersInSourceOrder() throws Exception {
        String page = get("necromancer", "");
        assertEquals(23, ids(page).size());
        for (var entry : Map.of("basic", 4, "core", 6, "corpse", 3, "macabre", 4, "curse", 2, "ultimate", 4).entrySet()) {
            assertEquals(entry.getValue().longValue(), Pattern.compile("data-skill-group=\"" + entry.getKey() + "\"").matcher(page).results().count());
        }
        assertTrue(page.contains("<option value=\"curse\">Klątwy / Curse</option>"));
        assertTrue(page.contains("<option value=\"CURSE\">Klątwy</option>"));
        var groups = List.of("basic", "core", "corpse", "macabre", "curse", "ultimate");
        for (int index = 1; index < groups.size(); index++) {
            assertTrue(page.indexOf("<option value=\"" + groups.get(index - 1) + "\"") < page.indexOf("<option value=\"" + groups.get(index) + "\""));
            assertTrue(page.indexOf("<option value=\"" + groups.get(index - 1).toUpperCase() + "\"") < page.indexOf("<option value=\"" + groups.get(index).toUpperCase() + "\""));
        }
        for (String filter : List.of("&skillGroup=curse", "&sourceCategory=CURSE", "&tag=CURSE")) {
            assertEquals(CURSE_IDS, Set.copyOf(ids(get("necromancer", filter))));
        }
        assertEquals(24, ids(get("paladin", "")).size());
    }

    @Test
    void shouldRenderPerImpulseAndPerEventCoefficientsWithoutDpsOrDurationTotals() throws Exception {
        var maiden = cells(row(get("necromancer", "&skillGroup=curse"), "zelazna_dziewica"));
        assertEquals(16, maiden.size());
        assertTrue(maiden.get(2).contains("<code>PERIODIC_DAMAGE</code>: 30% co 2 s"));
        assertTrue(maiden.get(2).contains("<code>EVENT_TRIGGERED_DAMAGE</code>: 30% na zdarzenie"));
        assertTrue(maiden.get(3).contains("<code>PERIODIC_DAMAGE</code>: 77% co 2 s"));
        assertTrue(maiden.get(3).contains("<code>EVENT_TRIGGERED_DAMAGE</code>: 77% na zdarzenie"));
        assertEquals("10", text(maiden.get(4)));
        assertEquals("-", text(maiden.get(5)));
        assertEquals("5%", text(maiden.get(6)));
        assertEquals("-", text(maiden.get(14)));
        assertEquals("-", text(maiden.get(15)));
        for (String cell : List.of(maiden.get(2), maiden.get(3))) {
            assertFalse(cell.contains("DAMAGE_PER_SECOND"));
            assertFalse(cell.contains("DAMAGE_OVER_DURATION"));
            assertFalse(cell.contains("/ sek."));
            assertFalse(cell.contains("w ciągu 30 s"));
            assertFalse(cell.contains("450%"));
            assertFalse(cell.contains("60%"));
        }
        assertTrue(row(get("necromancer", "&skillGroup=curse"), "zelazna_dziewica").contains("data-mechanic-tags=\"CURSE,PHYSICAL,PROFANE\""));
    }

    @Test
    void shouldRenderDecrepifyWithoutDamageLuckyHitCooldownOrBloodConversionInBaseMetadata() throws Exception {
        var sourceRow = row(get("necromancer", "&skillGroup=curse"), "zniedoleznienie");
        var decrepify = cells(sourceRow);
        assertEquals("nie dotyczy", text(decrepify.get(2)));
        assertEquals("nie dotyczy", text(decrepify.get(3)));
        assertEquals("10", text(decrepify.get(4)));
        assertEquals("-", text(decrepify.get(5)));
        assertEquals("-", text(decrepify.get(6)));
        assertTrue(decrepify.get(6).contains("Brak źródłowej wartości Lucky Hit"));
        assertEquals("-", text(decrepify.get(14)));
        assertEquals("-", text(decrepify.get(15)));
        assertTrue(sourceRow.contains("data-mechanic-tags=\"CURSE,PROFANE\""));
        assertEquals(List.of("zniedoleznienie"), ids(get("necromancer", "&skillGroup=curse&type=SUPPORT")));
    }

    @Test
    void shouldFindCurseNamesSourceDurationAndIntervalsThroughSharedSearch() throws Exception {
        for (String query : List.of("Żelazna Dziewica", "Złośliwa Radość", "Mistrz Małodobry", "Krwawa Pannica")) {
            assertEquals(List.of("zelazna_dziewica"), ids(get("necromancer", "&skillGroup=curse&q=" + encode(query))), query);
        }
        for (String query : List.of("Zniedołężnienie", "Świętokradczy Amok", "Upuszczanie Zdrowia", "Oszałamiająca Klątwa")) {
            assertEquals(List.of("zniedoleznienie"), ids(get("necromancer", "&skillGroup=curse&q=" + encode(query))), query);
        }
        assertEquals(CURSE_IDS, Set.copyOf(ids(get("necromancer", "&skillGroup=curse&q=" + encode("30 s")))));
        assertTrue(ids(get("necromancer", "&skillGroup=curse&q=" + encode("2 s"))).contains("zelazna_dziewica"));
    }

    @Test
    void shouldRenderThornsReviewBurstVariantAndMinionReplacementWithoutChangingBaseCells() throws Exception {
        String page = get("necromancer", "&skillGroup=curse");
        var maiden = row(page, "zelazna_dziewica");
        assertTrue(cells(maiden).get(13).contains("Złośliwa Radość"));
        assertTrue(maiden.contains("100% twoich Cierni"));
        assertEquals(List.of("zelazna_dziewica"), ids(get("necromancer", "&skillGroup=curse&hasManualReviewUpgrade=YES")));
        assertTrue(maiden.contains("BURST_DAMAGE; R1 150%; max-view 383%"));
        assertTrue(maiden.contains("zbiera 4 Blood Orbs"));
        assertFalse(cells(maiden).get(2).contains("BURST_DAMAGE"));
        var decrepify = row(page, "zniedoleznienie");
        assertTrue(decrepify.contains("Świętokradczy Amok"));
        assertTrue(decrepify.contains("zastępuje cel i efekt"));
        assertTrue(decrepify.contains("Minions zamiast wrogów"));
        assertTrue(decrepify.contains("bez przenoszenia Weaken / Slow na wrogów"));
        assertTrue(decrepify.contains("R1 +20%[x], max-view +51%[x]"));
        assertTrue(decrepify.contains("R1 10%, max-view 25.5%"));
        assertTrue(decrepify.contains("R1 3.0 s, max-view 4.2 s"));
        assertFalse(page.contains("<th>theoreticalDps</th>"));
    }

    private String get(String character, String parameters) throws Exception {
        String path = "/ranking-obrazen?character=" + character + parameters;
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), path);
        return response.body();
    }
    private static String text(String html) { return html.replaceAll("<[^>]+>", ""); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static List<String> ids(String page) {
        return Pattern.compile("data-skill-id=\"([^\"]+)\"").matcher(page).results().map(match -> match.group(1)).toList();
    }
    private static List<String> cells(String row) {
        return Pattern.compile("<td>([\\s\\S]*?)</td>").matcher(row).results().map(match -> match.group(1)).toList();
    }
    private static String row(String page, String id) {
        return Pattern.compile("<tr[^>]*data-skill-id=\"" + id + "\"[\\s\\S]*?</tr>").matcher(page).results()
                .map(match -> match.group()).findFirst().orElseThrow();
    }
}
