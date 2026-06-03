package krys.tempering;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Jawny katalog hartowania z danych przekazanych przez użytkownika. */
public final class ApplicationTemperingAffixRegistry implements TemperingAffixRegistry {
    private static final ApplicationTemperingAffixRegistry INSTANCE = new ApplicationTemperingAffixRegistry();

    private final List<TemperingAffixDefinition> definitions;

    private ApplicationTemperingAffixRegistry() {
        this.definitions = List.of(
                offense("offense_critical_strike_chance", "szansy na trafienie krytyczne", "+%s%% szansy na trafienie krytyczne", 0, 0, TemperingValueUnit.PERCENT, "Zakres hartowania wymaga weryfikacji; importer używa tej definicji wyłącznie do klasyfikacji dataOnly po limicie ordinary affixów."),
                defense("defense_maximum_life", "maksymalnego zdrowia", "+[%s - %s] maksymalnego zdrowia", 1000, 1500, TemperingValueUnit.FLAT, ""),
                defense("defense_armor", "pancerza", "+[%s - %s] pancerza", 1250, 2000, TemperingValueUnit.FLAT, ""),
                defense("defense_all_resistance", "do odporności na wszystkie żywioły", "+[%s - %s] do odporności na wszystkie żywioły", 60, 70, TemperingValueUnit.FLAT, ""),
                defense("defense_max_animus", "do maksymalnej liczby kumulacji Animuszu", "+[%s - %s] do maksymalnej liczby kumulacji Animuszu", 2, 3, TemperingValueUnit.FLAT, "Wpływa na cap Animuszu w runtime; Doskonalenie używa potwierdzonych wartości +7 i +12.", 5.0d),
                defense("defense_fire_resistance", "do odporności na: Ogień", "+[%s - %s] do odporności na: Ogień", 440, 490, TemperingValueUnit.FLAT, ""),
                defense("defense_lightning_resistance", "do odporności na: Błyskawice", "+[%s - %s] do odporności na: Błyskawice", 440, 490, TemperingValueUnit.FLAT, ""),
                defense("defense_cold_resistance", "do odporności na: Zimno", "+[%s - %s] do odporności na: Zimno", 440, 490, TemperingValueUnit.FLAT, ""),
                defense("defense_poison_resistance", "do odporności na: Trucizny", "+[%s - %s] do odporności na: Trucizny", 440, 490, TemperingValueUnit.FLAT, ""),
                defense("defense_shadow_resistance", "do odporności na: Cień", "+[%s - %s] do odporności na: Cień", 440, 490, TemperingValueUnit.FLAT, ""),
                defense("defense_physical_resistance", "do odporności na: Fizyczne", "+[%s - %s] do odporności na: Fizyczne", 440, 490, TemperingValueUnit.FLAT, ""),
                defense("defense_block_chance", "szansy na blok", "+[%s - %s]% szansy na blok", 2.5d, 5.0d, TemperingValueUnit.PERCENT, ""),
                defense("defense_arbiter_armor_percent", "pancerza pod postacią Arbitra", "+[%s - %s]% pancerza pod postacią Arbitra", 7.0d, 10.0d, TemperingValueUnit.PERCENT, "Opisowe; wymaga przyszłej decyzji modelu Przysięgi/Arbitra.")
        );
    }

    public static ApplicationTemperingAffixRegistry get() {
        return INSTANCE;
    }

    private static TemperingAffixDefinition defense(String id,
                                                    String displayName,
                                                    String descriptionTemplate,
                                                    double rangeMin,
                                                    double rangeMax,
                                                    TemperingValueUnit unit,
                                                    String notes) {
        return defense(id, displayName, descriptionTemplate, rangeMin, rangeMax, unit, notes, null);
    }

    private static TemperingAffixDefinition offense(String id,
                                                    String displayName,
                                                    String descriptionTemplate,
                                                    double rangeMin,
                                                    double rangeMax,
                                                    TemperingValueUnit unit,
                                                    String notes) {
        return new TemperingAffixDefinition(
                id,
                TemperingCategory.OFFENSE,
                displayName,
                descriptionTemplate,
                rangeMin,
                rangeMax,
                unit,
                TemperingRuntimeStatus.DATA_ONLY,
                notes
        );
    }

    private static TemperingAffixDefinition defense(String id,
                                                    String displayName,
                                                    String descriptionTemplate,
                                                    double rangeMin,
                                                    double rangeMax,
                                                    TemperingValueUnit unit,
                                                    String notes,
                                                    Double greaterAffixValueOverride) {
        return new TemperingAffixDefinition(
                id,
                TemperingCategory.DEFENSE,
                displayName,
                descriptionTemplate,
                rangeMin,
                rangeMax,
                unit,
                TemperingRuntimeStatus.DATA_ONLY,
                notes,
                greaterAffixValueOverride
        );
    }

    @Override
    public List<TemperingAffixDefinition> all() {
        return definitions;
    }

    @Override
    public List<TemperingAffixDefinition> byCategory(TemperingCategory category) {
        List<TemperingAffixDefinition> result = new ArrayList<>();
        for (TemperingAffixDefinition definition : definitions) {
            if (definition.getCategory() == category) {
                result.add(definition);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public Optional<TemperingAffixDefinition> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return definitions.stream()
                .filter(definition -> definition.getId().equals(id))
                .findFirst();
    }
}
