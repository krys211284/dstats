package krys.itemimport;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Wyznacza granice sekcji zwykłych affixów w spójnej sekwencji tooltipa. */
final class OrdinaryAffixSectionDetector {
    private static final int ORDINARY_AFFIX_LIMIT = 4;

    OrdinaryAffixSection detect(FullItemRead fullItemRead) {
        return detect(fullItemRead, List.of());
    }

    OrdinaryAffixSection detect(FullItemRead fullItemRead,
                                List<OrdinaryAffixSequenceCandidate> sequenceCandidates) {
        List<OrdinaryAffixSequenceCandidate> candidates = new ArrayList<>();
        for (OrdinaryAffixSequenceCandidate candidate : sequenceCandidates == null ? List.<OrdinaryAffixSequenceCandidate>of() : sequenceCandidates) {
            if (candidate != null && candidate.fullItemRead().hasAnyData()) {
                candidates.add(candidate);
            }
        }
        if (fullItemRead != null && fullItemRead.hasAnyData()) {
            candidates.add(OrdinaryAffixSequenceCandidate.global(fullItemRead));
        }
        if (candidates.isEmpty()) {
            return OrdinaryAffixSection.empty();
        }

        List<ScoredSection> scored = candidates.stream()
                .map(this::score)
                .peek(this::logCandidate)
                .toList();
        ScoredSection selected = scored.stream()
                .max(Comparator.comparingInt(ScoredSection::score))
                .orElseGet(() -> score(OrdinaryAffixSequenceCandidate.global(fullItemRead)));
        logSelection(scored, selected);
        logSection(selected);
        return selected.section();
    }

    private ScoredSection score(OrdinaryAffixSequenceCandidate candidate) {
        FullItemRead read = candidate.fullItemRead();
        List<FullItemReadLine> lines = read.getLines();
        int anchorIndex = lastBaseItemStatIndex(read);
        boolean hasBaseItemAnchor = anchorIndex >= 0 || isBaseItemStat(read.getBaseItemValue());
        boolean hasBaseWeaponAnchor = anchorIndex >= 0
                ? isBaseWeaponStat(lines.get(anchorIndex).getText())
                : isBaseWeaponStat(read.getBaseItemValue());
        boolean contaminatedAnchor = anchorIndex >= 0 && isContaminatedBaseAnchor(lines.get(anchorIndex).getText());
        int startIndex = Math.max(0, anchorIndex + 1);
        int endIndex = lines.size();
        String endMarker = "";
        String endReason = "end of read";
        if (hasBaseItemAnchor && !contaminatedAnchor) {
            for (int index = startIndex; index < lines.size(); index++) {
                EndMarker marker = endMarker(lines.get(index));
                if (marker != null) {
                    if (!marker.hard()
                            && candidateLineIndexes(lines, startIndex, index).size() < ORDINARY_AFFIX_LIMIT
                            && hasCandidateLineAfter(lines, index)) {
                        continue;
                    }
                    endIndex = index;
                    endMarker = lines.get(index).getText();
                    endReason = marker.reason();
                    break;
                }
            }
        }
        List<Integer> candidates = candidateLineIndexes(lines, startIndex, endIndex);
        List<Integer> rejected = rejectedAffixLikeIndexes(lines, endIndex);
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (hasBaseItemAnchor) {
            score += 120;
            reasons.add("base item stat anchor");
        } else {
            score -= 80;
            reasons.add("missing base item stat anchor");
        }
        if (contaminatedAnchor) {
            score -= 600;
            reasons.add("contaminated base anchor");
        }
        if (!endMarker.isBlank()) {
            score += 90;
            reasons.add("ordinary boundary marker");
        }
        if (!candidates.isEmpty()) {
            score += Math.min(candidates.size(), ORDINARY_AFFIX_LIMIT) * 45;
            reasons.add("ordinary candidates=" + candidates.size());
            if (candidates.size() == ORDINARY_AFFIX_LIMIT) {
                score += 35;
                reasons.add("ordinary cap filled before boundary");
            }
        } else {
            score -= 120;
            reasons.add("no ordinary candidates after anchor");
        }
        if (socketGemRuneEffectBeforeOrdinary(lines, startIndex, candidates)) {
            score -= 250;
            reasons.add("socket/gem effect appears before ordinary");
        }
        if (candidate.globalMergedSequence()) {
            score -= 30;
            reasons.add("global merged fallback");
        }
        if ("TYPED_MERGED".equals(candidate.sourceVariant()) || "MULTI_MERGED".equals(candidate.sourceVariant())) {
            score += 25;
            reasons.add("authoritative typed sequence");
        }
        OrdinaryAffixSection section = new OrdinaryAffixSection(
                read,
                candidate.sequenceId(),
                candidate.sourceVariant(),
                score,
                startIndex,
                endIndex,
                anchorIndex,
                anchorIndex >= 0 ? lines.get(anchorIndex).getText() : read.getBaseItemValue(),
                hasBaseItemAnchor,
                hasBaseWeaponAnchor,
                endMarker,
                endReason,
                candidates,
                rejected
        );
        return new ScoredSection(candidate, section, score, String.join("; ", reasons), contaminatedAnchor);
    }

    private static int lastBaseItemStatIndex(FullItemRead fullItemRead) {
        int result = -1;
        boolean seenBaseItemStatBlock = false;
        boolean closedBaseItemStatBlock = false;
        List<FullItemReadLine> lines = fullItemRead.getLines();
        for (int index = 0; index < lines.size(); index++) {
            FullItemReadLine line = lines.get(index);
            if (isBaseItemStat(line.getText()) && !closedBaseItemStatBlock && !isContaminatedBaseAnchor(line.getText())) {
                result = index;
                seenBaseItemStatBlock = true;
                continue;
            }
            if (seenBaseItemStatBlock && !isBaseItemStat(line.getText())) {
                closedBaseItemStatBlock = true;
            }
        }
        return result;
    }

    private static boolean isBaseItemStat(String text) {
        String normalized = normalize(text);
        return isBaseWeaponStat(text)
                || normalized.contains("PKT. PANCERZA")
                || normalized.contains("ARMOR")
                || normalized.contains("SZANSY NA BLOK")
                || normalized.contains("SZANSA NA BLOK")
                || normalized.contains("BLOCK CHANCE");
    }

    private static boolean isBaseWeaponStat(String text) {
        String normalized = normalize(text);
        return normalized.contains("PKT. OBRAZEN NA SEK.")
                || normalized.contains("OBRAZEN NA SEK.")
                || normalized.contains("DAMAGE PER SECOND")
                || normalized.contains("PKT. OBRAZEN ZA TRAFIENIE")
                || normalized.contains("OBRAZEN ZA TRAFIENIE")
                || normalized.contains("DAMAGE PER HIT")
                || normalized.contains("ATAKU NA SEKUNDE")
                || normalized.contains("ATTACKS PER SECOND");
    }

    private static boolean isContaminatedBaseAnchor(String text) {
        String normalized = normalize(text);
        String collapsed = normalized.replaceAll("[^A-Z0-9]", "");
        return collapsed.contains("MNOZNIK")
                && collapsed.contains("OBRAZEN")
                && (collapsed.contains("FIZYCZNE") || collapsed.contains("PHYSICAL"))
                && isBaseItemStat(text);
    }

    private static boolean socketGemRuneEffectBeforeOrdinary(List<FullItemReadLine> lines,
                                                            int startIndex,
                                                            List<Integer> candidateIndexes) {
        int firstCandidate = candidateIndexes.isEmpty() ? lines.size() : candidateIndexes.getFirst();
        for (int index = Math.max(0, startIndex); index < firstCandidate && index < lines.size(); index++) {
            String collapsed = normalize(lines.get(index).getText()).replaceAll("[^A-Z0-9]", "");
            if (collapsed.contains("MNOZNIK") && collapsed.contains("OBRAZEN")
                    && (collapsed.contains("FIZYCZNE") || collapsed.contains("PHYSICAL"))) {
                return true;
            }
        }
        return false;
    }

    private static EndMarker endMarker(FullItemReadLine line) {
        String normalized = normalize(line.getText());
        String collapsed = normalized.replaceAll("[^A-Z0-9]", "");
        if (collapsed.contains("NIEZNISZCZALNOSC")) {
            return new EndMarker("transmutation marker", true);
        }
        if (collapsed.contains("UMIEJETNOSCIPODSTAWOWE")
                || collapsed.contains("GDYMASZUMOCNIENIE")
                || collapsed.contains("PODDAJSIENIENAWISCI")
                || collapsed.contains("LASKIMATKI")
                || collapsed.contains("ASPEKT")
                || collapsed.contains("LEGENDARYPOWER")) {
            return new EndMarker("aspect or unique effect marker", false);
        }
        if (collapsed.contains("PUSTEGNIAZDO")
                || collapsed.equals("GNIAZDO")
                || collapsed.equals("GNIAZDA")
                || collapsed.contains("RUNE")
                || collapsed.contains("RUNA")) {
            return new EndMarker("socket/gem/rune marker", true);
        }
        if (collapsed.contains("WYMAGA")
                || collapsed.contains("PRZYPISANODOKONTA")
                || collapsed.contains("PRZEDMIOTZDODATKU")
                || collapsed.contains("BRAKMOZLIWOSCIMODYFIKACJI")
                || collapsed.contains("WARTOSCSPRZEDAZY")
                || collapsed.contains("TRWALOSC")
                || collapsed.contains("POROWNAJ")) {
            return new EndMarker("lore/vendor/requirement marker", true);
        }
        return null;
    }

    private static boolean hasCandidateLineAfter(List<FullItemReadLine> lines, int markerIndex) {
        for (int index = markerIndex + 1; index < lines.size(); index++) {
            if (isCandidateLine(lines.get(index))) {
                return true;
            }
        }
        return false;
    }

    private static List<Integer> candidateLineIndexes(List<FullItemReadLine> lines, int startIndex, int endIndex) {
        List<Integer> result = new ArrayList<>();
        for (int index = startIndex; index < endIndex; index++) {
            FullItemReadLine line = lines.get(index);
            if (isCandidateLine(line)) {
                result.add(index);
            }
        }
        return List.copyOf(result);
    }

    private static List<Integer> rejectedAffixLikeIndexes(List<FullItemReadLine> lines, int endIndex) {
        if (endIndex >= lines.size()) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        for (int index = endIndex + 1; index < lines.size(); index++) {
            FullItemReadLine line = lines.get(index);
            if (isCandidateLine(line)) {
                result.add(index);
            }
        }
        return List.copyOf(result);
    }

    static boolean isCandidateLine(FullItemReadLine line) {
        if (line == null || endMarker(line) != null || isBaseItemStat(line.getText())) {
            return false;
        }
        if (line.getType() != FullItemReadLineType.AFFIX && line.getType() != FullItemReadLineType.SOCKET) {
            return false;
        }
        return ImportedItemAffixType.detectFromLine(line.getText()).isPresent();
    }

    private void logCandidate(ScoredSection scored) {
        ItemImportDebugTrace.log("ORDINARY_SEQUENCE_CANDIDATE", () -> "sequenceId="
                + ItemImportDebugTrace.quote(scored.section().sequenceId())
                + " sourceVariant=" + ItemImportDebugTrace.quote(scored.section().sourceVariant())
                + " score=" + scored.score()
                + " anchor=" + ItemImportDebugTrace.compactText(scored.section().startAnchorText())
                + " candidates=" + scored.section().candidateLineIndexes().size()
                + " marker=" + ItemImportDebugTrace.compactText(scored.section().endMarker())
                + " reason=" + ItemImportDebugTrace.quote(scored.reason()));
        if (scored.contaminatedAnchor()) {
            ItemImportDebugTrace.log("ORDINARY_SEQUENCE_REJECTED", () -> "sequenceId="
                    + ItemImportDebugTrace.quote(scored.section().sequenceId())
                    + " reason=" + ItemImportDebugTrace.quote("base item stat anchor is contaminated by socket/gem effect"));
        }
    }

    private void logSelection(List<ScoredSection> scored, ScoredSection selected) {
        for (ScoredSection candidate : scored) {
            if (candidate == selected) {
                continue;
            }
            if (candidate.score() + 150 < selected.score()) {
                ItemImportDebugTrace.log("ORDINARY_SEQUENCE_REJECTED", () -> "sequenceId="
                        + ItemImportDebugTrace.quote(candidate.section().sequenceId())
                        + " reason=" + ItemImportDebugTrace.quote("lower score than selected coherent ordinary sequence"));
            }
        }
        ItemImportDebugTrace.log("ORDINARY_SEQUENCE_SELECTED", () -> "sequenceId="
                + ItemImportDebugTrace.quote(selected.section().sequenceId())
                + " sourceVariant=" + ItemImportDebugTrace.quote(selected.section().sourceVariant())
                + " score=" + selected.score()
                + " reason=" + ItemImportDebugTrace.quote("best coherent base->ordinary->boundary sequence"));
    }

    private static void logSection(ScoredSection selected) {
        OrdinaryAffixSection section = selected.section();
        List<FullItemReadLine> lines = section.sequenceRead().getLines();
        ItemImportDebugTrace.log("ORDINARY_AFFIX_SECTION_START", () -> "anchor="
                + ItemImportDebugTrace.compactText(section.startAnchorText())
                + " sequenceId=" + ItemImportDebugTrace.quote(section.sequenceId())
                + " startIndex=" + section.startIndex()
                + " reason=" + ItemImportDebugTrace.quote("after base item stats"));
        int order = 1;
        for (Integer index : section.candidateLineIndexes()) {
            FullItemReadLine line = lines.get(index);
            int selectedOrder = order++;
            String type = ImportedItemAffixType.detectFromLine(line.getText())
                    .map(Enum::name)
                    .orElse("UNKNOWN");
            ItemImportDebugTrace.log("ORDINARY_AFFIX_SECTION_CANDIDATE", () -> "order=" + selectedOrder
                    + " type=" + type
                    + " lineIndex=" + index
                    + " text=" + ItemImportDebugTrace.compactText(line.getText())
                    + " selected=" + (selectedOrder <= ORDINARY_AFFIX_LIMIT));
            if (line.getType() == FullItemReadLineType.SOCKET) {
                ItemImportDebugTrace.log("ORDINARY_AFFIX_SECTION_SOCKET_OVERRIDE", () -> "type=" + type
                        + " text=" + ItemImportDebugTrace.compactText(line.getText())
                        + " selected=AFFIX rejected=SOCKET"
                        + " reason=" + ItemImportDebugTrace.quote("line belongs to ordinary section"));
            }
        }
        ItemImportDebugTrace.log("ORDINARY_AFFIX_SECTION_END", () -> "marker="
                + ItemImportDebugTrace.compactText(section.endMarker())
                + " endIndex=" + section.endIndex()
                + " selectedCount=" + Math.min(section.candidateLineIndexes().size(), ORDINARY_AFFIX_LIMIT)
                + " max=" + ORDINARY_AFFIX_LIMIT
                + " reason=" + ItemImportDebugTrace.quote(section.endReason()));
        for (Integer index : section.rejectedAffixLikeLineIndexes()) {
            FullItemReadLine line = lines.get(index);
            String marker = section.endMarker().isBlank() ? "ordinary section end" : section.endMarker();
            String type = ImportedItemAffixType.detectFromLine(line.getText())
                    .map(Enum::name)
                    .orElse("UNKNOWN");
            ItemImportDebugTrace.log("ORDINARY_AFFIX_SECTION_REJECTED", () -> "type=" + type
                    + " lineIndex=" + index
                    + " text=" + ItemImportDebugTrace.compactText(line.getText())
                    + " reason=" + ItemImportDebugTrace.quote("outside ordinary section after marker " + marker));
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    private record EndMarker(String reason, boolean hard) {
    }

    private record ScoredSection(OrdinaryAffixSequenceCandidate candidate,
                                 OrdinaryAffixSection section,
                                 int score,
                                 String reason,
                                 boolean contaminatedAnchor) {
    }

    record OrdinaryAffixSection(FullItemRead sequenceRead,
                                String sequenceId,
                                String sourceVariant,
                                int score,
                                int startIndex,
                                int endIndex,
                                int startAnchorIndex,
                                String startAnchorText,
                                boolean hasBaseItemAnchor,
                                boolean hasBaseWeaponAnchor,
                                String endMarker,
                                String endReason,
                                List<Integer> candidateLineIndexes,
                                List<Integer> rejectedAffixLikeLineIndexes) {
        static OrdinaryAffixSection empty() {
            return new OrdinaryAffixSection(FullItemRead.empty(), "", "", 0, 0, 0, -1, "",
                    false, false, "", "empty read", List.of(), List.of());
        }

        public boolean hasBaseWeaponAnchor() {
            return hasBaseWeaponAnchor;
        }

        boolean containsLineIndex(int index) {
            return index >= startIndex && index < endIndex;
        }
    }
}
