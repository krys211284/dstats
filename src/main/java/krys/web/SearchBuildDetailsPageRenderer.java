package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.search.BuildSearchCandidate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Renderuje prostą stronę SSR ze szczegółami pojedynczego wyniku searcha. */
public final class SearchBuildDetailsPageRenderer {
    private final String template;

    public SearchBuildDetailsPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(SearchBuildDetailsPageModel model) {
        return template
                .replace("{{FORM_ERRORS}}", renderErrors(model.getValidationErrors()))
                .replace("{{DETAIL_SECTION}}", renderDetailSection(model));
    }

    private static String renderErrors(List<String> errors) {
        if (errors.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder("""
                <section class="panel panel-error">
                    <h2>Błędy drill-downu</h2>
                    <ul class="error-list">
                """);
        for (String error : errors) {
            html.append("<li>")
                    .append(CurrentBuildCalculationSectionsRenderer.escapeHtml(error))
                    .append("</li>");
        }
        html.append("""
                    </ul>
                </section>
                """);
        return html.toString();
    }

    private static String renderDetailSection(SearchBuildDetailsPageModel model) {
        if (!model.hasCalculation() || model.getCandidate() == null) {
            return """
                    <section class="panel result-panel">
                        <h2>Szczegóły wyniku searcha</h2>
                        <p>Drill-down M11 oczekuje wybranego wyniku z listy searcha. Wróć do strony searcha i wybierz reprezentanta z top wyników po normalizacji.</p>
                    </section>
                    """;
        }

        BuildSearchCandidate candidate = model.getCandidate();
        CurrentBuildCalculation calculation = model.getCalculation();
        String selectedResultLabel = model.getSelectedRank() > 0
                ? "#" + model.getSelectedRank()
                : "nieznany";

        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Szczegóły wyniku searcha</h2>
                    <p class="helper">""")
                .append(CurrentBuildCalculationSectionsRenderer.escapeHtml(model.getHelpText()))
                .append("</p>")
                .append("""
                    <div class="summary-grid">
                """)
                .append(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Wybrany wynik po normalizacji", selectedResultLabel))
                .append(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Build input", candidate.getInputProfileDescription()))
                .append(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Action bar skills", candidate.getActionBarSkillsDescription()))
                .append(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Action bar", candidate.getActionBarDescription()))
                .append(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Total damage", Long.toString(calculation.getResult().getTotalDamage())))
                .append(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("DPS", String.format(Locale.US, "%.4f", calculation.getResult().getDps())))
                .append(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Reactive contribution", Long.toString(calculation.getResult().getTotalReactiveDamage())))
                .append(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Judgement aktywny na końcu", calculation.getResult().isJudgementActiveAtEnd() ? "Tak" : "Nie"))
                .append(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Resolve aktywny na końcu", calculation.getResult().isResolveActiveAtEnd() ? "Tak" : "Nie"))
                .append(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Active block chance na końcu", String.format(Locale.US, "%.2f%%", calculation.getResult().getActiveBlockChanceAtEnd() * 100.0d)))
                .append(CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Active thorns bonus na końcu", String.format(Locale.US, "%.0f", calculation.getResult().getActiveThornsBonusAtEnd())))
                .append("""
                    </div>
                </section>
                """);
        html.append(CurrentBuildCalculationSectionsRenderer.renderDirectHitDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderDelayedHitDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderReactiveDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderStepTrace(calculation));
        return html.toString();
    }

    private static String loadTemplate() {
        try (InputStream inputStream = SearchBuildDetailsPageRenderer.class.getResourceAsStream("/templates/search-build-details.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Brak szablonu /templates/search-build-details.html");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się wczytać szablonu strony M11", exception);
        }
    }
}
