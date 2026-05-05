package krys.paladin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaladinSkillTreeRegistryTest {
    private static final Set<String> EXPECTED_MAIN_SKILLS = Set.of(
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
    );

    @Test
    void powinien_zawierac_wszystkie_glowne_umiejetnosci_paladyna_z_pdf() {
        Set<String> skillIds = mainSkillIds();

        assertEquals(EXPECTED_MAIN_SKILLS, skillIds);
        assertEquals(24, PaladinSkillTreeRegistry.allSkills().size());
    }

    @Test
    void powinien_obejmowac_wszystkie_pdfy_zrodlowe() {
        Set<String> sourcePdfs = PaladinSkillTreeRegistry.allSkills().stream()
                .map(PaladinTreeSkill::getSourcePdf)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                PaladinSkillTreeRegistry.BASIC_PDF,
                PaladinSkillTreeRegistry.CORE_PDF,
                PaladinSkillTreeRegistry.AURA_PDF,
                PaladinSkillTreeRegistry.COURAGE_PDF,
                PaladinSkillTreeRegistry.JUSTICE_PDF,
                PaladinSkillTreeRegistry.SPECIAL_POWERS_PDF
        ), sourcePdfs);
    }

    @Test
    void kazda_umiejetnosc_z_opisanymi_ulepszeniami_powinna_miec_trzy_grupy() {
        Map<String, List<Integer>> expectedGroupSizes = Map.ofEntries(
                Map.entry("wymach", List.of(2, 2, 3)),
                Map.entry("swiety_pocisk", List.of(2, 2, 3)),
                Map.entry("starcie", List.of(2, 2, 3)),
                Map.entry("natarcie", List.of(2, 2, 3)),
                Map.entry("blogoslawiona_tarcza", List.of(2, 2, 3)),
                Map.entry("blogoslawiony_mlot", List.of(2, 2, 3)),
                Map.entry("boska_lanca", List.of(2, 2, 3)),
                Map.entry("uderzenie_tarcza", List.of(2, 2, 3)),
                Map.entry("zapal", List.of(2, 2, 3)),
                Map.entry("aura_fanatyzmu", List.of(2, 2, 3)),
                Map.entry("aura_smialosci", List.of(2, 2, 3)),
                Map.entry("aura_swietej_swiatlosci", List.of(2, 2, 3)),
                Map.entry("szarza_z_tarcza", List.of(2, 2, 3)),
                Map.entry("egida", List.of(2, 2, 3)),
                Map.entry("spadajaca_gwiazda", List.of(2, 2, 3)),
                Map.entry("mobilizacja", List.of(2, 2, 3)),
                Map.entry("skazanie", List.of(2, 2, 3)),
                Map.entry("wlocznia_niebios", List.of(2, 2, 3)),
                Map.entry("konsekracja", List.of(2, 2, 3)),
                Map.entry("oczyszczenie", List.of(2, 2, 3)),
                Map.entry("furia_niebios", List.of(3, 2, 2)),
                Map.entry("forteca", List.of(2, 2, 3)),
                Map.entry("zenit", List.of(2, 2, 3)),
                Map.entry("arbiter_sprawiedliwosci", List.of(2, 2, 3))
        );

        for (Map.Entry<String, List<Integer>> expectation : expectedGroupSizes.entrySet()) {
            PaladinTreeSkill skill = PaladinSkillTreeRegistry.requireSkill(expectation.getKey());

            assertEquals(List.of("grupa_1", "grupa_2", "grupa_3"), groupIds(skill));
            assertEquals(expectation.getValue(), groupSizes(skill));
            assertTrue(skill.getUpgradeGroups().stream()
                    .flatMap(group -> group.getUpgrades().stream())
                    .allMatch(upgrade -> !upgrade.getSourceNote().isBlank()));
        }
    }

    @Test
    void forteca_powinna_byc_main_skillem_a_cierniowa_reduta_jej_ulepszeniem() {
        PaladinTreeSkill fortress = PaladinSkillTreeRegistry.requireSkill("forteca");
        Set<String> mainSkillIds = mainSkillIds();
        Set<String> fortressUpgradeNames = fortress.getUpgradeGroups().stream()
                .flatMap(group -> group.getUpgrades().stream())
                .map(PaladinSkillUpgrade::getName)
                .collect(Collectors.toSet());

        assertEquals("Forteca", fortress.getSkillName());
        assertFalse(mainSkillIds.contains("cierniowa_reduta"));
        assertFalse(mainSkillIds.contains("cierniowa_reduta_fortecy"));
        assertTrue(fortressUpgradeNames.contains("Cierniowa Reduta"));
    }

    @Test
    void skazanie_powinno_byc_umiejetnoscia_obrazeniowa_wymagajaca_weryfikacji() {
        PaladinTreeSkill condemnation = PaladinSkillTreeRegistry.requireSkill("skazanie");

        assertEquals(PaladinSkillTreeType.DAMAGE, condemnation.getType());
        assertEquals(PaladinSkillTreeStatus.NEEDS_VERIFICATION, condemnation.getStatus());
    }

    @Test
    void stare_foundation_skille_nie_powinny_byc_w_glownym_rejestrze_paladyna() {
        Set<String> skillIds = mainSkillIds();

        assertFalse(skillIds.contains("BRANDISH"));
        assertFalse(skillIds.contains("HOLY_BOLT"));
        assertFalse(skillIds.contains("CLASH"));
        assertFalse(skillIds.contains("ADVANCE"));
    }

    private static Set<String> mainSkillIds() {
        return PaladinSkillTreeRegistry.allSkills().stream()
                .map(PaladinTreeSkill::getSkillId)
                .collect(Collectors.toSet());
    }

    private static List<String> groupIds(PaladinTreeSkill skill) {
        return skill.getUpgradeGroups().stream()
                .map(PaladinSkillUpgradeGroup::getId)
                .toList();
    }

    private static List<Integer> groupSizes(PaladinTreeSkill skill) {
        return skill.getUpgradeGroups().stream()
                .map(group -> group.getUpgrades().size())
                .toList();
    }
}
