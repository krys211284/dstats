package krys.itemimport;

import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingAffixDefinition;
import krys.tempering.TemperingAffixRegistry;
import krys.tempering.TemperingRuntimeStatus;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Wyciąga potwierdzone hartowania z OCR bez mieszania ich ze zwykłymi affixami. */
public final class ImportedItemTemperingExtractor {
    private static final String MAX_ANIMUS_DEFINITION_ID = "defense_max_animus";
    private static final String GREATER_MARKERS = "*★⭐✦✧✱✳✴✵✶✷✸✹✺✻✼✽✾❋❂◆◇♦●•";
    private static final Pattern MAX_ANIMUS_LINE_PATTERN = Pattern.compile(
            "([\\Q" + GREATER_MARKERS + "\\E]?\\s*\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+do\\s+maksymalnej\\s+liczby\\s+kumulacji\\s+Animuszu)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern LEADING_VALUE_PATTERN = Pattern.compile("\\+\\s*([0-9]+(?:[,.][0-9]+)?)");

    private final TemperingAffixRegistry registry;

    public ImportedItemTemperingExtractor() {
        this(ApplicationTemperingAffixRegistry.get());
    }

    ImportedItemTemperingExtractor(TemperingAffixRegistry registry) {
        this.registry = registry;
    }

    public List<ItemTemperingAffix> extractTemperingAffixes(FullItemRead fullItemRead) {
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return List.of();
        }
        Long itemPower = fullItemRead.getDetails().getItemPower();
        Map<String, ItemTemperingAffix> affixes = new LinkedHashMap<>();
        for (FullItemReadLine line : fullItemRead.getLines()) {
            if (line.getType() != FullItemReadLineType.TEMPERING) {
                continue;
            }
            parseKnownTemperingLine(line.getText(), itemPower)
                    .ifPresent(affix -> affixes.putIfAbsent(affix.getDefinitionId(), affix));
        }
        return List.copyOf(affixes.values());
    }

    static boolean isKnownTemperingLine(String line) {
        return MAX_ANIMUS_LINE_PATTERN.matcher(line == null ? "" : line).matches();
    }

    static Optional<String> normalizeKnownTemperingLine(String line) {
        Matcher matcher = MAX_ANIMUS_LINE_PATTERN.matcher(line == null ? "" : line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String segment = stripTrailingBoundaryMarkers(matcher.group(1).trim());
        if (isDisplayedPerfectedMaxAnimus(segment)) {
            return Optional.of("★ +12 do maksymalnej liczby kumulacji Animuszu");
        }
        if (!hasGreaterMarker(segment)) {
            return Optional.of(segment);
        }
        return Optional.of("★ " + stripLeadingGreaterMarker(segment));
    }

    static List<String> splitKnownTemperingSegments(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }
        Matcher matcher = MAX_ANIMUS_LINE_PATTERN.matcher(line);
        if (!matcher.find()) {
            return List.of(line);
        }
        List<String> segments = new ArrayList<>();
        addIfNotBlank(segments, stripBoundaryMarkers(line.substring(0, matcher.start(1))));
        addIfNotBlank(segments, stripTrailingBoundaryMarkers(matcher.group(1).trim()));
        addIfNotBlank(segments, stripBoundaryMarkers(line.substring(matcher.end(1))));
        return segments.isEmpty() ? List.of(line) : List.copyOf(segments);
    }

    private Optional<ItemTemperingAffix> parseKnownTemperingLine(String line, Long itemPower) {
        Optional<TemperingAffixDefinition> definition = findCatalogDefinition(line, registry);
        if (definition.isEmpty()) {
            return Optional.empty();
        }
        String normalizedLine = normalize(line);
        String normalizedName = normalize(definition.get().getDisplayName());
        if (!normalizedLine.contains(normalizedName)) {
            return Optional.empty();
        }
        Optional<Double> value = parseLeadingValue(line);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        TemperingAffixDefinition resolvedDefinition = definition.get();
        boolean displayedPerfectedValue = isDisplayedPerfectedMaxAnimus(line);
        double storedValue = displayedPerfectedValue ? resolvedDefinition.greaterAffixValue() : value.get();
        if (!displayedPerfectedValue && isRejectedTemperingValue(line, storedValue, resolvedDefinition)) {
            ItemImportDebugTrace.log("TEMPERING_CANDIDATE", () -> "decision=rejected"
                    + " definitionId=" + ItemImportDebugTrace.quote(resolvedDefinition.getId())
                    + " selectedCandidate=false"
                    + " value=" + storedValue
                    + " sourceLine=" + ItemImportDebugTrace.compactText(line)
                    + " reason=" + ItemImportDebugTrace.quote("value outside tempering catalog range"));
            return Optional.empty();
        }
        boolean greaterAffix = displayedPerfectedValue || isGreaterAffix(line, itemPower, storedValue, resolvedDefinition);
        ItemTemperingAffix affix = new ItemTemperingAffix(
                resolvedDefinition.getId(),
                resolvedDefinition.getCategory(),
                storedValue,
                formatDisplayText(storedValue, resolvedDefinition),
                stripLeadingGreaterMarker(line),
                TemperingRuntimeStatus.DATA_ONLY,
                greaterAffix
        );
        ItemImportDebugTrace.log("TEMPERING_CANDIDATE", () -> "decision=selected"
                + " definitionId=" + ItemImportDebugTrace.quote(resolvedDefinition.getId())
                + " selectedCandidate=true"
                + " value=" + storedValue
                + " sourceLine=" + ItemImportDebugTrace.compactText(line)
                + " displayText=" + ItemImportDebugTrace.compactText(affix.getDisplayText()));
        return Optional.of(affix);
    }

    private static boolean isRejectedTemperingValue(String line,
                                                    double value,
                                                    TemperingAffixDefinition definition) {
        if (definition.accepts(value)) {
            return false;
        }
        if (sameValue(value, definition.greaterAffixValue())) {
            return false;
        }
        if (definition.getRangeMin() == 0.0d
                && definition.getRangeMax() == 0.0d
                && definition.getUnit() == krys.tempering.TemperingValueUnit.PERCENT) {
            return hasMultiDigitDecimalPrefix(line);
        }
        return true;
    }

    private static boolean hasMultiDigitDecimalPrefix(String line) {
        Matcher matcher = LEADING_VALUE_PATTERN.matcher(stripLeadingGreaterMarker(line == null ? "" : line));
        if (!matcher.find()) {
            return false;
        }
        String token = matcher.group(1).replace(',', '.');
        int dot = token.indexOf('.');
        return dot > 1;
    }

    Optional<ItemTemperingAffix> parseCatalogTemperingLine(String line, Long itemPower) {
        return parseKnownTemperingLine(line, itemPower);
    }

    private static Optional<TemperingAffixDefinition> findCatalogDefinition(String line) {
        return findCatalogDefinition(line, ApplicationTemperingAffixRegistry.get());
    }

    private static Optional<TemperingAffixDefinition> findCatalogDefinition(String line, TemperingAffixRegistry registry) {
        String normalizedLine = normalize(stripLeadingGreaterMarker(line == null ? "" : line));
        if (normalizedLine.isBlank()) {
            return Optional.empty();
        }
        Optional<TemperingAffixDefinition> maxAnimus = registry.findById(MAX_ANIMUS_DEFINITION_ID)
                .filter(definition -> normalizedLine.contains(normalize(definition.getDisplayName())));
        if (maxAnimus.isPresent()) {
            return maxAnimus;
        }
        return registry.all().stream()
                .filter(definition -> normalizedLine.contains(normalize(definition.getDisplayName())))
                .findFirst();
    }

    private static String formatDisplayText(double value, TemperingAffixDefinition definition) {
        String valueText = formatFlexibleValue(value);
        if (definition.getUnit() == krys.tempering.TemperingValueUnit.PERCENT) {
            return "+" + valueText + "% " + definition.getDisplayName();
        }
        return "+" + valueText + " " + definition.getDisplayName();
    }

    private static boolean isGreaterAffix(String line,
                                          Long itemPower,
                                          double value,
                                          TemperingAffixDefinition definition) {
        boolean equalsGreaterValue = sameValue(value, definition.greaterAffixValue());
        if (hasGreaterMarker(line) && equalsGreaterValue) {
            return true;
        }
        return itemPower != null
                && itemPower == 900L
                && equalsGreaterValue
                && !definition.accepts(value);
    }

    private static Optional<Double> parseLeadingValue(String line) {
        Matcher matcher = LEADING_VALUE_PATTERN.matcher(stripLeadingGreaterMarker(line == null ? "" : line));
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(matcher.group(1).replace(',', '.')));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static boolean isDisplayedPerfectedMaxAnimus(String line) {
        Optional<Double> value = parseLeadingValue(line);
        if (value.isEmpty()) {
            return false;
        }
        String normalizedLine = normalize(line);
        return sameValue(value.get(), 12.0d)
                && normalizedLine.contains("MAKSYMALNEJ LICZBY KUMULACJI ANIMUSZU");
    }

    private static boolean hasGreaterMarker(String line) {
        String trimmed = line == null ? "" : line.trim();
        return !trimmed.isBlank() && GREATER_MARKERS.indexOf(trimmed.charAt(0)) >= 0;
    }

    private static String stripLeadingGreaterMarker(String line) {
        String trimmed = line == null ? "" : line.trim();
        while (!trimmed.isBlank() && GREATER_MARKERS.indexOf(trimmed.charAt(0)) >= 0) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed;
    }

    private static String stripBoundaryMarkers(String line) {
        String value = line == null ? "" : line.trim();
        value = value.replaceAll("^[\\s\\Q" + GREATER_MARKERS + "\\E:;|/\\\\-]+", "").trim();
        value = value.replaceAll("[\\s\\Q" + GREATER_MARKERS + "\\E:;|/\\\\-]+$", "").trim();
        return value;
    }

    private static String stripTrailingBoundaryMarkers(String line) {
        String value = line == null ? "" : line.trim();
        value = value.replaceAll("[\\s\\Q" + GREATER_MARKERS + "\\E:;|/\\\\-]+$", "").trim();
        return value;
    }

    private static void addIfNotBlank(List<String> segments, String value) {
        if (value != null && !value.isBlank()) {
            segments.add(value);
        }
    }

    private static boolean sameValue(double left, double right) {
        return Math.abs(left - right) < 0.0000001d;
    }

    private static String formatFlexibleValue(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "")
                .replace('.', ',');
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }
}
