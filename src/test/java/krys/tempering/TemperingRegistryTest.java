package krys.tempering;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemperingRegistryTest {
    private final TemperingAffixRegistry registry = ApplicationTemperingAffixRegistry.get();

    @Test
    void globalne_kategorie_hartowania_maja_pelny_katalog_nazw() {
        assertEquals(6, TemperingCategory.values().length);
        assertEquals("Broń", TemperingCategory.WEAPON.getDisplayName());
        assertEquals("Ofensywa", TemperingCategory.OFFENSE.getDisplayName());
        assertEquals("Defensywa", TemperingCategory.DEFENSE.getDisplayName());
        assertEquals("Funkcjonalność", TemperingCategory.UTILITY.getDisplayName());
        assertEquals("Mobilność", TemperingCategory.MOBILITY.getDisplayName());
        assertEquals("Zasoby", TemperingCategory.RESOURCE.getDisplayName());
    }

    @Test
    void miecz_ma_tylko_potwierdzone_kategorie_bron_i_ofensywa() {
        List<TemperingCategory> categories = TemperingEligibilityRegistry.availableCategories(EquipmentSlot.MAIN_HAND, "Miecz");

        assertEquals(List.of(TemperingCategory.WEAPON, TemperingCategory.OFFENSE), categories);
        assertFalse(categories.contains(TemperingCategory.DEFENSE));
        assertFalse(categories.contains(TemperingCategory.UTILITY));
        assertFalse(categories.contains(TemperingCategory.MOBILITY));
        assertFalse(categories.contains(TemperingCategory.RESOURCE));
    }

    @Test
    void tarcza_ma_potwierdzone_kategorie_bron_ofensywa_defensywa_i_funkcjonalnosc() {
        List<TemperingCategory> categories = TemperingEligibilityRegistry.availableCategories(EquipmentSlot.OFF_HAND, "Tarcza");

        assertEquals(List.of(
                TemperingCategory.WEAPON,
                TemperingCategory.OFFENSE,
                TemperingCategory.DEFENSE,
                TemperingCategory.UTILITY
        ), categories);
        assertFalse(categories.contains(TemperingCategory.MOBILITY));
        assertFalse(categories.contains(TemperingCategory.RESOURCE));
    }

    @Test
    void katalog_defensywy_ma_12_affixow_z_zakresami_i_statusem_data_only() {
        List<TemperingAffixDefinition> defense = registry.byCategory(TemperingCategory.DEFENSE);

        assertEquals(12, defense.size());
        assertRange("defense_maximum_life", 1000.0d, 1500.0d);
        assertRange("defense_armor", 1250.0d, 2000.0d);
        assertRange("defense_all_resistance", 60.0d, 70.0d);
        assertRange("defense_max_animus", 2.0d, 3.0d);
        assertRange("defense_block_chance", 2.5d, 5.0d);
        assertRange("defense_arbiter_armor_percent", 7.0d, 10.0d);
        assertTrue(defense.stream().allMatch(definition -> definition.getRuntimeStatus() == TemperingRuntimeStatus.DATA_ONLY));
    }

    private void assertRange(String id, double min, double max) {
        TemperingAffixDefinition definition = registry.findById(id).orElseThrow();
        assertEquals(min, definition.getRangeMin(), 0.0000001d);
        assertEquals(max, definition.getRangeMax(), 0.0000001d);
    }
}
