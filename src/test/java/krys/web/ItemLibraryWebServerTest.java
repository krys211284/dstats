package krys.web;

import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje SSR biblioteki itemów: zapis, przegląd, założenie itemu i sekcję active items na current build. */
class ItemLibraryWebServerTest {
    private CurrentBuildWebServer webServer;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        Path tempDirectory = Files.createTempDirectory("item-library-web");
        webServer = new CurrentBuildWebServer(0, tempDirectory);
        webServer.start();
        httpClient = HttpClient.newHttpClient();
        baseUrl = "http://127.0.0.1:" + webServer.getPort();
    }

    @AfterEach
    void tearDown() {
        if (webServer != null) {
            webServer.close();
        }
    }

    @Test
    void shouldRenderHeroWarningWhenNoActiveHeroExists() throws Exception {
        HttpResponse<String> response = sendGet("/biblioteka-itemow");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Brak aktywnego bohatera"));
        assertTrue(response.body().contains("Biblioteka itemów pozostaje wspólna"));
        assertTrue(response.body().contains("Przejdź do modułu Bohaterowie"));
    }

    @Test
    void shouldSaveItemsRenderLibraryAndUseActiveItemOnCurrentBuildPage() throws Exception {
        createHero("Bibliotekarz", "13");
        primeHeroBuildQuery(buildCurrentBuildQuery());
        HttpResponse<String> firstSave = sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", "shield-a.png",
                "slot", "OFF_HAND",
                "weaponDamage", "0",
                "strength", "114",
                "intelligence", "0",
                "thorns", "494",
                "blockChance", "20",
                "retributionChance", "0",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, firstSave.statusCode());
        assertTrue(firstSave.body().contains("Zapisano item w bibliotece"));
        assertTrue(firstSave.body().contains("Ręka dodatkowa / shield-a.png"));
        assertTrue(firstSave.body().contains("Item zapisany do biblioteki"));
        assertTrue(firstSave.body().contains("Pracujesz teraz na bohaterze Bibliotekarz"));
        assertTrue(firstSave.body().contains("Załóż bohaterowi: Tarcza"));
        assertTrue(firstSave.body().contains("Wróć do aktualnego buildu"));
        assertTrue(firstSave.body().contains("<th>Item</th>"));
        assertTrue(firstSave.body().contains("<th>Slot / typ</th>"));
        assertTrue(firstSave.body().contains("<th>Affixy</th>"));

        HttpResponse<String> secondSave = sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", "shield-b.png",
                "slot", "OFF_HAND",
                "weaponDamage", "0",
                "strength", "120",
                "intelligence", "0",
                "thorns", "500",
                "blockChance", "22",
                "retributionChance", "0",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, secondSave.statusCode());
        assertTrue(secondSave.body().contains("Ręka dodatkowa / shield-a.png"));
        assertTrue(secondSave.body().contains("Ręka dodatkowa / shield-b.png"));
        assertTrue(secondSave.body().contains("2 itemy"));

        HttpResponse<String> activateResponse = sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "activateItem",
                "itemId", "2",
                "heroSlot", "OFF_HAND",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, activateResponse.statusCode());
        assertTrue(activateResponse.body().contains("Założono item Ręka dodatkowa / shield-b.png w slocie Tarcza bohatera Bibliotekarz."));
        assertTrue(activateResponse.body().contains("class=\"status-badge status-active\">Założony</span>"));
        assertTrue(activateResponse.body().contains("class=\"icon-action assign-action assign-action-selected\""));
        assertTrue(activateResponse.body().contains("aria-label=\"Item Ręka dodatkowa / shield-b.png jest już założony w slocie Tarcza\""));
        assertTrue(activateResponse.body().contains("class=\"icon-action assign-action\" title=\"Zmień w slocie: Tarcza\""));
        assertTrue(activateResponse.body().contains("aria-label=\"Zmień item w slocie Tarcza na Ręka dodatkowa / shield-a.png\""));
        assertFalse(activateResponse.body().contains(">Załóż bohaterowi:"));
        assertFalse(activateResponse.body().contains(">Zmień w slocie:"));
        assertTrue(activateResponse.body().contains("class=\"icon-action edit-action\""));
        assertTrue(activateResponse.body().contains("aria-label=\"Edytuj item Ręka dodatkowa / shield-b.png\""));
        assertTrue(activateResponse.body().contains("class=\"icon-action delete-action\""));
        assertTrue(activateResponse.body().contains("aria-label=\"Usuń item Ręka dodatkowa / shield-b.png\""));
        assertFalse(activateResponse.body().contains("Pokaż slot w current build"));
        assertTrue(activateResponse.body().contains("Ręka dodatkowa / shield-b.png"));

        HttpResponse<String> currentBuildResponse = sendGet("/policz-aktualny-build?" + buildCurrentBuildQuery());
        assertEquals(200, currentBuildResponse.statusCode());
        assertTrue(currentBuildResponse.body().contains("Ekwipunek aktualnego buildu"));
        assertTrue(currentBuildResponse.body().contains("Ręka dodatkowa / shield-b.png"));
        assertTrue(currentBuildResponse.body().contains("Efektywne staty do obliczeń"));
        assertTrue(currentBuildResponse.body().contains(">120<"));
        assertTrue(currentBuildResponse.body().contains("Do runtime trafiają: obrażenia broni=200, siła=150"));
    }

    @Test
    void shouldKeepCurrentBuildQueryWhenNavigatingFromLibraryToItemImport() throws Exception {
        createHero("Bibliotekarz", "13");
        primeHeroBuildQuery(buildCurrentBuildQuery());
        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow?" + buildCurrentBuildQuery());

        assertEquals(200, libraryResponse.statusCode());
        assertTrue(libraryResponse.body().contains("href=\"/importuj-item-ze-screena?level=13&amp;questSkillPoints=0&amp;weaponDamage=200&amp;strength=30"));

        HttpResponse<String> importResponse = sendGet("/importuj-item-ze-screena?" + buildCurrentBuildQuery());

        assertEquals(200, importResponse.statusCode());
        assertTrue(importResponse.body().contains("action=\"/importuj-item-ze-screena?level=13&amp;questSkillPoints=0&amp;weaponDamage=200&amp;strength=30"));
        assertTrue(importResponse.body().contains("blockChance=10&amp;retributionChance=15&amp;horizonSeconds=10"));
    }

    @Test
    void shouldRenderEmptyStateWithImportLinkAndDeleteMessage() throws Exception {
        createHero("Bibliotekarz", "13");
        primeHeroBuildQuery(buildCurrentBuildQuery());
        HttpResponse<String> emptyResponse = sendGet("/biblioteka-itemow?" + buildCurrentBuildQuery());

        assertEquals(200, emptyResponse.statusCode());
        assertTrue(emptyResponse.body().contains("Biblioteka jest pusta"));
        assertTrue(emptyResponse.body().contains("Zaimportuj pierwszy item"));
        assertTrue(emptyResponse.body().contains("href=\"/importuj-item-ze-screena?level=13&amp;questSkillPoints=0&amp;weaponDamage=200&amp;strength=30"));

        HttpResponse<String> saveResponse = sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", "empty-state.png",
                "slot", "OFF_HAND",
                "weaponDamage", "0",
                "strength", "10",
                "intelligence", "0",
                "thorns", "0",
                "blockChance", "0",
                "retributionChance", "0",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, saveResponse.statusCode());

        HttpResponse<String> deleteResponse = sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "deleteItem",
                "itemId", "1",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));

        assertEquals(200, deleteResponse.statusCode());
        assertTrue(deleteResponse.body().contains("Usunięto item z biblioteki."));
        assertTrue(deleteResponse.body().contains("Biblioteka jest pusta"));
        assertTrue(deleteResponse.body().contains("Importuj item ze screena"));
    }

    @Test
    void shouldOpenEditFormAndUpdateExistingActiveItemWithoutChangingItemId() throws Exception {
        createHero("Edytor", "13");
        primeHeroBuildQuery(buildCurrentBuildQuery());
        HttpResponse<String> importResponse = sendUrlEncodedPost("/importuj-item-ze-screena", shieldImportFields(
                "tarcza-edit.png",
                "Tarcza Edytora",
                "STRENGTH",
                "114",
                true,
                true
        ));
        assertEquals(200, importResponse.statusCode());

        HttpResponse<String> activateResponse = sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "activateItem",
                "itemId", "1",
                "heroSlot", "OFF_HAND",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, activateResponse.statusCode());

        HttpResponse<String> editForm = sendGet("/biblioteka-itemow/edytuj?itemId=1");

        assertEquals(200, editForm.statusCode());
        assertTrue(editForm.body().contains("Edytuj zapisany item"));
        assertTrue(editForm.body().contains("name=\"itemId\" value=\"1\""));
        assertTrue(editForm.body().contains("name=\"affixType_0\""));
        assertTrue(editForm.body().contains("name=\"affixValue_0\" value=\"114\""));
        assertTrue(editForm.body().contains("name=\"affixGreater_0\" value=\"true\" checked"));
        assertTrue(editForm.body().contains("value=\"inner-calm\""));
        assertTrue(editForm.body().contains("selected>Aspekt Wewnętrznego Spokoju"));
        assertTrue(editForm.body().contains("Tarcza Edytora"));

        Map<String, String> updateFields = shieldUpdateFields("1", "tarcza-edit.png", "Tarcza Edytora", "120", true, "inner-calm");
        HttpResponse<String> updateResponse = sendUrlEncodedPost("/biblioteka-itemow/edytuj", updateFields);

        assertEquals(200, updateResponse.statusCode());
        assertTrue(updateResponse.body().contains("Zapisano zmiany itemu."));
        assertTrue(updateResponse.body().contains("name=\"itemId\" value=\"1\""));
        assertTrue(updateResponse.body().contains("name=\"affixValue_0\" value=\"120\""));
        assertTrue(updateResponse.body().contains("name=\"affixGreater_0\" value=\"true\" checked"));

        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");

        assertEquals(200, libraryResponse.statusCode());
        assertTrue(libraryResponse.body().contains("1 item"));
        assertTrue(libraryResponse.body().contains("* +120 siły"));
        assertTrue(libraryResponse.body().contains("★ +120 siły"));
        assertFalse(libraryResponse.body().contains("+114 siły"));
        assertTrue(libraryResponse.body().contains("Wybrany aspekt: Aspekt Wewnętrznego Spokoju"));
        assertTrue(libraryResponse.body().contains("class=\"status-badge status-active\">Założony</span>"));

        HttpResponse<String> currentBuildResponse = sendGet("/policz-aktualny-build?" + buildCurrentBuildQuery());
        assertEquals(200, currentBuildResponse.statusCode());
        assertTrue(currentBuildResponse.body().contains("Ręka dodatkowa / tarcza-edit.png"));
        assertTrue(currentBuildResponse.body().contains("Do runtime trafiają: obrażenia broni=200, siła=150"));

        Map<String, String> removeAffixFields = shieldUpdateFields("1", "tarcza-edit.png", "Tarcza Edytora", "120", true, "inner-calm");
        removeAffixFields.put("affixCount", "0");
        HttpResponse<String> removeResponse = sendUrlEncodedPost("/biblioteka-itemow/edytuj", removeAffixFields);
        assertEquals(200, removeResponse.statusCode());

        HttpResponse<String> afterRemove = sendGet("/biblioteka-itemow");
        assertFalse(afterRemove.body().contains("+120 siły"));
        assertTrue(afterRemove.body().contains("Wybrany aspekt: Aspekt Wewnętrznego Spokoju"));
    }

    @Test
    void shouldShowErrorWhenEditingMissingItem() throws Exception {
        HttpResponse<String> response = sendGet("/biblioteka-itemow/edytuj?itemId=999");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Nie znaleziono itemu o podanym id w bibliotece."));
    }

    @Test
    void shouldFilterLibraryByStructuredItemDataAndPreserveFilterUi() throws Exception {
        createHero("Filtrator", "13");
        sendUrlEncodedPost("/importuj-item-ze-screena", shieldImportFields("tarcza-a.png", "Tarcza Alfa", "STRENGTH", "114", false, true));
        sendUrlEncodedPost("/importuj-item-ze-screena", shieldImportFields("tarcza-b.png", "Tarcza Beta", "THORNS", "494", false, false));
        sendUrlEncodedPost("/importuj-item-ze-screena", bootsImportFields("buty-speed.png", "Buty Szybkie", true));
        sendUrlEncodedPost("/biblioteka-itemow", Map.of(
                "action", "activateItem",
                "itemId", "1",
                "heroSlot", "OFF_HAND",
                "currentBuildQuery", ""
        ));

        HttpResponse<String> all = sendGet("/biblioteka-itemow");
        assertTrue(all.body().contains("Filtry biblioteki"));
        assertTrue(all.body().contains("Znaleziono 3 itemy"));
        assertTrue(all.body().contains("name=\"q\""));
        assertTrue(all.body().contains("Wyczyść filtry"));

        assertOnlyContains("/biblioteka-itemow?slot=BOOTS", "Buty / buty-speed.png", "Ręka dodatkowa / tarcza-a.png");
        assertOnlyContains("/biblioteka-itemow?type=" + encode("Tarcza"), "Ręka dodatkowa / tarcza-a.png", "Buty / buty-speed.png");
        assertOnlyContains("/biblioteka-itemow?status=used", "Ręka dodatkowa / tarcza-a.png", "Ręka dodatkowa / tarcza-b.png");
        assertOnlyContains("/biblioteka-itemow?status=unused", "Ręka dodatkowa / tarcza-b.png", "Ręka dodatkowa / tarcza-a.png");
        assertOnlyContains("/biblioteka-itemow?aspect=inner-calm", "Ręka dodatkowa / tarcza-a.png", "Ręka dodatkowa / tarcza-b.png");
        assertOnlyContains("/biblioteka-itemow?aspect=__NONE__", "Ręka dodatkowa / tarcza-b.png", "Ręka dodatkowa / tarcza-a.png");
        assertOnlyContains("/biblioteka-itemow?affix=THORNS", "Ręka dodatkowa / tarcza-b.png", "Ręka dodatkowa / tarcza-a.png");
        assertOnlyContains("/biblioteka-itemow?greater=true", "Buty / buty-speed.png", "Ręka dodatkowa / tarcza-a.png");
        assertOnlyContains("/biblioteka-itemow?q=Alfa", "Ręka dodatkowa / tarcza-a.png", "Ręka dodatkowa / tarcza-b.png");
        assertOnlyContains("/biblioteka-itemow?q=tarcza-b", "Ręka dodatkowa / tarcza-b.png", "Ręka dodatkowa / tarcza-a.png");
        assertOnlyContains("/biblioteka-itemow?q=" + encode("Wewnętrznego"), "Ręka dodatkowa / tarcza-a.png", "Ręka dodatkowa / tarcza-b.png");
        assertOnlyContains("/biblioteka-itemow?q=" + encode("Ciernie"), "Ręka dodatkowa / tarcza-b.png", "Ręka dodatkowa / tarcza-a.png");

        HttpResponse<String> selected = sendGet("/biblioteka-itemow?slot=BOOTS&greater=true");
        assertTrue(selected.body().contains("<option value=\"BOOTS\" selected>Buty</option>"));
        assertTrue(selected.body().contains("name=\"greater\" value=\"true\" checked"));

        HttpResponse<String> cleared = sendGet("/biblioteka-itemow");
        assertTrue(cleared.body().contains("Ręka dodatkowa / tarcza-a.png"));
        assertTrue(cleared.body().contains("Ręka dodatkowa / tarcza-b.png"));
        assertTrue(cleared.body().contains("Buty / buty-speed.png"));
    }

    private void createHero(String heroName, String heroLevel) throws Exception {
        HttpResponse<String> response = sendUrlEncodedPost("/bohaterowie", Map.of(
                "action", "createHero",
                "heroName", heroName,
                "heroClass", "PALADIN",
                "heroLevel", heroLevel
        ));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Utworzono bohatera " + heroName + "."));
    }

    private void primeHeroBuildQuery(String currentBuildQuery) throws Exception {
        HttpResponse<String> response = sendGet("/policz-aktualny-build?" + currentBuildQuery);
        assertEquals(200, response.statusCode());
    }

    private void assertOnlyContains(String path, String expected, String forbidden) throws Exception {
        HttpResponse<String> response = sendGet(path);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains(expected), path);
        assertFalse(response.body().contains(forbidden), path);
    }

    private static Map<String, String> shieldImportFields(String sourceImageName,
                                                          String itemName,
                                                          String affixType,
                                                          String affixValue,
                                                          boolean greaterAffix,
                                                          boolean selectedAspect) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("sourceImageName", sourceImageName);
        fields.put("slot", "OFF_HAND");
        fields.put("weaponDamage", "0");
        fields.put("fullItemRead", FullItemReadFormCodec.encode(new FullItemRead(
                itemName,
                "Tarcza",
                "Legendarny",
                "Moc przedmiotu: 800",
                "1 131 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.ITEM_NAME, itemName),
                        new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Tarcza"),
                        new FullItemReadLine(FullItemReadLineType.IMPLICIT, "20,0% szansy na blok [20,0]%"),
                        new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%")
                )
        )));
        fields.put("currentBuildQuery", "");
        fields.put("formAction", "confirmItem");
        fields.put("selectedAspectId", selectedAspect ? "inner-calm" : "");
        fields.put("affixCount", "1");
        fields.put("affixType_0", affixType);
        fields.put("affixValue_0", affixValue);
        if (greaterAffix) {
            fields.put("affixGreater_0", "true");
        }
        return fields;
    }

    private static Map<String, String> bootsImportFields(String sourceImageName, String itemName, boolean greaterAffix) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("sourceImageName", sourceImageName);
        fields.put("slot", "BOOTS");
        fields.put("weaponDamage", "0");
        fields.put("fullItemRead", FullItemReadFormCodec.encode(new FullItemRead(
                itemName,
                "Buty",
                "Legendarny",
                "Moc przedmiotu: 800",
                "354 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.ITEM_NAME, itemName),
                        new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Buty"),
                        new FullItemReadLine(FullItemReadLineType.SOCKET, "2 gniazda")
                )
        )));
        fields.put("currentBuildQuery", "");
        fields.put("formAction", "confirmItem");
        fields.put("affixCount", "1");
        fields.put("affixType_0", "MOVEMENT_SPEED");
        fields.put("affixValue_0", "12.5");
        if (greaterAffix) {
            fields.put("affixGreater_0", "true");
        }
        return fields;
    }

    private static Map<String, String> shieldUpdateFields(String itemId,
                                                          String sourceImageName,
                                                          String itemName,
                                                          String strength,
                                                          boolean greaterAffix,
                                                          String selectedAspectId) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("action", "updateItem");
        fields.put("itemId", itemId);
        fields.put("sourceImageName", sourceImageName);
        fields.put("slot", "OFF_HAND");
        fields.put("weaponDamage", "0");
        fields.put("fullItemRead", FullItemReadFormCodec.encode(new FullItemRead(
                itemName,
                "Tarcza",
                "Legendarny",
                "Moc przedmiotu: 800",
                "1 131 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.ITEM_NAME, itemName),
                        new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Tarcza"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 siły"),
                        new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%")
                )
        )));
        fields.put("selectedAspectId", selectedAspectId);
        fields.put("affixCount", "1");
        fields.put("affixType_0", "STRENGTH");
        fields.put("affixValue_0", strength);
        if (greaterAffix) {
            fields.put("affixGreater_0", "true");
        }
        return fields;
    }

    private static String buildCurrentBuildQuery() {
        return "level=13&questSkillPoints=0&weaponDamage=200&strength=30&intelligence=11&thorns=70&blockChance=10&retributionChance=15&horizonSeconds=10"
                + "&rank_BRANDISH=0&choiceUpgrade_BRANDISH=NONE"
                + "&rank_HOLY_BOLT=0&choiceUpgrade_HOLY_BOLT=NONE"
                + "&rank_CLASH=0&choiceUpgrade_CLASH=NONE"
                + "&rank_ADVANCE=5&baseUpgrade_ADVANCE=true&choiceUpgrade_ADVANCE=RIGHT"
                + "&actionBar1=ADVANCE&actionBar2=NONE&actionBar3=NONE&actionBar4=NONE";
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendUrlEncodedPost(String path, Map<String, String> fields) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(toFormBody(fields), StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String toFormBody(Map<String, String> formFields) {
        StringJoiner body = new StringJoiner("&");
        for (Map.Entry<String, String> entry : formFields.entrySet()) {
            body.add(encode(entry.getKey()) + "=" + encode(entry.getValue()));
        }
        return body.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
