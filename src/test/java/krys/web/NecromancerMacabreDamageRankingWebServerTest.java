package krys.web;

import krys.necromancer.NecromancerSkillTreeRegistry;
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

class NecromancerMacabreDamageRankingWebServerTest {
    private static final Set<String> MACABRE_IDS = Set.of("krwawa_mgla", "wiezienie_z_kosci", "kosciane_widmo", "golem");
    private CurrentBuildWebServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        server = new CurrentBuildWebServer(0, Files.createTempDirectory("necromancer-macabre-ranking"));
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
    void shouldExposeTwentyThreeRowsAndFourMacabreRowsThroughBothFiltersInCatalogOrder() throws Exception {
        String page = get("necromancer", "");
        assertEquals(23, ids(page).size());
        for (var group : List.of("basic", "core", "corpse", "macabre")) {
            int expected = switch (group) { case "basic", "macabre" -> 4; case "core" -> 6; default -> 3; };
            assertEquals(expected, Pattern.compile("data-skill-group=\"" + group + "\"").matcher(page).results().count());
        }
        assertTrue(page.contains("<option value=\"macabre\">Makabra / Macabre</option>"));
        assertTrue(page.contains("<option value=\"MACABRE\">Makabra</option>"));
        assertTrue(page.indexOf("<option value=\"corpse\"") < page.indexOf("<option value=\"macabre\""));
        for (String filter : List.of("&skillGroup=macabre", "&sourceCategory=MACABRE")) {
            assertEquals(MACABRE_IDS, Set.copyOf(ids(get("necromancer", filter))));
        }
        assertEquals(24, ids(get("paladin", "")).size());
    }

    @Test
    void shouldRenderMistDurationPrisonWithoutDamageOrLuckyHitAndSpiritChargesInTheExistingColumns() throws Exception {
        String page = get("necromancer", "&skillGroup=macabre");
        var mist = cells(row(page, "krwawa_mgla"));
        assertEquals(16, mist.size());
        assertTrue(mist.get(2).contains("420% w ciągu 3 s"));
        assertTrue(mist.get(3).contains("1071% w ciągu 3 s"));
        assertEquals("24 s", mist.get(14));
        var prison = cells(row(page, "wiezienie_z_kosci"));
        assertEquals("nie dotyczy", textContent(prison.get(2)));
        assertEquals("nie dotyczy", textContent(prison.get(3)));
        assertEquals("-", textContent(prison.get(6)));
        assertEquals("15 s", prison.get(14));
        var spirit = cells(row(page, "kosciane_widmo"));
        assertEquals("150%", spirit.get(2));
        assertEquals("383%", spirit.get(3));
        assertEquals("2 ładunki; 12 s / ładunek", textContent(spirit.get(14)));
        assertFalse(row(page, "kosciane_widmo").contains("Odnowienie 12 s"));
        var golem = cells(row(page, "golem"));
        assertEquals("125%", golem.get(2));
        assertEquals("319%", golem.get(3));
        assertEquals("16 s", golem.get(14));
        for (String id : MACABRE_IDS) {
            var cells = cells(row(page, id));
            assertEquals("-", cells.get(4), id);
            assertEquals("-", cells.get(5), id);
            assertEquals("-", cells.get(15), id);
        }
    }

    @Test
    void shouldIndexBaseChargeCountAndRechargeAndAllTwentyEightNewUpgradeNames() throws Exception {
        assertEquals(List.of("kosciane_widmo"), ids(get("necromancer", "&q=" + encode("ładunki"))));
        assertEquals(List.of("kosciane_widmo"), ids(get("necromancer", "&q=" + encode("2 ładunki"))));
        assertEquals(List.of("kosciane_widmo"), ids(get("necromancer", "&q=" + encode("12 s / ładunek"))));
        assertTrue(ids(get("necromancer", "&q=" + encode("Odnowienie 12 s"))).isEmpty());
        int count = 0;
        for (var skill : NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills().stream()
                .filter(skill -> skill.getSkillGroup().equals("macabre")).toList()) {
            for (var upgrade : skill.getUpgradeGroups().stream().flatMap(group -> group.getUpgrades().stream()).toList()) {
                assertTrue(ids(get("necromancer", "&q=" + encode(upgrade.getName()))).contains(skill.getSkillId()), upgrade.getName());
                count++;
            }
        }
        assertEquals(28, count);
        for (String query : List.of("Przypływ Krwi", "Bezdenna Ciemność", "Plugawa Żarłoczność", "Grobowy Kwiat")) {
            assertEquals(1, ids(get("necromancer", "&q=" + encode(query))).size(), query);
        }
        // Pełne 161 nazw sprawdza również współdzielony test HTTP Corpse.
    }

    @Test
    void shouldShowVariantCoefficientsAndCorpseOverridesWithoutChangingTheBaseProfiles() throws Exception {
        String page = get("necromancer", "&skillGroup=macabre");
        var mist = row(page, "krwawa_mgla");
        assertTrue(mist.contains("600% w ciągu 3 s"));
        assertTrue(mist.contains("max-view 1530% w ciągu 3 s"));
        assertTrue(mist.contains("Zużywa 1 pobliskie Zwłoki"));
        assertTrue(mist.contains("Odnowienie wariantu: 8 s"));
        assertEquals("24 s", cells(mist).get(14));
        assertEquals("-", cells(mist).get(15));
        var prison = row(page, "wiezienie_z_kosci");
        assertTrue(prison.contains("R1 450%"));
        assertTrue(prison.contains("max-view 1148%"));
        assertTrue(prison.contains("100% twoich Cierni"));
        assertFalse(cells(prison).get(2).contains("100%"));
        var spirit = row(page, "kosciane_widmo");
        assertTrue(spirit.contains("max-view 77%"));
        assertTrue(spirit.contains("max-view 638%"));
        assertTrue(spirit.contains("max-view 255%"));
        assertTrue(spirit.contains("koszt bazowy wariantu 30 Esencji"));
        assertTrue(spirit.contains("Wariant bez cooldownu i odnowienia ładunków"));
        assertEquals("2 ładunki; 12 s / ładunek", textContent(cells(spirit).get(14)));
        var golem = row(page, "golem");
        assertTrue(golem.contains("60% normalnych obrażeń"));
        assertTrue(golem.contains("3 mniejsze Golemy"));
        assertTrue(golem.contains("max-view 765%"));
        assertTrue(golem.contains("Pasywnie zużywa pobliskie Zwłoki"));
        assertEquals("16 s", cells(golem).get(14));
        assertEquals("-", cells(golem).get(15));
        assertFalse(page.contains("<th>theoreticalDps</th>"));
        assertFalse(page.contains("<th>damagePerUse</th>"));
        assertEquals(4, Pattern.compile("data-verification-status=\"NEEDS_VERIFICATION\"").matcher(page).results().count());
    }

    @Test
    void shouldKeepVariableGolemTypesAndThornsReviewDistinctFromBaseDamage() throws Exception {
        String page = get("necromancer", "&skillGroup=macabre");
        assertTrue(row(page, "golem").contains("data-mechanic-tags=\"MACABRE,MINION,SUMMON\""));
        assertEquals(List.of("wiezienie_z_kosci"),
                ids(get("necromancer", "&skillGroup=macabre&hasManualReviewUpgrade=YES")));
        assertTrue(cells(row(page, "wiezienie_z_kosci")).get(13).contains("Cierń"));
        assertTrue(row(page, "kosciane_widmo").contains("charge cooldown 12 -&gt; 10 s"));
    }

    private String get(String character, String parameters) throws Exception {
        String path = "/ranking-obrazen?character=" + character + parameters;
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), path);
        return response.body();
    }

    private static String textContent(String html) { return html.replaceAll("<[^>]+>", ""); }
    private static String encode(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8); }
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
