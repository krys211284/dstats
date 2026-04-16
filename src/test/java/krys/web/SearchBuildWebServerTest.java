package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.app.CurrentBuildCalculationService;
import krys.combat.DamageEngine;
import krys.simulation.ManualSimulationService;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy GUI searcha pokrywają minimalny flow SSR bez powielania logiki backendowego searcha. */
class SearchBuildWebServerTest {
    private CurrentBuildWebServer webServer;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        webServer = new CurrentBuildWebServer(0);
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
    void shouldRenderFormForSearchBuildPage() throws Exception {
        HttpResponse<String> response = sendGet("/znajdz-najlepszy-build");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Znajdź najlepszy build"));
        assertTrue(response.body().contains("Przestrzeń searcha"));
        assertTrue(response.body().contains("name=\"levelValues\""));
        assertTrue(response.body().contains("name=\"weaponDamageValues\""));
        assertTrue(response.body().contains("name=\"actionBarSizes\""));
        assertTrue(response.body().contains("name=\"topResultsLimit\""));
        assertTrue(response.body().contains(SearchBuildFormData.rankValuesFieldName(krys.skill.SkillId.ADVANCE)));
    }

    @Test
    void shouldRunSearchAndRenderRequiredSections() throws Exception {
        HttpResponse<String> response = sendPost(
                "/znajdz-najlepszy-build",
                buildReferenceSearchFields()
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Wejściowa przestrzeń searcha"));
        assertTrue(response.body().contains("Ocenieni kandydaci"));
        assertTrue(response.body().contains(">2949<"));
        assertTrue(response.body().contains("Wyniki po normalizacji"));
        assertTrue(response.body().contains(">137<"));
        assertTrue(response.body().contains("Top wyniki po normalizacji"));
        assertTrue(response.body().contains("Total damage"));
        assertTrue(response.body().contains(">439<"));
        assertTrue(response.body().contains("DPS"));
        assertTrue(response.body().contains("48.7778"));
        assertTrue(response.body().contains("Build input"));
        assertTrue(response.body().contains("Action bar skills"));
        assertTrue(response.body().contains("Advance -&gt; Clash"));
        assertTrue(response.body().contains("/znajdz-najlepszy-build/szczegoly"));
        assertTrue(response.body().contains("Pokaż pełną analizę kandydata"));
    }

    @Test
    void shouldAllowDrillDownFromSearchResultToCandidateDetailsOnSameRuntime() throws Exception {
        HttpResponse<String> searchResponse = sendPost(
                "/znajdz-najlepszy-build",
                buildReferenceSearchFields()
        );

        assertEquals(200, searchResponse.statusCode());
        Map<String, String> detailFields = extractFirstDetailsFormFields(searchResponse.body());

        HttpResponse<String> detailResponse = sendPost(
                "/znajdz-najlepszy-build/szczegoly",
                detailFields
        );

        assertEquals(200, detailResponse.statusCode());
        assertTrue(detailResponse.body().contains("Szczegóły wyniku searcha"));
        assertTrue(detailResponse.body().contains("Wybrany wynik po normalizacji"));
        assertTrue(detailResponse.body().contains("#1"));
        assertTrue(detailResponse.body().contains("Build input"));
        assertTrue(detailResponse.body().contains("Action bar skills"));
        assertTrue(detailResponse.body().contains("Action bar"));
        assertTrue(detailResponse.body().contains("Total damage"));
        assertTrue(detailResponse.body().contains("DPS"));
        assertTrue(detailResponse.body().contains("Direct hit debug"));
        assertTrue(detailResponse.body().contains("Delayed hit debug"));
        assertTrue(detailResponse.body().contains("Reactive debug"));
        assertTrue(detailResponse.body().contains("Step trace"));
        assertTrue(detailResponse.body().contains("Judgement aktywny na końcu"));
        assertTrue(detailResponse.body().contains("Resolve aktywny na końcu"));
        assertTrue(detailResponse.body().contains("Active block chance na końcu"));
        assertTrue(detailResponse.body().contains("Active thorns bonus na końcu"));
        assertTrue(detailResponse.body().contains("Advance"));
        assertTrue(detailResponse.body().contains("Clash"));

        CurrentBuildCalculation expectedCalculation = calculateDrillDownExpectedResult(detailFields);
        assertTrue(detailResponse.body().contains(">" + expectedCalculation.getResult().getTotalDamage() + "<"));
        assertTrue(detailResponse.body().contains(String.format(Locale.US, "%.4f", expectedCalculation.getResult().getDps())));
    }

    private static Map<String, String> buildReferenceSearchFields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("levelValues", "13");
        fields.put("weaponDamageValues", "8");
        fields.put("strengthValues", "18");
        fields.put("intelligenceValues", "0");
        fields.put("thornsValues", "50");
        fields.put("blockChanceValues", "50");
        fields.put("retributionChanceValues", "50");
        fields.put("actionBarSizes", "1,2");
        fields.put("horizonSeconds", "9");
        fields.put("topResultsLimit", "5");
        fields.put(SearchBuildFormData.rankValuesFieldName(krys.skill.SkillId.BRANDISH), "0,5");
        fields.put(SearchBuildFormData.baseUpgradeValuesFieldName(krys.skill.SkillId.BRANDISH), "false,true");
        fields.put(SearchBuildFormData.choiceValuesFieldName(krys.skill.SkillId.BRANDISH), "NONE,LEFT,RIGHT");
        fields.put(SearchBuildFormData.rankValuesFieldName(krys.skill.SkillId.HOLY_BOLT), "0,5");
        fields.put(SearchBuildFormData.baseUpgradeValuesFieldName(krys.skill.SkillId.HOLY_BOLT), "false,true");
        fields.put(SearchBuildFormData.choiceValuesFieldName(krys.skill.SkillId.HOLY_BOLT), "NONE");
        fields.put(SearchBuildFormData.rankValuesFieldName(krys.skill.SkillId.CLASH), "0,5");
        fields.put(SearchBuildFormData.baseUpgradeValuesFieldName(krys.skill.SkillId.CLASH), "false,true");
        fields.put(SearchBuildFormData.choiceValuesFieldName(krys.skill.SkillId.CLASH), "NONE,LEFT");
        fields.put(SearchBuildFormData.rankValuesFieldName(krys.skill.SkillId.ADVANCE), "0,5");
        fields.put(SearchBuildFormData.baseUpgradeValuesFieldName(krys.skill.SkillId.ADVANCE), "false,true");
        fields.put(SearchBuildFormData.choiceValuesFieldName(krys.skill.SkillId.ADVANCE), "NONE,LEFT,RIGHT");
        return fields;
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

    private static Map<String, String> extractFirstDetailsFormFields(String html) {
        Matcher formMatcher = Pattern.compile("(?s)<form method=\"post\" action=\"/znajdz-najlepszy-build/szczegoly\" class=\"detail-form\">(.*?)</form>")
                .matcher(html);
        assertTrue(formMatcher.find(), "Brak formularza drill-downu w odpowiedzi searcha.");

        Matcher inputMatcher = Pattern.compile("<input type=\"hidden\" name=\"([^\"]+)\" value=\"([^\"]*)\">")
                .matcher(formMatcher.group(1));
        Map<String, String> fields = new LinkedHashMap<>();
        while (inputMatcher.find()) {
            fields.put(inputMatcher.group(1), htmlUnescape(inputMatcher.group(2)));
        }
        return fields;
    }

    private static CurrentBuildCalculation calculateDrillDownExpectedResult(Map<String, String> formFields) {
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(formFields);
        CurrentBuildFormMapper.MappingResult mappingResult = new CurrentBuildFormMapper().map(formData);
        assertTrue(mappingResult.getErrors().isEmpty(), "Drill-down powinien przekazywać legalny CurrentBuildRequest.");
        return new CurrentBuildCalculationService(
                new ManualSimulationService(new DamageEngine())
        ).calculate(mappingResult.getRequest());
    }

    private static String htmlUnescape(String value) {
        return value
                .replace("&quot;", "\"")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&amp;", "&");
    }
}
