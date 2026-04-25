package krys.web;

import krys.itemknowledge.ItemKnowledgeEntry;
import krys.itemknowledge.ItemKnowledgeSnapshot;
import krys.itemimport.ImportedItemAffixType;

import java.util.Map;

/** Renderuje SSR bazy wiedzy o itemach jako warstwy obserwacji i resetu sezonowego. */
public final class ItemKnowledgePageRenderer {
    public String render(ItemKnowledgePageModel model) {
        return """
                <!DOCTYPE html>
                <html lang="pl">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Build WebApp - Baza wiedzy itemów</title>
                    <style>
                        %s
                        .layout { max-width: 1180px; margin: 0 auto; padding: 28px 16px 48px; }
                        .knowledge-grid { display: grid; gap: 16px; }
                        .knowledge-entry { padding: 16px; border: 1px solid var(--line); border-radius: 12px; background: #fff; }
                        .knowledge-entry h3 { margin-top: 0; }
                        .observation-list { margin: 8px 0 0; padding-left: 18px; }
                        .reset-form { display: flex; flex-wrap: wrap; gap: 10px; align-items: end; margin-top: 12px; }
                        .reset-form label { display: grid; gap: 6px; min-width: min(100%%, 280px); font-weight: 700; }
                        .reset-form input { padding: 10px 12px; border: 1px solid var(--line); border-radius: 10px; font: inherit; }
                    </style>
                </head>
                <body>
                <main class="layout">
                    %s
                    <section class="panel">
                        <span class="section-kicker">Itemy i import</span>
                        <h1>Baza wiedzy itemów</h1>
                        <p class="helper">To osobna warstwa obserwacji ucząca się wyłącznie z ręcznie zatwierdzonych itemów. Nie zmienia zapisanych itemów użytkownika ani runtime.</p>
                    </section>
                    %s
                    %s
                    %s
                    %s
                </main>
                </body>
                </html>
                """.formatted(
                AppShellRendererSupport.renderSharedStyles(),
                AppShellRendererSupport.renderGlobalNavigation("/baza-wiedzy-itemow"),
                renderMessages(model),
                renderErrors(model),
                renderSummary(model.getSnapshot()),
                renderEntries(model.getSnapshot())
        );
    }

    private static String renderMessages(ItemKnowledgePageModel model) {
        if (model.getMessages().isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<section class=\"panel panel-success\"><ul class=\"message-list\">");
        for (String message : model.getMessages()) {
            html.append("<li>").append(escapeHtml(message)).append("</li>");
        }
        html.append("</ul></section>");
        return html.toString();
    }

    private static String renderErrors(ItemKnowledgePageModel model) {
        if (model.getErrors().isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<section class=\"panel panel-error\"><ul class=\"message-list\">");
        for (String error : model.getErrors()) {
            html.append("<li>").append(escapeHtml(error)).append("</li>");
        }
        html.append("</ul></section>");
        return html.toString();
    }

    private static String renderSummary(ItemKnowledgeSnapshot snapshot) {
        return """
                <section class="panel">
                    <h2>Aktywna epoka wiedzy</h2>
                    <div class="summary-grid">
                        %s
                        %s
                        %s
                        %s
                        %s
                    </div>
                    <form class="reset-form" method="post" action="/baza-wiedzy-itemow">
                        <input type="hidden" name="action" value="resetKnowledge">
                        <label>
                            Nazwa nowej epoki
                            <input type="text" name="epochLabel" value="" placeholder="np. Sezon 8 / patch 2.x">
                        </label>
                        <button type="submit" class="secondary-button">Rozpocznij nową epokę i wyczyść obserwacje</button>
                    </form>
                </section>
                """.formatted(
                renderSummaryCard("Epoka", snapshot.getActiveEpoch().label()),
                renderSummaryCard("Wpisy typów itemów", Integer.toString(snapshot.getEntryCount())),
                renderSummaryCard("Zatwierdzone itemy w obserwacjach", Integer.toString(snapshot.getItemObservationCount())),
                renderSummaryCard("Obserwacje affixów", Integer.toString(snapshot.getAffixObservationCount())),
                renderSummaryCard("Obserwacje aspektów", Integer.toString(snapshot.getAspectObservationCount()))
        );
    }

    private static String renderEntries(ItemKnowledgeSnapshot snapshot) {
        if (snapshot.getEntries().isEmpty()) {
            return """
                    <section class="panel">
                        <h2>Obserwacje</h2>
                        <div class="empty-state">
                            <h3>Brak obserwacji w aktywnej epoce</h3>
                            <p>Baza wiedzy zacznie się uczyć dopiero po ręcznym zatwierdzeniu itemu w imporcie.</p>
                            <a class="nav-link" href="/importuj-item-ze-screena">Importuj item ze screena</a>
                        </div>
                    </section>
                    """;
        }
        StringBuilder html = new StringBuilder("""
                <section class="panel">
                    <h2>Obserwacje według typu itemu</h2>
                    <div class="knowledge-grid">
                """);
        for (ItemKnowledgeEntry entry : snapshot.getEntries()) {
            html.append(renderEntry(entry));
        }
        html.append("</div></section>");
        return html.toString();
    }

    private static String renderEntry(ItemKnowledgeEntry entry) {
        return """
                <article class="knowledge-entry">
                    <span class="section-kicker">%s</span>
                    <h3>%s</h3>
                    <div class="summary-grid">
                        %s
                        %s
                        %s
                    </div>
                    <h4>Zaobserwowane typy affixów</h4>
                    %s
                    <h4>Zaobserwowane aspekty / efekty specjalne</h4>
                    %s
                </article>
                """.formatted(
                escapeHtml(entry.getKey().slot().name()),
                escapeHtml(entry.getKey().itemType()),
                renderSummaryCard("Itemy", Integer.toString(entry.getItemObservationCount())),
                renderSummaryCard("Affixy", Integer.toString(entry.getTotalAffixObservations())),
                renderSummaryCard("Aspekty", Integer.toString(entry.getTotalAspectObservations())),
                renderAffixCounts(entry.getAffixTypeCounts()),
                renderAspectCounts(entry.getAspectCounts())
        );
    }

    private static String renderAffixCounts(Map<ImportedItemAffixType, Integer> counts) {
        if (counts.isEmpty()) {
            return "<p class=\"helper\">Brak zaobserwowanych affixów.</p>";
        }
        StringBuilder html = new StringBuilder("<ul class=\"observation-list\">");
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> html.append("<li>")
                        .append(escapeHtml(entry.getKey().getDisplayName()))
                        .append(": ")
                        .append(entry.getValue())
                        .append("</li>"));
        html.append("</ul>");
        return html.toString();
    }

    private static String renderAspectCounts(Map<String, Integer> counts) {
        if (counts.isEmpty()) {
            return "<p class=\"helper\">Brak zaobserwowanych aspektów.</p>";
        }
        StringBuilder html = new StringBuilder("<ul class=\"observation-list\">");
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> html.append("<li>")
                        .append(escapeHtml(entry.getKey()))
                        .append(": ")
                        .append(entry.getValue())
                        .append("</li>"));
        html.append("</ul>");
        return html.toString();
    }

    private static String renderSummaryCard(String label, String value) {
        return """
                <div class="summary-card">
                    <div class="summary-label">%s</div>
                    <div class="summary-value">%s</div>
                </div>
                """.formatted(escapeHtml(label), escapeHtml(value));
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
