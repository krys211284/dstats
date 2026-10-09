package krys.web;

import com.sun.net.httpserver.HttpServer;
import krys.hero.HeroClass;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.runtime.RuntimeSkillLoadout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class NecromancerProfileWebFlowTest {
    @TempDir Path directory;
    private CurrentBuildWebServer server;
    private HttpServer guardedServer;
    private String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void startServer() throws Exception {
        server = new CurrentBuildWebServer(0, directory);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.close();
        if (guardedServer != null) guardedServer.stop(0);
    }

    @Test
    void heroesPageShouldOfferBothClassesAndSupportNecromancerCreateEditActivateAndDelete() throws Exception {
        var page = get("/bohaterowie");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("value=\"PALADIN\">Paladyn"));
        assertTrue(page.body().contains("value=\"NECROMANCER\">Nekromanta"));
        create("Alaric", "PALADIN", "13");
        var response = create("Mortis", "NECROMANCER", "70");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Mortis"));
        assertTrue(response.body().contains("Nekromanta | poziom 70"));
        assertTrue(response.body().contains("name=\"action\" value=\"updateHeroDetails\""));
        var service = service();
        var necro = service.getHeroes().getLast();
        assertEquals(HeroClass.NECROMANCER, necro.getHeroClass());
        assertEquals("necromancer", necro.getRuntimeSkillLoadout().classId());
        assertTrue(necro.getRuntimeSkillLoadout().configurations().isEmpty());
        assertEquals(1, service.requireActiveHero().getHeroId());

        response = post("/bohaterowie", Map.of("action", "setActive", "heroId", Long.toString(necro.getHeroId())));
        assertEquals(200, response.statusCode());
        assertEquals(necro.getHeroId(), service.requireActiveHero().getHeroId());
        response = post("/bohaterowie", Map.of("action", "updateHeroDetails", "heroId", Long.toString(necro.getHeroId()),
                "heroName", "Morta", "heroLevel", "20", "heroClass", "PALADIN"));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Morta"));
        assertTrue(response.body().contains("Nekromanta | poziom 20"));
        var edited = service.requireActiveHero();
        assertEquals("Morta", edited.getName());
        assertEquals(HeroClass.NECROMANCER, edited.getHeroClass());
        assertEquals(necro.getRuntimeSkillLoadout(), edited.getRuntimeSkillLoadout());

        // Restart całej aplikacji odtwarza profil oraz wybór aktywnego bohatera z tych samych plików.
        server.close();
        server = new CurrentBuildWebServer(0, directory);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getPort();
        response = get("/bohaterowie");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Morta"));
        assertEquals(necro.getHeroId(), service().requireActiveHero().getHeroId());
        assertEquals(edited.getRuntimeSkillLoadout(), service().requireActiveHero().getRuntimeSkillLoadout());
        response = post("/bohaterowie", Map.of("action", "deleteHero", "heroId", Long.toString(necro.getHeroId())));
        assertEquals(200, response.statusCode());
        assertEquals(1, service().getHeroes().size());
        assertEquals(HeroClass.PALADIN, service().requireActiveHero().getHeroClass());
    }

    @Test
    void invalidCreateAndEditFieldsShouldReturnReadableErrorsWithoutMutation() throws Exception {
        var response = create("Mortis", "INVALID", "70");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Wybierz poprawną klasę bohatera"));
        assertTrue(service().getHeroes().isEmpty());
        create("Mortis", "NECROMANCER", "70");
        response = post("/bohaterowie", Map.of("action", "updateHeroDetails", "heroId", "1", "heroName", " ", "heroLevel", "20"));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Nazwa bohatera jest wymagana"));
        response = post("/bohaterowie", Map.of("action", "updateHeroDetails", "heroId", "1", "heroName", "Morta", "heroLevel", "71"));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Poziom bohatera musi być w zakresie 1..70"));
        assertEquals("Mortis", service().requireActiveHero().getName());
        assertEquals("70", service().requireActiveHero().getCurrentBuildFormData().getLevel());
    }

    @Test
    void currentBuildShouldOfferN11AndSearchStayUnsupportedWhileLegacyMutationsAreRejected() throws Exception {
        create("Mortis", "NECROMANCER", "70");
        var before = service().requireActiveHero();
        var current = get("/policz-aktualny-build?level=1&rank_HOLY_BOLT=5");
        assertNecromancerCurrentBuild(current);
        for (String action : List.of("calculate", "save")) {
            current = post("/policz-aktualny-build", Map.of("formAction", action, "level", "1", "weaponDamage", "invalid",
                    "rank_HOLY_BOLT", "5", "actionBar1", "HOLY_BOLT"));
            assertNecromancerCurrentBuild(current);
        }
        current = post("/policz-aktualny-build", Map.of("heroAction", "addAssignedSkill", "skillIdToAdd", "HOLY_BOLT"));
        assertNecromancerCurrentBuild(current);
        for (var response : List.of(get("/znajdz-najlepszy-build"), post("/znajdz-najlepszy-build", Map.of("levelValues", "70")),
                post("/znajdz-najlepszy-build/szczegoly", Map.of("level", "70")))) {
            assertSearchUnsupported(response);
        }
        assertEquals(before.getRuntimeSkillLoadout(), service().requireActiveHero().getRuntimeSkillLoadout());
        assertEquals(before.getCurrentBuildQuery(), service().requireActiveHero().getCurrentBuildQuery());
        assertEquals(before.getItemSelection().getSelectedItemIdsBySlot(), service().requireActiveHero().getItemSelection().getSelectedItemIdsBySlot());
    }

    @Test
    void inlineHeroSwitchShouldCrossTheCapabilityBoundaryInBothDirections() throws Exception {
        create("Alaric", "PALADIN", "13");
        create("Mortis", "NECROMANCER", "70");
        var paladinPage = get("/policz-aktualny-build");
        assertEquals(200, paladinPage.statusCode());
        assertTrue(paladinPage.body().contains("Mortis (Nekromanta)"));
        assertTrue(paladinPage.body().contains("name=\"formAction\" value=\"calculate\""));
        assertNecromancerCurrentBuild(post("/policz-aktualny-build", Map.of("heroAction", "setActiveHeroInline", "selectedHeroId", "2")));
        paladinPage = post("/policz-aktualny-build", Map.of("heroAction", "setActiveHeroInline", "selectedHeroId", "1"));
        assertEquals(200, paladinPage.statusCode());
        assertTrue(paladinPage.body().contains("name=\"formAction\" value=\"calculate\""));
        assertEquals(HeroClass.PALADIN, service().requireActiveHero().getHeroClass());
    }

    @Test
    void necromancerEndpointsShouldWorkWithNoLegacyMappersOrCalculationServicesEvenForPersistedTechnicalSkills() throws Exception {
        var key = new RuntimeSkillKey("necromancer", "krwotok");
        var loadout = new RuntimeSkillLoadout("necromancer", Map.of(key, new RuntimeSkillConfiguration(key, 1, Map.of())), List.of(key));
        var repository = new FileHeroProfileRepository(directory);
        repository.save(new HeroProfile(1, "Mortis", HeroClass.NECROMANCER, "level=70", HeroItemSelection.empty(), loadout));
        repository.saveActiveHeroId(1L);
        var service = new HeroService(repository);
        var items = new ItemLibraryService(new FileItemLibraryRepository(directory));
        guardedServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // Brak mapperów i usług obliczeń jest celowym dowodem, że żadna ścieżka Necro ich nie dereferencjuje.
        guardedServer.createContext("/policz-aktualny-build", new CurrentBuildController(null, new CurrentBuildPageRenderer(), null, items, service));
        guardedServer.createContext("/znajdz-najlepszy-build", new SearchBuildController(null, new SearchBuildPageRenderer(), null, items, service));
        guardedServer.createContext("/znajdz-najlepszy-build/szczegoly", new SearchBuildDetailsController(null, new SearchBuildDetailsPageRenderer(), null, service));
        guardedServer.start();
        baseUrl = "http://127.0.0.1:" + guardedServer.getAddress().getPort();
        assertNecromancerCurrentBuild(get("/policz-aktualny-build"));
        for (String action : List.of("save", "calculate")) {
            assertNecromancerCurrentBuild(post("/policz-aktualny-build", Map.of("formAction", action)));
        }
        assertSearchUnsupported(get("/znajdz-najlepszy-build"));
        assertSearchUnsupported(post("/znajdz-najlepszy-build", Map.of()));
        assertSearchUnsupported(post("/znajdz-najlepszy-build/szczegoly", Map.of()));
        assertEquals(loadout, service.requireActiveHero().getRuntimeSkillLoadout());
    }

    private static void assertNecromancerCurrentBuild(HttpResponse<String> response) {
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Mortis"));
        assertTrue(response.body().contains("Nekromanta"));
        assertTrue(response.body().contains(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Poziom bohatera", "70")));
        assertTrue(response.body().contains("N11 — bazowy direct hit Krwotoku R1"));
        assertTrue(response.body().contains("name=\"formAction\" value=\"calculate\""));
        for (String name : List.of("selectedPaladinOathId", "initialAnimus", "skillIdToAdd", "rank_HOLY_BOLT")) {
            assertFalse(response.body().contains("name=\"" + name + "\""));
        }
    }

    private static void assertSearchUnsupported(HttpResponse<String> response) {
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Mortis"));
        assertTrue(response.body().contains("Nekromanta"));
        assertTrue(response.body().contains(CurrentBuildRuntimeSupport.NECROMANCER_SEARCH_MESSAGE));
        assertFalse(response.body().contains("<form"));
        assertFalse(response.body().contains("HOLY_BOLT"));
    }

    private HeroService service() { return new HeroService(new FileHeroProfileRepository(directory)); }

    private HttpResponse<String> create(String name, String heroClass, String level) throws Exception {
        return post("/bohaterowie", Map.of("action", "createHero", "heroName", name, "heroClass", heroClass, "heroLevel", level));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, Map<String, String> fields) throws Exception {
        String body = fields.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
