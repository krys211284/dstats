package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadAffixUpdater;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImageMetadata;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportEditableFormFactory;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ItemImportFieldCandidate;
import krys.itemimport.ItemImportFieldConfidence;
import krys.itemimport.ItemImportFormMapper;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixRoll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje trwały zapis minimalnej biblioteki itemów bez bazy danych. */
class FileItemLibraryRepositoryTest {
    @Test
    void shouldPersistSavedItemsAndSelectionOnDisk() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-repo");
        FileItemLibraryRepository repository = new FileItemLibraryRepository(tempDirectory);

        SavedImportedItem firstItem = repository.save(new SavedImportedItem(
                0L,
                "OFF_HAND / tarcza-a.png",
                "tarcza-a.png",
                EquipmentSlot.OFF_HAND,
                0L,
                114.0d,
                0.0d,
                494.0d,
                20.0d,
                0.0d,
                new FullItemRead(
                        "Nieugaszony Bastion",
                        "Tarcza",
                        "Legendarny",
                        "800 mocy przedmiotu",
                        "1 131 pkt. pancerza",
                        List.of(new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 do siły [107 - 121]"))
                ),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 114.0d, "", true, 0, "* +114 do siły [107 - 121]", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.THORNS, 494.0d, "+494 cierni [473 - 506]")
                ),
                "inner-calm",
                ItemImportDetails.empty(),
                List.of(new ItemTemperingAffix(
                        "defense_maximum_life",
                        TemperingCategory.DEFENSE,
                        1500.0d,
                        "+1500 maksymalnego zdrowia",
                        TemperingRuntimeStatus.DATA_ONLY
                )),
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("STRENGTH"))
        ));
        SavedImportedItem secondItem = repository.save(new SavedImportedItem(
                0L,
                "OFF_HAND / tarcza-b.png",
                "tarcza-b.png",
                EquipmentSlot.OFF_HAND,
                0L,
                120.0d,
                0.0d,
                500.0d,
                22.0d,
                0.0d
        ));
        repository.saveSelection(new ActiveItemSelection(Map.of(EquipmentSlot.OFF_HAND, secondItem.getItemId())));

        FileItemLibraryRepository reloadedRepository = new FileItemLibraryRepository(tempDirectory);

        List<SavedImportedItem> savedItems = reloadedRepository.findAll();
        assertEquals(2, savedItems.size());
        assertEquals(firstItem.getItemId(), savedItems.get(0).getItemId());
        assertEquals(secondItem.getItemId(), savedItems.get(1).getItemId());
        assertEquals(EquipmentSlot.OFF_HAND, savedItems.get(0).getSlot());
        assertEquals("Nieugaszony Bastion", savedItems.get(0).getFullItemRead().getItemName());
        assertEquals("1 131 pkt. pancerza", savedItems.get(0).getFullItemRead().getBaseItemValue());
        assertEquals("+114 do siły [107 - 121]", savedItems.get(0).getFullItemRead().getLines().getFirst().getText());
        assertEquals(2, savedItems.get(0).getAffixes().size());
        assertEquals(ImportedItemAffixType.STRENGTH, savedItems.get(0).getAffixes().getFirst().getType());
        assertEquals(114.0d, savedItems.get(0).getAffixes().getFirst().getValue());
        assertEquals("* +114 do siły [107 - 121]", savedItems.get(0).getAffixes().getFirst().getSourceText());
        assertTrue(savedItems.get(0).getAffixes().getFirst().isGreaterAffix());
        assertEquals("inner-calm", savedItems.get(0).getSelectedAspectId());
        assertEquals(1, savedItems.get(0).getTemperingAffixes().size());
        assertEquals("defense_maximum_life", savedItems.get(0).getTemperingAffixes().getFirst().getDefinitionId());
        assertEquals(1500.0d, savedItems.get(0).getTemperingAffixes().getFirst().getValue(), 0.0000001d);
        assertEquals(TemperingRuntimeStatus.DATA_ONLY, savedItems.get(0).getTemperingAffixes().getFirst().getRuntimeStatus());
        assertFalse(savedItems.get(0).getTemperingAffixes().getFirst().isGreaterAffix());
        assertEquals(25, savedItems.get(0).getMasterworking().getQualityCurrent());
        assertEquals(25, savedItems.get(0).getMasterworking().getQualityMax());
        assertEquals("STRENGTH", savedItems.get(0).getMasterworking().getPerfectedAffix().getKey());
        assertEquals(0, savedItems.get(1).getMasterworking().getQualityCurrent());
        assertEquals(25, savedItems.get(1).getMasterworking().getQualityMax());
        assertTrue(reloadedRepository.findById(secondItem.getItemId()).isPresent());
        assertEquals(secondItem.getItemId(), reloadedRepository.loadSelection().getSelectedItemId(EquipmentSlot.OFF_HAND));
    }

    @Test
    void shouldReadLegacyMasterworkingEnabledPayloadWithoutUsingEnabledAsState() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-legacy-masterworking");
        String item = String.join("|",
                "ITEM",
                "1",
                encode("OFF_HAND / legacy.png"),
                encode("legacy.png"),
                EquipmentSlot.OFF_HAND.name(),
                "0",
                "0.0000",
                "0.0000",
                "0.0000",
                "0.0000",
                "0.0000",
                encode(""),
                encode(""),
                encode(""),
                encode(""),
                encode(""),
                encode("true|0|25")
        );
        String itemWithQuality = item.replace(encode("OFF_HAND / legacy.png"), encode("OFF_HAND / legacy-12.png"))
                .replace("ITEM|1|", "ITEM|2|")
                .replace(encode("true|0|25"), encode("true|12|25"));
        Files.write(tempDirectory.resolve("saved-items.db"), List.of(item, itemWithQuality), StandardCharsets.UTF_8);

        List<SavedImportedItem> savedItems = new FileItemLibraryRepository(tempDirectory).findAll();

        assertEquals(0, savedItems.get(0).getMasterworking().getQualityCurrent());
        assertEquals(25, savedItems.get(0).getMasterworking().getQualityMax());
        assertEquals(12, savedItems.get(1).getMasterworking().getQualityCurrent());
        assertEquals(25, savedItems.get(1).getMasterworking().getQualityMax());
    }

    @Test
    void shouldPersistTemperingPerfectedAffixSelection() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-masterworking-tempering");
        FileItemLibraryRepository repository = new FileItemLibraryRepository(tempDirectory);

        repository.save(new SavedImportedItem(
                0L,
                "OFF_HAND / tarcza.png",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                FullItemRead.empty(),
                List.of(),
                "",
                ItemImportDetails.empty(),
                List.of(new ItemTemperingAffix(
                        "defense_max_animus",
                        TemperingCategory.DEFENSE,
                        7.0d,
                        "+7 do maksymalnej liczby kumulacji Animuszu",
                        TemperingRuntimeStatus.DATA_ONLY,
                        true
                )),
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus"))
        ));

        SavedImportedItem reloaded = new FileItemLibraryRepository(tempDirectory).findAll().getFirst();

        assertEquals(25, reloaded.getMasterworking().getQualityCurrent());
        assertEquals("defense_max_animus", reloaded.getMasterworking().getPerfectedAffix().getKey());
    }

    @Test
    void shouldPersistHoradricTransfigurationOutcomes() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-transfiguration");
        FileItemLibraryRepository repository = new FileItemLibraryRepository(tempDirectory);

        repository.save(itemWithTransfiguration(ItemTransfiguration.transfigured(HoradricTransfigurationOutcome.INDESTRUCTIBLE)));
        repository.save(itemWithTransfiguration(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.UPGRADE_TO_GREATER_AFFIX,
                "DAMAGE_REDUCTION", null, "", null, null, false, "")));
        repository.save(itemWithTransfiguration(new ItemTransfiguration(
                true, true, HoradricTuningPrism.AGGRESSIVE, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "", new TransfigurationAffixRoll("PRIMARY_STAT", 180.0d), "", null, null, false, "")));
        repository.save(itemWithTransfiguration(new ItemTransfiguration(
                true, true, HoradricTuningPrism.KULLEAN, HoradricTransfigurationOutcome.REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX,
                "", null, "DAMAGE_REDUCTION", new TransfigurationAffixRoll("TOTAL_ARMOR_PERCENT", 10.0d), null, false, "")));
        repository.save(itemWithTransfiguration(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY,
                "", null, "", null, 15, false, "")));
        repository.save(itemWithTransfiguration(new ItemTransfiguration(
                true, false, HoradricTuningPrism.CHROMATIC, HoradricTransfigurationOutcome.UNKNOWN,
                "", null, "", null, null, false, "do sprawdzenia")));

        List<SavedImportedItem> reloaded = new FileItemLibraryRepository(tempDirectory).findAll();

        assertEquals(HoradricTransfigurationOutcome.INDESTRUCTIBLE, reloaded.get(0).getTransfiguration().getOutcome());
        assertTrue(reloaded.get(0).getTransfiguration().isIndestructible());
        assertEquals("DAMAGE_REDUCTION", reloaded.get(1).getTransfiguration().getUpgradedAffixRef());
        assertEquals("PRIMARY_STAT", reloaded.get(2).getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(180.0d, reloaded.get(2).getTransfiguration().getAddedTransfigurationAffix().getValue(), 0.0000001d);
        assertEquals(HoradricTuningPrism.AGGRESSIVE, reloaded.get(2).getTransfiguration().getTuningPrism());
        assertEquals("DAMAGE_REDUCTION", reloaded.get(3).getTransfiguration().getReplacedAffixRef());
        assertEquals("TOTAL_ARMOR_PERCENT", reloaded.get(3).getTransfiguration().getReplacementTransfigurationAffix().getDefinitionId());
        assertEquals(15, reloaded.get(4).getTransfiguration().getBonusQuality());
        assertFalse(reloaded.get(5).getTransfiguration().isLockedAfterTransfiguration());
        assertEquals("do sprawdzenia", reloaded.get(5).getTransfiguration().getNotes());
    }

    @Test
    void shouldReadLegacyItemsWithoutTransfigurationAsNotTransfigured() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-legacy-transfiguration");
        String item = String.join("|",
                "ITEM",
                "1",
                encode("OFF_HAND / legacy.png"),
                encode("legacy.png"),
                EquipmentSlot.OFF_HAND.name(),
                "0",
                "0.0000",
                "0.0000",
                "0.0000",
                "0.0000",
                "0.0000",
                encode(""),
                encode(""),
                encode(""),
                encode(""),
                encode(""),
                encode("0|25||")
        );
        Files.write(tempDirectory.resolve("saved-items.db"), List.of(item), StandardCharsets.UTF_8);

        SavedImportedItem reloaded = new FileItemLibraryRepository(tempDirectory).findAll().getFirst();

        assertFalse(reloaded.getTransfiguration().isTransfigured());
        assertFalse(reloaded.getTransfiguration().isLockedAfterTransfiguration());
        assertEquals(HoradricTransfigurationOutcome.NONE, reloaded.getTransfiguration().getOutcome());
    }

    @Test
    void shouldPersistBootItemStructureAfterDraftFormSaveAndReload() throws Exception {
        Path tempDirectory = Files.createTempDirectory("boot-item-library-repo");
        FileItemLibraryRepository repository = new FileItemLibraryRepository(tempDirectory);
        ItemLibraryService service = new ItemLibraryService(repository);
        FullItemRead fullItemRead = new FullItemRead(
                "Marsz Pokutnika",
                "Buty",
                "Rzadki przedmiot",
                "800 mocy przedmiotu",
                "354 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.ITEM_NAME, "Marsz Pokutnika"),
                        new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Buty"),
                        new FullItemReadLine(FullItemReadLineType.ITEM_POWER, "800 mocy przedmiotu"),
                        new FullItemReadLine(FullItemReadLineType.BASE_STAT, "354 pkt. pancerza"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+12,5% szybkości ruchu"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+7,0% uniku"),
                        new FullItemReadLine(FullItemReadLineType.SOCKET, "2 gniazda")
                )
        );
        ItemImageImportCandidateParseResult parseResult = new ItemImageImportCandidateParseResult(
                new ItemImageMetadata("buty.png", "image/png", "PNG", 1200, 800),
                fullItemRead,
                new ItemImportFieldCandidate<>("Buty", EquipmentSlot.BOOTS, ItemImportFieldConfidence.HIGH, "slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                ItemImportFieldCandidate.unknown("str"),
                ItemImportFieldCandidate.unknown("int"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution"),
                "Import wspomagany"
        );

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(parseResult);
        ItemImportFormMapper.MappingResult mappingResult = new ItemImportFormMapper().map(form);
        assertTrue(mappingResult.getErrors().isEmpty(), () -> String.join(", ", mappingResult.getErrors()));
        service.saveImportedItem(
                mappingResult.getItem(),
                new FullItemReadAffixUpdater().withEditedAffixes(form.getFullItemRead(), form.getAffixes())
        );

        SavedImportedItem savedItem = new FileItemLibraryRepository(tempDirectory).findAll().getFirst();

        assertEquals(EquipmentSlot.BOOTS, savedItem.getSlot());
        assertEquals("354 pkt. pancerza", savedItem.getFullItemRead().getBaseItemValue());
        assertEquals(2, savedItem.getAffixes().size());
        assertTrue(savedItem.getFullItemRead().getLines().stream()
                .filter(line -> line.getType() == FullItemReadLineType.BASE_STAT)
                .anyMatch(line -> line.getText().contains("354 pkt. pancerza")));
        assertFalse(savedItem.getAffixes().stream()
                .anyMatch(affix -> affix.getSourceText().contains("354 pkt. pancerza")));
    }

    @Test
    void shouldPersistVerathielWeaponDetailsUniqueEffectAndAffixes() throws Exception {
        Path tempDirectory = Files.createTempDirectory("verathiel-item-library-repo");
        FileItemLibraryRepository repository = new FileItemLibraryRepository(tempDirectory);
        ItemLibraryService service = new ItemLibraryService(repository);
        ItemImportDetails details = new ItemImportDetails(
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
        );
        FullItemRead fullItemRead = new FullItemRead(
                "Odłamek Verathiela",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "1 830 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+94 obrażeń od broni [94 - 157]"),
                        new FullItemReadLine(FullItemReadLineType.ASPECT, details.getUniqueEffectText())
                ),
                details
        );
        ItemImportEditableForm form = new ItemImportEditableForm(
                "miecz.png",
                "MAIN_HAND",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                fullItemRead,
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.WEAPON_DAMAGE_FLAT, 94.0d, "", false, 0, "+94 obrażeń od broni [94 - 157]", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.MAXIMUM_LIFE, 2141.0d, "", false, 1, "+2 141 maksymalnego zdrowia [1 831 - 2 200]", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.LIFE_ON_HIT, 545.0d, "", false, 2, "+545 pkt. zdrowia przy trafieniu [526 - 632]", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE, 15.0d, "%", false, 3, "Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]", ImportedItemAffixSource.OCR)
                ),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                details
        );

        ItemImportFormMapper.MappingResult mappingResult = new ItemImportFormMapper().map(form);
        assertTrue(mappingResult.getErrors().isEmpty(), () -> String.join(", ", mappingResult.getErrors()));
        service.saveImportedItem(mappingResult.getItem(), fullItemRead);

        SavedImportedItem savedItem = new FileItemLibraryRepository(tempDirectory).findAll().getFirst();

        assertEquals("Odłamek Verathiela", savedItem.getItemName());
        assertEquals("UNIQUE", savedItem.getItemRarity());
        assertTrue(savedItem.isAncient());
        assertEquals(EquipmentSlot.MAIN_HAND, savedItem.getEquipmentSlot());
        assertEquals(900L, savedItem.getItemPower());
        assertEquals(1830L, savedItem.getWeaponDps());
        assertEquals(1350L, savedItem.getWeaponDamageMin());
        assertEquals(1978L, savedItem.getWeaponDamageMax());
        assertEquals(1664L, savedItem.getAverageWeaponDamage());
        assertEquals(1.10d, savedItem.getAttacksPerSecond());
        assertEquals(0L, savedItem.getWeaponDamage());
        assertTrue(savedItem.getUniqueEffectText().contains("100%[x]"));
        assertTrue(savedItem.getUniqueEffectText().contains("[70 - 100]"));
        assertTrue(savedItem.getUniqueEffectText().contains("25 pkt. podstawowego zasobu"));
        assertEquals(4, savedItem.getAffixes().size());
        assertTrue(savedItem.getAffixes().stream()
                .anyMatch(affix -> affix.getSourceText().contains("+94 obrażeń od broni [94 - 157]")));
        assertTrue(savedItem.getAffixes().stream()
                .noneMatch(affix -> affix.getSourceText().contains("Umiejętności Podstawowe")));
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static SavedImportedItem itemWithTransfiguration(ItemTransfiguration transfiguration) {
        return new SavedImportedItem(
                0L,
                "OFF_HAND / tarcza.png",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                20.0d,
                0.0d,
                FullItemRead.empty(),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 225.0d, "", true, 0, "+225 siły", ImportedItemAffixSource.MANUAL),
                        new ImportedItemAffix(ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, "%", false, 1, "11,4% redukcji obrażeń", ImportedItemAffixSource.MANUAL)
                ),
                "",
                ItemImportDetails.empty(),
                List.of(),
                ItemMasterworking.defaultState(),
                transfiguration
        );
    }
}
