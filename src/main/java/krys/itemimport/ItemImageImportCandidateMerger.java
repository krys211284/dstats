package krys.itemimport;

import krys.item.EquipmentSlot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scala wyniki z wielu wariantów OCR w jeden deterministyczny candidate parse result. */
final class ItemImageImportCandidateMerger {
    private static final Pattern ROLL_RANGE_PATTERN = Pattern.compile(
            "\\[\\s*\\+?\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)"
                    + "(?:\\s*[-–—−]\\s*\\+?\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?))?"
                    + "\\s*]\\s*%?"
    );

    ItemImageImportCandidateParseResult merge(ItemImageMetadata metadata,
                                              int analyzedVariantCount,
                                              List<ItemImageImportCandidateParseResult> parseResults) {
        return merge(metadata, analyzedVariantCount, parseResults, null);
    }

    ItemImageImportCandidateParseResult merge(ItemImageMetadata metadata,
                                              int analyzedVariantCount,
                                              List<ItemImageImportCandidateParseResult> parseResults,
                                              ItemImageImportCandidateParseResult authoritativeParseResult) {
        return merge(metadata, analyzedVariantCount, parseResults, authoritativeParseResult, null);
    }

    ItemImageImportCandidateParseResult merge(ItemImageMetadata metadata,
                                              int analyzedVariantCount,
                                              List<ItemImageImportCandidateParseResult> parseResults,
                                              ItemImageImportCandidateParseResult authoritativeParseResult,
                                              GreaterAffixHeaderEvidence greaterAffixHeaderEvidence) {
        List<ItemImageImportCandidateParseResult> safeParseResults = parseResults == null ? List.of() : parseResults;
        List<ItemImageImportCandidateParseResult> fieldParseResults = new ArrayList<>(safeParseResults);
        if (authoritativeParseResult != null) {
            fieldParseResults.add(authoritativeParseResult);
        }
        ItemImportFieldCandidate<EquipmentSlot> slotCandidate = mergeField(
                fieldParseResults.stream().map(ItemImageImportCandidateParseResult::getSlotCandidate).toList(),
                "Nie udało się rozpoznać slotu / typu itemu z OCR."
        );
        FullItemRead fullItemRead = authoritativeParseResult == null
                ? mergeFullItemRead(safeParseResults)
                : mergeAuthoritativeFullItemRead(authoritativeParseResult.getFullItemRead(), safeParseResults);
        ItemImportFieldCandidate<Long> weaponDamageCandidate = mergeField(
                fieldParseResults.stream().map(ItemImageImportCandidateParseResult::getWeaponDamageCandidate).toList(),
                "Nie udało się rozpoznać pola `WEAPON DAMAGE` z OCR."
        );
        ItemImportFieldCandidate<Double> strengthCandidate = mergeField(
                fieldParseResults.stream().map(ItemImageImportCandidateParseResult::getStrengthCandidate).toList(),
                "Nie udało się rozpoznać pola `Strength` z OCR."
        );
        ItemImportFieldCandidate<Double> intelligenceCandidate = mergeField(
                fieldParseResults.stream().map(ItemImageImportCandidateParseResult::getIntelligenceCandidate).toList(),
                "Nie udało się rozpoznać pola `Intelligence` z OCR."
        );
        ItemImportFieldCandidate<Double> thornsCandidate = mergeField(
                fieldParseResults.stream().map(ItemImageImportCandidateParseResult::getThornsCandidate).toList(),
                "Nie udało się rozpoznać pola `Thorns` z OCR."
        );
        ItemImportFieldCandidate<Double> blockChanceCandidate = mergeField(
                fieldParseResults.stream().map(ItemImageImportCandidateParseResult::getBlockChanceCandidate).toList(),
                "Nie udało się rozpoznać pola `Block chance` z OCR."
        );
        ItemImportFieldCandidate<Double> retributionChanceCandidate = mergeField(
                fieldParseResults.stream().map(ItemImageImportCandidateParseResult::getRetributionChanceCandidate).toList(),
                "Nie udało się rozpoznać pola `Retribution chance` z OCR."
        );

        return new ItemImageImportCandidateParseResult(
                metadata,
                fullItemRead,
                slotCandidate,
                weaponDamageCandidate,
                strengthCandidate,
                intelligenceCandidate,
                thornsCandidate,
                blockChanceCandidate,
                retributionChanceCandidate,
                greaterAffixHeaderEvidence == null
                        ? bestHeaderEvidence(fieldParseResults)
                        : greaterAffixHeaderEvidence,
                buildImportNotice(analyzedVariantCount, slotCandidate, weaponDamageCandidate, strengthCandidate,
                        intelligenceCandidate, thornsCandidate, blockChanceCandidate, retributionChanceCandidate)
        );
    }

    private static GreaterAffixHeaderEvidence bestHeaderEvidence(List<ItemImageImportCandidateParseResult> parseResults) {
        GreaterAffixHeaderEvidence selected = GreaterAffixHeaderEvidence.notDetected();
        for (ItemImageImportCandidateParseResult parseResult : parseResults == null ? List.<ItemImageImportCandidateParseResult>of() : parseResults) {
            GreaterAffixHeaderEvidence candidate = parseResult.getGreaterAffixHeaderEvidence();
            if (isBetterHeaderEvidence(candidate, selected)) {
                selected = candidate;
            }
        }
        return selected;
    }

    private static boolean isBetterHeaderEvidence(GreaterAffixHeaderEvidence candidate,
                                                  GreaterAffixHeaderEvidence selected) {
        if (candidate == null) {
            return false;
        }
        if (selected == null) {
            return true;
        }
        if (candidate.isReliable() != selected.isReliable()) {
            return candidate.isReliable();
        }
        if (candidate.getDetectedCount() != selected.getDetectedCount()) {
            return candidate.getDetectedCount() > selected.getDetectedCount();
        }
        return sourceScore(candidate.getSource()) > sourceScore(selected.getSource());
    }

    private static int sourceScore(GreaterAffixHeaderEvidenceSource source) {
        return switch (source == null ? GreaterAffixHeaderEvidenceSource.NOT_DETECTED : source) {
            case OCR_HEADER_MIXED -> 3;
            case OCR_HEADER_LITERAL_STARS -> 2;
            case OCR_HEADER_ZERO_LIKE_RUN_HEURISTIC -> 1;
            case NOT_DETECTED -> 0;
        };
    }

    private static FullItemRead mergeAuthoritativeFullItemRead(FullItemRead authoritativeRead,
                                                               List<ItemImageImportCandidateParseResult> parseResults) {
        FullItemRead safeAuthoritativeRead = authoritativeRead == null ? FullItemRead.empty() : authoritativeRead;
        if (!safeAuthoritativeRead.hasAnyData()) {
            return mergeFullItemRead(parseResults);
        }
        List<ItemImportDetails> sourceDetails = new ArrayList<>();
        sourceDetails.add(safeAuthoritativeRead.getDetails());
        List<FullItemRead> rawReads = new ArrayList<>();
        Map<String, List<FullItemReadLine>> rawLinesByKey = new LinkedHashMap<>();
        for (ItemImageImportCandidateParseResult parseResult : parseResults == null ? List.<ItemImageImportCandidateParseResult>of() : parseResults) {
            FullItemRead read = parseResult.getFullItemRead();
            if (read != null && read.hasAnyData()) {
                rawReads.add(read);
                sourceDetails.add(read.getDetails());
                for (FullItemReadLine line : read.getLines()) {
                    if (line.getText().isBlank()) {
                        continue;
                    }
                    String key = fullReadLineDeduplicationKey(line);
                    rawLinesByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(line);
                }
            }
        }

        Map<String, FullItemReadLine> selectedLines = new LinkedHashMap<>();
        for (FullItemReadLine authoritativeLine : safeAuthoritativeRead.getLines()) {
            if (authoritativeLine.getText().isBlank()) {
                continue;
            }
            String key = fullReadLineDeduplicationKey(authoritativeLine);
            FullItemReadLine selected = authoritativeLine;
            for (FullItemReadLine rawLine : rawLinesByKey.getOrDefault(key, List.of())) {
                if (shouldReplaceMergedLine(key, selected, rawLine) || shouldPreferRawFullReadText(selected, rawLine)) {
                    selected = mergeSelectedLineWithVisualAnchor(rawLine, authoritativeLine, selected, rawLine);
                    logAffixMerge(key, authoritativeLine, rawLine, selected,
                            "raw candidate supplies better value while typed line keeps visual authority");
                } else {
                    selected = mergeSelectedLineWithVisualAnchor(selected, authoritativeLine, selected, rawLine);
                    logAffixMerge(key, selected, rawLine, selected,
                            "typed candidate keeps selected value and visual authority");
                }
            }
            selectedLines.put(key, selected);
        }

        Set<String> authoritativeKeys = selectedLines.keySet();
        for (Map.Entry<String, List<FullItemReadLine>> entry : rawLinesByKey.entrySet()) {
            if (authoritativeKeys.contains(entry.getKey())) {
                continue;
            }
            selectedLines.put(entry.getKey(), mergeLineCandidates(entry.getKey(), entry.getValue()));
        }

        FullItemRead rebuiltRead = ItemImageImportTextParser.buildFullItemRead(
                selectedLines.values().stream()
                        .map(FullItemReadLine::getText)
                        .toList()
        );
        ItemImportDetails details = mergeDetails(rebuiltRead.getDetails(), sourceDetails);
        String itemName = chooseBetterItemName(
                chooseBetterItemName(firstItemName(rawReads), safeAuthoritativeRead.getItemName()),
                details.getItemName()
        );
        return new FullItemRead(
                itemName,
                firstNonBlank(safeAuthoritativeRead.getItemTypeLine(), rebuiltRead.getItemTypeLine(), details.getItemType()),
                firstNonBlank(safeAuthoritativeRead.getRarity(), rebuiltRead.getRarity(), details.getItemRarity()),
                safeAuthoritativeRead.getItemPower().isBlank() && details.getItemPower() != null
                        ? "Moc przedmiotu: " + details.getItemPower()
                        : safeAuthoritativeRead.getItemPower(),
                firstNonBlank(safeAuthoritativeRead.getBaseItemValue(), rebuiltRead.getBaseItemValue()),
                List.copyOf(selectedLines.values()),
                details
        );
    }

    private static boolean shouldPreferRawFullReadText(FullItemReadLine selected, FullItemReadLine candidate) {
        if (selected == null || candidate == null || selected.getType() != FullItemReadLineType.ITEM_NAME
                || candidate.getType() != FullItemReadLineType.ITEM_NAME) {
            return false;
        }
        String selectedKey = normalizeForDeduplication(selected.getText()).replaceAll("[^A-Z0-9]", "");
        String candidateKey = normalizeForDeduplication(candidate.getText()).replaceAll("[^A-Z0-9]", "");
        if (!Objects.equals(selectedKey, candidateKey)) {
            return false;
        }
        String candidateText = candidate.getText();
        return !candidateText.isBlank() && candidateText.equals(candidateText.toUpperCase(Locale.ROOT));
    }

    private static FullItemReadLine mergeLineCandidates(String key, List<FullItemReadLine> candidates) {
        FullItemReadLine selected = null;
        for (FullItemReadLine candidate : candidates == null ? List.<FullItemReadLine>of() : candidates) {
            if (candidate.getText().isBlank()) {
                continue;
            }
            if (selected == null) {
                selected = candidate;
                continue;
            }
            if (shouldReplaceMergedLine(key, selected, candidate)) {
                selected = mergeSelectedLineWithBestVisualAnchor(candidate, selected, candidate);
            } else {
                selected = mergeSelectedLineWithBestVisualAnchor(selected, selected, candidate);
            }
        }
        return selected == null ? new FullItemReadLine(FullItemReadLineType.OTHER, "") : selected;
    }

    private static String firstItemName(List<FullItemRead> reads) {
        String best = "";
        for (FullItemRead read : reads == null ? List.<FullItemRead>of() : reads) {
            best = chooseBetterItemName(best, read.getItemName());
        }
        return best;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static FullItemRead mergeFullItemRead(List<ItemImageImportCandidateParseResult> parseResults) {
        Map<String, FullItemReadLine> mergedLines = new LinkedHashMap<>();
        String itemName = "";
        String itemTypeLine = "";
        String rarity = "";
        String itemPower = "";
        String baseItemValue = "";
        List<ItemImportDetails> sourceDetails = new ArrayList<>();

        for (ItemImageImportCandidateParseResult parseResult : parseResults) {
            FullItemRead read = parseResult.getFullItemRead();
            if (read == null || !read.hasAnyData()) {
                continue;
            }
            sourceDetails.add(read.getDetails());
            if (itemName.isBlank() && !read.getItemName().isBlank()) {
                itemName = read.getItemName();
            }
            if (itemTypeLine.isBlank() && !read.getItemTypeLine().isBlank()) {
                itemTypeLine = read.getItemTypeLine();
            }
            if (rarity.isBlank() && !read.getRarity().isBlank()) {
                rarity = read.getRarity();
            }
            if (itemPower.isBlank() && !read.getItemPower().isBlank()) {
                itemPower = read.getItemPower();
            }
            if (baseItemValue.isBlank() && !read.getBaseItemValue().isBlank()) {
                baseItemValue = read.getBaseItemValue();
            }
            for (FullItemReadLine line : read.getLines()) {
                if (line.getText().isBlank()) {
                    continue;
                }
                String key = fullReadLineDeduplicationKey(line);
                FullItemReadLine existingLine = mergedLines.get(key);
                if (existingLine == null) {
                    logAffixMerge(key, null, line, line, "first candidate for dedup key");
                    mergedLines.put(key, line);
                } else if (shouldReplaceMergedLine(key, existingLine, line)) {
                    FullItemReadLine selected = mergeSelectedLineWithBestVisualAnchor(line, existingLine, line);
                    logAffixMerge(key, existingLine, line, selected, mergeReplacementReason(key, existingLine, line));
                    mergedLines.put(key, selected);
                } else {
                    FullItemReadLine selected = mergeSelectedLineWithBestVisualAnchor(existingLine, existingLine, line);
                    logAffixMerge(key, existingLine, line, selected, mergeKeepReason(key, existingLine, line));
                    mergedLines.put(key, selected);
                }
            }
        }

        FullItemRead mergedRead = ItemImageImportTextParser.buildFullItemRead(
                mergedLines.values().stream()
                        .map(FullItemReadLine::getText)
                        .toList()
        );
        return new FullItemRead(
                chooseBetterItemName(itemName, mergedRead.getItemName()),
                itemTypeLine.isBlank() ? mergedRead.getItemTypeLine() : itemTypeLine,
                rarity.isBlank() ? mergedRead.getRarity() : rarity,
                itemPower.isBlank() ? mergedRead.getItemPower() : itemPower,
                baseItemValue.isBlank() ? mergedRead.getBaseItemValue() : baseItemValue,
                List.copyOf(mergedLines.values()),
                mergeDetails(mergedRead.getDetails(), sourceDetails)
        );
    }

    private static FullItemReadLine mergeSelectedLineWithBestVisualAnchor(FullItemReadLine selected,
                                                                          FullItemReadLine left,
                                                                          FullItemReadLine right) {
        FullItemReadLine visualAnchor = betterVisualAnchorLine(left, right);
        return mergeSelectedLineWithVisualAnchor(selected, visualAnchor, left, right);
    }

    private static FullItemReadLine mergeSelectedLineWithVisualAnchor(FullItemReadLine selected,
                                                                      FullItemReadLine visualAnchor,
                                                                      FullItemReadLine left,
                                                                      FullItemReadLine right) {
        FullItemReadLineSource source = selected.getSource().withVisualAnchorFrom(visualAnchor.getSource());
        FullItemReadLine markerLine = localGreaterMarkerLine(left, right);
        if (markerLine != null && !hasLocalGreaterMarker(selected) && !hasLocalGreaterMarker(visualAnchor)) {
            source = source.withParentRawLine(markerLine.getSource().getParentRawLine());
        }
        return selected.withSource(source);
    }

    private static FullItemReadLine betterVisualAnchorLine(FullItemReadLine left, FullItemReadLine right) {
        int leftOrder = visualOrder(left);
        int rightOrder = visualOrder(right);
        if (rightOrder < leftOrder) {
            return right;
        }
        if (rightOrder == leftOrder && hasLocalGreaterMarker(right) && !hasLocalGreaterMarker(left)) {
            return right;
        }
        return left;
    }

    private static int visualOrder(FullItemReadLine line) {
        if (line == null) {
            return Integer.MAX_VALUE;
        }
        return line.getSource().visualOrder(Integer.MAX_VALUE / 10_000, 0);
    }

    private static FullItemReadLine localGreaterMarkerLine(FullItemReadLine left, FullItemReadLine right) {
        if (hasLocalGreaterMarker(left)) {
            return left;
        }
        if (hasLocalGreaterMarker(right)) {
            return right;
        }
        return null;
    }

    private static boolean hasLocalGreaterMarker(FullItemReadLine line) {
        if (line == null) {
            return false;
        }
        FullItemReadLineSource source = line.getSource();
        return startsWithGreaterMarker(line.getText())
                || hasGreaterMarkerNearSegment(source.getSourceRawLine(), source.getSourceSegmentStart())
                || hasGreaterMarkerNearSegment(source.getParentRawLine(), source.getSourceSegmentStart())
                || hasGreaterMarkerNearSegment(source.getVisualSourceText(), source.getVisualSegmentStart());
    }

    private static boolean startsWithGreaterMarker(String text) {
        String trimmed = text == null ? "" : text.trim();
        return !trimmed.isBlank() && isGreaterMarker(trimmed.charAt(0));
    }

    private static boolean hasGreaterMarkerNearSegment(String rawLine, int segmentStart) {
        String safeRawLine = rawLine == null ? "" : rawLine;
        if (safeRawLine.isBlank()) {
            return false;
        }
        if (segmentStart <= 0) {
            return startsWithGreaterMarker(safeRawLine);
        }
        int safeSegmentStart = Math.min(segmentStart, safeRawLine.length());
        int markerIndex = -1;
        for (int index = 0; index < safeSegmentStart; index++) {
            if (isGreaterMarker(safeRawLine.charAt(index))) {
                markerIndex = index;
            }
        }
        if (markerIndex < 0) {
            return false;
        }
        String between = safeRawLine.substring(markerIndex + 1, safeSegmentStart);
        return between.replaceAll("\\s+", "").length() <= 6;
    }

    private static boolean containsGreaterMarker(String text) {
        String safe = text == null ? "" : text;
        for (int index = 0; index < safe.length(); index++) {
            if (isGreaterMarker(safe.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGreaterMarker(char marker) {
        return "*★⭐✦✧✱✳✴✵✶✷✸✹✺✻✼✽✾❋❂◆◇♦●•".indexOf(marker) >= 0;
    }

    private static boolean shouldReplaceMergedLine(String key, FullItemReadLine existingLine, FullItemReadLine candidateLine) {
        if (key != null && key.startsWith("AFFIX:")) {
            boolean existingHasCompatibleSingleReference = hasCompatibleSingleReference(key, existingLine.getText());
            boolean candidateHasCompatibleSingleReference = hasCompatibleSingleReference(key, candidateLine.getText());
            if (candidateHasCompatibleSingleReference && !existingHasCompatibleSingleReference) {
                return true;
            }
            if (existingHasCompatibleSingleReference && !candidateHasCompatibleSingleReference) {
                return false;
            }
            boolean existingHasRollRange = hasActualRollRange(existingLine.getText());
            boolean candidateHasRollRange = hasActualRollRange(candidateLine.getText());
            if (candidateHasRollRange && !existingHasRollRange) {
                return true;
            }
            if (existingHasRollRange && !candidateHasRollRange) {
                return false;
            }
        }
        return lineQualityScore(candidateLine) > lineQualityScore(existingLine);
    }

    private static void logAffixMerge(String key,
                                      FullItemReadLine existingLine,
                                      FullItemReadLine candidateLine,
                                      FullItemReadLine selectedLine,
                                      String reason) {
        if (!ItemImportDebugTrace.isEnabled() || key == null || !key.startsWith("AFFIX:")) {
            return;
        }
        ItemImportDebugTrace.log("AFFIX_MERGE", () -> "dedupKey=" + ItemImportDebugTrace.quote(key)
                + " existing=" + formatLineCandidate(existingLine)
                + " candidate=" + formatLineCandidate(candidateLine)
                + " selected=" + formatLineCandidate(selectedLine)
                + " reason=" + ItemImportDebugTrace.quote(reason)
                + " candidateHasReferenceOrRange=" + hasActualRollRange(candidateLine == null ? "" : candidateLine.getText())
                + " selectedHasReferenceOrRange=" + hasActualRollRange(selectedLine == null ? "" : selectedLine.getText()));
    }

    private static String formatLineCandidate(FullItemReadLine line) {
        if (line == null) {
            return "null";
        }
        return "{type=" + line.getType()
                + ", score=" + lineQualityScore(line)
                + ", selectedOrder=" + line.getSource().selectedOrder(0, 0)
                + ", visualOrder=" + line.getSource().visualOrder(0, 0)
                + ", text=" + ItemImportDebugTrace.compactText(line.getText())
                + ", tokens=" + ItemImportDebugTrace.numericTokens(line.getText())
                + "}";
    }

    private static String mergeReplacementReason(String key, FullItemReadLine existingLine, FullItemReadLine candidateLine) {
        if (key != null && key.startsWith("AFFIX:")) {
            boolean existingHasCompatibleSingleReference = hasCompatibleSingleReference(key, existingLine.getText());
            boolean candidateHasCompatibleSingleReference = hasCompatibleSingleReference(key, candidateLine.getText());
            if (candidateHasCompatibleSingleReference && !existingHasCompatibleSingleReference) {
                return "candidate has compatible single reference from OCR";
            }
            boolean existingHasRollRange = hasActualRollRange(existingLine.getText());
            boolean candidateHasRollRange = hasActualRollRange(candidateLine.getText());
            if (candidateHasRollRange && !existingHasRollRange) {
                return "candidate has OCR range/reference fragment";
            }
        }
        return "candidate has higher line quality score";
    }

    private static String mergeKeepReason(String key, FullItemReadLine existingLine, FullItemReadLine candidateLine) {
        if (key != null && key.startsWith("AFFIX:")) {
            boolean existingHasCompatibleSingleReference = hasCompatibleSingleReference(key, existingLine.getText());
            boolean candidateHasCompatibleSingleReference = hasCompatibleSingleReference(key, candidateLine.getText());
            if (existingHasCompatibleSingleReference && !candidateHasCompatibleSingleReference) {
                return "existing candidate keeps compatible single reference from OCR";
            }
            boolean existingHasRollRange = hasActualRollRange(existingLine.getText());
            boolean candidateHasRollRange = hasActualRollRange(candidateLine.getText());
            if (existingHasRollRange && !candidateHasRollRange) {
                return "existing candidate keeps OCR range/reference fragment";
            }
        }
        return "existing candidate has equal or higher line quality score";
    }

    private static boolean hasCompatibleSingleReference(String key, String text) {
        String[] keyParts = key == null ? new String[0] : key.split(":");
        if (keyParts.length < 3) {
            return false;
        }
        double displayedValue = parseRollRangeNumber(keyParts[2]);
        if (Double.isNaN(displayedValue)) {
            return false;
        }
        Matcher matcher = ROLL_RANGE_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.find() || matcher.group(2) != null) {
            return false;
        }
        double referenceValue = parseRollRangeNumber(matcher.group(1));
        return !Double.isNaN(referenceValue)
                && referenceValue <= displayedValue + 0.0001d
                && displayedValue <= referenceValue * 1.25d + 0.25d;
    }

    private static boolean hasActualRollRange(String text) {
        Matcher matcher = ROLL_RANGE_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return false;
        }
        double min = parseRollRangeNumber(matcher.group(1));
        double max = matcher.group(2) == null ? min : parseRollRangeNumber(matcher.group(2));
        return min <= max;
    }

    private static double parseRollRangeNumber(String token) {
        try {
            return Double.parseDouble((token == null ? "" : token).replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException exception) {
            return Double.NaN;
        }
    }

    private static ItemImportDetails mergeDetails(ItemImportDetails rebuiltDetails, List<ItemImportDetails> sourceDetails) {
        ItemImportDetails safeRebuiltDetails = rebuiltDetails == null ? ItemImportDetails.empty() : rebuiltDetails;
        List<ItemImportDetails> details = new ArrayList<>();
        details.add(safeRebuiltDetails);
        for (ItemImportDetails sourceDetail : sourceDetails) {
            if (sourceDetail != null && sourceDetail.hasAnyData()) {
                details.add(sourceDetail);
            }
        }
        WeaponDamageRangeSelection weaponDamageRange = selectBestWeaponDamageRange(details);
        return new ItemImportDetails(
                firstText(details, DetailTextField.ITEM_NAME),
                firstText(details, DetailTextField.ITEM_TYPE),
                firstText(details, DetailTextField.ITEM_RARITY),
                firstBoolean(details, safeRebuiltDetails.isAncient()),
                firstSlot(details),
                firstLong(details, DetailLongField.ITEM_POWER),
                firstLong(details, DetailLongField.WEAPON_DPS),
                weaponDamageRange.min(),
                weaponDamageRange.max(),
                weaponDamageRange.average(),
                firstDouble(details),
                firstLong(details, DetailLongField.ITEM_ARMOR),
                bestEffectText(details),
                firstMythicUnique(details, safeRebuiltDetails.isMythicUnique())
        );
    }

    private static WeaponDamageRangeSelection selectBestWeaponDamageRange(List<ItemImportDetails> details) {
        Long bestWeaponDps = firstLong(details, DetailLongField.WEAPON_DPS);
        Double bestAttacksPerSecond = firstDouble(details);
        WeaponDamageRangeSelection best = null;
        for (int index = 0; index < details.size(); index++) {
            ItemImportDetails detail = details.get(index);
            WeaponDamageRangeSelection candidate = buildWeaponDamageRangeSelection(detail, index,
                    bestWeaponDps, bestAttacksPerSecond).orElse(null);
            if (candidate == null) {
                continue;
            }
            logWeaponDamageRangeCandidate(candidate, bestWeaponDps, bestAttacksPerSecond);
            if (best == null || candidate.score() > best.score()
                    || (candidate.score() == best.score() && candidate.sourceIndex() < best.sourceIndex())) {
                best = candidate;
            }
        }
        if (best == null) {
            return new WeaponDamageRangeSelection(null, null, null, 0, Integer.MAX_VALUE,
                    "no complete weapon range candidate", false, null, false);
        }
        WeaponDamageRangeSelection selected = best;
        ItemImportDebugTrace.log("WEAPON_DAMAGE_RANGE_MERGE", () -> "selected=true"
                + " sourceIndex=" + selected.sourceIndex()
                + " weaponDamage=" + selected.min() + "-" + selected.max()
                + " averageWeaponDamage=" + selected.average()
                + " damageRangeScore=" + selected.score()
                + " dpsApsCoherent=" + selected.dpsApsCoherent()
                + " expectedDps=" + selected.expectedDps().map(value -> String.format(Locale.US, "%.2f", value)).orElse("null")
                + " reason=" + ItemImportDebugTrace.quote(selected.reason()));
        return best;
    }

    private static Optional<WeaponDamageRangeSelection> buildWeaponDamageRangeSelection(ItemImportDetails detail,
                                                                                       int sourceIndex,
                                                                                       Long bestWeaponDps,
                                                                                       Double bestAttacksPerSecond) {
        if (detail == null || detail.getWeaponDamageMin() == null || detail.getWeaponDamageMax() == null) {
            return Optional.empty();
        }
        Long min = detail.getWeaponDamageMin();
        Long max = detail.getWeaponDamageMax();
        long average = Math.round((min + max) / 2.0d);
        if (min <= 0L || max <= 0L || min >= max) {
            return Optional.of(new WeaponDamageRangeSelection(min, max, average, -10000, sourceIndex,
                    "rejected: invalid min/max relation", false, null, true));
        }
        int score = 1000;
        String reason = "valid min/max range";
        if (detail.getAverageWeaponDamage() != null && detail.getAverageWeaponDamage().equals(average)) {
            score += 250;
        } else if (detail.getAverageWeaponDamage() != null) {
            score -= 250;
            reason += "; average corrected from min/max";
        }
        boolean hasGroupedThousandsShape = min >= 1000L || max >= 1000L;
        if (hasGroupedThousandsShape) {
            score += 200;
        }
        Optional<Double> expectedDps = Optional.empty();
        boolean coherent = false;
        if (bestWeaponDps != null && bestWeaponDps > 0L && bestAttacksPerSecond != null && bestAttacksPerSecond > 0.0d) {
            double expected = average * bestAttacksPerSecond;
            expectedDps = Optional.of(expected);
            double tolerance = Math.max(6.0d, bestWeaponDps * 0.015d);
            double delta = Math.abs(expected - bestWeaponDps);
            if (delta <= tolerance) {
                score += 5000;
                coherent = true;
                reason += "; DPS/APS coherent";
            } else {
                score -= Math.min(4000, (int) Math.round(delta));
                reason += "; DPS/APS incoherent";
            }
        }
        return Optional.of(new WeaponDamageRangeSelection(min, max, average, score, sourceIndex, reason,
                coherent, expectedDps, false));
    }

    private static void logWeaponDamageRangeCandidate(WeaponDamageRangeSelection candidate,
                                                      Long weaponDps,
                                                      Double attacksPerSecond) {
        ItemImportDebugTrace.log("WEAPON_DAMAGE_RANGE_MERGE", () -> "selected=false"
                + " sourceIndex=" + candidate.sourceIndex()
                + " weaponDps=" + valueOrNull(weaponDps)
                + " attacksPerSecond=" + valueOrNull(attacksPerSecond)
                + " weaponDamage=" + valueOrNull(candidate.min()) + "-" + valueOrNull(candidate.max())
                + " averageWeaponDamage=" + valueOrNull(candidate.average())
                + " damageRangeScore=" + candidate.score()
                + " dpsApsCoherent=" + candidate.dpsApsCoherent()
                + " expectedDps=" + candidate.expectedDps().map(value -> String.format(Locale.US, "%.2f", value)).orElse("null")
                + " rejected=" + candidate.rejected()
                + " reason=" + ItemImportDebugTrace.quote(candidate.reason()));
    }

    private static String valueOrNull(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private static boolean firstMythicUnique(List<ItemImportDetails> details, boolean fallback) {
        for (ItemImportDetails detail : details) {
            if (detail.isMythicUnique()) {
                return true;
            }
        }
        return fallback;
    }

    private static String firstText(List<ItemImportDetails> details, DetailTextField field) {
        if (field == DetailTextField.ITEM_NAME) {
            String bestName = "";
            for (ItemImportDetails detail : details) {
                bestName = chooseBetterItemName(bestName, detail.getItemName());
            }
            return bestName;
        }
        for (ItemImportDetails detail : details) {
            String value = switch (field) {
                case ITEM_NAME -> detail.getItemName();
                case ITEM_TYPE -> detail.getItemType();
                case ITEM_RARITY -> detail.getItemRarity();
                case UNIQUE_EFFECT_TEXT -> detail.getUniqueEffectText();
            };
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String bestEffectText(List<ItemImportDetails> details) {
        String best = "";
        int bestScore = Integer.MIN_VALUE;
        int bestIndex = Integer.MAX_VALUE;
        for (int index = 0; index < details.size(); index++) {
            ItemImportDetails detail = details.get(index);
            String value = detail.getUniqueEffectText();
            if (value == null || value.isBlank()) {
                continue;
            }
            String normalized = EffectTextTokenNormalizer.normalizeMultiplierTokens(value);
            int score = effectTextScore(normalized);
            if (score > bestScore || (score == bestScore && index < bestIndex)) {
                best = normalized;
                bestScore = score;
                bestIndex = index;
            }
        }
        return best;
    }

    private static int effectTextScore(String text) {
        String safeText = text == null ? "" : text;
        return safeText.length() + EffectTextTokenNormalizer.semanticTokenScore(safeText);
    }

    private static String chooseBetterItemName(String current, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return current == null ? "" : current;
        }
        if (current == null || current.isBlank()) {
            return candidate;
        }
        int candidateScore = itemNameQualityScore(candidate);
        int currentScore = itemNameQualityScore(current);
        if (candidateScore != currentScore) {
            return candidateScore > currentScore ? candidate : current;
        }
        return candidate.length() > current.length() ? candidate : current;
    }

    private static int itemNameQualityScore(String value) {
        String text = value == null ? "" : value;
        int score = normalizeForDeduplication(text).replaceAll("[^A-Z0-9]", "").length();
        if (text.matches(".*[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ].*")) {
            score += 30;
        }
        if (text.equals(text.toUpperCase(Locale.ROOT))) {
            score += 10;
        }
        if (hasSuspiciousMixedTitleCase(text)) {
            score -= 80;
        }
        return score;
    }

    private static boolean hasSuspiciousMixedTitleCase(String value) {
        String lettersOnly = value == null ? "" : value.replaceAll("[^\\p{L}]", "");
        if (lettersOnly.length() < 5) {
            return false;
        }
        int uppercase = 0;
        int lowercase = 0;
        for (int index = 0; index < lettersOnly.length(); index++) {
            char character = lettersOnly.charAt(index);
            if (Character.isUpperCase(character)) {
                uppercase++;
            } else if (Character.isLowerCase(character)) {
                lowercase++;
            }
        }
        return uppercase >= 4 && lowercase > 0 && uppercase > lowercase * 3;
    }

    private static boolean firstBoolean(List<ItemImportDetails> details, boolean fallback) {
        for (ItemImportDetails detail : details) {
            if (detail.isAncient()) {
                return true;
            }
        }
        return fallback;
    }

    private static EquipmentSlot firstSlot(List<ItemImportDetails> details) {
        for (ItemImportDetails detail : details) {
            if (detail.getEquipmentSlot() != null) {
                return detail.getEquipmentSlot();
            }
        }
        return null;
    }

    private static Long firstLong(List<ItemImportDetails> details, DetailLongField field) {
        for (ItemImportDetails detail : details) {
            Long value = switch (field) {
                case ITEM_POWER -> detail.getItemPower();
                case WEAPON_DPS -> detail.getWeaponDps();
                case WEAPON_DAMAGE_MIN -> detail.getWeaponDamageMin();
                case WEAPON_DAMAGE_MAX -> detail.getWeaponDamageMax();
                case AVERAGE_WEAPON_DAMAGE -> detail.getAverageWeaponDamage();
                case ITEM_ARMOR -> detail.getItemArmor();
            };
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Double firstDouble(List<ItemImportDetails> details) {
        for (ItemImportDetails detail : details) {
            if (detail.getAttacksPerSecond() != null) {
                return detail.getAttacksPerSecond();
            }
        }
        return null;
    }

    private static String fullReadLineDeduplicationKey(FullItemReadLine line) {
        String normalized = normalizeForDeduplication(line.getText());
        FullItemReadLineType type = line.getType();
        if (type == FullItemReadLineType.ITEM_POWER && normalized.contains("MOC PRZEDMIOTU")) {
            return "ITEM_POWER:" + firstNumber(normalized);
        }
        if (type == FullItemReadLineType.BASE_STAT && normalized.contains("PANCERZ")) {
            return "BASE_ARMOR:" + firstNumber(normalized);
        }
        if (normalized.contains("REDUKCJI BLOKOWANYCH OBRAZEN")) {
            return "IMPLICIT:BLOCKED_DAMAGE_REDUCTION";
        }
        if (normalized.contains("SZANSY NA BLOK")) {
            return "IMPLICIT:BLOCK_CHANCE";
        }
        if (normalized.contains("OBRAZEN OD BRONI W GLOWNEJ RECE")) {
            return "IMPLICIT:MAIN_HAND_WEAPON_DAMAGE";
        }
        if (normalized.contains(" SILY")) {
            return "AFFIX:STRENGTH:" + firstNumber(normalized);
        }
        if (normalized.contains("ODPORNOSCI NA WSZYSTKIE ZYWIOLY")) {
            return "AFFIX:ALL_RESISTANCE:" + firstNumber(normalized);
        }
        if (normalized.contains("ODPORNOSCI NA: OGIEN") || normalized.contains("ODPORNOSCI NA OGIEN")) {
            return "AFFIX:FIRE_RESISTANCE:" + firstNumber(normalized);
        }
        if (normalized.contains("REDUKCJI OBRAZEN")) {
            return "AFFIX:DAMAGE_REDUCTION:" + firstNumber(normalized);
        }
        if (normalized.contains("CIERNI")) {
            return "AFFIX:THORNS:" + firstNumber(normalized);
        }
        String collapsed = normalized.replaceAll("[^A-Z0-9]", "");
        if (normalized.contains("SZANSY NA TRAFIENIE KRYTYCZNE") || normalized.contains("SZANSA NA TRAFIENIE KRYTYCZNE")) {
            return "AFFIX:CRITICAL_STRIKE_CHANCE:" + firstNumber(normalized);
        }
        if (isLuckyHitChanceLine(normalized, collapsed)) {
            return "AFFIX:LUCKY_HIT_CHANCE:" + firstNumber(normalized);
        }
        if (normalized.contains("SZYBKOSCI RUCHU")) {
            return "AFFIX:MOVEMENT_SPEED:" + firstNumber(normalized);
        }
        if (normalized.contains("UMIEJETNOSCI") && normalized.contains("GLOWNE")) {
            return "AFFIX:CORE_SKILL_RANKS:" + firstNumber(normalized);
        }
        if (normalized.contains("CZASU ODNOWIENIA")) {
            return "AFFIX:COOLDOWN_REDUCTION:" + firstNumber(normalized);
        }
        if (normalized.contains("OBRAZEN Z UPLYWEM CZASU")) {
            return "AFFIX:DAMAGE_OVER_TIME_MULTIPLIER:" + firstNumber(normalized);
        }
        if (normalized.contains("GDY MASZ UMOCNIENIE")) {
            return "ASPECT:FORTIFY_DAMAGE";
        }
        if (normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE")) {
            return "ASPECT:DAMAGE_INCREASED";
        }
        if (normalized.contains("TA PREMIA JEST TRZY RAZY WIEKSZA")) {
            return "ASPECT:STANDING_STILL_MULTIPLIER";
        }
        if (normalized.contains("ROZJUSZENIE")) {
            return "SEASONAL:ROZJUSZENIE:" + firstNumber(normalized);
        }
        return type.name() + ":" + normalized.replaceAll("\\s+", " ").trim();
    }

    private static boolean isLuckyHitChanceLine(String normalized, String collapsed) {
        if (collapsed.contains("PODSTAWOWEGOZASOBU")
                || collapsed.contains("PODSTAWOWYZASOB")
                || collapsed.contains("ODZYSKANIE")) {
            return false;
        }
        return normalized.contains("SZCZESLIWY TRAF")
                || normalized.contains("SZCZESNWY TRAF")
                || normalized.contains("SZANSY TRAF")
                || normalized.contains("SZANSY WY TRAF")
                || (collapsed.contains("SZANS") && collapsed.contains("TRAF") && !collapsed.contains("TRAFIENIEKRYTYCZNE"));
    }

    private static int lineQualityScore(FullItemReadLine line) {
        String text = line.getText() == null ? "" : line.getText();
        int score = text.length();
        String trimmedText = text == null ? "" : text.trim();
        if (trimmedText.startsWith("*")
                || trimmedText.startsWith("★")
                || trimmedText.startsWith("⭐")
                || trimmedText.startsWith("✦")
                || trimmedText.startsWith("✧")
                || trimmedText.startsWith("✱")
                || trimmedText.startsWith("✳")
                || trimmedText.startsWith("✴")
                || trimmedText.startsWith("✵")
                || trimmedText.startsWith("✶")
                || trimmedText.startsWith("✷")
                || trimmedText.startsWith("✸")
                || trimmedText.startsWith("✹")
                || trimmedText.startsWith("✺")
                || trimmedText.startsWith("✻")
                || trimmedText.startsWith("✼")
                || trimmedText.startsWith("✽")
                || trimmedText.startsWith("✾")
                || trimmedText.startsWith("❋")
                || trimmedText.startsWith("❂")
                || trimmedText.startsWith("◆")
                || trimmedText.startsWith("◇")
                || trimmedText.startsWith("♦")
                || trimmedText.startsWith("●")
                || trimmedText.startsWith("•")) {
            score += 300;
        }
        if (text.matches(".*[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ].*")) {
            score += 50;
        }
        if (text.contains("[") && text.contains("]")) {
            score += 20;
        }
        if (text.contains("%[x]")) {
            score += 30;
        }
        score += EffectTextTokenNormalizer.semanticTokenScore(text);
        if (text.contains("+[")) {
            score -= 3;
        }
        if (text.contains("1001") || text.contains("451") || text.contains("5061")) {
            score -= 5;
        }
        return score;
    }

    private static String firstNumber(String normalizedText) {
        Matcher matcher = Pattern.compile("[0-9]+(?:[,.][0-9]+)?").matcher(normalizedText);
        return matcher.find() ? matcher.group().replace(',', '.') : "";
    }

    private static String normalizeForDeduplication(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    private static <T> ItemImportFieldCandidate<T> mergeField(List<ItemImportFieldCandidate<T>> candidates,
                                                              String unknownNote) {
        List<ItemImportFieldCandidate<T>> knownCandidates = new ArrayList<>();
        for (ItemImportFieldCandidate<T> candidate : candidates) {
            if (candidate.getSuggestedValue() != null) {
                knownCandidates.add(candidate);
            }
        }
        if (knownCandidates.isEmpty()) {
            return ItemImportFieldCandidate.unknown(unknownNote);
        }

        List<FieldValueGroup<T>> groups = buildGroups(knownCandidates);
        groups.sort((left, right) -> {
            int byConfidence = Integer.compare(right.bestConfidenceScore(), left.bestConfidenceScore());
            if (byConfidence != 0) {
                return byConfidence;
            }
            int byOccurrences = Integer.compare(right.occurrences(), left.occurrences());
            if (byOccurrences != 0) {
                return byOccurrences;
            }
            return Integer.compare(left.firstIndex(), right.firstIndex());
        });

        FieldValueGroup<T> bestGroup = groups.getFirst();
        ItemImportFieldCandidate<T> bestCandidate = bestGroup.bestCandidate();
        if (groups.size() == 1) {
            return bestCandidate;
        }

        FieldValueGroup<T> secondGroup = groups.get(1);
        boolean sameRanking = bestGroup.bestConfidenceScore() == secondGroup.bestConfidenceScore()
                && bestGroup.occurrences() == secondGroup.occurrences();
        if (!sameRanking) {
            return bestCandidate;
        }

        return new ItemImportFieldCandidate<>(
                bestCandidate.getRawValue(),
                bestCandidate.getSuggestedValue(),
                lowerConfidence(bestCandidate.getConfidence()),
                bestCandidate.getNote() + " Równorzędne warianty OCR dały sprzeczne wartości, więc pole zostało zachowane z obniżoną pewnością."
        );
    }

    private static <T> List<FieldValueGroup<T>> buildGroups(List<ItemImportFieldCandidate<T>> candidates) {
        List<FieldValueGroup<T>> groups = new ArrayList<>();
        for (int index = 0; index < candidates.size(); index++) {
            ItemImportFieldCandidate<T> candidate = candidates.get(index);
            FieldValueGroup<T> existingGroup = findGroup(groups, candidate.getSuggestedValue());
            if (existingGroup == null) {
                groups.add(new FieldValueGroup<>(
                        candidate.getSuggestedValue(),
                        candidate,
                        confidenceScore(candidate.getConfidence()),
                        1,
                        index
                ));
                continue;
            }

            ItemImportFieldCandidate<T> bestCandidate = existingGroup.bestCandidate();
            int bestConfidenceScore = existingGroup.bestConfidenceScore();
            if (confidenceScore(candidate.getConfidence()) > bestConfidenceScore) {
                bestCandidate = candidate;
                bestConfidenceScore = confidenceScore(candidate.getConfidence());
            }
            groups.set(groups.indexOf(existingGroup), new FieldValueGroup<>(
                    existingGroup.suggestedValue(),
                    bestCandidate,
                    bestConfidenceScore,
                    existingGroup.occurrences() + 1,
                    existingGroup.firstIndex()
            ));
        }
        return groups;
    }

    private static <T> FieldValueGroup<T> findGroup(List<FieldValueGroup<T>> groups, T suggestedValue) {
        for (FieldValueGroup<T> group : groups) {
            if (Objects.equals(group.suggestedValue(), suggestedValue)) {
                return group;
            }
        }
        return null;
    }

    private static int confidenceScore(ItemImportFieldConfidence confidence) {
        return switch (confidence) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
            case UNKNOWN -> 0;
        };
    }

    private static ItemImportFieldConfidence lowerConfidence(ItemImportFieldConfidence confidence) {
        return switch (confidence) {
            case HIGH -> ItemImportFieldConfidence.MEDIUM;
            case MEDIUM -> ItemImportFieldConfidence.LOW;
            case LOW, UNKNOWN -> ItemImportFieldConfidence.UNKNOWN;
        };
    }

    private static String buildImportNotice(int analyzedVariantCount,
                                            ItemImportFieldCandidate<?>... candidates) {
        long recognizedCount = 0L;
        for (ItemImportFieldCandidate<?> candidate : candidates) {
            if (candidate.getSuggestedValue() != null) {
                recognizedCount++;
            }
        }
        if (recognizedCount == 0L) {
            return "OCR nie rozpoznał czytelnego tekstu z obrazu nawet po analizie "
                    + analyzedVariantCount
                    + " wariantów. Import nadal wymaga ręcznego potwierdzenia wszystkich pól.";
        }
        return "OCR rozpoznał " + recognizedCount + " z 7 pól foundation po analizie "
                + analyzedVariantCount
                + " wariantów obrazu. Wynik nadal wymaga ręcznego potwierdzenia użytkownika.";
    }

    private record FieldValueGroup<T>(T suggestedValue,
                                      ItemImportFieldCandidate<T> bestCandidate,
                                      int bestConfidenceScore,
                                      int occurrences,
                                      int firstIndex) {
    }

    private record WeaponDamageRangeSelection(Long min,
                                              Long max,
                                              Long average,
                                              int score,
                                              int sourceIndex,
                                              String reason,
                                              boolean dpsApsCoherent,
                                              Optional<Double> expectedDps,
                                              boolean rejected) {
    }

    private enum DetailTextField {
        ITEM_NAME,
        ITEM_TYPE,
        ITEM_RARITY,
        UNIQUE_EFFECT_TEXT
    }

    private enum DetailLongField {
        ITEM_POWER,
        WEAPON_DPS,
        WEAPON_DAMAGE_MIN,
        WEAPON_DAMAGE_MAX,
        AVERAGE_WEAPON_DAMAGE,
        ITEM_ARMOR
    }
}
