package krys.verification;

/** Kategorie pytań, które muszą przejść osobną weryfikację empiryczną. */
public enum VerificationCategory {
    SINGLE_TARGET_HIT_COUNT("singleTargetHitCount"),
    EXTRA_PROJECTILE_HIT_BEHAVIOR("extraProjectileHitBehavior"),
    RICOCHET_OR_BOUNCE_BEHAVIOR("ricochetOrBounceBehavior"),
    DELAYED_EXPLOSION_BEHAVIOR("delayedExplosionBehavior"),
    DOT_TICK_RATE("dotTickRate"),
    STATUS_APPLICATION_ORDER("statusApplicationOrder"),
    COOLDOWN_REDUCTION_TIMING("cooldownReductionTiming"),
    RESOURCE_COST_OR_GENERATION("resourceCostOrGeneration"),
    DURATION_OR_REFRESH_BEHAVIOR("durationOrRefreshBehavior"),
    REPLACEMENT_VS_ADDITIONAL_DAMAGE_COMPONENT("replacementVsAdditionalDamageComponent"),
    BOSS_CONTROL_BEHAVIOR("bossControlBehavior"),
    AURA_PASSIVE_VS_ACTIVE_BEHAVIOR("auraPassiveVsActiveBehavior");

    private final String id;

    VerificationCategory(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
