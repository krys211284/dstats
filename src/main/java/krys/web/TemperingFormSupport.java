package krys.web;

import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingAffixDefinition;
import krys.tempering.TemperingAffixRegistry;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pomocnik formularzy SSR dla hartowania itemów. */
final class TemperingFormSupport {
    private static final TemperingAffixRegistry REGISTRY = ApplicationTemperingAffixRegistry.get();

    private TemperingFormSupport() {
    }

    static ParseResult parse(Map<String, String> fields) {
        List<ItemTemperingAffix> affixes = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int count = parseCount(fields.get("temperingCount"));
        for (int index = 0; index < count; index++) {
            if ("true".equals(fields.get("temperingRemoved_" + index))) {
                continue;
            }
            parseRow(fields, index, errors).ifPresent(affixes::add);
        }
        return new ParseResult(List.copyOf(affixes), List.copyOf(errors));
    }

    private static java.util.Optional<ItemTemperingAffix> parseRow(Map<String, String> fields, int index, List<String> errors) {
        String rawCategory = fields.getOrDefault("temperingCategory_" + index, "");
        String definitionId = fields.getOrDefault("temperingDefinitionId_" + index, "");
        String rawValue = fields.getOrDefault("temperingValue_" + index, "");
        boolean empty = rawCategory.isBlank() && definitionId.isBlank() && rawValue.isBlank();
        if (empty) {
            return java.util.Optional.empty();
        }
        if (rawCategory.isBlank() || definitionId.isBlank() || rawValue.isBlank()) {
            errors.add("Hartowanie #" + (index + 1) + ": kategoria, affix i wartość są wymagane.");
            return java.util.Optional.empty();
        }
        try {
            TemperingCategory category = TemperingCategory.valueOf(rawCategory);
            double value = Double.parseDouble(rawValue.replace(',', '.'));
            if (value < 0.0d) {
                errors.add("Hartowanie #" + (index + 1) + ": wartość nie może być ujemna.");
                return java.util.Optional.empty();
            }
            TemperingRuntimeStatus status = REGISTRY.findById(definitionId)
                    .map(TemperingAffixDefinition::getRuntimeStatus)
                    .orElse(TemperingRuntimeStatus.DATA_ONLY);
            return java.util.Optional.of(new ItemTemperingAffix(definitionId, category, value, "", status));
        } catch (IllegalArgumentException exception) {
            errors.add("Hartowanie #" + (index + 1) + ": niepoprawna kategoria albo wartość.");
            return java.util.Optional.empty();
        }
    }

    private static int parseCount(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(rawValue));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    record ParseResult(List<ItemTemperingAffix> affixes, List<String> errors) {
    }
}
