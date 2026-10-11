package krys.hero;

import java.math.BigDecimal;

/** Rozbicie szansy krytycznej w UI z jawną etykietą statystyki źródłowej. */
public final class HeroCriticalChanceBreakdown {
    private final BigDecimal baseCriticalChancePercent;
    private final BigDecimal criticalChanceFromCoreStatPercent;
    private final String coreStatSourceLabel;
    private final BigDecimal criticalChanceFromItemsPercent;
    private final BigDecimal criticalChanceFromOtherSourcesPercent;

    public HeroCriticalChanceBreakdown(BigDecimal baseCriticalChancePercent,
                                       BigDecimal criticalChanceFromCoreStatPercent,
                                       BigDecimal criticalChanceFromItemsPercent,
                                       BigDecimal criticalChanceFromOtherSourcesPercent) {
        this(baseCriticalChancePercent, criticalChanceFromCoreStatPercent, criticalChanceFromItemsPercent,
                criticalChanceFromOtherSourcesPercent, "statystyki bazowej");
    }

    public HeroCriticalChanceBreakdown(BigDecimal baseCriticalChancePercent, BigDecimal criticalChanceFromCoreStatPercent,
                                       BigDecimal criticalChanceFromItemsPercent, BigDecimal criticalChanceFromOtherSourcesPercent,
                                       String coreStatSourceLabel) {
        this.baseCriticalChancePercent = baseCriticalChancePercent;
        this.criticalChanceFromCoreStatPercent = criticalChanceFromCoreStatPercent;
        this.coreStatSourceLabel = coreStatSourceLabel;
        this.criticalChanceFromItemsPercent = criticalChanceFromItemsPercent;
        this.criticalChanceFromOtherSourcesPercent = criticalChanceFromOtherSourcesPercent;
    }

    public BigDecimal getBaseCriticalChancePercent() {
        return baseCriticalChancePercent;
    }

    public BigDecimal getCriticalChanceFromCoreStatPercent() {
        return criticalChanceFromCoreStatPercent;
    }

    public String getCoreStatSourceLabel() { return coreStatSourceLabel; }

    public BigDecimal getCriticalChanceFromItemsPercent() {
        return criticalChanceFromItemsPercent;
    }

    public BigDecimal getCriticalChanceFromOtherSourcesPercent() {
        return criticalChanceFromOtherSourcesPercent;
    }

    public BigDecimal getTotalCriticalChancePercent() {
        return baseCriticalChancePercent
                .add(criticalChanceFromCoreStatPercent)
                .add(criticalChanceFromItemsPercent)
                .add(criticalChanceFromOtherSourcesPercent);
    }
}
