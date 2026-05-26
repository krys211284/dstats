package krys.itemimport;

import krys.item.HeroEquipmentSlot;
import krys.hero.HeroClass;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import krys.web.HeroItemSelection;
import krys.web.HeroProfile;
import krys.web.ItemImportPageModel;
import krys.web.ItemImportPageRenderer;
import krys.web.ItemLibraryPageModel;
import krys.web.ItemLibraryPageRenderer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static krys.itemimport.ItemImportTextFixtures.realShieldBottomText;
import static krys.itemimport.ItemImportTextFixtures.realShieldTopText;
import static krys.itemimport.ItemImportTextFixtures.stormMoonShieldBottomText;
import static krys.itemimport.ItemImportTextFixtures.stormMoonShieldTopText;

/** Test realnego rozpoznania ograniczonych pól foundation z pojedynczego screena itemu. */
class ItemImageImportServiceTest {
    private static final List<String> STABLE_SHIELD_CONTRACT_LINES = List.of(
            "MIAŻDŻĄCA TARCZA KOŚCIANYCH ŁUSEK",
            "Starożytna legendarna tarcza",
            "Moc przedmiotu: 900",
            "Pancerz: 1 202 pkt.",
            "20,0% szansy na blok [20,0]%",
            "+100% obrażeń od broni w głównej ręce [100]%",
            "* +225 siły",
            "* +490 do odporności na wszystkie żywioły",
            "* +787 do odporności na: Ogień",
            "11,4% redukcji obrażeń [11,0 - 15,0]",
            "61%[x]"
    );
    private static final List<String> SHIELD_OCR_LINES_WITH_SEASONAL_NOISE = List.of(
            "MIAŻDŻĄCA TARCZA KOŚCIANYCH ŁUSEK",
            "Starożytna legendarna tarcza",
            "Moc przedmiotu: 900",
            "Pancerz: 1 202 pkt.",
            "20,0% szansy na blok [20,0]%",
            "+100% obrażeń od broni w głównej ręce [100]%",
            "+225 siły",
            "+490 do odporności na wszystkie żywioły",
            "+787 do odporności na: Ogień",
            "11,4% redukcji obrażeń [11,0 - 15,0]",
            "Gdy masz umocnienie, zadajesz obrażenia zwiększone o 610[x] [45 - 65]%. 70 poziomu",
            "Puste gniazdo"
    );

    @Test
    void shouldAcceptOneToFiveScreensAndRejectSixScreensForOneItem() throws Exception {
        byte[] imageBytes = buildShieldLikeScreenshot();
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new FakeOcrTextReader(Map.of("original", "Tarcza\nMoc przedmiotu: 900")),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        for (int count = 1; count <= 5; count++) {
            ItemImageImportCandidateParseResult result = service.analyze(repeatedRequests(imageBytes, count));
            assertNotNull(result.getFullItemRead());
        }

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.analyze(repeatedRequests(imageBytes, 6)));
        assertEquals("Można przesłać maksymalnie 5 screenów jednego itemu.", exception.getMessage());
    }

    @Test
    void shouldAnalyzeTwoScreensAsOneMergedItem() throws Exception {
        byte[] imageBytes = buildShieldLikeScreenshot();
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new QueuedOcrTextReader(List.of(
                        realShieldTopText(),
                        realShieldBottomText()
                )),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(List.of(
                new ItemImageImportRequest("tarcza1.png", "image/png", imageBytes),
                new ItemImageImportRequest("tarcza2.png", "image/png", imageBytes)
        ));
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals("tarcza1.png, tarcza2.png", result.getImageMetadata().getOriginalFilename());
        assertTrue(result.getImportNotice().contains("Import wieloscreenowy: 2 obrazy scalone jako jeden item."));
        assertEquals("Miażdżąca Tarcza Kościanych Łusek", form.getItemName());
        assertEquals("1202", form.getItemArmor());
        assertEquals(25, form.getMasterworking().getQualityCurrent());
        assertEquals("defense_max_animus", form.getMasterworking().getPerfectedAffix().getKey());
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(96.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue());
    }

    @Test
    void shouldMergeComplementaryOcrVariantsDuringMultiScreenImport() throws Exception {
        byte[] imageBytes = buildShieldLikeScreenshot();
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new QueuedVariantOcrTextReader(List.of(
                        Map.of(
                                "original", """
                                        Miażdżąca Tarcza Kościanych Łusek
                                        Starożytna legendarna tarcza
                                        25 (+25) jakości
                                        20,0% szansy na blok [20,0]%
                                        +100% obrażeń od broni w głównej ręce [100]%
                                        """,
                                "text-crop", """
                                        Moc przedmiotu: 900
                                        1 502 pkt. pancerza
                                        +100% obrażeń od broni w głównej ręce [100]%
                                        +270 siły
                                        +588 do odporności na wszystkie żywioły
                                        +945 do odporności na: Ogień
                                        14,3% redukcji obrażeń [11,0 - 15,0]%
                                        """,
                                "text-crop-gray-x2-contrast", """
                                        MIAŻDŻĄCA TARCZA KOŚCIANYCH ŁUSEK Moc przedmiotu 900 25 (+25) jakości Przeistoczony +270 siły +588 do odporności na wszystkie żywioły +945 do odporności na: Ogień
                                        +388 do odporności na wszystkie żywioły
                                        +943 do odporności na: Ogień
                                        1181,3% redukcji obrażeń
                                        +1001 siły
                                        """
                        ),
                        Map.of(
                                "original", """
                                        Przewiń do góry
                                        14,3% redukcji obrażeń [11,0 - 15,0]% +96 pkt. do wszystkich współczynników [+75 - 100]
                                        +12 do maksymalnej liczby kumulacji Animuszu
                                        Gdy masz umocnienie, zadajesz obrażenia zwiększone o 610[x] [45 - 65]%. 70 poziomu
                                        Brak możliwości modyfikacji
                                        """,
                                "text-crop", """
                                        +96 pkt. do wszystkich współczynników [+75 - 100]
                                        +12 do maksymalnej liczby kumulacji Animuszu
                                        """
                        )
                )),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(List.of(
                new ItemImageImportRequest("tarcza1.png", "image/png", imageBytes),
                new ItemImageImportRequest("tarcza2.png", "image/png", imageBytes)
        ));
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertTrue(result.getImportNotice().contains("Import wieloscreenowy: 2 obrazy scalone jako jeden item."));
        assertTrue(result.getImportNotice().contains("OCR analizował"));
        assertFalse(result.getImportNotice().contains("Linie OCR przed merge:"));
        assertFalse(result.getImportNotice().contains("Canonical source text used by parser:"));
        assertFalse(result.getImportNotice().contains("Raw OCR variants debug:"));
        assertFalse(result.getImportNotice().contains("---original---"));
        assertFalse(result.getImportNotice().contains("---text-crop"));
        assertEquals("Miażdżąca Tarcza Kościanych Łusek", form.getItemName());
        assertEquals(25, form.getMasterworking().getQualityCurrent());
        assertEquals("defense_max_animus", form.getMasterworking().getPerfectedAffix().getKey());
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.STRENGTH, 225.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.ALL_RESISTANCE, 490.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.FIRE_RESISTANCE, 787.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, false);
        assertEquals(1, countFullReadLines(result, FullItemReadLineType.IMPLICIT, "20,0% szansy na blok [20,0]%"));
        assertEquals(1, countFullReadLines(result, FullItemReadLineType.IMPLICIT, "+100% obrażeń od broni w głównej ręce [100]%"));
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(96.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue());
        assertEquals("fortify_damage_increased", form.getSelectedAspectId());
        assertEquals("fortify_damage_increased", form.getOcrSuggestedAspectId());
        assertEquals(4, form.getAffixes().size());

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                result,
                List.of(),
                null,
                new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty()),
                "Import testowy",
                ""
        ));
        assertFalse(html.contains("Canonical source text used by parser"));
        assertFalse(html.contains("Raw OCR variants debug"));
        assertFalse(html.contains("---original---"));
        assertFalse(html.contains("---text-crop"));
        assertTrue(html.contains("Miażdżąca Tarcza Kościanych Łusek"));
        assertTrue(html.contains("Umocnienie: zwiększone obrażenia"));
        assertTrue(html.contains("name=\"transfigurationAddedDisplayedValue\" step=\"0.1\" value=\"96\""));
    }

    @Test
    void shouldImportStormMoonFrenzyShieldFromTwoScreensWithoutManualFixes() throws Exception {
        byte[] imageBytes = buildShieldLikeScreenshot();
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new QueuedOcrTextReader(List.of(
                        stormMoonShieldTopText(),
                        stormMoonShieldBottomText()
                )),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(List.of(
                new ItemImageImportRequest("burza-gora.png", "image/png", imageBytes),
                new ItemImageImportRequest("burza-dol.png", "image/png", imageBytes)
        ));
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals("Tarcza Burzy Księżycowego Szału", form.getItemName());
        assertEquals("Tarcza", form.getItemType());
        assertEquals("LEGENDARY", form.getItemRarity());
        assertTrue(form.isAncient());
        assertEquals("OFF_HAND", form.getSlot());
        assertEquals("900", form.getItemPower());
        assertEquals("1202", form.getItemArmor());
        assertEquals(25, form.getMasterworking().getQualityCurrent());
        assertEquals(25, form.getMasterworking().getQualityMax());
        assertEquals("defense_max_animus", form.getMasterworking().getPerfectedAffix().getKey());

        assertEquals(4, form.getAffixes().size());
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.STRENGTH, 173.6d, false);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 8.8d, false);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.DAMAGE_REDUCTION, 14.08d, false);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.COOLDOWN_REDUCTION, 10.25d, true);
        assertTrue(form.getAffixes().stream().noneMatch(affix -> affix.getSourceText().contains("jakości przedmiotu")));
        assertTrue(form.getAffixes().stream().noneMatch(affix -> affix.getSourceText().contains("maksymalnej liczby kumulacji Animuszu")));
        assertTrue(form.getAffixes().stream().noneMatch(affix -> affix.getSourceText().contains("Puste gniazdo")));

        assertEquals(1, form.getTemperingAffixes().size());
        assertEquals("defense_max_animus", form.getTemperingAffixes().getFirst().getDefinitionId());
        assertEquals(5.0d, form.getTemperingAffixes().getFirst().getValue());
        assertTrue(form.getTemperingAffixes().getFirst().isGreaterAffix());
        assertEquals(1, form.getSocketing().getSocketCount());
        assertTrue(form.getSocketing().socketAt(0).getGemId().isBlank());
        assertEquals(krys.transfiguration.HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY,
                form.getTransfiguration().getOutcome());
        assertEquals(4, form.getTransfiguration().getBonusQuality());
        assertEquals("naznaczenie_aspect", form.getSelectedAspectId());
        assertEquals("naznaczenie_aspect", form.getOcrSuggestedAspectId());
        assertTrue(form.getUniqueEffectText().contains("Wampirycznego Szału Krwi"));
        assertEquals(1, countFullReadLines(result, FullItemReadLineType.IMPLICIT, "20,0% szansy na blok [20,0]%"));
        assertEquals(1, countFullReadLines(result, FullItemReadLineType.IMPLICIT, "+100% obrażeń od broni w głównej ręce [100]%"));

        String allReadLines = result.getFullItemRead().getLines().stream()
                .map(FullItemReadLine::getText)
                .reduce("", (left, right) -> left + "\n" + right);
        assertFalse(allReadLines.contains("Rynsztunek w Zbrojowni"));
        assertFalse(allReadLines.contains("Przewiń"));
        assertFalse(allReadLines.contains("Wartość sprzedaży"));

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                result,
                List.of(),
                null,
                new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty()),
                "Import testowy",
                ""
        ));
        assertFalse(html.contains("Canonical source text used by parser"));
        assertFalse(html.contains("Raw OCR variants debug"));
        assertFalse(html.contains("---original---"));
        assertTrue(html.contains("Tarcza Burzy Księżycowego Szału"));
        assertTrue(html.contains("Naznaczenie"));
        assertTrue(html.contains("name=\"transfigurationBonusQuality\" min=\"1\" max=\"15\" step=\"1\" value=\"4\""));
        String stormFullReadHeader = itemReadHeaderByHeading(html, "Pełny odczyt widocznego itemu");
        assertFalse(stormFullReadHeader.contains("<div class=\"summary-label\">Pancerz</div>"), stormFullReadHeader);
        String stormShieldFields = fieldSetByLegend(html, "Dane tarczy");
        assertTrue(stormShieldFields.contains("name=\"itemArmor\" value=\"1202\""), stormShieldFields);
        assertTrue(stormShieldFields.contains("<span class=\"masterworking-value masterworking-value--upgraded\">1502</span>"), stormShieldFields);
        assertFalse(html.contains("Doskonalenie: brak reguły prezentacyjnej dla tego affixu"));
        assertTrue(html.contains("name=\"affixValue_0\" value=\"173.6\""));
        assertTrue(html.contains("name=\"affixValue_1\" value=\"8.8\""));
        assertTrue(html.contains("name=\"affixValue_2\" value=\"14.08\""));
        assertTrue(html.contains("name=\"affixValue_3\" value=\"10.25\""));
        assertTrue(html.contains(">217<"));
        assertTrue(html.contains(">11,0%<"));
        assertTrue(html.contains(">17,6%<"));
        assertTrue(html.contains(">12,3%<"));

        ItemImportFormMapper.MappingResult mappingResult = new ItemImportFormMapper().map(form);
        assertTrue(mappingResult.getErrors().isEmpty(), () -> String.join(", ", mappingResult.getErrors()));
        ValidatedImportedItem item = mappingResult.getItem();
        assertEquals(1202L, item.getItemArmor());
        assertEquals(25, item.getMasterworking().getQualityCurrent());
        assertEquals(4, item.getAffixes().size());
        assertEquals("naznaczenie_aspect", item.getSelectedAspectId());
        assertEquals(krys.transfiguration.HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY,
                item.getTransfiguration().getOutcome());
        assertEquals(4, item.getTransfiguration().getBonusQuality());
    }

    @Test
    void shouldMergeFieldsAcrossPreparedVariantsWithoutChangingImportFlow() throws Exception {
        byte[] imageBytes = buildShieldLikeScreenshot();
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new FakeOcrTextReader(Map.of(
                        "original", "PRZEWIN W DOL",
                        "text-crop", "Tarcza",
                        "text-crop-gray-x2-contrast", "+114 do siły [107 - 121]",
                        "text-crop-gray-x3-threshold", "+494 do cierni [473 - 506]\n+20,0% szansy na blok [18,0 - 22,5]",
                        "text-crop-gray-x3-sharpen", ""
                )),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(
                new ItemImageImportRequest("shield-like.png", "image/png", imageBytes)
        );

        assertEquals("shield-like.png", result.getImageMetadata().getOriginalFilename());
        assertEquals("OFF_HAND", result.getSlotCandidate().getSuggestedValue().name());
        assertEquals(114.0d, result.getStrengthCandidate().getSuggestedValue(),
                () -> result.getStrengthCandidate().getRawValue());
        assertEquals(494.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertNull(result.getIntelligenceCandidate().getSuggestedValue());
    }

    @Test
    void shouldMapKoscianychLusekShieldFromSnapshotOnlyWithoutClaimingTarczaPngFixture() throws Exception {
        byte[] imageBytes = buildShieldLikeScreenshot();
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new CapturedOcrSnapshotReader(Path.of("src/test/resources/items/tarcza-koscianych-lusek-ocr-snapshot.txt")),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(
                new ItemImageImportRequest("tarcza-koscianych-lusek-snapshot.png", "image/png", imageBytes)
        );

        assertEquals("tarcza-koscianych-lusek-snapshot.png", result.getImageMetadata().getOriginalFilename());
        assertShieldFoundationMapping(result);
        assertNull(result.getWeaponDamageCandidate().getSuggestedValue());
        assertHeaderAndUnrelatedNumbersDoNotLeakToShieldFoundationCandidates(result);
        assertEquals("Miażdżąca Tarcza Kościanych Łusek", result.getFullItemRead().getDetails().getItemName());
        assertEquals(1202L, result.getFullItemRead().getDetails().getItemArmor());
        assertFullReadContains(result, "Moc przedmiotu: 900");
        assertFullReadContains(result, "20,0% szansy na blok [20,0]%");
        assertFullReadContains(result, "+100% obrażeń od broni w głównej ręce [100]%");
        assertFullReadContains(result, "+225 siły");
        assertFullReadContains(result, "+490 do odporności na wszystkie żywioły");
        assertFullReadContains(result, "+787 do odporności na: Ogień");
        assertFullReadContains(result, "11,4% redukcji obrażeń");
        assertFullReadContains(result, "61%[x]");
        assertFullReadDoesNotContain(result, "[1001");
        assertFullReadDoesNotContain(result, "610[x]");
        assertFullReadDoesNotContain(result, "70 poziomu");

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);
        assertEquals("fortify_damage_increased", form.getOcrSuggestedAspectId());
        assertEquals(ItemImportFieldConfidence.HIGH, form.getOcrAspectConfidence());
        assertEquals("fortify_damage_increased", form.getSelectedAspectId());
        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                result,
                List.of(),
                null,
                new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty()),
                "Import testowy",
                ""
        ));
        assertTrue(html.contains("Miażdżąca Tarcza Kościanych Łusek"));
        assertTrue(html.contains("LEGENDARY / Legendarny"));
        assertTrue(html.contains("name=\"itemPower\" value=\"900\""));
        String fullReadHeader = itemReadHeaderByHeading(html, "Pełny odczyt widocznego itemu");
        assertFalse(fullReadHeader.contains("<div class=\"summary-label\">Pancerz</div>"), fullReadHeader);
        assertTrue(html.contains("Dane tarczy"));
        String shieldFields = fieldSetByLegend(html, "Dane tarczy");
        assertTrue(shieldFields.contains("name=\"itemArmor\" value=\"1202\""), shieldFields);
        assertTrue(html.contains("Linie bazowe"));
        assertFalse(html.contains("Linie bazowe / implicit"));
        assertFalse(html.contains("Linie bazowe / implicity"));
        assertTrue(html.contains("20,0% szansy na blok [20,0]%"));
        assertTrue(html.contains("+100% obrażeń od broni w głównej ręce [100]%"));
        assertTrue(html.contains("Odporność na wszystkie żywioły"));
        assertTrue(html.contains("Odporność na Ogień"));
        assertTrue(html.contains("Redukcja obrażeń"));
        assertTrue(html.contains("11,0 - 15,0"));
        assertTrue(html.contains("Umocnienie: zwiększone obrażenia"));
        assertTrue(html.contains("61%[x]"));
        assertTrue(html.contains("45 - 65"));
        assertTrue(html.contains("name=\"affixValue_"));
        assertFalse(html.contains("11 - 0"));
        assertFalse(html.contains("610[x]"));
        assertFalse(html.contains("70 poziomu"));
        assertEquals(3, countCheckedGreaterAffixes(html));
        assertFalse(html.contains("<legend>Dane broni</legend>"));
        assertFalse(html.contains("DPS broni"));
        assertFalse(html.contains("Obrażenia za trafienie min"));
        assertFalse(html.contains("Obrażenia za trafienie max"));
        assertFalse(html.contains("Średnie obrażenia trafienia"));
        assertFalse(html.contains("Ataki na sekundę"));
        assertFalse(html.contains("<option value=\"inner-calm\" selected"));
        ItemImportFormMapper.MappingResult mappingResult = new ItemImportFormMapper().map(form);
        assertTrue(mappingResult.getErrors().isEmpty(), () -> String.join(", ", mappingResult.getErrors()));
        ValidatedImportedItem item = mappingResult.getItem();
        assertEquals(225.0d, item.getStrength());
        assertNotEquals(450.0d, item.getStrength());
        assertEquals(0.0d, item.getThorns());
        assertEquals(20.0d, item.getBlockChance());
        assertEquals(1202L, item.getItemArmor());

        Path libraryDirectory = Files.createTempDirectory("shield-library-import");
        ItemLibraryService libraryService = new ItemLibraryService(new FileItemLibraryRepository(libraryDirectory));
        SavedImportedItem savedShield = libraryService.saveImportedItem(item, result.getFullItemRead());
        assertEquals("Miażdżąca Tarcza Kościanych Łusek", savedShield.getItemName());
        assertEquals(1202L, savedShield.getItemArmor());
        assertEquals("fortify_damage_increased", savedShield.getSelectedAspectId());
        assertEquals(4, savedShield.getAffixes().size());
        assertEquals(1, libraryService.getCompatibleItems(HeroEquipmentSlot.OFF_HAND).size());
        String libraryHtml = new ItemLibraryPageRenderer().render(new ItemLibraryPageModel(
                libraryService.getSavedItems(),
                new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty()),
                HeroItemSelection.empty(),
                List.of(),
                List.of(),
                "",
                null
        ));
        assertTrue(libraryHtml.contains("Pancerz"));
        assertTrue(libraryHtml.contains("1202"));
        assertTrue(libraryHtml.contains("Linie bazowe"));
        assertFalse(libraryHtml.contains("Implicit / linie bazowe"));
        assertTrue(libraryHtml.contains("20,0% szansy na blok [20,0]%"));
        assertTrue(libraryHtml.contains("+100% obrażeń od broni w głównej ręce [100]%"));
        assertTrue(libraryHtml.contains("Umocnienie: zwiększone obrażenia"));

        assertExactlyOnePerLine(result, List.of(
                "20,0% szansy na blok",
                "+100% obrażeń od broni w głównej ręce",
                "+225 siły",
                "+490 do odporności na wszystkie żywioły",
                "+787 do odporności na: Ogień",
                "11,4% redukcji obrażeń"
        ));

        assertAffixTypeOccursOnce(form, ImportedItemAffixType.STRENGTH);
        assertAffixTypeOccursOnce(form, ImportedItemAffixType.ALL_RESISTANCE);
        assertAffixTypeOccursOnce(form, ImportedItemAffixType.FIRE_RESISTANCE);
        assertAffixTypeOccursOnce(form, ImportedItemAffixType.DAMAGE_REDUCTION);
        assertEquals(4, form.getAffixes().size());
        assertAffixGreaterFlag(form, ImportedItemAffixType.STRENGTH, true);
        assertAffixGreaterFlag(form, ImportedItemAffixType.ALL_RESISTANCE, true);
        assertAffixGreaterFlag(form, ImportedItemAffixType.FIRE_RESISTANCE, true);
        assertAffixGreaterFlag(form, ImportedItemAffixType.DAMAGE_REDUCTION, false);
    }

    @Test
    void shouldRenderFinalShieldHtmlWithNormalizedAspectRangeAndGreaterAffixesFromFullFlow() throws Exception {
        byte[] imageBytes = buildShieldLikeScreenshot();
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new FakeOcrTextReader(Map.of(
                        "original", """
                                MIAŻDŻĄCA TARCZA KOŚCIANYCH ŁUSEK
                                Starożytna legendarna tarcza
                                Moc przedmiotu: 900
                                1 202 pkt. pancerza
                                20,0% szansy na blok
                                +100% obrażeń od broni w głównej ręce [1001
                                • +225 siły
                                • +490 do odporności na wszystkie żywioły
                                • +787 do odporności na: Ogień
                                11,4% redukcji obrażeń [11 - 0]
                                Gdy masz umocnienie, zadajesz obrażenia zwiększone 0 610/01x] [45 - 651%. 70 poziomu
                                """,
                        "text-crop", "MIAŻDŻĄCA TARCZA KOŚCIANYCH ŁUSEK\nStarożytna legendarna tarcza\nMoc przedmiotu: 900\nPancerz: 1 202 pkt.",
                        "shield-affix-crop-gray-x4-sharpen", "• +225 siły\n• +490 do odporności na wszystkie żywioły\n• +787 do odporności na: Ogień\n11,4% redukcji obrażeń [11 - 0]",
                        "shield-aspect-crop-gray-x4-sharpen", "Gdy masz umocnienie, zadajesz obrażenia zwiększone 0 610/01x] [45 - 651%. 70 poziomu"
                )),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(
                new ItemImageImportRequest("tarcza-koscianych-lusek-ui.png", "image/png", imageBytes)
        );
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);
        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                result,
                List.of(),
                null,
                new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty()),
                "Import testowy",
                ""
        ));

        assertTrue(html.contains("Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%."));
        assertTrue(html.contains("<option value=\"fortify_damage_increased\" data-allowed-slots=\"OFF_HAND\" selected>Umocnienie: zwiększone obrażenia</option>"));
        assertFalse(html.contains("610/01x"));
        assertFalse(html.contains("610[x]"));
        assertFalse(html.contains("651"));
        assertFalse(html.contains("70 poziomu"));
        assertTrue(html.contains("Dane tarczy"));
        assertFalse(html.contains("<legend>Dane broni</legend>"));
        assertFalse(html.contains("DPS broni"));
        assertFalse(html.contains("Ataki na sekundę"));
        assertTrue(html.contains("+100% obrażeń od broni w głównej ręce [100]%"));
        assertTrue(html.contains("20,0% szansy na blok [20,0]%"));
        assertTrue(html.contains("<option value=\"OFF_HAND\" selected>Tarcza</option>"));

        String strengthRow = affixRow(html, "Siła");
        assertTrue(strengthRow.contains("value=\"225\""), strengthRow);
        assertTrue(strengthRow.contains("value=\"true\" checked"), strengthRow);
        assertTrue(strengthRow.contains("Bez zakresu (Greater Affix)"), strengthRow);
        assertFalse(strengthRow.contains("Brak zakresu"), strengthRow);

        String allResistanceRow = affixRow(html, "Odporność na wszystkie żywioły");
        assertTrue(allResistanceRow.contains("value=\"490\""), allResistanceRow);
        assertTrue(allResistanceRow.contains("value=\"true\" checked"), allResistanceRow);
        assertTrue(allResistanceRow.contains("Bez zakresu (Greater Affix)"), allResistanceRow);

        String fireResistanceRow = affixRow(html, "Odporność na Ogień");
        assertTrue(fireResistanceRow.contains("value=\"787\""), fireResistanceRow);
        assertTrue(fireResistanceRow.contains("value=\"true\" checked"), fireResistanceRow);
        assertTrue(fireResistanceRow.contains("Bez zakresu (Greater Affix)"), fireResistanceRow);

        String damageReductionRow = affixRow(html, "Redukcja obrażeń");
        assertTrue(damageReductionRow.contains("value=\"11.4\""), damageReductionRow);
        assertTrue(damageReductionRow.contains("11,0 - 15,0"), damageReductionRow);
        assertFalse(damageReductionRow.contains("Brak zakresu"), damageReductionRow);
        assertFalse(damageReductionRow.contains("value=\"true\" checked"), damageReductionRow);

        assertGreaterAffixWithoutRollRange(form, ImportedItemAffixType.STRENGTH);
        assertGreaterAffixWithoutRollRange(form, ImportedItemAffixType.ALL_RESISTANCE);
        assertGreaterAffixWithoutRollRange(form, ImportedItemAffixType.FIRE_RESISTANCE);
        ImportedItemAffix damageReduction = affix(form, ImportedItemAffixType.DAMAGE_REDUCTION);
        assertFalse(damageReduction.isGreaterAffix());
        assertEquals(11.0d, damageReduction.getRollRangeMin());
        assertEquals(15.0d, damageReduction.getRollRangeMax());
    }

    @Test
    void shouldReadNestorskaEgidaShieldTextWithoutMappingItToKoscianychLusek() throws Exception {
        byte[] imageBytes = Files.readAllBytes(Path.of("src/test/resources/items/tarcza.png"));
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new FakeOcrTextReader(Map.of(
                        "original", """
                                NESTORSKA
                                EGIDA
                                WEWNĘTRZNEGO
                                SPOKOJU
                                Starożytna legendarna
                                tarcza
                                Moc przedmiotu: 800
                                1 131 pkt. pancerza
                                45% redukcji blokowanych obrażeń [45]%
                                20,0% szansy na blok [20,0]%
                                +100% obrażeń od broni w głównej ręce [100]%
                                +114 siły [107 - 121]
                                +494 cierni [473 - 506]
                                +7,0% szansy na szczęśliwy traf [7,0 - 8,0]%
                                * 13,2% redukcji czasu odnowienia
                                Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%.
                                Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek.
                                Puste gniazdo
                                """,
                        "text-crop", "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU\nStarożytna legendarna tarcza\nMoc przedmiotu: 800\n1 131 pkt. pancerza",
                        "bottom-effect-x4", "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%. Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek."
                )),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(
                new ItemImageImportRequest("tarcza.png", "image/png", imageBytes)
        );
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals("tarcza.png", result.getImageMetadata().getOriginalFilename());
        assertEquals("Nestorska Egida Wewnętrznego Spokoju", result.getFullItemRead().getDetails().getItemName());
        assertEquals("Tarcza", result.getFullItemRead().getDetails().getItemType());
        assertEquals(800L, result.getFullItemRead().getDetails().getItemPower());
        assertEquals(1131L, result.getFullItemRead().getDetails().getItemArmor());
        assertFalse(result.getFullItemRead().getDetails().getItemName().contains("Kościanych Łusek"));
        assertEquals("inner-calm", form.getSelectedAspectId());
        assertFalse("fortify_damage_increased".equals(form.getSelectedAspectId()));
    }

    @Test
    void shouldRenderVerathielWeaponRangeFromCapturedRealOcrFlow() throws Exception {
        Path imagePath = Path.of("src/test/resources/items/verathiel-miecz.png");
        Path snapshotPath = Path.of("src/test/resources/items/verathiel-windows-ocr-snapshot.txt");
        assertTrue(Files.exists(imagePath), "Fixture obrazu Odłamka Verathiela musi istnieć.");
        assertTrue(Files.exists(snapshotPath), "Snapshot rzeczywistego OCR Odłamka Verathiela musi istnieć.");
        byte[] imageBytes = Files.readAllBytes(imagePath);
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new CapturedOcrSnapshotReader(snapshotPath),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(
                new ItemImageImportRequest("verathiel-miecz.png", "image/png", imageBytes)
        );
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);
        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                result,
                List.of(),
                null,
                new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty()),
                "Import testowy",
                ""
        ));

        assertEquals(447, result.getImageMetadata().getWidth());
        assertEquals(736, result.getImageMetadata().getHeight());
        assertEquals(1830L, result.getFullItemRead().getDetails().getWeaponDps());
        assertEquals(1350L, result.getFullItemRead().getDetails().getWeaponDamageMin());
        assertEquals(1978L, result.getFullItemRead().getDetails().getWeaponDamageMax());
        assertEquals(1664L, result.getFullItemRead().getDetails().getAverageWeaponDamage());
        assertEquals(1.10d, result.getFullItemRead().getDetails().getAttacksPerSecond());

        assertTrue(html.contains("name=\"weaponDps\" value=\"1830\""));
        assertTrue(html.contains("name=\"weaponDamageMin\" value=\"1350\""));
        assertTrue(html.contains("name=\"weaponDamageMax\" value=\"1978\""));
        assertTrue(html.contains("name=\"averageWeaponDamage\" value=\"1664\""));
        assertTrue(html.contains("name=\"attacksPerSecond\" value=\"1.10\""));
        assertFalse(html.contains("name=\"averageWeaponDamage\" value=\"1830\""));
        assertFalse(html.contains("<td>Brak pewnego odczytu</td><td>HIGH</td><td>weaponDamageMin"));

        assertTrue(html.contains("526 - 632"));
        assertTrue(html.contains("Szczęśliwy traf: zasób podstawowy"));
        assertTrue(html.contains("title=\"Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +X podstawowego zasobu\""));
        assertTrue(html.contains("name=\"affixValue_3\" value=\"3\""));
        assertFalse(html.contains("<span class=\"summary-value\">+3</span>"));
        assertFalse(html.contains("15% / +3"));
        assertTrue(html.contains("3 - 4"));
        assertTrue(html.contains("aspect-effect-fieldset"));
        assertTrue(html.contains("Treść efektu"));
        assertFalse(html.contains("Aspekt / efekt legendarny"));
        assertFalse(html.contains("Unikatowy efekt / aspekt"));
        assertFalse(html.contains("Ta lista jest głównym modelem korekty itemu. Finalny zapis użyje tylko aktywnych wierszy widocznych w tej tabeli."));
        assertTrue(html.contains("name=\"affixType_0\""));
        assertTrue(html.contains("name=\"affixType_1\""));
        assertTrue(html.contains("name=\"affixType_2\""));
        assertTrue(html.contains("name=\"affixType_3\""));
        assertFalse(html.contains("name=\"affixType_4\""));
        assertEquals("verathiel_shard", form.getSelectedAspectId());
    }

    @Test
    void shouldReadFullShieldItemFromFixtureAndKeepFoundationMappingSeparate() throws Exception {
        byte[] imageBytes = Files.readAllBytes(Path.of("src/test/resources/items/tarcza.png"));
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new FakeOcrTextReader(Map.of(
                        "original", String.join("\n", SHIELD_OCR_LINES_WITH_SEASONAL_NOISE),
                        "text-crop", "MIAŻDŻĄCA TARCZA KOŚCIANYCH ŁUSEK\nStarożytna legendarna tarcza\nPancerz: 1 202 pkt.",
                        "text-crop-gray-x2-contrast", "* +225 siły\n* +490 do odporności na wszystkie żywioły\n* +787 do odporności na: Ogień",
                        "text-crop-gray-x3-threshold", "20,0% szansy na blok [20,0]%\n+100% obrażeń od broni w głównej ręce [100]%",
                        "text-crop-gray-x3-sharpen", "Moc przedmiotu: 900\n11,4% redukcji obrażeń [11,0 - 15,0]\nGdy masz umocnienie, zadajesz obrażenia zwiększone o 610[x] [45 - 65]%. 70 poziomu"
                )),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(
                new ItemImageImportRequest("tarcza.png", "image/png", imageBytes)
        );

        assertEquals("tarcza.png", result.getImageMetadata().getOriginalFilename());
        assertShieldFoundationMapping(result);
        assertNull(result.getWeaponDamageCandidate().getSuggestedValue());
        assertHeaderAndUnrelatedNumbersDoNotLeakToShieldFoundationCandidates(result);

        assertEquals("Miażdżąca Tarcza Kościanych Łusek", result.getFullItemRead().getDetails().getItemName());
        assertEquals("Starożytna legendarna tarcza", result.getFullItemRead().getItemTypeLine());
        assertEquals("Starożytna legendarna tarcza", result.getFullItemRead().getRarity());
        assertEquals("Moc przedmiotu: 900", result.getFullItemRead().getItemPower());
        assertEquals(1202L, result.getFullItemRead().getDetails().getItemArmor());
        List<String> fullReadLines = result.getFullItemRead().getLines().stream()
                .map(FullItemReadLine::getText)
                .toList();
        for (String expectedLine : STABLE_SHIELD_CONTRACT_LINES) {
            assertTrue(fullReadLines.stream().anyMatch(line -> line.contains(expectedLine)), "Brak stabilnej linii tarczy: " + expectedLine);
        }
        assertFalse(new ImportedItemAffixExtractor().extractEditableAffixes(result.getFullItemRead()).stream()
                .anyMatch(affix -> affix.getSourceText().contains("Rozjuszenie")),
                "Sezonowe Rozjuszenie nie może trafić do edytowalnych affixów itemu.");
        assertTrue(result.getFullItemRead().getLines().stream().anyMatch(line -> line.getType() == FullItemReadLineType.SOCKET));
        assertLineTypeContains(result, FullItemReadLineType.BASE_STAT, "1 202 pkt.");
        assertLineTypeContains(result, FullItemReadLineType.IMPLICIT, "20,0% szansy na blok");
        assertLineTypeContains(result, FullItemReadLineType.IMPLICIT, "+100% obrażeń od broni w głównej ręce");
        assertLineTypeContains(result, FullItemReadLineType.AFFIX, "+225 siły");
        assertLineTypeContains(result, FullItemReadLineType.AFFIX, "+490 do odporności na wszystkie żywioły");
        assertLineTypeContains(result, FullItemReadLineType.AFFIX, "+787 do odporności na: Ogień");
        assertLineTypeContains(result, FullItemReadLineType.AFFIX, "11,4% redukcji obrażeń");
        assertLineTypeContains(result, FullItemReadLineType.ASPECT, "61%[x]");

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);
        assertEquals(4, form.getAffixes().size());
        assertAffixGreaterFlag(form, ImportedItemAffixType.STRENGTH, true);
        assertAffixGreaterFlag(form, ImportedItemAffixType.ALL_RESISTANCE, true);
        assertAffixGreaterFlag(form, ImportedItemAffixType.FIRE_RESISTANCE, true);
        assertAffixGreaterFlag(form, ImportedItemAffixType.DAMAGE_REDUCTION, false);
    }

    @Test
    void shouldReadFullBootItemFromFixtureWithoutHallucinatingFoundationStats() throws Exception {
        byte[] imageBytes = Files.readAllBytes(Path.of("src/test/resources/items/buty.png"));
        ItemImageImportService service = new ItemImageImportService(
                new ItemImageOcrPreprocessor(),
                new FakeOcrTextReader(Map.of(
                        "original", "Marsz Pokutnika\nRzadki przedmiot\nButy\n800 mocy przedmiotu\n354 pkt. pancerza\n+12,5% szybkości ruchu\n+7,0% uniku\n2 gniazda",
                        "text-crop", "Buty\n+12,5% szybkości ruchu",
                        "text-crop-gray-x2-contrast", "+7,0% uniku",
                        "text-crop-gray-x3-threshold", "2 gniazda",
                        "text-crop-gray-x3-sharpen", "800 mocy przedmiotu"
                )),
                new ItemImageImportTextParser(),
                new ItemImageImportCandidateMerger()
        );

        ItemImageImportCandidateParseResult result = service.analyze(
                new ItemImageImportRequest("buty.png", "image/png", imageBytes)
        );

        assertEquals("BOOTS", result.getSlotCandidate().getSuggestedValue().name());
        assertNull(result.getWeaponDamageCandidate().getSuggestedValue());
        assertNull(result.getStrengthCandidate().getSuggestedValue());
        assertNull(result.getIntelligenceCandidate().getSuggestedValue());
        assertNull(result.getThornsCandidate().getSuggestedValue());
        assertNull(result.getBlockChanceCandidate().getSuggestedValue());
        assertNull(result.getRetributionChanceCandidate().getSuggestedValue());
        assertEquals("Marsz Pokutnika", result.getFullItemRead().getItemName());
        assertEquals("Buty", result.getFullItemRead().getItemTypeLine());
        assertEquals("800 mocy przedmiotu", result.getFullItemRead().getItemPower());
        assertTrue(result.getFullItemRead().getLines().stream().anyMatch(line -> line.getText().contains("+12,5% szybkości ruchu")));
        assertTrue(result.getFullItemRead().getLines().stream().anyMatch(line -> line.getType() == FullItemReadLineType.SOCKET));
        assertFalse(result.getFullItemRead().getLines().stream().anyMatch(line -> line.getText().contains("Rozjuszenie")));
        assertExactlyOnePerLine(result, List.of("+12,5% szybkości ruchu", "+7,0% uniku"));
        assertSocketPurity(result);
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);
        assertEquals(2, form.getAffixes().size());
        assertEquals(ImportedItemAffixType.MOVEMENT_SPEED, form.getAffixes().get(0).getType());
        assertEquals(12.5d, form.getAffixes().get(0).getValue());
        assertEquals(ImportedItemAffixType.DODGE_CHANCE, form.getAffixes().get(1).getType());
        assertEquals(7.0d, form.getAffixes().get(1).getValue());
    }

    private static void assertShieldFoundationMapping(ItemImageImportCandidateParseResult result) {
        assertEquals("OFF_HAND", result.getSlotCandidate().getSuggestedValue().name());
        assertEquals(225.0d, result.getStrengthCandidate().getSuggestedValue(),
                () -> result.getStrengthCandidate().getRawValue());
        assertNull(result.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, result.getBlockChanceCandidate().getSuggestedValue());
    }

    private static void assertHeaderAndUnrelatedNumbersDoNotLeakToShieldFoundationCandidates(
            ItemImageImportCandidateParseResult result
    ) {
        assertCandidateIsNotAnyOf(result.getStrengthCandidate(), 800.0d, 1131.0d, 45.0d, 100.0d);
        assertCandidateIsNotAnyOf(result.getThornsCandidate(), 800.0d, 1131.0d, 45.0d, 100.0d);
        assertCandidateIsNotAnyOf(result.getBlockChanceCandidate(), 800.0d, 1131.0d, 45.0d, 100.0d);
    }

    private static void assertCandidateIsNotAnyOf(ItemImportFieldCandidate<Double> candidate, double... forbiddenValues) {
        for (double forbiddenValue : forbiddenValues) {
            assertNotEquals(forbiddenValue, candidate.getSuggestedValue(),
                    () -> "Niepowiązana liczba wyciekła do foundation candidate z linii: " + candidate.getRawValue());
        }
    }

    private static void assertFullReadContains(ItemImageImportCandidateParseResult result, String expectedText) {
        assertTrue(result.getFullItemRead().getLines().stream()
                        .map(FullItemReadLine::getText)
                        .anyMatch(line -> line.contains(expectedText)),
                "Pełny odczyt itemu nie zawiera stabilnego tekstu: " + expectedText);
    }

    private static void assertFullReadDoesNotContain(ItemImageImportCandidateParseResult result, String forbiddenText) {
        assertFalse(result.getFullItemRead().getLines().stream()
                        .map(FullItemReadLine::getText)
                        .anyMatch(line -> line.contains(forbiddenText)),
                "Pełny odczyt itemu zawiera zakazany sklejony tekst: " + forbiddenText);
    }

    private static void assertAffixValueAndGreaterFlag(ItemImportEditableForm form,
                                                       ImportedItemAffixType expectedType,
                                                       double expectedValue,
                                                       boolean expectedGreaterAffix) {
        ImportedItemAffix affix = form.getAffixes().stream()
                .filter(candidate -> candidate.getType() == expectedType)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Brak affixu: " + expectedType.getDisplayName()));
        assertEquals(expectedValue, affix.getValue(), 0.0001d, expectedType.getDisplayName());
        assertEquals(expectedGreaterAffix, affix.isGreaterAffix(), expectedType.getDisplayName());
    }

    private static long countFullReadLines(ItemImageImportCandidateParseResult result,
                                           FullItemReadLineType type,
                                           String expectedText) {
        return result.getFullItemRead().getLines().stream()
                .filter(line -> line.getType() == type)
                .map(FullItemReadLine::getText)
                .filter(expectedText::equals)
                .count();
    }

    private static int countOccurrences(String text, String expectedText) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(expectedText, index)) >= 0) {
            count++;
            index += expectedText.length();
        }
        return count;
    }

    private static String itemReadHeaderByHeading(String html, String heading) {
        int headingIndex = html.indexOf("<h3>" + heading + "</h3>");
        if (headingIndex < 0) {
            throw new AssertionError("Brak sekcji pełnego odczytu: " + heading);
        }
        int start = html.indexOf("<div class=\"item-read-header\">", headingIndex);
        int end = html.indexOf("<div class=\"item-read-groups\">", headingIndex);
        if (start < 0 || end < 0 || end <= start) {
            throw new AssertionError("Nie udało się wyciąć nagłówka pełnego odczytu: " + heading);
        }
        return html.substring(start, end);
    }

    private static String fieldSetByLegend(String html, String legend) {
        int legendIndex = html.indexOf("<legend>" + legend + "</legend>");
        if (legendIndex < 0) {
            throw new AssertionError("Brak fieldsetu: " + legend);
        }
        int start = html.lastIndexOf("<fieldset", legendIndex);
        int end = html.indexOf("</fieldset>", legendIndex);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć fieldsetu: " + legend);
        }
        return html.substring(start, end + "</fieldset>".length());
    }

    private static int countCheckedGreaterAffixes(String html) {
        int checked = 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("name=\\\"affixGreater_[0-9]+\\\" value=\\\"true\\\" checked")
                .matcher(html);
        while (matcher.find()) {
            checked++;
        }
        return checked;
    }

    private static String affixRow(String html, String selectedAffixLabel) {
        String marker = " selected>" + selectedAffixLabel + "</option>";
        int markerIndex = html.indexOf(marker);
        if (markerIndex >= 0) {
            int rowStart = html.lastIndexOf("<tr", markerIndex);
            int rowEnd = html.indexOf("</tr>", markerIndex);
            if (rowStart >= 0 && rowEnd >= rowStart) {
                return html.substring(rowStart, rowEnd + "</tr>".length());
            }
        }
        throw new AssertionError("Brak wiersza affixu w finalnym HTML: " + selectedAffixLabel);
    }

    private static void assertExactlyOnePerLine(ItemImageImportCandidateParseResult result, List<String> expectedTexts) {
        for (String expectedText : expectedTexts) {
            List<String> matchingLines = result.getFullItemRead().getLines().stream()
                    .map(FullItemReadLine::getText)
                    .filter(line -> line.contains(expectedText))
                    .toList();
            assertEquals(1, matchingLines.size(),
                    "Stabilna linia pełnego odczytu występuje niepoprawną liczbę razy: "
                            + expectedText
                            + " -> "
                            + matchingLines);
        }
    }

    private static void assertAffixTypeOccursOnce(ItemImportEditableForm form, ImportedItemAffixType expectedType) {
        long count = form.getAffixes().stream()
                .filter(affix -> affix.getType() == expectedType)
                .count();
        assertEquals(1L, count, "Edytowalny affix występuje niepoprawną liczbę razy: " + expectedType.getDisplayName());
    }

    private static void assertAffixGreaterFlag(ItemImportEditableForm form, ImportedItemAffixType expectedType, boolean expectedGreaterAffix) {
        ImportedItemAffix affix = affix(form, expectedType);
        assertEquals(expectedGreaterAffix, affix.isGreaterAffix(), expectedType.getDisplayName());
    }

    private static void assertGreaterAffixWithoutRollRange(ItemImportEditableForm form, ImportedItemAffixType expectedType) {
        ImportedItemAffix affix = affix(form, expectedType);
        assertTrue(affix.isGreaterAffix(), expectedType.getDisplayName());
        assertNull(affix.getRollRangeMin(), expectedType.getDisplayName());
        assertNull(affix.getRollRangeMax(), expectedType.getDisplayName());
    }

    private static ImportedItemAffix affix(ItemImportEditableForm form, ImportedItemAffixType expectedType) {
        return form.getAffixes().stream()
                .filter(candidate -> candidate.getType() == expectedType)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Brak affixu: " + expectedType.getDisplayName()));
    }

    private static void assertLineTypeContains(
            ItemImageImportCandidateParseResult result,
            FullItemReadLineType lineType,
            String expectedText
    ) {
        assertTrue(result.getFullItemRead().getLines().stream()
                        .filter(line -> line.getType() == lineType)
                        .map(FullItemReadLine::getText)
                        .anyMatch(line -> line.contains(expectedText)),
                "Linie typu " + lineType + " nie zawierają tekstu: " + expectedText);
    }

    private static void assertFullAspectIntegrity(ItemImageImportCandidateParseResult result) {
        assertLineTypeContains(result, FullItemReadLineType.ASPECT, "11,0%[x]");
        assertLineTypeContains(result, FullItemReadLineType.ASPECT, "Ta premia jest trzy razy większa");
        long aspectLineCount = result.getFullItemRead().getLines().stream()
                .filter(line -> line.getType() == FullItemReadLineType.ASPECT)
                .map(FullItemReadLine::getText)
                .filter(line -> line.contains("Zadajesz obrażenia zwiększone")
                        || line.contains("Ta premia jest trzy razy większa"))
                .count();
        assertEquals(2L, aspectLineCount, "Aspekt tarczy musi składać się z dwóch stabilnych linii efektu.");
    }

    private static void assertSocketPurity(ItemImageImportCandidateParseResult result) {
        assertFalse(result.getFullItemRead().getLines().stream()
                        .filter(line -> line.getType() == FullItemReadLineType.SOCKET)
                        .map(FullItemReadLine::getText)
                        .anyMatch(line -> line.contains("Zadajesz obrażenia zwiększone")
                                || line.contains("Ta premia jest trzy razy większa")),
                "Socket / gniazdo nie może zawierać fragmentów aspektu legendarnego.");
    }

    @Test
    void shouldRecognizeFoundationFieldsFromSingleItemScreenshot() throws Exception {
        Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"));
        byte[] imageBytes = buildSyntheticItemScreenshot();

        ItemImageImportCandidateParseResult result = new ItemImageImportService().analyze(
                new ItemImageImportRequest("synthetic-item.png", "image/png", imageBytes)
        );

        assertEquals("synthetic-item.png", result.getImageMetadata().getOriginalFilename());
        assertNotNull(result.getSlotCandidate().getSuggestedValue());
        assertEquals("MAIN_HAND", result.getSlotCandidate().getSuggestedValue().name());
        assertEquals(321L, result.getWeaponDamageCandidate().getSuggestedValue());
        assertEquals(55.0d, result.getStrengthCandidate().getSuggestedValue());
        assertEquals(13.0d, result.getIntelligenceCandidate().getSuggestedValue());
        assertEquals(90.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(18.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertNotNull(result.getRetributionChanceCandidate().getSuggestedValue());
    }

    private static byte[] buildSyntheticItemScreenshot() throws Exception {
        BufferedImage image = new BufferedImage(1400, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(12, 14, 18));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 34));
        graphics.setColor(new Color(240, 232, 214));

        String[] lines = {
                "ITEM TYPE MAIN HAND",
                "WEAPON DAMAGE 321",
                "STRENGTH 55",
                "INTELLIGENCE 13",
                "THORNS 90",
                "BLOCK CHANCE 18",
                "RETRIBUTION CHANCE 25"
        };

        int y = 120;
        for (String line : lines) {
            graphics.drawString(line, 100, y);
            y += 95;
        }
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private static byte[] buildShieldLikeScreenshot() throws Exception {
        BufferedImage image = new BufferedImage(1600, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(12, 14, 18));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(132, 100, 32));
        graphics.drawRect(26, 24, image.getWidth() - 52, image.getHeight() - 48);

        graphics.setColor(new Color(40, 58, 150));
        graphics.fillRoundRect(120, 170, 390, 500, 40, 40);
        graphics.setColor(new Color(190, 72, 38));
        graphics.fillOval(210, 250, 210, 240);

        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 42));
        graphics.setColor(new Color(238, 229, 209));
        int y = 190;
        for (String line : List.of("TARCZA", "+114 DO SILY", "+494 DO CIERNI", "20,0% SZANSY NA BLOK")) {
            graphics.drawString(line, 840, y);
            y += 120;
        }

        graphics.setColor(new Color(245, 245, 245));
        graphics.fillRect(0, 1050, image.getWidth(), 110);
        graphics.setColor(new Color(24, 24, 24));
        graphics.drawString("PRZEWIN W DOL", 1080, 1125);
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private static List<ItemImageImportRequest> repeatedRequests(byte[] imageBytes, int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> new ItemImageImportRequest("screen-" + index + ".png", "image/png", imageBytes))
                .toList();
    }

    private static final class FakeOcrTextReader implements ItemImageOcrTextReader {
        private final Map<String, String> variantTexts;

        private FakeOcrTextReader(Map<String, String> variantTexts) {
            this.variantTexts = variantTexts;
        }

        @Override
        public List<ItemImageOcrTextVariant> readTextVariants(List<ItemImageOcrVariant> variants) {
            return variants.stream()
                    .map(variant -> new ItemImageOcrTextVariant(
                            variant.getVariantId(),
                            variantTexts.getOrDefault(variant.getVariantId(), "")
                    ))
                    .toList();
        }
    }

    private static final class QueuedOcrTextReader implements ItemImageOcrTextReader {
        private final List<String> texts;
        private int callIndex;

        private QueuedOcrTextReader(List<String> texts) {
            this.texts = List.copyOf(texts);
        }

        @Override
        public List<ItemImageOcrTextVariant> readTextVariants(List<ItemImageOcrVariant> variants) {
            String text = callIndex < texts.size() ? texts.get(callIndex) : "";
            callIndex++;
            return variants.stream()
                    .map(variant -> new ItemImageOcrTextVariant(variant.getVariantId(), text))
                    .toList();
        }
    }

    private static final class QueuedVariantOcrTextReader implements ItemImageOcrTextReader {
        private final List<Map<String, String>> textsByCall;
        private int callIndex;

        private QueuedVariantOcrTextReader(List<Map<String, String>> textsByCall) {
            this.textsByCall = List.copyOf(textsByCall);
        }

        @Override
        public List<ItemImageOcrTextVariant> readTextVariants(List<ItemImageOcrVariant> variants) {
            Map<String, String> current = callIndex < textsByCall.size() ? textsByCall.get(callIndex) : Map.of();
            callIndex++;
            return variants.stream()
                    .map(variant -> new ItemImageOcrTextVariant(
                            variant.getVariantId(),
                            current.getOrDefault(variant.getVariantId(), "")
                    ))
                    .toList();
        }
    }

    /** Czytnik zapisanego snapshotu OCR wariantów preprocessingowych. Konkretne testy określają, czy snapshot jest real-image, czy snapshot-only. */
    private static final class CapturedOcrSnapshotReader implements ItemImageOcrTextReader {
        private final Map<String, String> variantTexts;

        private CapturedOcrSnapshotReader(Path snapshotPath) throws Exception {
            this.variantTexts = parseSnapshot(snapshotPath);
        }

        @Override
        public List<ItemImageOcrTextVariant> readTextVariants(List<ItemImageOcrVariant> variants) {
            return variants.stream()
                    .map(variant -> new ItemImageOcrTextVariant(
                            variant.getVariantId(),
                            variantTexts.getOrDefault(variant.getVariantId(), "")
                    ))
                    .toList();
        }

        private static Map<String, String> parseSnapshot(Path snapshotPath) throws Exception {
            Map<String, StringBuilder> builders = new LinkedHashMap<>();
            String currentVariantId = null;
            for (String line : Files.readAllLines(snapshotPath)) {
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("---") && line.endsWith("---")) {
                    currentVariantId = line.substring(3, line.length() - 3);
                    builders.putIfAbsent(currentVariantId, new StringBuilder());
                    continue;
                }
                if (currentVariantId == null) {
                    continue;
                }
                StringBuilder builder = builders.get(currentVariantId);
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(line);
            }
            Map<String, String> snapshot = new LinkedHashMap<>();
            for (Map.Entry<String, StringBuilder> entry : builders.entrySet()) {
                snapshot.put(entry.getKey(), entry.getValue().toString());
            }
            return snapshot;
        }
    }
}
