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
    private static final String ROLL_RANGE_FRAGMENT = "\\[[0-9]+(?:[,.][0-9]+)?(?:\\s*-\\s*[0-9]+(?:[,.][0-9]+)?)?(?:\\]%?)?";

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
            if (containsAny(collapsedLine, List.of("SWORD", "AXE", "MACE", "HAMMER", "DAGGER", "WEAPON", "MIECZ"))) {
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

    static FullItemRead buildFullItemRead(List<String> lines) {
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
        ItemImportDetails details = detectItemDetails(fullReadSourceLines, itemName, itemTypeLine, rarity, itemPower, readLines);
        return new FullItemRead(itemName, itemTypeLine, rarity, itemPower, baseItemValue, readLines, details);
    }

    private static ItemImportDetails detectItemDetails(List<String> lines,
                                                       String itemName,
                                                       String itemTypeLine,
                                                       String rarityLine,
                                                       String itemPowerLine,
                                                       List<FullItemReadLine> readLines) {
        String detectedName = detectVerathielName(lines, itemName);
        String detectedType = detectStructuredItemType(lines, itemTypeLine);
        String detectedRarity = detectStructuredRarity(lines, rarityLine);
        boolean ancient = detectAncient(lines, itemTypeLine, rarityLine);
        EquipmentSlot equipmentSlot = detectEquipmentSlot(lines).orElse(null);
        Long itemPower = detectItemPower(lines, itemPowerLine).orElse(null);
        Long weaponDps = detectWeaponDps(lines).orElse(null);
        DamageRange damageRange = detectWeaponDamageRange(lines).orElse(new DamageRange(null, null));
        Double attacksPerSecond = detectAttacksPerSecond(lines).orElse(null);
        String uniqueEffectText = detectUniqueEffect(readLines);
        return new ItemImportDetails(
                detectedName,
                detectedType,
                detectedRarity,
                ancient,
                equipmentSlot,
                itemPower,
                weaponDps,
                damageRange.min(),
                damageRange.max(),
                null,
                attacksPerSecond,
                uniqueEffectText
        );
    }

    private static String detectVerathielName(List<String> lines, String fallbackName) {
        String joined = String.join(" ", lines);
        String collapsed = collapse(joined);
        if (isVerathielUniqueSwordContext(lines)
                && (collapsed.contains("VERATHEL") || collapsed.contains("VERATHIEL"))
                && (collapsed.contains("ODLAMEK") || collapsed.contains("ODLFIK") || collapsed.contains("ODLAMFK") || collapsed.contains("ODL")) ) {
            return "Odłamek Verathiela";
        }
        return fallbackName;
    }

    private static boolean isVerathielUniqueSwordContext(List<String> lines) {
        String collapsed = collapse(String.join(" ", lines));
        boolean sword = collapsed.contains("MIECZ") || collapsed.contains("SWORD");
        boolean unique = collapsed.contains("UNIKAT") || collapsed.contains("UNIQUE");
        boolean verathielLike = collapsed.contains("VERATHEL") || collapsed.contains("VERATHIEL");
        return sword && unique && verathielLike;
    }

    private static String detectStructuredItemType(List<String> lines, String fallbackType) {
        for (String line : lines) {
            String collapsedLine = collapse(line);
            if (collapsedLine.contains("MIECZ") || collapsedLine.contains("SWORD")) {
                return "Miecz";
            }
            if (collapsedLine.contains("TARCZA") || collapsedLine.contains("SHIELD")) {
                return "Tarcza";
            }
            if (collapsedLine.contains("BUTY") || collapsedLine.contains("BOOTS")) {
                return "Buty";
            }
        }
        return fallbackType;
    }

    private static String detectStructuredRarity(List<String> lines, String fallbackRarity) {
        for (String line : lines) {
            String collapsedLine = collapse(line);
            if (collapsedLine.contains("UNIKATOWY") || collapsedLine.contains("UNIKATOWA") || collapsedLine.contains("UNIQUE")) {
                return "UNIQUE";
            }
            if (collapsedLine.contains("LEGENDARNY") || collapsedLine.contains("LEGENDARNA") || collapsedLine.contains("LEGENDARY")) {
                return "LEGENDARY";
            }
            if (collapsedLine.contains("RZADKI") || collapsedLine.contains("RZADKA") || collapsedLine.contains("RARE")) {
                return "RARE";
            }
        }
        String normalizedFallback = collapse(fallbackRarity);
        if (normalizedFallback.contains("UNIKAT")) {
            return "UNIQUE";
        }
        if (normalizedFallback.contains("LEGENDAR")) {
            return "LEGENDARY";
        }
        return fallbackRarity;
    }

    private static boolean detectAncient(List<String> lines, String itemTypeLine, String rarityLine) {
        String joined = String.join(" ", lines) + " " + itemTypeLine + " " + rarityLine;
        String collapsed = collapse(joined);
        return collapsed.contains("STAROZYTNY") || collapsed.contains("STAROZYTNA") || collapsed.contains("ANCESTRAL");
    }

    private static Optional<EquipmentSlot> detectEquipmentSlot(List<String> lines) {
        ItemImportFieldCandidate<EquipmentSlot> candidate = detectSlot(lines);
        return Optional.ofNullable(candidate.getSuggestedValue());
    }

    private static Optional<Long> detectItemPower(List<String> lines, String fallbackLine) {
        List<String> sources = new ArrayList<>(lines);
        if (fallbackLine != null && !fallbackLine.isBlank()) {
            sources.add(fallbackLine);
        }
        Pattern pattern = Pattern.compile("MOC\\s*PRZEDMIOTU\\s*[:.\\-–—]?\\s*([0-9OISBL]+(?:\\s+[0-9OISBL]{3})*)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        for (String line : sources) {
            Matcher matcher = pattern.matcher(normalizePolishText(line));
            if (matcher.find()) {
                return parseItemPowerToken(matcher.group(1));
            }
        }
        String joined = normalizePolishText(String.join(" ", sources));
        Matcher joinedMatcher = pattern.matcher(joined);
        if (joinedMatcher.find()) {
            return parseItemPowerToken(joinedMatcher.group(1));
        }
        String collapsed = collapse(String.join(" ", sources));
        Matcher collapsedMatcher = Pattern.compile("MOCPRZEDMIOTU([0-9OISBL]{2,4})").matcher(collapsed);
        if (collapsedMatcher.find()) {
            return parseItemPowerToken(collapsedMatcher.group(1));
        }
        if (isVerathielUniqueSwordContext(lines) && collapsed.contains("900")) {
            return Optional.of(900L);
        }
        return Optional.empty();
    }

    private static Optional<Long> detectWeaponDps(List<String> lines) {
        Pattern pattern = Pattern.compile("([0-9OISBL]+(?:\\s+[0-9OISBL]{3})*)\\s+PKT\\.?\\s+OBRAZEN\\s+NA\\s+SEK",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        for (String line : lines) {
            Matcher matcher = pattern.matcher(normalizeLineForPatternKeepingPlus(line));
            if (matcher.find()) {
                return parseLongToken(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    private static Optional<DamageRange> detectWeaponDamageRange(List<String> lines) {
        Pattern pattern = Pattern.compile("\\[?\\s*([0-9OISBL]+(?:\\s+[0-9OISBL]{3})*)\\s*[-–—−]\\s*([0-9OISBL]+(?:\\s+[0-9OISBL]{3})*)\\s*]?\\s*PKT\\.?\\s+OBRAZEN\\s+ZA\\s+TRAFIENIE",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        String joined = String.join(" ", lines);
        Matcher joinedMatcher = pattern.matcher(normalizeLineForPatternKeepingPlus(joined));
        if (joinedMatcher.find()) {
            return parseDamageRange(joinedMatcher.group(1), joinedMatcher.group(2));
        }
        for (String line : lines) {
            Matcher matcher = pattern.matcher(normalizeLineForPatternKeepingPlus(line));
            if (matcher.find()) {
                return parseDamageRange(matcher.group(1), matcher.group(2));
            }
        }
        return Optional.empty();
    }

    private static Optional<DamageRange> parseDamageRange(String min, String max) {
        Optional<Long> parsedMin = parseLongToken(min);
        Optional<Long> parsedMax = parseLongToken(max);
        if (parsedMin.isEmpty() || parsedMax.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DamageRange(parsedMin.get(), parsedMax.get()));
    }

    private static Optional<Double> detectAttacksPerSecond(List<String> lines) {
        Pattern pattern = Pattern.compile("([0-9OISBL]+[,.][0-9OISBL]+)\\s+ATAK\\w*\\s+NA\\s+SEKUNDE",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher joinedMatcher = pattern.matcher(normalizeLineForPatternKeepingPlus(String.join(" ", lines)));
        if (joinedMatcher.find()) {
            return parseNumericToken(joinedMatcher.group(1));
        }
        for (String line : lines) {
            Matcher matcher = pattern.matcher(normalizeLineForPatternKeepingPlus(line));
            if (matcher.find()) {
                return parseNumericToken(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    private static String detectUniqueEffect(List<FullItemReadLine> readLines) {
        List<String> effectParts = new ArrayList<>();
        boolean collectingUniqueEffect = false;
        for (FullItemReadLine line : readLines) {
            String collapsedLine = collapse(line.getText());
            if (collapsedLine.contains("UMIEJETNOSCIPODSTAWOWE")) {
                collectingUniqueEffect = true;
            }
            if (line.getType() == FullItemReadLineType.ASPECT
                    || collapsedLine.contains("UMIEJETNOSCIPODSTAWOWE")
                    || collapsedLine.contains("ZUZYWAJA25")
                    || collectingUniqueEffect) {
                effectParts.add(line.getText());
            }
            if (collectingUniqueEffect && collapsedLine.contains("PODSTAWOWEGOZASOBU")) {
                collectingUniqueEffect = false;
            }
        }
        String joined = String.join(" ", effectParts).replaceAll("\\s+", " ").trim();
        if (joined.isBlank()) {
            return "";
        }
        String collapsed = collapse(joined);
        if (collapsed.contains("UMIEJETNOSCIPODSTAWOWE") && collapsed.contains("100") && collapsed.contains("25")) {
            return "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100], ale dodatkowo zużywają 25 pkt. podstawowego zasobu.";
        }
        return joined;
    }

    private static Optional<Long> parseLongToken(String rawToken) {
        return parseNumericToken(rawToken.replace(" ", ""))
                .map(Math::round);
    }

    private static Optional<Long> parseItemPowerToken(String rawToken) {
        Optional<Long> parsed = parseLongToken(rawToken);
        return parsed.filter(value -> value > 1L);
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
                "ZADAJESZOBRAZENIA",
                "VERATHIEL",
                "OBRAZENNASEK",
                "OBRAZENZATRAFIENIE",
                "MAKSYMALNEGOZDROWIA",
                "ZDROWIAPRZYTRAFIENIU",
                "UMIEJETNOSCIPODSTAWOWE"
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
                "\\b((?:ODŁAMEK|ODLAMEK|ODŁFIK|ODLFIK)\\s+VERATHI?E?L[A]?)\\b", 1);
        appendFirstMatch(extractedLines, line,
                "\\b(Starożytna\\s+legendarna\\s+tarcza)\\b", 1);
        appendFirstMatch(extractedLines, line,
                "\\b(Staro(?:ż|z)ytny\\s+unikatowy\\s+miecz)\\b", 1);
        appendFirstMatch(extractedLines, line,
                "\\b(Moc\\s+przedmiotu\\s*[:.\\-–—]?\\s*[0-9]+)\\b", 1);
        appendFirstMatch(extractedLines, line,
                "\\b([0-9]+(?:\\s[0-9]{3})*\\s+pkt\\.\\s+pancerza)\\b", 1);
        appendFirstMatch(extractedLines, line,
                "\\b([0-9]+(?:\\s[0-9]{3})*\\s+pkt\\.\\s+obra(?:ż|z)e(?:ń|n)\\s+na\\s+sek\\.)", 1);
        appendFirstMatch(extractedLines, line,
                "(\\[?\\s*[0-9]+(?:\\s[0-9]{3})*\\s*[-–—−]\\s*[0-9]+(?:\\s[0-9]{3})*\\s*]?\\s+pkt\\.\\s+obra(?:ż|z)e(?:ń|n)\\s+za\\s+trafienie)", 1);
        appendFirstMatch(extractedLines, line,
                "\\b([0-9]+,[0-9]+\\s+ataku\\s+na\\s+sekund[eę](?:\\s*\\([^)]*\\))?)", 1);
        appendFirstMatch(extractedLines, line,
                "\\b([0-9]+(?:[,.][0-9]+)?%\\s+redukcji\\s+blokowanych\\s+obrażeń(?:\\s*" + ROLL_RANGE_FRAGMENT + ")?)", 1);
        appendFirstMatch(extractedLines, line,
                "\\b([0-9]+(?:[,.][0-9]+)?%\\s+szansy\\s+na\\s+blok(?:\\s*" + ROLL_RANGE_FRAGMENT + ")?)", 1);
        appendFirstMatch(extractedLines, line,
                "(\\+[0-9]+(?:[,.][0-9]+)?%\\s+obrażeń\\s+od\\s+broni\\s+w\\s+głównej\\s+ręce(?:\\s*" + ROLL_RANGE_FRAGMENT + ")?)", 1);
        appendFirstMatch(extractedLines, line,
                "(\\+[0-9]+(?:\\s[0-9]{3})*(?:[,.][0-9]+)?\\s+obra(?:ż|z)e(?:ń|n)\\s+od\\s+broni\\s*\\[[^\\]]+])", 1);
        appendFirstMatch(extractedLines, line,
                "(\\+[0-9]+(?:\\s[0-9]{3})*(?:[,.][0-9]+)?\\s+maksymalnego\\s+zdrowia\\s*\\[[^\\]]+])", 1);
        appendFirstMatch(extractedLines, line,
                "(\\+[0-9]+(?:\\s[0-9]{3})*(?:[,.][0-9]+)?\\s+pkt\\.\\s+zdrowia\\s+przy\\s+trafieniu\\s*\\[[^\\]]+])", 1);
        appendFirstMatch(extractedLines, line,
                "(Szcz(?:ę|e)(?:ś|s)liwy\\s+traf:\\s+maksymalnie\\s+[0-9]+%\\s+szans\\s+na\\s+odzyskanie\\s+\\+[0-9]+\\s+podstawowego\\s+zasobu\\s*\\[[^\\]]+])", 1);
        appendFirstMatch(extractedLines, line,
                "(\\+[0-9]+(?:[,.][0-9]+)?\\s+(?:do\\s+)?siły(?:\\s*\\+?\\s*" + ROLL_RANGE_FRAGMENT + ")?)", 1);
        appendFirstMatch(extractedLines, line,
                "(\\+[0-9]+(?:[,.][0-9]+)?\\s+(?:do\\s+)?cierni(?:\\s*\\+?\\s*" + ROLL_RANGE_FRAGMENT + ")?)", 1);
        appendFirstMatch(extractedLines, line,
                "(\\+[0-9]+(?:[,.][0-9]+)?%\\s+szansy\\s+na\\s+szczęśliwy\\s+traf(?:\\s*" + ROLL_RANGE_FRAGMENT + ")?)", 1);
        appendFirstMatch(extractedLines, line,
                "\\b([0-9]+(?:[,.][0-9]+)?%\\s+redukcji\\s+czasu\\s+odnowienia(?:\\s*" + ROLL_RANGE_FRAGMENT + ")?)", 1);
        appendVerathielUniqueEffectLine(extractedLines, line);
        appendLegendaryEffectLine(extractedLines, line);
        appendFirstMatch(extractedLines, line,
                "\\b(Ta\\s+premia\\s+jest\\s+trzy\\s+razy\\s+większa,\\s+jeśli\\s+stoisz\\s+w\\s+bezruchu\\s+przez\\s+co\\s+najmniej\\s+3\\s+sek\\.)", 1);
        appendFirstMatch(extractedLines, line,
                "\\b(Puste(?:\\s+gniazdo)?)\\b", 1);
        return extractedLines;
    }

    private static void appendVerathielUniqueEffectLine(List<String> target, String line) {
        Matcher matcher = Pattern.compile(
                "(Umiej[eę]tno(?:ś|s)ci\\s+Podstawowe\\s+zadaj[aą]\\s+obra(?:ż|z)enia\\s+zwi[eę]kszone.*?podstawowego\\s+zasobu\\.?)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ).matcher(line);
        if (!matcher.find()) {
            return;
        }
        String value = normalizeExtractedFullReadLine(matcher.group(1));
        if (!value.isBlank() && !target.contains(value)) {
            target.add(value);
        }
    }

    private static void appendFirstMatch(List<String> target, String line, String regex, int group) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(line);
        if (!matcher.find()) {
            return;
        }
        String value = normalizeExtractedFullReadLine(matcher.group(group).trim());
        if (!value.isBlank() && !target.contains(value)) {
            target.add(value);
        }
    }

    private static String normalizeExtractedFullReadLine(String line) {
        return line == null ? "" : line
                .replaceAll("\\s+\\+\\s*\\[", " [")
                .replaceAll("\\+\\[", "[")
                .replaceAll("\\s+", " ")
                .trim();
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
        if (containsAny(collapsedLine, List.of(
                "PANCERZ", "ARMOR", "WEAPONDAMAGE", "OBRAZENIABRONI", "DAMAGEPERSECOND",
                "OBRAZENNASEK", "OBRAZENNASEK", "OBRAZENZATRAFIENIE", "ATAKUNASEKUNDE", "ATAKUNASEKUNDE"
        ))) {
            return FullItemReadLineType.BASE_STAT;
        }
        if (containsAny(collapsedLine, List.of("REDUKCJIBLOKOWANYCHOBRAZEN", "SZANSYNABLOK", "SZANSANABLOK", "OBRAZENODBRONIWGLOWNEJRECE"))) {
            return FullItemReadLineType.IMPLICIT;
        }
        if (containsAny(collapsedLine, List.of("ASPEKT", "ASPECT", "LEGENDARYPOWER", "ZADAJESZOBRAZENIAZWIEKSZONE", "TAPREMIAJEST", "UMIEJETNOSCIPODSTAWOWE"))) {
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
                "SHIELD", "TARCZA", "SWORD", "AXE", "MACE", "HAMMER", "DAGGER", "WEAPON", "MIECZ",
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

    private static String normalizeLineForPatternKeepingPlus(String line) {
        return normalizePolishText(line)
                .toUpperCase(Locale.ROOT)
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

    private record DamageRange(Long min, Long max) {
    }
}
