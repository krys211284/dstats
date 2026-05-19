package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.combat.DamageBreakdown;
import krys.combat.DamageComponentBreakdown;
import krys.combat.DelayedHitBreakdown;
import krys.combat.ReactiveHitBreakdown;
import krys.simulation.SimulationStepTrace;
import krys.simulation.SkillBarStateTrace;
import krys.simulation.SkillHitDebugSnapshot;
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

    private static String renderSummaryCardWithClasses(String label, String value, String extraClasses) {
        return "<article class=\"summary-card " + escapeHtml(extraClasses) + "\">\n"
                + "    <div class=\"summary-label\">" + escapeHtml(label) + "</div>\n"
                + "    <div class=\"summary-value\">" + escapeHtml(value) + "</div>\n"
                + "</article>\n";
    }

    private static String renderSummaryCardWithHtmlValue(String label, String valueHtml) {
        return "<article class=\"summary-card\">\n"
                + "    <div class=\"summary-label\">" + escapeHtml(label) + "</div>\n"
                + "    <div class=\"summary-value\">" + valueHtml + "</div>\n"
                + "</article>\n";
    }

    static String renderDirectHitDebug(CurrentBuildCalculation calculation) {
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Debug bezpośrednich trafień</h2>
                """);
        if (calculation.getResult().getDirectHitDebugSnapshots().isEmpty()) {
            html.append("<p>Brak bezpośrednich hitów w bieżącej symulacji.</p></section>");
            return html.toString();
        }

        for (SkillHitDebugSnapshot debugSnapshot : calculation.getResult().getDirectHitDebugSnapshots()) {
            DamageBreakdown breakdown = debugSnapshot.getBreakdown();
            html.append("""
                    <article class="subpanel">
                        <h3 title=\"""").append(escapeHtml(debugSnapshot.getSkillId().name()))
                    .append("\" data-skill-id=\"")
                    .append(escapeHtml(debugSnapshot.getSkillId().name()))
                    .append("\">")
                    .append(escapeHtml(HeroSkillCatalogAdapter.displayName(debugSnapshot.getSkillId())))
                    .append("</h3>")
                    .append("""
                        <div class="summary-grid compact-grid">
                    """)
                    .append(renderSummaryCard("Obrażenia broni", Long.toString(calculation.getSnapshot().getAverageWeaponDamage())))
                    .append(renderSummaryCard("Bonus tarczy do obrażeń broni głównej", formatMultiplierWithPercent(breakdown.getWeaponMultiplier())))
                    .append(renderSummaryCard(HeroSkillCatalogAdapter.displayName(debugSnapshot.getSkillId()) + " rank " + debugSnapshot.getSkillRank(),
                            firstComponentPercentLabel(breakdown)))
                    .append(renderSummaryCard("Siła",
                            CurrentBuildNumberFormatter.resource(breakdown.getMainStat())
                                    + " -> +" + CurrentBuildNumberFormatter.percentOneDecimal((breakdown.getMainStatMultiplier() - 1.0d) * 100.0d)
                                    + "% / ×" + CurrentBuildNumberFormatter.multiplier(breakdown.getMainStatMultiplier())))
                    .append(renderVerathielSummaryCard(breakdown))
                    .append(renderSummaryCard("Redukcja poziomu",
                            CurrentBuildNumberFormatter.percentWhole(breakdown.getLevelDamageReduction() * 100.0d)
                                    + "% / zostaje ×" + CurrentBuildNumberFormatter.multiplier(breakdown.getDamageTakenAfterLevelReductionMultiplier())))
                    .append(renderSummaryCard("Surowe trafienie", Long.toString(breakdown.getRawDamage())))
                    .append(renderSummaryCardWithClasses("Trafienie końcowe", Long.toString(breakdown.getFinalDamage()),
                            "damage-result-card damage-final-card"))
                    .append(renderSummaryCard("Surowe trafienie krytyczne", Long.toString(breakdown.getRawCriticalDamage())))
                    .append(renderSummaryCardWithClasses("Trafienie krytyczne", Long.toString(breakdown.getCriticalDamage()),
                            "damage-result-card damage-critical-card"))
                    .append("""
                        </div>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Komponent</th>
                                    <th>Źródło</th>
                                    <th>%</th>
                                    <th>Surowe</th>
                                    <th>Końcowe</th>
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
                    <h2>Debug opóźnionych trafień</h2>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Nazwa</th>
                                <th>Źródło</th>
                                <th>Nałożenie</th>
                                <th>Wyzwolenie</th>
                                <th>Status</th>
                                <th>Surowe</th>
                                <th>Końcowe</th>
                            </tr>
                        </thead>
                        <tbody>
                """);
        if (calculation.getResult().getDelayedHitBreakdowns().isEmpty()) {
            html.append("""
                    <tr>
                        <td colspan="7">Brak opóźnionych trafień.</td>
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
                        .append("<td>").append(escapeHtml(HeroSkillCatalogAdapter.displayName(entry.getSourceSkillName()))).append("</td>")
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
                    <h2>Debug obrażeń reaktywnych</h2>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Sekunda</th>
                                <th>Resolve</th>
                                <th>Aktywna szansa bloku</th>
                                <th>Aktywny bonus do kolców</th>
                                <th>Kolce surowe</th>
                                <th>Kolce końcowe</th>
                                <th>Retribution oczekiwane surowe</th>
                                <th>Retribution oczekiwane końcowe</th>
                                <th>Końcowe reaktywne</th>
                            </tr>
                        </thead>
                        <tbody>
                """);
        if (calculation.getResult().getReactiveHitBreakdowns().isEmpty()) {
            html.append("""
                    <tr>
                        <td colspan="9">Brak obrażeń reaktywnych.</td>
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
            html.append("<tr><td colspan=\"8\"><strong>Łączne obrażenia reaktywne</strong></td><td><strong>")
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
                    <h2>Ślad kroków symulacji</h2>
                    <table class="data-table trace-table">
                        <thead>
                            <tr>
                                <th>Sekunda</th>
                                <th>Akcja</th>
                                <th>Bezpośrednie</th>
                                <th>Opóźnione</th>
                                <th>Reaktywne</th>
                                <th>Krok</th>
                                <th>Narastająco</th>
                                <th>Kolejność ticków</th>
                                <th>Wiara przed</th>
                                <th>Koszt Wiary</th>
                                <th>Generacja Wiary</th>
                                <th>Regeneracja Wiary</th>
                                <th>Wiara po</th>
                                <th>Powód wyboru</th>
                                <th>Stan paska</th>
                            </tr>
                        </thead>
                        <tbody>
                """);
        for (SimulationStepTrace step : calculation.getResult().getStepTrace()) {
            html.append("<tr>")
                    .append("<td>").append(step.getSecond()).append("</td>")
                    .append("<td>").append(escapeHtml(HeroSkillCatalogAdapter.displayName(step.getActionName()))).append("</td>")
                    .append("<td>").append(step.getDirectDamage()).append("</td>")
                    .append("<td>").append(step.getDelayedDamage()).append("</td>")
                    .append("<td>").append(step.getReactiveDamage()).append("</td>")
                    .append("<td>").append(step.getTotalStepDamage()).append("</td>")
                    .append("<td>").append(step.getCumulativeDamage()).append("</td>")
                    .append("<td>").append(escapeHtml(step.getTickOrderLabel())).append("</td>")
                    .append("<td>").append(CurrentBuildNumberFormatter.resource(step.getPrimaryResourceBefore())).append("</td>")
                    .append("<td>").append(CurrentBuildNumberFormatter.resource(step.getPrimaryResourceCost())).append("</td>")
                    .append("<td>").append(CurrentBuildNumberFormatter.resource(step.getPrimaryResourceGenerated())).append("</td>")
                    .append("<td>").append(CurrentBuildNumberFormatter.signedResource(step.getPrimaryResourceRegenerated())).append("</td>")
                    .append("<td>").append(CurrentBuildNumberFormatter.resource(step.getPrimaryResourceAfter())).append("</td>")
                    .append("<td>").append(escapeHtml(HeroSkillCatalogAdapter.replaceRuntimeSkillNames(step.getSelectionReason()))).append("</td>")
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
            labels.add(HeroSkillCatalogAdapter.displayName(skillId));
        }
        return String.join(" -> ", labels);
    }

    private static String firstComponentPercentLabel(DamageBreakdown breakdown) {
        if (breakdown.getComponents().isEmpty()) {
            return "-";
        }
        return breakdown.getComponents().getFirst().getSkillDamagePercent() + "%";
    }

    private static String formatMultiplierWithPercent(double multiplier) {
        double percentBonus = (multiplier - 1.0d) * 100.0d;
        return "+" + CurrentBuildNumberFormatter.percentWhole(percentBonus)
                + "% / ×" + CurrentBuildNumberFormatter.multiplier(multiplier);
    }

    private static String renderVerathielSummaryCard(DamageBreakdown breakdown) {
        if (breakdown.getVerathielBasicSkillMultiplier() <= 1.0d) {
            return renderSummaryCard("Odłamek Verathiela", "Nieaktywny / ×1,00");
        }
        return renderSummaryCardWithHtmlValue(
                "Odłamek Verathiela",
                "+100%[x] / ×" + CurrentBuildNumberFormatter.multiplier(breakdown.getVerathielBasicSkillMultiplier())
                        + "<br><span class=\"summary-subvalue\">Koszt: +25 Wiary</span>"
        );
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
                    .append("<strong title=\"").append(escapeHtml(barState.getSkillId().name()))
                    .append("\" data-skill-id=\"")
                    .append(escapeHtml(barState.getSkillId().name()))
                    .append("\">")
                    .append(escapeHtml(HeroSkillCatalogAdapter.displayName(barState.getSkillId())))
                    .append("</strong>")
                    .append(" | ranga=").append(barState.getRank())
                    .append(" | legalny=").append(barState.isLegalActive() ? "tak" : "nie")
                    .append(" | odnowienie=").append(barState.isOnCooldown() ? "tak" : "nie")
                    .append(" | pozostałe odnowienie=").append(barState.getCooldownRemainingSeconds())
                    .append(" | zasób=").append(barState.hasRequiredResource() ? "tak" : "nie")
                    .append(" | wiara=").append(CurrentBuildNumberFormatter.resource(barState.getCurrentPrimaryResource()))
                    .append(" | koszt wiary=").append(CurrentBuildNumberFormatter.resource(barState.getEffectivePrimaryResourceCost()))
                    .append(" | nieużyty=").append(barState.isNeverUsed() ? "tak" : "nie")
                    .append(" | ostatnio użyty=").append(barState.getLastUsedSecond() == null ? "-" : barState.getLastUsedSecond())
                    .append(" | wybrany=").append(barState.isSelected() ? "tak" : "nie")
                    .append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }
}
