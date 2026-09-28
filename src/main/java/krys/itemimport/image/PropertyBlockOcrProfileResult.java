package krys.itemimport.image;

import java.util.List;

/** Diagnostyczny wynik pojedynczego profilu OCR bez interpretacji domenowej. */
public final class PropertyBlockOcrProfileResult {
    private final String profile;
    private final String rawText;
    private final List<String> numericTokens;
    private final double score;
    private final String failure;

    public PropertyBlockOcrProfileResult(String profile,
                                         String rawText,
                                         List<String> numericTokens,
                                         double score,
                                         String failure) {
        this.profile = profile == null ? "" : profile;
        this.rawText = rawText == null ? "" : rawText;
        this.numericTokens = List.copyOf(numericTokens == null ? List.of() : numericTokens);
        this.score = score;
        this.failure = failure == null ? "" : failure;
    }

    public String getProfile() {
        return profile;
    }

    public String getRawText() {
        return rawText;
    }

    public List<String> getNumericTokens() {
        return numericTokens;
    }

    public double getScore() {
        return score;
    }

    public String getFailure() {
        return failure;
    }
}
