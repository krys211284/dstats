package krys.necromancer;

import krys.hero.HeroClass;
import krys.ranking.DamageRankingMetric;
import krys.ranking.DamageRankingService;
import krys.ranking.DamageTargetMode;
import krys.ranking.PlayableClass;
import krys.ranking.SkillDamageVerificationStatus;
import krys.ranking.SkillTreeRegistryProvider;
import krys.skill.SkillId;
import krys.skilltree.CharacterTreeSkill;
import krys.skilltree.SkillResourceGenerationFrequency;
import krys.skilltree.SkillTreeStatus;
import krys.skilltree.SkillTreeType;
import krys.skilltree.SkillUpgrade;
import krys.skilltree.UpgradeDamageModifier;
import krys.skilltree.UpgradeDamageModifierType;
import krys.skilltree.UpgradeDamageSafety;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerSkillTreeRegistryTest {
    @Test
    void shouldExposeExactlyFourSourceSkillsAndTwentyEightUpgrades() {
        var registry = NecromancerSkillTreeRegistry.asCharacterRegistry();
        assertEquals(PlayableClass.NECROMANCER, registry.getPlayableClass());
        assertEquals(List.of("Kościane Odłamki", "Żniwa", "Rozkład", "Krwotok"),
                basicSkills().stream().map(CharacterTreeSkill::getSkillName).toList());
        assertEquals("Podstawowe / Basic", registry.treeGroupDisplayName("basic"));
        assertEquals(NecromancerSkillTreeRegistry.ESSENCE, registry.getResourceType());
        assertEquals("essence", registry.getResourceType().id());
        assertEquals("Koszt Esencji", registry.getResourceType().costLabel());
        assertEquals("Generowanie Esencji", registry.getResourceType().generationLabel());
        assertEquals("pkt. esencji", registry.getResourceType().unitLabel());
        for (var skill : basicSkills()) {
            assertEquals("basic", skill.getSkillGroup());
            assertEquals(SkillTreeType.DAMAGE, skill.getType());
            assertEquals(SkillTreeStatus.NEEDS_VERIFICATION, skill.getStatus());
            assertEquals(Set.of(NecromancerSkillTreeRegistry.BASIC), skill.getSkillCategories());
            assertEquals(List.of("grupa_1", "grupa_2", "grupa_3"), skill.getUpgradeGroups().stream().map(group -> group.getId()).toList());
            assertEquals(List.of(2, 2, 3), skill.getUpgradeGroups().stream().map(group -> group.getUpgrades().size()).toList());
            assertEquals(7, upgrades(skill).size());
            assertEquals(7, skill.getUpgradeDamageImpacts().size());
            assertTrue(skill.getTags().contains("BASIC"));
            assertTrue(skill.getSourceReference().startsWith("https://d4guides.gg/en/database/skills/"));
            assertTrue(skill.getNotes().contains("bez jawnej etykiety Rank 15"));
            assertTrue(skill.getNotes().contains(NecromancerSkillTreeRegistry.MAX_VIEW_SOURCE));
        }
        assertEquals(28, basicSkills().stream().mapToInt(skill -> upgrades(skill).size()).sum());
        assertEquals("Podstawowe", NecromancerSkillTreeRegistry.BASIC.displayName());
        assertEquals(0, NecromancerSkillTreeRegistry.BASIC.displayOrder());
    }

    @Test
    void shouldPreserveSourceRankEndpointsAndLuckyHitWithoutInventingIntermediateRanks() {
        // Golden values: baseline N1 / D4Guides R1 i bieżący max-view D4Builds, opisane w docs/necromancer/basic-source-data.md.
        Map<String, List<Integer>> golden = Map.of(
                "kosciane_odlamki", List.of(30, 77, 30), "zniwa", List.of(50, 128, 30),
                "rozklad", List.of(150, 383, 40), "krwotok", List.of(80, 204, 50));
        for (var skill : basicSkills()) {
            var values = golden.get(skill.getSkillId());
            assertEquals(values.get(0), skill.getBaseDamagePercentAtRank1());
            assertEquals(values.get(1), skill.getBaseDamagePercentAtTreeMaxRank());
            assertEquals(values.get(2), skill.getLuckyHitPercent());
            assertEquals(Map.of(1, values.get(0), 15, values.get(1)), skill.getBaseDamagePercentRanks().asMap());
            for (int rank = 2; rank < 15; rank++) {
                assertNull(skill.getBaseDamagePercentRanks().damagePercentAtRank(rank));
            }
        }
    }

    @Test
    void shouldKeepEssenceRangeFrequencyAndConditionalBonusesSeparate() {
        var registry = NecromancerSkillTreeRegistry.asCharacterRegistry();
        for (var expected : Map.of("kosciane_odlamki", 10, "zniwa", 15, "krwotok", 13).entrySet()) {
            var data = registry.requireSkill(expected.getKey()).getResourceData();
            assertEquals(BigDecimal.valueOf(expected.getValue()), data.generationMin());
            assertEquals(data.generationMin(), data.generationMax());
            assertEquals(SkillResourceGenerationFrequency.PER_USE, data.generationFrequency());
            assertNull(data.cost());
        }
        var decompose = registry.requireSkill("rozklad").getResourceData();
        assertEquals(new BigDecimal("12"), decompose.generationMin());
        assertEquals(new BigDecimal("16"), decompose.generationMax());
        assertEquals(SkillResourceGenerationFrequency.PER_SECOND, decompose.generationFrequency());
        assertNull(decompose.generationBonusKnown());
        assertEquals(new BigDecimal("5"), registry.requireSkill("kosciane_odlamki").getResourceData().generationBonusKnown());
        assertNull(registry.requireSkill("zniwa").getResourceData().generationBonusKnown());
        assertTrue(modifier("zniwa", "Generowanie Esencji").getCondition().contains("każdego trafionego wroga"));
        assertTrue(registry.requireSkill("zniwa").getUpgradeDamageModifiers().stream().anyMatch(modifier ->
                modifier.getUpgradeName().equals("Dorzynki") && modifier.getValue().equals("7.5 pkt. esencji")
                        && modifier.getType() == UpgradeDamageModifierType.RESOURCE_OR_COST));
    }

    @Test
    void shouldPreserveAllPolishUpgradeNamesAndTheirSourceComponents() {
        assertEquals(List.of("Pociski", "Generowanie Esencji", "Odsłonięcie", "Animusz", "Rykoszetujące Kolce", "Krwawe Odłamki", "Poszukiwacze Cienia"), names("kosciane_odlamki"));
        assertEquals(List.of("Szansa na Trafienie Krytyczne", "Generowanie Esencji", "Zajadłość", "Generowanie Zwłok", "Przejmujący Chłód", "Selekcja Naturalna", "Dorzynki"), names("zniwa"));
        assertEquals(List.of("Premia do Obrażeń", "Bariera", "Ograniczenie Kontroli", "Szansa na Szczęśliwy Traf", "Targanie", "Gnijący Rozprysk", "Próchnienie"), names("rozklad"));
        assertEquals(List.of("Szybkość Użycia", "Krwawa Kula", "Przytłoczenie", "Osłabienie", "Krwawy Wrzód", "Zmrożenie Krwi", "Rozdarcie Dusz"), names("krwotok"));
        assertEquals("7.5%", modifier("kosciane_odlamki", "Poszukiwacze Cienia").getValue());
        assertEquals("62.5%", modifier("zniwa", "Selekcja Naturalna").getValue());
        assertEquals("45%", modifier("kosciane_odlamki", "Rykoszetujące Kolce").getValue());
        assertEquals("25%", modifier("zniwa", "Dorzynki").getValue());
        assertEquals("113%", modifier("rozklad", "Gnijący Rozprysk").getValue());
        assertEquals("40%", modifier("krwotok", "Krwawy Wrzód").getValue());
        assertEquals("120%", modifier("krwotok", "Rozdarcie Dusz").getValue());
        assertTrue(modifier("zniwa", "Selekcja Naturalna").getNotes().contains("63%"));
        assertTrue(modifier("zniwa", "Dorzynki").getNotes().contains("zaokrąglone 8 Esencji"));
    }

    @Test
    void shouldKeepTheSpecificLuckyHitConflictVisibleAndAllModifiersOutsideRuntime() {
        var conflict = modifier("rozklad", "Szansa na Szczęśliwy Traf");
        assertEquals("+30%", conflict.getValue());
        assertEquals(UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, conflict.getSafeForRankingDisplay());
        assertEquals(UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW, conflict.getType());
        assertTrue(conflict.getNotes().contains("Konflikt źródeł"));
        assertTrue(conflict.getNotes().contains("changelogu 3.2.1"));
        assertEquals("10%[+]", modifier("zniwa", "Szansa na Trafienie Krytyczne").getValue());
        assertTrue(modifier("zniwa", "Szansa na Trafienie Krytyczne").getNotes().contains("+20%"));
        for (var skill : basicSkills()) {
            assertTrue(skill.getUpgradeDamageModifiers().stream().allMatch(modifier -> modifier.getSafeForRuntimeDps() == UpgradeDamageSafety.NO));
            assertTrue(skill.getUpgradeDamageModifiers().stream().filter(modifier -> modifier != conflict)
                    .allMatch(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES));
        }
    }

    @Test
    void productionProviderShouldResolveBothClassesAndNeverFallbackFromNecromancer() {
        var provider = SkillTreeRegistryProvider.production();
        assertEquals(List.of(PlayableClass.PALADIN, PlayableClass.NECROMANCER), provider.supportedClasses());
        assertEquals(24, provider.registryFor(PlayableClass.PALADIN).allSkills().size());
        assertEquals(23, provider.registryFor(PlayableClass.NECROMANCER).allSkills().size());
        assertEquals(PlayableClass.PALADIN, provider.registryFor(null).getPlayableClass());
        assertTrue(provider.registryFor(PlayableClass.NECROMANCER).findSkill("starcie").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> SkillTreeRegistryProvider.paladinOnly().registryFor(PlayableClass.NECROMANCER));
        assertEquals(PlayableClass.NECROMANCER, PlayableClass.fromQueryValue("necromancer").orElseThrow());
    }

    @Test
    void necromancerRankingShouldWorkWithoutAnEngineAndWithoutRuntimeIdentifiers() {
        // Brak instancji silnika sprawia, że każde niezamierzone wywołanie legacy engine przerwałoby test.
        var service = new DamageRankingService(null, SkillTreeRegistryProvider.production());
        var descriptions = service.describeTreeSkills(PlayableClass.NECROMANCER);
        assertEquals(23, descriptions.size());
        for (var entry : descriptions) {
            assertNull(entry.getDamagePerUse());
            assertEquals(NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill(entry.getSkillId()).getCooldownSeconds(),
                    entry.getCooldownSeconds());
            assertNull(entry.getEffectiveCycleSeconds());
            assertNull(entry.getTheoreticalDps());
            assertEquals(SkillDamageVerificationStatus.NEEDS_VERIFICATION, entry.getVerificationStatus());
            assertEquals(DamageTargetMode.SINGLE_TARGET, entry.getTargetMode());
            assertEquals(NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill(entry.getSkillId()).getSourceReference(), entry.getSourceReference());
        }
        for (var metric : DamageRankingMetric.values()) {
            assertEquals(23, service.rankDamageSkills(PlayableClass.NECROMANCER, metric).size());
        }
        assertEquals(List.of(HeroClass.PALADIN, HeroClass.NECROMANCER), List.of(HeroClass.values()));
        assertTrue(List.of(SkillId.values()).stream().noneMatch(id ->
                NecromancerSkillTreeRegistry.asCharacterRegistry().findSkill(id.name().toLowerCase(java.util.Locale.ROOT)).isPresent()));
    }

    private static List<CharacterTreeSkill> basicSkills() {
        return NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills().stream()
                .filter(skill -> skill.getSkillGroup().equals("basic")).toList();
    }

    private static List<SkillUpgrade> upgrades(CharacterTreeSkill skill) {
        return skill.getUpgradeGroups().stream().flatMap(group -> group.getUpgrades().stream()).toList();
    }

    private static List<String> names(String id) {
        return upgrades(NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill(id)).stream().map(SkillUpgrade::getName).toList();
    }

    private static UpgradeDamageModifier modifier(String id, String name) {
        return NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill(id).getUpgradeDamageModifiers().stream()
                .filter(modifier -> modifier.getUpgradeName().equals(name)).findFirst().orElseThrow();
    }
}
