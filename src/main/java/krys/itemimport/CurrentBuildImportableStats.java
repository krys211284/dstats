package krys.itemimport;

/** Agregowane pola current build, na które może wpływać import pojedynczego itemu. */
public final class CurrentBuildImportableStats {
    private final long weaponDamage;
    private final double strength;
    private final double intelligence;
    private final double thorns;
    private final double blockChance;
    private final double retributionChance;
    private final double criticalChancePercent;

    public CurrentBuildImportableStats(long weaponDamage,
                                       double strength,
                                       double intelligence,
                                       double thorns,
                                       double blockChance,
                                       double retributionChance) {
        this(weaponDamage, strength, intelligence, thorns, blockChance, retributionChance, 0.0d);
    }

    public CurrentBuildImportableStats(long weaponDamage,
                                       double strength,
                                       double intelligence,
                                       double thorns,
                                       double blockChance,
                                       double retributionChance,
                                       double criticalChancePercent) {
        this.weaponDamage = weaponDamage;
        this.strength = strength;
        this.intelligence = intelligence;
        this.thorns = thorns;
        this.blockChance = blockChance;
        this.retributionChance = retributionChance;
        this.criticalChancePercent = criticalChancePercent;
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

    public double getCriticalChancePercent() {
        return criticalChancePercent;
    }
}
