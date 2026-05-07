package krys.paladin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaladinSkillTreeRegistryTest {
    private static final Set<String> EXPECTED_TOP_LEVEL_SKILL_ENTRIES = Set.of(
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
    void powinien_zawierac_wszystkie_wpisy_umiejetnosci_w_drzewie_paladyna_z_pdf() {
        Set<String> skillIds = topLevelSkillEntryIds();

        assertEquals(EXPECTED_TOP_LEVEL_SKILL_ENTRIES, skillIds);
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
    void pdf_mocy_specjalnych_powinien_pozostac_bez_zmian() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(PaladinSkillTreeRegistry.SPECIAL_POWERS_PDF));
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);

        assertEquals("a559c9ddd65c0a64d31a5efbec2baae4a6db6aaa466060665736f580b0adefc0",
                HexFormat.of().formatHex(digest));
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
                Map.entry("furia_niebios", List.of(2, 2, 3)),
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
    void furia_niebios_powinna_miec_poprawiony_uklad_grup_ulepszen() {
        PaladinTreeSkill heavenFury = PaladinSkillTreeRegistry.requireSkill("furia_niebios");

        assertEquals(List.of(2, 2, 3), groupSizes(heavenFury));
        assertEquals(List.of("Czas Działania", "Spowolnienie"), upgradeNames(heavenFury.getUpgradeGroups().get(0)));
        assertEquals(List.of("Osąd", "Premia do Obrażeń"), upgradeNames(heavenFury.getUpgradeGroups().get(1)));
        assertEquals(List.of("Ostateczna Sprawiedliwość", "Krok w Światłości", "Potrojenie"), upgradeNames(heavenFury.getUpgradeGroups().get(2)));
    }

    @Test
    void zenit_powinien_miec_poprawiony_uklad_grup_ulepszen() {
        PaladinTreeSkill zenit = PaladinSkillTreeRegistry.requireSkill("zenit");

        assertEquals(List.of(2, 2, 3), groupSizes(zenit));
        assertEquals(List.of("Szansa na Trafienie Krytyczne", "Osłabienie"), upgradeNames(zenit.getUpgradeGroups().get(0)));
        assertEquals(List.of("Nieustępliwość", "Osłabienie"), upgradeNames(zenit.getUpgradeGroups().get(1)));
        assertEquals(List.of("Empirejska Klinga", "Rozdarcie", "Homilia Stali"), upgradeNames(zenit.getUpgradeGroups().get(2)));
    }

    @Test
    void forteca_powinna_byc_wpisem_umiejetnosci_w_drzewie_a_cierniowa_reduta_jej_ulepszeniem() {
        PaladinTreeSkill fortress = PaladinSkillTreeRegistry.requireSkill("forteca");
        Set<String> topLevelSkillEntryIds = topLevelSkillEntryIds();
        Set<String> fortressUpgradeNames = fortress.getUpgradeGroups().stream()
                .flatMap(group -> group.getUpgrades().stream())
                .map(PaladinSkillUpgrade::getName)
                .collect(Collectors.toSet());

        assertEquals("Forteca", fortress.getSkillName());
        assertEquals(PaladinSkillTreeRegistry.SPECIAL_POWERS_PDF, fortress.getSourcePdf());
        assertEquals("moce_specjalne", fortress.getSkillGroup());
        assertEquals(PaladinSkillTreeType.DEFENSIVE, fortress.getType());
        assertTrue(fortress.getNotes().contains("Specjalne, Defensywa, Moloch"));
        assertFalse(fortress.getSourcePdf().equals(PaladinSkillTreeRegistry.CORE_PDF));
        assertFalse(fortress.getSkillGroup().equals("core"));
        assertFalse(topLevelSkillEntryIds.contains("cierniowa_reduta"));
        assertFalse(topLevelSkillEntryIds.contains("cierniowa_reduta_fortecy"));
        assertTrue(fortressUpgradeNames.contains("Cierniowa Reduta"));
    }

    @Test
    void blogoslawiony_mlot_powinien_miec_bazowy_procent_obrazen_dla_maksymalnej_rangi_drzewa_z_pdf() {
        PaladinTreeSkill blessedHammer = PaladinSkillTreeRegistry.requireSkill("blogoslawiony_mlot");

        assertEquals(PaladinSkillTreeRegistry.CORE_PDF, blessedHammer.getSourcePdf());
        assertEquals("Błogosławiony Młot", blessedHammer.getSkillName());
        assertEquals(115, blessedHammer.getBaseDamagePercentAtRank1());
        assertEquals(293, blessedHammer.getBaseDamagePercentAtTreeMaxRank());
        assertEquals(115, blessedHammer.damagePercentAtRank1());
        assertEquals(293, blessedHammer.damagePercentAtTreeMaxRank(15));
        assertTrue(blessedHammer.getNotes().contains("pełną tabelę bazowych procentów obrażeń 1..15"));
        assertTrue(blessedHammer.getNotes().contains("nie odblokowuje DPS runtime"));
    }

    @Test
    void blogoslawiony_mlot_powinien_miec_pelna_tabele_rang_z_lokalnego_jsona() {
        PaladinTreeSkill blessedHammer = PaladinSkillTreeRegistry.requireSkill("blogoslawiony_mlot");

        assertEquals(Map.ofEntries(
                Map.entry(1, 115),
                Map.entry(2, 126),
                Map.entry(3, 138),
                Map.entry(4, 149),
                Map.entry(5, 167),
                Map.entry(6, 178),
                Map.entry(7, 190),
                Map.entry(8, 201),
                Map.entry(9, 213),
                Map.entry(10, 230),
                Map.entry(11, 241),
                Map.entry(12, 253),
                Map.entry(13, 264),
                Map.entry(14, 276),
                Map.entry(15, 293)
        ), blessedHammer.getBaseDamagePercentRanks().asMap());
    }

    @Test
    void pozostale_skille_paladyna_nie_powinny_miec_bazowych_procentow_bez_jawnego_r1_lub_tree_max() {
        Set<String> skillsWithTreeMaxPercent = Set.of("blogoslawiony_mlot");

        for (PaladinTreeSkill skill : PaladinSkillTreeRegistry.allSkills()) {
            if (!skillsWithTreeMaxPercent.contains(skill.getSkillId())) {
                assertEquals(null, skill.getBaseDamagePercentAtRank1(), skill.getSkillId());
                assertTrue(skill.getBaseDamagePercentRanks().isEmpty(), skill.getSkillId());
            }
            if (!skillsWithTreeMaxPercent.contains(skill.getSkillId())) {
                assertEquals(null, skill.getBaseDamagePercentAtTreeMaxRank(), skill.getSkillId());
            }
        }
    }

    @Test
    void reprezentatywne_null_procenty_powinny_miec_powod_w_notatkach() {
        PaladinTreeSkill nonDamage = PaladinSkillTreeRegistry.requireSkill("aura_fanatyzmu");
        PaladinTreeSkill noExplicitRank = PaladinSkillTreeRegistry.requireSkill("wymach");
        PaladinTreeSkill multiComponent = PaladinSkillTreeRegistry.requireSkill("furia_niebios");

        assertEquals(PaladinSkillTreeStatus.NON_DAMAGE, nonDamage.getStatus());
        assertEquals(null, nonDamage.getBaseDamagePercentAtRank1());
        assertEquals(null, nonDamage.getBaseDamagePercentAtTreeMaxRank());
        assertTrue(nonDamage.getNotes().contains("brak bezpośredniego modelu obrażeń"));

        assertEquals(null, noExplicitRank.getBaseDamagePercentAtRank1());
        assertEquals(null, noExplicitRank.getBaseDamagePercentAtTreeMaxRank());
        assertTrue(noExplicitRank.getNotes().contains("PDF nie podaje jednoznacznie R1/treeMax"));

        assertEquals(null, multiComponent.getBaseDamagePercentAtRank1());
        assertEquals(null, multiComponent.getBaseDamagePercentAtTreeMaxRank());
        assertTrue(multiComponent.getNotes().contains("wielohitowy/tickowy/warunkowy"));
    }

    @Test
    void skazanie_powinno_byc_umiejetnoscia_obrazeniowa_wymagajaca_weryfikacji() {
        PaladinTreeSkill condemnation = PaladinSkillTreeRegistry.requireSkill("skazanie");

        assertEquals(PaladinSkillTreeType.DAMAGE, condemnation.getType());
        assertEquals(PaladinSkillTreeStatus.NEEDS_VERIFICATION, condemnation.getStatus());
    }

    @Test
    void stare_foundation_skille_nie_powinny_byc_w_rejestrze_wpisow_umiejetnosci_paladyna() {
        Set<String> skillIds = topLevelSkillEntryIds();

        assertFalse(skillIds.contains("BRANDISH"));
        assertFalse(skillIds.contains("HOLY_BOLT"));
        assertFalse(skillIds.contains("CLASH"));
        assertFalse(skillIds.contains("ADVANCE"));
    }

    private static Set<String> topLevelSkillEntryIds() {
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

    private static List<String> upgradeNames(PaladinSkillUpgradeGroup group) {
        return group.getUpgrades().stream()
                .map(PaladinSkillUpgrade::getName)
                .toList();
    }
}
