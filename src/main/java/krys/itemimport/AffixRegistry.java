package krys.itemimport;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Katalog znanych affixów używany przez OCR i ręczną walidację itemu. */
public final class AffixRegistry {
    private final List<AffixDefinition> definitions;

    public AffixRegistry() {
        this(ApplicationAffixRegistry.seedDefinitions());
    }

    public AffixRegistry(List<AffixDefinition> definitions) {
        this.definitions = definitions == null ? List.of() : List.copyOf(definitions);
    }

    public List<AffixDefinition> all() {
        return definitions;
    }

    public Optional<AffixDefinition> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return definitions.stream()
                .filter(definition -> definition.getId().equals(id))
                .findFirst();
    }

    public Optional<AffixDefinition> findByType(ImportedItemAffixType type) {
        if (type == null) {
            return Optional.empty();
        }
        return definitions.stream()
                .filter(definition -> definition.getFormType() == type)
                .findFirst();
    }

    public List<AffixTextMatch> findMatches(String text) {
        String normalizedText = normalize(text);
        if (normalizedText.isBlank()) {
            return List.of();
        }
        List<AffixTextMatch> matches = new ArrayList<>();
        for (AffixDefinition definition : definitions) {
            if (!definition.isAutomaticMatchingAllowed()) {
                continue;
            }
            if (!semanticDefinitionAllowsMatch(definition, normalizedText)) {
                continue;
            }
            boolean matched = false;
            for (String alias : definition.getOcrAliases()) {
                String normalizedAlias = normalize(alias);
                int start = normalizedText.indexOf(normalizedAlias);
                if (start >= 0) {
                    matches.add(new AffixTextMatch(definition, start, start + normalizedAlias.length()));
                    matched = true;
                    break;
                }
            }
            if (matched) {
                continue;
            }
            if (allowsFuzzyFallback(definition)) {
                fuzzyAliasStart(definition, normalizedText)
                        .ifPresent(start -> matches.add(new AffixTextMatch(definition, start, start + 1)));
            }
        }
        return matches.stream()
                .sorted(Comparator
                        .comparingInt(AffixTextMatch::start)
                        .thenComparing(match -> match.definition().getId()))
                .toList();
    }

    private static boolean semanticDefinitionAllowsMatch(AffixDefinition definition, String normalizedText) {
        String collapsed = normalizedText.replaceAll("[^A-Z0-9]", "");
        if (definition.getFormType() == ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE) {
            return collapsed.contains("PODSTAWOWEGOZASOBU")
                    || collapsed.contains("PODSTAWOWYZASOB")
                    || collapsed.contains("ODZYSKANIE");
        }
        if (definition.getFormType() == ImportedItemAffixType.LUCKY_HIT_CHANCE) {
            boolean resourceContext = collapsed.contains("PODSTAWOWEGOZASOBU")
                    || collapsed.contains("PODSTAWOWYZASOB")
                    || collapsed.contains("ODZYSKANIE");
            boolean criticalContext = collapsed.contains("TRAFIENIEKRYTYCZNE")
                    || collapsed.contains("KRYTYCZNE");
            boolean chanceContext = collapsed.contains("SZANS")
                    || collapsed.contains("LUCKYHITCHANCE")
                    || collapsed.contains("SZCZESLIWYTRAF")
                    || collapsed.contains("TRAF");
            return chanceContext && !resourceContext && !criticalContext;
        }
        if (definition.getFormType() == ImportedItemAffixType.ALL_DAMAGE_MULTIPLIER) {
            boolean allDamageContext = collapsed.contains("WSZYSTKICHOBRAZEN")
                    || collapsed.contains("ALLDAMAGE");
            String valueShapeText = normalizedText.replaceAll("[^A-Z0-9%]", "");
            return allDamageContext && hasMultiplierValueShape(valueShapeText);
        }
        if (definition.getFormType() == ImportedItemAffixType.CORE_SKILL_RANKS) {
            return hasExplicitRankValueShape(normalizedText);
        }
        return true;
    }

    private static boolean hasMultiplierValueShape(String collapsedText) {
        if (collapsedText == null || collapsedText.isBlank()) {
            return false;
        }
        return Pattern.compile("MNOZNIKX?[0-9]+").matcher(collapsedText).find()
                || Pattern.compile("X[0-9]+").matcher(collapsedText).find()
                || Pattern.compile("[0-9]+%").matcher(collapsedText).find()
                || Pattern.compile("[0-9]+0WSZYSTKICHOBRAZEN").matcher(collapsedText).find();
    }

    private static boolean hasExplicitRankValueShape(String normalizedText) {
        String safeText = normalizedText == null ? "" : normalizedText;
        String collapsed = safeText.replaceAll("[^A-Z0-9+]", "");
        boolean explicitRankValue = Pattern.compile("(^|[^0-9])\\+\\s*[1-9][0-9]?\\s+DO\\s+UMIEJETNOSCI")
                .matcher(safeText)
                .find()
                || Pattern.compile("(^|[^0-9])\\+\\s*[1-9][0-9]?\\s+DO\\s+RANG")
                .matcher(safeText)
                .find()
                || Pattern.compile("\\+[1-9][0-9]?DOUMIEJETNOSCI").matcher(collapsed).find()
                || Pattern.compile("\\+[1-9][0-9]?DORANG").matcher(collapsed).find();
        return explicitRankValue
                && (safeText.contains("GLOWNE") || safeText.contains("CORE"))
                && !safeText.contains("UMIEJETNOSCI PODSTAWOWE");
    }

    private static boolean allowsFuzzyFallback(AffixDefinition definition) {
        return definition.getFormType() == ImportedItemAffixType.LUCKY_HIT_CHANCE
                || definition.getFormType() == ImportedItemAffixType.CRITICAL_STRIKE_CHANCE
                || definition.getFormType() == ImportedItemAffixType.MOVEMENT_SPEED
                || definition.getFormType() == ImportedItemAffixType.CORE_SKILL_RANKS;
    }

    private static Optional<Integer> fuzzyAliasStart(AffixDefinition definition, String normalizedText) {
        String[] textTokens = normalizedText.split("\\s+");
        Optional<Integer> bestStart = Optional.empty();
        for (String alias : definition.getOcrAliases()) {
            List<String> aliasTokens = significantTokens(normalize(alias));
            if (aliasTokens.isEmpty()) {
                continue;
            }
            int start = fuzzyTokensStart(textTokens, aliasTokens);
            if (start >= 0 && (bestStart.isEmpty() || start < bestStart.get())) {
                bestStart = Optional.of(start);
            }
        }
        return bestStart;
    }

    private static List<String> significantTokens(String normalizedAlias) {
        List<String> tokens = new ArrayList<>();
        for (String token : normalizedAlias.split("\\s+")) {
            if (token.length() < 4 || token.matches("[0-9]+")) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private static int fuzzyTokensStart(String[] textTokens, List<String> aliasTokens) {
        int firstMatch = -1;
        int nextTextIndex = 0;
        for (String aliasToken : aliasTokens) {
            boolean tokenMatched = false;
            for (int index = nextTextIndex; index < textTokens.length; index++) {
                if (tokensCompatible(textTokens[index], aliasToken)) {
                    if (firstMatch < 0) {
                        firstMatch = index;
                    }
                    nextTextIndex = index + 1;
                    tokenMatched = true;
                    break;
                }
            }
            if (!tokenMatched) {
                return -1;
            }
        }
        return firstMatch;
    }

    private static boolean tokensCompatible(String textToken, String aliasToken) {
        if (textToken.equals(aliasToken) || textToken.contains(aliasToken) || aliasToken.contains(textToken)) {
            return true;
        }
        int tolerance = aliasToken.length() >= 8 ? 2 : 1;
        return levenshteinDistance(textToken, aliasToken, tolerance) <= tolerance;
    }

    private static int levenshteinDistance(String left, String right, int maxDistance) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int index = 0; index <= right.length(); index++) {
            previous[index] = index;
        }
        for (int leftIndex = 1; leftIndex <= left.length(); leftIndex++) {
            current[0] = leftIndex;
            int rowMin = current[0];
            for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {
                int substitutionCost = left.charAt(leftIndex - 1) == right.charAt(rightIndex - 1) ? 0 : 1;
                current[rightIndex] = Math.min(
                        Math.min(current[rightIndex - 1] + 1, previous[rightIndex] + 1),
                        previous[rightIndex - 1] + substitutionCost
                );
                rowMin = Math.min(rowMin, current[rightIndex]);
            }
            if (rowMin > maxDistance) {
                return maxDistance + 1;
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Dopasowanie definicji affixu do fragmentu OCR. */
    public record AffixTextMatch(AffixDefinition definition, int start, int end) {
    }
}
