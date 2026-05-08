package krys.paladin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static krys.paladin.DamagePercentComponent.ACTIVE_DAMAGE;
import static krys.paladin.DamagePercentComponent.ADDITIONAL_STRIKE_DAMAGE;
import static krys.paladin.DamagePercentComponent.BURST_DAMAGE;
import static krys.paladin.DamagePercentComponent.FIRST_STRIKE_DAMAGE;
import static krys.paladin.DamagePercentComponent.JUMP_DAMAGE;
import static krys.paladin.DamagePercentComponent.LANDING_DAMAGE;
import static krys.paladin.DamagePercentComponent.PASSIVE_DAMAGE;
import static krys.paladin.DamagePercentComponent.PRIMARY_DAMAGE;
import static krys.paladin.DamagePercentComponent.SECOND_STRIKE_DAMAGE;
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
    private static final Map<String, Map<Integer, Integer>> EXPECTED_SIMPLE_DAMAGE_RANK_TABLES = Map.ofEntries(
            Map.entry("wymach", rankTable(75, 83, 90, 97, 109, 116, 124, 131, 139, 150, 157, 165, 172, 180, 191)),
            Map.entry("swiety_pocisk", rankTable(90, 99, 108, 117, 131, 139, 148, 157, 166, 180, 189, 198, 207, 216, 229)),
            Map.entry("starcie", rankTable(115, 126, 138, 149, 167, 178, 190, 201, 213, 230, 241, 253, 264, 276, 293)),
            Map.entry("natarcie", rankTable(105, 115, 126, 136, 152, 163, 173, 184, 194, 210, 220, 231, 241, 252, 268)),
            Map.entry("blogoslawiona_tarcza", rankTable(205, 226, 246, 266, 297, 318, 338, 359, 379, 410, 430, 451, 471, 492, 523)),
            Map.entry("blogoslawiony_mlot", rankTable(115, 126, 138, 149, 167, 178, 190, 201, 213, 230, 241, 253, 264, 276, 293)),
            Map.entry("boska_lanca", rankTable(90, 99, 108, 117, 131, 139, 148, 157, 166, 180, 189, 198, 207, 216, 229)),
            Map.entry("uderzenie_tarcza", rankTable(205, 226, 246, 266, 297, 318, 338, 359, 379, 410, 430, 451, 471, 492, 523)),
            Map.entry("szarza_z_tarcza", rankTable(90, 99, 108, 117, 131, 139, 148, 157, 166, 180, 189, 198, 207, 216, 229)),
            Map.entry("skazanie", rankTable(240, 264, 288, 312, 348, 372, 396, 420, 444, 480, 504, 528, 552, 576, 612)),
            Map.entry("konsekracja", rankTable(75, 83, 90, 97, 109, 116, 124, 131, 139, 150, 157, 165, 172, 180, 191))
    );
    private static final Set<String> MULTI_COMPONENT_OR_MANUAL_REVIEW_SKILLS_WITHOUT_SIMPLE_TABLE = Set.of(
            "zapal",
            "aura_swietej_swiatlosci",
            "spadajaca_gwiazda",
            "wlocznia_niebios",
            "zenit",
            "furia_niebios",
            "arbiter_sprawiedliwosci"
    );
    private static final Set<String> NON_DAMAGE_SKILLS_WITHOUT_SIMPLE_TABLE = Set.of(
            "aura_fanatyzmu",
            "aura_smialosci",
            "egida",
            "mobilizacja",
            "oczyszczenie",
            "forteca"
    );
    private static final Map<String, Map<DamagePercentComponent, Map<Integer, Integer>>> EXPECTED_COMPONENT_DAMAGE_RANK_TABLES = Map.ofEntries(
            Map.entry("zapal", Map.ofEntries(
                    Map.entry(PRIMARY_DAMAGE, rankTable(80, 88, 96, 104, 116, 124, 132, 140, 148, 160, 168, 176, 184, 192, 204)),
                    Map.entry(ADDITIONAL_STRIKE_DAMAGE, rankTable(20, 22, 24, 26, 29, 31, 33, 35, 37, 40, 42, 44, 46, 48, 51))
            )),
            Map.entry("aura_swietej_swiatlosci", Map.ofEntries(
                    Map.entry(PASSIVE_DAMAGE, rankTable(45, 50, 54, 58, 65, 70, 74, 79, 83, 90, 94, 99, 103, 108, 115)),
                    Map.entry(ACTIVE_DAMAGE, rankTable(320, 352, 384, 416, 464, 496, 528, 560, 592, 640, 672, 704, 736, 768, 816))
            )),
            Map.entry("spadajaca_gwiazda", Map.ofEntries(
                    Map.entry(LANDING_DAMAGE, rankTable(80, 264, 288, 312, 348, 372, 396, 420, 444, 480, 504, 528, 552, 576, 612))
            )),
            Map.entry("wlocznia_niebios", Map.ofEntries(
                    Map.entry(PRIMARY_DAMAGE, rankTable(160, 176, 192, 208, 232, 248, 264, 280, 296, 320, 336, 352, 368, 384, 408)),
                    Map.entry(BURST_DAMAGE, rankTable(120, 132, 144, 156, 174, 186, 198, 210, 222, 240, 252, 264, 276, 288, 306))
            )),
            Map.entry("zenit", Map.ofEntries(
                    Map.entry(FIRST_STRIKE_DAMAGE, rankTable(450, 495, 540, 585, 653, 697, 742, 788, 832, 900, 945, 990, 1035, 1080, 1147)),
                    Map.entry(SECOND_STRIKE_DAMAGE, rankTable(400, 440, 480, 520, 580, 620, 660, 700, 740, 800, 840, 880, 920, 960, 1020))
            ))
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
    void lokalne_dane_wiary_powinny_byc_rozdzielone_na_koszt_i_generowanie() {
        PaladinTreeSkill brandish = PaladinSkillTreeRegistry.requireSkill("wymach");
        PaladinTreeSkill holyBolt = PaladinSkillTreeRegistry.requireSkill("swiety_pocisk");
        PaladinTreeSkill clash = PaladinSkillTreeRegistry.requireSkill("starcie");
        PaladinTreeSkill advance = PaladinSkillTreeRegistry.requireSkill("natarcie");
        PaladinTreeSkill blessedHammer = PaladinSkillTreeRegistry.requireSkill("blogoslawiony_mlot");

        assertEquals(null, brandish.getFaithCost());
        assertEquals(14, brandish.getFaithGenerationBase());
        assertEquals(5, brandish.getFaithGenerationBonusKnown());
        assertEquals(16, holyBolt.getFaithGenerationBase());
        assertEquals(7, holyBolt.getFaithGenerationBonusKnown());
        assertEquals(20, clash.getFaithGenerationBase());
        assertEquals(10, clash.getFaithGenerationBonusKnown());
        assertEquals(18, advance.getFaithGenerationBase());
        assertEquals(null, advance.getFaithGenerationBonusKnown());
        assertEquals(10, blessedHammer.getFaithCost());
        assertEquals(null, blessedHammer.getFaithGenerationBase());
    }

    @Test
    void skille_simple_single_component_powinny_miec_pelne_tabele_rang_z_lokalnego_jsona() {
        assertEquals(SIMPLE_SINGLE_COMPONENT_SKILLS, EXPECTED_SIMPLE_DAMAGE_RANK_TABLES.keySet());

        for (Map.Entry<String, Map<Integer, Integer>> expectation : EXPECTED_SIMPLE_DAMAGE_RANK_TABLES.entrySet()) {
            PaladinTreeSkill skill = PaladinSkillTreeRegistry.requireSkill(expectation.getKey());

            assertFalse(skill.getBaseDamagePercentRanks().isEmpty(), expectation.getKey());
            assertEquals(15, skill.getBaseDamagePercentRanks().asMap().size(), expectation.getKey());
            assertEquals(IntStream.rangeClosed(1, 15).boxed().collect(Collectors.toSet()),
                    skill.getBaseDamagePercentRanks().asMap().keySet(),
                    expectation.getKey());
            assertEquals(expectation.getValue(), skill.getBaseDamagePercentRanks().asMap(), expectation.getKey());
            assertEquals(expectation.getValue().get(1), skill.getBaseDamagePercentAtRank1(), expectation.getKey());
            assertEquals(expectation.getValue().get(15), skill.getBaseDamagePercentAtTreeMaxRank(), expectation.getKey());
        }
    }

    @Test
    void zaimportowane_tabele_simple_single_component_powinny_byc_zgodne_z_lokalnym_jsonem() throws Exception {
        String json = Files.readString(Path.of("docs/paladin/source-md/paladin_fextralife_rank_tables.json"));

        for (String skillId : SIMPLE_SINGLE_COMPONENT_SKILLS) {
            assertEquals(EXPECTED_SIMPLE_DAMAGE_RANK_TABLES.get(skillId), firstBracketPercentTableFromJson(json, skillId), skillId);
            assertEquals(firstBracketPercentTableFromJson(json, skillId),
                    PaladinSkillTreeRegistry.requireSkill(skillId).getBaseDamagePercentRanks().asMap(),
                    skillId);
        }
    }

    @Test
    void blogoslawiony_mlot_powinien_pozostac_z_pelna_tabela_rang_bez_zmian() {
        PaladinTreeSkill blessedHammer = PaladinSkillTreeRegistry.requireSkill("blogoslawiony_mlot");

        assertEquals(rankTable(115, 126, 138, 149, 167, 178, 190, 201, 213, 230, 241, 253, 264, 276, 293),
                blessedHammer.getBaseDamagePercentRanks().asMap());
    }

    @Test
    void blogoslawiony_mlot_powinien_miec_opisowy_podzial_wplywu_ulepszen_na_obrazenia() {
        PaladinTreeSkill blessedHammer = PaladinSkillTreeRegistry.requireSkill("blogoslawiony_mlot");
        Map<String, UpgradeDamageImpactType> impactsByUpgradeName = blessedHammer.getUpgradeDamageImpacts().stream()
                .collect(Collectors.toMap(UpgradeDamageImpact::getUpgradeName, UpgradeDamageImpact::getType));

        assertEquals(7, impactsByUpgradeName.size());
        assertEquals(UpgradeDamageImpactType.COOLDOWN_OR_COST, impactsByUpgradeName.get("Redukcja Kosztu"));
        assertEquals(UpgradeDamageImpactType.NEEDS_VERIFICATION, impactsByUpgradeName.get("Premia do Obrażeń"));
        assertEquals(UpgradeDamageImpactType.COOLDOWN_OR_COST, impactsByUpgradeName.get("Zwiększenie Szybkości Użycia"));
        assertEquals(UpgradeDamageImpactType.STATUS_OR_UTILITY, impactsByUpgradeName.get("Spowolnienie"));
        assertEquals(UpgradeDamageImpactType.NO_DAMAGE_IMPACT, impactsByUpgradeName.get("Budująca Walka"));
        assertEquals(UpgradeDamageImpactType.NO_DAMAGE_IMPACT, impactsByUpgradeName.get("Apostolska Aureola"));
        assertEquals(UpgradeDamageImpactType.NEEDS_VERIFICATION, impactsByUpgradeName.get("Druzgocący Cios"));
        assertTrue(blessedHammer.getUpgradeDamageImpacts().stream()
                .filter(UpgradeDamageImpact::affectsDamage)
                .allMatch(impact -> impact.getDamagePercent() == null));
    }

    @Test
    void multi_component_non_damage_i_manual_review_nie_powinny_miec_prostej_tabeli_rang() {
        Set<String> skippedSkills = java.util.stream.Stream.concat(
                        MULTI_COMPONENT_OR_MANUAL_REVIEW_SKILLS_WITHOUT_SIMPLE_TABLE.stream(),
                        NON_DAMAGE_SKILLS_WITHOUT_SIMPLE_TABLE.stream())
                .collect(Collectors.toSet());

        for (String skillId : skippedSkills) {
            PaladinTreeSkill skill = PaladinSkillTreeRegistry.requireSkill(skillId);

            assertEquals(null, skill.getBaseDamagePercentAtRank1(), skillId);
            assertEquals(null, skill.getBaseDamagePercentAtTreeMaxRank(), skillId);
            assertTrue(skill.getBaseDamagePercentRanks().isEmpty(), skillId);
        }
    }

    @Test
    void multi_component_powinny_miec_komponentowe_tabele_rang_bez_prostej_tabeli_bazowej() {
        for (Map.Entry<String, Map<DamagePercentComponent, Map<Integer, Integer>>> expectation : EXPECTED_COMPONENT_DAMAGE_RANK_TABLES.entrySet()) {
            PaladinTreeSkill skill = PaladinSkillTreeRegistry.requireSkill(expectation.getKey());
            DamagePercentComponentRankTable componentTable = skill.getComponentDamagePercentRanks();

            assertTrue(skill.getBaseDamagePercentRanks().isEmpty(), expectation.getKey());
            assertEquals(null, skill.getBaseDamagePercentAtRank1(), expectation.getKey());
            assertEquals(null, skill.getBaseDamagePercentAtTreeMaxRank(), expectation.getKey());
            assertFalse(componentTable.isEmpty(), expectation.getKey());
            assertEquals(expectation.getValue().keySet(), componentTable.asMap().keySet(), expectation.getKey());
            for (Map.Entry<DamagePercentComponent, Map<Integer, Integer>> componentExpectation : expectation.getValue().entrySet()) {
                DamagePercentComponent component = componentExpectation.getKey();

                assertTrue(componentTable.hasComponent(component), expectation.getKey() + " " + component);
                assertEquals(componentExpectation.getValue(), componentTable.tableFor(component).asMap(), expectation.getKey() + " " + component);
                assertEquals(componentExpectation.getValue().get(1), componentTable.damagePercentAt(component, 1), expectation.getKey() + " " + component);
                assertEquals(componentExpectation.getValue().get(15), componentTable.damagePercentAt(component, 15), expectation.getKey() + " " + component);
            }
        }

        PaladinTreeSkill fallingStar = PaladinSkillTreeRegistry.requireSkill("spadajaca_gwiazda");
        assertFalse(fallingStar.getComponentDamagePercentRanks().hasComponent(JUMP_DAMAGE));
        assertTrue(fallingStar.getNotes().contains("JUMP_DAMAGE nie został zaimportowany"));
    }

    @Test
    void zaimportowane_tabele_component_multi_component_powinny_byc_zgodne_z_lokalnym_jsonem() throws Exception {
        String json = Files.readString(Path.of("docs/paladin/source-md/paladin_fextralife_rank_tables.json"));

        assertEquals(EXPECTED_COMPONENT_DAMAGE_RANK_TABLES.get("zapal").get(PRIMARY_DAMAGE), bracketPercentTableFromJson(json, "zapal", 0));
        assertEquals(EXPECTED_COMPONENT_DAMAGE_RANK_TABLES.get("zapal").get(ADDITIONAL_STRIKE_DAMAGE), bracketPercentTableFromJson(json, "zapal", 1));
        assertEquals(EXPECTED_COMPONENT_DAMAGE_RANK_TABLES.get("aura_swietej_swiatlosci").get(PASSIVE_DAMAGE), bracketPercentTableFromJson(json, "aura_swietej_swiatlosci", 0));
        assertEquals(EXPECTED_COMPONENT_DAMAGE_RANK_TABLES.get("aura_swietej_swiatlosci").get(ACTIVE_DAMAGE), bracketPercentTableFromJson(json, "aura_swietej_swiatlosci", 1));
        assertEquals(EXPECTED_COMPONENT_DAMAGE_RANK_TABLES.get("spadajaca_gwiazda").get(LANDING_DAMAGE), componentLabelTableFromJson(json, "spadajaca_gwiazda", "Landing Damage"));
        assertEquals(EXPECTED_COMPONENT_DAMAGE_RANK_TABLES.get("wlocznia_niebios").get(PRIMARY_DAMAGE), bracketPercentTableFromJson(json, "wlocznia_niebios", 0));
        assertEquals(EXPECTED_COMPONENT_DAMAGE_RANK_TABLES.get("wlocznia_niebios").get(BURST_DAMAGE), bracketPercentTableFromJson(json, "wlocznia_niebios", 1));
        assertEquals(EXPECTED_COMPONENT_DAMAGE_RANK_TABLES.get("zenit").get(FIRST_STRIKE_DAMAGE), bracketPercentTableFromJson(json, "zenit", 0));
        assertEquals(EXPECTED_COMPONENT_DAMAGE_RANK_TABLES.get("zenit").get(SECOND_STRIKE_DAMAGE), bracketPercentTableFromJson(json, "zenit", 1));

        assertEquals(Set.of(1, 3, 5, 6, 7, 9, 10, 11, 12, 14, 15),
                componentLabelTableFromJson(json, "spadajaca_gwiazda", "Jump Damage").keySet());
    }

    @Test
    void manual_review_i_non_damage_nie_powinny_miec_komponentowych_tabel_rang() {
        for (String skillId : Set.of("furia_niebios", "arbiter_sprawiedliwosci")) {
            assertTrue(PaladinSkillTreeRegistry.requireSkill(skillId).getComponentDamagePercentRanks().isEmpty(), skillId);
        }
        for (String skillId : NON_DAMAGE_SKILLS_WITHOUT_SIMPLE_TABLE) {
            assertTrue(PaladinSkillTreeRegistry.requireSkill(skillId).getComponentDamagePercentRanks().isEmpty(), skillId);
        }
    }

    @Test
    void reprezentatywne_null_procenty_powinny_miec_powod_w_notatkach() {
        PaladinTreeSkill nonDamage = PaladinSkillTreeRegistry.requireSkill("aura_fanatyzmu");
        PaladinTreeSkill noSimpleTable = PaladinSkillTreeRegistry.requireSkill("zapal");
        PaladinTreeSkill multiComponent = PaladinSkillTreeRegistry.requireSkill("furia_niebios");

        assertEquals(PaladinSkillTreeStatus.NON_DAMAGE, nonDamage.getStatus());
        assertEquals(null, nonDamage.getBaseDamagePercentAtRank1());
        assertEquals(null, nonDamage.getBaseDamagePercentAtTreeMaxRank());
        assertTrue(nonDamage.getNotes().contains("brak bezpośredniego modelu obrażeń"));

        assertEquals(null, noSimpleTable.getBaseDamagePercentAtRank1());
        assertEquals(null, noSimpleTable.getBaseDamagePercentAtTreeMaxRank());
        assertTrue(noSimpleTable.getNotes().contains("komponentowe tabele"));

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

    private static Map<Integer, Integer> rankTable(int rank1,
                                                   int rank2,
                                                   int rank3,
                                                   int rank4,
                                                   int rank5,
                                                   int rank6,
                                                   int rank7,
                                                   int rank8,
                                                   int rank9,
                                                   int rank10,
                                                   int rank11,
                                                   int rank12,
                                                   int rank13,
                                                   int rank14,
                                                   int rank15) {
        return Map.ofEntries(
                Map.entry(1, rank1),
                Map.entry(2, rank2),
                Map.entry(3, rank3),
                Map.entry(4, rank4),
                Map.entry(5, rank5),
                Map.entry(6, rank6),
                Map.entry(7, rank7),
                Map.entry(8, rank8),
                Map.entry(9, rank9),
                Map.entry(10, rank10),
                Map.entry(11, rank11),
                Map.entry(12, rank12),
                Map.entry(13, rank13),
                Map.entry(14, rank14),
                Map.entry(15, rank15)
        );
    }

    private static Map<Integer, Integer> firstBracketPercentTableFromJson(String json, String skillId) {
        return bracketPercentTableFromJson(json, skillId, 0);
    }

    private static Map<Integer, Integer> bracketPercentTableFromJson(String json, String skillId, int bracketPercentIndex) {
        String rankTableJson = rankTableJson(json, skillId);
        Pattern rankPattern = Pattern.compile("(?s)\\{\\s*\"rank\"\\s*:\\s*(\\d+).*?\"bracketPercents\"\\s*:\\s*\\[(.*?)\\].*?\\}");
        Matcher rankMatcher = rankPattern.matcher(rankTableJson);
        Map<Integer, Integer> table = new LinkedHashMap<>();
        while (rankMatcher.find()) {
            int rank = Integer.parseInt(rankMatcher.group(1));
            Matcher percentMatcher = Pattern.compile("\\d+").matcher(rankMatcher.group(2));
            int index = 0;
            while (percentMatcher.find()) {
                if (index == bracketPercentIndex) {
                    table.put(rank, Integer.parseInt(percentMatcher.group()));
                    break;
                }
                index++;
            }
        }
        if (table.size() != 15) {
            throw new AssertionError("Tabela JSON nie ma kompletu 1..15 dla: " + skillId);
        }
        return table;
    }

    private static Map<Integer, Integer> componentLabelTableFromJson(String json, String skillId, String label) {
        String rankTableJson = rankTableJson(json, skillId);
        Pattern rankPattern = Pattern.compile("(?s)\\{\\s*\"rank\"\\s*:\\s*(\\d+).*?\"upgrade\"\\s*:\\s*\"(.*?)\".*?\\}");
        Matcher rankMatcher = rankPattern.matcher(rankTableJson);
        Pattern labelPattern = Pattern.compile(Pattern.quote(label) + ": \\[([0-9,]+)%\\]");
        Map<Integer, Integer> table = new LinkedHashMap<>();
        while (rankMatcher.find()) {
            Matcher labelMatcher = labelPattern.matcher(rankMatcher.group(2));
            if (labelMatcher.find()) {
                table.put(Integer.parseInt(rankMatcher.group(1)), Integer.parseInt(labelMatcher.group(1).replace(",", "")));
            }
        }
        return table;
    }

    private static String rankTableJson(String json, String skillId) {
        String skillMarker = "\"skillId\": \"" + skillId + "\"";
        int skillIndex = json.indexOf(skillMarker);
        if (skillIndex < 0) {
            throw new AssertionError("Brak skilla w lokalnym JSON: " + skillId);
        }
        int rankTableIndex = json.indexOf("\"rankTable\"", skillIndex);
        int arrayStart = json.indexOf('[', rankTableIndex);
        int arrayEnd = findMatchingBracket(json, arrayStart);
        return json.substring(arrayStart + 1, arrayEnd);
    }

    private static int findMatchingBracket(String value, int arrayStart) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = arrayStart; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '[') {
                depth++;
            } else if (current == ']') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        throw new AssertionError("Nie zamknięto tablicy JSON.");
    }
}
