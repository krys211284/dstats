package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.socketing.ItemSocketing;
import krys.tempering.ItemTemperingAffix;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixCatalog;
import krys.transfiguration.TransfigurationAffixDefinition;
import krys.transfiguration.TransfigurationAffixRoll;
import krys.transfiguration.TransfigurationValueProvenance;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Buduje formularz ręcznego potwierdzenia z wstępnie rozpoznanych pól. */
public final class ItemImportEditableFormFactory {
    private final ImportedItemAffixExtractor affixExtractor = new ImportedItemAffixExtractor();
    private final ImportedItemTemperingExtractor temperingExtractor = new ImportedItemTemperingExtractor();
    private final AspectRegistry aspectRegistry;

    public ItemImportEditableFormFactory() {
        this(ApplicationAspectRegistry.get());
    }

    ItemImportEditableFormFactory(AspectRegistry aspectRegistry) {
        this.aspectRegistry = aspectRegistry;
    }

    public ItemImportEditableForm create(ItemImageImportCandidateParseResult parseResult) {
        try (ItemImportDebugTrace.Scope ignored = ItemImportDebugTrace.withMetadata(parseResult.getImageMetadata())) {
            ItemImportDraft draft = createDraft(parseResult);
            ItemImportDetails details = detailsWithCanonicalAspectEffect(parseResult, draft);
            ItemImportEditableForm form = new ItemImportEditableForm(
                    parseResult.getImageMetadata().getOriginalFilename(),
                    toSlotValue(parseResult.getSlotCandidate().getSuggestedValue() == null
                            ? parseResult.getFullItemRead().getDetails().getEquipmentSlot()
                            : parseResult.getSlotCandidate().getSuggestedValue()),
                    "0",
                    toDoubleValue(parseResult.getStrengthCandidate().getSuggestedValue()),
                    toDoubleValue(parseResult.getIntelligenceCandidate().getSuggestedValue()),
                    toDoubleValue(parseResult.getThornsCandidate().getSuggestedValue()),
                    toDoubleValue(parseResult.getBlockChanceCandidate().getSuggestedValue()),
                    toDoubleValue(parseResult.getRetributionChanceCandidate().getSuggestedValue()),
                    parseResult.getFullItemRead(),
                    draft.getAffixes(),
                    draft.getOcrSuggestedAspectId(),
                    draft.getOcrAspectConfidence(),
                    draft.getOcrSuggestedAspectId(),
                    details,
                    draft.getTemperingAffixes(),
                    draft.getMasterworking(),
                    draft.getTransfiguration(),
                    draft.getSocketing()
            );
            ItemImportDebugTrace.log("FINAL_IMPORT_FORM", () -> ItemImportDebugTrace.formatForm(form)
                    + " " + ItemImportDebugTrace.formatDetails(form.getDetails()));
            ItemImportDebugTrace.logAffixList("FINAL_IMPORT_FORM", form.getAffixes());
            for (int index = 0; index < form.getTemperingAffixes().size(); index++) {
                int finalIndex = index;
                ItemTemperingAffix affix = form.getTemperingAffixes().get(index);
                ItemImportDebugTrace.log("FINAL_IMPORT_FORM", () -> "temperingIndex=" + finalIndex
                        + " sourceCategory=tempering "
                        + ItemImportDebugTrace.formatTempering(affix));
            }
            return form;
        }
    }

    public ItemImportDraft createDraft(ItemImageImportCandidateParseResult parseResult) {
        AspectRegistry.AspectMatch aspectMatch = aspectRegistry.suggestFromFullRead(parseResult.getFullItemRead())
                .orElse(new AspectRegistry.AspectMatch("", ItemImportFieldConfidence.UNKNOWN));
        EquipmentSlot effectiveSlot = parseResult.getSlotCandidate().getSuggestedValue() == null
                ? parseResult.getFullItemRead().getDetails().getEquipmentSlot()
                : parseResult.getSlotCandidate().getSuggestedValue();
        if (!aspectMatch.aspectId().isBlank()
                && aspectRegistry.findById(aspectMatch.aspectId())
                .filter(aspect -> aspect.allowsSlot(effectiveSlot))
                .isEmpty()) {
            aspectMatch = new AspectRegistry.AspectMatch("", ItemImportFieldConfidence.UNKNOWN);
        }
        List<ImportedItemAffix> affixes = affixExtractor.extractEditableAffixes(parseResult.getFullItemRead());
        List<ItemTemperingAffix> temperingAffixes = temperingExtractor.extractTemperingAffixes(parseResult.getFullItemRead());
        ItemMasterworking masterworking = detectMasterworking(parseResult.getFullItemRead(), temperingAffixes);
        ItemTransfiguration transfiguration = detectTransfiguration(parseResult.getFullItemRead());
        ItemSocketing socketing = detectSocketing(parseResult.getFullItemRead());
        return new ItemImportDraft(
                parseResult,
                aspectMatch.aspectId(),
                aspectMatch.confidence(),
                affixes,
                temperingAffixes,
                masterworking,
                transfiguration,
                socketing
        );
    }

    private ItemImportDetails detailsWithCanonicalAspectEffect(ItemImageImportCandidateParseResult parseResult,
                                                               ItemImportDraft draft) {
        ItemImportDetails details = parseResult.getFullItemRead().getDetails();
        if (draft == null || draft.getOcrSuggestedAspectId().isBlank()) {
            return details;
        }
        Optional<AspectDefinition> definition = aspectRegistry.findById(draft.getOcrSuggestedAspectId());
        if (definition.isEmpty()) {
            return details;
        }
        AspectDefinition aspect = definition.get();
        String canonicalEffectText = canonicalEffectText(aspect, parseResult.getFullItemRead());
        ItemImportDebugTrace.log("ASPECT_MATCH", () -> "selectedAspectId=" + ItemImportDebugTrace.quote(aspect.getId())
                + " runtimeStatus=" + aspect.getRuntimeStatus()
                + " canonicalEffectText=" + ItemImportDebugTrace.compactText(canonicalEffectText)
                + " ocrEffectText=" + ItemImportDebugTrace.compactText(details.getUniqueEffectText()));
        return new ItemImportDetails(
                details.getItemName(),
                details.getItemType(),
                details.getItemRarity(),
                details.isAncient(),
                details.getEquipmentSlot(),
                details.getItemPower(),
                details.getWeaponDps(),
                details.getWeaponDamageMin(),
                details.getWeaponDamageMax(),
                details.getAverageWeaponDamage(),
                details.getAttacksPerSecond(),
                details.getItemArmor(),
                canonicalEffectText,
                details.isMythicUnique()
        );
    }

    private static String canonicalEffectText(AspectDefinition aspect, FullItemRead fullItemRead) {
        String canonical = EffectTextTokenNormalizer.normalizeMultiplierTokens(aspect.getEffectDescription());
        Optional<String> ocrMultiplier = extractOcrMultiplierToken(fullItemRead);
        if (ocrMultiplier.isPresent() && canonical.contains("X%[x]")) {
            return canonical.replace("X%[x]", ocrMultiplier.get());
        }
        return canonical;
    }

    private static Optional<String> extractOcrMultiplierToken(FullItemRead fullItemRead) {
        if (fullItemRead == null) {
            return Optional.empty();
        }
        String joined = EffectTextTokenNormalizer.normalizeMultiplierTokens(
                fullItemRead.getDetails().getUniqueEffectText()
                        + " "
                        + fullItemRead.getLines().stream()
                        .map(FullItemReadLine::getText)
                        .reduce("", (left, right) -> left + " " + right)
        );
        Matcher matcher = Pattern.compile("([0-9]+(?:[,.][0-9]+)?%\\[x])").matcher(joined);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static ItemSocketing detectSocketing(FullItemRead fullItemRead) {
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return ItemSocketing.empty();
        }
        boolean hasEmptySocket = fullItemRead.getLines().stream()
                .anyMatch(line -> line.getType() == FullItemReadLineType.SOCKET
                        && normalize(line.getText()).equals("PUSTE GNIAZDO"));
        return hasEmptySocket ? ItemSocketing.emptySockets(1) : ItemSocketing.empty();
    }

    private static ItemMasterworking detectMasterworking(FullItemRead fullItemRead, List<ItemTemperingAffix> temperingAffixes) {
        if (fullItemRead == null || !ItemImageImportTextParser.containsQuality25(
                fullItemRead.getLines().stream().map(FullItemReadLine::getText).toList())) {
            return ItemMasterworking.defaultState();
        }
        MasterworkedAffixSelection perfectedAffix = null;
        if (hasDisplayedPerfectedMaxAnimus(fullItemRead, temperingAffixes)) {
            perfectedAffix = MasterworkedAffixSelection.temperingAffix("defense_max_animus");
        }
        return new ItemMasterworking(25, 25, perfectedAffix);
    }

    private static boolean hasDisplayedPerfectedMaxAnimus(FullItemRead fullItemRead, List<ItemTemperingAffix> temperingAffixes) {
        boolean hasStoredMaxAnimus = temperingAffixes.stream()
                .anyMatch(affix -> "defense_max_animus".equals(affix.getDefinitionId())
                        && affix.isGreaterAffix()
                        && Math.abs(affix.getValue() - 5.0d) < 0.0001d);
        if (!hasStoredMaxAnimus) {
            return false;
        }
        return fullItemRead.getLines().stream()
                .map(FullItemReadLine::getText)
                .anyMatch(line -> normalize(line).contains("+12 DO MAKSYMALNEJ LICZBY KUMULACJI ANIMUSZU"));
    }

    private static ItemTransfiguration detectTransfiguration(FullItemRead fullItemRead) {
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return ItemTransfiguration.none();
        }
        boolean transfigured = hasLineContaining(fullItemRead, "PRZEISTOCZONY");
        boolean locked = hasLineContaining(fullItemRead, "BRAK MOZLIWOSCI MODYFIKACJI");
        Optional<TransfigurationAffixRoll> allStatsRoll = detectTransfigurationAffixRoll(fullItemRead);
        Optional<Integer> bonusQuality = detectBonusItemQuality(fullItemRead);
        if (!transfigured && allStatsRoll.isEmpty() && bonusQuality.isEmpty()) {
            return ItemTransfiguration.none();
        }
        HoradricTransfigurationOutcome outcome;
        if (allStatsRoll.isPresent()) {
            outcome = HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX;
        } else if (bonusQuality.isPresent()) {
            outcome = HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY;
        } else {
            outcome = HoradricTransfigurationOutcome.UNKNOWN;
        }
        return new ItemTransfiguration(
                true,
                locked || transfigured,
                HoradricTuningPrism.NONE,
                outcome,
                "",
                allStatsRoll.orElse(null),
                "",
                null,
                bonusQuality.orElse(null),
                false,
                ""
        );
    }

    private static Optional<Integer> detectBonusItemQuality(FullItemRead fullItemRead) {
        Optional<Integer> fromQualityLine = detectBonusQualityFromStackedQualityLine(fullItemRead);
        if (fromQualityLine.isPresent()) {
            return fromQualityLine;
        }
        for (FullItemReadLine line : fullItemRead.getLines()) {
            Optional<Integer> direct = extractBonusItemQualityDisplayedValue(line.getText());
            if (direct.isPresent()) {
                return direct;
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> detectBonusQualityFromStackedQualityLine(FullItemRead fullItemRead) {
        for (FullItemReadLine line : fullItemRead.getLines()) {
            String normalized = normalize(line.getText());
            if (!normalized.contains("JAKOSCI")) {
                continue;
            }
            Matcher matcher = Pattern.compile("\\b([0-9]{1,2})\\s*\\([^)]*\\+\\s*([0-9]{1,2})\\s*\\)\\s+JAKOSCI").matcher(normalized);
            if (!matcher.find()) {
                continue;
            }
            int total = Integer.parseInt(matcher.group(1));
            int masterworking = Integer.parseInt(matcher.group(2));
            int bonus = total - masterworking;
            if (masterworking == 25 && bonus >= 1 && bonus <= 15) {
                return Optional.of(bonus);
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> extractBonusItemQualityDisplayedValue(String line) {
        String normalized = normalize(line);
        Matcher matcher = Pattern.compile("\\+\\s*([0-9]+)\\s*(?:DO\\s+)?JAKOSCI\\s+PRZEDMIOTU").matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int bonus = Integer.parseInt(matcher.group(1));
        return bonus >= 1 && bonus <= 15 ? Optional.of(bonus) : Optional.empty();
    }

    private static Optional<TransfigurationAffixRoll> detectTransfigurationAffixRoll(FullItemRead fullItemRead) {
        for (FullItemReadLine line : fullItemRead.getLines()) {
            for (TransfigurationAffixDefinition definition : TransfigurationAffixCatalog.definitions()) {
                Optional<ParsedTransfigurationAffixRoll> roll = extractTransfigurationDisplayedValue(line.getText(), definition);
                if (roll.isPresent()) {
                    ParsedTransfigurationAffixRoll parsed = roll.get();
                    TransfigurationAffixRoll result = new TransfigurationAffixRoll(
                            definition.getId(),
                            parsed.displayedValue(),
                            TransfigurationValueProvenance.GAME_DISPLAYED_VALUE,
                            "",
                            parsed.sourceRangeMin(),
                            parsed.sourceRangeMax()
                    );
                    ItemImportDebugTrace.log("TRANSFIGURATION_CANDIDATE", () -> "definitionId="
                            + ItemImportDebugTrace.quote(definition.getId())
                            + " displayedValue=" + parsed.displayedValue()
                            + " sourceRangeMin=" + parsed.sourceRangeMin()
                            + " sourceRangeMax=" + parsed.sourceRangeMax()
                            + " sourceLine=" + ItemImportDebugTrace.compactText(line.getText()));
                    return Optional.of(result);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<ParsedTransfigurationAffixRoll> extractTransfigurationDisplayedValue(String line,
                                                                                                 TransfigurationAffixDefinition definition) {
        String normalized = normalize(line);
        String normalizedAnchor = normalize(definition.getDisplayName());
        int anchorIndex = normalized.indexOf(normalizedAnchor);
        if (anchorIndex < 0) {
            return Optional.empty();
        }
        String prefix = normalized.substring(0, anchorIndex);
        Matcher valueMatcher = Pattern.compile("\\+\\s*([0-9]+(?:[,.][0-9]+)?)").matcher(prefix);
        Double displayedValue = null;
        while (valueMatcher.find()) {
            displayedValue = Double.parseDouble(valueMatcher.group(1).replace(',', '.'));
        }
        if (displayedValue == null) {
            return Optional.empty();
        }
        Double sourceRangeMin = null;
        Double sourceRangeMax = null;
        String suffix = normalized.substring(anchorIndex + normalizedAnchor.length());
        Matcher rangeMatcher = Pattern.compile("\\[?\\s*\\+?\\s*1?([0-9]{1,3}(?:[,.][0-9]+)?)\\s*[-–—−]\\s*([0-9]{1,3}(?:[,.][0-9]+)?)1?\\s*]?")
                .matcher(suffix);
        if (rangeMatcher.find()) {
            sourceRangeMin = Double.parseDouble(rangeMatcher.group(1).replace(',', '.'));
            sourceRangeMax = Double.parseDouble(rangeMatcher.group(2).replace(',', '.'));
            if (sourceRangeMin > sourceRangeMax) {
                sourceRangeMin = null;
                sourceRangeMax = null;
            }
        }
        return Optional.of(new ParsedTransfigurationAffixRoll(displayedValue, sourceRangeMin, sourceRangeMax));
    }

    private record ParsedTransfigurationAffixRoll(double displayedValue, Double sourceRangeMin, Double sourceRangeMax) {
    }

    private static boolean hasLineContaining(FullItemRead fullItemRead, String normalizedNeedle) {
        return fullItemRead.getLines().stream()
                .map(FullItemReadLine::getText)
                .anyMatch(line -> normalize(line).contains(normalizedNeedle));
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

    private static String toSlotValue(EquipmentSlot slot) {
        return slot == null ? "" : slot.name();
    }

    private static String toLongValue(Long value) {
        return value == null ? "" : Long.toString(value);
    }

    private static String toDoubleValue(Double value) {
        return value == null ? "" : String.format(Locale.US, "%.0f", value);
    }
}
