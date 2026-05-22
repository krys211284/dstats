package krys.web;

import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ItemImportEditableForm;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixCatalog;
import krys.transfiguration.TransfigurationAffixDefinition;
import krys.transfiguration.TransfigurationAffixRoll;
import krys.transfiguration.TransfigurationPresentationSupport;

import java.util.List;

/** Renderer sekcji Przeistoczenia z Kostki Horadrimów. */
final class TransfigurationSectionRenderer {
    private TransfigurationSectionRenderer() {
    }

    static String renderEditor(ItemImportEditableForm form) {
        ItemTransfiguration transfiguration = form.getTransfiguration();
        boolean lockedSelected = !transfiguration.isTransfigured() || transfiguration.isLockedAfterTransfiguration();
        return """
                <section class="subpanel transfiguration-section">
                    <h3>Przeistoczenie / Kostka Horadrimów</h3>
                    <div class="transfiguration-grid">
                        <label>
                            Stan
                            <select name="transfigurationState">
                                %s
                            </select>
                        </label>
                        <label>
                            Wynik przeistoczenia
                            <select name="transfigurationOutcome">
                                %s
                            </select>
                        </label>
                        <label>
                            Pryzmat dostrojenia
                            <select name="transfigurationTuningPrism">
                                %s
                            </select>
                        </label>
                        <label>
                            Niemodyfikowalny po przeistoczeniu
                            <select name="transfigurationLockedAfter">
                                <option value="true"%s>Tak</option>
                                <option value="false"%s>Nie</option>
                            </select>
                        </label>
                    </div>
                    <div class="transfiguration-grid transfiguration-dynamic-grid">
                        %s
                        %s
                        %s
                        %s
                    </div>
                    <p class="helper">Przeistoczenie jest w tym etapie danymi itemu i prezentacją. Runtime nieaktywny.</p>
                </section>
                """.formatted(
                stateOptions(transfiguration),
                outcomeOptions(transfiguration),
                prismOptions(transfiguration),
                lockedSelected ? " selected" : "",
                !lockedSelected ? " selected" : "",
                upgradedAffixField(transfiguration, form.getAffixes()),
                addedAffixFields("transfigurationAdded", "Bonusowy affix Przeistoczenia", transfiguration.getAddedTransfigurationAffix()),
                replacedAffixFields(transfiguration, form.getAffixes()),
                bonusQualityField(transfiguration)
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
                        <li>%s</li>
                    </ul>
                </section>
                """.formatted(
                escape(TransfigurationPresentationSupport.compactSummary(transfiguration, affixes)),
                escape(TransfigurationPresentationSupport.lockStatus(transfiguration))
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

    private static String prismOptions(ItemTransfiguration transfiguration) {
        HoradricTuningPrism selected = transfiguration == null ? HoradricTuningPrism.NONE : transfiguration.getTuningPrism();
        StringBuilder html = new StringBuilder();
        for (HoradricTuningPrism prism : HoradricTuningPrism.values()) {
            html.append(option(prism.name(), prism.getDisplayName(), prism == selected));
        }
        return html.toString();
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
        String value = roll == null ? "" : krys.itemlibrary.ItemLibraryPresentationSupport.formatDecimal(roll.getValue()).replace(',', '.');
        String element = roll == null ? "" : roll.getElement();
        return """
                <label>
                    %s
                    <select name="%sAffixId">%s</select>
                </label>
                <label>
                    Wartość rolla
                    <input type="number" name="%sAffixValue" step="0.1" value="%s">
                </label>
                <label>
                    Element
                    <input type="text" name="%sAffixElement" value="%s">
                </label>
                """.formatted(
                escape(label),
                prefix,
                transfigurationAffixOptions(selectedId),
                prefix,
                escape(value),
                prefix,
                escape(element)
        );
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

    private static String escape(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }
}
