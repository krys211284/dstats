package krys.hero;

import java.math.BigDecimal;

/** Zweryfikowany baseline prezentacyjny statystyk klasy dla konkretnego poziomu i stanu bez itemów. */
public final class HeroClassStatBaseline {
    private final HeroClass heroClass;
    private final int level;
    private final int strength;
    private final int intelligence;
    private final int willpower;
    private final int dexterity;
    private final int toughness;
    private final HeroArmorBreakdown armorBreakdown;
    private final int maxHealth;
    private final int physicalResistance;
    private final int fireResistance;
    private final int lightningResistance;
    private final int coldResistance;
    private final int poisonResistance;
    private final int shadowResistance;
    private final long weaponDamage;
    private final BigDecimal weaponSpeed;
    private final HeroCriticalChanceBreakdown criticalChanceBreakdown;
    private final BigDecimal criticalDamagePercent;
    private final BigDecimal vulnerableDamagePercent;
    private final int thorns;

    public HeroClassStatBaseline(HeroClass heroClass,
                                 int level,
                                 int strength,
                                 int intelligence,
                                 int willpower,
                                 int dexterity,
                                 int toughness,
                                 HeroArmorBreakdown armorBreakdown,
                                 int maxHealth,
                                 int physicalResistance,
                                 int fireResistance,
                                 int lightningResistance,
                                 int coldResistance,
                                 int poisonResistance,
                                 int shadowResistance,
                                 long weaponDamage,
                                 BigDecimal weaponSpeed,
                                 HeroCriticalChanceBreakdown criticalChanceBreakdown,
                                 BigDecimal criticalDamagePercent,
                                 BigDecimal vulnerableDamagePercent,
                                 int thorns) {
        this.heroClass = heroClass;
        this.level = level;
        this.strength = strength;
        this.intelligence = intelligence;
        this.willpower = willpower;
        this.dexterity = dexterity;
        this.toughness = toughness;
        this.armorBreakdown = armorBreakdown;
        this.maxHealth = maxHealth;
        this.physicalResistance = physicalResistance;
        this.fireResistance = fireResistance;
        this.lightningResistance = lightningResistance;
        this.coldResistance = coldResistance;
        this.poisonResistance = poisonResistance;
        this.shadowResistance = shadowResistance;
        this.weaponDamage = weaponDamage;
        this.weaponSpeed = weaponSpeed;
        this.criticalChanceBreakdown = criticalChanceBreakdown;
        this.criticalDamagePercent = criticalDamagePercent;
        this.vulnerableDamagePercent = vulnerableDamagePercent;
        this.thorns = thorns;
    }

    public HeroClass getHeroClass() {
        return heroClass;
    }

    public int getLevel() {
        return level;
    }

    public int getStrength() {
        return strength;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public int getWillpower() {
        return willpower;
    }

    public int getDexterity() {
        return dexterity;
    }

    public int getToughness() {
        return toughness;
    }

    public int getArmor() {
        return armorBreakdown.getTotalArmor();
    }

    public HeroArmorBreakdown getArmorBreakdown() {
        return armorBreakdown;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getPhysicalResistance() {
        return physicalResistance;
    }

    public int getFireResistance() {
        return fireResistance;
    }

    public int getLightningResistance() {
        return lightningResistance;
    }

    public int getColdResistance() {
        return coldResistance;
    }

    public int getPoisonResistance() {
        return poisonResistance;
    }

    public int getShadowResistance() {
        return shadowResistance;
    }

    public long getWeaponDamage() {
        return weaponDamage;
    }

    public BigDecimal getWeaponSpeed() {
        return weaponSpeed;
    }

    public BigDecimal getCriticalChancePercent() {
        return criticalChanceBreakdown.getTotalCriticalChancePercent();
    }

    public HeroCriticalChanceBreakdown getCriticalChanceBreakdown() {
        return criticalChanceBreakdown;
    }

    public BigDecimal getCriticalDamagePercent() {
        return criticalDamagePercent;
    }

    public BigDecimal getVulnerableDamagePercent() {
        return vulnerableDamagePercent;
    }

    public int getThorns() {
        return thorns;
    }
}
