package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.socketing.ItemSocket;
import krys.socketing.ItemSocketing;
import krys.socketing.SocketGemRuneStat;
import krys.tempering.ItemTemperingAffix;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixCatalog;
import krys.transfiguration.TransfigurationAffixDefinition;
import krys.transfiguration.TransfigurationAffixRoll;
import krys.transfiguration.TransfigurationValueProvenance;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Buduje formularz ręcznego potwierdzenia z wstępnie rozpoznanych pól. */
public final class ItemImportEditableFormFactory {
    private static final int ORDINARY_AFFIX_LIMIT = 4;

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
            ItemImportDebugTrace.log("FINAL_IMPORT_FORM", () -> "finalRenderedOrder=" + form.getAffixes().stream()
                    .map(affix -> affix.getType().name())
                    .toList());
            ItemImportDebugTrace.logAffixList("FINAL_IMPORT_FORM", form.getAffixes());
            for (int index = 0; index < form.getTemperingAffixes().size(); index++) {
                int finalIndex = index;
                ItemTemperingAffix affix = form.getTemperingAffixes().get(index);
                ItemImportDebugTrace.log("FINAL_IMPORT_FORM", () -> "temperingIndex=" + finalIndex
                        + " sourceCategory=tempering "
                        + ItemImportDebugTrace.formatTemperingForm(
                        affix,
                        form.getFullItemRead(),
                        form.getMasterworking()
                ));
            }
            for (int index = 0; index < form.getSocketing().getSockets().size(); index++) {
                int finalIndex = index;
                krys.socketing.ItemSocket socket = form.getSocketing().getSockets().get(index);
                ItemImportDebugTrace.log("FINAL_IMPORT_FORM", () -> "socketIndex=" + finalIndex
                        + " " + ItemImportDebugTrace.formatSocket(socket));
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
        List<ImportedItemAffix> extractedAffixes = affixExtractor.extractEditableAffixes(parseResult.getFullItemRead());
        OrdinaryClassification ordinaryClassification = classifyOrdinaryAndOverflow(
                extractedAffixes,
                parseResult.getFullItemRead()
        );
        List<ImportedItemAffix> affixes = ordinaryClassification.ordinaryAffixes();
        List<ItemTemperingAffix> temperingAffixes = mergeTempering(
                temperingExtractor.extractTemperingAffixes(parseResult.getFullItemRead()),
                ordinaryClassification.overflowTemperingAffixes()
        );
        GreaterAffixMarkerSummary markerSummary = greaterAffixMarkerSummary(parseResult.getFullItemRead(), affixes, temperingAffixes);
        affixes = applyGreaterAffixConfirmationState(affixes, markerSummary);
        ItemMasterworking masterworking = detectMasterworking(parseResult.getFullItemRead(), affixes, temperingAffixes, markerSummary);
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

    private OrdinaryClassification classifyOrdinaryAndOverflow(List<ImportedItemAffix> extractedAffixes,
                                                               FullItemRead fullItemRead) {
        List<ImportedItemAffix> ordinary = new ArrayList<>();
        List<ItemTemperingAffix> overflowTempering = new ArrayList<>();
        Long itemPower = fullItemRead == null ? null : fullItemRead.getDetails().getItemPower();
        List<ImportedItemAffix> affixesByVisualOrder = (extractedAffixes == null ? List.<ImportedItemAffix>of() : extractedAffixes)
                .stream()
                .sorted(java.util.Comparator
                        .comparingInt(ImportedItemAffix::getVisualDisplayOrder)
                        .thenComparingInt(ImportedItemAffix::getDisplayOrder)
                        .thenComparing(ImportedItemAffix::getAffixDefinitionId)
                        .thenComparingDouble(ImportedItemAffix::getValue))
                .toList();
        long nonTemperingCandidateCount = affixesByVisualOrder.stream()
                .filter(affix -> temperingExtractor.parseCatalogTemperingLine(affix.getSourceText(), itemPower).isEmpty())
                .count();
        boolean deferredCatalogTempering = false;
        for (ImportedItemAffix affix : affixesByVisualOrder) {
            Optional<ItemTemperingAffix> catalogTempering = temperingExtractor.parseCatalogTemperingLine(affix.getSourceText(), itemPower);
            if (catalogTempering.isPresent() && nonTemperingCandidateCount >= ORDINARY_AFFIX_LIMIT) {
                deferredCatalogTempering = true;
                ItemImportDebugTrace.log("LINE_CLASSIFICATION", () -> "decision=temperingCandidateDeferred"
                        + " selectedVisualOrder=" + affix.getDisplayOrder()
                        + " sourceLine=" + ItemImportDebugTrace.compactText(affix.getSourceText())
                        + " matchedDefinitionId=" + ItemImportDebugTrace.quote(catalogTempering.get().getDefinitionId())
                        + " reason=" + ItemImportDebugTrace.quote("catalog tempering candidate does not reserve ordinary cap while ordinary set can be filled by non-tempering affixes"));
                continue;
            }
            if (ordinary.size() < ORDINARY_AFFIX_LIMIT) {
                ordinary.add(affix);
                ItemImportDebugTrace.log("LINE_CLASSIFICATION", () -> "decision=ordinary"
                        + " ordinaryIndex=" + (ordinary.size() - 1)
                        + " selectedVisualOrder=" + affix.getDisplayOrder()
                        + " visualSourceOrder=" + affix.getVisualDisplayOrder()
                        + " selectedValueSource=" + ItemImportDebugTrace.compactText(affix.getSourceText())
                        + " visualAnchorSource=" + ItemImportDebugTrace.compactText(affix.getVisualSourceText())
                        + " reason=" + ItemImportDebugTrace.quote("within ordinary affix cap by visual anchor order"));
                continue;
            }
            if (catalogTempering.isPresent()) {
                ItemTemperingAffix selected = catalogTempering.get();
                overflowTempering.add(selected);
                ItemImportDebugTrace.log("LINE_CLASSIFICATION", () -> "decision=tempering"
                        + " selectedVisualOrder=" + affix.getDisplayOrder()
                        + " sourceLine=" + ItemImportDebugTrace.compactText(affix.getSourceText())
                        + " matchedDefinitionId=" + ItemImportDebugTrace.quote(selected.getDefinitionId())
                        + " reason=" + ItemImportDebugTrace.quote("classified as tempering after ordinary cap by visual order and tempering catalog match"));
                continue;
            }
            ItemImportDebugTrace.log("LINE_CLASSIFICATION", () -> "decision=ignored"
                    + " selectedVisualOrder=" + affix.getDisplayOrder()
                    + " sourceLine=" + ItemImportDebugTrace.compactText(affix.getSourceText())
                    + " reason=" + ItemImportDebugTrace.quote("ordinary affix cap reached and no non-ordinary catalog matched"));
        }
        if (deferredCatalogTempering) {
            overflowTempering.addAll(detectTemperingAfterOrdinarySet(fullItemRead, ordinary, overflowTempering, itemPower));
        }
        List<ImportedItemAffix> ordinaryByVisualOrder = sortOrdinaryByVisualSourceOrder(fullItemRead, ordinary);
        ItemImportDebugTrace.log("FINAL_IMPORT_FORM", () -> "ordinaryAffixes=" + ordinary.size()
                + " ordinaryAffixCap=" + ORDINARY_AFFIX_LIMIT
                + " visualOrderBeforeRender=" + ordinary.stream()
                .map(affix -> affix.getType().name() + "@" + affix.getVisualDisplayOrder() + ":" + affix.getDisplayOrder())
                .toList()
                + " finalOrdinaryVisualOrder=" + ordinaryByVisualOrder.stream()
                .map(affix -> affix.getType().name() + "@" + visualSourceLineSortIndex(fullItemRead, affix) + ":" + affix.getDisplayOrder())
                .toList()
                + " overflowTempering=" + overflowTempering.size());
        return new OrdinaryClassification(List.copyOf(ordinaryByVisualOrder), List.copyOf(overflowTempering));
    }

    private static List<ImportedItemAffix> sortOrdinaryByVisualSourceOrder(FullItemRead fullItemRead,
                                                                           List<ImportedItemAffix> ordinary) {
        return (ordinary == null ? List.<ImportedItemAffix>of() : ordinary).stream()
                .sorted(java.util.Comparator
                        .comparingInt((ImportedItemAffix affix) -> visualSourceLineSortIndex(fullItemRead, affix))
                        .thenComparingInt(ImportedItemAffix::getVisualDisplayOrder)
                        .thenComparingInt(ImportedItemAffix::getDisplayOrder)
                        .thenComparing(ImportedItemAffix::getAffixDefinitionId)
                        .thenComparingDouble(ImportedItemAffix::getValue))
                .toList();
    }

    private static int visualSourceLineSortIndex(FullItemRead fullItemRead, ImportedItemAffix affix) {
        int index = sourceLineIndex(fullItemRead, affix.getVisualSourceText());
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private List<ItemTemperingAffix> detectTemperingAfterOrdinarySet(FullItemRead fullItemRead,
                                                                     List<ImportedItemAffix> ordinary,
                                                                     List<ItemTemperingAffix> alreadyDetected,
                                                                     Long itemPower) {
        if (fullItemRead == null || ordinary == null || ordinary.isEmpty()) {
            return List.of();
        }
        int lastOrdinaryLineIndex = lastOrdinarySourceLineIndex(fullItemRead, ordinary);
        if (lastOrdinaryLineIndex < 0) {
            return List.of();
        }
        List<ItemTemperingAffix> detected = new ArrayList<>();
        for (int index = lastOrdinaryLineIndex + 1; index < fullItemRead.getLines().size(); index++) {
            FullItemReadLine line = fullItemRead.getLines().get(index);
            Optional<ItemTemperingAffix> tempering = temperingExtractor.parseCatalogTemperingLine(line.getText(), itemPower);
            if (tempering.isEmpty() || containsTemperingDefinition(alreadyDetected, tempering.get().getDefinitionId())
                    || containsTemperingDefinition(detected, tempering.get().getDefinitionId())) {
                continue;
            }
            ItemTemperingAffix selected = tempering.get();
            detected.add(selected);
            int visualOrder = index;
            ItemImportDebugTrace.log("LINE_CLASSIFICATION", () -> "decision=tempering"
                    + " selectedVisualOrder=" + visualOrder
                    + " sourceLine=" + ItemImportDebugTrace.compactText(line.getText())
                    + " matchedDefinitionId=" + ItemImportDebugTrace.quote(selected.getDefinitionId())
                    + " reason=" + ItemImportDebugTrace.quote("classified as tempering after ordinary cap by visual order and tempering catalog match"));
        }
        return List.copyOf(detected);
    }

    private static int lastOrdinarySourceLineIndex(FullItemRead fullItemRead, List<ImportedItemAffix> ordinary) {
        int lastIndex = -1;
        for (ImportedItemAffix affix : ordinary) {
            int index = sourceLineIndex(fullItemRead, affix.getVisualSourceText());
            if (index > lastIndex) {
                lastIndex = index;
            }
        }
        return lastIndex;
    }

    private static int sourceLineIndex(FullItemRead fullItemRead, String sourceText) {
        if (fullItemRead == null) {
            return -1;
        }
        String normalizedSource = normalize(sourceText);
        if (normalizedSource.isBlank()) {
            return -1;
        }
        for (int index = 0; index < fullItemRead.getLines().size(); index++) {
            String normalizedLine = normalize(fullItemRead.getLines().get(index).getText());
            if (normalizedLine.equals(normalizedSource)
                    || normalizedLine.contains(normalizedSource)
                    || normalizedSource.contains(normalizedLine)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean containsTemperingDefinition(List<ItemTemperingAffix> affixes, String definitionId) {
        return (affixes == null ? List.<ItemTemperingAffix>of() : affixes).stream()
                .anyMatch(affix -> affix.getDefinitionId().equals(definitionId));
    }

    private static List<ItemTemperingAffix> mergeTempering(List<ItemTemperingAffix> base,
                                                           List<ItemTemperingAffix> overflow) {
        List<ItemTemperingAffix> result = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (ItemTemperingAffix affix : base == null ? List.<ItemTemperingAffix>of() : base) {
            if (!seen.contains(affix.getDefinitionId())) {
                result.add(affix);
                seen.add(affix.getDefinitionId());
            }
        }
        for (ItemTemperingAffix affix : overflow == null ? List.<ItemTemperingAffix>of() : overflow) {
            if (!seen.contains(affix.getDefinitionId())) {
                result.add(affix);
                seen.add(affix.getDefinitionId());
            }
        }
        return List.copyOf(result);
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
        List<SocketGemRuneStat> detectedStats = new ArrayList<>();
        boolean indestructibleTransfiguration = hasIndestructibleLine(fullItemRead);
        for (FullItemReadLine line : fullItemRead.getLines()) {
            String text = line.getText();
            boolean typedSocketStat = line.getType() == FullItemReadLineType.SOCKET
                    && isDetectedSocketStatLine(text);
            boolean physicalSocketFallback = line.getType() != FullItemReadLineType.SOCKET
                    && indestructibleTransfiguration
                    && isPhysicalDamageMultiplierLine(normalize(text));
            if (!typedSocketStat && !physicalSocketFallback) {
                continue;
            }
            if (physicalSocketFallback) {
                ItemImportDebugTrace.log("LINE_CLASSIFICATION", () -> "decision=socketGemRune"
                        + " sourceLine=" + ItemImportDebugTrace.compactText(text)
                        + " reason=" + ItemImportDebugTrace.quote("physical damage multiplier classified as socket/gem/rune data because indestructible is the stronger transfiguration result"));
            }
            addSocketGemRuneStat(detectedStats, SocketGemRuneStat.fromDetectedLine(text));
            if (detectedStats.size() >= ItemSocketing.MAX_SOCKET_COUNT) {
                break;
            }
        }
        long rawEmptySocketCount = fullItemRead.getLines().stream()
                .filter(line -> line.getType() == FullItemReadLineType.SOCKET)
                .map(FullItemReadLine::getText)
                .filter(text -> normalize(text).equals("PUSTE GNIAZDO"))
                .count();
        int occupiedSocketCount = detectedStats.size();
        int emptySocketCount = Math.min((int) rawEmptySocketCount, Math.max(0, ItemSocketing.MAX_SOCKET_COUNT - occupiedSocketCount));
        int totalSocketCount = Math.min(ItemSocketing.MAX_SOCKET_COUNT, occupiedSocketCount + emptySocketCount);
        if (totalSocketCount <= 0) {
            return ItemSocketing.empty();
        }
        List<ItemSocket> sockets = new ArrayList<>();
        int index = 0;
        for (SocketGemRuneStat stat : detectedStats) {
            sockets.add(ItemSocket.detectedStat(index, stat));
            index++;
        }
        while (index < totalSocketCount) {
            sockets.add(ItemSocket.empty(index));
            index++;
        }
        ItemImportDebugTrace.log("FINAL_IMPORT_FORM", () -> "SOCKET_GEM_RUNE_MODEL occupiedSocketCount="
                + occupiedSocketCount
                + " emptySocketCount=" + emptySocketCount
                + " totalSocketCount=" + totalSocketCount
                + " stats=" + detectedStats.stream()
                .map(SocketGemRuneStat::getDisplayText)
                .toList());
        return new ItemSocketing(totalSocketCount, sockets);
    }

    private static void addSocketGemRuneStat(List<SocketGemRuneStat> detectedStats, SocketGemRuneStat candidate) {
        ItemImportDebugTrace.log("SOCKET_GEM_RUNE_CANDIDATE", () -> "semanticKey="
                + ItemImportDebugTrace.quote(candidate.getSemanticKey())
                + " sourceLine=" + ItemImportDebugTrace.compactText(candidate.getSourceLine())
                + " displayText=" + ItemImportDebugTrace.compactText(candidate.getDisplayText())
                + " sourceQualityScore=" + candidate.sourceQualityScore()
                + " localAnchorBinding=" + candidate.shouldDeduplicateBySemanticKey()
                + " reason=" + ItemImportDebugTrace.quote(candidate.shouldDeduplicateBySemanticKey()
                ? "physical socket multiplier value bound to local Mnożnik x...% anchor"
                : "socket/gem/rune stat parsed from local socket line"));
        for (int index = 0; index < detectedStats.size(); index++) {
            SocketGemRuneStat existing = detectedStats.get(index);
            if (!candidate.shouldDeduplicateBySemanticKey()
                    || !existing.shouldDeduplicateBySemanticKey()
                    || !existing.getSemanticKey().equals(candidate.getSemanticKey())) {
                continue;
            }
            if (candidate.sourceQualityScore() > existing.sourceQualityScore()) {
                detectedStats.set(index, candidate);
                ItemImportDebugTrace.log("SOCKET_GEM_RUNE_CANDIDATE", () -> "decision=replaceDuplicate"
                        + " reason=" + ItemImportDebugTrace.quote("duplicate semantic socket stat with cleaner source line")
                        + " semanticKey=" + ItemImportDebugTrace.quote(candidate.getSemanticKey())
                        + " selected=" + ItemImportDebugTrace.compactText(candidate.getSourceLine())
                        + " rejected=" + ItemImportDebugTrace.compactText(existing.getSourceLine()));
            } else {
                ItemImportDebugTrace.log("SOCKET_GEM_RUNE_CANDIDATE", () -> "decision=rejectDuplicate"
                        + " reason=" + ItemImportDebugTrace.quote(candidate.hasLoreTail()
                        ? "duplicate semantic socket stat / worse source line / contains lore tail"
                        : "duplicate semantic socket stat / worse source line")
                        + " semanticKey=" + ItemImportDebugTrace.quote(candidate.getSemanticKey())
                        + " selected=" + ItemImportDebugTrace.compactText(existing.getSourceLine())
                        + " rejected=" + ItemImportDebugTrace.compactText(candidate.getSourceLine()));
            }
            return;
        }
        detectedStats.add(candidate);
    }

    private static boolean isDetectedSocketStatLine(String text) {
        String normalized = normalize(text);
        return (normalized.startsWith("+") || isPhysicalDamageMultiplierLine(normalized))
                && normalized.matches(".*[0-9].*");
    }

    private static boolean isPhysicalDamageMultiplierLine(String normalized) {
        return normalized.contains("MNOZNIK")
                && normalized.contains("OBRAZEN")
                && (normalized.contains("FIZYCZNE") || normalized.contains("PHYSICAL"));
    }

    private static List<ImportedItemAffix> applyGreaterAffixConfirmationState(List<ImportedItemAffix> affixes,
                                                                              GreaterAffixMarkerSummary markerSummary) {
        if (affixes == null || affixes.isEmpty() || markerSummary == null || !markerSummary.requiresConfirmation()) {
            return affixes == null ? List.of() : affixes;
        }
        List<ImportedItemAffix> result = new ArrayList<>();
        for (ImportedItemAffix affix : affixes) {
            boolean requiresConfirmation = !affix.isGreaterAffix();
            ImportedItemAffix updated = requiresConfirmation
                    ? affix.withVisualAnchor(
                    affix.getVisualSourceText(),
                    affix.getVisualDisplayOrder(),
                    affix.isGreaterAffix(),
                    true
            )
                    : affix;
            result.add(updated);
            ItemImportDebugTrace.log("GA_MARKER_FINAL_STATE", () -> "type=" + affix.getType()
                    + " value=" + affix.getValue()
                    + " finalGreaterAffix=" + updated.isGreaterAffix()
                    + " confirmationRequired=" + updated.isGreaterAffixConfirmationRequired()
                    + " visualAnchorSource=" + ItemImportDebugTrace.compactText(updated.getVisualSourceText())
                    + " reason=" + ItemImportDebugTrace.quote(requiresConfirmation
                    ? "global GA marker remains ambiguous for this affix"
                    : "local GA marker assigned to this affix"));
        }
        return List.copyOf(result);
    }

    private static ItemMasterworking detectMasterworking(FullItemRead fullItemRead,
                                                         List<ImportedItemAffix> affixes,
                                                         List<ItemTemperingAffix> temperingAffixes,
                                                         GreaterAffixMarkerSummary markerSummary) {
        if (fullItemRead == null || !ItemImageImportTextParser.containsQuality25(
                fullItemRead.getLines().stream().map(FullItemReadLine::getText).toList())) {
            return ItemMasterworking.defaultState();
        }
        MasterworkedAffixSelection perfectedAffix = null;
        if (hasDisplayedPerfectedMaxAnimus(fullItemRead, temperingAffixes)) {
            perfectedAffix = MasterworkedAffixSelection.temperingAffix("defense_max_animus");
        }
        ItemImportDebugTrace.log("GA_MARKER_SUMMARY", () -> "globalGaMarkersDetected=" + markerSummary.globalDetected()
                + " globalGaMarkerCount=" + markerSummary.globalCount()
                + " localGaMarkersAssignedCount=" + markerSummary.localAssignedCount()
                + " ambiguousGaMarkersRequiresConfirmation=" + markerSummary.requiresConfirmation());
        if (perfectedAffix == null && markerSummary.requiresConfirmation()) {
            perfectedAffix = MasterworkedAffixSelection.unknown("REQUIRES_CONFIRMATION", "ambiguous_ga_markers");
        }
        if (perfectedAffix == null && hasMasterworkingMarkers(affixes, temperingAffixes)) {
            ItemImportDebugTrace.log("MASTERWORKING_CANDIDATE", () -> "quality=25/25"
                    + " perfectedAffix=REQUIRES_CONFIRMATION"
                    + " candidates=" + masterworkingCandidateLabels(affixes, temperingAffixes)
                    + " reason=" + ItemImportDebugTrace.quote("quality 25/25 and GA/masterworking markers detected but selected perfected affix is ambiguous"));
        }
        return new ItemMasterworking(25, 25, perfectedAffix);
    }

    private static GreaterAffixMarkerSummary greaterAffixMarkerSummary(FullItemRead fullItemRead,
                                                                       List<ImportedItemAffix> affixes,
                                                                       List<ItemTemperingAffix> temperingAffixes) {
        int globalCount = countGlobalGreaterMarkers(fullItemRead);
        int localAssigned = (int) (affixes == null ? List.<ImportedItemAffix>of() : affixes).stream()
                .filter(ImportedItemAffix::isGreaterAffix)
                .count();
        localAssigned += (int) (temperingAffixes == null ? List.<ItemTemperingAffix>of() : temperingAffixes).stream()
                .filter(ItemTemperingAffix::isGreaterAffix)
                .count();
        return new GreaterAffixMarkerSummary(globalCount > 0, globalCount, localAssigned, globalCount > localAssigned);
    }

    private static int countGlobalGreaterMarkers(FullItemRead fullItemRead) {
        if (fullItemRead == null) {
            return 0;
        }
        int count = 0;
        for (FullItemReadLine line : fullItemRead.getLines()) {
            count += countGreaterMarkers(line.getText());
        }
        return count;
    }

    private static int countGreaterMarkers(String text) {
        String safe = text == null ? "" : text;
        int count = 0;
        for (int index = 0; index < safe.length(); index++) {
            if (isGreaterMarker(safe.charAt(index))) {
                count++;
            }
        }
        return count;
    }

    private static boolean isGreaterMarker(char marker) {
        return "*★⭐✦✧✱✳✴✵✶✷✸✹✺✻✼✽✾❋❂◆◇♦●•".indexOf(marker) >= 0;
    }

    private static boolean hasMasterworkingMarkers(List<ImportedItemAffix> affixes, List<ItemTemperingAffix> temperingAffixes) {
        return (affixes == null ? List.<ImportedItemAffix>of() : affixes).stream().anyMatch(ImportedItemAffix::isGreaterAffix)
                || (temperingAffixes == null ? List.<ItemTemperingAffix>of() : temperingAffixes).stream().anyMatch(ItemTemperingAffix::isGreaterAffix);
    }

    private static String masterworkingCandidateLabels(List<ImportedItemAffix> affixes, List<ItemTemperingAffix> temperingAffixes) {
        List<String> labels = new ArrayList<>();
        for (ImportedItemAffix affix : affixes == null ? List.<ImportedItemAffix>of() : affixes) {
            if (affix.isGreaterAffix()) {
                labels.add("ORDINARY_AFFIX:" + affix.getType().name());
            }
        }
        for (ItemTemperingAffix affix : temperingAffixes == null ? List.<ItemTemperingAffix>of() : temperingAffixes) {
            if (affix.isGreaterAffix()) {
                labels.add("TEMPERING_AFFIX:" + affix.getDefinitionId());
            }
        }
        return labels.toString();
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
        boolean indestructible = hasIndestructibleLine(fullItemRead);
        Optional<TransfigurationAffixRoll> allStatsRoll = detectTransfigurationAffixRoll(fullItemRead);
        Optional<Integer> bonusQuality = detectBonusItemQuality(fullItemRead);
        if (!transfigured && !indestructible && allStatsRoll.isEmpty() && bonusQuality.isEmpty()) {
            return ItemTransfiguration.none();
        }
        HoradricTransfigurationOutcome outcome;
        if (indestructible) {
            outcome = HoradricTransfigurationOutcome.INDESTRUCTIBLE;
            ItemImportDebugTrace.log("LINE_CLASSIFICATION", () -> "decision=transfiguration"
                    + " result=INDESTRUCTIBLE"
                    + " reason=" + ItemImportDebugTrace.quote("Niezniszczalność line detected with transfigured item context"));
        } else if (allStatsRoll.isPresent()) {
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
                outcome == HoradricTransfigurationOutcome.INDESTRUCTIBLE ? null : allStatsRoll.orElse(null),
                "",
                null,
                bonusQuality.orElse(null),
                false,
                ""
        );
    }

    private static boolean hasIndestructibleLine(FullItemRead fullItemRead) {
        return fullItemRead.getLines().stream()
                .map(FullItemReadLine::getText)
                .map(ItemImportEditableFormFactory::normalize)
                .anyMatch(line -> line.contains("NIEZNISZCZALNOSC")
                        || line.contains("NIEZNISZCZALNY")
                        || line.contains("INDESTRUCTIBLE"));
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
            if (line.getType() == FullItemReadLineType.SOCKET
                    && isPhysicalDamageMultiplierLine(normalize(line.getText()))) {
                ItemImportDebugTrace.log("LINE_CLASSIFICATION", () -> "decision=socketGemRune"
                        + " sourceLine=" + ItemImportDebugTrace.compactText(line.getText())
                        + " reason=" + ItemImportDebugTrace.quote("physical damage multiplier in socket/gem/rune region is not a transfiguration result"));
                continue;
            }
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
                            + " anchor=" + ItemImportDebugTrace.quote(parsed.anchor())
                            + " localValueWindow=" + ItemImportDebugTrace.compactText(parsed.localValueWindow())
                            + " displayedValue=" + parsed.displayedValue()
                            + " sourceRangeMin=" + parsed.sourceRangeMin()
                            + " sourceRangeMax=" + parsed.sourceRangeMax()
                            + " rejectedNumericTokens=" + ItemImportDebugTrace.compactText(parsed.rejectedNumericTokens())
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
        if (!transfigurationDefinitionAllowsLine(definition, normalized)) {
            return Optional.empty();
        }
        Optional<TransfigurationAnchor> anchor = findTransfigurationAnchor(normalized, definition);
        if (anchor.isEmpty()) {
            return Optional.empty();
        }
        int windowStart = localValueWindowStart(normalized, anchor.get().start());
        String localValueWindow = normalized.substring(windowStart, anchor.get().start());
        List<TransfigurationNumericToken> numericTokens = numericTokens(localValueWindow);
        List<String> rejectedTokens = new ArrayList<>();
        TransfigurationNumericToken selectedToken = null;
        for (int index = numericTokens.size() - 1; index >= 0; index--) {
            TransfigurationNumericToken token = numericTokens.get(index);
            Optional<Double> parsedValue = displayedTransfigurationValue(token, localValueWindow);
            if (parsedValue.isPresent()) {
                selectedToken = token.withDisplayedValue(parsedValue.get());
                break;
            }
            rejectedTokens.add(0, token.raw() + ": " + rejectionReason(token, localValueWindow));
        }
        if (selectedToken == null) {
            if (ItemImportDebugTrace.isEnabled()) {
                ItemImportDebugTrace.log("TRANSFIGURATION_CANDIDATE", () -> "definitionId="
                        + ItemImportDebugTrace.quote(definition.getId())
                        + " anchor=" + ItemImportDebugTrace.quote(anchor.get().text())
                        + " localValueWindow=" + ItemImportDebugTrace.compactText(localValueWindow)
                        + " displayedValue=UNKNOWN"
                        + " rejectedNumericTokens=" + ItemImportDebugTrace.compactText(String.join(" | ", rejectedTokens))
                        + " reason=\"no safe local value\""
                        + " sourceLine=" + ItemImportDebugTrace.compactText(line));
            }
            return Optional.empty();
        }
        Double sourceRangeMin = null;
        Double sourceRangeMax = null;
        String suffix = normalized.substring(anchor.get().end());
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
        return Optional.of(new ParsedTransfigurationAffixRoll(
                selectedToken.displayedValue(),
                sourceRangeMin,
                sourceRangeMax,
                anchor.get().text(),
                localValueWindow,
                String.join(" | ", rejectedTokens)
        ));
    }

    private static boolean transfigurationDefinitionAllowsLine(TransfigurationAffixDefinition definition,
                                                               String normalizedLine) {
        String collapsed = normalizedLine.replaceAll("[^A-Z0-9]", "");
        if ("PHYSICAL_DAMAGE_MULTIPLIER".equals(definition.getId())) {
            return collapsed.contains("FIZYCZNE") || collapsed.contains("PHYSICAL");
        }
        if ("ELEMENTAL_SPECIFIC_DAMAGE".equals(definition.getId())) {
            return definition.getElementOptions().stream()
                    .map(ItemImportEditableFormFactory::normalize)
                    .map(value -> value.replaceAll("[^A-Z0-9]", ""))
                    .anyMatch(collapsed::contains);
        }
        return true;
    }

    private static Optional<TransfigurationAnchor> findTransfigurationAnchor(String normalized,
                                                                             TransfigurationAffixDefinition definition) {
        List<String> aliases = new ArrayList<>();
        aliases.add(definition.getDisplayName());
        aliases.add(definition.getSourceName());
        if (definition.getDisplayName().startsWith("do ")) {
            aliases.add(definition.getDisplayName().substring(3));
        }
        TransfigurationAnchor best = null;
        for (String alias : aliases) {
            String normalizedAlias = normalize(alias)
                    .replace("[+]", "")
                    .replace("[X]", "")
                    .trim();
            if (normalizedAlias.length() < 5) {
                continue;
            }
            int index = normalized.indexOf(normalizedAlias);
            if (index < 0) {
                continue;
            }
            TransfigurationAnchor candidate = new TransfigurationAnchor(normalizedAlias, index, index + normalizedAlias.length());
            if (best == null
                    || candidate.start() < best.start()
                    || candidate.start() == best.start() && candidate.text().length() > best.text().length()) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    private static int localValueWindowStart(String normalizedLine, int anchorStart) {
        int latestPreviousAnchorEnd = 0;
        for (AffixDefinition definition : ApplicationAffixRegistry.get().all()) {
            latestPreviousAnchorEnd = Math.max(latestPreviousAnchorEnd,
                    previousAnchorEnd(normalizedLine, anchorStart, normalize(definition.getDisplayName())));
            for (String alias : definition.getOcrAliases()) {
                latestPreviousAnchorEnd = Math.max(latestPreviousAnchorEnd,
                        previousAnchorEnd(normalizedLine, anchorStart, normalize(alias)));
            }
        }
        for (TransfigurationAffixDefinition definition : TransfigurationAffixCatalog.definitions()) {
            latestPreviousAnchorEnd = Math.max(latestPreviousAnchorEnd,
                    previousAnchorEnd(normalizedLine, anchorStart, normalize(definition.getDisplayName())));
        }
        return Math.max(latestPreviousAnchorEnd, Math.max(0, anchorStart - 64));
    }

    private static int previousAnchorEnd(String normalizedLine, int anchorStart, String normalizedAnchor) {
        if (normalizedAnchor == null || normalizedAnchor.length() < 5) {
            return 0;
        }
        int index = normalizedLine.indexOf(normalizedAnchor);
        int latest = 0;
        while (index >= 0 && index < anchorStart) {
            latest = Math.max(latest, index + normalizedAnchor.length());
            index = normalizedLine.indexOf(normalizedAnchor, index + 1);
        }
        return latest;
    }

    private static List<TransfigurationNumericToken> numericTokens(String localValueWindow) {
        List<TransfigurationNumericToken> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("(\\+)?\\s*([0-9]+(?:[,.][0-9]+)?)(?:\\s*(PKT\\.?|PT\\.?|%))?")
                .matcher(localValueWindow);
        while (matcher.find()) {
            Optional<Double> value = parseNumber(matcher.group(2));
            if (value.isEmpty()) {
                continue;
            }
            result.add(new TransfigurationNumericToken(
                    matcher.group().trim(),
                    matcher.group(2),
                    value.get(),
                    matcher.start(),
                    matcher.end(),
                    matcher.group(1) != null,
                    matcher.group(3) == null ? "" : matcher.group(3),
                    value.get()
            ));
        }
        return result;
    }

    private static Optional<Double> displayedTransfigurationValue(TransfigurationNumericToken token,
                                                                  String localValueWindow) {
        if (isBracketReferenceToken(token, localValueWindow)) {
            return Optional.empty();
        }
        if (token.hasPlus() || !token.unit().isBlank()) {
            Optional<Double> recovered = recoverGluedFlatDisplayedValue(token, localValueWindow);
            return recovered.or(() -> Optional.of(token.value()));
        }
        return Optional.empty();
    }

    private static Optional<Double> recoverGluedFlatDisplayedValue(TransfigurationNumericToken token,
                                                                   String localValueWindow) {
        String unit = token.unit();
        if (!unit.startsWith("PKT") && !unit.startsWith("PT")) {
            return Optional.empty();
        }
        String digits = token.number().replaceAll("[^0-9]", "");
        if (token.hasPlus() || digits.length() <= 3 || !hasPreviousBracketArtifact(token, localValueWindow)) {
            return Optional.empty();
        }
        String suffix = digits.substring(digits.length() - 3);
        return parseNumber(suffix);
    }

    private static boolean hasPreviousBracketArtifact(TransfigurationNumericToken token, String localValueWindow) {
        String prefix = localValueWindow.substring(0, Math.min(localValueWindow.length(), Math.max(0, token.start())));
        return prefix.lastIndexOf('[') >= 0 || Pattern.compile("[0-9]\\s+$").matcher(prefix).find();
    }

    private static boolean isBracketReferenceToken(TransfigurationNumericToken token, String localValueWindow) {
        int before = token.start() - 1;
        while (before >= 0 && Character.isWhitespace(localValueWindow.charAt(before))) {
            before--;
        }
        int after = token.end();
        while (after < localValueWindow.length() && Character.isWhitespace(localValueWindow.charAt(after))) {
            after++;
        }
        return (before >= 0 && localValueWindow.charAt(before) == '[')
                || (after < localValueWindow.length() && localValueWindow.charAt(after) == ']');
    }

    private static String rejectionReason(TransfigurationNumericToken token, String localValueWindow) {
        if (isBracketReferenceToken(token, localValueWindow)) {
            return "belongs to previous bracket/reference";
        }
        if (!token.hasPlus() && token.unit().isBlank()) {
            return "has no local affix sign or unit";
        }
        return "not selected";
    }

    private record ParsedTransfigurationAffixRoll(double displayedValue,
                                                  Double sourceRangeMin,
                                                  Double sourceRangeMax,
                                                  String anchor,
                                                  String localValueWindow,
                                                  String rejectedNumericTokens) {
    }

    private record TransfigurationAnchor(String text, int start, int end) {
    }

    private record TransfigurationNumericToken(String raw,
                                               String number,
                                               double value,
                                               int start,
                                               int end,
                                               boolean hasPlus,
                                               String unit,
                                               double displayedValue) {
        private TransfigurationNumericToken withDisplayedValue(double newDisplayedValue) {
            return new TransfigurationNumericToken(raw, number, value, start, end, hasPlus, unit, newDisplayedValue);
        }
    }

    private record OrdinaryClassification(List<ImportedItemAffix> ordinaryAffixes,
                                          List<ItemTemperingAffix> overflowTemperingAffixes) {
    }

    private record GreaterAffixMarkerSummary(boolean globalDetected,
                                             int globalCount,
                                             int localAssignedCount,
                                             boolean requiresConfirmation) {
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

    private static Optional<Double> parseNumber(String rawToken) {
        try {
            return Optional.of(Double.parseDouble((rawToken == null ? "" : rawToken).replace(',', '.')));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
