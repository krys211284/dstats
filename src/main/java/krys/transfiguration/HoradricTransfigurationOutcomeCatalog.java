package krys.transfiguration;

import java.util.List;
import java.util.Optional;

/** Katalog regularnych wyników Przeistoczenia z lokalnych źródeł użytkownika. */
public final class HoradricTransfigurationOutcomeCatalog {
    private static final List<HoradricTransfigurationOutcomeDefinition> DEFINITIONS = List.of(
            new HoradricTransfigurationOutcomeDefinition(
                    HoradricTransfigurationOutcome.INDESTRUCTIBLE,
                    "Item nie traci wytrzymałości.",
                    "~20%"),
            new HoradricTransfigurationOutcomeDefinition(
                    HoradricTransfigurationOutcome.UPGRADE_TO_GREATER_AFFIX,
                    "Jeden zwykły non-GA affix zostaje ulepszony do Greater Affix.",
                    "~15%"),
            new HoradricTransfigurationOutcomeDefinition(
                    HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                    "Item dostaje dodatkowy specjalny affix z puli Przeistoczenia.",
                    "~35%"),
            new HoradricTransfigurationOutcomeDefinition(
                    HoradricTransfigurationOutcome.REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX,
                    "Jeden zwykły affix zostaje zastąpiony specjalnym affixem z puli Przeistoczenia.",
                    "~10%"),
            new HoradricTransfigurationOutcomeDefinition(
                    HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY,
                    "Item dostaje +1..15 dodatkowej jakości; broń dwuręczna +2..30.",
                    "~20%")
    );

    private HoradricTransfigurationOutcomeCatalog() {
    }

    public static List<HoradricTransfigurationOutcomeDefinition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<HoradricTransfigurationOutcomeDefinition> find(HoradricTransfigurationOutcome outcome) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.getOutcome() == outcome)
                .findFirst();
    }
}
