package krys.web;

import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.paladin.PaladinSkillTreeRegistry;
import krys.ranking.DamageRankingService;
import krys.ranking.PlayableClass;
import krys.ranking.SkillTreeRegistryProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerDamageRankingWebServerTest {
    private CurrentBuildWebServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        server = new CurrentBuildWebServer(0, Files.createTempDirectory("necromancer-ranking"));
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
    void productionRouteShouldPreserveFourBasicSkillsAndKeepPaladinDefault() throws Exception {
        String page = get("/ranking-obrazen?character=necromancer&skillGroup=basic");
        assertTrue(page.contains("<option value=\"necromancer\" selected>Nekromanta</option>"));
        assertTrue(page.contains("NecromancerSkillTreeRegistry"));
        assertTrue(page.contains("Koszt Esencji"));
        assertTrue(page.contains("Generowanie Esencji"));
        assertEquals(Set.of("kosciane_odlamki", "zniwa", "rozklad", "krwotok"), Set.copyOf(ids(page)));
        assertEquals(4, ids(page).size());
        assertFalse(page.contains("Koszt Wiary"));
        for (var skill : PaladinSkillTreeRegistry.allSkills()) {
            assertFalse(page.contains("data-skill-id=\"" + skill.getSkillId() + "\""));
        }
        String paladin = get("/ranking-obrazen?character=paladin");
        assertEquals(24, ids(paladin).size());
        assertEquals(PaladinSkillTreeRegistry.allSkills().stream().map(skill -> skill.getSkillId()).collect(Collectors.toSet()), Set.copyOf(ids(paladin)));
        assertEquals(paladin, get("/ranking-obrazen"));
    }

    @Test
    void searchShouldFindPolishNamesTagsAndAllTypesOfUpgrades() throws Exception {
        for (var skill : NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills().stream().filter(skill -> skill.getSkillGroup().equals("basic")).toList()) {
            assertEquals(List.of(skill.getSkillId()), ids(get("/ranking-obrazen?character=necromancer&skillGroup=basic&q="
                    + URLEncoder.encode(skill.getSkillName(), StandardCharsets.UTF_8))));
            for (var upgrade : skill.getUpgradeGroups().stream().flatMap(group -> group.getUpgrades().stream()).toList()) {
                assertTrue(ids(get("/ranking-obrazen?character=necromancer&skillGroup=basic&q="
                        + URLEncoder.encode(upgrade.getName(), StandardCharsets.UTF_8))).contains(skill.getSkillId()), upgrade.getName());
            }
        }
        assertEquals(List.of("zniwa"), ids(get("/ranking-obrazen?character=necromancer&skillGroup=basic&q=dorzynki")));
        assertEquals(List.of("kosciane_odlamki"), ids(get("/ranking-obrazen?character=necromancer&skillGroup=basic&tag=BONE")));
        assertEquals(List.of("rozklad"), ids(get("/ranking-obrazen?character=necromancer&skillGroup=basic&q=CHANNELED")));
        assertEquals(4, ids(get("/ranking-obrazen?character=necromancer&skillGroup=basic&sourceCategory=BASIC")).size());
    }

    @Test
    void sourceRangeShouldRenderPerSecondAndParticipateInFacetsAndBothSortOrders() throws Exception {
        String page = get("/ranking-obrazen?character=necromancer&skillGroup=basic&tag=CHANNELED");
        assertEquals(List.of("rozklad"), ids(page));
        assertTrue(page.contains("<td>12–16 / sek.</td>"));
        assertFalse(page.contains("<td>13</td>"));
        assertEquals(4, ids(get("/ranking-obrazen?character=necromancer&skillGroup=basic&hasResourceGeneration=YES")).size());
        assertEquals(0, ids(get("/ranking-obrazen?character=necromancer&skillGroup=basic&hasResourceGeneration=NO")).size());
        assertEquals(List.of("kosciane_odlamki", "rozklad", "krwotok", "zniwa"), ids(get(
                "/ranking-obrazen?character=necromancer&skillGroup=basic&sort=resourceGeneratedBase&direction=asc")));
        assertEquals(List.of("zniwa", "krwotok", "rozklad", "kosciane_odlamki"), ids(get(
                "/ranking-obrazen?character=necromancer&skillGroup=basic&sort=resourceGeneratedBase&direction=desc")));
        assertEquals(List.of("krwotok", "kosciane_odlamki", "zniwa", "rozklad"), ids(get(
                "/ranking-obrazen?character=necromancer&skillGroup=basic&sort=resourceGeneratedMaxKnown&direction=asc")));
        assertEquals(List.of("rozklad", "kosciane_odlamki", "zniwa", "krwotok"), ids(get(
                "/ranking-obrazen?character=necromancer&skillGroup=basic&sort=resourceGeneratedMaxKnown&direction=desc")));
        var registry = NecromancerSkillTreeRegistry.asCharacterRegistry();
        var entry = new DamageRankingService(null, SkillTreeRegistryProvider.production()).describeTreeSkills(PlayableClass.NECROMANCER)
                .stream().filter(value -> value.getSkillId().equals("rozklad")).findFirst().orElseThrow();
        var row = new DamageRankingRow(entry, registry.requireSkill("rozklad"), registry.getResourceType(), "Podstawowe / Basic");
        assertEquals(new BigDecimal("12"), row.getResourceGenerationBaseSortValue());
        assertEquals(new BigDecimal("16"), row.getResourceGenerationMaxKnownSortValue());
        assertTrue(row.hasResourceGeneration());
    }

    @Test
    void decimalsAndTheLuckyHitConflictShouldRemainVisibleInSourceTooltips() throws Exception {
        String page = get("/ranking-obrazen?character=necromancer&skillGroup=basic");
        assertTrue(page.contains("7.5%"));
        assertTrue(page.contains("62.5%"));
        assertTrue(page.contains("7.5 Esencji"));
        assertTrue(page.contains("Szansa na Szczęśliwy Traf"));
        assertTrue(page.contains("Konflikt źródeł"));
        assertTrue(page.contains("+30%"));
        assertEquals(List.of("rozklad"), ids(get("/ranking-obrazen?character=necromancer&skillGroup=basic&hasManualReviewUpgrade=YES")));
        assertEquals(List.of("krwotok", "rozklad", "zniwa", "kosciane_odlamki"), ids(get(
                "/ranking-obrazen?character=necromancer&skillGroup=basic&sort=maxExtraHitOrComponentPercent&direction=desc")));
    }

    private String get(String path) throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), path);
        return response.body();
    }

    private static List<String> ids(String page) {
        return Pattern.compile("data-skill-id=\"([^\"]+)\"").matcher(page).results().map(match -> match.group(1)).toList();
    }
}
