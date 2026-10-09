package krys.itemimport.image;

import krys.itemimport.AffixDefinition;
import krys.itemimport.AffixRegistry;
import krys.itemimport.AffixValueUnit;
import krys.itemimport.ApplicationAffixRegistry;
import krys.itemimport.ImportedItemAffixType;
import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.TemperingAffixDefinition;
import krys.tempering.TemperingAffixRegistry;
import krys.tempering.TemperingValueUnit;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.TransfigurationAffixCatalog;
import krys.transfiguration.TransfigurationAffixDefinition;
import krys.transfiguration.TransfigurationAffixValueKind;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Czyta neutralne wartosci PropertyBlock bez budowania ImportedItemAffix ani ItemImportDetails. */
public final class PropertyBlockValueReader {
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern BRACKET_VALUE = Pattern.compile(
            "\\[\\s*\\+?\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)(?:\\s*[-–—−]\\s*\\+?\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?))?\\s*]?\\s*%?");

    private final AffixRegistry affixRegistry;
    private final TemperingAffixRegistry temperingRegistry;

    public PropertyBlockValueReader() {
        this(ApplicationAffixRegistry.get(), ApplicationTemperingAffixRegistry.get());
    }

    public PropertyBlockValueReader(AffixRegistry affixRegistry, TemperingAffixRegistry temperingRegistry) {
        this.affixRegistry = Objects.requireNonNull(affixRegistry, "affixRegistry");
        this.temperingRegistry = Objects.requireNonNull(temperingRegistry, "temperingRegistry");
    }

    public PropertyBlockValueReadResult read(PropertyBlockReadResult readResult,
                                             PropertyBlockSemanticResult semanticResult) {
        Objects.requireNonNull(readResult, "readResult");
        Objects.requireNonNull(semanticResult, "semanticResult");
        validatePair(readResult, semanticResult);

        List<PropertyBlockNumericEvidence> evidence = semanticResult.getNumericEvidence().isEmpty()
                ? readResult.getNumericEvidence()
                : semanticResult.getNumericEvidence();
        boolean ambiguous = !semanticResult.isNumericValueCertain()
                || evidence.stream().anyMatch(PropertyBlockNumericEvidence::isAmbiguous);
        List<String> diagnostics = new ArrayList<>(semanticResult.getDiagnostics());
        if (ambiguous && diagnostics.stream().noneMatch(value -> value.contains("NUMERIC_OCR_AMBIGUOUS"))) {
            diagnostics.add("NUMERIC_OCR_AMBIGUOUS propagated_to_value_read");
        }
        List<PropertyBlockValueReadResult.NumericParameter> parameters = parameters(evidence, "parameter");

        return switch (semanticResult.getKind()) {
            case BASE_STAT -> baseStat(readResult, semanticResult, evidence, parameters, ambiguous, diagnostics);
            case IMPLICIT, AFFIX -> ordinaryValue(readResult, semanticResult, evidence, parameters, ambiguous, diagnostics);
            case TEMPERING -> temperingValue(readResult, semanticResult, evidence, parameters, ambiguous, diagnostics);
            case TRANSFIGURATION -> transfigurationValue(readResult, semanticResult, evidence, parameters, ambiguous, diagnostics);
            case ASPECT -> parameterOnly(readResult, semanticResult, parameters, ambiguous, diagnostics, "aspectParameters");
            case SOCKET -> socketValue(readResult, semanticResult, parameters, ambiguous, diagnostics);
            case UNKNOWN, AMBIGUOUS -> parameterOnly(readResult, semanticResult, parameters, ambiguous, diagnostics, "unmapped");
        };
    }

    private PropertyBlockValueReadResult baseStat(PropertyBlockReadResult readResult,
                                                  PropertyBlockSemanticResult semanticResult,
                                                  List<PropertyBlockNumericEvidence> evidence,
                                                  List<PropertyBlockValueReadResult.NumericParameter> parameters,
                                                  boolean ambiguous,
                                                  List<String> diagnostics) {
        String normalizedText = normalize(readResult.getRawText());
        SelectedNumbers selected = selectedNumbers(evidence, readResult.getRawText());
        Long itemArmor = null;
        Long weaponDps = null;
        Long weaponDamageMin = null;
        Long weaponDamageMax = null;
        Double attacksPerSecond = null;
        Double value = null;
        String role = "baseStat";

        if (containsArmor(normalizedText)) {
            value = selected.firstValue().orElse(null);
            itemArmor = roundedLong(value);
            role = "itemArmor";
            parameters = rerole(parameters, List.of("itemArmor"));
        } else if (containsWeaponDps(normalizedText)) {
            weaponDps = roundedLong(selected.firstValue().orElse(null));
            Optional<NumberToken> damage = selected.tokens().stream().filter(NumberToken::isRange).findFirst();
            if (damage.isPresent()) {
                weaponDamageMin = roundedLong(damage.get().rangeMin());
                weaponDamageMax = roundedLong(damage.get().rangeMax());
            }
            attacksPerSecond = selected.tokens().stream()
                    .filter(token -> !token.isRange())
                    .skip(1)
                    .map(NumberToken::value)
                    .findFirst()
                    .orElse(null);
            value = weaponDps == null ? null : weaponDps.doubleValue();
            role = "weaponBaseStats";
            parameters = rerole(parameters, List.of("weaponDps", "weaponDamageRange", "attacksPerSecond"));
        }

        return result(readResult, semanticResult, role, value, "", null, null, null,
                itemArmor, weaponDps, weaponDamageMin, weaponDamageMax, attacksPerSecond,
                parameters, !ambiguous && hasUsefulValue(value, itemArmor, weaponDps, weaponDamageMin, attacksPerSecond),
                ambiguous, diagnostics);
    }

    private PropertyBlockValueReadResult ordinaryValue(PropertyBlockReadResult readResult,
                                                       PropertyBlockSemanticResult semanticResult,
                                                       List<PropertyBlockNumericEvidence> evidence,
                                                       List<PropertyBlockValueReadResult.NumericParameter> parameters,
                                                       boolean ambiguous,
                                                       List<String> diagnostics) {
        SelectedNumbers selected = selectedNumbers(evidence, readResult.getRawText());
        Optional<NumberToken> firstValue = selected.tokens().stream().filter(token -> !token.isRange()).findFirst();
        BracketValue bracket = parseBracket(readResult.getRawText()).orElse(BracketValue.empty());
        String unit = ordinaryUnit(readResult.getRawText(), semanticResult.getDefinitionId(), firstValue.orElse(null));
        Double value = firstValue.map(NumberToken::value).orElse(null);
        Double rollMin = bracket.range() ? bracket.min() : selectedFirstRangeAfterCurrent(selected).map(NumberToken::rangeMin).orElse(null);
        Double rollMax = bracket.range() ? bracket.max() : selectedFirstRangeAfterCurrent(selected).map(NumberToken::rangeMax).orElse(null);
        Double referenceValue = bracket.single() ? bracket.min() : null;

        return result(readResult, semanticResult, semanticResult.getKind() == PropertyBlockSemanticKind.IMPLICIT
                        ? "implicitValue" : "affixValue",
                value, unit, rollMin, rollMax, referenceValue,
                null, null, null, null, null,
                rerole(parameters, ordinaryParameterRoles(parameters, rollMin, referenceValue)),
                !ambiguous && value != null,
                ambiguous, diagnostics);
    }

    private PropertyBlockValueReadResult temperingValue(PropertyBlockReadResult readResult,
                                                        PropertyBlockSemanticResult semanticResult,
                                                        List<PropertyBlockNumericEvidence> evidence,
                                                        List<PropertyBlockValueReadResult.NumericParameter> parameters,
                                                        boolean ambiguous,
                                                        List<String> diagnostics) {
        if (ambiguous) {
            return result(readResult, semanticResult, "temperingValue", null, unitFromTempering(semanticResult),
                    null, null, null, null, null, null, null, null,
                    parameters, false, true, diagnostics);
        }
        SelectedNumbers selected = selectedNumbers(evidence, readResult.getRawText());
        Double value = selected.tokens().stream().filter(token -> !token.isRange()).findFirst()
                .map(NumberToken::value)
                .orElse(null);
        BracketValue bracket = parseBracket(readResult.getRawText()).orElse(BracketValue.empty());
        return result(readResult, semanticResult, "temperingValue", value, unitFromTempering(semanticResult),
                bracket.range() ? bracket.min() : null,
                bracket.range() ? bracket.max() : null,
                bracket.single() ? bracket.min() : null,
                null, null, null, null, null,
                rerole(parameters, ordinaryParameterRoles(parameters, bracket.range() ? bracket.min() : null,
                        bracket.single() ? bracket.min() : null)),
                value != null, false, diagnostics);
    }

    private PropertyBlockValueReadResult transfigurationValue(PropertyBlockReadResult readResult,
                                                              PropertyBlockSemanticResult semanticResult,
                                                              List<PropertyBlockNumericEvidence> evidence,
                                                              List<PropertyBlockValueReadResult.NumericParameter> parameters,
                                                              boolean ambiguous,
                                                              List<String> diagnostics) {
        SelectedNumbers selected = selectedNumbers(evidence, readResult.getRawText());
        Double value = selected.tokens().stream().filter(token -> !token.isRange()).findFirst()
                .map(NumberToken::value)
                .orElse(null);
        BracketValue bracket = parseBracket(readResult.getRawText()).orElse(BracketValue.empty());
        return result(readResult, semanticResult, "transfigurationValue", value, unitFromTransfiguration(semanticResult),
                bracket.range() ? bracket.min() : null,
                bracket.range() ? bracket.max() : null,
                bracket.single() ? bracket.min() : null,
                null, null, null, null, null,
                rerole(parameters, ordinaryParameterRoles(parameters, bracket.range() ? bracket.min() : null,
                        bracket.single() ? bracket.min() : null)),
                !ambiguous && value != null, ambiguous, diagnostics);
    }

    private PropertyBlockValueReadResult socketValue(PropertyBlockReadResult readResult,
                                                     PropertyBlockSemanticResult semanticResult,
                                                     List<PropertyBlockValueReadResult.NumericParameter> parameters,
                                                     boolean ambiguous,
                                                     List<String> diagnostics) {
        if ("EMPTY_SOCKET".equals(semanticResult.getDefinitionId())) {
            return result(readResult, semanticResult, "emptySocket", null, "", null, null, null,
                    null, null, null, null, null, List.of(), !ambiguous, ambiguous, diagnostics);
        }
        return parameterOnly(readResult, semanticResult, rerole(parameters, List.of("socketParameter")),
                ambiguous, diagnostics, "socketParameters");
    }

    private PropertyBlockValueReadResult parameterOnly(PropertyBlockReadResult readResult,
                                                       PropertyBlockSemanticResult semanticResult,
                                                       List<PropertyBlockValueReadResult.NumericParameter> parameters,
                                                       boolean ambiguous,
                                                       List<String> diagnostics,
                                                       String role) {
        return result(readResult, semanticResult, role, null, "", null, null, null,
                null, null, null, null, null,
                parameters, !ambiguous, ambiguous, diagnostics);
    }

    private PropertyBlockValueReadResult result(PropertyBlockReadResult readResult,
                                                PropertyBlockSemanticResult semanticResult,
                                                String role,
                                                Double value,
                                                String unit,
                                                Double rollRangeMin,
                                                Double rollRangeMax,
                                                Double referenceValue,
                                                Long itemArmor,
                                                Long weaponDps,
                                                Long weaponDamageMin,
                                                Long weaponDamageMax,
                                                Double attacksPerSecond,
                                                List<PropertyBlockValueReadResult.NumericParameter> parameters,
                                                boolean certain,
                                                boolean ambiguous,
                                                List<String> diagnostics) {
        return new PropertyBlockValueReadResult(
                readResult.getSourcePart(),
                readResult.getBlockOrder(),
                semanticResult.getKind(),
                semanticResult.getDefinitionId(),
                role,
                value,
                unit,
                rollRangeMin,
                rollRangeMax,
                referenceValue,
                itemArmor,
                weaponDps,
                weaponDamageMin,
                weaponDamageMax,
                attacksPerSecond,
                parameters,
                certain,
                ambiguous,
                diagnostics,
                semanticResult.getNumericEvidence().isEmpty()
                        ? readResult.getNumericEvidence()
                        : semanticResult.getNumericEvidence()
        );
    }

    private static List<PropertyBlockValueReadResult.NumericParameter> parameters(
            List<PropertyBlockNumericEvidence> evidence,
            String role) {
        List<PropertyBlockValueReadResult.NumericParameter> parameters = new ArrayList<>();
        for (PropertyBlockNumericEvidence item : evidence) {
            NumberToken token = item.getSelectedValue().isBlank() ? null : parseNumberToken(item.getSelectedValue()).orElse(null);
            parameters.add(new PropertyBlockValueReadResult.NumericParameter(
                    item.getTokenOrder(),
                    role,
                    item.getSelectedValue(),
                    token == null || token.isRange() ? null : token.value(),
                    token == null || !token.isRange() ? null : token.rangeMin(),
                    token == null || !token.isRange() ? null : token.rangeMax(),
                    token == null ? "" : token.unit(),
                    item.isAmbiguous(),
                    item.getAlternativeValues()
            ));
        }
        return parameters;
    }

    private static List<PropertyBlockValueReadResult.NumericParameter> rerole(
            List<PropertyBlockValueReadResult.NumericParameter> parameters,
            List<String> roles) {
        List<PropertyBlockValueReadResult.NumericParameter> rerolled = new ArrayList<>();
        for (int index = 0; index < parameters.size(); index++) {
            PropertyBlockValueReadResult.NumericParameter parameter = parameters.get(index);
            String role = roles.isEmpty() ? parameter.getRole() : roles.get(Math.min(index, roles.size() - 1));
            rerolled.add(new PropertyBlockValueReadResult.NumericParameter(
                    parameter.getOrder(),
                    role,
                    parameter.getRawToken(),
                    parameter.getValue(),
                    parameter.getRangeMin(),
                    parameter.getRangeMax(),
                    parameter.getUnit(),
                    parameter.isAmbiguous(),
                    parameter.getAlternativeValues()
            ));
        }
        return rerolled;
    }

    private static List<String> ordinaryParameterRoles(List<PropertyBlockValueReadResult.NumericParameter> parameters,
                                                       Double rollRangeMin,
                                                       Double referenceValue) {
        List<String> roles = new ArrayList<>();
        for (int index = 0; index < parameters.size(); index++) {
            if (index == 0) {
                roles.add("currentValue");
            } else if (rollRangeMin != null) {
                roles.add("rollRange");
            } else if (referenceValue != null) {
                roles.add("referenceValue");
            } else {
                roles.add("parameter");
            }
        }
        return roles;
    }

    private static SelectedNumbers selectedNumbers(List<PropertyBlockNumericEvidence> evidence, String rawText) {
        List<NumberToken> tokens = new ArrayList<>();
        for (PropertyBlockNumericEvidence item : evidence) {
            String token = item.getSelectedValue();
            if ((token == null || token.isBlank()) && item.isAmbiguous()) {
                token = literalAlternativeFromRaw(item, rawText).orElse("");
            }
            if (!token.isBlank()) {
                parseNumberToken(token).ifPresent(tokens::add);
            }
        }
        return new SelectedNumbers(tokens);
    }

    private static Optional<String> literalAlternativeFromRaw(PropertyBlockNumericEvidence evidence, String rawText) {
        String normalizedRaw = normalizeNumericText(rawText);
        return evidence.getAlternativeValues().stream()
                .filter(alternative -> parseNumberToken(alternative).isPresent())
                .filter(alternative -> normalizedRaw.contains(normalizeNumericText(alternative)))
                .max((left, right) -> Integer.compare(ambiguityCandidateScore(left), ambiguityCandidateScore(right)));
    }

    private static int ambiguityCandidateScore(String token) {
        String safe = token == null ? "" : token;
        int score = safe.length();
        if (safe.contains("+")) {
            score += 10;
        }
        if (safe.contains("%")) {
            score += 5;
        }
        return score;
    }

    private static Optional<NumberToken> selectedFirstRangeAfterCurrent(SelectedNumbers selected) {
        return selected.tokens().stream().skip(1).filter(NumberToken::isRange).findFirst();
    }

    private static Optional<NumberToken> parseNumberToken(String token) {
        String safeToken = token == null ? "" : token.trim();
        if (safeToken.isBlank() || !DIGIT.matcher(safeToken).find()) {
            return Optional.empty();
        }
        String normalized = safeToken
                .replace('–', '-')
                .replace('—', '-')
                .replace('−', '-');
        boolean percent = normalized.contains("%");
        Matcher rangeMatcher = Pattern.compile(
                "^\\+?\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)\\s*-\\s*\\+?\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)\\s*%?$")
                .matcher(normalized);
        if (rangeMatcher.find()) {
            Optional<Double> min = parseDouble(rangeMatcher.group(1));
            Optional<Double> max = parseDouble(rangeMatcher.group(2));
            if (min.isPresent() && max.isPresent() && min.get() <= max.get()) {
                return Optional.of(NumberToken.range(safeToken, min.get(), max.get(), percent ? "%" : ""));
            }
        }
        Matcher valueMatcher = Pattern.compile("\\+?\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)\\s*%?")
                .matcher(normalized);
        if (!valueMatcher.find()) {
            return Optional.empty();
        }
        return parseDouble(valueMatcher.group(1))
                .map(value -> NumberToken.single(safeToken, value, percent ? "%" : ""));
    }

    private static Optional<BracketValue> parseBracket(String rawText) {
        Matcher matcher = BRACKET_VALUE.matcher(rawText == null ? "" : rawText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        Optional<Double> min = parseDouble(matcher.group(1));
        Optional<Double> max = matcher.group(2) == null ? min : parseDouble(matcher.group(2));
        if (min.isEmpty() || max.isEmpty() || min.get() > max.get()) {
            return Optional.empty();
        }
        return Optional.of(new BracketValue(min.get(), max.get(), matcher.group(2) == null));
    }

    private static Optional<Double> parseDouble(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(token.replace(" ", "").replace(',', '.')));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String ordinaryUnit(String rawText, String definitionId, NumberToken firstValue) {
        if (firstValue != null && "%".equals(firstValue.unit())) {
            return "%";
        }
        Optional<ImportedItemAffixType> type = affixRegistry.findById(definitionId)
                .map(AffixDefinition::getFormType)
                .or(() -> ImportedItemAffixType.detectFromLine(rawText));
        if (type.isEmpty()) {
            return "";
        }
        return switch (type.get()) {
            case BLOCK_CHANCE, RETRIBUTION_CHANCE, CRITICAL_STRIKE_CHANCE, LUCKY_HIT_CHANCE,
                 COOLDOWN_REDUCTION, MOVEMENT_SPEED, DODGE_CHANCE, DAMAGE_REDUCTION,
                 ALL_DAMAGE_MULTIPLIER, DAMAGE_OVER_TIME_MULTIPLIER -> "%";
            default -> "";
        };
    }

    private String unitFromTempering(PropertyBlockSemanticResult semanticResult) {
        return temperingRegistry.findById(semanticResult.getDefinitionId())
                .map(TemperingAffixDefinition::getUnit)
                .filter(unit -> unit == TemperingValueUnit.PERCENT)
                .map(unit -> "%")
                .orElse("");
    }

    private static String unitFromTransfiguration(PropertyBlockSemanticResult semanticResult) {
        if (semanticResult.getTransfigurationOutcome() == HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY) {
            return "";
        }
        return TransfigurationAffixCatalog.findById(semanticResult.getDefinitionId())
                .map(TransfigurationAffixDefinition::getValueKind)
                .filter(kind -> kind == TransfigurationAffixValueKind.PERCENT
                        || kind == TransfigurationAffixValueKind.MULTIPLICATIVE_PERCENT)
                .map(kind -> "%")
                .orElse("");
    }

    private static boolean containsArmor(String normalizedText) {
        return normalizedText.contains("PANCERZA");
    }

    private static boolean containsWeaponDps(String normalizedText) {
        return normalizedText.contains("OBRAZEN NA SEK");
    }

    private static boolean hasUsefulValue(Double value,
                                          Long itemArmor,
                                          Long weaponDps,
                                          Long weaponDamageMin,
                                          Double attacksPerSecond) {
        return value != null || itemArmor != null || weaponDps != null
                || weaponDamageMin != null || attacksPerSecond != null;
    }

    private static Long roundedLong(Double value) {
        return value == null ? null : Math.round(value);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizeNumericText(String value) {
        return (value == null ? "" : value)
                .replace('–', '-')
                .replace('—', '-')
                .replace('−', '-')
                .replaceAll("\\s+", "")
                .replace('.', ',');
    }

    private static void validatePair(PropertyBlockReadResult readResult, PropertyBlockSemanticResult semanticResult) {
        if (readResult.getSourcePart() != semanticResult.getSourcePart()
                || readResult.getBlockOrder() != semanticResult.getBlockOrder()) {
            throw new IllegalArgumentException("PropertyBlockReadResult i PropertyBlockSemanticResult musza opisywac ten sam blok.");
        }
    }

    private record SelectedNumbers(List<NumberToken> tokens) {
        private Optional<Double> firstValue() {
            return tokens.stream().filter(token -> !token.isRange()).findFirst().map(NumberToken::value);
        }
    }

    private record NumberToken(String raw, Double value, Double rangeMin, Double rangeMax, String unit) {
        private static NumberToken single(String raw, double value, String unit) {
            return new NumberToken(raw, value, null, null, unit);
        }

        private static NumberToken range(String raw, double min, double max, String unit) {
            return new NumberToken(raw, null, min, max, unit);
        }

        private boolean isRange() {
            return rangeMin != null && rangeMax != null;
        }
    }

    private record BracketValue(Double min, Double max, boolean single) {
        private static BracketValue empty() {
            return new BracketValue(null, null, false);
        }

        private boolean range() {
            return min != null && max != null && !single;
        }
    }
}
