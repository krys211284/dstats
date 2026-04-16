package krys.itemimport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Testy aplikowania wkładu pojedynczego itemu do istniejących statów current build. */
class ImportedItemCurrentBuildApplicationServiceTest {
    @Test
    void shouldOverwriteOnlyFieldsContributedByItem() {
        CurrentBuildImportableStats baseStats = new CurrentBuildImportableStats(200L, 30.0d, 11.0d, 70.0d, 10.0d, 15.0d);
        ImportedItemCurrentBuildContribution contribution = new ImportedItemCurrentBuildContribution(321L, 55.0d, 0.0d, 90.0d, 18.0d, 25.0d);

        CurrentBuildImportableStats result = new ImportedItemCurrentBuildApplicationService()
                .apply(baseStats, contribution, CurrentBuildItemApplicationMode.OVERWRITE);

        assertEquals(321L, result.getWeaponDamage());
        assertEquals(55.0d, result.getStrength());
        assertEquals(11.0d, result.getIntelligence());
        assertEquals(90.0d, result.getThorns());
        assertEquals(18.0d, result.getBlockChance());
        assertEquals(25.0d, result.getRetributionChance());
    }

    @Test
    void shouldAddContributionToExistingCurrentBuildStats() {
        CurrentBuildImportableStats baseStats = new CurrentBuildImportableStats(200L, 30.0d, 11.0d, 70.0d, 10.0d, 15.0d);
        ImportedItemCurrentBuildContribution contribution = new ImportedItemCurrentBuildContribution(321L, 55.0d, 13.0d, 90.0d, 18.0d, 25.0d);

        CurrentBuildImportableStats result = new ImportedItemCurrentBuildApplicationService()
                .apply(baseStats, contribution, CurrentBuildItemApplicationMode.ADD_CONTRIBUTION);

        assertEquals(521L, result.getWeaponDamage());
        assertEquals(85.0d, result.getStrength());
        assertEquals(24.0d, result.getIntelligence());
        assertEquals(160.0d, result.getThorns());
        assertEquals(28.0d, result.getBlockChance());
        assertEquals(40.0d, result.getRetributionChance());
    }
}
