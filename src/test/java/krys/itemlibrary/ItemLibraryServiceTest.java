package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ValidatedImportedItem;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import krys.web.HeroItemSelection;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje serwisy aplikacyjne biblioteki itemów oraz agregację do effective current build. */
class ItemLibraryServiceTest {
    @Test
    void shouldAllowSeveralItemsOfSameSlotAndSwitchActiveItem() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-service");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));

        SavedImportedItem shieldA = service.saveImportedItem(new ValidatedImportedItem(
                "shield-a.png",
                EquipmentSlot.OFF_HAND,
                0L,
                114.0d,
                0.0d,
                494.0d,
                20.0d,
                0.0d
        ));
        SavedImportedItem shieldB = service.saveImportedItem(new ValidatedImportedItem(
                "shield-b.png",
                EquipmentSlot.OFF_HAND,
                0L,
                120.0d,
                0.0d,
                500.0d,
                22.0d,
                0.0d
        ));

        assertEquals(2, service.getSavedItems().size());

        HeroItemSelection selectionA = HeroItemSelection.empty()
                .withSelectedItem(HeroEquipmentSlot.OFF_HAND, shieldA.getItemId());
        EffectiveCurrentBuildResolution resolutionA = service.resolveEffectiveCurrentBuild(
                new CurrentBuildImportableStats(0L, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
                selectionA
        );
        assertEquals(List.of(shieldA.getItemId()), resolutionA.getActiveItems().stream().map(assignment -> assignment.getItem().getItemId()).toList());

        HeroItemSelection selectionB = selectionA.withSelectedItem(HeroEquipmentSlot.OFF_HAND, shieldB.getItemId());
        EffectiveCurrentBuildResolution resolutionB = service.resolveEffectiveCurrentBuild(
                new CurrentBuildImportableStats(0L, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
                selectionB
        );
        assertEquals(List.of(shieldB.getItemId()), resolutionB.getActiveItems().stream().map(assignment -> assignment.getItem().getItemId()).toList());
        assertEquals(shieldB.getItemId(), selectionB.getSelectedItemId(HeroEquipmentSlot.OFF_HAND));
    }

    @Test
    void shouldAggregateActiveItemsIntoEffectiveCurrentBuild() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-effective");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));

        SavedImportedItem weapon = service.saveImportedItem(new ValidatedImportedItem(
                "weapon.png",
                EquipmentSlot.MAIN_HAND,
                321L,
                55.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        ));
        SavedImportedItem shield = service.saveImportedItem(new ValidatedImportedItem(
                "shield.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                11.0d,
                90.0d,
                18.0d,
                25.0d
        ));

        HeroItemSelection selection = new HeroItemSelection(Map.of(
                HeroEquipmentSlot.MAIN_HAND, weapon.getItemId(),
                HeroEquipmentSlot.OFF_HAND, shield.getItemId()
        ));

        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(
                new CurrentBuildImportableStats(200L, 30.0d, 11.0d, 70.0d, 10.0d, 15.0d),
                selection
        );

        assertEquals(2, resolution.getActiveItems().size());
        assertEquals(321L, resolution.getActiveItemsContribution().getWeaponDamage());
        assertEquals(55.0d, resolution.getActiveItemsContribution().getStrength());
        assertEquals(11.0d, resolution.getActiveItemsContribution().getIntelligence());
        assertEquals(90.0d, resolution.getActiveItemsContribution().getThorns());
        assertEquals(18.0d, resolution.getActiveItemsContribution().getBlockChance());
        assertEquals(25.0d, resolution.getActiveItemsContribution().getRetributionChance());
        assertEquals(521L, resolution.getEffectiveStats().getWeaponDamage());
        assertEquals(85.0d, resolution.getEffectiveStats().getStrength());
        assertEquals(22.0d, resolution.getEffectiveStats().getIntelligence());
        assertEquals(160.0d, resolution.getEffectiveStats().getThorns());
        assertEquals(28.0d, resolution.getEffectiveStats().getBlockChance());
        assertEquals(40.0d, resolution.getEffectiveStats().getRetributionChance());
    }

    @Test
    void shouldProjectActiveVerathielDetailsAndAffixesIntoHeroStatsWithoutRuntimeMixing() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-hero-stats");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem verathiel = service.saveImportedItem(verathielItem());

        CurrentHeroActiveItemStats stats = service.resolveActiveHeroItemStats(
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.MAIN_HAND, verathiel.getItemId())
        );

        assertEquals(1830L, stats.getWeaponDps());
        assertEquals(1350L, stats.getWeaponDamageMin());
        assertEquals(1978L, stats.getWeaponDamageMax());
        assertEquals(1664L, stats.getAverageWeaponDamage());
        assertEquals(1.10d, stats.getAttacksPerSecond(), 0.0000001d);
        assertEquals(2141.0d, stats.getMaximumLifeFromItems(), 0.0000001d);
        assertEquals(94.0d, stats.getFlatWeaponDamageFromAffixes(), 0.0000001d);
        assertEquals(545.0d, stats.getLifeOnHit(), 0.0000001d);
        assertEquals(3.0d, stats.getLuckyHitPrimaryResourceValue(), 0.0000001d);
        assertEquals(1664L, stats.getAverageWeaponDamage());
        assertEquals(2141.0d, stats.getMaximumLifeFromItems(), 0.0000001d);
    }

    @Test
    void shouldUseOnlyActiveCompatibleItemsForHeroStatsProjection() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-active-hero-stats");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem verathiel = service.saveImportedItem(verathielItem());
        SavedImportedItem shield = service.saveImportedItem(new ValidatedImportedItem(
                "shield.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                List.of(new ImportedItemAffix(ImportedItemAffixType.MAXIMUM_LIFE, 99.0d))
        ));

        CurrentHeroActiveItemStats inactiveStats = service.resolveActiveHeroItemStats(HeroItemSelection.empty());
        assertNull(inactiveStats.getWeaponDps());
        assertEquals(0.0d, inactiveStats.getMaximumLifeFromItems(), 0.0000001d);

        CurrentHeroActiveItemStats activeStats = service.resolveActiveHeroItemStats(
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.MAIN_HAND, verathiel.getItemId())
        );
        assertEquals(1830L, activeStats.getWeaponDps());
        assertEquals(2141.0d, activeStats.getMaximumLifeFromItems(), 0.0000001d);

        CurrentHeroActiveItemStats clearedStats = service.resolveActiveHeroItemStats(
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.MAIN_HAND, verathiel.getItemId()).withoutSlot(HeroEquipmentSlot.MAIN_HAND)
        );
        assertNull(clearedStats.getWeaponDps());
        assertEquals(0.0d, clearedStats.getMaximumLifeFromItems(), 0.0000001d);

        CurrentHeroActiveItemStats incompatibleStats = service.resolveActiveHeroItemStats(
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.MAIN_HAND, shield.getItemId())
        );
        assertNull(incompatibleStats.getWeaponDps());
        assertEquals(0.0d, incompatibleStats.getMaximumLifeFromItems(), 0.0000001d);
    }

    @Test
    void shouldApplyOnlyActiveMaxAnimusTemperingToHeroStatsProjection() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-active-tempering");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem shield = service.saveImportedItem(new ValidatedImportedItem(
                "tempered-shield.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                List.of(),
                "",
                ItemImportDetails.empty(),
                List.of(new ItemTemperingAffix(
                        "defense_max_animus",
                        TemperingCategory.DEFENSE,
                        5.0d,
                        "",
                        TemperingRuntimeStatus.DATA_ONLY,
                        true
                ))
        ));

        CurrentHeroActiveItemStats inactiveStats = service.resolveActiveHeroItemStats(HeroItemSelection.empty());
        assertEquals(0.0d, inactiveStats.getMaxAnimusFromTempering(), 0.0000001d);

        CurrentHeroActiveItemStats activeStats = service.resolveActiveHeroItemStats(
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.OFF_HAND, shield.getItemId())
        );
        assertEquals(5.0d, activeStats.getMaxAnimusFromTempering(), 0.0000001d);
        assertTrue(activeStats.getMaxAnimusTemperingSources().contains("hartowanie aktywnej tarczy: +5"));
    }

    @Test
    void shouldGenerateDeterministicSearchCombinationsWithAtMostOneItemPerSlot() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-search-combinations");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));

        service.saveImportedItem(new ValidatedImportedItem(
                "weapon-a.png",
                EquipmentSlot.MAIN_HAND,
                300L,
                55.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        ));
        service.saveImportedItem(new ValidatedImportedItem(
                "weapon-b.png",
                EquipmentSlot.MAIN_HAND,
                321L,
                60.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        ));
        service.saveImportedItem(new ValidatedImportedItem(
                "shield-a.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                11.0d,
                90.0d,
                18.0d,
                25.0d
        ));

        List<ItemLibrarySearchCombination> combinations = service.generateSearchCombinations();

        assertEquals(6, combinations.size());
        assertEquals("EMPTY", combinations.getFirst().toDeterministicKey());
        assertEquals(
                List.of(
                        "EMPTY",
                        "OFF_HAND#3",
                        "MAIN_HAND#1",
                        "MAIN_HAND#1|OFF_HAND#3",
                        "MAIN_HAND#2",
                        "MAIN_HAND#2|OFF_HAND#3"
                ),
                combinations.stream().map(ItemLibrarySearchCombination::toDeterministicKey).toList()
        );
        assertEquals(0L, combinations.getFirst().getTotalContribution().getWeaponDamage());
        assertEquals(321L, combinations.get(4).getTotalContribution().getWeaponDamage());
        assertEquals(90.0d, combinations.get(1).getTotalContribution().getThorns());
        assertEquals(321L, combinations.getLast().getTotalContribution().getWeaponDamage());
        assertEquals(60.0d, combinations.getLast().getTotalContribution().getStrength());
        assertEquals(11.0d, combinations.getLast().getTotalContribution().getIntelligence());
        assertEquals(90.0d, combinations.getLast().getTotalContribution().getThorns());
        assertEquals(18.0d, combinations.getLast().getTotalContribution().getBlockChance());
        assertEquals(25.0d, combinations.getLast().getTotalContribution().getRetributionChance());
        assertEquals(
                List.of(0, 1, 1, 2, 1, 2),
                combinations.stream().map(combination -> combination.getSelectedItems().size()).toList()
        );
        assertEquals(
                List.of(true, true, true, true, true, true),
                combinations.stream()
                        .map(combination -> combination.getSelectedItems().stream()
                                .map(assignment -> assignment.getHeroSlot())
                                .distinct()
                                .count() == combination.getSelectedItems().size())
                        .toList()
        );
    }

    private static ValidatedImportedItem verathielItem() {
        return new ValidatedImportedItem(
                "miecz.png",
                EquipmentSlot.MAIN_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.WEAPON_DAMAGE_FLAT, 94.0d, "", false, 0,
                                "+94 obrażeń od broni [94 - 157]", ImportedItemAffixSource.CORRECTED,
                                "verathiel_weapon_damage_flat", 94.0d, 157.0d, ""),
                        new ImportedItemAffix(ImportedItemAffixType.MAXIMUM_LIFE, 2141.0d, "", false, 1,
                                "+2 141 maksymalnego zdrowia [1 831 - 2 200]", ImportedItemAffixSource.CORRECTED,
                                "verathiel_maximum_life", 1831.0d, 2200.0d, ""),
                        new ImportedItemAffix(ImportedItemAffixType.LIFE_ON_HIT, 545.0d, "", false, 2,
                                "+545 pkt. zdrowia przy trafieniu [526 - 632]", ImportedItemAffixSource.CORRECTED,
                                "verathiel_life_on_hit", 526.0d, 632.0d, ""),
                        new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE, 3.0d, "", false, 3,
                                "Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]", ImportedItemAffixSource.CORRECTED,
                                "verathiel_lucky_hit_primary_resource", 3.0d, 4.0d, "+3")
                ),
                "verathiel_shard",
                new ItemImportDetails(
                        "Odłamek Verathiela",
                        "Miecz",
                        "UNIQUE",
                        true,
                        EquipmentSlot.MAIN_HAND,
                        900L,
                        1830L,
                        1350L,
                        1978L,
                        1664L,
                        1.10d,
                        "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100], ale dodatkowo zużywają 25 pkt. podstawowego zasobu."
                )
        );
    }
}
