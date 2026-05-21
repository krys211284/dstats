package krys.web;

import krys.itemimport.ItemImportEditableForm;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkingPresentationSupport;

/** Renderer sekcji Doskonalenia itemu. Mechanika jest tu wyłącznie danymi itemu. */
final class MasterworkingSectionRenderer {
    private MasterworkingSectionRenderer() {
    }

    static String renderEditor(ItemImportEditableForm form) {
        ItemMasterworking masterworking = form == null ? ItemMasterworking.defaultState() : form.getMasterworking();
        return """
                <section class="subpanel masterworking-section">
                    <h3>Doskonalenie</h3>
                    <fieldset class="inline-fieldset masterworking-fieldset">
                        <legend>Jakość</legend>
                        <label class="checkbox-label masterworking-enabled-field">
                            <input type="checkbox" name="masterworkingEnabled" value="true"%s> Doskonalenie aktywne / Item doskonalony
                        </label>
                        <div class="masterworking-grid">
                            <label>
                                Jakość aktualna
                                <input type="number" min="0" max="25" step="1" name="masterworkingQualityCurrent" value="%s">
                            </label>
                            <label>
                                Jakość maksymalna
                                <input type="number" min="25" max="25" step="1" name="masterworkingQualityMax" value="%s" readonly>
                            </label>
                            <span class="masterworking-runtime-status">%s</span>
                        </div>
                    </fieldset>
                </section>
                """.formatted(
                masterworking.isEnabled() ? " checked" : "",
                masterworking.getQualityCurrent(),
                masterworking.getQualityMax(),
                CurrentBuildCalculationSectionsRenderer.escapeHtml(MasterworkingPresentationSupport.runtimeStatusLabel())
        );
    }

    static String renderReadonlySummary(ItemMasterworking masterworking) {
        if (masterworking == null || !masterworking.isEnabled()) {
            return "";
        }
        return """
                <section class="item-line-group masterworking-summary">
                    <h5>Doskonalenie</h5>
                    <ul class="item-read-lines">
                        <li>Jakość %s</li>
                        <li>%s</li>
                    </ul>
                </section>
                """.formatted(
                CurrentBuildCalculationSectionsRenderer.escapeHtml(MasterworkingPresentationSupport.qualityLabel(masterworking)),
                CurrentBuildCalculationSectionsRenderer.escapeHtml(MasterworkingPresentationSupport.runtimeStatusLabel())
        );
    }

    static String compactChip(ItemMasterworking masterworking) {
        if (masterworking == null || !masterworking.isEnabled()) {
            return "";
        }
        return "Jakość " + MasterworkingPresentationSupport.qualityLabel(masterworking)
                + " | " + MasterworkingPresentationSupport.compactRuntimeStatusLabel();
    }
}
