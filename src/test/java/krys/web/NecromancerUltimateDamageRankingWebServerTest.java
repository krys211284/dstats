package krys.web;

import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class NecromancerUltimateDamageRankingWebServerTest {
    private static final Set<String> ULTIMATE_IDS = Set.of("armia_umarlych", "fala_krwi", "nawalnica_kosci", "spetanie_dusz");
    private CurrentBuildWebServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        server = new CurrentBuildWebServer(0, Files.createTempDirectory("necromancer-ultimate-ranking"));
        server.start();
        client = HttpClient.newHttpClient();
        baseUrl = "http://127.0.0.1:" + server.getPort();
    }
    @AfterEach
    void tearDown() { client.close(); server.close(); }

    @Test
    void shouldExposeCompleteCatalogAndFourUltimatesThroughAllThreeFilters() throws Exception {
        String page = get("");
        assertEquals(23, ids(page).size());
        for (var entry : Map.of("basic", 4, "core", 6, "corpse", 3, "macabre", 4, "curse", 2, "ultimate", 4).entrySet())
            assertEquals(entry.getValue().longValue(), Pattern.compile("data-skill-group=\"" + entry.getKey() + "\"").matcher(page).results().count());
        assertTrue(page.contains("<option value=\"ultimate\">Moce Specjalne / Ultimate</option>"));
        assertTrue(page.contains("<option value=\"ULTIMATE\">Moce Specjalne</option>"));
        for (String filter : List.of("&skillGroup=ultimate", "&sourceCategory=ULTIMATE", "&tag=ULTIMATE"))
            assertEquals(ULTIMATE_IDS, Set.copyOf(ids(get(filter))));
        assertEquals(ULTIMATE_IDS, Set.copyOf(ids(get("&q=" + encode("Moce Specjalne")))));
    }

    @Test
    void shouldRenderBasePercentDurationCooldownLuckyHitAndResourcePerEvent() throws Exception {
        String page = get("&skillGroup=ultimate");
        var army = cells(row(page, "armia_umarlych"));
        assertEquals("120%", text(army.get(2)));
        assertEquals("306%", text(army.get(3)));
        assertTrue(army.getFirst().contains("per explosion; liczba eksplozji nie jest stała"));
        assertEquals("64%", text(army.get(6)));
        assertEquals("60 s", text(army.get(14)));
        var wave = cells(row(page, "fala_krwi"));
        assertEquals("500%", text(wave.get(2)));
        assertEquals("1275%", text(wave.get(3)));
        assertEquals("50%", text(wave.get(6)));
        assertEquals("50 s", text(wave.get(14)));
        var storm = cells(row(page, "nawalnica_kosci"));
        assertTrue(storm.get(2).contains("DAMAGE_OVER_DURATION</code>: 400% w ciągu 10 s"));
        assertTrue(storm.get(3).contains("DAMAGE_OVER_DURATION</code>: 1020% w ciągu 10 s"));
        assertEquals("40%", text(storm.get(6)));
        assertEquals("60 s", text(storm.get(14)));
        var soul = cells(row(page, "spetanie_dusz"));
        assertTrue(soul.get(2).contains("DAMAGE_OVER_DURATION</code>: 350% w ciągu 8 s"));
        assertTrue(soul.get(3).contains("DAMAGE_OVER_DURATION</code>: 893% w ciągu 8 s"));
        assertEquals("20%", text(soul.get(6)));
        assertEquals("40 s", text(soul.get(14)));
        assertEquals("2 / zdarzenie", text(soul.get(5)));
        assertFalse(soul.get(5).contains("/ sek."));
        for (var id : ULTIMATE_IDS) {
            var sourceCells = cells(row(page, id));
            assertEquals(16, sourceCells.size());
            assertEquals("-", text(sourceCells.get(4)));
            assertEquals("-", text(sourceCells.get(15)));
            if (!id.equals("spetanie_dusz")) assertEquals("-", text(sourceCells.get(5)));
        }
        assertFalse(storm.get(2).contains("/ sek."));
        assertFalse(soul.get(2).contains("/ sek."));
        assertFalse(page.contains("<th>theoreticalDps</th>"));
    }

    @Test
    void shouldFindAllUltimateNamesVariantsAndAbsorptionSourceTextThroughSharedSearch() throws Exception {
        var queries = Map.of("armia_umarlych", List.of("Armia Umarłych", "Martwe Zimno", "per explosion"),
                "fala_krwi", List.of("Fala Krwi", "Hematolagnia", "Krwawy Przypływ"),
                "nawalnica_kosci", List.of("Nawałnica Kości", "Wygłodniały Cyklon", "Rzucone Kości"),
                "spetanie_dusz", List.of("Spętanie Dusz", "Destylacja Duszy", "0.25 s", "zdarzenie", "pochłonięcie jednej duszy przeciwnika"));
        for (var entry : queries.entrySet())
            for (var query : entry.getValue())
                assertEquals(List.of(entry.getKey()), ids(get("&skillGroup=ultimate&q=" + encode(query))), query);
    }

    @Test
    void shouldRenderSeparateVariantTablesAndOverridesWithoutChangingBaseValues() throws Exception {
        String page = get("&skillGroup=ultimate");
        String army = row(page, "armia_umarlych");
        assertTrue(army.contains("PRIMARY_DAMAGE; R1 150%; max-view 383%"));
        assertTrue(cells(army).get(13).contains("Niezłomne Dowództwo"));
        assertTrue(army.contains("Brak potwierdzonego max-view / rank15"));
        assertTrue(army.contains("R1 +30%[x]"));
        assertEquals(List.of("armia_umarlych"), ids(get("&skillGroup=ultimate&hasManualReviewUpgrade=YES")));
        String wave = row(page, "fala_krwi");
        assertTrue(wave.contains("ADDITIONAL_STRIKE_DAMAGE; R1 500%; max-view 1275%"));
        assertTrue(wave.contains("PRIMARY_DAMAGE; R1 800%; max-view 2040%"));
        assertTrue(wave.contains("PRIMARY_DAMAGE; R1 300%; max-view 765%"));
        assertTrue(wave.contains("koszt 70 Esencji"));
        assertTrue(wave.contains("no cooldown"));
        assertTrue(wave.contains("no Knock Back"));
        String storm = row(page, "nawalnica_kosci");
        assertTrue(storm.contains("DAMAGE_OVER_DURATION; R1 40% w ciągu 2 s; max-view 102% w ciągu 2 s"));
        assertTrue(storm.contains("Do 4 Zwłok; +2 s za każde"));
        String soul = row(page, "spetanie_dusz");
        assertTrue(soul.contains("EVENT_TRIGGERED_DAMAGE; R1 25% na zdarzenie; max-view 64% na zdarzenie"));
        assertTrue(soul.contains("BURST_DAMAGE; R1 75%; max-view 191%"));
        assertTrue(soul.contains("Frozen"));
        assertFalse(cells(soul).get(2).contains("EVENT_TRIGGERED_DAMAGE"));
        assertFalse(cells(storm).get(2).contains("40% w ciągu 2 s"));
    }

    @Test
    void shouldRecognizeAndSortEventGenerationBySourceAmountTwo() throws Exception {
        // Istniejący facet obejmuje także RESOURCE_OR_COST ulepszeń (Hematolagnia i zużycie Zwłok).
        assertEquals(Set.of("fala_krwi", "nawalnica_kosci", "spetanie_dusz"),
                Set.copyOf(ids(get("&skillGroup=ultimate&hasResourceGeneration=YES"))));
        assertEquals(Set.of("armia_umarlych"),
                Set.copyOf(ids(get("&skillGroup=ultimate&hasResourceGeneration=NO"))));
        for (String sort : List.of("resourceGeneratedBase", "resourceGeneratedMaxKnown")) {
            var ascending = ids(get("&hasResourceGeneration=YES&sort=" + sort + "&direction=asc"));
            assertEquals("spetanie_dusz", ascending.getFirst());
            var generatingIds = Set.of("kosciane_odlamki", "zniwa", "rozklad", "krwotok", "spetanie_dusz");
            // Brak liczby źródłowej nadal sortuje się na końcu, niezależnie od kierunku.
            var descending = ids(get("&hasResourceGeneration=YES&sort=" + sort + "&direction=desc"))
                    .stream().filter(generatingIds::contains).toList();
            assertEquals("spetanie_dusz", descending.getLast());
        }
    }

    private String get(String parameters) throws Exception {
        String path = "/ranking-obrazen?character=necromancer" + parameters;
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), path);
        return response.body();
    }
    private static String text(String html) { return html.replaceAll("<[^>]+>", ""); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static List<String> ids(String page) {
        return Pattern.compile("data-skill-id=\"([^\"]+)\"").matcher(page).results().map(match -> match.group(1)).toList();
    }
    private static List<String> cells(String row) {
        return Pattern.compile("<td>([\\s\\S]*?)</td>").matcher(row).results().map(match -> match.group(1)).toList();
    }
    private static String row(String page, String id) {
        return Pattern.compile("<tr[^>]*data-skill-id=\"" + Pattern.quote(id) + "\"[^>]*>[\\s\\S]*?</tr>").matcher(page).results().findFirst().orElseThrow().group();
    }
}
