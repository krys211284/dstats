package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.app.CurrentBuildCalculationService;
import krys.combat.DamageEngine;
import krys.item.EquipmentSlot;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
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
import java.nio.file.Files;
import java.nio.file.Path;
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
    private Path tempDirectory;

    @BeforeEach
    void setUp() throws IOException {
        tempDirectory = Files.createTempDirectory("search-build-web");
        seedItemLibrary(tempDirectory);
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
        HttpResponse<String> response = sendGet("/znajdz-najlepszy-build");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Brak aktywnego bohatera"));
        assertTrue(response.body().contains("Przejdź do modułu Bohaterowie"));
    }

    @Test
    void shouldRenderFormForSearchBuildPage() throws Exception {
        createHero("Szperacz", "13");
        HttpResponse<String> response = sendGet("/znajdz-najlepszy-build");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Znajdź najlepszy build"));
        assertTrue(response.body().contains("Aktywny bohater searcha"));
        assertTrue(response.body().contains("Konfiguracja searcha"));
        assertTrue(response.body().contains("Tryb biblioteki itemów"));
        assertTrue(response.body().contains("Przestrzeń statów wejściowych"));
        assertTrue(response.body().contains("name=\"levelValues\""));
        assertTrue(response.body().contains("name=\"weaponDamageValues\""));
        assertTrue(response.body().contains("name=\"useItemLibrary\""));
        assertTrue(response.body().contains("name=\"actionBarSizes\""));
        assertTrue(response.body().contains("name=\"topResultsLimit\""));
        assertTrue(response.body().contains(SearchBuildFormData.rankValuesFieldName(krys.skill.SkillId.ADVANCE)));
        assertFalse(response.body().contains(SearchBuildFormData.rankValuesFieldName(krys.skill.SkillId.CLASH)));
    }

    @Test
    void shouldRunSearchAndRenderRequiredSections() throws Exception {
        createHero("Szperacz", "13");
        assignAllFoundationSkills();
        HttpResponse<String> response = sendPost(
                "/znajdz-najlepszy-build",
                buildReferenceSearchFields()
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Wejściowa przestrzeń searcha"));
        assertTrue(response.body().contains("Audit i preflight searcha"));
        assertTrue(response.body().contains("Liczba legalnych kandydatów"));
        assertTrue(response.body().contains("Rozmiar przestrzeni statów"));
        assertTrue(response.body().contains("Rozmiar przestrzeni skilli"));
        assertTrue(response.body().contains("Rozmiar przestrzeni paska akcji"));
        assertTrue(response.body().contains("Skala przestrzeni searcha"));
        assertTrue(response.body().contains("Ocenieni kandydaci"));
        assertTrue(response.body().contains(">2949<"));
        assertTrue(response.body().contains("Wyniki po normalizacji"));
        assertTrue(response.body().contains(">137<"));
        assertTrue(response.body().contains("duża"));
        assertTrue(response.body().contains("Najlepsze wyniki po normalizacji"));
        assertTrue(response.body().contains("Łączne obrażenia"));
        assertTrue(response.body().contains(">439<"));
        assertTrue(response.body().contains("DPS"));
        assertTrue(response.body().contains("48.7778"));
        assertTrue(response.body().contains("Wejście buildu"));
        assertTrue(response.body().contains("Skille na pasku"));
        assertTrue(response.body().contains("Advance -&gt; Clash"));
        assertTrue(response.body().contains("/znajdz-najlepszy-build/szczegoly"));
        assertTrue(response.body().contains("Pokaż pełną analizę kandydata"));
    }

    @Test
    void shouldAllowDrillDownFromSearchResultToCandidateDetailsOnSameRuntime() throws Exception {
        createHero("Szperacz", "13");
        assignAllFoundationSkills();
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
        assertTrue(detailResponse.body().contains("Aktywny bohater"));
        assertTrue(detailResponse.body().contains("Wejście buildu"));
        assertTrue(detailResponse.body().contains("Skille na pasku"));
        assertTrue(detailResponse.body().contains("Pasek akcji"));
        assertTrue(detailResponse.body().contains("Tryb biblioteki itemów"));
        assertTrue(detailResponse.body().contains("Wybrane itemy z biblioteki"));
        assertTrue(detailResponse.body().contains("Łączny wkład itemów"));
        assertTrue(detailResponse.body().contains("Łączne obrażenia"));
        assertTrue(detailResponse.body().contains("DPS"));
        assertTrue(detailResponse.body().contains("Debug bezpośrednich trafień"));
        assertTrue(detailResponse.body().contains("Debug opóźnionych trafień"));
        assertTrue(detailResponse.body().contains("Debug obrażeń reaktywnych"));
        assertTrue(detailResponse.body().contains("Ślad kroków symulacji"));
        assertTrue(detailResponse.body().contains("Judgement aktywny na końcu"));
        assertTrue(detailResponse.body().contains("Resolve aktywny na końcu"));
        assertTrue(detailResponse.body().contains("Końcowa szansa bloku"));
        assertTrue(detailResponse.body().contains("Końcowy bonus do kolców"));
        assertTrue(detailResponse.body().contains("Advance"));
        assertTrue(detailResponse.body().contains("Clash"));

        CurrentBuildCalculation expectedCalculation = calculateDrillDownExpectedResult(detailFields);
        assertTrue(detailResponse.body().contains(">" + expectedCalculation.getResult().getTotalDamage() + "<"));
        assertTrue(detailResponse.body().contains(String.format(Locale.US, "%.4f", expectedCalculation.getResult().getDps())));
    }

    @Test
    void shouldRenderSelectedLibraryItemsAndReuseSameCombinationInDrillDown() throws Exception {
        createHero("Szperacz", "13");
        Map<String, String> fields = buildReferenceSearchFields();
        fields.put("useItemLibrary", "true");
        fields.put("weaponDamageValues", "0");
        fields.put("strengthValues", "18");
        fields.put("intelligenceValues", "0");
        fields.put("thornsValues", "50");
        fields.put("blockChanceValues", "50");
        fields.put("retributionChanceValues", "50");
        fields.put("actionBarSizes", "1");
        fields.put(SearchBuildFormData.rankValuesFieldName(krys.skill.SkillId.BRANDISH), "0");
        fields.put(SearchBuildFormData.rankValuesFieldName(krys.skill.SkillId.HOLY_BOLT), "0");
        fields.put(SearchBuildFormData.rankValuesFieldName(krys.skill.SkillId.CLASH), "0");
        fields.put(SearchBuildFormData.rankValuesFieldName(krys.skill.SkillId.ADVANCE), "5");
        fields.put(SearchBuildFormData.baseUpgradeValuesFieldName(krys.skill.SkillId.ADVANCE), "true");
        fields.put(SearchBuildFormData.choiceValuesFieldName(krys.skill.SkillId.ADVANCE), "LEFT");

        HttpResponse<String> searchResponse = sendPost("/znajdz-najlepszy-build", fields);

        assertEquals(200, searchResponse.statusCode());
        assertTrue(searchResponse.body().contains("Tryb biblioteki itemów"));
        assertTrue(searchResponse.body().contains("Włączony"));
        assertTrue(searchResponse.body().contains("Wybrane itemy z biblioteki"));
        assertTrue(searchResponse.body().contains("Broń"));
        assertTrue(searchResponse.body().contains("Tarcza"));
        assertTrue(searchResponse.body().contains("Łączny wkład itemów"));
        assertTrue(searchResponse.body().contains("obrażenia broni=321"));

        Map<String, String> detailFields = extractFirstDetailsFormFields(searchResponse.body());
        HttpResponse<String> detailResponse = sendPost("/znajdz-najlepszy-build/szczegoly", detailFields);

        assertEquals(200, detailResponse.statusCode());
        assertTrue(detailResponse.body().contains("Tryb biblioteki itemów"));
        assertTrue(detailResponse.body().contains("Włączony"));
        assertTrue(detailResponse.body().contains("Wybrane itemy z biblioteki"));
        assertTrue(detailResponse.body().contains("Broń"));
        assertTrue(detailResponse.body().contains("Tarcza"));
        assertTrue(detailResponse.body().contains("obrażenia broni=321"));
        assertTrue(detailResponse.body().contains("inteligencja=11"));

        CurrentBuildCalculation expectedCalculation = calculateDrillDownExpectedResult(detailFields);
        assertTrue(detailResponse.body().contains(">" + expectedCalculation.getResult().getTotalDamage() + "<"));
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

    private void assignAllFoundationSkills() throws Exception {
        assignSkill(krys.skill.SkillId.BRANDISH);
        assignSkill(krys.skill.SkillId.HOLY_BOLT);
        assignSkill(krys.skill.SkillId.CLASH);
    }

    private void assignSkill(krys.skill.SkillId skillId) throws Exception {
        HttpResponse<String> response = sendPost("/policz-aktualny-build", Map.of(
                "heroAction", "addAssignedSkill",
                "skillIdToAdd", skillId.name()
        ));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Dodano umiejętność " + krys.skill.PaladinSkillDefs.get(skillId).getName() + " do bohatera."));
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

    private static void seedItemLibrary(Path tempDirectory) {
        ItemLibraryService itemLibraryService = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        itemLibraryService.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "weapon-a.png",
                EquipmentSlot.MAIN_HAND,
                300L,
                55.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        ));
        itemLibraryService.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "weapon-b.png",
                EquipmentSlot.MAIN_HAND,
                321L,
                60.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        ));
        itemLibraryService.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "shield-a.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                11.0d,
                90.0d,
                18.0d,
                25.0d
        ));
    }
}
