package krys.itemimport;

import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.TemperingAffixDefinition;
import krys.tempering.TemperingAffixRegistry;
import krys.transfiguration.TransfigurationAffixCatalog;
import krys.transfiguration.TransfigurationAffixDefinition;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Dodaje kandydackie segmenty z długich bloków OCR na podstawie katalogów domenowych. */
final class ItemOcrBlockSegmenter {
    private final AffixRegistry affixRegistry;
    private final TemperingAffixRegistry temperingRegistry;
    private final AspectRegistry aspectRegistry;
    private final List<TransfigurationAffixDefinition> transfigurationDefinitions;

    ItemOcrBlockSegmenter() {
        this(ApplicationAffixRegistry.get(), ApplicationTemperingAffixRegistry.get(),
                ApplicationAspectRegistry.get(), TransfigurationAffixCatalog.definitions());
    }

    ItemOcrBlockSegmenter(AffixRegistry affixRegistry,
                          TemperingAffixRegistry temperingRegistry,
                          AspectRegistry aspectRegistry,
                          List<TransfigurationAffixDefinition> transfigurationDefinitions) {
        this.affixRegistry = affixRegistry;
        this.temperingRegistry = temperingRegistry;
        this.aspectRegistry = aspectRegistry;
        this.transfigurationDefinitions = transfigurationDefinitions == null ? List.of() : List.copyOf(transfigurationDefinitions);
    }

    List<String> segmentLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String line : lines) {
            addIfNew(result, seen, line);
            for (String segment : segmentLine(line)) {
                addIfNew(result, seen, segment);
            }
        }
        return result;
    }

    List<String> segmentLine(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }
        ItemImportDebugTrace.log("OCR_SEGMENTER_INPUT", () -> "source=" + ItemImportDebugTrace.compactText(line));
        String normalizedLine = normalize(line);
        List<Anchor> anchors = anchorsFor(line, normalizedLine);
        if (anchors.size() < 2 && line.length() < 80) {
            return List.of();
        }
        List<Anchor> compactAnchors = removeOverlappingAnchors(anchors);
        List<String> segments = new ArrayList<>();
        for (int index = 0; index < compactAnchors.size(); index++) {
            Anchor anchor = compactAnchors.get(index);
            int start = findSegmentStart(normalizedLine, anchor.start());
            int end = index + 1 < compactAnchors.size()
                    ? findSegmentStart(normalizedLine, compactAnchors.get(index + 1).start())
                    : normalizedLine.length();
            String segment = normalizeSegment(normalizedLine.substring(Math.max(0, start), Math.max(start, end)));
            if (isUsefulSegment(segment)) {
                segments.add(segment);
                ItemImportDebugTrace.log("OCR_SEGMENTER_MATCH", () -> "category=" + anchor.category()
                        + " definitionId=" + ItemImportDebugTrace.quote(anchor.definitionId())
                        + " segment=" + ItemImportDebugTrace.compactText(segment)
                        + " source=" + ItemImportDebugTrace.compactText(line));
            }
        }
        if (!segments.isEmpty()) {
            ItemImportDebugTrace.log("OCR_SEGMENTER_OUTPUT", () -> "segments=" + segments.size()
                    + " values=" + ItemImportDebugTrace.compactText(String.join(" | ", segments)));
        }
        return segments;
    }

    private List<Anchor> anchorsFor(String sourceLine, String normalizedLine) {
        List<Anchor> anchors = new ArrayList<>();
        for (AffixRegistry.AffixTextMatch match : affixRegistry.findMatches(sourceLine)) {
            anchors.add(new Anchor("affix", match.definition().getId(), match.start(), match.end()));
        }
        for (AffixDefinition definition : affixRegistry.all()) {
            addTextAnchor(anchors, normalizedLine, "affix", definition.getId(), definition.getDisplayName());
            for (String alias : definition.getOcrAliases()) {
                addTextAnchor(anchors, normalizedLine, "affix", definition.getId(), alias);
            }
        }
        for (TemperingAffixDefinition definition : temperingRegistry.all()) {
            addTextAnchor(anchors, normalizedLine, "tempering", definition.getId(), definition.getDisplayName());
        }
        for (TransfigurationAffixDefinition definition : transfigurationDefinitions) {
            addTextAnchor(anchors, normalizedLine, "transfiguration", definition.getId(), definition.getDisplayName());
        }
        for (AspectDefinition definition : aspectRegistry.all()) {
            for (String phrase : aspectPhrases(definition)) {
                addTextAnchor(anchors, normalizedLine, "aspect", definition.getId(), phrase);
            }
        }
        anchors.sort(Comparator
                .comparingInt(Anchor::start)
                .thenComparing((Anchor anchor) -> anchor.end() - anchor.start(), Comparator.reverseOrder())
                .thenComparing(Anchor::definitionId));
        return anchors;
    }

    private static void addTextAnchor(List<Anchor> anchors,
                                      String normalizedLine,
                                      String category,
                                      String definitionId,
                                      String phrase) {
        String normalizedPhrase = normalize(phrase);
        if (normalizedPhrase.length() < 6) {
            return;
        }
        int start = normalizedLine.indexOf(normalizedPhrase);
        if (start >= 0) {
            anchors.add(new Anchor(category, definitionId, start, start + normalizedPhrase.length()));
        }
    }

    private static List<String> aspectPhrases(AspectDefinition definition) {
        List<String> phrases = new ArrayList<>();
        phrases.add(definition.getDisplayName());
        phrases.addAll(definition.getTags());
        Matcher matcher = Pattern.compile("\\b(?:[A-ZĄĆĘŁŃÓŚŹŻa-ząćęłńóśźż]{5,}\\s+){2,6}[A-ZĄĆĘŁŃÓŚŹŻa-ząćęłńóśźż]{5,}\\b")
                .matcher(definition.getEffectDescription());
        while (matcher.find() && phrases.size() < 8) {
            phrases.add(matcher.group().trim());
        }
        return phrases;
    }

    private static List<Anchor> removeOverlappingAnchors(List<Anchor> anchors) {
        List<Anchor> result = new ArrayList<>();
        for (Anchor anchor : anchors) {
            boolean overlaps = false;
            for (Anchor existing : result) {
                if (anchor.start() >= existing.start() && anchor.start() < existing.end()) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                result.add(anchor);
            }
        }
        result.sort(Comparator.comparingInt(Anchor::start));
        return result;
    }

    private static int findSegmentStart(String sourceText, int anchorStart) {
        int index = Math.min(Math.max(0, anchorStart), sourceText.length());
        String prefix = sourceText.substring(0, index);
        Matcher valuePrefix = Pattern.compile("(?:^|\\s)(?:MNOZNIK\\s*[X×]?\\s*)?(?:[*★⭐✦✧•]\\s*)?\\+?\\s*[0-9]+(?:[,.][0-9]+)?\\s*(?:%|PKT\\.?|PT\\.?)?\\s*$")
                .matcher(prefix);
        if (valuePrefix.find()) {
            return valuePrefix.start();
        }
        while (index > 0) {
            char previous = sourceText.charAt(index - 1);
            if (Character.isDigit(previous)
                    || Character.isWhitespace(previous)
                    || previous == '+'
                    || previous == '*'
                    || previous == ','
                    || previous == '.'
                    || previous == '%'
                    || previous == '['
                    || previous == '('
                    || previous == '★'
                    || previous == '⭐'
                    || previous == '✦'
                    || previous == '✧'
                    || previous == '•') {
                index--;
                continue;
            }
            break;
        }
        return index;
    }

    private static String normalizeSegment(String segment) {
        String value = segment == null ? "" : segment.replaceAll("\\s+", " ").trim();
        value = value.replaceAll(
                "(?<!\\[)\\+\\s*1?([0-9]{2,3}(?:[,.][0-9]+)?)\\s*[-–—−]\\s*([0-9]{2,3}(?:[,.][0-9]+)?)1?(?=\\s|$|%)",
                "[$1 - $2]"
        );
        value = value.replaceAll("\\[\\s*\\+", "[");
        return value.trim();
    }

    private static boolean isUsefulSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return false;
        }
        String collapsed = segment.replaceAll("[^A-Z0-9]", "");
        return collapsed.length() >= 8
                && (segment.contains("+")
                || segment.contains("%")
                || segment.contains("[")
                || collapsed.contains("PODDAJSIENIENAWISCI")
                || collapsed.contains("ZADAJESZOBRAZENIA")
                || collapsed.contains("LASKIMATKI"));
    }

    private static void addIfNew(List<String> target, Set<String> seen, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        String key = normalize(line);
        if (seen.add(key)) {
            target.add(line);
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record Anchor(String category, String definitionId, int start, int end) {
    }
}
