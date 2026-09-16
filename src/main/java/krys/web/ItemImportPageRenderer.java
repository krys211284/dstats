package krys.web;

import java.util.List;

/** Renderuje SSR diagnostycznego pipeline'u obrazu itemu do Etapu 3A. */
public final class ItemImportPageRenderer {
    public String render(ItemImportPageModel model) {
        return """
                <!DOCTYPE html>
                <html lang="pl">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Build WebApp - Importuj item ze screena</title>
                    <style>
                        %s
                        .layout { max-width: 1500px; margin: 0 auto; padding: 28px 16px 48px; }
                        .hero { margin-bottom: 18px; }
                        .eyebrow { display: inline-block; margin-bottom: 10px; padding: 6px 10px; border-radius: 999px; background: var(--accent-soft); color: var(--accent); font-size: 0.85rem; font-weight: 700; letter-spacing: 0.04em; text-transform: uppercase; }
                        .panel { margin-bottom: 18px; padding: 18px; border: 1px solid var(--line); border-radius: 16px; background: rgba(255, 250, 242, 0.96); box-shadow: 0 12px 24px rgba(36, 33, 29, 0.05); }
                        .panel-error { border-color: rgba(141, 27, 27, 0.28); background: var(--error-bg); }
                        .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; align-items: end; }
                        label { display: grid; gap: 8px; font-weight: 600; }
                        input[type="file"] { width: 100%%; padding: 10px 12px; border: 1px solid var(--line); border-radius: 10px; background: #fff; color: var(--text); font: inherit; }
                        button { display: inline-block; border: none; border-radius: 999px; padding: 12px 18px; background: linear-gradient(135deg, #1a4b5a 0%%, #2d7288 100%%); color: #f5fbfd; font: inherit; font-weight: 700; cursor: pointer; }
                        .helper { margin-top: 10px; color: var(--muted); font-size: 0.95rem; line-height: 1.55; }
                        .submit-row { margin-top: 16px; }
                        .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; }
                        .summary-card { padding: 12px; border: 1px solid var(--line); border-radius: 12px; background: #fff; }
                        .summary-label { color: var(--muted); font-size: 0.88rem; margin-bottom: 6px; }
                        .summary-value { font-size: 1.05rem; font-weight: 700; overflow-wrap: anywhere; }
                        .subpanel { margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--line); }
                        .opencv-preview-layout, .opencv-divider-layout { display: grid; grid-template-columns: minmax(280px, 0.95fr) minmax(260px, 1.05fr); gap: 16px; align-items: start; }
                        .opencv-preview-frame { max-height: 760px; overflow: auto; padding: 12px; border: 1px solid rgba(109, 102, 92, 0.18); border-radius: 12px; background: #1d1c1a; }
                        .opencv-divider-overlay-frame { max-height: 520px; }
                        .opencv-preview-frame img, .opencv-part-frame img { display: block; max-width: 100%%; height: auto; margin: 0 auto; }
                        .opencv-preview-warning { color: var(--error); }
                        .opencv-part-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; margin-top: 14px; }
                        .opencv-part-card { padding: 12px; border: 1px solid rgba(109, 102, 92, 0.18); border-radius: 10px; background: #fff; }
                        .opencv-part-card h4 { margin: 0 0 6px; }
                        .opencv-part-frame { max-height: 360px; overflow: auto; padding: 10px; border: 1px solid rgba(109, 102, 92, 0.18); border-radius: 10px; background: #1d1c1a; }
                        .part1-raw-ocr { margin: 10px 0 0; padding: 10px; min-height: 44px; max-height: 180px; overflow: auto; border: 1px solid rgba(109, 102, 92, 0.18); border-radius: 8px; background: #f7f2ea; color: var(--text); white-space: pre-wrap; overflow-wrap: anywhere; font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace; font-size: 0.9rem; }
                        .part1-subroi-list { display: grid; gap: 10px; margin-top: 10px; }
                        .part1-subroi { padding-top: 10px; border-top: 1px solid rgba(109, 102, 92, 0.18); }
                        .part1-subroi img { display: block; max-width: 100%%; height: auto; margin-top: 6px; border: 1px solid rgba(109, 102, 92, 0.18); background: #1d1c1a; }
                        .part1-ocr-error { color: var(--error); }
                        .table-scroll { overflow-x: auto; }
                        table { width: 100%%; border-collapse: collapse; font-size: 0.94rem; }
                        th, td { padding: 10px 8px; border-bottom: 1px solid rgba(109, 102, 92, 0.18); text-align: left; vertical-align: top; }
                        thead th { color: var(--muted); font-weight: 700; }
                        .error-list { margin: 0; padding-left: 20px; color: var(--error); }
                        @media (max-width: 900px) {
                            .opencv-preview-layout, .opencv-divider-layout { grid-template-columns: 1fr; }
                        }
                    </style>
                </head>
                <body>
                <main class="layout">
                    %s
                    <header class="hero">
                        <div class="eyebrow">Import itemu / obraz</div>
                        <h1>Importuj pojedynczy item ze screena</h1>
                        <p class="helper">Aktualny pipeline diagnostyczny składa screenshoty, wykrywa separatory i normalizuje surowe części do stabilnych slotów strukturalnych. PART_1 ma dodatkowy surowy POC OCR per ROI; wynik nie jest jeszcze mapowany do domeny itemu.</p>
                    </header>
                    %s
                    %s
                    %s
                    %s
                </main>
                <script>
                (() => {
                    const screenshotInput = document.querySelector('[data-item-screenshot-input]');
                    const multiScreenshotInfo = document.querySelector('[data-multi-screenshot-info]');
                    const multiScreenshotCount = document.querySelector('[data-multi-screenshot-count]');
                    if (!screenshotInput || !multiScreenshotInfo || !multiScreenshotCount) return;
                    const refreshMultiScreenshotInfo = () => {
                        const count = screenshotInput.files ? screenshotInput.files.length : 0;
                        multiScreenshotCount.textContent = String(count);
                        multiScreenshotInfo.hidden = count <= 1;
                    };
                    screenshotInput.addEventListener('change', refreshMultiScreenshotInfo);
                    refreshMultiScreenshotInfo();
                })();
                </script>
                </body>
                </html>
                """.formatted(
                AppShellRendererSupport.renderSharedStyles(),
                AppShellRendererSupport.renderGlobalNavigation("/importuj-item-ze-screena"),
                renderHeroContext(model),
                renderUploadPanel(model),
                renderErrors(model.getValidationErrors()),
                renderOpenCvPreviewSection(model)
        );
    }

    private static String renderHeroContext(ItemImportPageModel model) {
        if (!model.hasActiveHero()) {
            return """
                    <section class="panel panel-error">
                        <h2>Brak aktywnego bohatera</h2>
                        <p class="helper">Ten moduł importu pracuje w kontekście aktywnego bohatera. Utwórz pierwszego bohatera albo ustaw istniejącego przed wgraniem screena itemu.</p>
                    </section>
                    """;
        }
        return """
                <section class="panel">
                    <h2>Aktywny bohater importu</h2>
                    <p class="helper">Importujesz teraz diagnostykę obrazu dla bohatera %s.</p>
                </section>
                """.formatted(escapeHtml(model.getActiveHero().getName()));
    }

    private static String renderUploadPanel(ItemImportPageModel model) {
        return """
                <section class="panel">
                    <h2>Wgraj screenshot itemu</h2>
                    <form method="post" action="%s" enctype="multipart/form-data">
                        <div class="form-grid">
                            <label>
                                Obraz pojedynczego itemu
                                <input type="file" name="itemImage" accept="image/png,image/jpeg,image/webp" multiple data-item-screenshot-input>
                            </label>
                        </div>
                        <p class="helper">%s</p>
                        <p class="helper" data-multi-screenshot-info hidden>Import wieloscreenowy: <span data-multi-screenshot-count>0</span> obrazów zostanie scalonych jako jeden item.</p>
                        <div class="submit-row">
                            <button type="submit">Wczytaj diagnostykę obrazu</button>
                        </div>
                    </form>
                </section>
                """.formatted(
                escapeHtml(buildUploadAction(model.getCurrentBuildQuery())),
                escapeHtml(model.getHelpText())
        );
    }

    private static String renderErrors(List<String> errors) {
        if (errors.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder("""
                <section class="panel panel-error">
                    <h2>Błędy formularza</h2>
                    <ul class="error-list">
                """);
        for (String error : errors) {
            html.append("<li>").append(escapeHtml(error)).append("</li>");
        }
        html.append("""
                    </ul>
                </section>
                """);
        return html.toString();
    }

    private static String renderOpenCvPreviewSection(ItemImportPageModel model) {
        if (!model.hasOpenCvImagePreview() && !model.hasOpenCvPreviewError()) {
            return "";
        }
        if (model.hasOpenCvPreviewError() && !model.hasOpenCvImagePreview()) {
            return """
                    <section class="panel panel-error opencv-preview-panel" data-opencv-preview-section>
                        <h2>Błąd podglądu OpenCV</h2>
                        <p class="helper">%s</p>
                    </section>
                    """.formatted(escapeHtml(model.getOpenCvPreviewError()));
        }

        ItemImportPageModel.OpenCvImagePreview preview = model.getOpenCvImagePreview();
        StringBuilder html = new StringBuilder("""
                <section class="panel opencv-preview-panel" data-opencv-preview-section>
                    <h2>Podgląd obrazu przygotowanego przez OpenCV</h2>
                    <div class="opencv-preview-layout">
                        <div class="opencv-preview-frame">
                            <img src="%s" alt="Złożony obraz itemu przygotowany przez OpenCV" data-opencv-assembled-preview>
                        </div>
                        <div class="summary-grid opencv-preview-metadata">
                """.formatted(escapeHtml(preview.getDataUri())));
        html.append(renderSummaryCard("Liczba obrazów źródłowych", Integer.toString(preview.getSourceImageCount())))
                .append(renderSummaryCard("Szerokość finalnego obrazu", Integer.toString(preview.getWidth())))
                .append(renderSummaryCard("Wysokość finalnego obrazu", Integer.toString(preview.getHeight())))
                .append(renderSummaryCard("Format", preview.getFormat()))
                .append(renderSummaryCard("Status składania", preview.getAssemblyStatus()))
                .append(renderSummaryCard("compositionPolicy", preview.getCompositionPolicy()))
                .append("""
                        </div>
                    </div>
                """);
        html.append(renderOpenCvStitchDiagnostics(preview));
        html.append(renderOpenCvHorizontalDividerDetection(preview));
        if (model.hasOpenCvPreviewError()) {
            html.append("<p class=\"helper opencv-preview-warning\">")
                    .append(escapeHtml(model.getOpenCvPreviewError()))
                    .append("</p>");
        }
        if (preview.hasDebugWarning()) {
            html.append("<p class=\"helper opencv-preview-warning\">")
                    .append(escapeHtml(preview.getDebugWarning()))
                    .append("</p>");
        }
        html.append("</section>");
        return html.toString();
    }

    private static String renderOpenCvStitchDiagnostics(ItemImportPageModel.OpenCvImagePreview preview) {
        if (!preview.hasStitchDiagnostics()) {
            return "";
        }
        StringBuilder html = new StringBuilder("""
                    <section class="subpanel opencv-stitch-diagnostics" data-opencv-stitch-diagnostics>
                        <h3>Diagnostyka stitching</h3>
                        <div class="table-scroll">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Para</th>
                                        <th>Status</th>
                                        <th>offset X</th>
                                        <th>offset Y</th>
                                        <th>Overlap</th>
                                        <th>Seam</th>
                                        <th>Confidence</th>
                                    </tr>
                                </thead>
                                <tbody>
                """);
        for (ItemImportPageModel.OpenCvStitchDiagnosticView diagnostic : preview.getStitchDiagnostics()) {
            html.append("<tr><td>")
                    .append(diagnostic.getFirstSourceIndex())
                    .append(" -> ")
                    .append(diagnostic.getSecondSourceIndex())
                    .append("</td><td>")
                    .append(escapeHtml(diagnostic.getStatus()))
                    .append("</td><td>")
                    .append(diagnostic.getOffsetX())
                    .append("</td><td>")
                    .append(diagnostic.getOffsetY())
                    .append("</td><td>")
                    .append(diagnostic.getOverlapHeight())
                    .append("</td><td>")
                    .append(diagnostic.getSeamY())
                    .append("</td><td>")
                    .append(escapeHtml(diagnostic.getConfidence()))
                    .append("</td></tr>");
        }
        html.append("""
                                </tbody>
                            </table>
                        </div>
                    </section>
                """);
        return html.toString();
    }

    private static String renderOpenCvHorizontalDividerDetection(ItemImportPageModel.OpenCvImagePreview preview) {
        StringBuilder html = new StringBuilder();
        if (preview.hasHorizontalDividerDetectionError()) {
            html.append("""
                    <section class="subpanel opencv-divider-error" data-opencv-divider-error>
                        <h3>Błąd detekcji separatorów</h3>
                        <p class="helper">%s</p>
                    </section>
                    """.formatted(escapeHtml(preview.getHorizontalDividerDetectionError())));
        }
        if (!preview.hasHorizontalDividerDetection()) {
            return html.toString();
        }

        ItemImportPageModel.HorizontalDividerDetectionView detection = preview.getHorizontalDividerDetection();
        html.append("""
                    <section class="subpanel opencv-divider-diagnostics" data-opencv-divider-diagnostics>
                        <h3>Detekcja separatorów poziomych</h3>
                        <div class="summary-grid">
                """);
        html.append(renderSummaryCard("Wykryte linie poziome", Integer.toString(detection.getDividerCount())))
                .append(renderSummaryCard("Powstałe części", Integer.toString(detection.getPartCount())))
                .append("""
                        </div>
                        <div class="opencv-divider-layout">
                            <div class="opencv-preview-frame opencv-divider-overlay-frame">
                                <img src="%s" alt="Złożony obraz itemu z oznaczonymi separatorami" data-opencv-divider-overlay data-opencv-divider-count="%d" data-opencv-part-count="%d">
                            </div>
                """.formatted(
                        escapeHtml(detection.getOverlayDataUri()),
                        detection.getDividerCount(),
                        detection.getPartCount()
                ));
        html.append(renderOpenCvDividerTable(detection));
        html.append("""
                        </div>
                """);
        html.append(renderOpenCvPartPreviews(detection));
        html.append(renderItemPartStructureNormalization(detection));
        html.append("""
                    </section>
                """);
        return html.toString();
    }

    private static String renderOpenCvDividerTable(ItemImportPageModel.HorizontalDividerDetectionView detection) {
        if (detection.getDividers().isEmpty()) {
            return "<p class=\"helper\" data-opencv-divider-empty>Brak strukturalnych separatorów poziomych.</p>";
        }
        StringBuilder html = new StringBuilder("""
                            <div class="table-scroll opencv-divider-table">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Separator</th>
                                            <th>Y</th>
                                            <th>Pasmo</th>
                                            <th>Strength</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                """);
        for (ItemImportPageModel.HorizontalDividerView divider : detection.getDividers()) {
            html.append("<tr data-opencv-divider-row>")
                    .append("<td>Separator ")
                    .append(divider.getIndex())
                    .append("</td><td data-opencv-divider-y>")
                    .append(divider.getY())
                    .append("</td><td>")
                    .append(divider.getBandStartY())
                    .append("..")
                    .append(divider.getBandEndY())
                    .append("</td><td data-opencv-divider-strength>")
                    .append(escapeHtml(divider.getStrength()))
                    .append("</td></tr>");
        }
        html.append("""
                                    </tbody>
                                </table>
                            </div>
                """);
        return html.toString();
    }

    private static String renderOpenCvPartPreviews(ItemImportPageModel.HorizontalDividerDetectionView detection) {
        StringBuilder html = new StringBuilder("""
                        <div class="opencv-part-grid" data-opencv-part-previews>
                """);
        for (ItemImportPageModel.ItemImagePartView part : detection.getParts()) {
            html.append("""
                            <article class="opencv-part-card" data-opencv-part-preview data-opencv-part-name="%s">
                                <h4>%s</h4>
                                <p class="helper">raw part #%d, %d..%d, %d x %d</p>
                                <div class="opencv-part-frame">
                                    <img src="%s" alt="%s">
                                </div>
                            </article>
                    """.formatted(
                    escapeHtml(part.getName()),
                    escapeHtml(part.getName()),
                    part.getIndex(),
                    part.getStartYInclusive(),
                    part.getEndYExclusive(),
                    part.getWidth(),
                    part.getHeight(),
                    escapeHtml(part.getDataUri()),
                    escapeHtml(part.getName())
            ));
        }
        html.append("""
                        </div>
                """);
        return html.toString();
    }

    private static String renderItemPartStructureNormalization(ItemImportPageModel.HorizontalDividerDetectionView detection) {
        StringBuilder html = new StringBuilder();
        if (detection.hasStructureNormalizationError()) {
            html.append("""
                    <section class="subpanel item-structure-error" data-item-structure-error>
                        <h3>Błąd normalizacji struktury</h3>
                        <p class="helper">%s</p>
                    </section>
                    """.formatted(escapeHtml(detection.getStructureNormalizationError())));
        }
        if (!detection.hasStructureNormalization()) {
            return html.toString();
        }

        ItemImportPageModel.ItemPartStructureNormalizationView normalization = detection.getStructureNormalization();
        html.append("""
                    <section class="subpanel item-structure-normalization" data-item-structure-normalization data-item-structure-status="%s" data-item-structure-raw-part-count="%d">
                        <h3>Normalizacja struktury</h3>
                        <div class="summary-grid">
                """.formatted(
                escapeHtml(normalization.getStatus()),
                normalization.getRawPartCount()
        ));
        html.append(renderSummaryCard("Status", normalization.getStatus()))
                .append(renderSummaryCard("Raw parts", Integer.toString(normalization.getRawPartCount())))
                .append("""
                        </div>
                """);
        if (normalization.getMappings().isEmpty()) {
            html.append("""
                        <p class="helper" data-item-structure-no-mapping>Brak normalizacji strukturalnej. Nie zgadujemy brakujących slotów.</p>
                    """);
        } else {
            html.append("""
                        <div class="table-scroll item-structure-table">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Structural slot</th>
                                        <th>Raw part</th>
                                    </tr>
                                </thead>
                                <tbody>
                    """);
            for (ItemImportPageModel.StructuralItemPartMappingView mapping : normalization.getMappings()) {
                html.append("<tr data-item-structure-row data-item-structure-slot=\"")
                        .append(escapeHtml(mapping.getStructuralSlot()))
                        .append("\" data-item-structure-present=\"")
                        .append(mapping.isPresent())
                        .append("\" data-item-structure-raw-part-index=\"")
                        .append(mapping.getRawPartIndex())
                        .append("\"><td>")
                        .append(escapeHtml(mapping.getStructuralSlot()))
                        .append("</td><td>")
                        .append(mapping.isPresent()
                                ? "raw part #" + mapping.getRawPartIndex()
                                : "absent")
                        .append("</td></tr>");
            }
            html.append("""
                                </tbody>
                            </table>
                        </div>
                    """);
        }
        if (normalization.hasDiagnosticReason()) {
            html.append("<p class=\"helper\" data-item-structure-diagnostic-reason>")
                    .append(escapeHtml(normalization.getDiagnosticReason()))
                    .append("</p>");
        }
        html.append(renderPart1Segmentation(normalization));
        html.append("""
                    </section>
                """);
        return html.toString();
    }

    private static String renderPart1Segmentation(ItemImportPageModel.ItemPartStructureNormalizationView normalization) {
        StringBuilder html = new StringBuilder();
        if (normalization.hasPart1SegmentationError()) {
            html.append("""
                        <section class="subpanel part1-segmentation-error" data-part1-segmentation-error>
                            <h4>PART_1 — błąd segmentacji ROI</h4>
                            <p class="helper">%s</p>
                        </section>
                    """.formatted(escapeHtml(normalization.getPart1SegmentationError())));
        }
        if (!normalization.hasPart1Segmentation()) {
            return html.toString();
        }

        ItemImportPageModel.Part1SegmentationView segmentation = normalization.getPart1Segmentation();
        html.append("""
                    <section class="subpanel part1-segmentation" data-part1-segmentation data-part1-source-index="%d" data-part1-source-name="%s">
                        <h4>PART_1 — Segmentacja ROI</h4>
                        <div class="opencv-part-grid">
                            <article class="opencv-part-card" data-part1-source>
                                <h4>Cały PART_1</h4>
                                <p class="helper">raw part #%d, %d x %d</p>
                                <div class="opencv-part-frame">
                                    <img src="%s" alt="Cały PART_1">
                                </div>
                            </article>
                    """.formatted(
                segmentation.getSourcePartIndex(),
                escapeHtml(segmentation.getSourcePartName()),
                segmentation.getSourcePartIndex(),
                segmentation.getSourcePartWidth(),
                segmentation.getSourcePartHeight(),
                escapeHtml(segmentation.getSourcePartDataUri())
        ));
        for (ItemImportPageModel.Part1RoiView roi : segmentation.getRois()) {
            html.append("""
                            <article class="opencv-part-card" data-part1-roi data-part1-roi-type="%s" data-part1-roi-x="%d" data-part1-roi-y="%d" data-part1-roi-width="%d" data-part1-roi-height="%d">
                                <h4>%s</h4>
                                <p class="helper">x=%d, y=%d, width=%d, height=%d</p>
                                <div class="opencv-part-frame">
                                    <img src="%s" alt="PART_1 ROI %s">
                                </div>
                                <p class="helper">OCR raw, %d ms%s</p>
                                <pre class="part1-raw-ocr" data-part1-roi-ocr>%s</pre>
                                %s
                                %s
                                %s
                            </article>
                    """.formatted(
                    escapeHtml(roi.getType()),
                    roi.getX(),
                    roi.getY(),
                    roi.getWidth(),
                    roi.getHeight(),
                    escapeHtml(roi.getType()),
                    roi.getX(),
                    roi.getY(),
                    roi.getWidth(),
                    roi.getHeight(),
                    escapeHtml(roi.getDataUri()),
                    escapeHtml(roi.getType()),
                    roi.getOcrDurationMillis(),
                    roi.getOcrPreprocessing().isBlank() ? "" : ", " + escapeHtml(roi.getOcrPreprocessing()),
                    escapeHtml(roi.getRawOcrText()),
                    renderGreaterAffixCount(roi),
                    renderPart1RoiSubRegions(roi),
                    renderPart1RoiOcrError(roi)
            ));
        }
        html.append("""
                        </div>
                """);
        html.append(renderPart1ReadResult(segmentation));
        html.append("""
                    </section>
                """);
        return html.toString();
    }

    private static String renderPart1ReadResult(ItemImportPageModel.Part1SegmentationView segmentation) {
        if (!segmentation.hasReadResult()) {
            return "";
        }
        ItemImportPageModel.Part1ReadResultView result = segmentation.getReadResult();
        StringBuilder html = new StringBuilder("""
                    <section class="subpanel part1-read-result" data-part1-read-result>
                        <h4>PART_1 — Wynik odczytu</h4>
                        <div class="summary-grid">
                """);
          html.append(renderSummaryCard("Nazwa", result.getItemName().isBlank() ? "brak" : result.getItemName()))
                  .append(renderSummaryCard("Greater Affix stars", Integer.toString(result.getGreaterAffixCount())))
                  .append(renderSummaryCard("Klasyfikacja raw", result.getClassificationRaw().isBlank() ? "brak" : result.getClassificationRaw()))
                  .append(renderSummaryCard("Typ", result.getItemType()))
                  .append(renderSummaryCard("Rzadkość", result.getItemRarity()))
                  .append(renderSummaryCard("Ancient", result.getAncient()))
                  .append(renderSummaryCard("Mythic Unique", result.getMythicUnique()))
                  .append(renderSummaryCard("Slot", result.getEquipmentSlot()))
                  .append(renderSummaryCard("Moc przedmiotu", result.getItemPower()))
                  .append(renderSummaryCard("Jakość wyświetlana", result.getDisplayedQuality()))
                .append(renderSummaryCard("Masterworking quality", result.getMasterworkingQuality()))
                .append("""
                        </div>
                """);
        if (result.hasParseFailures()) {
            html.append("""
                        <ul class="error-list" data-part1-read-failures>
                    """);
            for (String failure : result.getParseFailures()) {
                html.append("<li>").append(escapeHtml(failure)).append("</li>");
            }
            html.append("""
                        </ul>
                    """);
        }
        html.append("""
                    </section>
                """);
        return html.toString();
    }

    private static String renderPart1RoiSubRegions(ItemImportPageModel.Part1RoiView roi) {
        if (!roi.hasSubRegions()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<div class=\"part1-subroi-list\" data-part1-roi-subregions>");
        for (ItemImportPageModel.Part1RoiSubRegionView subRegion : roi.getSubRegions()) {
            html.append("""
                    <div class="part1-subroi" data-part1-roi-subregion data-part1-roi-subregion-type="%s" data-part1-roi-subregion-x="%d" data-part1-roi-subregion-y="%d" data-part1-roi-subregion-width="%d" data-part1-roi-subregion-height="%d">
                        <p class="helper">%s: x=%d, y=%d, width=%d, height=%d%s</p>
                        <img src="%s" alt="PART_1 sub-ROI %s">
                    </div>
                    """.formatted(
                    escapeHtml(subRegion.getType()),
                    subRegion.getX(),
                    subRegion.getY(),
                    subRegion.getWidth(),
                    subRegion.getHeight(),
                    escapeHtml(subRegion.getType()),
                    subRegion.getX(),
                    subRegion.getY(),
                    subRegion.getWidth(),
                    subRegion.getHeight(),
                    subRegion.getPreprocessing().isBlank() ? "" : ", " + escapeHtml(subRegion.getPreprocessing()),
                    escapeHtml(subRegion.getDataUri()),
                    escapeHtml(subRegion.getType())
            ));
        }
        html.append("</div>");
        return html.toString();
    }

    private static String renderGreaterAffixCount(ItemImportPageModel.Part1RoiView roi) {
        if (!roi.hasGreaterAffixDiagnostics()) {
            return "";
        }
        return "<p class=\"helper\" data-part1-greater-affix-count=\""
                + roi.getGreaterAffixCount()
                + "\">Greater Affix stars: "
                + roi.getGreaterAffixCount()
                + "</p>";
    }

    private static String renderPart1RoiOcrError(ItemImportPageModel.Part1RoiView roi) {
        if (!roi.hasOcrError()) {
            return "";
        }
        return "<p class=\"helper part1-ocr-error\" data-part1-roi-ocr-error>"
                + escapeHtml(roi.getOcrError())
                + "</p>";
    }

    private static String buildUploadAction(String currentBuildQuery) {
        if (currentBuildQuery == null || currentBuildQuery.isBlank()) {
            return "/importuj-item-ze-screena";
        }
        return "/importuj-item-ze-screena?" + currentBuildQuery;
    }

    private static String renderSummaryCard(String label, String value) {
        return CurrentBuildCalculationSectionsRenderer.renderSummaryCard(label, value);
    }

    private static String escapeHtml(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }
}
