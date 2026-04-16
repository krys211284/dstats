package krys.itemimport;

import krys.item.Item;
import krys.item.ItemStat;
import krys.item.ItemStatType;

import java.util.ArrayList;
import java.util.List;

/** Mapuje zatwierdzony item importu do aktualnego modelu statów itemu używanego przez runtime. */
public final class ValidatedImportedItemToItemMapper {
    public Item map(ValidatedImportedItem importedItem) {
        List<ItemStat> stats = new ArrayList<>();
        addStatIfPositive(stats, ItemStatType.STRENGTH, importedItem.getStrength());
        addStatIfPositive(stats, ItemStatType.INTELLIGENCE, importedItem.getIntelligence());
        addStatIfPositive(stats, ItemStatType.THORNS, importedItem.getThorns());
        addStatIfPositive(stats, ItemStatType.BLOCK_CHANCE, importedItem.getBlockChance());
        addStatIfPositive(stats, ItemStatType.RETRIBUTION_CHANCE, importedItem.getRetributionChance());
        return new Item(
                Math.abs(importedItem.getSourceImageName().hashCode()) + 1000,
                "Zaimportowany item: " + importedItem.getSourceImageName(),
                importedItem.getSlot(),
                stats
        );
    }

    private static void addStatIfPositive(List<ItemStat> stats, ItemStatType type, double value) {
        if (value > 0.0d) {
            stats.add(new ItemStat(type, value));
        }
    }
}
