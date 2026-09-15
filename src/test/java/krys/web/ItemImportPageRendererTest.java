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
        assertTrue(html.contains(">absent<"));
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
        assertFalse(html.contains("Odczyt OCR"));
        assertFalse(html.contains("parsed result"));
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
                                new ItemImportPageModel.StructuralItemPartMappingView("PART_4", true, 3)
                        ),
                        "",
                        part1Segmentation(),
                        ""
                ),
                ""
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
