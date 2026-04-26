package krys.itemimport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        Map<String, ImportedItemAffix> affixes = new LinkedHashMap<>();
        int displayOrder = 0;
        for (FullItemReadLine line : fullItemRead.getLines()) {
            if (!isEditableAffixLine(line)) {
                continue;
            }
            Optional<ImportedItemAffixType> type = ImportedItemAffixType.detectFromLine(line.getText());
            Optional<Double> value = firstNumber(line.getText());
            if (type.isPresent() && value.isPresent()) {
                ImportedItemAffix affix = new ImportedItemAffix(
                        type.get(),
                        value.get(),
                        defaultUnit(type.get()),
                        isGreaterAffixLine(line),
                        displayOrder,
                        line.getText(),
                        ImportedItemAffixSource.OCR
                );
                affixes.putIfAbsent(editableAffixDeduplicationKey(affix), affix);
                displayOrder++;
            }
        }
        return new ArrayList<>(affixes.values());
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

    private static String editableAffixDeduplicationKey(ImportedItemAffix affix) {
        return affix.getType().name()
                + "|"
                + String.format(Locale.US, "%.4f", affix.getValue())
                + "|"
                + normalize(affix.getSourceText()).replaceAll("\\s+", " ").trim();
    }

    private static boolean isGreaterAffixLine(FullItemReadLine line) {
        if (line == null || !isEditableAffixLine(line)) {
            return false;
        }
        String trimmedLine = line.getText().trim();
        return trimmedLine.startsWith("*")
                || trimmedLine.startsWith("★")
                || trimmedLine.startsWith("⭐")
                || trimmedLine.startsWith("✦")
                || !hasRollRangeOrRangeFragment(trimmedLine);
    }

    static boolean hasRollRangeOrRangeFragment(String line) {
        return line != null && line.contains("[");
    }

    private static String defaultUnit(ImportedItemAffixType type) {
        return switch (type) {
            case BLOCK_CHANCE, RETRIBUTION_CHANCE, LUCKY_HIT_CHANCE, COOLDOWN_REDUCTION,
                 MOVEMENT_SPEED, DODGE_CHANCE -> "%";
            case STRENGTH, INTELLIGENCE, THORNS -> "";
        };
    }

    private static String normalize(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(java.util.Locale.ROOT);
    }
}
