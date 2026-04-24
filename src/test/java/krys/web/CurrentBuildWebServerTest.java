package krys.web;

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
        assertTrue(response.body().contains("Aktywny bohater"));
        assertTrue(response.body().contains("name=\"selectedHeroId\""));
        assertTrue(response.body().contains("name=\"heroLevelEdit\""));
        assertTrue(response.body().contains("Ekwipunek aktualnego buildu"));
        assertTrue(response.body().contains("Użyte itemy"));
        assertTrue(response.body().contains("Efektywne staty do obliczeń"));
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
        assertTrue(response.body().contains("Zaawansowane: ręczne nadpisanie statów"));
        assertTrue(response.body().contains("Slot jest pusty"));
        assertTrue(response.body().contains(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.ADVANCE)));
        assertFalse(response.body().contains(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.HOLY_BOLT)));
        assertTrue(response.body().contains("<input type=\"number\" step=\"1\" name=\"weaponDamage\" value=\"8\">"));
        assertFalse(response.body().contains("min=\"1\" step=\"1\" name=\"weaponDamage\""));
        assertTrue(response.body().contains("Szansa bloku [%]"));
        assertTrue(response.body().contains("Szansa retribution [%]"));
        assertTrue(response.body().contains("name=\"actionBar1\""));
        assertTrue(response.body().contains("name=\"horizonSeconds\""));
        assertTrue(response.body().contains("Otwórz bibliotekę itemów"));
        assertTrue(response.body().contains("Importuj nowy item"));
        assertTrue(response.body().contains("Wybierz z biblioteki"));
        assertFalse(response.body().contains("Centrum buildu"));
        assertTrue(response.body().contains("max-width: 1460px;"));
        assertTrue(response.body().indexOf("Ekwipunek aktualnego buildu") < response.body().indexOf("Efektywne staty do obliczeń"));
        assertTrue(response.body().indexOf("Hełm") < response.body().indexOf("Broń"));
        assertTrue(response.body().indexOf("Amulet") < response.body().indexOf("Tarcza"));
    }

    @Test
    void shouldAllowInlineSwitchingActiveHeroAndUpdatingHeroLevel() throws Exception {
        createHero("Alaric", "13");
        createHero("Gregor", "25");

        HttpResponse<String> switchResponse = sendPost("/policz-aktualny-build", Map.of(
                "heroAction", "setActiveHeroInline",
                "selectedHeroId", "2"
        ));

        assertEquals(200, switchResponse.statusCode());
        assertTrue(switchResponse.body().contains("Zmieniono aktywnego bohatera bez opuszczania ekranu buildu."));
        assertTrue(switchResponse.body().contains("Gregor"));

        HttpResponse<String> levelResponse = sendPost("/policz-aktualny-build", Map.of(
                "heroAction", "updateHeroLevel",
                "heroLevelEdit", "31"
        ));

        assertEquals(200, levelResponse.statusCode());
        assertTrue(levelResponse.body().contains("Zapisano poziom aktywnego bohatera."));
        assertTrue(levelResponse.body().contains(summaryCard("Poziom bohatera", "31")));
    }

    @Test
    void shouldRenderOnlyAssignedSkillsAndSanitizeActionBarToAssignedLearnedSkills() throws Exception {
        createHero("Testowy bohater", "13");

        HttpResponse<String> initialResponse = sendGet("/policz-aktualny-build");
        assertEquals(200, initialResponse.statusCode());
        assertTrue(initialResponse.body().contains(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.ADVANCE)));
        assertFalse(initialResponse.body().contains(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.HOLY_BOLT)));

        HttpResponse<String> addSkillResponse = sendPost("/policz-aktualny-build", Map.of(
                "heroAction", "addAssignedSkill",
                "skillIdToAdd", "HOLY_BOLT"
        ));
        assertEquals(200, addSkillResponse.statusCode());
        assertFalse(addSkillResponse.body().contains("Wybierz poprawną umiejętność do przypisania bohaterowi."));
        assertTrue(addSkillResponse.body().contains("Dodano umiejętność Holy Bolt do bohatera."));
        assertTrue(addSkillResponse.body().contains(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.HOLY_BOLT)));

        Map<String, String> invalidBarFields = buildAdvanceFlashFields(10);
        invalidBarFields.put(CurrentBuildFormData.rankFieldName(krys.skill.SkillId.HOLY_BOLT), "0");
        invalidBarFields.put(CurrentBuildFormData.choiceFieldName(krys.skill.SkillId.HOLY_BOLT), "NONE");
        invalidBarFields.put(CurrentBuildFormData.actionBarFieldName(1), "HOLY_BOLT");

        HttpResponse<String> sanitizeResponse = sendPost("/policz-aktualny-build", invalidBarFields);

        assertEquals(200, sanitizeResponse.statusCode());
        assertTrue(sanitizeResponse.body().contains("Pasek akcji został oczyszczony do umiejętności przypisanych i nauczonych przez aktywnego bohatera."));
        assertFalse(sanitizeResponse.body().contains("Action bar slot 1 wskazuje skill bez rank > 0"));
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
        assertTrue(activateResponse.body().contains("obrażenia broni=321"));
        assertTrue(activateResponse.body().contains("Łączne obrażenia"));
    }

    @Test
    void shouldCalculateCurrentBuildAndRenderRequiredSections() throws Exception {
        createHero("Testowy bohater", "13");
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
        assertTrue(response.body().contains("Holy Bolt"));
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
        assertTrue(response.body().contains("Wybrany dodatkowy modyfikator nie jest dostępny dla skilla Holy Bolt."));
    }

    @Test
    void shouldRenderClashScenarioWithResolveAndReactiveBonuses() throws Exception {
        createHero("Testowy bohater", "13");
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
        assertTrue(response.body().contains("Clash"));
        assertTrue(response.body().contains("Punishment"));
    }

    @Test
    void shouldRenderAdvanceScenarioWithCooldownAndWait() throws Exception {
        createHero("Testowy bohater", "13");
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                buildAdvanceFlashFields(10)
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Advance"));
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
        assertTrue(response.body().contains("Ręczne nadpisanie statów"));
        assertTrue(response.body().contains("Użyte itemy"));
        assertTrue(response.body().contains("Efektywne staty do obliczeń"));
        assertTrue(response.body().contains("Łączne obrażenia"));
        assertTrue(response.body().contains("Efektywne obrażenia broni"));
        assertTrue(response.body().contains(">321<"));
        assertTrue(response.body().contains("Efektywna siła"));
        assertTrue(response.body().contains(">55<"));
        assertTrue(response.body().contains("Do runtime trafiają: obrażenia broni=321, siła=55"));
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
        assertTrue(response.body().contains(summaryCard("Szansa bloku [%]", "10.5")));
        assertTrue(response.body().contains(summaryCard("Szansa retribution [%]", "2.25")));
        assertTrue(response.body().contains(summaryCard("Łączna szansa bloku [%]", "18.25")));
        assertTrue(response.body().contains(summaryCard("Łączna szansa retribution [%]", "7.5")));
        assertTrue(response.body().contains(summaryCard("Szansa bloku [%]", "28.75")));
        assertTrue(response.body().contains(summaryCard("Szansa retribution [%]", "9.75")));
        assertTrue(response.body().contains("szansa bloku=18.25%, szansa retribution=7.5%"));
        assertTrue(response.body().contains("szansa bloku=28.75, szansa retribution=9.75"));
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
        fields.put(CurrentBuildFormData.actionBarFieldName(1), "NONE");
        fields.put(CurrentBuildFormData.actionBarFieldName(2), "NONE");
        fields.put(CurrentBuildFormData.actionBarFieldName(3), "NONE");
        fields.put(CurrentBuildFormData.actionBarFieldName(4), "NONE");
        return fields;
    }

    private static String buildCurrentBuildQuery() {
        return "level=13&weaponDamage=8&strength=18&intelligence=0&thorns=50&blockChance=50&retributionChance=50&horizonSeconds=10"
                + "&rank_BRANDISH=0&choiceUpgrade_BRANDISH=NONE"
                + "&rank_HOLY_BOLT=0&choiceUpgrade_HOLY_BOLT=NONE"
                + "&rank_CLASH=0&choiceUpgrade_CLASH=NONE"
                + "&rank_ADVANCE=5&baseUpgrade_ADVANCE=true&choiceUpgrade_ADVANCE=RIGHT"
                + "&actionBar1=ADVANCE&actionBar2=NONE&actionBar3=NONE&actionBar4=NONE";
    }

    private static String buildCurrentBuildQueryWithStats(String blockChance, String retributionChance) {
        return "level=13&weaponDamage=8&strength=18&intelligence=0&thorns=50&blockChance=" + blockChance + "&retributionChance=" + retributionChance + "&horizonSeconds=10"
                + "&rank_BRANDISH=0&choiceUpgrade_BRANDISH=NONE"
                + "&rank_HOLY_BOLT=0&choiceUpgrade_HOLY_BOLT=NONE"
                + "&rank_CLASH=0&choiceUpgrade_CLASH=NONE"
                + "&rank_ADVANCE=5&baseUpgrade_ADVANCE=true&choiceUpgrade_ADVANCE=RIGHT"
                + "&actionBar1=ADVANCE&actionBar2=NONE&actionBar3=NONE&actionBar4=NONE";
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
        assertTrue(response.body().contains("Dodano umiejętność " + krys.skill.PaladinSkillDefs.get(skillId).getName() + " do bohatera."));
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
