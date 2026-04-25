package krys.web;

import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.item.ItemStat;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImportEditableForm;
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
                    .append(renderFullItemReadSection(parseResult.getFullItemRead(), "Pełny odczyt widocznego itemu"))
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
                .append(renderCandidateRow("Obrażenia broni", parseResult.getWeaponDamageCandidate()))
                .append(renderCandidateRow("Siła", parseResult.getStrengthCandidate()))
                .append(renderCandidateRow("Inteligencja", parseResult.getIntelligenceCandidate()))
                .append(renderCandidateRow("Kolce", parseResult.getThornsCandidate()))
                .append(renderCandidateRow("Szansa bloku", parseResult.getBlockChanceCandidate()))
                .append(renderCandidateRow("Szansa retribution", parseResult.getRetributionChanceCandidate()))
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
                .append(renderHiddenField("fullItemRead", FullItemReadFormCodec.encode(form.getFullItemRead())))
                .append("""
                            <div class="form-grid">
                """)
                .append(renderSlotSelect(form.getSlot()))
                .append(renderNumberField("weaponDamage", "Obrażenia broni", form.getWeaponDamage(), "1"))
                .append(renderNumberField("strength", "Siła", form.getStrength(), "1"))
                .append(renderNumberField("intelligence", "Inteligencja", form.getIntelligence(), "1"))
                .append(renderNumberField("thorns", "Kolce", form.getThorns(), "1"))
                .append(renderNumberField("blockChance", "Szansa bloku [%]", form.getBlockChance(), "0.01"))
                .append(renderNumberField("retributionChance", "Szansa retribution [%]", form.getRetributionChance(), "0.01"))
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
        if (!model.hasActiveHero()) {
            return "";
        }
        if (!model.hasConfirmedImport()) {
            return "";
        }

        ItemImportPageModel.ConfirmedImportView confirmed = model.getConfirmedImportView();
        ValidatedImportedItem importedItem = confirmed.getImportedItem();
        SavedImportedItem savedItem = confirmed.getSavedItem();
        ImportedItemCurrentBuildContribution contribution = confirmed.getContribution();
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Zatwierdzony item zapisany do biblioteki</h2>
                    <div class="summary-grid">
                """);
        html.append(renderSummaryCard("Plik źródłowy", importedItem.getSourceImageName()))
                .append(renderSummaryCard("Aktywny bohater", model.getActiveHero().getName()))
                .append(renderSummaryCard("Slot / typ itemu", ItemLibraryPresentationSupport.slotDisplayName(importedItem.getSlot())))
                .append(renderSummaryCard("Identyfikator biblioteki", ItemLibraryPresentationSupport.userItemIdentifier(savedItem)))
                .append(renderSummaryCard("Wkład itemu", ItemLibraryPresentationSupport.itemContributionLabel(savedItem)))
                .append(renderSummaryCard("Obrażenia broni", Long.toString(importedItem.getWeaponDamage())))
                .append(renderSummaryCard("Siła", formatWhole(importedItem.getStrength())))
                .append(renderSummaryCard("Inteligencja", formatWhole(importedItem.getIntelligence())))
                .append(renderSummaryCard("Kolce", formatWhole(importedItem.getThorns())))
                .append(renderSummaryCard("Szansa bloku [%]", formatWhole(importedItem.getBlockChance())))
                .append(renderSummaryCard("Szansa retribution [%]", formatWhole(importedItem.getRetributionChance())))
                .append("</div>")
                .append(renderFullItemReadSection(savedItem.getFullItemRead(), "Pełny odczyt zapisany w bibliotece"))
                .append("""
                    <section class="subpanel">
                        <h3>Mapowanie do modelu itemu aplikacji</h3>
                        <div class="summary-grid compact-grid">
                """)
                .append(renderSummaryCard("Nazwa itemu", confirmed.getMappedItem().getName()))
                .append(renderSummaryCard("Slot w modelu aplikacji", ItemLibraryPresentationSupport.slotDisplayName(confirmed.getMappedItem().getSlot())))
                .append(renderSummaryCard("Staty modelu itemu", renderRuntimeStatsLabel(confirmed.getMappedItem().getStats())))
                .append("""
                        </div>
                    </section>
                    <section class="subpanel">
                        <h3>Mapowanie do aktualnego modelu buildu</h3>
                        <div class="summary-grid compact-grid">
                """)
                .append(renderSummaryCard("Obrażenia broni", Long.toString(contribution.getWeaponDamage())))
                .append(renderSummaryCard("Siła", formatWhole(contribution.getStrength())))
                .append(renderSummaryCard("Inteligencja", formatWhole(contribution.getIntelligence())))
                .append(renderSummaryCard("Kolce", formatWhole(contribution.getThorns())))
                .append(renderSummaryCard("Szansa bloku [%]", formatWhole(contribution.getBlockChance())))
                .append(renderSummaryCard("Szansa retribution [%]", formatWhole(contribution.getRetributionChance())))
                .append("""
                        </div>
                        <p class="helper">Import pozostaje wspomagany: zatwierdzony item został zapisany do biblioteki przez jawnie potwierdzone pola, a nie przez ukryty automat OCR. Założenie itemu nadal tylko ustawia wybór biblioteki dla aktywnego bohatera przed tym samym current build.</p>
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
                    <div class="summary-grid compact-grid">
                """.formatted(escapeHtml(heading)));
        html.append(renderSummaryCard("Nazwa itemu", emptyLabel(fullItemRead.getItemName())))
                .append(renderSummaryCard("Typ / slot", emptyLabel(fullItemRead.getItemTypeLine())))
                .append(renderSummaryCard("Rzadkość", emptyLabel(fullItemRead.getRarity())))
                .append(renderSummaryCard("Moc przedmiotu", emptyLabel(fullItemRead.getItemPower())))
                .append(renderSummaryCard("Bazowa wartość", emptyLabel(fullItemRead.getBaseItemValue())))
                .append("</div><table class=\"data-table\"><thead><tr><th>Typ linii</th><th>Odczytana linia</th></tr></thead><tbody>");
        for (FullItemReadLine line : fullItemRead.getLines()) {
            html.append("<tr><td>")
                    .append(escapeHtml(line.getType().getDisplayName()))
                    .append("</td><td>")
                    .append(escapeHtml(line.getText()))
                    .append("</td></tr>");
        }
        html.append("</tbody></table></section>");
        return html.toString();
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

    private static String renderCandidateRow(String label, ItemImportFieldCandidate<?> candidate) {
        String suggestedValue = renderCandidateValue(candidate.getSuggestedValue());
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
            html.append(renderSlotOption(slot.name(), ItemLibraryPresentationSupport.slotDisplayName(slot), slot.name().equals(selectedSlot)));
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
}
