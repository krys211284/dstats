package krys.web;

import krys.search.BuildSearchRequest;
import krys.search.BuildSearchSkillSpace;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Mapuje dane formularza GUI searcha M10 do istniejącego `BuildSearchRequest`. */
final class SearchBuildFormMapper {
    MappingResult map(SearchBuildFormData formData) {
        List<String> errors = new ArrayList<>();

        List<Integer> levelValues = parseIntegerList(formData.getLevelValues(), "Level values", 1, 5, errors);
        List<Long> weaponDamageValues = parseLongList(formData.getWeaponDamageValues(), "Weapon damage values", 1L, 5, errors);
        List<Double> strengthValues = parseDoubleList(formData.getStrengthValues(), "Strength values", 0.0d, 5, errors);
        List<Double> intelligenceValues = parseDoubleList(formData.getIntelligenceValues(), "Intelligence values", 0.0d, 5, errors);
        List<Double> thornsValues = parseDoubleList(formData.getThornsValues(), "Thorns values", 0.0d, 5, errors);
        List<Double> blockChanceValues = parseDoubleList(formData.getBlockChanceValues(), "Block chance values", 0.0d, 5, errors);
        List<Double> retributionChanceValues = parseDoubleList(formData.getRetributionChanceValues(), "Retribution chance values", 0.0d, 5, errors);
        List<Integer> actionBarSizes = parseIntegerList(formData.getActionBarSizes(), "Rozmiary action bara", 1, 4, errors);
        Integer horizonSeconds = parseInteger(formData.getHorizonSeconds(), "Horyzont symulacji", 1, errors);
        Integer topResultsLimit = parseInteger(formData.getTopResultsLimit(), "Top N", 1, errors);

        Map<SkillId, BuildSearchSkillSpace> skillSpaces = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            SearchBuildFormData.SkillSearchFormData skillConfig = formData.getSkillConfig(skillId);
            List<Integer> rankValues = parseIntegerList(skillConfig.getRankValues(), "Rank values dla " + skillId.name(), 0, 6, errors);
            List<Boolean> baseUpgradeValues = parseBooleanList(skillConfig.getBaseUpgradeValues(), "Base upgrade values dla " + skillId.name(), 2, errors);
            List<SkillUpgradeChoice> choiceValues = parseChoiceList(skillConfig.getChoiceValues(), "Choice values dla " + skillId.name(), 4, errors);
            if (rankValues == null || baseUpgradeValues == null || choiceValues == null) {
                continue;
            }
            try {
                skillSpaces.put(skillId, new BuildSearchSkillSpace(skillId, rankValues, baseUpgradeValues, choiceValues));
            } catch (IllegalArgumentException exception) {
                errors.add(exception.getMessage());
            }
        }

        if (!errors.isEmpty()
                || levelValues == null || weaponDamageValues == null || strengthValues == null
                || intelligenceValues == null || thornsValues == null || blockChanceValues == null
                || retributionChanceValues == null || actionBarSizes == null
                || horizonSeconds == null || topResultsLimit == null) {
            return new MappingResult(null, errors);
        }

        try {
            return new MappingResult(new BuildSearchRequest(
                    levelValues,
                    weaponDamageValues,
                    strengthValues,
                    intelligenceValues,
                    thornsValues,
                    blockChanceValues,
                    retributionChanceValues,
                    skillSpaces,
                    actionBarSizes,
                    horizonSeconds,
                    topResultsLimit
            ), errors);
        } catch (IllegalArgumentException exception) {
            errors.add(exception.getMessage());
            return new MappingResult(null, errors);
        }
    }

    private static Integer parseInteger(String rawValue, String label, int minimumInclusive, List<String> errors) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < minimumInclusive) {
                errors.add(label + " musi być >= " + minimumInclusive + ".");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.add(label + " musi być liczbą całkowitą.");
            return null;
        }
    }

    private static List<Integer> parseIntegerList(String rawValue, String label, int minimumInclusive, int maxItems, List<String> errors) {
        List<String> tokens = splitCsv(rawValue);
        if (!validateTokenCount(tokens, label, maxItems, errors)) {
            return null;
        }
        List<Integer> values = new ArrayList<>();
        for (String token : tokens) {
            try {
                int value = Integer.parseInt(token);
                if (value < minimumInclusive) {
                    errors.add(label + " muszą być >= " + minimumInclusive + ".");
                    return null;
                }
                values.add(value);
            } catch (NumberFormatException exception) {
                errors.add(label + " muszą zawierać liczby całkowite oddzielone przecinkami.");
                return null;
            }
        }
        return values;
    }

    private static List<Long> parseLongList(String rawValue, String label, long minimumInclusive, int maxItems, List<String> errors) {
        List<String> tokens = splitCsv(rawValue);
        if (!validateTokenCount(tokens, label, maxItems, errors)) {
            return null;
        }
        List<Long> values = new ArrayList<>();
        for (String token : tokens) {
            try {
                long value = Long.parseLong(token);
                if (value < minimumInclusive) {
                    errors.add(label + " muszą być >= " + minimumInclusive + ".");
                    return null;
                }
                values.add(value);
            } catch (NumberFormatException exception) {
                errors.add(label + " muszą zawierać liczby całkowite oddzielone przecinkami.");
                return null;
            }
        }
        return values;
    }

    private static List<Double> parseDoubleList(String rawValue, String label, double minimumInclusive, int maxItems, List<String> errors) {
        List<String> tokens = splitCsv(rawValue);
        if (!validateTokenCount(tokens, label, maxItems, errors)) {
            return null;
        }
        List<Double> values = new ArrayList<>();
        for (String token : tokens) {
            try {
                double value = Double.parseDouble(token);
                if (value < minimumInclusive) {
                    errors.add(label + " nie mogą być mniejsze niż " + String.format(Locale.US, "%.0f", minimumInclusive) + ".");
                    return null;
                }
                values.add(value);
            } catch (NumberFormatException exception) {
                errors.add(label + " muszą zawierać liczby oddzielone przecinkami.");
                return null;
            }
        }
        return values;
    }

    private static List<Boolean> parseBooleanList(String rawValue, String label, int maxItems, List<String> errors) {
        List<String> tokens = splitCsv(rawValue);
        if (!validateTokenCount(tokens, label, maxItems, errors)) {
            return null;
        }
        List<Boolean> values = new ArrayList<>();
        for (String token : tokens) {
            String normalized = token.toLowerCase(Locale.ROOT);
            if (!"true".equals(normalized) && !"false".equals(normalized)) {
                errors.add(label + " muszą zawierać wartości true/false oddzielone przecinkami.");
                return null;
            }
            values.add(Boolean.parseBoolean(normalized));
        }
        return values;
    }

    private static List<SkillUpgradeChoice> parseChoiceList(String rawValue, String label, int maxItems, List<String> errors) {
        List<String> tokens = splitCsv(rawValue);
        if (!validateTokenCount(tokens, label, maxItems, errors)) {
            return null;
        }
        List<SkillUpgradeChoice> values = new ArrayList<>();
        for (String token : tokens) {
            try {
                values.add(SkillUpgradeChoice.valueOf(token.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                errors.add(label + " muszą zawierać wartości enum oddzielone przecinkami.");
                return null;
            }
        }
        return values;
    }

    private static boolean validateTokenCount(List<String> tokens, String label, int maxItems, List<String> errors) {
        if (tokens.isEmpty()) {
            errors.add(label + " nie mogą być puste.");
            return false;
        }
        if (tokens.size() > maxItems) {
            errors.add(label + " przekraczają maksymalny wspierany rozmiar formularza (" + maxItems + ").");
            return false;
        }
        return true;
    }

    private static List<String> splitCsv(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        String[] parts = rawValue.split(",");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    static final class MappingResult {
        private final BuildSearchRequest request;
        private final List<String> errors;

        MappingResult(BuildSearchRequest request, List<String> errors) {
            this.request = request;
            this.errors = List.copyOf(errors);
        }

        BuildSearchRequest getRequest() {
            return request;
        }

        List<String> getErrors() {
            return errors;
        }
    }
}
