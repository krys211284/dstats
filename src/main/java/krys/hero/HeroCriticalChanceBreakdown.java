package krys.hero;

import java.math.BigDecimal;

/** Rozbicie szansy trafienia krytycznego w UI; nie zawiera niezweryfikowanego wzoru z Inteligencji. */
public final class HeroCriticalChanceBreakdown {
    private final BigDecimal baseCriticalChancePercent;
    private final BigDecimal criticalChanceFromIntelligencePercent;
    private final BigDecimal criticalChanceFromItemsPercent;
    private final BigDecimal criticalChanceFromOtherSourcesPercent;

    public HeroCriticalChanceBreakdown(BigDecimal baseCriticalChancePercent,
                                       BigDecimal criticalChanceFromIntelligencePercent,
                                       BigDecimal criticalChanceFromItemsPercent,
                                       BigDecimal criticalChanceFromOtherSourcesPercent) {
        this.baseCriticalChancePercent = baseCriticalChancePercent;
        this.criticalChanceFromIntelligencePercent = criticalChanceFromIntelligencePercent;
        this.criticalChanceFromItemsPercent = criticalChanceFromItemsPercent;
        this.criticalChanceFromOtherSourcesPercent = criticalChanceFromOtherSourcesPercent;
    }

    public BigDecimal getBaseCriticalChancePercent() {
        return baseCriticalChancePercent;
    }

    public BigDecimal getCriticalChanceFromIntelligencePercent() {
        return criticalChanceFromIntelligencePercent;
    }

    public BigDecimal getCriticalChanceFromItemsPercent() {
        return criticalChanceFromItemsPercent;
    }

    public BigDecimal getCriticalChanceFromOtherSourcesPercent() {
        return criticalChanceFromOtherSourcesPercent;
    }

    public BigDecimal getTotalCriticalChancePercent() {
        return baseCriticalChancePercent
                .add(criticalChanceFromIntelligencePercent)
                .add(criticalChanceFromItemsPercent)
                .add(criticalChanceFromOtherSourcesPercent);
    }
}
