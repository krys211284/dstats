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
        assertFalse(currentBuildResponse.body().contains("Efektywne staty do obliczeń"));
        assertTrue(currentBuildResponse.body().contains("Statystyki bohatera"));
        assertTrue(currentBuildResponse.body().contains("Techniczne wejście runtime"));
        assertTrue(currentBuildResponse.body().contains(">120<"));
        assertTrue(currentBuildResponse.body().contains(runtimeInputCard("Obrażenia broni", "0", "Brak aktywnej broni")));
        assertTrue(currentBuildResponse.body().contains(runtimeInputCard("Siła", "120", "Aktywne itemy")));
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
        assertTrue(editForm.body().contains("Ręczna edycja itemu"));
        assertTrue(editForm.body().contains("Dane tarczy"));
        assertFalse(editForm.body().contains("Dane broni"));
        assertTrue(editForm.body().contains("Aspekt / efekt"));
        assertTrue(editForm.body().contains("Zakres rolla"));
        assertTrue(editForm.body().contains("item-affix-add-grid"));
        assertFalse(editForm.body().contains("Odczyt OCR / źródło"));
        assertFalse(editForm.body().contains("Odczyt OCR efektu"));
        assertFalse(editForm.body().contains("Dane itemu zapisane w bibliotece"));
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
        assertFalse(libraryResponse.body().contains("* +120 siły"));
        assertTrue(libraryResponse.body().contains("★ +120 siły"));
        assertFalse(libraryResponse.body().contains("+114 siły"));
        assertTrue(libraryResponse.body().contains("Aspekt Wewnętrznego Spokoju"));
        assertFalse(libraryResponse.body().contains("Wybrany aspekt: Aspekt Wewnętrznego Spokoju"));
        assertTrue(libraryResponse.body().contains("class=\"status-badge status-active\">Założony</span>"));

        HttpResponse<String> currentBuildResponse = sendGet("/policz-aktualny-build?" + buildCurrentBuildQuery());
        assertEquals(200, currentBuildResponse.statusCode());
        assertTrue(currentBuildResponse.body().contains("Tarcza Edytora"));
        assertTrue(currentBuildResponse.body().contains(runtimeInputCard("Obrażenia broni", "0", "Brak aktywnej broni")));
        assertTrue(currentBuildResponse.body().contains(runtimeInputCard("Siła", "120", "Aktywne itemy")));

        Map<String, String> removeAffixFields = shieldUpdateFields("1", "tarcza-edit.png", "Tarcza Edytora", "120", true, "inner-calm");
        removeAffixFields.put("affixCount", "0");
        HttpResponse<String> removeResponse = sendUrlEncodedPost("/biblioteka-itemow/edytuj", removeAffixFields);
        assertEquals(200, removeResponse.statusCode());

        HttpResponse<String> afterRemove = sendGet("/biblioteka-itemow");
        assertFalse(afterRemove.body().contains("+120 siły"));
        assertTrue(afterRemove.body().contains("Aspekt Wewnętrznego Spokoju"));
        assertFalse(afterRemove.body().contains("Wybrany aspekt: Aspekt Wewnętrznego Spokoju"));
    }

    @Test
    void shouldEditSavedVerathielFromCanonicalDetailsWithoutLosingWeaponAndAffixRanges() throws Exception {
        createHero("Verathiel", "13");
        HttpResponse<String> importResponse = sendUrlEncodedPost("/importuj-item-ze-screena", verathielImportFields());
        assertEquals(200, importResponse.statusCode());
        assertTrue(importResponse.body().contains("Zatwierdzony item zapisany do biblioteki"));

        HttpResponse<String> libraryResponse = sendGet("/biblioteka-itemow");
        assertEquals(200, libraryResponse.statusCode());
        assertTrue(libraryResponse.body().contains("Odłamek Verathiela"));
        assertFalse(libraryResponse.body().contains("ODŁAMEK VERATHIEL"));
        assertTrue(libraryResponse.body().contains("Szczęśliwy traf: maks. 15% szans na odzyskanie +3 podstawowego zasobu"));
        assertFalse(libraryResponse.body().contains("Szczęśliwy traf: +3 podstawowego zasobu"));
        String verathielPopup = itemDetailsFragment(libraryResponse.body(), "1");
        assertTrue(verathielPopup.contains("Aspekt / efekt"));
        assertTrue(verathielPopup.contains("Odłamek Verathiela"));
        assertEquals(1, countOccurrences(verathielPopup, verathielEffectText()));
        assertFalse(verathielPopup.contains("Aspekt unikatowy:"));
        assertFalse(verathielPopup.contains("Status runtime:"));
        assertFalse(verathielPopup.contains("Nieaktywny w runtime DPS"));
        assertFalse(verathielPopup.contains("Efekt unikatowy zapisany opisowo"));
        assertFalse(verathielPopup.contains("Odczyt OCR efektu"));
        assertFalse(verathielPopup.contains("Diagnostyka OCR"));
        assertFalse(verathielPopup.contains("Źródło: OCR"));
        assertTrue(verathielPopup.contains("+94 obrażeń od broni [94 - 157]"));
        assertTrue(verathielPopup.contains("+2141 maksymalnego zdrowia [1831 - 2200]"));
        assertTrue(verathielPopup.contains("+545 zdrowia przy trafieniu [526 - 632]"));
        assertFalse(verathielPopup.contains("+545 pkt. zdrowia przy trafieniu [5 - 632]"));
        assertFalse(verathielPopup.contains("5 - 632"));
        assertTrue(verathielPopup.contains("Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]"));
        assertFalse(verathielPopup.contains("15% / +3"));

        HttpResponse<String> editForm = sendGet("/biblioteka-itemow/edytuj?itemId=1");
        assertEquals(200, editForm.statusCode());
        assertTrue(editForm.body().contains("name=\"itemName\" value=\"Odłamek Verathiela\""));
        assertFalse(editForm.body().contains("ODŁAMEK VERATHIEL"));
        assertFalse(editForm.body().contains("Diagnostyka OCR"));
        assertTrue(editForm.body().contains("name=\"itemPower\" value=\"900\""));
        assertTrue(editForm.body().contains("name=\"weaponDps\" value=\"1830\""));
        assertTrue(editForm.body().contains("name=\"weaponDamageMin\" value=\"1350\""));
        assertTrue(editForm.body().contains("name=\"weaponDamageMax\" value=\"1978\""));
        assertTrue(editForm.body().contains("name=\"averageWeaponDamage\" value=\"1664\""));
        assertTrue(editForm.body().contains("name=\"attacksPerSecond\" value=\"1.10\""));
        assertTrue(editForm.body().contains("<option value=\"verathiel_shard\""));
        assertTrue(editForm.body().contains("selected>Odłamek Verathiela</option>"));
        assertEquals(1, countOccurrences(editForm.body(), "Treść efektu"));
        assertFalse(editForm.body().contains("Wybrany aspekt:"));
        assertFalse(editForm.body().contains("Opis aspektu:"));
        assertFalse(editForm.body().contains("Odczyt OCR efektu"));
        assertFalse(editForm.body().contains("Dane itemu zapisane w bibliotece"));
        assertTrue(editForm.body().contains("affix-table"));
        assertTrue(editForm.body().contains("Zakres rolla"));
        assertFalse(editForm.body().contains("Odczyt OCR / źródło"));
        assertTrue(editForm.body().contains("name=\"affixValue_0\" value=\"94\""));
        assertTrue(editForm.body().contains("94 - 157"));
        assertTrue(editForm.body().contains("name=\"affixValue_1\" value=\"2141\""));
        assertTrue(editForm.body().contains("1831 - 2200"));
        assertTrue(editForm.body().contains("name=\"affixValue_2\" value=\"545\""));
        assertTrue(editForm.body().contains("526 - 632"));
        assertTrue(editForm.body().contains("Szczęśliwy traf: zasób podstawowy"));
        assertTrue(editForm.body().contains("name=\"affixValue_3\" value=\"3\""));
        assertTrue(editForm.body().contains("3 - 4"));
        assertTrue(editForm.body().contains("name=\"affixDisplayValue_3\" value=\"+3\""));
        assertTrue(editForm.body().contains("name=\"affixSource_3\" value=\"CORRECTED\""));
        assertEquals(1, countOccurrences(editForm.body(), "name=\"affixValue_3\""));

        HttpResponse<String> updateResponse = sendUrlEncodedPost("/biblioteka-itemow/edytuj", verathielEditFields("1"));
        assertEquals(200, updateResponse.statusCode());
        assertTrue(updateResponse.body().contains("Zapisano zmiany itemu."));
        assertTrue(updateResponse.body().contains("name=\"weaponDps\" value=\"1830\""));
        assertTrue(updateResponse.body().contains("name=\"weaponDamageMin\" value=\"1350\""));
        assertTrue(updateResponse.body().contains("name=\"weaponDamageMax\" value=\"1978\""));
        assertTrue(updateResponse.body().contains("name=\"averageWeaponDamage\" value=\"1664\""));
        assertTrue(updateResponse.body().contains("name=\"attacksPerSecond\" value=\"1.10\""));
        assertTrue(updateResponse.body().contains("526 - 632"));
        assertTrue(updateResponse.body().contains("name=\"affixValue_3\" value=\"3\""));
        assertTrue(updateResponse.body().contains("name=\"affixDisplayValue_3\" value=\"+3\""));
        assertTrue(updateResponse.body().contains("name=\"affixSource_3\" value=\"CORRECTED\""));

        HttpResponse<String> editAfterPost = sendGet("/biblioteka-itemow/edytuj?itemId=1");
        assertEquals(200, editAfterPost.statusCode());
        assertTrue(editAfterPost.body().contains("name=\"itemName\" value=\"Odłamek Verathiela\""));
        assertTrue(editAfterPost.body().contains("name=\"weaponDps\" value=\"1830\""));
        assertTrue(editAfterPost.body().contains("name=\"weaponDamageMin\" value=\"1350\""));
        assertTrue(editAfterPost.body().contains("name=\"weaponDamageMax\" value=\"1978\""));
        assertTrue(editAfterPost.body().contains("name=\"averageWeaponDamage\" value=\"1664\""));
        assertTrue(editAfterPost.body().contains("94 - 157"));
        assertTrue(editAfterPost.body().contains("1831 - 2200"));
        assertTrue(editAfterPost.body().contains("526 - 632"));
        assertTrue(editAfterPost.body().contains("3 - 4"));
        assertTrue(editAfterPost.body().contains("id=\"affixCount\" name=\"affixCount\" value=\"4\""));
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

        assertOnlyContains("/biblioteka-itemow?slot=BOOTS", "Buty Szybkie", "Tarcza Alfa");
        assertOnlyContains("/biblioteka-itemow?type=" + encode("Tarcza"), "Tarcza Alfa", "Buty Szybkie");
        assertOnlyContains("/biblioteka-itemow?status=used", "Tarcza Alfa", "Tarcza Beta");
        assertOnlyContains("/biblioteka-itemow?status=unused", "Tarcza Beta", "Tarcza Alfa");
        assertOnlyContains("/biblioteka-itemow?aspect=inner-calm", "Tarcza Alfa", "Tarcza Beta");
        assertOnlyContains("/biblioteka-itemow?aspect=__NONE__", "Tarcza Beta", "Tarcza Alfa");
        assertOnlyContains("/biblioteka-itemow?affix=THORNS", "Tarcza Beta", "Tarcza Alfa");
        assertOnlyContains("/biblioteka-itemow?greater=true", "Buty Szybkie", "Tarcza Alfa");
        assertOnlyContains("/biblioteka-itemow?q=Alfa", "Tarcza Alfa", "Tarcza Beta");
        assertOnlyContains("/biblioteka-itemow?q=tarcza-b", "Tarcza Beta", "Tarcza Alfa");
        assertOnlyContains("/biblioteka-itemow?q=" + encode("Wewnętrznego"), "Tarcza Alfa", "Tarcza Beta");
        assertOnlyContains("/biblioteka-itemow?q=" + encode("Ciernie"), "Tarcza Beta", "Tarcza Alfa");

        HttpResponse<String> selected = sendGet("/biblioteka-itemow?slot=BOOTS&greater=true");
        assertTrue(selected.body().contains("<option value=\"BOOTS\" selected>Buty</option>"));
        assertTrue(selected.body().contains("name=\"greater\" value=\"true\" checked"));

        HttpResponse<String> cleared = sendGet("/biblioteka-itemow");
        assertTrue(cleared.body().contains("Tarcza Alfa"));
        assertTrue(cleared.body().contains("Tarcza Beta"));
        assertTrue(cleared.body().contains("Buty Szybkie"));
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
        fields.put("itemName", itemName);
        fields.put("itemType", "Tarcza");
        fields.put("itemRarity", "LEGENDARY");
        fields.put("itemPower", "800");
        fields.put("uniqueEffectText", "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%");
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
        fields.put("itemName", itemName);
        fields.put("itemType", "Buty");
        fields.put("itemRarity", "LEGENDARY");
        fields.put("itemPower", "800");
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
        fields.put("itemName", itemName);
        fields.put("itemType", "Tarcza");
        fields.put("itemRarity", "LEGENDARY");
        fields.put("itemPower", "800");
        fields.put("uniqueEffectText", "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%");
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

    private static Map<String, String> verathielImportFields() {
        Map<String, String> fields = verathielEditFields("1");
        fields.remove("action");
        fields.remove("itemId");
        fields.put("formAction", "confirmItem");
        return fields;
    }

    private static Map<String, String> verathielEditFields(String itemId) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("action", "updateItem");
        fields.put("itemId", itemId);
        fields.put("sourceImageName", "miecz.png");
        fields.put("slot", "MAIN_HAND");
        fields.put("weaponDamage", "0");
        fields.put("itemName", "Odłamek Verathiela");
        fields.put("itemType", "Miecz");
        fields.put("itemRarity", "UNIQUE");
        fields.put("isAncientSubmitted", "true");
        fields.put("isAncient", "true");
        fields.put("itemPower", "900");
        fields.put("weaponDps", "1830");
        fields.put("weaponDamageMin", "1350");
        fields.put("weaponDamageMax", "1978");
        fields.put("averageWeaponDamage", "1664");
        fields.put("attacksPerSecond", "1.10");
        fields.put("uniqueEffectText", verathielEffectText());
        fields.put("selectedAspectId", "verathiel_shard");
        fields.put("fullItemRead", FullItemReadFormCodec.encode(new FullItemRead(
                "ODŁAMEK VERATHIEL",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "1 830 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.ITEM_NAME, "ODŁAMEK VERATHIEL"),
                        new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Starożytny unikatowy miecz"),
                        new FullItemReadLine(FullItemReadLineType.BASE_STAT, "1 830 pkt. obrażeń na sek."),
                        new FullItemReadLine(FullItemReadLineType.BASE_STAT, "[1 350 - 1 978] pkt. obrażeń za trafienie"),
                        new FullItemReadLine(FullItemReadLineType.BASE_STAT, "1,10 ataku na sekundę"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+94 obrażeń od broni [94 - 157]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+2 141 maksymalnego zdrowia [1 831 - 2 200]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+545 pkt. zdrowia przy trafieniu [526 - 632]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]"),
                        new FullItemReadLine(FullItemReadLineType.ASPECT, verathielEffectText())
                )
        )));
        fields.put("affixCount", "4");
        putAffix(fields, 0, "WEAPON_DAMAGE_FLAT", "94", "+94 obrażeń od broni [94 - 157]",
                "verathiel_weapon_damage_flat", "94", "157", "");
        putAffix(fields, 1, "MAXIMUM_LIFE", "2141", "+2 141 maksymalnego zdrowia [1 831 - 2 200]",
                "verathiel_maximum_life", "1831", "2200", "");
        putAffix(fields, 2, "LIFE_ON_HIT", "545", "+545 pkt. zdrowia przy trafieniu [5 - 632]",
                "verathiel_life_on_hit", "526", "632", "");
        putAffix(fields, 3, "LUCKY_HIT_PRIMARY_RESOURCE", "3",
                "Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]",
                "verathiel_lucky_hit_primary_resource", "3", "4", "+3");
        return fields;
    }

    private static void putAffix(Map<String, String> fields,
                                 int index,
                                 String type,
                                 String value,
                                 String sourceText,
                                 String definitionId,
                                 String rangeMin,
                                 String rangeMax,
                                 String displayValue) {
        fields.put("affixType_" + index, type);
        fields.put("affixValue_" + index, value);
        fields.put("affixSourceText_" + index, sourceText);
        fields.put("affixSource_" + index, "CORRECTED");
        fields.put("affixOriginalType_" + index, type);
        fields.put("affixOriginalValue_" + index, value);
        fields.put("affixDefinitionId_" + index, definitionId);
        fields.put("affixRangeMin_" + index, rangeMin);
        fields.put("affixRangeMax_" + index, rangeMax);
        fields.put("affixDisplayValue_" + index, displayValue);
    }

    private static String verathielEffectText() {
        return "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100], ale dodatkowo zużywają 25 pkt. podstawowego zasobu.";
    }

    private static String runtimeInputCard(String label, String value, String source) {
        return """
                <article class="summary-card runtime-input-card">
                    <div class="summary-label">""" + label + """
                </div>
                    <div class="summary-value">""" + value + """
                </div>
                    <div class="summary-source">Źródło: """ + source + """
                </div>
                </article>
                """;
    }

    private static String buildCurrentBuildQuery() {
        return "level=13&questSkillPoints=0&weaponDamage=200&strength=30&intelligence=11&thorns=70&blockChance=10&retributionChance=15&horizonSeconds=10"
                + "&rank_BRANDISH=0&choiceUpgrade_BRANDISH=NONE"
                + "&rank_HOLY_BOLT=0&choiceUpgrade_HOLY_BOLT=NONE"
                + "&rank_CLASH=0&choiceUpgrade_CLASH=NONE"
                + "&rank_ADVANCE=5&baseUpgrade_ADVANCE=true&choiceUpgrade_ADVANCE=RIGHT"
                + "&actionBar1=ADVANCE&actionBar2=NONE&actionBar3=NONE&actionBar4=NONE&actionBar5=NONE&actionBar6=NONE";
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

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = value.indexOf(needle);
        while (index >= 0) {
            count++;
            index = value.indexOf(needle, index + needle.length());
        }
        return count;
    }

    private static String itemDetailsFragment(String html, String itemId) {
        String marker = "<section id=\"item-details-" + itemId + "\"";
        int start = html.indexOf(marker);
        if (start < 0) {
            return "";
        }
        int next = html.indexOf("<section id=\"item-details-", start + marker.length());
        return next < 0 ? html.substring(start) : html.substring(start, next);
    }
}
