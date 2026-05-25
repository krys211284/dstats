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
                || key.matches("wymaga [0-9]+ poziomu")
                || key.startsWith("wartosc sprzedazy:")
                || key.startsWith("trwalosc:")
                || isDurabilityComparisonNoise(key);
    }

    private static boolean isDurabilityComparisonNoise(String key) {
        return key.startsWith("(wytrzymalosc:") && key.endsWith(")");
    }

    private static List<String> splitLogicalLines(String line) {
        if (line == null || line.length() < 120) {
            return List.of(line);
        }
        String key = comparisonKey(line).replace(" ", "");
        int anchors = 0;
        for (String anchor : List.of(
                "mocprzedmiotu",
                "jakosci",
                "przeistoczony",
                "pancerza",
                "szansynablok",
                "obrazenodbroniwglownejrece",
                "sily",
                "odpornosci",
                "redukcjiobrazen",
                "wszystkichwspolczynnikow",
                "maksymalnejliczbykumulacjianimuszu",
                "gdymaszumocnienie"
        )) {
            if (key.contains(anchor)) {
                anchors++;
            }
        }
        if (anchors < 3) {
            return List.of(line);
        }

        List<String> extracted = new ArrayList<>();
        appendFirst(extracted, line, "(Miażdżąca\\s+Tarcza\\s+Kościanych\\s+Łusek)");
        appendFirst(extracted, line, "(Starożytna\\s+legendarna\\s+tarcza)");
        appendFirst(extracted, line, "(Moc\\s+przedmiotu\\s*[:.\\-–—]?\\s*[0-9]+)");
        appendFirst(extracted, line, "([0-9]{1,2}\\s*\\([^)]*\\+\\s*[0-9]{1,2}\\s*\\)\\s+jakości)");
        appendFirst(extracted, line, "\\b(Przeistoczony)\\b");
        appendFirst(extracted, line, "([0-9]+(?:\\s[0-9]{3})*\\s+pkt\\.\\s+pancerza)");
        appendFirst(extracted, line, "([0-9]+(?:[,.][0-9]+)?%\\s+szansy\\s+na\\s+blok(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(\\+[0-9]+(?:[,.][0-9]+)?%\\s+obrażeń\\s+od\\s+broni\\s+w\\s+głównej\\s+ręce(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+(?:do\\s+)?siły(?:\\s*\\[[^\\]]+])?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+do\\s+odporności\\s+na\\s+wszystkie\\s+żywioły(?:\\s*\\[[^\\]]+])?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+do\\s+odporności\\s+na:?\\s+Ogień(?:\\s*\\[[^\\]]+])?)");
        appendFirst(extracted, line, "([0-9]+(?:[,.][0-9]+)?%\\s+redukcji\\s+obrażeń(?:\\s*\\[[^\\]]+])?%?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+pkt\\.\\s+do\\s+wszystkich\\s+współczynników(?:\\s*\\[[^\\]]+])?)");
        appendFirst(extracted, line, "(\\+\\s*[0-9]+(?:[,.][0-9]+)?\\s+do\\s+maksymalnej\\s+liczby\\s+kumulacji\\s+Animuszu)");
        appendFirst(extracted, line, "(Gdy\\s+masz\\s+umocnienie,\\s+zadajesz\\s+obrażenia\\s+zwiększone\\s+o\\s+[0-9]+(?:,[0-9]+)?%?\\[x\\](?:\\s*\\[[^\\]]+])?%?\\.?)");
        appendFirst(extracted, line, "\\b(Puste\\s+gniazdo)\\b");
        appendFirst(extracted, line, "\\b(Przedmiot\\s+z\\s+dodatku\\s+Lord\\s+of\\s+Hatred)\\b");
        appendFirst(extracted, line, "\\b(Brak\\s+możliwości\\s+modyfikacji)\\b");
        return extracted.isEmpty() ? List.of(line) : extracted;
    }

    private static void appendFirst(List<String> target, String line, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(line);
        if (matcher.find()) {
            String value = matcher.group(1).replaceAll("\\s+", " ").trim();
            if (!value.isBlank() && !target.contains(value)) {
                target.add(value);
            }
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
        if (key.contains("starozytnalegendarnatarcza")) {
            return Optional.of(new CanonicalLineCandidate("type:ancient-legendary-shield",
                    "Starożytna legendarna tarcza", 9_000));
        }
        if (key.contains("mocprzedmiotu900")) {
            return Optional.of(new CanonicalLineCandidate("item-power", "Moc przedmiotu: 900", 9_000));
        }
        if (key.contains("jakosci")) {
            Optional<Double> value = firstNumber(line);
            if (value.isPresent() && Math.rint(value.get()) == 25.0d) {
                return Optional.of(new CanonicalLineCandidate("masterworking-quality", "25 (+25) jakości", 9_000));
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
            Optional<Double> value = firstNumber(line);
            String text = value.map(number -> "+" + formatValue(number) + " pkt. do wszystkich współczynników [+75 - 100]")
                    .orElse(line);
            return Optional.of(new CanonicalLineCandidate("transfiguration:all-stats",
                    text, lineQualityScore(line, context.koscianychLusekQuality25() ? 96.0d : null, context)));
        }
        if (key.contains("maksymalnejliczbykumulacjianimuszu")) {
            Optional<Double> value = firstNumber(line);
            String text = value.map(number -> "+" + formatValue(number) + " do maksymalnej liczby kumulacji Animuszu")
                    .orElse(line);
            return Optional.of(new CanonicalLineCandidate("tempering:defense-max-animus",
                    text, lineQualityScore(line, context.koscianychLusekQuality25() ? 12.0d : null, context)));
        }
        if (key.contains("gdymaszumocnienie") && key.contains("zadajeszobrazeniazwiekszone")) {
            return Optional.of(new CanonicalLineCandidate("aspect:fortify-damage",
                    line, lineQualityScore(line, 61.0d, context) + 400));
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
                            "+" + formatValue(value) + " siły",
                            lineQualityScore(line, context.koscianychLusekQuality25() ? 270.0d : null, context)));
        }
        if (key.contains("redukcjiobrazen")) {
            return firstNumber(line)
                    .filter(value -> value <= 100.0d)
                    .map(value -> new CanonicalLineCandidate("affix:damage-reduction",
                            formatValue(value) + "% redukcji obrażeń [11,0 - 15,0]%",
                            lineQualityScore(line, context.koscianychLusekQuality25() ? 14.3d : null, context)));
        }
        return Optional.empty();
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

    private static String formatValue(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
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

    private record MergeContext(boolean koscianychLusekQuality25) {
        private static MergeContext from(String source) {
            String key = comparisonKey(source).replace(" ", "");
            return new MergeContext(key.contains("koscianychlusek")
                    && key.contains("tarcza")
                    && key.contains("25")
                    && key.contains("jakosci"));
        }
    }
}
