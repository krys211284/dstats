package krys.web;

import krys.hero.HeroClass;
import krys.item.*;
import krys.itemimport.*;
import krys.itemlibrary.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class ItemTypeWebFlowTest {
    @TempDir Path directory;
    CurrentBuildWebServer server;
    String baseUrl;
    final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach void start() throws Exception { server = new CurrentBuildWebServer(0, directory); server.start(); baseUrl = "http://127.0.0.1:" + server.getPort(); }
    @AfterEach void stop() { server.close(); }

    @Test
    void unknownDraftShouldSelectEmptyTypeAndSlotInsteadOfHelmet() {
        var form = new ItemImportEditableForm("unknown.png", "", "0", "0", "0", "0", "0", "0", FullItemRead.empty(), List.of(), "",
                new ItemImportDetails("Unknown", "Nieznany <typ>", "UNIQUE", false, null, null, null, null, null, null, null, ""));
        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(List.of(), null, "", null, "", "", form, List.of()));
        assertTrue(html.contains("<select name=\"itemType\""));
        assertFalse(html.contains("<input type=\"text\" name=\"itemType\""));
        assertTrue(html.contains("<option value=\"\" selected>UNKNOWN / wybierz typ itemu</option>"));
        assertTrue(html.contains("<option value=\"\" selected>UNKNOWN / wybierz slot</option>"));
        assertFalse(html.contains("<option value=\"HELMET\" selected>"));
        assertTrue(html.contains("Nieznany &lt;typ&gt;"));
        for (ItemType type : ItemTypeRegistry.selectableTypes()) { assertTrue(html.contains("value=\"" + type.getTechnicalId() + "\" data-equipment-slot=\"" + type.getEquipmentSlot() + "\"")); }
    }

    @Test
    void unknownTypeAndIncompatibleSlotShouldNotCreateAnItemThroughHttp() throws Exception {
        create(HeroClass.NECROMANCER);
        for (Map<String,String> identity : List.of(Map.of("itemType", "", "slot", ""), Map.of("itemType", "Nieznany typ", "slot", "HELMET"),
                Map.of("itemType", "SCYTHE_TWO_HANDED", "slot", "HELMET"))) {
            var fields = new HashMap<>(identity); fields.put("action", "saveImportedItem"); fields.put("sourceImageName", "olsnienie.png");
            var response = post("/biblioteka-itemow", fields);
            assertEquals(200, response.statusCode()); assertTrue(response.body().contains("panel-error"));
            assertTrue(library().getSavedItems().isEmpty());
        }
    }

    @ParameterizedTest
    @EnumSource(HeroClass.class)
    void currentBuildAndLibraryShouldRejectBothOrdersAndGlobalSaveWithoutRemovingItems(HeroClass heroClass) throws Exception {
        create(heroClass);
        long scythe = importItem("SCYTHE_TWO_HANDED", "MAIN_HAND");
        long shield = importItem("SHIELD", "OFF_HAND");
        activate(HeroEquipmentSlot.OFF_HAND, shield);
        var before = heroes().requireActiveHero().getItemSelection().getSelectedItemIdsBySlot();
        var rejected = post("/policz-aktualny-build", Map.of("slotAction", "setActiveSlotItem:MAIN_HAND", "selectedItemId_MAIN_HAND", Long.toString(scythe)));
        assertTrue(rejected.body().contains("Broń dwuręczna blokuje OFF_HAND"));
        assertEquals(before, heroes().requireActiveHero().getItemSelection().getSelectedItemIdsBySlot());
        rejected = post("/biblioteka-itemow", Map.of("action", "activateItem", "heroSlot", "MAIN_HAND", "itemId", Long.toString(scythe)));
        assertTrue(rejected.body().contains("Broń dwuręczna blokuje OFF_HAND"));
        rejected = post("/policz-aktualny-build", Map.of("formAction", "save", "selectedItemId_MAIN_HAND", Long.toString(scythe), "selectedItemId_OFF_HAND", Long.toString(shield)));
        assertTrue(rejected.body().contains("Broń dwuręczna blokuje OFF_HAND"));
        assertEquals(before, heroes().requireActiveHero().getItemSelection().getSelectedItemIdsBySlot());
        post("/policz-aktualny-build", Map.of("slotAction", "clearActiveSlotItem:OFF_HAND"));
        activate(HeroEquipmentSlot.MAIN_HAND, scythe);
        assertTrue(get("/policz-aktualny-build").body().contains("data-off-hand-blocked"));
        rejected = post("/policz-aktualny-build", Map.of("slotAction", "setActiveSlotItem:OFF_HAND", "selectedItemId_OFF_HAND", Long.toString(shield)));
        assertTrue(rejected.body().contains("Broń dwuręczna blokuje OFF_HAND"));
        assertNull(heroes().requireActiveHero().getItemSelection().getSelectedItemId(HeroEquipmentSlot.OFF_HAND));
        assertEquals(2, library().getSavedItems().size());
        assertEquals(ItemType.SCYTHE_TWO_HANDED, library().requireItem(scythe).getDetails().getResolvedItemType());
        assertTrue(get("/biblioteka-itemow").body().contains("Kosa dwuręczna"));
    }

    @Test
    void editingAnEquippedSwordIntoTwoHandedShouldBeRejectedWhenShieldIsActive() throws Exception {
        create(HeroClass.NECROMANCER);
        long sword = importItem("SWORD", "MAIN_HAND"); long shield = importItem("SHIELD", "OFF_HAND");
        activate(HeroEquipmentSlot.MAIN_HAND, sword); activate(HeroEquipmentSlot.OFF_HAND, shield);
        var response = post("/biblioteka-itemow/edytuj", Map.of("itemId", Long.toString(sword), "slot", "MAIN_HAND", "itemType", "SCYTHE_TWO_HANDED"));
        assertEquals(200, response.statusCode()); assertTrue(response.body().contains("Broń dwuręczna blokuje OFF_HAND"));
        assertEquals(ItemType.SWORD, library().requireItem(sword).getDetails().getResolvedItemType());
    }

    @ParameterizedTest
    @EnumSource(HeroClass.class)
    void malformedPersistedEquipmentShouldShowAnErrorWithoutRepairAndAllowExplicitRemoval(HeroClass heroClass) throws Exception {
        create(heroClass);
        long scythe = importItem("SCYTHE_TWO_HANDED", "MAIN_HAND"); long shield = importItem("SHIELD", "OFF_HAND");
        var invalid = new HeroItemSelection(Map.of(HeroEquipmentSlot.MAIN_HAND, scythe, HeroEquipmentSlot.OFF_HAND, shield));
        heroes().replaceActiveHeroItemSelection(invalid);
        var response = get("/policz-aktualny-build");
        assertEquals(200, response.statusCode()); assertTrue(response.body().contains("Broń dwuręczna blokuje OFF_HAND"));
        assertEquals(invalid.getSelectedItemIdsBySlot(), heroes().requireActiveHero().getItemSelection().getSelectedItemIdsBySlot());
        assertTrue(response.body().contains("value=\"clearActiveSlotItem:OFF_HAND\""));
        assertFalse(response.body().contains("id=\"necromancer-direct-hit-result\""));
        post("/policz-aktualny-build", Map.of("slotAction", "clearActiveSlotItem:OFF_HAND"));
        assertNull(heroes().requireActiveHero().getItemSelection().getSelectedItemId(HeroEquipmentSlot.OFF_HAND));
        assertEquals(2, library().getSavedItems().size());
    }

    private void create(HeroClass type) throws Exception { assertEquals(200, post("/bohaterowie", Map.of("action", "createHero", "heroName", "Mortis", "heroClass", type.name(), "heroLevel", "70")).statusCode()); }
    private long importItem(String type, String slot) throws Exception {
        var response = post("/biblioteka-itemow", Map.of("action", "saveImportedItem", "sourceImageName", "olsnienie.png", "itemName", type,
                "itemType", type, "slot", slot, "itemRarity", "UNIQUE", "isAncient", "true", "itemPower", "900"));
        assertEquals(200, response.statusCode()); assertTrue(response.body().contains("Zapisano item w bibliotece"));
        return library().getSavedItems().stream().filter(item -> item.getDetails().getResolvedItemType().name().equals(type)).findFirst().orElseThrow().getItemId();
    }
    private void activate(HeroEquipmentSlot slot, long id) throws Exception { var response = post("/biblioteka-itemow", Map.of("action", "activateItem", "heroSlot", slot.name(), "itemId", Long.toString(id))); assertEquals(200, response.statusCode()); assertEquals(id, heroes().requireActiveHero().getItemSelection().getSelectedItemId(slot)); }
    private ItemLibraryService library() { return new ItemLibraryService(new FileItemLibraryRepository(directory)); }
    private HeroService heroes() { return new HeroService(new FileHeroProfileRepository(directory)); }
    private HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create(baseUrl+path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private HttpResponse<String> post(String path, Map<String,String> fields) throws Exception {
        String body = fields.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl+path)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
