package krys.itemimport;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy zalążkowego katalogu aspektów używanego przez import i bibliotekę. */
class AspectRegistryTest {
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
}
