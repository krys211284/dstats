package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.ValidatedImportedItem;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        service.setActiveItem(EquipmentSlot.OFF_HAND, shieldA.getItemId());
        assertEquals(List.of(shieldA.getItemId()), service.getActiveItems().stream().map(SavedImportedItem::getItemId).toList());

        service.setActiveItem(EquipmentSlot.OFF_HAND, shieldB.getItemId());
        assertEquals(List.of(shieldB.getItemId()), service.getActiveItems().stream().map(SavedImportedItem::getItemId).toList());
        assertEquals(shieldB.getItemId(), service.getSelection().getSelectedItemId(EquipmentSlot.OFF_HAND));
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

        service.setActiveItem(EquipmentSlot.MAIN_HAND, weapon.getItemId());
        service.setActiveItem(EquipmentSlot.OFF_HAND, shield.getItemId());

        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(
                new CurrentBuildImportableStats(200L, 30.0d, 11.0d, 70.0d, 10.0d, 15.0d)
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
                                .map(SavedImportedItem::getSlot)
                                .distinct()
                                .count() == combination.getSelectedItems().size())
                        .toList()
        );
    }
}
