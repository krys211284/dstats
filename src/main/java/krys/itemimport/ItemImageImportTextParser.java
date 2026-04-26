package krys.itemimport;

import krys.item.EquipmentSlot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
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
                List.of("STRENGTH"));
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
                buildFullItemRead(lines),
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

    private static FullItemRead buildFullItemRead(List<String> lines) {
        List<String> fullReadSourceLines = expandFullItemReadLines(lines);
        List<FullItemReadLine> readLines = new ArrayList<>();
        String itemName = "";
        String itemTypeLine = "";
        String rarity = "";
        String itemPower = "";
        String baseItemValue = "";

        for (String line : fullReadSourceLines) {
            FullItemReadLineType type = classifyFullReadLine(line);
            String collapsedLine = collapse(line);
            readLines.add(new FullItemReadLine(type, line));
            if (type == FullItemReadLineType.ITEM_NAME && itemName.isBlank()) {
                itemName = line;
            }
            if (itemTypeLine.isBlank() && isItemTypeLine(collapsedLine)) {
                itemTypeLine = line;
            }
            if (rarity.isBlank() && isRarityLine(collapsedLine)) {
                rarity = line;
            }
            if (type == FullItemReadLineType.ITEM_POWER && itemPower.isBlank()) {
                itemPower = line;
            }
            if (type == FullItemReadLineType.BASE_STAT && baseItemValue.isBlank()) {
                baseItemValue = line;
            }
        }
        return new FullItemRead(itemName, itemTypeLine, rarity, itemPower, baseItemValue, readLines);
    }

    private static List<String> expandFullItemReadLines(List<String> lines) {
        List<String> expandedLines = new ArrayList<>();
        for (String line : lines) {
            if (!looksLikeCondensedFullItemReadLine(line)) {
                expandedLines.add(line);
                continue;
            }
            List<String> extractedLines = extractCondensedFullItemReadLines(line);
            if (extractedLines.isEmpty()) {
                expandedLines.add(line);
                continue;
            }
            expandedLines.addAll(extractedLines);
        }
        return expandedLines;
    }

    private static boolean looksLikeCondensedFullItemReadLine(String line) {
        if (line == null || line.length() < 80) {
            return false;
        }
        String collapsedLine = collapse(line);
        if (collapsedLine.contains("ZADAJESZOBRAZENIAZWIEKSZONEO") && line.contains("0/08")) {
            return true;
        }
        int anchors = 0;
        for (String anchor : List.of(
                "MOCPRZEDMIOTU",
                "PANCERZA",
                "REDUKCJIBLOKOWANYCHOBRAZEN",
                "SZANSYNABLOK",
                "GLOWNEJRECE",
                "SILY",
                "CIERNI",
                "SZCZESLIWYTRAF",
                "CZASUODNOWIENIA",
                "ZADAJESZOBRAZENIA"
        )) {
            if (collapsedLine.contains(anchor)) {
                anchors++;
            }
        }
        return anchors >= 3;
    }

    private static List<String> extractCondensedFullItemReadLines(String line) {
        List<String> extractedLines = new ArrayList<>();
        appendFirstMatch(extractedLines, line,
                "^\\s*([^*]+?)\\s+\\*\\s+Starożytna\\s+legendarna\\s+tarcza\\b", 1);
        appendFirstMatch(extractedLines, line,
                "\\b(Starożytna\\s+legendarna\\s+tarcza)\\b", 1);
        appendFirstMatch(extractedLines, line,
                "\\b(Moc\\s+przedmiotu:\\s*[0-9]+)\\b", 1);
        appendFirstMatch(extractedLines, line,
                "\\b([0-9][0-9 ]*\\s+pkt\\.\\s+pancerza)\\b", 1);
        appendFirstMatch(extractedLines, line,
                "\\b([0-9]+(?:[,.][0-9]+)?%\\s+redukcji\\s+blokowanych\\s+obrażeń(?:\\s*\\[[0-9,.\\s\\-]*(?:\\]%?)?)?)", 1);
        appendFirstMatch(extractedLines, line,
                "\\b([0-9]+(?:[,.][0-9]+)?%\\s+szansy\\s+na\\s+blok(?:\\s*\\[[0-9,.\\s\\-]*(?:\\]%?)?)?)", 1);
        appendFirstMatch(extractedLines, line,
                "(\\+[0-9]+(?:[,.][0-9]+)?%\\s+obrażeń\\s+od\\s+broni\\s+w\\s+głównej\\s+ręce(?:\\s*\\[[0-9,.\\s\\-]*(?:\\]%?)?)?)", 1);
        appendFirstMatch(extractedLines, line,
                "(\\+[0-9]+(?:[,.][0-9]+)?\\s+(?:do\\s+)?siły(?:\\s*\\[[0-9,.\\s\\-]*(?:\\])?)?)", 1);
        appendFirstMatch(extractedLines, line,
                "(\\+[0-9]+(?:[,.][0-9]+)?\\s+(?:do\\s+)?cierni(?:\\s*\\[[0-9,.\\s\\-]*(?:\\])?)?)", 1);
        appendFirstMatch(extractedLines, line,
                "(\\+[0-9]+(?:[,.][0-9]+)?%\\s+szansy\\s+na\\s+szczęśliwy\\s+traf(?:\\s*\\[[0-9,.\\s\\-]*(?:\\]%?)?)?)", 1);
        appendFirstMatch(extractedLines, line,
                "\\b([0-9]+(?:[,.][0-9]+)?%\\s+redukcji\\s+czasu\\s+odnowienia(?:\\s*\\[[0-9,.\\s\\-]*(?:\\]%?)?)?)", 1);
        appendLegendaryEffectLine(extractedLines, line);
        appendFirstMatch(extractedLines, line,
                "\\b(Ta\\s+premia\\s+jest\\s+trzy\\s+razy\\s+większa,\\s+jeśli\\s+stoisz\\s+w\\s+bezruchu\\s+przez\\s+co\\s+najmniej\\s+3\\s+sek\\.)", 1);
        appendFirstMatch(extractedLines, line,
                "\\b(Puste(?:\\s+gniazdo)?)\\b", 1);
        return extractedLines;
    }

    private static void appendFirstMatch(List<String> target, String line, String regex, int group) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(line);
        if (!matcher.find()) {
            return;
        }
        String value = matcher.group(group).trim();
        if (!value.isBlank() && !target.contains(value)) {
            target.add(value);
        }
    }

    private static void appendLegendaryEffectLine(List<String> target, String line) {
        Matcher exactMatcher = Pattern.compile(
                "\\b(Zadajesz\\s+obrażenia\\s+zwiększone\\s+o\\s+[0-9]+,[0-9]%\\[x\\](?:\\s*\\[[^\\]]+\\]%)?)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ).matcher(line);
        if (exactMatcher.find()) {
            String value = exactMatcher.group(1).trim();
            if (!target.contains(value)) {
                target.add(value);
            }
            return;
        }

        Matcher ocrMatcher = Pattern.compile(
                "\\bZadajesz\\s+obrażenia\\s+zwiększone\\s+o\\s+([0-9]+,[0-9])0/08\\b",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ).matcher(line);
        if (ocrMatcher.find()) {
            String value = "Zadajesz obrażenia zwiększone o " + ocrMatcher.group(1) + "%[x]";
            if (!target.contains(value)) {
                target.add(value);
            }
        }
    }

    private static FullItemReadLineType classifyFullReadLine(String line) {
        String collapsedLine = collapse(line);
        String normalizedLine = normalizeLineForPattern(line);
        if (collapsedLine.contains("MOCPRZEDMIOTU") || collapsedLine.contains("MOCYPRZEDMIOTU") || collapsedLine.contains("ITEMPOWER")) {
            return FullItemReadLineType.ITEM_POWER;
        }
        if (isRarityLine(collapsedLine)) {
            return FullItemReadLineType.RARITY;
        }
        if (isItemTypeLine(collapsedLine)) {
            return FullItemReadLineType.TYPE_OR_SLOT;
        }
        if (containsAny(collapsedLine, List.of("PANCERZ", "ARMOR", "WEAPONDAMAGE", "OBRAZENIABRONI", "DAMAGEPERSECOND"))) {
            return FullItemReadLineType.BASE_STAT;
        }
        if (containsAny(collapsedLine, List.of("REDUKCJIBLOKOWANYCHOBRAZEN", "SZANSYNABLOK", "SZANSANABLOK", "OBRAZENODBRONIWGLOWNEJRECE"))) {
            return FullItemReadLineType.IMPLICIT;
        }
        if (containsAny(collapsedLine, List.of("ASPEKT", "ASPECT", "LEGENDARYPOWER", "ZADAJESZOBRAZENIAZWIEKSZONE", "TAPREMIAJEST"))) {
            return FullItemReadLineType.ASPECT;
        }
        if (containsAny(collapsedLine, List.of("GNIAZDO", "GNIAZDA", "SOCKET", "SOCKETS", "PUSTE"))) {
            return FullItemReadLineType.SOCKET;
        }
        if (containsAny(collapsedLine, List.of("LEGENDARNA"))) {
            return FullItemReadLineType.ASPECT;
        }
        if (normalizedLine.startsWith("+") || normalizedLine.contains("[") || normalizedLine.contains("]") || collectNumericCandidates(normalizedLine).size() > 0) {
            return FullItemReadLineType.AFFIX;
        }
        return FullItemReadLineType.ITEM_NAME;
    }

    private static boolean isItemTypeLine(String collapsedLine) {
        return containsAny(collapsedLine, List.of(
                "MAINHAND", "OFFHAND", "CHEST", "RING", "BOOTS", "BUTY", "BUCIORY", "OBUWIE",
                "SHIELD", "TARCZA", "SWORD", "AXE", "MACE", "HAMMER", "DAGGER", "WEAPON",
                "FOCUS", "ARMOR", "CHESTPLATE", "BREASTPLATE", "BAND"
        ));
    }

    private static boolean isRarityLine(String collapsedLine) {
        return containsAny(collapsedLine, List.of(
                "LEGENDARNY", "LEGENDARNA", "LEGENDARNE", "LEGENDARY",
                "STAROZYTNY", "STAROZYTNA", "STAROZYTNE", "ANCESTRAL",
                "UNIKATOWY", "UNIKATOWA", "UNIQUE",
                "RZADKI", "RZADKA", "RARE",
                "MAGICZNY", "MAGICZNA", "MAGIC"
        ));
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
            Optional<Double> polishLeadingRoll = extractPolishLeadingRoll(normalizedLine, label);
            if (polishLeadingRoll.isPresent()) {
                return field(line, polishLeadingRoll.get(), ItemImportFieldConfidence.HIGH,
                        label + " rozpoznany bezpośrednio z tekstu OCR.");
            }
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

    private static Optional<Double> extractPolishLeadingRoll(String normalizedLine, String label) {
        String pattern = switch (label) {
            case "Strength" -> "\\b([0-9OISBL]+(?:[.,][0-9OISBL]+)?)\\s+(?:DO\\s+)?SIL[AY]?\\b";
            case "Thorns" -> "\\b([0-9OISBL]+(?:[.,][0-9OISBL]+)?)\\s+(?:DO\\s+)?CIERN\\w*\\b";
            case "Block chance" -> "\\b([0-9OISBL]+(?:[.,][0-9OISBL]+)?)\\s*%?\\s+SZANS[AY]\\s+NA\\s+BLOK\\b";
            default -> "";
        };
        if (pattern.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile(pattern).matcher(normalizedLine);
        while (matcher.find()) {
            int tokenStart = matcher.start(1);
            int tokenEnd = matcher.end(1);
            if (isInsideReferenceRange(normalizedLine, tokenStart) || isBaseItemValue(normalizedLine, tokenStart, tokenEnd)) {
                continue;
            }
            return parseNumericToken(matcher.group(1));
        }
        return Optional.empty();
    }

    private static Optional<Double> extractNumberNearPhrase(String normalizedLine, String phraseRegex) {
        List<NumericTokenCandidate> numericCandidates = collectNumericCandidates(normalizedLine);
        if (numericCandidates.isEmpty()) {
            return Optional.empty();
        }

        Matcher phraseMatcher = Pattern.compile(phraseRegex).matcher(normalizedLine);
        NumericTokenCandidate bestCandidate = null;
        int bestDistance = Integer.MAX_VALUE;
        while (phraseMatcher.find()) {
            NumericTokenCandidate candidate = selectBestCandidateForPhrase(
                    numericCandidates,
                    phraseMatcher.start(),
                    phraseMatcher.end()
            );
            if (candidate == null) {
                continue;
            }
            int distance = distanceToPhrase(candidate, phraseMatcher.start(), phraseMatcher.end());
            if (bestCandidate == null || distance < bestDistance) {
                bestCandidate = candidate;
                bestDistance = distance;
            }
        }

        return bestCandidate == null ? Optional.empty() : parseNumericToken(bestCandidate.rawToken());
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

    private static List<NumericTokenCandidate> collectNumericCandidates(String normalizedLine) {
        List<NumericTokenCandidate> candidates = new ArrayList<>();
        Matcher matcher = OCR_NUMBER_PATTERN.matcher(normalizedLine);
        while (matcher.find()) {
            String rawToken = matcher.group(1);
            if (!containsDecimalDigit(rawToken)) {
                continue;
            }
            candidates.add(new NumericTokenCandidate(
                    rawToken,
                    matcher.start(1),
                    matcher.end(1),
                    isInsideReferenceRange(normalizedLine, matcher.start(1)),
                    isBaseItemValue(normalizedLine, matcher.start(1), matcher.end(1))
            ));
        }
        return candidates;
    }

    private static NumericTokenCandidate selectBestCandidateForPhrase(List<NumericTokenCandidate> numericCandidates,
                                                                      int phraseStart,
                                                                      int phraseEnd) {
        List<NumericTokenCandidate> safeCandidates = numericCandidates.stream()
                .filter(candidate -> !candidate.insideReferenceRange())
                .filter(candidate -> !candidate.baseItemValue())
                .filter(candidate -> !overlapsPhrase(candidate, phraseStart, phraseEnd))
                .toList();

        return safeCandidates.stream()
                .min(Comparator
                        .comparingInt((NumericTokenCandidate candidate) -> distanceToPhrase(candidate, phraseStart, phraseEnd))
                        .thenComparingInt(candidate -> candidate.start() >= phraseEnd ? 0 : 1)
                        .thenComparingInt(candidate -> candidate.start() >= phraseEnd
                                ? candidate.start() - phraseEnd
                                : phraseStart - candidate.end())
                        .thenComparingInt(NumericTokenCandidate::start))
                .orElse(null);
    }

    private static int distanceToPhrase(NumericTokenCandidate candidate, int phraseStart, int phraseEnd) {
        if (candidate.end() <= phraseStart) {
            return phraseStart - candidate.end();
        }
        if (candidate.start() >= phraseEnd) {
            return candidate.start() - phraseEnd;
        }
        return 0;
    }

    private static boolean isInsideReferenceRange(String normalizedLine, int tokenStart) {
        int squareOpen = normalizedLine.lastIndexOf('[', tokenStart);
        int squareClose = normalizedLine.lastIndexOf(']', tokenStart);
        if (squareOpen >= 0 && squareOpen > squareClose) {
            return isStillInsideBrokenOcrRange(normalizedLine, squareOpen, tokenStart);
        }

        int roundOpen = normalizedLine.lastIndexOf('(', tokenStart);
        int roundClose = normalizedLine.lastIndexOf(')', tokenStart);
        return roundOpen >= 0 && roundOpen > roundClose
                && isStillInsideBrokenOcrRange(normalizedLine, roundOpen, tokenStart);
    }

    private static boolean isStillInsideBrokenOcrRange(String normalizedLine, int rangeOpen, int tokenStart) {
        String textBetweenOpenAndToken = normalizedLine.substring(rangeOpen + 1, tokenStart);
        if (textBetweenOpenAndToken.length() > 20) {
            return false;
        }
        if (textBetweenOpenAndToken.contains("%")) {
            return false;
        }
        for (int index = 0; index < textBetweenOpenAndToken.length(); index++) {
            if (Character.isLetter(textBetweenOpenAndToken.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBaseItemValue(String normalizedLine, int tokenStart, int tokenEnd) {
        Matcher armorMatcher = Pattern.compile("\\b(?:PANCERZ\\w*|ARMOR)\\b").matcher(normalizedLine);
        while (armorMatcher.find()) {
            if (armorMatcher.start() < tokenEnd) {
                continue;
            }
            String textBetweenTokenAndArmor = normalizedLine.substring(tokenEnd, armorMatcher.start());
            if (textBetweenTokenAndArmor.matches("[\\s.,:+\\-]*(?:[0-9OISBL]+[\\s.,:+\\-]*)*(?:(?:PKT|PTS?|POINTS?)\\.?[\\s.,:+\\-]*)?")) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlapsPhrase(NumericTokenCandidate candidate, int phraseStart, int phraseEnd) {
        return candidate.start() < phraseEnd && candidate.end() > phraseStart;
    }

    private static boolean containsDecimalDigit(String rawToken) {
        for (int index = 0; index < rawToken.length(); index++) {
            if (Character.isDigit(rawToken.charAt(index))) {
                return true;
            }
        }
        return false;
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
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizePolishText(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "");
    }

    private record NumericTokenCandidate(String rawToken,
                                         int start,
                                         int end,
                                         boolean insideReferenceRange,
                                         boolean baseItemValue) {
    }
}
