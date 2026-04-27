package krys.web;

import krys.item.EquipmentSlot;
import krys.itemimport.ApplicationAspectRegistry;
import krys.itemimport.AspectDefinition;
import krys.itemimport.AspectRegistry;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportEditableForm;
import krys.itemlibrary.ItemLibraryPresentationSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renderuje SSR edycji zapisanego itemu bez ponownego OCR. */
final class ItemEditPageRenderer {
    private static final AspectRegistry ASPECT_REGISTRY = ApplicationAspectRegistry.get();

    String render(ItemEditPageModel model) {
        return """
                <!DOCTYPE html>
                <html lang="pl">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Build WebApp - Edytuj item</title>
                    <style>
                        %s
                        .layout { max-width: 1160px; margin: 0 auto; padding: 28px 16px 48px; }
                        .manual-confirm-grid, .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; align-items: end; }
                        label { display: grid; gap: 8px; font-weight: 600; }
                        input[type="number"], select { width: 100%%; padding: 10px 12px; border: 1px solid var(--line); border-radius: 10px; background: #fff; color: var(--text); font: inherit; }
                        .checkbox-label { display: inline-flex; gap: 8px; align-items: center; font-weight: 600; }
                        .data-table { width: 100%%; border-collapse: collapse; font-size: 0.94rem; }
                        .data-table th, .data-table td { padding: 10px 8px; border-bottom: 1px solid rgba(109, 102, 92, 0.18); text-align: left; vertical-align: top; }
                        .add-affix-row, .item-line-group { margin-top: 14px; padding: 12px; border: 1px solid rgba(109, 102, 92, 0.18); border-radius: 10px; background: #fff; }
                    </style>
                </head>
                <body>
                <main class="layout">
                    %s
                    <section class="panel">
                        <span class="section-kicker">Biblioteka itemów</span>
                        <h1>Edytuj zapisany item</h1>
                        <p class="helper">Edycja pracuje na danych zapisanych w bibliotece. OCR nie jest uruchamiany ponownie.</p>
                        <a class="nav-link secondary-link" href="%s">Wróć do biblioteki</a>
                    </section>
                    %s
                    %s
                </main>
                %s
                </body>
                </html>
                """.formatted(
                AppShellRendererSupport.renderSharedStyles(),
                AppShellRendererSupport.renderGlobalNavigation("/biblioteka-itemow"),
                escapeHtml(ItemLibraryFilterQuerySupport.libraryUrl(model.getFilter())),
                renderMessages(model),
                model.hasItem() ? renderEditForm(model) : "",
                model.hasItem() ? renderScript() : ""
        );
    }

    private static String renderMessages(ItemEditPageModel model) {
        StringBuilder html = new StringBuilder();
        if (!model.getErrors().isEmpty()) {
            html.append("<section class=\"panel panel-error\"><h2>Błędy formularza</h2><ul class=\"message-list\">");
            for (String error : model.getErrors()) {
                html.append("<li>").append(escapeHtml(error)).append("</li>");
            }
            html.append("</ul></section>");
        }
        if (!model.getMessages().isEmpty()) {
            html.append("<section class=\"panel panel-success\"><ul class=\"message-list\">");
            for (String message : model.getMessages()) {
                html.append("<li>").append(escapeHtml(message)).append("</li>");
            }
            html.append("</ul></section>");
        }
        return html.toString();
    }

    private static String renderEditForm(ItemEditPageModel model) {
        ItemImportEditableForm form = model.getForm();
        return """
                <section class="panel">
                    <h2>Formularz edycji</h2>
                    <form method="post" action="/biblioteka-itemow/edytuj">
                        <input type="hidden" name="action" value="updateItem">
                        <input type="hidden" name="itemId" value="%s">
                        <input type="hidden" name="sourceImageName" value="%s">
                        <input type="hidden" name="fullItemRead" value="%s">
                        %s
                        <div class="manual-confirm-grid">
                            %s
                            %s
                            %s
                        </div>
                        %s
                        %s
                        <div class="submit-row">
                            <button type="submit">Zapisz zmiany</button>
                            <a class="nav-link secondary-link" href="%s">Anuluj</a>
                        </div>
                    </form>
                </section>
                """.formatted(
                model.getItem().getItemId(),
                escapeHtml(form.getSourceImageName()),
                escapeHtml(FullItemReadFormCodec.encode(form.getFullItemRead())),
                ItemLibraryFilterQuerySupport.hiddenFields(model.getFilter()),
                renderReadonlyItemType(form),
                renderSlotSelect(form.getSlot()),
                renderAspectSelect(form),
                renderFullItemRead(form.getFullItemRead()),
                renderAffixEditor(form),
                escapeHtml(ItemLibraryFilterQuerySupport.libraryUrl(model.getFilter()))
        );
    }

    private static String renderReadonlyItemType(ItemImportEditableForm form) {
        return """
                <div class="summary-card">
                    <div class="summary-label">Typ itemu</div>
                    <div class="summary-value">%s</div>
                </div>
                """.formatted(escapeHtml(simplifyItemType(form.getFullItemRead(), parseSlot(form.getSlot()))));
    }

    private static String simplifyItemType(FullItemRead fullItemRead, EquipmentSlot slot) {
        String typeLine = fullItemRead == null ? "" : fullItemRead.getItemTypeLine();
        String normalized = java.text.Normalizer.normalize(typeLine == null ? "" : typeLine, java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        if (normalized.contains("TARCZA")) {
            return "Tarcza";
        }
        if (normalized.contains("BUTY")) {
            return "Buty";
        }
        if (normalized.contains("BRON GLOWNA") || slot == EquipmentSlot.MAIN_HAND) {
            return "Broń główna";
        }
        if (typeLine != null && !typeLine.isBlank()) {
            return typeLine;
        }
        return slot == null ? "Brak" : ItemLibraryPresentationSupport.slotDisplayName(slot);
    }

    private static String renderSlotSelect(String selectedSlot) {
        StringBuilder html = new StringBuilder("""
                <label>
                    Slot ekwipunku
                    <select name="slot" id="itemSlotSelect">
                """);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            html.append("<option value=\"")
                    .append(slot.name())
                    .append("\"")
                    .append(slot.name().equals(selectedSlot) ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(ItemLibraryPresentationSupport.slotDisplayName(slot)))
                    .append("</option>");
        }
        html.append("</select></label>");
        return html.toString();
    }

    private static String renderAspectSelect(ItemImportEditableForm form) {
        EquipmentSlot selectedSlot = parseSlot(form.getSlot());
        String selectedAspectId = form.getSelectedAspectId();
        AspectDefinition selectedAspect = selectedAspectId == null || selectedAspectId.isBlank()
                ? null
                : ASPECT_REGISTRY.findById(selectedAspectId).orElse(null);
        StringBuilder html = new StringBuilder("""
                <label>
                    Aspekt
                    <select name="selectedAspectId" id="aspectSelect">
                        <option value="%s"%s>Brak wybranego aspektu</option>
                """.formatted("", selectedAspectId == null || selectedAspectId.isBlank() ? " selected" : ""));
        for (AspectDefinition aspect : ASPECT_REGISTRY.all()) {
            boolean selected = aspect.getId().equals(selectedAspectId);
            boolean allowed = selectedSlot != null && aspect.allowsSlot(selectedSlot);
            html.append("<option value=\"")
                    .append(escapeHtml(aspect.getId()))
                    .append("\" data-allowed-slots=\"")
                    .append(escapeHtml(allowedSlotNames(aspect)))
                    .append("\"")
                    .append(selected ? " selected" : "")
                    .append(!allowed && !selected ? " disabled hidden" : "")
                    .append(">")
                    .append(escapeHtml(aspect.getDisplayName()))
                    .append(selected && !allowed ? " (niezgodny ze slotem)" : "")
                    .append("</option>");
        }
        html.append("</select>");
        if (selectedAspect == null) {
            html.append("<span class=\"helper\">Brak wybranego aspektu.</span>");
        } else {
            html.append("<span class=\"helper\">Wybrany aspekt: ")
                    .append(escapeHtml(selectedAspect.getDisplayName()))
                    .append("</span><span class=\"helper\">Opis aspektu: ")
                    .append(escapeHtml(selectedAspect.getEffectDescription()))
                    .append("</span>");
        }
        for (String effectLine : ItemAspectEffectPresentation.effectLines(form.getFullItemRead())) {
            html.append("<span class=\"helper\">")
                    .append(escapeHtml(effectLine))
                    .append("</span>");
        }
        html.append("</label>");
        return html.toString();
    }

    private static String renderFullItemRead(FullItemRead fullItemRead) {
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return "";
        }
        return """
                <section class="item-line-group">
                    <h3>Dane itemu zapisane w bibliotece</h3>
                    <div class="summary-grid">
                        %s
                        %s
                        %s
                        %s
                    </div>
                    %s
                    %s
                    %s
                </section>
                """.formatted(
                renderMeta("Nazwa", fullItemRead.getItemName()),
                renderMeta("Base stat", fullItemRead.getBaseItemValue()),
                renderMeta("Rzadkość", fullItemRead.getRarity()),
                renderMeta("Moc przedmiotu", fullItemRead.getItemPower()),
                renderLineGroup("Implicit / linie bazowe", groupedLines(fullItemRead, FullItemReadLineType.IMPLICIT)),
                renderLineGroup("Socket / gniazdo", groupedLines(fullItemRead, FullItemReadLineType.SOCKET)),
                renderLineGroup("Dodatkowe linie diagnostyczne", groupedLines(fullItemRead, FullItemReadLineType.OTHER))
        );
    }

    private static String renderAffixEditor(ItemImportEditableForm form) {
        StringBuilder html = new StringBuilder("""
                <section class="item-line-group">
                    <h3>Affixy</h3>
                    <input type="hidden" id="affixCount" name="affixCount" value="%s">
                    <table class="data-table" id="affixTable">
                        <thead><tr><th>Typ</th><th>Wartość</th><th>Greater Affix</th><th>Akcja</th></tr></thead>
                        <tbody id="affixRows">
                """.formatted(form.getAffixes().size()));
        for (int index = 0; index < form.getAffixes().size(); index++) {
            ImportedItemAffix affix = form.getAffixes().get(index);
            html.append("""
                    <tr>
                        <td><select name="affixType_%s">%s</select></td>
                        <td><input type="number" min="0" step="0.01" name="affixValue_%s" value="%s"></td>
                        <td><label class="checkbox-label"><input type="checkbox" name="affixGreater_%s" value="true"%s> Gwiazdka</label></td>
                        <td><button type="button" class="secondary-button remove-affix-button">Usuń</button></td>
                    </tr>
                    """.formatted(
                    index,
                    renderAffixTypeOptions(affix.getType()),
                    index,
                    formatDecimal(affix.getValue()),
                    index,
                    affix.isGreaterAffix() ? " checked" : ""
            ));
        }
        html.append("""
                        </tbody>
                    </table>
                    <div class="add-affix-row">
                        <h4>Dodaj affix</h4>
                        <div class="form-grid">
                            <label>Typ affixu<select name="newAffixType"><option value="">Nie dodawaj</option>%s</select></label>
                            <label>Wartość<input type="number" min="0" step="0.01" name="newAffixValue" value=""></label>
                            <label class="checkbox-label"><input type="checkbox" id="newAffixGreater" name="newAffixGreater" value="true"> Greater Affix</label>
                        </div>
                        <button type="button" id="addAffixButton">Dodaj affix</button>
                    </div>
                    <template id="affixRowTemplate">
                        <tr>
                            <td><select name="affixType___INDEX__">%s</select></td>
                            <td><input type="number" min="0" step="0.01" name="affixValue___INDEX__" value="__VALUE__"></td>
                            <td><label class="checkbox-label"><input type="checkbox" name="affixGreater___INDEX__" value="true"> Gwiazdka</label></td>
                            <td><button type="button" class="secondary-button remove-affix-button">Usuń</button></td>
                        </tr>
                    </template>
                </section>
                """.formatted(renderAffixTypeOptions(null), renderAffixTypeOptions(null)));
        return html.toString();
    }

    private static String renderScript() {
        return """
                <script>
                (() => {
                    const rows = document.getElementById('affixRows');
                    const count = document.getElementById('affixCount');
                    const template = document.getElementById('affixRowTemplate');
                    const addButton = document.getElementById('addAffixButton');
                    const newType = document.querySelector('[name="newAffixType"]');
                    const newValue = document.querySelector('[name="newAffixValue"]');
                    const newGreater = document.getElementById('newAffixGreater');
                    if (!rows || !count || !template || !addButton || !newType || !newValue) return;
                    const renumberRows = () => {
                        Array.from(rows.querySelectorAll('tr')).forEach((row, index) => {
                            const type = row.querySelector('select[name^="affixType_"]');
                            const value = row.querySelector('input[name^="affixValue_"]');
                            const greater = row.querySelector('input[name^="affixGreater_"]');
                            if (type) type.name = `affixType_${index}`;
                            if (value) value.name = `affixValue_${index}`;
                            if (greater) greater.name = `affixGreater_${index}`;
                        });
                        count.value = rows.querySelectorAll('tr').length.toString();
                    };
                    rows.addEventListener('click', event => {
                        const button = event.target.closest('.remove-affix-button');
                        if (!button) return;
                        const row = button.closest('tr');
                        if (row) {
                            row.remove();
                            renumberRows();
                        }
                    });
                    addButton.addEventListener('click', () => {
                        const index = rows.querySelectorAll('tr').length;
                        const wrapper = document.createElement('tbody');
                        wrapper.innerHTML = template.innerHTML.replaceAll('__INDEX__', index).replaceAll('__VALUE__', newValue.value);
                        const row = wrapper.querySelector('tr');
                        if (!row) return;
                        const type = row.querySelector(`select[name="affixType_${index}"]`);
                        const greater = row.querySelector(`input[name="affixGreater_${index}"]`);
                        if (type) type.value = newType.value;
                        if (greater && newGreater) greater.checked = newGreater.checked;
                        rows.appendChild(row);
                        newType.value = '';
                        newValue.value = '';
                        if (newGreater) newGreater.checked = false;
                        renumberRows();
                    });
                    const form = rows.closest('form');
                    if (form) form.addEventListener('submit', renumberRows);
                })();
                (() => {
                    const slotSelect = document.getElementById('itemSlotSelect');
                    const aspectSelect = document.getElementById('aspectSelect');
                    if (!slotSelect || !aspectSelect) return;
                    const refreshAspectOptions = () => {
                        const slot = slotSelect.value;
                        Array.from(aspectSelect.options).forEach(option => {
                            if (!option.value) {
                                option.hidden = false;
                                option.disabled = false;
                                return;
                            }
                            const allowed = (option.dataset.allowedSlots || '').split(',').filter(Boolean).includes(slot);
                            option.hidden = !allowed;
                            option.disabled = !allowed;
                            if (!allowed && option.selected) aspectSelect.value = '';
                        });
                    };
                    slotSelect.addEventListener('change', refreshAspectOptions);
                    refreshAspectOptions();
                })();
                </script>
                """;
    }

    private static List<FullItemReadLine> groupedLines(FullItemRead fullItemRead, FullItemReadLineType type) {
        List<FullItemReadLine> lines = new ArrayList<>();
        for (FullItemReadLine line : fullItemRead.getLines()) {
            if (line.getType() == type) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String renderLineGroup(String heading, List<FullItemReadLine> lines) {
        if (lines.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<section class=\"item-line-group\"><h4>")
                .append(escapeHtml(heading))
                .append("</h4><ul>");
        for (FullItemReadLine line : lines) {
            html.append("<li>").append(escapeHtml(line.getText())).append("</li>");
        }
        html.append("</ul></section>");
        return html.toString();
    }

    private static String renderMeta(String label, String value) {
        return """
                <div class="summary-card">
                    <div class="summary-label">%s</div>
                    <div class="summary-value">%s</div>
                </div>
                """.formatted(escapeHtml(label), escapeHtml(value == null || value.isBlank() ? "Brak" : value));
    }

    private static String renderAffixTypeOptions(ImportedItemAffixType selectedType) {
        StringBuilder html = new StringBuilder();
        for (ImportedItemAffixType type : ImportedItemAffixType.values()) {
            html.append("<option value=\"")
                    .append(type.name())
                    .append("\"")
                    .append(type == selectedType ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(type.getDisplayName()))
                    .append("</option>");
        }
        return html.toString();
    }

    private static String allowedSlotNames(AspectDefinition aspect) {
        return aspect.getAllowedItemSlots().stream()
                .map(EquipmentSlot::name)
                .sorted()
                .reduce("", (left, right) -> left.isBlank() ? right : left + "," + right);
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

    private static String formatDecimal(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private static String escapeHtml(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }
}
