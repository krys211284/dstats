package krys.itemimport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normalizuje semantycznie istotne tokeny liczbowe w opisach efektow OCR. */
final class EffectTextTokenNormalizer {
    private static final Pattern BROKEN_MULTIPLIER_TOKEN = Pattern.compile(
            "(?<![0-9])([0-9]{1,3}?)(?:\\s*%|\\s*9[oóOÓ]?|\\s*[oóOÓ])\\s*\\[\\s*([xX×])\\s*]"
    );
    private static final Pattern ORDINARY_MULTIPLIER_TOKEN = Pattern.compile(
            "(?<![0-9])([0-9]{1,3})\\s*%\\s*\\[\\s*([xX×])\\s*]"
    );
    private static final Pattern NUMBER_TOKEN = Pattern.compile("[0-9]+(?:[,.][0-9]+)?");
    private static final Pattern PERCENT_TOKEN = Pattern.compile("[0-9]+(?:[,.][0-9]+)?\\s*%");
    private static final Pattern MULTIPLIER_TOKEN = Pattern.compile("[0-9]+(?:[,.][0-9]+)?\\s*%\\s*\\[\\s*[xX×]\\s*]");

    private EffectTextTokenNormalizer() {
    }

    static String normalizeMultiplierTokens(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = normalizeOrdinaryMultiplierTokens(text);
        Matcher matcher = BROKEN_MULTIPLIER_TOKEN.matcher(normalized);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + "%[x]"));
        }
        matcher.appendTail(buffer);
        return buffer.toString().replaceAll("\\s+", " ").trim();
    }

    static int semanticTokenScore(String text) {
        String normalized = normalizeMultiplierTokens(text);
        int score = 0;
        score += count(NUMBER_TOKEN, normalized) * 10;
        score += count(PERCENT_TOKEN, normalized) * 20;
        score += count(MULTIPLIER_TOKEN, normalized) * 80;
        score += countLiteral(normalized, "[") * 6;
        score += countLiteral(normalized, "]") * 6;
        score += countLiteral(normalized, "[x]") * 80;
        return score;
    }

    private static String normalizeOrdinaryMultiplierTokens(String text) {
        Matcher matcher = ORDINARY_MULTIPLIER_TOKEN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + "%[x]"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static int count(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static int countLiteral(String text, String needle) {
        if (text == null || text.isBlank() || needle == null || needle.isBlank()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
