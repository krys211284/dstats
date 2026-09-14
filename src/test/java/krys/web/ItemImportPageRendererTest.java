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
        assertTrue(html.contains("Pipeline kończy się na StructuredItemParts"));
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
                        ""
                ),
                ""
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
