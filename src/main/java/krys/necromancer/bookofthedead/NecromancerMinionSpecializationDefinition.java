package krys.necromancer.bookofthedead;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Pełna, niezmienna specjalizacja Księgi; nie zawiera stanu wyboru bohatera. */
public record NecromancerMinionSpecializationDefinition(
        NecromancerMinionSpecializationId id,
        String displayName,
        String canonicalName,
        int unlockLevel,
        NecromancerMinionTheme theme,
        String passiveDescription,
        List<BookOfDeadSourceFact> passiveFacts,
        List<BookOfDeadChoiceDefinition> choices,
        Optional<String> additionalUnlockRequirement,
        String sourceReference,
        BookOfDeadRuntimeStatus runtimeStatus) {

    public NecromancerMinionSpecializationDefinition {
        Objects.requireNonNull(id, "id");
        BookOfDeadSourceFact.requireText(displayName, "displayName");
        BookOfDeadSourceFact.requireText(canonicalName, "canonicalName");
        if (unlockLevel < 1) {
            throw new IllegalArgumentException("Poziom odblokowania musi być dodatni");
        }
        Objects.requireNonNull(theme, "theme");
        BookOfDeadSourceFact.requireText(passiveDescription, "passiveDescription");
        passiveFacts = BookOfDeadSourceFact.copyFacts(passiveFacts);
        if (passiveFacts.isEmpty()) {
            throw new IllegalArgumentException("Brak faktów pasywu / zdolności bazowej");
        }
        Objects.requireNonNull(additionalUnlockRequirement, "additionalUnlockRequirement");
        additionalUnlockRequirement.ifPresent(value -> BookOfDeadSourceFact.requireText(value, "unlockRequirement"));
        BookOfDeadSourceFact.requireText(sourceReference, "sourceReference");
        Objects.requireNonNull(runtimeStatus, "runtimeStatus");
        var byType = new EnumMap<BookOfDeadChoiceType, BookOfDeadChoiceDefinition>(BookOfDeadChoiceType.class);
        for (var choice : List.copyOf(choices)) {
            if (byType.putIfAbsent(choice.type(), choice) != null) {
                throw new IllegalArgumentException("Powtórzony typ wyboru: " + choice.type());
            }
            for (var conflict : choice.sourceConflicts()) {
                var fact = choice.effects().stream().filter(effect -> effect.id().equals(conflict.factId()))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Konflikt bez faktu źródłowego"));
                if (fact.value() == null || fact.value().compareTo(conflict.canonicalValue()) != 0) {
                    throw new IllegalArgumentException("Konflikt nie zgadza się z wartością katalogową");
                }
            }
        }
        if (byType.size() != BookOfDeadChoiceType.values().length) {
            throw new IllegalArgumentException("Specjalizacja wymaga Upgrade #1, Upgrade #2 i Sacrifice");
        }
        var penalty = byType.get(BookOfDeadChoiceType.SACRIFICE).summonPenalty().orElseThrow();
        var expected = id.family() == NecromancerMinionFamily.GOLEM
                ? BookOfDeadSummonPenalty.Type.GOLEM_DAMAGE_REDUCTION_MULTIPLICATIVE
                : BookOfDeadSummonPenalty.Type.SUMMON_COUNT_REDUCTION;
        if (penalty.type() != expected) {
            throw new IllegalArgumentException("Kara Sacrifice nie odpowiada rodzinie specjalizacji");
        }
        choices = byType.values().stream().sorted(Comparator.comparing(BookOfDeadChoiceDefinition::type)).toList();
    }

    public NecromancerMinionFamily family() {
        return id.family();
    }

    public BookOfDeadChoiceDefinition choice(BookOfDeadChoiceType type) {
        Objects.requireNonNull(type, "type");
        return choices.stream().filter(choice -> choice.type() == type).findFirst().orElseThrow();
    }
}
