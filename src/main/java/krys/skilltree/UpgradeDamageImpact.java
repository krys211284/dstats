package krys.skilltree;

import java.util.Objects;

/** Opisowy wpływ pojedynczego ulepszenia na obrażenia, bez wykonywania kalkulacji. */
public final class UpgradeDamageImpact {
    private final String upgradeName;
    private final String groupId;
    private final UpgradeDamageImpactType type;
    private final String description;
    private final Integer damagePercent;

    public UpgradeDamageImpact(String upgradeName,
                               String groupId,
                               UpgradeDamageImpactType type,
                               String description,
                               Integer damagePercent) {
        this.upgradeName = requireText(upgradeName, "upgradeName");
        this.groupId = requireText(groupId, "groupId");
        this.type = Objects.requireNonNull(type, "type");
        this.description = requireText(description, "description");
        this.damagePercent = damagePercent;
    }

    public String getUpgradeName() {
        return upgradeName;
    }

    public String getGroupId() {
        return groupId;
    }

    public UpgradeDamageImpactType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDamagePercent() {
        return damagePercent;
    }

    public boolean affectsDamage() {
        return switch (type) {
            case DIRECT_DAMAGE_PERCENT,
                    ADDITIONAL_HIT,
                    DAMAGE_OVER_TIME,
                    BURST_DAMAGE,
                    CONDITIONAL_DAMAGE,
                    NEEDS_VERIFICATION -> true;
            case STATUS_OR_UTILITY, COOLDOWN_OR_COST, NO_DAMAGE_IMPACT -> false;
        };
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }
}
