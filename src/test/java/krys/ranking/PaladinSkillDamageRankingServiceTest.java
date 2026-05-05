package krys.ranking;

import krys.app.SampleBuildFactory;
import krys.combat.DamageBreakdown;
import krys.combat.DamageComponentBreakdown;
import krys.combat.DamageEngine;
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

class PaladinSkillDamageRankingServiceTest {
    private final DamageEngine damageEngine = new DamageEngine();
    private final PaladinSkillDamageRankingService service = new PaladinSkillDamageRankingService(damageEngine);

    @Test
    void powinien_sortowac_ranking_malejaco_po_damage_per_use() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(List.of(
                new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.RIGHT),
                new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE),
                new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)
        ), List.of(SkillId.BRANDISH, SkillId.HOLY_BOLT, SkillId.ADVANCE));

        List<PaladinSkillDamageRankingEntry> ranking = service.rankDamageSkills(snapshot, PaladinDamageRankingMetric.DAMAGE_PER_USE);

        assertFalse(ranking.isEmpty());
        for (int i = 1; i < ranking.size(); i++) {
            assertTrue(ranking.get(i - 1).getDamagePerUse() >= ranking.get(i).getDamagePerUse());
        }
    }

    @Test
    void non_damage_nie_trafia_do_domyslnego_rankingu_obrazen() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(List.of(
                new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT),
                new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)
        ), List.of(SkillId.CLASH, SkillId.BRANDISH));

        List<PaladinSkillDamageRankingEntry> ranking = service.rankDamageSkills(snapshot, PaladinDamageRankingMetric.DAMAGE_PER_USE);
        Set<String> rankedSkillIds = ranking.stream()
                .map(PaladinSkillDamageRankingEntry::getSkillId)
                .collect(Collectors.toSet());

        assertFalse(rankedSkillIds.contains(SkillId.CLASH.name()));
        PaladinSkillDamageRankingEntry clashDescription = service.describeConfiguredFoundationSkills(snapshot).stream()
                .filter(entry -> entry.getSkillId().equals(SkillId.CLASH.name()))
                .findFirst()
                .orElseThrow();
        assertEquals(PaladinSkillDamageVerificationStatus.NON_DAMAGE, clashDescription.getVerificationStatus());
    }

    @Test
    void mechaniki_wymagajace_weryfikacji_nie_sa_oznaczane_jako_verified() {
        PaladinSkillDamageRankingEntry gatedEntry = service.describeVerificationGatedMechanics().stream()
                .filter(entry -> entry.getSkillId().equals("szarza_z_tarcza"))
                .findFirst()
                .orElseThrow();

        assertEquals(PaladinSkillDamageVerificationStatus.NEEDS_VERIFICATION, gatedEntry.getVerificationStatus());
        assertNotEquals(PaladinSkillDamageVerificationStatus.VERIFIED, gatedEntry.getVerificationStatus());
    }

    @Test
    void single_target_nie_dolicza_efektow_wielocelowych_do_pojedynczego_celu() {
        SkillState brandishState = new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.RIGHT);
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(brandishState);

        PaladinSkillDamageRankingEntry entry = service.rankDamageSkills(snapshot, PaladinDamageRankingMetric.DAMAGE_PER_USE)
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

    @Test
    void wynik_rankingu_zachowuje_source_pdf_i_skill_group() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)
        );

        PaladinSkillDamageRankingEntry entry = service.rankDamageSkills(snapshot, PaladinDamageRankingMetric.SINGLE_TARGET_DPS)
                .getFirst();

        assertEquals("docs/paladin/source-pdfs/paladin_basic_skill_registry_final.pdf", entry.getSourcePdf());
        assertEquals("basic", entry.getSkillGroup());
    }
}
