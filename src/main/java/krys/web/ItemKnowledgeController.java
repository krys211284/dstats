package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.itemknowledge.ItemKnowledgeService;
import krys.itemknowledge.ItemKnowledgeSnapshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Kontroler SSR osobnej bazy wiedzy o itemach. */
public final class ItemKnowledgeController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final ItemKnowledgeService itemKnowledgeService;
    private final ItemKnowledgePageRenderer renderer;

    public ItemKnowledgeController(ItemKnowledgeService itemKnowledgeService, ItemKnowledgePageRenderer renderer) {
        this.itemKnowledgeService = itemKnowledgeService;
        this.renderer = renderer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                renderPage(exchange, new ItemKnowledgePageModel(itemKnowledgeService.getSnapshot(), List.of(), List.of()));
                return;
            }
            if ("POST".equals(method)) {
                renderPage(exchange, handlePost(exchange));
                return;
            }
            exchange.getResponseHeaders().set("Allow", "GET, POST");
            exchange.sendResponseHeaders(405, -1);
        } finally {
            exchange.close();
        }
    }

    private ItemKnowledgePageModel handlePost(HttpExchange exchange) throws IOException {
        Map<String, String> fields = UrlEncodedFormSupport.parseBody(exchange);
        String action = fields.getOrDefault("action", "");
        if (!"resetKnowledge".equals(action)) {
            return new ItemKnowledgePageModel(itemKnowledgeService.getSnapshot(), List.of(), List.of("Nieobsługiwana akcja bazy wiedzy."));
        }
        ItemKnowledgeSnapshot snapshot = itemKnowledgeService.resetKnowledge(fields.getOrDefault("epochLabel", ""));
        return new ItemKnowledgePageModel(snapshot, List.of("Rozpoczęto nową epokę wiedzy i wyczyszczono aktywne obserwacje."), List.of());
    }

    private void renderPage(HttpExchange exchange, ItemKnowledgePageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}
