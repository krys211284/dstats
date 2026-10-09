package krys.web;

import krys.combat.DamageEngine;
import krys.ranking.CharacterSkillTreeRegistry;
import krys.ranking.PlayableClass;
import krys.ranking.DamageRankingService;
import krys.ranking.NeutralSkillTreeFixture;
import krys.ranking.SkillTreeRegistryProvider;
import krys.skilltree.*;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NeutralDamageRankingWebServerTest {
    @Test
    void sameControllerShouldRenderNeutralCategoriesResourcesComponentsAndFilterThem() throws Exception {
        var registry = NeutralSkillTreeFixture.registry();
        var provider = new SkillTreeRegistryProvider(List.of(registry));
        var service = new DamageRankingService(new DamageEngine(), provider);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ranking-obrazen", new DamageRankingController(service, new DamageRankingPageRenderer(), provider));
        server.start();
        try (var client = HttpClient.newHttpClient()) {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/ranking-obrazen?character=paladin";
            String page = get(client, baseUrl);
            assertTrue(page.contains("NeutralTestRegistry"));
            assertTrue(page.contains("Grupa testowa"));
            assertTrue(page.contains("Kategoria testowa"));
            assertTrue(page.contains("Koszt energii testowej"));
            assertTrue(page.contains("Generowanie energii testowej"));
            assertFalse(page.contains("Koszt Wiary"));
            assertFalse(page.contains("placeholder=\"Starcie, Adept, Odsłonięcie\""));
            assertTrue(page.contains("data-mechanic-tags=\"TEST_MECHANIC\""));
            assertTrue(page.contains("data-skill-id=\"test_direct\""));
            assertTrue(page.contains("data-skill-id=\"test_component\""));
            assertTrue(page.contains("data-skill-id=\"test_utility\""));
            assertTrue(page.contains(registry.requireSkill("test_direct").getBaseDamagePercentAtRank1() + "%"));
            assertTrue(page.contains("FIRST_STRIKE_DAMAGE"));

            String filtered = get(client, baseUrl + "&skillGroup=test_group&sourceCategory=test_category&tag=test_mechanic&q=umiejetnosc");
            assertTrue(filtered.contains("data-skill-id=\"test_direct\""));
            assertFalse(filtered.contains("data-skill-id=\"test_component\""));
            assertFalse(filtered.contains("data-skill-id=\"test_utility\""));
            assertTrue(filtered.contains("<option value=\"TEST_CATEGORY\" selected>"));
            assertTrue(filtered.contains("q=umiejetnosc"));

            String unsupported = get(client, baseUrl + "&verificationStatus=UNSUPPORTED");
            assertTrue(unsupported.contains("data-skill-id=\"test_component\""));
            assertFalse(unsupported.contains("data-skill-id=\"test_direct\""));
            String withoutGeneration = get(client, baseUrl + "&hasResourceGeneration=NO&sort=resourceCost&direction=asc");
            assertFalse(withoutGeneration.contains("data-skill-id=\"test_direct\""));
            assertTrue(withoutGeneration.contains("data-skill-id=\"test_component\""));
            assertTrue(withoutGeneration.contains("aria-sort=\"ascending\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRenderAndSearchPeriodicAndEventBaseAndVariantTablesWithoutDependingOnTheCharacter() throws Exception {
        // Golden N5 jest użyte w fikcyjnym rejestrze z kluczem Paladyna, bez katalogu Curse.
        var table = DamagePercentComponentRankTable.of(Map.of(
                DamagePercentComponent.PERIODIC_DAMAGE, DamagePercentRankTable.of(Map.of(1, 30, 15, 77)),
                DamagePercentComponent.EVENT_TRIGGERED_DAMAGE, DamagePercentRankTable.of(Map.of(1, 30, 15, 77))),
                Map.of(), Map.of(DamagePercentComponent.PERIODIC_DAMAGE, 2));
        var variant = new UpgradeDamageModifier("grupa_3", "Wariant testowy", UpgradeDamageSafety.YES,
                UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT, "30%", UpgradeDamageValueKind.COMPONENT_PERCENT,
                "Po wyborze", true, "Impuls i zdarzenie", true, UpgradeDamageSafety.YES,
                UpgradeDamageSafety.NO, "Neutralny wariant", table);
        var skills = new ArrayList<CharacterTreeSkill>();
        for (String id : List.of("neutral_base", "neutral_variant")) {
            skills.add(new CharacterTreeSkill(id, id, "Testowe źródło", "test_group", null, null,
                    DamagePercentRankTable.empty(),
                    id.equals("neutral_base") ? table : DamagePercentComponentRankTable.empty(),
                    SkillTreeType.DAMAGE, SkillTreeStatus.NEEDS_VERIFICATION, List.of(),
                    "Neutralne metadane źródłowe: czas 30 s.", Set.of(NeutralSkillTreeFixture.CATEGORY),
                    Set.of(NeutralSkillTreeFixture.TAG), SkillResourceData.EMPTY, null, List.of(),
                    id.equals("neutral_variant") ? List.of(variant) : List.of()));
        }
        var registry = new CharacterSkillTreeRegistry(PlayableClass.PALADIN, "NeutralTimingRegistry",
                skills, NeutralSkillTreeFixture.RESOURCE, Map.of("test_group", "Grupa testowa"));
        var provider = new SkillTreeRegistryProvider(List.of(registry));
        var service = new DamageRankingService(null, provider);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ranking-obrazen", new DamageRankingController(service, new DamageRankingPageRenderer(), provider));
        server.start();
        try (var client = HttpClient.newHttpClient()) {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/ranking-obrazen?character=paladin";
            String page = get(client, url);
            assertTrue(page.contains("PERIODIC_DAMAGE</code>: 30% co 2 s"));
            assertTrue(page.contains("PERIODIC_DAMAGE</code>: 77% co 2 s"));
            assertTrue(page.contains("EVENT_TRIGGERED_DAMAGE</code>: 30% na zdarzenie"));
            assertTrue(page.contains("EVENT_TRIGGERED_DAMAGE</code>: 77% na zdarzenie"));
            assertTrue(page.contains("PERIODIC_DAMAGE; R1 30% co 2 s; max-view 77% co 2 s"));
            assertTrue(page.contains("EVENT_TRIGGERED_DAMAGE; R1 30% na zdarzenie; max-view 77% na zdarzenie"));
            assertFalse(page.contains("30% / sek."));
            for (String query : List.of("2 s", "30 s", "na zdarzenie")) {
                String filtered = get(client, url + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
                assertTrue(filtered.contains("data-skill-id=\"neutral_base\""), query);
                assertTrue(filtered.contains("data-skill-id=\"neutral_variant\""), query);
            }
            for (var entry : service.describeTreeSkills(PlayableClass.PALADIN)) {
                assertNull(entry.getDamagePerUse());
                assertNull(entry.getEffectiveCycleSeconds());
                assertNull(entry.getTheoreticalDps());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRenderFilterSearchAndSortPerEventGenerationInAnIndependentPaladinFixture() throws Exception {
        // Golden N6 2/event oraz N1 10/use i 12–16/second, bez korzystania z rejestru Nekromanty.
        var resources = Map.of(
                "event", new SkillResourceData(null, new java.math.BigDecimal("2"), new java.math.BigDecimal("2"), null, SkillResourceGenerationFrequency.PER_EVENT),
                "use", new SkillResourceData(null, 10, null),
                "second", new SkillResourceData(null, new java.math.BigDecimal("12"), new java.math.BigDecimal("16"), null, SkillResourceGenerationFrequency.PER_SECOND),
                "none", SkillResourceData.EMPTY);
        var skills = new ArrayList<CharacterTreeSkill>();
        for (var id : List.of("event", "use", "second", "none"))
            skills.add(new CharacterTreeSkill(id, id, "Testowe źródło", "test_group", null, null,
                    DamagePercentRankTable.empty(), DamagePercentComponentRankTable.empty(),
                    SkillTreeType.DAMAGE, SkillTreeStatus.NEEDS_VERIFICATION, List.of(),
                    id.equals("event") ? "Zdarzenie: pochłonięcie jednej duszy przeciwnika; sprawdzanie co 0.25 s." : "Neutralny opis",
                    Set.of(NeutralSkillTreeFixture.CATEGORY), Set.of(NeutralSkillTreeFixture.TAG),
                    resources.get(id), null, List.of(), List.of()));
        var registry = new CharacterSkillTreeRegistry(PlayableClass.PALADIN, "NeutralEventResourceRegistry",
                skills, NeutralSkillTreeFixture.RESOURCE, Map.of("test_group", "Grupa testowa"));
        var provider = new SkillTreeRegistryProvider(List.of(registry));
        var service = new DamageRankingService(null, provider);
        var eventRow = new DamageRankingRow(service.describeTreeSkills(PlayableClass.PALADIN).stream()
                .filter(entry -> entry.getSkillId().equals("event")).findFirst().orElseThrow(), registry.requireSkill("event"),
                registry.getResourceType(), registry.treeGroupDisplayName("test_group"));
        assertEquals("2 / zdarzenie", eventRow.getResourceGenerationBaseSummary());
        assertEquals("2 / zdarzenie", eventRow.getResourceGenerationSummary());
        assertTrue(eventRow.hasResourceGeneration());
        assertEquals(new java.math.BigDecimal("2"), eventRow.getResourceGenerationBaseSortValue());
        assertEquals(new java.math.BigDecimal("2"), eventRow.getResourceGenerationMaxKnownSortValue());
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ranking-obrazen", new DamageRankingController(service, new DamageRankingPageRenderer(), provider));
        server.start();
        try (var client = HttpClient.newHttpClient()) {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/ranking-obrazen?character=paladin";
            String page = get(client, url);
            assertTrue(page.contains("2 / zdarzenie"));
            assertTrue(page.contains("12–16 / sek."));
            var eventCells = java.util.regex.Pattern.compile("<tr[^>]*data-skill-id=\"event\"[^>]*>[\\s\\S]*?</tr>")
                    .matcher(page).results().findFirst().orElseThrow().group();
            assertTrue(eventCells.contains("2 / zdarzenie"));
            assertFalse(eventCells.contains("/ sek."));
            assertTrue(eventCells.contains("title=\"Zdarzenie: pochłonięcie jednej duszy przeciwnika; sprawdzanie co 0.25 s.\""));
            for (var query : List.of("zdarzenie", "0.25 s", "pochłonięcie jednej duszy"))
                assertEquals(List.of("event"), resourceIds(get(client, url + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8))));
            assertEquals(Set.of("event", "use", "second"), Set.copyOf(resourceIds(get(client, url + "&hasResourceGeneration=YES"))));
            assertEquals(List.of("none"), resourceIds(get(client, url + "&hasResourceGeneration=NO")));
            for (var sort : List.of("resourceGeneratedBase", "resourceGeneratedMaxKnown")) {
                assertEquals(List.of("event", "use", "second", "none"),
                        resourceIds(get(client, url + "&sort=" + sort + "&direction=asc")));
                assertEquals(List.of("second", "use", "event", "none"),
                        resourceIds(get(client, url + "&sort=" + sort + "&direction=desc")));
            }
            for (var entry : service.describeTreeSkills(PlayableClass.PALADIN)) {
                assertNull(entry.getDamagePerUse());
                assertNull(entry.getEffectiveCycleSeconds());
                assertNull(entry.getTheoreticalDps());
            }
        } finally { server.stop(0); }
    }

    private static List<String> resourceIds(String page) {
        return java.util.regex.Pattern.compile("data-skill-id=\"([^\"]+)\"").matcher(page)
                .results().map(match -> match.group(1)).toList();
    }


    private static String get(HttpClient client, String url) throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return response.body();
    }
}
