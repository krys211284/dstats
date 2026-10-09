package krys.web;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.ApplicationAspectRegistry;
import krys.itemimport.AspectRuntimeStatus;
import krys.itemimport.FullItemRead;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportFormMapper;
import krys.itemimport.ItemImportValueMode;
import krys.itemimport.ValidatedImportedItem;
import krys.itemlibrary.EffectiveCurrentBuildResolution;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSource;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkingResolvedItemValueResolver;
import krys.socketing.ItemSocket;
import krys.socketing.ItemSocketing;
import krys.socketing.SocketGemRuneStat;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import krys.transfiguration.ItemTransfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(0.0d, resolution.getActiveItemsContribution().getStrength(), 0.0000001d);
        assertEquals(0.0d, resolution.getActiveItemsContribution().getIntelligence(), 0.0000001d);
        assertEquals(20.2d, runtimeStats.getCriticalChancePercent(), 0.0000001d);
        assertNotEquals(23.2d, runtimeStats.getCriticalChancePercent(), 0.0000001d);
        assertEquals(15.0d, resolution.getActiveHeroItemStats().getCriticalChancePercent(), 0.0000001d);
        assertEquals(AspectRuntimeStatus.DESCRIPTIVE_ONLY,
                ApplicationAspectRegistry.get().findById("heir_of_perdition").orElseThrow().getRuntimeStatus());
        assertNotEquals(36.36d, runtimeStats.getCriticalChancePercent(), 0.0000001d);
    }

    @Test
    void shouldKeepMythicUniqueArmorAlreadyCurrentAtQualityTwentyFive() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-mythic-armor");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem helmet = service.saveImportedItem(heirOfPerditionHelmet());
        HeroItemSelection selection = HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.HELMET, helmet.getItemId());

        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(legacyStats(), selection);

        assertEquals(2004L, resolution.getActiveHeroItemStats().getItemArmor());
        assertNotEquals(2505L, resolution.getActiveHeroItemStats().getItemArmor());
    }

    @Test
    void shouldKeepMythicUniqueDisplayedAffixCurrentWhenVerificationHasNoReferenceValue() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-mythic-displayed-affix");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        ItemImportEditableForm displayedForm = mythicDisplayedVerificationForm();

        ItemImportDisplayedValueFormNormalizer.NormalizedForm normalized =
                new ItemImportDisplayedValueFormNormalizer().normalize(displayedForm);
        ItemImportFormMapper.MappingResult mapping = new ItemImportFormMapper().map(normalized.form());

        assertTrue(normalized.errors().isEmpty());
        assertTrue(mapping.getErrors().isEmpty());
        assertNotNull(mapping.getItem());
        assertEquals(15.0d, mapping.getItem().getAffixes().getFirst().getValue(), 0.0001d);

        SavedImportedItem helmet = service.saveImportedItem(mapping.getItem(), displayedForm.getFullItemRead());
        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(
                legacyStats(),
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.HELMET, helmet.getItemId())
        );

        assertEquals(15.0d, resolution.getActiveItemsContribution().getCriticalChancePercent(), 0.0001d);
        assertEquals(15.0d, resolution.getActiveHeroItemStats().getCriticalChancePercent(), 0.0001d);
        assertNotEquals(12.0d, resolution.getActiveHeroItemStats().getCriticalChancePercent(), 0.0001d);
    }

    @Test
    void shouldPreserveExplicitMythicOrdinaryPerfectedAffixDuringDisplayedValueNormalization() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-mythic-explicit-perfect");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        ItemImportEditableForm displayedForm = mythicDisplayedVerificationForm(
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("CRITICAL_STRIKE_CHANCE"))
        );

        ItemImportDisplayedValueFormNormalizer.NormalizedForm normalized =
                new ItemImportDisplayedValueFormNormalizer().normalize(displayedForm);
        ItemImportFormMapper.MappingResult mapping = new ItemImportFormMapper().map(normalized.form());

        assertTrue(normalized.errors().isEmpty());
        assertTrue(mapping.getErrors().isEmpty());
        assertNotNull(mapping.getItem());
        assertEquals(15.0d, mapping.getItem().getAffixes().getFirst().getValue(), 0.0001d);
        assertNotNull(mapping.getItem().getMasterworking().getPerfectedAffix());
        assertEquals(MasterworkedAffixSource.ORDINARY_AFFIX,
                mapping.getItem().getMasterworking().getPerfectedAffix().getSource());
        assertEquals("CRITICAL_STRIKE_CHANCE", mapping.getItem().getMasterworking().getPerfectedAffix().getKey());

        SavedImportedItem helmet = service.saveImportedItem(mapping.getItem(), displayedForm.getFullItemRead());
        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(
                legacyStats(),
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.HELMET, helmet.getItemId())
        );

        assertEquals(15.0d, resolution.getActiveItemsContribution().getCriticalChancePercent(), 0.0001d);
        assertEquals(15.0d, resolution.getActiveHeroItemStats().getCriticalChancePercent(), 0.0001d);
        assertNotEquals(18.8d, resolution.getActiveHeroItemStats().getCriticalChancePercent(), 0.0001d);
    }

    @Test
    void shouldPreserveExplicitOrdinaryPerfectedAffixDuringDisplayedValueNormalization() {
        ItemImportEditableForm displayedForm = displayedStrengthGreaterAffixVerificationForm();

        ItemImportDisplayedValueFormNormalizer.NormalizedForm normalized =
                new ItemImportDisplayedValueFormNormalizer().normalize(displayedForm);
        ItemImportFormMapper.MappingResult mapping = new ItemImportFormMapper().map(normalized.form());

        assertTrue(normalized.errors().isEmpty());
        assertTrue(mapping.getErrors().isEmpty());
        assertNotNull(mapping.getItem());
        assertNotNull(mapping.getItem().getMasterworking().getPerfectedAffix());
        assertEquals(MasterworkedAffixSource.ORDINARY_AFFIX,
                mapping.getItem().getMasterworking().getPerfectedAffix().getSource());
        assertEquals("STRENGTH", mapping.getItem().getMasterworking().getPerfectedAffix().getKey());
        assertEquals(360.0d, masterworkingValueResolver().resolveAffixValue(
                mapping.getItem().getAffixes().getFirst(),
                mapping.getItem().getMasterworking()
        ), 0.0001d);
    }

    @Test
    void shouldSubmitDisplayedOrdinaryAffixReplacementAfterRemoveAndReAdd() {
        ItemImportEditableForm displayedForm = displayedShieldAfterOrdinaryAffixReplacement();

        ItemImportDisplayedValueFormNormalizer.NormalizedForm normalized =
                new ItemImportDisplayedValueFormNormalizer().normalize(displayedForm);
        ItemImportFormMapper.MappingResult mapping = new ItemImportFormMapper().map(normalized.form());

        assertTrue(normalized.errors().isEmpty(), normalized.errors().toString());
        assertTrue(mapping.getErrors().isEmpty(), mapping.getErrors().toString());
        assertNotNull(mapping.getItem());
        assertEquals(4, mapping.getItem().getAffixes().size());
        assertEquals(1202L, mapping.getItem().getDetails().getItemArmor());
        assertResolvedOrdinaryAffix(mapping.getItem(), ImportedItemAffixType.STRENGTH, 217.0d);
        assertResolvedOrdinaryAffix(mapping.getItem(), ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 11.0d);
        assertResolvedOrdinaryAffix(mapping.getItem(), ImportedItemAffixType.DAMAGE_REDUCTION, 17.6d);
        assertResolvedOrdinaryAffix(mapping.getItem(), ImportedItemAffixType.MOVEMENT_SPEED, 25.0d);
    }

    @Test
    void shouldSubmitDisplayedTemperingReplacementAndDetectPerfectedAffix() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-displayed-tempering-readd");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        ItemImportEditableForm displayedForm = displayedShieldAfterTemperingReplacement();

        ItemImportDisplayedValueFormNormalizer.NormalizedForm normalized =
                new ItemImportDisplayedValueFormNormalizer().normalize(displayedForm);
        ItemImportFormMapper.MappingResult mapping = new ItemImportFormMapper().map(normalized.form());

        assertTrue(normalized.errors().isEmpty(), normalized.errors().toString());
        assertTrue(mapping.getErrors().isEmpty(), mapping.getErrors().toString());
        assertNotNull(mapping.getItem());
        assertEquals(1, mapping.getItem().getTemperingAffixes().size());
        ItemTemperingAffix tempering = mapping.getItem().getTemperingAffixes().getFirst();
        assertEquals("defense_max_animus", tempering.getDefinitionId());
        assertEquals(5.0d, tempering.getValue(), 0.0001d);
        assertTrue(tempering.isGreaterAffix());
        assertNotNull(mapping.getItem().getMasterworking().getPerfectedAffix());
        assertEquals(MasterworkedAffixSource.TEMPERING_AFFIX,
                mapping.getItem().getMasterworking().getPerfectedAffix().getSource());
        assertEquals("defense_max_animus", mapping.getItem().getMasterworking().getPerfectedAffix().getKey());
        assertEquals(12.0d, masterworkingValueResolver().resolveTemperingValue(
                tempering,
                mapping.getItem().getMasterworking()
        ), 0.0001d);

        SavedImportedItem saved = service.saveImportedItem(mapping.getItem(), displayedForm.getFullItemRead());
        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(
                legacyStats(),
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.OFF_HAND, saved.getItemId())
        );

        assertEquals(12.0d, resolution.getActiveHeroItemStats().getMaxAnimusFromTempering(), 0.0001d);
        assertFalse(mapping.getErrors().contains("Hartowanie #1: Greater Affix przy hartowaniu jest dostępny tylko dla przedmiotów o mocy 900."));
    }

    @Test
    void shouldKeepDetectedSocketGemRuneStatsRuntimeInactive() throws Exception {
        Path tempDirectory = Files.createTempDirectory("runtime-input-socket-stat");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem helmet = service.saveImportedItem(heirOfPerditionHelmetWithDetectedSocketStats());
        CurrentBuildFormData formData = level70FormData();
        HeroItemSelection selection = HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.HELMET, helmet.getItemId());
        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(legacyStats(), selection);

        CurrentBuildImportableStats runtimeStats = resolver.resolve(hero(selection, formData), formData, resolution);

        assertEquals(79.0d, runtimeStats.getStrength(), 0.0001d);
        assertEquals(76.0d, runtimeStats.getIntelligence(), 0.0001d);
        assertEquals(0.0d, resolution.getActiveItemsContribution().getStrength(), 0.0001d);
        assertEquals(0.0d, resolution.getActiveItemsContribution().getIntelligence(), 0.0001d);
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

    private static ItemImportEditableForm mythicDisplayedVerificationForm() {
        return mythicDisplayedVerificationForm(new ItemMasterworking(25, 25));
    }

    private static ItemImportEditableForm mythicDisplayedVerificationForm(ItemMasterworking masterworking) {
        ItemImportDetails details = new ItemImportDetails(
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
                "Poddaj się nienawiści i doświadcz Łaski Matki.",
                true
        );
        return new ItemImportEditableForm(
                "dziedzic.png",
                "HELMET",
                "0",
                "",
                "",
                "",
                "",
                "",
                FullItemRead.empty(),
                List.of(new ImportedItemAffix(
                        ImportedItemAffixType.CRITICAL_STRIKE_CHANCE,
                        15.0d,
                        "%",
                        false,
                        0,
                        "+15,0% szansy na trafienie krytyczne",
                        ImportedItemAffixSource.OCR,
                        "critical_strike_chance",
                        null,
                        null,
                        null,
                        "15,0%"
                )),
                "heir_of_perdition",
                details,
                List.of(),
                masterworking,
                ItemTransfiguration.none(),
                ItemSocketing.empty(),
                ItemImportValueMode.GAME_DISPLAYED_VALUE
        );
    }

    private static ItemImportEditableForm displayedStrengthGreaterAffixVerificationForm() {
        ItemImportDetails details = new ItemImportDetails(
                "Tarcza GA",
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
        );
        return new ItemImportEditableForm(
                "tarcza-ga.png",
                "OFF_HAND",
                "0",
                "",
                "",
                "",
                "",
                "",
                FullItemRead.empty(),
                List.of(new ImportedItemAffix(
                        ImportedItemAffixType.STRENGTH,
                        360.0d,
                        "",
                        true,
                        0,
                        "+360 siły",
                        ImportedItemAffixSource.OCR,
                        "strength",
                        null,
                        null,
                        null,
                        "360"
                )),
                "",
                details,
                List.of(),
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("STRENGTH")),
                ItemTransfiguration.none(),
                ItemSocketing.empty(),
                ItemImportValueMode.GAME_DISPLAYED_VALUE
        );
    }

    private static ItemImportEditableForm displayedShieldAfterOrdinaryAffixReplacement() {
        ItemImportDetails details = displayedShieldDetails(1502L);
        return new ItemImportEditableForm(
                "tarcza-affix-readd.png",
                "OFF_HAND",
                "0",
                "",
                "",
                "",
                "",
                "",
                FullItemRead.empty(),
                List.of(
                        displayedAffix(ImportedItemAffixType.STRENGTH, 217.0d, "", false, 0,
                                "+217 siły [150 - 180]", "strength", 150.0d, 180.0d, "217"),
                        displayedAffix(ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 11.0d, "%", false, 1,
                                "+11,0% szansy na trafienie krytyczne", "critical_strike_chance", null, null, "11,0%"),
                        displayedAffix(ImportedItemAffixType.DAMAGE_REDUCTION, 17.6d, "%", false, 2,
                                "17,6% redukcji obrażeń", "damage_reduction", null, null, "17,6%"),
                        displayedAffix(ImportedItemAffixType.MOVEMENT_SPEED, 25.0d, "%", false, 3,
                                "+25,0% szybkości ruchu", "movement_speed", null, null, "25,0%")
                ),
                "",
                details,
                List.of(),
                new ItemMasterworking(25, 25),
                ItemTransfiguration.none(),
                ItemSocketing.empty(),
                ItemImportValueMode.GAME_DISPLAYED_VALUE
        );
    }

    private static ItemImportEditableForm displayedShieldAfterTemperingReplacement() {
        ItemImportDetails details = displayedShieldDetails(1502L);
        return new ItemImportEditableForm(
                "tarcza-tempering-readd.png",
                "OFF_HAND",
                "0",
                "",
                "",
                "",
                "",
                "",
                FullItemRead.empty(),
                List.of(),
                "",
                details,
                List.of(new ItemTemperingAffix(
                        "defense_max_animus",
                        TemperingCategory.DEFENSE,
                        12.0d,
                        "+12 do maksymalnej liczby kumulacji Animuszu",
                        "+12 do maksymalnej liczby kumulacji Animuszu",
                        TemperingRuntimeStatus.DATA_ONLY,
                        true
                )),
                new ItemMasterworking(25, 25),
                ItemTransfiguration.none(),
                ItemSocketing.empty(),
                ItemImportValueMode.GAME_DISPLAYED_VALUE
        );
    }

    private static ItemImportDetails displayedShieldDetails(Long armor) {
        return new ItemImportDetails(
                "Tarcza testowa",
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
                armor,
                ""
        );
    }

    private static ImportedItemAffix displayedAffix(ImportedItemAffixType type,
                                                    double displayedValue,
                                                    String unit,
                                                    boolean greaterAffix,
                                                    int displayOrder,
                                                    String rawLine,
                                                    String definitionId,
                                                    Double rollRangeMin,
                                                    Double rollRangeMax,
                                                    String displayValue) {
        return new ImportedItemAffix(
                type,
                displayedValue,
                unit,
                greaterAffix,
                displayOrder,
                rawLine,
                ImportedItemAffixSource.OCR,
                definitionId,
                rollRangeMin,
                rollRangeMax,
                null,
                displayValue
        );
    }

    private static void assertResolvedOrdinaryAffix(ValidatedImportedItem item,
                                                    ImportedItemAffixType type,
                                                    double expectedDisplayedValue) {
        ImportedItemAffix affix = item.getAffixes().stream()
                .filter(candidate -> candidate.getType() == type)
                .findFirst()
                .orElseThrow();
        assertEquals(expectedDisplayedValue, masterworkingValueResolver().resolveAffixValue(
                affix,
                item.getMasterworking()
        ), 0.0001d);
    }

    private static MasterworkingResolvedItemValueResolver masterworkingValueResolver() {
        return new MasterworkingResolvedItemValueResolver();
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
                ),
                List.of(),
                new ItemMasterworking(25, 25)
        );
    }

    private static ValidatedImportedItem heirOfPerditionHelmetWithDetectedSocketStats() {
        ValidatedImportedItem base = heirOfPerditionHelmet();
        return new ValidatedImportedItem(
                base.getSourceImageName(),
                base.getSlot(),
                base.getWeaponDamage(),
                base.getStrength(),
                base.getIntelligence(),
                base.getThorns(),
                base.getBlockChance(),
                base.getRetributionChance(),
                base.getAffixes(),
                base.getSelectedAspectId(),
                base.getDetails(),
                base.getTemperingAffixes(),
                base.getMasterworking(),
                base.getTransfiguration(),
                new ItemSocketing(2, List.of(
                        ItemSocket.detectedStat(0, SocketGemRuneStat.fromDetectedLine("+150 siły")),
                        ItemSocket.detectedStat(1, SocketGemRuneStat.fromDetectedLine("+120 siły"))
                ))
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

}
