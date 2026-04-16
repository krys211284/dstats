package krys.app;

import krys.hero.Hero;
import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.item.Item;
import krys.item.ItemStat;
import krys.item.ItemStatType;
import krys.simulation.HeroBuildSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Buduje runtime snapshot z realnego modelu wejścia użytkownika M8.
 * M8 celowo parametryzuje tylko podstawowe staty użytkownika, a pozostałe foundation affixy pozostawia jako stałe kontraktowe.
 */
public final class CurrentBuildSnapshotFactory {
    private static final double FOUNDATION_CRIT_DAMAGE = 1.5d;
    private static final double FOUNDATION_MAIN_HAND_WEAPON_DAMAGE = 100.0d;

    public HeroBuildSnapshot create(CurrentBuildRequest request) {
        Hero hero = new Hero(1, "Aktualny build", request.getLevel(), HeroClass.PALADIN);
        List<Item> items = List.of(
                new Item(1, "Konfigurowana broń", EquipmentSlot.MAIN_HAND, List.of(
                        new ItemStat(ItemStatType.CRIT_DAMAGE, FOUNDATION_CRIT_DAMAGE)
                )),
                new Item(2, "Konfigurowana tarcza", EquipmentSlot.OFF_HAND, buildShieldStats(request)),
                new Item(3, "Konfigurowany pancerz", EquipmentSlot.CHEST, buildChestStats(request)),
                new Item(4, "Konfigurowany ring", EquipmentSlot.RING, buildRingStats(request))
        );

        return new HeroBuildSnapshot(
                hero,
                0,
                request.getWeaponDamage(),
                0.0d,
                items,
                request.getLearnedSkills(),
                request.getActionBar()
        );
    }

    private static List<ItemStat> buildShieldStats(CurrentBuildRequest request) {
        List<ItemStat> stats = new ArrayList<>();
        stats.add(new ItemStat(ItemStatType.MAIN_HAND_WEAPON_DAMAGE, FOUNDATION_MAIN_HAND_WEAPON_DAMAGE));
        if (request.getStrength() > 0.0d) {
            stats.add(new ItemStat(ItemStatType.STRENGTH, request.getStrength()));
        }
        if (request.getThorns() > 0.0d) {
            stats.add(new ItemStat(ItemStatType.THORNS, request.getThorns()));
        }
        if (request.getBlockChance() > 0.0d) {
            stats.add(new ItemStat(ItemStatType.BLOCK_CHANCE, request.getBlockChance()));
        }
        return stats;
    }

    private static List<ItemStat> buildChestStats(CurrentBuildRequest request) {
        if (request.getIntelligence() <= 0.0d) {
            return List.of();
        }
        return List.of(new ItemStat(ItemStatType.INTELLIGENCE, request.getIntelligence()));
    }

    private static List<ItemStat> buildRingStats(CurrentBuildRequest request) {
        if (request.getRetributionChance() <= 0.0d) {
            return List.of();
        }
        return List.of(new ItemStat(ItemStatType.RETRIBUTION_CHANCE, request.getRetributionChance()));
    }
}
