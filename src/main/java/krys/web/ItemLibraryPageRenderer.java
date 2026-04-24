package krys.web;

import krys.item.HeroEquipmentSlot;
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
                .replace("{{APP_SHELL_STYLES}}", AppShellRendererSupport.renderSharedStyles())
                .replace("{{GLOBAL_NAV}}", AppShellRendererSupport.renderGlobalNavigation("/biblioteka-itemow"))
                .replace("{{HERO_CONTEXT}}", renderHeroContext(model))
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
                    <p class="helper">Item został zapisany trwale we wspólnej bibliotece. """)
                .append(escapeHtml(buildHeroSaveFeedback(model)))
                .append("""
                    </p>
                    <div class="hero-links">
                """)
                .append(model.hasActiveHero() ? renderActivateSavedItemForm(model, savedItem) : "")
                .append("<a class=\"nav-link secondary-button\" href=\"")
                .append(escapeHtml(buildCurrentBuildUrl(model.getCurrentBuildQuery())))
                .append("\">Wróć do aktualnego buildu</a></div></section>")
                .toString();
    }

    private static String renderHeroContext(ItemLibraryPageModel model) {
        if (!model.hasActiveHero()) {
            return """
                    <section class="panel panel-warning">
                        <h2>Brak aktywnego bohatera</h2>
                        <p>Biblioteka itemów pozostaje wspólna, ale bez aktywnego bohatera nie zobaczysz aktywnych slotów ani nie przypiszesz itemu do ekwipunku. Utwórz albo wybierz bohatera, aby pracować na jego buildzie.</p>
                        <div class="hero-links">
                            <a class="nav-link" href="/bohaterowie">Przejdź do modułu Bohaterowie</a>
                        </div>
                    </section>
                    """;
        }
        return """
                <section class="panel panel-success">
                    <h2>Aktywny bohater biblioteki</h2>
                    <p class="helper">Pracujesz teraz na bohaterze %s. Wspólna biblioteka itemów jest współdzielona, ale status aktywności i wybór slotów dotyczą tylko jego ekwipunku.</p>
                </section>
                """.formatted(escapeHtml(model.getActiveHero().getName()));
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
            java.util.List<HeroEquipmentSlot> activeSlots = resolveActiveHeroSlots(model, item);
            html.append("<tr><td>")
                    .append(escapeHtml(ItemLibraryPresentationSupport.slotDisplayName(item.getSlot())))
                    .append("</td><td>")
                    .append(escapeHtml(item.getDisplayName()))
                    .append("</td><td>")
                    .append(escapeHtml(ItemLibraryPresentationSupport.userItemIdentifier(item)))
                    .append("</td><td>")
                    .append(escapeHtml(ItemLibraryPresentationSupport.itemContributionLabel(item)))
                    .append("</td><td>")
                    .append(renderStatusCell(model, activeSlots))
                    .append("</td><td>")
                    .append(renderActivateForm(model, item, activeSlots))
                    .append(renderDeleteForm(model, item))
                    .append("</td></tr>");
        }
        return html.toString();
    }

    private static String renderStatusCell(ItemLibraryPageModel model, java.util.List<HeroEquipmentSlot> activeSlots) {
        if (!model.hasActiveHero()) {
            return """
                    <span class="status-badge status-inactive">Brak bohatera</span>
                    <div class="status-note">Wybierz aktywnego bohatera, aby zobaczyć przypisane sloty.</div>
                    """;
        }
        if (!activeSlots.isEmpty()) {
            return """
                    <span class="status-badge status-active">Aktywny</span>
                    <div class="status-note">Ten item zasila bohatera w slotach: %s.</div>
                    """.formatted(escapeHtml(joinHeroSlots(activeSlots)));
        }
        return """
                <span class="status-badge status-inactive">Nieaktywny</span>
                <div class="status-note">Możesz przypisać go do zgodnego slotu aktywnego bohatera.</div>
                """;
    }

    private static String renderActivateForm(ItemLibraryPageModel model, SavedImportedItem item, java.util.List<HeroEquipmentSlot> activeSlots) {
        if (!model.hasActiveHero()) {
            return "<span class=\"helper\">Brak aktywnego bohatera. Przypisz najpierw bohatera, aby ustawić aktywny item.</span>";
        }
        if (!activeSlots.isEmpty()) {
            return "<span class=\"helper\">Ten item jest już aktywny dla: " + escapeHtml(joinHeroSlots(activeSlots)) + ".</span>";
        }
        java.util.List<HeroEquipmentSlot> compatibleSlots = HeroEquipmentSlot.compatibleWith(item.getSlot());
        return """
                <div class="action-stack">
                    <form method="post" action="/biblioteka-itemow" class="inline-form">
                        <input type="hidden" name="action" value="activateItem">
                        <input type="hidden" name="itemId" value="%s">
                        %s
                        <input type="hidden" name="currentBuildQuery" value="%s">
                        <button type="submit">Ustaw jako aktywny</button>
                    </form>
                    <span class="helper">Ustawienie aktywnego itemu przypisuje go do wybranego slotu aktywnego bohatera.</span>
                </div>
                """.formatted(
                item.getItemId(),
                renderHeroSlotField(compatibleSlots),
                escapeHtml(model.getCurrentBuildQuery())
        );
    }

    private static String renderActivateSavedItemForm(ItemLibraryPageModel model, SavedImportedItem item) {
        java.util.List<HeroEquipmentSlot> compatibleSlots = HeroEquipmentSlot.compatibleWith(item.getSlot());
        return """
                <form method="post" action="/biblioteka-itemow" class="inline-form">
                    <input type="hidden" name="action" value="activateItem">
                    <input type="hidden" name="itemId" value="%s">
                    %s
                    <input type="hidden" name="currentBuildQuery" value="%s">
                    <button type="submit">Ustaw jako aktywny</button>
                </form>
                """.formatted(item.getItemId(), renderHeroSlotField(compatibleSlots), escapeHtml(model.getCurrentBuildQuery()));
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

    private static java.util.List<HeroEquipmentSlot> resolveActiveHeroSlots(ItemLibraryPageModel model, SavedImportedItem item) {
        if (!model.hasActiveHero()) {
            return java.util.List.of();
        }
        java.util.List<HeroEquipmentSlot> activeSlots = new java.util.ArrayList<>();
        for (HeroEquipmentSlot heroSlot : HeroEquipmentSlot.compatibleWith(item.getSlot())) {
            if (model.getActiveSelection().isSelected(heroSlot, item.getItemId())) {
                activeSlots.add(heroSlot);
            }
        }
        return java.util.List.copyOf(activeSlots);
    }

    private static String renderHeroSlotField(java.util.List<HeroEquipmentSlot> compatibleSlots) {
        if (compatibleSlots.size() == 1) {
            return "<input type=\"hidden\" name=\"heroSlot\" value=\"" + escapeHtml(compatibleSlots.getFirst().name()) + "\">";
        }
        StringBuilder html = new StringBuilder("<label>Slot bohatera<select name=\"heroSlot\">");
        for (HeroEquipmentSlot heroSlot : compatibleSlots) {
            html.append("<option value=\"")
                    .append(escapeHtml(heroSlot.name()))
                    .append("\">")
                    .append(escapeHtml(ItemLibraryPresentationSupport.heroSlotDisplayName(heroSlot)))
                    .append("</option>");
        }
        html.append("</select></label>");
        return html.toString();
    }

    private static String joinHeroSlots(java.util.List<HeroEquipmentSlot> heroSlots) {
        java.util.List<String> labels = new java.util.ArrayList<>();
        for (HeroEquipmentSlot heroSlot : heroSlots) {
            labels.add(ItemLibraryPresentationSupport.heroSlotDisplayName(heroSlot));
        }
        return String.join(", ", labels);
    }

    private static String buildHeroSaveFeedback(ItemLibraryPageModel model) {
        if (!model.hasActiveHero()) {
            return "Nie masz jeszcze aktywnego bohatera, więc item nie może zostać od razu przypisany do slotu.";
        }
        return "Pracujesz teraz na bohaterze " + model.getActiveHero().getName() + ", więc możesz od razu przypisać item do zgodnego slotu jego ekwipunku.";
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
