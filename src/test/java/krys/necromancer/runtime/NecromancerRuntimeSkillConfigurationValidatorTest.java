package krys.necromancer.runtime;

import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.necromancer.combat.NecromancerSeason15DirectDamageProfile;
import krys.necromancer.combat.NecromancerSeason15DirectDamageResolver;
import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerRuntimeSkillConfigurationValidatorTest {
    private final NecromancerRuntimeSkillConfigurationValidator validator = new NecromancerRuntimeSkillConfigurationValidator();

    @Test
    void hemorrhageShouldAcceptExactSelectionsAndAnyPartialGroupSet() {
        assertDoesNotThrow(() -> validator.validate(config("krwotok", 1, Map.of())));
        assertDoesNotThrow(() -> validator.validate(config("krwotok", 1, Map.of("grupa_1", "szybkosc_uzycia"))));
        assertDoesNotThrow(() -> validator.validate(config("krwotok", 1, Map.of("grupa_2", "oslabienie"))));
        assertDoesNotThrow(() -> validator.validate(config("krwotok", 1,
                Map.of("grupa_1", "szybkosc_uzycia", "grupa_2", "przytloczenie"))));
        assertDoesNotThrow(() -> validator.validate(config("krwotok", 1,
                Map.of("grupa_1", "szybkosc_uzycia", "grupa_2", "przytloczenie", "grupa_3", "krwawy_wrzod"))));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 14, 15})
    void catalogRankValidationShouldNotRequireKnownDamageCoefficient(int rank) {
        assertDoesNotThrow(() -> validator.validate(config("krwotok", rank, Map.of())));
    }

    @Test
    void crossGroupUnknownGroupAndUnknownOptionShouldBeRejected() {
        for (var selections : java.util.List.of(Map.of("grupa_1", "przytloczenie"),
                Map.of("grupa_4", "something"), Map.of("grupa_3", "unknown"), Map.of("legacy_base_upgrade", "enabled"))) {
            assertThrows(IllegalArgumentException.class, () -> validator.validate(config("krwotok", 1, selections)));
        }
    }

    @Test
    void wrongClassUnknownSkillAndOutOfCatalogRankShouldBeRejected() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
                new RuntimeSkillConfiguration(new RuntimeSkillKey("paladin", "krwotok"), 1, Map.of())));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config("unknown", 1, Map.of())));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config("krwotok", 16, Map.of())));
        assertThrows(NullPointerException.class, () -> validator.validate(null));
    }

    @Test
    void all23CatalogSkillsShouldBeRepresentableWithAndWithoutSelections() {
        var skills = NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills();
        assertEquals(23, skills.size());
        for (var skill : skills) {
            assertDoesNotThrow(() -> validator.validate(config(skill.getSkillId(), 1, Map.of())), skill.getSkillId());
            var selections = new HashMap<String, String>();
            for (var group : skill.getUpgradeGroups()) {
                assertFalse(group.getUpgrades().isEmpty());
                selections.put(group.getId(), group.getUpgrades().getFirst().getId());
            }
            assertEquals(skill.getUpgradeGroups().size(), selections.size());
            assertDoesNotThrow(() -> validator.validate(config(skill.getSkillId(), 1, selections)), skill.getSkillId());
        }
    }

    @Test
    void catalogValidityShouldNotEnableDamageSupportForOtherSkillsOrUpgrades() {
        var resolver = new NecromancerSeason15DirectDamageResolver();
        var profile = new NecromancerSeason15DirectDamageProfile(1000, 1000, 0, 0, 1, 0, 0, 0, 0, false);
        for (var configuration : java.util.List.of(config("kosciane_odlamki", 1, Map.of()),
                config("krwotok", 1, Map.of("grupa_1", "szybkosc_uzycia")), config("krwotok", 1,
                Map.of("grupa_1", "szybkosc_uzycia", "grupa_2", "przytloczenie", "grupa_3", "krwawy_wrzod")))) {
            assertDoesNotThrow(() -> validator.validate(configuration));
            assertThrows(UnsupportedOperationException.class, () -> resolver.calculate(configuration, profile));
        }
    }

    private static RuntimeSkillConfiguration config(String id, int rank, Map<String, String> selections) {
        return new RuntimeSkillConfiguration(new RuntimeSkillKey("necromancer", id), rank, selections);
    }
}
