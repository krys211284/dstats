package krys.web;

import krys.hero.HeroClassDef;
import krys.hero.HeroClassDefs;
import krys.hero.HeroClassStatBaseline;
import krys.hero.HeroClassStatBaselines;
import krys.item.Item;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.CurrentHeroActiveItemStats;
import krys.itemlibrary.ItemLibraryPresentationSupport;

import java.util.Optional;

/** Prezentacja statystyk bohatera w UI current build, oddzielona od technicznych effective stats runtime. */
public final class CurrentHeroStatsPresentation {
    private final String heroClassName;
    private final int level;
    private final double strength;
    private final double intelligence;
    private final CurrentBuildImportableStats activeItemStats;
    private final CurrentHeroActiveItemStats activeHeroItemStats;
    private final Optional<HeroClassStatBaseline> verifiedBaseline;

    private CurrentHeroStatsPresentation(String heroClassName,
                                         int level,
                                         double strength,
                                         double intelligence,
                                         CurrentBuildImportableStats activeItemStats,
                                         CurrentHeroActiveItemStats activeHeroItemStats,
                                         Optional<HeroClassStatBaseline> verifiedBaseline) {
        this.heroClassName = heroClassName;
        this.level = level;
        this.strength = strength;
        this.intelligence = intelligence;
        this.activeItemStats = activeItemStats;
        this.activeHeroItemStats = activeHeroItemStats;
        this.verifiedBaseline = verifiedBaseline;
    }

    public static CurrentHeroStatsPresentation from(CurrentBuildPageModel model) {
        HeroClassDef classDef = HeroClassDefs.get(model.getActiveHero().getHeroClass());
        int level = parseDisplayLevel(model.getFormData().getLevel());
        CurrentBuildImportableStats activeContribution = model.getActiveLibraryContribution();
        return new CurrentHeroStatsPresentation(
                classDef.getDisplayName(),
                level,
                classDef.resolveTotalMainStat(level, effectiveItems(activeContribution)),
                classDef.resolveTotalIntelligence(level, effectiveItems(activeContribution)),
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
        return baseline.getArmor() + Math.toIntExact(activeHeroItemStats.getItemArmor());
    }

    public String getActiveItemArmorDisplay() {
        return Long.toString(activeHeroItemStats.getItemArmor());
    }

    public int getPhysicalResistance(HeroClassStatBaseline baseline) {
        return baseline.getPhysicalResistance() + (int) Math.round(activeHeroItemStats.getAllResistance());
    }

    public int getFireResistance(HeroClassStatBaseline baseline) {
        return baseline.getFireResistance()
                + (int) Math.round(activeHeroItemStats.getAllResistance())
                + (int) Math.round(activeHeroItemStats.getFireResistance());
    }

    public int getLightningResistance(HeroClassStatBaseline baseline) {
        return baseline.getLightningResistance() + (int) Math.round(activeHeroItemStats.getAllResistance());
    }

    public int getColdResistance(HeroClassStatBaseline baseline) {
        return baseline.getColdResistance() + (int) Math.round(activeHeroItemStats.getAllResistance());
    }

    public int getPoisonResistance(HeroClassStatBaseline baseline) {
        return baseline.getPoisonResistance() + (int) Math.round(activeHeroItemStats.getAllResistance());
    }

    public int getShadowResistance(HeroClassStatBaseline baseline) {
        return baseline.getShadowResistance() + (int) Math.round(activeHeroItemStats.getAllResistance());
    }

    private static int parseDisplayLevel(String rawLevel) {
        try {
            return Math.max(1, Integer.parseInt(rawLevel));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private static java.util.List<Item> effectiveItems(CurrentBuildImportableStats activeContribution) {
        return java.util.List.of(new Item(
                0,
                "Aktywne itemy",
                null,
                java.util.List.of(
                        new krys.item.ItemStat(krys.item.ItemStatType.STRENGTH, activeContribution.getStrength()),
                        new krys.item.ItemStat(krys.item.ItemStatType.INTELLIGENCE, activeContribution.getIntelligence()),
                        new krys.item.ItemStat(krys.item.ItemStatType.MAIN_HAND_WEAPON_DAMAGE, activeContribution.getWeaponDamage()),
                        new krys.item.ItemStat(krys.item.ItemStatType.THORNS, activeContribution.getThorns()),
                        new krys.item.ItemStat(krys.item.ItemStatType.BLOCK_CHANCE, activeContribution.getBlockChance()),
                        new krys.item.ItemStat(krys.item.ItemStatType.RETRIBUTION_CHANCE, activeContribution.getRetributionChance())
                )
        ));
    }
}
