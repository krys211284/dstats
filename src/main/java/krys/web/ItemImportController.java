package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.AssembledItemImage;
import krys.itemimport.image.AssembledItemImageDebugWriter;
import krys.itemimport.image.DeterministicItemPartStructureNormalizer;
import krys.itemimport.image.DeterministicPart1RoiSegmenter;
import krys.itemimport.image.HorizontalDivider;
import krys.itemimport.image.HorizontalDividerDetectionResult;
import krys.itemimport.image.HorizontalDividerDetector;
import krys.itemimport.image.ItemImageAssembler;
import krys.itemimport.image.ItemImagePart;
import krys.itemimport.image.ItemPartStructureNormalizer;
import krys.itemimport.image.ItemStructureStatus;
import krys.itemimport.image.Part1Roi;
import krys.itemimport.image.Part1RoiOcrReader;
import krys.itemimport.image.Part1RoiOcrResult;
import krys.itemimport.image.Part1RoiOcrSubRegion;
import krys.itemimport.image.Part1ReadParser;
import krys.itemimport.image.Part1ReadResult;
import krys.itemimport.image.Part1RoiSegmenter;
import krys.itemimport.image.Part1SegmentationResult;
import krys.itemimport.image.PropertyBlock;
import krys.itemimport.image.PropertyBlockOcrReader;
import krys.itemimport.image.PropertyBlockReadResult;
import krys.itemimport.image.PropertyBlockSemanticMapper;
import krys.itemimport.image.PropertyBlockSemanticResult;
import krys.itemimport.image.PropertyBlockSegmentationResult;
import krys.itemimport.image.PropertyBlockSegmenter;
import krys.itemimport.image.StructuralItemPart;
import krys.itemimport.image.StructuralItemPartSlot;
import krys.itemimport.image.StructuredItemParts;
import krys.itemimport.image.TesseractPart1RoiOcrReader;
import krys.itemimport.image.TesseractPropertyBlockOcrReader;
import krys.itemimport.image.VisualPropertyBlockSegmenter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Kontroler diagnostycznego importu obrazu itemu bez zapisu wyniku do domeny itemu. */
public final class ItemImportController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final ItemImportPageRenderer renderer;
    private final HeroService heroService;
    private final ItemImageAssembler itemImageAssembler;
    private final AssembledItemImageDebugWriter assembledImageDebugWriter;
    private final HorizontalDividerDetector horizontalDividerDetector;
    private final ItemPartStructureNormalizer itemPartStructureNormalizer;
    private final Part1RoiSegmenter part1RoiSegmenter;
    private final Part1RoiOcrReader part1RoiOcrReader;
    private final PropertyBlockSegmenter propertyBlockSegmenter;
    private final PropertyBlockOcrReader propertyBlockOcrReader;
    private final Part1ReadParser part1ReadParser = new Part1ReadParser();
    private final PropertyBlockSemanticMapper propertyBlockSemanticMapper = new PropertyBlockSemanticMapper();

    public ItemImportController(ItemImportPageRenderer renderer,
                                HeroService heroService,
                                ItemImageAssembler itemImageAssembler,
                                AssembledItemImageDebugWriter assembledImageDebugWriter,
                                HorizontalDividerDetector horizontalDividerDetector) {
        this(renderer, heroService, itemImageAssembler, assembledImageDebugWriter, horizontalDividerDetector,
                new DeterministicItemPartStructureNormalizer(), new DeterministicPart1RoiSegmenter(),
                new TesseractPart1RoiOcrReader(), new VisualPropertyBlockSegmenter(),
                new TesseractPropertyBlockOcrReader());
    }

    ItemImportController(ItemImportPageRenderer renderer,
                         HeroService heroService,
                         ItemImageAssembler itemImageAssembler,
                         AssembledItemImageDebugWriter assembledImageDebugWriter,
                         HorizontalDividerDetector horizontalDividerDetector,
                         ItemPartStructureNormalizer itemPartStructureNormalizer) {
        this(renderer, heroService, itemImageAssembler, assembledImageDebugWriter, horizontalDividerDetector,
                itemPartStructureNormalizer, new DeterministicPart1RoiSegmenter(), new TesseractPart1RoiOcrReader(),
                new VisualPropertyBlockSegmenter(), new TesseractPropertyBlockOcrReader());
    }

    ItemImportController(ItemImportPageRenderer renderer,
                         HeroService heroService,
                         ItemImageAssembler itemImageAssembler,
                         AssembledItemImageDebugWriter assembledImageDebugWriter,
                         HorizontalDividerDetector horizontalDividerDetector,
                         ItemPartStructureNormalizer itemPartStructureNormalizer,
                         Part1RoiSegmenter part1RoiSegmenter) {
        this(renderer, heroService, itemImageAssembler, assembledImageDebugWriter, horizontalDividerDetector,
                itemPartStructureNormalizer, part1RoiSegmenter, new TesseractPart1RoiOcrReader(),
                new VisualPropertyBlockSegmenter(), new TesseractPropertyBlockOcrReader());
    }

    ItemImportController(ItemImportPageRenderer renderer,
                         HeroService heroService,
                         ItemImageAssembler itemImageAssembler,
                         AssembledItemImageDebugWriter assembledImageDebugWriter,
                         HorizontalDividerDetector horizontalDividerDetector,
                         ItemPartStructureNormalizer itemPartStructureNormalizer,
                         Part1RoiSegmenter part1RoiSegmenter,
                         Part1RoiOcrReader part1RoiOcrReader) {
        this(renderer, heroService, itemImageAssembler, assembledImageDebugWriter, horizontalDividerDetector,
                itemPartStructureNormalizer, part1RoiSegmenter, part1RoiOcrReader, new VisualPropertyBlockSegmenter(),
                new TesseractPropertyBlockOcrReader());
    }

    ItemImportController(ItemImportPageRenderer renderer,
                         HeroService heroService,
                         ItemImageAssembler itemImageAssembler,
                         AssembledItemImageDebugWriter assembledImageDebugWriter,
                         HorizontalDividerDetector horizontalDividerDetector,
                         ItemPartStructureNormalizer itemPartStructureNormalizer,
                         Part1RoiSegmenter part1RoiSegmenter,
                         Part1RoiOcrReader part1RoiOcrReader,
                         PropertyBlockSegmenter propertyBlockSegmenter) {
        this(renderer, heroService, itemImageAssembler, assembledImageDebugWriter, horizontalDividerDetector,
                itemPartStructureNormalizer, part1RoiSegmenter, part1RoiOcrReader, propertyBlockSegmenter,
                new TesseractPropertyBlockOcrReader());
    }

    ItemImportController(ItemImportPageRenderer renderer,
                         HeroService heroService,
                         ItemImageAssembler itemImageAssembler,
                         AssembledItemImageDebugWriter assembledImageDebugWriter,
                         HorizontalDividerDetector horizontalDividerDetector,
                         ItemPartStructureNormalizer itemPartStructureNormalizer,
                         Part1RoiSegmenter part1RoiSegmenter,
                         Part1RoiOcrReader part1RoiOcrReader,
                         PropertyBlockSegmenter propertyBlockSegmenter,
                         PropertyBlockOcrReader propertyBlockOcrReader) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.heroService = Objects.requireNonNull(heroService, "heroService");
        this.itemImageAssembler = Objects.requireNonNull(itemImageAssembler, "itemImageAssembler");
        this.assembledImageDebugWriter = assembledImageDebugWriter;
        this.horizontalDividerDetector = Objects.requireNonNull(horizontalDividerDetector, "horizontalDividerDetector");
        this.itemPartStructureNormalizer = Objects.requireNonNull(itemPartStructureNormalizer, "itemPartStructureNormalizer");
        this.part1RoiSegmenter = Objects.requireNonNull(part1RoiSegmenter, "part1RoiSegmenter");
        this.part1RoiOcrReader = Objects.requireNonNull(part1RoiOcrReader, "part1RoiOcrReader");
        this.propertyBlockSegmenter = Objects.requireNonNull(propertyBlockSegmenter, "propertyBlockSegmenter");
        this.propertyBlockOcrReader = Objects.requireNonNull(propertyBlockOcrReader, "propertyBlockOcrReader");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                HeroProfile activeHero = heroService.getActiveHero().orElse(null);
                String currentBuildQuery = activeHero == null ? "" : activeHero.getCurrentBuildQuery();
                renderPage(exchange, emptyPageModel(currentBuildQuery, activeHero));
                return;
            }
            if ("POST".equals(method)) {
                renderPage(exchange, handleImageUpload(exchange));
                return;
            }
            exchange.getResponseHeaders().set("Allow", "GET, POST");
            exchange.sendResponseHeaders(405, -1);
        } finally {
            exchange.close();
        }
    }

    private ItemImportPageModel handleImageUpload(HttpExchange exchange) throws IOException {
        HeroProfile activeHero = heroService.getActiveHero().orElse(null);
        if (activeHero == null) {
            return buildPageModel(List.of("Brak aktywnego bohatera. Utwórz albo wybierz bohatera przed importem itemu."),
                    null, "", "", null);
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
            return buildPageModel(List.of("Nieobsługiwany typ danych formularza dla importu itemu."),
                    null, "", activeHero.getCurrentBuildQuery(), activeHero);
        }

        try {
            MultipartFormSupport.MultipartFormData multipartFormData = MultipartFormSupport.parse(exchange);
            List<MultipartFormSupport.MultipartFilePart> fileParts = multipartFormData.requireFiles("itemImage");
            List<ItemImageImportRequest> importRequests = fileParts.stream()
                    .map(filePart -> new ItemImageImportRequest(
                            filePart.getOriginalFilename(),
                            filePart.getContentType(),
                            filePart.getContent()
                    ))
                    .toList();
            OpenCvPreviewResult previewResult = buildOpenCvPreview(importRequests);
            return buildPageModel(
                    List.of(),
                    previewResult.preview(),
                    previewResult.error(),
                    activeHero.getCurrentBuildQuery(),
                    activeHero
            );
        } catch (IllegalArgumentException exception) {
            return buildPageModel(
                    List.of(exception.getMessage()),
                    null,
                    "",
                    activeHero.getCurrentBuildQuery(),
                    activeHero
            );
        }
    }

    private ItemImportPageModel emptyPageModel(String currentBuildQuery, HeroProfile activeHero) {
        return buildPageModel(List.of(), null, "", currentBuildQuery, activeHero);
    }

    private ItemImportPageModel buildPageModel(List<String> errors,
                                               ItemImportPageModel.OpenCvImagePreview openCvImagePreview,
                                               String openCvPreviewError,
                                               String currentBuildQuery,
                                               HeroProfile activeHero) {
        return new ItemImportPageModel(
                errors,
                openCvImagePreview,
                openCvPreviewError,
                activeHero,
                buildHelpText(),
                currentBuildQuery
        );
    }

    private static String buildHelpText() {
        return "Możesz dodać jeden lub kilka screenów tego samego itemu. Jeśli tooltip jest przewijany, dodaj screeny w kolejności od góry do dołu. Limit: 1..5 screenów jednego itemu. Aktualny pipeline pokazuje diagnostykę struktury oraz surowy POC OCR tylko dla ROI PART_1.";
    }

    private void renderPage(HttpExchange exchange, ItemImportPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }

    private OpenCvPreviewResult buildOpenCvPreview(List<ItemImageImportRequest> importRequests) {
        try {
            AssembledItemImage assembledImage = itemImageAssembler.assemble(importRequests);
            String debugWarning = writeOpenCvDebugArtifact(assembledImage);
            String mimeType = "image/" + assembledImage.getFormat().toLowerCase(Locale.ROOT);
            String dataUri = "data:" + mimeType + ";base64,"
                    + Base64.getEncoder().encodeToString(assembledImage.getImageBytes());
            List<ItemImportPageModel.OpenCvStitchDiagnosticView> stitchDiagnostics = assembledImage.getStitchDiagnostics().stream()
                    .map(diagnostic -> new ItemImportPageModel.OpenCvStitchDiagnosticView(
                            diagnostic.getFirstSourceIndex(),
                            diagnostic.getSecondSourceIndex(),
                            diagnostic.getOffsetX(),
                            diagnostic.getOffsetY(),
                            diagnostic.getOverlapHeight(),
                            diagnostic.getSeamY(),
                            String.format(Locale.US, "%.3f", diagnostic.getConfidence()),
                            diagnostic.getStatus()
                    ))
                    .toList();
            HorizontalDividerPreviewResult horizontalDividerPreviewResult = buildHorizontalDividerPreview(assembledImage);
            return new OpenCvPreviewResult(
                    new ItemImportPageModel.OpenCvImagePreview(
                            dataUri,
                            assembledImage.getSourceImageCount(),
                            assembledImage.getWidth(),
                            assembledImage.getHeight(),
                            assembledImage.getFormat(),
                            assembledImage.getCompositionPolicy(),
                            assembledImage.getAssemblyStatus(),
                            stitchDiagnostics,
                            horizontalDividerPreviewResult.preview(),
                            horizontalDividerPreviewResult.error(),
                            debugWarning
                    ),
                    ""
            );
        } catch (RuntimeException exception) {
            return new OpenCvPreviewResult(null, "Błąd podglądu OpenCV: " + safeMessage(exception));
        }
    }

    private HorizontalDividerPreviewResult buildHorizontalDividerPreview(AssembledItemImage assembledImage) {
        try {
            HorizontalDividerDetectionResult result = horizontalDividerDetector.detect(assembledImage);
            String overlayMimeType = "image/" + result.getOverlayFormat().toLowerCase(Locale.ROOT);
            String overlayDataUri = "data:" + overlayMimeType + ";base64,"
                    + Base64.getEncoder().encodeToString(result.getOverlayImageBytes());
            List<ItemImportPageModel.HorizontalDividerView> dividers = new java.util.ArrayList<>();
            int dividerIndex = 1;
            for (HorizontalDivider divider : result.getDividers()) {
                dividers.add(new ItemImportPageModel.HorizontalDividerView(
                        dividerIndex++,
                        divider.getY(),
                        divider.getBandStartY(),
                        divider.getBandEndY(),
                        String.format(Locale.US, "%.3f", divider.getStrength())
                ));
            }
            List<ItemImportPageModel.ItemImagePartView> parts = new java.util.ArrayList<>();
            for (ItemImagePart part : result.getParts()) {
                String partMimeType = "image/" + part.getFormat().toLowerCase(Locale.ROOT);
                String partDataUri = "data:" + partMimeType + ";base64,"
                        + Base64.getEncoder().encodeToString(part.getImageBytes());
                parts.add(new ItemImportPageModel.ItemImagePartView(
                        part.getIndex(),
                        part.getName(),
                        part.getStartYInclusive(),
                        part.getEndYExclusive(),
                        part.getWidth(),
                        part.getHeight(),
                        partDataUri
                ));
            }
            StructureNormalizationPreviewResult structureNormalizationPreviewResult =
                    buildStructureNormalizationPreview(result.getParts());
            return new HorizontalDividerPreviewResult(
                    new ItemImportPageModel.HorizontalDividerDetectionView(
                            result.getDividers().size(),
                            result.getParts().size(),
                            overlayDataUri,
                            dividers,
                            parts,
                            structureNormalizationPreviewResult.preview(),
                            structureNormalizationPreviewResult.error()
                    ),
                    ""
            );
        } catch (RuntimeException exception) {
            return new HorizontalDividerPreviewResult(null, "Błąd detekcji separatorów: " + safeMessage(exception));
        }
    }

    private StructureNormalizationPreviewResult buildStructureNormalizationPreview(List<ItemImagePart> rawParts) {
        try {
            StructuredItemParts structuredItemParts = itemPartStructureNormalizer.normalize(rawParts);
            List<ItemImportPageModel.StructuralItemPartMappingView> mappings = new java.util.ArrayList<>();
            for (StructuralItemPartSlot slot : StructuralItemPartSlot.values()) {
                Optional<StructuralItemPart> structuralPart = structuredItemParts.find(slot);
                structuralPart.ifPresent(part -> mappings.add(
                        new ItemImportPageModel.StructuralItemPartMappingView(
                                slot.name(),
                                true,
                                rawPartOrderIndex(structuredItemParts.getRawParts(), part.getSourcePart()),
                                semanticStatusFor(slot, true)
                        )
                ));
                if (structuralPart.isEmpty()
                        && structuredItemParts.getStatus() == ItemStructureStatus.STRUCTURALLY_NORMALIZED) {
                    mappings.add(new ItemImportPageModel.StructuralItemPartMappingView(
                            slot.name(),
                            false,
                            0,
                            semanticStatusFor(slot, false)
                    ));
                }
            }
            Part1SegmentationPreviewResult part1SegmentationPreviewResult =
                    buildPart1SegmentationPreview(structuredItemParts);
            PropertyBlockSegmentationPreviewResult propertyBlockSegmentationPreviewResult =
                    buildPropertyBlockSegmentationPreview(structuredItemParts);
            return new StructureNormalizationPreviewResult(
                    new ItemImportPageModel.ItemPartStructureNormalizationView(
                            structuredItemParts.getStatus().name(),
                            structuredItemParts.getRawParts().size(),
                            mappings,
                            structuredItemParts.getDiagnosticReason(),
                            part1SegmentationPreviewResult.preview(),
                            part1SegmentationPreviewResult.error(),
                            propertyBlockSegmentationPreviewResult.previews(),
                            propertyBlockSegmentationPreviewResult.error()
                    ),
                    ""
            );
        } catch (RuntimeException exception) {
            return new StructureNormalizationPreviewResult(null, "Błąd normalizacji struktury: " + safeMessage(exception));
        }
    }

    private PropertyBlockSegmentationPreviewResult buildPropertyBlockSegmentationPreview(StructuredItemParts structuredItemParts) {
        List<ItemImportPageModel.PropertyBlockSegmentationView> previews = new java.util.ArrayList<>();
        try {
            for (StructuralItemPartSlot slot : List.of(StructuralItemPartSlot.PART_2, StructuralItemPartSlot.PART_3)) {
                Optional<StructuralItemPart> part = structuredItemParts.find(slot);
                if (part.isPresent()) {
                    previews.add(toPropertyBlockSegmentationView(propertyBlockSegmenter.segment(part.get())));
                }
            }
            return new PropertyBlockSegmentationPreviewResult(previews, "");
        } catch (RuntimeException exception) {
            return new PropertyBlockSegmentationPreviewResult(previews,
                    "Błąd segmentacji bloków właściwości PART_2/PART_3: " + safeMessage(exception));
        }
    }

    private ItemImportPageModel.PropertyBlockSegmentationView toPropertyBlockSegmentationView(
            PropertyBlockSegmentationResult result) {
        ItemImagePart sourcePart = result.getSourcePart().getSourcePart();
        List<ItemImportPageModel.PropertyBlockView> blockViews = result.getBlocks().stream()
                .map(this::toPropertyBlockView)
                .toList();
        return new ItemImportPageModel.PropertyBlockSegmentationView(
                result.getSourcePart().getSlot().name(),
                sourcePart.getIndex(),
                sourcePart.getName(),
                sourcePart.getWidth(),
                sourcePart.getHeight(),
                result.getDiagnostics(),
                blockViews
        );
    }

    private ItemImportPageModel.PropertyBlockView toPropertyBlockView(PropertyBlock block) {
        PropertyBlockReadResult readResult = propertyBlockOcrReader.read(block);
        PropertyBlockSemanticResult semanticResult = propertyBlockSemanticMapper.map(block, readResult);
        return new ItemImportPageModel.PropertyBlockView(
                block.getOrder(),
                block.getAnchorType().name(),
                block.getX(),
                block.getY(),
                block.getWidth(),
                block.getHeight(),
                toDataUri(block),
                block.getDiagnostics(),
                block.getAnchor()
                        .map(anchor -> "x=" + anchor.getX()
                                + ", y=" + anchor.getY()
                                + ", width=" + anchor.getWidth()
                                + ", height=" + anchor.getHeight()
                                + ", confidence=" + String.format(Locale.US, "%.3f", anchor.getConfidence()))
                        .orElse(""),
                toPropertyBlockReadResultView(readResult),
                toPropertyBlockSemanticResultView(semanticResult)
        );
    }

    private static ItemImportPageModel.PropertyBlockSemanticResultView toPropertyBlockSemanticResultView(
            PropertyBlockSemanticResult result) {
        return new ItemImportPageModel.PropertyBlockSemanticResultView(
                result.getKind().name(),
                result.getDefinitionId(),
                result.getCandidateDefinitionIds(),
                result.isGreaterAffix(),
                result.isNumericValueCertain()
        );
    }

    private static ItemImportPageModel.PropertyBlockReadResultView toPropertyBlockReadResultView(
            PropertyBlockReadResult result) {
        return new ItemImportPageModel.PropertyBlockReadResultView(
                result.getSourcePart().name(),
                result.getBlockOrder(),
                result.getAnchorType().name(),
                result.getRawText(),
                result.getDurationMillis(),
                result.getPreprocessing(),
                result.getFailureDiagnostics(),
                result.getNumericEvidence().stream()
                        .map(evidence -> new ItemImportPageModel.PropertyBlockNumericEvidenceView(
                                evidence.getTokenOrder(),
                                evidence.getSelectedValue(),
                                evidence.getSupportingProfiles(),
                                evidence.getAlternativeValues(),
                                evidence.getSupportCount(),
                                evidence.isAmbiguous()
                        ))
                        .toList()
        );
    }

    private Part1SegmentationPreviewResult buildPart1SegmentationPreview(StructuredItemParts structuredItemParts) {
        Optional<StructuralItemPart> part1 = structuredItemParts.find(StructuralItemPartSlot.PART_1);
        if (part1.isEmpty()) {
            return new Part1SegmentationPreviewResult(null, "");
        }
        try {
            Part1SegmentationResult result = part1RoiSegmenter.segment(part1.get());
            ItemImagePart sourcePart = result.getSourcePart().getSourcePart();
            String sourceMimeType = "image/" + sourcePart.getFormat().toLowerCase(Locale.ROOT);
            String sourceDataUri = "data:" + sourceMimeType + ";base64,"
                    + Base64.getEncoder().encodeToString(sourcePart.getImageBytes());
            List<Part1RoiPreviewResult> roiPreviewResults = result.getRois().stream()
                    .map(this::toPart1RoiPreviewResult)
                    .toList();
            List<ItemImportPageModel.Part1RoiView> roiViews = roiPreviewResults.stream()
                    .map(Part1RoiPreviewResult::view)
                    .toList();
            ItemImportPageModel.Part1ReadResultView readResultView = toPart1ReadResultView(part1ReadParser.parse(
                    roiPreviewResults.stream()
                            .flatMap(preview -> preview.ocrResult().stream())
                            .toList()
            ));
            return new Part1SegmentationPreviewResult(
                    new ItemImportPageModel.Part1SegmentationView(
                            sourceDataUri,
                            sourcePart.getIndex(),
                            sourcePart.getName(),
                            sourcePart.getWidth(),
                            sourcePart.getHeight(),
                            roiViews,
                            readResultView
                    ),
                    ""
            );
        } catch (RuntimeException exception) {
            return new Part1SegmentationPreviewResult(null, "Błąd segmentacji PART_1: " + safeMessage(exception));
        }
    }

    private Part1RoiPreviewResult toPart1RoiPreviewResult(Part1Roi roi) {
        try {
            Part1RoiOcrResult ocrResult = part1RoiOcrReader.read(roi);
            List<ItemImportPageModel.Part1RoiSubRegionView> subRegionViews = ocrResult.getSubRegions().stream()
                    .map(ItemImportController::toPart1RoiSubRegionView)
                    .toList();
            ItemImportPageModel.Part1RoiView view = new ItemImportPageModel.Part1RoiView(
                    roi.getType().name(),
                    roi.getX(),
                    roi.getY(),
                    roi.getWidth(),
                    roi.getHeight(),
                    toDataUri(roi),
                    ocrResult.getRawText(),
                    ocrResult.getDurationMillis(),
                    ocrResult.getPreprocessing(),
                    "",
                    subRegionViews,
                    ocrResult.getGreaterAffixCount()
            );
            return new Part1RoiPreviewResult(view, Optional.of(ocrResult));
        } catch (RuntimeException exception) {
            return new Part1RoiPreviewResult(new ItemImportPageModel.Part1RoiView(
                    roi.getType().name(),
                    roi.getX(),
                    roi.getY(),
                    roi.getWidth(),
                    roi.getHeight(),
                    toDataUri(roi),
                    "",
                    0L,
                    "",
                    "Błąd OCR PART_1 ROI: " + safeMessage(exception)
            ), Optional.empty());
        }
    }

    private static ItemImportPageModel.Part1ReadResultView toPart1ReadResultView(Part1ReadResult result) {
        return new ItemImportPageModel.Part1ReadResultView(
                  result.getItemName(),
                  result.getGreaterAffixCount(),
                  result.getClassificationRaw(),
                  result.getItemType(),
                  result.getItemRarity(),
                  result.getAncient().map(ItemImportController::formatBoolean).orElse(""),
                  result.getMythicUnique().map(ItemImportController::formatBoolean).orElse(""),
                  result.getEquipmentSlot().map(Enum::name).orElse(""),
                  result.getItemPower().isPresent() ? Integer.toString(result.getItemPower().getAsInt()) : "",
                  result.getDisplayedQuality().isPresent() ? Integer.toString(result.getDisplayedQuality().getAsInt()) : "",
                  result.getMasterworkingQuality().isPresent() ? Integer.toString(result.getMasterworkingQuality().getAsInt()) : "",
                  result.getParseFailures()
          );
      }

    private static String formatBoolean(boolean value) {
        return value ? "true" : "false";
    }

    private static String semanticStatusFor(StructuralItemPartSlot slot, boolean present) {
        if (slot == StructuralItemPartSlot.PART_4 && present) {
            return "SKIPPED_NOT_REQUIRED";
        }
        return "";
    }

    private static String toDataUri(Part1Roi roi) {
        String mimeType = "image/" + roi.getFormat().toLowerCase(Locale.ROOT);
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(roi.getImageBytes());
    }

    private static String toDataUri(PropertyBlock block) {
        String mimeType = "image/" + block.getFormat().toLowerCase(Locale.ROOT);
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(block.getImageBytes());
    }

    private static ItemImportPageModel.Part1RoiSubRegionView toPart1RoiSubRegionView(Part1RoiOcrSubRegion subRegion) {
        String mimeType = "image/" + subRegion.getFormat().toLowerCase(Locale.ROOT);
        String dataUri = "data:" + mimeType + ";base64,"
                + Base64.getEncoder().encodeToString(subRegion.getImageBytes());
        return new ItemImportPageModel.Part1RoiSubRegionView(
                subRegion.getType(),
                subRegion.getX(),
                subRegion.getY(),
                subRegion.getWidth(),
                subRegion.getHeight(),
                dataUri,
                subRegion.getPreprocessing()
        );
    }

    private static int rawPartOrderIndex(List<ItemImagePart> rawParts, ItemImagePart sourcePart) {
        for (int index = 0; index < rawParts.size(); index++) {
            if (rawParts.get(index) == sourcePart) {
                return index + 1;
            }
        }
        return 0;
    }

    private String writeOpenCvDebugArtifact(AssembledItemImage assembledImage) {
        if (assembledImageDebugWriter == null) {
            return "";
        }
        assembledImageDebugWriter.tryWriteDefault(assembledImage);
        return "";
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }

    private record OpenCvPreviewResult(ItemImportPageModel.OpenCvImagePreview preview, String error) {
    }

    private record HorizontalDividerPreviewResult(ItemImportPageModel.HorizontalDividerDetectionView preview,
                                                  String error) {
    }

    private record StructureNormalizationPreviewResult(ItemImportPageModel.ItemPartStructureNormalizationView preview,
                                                       String error) {
    }

    private record Part1SegmentationPreviewResult(ItemImportPageModel.Part1SegmentationView preview, String error) {
    }

    private record PropertyBlockSegmentationPreviewResult(
            List<ItemImportPageModel.PropertyBlockSegmentationView> previews,
            String error) {
    }

    private record Part1RoiPreviewResult(ItemImportPageModel.Part1RoiView view,
                                         Optional<Part1RoiOcrResult> ocrResult) {
    }
}
