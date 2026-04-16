package krys.itemimport;

/** Wkład pojedynczego zaimportowanego itemu do aktualnego modelu agregowanych statów buildu. */
public final class ImportedItemCurrentBuildContribution {
    private final long weaponDamage;
    private final double strength;
    private final double intelligence;
    private final double thorns;
    private final double blockChance;
    private final double retributionChance;

    public ImportedItemCurrentBuildContribution(long weaponDamage,
                                                double strength,
                                                double intelligence,
                                                double thorns,
                                                double blockChance,
                                                double retributionChance) {
        this.weaponDamage = weaponDamage;
        this.strength = strength;
        this.intelligence = intelligence;
        this.thorns = thorns;
        this.blockChance = blockChance;
        this.retributionChance = retributionChance;
    }

    public long getWeaponDamage() {
        return weaponDamage;
    }

    public double getStrength() {
        return strength;
    }

    public double getIntelligence() {
        return intelligence;
    }

    public double getThorns() {
        return thorns;
    }

    public double getBlockChance() {
        return blockChance;
    }

    public double getRetributionChance() {
        return retributionChance;
    }
}
