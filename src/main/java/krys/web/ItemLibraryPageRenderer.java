package krys.web;

import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.itemlibrary.ItemLibraryPresentationSupport;
import krys.itemlibrary.SavedImportedItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Renderuje SSR biblioteki itemów jako przegląd zapisanych itemów nad current build. */
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
                .append(model.hasActiveHero() ? renderAssignmentForms(model, savedItem) : "")
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
        StringBuilder html = new StringBuilder("<div class=\"library-groups\">");
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            List<SavedImportedItem> slotItems = model.getSavedItems().stream()
                    .filter(item -> item.getSlot() == slot)
                    .toList();
            if (slotItems.isEmpty()) {
                continue;
            }
            html.append(renderSlotGroup(model, slot, slotItems));
        }
        html.append("</div>");
        return html.toString();
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

    private static String renderSlotGroup(ItemLibraryPageModel model, EquipmentSlot slot, List<SavedImportedItem> slotItems) {
        String countLabel = slotItems.size() == 1 ? "1 item" : slotItems.size() + " itemy";
        StringBuilder html = new StringBuilder("""
                <section class="slot-group">
                    <div class="slot-group-head">
                        <div>
                            <span class="section-kicker">Slot itemu</span>
                            <h3>""")
                .append(escapeHtml(ItemLibraryPresentationSupport.slotDisplayName(slot)))
                .append("""
                            </h3>
                        </div>
                        <span class="slot-count">""")
                .append(escapeHtml(countLabel))
                .append("""
                        </span>
                    </div>
                    <div class="item-card-grid">
                """);
        for (SavedImportedItem item : slotItems) {
            html.append(renderItemCard(model, item));
        }
        html.append("""
                    </div>
                </section>
                """);
        return html.toString();
    }

    private static String renderItemCard(ItemLibraryPageModel model, SavedImportedItem item) {
        List<HeroEquipmentSlot> activeSlots = resolveActiveHeroSlots(model, item);
        return new StringBuilder("<article class=\"item-card")
                .append(activeSlots.isEmpty() ? "" : " item-card-active")
                .append("\"><div class=\"item-card-head\"><div><span class=\"slot-chip\">")
                .append(escapeHtml(ItemLibraryPresentationSupport.slotDisplayName(item.getSlot())))
                .append("</span><h4>")
                .append(escapeHtml(item.getDisplayName()))
                .append("</h4></div>")
                .append(renderUsageBadge(model, activeSlots))
                .append("</div><div class=\"item-meta-grid\">")
                .append(renderMeta("Identyfikator / źródło", ItemLibraryPresentationSupport.userItemIdentifier(item)))
                .append(renderMeta("Skrót wkładu", ItemLibraryPresentationSupport.shortContributionLabel(item)))
                .append(renderMeta("Zgodne sloty bohatera", joinHeroSlots(HeroEquipmentSlot.compatibleWith(item.getSlot()))))
                .append("</div>")
                .append(renderUsageDetails(model, activeSlots))
                .append("<div class=\"item-actions\">")
                .append(renderItemActions(model, item, activeSlots))
                .append("</div></article>")
                .toString();
    }

    private static String renderUsageBadge(ItemLibraryPageModel model, List<HeroEquipmentSlot> activeSlots) {
        if (!model.hasActiveHero()) {
            return "<span class=\"status-badge status-inactive\">Brak bohatera</span>";
        }
        if (!activeSlots.isEmpty()) {
            return "<span class=\"status-badge status-active\">Używany</span>";
        }
        return "<span class=\"status-badge status-inactive\">Nie używany</span>";
    }

    private static String renderUsageDetails(ItemLibraryPageModel model, List<HeroEquipmentSlot> activeSlots) {
        if (!model.hasActiveHero()) {
            return """
                    <div class="status-note">Brak aktywnego bohatera. Wybierz albo utwórz bohatera, aby zobaczyć użycie itemu i założyć go do slotu.</div>
                    """;
        }
        if (!activeSlots.isEmpty()) {
            return """
                    <div class="status-note">Używany przez aktywnego bohatera w slocie: %s.</div>
                    """.formatted(escapeHtml(joinHeroSlots(activeSlots)));
        }
        return """
                <div class="status-note">Nie używany przez aktywnego bohatera. Możesz założyć go do zgodnego slotu.</div>
                """;
    }

    private static String renderItemActions(ItemLibraryPageModel model, SavedImportedItem item, List<HeroEquipmentSlot> activeSlots) {
        if (!model.hasActiveHero()) {
            return """
                    <a class="nav-link secondary-link" href="/bohaterowie">Wybierz bohatera</a>
                    """
                    + renderDeleteForm(model, item);
        }
        return renderAssignmentForms(model, item)
                + "<a class=\"nav-link secondary-link\" href=\""
                + escapeHtml(buildCurrentBuildUrl(model.getCurrentBuildQuery()))
                + "\">Pokaż slot w current build</a>"
                + renderDeleteForm(model, item);
    }

    private static String renderAssignmentForms(ItemLibraryPageModel model, SavedImportedItem item) {
        StringBuilder html = new StringBuilder("<div class=\"assign-actions\">");
        for (HeroEquipmentSlot heroSlot : HeroEquipmentSlot.compatibleWith(item.getSlot())) {
            Long selectedItemId = model.getActiveSelection().getSelectedItemId(heroSlot);
            boolean slotEmpty = selectedItemId == null;
            boolean thisItemSelected = selectedItemId != null && selectedItemId == item.getItemId();
            String actionLabel = slotEmpty ? "Załóż bohaterowi" : "Zmień w slocie";
            if (thisItemSelected) {
                html.append("<span class=\"helper\">Już założony w slocie ")
                        .append(escapeHtml(ItemLibraryPresentationSupport.heroSlotDisplayName(heroSlot)))
                        .append(".</span>");
                continue;
            }
            html.append("""
                    <form method="post" action="/biblioteka-itemow" class="inline-form">
                        <input type="hidden" name="action" value="activateItem">
                        <input type="hidden" name="itemId" value="%s">
                        <input type="hidden" name="heroSlot" value="%s">
                        <input type="hidden" name="currentBuildQuery" value="%s">
                        <button type="submit">%s: %s</button>
                    </form>
                    """.formatted(
                    item.getItemId(),
                    heroSlot.name(),
                    escapeHtml(model.getCurrentBuildQuery()),
                    escapeHtml(actionLabel),
                    escapeHtml(ItemLibraryPresentationSupport.heroSlotDisplayName(heroSlot))
            ));
        }
        html.append("</div>");
        return html.toString();
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

    private static String renderMeta(String label, String value) {
        return """
                <div class="item-meta">
                    <span>%s</span>
                    <strong>%s</strong>
                </div>
                """.formatted(escapeHtml(label), escapeHtml(value));
    }

    private static List<HeroEquipmentSlot> resolveActiveHeroSlots(ItemLibraryPageModel model, SavedImportedItem item) {
        if (!model.hasActiveHero()) {
            return List.of();
        }
        List<HeroEquipmentSlot> activeSlots = new ArrayList<>();
        for (HeroEquipmentSlot heroSlot : HeroEquipmentSlot.compatibleWith(item.getSlot())) {
            if (model.getActiveSelection().isSelected(heroSlot, item.getItemId())) {
                activeSlots.add(heroSlot);
            }
        }
        return List.copyOf(activeSlots);
    }

    private static String joinHeroSlots(List<HeroEquipmentSlot> heroSlots) {
        List<String> labels = new ArrayList<>();
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
