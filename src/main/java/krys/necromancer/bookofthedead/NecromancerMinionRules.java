package krys.necromancer.bookofthedead;

import java.math.BigDecimal;
import java.util.Objects;

/** Globalne metadane minionów, bez obliczania Lucky Hit, dziedziczenia lub liczby przywołań. */
public record NecromancerMinionRules(
        BigDecimal baseLuckyHitChancePercent,
        BigDecimal usefulPlayerStatsAndBuffsInheritancePercent,
        String inheritanceDescription,
        String summoningWithSacrificeRequirement,
        String sourceReference) {

    public NecromancerMinionRules {
        Objects.requireNonNull(baseLuckyHitChancePercent, "baseLuckyHitChancePercent");
        Objects.requireNonNull(usefulPlayerStatsAndBuffsInheritancePercent, "inheritancePercent");
        BookOfDeadSourceFact.requireText(inheritanceDescription, "inheritanceDescription");
        BookOfDeadSourceFact.requireText(summoningWithSacrificeRequirement, "summoningWithSacrificeRequirement");
        BookOfDeadSourceFact.requireText(sourceReference, "sourceReference");
    }
}
