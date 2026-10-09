package krys.necromancer.bookofthedead;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Niewykonywalny wybór Book; Sacrifice posiada osobny bonus gracza i karę dla summonów. */
public record BookOfDeadChoiceDefinition(
        BookOfDeadChoiceType type,
        String description,
        List<BookOfDeadSourceFact> effects,
        List<BookOfDeadSourceFact> playerBonuses,
        Optional<BookOfDeadSummonPenalty> summonPenalty,
        List<SourceConflict> sourceConflicts) {

    public BookOfDeadChoiceDefinition {
        Objects.requireNonNull(type, "type");
        BookOfDeadSourceFact.requireText(description, "description");
        effects = BookOfDeadSourceFact.copyFacts(effects);
        playerBonuses = BookOfDeadSourceFact.copyFacts(playerBonuses);
        Objects.requireNonNull(summonPenalty, "summonPenalty");
        sourceConflicts = List.copyOf(sourceConflicts);
        if (type == BookOfDeadChoiceType.SACRIFICE) {
            if (playerBonuses.isEmpty() || summonPenalty.isEmpty() || !effects.isEmpty()) {
                throw new IllegalArgumentException("Sacrifice wymaga bonusów gracza i osobnej kary summonów");
            }
        } else if (effects.isEmpty() || !playerBonuses.isEmpty() || summonPenalty.isPresent()) {
            throw new IllegalArgumentException("Ulepszenie wymaga efektów i nie może zawierać Sacrifice");
        }
    }

    /** Rozstrzygnięcie konkretnej różnicy źródeł, bez flagi manual-review. */
    public record SourceConflict(
            String factId,
            BigDecimal displayedValue,
            String displayedSourceReference,
            BigDecimal canonicalValue,
            String officialSourceReference,
            Resolution status,
            String explanation) {
        public SourceConflict {
            BookOfDeadSourceFact.requireText(factId, "factId");
            Objects.requireNonNull(displayedValue, "displayedValue");
            Objects.requireNonNull(canonicalValue, "canonicalValue");
            BookOfDeadSourceFact.requireText(displayedSourceReference, "displayedSourceReference");
            BookOfDeadSourceFact.requireText(officialSourceReference, "officialSourceReference");
            Objects.requireNonNull(status, "status");
            BookOfDeadSourceFact.requireText(explanation, "explanation");
        }
    }

    public enum Resolution {
        OFFICIAL_SOURCE_RESOLVED
    }
}
