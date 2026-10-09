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

class NecromancerCorpseDamageRankingWebServerTest {
    private static final Set<String> CORPSE_IDS = Set.of("wybuch_zwlok", "trupie_macki", "szkieletowy_wojownik");
    private CurrentBuildWebServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        server = new CurrentBuildWebServer(0, Files.createTempDirectory("necromancer-corpse-ranking"));
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
    void shouldExposeTwentyThreeRowsAndExactlyThreeThroughBothCorpseFilters() throws Exception {
        String page = get("necromancer", "");
        assertEquals(23, ids(page).size());
        assertEquals(4, Pattern.compile("data-skill-group=\"basic\"").matcher(page).results().count());
        assertEquals(6, Pattern.compile("data-skill-group=\"core\"").matcher(page).results().count());
        assertEquals(3, Pattern.compile("data-skill-group=\"corpse\"").matcher(page).results().count());
        assertTrue(page.contains("<option value=\"corpse\">Zwłoki / Corpse</option>"));
        assertTrue(page.contains("<option value=\"CORPSE\">Zwłoki</option>"));
        assertTrue(page.indexOf("<option value=\"core\"") < page.indexOf("<option value=\"corpse\""));
        for (String filter : List.of("&skillGroup=corpse", "&sourceCategory=CORPSE")) {
            assertEquals(CORPSE_IDS, Set.copyOf(ids(get("necromancer", filter))));
        }
        assertEquals(4, ids(get("necromancer", "&skillGroup=basic")).size());
        assertEquals(6, ids(get("necromancer", "&skillGroup=core")).size());
        assertFalse(ids(page).contains("starcie"));
    }

    @Test
    void shouldRenderSourceCooldownAndConsumedRequirementsWithoutInventingPaladinMetadata() throws Exception {
        String page = get("necromancer", "&skillGroup=corpse");
        assertTrue(page.contains("<th>Odnowienie</th>"));
        assertTrue(page.contains("<th>Wymaganie użycia</th>"));
        assertFalse(page.contains("Koszt Zwłok"));
        assertCells(page, "wybuch_zwlok", "180%", "459%", "-", "1 × Zwłoki (zużywane)");
        assertCells(page, "trupie_macki", "20%", "98%", "11 s", "1 × Zwłoki (zużywane)");
        assertCells(page, "szkieletowy_wojownik", "65%", "166%", "10 s", "-");
        String paladin = get("paladin", "");
        assertEquals(24, ids(paladin).size());
        for (String id : ids(paladin)) {
            var cells = cells(row(paladin, id));
            assertEquals(16, cells.size(), id);
            assertEquals("-", cells.get(14), id);
            assertEquals("-", cells.get(15), id);
        }
    }

    @Test
    void shouldFindEveryOneOfTheOneHundredSixtyOneUpgradeNamesThroughSharedSearch() throws Exception {
        int count = 0;
        for (var skill : NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills()) {
            for (var group : skill.getUpgradeGroups()) {
                for (var upgrade : group.getUpgrades()) {
                    assertTrue(ids(get("necromancer", "&q=" + encode(upgrade.getName()))).contains(skill.getSkillId()),
                            skill.getSkillId() + " / " + upgrade.getName());
                    count++;
                }
            }
        }
        assertEquals(161, count);
        assertTrue(Set.copyOf(ids(get("necromancer", "&q=" + encode("Zwłoki")))).containsAll(CORPSE_IDS));
        assertEquals(List.of("szkieletowy_wojownik"), ids(get("necromancer", "&q=" + encode("Służba i Ofiara"))));
        assertEquals(List.of("trupie_macki"), ids(get("necromancer", "&q=" + encode("Szczęki Śmierci"))));
        assertEquals(List.of("wybuch_zwlok"), ids(get("necromancer", "&q=" + encode("Miazmaty"))));
        assertEquals(List.of("trupie_macki"), ids(get("necromancer", "&q=" + encode("Odnowienie 11 s"))));
    }

    @Test
    void shouldShowVariantDescriptionsWithoutSummingHitsOrUnlockingDps() throws Exception {
        String page = get("necromancer", "&skillGroup=corpse");
        var explosion = row(page, "wybuch_zwlok");
        assertTrue(explosion.contains("350% w ciągu 6 s"));
        assertTrue(explosion.contains("max-view 893%"));
        assertTrue(explosion.contains("8 odłamków"));
        assertTrue(explosion.contains("75%[x]"));
        assertTrue(explosion.contains("Oficjalny patch 3.1"));
        assertFalse(explosion.contains(">1440%</td>"));
        assertTrue(row(page, "trupie_macki").contains("max-view 255%"));
        assertTrue(row(page, "szkieletowy_wojownik").contains("max-view 383%"));
        assertFalse(row(page, "szkieletowy_wojownik").contains(">260%</td>"));
        assertFalse(page.contains("<th>theoreticalDps</th>"));
        assertFalse(page.contains("<th>damagePerUse</th>"));
        assertEquals(3, Pattern.compile("data-verification-status=\"NEEDS_VERIFICATION\"").matcher(page).results().count());
    }

    @Test
    void shouldExposeTheTendrilsConflictAndKeepResolvedBloodyMessOutOfManualReview() throws Exception {
        String page = get("necromancer", "&skillGroup=corpse");
        assertEquals(List.of("trupie_macki"), ids(get("necromancer", "&skillGroup=corpse&hasManualReviewUpgrade=YES")));
        var tendrils = row(page, "trupie_macki");
        assertTrue(tendrils.contains("Manual review"));
        assertTrue(tendrils.contains("+40%"));
        assertTrue(tendrils.contains("changelog EN D4Guides 3.2.1"));
        assertTrue(cells(tendrils).get(6).contains("Lucky Hit 30%"));
        assertTrue(tendrils.contains(DamageRankingSearchText.normalize("Zwłoki wymagane, ale niezużywane")));
        assertTrue(tendrils.contains(DamageRankingSearchText.normalize("Wariant nie wymaga Zwłok")));
    }

    private String get(String character, String parameters) throws Exception {
        String path = "/ranking-obrazen?character=" + character + parameters;
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), path);
        return response.body();
    }

    private static void assertCells(String page, String id, String rankOne, String maxView, String cooldown, String requirement) {
        var cells = cells(row(page, id));
        assertEquals(16, cells.size());
        assertEquals(rankOne, cells.get(2));
        assertEquals(maxView, cells.get(3));
        assertEquals("-", cells.get(4));
        assertEquals("-", cells.get(5));
        assertEquals(cooldown, cells.get(14));
        assertEquals(requirement, cells.get(15));
    }

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
