package krys.paladin;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamagePercentComponentTest {
    @Test
    void enum_powinien_zawierac_wymagane_komponenty_obrazen() {
        Set<String> components = java.util.Arrays.stream(DamagePercentComponent.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "PRIMARY_DAMAGE",
                "ADDITIONAL_STRIKE_DAMAGE",
                "PASSIVE_DAMAGE",
                "ACTIVE_DAMAGE",
                "JUMP_DAMAGE",
                "LANDING_DAMAGE",
                "BURST_DAMAGE",
                "DAMAGE_PER_SECOND",
                "FIRST_STRIKE_DAMAGE",
                "SECOND_STRIKE_DAMAGE"
        ), components);
    }
}
