package krys.web;

import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.item.ItemStat;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
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
                .append(renderHiddenField("weaponDamage", form.getWeaponDamage()))
                .append(renderHiddenField("strength", form.getStrength()))
                .append(renderHiddenField("intelligence", form.getIntelligence()))
                .append(renderHiddenField("thorns", form.getThorns()))
                .append(renderHiddenField("blockChance", form.getBlockChance()))
                .append(renderHiddenField("retributionChance", form.getRetributionChance()))
                .append("""
                            <div class="form-grid">
                """)
                .append(renderSlotSelect(form.getSlot()))
                .append("""
                            </div>
                """)
                .append(renderAffixEditor(form))
                .append("""
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
                    <div class="item-read-header">
                """.formatted(escapeHtml(heading)));
        html.append(renderItemHeaderField("Nazwa", emptyLabel(fullItemRead.getItemName())))
                .append(renderItemHeaderField("Typ", simplifyItemType(fullItemRead.getItemTypeLine())))
                .append(renderItemHeaderField("Rzadkość", simplifyRarity(fullItemRead.getRarity())))
                .append(renderItemHeaderField("Moc przedmiotu", simplifyItemPower(fullItemRead.getItemPower())))
                .append(renderItemHeaderField(baseValueLabel(fullItemRead.getBaseItemValue()), simplifyBaseValue(fullItemRead.getBaseItemValue())))
                .append("</div>")
                .append("""
                    <div class="item-read-groups">
                        <h4>Pełny zapis itemu</h4>
                    """)
                .append(renderLineGroup("Linie bazowe / implicit", groupedLines(fullItemRead, ItemReadLineGroup.IMPLICIT)))
                .append(renderLineGroup("Affixy", groupedLines(fullItemRead, ItemReadLineGroup.AFFIX), true))
                .append(renderLineGroup("Aspekt / efekt legendarny", groupedLines(fullItemRead, ItemReadLineGroup.SPECIAL)))
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
        if (line.getType() == FullItemReadLineType.SOCKET) {
            return ItemReadLineGroup.SOCKET;
        }
        if (normalized.contains("REDUKCJI BLOKOWANYCH OBRAZEN")
                || normalized.contains("SZANSY NA BLOK")
                || normalized.contains("OBRAZEN OD BRONI W GLOWNEJ RECE")) {
            return ItemReadLineGroup.IMPLICIT;
        }
        if (line.getType() == FullItemReadLineType.ASPECT
                || normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE")
                || normalized.contains("TA PREMIA JEST")) {
            return ItemReadLineGroup.SPECIAL;
        }
        if (line.getType() == FullItemReadLineType.AFFIX && !normalized.contains("ROZJUSZENIE")) {
            return ItemReadLineGroup.AFFIX;
        }
        return ItemReadLineGroup.OTHER;
    }

    private static String simplifyItemType(String itemTypeLine) {
        String normalized = normalizeForDisplayRules(itemTypeLine);
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
        if (normalized.contains("LEGENDARNA") || normalized.contains("LEGENDARNY") || normalized.contains("LEGENDARY")) {
            parts.add("legendarna");
        } else if (normalized.contains("UNIKATOWA") || normalized.contains("UNIKATOWY") || normalized.contains("UNIQUE")) {
            parts.add("unikatowa");
        } else if (normalized.contains("RZADKA") || normalized.contains("RZADKI") || normalized.contains("RARE")) {
            parts.add("rzadka");
        }
        if (!parts.isEmpty()) {
            return String.join(" ", parts);
        }
        return emptyLabel(rarity);
    }

    private static String simplifyItemPower(String itemPower) {
        String value = firstNumber(itemPower);
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
        return value.isBlank() ? emptyLabel(baseItemValue) : value;
    }

    private static String firstNumber(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+(?:\\s\\d{3})*(?:[,.]\\d+)?").matcher(value);
        return matcher.find() ? matcher.group() : "";
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
                    <p class="helper">Ta lista jest głównym modelem korekty itemu. Możesz zmienić typ i wartość affixu, oznaczyć błędny wiersz do usunięcia albo dodać brakujący affix. Niżej aplikacja pokazuje tylko projekcję tych danych do obecnego runtime.</p>
                    <input type="hidden" name="affixCount" value="%s">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Typ affixu</th>
                                <th>Wartość</th>
                                <th>Odczyt OCR / źródło</th>
                                <th>Usuń</th>
                            </tr>
                        </thead>
                        <tbody>
                """.formatted(form.getAffixes().size()));
        for (int index = 0; index < form.getAffixes().size(); index++) {
            ImportedItemAffix affix = form.getAffixes().get(index);
            html.append("""
                    <tr>
                        <td>
                            <select name="affixType_%s">%s</select>
                            <input type="hidden" name="affixOriginalType_%s" value="%s">
                        </td>
                        <td>
                            <input type="number" min="0" step="0.01" name="affixValue_%s" value="%s">
                            <input type="hidden" name="affixOriginalValue_%s" value="%s">
                        </td>
                        <td>
                            %s
                            <input type="hidden" name="affixSourceText_%s" value="%s">
                        </td>
                        <td><label class="checkbox-label"><input type="checkbox" name="affixRemoved_%s" value="true"> Usuń</label></td>
                    </tr>
                    """.formatted(
                    index,
                    renderAffixTypeOptions(affix.getType()),
                    index,
                    affix.getType().name(),
                    index,
                    formatDecimal(affix.getValue()),
                    index,
                    formatDecimal(affix.getValue()),
                    escapeHtml(affix.toDisplayLine()),
                    index,
                    escapeHtml(affix.getSourceText()),
                    index
            ));
        }
        html.append("""
                        </tbody>
                    </table>
                    <div class="add-affix-row">
                        <h4>Dodaj affix</h4>
                        <div class="form-grid">
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
                        </div>
                    </div>
                    <section class="subpanel">
                        <h4>Projekcja do aktualnego runtime</h4>
                        <p class="helper">Te pola są wtórnym widokiem kompatybilności. Aktualny runtime użyje tylko obsługiwanego podzbioru: siła, inteligencja, kolce, szansa bloku, szansa retribution i obrażenia broni.</p>
                        <div class="summary-grid compact-grid">
                            %s
                            %s
                            %s
                            %s
                            %s
                            %s
                        </div>
                    </section>
                </section>
                """.formatted(
                renderAffixTypeOptions(null),
                renderSummaryCard("Obrażenia broni", emptyNumberLabel(form.getWeaponDamage())),
                renderSummaryCard("Siła", emptyNumberLabel(projectedAffixValue(form, ImportedItemAffixType.STRENGTH, form.getStrength()))),
                renderSummaryCard("Inteligencja", emptyNumberLabel(projectedAffixValue(form, ImportedItemAffixType.INTELLIGENCE, form.getIntelligence()))),
                renderSummaryCard("Kolce", emptyNumberLabel(projectedAffixValue(form, ImportedItemAffixType.THORNS, form.getThorns()))),
                renderSummaryCard("Szansa bloku [%]", emptyNumberLabel(projectedAffixValue(form, ImportedItemAffixType.BLOCK_CHANCE, form.getBlockChance()))),
                renderSummaryCard("Szansa retribution [%]", emptyNumberLabel(projectedAffixValue(form, ImportedItemAffixType.RETRIBUTION_CHANCE, form.getRetributionChance())))
        ));
        return html.toString();
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

    private static String formatDecimal(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value);
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
