package krys.itemimport.image;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Agreguje neutralne głosy liczbowe profili OCR bez wiedzy o afiksach. */
final class PropertyBlockNumericEvidenceAggregator {
    static final String AMBIGUOUS_DIAGNOSTIC = "NUMERIC_OCR_AMBIGUOUS";
    private static final int MIN_SUPPORTING_PROFILES = 4;
    private static final double GAP_SCORE = -1.0d;
    private static final double INCOMPATIBLE_TOKEN_SCORE = -1.5d;
    private static final double INSERTION_GROUP_THRESHOLD = 1.0d;

    private static final Pattern NUMERIC_TOKEN = Pattern.compile(
            "(?<![\\p{L}\\d])[+-]?\\s*\\d(?:[\\d ]*\\d)?(?:[,.]\\d+)?"
                    + "(?:\\s*-\\s*\\d(?:[\\d ]*\\d)?(?:[,.]\\d+)?)?\\s*\\]?%?(?!\\d)");

    private PropertyBlockNumericEvidenceAggregator() {
    }

    static List<String> extractNumericTokens(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        Matcher matcher = NUMERIC_TOKEN.matcher(rawText);
        while (matcher.find()) {
            String token = matcher.group()
                    .replace("]", "")
                    .replace('\u2013', '-')
                    .replace('\u2212', '-')
                    .replaceAll("\\s+", "");
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return List.copyOf(tokens);
    }

    static Aggregation aggregate(List<PropertyBlockOcrProfileResult> profiles) {
        List<PropertyBlockOcrProfileResult> votingProfiles = profiles.stream()
                .filter(profile -> !profile.getNumericTokens().isEmpty())
                .toList();
        if (votingProfiles.isEmpty()) {
            return new Aggregation(List.of(), List.of());
        }
        PropertyBlockOcrProfileResult reference = referenceProfile(votingProfiles);
        List<EvidenceColumn> columns = alignProfiles(votingProfiles, reference.getNumericTokens());
        return evidenceFromColumns(columns, votingProfiles.size());
    }

    private static Aggregation evidenceFromColumns(List<EvidenceColumn> columns, int votingProfileCount) {
        List<PropertyBlockNumericEvidence> evidence = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        int tokenOrder = 1;
        for (EvidenceColumn column : columns) {
            if (column.isInsertedNoise()) {
                continue;
            }
            Map<String, List<String>> profilesByValue = new LinkedHashMap<>();
            for (TokenVote vote : column.votes()) {
                profilesByValue.computeIfAbsent(vote.value(), ignored -> new ArrayList<>())
                        .add(vote.profile());
            }
            if (profilesByValue.isEmpty()) {
                continue;
            }
            Map.Entry<String, List<String>> strongest = profilesByValue.entrySet().stream()
                    .max((left, right) -> Integer.compare(left.getValue().size(), right.getValue().size()))
                    .orElseThrow();
            int supportCount = strongest.getValue().size();
            boolean ambiguous = supportCount < MIN_SUPPORTING_PROFILES
                    || supportCount * 3 < votingProfileCount * 2
                    || mixedTokenShape(profilesByValue.keySet()) && supportCount < MIN_SUPPORTING_PROFILES + 2;
            String selectedValue = ambiguous ? "" : strongest.getKey();
            List<String> alternatives = profilesByValue.keySet().stream()
                    .filter(value -> ambiguous || !value.equals(selectedValue))
                    .toList();
            evidence.add(new PropertyBlockNumericEvidence(
                    tokenOrder,
                    selectedValue,
                    strongest.getValue(),
                    alternatives,
                    supportCount,
                    ambiguous
            ));
            if (ambiguous) {
                diagnostics.add(AMBIGUOUS_DIAGNOSTIC + " tokenOrder=" + tokenOrder
                        + " values=" + profilesByValue.keySet());
            }
            tokenOrder++;
        }
        return new Aggregation(evidence, diagnostics);
    }

    private static List<EvidenceColumn> alignProfiles(List<PropertyBlockOcrProfileResult> profiles,
                                                      List<String> referenceTokens) {
        List<EvidenceColumn> referenceColumns = referenceTokens.stream()
                .map(token -> new EvidenceColumn(false))
                .toList();
        Map<Integer, List<TokenVote>> insertionsBySlot = new LinkedHashMap<>();
        for (PropertyBlockOcrProfileResult profile : profiles) {
            SequenceAlignment alignment = alignToReference(referenceTokens, profile.getNumericTokens());
            for (AlignedToken alignedToken : alignment.tokens()) {
                if (alignedToken.referenceIndex() >= 0) {
                    referenceColumns.get(alignedToken.referenceIndex())
                            .add(profile.getProfile(), alignedToken.value());
                } else {
                    insertionsBySlot.computeIfAbsent(alignedToken.insertionSlot(), ignored -> new ArrayList<>())
                            .add(new TokenVote(profile.getProfile(), alignedToken.value()));
                }
            }
        }
        List<EvidenceColumn> columns = new ArrayList<>();
        for (int slot = 0; slot <= referenceTokens.size(); slot++) {
            columns.addAll(groupInsertions(insertionsBySlot.getOrDefault(slot, List.of())));
            if (slot < referenceTokens.size()) {
                columns.add(referenceColumns.get(slot));
            }
        }
        return columns;
    }

    private static List<EvidenceColumn> groupInsertions(List<TokenVote> insertions) {
        List<EvidenceColumn> columns = new ArrayList<>();
        for (TokenVote vote : insertions) {
            EvidenceColumn bestColumn = columns.stream()
                    .filter(column -> !column.hasProfile(vote.profile()))
                    .max(Comparator.comparingDouble(column -> column.similarityTo(vote.value())))
                    .filter(column -> column.similarityTo(vote.value()) >= INSERTION_GROUP_THRESHOLD)
                    .orElse(null);
            if (bestColumn == null) {
                bestColumn = new EvidenceColumn(true);
                columns.add(bestColumn);
            }
            bestColumn.add(vote.profile(), vote.value());
        }
        return columns;
    }

    private static PropertyBlockOcrProfileResult referenceProfile(List<PropertyBlockOcrProfileResult> profiles) {
        return profiles.stream()
                .max((left, right) -> {
                    int scoreComparison = Double.compare(referenceScore(left, profiles), referenceScore(right, profiles));
                    return scoreComparison != 0
                            ? scoreComparison
                            : Integer.compare(right.getNumericTokens().size(), left.getNumericTokens().size());
                })
                .orElseThrow();
    }

    private static double referenceScore(PropertyBlockOcrProfileResult candidate,
                                         List<PropertyBlockOcrProfileResult> profiles) {
        return profiles.stream()
                .mapToDouble(profile -> normalizedAlignmentScore(candidate.getNumericTokens(), profile.getNumericTokens()))
                .sum();
    }

    private static SequenceAlignment alignToReference(List<String> referenceTokens, List<String> candidateTokens) {
        int referenceSize = referenceTokens.size();
        int candidateSize = candidateTokens.size();
        double[][] scores = new double[referenceSize + 1][candidateSize + 1];
        Direction[][] directions = new Direction[referenceSize + 1][candidateSize + 1];
        for (int referenceIndex = 1; referenceIndex <= referenceSize; referenceIndex++) {
            scores[referenceIndex][0] = referenceIndex * GAP_SCORE;
            directions[referenceIndex][0] = Direction.UP;
        }
        for (int candidateIndex = 1; candidateIndex <= candidateSize; candidateIndex++) {
            scores[0][candidateIndex] = candidateIndex * GAP_SCORE;
            directions[0][candidateIndex] = Direction.LEFT;
        }
        for (int referenceIndex = 1; referenceIndex <= referenceSize; referenceIndex++) {
            for (int candidateIndex = 1; candidateIndex <= candidateSize; candidateIndex++) {
                double diagonal = scores[referenceIndex - 1][candidateIndex - 1]
                        + tokenSimilarity(referenceTokens.get(referenceIndex - 1), candidateTokens.get(candidateIndex - 1));
                double up = scores[referenceIndex - 1][candidateIndex] + GAP_SCORE;
                double left = scores[referenceIndex][candidateIndex - 1] + GAP_SCORE;
                if (diagonal >= up && diagonal >= left) {
                    scores[referenceIndex][candidateIndex] = diagonal;
                    directions[referenceIndex][candidateIndex] = Direction.DIAGONAL;
                } else if (left >= up) {
                    scores[referenceIndex][candidateIndex] = left;
                    directions[referenceIndex][candidateIndex] = Direction.LEFT;
                } else {
                    scores[referenceIndex][candidateIndex] = up;
                    directions[referenceIndex][candidateIndex] = Direction.UP;
                }
            }
        }
        List<AlignedToken> reversed = new ArrayList<>();
        int referenceIndex = referenceSize;
        int candidateIndex = candidateSize;
        while (referenceIndex > 0 || candidateIndex > 0) {
            Direction direction = directions[referenceIndex][candidateIndex];
            if (direction == Direction.DIAGONAL) {
                String candidateValue = candidateTokens.get(candidateIndex - 1);
                reversed.add(new AlignedToken(referenceIndex - 1, referenceIndex, candidateValue));
                referenceIndex--;
                candidateIndex--;
            } else if (direction == Direction.LEFT) {
                reversed.add(new AlignedToken(-1, referenceIndex, candidateTokens.get(candidateIndex - 1)));
                candidateIndex--;
            } else {
                referenceIndex--;
            }
        }
        return new SequenceAlignment(reversed.reversed());
    }

    private static double alignmentScore(List<String> referenceTokens, List<String> candidateTokens) {
        int referenceSize = referenceTokens.size();
        int candidateSize = candidateTokens.size();
        double[][] scores = new double[referenceSize + 1][candidateSize + 1];
        for (int referenceIndex = 1; referenceIndex <= referenceSize; referenceIndex++) {
            scores[referenceIndex][0] = referenceIndex * GAP_SCORE;
        }
        for (int candidateIndex = 1; candidateIndex <= candidateSize; candidateIndex++) {
            scores[0][candidateIndex] = candidateIndex * GAP_SCORE;
        }
        for (int referenceIndex = 1; referenceIndex <= referenceSize; referenceIndex++) {
            for (int candidateIndex = 1; candidateIndex <= candidateSize; candidateIndex++) {
                double diagonal = scores[referenceIndex - 1][candidateIndex - 1]
                        + tokenSimilarity(referenceTokens.get(referenceIndex - 1), candidateTokens.get(candidateIndex - 1));
                double up = scores[referenceIndex - 1][candidateIndex] + GAP_SCORE;
                double left = scores[referenceIndex][candidateIndex - 1] + GAP_SCORE;
                scores[referenceIndex][candidateIndex] = Math.max(diagonal, Math.max(up, left));
            }
        }
        return scores[referenceSize][candidateSize];
    }

    private static double normalizedAlignmentScore(List<String> referenceTokens, List<String> candidateTokens) {
        int alignmentLength = Math.max(1, Math.max(referenceTokens.size(), candidateTokens.size()));
        return alignmentScore(referenceTokens, candidateTokens) / alignmentLength;
    }

    private static double tokenSimilarity(String left, String right) {
        if (left.equals(right)) {
            return 8.0d;
        }
        NumericToken leftToken = NumericToken.parse(left);
        NumericToken rightToken = NumericToken.parse(right);
        if (leftToken.values().length != rightToken.values().length) {
            return INCOMPATIBLE_TOKEN_SCORE;
        }
        double worstCloseness = 1.0d;
        for (int index = 0; index < leftToken.values().length; index++) {
            double leftValue = leftToken.values()[index];
            double rightValue = rightToken.values()[index];
            double tolerance = Math.max(0.11d, Math.max(Math.abs(leftValue), Math.abs(rightValue)) * 0.01d);
            double closeness = 1.0d - Math.min(1.0d, Math.abs(leftValue - rightValue) / tolerance);
            worstCloseness = Math.min(worstCloseness, closeness);
        }
        if (worstCloseness <= 0.0d) {
            return INCOMPATIBLE_TOKEN_SCORE;
        }
        double unitPenalty = leftToken.percent() == rightToken.percent() ? 0.0d : 0.35d;
        double signPenalty = leftToken.sign() == rightToken.sign() ? 0.0d : 0.25d;
        return 1.0d + worstCloseness * 4.0d - unitPenalty - signPenalty;
    }

    private static boolean mixedTokenShape(Iterable<String> values) {
        Integer firstShape = null;
        for (String value : values) {
            int shape = NumericToken.parse(value).values().length;
            if (firstShape == null) {
                firstShape = shape;
            } else if (firstShape != shape) {
                return true;
            }
        }
        return false;
    }

    private enum Direction {
        DIAGONAL,
        UP,
        LEFT
    }

    private record SequenceAlignment(List<AlignedToken> tokens) {
        private SequenceAlignment {
            tokens = List.copyOf(tokens);
        }
    }

    private record AlignedToken(int referenceIndex, int insertionSlot, String value) {
    }

    private record TokenVote(String profile, String value) {
    }

    private static final class EvidenceColumn {
        private final boolean inserted;
        private final List<TokenVote> votes = new ArrayList<>();

        private EvidenceColumn(boolean inserted) {
            this.inserted = inserted;
        }

        private void add(String profile, String value) {
            if (!hasProfile(profile)) {
                votes.add(new TokenVote(profile, value));
            }
        }

        private boolean hasProfile(String profile) {
            return votes.stream().anyMatch(vote -> vote.profile().equals(profile));
        }

        private double similarityTo(String value) {
            return votes.stream()
                    .mapToDouble(vote -> tokenSimilarity(vote.value(), value))
                    .max()
                    .orElse(0.0d);
        }

        private boolean isInsertedNoise() {
            return inserted && votes.size() < MIN_SUPPORTING_PROFILES;
        }

        private List<TokenVote> votes() {
            return List.copyOf(votes);
        }
    }

    private record NumericToken(double[] values, boolean percent, char sign) {
        private static NumericToken parse(String rawValue) {
            String normalized = rawValue
                    .replace(" ", "")
                    .replace(',', '.');
            boolean percent = normalized.contains("%");
            char sign = normalized.startsWith("+") || normalized.startsWith("-")
                    ? normalized.charAt(0)
                    : '\0';
            String cleaned = normalized.replace("%", "");
            int rangeSeparator = cleaned.indexOf('-', sign == '\0' ? 0 : 1);
            if (rangeSeparator >= 0) {
                return new NumericToken(new double[]{
                        parseNumber(cleaned.substring(0, rangeSeparator)),
                        parseNumber(cleaned.substring(rangeSeparator + 1))
                }, percent, sign);
            }
            return new NumericToken(new double[]{parseNumber(cleaned)}, percent, sign);
        }

        private static double parseNumber(String value) {
            String cleaned = value.replace("+", "");
            if (cleaned.isBlank() || cleaned.equals("-")) {
                return 0.0d;
            }
            return Double.parseDouble(cleaned);
        }
    }

    record Aggregation(List<PropertyBlockNumericEvidence> evidence, List<String> diagnostics) {
        Aggregation {
            evidence = List.copyOf(evidence);
            diagnostics = List.copyOf(diagnostics);
        }
    }
}
