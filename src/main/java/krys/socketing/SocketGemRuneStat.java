package krys.socketing;

import krys.itemimport.ImportedItemAffixType;

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

    public SocketGemRuneStat(String displayText,
                             String normalizedText,
                             Double value,
                             ImportedItemAffixType matchedAffixType,
                             String sourceLine,
                             String sourceRegion,
                             String runtimeStatus,
                             boolean needsReview) {
        this.displayText = safe(displayText);
        this.normalizedText = normalizedText == null || normalizedText.isBlank()
                ? normalize(displayText)
                : normalizedText.trim();
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
    }

    public static SocketGemRuneStat fromDetectedLine(String line) {
        String text = safe(line);
        return new SocketGemRuneStat(
                text,
                normalize(text),
                firstNumber(text),
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

    public boolean hasDisplayText() {
        return !displayText.isBlank();
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

    private static String normalize(String value) {
        return Normalizer.normalize(safe(value), Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
