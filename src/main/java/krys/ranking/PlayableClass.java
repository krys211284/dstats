package krys.ranking;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Obsługiwane klasy postaci dla ogólnego rankingu obrażeń. */
public enum PlayableClass {
    PALADIN("paladin", "Paladyn");

    private final String queryValue;
    private final String displayName;

    PlayableClass(String queryValue, String displayName) {
        this.queryValue = queryValue;
        this.displayName = displayName;
    }

    public String getQueryValue() {
        return queryValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PlayableClass defaultClass() {
        return PALADIN;
    }

    public static PlayableClass fromQueryValueOrDefault(String value) {
        return fromQueryValue(value).orElse(defaultClass());
    }

    public static Optional<PlayableClass> fromQueryValue(String value) {
        if (value == null || value.isBlank() || "ALL".equals(value)) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(playableClass -> playableClass.queryValue.equals(normalized))
                .findFirst();
    }
}
