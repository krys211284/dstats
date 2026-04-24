package krys.hero;

import krys.item.Item;
import krys.item.ItemStatType;

import java.util.List;

/**
 * Definicja klasy bohatera używana przez silnik obrażeń.
 * Na tym etapie potrzebujemy wyłącznie parametrów wpływających na Strength i Intelligence.
 */
public final class HeroClassDef {
    private final HeroClass heroClass;
    private final String displayName;
    private final ItemStatType mainStatType;
    private final int baseMainStatAtLevelOne;
    private final int mainStatPerLevel;
    private final double mainStatDivisor;
    private final int baseIntelligenceAtLevelOne;
    private final int intelligencePerLevel;

    public HeroClassDef(HeroClass heroClass,
                        String displayName,
                        ItemStatType mainStatType,
                        int baseMainStatAtLevelOne,
                        int mainStatPerLevel,
                        double mainStatDivisor,
                        int baseIntelligenceAtLevelOne,
                        int intelligencePerLevel) {
        this.heroClass = heroClass;
        this.displayName = displayName;
        this.mainStatType = mainStatType;
        this.baseMainStatAtLevelOne = baseMainStatAtLevelOne;
        this.mainStatPerLevel = mainStatPerLevel;
        this.mainStatDivisor = mainStatDivisor;
        this.baseIntelligenceAtLevelOne = baseIntelligenceAtLevelOne;
        this.intelligencePerLevel = intelligencePerLevel;
    }

    public HeroClass getHeroClass() {
        return heroClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ItemStatType getMainStatType() {
        return mainStatType;
    }

    public double resolveTotalMainStat(int level, List<Item> equippedItems) {
        double itemMainStat = Item.sumStat(equippedItems, mainStatType);
        double baseMainStat = baseMainStatAtLevelOne + Math.max(0, level - 1) * mainStatPerLevel;
        return baseMainStat + itemMainStat;
    }

    public double resolveMainStatMultiplier(int level, List<Item> equippedItems) {
        return 1.0d + (resolveTotalMainStat(level, equippedItems) / mainStatDivisor);
    }

    public double resolveTotalIntelligence(int level, List<Item> equippedItems) {
        double itemIntelligence = Item.sumStat(equippedItems, ItemStatType.INTELLIGENCE);
        double baseIntelligence = baseIntelligenceAtLevelOne + Math.max(0, level - 1) * intelligencePerLevel;
        return baseIntelligence + itemIntelligence;
    }
}
