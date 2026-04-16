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
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
                .replace("{{BUILD_STATS_FIELDS}}", renderBuildStatsFields(model.getFormData()))
                .replace("{{SKILL_CONFIG_FIELDS}}", renderSkillConfigFields(model.getFormData()))
                .replace("{{ACTION_BAR_FIELDS}}", renderActionBarFields(model.getFormData()))
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
        html.append(renderSummaryCard("Level", Integer.toString(calculation.getRequest().getLevel())));
        html.append(renderSummaryCard("Weapon damage", Long.toString(calculation.getRequest().getWeaponDamage())));
        html.append(renderSummaryCard("Strength z itemów", String.format(Locale.US, "%.0f", calculation.getRequest().getStrength())));
        html.append(renderSummaryCard("Intelligence z itemów", String.format(Locale.US, "%.0f", calculation.getRequest().getIntelligence())));
        html.append(renderSummaryCard("Horyzont", calculation.getRequest().getHorizonSeconds() + " s"));
        html.append(renderSummaryCard("Action bar", buildActionBarLabel(calculation.getRequest().getActionBar())));
        html.append(renderSummaryCard("Total damage", Long.toString(calculation.getResult().getTotalDamage())));
        html.append(renderSummaryCard("DPS", String.format(Locale.US, "%.4f", calculation.getResult().getDps())));
        html.append(renderSummaryCard("Reactive contribution", Long.toString(calculation.getResult().getTotalReactiveDamage())));
        html.append(renderSummaryCard("Judgement aktywny na końcu", calculation.getResult().isJudgementActiveAtEnd() ? "Tak" : "Nie"));
        html.append(renderSummaryCard("Resolve aktywny na końcu", calculation.getResult().isResolveActiveAtEnd() ? "Tak" : "Nie"));
        html.append(renderSummaryCard("Active block chance na końcu", String.format(Locale.US, "%.2f%%", calculation.getResult().getActiveBlockChanceAtEnd() * 100.0d)));
        html.append(renderSummaryCard("Active thorns bonus na końcu", String.format(Locale.US, "%.0f", calculation.getResult().getActiveThornsBonusAtEnd())));
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

    private static String renderBuildStatsFields(CurrentBuildFormData formData) {
        return """
                <label>
                    Level bohatera
                    <input type="number" min="1" step="1" name="level" value="%s">
                </label>
                <label>
                    Weapon damage
                    <input type="number" min="1" step="1" name="weaponDamage" value="%s">
                </label>
                <label>
                    Strength z itemów
                    <input type="number" min="0" step="1" name="strength" value="%s">
                </label>
                <label>
                    Intelligence z itemów
                    <input type="number" min="0" step="1" name="intelligence" value="%s">
                </label>
                <label>
                    Thorns
                    <input type="number" min="0" step="1" name="thorns" value="%s">
                </label>
                <label>
                    Block chance [%]
                    <input type="number" min="0" step="0.01" name="blockChance" value="%s">
                </label>
                <label>
                    Retribution chance [%]
                    <input type="number" min="0" step="0.01" name="retributionChance" value="%s">
                </label>
                <label>
                    Horyzont symulacji [s]
                    <input type="number" min="1" step="1" name="horizonSeconds" value="%s">
                </label>
                """.formatted(
                escapeHtml(formData.getLevel()),
                escapeHtml(formData.getWeaponDamage()),
                escapeHtml(formData.getStrength()),
                escapeHtml(formData.getIntelligence()),
                escapeHtml(formData.getThorns()),
                escapeHtml(formData.getBlockChance()),
                escapeHtml(formData.getRetributionChance()),
                escapeHtml(formData.getHorizonSeconds())
        );
    }

    private static String renderSkillConfigFields(CurrentBuildFormData formData) {
        StringBuilder html = new StringBuilder();
        for (SkillId skillId : SkillId.values()) {
            CurrentBuildFormData.SkillConfigFormData skillConfig = formData.getSkillConfig(skillId);
            html.append("""
                    <article class="subpanel">
                        <h3>""").append(escapeHtml(PaladinSkillDefs.get(skillId).getName())).append("</h3>")
                    .append("""
                        <div class="form-grid">
                            <label>
                                Rank
                                <select name=\"""").append(CurrentBuildFormData.rankFieldName(skillId)).append("\">")
                    .append(renderRankOptions(skillConfig.getRank()))
                    .append("""
                                </select>
                            </label>
                            <label>
                                Bazowe rozszerzenie
                                <span class="checkbox-row">
                                    <input type="checkbox" name=\"""").append(CurrentBuildFormData.baseUpgradeFieldName(skillId)).append("\" value=\"true\" ")
                    .append(skillConfig.isBaseUpgrade() ? "checked" : "")
                    .append("""
>
                                    Włącz bazowe rozszerzenie
                                </span>
                            </label>
                            <label>
                                Dodatkowy modyfikator
                                <select name=\"""").append(CurrentBuildFormData.choiceFieldName(skillId)).append("\">")
                    .append(renderChoiceOptions(skillId, skillConfig.getChoiceUpgrade()))
                    .append("""
                                </select>
                            </label>
                        </div>
                    </article>
                    """);
        }
        return html.toString();
    }

    private static String renderActionBarFields(CurrentBuildFormData formData) {
        StringBuilder html = new StringBuilder();
        for (int slot = 1; slot <= 4; slot++) {
            html.append("""
                    <label>
                        Slot """).append(slot).append("""
                        <select name=\"""").append(CurrentBuildFormData.actionBarFieldName(slot)).append("\">")
                    .append(renderActionBarOptions(formData.getActionBarSlot(slot)))
                    .append("""
                        </select>
                    </label>
                    """);
        }
        return html.toString();
    }

    private static String renderRankOptions(String selectedRank) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        for (int rank = 0; rank <= 5; rank++) {
            String value = Integer.toString(rank);
            options.add(new CurrentBuildPageModel.SelectOption(value, value, value.equals(selectedRank)));
        }
        return renderOptions(options);
    }

    private static String renderChoiceOptions(SkillId skillId, String selectedChoice) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        options.add(new CurrentBuildPageModel.SelectOption(SkillUpgradeChoice.NONE.name(), "Brak", SkillUpgradeChoice.NONE.name().equals(selectedChoice)));
        for (SkillUpgradeChoice choiceUpgrade : PaladinSkillDefs.get(skillId).getAvailableChoiceUpgrades()) {
            if (choiceUpgrade == SkillUpgradeChoice.NONE) {
                continue;
            }
            options.add(new CurrentBuildPageModel.SelectOption(
                    choiceUpgrade.name(),
                    PaladinSkillDefs.getChoiceDisplayName(skillId, choiceUpgrade),
                    choiceUpgrade.name().equals(selectedChoice)
            ));
        }
        return renderOptions(options);
    }

    private static String renderActionBarOptions(String selectedSkillId) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        options.add(new CurrentBuildPageModel.SelectOption("NONE", "Brak", "NONE".equals(selectedSkillId)));
        for (SkillId skillId : SkillId.values()) {
            options.add(new CurrentBuildPageModel.SelectOption(
                    skillId.name(),
                    PaladinSkillDefs.get(skillId).getName(),
                    skillId.name().equals(selectedSkillId)
            ));
        }
        return renderOptions(options);
    }

    private static String buildActionBarLabel(List<SkillId> actionBar) {
        if (actionBar.isEmpty()) {
            return "Pusty";
        }
        List<String> labels = new ArrayList<>();
        for (SkillId skillId : actionBar) {
            labels.add(PaladinSkillDefs.get(skillId).getName());
        }
        return String.join(" -> ", labels);
    }

    private static String renderReactiveDebug(CurrentBuildCalculation calculation) {
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
            throw new IllegalStateException("Nie udało się wczytać szablonu strony M8", exception);
        }
    }
}
