package krys.runtime;

import java.util.Objects;
import java.util.regex.Pattern;

/** Neutralna, niemutowalna tożsamość: namespace klasy i techniczny identyfikator umiejętności. */
public record RuntimeSkillKey(String classId, String skillId) {
    private static final Pattern TECHNICAL_ID = Pattern.compile("[a-z0-9][a-z0-9_-]*");

    public RuntimeSkillKey {
        validateTechnicalId(classId, "classId");
        validateTechnicalId(skillId, "skillId");
    }

    /** Stabilny format zewnętrzny; samo API nie zapisuje klucza do żadnego repozytorium. */
    public String asExternalId() {
        return classId + ":" + skillId;
    }

    /** Odrzuca niepoprawne dane bez przycinania ani zmiany wielkości liter. */
    public static RuntimeSkillKey parse(String externalId) {
        Objects.requireNonNull(externalId, "externalId");
        int separator = externalId.indexOf(':');
        if (separator <= 0 || separator != externalId.lastIndexOf(':') || separator == externalId.length() - 1) {
            throw new IllegalArgumentException("Klucz wymaga dokładnie formatu classId:skillId");
        }
        return new RuntimeSkillKey(externalId.substring(0, separator), externalId.substring(separator + 1));
    }

    private static void validateTechnicalId(String value, String field) {
        Objects.requireNonNull(value, field);
        if (!TECHNICAL_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("Pole " + field + " wymaga technicznego id [a-z0-9][a-z0-9_-]*");
        }
    }
}
