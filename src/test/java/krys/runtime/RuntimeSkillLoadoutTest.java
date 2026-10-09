package krys.runtime;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeSkillLoadoutTest {
    private static final RuntimeSkillKey KEY = new RuntimeSkillKey("necromancer", "krwotok");
    private static final RuntimeSkillKey OTHER = new RuntimeSkillKey("necromancer", "kosciane_odlamki");

    @Test
    void emptyLoadoutShouldKeepItsClassWithoutDerivingItFromSkills() {
        for (String classId : List.of("paladin", "necromancer", "custom-class")) {
            var loadout = new RuntimeSkillLoadout(classId, Map.of(), List.of());
            assertEquals(classId, loadout.classId());
            assertTrue(loadout.configurations().isEmpty());
            assertTrue(loadout.actionBar().isEmpty());
        }
    }

    @Test
    void learnedSkillShouldBeAllowedOnActionBarAndAvailableByLookup() {
        var configuration = config(KEY, 1);
        var loadout = new RuntimeSkillLoadout("necromancer", Map.of(KEY, configuration), List.of(KEY));
        assertEquals(Optional.of(configuration), loadout.configuration(KEY));
        assertTrue(loadout.contains(KEY));
        assertTrue(loadout.isOnActionBar(KEY));
        assertEquals(Optional.empty(), loadout.configuration(OTHER));
        assertFalse(loadout.contains(OTHER));
        assertFalse(loadout.isOnActionBar(OTHER));
        assertThrows(NullPointerException.class, () -> loadout.configuration(null));
        assertThrows(NullPointerException.class, () -> loadout.contains(null));
        assertThrows(NullPointerException.class, () -> loadout.isOnActionBar(null));
    }

    @Test
    void assignedUnlearnedConfigurationShouldBeKeptOutsideActionBar() {
        var loadout = new RuntimeSkillLoadout("necromancer", Map.of(KEY, config(KEY, 0)), List.of());
        assertTrue(loadout.contains(KEY));
        assertFalse(loadout.configuration(KEY).orElseThrow().isLearned());
        assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillLoadout(
                "necromancer", loadout.configurations(), List.of(KEY)));
    }

    @Test
    void missingActionBarConfigurationAndDuplicatesShouldBeRejectedWithoutRepair() {
        var configurations = Map.of(KEY, config(KEY, 1));
        assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillLoadout("necromancer", configurations, List.of(OTHER)));
        assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillLoadout("necromancer", configurations, List.of(KEY, KEY)));
    }

    @Test
    void mapKeyMustMatchConfigurationIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillLoadout(
                "necromancer", Map.of(KEY, config(OTHER, 1)), List.of()));
    }

    @Test
    void configurationsAndActionBarMustBelongToLoadoutClass() {
        var paladin = new RuntimeSkillKey("paladin", "wymach");
        assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillLoadout("necromancer",
                Map.of(KEY, config(KEY, 1), paladin, config(paladin, 1)), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillLoadout("paladin", Map.of(KEY, config(KEY, 1)), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillLoadout(
                "necromancer", Map.of(KEY, config(KEY, 1)), List.of(paladin)));
    }

    @Test
    void invalidClassIdShouldBeRejectedWithoutNormalization() {
        assertThrows(NullPointerException.class, () -> new RuntimeSkillLoadout(null, Map.of(), List.of()));
        for (String invalid : new String[]{"", " ", "Paladin", " paladin", "paladin ", "two words", "class!", "żółć",
                "class:id", "class.id", "_class", "-class", "class\n"}) {
            assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillLoadout(invalid, Map.of(), List.of()), invalid);
        }
    }

    @Test
    void nullCollectionsAndNullEntriesShouldBeRejected() {
        assertThrows(NullPointerException.class, () -> new RuntimeSkillLoadout("necromancer", null, List.of()));
        assertThrows(NullPointerException.class, () -> new RuntimeSkillLoadout("necromancer", Map.of(), null));
        var nullKey = new HashMap<RuntimeSkillKey, RuntimeSkillConfiguration>();
        nullKey.put(null, config(KEY, 1));
        var nullConfiguration = new HashMap<RuntimeSkillKey, RuntimeSkillConfiguration>();
        nullConfiguration.put(KEY, null);
        assertThrows(NullPointerException.class, () -> new RuntimeSkillLoadout("necromancer", nullKey, List.of()));
        assertThrows(NullPointerException.class, () -> new RuntimeSkillLoadout("necromancer", nullConfiguration, List.of()));
        var nullBar = new ArrayList<RuntimeSkillKey>();
        nullBar.add(null);
        assertThrows(NullPointerException.class, () -> new RuntimeSkillLoadout("necromancer", Map.of(KEY, config(KEY, 1)), nullBar));
    }

    @Test
    void collectionsShouldBeDefensiveImmutableAndActionBarShouldKeepOrder() {
        var configurations = new HashMap<>(Map.of(KEY, config(KEY, 1), OTHER, config(OTHER, 1)));
        var actionBar = new ArrayList<>(List.of(OTHER, KEY));
        var loadout = new RuntimeSkillLoadout("necromancer", configurations, actionBar);
        configurations.clear();
        actionBar.clear();
        assertEquals(2, loadout.configurations().size());
        assertEquals(List.of(OTHER, KEY), loadout.actionBar());
        assertThrows(UnsupportedOperationException.class, () -> loadout.configurations().put(KEY, config(KEY, 0)));
        assertThrows(UnsupportedOperationException.class, () -> loadout.configurations().entrySet().iterator().next().setValue(config(KEY, 0)));
        assertThrows(UnsupportedOperationException.class, () -> loadout.actionBar().add(KEY));
        assertThrows(UnsupportedOperationException.class, () -> loadout.actionBar().set(0, KEY));
    }

    @Test
    void neutralLoadoutShouldAllowSevenLearnedActionBarKeysWithoutUiPolicy() {
        var keys = IntStream.range(0, 7).mapToObj(i -> new RuntimeSkillKey("custom", "skill_" + i)).toList();
        var configurations = new HashMap<RuntimeSkillKey, RuntimeSkillConfiguration>();
        keys.forEach(key -> configurations.put(key, config(key, 1)));
        var loadout = new RuntimeSkillLoadout("custom", configurations, keys);
        assertEquals(keys, loadout.actionBar());
        assertEquals(7, loadout.actionBar().size());
    }

    @Test
    void equalityShouldIncludeClassConfigurationsAndActionBarOrder() {
        var configurations = Map.of(KEY, config(KEY, 1), OTHER, config(OTHER, 1));
        var first = new RuntimeSkillLoadout("necromancer", configurations, List.of(KEY, OTHER));
        var equal = new RuntimeSkillLoadout("necromancer", configurations, List.of(KEY, OTHER));
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, new RuntimeSkillLoadout("necromancer", configurations, List.of(OTHER, KEY)));
        assertNotEquals(new RuntimeSkillLoadout("paladin", Map.of(), List.of()), new RuntimeSkillLoadout("necromancer", Map.of(), List.of()));
    }

    private static RuntimeSkillConfiguration config(RuntimeSkillKey key, int rank) {
        return new RuntimeSkillConfiguration(key, rank, Map.of());
    }
}
