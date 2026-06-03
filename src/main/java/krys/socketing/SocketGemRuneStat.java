package krys.socketing;

import krys.itemimport.ImportedItemAffixType;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

/** Statystyka wykryta w regionie socket / gem / rune; dane prezentacyjne, runtime nieaktywny. */
public final class SocketGemRuneStat {
    public static final String RUNTIME_STATUS = "DATA_ONLY";

    private final String displayText;
    private final String normalizedText;
    private final Double value;
    private final ImportedItemAffixType matchedAffixType;
    private final String sourceLine;
    private final String sourceRegion;
    private final String runtimeStatus;
    private final boolean needsReview;
    private final String damageType;
    private final String semanticKey;

    public SocketGemRuneStat(String displayText,
                             String normalizedText,
                             Double value,
                             ImportedItemAffixType matchedAffixType,
                             String sourceLine,
                             String sourceRegion,
                             String runtimeStatus,
                             boolean needsReview) {
        String rawDisplayText = safe(displayText);
        String normalized = normalizedText == null || normalizedText.isBlank()
                ? normalize(rawDisplayText)
                : normalizedText.trim();
        String canonicalPhysicalDisplay = canonicalPhysicalDamageMultiplierDisplay(rawDisplayText, normalized, value);
        this.displayText = canonicalPhysicalDisplay.isBlank() ? rawDisplayText : canonicalPhysicalDisplay;
        this.normalizedText = normalize(this.displayText);
        this.value = value;
        this.matchedAffixType = matchedAffixType;
        this.sourceLine = safe(sourceLine);
        this.sourceRegion = sourceRegion == null || sourceRegion.isBlank()
                ? "SOCKET_GEM_RUNE_REGION"
                : sourceRegion.trim();
        this.runtimeStatus = runtimeStatus == null || runtimeStatus.isBlank()
                ? RUNTIME_STATUS
                : runtimeStatus.trim();
        this.needsReview = needsReview;
        this.damageType = detectDamageType(this.normalizedText);
        this.semanticKey = buildSemanticKey();
    }

    public static SocketGemRuneStat fromDetectedLine(String line) {
        String text = safe(line);
        String normalized = normalize(text);
        Double localPhysicalMultiplier = physicalDamageMultiplierValue(text, normalized);
        Double value = localPhysicalMultiplier == null ? firstNumber(text) : localPhysicalMultiplier;
        String displayText = localPhysicalMultiplier == null
                ? text
                : "Mnożnik x" + normalizedNumber(localPhysicalMultiplier).replace('.', ',') + "% obrażeń (Fizyczne)";
        return new SocketGemRuneStat(
                displayText,
                normalized,
                value,
                ImportedItemAffixType.detectFromLine(text).orElse(null),
                text,
                "SOCKET_GEM_RUNE_REGION",
                RUNTIME_STATUS,
                false
        );
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public Double getValue() {
        return value;
    }

    public ImportedItemAffixType getMatchedAffixType() {
        return matchedAffixType;
    }

    public String getSourceLine() {
        return sourceLine;
    }

    public String getSourceRegion() {
        return sourceRegion;
    }

    public String getRuntimeStatus() {
        return runtimeStatus;
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public String getDamageType() {
        return damageType;
    }

    public String getSemanticKey() {
        return semanticKey;
    }

    public boolean hasDisplayText() {
        return !displayText.isBlank();
    }

    public boolean hasLoreTail() {
        String normalizedSource = normalize(sourceLine);
        return normalizedSource.contains("TO OSTRZE")
                || normalizedSource.contains("W REKACH")
                || normalizedSource.length() > normalizedText.length() + 8;
    }

    public boolean shouldDeduplicateBySemanticKey() {
        return semanticKey.startsWith("PHYSICAL_DAMAGE_MULTIPLIER|");
    }

    public int sourceQualityScore() {
        int score = 1000;
        if (!hasLoreTail()) {
            score += 300;
        }
        score -= Math.max(0, sourceLine.length() - displayText.length()) * 4;
        score -= Math.max(0, sourceLine.length() - 64);
        return score;
    }

    private static Double firstNumber(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)")
                .matcher(safe(value));
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group(1).replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Double physicalDamageMultiplierValue(String rawText, String normalizedText) {
        if (!isPhysicalDamageMultiplier(normalizedText)) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("MNOZNIK\\s*X\\s*([0-9]+(?:[,.][0-9]+)?)\\s*%?\\s*OBRAZEN[^A-Z0-9]*(?:\\(?\\s*FIZYCZNE\\s*\\)?|PHYSICAL)")
                .matcher(normalizedText);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group(1).replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(safe(value), Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String detectDamageType(String normalizedText) {
        String normalized = normalizedText == null ? "" : normalizedText;
        if (normalized.contains("FIZYCZNE") || normalized.contains("PHYSICAL")) {
            return "PHYSICAL";
        }
        return "";
    }

    private String buildSemanticKey() {
        if (isPhysicalDamageMultiplier(normalizedText)) {
            return "PHYSICAL_DAMAGE_MULTIPLIER|"
                    + normalizedNumber(value)
                    + "|"
                    + damageType
                    + "|"
                    + runtimeStatus;
        }
        String kind = matchedAffixType == null ? normalizedText.replaceAll("[0-9,. ]+", "#") : matchedAffixType.name();
        return kind + "|" + normalizedNumber(value) + "|" + damageType + "|" + runtimeStatus;
    }

    private static String canonicalPhysicalDamageMultiplierDisplay(String rawText, String normalizedText, Double parsedValue) {
        if (!isPhysicalDamageMultiplier(normalizedText)) {
            return "";
        }
        Double value = parsedValue == null ? firstNumber(rawText) : parsedValue;
        if (value == null) {
            return "";
        }
        return "Mnożnik x" + normalizedNumber(value).replace('.', ',') + "% obrażeń (Fizyczne)";
    }

    private static boolean isPhysicalDamageMultiplier(String normalizedText) {
        String normalized = normalizedText == null ? "" : normalizedText;
        return normalized.contains("MNOZNIK")
                && normalized.contains("OBRAZEN")
                && (normalized.contains("FIZYCZNE") || normalized.contains("PHYSICAL"));
    }

    private static String normalizedNumber(Double value) {
        if (value == null) {
            return "";
        }
        return BigDecimal.valueOf(value)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
