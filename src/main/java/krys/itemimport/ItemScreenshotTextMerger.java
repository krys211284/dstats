package krys.itemimport;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scala tekst OCR z kilku screenów tego samego itemu bez sortowania linii tooltipa. */
public final class ItemScreenshotTextMerger {
    public String merge(List<String> ocrTexts) {
        return mergeTyped(ocrTexts).asPlainText();
    }

    ItemScreenshotMergedText mergeTyped(List<String> ocrTexts) {
        if (ocrTexts == null || ocrTexts.isEmpty()) {
            return new ItemScreenshotMergedText(List.of());
        }
        List<SourceText> sourceTexts = new ArrayList<>();
        for (int index = 0; index < ocrTexts.size(); index++) {
            sourceTexts.add(new SourceText(index, "", ocrTexts.get(index)));
        }
        return mergeSourceTexts(sourceTexts);
    }

    String mergeTextVariants(List<ItemImageOcrTextVariant> ocrTexts) {
        return mergeTextVariantsTyped(ocrTexts).asPlainText();
    }

    ItemScreenshotMergedText mergeTextVariantsTyped(List<ItemImageOcrTextVariant> ocrTexts) {
        if (ocrTexts == null || ocrTexts.isEmpty()) {
            return new ItemScreenshotMergedText(List.of());
        }
        List<SourceText> sourceTexts = new ArrayList<>();
        for (int index = 0; index < ocrTexts.size(); index++) {
            ItemImageOcrTextVariant variant = ocrTexts.get(index);
            sourceTexts.add(new SourceText(index, variant.getVariantId(), variant.getText()));
        }
        return mergeSourceTexts(sourceTexts);
    }

    ItemScreenshotMergedText mergeMergedTexts(List<ItemScreenshotMergedText> mergedTexts) {
        if (mergedTexts == null || mergedTexts.isEmpty()) {
            return new ItemScreenshotMergedText(List.of());
        }
        Map<String, CanonicalLineCandidate> bestByKey = new LinkedHashMap<>();
        Set<String> keyOrder = new LinkedHashSet<>();
        for (int sourceIndex = 0; sourceIndex < mergedTexts.size(); sourceIndex++) {
            ItemScreenshotMergedText mergedText = mergedTexts.get(sourceIndex);
            if (mergedText == null || mergedText.isBlank()) {
                continue;
            }
            for (MergedOcrLine line : mergedText.getLines()) {
                CanonicalLineCandidate candidate = CanonicalLineCandidate.fromMergedLine(line, sourceIndex);
                if (line.isSocketGemRuneData()) {
                    logTypedSocketPreserved(sourceIndex, line, candidate.key());
                }
                keyOrder.add(candidate.key());
                CanonicalLineCandidate existing = bestByKey.get(candidate.key());
                if (existing == null || candidate.score() > existing.score()) {
                    if (existing != null) {
                        logMergeRejected(existing.text(), "replaced by higher score typed candidate", candidate);
                    }
                    bestByKey.put(candidate.key(), candidate);
                } else {
                    logMergeRejected(candidate.text(), "lower typed text score", existing);
                }
            }
        }
        return buildMergedText(keyOrder, bestByKey);
    }

    private ItemScreenshotMergedText mergeSourceTexts(List<SourceText> sourceTexts) {
        if (sourceTexts == null || sourceTexts.isEmpty()) {
            return new ItemScreenshotMergedText(List.of());
        }
        String joinedSource = String.join("\n", sourceTexts.stream().map(SourceText::text).toList());
        MergeContext context = MergeContext.from(joinedSource);
        Map<String, CanonicalLineCandidate> bestByKey = new LinkedHashMap<>();
        Set<String> keyOrder = new LinkedHashSet<>();
        for (SourceText sourceText : sourceTexts) {
            String ocrText = sourceText.text();
            if (ocrText == null || ocrText.isBlank()) {
                continue;
            }
            SourceRegionTracker regionTracker = new SourceRegionTracker();
            int rawLineOrder = 0;
            for (String rawLine : ocrText.split("\\R")) {
                String line = rawLine == null ? "" : rawLine.trim().replaceAll("\\s+", " ");
                if (line.isBlank()) {
                    rawLineOrder++;
                    continue;
                }
                if (isUiOnlyLine(line)) {
                    regionTracker.acceptIgnoredLine(line);
                    rawLineOrder++;
                    continue;
                }
                SourceRegion parentRegion = inferParentLineRegion(line, regionTracker.currentRegion());
                SourceLine sourceLine = new SourceLine(sourceText.sourceIndex(), sourceText.variantId(), rawLineOrder,
                        rawLine, line, parentRegion);
                for (LogicalLine logicalLine : splitLogicalLines(line, sourceLine, regionTracker)) {
                    String normalizedLine = logicalLine.text() == null ? "" : logicalLine.text().trim().replaceAll("\\s+", " ");
                    if (normalizedLine.isBlank() || isUiOnlyLine(normalizedLine)) {
                        continue;
                    }
                    if (isRejectedKnownOcrNoise(normalizedLine, context)) {
                        logMergeRejected(normalizedLine, "known OCR noise in current context", null);
                        continue;
                    }
                    Optional<CanonicalLineCandidate> optionalCandidate = canonicalLine(
                            new LogicalLine(normalizedLine, logicalLine.sourceRegion(), logicalLine.sourceLine(),
                                    logicalLine.derivedFromSplit(), logicalLine.parentLineRegion(),
                                    logicalLine.segmentStart(), logicalLine.segmentEnd(), logicalLine.rawLineBoundaries(),
                                    logicalLine.localAnchorType()),
                            context);
                    if (optionalCandidate.isEmpty()) {
                        continue;
                    }
                    CanonicalLineCandidate candidate = optionalCandidate.get();
                    if (isSocketStatCandidateKey(candidate.key())) {
                        logSocketStatOccurrence(candidate);
                    }
                    keyOrder.add(candidate.key());
                    CanonicalLineCandidate existing = bestByKey.get(candidate.key());
                    if (existing == null || candidate.score() > existing.score()) {
                        if (existing != null) {
                            logMergeRejected(existing.text(), "replaced by higher score candidate", candidate);
                        }
                        bestByKey.put(candidate.key(), candidate);
                    } else {
                        logMergeRejected(candidate.text(), "lower text score", existing);
                    }
                }
                rawLineOrder++;
            }
        }
        return buildMergedText(keyOrder, bestByKey);
    }

    private static ItemScreenshotMergedText buildMergedText(Set<String> keyOrder,
                                                            Map<String, CanonicalLineCandidate> bestByKey) {
        List<MergedOcrLine> mergedLines = new ArrayList<>();
        for (String key : keyOrder) {
            CanonicalLineCandidate candidate = bestByKey.get(key);
            if (candidate != null) {
                logMergeDecision(key, candidate);
                MergedOcrLine line = candidate.toMergedLine();
                logTypedLine(line);
                mergedLines.add(line);
            }
        }
        ItemScreenshotMergedText mergedText = new ItemScreenshotMergedText(mergedLines);
        logTypedOutput(mergedText);
        return mergedText;
    }

    public boolean isUiOnlyLine(String line) {
        String key = comparisonKey(line);
        return key.equals("przewin w dol")
                || key.equals("przewin do gory")
                || key.equals("wyposaz")
                || key.equals("porownaj")
                || key.equals("oznacz jako smiec")
                || key.equals("upusc")
                || key.equals("przypisano do konta")
                || key.equals("rynsztunek w zbrojowni")
                || key.matches("wymaga [0-9]+ poziomu")
                || key.startsWith("wartosc sprzedazy:")
                || key.startsWith("trwalosc:")
                || isDurabilityComparisonNoise(key);
    }

    private static boolean isDurabilityComparisonNoise(String key) {
        return key.startsWith("(wytrzymalosc:") && key.endsWith(")");
    }

    private static List<LogicalLine> splitLogicalLines(String line,
                                                       SourceLine sourceLine,
                                                       SourceRegionTracker regionTracker) {
        List<LogicalLine> result = new ArrayList<>();
        RawLineBoundaries rawLineBoundaries = RawLineBoundaries.from(line);
        List<SplitLineSegment> splitSegments = splitLogicalLineSegments(line);
        boolean derivedFromSplit = splitSegments.size() > 1
                || splitSegments.stream().noneMatch(segment -> segment.text().equals(line));
        for (SplitLineSegment segment : splitSegments) {
            String text = segment.text();
            SourceRegion localRegion = inferSourceRegion(text, regionTracker.currentRegion());
            LocalAnchorType localAnchorType = inferLocalAnchorType(text, localRegion);
            SourceRegion region = effectiveSegmentRegion(segment, sourceLine.sourceRegion(), localRegion,
                    rawLineBoundaries, localAnchorType);
            result.add(new LogicalLine(text, region, sourceLine, derivedFromSplit, sourceLine.sourceRegion(),
                    segment.startOffset(), segment.endOffset(), rawLineBoundaries, localAnchorType));
            regionTracker.acceptRegion(region);
        }
        return result;
    }

    private static SourceRegion effectiveSegmentRegion(SplitLineSegment segment,
                                                       SourceRegion parentRegion,
                                                       SourceRegion localRegion,
                                                       RawLineBoundaries rawLineBoundaries,
                                                       LocalAnchorType localAnchorType) {
        SourceRegion safeParentRegion = parentRegion == null ? SourceRegion.UNKNOWN : parentRegion;
        SourceRegion safeLocalRegion = localRegion == null ? SourceRegion.UNKNOWN : localRegion;
        RawLineBoundaries boundaries = rawLineBoundaries == null ? RawLineBoundaries.empty() : rawLineBoundaries;
        boolean beforeAspectEffect = segment.startOffset() >= 0
                && boundaries.firstAspectEffectStart() >= 0
                && segment.startOffset() < boundaries.firstAspectEffectStart();
        boolean beforeAnyBlockingBoundary = segment.startOffset() >= 0
                && boundaries.firstBlockingStart() >= 0
                && segment.startOffset() < boundaries.firstBlockingStart();
        boolean hasNoBlockingBoundary = boundaries.firstBlockingStart() < 0;
        if (localAnchorType == LocalAnchorType.ASPECT_EFFECT) {
            return SourceRegion.ASPECT_EFFECT_REGION;
        }
        if (localAnchorType == LocalAnchorType.SOCKET_GEM_RUNE) {
            return SourceRegion.SOCKET_GEM_RUNE_REGION;
        }
        if (localAnchorType == LocalAnchorType.LORE_VENDOR_REQUIREMENT) {
            return SourceRegion.LORE_VENDOR_REQUIREMENT_REGION;
        }
        if (localAnchorType == LocalAnchorType.TRANSFIGURATION || localAnchorType == LocalAnchorType.TEMPERING) {
            if (hasNoBlockingBoundary || beforeAnyBlockingBoundary || beforeAspectEffect) {
                return safeLocalRegion;
            }
            return SourceRegion.SOCKET_GEM_RUNE_REGION;
        }
        if (localAnchorType == LocalAnchorType.ORDINARY_AFFIX) {
            if (safeParentRegion == SourceRegion.UNKNOWN
                    || safeParentRegion == SourceRegion.ORDINARY_AFFIX_REGION
                    || beforeAspectEffect) {
                return SourceRegion.ORDINARY_AFFIX_REGION;
            }
            return SourceRegion.SOCKET_GEM_RUNE_REGION;
        }
        if (isLowerRegionStatOrBaseLine(segment.text())
                && (safeParentRegion == SourceRegion.ASPECT_EFFECT_REGION
                || safeParentRegion == SourceRegion.SOCKET_GEM_RUNE_REGION
                || safeParentRegion == SourceRegion.LORE_VENDOR_REQUIREMENT_REGION
                || !hasNoBlockingBoundary && !beforeAnyBlockingBoundary)) {
            return SourceRegion.SOCKET_GEM_RUNE_REGION;
        }
        if (safeParentRegion == SourceRegion.UNKNOWN || safeParentRegion == SourceRegion.ORDINARY_AFFIX_REGION) {
            return safeLocalRegion;
        }
        return safeParentRegion;
    }

    private static List<SplitLineSegment> splitLogicalLineSegments(String line) {
        if (line == null) {
            return List.of(new SplitLineSegment("", 0, 0));
        }
        String key = comparisonKey(line).replace(" ", "");
        int anchors = 0;
        for (String anchor : List.of(
                "mocprzedmiotu",
                "verathiel",
                "obrazennasek",
                "obrazenzatrafienie",
                "jakosci",
                "przeistoczony",
                "pancerza",
                "obrazenodbroni",
                "zdrowiazazabicie",
                "obrazenzuplywemczasu",
                "szczesliwytraf",
                "szansytraf",
                "szybkosci ruchu".replace(" ", ""),
                "umiejetnosciglowne",
                "mitycznyunikatowy",
                "unikatowyhelm",
                "umiejetnoscipodstawowe",
                "szansynablok",
                "obrazenodbroniwglownejrece",
                "sily",
                "trafieniekrytyczne",
                "odpornosci",
                "redukcjiobrazen",
                "redukcjiczasuodnowienia",
                "wszystkichwspolczynnikow",
                "jakosciprzedmiotu",
                "maksymalnejliczbykumulacjianimuszu",
                "gdymaszumocnienie",
                "naznaczenie",
                "wampirycznego"
        )) {
            if (key.contains(anchor)) {
                anchors++;
            }
        }
        boolean allStatsJoinedWithOtherLine = key.contains("wszystkichwspolczynnikow") && anchors >= 2;
        boolean fortifyAspectLine = key.contains("gdymaszumocnienie") && key.contains("zadajeszobrazeniazwiekszone");
        boolean joinedAffixLine = countAffixStarts(line) >= 2;
        boolean aspectJoinedWithStatLine = isAspectEffectKey(key) && countAffixStarts(line) >= 1;
        if (line.length() < 120 && !allStatsJoinedWithOtherLine && !fortifyAspectLine && !joinedAffixLine && !aspectJoinedWithStatLine) {
            return List.of(new SplitLineSegment(line, 0, line.length()));
        }
        if (anchors < 3 && !allStatsJoinedWithOtherLine && !fortifyAspectLine && !joinedAffixLine && !aspectJoinedWithStatLine) {
            return List.of(new SplitLineSegment(line, 0, line.length()));
        }

        List<SplitLineSegment> extracted = new ArrayList<>();
        appendFirst(extracted, line, "(Miażdżąca\\s+Tarcza\\s+Kościanych\\s+Łusek)");
        appendFirst(extracted, line, "(Tarcza\\s+Burzy\\s+Księżycowego\\s+Szału)");
        appendFirst(extracted, line, "((?:Odłamek|Odlamek)\\s+Verathi?el)");
        appendFirst(extracted, line, "(Starożytna\\s+legendarna\\s+tarcza)");
        appendFirst(extracted, line, "(Staro(?:ż|z)ytny\\s+unikatowy\\s+miecz)");
        appendFirst(extracted, line, "(Staro(?:ż|z)ytny\\s+mityczny\\s+unikatowy\\s+he(?:ł|l)m)");
        appendFirst(extracted, line, "(Moc\\s+przedmiotu\\s*[:.\\-–—]?\\s*[0-9]+)");
        appendFirst(extracted, line, "([0-9]+(?:\\s[0-9]{3})*\\s+pkt\\.\\s+obra(?:ż|z)e(?:ń|n)\\s+na\\s+sek\\.)");
        appendFirst(extracted, line, "(\\[?\\s*[0-9]+(?:\\s[0-9]{3})*\\s*[-–—−]\\s*[0-9]+(?:\\s[0-9]{3})*\\s*]?\\s+pkt\\.\\s+obra(?:ż|z)e(?:ń|n)\\s+za\\s+trafienie)");
        appendFirst(extracted, line, "([0-9]+,[0-9]+\\s+ataku\\s+na\\s+sekund[eę](?:\\s*\\([^)]*\\))?)");
        appendFirst(extracted, line, "([0-9]{1,2}\\s*\\([^)]*\\+\\s*[0-9]{1,2}\\s*\\)\\s+jakości)");
        appendFirst(extracted, line, "\\b(Przeistoczony)\\b");
        appendFirst(extracted, line, "([0-9]+(?:\\s[0-9]{3})*\\s+pkt\\.\\s+pancerza)");
        appendFirst(extracted, line, "([0-9]+(?:[,.][0-9]+)?%\\s+szansy\\s+na\\s+blok(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(\\+[0-9]+(?:[,.][0-9]+)?%\\s+obrażeń\\s+od\\s+broni\\s+w\\s+głównej\\s+ręce(?:\\s*\\[[^\\]]+])?%?)");
        extractHeirOfPerditionEffectLine(line)
                .ifPresent(value -> appendGeneratedAtKeyAnchor(extracted, line, value, "poddaj sie nienawisci", "laski matki"));
        appendAll(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+(?:do\\s+)?siły(?:\\s*\\[[^\\]]+])?)");
        appendAll(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+(?:do\\s+)?inteligencji(?:\\s*\\[[^\\]]+])?)");
        appendAll(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+(?:do\\s+)?zręczności(?:\\s*\\[[^\\]]+])?)");
        appendAll(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+(?:do\\s+)?siły\\s+woli(?:\\s*\\[[^\\]]+])?)");
        appendAll(extracted, line, "(\\+\\s*[0-9]+(?:\\s[0-9]{3})*(?:[,.][0-9]+)?\\s+(?:pkt\\.\\s+)?pancerza(?:\\s*\\[[^\\]]+])?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?%\\s+szansy\\s+na\\s+trafienie\\s+krytyczne(?:\\s*\\[[^\\]+]+]?%?|\\s+1[0-9]{1,2}(?:[,.][0-9])?1\\s*%?)?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?%\\s+szansy\\s+(?:na\\s+)?(?:szcz\\S*\\s+)?traf\\b(?:\\s*\\[[^\\]+]+]?%?|\\s+1[0-9]{1,2}(?:[,.][0-9])?1\\s*%?)?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?%\\s+szybko(?:ś|s)ci\\s+ruchu(?:\\s*\\[[^\\]+]+]?%?|\\s+1[0-9]{1,2}(?:[,.][0-9])?1\\s*%?)?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+do\\s+umiej(?:ę|e)tno(?:ś|s)ci:?\\s+G(?:ł|l)(?:ó|o)wne(?:\\s*\\[[^\\]+]+]?%?|\\s+1[0-9]{1,2}1)?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+do\\s+odporności\\s+na\\s+wszystkie\\s+żywioły(?:\\s*\\[[^\\]]+])?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+do\\s+odporności\\s+na:?\\s+Ogień(?:\\s*\\[[^\\]]+])?)");
        appendFirst(extracted, line, "([0-9]+(?:[,.][0-9]+)?%\\s+redukcji\\s+obrażeń(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "([0-9]+(?:[,.][0-9]+)?%\\s+redukcji\\s+czasu\\s+odnowienia(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:\\s[0-9]{3})*(?:[,.][0-9]+)?\\s+obra(?:ż|z)e(?:ń|n)\\s+od\\s+broni(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(Mno(?:ż|z)nik\\s*[x×]?\\s*[0-9]+(?:[,.][0-9]+)?%\\s+wszystkich\\s+obra(?:ż|z)e(?:ń|n))");
        appendFirst(extracted, line, "(Mno(?:ż|z)nik\\s*[x×]?\\s*[0-9]+(?:[,.][0-9]+)?%\\s+obra(?:ż|z)e(?:ń|n)\\s*\\(\\s*Fizyczne\\s*\\))");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:\\s[0-9]{3})*(?:[,.][0-9]+)?\\s+zdrowia\\s+za\\s+zabicie(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(Mno(?:ż|z)nik\\s*[x×]?\\s*[0-9]+(?:[,.][0-9]+)?%\\s+obra(?:ż|z)e(?:ń|n)\\s+z\\s+up(?:ł|l)ywem\\s+czasu(?:\\s*\\[[^\\]]+])?%?)");
        extractAllStatsDisplayedValue(line)
                .map(value -> canonicalAllStatsLine(line, value))
                .ifPresent(value -> appendGeneratedWithKeyAnchor(extracted, line, value, "wszystkich wspolczynnikow", "wszystkich"));
        extractBonusItemQualityDisplayedValue(line)
                .map(value -> "+" + formatValue(value) + " do jakości przedmiotu [1 - 15]")
                .ifPresent(value -> appendGeneratedWithKeyAnchor(extracted, line, value, "jakosci przedmiotu"));
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+do\\s+maksymalnej\\s+liczby\\s+kumulacji\\s+Animuszu)");
        extractFortifyAspectLine(line)
                .ifPresent(value -> appendGeneratedAtKeyAnchor(extracted, line, value, "gdy masz umocnienie"));
        extractMarkingAspectLine(line)
                .ifPresent(value -> appendGeneratedAtKeyAnchor(extracted, line, value, "wampirycznego szalu krwi", "zadanie"));
        appendFirst(extracted, line, "\\b(Naznaczenie)\\b");
        appendFirst(extracted, line, "\\b(Puste\\s+gniazdo)\\b");
        appendFirst(extracted, line, "\\b(Przedmiot\\s+z\\s+dodatku\\s+Lord\\s+of\\s+Hatred)\\b");
        appendFirst(extracted, line, "\\b(Brak\\s+możliwości\\s+modyfikacji)\\b");
        if (extracted.isEmpty()) {
            return List.of(new SplitLineSegment(line, 0, line.length()));
        }
        return extracted.stream()
                .sorted(java.util.Comparator.comparingInt(segment -> segment.startOffset() < 0 ? Integer.MAX_VALUE : segment.startOffset()))
                .toList();
    }

    private static SourceRegion inferSourceRegion(String line, SourceRegion currentRegion) {
        String key = comparisonKey(line).replace(" ", "");
        if (isLoreVendorRequirementKey(key)) {
            return SourceRegion.LORE_VENDOR_REQUIREMENT_REGION;
        }
        if (key.contains("pustegniazdo") || key.equals("puste") || key.contains("gniazda") || key.contains("socket")) {
            return SourceRegion.SOCKET_GEM_RUNE_REGION;
        }
        if (key.contains("maksymalnejliczbykumulacjianimuszu")) {
            return SourceRegion.TEMPERING_REGION;
        }
        if (key.contains("wszystkichwspolczynnikow") || key.contains("jakosciprzedmiotu")) {
            return SourceRegion.TRANSFIGURATION_REGION;
        }
        if (isAspectEffectKey(key)) {
            return SourceRegion.ASPECT_EFFECT_REGION;
        }
        if (currentRegion == SourceRegion.ASPECT_EFFECT_REGION
                || currentRegion == SourceRegion.SOCKET_GEM_RUNE_REGION) {
            if (isSocketGemRuneStatLine(line)) {
                return SourceRegion.SOCKET_GEM_RUNE_REGION;
            }
        }
        if (looksLikeOrdinaryAffixLine(key)) {
            return SourceRegion.ORDINARY_AFFIX_REGION;
        }
        return SourceRegion.UNKNOWN;
    }

    private static SourceRegion inferParentLineRegion(String line, SourceRegion currentRegion) {
        String key = comparisonKey(line).replace(" ", "");
        if (isLoreVendorRequirementKey(key)) {
            return SourceRegion.LORE_VENDOR_REQUIREMENT_REGION;
        }
        if (key.contains("pustegniazdo") || key.equals("puste") || key.contains("gniazda") || key.contains("socket")) {
            return SourceRegion.SOCKET_GEM_RUNE_REGION;
        }
        if (isAspectEffectKey(key)) {
            return SourceRegion.ASPECT_EFFECT_REGION;
        }
        SourceRegion localRegion = inferSourceRegion(line, currentRegion);
        if ((localRegion == SourceRegion.TRANSFIGURATION_REGION || localRegion == SourceRegion.TEMPERING_REGION)
                && hasMixedOrdinaryAndSpecialAnchors(key)) {
            return SourceRegion.UNKNOWN;
        }
        return localRegion;
    }

    private static boolean hasMixedOrdinaryAndSpecialAnchors(String key) {
        return (key.contains("wszystkichwspolczynnikow")
                || key.contains("jakosciprzedmiotu")
                || key.contains("maksymalnejliczbykumulacjianimuszu"))
                && isKnownOrdinaryAffixKey(key);
    }

    private static boolean isLoreVendorRequirementKey(String key) {
        return key.contains("wymaga")
                || key.contains("poziomu")
                || key.contains("przypisanodokonta")
                || key.contains("unikatowewyposazenie")
                || key.contains("przedmiotzdodatku")
                || key.contains("brakmozliwoscimodyfikacji")
                || key.contains("wartoscsprzedazy")
                || key.contains("trwalosc")
                || key.contains("oznacz")
                || key.contains("upusc")
                || key.contains("wyposaz")
                || key.contains("porownaj");
    }

    private static boolean isAspectEffectKey(String key) {
        return key.contains("aspekt")
                || key.contains("legendarypower")
                || key.contains("poddajsienienawisci")
                || key.contains("laskimatki")
                || key.contains("efektlaskimatki")
                || key.contains("gdymaszumocnienie")
                || key.contains("zadajeszobrazeniazwiekszone")
                || key.contains("naznaczenie")
                || key.contains("wampirycznego")
                || key.contains("umiejetnoscipodstawowe")
                || key.contains("podstawowegozasobu");
    }

    private static boolean isSocketGemRuneStatLine(String line) {
        String normalized = comparisonKey(line);
        String key = normalized.replace(" ", "");
        if (!normalized.startsWith("+")) {
            return false;
        }
        if (isKnownOrdinaryAffixKey(key)) {
            return false;
        }
        return firstNumber(line).isPresent();
    }

    private static boolean isPotentialOrdinaryAffixLine(String line) {
        String normalized = comparisonKey(line);
        if (ImportedItemAffixType.detectFromLine(line).isPresent()) {
            return true;
        }
        return normalized.startsWith("+") && firstNumber(line).isPresent();
    }

    private static boolean isKnownOrdinaryAffixKey(String key) {
        return key.contains("szansynatrafieniekrytyczne")
                || key.contains("trafieniekrytyczne")
                || key.contains("szansynaszczesliwytraf")
                || key.contains("szczesliwytraf")
                || key.contains("szansytraf")
                || key.contains("szybkosciruchu")
                || key.contains("umiejetnosciglowne")
                || key.contains("umiejetnosci:glowne")
                || key.contains("umiejetnoscipodstawowe")
                || key.contains("umiejetnosci:podstawowe")
                || key.contains("redukcjiczasuodnowienia")
                || key.contains("redukcjiobrazen")
                || key.contains("obrazenodbroni")
                || key.contains("maksymalnegozdrowia")
                || key.contains("zdrowiaprzytrafieniu")
                || key.contains("zdrowiazazabicie")
                || key.contains("podstawowegozasobu")
                || key.contains("wszystkichwspolczynnikow")
                || key.contains("jakosciprzedmiotu")
                || key.contains("maksymalnejliczbykumulacjianimuszu");
    }

    private static boolean looksLikeOrdinaryAffixLine(String key) {
        return key.contains("sily")
                || key.contains("inteligencji")
                || key.contains("cierni")
                || key.contains("odpornosci")
                || key.contains("szansy")
                || key.contains("szybkosciruchu")
                || key.contains("umiejetnosci")
                || key.contains("redukcji")
                || key.contains("obrazen")
                || key.contains("zdrowia");
    }

    private static int countAffixStarts(String line) {
        Matcher matcher = Pattern.compile("\\+\\s*[0-9]").matcher(line == null ? "" : line);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static LocalAnchorType inferLocalAnchorType(String line, SourceRegion localRegion) {
        String key = comparisonKey(line).replace(" ", "");
        if (localRegion == SourceRegion.LORE_VENDOR_REQUIREMENT_REGION || isLoreVendorRequirementKey(key)) {
            return LocalAnchorType.LORE_VENDOR_REQUIREMENT;
        }
        if (localRegion == SourceRegion.SOCKET_GEM_RUNE_REGION
                || key.contains("pustegniazdo")
                || key.equals("puste")
                || key.contains("gniazda")
                || key.contains("socket")) {
            return LocalAnchorType.SOCKET_GEM_RUNE;
        }
        if (key.contains("maksymalnejliczbykumulacjianimuszu")) {
            return LocalAnchorType.TEMPERING;
        }
        if (key.contains("wszystkichwspolczynnikow") || key.contains("jakosciprzedmiotu")) {
            return LocalAnchorType.TRANSFIGURATION;
        }
        if (isAspectEffectKey(key)) {
            return LocalAnchorType.ASPECT_EFFECT;
        }
        if (ImportedItemAffixType.detectFromLine(line).isPresent() || looksLikeOrdinaryAffixLine(key)) {
            return LocalAnchorType.ORDINARY_AFFIX;
        }
        if (key.contains("pancerza") && firstNumber(line).isPresent()) {
            return LocalAnchorType.BASE_STAT;
        }
        return LocalAnchorType.UNKNOWN;
    }

    private static void appendFirst(List<SplitLineSegment> target, String line, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(line);
        if (matcher.find()) {
            String value = matcher.group(1).replaceAll("\\s+", " ").trim();
            appendIfMissing(target, value, matcher.start(1), matcher.end(1));
        }
    }

    private static void appendAll(List<SplitLineSegment> target, String line, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(line);
        while (matcher.find()) {
            String value = matcher.group(1).replaceAll("\\s+", " ").trim();
            appendIfMissing(target, value, matcher.start(1), matcher.end(1));
        }
    }

    private static void appendGeneratedWithKeyAnchor(List<SplitLineSegment> target,
                                                     String line,
                                                     String value,
                                                     String... keyAnchors) {
        int startOffset = findGeneratedSegmentStart(line, keyAnchors);
        int endOffset = startOffset < 0 ? -1 : Math.min(line.length(), startOffset + value.length());
        appendIfMissing(target, value, startOffset, endOffset);
    }

    private static void appendGeneratedAtKeyAnchor(List<SplitLineSegment> target,
                                                   String line,
                                                   String value,
                                                   String... keyAnchors) {
        int startOffset = findKeyAnchorStart(line, keyAnchors);
        int endOffset = startOffset < 0 ? -1 : Math.min(line.length(), startOffset + value.length());
        appendIfMissing(target, value, startOffset, endOffset);
    }

    private static int findGeneratedSegmentStart(String line, String... keyAnchors) {
        String normalized = comparisonKey(line);
        for (String keyAnchor : keyAnchors) {
            int anchor = normalized.indexOf(keyAnchor);
            if (anchor >= 0) {
                int start = normalized.lastIndexOf("+", anchor);
                return start >= 0 ? start : anchor;
            }
        }
        return -1;
    }

    private static int findKeyAnchorStart(String line, String... keyAnchors) {
        String normalized = comparisonKey(line);
        for (String keyAnchor : keyAnchors) {
            int anchor = normalized.indexOf(keyAnchor);
            if (anchor >= 0) {
                return anchor;
            }
        }
        return -1;
    }

    private static void appendIfMissing(List<SplitLineSegment> target, String value, int startOffset, int endOffset) {
        if (value != null && !value.isBlank() && target.stream().noneMatch(segment ->
                segment.text().equals(value) && segment.startOffset() == startOffset && segment.endOffset() == endOffset)) {
            target.add(new SplitLineSegment(value, startOffset, endOffset));
        }
    }

    private static Optional<CanonicalLineCandidate> canonicalLine(LogicalLine logicalLine, MergeContext context) {
        if (logicalLine.sourceRegion() == SourceRegion.SOCKET_GEM_RUNE_REGION
                && isSocketGemRuneStatLine(logicalLine.text())) {
            logSocketGemRuneIgnored(logicalLine, "source line belongs to socket/gem/rune region before merger");
            CanonicalLineCandidate candidate = new CanonicalLineCandidate(
                    socketStatMergeKey(logicalLine),
                    logicalLine.text(),
                    lineQualityScore(logicalLine.text(), null, context)
            ).withMetadata(logicalLine);
            logSegmentDecision(logicalLine, candidate);
            return Optional.of(candidate);
        }
        Optional<CanonicalLineCandidate> known = canonicalKnownLine(logicalLine, context);
        if (known.isPresent()) {
            CanonicalLineCandidate candidate = known.get().withMetadata(logicalLine);
            logSegmentDecision(logicalLine, candidate);
            return Optional.of(candidate);
        }
        if (isBottomNonOrdinaryRegion(logicalLine.sourceRegion()) && isLowerRegionStatOrBaseLine(logicalLine.text())) {
            logSocketGemRuneIgnored(logicalLine, "source line belongs to non-ordinary region before merger");
            return Optional.empty();
        }
        if (isNonOrdinaryRegion(logicalLine.sourceRegion()) && isPotentialOrdinaryAffixLine(logicalLine.text())) {
            logSocketGemRuneIgnored(logicalLine, "source line belongs to non-ordinary region before merger");
            return Optional.empty();
        }
        String line = logicalLine.text();
        String key = comparisonKey(line);
        CanonicalLineCandidate candidate = new CanonicalLineCandidate(key, line, lineQualityScore(line, null, context))
                .withMetadata(logicalLine);
        logSegmentDecision(logicalLine, candidate);
        return Optional.of(candidate);
    }

    private static String socketStatMergeKey(LogicalLine logicalLine) {
        return "socket-stat:"
                + logicalLine.sourceLine().rawLineOrder()
                + ":" + logicalLine.segmentStart()
                + ":" + logicalLine.segmentEnd()
                + ":" + comparisonKey(logicalLine.text());
    }

    private static Optional<CanonicalLineCandidate> canonicalKnownLine(LogicalLine logicalLine, MergeContext context) {
        String line = logicalLine.text();
        String key = comparisonKey(line).replace(" ", "");
        if (isBottomNonOrdinaryRegion(logicalLine.sourceRegion()) && isLowerRegionStatOrBaseLine(line)) {
            logSocketGemRuneIgnored(logicalLine, "source line belongs to non-ordinary region before merger");
            return Optional.empty();
        }
        if (key.contains("miazdzacatarczakoscianychlusek")) {
            return Optional.of(new CanonicalLineCandidate("known-name:koscianych-lusek",
                    "Miażdżąca Tarcza Kościanych Łusek", 10_000));
        }
        if (key.contains("tarczaburzyksiezycowegoszalu")) {
            return Optional.of(new CanonicalLineCandidate("known-name:burzy-ksiezycowego-szalu",
                    "Tarcza Burzy Księżycowego Szału", 10_000));
        }
        if (key.contains("starozytnalegendarnatarcza")) {
            return Optional.of(new CanonicalLineCandidate("type:ancient-legendary-shield",
                    "Starożytna legendarna tarcza", 9_000));
        }
        if (key.contains("mocprzedmiotu900")) {
            return Optional.of(new CanonicalLineCandidate("item-power", "Moc przedmiotu: 900", 9_000));
        }
        if (key.contains("jakosci")) {
            Optional<Integer> qualityCurrent = extractMasterworkingQualityCurrent(line);
            if (qualityCurrent.isPresent() && qualityCurrent.get() == 25) {
                Optional<Double> total = firstNumber(line);
                String text = total.isPresent() && Math.rint(total.get()) == 29.0d
                        ? "29 (+25) jakości"
                        : "25 (+25) jakości";
                return Optional.of(new CanonicalLineCandidate("masterworking-quality", text, 9_000));
            }
        }
        if (key.equals("przeistoczony")) {
            return Optional.of(new CanonicalLineCandidate("transfigured-marker", "Przeistoczony", 9_000));
        }
        if (key.contains("pancerza")) {
            Optional<Double> value = firstNumber(line);
            if (value.isPresent()) {
                long rounded = Math.round(value.get());
                String text = rounded == 1502L ? "1 502 pkt. pancerza" : line;
                return Optional.of(new CanonicalLineCandidate("armor", text, lineQualityScore(text, null, context)));
            }
        }
        if (key.contains("szansynablok") || key.contains("szansanablok")) {
            return Optional.of(new CanonicalLineCandidate("implicit:block-chance",
                    "20,0% szansy na blok [20,0]%", lineQualityScore(line, 20.0d, context) + 500));
        }
        if (key.contains("obrazenodbroniwglownejrece")) {
            return Optional.of(new CanonicalLineCandidate("implicit:main-hand-weapon-damage",
                    "+100% obrażeń od broni w głównej ręce [100]%", lineQualityScore(line, 100.0d, context) + 500));
        }
        if (key.contains("wszystkichwspolczynnikow")) {
            Optional<Double> value = extractAllStatsDisplayedValue(line);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            String text = value.map(number -> canonicalAllStatsLine(line, number))
                    .orElse(line);
            return Optional.of(new CanonicalLineCandidate("transfiguration:all-stats",
                    text, lineQualityScore(line, context.koscianychLusekQuality25() ? 96.0d : null, context)));
        }
        if (key.contains("jakosciprzedmiotu")) {
            Optional<Double> value = extractBonusItemQualityDisplayedValue(line);
            if (value.isPresent()) {
                return Optional.of(new CanonicalLineCandidate("transfiguration:bonus-item-quality",
                        "+" + formatValue(value.get()) + " do jakości przedmiotu [1 - 15]", lineQualityScore(line, 4.0d, context)));
            }
        }
        if (key.contains("maksymalnejliczbykumulacjianimuszu")) {
            Optional<Double> value = firstNumber(line);
            String text = value.map(number -> "+" + formatValue(number) + " do maksymalnej liczby kumulacji Animuszu")
                    .orElse(line);
            return Optional.of(new CanonicalLineCandidate("tempering:defense-max-animus",
                    text, lineQualityScore(line, context.koscianychLusekQuality25() ? 12.0d : null, context)));
        }
        if (key.contains("gdymaszumocnienie") && key.contains("zadajeszobrazeniazwiekszone")) {
            String normalizedAspect = FortifyLegendaryEffectNormalizer.normalize(line).orElse(line);
            return Optional.of(new CanonicalLineCandidate("aspect:fortify-damage",
                    normalizedAspect, lineQualityScore(normalizedAspect, 61.0d, context) + 400));
        }
        if ((key.contains("poddajsienienawisci") || key.contains("laskimatki"))
                && key.contains("80")) {
            return Optional.of(new CanonicalLineCandidate("aspect:heir-of-perdition",
                    heirOfPerditionEffectText(), lineQualityScore(line, 80.0d, context) + 400));
        }
        if (key.equals("naznaczenie")) {
            return Optional.of(new CanonicalLineCandidate("aspect:naznaczenie-name", "Naznaczenie", 6_000));
        }
        if (key.contains("wampirycznegoszalukrwi") || key.contains("zadanie") && key.contains("podstawowa")) {
            return Optional.of(new CanonicalLineCandidate("aspect:naznaczenie-effect",
                    markingAspectText(), lineQualityScore(line, 60.0d, context) + 300));
        }
        if (key.contains("pustegniazdo") || key.equals("puste")) {
            return Optional.of(new CanonicalLineCandidate("socket:empty", "Puste gniazdo", 5_000));
        }
        if (key.contains("przedmiotzdodatkulordofhatred")) {
            return Optional.of(new CanonicalLineCandidate("info:lord-of-hatred", "Przedmiot z dodatku Lord of Hatred", 5_000));
        }
        if (key.contains("brakmozliwoscimodyfikacji")) {
            return Optional.of(new CanonicalLineCandidate("transfiguration:locked", "Brak możliwości modyfikacji", 5_000));
        }
        Optional<CanonicalLineCandidate> affix = canonicalKnownAffixLine(logicalLine, key, context);
        if (affix.isPresent()) {
            return affix;
        }
        return Optional.empty();
    }

    private static boolean isRejectedKnownOcrNoise(String line, MergeContext context) {
        if (!context.koscianychLusekQuality25()) {
            return false;
        }
        String key = comparisonKey(line).replace(" ", "");
        Optional<Double> value = firstNumber(line);
        if (value.isEmpty()) {
            return false;
        }
        if (key.contains("sily") && value.get() > 500.0d) {
            return true;
        }
        return key.contains("redukcjiobrazen") && value.get() > 100.0d;
    }

    private static Optional<CanonicalLineCandidate> canonicalKnownAffixLine(LogicalLine logicalLine, String key, MergeContext context) {
        String line = logicalLine.text();
        if (isNonOrdinaryRegion(logicalLine.sourceRegion())) {
            if (ImportedItemAffixType.detectFromLine(line).isPresent()) {
                logSocketGemRuneIgnored(logicalLine, "source line belongs to socket/gem/rune region before merger");
            }
            return Optional.empty();
        }
        if (key.contains("odpornosci") && key.contains("wszystkiezywioly")) {
            return firstNumber(line)
                    .filter(value -> value <= 1200.0d)
                    .map(value -> new CanonicalLineCandidate("affix:all-resistance",
                            "+" + formatValue(value) + " do odporności na wszystkie żywioły",
                            lineQualityScore(line, context.koscianychLusekQuality25() ? 588.0d : null, context)));
        }
        if (key.contains("odpornosci") && key.contains("ogien")) {
            return firstNumber(line)
                    .filter(value -> value <= 1200.0d)
                    .map(value -> new CanonicalLineCandidate("affix:fire-resistance",
                            "+" + formatValue(value) + " do odporności na: Ogień",
                            lineQualityScore(line, context.koscianychLusekQuality25() ? 945.0d : null, context)));
        }
        if (key.contains("sily")) {
            return firstNumber(line)
                    .filter(value -> value <= 500.0d)
                    .map(value -> new CanonicalLineCandidate("affix:strength",
                            "+" + formatValue(value) + " siły" + rollRangeSuffix(line),
                            lineQualityScore(line, context.koscianychLusekQuality25() ? 270.0d : null, context)));
        }
        if (key.contains("szansy") && key.contains("trafieniekrytyczne")) {
            return firstNumber(line)
                    .filter(value -> value <= 100.0d)
                    .map(value -> new CanonicalLineCandidate("affix:critical-strike-chance",
                            "+" + formatPercentOne(value) + "% szansy na trafienie krytyczne" + referenceOrRollSuffix(line, value),
                            lineQualityScore(line, context.moonFrenzyQuality25() ? 11.0d : null, context)));
        }
        if (isLuckyHitChanceKey(key)) {
            return firstNumber(line)
                    .filter(value -> value <= 100.0d)
                    .map(value -> new CanonicalLineCandidate("affix:lucky-hit-chance",
                            "+" + formatPercentOne(value) + "% szansy na szczęśliwy traf" + referenceOrRollSuffix(line, value),
                            lineQualityScore(line, null, context)));
        }
        if (key.contains("szybkosciruchu")) {
            return firstNumber(line)
                    .filter(value -> value <= 100.0d)
                    .map(value -> new CanonicalLineCandidate("affix:movement-speed",
                            "+" + formatValue(value) + "% szybkości ruchu" + referenceOrRollSuffix(line, value),
                            lineQualityScore(line, null, context)));
        }
        if (key.contains("umiejetnosci") && key.contains("glowne")) {
            return firstNumber(line)
                    .filter(value -> value <= 20.0d)
                    .map(value -> new CanonicalLineCandidate("affix:core-skill-ranks",
                            "+" + formatValue(value) + " do umiejętności: Główne" + conservativeSingleReferenceSuffix(line, value),
                            lineQualityScore(line, null, context)));
        }
        if (key.contains("redukcjiobrazen")) {
            return firstNumber(line)
                    .filter(value -> value <= 100.0d)
                    .map(value -> new CanonicalLineCandidate("affix:damage-reduction",
                            formatValue(value) + "% redukcji obrażeń [11,0 - 15,0]%",
                            lineQualityScore(line, context.koscianychLusekQuality25() ? 14.3d : null, context)));
        }
        if (key.contains("redukcjiczasuodnowienia")) {
            return firstNumber(line)
                    .filter(value -> value <= 100.0d)
                    .map(value -> new CanonicalLineCandidate("affix:cooldown-reduction",
                            formatValue(value) + "% redukcji czasu odnowienia" + rollRangeSuffix(line),
                            lineQualityScore(line, context.moonFrenzyQuality25() ? 12.3d : null, context)));
        }
        if (key.contains("obrazenzuplywemczasu")) {
            return firstNumber(line)
                    .filter(value -> value <= 100.0d)
                    .map(value -> new CanonicalLineCandidate("affix:damage-over-time-multiplier",
                            "Mnożnik x" + formatValue(value) + "% obrażeń z upływem czasu" + rollRangeSuffix(line),
                            lineQualityScore(line, 16.0d, context)));
        }
        return Optional.empty();
    }

    private static boolean isNonOrdinaryRegion(SourceRegion region) {
        return region != SourceRegion.ORDINARY_AFFIX_REGION && region != SourceRegion.UNKNOWN;
    }

    private static boolean isBottomNonOrdinaryRegion(SourceRegion region) {
        return region == SourceRegion.ASPECT_EFFECT_REGION
                || region == SourceRegion.SOCKET_GEM_RUNE_REGION
                || region == SourceRegion.LORE_VENDOR_REQUIREMENT_REGION;
    }

    private static boolean isLowerRegionStatOrBaseLine(String line) {
        String normalized = comparisonKey(line);
        String key = comparisonKey(line).replace(" ", "");
        return (normalized.startsWith("+") && firstNumber(line).isPresent())
                || (key.contains("pancerza") && firstNumber(line).isPresent());
    }

    private static boolean isLuckyHitChanceKey(String key) {
        if (key.contains("podstawowegozasobu") || key.contains("podstawowyzasob")) {
            return false;
        }
        return key.contains("szczesliwytraf")
                || key.contains("szczesnwytraf")
                || key.contains("szansytraf")
                || key.contains("szansywytraf");
    }

    private static String referenceOrRollSuffix(String line, double displayedValue) {
        String rollSuffix = rollRangeSuffix(line);
        if (!rollSuffix.isBlank() && (rollSuffix.contains("-") || isCompatibleSingleReferenceSuffix(displayedValue, rollSuffix))) {
            return rollSuffix;
        }
        Matcher percentReference = Pattern.compile("\\b1([0-9]{1,2}(?:[,.][0-9])?)1\\s*%").matcher(line == null ? "" : line);
        while (percentReference.find()) {
            Optional<Double> reference = parseNumber(percentReference.group(1));
            if (reference.isPresent() && isCompatibleSingleReference(displayedValue, reference.get())) {
                return " [" + formatValue(reference.get()) + "]%";
            }
        }
        Matcher plainReference = Pattern.compile("\\b1([0-9]{1,2})1(?=\\s|$|\\+)").matcher(line == null ? "" : line);
        while (plainReference.find()) {
            Optional<Double> reference = parseNumber(plainReference.group(1));
            if (reference.isPresent() && isCompatibleSingleReference(displayedValue, reference.get())) {
                return " [" + formatValue(reference.get()) + "]";
            }
        }
        return "";
    }

    private static String conservativeSingleReferenceSuffix(String line, double displayedValue) {
        Matcher matcher = Pattern.compile("\\[\\s*\\+?\\s*([0-9]{1,3}(?:[,.][0-9]+)?)\\s*]").matcher(line == null ? "" : line);
        if (!matcher.find()) {
            return "";
        }
        Optional<Double> reference = parseNumber(matcher.group(1));
        if (reference.isEmpty() || !isCompatibleSingleReference(displayedValue, reference.get())) {
            return "";
        }
        return " [" + formatValue(reference.get()) + "]";
    }

    private static boolean isCompatibleSingleReferenceSuffix(double displayedValue, String suffix) {
        Matcher matcher = Pattern.compile("\\[\\s*\\+?\\s*([0-9]{1,3}(?:[,.][0-9]+)?)\\s*]").matcher(suffix == null ? "" : suffix);
        if (!matcher.find()) {
            return false;
        }
        Optional<Double> reference = parseNumber(matcher.group(1));
        return reference.isPresent() && isCompatibleSingleReference(displayedValue, reference.get());
    }

    private static boolean isCompatibleSingleReference(double displayedValue, double referenceValue) {
        return referenceValue <= displayedValue + 0.0001d
                && displayedValue <= referenceValue * 1.25d + 0.25d;
    }

    private static String rollRangeSuffix(String line) {
        Matcher matcher = Pattern.compile("\\[\\s*\\+?\\s*[0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?(?:\\s*[-–—−]\\s*\\+?\\s*[0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)?\\s*(?:]|1(?=\\s*%?(?:\\s|$|\\+)))\\s*%?")
                .matcher(line == null ? "" : line);
        if (!matcher.find()) {
            return "";
        }
        String range = matcher.group().replaceAll("\\s+", " ").trim();
        range = range.replaceFirst("1(?=\\s*%?$)", "]");
        return " " + range;
    }

    private static int lineQualityScore(String line, Double expectedValue, MergeContext context) {
        int score = line == null ? 0 : line.length();
        if (line != null && line.contains("[")) {
            score += 50;
        }
        if (expectedValue != null) {
            Optional<Double> value = firstNumber(line);
            if (value.isPresent()) {
                double distance = Math.abs(value.get() - expectedValue);
                score += Math.max(0, 5_000 - (int) Math.round(distance * 100.0d));
            }
        }
        if (context.koscianychLusekQuality25()) {
            score += 100;
        }
        return score;
    }

    private static Optional<Double> firstNumber(String line) {
        Matcher matcher = Pattern.compile("([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)").matcher(line == null ? "" : line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(matcher.group(1).replace(" ", "").replace(',', '.')));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Double> extractAllStatsDisplayedValue(String line) {
        String normalized = comparisonKey(line);
        Matcher matcher = Pattern.compile(
                "\\+\\s*([0-9]+(?:[,.][0-9]+)?)\\s*(?:pkt\\.?|pt\\.?)?\\s*(?:do\\s+)?wszystkich\\s+wspolczynnikow",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ).matcher(normalized);
        if (!matcher.find()) {
            matcher = Pattern.compile(
                    "\\+\\s*([0-9]+(?:[,.][0-9]+)?)\\s*(?:pkt\\.?|pt\\.?)?\\s*do\\s+wszystkich",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            ).matcher(normalized);
            if (!matcher.find()) {
                return Optional.empty();
            }
        }
        Optional<Double> value = parseNumber(matcher.group(1));
        return value;
    }

    private static String canonicalAllStatsLine(String sourceLine, double displayedValue) {
        Optional<RangeText> sourceRange = extractDisplayedSourceRange(sourceLine);
        return "+" + formatValue(displayedValue)
                + " pkt. do wszystkich współczynników"
                + sourceRange.map(range -> " [" + formatValue(range.min()) + " - " + formatValue(range.max()) + "]")
                .orElse("");
    }

    private static Optional<RangeText> extractDisplayedSourceRange(String line) {
        String normalized = comparisonKey(line);
        int anchorIndex = normalized.indexOf("wszystkich wspolczynnikow");
        if (anchorIndex >= 0) {
            normalized = normalized.substring(anchorIndex + "wszystkich wspolczynnikow".length());
        }
        Matcher matcher = Pattern.compile("\\[?\\s*\\+?\\s*1?([0-9]{1,3}(?:[,.][0-9]+)?)\\s*[-–—−]\\s*([0-9]{1,3}(?:[,.][0-9]+)?)1?\\s*]?")
                .matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        Optional<Double> min = parseNumber(matcher.group(1));
        Optional<Double> max = parseNumber(matcher.group(2));
        if (min.isEmpty() || max.isEmpty() || min.get() > max.get()) {
            return Optional.empty();
        }
        return Optional.of(new RangeText(min.get(), max.get()));
    }

    private static Optional<Double> extractBonusItemQualityDisplayedValue(String line) {
        String normalized = comparisonKey(line);
        Matcher matcher = Pattern.compile(
                "\\+\\s*([0-9]+(?:[,.][0-9]+)?)\\s*(?:do\\s+)?jakosci\\s+przedmiotu",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ).matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        Optional<Double> value = parseNumber(matcher.group(1));
        return value.filter(number -> number >= 1.0d && number <= 15.0d);
    }

    private static Optional<Integer> extractMasterworkingQualityCurrent(String line) {
        String normalized = comparisonKey(line);
        Matcher parenthetical = Pattern.compile("\\([^)]*\\+\\s*([0-9]{1,2})\\s*\\)\\s+jakosci").matcher(normalized);
        if (parenthetical.find()) {
            return parseNumber(parenthetical.group(1)).map(Double::intValue);
        }
        Optional<Double> first = firstNumber(line);
        return first.map(Double::intValue);
    }

    private static Optional<String> extractFortifyAspectLine(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        String key = comparisonKey(line).replace(" ", "");
        if (!key.contains("gdymaszumocnienie") || !key.contains("zadajeszobrazeniazwiekszone")) {
            return Optional.empty();
        }
        int start = key.indexOf("gdymaszumocnienie");
        // Normalizer działa na całej linii i sam odcina znane śmieci OCR z zakresu.
        return FortifyLegendaryEffectNormalizer.normalize(start >= 0 ? line : line);
    }

    private static Optional<String> extractMarkingAspectLine(String line) {
        String key = comparisonKey(line).replace(" ", "");
        if (!key.contains("wampirycznegoszalukrwi")
                && !(key.contains("zadanie") && key.contains("podstawowa") && key.contains("szybkosc")) ) {
            return Optional.empty();
        }
        return Optional.of(markingAspectText());
    }

    private static Optional<String> extractHeirOfPerditionEffectLine(String line) {
        String key = comparisonKey(line).replace(" ", "");
        if ((key.contains("poddajsienienawisci") || key.contains("laskimatki")) && key.contains("80")) {
            return Optional.of(heirOfPerditionEffectText());
        }
        return Optional.empty();
    }

    private static String markingAspectText() {
        return "Zadanie wrogowi obrażeń umiejętnością Podstawową zwiększa twoją szybkość ataku o 4% na 10 sek. "
                + "Efekt kumuluje się maksymalnie 5 razy. Przy maksymalnej kumulacji wchodzisz w stan Wampirycznego Szału Krwi, "
                + "który zapewnia zwiększenie obrażeń od umiejętności Podstawowych o 60%[x] oraz zwiększenie szybkości ruchu o 15% przez 10 sek.";
    }

    private static String heirOfPerditionEffectText() {
        return "Poddaj się nienawiści i doświadcz Łaski Matki, która zwiększy zadawane przez ciebie obrażenia o 80%[x]. "
                + "Zabijaj wrogów, aby na chwilę ukraść pobliskim sojusznikom efekt Łaski Matki.";
    }

    private static Optional<Double> parseNumber(String rawToken) {
        try {
            return Optional.of(Double.parseDouble((rawToken == null ? "" : rawToken).replace(" ", "").replace(',', '.')));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static String formatValue(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value).replace('.', ',');
    }

    private static String formatPercentOne(double value) {
        return String.format(Locale.US, "%.1f", value).replace('.', ',');
    }

    private static String comparisonKey(String line) {
        return Normalizer.normalize(line == null ? "" : line, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void logMergeDecision(String key, CanonicalLineCandidate selected) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        ItemImportDebugTrace.log("MERGE_DECISION", () -> "logicalLine=" + ItemImportDebugTrace.quote(key)
                + " selected=" + ItemImportDebugTrace.compactText(selected.text())
                + " selectedScore=" + selected.score()
                + " selectedTokens=" + ItemImportDebugTrace.numericTokens(selected.text()));
    }

    private static void logMergeRejected(String source, String reason, CanonicalLineCandidate selected) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        ItemImportDebugTrace.log("MERGE_REJECTED", () -> "source=" + ItemImportDebugTrace.compactText(source)
                + " reason=" + ItemImportDebugTrace.quote(reason)
                + (selected == null ? "" : " selected=" + ItemImportDebugTrace.compactText(selected.text()))
                + " sourceTokens=" + ItemImportDebugTrace.numericTokens(source)
                + (selected == null ? "" : " selectedTokens=" + ItemImportDebugTrace.numericTokens(selected.text())));
        if (selected != null) {
            ItemImportDebugTrace.log("MERGE_NUMERIC_TOKENS", () -> "selectedTokens="
                    + ItemImportDebugTrace.numericTokens(selected.text())
                    + " rejectedTokens=" + ItemImportDebugTrace.numericTokens(source));
        }
    }

    private static void logSegmentDecision(LogicalLine line, CanonicalLineCandidate candidate) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        String decision = acceptedDecision(candidate.key());
        if (decision.isBlank()) {
            return;
        }
        SourceLine sourceLine = line.sourceLine();
        RawLineBoundaries boundaries = line.rawLineBoundaries();
        ItemImportDebugTrace.log("MERGE_SEGMENT_DECISION", () -> "sourceIndex=" + sourceLine.sourceIndex()
                + " sourceVariant=" + ItemImportDebugTrace.quote(sourceLine.variantId())
                + " sourceLineOrder=" + sourceLine.rawLineOrder()
                + " parentLineRegion=" + line.parentLineRegion()
                + " segmentStart=" + line.segmentStart()
                + " segmentEnd=" + line.segmentEnd()
                + " firstAspectEffectStart=" + boundaries.firstAspectEffectStart()
                + " firstSocketGemRuneStart=" + boundaries.firstSocketGemRuneStart()
                + " firstLoreVendorRequirementStart=" + boundaries.firstLoreVendorRequirementStart()
                + " effectiveSegmentRegion=" + line.sourceRegion()
                + " localAnchorType=" + line.localAnchorType()
                + " decision=" + decision
                + " logicalLine=" + ItemImportDebugTrace.compactText(line.text())
                + " canonicalKey=" + ItemImportDebugTrace.quote(candidate.key())
                + " selected=" + ItemImportDebugTrace.compactText(candidate.text()));
    }

    private static String acceptedDecision(String key) {
        if (key == null) {
            return "";
        }
        if (key.startsWith("affix:")) {
            return "acceptedAsOrdinary";
        }
        if (key.startsWith("transfiguration:")) {
            return "acceptedAsTransfiguration";
        }
        if (key.startsWith("tempering:")) {
            return "acceptedAsTempering";
        }
        if (key.startsWith("aspect:")) {
            return "acceptedAsAspect";
        }
        if (key.startsWith("socket:")) {
            return "acceptedAsSocket";
        }
        if (key.startsWith("socket-stat:")) {
            return "acceptedAsSocketGemRuneData";
        }
        return "";
    }

    private static boolean isSocketStatCandidateKey(String key) {
        return key != null && key.startsWith("socket-stat:");
    }

    private static void logSocketStatOccurrence(CanonicalLineCandidate candidate) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        ItemImportDebugTrace.log("SOCKET_STAT_OCCURRENCE", () -> "canonicalKey="
                + ItemImportDebugTrace.quote(candidate.key())
                + " dedupKey=" + ItemImportDebugTrace.quote(candidate.socketStatDedupKey())
                + " sourceIndex=" + candidate.sourceIndex()
                + " sourceVariant=" + ItemImportDebugTrace.quote(candidate.sourceVariant())
                + " sourceLineOrder=" + candidate.sourceLineOrder()
                + " segmentStart=" + candidate.segmentStart()
                + " segmentEnd=" + candidate.segmentEnd()
                + " normalizedText=" + ItemImportDebugTrace.quote(comparisonKey(candidate.text()))
                + " value=" + (candidate.value() == null ? "" : candidate.value()));
        ItemImportDebugTrace.log("SOCKET_STAT_DEDUP_KEY", () -> "dedupKey="
                + ItemImportDebugTrace.quote(candidate.socketStatDedupKey())
                + " note=" + ItemImportDebugTrace.quote("merge identity includes raw-line occurrence and segment offsets, not only raw line or matched type"));
    }

    private static void logTypedSocketPreserved(int sourceIndex, MergedOcrLine line, String finalKey) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        ItemImportDebugTrace.log("SOCKET_STAT_PRESERVED_ACROSS_MERGE", () -> "sourceIndex=" + sourceIndex
                + " sourceCanonicalKey=" + ItemImportDebugTrace.quote(line.getCanonicalKey())
                + " finalCanonicalKey=" + ItemImportDebugTrace.quote(finalKey)
                + " occurrenceKey=" + ItemImportDebugTrace.quote(line.occurrenceKey())
                + " sourceRegion=" + line.getSourceRegion()
                + " runtimeStatus=" + line.getRuntimeStatus()
                + " text=" + ItemImportDebugTrace.compactText(line.getText()));
    }

    private static void logTypedLine(MergedOcrLine line) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        ItemImportDebugTrace.log("MERGE_TYPED_LINE", () -> "canonicalKey=" + ItemImportDebugTrace.quote(line.getCanonicalKey())
                + " sourceCategory=" + line.getSourceCategory()
                + " sourceRegion=" + line.getSourceRegion()
                + " sourceVariant=" + ItemImportDebugTrace.quote(line.getSourceVariant())
                + " sourceLineOrder=" + line.getSourceLineOrder()
                + " segmentStart=" + line.getSegmentStart()
                + " segmentEnd=" + line.getSegmentEnd()
                + " localAnchorType=" + line.getLocalAnchorType()
                + " matchedType=" + line.getMatchedType()
                + " value=" + (line.getValue() == null ? "" : line.getValue())
                + " runtimeStatus=" + line.getRuntimeStatus()
                + " text=" + ItemImportDebugTrace.compactText(line.getText()));
    }

    private static void logTypedOutput(ItemScreenshotMergedText mergedText) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        long socketStats = mergedText.getLines().stream()
                .filter(MergedOcrLine::isSocketGemRuneData)
                .count();
        ItemImportDebugTrace.log("MERGE_TYPED_OUTPUT", () -> "lineCount=" + mergedText.getLines().size()
                + " socketGemRuneStats=" + socketStats
                + " text=" + ItemImportDebugTrace.compactText(mergedText.asPlainText()));
    }

    private static void logSocketGemRuneIgnored(LogicalLine line, String reason) {
        if (!ItemImportDebugTrace.isEnabled()) {
            return;
        }
        SourceLine sourceLine = line.sourceLine();
        RawLineBoundaries boundaries = line.rawLineBoundaries();
        String matchedAffixType = ImportedItemAffixType.detectFromLine(line.text())
                .map(Enum::name)
                .orElse("");
        ItemImportDebugTrace.log("SOCKET_GEM_RUNE_CANDIDATE", () -> "sourceIndex=" + sourceLine.sourceIndex()
                + " sourceVariant=" + ItemImportDebugTrace.quote(sourceLine.variantId())
                + " sourceLineOrder=" + sourceLine.rawLineOrder()
                + " sourceRegion=" + line.sourceRegion()
                + " parentLineRegion=" + line.parentLineRegion()
                + " derivedFromSplit=" + line.derivedFromSplit()
                + " segmentStart=" + line.segmentStart()
                + " segmentEnd=" + line.segmentEnd()
                + " firstAspectEffectStart=" + boundaries.firstAspectEffectStart()
                + " firstSocketGemRuneStart=" + boundaries.firstSocketGemRuneStart()
                + " firstLoreVendorRequirementStart=" + boundaries.firstLoreVendorRequirementStart()
                + " effectiveSegmentRegion=" + line.sourceRegion()
                + " localAnchorType=" + line.localAnchorType()
                + " decision=ignoredAsSocketGemRune"
                + " sourceRawLine=" + ItemImportDebugTrace.compactText(sourceLine.rawLine())
                + " logicalLine=" + ItemImportDebugTrace.compactText(line.text())
                + (matchedAffixType.isBlank() ? "" : " matchedAffixType=" + matchedAffixType)
                + " ignoredForOrdinaryAffixes=true"
                + " reason=" + ItemImportDebugTrace.quote(reason));
    }

    private enum SourceRegion {
        ORDINARY_AFFIX_REGION,
        TRANSFIGURATION_REGION,
        TEMPERING_REGION,
        ASPECT_EFFECT_REGION,
        SOCKET_GEM_RUNE_REGION,
        LORE_VENDOR_REQUIREMENT_REGION,
        UNKNOWN
    }

    private enum LocalAnchorType {
        ORDINARY_AFFIX,
        TRANSFIGURATION,
        TEMPERING,
        ASPECT_EFFECT,
        SOCKET_GEM_RUNE,
        LORE_VENDOR_REQUIREMENT,
        BASE_STAT,
        UNKNOWN
    }

    private static final class SourceRegionTracker {
        private SourceRegion currentRegion = SourceRegion.UNKNOWN;

        SourceRegion currentRegion() {
            return currentRegion;
        }

        void acceptRegion(SourceRegion region) {
            if (region == SourceRegion.LORE_VENDOR_REQUIREMENT_REGION) {
                currentRegion = SourceRegion.LORE_VENDOR_REQUIREMENT_REGION;
                return;
            }
            if (region == SourceRegion.ASPECT_EFFECT_REGION || region == SourceRegion.SOCKET_GEM_RUNE_REGION) {
                currentRegion = region;
            }
        }

        void acceptIgnoredLine(String line) {
            String key = comparisonKey(line).replace(" ", "");
            if (isLoreVendorRequirementKey(key)) {
                currentRegion = SourceRegion.LORE_VENDOR_REQUIREMENT_REGION;
            }
        }
    }

    private record SourceText(int sourceIndex, String variantId, String text) {
        private SourceText {
            variantId = variantId == null ? "" : variantId;
            text = text == null ? "" : text;
        }
    }

    private record SourceLine(int sourceIndex, String variantId, int rawLineOrder, String rawLine, String normalizedLine,
                              SourceRegion sourceRegion) {
        private SourceLine {
            variantId = variantId == null ? "" : variantId;
            rawLine = rawLine == null ? "" : rawLine;
            normalizedLine = normalizedLine == null ? "" : normalizedLine;
            sourceRegion = sourceRegion == null ? SourceRegion.UNKNOWN : sourceRegion;
        }
    }

    private record LogicalLine(String text, SourceRegion sourceRegion, SourceLine sourceLine,
                               boolean derivedFromSplit, SourceRegion parentLineRegion,
                               int segmentStart, int segmentEnd, RawLineBoundaries rawLineBoundaries,
                               LocalAnchorType localAnchorType) {
        private LogicalLine {
            text = text == null ? "" : text;
            sourceRegion = sourceRegion == null ? SourceRegion.UNKNOWN : sourceRegion;
            parentLineRegion = parentLineRegion == null ? SourceRegion.UNKNOWN : parentLineRegion;
            rawLineBoundaries = rawLineBoundaries == null ? RawLineBoundaries.empty() : rawLineBoundaries;
            localAnchorType = localAnchorType == null ? LocalAnchorType.UNKNOWN : localAnchorType;
        }
    }

    private record SplitLineSegment(String text, int startOffset, int endOffset) {
        private SplitLineSegment {
            text = text == null ? "" : text;
        }
    }

    private record RawLineBoundaries(int firstAspectEffectStart,
                                     int firstSocketGemRuneStart,
                                     int firstLoreVendorRequirementStart) {
        private static RawLineBoundaries from(String line) {
            return new RawLineBoundaries(
                    firstPatternStart(line,
                            "\\b[Pp]oddaj\\s+si[eę]\\s+nienawi[sś]ci",
                            "\\b[Gg]dy\\s+masz\\s+umocnienie",
                            "\\b[Nn]aznaczenie",
                            "\\b[Aa]spekt\\b",
                            "\\b[Ww]ampirycznego\\b"),
                    firstPatternStart(line,
                            "\\b[Pp]uste\\s+gniazdo\\b",
                            "\\bgniazda\\b",
                            "\\bsocket\\b"),
                    firstPatternStart(line,
                            "\\b[Ww]ymaga\\s+[0-9]+\\s+poziomu\\b",
                            "\\b[Pp]rzypisano\\s+do\\s+konta\\b",
                            "\\b[Uu]nikatowe\\s+wyposa[żz]enie\\b",
                            "\\b[Pp]rzedmiot\\s+z\\s+dodatku\\b",
                            "\\b[Bb]rak\\s+mo[żz]liwo[śs]ci\\s+modyfikacji\\b",
                            "\\b[Ww]arto[śs][ćc]\\s+sprzeda[żz]y\\b",
                            "\\b[Tt]rwa[łl]o[śs][ćc]\\b",
                            "\\b[Oo]znacz\\b",
                            "\\b[Uu]pu[śs][ćc]\\b",
                            "\\b[Ww]yposa[żz]\\b",
                            "\\b[Pp]or[oó]wnaj\\b")
            );
        }

        private static RawLineBoundaries empty() {
            return new RawLineBoundaries(-1, -1, -1);
        }

        private int firstBlockingStart() {
            int first = -1;
            for (int value : List.of(firstAspectEffectStart, firstSocketGemRuneStart, firstLoreVendorRequirementStart)) {
                if (value >= 0 && (first < 0 || value < first)) {
                    first = value;
                }
            }
            return first;
        }

        private static int firstPatternStart(String line, String... patterns) {
            int first = -1;
            String safeLine = line == null ? "" : line;
            for (String pattern : patterns) {
                Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(safeLine);
                if (matcher.find() && (first < 0 || matcher.start() < first)) {
                    first = matcher.start();
                }
            }
            return first;
        }
    }

    private record CanonicalLineCandidate(String key,
                                          String text,
                                          int score,
                                          String sourceCategory,
                                          int sourceIndex,
                                          String sourceRegion,
                                          String sourceRawLine,
                                          String sourceVariant,
                                          int sourceLineOrder,
                                          int segmentStart,
                                          int segmentEnd,
                                          String localAnchorType,
                                          String matchedType,
                                          Double value,
                                          String runtimeStatus) {
        private CanonicalLineCandidate(String key, String text, int score) {
            this(key, text, score, sourceCategoryFromKey(key), -1, SourceRegion.UNKNOWN.name(), "",
                    "", -1, -1, -1, LocalAnchorType.UNKNOWN.name(), "", firstNumber(text).orElse(null),
                    runtimeStatusFromKey(key));
        }

        private CanonicalLineCandidate {
            key = key == null ? "" : key;
            text = text == null ? "" : text;
            sourceCategory = sourceCategory == null ? "" : sourceCategory;
            sourceRegion = sourceRegion == null ? SourceRegion.UNKNOWN.name() : sourceRegion;
            sourceRawLine = sourceRawLine == null ? "" : sourceRawLine;
            sourceVariant = sourceVariant == null ? "" : sourceVariant;
            localAnchorType = localAnchorType == null ? LocalAnchorType.UNKNOWN.name() : localAnchorType;
            matchedType = matchedType == null ? "" : matchedType;
            runtimeStatus = runtimeStatus == null ? "" : runtimeStatus;
        }

        private CanonicalLineCandidate withMetadata(LogicalLine line) {
            SourceLine sourceLine = line.sourceLine();
            String matched = ImportedItemAffixType.detectFromLine(text)
                    .map(Enum::name)
                    .orElse("");
            return new CanonicalLineCandidate(
                    key,
                    text,
                    score,
                    sourceCategoryFromKey(key),
                    sourceLine.sourceIndex(),
                    line.sourceRegion().name(),
                    sourceLine.rawLine(),
                    sourceLine.variantId(),
                    sourceLine.rawLineOrder(),
                    line.segmentStart(),
                    line.segmentEnd(),
                    line.localAnchorType().name(),
                    matched,
                    firstNumber(text).orElse(null),
                    runtimeStatusFromKey(key)
            );
        }

        private MergedOcrLine toMergedLine() {
            return new MergedOcrLine(
                    text,
                    key,
                    sourceCategory,
                    sourceRegion,
                    sourceRawLine,
                    sourceVariant,
                    sourceLineOrder,
                    segmentStart,
                    segmentEnd,
                    localAnchorType,
                    matchedType,
                    value,
                    runtimeStatus
            );
        }

        private String socketStatDedupKey() {
            return sourceIndex
                    + ":" + sourceVariant
                    + ":" + sourceLineOrder
                    + ":" + segmentStart
                    + ":" + segmentEnd
                    + ":" + comparisonKey(text)
                    + ":" + (value == null ? "" : value);
        }

        private static CanonicalLineCandidate fromMergedLine(MergedOcrLine line, int sourceIndex) {
            String key = line.isSocketGemRuneData()
                    ? "socket-stat:screen:" + sourceIndex + ":" + line.occurrenceKey()
                    : line.getCanonicalKey();
            return new CanonicalLineCandidate(
                    key,
                    line.getText(),
                    lineQualityScore(line.getText(), line.getValue(), MergeContext.empty()),
                    line.getSourceCategory().isBlank() ? sourceCategoryFromKey(key) : line.getSourceCategory(),
                    sourceIndex,
                    line.getSourceRegion(),
                    line.getSourceRawLine(),
                    line.getSourceVariant(),
                    line.getSourceLineOrder(),
                    line.getSegmentStart(),
                    line.getSegmentEnd(),
                    line.getLocalAnchorType(),
                    line.getMatchedType(),
                    line.getValue(),
                    line.isSocketGemRuneData() ? MergedOcrLine.DATA_ONLY : line.getRuntimeStatus()
            );
        }
    }

    private static String sourceCategoryFromKey(String key) {
        if (key == null) {
            return "UNKNOWN";
        }
        if (key.startsWith("socket-stat:")) {
            return MergedOcrLine.SOCKET_GEM_RUNE;
        }
        if (key.startsWith("socket:")) {
            return "SOCKET";
        }
        if (key.startsWith("affix:")) {
            return "ORDINARY_AFFIX";
        }
        if (key.startsWith("transfiguration:")) {
            return "TRANSFIGURATION";
        }
        if (key.startsWith("tempering:")) {
            return "TEMPERING";
        }
        if (key.startsWith("aspect:")) {
            return "ASPECT";
        }
        if (key.startsWith("implicit:") || key.equals("armor")) {
            return "BASE_OR_IMPLICIT";
        }
        return "UNKNOWN";
    }

    private static String runtimeStatusFromKey(String key) {
        return isSocketStatCandidateKey(key) ? MergedOcrLine.DATA_ONLY : "";
    }

    private record RangeText(double min, double max) {
    }

    private record MergeContext(boolean koscianychLusekQuality25, boolean moonFrenzyQuality25) {
        private static MergeContext empty() {
            return new MergeContext(false, false);
        }

        private static MergeContext from(String source) {
            String key = comparisonKey(source).replace(" ", "");
            return new MergeContext(key.contains("koscianychlusek")
                    && key.contains("tarcza")
                    && key.contains("25")
                    && key.contains("jakosci"),
                    (key.contains("burzyksiezycowegoszalu") || key.contains("tarczaburzy"))
                            && key.contains("25")
                            && key.contains("jakosci"));
        }
    }
}
