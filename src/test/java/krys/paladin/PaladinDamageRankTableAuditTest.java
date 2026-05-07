package krys.paladin;

import krys.combat.DamageEngine;
import krys.ranking.DamageRankingService;
import krys.ranking.PaladinSkillDamageRankingEntry;
import krys.ranking.PlayableClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaladinDamageRankTableAuditTest {
    private static final Path AUDIT_PATH = Path.of("docs/paladin/source-md/paladin_damage_rank_table_audit.md");
    private static final List<String> REQUIRED_COLUMNS = List.of(
            "skillId",
            "polishName",
            "englishName",
            "sourceHtml",
            "classification",
            "detectedComponents",
            "rank1Summary",
            "rank15Summary",
            "recommendedModel",
            "notes"
    );

    @Test
    void audyt_tabel_rang_powinien_istniec_i_zawierac_24_skille_z_wymaganymi_kolumnami() throws IOException {
        String audit = Files.readString(AUDIT_PATH, StandardCharsets.UTF_8);
        List<Map<String, String>> rows = auditRows(audit);

        assertEquals(24, rows.size());
        for (String column : REQUIRED_COLUMNS) {
            assertTrue(audit.contains(column), column);
        }
        assertEquals(Set.of(
                "wymach",
                "swiety_pocisk",
                "starcie",
                "natarcie",
                "blogoslawiona_tarcza",
                "blogoslawiony_mlot",
                "boska_lanca",
                "uderzenie_tarcza",
                "zapal",
                "aura_fanatyzmu",
                "aura_smialosci",
                "aura_swietej_swiatlosci",
                "szarza_z_tarcza",
                "egida",
                "spadajaca_gwiazda",
                "mobilizacja",
                "skazanie",
                "wlocznia_niebios",
                "konsekracja",
                "oczyszczenie",
                "furia_niebios",
                "forteca",
                "zenit",
                "arbiter_sprawiedliwosci"
        ), rows.stream().map(row -> value(row, "skillId")).collect(Collectors.toSet()));
    }

    @Test
    void audyt_powinien_klasyfikowac_reprezentatywne_skille_zgodnie_z_ryzykiem_importu() throws IOException {
        Map<String, Map<String, String>> rowsBySkillId = auditRows(Files.readString(AUDIT_PATH, StandardCharsets.UTF_8)).stream()
                .collect(Collectors.toMap(row -> value(row, "skillId"), row -> row));

        assertClassification(rowsBySkillId, "blogoslawiony_mlot", "SIMPLE_SINGLE_COMPONENT", "DamagePercentRankTable");
        assertClassification(rowsBySkillId, "zapal", "MULTI_COMPONENT", "DamagePercentComponentRankTable");
        assertClassification(rowsBySkillId, "spadajaca_gwiazda", "MULTI_COMPONENT", "DamagePercentComponentRankTable");

        assertTrue(value(rowsBySkillId.get("blogoslawiony_mlot"), "notes").contains("1=115"));
        assertTrue(value(rowsBySkillId.get("zapal"), "detectedComponents").contains("ADDITIONAL_STRIKE_DAMAGE"));
        assertTrue(value(rowsBySkillId.get("spadajaca_gwiazda"), "detectedComponents").contains("JUMP_DAMAGE"));
        assertTrue(value(rowsBySkillId.get("spadajaca_gwiazda"), "detectedComponents").contains("LANDING_DAMAGE"));
    }

    @Test
    void audyt_nie_powinien_zmieniac_tabel_rang_w_rejestrze_paladyna() {
        Set<String> skillsWithRankTables = PaladinSkillTreeRegistry.allSkills().stream()
                .filter(skill -> !skill.getBaseDamagePercentRanks().isEmpty())
                .map(PaladinTreeSkill::getSkillId)
                .collect(Collectors.toSet());

        assertEquals(Set.of("blogoslawiony_mlot"), skillsWithRankTables);
        assertEquals(15, PaladinSkillTreeRegistry.requireSkill("blogoslawiony_mlot").getBaseDamagePercentRanks().asMap().size());
        assertEquals(115, PaladinSkillTreeRegistry.requireSkill("blogoslawiony_mlot").damagePercentAtRank1());
        assertEquals(293, PaladinSkillTreeRegistry.requireSkill("blogoslawiony_mlot").damagePercentAtTreeMaxRank(15));
    }

    @Test
    void runtime_dps_powinien_pozostac_zablokowany_dla_opisowego_rejestru_paladyna() {
        DamageRankingService service = new DamageRankingService(new DamageEngine());
        List<PaladinSkillDamageRankingEntry> entries = service.describeTreeSkills(PlayableClass.PALADIN);

        assertEquals(24, entries.size());
        assertTrue(entries.stream().allMatch(entry -> entry.getDamagePerUse() == null));
        assertTrue(entries.stream().allMatch(entry -> entry.getTheoreticalDps() == null));
    }

    @Test
    void pdf_mocy_specjalnych_powinien_pozostac_bez_zmian_po_audycie() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(PaladinSkillTreeRegistry.SPECIAL_POWERS_PDF));
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);

        assertEquals("a559c9ddd65c0a64d31a5efbec2baae4a6db6aaa466060665736f580b0adefc0",
                HexFormat.of().formatHex(digest));
    }

    private static void assertClassification(Map<String, Map<String, String>> rowsBySkillId,
                                             String skillId,
                                             String classification,
                                             String recommendedModel) {
        Map<String, String> row = rowsBySkillId.get(skillId);
        assertNotNull(row, skillId);
        assertEquals(classification, value(row, "classification"));
        assertEquals(recommendedModel, value(row, "recommendedModel"));
    }

    private static List<Map<String, String>> auditRows(String audit) {
        List<String> headers = Arrays.stream(audit.lines()
                        .filter(line -> line.startsWith("| skillId |"))
                        .findFirst()
                        .orElseThrow()
                        .split("\\|"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        return audit.lines()
                .filter(line -> line.startsWith("| `"))
                .map(line -> parseRow(headers, line))
                .toList();
    }

    private static Map<String, String> parseRow(List<String> headers, String line) {
        List<String> values = Arrays.stream(line.split("\\|", -1))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        if (values.size() != headers.size()) {
            throw new AssertionError("Niepoprawna liczba kolumn audytu: " + line);
        }
        return java.util.stream.IntStream.range(0, headers.size())
                .boxed()
                .collect(Collectors.toMap(headers::get, index -> stripCode(values.get(index))));
    }

    private static String value(Map<String, String> row, String key) {
        return row.get(key);
    }

    private static String stripCode(String value) {
        if (value.startsWith("`") && value.endsWith("`")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
