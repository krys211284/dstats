package krys.web;

import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Buduje bezpieczną prezentację pomocniczego odczytu OCR efektu aspektu. */
final class ItemAspectEffectPresentation {
    static final String INCOMPLETE_EFFECT_MESSAGE = "Odczyt efektu OCR niepełny / wymaga ręcznej weryfikacji.";

    private ItemAspectEffectPresentation() {
    }

    static List<String> effectLines(FullItemRead fullItemRead) {
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return List.of();
        }
        List<String> safeHeads = new ArrayList<>();
        List<String> tails = new ArrayList<>();
        List<String> otherEffects = new ArrayList<>();
        boolean skippedBrokenEffect = false;

        for (FullItemReadLine line : fullItemRead.getLines()) {
            String text = line.getText();
            if (!isAspectLike(line)) {
                continue;
            }
            String normalized = normalize(text);
            boolean hasHead = normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE");
            boolean hasTail = normalized.contains("TA PREMIA JEST");
            if (hasHead && isSafeEffectHead(normalized)) {
                addUnique(safeHeads, text);
                continue;
            }
            if (hasHead) {
                skippedBrokenEffect = true;
                continue;
            }
            if (hasTail) {
                addUnique(tails, text);
                continue;
            }
            addUnique(otherEffects, text);
        }

        List<String> result = new ArrayList<>();
        if (!safeHeads.isEmpty()) {
            String tail = tails.isEmpty() ? "" : tails.getFirst();
            for (String head : safeHeads) {
                addUnique(result, "Odczyt OCR efektu: " + (tail.isBlank() ? head : head + " " + tail));
            }
            return result;
        }
        for (String effect : otherEffects) {
            addUnique(result, "Odczyt OCR efektu: " + effect);
        }
        if (result.isEmpty() && (!tails.isEmpty() || skippedBrokenEffect)) {
            result.add(INCOMPLETE_EFFECT_MESSAGE);
        }
        return result;
    }

    private static boolean isAspectLike(FullItemReadLine line) {
        String normalized = normalize(line.getText());
        return line.getType() == FullItemReadLineType.ASPECT
                || normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE")
                || normalized.contains("TA PREMIA JEST");
    }

    private static boolean isSafeEffectHead(String normalized) {
        return normalized.contains("%[X]")
                && !normalized.contains("TA PREMIA JEST");
    }

    private static void addUnique(List<String> lines, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalizedValue = normalize(value).replaceAll("\\s+", " ").trim();
        for (String line : lines) {
            if (normalize(line).replaceAll("\\s+", " ").trim().equals(normalizedValue)) {
                return;
            }
        }
        lines.add(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }
}
