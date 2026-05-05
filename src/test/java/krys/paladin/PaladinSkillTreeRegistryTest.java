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
    @Test
    void powinien_zawierac_nowe_umiejetnosci_paladyna_z_pdf() {
        Set<String> skillIds = PaladinSkillTreeRegistry.allSkills().stream()
                .map(PaladinTreeSkill::getSkillId)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "skazanie",
                "wlocznia_niebios",
                "konsekracja",
                "oczyszczenie",
                "furia_niebios",
                "cierniowa_reduta_fortecy",
                "zenit",
                "arbiter_sprawiedliwosci"
        ), skillIds);
    }

    @Test
    void kazda_nowa_umiejetnosc_powinna_miec_poprawne_zrodlo_grupe_i_status() {
        Map<String, ExpectedSkillMetadata> expected = Map.of(
                "skazanie", new ExpectedSkillMetadata(PaladinSkillTreeRegistry.JUSTICE_PDF, "sprawiedliwosc", PaladinSkillTreeStatus.UNSUPPORTED),
                "wlocznia_niebios", new ExpectedSkillMetadata(PaladinSkillTreeRegistry.JUSTICE_PDF, "sprawiedliwosc", PaladinSkillTreeStatus.NEEDS_VERIFICATION),
                "konsekracja", new ExpectedSkillMetadata(PaladinSkillTreeRegistry.JUSTICE_PDF, "sprawiedliwosc", PaladinSkillTreeStatus.NEEDS_VERIFICATION),
                "oczyszczenie", new ExpectedSkillMetadata(PaladinSkillTreeRegistry.JUSTICE_PDF, "sprawiedliwosc", PaladinSkillTreeStatus.NEEDS_VERIFICATION),
                "furia_niebios", new ExpectedSkillMetadata(PaladinSkillTreeRegistry.SPECIAL_POWERS_PDF, "moce_specjalne", PaladinSkillTreeStatus.NEEDS_VERIFICATION),
                "cierniowa_reduta_fortecy", new ExpectedSkillMetadata(PaladinSkillTreeRegistry.SPECIAL_POWERS_PDF, "moce_specjalne", PaladinSkillTreeStatus.NEEDS_VERIFICATION),
                "zenit", new ExpectedSkillMetadata(PaladinSkillTreeRegistry.SPECIAL_POWERS_PDF, "moce_specjalne", PaladinSkillTreeStatus.NEEDS_VERIFICATION),
                "arbiter_sprawiedliwosci", new ExpectedSkillMetadata(PaladinSkillTreeRegistry.SPECIAL_POWERS_PDF, "moce_specjalne", PaladinSkillTreeStatus.NEEDS_VERIFICATION)
        );

        for (Map.Entry<String, ExpectedSkillMetadata> expectation : expected.entrySet()) {
            PaladinTreeSkill skill = PaladinSkillTreeRegistry.requireSkill(expectation.getKey());

            assertEquals(expectation.getValue().sourcePdf(), skill.getSourcePdf());
            assertEquals(expectation.getValue().skillGroup(), skill.getSkillGroup());
            assertEquals(expectation.getValue().status(), skill.getStatus());
        }
    }

    @Test
    void zenit_powinien_miec_poprawiony_uklad_grup_ulepszen() {
        PaladinTreeSkill zenith = PaladinSkillTreeRegistry.requireSkill("zenit");
        List<PaladinSkillUpgradeGroup> groups = zenith.getUpgradeGroups();

        assertEquals(3, groups.size());
        assertEquals(List.of("Szansa na Trafienie Krytyczne", "Osłabienie"), upgradeNames(groups.get(0)));
        assertEquals(List.of(
                "Nieustępliwość",
                "Osłabienie: zabijanie osłabionych wrogów podczas działania Zenitu skraca jego czas odnowienia o 2 sek."
        ), upgradeNames(groups.get(1)));
        assertEquals(List.of("Empirejska Klinga", "Rozdarcie", "Homilia Stali"), upgradeNames(groups.get(2)));
    }

    @Test
    void stare_foundation_skille_nie_powinny_byc_w_glownym_rejestrze_paladyna() {
        Set<String> skillIds = PaladinSkillTreeRegistry.allSkills().stream()
                .map(PaladinTreeSkill::getSkillId)
                .collect(Collectors.toSet());

        assertFalse(skillIds.contains("BRANDISH"));
        assertFalse(skillIds.contains("HOLY_BOLT"));
        assertFalse(skillIds.contains("CLASH"));
        assertFalse(skillIds.contains("ADVANCE"));
    }

    private static List<String> upgradeNames(PaladinSkillUpgradeGroup group) {
        return group.getUpgrades().stream()
                .map(PaladinSkillUpgrade::getName)
                .toList();
    }

    private record ExpectedSkillMetadata(String sourcePdf, String skillGroup, PaladinSkillTreeStatus status) {
    }
}
