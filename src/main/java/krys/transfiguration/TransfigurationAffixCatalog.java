package krys.transfiguration;

import java.util.List;
import java.util.Optional;

/** Katalog zwykłych affixów Przeistoczenia z lokalnej kopii strony Maxroll. */
public final class TransfigurationAffixCatalog {
    private static final List<String> ELEMENT_OPTIONS = List.of(
            "Cold", "Fire", "Holy", "Lightning", "Physical", "Poison", "Shadow"
    );

    private static final List<TransfigurationAffixDefinition> DEFINITIONS = List.of(
            definition("ALL_STATS", "All Stats", "All Stats [+]", 75, 100, TransfigurationAffixValueKind.FLAT),
            definition("ATTACK_SPEED", "Attack Speed", "Attack Speed [+]", 8, 10, TransfigurationAffixValueKind.PERCENT),
            definition("COOLDOWN_REDUCTION", "Cooldown Reduction", "Cooldown Reduction", 10, 12, TransfigurationAffixValueKind.PERCENT),
            definition("CRITICAL_STRIKE_CHANCE", "Critical Strike Chance", "Critical Strike Chance [+]", 3.5, 5, TransfigurationAffixValueKind.PERCENT),
            new TransfigurationAffixDefinition(
                    "ELEMENTAL_SPECIFIC_DAMAGE",
                    "Elemental specific Damage",
                    "Elemental specific Damage [x]",
                    8,
                    10,
                    TransfigurationAffixValueKind.MULTIPLICATIVE_PERCENT,
                    true,
                    "Element może być: Cold, Fire, Holy, Lightning, Physical, Poison, Shadow.",
                    ELEMENT_OPTIONS),
            definition("GEM_STRENGTH", "Gem Strength", "Gem Strength for an item [x]", 75, 100, TransfigurationAffixValueKind.MULTIPLICATIVE_PERCENT),
            definition("LIFE_ON_HIT", "Life on Hit", "Life on Hit", 263, 316, TransfigurationAffixValueKind.FLAT),
            definition("LUCKY_HIT_CHANCE", "Lucky Hit Chance", "Lucky Hit Chance", 6, 8, TransfigurationAffixValueKind.PERCENT),
            definition("MAX_LIFE_PERCENT", "Max Life%", "Max Life% [+]", 6, 8, TransfigurationAffixValueKind.PERCENT),
            definition("MAX_RESOURCE", "Max Resource", "Max Resource [+]", 15, 20, TransfigurationAffixValueKind.FLAT),
            definition("MOVEMENT_SPEED", "Movement Speed", "Movement Speed [+]", 20, 30, TransfigurationAffixValueKind.PERCENT),
            definition("PRIMARY_STAT", "Primary Stat", "Primary Stat [+]", 150, 180, TransfigurationAffixValueKind.FLAT),
            definition("PRIMARY_STAT_PERCENT", "Primary Stat%", "Primary Stat% [+]", 3.5, 5, TransfigurationAffixValueKind.PERCENT),
            definition("RESOURCE_COST_REDUCTION", "Resource Cost Reduction", "Resource Cost Reduction", 6, 8, TransfigurationAffixValueKind.PERCENT),
            definition("TOTAL_ARMOR_PERCENT", "Total Armor%", "Total Armor% [+]", 8, 10, TransfigurationAffixValueKind.PERCENT),
            definition("TOTAL_RESISTANCE_PERCENT", "Total Resistance%", "Total Resistance% [+]", 8, 10, TransfigurationAffixValueKind.PERCENT)
    );

    private TransfigurationAffixCatalog() {
    }

    public static List<TransfigurationAffixDefinition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<TransfigurationAffixDefinition> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return DEFINITIONS.stream()
                .filter(definition -> definition.getId().equals(id))
                .findFirst();
    }

    private static TransfigurationAffixDefinition definition(String id,
                                                            String displayName,
                                                            String sourceName,
                                                            double min,
                                                            double max,
                                                            TransfigurationAffixValueKind valueKind) {
        return new TransfigurationAffixDefinition(id, displayName, sourceName, min, max, valueKind,
                true, "Zakres źródłowy dla non-2H weapons; broń dwuręczna ma wartości podwojone.", List.of());
    }
}
