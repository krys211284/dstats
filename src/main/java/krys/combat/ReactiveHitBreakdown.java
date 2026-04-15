package krys.combat;

/**
 * Debug pojedynczego zdarzenia reactive damage wywołanego trafieniem przeciwnika.
 * M5a obejmuje foundation dla Thorns i deterministycznego expected value Retribution.
 */
public final class ReactiveHitBreakdown {
    private final int triggeredSecond;
    private final double baseThornsFromBuild;
    private final double mainStatMultiplier;
    private final double blockChance;
    private final double retributionChance;
    private final long thornsRawDamage;
    private final long thornsFinalDamage;
    private final long retributionExpectedRawDamage;
    private final long retributionExpectedFinalDamage;
    private final long reactiveFinalDamage;

    public ReactiveHitBreakdown(int triggeredSecond,
                                double baseThornsFromBuild,
                                double mainStatMultiplier,
                                double blockChance,
                                double retributionChance,
                                long thornsRawDamage,
                                long thornsFinalDamage,
                                long retributionExpectedRawDamage,
                                long retributionExpectedFinalDamage,
                                long reactiveFinalDamage) {
        this.triggeredSecond = triggeredSecond;
        this.baseThornsFromBuild = baseThornsFromBuild;
        this.mainStatMultiplier = mainStatMultiplier;
        this.blockChance = blockChance;
        this.retributionChance = retributionChance;
        this.thornsRawDamage = thornsRawDamage;
        this.thornsFinalDamage = thornsFinalDamage;
        this.retributionExpectedRawDamage = retributionExpectedRawDamage;
        this.retributionExpectedFinalDamage = retributionExpectedFinalDamage;
        this.reactiveFinalDamage = reactiveFinalDamage;
    }

    public int getTriggeredSecond() {
        return triggeredSecond;
    }

    public double getBaseThornsFromBuild() {
        return baseThornsFromBuild;
    }

    public double getMainStatMultiplier() {
        return mainStatMultiplier;
    }

    public double getBlockChance() {
        return blockChance;
    }

    public double getRetributionChance() {
        return retributionChance;
    }

    public long getThornsRawDamage() {
        return thornsRawDamage;
    }

    public long getThornsFinalDamage() {
        return thornsFinalDamage;
    }

    public long getRetributionExpectedRawDamage() {
        return retributionExpectedRawDamage;
    }

    public long getRetributionExpectedFinalDamage() {
        return retributionExpectedFinalDamage;
    }

    public long getReactiveFinalDamage() {
        return reactiveFinalDamage;
    }
}
