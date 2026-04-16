package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.combat.DamageBreakdown;
import krys.combat.DamageComponentBreakdown;
import krys.combat.DelayedHitBreakdown;
import krys.combat.ReactiveHitBreakdown;
import krys.simulation.SimulationStepTrace;
import krys.simulation.SkillBarStateTrace;
import krys.simulation.SkillHitDebugSnapshot;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Wspólny renderer sekcji wyniku runtime używany przez manual simulation i drill-down searcha. */
final class CurrentBuildCalculationSectionsRenderer {
    private CurrentBuildCalculationSectionsRenderer() {
    }

    static String renderSummaryCard(String label, String value) {
        return """
                <article class="summary-card">
                    <div class="summary-label">""" + escapeHtml(label) + """
                </div>
                    <div class="summary-value">""" + escapeHtml(value) + """
                </div>
                </article>
                """;
    }

    static String renderDirectHitDebug(CurrentBuildCalculation calculation) {
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Direct hit debug</h2>
                    <p class="muted">Debug bezpośrednich hitów</p>
                """);
        if (calculation.getResult().getDirectHitDebugSnapshots().isEmpty()) {
            html.append("<p>Brak bezpośrednich hitów w bieżącej symulacji.</p></section>");
            return html.toString();
        }

        for (SkillHitDebugSnapshot debugSnapshot : calculation.getResult().getDirectHitDebugSnapshots()) {
            DamageBreakdown breakdown = debugSnapshot.getBreakdown();
            html.append("""
                    <article class="subpanel">
                        <h3>""").append(escapeHtml(debugSnapshot.getSkillName())).append("</h3>")
                    .append("""
                        <div class="summary-grid compact-grid">
                    """)
                    .append(renderSummaryCard("Raw hit", Long.toString(breakdown.getRawDamage())))
                    .append(renderSummaryCard("Single hit", Long.toString(breakdown.getFinalDamage())))
                    .append(renderSummaryCard("Raw crit hit", Long.toString(breakdown.getRawCriticalDamage())))
                    .append(renderSummaryCard("Critical hit", Long.toString(breakdown.getCriticalDamage())))
                    .append("""
                        </div>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Komponent</th>
                                    <th>Źródło</th>
                                    <th>%</th>
                                    <th>Raw</th>
                                    <th>Final</th>
                                    <th>Single target</th>
                                </tr>
                            </thead>
                            <tbody>
                    """);
            for (DamageComponentBreakdown component : breakdown.getComponents()) {
                html.append("<tr>")
                        .append("<td>").append(escapeHtml(component.getName())).append("</td>")
                        .append("<td>").append(escapeHtml(component.getSource())).append("</td>")
                        .append("<td>").append(component.getSkillDamagePercent()).append("</td>")
                        .append("<td>").append(component.getRawDamage()).append("</td>")
                        .append("<td>").append(component.getFinalDamage()).append("</td>")
                        .append("<td>").append(component.isIncludedInSingleTarget() ? "Tak" : "Nie").append("</td>")
                        .append("</tr>");
            }
            html.append("""
                            </tbody>
                        </table>
                    </article>
                    """);
        }
        html.append("</section>");
        return html.toString();
    }

    static String renderDelayedHitDebug(CurrentBuildCalculation calculation) {
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Delayed hit debug</h2>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Nazwa</th>
                                <th>Source</th>
                                <th>Apply</th>
                                <th>Trigger</th>
                                <th>Status</th>
                                <th>Raw</th>
                                <th>Final</th>
                            </tr>
                        </thead>
                        <tbody>
                """);
        if (calculation.getResult().getDelayedHitBreakdowns().isEmpty()) {
            html.append("""
                    <tr>
                        <td colspan="7">Brak delayed hitów.</td>
                    </tr>
                    """);
        } else {
            for (DelayedHitBreakdown entry : calculation.getResult().getDelayedHitBreakdowns()) {
                String status = entry.isDetonated()
                        ? "Detonował w t=" + entry.getDetonatedSecond()
                        : "Pozostał aktywny do końca horyzontu";
                String raw = entry.getBreakdown() == null ? "-" : Long.toString(entry.getBreakdown().getRawDamage());
                String fin = entry.getBreakdown() == null ? "-" : Long.toString(entry.getBreakdown().getFinalDamage());
                html.append("<tr>")
                        .append("<td>").append(escapeHtml(entry.getDelayedHitName())).append("</td>")
                        .append("<td>").append(escapeHtml(entry.getSourceSkillName())).append("</td>")
                        .append("<td>t=").append(entry.getAppliedSecond()).append("</td>")
                        .append("<td>t=").append(entry.getTriggerSecond()).append("</td>")
                        .append("<td>").append(escapeHtml(status)).append("</td>")
                        .append("<td>").append(escapeHtml(raw)).append("</td>")
                        .append("<td>").append(escapeHtml(fin)).append("</td>")
                        .append("</tr>");
            }
        }
        html.append("""
                        </tbody>
                    </table>
                </section>
                """);
        return html.toString();
    }

    static String renderReactiveDebug(CurrentBuildCalculation calculation) {
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Reactive debug</h2>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Sekunda</th>
                                <th>Resolve</th>
                                <th>Active block chance</th>
                                <th>Active thorns bonus</th>
                                <th>Thorns raw</th>
                                <th>Thorns final</th>
                                <th>Retribution expected raw</th>
                                <th>Retribution expected final</th>
                                <th>Reactive final</th>
                            </tr>
                        </thead>
                        <tbody>
                """);
        if (calculation.getResult().getReactiveHitBreakdowns().isEmpty()) {
            html.append("""
                    <tr>
                        <td colspan="9">Brak reactive damage.</td>
                    </tr>
                    """);
        } else {
            for (ReactiveHitBreakdown entry : calculation.getResult().getReactiveHitBreakdowns()) {
                html.append("<tr>")
                        .append("<td>t=").append(entry.getTriggeredSecond()).append("</td>")
                        .append("<td>").append(entry.isResolveActive() ? "Tak (" + entry.getResolveRemainingSeconds() + " s)" : "Nie").append("</td>")
                        .append("<td>").append(escapeHtml(String.format(Locale.US, "%.2f%%", entry.getActiveBlockChance() * 100.0d))).append("</td>")
                        .append("<td>").append(escapeHtml(String.format(Locale.US, "%.0f", entry.getActiveThornsBonus()))).append("</td>")
                        .append("<td>").append(entry.getThornsRawDamage()).append("</td>")
                        .append("<td>").append(entry.getThornsFinalDamage()).append("</td>")
                        .append("<td>").append(entry.getRetributionExpectedRawDamage()).append("</td>")
                        .append("<td>").append(entry.getRetributionExpectedFinalDamage()).append("</td>")
                        .append("<td>").append(entry.getReactiveFinalDamage()).append("</td>")
                        .append("</tr>");
            }
            html.append("<tr><td colspan=\"8\"><strong>Reactive total</strong></td><td><strong>")
                    .append(calculation.getResult().getTotalReactiveDamage())
                    .append("</strong></td></tr>");
        }
        html.append("""
                        </tbody>
                    </table>
                </section>
                """);
        return html.toString();
    }

    static String renderStepTrace(CurrentBuildCalculation calculation) {
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Step trace</h2>
                    <table class="data-table trace-table">
                        <thead>
                            <tr>
                                <th>Sekunda</th>
                                <th>Akcja</th>
                                <th>Direct</th>
                                <th>Delayed</th>
                                <th>Reactive</th>
                                <th>Krok</th>
                                <th>Cumulative</th>
                                <th>Tick order</th>
                                <th>Selection reason</th>
                                <th>Stan paska</th>
                            </tr>
                        </thead>
                        <tbody>
                """);
        for (SimulationStepTrace step : calculation.getResult().getStepTrace()) {
            html.append("<tr>")
                    .append("<td>").append(step.getSecond()).append("</td>")
                    .append("<td>").append(escapeHtml(step.getActionName())).append("</td>")
                    .append("<td>").append(step.getDirectDamage()).append("</td>")
                    .append("<td>").append(step.getDelayedDamage()).append("</td>")
                    .append("<td>").append(step.getReactiveDamage()).append("</td>")
                    .append("<td>").append(step.getTotalStepDamage()).append("</td>")
                    .append("<td>").append(step.getCumulativeDamage()).append("</td>")
                    .append("<td>").append(escapeHtml(step.getTickOrderLabel())).append("</td>")
                    .append("<td>").append(escapeHtml(step.getSelectionReason())).append("</td>")
                    .append("<td>").append(renderSkillBarStates(step.getSkillBarStates())).append("</td>")
                    .append("</tr>");
        }
        html.append("""
                        </tbody>
                    </table>
                </section>
                """);
        return html.toString();
    }

    static String buildActionBarLabel(List<SkillId> actionBar) {
        if (actionBar.isEmpty()) {
            return "Pusty";
        }
        List<String> labels = new ArrayList<>();
        for (SkillId skillId : actionBar) {
            labels.add(PaladinSkillDefs.get(skillId).getName());
        }
        return String.join(" -> ", labels);
    }

    static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String renderSkillBarStates(List<SkillBarStateTrace> barStates) {
        if (barStates.isEmpty()) {
            return "<span class=\"muted\">Pusty pasek</span>";
        }

        StringBuilder html = new StringBuilder("<div class=\"bar-state-list\">");
        for (SkillBarStateTrace barState : barStates) {
            html.append("<div class=\"bar-state-item\">")
                    .append("<strong>").append(escapeHtml(barState.getSkillName())).append("</strong>")
                    .append(" | rank=").append(barState.getRank())
                    .append(" | legal=").append(barState.isLegalActive())
                    .append(" | cooldown=").append(barState.isOnCooldown())
                    .append(" | cooldownRemaining=").append(barState.getCooldownRemainingSeconds())
                    .append(" | resource=").append(barState.hasRequiredResource())
                    .append(" | neverUsed=").append(barState.isNeverUsed())
                    .append(" | lastUsed=").append(barState.getLastUsedSecond() == null ? "-" : barState.getLastUsedSecond())
                    .append(" | selected=").append(barState.isSelected())
                    .append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }
}
