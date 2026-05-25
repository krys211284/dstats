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
import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingPresentationSupport;

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
        StringBuilder html = new StringBuilder("<div id=\"biblioteka-lista\" class=\"library-index\">");
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
                    </table></div>
                """);
        for (SavedImportedItem item : model.getSavedItems()) {
            html.append(renderItemDetailsModal(item));
        }
        html.append("</div>");
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
                .append(renderItemActions(model, item, activeSlots))
                .append("</div></td></tr>")
                .toString();
    }

    private static String renderItemCell(SavedImportedItem item, List<HeroEquipmentSlot> activeSlots) {
        FullItemRead fullItemRead = item.getFullItemRead();
        String itemName = ItemLibraryPresentationSupport.canonicalItemName(item);
        List<String> meta = new ArrayList<>();
        if (fullItemRead != null) {
            String rarity = simplifyRarity(item.getItemRarity().isBlank() ? fullItemRead.getRarity() : item.getItemRarity());
            String power = item.getItemPower() == null ? simplifyItemPower(fullItemRead.getItemPower()) : Long.toString(item.getItemPower());
            if (!"Brak pewnego odczytu".equals(rarity)) {
                meta.add(rarity);
            }
            if (!"Brak pewnego odczytu".equals(power)) {
                meta.add("Moc " + power);
            }
        }
        return new StringBuilder("<div class=\"item-title-line\"><a class=\"item-name item-details-link\" href=\"#item-details-")
                .append(item.getItemId())
                .append("\" aria-haspopup=\"dialog\" aria-controls=\"item-details-")
                .append(item.getItemId())
                .append("\">")
                .append(escapeHtml(itemName))
                .append("</a>")
                .append(activeSlots.isEmpty() ? "" : "<span class=\"status-badge status-active\">Założony</span>")
                .append("</div>")
                .append(meta.isEmpty() ? "" : "<div class=\"item-submeta\">" + escapeHtml(String.join(" • ", meta)) + "</div>")
                .toString();
    }

    private static String renderSlotTypeCell(SavedImportedItem item) {
        String rawType = item.getItemType().isBlank() ? item.getFullItemRead().getItemTypeLine() : item.getItemType();
        String itemType = simplifyItemType(rawType);
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
        if (aspect == null) {
            return escapeHtml(item.getSelectedAspectId());
        }
        return "<span class=\"aspect-summary\" title=\""
                + escapeHtml(aspect.getEffectDescription())
                + "\" aria-label=\""
                + escapeHtml(aspect.getDisplayName() + ". " + aspect.getEffectDescription())
                + "\">"
                + escapeHtml(aspect.getDisplayName())
                + "</span>";
    }

    private static String renderAffixSummary(SavedImportedItem item) {
        if (item.getAffixes().isEmpty() && item.getTemperingAffixes().isEmpty()
                && !item.getTransfiguration().isTransfigured()
                && item.getSocketing().getSocketCount() <= 0) {
            return "<span class=\"muted-value\">Brak zatwierdzonych affixów</span>";
        }
        StringBuilder html = new StringBuilder("<ul class=\"affix-summary\">");
        for (ImportedItemAffix affix : item.getAffixes()) {
            html.append("<li>")
                    .append(formatAffixSummaryLine(item, affix))
                    .append("</li>");
        }
        for (ItemTemperingAffix affix : item.getTemperingAffixes()) {
            html.append("<li>")
                    .append(escapeHtml(affix.getCategory().getDisplayName()))
                    .append(": ")
                    .append(formatTemperingDetailsLine(item, affix))
                    .append(affix.isGreaterAffix() ? " | Greater Affix / Gwiazdka" : "")
                    .append("</li>");
        }
        String transfigurationSummary = TransfigurationSectionRenderer.compactChip(item.getTransfiguration(), item.getAffixes());
        if (!transfigurationSummary.isBlank()) {
            html.append("<li>").append(escapeHtml(transfigurationSummary)).append("</li>");
        }
        String socketingSummary = krys.socketing.SocketingPresentationSupport.compactSummary(
                item.getSocketing(), item.getSlot(), item.getDetails());
        if (!socketingSummary.isBlank()) {
            html.append("<li>").append(escapeHtml(socketingSummary)).append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private static String formatAffixSummaryLine(SavedImportedItem item, ImportedItemAffix affix) {
        if (item.getMasterworking() != null && item.getMasterworking().hasVisibleProgress()) {
            return MasterworkingSectionRenderer.formatAffixReadonlyLine(item.getMasterworking(), affix);
        }
        return escapeHtml(ItemLibraryPresentationSupport.formatAffixForList(affix));
    }

    private static String renderItemDetailsModal(SavedImportedItem item) {
        String title = ItemLibraryPresentationSupport.canonicalItemName(item);
        return """
                <section id="item-details-%s" class="item-details-modal" role="dialog" aria-modal="true" aria-labelledby="item-details-title-%s">
                    <a class="modal-backdrop" href="#biblioteka-lista" aria-label="Zamknij szczegóły itemu"></a>
                    <div class="item-details-dialog">
                        <div class="modal-head">
                            <h3 id="item-details-title-%s">%s</h3>
                            <a class="modal-close" href="#biblioteka-lista" aria-label="Zamknij szczegóły itemu">×</a>
                        </div>
                        %s
                    </div>
                </section>
                """.formatted(
                item.getItemId(),
                item.getItemId(),
                item.getItemId(),
                escapeHtml(title),
                renderFullItemPreview(item)
        );
    }

    private static String renderFullItemPreview(SavedImportedItem item) {
        FullItemRead fullItemRead = item.getFullItemRead();
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return "<div class=\"status-note\">Brak zapisanego pełnego odczytu OCR dla tego itemu.</div>"
                    + renderTemperingDetails(item)
                    + MasterworkingSectionRenderer.renderReadonlySummary(
                    item.getMasterworking(),
                    item.getAffixes(),
                    item.getTemperingAffixes())
                    + TransfigurationSectionRenderer.renderReadonlySummary(item.getTransfiguration(), item.getAffixes())
                    + SocketingSectionRenderer.renderReadonlySummary(item);
        }
        List<String> baseStats = collectBaseStats(fullItemRead);
        List<String> implicitLines = collectLines(fullItemRead, ItemReadLineGroup.IMPLICIT);
        List<String> affixLines = item.getAffixes().stream()
                .map(affix -> formatAffixDetailsLine(item, affix))
                .toList();
        List<String> socketLines = collectLines(fullItemRead, ItemReadLineGroup.SOCKET);

        StringBuilder html = new StringBuilder("""
                <div class="item-read-details">
                    <section class="item-line-group item-line-group-basic">
                        <h5>Dane podstawowe</h5>
                    <div class="item-meta-grid">
                """);
        html.append(renderMeta("Nazwa itemu", emptyLabel(firstNonBlank(item.getItemName(), fullItemRead.getItemName()))))
                .append(renderMeta("Typ itemu", emptyLabel(firstNonBlank(item.getItemType(), simplifyItemType(fullItemRead.getItemTypeLine())))))
                .append(renderMeta("Slot ekwipunku", ItemLibraryPresentationSupport.slotDisplayName(item.getSlot())))
                .append(renderMeta("Rzadkość", simplifyRarity(firstNonBlank(item.getItemRarity(), fullItemRead.getRarity()))))
                .append(renderMeta("Ancient", item.isAncient() ? "true" : "false"))
                .append(renderMeta("Moc przedmiotu", item.getItemPower() == null ? simplifyItemPower(fullItemRead.getItemPower()) : Long.toString(item.getItemPower())));
        if (isShield(item)) {
            html.append(renderMetaHtml("Pancerz", formatArmorDetailsValue(item)));
        } else if (isWeapon(item)) {
            html.append(renderMeta("DPS broni", nullableLongLabel(item.getWeaponDps())))
                    .append(renderMeta("Obrażenia min/max", weaponRangeLabel(item)))
                    .append(renderMeta("Średnie obrażenia trafienia", nullableLongLabel(item.getAverageWeaponDamage())))
                    .append(renderMeta("Ataki na sekundę", item.getAttacksPerSecond() == null ? "Brak pewnego odczytu" : String.format(java.util.Locale.US, "%.2f", item.getAttacksPerSecond())));
        }
        html.append(renderMeta("Identyfikator", item.getDisplayName()))
                .append("</div></section>")
                .append(renderTextLineGroup("Base stats", baseStats))
                .append(renderTextLineGroup("Linie bazowe", implicitLines))
                .append(renderHtmlLineGroup("Affixy", affixLines))
                .append(renderTemperingDetails(item))
                .append(MasterworkingSectionRenderer.renderReadonlySummary(
                        item.getMasterworking(),
                        item.getAffixes(),
                        item.getTemperingAffixes()))
                .append(TransfigurationSectionRenderer.renderReadonlySummary(
                        item.getTransfiguration(),
                        item.getAffixes()))
                .append(SocketingSectionRenderer.renderReadonlySummary(item))
                .append(renderAspectDetails(item))
                .append(renderTextLineGroup("Socket / gniazdo", socketLines))
                .append("</div>");
        return html.toString();
    }

    private static String renderTemperingDetails(SavedImportedItem item) {
        if (item.getTemperingAffixes().isEmpty()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (ItemTemperingAffix affix : item.getTemperingAffixes()) {
            lines.add(affix.getCategory().getDisplayName()
                    + ": "
                    + formatTemperingDetailsLine(item, affix)
                    + (affix.isGreaterAffix() ? " | Greater Affix / Gwiazdka" : "")
                    + " | Status: "
                    + affix.getRuntimeStatus().getDisplayName());
        }
        return renderHtmlLineGroup("Hartowanie", lines);
    }

    private static String formatArmorDetailsValue(SavedImportedItem item) {
        if (item.getMasterworking() != null && item.getMasterworking().hasVisibleProgress()) {
            return MasterworkingSectionRenderer.formatArmorReadonlyValue(item.getMasterworking(), item.getItemArmor());
        }
        return nullableLongLabel(item.getItemArmor());
    }

    private static String formatAffixDetailsLine(SavedImportedItem item, ImportedItemAffix affix) {
        if (item.getMasterworking() != null && item.getMasterworking().hasVisibleProgress()) {
            return MasterworkingSectionRenderer.formatAffixReadonlyLine(item.getMasterworking(), affix);
        }
        return escapeHtml(ItemLibraryPresentationSupport.formatAffixForDetails(affix));
    }

    private static String formatTemperingDetailsLine(SavedImportedItem item, ItemTemperingAffix affix) {
        if (item.getMasterworking() != null && item.getMasterworking().hasVisibleProgress()) {
            return MasterworkingSectionRenderer.formatTemperingReadonlyLine(item.getMasterworking(), affix);
        }
        return escapeHtml(TemperingPresentationSupport.formatAffix(affix, ApplicationTemperingAffixRegistry.get()));
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

    private static String renderHtmlLineGroup(String heading, List<String> lines) {
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
            html.append("<li>").append(line).append("</li>");
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

    private static String renderAspectDetails(SavedImportedItem item) {
        List<String> lines = new ArrayList<>();
        if (!item.getSelectedAspectId().isBlank()) {
            AspectDefinition aspect = ASPECT_REGISTRY.findById(item.getSelectedAspectId()).orElse(null);
            if (aspect == null) {
                addUnique(lines, item.getSelectedAspectId());
            } else {
                addUnique(lines, aspect.getDisplayName());
                addUnique(lines, firstNonBlank(item.getUniqueEffectText(), aspect.getEffectDescription()));
            }
        } else {
            addUnique(lines, "Brak wybranego aspektu.");
        }
        if (!item.getSelectedAspectId().isBlank() && !item.getUniqueEffectText().isBlank()) {
            addUnique(lines, item.getUniqueEffectText());
        }
        return renderTextLineGroup("Aspekt / efekt", lines);
    }

    private static boolean isShield(SavedImportedItem item) {
        String normalizedType = normalizeForDisplayRules(item.getItemType());
        return item.getSlot() == krys.item.EquipmentSlot.OFF_HAND
                && (normalizedType.contains("TARCZA") || item.getItemArmor() != null);
    }

    private static boolean isWeapon(SavedImportedItem item) {
        String normalizedType = normalizeForDisplayRules(item.getItemType());
        return item.getSlot() == krys.item.EquipmentSlot.MAIN_HAND
                || normalizedType.contains("MIECZ")
                || normalizedType.contains("SWORD")
                || item.getWeaponDps() != null
                || item.getWeaponDamageMin() != null
                || item.getWeaponDamageMax() != null
                || item.getAverageWeaponDamage() != null
                || item.getAttacksPerSecond() != null;
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
        String itemName = ItemLibraryPresentationSupport.canonicalItemName(item);
        String editLink = "<a class=\"icon-action edit-action\" href=\""
                + escapeHtml(buildEditItemUrl(model, item))
                + "\" title=\"Edytuj\" aria-label=\"Edytuj item "
                + escapeHtml(itemName)
                + "\">✎<span class=\"sr-only\">Edytuj</span></a>";
        if (!model.hasActiveHero()) {
            return "<a class=\"icon-action assign-action\" href=\"/bohaterowie\" title=\"Wybierz bohatera\" aria-label=\"Wybierz bohatera, aby założyć item "
                    + escapeHtml(itemName)
                    + "\">⇧<span class=\"sr-only\">Wybierz bohatera</span></a>"
                    + editLink
                    + renderDeleteForm(model, item);
        }
        return renderAssignmentForms(model, item, true)
                + editLink
                + renderDeleteForm(model, item);
    }

    private static String renderAssignmentForms(ItemLibraryPageModel model, SavedImportedItem item) {
        return renderAssignmentForms(model, item, false);
    }

    private static String renderAssignmentForms(ItemLibraryPageModel model, SavedImportedItem item, boolean compact) {
        StringBuilder html = new StringBuilder("<div class=\"assign-actions\">");
        String itemName = ItemLibraryPresentationSupport.canonicalItemName(item);
        for (HeroEquipmentSlot heroSlot : HeroEquipmentSlot.compatibleWith(item.getSlot())) {
            Long selectedItemId = model.getActiveSelection().getSelectedItemId(heroSlot);
            boolean slotEmpty = selectedItemId == null;
            boolean thisItemSelected = selectedItemId != null && selectedItemId == item.getItemId();
            String actionLabel = slotEmpty ? "Załóż bohaterowi" : "Zmień w slocie";
            String heroSlotLabel = ItemLibraryPresentationSupport.heroSlotDisplayName(heroSlot);
            if (thisItemSelected) {
                if (compact) {
                    html.append("<span class=\"icon-action assign-action assign-action-selected\" title=\"Już założony w slocie ")
                            .append(escapeHtml(heroSlotLabel))
                            .append("\" aria-label=\"Item ")
                            .append(escapeHtml(itemName))
                            .append(" jest już założony w slocie ")
                            .append(escapeHtml(heroSlotLabel))
                            .append("\">✓<span class=\"sr-only\">Już założony</span></span>");
                } else {
                    html.append("<span class=\"helper\">Już założony w slocie ")
                            .append(escapeHtml(heroSlotLabel))
                            .append(".</span>");
                }
                continue;
            }
            String assignAriaLabel = slotEmpty
                    ? "Załóż item " + itemName + " bohaterowi w slocie " + heroSlotLabel
                    : "Zmień item w slocie " + heroSlotLabel + " na " + itemName;
            String assignIcon = slotEmpty ? "⇧" : "⇄";
            if (compact) {
                html.append("""
                        <form method="post" action="/biblioteka-itemow" class="inline-form action-form">
                            <input type="hidden" name="action" value="activateItem">
                            <input type="hidden" name="itemId" value="%s">
                            <input type="hidden" name="heroSlot" value="%s">
                            <input type="hidden" name="currentBuildQuery" value="%s">
                            %s
                            <button type="submit" class="icon-action assign-action" title="%s: %s" aria-label="%s">%s<span class="sr-only">%s</span></button>
                        </form>
                        """.formatted(
                        item.getItemId(),
                        heroSlot.name(),
                        escapeHtml(model.getCurrentBuildQuery()),
                        renderFilterHiddenFields(model.getFilter()),
                        escapeHtml(actionLabel),
                        escapeHtml(heroSlotLabel),
                        escapeHtml(assignAriaLabel),
                        escapeHtml(assignIcon),
                        escapeHtml(actionLabel)
                ));
            } else {
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
                        escapeHtml(heroSlotLabel)
                ));
            }
        }
        html.append("</div>");
        return html.toString();
    }

    private static String renderDeleteForm(ItemLibraryPageModel model, SavedImportedItem item) {
        String itemName = ItemLibraryPresentationSupport.canonicalItemName(item);
        return """
                <form method="post" action="/biblioteka-itemow" class="inline-form action-form">
                    <input type="hidden" name="action" value="deleteItem">
                    <input type="hidden" name="itemId" value="%s">
                    <input type="hidden" name="currentBuildQuery" value="%s">
                    %s
                    <button type="submit" class="icon-action delete-action" title="Usuń" aria-label="Usuń item %s">×<span class="sr-only">Usuń</span></button>
                </form>
                """.formatted(
                item.getItemId(),
                escapeHtml(model.getCurrentBuildQuery()),
                renderFilterHiddenFields(model.getFilter()),
                escapeHtml(itemName)
        );
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
                || normalized.contains("TA PREMIA JEST")
                || normalized.contains("UMIEJETNOSCI PODSTAWOWE")) {
            return ItemReadLineGroup.SPECIAL;
        }
        if (line.getType() == FullItemReadLineType.SOCKET) {
            return ItemReadLineGroup.SOCKET;
        }
        if (line.getType() == FullItemReadLineType.TEMPERING) {
            return ItemReadLineGroup.AFFIX;
        }
        if (line.getType() == FullItemReadLineType.AFFIX && !normalized.contains("ROZJUSZENIE")) {
            return ItemReadLineGroup.AFFIX;
        }
        return ItemReadLineGroup.OTHER;
    }

    private static String simplifyItemType(String itemTypeLine) {
        String normalized = normalizeForDisplayRules(itemTypeLine);
        if (normalized.contains("MIECZ") || normalized.contains("SWORD")) {
            return "Miecz";
        }
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

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private static String nullableLongLabel(Long value) {
        return value == null ? "Brak pewnego odczytu" : Long.toString(value);
    }

    private static String weaponRangeLabel(SavedImportedItem item) {
        if (item.getWeaponDamageMin() == null || item.getWeaponDamageMax() == null) {
            return "Brak pewnego odczytu";
        }
        return item.getWeaponDamageMin() + " - " + item.getWeaponDamageMax();
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

    private static String renderMetaHtml(String label, String valueHtml) {
        return """
                <div class="item-meta">
                    <span>%s</span>
                    <strong>%s</strong>
                </div>
                """.formatted(escapeHtml(label), valueHtml);
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
