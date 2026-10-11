package krys.web;

import krys.hero.HeroClass;
import krys.hero.HeroClassCoreStatProgressions;
import krys.hero.HeroCoreStats;
import krys.hero.HeroCoreStatDerivedFacts;
import krys.hero.HeroClassStatBaseline;
import krys.hero.HeroClassStatBaselines;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.CurrentHeroActiveItemStats;
import krys.itemlibrary.ItemLibraryPresentationSupport;

import java.util.Optional;
import java.math.BigDecimal;

/** Prezentacja statystyk bohatera w UI current build, oddzielona od technicznych effective stats runtime. */
public final class CurrentHeroStatsPresentation {
    private final String heroClassName;
    private final int level;
    private final double strength;
    private final double intelligence;
    private final HeroCoreStats classLevelCoreStats;
    private final HeroClass heroClass;
    private final CurrentBuildImportableStats activeItemStats;
    private final CurrentHeroActiveItemStats activeHeroItemStats;
    private final Optional<HeroClassStatBaseline> verifiedBaseline;

    private CurrentHeroStatsPresentation(HeroClass heroClass,
                                         int level,
                                         double strength,
                                         double intelligence,
                                         HeroCoreStats classLevelCoreStats,
                                         CurrentBuildImportableStats activeItemStats,
                                         CurrentHeroActiveItemStats activeHeroItemStats,
                                         Optional<HeroClassStatBaseline> verifiedBaseline) {
        this.heroClassName = heroClass.getDisplayName();
        this.heroClass = heroClass;
        this.level = level;
        this.strength = strength;
        this.intelligence = intelligence;
        this.classLevelCoreStats = classLevelCoreStats;
        this.activeItemStats = activeItemStats;
        this.activeHeroItemStats = activeHeroItemStats;
        this.verifiedBaseline = verifiedBaseline;
    }

    public static CurrentHeroStatsPresentation from(CurrentBuildPageModel model) {
        HeroClass heroClass = model.getActiveHero().getHeroClass();
        int level = parseDisplayLevel(model.getHeroLevel());
        HeroCoreStats core = HeroClassCoreStatProgressions.get(heroClass).atLevel(level);
        double additionalIntelligence = 0;
        if (heroClass == HeroClass.NECROMANCER) {
            // Niepoprawny formularz pozostaje widoczny z błędami; statystyki pokazują wtedy znany baseline i itemy.
            try { additionalIntelligence = model.getNecromancerFormData().parsedAdditionalNonItemIntelligence(); }
            catch (IllegalArgumentException exception) { additionalIntelligence = 0; }
        }
        CurrentBuildImportableStats activeContribution = model.getActiveLibraryContribution();
        return new CurrentHeroStatsPresentation(
                heroClass,
                level,
                core.strength() + activeContribution.getStrength(),
                core.intelligence() + additionalIntelligence + activeContribution.getIntelligence(),
                core,
                activeContribution,
                model.getActiveHeroItemStats(),
                HeroClassStatBaselines.find(model.getActiveHero().getHeroClass(), level)
        );
    }

    public String getHeroClassName() {
        return heroClassName;
    }

    public int getLevel() {
        return level;
    }

    public String getStrengthDisplay() {
        return ItemLibraryPresentationSupport.formatWhole(strength);
    }

    public String getIntelligenceDisplay() {
        return ItemLibraryPresentationSupport.formatWhole(intelligence);
    }

    public String getWillpowerDisplay() { return Integer.toString(classLevelCoreStats.willpower()); }
    public String getDexterityDisplay() { return Integer.toString(classLevelCoreStats.dexterity()); }
    public int getClassLevelIntelligence() { return classLevelCoreStats.intelligence(); }
    public HeroCoreStatDerivedFacts getClassLevelCoreStatFacts(BigDecimal skillDamagePercentPerIntelligence) {
        return HeroCoreStatDerivedFacts.from(classLevelCoreStats, skillDamagePercentPerIntelligence);
    }

    public CurrentBuildImportableStats getActiveItemStats() {
        return activeItemStats;
    }

    public CurrentHeroActiveItemStats getActiveHeroItemStats() {
        return activeHeroItemStats;
    }

    public Optional<HeroClassStatBaseline> getVerifiedBaseline() {
        return verifiedBaseline;
    }

    public long getWeaponDamage() {
        if (heroClass == HeroClass.NECROMANCER) {
            return CurrentBuildWeaponDamageResolver.resolve(activeHeroItemStats, activeItemStats);
        }
        return verifiedBaseline.map(HeroClassStatBaseline::getWeaponDamage).orElse(0L)
                + activeItemStats.getWeaponDamage();
    }

    public int getMaxHealth() {
        return verifiedBaseline.map(HeroClassStatBaseline::getMaxHealth).orElse(0)
                + (int) Math.round(activeHeroItemStats.getMaximumLifeFromItems());
    }

    public String getMaximumLifeFromItemsDisplay() {
        return ItemLibraryPresentationSupport.formatWhole(activeHeroItemStats.getMaximumLifeFromItems());
    }

    public double getThorns() {
        return verifiedBaseline.map(HeroClassStatBaseline::getThorns).orElse(0)
                + activeItemStats.getThorns();
    }

    public boolean hasActiveItemBlockChance() {
        return activeItemStats.getBlockChance() > 0.0d;
    }

    public boolean hasActiveItemRetributionChance() {
        return activeItemStats.getRetributionChance() > 0.0d;
    }

    public int getTotalArmor(HeroClassStatBaseline baseline) {
        if (heroClass == HeroClass.NECROMANCER) {
            return Math.toIntExact(getArmorFromStrength() + activeHeroItemStats.getItemArmor())
                    + baseline.getArmorBreakdown().getArmorFromItems() + baseline.getArmorBreakdown().getArmorFromOtherSources();
        }
        return baseline.getArmor() + Math.toIntExact(activeHeroItemStats.getItemArmor());
    }

    public int getArmorFromStrength() {
        return heroClass == HeroClass.NECROMANCER ? Math.toIntExact(Math.round(strength * 2))
                : verifiedBaseline.orElseThrow().getArmorBreakdown().getArmorFromStrength();
    }

    public String getActiveItemArmorDisplay() {
        return Long.toString(activeHeroItemStats.getItemArmor());
    }

    public int getPhysicalResistance(HeroClassStatBaseline baseline) {
        if (heroClass == HeroClass.NECROMANCER) { return necromancerAllResistance(); }
        return baseline.getPhysicalResistance() + (int) Math.round(activeHeroItemStats.getAllResistance());
    }

    public int getFireResistance(HeroClassStatBaseline baseline) {
        if (heroClass == HeroClass.NECROMANCER) { return necromancerAllResistance() + (int) Math.round(activeHeroItemStats.getFireResistance()); }
        return baseline.getFireResistance()
                + (int) Math.round(activeHeroItemStats.getAllResistance())
                + (int) Math.round(activeHeroItemStats.getFireResistance());
    }

    public int getLightningResistance(HeroClassStatBaseline baseline) {
        if (heroClass == HeroClass.NECROMANCER) { return necromancerAllResistance(); }
        return baseline.getLightningResistance() + (int) Math.round(activeHeroItemStats.getAllResistance());
    }

    public int getColdResistance(HeroClassStatBaseline baseline) {
        if (heroClass == HeroClass.NECROMANCER) { return necromancerAllResistance(); }
        return baseline.getColdResistance() + (int) Math.round(activeHeroItemStats.getAllResistance());
    }

    public int getPoisonResistance(HeroClassStatBaseline baseline) {
        if (heroClass == HeroClass.NECROMANCER) { return necromancerAllResistance(); }
        return baseline.getPoisonResistance() + (int) Math.round(activeHeroItemStats.getAllResistance());
    }

    public int getShadowResistance(HeroClassStatBaseline baseline) {
        if (heroClass == HeroClass.NECROMANCER) { return necromancerAllResistance(); }
        return baseline.getShadowResistance() + (int) Math.round(activeHeroItemStats.getAllResistance());
    }

    private int necromancerAllResistance() {
        return BigDecimal.valueOf(intelligence).multiply(new BigDecimal("0.4"))
                .add(BigDecimal.valueOf(activeHeroItemStats.getAllResistance()))
                .setScale(0, java.math.RoundingMode.HALF_UP).intValueExact();
    }

    private static int parseDisplayLevel(String rawLevel) {
        try {
            return Math.max(1, Integer.parseInt(rawLevel));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

}
