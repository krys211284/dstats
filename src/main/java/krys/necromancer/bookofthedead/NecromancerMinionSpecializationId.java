package krys.necromancer.bookofthedead;

/** Stałe identyfikatory specjalizacji; nie są identyfikatorami skilli. */
public enum NecromancerMinionSpecializationId {
    SKIRMISHER("skirmisher", NecromancerMinionFamily.SKELETAL_WARRIOR),
    DEFENDER("defender", NecromancerMinionFamily.SKELETAL_WARRIOR),
    REAPER("reaper", NecromancerMinionFamily.SKELETAL_WARRIOR),
    SHADOW_MAGE("shadow_mage", NecromancerMinionFamily.SKELETAL_MAGE),
    COLD_MAGE("cold_mage", NecromancerMinionFamily.SKELETAL_MAGE),
    BONE_MAGE("bone_mage", NecromancerMinionFamily.SKELETAL_MAGE),
    BONE_GOLEM("bone_golem", NecromancerMinionFamily.GOLEM),
    BLOOD_GOLEM("blood_golem", NecromancerMinionFamily.GOLEM),
    IRON_GOLEM("iron_golem", NecromancerMinionFamily.GOLEM);

    private final String rawId;
    private final NecromancerMinionFamily family;

    NecromancerMinionSpecializationId(String rawId, NecromancerMinionFamily family) {
        this.rawId = rawId;
        this.family = family;
    }

    public String rawId() {
        return rawId;
    }

    public NecromancerMinionFamily family() {
        return family;
    }
}
