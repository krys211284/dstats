package krys.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pełny wynik obliczenia pojedynczego uderzenia wraz z danymi debugowymi. */
public final class DamageBreakdown {
    private final long baseDamage;
    private final long rawDamage;
    private final long finalDamage;
    private final long rawCriticalDamage;
    private final long criticalDamage;
    private final double weaponMultiplier;
    private final double mainStat;
    private final double mainStatMultiplier;
    private final double additivePercent;
    private final double additiveMultiplier;
    private final double vulnerableMultiplier;
    private final double critMultiplier;
    private final double critDamageBonusTotal;
    private final double critDamageBonusFromItems;
    private final double critDamageBonusFromIntelligence;
    private final double intelligence;
    private final double levelDamageReduction;
    private final String selectedModifierName;
    private final List<DamageComponentBreakdown> components;

    public DamageBreakdown(long baseDamage,
                           long rawDamage,
                           long finalDamage,
                           long rawCriticalDamage,
                           long criticalDamage,
                           double weaponMultiplier,
                           double mainStat,
                           double mainStatMultiplier,
                           double additivePercent,
                           double additiveMultiplier,
                           double vulnerableMultiplier,
                           double critMultiplier,
                           double critDamageBonusTotal,
                           double critDamageBonusFromItems,
                           double critDamageBonusFromIntelligence,
                           double intelligence,
                           double levelDamageReduction,
                           String selectedModifierName,
                           List<DamageComponentBreakdown> components) {
        this.baseDamage = baseDamage;
        this.rawDamage = rawDamage;
        this.finalDamage = finalDamage;
        this.rawCriticalDamage = rawCriticalDamage;
        this.criticalDamage = criticalDamage;
        this.weaponMultiplier = weaponMultiplier;
        this.mainStat = mainStat;
        this.mainStatMultiplier = mainStatMultiplier;
        this.additivePercent = additivePercent;
        this.additiveMultiplier = additiveMultiplier;
        this.vulnerableMultiplier = vulnerableMultiplier;
        this.critMultiplier = critMultiplier;
        this.critDamageBonusTotal = critDamageBonusTotal;
        this.critDamageBonusFromItems = critDamageBonusFromItems;
        this.critDamageBonusFromIntelligence = critDamageBonusFromIntelligence;
        this.intelligence = intelligence;
        this.levelDamageReduction = levelDamageReduction;
        this.selectedModifierName = selectedModifierName;
        this.components = Collections.unmodifiableList(new ArrayList<>(components));
    }

    public long getBaseDamage() {
        return baseDamage;
    }

    public long getRawDamage() {
        return rawDamage;
    }

    public long getFinalDamage() {
        return finalDamage;
    }

    public long getRawCriticalDamage() {
        return rawCriticalDamage;
    }

    public long getCriticalDamage() {
        return criticalDamage;
    }

    public double getWeaponMultiplier() {
        return weaponMultiplier;
    }

    public double getMainStat() {
        return mainStat;
    }

    public double getMainStatMultiplier() {
        return mainStatMultiplier;
    }

    public double getAdditivePercent() {
        return additivePercent;
    }

    public double getAdditiveMultiplier() {
        return additiveMultiplier;
    }

    public double getVulnerableMultiplier() {
        return vulnerableMultiplier;
    }

    public double getCritMultiplier() {
        return critMultiplier;
    }

    public double getCritDamageBonusTotal() {
        return critDamageBonusTotal;
    }

    public double getCritDamageBonusFromItems() {
        return critDamageBonusFromItems;
    }

    public double getCritDamageBonusFromIntelligence() {
        return critDamageBonusFromIntelligence;
    }

    public double getIntelligence() {
        return intelligence;
    }

    public double getLevelDamageReduction() {
        return levelDamageReduction;
    }

    public String getSelectedModifierName() {
        return selectedModifierName;
    }

    public List<DamageComponentBreakdown> getComponents() {
        return components;
    }
}
