package krys.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Wyłącznie jawne wejścia pierwszego direct hitu; ranga i upgrade'y nie są edytowalne. */
public record NecromancerCurrentBuildFormData(String nonItemIntelligence, boolean targetVulnerable,
                                             String selectedSkillId) {
    private static final Set<String> POST_FIELDS = Set.of("nonItemIntelligence", "targetVulnerable", "selectedSkillId", "formAction", "slotAction");

    public NecromancerCurrentBuildFormData {
        nonItemIntelligence = nonItemIntelligence == null ? "" : nonItemIntelligence.trim();
        selectedSkillId = selectedSkillId == null ? "" : selectedSkillId.trim();
        if ("NONE".equals(selectedSkillId)) { selectedSkillId = ""; }
    }

    public static NecromancerCurrentBuildFormData fromFormFields(Map<String, String> fields, NecromancerCurrentBuildFormData saved) {
        return new NecromancerCurrentBuildFormData(fields.getOrDefault("nonItemIntelligence", saved.nonItemIntelligence()),
                "true".equals(fields.get("targetVulnerable")), fields.getOrDefault("selectedSkillId", saved.selectedSkillId()));
    }

    public double parsedNonItemIntelligence() {
        try {
            double value = nonItemIntelligence.isBlank() ? 0.0d : Double.parseDouble(nonItemIntelligence.replace(',', '.'));
            if (!Double.isFinite(value) || value < 0.0d) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Inteligencja poza aktywnymi itemami musi być skończoną liczbą nieujemną.");
        }
    }

    public List<String> validationErrors() {
        List<String> errors = new ArrayList<>();
        try { parsedNonItemIntelligence(); } catch (IllegalArgumentException exception) { errors.add(exception.getMessage()); }
        if (!selectedSkillId.isEmpty() && !"krwotok".equals(selectedSkillId)) {
            errors.add("N11 obsługuje wyłącznie Krwotok R1.");
        }
        return List.copyOf(errors);
    }

    public static List<String> postValidationErrors(Map<String, String> fields) {
        List<String> errors = new ArrayList<>();
        if (fields.keySet().stream().anyMatch(key -> !POST_FIELDS.contains(key) && !key.startsWith("selectedItemId_"))) {
            errors.add("N11 nie przyjmuje edycji rang, selections ani pól legacy Paladyna.");
        }
        String vulnerable = fields.get("targetVulnerable");
        if (vulnerable != null && !"true".equals(vulnerable) && !"false".equals(vulnerable)) {
            errors.add("Target Vulnerable musi być wartością boolean.");
        }
        String action = fields.get("formAction");
        if (action != null && !"calculate".equals(action) && !"save".equals(action)) {
            errors.add("Nieobsługiwana akcja formularza Nekromanty.");
        }
        return List.copyOf(errors);
    }
}
