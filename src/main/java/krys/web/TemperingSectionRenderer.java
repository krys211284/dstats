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
    private static final int TEMPERING_LIMIT = 1;

    private TemperingSectionRenderer() {
    }

    static String renderEditor(ItemImportEditableForm form) {
        EquipmentSlot slot = parseSlot(form.getSlot());
        List<TemperingCategory> categories = TemperingEligibilityRegistry.availableCategories(slot, form.getItemType());
        boolean greaterAffixAvailable = "900".equals(form.getItemPower());
        boolean limitUsed = form.getTemperingAffixes().size() >= TEMPERING_LIMIT;
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

        html.append("<div id=\"temperingRows\" class=\"tempering-readonly-list\">");
        for (int index = 0; index < form.getTemperingAffixes().size(); index++) {
            html.append(renderExistingRow(form.getTemperingAffixes().get(index), index));
        }
        html.append("</div>");
        if (!categories.isEmpty()) {
            html.append(renderAddRow(categories, greaterAffixAvailable, limitUsed));
        }
        html.append(renderCategoryCatalogNotes(categories))
                .append(renderAffixCatalogData(categories, greaterAffixAvailable))
                .append(renderTemplate(categories, greaterAffixAvailable))
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
                    const newGreater = document.querySelector('[name="newTemperingGreaterAffix"]');
                    const addControls = document.getElementById('temperingAddControls');
                    const limitMessage = document.getElementById('temperingLimitMessage');
                    const valueMessage = document.getElementById('newTemperingValueMessage');
                    const rangeMessage = document.getElementById('newTemperingRangeMessage');
                    const catalogNode = document.getElementById('temperingAffixCatalog');
                    if (!rows || !count || !template || !addButton || !newCategory || !newDefinition || !newValue || !catalogNode) return;
                    const greaterAvailable = catalogNode.dataset.greaterAvailable === 'true';
                    const limit = Number.parseInt(catalogNode.dataset.limit || '1', 10);
                    let catalog = {};
                    try {
                        catalog = JSON.parse(catalogNode.textContent || '{}');
                    } catch (error) {
                        catalog = {};
                    }
                    const messageText = 'Katalog affixów tej kategorii nie został jeszcze uzupełniony.';
                    const buildOptions = (select, category, selectedValue = '') => {
                        const definitions = catalog[category] || [];
                        select.innerHTML = '';
                        if (definitions.length === 0) {
                            select.disabled = true;
                            return false;
                        }
                        select.disabled = false;
                        const placeholder = document.createElement('option');
                        placeholder.value = '';
                        placeholder.textContent = 'Wybierz affix z katalogu';
                        select.appendChild(placeholder);
                        definitions.forEach(definition => {
                            const option = document.createElement('option');
                            option.value = definition.id;
                            option.textContent = definition.label;
                        option.dataset.rangeMin = definition.rangeMin;
                        option.dataset.rangeMax = definition.rangeMax;
                        option.dataset.rangeLabel = definition.rangeLabel;
                            option.dataset.rangeFullLabel = definition.rangeFullLabel;
                            option.dataset.step = definition.step;
                            option.dataset.greaterValue = definition.greaterValue;
                            option.dataset.greaterLabel = definition.greaterLabel;
                            option.dataset.effectName = definition.effectName;
                            option.dataset.percent = definition.percent;
                            if (definition.id === selectedValue) option.selected = true;
                            select.appendChild(option);
                        });
                        if (!definitions.some(definition => definition.id === selectedValue)) {
                            select.value = '';
                        }
                        return true;
                    };
                    const selectedDefinition = select => {
                        const option = select && select.selectedOptions ? select.selectedOptions[0] : null;
                        if (!option || !option.value) return null;
                        return option;
                    };
                    const escapeHtml = value => String(value ?? '')
                        .replaceAll('&', '&amp;')
                        .replaceAll('<', '&lt;')
                        .replaceAll('>', '&gt;')
                        .replaceAll('"', '&quot;');
                    const updateMessage = (message, hasCatalog) => {
                        if (!message) return;
                        message.textContent = hasCatalog ? '' : messageText;
                        message.hidden = hasCatalog;
                    };
                    const updateGreaterState = (definitionSelect, valueInput, checkbox, labelNode, rangeNode) => {
                        if (!checkbox) return;
                        const option = selectedDefinition(definitionSelect);
                        checkbox.disabled = !greaterAvailable || !option;
                        if (checkbox.disabled) {
                            checkbox.checked = false;
                        }
                        if (labelNode && option) {
                            const greaterLabel = option.dataset.greaterLabel || 'brak danych';
                            labelNode.textContent = `Wartość GA: ${greaterLabel}`;
                            labelNode.hidden = false;
                        } else if (labelNode) {
                            labelNode.textContent = '';
                            labelNode.hidden = true;
                        }
                        if (valueInput) {
                            valueInput.readOnly = checkbox.checked && !!option;
                            if (checkbox.checked && option) {
                                valueInput.value = option.dataset.greaterValue || '';
                            }
                            if (option && !checkbox.checked) {
                                valueInput.min = option.dataset.rangeMin || '';
                                valueInput.max = option.dataset.rangeMax || '';
                                valueInput.step = option.dataset.step || '0.01';
                            }
                        }
                        if (rangeNode && option) {
                            const rangeLabel = option.dataset.rangeLabel || 'brak danych';
                            rangeNode.textContent = `Zakres: ${rangeLabel}`;
                            rangeNode.hidden = false;
                        } else if (rangeNode) {
                            rangeNode.textContent = '';
                            rangeNode.hidden = true;
                        }
                    };
                    const valueIsValid = () => {
                        const option = selectedDefinition(newDefinition);
                        if (!option || !newValue.value) return false;
                        const value = Number.parseFloat(newValue.value.replace(',', '.'));
                        if (!Number.isFinite(value)) return false;
                        if (newGreater && newGreater.checked) {
                            return Math.abs(value - Number.parseFloat(option.dataset.greaterValue)) < 0.0000001;
                        }
                        return value >= Number.parseFloat(option.dataset.rangeMin) && value <= Number.parseFloat(option.dataset.rangeMax);
                    };
                    const formatPolishNumber = rawValue => {
                        const value = Number.parseFloat(String(rawValue || '').replace(',', '.'));
                        if (!Number.isFinite(value)) return String(rawValue || '');
                        return new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 2 }).format(value);
                    };
                    const formatSavedEffect = (option, rawValue, greaterChecked) => {
                        const value = formatPolishNumber(rawValue);
                        const suffix = option.dataset.percent === 'true' ? '% ' : ' ';
                        const effect = `+${value}${suffix}${option.dataset.effectName || option.textContent}`;
                        return greaterChecked ? `★ ${effect}` : effect;
                    };
                    const updateLimitState = () => {
                        const used = rows.querySelectorAll('[data-tempering-row]').length >= limit;
                        if (addControls) addControls.hidden = used;
                        if (limitMessage) limitMessage.hidden = !used;
                        return used;
                    };
                    const updateAddState = () => {
                        const hasCatalog = buildOptions(newDefinition, newCategory.value, newDefinition.value);
                        const message = document.getElementById('newTemperingCatalogMessage');
                        const greaterLabel = document.getElementById('newTemperingGreaterValue');
                        updateGreaterState(newDefinition, newValue, newGreater, greaterLabel, rangeMessage);
                        updateMessage(message, hasCatalog);
                        const valid = hasCatalog && valueIsValid();
                        if (valueMessage) {
                            valueMessage.hidden = valid || !newDefinition.value || !newValue.value;
                            const rangeLabel = selectedDefinition(newDefinition)?.dataset.rangeLabel || 'brak danych';
                            const greaterLabel = selectedDefinition(newDefinition)?.dataset.greaterLabel || 'brak danych';
                            valueMessage.textContent = newGreater && newGreater.checked
                                ? `Greater Affix dla tego affixu wymaga wartości ${greaterLabel}.`
                                : `Wartość rolla musi mieścić się w zakresie ${rangeLabel}.`;
                        }
                        addButton.disabled = updateLimitState() || !valid;
                    };
                    const renumberRows = () => {
                        Array.from(rows.querySelectorAll('[data-tempering-row]')).forEach((row, index) => {
                            row.querySelectorAll('select[name^="temperingCategory_"], select[name^="temperingDefinitionId_"], input[name^="temperingValue_"], input[name^="temperingGreaterAffix_"]').forEach(control => {
                                const base = control.name.substring(0, control.name.lastIndexOf('_'));
                                control.name = `${base}_${index}`;
                            });
                        });
                        count.value = rows.querySelectorAll('[data-tempering-row]').length.toString();
                    };
                    rows.addEventListener('click', event => {
                        const button = event.target.closest('.remove-tempering-button');
                        if (!button) return;
                        const row = button.closest('[data-tempering-row]');
                        if (row) {
                            row.remove();
                            renumberRows();
                            updateAddState();
                        }
                    });
                    addButton.addEventListener('click', () => {
                        if (addButton.disabled) return;
                        const index = rows.querySelectorAll('[data-tempering-row]').length;
                        const option = selectedDefinition(newDefinition);
                        if (!option) return;
                        const greaterChecked = !!(newGreater && newGreater.checked && !newGreater.disabled);
                        const wrapper = document.createElement('div');
                        wrapper.innerHTML = template.innerHTML
                            .replaceAll('__INDEX__', index)
                            .replaceAll('__CATEGORY__', escapeHtml(newCategory.value))
                            .replaceAll('__CATEGORY_LABEL__', escapeHtml(newCategory.selectedOptions[0]?.textContent || newCategory.value))
                            .replaceAll('__DEFINITION__', escapeHtml(newDefinition.value))
                            .replaceAll('__AFFIX_EFFECT__', escapeHtml(formatSavedEffect(option, newValue.value, greaterChecked)))
                            .replaceAll('__VALUE__', escapeHtml(newValue.value))
                            .replaceAll('__GREATER_HIDDEN__', greaterChecked ? `<input type="hidden" name="temperingGreaterAffix_${index}" value="true">` : '')
                            .replaceAll('__GREATER_BADGE__', greaterChecked ? '<span class="tempering-greater-badge">★ Greater Affix</span>' : '');
                        const row = wrapper.querySelector('[data-tempering-row]');
                        if (!row) return;
                        rows.appendChild(row);
                        newValue.value = '';
                        if (newGreater) newGreater.checked = false;
                        updateAddState();
                        renumberRows();
                    });
                    rows.addEventListener('change', event => {
                        if (event.target.matches('select[name^="temperingCategory_"], select[name^="temperingDefinitionId_"], input[name^="temperingGreaterAffix_"]')) updateAddState();
                    });
                    newCategory.addEventListener('change', () => {
                        newDefinition.value = '';
                        updateAddState();
                    });
                    newDefinition.addEventListener('change', updateAddState);
                    newValue.addEventListener('input', updateAddState);
                    if (newGreater) newGreater.addEventListener('change', updateAddState);
                    updateAddState();
                    const form = rows.closest('form');
                    if (form) form.addEventListener('submit', renumberRows);
                })();
                """;
    }

    private static String renderExistingRow(ItemTemperingAffix affix, int index) {
        TemperingAffixDefinition definition = REGISTRY.findById(affix.getDefinitionId()).orElse(null);
        String status = definition == null
                ? TemperingPresentationSupport.compactRuntimeStatus(affix.getRuntimeStatus())
                : TemperingPresentationSupport.compactRuntimeStatus(definition.getRuntimeStatus());
        String affixLabel = TemperingPresentationSupport.formatSavedAffixEffect(affix, REGISTRY);
        if (affix.isGreaterAffix()) {
            affixLabel = "★ " + affixLabel;
        }
        String greaterBadge = affix.isGreaterAffix()
                ? "<span class=\"tempering-greater-badge\">★ Greater Affix</span>"
                : "";
        return """
                <article class="tempering-existing-card" data-tempering-row>
                    <div class="tempering-existing-header">
                        <span class="tempering-existing-category">%s</span>
                        %s
                        <input type="hidden" name="temperingCategory_%s" value="%s">
                        <input type="hidden" name="temperingDefinitionId_%s" value="%s">
                        <input type="hidden" name="temperingValue_%s" value="%s">
                        %s
                    </div>
                    <strong class="tempering-existing-affix">%s</strong>
                    <span class="tempering-runtime-status">%s</span>
                    <div class="tempering-existing-actions">
                        <button type="button" class="secondary-button remove-tempering-button">Usuń</button>
                    </div>
                </article>
                """.formatted(
                escapeHtml(affix.getCategory().getDisplayName()),
                greaterBadge,
                index,
                affix.getCategory().name(),
                index,
                escapeHtml(affix.getDefinitionId()),
                index,
                escapeHtml(inputValue(affix, definition)),
                affix.isGreaterAffix() ? "<input type=\"hidden\" name=\"temperingGreaterAffix_" + index + "\" value=\"true\">" : "",
                escapeHtml(affixLabel),
                escapeHtml(status)
        );
    }

    private static String renderAddRow(List<TemperingCategory> categories, boolean greaterAffixAvailable, boolean limitUsed) {
        if (limitUsed) {
            return """
                    <div class="tempering-add-card tempering-limit-card">
                        <p class="helper" id="temperingLimitMessage">Limit hartowania dla tego przedmiotu został wykorzystany.</p>
                        <div class="tempering-add-grid" id="temperingAddControls" hidden>
                            <label class="tempering-add-field-category">
                                Kategoria
                                <select name="newTemperingCategory">%s</select>
                            </label>
                            <label class="tempering-add-field-affix">
                                Affix
                                <select name="newTemperingDefinitionId" data-tempering-affix-select%s>%s</select>
                                <span class="helper tempering-catalog-message" id="newTemperingCatalogMessage"%s>%s</span>
                            </label>
                            <label class="tempering-add-field-value">
                                Wartość rolla
                                <input type="number" min="0" step="0.01" name="newTemperingValue" value="">
                                <span class="helper" id="newTemperingRangeMessage" hidden></span>
                            </label>
                            <label class="tempering-add-field-greater">
                                Greater Affix / Gwiazdka
                                <input type="checkbox" name="newTemperingGreaterAffix" value="true"%s>
                                <span class="helper" id="newTemperingGreaterValue" hidden></span>
                                %s
                            </label>
                            <button type="button" id="addTemperingButton" disabled>Dodaj hartowanie</button>
                            <div class="tempering-validation-message">
                                <span class="helper" id="newTemperingValueMessage" hidden></span>
                            </div>
                        </div>
                    </div>
                    """.formatted(
                    renderCategoryOptions(categories, categories.getFirst()),
                    renderDisabledAttribute(categories.getFirst()),
                    renderDefinitionOptions(categories.getFirst(), ""),
                    renderMessageHiddenAttribute(categories.getFirst()),
                    renderCatalogMessage(categories.getFirst()),
                    greaterAffixAvailable ? "" : " disabled",
                    greaterAffixAvailable ? "" : "<span class=\"helper\">Greater Affix przy hartowaniu jest dostępny tylko dla przedmiotów o mocy 900.</span>"
            );
        }
        return """
                <div class="tempering-add-card">
                    <h4>Dodaj hartowanie</h4>
                    <p class="helper" id="temperingLimitMessage"%s>Limit hartowania dla tego przedmiotu został wykorzystany.</p>
                    <div class="tempering-add-grid" id="temperingAddControls"%s>
                        <label class="tempering-add-field-category">
                            Kategoria
                            <select name="newTemperingCategory">%s</select>
                        </label>
                        <label class="tempering-add-field-affix">
                            Affix
                            <select name="newTemperingDefinitionId" data-tempering-affix-select%s>%s</select>
                            <span class="helper tempering-catalog-message" id="newTemperingCatalogMessage"%s>%s</span>
                        </label>
                        <label class="tempering-add-field-value">
                            Wartość rolla
                            <input type="number" min="0" step="0.01" name="newTemperingValue" value="">
                            <span class="helper" id="newTemperingRangeMessage" hidden></span>
                        </label>
                        <label class="tempering-add-field-greater">
                            Greater Affix / Gwiazdka
                            <input type="checkbox" name="newTemperingGreaterAffix" value="true"%s>
                            <span class="helper" id="newTemperingGreaterValue" hidden></span>
                            %s
                        </label>
                        <div class="item-affix-add-actions tempering-add-field-action">
                            <button type="button" id="addTemperingButton"%s>Dodaj hartowanie</button>
                        </div>
                        <div class="tempering-validation-message">
                            <span class="helper" id="newTemperingValueMessage" hidden></span>
                        </div>
                    </div>
                </div>
                """.formatted(
                " hidden",
                "",
                renderCategoryOptions(categories, categories.getFirst()),
                renderDisabledAttribute(categories.getFirst()),
                renderDefinitionOptions(categories.getFirst(), ""),
                renderMessageHiddenAttribute(categories.getFirst()),
                renderCatalogMessage(categories.getFirst()),
                greaterAffixAvailable ? "" : " disabled",
                greaterAffixAvailable ? "" : "<span class=\"helper\">Greater Affix przy hartowaniu jest dostępny tylko dla przedmiotów o mocy 900.</span>",
                REGISTRY.byCategory(categories.getFirst()).isEmpty() ? " disabled" : ""
        );
    }

    private static String renderTemplate(List<TemperingCategory> categories, boolean greaterAffixAvailable) {
        if (categories.isEmpty()) {
            return "";
        }
        return """
                <template id="temperingRowTemplate">
                    <article class="tempering-existing-card" data-tempering-row>
                        <div class="tempering-existing-header"><span class="tempering-existing-category">__CATEGORY_LABEL__</span>__GREATER_BADGE__<input type="hidden" name="temperingCategory___INDEX__" value="__CATEGORY__"><input type="hidden" name="temperingDefinitionId___INDEX__" value="__DEFINITION__"><input type="hidden" name="temperingValue___INDEX__" value="__VALUE__">__GREATER_HIDDEN__</div>
                        <strong class="tempering-existing-affix">__AFFIX_EFFECT__</strong>
                        <span class="tempering-runtime-status">Runtime nieaktywny</span>
                        <div class="tempering-existing-actions"><button type="button" class="secondary-button remove-tempering-button">Usuń</button></div>
                    </article>
                </template>
                """;
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

    private static String renderAffixCatalogData(List<TemperingCategory> categories, boolean greaterAffixAvailable) {
        if (categories.isEmpty()) {
            return "";
        }
        StringBuilder json = new StringBuilder("<script type=\"application/json\" id=\"temperingAffixCatalog\" data-limit=\"")
                .append(TEMPERING_LIMIT)
                .append("\" data-greater-available=\"")
                .append(greaterAffixAvailable)
                .append("\">{");
        for (int index = 0; index < categories.size(); index++) {
            TemperingCategory category = categories.get(index);
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(category.name()).append("\":[");
            List<TemperingAffixDefinition> definitions = REGISTRY.byCategory(category);
            for (int definitionIndex = 0; definitionIndex < definitions.size(); definitionIndex++) {
                TemperingAffixDefinition definition = definitions.get(definitionIndex);
                if (definitionIndex > 0) {
                    json.append(',');
                }
                json.append("{\"id\":\"")
                        .append(jsonEscape(definition.getId()))
                        .append("\",\"label\":\"")
                        .append(jsonEscape(optionLabel(definition)))
                        .append("\",\"rangeMin\":\"")
                        .append(jsonEscape(rawValue(definition.getRangeMin())))
                        .append("\",\"rangeMax\":\"")
                        .append(jsonEscape(rawValue(definition.getRangeMax())))
                        .append("\",\"rangeLabel\":\"")
                        .append(jsonEscape(TemperingPresentationSupport.formatRange(definition)))
                        .append("\",\"rangeFullLabel\":\"")
                        .append(jsonEscape(fullRangeLabel(definition)))
                        .append("\",\"step\":\"")
                        .append(definition.getUnit() == krys.tempering.TemperingValueUnit.PERCENT ? "0.01" : "1")
                        .append("\",\"greaterValue\":\"")
                        .append(jsonEscape(rawGreaterValue(definition)))
                        .append("\",\"greaterLabel\":\"")
                        .append(jsonEscape(TemperingPresentationSupport.formatGreaterAffixValue(definition)
                                + (definition.getUnit() == krys.tempering.TemperingValueUnit.PERCENT ? "%" : "")))
                        .append("\",\"effectName\":\"")
                        .append(jsonEscape(definition.getDisplayName()))
                        .append("\",\"percent\":\"")
                        .append(definition.getUnit() == krys.tempering.TemperingValueUnit.PERCENT)
                        .append("\"}");
            }
            json.append(']');
        }
        json.append("}</script>");
        return json.toString();
    }

    private static String renderDefinitionOptions(TemperingCategory category, String selectedDefinitionId) {
        StringBuilder html = new StringBuilder("<option value=\"\">Wybierz affix z katalogu</option>");
        for (TemperingAffixDefinition definition : REGISTRY.byCategory(category)) {
            html.append("<option value=\"")
                    .append(escapeHtml(definition.getId()))
                    .append("\" data-category=\"")
                    .append(definition.getCategory().name())
                    .append("\"")
                    .append(definition.getId().equals(selectedDefinitionId) ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(optionLabel(definition)))
                    .append("</option>");
        }
        return html.toString();
    }

    private static String optionLabel(TemperingAffixDefinition definition) {
        return definition.getCategory().getDisplayName()
                + ": "
                + definition.getDisplayName()
                + " ["
                + TemperingPresentationSupport.formatRange(definition)
                + (definition.getUnit() == krys.tempering.TemperingValueUnit.PERCENT ? "]%" : "]");
    }

    private static String rangeLabel(TemperingAffixDefinition definition) {
        return TemperingPresentationSupport.formatRange(definition)
                + "; Wartość GA: "
                + TemperingPresentationSupport.formatGreaterAffixValue(definition)
                + (definition.getUnit() == krys.tempering.TemperingValueUnit.PERCENT ? "%" : "");
    }

    private static String fullRangeLabel(TemperingAffixDefinition definition) {
        return "Zakres: " + rangeLabel(definition);
    }

    private static String rawGreaterValue(TemperingAffixDefinition definition) {
        return rawValue(definition.greaterAffixValue());
    }

    private static String rawValue(double value) {
        return java.math.BigDecimal.valueOf(value)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String inputValue(ItemTemperingAffix affix, TemperingAffixDefinition definition) {
        if (definition == null) {
            return java.math.BigDecimal.valueOf(affix.getValue()).stripTrailingZeros().toPlainString();
        }
        String formatted = affix.isGreaterAffix()
                ? TemperingPresentationSupport.formatGreaterAffixValue(definition)
                : TemperingPresentationSupport.formatValue(affix.getValue(), definition.getUnit());
        return formatted.replace(',', '.');
    }

    private static String renderDisabledAttribute(TemperingCategory category) {
        return REGISTRY.byCategory(category).isEmpty() ? " disabled" : "";
    }

    private static String renderMessageHiddenAttribute(TemperingCategory category) {
        return REGISTRY.byCategory(category).isEmpty() ? "" : " hidden";
    }

    private static String renderCatalogMessage(TemperingCategory category) {
        return REGISTRY.byCategory(category).isEmpty()
                ? "Katalog affixów tej kategorii nie został jeszcze uzupełniony."
                : "";
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
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
