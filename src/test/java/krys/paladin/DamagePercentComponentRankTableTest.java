package krys.paladin;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static krys.paladin.DamagePercentComponent.BURST_DAMAGE;
import static krys.paladin.DamagePercentComponent.PRIMARY_DAMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamagePercentComponentRankTableTest {
    @Test
    void powinien_przechowywac_wiele_komponentow_i_zwracac_wartosci_per_ranga() {
        DamagePercentRankTable primary = DamagePercentRankTable.of(Map.of(1, 160, 15, 408));
        DamagePercentRankTable burst = DamagePercentRankTable.of(Map.of(1, 120, 15, 306));
        DamagePercentComponentRankTable table = DamagePercentComponentRankTable.of(Map.of(
                PRIMARY_DAMAGE, primary,
                BURST_DAMAGE, burst
        ));

        assertFalse(table.isEmpty());
        assertTrue(table.hasComponent(PRIMARY_DAMAGE));
        assertTrue(table.hasComponent(BURST_DAMAGE));
        assertEquals(primary, table.tableFor(PRIMARY_DAMAGE));
        assertEquals(160, table.damagePercentAt(PRIMARY_DAMAGE, 1));
        assertEquals(306, table.damagePercentAt(BURST_DAMAGE, 15));
    }

    @Test
    void powinien_byc_null_safe_dla_brakujacego_komponentu() {
        DamagePercentComponentRankTable table = DamagePercentComponentRankTable.of(Map.of(
                PRIMARY_DAMAGE, DamagePercentRankTable.of(Map.of(1, 160))
        ));

        assertFalse(table.hasComponent(BURST_DAMAGE));
        assertNull(table.tableFor(BURST_DAMAGE));
        assertNull(table.damagePercentAt(BURST_DAMAGE, 1));
    }

    @Test
    void powinien_odrzucac_null_component_i_null_table() {
        Map<DamagePercentComponent, DamagePercentRankTable> nullComponent = new LinkedHashMap<>();
        nullComponent.put(null, DamagePercentRankTable.of(Map.of(1, 160)));

        Map<DamagePercentComponent, DamagePercentRankTable> nullTable = new LinkedHashMap<>();
        nullTable.put(PRIMARY_DAMAGE, null);

        assertThrows(NullPointerException.class, () -> DamagePercentComponentRankTable.of(nullComponent));
        assertThrows(NullPointerException.class, () -> DamagePercentComponentRankTable.of(nullTable));

        DamagePercentComponentRankTable table = DamagePercentComponentRankTable.empty();
        assertThrows(NullPointerException.class, () -> table.tableFor(null));
        assertThrows(NullPointerException.class, () -> table.hasComponent(null));
    }

    @Test
    void powinien_byc_niemutowalny_po_utworzeniu() {
        Map<DamagePercentComponent, DamagePercentRankTable> source = new LinkedHashMap<>();
        source.put(PRIMARY_DAMAGE, DamagePercentRankTable.of(Map.of(1, 160)));

        DamagePercentComponentRankTable table = DamagePercentComponentRankTable.of(source);
        source.put(BURST_DAMAGE, DamagePercentRankTable.of(Map.of(1, 120)));
        table.asMap().put(BURST_DAMAGE, DamagePercentRankTable.of(Map.of(1, 999)));

        assertTrue(table.hasComponent(PRIMARY_DAMAGE));
        assertFalse(table.hasComponent(BURST_DAMAGE));
        assertEquals(Set.of(PRIMARY_DAMAGE), table.asMap().keySet());
    }

    @Test
    void pusta_tabela_powinna_byc_dozwolona() {
        DamagePercentComponentRankTable table = DamagePercentComponentRankTable.empty();

        assertTrue(table.isEmpty());
        assertTrue(table.asMap().isEmpty());
        assertNull(table.tableFor(PRIMARY_DAMAGE));
        assertNull(table.damagePercentAt(PRIMARY_DAMAGE, 1));
    }
}
