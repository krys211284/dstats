package krys.itemimport;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Scala tekst OCR z kilku screenów tego samego itemu bez sortowania linii tooltipa. */
public final class ItemScreenshotTextMerger {
    public String merge(List<String> ocrTexts) {
        if (ocrTexts == null || ocrTexts.isEmpty()) {
            return "";
        }
        List<String> mergedLines = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String ocrText : ocrTexts) {
            if (ocrText == null || ocrText.isBlank()) {
                continue;
            }
            for (String rawLine : ocrText.split("\\R")) {
                String line = rawLine == null ? "" : rawLine.trim().replaceAll("\\s+", " ");
                if (line.isBlank() || isUiOnlyLine(line)) {
                    continue;
                }
                String key = comparisonKey(line);
                if (seen.add(key)) {
                    mergedLines.add(line);
                }
            }
        }
        return String.join(System.lineSeparator(), mergedLines);
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
                || key.matches("wymaga [0-9]+ poziomu")
                || key.startsWith("wartosc sprzedazy:")
                || key.startsWith("trwalosc:")
                || isDurabilityComparisonNoise(key);
    }

    private static boolean isDurabilityComparisonNoise(String key) {
        return key.startsWith("(wytrzymalosc:") && key.endsWith(")");
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
}
