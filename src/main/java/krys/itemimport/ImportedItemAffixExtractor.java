package krys.itemimport;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Wyciąga edytowalne affixy z pełnego odczytu OCR itemu przez katalog affixów. */
public final class ImportedItemAffixExtractor {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)");
    private static final Pattern SIGNED_NUMBER_PATTERN = Pattern.compile("\\+\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)|\\b([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)\\s*%");
    private static final Pattern ROLL_RANGE_PATTERN = Pattern.compile("\\[\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)\\s*[-–—−]\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)\\s*]?");
    private static final Pattern CHANCE_PATTERN = Pattern.compile("([0-9]+(?:[,.][0-9]+)?)\\s*%\\s+SZANS", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("\\+\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)\\s+PODSTAWOWEGO\\s+ZASOBU", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final AffixRegistry affixRegistry;

    public ImportedItemAffixExtractor() {
        this(ApplicationAffixRegistry.get());
    }

    ImportedItemAffixExtractor(AffixRegistry affixRegistry) {
        this.affixRegistry = affixRegistry;
    }

    public List<ImportedItemAffix> extractEditableAffixes(FullItemRead fullItemRead) {
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return List.of();
        }
        Map<String, ImportedItemAffix> affixes = new LinkedHashMap<>();
        int displayOrder = 0;
        for (FullItemReadLine line : fullItemRead.getLines()) {
            if (!isEditableAffixLine(line)) {
                continue;
            }
            for (ImportedItemAffix affix : extractAffixesFromLine(line, displayOrder)) {
                String key = editableAffixDeduplicationKey(affix);
                ImportedItemAffix existing = affixes.get(key);
                if (existing == null || affixQualityScore(affix) > affixQualityScore(existing)) {
                    affixes.put(key, affix);
                }
                displayOrder++;
            }
        }
        return new ArrayList<>(affixes.values());
    }

    static boolean isEditableAffixLine(FullItemReadLine line) {
        if (line == null || line.getType() != FullItemReadLineType.AFFIX) {
            return false;
        }
        String normalized = normalize(line.getText());
        return !normalized.contains("REDUKCJI BLOKOWANYCH OBRAZEN")
                && !normalized.contains("SZANSY NA BLOK")
                && !normalized.contains("OBRAZEN OD BRONI W GLOWNEJ RECE")
                && !normalized.contains("ROZJUSZENIE")
                && !normalized.contains("UMIEJETNOSCI PODSTAWOWE");
    }

    private List<ImportedItemAffix> extractAffixesFromLine(FullItemReadLine line, int baseDisplayOrder) {
        String text = line.getText();
        List<AffixRegistry.AffixTextMatch> matches = affixRegistry.findMatches(text);
        if (matches.isEmpty()) {
            return fallbackExtract(text, baseDisplayOrder);
        }

        List<AffixRegistry.AffixTextMatch> compactMatches = removeContainedMatches(matches);
        List<ImportedItemAffix> affixes = new ArrayList<>();
        for (int index = 0; index < compactMatches.size(); index++) {
            AffixRegistry.AffixTextMatch match = compactMatches.get(index);
            int segmentStart = findSegmentStart(text, match.start());
            int segmentEnd = index + 1 < compactMatches.size()
                    ? findSegmentStart(text, compactMatches.get(index + 1).start())
                    : text.length();
            String segment = text.substring(Math.max(0, segmentStart), Math.max(segmentStart, segmentEnd)).trim();
            buildAffix(match.definition(), segment, text, baseDisplayOrder + affixes.size()).ifPresent(affixes::add);
        }
        return affixes;
    }

    private static List<AffixRegistry.AffixTextMatch> removeContainedMatches(List<AffixRegistry.AffixTextMatch> matches) {
        List<AffixRegistry.AffixTextMatch> ordered = matches.stream()
                .sorted(Comparator
                        .comparingInt(AffixRegistry.AffixTextMatch::start)
                        .thenComparing((AffixRegistry.AffixTextMatch match) -> match.end() - match.start(), Comparator.reverseOrder()))
                .toList();
        List<AffixRegistry.AffixTextMatch> result = new ArrayList<>();
        for (AffixRegistry.AffixTextMatch match : ordered) {
            boolean overlapsExisting = false;
            for (AffixRegistry.AffixTextMatch existing : result) {
                if (match.start() >= existing.start() && match.start() < existing.end()) {
                    overlapsExisting = true;
                    break;
                }
            }
            if (!overlapsExisting) {
                result.add(match);
            }
        }
        result.sort(Comparator.comparingInt(AffixRegistry.AffixTextMatch::start));
        return result;
    }

    private List<ImportedItemAffix> fallbackExtract(String text, int displayOrder) {
        Optional<ImportedItemAffixType> type = ImportedItemAffixType.detectFromLine(text);
        Optional<Double> value = firstNumber(text);
        if (type.isEmpty() || value.isEmpty()) {
            return List.of();
        }
        AffixDefinition definition = affixRegistry.findByType(type.get()).orElse(null);
        Optional<RollRange> rollRange = parseRollRange(text);
        return List.of(new ImportedItemAffix(
                type.get(),
                value.get(),
                defaultUnit(type.get()),
                isGreaterAffixLine(text),
                displayOrder,
                text,
                ImportedItemAffixSource.OCR,
                definition == null ? "" : definition.getId(),
                rollRange.map(RollRange::min).orElse(null),
                rollRange.map(RollRange::max).orElse(null),
                ""
        ));
    }

    private static Optional<ImportedItemAffix> buildAffix(AffixDefinition definition,
                                                          String segment,
                                                          String sourceLine,
                                                          int displayOrder) {
        Optional<RollRange> rollRange = parseRollRange(segment);
        if (definition.getFormType() == ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE) {
            Optional<Double> chance = parseChancePercent(segment);
            Optional<Double> resource = parseResourceAmount(segment);
            if (resource.isEmpty()) {
                return Optional.empty();
            }
            String displayValue = chance
                    .map(value -> formatValue(value) + "% / +" + formatValue(resource.get()))
                    .orElse("+" + formatValue(resource.get()));
            return Optional.of(new ImportedItemAffix(
                    definition.getFormType(),
                    resource.get(),
                    defaultUnit(definition.getFormType()),
                    isGreaterAffixLine(segment),
                    displayOrder,
                    segment.isBlank() ? sourceLine : segment,
                    ImportedItemAffixSource.OCR,
                    definition.getId(),
                    rollRange.map(RollRange::min).orElse(null),
                    rollRange.map(RollRange::max).orElse(null),
                    displayValue
            ));
        }

        Optional<Double> value = firstSignedNumber(segment);
        if (value.isEmpty()) {
            value = firstNumber(segment);
        }
        if (value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ImportedItemAffix(
                definition.getFormType(),
                value.get(),
                defaultUnit(definition.getFormType()),
                isGreaterAffixLine(segment),
                displayOrder,
                segment.isBlank() ? sourceLine : segment,
                ImportedItemAffixSource.OCR,
                definition.getId(),
                rollRange.map(RollRange::min).orElse(null),
                rollRange.map(RollRange::max).orElse(null),
                ""
        ));
    }

    private static int findSegmentStart(String sourceText, int normalizedMatchStart) {
        int index = Math.min(Math.max(0, normalizedMatchStart), sourceText.length());
        while (index > 0) {
            char previous = sourceText.charAt(index - 1);
            if (Character.isDigit(previous)
                    || Character.isWhitespace(previous)
                    || previous == '+'
                    || previous == '*'
                    || previous == ','
                    || previous == '.'
                    || previous == '%'
                    || previous == '★'
                    || previous == '⭐'
                    || previous == '✦') {
                index--;
                continue;
            }
            break;
        }
        return index;
    }

    private static Optional<Double> firstNumber(String text) {
        Matcher matcher = NUMBER_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return parseDouble(matcher.group(1));
    }

    private static Optional<Double> firstSignedNumber(String text) {
        Matcher matcher = SIGNED_NUMBER_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String token = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
        return parseDouble(token);
    }

    private static Optional<RollRange> parseRollRange(String text) {
        Matcher matcher = ROLL_RANGE_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        Optional<Double> min = parseDouble(matcher.group(1));
        Optional<Double> max = parseDouble(matcher.group(2));
        if (min.isEmpty() || max.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RollRange(min.get(), max.get()));
    }

    private static Optional<Double> parseChancePercent(String text) {
        Matcher matcher = CHANCE_PATTERN.matcher(normalize(text));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return parseDouble(matcher.group(1));
    }

    private static Optional<Double> parseResourceAmount(String text) {
        Matcher matcher = RESOURCE_PATTERN.matcher(normalize(text));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return parseDouble(matcher.group(1));
    }

    private static Optional<Double> parseDouble(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(token.replace(" ", "").replace(',', '.')));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static String editableAffixDeduplicationKey(ImportedItemAffix affix) {
        return affix.getAffixDefinitionId()
                + "|"
                + normalizeValueForDeduplication(affix)
                + "|"
                + affix.isGreaterAffix();
    }

    private static String normalizeValueForDeduplication(ImportedItemAffix affix) {
        if (!affix.getDisplayValue().isBlank()) {
            return normalize(affix.getDisplayValue());
        }
        return String.format(Locale.US, "%.4f", affix.getValue());
    }

    private int affixQualityScore(ImportedItemAffix affix) {
        int score = affix.getSourceText().length();
        if (affix.getRollRangeMin() != null && affix.getRollRangeMax() != null) {
            score += 100;
            AffixDefinition definition = affixRegistry.findById(affix.getAffixDefinitionId()).orElse(null);
            if (definition != null
                    && definition.getRollRangeMin() != null
                    && definition.getRollRangeMax() != null
                    && Math.abs(definition.getRollRangeMin() - affix.getRollRangeMin()) < 0.0001d
                    && Math.abs(definition.getRollRangeMax() - affix.getRollRangeMax()) < 0.0001d) {
                score += 100;
            }
        }
        if (affix.getSourceText().contains("[")) {
            score += 10;
        }
        return score;
    }

    private static boolean isGreaterAffixLine(String line) {
        String trimmedLine = line == null ? "" : line.trim();
        return trimmedLine.startsWith("*")
                || trimmedLine.startsWith("★")
                || trimmedLine.startsWith("⭐")
                || trimmedLine.startsWith("✦");
    }

    static boolean hasRollRangeOrRangeFragment(String line) {
        return line != null && line.contains("[");
    }

    private static String defaultUnit(ImportedItemAffixType type) {
        return switch (type) {
            case BLOCK_CHANCE, RETRIBUTION_CHANCE, LUCKY_HIT_CHANCE, COOLDOWN_REDUCTION,
                 MOVEMENT_SPEED, DODGE_CHANCE -> "%";
            case STRENGTH, INTELLIGENCE, THORNS, WEAPON_DAMAGE_FLAT, MAXIMUM_LIFE, LIFE_ON_HIT,
                 LUCKY_HIT_PRIMARY_RESOURCE -> "";
        };
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String formatValue(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value).replace('.', ',');
    }

    private record RollRange(Double min, Double max) {
    }
}
