package krys.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Minimalny model itemu potrzebny do agregacji statów w Damage Engine. */
public final class Item {
    private final int id;
    private final String name;
    private final EquipmentSlot slot;
    private final List<ItemStat> stats;

    public Item(int id, String name, EquipmentSlot slot, List<ItemStat> stats) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.stats = Collections.unmodifiableList(new ArrayList<>(stats));
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public List<ItemStat> getStats() {
        return stats;
    }

    public static double sumStat(List<Item> items, ItemStatType statType) {
        return items.stream()
                .flatMap(item -> item.getStats().stream())
                .filter(stat -> stat.getType() == statType)
                .mapToDouble(ItemStat::getValue)
                .sum();
    }
}
