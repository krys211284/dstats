package krys.web;

import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ItemImportEditableForm;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixCatalog;
import krys.transfiguration.TransfigurationAffixDefinition;
import krys.transfiguration.TransfigurationAffixRoll;
import krys.transfiguration.TransfigurationPresentationSupport;
import krys.transfiguration.TransfigurationValueProvenance;

import java.util.List;

/** Renderer sekcji Przeistoczenia z Kostki Horadrimów. */
final class TransfigurationSectionRenderer {
    private TransfigurationSectionRenderer() {
    }

    static String renderEditor(ItemImportEditableForm form) {
        ItemTransfiguration transfiguration = form.getTransfiguration();
        boolean active = transfiguration.isTransfigured();
        return """
                <section class="subpanel transfiguration-section" data-transfiguration-section>
                    <h3>Przeistoczenie / Kostka Horadrimów</h3>
                    <div class="transfiguration-grid">
                        <label>
                            Stan
                            <select name="transfigurationState" data-transfiguration-state>
                                %s
                            </select>
                        </label>
                    </div>
                    <p class="helper" data-transfiguration-empty%s>Przeistoczenie nie jest ustawione dla tego itemu.</p>
                    <div class="transfiguration-grid" data-transfiguration-active-fields%s>
                        <label>
                            Wynik przeistoczenia
                            <select name="transfigurationOutcome" data-transfiguration-outcome-select>
                                %s
                            </select>
                        </label>
                    </div>
                    %s
                    <p class="helper" data-transfiguration-runtime%s>Przeistoczenie jest w tym etapie danymi itemu i prezentacją. Runtime nieaktywny.</p>
                </section>
                """.formatted(
                stateOptions(transfiguration),
                active ? " hidden" : "",
                active ? "" : " hidden",
                outcomeOptions(transfiguration),
                renderOutcomeFields(transfiguration, form.getAffixes()),
                active ? "" : " hidden"
        );
    }

    static String renderReadonlySummary(ItemTransfiguration transfiguration, List<ImportedItemAffix> affixes) {
        if (transfiguration == null || !transfiguration.isTransfigured()) {
            return "";
        }
        return """
                <section class="item-line-group transfiguration-readonly">
                    <h5>Przeistoczenie / Kostka Horadrimów</h5>
                    <ul class="item-read-lines">
                        <li>%s</li>
                    </ul>
                </section>
                """.formatted(
                escape(TransfigurationPresentationSupport.compactSummary(transfiguration, affixes))
        );
    }

    static String compactChip(ItemTransfiguration transfiguration, List<ImportedItemAffix> affixes) {
        return TransfigurationPresentationSupport.compactSummary(transfiguration, affixes);
    }

    private static String stateOptions(ItemTransfiguration transfiguration) {
        boolean active = transfiguration != null && transfiguration.isTransfigured();
        return option("NONE", "Nieprzeistoczony", !active)
                + option("TRANSFIGURED", "Przeistoczony", active);
    }

    private static String outcomeOptions(ItemTransfiguration transfiguration) {
        HoradricTransfigurationOutcome selected = transfiguration == null ? HoradricTransfigurationOutcome.NONE : transfiguration.getOutcome();
        StringBuilder html = new StringBuilder();
        for (HoradricTransfigurationOutcome outcome : HoradricTransfigurationOutcome.values()) {
            html.append(option(outcome.name(), outcome.getDisplayName(), outcome == selected));
        }
        return html.toString();
    }

    private static String renderOutcomeFields(ItemTransfiguration transfiguration, List<ImportedItemAffix> affixes) {
        boolean active = transfiguration.isTransfigured();
        HoradricTransfigurationOutcome selected = transfiguration.getOutcome();
        StringBuilder html = new StringBuilder();
        html.append(outcomeGroup(
                HoradricTransfigurationOutcome.UPGRADE_TO_GREATER_AFFIX,
                upgradedAffixField(transfiguration, affixes),
                active && selected == HoradricTransfigurationOutcome.UPGRADE_TO_GREATER_AFFIX));
        html.append(outcomeGroup(
                HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                addedAffixFields("transfigurationAdded",
                        "Bonusowy affix Przeistoczenia",
                        transfiguration.getAddedTransfigurationAffix()),
                active && selected == HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX));
        html.append(outcomeGroup(
                HoradricTransfigurationOutcome.REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX,
                replacedAffixFields(transfiguration, affixes),
                active && selected == HoradricTransfigurationOutcome.REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX));
        html.append(outcomeGroup(
                HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY,
                bonusQualityField(transfiguration),
                active && selected == HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY));
        return html.toString();
    }

    private static String outcomeGroup(HoradricTransfigurationOutcome outcome, String content, boolean visible) {
        return """
                <div class="transfiguration-grid transfiguration-dynamic-grid" data-transfiguration-field data-transfiguration-outcome-fields data-transfiguration-outcome="%s"%s>
                    %s
                </div>
                """.formatted(outcome.name(), visible ? "" : " hidden", content);
    }

    private static String upgradedAffixField(ItemTransfiguration transfiguration, List<ImportedItemAffix> affixes) {
        return """
                <label>
                    Affix ulepszony do Greater Affix
                    <select name="transfigurationUpgradedAffixRef">%s</select>
                </label>
                """.formatted(affixRefOptions(affixes, transfiguration.getUpgradedAffixRef()));
    }

    private static String replacedAffixFields(ItemTransfiguration transfiguration, List<ImportedItemAffix> affixes) {
        return """
                <label>
                    Affix do zastąpienia
                    <select name="transfigurationReplacedAffixRef">%s</select>
                </label>
                %s
                """.formatted(
                affixRefOptions(affixes, transfiguration.getReplacedAffixRef()),
                addedAffixFields("transfigurationReplacement", "Affix zastępujący", transfiguration.getReplacementTransfigurationAffix())
        );
    }

    private static String affixRefOptions(List<ImportedItemAffix> affixes, String selectedRef) {
        StringBuilder html = new StringBuilder(option("", "Brak", selectedRef == null || selectedRef.isBlank()));
        for (ImportedItemAffix affix : affixes == null ? List.<ImportedItemAffix>of() : affixes) {
            if (affix.isGreaterAffix()) {
                continue;
            }
            String label = affix.getType().getDisplayName() + " " + krys.itemlibrary.ItemLibraryPresentationSupport.formatDecimal(affix.getValue());
            html.append(option(affix.getType().name(), label, affix.getType().name().equals(selectedRef)));
        }
        return html.toString();
    }

    private static String addedAffixFields(String prefix, String label, TransfigurationAffixRoll roll) {
        String selectedId = roll == null ? "" : roll.getDefinitionId();
        String value = roll == null ? "" : krys.itemlibrary.ItemLibraryPresentationSupport.formatDecimal(roll.getDisplayedValue()).replace(',', '.');
        String element = roll == null ? "" : roll.getElement();
        boolean showElement = "ELEMENTAL_SPECIFIC_DAMAGE".equals(selectedId);
        return """
                <label>
                    %s
                    <select name="%sAffixId">%s</select>
                </label>
                <label>
                    Wartość widoczna na itemie
                    <input type="number" name="%sDisplayedValue" step="0.1" value="%s">
                    <span class="helper">Przepisz finalną wartość z itemu w grze. Dla realnego bonusu +96 do wszystkich współczynników wpisz 96.</span>
                </label>
                <label>
                    Pochodzenie wartości
                    <select name="%sValueProvenance">%s</select>
                    <span class="helper">Domyślnie używaj wartości widocznej w grze; nie trzeba ręcznie odwracać jakości 25/25 do source rolla.</span>
                </label>
                %s
                """.formatted(
                escape(label),
                prefix,
                transfigurationAffixOptions(selectedId),
                prefix,
                escape(value),
                prefix,
                provenanceOptions(roll),
                elementField(prefix, element, showElement)
        );
    }

    private static String elementField(String prefix, String element, boolean visible) {
        return """
                <label data-transfiguration-field data-transfiguration-element-for="%s"%s>
                    Element
                    <input type="text" name="%sAffixElement" value="%s">
                </label>
                """.formatted(prefix, visible ? "" : " hidden", prefix, escape(element));
    }

    private static String provenanceOptions(TransfigurationAffixRoll roll) {
        TransfigurationValueProvenance selected = roll == null
                ? TransfigurationValueProvenance.GAME_DISPLAYED_VALUE
                : roll.getValueProvenance();
        StringBuilder html = new StringBuilder();
        for (TransfigurationValueProvenance provenance : TransfigurationValueProvenance.values()) {
            html.append(option(provenance.name(), provenance.getDisplayName(), provenance == selected));
        }
        return html.toString();
    }

    private static String transfigurationAffixOptions(String selectedId) {
        StringBuilder html = new StringBuilder(option("", "Brak", selectedId == null || selectedId.isBlank()));
        for (TransfigurationAffixDefinition definition : TransfigurationAffixCatalog.definitions()) {
            String label = definition.getDisplayName() + " [" + TransfigurationPresentationSupport.formatRange(definition) + "]";
            html.append(option(definition.getId(), label, definition.getId().equals(selectedId)));
        }
        return html.toString();
    }

    private static String bonusQualityField(ItemTransfiguration transfiguration) {
        return """
                <label>
                    Bonusowa jakość itemu
                    <input type="number" name="transfigurationBonusQuality" min="1" max="15" step="1" value="%s">
                </label>
                """.formatted(transfiguration.getBonusQuality() == null ? "" : transfiguration.getBonusQuality());
    }

    private static String option(String value, String label, boolean selected) {
        return "<option value=\"" + escape(value) + "\"" + (selected ? " selected" : "") + ">"
                + escape(label)
                + "</option>";
    }

    static String renderScript() {
        return """
                (() => {
                    const refreshSection = section => {
                        const state = section.querySelector('[name="transfigurationState"]');
                        const outcome = section.querySelector('[name="transfigurationOutcome"]');
                        const activeFields = section.querySelector('[data-transfiguration-active-fields]');
                        const emptyMessage = section.querySelector('[data-transfiguration-empty]');
                        const runtimeMessage = section.querySelector('[data-transfiguration-runtime]');
                        if (!state) return;
                        const transfigured = state.value === 'TRANSFIGURED';
                        if (activeFields) activeFields.hidden = !transfigured;
                        if (emptyMessage) emptyMessage.hidden = transfigured;
                        if (runtimeMessage) runtimeMessage.hidden = !transfigured;
                        section.querySelectorAll('[data-transfiguration-outcome-fields]').forEach(group => {
                            const visible = transfigured && outcome && group.dataset.transfigurationOutcome === outcome.value;
                            group.hidden = !visible;
                        });
                        refreshElementField(section, 'transfigurationAdded');
                        refreshElementField(section, 'transfigurationReplacement');
                    };
                    const refreshElementField = (section, prefix) => {
                        const select = section.querySelector(`[name="${prefix}AffixId"]`);
                        const field = section.querySelector(`[data-transfiguration-element-for="${prefix}"]`);
                        if (!select || !field) return;
                        const group = field.closest('[data-transfiguration-outcome]');
                        field.hidden = group.hidden || select.value !== 'ELEMENTAL_SPECIFIC_DAMAGE';
                    };
                    document.querySelectorAll('[data-transfiguration-section]').forEach(section => {
                        ['transfigurationState', 'transfigurationOutcome', 'transfigurationAddedAffixId', 'transfigurationReplacementAffixId']
                            .map(name => section.querySelector(`[name="${name}"]`))
                            .filter(Boolean)
                            .forEach(control => control.addEventListener('change', () => refreshSection(section)));
                        refreshSection(section);
                    });
                })();
                """;
    }

    private static String escape(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }
}
