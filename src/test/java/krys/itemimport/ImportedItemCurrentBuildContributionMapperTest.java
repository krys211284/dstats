package krys.itemimport;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Test mapowania zatwierdzonego itemu do agregowanego modelu current build. */
class ImportedItemCurrentBuildContributionMapperTest {
    @Test
    void shouldMapImportedItemToCurrentBuildContribution() {
        ValidatedImportedItem importedItem = new ValidatedImportedItem(
                "miecz.png",
                EquipmentSlot.MAIN_HAND,
                321L,
                55.0d,
                13.0d,
                90.0d,
                17.0d,
                29.0d
        );

        ImportedItemCurrentBuildContribution contribution = new ImportedItemCurrentBuildContributionMapper().map(importedItem);

        assertEquals(321L, contribution.getWeaponDamage());
        assertEquals(55.0d, contribution.getStrength());
        assertEquals(13.0d, contribution.getIntelligence());
        assertEquals(90.0d, contribution.getThorns());
        assertEquals(17.0d, contribution.getBlockChance());
        assertEquals(29.0d, contribution.getRetributionChance());
    }
}
