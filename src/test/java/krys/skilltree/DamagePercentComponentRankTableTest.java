package krys.skilltree;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static krys.skilltree.DamagePercentComponent.BURST_DAMAGE;
import static krys.skilltree.DamagePercentComponent.PRIMARY_DAMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamagePercentComponentRankTableTest {
    @Test
    void powinien_rozdzielac_interwal_impulsow_od_duration_bez_sumowania_obrazen() {
        // Golden N5: 30/77 na impuls co 2 s oraz na zdarzenie; 30 s to osobny czas klątwy.
        var periodic = DamagePercentComponent.PERIODIC_DAMAGE;
        var event = DamagePercentComponent.EVENT_TRIGGERED_DAMAGE;
        Map<DamagePercentComponent, Integer> intervals = new LinkedHashMap<>(Map.of(periodic, 2));
        var table = DamagePercentComponentRankTable.of(Map.of(
                periodic, DamagePercentRankTable.of(Map.of(1, 30, 15, 77)),
                event, DamagePercentRankTable.of(Map.of(1, 30, 15, 77))), Map.of(), intervals);
        intervals.clear();
        assertEquals(2, table.intervalSecondsFor(periodic));
        assertNull(table.intervalSecondsFor(event));
        for (var component : Set.of(periodic, event)) {
            assertEquals(Map.of(1, 30, 15, 77), table.tableFor(component).asMap());
            assertNull(table.durationSecondsFor(component));
            for (int rank = 2; rank <= 14; rank++) assertNull(table.damagePercentAt(component, rank));
        }
        assertFalse(table.hasComponent(DamagePercentComponent.DAMAGE_PER_SECOND));
        assertFalse(table.hasComponent(DamagePercentComponent.DAMAGE_OVER_DURATION));
        assertNull(DamagePercentComponentRankTable.empty().intervalSecondsFor(periodic));
        assertThrows(NullPointerException.class, () -> table.intervalSecondsFor(null));
    }

    @Test
    void powinien_dopuszczac_brak_interwalu_i_duration_dla_nowych_komponentow() {
        for (var component : Set.of(DamagePercentComponent.PERIODIC_DAMAGE, DamagePercentComponent.EVENT_TRIGGERED_DAMAGE)) {
            var table = DamagePercentComponentRankTable.of(Map.of(component, DamagePercentRankTable.of(Map.of(1, 30, 15, 77))));
            assertNull(table.intervalSecondsFor(component));
            assertNull(table.durationSecondsFor(component));
            assertEquals(Map.of(1, 30, 15, 77), table.tableFor(component).asMap());
        }
    }

    @Test
    void powinien_odrzucac_niedodatni_lub_osierocony_interwal_i_nie_zastepowac_nim_duration() {
        var component = DamagePercentComponent.PERIODIC_DAMAGE;
        var ranks = Map.of(component, DamagePercentRankTable.of(Map.of(1, 30, 15, 77)));
        for (int seconds : new int[]{0, -1}) {
            assertThrows(IllegalArgumentException.class, () -> DamagePercentComponentRankTable.of(ranks, Map.of(), Map.of(component, seconds)));
        }
        assertThrows(IllegalArgumentException.class, () -> DamagePercentComponentRankTable.of(Map.of(), Map.of(), Map.of(component, 2)));
        assertThrows(IllegalArgumentException.class, () -> DamagePercentComponentRankTable.of(ranks, Map.of(), Map.of(BURST_DAMAGE, 2)));
        assertThrows(NullPointerException.class, () -> DamagePercentComponentRankTable.of(ranks, Map.of(), null));
        var nullInterval = new LinkedHashMap<DamagePercentComponent, Integer>();
        nullInterval.put(component, null);
        assertThrows(NullPointerException.class, () -> DamagePercentComponentRankTable.of(ranks, Map.of(), nullInterval));
        var total = DamagePercentComponent.DAMAGE_OVER_DURATION;
        assertThrows(IllegalArgumentException.class, () -> DamagePercentComponentRankTable.of(
                Map.of(total, DamagePercentRankTable.of(Map.of(1, 210, 15, 536))), Map.of(), Map.of(total, 2)));
    }

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
    void powinien_przechowywac_laczny_procent_za_jawny_okres_bez_przeliczania_na_sekunde() {
        // Golden values Moru: D4Guides R1 oraz max-view D4Builds, 210 / 536 łącznie za 6 s.
        var component = DamagePercentComponent.DAMAGE_OVER_DURATION;
        Map<DamagePercentComponent, Integer> durations = new LinkedHashMap<>(Map.of(component, 6));
        var table = DamagePercentComponentRankTable.of(Map.of(component, DamagePercentRankTable.of(Map.of(1, 210, 15, 536))), durations);
        durations.clear();
        assertEquals(210, table.damagePercentAt(component, 1));
        assertEquals(536, table.damagePercentAt(component, 15));
        assertEquals(6, table.durationSecondsFor(component));
        assertNull(table.durationSecondsFor(PRIMARY_DAMAGE));
        assertNull(DamagePercentComponentRankTable.of(Map.of(PRIMARY_DAMAGE, DamagePercentRankTable.of(Map.of(1, 160))))
                .durationSecondsFor(PRIMARY_DAMAGE));
    }

    @Test
    void powinien_odrzucac_brak_czasu_lub_czas_niepasujacy_do_komponentu() {
        var component = DamagePercentComponent.DAMAGE_OVER_DURATION;
        var ranks = Map.of(component, DamagePercentRankTable.of(Map.of(1, 210)));
        assertThrows(IllegalArgumentException.class, () -> DamagePercentComponentRankTable.of(ranks));
        assertThrows(IllegalArgumentException.class, () -> DamagePercentComponentRankTable.of(ranks, Map.of(component, 0)));
        assertThrows(IllegalArgumentException.class, () -> DamagePercentComponentRankTable.of(Map.of(), Map.of(component, 6)));
        assertThrows(NullPointerException.class, () -> DamagePercentComponentRankTable.of(ranks, null));
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
