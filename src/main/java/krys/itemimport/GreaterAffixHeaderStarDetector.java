package krys.itemimport;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Wykrywa liczbę ikon Greater Affix z bezpiecznego nagłówka OCR itemu. */
final class GreaterAffixHeaderStarDetector {
    private static final String LITERAL_MARKERS = "*★✦✧⭐";
    private static final Pattern ZERO_LIKE_RUN_PATTERN = Pattern.compile("(?<![\\p{L}0-9])(?:[0Oo○◦](?:\\s*[0Oo○◦])*)(?![\\p{L}0-9])",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern ITEM_TYPE_OR_RARITY_PATTERN = Pattern.compile(
            "\\b("
                    + "STAROZYTNY|STAROZYTNA|UNIKATOWY|UNIKATOWA|LEGENDARNY|LEGENDARNA|RZADKI|RZADKA|MIECZ|TOPOR|BUŁAWA|BULAWA|HELM|HEŁM|REKAWICE|RĘKAWICE|BUTY|PIERSCIEN|PIERŚCIEŃ|AMULET|SPODNIE|ZBROJA|TARCZA|"
                    + "ANCESTRAL|UNIQUE|LEGENDARY|RARE|SWORD|AXE|MACE|PANTS|BOOTS|GLOVES|HELM|CHEST|AMULET|RING"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final List<String> HEADER_END_ANCHORS = List.of(
            "MOC PRZEDMIOTU",
            "ITEM POWER",
            "PKT OBRAZEN NA SEK",
            "OBRAZEN NA SEK",
            "DAMAGE PER SECOND",
            "PKT OBRAZEN ZA TRAFIENIE",
            "OBRAZEN ZA TRAFIENIE",
            "DAMAGE PER HIT",
            "ATAKU NA SEKUNDE",
            "ATTACKS PER SECOND",
            "JAKOSCI",
            "RYNSZTUNEK",
            "ZBROWNIOWI",
            "ZBROJOWNI",
            "PRZEISTOCZONY",
            "TEMPERED",
            "MASTERWORKED",
            "SZANSY",
            "SZANSA",
            "MNOZNIK",
            "ASPEKT",
            "ASPECT",
            "GNIAZDO",
            "SOCKET",
            "WYMAGA",
            "WARTOSC SPRZEDAZY"
    );

    GreaterAffixHeaderEvidence detect(List<ItemImageOcrTextVariant> variants) {
        List<ItemImageOcrTextVariant> safeVariants = variants == null ? List.of() : variants;
        Candidate selected = null;
        List<String> warnings = new ArrayList<>();
        for (ItemImageOcrTextVariant variant : safeVariants) {
            Candidate candidate = detectVariant(variant);
            warnings.addAll(candidate.warnings());
            if (candidate.detectedCount() > 0) {
                logCandidate(candidate);
                if (selected == null || compare(candidate, selected) < 0) {
                    selected = candidate;
                }
            }
        }
        if (selected == null) {
            String warningText = String.join(" | ", warnings);
            ItemImportDebugTrace.log("HEADER_GA_STAR_SELECTED", () -> "detectedCount=0"
                    + " source=" + GreaterAffixHeaderEvidenceSource.NOT_DETECTED
                    + " reliable=false"
                    + (warningText.isEmpty() ? "" : " warnings=" + ItemImportDebugTrace.quote(warningText)));
            return new GreaterAffixHeaderEvidence(
                    0,
                    GreaterAffixHeaderEvidenceSource.NOT_DETECTED,
                    "",
                    "",
                    false,
                    warnings
            );
        }
        Candidate selectedCandidate = selected;
        ItemImportDebugTrace.log("HEADER_GA_STAR_SELECTED", () -> "detectedCount=" + selectedCandidate.detectedCount()
                + " source=" + selectedCandidate.source()
                + " variantId=" + ItemImportDebugTrace.quote(selectedCandidate.variantId())
                + " reliable=" + selectedCandidate.reliable()
                + " headerText=" + ItemImportDebugTrace.compactText(selectedCandidate.headerText()));
        return new GreaterAffixHeaderEvidence(
                selectedCandidate.detectedCount(),
                selectedCandidate.source(),
                selectedCandidate.variantId(),
                selectedCandidate.headerText(),
                selectedCandidate.reliable(),
                warnings
        );
    }

    private Candidate detectVariant(ItemImageOcrTextVariant variant) {
        String variantId = variant == null ? "" : variant.getVariantId();
        String text = variant == null ? "" : variant.getText();
        HeaderWindow header = headerWindow(text);
        String headerText = header.text();
        List<String> warnings = new ArrayList<>(header.warnings());
        if (headerText.isBlank()) {
            return Candidate.notDetected(variantId, warnings);
        }
        CandidateToken token = bestToken(headerText);
        if (token.count() <= 0) {
            logRejected(variantId, headerText, "no literal or zero-like Greater Affix candidate in header window");
            return Candidate.notDetected(variantId, warnings);
        }
        if (!hasItemNameBefore(headerText, token.start())) {
            logRejected(variantId, headerText, "candidate appears before item name context");
            return Candidate.notDetected(variantId, warnings);
        }
        boolean hasTypeOrRarity = hasTypeOrRarityAfter(headerText, token.end());
        boolean reliable = hasTypeOrRarity || token.literalCount() > 0;
        if (!reliable) {
            warnings.add("Header GA candidate without explicit type/rarity context: " + headerText);
        }
        GreaterAffixHeaderEvidenceSource source = sourceFor(token.literalCount(), token.zeroLikeCount());
        return new Candidate(
                variantId,
                headerText,
                token.literalCount(),
                token.zeroLikeCount(),
                token.count(),
                source,
                reliable,
                warnings
        );
    }

    private static HeaderWindow headerWindow(String text) {
        String normalized = normalizeTextForHeader(text);
        if (normalized.isBlank()) {
            return new HeaderWindow("", List.of());
        }
        normalized = removeUiEquippedStatus(normalized);
        int end = normalized.length();
        String folded = fold(normalized);
        for (String anchor : HEADER_END_ANCHORS) {
            int index = folded.indexOf(anchor);
            if (index >= 0) {
                end = Math.min(end, index);
            }
        }
        Matcher affixStart = Pattern.compile("(^|\\s)\\+\\s*[0-9]").matcher(folded);
        if (affixStart.find()) {
            end = Math.min(end, affixStart.start());
        }
        String header = normalized.substring(0, Math.max(0, end)).trim();
        List<String> warnings = new ArrayList<>();
        if (end < normalized.length()) {
            String tail = normalized.substring(end);
            Matcher rejected = ZERO_LIKE_RUN_PATTERN.matcher(tail);
            while (rejected.find()) {
                warnings.add("Rejected zero-like run after header anchor: " + rejected.group().trim());
                ItemImportDebugTrace.log("HEADER_GA_STAR_REJECTED", () -> "reason="
                        + ItemImportDebugTrace.quote("zero-like run appears after item power/stat anchor")
                        + " rejectedText=" + ItemImportDebugTrace.compactText(rejected.group()));
            }
        }
        return new HeaderWindow(header, warnings);
    }

    private static CandidateToken bestToken(String headerText) {
        List<CandidateToken> tokens = new ArrayList<>();
        for (int index = 0; index < headerText.length(); index++) {
            if (!isLiteralMarker(headerText.charAt(index))) {
                continue;
            }
            int end = index + 1;
            int literalCount = 1;
            int zeroLikeCount = 0;
            int cursor = end;
            while (cursor < headerText.length()) {
                int next = nextNonWhitespace(headerText, cursor);
                if (next < 0) {
                    break;
                }
                char nextChar = headerText.charAt(next);
                if (isLiteralMarker(nextChar)) {
                    literalCount++;
                    cursor = next + 1;
                    end = cursor;
                    continue;
                }
                ZeroLikeRun run = zeroLikeRunAt(headerText, next);
                if (run.count() > 0 && isSeparatedFromNumber(headerText, run.start(), run.end())) {
                    zeroLikeCount += run.count();
                    cursor = run.end();
                    end = cursor;
                    continue;
                }
                break;
            }
            tokens.add(new CandidateToken(index, end, literalCount, zeroLikeCount));
        }

        Matcher zeroMatcher = ZERO_LIKE_RUN_PATTERN.matcher(headerText);
        while (zeroMatcher.find()) {
            int count = countZeroLikeCharacters(zeroMatcher.group());
            if (count <= 0 || !isSeparatedFromNumber(headerText, zeroMatcher.start(), zeroMatcher.end())) {
                continue;
            }
            tokens.add(new CandidateToken(zeroMatcher.start(), zeroMatcher.end(), 0, count));
        }

        return tokens.stream()
                .max(Comparator.comparingInt(CandidateToken::count)
                        .thenComparingInt(CandidateToken::literalCount)
                        .thenComparing(token -> -token.start()))
                .orElse(new CandidateToken(0, 0, 0, 0));
    }

    private static ZeroLikeRun zeroLikeRunAt(String text, int start) {
        Matcher matcher = ZERO_LIKE_RUN_PATTERN.matcher(text);
        matcher.region(start, text.length());
        if (!matcher.lookingAt()) {
            return new ZeroLikeRun(start, start, 0);
        }
        return new ZeroLikeRun(matcher.start(), matcher.end(), countZeroLikeCharacters(matcher.group()));
    }

    private static boolean hasItemNameBefore(String headerText, int tokenStart) {
        if (tokenStart <= 0 || tokenStart > headerText.length()) {
            return false;
        }
        String before = headerText.substring(0, tokenStart).trim();
        if (before.isBlank()) {
            return false;
        }
        String folded = fold(before);
        if (folded.endsWith("NA WYPOSAZENIU") || folded.endsWith("EQUIPPED")) {
            return false;
        }
        int letters = 0;
        for (int index = 0; index < before.length(); index++) {
            if (Character.isLetter(before.charAt(index))) {
                letters++;
            }
        }
        return letters >= 3;
    }

    private static boolean hasTypeOrRarityAfter(String headerText, int tokenEnd) {
        if (tokenEnd < 0 || tokenEnd > headerText.length()) {
            return false;
        }
        String after = headerText.substring(tokenEnd);
        return ITEM_TYPE_OR_RARITY_PATTERN.matcher(fold(after)).find();
    }

    private static GreaterAffixHeaderEvidenceSource sourceFor(int literalCount, int zeroLikeCount) {
        if (literalCount > 0 && zeroLikeCount > 0) {
            return GreaterAffixHeaderEvidenceSource.OCR_HEADER_MIXED;
        }
        if (literalCount > 0) {
            return GreaterAffixHeaderEvidenceSource.OCR_HEADER_LITERAL_STARS;
        }
        if (zeroLikeCount > 0) {
            return GreaterAffixHeaderEvidenceSource.OCR_HEADER_ZERO_LIKE_RUN_HEURISTIC;
        }
        return GreaterAffixHeaderEvidenceSource.NOT_DETECTED;
    }

    private static void logCandidate(Candidate candidate) {
        ItemImportDebugTrace.log("HEADER_GA_STAR_CANDIDATE", () -> "variantId=" + ItemImportDebugTrace.quote(candidate.variantId())
                + " headerText=" + ItemImportDebugTrace.compactText(candidate.headerText())
                + " literalStars=" + candidate.literalCount()
                + " zeroLikeRunStars=" + candidate.zeroLikeCount()
                + " detectedCount=" + candidate.detectedCount()
                + " reliable=" + candidate.reliable()
                + " source=" + candidate.source());
    }

    private static void logRejected(String variantId, String headerText, String reason) {
        ItemImportDebugTrace.log("HEADER_GA_STAR_REJECTED", () -> "variantId=" + ItemImportDebugTrace.quote(variantId)
                + " reason=" + ItemImportDebugTrace.quote(reason)
                + " headerText=" + ItemImportDebugTrace.compactText(headerText));
    }

    private static int compare(Candidate left, Candidate right) {
        int byReliable = Boolean.compare(right.reliable(), left.reliable());
        if (byReliable != 0) {
            return byReliable;
        }
        int byCount = Integer.compare(right.detectedCount(), left.detectedCount());
        if (byCount != 0) {
            return byCount;
        }
        int bySource = Integer.compare(sourceScore(right.source()), sourceScore(left.source()));
        if (bySource != 0) {
            return bySource;
        }
        return Integer.compare(left.variantId().length(), right.variantId().length());
    }

    private static int sourceScore(GreaterAffixHeaderEvidenceSource source) {
        return switch (source) {
            case OCR_HEADER_MIXED -> 3;
            case OCR_HEADER_LITERAL_STARS -> 2;
            case OCR_HEADER_ZERO_LIKE_RUN_HEURISTIC -> 1;
            case NOT_DETECTED -> 0;
        };
    }

    private static String normalizeTextForHeader(String text) {
        return (text == null ? "" : text)
                .replace('\r', '\n')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String removeUiEquippedStatus(String text) {
        return text.replaceAll("(?iu)\\bNA\\s+WYPOSA[ŻZ]ENIU\\b", " ")
                .replaceAll("(?iu)\\bEQUIPPED\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String fold(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    private static int nextNonWhitespace(String text, int start) {
        for (int index = start; index < text.length(); index++) {
            if (!Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isSeparatedFromNumber(String text, int start, int end) {
        char before = start <= 0 ? ' ' : text.charAt(start - 1);
        char after = end >= text.length() ? ' ' : text.charAt(end);
        return !Character.isDigit(before) && !Character.isDigit(after);
    }

    private static boolean isLiteralMarker(char marker) {
        return LITERAL_MARKERS.indexOf(marker) >= 0;
    }

    private static boolean isZeroLike(char value) {
        return value == '0' || value == 'O' || value == 'o' || value == '○' || value == '◦';
    }

    private static int countZeroLikeCharacters(String text) {
        int count = 0;
        for (int index = 0; index < (text == null ? "" : text).length(); index++) {
            if (isZeroLike(text.charAt(index))) {
                count++;
            }
        }
        return count;
    }

    private record HeaderWindow(String text, List<String> warnings) {
    }

    private record Candidate(String variantId,
                             String headerText,
                             int literalCount,
                             int zeroLikeCount,
                             int detectedCount,
                             GreaterAffixHeaderEvidenceSource source,
                             boolean reliable,
                             List<String> warnings) {
        private static Candidate notDetected(String variantId, List<String> warnings) {
            return new Candidate(variantId == null ? "" : variantId, "", 0, 0, 0,
                    GreaterAffixHeaderEvidenceSource.NOT_DETECTED, false, warnings == null ? List.of() : warnings);
        }
    }

    private record CandidateToken(int start, int end, int literalCount, int zeroLikeCount) {
        private int count() {
            return literalCount + zeroLikeCount;
        }
    }

    private record ZeroLikeRun(int start, int end, int count) {
    }
}
