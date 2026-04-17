package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.SavedImportedItem;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
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
                .replace("{{ACTIVE_LIBRARY_SECTION}}", renderActiveLibrarySection(model))
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
        html.append(renderSummaryCard("Weapon damage effective", Long.toString(calculation.getRequest().getWeaponDamage())));
        html.append(renderSummaryCard("Strength effective", String.format(Locale.US, "%.0f", calculation.getRequest().getStrength())));
        html.append(renderSummaryCard("Intelligence effective", String.format(Locale.US, "%.0f", calculation.getRequest().getIntelligence())));
        html.append(renderSummaryCard("Horyzont", calculation.getRequest().getHorizonSeconds() + " s"));
        html.append(renderSummaryCard("Action bar", CurrentBuildCalculationSectionsRenderer.buildActionBarLabel(calculation.getRequest().getActionBar())));
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
        html.append(CurrentBuildCalculationSectionsRenderer.renderDirectHitDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderDelayedHitDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderReactiveDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderStepTrace(calculation));
        return html.toString();
    }

    private static String renderActiveLibrarySection(CurrentBuildPageModel model) {
        StringBuilder html = new StringBuilder("""
                <section class="subpanel">
                    <h3>Aktywne itemy z biblioteki</h3>
                    <p class="helper">Poniższe itemy są dodawane do ręcznej bazy z formularza. Biblioteka itemów pozostaje warstwą aplikacyjną nad current build i nie buduje alternatywnego runtime.</p>
                    <div class="hero-links">
                        <a class="nav-link" href=\"""")
                .append(escapeHtml(model.getItemLibraryUrl()))
                .append("\">Otwórz bibliotekę itemów</a>")
                .append("""
                    </div>
                """);

        if (!model.hasActiveLibraryItems()) {
            html.append("<p class=\"helper\">Brak aktywnych itemów w bibliotece. Formularz current build działa wyłącznie na ręcznej bazie.</p>");
            html.append("</section>");
            return html.toString();
        }

        CurrentBuildImportableStats contribution = model.getActiveLibraryContribution();
        html.append("<div class=\"summary-grid compact-grid\">")
                .append(renderSummaryCard("Weapon damage z biblioteki", Long.toString(contribution.getWeaponDamage())))
                .append(renderSummaryCard("Strength z biblioteki", formatWhole(contribution.getStrength())))
                .append(renderSummaryCard("Intelligence z biblioteki", formatWhole(contribution.getIntelligence())))
                .append(renderSummaryCard("Thorns z biblioteki", formatWhole(contribution.getThorns())))
                .append(renderSummaryCard("Block chance z biblioteki [%]", formatWhole(contribution.getBlockChance())))
                .append(renderSummaryCard("Retribution chance z biblioteki [%]", formatWhole(contribution.getRetributionChance())))
                .append("</div>")
                .append("<table class=\"data-table\"><thead><tr><th>Slot</th><th>Item</th><th>Źródło</th><th>Wkład</th></tr></thead><tbody>");
        for (SavedImportedItem item : model.getActiveLibraryItems()) {
            html.append("<tr><td>")
                    .append(escapeHtml(item.getSlot().name()))
                    .append("</td><td>")
                    .append(escapeHtml(item.getDisplayName()))
                    .append("</td><td>")
                    .append(escapeHtml(item.getSourceImageName()))
                    .append("</td><td>")
                    .append(escapeHtml(buildItemContributionLabel(item)))
                    .append("</td></tr>");
        }
        html.append("</tbody></table>");
        if (model.getEffectiveStats() != null) {
            CurrentBuildImportableStats effectiveStats = model.getEffectiveStats();
            html.append("<p class=\"helper\">Effective current build do runtime: weapon damage=")
                    .append(escapeHtml(Long.toString(effectiveStats.getWeaponDamage())))
                    .append(", strength=")
                    .append(escapeHtml(formatWhole(effectiveStats.getStrength())))
                    .append(", intelligence=")
                    .append(escapeHtml(formatWhole(effectiveStats.getIntelligence())))
                    .append(", thorns=")
                    .append(escapeHtml(formatWhole(effectiveStats.getThorns())))
                    .append(", block chance=")
                    .append(escapeHtml(formatWhole(effectiveStats.getBlockChance())))
                    .append(", retribution chance=")
                    .append(escapeHtml(formatWhole(effectiveStats.getRetributionChance())))
                    .append(".</p>");
        }
        html.append("</section>");
        return html.toString();
    }

    private static String renderSummaryCard(String label, String value) {
        return CurrentBuildCalculationSectionsRenderer.renderSummaryCard(label, value);
    }

    private static String renderBuildStatsFields(CurrentBuildFormData formData) {
        return """
                <label>
                    Level bohatera
                    <input type="number" min="1" step="1" name="level" value="{{LEVEL}}">
                </label>
                <label>
                    Weapon damage
                    <input type="number" min="1" step="1" name="weaponDamage" value="{{WEAPON_DAMAGE}}">
                </label>
                <label>
                    Strength z itemów
                    <input type="number" min="0" step="1" name="strength" value="{{STRENGTH}}">
                </label>
                <label>
                    Intelligence z itemów
                    <input type="number" min="0" step="1" name="intelligence" value="{{INTELLIGENCE}}">
                </label>
                <label>
                    Thorns
                    <input type="number" min="0" step="1" name="thorns" value="{{THORNS}}">
                </label>
                <label>
                    Block chance [%]
                    <input type="number" min="0" step="0.01" name="blockChance" value="{{BLOCK_CHANCE}}">
                </label>
                <label>
                    Retribution chance [%]
                    <input type="number" min="0" step="0.01" name="retributionChance" value="{{RETRIBUTION_CHANCE}}">
                </label>
                <label>
                    Horyzont symulacji [s]
                    <input type="number" min="1" step="1" name="horizonSeconds" value="{{HORIZON_SECONDS}}">
                </label>
                """
                .replace("{{LEVEL}}", escapeHtml(formData.getLevel()))
                .replace("{{WEAPON_DAMAGE}}", escapeHtml(formData.getWeaponDamage()))
                .replace("{{STRENGTH}}", escapeHtml(formData.getStrength()))
                .replace("{{INTELLIGENCE}}", escapeHtml(formData.getIntelligence()))
                .replace("{{THORNS}}", escapeHtml(formData.getThorns()))
                .replace("{{BLOCK_CHANCE}}", escapeHtml(formData.getBlockChance()))
                .replace("{{RETRIBUTION_CHANCE}}", escapeHtml(formData.getRetributionChance()))
                .replace("{{HORIZON_SECONDS}}", escapeHtml(formData.getHorizonSeconds()));
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

    private static String escapeHtml(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }

    private static String formatWhole(double value) {
        return String.format(Locale.US, "%.0f", value);
    }

    private static String buildItemContributionLabel(SavedImportedItem item) {
        List<String> parts = new ArrayList<>();
        if (item.getWeaponDamage() > 0L) {
            parts.add("weapon=" + item.getWeaponDamage());
        }
        if (item.getStrength() > 0.0d) {
            parts.add("str=" + formatWhole(item.getStrength()));
        }
        if (item.getIntelligence() > 0.0d) {
            parts.add("int=" + formatWhole(item.getIntelligence()));
        }
        if (item.getThorns() > 0.0d) {
            parts.add("thorns=" + formatWhole(item.getThorns()));
        }
        if (item.getBlockChance() > 0.0d) {
            parts.add("block=" + formatWhole(item.getBlockChance()) + "%");
        }
        if (item.getRetributionChance() > 0.0d) {
            parts.add("retribution=" + formatWhole(item.getRetributionChance()) + "%");
        }
        return parts.isEmpty() ? "Brak wkładu do current build" : String.join(", ", parts);
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
