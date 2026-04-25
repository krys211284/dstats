package krys.itemimport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Wyciąga edytowalne affixy z pełnego odczytu itemu. */
public final class ImportedItemAffixExtractor {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9]+(?:[,.][0-9]+)?)");

    public List<ImportedItemAffix> extractEditableAffixes(FullItemRead fullItemRead) {
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return List.of();
        }
        List<ImportedItemAffix> affixes = new ArrayList<>();
        for (FullItemReadLine line : fullItemRead.getLines()) {
            if (!isEditableAffixLine(line)) {
                continue;
            }
            Optional<ImportedItemAffixType> type = ImportedItemAffixType.detectFromLine(line.getText());
            Optional<Double> value = firstNumber(line.getText());
            if (type.isPresent() && value.isPresent()) {
                affixes.add(new ImportedItemAffix(type.get(), value.get(), line.getText()));
            }
        }
        return affixes;
    }

    static boolean isEditableAffixLine(FullItemReadLine line) {
        if (line == null || line.getType() != FullItemReadLineType.AFFIX) {
            return false;
        }
        String normalized = normalize(line.getText());
        return !normalized.contains("REDUKCJI BLOKOWANYCH OBRAZEN")
                && !normalized.contains("SZANSY NA BLOK")
                && !normalized.contains("OBRAZEN OD BRONI W GLOWNEJ RECE")
                && !normalized.contains("ROZJUSZENIE");
    }

    private static Optional<Double> firstNumber(String text) {
        Matcher matcher = NUMBER_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Double.parseDouble(matcher.group(1).replace(',', '.')));
    }

    private static String normalize(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(java.util.Locale.ROOT);
    }
}
