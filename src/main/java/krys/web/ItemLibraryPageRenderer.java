package krys.web;

import krys.itemlibrary.ItemLibraryPresentationSupport;
import krys.itemlibrary.SavedImportedItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Renderuje prosty SSR minimalnej biblioteki itemów nad current build. */
public final class ItemLibraryPageRenderer {
    private final String template;

    public ItemLibraryPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(ItemLibraryPageModel model) {
        return template
                .replace("{{GLOBAL_NAV}}", AppShellRendererSupport.renderGlobalNavigation("/biblioteka-itemow"))
                .replace("{{MESSAGES}}", renderMessages(model.getMessages()))
                .replace("{{ERRORS}}", renderErrors(model.getErrors()))
                .replace("{{SAVE_FEEDBACK}}", renderSavedItemFeedback(model))
                .replace("{{CURRENT_BUILD_URL}}", escapeHtml(buildCurrentBuildUrl(model.getCurrentBuildQuery())))
                .replace("{{IMPORT_ITEM_URL}}", escapeHtml(buildItemImportUrl(model.getCurrentBuildQuery())))
                .replace("{{LIBRARY_CONTENT}}", renderLibraryContent(model));
    }

    private static String renderMessages(java.util.List<String> messages) {
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

    private static String renderErrors(java.util.List<String> errors) {
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

    private static String renderSavedItemFeedback(ItemLibraryPageModel model) {
        if (!model.hasSavedItemFeedback()) {
            return "";
        }
        SavedImportedItem savedItem = model.getSavedItemFeedback();
        return new StringBuilder("""
                <section class="panel save-feedback-panel">
                    <h2>Item zapisany do biblioteki</h2>
                    <div class="summary-grid">
                """)
                .append(renderSummaryCard("Nazwa zapisanego itemu", savedItem.getDisplayName()))
                .append(renderSummaryCard("Slot", ItemLibraryPresentationSupport.slotDisplayName(savedItem.getSlot())))
                .append(renderSummaryCard("Identyfikator", ItemLibraryPresentationSupport.userItemIdentifier(savedItem)))
                .append(renderSummaryCard("Wkład do buildu", ItemLibraryPresentationSupport.itemContributionLabel(savedItem)))
                .append("""
                    </div>
                    <p class="helper">Item został zapisany trwale w bibliotece. Jesteś już na stronie biblioteki, więc możesz od razu ustawić go jako aktywny dla tego slotu albo wrócić do aktualnego buildu.</p>
                    <div class="hero-links">
                """)
                .append(renderActivateSavedItemForm(model, savedItem))
                .append("<a class=\"nav-link secondary-button\" href=\"")
                .append(escapeHtml(buildCurrentBuildUrl(model.getCurrentBuildQuery())))
                .append("\">Wróć do aktualnego buildu</a></div></section>")
                .toString();
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
                        <th>Nazwa itemu</th>
                        <th>Identyfikator / źródło</th>
                        <th>Wkład do buildu</th>
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
                    .append(escapeHtml(ItemLibraryPresentationSupport.slotDisplayName(item.getSlot())))
                    .append("</td><td>")
                    .append(escapeHtml(item.getDisplayName()))
                    .append("</td><td>")
                    .append(escapeHtml(ItemLibraryPresentationSupport.userItemIdentifier(item)))
                    .append("</td><td>")
                    .append(escapeHtml(ItemLibraryPresentationSupport.itemContributionLabel(item)))
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
                    <div class="status-note">Ten item zasila aktualny build w swoim slocie.</div>
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

    private static String renderActivateSavedItemForm(ItemLibraryPageModel model, SavedImportedItem item) {
        return """
                <form method="post" action="/biblioteka-itemow" class="inline-form">
                    <input type="hidden" name="action" value="activateItem">
                    <input type="hidden" name="itemId" value="%s">
                    <input type="hidden" name="slot" value="%s">
                    <input type="hidden" name="currentBuildQuery" value="%s">
                    <button type="submit">Ustaw jako aktywny</button>
                </form>
                """.formatted(item.getItemId(), escapeHtml(item.getSlot().name()), escapeHtml(model.getCurrentBuildQuery()));
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

    private static String renderSummaryCard(String label, String value) {
        return CurrentBuildCalculationSectionsRenderer.renderSummaryCard(label, value);
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
