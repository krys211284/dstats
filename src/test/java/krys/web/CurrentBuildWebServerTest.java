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

/** Testy GUI pokrywają endpoint formularza i podstawowy render wyniku bez powielania logiki runtime. */
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
    void shouldRenderFormForCurrentBuildPage() throws Exception {
        HttpResponse<String> response = sendGet("/policz-aktualny-build");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Policz aktualny build"));
        assertTrue(response.body().contains("Wejście current build"));
        assertTrue(response.body().contains("Baza ręczna"));
        assertTrue(response.body().contains("Aktywne itemy z biblioteki"));
        assertTrue(response.body().contains("Efektywne staty do obliczeń"));
        assertTrue(response.body().contains("name=\"level\""));
        assertTrue(response.body().contains("<input type=\"number\" step=\"1\" name=\"weaponDamage\" value=\"8\">"));
        assertFalse(response.body().contains("min=\"1\" step=\"1\" name=\"weaponDamage\""));
        assertTrue(response.body().contains("Block chance [%]"));
        assertTrue(response.body().contains("Retribution chance [%]"));
        assertTrue(response.body().contains("name=\"actionBar1\""));
        assertTrue(response.body().contains("name=\"horizonSeconds\""));
        assertTrue(response.body().contains("Otwórz bibliotekę itemów"));
    }

    @Test
    void shouldCalculateCurrentBuildAndRenderRequiredSections() throws Exception {
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                buildHolyBoltJudgementFields()
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Total damage"));
        assertTrue(response.body().contains(">1732<"));
        assertTrue(response.body().contains("DPS"));
        assertTrue(response.body().contains("Debug bezpośrednich hitów"));
        assertTrue(response.body().contains("Delayed hit debug"));
        assertTrue(response.body().contains("Reactive debug"));
        assertTrue(response.body().contains("Reactive contribution"));
        assertTrue(response.body().contains(">800<"));
        assertTrue(response.body().contains(">52<"));
        assertTrue(response.body().contains(">32<"));
        assertTrue(response.body().contains(">13<"));
        assertTrue(response.body().contains(">8<"));
        assertTrue(response.body().contains(">40<"));
        assertTrue(response.body().contains("Step trace"));
        assertTrue(response.body().contains("Judgement"));
        assertTrue(response.body().contains("Holy Bolt"));
    }

    @Test
    void shouldRejectChoiceThatDoesNotBelongToSelectedSkill() throws Exception {
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
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                buildClashPunishmentFields(9)
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Total damage"));
        assertTrue(response.body().contains("Reactive contribution"));
        assertTrue(response.body().contains("264"));
        assertTrue(response.body().contains("Reactive debug"));
        assertTrue(response.body().contains("Resolve aktywny na końcu"));
        assertTrue(response.body().contains("Active block chance na końcu"));
        assertTrue(response.body().contains("Active thorns bonus na końcu"));
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
        HttpResponse<String> response = sendPost(
                "/policz-aktualny-build",
                buildAdvanceFlashFields(10)
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Advance"));
        assertTrue(response.body().contains("Flash of the Blade"));
        assertTrue(response.body().contains("Total damage"));
        assertTrue(response.body().contains("186"));
        assertTrue(response.body().contains("Debug bezpośrednich hitów"));
        assertTrue(response.body().contains(">322<"));
        assertTrue(response.body().contains(">33<"));
        assertTrue(response.body().contains("Step trace"));
        assertTrue(response.body().contains("WAIT"));
        assertTrue(response.body().contains("cooldown=true"));
        assertTrue(response.body().contains("cooldownRemaining=7"));
    }

    @Test
    void shouldCalculateEffectiveCurrentBuildWhenManualBaseIsBlankOrZeroAndLibraryCompletesStats() throws Exception {
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
                "slot", "MAIN_HAND",
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
        assertTrue(response.body().contains("Baza ręczna"));
        assertTrue(response.body().contains("Aktywne itemy z biblioteki"));
        assertTrue(response.body().contains("Efektywne staty do obliczeń"));
        assertTrue(response.body().contains("Total damage"));
        assertTrue(response.body().contains("Weapon damage effective"));
        assertTrue(response.body().contains(">321<"));
        assertTrue(response.body().contains("Strength effective"));
        assertTrue(response.body().contains(">55<"));
        assertTrue(response.body().contains("Do obliczeń runtime trafiają: weapon damage=321, strength=55"));
        assertFalse(response.body().contains("Weapon damage musi być >= 1."));
    }

    @Test
    void shouldPreserveDecimalPercentagesInCurrentBuildSummarySections() throws Exception {
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
                "slot", "OFF_HAND",
                "currentBuildQuery", buildCurrentBuildQuery()
        ));
        assertEquals(200, activateResponse.statusCode());

        HttpResponse<String> response = sendGet("/policz-aktualny-build?" + buildCurrentBuildQueryWithStats("10.5", "2.25"));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains(summaryCard("Block chance [%]", "10.5")));
        assertTrue(response.body().contains(summaryCard("Retribution chance [%]", "2.25")));
        assertTrue(response.body().contains(summaryCard("Block chance [%]", "18.25")));
        assertTrue(response.body().contains(summaryCard("Retribution chance [%]", "7.5")));
        assertTrue(response.body().contains(summaryCard("Block chance [%]", "28.75")));
        assertTrue(response.body().contains(summaryCard("Retribution chance [%]", "9.75")));
        assertTrue(response.body().contains("block=18.25%, retribution=7.5%"));
        assertTrue(response.body().contains("block chance=28.75, retribution chance=9.75"));
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
