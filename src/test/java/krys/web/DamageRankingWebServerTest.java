package krys.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRankingWebServerTest {
    private static final Map<String, List<Integer>> SIMPLE_SINGLE_COMPONENT_R1_TREE_MAX = Map.ofEntries(
            Map.entry("wymach", List.of(75, 191)),
            Map.entry("swiety_pocisk", List.of(90, 229)),
            Map.entry("starcie", List.of(115, 293)),
            Map.entry("natarcie", List.of(105, 268)),
            Map.entry("blogoslawiona_tarcza", List.of(205, 523)),
            Map.entry("blogoslawiony_mlot", List.of(115, 293)),
            Map.entry("boska_lanca", List.of(90, 229)),
            Map.entry("uderzenie_tarcza", List.of(205, 523)),
            Map.entry("szarza_z_tarcza", List.of(90, 229)),
            Map.entry("skazanie", List.of(240, 612)),
            Map.entry("konsekracja", List.of(75, 191))
    );
    private static final Set<String> NON_DAMAGE_DISPLAY_SKILLS = Set.of(
            "aura_fanatyzmu",
            "aura_smialosci",
            "egida",
            "mobilizacja",
            "oczyszczenie",
            "forteca"
    );
    private static final Set<String> MANUAL_REVIEW_DISPLAY_SKILLS = Set.of(
            "furia_niebios",
            "arbiter_sprawiedliwosci"
    );

    private CurrentBuildWebServer webServer;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        Path tempDirectory = Files.createTempDirectory("paladin-ranking-web");
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
    void shouldRenderAllTwentyFourPaladinTreeSkillsByDefault() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Ranking obrażeń"));
        assertTrue(response.body().contains("<option value=\"paladin\" selected>Paladyn</option>"));
        assertTrue(response.body().contains("Wpisy w rejestrze"));
        assertTrue(response.body().contains(">24<"));
        assertEquals(24, countSkillRows(response.body()));
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
        assertTrue(response.body().contains("data-skill-id=\"forteca\""));
        assertTrue(response.body().contains("data-skill-id=\"arbiter_sprawiedliwosci\""));
    }

    @Test
    void shouldRenderPaladinRankingForExplicitCharacterParameter() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("PaladinSkillTreeRegistry"));
        assertTrue(response.body().contains("<main class=\"layout wide-page ranking-page\">"));
        assertTrue(response.body().contains(".wide-page.ranking-page"));
        assertTrue(response.body().contains("width: calc(100vw - 32px)"));
        assertTrue(response.body().contains("max-width: none"));
        assertTrue(response.body().contains("overflow-x: auto"));
        assertTrue(response.body().contains(">skillName<"));
        assertTrue(response.body().contains(">Kategoria<"));
        assertTrue(response.body().contains(">tags<"));
        assertTrue(response.body().contains(">type<"));
        assertTrue(response.body().contains(">Obrażenia % R1<"));
        assertTrue(response.body().contains(">Obrażenia % max drzewo<"));
        assertTrue(response.body().contains(">Dmg multiplier<"));
        assertTrue(response.body().contains(">Dmg bonus<"));
        assertTrue(response.body().contains(">Extra hit / component<"));
        assertTrue(response.body().contains(">Damage over time<"));
        assertTrue(response.body().contains(">Status / debuff<"));
        assertTrue(response.body().contains(">Resource<"));
        assertTrue(response.body().contains(">Speed / cooldown<"));
        assertTrue(response.body().contains(">Defense / utility<"));
        assertTrue(response.body().contains(">Manual review<"));
        assertFalse(response.body().contains(">Direct dmg upgrade<"));
        assertFalse(response.body().contains(">New dmg component<"));
        assertFalse(response.body().contains("<th>grupa_1: wpływ na obrażenia</th>"));
        assertFalse(response.body().contains("<th>grupa_2: wpływ na obrażenia</th>"));
        assertFalse(response.body().contains("<th>grupa_3: wpływ na obrażenia</th>"));
        assertFalse(response.body().contains("<th>skillId</th>"));
        assertFalse(response.body().contains("<th>verificationStatus</th>"));
        assertFalse(response.body().contains("<th>Komponenty obrażeń</th>"));
        assertFalse(response.body().contains("<th>Ulepszenia wpływające na obrażenia</th>"));
        assertFalse(response.body().contains("<th>Ulepszenia bez wpływu na obrażenia</th>"));
        assertFalse(response.body().contains("<th>damagePerUse</th>"));
        assertFalse(response.body().contains("<th>theoreticalDps</th>"));
        assertFalse(response.body().contains("<th>singleTargetDps</th>"));
        assertFalse(response.body().contains("<th>reason / notes</th>"));
        assertFalse(response.body().contains("<th>sourcePdf</th>"));
        assertFalse(response.body().contains("Metryka rankingu"));
        assertFalse(response.body().contains("DAMAGE_PER_USE"));
        assertFalse(response.body().contains("THEORETICAL_DPS"));
        assertFalse(response.body().contains("SINGLE_TARGET_DPS"));
        assertFalse(response.body().contains("brak wpływu na obrażenia"));
        assertTrue(response.body().contains("name=\"tag\""));
        assertTrue(response.body().contains("name=\"hasDirectUpgradeDamage\""));
        assertTrue(response.body().contains("name=\"hasNewDamageComponent\""));
        assertTrue(response.body().contains("name=\"hasStatusDamageEnabler\""));
        assertTrue(response.body().contains("name=\"hasResourceGeneration\""));
        assertTrue(response.body().contains("name=\"hasCooldownOrCastSpeed\""));
        assertTrue(response.body().contains("name=\"hasDefenseOrUtility\""));
        assertTrue(response.body().contains("name=\"hasManualReviewUpgrade\""));
        assertTrue(response.body().contains("aria-sort=\"descending\""));
        assertTrue(response.body().contains("sort=skillName"));
        assertTrue(response.body().contains("sort=baseDamageTreeMax"));
        assertTrue(response.body().contains("sort=maxDamageMultiplierPercent"));
        assertTrue(response.body().contains("sort=maxExtraHitOrComponentPercent"));
        assertTrue(response.body().contains("Dmg multiplier = mnożnik"));
        assertTrue(response.body().contains("Extra hit / component = osobny hit lub komponent"));
        assertEquals(24, countSkillRows(response.body()));
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
        assertTrue(response.body().contains("data-verification-status=\"NEEDS_VERIFICATION\""));
        assertTrue(response.body().contains("title=\"Status weryfikacji: NEEDS_VERIFICATION\""));
        assertTrue(response.body().contains("verification-row verification-needs-verification"));
        assertTrue(response.body().contains("verification-supported"));
        assertTrue(response.body().contains("verification-non-damage"));
        assertTrue(response.body().contains("verification-unsupported"));
        assertTrue(response.body().contains("data-skill-id=\"zenit\""));
        assertTrue(response.body().contains("data-skill-id=\"forteca\""));
    }

    @Test
    void baseDamagePercentColumnsShouldRenderSimpleValuesComponentsAndReviewStates() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin");

        assertEquals(200, response.statusCode());
        assertEquals(24, countSkillRows(response.body()));
        assertTrue(response.body().contains("data-skill-id=\"blogoslawiony_mlot\""));
        assertTrue(response.body().contains(">115%</td>"));
        assertTrue(response.body().contains(">293%</td>"));
        assertTrue(response.body().contains("data-skill-id=\"wymach\""));
        assertTrue(response.body().contains(">75%</td>"));
        assertTrue(response.body().contains(">191%</td>"));
        assertTrue(response.body().contains("data-skill-id=\"skazanie\""));
        assertTrue(response.body().contains(">240%</td>"));
        assertTrue(response.body().contains(">612%</td>"));
        assertTrue(response.body().contains("data-skill-id=\"blogoslawiona_tarcza\""));
        assertTrue(response.body().contains(">205%</td>"));
        assertTrue(response.body().contains(">523%</td>"));
        assertBaseDamageCells(response.body());
        assertFalse(response.body().contains(">0%</td>"));
        assertFalse(response.body().contains("suma komponentów"));
        assertFalse(response.body().contains("total"));
        assertFalse(response.body().contains("razem"));
    }

    @Test
    void multiComponentSkillsShouldRenderComponentPercentsWithoutFlattening() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin");

        assertEquals(200, response.statusCode());
        assertRowContains(response.body(), "zapal",
                "PRIMARY_DAMAGE", "80%", "204%", "ADDITIONAL_STRIKE_DAMAGE", "20%", "51%");
        assertRowContains(response.body(), "aura_swietej_swiatlosci",
                "PASSIVE_DAMAGE", "45%", "115%", "ACTIVE_DAMAGE", "320%", "816%");
        assertRowContains(response.body(), "spadajaca_gwiazda",
                "LANDING_DAMAGE", "80%", "612%");
        assertRowDoesNotContain(response.body(), "spadajaca_gwiazda", "JUMP_DAMAGE");
        assertRowContains(response.body(), "wlocznia_niebios", "PRIMARY_DAMAGE", "BURST_DAMAGE");
        assertRowContains(response.body(), "zenit", "FIRST_STRIKE_DAMAGE", "SECOND_STRIKE_DAMAGE");
        assertFalse(response.body().contains("suma komponentów"));
        assertFalse(response.body().contains("total"));
        assertFalse(response.body().contains("razem"));
    }

    @Test
    void brandishUpgradeGroupsShouldRenderConfirmedModifierValuesWithoutSumming() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin");
        String brandishRow = rowHtml(response.body(), "wymach");

        assertEquals(200, response.statusCode());
        assertTrue(brandishRow.contains("Zwiększenie Obrażeń"));
        assertTrue(brandishRow.contains("20%[X]"));
        assertTrue(brandishRow.contains("Szybkość Użycia"));
        assertTrue(brandishRow.contains("20%[+]"));
        assertTrue(brandishRow.contains("Odsłonięcie"));
        assertTrue(brandishRow.contains("status / pośredni wpływ"));
        assertTrue(brandishRow.contains("Generowanie Wiary"));
        assertTrue(brandishRow.contains("zasób"));
        assertTrue(brandishRow.contains("Powracająca Światłość"));
        assertTrue(brandishRow.contains("52%"));
        assertTrue(brandishRow.contains("Miecz Mistrzostwa"));
        assertTrue(brandishRow.contains("128%"));
        assertTrue(brandishRow.contains("Krzyżowe Uderzenie"));
        assertTrue(brandishRow.contains("120%"));
        assertTrue(brandishRow.contains(">Adept</td>"));
        List<String> cells = tableCells(brandishRow);
        assertTrue(cells.get(6).contains("20%[X]"));
        assertFalse(cells.get(6).contains("128%"));
        assertTrue(cells.get(8).contains("52%"));
        assertTrue(cells.get(8).contains("120%"));
        assertTrue(cells.get(8).contains("128%"));
        assertFalse(cells.get(8).contains("20%[X]"));
        assertTrue(cells.get(10).contains("Odsłonięcie"));
        assertFalse(cells.get(10).contains("20%[X]"));
        assertTrue(cells.get(11).contains("Generowanie Wiary"));
        assertTrue(cells.get(12).contains("Szybkość Użycia"));
        assertTrue(cells.get(12).contains("20%[+]"));
        assertFalse(cells.get(6).contains("20%[+]"));
        assertTrue(cells.get(6).contains("20%[X]</span> <span class=\"facet-name\">&mdash; Zwiększenie Obrażeń"));
        assertTrue(cells.get(8).contains("52%</span> <span class=\"facet-name\">&mdash; Powracająca Światłość"));
        assertTrue(cells.get(8).contains("120%</span> <span class=\"facet-name\">&mdash; Krzyżowe Uderzenie"));
        assertTrue(cells.get(8).contains("128%</span> <span class=\"facet-name\">&mdash; Miecz Mistrzostwa"));
        assertFalse(brandishRow.contains("suma"));
        assertFalse(brandishRow.contains("razem"));
        assertFalse(brandishRow.contains("total"));
        assertFalse(brandishRow.contains("DPS"));
        assertFalse(brandishRow.contains("brak wpływu na obrażenia"));
    }

    @Test
    void nonDamageAndManualReviewSkillsShouldRenderExplicitStatesInBaseColumns() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin");

        assertEquals(200, response.statusCode());
        for (String skillId : NON_DAMAGE_DISPLAY_SKILLS) {
            assertRowContains(response.body(), skillId, "nie dotyczy");
        }
        for (String skillId : MANUAL_REVIEW_DISPLAY_SKILLS) {
            assertRowContains(response.body(), skillId, "wymaga weryfikacji");
        }
    }

    @Test
    void legacyPaladinRankingEndpointShouldRenderTheGenericPaladinRankingAlias() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen-paladyna");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Ranking obrażeń"));
        assertTrue(response.body().contains("<form class=\"ranking-filters\" method=\"get\" action=\"/ranking-obrazen\">"));
        assertEquals(24, countSkillRows(response.body()));
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
    }

    @Test
    void shouldNotExposeLegacyFoundationSkillsAsDefaultPaladinTree() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin");

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("data-skill-id=\"BRANDISH\""));
        assertFalse(response.body().contains("data-skill-id=\"HOLY_BOLT\""));
        assertFalse(response.body().contains("data-skill-id=\"CLASH\""));
        assertFalse(response.body().contains("data-skill-id=\"ADVANCE\""));
        assertFalse(response.body().contains(">Brandish<"));
        assertFalse(response.body().contains(">Holy Bolt<"));
        assertFalse(response.body().contains(">Clash<"));
        assertFalse(response.body().contains(">Advance<"));
    }

    @Test
    void needsVerificationSkillsShouldNotHaveCalculatedDpsValues() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin&verificationStatus=NEEDS_VERIFICATION");

        assertEquals(200, response.statusCode());
        assertTrue(countSkillRows(response.body()) > 0);
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
        assertFalse(response.body().contains("data-verification-status=\"NON_DAMAGE\""));
        assertFalse(response.body().contains("data-verification-status=\"SUPPORTED\""));
        assertFalse(response.body().contains("<th>damagePerUse</th>"));
        assertFalse(response.body().contains("<th>theoreticalDps</th>"));
        assertFalse(response.body().contains("<th>singleTargetDps</th>"));
        assertFalse(response.body().contains("zablokowane"));
        assertTrue(response.body().contains("wymaga weryfikacji"));
    }

    @Test
    void nonDamageSkillsShouldNotBeTreatedAsDamageSkills() throws Exception {
        HttpResponse<String> nonDamageResponse = sendGet("/ranking-obrazen?character=paladin&verificationStatus=NON_DAMAGE");

        assertEquals(200, nonDamageResponse.statusCode());
        assertEquals(2, countSkillRows(nonDamageResponse.body()));
        assertTrue(nonDamageResponse.body().contains("data-skill-id=\"aura_fanatyzmu\""));
        assertTrue(nonDamageResponse.body().contains("data-skill-id=\"mobilizacja\""));
        assertFalse(nonDamageResponse.body().contains("data-skill-type=\"DAMAGE\""));

        HttpResponse<String> damageTypeResponse = sendGet("/ranking-obrazen?character=paladin&type=DAMAGE");
        assertEquals(200, damageTypeResponse.statusCode());
        assertFalse(damageTypeResponse.body().contains("data-skill-id=\"aura_fanatyzmu\""));
        assertFalse(damageTypeResponse.body().contains("data-skill-id=\"mobilizacja\""));
    }

    @Test
    void shouldFilterBySkillGroupStatusAndType() throws Exception {
        HttpResponse<String> response = sendGet("/ranking-obrazen?character=paladin&skillGroup=moce_specjalne&verificationStatus=NEEDS_VERIFICATION&type=DAMAGE");

        assertEquals(200, response.statusCode());
        assertEquals(2, countSkillRows(response.body()));
        assertTrue(response.body().contains("data-skill-id=\"furia_niebios\""));
        assertTrue(response.body().contains("data-skill-id=\"arbiter_sprawiedliwosci\""));
        assertFalse(response.body().contains("data-skill-id=\"forteca\""));
        assertFalse(response.body().contains("Metryka rankingu"));
    }

    @Test
    void sortableHeadersShouldSortPaladinTreeWithoutMetricFilter() throws Exception {
        HttpResponse<String> defaultResponse = sendGet("/ranking-obrazen?character=paladin");
        HttpResponse<String> skillNameAscResponse = sendGet("/ranking-obrazen?character=paladin&sort=skillName&direction=asc");
        HttpResponse<String> skillCategoryAscResponse = sendGet("/ranking-obrazen?character=paladin&sort=skillCategory&direction=asc");
        HttpResponse<String> multiplierDescResponse = sendGet("/ranking-obrazen?character=paladin&sort=maxDamageMultiplierPercent&direction=desc");
        HttpResponse<String> extraComponentDescResponse = sendGet("/ranking-obrazen?character=paladin&sort=maxExtraHitOrComponentPercent&direction=desc");

        assertEquals(200, defaultResponse.statusCode());
        assertEquals(24, countSkillRows(defaultResponse.body()));
        assertEquals("skazanie", firstSkillId(defaultResponse.body()));
        assertTrue(defaultResponse.body().contains("aria-sort=\"descending\""));
        assertRowOrder(defaultResponse.body(), "skazanie", "blogoslawiony_mlot");

        assertEquals(200, skillNameAscResponse.statusCode());
        assertEquals("arbiter_sprawiedliwosci", firstSkillId(skillNameAscResponse.body()));
        assertTrue(skillNameAscResponse.body().contains("sort=skillName"));
        assertTrue(skillNameAscResponse.body().contains("aria-sort=\"ascending\""));

        assertEquals(200, skillCategoryAscResponse.statusCode());
        assertEquals("Adept", firstSkillCategory(skillCategoryAscResponse.body()));
        assertTrue(skillCategoryAscResponse.body().contains("sort=skillCategory"));
        assertFalse(skillCategoryAscResponse.body().contains("Metryka rankingu"));

        assertEquals(200, multiplierDescResponse.statusCode());
        assertEquals("wymach", firstSkillId(multiplierDescResponse.body()));
        assertTrue(multiplierDescResponse.body().contains("sort=maxDamageMultiplierPercent"));
        assertEquals(1, countOccurrences(multiplierDescResponse.body(), "aria-sort=\"descending\""));

        assertEquals(200, extraComponentDescResponse.statusCode());
        assertEquals("wymach", firstSkillId(extraComponentDescResponse.body()));
        assertTrue(extraComponentDescResponse.body().contains("sort=maxExtraHitOrComponentPercent"));
        assertTrue(rowHtml(extraComponentDescResponse.body(), "wymach").contains("128%"));
    }

    @Test
    void facetedFiltersShouldFilterUpgradeAndTagColumns() throws Exception {
        HttpResponse<String> resourceResponse = sendGet("/ranking-obrazen?character=paladin&hasResourceGeneration=YES");
        HttpResponse<String> speedResponse = sendGet("/ranking-obrazen?character=paladin&hasCooldownOrCastSpeed=YES");
        HttpResponse<String> newComponentResponse = sendGet("/ranking-obrazen?character=paladin&hasNewDamageComponent=YES");
        HttpResponse<String> manualResponse = sendGet("/ranking-obrazen?character=paladin&hasManualReviewUpgrade=YES");
        HttpResponse<String> tagResponse = sendGet("/ranking-obrazen?character=paladin&tag=FAITH_GENERATION");

        assertEquals(200, resourceResponse.statusCode());
        assertTrue(resourceResponse.body().contains("data-skill-id=\"wymach\""));
        assertTrue(resourceResponse.body().contains("name=\"hasResourceGeneration\""));

        assertEquals(200, speedResponse.statusCode());
        assertTrue(speedResponse.body().contains("data-skill-id=\"wymach\""));

        assertEquals(200, newComponentResponse.statusCode());
        assertTrue(newComponentResponse.body().contains("data-skill-id=\"wymach\""));

        assertEquals(200, manualResponse.statusCode());
        assertTrue(manualResponse.body().contains("wymaga weryfikacji"));

        assertEquals(200, tagResponse.statusCode());
        assertTrue(tagResponse.body().contains("data-skill-id=\"wymach\""));
        assertTrue(tagResponse.body().contains("FAITH_GENERATION"));
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static int countSkillRows(String html) {
        Matcher matcher = Pattern.compile("data-skill-row=\"true\"").matcher(html);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static void assertBaseDamageCells(String html) {
        Matcher matcher = Pattern.compile("(?s)<tr [^>]*data-skill-row=\"true\"[^>]*data-skill-id=\"([^\"]+)\"[^>]*>(.*?)</tr>")
                .matcher(html);
        int found = 0;
        while (matcher.find()) {
            found++;
            String skillId = matcher.group(1);
            List<String> cells = tableCells(matcher.group(2));
            if (cells.size() != 15) {
                throw new AssertionError("Niepoprawna liczba komórek dla " + skillId + ": " + cells.size());
            }
            String rankOneCell = cells.get(4);
            String treeMaxCell = cells.get(5);
            if (SIMPLE_SINGLE_COMPONENT_R1_TREE_MAX.containsKey(skillId)) {
                List<Integer> expected = SIMPLE_SINGLE_COMPONENT_R1_TREE_MAX.get(skillId);
                if (!rankOneCell.contains(expected.get(0) + "%") || !treeMaxCell.contains(expected.get(1) + "%")) {
                    throw new AssertionError("Niepoprawne procenty dla " + skillId + ": " + rankOneCell + " / " + treeMaxCell);
                }
            } else if (Set.of("zapal", "aura_swietej_swiatlosci", "spadajaca_gwiazda", "wlocznia_niebios", "zenit").contains(skillId)) {
                if (rankOneCell.contains("brak danych") || treeMaxCell.contains("brak danych")) {
                    throw new AssertionError("Skill komponentowy nie powinien pokazywać braku danych: " + skillId);
                }
                if (!rankOneCell.contains("<code>") || !treeMaxCell.contains("<code>")) {
                    throw new AssertionError("Skill komponentowy powinien pokazywać nazwy komponentów: " + skillId);
                }
            } else if (NON_DAMAGE_DISPLAY_SKILLS.contains(skillId)) {
                if (!rankOneCell.contains("nie dotyczy") || !treeMaxCell.contains("nie dotyczy")) {
                    throw new AssertionError("Skill nieobrażeniowy powinien pokazywać 'nie dotyczy': " + skillId);
                }
            } else if (MANUAL_REVIEW_DISPLAY_SKILLS.contains(skillId)) {
                if (!rankOneCell.contains("wymaga weryfikacji") || !treeMaxCell.contains("wymaga weryfikacji")) {
                    throw new AssertionError("Skill manual review powinien pokazywać 'wymaga weryfikacji': " + skillId);
                }
            } else {
                throw new AssertionError("Nieoczekiwany skill w rankingu: " + skillId);
            }
            if (rankOneCell.contains("zablokowane") || treeMaxCell.contains("zablokowane")) {
                throw new AssertionError("Bazowe procenty nie powinny renderować blokady runtime: " + skillId);
            }
        }
        assertEquals(24, found);
    }

    private static void assertRowContains(String html, String skillId, String... fragments) {
        String row = rowHtml(html, skillId);
        for (String fragment : fragments) {
            assertTrue(row.contains(fragment), skillId + " powinien zawierać: " + fragment);
        }
    }

    private static void assertRowDoesNotContain(String html, String skillId, String fragment) {
        assertFalse(rowHtml(html, skillId).contains(fragment), skillId + " nie powinien zawierać: " + fragment);
    }

    private static String rowHtml(String html, String skillId) {
        Matcher matcher = Pattern.compile("(?s)<tr [^>]*data-skill-id=\"" + Pattern.quote(skillId) + "\"[^>]*>(.*?)</tr>")
                .matcher(html);
        if (!matcher.find()) {
            throw new AssertionError("Brak wiersza: " + skillId);
        }
        return matcher.group(1);
    }

    private static List<String> tableCells(String rowHtml) {
        Matcher cellMatcher = Pattern.compile("(?s)<td>(.*?)</td>").matcher(rowHtml);
        java.util.ArrayList<String> cells = new java.util.ArrayList<>();
        while (cellMatcher.find()) {
            cells.add(cellMatcher.group(1));
        }
        return cells;
    }

    private static String firstSkillId(String html) {
        Matcher matcher = Pattern.compile("data-skill-row=\"true\" data-skill-id=\"([^\"]+)\"").matcher(html);
        if (!matcher.find()) {
            throw new AssertionError("Brak wierszy rankingu.");
        }
        return matcher.group(1);
    }

    private static String firstSkillCategory(String html) {
        String row = rowHtml(html, firstSkillId(html));
        List<String> cells = tableCells(row);
        return cells.get(1);
    }

    private static int countOccurrences(String text, String fragment) {
        int count = 0;
        int index = text.indexOf(fragment);
        while (index >= 0) {
            count++;
            index = text.indexOf(fragment, index + fragment.length());
        }
        return count;
    }

    private static void assertRowOrder(String html, String firstSkillId, String secondSkillId) {
        int firstIndex = html.indexOf("data-skill-id=\"" + firstSkillId + "\"");
        int secondIndex = html.indexOf("data-skill-id=\"" + secondSkillId + "\"");
        if (firstIndex < 0 || secondIndex < 0 || firstIndex >= secondIndex) {
            throw new AssertionError(firstSkillId + " powinien być przed " + secondSkillId);
        }
    }
}
