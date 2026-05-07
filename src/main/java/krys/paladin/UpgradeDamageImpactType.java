package krys.paladin;

/** Opisowy typ wpływu ulepszenia na obrażenia, bez liczenia DPS. */
public enum UpgradeDamageImpactType {
    DIRECT_DAMAGE_PERCENT,
    ADDITIONAL_HIT,
    DAMAGE_OVER_TIME,
    BURST_DAMAGE,
    CONDITIONAL_DAMAGE,
    STATUS_OR_UTILITY,
    COOLDOWN_OR_COST,
    NO_DAMAGE_IMPACT,
    NEEDS_VERIFICATION
}
