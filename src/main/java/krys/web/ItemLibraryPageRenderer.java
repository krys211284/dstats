package krys.web;

import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.ApplicationAspectRegistry;
import krys.itemimport.AspectDefinition;
import krys.itemimport.AspectRegistry;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemlibrary.ItemLibraryFilter;
import krys.itemlibrary.ItemLibraryPresentationSupport;
import krys.itemlibrary.SavedImportedItem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Renderuje SSR biblioteki itemów jako przegląd zapisanych itemów nad current build. */
public final class ItemLibraryPageRenderer {
    private static final AspectRegistry ASPECT_REGISTRY = ApplicationAspectRegistry.get();
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
                .replace("{{LIBRARY_CONTENT}}", renderFilters(model) + renderLibraryContent(model));
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
        StringBuilder html = new StringBuilder("<div class=\"library-index\">");
        html.append("<p class=\"helper library-result-count\">Znaleziono ")
                .append(model.getSavedItems().size())
                .append(resultCountLabel(model.getSavedItems().size()))
                .append(model.getFilter().isEmpty() ? "" : " z " + model.getTotalSavedItemCount())
                .append(".</p><div class=\"item-index-scroll\"><table class=\"data-table item-index-table\">")
                .append("""
                        <thead>
                            <tr>
                                <th>Item</th>
                                <th>Slot / typ</th>
                                <th>Aspekt</th>
                                <th>Affixy</th>
                                <th>Akcje</th>
                            </tr>
                        </thead>
                        <tbody>
                        """);
        for (SavedImportedItem item : model.getSavedItems()) {
            html.append(renderItemIndexRow(model, item));
        }
        html.append("""
                        </tbody>
                    </table></div></div>
                """);
        return html.toString();
    }

    private static String renderEmptyState(ItemLibraryPageModel model) {
        if (model.getTotalSavedItemCount() > 0) {
            return """
                    <div class="empty-state">
                        <h3>Brak itemów dla wybranych filtrów</h3>
                        <p>Zmień filtry albo wyczyść je, aby wrócić do pełnej listy zapisanych itemów.</p>
                        <a class="nav-link" href="/biblioteka-itemow">Wyczyść filtry</a>
                    </div>
                    """;
        }
        return """
                <div class="empty-state">
                    <h3>Biblioteka jest pusta</h3>
                    <p>Zaimportuj pierwszy item, aby zapisać go w bibliotece i potem wybrać aktywny item dla slotu.</p>
                    <a class="nav-link" href="%s">Importuj item ze screena</a>
                </div>
                """.formatted(escapeHtml(buildItemImportUrl(model.getCurrentBuildQuery())));
    }

    private static String renderItemIndexRow(ItemLibraryPageModel model, SavedImportedItem item) {
        List<HeroEquipmentSlot> activeSlots = resolveActiveHeroSlots(model, item);
        return new StringBuilder("<tr class=\"item-index-row")
                .append(activeSlots.isEmpty() ? "" : " item-index-row-active")
                .append("\" data-item-id=\"")
                .append(item.getItemId())
                .append("\"><td>")
                .append(renderItemCell(item, activeSlots))
                .append("</td><td>")
                .append(renderSlotTypeCell(item))
                .append("</td><td>")
                .append(renderAspectSummary(item))
                .append("</td><td>")
                .append(renderAffixSummary(item))
                .append("</td><td class=\"actions-cell\"><div class=\"item-actions\">")
                .append(renderItemDetailsDisclosure(item))
                .append(renderItemActions(model, item, activeSlots))
                .append("</div></td></tr>")
                .toString();
    }

    private static String renderItemCell(SavedImportedItem item, List<HeroEquipmentSlot> activeSlots) {
        FullItemRead fullItemRead = item.getFullItemRead();
        String itemName = fullItemRead != null && !fullItemRead.getItemName().isBlank()
                ? fullItemRead.getItemName()
                : item.getDisplayName();
        List<String> meta = new ArrayList<>();
        if (fullItemRead != null) {
            String rarity = simplifyRarity(fullItemRead.getRarity());
            String power = simplifyItemPower(fullItemRead.getItemPower());
            if (!"Brak pewnego odczytu".equals(rarity)) {
                meta.add(rarity);
            }
            if (!"Brak pewnego odczytu".equals(power)) {
                meta.add("Moc " + power);
            }
        }
        return new StringBuilder("<div class=\"item-title-line\"><span class=\"item-name\">")
                .append(escapeHtml(itemName))
                .append("</span>")
                .append(activeSlots.isEmpty() ? "" : "<span class=\"status-badge status-active\">Założony</span>")
                .append("</div>")
                .append(meta.isEmpty() ? "" : "<div class=\"item-submeta\">" + escapeHtml(String.join(" • ", meta)) + "</div>")
                .toString();
    }

    private static String renderSlotTypeCell(SavedImportedItem item) {
        String itemType = simplifyItemType(item.getFullItemRead().getItemTypeLine());
        return """
                <div class="slot-type-cell">
                    <strong>%s</strong>
                    <span>%s</span>
                </div>
                """.formatted(
                escapeHtml(ItemLibraryPresentationSupport.slotDisplayName(item.getSlot())),
                escapeHtml(itemType)
        );
    }

    private static String renderAspectSummary(SavedImportedItem item) {
        if (item.getSelectedAspectId().isBlank()) {
            return "<span class=\"muted-value\">Brak</span>";
        }
        AspectDefinition aspect = ASPECT_REGISTRY.findById(item.getSelectedAspectId()).orElse(null);
        return aspect == null
                ? escapeHtml(item.getSelectedAspectId())
                : escapeHtml(aspect.getDisplayName());
    }

    private static String renderAffixSummary(SavedImportedItem item) {
        if (item.getAffixes().isEmpty()) {
            return "<span class=\"muted-value\">Brak zatwierdzonych affixów</span>";
        }
        StringBuilder html = new StringBuilder("<div class=\"affix-summary\">");
        for (ImportedItemAffix affix : item.getAffixes()) {
            html.append("<span>")
                    .append(escapeHtml(compactAffixLine(affix)))
                    .append("</span>");
        }
        html.append("</div>");
        return html.toString();
    }

    private static String compactAffixLine(ImportedItemAffix affix) {
        String line = affix.getType().formatLine(affix.getValue());
        if (!affix.isGreaterAffix()) {
            return line;
        }
        return "★ " + line.replaceFirst("^[*★⭐✦]\\s*", "");
    }

    private static String renderItemDetailsDisclosure(SavedImportedItem item) {
        return """
                <details class="item-row-details">
                    <summary>Szczegóły</summary>
                    %s
                </details>
                """.formatted(renderFullItemPreview(item));
    }

    private static String renderFullItemPreview(SavedImportedItem item) {
        FullItemRead fullItemRead = item.getFullItemRead();
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return "<div class=\"status-note\">Brak zapisanego pełnego odczytu OCR dla tego itemu.</div>";
        }
        List<String> baseStats = collectBaseStats(fullItemRead);
        List<String> implicitLines = collectLines(fullItemRead, ItemReadLineGroup.IMPLICIT);
        List<String> affixLines = item.getAffixes().stream()
                .map(ImportedItemAffix::toDisplayLine)
                .toList();
        List<String> aspectLines = collectAspectLines(item, fullItemRead);
        List<String> socketLines = collectLines(fullItemRead, ItemReadLineGroup.SOCKET);
        List<String> diagnosticLines = collectLines(fullItemRead, ItemReadLineGroup.OTHER);

        StringBuilder html = new StringBuilder("""
                <div class="item-read-details">
                    <section class="item-line-group item-line-group-basic">
                        <h5>Dane podstawowe</h5>
                    <div class="item-meta-grid">
                """);
        html.append(renderMeta("Nazwa itemu", emptyLabel(fullItemRead.getItemName())))
                .append(renderMeta("Typ itemu", simplifyItemType(fullItemRead.getItemTypeLine())))
                .append(renderMeta("Slot ekwipunku", ItemLibraryPresentationSupport.slotDisplayName(item.getSlot())))
                .append(renderMeta("Rzadkość", simplifyRarity(fullItemRead.getRarity())))
                .append(renderMeta("Moc przedmiotu", simplifyItemPower(fullItemRead.getItemPower())))
                .append(renderMeta("Identyfikator", item.getDisplayName()))
                .append(renderMeta("Źródło", item.getSourceImageName()))
                .append("</div></section>")
                .append(renderTextLineGroup("Base stats", baseStats))
                .append(renderTextLineGroup("Implicit / linie bazowe", implicitLines))
                .append(renderTextLineGroup("Affixy", affixLines))
                .append(renderTextLineGroup("Aspekt / efekt legendarny", aspectLines))
                .append(renderTextLineGroup("Socket / gniazdo", socketLines))
                .append(renderDiagnostics(diagnosticLines))
                .append("</div>");
        return html.toString();
    }

    private static String renderTextLineGroup(String heading, List<String> lines) {
        StringBuilder html = new StringBuilder("""
                <section class="item-line-group">
                    <h5>%s</h5>
                """.formatted(escapeHtml(heading)));
        if (lines.isEmpty()) {
            html.append("<p class=\"helper\">Brak zapisanych linii w tej sekcji.</p></section>");
            return html.toString();
        }
        html.append("<ul class=\"item-read-lines\">");
        for (String line : lines) {
            html.append("<li>").append(escapeHtml(line)).append("</li>");
        }
        html.append("</ul></section>");
        return html.toString();
    }

    private static List<String> collectBaseStats(FullItemRead fullItemRead) {
        List<String> lines = new ArrayList<>();
        addUnique(lines, normalizedBaseStatLine(fullItemRead.getBaseItemValue()));
        for (FullItemReadLine line : fullItemRead.getLines()) {
            if (line.getType() == FullItemReadLineType.BASE_STAT) {
                addUnique(lines, normalizedBaseStatLine(line.getText()));
            }
        }
        return lines;
    }

    private static List<String> collectLines(FullItemRead fullItemRead, ItemReadLineGroup group) {
        List<String> lines = new ArrayList<>();
        for (FullItemReadLine line : fullItemRead.getLines()) {
            if (classifyPresentationLine(line) == group) {
                addUnique(lines, line.getText());
            }
        }
        return lines;
    }

    private static List<String> collectAspectLines(SavedImportedItem item, FullItemRead fullItemRead) {
        List<String> lines = new ArrayList<>();
        if (!item.getSelectedAspectId().isBlank()) {
            AspectDefinition aspect = ASPECT_REGISTRY.findById(item.getSelectedAspectId()).orElse(null);
            if (aspect == null) {
                addUnique(lines, "Wybrany aspekt: " + item.getSelectedAspectId());
            } else {
                addUnique(lines, "Wybrany aspekt: " + aspect.getDisplayName());
                addUnique(lines, "Opis aspektu: " + aspect.getEffectDescription());
            }
        } else {
            addUnique(lines, "Brak wybranego aspektu.");
        }
        return lines;
    }

    private static String renderDiagnostics(List<String> diagnosticLines) {
        if (diagnosticLines.isEmpty()) {
            return "";
        }
        return """
                <details class="item-line-group">
                    <summary>Diagnostyka OCR</summary>
                    <ul class="item-read-lines">
                        %s
                    </ul>
                </details>
                """.formatted(renderListItems(diagnosticLines));
    }

    private static String renderListItems(List<String> lines) {
        StringBuilder html = new StringBuilder();
        for (String line : lines) {
            html.append("<li>").append(escapeHtml(line)).append("</li>");
        }
        return html.toString();
    }

    private static String normalizedBaseStatLine(String line) {
        String normalized = normalizeForDisplayRules(line);
        if (!normalized.contains("PANCERZ")) {
            return line;
        }
        java.util.regex.Matcher armorWithUnit = java.util.regex.Pattern
                .compile("([0-9]+(?:\\s[0-9]{3})*\\s+pkt\\.?\\s+pancerza)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(line == null ? "" : line);
        String lastMatch = "";
        while (armorWithUnit.find()) {
            lastMatch = armorWithUnit.group(1).trim();
        }
        if (!lastMatch.isBlank()) {
            return lastMatch;
        }
        java.util.regex.Matcher armorValue = java.util.regex.Pattern
                .compile("([0-9]+(?:\\s[0-9]{3})*)\\s+pkt\\.?", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(line == null ? "" : line);
        while (armorValue.find()) {
            lastMatch = armorValue.group(1).trim() + " pkt.";
        }
        return lastMatch.isBlank() ? line : lastMatch;
    }

    private static void addUnique(List<String> lines, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalizedValue = normalizeForDisplayRules(value).replaceAll("\\s+", " ").trim();
        for (String line : lines) {
            if (normalizeForDisplayRules(line).replaceAll("\\s+", " ").trim().equals(normalizedValue)) {
                return;
            }
        }
        lines.add(value);
    }

    private static String renderItemActions(ItemLibraryPageModel model, SavedImportedItem item, List<HeroEquipmentSlot> activeSlots) {
        String editLink = "<a class=\"nav-link secondary-link\" href=\""
                + escapeHtml(buildEditItemUrl(model, item))
                + "\">Edytuj</a>";
        if (!model.hasActiveHero()) {
            return """
                    <a class="nav-link secondary-link" href="/bohaterowie">Wybierz bohatera</a>
                    """
                    + editLink
                    + renderDeleteForm(model, item);
        }
        return renderAssignmentForms(model, item)
                + editLink
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
                        %s
                        <button type="submit">%s: %s</button>
                    </form>
                    """.formatted(
                    item.getItemId(),
                    heroSlot.name(),
                    escapeHtml(model.getCurrentBuildQuery()),
                    renderFilterHiddenFields(model.getFilter()),
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
                        %s
                        <button type="submit" class="secondary-button">Usuń</button>
                    </form>
                </div>
                """.formatted(item.getItemId(), escapeHtml(model.getCurrentBuildQuery()), renderFilterHiddenFields(model.getFilter()));
    }

    private static String renderFilters(ItemLibraryPageModel model) {
        ItemLibraryFilter filter = model.getFilter();
        return """
                <section class="panel">
                    <h2>Filtry biblioteki</h2>
                    <form method="get" action="/biblioteka-itemow" class="form-grid">
                        <label>
                            Szukaj
                            <input type="text" name="q" value="%s" placeholder="Nazwa, plik, aspekt albo affix">
                        </label>
                        <label>
                            Slot
                            <select name="slot">
                                <option value="">Wszystkie</option>
                                %s
                            </select>
                        </label>
                        <label>
                            Typ itemu
                            <select name="type">
                                <option value="">Wszystkie</option>
                                %s
                            </select>
                        </label>
                        <label>
                            Status użycia
                            <select name="status">
                                %s
                            </select>
                        </label>
                        <label>
                            Aspekt
                            <select name="aspect">
                                <option value="">Wszystkie</option>
                                <option value="%s"%s>Brak aspektu</option>
                                %s
                            </select>
                        </label>
                        <label>
                            Affix
                            <select name="affix">
                                <option value="">Wszystkie</option>
                                %s
                            </select>
                        </label>
                        <label class="checkbox-label">
                            <input type="checkbox" name="greater" value="true"%s> Tylko itemy z Greater Affix
                        </label>
                        <div class="submit-row">
                            <button type="submit">Filtruj</button>
                            <a class="nav-link secondary-link" href="/biblioteka-itemow">Wyczyść filtry</a>
                        </div>
                    </form>
                    <p class="helper">Znaleziono %s%s.</p>
                </section>
                """.formatted(
                escapeHtml(filter.getQuery()),
                renderSlotFilterOptions(filter.getSlot()),
                renderItemTypeFilterOptions(model.getAvailableItemTypes(), filter.getItemType()),
                renderStatusFilterOptions(filter.getStatus()),
                ItemLibraryFilter.ASPECT_NONE,
                ItemLibraryFilter.ASPECT_NONE.equals(filter.getAspect()) ? " selected" : "",
                renderAspectFilterOptions(filter.getAspect()),
                renderAffixFilterOptions(filter.getAffix()),
                filter.isGreaterOnly() ? " checked" : "",
                model.getSavedItems().size(),
                resultCountLabel(model.getSavedItems().size())
        );
    }

    private static String renderSlotFilterOptions(String selectedSlot) {
        StringBuilder html = new StringBuilder();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            html.append("<option value=\"")
                    .append(slot.name())
                    .append("\"")
                    .append(slot.name().equals(selectedSlot) ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(ItemLibraryPresentationSupport.slotDisplayName(slot)))
                    .append("</option>");
        }
        return html.toString();
    }

    private static String renderItemTypeFilterOptions(List<String> itemTypes, String selectedItemType) {
        StringBuilder html = new StringBuilder();
        for (String itemType : itemTypes) {
            html.append("<option value=\"")
                    .append(escapeHtml(itemType))
                    .append("\"")
                    .append(itemType.equals(selectedItemType) ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(itemType))
                    .append("</option>");
        }
        return html.toString();
    }

    private static String renderStatusFilterOptions(String selectedStatus) {
        return renderFilterOption("", "Wszystkie", selectedStatus.isBlank())
                + renderFilterOption("used", "Założone", "used".equals(selectedStatus))
                + renderFilterOption("unused", "Nieużywane", "unused".equals(selectedStatus));
    }

    private static String renderAspectFilterOptions(String selectedAspect) {
        StringBuilder html = new StringBuilder();
        for (AspectDefinition aspect : ASPECT_REGISTRY.all()) {
            html.append("<option value=\"")
                    .append(escapeHtml(aspect.getId()))
                    .append("\"")
                    .append(aspect.getId().equals(selectedAspect) ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(aspect.getDisplayName()))
                    .append("</option>");
        }
        return html.toString();
    }

    private static String renderAffixFilterOptions(String selectedAffix) {
        StringBuilder html = new StringBuilder();
        for (ImportedItemAffixType type : ImportedItemAffixType.values()) {
            html.append("<option value=\"")
                    .append(type.name())
                    .append("\"")
                    .append(type.name().equals(selectedAffix) ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(type.getDisplayName()))
                    .append("</option>");
        }
        return html.toString();
    }

    private static String renderFilterOption(String value, String label, boolean selected) {
        return "<option value=\"" + escapeHtml(value) + "\"" + (selected ? " selected" : "") + ">"
                + escapeHtml(label)
                + "</option>";
    }

    private static String renderFilterHiddenFields(ItemLibraryFilter filter) {
        StringBuilder html = new StringBuilder();
        appendHiddenFilter(html, "q", filter.getQuery());
        appendHiddenFilter(html, "slot", filter.getSlot());
        appendHiddenFilter(html, "type", filter.getItemType());
        appendHiddenFilter(html, "status", filter.getStatus());
        appendHiddenFilter(html, "aspect", filter.getAspect());
        appendHiddenFilter(html, "affix", filter.getAffix());
        if (filter.isGreaterOnly()) {
            appendHiddenFilter(html, "greater", "true");
        }
        return html.toString();
    }

    private static void appendHiddenFilter(StringBuilder html, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        html.append("<input type=\"hidden\" name=\"")
                .append(escapeHtml(name))
                .append("\" value=\"")
                .append(escapeHtml(value))
                .append("\">");
    }

    private static String buildEditItemUrl(ItemLibraryPageModel model, SavedImportedItem item) {
        String filterQuery = buildFilterQuery(model.getFilter());
        String url = "/biblioteka-itemow/edytuj?itemId=" + item.getItemId();
        if (!filterQuery.isBlank()) {
            url += "&" + filterQuery;
        }
        return url;
    }

    private static String buildFilterQuery(ItemLibraryFilter filter) {
        List<String> parts = new ArrayList<>();
        appendQueryPart(parts, "q", filter.getQuery());
        appendQueryPart(parts, "slot", filter.getSlot());
        appendQueryPart(parts, "type", filter.getItemType());
        appendQueryPart(parts, "status", filter.getStatus());
        appendQueryPart(parts, "aspect", filter.getAspect());
        appendQueryPart(parts, "affix", filter.getAffix());
        if (filter.isGreaterOnly()) {
            appendQueryPart(parts, "greater", "true");
        }
        return String.join("&", parts);
    }

    private static void appendQueryPart(List<String> parts, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        parts.add(encodeUrl(name) + "=" + encodeUrl(value));
    }

    private static String encodeUrl(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String resultCountLabel(int count) {
        return count == 1 ? " item" : " itemy";
    }

    private static String renderSummaryCard(String label, String value) {
        return CurrentBuildCalculationSectionsRenderer.renderSummaryCard(label, value);
    }

    private static ItemReadLineGroup classifyPresentationLine(FullItemReadLine line) {
        String normalized = normalizeForDisplayRules(line.getText());
        if (line.getType() == FullItemReadLineType.ITEM_NAME
                || line.getType() == FullItemReadLineType.TYPE_OR_SLOT
                || line.getType() == FullItemReadLineType.RARITY
                || line.getType() == FullItemReadLineType.ITEM_POWER
                || line.getType() == FullItemReadLineType.BASE_STAT) {
            return ItemReadLineGroup.HEADER;
        }
        if (line.getType() == FullItemReadLineType.IMPLICIT
                || normalized.contains("REDUKCJI BLOKOWANYCH OBRAZEN")
                || normalized.contains("SZANSY NA BLOK")
                || normalized.contains("SZANSA NA BLOK")
                || normalized.contains("OBRAZEN OD BRONI W GLOWNEJ RECE")) {
            return ItemReadLineGroup.IMPLICIT;
        }
        if (line.getType() == FullItemReadLineType.ASPECT
                || normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE")
                || normalized.contains("TA PREMIA JEST")) {
            return ItemReadLineGroup.SPECIAL;
        }
        if (line.getType() == FullItemReadLineType.SOCKET) {
            return ItemReadLineGroup.SOCKET;
        }
        if (line.getType() == FullItemReadLineType.AFFIX && !normalized.contains("ROZJUSZENIE")) {
            return ItemReadLineGroup.AFFIX;
        }
        return ItemReadLineGroup.OTHER;
    }

    private static String simplifyItemType(String itemTypeLine) {
        String normalized = normalizeForDisplayRules(itemTypeLine);
        if (normalized.contains("TARCZA") || normalized.contains("SHIELD")) {
            return "Tarcza";
        }
        if (normalized.contains("BUTY") || normalized.contains("BOOTS")) {
            return "Buty";
        }
        if (normalized.contains("BRON GLOWNA") || normalized.contains("MAIN HAND")) {
            return "Broń główna";
        }
        if (normalized.contains("REKA DODATKOWA") || normalized.contains("OFF HAND")) {
            return "Ręka dodatkowa";
        }
        return emptyLabel(itemTypeLine);
    }

    private static String simplifyRarity(String rarity) {
        String normalized = normalizeForDisplayRules(rarity);
        List<String> parts = new ArrayList<>();
        if (normalized.contains("STAROZYTNA") || normalized.contains("STAROZYTNY") || normalized.contains("ANCESTRAL")) {
            parts.add("Starożytna");
        }
        if (normalized.contains("LEGENDARNA") || normalized.contains("LEGENDARNY") || normalized.contains("LEGENDARY")) {
            parts.add("legendarna");
        } else if (normalized.contains("UNIKATOWA") || normalized.contains("UNIKATOWY") || normalized.contains("UNIQUE")) {
            parts.add("unikatowa");
        } else if (normalized.contains("RZADKA") || normalized.contains("RZADKI") || normalized.contains("RARE")) {
            parts.add("rzadka");
        }
        if (!parts.isEmpty()) {
            return String.join(" ", parts);
        }
        return emptyLabel(rarity);
    }

    private static String simplifyItemPower(String itemPower) {
        String value = firstNumber(itemPower);
        return value.isBlank() ? emptyLabel(itemPower) : value;
    }

    private static String firstNumber(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+(?:\\s\\d{3})*(?:[,.]\\d+)?").matcher(value);
        return matcher.find() ? matcher.group() : "";
    }

    private static String normalizeForDisplayRules(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(java.util.Locale.ROOT);
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

    private static String emptyLabel(String value) {
        return value == null || value.isBlank() ? "Brak pewnego odczytu" : value;
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

    private enum ItemReadLineGroup {
        HEADER,
        IMPLICIT,
        AFFIX,
        SPECIAL,
        SOCKET,
        OTHER
    }
}
