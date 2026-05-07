package krys.paladin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Niemutowalna mapa komponentow obrazen na ich tabele procentow per ranga. */
public final class DamagePercentComponentRankTable {
    private static final DamagePercentComponentRankTable EMPTY = new DamagePercentComponentRankTable(Map.of());

    private final Map<DamagePercentComponent, DamagePercentRankTable> tablesByComponent;

    private DamagePercentComponentRankTable(Map<DamagePercentComponent, DamagePercentRankTable> tablesByComponent) {
        this.tablesByComponent = Map.copyOf(tablesByComponent);
    }

    public static DamagePercentComponentRankTable empty() {
        return EMPTY;
    }

    public static DamagePercentComponentRankTable of(Map<DamagePercentComponent, DamagePercentRankTable> tablesByComponent) {
        Objects.requireNonNull(tablesByComponent, "tablesByComponent");
        if (tablesByComponent.isEmpty()) {
            return empty();
        }

        Map<DamagePercentComponent, DamagePercentRankTable> validated = new LinkedHashMap<>();
        for (Map.Entry<DamagePercentComponent, DamagePercentRankTable> entry : tablesByComponent.entrySet()) {
            DamagePercentComponent component = Objects.requireNonNull(entry.getKey(), "component");
            DamagePercentRankTable table = Objects.requireNonNull(entry.getValue(), "table");
            validated.put(component, table);
        }
        return new DamagePercentComponentRankTable(validated);
    }

    public DamagePercentRankTable tableFor(DamagePercentComponent component) {
        Objects.requireNonNull(component, "component");
        return tablesByComponent.get(component);
    }

    public Integer damagePercentAt(DamagePercentComponent component, int rank) {
        DamagePercentRankTable table = tableFor(component);
        return table == null ? null : table.damagePercentAtRank(rank);
    }

    public boolean hasComponent(DamagePercentComponent component) {
        Objects.requireNonNull(component, "component");
        return tablesByComponent.containsKey(component);
    }

    public boolean isEmpty() {
        return tablesByComponent.isEmpty();
    }

    public Map<DamagePercentComponent, DamagePercentRankTable> asMap() {
        return new LinkedHashMap<>(tablesByComponent);
    }
}
