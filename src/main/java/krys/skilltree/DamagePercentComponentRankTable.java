package krys.skilltree;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Niemutowalna mapa komponentow obrazen na ich tabele procentow per ranga. */
public final class DamagePercentComponentRankTable {
    private static final DamagePercentComponentRankTable EMPTY = new DamagePercentComponentRankTable(Map.of(), Map.of(), Map.of());

    private final Map<DamagePercentComponent, DamagePercentRankTable> tablesByComponent;
    private final Map<DamagePercentComponent, Integer> durationSecondsByComponent;
    private final Map<DamagePercentComponent, Integer> intervalSecondsByComponent;

    private DamagePercentComponentRankTable(Map<DamagePercentComponent, DamagePercentRankTable> tablesByComponent,
                                           Map<DamagePercentComponent, Integer> durationSecondsByComponent,
                                           Map<DamagePercentComponent, Integer> intervalSecondsByComponent) {
        this.tablesByComponent = Map.copyOf(tablesByComponent);
        this.durationSecondsByComponent = Map.copyOf(durationSecondsByComponent);
        this.intervalSecondsByComponent = Map.copyOf(intervalSecondsByComponent);
    }

    public static DamagePercentComponentRankTable empty() {
        return EMPTY;
    }

    public static DamagePercentComponentRankTable of(Map<DamagePercentComponent, DamagePercentRankTable> tablesByComponent) {
        return of(tablesByComponent, Map.of());
    }

    /** Czas określa okres, za który podano łączny procent; nie przelicza go na DPS. */
    public static DamagePercentComponentRankTable of(Map<DamagePercentComponent, DamagePercentRankTable> tablesByComponent,
                                                    Map<DamagePercentComponent, Integer> durationSecondsByComponent) {
        return of(tablesByComponent, durationSecondsByComponent, Map.of());
    }

    /** Interwał opisuje odstęp między impulsami, niezależnie od okresu łącznych obrażeń; nie liczy DPS. */
    public static DamagePercentComponentRankTable of(Map<DamagePercentComponent, DamagePercentRankTable> tablesByComponent,
                                                    Map<DamagePercentComponent, Integer> durationSecondsByComponent,
                                                    Map<DamagePercentComponent, Integer> intervalSecondsByComponent) {
        Objects.requireNonNull(tablesByComponent, "tablesByComponent");
        Objects.requireNonNull(durationSecondsByComponent, "durationSecondsByComponent");
        Objects.requireNonNull(intervalSecondsByComponent, "intervalSecondsByComponent");

        Map<DamagePercentComponent, DamagePercentRankTable> validated = new LinkedHashMap<>();
        for (Map.Entry<DamagePercentComponent, DamagePercentRankTable> entry : tablesByComponent.entrySet()) {
            DamagePercentComponent component = Objects.requireNonNull(entry.getKey(), "component");
            DamagePercentRankTable table = Objects.requireNonNull(entry.getValue(), "table");
            validated.put(component, table);
        }
        Map<DamagePercentComponent, Integer> durations = new LinkedHashMap<>();
        for (var entry : durationSecondsByComponent.entrySet()) {
            var component = Objects.requireNonNull(entry.getKey(), "component");
            var seconds = Objects.requireNonNull(entry.getValue(), "durationSeconds");
            if (seconds <= 0 || !validated.containsKey(component)) {
                throw new IllegalArgumentException("Czas musi być dodatni i przypisany do obecnego komponentu");
            }
            durations.put(component, seconds);
        }
        if (validated.containsKey(DamagePercentComponent.DAMAGE_OVER_DURATION)
                && !durations.containsKey(DamagePercentComponent.DAMAGE_OVER_DURATION)) {
            throw new IllegalArgumentException("DAMAGE_OVER_DURATION wymaga jawnego czasu trwania");
        }
        Map<DamagePercentComponent, Integer> intervals = new LinkedHashMap<>();
        for (var entry : intervalSecondsByComponent.entrySet()) {
            var component = Objects.requireNonNull(entry.getKey(), "component");
            var seconds = Objects.requireNonNull(entry.getValue(), "intervalSeconds");
            if (seconds <= 0 || !validated.containsKey(component)) {
                throw new IllegalArgumentException("Interwał musi być dodatni i przypisany do obecnego komponentu");
            }
            intervals.put(component, seconds);
        }
        return validated.isEmpty() ? empty() : new DamagePercentComponentRankTable(validated, durations, intervals);
    }

    public Integer intervalSecondsFor(DamagePercentComponent component) {
        Objects.requireNonNull(component, "component");
        return intervalSecondsByComponent.get(component);
    }

    public Integer durationSecondsFor(DamagePercentComponent component) {
        Objects.requireNonNull(component, "component");
        return durationSecondsByComponent.get(component);
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
