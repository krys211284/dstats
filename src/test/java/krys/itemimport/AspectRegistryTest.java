package krys.itemimport;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy zalążkowego katalogu aspektów używanego przez import i bibliotekę. */
class AspectRegistryTest {
    private static final List<String> NEW_OFFENSIVE_ASPECT_IDS = List.of(
            "immortal_glory_aspect",
            "watkins_law_aspect",
            "proselytism_aspect",
            "redirected_power_aspect",
            "sanctified_punishment_aspect",
            "crushing_aspect",
            "accelerating_aspect",
            "golden_hour_aspect",
            "elemental_fate_aspect",
            "bristling_aspect",
            "relentless_aspect",
            "revelatory_aspect",
            "penitential_aspect",
            "smiting_aspect",
            "conceited_aspect"
    );

    @Test
    void shouldContainNewVerifiedPaladinOffensiveAspects() {
        AspectRegistry registry = ApplicationAspectRegistry.get();
        Set<String> ids = new HashSet<>();
        for (AspectDefinition aspect : registry.all()) {
            assertTrue(ids.add(aspect.getId()), "Zduplikowane id aspektu: " + aspect.getId());
        }

        for (String id : NEW_OFFENSIVE_ASPECT_IDS) {
            AspectDefinition aspect = registry.findById(id).orElseThrow();
            assertEquals(AspectCategory.OFFENSE, aspect.getCategory(), id);
            assertEquals(AspectType.LEGENDARY, aspect.getAspectType(), id);
            assertEquals(AspectRuntimeStatus.DESCRIPTIVE_ONLY, aspect.getRuntimeStatus(), id);
            assertEquals(AspectDefinitionSource.VERIFIED_SCREENSHOT, aspect.getSource(), id);
            assertFalse(aspect.getDisplayName().isBlank(), id);
            assertFalse(aspect.getEffectDescription().isBlank(), id);
            assertFalse(aspect.getRolls().isEmpty(), id);
            assertTrue(aspect.allowsSlot(EquipmentSlot.OFF_HAND), id);
        }
    }

    @Test
    void shouldExposeDetailedValuesForNewOffensiveAspects() {
        assertRoll("immortal_glory_aspect", "damage_multiplier", 35.0d, 20.0d, 35.0d);
        assertRoll("watkins_law_aspect", "damage_multiplier", 65.0d, 45.0d, 65.0d);
        assertRoll("proselytism_aspect", "jump_chance", 40.0d, null, null);
        assertRoll("proselytism_aspect", "damage_multiplier", 150.0d, 50.0d, 150.0d);
        assertRoll("redirected_power_aspect", "critical_damage_from_block", 60.0d, 40.0d, 60.0d);
        assertRoll("sanctified_punishment_aspect", "holy_fire_damage_multiplier", 60.0d, 40.0d, 60.0d);
        assertRoll("crushing_aspect", "fortify_damage_multiplier", 65.0d, 45.0d, 65.0d);
        assertRoll("accelerating_aspect", "attack_speed_bonus", 50.0d, 30.0d, 50.0d);
        assertEquals(5, aspect("accelerating_aspect").getDurationSeconds());
        assertRoll("golden_hour_aspect", "judgement_damage_multiplier", 100.0d, 60.0d, 100.0d);
        assertRoll("elemental_fate_aspect", "elemental_damage_multiplier", 60.0d, 40.0d, 60.0d);
        assertEquals(7, aspect("elemental_fate_aspect").getDurationSeconds());
        assertEquals(6, aspect("elemental_fate_aspect").getMaxStacks());
        assertRoll("bristling_aspect", "thorns_retaliation", 300.0d, 200.0d, 300.0d);
        assertRoll("relentless_aspect", "knockdown_damage_taken", 60.0d, 50.0d, 60.0d);
        assertEquals(6, aspect("relentless_aspect").getDurationSeconds());
        assertRoll("revelatory_aspect", "zealot_damage_multiplier", 32.5d, 12.5d, 32.5d);
        assertRoll("penitential_aspect", "critical_chance", 15.0d, 5.0d, 15.0d);
        assertRoll("penitential_aspect", "critical_damage", 55.0d, 35.0d, 55.0d);
        assertEquals(4, aspect("penitential_aspect").getDurationSeconds());
        assertRoll("smiting_aspect", "overpower_damage_multiplier", 90.0d, 60.0d, 90.0d);
        assertRoll("conceited_aspect", "barrier_damage_multiplier", 60.0d, 40.0d, 60.0d);
    }

    @Test
    void shouldSuggestNewAspectsByPolishNameFromOcrText() {
        AspectRegistry registry = ApplicationAspectRegistry.get();
        for (String id : NEW_OFFENSIVE_ASPECT_IDS) {
            AspectDefinition aspect = registry.findById(id).orElseThrow();
            FullItemRead fullRead = new FullItemRead(
                    "Test",
                    "Tarcza",
                    "Legendarny",
                    "Moc przedmiotu: 900",
                    "",
                    List.of(new FullItemReadLine(FullItemReadLineType.ASPECT, aspect.getDisplayName()))
            );

            AspectRegistry.AspectMatch match = registry.suggestFromFullRead(fullRead).orElseThrow();

            assertEquals(id, match.aspectId(), aspect.getDisplayName());
            assertEquals(ItemImportFieldConfidence.HIGH, match.confidence());
        }
    }

    @Test
    void shouldContainNaznaczenieAspectForStormShieldImport() {
        AspectDefinition aspect = ApplicationAspectRegistry.get().findById("naznaczenie_aspect").orElseThrow();

        assertEquals("Naznaczenie", aspect.getDisplayName());
        assertEquals(AspectCategory.OFFENSE, aspect.getCategory());
        assertEquals(AspectDefinitionSource.VERIFIED_SCREENSHOT, aspect.getSource());
        assertEquals(AspectRuntimeStatus.DESCRIPTIVE_ONLY, aspect.getRuntimeStatus());
        assertTrue(aspect.getEffectDescription().contains("Wampirycznego Szału Krwi"));
        assertTrue(aspect.allowsSlot(EquipmentSlot.OFF_HAND));
    }

    @Test
    void shouldFindKnownSeedAspectByStableId() {
        AspectRegistry registry = ApplicationAspectRegistry.get();

        AspectDefinition aspect = registry.findById("inner-calm").orElseThrow();

        assertEquals("inner-calm", aspect.getId());
        assertEquals("Aspekt Wewnętrznego Spokoju", aspect.getDisplayName());
        assertEquals(AspectType.LEGENDARY, aspect.getAspectType());
        assertTrue(aspect.getEffectDescription().contains("Zwiększa zadawane obrażenia podczas stania w bezruchu"));
        assertTrue(aspect.getEffectDescription().contains("Premia jest trzykrotnie większa"));
        assertTrue(aspect.getEffectDescription().contains("co najmniej 3 sekundy"));
        assertFalse(aspect.getEffectDescription().contains("%"));
    }

    @Test
    void shouldContainVerathielAsUniqueDescriptiveAspect() {
        AspectRegistry registry = ApplicationAspectRegistry.get();

        AspectDefinition aspect = registry.findById("verathiel_shard").orElseThrow();

        assertEquals("Odłamek Verathiela", aspect.getDisplayName());
        assertEquals(AspectType.UNIQUE, aspect.getAspectType());
        assertTrue(aspect.isUniqueAspect());
        assertEquals(AspectRuntimeStatus.DESCRIPTIVE_ONLY, aspect.getRuntimeStatus());
        assertTrue(aspect.allowsSlot(EquipmentSlot.MAIN_HAND));
        assertFalse(aspect.allowsSlot(EquipmentSlot.OFF_HAND));
        assertTrue(aspect.getEffectDescription().contains("100%[x]"));
        assertTrue(aspect.getEffectDescription().contains("[70 - 100]"));
        assertTrue(aspect.getEffectDescription().contains("25 pkt. podstawowego zasobu"));
    }

    @Test
    void shouldReturnEmptyForUnknownAspectId() {
        AspectRegistry registry = ApplicationAspectRegistry.get();

        assertTrue(registry.findById("test-aspect").isEmpty());
    }

    @Test
    void shouldEnforceAllowedItemSlots() {
        AspectRegistry registry = ApplicationAspectRegistry.get();
        AspectDefinition aspect = registry.findById("inner-calm").orElseThrow();

        assertTrue(aspect.allowsSlot(EquipmentSlot.OFF_HAND));
        assertTrue(registry.allowedForSlot(EquipmentSlot.BOOTS).isEmpty());
    }

    private static AspectDefinition aspect(String id) {
        return ApplicationAspectRegistry.get().findById(id).orElseThrow();
    }

    private static void assertRoll(String aspectId,
                                   String rollId,
                                   double current,
                                   Double rangeMin,
                                   Double rangeMax) {
        AspectRollDefinition roll = aspect(aspectId).getRolls().stream()
                .filter(candidate -> candidate.getId().equals(rollId))
                .findFirst()
                .orElseThrow();
        assertEquals(current, roll.getCurrentValue(), 0.0000001d);
        assertEquals(rangeMin, roll.getRangeMin());
        assertEquals(rangeMax, roll.getRangeMax());
    }
}
