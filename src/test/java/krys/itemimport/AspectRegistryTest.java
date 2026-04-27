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
        assertTrue(aspect.getEffectDescription().contains("Zwiększa zadawane obrażenia podczas stania w bezruchu"));
        assertTrue(aspect.getEffectDescription().contains("Premia jest trzykrotnie większa"));
        assertTrue(aspect.getEffectDescription().contains("co najmniej 3 sekundy"));
        assertFalse(aspect.getEffectDescription().contains("%"));
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
