package krys.itemlibrary;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje stabilne rozwiązywanie katalogu danych biblioteki itemów oraz migrację legacy storage. */
class ItemLibraryDataDirectoryResolverTest {
    @Test
    void shouldUseConfiguredDataDirectoryOverride() throws Exception {
        Path userHome = Files.createTempDirectory("dstats-user-home");
        Path legacyDataDirectory = Files.createTempDirectory("dstats-legacy");
        Path overrideDirectory = Files.createTempDirectory("dstats-override");

        ItemLibraryDataDirectoryResolver resolver = new ItemLibraryDataDirectoryResolver(
                overrideDirectory.toString(),
                userHome,
                legacyDataDirectory
        );

        assertEquals(overrideDirectory.toAbsolutePath().normalize(), resolver.resolveDataDirectory());
    }

    @Test
    void shouldResolveStableDefaultDirectoryOutsideTargetAndContainDstatsSegment() throws Exception {
        Path userHome = Files.createTempDirectory("dstats-user-home");
        Path legacyDataDirectory = Files.createTempDirectory("dstats-legacy");

        ItemLibraryDataDirectoryResolver resolver = new ItemLibraryDataDirectoryResolver(
                null,
                userHome,
                legacyDataDirectory
        );

        Path resolvedDataDirectory = resolver.resolveDataDirectory();

        assertEquals(
                userHome.resolve(".dstats").resolve("item-library").toAbsolutePath().normalize(),
                resolvedDataDirectory
        );
        assertTrue(resolvedDataDirectory.toString().contains("dstats"));
        assertFalse(resolvedDataDirectory.toString().contains("target"));
    }

    @Test
    void shouldMigrateLegacySavedItemsAndActiveSelectionToStableDirectory() throws Exception {
        Path userHome = Files.createTempDirectory("dstats-user-home");
        Path legacyDataDirectory = Files.createTempDirectory("dstats-legacy");
        FileItemLibraryRepository legacyRepository = new FileItemLibraryRepository(legacyDataDirectory);

        SavedImportedItem savedItem = legacyRepository.save(buildShield(0L, "legacy-shield.png", 114.0d, 494.0d, 20.0d));
        legacyRepository.saveSelection(new ActiveItemSelection(Map.of(EquipmentSlot.OFF_HAND, savedItem.getItemId())));

        ItemLibraryDataDirectoryResolver resolver = new ItemLibraryDataDirectoryResolver(
                null,
                userHome,
                legacyDataDirectory
        );

        Path resolvedDataDirectory = resolver.resolveDataDirectory();
        FileItemLibraryRepository migratedRepository = new FileItemLibraryRepository(resolvedDataDirectory);

        List<SavedImportedItem> migratedItems = migratedRepository.findAll();
        assertEquals(1, migratedItems.size());
        assertEquals(savedItem.getItemId(), migratedItems.get(0).getItemId());
        assertEquals(savedItem.getDisplayName(), migratedItems.get(0).getDisplayName());
        assertEquals(savedItem.getThorns(), migratedItems.get(0).getThorns());
        assertEquals(savedItem.getItemId(), migratedRepository.loadSelection().getSelectedItemId(EquipmentSlot.OFF_HAND));
        assertTrue(Files.isRegularFile(resolvedDataDirectory.resolve("saved-items.db")));
        assertTrue(Files.isRegularFile(resolvedDataDirectory.resolve("active-selection.db")));
    }

    @Test
    void shouldKeepExistingStableDirectoryDataWhenLegacyLocationAlsoContainsData() throws Exception {
        Path userHome = Files.createTempDirectory("dstats-user-home");
        Path legacyDataDirectory = Files.createTempDirectory("dstats-legacy");
        Path stableDataDirectory = userHome.resolve(".dstats").resolve("item-library");

        FileItemLibraryRepository legacyRepository = new FileItemLibraryRepository(legacyDataDirectory);
        SavedImportedItem legacyItem = legacyRepository.save(buildShield(0L, "legacy-shield.png", 114.0d, 494.0d, 20.0d));
        legacyRepository.saveSelection(new ActiveItemSelection(Map.of(EquipmentSlot.OFF_HAND, legacyItem.getItemId())));

        FileItemLibraryRepository stableRepository = new FileItemLibraryRepository(stableDataDirectory);
        SavedImportedItem stableItem = stableRepository.save(buildShield(0L, "stable-shield.png", 120.0d, 500.0d, 22.0d));
        stableRepository.saveSelection(new ActiveItemSelection(Map.of(EquipmentSlot.OFF_HAND, stableItem.getItemId())));

        ItemLibraryDataDirectoryResolver resolver = new ItemLibraryDataDirectoryResolver(
                null,
                userHome,
                legacyDataDirectory
        );

        Path resolvedDataDirectory = resolver.resolveDataDirectory();
        FileItemLibraryRepository reloadedStableRepository = new FileItemLibraryRepository(resolvedDataDirectory);

        List<SavedImportedItem> persistedItems = reloadedStableRepository.findAll();
        assertEquals(1, persistedItems.size());
        assertEquals(stableItem.getDisplayName(), persistedItems.get(0).getDisplayName());
        assertNotNull(reloadedStableRepository.loadSelection().getSelectedItemId(EquipmentSlot.OFF_HAND));
        assertEquals(stableItem.getItemId(), reloadedStableRepository.loadSelection().getSelectedItemId(EquipmentSlot.OFF_HAND));
        assertEquals(stableDataDirectory.toAbsolutePath().normalize(), resolvedDataDirectory);
    }

    private static SavedImportedItem buildShield(long itemId,
                                                 String sourceImageName,
                                                 double strength,
                                                 double thorns,
                                                 double blockChance) {
        return new SavedImportedItem(
                itemId,
                "OFF_HAND / " + sourceImageName,
                sourceImageName,
                EquipmentSlot.OFF_HAND,
                0L,
                strength,
                0.0d,
                thorns,
                blockChance,
                0.0d
        );
    }
}
