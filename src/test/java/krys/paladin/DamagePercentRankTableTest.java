package krys.paladin;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamagePercentRankTableTest {
    @Test
    void powinien_przechowywac_rangi_od_1_do_15_i_zwracac_istniejace_wartosci() {
        DamagePercentRankTable table = DamagePercentRankTable.of(fullRankTable());

        assertEquals(15, table.asMap().size());
        assertEquals(115, table.damagePercentAtRank(1));
        assertEquals(293, table.damagePercentAtRank(15));
        assertEquals(115, table.damagePercentAtRank1());
        assertEquals(293, table.damagePercentAtTreeMaxRank(15));
    }

    @Test
    void powinien_zwracac_null_dla_brakujacej_rangi_w_poprawnym_zakresie() {
        DamagePercentRankTable table = DamagePercentRankTable.of(Map.of(1, 115, 15, 293));

        assertNull(table.damagePercentAtRank(2));
    }

    @Test
    void powinien_odrzucac_rangi_poza_zakresem_przy_tworzeniu() {
        assertThrows(IllegalArgumentException.class, () -> DamagePercentRankTable.of(Map.of(0, 100)));
        assertThrows(IllegalArgumentException.class, () -> DamagePercentRankTable.of(Map.of(16, 300)));
    }

    @Test
    void powinien_odrzucac_null_rank_i_null_value_przy_tworzeniu() {
        Map<Integer, Integer> nullRank = new LinkedHashMap<>();
        nullRank.put(null, 100);

        Map<Integer, Integer> nullValue = new LinkedHashMap<>();
        nullValue.put(1, null);

        assertThrows(NullPointerException.class, () -> DamagePercentRankTable.of(nullRank));
        assertThrows(NullPointerException.class, () -> DamagePercentRankTable.of(nullValue));
    }

    @Test
    void powinien_byc_niemutowalny_po_utworzeniu() {
        Map<Integer, Integer> source = new LinkedHashMap<>();
        source.put(1, 115);

        DamagePercentRankTable table = DamagePercentRankTable.of(source);
        source.put(1, 999);
        source.put(2, 126);

        Map<Integer, Integer> snapshot = table.asMap();
        snapshot.put(1, 888);

        assertEquals(115, table.damagePercentAtRank(1));
        assertNull(table.damagePercentAtRank(2));
        assertEquals(Map.of(1, 115), table.asMap());
    }

    @Test
    void pusta_tabela_powinna_byc_null_safe() {
        DamagePercentRankTable table = DamagePercentRankTable.empty();

        assertTrue(table.isEmpty());
        assertNull(table.damagePercentAtRank1());
        assertNull(table.damagePercentAtTreeMaxRank(15));
        assertTrue(table.asMap().isEmpty());
    }

    private static Map<Integer, Integer> fullRankTable() {
        return Map.ofEntries(
                Map.entry(1, 115),
                Map.entry(2, 126),
                Map.entry(3, 138),
                Map.entry(4, 149),
                Map.entry(5, 167),
                Map.entry(6, 178),
                Map.entry(7, 190),
                Map.entry(8, 201),
                Map.entry(9, 213),
                Map.entry(10, 230),
                Map.entry(11, 241),
                Map.entry(12, 253),
                Map.entry(13, 264),
                Map.entry(14, 276),
                Map.entry(15, 293)
        );
    }
}
