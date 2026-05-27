package krys.itemlibrary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** User-facing projekcja statystyk wynikających wyłącznie z aktywnie założonych itemów. */
public final class CurrentHeroActiveItemStats {
    private final Long weaponDps;
    private final Long weaponDamageMin;
    private final Long weaponDamageMax;
    private final Long averageWeaponDamage;
    private final Double attacksPerSecond;
    private final double maximumLifeFromItems;
    private final double flatWeaponDamageFromAffixes;
    private final double lifeOnHit;
    private final double luckyHitPrimaryResourceValue;
    private final double strength;
    private final double intelligence;
    private final double thorns;
    private final double blockChance;
    private final double retributionChance;
    private final double criticalChancePercent;
    private final long itemArmor;
    private final double fireResistance;
    private final double allResistance;
    private final double damageReduction;
    private final double maxAnimusFromTempering;
    private final List<String> descriptiveAffixes;
    private final List<String> statisticalAffixes;
    private final List<String> descriptiveEffectAffixes;
    private final List<String> maxAnimusTemperingSources;

    public CurrentHeroActiveItemStats(Long weaponDps,
                                      Long weaponDamageMin,
                                      Long weaponDamageMax,
                                      Long averageWeaponDamage,
                                      Double attacksPerSecond,
                                      double maximumLifeFromItems,
                                      double flatWeaponDamageFromAffixes,
                                      double lifeOnHit,
                                      double luckyHitPrimaryResourceValue,
                                      double strength,
                                      double intelligence,
                                      double thorns,
                                      double blockChance,
                                      double retributionChance,
                                      List<String> descriptiveAffixes) {
        this(weaponDps, weaponDamageMin, weaponDamageMax, averageWeaponDamage, attacksPerSecond,
                maximumLifeFromItems, flatWeaponDamageFromAffixes, lifeOnHit, luckyHitPrimaryResourceValue,
                strength, intelligence, thorns, blockChance, retributionChance, 0.0d,
                0L, 0.0d, 0.0d, 0.0d, 0.0d,
                descriptiveAffixes, List.of(), List.of(), List.of());
    }

    public CurrentHeroActiveItemStats(Long weaponDps,
                                      Long weaponDamageMin,
                                      Long weaponDamageMax,
                                      Long averageWeaponDamage,
                                      Double attacksPerSecond,
                                      double maximumLifeFromItems,
                                      double flatWeaponDamageFromAffixes,
                                      double lifeOnHit,
                                      double luckyHitPrimaryResourceValue,
                                      double strength,
                                      double intelligence,
                                      double thorns,
                                      double blockChance,
                                      double retributionChance,
                                      double criticalChancePercent,
                                      long itemArmor,
                                      double fireResistance,
                                      double allResistance,
                                      double damageReduction,
                                      double maxAnimusFromTempering,
                                      List<String> descriptiveAffixes,
                                      List<String> statisticalAffixes,
                                      List<String> descriptiveEffectAffixes,
                                      List<String> maxAnimusTemperingSources) {
        this.weaponDps = weaponDps;
        this.weaponDamageMin = weaponDamageMin;
        this.weaponDamageMax = weaponDamageMax;
        this.averageWeaponDamage = averageWeaponDamage;
        this.attacksPerSecond = attacksPerSecond;
        this.maximumLifeFromItems = maximumLifeFromItems;
        this.flatWeaponDamageFromAffixes = flatWeaponDamageFromAffixes;
        this.lifeOnHit = lifeOnHit;
        this.luckyHitPrimaryResourceValue = luckyHitPrimaryResourceValue;
        this.strength = strength;
        this.intelligence = intelligence;
        this.thorns = thorns;
        this.blockChance = blockChance;
        this.retributionChance = retributionChance;
        this.criticalChancePercent = criticalChancePercent;
        this.itemArmor = itemArmor;
        this.fireResistance = fireResistance;
        this.allResistance = allResistance;
        this.damageReduction = damageReduction;
        this.maxAnimusFromTempering = maxAnimusFromTempering;
        this.descriptiveAffixes = Collections.unmodifiableList(new ArrayList<>(descriptiveAffixes == null ? List.of() : descriptiveAffixes));
        this.statisticalAffixes = Collections.unmodifiableList(new ArrayList<>(statisticalAffixes == null ? List.of() : statisticalAffixes));
        this.descriptiveEffectAffixes = Collections.unmodifiableList(new ArrayList<>(descriptiveEffectAffixes == null ? List.of() : descriptiveEffectAffixes));
        this.maxAnimusTemperingSources = Collections.unmodifiableList(new ArrayList<>(maxAnimusTemperingSources == null ? List.of() : maxAnimusTemperingSources));
    }

    public static CurrentHeroActiveItemStats empty() {
        return new CurrentHeroActiveItemStats(null, null, null, null, null,
                0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, List.of());
    }

    public Long getWeaponDps() {
        return weaponDps;
    }

    public Long getWeaponDamageMin() {
        return weaponDamageMin;
    }

    public Long getWeaponDamageMax() {
        return weaponDamageMax;
    }

    public Long getAverageWeaponDamage() {
        return averageWeaponDamage;
    }

    public Double getAttacksPerSecond() {
        return attacksPerSecond;
    }

    public double getMaximumLifeFromItems() {
        return maximumLifeFromItems;
    }

    public double getFlatWeaponDamageFromAffixes() {
        return flatWeaponDamageFromAffixes;
    }

    public double getLifeOnHit() {
        return lifeOnHit;
    }

    public double getLuckyHitPrimaryResourceValue() {
        return luckyHitPrimaryResourceValue;
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

    public long getItemArmor() {
        return itemArmor;
    }

    public double getFireResistance() {
        return fireResistance;
    }

    public double getAllResistance() {
        return allResistance;
    }

    public double getDamageReduction() {
        return damageReduction;
    }

    public double getMaxAnimusFromTempering() {
        return maxAnimusFromTempering;
    }

    public List<String> getDescriptiveAffixes() {
        return descriptiveAffixes;
    }

    public List<String> getStatisticalAffixes() {
        return statisticalAffixes;
    }

    public List<String> getDescriptiveEffectAffixes() {
        return descriptiveEffectAffixes;
    }

    public List<String> getMaxAnimusTemperingSources() {
        return maxAnimusTemperingSources;
    }

    public boolean hasActiveWeaponDetails() {
        return weaponDps != null
                || weaponDamageMin != null
                || weaponDamageMax != null
                || averageWeaponDamage != null
                || attacksPerSecond != null;
    }

    public boolean hasDescriptiveAffixes() {
        return !descriptiveAffixes.isEmpty();
    }

    public boolean hasGroupedAffixes() {
        return !statisticalAffixes.isEmpty() || !descriptiveEffectAffixes.isEmpty();
    }
}
