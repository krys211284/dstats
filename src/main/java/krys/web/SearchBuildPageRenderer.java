package krys.web;

import krys.app.CurrentBuildRequest;
import krys.search.BuildSearchAudit;
import krys.search.BuildSearchCandidate;
import krys.search.BuildSearchRankedResult;
import krys.search.BuildSearchRequest;
import krys.search.BuildSearchResult;
import krys.search.BuildSearchSkillSpace;
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

/** Renderuje pojedynczy szablon HTML dla prostego SSR searcha bez alternatywnej logiki backendowej. */
public final class SearchBuildPageRenderer {
    private final String template;

    public SearchBuildPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(SearchBuildPageModel model) {
        return template
                .replace("{{SEARCH_SPACE_FIELDS}}", renderSearchSpaceFields(model.getFormData()))
                .replace("{{SKILL_SPACE_FIELDS}}", renderSkillSpaceFields(model.getFormData()))
                .replace("{{FORM_ERRORS}}", renderErrors(model.getValidationErrors()))
                .replace("{{HELP_TEXT}}", escapeHtml(model.getHelpText()))
                .replace("{{RESULT_SECTION}}", renderResultSection(model));
    }

    private static String renderSearchSpaceFields(SearchBuildFormData formData) {
        return """
                <label>
                    Level values
                    <input type="text" name="levelValues" value="{{LEVEL_VALUES}}">
                </label>
                <label>
                    Weapon damage values
                    <input type="text" name="weaponDamageValues" value="{{WEAPON_DAMAGE_VALUES}}">
                </label>
                <label class="checkbox-label">
                    <span>Tryb biblioteki itemów</span>
                    <span class="checkbox-row">
                        <input type="checkbox" name="useItemLibrary" value="true" {{USE_ITEM_LIBRARY_CHECKED}}>
                        <span>Buduj kandydatów z zapisanych itemów biblioteki i składaj ich wkład do effective current build przed runtime.</span>
                    </span>
                </label>
                <label>
                    Strength values
                    <input type="text" name="strengthValues" value="{{STRENGTH_VALUES}}">
                </label>
                <label>
                    Intelligence values
                    <input type="text" name="intelligenceValues" value="{{INTELLIGENCE_VALUES}}">
                </label>
                <label>
                    Thorns values
                    <input type="text" name="thornsValues" value="{{THORNS_VALUES}}">
                </label>
                <label>
                    Block chance values [%]
                    <input type="text" name="blockChanceValues" value="{{BLOCK_CHANCE_VALUES}}">
                </label>
                <label>
                    Retribution chance values [%]
                    <input type="text" name="retributionChanceValues" value="{{RETRIBUTION_CHANCE_VALUES}}">
                </label>
                <label>
                    Rozmiary action bara
                    <input type="text" name="actionBarSizes" value="{{ACTION_BAR_SIZES}}">
                </label>
                <label>
                    Horyzont symulacji [s]
                    <input type="number" min="1" step="1" name="horizonSeconds" value="{{HORIZON_SECONDS}}">
                </label>
                <label>
                    Top N wyników
                    <input type="number" min="1" step="1" name="topResultsLimit" value="{{TOP_RESULTS_LIMIT}}">
                </label>
                """
                .replace("{{USE_ITEM_LIBRARY_CHECKED}}", formData.isUseItemLibrary() ? "checked" : "")
                .replace("{{LEVEL_VALUES}}", escapeHtml(formData.getLevelValues()))
                .replace("{{WEAPON_DAMAGE_VALUES}}", escapeHtml(formData.getWeaponDamageValues()))
                .replace("{{STRENGTH_VALUES}}", escapeHtml(formData.getStrengthValues()))
                .replace("{{INTELLIGENCE_VALUES}}", escapeHtml(formData.getIntelligenceValues()))
                .replace("{{THORNS_VALUES}}", escapeHtml(formData.getThornsValues()))
                .replace("{{BLOCK_CHANCE_VALUES}}", escapeHtml(formData.getBlockChanceValues()))
                .replace("{{RETRIBUTION_CHANCE_VALUES}}", escapeHtml(formData.getRetributionChanceValues()))
                .replace("{{ACTION_BAR_SIZES}}", escapeHtml(formData.getActionBarSizes()))
                .replace("{{HORIZON_SECONDS}}", escapeHtml(formData.getHorizonSeconds()))
                .replace("{{TOP_RESULTS_LIMIT}}", escapeHtml(formData.getTopResultsLimit()));
    }

    private static String renderSkillSpaceFields(SearchBuildFormData formData) {
        StringBuilder html = new StringBuilder();
        for (SkillId skillId : SkillId.values()) {
            SearchBuildFormData.SkillSearchFormData skillConfig = formData.getSkillConfig(skillId);
            html.append("""
                    <article class="subpanel skill-space">
                        <h3>""").append(escapeHtml(PaladinSkillDefs.get(skillId).getName())).append("</h3>")
                    .append("""
                        <div class="form-grid">
                            <label>
                                Dozwolone ranki
                                <input type="text" name=\"""").append(SearchBuildFormData.rankValuesFieldName(skillId)).append("\" value=\"")
                    .append(escapeHtml(skillConfig.getRankValues()))
                    .append("\">")
                    .append("""
                            </label>
                            <label>
                                Dozwolone base upgrade values
                                <input type="text" name=\"""").append(SearchBuildFormData.baseUpgradeValuesFieldName(skillId)).append("\" value=\"")
                    .append(escapeHtml(skillConfig.getBaseUpgradeValues()))
                    .append("\">")
                    .append("""
                            </label>
                            <label>
                                Dozwolone choice values
                                <input type="text" name=\"""").append(SearchBuildFormData.choiceValuesFieldName(skillId)).append("\" value=\"")
                    .append(escapeHtml(skillConfig.getChoiceValues()))
                    .append("\">")
                    .append("""
                            </label>
                        </div>
                    </article>
                    """);
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

    private static String renderResultSection(SearchBuildPageModel model) {
        if (!model.hasResult()) {
            return """
                    <section class="panel result-panel">
                        <h2>Wynik searcha</h2>
                        <p>To jest minimalne GUI SSR dla flow „Znajdź najlepszy build”. Wypełnij przestrzeń searcha, uruchom backend, sprawdź audit/preflight i przejdź do drill-downu wybranego reprezentanta z top wyników po normalizacji.</p>
                    </section>
                    """;
        }

        BuildSearchResult result = model.getResult();
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Wynik searcha</h2>
                    <div class="summary-grid">
                """);
        html.append(renderSummaryCard("Liczba legalnych kandydatów", Long.toString(result.getAudit().getLegalCandidateCount())));
        html.append(renderSummaryCard("Ocenieni kandydaci", Integer.toString(result.getEvaluatedCandidateCount())));
        html.append(renderSummaryCard("Wyniki po normalizacji", Integer.toString(result.getNormalizedResultCount())));
        html.append(renderSummaryCard("Skala search space", result.getAudit().getSpaceScale().getDisplayName()));
        html.append(renderSummaryCard("Top N", Integer.toString(result.getRequest().getTopResultsLimit())));
        html.append(renderSummaryCard("Horyzont", result.getRequest().getHorizonSeconds() + " s"));
        html.append(renderSummaryCard("Tryb biblioteki itemów", result.getRequest().isUseItemLibrary() ? "Włączony" : "Wyłączony"));
        html.append("</div></section>");
        html.append(renderAuditSection(result.getAudit()));
        html.append(renderSearchSpaceSummary(result.getRequest()));
        html.append(renderTopResults(result));
        return html.toString();
    }

    private static String renderAuditSection(BuildSearchAudit audit) {
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Audit / preflight searcha</h2>
                    <div class="summary-grid">
                """);
        html.append(renderSummaryCard("Liczba legalnych kandydatów", Long.toString(audit.getLegalCandidateCount())));
        html.append(renderSummaryCard("Rozmiar przestrzeni statów", Long.toString(audit.getStatSpaceSize())));
        if (audit.isUsingItemLibrary()) {
            html.append(renderSummaryCard("Rozmiar przestrzeni biblioteki itemów", Long.toString(audit.getItemLibraryCombinationSpaceSize())));
        }
        html.append(renderSummaryCard("Rozmiar przestrzeni skilli", Long.toString(audit.getSkillSpaceSize())));
        html.append(renderSummaryCard("Rozmiar przestrzeni action bara", Long.toString(audit.getActionBarSpaceSize())));
        html.append(renderSummaryCard("Skala search space", audit.getSpaceScale().getDisplayName()));
        html.append("""
                    </div>
                </section>
                """);
        return html.toString();
    }

    private static String renderSearchSpaceSummary(BuildSearchRequest request) {
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Wejściowa przestrzeń searcha</h2>
                    <div class="summary-grid">
                """);
        html.append(renderSummaryCard("Level values", joinIntegers(request.getLevelValues())));
        html.append(renderSummaryCard("Weapon damage values", joinLongs(request.getWeaponDamageValues())));
        html.append(renderSummaryCard("Tryb biblioteki itemów", request.isUseItemLibrary() ? "Włączony" : "Wyłączony"));
        html.append(renderSummaryCard("Strength values", joinWholeDoubles(request.getStrengthValues())));
        html.append(renderSummaryCard("Intelligence values", joinWholeDoubles(request.getIntelligenceValues())));
        html.append(renderSummaryCard("Thorns values", joinWholeDoubles(request.getThornsValues())));
        html.append(renderSummaryCard("Block chance values [%]", joinWholeDoubles(request.getBlockChanceValues())));
        html.append(renderSummaryCard("Retribution chance values [%]", joinWholeDoubles(request.getRetributionChanceValues())));
        html.append(renderSummaryCard("Rozmiary action bara", joinIntegers(request.getActionBarSizes())));
        html.append("</div>");

        html.append("""
                    <div class="subpanel">
                        <h3>Zakresy skilli foundation</h3>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Skill</th>
                                    <th>Rank values</th>
                                    <th>Base upgrade values</th>
                                    <th>Choice values</th>
                                </tr>
                            </thead>
                            <tbody>
                """);
        for (SkillId skillId : SkillId.values()) {
            BuildSearchSkillSpace skillSpace = request.getSkillSpace(skillId);
            html.append("<tr>")
                    .append("<td>").append(escapeHtml(PaladinSkillDefs.get(skillId).getName())).append("</td>")
                    .append("<td>").append(escapeHtml(joinIntegers(skillSpace.getRankValues()))).append("</td>")
                    .append("<td>").append(escapeHtml(joinBooleans(skillSpace.getBaseUpgradeValues()))).append("</td>")
                    .append("<td>").append(escapeHtml(joinChoices(skillSpace.getChoiceUpgradeValues()))).append("</td>")
                    .append("</tr>");
        }
        html.append("""
                            </tbody>
                        </table>
                    </div>
                </section>
                """);
        return html.toString();
    }

    private static String renderTopResults(BuildSearchResult result) {
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Top wyniki po normalizacji</h2>
                """);
        if (result.getTopResults().isEmpty()) {
            html.append("<p>Brak legalnych kandydatów.</p></section>");
            return html.toString();
        }

        for (BuildSearchRankedResult rankedResult : result.getTopResults()) {
            html.append("""
                    <article class="subpanel">
                        <h3>#""").append(rankedResult.getRank()).append("</h3>")
                    .append("""
                        <div class="summary-grid compact-grid">
                    """)
                    .append(renderSummaryCard("Build input", rankedResult.getCandidate().getInputProfileDescription()))
                    .append(renderSummaryCard("Action bar skills", rankedResult.getCandidate().getActionBarSkillsDescription()))
                    .append(renderSummaryCard("Action bar", rankedResult.getCandidate().getActionBarDescription()))
                    .append(renderSummaryCard("Tryb biblioteki itemów", rankedResult.getCandidate().getItemLibraryModeDescription()))
                    .append(renderSummaryCard("Wybrane itemy z biblioteki", rankedResult.getCandidate().getSelectedItemLibraryItemsDescription()))
                    .append(renderSummaryCard("Łączny wkład itemów", rankedResult.getCandidate().getItemLibraryContributionDescription()))
                    .append(renderSummaryCard("Total damage", Long.toString(rankedResult.getSimulationResult().getTotalDamage())))
                    .append(renderSummaryCard("DPS", String.format(Locale.US, "%.4f", rankedResult.getSimulationResult().getDps())))
                    .append("""
                        </div>
                    """)
                    .append(renderDetailsForm(rankedResult))
                    .append("""
                    </article>
                    """);
        }
        html.append("</section>");
        return html.toString();
    }

    private static String renderDetailsForm(BuildSearchRankedResult rankedResult) {
        CurrentBuildRequest request = rankedResult.getCandidate().getCurrentBuildRequest();
        BuildSearchCandidate candidate = rankedResult.getCandidate();
        StringBuilder html = new StringBuilder("""
                <form method="post" action="/znajdz-najlepszy-build/szczegoly" class="detail-form">
                """);
        appendHiddenField(html, "selectedRank", Integer.toString(rankedResult.getRank()));
        appendHiddenField(html, "useItemLibrary", Boolean.toString(candidate.usesItemLibrary()));
        appendHiddenField(html, "level", Integer.toString(request.getLevel()));
        appendHiddenField(html, "weaponDamage", Long.toString(request.getWeaponDamage()));
        appendHiddenField(html, "strength", Double.toString(request.getStrength()));
        appendHiddenField(html, "intelligence", Double.toString(request.getIntelligence()));
        appendHiddenField(html, "thorns", Double.toString(request.getThorns()));
        appendHiddenField(html, "blockChance", Double.toString(request.getBlockChance()));
        appendHiddenField(html, "retributionChance", Double.toString(request.getRetributionChance()));
        appendHiddenField(html, "horizonSeconds", Integer.toString(request.getHorizonSeconds()));

        for (SkillId skillId : SkillId.values()) {
            SkillState skillState = request.getLearnedSkills().get(skillId);
            appendHiddenField(html, CurrentBuildFormData.rankFieldName(skillId), Integer.toString(skillState == null ? 0 : skillState.getRank()));
            if (skillState != null && skillState.isBaseUpgrade()) {
                appendHiddenField(html, CurrentBuildFormData.baseUpgradeFieldName(skillId), "true");
            }
            appendHiddenField(html, CurrentBuildFormData.choiceFieldName(skillId), skillState == null ? SkillUpgradeChoice.NONE.name() : skillState.getChoiceUpgrade().name());
        }

        for (int slot = 1; slot <= 4; slot++) {
            String value = slot <= request.getActionBar().size()
                    ? request.getActionBar().get(slot - 1).name()
                    : "NONE";
            appendHiddenField(html, CurrentBuildFormData.actionBarFieldName(slot), value);
        }
        appendHiddenField(html, "itemLibrarySelectedCount", Integer.toString(candidate.getItemLibraryCombination().getSelectedItems().size()));
        for (int index = 0; index < candidate.getItemLibraryCombination().getSelectedItems().size(); index++) {
            appendLibraryItemHiddenFields(html, index, candidate.getItemLibraryCombination().getSelectedItems().get(index));
        }
        html.append("""
                    <button type="submit">Pokaż pełną analizę kandydata</button>
                </form>
                """);
        return html.toString();
    }

    private static void appendLibraryItemHiddenFields(StringBuilder html, int index, krys.itemlibrary.SavedImportedItem item) {
        appendHiddenField(html, "itemLibraryItemId_" + index, Long.toString(item.getItemId()));
        appendHiddenField(html, "itemLibraryDisplayName_" + index, item.getDisplayName());
        appendHiddenField(html, "itemLibrarySourceImageName_" + index, item.getSourceImageName());
        appendHiddenField(html, "itemLibrarySlot_" + index, item.getSlot().name());
        appendHiddenField(html, "itemLibraryWeaponDamage_" + index, Long.toString(item.getWeaponDamage()));
        appendHiddenField(html, "itemLibraryStrength_" + index, Double.toString(item.getStrength()));
        appendHiddenField(html, "itemLibraryIntelligence_" + index, Double.toString(item.getIntelligence()));
        appendHiddenField(html, "itemLibraryThorns_" + index, Double.toString(item.getThorns()));
        appendHiddenField(html, "itemLibraryBlockChance_" + index, Double.toString(item.getBlockChance()));
        appendHiddenField(html, "itemLibraryRetributionChance_" + index, Double.toString(item.getRetributionChance()));
    }

    private static void appendHiddenField(StringBuilder html, String name, String value) {
        html.append("<input type=\"hidden\" name=\"")
                .append(escapeHtml(name))
                .append("\" value=\"")
                .append(escapeHtml(value))
                .append("\">");
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

    private static String joinIntegers(List<Integer> values) {
        return values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
    }

    private static String joinLongs(List<Long> values) {
        return values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
    }

    private static String joinWholeDoubles(List<Double> values) {
        return values.stream()
                .map(value -> String.format(Locale.US, "%.0f", value))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String joinBooleans(List<Boolean> values) {
        return values.stream()
                .map(value -> Boolean.TRUE.equals(value) ? "true" : "false")
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String joinChoices(List<SkillUpgradeChoice> values) {
        List<String> labels = new ArrayList<>();
        for (SkillUpgradeChoice choiceUpgrade : values) {
            labels.add(choiceUpgrade.name());
        }
        return String.join(", ", labels);
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
        try (InputStream inputStream = SearchBuildPageRenderer.class.getResourceAsStream("/templates/search-build.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Brak szablonu /templates/search-build.html");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się wczytać szablonu strony M12", exception);
        }
    }
}
