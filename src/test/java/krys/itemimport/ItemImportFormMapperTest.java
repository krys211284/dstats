package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkedAffixSource;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy walidacji ręcznie poprawionego itemu przed zatwierdzeniem do modelu aplikacji. */
class ItemImportFormMapperTest {
    @Test
    void shouldValidateManuallyCorrectedMainHandItem() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "topor.png",
                "MAIN_HAND",
                "444",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 70.0d, "+70 siły"))
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(EquipmentSlot.MAIN_HAND, result.getItem().getSlot());
        assertEquals(444L, result.getItem().getWeaponDamage());
        assertEquals(70.0d, result.getItem().getStrength());
    }

    @Test
    void shouldRejectWeaponDamageOutsideMainHandSlot() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "150",
                "12",
                "0",
                "30",
                "18",
                "25"
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertNull(result.getItem());
        assertEquals(1, result.getErrors().size());
        assertEquals("Weapon damage można ustawić wyłącznie dla slotu MAIN_HAND.", result.getErrors().getFirst());
    }

    @Test
    void shouldProjectRuntimeStatsFromEditableAffixList() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "20.0",
                "0",
                new FullItemRead(
                        "Tarcza",
                        "Tarcza",
                        "Legendarny",
                        "Moc przedmiotu: 800",
                        "Pancerz: 1 131 pkt.",
                        List.of(new FullItemReadLine(FullItemReadLineType.AFFIX, "20,0% szansy na blok [20,01]%"))
                ),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 114.0d, "+114 siły [107 - 121]"),
                        new ImportedItemAffix(ImportedItemAffixType.THORNS, 494.0d, "+494 cierni [473 - 506]"),
                        new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_CHANCE, 7.0d, "+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%"),
                        new ImportedItemAffix(ImportedItemAffixType.COOLDOWN_REDUCTION, 13.2d, "13,2% redukcji czasu odnowienia")
                )
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(EquipmentSlot.OFF_HAND, result.getItem().getSlot());
        assertEquals(114.0d, result.getItem().getStrength());
        assertEquals(494.0d, result.getItem().getThorns());
        assertEquals(20.0d, result.getItem().getBlockChance());
        assertEquals(4, result.getItem().getAffixes().size());
    }

    @Test
    void shouldAcceptAspectAllowedForImportedItemSlot() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 225.0d)),
                "inner-calm",
                ItemImportFieldConfidence.HIGH,
                "inner-calm"
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertTrue(result.getErrors().isEmpty(), () -> String.join(", ", result.getErrors()));
        assertEquals("inner-calm", result.getItem().getSelectedAspectId());
    }

    @Test
    void shouldRejectAspectOutsideImportedItemSlot() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "buty.png",
                "BOOTS",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 225.0d)),
                "inner-calm",
                ItemImportFieldConfidence.HIGH,
                "inner-calm"
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertNull(result.getItem());
        assertTrue(result.getErrors().contains("Wybrany aspekt nie pasuje do slotu itemu."));
    }

    @Test
    void shouldRejectAspectWhenSlotWasChangedToIncompatibleOne() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "BOOTS",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(),
                "inner-calm",
                ItemImportFieldConfidence.HIGH,
                "inner-calm"
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertNull(result.getItem());
        assertTrue(result.getErrors().contains("Wybrany aspekt nie pasuje do slotu itemu."));
    }

    @Test
    void shouldValidateMasterworkingQualityRange() {
        assertMasterworkingAccepted(new ItemMasterworking(0, 25));
        assertMasterworkingAccepted(new ItemMasterworking(25, 25));

        assertMasterworkingRejected(new ItemMasterworking(-1, 25), "Jakość aktualna musi mieścić się w zakresie 0 - 25");
        assertMasterworkingRejected(new ItemMasterworking(26, 25), "Jakość aktualna musi mieścić się w zakresie 0 - 25");
        assertMasterworkingRejected(new ItemMasterworking(0, 0), "Jakość maksymalna musi wynosić 25");
        assertMasterworkingRejected(new ItemMasterworking(0, 24), "Jakość maksymalna musi wynosić 25");
    }

    @Test
    void shouldValidatePerfectedAffixOnlyForQualityTwentyFive() {
        assertMasterworkingRejected(
                new ItemMasterworking(24, 25, MasterworkedAffixSelection.ordinaryAffix("STRENGTH")),
                "aktualny doskonalony afiks można wskazać dopiero przy jakości 25/25"
        );
        assertMasterworkingAccepted(new ItemMasterworking(25, 25));
        assertMasterworkingAccepted(new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("STRENGTH")));
        assertMasterworkingAccepted(new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus")));
        assertMasterworkingRejected(
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("FIRE_RESISTANCE")),
                "wskazany zwykły affix nie występuje na itemie"
        );
        assertMasterworkingRejected(
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_maximum_life")),
                "wskazane hartowanie nie występuje na itemie"
        );
        assertMasterworkingRejected(
                new ItemMasterworking(25, 25, new MasterworkedAffixSelection(MasterworkedAffixSource.TEMPERING_AFFIX, "unknown_tempering")),
                "wskazane hartowanie nie istnieje w katalogu"
        );
    }

    @Test
    void shouldKeepSourceValuesWhenMasterworkingPresentationWouldIncreaseThem() {
        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(
                formWithMasterworking(new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("STRENGTH")))
        );

        assertTrue(result.getErrors().isEmpty(), () -> String.join(", ", result.getErrors()));
        ImportedItemAffix strength = result.getItem().getAffixes().stream()
                .filter(affix -> affix.getType() == ImportedItemAffixType.STRENGTH)
                .findFirst()
                .orElseThrow();
        ItemTemperingAffix maxAnimus = result.getItem().getTemperingAffixes().stream()
                .filter(affix -> affix.getDefinitionId().equals("defense_max_animus"))
                .findFirst()
                .orElseThrow();
        assertEquals(225.0d, strength.getValue());
        assertFalse(strength.getValue() == 270.0d);
        assertFalse(strength.getValue() == 360.0d);
        assertEquals(5.0d, maxAnimus.getValue());
        assertFalse(maxAnimus.getValue() == 7.0d);
        assertFalse(maxAnimus.getValue() == 12.0d);
    }

    private static void assertMasterworkingAccepted(ItemMasterworking masterworking) {
        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(formWithMasterworking(masterworking));

        assertTrue(result.getErrors().isEmpty(), () -> String.join(", ", result.getErrors()));
        assertEquals(masterworking.getQualityCurrent(), result.getItem().getMasterworking().getQualityCurrent());
        assertEquals(25, result.getItem().getMasterworking().getQualityMax());
        assertEquals(
                masterworking.getPerfectedAffix() == null ? null : masterworking.getPerfectedAffix().getKey(),
                result.getItem().getMasterworking().getPerfectedAffix() == null ? null : result.getItem().getMasterworking().getPerfectedAffix().getKey()
        );
    }

    private static void assertMasterworkingRejected(ItemMasterworking masterworking, String expectedErrorFragment) {
        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(formWithMasterworking(masterworking));

        assertNull(result.getItem());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains(expectedErrorFragment)),
                () -> String.join(", ", result.getErrors()));
    }

    private static ItemImportEditableForm formWithMasterworking(ItemMasterworking masterworking) {
        return new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "20",
                "0",
                FullItemRead.empty(),
                List.of(new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 225.0d)),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                new ItemImportDetails("Tarcza", "Tarcza", "LEGENDARY", true, EquipmentSlot.OFF_HAND,
                        900L, null, null, null, null, null, 1202L, ""),
                List.of(new ItemTemperingAffix(
                        "defense_max_animus",
                        TemperingCategory.DEFENSE,
                        5.0d,
                        "",
                        TemperingRuntimeStatus.DATA_ONLY,
                        true
                )),
                masterworking
        );
    }

    @Test
    void shouldKeepVerathielWeaponDetailsOutOfLegacyWeaponDamageInFullFlow() throws Exception {
        ItemImageImportCandidateParseResult parseResult = new ItemImageImportTextParser().parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                ItemImageImportTextParserTest.verathielRawText()
        );
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(parseResult);

        ItemImportFormMapper.MappingResult mappingResult = new ItemImportFormMapper().map(form);
        assertTrue(mappingResult.getErrors().isEmpty(), () -> String.join(", ", mappingResult.getErrors()));

        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(Files.createTempDirectory("verathiel-flow")));
        SavedImportedItem saved = service.saveImportedItem(mappingResult.getItem(), form.getFullItemRead());
        SavedImportedItem reloaded = service.requireItem(saved.getItemId());

        assertEquals(1830L, reloaded.getWeaponDps());
        assertEquals(1350L, reloaded.getWeaponDamageMin());
        assertEquals(1978L, reloaded.getWeaponDamageMax());
        assertEquals(1664L, reloaded.getAverageWeaponDamage());
        assertEquals(0L, reloaded.getWeaponDamage());
        assertFalse(reloaded.getWeaponDamage() == 1830L);
        assertFalse(reloaded.getAverageWeaponDamage() == 1758L);
        assertEquals("verathiel_shard", reloaded.getSelectedAspectId());
        assertEquals(4, reloaded.getAffixes().size());
        ImportedItemAffix weaponDamageAffix = reloaded.getAffixes().stream()
                .filter(affix -> affix.getType() == ImportedItemAffixType.WEAPON_DAMAGE_FLAT)
                .findFirst()
                .orElseThrow();
        assertEquals(94.0d, weaponDamageAffix.getValue());
        assertEquals(94.0d, weaponDamageAffix.getRollRangeMin());
        assertEquals(157.0d, weaponDamageAffix.getRollRangeMax());
        ImportedItemAffix lifeOnHitAffix = reloaded.getAffixes().stream()
                .filter(affix -> affix.getType() == ImportedItemAffixType.LIFE_ON_HIT)
                .findFirst()
                .orElseThrow();
        assertEquals(545.0d, lifeOnHitAffix.getValue());
        assertEquals(526.0d, lifeOnHitAffix.getRollRangeMin());
        assertEquals(632.0d, lifeOnHitAffix.getRollRangeMax());
        ImportedItemAffix luckyHitAffix = reloaded.getAffixes().stream()
                .filter(affix -> affix.getType() == ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE)
                .findFirst()
                .orElseThrow();
        assertEquals(3.0d, luckyHitAffix.getValue());
        assertEquals("+3", luckyHitAffix.getDisplayValue());
        assertEquals(3.0d, luckyHitAffix.getRollRangeMin());
        assertEquals(4.0d, luckyHitAffix.getRollRangeMax());
        assertTrue(reloaded.getUniqueEffectText().contains("100%[x]"));
    }
}
