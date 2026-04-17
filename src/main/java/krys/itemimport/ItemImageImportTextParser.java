package krys.itemimport;

import krys.item.EquipmentSlot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mapuje surowy tekst OCR ograniczonego foundation do candidate parse result pojedynczego itemu. */
final class ItemImageImportTextParser {
    private static final Pattern OCR_NUMBER_PATTERN = Pattern.compile("([0-9OISBL]+(?:[.,][0-9OISBL]+)?)");

    ItemImageImportCandidateParseResult parse(ItemImageMetadata metadata, String ocrText) {
        List<String> lines = normalizedLines(ocrText);
        ItemImportFieldCandidate<EquipmentSlot> slotCandidate = detectSlot(lines);
        ItemImportFieldCandidate<Long> weaponDamageCandidate = detectLong(lines, "WEAPON DAMAGE",
                List.of("WEAPON\\s*DAMAGE"), List.of("DAMAGE"));
        ItemImportFieldCandidate<Double> strengthCandidate = detectDouble(lines, "Strength",
                List.of("STRENGTH", "(?:DO\\s+)?SILY", "(?:DO\\s+)?SILE", "(?:DO\\s+)?SILA"),
                List.of("STRENGTH", "SIL"));
        ItemImportFieldCandidate<Double> intelligenceCandidate = detectDouble(lines, "Intelligence",
                List.of("INTELLIGENCE", "(?:DO\\s+)?INTELIGENCJI", "(?:DO\\s+)?INTELIGENCJA"),
                List.of("INTELIGENC"));
        ItemImportFieldCandidate<Double> thornsCandidate = detectDouble(lines, "Thorns",
                List.of("THORNS", "(?:DO\\s+)?CIERNI", "CIERNIE"),
                List.of("THORNS", "CIERN"));
        ItemImportFieldCandidate<Double> blockChanceCandidate = detectDouble(lines, "Block chance",
                List.of("BLOCK\\s*CHANCE", "SZANSA\\s+NA\\s+BLOK", "SZANSY\\s+NA\\s+BLOK"),
                List.of("BLOCK", "BLOK"));
        ItemImportFieldCandidate<Double> retributionChanceCandidate = detectDouble(lines, "Retribution chance",
                List.of("RETRIBUTION\\s*CHANCE", "SZANSA\\s+NA\\s+ODWET", "SZANSY\\s+NA\\s+ODWET"),
                List.of("RETRIBUTION", "ODWET"));

        if (slotCandidate.getSuggestedValue() == null && weaponDamageCandidate.getSuggestedValue() != null) {
            slotCandidate = new ItemImportFieldCandidate<>(
                    weaponDamageCandidate.getRawValue(),
                    EquipmentSlot.MAIN_HAND,
                    ItemImportFieldConfidence.LOW,
                    "Slot MAIN_HAND został ostrożnie wywnioskowany z odczytanego weapon damage."
            );
        }

        return new ItemImageImportCandidateParseResult(
                metadata,
                slotCandidate,
                weaponDamageCandidate,
                strengthCandidate,
                intelligenceCandidate,
                thornsCandidate,
                blockChanceCandidate,
                retributionChanceCandidate,
                buildImportNotice(lines, slotCandidate, weaponDamageCandidate, strengthCandidate,
                        intelligenceCandidate, thornsCandidate, blockChanceCandidate, retributionChanceCandidate)
        );
    }

    private static ItemImportFieldCandidate<EquipmentSlot> detectSlot(List<String> lines) {
        for (String line : lines) {
            String collapsedLine = collapse(line);
            if (collapsedLine.contains("MAINHAND")) {
                return field(line, EquipmentSlot.MAIN_HAND, ItemImportFieldConfidence.HIGH,
                        "Slot rozpoznany bezpośrednio z tekstu OCR.");
            }
            if (collapsedLine.contains("OFFHAND")) {
                return field(line, EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.HIGH,
                        "Slot rozpoznany bezpośrednio z tekstu OCR.");
            }
            if (collapsedLine.contains("CHEST")) {
                return field(line, EquipmentSlot.CHEST, ItemImportFieldConfidence.HIGH,
                        "Slot rozpoznany bezpośrednio z tekstu OCR.");
            }
            if (collapsedLine.contains("RING")) {
                return field(line, EquipmentSlot.RING, ItemImportFieldConfidence.HIGH,
                        "Slot rozpoznany bezpośrednio z tekstu OCR.");
            }
            if (containsAny(collapsedLine, List.of("BOOTS", "BUTY", "BUCIORY", "OBUWIE"))) {
                return field(line, EquipmentSlot.BOOTS, ItemImportFieldConfidence.HIGH,
                        "Slot rozpoznany bezpośrednio z tekstu OCR.");
            }
            if (containsAny(collapsedLine, List.of("SHIELD", "TARCZA"))) {
                return field(line, EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.HIGH,
                        "Slot rozpoznany bezpośrednio z nazwy typu itemu w OCR.");
            }
            if (containsAny(collapsedLine, List.of("SWORD", "AXE", "MACE", "HAMMER", "DAGGER", "WEAPON"))) {
                return field(line, EquipmentSlot.MAIN_HAND, ItemImportFieldConfidence.MEDIUM,
                        "Slot MAIN_HAND został wywnioskowany z typu broni w OCR.");
            }
            if (containsAny(collapsedLine, List.of("FOCUS"))) {
                return field(line, EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.MEDIUM,
                        "Slot OFF_HAND został wywnioskowany z typu itemu w OCR.");
            }
            if (containsAny(collapsedLine, List.of("ARMOR", "CHESTPLATE", "BREASTPLATE"))) {
                return field(line, EquipmentSlot.CHEST, ItemImportFieldConfidence.MEDIUM,
                        "Slot CHEST został wywnioskowany z typu itemu w OCR.");
            }
            if (containsAny(collapsedLine, List.of("BAND"))) {
                return field(line, EquipmentSlot.RING, ItemImportFieldConfidence.MEDIUM,
                        "Slot RING został wywnioskowany z typu itemu w OCR.");
            }
        }
        return ItemImportFieldCandidate.unknown("Nie udało się rozpoznać slotu / typu itemu z OCR.");
    }

    private static ItemImportFieldCandidate<Long> detectLong(List<String> lines,
                                                             String label,
                                                             List<String> exactTokens,
                                                             List<String> fuzzyTokens) {
        ItemImportFieldCandidate<Double> candidate = detectDouble(lines, label, exactTokens, fuzzyTokens);
        if (candidate.getSuggestedValue() == null) {
            return ItemImportFieldCandidate.unknown(candidate.getNote());
        }
        return new ItemImportFieldCandidate<>(
                candidate.getRawValue(),
                Math.round(candidate.getSuggestedValue()),
                candidate.getConfidence(),
                candidate.getNote()
        );
    }

    private static ItemImportFieldCandidate<Double> detectDouble(List<String> lines,
                                                                 String label,
                                                                 List<String> exactPhrases,
                                                                 List<String> fuzzyPhrases) {
        for (String line : lines) {
            String normalizedLine = normalizeLineForPattern(line);
            for (String phrase : exactPhrases) {
                Optional<Double> number = extractNumberNearPhrase(normalizedLine, phrase);
                if (number.isPresent()) {
                    return field(line, number.get(), ItemImportFieldConfidence.HIGH,
                            label + " rozpoznany bezpośrednio z tekstu OCR.");
                }
            }
        }
        for (String line : lines) {
            String normalizedLine = normalizeLineForPattern(line);
            for (String phrase : fuzzyPhrases) {
                Optional<Double> number = extractNumberNearPhrase(normalizedLine, phrase);
                if (number.isPresent()) {
                    return field(line, number.get(), ItemImportFieldConfidence.MEDIUM,
                            label + " rozpoznany heurystycznie z tekstu OCR.");
                }
            }
        }
        return ItemImportFieldCandidate.unknown("Nie udało się rozpoznać pola `" + label + "` z OCR.");
    }

    private static Optional<Double> extractNumberNearPhrase(String normalizedLine, String phraseRegex) {
        Pattern phraseBeforeNumber = Pattern.compile(
                phraseRegex + "[^0-9OISBL]*" + OCR_NUMBER_PATTERN.pattern()
        );
        Matcher phraseBeforeMatcher = phraseBeforeNumber.matcher(normalizedLine);
        if (phraseBeforeMatcher.find()) {
            return parseNumericToken(phraseBeforeMatcher.group(1));
        }

        Pattern numberBeforePhrase = Pattern.compile(
                OCR_NUMBER_PATTERN.pattern() + "\\s*%?\\s*" + phraseRegex
        );
        Matcher numberBeforeMatcher = numberBeforePhrase.matcher(normalizedLine);
        if (numberBeforeMatcher.find()) {
            return parseNumericToken(numberBeforeMatcher.group(1));
        }
        return Optional.empty();
    }

    private static Optional<Double> parseNumericToken(String rawToken) {
        String numericToken = rawToken
                .replace('O', '0')
                .replace('I', '1')
                .replace('S', '5')
                .replace('B', '8')
                .replace('L', '1')
                .replace(',', '.');
        try {
            return Optional.of(Double.parseDouble(numericToken));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static boolean containsAny(String collapsedLine, List<String> tokens) {
        for (String token : tokens) {
            if (collapsedLine.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static <T> ItemImportFieldCandidate<T> field(String rawValue,
                                                         T suggestedValue,
                                                         ItemImportFieldConfidence confidence,
                                                         String note) {
        return new ItemImportFieldCandidate<>(rawValue, suggestedValue, confidence, note);
    }

    private static List<String> normalizedLines(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        for (String line : text.split("\\R")) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isBlank()) {
                lines.add(trimmedLine);
            }
        }
        return lines;
    }

    private static String buildImportNotice(List<String> lines,
                                            ItemImportFieldCandidate<?>... candidates) {
        long recognizedCount = 0L;
        for (ItemImportFieldCandidate<?> candidate : candidates) {
            if (candidate.getSuggestedValue() != null) {
                recognizedCount++;
            }
        }
        if (lines.isEmpty()) {
            return "OCR nie rozpoznał czytelnego tekstu z obrazu. Import nadal wymaga ręcznego potwierdzenia wszystkich pól.";
        }
        return "OCR rozpoznał " + recognizedCount + " z 7 pól foundation na podstawie " + lines.size()
                + " linii tekstu. Wynik nadal wymaga ręcznego potwierdzenia użytkownika.";
    }

    private static String collapse(String text) {
        return normalizePolishText(text)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
    }

    private static String normalizeLineForPattern(String line) {
        return normalizePolishText(line)
                .toUpperCase(Locale.ROOT)
                .replace('+', ' ')
                .replace('%', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizePolishText(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "");
    }
}
