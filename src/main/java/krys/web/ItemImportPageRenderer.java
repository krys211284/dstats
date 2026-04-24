package krys.web;

import krys.item.EquipmentSlot;
import krys.item.ItemStat;
import krys.itemimport.CurrentBuildItemApplicationMode;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportFieldCandidate;
import krys.itemimport.ValidatedImportedItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renderuje prosty SSR dla flow importu pojedynczego itemu ze screena z ręcznym zatwierdzeniem. */
public final class ItemImportPageRenderer {
    private final String template;

    public ItemImportPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(ItemImportPageModel model) {
        return template
                .replace("{{GLOBAL_NAV}}", AppShellRendererSupport.renderGlobalNavigation("/importuj-item-ze-screena"))
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
        if (!model.hasEditableForm()) {
            return """
                    <section class="panel result-panel">
                        <h2>Wstępnie rozpoznane pola</h2>
                        <p>Wgraj screenshot pojedynczego itemu. Foundation importu sprawdzi, czy plik jest prawidłowym obrazem, pokaże poziom niepewności i pozwoli ręcznie zatwierdzić wartości.</p>
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
                    .append("""
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Pole</th>
                                <th>Sugerowana wartość</th>
                                <th>Pewność</th>
                                <th>Uwagi</th>
                            </tr>
                        </thead>
                        <tbody>
                """)
                .append(renderCandidateRow("Slot / typ itemu", parseResult.getSlotCandidate()))
                .append(renderCandidateRow("Weapon damage", parseResult.getWeaponDamageCandidate()))
                .append(renderCandidateRow("Strength", parseResult.getStrengthCandidate()))
                .append(renderCandidateRow("Intelligence", parseResult.getIntelligenceCandidate()))
                .append(renderCandidateRow("Thorns", parseResult.getThornsCandidate()))
                .append(renderCandidateRow("Block chance", parseResult.getBlockChanceCandidate()))
                .append(renderCandidateRow("Retribution chance", parseResult.getRetributionChanceCandidate()))
                .append("""
                        </tbody>
                    </table>
                    """);
        } else {
            html.append("<p class=\"helper\">Brak pełnego odczytu obrazu w bieżącym renderze. Możesz dalej ręcznie poprawić pola i zatwierdzić item.</p>");
        }

        html.append("""
                    <section class="subpanel">
                        <h3>Ręczne potwierdzenie itemu</h3>
                        <form method="post" action="/importuj-item-ze-screena">
                """)
                .append(renderHiddenField("sourceImageName", form.getSourceImageName()))
                .append(renderHiddenField("currentBuildQuery", model.getCurrentBuildQuery()))
                .append("""
                            <div class="form-grid">
                """)
                .append(renderSlotSelect(form.getSlot()))
                .append(renderNumberField("weaponDamage", "Weapon damage", form.getWeaponDamage(), "1"))
                .append(renderNumberField("strength", "Strength", form.getStrength(), "1"))
                .append(renderNumberField("intelligence", "Intelligence", form.getIntelligence(), "1"))
                .append(renderNumberField("thorns", "Thorns", form.getThorns(), "1"))
                .append(renderNumberField("blockChance", "Block chance [%]", form.getBlockChance(), "0.01"))
                .append(renderNumberField("retributionChance", "Retribution chance [%]", form.getRetributionChance(), "0.01"))
                .append("""
                            </div>
                            <div class="submit-row">
                                <button type="submit">Zatwierdź item</button>
                            </div>
                        </form>
                    </section>
                </section>
                """);
        return html.toString();
    }

    private static String renderConfirmSection(ItemImportPageModel model) {
        if (!model.hasConfirmedImport()) {
            return "";
        }

        ItemImportPageModel.ConfirmedImportView confirmed = model.getConfirmedImportView();
        ValidatedImportedItem importedItem = confirmed.getImportedItem();
        ImportedItemCurrentBuildContribution contribution = confirmed.getContribution();
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Zatwierdzony item</h2>
                    <div class="summary-grid">
                """);
        html.append(renderSummaryCard("Plik źródłowy", importedItem.getSourceImageName()))
                .append(renderSummaryCard("Slot / typ itemu", importedItem.getSlot().name()))
                .append(renderSummaryCard("Weapon damage", Long.toString(importedItem.getWeaponDamage())))
                .append(renderSummaryCard("Strength", formatWhole(importedItem.getStrength())))
                .append(renderSummaryCard("Intelligence", formatWhole(importedItem.getIntelligence())))
                .append(renderSummaryCard("Thorns", formatWhole(importedItem.getThorns())))
                .append(renderSummaryCard("Block chance [%]", formatWhole(importedItem.getBlockChance())))
                .append(renderSummaryCard("Retribution chance [%]", formatWhole(importedItem.getRetributionChance())))
                .append("</div>")
                .append("""
                    <section class="subpanel">
                        <h3>Mapowanie do modelu itemu aplikacji</h3>
                        <div class="summary-grid compact-grid">
                """)
                .append(renderSummaryCard("Nazwa itemu", confirmed.getMappedItem().getName()))
                .append(renderSummaryCard("Slot runtime", confirmed.getMappedItem().getSlot().name()))
                .append(renderSummaryCard("Staty runtime", renderRuntimeStatsLabel(confirmed.getMappedItem().getStats())))
                .append("""
                        </div>
                    </section>
                    <section class="subpanel">
                        <h3>Mapowanie do aktualnego modelu buildu</h3>
                        <div class="summary-grid compact-grid">
                """)
                .append(renderSummaryCard("Weapon damage", Long.toString(contribution.getWeaponDamage())))
                .append(renderSummaryCard("Strength", formatWhole(contribution.getStrength())))
                .append(renderSummaryCard("Intelligence", formatWhole(contribution.getIntelligence())))
                .append(renderSummaryCard("Thorns", formatWhole(contribution.getThorns())))
                .append(renderSummaryCard("Block chance [%]", formatWhole(contribution.getBlockChance())))
                .append(renderSummaryCard("Retribution chance [%]", formatWhole(contribution.getRetributionChance())))
                .append("""
                        </div>
                        <p class="helper">Import pozostaje wspomagany: zatwierdzony item zasila aktualny agregowany model buildu przez jawne mapowanie pól, a nie przez ukryty automat OCR.</p>
                        <div class="action-links">
                            <a class="link-button" href=\"""")
                .append(escapeHtml(confirmed.getOverwriteCurrentBuildUrl()))
                .append("\">")
                .append(escapeHtml(CurrentBuildItemApplicationMode.OVERWRITE.getDisplayName()))
                .append("</a>")
                .append("<a class=\"link-button secondary-button\" href=\"")
                .append(escapeHtml(confirmed.getAddContributionCurrentBuildUrl()))
                .append("\">")
                .append(escapeHtml(CurrentBuildItemApplicationMode.ADD_CONTRIBUTION.getDisplayName()))
                .append("</a>")
                .append("""
                        </div>
                        <form method="post" action="/biblioteka-itemow" class="submit-row">
                    """)
                .append(renderHiddenField("action", "saveImportedItem"))
                .append(renderHiddenField("currentBuildQuery", model.getCurrentBuildQuery()))
                .append(renderHiddenField("sourceImageName", importedItem.getSourceImageName()))
                .append(renderHiddenField("slot", importedItem.getSlot().name()))
                .append(renderHiddenField("weaponDamage", Long.toString(importedItem.getWeaponDamage())))
                .append(renderHiddenField("strength", formatWhole(importedItem.getStrength())))
                .append(renderHiddenField("intelligence", formatWhole(importedItem.getIntelligence())))
                .append(renderHiddenField("thorns", formatWhole(importedItem.getThorns())))
                .append(renderHiddenField("blockChance", formatWhole(importedItem.getBlockChance())))
                .append(renderHiddenField("retributionChance", formatWhole(importedItem.getRetributionChance())))
                .append("""
                            <button type="submit">Zapisz do biblioteki</button>
                        </form>
                    """)
                .append("""
                        <p class="helper">`Zastosuj do current build` zachowuje techniczne znaczenie trybu `nadpisz`: podstawia wkład itemu tylko w polach, które item rzeczywiście wnosi. `Dodaj wkład do current build` sumuje ten wkład do statów current build przekazanych do importu.</p>
                    """)
                .append("""
                    </section>
                </section>
                """);
        return html.toString();
    }

    private static String renderCandidateRow(String label, ItemImportFieldCandidate<?> candidate) {
        String suggestedValue = candidate.getSuggestedValue() == null ? "Brak" : candidate.getSuggestedValue().toString();
        String note = candidate.getNote() == null || candidate.getNote().isBlank() ? "-" : candidate.getNote();
        return "<tr><td>" + escapeHtml(label) + "</td><td>" + escapeHtml(suggestedValue) + "</td><td>"
                + escapeHtml(candidate.getConfidence().getDisplayName()) + "</td><td>" + escapeHtml(note) + "</td></tr>";
    }

    private static String renderSlotSelect(String selectedSlot) {
        StringBuilder html = new StringBuilder("""
                <label>
                    Slot / typ itemu
                    <select name="slot">
                """);
        html.append(renderSlotOption("", "Wybierz slot", selectedSlot == null || selectedSlot.isBlank()));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            html.append(renderSlotOption(slot.name(), slot.name(), slot.name().equals(selectedSlot)));
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

    private static String renderSummaryCard(String label, String value) {
        return CurrentBuildCalculationSectionsRenderer.renderSummaryCard(label, value);
    }

    private static String renderRuntimeStatsLabel(List<ItemStat> stats) {
        if (stats.isEmpty()) {
            return "Brak statów runtime";
        }
        List<String> labels = new ArrayList<>();
        for (ItemStat stat : stats) {
            labels.add(stat.getType().name() + "=" + formatWhole(stat.getValue()));
        }
        return String.join(", ", labels);
    }

    private static String formatWhole(double value) {
        return String.format(Locale.US, "%.0f", value);
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
}
