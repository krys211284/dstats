package krys.web;

import krys.hero.HeroClassDef;
import krys.hero.HeroClassDefs;
import krys.hero.HeroClassStatBaseline;
import krys.hero.HeroClassStatBaselines;
import krys.item.Item;
import krys.item.ItemStat;
import krys.item.ItemStatType;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.CurrentHeroActiveItemStats;
import krys.itemlibrary.HeroSlotItemAssignment;
import krys.itemlibrary.ItemLibraryPresentationSupport;
import krys.itemlibrary.SavedImportedItem;

import java.util.ArrayList;
import java.util.List;
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
        List<Item> equippedItems = activeItemsAsItems(model);
        return new CurrentHeroStatsPresentation(
                classDef.getDisplayName(),
                level,
                classDef.resolveTotalMainStat(level, equippedItems),
                classDef.resolveTotalIntelligence(level, equippedItems),
                model.getActiveLibraryContribution(),
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

    private static int parseDisplayLevel(String rawLevel) {
        try {
            return Math.max(1, Integer.parseInt(rawLevel));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private static List<Item> activeItemsAsItems(CurrentBuildPageModel model) {
        List<Item> items = new ArrayList<>();
        for (HeroSlotItemAssignment assignment : model.getActiveLibraryItems()) {
            SavedImportedItem item = assignment.getItem();
            items.add(new Item(
                    Math.toIntExact(item.getItemId()),
                    item.getDisplayName(),
                    item.getSlot(),
                    List.of(
                            new ItemStat(ItemStatType.STRENGTH, item.getStrength()),
                            new ItemStat(ItemStatType.INTELLIGENCE, item.getIntelligence()),
                            new ItemStat(ItemStatType.MAIN_HAND_WEAPON_DAMAGE, item.getWeaponDamage()),
                            new ItemStat(ItemStatType.THORNS, item.getThorns()),
                            new ItemStat(ItemStatType.BLOCK_CHANCE, item.getBlockChance()),
                            new ItemStat(ItemStatType.RETRIBUTION_CHANCE, item.getRetributionChance())
                    )
            ));
        }
        return items;
    }
}
