package krys.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Wyłącznie jawne wejścia pierwszego direct hitu; ranga i upgrade'y nie są edytowalne. */
public record NecromancerCurrentBuildFormData(String additionalNonItemIntelligence, boolean targetVulnerable,
                                             String selectedSkillId) {
    private static final Set<String> POST_FIELDS = Set.of("additionalNonItemIntelligence", "nonItemIntelligence", "targetVulnerable", "selectedSkillId", "formAction", "slotAction");

    public NecromancerCurrentBuildFormData {
        additionalNonItemIntelligence = additionalNonItemIntelligence == null ? "" : additionalNonItemIntelligence.trim();
        selectedSkillId = selectedSkillId == null ? "" : selectedSkillId.trim();
        if ("NONE".equals(selectedSkillId)) { selectedSkillId = ""; }
    }

    public static NecromancerCurrentBuildFormData fromFormFields(Map<String, String> fields, NecromancerCurrentBuildFormData saved,
                                                                 int classLevelIntelligence) {
        String additional = fields.getOrDefault("additionalNonItemIntelligence", saved.additionalNonItemIntelligence());
        if (!fields.containsKey("additionalNonItemIntelligence") && fields.containsKey("nonItemIntelligence")) {
            additional = NecromancerCurrentBuildFormQuerySupport.additionalFromLegacy(fields.get("nonItemIntelligence"), classLevelIntelligence);
        }
        return new NecromancerCurrentBuildFormData(additional,
                "true".equals(fields.get("targetVulnerable")), fields.getOrDefault("selectedSkillId", saved.selectedSkillId()));
    }

    public double parsedAdditionalNonItemIntelligence() {
        try {
            double value = additionalNonItemIntelligence.isBlank() ? 0.0d : Double.parseDouble(additionalNonItemIntelligence.replace(',', '.'));
            if (!Double.isFinite(value) || value < 0.0d) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Dodatkowa Inteligencja poza baseline'em i aktywnymi itemami musi być skończoną liczbą nieujemną.");
        }
    }

    public List<String> validationErrors() {
        List<String> errors = new ArrayList<>();
        try { parsedAdditionalNonItemIntelligence(); } catch (IllegalArgumentException exception) { errors.add(exception.getMessage()); }
        if (!selectedSkillId.isEmpty() && !"krwotok".equals(selectedSkillId)) {
            errors.add("N11 obsługuje wyłącznie Krwotok R1.");
        }
        return List.copyOf(errors);
    }

    public static List<String> postValidationErrors(Map<String, String> fields) {
        List<String> errors = new ArrayList<>();
        if (fields.containsKey("additionalNonItemIntelligence") && fields.containsKey("nonItemIntelligence")) {
            errors.add("Prześlij wyłącznie dodatkową Intelligence albo historyczne pole N11, nie oba naraz.");
        }
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
