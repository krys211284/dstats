package krys.web;

import krys.item.EquipmentSlot;
import krys.itemimport.ItemImportEditableForm;
import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingAffixDefinition;
import krys.tempering.TemperingAffixRegistry;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingEligibilityRegistry;
import krys.tempering.TemperingPresentationSupport;

import java.util.List;

/** Wspólny renderer sekcji hartowania dla importu i edycji itemu. */
final class TemperingSectionRenderer {
    private static final TemperingAffixRegistry REGISTRY = ApplicationTemperingAffixRegistry.get();

    private TemperingSectionRenderer() {
    }

    static String renderEditor(ItemImportEditableForm form) {
        EquipmentSlot slot = parseSlot(form.getSlot());
        List<TemperingCategory> categories = TemperingEligibilityRegistry.availableCategories(slot, form.getItemType());
        StringBuilder html = new StringBuilder("""
                <section class="subpanel tempering-section">
                    <h3>Hartowanie</h3>
                    <input type="hidden" id="temperingCount" name="temperingCount" value="%s">
                """.formatted(form.getTemperingAffixes().size()));
        if (categories.isEmpty()) {
            html.append("<p class=\"helper\">Brak danych dostępności hartowania dla tego typu itemu.</p>");
        } else {
            html.append("<p class=\"helper\">Dostępne kategorie: ")
                    .append(escapeHtml(categoryLabels(categories)))
                    .append(".</p>");
        }

        html.append("""
                    <div class="affix-table-wrap">
                    <table class="data-table affix-table tempering-table">
                        <thead>
                            <tr>
                                <th>Kategoria</th>
                                <th>Affix</th>
                                <th>Wartość rolla</th>
                                <th>Zakres</th>
                                <th>Status runtime</th>
                                <th>Akcja</th>
                            </tr>
                        </thead>
                        <tbody id="temperingRows">
                """);
        for (int index = 0; index < form.getTemperingAffixes().size(); index++) {
            html.append(renderExistingRow(form.getTemperingAffixes().get(index), categories, index));
        }
        html.append("""
                        </tbody>
                    </table>
                    </div>
                """);
        if (!categories.isEmpty()) {
            html.append(renderAddRow(categories));
        }
        html.append(renderCategoryCatalogNotes(categories))
                .append(renderTemplate(categories))
                .append("</section>");
        return html.toString();
    }

    static String renderScript() {
        return """
                (() => {
                    const rows = document.getElementById('temperingRows');
                    const count = document.getElementById('temperingCount');
                    const template = document.getElementById('temperingRowTemplate');
                    const addButton = document.getElementById('addTemperingButton');
                    const newCategory = document.querySelector('[name="newTemperingCategory"]');
                    const newDefinition = document.querySelector('[name="newTemperingDefinitionId"]');
                    const newValue = document.querySelector('[name="newTemperingValue"]');
                    if (!rows || !count || !template || !addButton || !newCategory || !newDefinition || !newValue) return;
                    const renumberRows = () => {
                        Array.from(rows.querySelectorAll('tr')).forEach((row, index) => {
                            row.querySelectorAll('select[name^="temperingCategory_"], select[name^="temperingDefinitionId_"], input[name^="temperingValue_"]').forEach(control => {
                                const base = control.name.substring(0, control.name.lastIndexOf('_'));
                                control.name = `${base}_${index}`;
                            });
                        });
                        count.value = rows.querySelectorAll('tr').length.toString();
                    };
                    rows.addEventListener('click', event => {
                        const button = event.target.closest('.remove-tempering-button');
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
                        const category = row.querySelector(`select[name="temperingCategory_${index}"]`);
                        const definition = row.querySelector(`select[name="temperingDefinitionId_${index}"]`);
                        if (category) category.value = newCategory.value;
                        if (definition) definition.value = newDefinition.value;
                        rows.appendChild(row);
                        newValue.value = '';
                        renumberRows();
                    });
                    const form = rows.closest('form');
                    if (form) form.addEventListener('submit', renumberRows);
                })();
                """;
    }

    private static String renderExistingRow(ItemTemperingAffix affix, List<TemperingCategory> categories, int index) {
        TemperingAffixDefinition definition = REGISTRY.findById(affix.getDefinitionId()).orElse(null);
        String range = definition == null ? "Brak zakresu" : TemperingPresentationSupport.formatRange(definition);
        String status = definition == null ? affix.getRuntimeStatus().getDisplayName() : definition.getRuntimeStatus().getDisplayName();
        return """
                <tr>
                    <td><select name="temperingCategory_%s">%s</select></td>
                    <td><select name="temperingDefinitionId_%s">%s</select></td>
                    <td><input type="number" min="0" step="0.01" name="temperingValue_%s" value="%s"></td>
                    <td>%s</td>
                    <td>%s</td>
                    <td><button type="button" class="secondary-button remove-tempering-button">Usuń</button></td>
                </tr>
                """.formatted(
                index,
                renderCategoryOptions(categories, affix.getCategory()),
                index,
                renderDefinitionOptions(categories, affix.getDefinitionId()),
                index,
                escapeHtml(TemperingPresentationSupport.formatValue(affix.getValue(), definition == null ? krys.tempering.TemperingValueUnit.FLAT : definition.getUnit()).replace(',', '.')),
                escapeHtml(range),
                escapeHtml(status)
        );
    }

    private static String renderAddRow(List<TemperingCategory> categories) {
        return """
                <div class="add-affix-row tempering-add-row">
                    <h4>Dodaj hartowanie</h4>
                    <div class="item-affix-add-grid">
                        <label>
                            Kategoria
                            <select name="newTemperingCategory">%s</select>
                        </label>
                        <label>
                            Affix
                            <select name="newTemperingDefinitionId">%s</select>
                        </label>
                        <label>
                            Wartość rolla
                            <input type="number" min="0" step="0.01" name="newTemperingValue" value="">
                        </label>
                        <div class="item-affix-add-actions">
                            <button type="button" id="addTemperingButton">Dodaj hartowanie</button>
                        </div>
                    </div>
                </div>
                """.formatted(renderCategoryOptions(categories, categories.getFirst()), renderDefinitionOptions(categories, ""));
    }

    private static String renderTemplate(List<TemperingCategory> categories) {
        if (categories.isEmpty()) {
            return "";
        }
        return """
                <template id="temperingRowTemplate">
                    <tr>
                        <td><select name="temperingCategory___INDEX__">%s</select></td>
                        <td><select name="temperingDefinitionId___INDEX__">%s</select></td>
                        <td><input type="number" min="0" step="0.01" name="temperingValue___INDEX__" value="__VALUE__"></td>
                        <td><span class="helper">Zakres walidowany po wyborze affixu</span></td>
                        <td>Dane itemu / runtime nieaktywny</td>
                        <td><button type="button" class="secondary-button remove-tempering-button">Usuń</button></td>
                    </tr>
                </template>
                """.formatted(renderCategoryOptions(categories, categories.getFirst()), renderDefinitionOptions(categories, ""));
    }

    private static String renderCategoryCatalogNotes(List<TemperingCategory> categories) {
        if (categories.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<div class=\"tempering-category-notes\">");
        for (TemperingCategory category : categories) {
            if (REGISTRY.byCategory(category).isEmpty()) {
                html.append("<p class=\"helper\" data-tempering-category=\"")
                        .append(category.name())
                        .append("\">")
                        .append(escapeHtml(category.getDisplayName()))
                        .append(": Katalog affixów tej kategorii nie został jeszcze uzupełniony.</p>");
            }
        }
        html.append("</div>");
        return html.toString();
    }

    private static String renderCategoryOptions(List<TemperingCategory> categories, TemperingCategory selectedCategory) {
        StringBuilder html = new StringBuilder();
        for (TemperingCategory category : categories) {
            html.append("<option value=\"")
                    .append(category.name())
                    .append("\"")
                    .append(category == selectedCategory ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(category.getDisplayName()))
                    .append("</option>");
        }
        return html.toString();
    }

    private static String renderDefinitionOptions(List<TemperingCategory> categories, String selectedDefinitionId) {
        StringBuilder html = new StringBuilder("<option value=\"\">Wybierz affix z katalogu</option>");
        for (TemperingAffixDefinition definition : REGISTRY.all()) {
            if (!categories.contains(definition.getCategory())) {
                continue;
            }
            html.append("<option value=\"")
                    .append(escapeHtml(definition.getId()))
                    .append("\" data-category=\"")
                    .append(definition.getCategory().name())
                    .append("\"")
                    .append(definition.getId().equals(selectedDefinitionId) ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(definition.getCategory().getDisplayName()))
                    .append(": ")
                    .append(escapeHtml(definition.getDisplayName()))
                    .append(" [")
                    .append(escapeHtml(TemperingPresentationSupport.formatRange(definition)))
                    .append(definition.getUnit() == krys.tempering.TemperingValueUnit.PERCENT ? "%" : "")
                    .append("]</option>");
        }
        return html.toString();
    }

    private static String categoryLabels(List<TemperingCategory> categories) {
        return categories.stream()
                .map(TemperingCategory::getDisplayName)
                .reduce("", (left, right) -> left.isBlank() ? right : left + ", " + right);
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

    private static String escapeHtml(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }
}
