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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Renderuje pojedynczy szablon HTML dla prostego SSR bez zewnętrznego frameworka webowego. */
public final class CurrentBuildPageRenderer {
    private final String template;

    public CurrentBuildPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(CurrentBuildPageModel model) {
        return template
                .replace("{{SKILL_OPTIONS}}", renderOptions(model.getSkillOptions()))
                .replace("{{RANK_OPTIONS}}", renderOptions(model.getRankOptions()))
                .replace("{{CHOICE_OPTIONS}}", renderOptions(model.getChoiceOptions()))
                .replace("{{BASE_UPGRADE_CHECKED}}", model.getFormData().isBaseUpgrade() ? "checked" : "")
                .replace("{{HORIZON_SECONDS}}", escapeHtml(model.getFormData().getHorizonSeconds()))
                .replace("{{FORM_ERRORS}}", renderErrors(model.getValidationErrors()))
                .replace("{{CHOICE_HELP}}", escapeHtml(model.getChoiceHelpText()))
                .replace("{{RESULT_SECTION}}", renderResultSection(model));
    }

    private static String renderOptions(List<CurrentBuildPageModel.SelectOption> options) {
        StringBuilder html = new StringBuilder();
        for (CurrentBuildPageModel.SelectOption option : options) {
            html.append("<option value=\"")
                    .append(escapeHtml(option.getValue()))
                    .append("\"");
            if (option.isSelected()) {
                html.append(" selected");
            }
            html.append(">")
                    .append(escapeHtml(option.getLabel()))
                    .append("</option>");
        }
        return html.toString();
    }

    private static String renderErrors(List<String> errors) {
        if (errors.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder("""
                <section class="panel panel-error">
                    <h2>Błędy formularza</h2>
                    <ul class="error-list">
                """);
        for (String error : errors) {
            html.append("<li>").append(escapeHtml(error)).append("</li>");
        }
        html.append("""
                    </ul>
                </section>
                """);
        return html.toString();
    }

    private static String renderResultSection(CurrentBuildPageModel model) {
        if (!model.hasResult()) {
            return """
                    <section class="panel result-panel">
                        <h2>Wynik symulacji</h2>
                        <p>To jest aktualny foundation manual simulation dla trybu „Policz aktualny build”. Wypełnij formularz i uruchom obliczenie.</p>
                    </section>
                    """;
        }

        CurrentBuildCalculation calculation = model.getCalculation();
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Wynik symulacji</h2>
                    <div class="summary-grid">
                """);
        html.append(renderSummaryCard("Skill", PaladinSkillDefs.get(calculation.getRequest().getSkillId()).getName()));
        html.append(renderSummaryCard("Rank", Integer.toString(calculation.getRequest().getRank())));
        html.append(renderSummaryCard("Bazowe rozszerzenie", calculation.getRequest().isBaseUpgrade() ? "Tak" : "Nie"));
        html.append(renderSummaryCard("Dodatkowy modyfikator", calculation.getRequest().getChoiceUpgrade().getDisplayName()));
        html.append(renderSummaryCard("Horyzont", calculation.getRequest().getHorizonSeconds() + " s"));
        html.append(renderSummaryCard("Total damage", Long.toString(calculation.getResult().getTotalDamage())));
        html.append(renderSummaryCard("DPS", String.format(Locale.US, "%.4f", calculation.getResult().getDps())));
        html.append(renderSummaryCard("Reactive contribution", Long.toString(calculation.getResult().getTotalReactiveDamage())));
        html.append(renderSummaryCard("Judgement aktywny na końcu", calculation.getResult().isJudgementActiveAtEnd() ? "Tak" : "Nie"));
        html.append("""
                    </div>
                </section>
                """);
        html.append(renderDirectHitDebug(calculation));
        html.append(renderDelayedHitDebug(calculation));
        html.append(renderReactiveDebug(calculation));
        html.append(renderStepTrace(calculation));
        return html.toString();
    }

    private static String renderSummaryCard(String label, String value) {
        return """
                <article class="summary-card">
                    <div class="summary-label">""" + escapeHtml(label) + """
                </div>
                    <div class="summary-value">""" + escapeHtml(value) + """
                </div>
                </article>
                """;
    }

    private static String renderDirectHitDebug(CurrentBuildCalculation calculation) {
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Debug bezpośrednich hitów</h2>
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

    private static String renderDelayedHitDebug(CurrentBuildCalculation calculation) {
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

    private static String renderReactiveDebug(CurrentBuildCalculation calculation) {
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Reactive debug</h2>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Sekunda</th>
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
                        <td colspan="6">Brak reactive damage.</td>
                    </tr>
                    """);
        } else {
            for (ReactiveHitBreakdown entry : calculation.getResult().getReactiveHitBreakdowns()) {
                html.append("<tr>")
                        .append("<td>t=").append(entry.getTriggeredSecond()).append("</td>")
                        .append("<td>").append(entry.getThornsRawDamage()).append("</td>")
                        .append("<td>").append(entry.getThornsFinalDamage()).append("</td>")
                        .append("<td>").append(entry.getRetributionExpectedRawDamage()).append("</td>")
                        .append("<td>").append(entry.getRetributionExpectedFinalDamage()).append("</td>")
                        .append("<td>").append(entry.getReactiveFinalDamage()).append("</td>")
                        .append("</tr>");
            }
            html.append("<tr><td colspan=\"5\"><strong>Reactive total</strong></td><td><strong>")
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

    private static String renderStepTrace(CurrentBuildCalculation calculation) {
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
                    .append(" | resource=").append(barState.hasRequiredResource())
                    .append(" | neverUsed=").append(barState.isNeverUsed())
                    .append(" | lastUsed=").append(barState.getLastUsedSecond() == null ? "-" : barState.getLastUsedSecond())
                    .append(" | selected=").append(barState.isSelected())
                    .append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String loadTemplate() {
        try (InputStream inputStream = CurrentBuildPageRenderer.class.getResourceAsStream("/templates/current-build.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Brak szablonu /templates/current-build.html");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się wczytać szablonu strony M5a", exception);
        }
    }
}
