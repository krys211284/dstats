package krys.skill;

/**
 * Minimalny profil self-buffa reactive nakładanego przez skill na bohatera.
 * M6 wykorzystuje go do Clash bez dokładania osobnego silnika ani bocznej logiki.
 */
public final class ReactiveSelfBuffProfile {
    private final boolean grantsResolve;
    private final int durationSeconds;
    private final double blockChanceBonusPercent;
    private final double thornsBonus;

    public ReactiveSelfBuffProfile(boolean grantsResolve,
                                   int durationSeconds,
                                   double blockChanceBonusPercent,
                                   double thornsBonus) {
        this.grantsResolve = grantsResolve;
        this.durationSeconds = durationSeconds;
        this.blockChanceBonusPercent = blockChanceBonusPercent;
        this.thornsBonus = thornsBonus;
    }

    public boolean isGrantsResolve() {
        return grantsResolve;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public double getBlockChanceBonusPercent() {
        return blockChanceBonusPercent;
    }

    public double getThornsBonus() {
        return thornsBonus;
    }
}
