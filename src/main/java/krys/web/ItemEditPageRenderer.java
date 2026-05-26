package krys.web;

import krys.item.EquipmentSlot;
import krys.itemimport.AffixDefinition;
import krys.itemimport.AffixRegistry;
import krys.itemimport.ApplicationAffixRegistry;
import krys.itemimport.ApplicationAspectRegistry;
import krys.itemimport.AspectDefinition;
import krys.itemimport.AspectRegistry;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportEditableForm;
import krys.itemlibrary.ItemLibraryPresentationSupport;

import java.util.Locale;

/** Renderuje SSR edycji zapisanego itemu bez ponownego OCR. */
final class ItemEditPageRenderer {
    private static final AspectRegistry ASPECT_REGISTRY = ApplicationAspectRegistry.get();
    private static final AffixRegistry AFFIX_REGISTRY = ApplicationAffixRegistry.get();

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
                        .layout.wide-item-page { max-width: 1500px; }
                        .panel { margin-bottom: 18px; padding: 18px; border: 1px solid var(--line); border-radius: 16px; background: rgba(255, 250, 242, 0.96); box-shadow: 0 12px 24px rgba(36, 33, 29, 0.05); }
                        .panel-error { border-color: rgba(141, 27, 27, 0.28); background: var(--error-bg); }
                        .panel-success { border-color: rgba(44, 122, 84, 0.28); background: rgba(239, 249, 241, 0.96); }
                        .manual-confirm-grid { display: grid; grid-template-columns: repeat(2, minmax(240px, 1fr)); gap: 14px; align-items: end; }
                        label { display: grid; gap: 8px; font-weight: 600; }
                        .checkbox-label { display: inline-flex; gap: 8px; align-items: center; font-weight: 600; }
                        input[type="number"], input[type="text"], select, textarea { width: 100%%; padding: 10px 12px; border: 1px solid var(--line); border-radius: 10px; background: #fff; color: var(--text); font: inherit; }
                        textarea { resize: vertical; }
                        [hidden] { display: none !important; }
                        .inline-fieldset { display: grid; gap: 10px; grid-column: 1 / -1; padding: 12px; border: 1px solid var(--line); border-radius: 10px; background: rgba(255, 255, 255, 0.72); }
                        .inline-fieldset legend { padding: 0 6px; color: var(--muted); font-weight: 700; }
                        .aspect-effect-fieldset { grid-template-columns: minmax(240px, 0.75fr) minmax(320px, 1.25fr); align-items: start; }
                        .aspect-effect-text textarea { min-height: 112px; }
                        .helper { margin-top: 10px; color: var(--muted); font-size: 0.95rem; }
                        .submit-row { margin-top: 16px; display: flex; gap: 10px; flex-wrap: wrap; }
                        button, .link-button, .secondary-link { display: inline-block; border: none; border-radius: 999px; padding: 12px 18px; background: linear-gradient(135deg, #1a4b5a 0%%, #2d7288 100%%); color: #f5fbfd; font: inherit; font-weight: 700; text-decoration: none; cursor: pointer; }
                        .secondary-button, .secondary-link { background: linear-gradient(135deg, #5b7a86 0%%, #7898a4 100%%); }
                        .subpanel { margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--line); }
                        .add-affix-row { margin-top: 14px; padding: 12px; border: 1px solid rgba(109, 102, 92, 0.18); border-radius: 10px; background: rgba(255, 255, 255, 0.72); }
                        .item-affix-add-grid { display: grid; grid-template-columns: minmax(280px, 1.4fr) minmax(120px, 0.55fr) minmax(150px, 0.45fr) auto; gap: 12px; align-items: end; }
                        .item-affix-add-actions { display: flex; align-items: end; justify-content: flex-end; gap: 8px; }
                        .tempering-readonly-list { display: grid; gap: 12px; margin-top: 12px; }
                        .tempering-readonly-card { display: grid; grid-template-columns: minmax(120px, 0.45fr) minmax(320px, 1.4fr) minmax(120px, 0.4fr) minmax(180px, 0.55fr) minmax(220px, 0.75fr) minmax(180px, 0.6fr) auto; gap: 12px; align-items: center; padding: 12px; border: 1px solid rgba(109, 102, 92, 0.18); border-radius: 10px; background: rgba(255, 255, 255, 0.84); }
                        .tempering-readonly-field { display: grid; gap: 4px; min-width: 0; }
                        .tempering-readonly-label { color: var(--muted); font-size: 0.82rem; font-weight: 700; }
                        .tempering-readonly-value { overflow-wrap: anywhere; }
                        .tempering-readonly-actions { display: flex; justify-content: flex-end; }
                        .tempering-existing-card { display: grid; grid-template-columns: minmax(110px, 0.5fr) minmax(320px, 1.8fr) minmax(150px, 0.55fr) auto; gap: 12px; align-items: center; padding: 12px; border: 1px solid rgba(109, 102, 92, 0.18); border-radius: 10px; background: rgba(255, 255, 255, 0.84); }
                        .tempering-existing-header { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; min-width: 0; }
                        .tempering-existing-category { font-weight: 800; }
                        .tempering-existing-affix { overflow-wrap: anywhere; }
                        .tempering-greater-badge, .tempering-runtime-status { display: inline-flex; width: fit-content; align-items: center; gap: 4px; padding: 4px 8px; border-radius: 999px; border: 1px solid rgba(30, 85, 102, 0.18); background: #f8fbfc; color: var(--accent); font-size: 0.84rem; font-weight: 700; }
                        .tempering-runtime-status { color: var(--muted); border-color: rgba(109, 102, 92, 0.18); background: #fff; }
                        .tempering-existing-actions { display: flex; justify-content: flex-end; }
                        .tempering-add-card { margin-top: 14px; padding: 14px; border: 1px solid rgba(109, 102, 92, 0.18); border-radius: 10px; background: rgba(255, 255, 255, 0.72); }
                        .tempering-add-grid { display: grid; grid-template-columns: minmax(140px, 0.55fr) minmax(360px, 1.8fr) minmax(150px, 0.5fr) minmax(220px, 0.75fr) auto; gap: 12px; align-items: start; }
                        .tempering-add-field-affix { min-width: 0; }
                        .tempering-add-field-action { align-self: end; }
                        .tempering-validation-message { grid-column: 1 / -1; }
                        .tempering-validation-message .helper { color: var(--error); }
                        .affix-table-wrap { width: 100%%; overflow-x: auto; padding-bottom: 4px; }
                        .data-table { width: 100%%; border-collapse: collapse; font-size: 0.94rem; }
                        .affix-table { min-width: 820px; table-layout: fixed; }
                        .affix-type-col { width: 36%%; }
                        .affix-value-col { width: 16%%; }
                        .affix-range-col { width: 17%%; }
                        .affix-greater-col { width: 16%%; }
                        .affix-action-col { width: 15%%; }
                        .data-table th, .data-table td { padding: 10px 8px; border-bottom: 1px solid rgba(109, 102, 92, 0.18); text-align: left; vertical-align: top; }
                        .data-table thead th { color: var(--muted); font-weight: 700; }
                        .affix-table select, .affix-table input[type="number"] { min-width: 0; height: 42px; }
                        .affix-value-cell input[type="number"] { max-width: 120px; }
                        .affix-range-cell { white-space: nowrap; }
                        .affix-action-cell { text-align: right; white-space: nowrap; }
                        .affix-greater-cell .checkbox-label { min-height: 40px; }
                        .tempering-category-notes { margin-top: 10px; }
                        .masterworking-grid { display: grid; grid-template-columns: minmax(160px, 0.55fr) minmax(220px, 1fr); gap: 12px; align-items: end; }
                        .masterworking-runtime-status { display: inline-flex; width: fit-content; align-items: center; padding: 6px 10px; border-radius: 999px; border: 1px solid rgba(109, 102, 92, 0.18); background: #fff; color: var(--muted); font-weight: 700; }
                        .transfiguration-grid { display: grid; grid-template-columns: repeat(4, minmax(160px, 1fr)); gap: 12px; align-items: end; }
                        .transfiguration-dynamic-grid { margin-top: 12px; grid-template-columns: repeat(3, minmax(180px, 1fr)); }
                        .socketing-grid { display: grid; grid-template-columns: minmax(160px, 0.4fr); gap: 12px; align-items: end; }
                        .socketing-row { display: grid; grid-template-columns: minmax(140px, 0.45fr) minmax(280px, 1fr) minmax(260px, 1fr); gap: 12px; align-items: end; }
                        @media (max-width: 900px) {
                            .manual-confirm-grid, .aspect-effect-fieldset, .item-affix-add-grid, .tempering-add-grid, .tempering-readonly-card, .tempering-existing-card, .masterworking-grid, .transfiguration-grid, .transfiguration-dynamic-grid, .socketing-row { grid-template-columns: 1fr; }
                            .item-affix-add-actions, .tempering-readonly-actions, .tempering-existing-actions { justify-content: flex-start; }
                            .data-table, .data-table thead, .data-table tbody, .data-table tr, .data-table th, .data-table td { display: block; }
                            .data-table thead { display: none; }
                            .affix-table { min-width: 0; }
                            .data-table tr { margin-bottom: 12px; padding: 10px; border: 1px solid rgba(109, 102, 92, 0.18); border-radius: 12px; background: #fff; }
                            .data-table td { border: none; padding: 6px 0; }
                        }
                    </style>
                </head>
                <body>
                <main class="layout wide-item-page">
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
                    <h2>Ręczna edycja itemu</h2>
                    <form method="post" action="/biblioteka-itemow/edytuj">
                        <input type="hidden" name="action" value="updateItem">
                        <input type="hidden" name="itemId" value="%s">
                        <input type="hidden" name="sourceImageName" value="%s">
                        <input type="hidden" name="fullItemRead" value="%s">
                        <input type="hidden" name="weaponDamage" value="%s">
                        <input type="hidden" name="strength" value="%s">
                        <input type="hidden" name="intelligence" value="%s">
                        <input type="hidden" name="thorns" value="%s">
                        <input type="hidden" name="blockChance" value="%s">
                        <input type="hidden" name="retributionChance" value="%s">
                        %s
                        <div class="manual-confirm-grid">
                            %s
                            %s
                            %s
                            %s
                            %s
                            %s
                            %s
                        </div>
                        %s
                        %s
                        %s
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
                escapeHtml(emptyNumberLabel(form.getWeaponDamage())),
                escapeHtml(emptyNumberLabel(form.getStrength())),
                escapeHtml(emptyNumberLabel(form.getIntelligence())),
                escapeHtml(emptyNumberLabel(form.getThorns())),
                escapeHtml(emptyNumberLabel(form.getBlockChance())),
                escapeHtml(emptyNumberLabel(form.getRetributionChance())),
                ItemLibraryFilterQuerySupport.hiddenFields(model.getFilter()),
                renderItemIdentityFields(form, model.getItem()),
                renderSlotSelect(form.getSlot()),
                renderRaritySelect(form.getItemRarity()),
                renderAncientCheckbox(form.isAncient()),
                renderNumberField("itemPower", "Moc przedmiotu", form.getItemPower(), "1"),
                renderItemTypeFieldSet(form),
                renderAspectSelect(form),
                renderAffixEditor(form),
                TemperingSectionRenderer.renderEditor(form),
                MasterworkingSectionRenderer.renderEditor(form),
                TransfigurationSectionRenderer.renderEditor(form),
                SocketingSectionRenderer.renderEditor(form),
                escapeHtml(ItemLibraryFilterQuerySupport.libraryUrl(model.getFilter()))
        );
    }

    private static String renderItemIdentityFields(ItemImportEditableForm form, krys.itemlibrary.SavedImportedItem item) {
        return """
                <label>
                    Nazwa
                    <input type="text" name="itemName" value="%s">
                </label>
                <label>
                    Typ itemu
                    <input type="text" name="itemType" value="%s">
                </label>
                """.formatted(
                escapeHtml(firstNonBlank(form.getItemName(), ItemLibraryPresentationSupport.canonicalItemName(item))),
                escapeHtml(firstNonBlank(form.getItemType(), simplifyItemType(form.getFullItemRead().getItemTypeLine())))
        );
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

    private static String renderRaritySelect(String selectedRarity) {
        String selected = selectedRarity == null ? "" : selectedRarity;
        return """
                <label>
                    Rzadkość
                    <select name="itemRarity">
                        %s
                    </select>
                </label>
                """.formatted(
                renderRarityOption("", "UNKNOWN / do potwierdzenia", selected.isBlank())
                        + renderRarityOption("UNIQUE", "UNIQUE / Unikatowy", "UNIQUE".equalsIgnoreCase(selected))
                        + renderRarityOption("LEGENDARY", "LEGENDARY / Legendarny", "LEGENDARY".equalsIgnoreCase(selected))
                        + renderRarityOption("RARE", "RARE / Rzadki", "RARE".equalsIgnoreCase(selected))
        );
    }

    private static String renderRarityOption(String value, String label, boolean selected) {
        return "<option value=\"" + escapeHtml(value) + "\"" + (selected ? " selected" : "") + ">"
                + escapeHtml(label) + "</option>";
    }

    private static String renderAncientCheckbox(boolean ancient) {
        return """
                <label class="checkbox-label">
                    <input type="hidden" name="isAncientSubmitted" value="true">
                    <input type="checkbox" name="isAncient" value="true"%s> Ancient / starożytny
                </label>
                """.formatted(ancient ? " checked" : "");
    }

    private static String renderItemTypeFieldSet(ItemImportEditableForm form) {
        if (isShield(form)) {
            return """
                    <fieldset class="inline-fieldset">
                        <legend>Dane tarczy</legend>
                        %s
                        %s
                    </fieldset>
                    """.formatted(
                    renderNumberField("itemArmor", "Pancerz", form.getItemArmor(), "1"),
                    MasterworkingSectionRenderer.renderArmorEditorHint(form.getMasterworking(), form.getItemArmor())
            );
        }
        if (!isWeapon(form)) {
            return "<input type=\"hidden\" name=\"itemArmor\" value=\"" + escapeHtml(form.getItemArmor()) + "\">";
        }
        return renderWeaponFieldSet(form);
    }

    private static String renderWeaponFieldSet(ItemImportEditableForm form) {
        String averageValue = form.getAverageWeaponDamage();
        if ((averageValue == null || averageValue.isBlank())
                && !form.getWeaponDamageMin().isBlank()
                && !form.getWeaponDamageMax().isBlank()) {
            try {
                long min = Long.parseLong(form.getWeaponDamageMin());
                long max = Long.parseLong(form.getWeaponDamageMax());
                averageValue = Long.toString(Math.round((min + max) / 2.0d));
            } catch (NumberFormatException exception) {
                averageValue = form.getAverageWeaponDamage();
            }
        }
        return """
                <fieldset class="inline-fieldset">
                    <legend>Dane broni</legend>
                    %s
                    %s
                    %s
                    %s
                    %s
                </fieldset>
                """.formatted(
                renderNumberField("weaponDps", "DPS broni", form.getWeaponDps(), "1"),
                renderNumberField("weaponDamageMin", "Obrażenia za trafienie min", form.getWeaponDamageMin(), "1"),
                renderNumberField("weaponDamageMax", "Obrażenia za trafienie max", form.getWeaponDamageMax(), "1"),
                renderNumberField("averageWeaponDamage", "Średnie obrażenia trafienia", averageValue, "1"),
                renderNumberField("attacksPerSecond", "Ataki na sekundę", form.getAttacksPerSecond(), "0.01")
        );
    }

    private static boolean isShield(ItemImportEditableForm form) {
        EquipmentSlot slot = parseSlot(form.getSlot());
        String normalizedType = java.text.Normalizer.normalize(form.getItemType() == null ? "" : form.getItemType(), java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        return slot == EquipmentSlot.OFF_HAND && (normalizedType.contains("TARCZA") || !form.getItemArmor().isBlank());
    }

    private static boolean isWeapon(ItemImportEditableForm form) {
        EquipmentSlot slot = parseSlot(form.getSlot());
        String normalizedType = java.text.Normalizer.normalize(form.getItemType() == null ? "" : form.getItemType(), java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        return slot == EquipmentSlot.MAIN_HAND
                || normalizedType.contains("MIECZ")
                || normalizedType.contains("SWORD")
                || !form.getWeaponDps().isBlank()
                || !form.getWeaponDamageMin().isBlank()
                || !form.getWeaponDamageMax().isBlank()
                || !form.getAverageWeaponDamage().isBlank()
                || !form.getAttacksPerSecond().isBlank();
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
            boolean allowed = selectedSlot != null && aspect.allowsSlot(selectedSlot);
            boolean selected = aspect.getId().equals(selectedAspectId);
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
        html.append("""
                    </select>
                </label>
                <label class="aspect-effect-text">
                    Treść efektu
                    <textarea name="uniqueEffectText" rows="4">%s</textarea>
                </label>
                """.formatted(escapeHtml(aspectEffectText(form, selectedAspect))));
        return "<fieldset class=\"inline-fieldset aspect-effect-fieldset\"><legend>Aspekt / efekt</legend>"
                + html
                + "</fieldset>";
    }

    private static String renderAffixEditor(ItemImportEditableForm form) {
        StringBuilder html = new StringBuilder("""
                <section class="subpanel">
                    <h3>Affixy</h3>
                    <input type="hidden" id="affixCount" name="affixCount" value="%s">
                    <div class="affix-table-wrap">
                    <table class="data-table affix-table" id="affixTable">
                        <colgroup>
                            <col class="affix-type-col">
                            <col class="affix-value-col">
                            <col class="affix-range-col">
                            <col class="affix-greater-col">
                            <col class="affix-action-col">
                        </colgroup>
                        <thead>
                            <tr>
                                <th>Typ affixu</th>
                                <th>Wartość</th>
                                <th>Zakres rolla</th>
                                <th>Greater Affix</th>
                                <th>Akcja</th>
                            </tr>
                        </thead>
                        <tbody id="affixRows">
                """.formatted(form.getAffixes().size()));
        for (int index = 0; index < form.getAffixes().size(); index++) {
            ImportedItemAffix affix = form.getAffixes().get(index);
            html.append("""
                    <tr>
                        <td class="affix-type-cell">
                            <select name="affixType_%s">%s</select>
                            <input type="hidden" name="affixSourceText_%s" value="%s">
                            <input type="hidden" name="affixSource_%s" value="%s">
                            <input type="hidden" name="affixOriginalType_%s" value="%s">
                            <input type="hidden" name="affixOriginalValue_%s" value="%s">
                            <input type="hidden" name="affixDefinitionId_%s" value="%s">
                            <input type="hidden" name="affixRangeMin_%s" value="%s">
                            <input type="hidden" name="affixRangeMax_%s" value="%s">
                            <input type="hidden" name="affixDisplayValue_%s" value="%s">
                        </td>
                        <td class="affix-value-cell">%s<label class="masterworking-source-value-field">%s</label></td>
                        <td class="affix-range-cell">%s</td>
                        <td class="affix-greater-cell"><label class="checkbox-label"><input type="checkbox" name="affixGreater_%s" value="true"%s> Gwiazdka</label></td>
                        <td class="affix-action-cell"><button type="button" class="secondary-button remove-affix-button">Usuń</button></td>
                    </tr>
                    """.formatted(
                    index,
                    renderAffixTypeOptions(affix.getType()),
                    index,
                    escapeHtml(affix.getSourceText()),
                    index,
                    affix.getSource().name(),
                    index,
                    escapeHtml(affix.getType().name()),
                    index,
                    formatDecimal(affix.getValue()),
                    index,
                    escapeHtml(affix.getAffixDefinitionId()),
                    index,
                    affix.getRollRangeMin() == null ? "" : formatDecimal(affix.getRollRangeMin()),
                    index,
                    affix.getRollRangeMax() == null ? "" : formatDecimal(affix.getRollRangeMax()),
                    index,
                    escapeHtml(affix.getDisplayValue()),
                    MasterworkingSectionRenderer.renderAffixEditorHint(form.getMasterworking(), affix),
                    renderAffixValueControl(index, affix),
                    escapeHtml(rollRangeLabel(affix)),
                    index,
                    affix.isGreaterAffix() ? " checked" : ""
            ));
        }
        html.append("""
                        </tbody>
                    </table>
                    </div>
                    """);
        if (form.getAffixes().size() >= 4) {
            html.append("""
                    <div class="add-affix-row">
                        <p class="helper">Limit affixów dla tego przedmiotu został wykorzystany.</p>
                    </div>
                    """);
        } else {
            html.append("""
                    <div class="add-affix-row">
                        <h4>Dodaj affix</h4>
                        <div class="item-affix-add-grid">
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
                            <label class="checkbox-label">
                                <input type="checkbox" id="newAffixGreater" name="newAffixGreater" value="true"> Greater Affix
                            </label>
                            <div class="item-affix-add-actions">
                                <button type="button" id="addAffixButton">Dodaj affix</button>
                                <noscript>
                                    <button type="submit" name="formAction" value="addAffix">Dodaj affix</button>
                                </noscript>
                            </div>
                        </div>
                    </div>
                    <template id="affixRowTemplate">
                        <tr>
                            <td class="affix-type-cell"><select name="affixType___INDEX__">%s</select><input type="hidden" name="affixSourceText___INDEX__" value=""><input type="hidden" name="affixSource___INDEX__" value="MANUAL"><input type="hidden" name="affixOriginalType___INDEX__" value=""><input type="hidden" name="affixOriginalValue___INDEX__" value=""><input type="hidden" name="affixDefinitionId___INDEX__" value=""><input type="hidden" name="affixRangeMin___INDEX__" value=""><input type="hidden" name="affixRangeMax___INDEX__" value=""><input type="hidden" name="affixDisplayValue___INDEX__" value=""></td>
                            <td class="affix-value-cell"><input type="number" min="0" step="0.01" name="affixValue___INDEX__" value="__VALUE__"></td>
                            <td class="affix-range-cell"><span class="helper">Brak zakresu</span></td>
                            <td class="affix-greater-cell"><label class="checkbox-label"><input type="checkbox" name="affixGreater___INDEX__" value="true"> Gwiazdka</label></td>
                            <td class="affix-action-cell"><button type="button" class="secondary-button remove-affix-button">Usuń</button></td>
                        </tr>
                    </template>
                    """.formatted(renderAffixTypeOptions(null), renderAffixTypeOptions(null)));
        }
        html.append("""
                </section>
                """);
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
                    if (!rows || !count) return;
                    const renumberRows = () => {
                        Array.from(rows.querySelectorAll('tr')).forEach((row, index) => {
                            row.querySelectorAll('select[name^="affixType_"], input[name^="affixValue_"], input[name^="affixGreater_"], input[name^="affixSourceText_"], input[name^="affixSource_"], input[name^="affixOriginalType_"], input[name^="affixOriginalValue_"], input[name^="affixDefinitionId_"], input[name^="affixRangeMin_"], input[name^="affixRangeMax_"], input[name^="affixDisplayValue_"]').forEach(control => {
                                const base = control.name.substring(0, control.name.lastIndexOf('_'));
                                control.name = `${base}_${index}`;
                            });
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
                    if (template && addButton && newType && newValue) {
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
                    }
                    const form = rows.closest('form');
                    if (form) form.addEventListener('submit', renumberRows);
                })();
                %s
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
                """.formatted(TemperingSectionRenderer.renderScript()
                + "\n"
                + TransfigurationSectionRenderer.renderScript()
                + "\n"
                + SocketingSectionRenderer.renderScript());
    }

    private static String renderAffixTypeOptions(ImportedItemAffixType selectedType) {
        StringBuilder html = new StringBuilder();
        for (AffixDefinition definition : AFFIX_REGISTRY.all()) {
            ImportedItemAffixType type = definition.getFormType();
            html.append("<option value=\"")
                    .append(type.name())
                    .append("\" data-affix-definition-id=\"")
                    .append(escapeHtml(definition.getId()))
                    .append("\" title=\"")
                    .append(escapeHtml(definition.getDescription()))
                    .append("\" aria-label=\"")
                    .append(escapeHtml(definition.getDescription()))
                    .append("\"")
                    .append(type == selectedType ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(definition.getDisplayName()))
                    .append("</option>");
        }
        return html.toString();
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

    private static String renderAffixValueControl(int index, ImportedItemAffix affix) {
        return "<input type=\"number\" min=\"0\" step=\"0.01\" name=\"affixValue_"
                + index
                + "\" value=\""
                + escapeHtml(formatDecimal(affix == null ? 0.0d : affix.getValue()))
                + "\">";
    }

    private static String aspectEffectText(ItemImportEditableForm form, AspectDefinition selectedAspect) {
        String effectText = firstNonBlank(form.getUniqueEffectText(), form.getFullItemRead().getDetails().getUniqueEffectText());
        if (effectText.isBlank() && selectedAspect != null) {
            return selectedAspect.getEffectDescription();
        }
        return effectText;
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

    private static String simplifyItemType(String itemTypeLine) {
        String normalized = java.text.Normalizer.normalize(itemTypeLine == null ? "" : itemTypeLine, java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        if (normalized.contains("MIECZ") || normalized.contains("SWORD")) {
            return "Miecz";
        }
        if (normalized.contains("TARCZA")) {
            return "Tarcza";
        }
        if (normalized.contains("BUTY")) {
            return "Buty";
        }
        return itemTypeLine == null ? "" : itemTypeLine;
    }

    private static String formatDecimal(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String rollRangeLabel(ImportedItemAffix affix) {
        if (affix != null && affix.isGreaterAffix()
                && affix.getRollRangeMin() == null
                && affix.getRollRangeMax() == null) {
            return "Bez zakresu (Greater Affix)";
        }
        String value = affix == null ? "" : affix.getRollRangeLabel();
        return value == null || value.isBlank() ? "Brak zakresu" : value;
    }

    private static String emptyNumberLabel(String value) {
        return value == null || value.isBlank() ? "0" : value;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? (fallback == null ? "" : fallback) : preferred;
    }

    private static String escapeHtml(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }
}
