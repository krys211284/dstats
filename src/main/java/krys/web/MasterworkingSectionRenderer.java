package krys.web;

import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportEditableForm;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkingPresentationSupport;
import krys.masterworking.MasterworkingPresentationValue;
import krys.masterworking.MasterworkingPresentationValueResolver;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkedAffixSource;
import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingAffixRegistry;

import java.util.List;

/** Renderer sekcji Doskonalenia itemu. Mechanika jest tu wyłącznie danymi itemu. */
final class MasterworkingSectionRenderer {
    private static final TemperingAffixRegistry TEMPERING_REGISTRY = ApplicationTemperingAffixRegistry.get();
    private static final MasterworkingPresentationValueResolver VALUE_RESOLVER = new MasterworkingPresentationValueResolver();

    private MasterworkingSectionRenderer() {
    }

    static String renderEditor(ItemImportEditableForm form) {
        ItemMasterworking masterworking = form == null ? ItemMasterworking.defaultState() : form.getMasterworking();
        String perfectedAffixSelector = masterworking.getQualityCurrent() == ItemMasterworking.DEFAULT_QUALITY_MAX
                ? renderPerfectedAffixSelector(masterworking, form.getAffixes(), form.getTemperingAffixes())
                : "";
        return """
                <section class="subpanel masterworking-section">
                    <h3>Doskonalenie</h3>
                    <fieldset class="inline-fieldset masterworking-fieldset">
                        <legend>Jakość</legend>
                        <div class="masterworking-grid">
                            <label>
                                Jakość aktualna
                                <select name="masterworkingQualityCurrent">%s</select>
                            </label>
                            <label>
                                Jakość maksymalna
                                <input type="number" min="25" max="25" step="1" name="masterworkingQualityMax" value="%s" readonly>
                            </label>
                            <span class="masterworking-runtime-status">%s</span>
                        </div>
                        %s
                        <p class="helper">%s</p>
                    </fieldset>
                </section>
                """.formatted(
                renderQualityCurrentOptions(masterworking.getQualityCurrent()),
                masterworking.getQualityMax(),
                CurrentBuildCalculationSectionsRenderer.escapeHtml(MasterworkingPresentationSupport.runtimeStatusLabel()),
                perfectedAffixSelector,
                escapeHtml(MasterworkingPresentationValueResolver.RUNTIME_INACTIVE_NOTE)
        );
    }

    static String renderReadonlySummary(ItemMasterworking masterworking,
                                        List<ImportedItemAffix> affixes,
                                        List<ItemTemperingAffix> temperingAffixes) {
        if (masterworking == null || !masterworking.hasVisibleProgress()) {
            return "";
        }
        String perfectedAffixLabel = perfectedAffixLabel(masterworking.getPerfectedAffix(), affixes, temperingAffixes, false);
        return """
                <section class="item-line-group masterworking-summary">
                    <h5>Doskonalenie</h5>
                    <ul class="item-read-lines">
                        <li>%s</li>
                    </ul>
                </section>
                """.formatted(
                CurrentBuildCalculationSectionsRenderer.escapeHtml(MasterworkingPresentationSupport.compactSummary(masterworking, perfectedAffixLabel))
        );
    }

    static String compactChip(ItemMasterworking masterworking,
                              List<ImportedItemAffix> affixes,
                              List<ItemTemperingAffix> temperingAffixes) {
        if (masterworking == null || !masterworking.hasVisibleProgress()) {
            return "";
        }
        String perfectedAffixLabel = perfectedAffixLabel(masterworking.getPerfectedAffix(), affixes, temperingAffixes, false);
        return MasterworkingPresentationSupport.compactSummary(masterworking, perfectedAffixLabel);
    }

    static String renderArmorEditorHint(ItemMasterworking masterworking, String rawArmor) {
        if (masterworking == null || !masterworking.hasVisibleProgress()) {
            return "";
        }
        Long armor = parseLong(rawArmor);
        if (armor == null || armor <= 0L) {
            return "";
        }
        return renderEditorCurrentValue(VALUE_RESOLVER.resolveArmor(armor, masterworking));
    }

    static String renderAffixEditorHint(ItemMasterworking masterworking, ImportedItemAffix affix) {
        if (masterworking == null || !masterworking.hasVisibleProgress()) {
            return "";
        }
        return renderEditorCurrentValue(VALUE_RESOLVER.resolveAffix(affix, masterworking));
    }

    static String renderTemperingEditorHint(ItemMasterworking masterworking, ItemTemperingAffix affix) {
        if (masterworking == null || !masterworking.hasVisibleProgress()) {
            return "";
        }
        return "";
    }

    static String renderTemperingEditorCurrentLine(ItemMasterworking masterworking, ItemTemperingAffix affix, String fallbackLabel) {
        if (masterworking == null || !masterworking.hasVisibleProgress()) {
            return escapeHtml(fallbackLabel);
        }
        return formatValueLine(VALUE_RESOLVER.resolveTempering(affix, masterworking), affix.isGreaterAffix());
    }

    static String formatArmorReadonlyValue(ItemMasterworking masterworking, Long itemArmor) {
        if (itemArmor == null || itemArmor <= 0L) {
            return "Brak pewnego odczytu";
        }
        MasterworkingPresentationValue value = VALUE_RESOLVER.resolveArmor(itemArmor, masterworking);
        return formatReadonlyValue(value, false);
    }

    static String formatAffixReadonlyLine(ItemMasterworking masterworking, ImportedItemAffix affix) {
        if (masterworking == null || !masterworking.hasVisibleProgress()) {
            return escapeHtml(krys.itemlibrary.ItemLibraryPresentationSupport.formatAffixForDetails(affix));
        }
        return formatValueLine(VALUE_RESOLVER.resolveAffix(affix, masterworking), affix.isGreaterAffix());
    }

    static String formatTemperingReadonlyLine(ItemMasterworking masterworking, ItemTemperingAffix affix) {
        if (masterworking == null || !masterworking.hasVisibleProgress()) {
            return escapeHtml(krys.tempering.TemperingPresentationSupport.formatAffix(affix, TEMPERING_REGISTRY));
        }
        return formatValueLine(VALUE_RESOLVER.resolveTempering(affix, masterworking), affix.isGreaterAffix());
    }

    private static String renderEditorCurrentValue(MasterworkingPresentationValue value) {
        if (value == null || !hasVisiblePresentation(value)) {
            return "";
        }
        StringBuilder html = new StringBuilder("<div class=\"masterworking-current-value\">")
                .append(valueSpan(value));
        if (value.isPerfected()) {
            html.append(" ").append(perfectedBadge());
        }
        if (!value.isSupported() || !value.getNote().isBlank() && !value.getNote().startsWith("Doskonalenie: Jakość ")) {
            html.append("<span class=\"helper masterworking-rule-note\">").append(escapeHtml(value.getNote())).append("</span>");
        }
        html.append("</div>");
        return html.toString();
    }

    private static boolean hasVisiblePresentation(MasterworkingPresentationValue value) {
        return value.hasChangedValue() || value.isPerfected() || !value.isSupported();
    }

    private static String formatReadonlyValue(MasterworkingPresentationValue value, boolean greaterAffix) {
        if (value == null) {
            return "";
        }
        if (!hasVisiblePresentation(value)) {
            String prefix = greaterAffix ? "★ " : "";
            return prefix + valueSpan(value);
        }
        StringBuilder line = new StringBuilder();
        if (greaterAffix) {
            line.append("★ ");
        }
        line.append(valueSpan(value));
        if (!value.getSuffixLabel().isBlank()) {
            line.append(" ").append(escapeHtml(value.getSuffixLabel()));
        }
        if (value.isPerfected()) {
            line.append(" ").append(perfectedBadge());
        }
        if (!value.isSupported() || !value.getNote().isBlank() && !value.getNote().startsWith("Doskonalenie: Jakość ")) {
            line.append(" <span class=\"helper masterworking-rule-note\">").append(escapeHtml(value.getNote())).append("</span>");
        }
        return line.toString();
    }

    private static String formatValueLine(MasterworkingPresentationValue value, boolean greaterAffix) {
        StringBuilder line = new StringBuilder();
        if (greaterAffix) {
            line.append("★ ");
        }
        if (value.getSuffixLabel().isBlank()) {
            line.append(escapeHtml(value.getLabel())).append(" ");
        }
        line.append(valueSpan(value));
        if (!value.getSuffixLabel().isBlank()) {
            line.append(" ").append(escapeHtml(value.getSuffixLabel()));
        }
        if (value.isPerfected()) {
            line.append(" ").append(perfectedBadge());
        }
        if (!value.isSupported()) {
            line.append(" <span class=\"helper masterworking-rule-note\">").append(escapeHtml(value.getNote())).append("</span>");
        }
        return line.toString();
    }

    private static String valueSpan(MasterworkingPresentationValue value) {
        StringBuilder classes = new StringBuilder("masterworking-value");
        if (value.isPerfected()) {
            classes.append(" masterworking-value--perfected");
        } else if (value.hasChangedValue()) {
            classes.append(" masterworking-value--upgraded");
        }
        return "<span class=\"" + classes + "\">" + escapeHtml(value.getDisplayValueLabel()) + "</span>";
    }

    private static String perfectedBadge() {
        return "<span class=\"masterworking-perfected-badge\">Doskonalony afiks</span>";
    }

    private static String renderPerfectedAffixSelector(ItemMasterworking masterworking,
                                                       List<ImportedItemAffix> affixes,
                                                       List<ItemTemperingAffix> temperingAffixes) {
        String selectedValue = selectionValue(masterworking.getPerfectedAffix());
        StringBuilder options = new StringBuilder("<option value=\"\"");
        if (selectedValue.isBlank()) {
            options.append(" selected");
        }
        options.append(">Brak / nie wybrano</option>");
        for (ImportedItemAffix affix : affixes) {
            String value = selectionValue(new MasterworkedAffixSelection(MasterworkedAffixSource.ORDINARY_AFFIX, affix.getType().name()));
            options.append("<option value=\"")
                    .append(escapeHtml(value))
                    .append("\"")
                    .append(value.equals(selectedValue) ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(affix.getName()))
                    .append("</option>");
        }
        for (ItemTemperingAffix affix : temperingAffixes) {
            String value = selectionValue(new MasterworkedAffixSelection(MasterworkedAffixSource.TEMPERING_AFFIX, affix.getDefinitionId()));
            options.append("<option value=\"")
                    .append(escapeHtml(value))
                    .append("\"")
                    .append(value.equals(selectedValue) ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(perfectedAffixLabel(new MasterworkedAffixSelection(MasterworkedAffixSource.TEMPERING_AFFIX, affix.getDefinitionId()), List.of(), temperingAffixes, true)))
                    .append("</option>");
        }
        return """
                <label class="masterworking-perfected-affix-field">
                    Aktualny doskonalony afiks
                    <select name="masterworkingPerfectedAffix">%s</select>
                </label>
                """.formatted(options);
    }

    private static String selectionValue(MasterworkedAffixSelection selection) {
        if (selection == null || selection.getSource() == null || selection.getKey().isBlank()) {
            return "";
        }
        return selection.getSource().name() + ":" + selection.getKey();
    }

    static String perfectedAffixLabel(MasterworkedAffixSelection selection,
                                      List<ImportedItemAffix> affixes,
                                      List<ItemTemperingAffix> temperingAffixes,
                                      boolean prefixTempering) {
        if (selection == null || selection.getSource() == null || selection.getKey().isBlank()) {
            return "";
        }
        if (selection.getSource() == MasterworkedAffixSource.ORDINARY_AFFIX) {
            for (ImportedItemAffix affix : affixes) {
                if (affix.getType().name().equals(selection.getKey())) {
                    return affix.getName();
                }
            }
            try {
                return ImportedItemAffixType.valueOf(selection.getKey()).getDisplayName();
            } catch (IllegalArgumentException exception) {
                return "";
            }
        }
        if (selection.getSource() == MasterworkedAffixSource.TEMPERING_AFFIX) {
            boolean itemHasTempering = temperingAffixes.stream()
                    .anyMatch(affix -> affix.getDefinitionId().equals(selection.getKey()));
            if (!itemHasTempering) {
                return "";
            }
            String label = TEMPERING_REGISTRY.findById(selection.getKey())
                    .map(definition -> cleanTemperingLabel(definition.getDisplayName()))
                    .orElse(cleanTemperingLabel(selection.getKey()));
            return prefixTempering ? "Hartowanie: " + label : label;
        }
        return "";
    }

    private static String cleanTemperingLabel(String label) {
        if (label == null) {
            return "";
        }
        if ("do maksymalnej liczby kumulacji Animuszu".equals(label)) {
            return "maksymalna liczba kumulacji Animuszu";
        }
        return label.startsWith("do ") ? label.substring(3) : label;
    }

    private static String renderQualityCurrentOptions(int selectedQuality) {
        StringBuilder options = new StringBuilder();
        int safeSelected = ItemMasterworking.isAllowedQualityStep(selectedQuality)
                ? selectedQuality
                : ItemMasterworking.DEFAULT_QUALITY_CURRENT;
        for (Integer step : ItemMasterworking.ALLOWED_QUALITY_STEPS) {
            options.append("<option value=\"")
                    .append(step)
                    .append("\"")
                    .append(step == safeSelected ? " selected" : "")
                    .append(">")
                    .append(step)
                    .append("/")
                    .append(ItemMasterworking.DEFAULT_QUALITY_MAX)
                    .append("</option>");
        }
        return options.toString();
    }

    private static String escapeHtml(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }

    private static Long parseLong(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(rawValue.replace(" ", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
