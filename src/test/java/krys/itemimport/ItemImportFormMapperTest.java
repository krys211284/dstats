package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkedAffixSource;
import krys.socketing.ItemSocket;
import krys.socketing.ItemSocketing;
import krys.socketing.SocketContentType;
import krys.socketing.SocketGemRuneStat;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixRoll;
import krys.transfiguration.TransfigurationValueProvenance;
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
    void shouldDefaultImportedItemToNoSockets() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0"
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(0, result.getItem().getSocketing().getSocketCount());
        assertTrue(result.getItem().getSocketing().getSockets().isEmpty());
    }

    @Test
    void shouldValidateSocketedGem() {
        ItemImportEditableForm form = socketedForm(new ItemSocketing(
                1,
                List.of(ItemSocket.gem(0, "diamond_grand"))
        ));

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(1, result.getItem().getSocketing().getSocketCount());
        assertEquals(SocketContentType.GEM, result.getItem().getSocketing().socketAt(0).getContentType());
        assertEquals("diamond_grand", result.getItem().getSocketing().socketAt(0).getGemId());
    }

    @Test
    void shouldValidateDetectedSocketGemRuneStatAsRuntimeInactiveSocketData() {
        ItemImportEditableForm form = socketedForm(new ItemSocketing(
                1,
                List.of(ItemSocket.detectedStat(0, SocketGemRuneStat.fromDetectedLine("+120 inteligencji")))
        ));

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(1, result.getItem().getSocketing().getSocketCount());
        assertEquals(1, result.getItem().getSocketing().getOccupiedSocketCount());
        assertEquals(SocketContentType.DETECTED_STAT, result.getItem().getSocketing().socketAt(0).getContentType());
        assertEquals("+120 inteligencji", result.getItem().getSocketing().socketAt(0).getDetectedStat().getDisplayText());
        assertEquals("DATA_ONLY", result.getItem().getSocketing().socketAt(0).getDetectedStat().getRuntimeStatus());
        assertEquals(0.0d, result.getItem().getIntelligence(), 0.0001d);
    }

    @Test
    void shouldRejectInvalidGemId() {
        ItemImportEditableForm form = socketedForm(new ItemSocketing(
                1,
                List.of(ItemSocket.gem(0, "diamond_fake"))
        ));

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertNull(result.getItem());
        assertTrue(result.getErrors().contains("Nieznany gem: diamond_fake"));
    }

    @Test
    void shouldRejectSocketCountOutsideSupportedRange() {
        ItemImportEditableForm form = socketedForm(ItemSocketing.emptySockets(3));

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertNull(result.getItem());
        assertTrue(result.getErrors().contains("Gniazda: liczba gniazd musi być od 0 do 2."));
    }

    @Test
    void shouldRejectExtraSocketRowsBeyondDeclaredCount() {
        ItemImportEditableForm form = socketedForm(new ItemSocketing(
                1,
                List.of(ItemSocket.empty(0), ItemSocket.gem(1, "ruby_grand"))
        ));

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertNull(result.getItem());
        assertTrue(result.getErrors().contains("Gniazda: liczba przesłanych gniazd nie może przekraczać wybranej liczby gniazd."));
    }

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

    private static ItemImportEditableForm socketedForm(ItemSocketing socketing) {
        return new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                ItemImportDetails.empty(),
                List.of(),
                ItemMasterworking.defaultState(),
                krys.transfiguration.ItemTransfiguration.none(),
                socketing
        );
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
    void shouldValidateMasterworkingQualityAllowedSteps() {
        for (int quality : ItemMasterworking.ALLOWED_QUALITY_STEPS) {
            assertMasterworkingAccepted(new ItemMasterworking(quality, 25));
        }

        for (int quality : List.of(-1, 1, 2, 4, 5, 7, 8, 10, 16, 18, 22, 24, 26)) {
            assertMasterworkingRejected(
                    new ItemMasterworking(quality, 25),
                    "Jakość Doskonalenia musi być jednym z progów: 0, 3, 6, 9, 12, 15, 17, 20, 21, 25"
            );
        }
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

    @Test
    void shouldDefaultMissingTransfigurationToNotTransfigured() {
        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(formWithTransfiguration(ItemTransfiguration.none()));

        assertTrue(result.getErrors().isEmpty(), () -> String.join(", ", result.getErrors()));
        assertFalse(result.getItem().getTransfiguration().isTransfigured());
        assertFalse(result.getItem().getTransfiguration().isLockedAfterTransfiguration());
        assertEquals(HoradricTuningPrism.NONE, result.getItem().getTransfiguration().getTuningPrism());
        assertEquals(HoradricTransfigurationOutcome.NONE, result.getItem().getTransfiguration().getOutcome());
    }

    @Test
    void shouldValidateUpgradeToGreaterAffixTransfiguration() {
        assertTransfigurationAccepted(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.UPGRADE_TO_GREATER_AFFIX,
                "DAMAGE_REDUCTION", null, "", null, null, false, ""));
        assertTransfigurationRejected(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.UPGRADE_TO_GREATER_AFFIX,
                "STRENGTH", null, "", null, null, false, ""), "nie może być już Greater Affix");
        assertTransfigurationRejected(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.UPGRADE_TO_GREATER_AFFIX,
                "TEMPERING_AFFIX:defense_max_animus", null, "", null, null, false, ""), "nie może wskazywać hartowania");
    }

    @Test
    void shouldValidateBonusTransfigurationAffixRoll() {
        assertTransfigurationAccepted(new ItemTransfiguration(
                true, true, HoradricTuningPrism.AGGRESSIVE, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "", sourceRoll("PRIMARY_STAT", 150.0d), "", null, null, false, ""));
        assertTransfigurationAccepted(new ItemTransfiguration(
                true, true, HoradricTuningPrism.AGGRESSIVE, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "", sourceRoll("PRIMARY_STAT", 180.0d), "", null, null, false, ""));
        assertTransfigurationRejected(new ItemTransfiguration(
                true, true, HoradricTuningPrism.AGGRESSIVE, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "", sourceRoll("PRIMARY_STAT", 149.0d), "", null, null, false, ""), "150-180");
        assertTransfigurationRejected(new ItemTransfiguration(
                true, true, HoradricTuningPrism.AGGRESSIVE, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "", sourceRoll("PRIMARY_STAT", 181.0d), "", null, null, false, ""), "150-180");
        assertTransfigurationAccepted(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "", sourceRoll("CRITICAL_STRIKE_CHANCE", 3.5d), "", null, null, false, ""));
        assertTransfigurationAccepted(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "", sourceRoll("CRITICAL_STRIKE_CHANCE", 5.0d), "", null, null, false, ""));
        assertTransfigurationRejected(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "", sourceRoll("CRITICAL_STRIKE_CHANCE", 3.4d), "", null, null, false, ""), "3,5-5%");
        assertTransfigurationRejected(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "", sourceRoll("CRITICAL_STRIKE_CHANCE", 5.1d), "", null, null, false, ""), "3,5-5%");
    }

    @Test
    void shouldValidateGameDisplayedTransfigurationValueAgainstMasterworkingQuality() {
        ItemTransfiguration displayedAllStats = new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "", new TransfigurationAffixRoll("ALL_STATS", 96.0d, TransfigurationValueProvenance.GAME_DISPLAYED_VALUE, ""),
                "", null, null, false, "");
        assertTransfigurationAccepted(displayedAllStats, new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus")));
        assertTransfigurationAccepted(displayedAllStats, new ItemMasterworking(0, 25));

        assertTransfigurationRejected(new ItemTransfiguration(
                        true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                        "", new TransfigurationAffixRoll("ALL_STATS", 140.0d, TransfigurationValueProvenance.GAME_DISPLAYED_VALUE, ""),
                        "", null, null, false, ""),
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus")),
                "93,75-125");
    }

    @Test
    void shouldValidateReplacementTransfiguration() {
        assertTransfigurationAccepted(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX,
                "", null, "DAMAGE_REDUCTION", new TransfigurationAffixRoll("TOTAL_ARMOR_PERCENT", 10.0d), null, false, ""));
        assertTransfigurationRejected(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX,
                "", null, "STRENGTH", new TransfigurationAffixRoll("TOTAL_ARMOR_PERCENT", 10.0d), null, false, ""), "nie może być już Greater Affix");
        assertTransfigurationRejected(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX,
                "", null, "TEMPERING_AFFIX:defense_max_animus", new TransfigurationAffixRoll("TOTAL_ARMOR_PERCENT", 10.0d), null, false, ""), "nie może wskazywać hartowania");
    }

    @Test
    void shouldValidateBonusItemQualityTransfiguration() {
        assertTransfigurationAccepted(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY,
                "", null, "", null, 1, false, ""));
        assertTransfigurationAccepted(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY,
                "", null, "", null, 15, false, ""));
        assertTransfigurationRejected(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY,
                "", null, "", null, 0, false, ""), "od 1 do 15");
        assertTransfigurationRejected(new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY,
                "", null, "", null, 16, false, ""), "od 1 do 15");
    }

    @Test
    void shouldClearInactiveTransfigurationFieldsForOutcome() {
        ItemTransfiguration indestructibleWithStaleRoll = new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.INDESTRUCTIBLE,
                "DAMAGE_REDUCTION",
                new TransfigurationAffixRoll("ALL_STATS", 96.0d, TransfigurationValueProvenance.GAME_DISPLAYED_VALUE, ""),
                "DAMAGE_REDUCTION",
                new TransfigurationAffixRoll("TOTAL_ARMOR_PERCENT", 10.0d),
                15,
                true,
                "");

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(formWithTransfiguration(indestructibleWithStaleRoll));

        assertTrue(result.getErrors().isEmpty(), () -> String.join(", ", result.getErrors()));
        ItemTransfiguration cleaned = result.getItem().getTransfiguration();
        assertEquals(HoradricTransfigurationOutcome.INDESTRUCTIBLE, cleaned.getOutcome());
        assertNull(cleaned.getAddedTransfigurationAffix());
        assertNull(cleaned.getReplacementTransfigurationAffix());
        assertEquals("", cleaned.getUpgradedAffixRef());
        assertEquals("", cleaned.getReplacedAffixRef());
        assertNull(cleaned.getBonusQuality());

        ItemTransfiguration bonusQualityWithStaleRoll = new ItemTransfiguration(
                true, true, HoradricTuningPrism.NONE, HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY,
                "DAMAGE_REDUCTION",
                new TransfigurationAffixRoll("ALL_STATS", 96.0d),
                "DAMAGE_REDUCTION",
                new TransfigurationAffixRoll("TOTAL_ARMOR_PERCENT", 10.0d),
                15,
                false,
                "");
        ItemImportFormMapper.MappingResult bonusQualityResult = new ItemImportFormMapper().map(formWithTransfiguration(bonusQualityWithStaleRoll));

        assertTrue(bonusQualityResult.getErrors().isEmpty(), () -> String.join(", ", bonusQualityResult.getErrors()));
        ItemTransfiguration bonusQualityCleaned = bonusQualityResult.getItem().getTransfiguration();
        assertEquals(HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY, bonusQualityCleaned.getOutcome());
        assertNull(bonusQualityCleaned.getAddedTransfigurationAffix());
        assertNull(bonusQualityCleaned.getReplacementTransfigurationAffix());
        assertEquals("", bonusQualityCleaned.getUpgradedAffixRef());
        assertEquals("", bonusQualityCleaned.getReplacedAffixRef());
        assertEquals(15, bonusQualityCleaned.getBonusQuality());
    }

    @Test
    void shouldKeepOnlyBonusTransfigurationAffixFieldsForRealShield() {
        ItemTransfiguration realShieldTransfiguration = new ItemTransfiguration(
                true,
                true,
                HoradricTuningPrism.NONE,
                HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "DAMAGE_REDUCTION",
                new TransfigurationAffixRoll("ALL_STATS", 96.0d, TransfigurationValueProvenance.GAME_DISPLAYED_VALUE, ""),
                "DAMAGE_REDUCTION",
                new TransfigurationAffixRoll("TOTAL_ARMOR_PERCENT", 10.0d),
                15,
                false,
                "");

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(
                formWithTransfiguration(realShieldTransfiguration,
                        new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus"))));

        assertTrue(result.getErrors().isEmpty(), () -> String.join(", ", result.getErrors()));
        ItemTransfiguration cleaned = result.getItem().getTransfiguration();
        assertEquals(HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX, cleaned.getOutcome());
        assertTrue(cleaned.isLockedAfterTransfiguration());
        assertEquals(HoradricTuningPrism.NONE, cleaned.getTuningPrism());
        assertEquals("ALL_STATS", cleaned.getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(96.0d, cleaned.getAddedTransfigurationAffix().getDisplayedValue());
        assertEquals(TransfigurationValueProvenance.GAME_DISPLAYED_VALUE, cleaned.getAddedTransfigurationAffix().getValueProvenance());
        assertEquals("", cleaned.getUpgradedAffixRef());
        assertEquals("", cleaned.getReplacedAffixRef());
        assertNull(cleaned.getReplacementTransfigurationAffix());
        assertNull(cleaned.getBonusQuality());
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

    private static void assertTransfigurationAccepted(ItemTransfiguration transfiguration) {
        assertTransfigurationAccepted(transfiguration, ItemMasterworking.defaultState());
    }

    private static void assertTransfigurationAccepted(ItemTransfiguration transfiguration, ItemMasterworking masterworking) {
        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(formWithTransfiguration(transfiguration, masterworking));

        assertTrue(result.getErrors().isEmpty(), () -> String.join(", ", result.getErrors()));
        assertEquals(transfiguration.getOutcome(), result.getItem().getTransfiguration().getOutcome());
    }

    private static void assertTransfigurationRejected(ItemTransfiguration transfiguration, String expectedErrorFragment) {
        assertTransfigurationRejected(transfiguration, ItemMasterworking.defaultState(), expectedErrorFragment);
    }

    private static void assertTransfigurationRejected(ItemTransfiguration transfiguration,
                                                      ItemMasterworking masterworking,
                                                      String expectedErrorFragment) {
        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(formWithTransfiguration(transfiguration, masterworking));

        assertNull(result.getItem());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains(expectedErrorFragment)),
                () -> String.join(", ", result.getErrors()));
    }

    private static ItemImportEditableForm formWithTransfiguration(ItemTransfiguration transfiguration) {
        return formWithTransfiguration(transfiguration, ItemMasterworking.defaultState());
    }

    private static ItemImportEditableForm formWithTransfiguration(ItemTransfiguration transfiguration, ItemMasterworking masterworking) {
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
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 225.0d, "", true, 0, "+225 siły", ImportedItemAffixSource.MANUAL),
                        new ImportedItemAffix(ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, "%", false, 1, "11,4% redukcji obrażeń", ImportedItemAffixSource.MANUAL)
                ),
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
                masterworking,
                transfiguration
        );
    }

    private static TransfigurationAffixRoll sourceRoll(String definitionId, double value) {
        return new TransfigurationAffixRoll(definitionId, value, TransfigurationValueProvenance.SOURCE_ROLL, "");
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
