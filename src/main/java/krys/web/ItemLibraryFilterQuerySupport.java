package krys.web;

import krys.itemlibrary.ItemLibraryFilter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Pomocnicze kodowanie filtrów biblioteki między akcjami SSR. */
final class ItemLibraryFilterQuerySupport {
    private ItemLibraryFilterQuerySupport() {
    }

    static String toQuery(ItemLibraryFilter filter) {
        List<String> parts = new ArrayList<>();
        append(parts, "q", filter.getQuery());
        append(parts, "slot", filter.getSlot());
        append(parts, "type", filter.getItemType());
        append(parts, "status", filter.getStatus());
        append(parts, "aspect", filter.getAspect());
        append(parts, "affix", filter.getAffix());
        if (filter.isGreaterOnly()) {
            append(parts, "greater", "true");
        }
        return String.join("&", parts);
    }

    static String libraryUrl(ItemLibraryFilter filter) {
        String query = toQuery(filter);
        return query.isBlank() ? "/biblioteka-itemow" : "/biblioteka-itemow?" + query;
    }

    static String hiddenFields(ItemLibraryFilter filter) {
        StringBuilder html = new StringBuilder();
        appendHidden(html, "q", filter.getQuery());
        appendHidden(html, "slot", filter.getSlot());
        appendHidden(html, "type", filter.getItemType());
        appendHidden(html, "status", filter.getStatus());
        appendHidden(html, "aspect", filter.getAspect());
        appendHidden(html, "affix", filter.getAffix());
        if (filter.isGreaterOnly()) {
            appendHidden(html, "greater", "true");
        }
        return html.toString();
    }

    private static void append(List<String> parts, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        parts.add(encode(name) + "=" + encode(value));
    }

    private static void appendHidden(StringBuilder html, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        html.append("<input type=\"hidden\" name=\"")
                .append(escapeHtml(name))
                .append("\" value=\"")
                .append(escapeHtml(value))
                .append("\">");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String escapeHtml(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }
}
