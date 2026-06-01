package krys.web;

import krys.item.EquipmentSlot;
import krys.itemimport.ItemImportEditableForm;
import krys.itemlibrary.SavedImportedItem;
import krys.socketing.GemCatalog;
import krys.socketing.GemDefinition;
import krys.socketing.ItemSocket;
import krys.socketing.ItemSocketing;
import krys.socketing.SocketContentType;
import krys.socketing.SocketEffectContext;
import krys.socketing.SocketGemRuneStat;
import krys.socketing.SocketingPresentationSupport;

import java.util.List;
import java.util.Optional;

/** Renderer normalnego UI gniazd itemu. */
final class SocketingSectionRenderer {
    private SocketingSectionRenderer() {
    }

    static String renderEditor(ItemImportEditableForm form) {
        ItemSocketing socketing = form.getSocketing() == null ? ItemSocketing.empty() : form.getSocketing();
        Optional<SocketEffectContext> context = resolveContext(form.getSlot(), form.getDetails());
        StringBuilder html = new StringBuilder("""
                <fieldset class="inline-fieldset socketing-fieldset" data-socketing-section>
                    <legend>Gniazda</legend>
                    <div class="socketing-grid">
                        <label>
                            Liczba gniazd
                            <select name="socketCount" data-socket-count>
                """);
        for (int count = ItemSocketing.MIN_SOCKET_COUNT; count <= ItemSocketing.MAX_SOCKET_COUNT; count++) {
            html.append("<option value=\"")
                    .append(count)
                    .append("\"")
                    .append(socketing.getSocketCount() == count ? " selected" : "")
                    .append(">")
                    .append(count)
                    .append("</option>");
        }
        html.append("""
                            </select>
                            <span class="helper">Liczba gniazd: %d</span>
                        </label>
                    </div>
                """.formatted(socketing.getSocketCount()));
        for (int index = 0; index < ItemSocketing.MAX_SOCKET_COUNT; index++) {
            html.append(renderSocketRow(index, socketing.socketAt(index), index >= socketing.getSocketCount(), context));
        }
        html.append("""
                    <p class="helper">Gemy są na tym etapie zapisywane i prezentowane, ale runtime pozostaje nieaktywny.</p>
                </fieldset>
                """);
        return html.toString();
    }

    static String renderReadonlySummary(ItemSocketing socketing, EquipmentSlot slot, krys.itemimport.ItemImportDetails details) {
        String summary = SocketingPresentationSupport.compactSummary(socketing, slot, details);
        if (summary.isBlank()) {
            return "";
        }
        return """
                <section class="subpanel socketing-readonly">
                    <h3>Gniazda</h3>
                    <p class="helper">%s</p>
                </section>
                """.formatted(escape(summary));
    }

    static String renderReadonlySummary(SavedImportedItem item) {
        return renderReadonlySummary(item.getSocketing(), item.getSlot(), item.getDetails());
    }

    static List<String> compactChips(SavedImportedItem item) {
        return SocketingPresentationSupport.compactLines(item.getSocketing(), item.getSlot(), item.getDetails());
    }

    static String renderScript() {
        return """
                (() => {
                    const sections = document.querySelectorAll('[data-socketing-section]');
                    sections.forEach(section => {
                        const countSelect = section.querySelector('[data-socket-count]');
                        const rows = Array.from(section.querySelectorAll('[data-socket-row]'));
                        if (!countSelect) return;
                        const refreshRow = row => {
                            const content = row.querySelector('[data-socket-content]');
                            const gem = row.querySelector('[data-socket-gem]');
                            const effect = row.querySelector('[data-socket-effect]');
                            const gemActive = content && content.value === 'GEM';
                            if (gem) {
                                gem.hidden = !gemActive;
                                gem.disabled = !gemActive;
                            }
                            if (effect) {
                                const option = gem && gem.selectedOptions.length ? gem.selectedOptions[0] : null;
                                effect.textContent = gemActive && option ? option.dataset.effect || '' : '';
                                effect.hidden = !gemActive || !effect.textContent;
                            }
                        };
                        const refresh = () => {
                            const count = Number.parseInt(countSelect.value || '0', 10);
                            rows.forEach(row => {
                                const index = Number.parseInt(row.dataset.socketIndex || '0', 10);
                                row.hidden = index >= count;
                                refreshRow(row);
                            });
                        };
                        rows.forEach(row => {
                            const content = row.querySelector('[data-socket-content]');
                            const gem = row.querySelector('[data-socket-gem]');
                            if (content) content.addEventListener('change', refresh);
                            if (gem) gem.addEventListener('change', refresh);
                        });
                        countSelect.addEventListener('change', refresh);
                        refresh();
                    });
                })();
                """;
    }

    private static String renderSocketRow(int index,
                                          ItemSocket socket,
                                          boolean hidden,
                                          Optional<SocketEffectContext> context) {
        SocketContentType contentType = socket.getContentType() == null ? SocketContentType.EMPTY : socket.getContentType();
        String selectedGemId = socket.getGemId() == null ? "" : socket.getGemId();
        boolean gemSelected = contentType == SocketContentType.GEM;
        return """
                <div class="socketing-row" data-socket-row data-socket-index="%d"%s>
                    <label>
                        Gniazdo %d
                        <select name="socketContent_%d" data-socket-content>
                            <option value="EMPTY"%s>Puste</option>
                            <option value="GEM"%s>Gem</option>
                            <option value="DETECTED_STAT"%s>Wykryty stat gema/runy</option>
                        </select>
                    </label>
                    <label%s>
                        Gem
                        <select name="socketGemId_%d" data-socket-gem%s>
                            %s
                        </select>
                    </label>
                    %s
                    <p class="helper socketing-effect" data-socket-effect%s>%s</p>
                </div>
                """.formatted(
                index,
                hidden ? " hidden" : "",
                index + 1,
                index,
                contentType == SocketContentType.EMPTY ? " selected" : "",
                gemSelected ? " selected" : "",
                contentType == SocketContentType.DETECTED_STAT ? " selected" : "",
                gemSelected ? "" : " hidden",
                index,
                gemSelected ? "" : " disabled",
                renderGemOptions(selectedGemId, context),
                renderDetectedStatFields(index, socket),
                gemSelected ? "" : " hidden",
                escape(selectedGemEffect(selectedGemId, context))
        );
    }

    private static String renderDetectedStatFields(int index, ItemSocket socket) {
        if (socket == null || socket.getContentType() != SocketContentType.DETECTED_STAT) {
            return """
                    <input type="hidden" name="socketDetectedDisplayText_%d" value="">
                    <input type="hidden" name="socketDetectedNormalizedText_%d" value="">
                    <input type="hidden" name="socketDetectedValue_%d" value="">
                    <input type="hidden" name="socketDetectedMatchedAffixType_%d" value="">
                    <input type="hidden" name="socketDetectedSourceLine_%d" value="">
                    """.formatted(index, index, index, index, index);
        }
        SocketGemRuneStat stat = socket.getDetectedStat();
        String displayText = stat == null ? "" : stat.getDisplayText();
        String normalizedText = stat == null ? "" : stat.getNormalizedText();
        String value = stat == null || stat.getValue() == null ? "" : formatStatValue(stat.getValue());
        String matchedType = stat == null || stat.getMatchedAffixType() == null ? "" : stat.getMatchedAffixType().name();
        String sourceLine = stat == null ? "" : stat.getSourceLine();
        return """
                <div class="helper">
                    Wykryty stat: %s · Runtime nieaktywny
                    <input type="hidden" name="socketDetectedDisplayText_%d" value="%s">
                    <input type="hidden" name="socketDetectedNormalizedText_%d" value="%s">
                    <input type="hidden" name="socketDetectedValue_%d" value="%s">
                    <input type="hidden" name="socketDetectedMatchedAffixType_%d" value="%s">
                    <input type="hidden" name="socketDetectedSourceLine_%d" value="%s">
                </div>
                """.formatted(
                escape(displayText),
                index,
                escape(displayText),
                index,
                escape(normalizedText),
                index,
                escape(value),
                index,
                escape(matchedType),
                index,
                escape(sourceLine)
        );
    }

    private static String formatStatValue(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private static String renderGemOptions(String selectedGemId, Optional<SocketEffectContext> context) {
        StringBuilder html = new StringBuilder("<option value=\"\"></option>");
        for (GemDefinition definition : GemCatalog.all()) {
            String effect = SocketingPresentationSupport.effectLabel(definition, context);
            html.append("<option value=\"")
                    .append(escape(definition.getId()))
                    .append("\" data-effect=\"")
                    .append(escape(effect))
                    .append("\"")
                    .append(definition.getId().equals(selectedGemId) ? " selected" : "")
                    .append(">")
                    .append(escape(definition.getDisplayName()))
                    .append("</option>");
        }
        return html.toString();
    }

    private static String selectedGemEffect(String gemId, Optional<SocketEffectContext> context) {
        return GemCatalog.findById(gemId)
                .map(definition -> SocketingPresentationSupport.effectLabel(definition, context))
                .orElse("");
    }

    private static Optional<SocketEffectContext> resolveContext(String rawSlot, krys.itemimport.ItemImportDetails details) {
        EquipmentSlot slot = null;
        if (rawSlot != null && !rawSlot.isBlank()) {
            try {
                slot = EquipmentSlot.valueOf(rawSlot);
            } catch (IllegalArgumentException exception) {
                slot = null;
            }
        }
        return SocketingPresentationSupport.resolveContext(slot, details);
    }

    private static String escape(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value == null ? "" : value);
    }
}
