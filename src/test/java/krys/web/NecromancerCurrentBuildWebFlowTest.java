package krys.web;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ValidatedImportedItem;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.masterworking.ItemMasterworking;
import krys.necromancer.app.NecromancerCurrentBuildCalculationService;
import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.runtime.RuntimeSkillLoadout;
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
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Goldeny przez prawdziwy HTTP, plikowy profil, mapper biblioteki i usługę N11. */
class NecromancerCurrentBuildWebFlowTest {
    @TempDir Path directory;
    private CurrentBuildWebServer server;
    private String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();
    private final RuntimeSkillKey key = NecromancerCurrentBuildCalculationService.KRWOTOK;

    @BeforeEach
    void start() throws Exception {
        startServer();
        var response = post("/bohaterowie", Map.of("action", "createHero", "heroName", "Mortis", "heroClass", "NECROMANCER", "heroLevel", "70"));
        assertEquals(200, response.statusCode());
    }

    private void startServer() throws Exception {
        server = new CurrentBuildWebServer(0, directory);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getPort();
    }

    @AfterEach
    void stop() { if (server != null) { server.close(); } }

    @Test
    void baseGoldenShouldUsePersistedCanonicalKrwotokAndActiveWeaponAverage() throws Exception {
        activateWeapon(true);
        assertSaved(postBuild(form("save", "1000", false, "krwotok")));
        assertHits(postBuild(form("calculate", "1000", false, "krwotok")), 2100, 3150);
    }

    @Test
    void vulnerableGoldenShouldUseN8BMultiplier() throws Exception {
        activateWeapon(false);
        var response = postBuild(form("calculate", "1000", true, "krwotok"));
        assertHits(response, 2520, 3780);
        assertCard(response, "Mnożnik Vulnerable", "1.2");
    }

    @Test
    void itemIntelligenceGoldenShouldFlowThroughImportedAffixMapper() throws Exception {
        activateWeapon(false);
        var item = items().saveImportedItem(intelligenceHelmet(ItemMasterworking.defaultState()));
        assertEquals(200, postBuild(Map.of("slotAction", "setActiveSlotItem:HELMET", "selectedItemId_HELMET", Long.toString(item.getItemId()))).statusCode());
        assertEquals(100, activeItemIntelligence());
        var response = postBuild(form("calculate", "900", false, "krwotok"));
        assertHits(response, 2100, 3150);
        assertCard(response, "Inteligencja poza aktywnymi itemami", "900");
        assertCard(response, "Inteligencja z aktywnych itemów", "100");
        assertCard(response, "Efektywna inteligencja", "1000");
        assertCard(response, "Mnożnik main stat", "2.625");
    }

    @Test
    void masterworkingShouldUseActualResolvedItemValueAndStillReachTheKnownGolden() throws Exception {
        activateWeapon(false);
        var item = items().saveImportedItem(intelligenceHelmet(new ItemMasterworking(25, 25)));
        postBuild(Map.of("slotAction", "setActiveSlotItem:HELMET", "selectedItemId_HELMET", Long.toString(item.getItemId())));
        double resolved = activeItemIntelligence();
        assertNotEquals(100.0, resolved, "Masterworking musi zmienić source value tego fixture");
        // Pozostałą INT dobieramy z rzeczywistego mappera do znanego goldena 1000 effective INT.
        var response = postBuild(form("calculate", Double.toString(1000.0 - resolved), false, "krwotok"));
        assertHits(response, 2100, 3150);
        assertCard(response, "Inteligencja z aktywnych itemów", java.math.BigDecimal.valueOf(resolved).stripTrailingZeros().toPlainString());
    }

    @Test
    void missingWeaponShouldProduceAnErrorWithoutSuccessfulZeroDamage() throws Exception {
        var response = postBuild(form("calculate", "1000", false, "krwotok"));
        assertError(response, "Wybierz aktywną broń z poprawnymi średnimi obrażeniami.");
    }

    @Test
    void unsupportedItemAffixesAndCriticalChanceShouldNotChangeTheKnownDirectHits() throws Exception {
        var item = items().saveImportedItem(new ValidatedImportedItem("affixes.png", EquipmentSlot.MAIN_HAND, 1000, 0, 0, 0, 0, 0,
                List.of(new ImportedItemAffix(ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 100),
                        new ImportedItemAffix(ImportedItemAffixType.ALL_DAMAGE_MULTIPLIER, 100),
                        new ImportedItemAffix(ImportedItemAffixType.DAMAGE_OVER_TIME_MULTIPLIER, 100),
                        new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_CHANCE, 100))));
        postBuild(Map.of("slotAction", "setActiveSlotItem:MAIN_HAND", "selectedItemId_MAIN_HAND", Long.toString(item.getItemId())));
        assertHits(postBuild(form("calculate", "1000", false, "krwotok")), 2100, 3150);
    }

    @Test
    void heroDetailsEditShouldPreserveSavedNecromancerContextLoadoutAndItems() throws Exception {
        activateWeapon(false);
        assertSaved(postBuild(form("save", "900", true, "krwotok")));
        var before = heroes().requireActiveHero();
        var response = post("/bohaterowie", Map.of("action", "updateHeroDetails", "heroId", "1", "heroName", "Morta", "heroLevel", "20"));
        assertEquals(200, response.statusCode());
        var after = heroes().requireActiveHero();
        assertEquals(before.getRuntimeSkillLoadout(), after.getRuntimeSkillLoadout());
        assertEquals(before.getItemSelection().getSelectedItemIdsBySlot(), after.getItemSelection().getSelectedItemIdsBySlot());
        var fields = UrlEncodedFormSupport.parseQuery(after.getCurrentBuildQuery());
        assertEquals("900", fields.get(NecromancerCurrentBuildFormQuerySupport.INTELLIGENCE));
        assertEquals("true", fields.get(NecromancerCurrentBuildFormQuerySupport.VULNERABLE));
        assertEquals("20", fields.get("level"));
        assertTrue(get("/policz-aktualny-build").body().contains("Morta"));
    }

    @Test
    void missingSkillShouldProduceAnErrorDespiteAValidWeapon() throws Exception {
        activateWeapon(false);
        assertError(postBuild(form("calculate", "1000", false, "")), "Wybierz Krwotok R1");
    }

    @Test
    void persistedUpgradesShouldStayIntactAndNeverExecuteTheBaseHit() throws Exception {
        activateWeapon(false);
        var configuration = new RuntimeSkillConfiguration(key, 1, Map.of("grupa_1", "szybkosc_uzycia"));
        persist(new RuntimeSkillLoadout("necromancer", Map.of(key, configuration), List.of(key)));
        var before = heroes().requireActiveHero();
        var page = get("/policz-aktualny-build");
        assertTrue(page.body().contains("Obecna konfiguracja Krwotoku zawiera upgrade"));
        for (String action : List.of("calculate", "save")) {
            assertError(postBuild(form(action, "1000", false, "krwotok")), "Upgrade'y Krwotoku nie są jeszcze wykonywane");
            assertStateEquals(before, heroes().requireActiveHero());
        }
    }

    @Test
    void persistedRankTwoRankZeroAndOffBarShouldNotBeRepairedByTheForm() throws Exception {
        activateWeapon(false);
        for (int rank : List.of(0, 1, 2)) {
            var configuration = new RuntimeSkillConfiguration(key, rank, Map.of());
            persist(new RuntimeSkillLoadout("necromancer", Map.of(key, configuration), rank == 2 ? List.of(key) : List.of()));
            var before = heroes().requireActiveHero();
            var response = postBuild(form("calculate", "1000", false, "krwotok"));
            assertError(response, rank == 2 ? "obecna konfiguracja ma rangę 2" : "Krwotok musi znajdować się na pasku akcji.");
            assertStateEquals(before, heroes().requireActiveHero());
        }
    }

    @Test
    void saveAndServerRestartShouldRestoreSkillQueryItemSelectionAndUi() throws Exception {
        var weapon = items().saveImportedItem(weapon(false));
        var fields = form("save", "900", true, "krwotok");
        fields.put("selectedItemId_MAIN_HAND", Long.toString(weapon.getItemId()));
        assertSaved(postBuild(fields));
        server.close();
        startServer();
        var hero = heroes().requireActiveHero();
        assertEquals(HeroClass.NECROMANCER, hero.getHeroClass());
        assertEquals(new RuntimeSkillConfiguration(key, 1, Map.of()), hero.getRuntimeSkillLoadout().configurations().get(key));
        assertEquals(List.of(key), hero.getRuntimeSkillLoadout().actionBar());
        assertEquals(weapon.getItemId(), hero.getItemSelection().getSelectedItemId(HeroEquipmentSlot.MAIN_HAND));
        var query = UrlEncodedFormSupport.parseQuery(hero.getCurrentBuildQuery());
        assertEquals("900", query.get(NecromancerCurrentBuildFormQuerySupport.INTELLIGENCE));
        assertEquals("true", query.get(NecromancerCurrentBuildFormQuerySupport.VULNERABLE));
        assertFalse(query.containsKey("selectedSkillId"));
        var response = get("/policz-aktualny-build");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("value=\"krwotok\" selected"));
        assertTrue(response.body().contains("name=\"nonItemIntelligence\" value=\"900\""));
        assertTrue(response.body().contains("name=\"targetVulnerable\" value=\"true\" checked"));
        assertTrue(response.body().contains("N11 — bazowy direct hit Krwotoku R1"));
        assertTrue(response.body().contains("N11 fixture weapon"));
        assertFalse(response.body().contains("id=\"necromancer-direct-hit-result\""));
    }

    @Test
    void transientCalculateShouldNotSaveQueryLoadoutOrPostedGlobalItemChoices() throws Exception {
        activateWeapon(false);
        var other = items().saveImportedItem(intelligenceHelmet(ItemMasterworking.defaultState()));
        var before = heroes().requireActiveHero();
        var fields = form("calculate", "1000", true, "krwotok");
        fields.put("selectedItemId_HELMET", Long.toString(other.getItemId()));
        assertHits(postBuild(fields), 2520, 3780);
        assertStateEquals(before, heroes().requireActiveHero());
        var page = get("/policz-aktualny-build");
        assertTrue(page.body().contains("value=\"\" selected>Brak umiejętności"));
        assertTrue(page.body().contains("name=\"nonItemIntelligence\" value=\"0\""));
    }

    @Test
    void saveAddAndRemoveShouldKeepOtherConfigurationsBarAndUnknownQueryFields() throws Exception {
        var other = new RuntimeSkillKey("necromancer", "mor");
        var configuration = new RuntimeSkillConfiguration(other, 15, Map.of("grupa_1", "premia"));
        persist(new RuntimeSkillLoadout("necromancer", Map.of(other, configuration), List.of(other)));
        var hero = heroes().requireActiveHero();
        heroes().saveActiveHeroRuntimeState(hero.getCurrentBuildQuery() + "&futureField=a%2Bb", hero.getRuntimeSkillLoadout());
        assertSaved(postBuild(form("save", "900", true, "krwotok")));
        var added = heroes().requireActiveHero();
        assertEquals(configuration, added.getRuntimeSkillLoadout().configurations().get(other));
        assertEquals(List.of(other, key), added.getRuntimeSkillLoadout().actionBar());
        assertEquals("a+b", UrlEncodedFormSupport.parseQuery(added.getCurrentBuildQuery()).get("futureField"));
        activateWeapon(false);
        assertHits(postBuild(form("calculate", "1000", false, "krwotok")), 2100, 3150);
        assertSaved(postBuild(form("save", "900", true, "NONE")));
        var removed = heroes().requireActiveHero().getRuntimeSkillLoadout();
        assertEquals(Map.of(other, configuration), removed.configurations());
        assertEquals(List.of(other), removed.actionBar());
    }

    @Test
    void invalidSaveShouldNotMutateQueryLoadoutOrAnyItemSlot() throws Exception {
        var weapon = items().saveImportedItem(weapon(false));
        var before = heroes().requireActiveHero();
        var fields = form("save", "NaN", true, "krwotok");
        fields.put("selectedItemId_MAIN_HAND", Long.toString(weapon.getItemId()));
        assertError(postBuild(fields), "skończoną liczbą nieujemną");
        assertStateEquals(before, heroes().requireActiveHero());
        fields.put("nonItemIntelligence", "900");
        fields.put("selectedItemId_OFF_HAND", "999999");
        assertEquals(200, postBuild(fields).statusCode());
        assertStateEquals(before, heroes().requireActiveHero());
        fields.remove("selectedItemId_OFF_HAND");
        fields.put("rank", "2");
        assertError(postBuild(fields), "nie przyjmuje edycji rang");
        assertStateEquals(before, heroes().requireActiveHero());
    }

    @Test
    void equipmentActionsShouldImmediatelySetAndClearItemsWithoutSavingTransientContext() throws Exception {
        var item = items().saveImportedItem(weapon(false));
        var before = heroes().requireActiveHero();
        var fields = form("calculate", "900", true, "krwotok");
        fields.put("slotAction", "setActiveSlotItem:MAIN_HAND");
        fields.put("selectedItemId_MAIN_HAND", Long.toString(item.getItemId()));
        assertEquals(200, postBuild(fields).statusCode());
        var after = heroes().requireActiveHero();
        assertEquals(item.getItemId(), after.getItemSelection().getSelectedItemId(HeroEquipmentSlot.MAIN_HAND));
        assertEquals(before.getCurrentBuildQuery(), after.getCurrentBuildQuery());
        assertEquals(before.getRuntimeSkillLoadout(), after.getRuntimeSkillLoadout());
        postBuild(Map.of("slotAction", "clearActiveSlotItem:MAIN_HAND"));
        assertNull(heroes().requireActiveHero().getItemSelection().getSelectedItemId(HeroEquipmentSlot.MAIN_HAND));
    }

    @Test
    void getShouldIgnoreLegacyAndNecromancerQueryOverridesAndRenderOnlyTheN11Form() throws Exception {
        var before = heroes().requireActiveHero();
        var response = get("/policz-aktualny-build?rank_HOLY_BOLT=5&necromancerNonItemIntelligence=1000&selectedSkillId=krwotok");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("name=\"nonItemIntelligence\" value=\"0\""));
        assertTrue(response.body().contains("PRE_TARGET_MITIGATION"));
        assertTrue(response.body().contains("80% z katalogu"));
        for (String field : List.of("selectedPaladinOathId", "initialAnimus", "skillIdToAdd", "rank_HOLY_BOLT", "simulationStepCount")) {
            assertFalse(response.body().contains("name=\"" + field + "\""), field);
        }
        assertStateEquals(before, heroes().requireActiveHero());
    }

    private void activateWeapon(boolean detailed) throws Exception {
        var item = items().saveImportedItem(weapon(detailed));
        assertEquals(200, postBuild(Map.of("slotAction", "setActiveSlotItem:MAIN_HAND", "selectedItemId_MAIN_HAND", Long.toString(item.getItemId()))).statusCode());
        assertEquals(item.getItemId(), heroes().requireActiveHero().getItemSelection().getSelectedItemId(HeroEquipmentSlot.MAIN_HAND));
    }

    private static ValidatedImportedItem weapon(boolean detailed) {
        var details = detailed ? new ItemImportDetails("N11 fixture weapon", "Miecz", "Legendarny", false, EquipmentSlot.MAIN_HAND,
                null, 3150L, 900L, 1100L, 1000L, 1.1, "") : new ItemImportDetails("N11 fixture weapon", "Miecz", "Legendarny", false, EquipmentSlot.MAIN_HAND,
                null, null, null, null, null, null, "");
        return new ValidatedImportedItem("weapon.png", EquipmentSlot.MAIN_HAND, detailed ? 3150 : 1000, 0, 0, 0, 0, 0, List.of(), "", details);
    }

    private static ValidatedImportedItem intelligenceHelmet(ItemMasterworking masterworking) {
        return new ValidatedImportedItem("helmet.png", EquipmentSlot.HELMET, 0, 0, 100, 0, 0, 0,
                List.of(new ImportedItemAffix(ImportedItemAffixType.INTELLIGENCE, 100)), "", ItemImportDetails.empty(), List.of(), masterworking);
    }

    private double activeItemIntelligence() {
        return items().resolveEffectiveCurrentBuild(new CurrentBuildImportableStats(0, 0, 0, 0, 0, 0), heroes().requireActiveHero().getItemSelection()).getActiveItemsContribution().getIntelligence();
    }

    private void persist(RuntimeSkillLoadout loadout) { var service = heroes(); service.saveActiveHeroRuntimeState(service.requireActiveHero().getCurrentBuildQuery(), loadout); }
    private HeroService heroes() { return new HeroService(new FileHeroProfileRepository(directory)); }
    private ItemLibraryService items() { return new ItemLibraryService(new FileItemLibraryRepository(directory)); }
    private static LinkedHashMap<String, String> form(String action, String intelligence, boolean vulnerable, String skill) {
        return new LinkedHashMap<>(Map.of("formAction", action, "nonItemIntelligence", intelligence, "targetVulnerable", Boolean.toString(vulnerable), "selectedSkillId", skill));
    }
    private HttpResponse<String> postBuild(Map<String, String> fields) throws Exception { return post("/policz-aktualny-build", fields); }
    private static void assertSaved(HttpResponse<String> response) { assertEquals(200, response.statusCode()); assertTrue(response.body().contains("Zapisano konfigurację Nekromanty."), response.body()); }
    private static void assertError(HttpResponse<String> response, String message) { assertEquals(200, response.statusCode()); assertTrue(response.body().contains(message), response.body()); assertFalse(response.body().contains("id=\"necromancer-direct-hit-result\"")); }
    private static void assertHits(HttpResponse<String> response, long normal, long critical) { assertEquals(200, response.statusCode()); assertTrue(response.body().contains("id=\"necromancer-direct-hit-result\""), response.body()); assertCard(response, "Normal direct hit", Long.toString(normal)); assertCard(response, "Critical direct hit", Long.toString(critical)); }
    private static void assertCard(HttpResponse<String> response, String label, String value) { assertTrue(response.body().contains(CurrentBuildCalculationSectionsRenderer.renderSummaryCard(label, value)), label + "=" + value + "\n" + response.body()); }
    private static void assertStateEquals(HeroProfile before, HeroProfile after) { assertEquals(before.getCurrentBuildQuery(), after.getCurrentBuildQuery()); assertEquals(before.getRuntimeSkillLoadout(), after.getRuntimeSkillLoadout()); assertEquals(before.getItemSelection().getSelectedItemIdsBySlot(), after.getItemSelection().getSelectedItemIdsBySlot()); }
    private HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private HttpResponse<String> post(String path, Map<String, String> fields) throws Exception {
        var body = fields.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
