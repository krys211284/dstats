package krys.web;

import krys.hero.HeroClass;
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
                "Pomoc importu.",
                ""
        ));

        assertTrue(html.contains("Wgraj screenshot itemu"));
        assertTrue(html.contains("name=\"itemImage\""));
        assertTrue(html.contains("surowy POC OCR per ROI"));
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
                ""
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
                ""
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
                ""
        ));

        assertTrue(html.contains("data-opencv-assembled-preview"));
        assertTrue(html.contains("Błąd detekcji separatorów: test"));
        assertFalse(html.contains("data-item-structure-normalization"));
        assertFalse(html.contains("Surowy tekst OCR"));
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
                                        "BASE_STAT", "", List.of(), false, true)
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

    private static HeroProfile activeHero() {
        return new HeroProfile(1L, "Importer", HeroClass.PALADIN,
                CurrentBuildFormQuerySupport.toQuery(CurrentBuildFormData.defaultValues()),
                HeroItemSelection.empty(),
                HeroSkillLoadout.fromCurrentBuildFormData(CurrentBuildFormData.defaultValues()));
    }
}
