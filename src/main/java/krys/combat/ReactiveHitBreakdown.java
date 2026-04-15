package krys.combat;

/**
 * Debug pojedynczego zdarzenia reactive damage wywołanego trafieniem przeciwnika.
 * M6 rozszerza foundation o Clash, Resolve i czasowy bonus do Thorns bez osobnego silnika reactive.
 */
public final class ReactiveHitBreakdown {
    private final int triggeredSecond;
    private final double baseThornsFromBuild;
    private final double mainStatMultiplier;
    private final double blockChanceFromBuild;
    private final double activeBlockChance;
    private final double activeThornsBonus;
    private final double retributionChance;
    private final boolean resolveActive;
    private final int resolveRemainingSeconds;
    private final boolean punishmentActive;
    private final long thornsRawDamage;
    private final long thornsFinalDamage;
    private final long retributionExpectedRawDamage;
    private final long retributionExpectedFinalDamage;
    private final long reactiveFinalDamage;

    public ReactiveHitBreakdown(int triggeredSecond,
                                double baseThornsFromBuild,
                                double mainStatMultiplier,
                                double blockChanceFromBuild,
                                double activeBlockChance,
                                double activeThornsBonus,
                                double retributionChance,
                                boolean resolveActive,
                                int resolveRemainingSeconds,
                                boolean punishmentActive,
                                long thornsRawDamage,
                                long thornsFinalDamage,
                                long retributionExpectedRawDamage,
                                long retributionExpectedFinalDamage,
                                long reactiveFinalDamage) {
        this.triggeredSecond = triggeredSecond;
        this.baseThornsFromBuild = baseThornsFromBuild;
        this.mainStatMultiplier = mainStatMultiplier;
        this.blockChanceFromBuild = blockChanceFromBuild;
        this.activeBlockChance = activeBlockChance;
        this.activeThornsBonus = activeThornsBonus;
        this.retributionChance = retributionChance;
        this.resolveActive = resolveActive;
        this.resolveRemainingSeconds = resolveRemainingSeconds;
        this.punishmentActive = punishmentActive;
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

    public double getBlockChanceFromBuild() {
        return blockChanceFromBuild;
    }

    public double getActiveBlockChance() {
        return activeBlockChance;
    }

    public double getActiveThornsBonus() {
        return activeThornsBonus;
    }

    public double getRetributionChance() {
        return retributionChance;
    }

    public boolean isResolveActive() {
        return resolveActive;
    }

    public int getResolveRemainingSeconds() {
        return resolveRemainingSeconds;
    }

    public boolean isPunishmentActive() {
        return punishmentActive;
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
