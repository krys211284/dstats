package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.opencv.OpenCvHorizontalDividerDetector;
import krys.itemimport.image.opencv.OpenCvItemImageAssembler;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Diagnostyka przepływu komponent -> anchor -> pasmo -> PropertyBlock bez zmiany heurystyk produkcyjnych. */
class PropertyBlockPipelineDiagnosticsTest {
    private static final Path OUTPUT = Path.of("target", "test-debug", "property-blocks");

    private final OpenCvItemImageAssembler assembler = new OpenCvItemImageAssembler();
    private final OpenCvHorizontalDividerDetector dividerDetector = new OpenCvHorizontalDividerDetector();
    private final ItemPartStructureNormalizer normalizer = new DeterministicItemPartStructureNormalizer();
    private final VisualPropertyBlockAnchorDetector detector = new VisualPropertyBlockAnchorDetector();
    private final PropertyBlockSegmenter segmenter = new VisualPropertyBlockSegmenter(detector);

    @Test
    void shouldExposePropertyBlockPipelineWithoutChangingProductionDecisions() throws Exception {
        Files.createDirectories(OUTPUT);
        List<DiagnosticFixture> fixtures = List.of(
                new DiagnosticFixture("buty-part3", List.of("buty.png"),
                        StructuralItemPartSlot.PART_3, 2),
                new DiagnosticFixture("dziedzic-full-part3", List.of("dziedzic-zatracenia-helm-full.png"),
                        StructuralItemPartSlot.PART_3, 10),
                new DiagnosticFixture("dziedzic-pair-part3",
                        List.of("dziedzic-zatracenia-helm-1.png", "dziedzic-zatracenia-helm-2.png"),
                        StructuralItemPartSlot.PART_3, 10),
                new DiagnosticFixture("tarcza-pair-part3", List.of("tarcza1.png", "tarcza2.png"),
                        StructuralItemPartSlot.PART_3, 8),
                new DiagnosticFixture("tarcza-single-part3", List.of("tarcza.png"),
                        StructuralItemPartSlot.PART_3, 4),
                new DiagnosticFixture("verathiel-part3", List.of("verathiel-miecz-v2.png"),
                        StructuralItemPartSlot.PART_3, 6),
                new DiagnosticFixture("tarcza-pair-part2", List.of("tarcza1.png", "tarcza2.png"),
                        StructuralItemPartSlot.PART_2, 3)
        );

        for (DiagnosticFixture fixture : fixtures) {
            DiagnosticResult result = diagnose(fixture);
            assertEquals(fixture.expectedBlockCount(), result.blocks().size(),
                    fixture.label() + " block count");
            writeDiagnostics(result);
            System.out.println(result.summary());
        }
        Files.writeString(OUTPUT.resolve("logical-audit.tsv"), logicalAuditTsv(), StandardCharsets.UTF_8);

        assertTrue(Files.isRegularFile(OUTPUT.resolve("tarcza-pair-part3-components.tsv")));
        assertTrue(Files.isRegularFile(OUTPUT.resolve("logical-audit.tsv")));
    }

    private static String logicalAuditTsv() {
        StringBuilder text = new StringBuilder("fixture\texpected_entry\tresult\tevidence\tcause\tpipeline_stage\n");
        List<LogicalAuditRow> rows = List.of(
                new LogicalAuditRow("dziedzic-pair-part3", "#1 leading base stats", "REAL",
                        "B1/T1-T2", "Prawidlowy leading block.", "segmentation"),
                new LogicalAuditRow("dziedzic-pair-part3", "#2 critical-hit affix", "REAL",
                        "B2/T3-T4; recovered anchor from C001 frame split", "Lokalny anchor zostal odzyskany z mega-komponentu ramki przed odrzuceniem C001 jako TOO_LARGE.", "connected-components/frame split -> grouping"),
                new LogicalAuditRow("dziedzic-pair-part3", "#3-#4 affixes", "REAL",
                        "B3-B4/T5-T6", "Oba wpisy maja PropertyBlock.", "segmentation"),
                new LogicalAuditRow("dziedzic-pair-part3", "#5 core-skills affix", "REAL",
                        "B5/T7-T8; recovered anchor from C001 frame split", "Lokalny anchor zostal odzyskany z mega-komponentu ramki przed odrzuceniem C001 jako TOO_LARGE.", "connected-components/frame split -> anchor association"),
                new LogicalAuditRow("dziedzic-pair-part3", "#6-#10 remaining entries", "REAL",
                        "B6-B10", "Trzy wpisy modyfikatorow i oba sockety maja PropertyBlock.", "segmentation"),
                new LogicalAuditRow("dziedzic-pair-part3", "flavor text", "FALSE_POSITIVE",
                        "C146=21,502,19,17; T20-T21; no PropertyBlock after socket-pair trailing guard", "Fragment kursywy nadal spelnia geometrie anchora, ale nie tworzy bloku po parze socketow.", "candidate filtering -> trailing guard"),
                new LogicalAuditRow("verathiel-part3", "#1-#6 expected entries", "REAL",
                        "B1-B6; T1-T13", "Wszystkie prawdziwe wpisy maja PropertyBlock.", "segmentation"),
                new LogicalAuditRow("verathiel-part3", "lore fragment 1", "FALSE_POSITIVE",
                        "C032=29,320,17,14; T14-T15; no PropertyBlock", "Poczatek kursywy pozostaje kandydatem geometrycznym, ale brak odstepu od tekstu identyfikuje go jako fragment lore.", "draft flavor filtering"),
                new LogicalAuditRow("verathiel-part3", "lore fragment 2", "FALSE_POSITIVE",
                        "C037=27,361,28,17; T16-T18; no PropertyBlock", "Kolejny fragment kursywy nalezy do tego samego koncowego ciagu lore i nie tworzy bloku.", "draft flavor filtering"),
                new LogicalAuditRow("tarcza-pair-part2", "#1 leading base stat", "REAL_FALSE_ANCHOR",
                        "B1; C005=46,14,9,16; T1", "Ten sam leading region zostal zachowany, ale fragment tekstu otrzymal UNKNOWN_ICON.", "candidate filtering/classification -> anchor association"),
                new LogicalAuditRow("tarcza-pair-part2", "#2-#3 affixes", "REAL",
                        "B2-B3", "Oba wpisy maja PropertyBlock.", "segmentation"),
                new LogicalAuditRow("tarcza-pair-part3", "#1-#8 expected entries", "REAL",
                        "B1-B8; B7=T9-T19; B8=38,433,131,28", "Legendary power zawiera wszystkie 11 linii, a EMPTY_SOCKET caly tekst Puste gniazdo.", "continuation grouping -> final bounds"),
                new LogicalAuditRow("dziedzic-full-part3", "#1-#10 expected entries", "REAL",
                        "B1-B10", "Wszystkie logiczne wpisy maja PropertyBlock; typy anchorow pozostaja poza audytem.", "segmentation")
        );
        for (LogicalAuditRow row : rows) {
            text.append(row.fixture()).append('\t').append(row.expectedEntry()).append('\t')
                    .append(row.result()).append('\t').append(row.evidence()).append('\t')
                    .append(row.cause()).append('\t').append(row.pipelineStage()).append('\n');
        }
        return text.toString();
    }

    private DiagnosticResult diagnose(DiagnosticFixture fixture) throws Exception {
        StructuralItemPart part = structured(fixture.filenames()).find(fixture.slot()).orElseThrow();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(part.getSourcePart().getImageBytes()));
        Scale scale = Scale.from(image);
        List<Candidate> candidates = extractRawComponents(image, scale);
        applyDetectorFilteringAndMerge(candidates, image, scale);

        List<PropertyBlockAnchor> detectorAnchors = detector.detect(image);
        List<PropertyBlockAnchor> segmenterAnchors = filterAnchorColumn(detectorAnchors, image.getWidth());
        correlateProductionAnchors(candidates, detectorAnchors, segmenterAnchors);

        List<TextBand> bands = detectTextBands(image, scale);
        associateBands(bands, segmenterAnchors, image.getWidth());

        PropertyBlockSegmentationResult segmentation = segmenter.segment(part);
        correlateBlocks(candidates, bands, segmentation.getBlocks());
        return new DiagnosticResult(fixture, image, scale, candidates, bands, detectorAnchors,
                segmenterAnchors, segmentation.getBlocks());
    }

    private StructuredItemParts structured(List<String> filenames) throws Exception {
        List<ItemImageImportRequest> requests = new ArrayList<>();
        for (String filename : filenames) {
            requests.add(new ItemImageImportRequest(filename, "image/png",
                    Files.readAllBytes(Path.of("src", "test", "resources", "items", filename))));
        }
        AssembledItemImage assembled = assembler.assemble(requests);
        return normalizer.normalize(dividerDetector.detect(assembled).getParts());
    }

    private static List<Candidate> extractRawComponents(BufferedImage image, Scale scale) {
        int laneWidth = Math.max(1, Math.min(image.getWidth(), (int) Math.ceil(image.getWidth() * 0.24d)));
        boolean[][] visited = new boolean[image.getHeight()][laneWidth];
        List<Candidate> candidates = new ArrayList<>();
        int index = 1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < laneWidth; x++) {
                if (visited[y][x] || !isCandidatePixel(image, x, y, scale)) {
                    continue;
                }
                Candidate candidate = collect(image, visited, x, y, laneWidth, index++, scale);
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private static Candidate collect(BufferedImage image,
                                     boolean[][] visited,
                                     int startX,
                                     int startY,
                                     int laneWidth,
                                     int index,
                                     Scale scale) {
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;
        Candidate candidate = new Candidate(index, laneWidth, image.getWidth(), image.getHeight(), scale.unit());
        while (!queue.isEmpty()) {
            int[] point = queue.removeFirst();
            int x = point[0];
            int y = point[1];
            candidate.add(x, y, image.getRGB(x, y));
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) {
                        continue;
                    }
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx < 0 || nx >= laneWidth || ny < 0 || ny >= image.getHeight()
                            || visited[ny][nx] || !isCandidatePixel(image, nx, ny, scale)) {
                        continue;
                    }
                    visited[ny][nx] = true;
                    queue.addLast(new int[]{nx, ny});
                }
            }
        }
        return candidate;
    }

    private static void applyDetectorFilteringAndMerge(List<Candidate> candidates,
                                                       BufferedImage image,
                                                       Scale scale) {
        List<Candidate> accepted = new ArrayList<>();
        for (Candidate candidate : candidates) {
            candidate.rejectionReason = rejectionReason(candidate, image, scale);
            if (candidate.rejectionReason != null) {
                candidate.detectorStatus = "REJECTED";
                continue;
            }
            candidate.classification = classify(candidate, scale);
            candidate.detectorStatus = "ACCEPTED";
            accepted.add(candidate);
        }

        accepted.sort(Comparator.comparingInt(Candidate::minY).thenComparingInt(Candidate::minX));
        List<Candidate> merged = new ArrayList<>();
        for (Candidate candidate : accepted) {
            if (!merged.isEmpty()) {
                Candidate previous = merged.getLast();
                boolean closeY = Math.abs(previous.centerY() - candidate.centerY())
                        <= Math.max(1, scale.unit() * 0.18d);
                boolean closeX = Math.abs(previous.centerX() - candidate.centerX())
                        <= Math.max(1, scale.unit() * 0.45d);
                if (closeY && closeX) {
                    if (candidate.area() > previous.area()) {
                        previous.detectorStatus = "MERGED";
                        previous.mergedWith = candidate.id();
                        merged.set(merged.size() - 1, candidate);
                    } else {
                        candidate.detectorStatus = "MERGED";
                        candidate.mergedWith = previous.id();
                    }
                    continue;
                }
            }
            merged.add(candidate);
        }
    }

    private static String rejectionReason(Candidate candidate, BufferedImage image, Scale scale) {
        double unit = scale.unit();
        if (candidate.pixelCount < Math.max(3, Math.round(unit * unit * 0.015d))) {
            return "AREA_OUT_OF_RANGE";
        }
        if (candidate.width() < Math.max(2, unit * 0.18d)
                || candidate.height() < Math.max(2, unit * 0.18d)) {
            return "TOO_SMALL";
        }
        if ((candidate.width() > unit * 1.85d || candidate.height() > unit * 1.85d)
                && !isSocketSizedComponent(candidate, image.getWidth(), scale)) {
            return "TOO_LARGE";
        }
        if (candidate.minX < image.getWidth() * 0.025d || candidate.minY < image.getHeight() * 0.015d) {
            return "FRAME_NOISE";
        }
        if (candidate.centerX() > image.getWidth() * 0.18d) {
            return "OUTSIDE_ANCHOR_REGION";
        }
        double fill = candidate.pixelCount / (double) candidate.area();
        if (fill < 0.08d || fill > 0.94d) {
            return "AREA_OUT_OF_RANGE";
        }
        double aspect = candidate.aspect();
        if (aspect < 0.30d || aspect > 2.90d) {
            return "ASPECT_RATIO_OUT_OF_RANGE";
        }
        return null;
    }

    private static boolean isSocketSizedComponent(Candidate candidate, int imageWidth, Scale scale) {
        double unit = scale.unit();
        double height = candidate.height();
        return height >= unit * 1.15d
                && height <= unit * 1.45d
                && candidate.width() <= Math.min(unit * 3.55d, imageWidth * 0.19d)
                && candidate.aspect() <= 2.65d;
    }

    private static void correlateProductionAnchors(List<Candidate> candidates,
                                                   List<PropertyBlockAnchor> detectorAnchors,
                                                   List<PropertyBlockAnchor> segmenterAnchors) {
        for (Candidate candidate : candidates) {
            Optional<PropertyBlockAnchor> detectorAnchor = detectorAnchors.stream()
                    .filter(anchor -> sameBounds(candidate, anchor))
                    .findFirst();
            if (detectorAnchor.isEmpty()) {
                continue;
            }
            candidate.detectorStatus = "ACCEPTED";
            candidate.classification = detectorAnchor.get().getType();
            boolean acceptedBySegmenter = segmenterAnchors.stream().anyMatch(anchor -> sameBounds(candidate, anchor));
            candidate.segmenterStatus = acceptedBySegmenter ? "ACCEPTED" : "REJECTED";
            if (!acceptedBySegmenter) {
                candidate.rejectionReason = "OUTSIDE_ICON_COLUMN";
            }
        }
    }

    private static void associateBands(List<TextBand> bands,
                                       List<PropertyBlockAnchor> anchors,
                                       int imageWidth) {
        for (TextBand band : bands) {
            Optional<PropertyBlockAnchor> anchor = findAnchorForBand(band, anchors, imageWidth);
            anchor.ifPresent(value -> band.anchor = value);
        }
    }

    private static void correlateBlocks(List<Candidate> candidates,
                                        List<TextBand> bands,
                                        List<PropertyBlock> blocks) {
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            PropertyBlock block = blocks.get(blockIndex);
            int index = blockIndex + 1;
            block.getAnchor().ifPresent(anchor -> candidates.stream()
                    .filter(candidate -> sameBounds(candidate, anchor))
                    .findFirst()
                    .ifPresent(candidate -> {
                        candidate.createdBlock = index;
                        candidate.blockBounds = bounds(block);
                    }));
            for (TextBand band : bands) {
                int centerY = band.centerY();
                if (centerY >= block.getY() && centerY < block.getY() + block.getHeight()
                        && band.maxX > block.getX() && band.minX < block.getX() + block.getWidth()) {
                    band.blockIndex = index;
                }
            }
        }
    }

    private static List<PropertyBlockAnchor> filterAnchorColumn(List<PropertyBlockAnchor> anchors, int imageWidth) {
        if (anchors.isEmpty()) {
            return anchors;
        }
        double unit = Math.max(8.0d, imageWidth * 0.052d);
        int minX = anchors.stream().mapToInt(PropertyBlockAnchor::getX).min().orElse(0);
        double maxIconColumnX = minX + unit * 0.82d;
        double maxCompactIconColumnX = minX + unit * 1.50d;
        return anchors.stream()
                .filter(anchor -> anchor.getX() <= maxIconColumnX
                        || (isFrameRecoveredAnchor(anchor)
                        && anchor.getX() <= maxCompactIconColumnX
                        && anchor.getWidth() <= unit * 0.95d))
                .toList();
    }

    private static boolean isFrameRecoveredAnchor(PropertyBlockAnchor anchor) {
        return anchor.getConfidence() > 0.0d && anchor.getConfidence() < 0.445d;
    }

    private static Optional<PropertyBlockAnchor> findAnchorForBand(TextBand band,
                                                                   List<PropertyBlockAnchor> anchors,
                                                                   int imageWidth) {
        int centerY = band.centerY();
        return anchors.stream()
                .filter(anchor -> Math.abs(centerY(anchor) - centerY)
                        <= Math.max(band.height(), imageWidth * 0.025d))
                .filter(anchor -> centerX(anchor) < Math.max(band.minX, imageWidth * 0.27d))
                .min(Comparator.comparingInt(PropertyBlockPipelineDiagnosticsTest::centerX)
                        .thenComparingInt(anchor -> Math.abs(centerY(anchor) - centerY)));
    }

    private static List<TextBand> detectTextBands(BufferedImage image, Scale scale) {
        int width = image.getWidth();
        int height = image.getHeight();
        int marginX = horizontalScanMargin(width);
        boolean[] activeRows = new boolean[height];
        for (int y = 0; y < height; y++) {
            int foreground = 0;
            for (int x = marginX; x < width - marginX; x++) {
                if (isForeground(image.getRGB(x, y))) {
                    foreground++;
                }
            }
            activeRows[y] = foreground >= Math.max(3, (int) Math.round(width * 0.022d));
        }

        List<int[]> verticalBands = new ArrayList<>();
        int y = 0;
        while (y < height) {
            while (y < height && !activeRows[y]) {
                y++;
            }
            if (y >= height) {
                break;
            }
            int start = y;
            int lastActive = y;
            int gap = 0;
            int bridgeGap = Math.max(1, (int) Math.round(scale.unit() * 0.055d));
            while (y + 1 < height && gap <= bridgeGap) {
                y++;
                if (activeRows[y]) {
                    lastActive = y;
                    gap = 0;
                } else {
                    gap++;
                }
            }
            int end = lastActive + 1;
            if (end - start >= Math.max(2, (int) Math.round(scale.unit() * 0.12d))) {
                verticalBands.add(new int[]{start, end});
            }
            y = Math.max(y, end);
        }

        List<TextBand> lines = new ArrayList<>();
        int sourceIndex = 1;
        for (int[] verticalBand : verticalBands) {
            TextBand bounds = foregroundBounds(image, verticalBand[0], verticalBand[1], sourceIndex++);
            if (bounds != null
                    && bounds.width() >= Math.max(5, (int) Math.round(scale.unit() * 0.28d))
                    && bounds.height() >= Math.max(2, (int) Math.round(scale.unit() * 0.12d))) {
                lines.add(bounds);
            }
        }

        List<TextBand> merged = new ArrayList<>();
        for (TextBand line : lines.stream().sorted(Comparator.comparingInt(line -> line.minY)).toList()) {
            if (!merged.isEmpty()) {
                TextBand previous = merged.getLast();
                int gap = line.minY - previous.maxY;
                boolean sameVisualLine = gap <= Math.max(1, (int) Math.round(scale.unit() * 0.10d))
                        && Math.abs(line.centerY() - previous.centerY())
                        <= Math.max(previous.height(), line.height());
                if (sameVisualLine) {
                    previous.union(line);
                    continue;
                }
            }
            merged.add(line);
        }
        for (int index = 0; index < merged.size(); index++) {
            merged.get(index).index = index + 1;
        }
        return merged;
    }

    private static TextBand foregroundBounds(BufferedImage image, int startY, int endY, int index) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int marginX = horizontalScanMargin(image.getWidth());
        for (int y = startY; y < endY; y++) {
            for (int x = marginX; x < image.getWidth() - marginX; x++) {
                if (isForeground(image.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return minX == Integer.MAX_VALUE ? null : new TextBand(index, minX, minY, maxX + 1, maxY + 1);
    }

    private static void writeDiagnostics(DiagnosticResult result) throws Exception {
        String prefix = result.fixture().label();
        Files.writeString(OUTPUT.resolve(prefix + "-components.tsv"), componentsTsv(result), StandardCharsets.UTF_8);
        Files.writeString(OUTPUT.resolve(prefix + "-bands.tsv"), bandsTsv(result), StandardCharsets.UTF_8);
        Files.writeString(OUTPUT.resolve(prefix + "-blocks.tsv"), blocksTsv(result), StandardCharsets.UTF_8);
        ImageIO.write(overlay(result), "PNG", OUTPUT.resolve(prefix + "-overlay.png").toFile());
    }

    private static String componentsTsv(DiagnosticResult result) {
        StringBuilder text = new StringBuilder("id\tbounds\tnormalized_xywh\tcenter_xy\tpixels\tarea\taspect\tfill"
                + "\thue\tsaturation\tbrightness"
                + "\tchromatic_hue\tcolored_ratio\tcenter_fill\tborder_fill\tsymmetry\thole_ratio"
                + "\tlane_position\tlane_offset\tdetector_status\tsegmenter_status\treason\tmerged_with"
                + "\tclassification\ttext_bands\tproperty_block\tproperty_block_bounds\n");
        for (Candidate candidate : result.candidates()) {
            List<Integer> bandIndexes = result.bands().stream()
                    .filter(band -> band.anchor != null && sameBounds(candidate, band.anchor))
                    .map(band -> band.index)
                    .toList();
            text.append(candidate.id()).append('\t').append(candidate.bounds()).append('\t')
                    .append(candidate.normalizedBounds()).append('\t').append(candidate.centerBounds()).append('\t')
                    .append(candidate.pixelCount).append('\t').append(candidate.area()).append('\t')
                    .append(format(candidate.aspect())).append('\t').append(format(candidate.fill())).append('\t')
                    .append(format(candidate.averageHue())).append('\t')
                    .append(format(candidate.averageSaturation())).append('\t')
                    .append(format(candidate.averageBrightness())).append('\t')
                    .append(format(candidate.chromaticHue())).append('\t')
                    .append(format(candidate.coloredRatio())).append('\t')
                    .append(format(candidate.centerFill())).append('\t')
                    .append(format(candidate.borderFill())).append('\t')
                    .append(format(candidate.symmetry())).append('\t')
                    .append(format(candidate.holeRatio())).append('\t')
                    .append(format(candidate.centerX() / candidate.laneWidth)).append('\t')
                    .append(format(candidate.laneWidth - candidate.centerX())).append('\t')
                    .append(candidate.detectorStatus).append('\t').append(candidate.segmenterStatus).append('\t')
                    .append(value(candidate.rejectionReason)).append('\t').append(value(candidate.mergedWith)).append('\t')
                    .append(value(candidate.classification)).append('\t').append(bandIndexes).append('\t')
                    .append(candidate.createdBlock == 0 ? "NO" : candidate.createdBlock).append('\t')
                    .append(value(candidate.blockBounds)).append('\n');
        }
        return text.toString();
    }

    private static String bandsTsv(DiagnosticResult result) {
        StringBuilder text = new StringBuilder("index\tbounds\tnormalized_xywh\tanchor_bounds\tanchor_type"
                + "\tproperty_block\tstatus\n");
        for (TextBand band : result.bands()) {
            text.append(band.index).append('\t').append(band.bounds()).append('\t')
                    .append(normalized(band.minX, band.minY, band.width(), band.height(),
                            result.image().getWidth(), result.image().getHeight())).append('\t')
                    .append(band.anchor == null ? "-" : bounds(band.anchor)).append('\t')
                    .append(band.anchor == null ? "-" : band.anchor.getType()).append('\t')
                    .append(band.blockIndex == 0 ? "NO" : band.blockIndex).append('\t')
                    .append(band.anchor != null ? "ASSIGNED_TO_CANDIDATE"
                            : band.blockIndex != 0 ? "CONTINUATION" : "UNASSIGNED").append('\n');
        }
        return text.toString();
    }

    private static String blocksTsv(DiagnosticResult result) {
        StringBuilder text = new StringBuilder("index\tbounds\tnormalized_xywh\tanchor_bounds\tanchor_type\tdiagnostics\n");
        for (PropertyBlock block : result.blocks()) {
            text.append(block.getOrder()).append('\t').append(bounds(block)).append('\t')
                    .append(normalized(block.getX(), block.getY(), block.getWidth(), block.getHeight(),
                            result.image().getWidth(), result.image().getHeight())).append('\t')
                    .append(block.getAnchor().map(PropertyBlockPipelineDiagnosticsTest::bounds).orElse("-"))
                    .append('\t').append(block.getAnchorType()).append('\t').append(block.getDiagnostics()).append('\n');
        }
        return text.toString();
    }

    private static BufferedImage overlay(DiagnosticResult result) {
        BufferedImage overlay = new BufferedImage(result.image().getWidth(), result.image().getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = overlay.createGraphics();
        graphics.drawImage(result.image(), 0, 0, null);
        for (Candidate candidate : result.candidates()) {
            if ("ACCEPTED".equals(candidate.segmenterStatus)) {
                graphics.setColor(Color.GREEN);
            } else if ("MERGED".equals(candidate.detectorStatus)) {
                graphics.setColor(Color.ORANGE);
            } else {
                graphics.setColor(Color.RED);
            }
            graphics.drawRect(candidate.minX, candidate.minY, candidate.width() - 1, candidate.height() - 1);
            if (!"REJECTED".equals(candidate.detectorStatus) || candidate.area() >= 20) {
                graphics.drawString(candidate.id(), candidate.minX, Math.max(9, candidate.minY - 1));
            }
        }
        graphics.setColor(Color.CYAN);
        for (TextBand band : result.bands()) {
            graphics.drawRect(band.minX, band.minY, band.width() - 1, band.height() - 1);
            graphics.drawString("T" + band.index, band.minX, Math.max(9, band.minY - 1));
        }
        graphics.setColor(Color.MAGENTA);
        for (PropertyBlock block : result.blocks()) {
            graphics.drawRect(block.getX(), block.getY(), block.getWidth() - 1, block.getHeight() - 1);
            graphics.drawString("B" + block.getOrder(), block.getX(), Math.max(9, block.getY() - 1));
        }
        graphics.dispose();
        return overlay;
    }

    private static PropertyBlockAnchorType classify(Candidate component, Scale scale) {
        int width = component.width();
        int height = component.height();
        double saturation = component.averageSaturation();
        double brightness = component.averageBrightness();
        double fill = component.fill();
        double normalizedSize = Math.max(width, height) / scale.unit();
        double normalizedWidth = width / scale.unit();
        double normalizedHeight = height / scale.unit();
        double aspect = component.aspect();
        double chromaticHue = component.chromaticHue();
        double coloredRatio = component.coloredRatio();
        double centerFill = component.centerFill();
        double borderFill = component.borderFill();
        double holeRatio = component.holeRatio();

        if (normalizedHeight >= 0.75d && normalizedHeight <= 1.05d
                && isIconAspect(aspect, 0.45d, 0.66d)
                && fill >= 0.44d && coloredRatio <= 0.10d) {
            return PropertyBlockAnchorType.NONE;
        }

        boolean socketGeometry = normalizedHeight >= 1.15d && normalizedHeight <= 1.48d
                && normalizedWidth >= 1.25d && normalizedWidth <= 3.60d
                && isIconAspect(aspect, 1.05d, 2.70d);
        if (socketGeometry) {
            boolean filled = isRubyHue(chromaticHue) || holeRatio < 0.06d;
            return filled ? PropertyBlockAnchorType.FILLED_SOCKET : PropertyBlockAnchorType.EMPTY_SOCKET;
        }

        if (normalizedSize <= 0.26d
                && isIconAspect(aspect, 0.75d, 1.25d)
                && isPurpleOrBlue(chromaticHue, saturation)) {
            return PropertyBlockAnchorType.TRANSFIGURATION_MARK;
        }

        if (normalizedSize >= 0.48d && normalizedSize <= 0.70d
                && isIconAspect(aspect, 0.75d, 1.35d)
                && fill >= 0.68d
                && coloredRatio >= 0.45d
                && isGold(chromaticHue, saturation)) {
            return PropertyBlockAnchorType.LEGENDARY_POWER_STAR;
        }

        boolean chromaticUniqueStar = normalizedSize >= 0.60d && normalizedSize <= 0.78d
                && isIconAspect(aspect, 0.75d, 1.25d)
                && saturation >= 0.25d
                && coloredRatio >= 0.45d
                && isPurpleOrBlue(chromaticHue, saturation);
        boolean sparseUniqueStar = normalizedSize >= 0.72d && normalizedSize <= 0.90d
                && isIconAspect(aspect, 0.75d, 1.30d)
                && fill >= 0.45d && fill <= 0.62d
                && borderFill <= 0.35d
                && centerFill >= 0.80d;
        if (chromaticUniqueStar || sparseUniqueStar) {
            return PropertyBlockAnchorType.UNIQUE_POWER_STAR;
        }

        if (normalizedSize >= 0.75d && normalizedSize <= 1.10d
                && isIconAspect(aspect, 0.75d, 1.25d)
                && fill >= 0.60d && fill <= 0.78d
                && saturation >= 0.30d
                && coloredRatio >= 0.30d
                && isWarmStarHue(chromaticHue)) {
            return PropertyBlockAnchorType.GREATER_STAR;
        }

        if (isIconAspect(aspect, 0.72d, 1.38d)
                && fill >= 0.24d && fill <= 0.84d
                && normalizedSize >= 0.27d && normalizedSize <= 0.70d
                && brightness >= 0.24d
                && centerFill >= 0.70d) {
            return PropertyBlockAnchorType.DIAMOND;
        }

        if (isIconAspect(aspect, 0.75d, 1.25d)
                && normalizedSize >= 0.82d && normalizedSize <= 1.08d
                && fill >= 0.79d && fill <= 0.90d
                && brightness >= 0.34d
                && (saturation < 0.25d || coloredRatio <= 0.10d)
                && centerFill >= 0.90d) {
            return PropertyBlockAnchorType.TRANSFIGURATION_MARK;
        }
        return PropertyBlockAnchorType.UNKNOWN_ICON;
    }

    private static boolean isCandidatePixel(BufferedImage image, int x, int y, Scale scale) {
        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        int localContrast = localContrast(image, x, y);
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        return max >= scale.brightnessFloor()
                || localContrast >= scale.contrastFloor()
                || (max >= scale.colorBrightnessFloor() && hsb[1] >= 0.14f && max - min >= 8);
    }

    private static int localContrast(BufferedImage image, int x, int y) {
        int luminance = luminance(image.getRGB(x, y));
        int maxDifference = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < image.getWidth() && ny < image.getHeight()) {
                    maxDifference = Math.max(maxDifference,
                            Math.abs(luminance - luminance(image.getRGB(nx, ny))));
                }
            }
        }
        return maxDifference;
    }

    private static int luminance(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        return (red * 299 + green * 587 + blue * 114) / 1000;
    }

    private static boolean isForeground(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        return max >= 170 || (max >= 118 && hsb[1] >= 0.16f && max - min >= 18);
    }

    private static int horizontalScanMargin(int width) {
        return Math.max(1, (int) Math.round(width * 0.04d));
    }

    private static boolean isIconAspect(double aspect, double min, double max) {
        return aspect >= min && aspect <= max;
    }

    private static boolean isGold(double hue, double saturation) {
        return saturation >= 0.20d && hue >= 20.0d && hue <= 75.0d;
    }

    private static boolean isPurpleOrBlue(double hue, double saturation) {
        return saturation >= 0.20d && ((hue >= 205.0d && hue <= 285.0d) || (hue >= 285.0d && hue <= 330.0d));
    }

    private static boolean isRubyHue(double hue) {
        return hue <= 22.0d || hue >= 300.0d;
    }

    private static boolean isWarmStarHue(double hue) {
        return hue <= 25.0d || hue >= 340.0d;
    }

    private static boolean sameBounds(Candidate candidate, PropertyBlockAnchor anchor) {
        return candidate.minX == anchor.getX() && candidate.minY == anchor.getY()
                && candidate.width() == anchor.getWidth() && candidate.height() == anchor.getHeight();
    }

    private static int centerX(PropertyBlockAnchor anchor) {
        return anchor.getX() + anchor.getWidth() / 2;
    }

    private static int centerY(PropertyBlockAnchor anchor) {
        return anchor.getY() + anchor.getHeight() / 2;
    }

    private static String bounds(PropertyBlockAnchor anchor) {
        return anchor.getX() + "," + anchor.getY() + "," + anchor.getWidth() + "," + anchor.getHeight();
    }

    private static String bounds(PropertyBlock block) {
        return block.getX() + "," + block.getY() + "," + block.getWidth() + "," + block.getHeight();
    }

    private static String normalized(int x, int y, int width, int height, int imageWidth, int imageHeight) {
        return format(x / (double) imageWidth) + "," + format(y / (double) imageHeight) + ","
                + format(width / (double) imageWidth) + "," + format(height / (double) imageHeight);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String value(Object value) {
        return value == null ? "-" : value.toString();
    }

    private record DiagnosticFixture(String label,
                                     List<String> filenames,
                                     StructuralItemPartSlot slot,
                                     int expectedBlockCount) {
    }

    private record LogicalAuditRow(String fixture,
                                   String expectedEntry,
                                   String result,
                                   String evidence,
                                   String cause,
                                   String pipelineStage) {
    }

    private record DiagnosticResult(DiagnosticFixture fixture,
                                    BufferedImage image,
                                    Scale scale,
                                    List<Candidate> candidates,
                                    List<TextBand> bands,
                                    List<PropertyBlockAnchor> detectorAnchors,
                                    List<PropertyBlockAnchor> segmenterAnchors,
                                    List<PropertyBlock> blocks) {
        private String summary() {
            Map<String, Long> reasons = new LinkedHashMap<>();
            candidates.stream().filter(candidate -> candidate.rejectionReason != null)
                    .forEach(candidate -> reasons.merge(candidate.rejectionReason, 1L, Long::sum));
            return fixture.label() + ": image=" + image.getWidth() + "x" + image.getHeight()
                    + ", raw=" + candidates.size() + ", detectorAnchors=" + detectorAnchors.size()
                    + ", segmenterAnchors=" + segmenterAnchors.size() + ", bands=" + bands.size()
                    + ", blocks=" + blocks.size() + ", rejectionReasons=" + reasons;
        }
    }

    private record Scale(double unit, int brightnessFloor, int colorBrightnessFloor, int contrastFloor) {
        private static Scale from(BufferedImage image) {
            return new Scale(Math.max(8.0d, image.getWidth() * 0.052d), 96, 76, 20);
        }
    }

    private static final class Candidate {
        private final int index;
        private final int laneWidth;
        private final int imageWidth;
        private final int imageHeight;
        private final double unit;
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;
        private int pixelCount;
        private double hueSum;
        private double saturationSum;
        private double brightnessSum;
        private final List<CandidatePixel> pixels = new ArrayList<>();
        private String detectorStatus = "RAW";
        private String segmenterStatus = "NOT_REACHED";
        private String rejectionReason;
        private String mergedWith;
        private PropertyBlockAnchorType classification;
        private int createdBlock;
        private String blockBounds;

        private Candidate(int index, int laneWidth, int imageWidth, int imageHeight, double unit) {
            this.index = index;
            this.laneWidth = laneWidth;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.unit = unit;
        }

        private void add(int x, int y, int rgb) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            pixelCount++;
            pixels.add(new CandidatePixel(x, y, rgb));
            float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, null);
            hueSum += hsb[0] * 360.0d;
            saturationSum += hsb[1];
            brightnessSum += hsb[2];
        }

        private String id() {
            return "C" + String.format(Locale.ROOT, "%03d", index);
        }

        private int minX() {
            return minX;
        }

        private int minY() {
            return minY;
        }

        private int width() {
            return maxX - minX + 1;
        }

        private int height() {
            return maxY - minY + 1;
        }

        private int area() {
            return width() * height();
        }

        private double centerX() {
            return minX + width() / 2.0d;
        }

        private double centerY() {
            return minY + height() / 2.0d;
        }

        private double aspect() {
            return width() / (double) height();
        }

        private double fill() {
            return pixelCount / (double) area();
        }

        private double averageHue() {
            return hueSum / pixelCount;
        }

        private double averageSaturation() {
            return saturationSum / pixelCount;
        }

        private double averageBrightness() {
            return brightnessSum / pixelCount;
        }

        private double chromaticHue() {
            double x = 0.0d;
            double y = 0.0d;
            for (CandidatePixel pixel : pixels) {
                float[] hsb = hsb(pixel.rgb());
                double weight = hsb[1] * hsb[2];
                double angle = hsb[0] * Math.PI * 2.0d;
                x += Math.cos(angle) * weight;
                y += Math.sin(angle) * weight;
            }
            double hue = Math.toDegrees(Math.atan2(y, x));
            return hue < 0.0d ? hue + 360.0d : hue;
        }

        private double coloredRatio() {
            long colored = pixels.stream().filter(pixel -> {
                float[] hsb = hsb(pixel.rgb());
                return hsb[1] >= 0.25f && hsb[2] >= 0.25f;
            }).count();
            return colored / (double) pixelCount;
        }

        private double centerFill() {
            int startX = minX + width() / 4;
            int endX = maxX - width() / 4;
            int startY = minY + height() / 4;
            int endY = maxY - height() / 4;
            int centerArea = Math.max(1, (endX - startX + 1) * (endY - startY + 1));
            long centerPixels = pixels.stream()
                    .filter(pixel -> pixel.x() >= startX && pixel.x() <= endX
                            && pixel.y() >= startY && pixel.y() <= endY)
                    .count();
            return centerPixels / (double) centerArea;
        }

        private double borderFill() {
            int thickness = Math.max(1, (int) Math.round(Math.min(width(), height()) * 0.15d));
            int innerWidth = Math.max(0, width() - thickness * 2);
            int innerHeight = Math.max(0, height() - thickness * 2);
            int borderArea = Math.max(1, area() - innerWidth * innerHeight);
            long borderPixels = pixels.stream()
                    .filter(pixel -> pixel.x() < minX + thickness || pixel.x() > maxX - thickness
                            || pixel.y() < minY + thickness || pixel.y() > maxY - thickness)
                    .count();
            return borderPixels / (double) borderArea;
        }

        private double symmetry() {
            boolean[][] mask = mask();
            int mismatches = 0;
            for (int y = 0; y < height(); y++) {
                for (int x = 0; x < width(); x++) {
                    if (mask[y][x] != mask[y][width() - x - 1]
                            || mask[y][x] != mask[height() - y - 1][x]) {
                        mismatches++;
                    }
                }
            }
            return 1.0d - mismatches / (double) area();
        }

        private double holeRatio() {
            boolean[][] mask = mask();
            boolean[][] outside = new boolean[height()][width()];
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            for (int x = 0; x < width(); x++) {
                enqueueBackground(mask, outside, queue, x, 0);
                enqueueBackground(mask, outside, queue, x, height() - 1);
            }
            for (int y = 0; y < height(); y++) {
                enqueueBackground(mask, outside, queue, 0, y);
                enqueueBackground(mask, outside, queue, width() - 1, y);
            }
            while (!queue.isEmpty()) {
                int[] point = queue.removeFirst();
                int x = point[0];
                int y = point[1];
                enqueueBackground(mask, outside, queue, x - 1, y);
                enqueueBackground(mask, outside, queue, x + 1, y);
                enqueueBackground(mask, outside, queue, x, y - 1);
                enqueueBackground(mask, outside, queue, x, y + 1);
            }
            int holes = 0;
            for (int y = 0; y < height(); y++) {
                for (int x = 0; x < width(); x++) {
                    if (!mask[y][x] && !outside[y][x]) {
                        holes++;
                    }
                }
            }
            return holes / (double) area();
        }

        private boolean[][] mask() {
            boolean[][] mask = new boolean[height()][width()];
            for (CandidatePixel pixel : pixels) {
                mask[pixel.y() - minY][pixel.x() - minX] = true;
            }
            return mask;
        }

        private static void enqueueBackground(boolean[][] mask,
                                              boolean[][] outside,
                                              ArrayDeque<int[]> queue,
                                              int x,
                                              int y) {
            if (y < 0 || y >= mask.length || x < 0 || x >= mask[0].length
                    || mask[y][x] || outside[y][x]) {
                return;
            }
            outside[y][x] = true;
            queue.addLast(new int[]{x, y});
        }

        private static float[] hsb(int rgb) {
            return Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, null);
        }

        private String bounds() {
            return minX + "," + minY + "," + width() + "," + height();
        }

        private String normalizedBounds() {
            return normalized(minX, minY, width(), height(), imageWidth, imageHeight);
        }

        private String centerBounds() {
            return format(centerX()) + "," + format(centerY());
        }
    }

    private record CandidatePixel(int x, int y, int rgb) {
    }

    private static final class TextBand {
        private int index;
        private int minX;
        private int minY;
        private int maxX;
        private int maxY;
        private PropertyBlockAnchor anchor;
        private int blockIndex;

        private TextBand(int index, int minX, int minY, int maxX, int maxY) {
            this.index = index;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        private int width() {
            return maxX - minX;
        }

        private int height() {
            return maxY - minY;
        }

        private int centerY() {
            return minY + height() / 2;
        }

        private String bounds() {
            return minX + "," + minY + "," + width() + "," + height();
        }

        private void union(TextBand other) {
            minX = Math.min(minX, other.minX);
            minY = Math.min(minY, other.minY);
            maxX = Math.max(maxX, other.maxX);
            maxY = Math.max(maxY, other.maxY);
        }
    }
}
