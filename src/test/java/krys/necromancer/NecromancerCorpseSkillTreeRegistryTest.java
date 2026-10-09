package krys.necromancer;

import krys.ranking.DamageRankingMetric;
import krys.ranking.DamageRankingService;
import krys.ranking.PlayableClass;
import krys.skilltree.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static krys.skilltree.DamagePercentComponent.*;
import static org.junit.jupiter.api.Assertions.*;

class NecromancerCorpseSkillTreeRegistryTest {
    @Test
    void shouldExposeThreeCorpseSkillsAndTwentyOneUpgradesAfterTheUnchangedBasicAndCore() {
        var registry = NecromancerSkillTreeRegistry.asCharacterRegistry();
        assertEquals(23, registry.allSkills().size());
        assertEquals(161, registry.allSkills().stream().flatMap(skill -> skill.getUpgradeGroups().stream())
                .mapToInt(group -> group.getUpgrades().size()).sum());
        var skills = registry.allSkills().stream().filter(skill -> skill.getSkillGroup().equals("corpse")).toList();
        assertEquals(List.of("wybuch_zwlok", "trupie_macki", "szkieletowy_wojownik"), skills.stream().map(CharacterTreeSkill::getSkillId).toList());
        assertEquals("Zwłoki / Corpse", registry.treeGroupDisplayName("corpse"));
        assertEquals(new SourceSkillCategory("CORPSE", "Zwłoki", 2), NecromancerSkillTreeRegistry.CORPSE);
        for (var skill : skills) {
            assertEquals(Set.of(NecromancerSkillTreeRegistry.CORPSE), skill.getSkillCategories());
            assertEquals(SkillTreeType.DAMAGE, skill.getType());
            assertEquals(SkillTreeStatus.NEEDS_VERIFICATION, skill.getStatus());
            assertEquals(List.of(2, 2, 3), skill.getUpgradeGroups().stream().map(group -> group.getUpgrades().size()).toList());
            assertEquals(7, skill.getUpgradeDamageImpacts().size());
            assertTrue(skill.getUpgradeDamageModifiers().stream().allMatch(modifier -> modifier.getSafeForRuntimeDps() == UpgradeDamageSafety.NO));
            assertTrue(skill.getNotes().contains("bez jawnej etykiety Rank 15"));
        }
    }

    // Golden N3: dane bazowe z D4Guides / Icy Veins, max-view z głównej domeny D4Builds.
    @ParameterizedTest
    @CsvSource({"wybuch_zwlok,180,459,25", "trupie_macki,20,98,30", "szkieletowy_wojownik,65,166,20"})
    void shouldKeepSparseBaseTablesAndLuckyHit(String id, int rankOne, int maxView, int luckyHit) {
        var skill = skill(id);
        assertEquals(Map.of(1, rankOne, 15, maxView), skill.getBaseDamagePercentRanks().asMap());
        assertEquals(rankOne, skill.getBaseDamagePercentAtRank1());
        assertEquals(maxView, skill.getBaseDamagePercentAtTreeMaxRank());
        assertEquals(luckyHit, skill.getLuckyHitPercent());
        assertTrue(skill.getComponentDamagePercentRanks().isEmpty());
        for (int rank = 2; rank <= 14; rank++) assertNull(skill.getBaseDamagePercentRanks().damagePercentAtRank(rank));
    }

    @Test
    void shouldSeparateConsumedCorpsesFromEssenceAndFromTheActiveWarriorCommand() {
        var requirement = new SkillActivationRequirement("corpse", "Zwłoki", 1, SkillActivationConsumptionMode.CONSUMED_ON_USE);
        for (String id : List.of("wybuch_zwlok", "trupie_macki")) {
            assertEquals(List.of(requirement), skill(id).getActivationRequirements());
            assertEquals(Set.of("CORPSE", "PROFANE", "PHYSICAL"), skill(id).getTags());
            assertNull(skill(id).getResourceData().cost());
            assertNull(skill(id).getResourceData().generationMin());
            assertNull(skill(id).getResourceData().generationMax());
            assertNull(skill(id).getResourceData().generationBonusKnown());
        }
        assertEquals("+15 pkt. esencji", modifier("wybuch_zwlok", "Generowanie Esencji").getValue());
        assertEquals("+35 pkt. esencji", modifier("trupie_macki", "Generowanie Esencji").getValue());
        assertEquals(UpgradeDamageModifierType.RESOURCE_OR_COST, modifier("wybuch_zwlok", "Wiele Zwłok").getType());
        assertEquals(UpgradeDamageModifierType.RESOURCE_OR_COST, modifier("wybuch_zwlok", "Wydajne Zwłoki").getType());
        assertEquals("Zwłoki wymagane, ale niezużywane", modifier("trupie_macki", "Gorzkie Żniwa").getValue());
        assertEquals("Wariant nie wymaga Zwłok", modifier("trupie_macki", "Chodź Tu!").getValue());
        assertTrue(skill("szkieletowy_wojownik").getActivationRequirements().isEmpty());
        assertEquals(Set.of("CORPSE", "MINION", "SUMMON"), skill("szkieletowy_wojownik").getTags());
        assertTrue(skill("szkieletowy_wojownik").getNotes().contains("pasywnie: co 2 s"));
        assertTrue(skill("szkieletowy_wojownik").getNotes().contains("Aktywnie: rozkazuje istniejącym"));
        assertTrue(skill("szkieletowy_wojownik").getNotes().contains("Book of the Dead"));
    }

    @Test
    void shouldPreserveVariantTablesOutsideBaseDamageAndNeverSumShardsOrMinions() {
        assertVariant("wybuch_zwlok", "Miazmaty", DAMAGE_OVER_DURATION, 350, 893, 6, false);
        assertVariant("wybuch_zwlok", "Szrapnel", PRIMARY_DAMAGE, 180, 459, null, false);
        assertVariant("trupie_macki", "Szczęki Śmierci", PRIMARY_DAMAGE, 100, 255, null, false);
        assertVariant("szkieletowy_wojownik", "Służba i Ofiara", BURST_DAMAGE, 150, 383, null, true);
        assertEquals(UpgradeDamageModifierType.DAMAGE_OVER_TIME, modifier("wybuch_zwlok", "Miazmaty").getType());
        assertTrue(modifier("wybuch_zwlok", "Miazmaty").getCondition().contains("zastępuje"));
        assertTrue(modifier("wybuch_zwlok", "Szrapnel").getCondition().contains("zastępuje"));
        assertTrue(modifier("trupie_macki", "Szczęki Śmierci").getCondition().contains("zastępuje"));
        assertTrue(skill("wybuch_zwlok").getUpgradeDamageModifiers().stream().anyMatch(modifier -> modifier.getValue().equals("8 odłamków")));
        assertEquals(180, skill("wybuch_zwlok").getBaseDamagePercentAtRank1());
        assertEquals(20, skill("trupie_macki").getBaseDamagePercentAtRank1());
        assertEquals(65, skill("szkieletowy_wojownik").getBaseDamagePercentAtRank1());
        assertEquals("+3 wojowników", modifier("szkieletowy_wojownik", "Władca Marionetek").getValue());
        assertTrue(modifier("szkieletowy_wojownik", "Służba i Ofiara").getNotes().contains("2 s -> 0.5 s"));
        var litany = modifier("szkieletowy_wojownik", "Litania Śmierci");
        assertTrue(litany.scalesWithSkillRank());
        assertTrue(litany.getNotes().contains("max-view +21%[+]"));
        assertEquals(UpgradeDamageModifierType.STATUS_DAMAGE_ENABLER, litany.getType());
        assertNull(skill("szkieletowy_wojownik").getUpgradeDamageImpacts().stream()
                .filter(impact -> impact.getUpgradeName().equals("Litania Śmierci")).findFirst().orElseThrow().getDamagePercent());
    }

    @Test
    void shouldResolveBloodyMessFromOfficialPatchAndRetainTheUnresolvedTendrilsLuckyHitConflict() {
        var bloody = modifier("wybuch_zwlok", "Krwawa Miazga");
        assertEquals("75%[x]", bloody.getValue());
        assertEquals(UpgradeDamageSafety.YES, bloody.getSafeForRankingDisplay());
        assertTrue(bloody.getNotes().contains("Oficjalny patch 3.1"));
        assertTrue(bloody.getNotes().contains("nieaktualne 50%[x]"));
        var conflict = modifier("trupie_macki", "Szansa na Szczęśliwy Traf");
        assertEquals("+40%", conflict.getValue());
        assertEquals(UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW, conflict.getType());
        assertEquals(UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, conflict.getSafeForRankingDisplay());
        assertEquals("grupa_2", conflict.getUpgradeGroup());
        assertTrue(conflict.getNotes().contains("changelog EN D4Guides 3.2.1"));
        assertEquals(UpgradeDamageImpactType.NEEDS_VERIFICATION, skill("trupie_macki").getUpgradeDamageImpacts().stream()
                .filter(impact -> impact.getUpgradeName().equals(conflict.getUpgradeName())).findFirst().orElseThrow().getType());
        assertEquals(30, skill("trupie_macki").getLuckyHitPercent());
    }

    @Test
    void shouldForwardCooldownWithoutUnlockingRuntimeForAnyRankingMetric() {
        assertNull(skill("wybuch_zwlok").getCooldownSeconds());
        assertEquals(11, skill("trupie_macki").getCooldownSeconds());
        assertEquals(10, skill("szkieletowy_wojownik").getCooldownSeconds());
        var service = new DamageRankingService(null);
        for (var metric : DamageRankingMetric.values()) {
            var entries = service.rankDamageSkills(PlayableClass.NECROMANCER, metric);
            assertEquals(23, entries.size());
            for (var entry : entries) {
                assertEquals(skill(entry.getSkillId()).getCooldownSeconds(), entry.getCooldownSeconds());
                assertNull(entry.getDamagePerUse());
                assertNull(entry.getEffectiveCycleSeconds());
                assertNull(entry.getTheoreticalDps());
            }
        }
    }

    private static void assertVariant(String id, String name, DamagePercentComponent component, int rankOne,
                                      int maxView, Integer duration, boolean newComponent) {
        var modifier = modifier(id, name);
        var table = modifier.getComponentDamagePercentRanks();
        assertEquals(Set.of(component), table.asMap().keySet());
        assertEquals(Map.of(1, rankOne, 15, maxView), table.tableFor(component).asMap());
        assertEquals(duration, table.durationSecondsFor(component));
        assertEquals(newComponent, modifier.createsNewDamageComponent());
        for (int rank = 2; rank <= 14; rank++) assertNull(table.damagePercentAt(component, rank));
    }

    private static CharacterTreeSkill skill(String id) {
        return NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill(id);
    }

    private static UpgradeDamageModifier modifier(String id, String name) {
        return skill(id).getUpgradeDamageModifiers().stream().filter(modifier -> modifier.getUpgradeName().equals(name)).findFirst().orElseThrow();
    }
}
