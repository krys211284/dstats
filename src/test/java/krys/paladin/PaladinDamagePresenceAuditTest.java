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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaladinDamagePresenceAuditTest {
    private static final Path AUDIT_PATH = Path.of("docs/paladin/source-md/paladin_damage_presence_audit.md");
    private static final List<String> REQUIRED_COLUMNS = List.of(
            "skillId",
            "polishName",
            "englishName",
            "skillGroup",
            "currentRegistryClassification",
            "baseSkillHasDamage",
            "baseDamageRankTable",
            "componentDamageRankTable",
            "upgradeAddsOrChangesDamage",
            "upgradeDamageGroups",
            "currentMainRankingDisplay",
            "finalDamagePresence",
            "notes"
    );
    private static final Set<String> SIMPLE_SINGLE_COMPONENT_SKILLS = Set.of(
            "wymach",
            "swiety_pocisk",
            "starcie",
            "natarcie",
            "blogoslawiona_tarcza",
            "blogoslawiony_mlot",
            "boska_lanca",
            "uderzenie_tarcza",
            "szarza_z_tarcza",
            "skazanie",
            "konsekracja"
    );
    private static final Set<String> MULTI_COMPONENT_SKILLS = Set.of(
            "zapal",
            "aura_swietej_swiatlosci",
            "spadajaca_gwiazda",
            "wlocznia_niebios",
            "zenit"
    );
    private static final Set<String> NON_DAMAGE_OR_MANUAL_WITHOUT_TABLES = Set.of(
            "aura_fanatyzmu",
            "aura_smialosci",
            "egida",
            "mobilizacja",
            "oczyszczenie",
            "forteca",
            "furia_niebios",
            "arbiter_sprawiedliwosci"
    );

    @Test
    void audyt_obecnosci_obrazen_powinien_istniec_i_zawierac_24_skille_z_wymaganymi_kolumnami() throws IOException {
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
    void audyt_powinien_rozdzielac_obecnosc_obrazen_bazowych_komponentowych_i_wymagajacych_weryfikacji() throws IOException {
        Map<String, Map<String, String>> rowsBySkillId = rowsBySkillId();

        for (String skillId : SIMPLE_SINGLE_COMPONENT_SKILLS) {
            assertEquals("SIMPLE_SINGLE_COMPONENT", value(rowsBySkillId.get(skillId), "currentRegistryClassification"), skillId);
            assertEquals("SIMPLE_FULL_1_TO_15", value(rowsBySkillId.get(skillId), "baseDamageRankTable"), skillId);
            assertEquals("HAS_BASE_DAMAGE", value(rowsBySkillId.get(skillId), "finalDamagePresence"), skillId);
        }

        for (String skillId : MULTI_COMPONENT_SKILLS) {
            assertEquals("MULTI_COMPONENT", value(rowsBySkillId.get(skillId), "currentRegistryClassification"), skillId);
            assertEquals("HAS_COMPONENT_DAMAGE", value(rowsBySkillId.get(skillId), "finalDamagePresence"), skillId);
            assertTrue(!value(rowsBySkillId.get(skillId), "componentDamageRankTable").equals("none"), skillId);
        }

        assertEquals("NEEDS_MANUAL_REVIEW", value(rowsBySkillId.get("furia_niebios"), "finalDamagePresence"));
        assertEquals("NEEDS_MANUAL_REVIEW", value(rowsBySkillId.get("arbiter_sprawiedliwosci"), "finalDamagePresence"));
    }

    @Test
    void non_damage_z_ulepszeniami_obrazeniowymi_nie_powinny_byc_automatycznie_final_non_damage() throws IOException {
        List<Map<String, String>> nonDamageRowsWithDamageUpgrades = auditRows(Files.readString(AUDIT_PATH, StandardCharsets.UTF_8)).stream()
                .filter(row -> value(row, "currentRegistryClassification").equals("NON_DAMAGE"))
                .filter(row -> !value(row, "upgradeAddsOrChangesDamage").equals("NO"))
                .toList();

        assertTrue(!nonDamageRowsWithDamageUpgrades.isEmpty());
        for (Map<String, String> row : nonDamageRowsWithDamageUpgrades) {
            assertTrue(Set.of("HAS_UPGRADE_DAMAGE_ONLY", "NEEDS_MANUAL_REVIEW").contains(value(row, "finalDamagePresence")),
                    value(row, "skillId"));
        }
    }

    @Test
    void audyt_nie_powinien_zmieniac_liczby_prostych_i_komponentowych_tabel_w_rejestrze() {
        Set<String> skillsWithSimpleTables = PaladinSkillTreeRegistry.allSkills().stream()
                .filter(skill -> !skill.getBaseDamagePercentRanks().isEmpty())
                .map(PaladinTreeSkill::getSkillId)
                .collect(Collectors.toSet());
        Set<String> skillsWithComponentTables = PaladinSkillTreeRegistry.allSkills().stream()
                .filter(skill -> !skill.getComponentDamagePercentRanks().isEmpty())
                .map(PaladinTreeSkill::getSkillId)
                .collect(Collectors.toSet());

        assertEquals(SIMPLE_SINGLE_COMPONENT_SKILLS, skillsWithSimpleTables);
        assertEquals(MULTI_COMPONENT_SKILLS, skillsWithComponentTables);

        for (String skillId : NON_DAMAGE_OR_MANUAL_WITHOUT_TABLES) {
            PaladinTreeSkill skill = PaladinSkillTreeRegistry.requireSkill(skillId);

            assertTrue(skill.getBaseDamagePercentRanks().isEmpty(), skillId);
            assertTrue(skill.getComponentDamagePercentRanks().isEmpty(), skillId);
        }
    }

    @Test
    void runtime_dps_powinien_pozostac_zablokowany_po_audycie_obecnosci_obrazen() {
        DamageRankingService service = new DamageRankingService(new DamageEngine());
        List<PaladinSkillDamageRankingEntry> entries = service.describeTreeSkills(PlayableClass.PALADIN);

        assertEquals(24, entries.size());
        assertTrue(entries.stream().allMatch(entry -> entry.getDamagePerUse() == null));
        assertTrue(entries.stream().allMatch(entry -> entry.getTheoreticalDps() == null));
    }

    private static Map<String, Map<String, String>> rowsBySkillId() throws IOException {
        return auditRows(Files.readString(AUDIT_PATH, StandardCharsets.UTF_8)).stream()
                .collect(Collectors.toMap(row -> value(row, "skillId"), row -> row));
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
        assertNotNull(row, key);
        return row.get(key);
    }

    private static String stripCode(String value) {
        if (value.startsWith("`") && value.endsWith("`")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
