package krys.ranking;

import krys.app.SampleBuildFactory;
import krys.combat.DamageBreakdown;
import krys.combat.DamageComponentBreakdown;
import krys.combat.DamageEngine;
import krys.paladin.PaladinSkillTreeRegistry;
import krys.paladin.PaladinSkillTreeStatus;
import krys.paladin.PaladinSkillTreeType;
import krys.paladin.PaladinTreeSkill;
import krys.simulation.HeroBuildSnapshot;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRankingServiceTest {
    private final DamageEngine damageEngine = new DamageEngine();
    private final DamageRankingService service = new DamageRankingService(damageEngine);

    @Test
    void ranking_domyslny_powinien_korzystac_z_nowego_rejestru_pdf_zamiast_starego_foundation() {
        List<PaladinSkillDamageRankingEntry> ranking = service.rankDamageSkills(PaladinDamageRankingMetric.DAMAGE_PER_USE);
        Set<String> skillIds = ranking.stream()
                .map(PaladinSkillDamageRankingEntry::getSkillId)
                .collect(Collectors.toSet());

        assertTrue(skillIds.contains("wlocznia_niebios"));
        assertTrue(skillIds.contains("zenit"));
        assertFalse(skillIds.contains(SkillId.BRANDISH.name()));
        assertFalse(skillIds.contains(SkillId.HOLY_BOLT.name()));
        assertFalse(skillIds.contains(SkillId.CLASH.name()));
        assertFalse(skillIds.contains(SkillId.ADVANCE.name()));
    }

    @Test
    void paladyn_przez_provider_powinien_uzywac_paladin_skill_tree_registry() {
        SkillTreeRegistryProvider provider = SkillTreeRegistryProvider.paladinOnly();
        CharacterSkillTreeRegistry registry = provider.registryFor(PlayableClass.PALADIN);
        Set<String> providerSkillIds = registry.allSkills().stream()
                .map(skill -> skill.getSkillId())
                .collect(Collectors.toSet());
        Set<String> paladinRegistrySkillIds = PaladinSkillTreeRegistry.allSkills().stream()
                .map(skill -> skill.getSkillId())
                .collect(Collectors.toSet());

        assertEquals(PlayableClass.PALADIN, registry.getPlayableClass());
        assertEquals("PaladinSkillTreeRegistry", registry.getRegistryName());
        assertEquals(paladinRegistrySkillIds, providerSkillIds);
        assertEquals(24, registry.allSkills().size());
        assertEquals("Zenit", registry.requireSkill("zenit").getSkillName());
    }

    @Test
    void ranking_powinien_pokazywac_niepoliczalne_umiejetnosci_bez_wynikow_dps() {
        List<PaladinSkillDamageRankingEntry> ranking = service.rankDamageSkills(PaladinDamageRankingMetric.SINGLE_TARGET_DPS);

        assertFalse(ranking.isEmpty());
        assertTrue(ranking.stream().anyMatch(entry -> entry.getVerificationStatus() == PaladinSkillDamageVerificationStatus.NEEDS_VERIFICATION));
        assertTrue(ranking.stream().allMatch(entry -> entry.getDamagePerUse() == null));
        assertTrue(ranking.stream().allMatch(entry -> entry.getEffectiveCycleSeconds() == null));
        assertTrue(ranking.stream().allMatch(entry -> entry.getTheoreticalDps() == null));
    }

    @Test
    void ranking_paladyna_powinien_pokazywac_tylko_jawnie_zweryfikowane_bazowe_procenty_obrazen() {
        List<PaladinSkillDamageRankingEntry> entries = service.describeTreeSkills(PlayableClass.PALADIN);

        assertEquals(24, entries.size());
        PaladinSkillDamageRankingEntry blessedHammer = entries.stream()
                .filter(entry -> entry.getSkillId().equals("blogoslawiony_mlot"))
                .findFirst()
                .orElseThrow();
        assertEquals(115, blessedHammer.getBaseDamagePercentAtRank1());
        assertEquals(293, blessedHammer.getBaseDamagePercentAtTreeMaxRank());
        assertEquals(Set.of("blogoslawiony_mlot"), entries.stream()
                .filter(entry -> entry.getBaseDamagePercentAtRank1() != null)
                .map(PaladinSkillDamageRankingEntry::getSkillId)
                .collect(Collectors.toSet()));
        assertEquals(Set.of("blogoslawiony_mlot"), entries.stream()
                .filter(entry -> entry.getBaseDamagePercentAtTreeMaxRank() != null)
                .map(PaladinSkillDamageRankingEntry::getSkillId)
                .collect(Collectors.toSet()));
        assertTrue(entries.stream().allMatch(entry -> entry.getDamagePerUse() == null));
        assertTrue(entries.stream().allMatch(entry -> entry.getTheoreticalDps() == null));
    }

    @Test
    void sortowanie_po_bazowych_procentach_obrazen_powinno_sortowac_wartosci_jawne_przed_brakiem_danych() {
        DamageRankingService fixtureService = new DamageRankingService(new DamageEngine(), new SkillTreeRegistryProvider(List.of(
                new CharacterSkillTreeRegistry(PlayableClass.PALADIN, "TestSkillTreeRegistry", List.of(
                        fixtureSkill("skill_a", "Skill A", 100, 300),
                        fixtureSkill("skill_b", "Skill B", 150, 250),
                        fixtureSkill("skill_c", "Skill C", null, null)
                ))
        )));

        List<String> rankOneOrder = fixtureService.rankDamageSkills(PlayableClass.PALADIN, PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_RANK_1)
                .stream()
                .map(PaladinSkillDamageRankingEntry::getSkillId)
                .toList();
        List<String> treeMaxOrder = fixtureService.rankDamageSkills(PlayableClass.PALADIN, PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX)
                .stream()
                .map(PaladinSkillDamageRankingEntry::getSkillId)
                .toList();

        assertEquals(List.of("skill_b", "skill_a", "skill_c"), rankOneOrder);
        assertEquals(List.of("skill_a", "skill_b", "skill_c"), treeMaxOrder);
    }

    @Test
    void sortowanie_po_bazowych_procentach_dla_rejestru_paladyna_powinno_wynosic_blogoslawiony_mlot_nad_brak_danych() {
        List<String> rankOneOrder = service.rankDamageSkills(PlayableClass.PALADIN, PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_RANK_1)
                .stream()
                .map(PaladinSkillDamageRankingEntry::getSkillId)
                .toList();
        List<String> treeMaxOrder = service.rankDamageSkills(PlayableClass.PALADIN, PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX)
                .stream()
                .map(PaladinSkillDamageRankingEntry::getSkillId)
                .toList();

        assertEquals("blogoslawiony_mlot", rankOneOrder.get(0));
        assertEquals("blogoslawiony_mlot", treeMaxOrder.get(0));
    }

    @Test
    void mechaniki_wymagajace_weryfikacji_nie_sa_oznaczane_jako_supported() {
        PaladinSkillDamageRankingEntry gatedEntry = service.rankDamageSkills(PaladinDamageRankingMetric.DAMAGE_PER_USE).stream()
                .filter(entry -> entry.getSkillId().equals("wlocznia_niebios"))
                .findFirst()
                .orElseThrow();

        assertEquals(PaladinSkillDamageVerificationStatus.NEEDS_VERIFICATION, gatedEntry.getVerificationStatus());
        assertNotEquals(PaladinSkillDamageVerificationStatus.SUPPORTED, gatedEntry.getVerificationStatus());
    }

    @Test
    void wynik_rankingu_zachowuje_source_pdf_i_skill_group_z_nowego_rejestru() {
        PaladinSkillDamageRankingEntry entry = service.rankDamageSkills(PaladinDamageRankingMetric.SINGLE_TARGET_DPS)
                .stream()
                .filter(rankingEntry -> rankingEntry.getSkillId().equals("furia_niebios"))
                .findFirst()
                .orElseThrow();

        assertEquals("docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf", entry.getSourcePdf());
        assertEquals("moce_specjalne", entry.getSkillGroup());
    }

    @Test
    void legacy_foundation_ranking_pozostaje_test_only_i_sortuje_malejaco_po_damage_per_use() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(List.of(
                new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.RIGHT),
                new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE),
                new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)
        ), List.of(SkillId.BRANDISH, SkillId.HOLY_BOLT, SkillId.ADVANCE));

        List<PaladinSkillDamageRankingEntry> ranking = service.rankLegacyFoundationDamageSkills(snapshot, PaladinDamageRankingMetric.DAMAGE_PER_USE);

        assertFalse(ranking.isEmpty());
        for (int i = 1; i < ranking.size(); i++) {
            assertTrue(ranking.get(i - 1).getDamagePerUse() >= ranking.get(i).getDamagePerUse());
        }
        assertTrue(ranking.stream().allMatch(entry -> entry.getNotes().startsWith("Legacy/test-only")));
    }

    @Test
    void legacy_non_damage_nie_trafia_do_legacy_rankingu_obrazen() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(List.of(
                new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT),
                new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)
        ), List.of(SkillId.CLASH, SkillId.BRANDISH));

        List<PaladinSkillDamageRankingEntry> ranking = service.rankLegacyFoundationDamageSkills(snapshot, PaladinDamageRankingMetric.DAMAGE_PER_USE);
        Set<String> rankedSkillIds = ranking.stream()
                .map(PaladinSkillDamageRankingEntry::getSkillId)
                .collect(Collectors.toSet());

        assertFalse(rankedSkillIds.contains(SkillId.CLASH.name()));
        PaladinSkillDamageRankingEntry clashDescription = service.describeLegacyConfiguredFoundationSkills(snapshot).stream()
                .filter(entry -> entry.getSkillId().equals(SkillId.CLASH.name()))
                .findFirst()
                .orElseThrow();
        assertEquals(PaladinSkillDamageVerificationStatus.NON_DAMAGE, clashDescription.getVerificationStatus());
    }

    @Test
    void legacy_single_target_nie_dolicza_efektow_wielocelowych_do_pojedynczego_celu() {
        SkillState brandishState = new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.RIGHT);
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(brandishState);

        PaladinSkillDamageRankingEntry entry = service.rankLegacyFoundationDamageSkills(snapshot, PaladinDamageRankingMetric.DAMAGE_PER_USE)
                .stream()
                .filter(rankingEntry -> rankingEntry.getSkillId().equals(SkillId.BRANDISH.name()))
                .findFirst()
                .orElseThrow();
        DamageBreakdown breakdown = damageEngine.calculate(snapshot, SkillId.BRANDISH, EnumSet.noneOf(krys.skill.StatusId.class));
        long sumIncludedActiveComponents = breakdown.getComponents().stream()
                .filter(DamageComponentBreakdown::isActive)
                .filter(DamageComponentBreakdown::isIncludedInSingleTarget)
                .mapToLong(DamageComponentBreakdown::getFinalDamage)
                .sum();

        assertEquals(breakdown.getFinalDamage(), entry.getDamagePerUse());
        assertEquals(sumIncludedActiveComponents, entry.getDamagePerUse());
        assertTrue(breakdown.getComponents().stream().anyMatch(component -> !component.isIncludedInSingleTarget()));
        assertEquals(PaladinDamageTargetMode.SINGLE_TARGET, entry.getTargetMode());
    }

    private static PaladinTreeSkill fixtureSkill(String skillId,
                                                 String skillName,
                                                 Integer baseDamagePercentAtRank1,
                                                 Integer baseDamagePercentAtTreeMaxRank) {
        return new PaladinTreeSkill(
                skillId,
                skillName,
                "test.pdf",
                "test",
                baseDamagePercentAtRank1,
                baseDamagePercentAtTreeMaxRank,
                PaladinSkillTreeType.DAMAGE,
                PaladinSkillTreeStatus.NEEDS_VERIFICATION,
                List.of(),
                "Fixture sortowania bazowych procentów obrażeń."
        );
    }
}
