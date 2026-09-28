package krys.itemimport.image;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Agreguje neutralne głosy liczbowe profili OCR bez wiedzy o afiksach. */
final class PropertyBlockNumericEvidenceAggregator {
    static final String AMBIGUOUS_DIAGNOSTIC = "NUMERIC_OCR_AMBIGUOUS";
    private static final int MIN_SUPPORTING_PROFILES = 4;

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
        int tokenCount = modalTokenCount(votingProfiles);
        List<PropertyBlockOcrProfileResult> alignedProfiles = votingProfiles.stream()
                .filter(profile -> profile.getNumericTokens().size() == tokenCount)
                .toList();
        List<PropertyBlockNumericEvidence> evidence = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (int tokenIndex = 0; tokenIndex < tokenCount; tokenIndex++) {
            Map<String, List<String>> profilesByValue = new LinkedHashMap<>();
            for (PropertyBlockOcrProfileResult profile : alignedProfiles) {
                String value = profile.getNumericTokens().get(tokenIndex);
                profilesByValue.computeIfAbsent(value, ignored -> new ArrayList<>())
                        .add(profile.getProfile());
            }
            Map.Entry<String, List<String>> strongest = profilesByValue.entrySet().stream()
                    .max((left, right) -> Integer.compare(left.getValue().size(), right.getValue().size()))
                    .orElseThrow();
            int supportCount = strongest.getValue().size();
            boolean ambiguous = supportCount < MIN_SUPPORTING_PROFILES
                    || supportCount * 3 < alignedProfiles.size() * 2;
            String selectedValue = ambiguous ? "" : strongest.getKey();
            List<String> alternatives = profilesByValue.keySet().stream()
                    .filter(value -> ambiguous || !value.equals(selectedValue))
                    .toList();
            evidence.add(new PropertyBlockNumericEvidence(
                    tokenIndex + 1,
                    selectedValue,
                    strongest.getValue(),
                    alternatives,
                    supportCount,
                    ambiguous
            ));
            if (ambiguous) {
                diagnostics.add(AMBIGUOUS_DIAGNOSTIC + " tokenOrder=" + (tokenIndex + 1)
                        + " values=" + profilesByValue.keySet());
            }
        }
        return new Aggregation(evidence, diagnostics);
    }

    private static int modalTokenCount(List<PropertyBlockOcrProfileResult> profiles) {
        Map<Integer, Integer> frequencies = new LinkedHashMap<>();
        for (PropertyBlockOcrProfileResult profile : profiles) {
            frequencies.merge(profile.getNumericTokens().size(), 1, Integer::sum);
        }
        return frequencies.entrySet().stream()
                .max((left, right) -> {
                    int frequencyComparison = Integer.compare(left.getValue(), right.getValue());
                    return frequencyComparison != 0
                            ? frequencyComparison
                            : Integer.compare(right.getKey(), left.getKey());
                })
                .orElseThrow()
                .getKey();
    }

    record Aggregation(List<PropertyBlockNumericEvidence> evidence, List<String> diagnostics) {
        Aggregation {
            evidence = List.copyOf(evidence);
            diagnostics = List.copyOf(diagnostics);
        }
    }
}
