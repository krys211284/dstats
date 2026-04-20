package krys.web;

import krys.itemlibrary.SavedImportedItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renderuje prosty SSR minimalnej biblioteki itemów nad current build. */
public final class ItemLibraryPageRenderer {
    private final String template;

    public ItemLibraryPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(ItemLibraryPageModel model) {
        return template
                .replace("{{MESSAGES}}", renderMessages(model.getMessages()))
                .replace("{{ERRORS}}", renderErrors(model.getErrors()))
                .replace("{{CURRENT_BUILD_URL}}", escapeHtml(buildCurrentBuildUrl(model.getCurrentBuildQuery())))
                .replace("{{IMPORT_ITEM_URL}}", escapeHtml(buildItemImportUrl(model.getCurrentBuildQuery())))
                .replace("{{LIBRARY_CONTENT}}", renderLibraryContent(model));
    }

    private static String renderMessages(List<String> messages) {
        if (messages.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<section class=\"panel panel-success\"><ul class=\"message-list\">");
        for (String message : messages) {
            html.append("<li>").append(escapeHtml(message)).append("</li>");
        }
        html.append("</ul></section>");
        return html.toString();
    }

    private static String renderErrors(List<String> errors) {
        if (errors.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<section class=\"panel panel-error\"><ul class=\"message-list\">");
        for (String error : errors) {
            html.append("<li>").append(escapeHtml(error)).append("</li>");
        }
        html.append("</ul></section>");
        return html.toString();
    }

    private static String renderLibraryContent(ItemLibraryPageModel model) {
        if (model.getSavedItems().isEmpty()) {
            return renderEmptyState(model);
        }
        return """
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>Slot</th>
                        <th>Display name</th>
                        <th>Plik źródłowy</th>
                        <th>Wkład do current build</th>
                        <th>Status</th>
                        <th>Akcje</th>
                    </tr>
                    </thead>
                    <tbody>
                """
                + renderItemRows(model)
                + """
                    </tbody>
                </table>
                """;
    }

    private static String renderEmptyState(ItemLibraryPageModel model) {
        return """
                <div class="empty-state">
                    <h3>Biblioteka jest pusta</h3>
                    <p>Zaimportuj pierwszy item, aby zapisać go w bibliotece i potem wybrać aktywny item dla slotu.</p>
                    <a class="nav-link" href="%s">Importuj item ze screena</a>
                </div>
                """.formatted(escapeHtml(buildItemImportUrl(model.getCurrentBuildQuery())));
    }

    private static String renderItemRows(ItemLibraryPageModel model) {
        StringBuilder html = new StringBuilder();
        for (SavedImportedItem item : model.getSavedItems()) {
            boolean active = model.getActiveSelection().isSelected(item.getSlot(), item.getItemId());
            html.append("<tr><td>")
                    .append(escapeHtml(item.getSlot().name()))
                    .append("</td><td>")
                    .append(escapeHtml(item.getDisplayName()))
                    .append("</td><td>")
                    .append(escapeHtml(item.getSourceImageName()))
                    .append("</td><td>")
                    .append(escapeHtml(buildContributionLabel(item)))
                    .append("</td><td>")
                    .append(renderStatusCell(active))
                    .append("</td><td>")
                    .append(renderActivateForm(model, item, active))
                    .append(renderDeleteForm(model, item))
                    .append("</td></tr>");
        }
        return html.toString();
    }

    private static String renderStatusCell(boolean active) {
        if (active) {
            return """
                    <span class="status-badge status-active">Aktywny</span>
                    <div class="status-note">Ten item zasila current build w swoim slocie.</div>
                    """;
        }
        return """
                <span class="status-badge status-inactive">Nieaktywny</span>
                <div class="status-note">Możesz go aktywować zamiast bieżącego wyboru w tym slocie.</div>
                """;
    }

    private static String renderActivateForm(ItemLibraryPageModel model, SavedImportedItem item, boolean active) {
        if (active) {
            return "<span class=\"helper\">Aktywny item dla tego slotu jest już ustawiony.</span>";
        }
        return """
                <div class="action-stack">
                    <form method="post" action="/biblioteka-itemow" class="inline-form">
                        <input type="hidden" name="action" value="activateItem">
                        <input type="hidden" name="itemId" value="%s">
                        <input type="hidden" name="slot" value="%s">
                        <input type="hidden" name="currentBuildQuery" value="%s">
                        <button type="submit">Ustaw jako aktywny</button>
                    </form>
                    <span class="helper">Ustawienie aktywnego itemu zastępuje poprzedni aktywny item w tym samym slocie.</span>
                </div>
                """.formatted(
                item.getItemId(),
                escapeHtml(item.getSlot().name()),
                escapeHtml(model.getCurrentBuildQuery())
        );
    }

    private static String renderDeleteForm(ItemLibraryPageModel model, SavedImportedItem item) {
        return """
                <div class="action-stack">
                    <form method="post" action="/biblioteka-itemow" class="inline-form">
                        <input type="hidden" name="action" value="deleteItem">
                        <input type="hidden" name="itemId" value="%s">
                        <input type="hidden" name="currentBuildQuery" value="%s">
                        <button type="submit" class="secondary-button">Usuń</button>
                    </form>
                </div>
                """.formatted(item.getItemId(), escapeHtml(model.getCurrentBuildQuery()));
    }

    private static String buildContributionLabel(SavedImportedItem item) {
        List<String> labels = new ArrayList<>();
        if (item.getWeaponDamage() > 0L) {
            labels.add("weapon=" + item.getWeaponDamage());
        }
        if (item.getStrength() > 0.0d) {
            labels.add("str=" + formatWhole(item.getStrength()));
        }
        if (item.getIntelligence() > 0.0d) {
            labels.add("int=" + formatWhole(item.getIntelligence()));
        }
        if (item.getThorns() > 0.0d) {
            labels.add("thorns=" + formatWhole(item.getThorns()));
        }
        if (item.getBlockChance() > 0.0d) {
            labels.add("block=" + formatWhole(item.getBlockChance()) + "%");
        }
        if (item.getRetributionChance() > 0.0d) {
            labels.add("retribution=" + formatWhole(item.getRetributionChance()) + "%");
        }
        return labels.isEmpty() ? "Brak wkładu" : String.join(", ", labels);
    }

    private static String buildCurrentBuildUrl(String currentBuildQuery) {
        if (currentBuildQuery == null || currentBuildQuery.isBlank()) {
            return "/policz-aktualny-build";
        }
        return "/policz-aktualny-build?" + currentBuildQuery;
    }

    private static String buildItemImportUrl(String currentBuildQuery) {
        if (currentBuildQuery == null || currentBuildQuery.isBlank()) {
            return "/importuj-item-ze-screena";
        }
        return "/importuj-item-ze-screena?" + currentBuildQuery;
    }

    private static String formatWhole(double value) {
        return String.format(Locale.US, "%.0f", value);
    }

    private static String escapeHtml(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }

    private static String loadTemplate() {
        try (InputStream inputStream = ItemLibraryPageRenderer.class.getResourceAsStream("/templates/item-library.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Brak szablonu /templates/item-library.html");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się wczytać szablonu strony biblioteki itemów.", exception);
        }
    }
}
