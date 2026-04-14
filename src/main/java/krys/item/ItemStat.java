package krys.item;

/** Pojedynczy stat itemu używany przez snapshot buildu i Damage Engine. */
public final class ItemStat {
    private final ItemStatType type;
    private final double value;

    public ItemStat(ItemStatType type, double value) {
        this.type = type;
        this.value = value;
    }

    public ItemStatType getType() {
        return type;
    }

    public double getValue() {
        return value;
    }
}
