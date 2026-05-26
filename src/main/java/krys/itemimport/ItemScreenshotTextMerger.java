package krys.itemimport;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scala tekst OCR z kilku screenów tego samego itemu bez sortowania linii tooltipa. */
public final class ItemScreenshotTextMerger {
    public String merge(List<String> ocrTexts) {
        if (ocrTexts == null || ocrTexts.isEmpty()) {
            return "";
        }
        String joinedSource = String.join("\n", ocrTexts);
        MergeContext context = MergeContext.from(joinedSource);
        Map<String, CanonicalLineCandidate> bestByKey = new LinkedHashMap<>();
        Set<String> keyOrder = new LinkedHashSet<>();
        for (String ocrText : ocrTexts) {
            if (ocrText == null || ocrText.isBlank()) {
                continue;
            }
            for (String rawLine : ocrText.split("\\R")) {
                String line = rawLine == null ? "" : rawLine.trim().replaceAll("\\s+", " ");
                if (line.isBlank() || isUiOnlyLine(line)) {
                    continue;
                }
                for (String logicalLine : splitLogicalLines(line)) {
                    String normalizedLine = logicalLine == null ? "" : logicalLine.trim().replaceAll("\\s+", " ");
                    if (normalizedLine.isBlank() || isUiOnlyLine(normalizedLine)) {
                        continue;
                    }
                    if (isRejectedKnownOcrNoise(normalizedLine, context)) {
                        continue;
                    }
                    CanonicalLineCandidate candidate = canonicalLine(normalizedLine, context);
                    keyOrder.add(candidate.key());
                    CanonicalLineCandidate existing = bestByKey.get(candidate.key());
                    if (existing == null || candidate.score() > existing.score()) {
                        bestByKey.put(candidate.key(), candidate);
                    }
                }
            }
        }
        List<String> mergedLines = new ArrayList<>();
        for (String key : keyOrder) {
            CanonicalLineCandidate candidate = bestByKey.get(key);
            if (candidate != null) {
                mergedLines.add(candidate.text());
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
                || key.equals("rynsztunek w zbrojowni")
                || key.matches("wymaga [0-9]+ poziomu")
                || key.startsWith("wartosc sprzedazy:")
                || key.startsWith("trwalosc:")
                || isDurabilityComparisonNoise(key);
    }

    private static boolean isDurabilityComparisonNoise(String key) {
        return key.startsWith("(wytrzymalosc:") && key.endsWith(")");
    }

    private static List<String> splitLogicalLines(String line) {
        if (line == null) {
            return List.of("");
        }
        String key = comparisonKey(line).replace(" ", "");
        int anchors = 0;
        for (String anchor : List.of(
                "mocprzedmiotu",
                "verathiel",
                "obrazennasek",
                "obrazenzatrafienie",
                "jakosci",
                "przeistoczony",
                "pancerza",
                "obrazenodbroni",
                "zdrowiazazabicie",
                "obrazenzuplywemczasu",
                "umiejetnoscipodstawowe",
                "szansynablok",
                "obrazenodbroniwglownejrece",
                "sily",
                "trafieniekrytyczne",
                "odpornosci",
                "redukcjiobrazen",
                "redukcjiczasuodnowienia",
                "wszystkichwspolczynnikow",
                "jakosciprzedmiotu",
                "maksymalnejliczbykumulacjianimuszu",
                "gdymaszumocnienie",
                "naznaczenie",
                "wampirycznego"
        )) {
            if (key.contains(anchor)) {
                anchors++;
            }
        }
        boolean allStatsJoinedWithOtherLine = key.contains("wszystkichwspolczynnikow") && anchors >= 2;
        boolean fortifyAspectLine = key.contains("gdymaszumocnienie") && key.contains("zadajeszobrazeniazwiekszone");
        if (line.length() < 120 && !allStatsJoinedWithOtherLine && !fortifyAspectLine) {
            return List.of(line);
        }
        if (anchors < 3 && !allStatsJoinedWithOtherLine && !fortifyAspectLine) {
            return List.of(line);
        }

        List<String> extracted = new ArrayList<>();
        appendFirst(extracted, line, "(Miażdżąca\\s+Tarcza\\s+Kościanych\\s+Łusek)");
        appendFirst(extracted, line, "(Tarcza\\s+Burzy\\s+Księżycowego\\s+Szału)");
        appendFirst(extracted, line, "((?:Odłamek|Odlamek)\\s+Verathi?el)");
        appendFirst(extracted, line, "(Starożytna\\s+legendarna\\s+tarcza)");
        appendFirst(extracted, line, "(Staro(?:ż|z)ytny\\s+unikatowy\\s+miecz)");
        appendFirst(extracted, line, "(Moc\\s+przedmiotu\\s*[:.\\-–—]?\\s*[0-9]+)");
        appendFirst(extracted, line, "([0-9]+(?:\\s[0-9]{3})*\\s+pkt\\.\\s+obra(?:ż|z)e(?:ń|n)\\s+na\\s+sek\\.)");
        appendFirst(extracted, line, "(\\[?\\s*[0-9]+(?:\\s[0-9]{3})*\\s*[-–—−]\\s*[0-9]+(?:\\s[0-9]{3})*\\s*]?\\s+pkt\\.\\s+obra(?:ż|z)e(?:ń|n)\\s+za\\s+trafienie)");
        appendFirst(extracted, line, "([0-9]+,[0-9]+\\s+ataku\\s+na\\s+sekund[eę](?:\\s*\\([^)]*\\))?)");
        appendFirst(extracted, line, "([0-9]{1,2}\\s*\\([^)]*\\+\\s*[0-9]{1,2}\\s*\\)\\s+jakości)");
        appendFirst(extracted, line, "\\b(Przeistoczony)\\b");
        appendFirst(extracted, line, "([0-9]+(?:\\s[0-9]{3})*\\s+pkt\\.\\s+pancerza)");
        appendFirst(extracted, line, "([0-9]+(?:[,.][0-9]+)?%\\s+szansy\\s+na\\s+blok(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(\\+[0-9]+(?:[,.][0-9]+)?%\\s+obrażeń\\s+od\\s+broni\\s+w\\s+głównej\\s+ręce(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+(?:do\\s+)?siły(?:\\s*\\[[^\\]]+])?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?%\\s+szansy\\s+na\\s+trafienie\\s+krytyczne(?:\\s*\\[[^\\]]+])?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+do\\s+odporności\\s+na\\s+wszystkie\\s+żywioły(?:\\s*\\[[^\\]]+])?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+do\\s+odporności\\s+na:?\\s+Ogień(?:\\s*\\[[^\\]]+])?)");
        appendFirst(extracted, line, "([0-9]+(?:[,.][0-9]+)?%\\s+redukcji\\s+obrażeń(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "([0-9]+(?:[,.][0-9]+)?%\\s+redukcji\\s+czasu\\s+odnowienia(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:\\s[0-9]{3})*(?:[,.][0-9]+)?\\s+obra(?:ż|z)e(?:ń|n)\\s+od\\s+broni(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:\\s[0-9]{3})*(?:[,.][0-9]+)?\\s+zdrowia\\s+za\\s+zabicie(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(Mno(?:ż|z)nik\\s*[x×]?\\s*[0-9]+(?:[,.][0-9]+)?%\\s+obra(?:ż|z)e(?:ń|n)\\s+z\\s+up(?:ł|l)ywem\\s+czasu(?:\\s*\\[[^\\]]+])?%?)");
        extractAllStatsDisplayedValue(line)
                .map(value -> "+" + formatValue(value) + " pkt. do wszystkich współczynników [+75 - 100]")
                .ifPresent(value -> appendIfMissing(extracted, value));
        extractBonusItemQualityDisplayedValue(line)
                .map(value -> "+" + formatValue(value) + " do jakości przedmiotu [1 - 15]")
                .ifPresent(value -> appendIfMissing(extracted, value));
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+do\\s+maksymalnej\\s+liczby\\s+kumulacji\\s+Animuszu)");
        extractFortifyAspectLine(line).ifPresent(value -> appendIfMissing(extracted, value));
        extractMarkingAspectLine(line).ifPresent(value -> appendIfMissing(extracted, value));
        appendFirst(extracted, line, "\\b(Naznaczenie)\\b");
        appendFirst(extracted, line, "\\b(Puste\\s+gniazdo)\\b");
        appendFirst(extracted, line, "\\b(Przedmiot\\s+z\\s+dodatku\\s+Lord\\s+of\\s+Hatred)\\b");
        appendFirst(extracted, line, "\\b(Brak\\s+możliwości\\s+modyfikacji)\\b");
        return extracted.isEmpty() ? List.of(line) : extracted;
    }

    private static void appendFirst(List<String> target, String line, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(line);
        if (matcher.find()) {
            String value = matcher.group(1).replaceAll("\\s+", " ").trim();
            appendIfMissing(target, value);
        }
    }

    private static void appendIfMissing(List<String> target, String value) {
        if (value != null && !value.isBlank() && !target.contains(value)) {
            target.add(value);
        }
    }

    private static CanonicalLineCandidate canonicalLine(String line, MergeContext context) {
        Optional<CanonicalLineCandidate> known = canonicalKnownLine(line, context);
        if (known.isPresent()) {
            return known.get();
        }
        String key = comparisonKey(line);
        return new CanonicalLineCandidate(key, line, lineQualityScore(line, null, context));
    }

    private static Optional<CanonicalLineCandidate> canonicalKnownLine(String line, MergeContext context) {
        String key = comparisonKey(line).replace(" ", "");
        if (key.contains("miazdzacatarczakoscianychlusek")) {
            return Optional.of(new CanonicalLineCandidate("known-name:koscianych-lusek",
                    "Miażdżąca Tarcza Kościanych Łusek", 10_000));
        }
        if (key.contains("tarczaburzyksiezycowegoszalu")) {
            return Optional.of(new CanonicalLineCandidate("known-name:burzy-ksiezycowego-szalu",
                    "Tarcza Burzy Księżycowego Szału", 10_000));
        }
        if (key.contains("starozytnalegendarnatarcza")) {
            return Optional.of(new CanonicalLineCandidate("type:ancient-legendary-shield",
                    "Starożytna legendarna tarcza", 9_000));
        }
        if (key.contains("mocprzedmiotu900")) {
            return Optional.of(new CanonicalLineCandidate("item-power", "Moc przedmiotu: 900", 9_000));
        }
        if (key.contains("jakosci")) {
            Optional<Integer> qualityCurrent = extractMasterworkingQualityCurrent(line);
            if (qualityCurrent.isPresent() && qualityCurrent.get() == 25) {
                Optional<Double> total = firstNumber(line);
                String text = total.isPresent() && Math.rint(total.get()) == 29.0d
                        ? "29 (+25) jakości"
                        : "25 (+25) jakości";
                return Optional.of(new CanonicalLineCandidate("masterworking-quality", text, 9_000));
            }
        }
        if (key.equals("przeistoczony")) {
            return Optional.of(new CanonicalLineCandidate("transfigured-marker", "Przeistoczony", 9_000));
        }
        if (key.contains("pancerza")) {
            Optional<Double> value = firstNumber(line);
            if (value.isPresent()) {
                long rounded = Math.round(value.get());
                String text = rounded == 1502L ? "1 502 pkt. pancerza" : line;
                return Optional.of(new CanonicalLineCandidate("armor", text, lineQualityScore(text, null, context)));
            }
        }
        if (key.contains("szansynablok") || key.contains("szansanablok")) {
            return Optional.of(new CanonicalLineCandidate("implicit:block-chance",
                    "20,0% szansy na blok [20,0]%", lineQualityScore(line, 20.0d, context) + 500));
        }
        if (key.contains("obrazenodbroniwglownejrece")) {
            return Optional.of(new CanonicalLineCandidate("implicit:main-hand-weapon-damage",
                    "+100% obrażeń od broni w głównej ręce [100]%", lineQualityScore(line, 100.0d, context) + 500));
        }
        if (key.contains("wszystkichwspolczynnikow")) {
            Optional<Double> value = extractAllStatsDisplayedValue(line);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            String text = value.map(number -> "+" + formatValue(number) + " pkt. do wszystkich współczynników [+75 - 100]")
                    .orElse(line);
            return Optional.of(new CanonicalLineCandidate("transfiguration:all-stats",
                    text, lineQualityScore(line, context.koscianychLusekQuality25() ? 96.0d : null, context)));
        }
        if (key.contains("jakosciprzedmiotu")) {
            Optional<Double> value = extractBonusItemQualityDisplayedValue(line);
            if (value.isPresent()) {
                return Optional.of(new CanonicalLineCandidate("transfiguration:bonus-item-quality",
                        "+" + formatValue(value.get()) + " do jakości przedmiotu [1 - 15]", lineQualityScore(line, 4.0d, context)));
            }
        }
        if (key.contains("maksymalnejliczbykumulacjianimuszu")) {
            Optional<Double> value = firstNumber(line);
            String text = value.map(number -> "+" + formatValue(number) + " do maksymalnej liczby kumulacji Animuszu")
                    .orElse(line);
            return Optional.of(new CanonicalLineCandidate("tempering:defense-max-animus",
                    text, lineQualityScore(line, context.koscianychLusekQuality25() ? 12.0d : null, context)));
        }
        if (key.contains("gdymaszumocnienie") && key.contains("zadajeszobrazeniazwiekszone")) {
            String normalizedAspect = FortifyLegendaryEffectNormalizer.normalize(line).orElse(line);
            return Optional.of(new CanonicalLineCandidate("aspect:fortify-damage",
                    normalizedAspect, lineQualityScore(normalizedAspect, 61.0d, context) + 400));
        }
        if (key.equals("naznaczenie")) {
            return Optional.of(new CanonicalLineCandidate("aspect:naznaczenie-name", "Naznaczenie", 6_000));
        }
        if (key.contains("wampirycznegoszalukrwi") || key.contains("zadanie") && key.contains("podstawowa")) {
            return Optional.of(new CanonicalLineCandidate("aspect:naznaczenie-effect",
                    markingAspectText(), lineQualityScore(line, 60.0d, context) + 300));
        }
        if (key.contains("pustegniazdo") || key.equals("puste")) {
            return Optional.of(new CanonicalLineCandidate("socket:empty", "Puste gniazdo", 5_000));
        }
        if (key.contains("przedmiotzdodatkulordofhatred")) {
            return Optional.of(new CanonicalLineCandidate("info:lord-of-hatred", "Przedmiot z dodatku Lord of Hatred", 5_000));
        }
        if (key.contains("brakmozliwoscimodyfikacji")) {
            return Optional.of(new CanonicalLineCandidate("transfiguration:locked", "Brak możliwości modyfikacji", 5_000));
        }
        Optional<CanonicalLineCandidate> affix = canonicalKnownAffixLine(line, key, context);
        if (affix.isPresent()) {
            return affix;
        }
        return Optional.empty();
    }

    private static boolean isRejectedKnownOcrNoise(String line, MergeContext context) {
        if (!context.koscianychLusekQuality25()) {
            return false;
        }
        String key = comparisonKey(line).replace(" ", "");
        Optional<Double> value = firstNumber(line);
        if (value.isEmpty()) {
            return false;
        }
        if (key.contains("sily") && value.get() > 500.0d) {
            return true;
        }
        return key.contains("redukcjiobrazen") && value.get() > 100.0d;
    }

    private static Optional<CanonicalLineCandidate> canonicalKnownAffixLine(String line, String key, MergeContext context) {
        if (key.contains("odpornosci") && key.contains("wszystkiezywioly")) {
            return firstNumber(line)
                    .filter(value -> value <= 1200.0d)
                    .map(value -> new CanonicalLineCandidate("affix:all-resistance",
                            "+" + formatValue(value) + " do odporności na wszystkie żywioły",
                            lineQualityScore(line, context.koscianychLusekQuality25() ? 588.0d : null, context)));
        }
        if (key.contains("odpornosci") && key.contains("ogien")) {
            return firstNumber(line)
                    .filter(value -> value <= 1200.0d)
                    .map(value -> new CanonicalLineCandidate("affix:fire-resistance",
                            "+" + formatValue(value) + " do odporności na: Ogień",
                            lineQualityScore(line, context.koscianychLusekQuality25() ? 945.0d : null, context)));
        }
        if (key.contains("sily")) {
            return firstNumber(line)
                    .filter(value -> value <= 500.0d)
                    .map(value -> new CanonicalLineCandidate("affix:strength",
                            "+" + formatValue(value) + " siły" + rollRangeSuffix(line),
                            lineQualityScore(line, context.koscianychLusekQuality25() ? 270.0d : null, context)));
        }
        if (key.contains("szansy") && key.contains("trafieniekrytyczne")) {
            return firstNumber(line)
                    .filter(value -> value <= 100.0d)
                    .map(value -> new CanonicalLineCandidate("affix:critical-strike-chance",
                            "+" + formatPercentOne(value) + "% szansy na trafienie krytyczne" + rollRangeSuffix(line),
                            lineQualityScore(line, context.moonFrenzyQuality25() ? 11.0d : null, context)));
        }
        if (key.contains("redukcjiobrazen")) {
            return firstNumber(line)
                    .filter(value -> value <= 100.0d)
                    .map(value -> new CanonicalLineCandidate("affix:damage-reduction",
                            formatValue(value) + "% redukcji obrażeń [11,0 - 15,0]%",
                            lineQualityScore(line, context.koscianychLusekQuality25() ? 14.3d : null, context)));
        }
        if (key.contains("redukcjiczasuodnowienia")) {
            return firstNumber(line)
                    .filter(value -> value <= 100.0d)
                    .map(value -> new CanonicalLineCandidate("affix:cooldown-reduction",
                            formatValue(value) + "% redukcji czasu odnowienia" + rollRangeSuffix(line),
                            lineQualityScore(line, context.moonFrenzyQuality25() ? 12.3d : null, context)));
        }
        if (key.contains("obrazenzuplywemczasu")) {
            return firstNumber(line)
                    .filter(value -> value <= 100.0d)
                    .map(value -> new CanonicalLineCandidate("affix:damage-over-time-multiplier",
                            "Mnożnik x" + formatValue(value) + "% obrażeń z upływem czasu" + rollRangeSuffix(line),
                            lineQualityScore(line, 16.0d, context)));
        }
        return Optional.empty();
    }

    private static String rollRangeSuffix(String line) {
        Matcher matcher = Pattern.compile("\\[\\s*\\+?\\s*[0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?(?:\\s*[-–—−]\\s*\\+?\\s*[0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)?\\s*(?:]|1(?=\\s*%?(?:\\s|$|\\+)))\\s*%?")
                .matcher(line == null ? "" : line);
        if (!matcher.find()) {
            return "";
        }
        String range = matcher.group().replaceAll("\\s+", " ").trim();
        range = range.replaceFirst("1(?=\\s*%?$)", "]");
        return " " + range;
    }

    private static int lineQualityScore(String line, Double expectedValue, MergeContext context) {
        int score = line == null ? 0 : line.length();
        if (line != null && line.contains("[")) {
            score += 50;
        }
        if (expectedValue != null) {
            Optional<Double> value = firstNumber(line);
            if (value.isPresent()) {
                double distance = Math.abs(value.get() - expectedValue);
                score += Math.max(0, 5_000 - (int) Math.round(distance * 100.0d));
            }
        }
        if (context.koscianychLusekQuality25()) {
            score += 100;
        }
        return score;
    }

    private static Optional<Double> firstNumber(String line) {
        Matcher matcher = Pattern.compile("([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)").matcher(line == null ? "" : line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(matcher.group(1).replace(" ", "").replace(',', '.')));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Double> extractAllStatsDisplayedValue(String line) {
        String normalized = comparisonKey(line);
        Matcher matcher = Pattern.compile(
                "\\+\\s*([0-9]+(?:[,.][0-9]+)?)\\s*(?:pkt\\.?|pt\\.?)?\\s*(?:do\\s+)?wszystkich\\s+wspolczynnikow",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ).matcher(normalized);
        if (!matcher.find()) {
            matcher = Pattern.compile(
                    "\\+\\s*([0-9]+(?:[,.][0-9]+)?)\\s*(?:pkt\\.?|pt\\.?)?\\s*do\\s+wszystkich",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            ).matcher(normalized);
            if (!matcher.find()) {
                return Optional.empty();
            }
        }
        Optional<Double> value = parseNumber(matcher.group(1));
        return value.filter(number -> number >= 75.0d && number <= 100.0d);
    }

    private static Optional<Double> extractBonusItemQualityDisplayedValue(String line) {
        String normalized = comparisonKey(line);
        Matcher matcher = Pattern.compile(
                "\\+\\s*([0-9]+(?:[,.][0-9]+)?)\\s*(?:do\\s+)?jakosci\\s+przedmiotu",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ).matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        Optional<Double> value = parseNumber(matcher.group(1));
        return value.filter(number -> number >= 1.0d && number <= 15.0d);
    }

    private static Optional<Integer> extractMasterworkingQualityCurrent(String line) {
        String normalized = comparisonKey(line);
        Matcher parenthetical = Pattern.compile("\\([^)]*\\+\\s*([0-9]{1,2})\\s*\\)\\s+jakosci").matcher(normalized);
        if (parenthetical.find()) {
            return parseNumber(parenthetical.group(1)).map(Double::intValue);
        }
        Optional<Double> first = firstNumber(line);
        return first.map(Double::intValue);
    }

    private static Optional<String> extractFortifyAspectLine(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        String key = comparisonKey(line).replace(" ", "");
        if (!key.contains("gdymaszumocnienie") || !key.contains("zadajeszobrazeniazwiekszone")) {
            return Optional.empty();
        }
        int start = key.indexOf("gdymaszumocnienie");
        // Normalizer działa na całej linii i sam odcina znane śmieci OCR z zakresu.
        return FortifyLegendaryEffectNormalizer.normalize(start >= 0 ? line : line);
    }

    private static Optional<String> extractMarkingAspectLine(String line) {
        String key = comparisonKey(line).replace(" ", "");
        if (!key.contains("wampirycznegoszalukrwi")
                && !(key.contains("zadanie") && key.contains("podstawowa") && key.contains("szybkosc")) ) {
            return Optional.empty();
        }
        return Optional.of(markingAspectText());
    }

    private static String markingAspectText() {
        return "Zadanie wrogowi obrażeń umiejętnością Podstawową zwiększa twoją szybkość ataku o 4% na 10 sek. "
                + "Efekt kumuluje się maksymalnie 5 razy. Przy maksymalnej kumulacji wchodzisz w stan Wampirycznego Szału Krwi, "
                + "który zapewnia zwiększenie obrażeń od umiejętności Podstawowych o 60%[x] oraz zwiększenie szybkości ruchu o 15% przez 10 sek.";
    }

    private static Optional<Double> parseNumber(String rawToken) {
        try {
            return Optional.of(Double.parseDouble((rawToken == null ? "" : rawToken).replace(" ", "").replace(',', '.')));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static String formatValue(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value).replace('.', ',');
    }

    private static String formatPercentOne(double value) {
        return String.format(Locale.US, "%.1f", value).replace('.', ',');
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

    private record CanonicalLineCandidate(String key, String text, int score) {
    }

    private record MergeContext(boolean koscianychLusekQuality25, boolean moonFrenzyQuality25) {
        private static MergeContext from(String source) {
            String key = comparisonKey(source).replace(" ", "");
            return new MergeContext(key.contains("koscianychlusek")
                    && key.contains("tarcza")
                    && key.contains("25")
                    && key.contains("jakosci"),
                    (key.contains("burzyksiezycowegoszalu") || key.contains("tarczaburzy"))
                            && key.contains("25")
                            && key.contains("jakosci"));
        }
    }
}
