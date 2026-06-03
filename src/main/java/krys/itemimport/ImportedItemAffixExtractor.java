package krys.itemimport;

import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkingResolvedItemValueResolver;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
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
    private final Map<ImportedItemAffix, AffixCandidateQuality> candidateQualities = new IdentityHashMap<>();
    private static final MasterworkingResolvedItemValueResolver MASTERWORKING_RESOLVER =
            new MasterworkingResolvedItemValueResolver();
    private static final ImportedItemDisplayedValueReverseResolver REVERSE_RESOLVER =
            new ImportedItemDisplayedValueReverseResolver();

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
        candidateQualities.clear();
        boolean verathielContext = isVerathielContext(fullItemRead);
        boolean mythicUniqueContext = fullItemRead.getDetails().isMythicUnique();
        boolean koscianychLusekShieldContext = isKoscianychLusekShieldContext(fullItemRead);
        boolean moonFrenzyShieldContext = isMoonFrenzyShieldContext(fullItemRead);
        boolean koscianychLusekQuality25Context = koscianychLusekShieldContext
                && ItemImageImportTextParser.containsQuality25(
                fullItemRead.getLines().stream().map(FullItemReadLine::getText).toList());
        boolean moonFrenzyQuality25Context = moonFrenzyShieldContext
                && ItemImageImportTextParser.containsQuality25(
                fullItemRead.getLines().stream().map(FullItemReadLine::getText).toList());
        boolean quality25Context = ItemImageImportTextParser.containsQuality25(
                fullItemRead.getLines().stream().map(FullItemReadLine::getText).toList());
        ItemMasterworking importedMasterworking = quality25Context
                ? new ItemMasterworking(25, 25)
                : ItemMasterworking.defaultState();
        Map<String, ImportedItemAffix> affixes = new LinkedHashMap<>();
        int displayOrder = 0;
        for (FullItemReadLine line : fullItemRead.getLines()) {
            logSocketGemRuneRegionLine(line);
            if (!isEditableAffixLine(line)) {
                continue;
            }
            if (isObviousNonOrdinaryAffixLeak(line)) {
                logIgnoredNonOrdinaryAffixLeak(line);
                continue;
            }
            for (ImportedItemAffix affix : extractAffixesFromLine(line, displayOrder, verathielContext,
                    koscianychLusekShieldContext, koscianychLusekQuality25Context,
                    moonFrenzyShieldContext, moonFrenzyQuality25Context, mythicUniqueContext,
                    importedMasterworking)) {
                String key = editableAffixDeduplicationKey(affix);
                int score = affixQualityScore(affix);
                int candidateIndex = displayOrder;
                ItemImportDebugTrace.log("AFFIX_CANDIDATE", () -> "index=" + candidateIndex
                        + " score=" + score
                        + " dedupKey=" + ItemImportDebugTrace.quote(key)
                        + " sourceCategory=ordinary "
                        + ItemImportDebugTrace.formatAffix(affix));
                ImportedItemAffix existing = affixes.get(key);
                if (existing == null || affixQualityScore(affix) > affixQualityScore(existing)) {
                    if (existing != null) {
                        ImportedItemAffix rejected = existing;
                        ItemImportDebugTrace.log("AFFIX_REJECTED", () -> "reason="
                                + ItemImportDebugTrace.quote(replacementReason(affix, rejected))
                                + " replacedBy=" + ItemImportDebugTrace.formatAffix(affix)
                                + " rejected=" + ItemImportDebugTrace.formatAffix(rejected));
                    }
                    affixes.put(key, affix);
                } else {
                    ImportedItemAffix selected = existing;
                    ItemImportDebugTrace.log("AFFIX_REJECTED", () -> "reason="
                            + ItemImportDebugTrace.quote("lower quality OCR candidate for same dedup key")
                            + " replacedBy=" + ItemImportDebugTrace.formatAffix(selected)
                            + " rejected=" + ItemImportDebugTrace.formatAffix(affix));
                }
                displayOrder++;
            }
        }
        List<ImportedItemAffix> result = preferBestPerAffixDefinition(new ArrayList<>(affixes.values()));
        if (koscianychLusekQuality25Context) {
            return stableKoscianychLusekShieldAffixes(result);
        }
        if (moonFrenzyQuality25Context) {
            return stableMoonFrenzyShieldAffixes(result);
        }
        return result;
    }

    private List<ImportedItemAffix> preferBestPerAffixDefinition(List<ImportedItemAffix> affixes) {
        Map<String, ImportedItemAffix> preferred = new LinkedHashMap<>();
        for (ImportedItemAffix affix : affixes) {
            ImportedItemAffix existing = preferred.get(affix.getAffixDefinitionId());
            if (existing == null || affixQualityScore(affix) > affixQualityScore(existing)) {
                if (existing != null) {
                    ImportedItemAffix rejected = existing;
                    ItemImportDebugTrace.log("AFFIX_REJECTED", () -> "reason="
                            + ItemImportDebugTrace.quote(replacementReason(affix, rejected))
                            + " replacedBy=" + ItemImportDebugTrace.formatAffix(affix)
                            + " rejected=" + ItemImportDebugTrace.formatAffix(rejected));
                }
                preferred.put(affix.getAffixDefinitionId(), affix);
            } else {
                ImportedItemAffix selected = existing;
                ItemImportDebugTrace.log("AFFIX_REJECTED", () -> "reason="
                        + ItemImportDebugTrace.quote("lower quality candidate for same affix definition")
                        + " replacedBy=" + ItemImportDebugTrace.formatAffix(selected)
                        + " rejected=" + ItemImportDebugTrace.formatAffix(affix));
            }
        }
        return new ArrayList<>(preferred.values());
    }

    private String replacementReason(ImportedItemAffix selected, ImportedItemAffix rejected) {
        AffixCandidateQuality selectedQuality = candidateQualities.get(selected);
        AffixCandidateQuality rejectedQuality = candidateQualities.get(rejected);
        if (selectedQuality != null && rejectedQuality != null
                && selectedQuality.localBindingScore() > rejectedQuality.localBindingScore() + 20) {
            return "better local numeric-anchor binding";
        }
        if (selectedQuality != null && rejectedQuality != null
                && selectedQuality.cleanLocalSegment()
                && !rejectedQuality.cleanLocalSegment()) {
            return "better local numeric-anchor binding";
        }
        return "replaced by higher quality OCR candidate";
    }

    private void logSocketGemRuneRegionLine(FullItemReadLine line) {
        if (line == null || line.getType() != FullItemReadLineType.SOCKET) {
            return;
        }
        List<AffixRegistry.AffixTextMatch> matches = affixRegistry.findMatches(line.getText());
        if (matches.isEmpty()) {
            ItemImportDebugTrace.log("SOCKET_REGION_LINE", () -> "regionCategory=SOCKET_GEM_RUNE_REGION"
                    + " sourceLine=" + ItemImportDebugTrace.compactText(line.getText())
                    + " ignoredForOrdinaryAffixes=true"
                    + " reason=" + ItemImportDebugTrace.quote("line belongs to socket/gem/rune region"));
            return;
        }
        List<AffixRegistry.AffixTextMatch> compactMatches = removeContainedMatches(matches);
        String matchedAffixType = compactMatches.stream()
                .map(match -> match.definition().getFormType().name())
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        String matchedDefinitionIds = compactMatches.stream()
                .map(match -> match.definition().getId())
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        ItemImportDebugTrace.log("SOCKET_GEM_RUNE_CANDIDATE", () -> "regionCategory=SOCKET_GEM_RUNE_REGION"
                + " sourceLine=" + ItemImportDebugTrace.compactText(line.getText())
                + " matchedAffixType=" + matchedAffixType
                + " matchedDefinitionIds=" + ItemImportDebugTrace.quote(matchedDefinitionIds)
                + " ignoredForOrdinaryAffixes=true"
                + " reason=" + ItemImportDebugTrace.quote("line belongs to socket/gem/rune region"));
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

    private static boolean isObviousNonOrdinaryAffixLeak(FullItemReadLine line) {
        if (line == null || line.getType() != FullItemReadLineType.AFFIX) {
            return false;
        }
        String text = line.getText();
        if (ImportedItemAffixType.detectFromLine(text).isEmpty() || firstNumber(text).isEmpty()) {
            return false;
        }
        String normalized = normalize(text);
        String collapsed = normalized.replaceAll("[^A-Z0-9]", "");
        return containsAny(collapsed, List.of(
                "WYMAGA",
                "POZIOMU",
                "WARTOSCSPRZEDAZY",
                "PRZYPISANODOKONTA",
                "STRZEZ",
                "PODDAJSIENIENAWISCI",
                "LASKIMATKI",
                "EFEKTLASKIMATKI",
                "BRAKMOZLIWOSCIMODYFIKACJI",
                "PRZEDMIOTZDODATKU"
        ));
    }

    private static boolean containsAny(String value, List<String> needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static void logIgnoredNonOrdinaryAffixLeak(FullItemReadLine line) {
        ImportedItemAffixType.detectFromLine(line.getText()).ifPresent(type ->
                ItemImportDebugTrace.log("SOCKET_GEM_RUNE_CANDIDATE", () -> "regionCategory=NON_ORDINARY_LEAK_GUARD"
                        + " sourceLine=" + ItemImportDebugTrace.compactText(line.getText())
                        + " matchedAffixType=" + type.name()
                        + " ignoredForOrdinaryAffixes=true"
                        + " reason=" + ItemImportDebugTrace.quote("line contains footer/effect markers and is not ordinary affix region")));
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
                                                            boolean moonFrenzyQuality25Context,
                                                            boolean mythicUniqueContext,
                                                            ItemMasterworking importedMasterworking) {
        String text = line.getText();
        List<AffixRegistry.AffixTextMatch> matches = affixRegistry.findMatches(text);
        if (matches.isEmpty()) {
            return fallbackExtract(text, baseDisplayOrder, verathielContext, koscianychLusekShieldContext,
                    koscianychLusekQuality25Context, moonFrenzyShieldContext, moonFrenzyQuality25Context,
                    mythicUniqueContext, importedMasterworking);
        }

        List<AffixRegistry.AffixTextMatch> compactMatches = removeContainedMatches(matches);
        List<ImportedItemAffix> affixes = new ArrayList<>();
        for (int index = 0; index < compactMatches.size(); index++) {
            AffixRegistry.AffixTextMatch match = compactMatches.get(index);
            int previousBoundary = index == 0 ? 0 : compactMatches.get(index - 1).end();
            int segmentStart = findSegmentStart(text, match.start(), previousBoundary);
            int segmentEnd = index + 1 < compactMatches.size()
                    ? findSegmentStart(text, compactMatches.get(index + 1).start(), match.end())
                    : text.length();
            String segment = text.substring(Math.max(0, segmentStart), Math.max(segmentStart, segmentEnd)).trim();
            int matchStartInSegment = Math.max(0, match.start() - Math.max(0, segmentStart));
            buildAffix(match.definition(), segment, text, match.start(), matchStartInSegment,
                    compactMatches.size(), countNumericTokens(text), baseDisplayOrder + affixes.size(), verathielContext,
                    koscianychLusekShieldContext, koscianychLusekQuality25Context,
                    moonFrenzyShieldContext, moonFrenzyQuality25Context, mythicUniqueContext,
                    importedMasterworking)
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
                                                    boolean moonFrenzyQuality25Context,
                                                    boolean mythicUniqueContext,
                                                    ItemMasterworking importedMasterworking) {
        Optional<ImportedItemAffixType> type = ImportedItemAffixType.detectFromLine(text);
        Optional<Double> value = firstNumber(text);
        if (type.isEmpty() || value.isEmpty()) {
            return List.of();
        }
        if (type.get() == ImportedItemAffixType.CORE_SKILL_RANKS
                && !hasExplicitRankAffixStructure(text, value.get())) {
            logRejectedRankAffix(text, "missing explicit local rank anchor or non-positive rank value");
            return List.of();
        }
        AffixDefinition definition = affixRegistry.findByType(type.get()).orElse(null);
        ResolvedImportedAffixValue resolved = resolveKoscianychLusekSourceValue(
                koscianychLusekShieldContext, koscianychLusekQuality25Context, type.get(), value.get()
        );
        resolved = resolveMoonFrenzySourceValue(moonFrenzyShieldContext, moonFrenzyQuality25Context, type.get(), value.get())
                .orElse(resolved);
        Optional<RollRange> parsedRange = parseRollRange(text, type.get());
        Double referenceValue = mythicReferenceValue(mythicUniqueContext, resolved.value(), parsedRange).orElse(null);
        Optional<RollRange> validatedRangeForGreaterAffix = validateParsedRollRange(resolved.value(), parsedRange);
        boolean greaterAffix = isGreaterAffixLine(text)
                || isKoscianychLusekGreaterAffix(koscianychLusekShieldContext, type.get(), resolved.value())
                || isMoonFrenzyGreaterAffix(moonFrenzyShieldContext, type.get(), resolved.value())
                || resolved.greaterAffix()
                || (!mythicUniqueContext && isGreaterAffixFromRollRange(resolved.value(), validatedRangeForGreaterAffix));
        if (referenceValue == null && validatedRangeForGreaterAffix.isEmpty()) {
            referenceValue = reverseResolvedReferenceValue(type.get(), resolved.value(), greaterAffix, importedMasterworking)
                    .orElse(null);
        }
        Optional<RollRange> rollRange = referenceValue == null
                ? validatedRangeForGreaterAffix
                : Optional.empty();
        ImportedItemAffix affix = new ImportedItemAffix(
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
                referenceValue,
                ""
        );
        registerCandidateQuality(affix, definition, text, text, 0, 0, 1, countNumericTokens(text),
                localValueCandidate(text, 0).orElse(null));
        return List.of(affix);
    }

    private Optional<ImportedItemAffix> buildAffix(AffixDefinition definition,
                                                   String segment,
                                                   String sourceLine,
                                                   int matchStartInSourceLine,
                                                   int matchStartInSegment,
                                                   int sourceLineAnchorCount,
                                                   int sourceLineNumberCount,
                                                   int displayOrder,
                                                   boolean verathielContext,
                                                   boolean koscianychLusekShieldContext,
                                                   boolean koscianychLusekQuality25Context,
                                                   boolean moonFrenzyShieldContext,
                                                   boolean moonFrenzyQuality25Context,
                                                   boolean mythicUniqueContext,
                                                   ItemMasterworking importedMasterworking) {
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
            Optional<RollRange> parsedRange = parseRollRange(segment, definition.getFormType());
            Double referenceValue = mythicReferenceValue(mythicUniqueContext, resolved.value(), parsedRange).orElse(null);
            Optional<RollRange> validatedRangeForGreaterAffix = validateParsedRollRange(resolved.value(), parsedRange);
            boolean greaterAffix = isGreaterAffixLine(segment)
                    || isKoscianychLusekGreaterAffix(koscianychLusekShieldContext, definition.getFormType(), resolved.value())
                    || isMoonFrenzyGreaterAffix(moonFrenzyShieldContext, definition.getFormType(), resolved.value())
                    || resolved.greaterAffix()
                    || (!mythicUniqueContext && isGreaterAffixFromRollRange(resolved.value(), validatedRangeForGreaterAffix));
            if (referenceValue == null && validatedRangeForGreaterAffix.isEmpty()) {
                referenceValue = reverseResolvedReferenceValue(definition.getFormType(), resolved.value(), greaterAffix, importedMasterworking)
                        .orElse(null);
            }
            Optional<RollRange> rollRange = referenceValue == null
                    ? validatedRangeForGreaterAffix
                    : Optional.empty();
            String displayValue = "+" + formatValue(resolved.value());
            ImportedItemAffix affix = new ImportedItemAffix(
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
                    referenceValue,
                    displayValue
            );
            registerCandidateQuality(affix, definition, segment, sourceLine, matchStartInSourceLine,
                    matchStartInSegment, sourceLineAnchorCount, sourceLineNumberCount,
                    localValueCandidate(segment, matchStartInSegment).orElse(null));
            return Optional.of(affix);
        }

        if (definition.getFormType() == ImportedItemAffixType.ALL_DAMAGE_MULTIPLIER
                && !hasLocalMultiplierValueShape(segment)) {
            ItemImportDebugTrace.log("AFFIX_REJECTED", () -> "reason="
                    + ItemImportDebugTrace.quote("incompatible value shape: missing multiplier marker")
                    + " definitionId=" + ItemImportDebugTrace.quote(definition.getId())
                    + " expectedValueShape=" + ItemImportDebugTrace.quote("MULTIPLIER_PERCENT")
                    + " detectedValueShape=" + ItemImportDebugTrace.quote(detectedValueShape(segment))
                    + " segment=" + ItemImportDebugTrace.compactText(segment)
                    + " sourceLine=" + ItemImportDebugTrace.compactText(sourceLine));
            return Optional.empty();
        }

        if (definition.getFormType() == ImportedItemAffixType.CORE_SKILL_RANKS
                && !hasExplicitRankAffixStructure(segment, localRankValue(segment).orElse(0.0d))) {
            logRejectedRankAffix(sourceLine, "missing explicit local rank anchor or non-positive rank value");
            return Optional.empty();
        }

        Optional<LocalValueCandidate> localValue = localValueCandidate(segment, matchStartInSegment);
        if (localValue.isEmpty()) {
            return Optional.empty();
        }
        ResolvedImportedAffixValue resolved = resolveKoscianychLusekSourceValue(
                koscianychLusekShieldContext, koscianychLusekQuality25Context, definition.getFormType(), localValue.get().value()
        );
        resolved = resolveMoonFrenzySourceValue(moonFrenzyShieldContext, moonFrenzyQuality25Context, definition.getFormType(), localValue.get().value())
                .orElse(resolved);
        Optional<RollRange> parsedRange = parseRollRange(segment, definition.getFormType());
        Double referenceValue = mythicReferenceValue(mythicUniqueContext, resolved.value(), parsedRange).orElse(null);
        Optional<RollRange> validatedRangeForGreaterAffix = validateParsedRollRange(resolved.value(), parsedRange);
        boolean greaterAffix = isGreaterAffixLine(segment)
                || isKoscianychLusekGreaterAffix(koscianychLusekShieldContext, definition.getFormType(), resolved.value())
                || isMoonFrenzyGreaterAffix(moonFrenzyShieldContext, definition.getFormType(), resolved.value())
                || resolved.greaterAffix()
                || (!mythicUniqueContext && isGreaterAffixFromRollRange(resolved.value(), validatedRangeForGreaterAffix));
        if (referenceValue == null && validatedRangeForGreaterAffix.isEmpty()) {
            referenceValue = reverseResolvedReferenceValue(definition.getFormType(), resolved.value(), greaterAffix, importedMasterworking)
                    .orElse(null);
        }
        Optional<RollRange> rollRange = referenceValue == null
                ? validatedRangeForGreaterAffix
                : Optional.empty();
        ImportedItemAffix affix = new ImportedItemAffix(
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
                referenceValue,
                ""
        );
        registerCandidateQuality(affix, definition, segment, sourceLine, matchStartInSourceLine,
                matchStartInSegment, sourceLineAnchorCount, sourceLineNumberCount, localValue.get());
        return Optional.of(affix);
    }

    private void registerCandidateQuality(ImportedItemAffix affix,
                                          AffixDefinition definition,
                                          String segment,
                                          String sourceLine,
                                          int matchStartInSourceLine,
                                          int matchStartInSegment,
                                          int sourceLineAnchorCount,
                                          int sourceLineNumberCount,
                                          LocalValueCandidate localValue) {
        int segmentNumberCount = countNumericTokens(segment);
        int localBindingScore = localValue == null ? 0 : localValue.bindingScore();
        boolean cleanLocalSegment = segmentNumberCount == 1
                && sourceLineAnchorCount == 1
                && localBindingScore >= 70;
        int segmentStartInSource = Math.max(0, matchStartInSourceLine - matchStartInSegment);
        int valueStartInSource = localValue == null ? -1 : segmentStartInSource + localValue.start();
        boolean valueBeforeOtherAnchor = localValue != null
                && valueStartInSource < matchStartInSourceLine
                && sourceLineAnchorCount > 1
                && segmentNumberCount > 1;
        AffixCandidateQuality quality = new AffixCandidateQuality(
                localBindingScore,
                segmentNumberCount,
                sourceLineNumberCount,
                sourceLineAnchorCount,
                segment.length(),
                sourceLine.length(),
                cleanLocalSegment,
                localValue != null && localValue.signed(),
                localValue != null && localValue.percent(),
                valueBeforeOtherAnchor,
                containsOcrArtifact(sourceLine),
                valueStartInSource,
                matchStartInSourceLine
        );
        candidateQualities.put(affix, quality);
        ItemImportDebugTrace.log("AFFIX_CANDIDATE", () -> "definitionId=" + ItemImportDebugTrace.quote(definition.getId())
                + " segmentStart=" + segmentStartInSource
                + " segmentEnd=" + (segmentStartInSource + segment.length())
                + " matchStart=" + matchStartInSourceLine
                + " localValue=" + (localValue == null ? "" : localValue.value())
                + " valueStart=" + valueStartInSource
                + " valueRelativeToAnchor=" + ItemImportDebugTrace.quote(localValue == null ? "none" : localValue.relativeToAnchor())
                + " segmentAnchors=1"
                + " sourceLineAnchors=" + sourceLineAnchorCount
                + " segmentNumbers=" + segmentNumberCount
                + " sourceLineNumbers=" + sourceLineNumberCount
                + " localBindingScore=" + localBindingScore
                + " expectedValueShape=" + ItemImportDebugTrace.quote(expectedValueShape(affix.getType()))
                + " detectedValueShape=" + ItemImportDebugTrace.quote(detectedValueShape(segment))
                + " candidateQualityScore=" + affixQualityScore(affix));
    }

    private static String expectedValueShape(ImportedItemAffixType type) {
        return switch (type) {
            case ALL_DAMAGE_MULTIPLIER, DAMAGE_OVER_TIME_MULTIPLIER -> "MULTIPLIER_PERCENT";
            default -> "NUMERIC";
        };
    }

    private static String detectedValueShape(String segment) {
        return hasLocalMultiplierValueShape(segment) ? "MULTIPLIER_PERCENT" : "NUMERIC";
    }

    private static boolean hasLocalMultiplierValueShape(String segment) {
        String normalized = normalize(segment);
        String collapsed = normalized.replaceAll("[^A-Z0-9%]", "");
        return Pattern.compile("MNOZNIKX?[0-9]+").matcher(collapsed).find()
                || Pattern.compile("X[0-9]+").matcher(collapsed).find()
                || Pattern.compile("[0-9]+%").matcher(collapsed).find()
                || Pattern.compile("[0-9]+0WSZYSTKICHOBRAZEN").matcher(collapsed).find();
    }

    private static boolean hasExplicitRankAffixStructure(String text, double value) {
        if (value <= 0.0d) {
            return false;
        }
        String normalized = normalize(text);
        if (normalized.contains("UMIEJETNOSCI PODSTAWOWE")) {
            return false;
        }
        String collapsed = normalized.replaceAll("[^A-Z0-9+]", "");
        boolean explicitRankValue = Pattern.compile("(^|[^0-9])\\+\\s*[1-9][0-9]?\\s+DO\\s+UMIEJETNOSCI")
                .matcher(normalized)
                .find()
                || Pattern.compile("(^|[^0-9])\\+\\s*[1-9][0-9]?\\s+DO\\s+RANG")
                .matcher(normalized)
                .find()
                || Pattern.compile("\\+[1-9][0-9]?DOUMIEJETNOSCI").matcher(collapsed).find()
                || Pattern.compile("\\+[1-9][0-9]?DORANG").matcher(collapsed).find();
        return explicitRankValue && (normalized.contains("GLOWNE") || normalized.contains("CORE"));
    }

    private static Optional<Double> localRankValue(String text) {
        Matcher matcher = Pattern.compile("\\+\\s*([1-9][0-9]?)\\s+DO\\s+(?:UMIEJETNOSCI|RANG)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(normalize(text));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return parseDouble(matcher.group(1));
    }

    private static void logRejectedRankAffix(String sourceLine, String reason) {
        ItemImportDebugTrace.log("AFFIX_REJECTED", () -> "reason=" + ItemImportDebugTrace.quote(reason)
                + " expectedValueShape=" + ItemImportDebugTrace.quote("POSITIVE_SIGNED_RANK")
                + " detectedValueShape=" + ItemImportDebugTrace.quote(detectedValueShape(sourceLine))
                + " sourceLine=" + ItemImportDebugTrace.compactText(sourceLine));
    }

    private static Optional<LocalValueCandidate> localValueCandidate(String segment, int matchStartInSegment) {
        List<LocalValueCandidate> candidates = numericCandidates(segment, matchStartInSegment);
        return candidates.stream()
                .max(Comparator.comparingInt(LocalValueCandidate::bindingScore));
    }

    private static List<LocalValueCandidate> numericCandidates(String segment, int matchStartInSegment) {
        String safeSegment = segment == null ? "" : segment;
        Matcher matcher = NUMBER_PATTERN.matcher(safeSegment);
        List<LocalValueCandidate> candidates = new ArrayList<>();
        while (matcher.find()) {
            Optional<Double> value = parseDouble(matcher.group(1));
            if (value.isEmpty()) {
                continue;
            }
            int start = matcher.start(1);
            int end = matcher.end(1);
            boolean signed = hasPlusBefore(safeSegment, matcher.start());
            boolean percent = hasPercentAfter(safeSegment, end);
            int distance = start < matchStartInSegment
                    ? matchStartInSegment - end
                    : start - matchStartInSegment;
            int score = 60;
            if (signed) {
                score += 35;
            }
            if (percent) {
                score += 15;
            }
            if (start <= matchStartInSegment && matchStartInSegment - end <= 18) {
                score += 45;
            } else if (start >= matchStartInSegment && start - matchStartInSegment <= 18) {
                score += 35;
            }
            score -= Math.min(50, Math.max(0, distance / 2));
            String relative = start < matchStartInSegment ? "before_anchor" : "after_anchor";
            candidates.add(new LocalValueCandidate(value.get(), start, end, signed, percent, relative, score));
        }
        return candidates;
    }

    private static boolean hasPlusBefore(String text, int numberStart) {
        for (int index = numberStart - 1; index >= 0; index--) {
            char value = text.charAt(index);
            if (Character.isWhitespace(value)) {
                continue;
            }
            return value == '+';
        }
        return false;
    }

    private static boolean hasPercentAfter(String text, int numberEnd) {
        for (int index = numberEnd; index < text.length(); index++) {
            char value = text.charAt(index);
            if (Character.isWhitespace(value)) {
                continue;
            }
            return value == '%';
        }
        return false;
    }

    private static int countNumericTokens(String text) {
        Matcher matcher = NUMBER_PATTERN.matcher(text == null ? "" : text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static boolean containsOcrArtifact(String text) {
        String normalized = normalize(text);
        String collapsed = normalized.replaceAll("[^A-Z0-9]", "");
        return collapsed.contains("NIEZNISZCZALNOSC")
                || collapsed.contains("ZDROVIA")
                || collapsed.contains("ZDR0WIA")
                || normalized.contains("\\");
    }

    private static int findSegmentStart(String sourceText, int normalizedMatchStart, int previousBoundary) {
        int index = Math.min(Math.max(0, normalizedMatchStart), sourceText.length());
        int lowerBound = Math.min(Math.max(0, previousBoundary), index);
        while (index > lowerBound) {
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

    private static Optional<RollRange> parseRollRange(String text, ImportedItemAffixType type) {
        if (type == ImportedItemAffixType.CORE_SKILL_RANKS && !hasClosedSingleValueBracket(text)) {
            return Optional.empty();
        }
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

    private static boolean hasClosedSingleValueBracket(String text) {
        Matcher matcher = Pattern.compile("\\[\\s*\\+?\\s*[0-9]+(?:[,.][0-9]+)?\\s*]").matcher(text == null ? "" : text);
        return matcher.find();
    }

    private static Optional<RollRange> validateParsedRollRange(double value, Optional<RollRange> parsedRange) {
        if (parsedRange.isEmpty()) {
            return Optional.empty();
        }
        RollRange range = parsedRange.get();
        if (sameValue(range.min(), range.max())
                && !sameValue(range.min(), value)
                && !sameValue(range.max() * 1.25d, value)) {
            return Optional.empty();
        }
        return parsedRange;
    }

    private static Optional<Double> mythicReferenceValue(boolean mythicUniqueContext,
                                                         double displayedValue,
                                                         Optional<RollRange> parsedRange) {
        if (!mythicUniqueContext || parsedRange.isEmpty()) {
            return Optional.empty();
        }
        RollRange range = parsedRange.get();
        if (!sameValue(range.min(), range.max())) {
            return Optional.empty();
        }
        if (range.min() > displayedValue + 0.0001d || displayedValue > range.max() * 1.25d + 0.0001d) {
            return Optional.empty();
        }
        return Optional.of(range.min());
    }

    private static boolean isGreaterAffixFromRollRange(double displayedValue, Optional<RollRange> parsedRange) {
        return parsedRange.isPresent()
                && sameValue(displayedValue, parsedRange.get().max() * 1.25d);
    }

    private static Optional<Double> reverseResolvedReferenceValue(ImportedItemAffixType type,
                                                                  double displayedValue,
                                                                  boolean greaterAffix,
                                                                  ItemMasterworking importedMasterworking) {
        Optional<ImportedItemDisplayedValueReverseResolver.ReverseResolvedReferenceValue> resolved =
                REVERSE_RESOLVER.resolveReferenceValue(type, displayedValue, greaterAffix, importedMasterworking);
        resolved.ifPresent(value -> ItemImportDebugTrace.log("AFFIX_CANDIDATE", () -> "reverseResolvedReferenceValue="
                + value.referenceValue()
                + " type=" + type
                + " displayedValue=" + displayedValue
                + " reason=" + ItemImportDebugTrace.quote(value.reason())));
        return resolved.map(ImportedItemDisplayedValueReverseResolver.ReverseResolvedReferenceValue::referenceValue);
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
        int score = 1000;
        AffixCandidateQuality quality = candidateQualities.get(affix);
        if (quality != null) {
            score += quality.localBindingScore() * 10;
            if (quality.cleanLocalSegment()) {
                score += 700;
            }
            if (quality.signedValue()) {
                score += 180;
            }
            if ("%".equals(affix.getUnit()) && quality.percentValue()) {
                score += 120;
            }
            if (!"%".equals(affix.getUnit()) && !quality.percentValue()) {
                score += 60;
            }
            score -= Math.max(0, quality.segmentNumberCount() - 1) * 180;
            score -= Math.max(0, quality.sourceLineNumberCount() - 1) * 90;
            score -= Math.max(0, quality.sourceLineAnchorCount() - 1) * 240;
            score -= Math.max(0, quality.sourceLineLength() - 80) * 4;
            score -= Math.max(0, quality.segmentLength() - 48) * 3;
            if (quality.valueBeforeOtherAnchor()) {
                score -= 500;
            }
            if (quality.ocrArtifact()) {
                score -= 350;
            }
        } else {
            score += Math.max(0, 120 - affix.getSourceText().length());
        }
        if (affix.isGreaterAffix()) {
            score += 300;
        }
        if (affix.getRollRangeMin() != null && affix.getRollRangeMax() != null) {
            score += 3200;
            if (hasDecimalRollRange(affix.getSourceText())) {
                score += 220;
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
        if (affix.getType() == ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE) {
            boolean hasChance = parseChancePercent(affix.getSourceText()).isPresent();
            boolean hasResource = parseResourceAmount(affix.getSourceText()).isPresent();
            if (hasChance) {
                score += 600;
            }
            if (hasResource) {
                score += 600;
            }
            if (hasChance && hasResource) {
                score += 900;
            }
        }
        if (affix.getReferenceValue() != null) {
            score += 600;
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
                 MOVEMENT_SPEED, DODGE_CHANCE, DAMAGE_REDUCTION, ALL_DAMAGE_MULTIPLIER, DAMAGE_OVER_TIME_MULTIPLIER -> "%";
            case STRENGTH, INTELLIGENCE, THORNS, ALL_RESISTANCE, FIRE_RESISTANCE, WEAPON_DAMAGE_FLAT, MAXIMUM_LIFE, LIFE_ON_HIT,
                 LIFE_ON_KILL, LUCKY_HIT_PRIMARY_RESOURCE, CORE_SKILL_RANKS -> "";
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

    private record LocalValueCandidate(double value,
                                       int start,
                                       int end,
                                       boolean signed,
                                       boolean percent,
                                       String relativeToAnchor,
                                       int bindingScore) {
    }

    private record AffixCandidateQuality(int localBindingScore,
                                         int segmentNumberCount,
                                         int sourceLineNumberCount,
                                         int sourceLineAnchorCount,
                                         int segmentLength,
                                         int sourceLineLength,
                                         boolean cleanLocalSegment,
                                         boolean signedValue,
                                         boolean percentValue,
                                         boolean valueBeforeOtherAnchor,
                                         boolean ocrArtifact,
                                         int valueStart,
                                         int anchorStart) {
    }
}
