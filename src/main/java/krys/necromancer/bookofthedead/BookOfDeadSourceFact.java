package krys.necromancer.bookofthedead;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Fakt opisowy. Jednostka określa znaczenie liczby, lecz nie wykonuje żadnego przeliczenia. */
public record BookOfDeadSourceFact(String id, String description, BigDecimal value, Unit unit) {
    public enum Unit {
        TEXT, SOURCE_DEPENDENT_AMOUNT,
        COUNT, SECONDS, FLAT_AMOUNT, PERCENT, PERCENT_ADDITIVE, PERCENT_MULTIPLICATIVE,
        PERCENT_CHANCE, PERCENT_OF_NORMAL_DAMAGE, PERCENT_OF_MAXIMUM_LIFE,
        PERCENT_OF_THORNS, SOURCE_DAMAGE_PERCENT, SOURCE_DAMAGE_PERCENT_MULTIPLICATIVE,
        PERCENT_PER_SECOND
    }

    public BookOfDeadSourceFact {
        requireText(id, "id");
        requireText(description, "description");
        Objects.requireNonNull(unit, "unit");
        boolean descriptive = unit == Unit.TEXT || unit == Unit.SOURCE_DEPENDENT_AMOUNT;
        if (descriptive != (value == null)) {
            throw new IllegalArgumentException("Fakt opisowy wymaga braku liczby, a liczbowy jawnej wartości");
        }
    }

    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Puste pole: " + field);
        }
        return value;
    }

    static List<BookOfDeadSourceFact> copyFacts(List<BookOfDeadSourceFact> facts) {
        List<BookOfDeadSourceFact> copy = List.copyOf(facts);
        var ids = new HashSet<String>();
        for (var fact : copy) {
            if (!ids.add(fact.id())) {
                throw new IllegalArgumentException("Powtórzony identyfikator faktu: " + fact.id());
            }
        }
        return copy;
    }
}
