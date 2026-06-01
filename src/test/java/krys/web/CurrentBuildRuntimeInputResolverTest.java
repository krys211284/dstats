package krys.web;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.ApplicationAspectRegistry;
import krys.itemimport.AspectRuntimeStatus;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportDebugTrace;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ValidatedImportedItem;
import krys.itemlibrary.EffectiveCurrentBuildResolution;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Testuje mapowanie aktywnego bohatera i aktywnej broni do wejścia istniejącego runtime current build. */
class CurrentBuildRuntimeInputResolverTest {
    private final CurrentBuildRuntimeInputResolver resolver = new CurrentBuildRuntimeInputResolver();

    @AfterEach
    void clearItemImportDebugProperties() {
        System.clearProperty(ItemImportDebugTrace.JVM_PROPERTY);
        System.clearProperty(ItemImportDebugTrace.CONFIG_PROPERTY);
        System.clearProperty(ItemImportDebugTrace.FILE_PROPERTY);
    }

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
    void shouldUseZeroWeaponDamageWhenSavedWeaponIsNotActive() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-inactive");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        service.saveImportedItem(verathielItem());
        HeroItemSelection emptySelection = HeroItemSelection.empty();
        CurrentBuildFormData formData = level70FormData();
        EffectiveCurrentBuildResolution libraryResolution = service.resolveEffectiveCurrentBuild(legacyStats(), emptySelection);

        CurrentBuildImportableStats runtimeStats = resolver.resolve(hero(emptySelection, formData), formData, libraryResolution);

        assertEquals(0L, runtimeStats.getWeaponDamage());
        assertEquals(79.0d, runtimeStats.getStrength(), 0.0000001d);
        assertEquals(76.0d, runtimeStats.getIntelligence(), 0.0000001d);
        assertEquals(0.0d, runtimeStats.getThorns(), 0.0000001d);
        assertEquals(0.0d, runtimeStats.getBlockChance(), 0.0000001d);
        assertEquals(0.0d, runtimeStats.getRetributionChance(), 0.0000001d);
    }

    @Test
    void shouldResolveMaximumAnimusFromOnlyActiveTempering() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-tempering");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem shield = service.saveImportedItem(temperedShield());
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(java.util.Map.of(
                "level", "70",
                "maxAnimus", "8",
                "initialAnimus", "13"
        ));

        EffectiveCurrentBuildResolution inactiveResolution = service.resolveEffectiveCurrentBuild(
                legacyStats(),
                HeroItemSelection.empty()
        );
        assertEquals(8.0d, CurrentBuildRuntimeInputResolver.resolveMaximumAnimus(formData, inactiveResolution), 0.0000001d);

        EffectiveCurrentBuildResolution activeResolution = service.resolveEffectiveCurrentBuild(
                legacyStats(),
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.OFF_HAND, shield.getItemId())
        );
        assertEquals(13.0d, CurrentBuildRuntimeInputResolver.resolveMaximumAnimus(formData, activeResolution), 0.0000001d);
        CurrentBuildFormData effectiveFormData = resolver.applyRuntimeResourceBonuses(formData, activeResolution);
        assertEquals("13", effectiveFormData.getMaxAnimus());
    }

    @Test
    void shouldResolveMasterworkedEffectiveItemValuesForRuntimeInput() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-masterworking");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem shield = service.saveImportedItem(referenceShield(
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus"))
        ));
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(java.util.Map.of(
                "level", "70",
                "maxAnimus", "8",
                "initialAnimus", "20"
        ));

        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(
                legacyStats(),
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.OFF_HAND, shield.getItemId())
        );
        CurrentBuildImportableStats runtimeStats = resolver.resolve(hero(HeroItemSelection.empty(), formData), formData, resolution);

        assertEquals(270.0d, resolution.getActiveItemsContribution().getStrength(), 0.0000001d);
        assertEquals(349.0d, runtimeStats.getStrength(), 0.0000001d);
        assertEquals(1502L, resolution.getActiveHeroItemStats().getItemArmor());
        assertEquals(945.0d, resolution.getActiveHeroItemStats().getFireResistance(), 0.0000001d);
        assertEquals(588.0d, resolution.getActiveHeroItemStats().getAllResistance(), 0.0000001d);
        assertEquals(14.3d, resolution.getActiveHeroItemStats().getDamageReduction(), 0.0000001d);
        assertEquals(12.0d, resolution.getActiveHeroItemStats().getMaxAnimusFromTempering(), 0.0000001d);
        assertEquals(20.0d, CurrentBuildRuntimeInputResolver.resolveMaximumAnimus(formData, resolution), 0.0000001d);
    }

    @Test
    void shouldResolveMasterworkedMaxAnimusWithoutPerfectedAffixAsFifteen() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-masterworking-no-perfect");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem shield = service.saveImportedItem(referenceShield(new ItemMasterworking(25, 25)));
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(java.util.Map.of(
                "level", "70",
                "maxAnimus", "8",
                "initialAnimus", "15"
        ));

        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(
                legacyStats(),
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.OFF_HAND, shield.getItemId())
        );

        assertEquals(7.0d, resolution.getActiveHeroItemStats().getMaxAnimusFromTempering(), 0.0000001d);
        assertEquals(15.0d, CurrentBuildRuntimeInputResolver.resolveMaximumAnimus(formData, resolution), 0.0000001d);
    }

    @Test
    void shouldProjectCriticalStrikeChanceFromActiveHelmetWithoutIntelligenceFormula() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-critical-helmet");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem helmet = service.saveImportedItem(heirOfPerditionHelmet());
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(java.util.Map.of(
                "level", "70",
                "intelligence", "999"
        ));
        HeroItemSelection selection = HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.HELMET, helmet.getItemId());

        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(legacyStats(), selection);
        CurrentBuildImportableStats runtimeStats = resolver.resolve(hero(selection, formData), formData, resolution);

        assertEquals(15.0d, resolution.getActiveItemsContribution().getCriticalChancePercent(), 0.0000001d);
        assertEquals(20.2d, runtimeStats.getCriticalChancePercent(), 0.0000001d);
        assertNotEquals(23.2d, runtimeStats.getCriticalChancePercent(), 0.0000001d);
        assertEquals(15.0d, resolution.getActiveHeroItemStats().getCriticalChancePercent(), 0.0000001d);
        assertEquals(AspectRuntimeStatus.DESCRIPTIVE_ONLY,
                ApplicationAspectRegistry.get().findById("heir_of_perdition").orElseThrow().getRuntimeStatus());
        assertNotEquals(36.36d, runtimeStats.getCriticalChancePercent(), 0.0000001d);
    }

    @Test
    void shouldLogRuntimeContributionWhenItemImportDebugIsEnabled() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-debug-trace");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem helmet = service.saveImportedItem(heirOfPerditionHelmet());
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(java.util.Map.of(
                "level", "70",
                "intelligence", "999"
        ));
        HeroItemSelection selection = HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.HELMET, helmet.getItemId());
        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(legacyStats(), selection);
        System.setProperty(ItemImportDebugTrace.JVM_PROPERTY, "true");
        System.setProperty(ItemImportDebugTrace.FILE_PROPERTY,
                Path.of("target", "item-import-debug-runtime-test.log").toString());

        final CurrentBuildImportableStats[] runtimeStats = new CurrentBuildImportableStats[1];
        String logs = captureItemImportDebugLogs(() ->
                runtimeStats[0] = resolver.resolve(hero(selection, formData), formData, resolution));

        assertEquals(20.2d, runtimeStats[0].getCriticalChancePercent(), 0.0000001d);
        assertLogsContain(logs, "RUNTIME_CONTRIBUTION");
        assertLogsContain(logs, "activeItem slot=HELMET");
        assertLogsContain(logs, "RUNTIME_AFFIX");
        assertLogsContain(logs, "type=CRITICAL_STRIKE_CHANCE");
        assertLogsContain(logs, "stored=15.0000");
        assertLogsContain(logs, "referenceValue=12.0000");
        assertLogsContain(logs, "resolved=15.0000");
        assertLogsContain(logs, "type=LUCKY_HIT_CHANCE");
        assertLogsContain(logs, "stored=25.0000");
        assertLogsContain(logs, "referenceValue=null");
        assertLogsContain(logs, "resolved=25.0000");
        assertLogsContain(logs, "RUNTIME_SUM");
        assertLogsContain(logs, "criticalChancePercent=20.2");
        org.junit.jupiter.api.Assertions.assertFalse(logs.contains("resolved=31.2500"), logs);
    }

    @Test
    void shouldLogRuntimeTemperingContributionWithStoredAndResolvedValues() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-debug-tempering-trace");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem shield = service.saveImportedItem(referenceShield(
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus"))
        ));
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(java.util.Map.of(
                "level", "70",
                "maxAnimus", "8",
                "initialAnimus", "20"
        ));
        HeroItemSelection selection = HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.OFF_HAND, shield.getItemId());
        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(legacyStats(), selection);
        System.setProperty(ItemImportDebugTrace.JVM_PROPERTY, "true");
        System.setProperty(ItemImportDebugTrace.FILE_PROPERTY,
                Path.of("target", "item-import-debug-runtime-tempering-test.log").toString());

        String logs = captureItemImportDebugLogs(() ->
                resolver.resolve(hero(selection, formData), formData, resolution));

        assertLogsContain(logs, "RUNTIME_TEMPERING");
        assertLogsContain(logs, "definitionId=\"defense_max_animus\"");
        assertLogsContain(logs, "storedValue=5");
        assertLogsContain(logs, "resolvedValue=12");
        assertLogsContain(logs, "resolvedDisplayText=\"+12 do maksymalnej liczby kumulacji Animuszu\"");
        assertLogsContain(logs, "masterworkingQuality=25/25");
        assertLogsContain(logs, "perfectedAffix=\"TEMPERING_AFFIX:defense_max_animus\"");
        assertLogsContain(logs, "reason=\"stored value is GA/base import value; resolved value uses masterworking perfected tempering\"");
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

    private static ValidatedImportedItem temperedShield() {
        return new ValidatedImportedItem(
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
        );
    }

    private static ValidatedImportedItem heirOfPerditionHelmet() {
        return new ValidatedImportedItem(
                "dziedzic-zatracenia.png",
                EquipmentSlot.HELMET,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 15.0d, "%", false, 0,
                                "+15,0% szansy na trafienie krytyczne [12,0]%", ImportedItemAffixSource.CORRECTED,
                                "critical_strike_chance", null, null, 12.0d, ""),
                        new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_CHANCE, 25.0d, "%", false, 1,
                                "+25,0% szansy na szczęśliwy traf", ImportedItemAffixSource.CORRECTED,
                                "lucky_hit_chance", null, null, null, "")
                ),
                "heir_of_perdition",
                new ItemImportDetails(
                        "Dziedzic Zatracenia",
                        "Hełm",
                        "UNIQUE",
                        true,
                        EquipmentSlot.HELMET,
                        900L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        2004L,
                        "Poddaj się nienawiści i doświadcz Łaski Matki, która zwiększy zadawane przez ciebie obrażenia o 80%[x]. Zabijaj wrogów, aby na chwilę ukraść pobliskim sojusznikom efekt Łaski Matki.",
                        true
                )
        );
    }

    private static ValidatedImportedItem referenceShield(ItemMasterworking masterworking) {
        return new ValidatedImportedItem(
                "kosciane-luski.png",
                EquipmentSlot.OFF_HAND,
                0L,
                225.0d,
                0.0d,
                0.0d,
                20.0d,
                0.0d,
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 225.0d, "", true, 0, "+225 siły", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.FIRE_RESISTANCE, 787.0d, "", true, 1, "+787 do odporności na: Ogień", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, "%", false, 2, "11,4% redukcji obrażeń", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.ALL_RESISTANCE, 490.0d, "", true, 3, "+490 do odporności na wszystkie żywioły", ImportedItemAffixSource.OCR)
                ),
                "",
                new ItemImportDetails(
                        "Miażdżąca Tarcza Kościanych Łusek",
                        "Tarcza",
                        "LEGENDARY",
                        true,
                        EquipmentSlot.OFF_HAND,
                        900L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        1202L,
                        ""
                ),
                List.of(new ItemTemperingAffix(
                        "defense_max_animus",
                        TemperingCategory.DEFENSE,
                        5.0d,
                        "+5 do maksymalnej liczby kumulacji Animuszu",
                        TemperingRuntimeStatus.DATA_ONLY,
                        true
                )),
                masterworking
        );
    }

    private static String captureItemImportDebugLogs(ThrowingRunnable runnable) throws Exception {
        Logger logger = Logger.getLogger(ItemImportDebugTrace.LOGGER_NAME);
        CapturingHandler handler = new CapturingHandler();
        handler.setLevel(Level.ALL);
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        Level previousLevel = logger.getLevel();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        logger.addHandler(handler);
        try {
            runnable.run();
            return handler.contents();
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(previousUseParentHandlers);
            logger.setLevel(previousLevel);
        }
    }

    private static void assertLogsContain(String logs, String expected) {
        org.junit.jupiter.api.Assertions.assertTrue(logs.contains(expected), () -> "Brak w logu: " + expected + "\n" + logs);
    }

    private static final class CapturingHandler extends Handler {
        private final StringBuilder builder = new StringBuilder();

        @Override
        public void publish(LogRecord record) {
            builder.append(record.getMessage()).append('\n');
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        private String contents() {
            return builder.toString();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
