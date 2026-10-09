package krys.ranking;

import krys.combat.DamageEngine;
import krys.skilltree.CharacterTreeSkill;
import krys.skilltree.DamagePercentComponentRankTable;
import krys.skilltree.DamagePercentRankTable;
import krys.skilltree.SkillResourceData;
import krys.skilltree.SkillResourceType;
import krys.skilltree.SkillTreeStatus;
import krys.skilltree.SkillTreeType;
import krys.skilltree.SkillUpgrade;
import krys.skilltree.SkillUpgradeGroup;
import krys.skilltree.SourceSkillCategory;
import krys.skilltree.UpgradeDamageImpact;
import krys.skilltree.UpgradeDamageModifier;
import krys.web.DamageRankingController;
import krys.web.DamageRankingFilter;
import krys.web.DamageRankingPageModel;
import krys.web.DamageRankingPageRenderer;
import krys.web.DamageRankingRow;
import krys.web.DamageRankingSearchText;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CharacterSkillTreeRegistryTest {
    @Test
    void shouldConstructRegistryAndRankNeutralSkillsWithoutPaladinModels() {
        CharacterSkillTreeRegistry registry = NeutralSkillTreeFixture.registry();
        SkillTreeRegistryProvider provider = new SkillTreeRegistryProvider(List.of(registry));
        DamageRankingService service = new DamageRankingService(new DamageEngine(), provider);

        assertSame(registry, provider.registryFor(null));
        assertSame(registry.allSkills().getFirst(), registry.requireSkill("test_direct"));
        assertEquals(NeutralSkillTreeFixture.RESOURCE, registry.getResourceType());
        assertEquals("Grupa testowa", registry.treeGroupDisplayName("test_group"));
        assertEquals("unknown_group", registry.treeGroupDisplayName("unknown_group"));
        assertTrue(registry.supportsSourceCategory(NeutralSkillTreeFixture.CATEGORY.getId()));
        assertTrue(registry.supportsMechanicTag(NeutralSkillTreeFixture.TAG));
        assertTrue(registry.findSkill("unknown").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> registry.requireSkill("unknown"));

        List<SkillDamageRankingEntry> descriptions = service.describeTreeSkills(PlayableClass.PALADIN);
        assertEquals(registry.allSkills().stream().map(CharacterTreeSkill::getSkillId).toList(),
                descriptions.stream().map(SkillDamageRankingEntry::getSkillId).toList());
        assertEquals(List.of(SkillDamageVerificationStatus.NEEDS_VERIFICATION,
                        SkillDamageVerificationStatus.UNSUPPORTED, SkillDamageVerificationStatus.NON_DAMAGE),
                descriptions.stream().map(SkillDamageRankingEntry::getVerificationStatus).toList());
        assertTrue(descriptions.stream().allMatch(entry -> entry.getDamagePerUse() == null
                && entry.getEffectiveCycleSeconds() == null && entry.getTheoreticalDps() == null));
        assertTrue(descriptions.stream().allMatch(entry -> entry.getTargetMode() == DamageTargetMode.SINGLE_TARGET));
        assertEquals(registry.requireSkill("test_direct").getBaseDamagePercentAtRank1(),
                descriptions.getFirst().getBaseDamagePercentAtRank1());
        assertEquals(registry.requireSkill("test_direct").getBaseDamagePercentAtTreeMaxRank(),
                descriptions.getFirst().getBaseDamagePercentAtTreeMaxRank());
        assertEquals(List.of("test_direct", "test_component"), service.rankDamageSkills(
                        PlayableClass.PALADIN, DamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX).stream()
                .map(SkillDamageRankingEntry::getSkillId).toList());
    }

    @Test
    void shouldDefensivelyCopyRegistryAndRejectDuplicateSkillIds() {
        CharacterTreeSkill skill = NeutralSkillTreeFixture.registry().requireSkill("test_direct");
        List<CharacterTreeSkill> input = new ArrayList<>(List.of(skill));
        CharacterSkillTreeRegistry registry = new CharacterSkillTreeRegistry(PlayableClass.PALADIN, "Test", input);
        input.clear();
        assertEquals(List.of(skill), registry.allSkills());
        assertThrows(UnsupportedOperationException.class, () -> registry.allSkills().clear());
        assertThrows(IllegalStateException.class, () -> new CharacterSkillTreeRegistry(
                PlayableClass.PALADIN, "Duplicate", List.of(skill, skill)));
        assertThrows(UnsupportedOperationException.class, () -> skill.getSkillCategories().clear());
        assertThrows(UnsupportedOperationException.class, () -> skill.getTags().clear());
        assertThrows(UnsupportedOperationException.class, () -> skill.getUpgradeGroups().clear());
        assertThrows(UnsupportedOperationException.class, () -> skill.getUpgradeDamageModifiers().clear());
    }

    @Test
    void generalTreeAndRankingClassesShouldHaveNoPaladinDomainDependency() throws IOException {
        for (Class<?> type : List.of(CharacterTreeSkill.class, SkillTreeType.class, SkillTreeStatus.class,
                SkillUpgrade.class, SkillUpgradeGroup.class, SourceSkillCategory.class, SkillResourceData.class,
                SkillResourceType.class, DamagePercentRankTable.class, DamagePercentComponentRankTable.class,
                UpgradeDamageImpact.class, UpgradeDamageModifier.class, CharacterSkillTreeRegistry.class,
                DamageRankingService.class, SkillDamageRankingEntry.class, SkillDamageModelType.class,
                SkillDamageVerificationStatus.class, DamageRankingMetric.class, DamageTargetMode.class,
                DamageRankingController.class, DamageRankingRow.class, DamageRankingPageModel.class,
                DamageRankingPageRenderer.class, DamageRankingFilter.class, DamageRankingSearchText.class)) {
            String bytecode = classBytes(type);
            assertFalse(bytecode.contains("krys/paladin/"), type.getName());
            assertFalse(bytecode.contains("krys/skill/Paladin"), type.getName());
            assertFalse(bytecode.contains("PaladinTreeSkill"), type.getName());
        }
        // Fabryka domyślnego providera może znać rejestr klasy, ale jego kontrakt nie zna modelu Paladyna.
        assertFalse(classBytes(SkillTreeRegistryProvider.class).contains("PaladinTreeSkill"));
    }

    private static String classBytes(Class<?> type) throws IOException {
        try (var stream = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
            assertNotNull(stream, type.getName());
            return new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
