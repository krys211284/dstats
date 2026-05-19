package krys.web;

import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.item.ItemStat;
import krys.itemimport.AffixDefinition;
import krys.itemimport.AffixRegistry;
import krys.itemimport.ApplicationAffixRegistry;
import krys.itemimport.ApplicationAspectRegistry;
import krys.itemimport.AspectDefinition;
import krys.itemimport.AspectRegistry;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ItemImportFieldCandidate;
import krys.itemimport.ValidatedImportedItem;
import krys.itemlibrary.ItemLibraryPresentationSupport;
import krys.itemlibrary.SavedImportedItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renderuje prosty SSR dla flow importu pojedynczego itemu ze screena z ręcznym zatwierdzeniem. */
public final class ItemImportPageRenderer {
    private final String template;
    private static final AspectRegistry ASPECT_REGISTRY = ApplicationAspectRegistry.get();
    private static final AffixRegistry AFFIX_REGISTRY = ApplicationAffixRegistry.get();

    public ItemImportPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(ItemImportPageModel model) {
        return template
                .replace("{{APP_SHELL_STYLES}}", AppShellRendererSupport.renderSharedStyles())
                .replace("{{GLOBAL_NAV}}", AppShellRendererSupport.renderGlobalNavigation("/importuj-item-ze-screena"))
                .replace("{{HERO_CONTEXT}}", renderHeroContext(model))
                .replace("{{FORM_ERRORS}}", renderErrors(model.getValidationErrors()))
                .replace("{{HELP_TEXT}}", escapeHtml(model.getHelpText()))
                .replace("{{UPLOAD_ACTION}}", escapeHtml(buildUploadAction(model.getCurrentBuildQuery())))
                .replace("{{PARSE_SECTION}}", renderParseSection(model))
                .replace("{{CONFIRM_SECTION}}", renderConfirmSection(model));
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

    private static String renderParseSection(ItemImportPageModel model) {
        if (!model.hasActiveHero()) {
            return """
                    <section class="panel result-panel">
                        <h2>Brak aktywnego bohatera</h2>
                        <div class="empty-state">
                            <h3>Najpierw wybierz bohatera</h3>
                            <p>Import pojedynczego itemu działa w kontekście aktywnego bohatera. Utwórz pierwszego bohatera albo ustaw istniejącego, aby po imporcie wiedzieć, dla kogo zapisujesz item i czy chcesz przypisać go do jego ekwipunku.</p>
                            <a class="link-button" href="/bohaterowie">Przejdź do modułu Bohaterowie</a>
                        </div>
                    </section>
                    """;
        }
        if (model.hasConfirmedImport()) {
            return "";
        }
        if (!model.hasEditableForm()) {
            return """
                    <section class="panel result-panel">
                        <h2>Wstępnie rozpoznane pola</h2>
                        <div class="empty-state">
                            <h3>Tu pojawią się rozpoznane pola itemu</h3>
                            <p>Po wgraniu screena aplikacja pokaże w tym miejscu wstępny odczyt slotu i statów wraz z poziomem niepewności. Następnie ręcznie potwierdzisz lub poprawisz wartości przed zapisaniem itemu.</p>
                        </div>
                    </section>
                    """;
        }

        ItemImageImportCandidateParseResult parseResult = model.getParseResult();
        ItemImportEditableForm form = model.getEditableForm();
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Wstępnie rozpoznane pola</h2>
                """);

        if (model.hasParseResult()) {
            html.append("<div class=\"summary-grid\">")
                    .append(renderSummaryCard("Plik źródłowy", parseResult.getImageMetadata().getOriginalFilename()))
                    .append(renderSummaryCard("Format", parseResult.getImageMetadata().getFormat()))
                    .append(renderSummaryCard("Content-Type", parseResult.getImageMetadata().getContentType()))
                    .append(renderSummaryCard("Rozmiar obrazu", parseResult.getImageMetadata().getWidth() + " x " + parseResult.getImageMetadata().getHeight()))
                    .append("</div>")
                    .append("<p class=\"helper\">")
                    .append(escapeHtml(parseResult.getImportNotice()))
                    .append("</p>")
                    .append(renderFullItemReadSection(parseResult.getFullItemRead(), "Pełny odczyt widocznego itemu"));
        } else if (form.getFullItemRead().hasAnyData()) {
            html.append(renderFullItemReadSection(form.getFullItemRead(), "Pełny odczyt widocznego itemu"));
        }

        html.append("""
                    <section class="subpanel">
                        <h3>Ręczne potwierdzenie itemu</h3>
                        <form method="post" action="/importuj-item-ze-screena">
                """)
                .append(renderHiddenField("sourceImageName", form.getSourceImageName()))
                .append(renderHiddenField("currentBuildQuery", model.getCurrentBuildQuery()))
                .append(renderHiddenField("fullItemRead", FullItemReadFormCodec.encode(form.getFullItemRead())))
                .append(renderHiddenField("weaponDamage", emptyNumberLabel(form.getWeaponDamage())))
                .append(renderHiddenField("ocrSuggestedAspectId", form.getOcrSuggestedAspectId()))
                .append(renderHiddenField("ocrAspectConfidence", form.getOcrAspectConfidence().name()))
                .append("""
                            <div class="manual-confirm-grid">
                """)
                .append(renderItemIdentityFields(form))
                .append(renderSlotSelect(form.getSlot(), isShield(form.getDetails())))
                .append(renderRaritySelect(form.getItemRarity()))
                .append(renderAncientCheckbox(form.isAncient()))
                .append(renderNumberField("itemPower", "Moc przedmiotu", form.getItemPower(), "1"))
                .append(renderItemTypeFieldSet(form))
                .append(renderAspectSelect(form))
                .append("""
                            </div>
                """)
                .append(renderAffixEditor(form))
                .append("""
                            <div class="submit-row">
                                <button type="submit" name="formAction" value="confirmItem">Zatwierdź item</button>
                            </div>
                        </form>
                    </section>
                </section>
                """);
        return html.toString();
    }

    private static String renderConfirmSection(ItemImportPageModel model) {
        if (!model.hasActiveHero()) {
            return "";
        }
        if (!model.hasConfirmedImport()) {
            return "";
        }

        ItemImportPageModel.ConfirmedImportView confirmed = model.getConfirmedImportView();
        ValidatedImportedItem importedItem = confirmed.getImportedItem();
        SavedImportedItem savedItem = confirmed.getSavedItem();
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Zatwierdzony item zapisany do biblioteki</h2>
                    <div class="summary-grid">
                """);
        html.append(renderSummaryCard("Plik źródłowy", importedItem.getSourceImageName()))
                .append(renderSummaryCard("Aktywny bohater", model.getActiveHero().getName()))
                .append(renderSummaryCard("Slot / typ itemu", ItemLibraryPresentationSupport.slotDisplayName(importedItem.getSlot())))
                .append(renderSummaryCard("Identyfikator biblioteki", ItemLibraryPresentationSupport.userItemIdentifier(savedItem)))
                .append(renderSummaryCard("Wybrany aspekt", selectedAspectLabel(importedItem.getSelectedAspectId())))
                .append("</div>")
                .append(renderFullItemReadSection(savedItem.getFullItemRead(), "Pełny odczyt zapisany w bibliotece"))
                .append("""
                    <section class="subpanel">
                        <h3>Dalsze akcje</h3>
                        <p class="helper">Import pozostaje wspomagany: zatwierdzony item został zapisany do biblioteki przez jawnie widoczny formularz korekty, a nie przez ukryty automat OCR.</p>
                        <div class="action-links">
                    """)
                .append(renderAssignSavedItemForms(model, savedItem))
                .append("<a class=\"link-button secondary-button\" href=\"/biblioteka-itemow\">Przejdź do biblioteki</a>")
                .append("<a class=\"link-button secondary-button\" href=\"")
                .append(escapeHtml(buildCurrentBuildUrl(model.getCurrentBuildQuery())))
                .append("\">Wróć do aktualnego buildu</a>")
                .append("""
                        </div>
                    """)
                .append("""
                    </section>
                </section>
                """);
        return html.toString();
    }

    private static String renderFullItemReadSection(FullItemRead fullItemRead, String heading) {
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return """
                    <section class="subpanel">
                        <h3>%s</h3>
                        <div class="empty-state">
                            <p>OCR nie dostarczył stabilnych linii pełnego odczytu itemu. Foundation mapping nadal można potwierdzić ręcznie.</p>
                        </div>
                    </section>
                    """.formatted(escapeHtml(heading));
        }
        StringBuilder html = new StringBuilder("""
                <section class="subpanel">
                    <h3>%s</h3>
                    <div class="item-read-header">
                """.formatted(escapeHtml(heading)));
        ItemImportDetails details = fullItemRead.getDetails();
        html.append(renderItemHeaderField("Nazwa", emptyLabel(firstNonBlank(details.getItemName(), fullItemRead.getItemName()))))
                .append(renderItemHeaderField("Typ", emptyLabel(firstNonBlank(details.getItemType(), simplifyItemType(fullItemRead.getItemTypeLine())))))
                .append(renderItemHeaderField("Rzadkość", simplifyRarity(firstNonBlank(details.getItemRarity(), fullItemRead.getRarity()))))
                .append(renderItemHeaderField("Ancient", details.isAncient() ? "true" : "false"))
                .append(renderItemHeaderField("Slot", slotDisplayName(details)))
                .append(renderItemHeaderField("Moc przedmiotu", details.getItemPower() == null ? simplifyItemPower(fullItemRead.getItemPower()) : Long.toString(details.getItemPower())))
                .append(renderItemTypeSummaryFields(details))
                .append(renderBaseValueHeader(fullItemRead))
                .append("</div>")
                .append("""
                    <div class="item-read-groups">
                        <h4>Pełny zapis itemu</h4>
                    """)
                .append(renderLineGroup("Linie bazowe", groupedLines(fullItemRead, ItemReadLineGroup.IMPLICIT)))
                .append(renderLineGroup("Dodatkowe / sezonowe linie", groupedLines(fullItemRead, ItemReadLineGroup.OTHER)))
                .append(renderLineGroup("Socket / gniazdo", groupedLines(fullItemRead, ItemReadLineGroup.SOCKET)))
                .append("</div>")
                .append("</section>");
        return html.toString();
    }

    private static String renderItemHeaderField(String label, String value) {
        return """
                <div class="item-header-field">
                    <div class="summary-label">%s</div>
                    <div class="summary-value">%s</div>
                </div>
                """.formatted(escapeHtml(label), escapeHtml(value));
    }

    private static String renderBaseValueHeader(FullItemRead fullItemRead) {
        if (fullItemRead == null || fullItemRead.getBaseItemValue().isBlank()) {
            return "";
        }
        if (fullItemRead.getDetails().getWeaponDps() != null
                || fullItemRead.getDetails().getWeaponDamageMin() != null
                || fullItemRead.getDetails().getWeaponDamageMax() != null
                || fullItemRead.getDetails().getItemArmor() != null) {
            return "";
        }
        return renderItemHeaderField(baseValueLabel(fullItemRead.getBaseItemValue()), simplifyBaseValue(fullItemRead.getBaseItemValue()));
    }

    private static String renderItemTypeSummaryFields(ItemImportDetails details) {
        if (details == null || !details.hasAnyData()) {
            return "";
        }
        if (isShield(details)) {
            return renderItemHeaderField("Pancerz", nullableLongLabel(details.getItemArmor()));
        }
        if (!isWeapon(details)) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        html.append(renderItemHeaderField("DPS broni", nullableLongLabel(details.getWeaponDps())));
        html.append(renderItemHeaderField("Obrażenia za trafienie min", nullableLongLabel(details.getWeaponDamageMin())));
        html.append(renderItemHeaderField("Obrażenia za trafienie max", nullableLongLabel(details.getWeaponDamageMax())));
        html.append(renderItemHeaderField("Średnie obrażenia trafienia", nullableLongLabel(details.getAverageWeaponDamage())));
        html.append(renderItemHeaderField("Ataki na sekundę", details.getAttacksPerSecond() == null
                ? "Brak pewnego odczytu"
                : String.format(Locale.US, "%.2f", details.getAttacksPerSecond())));
        return html.toString();
    }

    private static String renderLineGroup(String heading, List<FullItemReadLine> lines) {
        return renderLineGroup(heading, lines, false);
    }

    private static String renderLineGroup(String heading, List<FullItemReadLine> lines, boolean primary) {
        if (lines.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("""
                <section class="item-line-group%s">
                    <h5>%s</h5>
                    <ul class="item-line-list">
                """.formatted(primary ? " item-line-group-primary" : "", escapeHtml(heading)));
        for (FullItemReadLine line : lines) {
            html.append("<li>").append(escapeHtml(line.getText())).append("</li>");
        }
        html.append("</ul></section>");
        return html.toString();
    }

    private static List<FullItemReadLine> groupedLines(FullItemRead fullItemRead, ItemReadLineGroup group) {
        List<FullItemReadLine> lines = new ArrayList<>();
        for (FullItemReadLine line : fullItemRead.getLines()) {
            if (classifyPresentationLine(line) == group) {
                lines.add(line);
            }
        }
        return lines;
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
        if (normalized.equals("LEGENDARY") || normalized.contains("LEGENDARNA") || normalized.contains("LEGENDARNY") || normalized.contains("LEGENDARY")) {
            parts.add("legendarna");
        } else if (normalized.equals("UNIQUE") || normalized.contains("UNIKATOWA") || normalized.contains("UNIKATOWY") || normalized.contains("UNIQUE")) {
            parts.add("unikatowa");
        } else if (normalized.equals("RARE") || normalized.contains("RZADKA") || normalized.contains("RZADKI") || normalized.contains("RARE")) {
            parts.add("rzadka");
        }
        if (!parts.isEmpty()) {
            return String.join(" ", parts);
        }
        return emptyLabel(rarity);
    }

    private static String simplifyItemPower(String itemPower) {
        String value = firstNumber(itemPower);
        if ("1".equals(value)) {
            return "Brak pewnego odczytu";
        }
        return value.isBlank() ? emptyLabel(itemPower) : value;
    }

    private static String baseValueLabel(String baseItemValue) {
        String normalized = normalizeForDisplayRules(baseItemValue);
        if (normalized.contains("PANCERZ") || normalized.contains("ARMOR")) {
            return "Pancerz";
        }
        if (normalized.contains("OBRAZEN") || normalized.contains("DAMAGE")) {
            return "Bazowe obrażenia";
        }
        return "Bazowa wartość";
    }

    private static String simplifyBaseValue(String baseItemValue) {
        String value = firstNumber(baseItemValue);
        String normalized = normalizeForDisplayRules(baseItemValue);
        if ("1".equals(value) && (normalized.contains("OBRAZEN") || normalized.contains("DAMAGE"))) {
            return "Brak pewnego odczytu";
        }
        return value.isBlank() ? emptyLabel(baseItemValue) : value;
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

    private static String renderAffixDisplayValue(ImportedItemAffix affix) {
        if (affix == null || affix.getDisplayValue().isBlank()) {
            return "";
        }
        return "<div class=\"helper\">" + escapeHtml(affix.getDisplayValue()) + "</div>";
    }

    private static String renderAffixValueControl(int index, ImportedItemAffix affix) {
        return "<input type=\"number\" min=\"0\" step=\"0.01\" name=\"affixValue_"
                + index
                + "\" value=\""
                + escapeHtml(formatDecimal(affix == null ? 0.0d : affix.getValue()))
                + "\">";
    }

    private static String rollRangeLabel(ImportedItemAffix affix) {
        if (affix != null && affix.isGreaterAffix()
                && affix.getRollRangeMin() == null
                && affix.getRollRangeMax() == null) {
            return "Bez zakresu (Greater Affix)";
        }
        String value = affix == null ? "" : affix.getRollRangeLabel();
        return value == null || value.isBlank() ? "Brak zakresu" : value;
    }

    private static String normalizeForDisplayRules(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String renderAssignSavedItemForms(ItemImportPageModel model, SavedImportedItem savedItem) {
        StringBuilder html = new StringBuilder();
        for (HeroEquipmentSlot heroSlot : HeroEquipmentSlot.compatibleWith(savedItem.getSlot())) {
            boolean slotEmpty = model.getActiveHero().getItemSelection().getSelectedItemId(heroSlot) == null;
            String actionLabel = slotEmpty ? "Załóż bohaterowi" : "Zmień w slocie";
            html.append("""
                    <form method="post" action="/biblioteka-itemow" class="inline-form">
                        <input type="hidden" name="action" value="activateItem">
                        <input type="hidden" name="itemId" value="%s">
                        <input type="hidden" name="heroSlot" value="%s">
                        <input type="hidden" name="currentBuildQuery" value="%s">
                        <button type="submit">%s: %s</button>
                    </form>
                    """.formatted(
                    savedItem.getItemId(),
                    heroSlot.name(),
                    escapeHtml(model.getCurrentBuildQuery()),
                    escapeHtml(actionLabel),
                    escapeHtml(ItemLibraryPresentationSupport.heroSlotDisplayName(heroSlot))
            ));
        }
        return html.toString();
    }

    private static String renderAffixEditor(ItemImportEditableForm form) {
        StringBuilder html = new StringBuilder("""
                <section class="subpanel">
                    <h3>Ręczna weryfikacja affixów</h3>
                    <input type="hidden" id="affixCount" name="affixCount" value="%s">
                    <div class="affix-table-wrap">
                    <table class="data-table affix-table" id="affixTable">
                        <colgroup>
                            <col class="affix-type-col">
                            <col class="affix-value-col">
                            <col class="affix-range-col">
                            <col class="affix-greater-col">
                            <col class="affix-action-col">
                        </colgroup>
                        <thead>
                            <tr>
                                <th>Typ affixu</th>
                                <th>Wartość</th>
                                <th>Zakres rolla</th>
                                <th>Greater Affix</th>
                                <th>Akcja</th>
                            </tr>
                        </thead>
                        <tbody id="affixRows">
                """.formatted(form.getAffixes().size()));
        for (int index = 0; index < form.getAffixes().size(); index++) {
            ImportedItemAffix affix = form.getAffixes().get(index);
            html.append("""
                    <tr>
                        <td class="affix-type-cell">
                            <select name="affixType_%s">%s</select>
                            <input type="hidden" name="affixSourceText_%s" value="%s">
                            <input type="hidden" name="affixOriginalType_%s" value="%s">
                            <input type="hidden" name="affixOriginalValue_%s" value="%s">
                            <input type="hidden" name="affixDefinitionId_%s" value="%s">
                            <input type="hidden" name="affixRangeMin_%s" value="%s">
                            <input type="hidden" name="affixRangeMax_%s" value="%s">
                            <input type="hidden" name="affixDisplayValue_%s" value="%s">
                        </td>
                        <td class="affix-value-cell">
                            %s
                        </td>
                        <td class="affix-range-cell">
                            %s
                        </td>
                        <td class="affix-greater-cell">
                            <label class="checkbox-label"><input type="checkbox" name="affixGreater_%s" value="true"%s> Gwiazdka</label>
                        </td>
                        <td class="affix-action-cell"><button type="button" class="secondary-button remove-affix-button">Usuń</button></td>
                    </tr>
                    """.formatted(
                    index,
                    renderAffixTypeOptions(affix.getType()),
                    index,
                    escapeHtml(affix.getSourceText()),
                    index,
                    escapeHtml(affix.getType().name()),
                    index,
                    formatDecimal(affix.getValue()),
                    index,
                    escapeHtml(affix.getAffixDefinitionId()),
                    index,
                    affix.getRollRangeMin() == null ? "" : formatDecimal(affix.getRollRangeMin()),
                    index,
                    affix.getRollRangeMax() == null ? "" : formatDecimal(affix.getRollRangeMax()),
                    index,
                    escapeHtml(affix.getDisplayValue()),
                    renderAffixValueControl(index, affix),
                    escapeHtml(rollRangeLabel(affix)),
                    index,
                    affix.isGreaterAffix() ? " checked" : ""
            ));
        }
        html.append("""
                        </tbody>
                    </table>
                    </div>
                    <div class="add-affix-row">
                        <h4>Dodaj affix</h4>
                        <div class="item-affix-add-grid">
                            <label>
                                Typ affixu
                                <select name="newAffixType">
                                    <option value="">Nie dodawaj</option>
                                    %s
                                </select>
                            </label>
                            <label>
                                Wartość
                                <input type="number" min="0" step="0.01" name="newAffixValue" value="">
                            </label>
                            <label class="checkbox-label">
                                <input type="checkbox" id="newAffixGreater" value="true"> Greater Affix
                            </label>
                            <div class="item-affix-add-actions">
                                <button type="button" id="addAffixButton">Dodaj affix</button>
                                <noscript>
                                    <button type="submit" name="formAction" value="addAffix">Dodaj affix</button>
                                </noscript>
                            </div>
                        </div>
                    </div>
                    <template id="affixRowTemplate">
                        <tr>
                            <td class="affix-type-cell"><select name="affixType___INDEX__">%s</select><input type="hidden" name="affixSourceText___INDEX__" value=""><input type="hidden" name="affixDefinitionId___INDEX__" value=""><input type="hidden" name="affixRangeMin___INDEX__" value=""><input type="hidden" name="affixRangeMax___INDEX__" value=""><input type="hidden" name="affixDisplayValue___INDEX__" value=""></td>
                            <td class="affix-value-cell"><input type="number" min="0" step="0.01" name="affixValue___INDEX__" value="__VALUE__"></td>
                            <td class="affix-range-cell"><span class="helper">Brak zakresu</span></td>
                            <td class="affix-greater-cell"><label class="checkbox-label"><input type="checkbox" name="affixGreater___INDEX__" value="true"> Gwiazdka</label></td>
                            <td class="affix-action-cell"><button type="button" class="secondary-button remove-affix-button">Usuń</button></td>
                        </tr>
                    </template>
                </section>
                """.formatted(
                renderAffixTypeOptions(null),
                renderAffixTypeOptions(null)
        ));
        return html.toString();
    }

    private static String renderAspectSelect(ItemImportEditableForm form) {
        EquipmentSlot selectedSlot = parseSlot(form.getSlot());
        String selectedAspectId = form.getSelectedAspectId();
        AspectDefinition selectedAspect = selectedAspectId == null || selectedAspectId.isBlank()
                ? null
                : ASPECT_REGISTRY.findById(selectedAspectId).orElse(null);
        boolean selectedAspectKnown = selectedAspectId != null
                && !selectedAspectId.isBlank()
                && selectedAspect != null;
        boolean selectedAspectAllowed = selectedAspectKnown
                && selectedAspect.allowsSlot(selectedSlot);
        StringBuilder html = new StringBuilder("""
                <label>
                    Aspekt
                    <select name="selectedAspectId" id="aspectSelect">
                        <option value="%s"%s>Brak wybranego aspektu</option>
                """.formatted("", selectedAspectId == null || selectedAspectId.isBlank() ? " selected" : ""));
        for (AspectDefinition aspect : ASPECT_REGISTRY.all()) {
            boolean allowed = selectedSlot != null && aspect.allowsSlot(selectedSlot);
            boolean selected = aspect.getId().equals(selectedAspectId);
            html.append("<option value=\"")
                    .append(escapeHtml(aspect.getId()))
                    .append("\"")
                    .append(" data-allowed-slots=\"")
                    .append(escapeHtml(allowedSlotNames(aspect)))
                    .append("\"")
                    .append(selected ? " selected" : "")
                    .append(!allowed && !selected ? " disabled hidden" : "")
                    .append(">")
                    .append(escapeHtml(aspect.getDisplayName()))
                    .append(selected && !allowed ? " (niezgodny ze slotem)" : "")
                    .append("</option>");
        }
        html.append("""
                    </select>
                """);
        if (selectedAspect == null) {
            html.append("<span class=\"helper\">Brak wybranego aspektu.</span>");
        }
        if (selectedAspect == null && hasAspectText(form.getFullItemRead())) {
            html.append("<span class=\"helper\">OCR wykrył tekst aspektu, ale nie znaleziono dopasowania w katalogu aspektów. Wybierz ręcznie albo zostaw brak.</span>");
        }
        if (selectedAspectKnown && !selectedAspectAllowed) {
            html.append("<span class=\"helper\">Wybrany aspekt nie pasuje do obecnego slotu itemu i wymaga zmiany przed zapisem.</span>");
        }
        html.append("""
                </label>
                <label class="aspect-effect-text">
                    Treść efektu
                    <textarea name="uniqueEffectText" rows="4">%s</textarea>
                </label>
                """.formatted(escapeHtml(aspectEffectText(form, selectedAspect))));
        return "<fieldset class=\"inline-fieldset aspect-effect-fieldset\"><legend>Aspekt / efekt</legend>"
                + html
                + "</fieldset>";
    }

    private static String aspectEffectText(ItemImportEditableForm form, AspectDefinition selectedAspect) {
        String effectText = firstNonBlank(form.getUniqueEffectText(), form.getFullItemRead().getDetails().getUniqueEffectText());
        if (effectText.isBlank() && selectedAspect != null) {
            return selectedAspect.getEffectDescription();
        }
        return effectText;
    }

    private static String selectedAspectLabel(String selectedAspectId) {
        if (selectedAspectId == null || selectedAspectId.isBlank()) {
            return "Brak";
        }
        return ASPECT_REGISTRY.findById(selectedAspectId)
                .map(AspectDefinition::getDisplayName)
                .orElse(selectedAspectId);
    }

    private static EquipmentSlot parseSlot(String rawSlot) {
        if (rawSlot == null || rawSlot.isBlank()) {
            return null;
        }
        try {
            return EquipmentSlot.valueOf(rawSlot);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
    private static String allowedSlotNames(AspectDefinition aspect) {
        return aspect.getAllowedItemSlots().stream()
                .map(EquipmentSlot::name)
                .sorted()
                .reduce("", (left, right) -> left.isBlank() ? right : left + "," + right);
    }

    private static boolean hasAspectText(FullItemRead fullItemRead) {
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return false;
        }
        for (FullItemReadLine line : fullItemRead.getLines()) {
            String normalized = normalizeForDisplayRules(line.getText());
            if (line.getType() == FullItemReadLineType.ASPECT
                    || normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE")
                    || normalized.contains("TA PREMIA JEST")) {
                return true;
            }
        }
        return false;
    }

    private static String renderItemIdentityFields(ItemImportEditableForm form) {
        return """
                <label>
                    Nazwa
                    <input type="text" name="itemName" value="%s">
                </label>
                <label>
                    Typ itemu
                    <input type="text" name="itemType" value="%s">
                </label>
                """.formatted(
                escapeHtml(firstNonBlank(form.getItemName(), form.getFullItemRead().getItemName())),
                escapeHtml(firstNonBlank(form.getItemType(), simplifyItemType(form.getFullItemRead().getItemTypeLine())))
        );
    }

    private static String renderRaritySelect(String selectedRarity) {
        String selected = selectedRarity == null ? "" : selectedRarity;
        return """
                <label>
                    Rzadkość
                    <select name="itemRarity">
                        %s
                    </select>
                </label>
                """.formatted(
                renderRarityOption("", "UNKNOWN / do potwierdzenia", selected.isBlank())
                        + renderRarityOption("UNIQUE", "UNIQUE / Unikatowy", "UNIQUE".equalsIgnoreCase(selected))
                        + renderRarityOption("LEGENDARY", "LEGENDARY / Legendarny", "LEGENDARY".equalsIgnoreCase(selected))
                        + renderRarityOption("RARE", "RARE / Rzadki", "RARE".equalsIgnoreCase(selected))
        );
    }

    private static String renderRarityOption(String value, String label, boolean selected) {
        return "<option value=\"" + escapeHtml(value) + "\"" + (selected ? " selected" : "") + ">"
                + escapeHtml(label) + "</option>";
    }

    private static String renderAncientCheckbox(boolean ancient) {
        return """
                <label class="checkbox-label">
                    <input type="checkbox" name="isAncient" value="true"%s> Ancient / starożytny
                </label>
                """.formatted(ancient ? " checked" : "");
    }

    private static String renderItemTypeFieldSet(ItemImportEditableForm form) {
        if (isShield(form.getDetails())) {
            return renderShieldFieldSet(form);
        }
        if (!isWeapon(form.getDetails())) {
            return renderHiddenField("itemArmor", form.getItemArmor());
        }
        return renderWeaponFieldSet(form);
    }

    private static String renderShieldFieldSet(ItemImportEditableForm form) {
        StringBuilder implicitLines = new StringBuilder();
        for (FullItemReadLine line : form.getFullItemRead().getLines()) {
            if (classifyPresentationLine(line) == ItemReadLineGroup.IMPLICIT) {
                implicitLines.append("<li>").append(escapeHtml(line.getText())).append("</li>");
            }
        }
        return """
                <fieldset class="inline-fieldset">
                    <legend>Dane tarczy</legend>
                    %s
                    <section class="item-line-group">
                        <h5>Linie bazowe</h5>
                        <ul class="item-line-list">%s</ul>
                    </section>
                </fieldset>
                """.formatted(
                renderNumberField("itemArmor", "Pancerz", form.getItemArmor(), "1"),
                implicitLines
        );
    }

    private static String renderWeaponFieldSet(ItemImportEditableForm form) {
        String averageValue = form.getAverageWeaponDamage();
        if ((averageValue == null || averageValue.isBlank())
                && !form.getWeaponDamageMin().isBlank()
                && !form.getWeaponDamageMax().isBlank()) {
            try {
                long min = Long.parseLong(form.getWeaponDamageMin());
                long max = Long.parseLong(form.getWeaponDamageMax());
                averageValue = Long.toString(Math.round((min + max) / 2.0d));
            } catch (NumberFormatException exception) {
                averageValue = form.getAverageWeaponDamage();
            }
        }
        return """
                <fieldset class="inline-fieldset">
                    <legend>Dane broni</legend>
                    %s
                    %s
                    %s
                    %s
                    %s
                </fieldset>
                """.formatted(
                renderNumberField("weaponDps", "DPS broni", form.getWeaponDps(), "1"),
                renderNumberField("weaponDamageMin", "Obrażenia za trafienie min", form.getWeaponDamageMin(), "1"),
                renderNumberField("weaponDamageMax", "Obrażenia za trafienie max", form.getWeaponDamageMax(), "1"),
                renderNumberField("averageWeaponDamage", "Średnie obrażenia trafienia", averageValue, "1"),
                renderNumberField("attacksPerSecond", "Ataki na sekundę", form.getAttacksPerSecond(), "0.01")
        );
    }

    private static boolean isShield(ItemImportDetails details) {
        if (details == null) {
            return false;
        }
        String normalizedType = normalizeForDisplayRules(details.getItemType());
        return details.getEquipmentSlot() == EquipmentSlot.OFF_HAND
                && (normalizedType.contains("TARCZA") || details.getItemArmor() != null);
    }

    private static boolean isWeapon(ItemImportDetails details) {
        if (details == null) {
            return false;
        }
        String normalizedType = normalizeForDisplayRules(details.getItemType());
        return details.getEquipmentSlot() == EquipmentSlot.MAIN_HAND
                || normalizedType.contains("MIECZ")
                || normalizedType.contains("SWORD")
                || details.getWeaponDps() != null
                || details.getWeaponDamageMin() != null
                || details.getWeaponDamageMax() != null
                || details.getAverageWeaponDamage() != null
                || details.getAttacksPerSecond() != null;
    }

    private static String slotDisplayName(ItemImportDetails details) {
        if (details == null || details.getEquipmentSlot() == null) {
            return "Brak pewnego odczytu";
        }
        if (isShield(details)) {
            return "Tarcza";
        }
        return ItemLibraryPresentationSupport.slotDisplayName(details.getEquipmentSlot());
    }

    private static String renderAffixTypeOptions(ImportedItemAffixType selectedType) {
        StringBuilder html = new StringBuilder();
        for (AffixDefinition definition : AFFIX_REGISTRY.all()) {
            ImportedItemAffixType type = definition.getFormType();
            html.append("<option value=\"")
                    .append(type.name())
                    .append("\"")
                    .append(" data-affix-definition-id=\"")
                    .append(escapeHtml(definition.getId()))
                    .append("\"")
                    .append(" title=\"")
                    .append(escapeHtml(definition.getDescription()))
                    .append("\"")
                    .append(" aria-label=\"")
                    .append(escapeHtml(definition.getDescription()))
                    .append("\"")
                    .append(type == selectedType ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(definition.getDisplayName()))
                    .append("</option>");
        }
        return html.toString();
    }

    private static String renderCandidateRow(String label, ItemImportFieldCandidate<?> candidate) {
        String suggestedValue = renderCandidateValue(candidate.getSuggestedValue());
        String note = candidate.getNote() == null || candidate.getNote().isBlank() ? "-" : candidate.getNote();
        return "<tr><td>" + escapeHtml(label) + "</td><td>" + escapeHtml(suggestedValue) + "</td><td>"
                + escapeHtml(candidate.getConfidence().getDisplayName()) + "</td><td>" + escapeHtml(note) + "</td></tr>";
    }

    private static String renderSlotSelect(String selectedSlot, boolean shieldContext) {
        StringBuilder html = new StringBuilder("""
                <label>
                    Slot ekwipunku
                    <select name="slot" id="itemSlotSelect">
                """);
        html.append(renderSlotOption("", "Wybierz slot", selectedSlot == null || selectedSlot.isBlank()));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String label = shieldContext && slot == EquipmentSlot.OFF_HAND
                    ? "Tarcza"
                    : ItemLibraryPresentationSupport.slotDisplayName(slot);
            html.append(renderSlotOption(slot.name(), label, slot.name().equals(selectedSlot)));
        }
        html.append("""
                    </select>
                </label>
                """);
        return html.toString();
    }

    private static String renderSlotOption(String value, String label, boolean selected) {
        return "<option value=\"" + escapeHtml(value) + "\"" + (selected ? " selected" : "") + ">"
                + escapeHtml(label) + "</option>";
    }

    private static String renderNumberField(String name, String label, String value, String step) {
        return "<label>"
                + escapeHtml(label)
                + "<input type=\"number\" min=\"0\" step=\""
                + escapeHtml(step)
                + "\" name=\""
                + escapeHtml(name)
                + "\" value=\""
                + escapeHtml(value)
                + "\"></label>";
    }

    private static String renderHiddenField(String name, String value) {
        return "<input type=\"hidden\" name=\"" + escapeHtml(name) + "\" value=\"" + escapeHtml(value) + "\">";
    }

    private static String buildUploadAction(String currentBuildQuery) {
        if (currentBuildQuery == null || currentBuildQuery.isBlank()) {
            return "/importuj-item-ze-screena";
        }
        return "/importuj-item-ze-screena?" + currentBuildQuery;
    }

    private static String buildCurrentBuildUrl(String currentBuildQuery) {
        if (currentBuildQuery == null || currentBuildQuery.isBlank()) {
            return "/policz-aktualny-build";
        }
        return "/policz-aktualny-build?" + currentBuildQuery;
    }

    private static String renderSummaryCard(String label, String value) {
        return CurrentBuildCalculationSectionsRenderer.renderSummaryCard(label, value);
    }

    private static String renderHeroContext(ItemImportPageModel model) {
        if (!model.hasActiveHero()) {
            return """
                    <section class="panel panel-error">
                        <h2>Brak aktywnego bohatera</h2>
                        <p>Ten moduł importu jest przygotowany pod pracę w kontekście bohatera i jego buildu. Bez aktywnego bohatera nie pokażemy dalszych akcji przypisania itemu.</p>
                    </section>
                    """;
        }
        return """
                <section class="panel">
                    <h2>Aktywny bohater importu</h2>
                    <p class="helper">Importujesz teraz item dla bohatera %s. Po zapisaniu do biblioteki możesz od razu przypisać go do zgodnego slotu jego ekwipunku.</p>
                </section>
                """.formatted(escapeHtml(model.getActiveHero().getName()));
    }

    private static String renderRuntimeStatsLabel(List<ItemStat> stats) {
        if (stats.isEmpty()) {
            return "Brak statów modelu itemu";
        }
        List<String> labels = new ArrayList<>();
        for (ItemStat stat : stats) {
            labels.add(ItemLibraryPresentationSupport.itemStatDisplayName(stat.getType()) + "=" + formatWhole(stat.getValue()));
        }
        return String.join(", ", labels);
    }

    private static String renderCandidateValue(Object value) {
        if (value == null) {
            return "Brak";
        }
        if (value instanceof EquipmentSlot slot) {
            return ItemLibraryPresentationSupport.slotDisplayName(slot);
        }
        return value.toString();
    }

    private static String formatWhole(double value) {
        return String.format(Locale.US, "%.0f", value);
    }

    private static String formatDecimal(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String projectedAffixValue(ItemImportEditableForm form, ImportedItemAffixType type, String fallbackValue) {
        double total = 0.0d;
        for (ImportedItemAffix affix : form.getAffixes()) {
            if (affix.getType() == type) {
                total += affix.getValue();
            }
        }
        return total > 0.0d ? formatDecimal(total) : fallbackValue;
    }

    private static String emptyNumberLabel(String value) {
        return value == null || value.isBlank() ? "0" : value;
    }

    private static String emptyLabel(String value) {
        return value == null || value.isBlank() ? "Brak pewnego odczytu" : value;
    }

    private static String escapeHtml(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }

    private static String loadTemplate() {
        try (InputStream inputStream = ItemImportPageRenderer.class.getResourceAsStream("/templates/item-import.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Brak szablonu /templates/item-import.html");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się wczytać szablonu strony importu itemu", exception);
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
