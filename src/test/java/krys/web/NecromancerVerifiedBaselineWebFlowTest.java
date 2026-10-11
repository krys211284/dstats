package krys.web;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.ValidatedImportedItem;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Acceptance snapshot przez HTTP: nagi Nekromanta 70, bez ręcznego wpisywania bazowej INT. */
class NecromancerVerifiedBaselineWebFlowTest {
    @TempDir Path directory;
    private CurrentBuildWebServer server;
    private String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void start() throws Exception { restart(); }

    private void restart() throws Exception {
        if (server != null) { server.close(); }
        server = new CurrentBuildWebServer(0, directory);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getPort();
    }

    @AfterEach
    void stop() { if (server != null) { server.close(); } }

    @Test
    void nakedLevel70ShouldMatchAllVerifiedGameStatsWithoutManualIntelligence() throws Exception {
        create(70);
        var hero = heroes().requireActiveHero();
        assertEquals(HeroClass.NECROMANCER, hero.getHeroClass());
        assertTrue(hero.getItemSelection().getSelectedItemIdsBySlot().isEmpty());
        assertTrue(hero.getRuntimeSkillLoadout().configurations().isEmpty());
        assertTrue(hero.getRuntimeSkillLoadout().actionBar().isEmpty());
        var response = get();
        assertEquals(200, response.statusCode());
        String html = response.body();
        assertTrue(html.contains("<summary>Statystyki bohatera</summary>"));
        assertCard(html, "Klasa", "Nekromanta");
        assertCard(html, "Poziom", "70");
        assertCard(html, "Siła", "76");
        assertCard(html, "Inteligencja", "79");
        assertCard(html, "Siła woli", "77");
        assertCard(html, "Zręczność", "76");
        assertCard(html, "Wytrzymałość", "1611");
        assertCard(html, "Pancerz", "152");
        assertCard(html, "Maksimum zdrowia", "1526");
        for (String label : new String[]{"Fizyczne", "Ogień", "Błyskawice", "Zimno", "Trucizna", "Cień"}) { assertCard(html, label, "32"); }
        assertCard(html, "Podstawowe obrażenia od broni", "0");
        assertCard(html, "Szybkość broni", "1,00");
        assertCard(html, "Szansa na trafienie krytyczne", "5,2%");
        assertCard(html, "Obrażenia od trafień krytycznych", "50,0%");
        assertCard(html, "Obrażenia zadawane odsłoniętym celom", "20,0%");
        assertCard(html, "Ciernie", "0");
        assertTrue(html.contains("+0,2% z Zręczności"));
        assertFalse(html.contains("z Inteligencji"));
        assertTrue(html.contains("name=\"additionalNonItemIntelligence\" value=\"0\""));
        assertFalse(html.contains("name=\"nonItemIntelligence\""));
        assertFalse(html.contains("id=\"necromancer-direct-hit-result\""));
        assertCard(html, "Obrażenia umiejętności z bazowej Inteligencji", "12,8%");
        assertCard(html, "Generowanie zasobów z bazowej Siły woli", "0,4%");
        assertCard(html, "Otrzymywane leczenie z bazowej Siły woli", "2,7%");
        assertCard(html, "Szansa krytyczna z bazowej Zręczności", "0,2%");
        assertCard(html, "Szansa uniku z bazowej Zręczności", "0,5%");
    }

    @Test
    void otherLevelsShouldShowOnlyVerifiedCoreProgressionAndNoInventedFullSnapshot() throws Exception {
        create(2);
        String html = get().body();
        assertCard(html, "Siła", "8");
        assertCard(html, "Inteligencja", "11");
        assertCard(html, "Siła woli", "9");
        assertCard(html, "Zręczność", "8");
        assertFalse(html.contains("<div class=\"summary-label\">Wytrzymałość"));
        assertFalse(html.contains("<div class=\"summary-label\">Maksimum zdrowia"));
        assertTrue(html.contains("Brak jawnego baseline'u gry dla tego poziomu"));
    }

    @Test
    void additionalIntelligenceShouldSaveRestartAndNotDoubleCountBaselineInKnownGolden() throws Exception {
        create(70);
        activateWeapon();
        var fields = Map.of("formAction", "save", "selectedSkillId", "krwotok", "additionalNonItemIntelligence", "921", "targetVulnerable", "false");
        var saved = post("/policz-aktualny-build", fields);
        assertEquals(200, saved.statusCode());
        assertTrue(saved.body().contains("Zapisano konfigurację Nekromanty."));
        restart();
        assertTrue(get().body().contains("name=\"additionalNonItemIntelligence\" value=\"921\""));
        var response = post("/policz-aktualny-build", Map.of("formAction", "calculate", "selectedSkillId", "krwotok", "additionalNonItemIntelligence", "921", "targetVulnerable", "false"));
        assertEquals(200, response.statusCode());
        assertCard(response.body(), "Inteligencja baseline klasy/poziomu", "79");
        assertCard(response.body(), "Efektywna inteligencja", "1000");
        assertCard(response.body(), "Normal direct hit", "2100");
        assertCard(response.body(), "Critical direct hit", "3150");
    }

    @Test
    void oldN11QueryShouldMigrateWithoutMutationOnGetOrLossOfUnrelatedFields() throws Exception {
        create(70);
        var service = heroes();
        var hero = service.requireActiveHero();
        String legacy = hero.getCurrentBuildQuery() + "&necromancerNonItemIntelligence=1000&unknown=a%2Bb";
        service.saveActiveHeroRuntimeState(legacy, hero.getRuntimeSkillLoadout());
        var response = get();
        assertTrue(response.body().contains("name=\"additionalNonItemIntelligence\" value=\"921\""));
        assertEquals(legacy, heroes().requireActiveHero().getCurrentBuildQuery());
        response = post("/policz-aktualny-build", Map.of("formAction", "save", "additionalNonItemIntelligence", "921", "selectedSkillId", ""));
        assertEquals(200, response.statusCode());
        var query = UrlEncodedFormSupport.parseQuery(heroes().requireActiveHero().getCurrentBuildQuery());
        assertEquals("921", query.get(NecromancerCurrentBuildFormQuerySupport.INTELLIGENCE));
        assertFalse(query.containsKey(NecromancerCurrentBuildFormQuerySupport.LEGACY_INTELLIGENCE));
        assertEquals("a+b", query.get("unknown"));
        restart();
        assertTrue(get().body().contains("name=\"additionalNonItemIntelligence\" value=\"921\""));
    }

    @Test
    void legacyZeroShouldShowBaselineAndAnExplicitMigrationExplanation() throws Exception {
        create(70);
        var service = heroes();
        var hero = service.requireActiveHero();
        service.saveActiveHeroRuntimeState(hero.getCurrentBuildQuery() + "&necromancerNonItemIntelligence=0", hero.getRuntimeSkillLoadout());
        var response = get();
        assertEquals(200, response.statusCode());
        assertCard(response.body(), "Inteligencja", "79");
        assertTrue(response.body().contains("odpowiada dodatkowej Intelligence 0"));
    }

    private void create(int level) throws Exception {
        assertEquals(200, post("/bohaterowie", Map.of("action", "createHero", "heroName", "Mortis", "heroClass", "NECROMANCER", "heroLevel", Integer.toString(level))).statusCode());
    }

    private void activateWeapon() {
        var items = new ItemLibraryService(new FileItemLibraryRepository(directory));
        var item = items.saveImportedItem(new ValidatedImportedItem("weapon.png", EquipmentSlot.MAIN_HAND, 1000, 0, 0, 0, 0, 0));
        heroes().setActiveHeroItem(HeroEquipmentSlot.MAIN_HAND, item.getItemId());
    }

    private HeroService heroes() { return new HeroService(new FileHeroProfileRepository(directory)); }
    private HttpResponse<String> get() throws Exception { return client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/policz-aktualny-build")).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private HttpResponse<String> post(String path, Map<String, String> fields) throws Exception {
        String body = fields.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void assertCard(String html, String label, String value) {
        String expression = "<div class=\"summary-label\">\\s*" + Pattern.quote(label) + "\\s*</div>\\s*<div class=\"summary-value\">\\s*" + Pattern.quote(value) + "\\s*</div>";
        assertTrue(Pattern.compile(expression).matcher(html).find(), label + "=" + value + "\n" + html);
    }
}
