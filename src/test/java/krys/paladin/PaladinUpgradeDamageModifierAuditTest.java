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

class PaladinUpgradeDamageModifierAuditTest {
    private static final Path AUDIT_PATH = Path.of("docs/paladin/source-md/paladin_upgrade_damage_modifier_audit.md");
    private static final List<String> REQUIRED_COLUMNS = List.of(
            "skillId",
            "polishName",
            "englishName",
            "skillGroup",
            "upgradeGroup",
            "upgradeName",
            "upgradeHasDamageModifier",
            "modifierType",
            "value",
            "valueKind",
            "condition",
            "createsNewDamageComponent",
            "affectedComponent",
            "scalesWithSkillRank",
            "safeForRankingDisplay",
            "safeForRuntimeDps",
            "notes"
    );

    @Test
    void audyt_modyfikatorow_ulepszen_powinien_istniec_i_obejmowac_24_skille() throws IOException {
        String audit = Files.readString(AUDIT_PATH, StandardCharsets.UTF_8);
        List<Map<String, String>> rows = auditRows(audit);

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
    void audyt_powinien_zawierac_potwierdzone_modyfikatory_wymachu() throws IOException {
        List<Map<String, String>> brandishRows = auditRows(Files.readString(AUDIT_PATH, StandardCharsets.UTF_8)).stream()
                .filter(row -> value(row, "skillId").equals("wymach"))
                .toList();

        assertBrandishModifier(brandishRows, "Zwiększenie Obrażeń", "MULTIPLICATIVE_DAMAGE_PERCENT", "20%[X]", "PERCENT_X");
        assertBrandishModifier(brandishRows, "Krzyżowe Uderzenie", "ADDITIONAL_HIT_OR_STRIKE", "2 dodatkowe łuki; 120%", "COMPONENT_PERCENT");
        assertBrandishModifier(brandishRows, "Szybkość Użycia", "CAST_SPEED_OR_COOLDOWN", "20%[+]", "PERCENT_PLUS");
        assertBrandishModifier(brandishRows, "Odsłonięcie", "STATUS_DAMAGE_ENABLER", "20%", "PERCENT_PLUS");
        assertBrandishModifier(brandishRows, "Generowanie Wiary", "RESOURCE_OR_COST", "5 Faith", "TEXT_ONLY");
    }

    @Test
    void audyt_i_model_nie_powinny_odblokowywac_runtime_dps() {
        assertTrue(PaladinSkillTreeRegistry.allSkills().stream()
                .flatMap(skill -> skill.getUpgradeDamageModifiers().stream())
                .allMatch(modifier -> modifier.getSafeForRuntimeDps() != UpgradeDamageSafety.YES));

        DamageRankingService service = new DamageRankingService(new DamageEngine());
        List<PaladinSkillDamageRankingEntry> entries = service.describeTreeSkills(PlayableClass.PALADIN);

        assertEquals(24, entries.size());
        assertTrue(entries.stream().allMatch(entry -> entry.getDamagePerUse() == null));
        assertTrue(entries.stream().allMatch(entry -> entry.getTheoreticalDps() == null));
    }

    private static void assertBrandishModifier(List<Map<String, String>> brandishRows,
                                               String upgradeName,
                                               String modifierType,
                                               String value,
                                               String valueKind) {
        Map<String, String> row = brandishRows.stream()
                .filter(candidate -> value(candidate, "upgradeName").equals(upgradeName))
                .findFirst()
                .orElseThrow();

        assertEquals(modifierType, value(row, "modifierType"));
        assertEquals(value, value(row, "value"));
        assertEquals(valueKind, value(row, "valueKind"));
        assertEquals("YES", value(row, "safeForRankingDisplay"));
        assertTrue(Set.of("NO", "NEEDS_MANUAL_REVIEW").contains(value(row, "safeForRuntimeDps")));
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
