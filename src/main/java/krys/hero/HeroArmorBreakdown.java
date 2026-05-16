package krys.hero;

/** Rozbicie pancerza w statystykach bohatera; model prezentacyjny, nie wejście runtime DPS. */
public final class HeroArmorBreakdown {
    private final int armorFromStrength;
    private final int armorFromItems;
    private final int armorFromOtherSources;

    public HeroArmorBreakdown(int armorFromStrength, int armorFromItems, int armorFromOtherSources) {
        this.armorFromStrength = armorFromStrength;
        this.armorFromItems = armorFromItems;
        this.armorFromOtherSources = armorFromOtherSources;
    }

    public int getArmorFromStrength() {
        return armorFromStrength;
    }

    public int getArmorFromItems() {
        return armorFromItems;
    }

    public int getArmorFromOtherSources() {
        return armorFromOtherSources;
    }

    public int getTotalArmor() {
        return armorFromStrength + armorFromItems + armorFromOtherSources;
    }
}
