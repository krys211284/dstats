package krys.socketing;

/** Jedna definicja gema z katalogu GemCatalog v1. */
public final class GemDefinition {
    private final String id;
    private final GemFamily family;
    private final GemTier tier;
    private final String displayName;
    private final String rarityLabel;
    private final Integer requiredLevel;
    private final GemEffect weaponEffect;
    private final GemEffect armorEffect;
    private final GemEffect jewelryEffect;
    private final GemValueVerificationStatus verificationStatus;

    public GemDefinition(String id,
                         GemFamily family,
                         GemTier tier,
                         String displayName,
                         String rarityLabel,
                         Integer requiredLevel,
                         String weaponEffect,
                         String armorEffect,
                         String jewelryEffect,
                         GemValueVerificationStatus verificationStatus) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id gema jest wymagane.");
        }
        if (family == null || tier == null) {
            throw new IllegalArgumentException("Rodzina i tier gema są wymagane.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Nazwa gema jest wymagana.");
        }
        if (rarityLabel == null || rarityLabel.isBlank()) {
            throw new IllegalArgumentException("Rzadkość gema jest wymagana.");
        }
        if (verificationStatus == null) {
            throw new IllegalArgumentException("Status weryfikacji gema jest wymagany.");
        }
        this.id = id;
        this.family = family;
        this.tier = tier;
        this.displayName = displayName;
        this.rarityLabel = rarityLabel;
        this.requiredLevel = requiredLevel;
        this.weaponEffect = new GemEffect(SocketEffectContext.WEAPON, weaponEffect);
        this.armorEffect = new GemEffect(SocketEffectContext.ARMOR, armorEffect);
        this.jewelryEffect = new GemEffect(SocketEffectContext.JEWELRY, jewelryEffect);
        this.verificationStatus = verificationStatus;
    }

    public String getId() {
        return id;
    }

    public GemFamily getFamily() {
        return family;
    }

    public GemTier getTier() {
        return tier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRarityLabel() {
        return rarityLabel;
    }

    public Integer getRequiredLevel() {
        return requiredLevel;
    }

    public GemEffect getWeaponEffect() {
        return weaponEffect;
    }

    public GemEffect getArmorEffect() {
        return armorEffect;
    }

    public GemEffect getJewelryEffect() {
        return jewelryEffect;
    }

    public GemEffect effectFor(SocketEffectContext context) {
        return switch (context) {
            case WEAPON -> weaponEffect;
            case ARMOR -> armorEffect;
            case JEWELRY -> jewelryEffect;
        };
    }

    public GemValueVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }
}
