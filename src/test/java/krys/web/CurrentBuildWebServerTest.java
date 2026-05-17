package krys.web;

import krys.hero.HeroClass;
import krys.hero.HeroClassDef;
import krys.hero.HeroClassDefs;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy GUI pokrywają endpoint formularza, sekcję ekwipunku i podstawowy render wyniku bez powielania logiki runtime. */
class CurrentBuildWebServerTest {
    private CurrentBuildWebServer webServer;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        Path tempDirectory = Files.createTempDirectory("current-build-web");
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
    void shouldRenderEmptyStateWhenNoActiveHeroExists() throws Exception {
        HttpResponse<String> response = sendGet("/policz-aktualny-build");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Brak aktywnego bohatera"));
        assertTrue(response.body().contains("Przejdź do modułu Bohaterowie"));
        assertFalse(response.body().contains("name=\"level\""));
    }

    @Test
    void shouldRenderFormForCurrentBuildPage() throws Exception {
        createHero("Testowy bohater", "13");
        HttpResponse<String> response = sendGet("/policz-aktualny-build");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Policz aktualny build"));
        assertFalse(response.body().contains("<h1>Policz aktualny build</h1>"));
        assertFalse(response.body().contains("Bohater / Build / SSR"));
        assertFalse(response.body().contains("Operacyjny ekran aktywnego bohatera"));
        assertTrue(response.body().contains("<main class=\"layout current-build-wide\">"));
        assertTrue(response.body().contains(".layout.current-build-wide"));
        assertTrue(response.body().contains("current-build-details"));
        assertTrue(response.body().contains("<details class=\"current-build-details hero-context-details\">"));
        assertTrue(response.body().contains("<details class=\"current-build-details skill-point-details\">"));
        assertTrue(response.body().contains("<details class=\"current-build-details assigned-skills-details\">"));
        assertTrue(response.body().contains("<details class=\"current-build-details action-bar-details\">"));
        assertTrue(response.body().contains("<details class=\"current-build-details hero-stats-details\">"));
        assertTrue(response.body().contains("<details class=\"current-build-details equipment-details\">"));
        assertTrue(response.body().contains("<details class=\"current-build-details simulation-debug-details\">"));
        assertFalse(response.body().contains("<details class=\"current-build-details skill-point-details\" open>"));
        assertFalse(response.body().contains("<details class=\"current-build-details assigned-skills-details\" open>"));
        assertFalse(response.body().contains("<details class=\"current-build-details action-bar-details\" open>"));
        assertFalse(response.body().contains("<details class=\"current-build-details hero-stats-details\" open>"));
        assertFalse(response.body().contains("<details class=\"current-build-details simulation-debug-details\" open>"));
        assertFalse(response.body().contains("<details class=\"current-build-details used-items-details\">"));
        assertTrue(response.body().contains("Aktywny bohater"));
        assertTrue(response.body().contains("name=\"selectedHeroId\""));
        assertFalse(response.body().contains("name=\"heroLevelEdit\""));
        assertFalse(response.body().contains("Zapisz poziom"));
        assertEquals(1, countOccurrences(response.body(), "name=\"level\""));
        assertTrue(response.body().contains("current-build-sticky-actions"));
        assertTrue(response.body().contains("position: sticky"));
        assertTrue(response.body().contains("Zapisz zmiany"));
        assertTrue(response.body().contains("Wycofaj zmiany"));
        assertTrue(response.body().contains("Ekwipunek aktualnego buildu"));
        assertFalse(response.body().contains("Szczegóły użytych itemów"));
        assertFalse(response.body().contains("<h3>Użyte itemy</h3>"));
        assertFalse(response.body().contains("<h4>Brak użytych itemów</h4>"));
        assertFalse(response.body().contains("Efektywne staty do obliczeń"));
        assertTrue(response.body().contains("Techniczne wejście runtime"));
        assertTrue(response.body().contains("Punkty umiejętności"));
        assertFalse(response.body().contains("Budżet waliduje konfigurację bohatera"));
        assertFalse(response.body().contains("Konfiguracja mieści się w budżecie punktów umiejętności."));
        assertTrue(response.body().contains("skill-point-fields"));
        assertTrue(response.body().contains("skill-point-summary"));
        assertTrue(response.body().contains("minmax(190px, 1fr)"));
        assertTrue(response.body().contains("equipment-paperdoll"));
        assertTrue(response.body().contains("equipment-column-left"));
        assertTrue(response.body().contains("equipment-column-right"));
        assertTrue(response.body().contains("Hełm"));
        assertTrue(response.body().contains("Zbroja"));
        assertTrue(response.body().contains("Rękawice"));
        assertTrue(response.body().contains("Spodnie"));
        assertTrue(response.body().contains("Buty"));
        assertTrue(response.body().contains("Broń"));
        assertTrue(response.body().contains("Amulet"));
        assertTrue(response.body().contains("Pierścień 1"));
        assertTrue(response.body().contains("Pierścień 2"));
        assertTrue(response.body().contains("Tarcza"));
        assertTrue(response.body().contains("Statystyki bohatera"));
        assertFalse(response.body().contains("Podstawowe statystyki bohatera"));
        assertFalse(response.body().contains("Szansa na trafienie krytyczne"));
        HeroClassDef paladinDef = HeroClassDefs.get(HeroClass.PALADIN);
        assertTrue(response.body().contains(summaryCard("Siła", formatWholeForTest(paladinDef.resolveTotalMainStat(13, List.of())))));
        assertTrue(response.body().contains(summaryCard("Inteligencja", formatWholeForTest(paladinDef.resolveTotalIntelligence(13, List.of())))));
        assertTrue(response.body().contains("Brak jawnego baseline'u gry dla tego poziomu"));
        assertFalse(response.body().contains(summaryCard("Podstawowe obrażenia od broni", "0")));
        assertFalse(response.body().contains(summaryCard("Ciernie", "0")));
        assertFalse(response.body().contains(summaryCard("Szansa na blok z aktywnych itemów [%]", "0")));
        assertFalse(response.body().contains(summaryCard("Szansa retribution z aktywnych itemów [%]", "0")));
        assertFalse(response.body().contains(summaryCard("Wytrzymałość", "1610")));
        assertFalse(response.body().contains(summaryCard("Obrażenia broni", "8")));
        assertFalse(response.body().contains(summaryCard("Siła", "18")));
        assertFalse(response.body().contains(summaryCard("Inteligencja", "0")));
        assertFalse(response.body().contains(summaryCard("Kolce", "50")));
        assertFalse(response.body().contains("Zaawansowane: ręczne nadpisanie statów"));
        assertTrue(response.body().contains("Slot jest pusty"));
        assertTrue(response.body().contains(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.ADVANCE)));
        assertTrue(response.body().contains("Ranga z punktów"));
        assertTrue(response.body().contains("<option value=\"0\">0</option>"));
        assertTrue(response.body().contains("<option value=\"15\">15</option>"));
        assertFalse(response.body().contains("<option value=\"16\">16</option>"));
        assertFalse(response.body().contains("name=\"" + CurrentBuildFormData.rankFieldName(krys.skill.SkillId.HOLY_BOLT) + "\""));
        assertFalse(response.body().contains("name=\"weaponDamage\""));
        assertFalse(response.body().contains("name=\"horizonSeconds\""));
        assertTrue(response.body().contains("name=\"actionBar1\""));
        assertTrue(response.body().contains("name=\"actionBar6\""));
        assertTrue(response.body().contains("Otwórz bibliotekę itemów"));
        assertTrue(response.body().contains("Importuj nowy item"));
        assertTrue(response.body().contains("Wybierz z biblioteki"));
        assertFalse(response.body().contains("Centrum buildu"));
        assertTrue(response.body().contains("max-width: 1840px;"));
        assertTrue(response.body().indexOf("<div class=\"current-build-sticky-actions\">") < response.body().indexOf("<details class=\"current-build-details skill-point-details\">"));
        assertTrue(response.body().indexOf("Punkty umiejętności") < response.body().indexOf("Umiejętności bohatera"));
        assertTrue(response.body().indexOf("Umiejętności bohatera") < response.body().indexOf("Pasek akcji bohatera"));
        assertTrue(response.body().indexOf("Pasek akcji bohatera") < response.body().indexOf("Statystyki bohatera"));
        assertTrue(response.body().indexOf("Statystyki bohatera") < response.body().indexOf("Ekwipunek aktualnego buildu"));
        assertTrue(response.body().indexOf("Ekwipunek aktualnego buildu") < response.body().indexOf("Wynik symulacji"));
        assertTrue(response.body().indexOf("Wynik symulacji") < response.body().indexOf("Debug symulacji"));
        assertTrue(response.body().indexOf("Hełm") < response.body().indexOf("Broń"));
        assertTrue(response.body().indexOf("Amulet") < response.body().indexOf("Tarcza"));
    }

    @Test
    void shouldAllowInlineSwitchingActiveHero() throws Exception {
        createHero("Alaric", "13");
        createHero("Gregor", "25");

        HttpResponse<String> switchResponse = sendPost("/policz-aktualny-build", Map.of(
                "heroAction", "setActiveHeroInline",
                "selectedHeroId", "2"
        ));

        assertEquals(200, switchResponse.statusCode());
        assertTrue(switchResponse.body().contains("Zmieniono aktywnego bohatera bez opuszczania ekranu buildu."));
        assertTrue(switchResponse.body().contains("Gregor"));
        assertTrue(switchResponse.body().contains(summaryCard("Poziom bohatera", "25")));
    }

    @Test
    void shouldSaveHeroLevelOnlyFromSkillPointSection() throws Exception {
        createHero("Alaric", "13");

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("level", "50");
        fields.put("questSkillPoints", "14");

        HttpResponse<String> levelResponse = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, levelResponse.statusCode());
        assertTrue(levelResponse.body().contains(summaryCard("Poziom bohatera", "50")));
        assertTrue(levelResponse.body().contains("name=\"level\" value=\"50\""));
        assertTrue(levelResponse.body().contains(summaryCard("Punkty z poziomu", "49")));
        assertEquals(1, countOccurrences(levelResponse.body(), "name=\"level\""));
        assertFalse(levelResponse.body().contains("name=\"heroLevelEdit\""));
    }

    @Test
    void shouldRenderVerifiedPaladinLevel70StatsWithoutItems() throws Exception {
        createHero("Paladyn bez itemów", "70");

        HttpResponse<String> response = sendGet("/policz-aktualny-build");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Statystyki bohatera"));
        assertFalse(response.body().contains("Baseline gry: Paladyn poziom 70 bez itemów. Aktywne itemy są doliczane tylko dla jawnych pól modelu."));
        assertFalse(response.body().contains("Atrybuty dodatkowe"));
        assertTrue(response.body().contains(summaryCard("Klasa", "Paladyn")));
        assertTrue(response.body().contains(summaryCard("Poziom", "70")));
        assertTrue(response.body().contains(summaryCard("Siła", "79")));
        assertTrue(response.body().contains(summaryCard("Inteligencja", "76")));
        assertTrue(response.body().contains(summaryCard("Siła woli", "76")));
        assertTrue(response.body().contains(summaryCard("Zręczność", "77")));
        assertTrue(response.body().indexOf("<h3>Główne") < response.body().indexOf(summaryCard("Klasa", "Paladyn")));
        assertTrue(response.body().indexOf(summaryCard("Klasa", "Paladyn")) < response.body().indexOf(summaryCard("Poziom", "70")));
        assertTrue(response.body().indexOf(summaryCard("Poziom", "70")) < response.body().indexOf(summaryCard("Siła", "79")));
        assertTrue(response.body().indexOf(summaryCard("Siła", "79")) < response.body().indexOf(summaryCard("Inteligencja", "76")));
        assertTrue(response.body().indexOf(summaryCard("Inteligencja", "76")) < response.body().indexOf(summaryCard("Siła woli", "76")));
        assertTrue(response.body().indexOf(summaryCard("Siła woli", "76")) < response.body().indexOf(summaryCard("Zręczność", "77")));
        assertTrue(response.body().indexOf(summaryCard("Zręczność", "77")) < response.body().indexOf("<h3>Pancerz i defensywa"));
        assertTrue(response.body().contains(summaryCard("Wytrzymałość", "1610")));
        assertTrue(response.body().contains(summaryCardWithTooltipPrefix("Pancerz", "158", "158 z siły, 0 z itemów/głównego wyposażenia, 0 z innych źródeł, razem 158.")));
        assertTrue(response.body().contains(summaryCardWithTooltipPrefix("Maksimum zdrowia", "1526", "Baseline: 1526; aktywne itemy: +0; razem: 1526.")));
        assertTrue(response.body().contains(summaryCard("Fizyczne", "30")));
        assertTrue(response.body().contains(summaryCard("Ogień", "30")));
        assertTrue(response.body().contains(summaryCard("Błyskawice", "30")));
        assertTrue(response.body().contains(summaryCard("Zimno", "30")));
        assertTrue(response.body().contains(summaryCard("Trucizna", "30")));
        assertTrue(response.body().contains(summaryCard("Cień", "30")));
        assertFalse(response.body().contains(summaryCard("Odporności", "30")));
        assertTrue(response.body().contains(summaryCard("Podstawowe obrażenia od broni", "0")));
        assertTrue(response.body().contains(summaryCard("Szybkość broni", "1,00")));
        assertTrue(response.body().contains(summaryCardWithTooltipPrefix("Szansa na trafienie krytyczne", "5,2%", "bazowo 5,0%, +0,2% z Inteligencji, +0,0% z itemów, +0,0% z innych źródeł, razem 5,2%.")));
        assertTrue(response.body().contains(summaryCard("Obrażenia od trafień krytycznych", "50,0%")));
        assertTrue(response.body().contains(summaryCard("Obrażenia zadawane odsłoniętym celom", "20,0%")));
        assertTrue(response.body().contains(summaryCard("Ciernie", "0")));
        assertFalse(response.body().contains(summaryCard("Obrażenia broni", "8")));
        assertFalse(response.body().contains(summaryCard("Siła", "18")));
        assertFalse(response.body().contains(summaryCard("Inteligencja", "0")));
        assertFalse(response.body().contains(summaryCard("Kolce", "50")));
        assertFalse(response.body().contains(summaryCard("Szansa na blok [%]", "50")));
        String visibleStats = stripTooltipAttributes(response.body());
        assertFalse(visibleStats.contains("158 z siły"));
        assertFalse(visibleStats.contains("bazowo 5,0%"));
        assertFalse(response.body().contains("Odłamek Verathiela"));
        assertFalse(response.body().contains("DPS broni"));
    }

    @Test
    void shouldProjectActiveVerathielIntoCurrentHeroStatsWithoutRuntimeDpsUnlock() throws Exception {
        createHero("Paladyn z Verathielem", "70");
        HttpResponse<String> saveResponse = sendPost("/importuj-item-ze-screena", verathielImportFields());
        assertEquals(200, saveResponse.statusCode());
        assertTrue(saveResponse.body().contains("Odłamek Verathiela"));

        HttpResponse<String> inactiveResponse = sendGet("/policz-aktualny-build");
        assertEquals(200, inactiveResponse.statusCode());
        assertTrue(inactiveResponse.body().contains(summaryCardWithTooltipPrefix("Maksimum zdrowia", "1526", "Baseline: 1526; aktywne itemy: +0; razem: 1526.")));
        assertFalse(inactiveResponse.body().contains(summaryCard("DPS broni", "1830")));

        HttpResponse<String> activateResponse = sendPost("/biblioteka-itemow", Map.of(
                "action", "activateItem",
                "itemId", "1",
                "heroSlot", "MAIN_HAND",
                "currentBuildQuery", ""
        ));
        assertEquals(200, activateResponse.statusCode());

        HttpResponse<String> activeResponse = sendGet("/policz-aktualny-build");
        String html = activeResponse.body();
        assertEquals(200, activeResponse.statusCode());
        assertTrue(html.contains("Odłamek Verathiela"));
        assertTrue(html.contains("class=\"status-badge status-active\">Aktywny</span>"));
        String slotCard = activeSlotCard(html);
        assertFalse(slotCard.contains("Brak wkładu"));
        assertTrue(slotCard.contains("slot-chip"));
        assertTrue(slotCard.contains("Broń"));
        assertTrue(slotCard.contains("Wkład w statystyki"));
        assertTrue(slotCard.contains("Efekty opisowe"));
        assertTrue(slotCard.contains("DPS 1830"));
        assertTrue(slotCard.contains("1350 - 1978 obrażeń za trafienie"));
        assertTrue(slotCard.contains("+2141 maksymalnego zdrowia"));
        assertTrue(slotCard.contains("+94 obrażeń od broni"));
        assertTrue(slotCard.contains("+545 zdrowia przy trafieniu"));
        assertTrue(slotCard.contains("Lucky Hit 15%: +3 zasobu"));
        assertFalse(slotCard.contains("DPS 1830 • 1350 - 1978"));
        String verathielOption = itemSelectOptionForValue(html, "MAIN_HAND", "1");
        assertTrue(verathielOption.contains("Odłamek Verathiela | DPS 1830 | +2141 zdrowia"));
        assertFalse(verathielOption.contains("Lucky Hit"));
        assertFalse(verathielOption.contains("+545 zdrowia przy trafieniu"));
        assertFalse(verathielOption.contains("+94 obrażeń od broni"));
        assertFalse(verathielOption.contains("1350 - 1978"));
        assertTrue(html.contains(summaryCardWithTooltipPrefix("Maksimum zdrowia", "3667", "Baseline: 1526; aktywne itemy: +2141; razem: 3667.")));
        assertTrue(html.contains(summaryCard("DPS broni", "1830")));
        assertTrue(html.contains(summaryCard("Obrażenia za trafienie", "1350 - 1978")));
        assertTrue(html.contains(summaryCard("Średnie obrażenia trafienia", "1664")));
        assertTrue(html.contains(summaryCard("Ataki na sekundę", "1,10")));
        assertTrue(html.contains("<h3>Aktywna broń"));
        String runtimeInput = technicalRuntimeInputSection(html);
        assertTrue(runtimeInput.contains(runtimeInputCard("Obrażenia broni", "1664", "Aktywna broń: Odłamek Verathiela, średnie obrażenia trafienia")));
        assertTrue(runtimeInput.contains(runtimeInputCard("Siła", "79", "Baseline Paladyn poziom 70")));
        assertTrue(runtimeInput.contains(runtimeInputCard("Inteligencja", "76", "Baseline Paladyn poziom 70")));
        assertTrue(runtimeInput.contains(runtimeInputCard("Kolce", "0", "Brak jawnego wkładu current build")));
        assertTrue(runtimeInput.contains(runtimeInputCard("Szansa bloku [%]", "0", "Brak jawnego wkładu current build")));
        assertTrue(runtimeInput.contains(runtimeInputCard("Szansa retribution [%]", "0", "Brak jawnego wkładu current build")));
        assertFalse(runtimeInput.contains("Manual: obrażenia broni"));
        assertFalse(runtimeInput.contains("Manual: siła"));
        assertFalse(runtimeInput.contains("Manual: inteligencja"));
        assertFalse(runtimeInput.contains("Manual: kolce"));
        assertFalse(runtimeInput.contains("Manual: szansa bloku"));
        assertFalse(runtimeInput.contains("Manual: szansa retribution"));
        assertFalse(runtimeInput.contains("legacy fallback"));
        assertFalse(runtimeInput.contains("Legacy/manual fallbacki formularza"));
        assertFalse(runtimeInput.contains(summaryCard("Obrażenia broni", "8")));
        assertFalse(runtimeInput.contains(summaryCard("Obrażenia broni", "1830")));
        assertTrue(html.contains("+94 obrażeń od broni [94 - 157]"));
        assertTrue(html.contains("+2141 maksymalnego zdrowia [1831 - 2200]"));
        assertTrue(html.contains("+545 zdrowia przy trafieniu [526 - 632]"));
        assertTrue(html.contains("Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]"));
        String activeAffixesGroup = heroStatGroup(html, "Aktywne affixy itemów");
        assertTrue(activeAffixesGroup.contains("Wkład statystyczny"));
        assertTrue(activeAffixesGroup.contains("Efekty opisowe"));
        assertEquals(4, countOccurrences(activeAffixesGroup, "<li>"));
        assertFalse(activeAffixesGroup.contains("OCR"));
        assertFalse(activeAffixesGroup.contains("source"));
        assertFalse(activeAffixesGroup.contains("15% / +3"));
        String offenseGroup = heroStatGroup(html, "Ofensywa");
        assertFalse(offenseGroup.contains(summaryCard("Podstawowe obrażenia od broni", "0")));
        assertFalse(offenseGroup.contains(summaryCard("Szybkość broni", "1,00")));
        assertTrue(offenseGroup.contains(summaryCardWithTooltipPrefix("Szansa na trafienie krytyczne", "5,2%", "bazowo 5,0%, +0,2% z Inteligencji, +0,0% z itemów, +0,0% z innych źródeł, razem 5,2%.")));
        assertTrue(offenseGroup.contains(summaryCard("Obrażenia od trafień krytycznych", "50,0%")));
        assertTrue(offenseGroup.contains(summaryCard("Obrażenia zadawane odsłoniętym celom", "20,0%")));
        assertTrue(html.contains(summaryCard("Ciernie", "0")));
        assertFalse(html.contains(summaryCardWithTooltipPrefix("Maksimum zdrowia", "4212", "Baseline: 1526; aktywne itemy: +2686; razem: 4212.")));
        assertFalse(html.contains(summaryCard("Średnie obrażenia trafienia", "1758")));
        assertFalse(technicalRuntimeInputSection(html).contains(summaryCard("Obrażenia broni", "1830")));

        HttpResponse<String> calculationResponse = sendPost("/policz-aktualny-build", buildAdvanceRankOneLevel70Fields());
        String calculationHtml = calculationResponse.body();
        assertEquals(200, calculationResponse.statusCode());
        assertTrue(calculationHtml.contains(summaryCard("Efektywne obrażenia broni", "1664")));
        assertTrue(calculationHtml.contains(summaryCard("Efektywna siła", "79")));
        assertTrue(calculationHtml.contains(summaryCard("Efektywna inteligencja", "76")));
        assertTrue(technicalRuntimeInputSection(calculationHtml)
                .contains(runtimeInputCard("Obrażenia broni", "1664", "Aktywna broń: Odłamek Verathiela, średnie obrażenia trafienia")));
        assertTrue(calculationHtml.contains("Łączne obrażenia"));
        assertFalse(calculationHtml.contains(summaryCard("Łączne obrażenia", "0")));

        Map<String, String> clearFields = buildAdvanceFlashFields(10);
        clearFields.put("level", "70");
        clearFields.put("slotAction", "clearActiveSlotItem:MAIN_HAND");
        HttpResponse<String> clearResponse = sendPost("/policz-aktualny-build", clearFields);
        assertEquals(200, clearResponse.statusCode());
        assertFalse(clearResponse.body().contains("Weapon damage musi być >= 1"));
        assertTrue(equipmentSlotCard(clearResponse.body(), "MAIN_HAND").contains("Slot jest pusty"));
        assertTrue(clearResponse.body().contains(summaryCardWithTooltipPrefix("Maksimum zdrowia", "1526", "Baseline: 1526; aktywne itemy: +0; razem: 1526.")));
        assertFalse(clearResponse.body().contains(summaryCard("DPS broni", "1830")));
        String clearRuntimeInput = technicalRuntimeInputSection(clearResponse.body());
        assertTrue(clearRuntimeInput.contains(runtimeInputCard("Obrażenia broni", "0", "Brak aktywnej broni")));
        assertFalse(clearRuntimeInput.contains("Odłamek Verathiela"));
        assertFalse(clearRuntimeInput.contains("1664"));
        assertFalse(clearResponse.body().contains(summaryCard("Efektywne obrażenia broni", "1664")));

        HttpResponse<String> afterClearGet = sendGet("/policz-aktualny-build");
        assertEquals(200, afterClearGet.statusCode());
        assertTrue(equipmentSlotCard(afterClearGet.body(), "MAIN_HAND").contains("Slot jest pusty"));
        assertTrue(afterClearGet.body().contains(summaryCardWithTooltipPrefix("Maksimum zdrowia", "1526", "Baseline: 1526; aktywne itemy: +0; razem: 1526.")));
        assertTrue(technicalRuntimeInputSection(afterClearGet.body()).contains(runtimeInputCard("Obrażenia broni", "0", "Brak aktywnej broni")));
        assertFalse(technicalRuntimeInputSection(afterClearGet.body()).contains("Odłamek Verathiela"));

        Map<String, String> reselectFields = buildAdvanceFlashFields(10);
        reselectFields.put("level", "70");
        reselectFields.put("selectedItemId_MAIN_HAND", "1");
        HttpResponse<String> reselectResponse = sendPost("/policz-aktualny-build", reselectFields);
        assertEquals(200, reselectResponse.statusCode());
        assertFalse(reselectResponse.body().contains("Weapon damage musi być >= 1"));
        assertTrue(equipmentSlotCard(reselectResponse.body(), "MAIN_HAND").contains("class=\"status-badge status-active\">Aktywny</span>"));
        assertTrue(reselectResponse.body().contains(summaryCardWithTooltipPrefix("Maksimum zdrowia", "3667", "Baseline: 1526; aktywne itemy: +2141; razem: 3667.")));
        assertTrue(technicalRuntimeInputSection(reselectResponse.body())
                .contains(runtimeInputCard("Obrażenia broni", "1664", "Aktywna broń: Odłamek Verathiela, średnie obrażenia trafienia")));
    }

    @Test
    void shouldShowZeroWeaponDamageWithoutActiveWeaponInCurrentBuildDebug() throws Exception {
        createHero("Paladyn bez broni", "70");

        HttpResponse<String> response = sendGet("/policz-aktualny-build");

        assertEquals(200, response.statusCode());
        String runtimeInput = technicalRuntimeInputSection(response.body());
        assertTrue(runtimeInput.contains(runtimeInputCard("Obrażenia broni", "0", "Brak aktywnej broni")));
        assertFalse(runtimeInput.contains("Manual: obrażenia broni"));
        assertFalse(runtimeInput.contains(summaryCard("Obrażenia broni", "8")));

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("level", "70");
        HttpResponse<String> saveResponse = sendPost("/policz-aktualny-build", fields);
        assertEquals(200, saveResponse.statusCode());
        assertFalse(saveResponse.body().contains("Weapon damage musi być >= 1"));
        assertTrue(equipmentSlotCard(saveResponse.body(), "MAIN_HAND").contains("Slot jest pusty"));
        assertTrue(technicalRuntimeInputSection(saveResponse.body()).contains(runtimeInputCard("Obrażenia broni", "0", "Brak aktywnej broni")));
    }

    @Test
    void shouldKeepActiveVerathielWhenGlobalSaveSubmitsEmptySlotSelect() throws Exception {
        createHero("Pusty select Verathiela", "70");
        saveAndActivateVerathiel();
        Map<String, String> fields = buildAdvanceRankOneLevel70Fields();
        fields.put("selectedItemId_MAIN_HAND", "");

        HttpResponse<String> response = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("Weapon damage musi być >= 1"));
        assertTrue(equipmentSlotCard(response.body(), "MAIN_HAND").contains("class=\"status-badge status-active\">Aktywny</span>"));
        assertTrue(technicalRuntimeInputSection(response.body())
                .contains(runtimeInputCard("Obrażenia broni", "1664", "Aktywna broń: Odłamek Verathiela, średnie obrażenia trafienia")));
    }

    @Test
    void shouldExplainEmptyActionBarSimulationWithoutRuntimeError() throws Exception {
        createHero("Paladyn pusty pasek", "70");
        assignSkill(krys.skill.SkillId.ADVANCE);
        saveAndActivateVerathiel();
        Map<String, String> fields = buildAdvanceRankOneLevel70Fields();
        for (int slot = 1; slot <= CurrentBuildFormData.ACTION_BAR_SLOT_COUNT; slot++) {
            fields.put(CurrentBuildFormData.actionBarFieldName(slot), "NONE");
        }

        HttpResponse<String> response = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("Action bar może zawierać"));
        assertTrue(response.body().contains("Symulacja nie wykonała trafień, ponieważ pasek akcji jest pusty."));
    }

    @Test
    void shouldNotIntroduceUnverifiedCriticalChanceFormulaOrCritDamageMapping() throws Exception {
        String source = Files.readString(Path.of("src/main/java/krys/hero/HeroClassStatBaselines.java"))
                + Files.readString(Path.of("src/main/java/krys/hero/HeroCriticalChanceBreakdown.java"))
                + Files.readString(Path.of("src/main/java/krys/web/CurrentBuildPageRenderer.java"))
                + Files.readString(Path.of("src/main/java/krys/web/CurrentHeroStatsPresentation.java"));

        assertFalse(java.util.regex.Pattern
                .compile("criticalChanceFromIntelligence[^;=]*=\\s*[^;]*intelligence\\s*\\*",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(source)
                .find());
        assertFalse(source.contains("ItemStatType.CRIT_DAMAGE"));
        assertFalse(Files.readString(Path.of("src/main/java/krys/item/ItemStatType.java")).contains("CRIT_CHANCE"));
    }

    @Test
    void shouldRenderOnlyAssignedSkillsAndRejectActionBarOutsideAssignedLearnedSkills() throws Exception {
        createHero("Testowy bohater", "13");

        HttpResponse<String> initialResponse = sendGet("/policz-aktualny-build");
        assertEquals(200, initialResponse.statusCode());
        assertTrue(initialResponse.body().contains(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.ADVANCE)));
        assertFalse(initialResponse.body().contains("name=\"" + CurrentBuildFormData.rankFieldName(krys.skill.SkillId.HOLY_BOLT) + "\""));

        HttpResponse<String> addSkillResponse = sendPost("/policz-aktualny-build", Map.of(
                "heroAction", "addAssignedSkill",
                "skillIdToAdd", "HOLY_BOLT"
        ));
        assertEquals(200, addSkillResponse.statusCode());
        assertFalse(addSkillResponse.body().contains("Wybierz poprawną umiejętność do przypisania bohaterowi."));
        assertTrue(addSkillResponse.body().contains("Dodano umiejętność Święty Pocisk do bohatera."));
        assertTrue(addSkillResponse.body().contains(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.HOLY_BOLT)));

        Map<String, String> invalidBarFields = buildAdvanceFlashFields(10);
        invalidBarFields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.HOLY_BOLT), "0");
        invalidBarFields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.HOLY_BOLT), "NONE");
        invalidBarFields.put(CurrentBuildFormData.actionBarFieldName(1), "HOLY_BOLT");

        HttpResponse<String> sanitizeResponse = sendPost("/policz-aktualny-build", invalidBarFields);

        assertEquals(200, sanitizeResponse.statusCode());
        assertFalse(sanitizeResponse.body().contains("Pasek akcji został oczyszczony do umiejętności przypisanych i nauczonych przez aktywnego bohatera."));
        assertTrue(sanitizeResponse.body().contains("Action bar slot 1 wskazuje skill bez rank &gt; 0"));

        HttpResponse<String> reloadResponse = sendGet("/policz-aktualny-build");
        assertEquals(200, reloadResponse.statusCode());
        assertFalse(reloadResponse.body().contains("<option value=\"HOLY_BOLT\" selected>Święty Pocisk</option>"));
    }

    @Test
    void shouldRenderFreshClashAssignedSkillAsInactiveRankZero() throws Exception {
        createHero("Testowy bohater", "13");

        HttpResponse<String> initialResponse = sendGet("/policz-aktualny-build");
        assertEquals(200, initialResponse.statusCode());
        assertTrue(initialResponse.body().contains("Umiejętności bohatera"));
        assertTrue(initialResponse.body().contains("<option value=\"CLASH\">Starcie</option>"));
        assertFalse(initialResponse.body().contains("<option value=\"CLASH\">Clash</option>"));

        HttpResponse<String> addClashResponse = sendPost("/policz-aktualny-build", Map.of(
                "heroAction", "addAssignedSkill",
                "skillIdToAdd", "CLASH"
        ));
        assertEquals(200, addClashResponse.statusCode());
        assertTrue(addClashResponse.body().contains("Dodano umiejętność Starcie do bohatera."));

        String clashCard = assignedSkillCard(addClashResponse.body(), "CLASH");
        assertTrue(clashCard.contains("<h4>Starcie"));
        assertTrue(clashCard.contains("Aktualne dane umiejętności"));
        assertTrue(clashCard.contains("Konfiguracja runtime legacy"));
        assertTrue(clashCard.contains("Opisowe modyfikatory z drzewa Paladyna nie są jeszcze aktywne w runtime DPS."));
        assertTrue(clashCard.contains(summaryCard("Nazwa", "Starcie")));
        assertTrue(clashCard.contains(summaryCard("Aktualna ranga", "0")));
        assertTrue(clashCard.contains(summaryCard("Kategorie z gry", "Podstawowe, Moloch")));
        assertTrue(clashCard.contains("Ranga 0 — umiejętność przypisana, ale nieaktywna w danych bojowych."));
        assertTrue(clashCard.contains("Brak aktywnych modyfikatorów z konfiguracji."));
        assertFalse(clashCard.contains("115%"));
        assertFalse(clashCard.contains("293%"));
        assertFalse(clashCard.contains(">Lucky Hit<"));
        assertFalse(clashCard.contains(">Generowanie Wiary<"));

        String visibleCard = stripTooltipAttributes(clashCard);
        assertFalse(visibleCard.contains(">Zwiększenie Obrażeń</li>"));
        assertFalse(visibleCard.contains(">Brać Ich</li>"));
        assertFalse(visibleCard.contains(">Potyczka</li>"));
        assertFalse(visibleCard.contains(">Marsz Krzyżowca</li>"));
        assertFalse(visibleCard.contains(">Animusz</li>"));
        assertFalse(visibleCard.contains(">Skuteczność Marszu Krzyżowca</li>"));
        assertFalse(visibleCard.contains(">Kara</li>"));
        assertFalse(visibleCard.contains("+10"));
        assertFalse(visibleCard.contains("20%[X]"));
        assertFalse(visibleCard.contains("8%[X]"));
        assertFalse(visibleCard.contains("155%"));
        assertFalse(visibleCard.contains("15%[X]"));
        assertFalse(visibleCard.contains("25%[+]"));
        assertFalse(visibleCard.contains("25%[X]"));
        assertFalse(visibleCard.contains("30%[+]"));
        assertFalse(visibleCard.contains("3489"));
        assertFalse(visibleCard.contains("Odwet / ciernie"));
        assertFalse(visibleCard.contains("szansa na blok"));
        assertFalse(visibleCard.contains("efekt co 3. atak"));
        assertFalse(clashCard.contains("damagePerUse"));
        assertFalse(clashCard.contains("theoreticalDps"));

        HttpResponse<String> reloadResponse = sendGet("/policz-aktualny-build");
        assertEquals(200, reloadResponse.statusCode());
        assertTrue(assignedSkillCard(reloadResponse.body(), "CLASH").contains("<h4>Starcie"));

        HttpResponse<String> rankingResponse = sendGet("/ranking-obrazen?character=paladin&skillGroup=basic&q=star");
        assertEquals(200, rankingResponse.statusCode());
        assertTrue(rankingResponse.body().contains("data-skill-id=\"starcie\""));
        assertFalse(rankingResponse.body().contains(">Grupa drzewa<"));
        assertFalse(rankingResponse.body().contains(">tags<"));
        assertFalse(rankingResponse.body().contains(">type<"));
        assertFalse(rankingResponse.body().contains(">Speed / cooldown<"));
    }

    @Test
    void shouldRenderClashCurrentRankOneValuesWithoutCatalogMax() throws Exception {
        createHero("Testowy bohater", "13");
        assignSkill(krys.skill.SkillId.CLASH);

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.CLASH), "1");
        fields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.CLASH), "NONE");

        HttpResponse<String> response = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, response.statusCode());
        String clashCard = assignedSkillCard(response.body(), "CLASH");
        assertTrue(clashCard.contains(summaryCard("Aktualna ranga", "1")));
        assertTrue(clashCard.contains(summaryCard("Obrażenia na randze 1", "115%")));
        assertTrue(clashCard.contains(summaryCard("Lucky Hit", "63%")));
        assertTrue(clashCard.contains(summaryCard("Bazowe generowanie Wiary", "20")));
        assertFalse(clashCard.contains(">R1<"));
        assertFalse(clashCard.contains(">Max drzewo<"));
        assertFalse(clashCard.contains("293%"));

        String visibleCard = stripTooltipAttributes(clashCard);
        assertTrue(visibleCard.contains(">Marsz Krzyżowca</li>"));
        assertTrue(visibleCard.contains("Brak aktywnych modyfikatorów z konfiguracji."));
        assertFalse(visibleCard.contains(">Generowanie Wiary</li>"));
        assertFalse(visibleCard.contains(">Zwiększenie Obrażeń</li>"));
        assertFalse(visibleCard.contains(">Brać Ich</li>"));
        assertFalse(visibleCard.contains(">Potyczka</li>"));
        assertFalse(visibleCard.contains(">Animusz</li>"));
        assertFalse(visibleCard.contains(">Skuteczność Marszu Krzyżowca</li>"));
        assertFalse(visibleCard.contains(">Kara</li>"));
    }

    @Test
    void shouldRenderBoughtRankFifteenWithoutTreatingItAsEffectiveRank() throws Exception {
        createHero("Testowy bohater", "13");
        assignSkill(krys.skill.SkillId.CLASH);

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("level", "70");
        fields.put("questSkillPoints", "14");
        fields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.CLASH), "15");
        fields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.CLASH), "NONE");

        HttpResponse<String> response = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, response.statusCode());
        String clashCard = assignedSkillCard(response.body(), "CLASH");
        assertTrue(clashCard.contains(summaryCard("Aktualna ranga", "15")));
        assertTrue(clashCard.contains(summaryCard("Obrażenia na randze 15", "293%")));
        assertFalse(clashCard.contains(">Max drzewo<"));
        assertFalse(clashCard.contains("115%"));
    }

    @Test
    void shouldRejectBoughtSkillRankAboveFifteen() throws Exception {
        createHero("Testowy bohater", "70");
        assignSkill(krys.skill.SkillId.CLASH);

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("level", "70");
        fields.put("questSkillPoints", "14");
        fields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.CLASH), "16");
        fields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.CLASH), "NONE");

        HttpResponse<String> response = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Ranga z punktów umiejętności CLASH musi być w zakresie 0..15."));
    }

    @Test
    void shouldRenderAndPersistSkillPointBudgetOnCurrentBuild() throws Exception {
        createHero("Testowy bohater", "13");

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("level", "70");
        fields.put("questSkillPoints", "14");

        HttpResponse<String> response = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Punkty umiejętności"));
        assertTrue(response.body().contains(summaryCard("Punkty z poziomu", "69")));
        assertTrue(response.body().contains(summaryCard("Dodatkowe punkty z zadań", "14")));
        assertTrue(response.body().contains(summaryCard("Dostępne punkty", "83")));
        assertTrue(response.body().contains(summaryCard("Wydane punkty", "7")));
        assertTrue(response.body().contains(summaryCard("Pozostałe punkty", "76")));
        assertTrue(response.body().contains("<a class=\"nav-link secondary-link\" href=\"/policz-aktualny-build\">Wycofaj zmiany</a>"));

        HttpResponse<String> reloadResponse = sendGet("/policz-aktualny-build");
        assertEquals(200, reloadResponse.statusCode());
        assertTrue(reloadResponse.body().contains("name=\"questSkillPoints\" value=\"14\""));
        assertTrue(reloadResponse.body().contains(summaryCard("Dostępne punkty", "83")));
    }

    @Test
    void shouldWithdrawUnsavedChangesByReloadingSavedCurrentBuildState() throws Exception {
        createHero("Testowy bohater", "13");
        assignSkill(krys.skill.SkillId.CLASH);

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("level", "50");
        fields.put("questSkillPoints", "14");
        fields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.CLASH), "1");
        fields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.CLASH), "NONE");
        HttpResponse<String> saveResponse = sendPost("/policz-aktualny-build", fields);
        assertEquals(200, saveResponse.statusCode());
        assertTrue(saveResponse.body().contains("<h4>Starcie"));

        HttpResponse<String> withdrawResponse = sendGet("/policz-aktualny-build");

        assertEquals(200, withdrawResponse.statusCode());
        assertTrue(withdrawResponse.body().contains("<h4>Starcie"));
        assertTrue(withdrawResponse.body().contains(summaryCard("Poziom bohatera", "50")));
        assertTrue(withdrawResponse.body().contains("name=\"questSkillPoints\" value=\"14\""));
        assertTrue(withdrawResponse.body().contains(summaryCard("Aktualna ranga", "1")));
    }

    @Test
    void shouldRejectSkillPointBudgetInputsOutsideAllowedRange() throws Exception {
        createHero("Testowy bohater", "13");

        Map<String, String> tooManyQuestPoints = buildAdvanceFlashFields(10);
        tooManyQuestPoints.put("level", "70");
        tooManyQuestPoints.put("questSkillPoints", "15");

        HttpResponse<String> questResponse = sendPost("/policz-aktualny-build", tooManyQuestPoints);

        assertEquals(200, questResponse.statusCode());
        assertTrue(questResponse.body().contains("Dodatkowe punkty z zadań musi być w zakresie 0..14."));

        Map<String, String> tooHighLevel = buildAdvanceFlashFields(10);
        tooHighLevel.put("level", "71");
        tooHighLevel.put("questSkillPoints", "0");

        HttpResponse<String> levelResponse = sendPost("/policz-aktualny-build", tooHighLevel);

        assertEquals(200, levelResponse.statusCode());
        assertTrue(levelResponse.body().contains("Poziom bohatera musi być w zakresie 1..70."));
    }

    @Test
    void shouldShowIllegalConfigurationWhenSpentSkillPointsExceedBudget() throws Exception {
        createHero("Testowy bohater", "13");

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("level", "1");
        fields.put("questSkillPoints", "0");

        HttpResponse<String> response = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Nie można zapisać: wydano 7 punktów, dostępne 0."));
        assertFalse(response.body().contains("Konfiguracja punktów umiejętności wymaga poprawy przed uznaniem buildu za legalny."));
        assertFalse(response.body().contains("Konfiguracja mieści się w budżecie punktów umiejętności."));
        assertFalse(response.body().contains("Łączne obrażenia"));

        HttpResponse<String> reloadResponse = sendGet("/policz-aktualny-build");
        assertEquals(200, reloadResponse.statusCode());
        assertTrue(reloadResponse.body().contains(summaryCard("Poziom bohatera", "13")));
    }

    @Test
    void shouldRenderOnlySelectedClashModifierAsActive() throws Exception {
        createHero("Testowy bohater", "13");
        assignSkill(krys.skill.SkillId.CLASH);

        HttpResponse<String> response = sendPost("/policz-aktualny-build", buildClashPunishmentFields(10));

        assertEquals(200, response.statusCode());
        String clashCard = assignedSkillCard(response.body(), "CLASH");
        String visibleCard = stripTooltipAttributes(clashCard);
        assertTrue(visibleCard.contains(">Kara</li>"));
        assertTrue(clashCard.contains("Manual review: Kara"));
        assertTrue(clashCard.contains("Odwet"));
        assertTrue(clashCard.contains("3489 cierni"));
        assertFalse(visibleCard.contains(">Zwiększenie Obrażeń</li>"));
        assertFalse(visibleCard.contains(">Brać Ich</li>"));
        assertFalse(visibleCard.contains(">Potyczka</li>"));
        assertFalse(visibleCard.contains(">Animusz</li>"));
        assertFalse(visibleCard.contains(">Skuteczność Marszu Krzyżowca</li>"));
        assertFalse(visibleCard.contains("+10"));
        assertFalse(visibleCard.contains("20%[X]"));
        assertFalse(visibleCard.contains("8%[X]"));
        assertFalse(visibleCard.contains("155%"));
        assertFalse(visibleCard.contains("15%[X]"));
        assertFalse(visibleCard.contains("25%[+]"));
        assertFalse(visibleCard.contains("25%[X]"));
        assertFalse(visibleCard.contains("30%[+]"));
        assertFalse(visibleCard.contains("3489"));
        assertFalse(visibleCard.contains("Odwet / ciernie"));
        assertFalse(visibleCard.contains("szansa na blok"));
        assertFalse(visibleCard.contains("efekt co 3. atak"));
    }

    @Test
    void shouldRenderCurrentBuildClashNamesInPolishAcrossActionBarResultAndDebug() throws Exception {
        createHero("Polskie nazwy current build", "13");
        saveAndActivateReferenceRuntimeItem();
        assignSkill(krys.skill.SkillId.CLASH);

        HttpResponse<String> response = sendPost("/policz-aktualny-build", buildClashPunishmentFields(9));

        assertEquals(200, response.statusCode());
        String html = response.body();
        String actionBarOption = actionBarOptionForValue(html, "actionBar1", "CLASH");
        assertTrue(actionBarOption.contains(">Starcie</option>"));
        assertFalse(actionBarOption.contains(">Clash</option>"));
        assertTrue(html.contains(summaryCard("Pasek akcji", "Starcie")));
        assertFalse(html.contains(summaryCard("Pasek akcji", "Clash")));

        String directHitDebug = sectionByHeading(html, "Debug bezpośrednich trafień");
        assertTrue(directHitDebug.contains("<h3 title=\"CLASH\" data-skill-id=\"CLASH\">Starcie</h3>"));
        assertFalse(directHitDebug.contains(">Clash</h3>"));

        String stepTrace = sectionByHeading(html, "Ślad kroków symulacji");
        assertTrue(stepTrace.contains("<td>Starcie</td>"));
        assertTrue(stepTrace.contains("Wybrano Starcie"));
        assertFalse(stepTrace.contains("<td>Clash</td>"));
        assertFalse(stepTrace.contains("Wybrano Clash"));
    }

    @Test
    void shouldNotRenderRemovedCurrentBuildDebugDescriptions() throws Exception {
        createHero("Odchudzony debug", "13");

        HttpResponse<String> response = sendGet("/policz-aktualny-build");

        assertEquals(200, response.statusCode());
        String html = response.body();
        assertFalse(html.contains("Faktyczne wartości przekazane do runtime po złożeniu aktywnego bohatera"));
        assertFalse(html.contains("Aktywny bohater + aktywna broń + jawne wkłady current build = wejście runtime"));
        assertFalse(html.contains("Szczegóły pojedynczych trafień obliczonych przez runtime"));
        assertFalse(html.contains("foundation manual simulation dla trybu"));
    }

    @Test
    void shouldRenderEquipmentSectionAndAllowChangingActiveItemPerSlot() throws Exception {
        createHero("Testowy bohater", "13");
        HttpResponse<String> firstSave = sendPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", "sword-a.png",
                "slot", "MAIN_HAND",
                "weaponDamage", "310",
                "strength", "40",
                "intelligence", "0",
                "thorns", "0",
                "blockChance", "0",
                "retributionChance", "0",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, firstSave.statusCode());

        HttpResponse<String> secondSave = sendPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", "sword-b.png",
                "slot", "MAIN_HAND",
                "weaponDamage", "321",
                "strength", "55",
                "intelligence", "0",
                "thorns", "0",
                "blockChance", "0",
                "retributionChance", "0",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, secondSave.statusCode());

        HttpResponse<String> currentBuildResponse = sendGet("/policz-aktualny-build?" + buildCurrentBuildQuery());

        assertEquals(200, currentBuildResponse.statusCode());
        assertTrue(currentBuildResponse.body().contains("name=\"selectedItemId_MAIN_HAND\""));
        assertTrue(currentBuildResponse.body().contains("Wybierz z biblioteki"));
        assertTrue(currentBuildResponse.body().contains("Importuj nowy item"));
        assertFalse(currentBuildResponse.body().contains("Dodaj item do biblioteki"));
        assertTrue(currentBuildResponse.body().contains("Broń główna / sword-a.png"));
        assertTrue(currentBuildResponse.body().contains("Broń główna / sword-b.png"));
        assertFalse(currentBuildResponse.body().contains(">MAIN_HAND<"));

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("selectedItemId_MAIN_HAND", "2");
        fields.put("slotAction", "setActiveSlotItem:MAIN_HAND");

        HttpResponse<String> activateResponse = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, activateResponse.statusCode());
        assertTrue(activateResponse.body().contains("Zmieniono aktywny item dla slotu Broń."));
        assertTrue(activateResponse.body().contains("class=\"status-badge status-active\">Aktywny</span>"));
        assertTrue(activateResponse.body().contains("Broń główna / sword-b.png"));
        assertTrue(activateResponse.body().contains(">Zmień item<"));
        assertTrue(activateResponse.body().contains("Wyczyść slot"));
        assertTrue(activateResponse.body().contains("Łączne obrażenia"));
    }

    @Test
    void shouldApplySelectedItemFromGlobalSaveForEmptySlot() throws Exception {
        createHero("Globalny zapis itemu", "13");
        saveSimpleMainHandItem("sword-global.png", "321", "55");

        HttpResponse<String> initialResponse = sendGet("/policz-aktualny-build");
        assertEquals(200, initialResponse.statusCode());
        assertTrue(equipmentSlotCard(initialResponse.body(), "MAIN_HAND").contains("Slot jest pusty"));

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("selectedItemId_MAIN_HAND", "1");

        HttpResponse<String> saveResponse = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, saveResponse.statusCode());
        assertTrue(saveResponse.body().contains("Zmieniono aktywny item dla slotu Broń."));
        assertTrue(equipmentSlotCard(saveResponse.body(), "MAIN_HAND").contains("class=\"status-badge status-active\">Aktywny</span>"));
        assertTrue(equipmentSlotCard(saveResponse.body(), "MAIN_HAND").contains("Broń główna / sword-global.png"));
        assertFalse(equipmentSlotCard(saveResponse.body(), "MAIN_HAND").contains("Slot jest pusty"));

        HttpResponse<String> reloadResponse = sendGet("/policz-aktualny-build");
        assertEquals(200, reloadResponse.statusCode());
        assertTrue(equipmentSlotCard(reloadResponse.body(), "MAIN_HAND").contains("class=\"status-badge status-active\">Aktywny</span>"));
        assertTrue(equipmentSlotCard(reloadResponse.body(), "MAIN_HAND").contains("Broń główna / sword-global.png"));
        assertFalse(equipmentSlotCard(reloadResponse.body(), "MAIN_HAND").contains("Slot jest pusty"));
    }

    @Test
    void shouldKeepActiveItemWhenGlobalSaveSubmitsEmptySlotSelect() throws Exception {
        createHero("Pusty select nie czyści", "13");
        saveSimpleMainHandItem("sword-keep.png", "321", "55");
        HttpResponse<String> activateResponse = sendPost("/biblioteka-itemow", Map.of(
                "action", "activateItem",
                "itemId", "1",
                "heroSlot", "MAIN_HAND",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, activateResponse.statusCode());

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("selectedItemId_MAIN_HAND", "");

        HttpResponse<String> saveResponse = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, saveResponse.statusCode());
        assertFalse(saveResponse.body().contains("Błędy formularza"));
        assertTrue(equipmentSlotCard(saveResponse.body(), "MAIN_HAND").contains("class=\"status-badge status-active\">Aktywny</span>"));
        assertTrue(equipmentSlotCard(saveResponse.body(), "MAIN_HAND").contains("Broń główna / sword-keep.png"));
        assertFalse(equipmentSlotCard(saveResponse.body(), "MAIN_HAND").contains("Slot jest pusty"));
    }

    @Test
    void shouldStillClearActiveSlotOnlyWithExplicitClearButton() throws Exception {
        createHero("Jawne czyszczenie slotu", "13");
        saveSimpleMainHandItem("sword-clear.png", "321", "55");
        HttpResponse<String> activateResponse = sendPost("/biblioteka-itemow", Map.of(
                "action", "activateItem",
                "itemId", "1",
                "heroSlot", "MAIN_HAND",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, activateResponse.statusCode());

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("slotAction", "clearActiveSlotItem:MAIN_HAND");

        HttpResponse<String> clearResponse = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, clearResponse.statusCode());
        assertTrue(clearResponse.body().contains("Wyczyszczono aktywny item dla slotu Broń."));
        assertTrue(equipmentSlotCard(clearResponse.body(), "MAIN_HAND").contains("Slot jest pusty"));
        assertFalse(equipmentSlotCard(clearResponse.body(), "MAIN_HAND").contains("class=\"status-badge status-active\">Aktywny</span>"));
    }

    @Test
    void shouldCalculateCurrentBuildAndRenderRequiredSections() throws Exception {
        createHero("Testowy bohater", "13");
        saveAndActivateReferenceRuntimeItem();
        assignSkill(krys.skill.SkillId.HOLY_BOLT);
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                buildHolyBoltJudgementFields()
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Łączne obrażenia"));
        assertTrue(response.body().contains(">1732<"));
        assertTrue(response.body().contains("DPS"));
        assertTrue(response.body().contains("Debug bezpośrednich trafień"));
        assertTrue(response.body().contains("Debug opóźnionych trafień"));
        assertTrue(response.body().contains("Debug obrażeń reaktywnych"));
        assertTrue(response.body().contains("Wkład obrażeń reaktywnych"));
        assertTrue(response.body().contains(">800<"));
        assertTrue(response.body().contains(">52<"));
        assertTrue(response.body().contains(">32<"));
        assertTrue(response.body().contains(">13<"));
        assertTrue(response.body().contains(">8<"));
        assertTrue(response.body().contains(">40<"));
        assertTrue(response.body().contains("Ślad kroków symulacji"));
        assertTrue(response.body().contains("Judgement"));
        assertTrue(response.body().contains("Święty Pocisk"));
    }

    @Test
    void shouldRejectChoiceThatDoesNotBelongToSelectedSkill() throws Exception {
        createHero("Testowy bohater", "13");
        assignSkill(krys.skill.SkillId.HOLY_BOLT);
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                buildInvalidHolyBoltChoiceFields()
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Błędy formularza"));
        assertTrue(response.body().contains("Wybrany dodatkowy modyfikator nie jest dostępny dla skilla Święty Pocisk."));
    }

    @Test
    void shouldRenderClashScenarioWithResolveAndReactiveBonuses() throws Exception {
        createHero("Testowy bohater", "13");
        saveAndActivateReferenceRuntimeItem();
        assignSkill(krys.skill.SkillId.CLASH);
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                buildClashPunishmentFields(9)
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Łączne obrażenia"));
        assertTrue(response.body().contains("Wkład obrażeń reaktywnych"));
        assertTrue(response.body().contains("264"));
        assertTrue(response.body().contains("Debug obrażeń reaktywnych"));
        assertTrue(response.body().contains("Resolve aktywny na końcu"));
        assertTrue(response.body().contains("Końcowa szansa bloku"));
        assertTrue(response.body().contains("Końcowy bonus do kolców"));
        assertTrue(response.body().contains("75.00%"));
        assertTrue(response.body().contains(">50<"));
        assertTrue(response.body().contains(">104<"));
        assertTrue(response.body().contains(">64<"));
        assertTrue(response.body().contains(">39<"));
        assertTrue(response.body().contains(">24<"));
        assertTrue(response.body().contains(">88<"));
        assertTrue(response.body().contains("Starcie"));
        assertTrue(response.body().contains("Punishment"));
    }

    @Test
    void shouldRenderAdvanceScenarioWithCooldownAndWait() throws Exception {
        createHero("Testowy bohater", "13");
        saveAndActivateReferenceRuntimeItem();
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                buildAdvanceFlashFields(10)
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Natarcie"));
        assertTrue(response.body().contains("Flash of the Blade"));
        assertTrue(response.body().contains("Łączne obrażenia"));
        assertTrue(response.body().contains("186"));
        assertTrue(response.body().contains("Debug bezpośrednich trafień"));
        assertTrue(response.body().contains(">322<"));
        assertTrue(response.body().contains(">33<"));
        assertTrue(response.body().contains("Ślad kroków symulacji"));
        assertTrue(response.body().contains("WAIT"));
        assertTrue(response.body().contains("odnowienie=tak"));
        assertTrue(response.body().contains("pozostałe odnowienie=7"));
    }

    @Test
    void shouldCalculateEffectiveCurrentBuildWhenManualBaseIsBlankOrZeroAndLibraryCompletesStats() throws Exception {
        createHero("Testowy bohater", "13");
        HttpResponse<String> saveResponse = sendPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", "weapon-library.png",
                "slot", "MAIN_HAND",
                "weaponDamage", "321",
                "strength", "55",
                "intelligence", "0",
                "thorns", "0",
                "blockChance", "0",
                "retributionChance", "0",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, saveResponse.statusCode());

        HttpResponse<String> activateResponse = sendPost("/biblioteka-itemow", Map.of(
                "action", "activateItem",
                "itemId", "1",
                "heroSlot", "MAIN_HAND",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, activateResponse.statusCode());

        Map<String, String> fields = buildAdvanceFlashFields(10);
        fields.put("weaponDamage", "");
        fields.put("strength", "");
        fields.put("intelligence", "0");
        fields.put("thorns", "0");
        fields.put("blockChance", "0");
        fields.put("retributionChance", "0");

        HttpResponse<String> response = sendPost("/policz-aktualny-build", fields);

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("Ręczne nadpisanie statów"));
        assertFalse(response.body().contains("Szczegóły użytych itemów"));
        assertFalse(response.body().contains("Efektywne staty do obliczeń"));
        assertTrue(response.body().contains("Debug symulacji"));
        assertTrue(response.body().contains("Techniczne wejście runtime"));
        assertTrue(response.body().contains("Łączne obrażenia"));
        assertTrue(response.body().contains("Efektywne obrażenia broni"));
        assertTrue(response.body().contains(">321<"));
        assertTrue(response.body().contains("Efektywna siła"));
        assertTrue(response.body().contains(">55<"));
        String runtimeInput = technicalRuntimeInputSection(response.body());
        assertTrue(runtimeInput.contains(runtimeInputCard("Obrażenia broni", "321", "Aktywny item: zapisane obrażenia broni")));
        assertTrue(runtimeInput.contains(runtimeInputCard("Siła", "55", "Aktywne itemy")));
        assertFalse(response.body().contains("Obrażenia broni musi być >= 1."));
    }

    @Test
    void shouldPreserveDecimalPercentagesInCurrentBuildSummarySections() throws Exception {
        createHero("Testowy bohater", "13");
        HttpResponse<String> saveResponse = sendPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", "decimal-shield.png",
                "slot", "OFF_HAND",
                "weaponDamage", "0",
                "strength", "0",
                "intelligence", "0",
                "thorns", "0",
                "blockChance", "18.25",
                "retributionChance", "7.5",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, saveResponse.statusCode());

        HttpResponse<String> activateResponse = sendPost("/biblioteka-itemow", Map.of(
                "action", "activateItem",
                "itemId", "1",
                "heroSlot", "OFF_HAND",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, activateResponse.statusCode());

        HttpResponse<String> response = sendGet("/policz-aktualny-build?" + buildCurrentBuildQueryWithStats("10.5", "2.25"));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains(summaryCard("Szansa na blok z aktywnych itemów [%]", "18.25")));
        assertTrue(response.body().contains(summaryCard("Szansa retribution z aktywnych itemów [%]", "7.5")));
        assertFalse(response.body().contains(summaryCard("Szansa na blok [%]", "28.75")));
        String runtimeInput = technicalRuntimeInputSection(response.body());
        assertTrue(runtimeInput.contains(runtimeInputCard("Szansa bloku [%]", "18.25", "Aktywne itemy")));
        assertTrue(runtimeInput.contains(runtimeInputCard("Szansa retribution [%]", "7.5", "Aktywne itemy")));
        assertFalse(response.body().contains("szansa bloku=18.25, szansa retribution=7.5"));
    }

    private static Map<String, String> buildHolyBoltJudgementFields() {
        Map<String, String> fields = buildBaseReferenceFields("60");
        fields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.HOLY_BOLT), "5");
        fields.put(CurrentBuildFormData.baseUpgradeFieldName(krys.skill.SkillId.HOLY_BOLT), "true");
        fields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.HOLY_BOLT), "NONE");
        fields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.ADVANCE), "0");
        fields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.ADVANCE), "NONE");
        fields.put(CurrentBuildFormData.actionBarFieldName(1), "HOLY_BOLT");
        return fields;
    }

    private static Map<String, String> buildInvalidHolyBoltChoiceFields() {
        Map<String, String> fields = buildHolyBoltJudgementFields();
        fields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.HOLY_BOLT), "LEFT");
        return fields;
    }

    private static Map<String, String> buildClashPunishmentFields(int horizonSeconds) {
        Map<String, String> fields = buildBaseReferenceFields(Integer.toString(horizonSeconds));
        fields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.CLASH), "5");
        fields.put(CurrentBuildFormData.baseUpgradeFieldName(krys.skill.SkillId.CLASH), "true");
        fields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.CLASH), "LEFT");
        fields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.ADVANCE), "0");
        fields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.ADVANCE), "NONE");
        fields.put(CurrentBuildFormData.actionBarFieldName(1), "CLASH");
        return fields;
    }

    private static Map<String, String> buildAdvanceRankOneLevel70Fields() {
        Map<String, String> fields = buildBaseReferenceFields("10");
        fields.put("level", "70");
        fields.put("weaponDamage", "8");
        fields.put("strength", "18");
        fields.put("intelligence", "0");
        fields.put("thorns", "50");
        fields.put("blockChance", "50");
        fields.put("retributionChance", "50");
        fields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.ADVANCE), "1");
        fields.put(CurrentBuildFormData.baseUpgradeFieldName(krys.skill.SkillId.ADVANCE), "true");
        fields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.ADVANCE), "RIGHT");
        fields.put(CurrentBuildFormData.actionBarFieldName(1), "ADVANCE");
        return fields;
    }

    private static Map<String, String> buildAdvanceFlashFields(int horizonSeconds) {
        Map<String, String> fields = buildBaseReferenceFields(Integer.toString(horizonSeconds));
        fields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.ADVANCE), "5");
        fields.put(CurrentBuildFormData.baseUpgradeFieldName(krys.skill.SkillId.ADVANCE), "true");
        fields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.ADVANCE), "RIGHT");
        fields.put(CurrentBuildFormData.actionBarFieldName(1), "ADVANCE");
        return fields;
    }

    private static Map<String, String> buildBaseReferenceFields(String horizonSeconds) {
        Map<String, String> fields = new HashMap<>();
        fields.put("level", "13");
        fields.put("questSkillPoints", "0");
        fields.put("weaponDamage", "8");
        fields.put("strength", "18");
        fields.put("intelligence", "0");
        fields.put("thorns", "50");
        fields.put("blockChance", "50");
        fields.put("retributionChance", "50");
        fields.put("horizonSeconds", horizonSeconds);
        for (krys.skill.SkillId skillId : krys.skill.SkillId.values()) {
            fields.put(CurrentBuildFormData.rankFieldName(skillId), "0");
            fields.put(CurrentBuildFormData.choiceFieldName(skillId), "NONE");
        }
        for (int slot = 1; slot <= CurrentBuildFormData.ACTION_BAR_SLOT_COUNT; slot++) {
            fields.put(CurrentBuildFormData.actionBarFieldName(slot), "NONE");
        }
        return fields;
    }

    private static String buildCurrentBuildQuery() {
        return "level=13&questSkillPoints=0&weaponDamage=8&strength=18&intelligence=0&thorns=50&blockChance=50&retributionChance=50&horizonSeconds=10"
                + "&rank_BRANDISH=0&choiceUpgrade_BRANDISH=NONE"
                + "&rank_HOLY_BOLT=0&choiceUpgrade_HOLY_BOLT=NONE"
                + "&rank_CLASH=0&choiceUpgrade_CLASH=NONE"
                + "&rank_ADVANCE=5&baseUpgrade_ADVANCE=true&choiceUpgrade_ADVANCE=RIGHT"
                + "&actionBar1=ADVANCE&actionBar2=NONE&actionBar3=NONE&actionBar4=NONE&actionBar5=NONE&actionBar6=NONE";
    }

    private static String buildCurrentBuildQueryWithStats(String blockChance, String retributionChance) {
        return "level=13&questSkillPoints=0&weaponDamage=8&strength=18&intelligence=0&thorns=50&blockChance=" + blockChance + "&retributionChance=" + retributionChance + "&horizonSeconds=10"
                + "&rank_BRANDISH=0&choiceUpgrade_BRANDISH=NONE"
                + "&rank_HOLY_BOLT=0&choiceUpgrade_HOLY_BOLT=NONE"
                + "&rank_CLASH=0&choiceUpgrade_CLASH=NONE"
                + "&rank_ADVANCE=5&baseUpgrade_ADVANCE=true&choiceUpgrade_ADVANCE=RIGHT"
                + "&actionBar1=ADVANCE&actionBar2=NONE&actionBar3=NONE&actionBar4=NONE&actionBar5=NONE&actionBar6=NONE";
    }

    private static Map<String, String> verathielImportFields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("formAction", "confirmItem");
        fields.put("sourceImageName", "miecz.png");
        fields.put("slot", "MAIN_HAND");
        fields.put("weaponDamage", "0");
        fields.put("strength", "0");
        fields.put("intelligence", "0");
        fields.put("thorns", "0");
        fields.put("blockChance", "0");
        fields.put("retributionChance", "0");
        fields.put("itemName", "Odłamek Verathiela");
        fields.put("itemType", "Miecz");
        fields.put("itemRarity", "UNIQUE");
        fields.put("isAncient", "true");
        fields.put("itemPower", "900");
        fields.put("weaponDps", "1830");
        fields.put("weaponDamageMin", "1350");
        fields.put("weaponDamageMax", "1978");
        fields.put("averageWeaponDamage", "1664");
        fields.put("attacksPerSecond", "1.10");
        fields.put("uniqueEffectText", "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100], ale dodatkowo zużywają 25 pkt. podstawowego zasobu.");
        fields.put("selectedAspectId", "verathiel_shard");
        fields.put("affixCount", "4");
        putAffix(fields, 0, "WEAPON_DAMAGE_FLAT", "94", "+94 obrażeń od broni [94 - 157]",
                "verathiel_weapon_damage_flat", "94", "157", "");
        putAffix(fields, 1, "MAXIMUM_LIFE", "2141", "+2 141 maksymalnego zdrowia [1 831 - 2 200]",
                "verathiel_maximum_life", "1831", "2200", "");
        putAffix(fields, 2, "LIFE_ON_HIT", "545", "+545 pkt. zdrowia przy trafieniu [526 - 632]",
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

    private static String summaryCard(String label, String value) {
        return """
                <article class="summary-card">
                    <div class="summary-label">""" + label + """
                </div>
                    <div class="summary-value">""" + value + """
                </div>
                </article>
                """;
    }

    private static String summaryCardWithTooltipPrefix(String label, String value, String tooltip) {
        return "<article class=\"summary-card\" title=\"" + tooltip
                + "\" aria-label=\"" + label + ": " + value + ". " + tooltip + "\">\n"
                + "    <div class=\"summary-label\">" + label + "\n"
                + "</div>\n"
                + "    <div class=\"summary-value\">" + value + "\n"
                + "</div>\n"
                + "</article>\n";
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

    private static String formatWholeForTest(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private static String activeSlotCard(String html) {
        int itemIndex = html.indexOf("Odłamek Verathiela");
        if (itemIndex < 0) {
            throw new AssertionError("Brak aktywnej karty Odłamka Verathiela.");
        }
        int start = html.lastIndexOf("<article class=\"equipment-slot", itemIndex);
        int end = html.indexOf("</article>", itemIndex);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć karty aktywnego slotu.");
        }
        return html.substring(start, end);
    }

    private static String equipmentSlotCard(String html, String slotName) {
        String marker = "<article class=\"equipment-slot equipment-slot-" + slotName.toLowerCase(java.util.Locale.ROOT);
        int start = html.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("Brak karty slotu: " + slotName);
        }
        int end = html.indexOf("</article>", start);
        if (end < 0) {
            throw new AssertionError("Nie udało się wyciąć karty slotu: " + slotName);
        }
        return html.substring(start, end);
    }

    private static String itemSelectOptionForValue(String html, String slot, String value) {
        String selectMarker = "name=\"selectedItemId_" + slot + "\"";
        int selectStart = html.indexOf(selectMarker);
        if (selectStart < 0) {
            throw new AssertionError("Brak selecta itemu dla slotu: " + slot);
        }
        int selectEnd = html.indexOf("</select>", selectStart);
        if (selectEnd < 0) {
            throw new AssertionError("Nie udało się wyciąć selecta itemu dla slotu: " + slot);
        }
        String selectHtml = html.substring(selectStart, selectEnd);
        String marker = "<option value=\"" + value + "\"";
        int start = selectHtml.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("Brak opcji selecta o wartości: " + value);
        }
        int end = selectHtml.indexOf("</option>", start);
        if (end < 0) {
            throw new AssertionError("Nie udało się wyciąć opcji selecta o wartości: " + value);
        }
        return selectHtml.substring(start, end);
    }

    private static String actionBarOptionForValue(String html, String fieldName, String value) {
        String selectMarker = "name=\"" + fieldName + "\"";
        int selectStart = html.indexOf(selectMarker);
        if (selectStart < 0) {
            throw new AssertionError("Brak selecta paska akcji: " + fieldName);
        }
        int selectEnd = html.indexOf("</select>", selectStart);
        if (selectEnd < 0) {
            throw new AssertionError("Nie udało się wyciąć selecta paska akcji: " + fieldName);
        }
        String selectHtml = html.substring(selectStart, selectEnd);
        String marker = "<option value=\"" + value + "\"";
        int start = selectHtml.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("Brak opcji paska akcji o wartości: " + value);
        }
        int end = selectHtml.indexOf("</option>", start);
        if (end < 0) {
            throw new AssertionError("Nie udało się wyciąć opcji paska akcji o wartości: " + value);
        }
        return selectHtml.substring(start, end + "</option>".length());
    }

    private static String heroStatGroup(String html, String title) {
        String marker = "<h3>" + title;
        int heading = html.indexOf(marker);
        if (heading < 0) {
            throw new AssertionError("Brak grupy statystyk: " + title);
        }
        int start = html.lastIndexOf("<div class=\"hero-stat-group", heading);
        int next = html.indexOf("<div class=\"hero-stat-group", heading + marker.length());
        int detailsEnd = html.indexOf("</section>", heading);
        int end = next >= 0 ? next : detailsEnd;
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć grupy statystyk: " + title);
        }
        return html.substring(start, end);
    }

    private static String sectionByHeading(String html, String headingText) {
        String marker = "<h2>" + headingText + "</h2>";
        int heading = html.indexOf(marker);
        if (heading < 0) {
            throw new AssertionError("Brak sekcji: " + headingText);
        }
        int start = html.lastIndexOf("<section", heading);
        int end = html.indexOf("</section>", heading);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć sekcji: " + headingText);
        }
        return html.substring(start, end + "</section>".length());
    }

    private static String technicalRuntimeInputSection(String html) {
        int heading = html.indexOf("<h2>Techniczne wejście runtime</h2>");
        if (heading < 0) {
            throw new AssertionError("Brak sekcji technicznego wejścia runtime.");
        }
        int start = html.lastIndexOf("<section", heading);
        int end = html.indexOf("</section>", heading);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć sekcji technicznego wejścia runtime.");
        }
        return html.substring(start, end + "</section>".length());
    }

    private void createHero(String heroName, String heroLevel) throws Exception {
        HttpResponse<String> response = sendPost("/bohaterowie", Map.of(
                "action", "createHero",
                "heroName", heroName,
                "heroClass", "PALADIN",
                "heroLevel", heroLevel
        ));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Utworzono bohatera " + heroName + "."));
    }

    private void assignSkill(krys.skill.SkillId skillId) throws Exception {
        HttpResponse<String> response = sendPost("/policz-aktualny-build", Map.of(
                "heroAction", "addAssignedSkill",
                "skillIdToAdd", skillId.name()
        ));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Dodano umiejętność " + HeroSkillCatalogAdapter.displayName(skillId) + " do bohatera."));
    }

    private void saveAndActivateVerathiel() throws Exception {
        HttpResponse<String> saveResponse = sendPost("/importuj-item-ze-screena", verathielImportFields());
        assertEquals(200, saveResponse.statusCode());

        HttpResponse<String> activateResponse = sendPost("/biblioteka-itemow", Map.of(
                "action", "activateItem",
                "itemId", "1",
                "heroSlot", "MAIN_HAND",
                "currentBuildQuery", ""
        ));
        assertEquals(200, activateResponse.statusCode());
    }

    private void saveAndActivateReferenceRuntimeItem() throws Exception {
        HttpResponse<String> saveResponse = sendPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", "reference-runtime-item.png",
                "slot", "MAIN_HAND",
                "weaponDamage", "8",
                "strength", "18",
                "intelligence", "0",
                "thorns", "50",
                "blockChance", "50",
                "retributionChance", "50",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, saveResponse.statusCode());

        HttpResponse<String> activateResponse = sendPost("/biblioteka-itemow", Map.of(
                "action", "activateItem",
                "itemId", "1",
                "heroSlot", "MAIN_HAND",
                "currentBuildQuery", ""
        ));
        assertEquals(200, activateResponse.statusCode());
    }

    private void saveSimpleMainHandItem(String sourceImageName, String weaponDamage, String strength) throws Exception {
        HttpResponse<String> saveResponse = sendPost("/biblioteka-itemow", Map.of(
                "action", "saveImportedItem",
                "sourceImageName", sourceImageName,
                "slot", "MAIN_HAND",
                "weaponDamage", weaponDamage,
                "strength", strength,
                "intelligence", "0",
                "thorns", "0",
                "blockChance", "0",
                "retributionChance", "0",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, saveResponse.statusCode());
    }

    private static String assignedSkillCard(String html, String skillId) {
        String marker = "<article class=\"skill-card\" data-assigned-skill-id=\"" + skillId + "\">";
        int start = html.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("Brak karty przypisanej umiejętności: " + skillId);
        }
        int nextCard = html.indexOf("<article class=\"skill-card\" data-assigned-skill-id=\"", start + marker.length());
        int gridEnd = html.indexOf("</div></section>", start);
        int end = nextCard >= 0 ? nextCard : gridEnd;
        if (end < 0) {
            throw new AssertionError("Nie udało się wyznaczyć końca karty przypisanej umiejętności: " + skillId);
        }
        return html.substring(start, end);
    }

    private static String stripTooltipAttributes(String html) {
        return html
                .replaceAll(" title=\"[^\"]*\"", "")
                .replaceAll(" aria-label=\"[^\"]*\"", "");
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendPost(String path, Map<String, String> formFields) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(toFormBody(formFields), StandardCharsets.UTF_8))
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
