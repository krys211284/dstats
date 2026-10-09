package krys.skilltree;

import krys.ranking.CharacterSkillTreeRegistry;
import krys.ranking.DamageRankingService;
import krys.ranking.PlayableClass;
import krys.ranking.SkillTreeRegistryProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CharacterTreeSkillMetadataTest {
    @Test
    void shouldAllowUnknownCooldownAndRejectZeroOrNegativeCooldown() {
        assertNull(skill(null, List.of()).getCooldownSeconds());
        // 11 s pochodzi z golden źródłowego odnowienia dostarczonego dla N3.
        assertEquals(11, skill(11, List.of()).getCooldownSeconds());
        assertThrows(IllegalArgumentException.class, () -> skill(0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> skill(-1, List.of()));
    }

    @Test
    void shouldDefensivelyCopyActivationRequirementsAndRejectNullEntries() {
        var requirement = new SkillActivationRequirement("test_object", "Obiekt testowy", 1,
                SkillActivationConsumptionMode.REQUIRED_NOT_CONSUMED);
        var mutable = new ArrayList<>(List.of(requirement));
        var skill = skill(null, mutable);
        mutable.clear();
        assertEquals(List.of(requirement), skill.getActivationRequirements());
        assertThrows(UnsupportedOperationException.class, () -> skill.getActivationRequirements().clear());
        assertThrows(NullPointerException.class, () -> skill(null, null));
        mutable.add(null);
        assertThrows(NullPointerException.class, () -> skill(null, mutable));
    }

    @Test
    void shouldPreserveEmptyDefaultsForEveryExistingCatalogSkillAndModifier() {
        var provider = SkillTreeRegistryProvider.production();
        assertEquals(24, provider.registryFor(PlayableClass.PALADIN).allSkills().size());
        int unchangedSkills = 0;
        for (var character : provider.supportedClasses()) {
            for (var skill : provider.registryFor(character).allSkills()) {
                if (Set.of("corpse", "macabre", "curse", "ultimate").contains(skill.getSkillGroup())) continue;
                unchangedSkills++;
                assertNull(skill.getCooldownSeconds(), skill.getSkillId());
                assertTrue(skill.getActivationRequirements().isEmpty(), skill.getSkillId());
                assertTrue(skill.getUpgradeDamageModifiers().stream()
                        .allMatch(modifier -> modifier.getComponentDamagePercentRanks().isEmpty()), skill.getSkillId());
            }
        }
        assertEquals(34, unchangedSkills); // 24 Paladyna + 10 Basic/Core Nekromanty.
        var simple = new CharacterTreeSkill("test", "Test", "Źródło", "test", null, null,
                SkillTreeType.DAMAGE, SkillTreeStatus.NEEDS_VERIFICATION, List.of(), "Opis");
        assertNull(simple.getCooldownSeconds());
        assertTrue(simple.getActivationRequirements().isEmpty());
    }

    @Test
    void shouldForwardSourceCooldownWithoutComputingAnyRuntimeMetric() {
        var registry = new CharacterSkillTreeRegistry(PlayableClass.PALADIN, "Neutralny test", List.of(skill(11, List.of())),
                new SkillResourceType("test", "Koszt testowy", "Generowanie testowe", "pkt."), Map.of("test", "Test"));
        var service = new DamageRankingService(null, new SkillTreeRegistryProvider(List.of(registry)));
        var entry = service.describeTreeSkills(PlayableClass.PALADIN).getFirst();
        assertEquals(11, entry.getCooldownSeconds());
        assertNull(entry.getDamagePerUse());
        assertNull(entry.getEffectiveCycleSeconds());
        assertNull(entry.getTheoreticalDps());
    }

    private static CharacterTreeSkill skill(Integer cooldown, List<SkillActivationRequirement> requirements) {
        return new CharacterTreeSkill("test", "Test", "Źródło", "test", null, null,
                DamagePercentRankTable.empty(), DamagePercentComponentRankTable.empty(), SkillTreeType.DAMAGE,
                SkillTreeStatus.NEEDS_VERIFICATION, List.of(), "Opis", Set.of(), Set.of(), SkillResourceData.EMPTY,
                null, List.of(), List.of(), cooldown, requirements);
    }
}
