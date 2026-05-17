package krys.web;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ValidatedImportedItem;
import krys.itemlibrary.EffectiveCurrentBuildResolution;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Testuje mapowanie aktywnego bohatera i aktywnej broni do wejścia istniejącego runtime current build. */
class CurrentBuildRuntimeInputResolverTest {
    private final CurrentBuildRuntimeInputResolver resolver = new CurrentBuildRuntimeInputResolver();

    @Test
    void shouldUseActiveWeaponAverageDamageAsRuntimeWeaponDamage() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-verathiel");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem verathiel = service.saveImportedItem(verathielItem());
        HeroItemSelection selection = HeroItemSelection.empty()
                .withSelectedItem(HeroEquipmentSlot.MAIN_HAND, verathiel.getItemId());
        CurrentBuildFormData formData = level70FormData();
        EffectiveCurrentBuildResolution libraryResolution = service.resolveEffectiveCurrentBuild(legacyStats(), selection);

        CurrentBuildImportableStats runtimeStats = resolver.resolve(hero(selection, formData), formData, libraryResolution);

        assertEquals(1664L, runtimeStats.getWeaponDamage());
        assertNotEquals(1830L, runtimeStats.getWeaponDamage());
        assertNotEquals(1758L, runtimeStats.getWeaponDamage());
        assertEquals(79.0d, runtimeStats.getStrength(), 0.0000001d);
        assertEquals(76.0d, runtimeStats.getIntelligence(), 0.0000001d);
        assertEquals(0.0d, runtimeStats.getThorns(), 0.0000001d);
        assertEquals(0.0d, runtimeStats.getBlockChance(), 0.0000001d);
        assertEquals(0.0d, runtimeStats.getRetributionChance(), 0.0000001d);
    }

    @Test
    void shouldKeepWeaponDamageFallbackWhenSavedWeaponIsNotActive() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-inactive");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        service.saveImportedItem(verathielItem());
        HeroItemSelection emptySelection = HeroItemSelection.empty();
        CurrentBuildFormData formData = level70FormData();
        EffectiveCurrentBuildResolution libraryResolution = service.resolveEffectiveCurrentBuild(legacyStats(), emptySelection);

        CurrentBuildImportableStats runtimeStats = resolver.resolve(hero(emptySelection, formData), formData, libraryResolution);

        assertEquals(8L, runtimeStats.getWeaponDamage());
        assertEquals(79.0d, runtimeStats.getStrength(), 0.0000001d);
        assertEquals(76.0d, runtimeStats.getIntelligence(), 0.0000001d);
    }

    private static HeroProfile hero(HeroItemSelection selection, CurrentBuildFormData formData) {
        return new HeroProfile(
                1L,
                "Paladyn",
                HeroClass.PALADIN,
                CurrentBuildFormQuerySupport.toQuery(formData),
                selection
        );
    }

    private static CurrentBuildFormData level70FormData() {
        return CurrentBuildFormData.fromFormFields(java.util.Map.of(
                "level", "70",
                "weaponDamage", "8",
                "strength", "18",
                "intelligence", "0",
                "thorns", "50",
                "blockChance", "50",
                "retributionChance", "50"
        ));
    }

    private static CurrentBuildImportableStats legacyStats() {
        return new CurrentBuildImportableStats(8L, 18.0d, 0.0d, 50.0d, 50.0d, 50.0d);
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
