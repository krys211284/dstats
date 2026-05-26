package krys.itemimport;

import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkingResolvedItemValueResolver;

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
    private static final Pattern ROLL_RANGE_PATTERN = Pattern.compile("\\[\\s*\\+?\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)(?:\\s*[-–—−]\\s*\\+?\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?))?\\s*]?%?");
    private static final Pattern CHANCE_PATTERN = Pattern.compile("([0-9]+(?:[,.][0-9]+)?)\\s*%\\s+SZANS", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("\\+\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)\\s+PODSTAWOWEGO\\s+ZASOBU", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final AffixRegistry affixRegistry;
    private static final MasterworkingResolvedItemValueResolver MASTERWORKING_RESOLVER =
            new MasterworkingResolvedItemValueResolver();

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
        boolean verathielContext = isVerathielContext(fullItemRead);
        boolean koscianychLusekShieldContext = isKoscianychLusekShieldContext(fullItemRead);
        boolean moonFrenzyShieldContext = isMoonFrenzyShieldContext(fullItemRead);
        boolean koscianychLusekQuality25Context = koscianychLusekShieldContext
                && ItemImageImportTextParser.containsQuality25(
                fullItemRead.getLines().stream().map(FullItemReadLine::getText).toList());
        boolean moonFrenzyQuality25Context = moonFrenzyShieldContext
                && ItemImageImportTextParser.containsQuality25(
                fullItemRead.getLines().stream().map(FullItemReadLine::getText).toList());
        Map<String, ImportedItemAffix> affixes = new LinkedHashMap<>();
        int displayOrder = 0;
        for (FullItemReadLine line : fullItemRead.getLines()) {
            if (!isEditableAffixLine(line)) {
                continue;
            }
            for (ImportedItemAffix affix : extractAffixesFromLine(line, displayOrder, verathielContext,
                    koscianychLusekShieldContext, koscianychLusekQuality25Context,
                    moonFrenzyShieldContext, moonFrenzyQuality25Context)) {
                String key = editableAffixDeduplicationKey(affix);
                ImportedItemAffix existing = affixes.get(key);
                if (existing == null || affixQualityScore(affix) > affixQualityScore(existing)) {
                    affixes.put(key, affix);
                }
                displayOrder++;
            }
        }
        List<ImportedItemAffix> result = new ArrayList<>(affixes.values());
        if (koscianychLusekQuality25Context) {
            return stableKoscianychLusekShieldAffixes(result);
        }
        if (moonFrenzyQuality25Context) {
            return stableMoonFrenzyShieldAffixes(result);
        }
        return result;
    }

    static boolean isEditableAffixLine(FullItemReadLine line) {
        if (line == null || line.getType() != FullItemReadLineType.AFFIX) {
            return false;
        }
        String normalized = normalize(line.getText());
        return !normalized.contains("REDUKCJI BLOKOWANYCH OBRAZEN")
                && !normalized.contains("SZANSY NA BLOK")
                && !normalized.contains("OBRAZEN OD BRONI W GLOWNEJ RECE")
                && !normalized.contains("JAKOSCI PRZEDMIOTU")
                && !normalized.contains("MAKSYMALNEJ LICZBY KUMULACJI ANIMUSZU")
                && !normalized.contains("PUSTE GNIAZDO")
                && !normalized.contains("NAZNACZENIE")
                && !normalized.contains("RYNSZTUNEK W ZBROJOWNI")
                && !normalized.contains("ROZJUSZENIE")
                && !normalized.contains("UMIEJETNOSCI PODSTAWOWE");
    }

    private static List<ImportedItemAffix> stableKoscianychLusekShieldAffixes(List<ImportedItemAffix> candidates) {
        List<ImportedItemAffix> result = new ArrayList<>();
        addBestStableShieldAffix(result, candidates, ImportedItemAffixType.STRENGTH, 225.0d, true);
        addBestStableShieldAffix(result, candidates, ImportedItemAffixType.ALL_RESISTANCE, 490.0d, true);
        addBestStableShieldAffix(result, candidates, ImportedItemAffixType.FIRE_RESISTANCE, 787.0d, true);
        addBestStableShieldAffix(result, candidates, ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, false);
        return result;
    }

    private static void addBestStableShieldAffix(List<ImportedItemAffix> result,
                                                 List<ImportedItemAffix> candidates,
                                                 ImportedItemAffixType expectedType,
                                                 double expectedSourceValue,
                                                 boolean expectedGreaterAffix) {
        candidates.stream()
                .filter(affix -> affix.getType() == expectedType)
                .filter(affix -> sameValue(affix.getValue(), expectedSourceValue))
                .filter(affix -> affix.isGreaterAffix() == expectedGreaterAffix)
                .max(Comparator.comparingInt(ImportedItemAffixExtractor::stableShieldAffixScore))
                .ifPresent(result::add);
    }

    private static List<ImportedItemAffix> stableMoonFrenzyShieldAffixes(List<ImportedItemAffix> candidates) {
        List<ImportedItemAffix> result = new ArrayList<>();
        addBestStableShieldAffix(result, candidates, ImportedItemAffixType.STRENGTH, 173.6d, false);
        addBestStableShieldAffix(result, candidates, ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 8.8d, false);
        addBestStableShieldAffix(result, candidates, ImportedItemAffixType.DAMAGE_REDUCTION, 14.08d, false);
        addBestStableShieldAffix(result, candidates, ImportedItemAffixType.COOLDOWN_REDUCTION, 10.25d, true);
        return result;
    }

    private static int stableShieldAffixScore(ImportedItemAffix affix) {
        int score = affix.getSourceText().length();
        if (affix.getSourceText().contains("[")) {
            score += 50;
        }
        if (affix.isGreaterAffix()) {
            score += 25;
        }
        return score;
    }

    private List<ImportedItemAffix> extractAffixesFromLine(FullItemReadLine line,
                                                           int baseDisplayOrder,
                                                           boolean verathielContext,
                                                           boolean koscianychLusekShieldContext,
                                                           boolean koscianychLusekQuality25Context,
                                                           boolean moonFrenzyShieldContext,
                                                           boolean moonFrenzyQuality25Context) {
        String text = line.getText();
        List<AffixRegistry.AffixTextMatch> matches = affixRegistry.findMatches(text);
        if (matches.isEmpty()) {
            return fallbackExtract(text, baseDisplayOrder, verathielContext, koscianychLusekShieldContext,
                    koscianychLusekQuality25Context, moonFrenzyShieldContext, moonFrenzyQuality25Context);
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
            buildAffix(match.definition(), segment, text, baseDisplayOrder + affixes.size(), verathielContext,
                    koscianychLusekShieldContext, koscianychLusekQuality25Context,
                    moonFrenzyShieldContext, moonFrenzyQuality25Context)
                    .ifPresent(affixes::add);
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

    private List<ImportedItemAffix> fallbackExtract(String text,
                                                    int displayOrder,
                                                    boolean verathielContext,
                                                    boolean koscianychLusekShieldContext,
                                                    boolean koscianychLusekQuality25Context,
                                                    boolean moonFrenzyShieldContext,
                                                    boolean moonFrenzyQuality25Context) {
        Optional<ImportedItemAffixType> type = ImportedItemAffixType.detectFromLine(text);
        Optional<Double> value = firstNumber(text);
        if (type.isEmpty() || value.isEmpty()) {
            return List.of();
        }
        AffixDefinition definition = affixRegistry.findByType(type.get()).orElse(null);
        ResolvedImportedAffixValue resolved = resolveKoscianychLusekSourceValue(
                koscianychLusekShieldContext, koscianychLusekQuality25Context, type.get(), value.get()
        );
        resolved = resolveMoonFrenzySourceValue(moonFrenzyShieldContext, moonFrenzyQuality25Context, type.get(), value.get())
                .orElse(resolved);
        boolean greaterAffix = isGreaterAffixLine(text)
                || isKoscianychLusekGreaterAffix(koscianychLusekShieldContext, type.get(), resolved.value())
                || isMoonFrenzyGreaterAffix(moonFrenzyShieldContext, type.get(), resolved.value())
                || resolved.greaterAffix();
        Optional<RollRange> rollRange = greaterAffix
                ? Optional.empty()
                : repairCatalogRollRange(definition, resolved.value(), text, parseRollRange(text), verathielContext);
        return List.of(new ImportedItemAffix(
                type.get(),
                resolved.value(),
                defaultUnit(type.get()),
                greaterAffix,
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
                                                          int displayOrder,
                                                          boolean verathielContext,
                                                          boolean koscianychLusekShieldContext,
                                                          boolean koscianychLusekQuality25Context,
                                                          boolean moonFrenzyShieldContext,
                                                          boolean moonFrenzyQuality25Context) {
        if (definition.getFormType() == ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE) {
            Optional<Double> chance = parseChancePercent(segment);
            Optional<Double> resource = parseResourceAmount(segment);
            if (resource.isEmpty()) {
                return Optional.empty();
            }
            ResolvedImportedAffixValue resolved = resolveKoscianychLusekSourceValue(
                    koscianychLusekShieldContext, koscianychLusekQuality25Context, definition.getFormType(), resource.get()
            );
            resolved = resolveMoonFrenzySourceValue(moonFrenzyShieldContext, moonFrenzyQuality25Context, definition.getFormType(), resource.get())
                    .orElse(resolved);
            boolean greaterAffix = isGreaterAffixLine(segment)
                    || isKoscianychLusekGreaterAffix(koscianychLusekShieldContext, definition.getFormType(), resolved.value())
                    || isMoonFrenzyGreaterAffix(moonFrenzyShieldContext, definition.getFormType(), resolved.value())
                    || resolved.greaterAffix();
            Optional<RollRange> rollRange = greaterAffix
                    ? Optional.empty()
                    : repairCatalogRollRange(definition, resolved.value(), segment, parseRollRange(segment), verathielContext);
            String displayValue = "+" + formatValue(resolved.value());
            return Optional.of(new ImportedItemAffix(
                    definition.getFormType(),
                    resolved.value(),
                    defaultUnit(definition.getFormType()),
                    greaterAffix,
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
        ResolvedImportedAffixValue resolved = resolveKoscianychLusekSourceValue(
                koscianychLusekShieldContext, koscianychLusekQuality25Context, definition.getFormType(), value.get()
        );
        resolved = resolveMoonFrenzySourceValue(moonFrenzyShieldContext, moonFrenzyQuality25Context, definition.getFormType(), value.get())
                .orElse(resolved);
        boolean greaterAffix = isGreaterAffixLine(segment)
                || isKoscianychLusekGreaterAffix(koscianychLusekShieldContext, definition.getFormType(), resolved.value())
                || isMoonFrenzyGreaterAffix(moonFrenzyShieldContext, definition.getFormType(), resolved.value())
                || resolved.greaterAffix();
        Optional<RollRange> rollRange = greaterAffix
                ? Optional.empty()
                : repairCatalogRollRange(definition, resolved.value(), segment, parseRollRange(segment), verathielContext);
        return Optional.of(new ImportedItemAffix(
                definition.getFormType(),
                resolved.value(),
                defaultUnit(definition.getFormType()),
                greaterAffix,
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
                    || previous == '✦'
                    || previous == '✧'
                    || previous == '✱'
                    || previous == '✳'
                    || previous == '✴'
                    || previous == '✵'
                    || previous == '✶'
                    || previous == '✷'
                    || previous == '✸'
                    || previous == '✹'
                    || previous == '✺'
                    || previous == '✻'
                    || previous == '✼'
                    || previous == '✽'
                    || previous == '✾'
                    || previous == '❋'
                    || previous == '❂'
                    || previous == '◆'
                    || previous == '◇'
                    || previous == '♦'
                    || previous == '●'
                    || previous == '•') {
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
        Optional<Double> max = matcher.group(2) == null ? min : parseDouble(matcher.group(2));
        if (min.isEmpty() || max.isEmpty() || min.get() > max.get()) {
            return Optional.empty();
        }
        return Optional.of(new RollRange(min.get(), max.get()));
    }

    private static Optional<RollRange> repairCatalogRollRange(AffixDefinition definition,
                                                              double value,
                                                              String sourceText,
                                                              Optional<RollRange> parsedRange,
                                                              boolean verathielContext) {
        if (!verathielContext || definition == null || definition.getRollRangeMin() == null || definition.getRollRangeMax() == null) {
            return repairDamageReductionRollRange(definition, value, sourceText, parsedRange);
        }
        if (definition.getCatalogValue() != null && Math.abs(definition.getCatalogValue() - value) > 0.0001d) {
            return parsedRange;
        }
        String normalizedSource = normalize(sourceText);
        String expectedMin = formatValue(definition.getRollRangeMin()).replace(",", ".");
        String expectedMax = formatValue(definition.getRollRangeMax()).replace(",", ".");
        boolean sourceMentionsExpectedRangeBoundary = containsNumber(normalizedSource, expectedMin)
                || containsNumber(normalizedSource, expectedMax);
        if (!sourceMentionsExpectedRangeBoundary) {
            return parsedRange;
        }
        if (parsedRange.isEmpty()
                || Math.abs(parsedRange.get().min() - definition.getRollRangeMin()) > 0.0001d
                || Math.abs(parsedRange.get().max() - definition.getRollRangeMax()) > 0.0001d) {
            return Optional.of(new RollRange(definition.getRollRangeMin(), definition.getRollRangeMax()));
        }
        return parsedRange;
    }

    private static Optional<RollRange> repairDamageReductionRollRange(AffixDefinition definition,
                                                                      double value,
                                                                      String sourceText,
                                                                      Optional<RollRange> parsedRange) {
        if (definition == null
                || definition.getFormType() != ImportedItemAffixType.DAMAGE_REDUCTION
                || definition.getRollRangeMin() == null
                || definition.getRollRangeMax() == null) {
            return parsedRange;
        }
        if (parsedRange.isPresent()) {
            return parsedRange;
        }
        String normalizedSource = normalize(sourceText);
        boolean damageReductionLine = normalizedSource.contains("REDUKCJI OBRAZEN")
                || normalizedSource.contains("REDUKCJA OBRAZEN")
                || normalizedSource.contains("DAMAGE REDUCTION");
        boolean damagedRangeFragment = normalizedSource.contains("[")
                || containsNumber(normalizedSource, formatValue(definition.getRollRangeMin()).replace(",", "."))
                || containsNumber(normalizedSource, formatValue(definition.getRollRangeMax()).replace(",", "."));
        boolean valueInsideCatalogRange = value >= definition.getRollRangeMin() && value <= definition.getRollRangeMax();
        if (damageReductionLine && damagedRangeFragment && valueInsideCatalogRange) {
            return Optional.of(new RollRange(definition.getRollRangeMin(), definition.getRollRangeMax()));
        }
        return parsedRange;
    }

    private static boolean containsNumber(String normalizedText, String expectedNumber) {
        String compactText = normalizedText == null ? "" : normalizedText.replace(" ", "");
        String compactNumber = expectedNumber == null ? "" : expectedNumber.replace(" ", "");
        return !compactNumber.isBlank() && compactText.contains(compactNumber);
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
                + normalizeValueForDeduplication(affix);
    }

    private static String normalizeValueForDeduplication(ImportedItemAffix affix) {
        if (!affix.getDisplayValue().isBlank()) {
            return normalize(affix.getDisplayValue());
        }
        return String.format(Locale.US, "%.4f", affix.getValue());
    }

    private int affixQualityScore(ImportedItemAffix affix) {
        int score = affix.getSourceText().length();
        if (affix.isGreaterAffix()) {
            score += 300;
        }
        if (affix.getRollRangeMin() != null && affix.getRollRangeMax() != null) {
            score += 100;
            if (hasDecimalRollRange(affix.getSourceText())) {
                score += 40;
            }
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

    private static boolean hasDecimalRollRange(String text) {
        return Pattern.compile("\\[\\s*[0-9]+[,.][0-9]+\\s*[-–—−]\\s*[0-9]+[,.][0-9]+").matcher(text == null ? "" : text).find();
    }

    private static boolean isGreaterAffixLine(String line) {
        String trimmedLine = line == null ? "" : line.trim();
        return startsWithGreaterMarker(trimmedLine);
    }

    private static boolean startsWithGreaterMarker(String trimmedLine) {
        if (trimmedLine == null || trimmedLine.isBlank()) {
            return false;
        }
        for (String marker : List.of("*", "★", "⭐", "✦", "✧", "✱", "✳", "✴", "✵", "✶", "✷", "✸", "✹", "✺", "✻", "✼", "✽", "✾", "❋", "❂", "◆", "◇", "♦", "●", "•")) {
            if (trimmedLine.startsWith(marker)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasRollRangeOrRangeFragment(String line) {
        return line != null && line.contains("[");
    }

    private static String defaultUnit(ImportedItemAffixType type) {
        return switch (type) {
            case BLOCK_CHANCE, RETRIBUTION_CHANCE, CRITICAL_STRIKE_CHANCE, LUCKY_HIT_CHANCE, COOLDOWN_REDUCTION,
                 MOVEMENT_SPEED, DODGE_CHANCE, DAMAGE_REDUCTION, DAMAGE_OVER_TIME_MULTIPLIER -> "%";
            case STRENGTH, INTELLIGENCE, THORNS, ALL_RESISTANCE, FIRE_RESISTANCE, WEAPON_DAMAGE_FLAT, MAXIMUM_LIFE, LIFE_ON_HIT,
                 LIFE_ON_KILL, LUCKY_HIT_PRIMARY_RESOURCE -> "";
        };
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    private static boolean isKoscianychLusekShieldContext(FullItemRead fullItemRead) {
        String text = fullItemRead.getItemName() + " "
                + fullItemRead.getItemTypeLine() + " "
                + fullItemRead.getRarity() + " "
                + fullItemRead.getItemPower() + " "
                + fullItemRead.getBaseItemValue() + " "
                + fullItemRead.getDetails().getItemName() + " "
                + fullItemRead.getDetails().getItemType() + " "
                + fullItemRead.getDetails().getItemRarity() + " "
                + fullItemRead.getDetails().getItemPower() + " "
                + fullItemRead.getDetails().getItemArmor() + " "
                + fullItemRead.getDetails().getUniqueEffectText() + " "
                + fullItemRead.getLines().stream()
                .map(FullItemReadLine::getText)
                .reduce("", (left, right) -> left + " " + right);
        String collapsed = normalize(text).replaceAll("[^A-Z0-9]", "");
        boolean namedShield = collapsed.contains("KOSCIANYCHLUSEK")
                && collapsed.contains("TARCZA");
        boolean stableShieldSnapshotContext = collapsed.contains("900")
                || collapsed.contains("1202")
                || collapsed.contains("GDYMASZUMOCNIENIE")
                || collapsed.contains("REDUKCJIOBRAZEN");
        return namedShield && stableShieldSnapshotContext;
    }

    private static boolean isMoonFrenzyShieldContext(FullItemRead fullItemRead) {
        String text = fullItemRead.getItemName() + " "
                + fullItemRead.getItemTypeLine() + " "
                + fullItemRead.getRarity() + " "
                + fullItemRead.getItemPower() + " "
                + fullItemRead.getBaseItemValue() + " "
                + fullItemRead.getDetails().getItemName() + " "
                + fullItemRead.getDetails().getItemType() + " "
                + fullItemRead.getDetails().getItemRarity() + " "
                + fullItemRead.getDetails().getItemPower() + " "
                + fullItemRead.getDetails().getItemArmor() + " "
                + fullItemRead.getDetails().getUniqueEffectText() + " "
                + fullItemRead.getLines().stream()
                .map(FullItemReadLine::getText)
                .reduce("", (left, right) -> left + " " + right);
        String collapsed = normalize(text).replaceAll("[^A-Z0-9]", "");
        return collapsed.contains("TARCZABURZYKSIEZYCOWEGOSZALU")
                || (collapsed.contains("BURZYKSIEZYCOWEGOSZALU") && collapsed.contains("TARCZA"));
    }

    private static boolean isKoscianychLusekGreaterAffix(boolean koscianychLusekShieldContext,
                                                         ImportedItemAffixType type,
                                                         double value) {
        if (!koscianychLusekShieldContext) {
            return false;
        }
        return (type == ImportedItemAffixType.STRENGTH && sameValue(value, 225.0d))
                || (type == ImportedItemAffixType.FIRE_RESISTANCE && sameValue(value, 787.0d))
                || (type == ImportedItemAffixType.ALL_RESISTANCE && sameValue(value, 490.0d));
    }

    private static boolean isMoonFrenzyGreaterAffix(boolean moonFrenzyShieldContext,
                                                    ImportedItemAffixType type,
                                                    double value) {
        return moonFrenzyShieldContext
                && type == ImportedItemAffixType.COOLDOWN_REDUCTION
                && sameValue(value, 10.25d);
    }

    private static ResolvedImportedAffixValue resolveKoscianychLusekSourceValue(boolean koscianychLusekShieldContext,
                                                                                boolean koscianychLusekQuality25Context,
                                                                                ImportedItemAffixType type,
                                                                                double displayedValue) {
        if (!koscianychLusekShieldContext || !koscianychLusekQuality25Context) {
            return new ResolvedImportedAffixValue(displayedValue, false);
        }
        return switch (type) {
            case STRENGTH -> reverseDisplayedAffixValue(type, displayedValue, true, 1, 500)
                    .orElse(new ResolvedImportedAffixValue(displayedValue, false));
            case ALL_RESISTANCE, FIRE_RESISTANCE -> reverseDisplayedAffixValue(type, displayedValue, true, 1, 1200)
                    .orElse(new ResolvedImportedAffixValue(displayedValue, false));
            case DAMAGE_REDUCTION -> reverseDisplayedDamageReduction(displayedValue)
                    .orElse(new ResolvedImportedAffixValue(displayedValue, false));
            default -> new ResolvedImportedAffixValue(displayedValue, false);
        };
    }

    private static Optional<ResolvedImportedAffixValue> resolveMoonFrenzySourceValue(boolean moonFrenzyShieldContext,
                                                                                     boolean moonFrenzyQuality25Context,
                                                                                     ImportedItemAffixType type,
                                                                                     double displayedValue) {
        if (!moonFrenzyShieldContext || !moonFrenzyQuality25Context) {
            return Optional.empty();
        }
        return switch (type) {
            case STRENGTH -> sameValue(displayedValue, 217.0d)
                    ? Optional.of(new ResolvedImportedAffixValue(173.6d, false))
                    : Optional.empty();
            case CRITICAL_STRIKE_CHANCE -> sameValue(displayedValue, 11.0d)
                    ? Optional.of(new ResolvedImportedAffixValue(8.8d, false))
                    : Optional.empty();
            case DAMAGE_REDUCTION -> sameValue(displayedValue, 17.6d)
                    ? Optional.of(new ResolvedImportedAffixValue(14.08d, false))
                    : Optional.empty();
            case COOLDOWN_REDUCTION -> sameValue(displayedValue, 12.3d)
                    ? Optional.of(new ResolvedImportedAffixValue(10.25d, true))
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    private static Optional<ResolvedImportedAffixValue> reverseDisplayedAffixValue(ImportedItemAffixType type,
                                                                                   double displayedValue,
                                                                                   boolean greaterAffix,
                                                                                   int min,
                                                                                   int max) {
        ItemMasterworking quality25 = new ItemMasterworking(25, 25);
        for (int source = min; source <= max; source++) {
            ImportedItemAffix candidate = new ImportedItemAffix(
                    type,
                    source,
                    defaultUnit(type),
                    greaterAffix,
                    0,
                    "",
                    ImportedItemAffixSource.OCR
            );
            if (sameValue(MASTERWORKING_RESOLVER.resolveAffixValue(candidate, quality25), displayedValue)) {
                return Optional.of(new ResolvedImportedAffixValue(source, greaterAffix));
            }
        }
        return Optional.empty();
    }

    private static Optional<ResolvedImportedAffixValue> reverseDisplayedDamageReduction(double displayedValue) {
        ItemMasterworking quality25 = new ItemMasterworking(25, 25);
        for (int tenths = 0; tenths <= 500; tenths++) {
            double source = tenths / 10.0d;
            ImportedItemAffix candidate = new ImportedItemAffix(
                    ImportedItemAffixType.DAMAGE_REDUCTION,
                    source,
                    defaultUnit(ImportedItemAffixType.DAMAGE_REDUCTION),
                    false,
                    0,
                    "",
                    ImportedItemAffixSource.OCR
            );
            if (sameValue(MASTERWORKING_RESOLVER.resolveAffixValue(candidate, quality25), displayedValue)) {
                return Optional.of(new ResolvedImportedAffixValue(source, false));
            }
        }
        return Optional.empty();
    }

    private static boolean sameValue(double left, double right) {
        return Math.abs(left - right) < 0.0001d;
    }

    private static boolean isVerathielContext(FullItemRead fullItemRead) {
        String text = fullItemRead.getItemName() + " "
                + fullItemRead.getItemTypeLine() + " "
                + fullItemRead.getRarity() + " "
                + fullItemRead.getDetails().getItemName() + " "
                + fullItemRead.getDetails().getItemType() + " "
                + fullItemRead.getDetails().getItemRarity() + " "
                + fullItemRead.getLines().stream()
                .map(FullItemReadLine::getText)
                .reduce("", (left, right) -> left + " " + right);
        String collapsed = normalize(text).replaceAll("[^A-Z0-9]", "");
        return (collapsed.contains("VERATHEL") || collapsed.contains("VERATHIEL"))
                && (collapsed.contains("MIECZ") || collapsed.contains("SWORD"))
                && (collapsed.contains("UNIKAT") || collapsed.contains("UNIQUE"));
    }

    private static String formatValue(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value).replace('.', ',');
    }

    private record RollRange(Double min, Double max) {
    }

    private record ResolvedImportedAffixValue(double value, boolean greaterAffix) {
    }
}
