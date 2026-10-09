package krys.paladin;

import krys.ranking.CharacterSkillTreeRegistry;
import krys.skilltree.CharacterTreeSkill;
import krys.skilltree.UpgradeDamageModifier;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PaladinSkillTreeAdapterTest {
    @Test
    void shouldPreserveAllTwentyFourSourceSkillsAndEverySharedMetadataField() {
        CharacterSkillTreeRegistry registry = PaladinSkillTreeRegistry.asCharacterRegistry();
        assertEquals(24, registry.allSkills().size());
        assertEquals(PaladinSkillTreeAdapter.FAITH, registry.getResourceType());
        assertEquals(PaladinSkillTreeRegistry.allSkills().stream().map(PaladinTreeSkill::getSkillId).toList(),
                registry.allSkills().stream().map(CharacterTreeSkill::getSkillId).toList());
        for (PaladinTreeSkill source : PaladinSkillTreeRegistry.allSkills()) {
            CharacterTreeSkill shared = registry.requireSkill(source.getSkillId());
            assertEquals(source.getSkillName(), shared.getSkillName());
            assertEquals(source.getSourcePdf(), shared.getSourceReference());
            assertEquals(source.getSkillGroup(), shared.getSkillGroup());
            assertEquals(source.getNotes(), shared.getNotes());
            assertEquals(source.getType().name(), shared.getType().name());
            assertEquals(source.getStatus().name(), shared.getStatus().name());
            assertEquals(source.getBaseDamagePercentAtRank1(), shared.getBaseDamagePercentAtRank1());
            assertEquals(source.getBaseDamagePercentAtTreeMaxRank(), shared.getBaseDamagePercentAtTreeMaxRank());
            assertSame(source.getBaseDamagePercentRanks(), shared.getBaseDamagePercentRanks());
            assertSame(source.getComponentDamagePercentRanks(), shared.getComponentDamagePercentRanks());
            assertEquals(decimal(source.getFaithCost()), shared.getResourceData().cost());
            assertEquals(decimal(source.getFaithGenerationBase()), shared.getResourceData().generationMin());
            assertEquals(decimal(source.getFaithGenerationBonusKnown()), shared.getResourceData().generationBonusKnown());
            assertEquals(source.getLuckyHitPercent(), shared.getLuckyHitPercent());
            assertEquals(source.getTags().stream().map(Enum::name).collect(Collectors.toSet()), shared.getTags());
            assertEquals(source.getSkillCategories().stream().map(Enum::name).collect(Collectors.toSet()),
                    shared.getSkillCategories().stream().map(category -> category.getId()).collect(Collectors.toSet()));
            for (SkillCategory category : source.getSkillCategories()) {
                var mapped = shared.getSkillCategories().stream().filter(value -> value.getId().equals(category.name())).findFirst().orElseThrow();
                assertEquals(category.getDisplayName(), mapped.getDisplayName());
                assertEquals(category.getDisplayOrder(), mapped.getDisplayOrder());
            }
            assertEquals(source.getUpgradeGroups().size(), shared.getUpgradeGroups().size());
            for (int index = 0; index < source.getUpgradeGroups().size(); index++) {
                var originalGroup = source.getUpgradeGroups().get(index);
                var mappedGroup = shared.getUpgradeGroups().get(index);
                assertEquals(originalGroup.getId(), mappedGroup.getId());
                assertEquals(originalGroup.getName(), mappedGroup.getName());
                assertEquals(originalGroup.getUpgrades().size(), mappedGroup.getUpgrades().size());
                for (int upgradeIndex = 0; upgradeIndex < originalGroup.getUpgrades().size(); upgradeIndex++) {
                    var originalUpgrade = originalGroup.getUpgrades().get(upgradeIndex);
                    var mappedUpgrade = mappedGroup.getUpgrades().get(upgradeIndex);
                    assertEquals(originalUpgrade.getId(), mappedUpgrade.getId());
                    assertEquals(originalUpgrade.getName(), mappedUpgrade.getName());
                    assertEquals(originalUpgrade.getStatus().name(), mappedUpgrade.getStatus().name());
                    assertEquals(originalUpgrade.getSourceNote(), mappedUpgrade.getSourceNote());
                }
            }
            assertEquals(source.getUpgradeDamageModifiers().stream().map(PaladinSkillTreeAdapterTest::modifierFields).toList(),
                    shared.getUpgradeDamageModifiers().stream().map(PaladinSkillTreeAdapterTest::modifierFields).toList());
            assertEquals(source.getUpgradeDamageImpacts().stream().map(impact -> java.util.Arrays.asList(
                            impact.getUpgradeName(), impact.getGroupId(), impact.getType(), impact.getDescription(), impact.getDamagePercent())).toList(),
                    shared.getUpgradeDamageImpacts().stream().map(impact -> java.util.Arrays.asList(
                            impact.getUpgradeName(), impact.getGroupId(), impact.getType(), impact.getDescription(), impact.getDamagePercent())).toList());
        }
        assertTrue(Set.of(SkillCategory.values()).stream().allMatch(category -> registry.supportsSourceCategory(category.name())));
        assertTrue(Set.of(SkillTag.values()).stream().allMatch(tag -> registry.supportsMechanicTag(tag.name())));
    }

    private static java.math.BigDecimal decimal(Integer value) {
        return value == null ? null : java.math.BigDecimal.valueOf(value);
    }

    private static java.util.List<?> modifierFields(UpgradeDamageModifier modifier) {
        return java.util.List.of(modifier.getUpgradeGroup(), modifier.getUpgradeName(), modifier.getUpgradeHasDamageModifier(),
                modifier.getType(), modifier.getValue(), modifier.getValueKind(), modifier.getCondition(),
                modifier.createsNewDamageComponent(), modifier.getAffectedComponent(), modifier.scalesWithSkillRank(),
                modifier.getSafeForRankingDisplay(), modifier.getSafeForRuntimeDps(), modifier.getNotes(),
                modifier.getRankingTooltipSourceLabel(), modifier.getRankingTooltipDescription());
    }
}
