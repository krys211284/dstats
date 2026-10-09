package krys.necromancer.app;

import krys.necromancer.combat.NecromancerSeason15DirectDamageProfile;
import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.runtime.RuntimeSkillLoadout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerCurrentBuildCalculationServiceTest {
    private final NecromancerCurrentBuildCalculationService service = new NecromancerCurrentBuildCalculationService();
    private final RuntimeSkillKey key = NecromancerCurrentBuildCalculationService.KRWOTOK;
    private final NecromancerSeason15DirectDamageProfile profile = new NecromancerSeason15DirectDamageProfile(1000, 1000, 0, 0, 1, 0, 0, 0, 0, false);

    @Test
    void shouldReadTheCanonicalConfigurationAndRetainProfileAndDirectResult() {
        var configuration = new RuntimeSkillConfiguration(key, 1, Map.of());
        var calculation = service.calculate(new RuntimeSkillLoadout("necromancer", Map.of(key, configuration), List.of(key)), profile);
        assertSame(configuration, calculation.configuration());
        assertSame(profile, calculation.profile());
        assertEquals(2100, calculation.result().finalDamage());
        assertEquals(3150, calculation.result().criticalDamage());
    }

    @Test
    void shouldRejectOtherClassesAndMissingKrwotok() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.calculate(new RuntimeSkillLoadout("paladin", Map.of(), List.of()), profile)).getMessage().contains("Nekromanty"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.calculate(new RuntimeSkillLoadout("necromancer", Map.of(), List.of()), profile)).getMessage().contains("Wybierz Krwotok R1"));
    }

    @Test
    void shouldRejectKrwotokOutsideActionBarIncludingUnlearnedRankZero() {
        for (int rank : List.of(0, 1)) {
            var loadout = new RuntimeSkillLoadout("necromancer", Map.of(key, new RuntimeSkillConfiguration(key, rank, Map.of())), List.of());
            assertEquals("Krwotok musi znajdować się na pasku akcji.", assertThrows(IllegalArgumentException.class, () -> service.calculate(loadout, profile)).getMessage());
        }
    }

    @Test
    void shouldDelegateUnsupportedRankAndSelectionsToN8BWithoutRepair() {
        for (var configuration : List.of(new RuntimeSkillConfiguration(key, 2, Map.of()),
                new RuntimeSkillConfiguration(key, 1, Map.of("grupa_1", "szybkosc_uzycia")))) {
            var loadout = new RuntimeSkillLoadout("necromancer", Map.of(key, configuration), List.of(key));
            assertTrue(assertThrows(UnsupportedOperationException.class, () -> service.calculate(loadout, profile)).getMessage().contains("N8B"));
            assertSame(configuration, loadout.configurations().get(key));
        }
    }

    @Test
    void shouldIgnoreExecutionOfOtherConfigurationsAndKeepTheirBarOrder() {
        var other = new RuntimeSkillKey("necromancer", "mor");
        var configuration = new RuntimeSkillConfiguration(key, 1, Map.of());
        var loadout = new RuntimeSkillLoadout("necromancer", Map.of(key, configuration,
                other, new RuntimeSkillConfiguration(other, 15, Map.of("grupa_1", "premia"))), List.of(other, key));
        assertEquals(2100, service.calculate(loadout, profile).result().finalDamage());
        assertEquals(List.of(other, key), loadout.actionBar());
        assertEquals(15, loadout.configurations().get(other).rank());
    }
}
