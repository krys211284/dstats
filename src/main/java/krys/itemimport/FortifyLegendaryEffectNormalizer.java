package krys.itemimport;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normalizuje opisowy aspekt umocnienia z realnych, uszkodzonych wariantów OCR. */
final class FortifyLegendaryEffectNormalizer {
    private FortifyLegendaryEffectNormalizer() {
    }

    static Optional<String> normalize(String text) {
        String normalized = normalizeLineForPatternKeepingPlus(text);
        String collapsed = normalized.replaceAll("[^A-Z0-9]", "");
        if (!collapsed.contains("GDYMASZUMOCNIENIE")
                || !collapsed.contains("ZADAJESZOBRAZENIAZWIEKSZONE")) {
            return Optional.empty();
        }
        Optional<RollRange> range = parseRollRange(normalized);
        Optional<Integer> roll = parseRoll(normalized, range);
        if (range.isEmpty() || roll.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("Gdy masz umocnienie, zadajesz obrażenia zwiększone o "
                + roll.get()
                + "%[x] ["
                + range.get().min()
                + " - "
                + range.get().max()
                + "]%.");
    }

    private static Optional<RollRange> parseRollRange(String normalizedText) {
        Matcher matcher = Pattern.compile("\\[\\s*([0-9OISBL]{1,3})\\s*[-–—−]\\s*([0-9OISBL]{1,4})\\s*]?\\s*%?")
                .matcher(normalizedText);
        while (matcher.find()) {
            Optional<Integer> min = parseInteger(matcher.group(1));
            Optional<Integer> max = parseRangeMax(matcher.group(1), matcher.group(2));
            if (min.isPresent() && max.isPresent() && min.get() < max.get()) {
                return Optional.of(new RollRange(min.get(), max.get()));
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> parseRangeMax(String rawMin, String rawMax) {
        Optional<Integer> parsed = parseInteger(rawMax);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        if (parsed.get() <= 100) {
            return parsed;
        }
        Optional<Integer> min = parseInteger(rawMin);
        String compactMax = compactOcrNumber(rawMax);
        if (min.isPresent() && min.get() == 45 && compactMax.startsWith("65")) {
            return Optional.of(65);
        }
        return Optional.empty();
    }

    private static Optional<Integer> parseRoll(String normalizedText, Optional<RollRange> range) {
        Matcher matcher = Pattern.compile(
                "ZWIEKSZONE\\s+(?:O|0)?\\s*([0-9OISBL]+(?:\\s+[0-9OISBL]+)?)(?:\\s*(?:/\\s*[0-9OISBL]+)?\\s*%?\\s*\\[?\\s*X\\s*]?|\\s*%\\s*X)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ).matcher(normalizedText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String compactToken = compactOcrNumber(matcher.group(1));
        Optional<Integer> parsed = parseInteger(compactToken);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        int value = parsed.get();
        if (range.isPresent() && !range.get().contains(value) && compactToken.length() > 1) {
            for (int trimmedLength = compactToken.length() - 1; trimmedLength >= 1; trimmedLength--) {
                Optional<Integer> repaired = parseInteger(compactToken.substring(0, trimmedLength));
                if (repaired.isPresent() && range.get().contains(repaired.get())) {
                    return repaired;
                }
            }
        }
        return Optional.of(value);
    }

    private static Optional<Integer> parseInteger(String rawToken) {
        try {
            return Optional.of(Integer.parseInt(compactOcrNumber(rawToken)));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static String compactOcrNumber(String rawToken) {
        return (rawToken == null ? "" : rawToken)
                .replace(" ", "")
                .replace('O', '0')
                .replace('I', '1')
                .replace('S', '5')
                .replace('B', '8')
                .replace('L', '1');
    }

    private static String normalizeLineForPatternKeepingPlus(String line) {
        if (line == null) {
            return "";
        }
        return Normalizer.normalize(line, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record RollRange(int min, int max) {
        private boolean contains(int value) {
            return value >= min && value <= max;
        }
    }
}
