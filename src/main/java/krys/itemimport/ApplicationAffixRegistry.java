package krys.itemimport;

import java.util.List;

/** Wspólny punkt dostępu do katalogu affixów aplikacji. */
public final class ApplicationAffixRegistry {
    private static final List<AffixDefinition> SEED_DEFINITIONS = List.of(
            new AffixDefinition(
                    "verathiel_weapon_damage_flat",
                    ImportedItemAffixType.WEAPON_DAMAGE_FLAT,
                    "Obrażenia od broni",
                    AffixCategory.OFFENSIVE,
                    List.of("obrażeń od broni", "obrażen od broni", "obrażen od broni", "obrazen od broni"),
                    AffixValueUnit.FLAT,
                    94.0d,
                    94.0d,
                    157.0d,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "verathiel_maximum_life",
                    ImportedItemAffixType.MAXIMUM_LIFE,
                    "Maksymalne zdrowie",
                    AffixCategory.DEFENSIVE,
                    List.of("maksymalnego zdrowia", "maksymalne zdrowie", "maximum life"),
                    AffixValueUnit.FLAT,
                    2141.0d,
                    1831.0d,
                    2200.0d,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "verathiel_life_on_hit",
                    ImportedItemAffixType.LIFE_ON_HIT,
                    "Zdrowie przy trafieniu",
                    AffixCategory.DEFENSIVE,
                    List.of("pkt. zdrowia przy trafieniu", "zdrowia przy trafieniu", "life on hit"),
                    AffixValueUnit.POINTS,
                    545.0d,
                    526.0d,
                    632.0d,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "verathiel_lucky_hit_primary_resource",
                    ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE,
                    "Szczęśliwy traf: odzyskanie podstawowego zasobu",
                    AffixCategory.RESOURCE,
                    List.of("Szczęśliwy traf", "Szczesliwy traf", "maksymalnie 15% szans", "odzyskanie +3 podstawowego zasobu"),
                    AffixValueUnit.TEXT,
                    3.0d,
                    3.0d,
                    4.0d,
                    15.0d,
                    3.0d,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "strength",
                    ImportedItemAffixType.STRENGTH,
                    "Siła",
                    AffixCategory.ATTRIBUTE,
                    List.of("siły", "sily", "strength"),
                    AffixValueUnit.FLAT,
                    null,
                    null,
                    null,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "intelligence",
                    ImportedItemAffixType.INTELLIGENCE,
                    "Inteligencja",
                    AffixCategory.ATTRIBUTE,
                    List.of("inteligencji", "intelligence"),
                    AffixValueUnit.FLAT,
                    null,
                    null,
                    null,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "thorns",
                    ImportedItemAffixType.THORNS,
                    "Ciernie",
                    AffixCategory.DEFENSIVE,
                    List.of("cierni", "thorns"),
                    AffixValueUnit.POINTS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "block_chance",
                    ImportedItemAffixType.BLOCK_CHANCE,
                    "Szansa na blok",
                    AffixCategory.DEFENSIVE,
                    List.of("szansy na blok", "szansa na blok", "block chance"),
                    AffixValueUnit.PERCENT,
                    null,
                    null,
                    null,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "retribution_chance",
                    ImportedItemAffixType.RETRIBUTION_CHANCE,
                    "Szansa retribution",
                    AffixCategory.UTILITY,
                    List.of("retribution", "odwet"),
                    AffixValueUnit.PERCENT,
                    null,
                    null,
                    null,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "lucky_hit_chance",
                    ImportedItemAffixType.LUCKY_HIT_CHANCE,
                    "Szansa na szczęśliwy traf",
                    AffixCategory.UTILITY,
                    List.of("szansy na szczęśliwy traf", "szansa na szczęśliwy traf", "lucky hit chance"),
                    AffixValueUnit.PERCENT,
                    null,
                    null,
                    null,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "cooldown_reduction",
                    ImportedItemAffixType.COOLDOWN_REDUCTION,
                    "Redukcja czasu odnowienia",
                    AffixCategory.UTILITY,
                    List.of("redukcji czasu odnowienia", "cooldown"),
                    AffixValueUnit.PERCENT,
                    null,
                    null,
                    null,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "movement_speed",
                    ImportedItemAffixType.MOVEMENT_SPEED,
                    "Szybkość ruchu",
                    AffixCategory.UTILITY,
                    List.of("szybkości ruchu", "szybkosci ruchu", "movement speed"),
                    AffixValueUnit.PERCENT,
                    null,
                    null,
                    null,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            ),
            new AffixDefinition(
                    "dodge_chance",
                    ImportedItemAffixType.DODGE_CHANCE,
                    "Unik",
                    AffixCategory.DEFENSIVE,
                    List.of("uniku", "dodge"),
                    AffixValueUnit.PERCENT,
                    null,
                    null,
                    null,
                    null,
                    null,
                    AffixRuntimeStatus.DESCRIPTIVE_ONLY,
                    true,
                    true
            )
    );

    private static final AffixRegistry INSTANCE = new AffixRegistry(SEED_DEFINITIONS);

    private ApplicationAffixRegistry() {
    }

    public static AffixRegistry get() {
        return INSTANCE;
    }

    static List<AffixDefinition> seedDefinitions() {
        return SEED_DEFINITIONS;
    }
}
