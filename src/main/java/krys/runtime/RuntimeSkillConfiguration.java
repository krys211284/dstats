package krys.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Niemutowalna konfiguracja pojedynczej umiejętności; sloty wyboru nie mają tu znaczenia domenowego. */
public record RuntimeSkillConfiguration(RuntimeSkillKey skillKey, int rank, Map<String, String> selections) {
    private static final Pattern TECHNICAL_ID = Pattern.compile("[a-z0-9][a-z0-9_-]*");

    public RuntimeSkillConfiguration {
        Objects.requireNonNull(skillKey, "skillKey");
        if (rank < 0) {
            throw new IllegalArgumentException("Ranga konfiguracji musi być nieujemna");
        }
        selections = Map.copyOf(Objects.requireNonNull(selections, "selections"));
        for (var selection : selections.entrySet()) {
            validateTechnicalId(selection.getKey(), "selectionSlotId");
            validateTechnicalId(selection.getValue(), "selectedOptionId");
        }
        if (rank == 0 && !selections.isEmpty()) {
            throw new IllegalArgumentException("Nienauczona umiejętność nie może mieć wyborów");
        }
    }

    public boolean isLearned() {
        return rank > 0;
    }

    /** Brak wpisu oznacza brak wyboru; lookup nie normalizuje identyfikatora slotu. */
    public Optional<String> selectedOption(String selectionSlotId) {
        validateTechnicalId(selectionSlotId, "selectionSlotId");
        return Optional.ofNullable(selections.get(selectionSlotId));
    }

    private static void validateTechnicalId(String value, String field) {
        Objects.requireNonNull(value, field);
        if (!TECHNICAL_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("Pole " + field + " wymaga technicznego id [a-z0-9][a-z0-9_-]*");
        }
    }
}
