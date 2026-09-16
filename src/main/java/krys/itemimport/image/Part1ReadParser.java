package krys.itemimport.image;

import krys.item.EquipmentSlot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser neutralnego odczytu PART_1 z surowych wyników OCR i detektora gwiazdek. */
public final class Part1ReadParser {
    private static final Pattern ITEM_POWER_PATTERN =
            Pattern.compile("(?iu)\\bmoc\\b\\D{0,40}(\\d{2,4})");
    private static final Pattern QUALITY_PATTERN =
            Pattern.compile("(?iu)^\\D*(\\d{1,3})\\s*\\([^+)]*\\+\\s*(\\d{1,3})\\s*\\)\\s*jako");
    private static final List<TypeMapping> TYPE_MAPPINGS = List.of(
            new TypeMapping("tarcza", "Tarcza", EquipmentSlot.OFF_HAND),
            new TypeMapping("hełm", "Hełm", EquipmentSlot.HELMET),
            new TypeMapping("buty", "Buty", EquipmentSlot.BOOTS),
            new TypeMapping("miecz", "Miecz", EquipmentSlot.MAIN_HAND)
    );

    public Part1ReadResult parse(List<Part1RoiOcrResult> ocrResults) {
        Objects.requireNonNull(ocrResults, "ocrResults");
        EnumMap<Part1RoiType, Part1RoiOcrResult> byType = new EnumMap<>(Part1RoiType.class);
        for (Part1RoiOcrResult result : ocrResults) {
            byType.put(result.getType(), result);
        }

        List<String> failures = new ArrayList<>();
        Part1RoiOcrResult nameResult = byType.get(Part1RoiType.NAME_AND_STARS);
        String itemName = normalizeText(rawText(nameResult));
        if (itemName.isBlank()) {
            failures.add("Brak nazwy itemu w NAME_AND_STARS.");
        }
        int greaterAffixCount = nameResult == null ? 0 : nameResult.getGreaterAffixCount();
        String classificationRaw = normalizeText(rawText(byType.get(Part1RoiType.CLASSIFICATION)));
        Classification classification = parseClassification(classificationRaw, failures);
        OptionalInt itemPower = parseItemPower(rawText(byType.get(Part1RoiType.ITEM_POWER)));
        if (!byType.containsKey(Part1RoiType.ITEM_POWER) || rawText(byType.get(Part1RoiType.ITEM_POWER)).isBlank()) {
            failures.add("Brak mocy przedmiotu w ITEM_POWER.");
        } else if (itemPower.isEmpty()) {
            failures.add("Nie udało się sparsować mocy przedmiotu z ITEM_POWER.");
        }

        Quality quality = parseQuality(rawText(byType.get(Part1RoiType.OPTIONAL_INFO)), failures);
        return new Part1ReadResult(
                itemName,
                greaterAffixCount,
                classificationRaw,
                classification.itemType(),
                classification.itemRarity(),
                classification.ancient(),
                classification.mythicUnique(),
                classification.equipmentSlot(),
                itemPower,
                quality.displayedQuality(),
                quality.masterworkingQuality(),
                byType,
                failures
        );
    }

    private static String rawText(Part1RoiOcrResult result) {
        return result == null ? "" : result.getRawText();
    }

    private static String normalizeText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }
        return rawText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static OptionalInt parseItemPower(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return OptionalInt.empty();
        }
        Matcher matcher = ITEM_POWER_PATTERN.matcher(normalizeText(rawText));
        if (!matcher.find()) {
            return OptionalInt.empty();
        }
        return parsePositiveInt(matcher.group(1));
    }

    private static Classification parseClassification(String classificationRaw, List<String> failures) {
        if (classificationRaw.isBlank()) {
            failures.add("Brak klasyfikacji w CLASSIFICATION.");
            return Classification.empty();
        }
        String normalized = normalizeForMatching(classificationRaw);
        Optional<Boolean> ancient = Optional.of(normalized.contains("starozytn"));
        Optional<Boolean> mythicUnique = Optional.of(normalized.contains("mityczn") && normalized.contains("unikat"));

        String itemRarity = "";
        if (normalized.contains("legendarn")) {
            itemRarity = "LEGENDARY";
        }
        if (normalized.contains("unikat")) {
            if (!itemRarity.isBlank()) {
                failures.add("Niejednoznaczna rzadkość w CLASSIFICATION: " + classificationRaw);
                itemRarity = "";
            } else {
                itemRarity = "UNIQUE";
            }
        }
        if (itemRarity.isBlank()) {
            failures.add("Nie udało się sparsować rzadkości z CLASSIFICATION: " + classificationRaw);
        }

        List<TypeMapping> matchedTypes = TYPE_MAPPINGS.stream()
                .filter(mapping -> containsToken(normalized, mapping.normalizedToken()))
                .toList();
        String itemType = "";
        Optional<EquipmentSlot> equipmentSlot = Optional.empty();
        if (matchedTypes.size() == 1) {
            itemType = matchedTypes.getFirst().itemType();
            equipmentSlot = Optional.of(matchedTypes.getFirst().equipmentSlot());
        } else if (matchedTypes.isEmpty()) {
            failures.add("Nie udało się sparsować typu itemu z CLASSIFICATION: " + classificationRaw);
        } else {
            failures.add("Niejednoznaczny typ itemu w CLASSIFICATION: " + classificationRaw);
        }

        return new Classification(itemType, itemRarity, ancient, mythicUnique, equipmentSlot);
    }

    private static Quality parseQuality(String rawText, List<String> failures) {
        if (rawText == null || rawText.isBlank()) {
            return Quality.empty();
        }
        for (String line : rawText.lines().map(String::trim).filter(line -> !line.isBlank()).toList()) {
            if (isIgnoredOptionalInfoLine(line)) {
                continue;
            }
            Matcher matcher = QUALITY_PATTERN.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            OptionalInt displayedQuality = parsePositiveInt(matcher.group(1));
            OptionalInt masterworkingQuality = parsePositiveInt(matcher.group(2));
            if (displayedQuality.isPresent() && masterworkingQuality.isPresent()) {
                return new Quality(displayedQuality, masterworkingQuality);
            }
        }
        if (rawText.lines().map(String::trim).filter(line -> !line.isBlank())
                .filter(line -> !isIgnoredOptionalInfoLine(line))
                .anyMatch(Part1ReadParser::looksLikeQualityLine)) {
            failures.add("Nie udało się sparsować jakości z OPTIONAL_INFO.");
        }
        return Quality.empty();
    }

    private static boolean isIgnoredOptionalInfoLine(String line) {
        String normalized = normalizeForMatching(line);
        return normalized.contains("rynsztunek")
                || normalized.contains("zbrojowni")
                || normalized.contains("przeistoczony");
    }

    private static boolean looksLikeQualityLine(String line) {
        String normalized = normalizeForMatching(line);
        return normalized.contains("jakosc")
                || normalized.contains("jakosci")
                || (normalized.contains("+") && normalized.matches(".*\\d.*"));
    }

    private static boolean containsToken(String normalizedText, String normalizedToken) {
        return Pattern.compile("(^|\\s)" + Pattern.quote(normalizedToken) + "($|\\s)")
                .matcher(normalizedText)
                .find();
    }

    private static String normalizeForMatching(String text) {
        return normalizeText(text)
                .toLowerCase(Locale.ROOT)
                .replace('ą', 'a')
                .replace('ć', 'c')
                .replace('ę', 'e')
                .replace('ł', 'l')
                .replace('ń', 'n')
                .replace('ó', 'o')
                .replace('ś', 's')
                .replace('ż', 'z')
                .replace('ź', 'z');
    }

    private static OptionalInt parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 0 ? OptionalInt.of(parsed) : OptionalInt.empty();
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }

    private record Quality(OptionalInt displayedQuality, OptionalInt masterworkingQuality) {
        private static Quality empty() {
            return new Quality(OptionalInt.empty(), OptionalInt.empty());
        }
    }

    private record Classification(String itemType,
                                  String itemRarity,
                                  Optional<Boolean> ancient,
                                  Optional<Boolean> mythicUnique,
                                  Optional<EquipmentSlot> equipmentSlot) {
        private static Classification empty() {
            return new Classification("", "", Optional.empty(), Optional.empty(), Optional.empty());
        }
    }

    private record TypeMapping(String token, String itemType, EquipmentSlot equipmentSlot) {
        private String normalizedToken() {
            return normalizeForMatching(token);
        }
    }
}
