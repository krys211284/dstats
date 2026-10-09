package krys.web;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportValueMode;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.socketing.ItemSocketing;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy SSR importu obrazu po zamknięciu pipeline'u na Etapie 3A. */
class ItemImportPageRendererTest {
    private final ItemImportPageRenderer renderer = new ItemImportPageRenderer();

    @Test
    void shouldRenderUploadPanelWithoutLegacyOcrSections() {
        String html = renderer.render(new ItemImportPageModel(
                List.of(),
                null,
                "",
                activeHero(),
                "Dodaj od 1 do 5 screenshotów tego samego itemu. Jeśli tooltip jest przewijany, dodaj screeny w kolejności od góry do dołu.",
                ""
        ));

        assertTrue(html.contains("Importuj item ze screena"));
        assertTrue(html.contains("name=\"itemImage\""));
        assertTrue(html.contains("Odczytaj item"));
        assertTrue(html.contains("Jeśli tooltip jest przewijany"));
        assertFalse(html.contains("surowy POC OCR per ROI"));
        assertFalse(html.contains("pipeline diagnostyczny"));
        assertFalse(html.contains("Wczytaj diagnostykę obrazu"));
        assertFalse(html.contains("Wstępnie rozpoznane pola"));
        assertFalse(html.contains("Surowy tekst OCR"));
        assertFalse(html.contains("Zatwierdź import itemu"));
    }

    @Test
    void shouldRenderStageOneTwoAndThreeADiagnostics() {
        ItemImportPageModel.OpenCvImagePreview preview = new ItemImportPageModel.OpenCvImagePreview(
                "data:image/png;base64,AAA=",
                2,
                320,
                640,
                "PNG",
                "vertical-stitch",
                "STITCHED",
                List.of(new ItemImportPageModel.OpenCvStitchDiagnosticView(1, 2, 0, -14, 80, 240, "0.930", "MATCHED")),
                dividerDetection(),
                "",
                ""
        );

        String html = renderer.render(new ItemImportPageModel(
                List.of(),
                preview,
                "",
                activeHero(),
                "Pomoc importu.",
                "",
                null,
                List.of(),
                true
        ));

        assertTrue(html.contains("data-opencv-assembled-preview"));
        assertTrue(html.contains("data-opencv-stitch-diagnostics"));
        assertTrue(html.contains("data-opencv-divider-diagnostics"));
        assertTrue(html.contains("data-opencv-part-preview"));
        assertTrue(html.contains("data-item-structure-normalization"));
        assertTrue(html.contains("data-item-structure-status=\"STRUCTURALLY_NORMALIZED\""));
        assertTrue(html.contains("data-item-structure-slot=\"PART_1\""));
        assertTrue(html.contains("data-item-structure-slot=\"PART_2\""));
        assertTrue(html.contains("data-item-structure-slot=\"PART_3\""));
        assertTrue(html.contains("data-item-structure-slot=\"PART_4\""));
        assertTrue(html.contains("data-item-structure-present=\"false\""));
        assertTrue(html.contains("Status semantyczny"));
        assertTrue(html.contains("data-item-structure-semantic-status=\"SKIPPED_NOT_REQUIRED\""));
        assertTrue(html.contains("SKIPPED_NOT_REQUIRED — pominięty zgodnie z projektem"));
        assertTrue(html.contains(">absent<"));
        assertTrue(html.contains("data-property-block-segmentation"));
        assertTrue(html.contains("PART_2/PART_3 — Bloki właściwości"));
        assertTrue(html.contains("data-property-block-source=\"PART_3\" data-property-block-count=\"2\""));
        assertTrue(html.contains("data-property-block-anchor-type=\"NONE\""));
        assertTrue(html.contains("data-property-block-anchor-type=\"DIAMOND\""));
        assertTrue(html.contains("anchorType=NONE, x=10, y=12, width=140, height=30"));
        assertTrue(html.contains("anchor x=6, y=48, width=12, height=12, confidence=0.810"));
        assertTrue(html.contains("data-property-block-read-result"));
        assertTrue(html.contains("data-property-block-ocr"));
        assertTrue(html.contains("sourcePart=PART_3, blockOrder=1, anchorType=NONE"));
        assertTrue(html.contains("RAW BLOCK ONE"));
        assertTrue(html.contains("property-block-profile-2"));
        assertTrue(html.contains("diagnostyka OCR bloku"));
        assertTrue(html.contains("data-property-block-numeric-evidence"));
        assertTrue(html.contains("selectedValue=<span data-selected-value>2004</span>"));
        assertTrue(html.contains("supportCount=<span data-support-count>5</span>"));
        assertTrue(html.contains("supportingProfiles=<span data-supporting-profiles>[profile-1, profile-2, profile-3, profile-4, profile-5]</span>"));
        assertTrue(html.contains("alternativeValues=<span data-alternative-values>[20604]</span>"));
        assertTrue(html.contains("ambiguous=<span data-ambiguous-value>false</span>"));
        assertTrue(html.contains("data-numeric-ocr-ambiguous>NUMERIC_OCR_AMBIGUOUS"));
        assertTrue(html.contains("data-property-block-semantic-result"));
        assertTrue(html.contains("semanticKind=<span data-semantic-kind>BASE_STAT</span>"));
        assertTrue(html.contains("definitionId=<span data-definition-id>cooldown_reduction</span>"));
        assertTrue(html.contains("candidateDefinitionIds=<span data-candidate-definition-ids>[cooldown_reduction]</span>"));
        assertTrue(html.contains("greaterAffix=<span data-greater-affix>true</span>"));
        assertTrue(html.contains("numericValueCertain=<span data-numeric-value-certain>false</span>"));
        assertTrue(html.contains("data-property-block-value-read-result"));
        assertTrue(html.contains("data-value-role=\"itemArmor\""));
        assertTrue(html.contains("itemArmor=<span data-value-item-armor>2004</span>"));
        assertTrue(html.contains("data-value-parameter-role=\"itemArmor\""));
        assertTrue(html.contains("data-part1-segmentation"));
        assertTrue(html.contains("data-part1-source"));
        assertFalse(html.contains("data-part1-content"));
        assertFalse(html.contains("PART_1_CONTENT"));
        assertTrue(html.contains("data-part1-roi-type=\"NAME_AND_STARS\""));
        assertTrue(html.contains("data-part1-roi-x=\"4\""));
        assertTrue(html.contains("data-part1-roi-y=\"8\""));
        assertTrue(html.contains("data-part1-roi-width=\"220\""));
        assertTrue(html.contains("data-part1-roi-height=\"64\""));
        assertTrue(html.contains("data-part1-roi-ocr"));
        assertTrue(html.contains("data-part1-roi-subregion-type=\"NAME_TEXT\""));
        assertTrue(html.contains("NAME_TEXT: x=6, y=10, width=160, height=50"));
        assertTrue(html.contains("data-part1-roi-subregion-type=\"NAME_TEXT_CLEANED\""));
        assertTrue(html.contains("NAME_TEXT_CLEANED: x=6, y=10, width=160, height=50"));
        assertTrue(html.contains("data-part1-roi-subregion-type=\"NAME_TEXT_FINAL\""));
        assertTrue(html.contains("data-part1-greater-affix-count=\"2\""));
        assertTrue(html.contains("Greater Affix stars: 2"));
        assertTrue(html.contains("data-part1-roi-subregion-type=\"GREATER_AFFIX_STAR_1\""));
        assertTrue(html.contains("confidence=0.910"));
        assertTrue(html.contains("profile-name"));
        assertTrue(html.contains("frame/ornament cleanup"));
        assertTrue(html.contains("final-profile"));
        assertTrue(html.contains("BURZOWY KROK"));
        assertTrue(html.contains("data-part1-read-result"));
        assertTrue(html.contains("PART_1 — Wynik odczytu"));
        assertTrue(html.contains("Nazwa"));
        assertTrue(html.contains("BURZOWY KROK KSIĘŻYCOWEGO SZAŁU"));
        assertTrue(html.contains("Greater Affix stars"));
        assertTrue(html.contains("Klasyfikacja raw"));
        assertTrue(html.contains("Starożytna legendarna tarcza"));
        assertTrue(html.contains("Typ"));
        assertTrue(html.contains("Tarcza"));
        assertTrue(html.contains("Rzadkość"));
        assertTrue(html.contains("LEGENDARY"));
        assertTrue(html.contains("Ancient"));
        assertTrue(html.contains("true"));
        assertTrue(html.contains("Mythic Unique"));
        assertTrue(html.contains("false"));
        assertTrue(html.contains("Slot"));
        assertTrue(html.contains("OFF_HAND"));
        assertTrue(html.contains("Moc przedmiotu"));
        assertTrue(html.contains("900"));
        assertTrue(html.contains("Jakość wyświetlana"));
        assertTrue(html.contains("29"));
        assertTrue(html.contains("Masterworking quality"));
        assertTrue(html.contains("25"));
        assertFalse(html.contains("Odczyt OCR"));
        assertFalse(html.contains("Zatwierdź import itemu"));
    }

    @Test
    void shouldRenderIncompleteStructureWithoutGuessedMappings() {
        ItemImportPageModel.OpenCvImagePreview preview = new ItemImportPageModel.OpenCvImagePreview(
                "data:image/png;base64,AAA=",
                1,
                320,
                420,
                "PNG",
                "single-image",
                "SINGLE_IMAGE",
                List.of(),
                incompleteDividerDetection(),
                "",
                ""
        );

        String html = renderer.render(new ItemImportPageModel(
                List.of(),
                preview,
                "",
                activeHero(),
                "Pomoc importu.",
                "",
                null,
                List.of(),
                true
        ));

        assertTrue(html.contains("data-item-structure-status=\"INCOMPLETE\""));
        assertTrue(html.contains("data-item-structure-raw-part-count=\"2\""));
        assertTrue(html.contains("Brak normalizacji strukturalnej. Nie zgadujemy brakujących slotów."));
        assertFalse(html.contains("data-item-structure-row"));
        assertFalse(html.contains("data-item-structure-slot=\"PART_1\""));
        assertFalse(html.contains("data-item-structure-slot=\"PART_2\""));
    }

    @Test
    void shouldRenderStageErrorsWithoutSyntheticLegacyResult() {
        ItemImportPageModel.OpenCvImagePreview preview = new ItemImportPageModel.OpenCvImagePreview(
                "data:image/png;base64,AAA=",
                1,
                320,
                480,
                "PNG",
                "single-image",
                "SINGLE_IMAGE",
                List.of(),
                null,
                "Błąd detekcji separatorów: test",
                ""
        );

        String html = renderer.render(new ItemImportPageModel(
                List.of(),
                preview,
                "",
                activeHero(),
                "Pomoc importu.",
                "",
                null,
                List.of(),
                true
        ));

        assertTrue(html.contains("data-opencv-assembled-preview"));
        assertTrue(html.contains("Błąd detekcji separatorów: test"));
        assertFalse(html.contains("data-item-structure-normalization"));
        assertFalse(html.contains("Surowy tekst OCR"));
    }

    @Test
    void shouldRenderEditableVerificationFormOnlyWhenDraftIsAvailable() {
        ItemImportDetails details = new ItemImportDetails(
                "TARCZA BURZY KSIĘŻYCOWEGO SZAŁU",
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
                1502L,
                ""
        );
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "",
                "",
                "",
                "",
                "",
                new FullItemRead("TARCZA BURZY KSIĘŻYCOWEGO SZAŁU", "Tarcza", "LEGENDARY", "900", "1502 pancerza", List.of(), details),
                List.of(),
                "",
                details
        );

        String html = renderer.render(new ItemImportPageModel(
                List.of(),
                null,
                "",
                activeHero(),
                "Pomoc importu.",
                "heroId=1",
                form,
                List.of("NUMERIC_OCR_AMBIGUOUS: test")
        ));

        assertTrue(html.contains("data-image-import-verification"));
        assertTrue(html.contains("Weryfikacja itemu przed zapisem"));
        assertTrue(html.contains("action=\"/biblioteka-itemow\""));
        assertTrue(html.contains("name=\"action\" value=\"saveImportedItem\""));
        assertTrue(html.contains("name=\"currentBuildQuery\" value=\"heroId=1\""));
        assertTrue(html.contains("name=\"itemArmor\" value=\"1502\""));
        assertTrue(html.contains("Niektóre wartości mogły zostać odczytane niedokładnie."));
        assertFalse(html.contains("Wymaga sprawdzenia: NUMERIC_OCR_AMBIGUOUS: test"));
        assertFalse(html.contains("NUMERIC_OCR_AMBIGUOUS"));
        assertTrue(html.contains("Zapisz item"));
    }

    @Test
    void shouldRenderTechnicalReviewNotesInDiagnosticsMode() {
        ItemImportDetails details = new ItemImportDetails(
                "TARCZA BURZY KSIĘŻYCOWEGO SZAŁU",
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
                1502L,
                ""
        );
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "",
                "",
                "",
                "",
                "",
                new FullItemRead("TARCZA BURZY KSIĘŻYCOWEGO SZAŁU", "Tarcza", "LEGENDARY", "900", "1502 pancerza", List.of(), details),
                List.of(),
                "",
                details
        );

        String html = renderer.render(new ItemImportPageModel(
                List.of(),
                null,
                "",
                activeHero(),
                "Pomoc importu.",
                "heroId=1",
                form,
                List.of("NUMERIC_OCR_AMBIGUOUS: test"),
                true
        ));

        assertTrue(html.contains("data-image-import-verification"));
        assertTrue(html.contains("Wymaga sprawdzenia: NUMERIC_OCR_AMBIGUOUS: test"));
        assertTrue(html.contains("Zapisz item"));
    }

    @Test
    void shouldRenderShieldVerificationWithResolvedMasterworkingValuesWithoutDoubleApplyingQuality() {
        ItemImportDetails details = new ItemImportDetails(
                "TARCZA BURZY KSIĘŻYCOWEGO SZAŁU",
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
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "",
                "",
                "",
                "",
                "",
                new FullItemRead("TARCZA BURZY KSIĘŻYCOWEGO SZAŁU", "Tarcza", "LEGENDARY", "900",
                        "1502 pancerza", List.of(), details),
                List.of(
                        affix(ImportedItemAffixType.STRENGTH, 173.6d, "", false,
                                "+217 siły [150 - 180]", "strength", 150.0d, 180.0d, "217"),
                        affix(ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 8.8d, "%", false,
                                "+11,0% szansy na trafienie krytyczne", "critical_strike_chance", null, null, "11,0%"),
                        affix(ImportedItemAffixType.DAMAGE_REDUCTION, 14.08d, "%", false,
                                "17,6% redukcji obrażeń", "damage_reduction", null, null, "17,6%"),
                        affix(ImportedItemAffixType.COOLDOWN_REDUCTION, 10.25d, "%", true,
                                "12,3% redukcji czasu odnowienia [10 - 12]%", "cooldown_reduction", 10.0d, 12.0d, "12,3%")
                ),
                "",
                details,
                List.of(new ItemTemperingAffix(
                        "defense_max_animus",
                        TemperingCategory.DEFENSE,
                        5.0d,
                        "+12 do maksymalnej liczby kumulacji Animuszu",
                        "+12 do maksymalnej liczby kumulacji Animuszu",
                        TemperingRuntimeStatus.DATA_ONLY,
                        true
                )),
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus")),
                krys.transfiguration.ItemTransfiguration.none(),
                ItemSocketing.empty(),
                ItemImportValueMode.GAME_DISPLAYED_VALUE
        );

        String html = renderer.render(new ItemImportPageModel(
                List.of(),
                null,
                "",
                activeHero(),
                "Pomoc importu.",
                "",
                form,
                List.of()
        ));

        assertTrue(html.contains("data-image-import-verification"));
        assertTrue(html.contains("name=\"itemImportValueMode\" value=\"GAME_DISPLAYED_VALUE\""));
        assertTrue(html.contains("name=\"itemArmor\" value=\"1502\""));
        assertTrue(html.contains("name=\"affixValue_0\" value=\"217\""));
        assertTrue(html.contains("name=\"affixValue_1\" value=\"11.0\""));
        assertTrue(html.contains("name=\"affixValue_2\" value=\"17.6\""));
        assertTrue(html.contains("name=\"affixValue_3\" value=\"12.3\""));
        assertTrue(html.contains("name=\"temperingValue_0\" value=\"12\""));
        assertTrue(html.contains("Wartość z gry"));
        assertFalse(html.contains("Wartość edytowalna"));
        assertTrue(html.contains("id=\"affixLimitMessage\""));
        assertTrue(html.contains("id=\"affixAddControls\" hidden"));
        assertTrue(html.contains("id=\"addAffixButton\""));
        assertTrue(html.contains("name=\"newAffixType\""));
        assertTrue(html.contains("name=\"newAffixValue\""));
        assertTrue(html.contains("id=\"newAffixGreater\""));
        assertTrue(html.contains("id=\"affixRowTemplate\""));
        assertTrue(html.contains("id=\"temperingLimitMessage\""));
        assertTrue(html.contains("id=\"temperingAddControls\" hidden"));
        assertTrue(html.contains("id=\"addTemperingButton\" disabled"));
        assertTrue(html.contains("name=\"newTemperingCategory\""));
        assertTrue(html.contains("name=\"newTemperingDefinitionId\""));
        assertTrue(html.contains("name=\"newTemperingValue\""));
        assertTrue(html.contains("id=\"temperingRowTemplate\""));
        assertTrue(html.contains("data-value-mode=\"GAME_DISPLAYED_VALUE\""));
        assertTrue(html.contains("+12"));
        assertTrue(html.contains("Animuszu"));
        assertFalse(html.contains(">1877<"));
        assertFalse(html.contains(">271<"));
        assertFalse(html.contains("13,8%"));
        assertFalse(html.contains("22,0%"));
        assertFalse(html.contains("14,8%"));
    }

    @Test
    void shouldRenderTransfigurationAffixRefsAsDisplayedValuesInImageVerificationMode() {
        ItemImportDetails details = new ItemImportDetails(
                "TARCZA BURZY KSIĘŻYCOWEGO SZAŁU",
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
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "",
                "",
                "",
                "",
                "",
                new FullItemRead("TARCZA BURZY KSIĘŻYCOWEGO SZAŁU", "Tarcza", "LEGENDARY", "900",
                        "1502 pancerza", List.of(), details),
                List.of(
                        affix(ImportedItemAffixType.STRENGTH, 173.6d, "", false,
                                "+217 siły [150 - 180]", "strength", 150.0d, 180.0d, "217"),
                        affix(ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 8.8d, "%", false,
                                "+11,0% szansy na trafienie krytyczne", "critical_strike_chance", null, null, "11,0%"),
                        affix(ImportedItemAffixType.DAMAGE_REDUCTION, 14.08d, "%", false,
                                "17,6% redukcji obrażeń", "damage_reduction", null, null, "17,6%")
                ),
                "",
                details,
                List.of(),
                new ItemMasterworking(25, 25),
                new ItemTransfiguration(
                        true,
                        true,
                        HoradricTuningPrism.NONE,
                        HoradricTransfigurationOutcome.UPGRADE_TO_GREATER_AFFIX,
                        "STRENGTH",
                        null,
                        "",
                        null,
                        null,
                        false,
                        ""
                ),
                ItemSocketing.empty(),
                ItemImportValueMode.GAME_DISPLAYED_VALUE
        );

        String html = renderer.render(new ItemImportPageModel(
                List.of(),
                null,
                "",
                activeHero(),
                "Pomoc importu.",
                "",
                form,
                List.of()
        ));

        assertTrue(html.contains("<option value=\"STRENGTH\" selected>Siła 217</option>"));
        assertTrue(html.contains("<option value=\"STRENGTH\">Siła 217</option>"));
        assertTrue(html.contains("<option value=\"CRITICAL_STRIKE_CHANCE\">Szansa na trafienie krytyczne 11</option>"));
        assertTrue(html.contains("<option value=\"DAMAGE_REDUCTION\">Redukcja obrażeń 17.6</option>"));
        assertFalse(html.contains("<option value=\"STRENGTH\" selected>Siła 173.6</option>"));
        assertFalse(html.contains("<option value=\"STRENGTH\">Siła 173.6</option>"));
        assertFalse(html.contains("<option value=\"CRITICAL_STRIKE_CHANCE\">Szansa na trafienie krytyczne 8.8</option>"));
        assertFalse(html.contains("<option value=\"DAMAGE_REDUCTION\">Redukcja obrażeń 14.08</option>"));
    }

    private static ItemImportPageModel.HorizontalDividerDetectionView dividerDetection() {
        return new ItemImportPageModel.HorizontalDividerDetectionView(
                2,
                3,
                "data:image/png;base64,BBB=",
                List.of(
                        new ItemImportPageModel.HorizontalDividerView(1, 160, 158, 162, "0.910"),
                        new ItemImportPageModel.HorizontalDividerView(2, 420, 418, 422, "0.890")
                ),
                List.of(
                        new ItemImportPageModel.ItemImagePartView(1, "PART_1_RAW", 0, 160, 320, 160, "data:image/png;base64,CCC="),
                        new ItemImportPageModel.ItemImagePartView(2, "PART_2_RAW", 160, 420, 320, 260, "data:image/png;base64,DDD="),
                        new ItemImportPageModel.ItemImagePartView(3, "PART_3_RAW", 420, 640, 320, 220, "data:image/png;base64,EEE=")
                ),
                new ItemImportPageModel.ItemPartStructureNormalizationView(
                        "STRUCTURALLY_NORMALIZED",
                        3,
                        List.of(
                                new ItemImportPageModel.StructuralItemPartMappingView("PART_1", true, 1),
                                new ItemImportPageModel.StructuralItemPartMappingView("PART_2", false, 0),
                                new ItemImportPageModel.StructuralItemPartMappingView("PART_3", true, 2),
                                new ItemImportPageModel.StructuralItemPartMappingView(
                                        "PART_4", true, 3, "SKIPPED_NOT_REQUIRED")
                        ),
                        "",
                        part1Segmentation(),
                        "",
                        List.of(propertyBlockSegmentation()),
                        ""
                ),
                ""
        );
    }

    private static ItemImportPageModel.PropertyBlockSegmentationView propertyBlockSegmentation() {
        return new ItemImportPageModel.PropertyBlockSegmentationView(
                "PART_3",
                2,
                "PART_3_RAW",
                320,
                220,
                "lines=3, anchors=1, blocks=2",
                List.of(
                        new ItemImportPageModel.PropertyBlockView(
                                1,
                                "NONE",
                                10,
                                12,
                                140,
                                30,
                                "data:image/png;base64,BLK1=",
                                "lineCount=1",
                                "",
                                new ItemImportPageModel.PropertyBlockReadResultView(
                                        "PART_3",
                                        1,
                                        "NONE",
                                        "RAW BLOCK ONE",
                                        12L,
                                        "property-block-profile-1",
                                        List.of(),
                                        List.of(new ItemImportPageModel.PropertyBlockNumericEvidenceView(
                                                1,
                                                "2004",
                                                List.of("profile-1", "profile-2", "profile-3", "profile-4", "profile-5"),
                                                List.of("20604"),
                                                5,
                                                false
                                        ))
                                ),
                                new ItemImportPageModel.PropertyBlockSemanticResultView(
                                        "BASE_STAT", "", List.of(), false, true),
                                new ItemImportPageModel.PropertyBlockValueReadResultView(
                                        "itemArmor",
                                        "2004",
                                        "",
                                        "",
                                        "",
                                        "2004",
                                        "",
                                        "",
                                        "",
                                        true,
                                        false,
                                        List.of(new ItemImportPageModel.PropertyBlockValueParameterView(
                                                1,
                                                "itemArmor",
                                                "2004",
                                                "2004",
                                                "",
                                                "",
                                                false,
                                                List.of("20604")
                                        )),
                                        List.of())
                        ),
                        new ItemImportPageModel.PropertyBlockView(
                                2,
                                "DIAMOND",
                                4,
                                44,
                                190,
                                42,
                                "data:image/png;base64,BLK2=",
                                "lineCount=2",
                                "x=6, y=48, width=12, height=12, confidence=0.810",
                                new ItemImportPageModel.PropertyBlockReadResultView(
                                        "PART_3",
                                        2,
                                        "DIAMOND",
                                        "RAW BLOCK TWO",
                                        13L,
                                        "property-block-profile-2",
                                        List.of("diagnostyka OCR bloku", "NUMERIC_OCR_AMBIGUOUS tokenOrder=1"),
                                        List.of(new ItemImportPageModel.PropertyBlockNumericEvidenceView(
                                                1,
                                                "",
                                                List.of("profile-1", "profile-2"),
                                                List.of("12,3%", "12,5%"),
                                                2,
                                                true
                                        ))
                                ),
                                new ItemImportPageModel.PropertyBlockSemanticResultView(
                                        "AFFIX",
                                        "cooldown_reduction",
                                        List.of("cooldown_reduction"),
                                        true,
                                        false)
                        )
                )
        );
    }

    private static ItemImportPageModel.Part1SegmentationView part1Segmentation() {
        return new ItemImportPageModel.Part1SegmentationView(
                "data:image/png;base64,PPP=",
                1,
                "PART_1_RAW",
                320,
                160,
                List.of(
                        new ItemImportPageModel.Part1RoiView("NAME_AND_STARS", 4, 8, 220, 64, "data:image/png;base64,NAM=",
                                "BURZOWY KROK", 11L, "stub-preprocess", "", List.of(
                                new ItemImportPageModel.Part1RoiSubRegionView("NAME_TEXT", 6, 10, 160, 50,
                                        "data:image/png;base64,SUB=", "profile-name"),
                                new ItemImportPageModel.Part1RoiSubRegionView("NAME_TEXT_CLEANED", 6, 10, 160, 50,
                                        "data:image/png;base64,CLN=", "frame/ornament cleanup"),
                                new ItemImportPageModel.Part1RoiSubRegionView("NAME_TEXT_FINAL", 0, 0, 676, 236,
                                        "data:image/png;base64,FIN=", "final-profile"),
                                new ItemImportPageModel.Part1RoiSubRegionView("GREATER_AFFIX_STAR_1", 170, 42, 22, 22,
                                        "data:image/png;base64,STA=", "visual star candidate confidence=0.910")
                        ), 2),
                        new ItemImportPageModel.Part1RoiView("CLASSIFICATION", 8, 78, 210, 42, "data:image/png;base64,CLS="),
                        new ItemImportPageModel.Part1RoiView("ITEM_POWER", 8, 124, 190, 26, "data:image/png;base64,POW="),
                        new ItemImportPageModel.Part1RoiView("OPTIONAL_INFO", 8, 150, 150, 20, "data:image/png;base64,OPT=")
                ),
                new ItemImportPageModel.Part1ReadResultView(
                        "BURZOWY KROK KSIĘŻYCOWEGO SZAŁU",
                        2,
                        "Starożytna legendarna tarcza",
                        "Tarcza",
                        "LEGENDARY",
                        "true",
                        "false",
                        "OFF_HAND",
                        "900",
                        "29",
                        "25",
                        List.of()
                )
        );
    }

    private static ItemImportPageModel.HorizontalDividerDetectionView incompleteDividerDetection() {
        return new ItemImportPageModel.HorizontalDividerDetectionView(
                1,
                2,
                "data:image/png;base64,BBB=",
                List.of(new ItemImportPageModel.HorizontalDividerView(1, 210, 208, 212, "0.910")),
                List.of(
                        new ItemImportPageModel.ItemImagePartView(1, "PART_1_RAW", 0, 210, 320, 210, "data:image/png;base64,CCC="),
                        new ItemImportPageModel.ItemImagePartView(2, "PART_2_RAW", 210, 420, 320, 210, "data:image/png;base64,DDD=")
                ),
                new ItemImportPageModel.ItemPartStructureNormalizationView(
                        "INCOMPLETE",
                        2,
                        List.of(),
                        "2 raw parts nie wystarczają do jednoznacznej normalizacji struktury."
                ),
                ""
        );
    }

    private static ImportedItemAffix affix(ImportedItemAffixType type,
                                           double sourceValue,
                                           String unit,
                                           boolean greaterAffix,
                                           String rawLine,
                                           String definitionId,
                                           Double rollRangeMin,
                                           Double rollRangeMax,
                                           String displayValue) {
        return new ImportedItemAffix(
                type,
                sourceValue,
                unit,
                greaterAffix,
                0,
                rawLine,
                ImportedItemAffixSource.OCR,
                definitionId,
                rollRangeMin,
                rollRangeMax,
                null,
                displayValue
        );
    }

    private static HeroProfile activeHero() {
        return new HeroProfile(1L, "Importer", HeroClass.PALADIN,
                CurrentBuildFormQuerySupport.toQuery(CurrentBuildFormData.defaultValues()),
                HeroItemSelection.empty(),
                HeroSkillLoadout.fromCurrentBuildFormData(CurrentBuildFormData.defaultValues()));
    }
}
