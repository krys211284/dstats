package krys.itemimport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy katalogu affixów używanego przez import OCR itemów. */
class AffixRegistryTest {
    @Test
    void shouldContainFourVerathielAffixesWithOcrAliases() {
        AffixRegistry registry = ApplicationAffixRegistry.get();
        List<AffixDefinition> verathielAffixes = registry.all().stream()
                .filter(definition -> definition.getId().startsWith("verathiel_"))
                .toList();

        assertEquals(4, verathielAffixes.size());
        for (AffixDefinition definition : verathielAffixes) {
            assertFalse(definition.getOcrAliases().isEmpty(), definition.getId());
            assertEquals(AffixRuntimeStatus.DESCRIPTIVE_ONLY, definition.getRuntimeStatus(), definition.getId());
            assertTrue(definition.isAutomaticMatchingAllowed(), definition.getId());
            assertTrue(definition.isManualVerificationRequired(), definition.getId());
        }
    }

    @Test
    void shouldMatchEveryVerathielAffixFromOcrText() {
        AffixRegistry registry = ApplicationAffixRegistry.get();

        assertMatched(registry, "+94 obrażeń od broni [94 - 157]", "verathiel_weapon_damage_flat");
        assertMatched(registry, "+2 141 maksymalnego zdrowia [1 831 - 2 200]", "verathiel_maximum_life");
        assertMatched(registry, "+545 pkt. zdrowia przy trafieniu [526 - 632]", "verathiel_life_on_hit");
        assertMatched(registry, "Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]",
                "verathiel_lucky_hit_primary_resource");
    }

    @Test
    void shouldKeepAffixCatalogSeparateFromAspectCatalog() {
        AffixRegistry affixRegistry = ApplicationAffixRegistry.get();
        AspectRegistry aspectRegistry = ApplicationAspectRegistry.get();

        assertTrue(aspectRegistry.findById("verathiel_shard").isPresent());
        assertTrue(affixRegistry.findById("verathiel_shard").isEmpty());
        assertTrue(affixRegistry.all().stream()
                .noneMatch(definition -> definition.getDisplayName().equals("Odłamek Verathiela")));
    }

    private static void assertMatched(AffixRegistry registry, String text, String expectedDefinitionId) {
        assertTrue(registry.findMatches(text).stream()
                .anyMatch(match -> match.definition().getId().equals(expectedDefinitionId)), text);
    }
}
